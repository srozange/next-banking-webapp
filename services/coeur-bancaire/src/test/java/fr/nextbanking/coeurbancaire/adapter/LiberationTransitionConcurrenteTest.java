package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.CommandeDecision;
import fr.nextbanking.coeurbancaire.application.port.in.IssueDecision;
import fr.nextbanking.coeurbancaire.application.port.in.LibererVirement;
import fr.nextbanking.coeurbancaire.application.port.in.RejeterEcheances;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Transitions concurrentes sur une vraie base : une libération et le rejet automatique à l'heure butoir (horloge qui
 * tranche, B5), deux libérations simultanées avant l'heure butoir. La transition conditionnelle laisse gagner une
 * seule : un seul état final, aucun débit, une seule entrée d'audit de décision, une seule remise.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, properties = "spring.kafka.listener.auto-startup=false")
@DirtiesContext
@Import(LiberationTransitionConcurrenteTest.ConfigurationHorloge.class)
@Tag("FR-027-16")
@Tag("FR-027-17")
@Tag("FR-016-19")
class LiberationTransitionConcurrenteTest {

    private static final int MANCHES = 5;
    private static final Instant BUTOIR = HorlogeFixe.DIX_HEURES_PARIS.plusSeconds(24 * 3600);

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @TestConfiguration
    static class ConfigurationHorloge {
        @Bean
        @Primary
        HorlogeFixe horlogeFixe() {
            return new HorlogeFixe();
        }
    }

    @Autowired
    private HorlogeFixe horloge;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private DataSource source;
    @Autowired
    private LibererVirement libererVirement;
    @Autowired
    private RejeterEcheances rejeterEcheances;

    @BeforeEach
    void recharger() throws Exception {
        horloge.regler(HorlogeFixe.DIX_HEURES_PARIS);
        DonneesDeReference.recharger(source, jdbc);
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    private void suspendu() {
        jdbc.sql("truncate table virements, outbox, outbox_audit restart identity cascade").update();
        jdbc.sql("""
                insert into virements (id, client_id, compte_source_id, compte_destinataire_id, montant, etat, statut,
                                       soumis_le, heure_butoir, motif_suspension)
                values (12, 1, 1, 2, 100.00, 'SUSPENDU', 'SUCCESS', timestamptz '2026-11-17 09:00:00+00',
                        timestamptz '2026-11-18 09:00:00+00', 'motif')""").update();
    }

    private static CommandeDecision decision() {
        return new CommandeDecision(99, Set.of(Role.ADMIN), 12, "vérifié", "203.0.113.9");
    }

    private List<Object> lancer(Callable<Object>... travaux) throws Exception {
        ExecutorService fils = Executors.newFixedThreadPool(travaux.length);
        try {
            CountDownLatch depart = new CountDownLatch(1);
            List<Future<Object>> futurs = new ArrayList<>();
            for (Callable<Object> travail : travaux) {
                futurs.add(fils.submit(() -> {
                    depart.await();
                    try {
                        return travail.call();
                    } catch (ErreurVirement refus) {
                        return refus;
                    }
                }));
            }
            depart.countDown();
            List<Object> issues = new ArrayList<>();
            for (Future<Object> futur : futurs) {
                issues.add(futur.get());
            }
            return issues;
        } finally {
            fils.shutdownNow();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void une_liberation_et_le_rejet_automatique_a_l_heure_butoir_ne_laissent_gagner_qu_une_transition() throws Exception {
        for (int manche = 0; manche < MANCHES; manche++) {
            suspendu();
            horloge.regler(BUTOIR);

            lancer(() -> libererVirement.liberer(decision()), () -> rejeterEcheances.rejeterLesEchus());

            assertThat(jdbc.sql("select etat from virements where id = 12").query(String.class).single())
                    .isEqualTo("REJETE");
            assertThat(jdbc.sql("select solde from comptes where id = 1").query(String.class).single())
                    .isEqualTo("2130.00");
            assertThat(jdbc.sql("select count(*) from operations").query(Long.class).single()).isZero();
            assertThat(jdbc.sql("select count(*) from outbox").query(Long.class).single()).isZero();
            assertThat(jdbc.sql("select count(*) from outbox_audit where charge like '%ADMIN_TRANSFER_RELEASE%' "
                    + "or charge like '%TRANSFER_AUTO_REJECT%'").query(Long.class).single()).isEqualTo(1L);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void deux_liberations_simultanees_ne_remettent_le_virement_qu_une_fois() throws Exception {
        for (int manche = 0; manche < MANCHES; manche++) {
            suspendu();

            List<Object> issues = lancer(() -> libererVirement.liberer(decision()),
                    () -> libererVirement.liberer(decision()));

            assertThat(issues.stream().filter(i -> i == IssueDecision.ACCEPTEE)).hasSize(1);
            assertThat(issues.stream().filter(i -> i instanceof ErreurVirement)).hasSize(1);
            assertThat(jdbc.sql("select etat from virements where id = 12").query(String.class).single())
                    .isEqualTo("LIBERE");
            assertThat(jdbc.sql("select count(*) from outbox where topic = 'coeur-bancaire.virement-a-traiter.v1'")
                    .query(Long.class).single()).isEqualTo(1L);
            assertThat(jdbc.sql("select count(*) from outbox_audit").query(Long.class).single()).isEqualTo(1L);
        }
    }
}
