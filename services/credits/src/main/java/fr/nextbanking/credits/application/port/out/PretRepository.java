package fr.nextbanking.credits.application.port.out;

import fr.nextbanking.credits.domain.EcheancePret;
import fr.nextbanking.credits.domain.NouveauPret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.StatutPret;
import java.util.List;
import java.util.Optional;

/**
 * Port sortant : prêts persistés.
 */
public interface PretRepository {

    /** Prêts du client, date de début décroissante. */
    List<Pret> parClient(long clientId);

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
}
