package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-028-12")
class ParametresDeploiementTest {

    private static UnaryOperator<String> source(Map<String, String> valeurs) {
        return valeurs::get;
    }

    @Test
    void l_environnement_l_emporte_sur_les_proprietes_systeme() {
        ParametresDeploiement parametres = new ParametresDeploiement(
                source(Map.of("POWENS_CLIENT_SECRET", "env")), source(Map.of("POWENS_CLIENT_SECRET", "systeme")));

        assertThat(parametres.secret()).contains("env");
    }

    @Test
    void une_valeur_blanche_vaut_absence_et_la_propriete_systeme_prend_le_relais() {
        ParametresDeploiement parametres = new ParametresDeploiement(
                source(Map.of("POWENS_CLIENT_SECRET", "  ", "POWENS_URL_RETOUR", "")),
                source(Map.of("POWENS_CLIENT_SECRET", " systeme ", "POWENS_DOMAINE", "d.example")));

        assertThat(parametres.secret()).contains("systeme");
        assertThat(parametres.urlRetour()).isEmpty();
        assertThat(parametres.domaine()).contains("d.example");
        assertThat(parametres.clientId()).isEmpty();
        assertThat(parametres.banques()).isEmpty();
    }

    @Test
    void sans_source_aucun_secret() {
        assertThat(new ParametresDeploiement(source(Map.of()), source(Map.of())).secret()).isEmpty();
    }

    @Test
    void les_sources_par_defaut_sont_l_environnement_et_les_proprietes_du_processus() {
        System.setProperty("POWENS_CONNECTOR_ID", "12,13");
        try {
            assertThat(new ParametresDeploiement().banques())
                    .isEqualTo(java.util.Optional.ofNullable(System.getenv("POWENS_CONNECTOR_ID"))
                            .filter(v -> !v.isBlank()).map(String::trim).or(() -> java.util.Optional.of("12,13")));
        } finally {
            System.clearProperty("POWENS_CONNECTOR_ID");
        }
    }
}
