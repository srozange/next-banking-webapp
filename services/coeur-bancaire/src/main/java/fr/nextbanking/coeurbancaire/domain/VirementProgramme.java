package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;

/**
 * Virement programmé d'un client : compte source, bénéficiaire interne désigné par son numéro de compte, montant,
 * fréquence, prochaine exécution, état et nombre d'exécutions déjà faites.
 */
public record VirementProgramme(long id, long clientId, long compteSourceId, String numeroBeneficiaire,
        String libelleBeneficiaire, Montant montant, FrequenceVirement frequence, String motif,
        Instant prochaineExecution, boolean actif, int nbExecutions, Instant creeLe) {
}
