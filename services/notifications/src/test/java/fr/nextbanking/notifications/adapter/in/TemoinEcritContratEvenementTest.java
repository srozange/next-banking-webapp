package fr.nextbanking.notifications.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat consommateur : l'exemple du schéma du producteur se lit et produit l'effet attendu.
 */
class TemoinEcritContratEvenementTest {

    private static final Path SCHEMA = Path.of("../../contrats/evenements/coeur-bancaire.temoin-ecrit.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void l_exemple_du_schema_est_lu_et_transmis_au_cas_d_usage() {
        JsonNode exemple = json.readTree(SCHEMA.toFile()).get("examples").get(0);
        List<String> recus = new ArrayList<>();
        RecepteurTemoin recepteur = new RecepteurTemoin((idEvenement, temoinId) -> {
            recus.add(idEvenement + " " + temoinId);
            return true;
        }, json);

        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(recus).containsExactly(UUID.fromString(exemple.get("idEvenement").asString()) + " "
                + exemple.get("donnees").get("temoinId").asLong());
    }
}
