package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Choix de l'adaptateur et validation des réglages au démarrage, sans base ni réseau. */
class ConfigurationPowensTest {

    private final ApplicationContextRunner contexte = new ApplicationContextRunner()
            .withUserConfiguration(ConfigurationPowens.class);

    @Test
    @Tag("FR-028-01")
    @Tag("FR-012-05")
    void sans_aucune_propriete_nb_fournisseurs_l_adaptateur_simule_est_actif() {
        contexte.run(demarre -> {
            assertThat(demarre).hasNotFailed();
            assertThat(demarre.getBean(Powens.class)).isInstanceOf(PowensSimule.class);
        });
    }

    @Test
    @Tag("FR-028-01")
    void le_mode_reel_avec_un_secret_active_l_adaptateur_reel() {
        assumeTrue(System.getenv(ParametresDeploiement.SECRET) == null, "secret Powens posé dans l'environnement");
        System.setProperty(ParametresDeploiement.SECRET, "secret-de-test");
        try {
            contexte.withPropertyValues("nb.fournisseurs.powens.mode=reel")
                    .run(demarre -> assertThat(demarre.getBean(Powens.class)).isInstanceOf(PowensReel.class));
        } finally {
            System.clearProperty(ParametresDeploiement.SECRET);
        }
    }

    @Test
    @Tag("FR-028-12")
    void le_mode_reel_sans_secret_client_empeche_le_demarrage() {
        assumeTrue(System.getenv(ParametresDeploiement.SECRET) == null, "secret Powens posé dans l'environnement");
        contexte.withPropertyValues("nb.fournisseurs.powens.mode=reel").run(demarre -> {
            assertThat(demarre).hasFailed();
            assertThat(demarre.getStartupFailure()).rootCause().isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("POWENS_CLIENT_SECRET");
        });
    }

    @Test
    @Tag("FR-028-12")
    void une_valeur_invalide_empeche_le_demarrage() {
        for (String reglage : new String[] {"mode=inconnu", "delai-attente=0s", "seuil-echecs=0",
                "duree-ouverture=-5s", "domaine=", "banques= "}) {
            contexte.withPropertyValues("nb.fournisseurs.powens." + reglage)
                    .run(demarre -> assertThat(demarre).as(reglage).hasFailed());
        }
    }

    @Test
    @Tag("FR-028-12")
    void le_client_http_applique_le_delai_a_la_connexion_et_a_la_lecture() {
        var fabrique = ConfigurationPowens.fabrique(java.time.Duration.ofSeconds(15));

        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(fabrique, "readTimeout"))
                .isEqualTo(java.time.Duration.ofSeconds(15));
        assertThat(((java.net.http.HttpClient) org.springframework.test.util.ReflectionTestUtils
                .getField(fabrique, "httpClient")).connectTimeout()).contains(java.time.Duration.ofSeconds(15));
    }
}
