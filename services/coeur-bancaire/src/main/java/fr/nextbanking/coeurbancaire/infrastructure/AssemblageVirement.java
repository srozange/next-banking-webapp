package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.ConsulterComptesCasUsage;
import fr.nextbanking.coeurbancaire.application.ExecuterVirementCasUsage;
import fr.nextbanking.coeurbancaire.application.TraiterVirementATraiterCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterComptes;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement;
import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage du virement, écrits en Java pur.
 */
@Configuration
public class AssemblageVirement {

    @Bean
    ExecuterVirement executerVirement(Comptes comptes, Operations operations, DepotVirements depotVirements,
            EvenementsSortants evenementsSortants, JournalAudit journalAudit, Transaction transaction,
            Horloge horloge, SeuilsFraude seuils) {
        return new ExecuterVirementCasUsage(comptes, operations, depotVirements, evenementsSortants, journalAudit,
                transaction, horloge, seuils);
    }

    @Bean
    ConsulterComptes consulterComptes(Comptes comptes) {
        return new ConsulterComptesCasUsage(comptes);
    }

    @Bean
    TraiterVirementATraiter traiterVirementATraiter(DepotVirements depotVirements,
            EvenementsTraites evenementsTraites, Transaction transaction, Horloge horloge) {
        return new TraiterVirementATraiterCasUsage(depotVirements, evenementsTraites, transaction, horloge);
    }
}
