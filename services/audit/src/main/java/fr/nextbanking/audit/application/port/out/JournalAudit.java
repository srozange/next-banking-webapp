package fr.nextbanking.audit.application.port.out;

import fr.nextbanking.audit.domain.EntreeAudit;

/**
 * Port sortant : journal d'audit ({@code audit_logs}), en écriture seule.
 */
public interface JournalAudit {

    /** Ajoute l'entrée au journal, dans la transaction courante. */
    void enregistrer(EntreeAudit entree);
}
