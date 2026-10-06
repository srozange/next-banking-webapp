package fr.nextbanking.bourse.application.port.out;

import fr.nextbanking.bourse.domain.AjoutRefuse;
import fr.nextbanking.bourse.domain.NouvelleValeur;
import fr.nextbanking.bourse.domain.ValeurWatchlist;
import java.util.List;

/**
 * Port sortant : valeurs de la watchlist commune.
 */
public interface DepotWatchlist {

    /** Toutes les valeurs, triées par marché puis par symbole. */
    List<ValeurWatchlist> listerTrie();

    /** Vrai si le symbole existe déjà, sans tenir compte de la casse. */
    boolean existeSymbole(String symbole);

    /**
     * Enregistre la valeur et la rend avec son identifiant.
     *
     * @throws AjoutRefuse motif {@code DOUBLON} si le symbole a été ajouté entre-temps
     */
    ValeurWatchlist ajouter(NouvelleValeur valeur);

    /** Supprime la valeur ; faux si aucune ne porte cet identifiant. */
    boolean supprimer(long id);
}
