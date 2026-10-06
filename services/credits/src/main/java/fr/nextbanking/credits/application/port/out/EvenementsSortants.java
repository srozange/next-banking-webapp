package fr.nextbanking.credits.application.port.out;

import fr.nextbanking.credits.domain.PretDemande;

/**
 * Port sortant : événements publiés par l'outbox.
 */
public interface EvenementsSortants {

    /** Consigne l'événement dans l'outbox, dans la transaction courante ; rend l'identifiant de la ligne. */
    long consigner(PretDemande evenement);

    /** Publie la ligne d'outbox après validation ; un échec la laisse au relais. */
    void publier(long idLigne);
}
