package fr.nextbanking.coeurbancaire.adapter.in.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.application.port.in.RecevoirClientModifie.Issue;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat consommateur de {@code identite-clients.client-modifie.v1} (test {@code *ContratEvenement*} du consommateur) :
 * l'exemple du schéma du producteur se lit, son nom complet, son conseiller (nul accepté), sa suppression et sa
 * {@code versionAgregat} sont confiés au cas d'usage ;
 * un type ou une version inattendus sont ignorés.
 */
@ExtendWith(OutputCaptureExtension.class)
@Tag("FR-016-19")
class RecepteurClientModifieTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/identite-clients.client-modifie.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode exemple = json.readTree(SCHEMA.toFile()).get("examples").get(0);
    private final List<String> recus = new ArrayList<>();
    private Issue issue = Issue.ENREGISTRE;
    private final RecepteurClientModifie recepteur = new RecepteurClientModifie(
            (idEvenement, client) -> {
                recus.add(idEvenement + "|" + client.clientId() + "|" + client.nomComplet() + "|"
                        + client.conseillerId() + "|" + client.supprime() + "|" + client.versionAgregat());
                return issue;
            }, json);

    @Test
    void l_exemple_du_schema_est_lu_et_son_nom_complet_confie_au_cas_d_usage() {
        JsonNode donnees = exemple.get("donnees");

        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(recus).containsExactly(exemple.get("idEvenement").asString() + "|" + donnees.get("clientId").asLong()
                + "|" + donnees.get("nomComplet").asString() + "|" + donnees.get("conseillerId").asLong() + "|"
                + donnees.get("supprime").asBoolean() + "|" + donnees.get("versionAgregat").asLong());
    }

    @Test
    @Tag("FR-015-02")
    void le_conseiller_et_la_suppression_sont_lus_et_un_conseiller_nul_est_accepte() {
        recepteur.recevoir(json.writeValueAsString(exemple)
                .replace("\"conseillerId\":11", "\"conseillerId\":null")
                .replace("\"supprime\":false", "\"supprime\":true"));
        recepteur.recevoir(json.writeValueAsString(exemple).replace("\"conseillerId\":11", "\"conseillerId\":12"));

        assertThat(recus).hasSize(2);
        assertThat(recus.get(0)).endsWith("|Jean Dupont|null|true|2");
        assertThat(recus.get(1)).endsWith("|Jean Dupont|12|false|2");
    }

    @Test
    void ignore_un_type_ou_une_version_inattendus(CapturedOutput sortie) {
        recepteur.recevoir(json.writeValueAsString(exemple).replace("client-modifie\"", "autre\""));
        recepteur.recevoir(json.writeValueAsString(exemple).replace("\"version\":1,", "\"version\":2,"));

        assertThat(recus).isEmpty();
        assertThat(sortie.getOut()).contains("inattendus");
    }

    @Test
    void journalise_un_message_deja_traite_ou_une_version_ancienne(CapturedOutput sortie) {
        issue = Issue.DEJA_TRAITE;
        recepteur.recevoir(json.writeValueAsString(exemple));
        issue = Issue.IGNORE_ANCIEN;
        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(recus).hasSize(2);
        assertThat(sortie.getOut()).contains("déjà traité", "ancienne");
    }
}
