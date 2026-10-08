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

    /** Virements actifs dont la prochaine exécution est atteinte, par prochaine exécution croissante puis identifiant. */
    List<VirementProgramme> dues(Instant maintenant);

    /**
     * Prend l'échéance : mise à jour conditionnelle ({@code id}, actif, prochaine exécution = échéance prévue) qui
     * avance la prochaine exécution, ajoute une exécution et enregistre la dernière. Vrai si une ligne a été modifiée
     * (faux : un autre passage l'a prise, aucun effet).
     */
    boolean prendreEcheance(long id, Instant echeancePrevue, Instant prochaine, Instant executeLe);

    /**
     * Désactive le virement : mise à jour conditionnelle ({@code id}, actif, prochaine exécution = échéance prévue)
     * qui passe {@code actif} à faux. Vrai si une ligne a été modifiée.
     */
    boolean desactiver(long id, Instant echeancePrevue);

    /**
     * Marque l'échéance comme notifiée (une notification au plus par virement et par échéance). Vrai si la marque a
     * été posée par cet appel, faux si l'échéance était déjà notifiée.
     */
    boolean marquerNotifiee(long id, Instant echeance, Instant le);

    /** Supprime la ligne du virement du client, quel que soit son état. Vrai si une ligne a été supprimée. */
    boolean supprimer(long id, long clientId);
}
