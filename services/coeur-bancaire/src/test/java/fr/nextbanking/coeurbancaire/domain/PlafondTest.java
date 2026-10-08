package fr.nextbanking.coeurbancaire.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.coeurbancaire.domain.ErreurPlafond.Cause;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PlafondTest {

    private static void refuse(String texte, Cause cause) {
        assertThatThrownBy(() -> Plafond.lire(texte)).isInstanceOfSatisfying(ErreurPlafond.class,
                e -> assertThat(e.motif()).isEqualTo(cause));
    }

    @Test
    @Tag("FR-006-07")
    void un_plafond_positif_est_lu_a_deux_decimales() {
        assertThat(Plafond.lire("500").texte()).isEqualTo("500.00");
        assertThat(Plafond.lire(" 12.5 ").texte()).isEqualTo("12.50");
        assertThat(Plafond.lire("0.01").texte()).isEqualTo("0.01");
        assertThat(Plafond.lire("9999999999999").texte()).isEqualTo("9999999999999.00");
    }

    @Test
    @Tag("FR-006-07")
    void un_plafond_est_arrondi_a_deux_decimales_moitie_vers_le_haut() {
        assertThat(Plafond.lire("10.005").texte()).isEqualTo("10.01");
        assertThat(Plafond.lire("10.004").texte()).isEqualTo("10.00");
    }

    @Test
    @Tag("FR-006-05")
    void un_plafond_absent_illisible_nul_ou_negatif_est_invalide() {
        refuse(null, Cause.PLAFOND_INVALIDE);
        refuse("", Cause.PLAFOND_INVALIDE);
        refuse("abc", Cause.PLAFOND_INVALIDE);
        refuse("0", Cause.PLAFOND_INVALIDE);
        refuse("-10", Cause.PLAFOND_INVALIDE);
        refuse("0.001", Cause.PLAFOND_INVALIDE);
    }

    @Test
    @Tag("FR-006-05")
    void au_dela_de_treize_chiffres_entiers_le_plafond_est_refuse_explicitement() {
        refuse("10000000000000", Cause.PLAFOND_TROP_GRAND);
        refuse("12345678901234.5", Cause.PLAFOND_TROP_GRAND);
        refuse("9999999999999.995", Cause.PLAFOND_TROP_GRAND);
        refuse("1E999999999", Cause.PLAFOND_TROP_GRAND);
    }

    @Test
    @Tag("FR-006-05")
    void les_causes_portent_les_textes_attendus() {
        assertThat(Cause.CATEGORIE_INVALIDE.texte()).isEqualTo("Veuillez selectionner une categorie");
        assertThat(Cause.PLAFOND_INVALIDE.texte()).isEqualTo("Veuillez saisir un plafond valide");
        assertThat(Cause.PLAFOND_TROP_GRAND.texte())
                .isEqualTo("Le plafond ne doit pas dépasser 13 chiffres avant la virgule");
        assertThat(Cause.MOIS_INVALIDE.texte()).isNull();
        assertThat(new ErreurPlafond(Cause.MOIS_INVALIDE).getMessage()).isEqualTo("MOIS_INVALIDE");
        assertThat(new ErreurPlafond(Cause.INTROUVABLE).getMessage()).isNotBlank();
    }
}
