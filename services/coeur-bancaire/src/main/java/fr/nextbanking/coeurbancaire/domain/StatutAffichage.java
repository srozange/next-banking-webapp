package fr.nextbanking.coeurbancaire.domain;

/**
 * Statut d'affichage d'un virement (SUCCESS ou PENDING), distinct de son état : PENDING pour un montant au moins
 * égal au seuil de mise en attente (BUG-009 conservé provisoirement : le virement est déjà débité).
 */
public enum StatutAffichage {
    SUCCESS,
    PENDING
}
