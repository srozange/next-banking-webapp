package fr.nextbanking.documents.adapter;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/** Texte d'un PDF (PDFBox, en portée test), espaces et retours à la ligne ramenés à une espace. */
public final class PdfTexte {

    private PdfTexte() {
    }

    public static String extraire(byte[] pdf) {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ").trim();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
