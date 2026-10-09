package fr.nextbanking.coeurbancaire.domain;

/**
 * Statut d'affichage d'un virement (SUCCESS ou PENDING), distinct de son état : PENDING pour un montant au moins
 * égal au seuil de mise en attente. Le virement PENDING est exécuté et débité : il est annoncé comme une « transaction
 * importante » (FR-003-29, BUG-009 corrigé).
 */
public enum StatutAffichage {
    SUCCESS,
    PENDING
}
