package fr.nextbanking.coeurbancaire.domain;

/**
 * Codes d'audit produits par le cœur bancaire (catalogue fermé AR-40). La story 3.1 produit VIREMENT puis
 * TRANSFER_EXECUTED ; les autres codes de virement arrivent avec les stories suivantes.
 */
public enum ActionAudit {
    VIREMENT,
    TRANSFER_EXECUTED
}
