package fr.nextbanking.credits.application.port.out;

import fr.nextbanking.credits.domain.Echeance;
import fr.nextbanking.credits.domain.EcheancePret;
import fr.nextbanking.credits.domain.NouveauPret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.StatutPret;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : prêts persistés.
 */
public interface PretRepository {

    /** Prêts du client, date de début décroissante. */
    List<Pret> parClient(long clientId);

    /** Prêts EN_ATTENTE de tous les clients, date de début décroissante. */
    List<Pret> enAttente();

    /** Tous les prêts, tous statuts, date de début décroissante. */
    List<Pret> tous();

    Optional<Pret> parId(long pretId);

    /** Enregistre le prêt EN_ATTENTE dans la transaction courante ; rend son identifiant. */
    long creer(NouveauPret pret);

    /**
     * Change le statut du prêt seulement s'il est encore au statut {@code de} (mise à jour conditionnelle) ; faux si
     * aucune ligne n'a changé.
     */
    boolean transitionner(long pretId, StatutPret de, StatutPret vers);

    /** Enregistre l'échéancier du prêt, échéances non payées, dans la transaction courante. */
    void creerEcheancier(long pretId, List<EcheancePret> echeances);

    /** Échéances du prêt, triées par numéro. */
    List<Echeance> echeances(long pretId);

    Optional<Echeance> echeanceParId(long echeanceId);

    /**
     * Marque l'échéance payée à l'instant donné seulement si elle ne l'est pas encore (mise à jour conditionnelle) ;
     * faux si elle était déjà payée (date inchangée).
     */
    boolean payerEcheance(long echeanceId, Instant datePaiement);

    /** Nombre d'échéances non payées du prêt. */
    long nombreEcheancesNonPayees(long pretId);

    /** Verrouille la ligne du prêt jusqu'à la fin de la transaction courante. */
    void verrouiller(long pretId);

    /** Supprime le prêt et, en cascade, ses échéances ; faux si le prêt n'existait pas. */
    boolean supprimer(long pretId);
}
