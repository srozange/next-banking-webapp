package fr.nextbanking.documents.application.port.in;

import fr.nextbanking.documents.domain.DocumentPdf;

/** Port entrant : téléchargement d'un document PDF par son titulaire. */
public interface TelechargerDocument {

    /**
     * Demande reçue : le client du jeton, et les paramètres {@code type} et {@code compteId} tels que reçus (chaînes,
     * éventuellement absents).
     */
    record Commande(long clientId, String type, String compteId) {
    }

    /**
     * Valide la demande (type puis compte), contrôle que le compte est celui du client, génère le document et compte
     * le téléchargement. Rend le document, ou lève {@code DemandeDocumentInvalide}, {@code CompteIntrouvable},
     * {@code ClientIntrouvable} ou {@code AccesCompteRefuse} sans rien générer ni compter.
     */
    DocumentPdf telecharger(Commande commande);
}
