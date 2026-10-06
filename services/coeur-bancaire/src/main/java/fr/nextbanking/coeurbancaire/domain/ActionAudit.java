package fr.nextbanking.coeurbancaire.domain;

/**
 * Codes d'audit produits par le cœur bancaire (catalogue fermé AR-40, FR-026-03) : VIREMENT puis TRANSFER_EXECUTED
 * pour un virement exécuté ; ADMIN_TRANSFER_RELEASE et ADMIN_TRANSFER_REJECT pour la décision de l'administrateur ;
 * TRANSFER_AUTO_REJECT pour le rejet à l'échéance ; TRANSFER_REJECT_INSUFFICIENT_FUNDS pour le rejet au débit.
 */
public enum ActionAudit {
    VIREMENT,
    TRANSFER_EXECUTED,
    ADMIN_TRANSFER_RELEASE,
    ADMIN_TRANSFER_REJECT,
    TRANSFER_AUTO_REJECT,
    TRANSFER_REJECT_INSUFFICIENT_FUNDS
}
