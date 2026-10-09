package fr.nextbanking.documents.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.nextbanking.documents.adapter.PdfTexte;
import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.DonneesDocument;
import fr.nextbanking.documents.domain.TypeDocument;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Contenu des PDF du RIB et de l'attestation, relu par PDFBox : textes du legacy, date et solde fournis. */
class GenerateurPdfOpenPdfTest {

    private static final String PIED = "Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012"
            + " — Établissement de crédit agréé par l'ACPR";

    private final GenerateurPdfOpenPdf generateur = new GenerateurPdfOpenPdf();
    private final DonneesDocument donnees = new DonneesDocument(1L, "Jean Dupont", 1L, "FR76-0001-0001",
            "FR76-0001-0001", new BigDecimal("2130.00"), LocalDate.of(2026, 10, 10));

    @Test
    @Tag("FR-017-05")
    @Tag("FR-008-08")
    void le_rib_porte_les_textes_du_legacy_le_titulaire_et_la_date_de_l_horloge() {
        DocumentPdf rib = generateur.generer(TypeDocument.RIB, donnees);

        assertThat(rib.nomFichier()).isEqualTo("rib_1.pdf");
        assertThat(new String(rib.contenu(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(PdfTexte.extraire(rib.contenu())).contains(
                "Demo Bank — Releve d Identite Bancaire (RIB)",
                "Titulaire Jean Dupont",
                "Banque Demo Bank SA",
                "Code banque 30004",
                "Code guichet 00001",
                "Numero de compte 00010001",
                "Cle RIB 42",
                "IBAN FR76-0001-0001",
                "BIC / SWIFT DEMOFR21XXX",
                "Domiciliation Demo Bank - Agence Paris Centre",
                "Document genere le 10/10/2026 - Demo Bank SA, 1 Place Vendome, 75001 Paris",
                PIED);
    }

    @Test
    @Tag("FR-017-06")
    @Tag("FR-008-09")
    void l_attestation_porte_le_solde_de_ce_compte_et_la_date_en_lettres_de_l_horloge() {
        DocumentPdf attestation = generateur.generer(TypeDocument.ATTESTATION, donnees);

        assertThat(attestation.nomFichier()).isEqualTo("attestation_solde_1.pdf");
        assertThat(PdfTexte.extraire(attestation.contenu())).contains(
                "Demo Bank — Attestation de Solde",
                "Demo Bank SA, dont le siege social est situe 1 Place Vendome, 75001 Paris, atteste par la presente que :",
                "M. / Mme Jean Dupont",
                "est titulaire du compte n° FR76-0001-0001",
                "Le solde de ce compte, arrete au 10 Octobre 2026, est de :",
                "2130.00 EUR",
                "Cette attestation est delivree a la demande de l interesse(e) pour faire valoir ce que de droit.",
                "Fait a Paris, le 10 Octobre 2026",
                PIED);
    }

    @Test
    void le_releve_et_l_ifu_ne_sont_pas_encore_generes() {
        for (TypeDocument type : new TypeDocument[] {TypeDocument.RELEVE, TypeDocument.IFU}) {
            assertThatThrownBy(() -> generateur.generer(type, donnees))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining(type.name());
        }
    }

    @Test
    void les_montants_ont_deux_decimales_et_les_mois_leur_libelle_accentue() {
        assertThat(GenerateurPdfOpenPdf.montant(new BigDecimal("17330"))).isEqualTo("17330.00 EUR");
        assertThat(GenerateurPdfOpenPdf.montant(new BigDecimal("0.005"))).isEqualTo("0.01 EUR");
        assertThat(GenerateurPdfOpenPdf.jourEnLettres(LocalDate.of(2026, 2, 3))).isEqualTo("03 Février 2026");
        assertThat(GenerateurPdfOpenPdf.jourEnLettres(LocalDate.of(2026, 8, 31))).isEqualTo("31 Août 2026");
        assertThat(GenerateurPdfOpenPdf.jourEnLettres(LocalDate.of(2026, 12, 25))).isEqualTo("25 Décembre 2026");
    }
}
