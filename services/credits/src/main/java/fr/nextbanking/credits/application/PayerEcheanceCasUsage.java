package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.PayerEcheance;
import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.Echeance;
import fr.nextbanking.credits.domain.StatutPret;

/**
 * Cas d'usage : dans une transaction, verrouille la ligne du prêt (deux paiements concurrents des dernières
 * échéances se suivent), marque l'échéance payée par mise à jour conditionnelle (zéro ligne : déjà payée, rien ne
 * change), puis passe le prêt à REMBOURSE, par mise à jour conditionnelle sur EN_COURS, s'il ne reste aucune échéance
 * non payée. Aucun événement, aucun appel à un autre service, aucun débit.
 */
public class PayerEcheanceCasUsage implements PayerEcheance {

    private final PretRepository prets;
    private final Transaction transaction;
    private final Horloge horloge;

    public PayerEcheanceCasUsage(PretRepository prets, Transaction transaction, Horloge horloge) {
        this.prets = prets;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public Echeance payer(long clientId, long echeanceId) {
        try {
            return transaction.executer(() -> payerDansTransaction(clientId, echeanceId));
        } catch (EcheanceIntrouvable | PaiementRefuse refus) {
            throw refus;
        } catch (RuntimeException panne) {
            throw new PaiementEnEchec(panne);
        }
    }

    private Echeance payerDansTransaction(long clientId, long echeanceId) {
        Echeance echeance = prets.echeanceParId(echeanceId).orElseThrow(() -> new EcheanceIntrouvable(echeanceId));
        long pretId = echeance.pretId();
        if (prets.parId(pretId).map(pret -> pret.clientId() != clientId).orElse(true)) {
            throw new PaiementRefuse(echeanceId);
        }
        prets.verrouiller(pretId);
        prets.payerEcheance(echeanceId, horloge.instant());
        if (prets.nombreEcheancesNonPayees(pretId) == 0) {
            prets.transitionner(pretId, StatutPret.EN_COURS, StatutPret.REMBOURSE);
        }
        return prets.echeanceParId(echeanceId).orElseThrow(() -> new EcheanceIntrouvable(echeanceId));
    }
}
