package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.ActionAudit;
import fr.nextbanking.coeurbancaire.domain.EntreeAudit;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Écriture des entrées d'audit d'un virement (FR-026-03, FR-026-04) : toutes dans {@code outbox_audit}, dans une
 * transaction indépendante de celle du virement, dans l'ordre, puis publiées. Un échec est journalisé : il ne défait
 * jamais la décision déjà validée.
 */
final class EcritureAudit {

    private static final Logger JOURNAL = Logger.getLogger(EcritureAudit.class.getName());

    /** Entrée à écrire : utilisateur, adresse IP (absente pour le système), code et détail. */
    record Entree(String utilisateur, String adresseIp, ActionAudit action, String detail) {
    }

    private final JournalAudit audit;
    private final Transaction transaction;

    EcritureAudit(JournalAudit audit, Transaction transaction) {
        this.audit = audit;
        this.transaction = transaction;
    }

    void ecrire(long virementId, Instant horodatage, List<Entree> entrees) {
        try {
            List<Long> lignes = transaction.executerIndependamment(() -> entrees.stream()
                    .map(entree -> audit.consigner(new EntreeAudit(UUID.randomUUID(), entree.utilisateur(),
                            entree.action(), entree.adresseIp(), horodatage, entree.detail(), virementId)))
                    .toList());
            lignes.forEach(audit::publier);
        } catch (RuntimeException echec) {
            JOURNAL.log(Level.SEVERE, "Entrée d'audit du virement " + virementId + " non écrite", echec);
        }
    }
}
