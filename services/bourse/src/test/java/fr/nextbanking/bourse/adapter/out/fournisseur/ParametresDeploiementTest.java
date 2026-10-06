package fr.nextbanking.bourse.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ParametresDeploiementTest {

    @Test
    @Tag("FR-029-04")
    void l_adresse_vient_de_l_environnement_puis_de_la_propriete_systeme_puis_de_la_valeur_par_defaut() {
        String nom = "ALPHA_VANTAGE_BASE_URL";
        assertThat(parametres(Map.of(nom, "http://env/query"), Map.of(nom, "http://systeme/query")).adresse())
                .isEqualTo("http://env/query");
        assertThat(parametres(Map.of(nom, "  "), Map.of(nom, "http://systeme/query")).adresse())
                .isEqualTo("http://systeme/query");
        assertThat(parametres(Map.of(), Map.of(nom, "")).adresse()).isEqualTo("https://www.alphavantage.co/query");
    }

    @Test
    @Tag("FR-029-05")
    @Tag("FR-009-14")
    @Tag("BUG-091")
    @Tag("BUG-034")
    void la_cle_vient_de_l_environnement_puis_de_la_propriete_systeme_sans_valeur_par_defaut() {
        String nom = "ALPHA_VANTAGE_API_KEY";
        assertThat(parametres(Map.of(nom, " cle-env "), Map.of(nom, "cle-systeme")).cle()).contains("cle-env");
        assertThat(parametres(Map.of(), Map.of(nom, "cle-systeme")).cle()).contains("cle-systeme");
        assertThat(parametres(Map.of(nom, ""), Map.of(nom, " ")).cle()).isEmpty();
        assertThat(parametres(Map.of(), Map.of()).cle()).isEmpty();
    }

    @Test
    void lit_par_defaut_l_environnement_et_les_proprietes_systeme_du_processus() {
        String nom = "NB_TEST_PARAMETRE_" + System.nanoTime();
        System.setProperty(nom, "valeur-systeme");
        try {
            assertThat(new ParametresDeploiement().lire(nom)).contains("valeur-systeme");
        } finally {
            System.clearProperty(nom);
        }
    }

    private static ParametresDeploiement parametres(Map<String, String> environnement, Map<String, String> systeme) {
        return new ParametresDeploiement(environnement::get, systeme::get);
    }
}
