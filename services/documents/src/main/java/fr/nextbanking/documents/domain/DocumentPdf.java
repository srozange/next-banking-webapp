package fr.nextbanking.documents.domain;

/** Document généré : nom du fichier proposé au téléchargement et contenu PDF. */
public record DocumentPdf(String nomFichier, byte[] contenu) {

    public DocumentPdf {
        if (nomFichier == null || nomFichier.isBlank() || contenu == null || contenu.length == 0) {
            throw new IllegalArgumentException("Le nom du fichier et le contenu du document sont obligatoires.");
        }
    }
}
