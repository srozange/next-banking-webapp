package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.ConsulterEcheances;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.domain.Echeance;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.StatutPret;
import java.util.List;

/**
 * Cas d'usage : l'échéancier d'un prêt n'est lisible que par son propriétaire, et seulement pour un prêt EN_COURS
 * ou REMBOURSE (sinon la liste est vide).
 */
public class ConsulterEcheancesCasUsage implements ConsulterEcheances {

    private final PretRepository prets;

    public ConsulterEcheancesCasUsage(PretRepository prets) {
        this.prets = prets;
    }

    @Override
    public List<Echeance> consulter(long clientId, long pretId) {
        Pret pret = prets.parId(pretId).orElseThrow(() -> new PretIntrouvable(pretId));
        if (pret.clientId() != clientId) {
            throw new AccesPretRefuse(pretId);
        }
        if (pret.statut() != StatutPret.EN_COURS && pret.statut() != StatutPret.REMBOURSE) {
            return List.of();
        }
        return prets.echeances(pretId);
    }
}
