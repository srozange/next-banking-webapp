package fr.nextbanking.coeurbancaire.domain;

/** Les huit signaux de fraude évalués pour chaque virement (FEAT-027). */
public enum TypeSignal {
    LARGE_AMOUNT,
    HIGH_FREQUENCY,
    NEW_BENEFICIARY,
    UNUSUAL_HOURS,
    ROUND_AMOUNT,
    FOREIGN_IBAN,
    RAPID_SUCCESSION,
    VELOCITY_CHECK
}
