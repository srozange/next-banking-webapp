package fr.nextbanking.documents.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Données d'un document à imprimer : le titulaire (nom complet de la projection des clients), le compte concerné
 * (absent pour un document du client, comme l'IFU) avec son solde, la date du jour du port Horloge, et selon le type
 * le relevé du mois ou l'IFU de l'année (absents pour le RIB et l'attestation).
 */
public record DonneesDocument(long clientId, String titulaire, Long compteId, String numeroCompte, String iban,
        BigDecimal solde, LocalDate date, Releve releve, Ifu ifu) {

    public DonneesDocument {
        if (titulaire == null || date == null) {
            throw new IllegalArgumentException("Le titulaire et la date du document sont obligatoires.");
        }
    }

    /** Données d'un RIB ou d'une attestation : ni relevé ni IFU. */
    public DonneesDocument(long clientId, String titulaire, Long compteId, String numeroCompte, String iban,
            BigDecimal solde, LocalDate date) {
        this(clientId, titulaire, compteId, numeroCompte, iban, solde, date, null, null);
    }
}
