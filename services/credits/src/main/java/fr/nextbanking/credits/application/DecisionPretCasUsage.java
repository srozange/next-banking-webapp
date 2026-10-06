package fr.nextbanking.credits.application;

import fr.nextbanking.credits.application.port.in.DecisionPret;
import fr.nextbanking.credits.application.port.out.EvenementsSortants;
import fr.nextbanking.credits.application.port.out.EvenementsTraites;
import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import fr.nextbanking.credits.domain.DecisionScoring;
import fr.nextbanking.credits.domain.EcheancePret;
import fr.nextbanking.credits.domain.Pret;
import fr.nextbanking.credits.domain.PretDecide;
import fr.nextbanking.credits.domain.Scoring;
import fr.nextbanking.credits.domain.StatutPret;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Cas d'usage : applique le scoring à un prêt EN_ATTENTE. Dans une même transaction : marque l'événement traité,
 * fait la transition conditionnelle (EN_COURS avec échéancier, ou REFUSE) et consigne {@code pret-decide} dans
 * l'outbox ; la publication suit la validation. Un prêt inconnu, ou qui n'est plus EN_ATTENTE, est ignoré sans
 * effet ni événement.
 */
public class DecisionPretCasUsage implements DecisionPret {

    private final PretRepository prets;
    private final EvenementsTraites traites;
    private final EvenementsSortants evenements;
    private final Transaction transaction;
    private final Horloge horloge;

    public DecisionPretCasUsage(PretRepository prets, EvenementsTraites traites, EvenementsSortants evenements,
            Transaction transaction, Horloge horloge) {
        this.prets = prets;
        this.traites = traites;
        this.evenements = evenements;
        this.transaction = transaction;
        this.horloge = horloge;
    }

    @Override
    public boolean decider(UUID idEvenement, long pretId) {
        Issue issue = transaction.executer(() -> {
            if (!traites.marquerTraite(idEvenement)) {
                return new Issue(false, OptionalLong.empty());
            }
            return new Issue(true, prets.parId(pretId)
                    .filter(pret -> pret.statut() == StatutPret.EN_ATTENTE)
                    .map(this::appliquer)
                    .orElse(OptionalLong.empty()));
        });
        if (issue.ligne().isPresent()) {
            try {
                evenements.publier(issue.ligne().getAsLong());
            } catch (RuntimeException panne) {
                // La décision est validée avec sa ligne d'outbox : le relais republiera la ligne.
            }
        }
        return issue.nouveau();
    }

    private OptionalLong appliquer(Pret pret) {
        DecisionScoring decision = Scoring.decider(pret);
        if (!prets.transitionner(pret.id(), StatutPret.EN_ATTENTE, PretDecide.statut(decision))) {
            return OptionalLong.empty();
        }
        if (decision == DecisionScoring.APPROUVE) {
            prets.creerEcheancier(pret.id(), EcheancePret.echeancier(pret));
        }
        return OptionalLong.of(
                evenements.consigner(PretDecide.de(UUID.randomUUID(), pret, decision, horloge.instant())));
    }

    private record Issue(boolean nouveau, OptionalLong ligne) {
    }
}
