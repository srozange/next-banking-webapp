package fr.nextbanking.notifications.application.port.out;

import fr.nextbanking.notifications.domain.TemoinRecu;

/**
 * Port sortant : conserver les témoins reçus.
 */
public interface RegistreTemoinsRecus {

    void enregistrer(TemoinRecu temoin);
}
