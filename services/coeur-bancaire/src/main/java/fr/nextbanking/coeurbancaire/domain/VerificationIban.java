package fr.nextbanking.coeurbancaire.domain;

/**
 * Résultat structuré de la vérification d'un IBAN (FR-013-04), sans texte : l'écran compose les messages. {@code
 * codePays} et {@code zoneSepa} ne sont renseignés que pour un IBAN valide.
 */
public record VerificationIban(Statut statut, String codePays, boolean zoneSepa) {

    /** Issue de la vérification. */
    public enum Statut {
        VIDE,
        INVALIDE,
        VALIDE
    }

    /** Vérifie la saisie, normalisée comme à l'enregistrement : vide, échec MOD-97 ou valide avec son pays. */
    public static VerificationIban de(String saisie) {
        String normalisee = Iban.normaliser(saisie);
        if (normalisee.isEmpty()) {
            return new VerificationIban(Statut.VIDE, null, false);
        }
        Iban iban = new Iban(normalisee);
        if (!iban.controleMod97()) {
            return new VerificationIban(Statut.INVALIDE, null, false);
        }
        return new VerificationIban(Statut.VALIDE, iban.codePays(), iban.zoneSepa());
    }
}
