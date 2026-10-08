package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.GererEtatVirementProgramme;
import fr.nextbanking.coeurbancaire.application.port.out.VirementsProgrammes;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.ErreurVirementProgramme.Cause;
import java.util.logging.Logger;

/**
 * Cas d'usage : suspension, réactivation et suppression d'un virement programmé. Chaque action est conditionnelle
 * (propriété et état attendu dans la même requête) : un virement d'autrui, inexistant ou déjà dans l'état demandé
 * donne le même refus (BUG-013), sans indice d'existence. Le journal ne porte que des identifiants numériques.
 */
public class GererEtatVirementProgrammeCasUsage implements GererEtatVirementProgramme {

    private static final Logger JOURNAL = Logger.getLogger(GererEtatVirementProgrammeCasUsage.class.getName());

    private final VirementsProgrammes virements;

    public GererEtatVirementProgrammeCasUsage(VirementsProgrammes virements) {
        this.virements = virements;
    }

    @Override
    public void suspendre(long clientId, long virementId) {
        changer(clientId, virementId, true, "Suspension");
    }

    @Override
    public void reactiver(long clientId, long virementId) {
        changer(clientId, virementId, false, "Réactivation");
    }

    @Override
    public void supprimer(long clientId, long virementId) {
        if (!virements.supprimer(virementId, clientId)) {
            throw refus("Suppression", clientId, virementId);
        }
        JOURNAL.info(() -> "Virement programmé " + virementId + " du client " + clientId + " supprimé");
    }

    private void changer(long clientId, long virementId, boolean actifAttendu, String action) {
        if (!virements.changerEtat(virementId, clientId, actifAttendu)) {
            throw refus(action, clientId, virementId);
        }
        JOURNAL.info(() -> action + " du virement programmé " + virementId + " du client " + clientId + " appliquée");
    }

    private static ErreurVirementProgramme refus(String action, long clientId, long virementId) {
        JOURNAL.warning(() -> action + " refusée : virement programmé " + virementId + " introuvable pour le client "
                + clientId);
        return new ErreurVirementProgramme(Cause.INTROUVABLE);
    }
}
