package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.ConsulterSuiviVirements;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.domain.LigneSuivi;
import java.util.List;

/** Cas d'usage : l'état des virements passés en revue du client authentifié (A-027-10). */
public class ConsulterSuiviVirementsCasUsage implements ConsulterSuiviVirements {

    /** Nombre de virements affichés dans « Suivi de mes virements ». */
    static final int PROFONDEUR = 4;

    private final DepotVirements virements;

    public ConsulterSuiviVirementsCasUsage(DepotVirements virements) {
        this.virements = virements;
    }

    @Override
    public List<LigneSuivi> suivi(long clientId) {
        return virements.suiviDuClient(clientId, PROFONDEUR);
    }
}
