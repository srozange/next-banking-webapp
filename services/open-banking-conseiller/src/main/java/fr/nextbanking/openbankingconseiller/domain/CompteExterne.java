package fr.nextbanking.openbankingconseiller.domain;

import java.math.BigDecimal;

/**
 * Compte détenu dans une autre banque, tel que Powens le décrit. Un solde absent ({@code null}) est distinct de zéro.
 */
public record CompteExterne(long id, String nom, String type, BigDecimal solde, String iban) {
}
