package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterRevue;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.domain.LigneRevue;
import fr.nextbanking.coeurbancaire.domain.LigneTraitee;
import fr.nextbanking.coeurbancaire.domain.Role;
import java.util.List;
import java.util.Set;

/** Cas d'usage : file de revue et historique des virements traités, pour l'administrateur seul (D6). */
public class ConsulterRevueCasUsage implements ConsulterRevue {

    /** Profondeur de l'historique des virements traités (décision du 2026-10-05). */
    static final int PROFONDEUR_HISTORIQUE = 50;

    private final DepotVirements virements;

    public ConsulterRevueCasUsage(DepotVirements virements) {
        this.virements = virements;
    }

    @Override
    public List<LigneRevue> fileDeRevue(Set<Role> roles) {
        DecisionAdministrateur.exigerAdmin(roles);
        return virements.fileDeRevue();
    }

    @Override
    public List<LigneTraitee> historique(Set<Role> roles) {
        DecisionAdministrateur.exigerAdmin(roles);
        return virements.historiqueDesTraites(PROFONDEUR_HISTORIQUE);
    }
}
