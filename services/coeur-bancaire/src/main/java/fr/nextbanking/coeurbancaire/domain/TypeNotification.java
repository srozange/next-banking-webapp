package fr.nextbanking.coeurbancaire.domain;

/**
 * Type d'une notification de virement : CONFIRMATION, ALERTE pour un virement exécuté, FRAUDE pour un virement suspendu.
 */
public enum TypeNotification {
    CONFIRMATION, ALERTE, FRAUDE
}
