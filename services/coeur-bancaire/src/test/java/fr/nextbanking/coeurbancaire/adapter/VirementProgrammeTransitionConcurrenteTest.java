package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.GererEtatVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme.Cause;
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
 * Suspensions et suppressions simultanées du même virement programmé sur une vraie base : les mises à jour
 * conditionnelles n'appliquent chaque transition qu'une fois, les perdantes sont introuvables.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-004-09")
@Tag("FR-004-10")
class VirementProgrammeTransitionConcurrenteTest {

    private static final int MANCHES = 5;
    private static final int PAR_ACTION = 3;

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private GererEtatVirementProgramme etat;

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

    private void inserer() {
        jdbc.sql("delete from virements_programmes").update();
        jdbc.sql("""
                insert into virements_programmes (id, client_id, compte_source_id, numero_beneficiaire,
                    libelle_beneficiaire, montant, frequence, motif, prochaine_execution, actif, nb_executions, cree_le)
                values (1, 1, 1, 'FR76-0001-0002', 'Loyer', 50.00, 'MENSUEL', null, now(), true, 0, now())""")
                .update();
    }

    @Test
    void une_suppression_et_au_plus_une_suspension_gagnent_les_autres_sont_introuvables() throws Exception {
        for (int manche = 0; manche < MANCHES; manche++) {
            inserer();

            List<Object> suspensions = new ArrayList<>();
            List<Object> suppressions = new ArrayList<>();
            lancer(suspensions, suppressions);

            assertThat(suppressions.stream().filter(i -> i instanceof Boolean)).hasSize(1);
            assertThat(suppressions.stream().filter(VirementProgrammeTransitionConcurrenteTest::introuvable))
                    .hasSize(PAR_ACTION - 1);
            assertThat(suspensions.stream().filter(i -> i instanceof Boolean).count()).isLessThanOrEqualTo(1);
            assertThat(suspensions.stream().filter(i -> i instanceof Boolean).count()
                    + suspensions.stream().filter(VirementProgrammeTransitionConcurrenteTest::introuvable).count())
                    .isEqualTo(PAR_ACTION);
            assertThat(jdbc.sql("select count(*) from virements_programmes where id = 1").query(Long.class)
                    .single()).isZero();
        }
    }

    private static boolean introuvable(Object issue) {
        return issue instanceof ErreurVirementProgramme erreur && erreur.motif() == Cause.INTROUVABLE;
    }

    private void lancer(List<Object> suspensions, List<Object> suppressions) throws Exception {
        ExecutorService fils = Executors.newFixedThreadPool(2 * PAR_ACTION);
        try {
            CountDownLatch depart = new CountDownLatch(1);
            List<Future<Object>> suspendus = new ArrayList<>();
            List<Future<Object>> supprimes = new ArrayList<>();
            for (int i = 0; i < PAR_ACTION; i++) {
                suspendus.add(fils.submit(travail(depart, () -> etat.suspendre(1, 1))));
                supprimes.add(fils.submit(travail(depart, () -> etat.supprimer(1, 1))));
            }
            depart.countDown();
            for (Future<Object> futur : suspendus) {
                suspensions.add(futur.get());
            }
            for (Future<Object> futur : supprimes) {
                suppressions.add(futur.get());
            }
        } finally {
            fils.shutdownNow();
        }
    }

    private static Callable<Object> travail(CountDownLatch depart, Runnable action) {
        return () -> {
            depart.await();
            try {
                action.run();
                return Boolean.TRUE;
            } catch (ErreurVirementProgramme refus) {
                return refus;
            }
        };
    }
}
