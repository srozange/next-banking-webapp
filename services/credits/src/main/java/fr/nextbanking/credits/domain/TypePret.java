package fr.nextbanking.credits.domain;

import java.math.BigDecimal;

/**
 * Types de prêt du legacy (RG-007-011), avec leur plafond de scoring (FR-024-04) et leur libellé de notification.
 */
public enum TypePret {
    IMMOBILIER("600000", "Immobilier"),
    AUTO("40000", "Automobile"),
    CONSOMMATION("25000", "Consommation"),
    TRAVAUX("80000", "Travaux"),
    ETUDIANT("20000", "Etudiant");

    private final BigDecimal plafond;
    private final String libelle;

    TypePret(String plafond, String libelle) {
        this.plafond = new BigDecimal(plafond);
        this.libelle = libelle;
    }

    /** Montant maximal approuvé automatiquement ; le montant égal au plafond est approuvé. */
    public BigDecimal plafond() {
        return plafond;
    }

    /** Libellé du type dans les notifications. */
    public String libelle() {
        return libelle;
    }
}
