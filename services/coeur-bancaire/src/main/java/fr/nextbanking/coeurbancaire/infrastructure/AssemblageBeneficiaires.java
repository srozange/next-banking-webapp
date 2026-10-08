package fr.nextbanking.coeurbancaire.infrastructure;

import fr.nextbanking.coeurbancaire.application.ConsulterBeneficiairesCasUsage;
import fr.nextbanking.coeurbancaire.application.SupprimerBeneficiaireCasUsage;
import fr.nextbanking.coeurbancaire.application.port.in.ConsulterBeneficiaires;
import fr.nextbanking.coeurbancaire.application.port.in.SupprimerBeneficiaire;
import fr.nextbanking.coeurbancaire.application.port.out.Beneficiaires;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage des bénéficiaires externes, écrits en Java pur.
 */
@Configuration
public class AssemblageBeneficiaires {

    @Bean
    ConsulterBeneficiaires consulterBeneficiaires(Beneficiaires beneficiaires) {
        return new ConsulterBeneficiairesCasUsage(beneficiaires);
    }

    @Bean
    SupprimerBeneficiaire supprimerBeneficiaire(Beneficiaires beneficiaires) {
        return new SupprimerBeneficiaireCasUsage(beneficiaires);
    }
}
