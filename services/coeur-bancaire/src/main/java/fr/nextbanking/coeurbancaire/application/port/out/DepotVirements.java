package fr.nextbanking.coeurbancaire.application.port.out;

import fr.nextbanking.coeurbancaire.domain.DecisionRevue;
import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.LigneRevue;
import fr.nextbanking.coeurbancaire.domain.LigneSuivi;
import fr.nextbanking.coeurbancaire.domain.LigneTraitee;
import fr.nextbanking.coeurbancaire.domain.Virement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : virements enregistrés, avec leur clé d'idempotence.
 */
public interface DepotVirements {

    /** Réserve l'identifiant du prochain virement ; il n'est jamais repris, même si la transaction est annulée. */
    long attribuerIdentifiant();

    /**
     * Enregistre le virement avec sa clé d'idempotence (absente : aucune).
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement de cause {@code CLE_IDEMPOTENCE_DEJA_UTILISEE} si le
     *         client a déjà un virement sous cette clé
     */
    void enregistrer(Virement virement, String cleIdempotence);

    /** Virement du client enregistré sous cette clé d'idempotence. */
    Optional<Virement> parCle(long clientId, String cleIdempotence);

    /** Virement d'identifiant donné. */
    Optional<Virement> parId(long virementId);

    /**
     * Transition conditionnelle ({@code UPDATE … WHERE id = ? AND etat = ?}) : la première gagne, zéro ligne modifiée
     * est une transition refusée (faux). La décision, quand il y en a une, est enregistrée avec la transition.
     */
    boolean transiter(long virementId, EtatVirement de, EtatVirement vers, DecisionRevue decision);

    /** Virements suspendus dont l'heure butoir est atteinte, du plus ancien au plus récent. */
    List<Virement> suspendusEchus(Instant maintenant);

    /** File de revue : virements suspendus, heure butoir la plus proche d'abord. */
    List<LigneRevue> fileDeRevue();

    /** Historique : les {@code limite} derniers virements libérés ou rejetés, du plus récent au plus ancien. */
    List<LigneTraitee> historiqueDesTraites(int limite);

    /** Virements du client passés en revue (suspendus à la soumission), les {@code limite} plus récents d'abord. */
    List<LigneSuivi> suiviDuClient(long clientId, int limite);
}
