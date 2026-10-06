package fr.nextbanking.credits.domain;

/**
 * Notification rendue par credits pour le centre de notifications : le service des notifications la stocke sans
 * règle métier (textes du legacy, titres pret.notif.titre.approuve / pret.notif.titre.refuse).
 */
public record Notification(long clientId, TypeNotification type, String titre, String message) {

    static final String LIBELLE_INCONNU = "(type non reconnu)";
    static final String TITRE_APPROUVE = "Demande de prêt approuvée";
    static final String TITRE_REFUSE = "Demande de prêt refusée";

    /** Notification de la décision prise sur le prêt. */
    public static Notification de(Pret pret, DecisionScoring decision) {
        String libelle = pret.type() == null ? LIBELLE_INCONNU : pret.type().libelle();
        String debut = "Votre demande de pret " + libelle + " de " + pret.montant().toPlainString() + " EUR a ete ";
        if (decision == DecisionScoring.APPROUVE) {
            return new Notification(pret.clientId(), TypeNotification.CONFIRMATION, TITRE_APPROUVE,
                    debut + "approuvee par le service credit.");
        }
        return new Notification(pret.clientId(), TypeNotification.ALERTE, TITRE_REFUSE,
                debut + "refusee (plafond depasse ou duree excessive).");
    }
}
