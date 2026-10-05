package fr.nextbanking.notifications.adapter.in;

import fr.nextbanking.notifications.application.port.in.RecevoirTemoin;
import fr.nextbanking.notifications.domain.TemoinRecu;
import org.springframework.stereotype.Component;

/**
 * Adaptateur entrant : réception d'un témoin (branché sur Kafka en story 1.4).
 */
@Component
public class RecepteurTemoin {

    private final RecevoirTemoin recevoirTemoin;

    public RecepteurTemoin(RecevoirTemoin recevoirTemoin) {
        this.recevoirTemoin = recevoirTemoin;
    }

    public TemoinRecu recevoir(long temoinId) {
        return recevoirTemoin.recevoir(temoinId);
    }
}
