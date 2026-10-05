package fr.nextbanking.notifications.application;

import fr.nextbanking.notifications.application.port.in.RecevoirTemoin;
import fr.nextbanking.notifications.application.port.out.RegistreTemoinsRecus;
import fr.nextbanking.notifications.domain.TemoinRecu;

/**
 * Cas d'usage : valide le témoin reçu par le domaine, puis le confie au registre.
 */
public class RecevoirTemoinCasUsage implements RecevoirTemoin {

    private final RegistreTemoinsRecus registre;

    public RecevoirTemoinCasUsage(RegistreTemoinsRecus registre) {
        this.registre = registre;
    }

    @Override
    public TemoinRecu recevoir(long temoinId) {
        TemoinRecu temoin = new TemoinRecu(temoinId);
        registre.enregistrer(temoin);
        return temoin;
    }
}
