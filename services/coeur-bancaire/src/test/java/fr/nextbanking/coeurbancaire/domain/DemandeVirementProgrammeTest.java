package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme.Cause;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Validation et normalisation de la demande de virement programmé (FR-004-04 à 08). */
class DemandeVirementProgrammeTest {

    private static DemandeVirementProgramme valide(Long compte, String numero, String libelle, String montant,
            String frequence, String motif, String date) {
        return DemandeVirementProgramme.valider(compte, numero, libelle, montant, frequence, motif, date);
    }

    private static void refus(Cause cause, Long compte, String numero, String libelle, String montant,
            String frequence, String motif, String date) {
        assertThatThrownBy(() -> valide(compte, numero, libelle, montant, frequence, motif, date))
                .isInstanceOfSatisfying(ErreurVirementProgramme.class, e -> assertThat(e.motif()).isEqualTo(cause));
    }

    @Test
    @Tag("FR-004-07")
    void la_saisie_nominale_est_normalisee() {
        DemandeVirementProgramme demande = valide(1L, " FR76-0001-0002 ", " [retro] test virprog ", " 50 ", "MENSUEL",
                "  [retro] virement programm gm ", " 2027-01-01 ");

        assertThat(demande.compteSourceId()).isEqualTo(1L);
        assertThat(demande.numeroBeneficiaire()).isEqualTo("FR76-0001-0002");
        assertThat(demande.libelleBeneficiaire()).isEqualTo("[retro] test virprog");
        assertThat(demande.montant().texte()).isEqualTo("50.00");
        assertThat(demande.frequence()).isEqualTo(FrequenceVirement.MENSUEL);
        assertThat(demande.motif()).isEqualTo("[retro] virement programm gm");
        assertThat(demande.premiereExecution()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @Tag("FR-004-07")
    void un_motif_blanc_ou_absent_devient_nul_et_une_date_passee_est_acceptee() {
        assertThat(valide(1L, "n", "l", "1", "ANNUEL", "   ", "2020-01-01").motif()).isNull();
        assertThat(valide(1L, "n", "l", "1", "ANNUEL", null, "2020-01-01").premiereExecution())
                .isEqualTo(LocalDate.of(2020, 1, 1));
    }

    @Test
    @Tag("FR-004-08")
    void les_six_frequences_sont_dans_l_ordre_du_legacy() {
        assertThat(FrequenceVirement.values()).extracting(Enum::name).containsExactly("HEBDOMADAIRE", "MENSUEL",
                "BIMESTRIEL", "TRIMESTRIEL", "SEMESTRIEL", "ANNUEL");
        for (FrequenceVirement f : FrequenceVirement.values()) {
            assertThat(valide(1L, "n", "l", "1", f.name(), null, "2027-01-01").frequence()).isEqualTo(f);
        }
    }

    @Test
    @Tag("FR-004-08")
    void une_frequence_inconnue_ou_absente_est_refusee() {
        refus(Cause.FREQUENCE_INVALIDE, 1L, "n", "l", "1", "mensuel", null, "2027-01-01");
        refus(Cause.FREQUENCE_INVALIDE, 1L, "n", "l", "1", null, null, "2027-01-01");
    }

    @Test
    @Tag("FR-004-04")
    void les_regles_sont_verifiees_dans_l_ordre_du_legacy() {
        refus(Cause.COMPTE_SOURCE_OBLIGATOIRE, null, " ", " ", "", "x", null, "");
        refus(Cause.NUMERO_OBLIGATOIRE, 1L, " ", " ", "", "x", null, "");
        refus(Cause.NUMERO_OBLIGATOIRE, 1L, null, "l", "1", "MENSUEL", null, "2027-01-01");
        refus(Cause.LIBELLE_OBLIGATOIRE, 1L, "n", " ", "", "x", null, "");
        refus(Cause.LIBELLE_OBLIGATOIRE, 1L, "n", null, "1", "MENSUEL", null, "2027-01-01");
        refus(Cause.MONTANT_INVALIDE, 1L, "n", "l", "", "x", null, "");
        refus(Cause.DATE_OBLIGATOIRE, 1L, "n", "l", "1", "x", null, "  ");
        refus(Cause.DATE_OBLIGATOIRE, 1L, "n", "l", "1", "x", null, null);
        refus(Cause.DATE_INVALIDE, 1L, "n", "l", "1", "x", null, "01/01/2027");
        refus(Cause.FREQUENCE_INVALIDE, 1L, "n", "l", "1", "x", "m".repeat(256), "2027-01-01");
    }

    @Test
    @Tag("FR-004-05")
    void le_montant_vide_nul_negatif_ou_illisible_donne_le_message_unique() {
        for (String montant : new String[] {null, "", "  ", "0", "0.00", "-5", "abc", "1.234"}) {
            refus(Cause.MONTANT_INVALIDE, 1L, "n", "l", montant, "MENSUEL", null, "2027-01-01");
        }
        assertThat(Cause.MONTANT_INVALIDE.texte()).isEqualTo("Montant invalide");
    }

    @Test
    @Tag("FR-004-04")
    void la_date_doit_etre_iso_et_reelle() {
        for (String date : new String[] {"01/01/2027", "2027-1-1", "2027-02-30", "2027-13-01", "demain", "2027-01-01x"}) {
            refus(Cause.DATE_INVALIDE, 1L, "n", "l", "1", "MENSUEL", null, date);
        }
    }

    @Test
    @Tag("FR-004-06")
    void les_longueurs_maximales_sont_acceptees_et_le_depassement_refuse() {
        assertThat(valide(1L, "n".repeat(34), "l".repeat(100), "1", "MENSUEL", "m".repeat(255), "2027-01-01"))
                .isNotNull();
        refus(Cause.TROP_LONG, 1L, "n".repeat(35), "l", "1", "MENSUEL", null, "2027-01-01");
        refus(Cause.TROP_LONG, 1L, "n", "l".repeat(101), "1", "MENSUEL", null, "2027-01-01");
        refus(Cause.TROP_LONG, 1L, "n", "l", "1", "MENSUEL", "m".repeat(256), "2027-01-01");
    }

    @Test
    @Tag("FR-004-06")
    @Tag("FR-004-13")
    void chaque_cause_porte_son_texte_et_son_champ() {
        assertThat(Cause.COMPTE_SOURCE_OBLIGATOIRE.texte()).isEqualTo("Veuillez selectionner un compte source");
        assertThat(Cause.NUMERO_OBLIGATOIRE.texte()).isEqualTo("Veuillez saisir le numero de compte destinataire");
        assertThat(Cause.LIBELLE_OBLIGATOIRE.texte()).isEqualTo("Veuillez saisir le libelle du beneficiaire");
        assertThat(Cause.DATE_OBLIGATOIRE.texte()).isEqualTo("Veuillez saisir la date de premiere execution");
        assertThat(Cause.DATE_INVALIDE.texte()).isEqualTo("Format de date invalide (attendu : yyyy-MM-dd)");
        assertThat(Cause.COMPTE_SOURCE_INTROUVABLE.texte()).isEqualTo("Erreur : Client ou compte introuvable");
        assertThat(Cause.COMPTE_SOURCE_ETRANGER.texte()).isEqualTo("Erreur : Compte non autorise");
        assertThat(Cause.DESTINATAIRE_INTROUVABLE.texte()).isEqualTo("Compte destinataire introuvable");
        assertThat(Cause.TROP_LONG.texte()).isNull();
        assertThat(Cause.TROP_LONG.champ()).isNull();
        assertThat(new ErreurVirementProgramme(Cause.TROP_LONG).getMessage()).isEqualTo("TROP_LONG");
        for (ErreurVirementProgramme.Champ champ : ErreurVirementProgramme.Champ.values()) {
            assertThat(champ.nom()).isNotBlank();
        }
        assertThat(Cause.MONTANT_INVALIDE.champ().nom()).isEqualTo("montant");
        assertThat(new ErreurVirementProgramme(Cause.DATE_INVALIDE).getMessage())
                .isEqualTo("Format de date invalide (attendu : yyyy-MM-dd)");
    }
}
