package fr.nextbanking.notifications.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat consommateur : l'exemple du schéma du producteur se lit et la notification rendue est transmise telle
 * quelle au cas d'usage.
 */
@Tag("FR-002-09")
@Tag("BUG-007")
class VirementDecideContratEvenementTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void l_exemple_du_schema_est_lu_et_transmis_au_cas_d_usage() {
        JsonNode exemple = json.readTree(SCHEMA.toFile()).get("examples").get(0);
        JsonNode donnees = exemple.get("donnees");
        JsonNode notification = donnees.get("notification");
        List<String> recus = new ArrayList<>();
        RecepteurVirementDecide recepteur = new RecepteurVirementDecide((idEvenement, virementId, rendue) -> {
            recus.add(idEvenement + "|" + virementId + "|" + rendue.clientId() + "|" + rendue.type() + "|"
                    + rendue.titre() + "|" + rendue.message());
            return true;
        }, json);

        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(recus).containsExactly(exemple.get("idEvenement").asString() + "|"
                + donnees.get("virementId").asLong() + "|" + notification.get("clientId").asLong() + "|"
                + notification.get("type").asString() + "|null|" + notification.get("message").asString());
    }
}
