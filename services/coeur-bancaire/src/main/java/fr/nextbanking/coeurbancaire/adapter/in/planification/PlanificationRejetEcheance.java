package fr.nextbanking.coeurbancaire.adapter.in.planification;

import fr.nextbanking.coeurbancaire.application.port.in.RejeterEcheances;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Adaptateur entrant : déclenche toutes les minutes, en zone {@code Europe/Paris}, le rejet automatique des virements
 * suspendus dont l'heure butoir est atteinte, nuit et week-end compris (FR-027-17, NFR-BATCH-01). Désactivé en profil
 * {@code rejeu} : le rejeu du golden master ne doit rien rejeter tout seul.
 */
@Component
@Profile("!rejeu")
public class PlanificationRejetEcheance {

    private static final Logger JOURNAL = LoggerFactory.getLogger(PlanificationRejetEcheance.class);

    private final RejeterEcheances rejeterEcheances;

    public PlanificationRejetEcheance(RejeterEcheances rejeterEcheances) {
        this.rejeterEcheances = rejeterEcheances;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Europe/Paris")
    public void rejeterLesVirementsEchus() {
        int rejetes = rejeterEcheances.rejeterLesEchus();
        if (rejetes > 0) {
            JOURNAL.info("{} virement(s) suspendu(s) rejeté(s) à l'heure butoir", rejetes);
        }
    }
}
