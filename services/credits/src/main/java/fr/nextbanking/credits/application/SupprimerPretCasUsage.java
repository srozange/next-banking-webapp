package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.SupprimerPret;
import fr.nextbanking.credits.application.port.out.PretRepository;

/**
 * Cas d'usage : supprime définitivement un prêt de son propriétaire, quel que soit son statut, avec ses échéances
 * (cascade en base). Aucune confirmation, aucun événement.
 */
public class SupprimerPretCasUsage implements SupprimerPret {

    private final PretRepository prets;

    public SupprimerPretCasUsage(PretRepository prets) {
        this.prets = prets;
    }

    @Override
    public void supprimer(long clientId, long pretId) {
        try {
            if (prets.parId(pretId).orElseThrow(() -> new PretIntrouvable(pretId)).clientId() != clientId) {
                throw new SuppressionRefusee(pretId);
            }
            prets.supprimer(pretId);
        } catch (PretIntrouvable | SuppressionRefusee refus) {
            throw refus;
        } catch (RuntimeException panne) {
            throw new SuppressionEnEchec(panne);
        }
    }
}
