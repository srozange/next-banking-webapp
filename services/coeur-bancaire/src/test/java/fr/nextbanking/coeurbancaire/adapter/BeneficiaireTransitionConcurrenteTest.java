package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.SupprimerBeneficiaire;
import fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire;
import fr.nextbanking.coeurbancaire.domain.ErreurBeneficiaire.Cause;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Suppressions simultanées du même bénéficiaire sur une vraie base : la mise à jour conditionnelle
 * {@code where actif} ne laisse passer qu'une transition actif vers inactif, la perdante est introuvable.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-013-09")
@Tag("BUG-047")
class BeneficiaireTransitionConcurrenteTest {

    private static final int MANCHES = 5;
    private static final int CONCURRENTS = 4;

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private SupprimerBeneficiaire supprimer;

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
    void une_seule_suppression_simultanee_gagne_et_les_autres_sont_introuvables() throws Exception {
        for (int manche = 0; manche < MANCHES; manche++) {
            jdbc.sql("update beneficiaires_externes set actif = true where id = 1").update();

            List<Object> issues = lancer();

            assertThat(issues.stream().filter(i -> i instanceof Boolean)).hasSize(1);
            assertThat(issues.stream().filter(i -> i instanceof ErreurBeneficiaire erreur
                    && erreur.motif() == Cause.INTROUVABLE)).hasSize(CONCURRENTS - 1);
            assertThat(jdbc.sql("select actif from beneficiaires_externes where id = 1").query(Boolean.class)
                    .single()).isFalse();
        }
    }

    private List<Object> lancer() throws Exception {
        ExecutorService fils = Executors.newFixedThreadPool(CONCURRENTS);
        try {
            CountDownLatch depart = new CountDownLatch(1);
            List<Future<Object>> futurs = new ArrayList<>();
            for (int i = 0; i < CONCURRENTS; i++) {
                Callable<Object> travail = () -> {
                    depart.await();
                    try {
                        supprimer.supprimer(1, 1);
                        return Boolean.TRUE;
                    } catch (ErreurBeneficiaire refus) {
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
}
