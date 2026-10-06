package fr.nextbanking.coeurbancaire.domain;

/**
 * État d'un virement. La story 3.1 ne produit que l'exécution immédiate ; la suspension, la libération, le rejet
 * et le refus pour erreur de contrôle arrivent avec les stories 3.2 à 3.4.
 */
public enum EtatVirement {
    EXECUTE
}
