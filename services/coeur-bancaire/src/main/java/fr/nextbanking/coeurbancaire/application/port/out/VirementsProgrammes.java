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

    /**
     * Passe {@code actif} de {@code actifAttendu} à son contraire, par mise à jour conditionnelle sur le virement du
     * client ; seul {@code actif} change. Vrai si une ligne a été modifiée.
     */
    boolean changerEtat(long id, long clientId, boolean actifAttendu);

    /** Supprime la ligne du virement du client, quel que soit son état. Vrai si une ligne a été supprimée. */
    boolean supprimer(long id, long clientId);
}
