package fr.nextbanking.credits.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.domain.PretDemande;
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
 * Contrat producteur : l'enveloppe produite a exactement les champs requis du schéma, avec les valeurs fixées, et
 * l'exemple du schéma est ce que le producteur écrit.
 */
@Tag("FR-024-01")
class PretDemandeContratEvenementProducteurTest {

    private static final Path SCHEMA = Path.of("../../contrats/evenements/credits.pret-demande.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JsonNode produit = json.readTree(new EvenementsSortantsKafka(null, null, json).enJson(
            new PretDemande(UUID.fromString("3f2c1a9e-8b7d-4c6e-9f10-2a3b4c5d6e7f"), 42L,
                    Instant.parse("2026-10-06T08:30:00Z"))));

    @Test
    void l_enveloppe_a_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
        assertThat(produit.has("__TypeId__")).isFalse();
    }

    @Test
    void les_donnees_ont_exactement_les_champs_du_schema() {
        JsonNode donnees = schema.get("properties").get("donnees");

        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
        assertThat(produit.get("donnees").get("pretId").isIntegralNumber()).isTrue();
    }

    @Test
    void les_valeurs_fixees_par_le_schema_sont_respectees() {
        JsonNode proprietes = schema.get("properties");

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(UUID.fromString(produit.get("idEvenement").asString())).isNotNull();
        assertThat(Instant.parse(produit.get("horodatage").asString())).isEqualTo(Instant.parse("2026-10-06T08:30:00Z"));
        assertThat(schema.get("properties").get("donnees").get("properties").get("pretId").get("minimum").asInt())
                .isEqualTo(1);
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
