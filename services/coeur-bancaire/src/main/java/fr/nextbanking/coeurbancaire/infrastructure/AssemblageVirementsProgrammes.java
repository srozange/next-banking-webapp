package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.ConsulterVirementsProgrammesCasUsage;
import fr.nextbanking.coeurbancaire.application.CreerVirementProgrammeCasUsage;
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
    CreerVirementProgramme creerVirementProgramme(VirementsProgrammes virements, Comptes comptes, Horloge horloge) {
        return new CreerVirementProgrammeCasUsage(virements, comptes, horloge);
    }
}
