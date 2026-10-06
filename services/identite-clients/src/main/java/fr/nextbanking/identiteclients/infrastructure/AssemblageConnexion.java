package fr.nextbanking.identiteclients.infrastructure;

import fr.nextbanking.identiteclients.application.ConsulterProfilCasUsage;
import fr.nextbanking.identiteclients.application.SeConnecterCasUsage;
import fr.nextbanking.identiteclients.application.port.in.ConsulterProfil;
import fr.nextbanking.identiteclients.application.port.in.SeConnecter;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.application.port.out.MotsDePasse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage de connexion et de profil, écrits en Java pur.
 */
@Configuration
public class AssemblageConnexion {

    @Bean
    SeConnecter seConnecter(DepotClients depotClients, MotsDePasse motsDePasse, EmetteurJeton emetteurJeton) {
        return new SeConnecterCasUsage(depotClients, motsDePasse, emetteurJeton);
    }

    @Bean
    ConsulterProfil consulterProfil(DepotClients depotClients) {
        return new ConsulterProfilCasUsage(depotClients);
    }
}
