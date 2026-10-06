package fr.nextbanking.coeurbancaire.application.port.in;

import fr.nextbanking.coeurbancaire.domain.EtatVirement;
import fr.nextbanking.coeurbancaire.domain.StatutAffichage;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Port entrant : soumettre un virement immédiat entre deux comptes du client authentifié.
 */
public interface ExecuterVirement {

    /**
     * Soumet le virement, ou rend le premier résultat si la clé d'idempotence a déjà servi. Le contrôle de fraude
     * décide avant tout débit : EXÉCUTÉ, ou SUSPENDU (aucun débit).
     *
     * @throws fr.nextbanking.coeurbancaire.domain.ErreurVirement saisie ou compte refusé, solde insuffisant, remise
     *         au traitement asynchrone impossible, ou contrôle en erreur (virement REFUSÉ_ERREUR_CONTRÔLE enregistré)
     */
    Resultat executer(Commande commande);

    /** Soumission : {@code clientId} vient du jeton, l'adresse IP d'origine est résolue par l'adaptateur entrant. */
    record Commande(long clientId, Long compteSourceId, Long compteDestinataireId, String montant, String motif,
            String cleIdempotence, String adresseIp) {
    }

    /** Résultat : état, heure butoir (SUSPENDU seulement), statut d'affichage et écho des champs saisis. */
    record Resultat(long virementId, EtatVirement etat, Instant heureButoir, StatutAffichage statut,
            long compteSourceId, long compteDestinataireId, BigDecimal montant, String motif) {
    }
}
