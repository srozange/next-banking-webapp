package fr.nextbanking.audit.adapter.in.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.audit.application.port.in.EnregistrerEntreeAudit.Issue;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat consommateur : l'exemple du schéma du producteur ({@code identite-clients.audit.v1}) se lit et produit
 * l'effet attendu.
 */
@Tag("FR-026-01")
class AuditContratEvenementTest {

    private static final Path SCHEMA = Path.of("../../contrats/evenements/identite-clients.audit.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void l_exemple_du_schema_est_lu_et_transmis_au_cas_d_usage() {
        JsonNode exemple = json.readTree(SCHEMA.toFile()).get("examples").get(0);
        JsonNode donnees = exemple.get("donnees");
        List<String> recus = new ArrayList<>();
        RecepteurAudit recepteur = new RecepteurAudit((idEvenement, utilisateur, action, detail, adresseIp, h) -> {
            recus.add(idEvenement + "|" + utilisateur + "|" + action + "|" + detail + "|" + adresseIp + "|" + h);
            return Issue.ENREGISTREE;
        }, json);

        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(recus).containsExactly(exemple.get("idEvenement").asString() + "|"
                + donnees.get("utilisateur").asString() + "|" + donnees.get("action").asString() + "|"
                + donnees.get("detail").asString() + "|" + donnees.get("adresseIp").asString() + "|"
                + Instant.parse(exemple.get("horodatage").asString()));
    }
}
