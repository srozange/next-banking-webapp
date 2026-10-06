package fr.nextbanking.identiteclients.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Contrat producteur : l'événement d'état du client a exactement les champs du schéma ; un champ retiré fait échouer
 * le test en le nommant.
 */
@Tag("FR-016-11")
class ClientModifieContratEvenementTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/identite-clients.client-modifie.v1.schema.json");
    private static final UUID ID = UUID.fromString("3c9d7e1b-2a4f-4b6c-9d8e-1f0a2b3c4d5e");
    private static final Instant HORODATAGE = Instant.parse("2026-10-06T09:00:00Z");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final PublicationEtatClientKafka publication = new PublicationEtatClientKafka(null, null, json);
    private final JsonNode produit = json.readTree(publication.enJson(ID, HORODATAGE,
            new EnveloppeClientModifie.Donnees(1L, "user1", "Jean Dupont", "CLIENT", false, 11L, "2026-10-06", false,
                    2L)));

    @Test
    void l_enveloppe_a_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
    }

    @Test
    void chaque_champ_de_l_etat_du_client_est_produit_et_decrit_par_le_schema() {
        JsonNode donnees = schema.get("properties").get("donnees");

        for (String champ : requis(donnees)) {
            assertThat(produit.get("donnees").has(champ)).as("champ %s produit", champ).isTrue();
        }
        for (String champ : champs(produit.get("donnees"))) {
            assertThat(proprietes(donnees)).as("champ %s décrit", champ).contains(champ);
        }
        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
        assertThat(requis(donnees)).contains("conseillerId");
    }

    @Test
    void les_valeurs_fixees_par_le_schema_sont_respectees() {
        JsonNode proprietes = schema.get("properties");

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(Instant.parse(produit.get("horodatage").asString())).isEqualTo(HORODATAGE);
    }

    @Test
    void un_client_sans_conseiller_ecrit_conseiller_id_null() {
        JsonNode donnees = json.readTree(publication.enJson(ID, HORODATAGE,
                new EnveloppeClientModifie.Donnees(12L, "marc", "Marc Lefevre", "CONSEILLER", true, null,
                        "2026-10-06", false, 1L))).get("donnees");

        assertThat(donnees.has("conseillerId")).isTrue();
        assertThat(donnees.get("conseillerId").isNull()).isTrue();
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
