package fr.nextbanking.documents.adapter.in.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.documents.application.port.in.RecevoirCompteModifie.Issue;
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.TypeCompte;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat consommateur de {@code coeur-bancaire.compte-modifie.v1} : l'exemple du schéma du producteur se lit, ses
 * huit champs sont confiés au cas d'usage ; un type ou une version inattendus sont ignorés.
 */
@ExtendWith(OutputCaptureExtension.class)
class CompteModifieContratEvenementConsommateurTest {

    private static final Path SCHEMA = Path.of("../../contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode exemple = json.readTree(SCHEMA.toFile()).get("examples").get(0);
    private final List<CompteProjete> recus = new ArrayList<>();
    private final List<UUID> identifiants = new ArrayList<>();
    private Issue issue = Issue.ENREGISTRE;
    private final RecepteurCompteModifie recepteur = new RecepteurCompteModifie((idEvenement, compte) -> {
        identifiants.add(idEvenement);
        recus.add(compte);
        return issue;
    }, json);

    @Test
    void l_exemple_du_schema_est_lu_avec_ses_huit_champs() {
        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(identifiants).containsExactly(UUID.fromString(exemple.get("idEvenement").asString()));
        assertThat(recus).containsExactly(new CompteProjete(1L, 1L, "FR76-0001-0001", TypeCompte.COURANT,
                "FR76-0001-0001", "DEMOFR21XXX", new BigDecimal("2030.00"), 2L));
        assertThat(recus.getFirst().solde().scale()).isEqualTo(2);
    }

    @Test
    void ignore_un_type_ou_une_version_inattendus(CapturedOutput sortie) {
        recepteur.recevoir(json.writeValueAsString(exemple).replace("compte-modifie\"", "autre\""));
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
        assertThat(sortie.getOut()).doesNotContain("FR76-0001-0001");
    }
}
