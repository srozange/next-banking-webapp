package fr.nextbanking.coeurbancaire.domain;

/**
 * Notification de l'issue d'un virement, entièrement rendue par le cœur bancaire (AD-25) : le service
 * {@code notifications} la stocke telle quelle. Le titre n'a aucun texte défini pour un virement : il est absent
 * (point ouvert).
 */
public record NotificationVirement(long clientId, TypeNotification type, String titre, String message) {

    public NotificationVirement {
        if (type == null || message == null || message.isBlank()) {
            throw new IllegalArgumentException("Le type et le message de la notification sont obligatoires.");
        }
    }

    /**
     * Notification d'un virement exécuté : ALERTE à partir du seuil d'alerte, CONFIRMATION en dessous ; le message de
     * confirmation reprend le motif quand il existe.
     */
    public static NotificationVirement pourExecution(Virement virement, String numeroDestinataire,
            SeuilsFraude seuils) {
        String montant = virement.montant().toPlainString();
        if (virement.montant().compareTo(seuils.seuilAlerte()) >= 0) {
            return new NotificationVirement(virement.clientId(), TypeNotification.ALERTE, null,
                    TextesVirement.transactionImportante(montant));
        }
        return new NotificationVirement(virement.clientId(), TypeNotification.CONFIRMATION, null,
                TextesVirement.confirmation(montant, numeroDestinataire, virement.motif()));
    }
}
