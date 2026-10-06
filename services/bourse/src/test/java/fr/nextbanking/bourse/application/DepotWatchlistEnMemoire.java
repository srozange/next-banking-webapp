package fr.nextbanking.bourse.application;

import fr.nextbanking.bourse.application.port.out.DepotWatchlist;
import fr.nextbanking.bourse.domain.NouvelleValeur;
import fr.nextbanking.bourse.domain.ValeurWatchlist;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Dépôt de test, en mémoire, trié comme la base. */
class DepotWatchlistEnMemoire implements DepotWatchlist {

    final List<ValeurWatchlist> valeurs = new ArrayList<>();
    private long suivant = 1;

    DepotWatchlistEnMemoire avec(String symbole, String nom, String marche, String devise) {
        valeurs.add(new ValeurWatchlist(suivant++, symbole, nom, marche, devise));
        return this;
    }

    @Override
    public List<ValeurWatchlist> listerTrie() {
        return valeurs.stream()
                .sorted(Comparator.comparing(ValeurWatchlist::marche).thenComparing(ValeurWatchlist::symbole))
                .toList();
    }

    @Override
    public boolean existeSymbole(String symbole) {
        return valeurs.stream().anyMatch(v -> v.symbole().toUpperCase(Locale.ROOT)
                .equals(symbole.toUpperCase(Locale.ROOT)));
    }

    @Override
    public ValeurWatchlist ajouter(NouvelleValeur valeur) {
        ValeurWatchlist ajoutee = new ValeurWatchlist(suivant++, valeur.symbole(), valeur.nom(), valeur.marche(),
                valeur.devise());
        valeurs.add(ajoutee);
        return ajoutee;
    }

    @Override
    public boolean supprimer(long id) {
        return valeurs.removeIf(v -> v.id() == id);
    }
}
