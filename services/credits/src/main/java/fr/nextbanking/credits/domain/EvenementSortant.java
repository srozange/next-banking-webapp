package fr.nextbanking.credits.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement publié par credits via l'outbox, clé = identifiant du prêt.
 */
public sealed interface EvenementSortant permits PretDemande, PretDecide {

    UUID idEvenement();

    long pretId();

    Instant horodatage();
}
