package fr.nextbanking.credits.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.domain.ActionAudit;
import fr.nextbanking.credits.domain.EntreeAudit;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat producteur de {@code credits.audit.v1} : l'enveloppe produite a exactement les champs du schéma, ses valeurs
 * fixées, un code du catalogue fermé, et reproduit son exemple.
 */
@Tag("FR-026-02")
class AuditContratEvenementProducteurTest {

    private static final Path SCHEMA = Path.of("../../contrats/evenements/credits.audit.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JsonNode produit = json.readTree(new JournalAuditKafka(null, null, json).enJson(
            EntreeAudit.decision(UUID.fromString("9a2c6e1d-4b7f-4a3e-8c5d-2e1f0a9b8c7d"), "10", true, "203.0.113.7",
                    Instant.parse("2026-10-09T10:15:00Z"), 42L)));

    @Test
    void l_enveloppe_et_les_donnees_ont_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
        JsonNode donnees = schema.get("properties").get("donnees");
        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
    }

    @Test
    void les_valeurs_fixees_et_le_catalogue_ferme_du_schema_sont_respectes() {
        JsonNode proprietes = schema.get("properties");
        Set<String> catalogue = new HashSet<>();
        proprietes.get("donnees").get("properties").get("action").get("enum")
                .forEach(code -> catalogue.add(code.asString()));

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(catalogue).containsExactlyInAnyOrder("ADMIN_LOAN_APPROVE", "ADMIN_LOAN_REFUSE");
        for (ActionAudit action : ActionAudit.values()) {
            assertThat(catalogue).contains(action.name());
        }
    }

    @Test
    void l_exemple_du_schema_est_celui_que_le_producteur_ecrit() {
        assertThat(produit).isEqualTo(schema.get("examples").get(0));
    }

    private static Set<String> champs(JsonNode objet) {
        return new HashSet<>(objet.propertyNames());
    }

    private static Set<String> proprietes(JsonNode schema) {
        return new HashSet<>(schema.get("properties").propertyNames());
    }

    private static Set<String> requis(JsonNode schema) {
        Set<String> requis = new HashSet<>();
        schema.get("required").forEach(champ -> requis.add(champ.asString()));
        return requis;
    }
}
