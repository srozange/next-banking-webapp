package fr.nextbanking.coeurbancaire.adapter.out.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.NotificationVirement;
import fr.nextbanking.coeurbancaire.domain.TypeNotification;
import fr.nextbanking.coeurbancaire.domain.VirementDecide;
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
 * Contrat producteur de {@code coeur-bancaire.virement-decide.v1} : l'enveloppe produite, bloc {@code notification}
 * compris, a exactement les champs du schéma, avec les valeurs fixées, et reproduit son exemple.
 */
@Tag("FR-002-10")
@Tag("FR-003-14")
class VirementDecideContratEvenementTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode schema = json.readTree(SCHEMA.toFile());
    private final JsonNode produit = json.readTree(new EvenementsSortantsKafka(null, null, json).enJson(
            new VirementDecide(UUID.fromString("6a9b2c3d-4e5f-4a6b-b7c8-9d0e1f2a3b4c"), 12L, 1L, EtatVirement.EXECUTE,
                    new NotificationVirement(1L, TypeNotification.CONFIRMATION, null,
                            "Virement de 100.00 EUR vers le compte FR76-0001-0002 confirme (motif : loyer)"),
                    Instant.parse("2026-11-17T09:00:00Z"))));

    @Test
    void l_enveloppe_les_donnees_et_la_notification_ont_exactement_les_champs_du_schema() {
        assertThat(champs(produit)).isEqualTo(requis(schema)).isEqualTo(proprietes(schema));
        JsonNode donnees = schema.get("properties").get("donnees");
        assertThat(champs(produit.get("donnees"))).isEqualTo(requis(donnees)).isEqualTo(proprietes(donnees));
        JsonNode notification = donnees.get("properties").get("notification");
        assertThat(champs(produit.get("donnees").get("notification"))).isEqualTo(requis(notification))
                .isEqualTo(proprietes(notification));
        assertThat(produit.get("donnees").get("virementId").isIntegralNumber()).isTrue();
        assertThat(produit.get("donnees").get("notification").get("titre").isNull()).isTrue();
    }

    @Test
    void les_valeurs_fixees_par_le_schema_sont_respectees() {
        JsonNode proprietes = schema.get("properties");

        assertThat(produit.get("type")).isEqualTo(proprietes.get("type").get("const"));
        assertThat(produit.get("version")).isEqualTo(proprietes.get("version").get("const"));
        assertThat(produit.get("producteur")).isEqualTo(proprietes.get("producteur").get("const"));
        assertThat(proprietes.get("donnees").get("properties").get("etat").get("enum").get(0).asString())
                .isEqualTo(produit.get("donnees").get("etat").asString());
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

    @Test
    @Tag("FR-027-04")
    void tous_les_etats_et_types_produits_sont_dans_les_enumerations_du_schema() {
        JsonNode proprietes = schema.get("properties").get("donnees").get("properties");
        Set<String> etats = new HashSet<>();
        proprietes.get("etat").get("enum").forEach(etat -> etats.add(etat.asString()));
        Set<String> types = new HashSet<>();
        proprietes.get("notification").get("properties").get("type").get("enum")
                .forEach(type -> types.add(type.asString()));
        EvenementsSortantsKafka kafka = new EvenementsSortantsKafka(null, null, json);

        JsonNode suspendu = json.readTree(kafka.enJson(new VirementDecide(UUID.randomUUID(), 13L, 1L,
                EtatVirement.SUSPENDU, new NotificationVirement(1L, TypeNotification.FRAUDE, null,
                        "Votre virement de 10000.00 EUR vers le compte FR76-0001-0002 est en cours de vérification."),
                Instant.parse("2026-11-17T09:00:00Z"))));

        assertThat(etats).containsExactlyInAnyOrder("EXECUTE", "SUSPENDU");
        assertThat(types).containsExactlyInAnyOrder("CONFIRMATION", "ALERTE", "FRAUDE");
        assertThat(etats).contains(suspendu.get("donnees").get("etat").asString());
        assertThat(types).contains(suspendu.get("donnees").get("notification").get("type").asString());
        assertThat(suspendu.get("donnees").get("notification").get("titre").isNull()).isTrue();
    }
}
