package fr.nextbanking.bourse.domain;

/**
 * Demande d'historique sans symbole (absent ou blanc) : requête invalide.
 */
public class SymboleManquant extends RuntimeException {

    public SymboleManquant() {
        super("Symbole manquant");
    }
}
