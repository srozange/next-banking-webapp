package fr.nextbanking.credits.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcul d'un emprunt à annuité constante (RG-007-008 à RG-007-010) : arrondi HALF_UP à 2 décimales, coût total =
 * mensualité × durée, dernière ligne soldant le capital restant dû.
 */
public final class Annuite {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);
    private static final BigDecimal DOUZE = BigDecimal.valueOf(12);

    private Annuite() {
    }

    /** Simule l'emprunt : taux nul, mensualité = montant / durée ; sinon formule d'annuité constante (PMT). */
    public static Simulation simuler(DemandeSimulation demande) {
        BigDecimal montant = demande.montant();
        int duree = demande.dureeMois();
        BigDecimal tauxMensuel = demande.tauxAnnuel().divide(CENT, MathContext.DECIMAL128)
                .divide(DOUZE, MathContext.DECIMAL128);
        BigDecimal mensualite = tauxMensuel.signum() == 0
                ? montant.divide(BigDecimal.valueOf(duree), 2, RoundingMode.HALF_UP)
                : annuite(montant, tauxMensuel, duree);
        BigDecimal coutTotal = mensualite.multiply(BigDecimal.valueOf(duree));
        List<LignePret> echeancier = new ArrayList<>(duree);
        BigDecimal restant = montant;
        for (int numero = 1; numero <= duree; numero++) {
            BigDecimal interets = restant.multiply(tauxMensuel).setScale(2, RoundingMode.HALF_UP);
            BigDecimal capital = numero == duree ? restant : mensualite.subtract(interets);
            restant = restant.subtract(capital);
            echeancier.add(new LignePret(numero, capital, interets, capital.add(interets), restant));
        }
        return new Simulation(mensualite, coutTotal, coutTotal.subtract(montant), List.copyOf(echeancier));
    }

    private static BigDecimal annuite(BigDecimal montant, BigDecimal tauxMensuel, int duree) {
        BigDecimal facteur = BigDecimal.ONE.add(tauxMensuel).pow(duree, MathContext.DECIMAL128);
        return montant.multiply(tauxMensuel).multiply(facteur)
                .divide(facteur.subtract(BigDecimal.ONE), MathContext.DECIMAL128)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
