package fr.nextbanking.identiteclients.adapter.out.evenements;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relais planifié : republie chaque seconde les lignes d'outbox d'audit restées non publiées, dans l'ordre
 * de leur identifiant ; s'arrête à la première ligne en échec pour garder l'ordre.
 */
@Component
public class RelaisOutboxAudit {

    private final JournalAuditKafka journal;

    public RelaisOutboxAudit(JournalAuditKafka journal) {
        this.journal = journal;
    }

    @Scheduled(fixedDelay = 1000)
    public void relayer() {
        for (LigneOutboxAudit ligne : journal.nonPubliees()) {
            if (!journal.envoyer(ligne)) {
                return;
            }
        }
    }
}
