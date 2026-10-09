package fr.nextbanking.documents.application;

import fr.nextbanking.documents.application.port.in.TelechargerDocument;
import fr.nextbanking.documents.application.port.out.Documents;
import fr.nextbanking.documents.application.port.out.GenerateurPdf;
import fr.nextbanking.documents.application.port.out.Horloge;
import fr.nextbanking.documents.application.port.out.ProjectionClients;
import fr.nextbanking.documents.application.port.out.ProjectionComptes;
import fr.nextbanking.documents.application.port.out.ProjectionOperations;
import fr.nextbanking.documents.domain.CompteProjete;
import fr.nextbanking.documents.domain.DocumentPdf;
import fr.nextbanking.documents.domain.DonneesDocument;
import fr.nextbanking.documents.domain.Ifu;
import fr.nextbanking.documents.domain.LigneReleve;
import fr.nextbanking.documents.domain.Releve;
import fr.nextbanking.documents.domain.TypeDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/**
 * Cas d'usage : télécharger un document PDF. Les refus se décident dans l'ordre : {@code type} absent, {@code type}
 * inconnu, {@code compteId} absent ou non numérique (documents d'un compte), {@code annee} (relevé et IFU) puis
 * {@code mois} (relevé) invalides, compte absent de la projection, compte d'un autre client (aucun contrôle de rôle :
 * CLIENT, CONSEILLER et ADMIN ne téléchargent que leurs comptes), client absent de la projection, puis seulement la
 * génération. Le client d'un IFU est toujours celui du jeton. Les bornes du mois ou de l'année se calculent dans le
 * fuseau de l'{@link Horloge}. Le compteur de téléchargements n'est incrémenté qu'après une génération réussie : un
 * refus ou une panne ne compte rien.
 */
public class TelechargerDocumentCasUsage implements TelechargerDocument {

    private static final int ANNEE_MIN = 2000;
    private static final int ANNEE_MAX = 2100;

    private final ProjectionComptes comptes;
    private final ProjectionClients clients;
    private final ProjectionOperations operations;
    private final GenerateurPdf generateur;
    private final Documents documents;
    private final Horloge horloge;

    public TelechargerDocumentCasUsage(ProjectionComptes comptes, ProjectionClients clients,
            ProjectionOperations operations, GenerateurPdf generateur, Documents documents, Horloge horloge) {
        this.comptes = comptes;
        this.clients = clients;
        this.operations = operations;
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
        Long compteId = type.exigeCompte() ? lireCompteId(commande.compteId()) : null;
        int annee = type.exigeAnnee() ? lireEntier(commande.annee(), ANNEE_MIN, ANNEE_MAX) : 0;
        int mois = type.exigeMois() ? lireEntier(commande.mois(), 1, 12) : 0;
        CompteProjete compte = compteId == null ? null : controlerCompte(compteId, commande.clientId());
        String titulaire = clients.nomComplet(commande.clientId())
                .orElseThrow(() -> new ClientIntrouvable(commande.clientId()));
        DonneesDocument donnees = switch (type) {
            case RELEVE -> releve(commande.clientId(), titulaire, compte, annee, mois);
            case IFU -> ifu(commande.clientId(), titulaire, annee);
            case RIB, ATTESTATION -> pourUnCompte(commande.clientId(), titulaire, compte);
        };
        DocumentPdf document = generateur.generer(type, donnees);
        documents.compterTelechargement(commande.clientId(), type, parametres(type, compte, annee, mois), document,
                horloge.instant());
        return document;
    }

    private CompteProjete controlerCompte(long compteId, long clientId) {
        CompteProjete compte = comptes.trouver(compteId).orElseThrow(() -> new CompteIntrouvable(compteId));
        if (!compte.appartientA(clientId)) {
            throw new AccesCompteRefuse(compte.compteId());
        }
        return compte;
    }

    private DonneesDocument pourUnCompte(long clientId, String titulaire, CompteProjete compte) {
        return new DonneesDocument(clientId, titulaire, compte.compteId(), compte.numero(), compte.iban(),
                compte.solde(), horloge.aujourdhui());
    }

    private DonneesDocument releve(long clientId, String titulaire, CompteProjete compte, int annee, int mois) {
        ZoneId fuseau = horloge.fuseau();
        YearMonth periode = YearMonth.of(annee, mois);
        List<LigneReleve> lignes = operations.duCompte(compte.compteId(), debut(periode.atDay(1), fuseau),
                debut(periode.plusMonths(1).atDay(1), fuseau)).stream()
                .map(o -> new LigneReleve(o.instant().atZone(fuseau).toLocalDate(), o.libelle(), o.type(),
                        o.montant(), o.soldeApres()))
                .toList();
        return new DonneesDocument(clientId, titulaire, compte.compteId(), compte.numero(), compte.iban(),
                compte.solde(), horloge.aujourdhui(), new Releve(compte.type(), annee, mois, lignes), null);
    }

    private DonneesDocument ifu(long clientId, String titulaire, int annee) {
        ZoneId fuseau = horloge.fuseau();
        BigDecimal total = operations.totalInteretsCredites(clientId, debut(LocalDate.of(annee, 1, 1), fuseau),
                debut(LocalDate.of(annee + 1, 1, 1), fuseau));
        return new DonneesDocument(clientId, titulaire, null, null, null, null, horloge.aujourdhui(), null,
                Ifu.de(annee, total));
    }

    private static Instant debut(LocalDate jour, ZoneId fuseau) {
        return jour.atStartOfDay(fuseau).toInstant();
    }

    private static String parametres(TypeDocument type, CompteProjete compte, int annee, int mois) {
        return switch (type) {
            case RELEVE -> "compteId=" + compte.compteId() + ";annee=" + annee + ";mois=" + mois;
            case IFU -> "annee=" + annee;
            case RIB, ATTESTATION -> "compteId=" + compte.compteId();
        };
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

    /** Entier de la plage donnée ; absent, vide, non numérique ou hors plage, la période est invalide. */
    private static int lireEntier(String valeur, int min, int max) {
        if (valeur == null || valeur.isBlank()) {
            throw DemandeDocumentInvalide.periodeInvalide();
        }
        int entier;
        try {
            entier = Integer.parseInt(valeur.trim());
        } catch (NumberFormatException e) {
            throw DemandeDocumentInvalide.periodeInvalide();
        }
        if (entier < min || entier > max) {
            throw DemandeDocumentInvalide.periodeInvalide();
        }
        return entier;
    }
}
