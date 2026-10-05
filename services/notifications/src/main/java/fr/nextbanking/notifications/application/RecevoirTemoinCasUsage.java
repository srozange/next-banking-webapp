package fr.nextbanking.notifications.application;

import fr.nextbanking.notifications.application.port.in.RecevoirTemoin;
import fr.nextbanking.notifications.application.port.out.EvenementsTraites;
import fr.nextbanking.notifications.application.port.out.RegistreTemoinsRecus;
import fr.nextbanking.notifications.application.port.out.Transaction;
import fr.nextbanking.notifications.domain.TemoinRecu;
import java.util.UUID;

/**
 * Cas d'usage idempotent : dans une même transaction, marque l'événement traité et enregistre le témoin ;
 * un événement déjà traité est ignoré.
 */
public class RecevoirTemoinCasUsage implements RecevoirTemoin {

    private final RegistreTemoinsRecus registre;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;

    public RecevoirTemoinCasUsage(RegistreTemoinsRecus registre, EvenementsTraites evenementsTraites,
            Transaction transaction) {
        this.registre = registre;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
    }

    @Override
    public boolean recevoir(UUID idEvenement, long temoinId) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        TemoinRecu temoin = new TemoinRecu(temoinId);
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement)) {
                return false;
            }
            registre.enregistrer(temoin);
            return true;
        });
    }
}
