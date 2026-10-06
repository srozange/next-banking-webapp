package fr.nextbanking.credits.adapter.out.evenements;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Relais planifié : republie chaque seconde les lignes d'outbox restées non publiées, dans l'ordre
 * de leur identifiant ; s'arrête à la première ligne en échec pour garder l'ordre.
 */
@Component
public class RelaisOutbox {

    private final EvenementsSortantsKafka evenements;

    public RelaisOutbox(EvenementsSortantsKafka evenements) {
        this.evenements = evenements;
    }

    @Scheduled(fixedDelay = 1000)
    public void relayer() {
        for (LigneOutbox ligne : evenements.nonPubliees()) {
            if (!evenements.envoyer(ligne)) {
                return;
            }
        }
    }
}
