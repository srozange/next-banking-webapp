package fr.nextbanking.coeurbancaire.application.port.out;

/**
 * Port sortant : plafonds mensuels de dépenses par catégorie (lecture seule jusqu'à la story 4.13).
 */
public interface Plafonds {

    /** Nombre de plafonds définis par le client pour le mois (AAAA-MM). */
    long compter(long clientId, String mois);
}
