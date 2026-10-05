package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.TemoinEcrit;

/**
 * Port sortant : événements publiés par l'outbox.
 */
public interface EvenementsSortants {

    /** Consigne l'événement dans l'outbox, dans la transaction courante ; rend l'identifiant de la ligne. */
    long consigner(TemoinEcrit evenement);

    /** Publie la ligne d'outbox après validation ; un échec la laisse au relais. */
    void publier(long idLigne);
}
