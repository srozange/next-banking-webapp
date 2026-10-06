package fr.nextbanking.audit.application;

import fr.nextbanking.audit.application.port.in.EnregistrerEntreeAudit;
import fr.nextbanking.audit.application.port.out.EvenementsTraites;
import fr.nextbanking.audit.application.port.out.JournalAudit;
import fr.nextbanking.audit.application.port.out.Transaction;
import fr.nextbanking.audit.domain.ActionAudit;
import fr.nextbanking.audit.domain.EntreeAudit;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Cas d'usage idempotent : un code hors catalogue est rejeté sans rien écrire ; sinon, dans une même transaction,
 * l'événement est marqué traité et l'entrée ajoutée au journal ; un événement déjà traité est ignoré.
 */
public class EnregistrerEntreeAuditCasUsage implements EnregistrerEntreeAudit {

    private final JournalAudit journal;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;

    public EnregistrerEntreeAuditCasUsage(JournalAudit journal, EvenementsTraites evenementsTraites,
            Transaction transaction) {
        this.journal = journal;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
    }

    @Override
    public Issue enregistrer(UUID idEvenement, String utilisateur, String action, String detail, String adresseIp,
            Instant horodatage) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        Optional<ActionAudit> code = ActionAudit.depuisCode(action);
        if (code.isEmpty()) {
            return Issue.CODE_INCONNU;
        }
        EntreeAudit entree = new EntreeAudit(code.get(), horodatage, detail, adresseIp, utilisateur);
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement)) {
                return Issue.DEJA_TRAITEE;
            }
            journal.enregistrer(entree);
            return Issue.ENREGISTREE;
        });
    }
}
