package fr.nextbanking.credits.domain;

import fr.nextbanking.credits.domain.DemandePretInvalide.Motif;
import java.math.BigDecimal;

/**
 * Données validées d'une demande de prêt : type connu, montant strictement positif, taux annuel de 0 à 30 %, durée de
 * 1 à 300 mois (301 à 360 refusés dès la soumission) et objet non vide (RG-007-011 à RG-007-019, FR-004-05).
 * La longueur de l'objet n'est pas contrôlée (bug du legacy conservé).
 */
public record DemandePret(TypePret type, BigDecimal montant, BigDecimal tauxAnnuel, int dureeMois, String objet) {

    static final int DUREE_MAXIMUM = 300;

    /**
     * Lit les champs saisis, dans l'ordre type, montant, taux, durée, objet.
     *
     * @throws DemandePretInvalide au premier champ absent, non numérique ou hors limites
     */
    public static DemandePret lire(String type, String montant, String tauxAnnuel, String dureeMois, String objet) {
        TypePret typeLu = type(type);
        BigDecimal montantLu;
        BigDecimal tauxLu;
        try {
            montantLu = DemandeSimulation.lireMontant(montant);
        } catch (SimulationInvalide refus) {
            throw new DemandePretInvalide(Motif.MONTANT);
        }
        try {
            tauxLu = DemandeSimulation.lireTaux(tauxAnnuel);
        } catch (SimulationInvalide refus) {
            throw new DemandePretInvalide(Motif.TAUX);
        }
        int dureeLue = DemandeSimulation.entier(dureeMois);
        if (dureeLue <= 0 || dureeLue > DUREE_MAXIMUM) {
            throw new DemandePretInvalide(Motif.DUREE);
        }
        if (objet == null || objet.isBlank()) {
            throw new DemandePretInvalide(Motif.OBJET);
        }
        return new DemandePret(typeLu, montantLu, tauxLu, dureeLue, objet);
    }

    private static TypePret type(String valeur) {
        if (valeur != null) {
            for (TypePret type : TypePret.values()) {
                if (type.name().equals(valeur.strip())) {
                    return type;
                }
            }
        }
        throw new DemandePretInvalide(Motif.TYPE);
    }
}
