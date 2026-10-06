package fr.nextbanking.identiteclients.application.port.out;

import fr.nextbanking.identiteclients.domain.EntreeAudit;

/**
 * Port sortant : entrées d'audit publiées par l'outbox d'audit ({@code outbox_audit}).
 */
public interface JournalAudit {

    /** Consigne l'entrée dans l'outbox d'audit, dans la transaction courante ; rend l'identifiant de la ligne. */
    long consigner(EntreeAudit entree);

    /** Publie la ligne d'outbox d'audit après validation ; un échec la laisse au relais. */
    void publier(long idLigne);
}
