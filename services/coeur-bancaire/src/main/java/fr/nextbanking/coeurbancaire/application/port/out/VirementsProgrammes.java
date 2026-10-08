package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.DemandeVirementProgramme;
import fr.nextbanking.coeurbancaire.domain.VirementProgramme;
import java.time.Instant;
import java.util.List;

/** Port sortant : virements programmés des clients. */
public interface VirementsProgrammes {

    /** Enregistre le virement du client, actif, à 0 exécution, et rend la ligne créée. */
    VirementProgramme enregistrer(long clientId, DemandeVirementProgramme demande, Instant prochaineExecution,
            Instant creeLe);

    /** Virements du client, par prochaine exécution croissante puis identifiant. */
    List<VirementProgramme> listerDuClient(long clientId);
}
