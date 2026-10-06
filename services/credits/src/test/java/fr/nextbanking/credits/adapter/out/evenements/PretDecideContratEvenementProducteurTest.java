package fr.nextbanking.credits.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.domain.Notification;
import fr.nextbanking.credits.domain.PretDecide;
import fr.nextbanking.credits.domain.StatutPret;
import fr.nextbanking.credits.domain.TypeNotification;
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
 * Contrat producteur : l'enveloppe {@code pret-decide} a exactement les champs du schéma, les valeurs fixées, et
 * l'exemple du schéma est ce que le producteur écrit.
 */
@Tag("FR-024-06")
class PretDecideContratEvenementProducteurTest {

    private static final Path SCHEMA = Path.of("../../contrats/evenements/credits.pret-decide.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JsonNode produit = json.readTree(new EvenementsSortantsKafka(null, null, json).enJson(
            new PretDecide(UUID.fromString("7a1d2c3b-4e5f-4a6b-8c7d-9e0f1a2b3c4d"), 42L, 5L, StatutPret.EN_COURS,
                    new Notification(5L, TypeNotification.CONFIRMATION, "Demande de prêt approuvée",
                            "Votre demande de pret Consommation de 10000.00 EUR a ete approuvee par le service credit."),
                    Instant.parse("2026-10-06T08:30:01Z"))));

    @Test
    void l_enveloppe_a_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
        assertThat(produit.has("__TypeId__")).isFalse();
    }

    @Test
    void les_donnees_et_la_notification_ont_exactement_les_champs_du_schema() {
        JsonNode donnees = schema.get("properties").get("donnees");
        JsonNode notification = donnees.get("properties").get("notification");

        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
        assertThat(champs(produit.get("donnees").get("notification"))).isEqualTo(requis(notification))
                .isEqualTo(proprietes(notification));
        assertThat(produit.get("donnees").get("pretId").isIntegralNumber()).isTrue();
        assertThat(produit.get("donnees").get("clientId").isIntegralNumber()).isTrue();
    }

    @Test
    void les_valeurs_fixees_par_le_schema_sont_respectees() {
        JsonNode proprietes = schema.get("properties");
        JsonNode donnees = proprietes.get("donnees").get("properties");

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(UUID.fromString(produit.get("idEvenement").asString())).isNotNull();
        assertThat(Instant.parse(produit.get("horodatage").asString())).isEqualTo(Instant.parse("2026-10-06T08:30:01Z"));
        assertThat(valeurs(donnees.get("statut").get("enum"))).containsExactlyInAnyOrder("EN_COURS", "REFUSE");
        assertThat(valeurs(donnees.get("notification").get("properties").get("type").get("enum")))
                .containsExactlyInAnyOrder("CONFIRMATION", "ALERTE");
    }

    @Test
    void l_exemple_du_schema_est_celui_que_le_producteur_ecrit() {
        assertThat(produit).isEqualTo(schema.get("examples").get(0));
    }

    private static Set<String> valeurs(JsonNode liste) {
        Set<String> valeurs = new HashSet<>();
        liste.forEach(valeur -> valeurs.add(valeur.asString()));
        return valeurs;
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
