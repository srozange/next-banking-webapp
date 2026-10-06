package fr.nextbanking.identiteclients.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("FR-001-03")
class IdentifiantsTest {

    @Test
    void accepte_un_identifiant_et_un_mot_de_passe_renseignes() {
        Identifiants identifiants = new Identifiants("user1", "secret");

        assertThat(identifiants.identifiant()).isEqualTo("user1");
        assertThat(identifiants.motDePasse()).isEqualTo("secret");
    }

    @Test
    void refuse_un_identifiant_absent_vide_ou_blanc() {
        for (String identifiant : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> new Identifiants(identifiant, "secret"))
                    .isInstanceOfSatisfying(ConnexionRefusee.class,
                            refus -> assertThat(refus.motif()).isEqualTo(ConnexionRefusee.Motif.IDENTIFIANT_REQUIS));
        }
    }

    @Test
    void refuse_un_mot_de_passe_absent_ou_vide() {
        for (String motDePasse : new String[] {null, ""}) {
            assertThatThrownBy(() -> new Identifiants("user1", motDePasse))
                    .isInstanceOfSatisfying(ConnexionRefusee.class,
                            refus -> assertThat(refus.motif()).isEqualTo(ConnexionRefusee.Motif.MOT_DE_PASSE_REQUIS));
        }
    }

    @Test
    void controle_l_identifiant_avant_le_mot_de_passe() {
        assertThatThrownBy(() -> new Identifiants("", ""))
                .isInstanceOfSatisfying(ConnexionRefusee.class,
                        refus -> assertThat(refus.motif()).isEqualTo(ConnexionRefusee.Motif.IDENTIFIANT_REQUIS));
    }

    @Test
    @Tag("BUG-002")
    @Tag("FR-001-08")
    void ne_montre_jamais_le_mot_de_passe() {
        assertThat(new Identifiants("user1", "secret-tres-garde").toString())
                .contains("user1")
                .doesNotContain("secret-tres-garde");
    }
}
