package fr.nextbanking.bourse.application;

import fr.nextbanking.bourse.application.port.in.AjouterValeur;
import fr.nextbanking.bourse.application.port.out.DepotWatchlist;
import fr.nextbanking.bourse.domain.AjoutRefuse;
import fr.nextbanking.bourse.domain.NouvelleValeur;
import fr.nextbanking.bourse.domain.ValeurWatchlist;

/**
 * Ajoute une valeur : normalisation et contrôles de saisie d'abord, puis refus d'un symbole déjà présent.
 */
public class AjouterValeurCasUsage implements AjouterValeur {

    private final DepotWatchlist depot;

    public AjouterValeurCasUsage(DepotWatchlist depot) {
        this.depot = depot;
    }

    @Override
    public ValeurWatchlist ajouter(String symbole, String nom, String marche, String devise) {
        NouvelleValeur valeur = NouvelleValeur.depuis(symbole, nom, marche, devise);
        if (depot.existeSymbole(valeur.symbole())) {
            throw AjoutRefuse.doublon(valeur.symbole());
        }
        return depot.ajouter(valeur);
    }
}
