package fr.nextbanking.bourse.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ExceptionsTest {

    @Test
    @Tag("FR-029-09")
    void un_doublon_porte_son_symbole() {
        AjoutRefuse refus = AjoutRefuse.doublon("BNP.PA");

        assertThat(refus.motif()).isEqualTo(AjoutRefuse.Motif.DOUBLON);
        assertThat(refus.symbole()).isEqualTo("BNP.PA");
        assertThat(refus.getMessage()).isEqualTo("Ajout refusé : DOUBLON");
    }

    @Test
    @Tag("FR-009-13")
    void une_valeur_introuvable_porte_son_identifiant() {
        ValeurIntrouvable introuvable = new ValeurIntrouvable(42);

        assertThat(introuvable.id()).isEqualTo(42);
        assertThat(introuvable.getMessage()).isEqualTo("Valeur introuvable : 42");
    }
}
