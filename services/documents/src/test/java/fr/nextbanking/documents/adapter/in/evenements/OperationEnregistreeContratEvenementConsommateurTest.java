package fr.nextbanking.documents.adapter.in.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.documents.application.port.in.RecevoirOperationEnregistree.Issue;
import fr.nextbanking.documents.domain.OperationProjetee;
import fr.nextbanking.documents.domain.TypeOperation;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrat consommateur de {@code coeur-bancaire.operation-enregistree.v1} : l'exemple du schéma du producteur se lit,
 * ses huit champs sont confiés au cas d'usage ; un type ou une version inattendus sont ignorés ; le libellé n'est
 * jamais journalisé.
 */
@Tag("FR-017-04")
@ExtendWith(OutputCaptureExtension.class)
class OperationEnregistreeContratEvenementConsommateurTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/coeur-bancaire.operation-enregistree.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode exemple = json.readTree(SCHEMA.toFile()).get("examples").get(0);
    private final List<OperationProjetee> recues = new ArrayList<>();
    private final List<UUID> identifiants = new ArrayList<>();
    private Issue issue = Issue.ENREGISTRE;
    private final RecepteurOperationEnregistree recepteur = new RecepteurOperationEnregistree((idEvenement, op) -> {
        identifiants.add(idEvenement);
        recues.add(op);
        return issue;
    }, json);

    @Test
    void l_exemple_du_schema_est_lu_avec_ses_huit_champs() {
        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(identifiants).containsExactly(UUID.fromString(exemple.get("idEvenement").asString()));
        assertThat(recues).containsExactly(new OperationProjetee(42L, 1L, Instant.parse("2026-11-17T09:00:00Z"),
                "Virement vers FR76-0001-0002", TypeOperation.DEBIT, new BigDecimal("100.00"),
                new BigDecimal("2030.00"), 2L));
        assertThat(recues.getFirst().montant().scale()).isEqualTo(2);
    }

    @Test
    void ignore_un_type_ou_une_version_inattendus(CapturedOutput sortie) {
        recepteur.recevoir(json.writeValueAsString(exemple).replace("operation-enregistree\"", "autre\""));
        recepteur.recevoir(json.writeValueAsString(exemple).replace("\"version\":1,", "\"version\":2,"));

        assertThat(recues).isEmpty();
        assertThat(sortie.getOut()).contains("inattendus");
    }

    @Test
    void journalise_un_message_deja_traite_ou_une_operation_deja_presente_sans_jamais_le_libelle(
            CapturedOutput sortie) {
        issue = Issue.DEJA_TRAITE;
        recepteur.recevoir(json.writeValueAsString(exemple));
        issue = Issue.DEJA_PRESENTE;
        recepteur.recevoir(json.writeValueAsString(exemple));
        issue = Issue.ENREGISTRE;
        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(recues).hasSize(3);
        assertThat(sortie.getOut()).contains("déjà traité", "déjà présente");
        assertThat(sortie.getOut()).doesNotContain("Virement vers", "FR76-0001");
    }
}
