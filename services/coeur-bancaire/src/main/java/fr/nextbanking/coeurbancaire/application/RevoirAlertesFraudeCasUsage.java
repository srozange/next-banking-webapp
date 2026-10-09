package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.RevoirAlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.domain.AlerteAdministree;
import fr.nextbanking.coeurbancaire.domain.Role;
import fr.nextbanking.coeurbancaire.domain.StatutAlerte;
import java.util.List;
import java.util.Set;

/**
 * Cas d'usage : l'administrateur liste toutes les alertes de fraude (BUG-056 corrigé) et confirme une alerte ou la
 * classe en faux positif. La revue n'écrit que l'alerte : le virement suspendu, les comptes, l'outbox et l'audit ne
 * changent pas. Auteur enregistré par identifiant, heure lue sur l'horloge.
 */
public class RevoirAlertesFraudeCasUsage implements RevoirAlertesFraude {

    private final AlertesFraude alertes;
    private final Horloge horloge;

    public RevoirAlertesFraudeCasUsage(AlertesFraude alertes, Horloge horloge) {
        this.alertes = alertes;
        this.horloge = horloge;
    }

    @Override
    public List<AlerteAdministree> toutes(Set<Role> roles) {
        DecisionAdministrateur.exigerAdmin(roles);
        return alertes.toutes();
    }

    @Override
    public boolean revoir(Long alerteId, StatutAlerte issue, long acteurId, Set<Role> roles) {
        DecisionAdministrateur.exigerAdmin(roles);
        if (issue != StatutAlerte.CONFIRMED && issue != StatutAlerte.FALSE_POSITIVE) {
            throw new IllegalArgumentException("Issue de revue d'une alerte : CONFIRMED ou FALSE_POSITIVE.");
        }
        if (alerteId == null) {
            return false;
        }
        return alertes.revoir(alerteId, issue, Long.toString(acteurId), horloge.instant());
    }
}
