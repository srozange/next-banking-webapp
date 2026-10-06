package fr.nextbanking.openbankingconseiller.infrastructure;

import fr.nextbanking.openbankingconseiller.application.ConnecterPowensCasUsage;
import fr.nextbanking.openbankingconseiller.application.ConsulterEtatPowensCasUsage;
import fr.nextbanking.openbankingconseiller.application.DeconnecterPowensCasUsage;
import fr.nextbanking.openbankingconseiller.application.RafraichirComptesExternesCasUsage;
import fr.nextbanking.openbankingconseiller.application.SelectionnerCompteExterneCasUsage;
import fr.nextbanking.openbankingconseiller.application.port.in.DeconnecterPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.RafraichirComptesExternes;
import fr.nextbanking.openbankingconseiller.application.port.in.SelectionnerCompteExterne;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotComptesExternes;
import fr.nextbanking.openbankingconseiller.application.TraiterRetourPowensCasUsage;
import fr.nextbanking.openbankingconseiller.application.port.in.ConnecterPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.ConsulterEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.in.TraiterRetourPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.DepotEtatPowens;
import fr.nextbanking.openbankingconseiller.application.port.out.EmetteurState;
import fr.nextbanking.openbankingconseiller.application.port.out.Horloge;
import fr.nextbanking.openbankingconseiller.application.port.out.Powens;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage Powens, écrits en Java pur.
 */
@Configuration
public class AssemblagePowens {

    @Bean
    ConnecterPowens connecterPowens(Powens powens, DepotEtatPowens depot, EmetteurState emetteur, Horloge horloge) {
        return new ConnecterPowensCasUsage(powens, depot, emetteur, horloge);
    }

    @Bean
    ConsulterEtatPowens consulterEtatPowens(DepotEtatPowens depot, DepotComptesExternes comptes) {
        return new ConsulterEtatPowensCasUsage(depot, comptes);
    }

    @Bean
    SelectionnerCompteExterne selectionnerCompteExterne(Powens powens, DepotEtatPowens depot,
            DepotComptesExternes comptes, ConsulterEtatPowens consulter) {
        return new SelectionnerCompteExterneCasUsage(powens, depot, comptes, consulter);
    }

    @Bean
    RafraichirComptesExternes rafraichirComptesExternes(Powens powens, DepotEtatPowens depot,
            DepotComptesExternes comptes, ConsulterEtatPowens consulter) {
        return new RafraichirComptesExternesCasUsage(powens, depot, comptes, consulter);
    }

    @Bean
    DeconnecterPowens deconnecterPowens(DepotEtatPowens depot) {
        return new DeconnecterPowensCasUsage(depot);
    }

    @Bean
    TraiterRetourPowens traiterRetourPowens(Powens powens, DepotEtatPowens depot, EmetteurState emetteur,
            Horloge horloge) {
        return new TraiterRetourPowensCasUsage(powens, depot, emetteur, horloge);
    }
}
