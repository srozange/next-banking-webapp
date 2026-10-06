package fr.nextbanking.coeurbancaire.domain;

import java.math.BigDecimal;

/**
 * Seuils {@code nb.fraude.*} du contrôle de fraude, validés au démarrage (NFR-SEC-08) : une valeur absente, nulle ou
 * négative est invalide.
 *
 * @param scoreCumuleMax somme des scores au-delà de laquelle des alertes sont écrites (75)
 * @param montantEleve seuil de LARGE_AMOUNT à 40 (5 000)
 * @param montantTresEleve seuil de LARGE_AMOUNT à 80 (15 000)
 * @param plafondQuotidien opérations du jour au-delà desquelles HIGH_FREQUENCY est levé (8)
 * @param successionRapideSecondes écart en dessous duquel RAPID_SUCCESSION est levé (30)
 * @param velociteFenetreMinutes fenêtre de VELOCITY_CHECK (60)
 * @param velociteNombreMax nombre de virements importants à partir duquel VELOCITY_CHECK est levé (5)
 * @param velociteMontant montant au-dessus duquel un virement exécuté compte pour VELOCITY_CHECK (500, strictement)
 */
public record ParametresControleFraude(int scoreCumuleMax, BigDecimal montantEleve, BigDecimal montantTresEleve,
        int plafondQuotidien, int successionRapideSecondes, int velociteFenetreMinutes, int velociteNombreMax,
        BigDecimal velociteMontant) {

    public ParametresControleFraude {
        exigerPositif("nb.fraude.score-cumule-max", scoreCumuleMax);
        exigerPositif("nb.fraude.montant-eleve", montantEleve);
        exigerPositif("nb.fraude.montant-tres-eleve", montantTresEleve);
        exigerPositif("nb.fraude.plafond-quotidien", plafondQuotidien);
        exigerPositif("nb.fraude.succession-rapide-secondes", successionRapideSecondes);
        exigerPositif("nb.fraude.velocite-fenetre-minutes", velociteFenetreMinutes);
        exigerPositif("nb.fraude.velocite-nombre-max", velociteNombreMax);
        exigerPositif("nb.fraude.velocite-montant", velociteMontant);
    }

    private static void exigerPositif(String propriete, int valeur) {
        if (valeur <= 0) {
            throw invalide(propriete);
        }
    }

    private static void exigerPositif(String propriete, BigDecimal valeur) {
        if (valeur == null || valeur.signum() <= 0) {
            throw invalide(propriete);
        }
    }

    private static IllegalArgumentException invalide(String propriete) {
        return new IllegalArgumentException("La propriété " + propriete + " doit être strictement positive.");
    }
}
