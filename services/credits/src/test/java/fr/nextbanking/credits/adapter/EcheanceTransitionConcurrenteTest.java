package fr.nextbanking.credits.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.port.in.PayerEcheance;
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
 * Paiements concurrents sur une vraie base : deux paiements de la même échéance n'ont qu'un effet (une date), et deux
 * paiements simultanés des deux dernières échéances laissent le prêt REMBOURSE grâce au verrou de la ligne du prêt.
 */
@SpringBootTest(classes = ApplicationCredits.class)
@DirtiesContext
class EcheanceTransitionConcurrenteTest {

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
    private PayerEcheance payerEcheance;

    @BeforeEach
    void donnees() {
        OutilsHttp.chargerDonnees(base);
    }

    private <T> java.util.List<T> enParallele(java.util.function.Supplier<T> premier,
            java.util.function.Supplier<T> second) throws Exception {
        CountDownLatch depart = new CountDownLatch(1);
        CompletableFuture<T> a = CompletableFuture.supplyAsync(() -> {
            attendre(depart);
            return premier.get();
        });
        CompletableFuture<T> b = CompletableFuture.supplyAsync(() -> {
            attendre(depart);
            return second.get();
        });
        depart.countDown();
        return java.util.List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
    }

    private static void attendre(CountDownLatch depart) {
        try {
            depart.await(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @Tag("FR-007-12")
    void deux_paiements_concurrents_de_la_meme_echeance_n_ont_qu_un_effet() throws Exception {
        var resultats = enParallele(() -> payerEcheance.payer(1L, 5L), () -> payerEcheance.payer(1L, 5L));

        assertThat(resultats).allSatisfy(echeance -> assertThat(echeance.paye()).isTrue());
        assertThat(resultats.get(0).datePaiement()).isEqualTo(resultats.get(1).datePaiement());
        assertThat(jdbc.sql("select count(*) from loan_repayments where loan_id = 1 and not paye")
                .query(Long.class).single()).isEqualTo(7L);
        assertThat(jdbc.sql("select status from loans where id = 1").query(String.class).single())
                .isEqualTo("EN_COURS");
    }

    @Test
    @Tag("FR-007-12")
    @Tag("FR-007-13")
    void deux_paiements_concurrents_des_deux_dernieres_echeances_laissent_le_pret_rembourse() throws Exception {
        payerEcheance.payer(1L, 15L);
        payerEcheance.payer(1L, 16L);

        enParallele(() -> payerEcheance.payer(1L, 17L), () -> payerEcheance.payer(1L, 18L));

        assertThat(jdbc.sql("select count(*) from loan_repayments where loan_id = 2 and not paye")
                .query(Long.class).single()).isZero();
        assertThat(jdbc.sql("select status from loans where id = 2").query(String.class).single())
                .isEqualTo("REMBOURSE");
    }
}
