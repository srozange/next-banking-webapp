package fr.nextbanking.notifications.application.port.in;

import fr.nextbanking.notifications.domain.TemoinRecu;

/**
 * Port entrant : recevoir un témoin.
 */
public interface RecevoirTemoin {

    TemoinRecu recevoir(long temoinId);
}
