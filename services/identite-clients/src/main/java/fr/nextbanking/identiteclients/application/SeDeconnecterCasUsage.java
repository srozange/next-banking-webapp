package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.application.port.in.SeDeconnecter;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import fr.nextbanking.identiteclients.domain.EntreeAudit;
import java.util.UUID;

/**
 * Cas d'usage : consigne l'entrée d'audit LOGOUT dans une transaction, puis la publie après validation.
 * Aucune liste de révocation : le jeton reste accepté jusqu'à son expiration.
 */
public class SeDeconnecterCasUsage implements SeDeconnecter {

    private final JournalAudit journal;
    private final Transaction transaction;
    private final Horloge horloge;

    public SeDeconnecterCasUsage(JournalAudit journal, Transaction transaction, Horloge horloge) {
        this.journal = journal;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public void seDeconnecter(long clientId, String adresseIp) {
        EntreeAudit entree = EntreeAudit.deconnexion(clientId, adresseIp, UUID.randomUUID(), horloge.instant());
        long idLigne = transaction.executer(() -> journal.consigner(entree));
        journal.publier(idLigne);
    }
}
