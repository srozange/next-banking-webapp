package fr.nextbanking.documents.application.port.in;

import fr.nextbanking.documents.domain.DocumentPdf;

/** Port entrant : téléchargement d'un document PDF par son titulaire. */
public interface TelechargerDocument {

    /**
     * Demande reçue : le client du jeton, et les paramètres {@code type}, {@code compteId}, {@code annee} et
     * {@code mois} tels que reçus (chaînes, éventuellement absents). Le client d'un IFU est toujours celui du jeton.
     */
    record Commande(long clientId, String type, String compteId, String annee, String mois) {
    }

    /**
     * Valide la demande (type, compte, année puis mois), contrôle que le compte est celui du client, génère le document et compte
     * le téléchargement. Rend le document, ou lève {@code DemandeDocumentInvalide}, {@code CompteIntrouvable},
     * {@code ClientIntrouvable} ou {@code AccesCompteRefuse} sans rien générer ni compter.
     */
    DocumentPdf telecharger(Commande commande);
}
