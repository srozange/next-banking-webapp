package fr.nextbanking.credits.domain;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.credits.domain.DemandePretInvalide.Motif;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DemandePretTest {

    private static Motif motif(String type, String montant, String taux, String duree, String objet) {
        try {
            DemandePret.lire(type, montant, taux, duree, objet);
            return null;
        } catch (DemandePretInvalide refus) {
            return refus.motif();
        }
    }

    @Test
    @Tag("FR-007-07")
    void lit_une_demande_nominale() {
        DemandePret demande = DemandePret.lire("CONSOMMATION", "10000", "3.5", " 300 ", "Voiture");

        assertThat(demande.type()).isEqualTo(TypePret.CONSOMMATION);
        assertThat(demande.montant()).isEqualByComparingTo("10000.00");
        assertThat(demande.montant().scale()).isEqualTo(2);
        assertThat(demande.tauxAnnuel()).isEqualByComparingTo("3.5");
        assertThat(demande.dureeMois()).isEqualTo(300);
        assertThat(demande.objet()).isEqualTo("Voiture");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "INCONNU", "auto", "Consommation"})
    @Tag("FR-007-05")
    void un_type_absent_ou_inconnu_est_refuse(String type) {
        assertThat(motif(type, "10000", "3.5", "36", "Voiture")).isEqualTo(Motif.TYPE);
    }

    @Test
    @Tag("FR-007-05")
    void les_cinq_types_sont_acceptes() {
        for (TypePret type : TypePret.values()) {
            assertThat(motif(type.name(), "10000", "3.5", "36", "x")).isNull();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "abc", "0", "0.004", "-5", "100000000000000000000"})
    @Tag("FR-004-05")
    void un_montant_invalide_est_refuse(String montant) {
        assertThat(motif("AUTO", montant, "3.5", "36", "x")).isEqualTo(Motif.MONTANT);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"-1", "31", "30.01", "abc"})
    @Tag("FR-007-05")
    void un_taux_hors_plage_est_refuse(String taux) {
        assertThat(motif("AUTO", "100", taux, "36", "x")).isEqualTo(Motif.TAUX);
    }

    @Test
    @Tag("FR-007-05")
    void les_bornes_du_taux_sont_acceptees() {
        assertThat(motif("AUTO", "100", "0", "36", "x")).isNull();
        assertThat(motif("AUTO", "100", "30", "36", "x")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0", "-1", "301", "360", "361", "12.5", "abc", "99999999999"})
    @Tag("FR-007-06")
    @Tag("BUG-025")
    void une_duree_hors_de_1_a_300_est_refusee(String duree) {
        assertThat(motif("AUTO", "100", "3.5", duree, "x")).isEqualTo(Motif.DUREE);
    }

    @Test
    @Tag("FR-007-06")
    @Tag("BUG-025")
    void les_durees_1_et_300_sont_acceptees() {
        assertThat(motif("AUTO", "100", "3.5", "1", "x")).isNull();
        assertThat(motif("AUTO", "100", "3.5", "300", "x")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   \t"})
    @Tag("FR-007-05")
    void un_objet_absent_ou_blanc_est_refuse(String objet) {
        assertThat(motif("AUTO", "100", "3.5", "36", objet)).isEqualTo(Motif.OBJET);
    }

    @Test
    @Tag("FR-007-05")
    void le_premier_champ_invalide_l_emporte_dans_l_ordre_type_montant_taux_duree_objet() {
        assertThat(motif(null, "", "99", "0", "")).isEqualTo(Motif.TYPE);
        assertThat(motif("AUTO", "", "99", "0", "")).isEqualTo(Motif.MONTANT);
        assertThat(motif("AUTO", "100", "99", "0", "")).isEqualTo(Motif.TAUX);
        assertThat(motif("AUTO", "100", "3", "0", "")).isEqualTo(Motif.DUREE);
        assertThat(motif("AUTO", "100", "3", "12", "")).isEqualTo(Motif.OBJET);
    }

    @Test
    @Tag("FR-007-05")
    void ne_controle_pas_la_longueur_de_l_objet() {
        assertThat(motif("AUTO", "100", "3", "12", "x".repeat(1000))).isNull();
    }
}
