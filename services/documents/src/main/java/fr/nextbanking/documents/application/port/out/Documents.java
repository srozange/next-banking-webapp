package fr.nextbanking.documents.application.port.out;

import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.TypeDocument;
import java.time.Instant;

/**
 * Port sortant : table {@code documents}, une ligne par client, type et paramètres, avec son compteur de
 * téléchargements.
 */
public interface Documents {

    /**
     * Compte un téléchargement réussi : crée la ligne (client, type, paramètres) à la première génération avec un
     * compteur de 1, l'incrémente ensuite de 1.
     */
    void compterTelechargement(long clientId, TypeDocument type, String parametres, DocumentPdf document,
            Instant instant);
}
