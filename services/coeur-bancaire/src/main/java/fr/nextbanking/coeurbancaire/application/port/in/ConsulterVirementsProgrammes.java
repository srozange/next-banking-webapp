package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.LigneVirementProgramme;
import java.util.List;

/** Port entrant : virements programmés du client authentifié. */
public interface ConsulterVirementsProgrammes {

    /** Virements du client, par prochaine exécution croissante. */
    List<LigneVirementProgramme> duClient(long clientId);
}
