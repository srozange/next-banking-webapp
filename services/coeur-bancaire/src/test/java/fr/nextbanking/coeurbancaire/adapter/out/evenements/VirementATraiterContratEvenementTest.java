package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import fr.nextbanking.coeurbancaire.domain.Motif;
import fr.nextbanking.coeurbancaire.domain.CompteId;
import fr.nextbanking.coeurbancaire.domain.Montant;
import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.VirementATraiter;
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
 * Contrat producteur de {@code coeur-bancaire.virement-a-traiter.v1} : l'enveloppe produite a exactement les champs
 * du schéma, avec les valeurs fixées, et reproduit son exemple.
 */
@Tag("FR-023-01")
class VirementATraiterContratEvenementTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/coeur-bancaire.virement-a-traiter.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JsonNode produit = json.readTree(new EvenementsSortantsKafka(null, null, json).enJson(
            new VirementATraiter(UUID.fromString("5e8f1a2b-3c4d-4e5f-a6b7-8c9d0e1f2a3b"), 12L,
                    Montant.lire("100.00"), 1L, new CompteId(2), Motif.lire("loyer"), Instant.parse("2026-11-17T09:00:00Z"))));

    @Test
    void l_enveloppe_et_les_donnees_ont_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
        JsonNode donnees = schema.get("properties").get("donnees");
        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
        assertThat(produit.get("donnees").get("montant").isString()).isTrue();
        assertThat(produit.get("donnees").get("virementId").isIntegralNumber()).isTrue();
    }

    @Test
    void les_valeurs_fixees_par_le_schema_sont_respectees() {
        JsonNode proprietes = schema.get("properties");

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
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
