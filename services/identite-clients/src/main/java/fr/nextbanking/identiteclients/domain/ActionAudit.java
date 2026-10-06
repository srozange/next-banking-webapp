package fr.nextbanking.identiteclients.domain;

/**
 * Codes d'audit produits par le service identite-clients, avec le détail du legacy (FEAT-026, {@code MESSAGES.md}).
 */
public enum ActionAudit {
    LOGIN("Connexion réussie"),
    LOGIN_FAILED("Tentative échouée"),
    LOGOUT("Déconnexion");

    private final String detail;

    ActionAudit(String detail) {
        this.detail = detail;
    }

    /** Texte du champ {@code detail} de l'entrée d'audit. */
    public String detail() {
        return detail;
    }
}
