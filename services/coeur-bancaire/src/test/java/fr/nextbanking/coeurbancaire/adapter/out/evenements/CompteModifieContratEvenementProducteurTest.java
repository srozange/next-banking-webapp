package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.Compte;
import fr.nextbanking.coeurbancaire.domain.CompteModifie;
import fr.nextbanking.coeurbancaire.domain.EtatCompte;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.TypeCompte;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat producteur de {@code coeur-bancaire.compte-modifie.v1} : l'enveloppe produite a exactement les champs du
 * schéma, avec les valeurs fixées, l'IBAN égal au numéro, le solde en chaîne à deux décimales, et reproduit son
 * exemple.
 */
class CompteModifieContratEvenementProducteurTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JsonNode produit = json.readTree(new EvenementsSortantsKafka(null, null, json).enJson(
            new CompteModifie(UUID.fromString("7b8c3d4e-5f60-4b7c-8d9e-0f1a2b3c4d5e"),
                    new EtatCompte(new Compte(1L, 1L, "FR76-0001-0001", TypeCompte.COURANT, Montant.lire("2030.00")),
                            2L),
                    Instant.parse("2026-11-17T09:00:00Z"))));

    @Test
    void l_enveloppe_et_les_donnees_ont_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
        JsonNode donnees = schema.get("properties").get("donnees");
        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
        assertThat(produit.get("donnees").get("compteId").isIntegralNumber()).isTrue();
        assertThat(produit.get("donnees").get("clientId").isIntegralNumber()).isTrue();
        assertThat(produit.get("donnees").get("versionAgregat").isIntegralNumber()).isTrue();
        assertThat(produit.get("donnees").get("solde").isString()).isTrue();
    }

    @Test
    void les_valeurs_fixees_par_le_schema_sont_respectees() {
        JsonNode proprietes = schema.get("properties");
        JsonNode donnees = proprietes.get("donnees").get("properties");

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(produit.get("donnees").get("bic")).isEqualTo(donnees.get("bic").get("const"));
        assertThat(produit.get("donnees").get("iban")).isEqualTo(produit.get("donnees").get("numero"));
        assertThat(produit.get("donnees").get("solde").asString()).matches(donnees.get("solde").get("pattern").asString());
        Set<String> types = new HashSet<>();
        donnees.get("type").get("enum").forEach(type -> types.add(type.asString()));
        assertThat(types).containsExactlyInAnyOrder("COURANT", "EPARGNE");
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
