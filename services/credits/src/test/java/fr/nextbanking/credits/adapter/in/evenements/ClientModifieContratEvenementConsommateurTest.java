package fr.nextbanking.credits.adapter.in.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.application.port.in.RecevoirClientModifie.Issue;
import fr.nextbanking.credits.domain.ClientProjete;
import fr.nextbanking.credits.domain.Role;
import java.nio.file.Path;
import java.time.LocalDate;
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
 * Contrat consommateur de {@code identite-clients.client-modifie.v1} : l'exemple du schéma du producteur se lit, ses
 * neuf champs sont confiés au cas d'usage ; un type ou une version inattendus sont ignorés.
 */
@ExtendWith(OutputCaptureExtension.class)
@Tag("FR-016-03")
class ClientModifieContratEvenementConsommateurTest {

    private static final Path SCHEMA =
            Path.of("../../contrats/evenements/identite-clients.client-modifie.v1.schema.json");

    private final JsonMapper json = JsonMapper.builder().build();
    private final JsonNode exemple = json.readTree(SCHEMA.toFile()).get("examples").get(0);
    private final List<ClientProjete> recus = new ArrayList<>();
    private final List<UUID> identifiants = new ArrayList<>();
    private Issue issue = Issue.ENREGISTRE;
    private final RecepteurClientModifie recepteur = new RecepteurClientModifie((idEvenement, client) -> {
        identifiants.add(idEvenement);
        recus.add(client);
        return issue;
    }, json);

    @Test
    void l_exemple_du_schema_est_lu_avec_ses_neuf_champs() {
        recepteur.recevoir(json.writeValueAsString(exemple));

        assertThat(identifiants).containsExactly(UUID.fromString(exemple.get("idEvenement").asString()));
        assertThat(recus).containsExactly(new ClientProjete(1L, "user1", "Jean Dupont", Role.CLIENT, false, 11L,
                LocalDate.of(2026, 10, 6), false, 2L));
    }

    @Test
    void un_conseiller_absent_est_lu_comme_nul() {
        recepteur.recevoir(json.writeValueAsString(exemple).replace("\"conseillerId\":11", "\"conseillerId\":null"));

        assertThat(recus).singleElement().satisfies(client -> assertThat(client.conseillerId()).isNull());
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
