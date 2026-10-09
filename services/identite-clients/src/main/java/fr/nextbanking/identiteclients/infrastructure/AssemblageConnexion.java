package fr.nextbanking.identiteclients.infrastructure;

import fr.nextbanking.identiteclients.application.ChangerEtatClientCasUsage;
import fr.nextbanking.identiteclients.application.ConsulterPortefeuilleCasUsage;
import fr.nextbanking.identiteclients.application.ConsulterProfilCasUsage;
import fr.nextbanking.identiteclients.application.RenouvelerSessionCasUsage;
import fr.nextbanking.identiteclients.application.SeConnecterCasUsage;
import fr.nextbanking.identiteclients.application.SeDeconnecterCasUsage;
import fr.nextbanking.identiteclients.application.port.in.ChangerEtatClient;
import fr.nextbanking.identiteclients.application.port.in.ConsulterPortefeuille;
import fr.nextbanking.identiteclients.application.port.in.ConsulterProfil;
import fr.nextbanking.identiteclients.application.port.in.RenouvelerSession;
import fr.nextbanking.identiteclients.application.port.in.SeConnecter;
import fr.nextbanking.identiteclients.application.port.in.SeDeconnecter;
import fr.nextbanking.identiteclients.application.port.out.DepotClients;
import fr.nextbanking.identiteclients.application.port.out.EmetteurJeton;
import fr.nextbanking.identiteclients.application.port.out.Horloge;
import fr.nextbanking.identiteclients.application.port.out.JournalAudit;
import fr.nextbanking.identiteclients.application.port.out.MotsDePasse;
import fr.nextbanking.identiteclients.application.port.out.PortefeuilleClients;
import fr.nextbanking.identiteclients.application.port.out.PublicationEtatClient;
import fr.nextbanking.identiteclients.application.port.out.Transaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage de connexion, de profil, de déconnexion, de renouvellement, de changement d'état d'un client et du portefeuille d'un conseiller, écrits en Java pur.
 */
@Configuration
public class AssemblageConnexion {

    @Bean
    SeConnecter seConnecter(DepotClients depotClients, MotsDePasse motsDePasse, EmetteurJeton emetteurJeton,
            JournalAudit journalAudit, Transaction transaction, Horloge horloge) {
        return new SeConnecterCasUsage(depotClients, motsDePasse, emetteurJeton, journalAudit, transaction, horloge);
    }

    @Bean
    ConsulterProfil consulterProfil(DepotClients depotClients) {
        return new ConsulterProfilCasUsage(depotClients);
    }

    @Bean
    SeDeconnecter seDeconnecter(JournalAudit journalAudit, Transaction transaction, Horloge horloge) {
        return new SeDeconnecterCasUsage(journalAudit, transaction, horloge);
    }

    @Bean
    ChangerEtatClient changerEtatClient(DepotClients depotClients, JournalAudit journalAudit,
            PublicationEtatClient publicationEtatClient, Transaction transaction, Horloge horloge) {
        return new ChangerEtatClientCasUsage(depotClients, journalAudit, publicationEtatClient, transaction,
                horloge);
    }

    @Bean
    ConsulterPortefeuille consulterPortefeuille(PortefeuilleClients portefeuilleClients) {
        return new ConsulterPortefeuilleCasUsage(portefeuilleClients);
    }

    @Bean
    RenouvelerSession renouvelerSession(DepotClients depotClients, EmetteurJeton emetteurJeton) {
        return new RenouvelerSessionCasUsage(depotClients, emetteurJeton);
    }
}
