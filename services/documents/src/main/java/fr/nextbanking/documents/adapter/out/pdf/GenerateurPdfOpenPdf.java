package fr.nextbanking.documents.adapter.out.pdf;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.draw.LineSeparator;
import fr.nextbanking.documents.application.port.out.GenerateurPdf;
import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.DonneesDocument;
import fr.nextbanking.documents.domain.Ifu;
import fr.nextbanking.documents.domain.LibellesMois;
import fr.nextbanking.documents.domain.LigneReleve;
import fr.nextbanking.documents.domain.Releve;
import fr.nextbanking.documents.domain.TypeOperation;
import fr.nextbanking.documents.domain.TypeDocument;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Adaptateur sortant : seul producteur de PDF, par OpenPDF (LGPL/MPL), confiné à ce paquet. Textes du legacy
 * ({@code docs/retro/features/FEAT-017/MESSAGES.md}) : graphie sans accent là où le legacy n'en a pas. Le RIB et
 * l'attestation de solde sont livrés par la story 6.1, le relevé mensuel et l'IFU par la story 6.2 (disposition,
 * polices et formats du legacy {@code DocumentService}).
 */
@Component
public class GenerateurPdfOpenPdf implements GenerateurPdf {

    static final String BANQUE = "Demo Bank";
    static final String PIED_DE_PAGE = "Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012"
            + " — Établissement de crédit agréé par l'ACPR";
    static final String CODE_BANQUE = "30004";
    static final String CODE_GUICHET = "00001";
    static final String CLE_RIB = "42";
    static final String BIC = "DEMOFR21XXX";
    static final String DOMICILIATION = "Demo Bank - Agence Paris Centre";
    static final String SIREN = "123 456 789 00012";
    static final String PREFIXE_IBAN = "FR76-";

    private static final Color ROUGE = new Color(227, 0, 15);
    private static final Color SOMBRE = new Color(26, 26, 26);
    private static final Color GRIS = new Color(136, 136, 136);
    private static final Color CLAIR = new Color(221, 221, 221);
    private static final Color VERT = new Color(46, 125, 50);
    private static final DateTimeFormatter JOUR_COURT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public DocumentPdf generer(TypeDocument type, DonneesDocument donnees) {
        return switch (type) {
            case RIB -> new DocumentPdf("rib_" + donnees.compteId() + ".pdf", rib(donnees));
            case ATTESTATION -> new DocumentPdf("attestation_solde_" + donnees.compteId() + ".pdf",
                    attestation(donnees));
            case RELEVE -> new DocumentPdf(String.format("releve_%d_%d_%02d.pdf", donnees.compteId(),
                    donnees.releve().annee(), donnees.releve().mois()), releve(donnees));
            case IFU -> new DocumentPdf("ifu_" + donnees.clientId() + "_" + donnees.ifu().annee() + ".pdf",
                    ifu(donnees));
        };
    }

    private byte[] releve(DonneesDocument donnees) {
        Releve releve = donnees.releve();
        return ecrire(PageSize.A4, 50, 60, document -> {
            entete(document, "Releve de compte");
            ligne(document, "Compte", donnees.numeroCompte() + " (" + releve.typeCompte() + ")");
            ligne(document, "Titulaire", donnees.titulaire());
            ligne(document, "Periode", LibellesMois.libelle(releve.mois()) + " " + releve.annee());
            ligne(document, "Solde actuel", montant(donnees.solde()));
            separateur(document);
            if (releve.lignes().isEmpty()) {
                document.add(new Paragraph("Aucune operation sur cette periode.",
                        new Font(Font.HELVETICA, 10, Font.NORMAL, GRIS)));
            } else {
                PdfPTable tableau = new PdfPTable(5);
                tableau.setWidthPercentage(100);
                tableau.setWidths(new float[] {15, 38, 12, 17, 18});
                tableau.setSpacingBefore(10);
                enteteTableau(tableau, "Date", "Description", "Type", "Montant", "Solde apres");
                for (LigneReleve ligne : releve.lignes()) {
                    boolean credit = ligne.type() == TypeOperation.CREDIT;
                    ligneTableau(tableau, credit ? VERT : ROUGE, JOUR_COURT.format(ligne.date()), ligne.libelle(),
                            ligne.type().name(), (credit ? "+" : "-") + montant(ligne.montant()),
                            montant(ligne.soldeApres()));
                }
                document.add(tableau);
            }
            pied(document);
        });
    }

    private byte[] ifu(DonneesDocument donnees) {
        Ifu ifu = donnees.ifu();
        return ecrire(PageSize.A4, 60, 60, document -> {
            entete(document, "Imprime Fiscal Unique (IFU) " + ifu.annee());
            ligne(document, "Etablissement", BANQUE + " SA");
            ligne(document, "SIREN", SIREN);
            ligne(document, "Titulaire", donnees.titulaire());
            ligne(document, "Adresse", "1 rue de la Paix, 75001 Paris");
            ligne(document, "Annee fiscale", String.valueOf(ifu.annee()));
            separateur(document);
            PdfPTable tableau = new PdfPTable(2);
            tableau.setWidthPercentage(80);
            tableau.setSpacingBefore(10);
            enteteTableau(tableau, "Nature des revenus", "Montant brut");
            ligneTableau(tableau, SOMBRE, "Interets et produits de placement", montant(ifu.totalInterets()));
            ligneTableau(tableau, ROUGE, "Prelevement Forfaitaire Unique (30%)", montant(ifu.pfu()));
            document.add(tableau);
            document.add(new Paragraph("\nCe document est fourni a titre informatif. Il recense les revenus de capitaux"
                    + " mobiliers soumis au PFU pour l annee " + ifu.annee() + ".\n\nGenere le "
                    + JOUR_COURT.format(donnees.date()), new Font(Font.HELVETICA, 9, Font.NORMAL, GRIS)));
            pied(document);
        });
    }

    private byte[] rib(DonneesDocument donnees) {
        return ecrire(new Rectangle(595, 340), 50, 40, document -> {
            entete(document, "Releve d Identite Bancaire (RIB)");
            ligne(document, "Titulaire", donnees.titulaire());
            ligne(document, "Banque", BANQUE + " SA");
            ligne(document, "Code banque", CODE_BANQUE);
            ligne(document, "Code guichet", CODE_GUICHET);
            ligne(document, "Numero de compte",
                    donnees.numeroCompte().replace(PREFIXE_IBAN, "").replace("-", ""));
            ligne(document, "Cle RIB", CLE_RIB);
            ligne(document, "IBAN", donnees.iban());
            ligne(document, "BIC / SWIFT", BIC);
            ligne(document, "Domiciliation", DOMICILIATION);
            separateur(document);
            document.add(new Paragraph("Document genere le " + JOUR_COURT.format(donnees.date())
                    + " - " + BANQUE + " SA, 1 Place Vendome, 75001 Paris",
                    new Font(Font.HELVETICA, 8, Font.NORMAL, GRIS)));
            pied(document);
        });
    }

    private byte[] attestation(DonneesDocument donnees) {
        String jour = jourEnLettres(donnees.date());
        return ecrire(PageSize.A4, 80, 80, document -> {
            entete(document, "Attestation de Solde");
            Paragraph introduction = new Paragraph("\n" + BANQUE + " SA, dont le siege social est situe"
                    + " 1 Place Vendome, 75001 Paris, atteste par la presente que :\n",
                    new Font(Font.HELVETICA, 11, Font.NORMAL, SOMBRE));
            introduction.setSpacingAfter(10);
            document.add(introduction);
            Font gras = new Font(Font.HELVETICA, 12, Font.BOLD, SOMBRE);
            document.add(new Paragraph("M. / Mme " + donnees.titulaire(), gras));
            document.add(new Paragraph("est titulaire du compte n° " + donnees.numeroCompte(), gras));
            document.add(new Paragraph("\nLe solde de ce compte, arrete au " + jour + ", est de :\n",
                    new Font(Font.HELVETICA, 11, Font.NORMAL, SOMBRE)));
            Paragraph montant = new Paragraph(montant(donnees.solde()), new Font(Font.HELVETICA, 18, Font.BOLD, ROUGE));
            montant.setAlignment(Element.ALIGN_CENTER);
            montant.setSpacingBefore(10);
            montant.setSpacingAfter(20);
            document.add(montant);
            document.add(new Paragraph("Cette attestation est delivree a la demande de l interesse(e) pour faire valoir"
                    + " ce que de droit.\n\nFait a Paris, le " + jour,
                    new Font(Font.HELVETICA, 11, Font.NORMAL, SOMBRE)));
            pied(document);
        });
    }

    /** Montant à 2 décimales suivi de l'unité, point décimal, sans séparateur de milliers : « 2130.00 EUR ». */
    static String montant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString() + " EUR";
    }

    /** Jour en lettres : jour sur deux chiffres, mois en français avec accents, année : « 09 Octobre 2026 ». */
    static String jourEnLettres(LocalDate date) {
        return String.format("%02d %s %d", date.getDayOfMonth(), LibellesMois.libelle(date.getMonthValue()),
                date.getYear());
    }

    /** Écriture d'un document : page, marges, contenu ; toute erreur de la bibliothèque est une panne de génération. */
    private static byte[] ecrire(Rectangle page, float margeHorizontale, float margeVerticale, Contenu contenu) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(page, margeHorizontale, margeHorizontale, margeVerticale, margeVerticale);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();
            contenu.ecrire(document);
            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Impossible de générer le PDF", e);
        }
        return sortie.toByteArray();
    }

    private static void entete(Document document, String titre) throws DocumentException {
        PdfPTable bandeau = new PdfPTable(1);
        bandeau.setWidthPercentage(100);
        PdfPCell cellule = new PdfPCell(new Phrase(BANQUE + " — " + titre,
                new Font(Font.HELVETICA, 14, Font.BOLD, Color.WHITE)));
        cellule.setBackgroundColor(SOMBRE);
        cellule.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cellule.setPadding(10);
        cellule.setBorder(Rectangle.NO_BORDER);
        bandeau.addCell(cellule);
        bandeau.setSpacingAfter(16);
        document.add(bandeau);
    }

    private static void ligne(Document document, String libelle, String valeur) throws DocumentException {
        PdfPTable ligne = new PdfPTable(2);
        ligne.setWidthPercentage(100);
        ligne.setWidths(new float[] {2, 5});
        ligne.addCell(cellule(libelle, new Font(Font.HELVETICA, 10, Font.BOLD, GRIS)));
        ligne.addCell(cellule(valeur, new Font(Font.HELVETICA, 10, Font.NORMAL, SOMBRE)));
        document.add(ligne);
    }

    private static PdfPCell cellule(String texte, Font police) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, police));
        cellule.setBorder(Rectangle.NO_BORDER);
        cellule.setPaddingBottom(4);
        return cellule;
    }

    private static void enteteTableau(PdfPTable tableau, String... titres) {
        Font police = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        for (String titre : titres) {
            PdfPCell cellule = new PdfPCell(new Phrase(titre, police));
            cellule.setBackgroundColor(SOMBRE);
            cellule.setPadding(6);
            cellule.setBorderColor(Color.WHITE);
            tableau.addCell(cellule);
        }
    }

    private static void ligneTableau(PdfPTable tableau, Color couleur, String... valeurs) {
        Font police = new Font(Font.HELVETICA, 9, Font.NORMAL, couleur);
        for (String valeur : valeurs) {
            PdfPCell cellule = new PdfPCell(new Phrase(valeur, police));
            cellule.setPadding(5);
            cellule.setBorderColor(CLAIR);
            tableau.addCell(cellule);
        }
    }

    private static void separateur(Document document) throws DocumentException {
        document.add(new Chunk(new LineSeparator(1, 100, ROUGE, Element.ALIGN_CENTER, -2)));
        document.add(Chunk.NEWLINE);
    }

    private static void pied(Document document) throws DocumentException {
        document.add(Chunk.NEWLINE);
        separateur(document);
        Paragraph pied = new Paragraph(PIED_DE_PAGE, new Font(Font.HELVETICA, 7, Font.NORMAL, GRIS));
        pied.setAlignment(Element.ALIGN_CENTER);
        document.add(pied);
    }

    /** Contenu d'un document, écrit dans la page ouverte. */
    @FunctionalInterface
    private interface Contenu {
        void ecrire(Document document) throws DocumentException;
    }
}
