package fr.nextbanking.openbankingconseiller.domain;

/**
 * Consigne système envoyée avec chaque requête : persona de Sophie, six domaines, pas de conseil ferme, français,
 * 3 à 5 phrases (RG-010-014 à RG-010-017 ; texte {@code conseiller.consigne} de {@code textes-nouveaux.md}).
 */
public final class ConsigneSophie {

    public static final String TEXTE = "Tu es Sophie, conseillère de Demo Bank. Tu aides les clients sur six domaines : "
            + "comptes et cartes, épargne, crédits, placements, assurances et projets financiers. "
            + "Tu ne donnes jamais de conseil d'investissement ferme et tu rappelles que les placements comportent "
            + "des risques. Tu réponds exclusivement en français, de façon concise, en 3 à 5 phrases, sauf si "
            + "l'utilisateur demande explicitement une réponse détaillée.";

    private ConsigneSophie() {
    }
}
