package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.CompteModifie;
import fr.nextbanking.coeurbancaire.domain.EcheanceNonExecutee;
import fr.nextbanking.coeurbancaire.domain.TemoinEcrit;
import fr.nextbanking.coeurbancaire.domain.VirementATraiter;
import fr.nextbanking.coeurbancaire.domain.VirementDecide;

/**
 * Port sortant : événements publiés par l'outbox.
 */
public interface EvenementsSortants {

    /** Consigne l'événement dans l'outbox, dans la transaction courante ; rend l'identifiant de la ligne. */
    long consigner(TemoinEcrit evenement);

    /** Consigne la remise d'un virement au traitement asynchrone, dans la transaction courante ; rend la ligne. */
    long consigner(VirementATraiter evenement);

    /** Consigne la décision d'un virement et sa notification, dans la transaction courante ; rend la ligne. */
    long consigner(VirementDecide evenement);

    /** Consigne une échéance non exécutée et sa notification, dans la transaction courante ; rend la ligne. */
    long consigner(EcheanceNonExecutee evenement);

    /** Consigne l'état d'un compte modifié (un événement par compte), dans la transaction courante ; rend la ligne. */
    long consigner(CompteModifie evenement);

    /** Publie la ligne d'outbox après validation ; un échec la laisse au relais. */
    void publier(long idLigne);
}
