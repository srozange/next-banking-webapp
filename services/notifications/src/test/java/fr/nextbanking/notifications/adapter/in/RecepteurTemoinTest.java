package fr.nextbanking.notifications.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RecepteurTemoinTest {

    private static final String ID = "3f2c1a9e-8b7d-4c6e-9f10-2a3b4c5d6e7f";

    private final List<String> recus = new ArrayList<>();
    private boolean dejaTraite;
    private final RecepteurTemoin recepteur = new RecepteurTemoin((idEvenement, temoinId) -> {
        recus.add(idEvenement + " " + temoinId);
        return !dejaTraite;
    }, JsonMapper.builder().build());

    @Test
    void confie_le_temoin_de_l_enveloppe_au_cas_d_usage() {
        recepteur.recevoir(enveloppe("temoin-ecrit", 1));

        assertThat(recus).containsExactly(UUID.fromString(ID) + " 42");
    }

    @Test
    void accepte_un_evenement_deja_traite_sans_erreur() {
        dejaTraite = true;

        recepteur.recevoir(enveloppe("temoin-ecrit", 1));

        assertThat(recus).hasSize(1);
    }

    @Test
    void ignore_un_autre_type_ou_une_autre_version() {
        recepteur.recevoir(enveloppe("autre-evenement", 1));
        recepteur.recevoir(enveloppe("temoin-ecrit", 2));

        assertThat(recus).isEmpty();
    }

    private static String enveloppe(String type, int version) {
        return """
                {"idEvenement":"%s","type":"%s","version":%d,"horodatage":"2026-10-05T08:30:00Z",\
                "producteur":"coeur-bancaire","donnees":{"temoinId":42}}""".formatted(ID, type, version);
    }
}
