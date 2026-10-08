package fr.nextbanking.coeurbancaire.domain;

/**
 * Fréquence d'un virement programmé, dans l'ordre proposé à l'écran (FR-004-08). Les noms sont ceux de l'énumération
 * du legacy et les valeurs échangées avec le front.
 */
public enum FrequenceVirement {
    HEBDOMADAIRE,
    MENSUEL,
    BIMESTRIEL,
    TRIMESTRIEL,
    SEMESTRIEL,
    ANNUEL
}
