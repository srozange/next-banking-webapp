package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import java.math.BigDecimal;

/**
 * Port entrant : exécuter un virement immédiat entre deux comptes du client authentifié.
 */
public interface ExecuterVirement {

    /**
     * Exécute le virement, ou rend le premier résultat si la clé d'idempotence a déjà servi.
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement saisie ou compte refusé, solde insuffisant, ou
     *         remise au traitement asynchrone impossible
     */
    Resultat executer(Commande commande);

    /** Soumission : {@code clientId} vient du jeton, l'adresse IP d'origine est résolue par l'adaptateur entrant. */
    record Commande(long clientId, Long compteSourceId, Long compteDestinataireId, String montant, String motif,
            String cleIdempotence, String adresseIp) {
    }

    /** Résultat : état, statut d'affichage et écho des champs saisis. */
    record Resultat(long virementId, EtatVirement etat, StatutAffichage statut, long compteSourceId,
            long compteDestinataireId, BigDecimal montant, String motif) {
    }
}
