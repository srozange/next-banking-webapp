package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ErreurSaisieBeneficiaire.Cause;
import fr.nextbanking.coeurbancaire.domain.ErreurSaisieBeneficiaire.Champ;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Validation et normalisation de la demande d'ajout d'un bénéficiaire (FR-013-05 à 07). */
class DemandeBeneficiaireTest {

    private static final String IBAN = "FR3530004000000123456789010";

    private static void refus(Champ champ, Cause cause, String message, String libelle, String titulaire,
            String iban, String bic, String banque) {
        assertThatThrownBy(() -> DemandeBeneficiaire.valider(libelle, titulaire, iban, bic, banque))
                .isInstanceOfSatisfying(ErreurSaisieBeneficiaire.class, e -> {
                    assertThat(e.champ()).isEqualTo(champ);
                    assertThat(e.motif()).isEqualTo(cause);
                    assertThat(e.getMessage()).isEqualTo(message);
                });
    }

    @Test
    @Tag("FR-013-07")
    void la_saisie_est_normalisee() {
        DemandeBeneficiaire demande = DemandeBeneficiaire.valider("[retro] test beneficiaire", "Retro Test SA",
                " fr35 3000 4000 0001 2345 6789 010 ", " hsbcfrppxxx ", "   ");

        assertThat(demande.iban().enClair()).isEqualTo(IBAN);
        assertThat(demande.bic()).isEqualTo("HSBCFRPPXXX");
        assertThat(demande.banque()).isNull();
        assertThat(demande.zoneSepa()).isTrue();
        assertThat(demande.libelle()).isEqualTo("[retro] test beneficiaire");
    }

    @Test
    @Tag("FR-013-07")
    void bic_et_banque_absents_restent_absents_et_la_banque_est_rognee() {
        DemandeBeneficiaire sans = DemandeBeneficiaire.valider("a", "b", IBAN, null, null);
        DemandeBeneficiaire avec = DemandeBeneficiaire.valider("a", "b", "TR330006100519786457841326", "abcdfr2a",
                "  HSBC France ");

        assertThat(sans.bic()).isNull();
        assertThat(sans.banque()).isNull();
        assertThat(DemandeBeneficiaire.valider("a", "b", IBAN, "   ", "").bic()).isNull();
        assertThat(avec.bic()).isEqualTo("ABCDFR2A");
        assertThat(avec.banque()).isEqualTo("HSBC France");
        assertThat(avec.zoneSepa()).isFalse();
    }

    @Test
    @Tag("FR-013-05")
    void chaque_champ_obligatoire_vide_est_refuse_dans_l_ordre() {
        refus(Champ.LIBELLE, Cause.OBLIGATOIRE, "Le libellé est obligatoire.", "", "t", IBAN, null, null);
        refus(Champ.LIBELLE, Cause.OBLIGATOIRE, "Le libellé est obligatoire.", null, "", "", "XX", null);
        refus(Champ.LIBELLE, Cause.OBLIGATOIRE, "Le libellé est obligatoire.", "  ", "t", IBAN, null, null);
        refus(Champ.IBAN, Cause.OBLIGATOIRE, "L'IBAN est obligatoire.", "a", "", "", null, null);
        refus(Champ.IBAN, Cause.OBLIGATOIRE, "L'IBAN est obligatoire.", "a", "t", null, null, null);
        refus(Champ.IBAN, Cause.OBLIGATOIRE, "L'IBAN est obligatoire.", "a", "t", "  ", null, null);
        refus(Champ.TITULAIRE, Cause.OBLIGATOIRE, "Le nom du titulaire est obligatoire.", "a", "", IBAN, "XX", null);
        refus(Champ.TITULAIRE, Cause.OBLIGATOIRE, "Le nom du titulaire est obligatoire.", "a", null, IBAN, null, null);
    }

    @Test
    @Tag("FR-013-05")
    void un_iban_a_la_cle_fausse_est_refuse_avant_le_titulaire() {
        refus(Champ.IBAN, Cause.IBAN_INVALIDE, "IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97).",
                "a", "", "FR3530004000000123456789011", null, null);
    }

    @Test
    @Tag("FR-013-05")
    void un_bic_mal_forme_est_refuse() {
        String message = "BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques.";
        refus(Champ.BIC, Cause.BIC_INVALIDE, message, "a", "b", IBAN, "HSBCFR", null);
        refus(Champ.BIC, Cause.BIC_INVALIDE, message, "a", "b", IBAN, "HSBCFRPPXX", null);
        refus(Champ.BIC, Cause.BIC_INVALIDE, message, "a", "b", IBAN, "1SBCFRPP", null);
        assertThat(DemandeBeneficiaire.valider("a", "b", IBAN, "HSBCFRPP", null).bic()).isEqualTo("HSBCFRPP");
    }

    @Test
    @Tag("FR-013-06")
    void au_dela_de_cent_caracteres_le_libelle_le_titulaire_et_la_banque_sont_refuses() {
        String cent = "x".repeat(100);
        String cent_un = "x".repeat(101);

        assertThat(DemandeBeneficiaire.valider(cent, cent, IBAN, null, cent).banque()).hasSize(100);
        refus(Champ.LIBELLE, Cause.TROP_LONG, "Longueur maximale dépassée.", cent_un, "b", IBAN, null, null);
        refus(Champ.TITULAIRE, Cause.TROP_LONG, "Longueur maximale dépassée.", "a", cent_un, IBAN, null, null);
        refus(Champ.BANQUE, Cause.TROP_LONG, "Longueur maximale dépassée.", "a", "b", IBAN, null, cent_un);
    }

    @Test
    void les_noms_de_champ_sont_ceux_du_corps_json() {
        assertThat(Champ.values()).extracting(Champ::nom)
                .containsExactly("libelle", "titulaire", "iban", "bic", "banque");
    }
}
