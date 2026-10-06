package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.ConsulterComptesCasUsage;
import fr.nextbanking.coeurbancaire.application.ConsulterReleveCasUsage;
import fr.nextbanking.coeurbancaire.application.ConsulterTableauDeBordCasUsage;
import fr.nextbanking.coeurbancaire.application.ConsulterRevueCasUsage;
import fr.nextbanking.coeurbancaire.application.ConsulterSuiviVirementsCasUsage;
import fr.nextbanking.coeurbancaire.application.EcheanceRevueCasUsage;
import fr.nextbanking.coeurbancaire.application.LiberationVirementCasUsage;
import fr.nextbanking.coeurbancaire.application.RecevoirClientModifieCasUsage;
import fr.nextbanking.coeurbancaire.application.RejetVirementCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterRevue;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterSuiviVirements;
import fr.nextbanking.coeurbancaire.application.port.in.LibererVirement;
import fr.nextbanking.coeurbancaire.application.port.in.RecevoirClientModifie;
import fr.nextbanking.coeurbancaire.application.port.in.RejeterEcheances;
import fr.nextbanking.coeurbancaire.application.port.in.RejeterVirement;
import fr.nextbanking.coeurbancaire.application.port.out.JournalBatch;
import fr.nextbanking.coeurbancaire.application.port.out.ProjectionClients;
import fr.nextbanking.coeurbancaire.application.ControleFraudeCasUsage;
import fr.nextbanking.coeurbancaire.application.ExecuterVirementCasUsage;
import fr.nextbanking.coeurbancaire.application.TraiterVirementATraiterCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterComptes;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterReleve;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterTableauDeBord;
import fr.nextbanking.coeurbancaire.application.port.in.ExecuterVirement;
import fr.nextbanking.coeurbancaire.application.port.in.TraiterVirementATraiter;
import fr.nextbanking.coeurbancaire.application.port.out.AlertesFraude;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import fr.nextbanking.coeurbancaire.application.port.out.Comptes;
import fr.nextbanking.coeurbancaire.application.port.out.DepotVirements;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsSortants;
import fr.nextbanking.coeurbancaire.application.port.out.EvenementsTraites;
import fr.nextbanking.coeurbancaire.application.port.out.HistoriqueVirements;
import fr.nextbanking.coeurbancaire.application.port.out.Horloge;
import fr.nextbanking.coeurbancaire.application.port.out.JournalAudit;
import fr.nextbanking.coeurbancaire.application.port.out.Operations;
import fr.nextbanking.coeurbancaire.application.port.out.Transaction;
import fr.nextbanking.coeurbancaire.domain.ParametresControleFraude;
import fr.nextbanking.coeurbancaire.domain.SeuilsFraude;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage du virement, écrits en Java pur.
 */
@Configuration
public class AssemblageVirement {

    @Bean
    ControleFraudeCasUsage controleFraude(HistoriqueVirements historique, Beneficiaires beneficiaires,
            AlertesFraude alertes, Horloge horloge, ParametresControleFraude parametres) {
        return new ControleFraudeCasUsage(historique, beneficiaires, alertes, horloge, parametres);
    }

    @Bean
    ExecuterVirement executerVirement(Comptes comptes, Operations operations, DepotVirements depotVirements,
            EvenementsSortants evenementsSortants, JournalAudit journalAudit, Transaction transaction,
            Horloge horloge, SeuilsFraude seuils, ControleFraudeCasUsage controleFraude) {
        return new ExecuterVirementCasUsage(comptes, operations, depotVirements, evenementsSortants, journalAudit,
                transaction, horloge, seuils, controleFraude);
    }

    @Bean
    ConsulterComptes consulterComptes(Comptes comptes) {
        return new ConsulterComptesCasUsage(comptes);
    }

    @Bean
    ConsulterReleve consulterReleve(Comptes comptes, Operations operations) {
        return new ConsulterReleveCasUsage(comptes, operations);
    }

    @Bean
    ConsulterTableauDeBord consulterTableauDeBord(Comptes comptes, Operations operations) {
        return new ConsulterTableauDeBordCasUsage(comptes, operations);
    }

    @Bean
    TraiterVirementATraiter traiterVirementATraiter(DepotVirements depotVirements,
            EvenementsTraites evenementsTraites, Transaction transaction, Horloge horloge, Comptes comptes,
            Operations operations, JournalAudit journalAudit) {
        return new TraiterVirementATraiterCasUsage(depotVirements, evenementsTraites, transaction, horloge, comptes,
                operations, journalAudit);
    }

    @Bean
    LibererVirement libererVirement(Comptes comptes, DepotVirements depotVirements, AlertesFraude alertes,
            EvenementsSortants evenementsSortants, JournalAudit journalAudit, Transaction transaction,
            Horloge horloge) {
        return new LiberationVirementCasUsage(comptes, depotVirements, alertes, evenementsSortants, journalAudit,
                transaction, horloge);
    }

    @Bean
    RejeterVirement rejeterVirement(DepotVirements depotVirements, AlertesFraude alertes, JournalAudit journalAudit,
            Transaction transaction, Horloge horloge) {
        return new RejetVirementCasUsage(depotVirements, alertes, journalAudit, transaction, horloge);
    }

    @Bean
    RejeterEcheances rejeterEcheances(DepotVirements depotVirements, JournalAudit journalAudit,
            JournalBatch journalBatch, Transaction transaction, Horloge horloge) {
        return new EcheanceRevueCasUsage(depotVirements, journalAudit, journalBatch, transaction, horloge);
    }

    @Bean
    ConsulterRevue consulterRevue(DepotVirements depotVirements) {
        return new ConsulterRevueCasUsage(depotVirements);
    }

    @Bean
    ConsulterSuiviVirements consulterSuiviVirements(DepotVirements depotVirements) {
        return new ConsulterSuiviVirementsCasUsage(depotVirements);
    }

    @Bean
    RecevoirClientModifie recevoirClientModifie(ProjectionClients projection, EvenementsTraites evenementsTraites,
            Transaction transaction, Horloge horloge) {
        return new RecevoirClientModifieCasUsage(projection, evenementsTraites, transaction, horloge);
    }
}
