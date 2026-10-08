package fr.nextbanking.coeurbancaire.domain;

import java.time.LocalDate;

/**
 * Virement programmé tel que l'écran le liste : numéro du compte source et prochaine exécution rendue en date du
 * fuseau métier.
 */
public record LigneVirementProgramme(long id, long compteSourceId, String numeroCompteSource,
        String numeroBeneficiaire, String libelleBeneficiaire, Montant montant, FrequenceVirement frequence,
        String motif, LocalDate prochaineExecution, boolean actif, int nbExecutions) {
}
