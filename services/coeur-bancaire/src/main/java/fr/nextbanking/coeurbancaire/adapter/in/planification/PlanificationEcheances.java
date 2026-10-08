package fr.nextbanking.coeurbancaire.adapter.in.planification;

import fr.nextbanking.coeurbancaire.application.port.in.ExecuterEcheances;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptateur entrant : déclenche toutes les 5 minutes, en zone {@code Europe/Paris}, l'exécution des échéances dues
 * des virements programmés (FR-021-01, NFR-BATCH-01). Désactivé en profil {@code rejeu} : le rejeu du golden master
 * ne doit rien exécuter tout seul.
 */
@Component
@Profile("!rejeu")
public class PlanificationEcheances {

    private final ExecuterEcheances executerEcheances;

    public PlanificationEcheances(ExecuterEcheances executerEcheances) {
        this.executerEcheances = executerEcheances;
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "Europe/Paris")
    public void executerLesEcheancesDues() {
        executerEcheances.executerLesDues();
    }
}
