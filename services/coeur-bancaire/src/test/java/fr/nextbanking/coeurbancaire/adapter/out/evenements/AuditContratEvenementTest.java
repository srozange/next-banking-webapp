package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.EntreeAudit;
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
 * Contrat producteur de {@code coeur-bancaire.audit.v1} : l'enveloppe produite a exactement les champs du schéma, ses
 * valeurs fixées, un code du catalogue fermé, et reproduit son exemple.
 */
@Tag("FR-026-03")
class AuditContratEvenementTest {

    private static final Path SCHEMA = Path.of("../../contrats/evenements/coeur-bancaire.audit.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JsonNode produit = json.readTree(new JournalAuditKafka(null, null, json).enJson(
            new EntreeAudit(UUID.fromString("9c4d7e21-6a3b-4f80-b1c2-5d6e7f8a9b0c"), "1", ActionAudit.VIREMENT,
                    "203.0.113.7", Instant.parse("2026-11-17T09:00:00Z"),
                    "Montant: 100.00 - Motif: loyer - Issue: EXECUTE", 12)));

    @Test
    void l_enveloppe_et_les_donnees_ont_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
        JsonNode donnees = schema.get("properties").get("donnees");
        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
    }

    @Test
    void les_valeurs_fixees_et_le_catalogue_du_schema_sont_respectes() {
        JsonNode proprietes = schema.get("properties");
        Set<String> catalogue = new HashSet<>();
        proprietes.get("donnees").get("properties").get("action").get("enum")
                .forEach(code -> catalogue.add(code.asString()));

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(catalogue).contains(produit.get("donnees").get("action").asString())
                .contains("VIREMENT", "TRANSFER_EXECUTED");
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
