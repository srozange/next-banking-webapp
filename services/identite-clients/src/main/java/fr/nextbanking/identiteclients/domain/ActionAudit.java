package fr.nextbanking.identiteclients.domain;

/**
 * Codes d'audit produits par le service identite-clients, avec le détail du legacy (FEAT-026, {@code MESSAGES.md}).
 */
public enum ActionAudit {
    LOGIN("Connexion réussie"),
    LOGIN_FAILED("Tentative échouée"),
    LOGOUT("Déconnexion"),
    ADMIN_DEACTIVATE("Désactivation client id=%d"),
    ADMIN_ACTIVATE("Activation client id=%d");

    private final String detail;

    ActionAudit(String detail) {
        this.detail = detail;
    }

    /** Texte du champ {@code detail} de l'entrée d'audit (modèle {@code %d} pour les actions sur un client). */
    public String detail() {
        return detail;
    }

    /** Détail d'une action portant sur le client donné. */
    public String detail(long clientId) {
        return String.format(detail, clientId);
    }
}
