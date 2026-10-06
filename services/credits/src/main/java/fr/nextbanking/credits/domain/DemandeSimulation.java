package fr.nextbanking.credits.domain;

import fr.nextbanking.credits.domain.SimulationInvalide.Motif;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Données validées d'une simulation : montant strictement positif, taux annuel de 0 à 30 %, durée de 1 à 360 mois
 * (RG-007-003 à RG-007-005, FR-004-05).
 */
public record DemandeSimulation(BigDecimal montant, BigDecimal tauxAnnuel, int dureeMois) {

    static final BigDecimal TAUX_MAXIMUM = BigDecimal.valueOf(30);
    static final int DUREE_MAXIMUM = 360;
    /** Un montant tient dans numeric(19,2) : 17 chiffres avant la virgule au plus. */
    static final int CHIFFRES_ENTIERS_MAXIMUM = 17;

    /**
     * Lit les trois champs saisis, dans l'ordre montant, taux, durée.
     *
     * @throws SimulationInvalide au premier champ absent, non numérique ou hors limites
     */
    public static DemandeSimulation lire(String montant, String tauxAnnuel, String dureeMois) {
        BigDecimal montantLu = nombre(montant);
        if (montantLu == null || montantLu.precision() - montantLu.scale() > CHIFFRES_ENTIERS_MAXIMUM
                || montantLu.setScale(2, RoundingMode.HALF_UP).signum() <= 0) {
            throw new SimulationInvalide(Motif.MONTANT);
        }
        BigDecimal tauxLu = nombre(tauxAnnuel);
        if (tauxLu == null || tauxLu.signum() < 0 || tauxLu.compareTo(TAUX_MAXIMUM) > 0) {
            throw new SimulationInvalide(Motif.TAUX);
        }
        int dureeLue = entier(dureeMois);
        if (dureeLue <= 0 || dureeLue > DUREE_MAXIMUM) {
            throw new SimulationInvalide(Motif.DUREE);
        }
        return new DemandeSimulation(montantLu.setScale(2, RoundingMode.HALF_UP), tauxLu, dureeLue);
    }

    private static BigDecimal nombre(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(valeur.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Entier lu, ou 0 (refusé comme hors limites) si la valeur est absente, non entière ou trop grande. */
    private static int entier(String valeur) {
        if (valeur == null) {
            return 0;
        }
        try {
            return Integer.parseInt(valeur.strip());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
