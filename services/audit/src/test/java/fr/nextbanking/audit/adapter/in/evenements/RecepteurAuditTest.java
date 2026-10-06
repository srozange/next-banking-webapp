package fr.nextbanking.audit.adapter.in.evenements;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.audit.application.port.in.EnregistrerEntreeAudit.Issue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(OutputCaptureExtension.class)
@Tag("FR-026-01")
class RecepteurAuditTest {

    private static final String ID = "7b1e4c2a-5d3f-4e6a-8b9c-0d1e2f3a4b5c";

    private final List<String> recus = new ArrayList<>();
    private Issue issue = Issue.ENREGISTREE;
    private final RecepteurAudit recepteur = new RecepteurAudit(
            (idEvenement, utilisateur, action, detail, adresseIp, horodatage) -> {
                recus.add(idEvenement + "|" + utilisateur + "|" + action + "|" + detail + "|" + adresseIp + "|"
                        + horodatage);
                return issue;
            }, JsonMapper.builder().build());

    @Test
    void confie_l_entree_de_l_enveloppe_au_cas_d_usage_avec_son_horodatage() {
        recepteur.recevoir(enveloppe("audit", 1, "LOGIN", "\"203.0.113.7\""));

        assertThat(recus).containsExactly(ID + "|1|LOGIN|Connexion réussie|203.0.113.7|2026-10-05T08:30:00Z");
    }

    @Test
    void transmet_une_adresse_absente() {
        recepteur.recevoir(enveloppe("audit", 1, "LOGIN", "null"));

        assertThat(recus).singleElement().asString().contains("|null|");
    }

    @Test
    void journalise_un_code_inconnu_rejete(CapturedOutput sortie) {
        issue = Issue.CODE_INCONNU;

        recepteur.recevoir(enveloppe("audit", 1, "CODE_INCONNU", "null"));

        assertThat(sortie.getOut()).contains("Code d'audit inconnu rejeté : CODE_INCONNU");
    }

    @Test
    void accepte_un_evenement_deja_traite_sans_erreur(CapturedOutput sortie) {
        issue = Issue.DEJA_TRAITEE;

        recepteur.recevoir(enveloppe("audit", 1, "LOGIN", "null"));

        assertThat(recus).hasSize(1);
        assertThat(sortie.getOut()).contains("déjà traité");
    }

    @Test
    void ignore_et_journalise_un_autre_type_ou_une_autre_version(CapturedOutput sortie) {
        recepteur.recevoir(enveloppe("autre-evenement", 1, "LOGIN", "null"));
        recepteur.recevoir(enveloppe("audit", 2, "LOGIN", "null"));

        assertThat(recus).isEmpty();
        assertThat(sortie.getOut()).contains("type autre-evenement version 1 inattendus", "type audit version 2 inattendus");
    }

    private static String enveloppe(String type, int version, String action, String adresseIp) {
        return """
                {"idEvenement":"%s","type":"%s","version":%d,"horodatage":"2026-10-05T08:30:00Z",\
                "producteur":"identite-clients","donnees":{"utilisateur":"1","action":"%s",\
                "detail":"Connexion réussie","adresseIp":%s}}""".formatted(ID, type, version, action, adresseIp);
    }
}
