package fr.nextbanking.credits.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.EcheancePret;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Transition conditionnelle {@code UPDATE ... WHERE status = 'EN_ATTENTE'} sur une vraie base : une approbation et un
 * refus concurrents, une seule transition gagne, un seul échéancier existe, le perdant est ignoré.
 */
@SpringBootTest(classes = ApplicationCredits.class)
@DirtiesContext
class PretTransitionConcurrenteTest {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
        registre.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @Autowired
    private DataSource base;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PretRepository prets;

    @Autowired
    private Transaction transaction;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    private long pretEnAttente() {
        return jdbc.sql("""
                insert into loans (customer_id, type, montant, taux_annuel, duree_mois, mensualite, cout_total,
                                   total_interets, motif, status, date_debut, date_fin)
                values (5, 'CONSOMMATION', 1200, 0, 12, 100, 1200, 0, 'x', 'EN_ATTENTE', date '2026-10-01',
                        date '2027-10-01') returning id""").query(Long.class).single();
    }

    @Test
    @Tag("FR-024-08")
    void une_approbation_et_un_refus_concurrents_n_ont_qu_un_gagnant() throws Exception {
        long pretId = pretEnAttente();
        CountDownLatch approbationEcrite = new CountDownLatch(1);
        CountDownLatch refusLance = new CountDownLatch(1);

        CompletableFuture<Boolean> approbation = CompletableFuture.supplyAsync(() -> transaction.executer(() -> {
            boolean gagne = prets.transitionner(pretId, StatutPret.EN_ATTENTE, StatutPret.EN_COURS);
            if (gagne) {
                prets.creerEcheancier(pretId, echeancier(pretId));
            }
            approbationEcrite.countDown();
            attendre(refusLance);
            return gagne;
        }));
        approbationEcrite.await(20, TimeUnit.SECONDS);
        CompletableFuture<Boolean> refus = CompletableFuture.supplyAsync(() -> {
            refusLance.countDown();
            // Bloque sur la ligne verrouillée jusqu'à la validation de l'approbation, puis ne trouve plus EN_ATTENTE.
            return transaction.executer(() -> prets.transitionner(pretId, StatutPret.EN_ATTENTE, StatutPret.REFUSE));
        });

        assertThat(approbation.get(30, TimeUnit.SECONDS)).isTrue();
        assertThat(refus.get(30, TimeUnit.SECONDS)).isFalse();
        assertThat(jdbc.sql("select status from loans where id = :id").param("id", pretId).query(String.class)
                .single()).isEqualTo("EN_COURS");
        assertThat(jdbc.sql("select count(*) from loan_repayments where loan_id = :id and not paye")
                .param("id", pretId).query(Long.class).single()).isEqualTo(12L);
    }

    @Test
    @Tag("FR-024-08")
    void une_transition_depuis_un_autre_statut_ne_change_rien() {
        long pretId = pretEnAttente();

        assertThat(prets.transitionner(pretId, StatutPret.EN_ATTENTE, StatutPret.REFUSE)).isTrue();
        assertThat(prets.transitionner(pretId, StatutPret.EN_ATTENTE, StatutPret.EN_COURS)).isFalse();
        assertThat(prets.parId(pretId).orElseThrow().statut()).isEqualTo(StatutPret.REFUSE);
    }

    private java.util.List<EcheancePret> echeancier(long pretId) {
        return EcheancePret.echeancier(prets.parId(pretId).orElseThrow());
    }

    private static void attendre(CountDownLatch verrou) {
        try {
            verrou.await(20, TimeUnit.SECONDS);
            // Laisse le refus atteindre le verrou de ligne avant de valider.
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
