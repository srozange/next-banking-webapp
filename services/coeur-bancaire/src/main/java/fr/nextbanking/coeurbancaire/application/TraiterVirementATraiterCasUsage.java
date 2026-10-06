package fr.nextbanking.coeurbancaire.application;

import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import java.util.UUID;

/**
 * Cas d'usage idempotent : consomme la remise d'un virement au traitement asynchrone. Dans une même transaction,
 * l'événement est marqué traité puis le virement est lu. Un virement exécuté est déjà débité : aucun débit et
 * aucune notification. (La libération d'un virement suspendu arrive avec la story 3.4.)
 */
public class TraiterVirementATraiterCasUsage implements TraiterVirementATraiter {

    private final DepotVirements virements;
    private final EvenementsTraites evenementsTraites;
    private final Transaction transaction;
    private final Horloge horloge;

    public TraiterVirementATraiterCasUsage(DepotVirements virements, EvenementsTraites evenementsTraites,
            Transaction transaction, Horloge horloge) {
        this.virements = virements;
        this.evenementsTraites = evenementsTraites;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public Issue traiter(UUID idEvenement, long virementId) {
        if (idEvenement == null) {
            throw new IllegalArgumentException("L'identifiant de l'événement est obligatoire.");
        }
        return transaction.executer(() -> {
            if (!evenementsTraites.marquerTraite(idEvenement, horloge.instant())) {
                return Issue.DEJA_TRAITE;
            }
            return virements.parId(virementId).isPresent() ? Issue.DEJA_EXECUTE : Issue.VIREMENT_INCONNU;
        });
    }
}
