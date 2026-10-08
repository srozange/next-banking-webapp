package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.DecisionAdministrateurPret;
import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.JournalAudit;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.EcheancePret;
import fr.nextbanking.credits.domain.EntreeAudit;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.Role;
import fr.nextbanking.credits.domain.StatutPret;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cas d'usage : décision manuelle de l'administrateur sur un prêt EN_ATTENTE. Rôle ADMIN exigé. Dans la transaction
 * métier : transition conditionnelle (EN_COURS avec échéancier, ou REFUSE) ; l'entrée d'audit est écrite dans
 * {@code outbox_audit} par une transaction indépendante, avant la fin de la transaction métier, de sorte qu'une erreur
 * ultérieure annule la décision mais laisse la ligne d'audit. Un prêt inconnu ou qui n'est plus EN_ATTENTE est ignoré
 * (ni échéancier ni audit). Aucun événement {@code pret-decide} ni notification.
 */
public class DecisionAdministrateurPretCasUsage implements DecisionAdministrateurPret {

    private final PretRepository prets;
    private final JournalAudit audit;
    private final Transaction transaction;
    private final Horloge horloge;

    public DecisionAdministrateurPretCasUsage(PretRepository prets, JournalAudit audit, Transaction transaction,
            Horloge horloge) {
        this.prets = prets;
        this.audit = audit;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public void approuver(Set<Role> roles, String administrateur, long pretId, String adresseIp) {
        decider(roles, administrateur, pretId, adresseIp, true);
    }

    @Override
    public void refuser(Set<Role> roles, String administrateur, long pretId, String adresseIp) {
        decider(roles, administrateur, pretId, adresseIp, false);
    }

    private void decider(Set<Role> roles, String administrateur, long pretId, String adresseIp, boolean approbation) {
        DroitsAdministrateur.exigerAdmin(roles);
        List<Long> lignesAudit = new ArrayList<>();
        try {
            transaction.executer(() -> {
                prets.parId(pretId).filter(pret -> pret.statut() == StatutPret.EN_ATTENTE)
                        .ifPresent(pret -> appliquer(pret, administrateur, adresseIp, approbation, lignesAudit));
                return null;
            });
        } catch (RuntimeException echec) {
            throw new DecisionAdministrateurEnEchec(echec);
        } finally {
            publier(lignesAudit);
        }
    }

    private void appliquer(Pret pret, String administrateur, String adresseIp, boolean approbation,
            List<Long> lignesAudit) {
        StatutPret vers = approbation ? StatutPret.EN_COURS : StatutPret.REFUSE;
        if (!prets.transitionner(pret.id(), StatutPret.EN_ATTENTE, vers)) {
            return;
        }
        EntreeAudit entree = EntreeAudit.decision(UUID.randomUUID(), administrateur, approbation, adresseIp,
                horloge.instant(), pret.id());
        lignesAudit.add(transaction.executerIndependamment(() -> audit.consigner(entree)));
        if (approbation) {
            prets.creerEcheancier(pret.id(), EcheancePret.echeancier(pret));
        }
    }

    // La ligne d'audit est validée même si la décision est annulée : elle est publiée dans tous les cas, et un échec de
    // publication ne défait rien (le relais republie la ligne).
    private void publier(List<Long> lignesAudit) {
        for (long ligne : lignesAudit) {
            try {
                audit.publier(ligne);
            } catch (RuntimeException panne) {
                // Laissée au relais.
            }
        }
    }
}
