package fr.nextbanking.credits.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Prêt d'un client, désigné par son identifiant numérique (jamais par un numéro de compte).
 */
public record Pret(long id, long clientId, TypePret type, BigDecimal montant, BigDecimal tauxAnnuel, int dureeMois,
        BigDecimal mensualite, BigDecimal coutTotal, BigDecimal totalInterets, String objet, StatutPret statut,
        LocalDate dateDebut, LocalDate dateFin) {
}
