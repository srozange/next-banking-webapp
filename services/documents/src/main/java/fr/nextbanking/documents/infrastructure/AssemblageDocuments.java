package fr.nextbanking.documents.infrastructure;

import fr.nextbanking.documents.application.RecevoirClientModifieCasUsage;
import fr.nextbanking.documents.application.RecevoirCompteModifieCasUsage;
import fr.nextbanking.documents.application.TelechargerDocumentCasUsage;
import fr.nextbanking.documents.application.port.in.RecevoirClientModifie;
import fr.nextbanking.documents.application.port.in.RecevoirCompteModifie;
import fr.nextbanking.documents.application.port.in.TelechargerDocument;
import fr.nextbanking.documents.application.port.out.Documents;
import fr.nextbanking.documents.application.port.out.EvenementsTraites;
import fr.nextbanking.documents.application.port.out.GenerateurPdf;
import fr.nextbanking.documents.application.port.out.Horloge;
import fr.nextbanking.documents.application.port.out.ProjectionClients;
import fr.nextbanking.documents.application.port.out.ProjectionComptes;
import fr.nextbanking.documents.application.port.out.Transaction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assemble les cas d'usage de téléchargement des documents et de projection des comptes et des clients, écrits en Java
 * pur.
 */
@Configuration
public class AssemblageDocuments {

    @Bean
    TelechargerDocument telechargerDocument(ProjectionComptes comptes, ProjectionClients clients,
            GenerateurPdf generateur, Documents documents, Horloge horloge) {
        return new TelechargerDocumentCasUsage(comptes, clients, generateur, documents, horloge);
    }

    @Bean
    RecevoirClientModifie recevoirClientModifie(ProjectionClients projection, EvenementsTraites traites,
            Transaction transaction) {
        return new RecevoirClientModifieCasUsage(projection, traites, transaction);
    }

    @Bean
    RecevoirCompteModifie recevoirCompteModifie(ProjectionComptes projection, EvenementsTraites traites,
            Transaction transaction) {
        return new RecevoirCompteModifieCasUsage(projection, traites, transaction);
    }
}
