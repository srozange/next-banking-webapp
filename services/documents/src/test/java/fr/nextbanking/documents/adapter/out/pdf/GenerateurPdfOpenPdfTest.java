package fr.nextbanking.documents.adapter.out.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import fr.nextbanking.documents.adapter.PdfTexte;
import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.DonneesDocument;
import fr.nextbanking.documents.domain.Ifu;
import fr.nextbanking.documents.domain.LigneReleve;
import fr.nextbanking.documents.domain.Releve;
import fr.nextbanking.documents.domain.TypeCompte;
import fr.nextbanking.documents.domain.TypeDocument;
import fr.nextbanking.documents.domain.TypeOperation;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Contenu des PDF du RIB, de l'attestation, du relevé et de l'IFU, relu par PDFBox : textes du legacy, date et solde fournis. */
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

    private static DonneesDocument donneesReleve(Releve releve) {
        return new DonneesDocument(1L, "Jean Dupont", 1L, "FR76-0001-0001", "FR76-0001-0001",
                new BigDecimal("2130.00"), LocalDate.of(2026, 10, 10), releve, null);
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-07")
    void le_releve_liste_ses_operations_avec_les_signes_les_types_et_les_textes_du_legacy() {
        DocumentPdf releve = generateur.generer(TypeDocument.RELEVE, donneesReleve(new Releve(TypeCompte.COURANT,
                2026, 3, List.of(
                        new LigneReleve(LocalDate.of(2026, 3, 1), "Salaire", TypeOperation.CREDIT,
                                new BigDecimal("1500"), new BigDecimal("3630.00")),
                        new LigneReleve(LocalDate.of(2026, 3, 31), "Loyer", TypeOperation.DEBIT,
                                new BigDecimal("100.50"), new BigDecimal("3529.50"))))));

        assertThat(releve.nomFichier()).isEqualTo("releve_1_2026_03.pdf");
        String texte = PdfTexte.extraire(releve.contenu());
        assertThat(texte).contains("Demo Bank — Releve de compte", "Compte FR76-0001-0001 (COURANT)",
                "Titulaire Jean Dupont", "Periode Mars 2026", "Solde actuel 2130.00 EUR",
                "Date Description Type Montant Solde apres",
                "01/03/2026 Salaire CREDIT +1500.00 EUR 3630.00 EUR",
                "31/03/2026 Loyer DEBIT -100.50 EUR 3529.50 EUR", PIED);
        assertThat(texte.indexOf("Salaire")).isLessThan(texte.indexOf("Loyer"));
        assertThat(texte).doesNotContain("Aucune operation");
    }

    @Test
    @Tag("FR-017-07")
    @Tag("FR-008-07")
    void un_releve_sans_operation_affiche_le_message_du_legacy_sans_tableau() {
        DocumentPdf releve = generateur.generer(TypeDocument.RELEVE,
                donneesReleve(new Releve(TypeCompte.EPARGNE, 2026, 12, List.of())));

        assertThat(releve.nomFichier()).isEqualTo("releve_1_2026_12.pdf");
        assertThat(PdfTexte.extraire(releve.contenu())).contains("Compte FR76-0001-0001 (EPARGNE)",
                "Periode Décembre 2026", "Aucune operation sur cette periode.", PIED)
                .doesNotContain("Solde apres");
    }

    @Test
    @Tag("FR-017-04")
    @Tag("FR-008-10")
    void l_ifu_porte_les_textes_du_legacy_le_total_des_interets_et_le_pfu() {
        DocumentPdf ifu = generateur.generer(TypeDocument.IFU, new DonneesDocument(1L, "Jean Dupont", null, null, null,
                null, LocalDate.of(2026, 10, 10), null, Ifu.de(2025, new BigDecimal("12.35"))));

        assertThat(ifu.nomFichier()).isEqualTo("ifu_1_2025.pdf");
        assertThat(PdfTexte.extraire(ifu.contenu())).contains("Demo Bank — Imprime Fiscal Unique (IFU) 2025",
                "Etablissement Demo Bank SA", "SIREN 123 456 789 00012", "Titulaire Jean Dupont",
                "Adresse 1 rue de la Paix, 75001 Paris", "Annee fiscale 2025",
                "Nature des revenus Montant brut", "Interets et produits de placement 12.35 EUR",
                "Prelevement Forfaitaire Unique (30%) 3.71 EUR",
                "Ce document est fourni a titre informatif. Il recense les revenus de capitaux mobiliers soumis au PFU"
                        + " pour l annee 2025.",
                "Genere le 10/10/2026", PIED);
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
