package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.ConsulterPret;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.domain.Pret;

/**
 * Cas d'usage : un prêt n'est consultable que par son propriétaire.
 */
public class ConsulterPretCasUsage implements ConsulterPret {

    private final PretRepository prets;

    public ConsulterPretCasUsage(PretRepository prets) {
        this.prets = prets;
    }

    @Override
    public Pret consulter(long clientId, long pretId) {
        Pret pret = prets.parId(pretId).orElseThrow(() -> new PretIntrouvable(pretId));
        if (pret.clientId() != clientId) {
            throw new AccesPretRefuse(pretId);
        }
        return pret;
    }
}
