package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement.Commande;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement;
import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Soumissions concurrentes sur une vraie base : la mise à jour conditionnelle {@code solde >= montant} garde le solde
 * positif, un seul virement gagne quand le solde n'en couvre qu'un, et une même clé d'idempotence ne débite qu'une fois
 * (AR-43).
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Import(VirementTransitionConcurrenteTest.ConfigurationHorloge.class)
@Tag("FR-003-06")
@Tag("BUG-008")
class VirementTransitionConcurrenteTest {

    private static final int MANCHES = 5;

    /** Horloge réglée à 10:00 : le contrôle de fraude suspend tout virement à partir de 23:00 (heure inhabituelle). */
    @TestConfiguration
    static class ConfigurationHorloge {
        @Bean
        @Primary
        HorlogeFixe horlogeFixe() {
            return new HorlogeFixe();
        }
    }

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private ExecuterVirement executerVirement;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private DataSource source;

    @AfterEach
    void nettoyer() {
        DonneesDeReference.vider(jdbc);
    }

    @BeforeEach
    void recharger() throws Exception {
        DonneesDeReference.recharger(source, jdbc);
    }

    @Test
    void un_solde_qui_ne_couvre_qu_un_virement_n_en_laisse_executer_qu_un() throws Exception {
        for (int manche = 0; manche < MANCHES; manche++) {
            jdbc.sql("update comptes set solde = 150.00 where id = 1").update();
            jdbc.sql("truncate table operations, virements restart identity cascade").update();

            List<Object> issues = lancer(commande("a" + manche), commande("b" + manche));

            assertThat(issues.stream().filter(i -> i instanceof ExecuterVirement.Resultat)).hasSize(1);
            assertThat(issues.stream().filter(i -> i instanceof ErreurVirement erreur
                    && erreur.motif() == Cause.SOLDE_INSUFFISANT)
                    .map(i -> ((ErreurVirement) i).getMessage()))
                    .containsExactly("Solde insuffisant sur le compte source");
            assertThat(solde(1)).isEqualByComparingTo("50.00");
            assertThat(count("virements")).isEqualTo(1);
            assertThat(count("operations")).isEqualTo(2);
        }
    }

    @Test
    void deux_virements_croises_ne_s_interbloquent_pas_et_gardent_la_somme_des_soldes() throws Exception {
        for (int manche = 0; manche < MANCHES; manche++) {
            List<Object> issues = lancer(commande("x" + manche),
                    new Commande(1, 2L, 1L, "10.00", null, "y" + manche, "127.0.0.1"));

            assertThat(issues).allMatch(i -> i instanceof ExecuterVirement.Resultat);
        }
        assertThat(solde(1).add(solde(2))).isEqualByComparingTo("17330.00");
    }

    @Test
    void une_meme_cle_d_idempotence_soumise_en_meme_temps_ne_debite_qu_une_fois() throws Exception {
        List<Object> issues = lancer(commande("meme-cle"), commande("meme-cle"), commande("meme-cle"));

        assertThat(issues).allMatch(i -> i instanceof ExecuterVirement.Resultat);
        assertThat(issues.stream().map(i -> ((ExecuterVirement.Resultat) i).virementId()).distinct()).hasSize(1);
        assertThat(count("virements")).isEqualTo(1);
        assertThat(solde(1)).isEqualByComparingTo("2030.00");
    }

    private static Commande commande(String cle) {
        return new Commande(1, 1L, 2L, "100.00", "concurrent", cle, "127.0.0.1");
    }

    private List<Object> lancer(Commande... commandes) throws Exception {
        ExecutorService fils = Executors.newFixedThreadPool(commandes.length);
        try {
            CountDownLatch depart = new CountDownLatch(1);
            List<Future<Object>> futurs = new ArrayList<>();
            for (Commande commande : commandes) {
                Callable<Object> travail = () -> {
                    depart.await();
                    try {
                        return executerVirement.executer(commande);
                    } catch (ErreurVirement refus) {
                        return refus;
                    }
                };
                futurs.add(fils.submit(travail));
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

    private BigDecimal solde(long compteId) {
        return jdbc.sql("select solde from comptes where id = :id").param("id", compteId)
                .query(BigDecimal.class).single();
    }

    private long count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Long.class).single();
    }
}
