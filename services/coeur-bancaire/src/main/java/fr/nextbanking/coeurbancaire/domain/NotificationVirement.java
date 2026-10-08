package fr.nextbanking.coeurbancaire.domain;

import java.time.ZoneId;

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
        String montant = virement.montant().texte();
        if (virement.montant().atteint(seuils.seuilAlerte())) {
            return new NotificationVirement(virement.clientId(), TypeNotification.ALERTE, null,
                    TextesVirement.transactionImportante(montant));
        }
        return new NotificationVirement(virement.clientId(), TypeNotification.CONFIRMATION, null,
                TextesVirement.confirmation(montant, numeroDestinataire, virement.motif().texte()));
    }

    /** Notification FRAUDE d'un virement suspendu : l'avis de suspension, sans mot « fraude », score ni signal. */
    public static NotificationVirement pourSuspension(Virement virement, String numeroDestinataire, ZoneId fuseau) {
        return new NotificationVirement(virement.clientId(), TypeNotification.FRAUDE, null,
                TextesVirement.avisSuspension(virement.montant().texte(), numeroDestinataire,
                        virement.heureButoir(), fuseau));
    }

    /** Notification ALERTE d'une échéance non exécutée pour solde insuffisant. */
    public static NotificationVirement pourEcheanceSoldeInsuffisant(long clientId, String libelleBeneficiaire) {
        return new NotificationVirement(clientId, TypeNotification.ALERTE, TextesVirement.TITRE_ECHEANCE_SOLDE,
                TextesVirement.messageEcheanceSolde(libelleBeneficiaire));
    }

    /** Notification ALERTE d'une échéance refusée : compte destinataire introuvable. */
    public static NotificationVirement pourEcheanceCompteIntrouvable(long clientId, String libelleBeneficiaire) {
        return new NotificationVirement(clientId, TypeNotification.ALERTE, TextesVirement.TITRE_ECHEANCE_INTROUVABLE,
                TextesVirement.messageEcheanceIntrouvable(libelleBeneficiaire));
    }
}
