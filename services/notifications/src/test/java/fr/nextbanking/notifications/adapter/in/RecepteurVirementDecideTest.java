package fr.nextbanking.notifications.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.notifications.application.port.in.RecevoirVirementDecide.Notification;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@Tag("FR-002-09")
@Tag("BUG-007")
class RecepteurVirementDecideTest {

    private static final String ID = "6a9b2c3d-4e5f-4a6b-b7c8-9d0e1f2a3b4c";

    private final List<String> recus = new ArrayList<>();
    private boolean dejaTraite;
    private final RecepteurVirementDecide recepteur = new RecepteurVirementDecide(
            (UUID idEvenement, long virementId, Notification notification) -> {
                recus.add(idEvenement + " " + virementId + " " + notification);
                return !dejaTraite;
            }, JsonMapper.builder().build());

    @Test
    void confie_la_notification_rendue_de_l_enveloppe_au_cas_d_usage() {
        recepteur.recevoir(enveloppe("virement-decide", 1));

        assertThat(recus).containsExactly(UUID.fromString(ID) + " 12 "
                + new Notification(7, "ALERTE", null, "Virement de 5000.00 EUR en attente"));
    }

    @Test
    void accepte_un_evenement_deja_traite_sans_erreur() {
        dejaTraite = true;

        recepteur.recevoir(enveloppe("virement-decide", 1));

        assertThat(recus).hasSize(1);
    }

    @Test
    void ignore_un_autre_type_ou_une_autre_version() {
        recepteur.recevoir(enveloppe("autre-evenement", 1));
        recepteur.recevoir(enveloppe("virement-decide", 2));

        assertThat(recus).isEmpty();
    }

    private static String enveloppe(String type, int version) {
        return """
                {"idEvenement":"%s","type":"%s","version":%d,"horodatage":"2026-11-17T09:00:00Z",\
                "producteur":"coeur-bancaire","donnees":{"virementId":12,"clientId":7,"etat":"EXECUTE",\
                "notification":{"clientId":7,"type":"ALERTE","titre":null,\
                "message":"Virement de 5000.00 EUR en attente"}}}""".formatted(ID, type, version);
    }
}
