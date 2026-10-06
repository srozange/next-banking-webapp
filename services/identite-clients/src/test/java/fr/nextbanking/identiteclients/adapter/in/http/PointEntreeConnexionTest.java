package fr.nextbanking.identiteclients.adapter.in.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-001-08")
@Tag("BUG-002")
class PointEntreeConnexionTest {

    @Test
    void la_demande_ne_montre_jamais_le_mot_de_passe() {
        assertThat(new PointEntreeConnexion.DemandeConnexion("user1", "secret-garde").toString())
                .contains("user1")
                .doesNotContain("secret-garde");
    }

    @Test
    void la_reponse_ne_montre_jamais_le_jeton() {
        assertThat(new PointEntreeConnexion.JetonEmis("eyJ.jeton.signe").toString()).doesNotContain("eyJ");
    }
}
