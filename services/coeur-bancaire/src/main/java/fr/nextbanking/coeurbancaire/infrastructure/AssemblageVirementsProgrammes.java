package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.ConsulterVirementsProgrammesCasUsage;
import fr.nextbanking.coeurbancaire.application.CreerVirementProgrammeCasUsage;
import fr.nextbanking.coeurbancaire.application.ControleFraudeCasUsage;
import fr.nextbanking.coeurbancaire.application.ExecuterEcheancesCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterEcheances;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.JournalBatch;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import fr.nextbanking.coeurbancaire.application.GererEtatVirementProgrammeCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterVirementsProgrammes;
import fr.nextbanking.coeurbancaire.application.port.in.GererEtatVirementProgramme;
import fr.nextbanking.coeurbancaire.application.port.in.CreerVirementProgramme;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.VirementsProgrammes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage des virements programmés, écrits en Java pur.
 */
@Configuration
public class AssemblageVirementsProgrammes {

    @Bean
    ConsulterVirementsProgrammes consulterVirementsProgrammes(VirementsProgrammes virements, Comptes comptes,
            Horloge horloge) {
        return new ConsulterVirementsProgrammesCasUsage(virements, comptes, horloge);
    }

    @Bean
    GererEtatVirementProgramme gererEtatVirementProgramme(VirementsProgrammes virements) {
        return new GererEtatVirementProgrammeCasUsage(virements);
    }

    @Bean
    ExecuterEcheances executerEcheances(VirementsProgrammes virements, Comptes comptes, Operations operations,
            DepotVirements depotVirements, JournalAudit journalAudit, JournalBatch journalBatch,
            Transaction transaction, EvenementsSortants evenements, Horloge horloge, SeuilsFraude seuils,
            ControleFraudeCasUsage controleFraude) {
        return new ExecuterEcheancesCasUsage(virements, comptes, operations, depotVirements, journalAudit,
                journalBatch, transaction, evenements, horloge, seuils, controleFraude);
    }

    @Bean
    CreerVirementProgramme creerVirementProgramme(VirementsProgrammes virements, Comptes comptes, Horloge horloge) {
        return new CreerVirementProgrammeCasUsage(virements, comptes, horloge);
    }
}
