package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.CategorieBudget;
import fr.nextbanking.coeurbancaire.domain.Montant;
import fr.nextbanking.coeurbancaire.domain.Plafond;
import java.util.List;

/**
 * Port sortant : plafonds mensuels de dépenses par catégorie.
 */
public interface Plafonds {

    /** Nombre de plafonds définis par le client pour le mois (AAAA-MM). */
    long compter(long clientId, String mois);

    /** Plafonds du client pour le mois (AAAA-MM), sans ordre garanti. */
    List<Plafond> duMois(long clientId, String mois);

    /**
     * Crée le plafond (client, catégorie, mois) ou remplace sa valeur, en une seule opération atomique : deux appels
     * concurrents ne laissent qu'un plafond.
     */
    Plafond enregistrer(long clientId, CategorieBudget categorie, String mois, Montant plafond);

    /** Supprime le plafond s'il appartient au client ; faux s'il n'existe pas ou appartient à un autre client. */
    boolean retirer(long clientId, long plafondId);
}
