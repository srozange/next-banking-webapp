package fr.nextbanking.bourse.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import fr.nextbanking.bourse.application.port.out.Cotations;
import fr.nextbanking.bourse.application.port.out.Horloge;
import fr.nextbanking.bourse.domain.TypeSerie;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Choix de l'adaptateur et validation des réglages au démarrage, sans base ni réseau.
 */
class ConfigurationAlphaVantageTest {

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withBean(Horloge.class, () -> AlphaVantageSimuleTest.horloge(LocalDate.of(2026, 11, 17)))
            .withUserConfiguration(ConfigurationAlphaVantage.class);

    @Test
    @Tag("FR-029-05")
    void sans_aucune_propriete_nb_fournisseurs_l_adaptateur_simule_est_actif() {
        contexte.run(demarre -> {
            assertThat(demarre).hasNotFailed();
            assertThat(demarre.getBean(Cotations.class)).isInstanceOf(AlphaVantageSimule.class);
        });
    }

    @Test
    void le_mode_reel_active_l_adaptateur_reel() {
        contexte.withPropertyValues("nb.fournisseurs.alphavantage.mode=reel",
                        "nb.fournisseurs.alphavantage.delai-attente=2s")
                .run(demarre -> assertThat(demarre.getBean(Cotations.class)).isInstanceOf(AlphaVantageCotations.class));
    }

    @Test
    @Tag("FR-029-04")
    void en_mode_reel_l_adresse_et_la_cle_sont_lues_dans_les_proprietes_systeme_au_demarrage() {
        assumeTrue(System.getenv(ParametresDeploiement.ADRESSE) == null
                && System.getenv(ParametresDeploiement.CLE) == null, "variables d'environnement AlphaVantage posées");
        BouchonAlphaVantage bouchon = new BouchonAlphaVantage();
        try {
            System.setProperty(ParametresDeploiement.ADRESSE, bouchon.adresse());
            System.setProperty(ParametresDeploiement.CLE, "cle-de-test");
            contexte.withPropertyValues("nb.fournisseurs.alphavantage.mode=reel").run(demarre -> {
                assertThat(demarre.getBean(Cotations.class).serie("AI.PA", TypeSerie.JOURNALIERE)).isNotEmpty();
                assertThat(bouchon.requetes()).singleElement().satisfies(requete ->
                        assertThat(requete.split("&")).contains("symbol=AI.PA", "apikey=cle-de-test"));
            });
        } finally {
            System.clearProperty(ParametresDeploiement.ADRESSE);
            System.clearProperty(ParametresDeploiement.CLE);
            bouchon.close();
        }
    }

    @Test
    void une_valeur_invalide_empeche_le_demarrage() {
        for (String reglage : new String[] {"mode=inconnu", "delai-attente=0s", "delai-attente=-1s",
                "seuil-echecs=0", "duree-ouverture=0s", "duree-cache=-5m"}) {
            contexte.withPropertyValues("nb.fournisseurs.alphavantage." + reglage)
                    .run(demarre -> assertThat(demarre).as(reglage).hasFailed());
        }
    }
}
