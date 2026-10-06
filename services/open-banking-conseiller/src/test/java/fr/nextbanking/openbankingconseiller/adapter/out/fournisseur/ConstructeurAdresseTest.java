package fr.nextbanking.openbankingconseiller.adapter.out.fournisseur;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-028-02")
class ConstructeurAdresseTest {

    @Test
    void l_adresse_porte_domaine_client_retour_code_banques_et_state_encodes() {
        String adresse = ConstructeurAdresse.adresse("demi-sandbox.biapi.pro", "99961298",
                "https://app.example/retour?x=1&y=é", "co de/+=", "59,60", "ab.cd-ef_gh");

        assertThat(adresse).isEqualTo("https://webview.powens.com/connect?domain=demi-sandbox.biapi.pro"
                + "&client_id=99961298&redirect_uri=https%3A%2F%2Fapp.example%2Fretour%3Fx%3D1%26y%3D%C3%A9"
                + "&code=co+de%2F%2B%3D&connector_ids=59%2C60&state=ab.cd-ef_gh");
    }
}
