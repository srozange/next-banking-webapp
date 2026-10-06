package fr.nextbanking.identiteclients.application;

import fr.nextbanking.identiteclients.application.port.in.ChangerEtatClient;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import fr.nextbanking.identiteclients.domain.AccesRefuse;
import fr.nextbanking.identiteclients.domain.ClientIntrouvable;
import fr.nextbanking.identiteclients.domain.EntreeAudit;
import fr.nextbanking.identiteclients.domain.Role;
import java.util.Set;
import java.util.UUID;

/**
 * Cas d'usage : exige le rôle ADMIN (refus sans audit), vérifie que le client existe (sans audit), puis, dans une
 * transaction, consigne l'entrée d'audit ADMIN_DEACTIVATE ou ADMIN_ACTIVATE dans une transaction indépendante,
 * conservée même si l'écriture de {@code active} échoue, avant de modifier l'état. L'entrée est publiée après
 * validation.
 */
public class ChangerEtatClientCasUsage implements ChangerEtatClient {

    private final DepotClients depot;
    private final JournalAudit journal;
    private final Transaction transaction;
    private final Horloge horloge;

    public ChangerEtatClientCasUsage(DepotClients depot, JournalAudit journal, Transaction transaction,
            Horloge horloge) {
        this.depot = depot;
        this.journal = journal;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public void desactiver(long acteurId, Set<Role> rolesActeur, long clientId, String adresseIp) {
        changer(rolesActeur, clientId, false, () -> EntreeAudit.desactivationClient(acteurId, clientId, adresseIp,
                UUID.randomUUID(), horloge.instant()));
    }

    @Override
    public void activer(long acteurId, Set<Role> rolesActeur, long clientId, String adresseIp) {
        changer(rolesActeur, clientId, true, () -> EntreeAudit.activationClient(acteurId, clientId, adresseIp,
                UUID.randomUUID(), horloge.instant()));
    }

    private void changer(Set<Role> rolesActeur, long clientId, boolean actif,
            java.util.function.Supplier<EntreeAudit> entree) {
        if (rolesActeur == null || !rolesActeur.contains(Role.ADMIN)) {
            throw new AccesRefuse();
        }
        long idLigne = transaction.executer(() -> {
            depot.parId(clientId).orElseThrow(ClientIntrouvable::new);
            EntreeAudit aConsigner = entree.get();
            long ligne = transaction.executerIndependamment(() -> journal.consigner(aConsigner));
            depot.changerActif(clientId, actif);
            return ligne;
        });
        journal.publier(idLigne);
    }
}
