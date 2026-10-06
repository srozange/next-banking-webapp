package fr.nextbanking.credits.infrastructure;

import fr.nextbanking.credits.application.ConsulterEcheancesCasUsage;
import fr.nextbanking.credits.application.ConsulterPretCasUsage;
import fr.nextbanking.credits.application.DecisionPretCasUsage;
import fr.nextbanking.credits.application.DeposerDemandePretCasUsage;
import fr.nextbanking.credits.application.ListerPretsCasUsage;
import fr.nextbanking.credits.application.PayerEcheanceCasUsage;
import fr.nextbanking.credits.application.SimulerEmpruntCasUsage;
import fr.nextbanking.credits.application.SupprimerPretCasUsage;
import fr.nextbanking.credits.application.port.in.ConsulterEcheances;
import fr.nextbanking.credits.application.port.in.ConsulterPret;
import fr.nextbanking.credits.application.port.in.DecisionPret;
import fr.nextbanking.credits.application.port.in.DeposerDemandePret;
import fr.nextbanking.credits.application.port.in.ListerPrets;
import fr.nextbanking.credits.application.port.in.PayerEcheance;
import fr.nextbanking.credits.application.port.in.SimulerEmprunt;
import fr.nextbanking.credits.application.port.in.SupprimerPret;
import fr.nextbanking.credits.application.port.out.CalculSimulation;
import fr.nextbanking.credits.application.port.out.EvenementsSortants;
import fr.nextbanking.credits.application.port.out.EvenementsTraites;
import fr.nextbanking.credits.application.port.out.Horloge;
import fr.nextbanking.credits.application.port.out.PretRepository;
import fr.nextbanking.credits.application.port.out.Transaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage de simulation, de dépôt de demande, de décision et de consultation des prêts, écrits en Java pur.
 */
@Configuration
public class AssemblageCredits {

    @Bean
    SimulerEmprunt simulerEmprunt(CalculSimulation calcul) {
        return new SimulerEmpruntCasUsage(calcul);
    }

    @Bean
    DeposerDemandePret deposerDemandePret(PretRepository prets, EvenementsSortants evenements,
            Transaction transaction, Horloge horloge) {
        return new DeposerDemandePretCasUsage(prets, evenements, transaction, horloge);
    }

    @Bean
    DecisionPret decisionPret(PretRepository prets, EvenementsTraites traites, EvenementsSortants evenements,
            Transaction transaction, Horloge horloge) {
        return new DecisionPretCasUsage(prets, traites, evenements, transaction, horloge);
    }

    @Bean
    ListerPrets listerPrets(PretRepository prets) {
        return new ListerPretsCasUsage(prets);
    }

    @Bean
    ConsulterPret consulterPret(PretRepository prets) {
        return new ConsulterPretCasUsage(prets);
    }

    @Bean
    ConsulterEcheances consulterEcheances(PretRepository prets) {
        return new ConsulterEcheancesCasUsage(prets);
    }

    @Bean
    PayerEcheance payerEcheance(PretRepository prets, Transaction transaction, Horloge horloge) {
        return new PayerEcheanceCasUsage(prets, transaction, horloge);
    }

    @Bean
    SupprimerPret supprimerPret(PretRepository prets) {
        return new SupprimerPretCasUsage(prets);
    }
}
