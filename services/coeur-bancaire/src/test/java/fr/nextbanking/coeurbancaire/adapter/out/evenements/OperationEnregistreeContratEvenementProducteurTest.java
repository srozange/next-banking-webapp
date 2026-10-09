package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Operation;
import fr.nextbanking.coeurbancaire.domain.OperationEnregistree;
import fr.nextbanking.coeurbancaire.domain.TypeOperation;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat producteur de {@code coeur-bancaire.operation-enregistree.v1} : l'enveloppe produite a exactement les champs du
 * schéma, avec les valeurs fixées, l'IBAN égal au numéro, le solde en chaîne à deux décimales, et reproduit son
 * exemple.
 */
class OperationEnregistreeContratEvenementProducteurTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/coeur-bancaire.operation-enregistree.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JsonNode produit = json.readTree(new EvenementsSortantsKafka(null, null, json).enJson(
            new OperationEnregistree(UUID.fromString("9c1d2e3f-4a5b-4c6d-8e7f-0a1b2c3d4e5f"), 42L,
                    new Operation(1L, TypeOperation.DEBIT, Montant.lire("100.00"), Montant.lire("2030.00"),
                            "Virement vers FR76-0001-0002", Instant.parse("2026-11-17T09:00:00Z"), 7L),
                    2L, Instant.parse("2026-11-17T09:00:00Z"))));

    @Test
    void l_enveloppe_et_les_donnees_ont_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
        JsonNode donnees = schema.get("properties").get("donnees");
        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
        assertThat(produit.get("donnees").get("operationId").isIntegralNumber()).isTrue();
        assertThat(produit.get("donnees").get("compteId").isIntegralNumber()).isTrue();
        assertThat(produit.get("donnees").get("versionAgregat").isIntegralNumber()).isTrue();
        assertThat(produit.get("donnees").get("montant").isString()).isTrue();
        assertThat(produit.get("donnees").get("soldeApres").isString()).isTrue();
    }

    @Test
    void les_valeurs_fixees_par_le_schema_sont_respectees() {
        JsonNode proprietes = schema.get("properties");
        JsonNode donnees = proprietes.get("donnees").get("properties");

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(produit.get("donnees").get("montant").asString()).matches(donnees.get("montant").get("pattern").asString());
        assertThat(produit.get("donnees").get("soldeApres").asString())
                .matches(donnees.get("soldeApres").get("pattern").asString());
        Set<String> types = new HashSet<>();
        donnees.get("type").get("enum").forEach(type -> types.add(type.asString()));
        assertThat(types).containsExactlyInAnyOrder("CREDIT", "DEBIT");
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
