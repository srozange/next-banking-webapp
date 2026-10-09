package fr.nextbanking.documents.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Données d'un document à imprimer : le titulaire (nom complet de la projection des clients), le compte concerné
 * (absent pour un document du client, comme l'IFU) avec son solde, et la date du jour du port Horloge.
 */
public record DonneesDocument(long clientId, String titulaire, Long compteId, String numeroCompte, String iban,
        BigDecimal solde, LocalDate date) {

    public DonneesDocument {
        if (titulaire == null || date == null) {
            throw new IllegalArgumentException("Le titulaire et la date du document sont obligatoires.");
        }
    }
}
