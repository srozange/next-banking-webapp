package fr.nextbanking.documents.application;

import fr.nextbanking.documents.application.port.in.TelechargerDocument;
import fr.nextbanking.documents.application.port.out.Documents;
import fr.nextbanking.documents.application.port.out.GenerateurPdf;
import fr.nextbanking.documents.application.port.out.Horloge;
import fr.nextbanking.documents.application.port.out.ProjectionClients;
import fr.nextbanking.documents.application.port.out.ProjectionComptes;
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.DonneesDocument;
import fr.nextbanking.documents.domain.TypeDocument;

/**
 * Cas d'usage : télécharger un document PDF. Les refus se décident dans l'ordre : {@code type} absent, {@code type}
 * inconnu, {@code compteId} absent ou non numérique (documents d'un compte), compte absent de la projection, compte
 * d'un autre client (aucun contrôle de rôle : CLIENT, CONSEILLER et ADMIN ne téléchargent que leurs comptes), puis
 * seulement la génération. Le compteur de téléchargements n'est incrémenté qu'après une génération réussie : un refus
 * ou une panne ne compte rien.
 */
public class TelechargerDocumentCasUsage implements TelechargerDocument {

    private final ProjectionComptes comptes;
    private final ProjectionClients clients;
    private final GenerateurPdf generateur;
    private final Documents documents;
    private final Horloge horloge;

    public TelechargerDocumentCasUsage(ProjectionComptes comptes, ProjectionClients clients, GenerateurPdf generateur,
            Documents documents, Horloge horloge) {
        this.comptes = comptes;
        this.clients = clients;
        this.generateur = generateur;
        this.documents = documents;
        this.horloge = horloge;
    }

    @Override
    public DocumentPdf telecharger(Commande commande) {
        if (commande.type() == null) {
            throw DemandeDocumentInvalide.typeManquant();
        }
        TypeDocument type = TypeDocument.depuis(commande.type())
                .orElseThrow(() -> DemandeDocumentInvalide.typeInconnu(commande.type()));
        CompteProjete compte = null;
        if (type.exigeCompte()) {
            long compteId = lireCompteId(commande.compteId());
            compte = comptes.trouver(compteId).orElseThrow(() -> new CompteIntrouvable(compteId));
            if (!compte.appartientA(commande.clientId())) {
                throw new AccesCompteRefuse(compte.compteId());
            }
        }
        String titulaire = clients.nomComplet(commande.clientId())
                .orElseThrow(() -> new ClientIntrouvable(commande.clientId()));
        DocumentPdf document = generateur.generer(type, donnees(commande.clientId(), titulaire, compte));
        documents.compterTelechargement(commande.clientId(), type, parametres(compte), document, horloge.instant());
        return document;
    }

    private DonneesDocument donnees(long clientId, String titulaire, CompteProjete compte) {
        if (compte == null) {
            return new DonneesDocument(clientId, titulaire, null, null, null, null, horloge.aujourdhui());
        }
        return new DonneesDocument(clientId, titulaire, compte.compteId(), compte.numero(), compte.iban(),
                compte.solde(), horloge.aujourdhui());
    }

    private static String parametres(CompteProjete compte) {
        return compte == null ? "" : "compteId=" + compte.compteId();
    }

    private static long lireCompteId(String valeur) {
        if (valeur == null || valeur.isBlank()) {
            throw DemandeDocumentInvalide.compteIdManquant();
        }
        try {
            return Long.parseLong(valeur.trim());
        } catch (NumberFormatException e) {
            throw DemandeDocumentInvalide.compteIdManquant();
        }
    }
}
