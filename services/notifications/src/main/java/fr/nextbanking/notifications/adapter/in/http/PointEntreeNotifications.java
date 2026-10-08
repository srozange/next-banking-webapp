package fr.nextbanking.notifications.adapter.in.http;

import fr.nextbanking.notifications.application.port.in.ConsulterNotification;
import fr.nextbanking.notifications.application.port.in.EffacerToutesLesNotifications;
import fr.nextbanking.notifications.application.port.in.ListerNotifications;
import fr.nextbanking.notifications.application.port.in.MarquerToutesLues;
import fr.nextbanking.notifications.domain.NotificationDuClient;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur entrant HTTP : lecture et gestion des notifications de l'utilisateur désigné par le sujet du jeton, tout
 * rôle confondu. L'identité ne vient jamais d'un paramètre ni d'un corps ; un sujet qui n'est pas un identifiant
 * numérique donne 401.
 */
@RestController
public class PointEntreeNotifications {

    private final ListerNotifications listerNotifications;
    private final ConsulterNotification consulterNotification;
    private final MarquerToutesLues marquerToutesLues;
    private final EffacerToutesLesNotifications effacerToutesLesNotifications;

    public PointEntreeNotifications(ListerNotifications listerNotifications,
            ConsulterNotification consulterNotification, MarquerToutesLues marquerToutesLues,
            EffacerToutesLesNotifications effacerToutesLesNotifications) {
        this.listerNotifications = listerNotifications;
        this.consulterNotification = consulterNotification;
        this.marquerToutesLues = marquerToutesLues;
        this.effacerToutesLesNotifications = effacerToutesLesNotifications;
    }

    @GetMapping("/api/notifications/notifications")
    public List<NotificationJson> lister(@AuthenticationPrincipal Jwt jeton) {
        return listerNotifications.lister(clientId(jeton)).stream().map(NotificationJson::de).toList();
    }

    @GetMapping("/api/notifications/notifications/{notificationId}")
    public NotificationJson consulter(@AuthenticationPrincipal Jwt jeton, @PathVariable long notificationId) {
        return NotificationJson.de(consulterNotification.consulter(clientId(jeton), notificationId));
    }

    @PostMapping("/api/notifications/notifications/lecture")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toutMarquerLu(@AuthenticationPrincipal Jwt jeton) {
        marquerToutesLues.marquer(clientId(jeton));
    }

    @DeleteMapping("/api/notifications/notifications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void toutEffacer(@AuthenticationPrincipal Jwt jeton) {
        effacerToutesLesNotifications.effacer(clientId(jeton));
    }

    private static long clientId(Jwt jeton) {
        try {
            return Long.parseLong(jeton.getSubject());
        } catch (NumberFormatException e) {
            throw new AuthentificationRequise();
        }
    }

    /** Notification : champs en français, {@code creeLe} en instant ISO-8601 UTC. Le titre n'est pas servi. */
    public record NotificationJson(long id, String type, String message, String creeLe, boolean lue) {

        static NotificationJson de(NotificationDuClient notification) {
            return new NotificationJson(notification.id(), notification.type(), notification.message(),
                    notification.creeLe().truncatedTo(ChronoUnit.MILLIS).toString(), notification.lue());
        }
    }
}
