package fr.nextbanking.credits.application.port.in;

import fr.nextbanking.credits.domain.Pret;

/**
 * Port entrant : déposer une demande de prêt à partir des champs saisis.
 */
public interface DeposerDemandePret {

    /**
     * Enregistre le prêt EN_ATTENTE et sa transmission au scoring dans une même transaction.
     *
     * @throws fr.nextbanking.credits.domain.DemandePretInvalide si un champ est refusé (rien n'est écrit)
     * @throws fr.nextbanking.credits.application.TransmissionEnEchec si la ligne d'outbox n'a pas pu être écrite
     * @throws fr.nextbanking.credits.application.DemandePretEnEchec si l'enregistrement échoue autrement
     */
    Pret deposer(long clientId, String type, String montant, String tauxAnnuel, String dureeMois, String objet);
}
