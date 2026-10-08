package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterEcheances;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Passages simultanés sur la même échéance, sur une vraie base : la prise conditionnelle de l'échéance ne laisse
 * passer qu'un seul virement et un seul débit.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, properties = "spring.kafka.listener.auto-startup=false")
@DirtiesContext
@Tag("FR-021-07")
@Tag("FR-021-02")
class EcheanceTransitionConcurrenteTest {

    private static final int MANCHES = 5;
    private static final int PASSAGES = 4;

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private ExecuterEcheances echeances;
    @Autowired
    private JdbcClient jdbc;
    @Autowired
    private DataSource source;

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
    }

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @Test
    void deux_passages_simultanes_ne_creent_qu_un_virement_et_un_debit() throws Exception {
        for (int manche = 0; manche < MANCHES; manche++) {
            jdbc.sql("delete from virements_programmes").update();
            jdbc.sql("delete from operations").update();
            jdbc.sql("delete from virements").update();
            jdbc.sql("update comptes set solde = 2130.00 where id = 1").update();
            jdbc.sql("""
                    insert into virements_programmes (id, client_id, compte_source_id, numero_beneficiaire,
                        libelle_beneficiaire, montant, frequence, motif, prochaine_execution, actif, nb_executions,
                        cree_le)
                    values (1, 1, 1, 'FR76-0001-0002', 'Livret', 100.00, 'MENSUEL', null,
                        now() - interval '1 hour', true, 0, now())""").update();

            int total = lancer();

            assertThat(total).isEqualTo(1);
            assertThat(jdbc.sql("select count(*) from virements").query(Long.class).single()).isEqualTo(1);
            assertThat(jdbc.sql("select count(*) from operations").query(Long.class).single()).isEqualTo(2);
            assertThat(jdbc.sql("select solde from comptes where id = 1").query(BigDecimal.class).single())
                    .isEqualByComparingTo("2030.00");
            assertThat(jdbc.sql("select nb_executions from virements_programmes").query(Integer.class).single())
                    .isEqualTo(1);
        }
    }

    private int lancer() throws Exception {
        ExecutorService fils = Executors.newFixedThreadPool(PASSAGES);
        try {
            CountDownLatch depart = new CountDownLatch(1);
            List<Future<Integer>> futurs = new ArrayList<>();
            for (int i = 0; i < PASSAGES; i++) {
                futurs.add(fils.submit(() -> {
                    depart.await();
                    return echeances.executerLesDues();
                }));
            }
            depart.countDown();
            int total = 0;
            for (Future<Integer> futur : futurs) {
                total += futur.get();
            }
            return total;
        } finally {
            fils.shutdownNow();
        }
    }
}
