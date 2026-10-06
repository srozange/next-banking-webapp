package fr.nextbanking.credits.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Échéance non payée d'un prêt approuvé : numéro de 1 à N, date = début + numéro mois, montants de l'annuité.
 */
public record EcheancePret(int numero, LocalDate dateEcheance, BigDecimal capital, BigDecimal interets,
        BigDecimal total, BigDecimal capitalRestant) {

    /** Échéancier complet du prêt, une échéance par mois. */
    public static List<EcheancePret> echeancier(Pret pret) {
        return Annuite.simuler(new DemandeSimulation(pret.montant(), pret.tauxAnnuel(), pret.dureeMois()))
                .echeancier().stream()
                .map(ligne -> new EcheancePret(ligne.numero(), pret.dateDebut().plusMonths(ligne.numero()),
                        ligne.capital(), ligne.interets(), ligne.total(), ligne.capitalRestant()))
                .toList();
    }
}
