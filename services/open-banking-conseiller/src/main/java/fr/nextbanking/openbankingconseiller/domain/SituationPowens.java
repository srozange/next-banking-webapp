package fr.nextbanking.openbankingconseiller.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Situation d'un client vis-à-vis de Powens, lue en base : état, comptes externes, compte sélectionné et ses
 * transactions.
 *
 * @param etat         état Powens (jeton, connexion, message)
 * @param comptes      comptes externes enregistrés
 * @param selectionne  identifiant du compte sélectionné, absent s'il n'y en a pas
 * @param transactions transactions enregistrées du compte sélectionné
 */
public record SituationPowens(EtatPowens etat, List<CompteExterne> comptes, Long selectionne,
        List<TransactionExterne> transactions) {

    public SituationPowens {
        comptes = List.copyOf(comptes);
        transactions = List.copyOf(transactions);
    }

    /** Client sans connexion ni compte. */
    public static SituationPowens vide() {
        return new SituationPowens(EtatPowens.nonConnecte(), List.of(), null, List.of());
    }

    /** Somme des soldes connus à 2 décimales : un solde absent n'est pas compté (BUG-042). */
    public BigDecimal patrimoineTotal() {
        return comptes.stream()
                .map(CompteExterne::solde)
                .filter(solde -> solde != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
