package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.Plafond;

/**
 * Port entrant : plafonds mensuels de dépenses du client authentifié (FEAT-006).
 */
public interface GererPlafonds {

    /**
     * Crée ou remplace le plafond unique (client, catégorie, mois), à partir de la saisie brute.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurPlafond catégorie absente, inconnue ou « Revenus », plafond
     *     invalide ou de plus de 13 chiffres entiers, mois illisible
     */
    Plafond enregistrer(long clientId, String categorie, String mois, String plafond);

    /**
     * Supprime un plafond du client.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurPlafond {@code INTROUVABLE} si le plafond est inexistant ou
     *     appartient à un autre client (même refus)
     */
    void supprimer(long clientId, long plafondId);
}
