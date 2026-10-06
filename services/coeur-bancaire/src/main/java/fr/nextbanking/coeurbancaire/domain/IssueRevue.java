package fr.nextbanking.coeurbancaire.domain;

/** Issue d'un virement passé en revue, affichée dans l'historique des virements traités. */
public enum IssueRevue {
    LIBERE,
    REJETE,
    REJETE_HEURE_BUTOIR,
    REJETE_SOLDE_AU_DEBIT
}
