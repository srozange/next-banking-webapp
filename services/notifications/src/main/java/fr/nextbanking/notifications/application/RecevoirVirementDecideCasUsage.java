package fr.nextbanking.notifications.application;

import fr.nextbanking.notifications.application.port.in.RecevoirVirementDecide;
import fr.nextbanking.notifications.application.port.out.EvenementsTraites;
import fr.nextbanking.notifications.application.port.out.Horloge;
import fr.nextbanking.notifications.application.port.out.Notifications;
import fr.nextbanking.notifications.application.port.out.Transaction;
import fr.nextbanking.notifications.domain.PlafondNotifications;
import java.util.UUID;

/**
 * Cas d'usage idempotent : dans une même transaction, marque l'événement traité, place la notification rendue par le
 * producteur en tête de la liste de l'utilisateur et supprime les plus anciennes au-delà du plafond. Aucun texte
 * composé, aucun seuil métier lu ici.
 */
public class RecevoirVirementDecideCasUsage implements RecevoirVirementDecide {

    private final Notifications notifications;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;
    private final Horloge horloge;
    private final PlafondNotifications plafond;

    public RecevoirVirementDecideCasUsage(Notifications notifications, EvenementsTraites evenementsTraites,
            Transaction transaction, Horloge horloge, PlafondNotifications plafond) {
        this.notifications = notifications;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
        this.horloge = horloge;
        this.plafond = plafond;
    }

    @Override
    public boolean recevoir(UUID idEvenement, long virementId, Notification notification) {
        if (idEvenement == null || notification == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement et la notification sont obligatoires.");
        }
        var recue = fr.nextbanking.notifications.domain.Notification.recue(virementId, notification.clientId(),
                notification.type(), notification.titre(), notification.message(), horloge.instant());
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement)) {
                return false;
            }
            notifications.ajouter(recue);
            notifications.conserverLesPlusRecentes(recue.clientId(), plafond.valeur());
            return true;
        });
    }
}
