package fr.nextbanking.coeurbancaire.domain;

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

    /** Mention du legacy ajoutée aux opérations d'un virement PENDING (BUG-009 conservé provisoirement). */
    public static final String EN_ATTENTE = "[EN ATTENTE]";
    /** Texte de la notification ALERTE du legacy, rendu par la story 3.3 ({@code {montant}} = montant). */
    public static final String TRANSACTION_IMPORTANTE =
            "Virement de %s EUR en attente de validation - transaction importante, verification en cours.";

    /** Texte de la notification CONFIRMATION du legacy ({@code {montant}}, {@code {numCompte}}). */
    private static final String CONFIRMATION = "Virement de %s EUR vers le compte %s confirme";
    /** Suffixe du message de confirmation quand le virement a un motif. */
    private static final String CONFIRMATION_MOTIF = " (motif : %s)";

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
