package fr.nextbanking.coeurbancaire.adapter.out.persistance;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.adapter.DonneesDeReference;
import fr.nextbanking.coeurbancaire.application.port.out.Plafonds;
import fr.nextbanking.coeurbancaire.domain.CategorieBudget;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Plafond;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import fr.nextbanking.coeurbancaire.infrastructure.ApplicationCoeurBancaire;
import fr.nextbanking.coeurbancaire.infrastructure.Conteneurs;
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

/** Décompte des plafonds sur une vraie base : par client et par mois. */
@SpringBootTest(classes = ApplicationCoeurBancaire.class)
@DirtiesContext
@Tag("FR-006-01")
@Tag("FR-006-04")
@Tag("FR-006-07")
@Tag("FR-006-08")
@Tag("FR-006-11")
class PlafondsJdbcIT {

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registre) {
        Conteneurs.proprietes(registre);
    }

    @Autowired
    private Plafonds plafonds;
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

    private void plafond(long clientId, String categorie, String mois) {
        jdbc.sql("insert into plafonds_budget (client_id, categorie, mois, plafond) values (:c, :k, :m, 500.00)")
                .param("c", clientId).param("k", categorie).param("m", mois).update();
    }

    @Test
    void seuls_les_plafonds_du_client_et_du_mois_sont_comptes() {
        plafond(1, "ALIMENTATION", "2026-03");
        plafond(1, "LOGEMENT", "2026-03");
        plafond(1, "LOGEMENT", "2026-04");
        plafond(8, "LOGEMENT", "2026-03");

        assertThat(plafonds.compter(1, "2026-03")).isEqualTo(2);
        assertThat(plafonds.compter(1, "2026-04")).isEqualTo(1);
        assertThat(plafonds.compter(1, "2026-05")).isZero();
        assertThat(plafonds.compter(10, "2026-03")).isZero();
    }

    @Test
    void le_plafond_enregistre_deux_fois_reste_unique_avec_la_derniere_valeur() {
        Plafond premier = plafonds.enregistrer(1, CategorieBudget.ALIMENTATION, "2026-03", Montant.lire("500"));
        Plafond second = plafonds.enregistrer(1, CategorieBudget.ALIMENTATION, "2026-03", Montant.lire("300"));
        plafonds.enregistrer(8, CategorieBudget.ALIMENTATION, "2026-03", Montant.lire("1"));

        assertThat(second.id()).isEqualTo(premier.id());
        assertThat(plafonds.compter(1, "2026-03")).isEqualTo(1);
        List<Plafond> duMois = plafonds.duMois(1, "2026-03");
        assertThat(duMois).singleElement().satisfies(p -> {
            assertThat(p.montant().texte()).isEqualTo("300.00");
            assertThat(p.categorie()).isEqualTo(CategorieBudget.ALIMENTATION);
            assertThat(p.mois()).isEqualTo("2026-03");
        });
        assertThat(plafonds.duMois(1, "2026-04")).isEmpty();
    }

    @Test
    void des_enregistrements_concurrents_ne_laissent_qu_un_plafond() throws Exception {
        ExecutorService executeur = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Plafond>> taches = new java.util.ArrayList<>();
            for (int i = 1; i <= 16; i++) {
                Montant montant = Montant.lire(Integer.toString(i * 10));
                taches.add(() -> plafonds.enregistrer(1, CategorieBudget.LOGEMENT, "2026-03", montant));
            }
            for (Future<Plafond> resultat : executeur.invokeAll(taches)) {
                resultat.get();
            }
        } finally {
            executeur.shutdownNow();
        }

        assertThat(plafonds.compter(1, "2026-03")).isEqualTo(1);
    }

    @Test
    void la_suppression_filtre_sur_le_client() {
        Plafond plafond = plafonds.enregistrer(1, CategorieBudget.LOGEMENT, "2026-03", Montant.lire("500"));

        assertThat(plafonds.retirer(8, plafond.id())).isFalse();
        assertThat(plafonds.compter(1, "2026-03")).isEqualTo(1);
        assertThat(plafonds.retirer(1, plafond.id())).isTrue();
        assertThat(plafonds.retirer(1, plafond.id())).isFalse();
        assertThat(plafonds.compter(1, "2026-03")).isZero();
    }
}
