package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.ListerPrets;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.domain.Pret;
import java.util.List;

/**
 * Cas d'usage : prêts du seul client demandeur, date de début décroissante (RG-007-037).
 */
public class ListerPretsCasUsage implements ListerPrets {

    private final PretRepository prets;

    public ListerPretsCasUsage(PretRepository prets) {
        this.prets = prets;
    }

    @Override
    public List<Pret> lister(long clientId) {
        return prets.parClient(clientId);
    }
}
