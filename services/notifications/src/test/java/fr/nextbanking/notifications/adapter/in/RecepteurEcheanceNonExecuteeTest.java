package fr.nextbanking.notifications.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.notifications.application.port.in.RecevoirEcheanceNonExecutee.Notification;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@Tag("FR-004-12")
@Tag("FR-021-04")
class RecepteurEcheanceNonExecuteeTest {

    private static final String ID = "7b0c3d4e-5f60-4b7c-8d9e-0f1a2b3c4d5e";

    private final List<String> recus = new ArrayList<>();
    private boolean dejaTraite;
    private final RecepteurEcheanceNonExecutee recepteur = new RecepteurEcheanceNonExecutee(
            (UUID idEvenement, long virementProgrammeId, Notification notification) -> {
                recus.add(idEvenement + " " + virementProgrammeId + " " + notification);
                return !dejaTraite;
            }, JsonMapper.builder().build());

    @Test
    void confie_la_notification_rendue_de_l_enveloppe_au_cas_d_usage() {
        recepteur.recevoir(enveloppe("echeance-non-executee", 1));

        assertThat(recus).containsExactly(UUID.fromString(ID) + " 3 "
                + new Notification(7, "ALERTE", "Virement programmé refusé", "Texte rendu"));
    }

    @Test
    void accepte_un_evenement_deja_traite_sans_erreur() {
        dejaTraite = true;

        recepteur.recevoir(enveloppe("echeance-non-executee", 1));

        assertThat(recus).hasSize(1);
    }

    @Test
    void ignore_un_autre_type_ou_une_autre_version() {
        recepteur.recevoir(enveloppe("autre-evenement", 1));
        recepteur.recevoir(enveloppe("echeance-non-executee", 2));

        assertThat(recus).isEmpty();
    }

    private static String enveloppe(String type, int version) {
        return """
                {"idEvenement":"%s","type":"%s","version":%d,"horodatage":"2026-11-17T09:00:00Z",\
                "producteur":"coeur-bancaire","donnees":{"virementProgrammeId":3,\
                "notification":{"clientId":7,"type":"ALERTE","titre":"Virement programmé refusé",\
                "message":"Texte rendu"}}}""".formatted(ID, type, version);
    }
}
