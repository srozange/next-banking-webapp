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
    @Tag("FR-017-07")
    void seuls_le_releve_et_l_ifu_exigent_une_annee_et_seul_le_releve_un_mois() {
        assertThat(TypeDocument.RIB.exigeAnnee()).isFalse();
        assertThat(TypeDocument.ATTESTATION.exigeAnnee()).isFalse();
        assertThat(TypeDocument.RELEVE.exigeAnnee()).isTrue();
        assertThat(TypeDocument.IFU.exigeAnnee()).isTrue();
        assertThat(TypeDocument.RIB.exigeMois()).isFalse();
        assertThat(TypeDocument.IFU.exigeMois()).isFalse();
        assertThat(TypeDocument.RELEVE.exigeMois()).isTrue();
    }

    @Test
    @Tag("FR-017-04")
    @Tag("FR-008-10")
    void le_pfu_est_trente_pour_cent_du_total_arrondi_au_demi_superieur() {
        assertThat(Ifu.de(2025, new BigDecimal("12.35")).pfu()).isEqualByComparingTo("3.71");
        assertThat(Ifu.de(2025, new BigDecimal("12.35")).totalInterets().toPlainString()).isEqualTo("12.35");
        assertThat(Ifu.de(2025, new BigDecimal("0.01")).pfu()).isEqualByComparingTo("0.00");
        assertThat(Ifu.de(2025, new BigDecimal("0.02")).pfu()).isEqualByComparingTo("0.01");
        assertThat(Ifu.de(2025, BigDecimal.ZERO)).isEqualTo(new Ifu(2025, new BigDecimal("0.00"),
                new BigDecimal("0.00")));
        assertThat(Ifu.de(2025, new BigDecimal("100")).pfu().toPlainString()).isEqualTo("30.00");
    }

    @Test
    void une_operation_projetee_exige_ses_champs() {
        java.time.Instant instant = java.time.Instant.parse("2026-03-01T09:00:00Z");
        BigDecimal un = BigDecimal.ONE;

        assertThat(new OperationProjetee(1L, 1L, instant, "x", TypeOperation.CREDIT, un, un, 1L).libelle())
                .isEqualTo("x");
        assertThatThrownBy(() -> new OperationProjetee(1L, 1L, null, "x", TypeOperation.CREDIT, un, un, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationProjetee(1L, 1L, instant, null, TypeOperation.CREDIT, un, un, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationProjetee(1L, 1L, instant, "x", null, un, un, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationProjetee(1L, 1L, instant, "x", TypeOperation.CREDIT, null, un, 1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OperationProjetee(1L, 1L, instant, "x", TypeOperation.CREDIT, un, null, 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void un_releve_garde_une_copie_immuable_de_ses_lignes() {
        java.util.List<LigneReleve> lignes = new java.util.ArrayList<>();
        Releve releve = new Releve(TypeCompte.COURANT, 2026, 3, lignes);
        lignes.add(new LigneReleve(LocalDate.of(2026, 3, 1), "x", TypeOperation.DEBIT, BigDecimal.ONE, BigDecimal.TEN));

        assertThat(releve.lignes()).isEmpty();
        assertThatThrownBy(() -> releve.lignes().add(null)).isInstanceOf(UnsupportedOperationException.class);
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
