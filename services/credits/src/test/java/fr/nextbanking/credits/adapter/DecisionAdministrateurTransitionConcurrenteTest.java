package fr.nextbanking.credits.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.port.in.DecisionAdministrateurPret;
import fr.nextbanking.credits.domain.Role;
import fr.nextbanking.credits.infrastructure.ApplicationCredits;
import fr.nextbanking.credits.infrastructure.Conteneurs;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
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
 * Décisions concurrentes de l'administrateur sur le même prêt EN_ATTENTE, sur une vraie base : une seule transition
 * gagne, un seul échéancier existe, une seule ligne d'audit est écrite, le perdant est ignoré sans erreur.
 */
@SpringBootTest(classes = ApplicationCredits.class)
@DirtiesContext
@Tag("FR-016-04")
@Tag("FR-016-05")
@Tag("FR-026-04")
class DecisionAdministrateurTransitionConcurrenteTest {

    private static final Set<Role> ADMIN = Set.of(Role.ADMIN);

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
    private DecisionAdministrateurPret decision;

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
    void une_approbation_et_un_refus_simultanes_n_ont_qu_un_effet_et_une_seule_ligne_d_audit() throws Exception {
        for (int essai = 0; essai < 5; essai++) {
            long pretId = pretEnAttente();
            CyclicBarrier depart = new CyclicBarrier(2);

            CompletableFuture<Void> approbation = CompletableFuture.runAsync(() -> {
                attendre(depart);
                decision.approuver(ADMIN, "10", pretId, "203.0.113.7");
            });
            CompletableFuture<Void> refus = CompletableFuture.runAsync(() -> {
                attendre(depart);
                decision.refuser(ADMIN, "11", pretId, "203.0.113.8");
            });
            approbation.get(30, TimeUnit.SECONDS);
            refus.get(30, TimeUnit.SECONDS);

            String statut = jdbc.sql("select status from loans where id = :id").param("id", pretId)
                    .query(String.class).single();
            List<String> actions = jdbc.sql("select charge from outbox_audit where cle = :cle")
                    .param("cle", Long.toString(pretId)).query(String.class).list();
            long echeances = jdbc.sql("select count(*) from loan_repayments where loan_id = :id")
                    .param("id", pretId).query(Long.class).single();
            assertThat(actions).hasSize(1);
            if (statut.equals("EN_COURS")) {
                assertThat(actions.getFirst()).contains("ADMIN_LOAN_APPROVE");
                assertThat(echeances).isEqualTo(12L);
            } else {
                assertThat(statut).isEqualTo("REFUSE");
                assertThat(actions.getFirst()).contains("ADMIN_LOAN_REFUSE");
                assertThat(echeances).isZero();
            }
        }
    }

    private static void attendre(CyclicBarrier depart) {
        try {
            depart.await(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
