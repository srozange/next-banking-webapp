package fr.nextbanking.documents.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ModelesDuDomaineTest {

    private static final BigDecimal SOLDE = new BigDecimal("2130.00");

    @Test
    @Tag("FR-017-02")
    @Tag("FR-008-11")
    void le_type_se_lit_sans_egard_a_la_casse() {
        assertThat(TypeDocument.depuis("RIB")).contains(TypeDocument.RIB);
        assertThat(TypeDocument.depuis("rib")).contains(TypeDocument.RIB);
        assertThat(TypeDocument.depuis("Rib")).contains(TypeDocument.RIB);
        assertThat(TypeDocument.depuis("attestation")).contains(TypeDocument.ATTESTATION);
        assertThat(TypeDocument.depuis("Releve")).contains(TypeDocument.RELEVE);
        assertThat(TypeDocument.depuis("ifu")).contains(TypeDocument.IFU);
        assertThat(TypeDocument.depuis("FACTURE")).isEmpty();
        assertThat(TypeDocument.depuis("")).isEmpty();
        assertThat(TypeDocument.depuis(" RIB")).isEmpty();
    }

    @Test
    void seul_l_ifu_est_un_document_du_client_et_non_d_un_compte() {
        assertThat(TypeDocument.RIB.exigeCompte()).isTrue();
        assertThat(TypeDocument.ATTESTATION.exigeCompte()).isTrue();
        assertThat(TypeDocument.RELEVE.exigeCompte()).isTrue();
        assertThat(TypeDocument.IFU.exigeCompte()).isFalse();
    }

    @Test
    void les_mois_ont_leur_libelle_francais_avec_accents() {
        assertThat(java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(LibellesMois::libelle)).containsExactly(
                "Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre",
                "Novembre", "Décembre");
    }

    @Test
    void un_compte_appartient_a_son_titulaire_seul() {
        CompteProjete compte = new CompteProjete(1L, 1L, "FR76-0001-0001", TypeCompte.COURANT, "FR76-0001-0001",
                "DEMOFR21XXX", SOLDE, 1L);

        assertThat(compte.appartientA(1L)).isTrue();
        assertThat(compte.appartientA(2L)).isFalse();
    }

    @Test
    void un_compte_exige_numero_type_iban_bic_et_solde() {
        assertThatThrownBy(() -> compte(null, TypeCompte.COURANT, "i", "b", SOLDE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> compte(" ", TypeCompte.COURANT, "i", "b", SOLDE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> compte("n", null, "i", "b", SOLDE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> compte("n", TypeCompte.COURANT, null, "b", SOLDE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> compte("n", TypeCompte.COURANT, "i", null, SOLDE)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> compte("n", TypeCompte.COURANT, "i", "b", null)).isInstanceOf(IllegalArgumentException.class);
    }

    private static CompteProjete compte(String numero, TypeCompte type, String iban, String bic, BigDecimal solde) {
        return new CompteProjete(1L, 1L, numero, type, iban, bic, solde, 1L);
    }

    @Test
    void des_donnees_de_document_exigent_un_titulaire_et_une_date() {
        LocalDate jour = LocalDate.of(2026, 10, 9);

        assertThat(new DonneesDocument(1L, "Jean Dupont", null, null, null, null, jour).compteId()).isNull();
        assertThatThrownBy(() -> new DonneesDocument(1L, null, 1L, "n", "i", SOLDE, jour))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DonneesDocument(1L, "Jean Dupont", 1L, "n", "i", SOLDE, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_document_exige_un_nom_de_fichier_et_un_contenu() {
        byte[] contenu = {1};

        assertThat(new DocumentPdf("rib_1.pdf", contenu).nomFichier()).isEqualTo("rib_1.pdf");
        assertThatThrownBy(() -> new DocumentPdf(null, contenu)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DocumentPdf(" ", contenu)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DocumentPdf("rib_1.pdf", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DocumentPdf("rib_1.pdf", new byte[0])).isInstanceOf(IllegalArgumentException.class);
    }
}
