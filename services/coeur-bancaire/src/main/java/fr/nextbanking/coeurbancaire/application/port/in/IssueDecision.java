package fr.nextbanking.coeurbancaire.application.port.in;

/** Issue d'une décision de l'administrateur : acceptée, ou refusée avec rejet du virement sans aucun débit. */
public enum IssueDecision {
    ACCEPTEE,
    REFUSEE_SOLDE_INSUFFISANT,
    REFUSEE_HEURE_BUTOIR
}
