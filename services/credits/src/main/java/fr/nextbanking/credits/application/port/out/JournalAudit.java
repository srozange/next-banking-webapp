package fr.nextbanking.credits.application.port.out;

import fr.nextbanking.credits.domain.EntreeAudit;

/**
 * Port sortant : journal d'audit des décisions de l'administrateur, par l'outbox d'audit ({@code outbox_audit}).
 */
public interface JournalAudit {

    /** Consigne l'entrée dans l'outbox d'audit, dans la transaction courante ; rend l'identifiant de la ligne. */
    long consigner(EntreeAudit entree);

    /** Publie la ligne après validation ; un échec la laisse au relais. */
    void publier(long idLigne);
}
