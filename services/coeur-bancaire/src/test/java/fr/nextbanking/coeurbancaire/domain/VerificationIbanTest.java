package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.coeurbancaire.domain.VerificationIban.Statut;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Vérification MOD-97 d'un IBAN saisi (FR-013-04). */
@Tag("FR-013-04")
class VerificationIbanTest {

    @Test
    void une_saisie_absente_ou_blanche_est_vide() {
        assertThat(VerificationIban.de(null).statut()).isEqualTo(Statut.VIDE);
        assertThat(VerificationIban.de("").statut()).isEqualTo(Statut.VIDE);
        assertThat(VerificationIban.de("   ").statut()).isEqualTo(Statut.VIDE);
    }

    @Test
    void une_cle_fausse_ou_un_format_incorrect_est_invalide() {
        assertThat(VerificationIban.de("FR3530004000000123456789011"))
                .isEqualTo(new VerificationIban(Statut.INVALIDE, null, false));
        assertThat(VerificationIban.de("n'importe quoi").statut()).isEqualTo(Statut.INVALIDE);
        assertThat(VerificationIban.de("FR35").statut()).isEqualTo(Statut.INVALIDE);
        assertThat(VerificationIban.de("1235300040000001234567890").statut()).isEqualTo(Statut.INVALIDE);
    }

    @Test
    void un_iban_valide_donne_son_pays_et_sa_zone() {
        assertThat(VerificationIban.de(" fr35 3000 4000 0001 2345 6789 010 "))
                .isEqualTo(new VerificationIban(Statut.VALIDE, "FR", true));
        assertThat(VerificationIban.de("TR330006100519786457841326"))
                .isEqualTo(new VerificationIban(Statut.VALIDE, "TR", false));
        assertThat(VerificationIban.de("GB29NWBK60161331926819").codePays()).isEqualTo("GB");
        assertThat(VerificationIban.de("DE89370400440532013000").statut()).isEqualTo(Statut.VALIDE);
    }
}
