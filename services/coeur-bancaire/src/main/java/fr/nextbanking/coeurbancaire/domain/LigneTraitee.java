package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;

/** Virement traité (libéré ou rejeté) de l'historique de l'administrateur : sa ligne de revue, son issue et sa date. */
public record LigneTraitee(LigneRevue ligne, IssueRevue issue, Instant dateIssue) {
}
