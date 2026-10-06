package fr.nextbanking.coeurbancaire.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Seul fichier de textes du virement dans le cœur bancaire : textes du legacy ({@code MESSAGES.md}, FEAT-003) et
 * textes nouveaux ({@code textes-nouveaux.md}, clé entre parenthèses). Aucun texte n'est inventé.
 */
public final class TextesVirement {

    /** Message serveur du legacy (RG-003-003, RG-003-004). */
    public static final String COMPTES_OBLIGATOIRES = "Veuillez sélectionner les deux comptes";
    /** Message du legacy (RG-003-005). */
    public static final String COMPTES_IDENTIQUES = "Le compte source et destination doivent être différents";
    /** Message du legacy débarrassé du nom d'exception (A-003-1, BUG-008, BUG-011). */
    public static final String MONTANT_INVALIDE = "Erreur lors du virement : Le montant doit etre positif";
    /** virement.validation.motif.longueur */
    public static final String MOTIF_TROP_LONG = "Le motif ne doit pas dépasser 100 caractères";
    /** Message du legacy débarrassé du nom d'exception (A-003-1). */
    public static final String COMPTE_INTROUVABLE = "Erreur lors du virement : Compte introuvable";
    /** Message du legacy resté mort dans le legacy, désormais affiché (A-003-2, BUG-008, BUG-011). */
    public static final String SOLDE_INSUFFISANT = "Solde insuffisant sur le compte source";
    /** Texte d'échec de remise (A-023-1, BUG-073). */
    public static final String REMISE_IMPOSSIBLE =
            "Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard";
    /** Texte générique d'accès refusé (compte d'un autre client). */
    public static final String ACCES_REFUSE = "Accès refusé";
    /** Texte générique d'une ressource inexistante (relevé d'un compte inexistant). */
    public static final String RESSOURCE_INTROUVABLE = "Ressource introuvable";

    /** Mention du legacy ajoutée aux opérations d'un virement PENDING (BUG-009 conservé provisoirement). */
    public static final String EN_ATTENTE = "[EN ATTENTE]";
    /** Texte de la notification ALERTE du legacy, rendu par la story 3.3 ({@code {montant}} = montant). */
    public static final String TRANSACTION_IMPORTANTE =
            "Virement de %s EUR en attente de validation - transaction importante, verification en cours.";

    /** Texte de la notification CONFIRMATION du legacy ({@code {montant}}, {@code {numCompte}}). */
    private static final String CONFIRMATION = "Virement de %s EUR vers le compte %s confirme";
    /** Suffixe du message de confirmation quand le virement a un motif. */
    private static final String CONFIRMATION_MOTIF = " (motif : %s)";

    /** virement.refus.erreur.controle ({@code {référence}} = identifiant de trace). */
    private static final String REFUS_CONTROLE =
            "Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. "
                    + "Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous "
                    + "avec la référence %s.";
    /** revue.erreur.motif.manquant (MSG-007-127). */
    public static final String MOTIF_DECISION_OBLIGATOIRE = "Veuillez indiquer un motif";
    /** revue.erreur.solde.insuffisant */
    public static final String LIBERATION_REFUSEE_SOLDE =
            "Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit.";
    /** revue.erreur.heure.butoir */
    public static final String HEURE_BUTOIR_DEPASSEE = "Heure butoir dépassée : le virement est rejeté.";
    /** Virement absent ou qui n'est plus suspendu : texte générique du statut 404 (api.erreur.404). */
    public static final String VIREMENT_NON_SUSPENDU = "Ressource introuvable";

    private static final String AVIS_SUSPENSION =
            "Votre virement de %s EUR vers le compte %s est en cours de vérification. Rien n'a été débité. "
                    + "Vous aurez notre réponse avant le %s à %s. "
                    + "Sans décision à cette heure, il sera rejeté sans aucun débit.";
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    private static final String VERS = "Virement vers ";
    private static final String DEPUIS = "Virement depuis ";
    private static final String SEPARATEUR_MOTIF = " - ";

    private TextesVirement() {
    }

    /** Message de la notification ALERTE. */
    public static String transactionImportante(String montant) {
        return String.format(TRANSACTION_IMPORTANTE, montant);
    }

    /** Message de la notification CONFIRMATION ; un motif absent ou blanc n'est pas repris. */
    public static String confirmation(String montant, String numeroDestinataire, String motif) {
        String message = String.format(CONFIRMATION, montant, numeroDestinataire);
        return motif == null || motif.isBlank() ? message : message + String.format(CONFIRMATION_MOTIF, motif);
    }

    /** Libellé de l'opération DEBIT du compte source. */
    public static String libelleDebit(String numeroDestinataire, String motif, StatutAffichage statut) {
        return libelle(VERS + numeroDestinataire, motif, statut);
    }

    /** Libellé de l'opération CREDIT du compte destinataire. */
    public static String libelleCredit(String numeroSource, String motif, StatutAffichage statut) {
        return libelle(DEPUIS + numeroSource, motif, statut);
    }

    /** Détail de l'entrée d'audit VIREMENT. */
    public static String detailVirement(String montant, String motif, String issue) {
        return "Montant: " + montant + " - Motif: " + motifOuVide(motif) + " - Issue: " + issue;
    }

    /** Détail de l'entrée d'audit TRANSFER_EXECUTED. */
    public static String detailExecution(String montant, String motif) {
        return "Montant: " + montant + " - Motif: " + motifOuVide(motif);
    }

    /** Message du refus pour erreur de contrôle, avec la référence de trace. */
    public static String refusControle(String reference) {
        return String.format(REFUS_CONTROLE, reference);
    }

    /** Texte de la notification FRAUDE et de l'avis de suspension (virement.avis.suspension). */
    public static String avisSuspension(String montant, String numeroDestinataire, Instant heureButoir,
            ZoneId fuseau) {
        return String.format(AVIS_SUSPENSION, montant, numeroDestinataire, formaterDate(heureButoir, fuseau),
                formaterHeure(heureButoir, fuseau));
    }

    /** Date de l'heure butoir, {@code dd/MM/yyyy}. */
    public static String formaterDate(Instant instant, ZoneId fuseau) {
        return FORMAT_DATE.format(instant.atZone(fuseau));
    }

    /** Heure de l'heure butoir, {@code HH:mm}. */
    public static String formaterHeure(Instant instant, ZoneId fuseau) {
        return FORMAT_HEURE.format(instant.atZone(fuseau));
    }

    /** Motif de suspension pour un score trop élevé (revue.motif.score). */
    public static String motifSuspensionScore(int score, List<String> signaux) {
        return "Score cumulé " + score + " - signaux : " + String.join(", ", signaux);
    }

    /** Motif de suspension pour un montant au moins égal au seuil de fraude (revue.motif.seuil). */
    public static String motifSuspensionSeuil(String seuil) {
        return "Montant supérieur ou égal au seuil anti-fraude (" + seuil + " EUR)";
    }

    /** Détails de la ligne SUSPENDED_TRANSFERS_EXPIRY du journal d'exécution : virements rejetés. */
    public static String detailsEcheance(int rejetes) {
        return rejetes + " virement(s) suspendu(s) rejeté(s) à l'heure butoir";
    }

    /** Détails de la ligne SUSPENDED_TRANSFERS_EXPIRY en échec : virements rejetés et en erreur. */
    public static String detailsEcheanceEchec(int rejetes, int echecs) {
        return rejetes + " virement(s) rejeté(s), " + echecs + " en erreur";
    }

    /** Détail de l'entrée d'audit ADMIN_TRANSFER_RELEASE. */
    public static String detailLiberation(long virementId, String motif, String issue) {
        return "Liberation virement id=" + virementId + " - Motif: " + motif + " - Issue: " + issue;
    }

    /** Détail de l'entrée d'audit ADMIN_TRANSFER_REJECT. */
    public static String detailRejet(long virementId, String motif) {
        return "Rejet virement id=" + virementId + " - Motif: " + motif;
    }

    /** Détail de l'entrée d'audit TRANSFER_AUTO_REJECT. */
    public static String detailRejetAutomatique(long virementId) {
        return "Rejet automatique virement id=" + virementId;
    }

    /** Détail de l'entrée d'audit TRANSFER_REJECT_INSUFFICIENT_FUNDS. */
    public static String detailRejetSoldeAuDebit(long virementId) {
        return "Rejet virement id=" + virementId + " - Motif: solde insuffisant au debit";
    }

    /** Description d'alerte LARGE_AMOUNT à 40 (FEAT-027 MESSAGES.md §3). */
    public static String descriptionMontantEleve(String montant, String seuil) {
        return "Transaction amount " + montant + " exceeds the large threshold of " + seuil;
    }

    /** Description d'alerte LARGE_AMOUNT à 80. */
    public static String descriptionMontantTresEleve(String montant, String seuil) {
        return "Transaction amount " + montant + " exceeds the very-large threshold of " + seuil;
    }

    /** Description d'alerte HIGH_FREQUENCY. */
    public static String descriptionFrequence(long nombre, int plafond) {
        return "Customer has " + nombre + " transactions today, exceeding the daily limit of " + plafond;
    }

    /** Description d'alerte NEW_BENEFICIARY (le numéro est celui du legacy ; seul le journal le masque). */
    public static String descriptionNouveauBeneficiaire(String iban) {
        return "Target IBAN " + iban + " is not registered as a trusted beneficiary";
    }

    /** Description d'alerte UNUSUAL_HOURS. */
    public static String descriptionHeureInhabituelle(int heure) {
        return "Transaction initiated at hour " + heure + " which is outside normal operating hours (06:00-23:00)";
    }

    /** Description d'alerte ROUND_AMOUNT. */
    public static String descriptionMontantRond(String montant) {
        return "Transaction amount " + montant + " is a suspicious round figure (multiple of 1000)";
    }

    /** Description d'alerte FOREIGN_IBAN. */
    public static String descriptionIbanEtranger(String iban) {
        return "Target IBAN " + iban + " does not start with FR (possible foreign transfer)";
    }

    /** Description d'alerte RAPID_SUCCESSION. */
    public static String descriptionSuccessionRapide(long secondes, int seuilSecondes) {
        return "Previous transaction was only " + secondes + " seconds ago (threshold: " + seuilSecondes + "s)";
    }

    /** Description d'alerte VELOCITY_CHECK. */
    public static String descriptionVelocite(long nombre, int fenetreMinutes, int nombreMax) {
        return "Customer executed " + nombre + " transactions above 500 EUR in the last " + fenetreMinutes
                + " minutes (limit: " + nombreMax + ")";
    }

    private static String libelle(String base, String motif, StatutAffichage statut) {
        String avecMotif = motif == null || motif.isBlank() ? base : base + SEPARATEUR_MOTIF + motif;
        return statut == StatutAffichage.PENDING ? EN_ATTENTE + " " + avecMotif : avecMotif;
    }

    private static String motifOuVide(String motif) {
        return motif == null ? "" : motif;
    }
}
