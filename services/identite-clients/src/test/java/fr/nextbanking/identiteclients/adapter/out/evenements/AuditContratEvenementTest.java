package fr.nextbanking.identiteclients.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.identiteclients.domain.ActionAudit;
import fr.nextbanking.identiteclients.domain.EntreeAudit;
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
 * Contrat producteur : l'enveloppe d'audit produite a exactement les champs requis du schéma, avec les valeurs
 * fixées ; l'horodatage est celui lu sur l'Horloge (fixe ici).
 */
@Tag("FR-026-01")
@Tag("FR-001-12")
class AuditContratEvenementTest {

    private static final Path SCHEMA = Path.of("../../contrats/evenements/identite-clients.audit.v1.schema.json");
    private static final UUID ID = UUID.fromString("7b1e4c2a-5d3f-4e6a-8b9c-0d1e2f3a4b5c");
    private static final Instant HORODATAGE = Instant.parse("2026-10-05T08:30:00Z");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JournalAuditKafka journal = new JournalAuditKafka(null, null, json);
    private final JsonNode produit = json.readTree(journal.enJson(
            EntreeAudit.connexionReussie(1L, "203.0.113.7", ID, HORODATAGE)));

    @Test
    void l_enveloppe_a_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
    }

    @Test
    void les_donnees_ont_exactement_les_champs_du_schema() {
        JsonNode donnees = schema.get("properties").get("donnees");

        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
    }

    @Test
    void les_valeurs_fixees_par_le_schema_sont_respectees() {
        JsonNode proprietes = schema.get("properties");

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(Instant.parse(produit.get("horodatage").asString())).isEqualTo(HORODATAGE);
        assertThat(produit.get("horodatage").asString()).endsWith("Z");
    }

    @Test
    void chaque_code_produit_appartient_au_catalogue_ferme_du_schema() {
        Set<String> catalogue = new HashSet<>();
        schema.get("properties").get("donnees").get("properties").get("action").get("enum")
                .forEach(code -> catalogue.add(code.asString()));

        assertThat(catalogue).hasSize(13);
        for (ActionAudit action : ActionAudit.values()) {
            assertThat(catalogue).contains(action.name());
        }
    }

    @Test
    void une_tentative_echouee_porte_l_identifiant_saisi_et_une_adresse_absente_est_ecrite_null() {
        JsonNode echec = json.readTree(journal.enJson(
                EntreeAudit.tentativeEchouee("retro-compte-inexistant", null, ID, HORODATAGE))).get("donnees");

        assertThat(echec.get("utilisateur").asString()).isEqualTo("retro-compte-inexistant");
        assertThat(echec.get("action").asString()).isEqualTo("LOGIN_FAILED");
        assertThat(echec.get("detail").asString()).isEqualTo("Tentative échouée");
        assertThat(echec.has("adresseIp")).isTrue();
        assertThat(echec.get("adresseIp").isNull()).isTrue();
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
