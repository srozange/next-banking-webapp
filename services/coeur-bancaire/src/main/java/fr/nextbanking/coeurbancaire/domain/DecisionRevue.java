package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;

/**
 * Décision prise sur un virement suspendu ou libéré : issue, décideur ({@code SYSTEM} pour un traitement), motif (absent
 * pour une décision du système) et date lue sur le port Horloge.
 */
public record DecisionRevue(IssueRevue issue, String decideur, String motif, Instant date) {

    /** Décideur d'un traitement du système. */
    public static final String SYSTEME = "SYSTEM";

    public DecisionRevue {
        if (issue == null || decideur == null || decideur.isBlank() || date == null) {
            throw new IllegalArgumentException("Une décision porte une issue, un décideur et une date.");
        }
    }
}
