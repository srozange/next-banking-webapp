package fr.nextbanking.coeurbancaire.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.RevoirAlertesFraude;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Confirmations et classements en faux positif simultanés de la même alerte sur une vraie base : la mise à jour
 * conditionnelle {@code where statut = 'OPEN'} ne laisse passer qu'une transition ; le statut final est l'une des deux
 * issues, avec l'auteur de la revue gagnante.
 */
@SpringBootTest(classes = ApplicationCoeurBancaire.class, properties = "spring.kafka.listener.auto-startup=false")
@DirtiesContext
@Tag("FR-016-15")
class AlerteFraudeTransitionConcurrenteTest {

    private static final int MANCHES = 5;
    private static final int CONCURRENTS = 4;
    private static final Set<Role> ADMIN = Set.of(Role.ADMIN);

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private RevoirAlertesFraude revoir;

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
    void une_seule_revue_simultanee_gagne() throws Exception {
        for (int manche = 0; manche < MANCHES; manche++) {
            long id = jdbc.sql("""
                    insert into alertes_fraude (client_id, regle, score, description, statut, cree_le)
                    values (1, 'LARGE_AMOUNT', 45, 'a', 'OPEN', timestamptz '2026-11-17 09:00:00+00')
                    returning id""").query(Long.class).single();

            List<Boolean> issues = lancer(id);

            assertThat(issues.stream().filter(Boolean::booleanValue)).hasSize(1);
            int gagnant = issues.indexOf(Boolean.TRUE);
            StatutAlerte attendu = gagnant % 2 == 0 ? StatutAlerte.CONFIRMED : StatutAlerte.FALSE_POSITIVE;
            assertThat(jdbc.sql("select statut from alertes_fraude where id = :id").param("id", id)
                    .query(String.class).single()).isEqualTo(attendu.name());
            assertThat(jdbc.sql("select revu_par from alertes_fraude where id = :id").param("id", id)
                    .query(String.class).single()).isEqualTo(Integer.toString(100 + gagnant));
        }
    }

    private List<Boolean> lancer(long id) throws Exception {
        ExecutorService fils = Executors.newFixedThreadPool(CONCURRENTS);
        try {
            CountDownLatch depart = new CountDownLatch(1);
            List<Future<Boolean>> futurs = new ArrayList<>();
            for (int i = 0; i < CONCURRENTS; i++) {
                StatutAlerte issue = i % 2 == 0 ? StatutAlerte.CONFIRMED : StatutAlerte.FALSE_POSITIVE;
                long acteur = 100 + i;
                Callable<Boolean> travail = () -> {
                    depart.await();
                    return revoir.revoir(id, issue, acteur, ADMIN);
                };
                futurs.add(fils.submit(travail));
            }
            depart.countDown();
            List<Boolean> issues = new ArrayList<>();
            for (Future<Boolean> futur : futurs) {
                issues.add(futur.get());
            }
            return issues;
        } finally {
            fils.shutdownNow();
        }
    }
}
