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

    private static final String VERS = "Virement vers ";
    private static final String DEPUIS = "Virement depuis ";
    private static final String SEPARATEUR_MOTIF = " - ";

    private TextesVirement() {
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

    private static String libelle(String base, String motif, StatutAffichage statut) {
        String avecMotif = motif == null || motif.isBlank() ? base : base + SEPARATEUR_MOTIF + motif;
        return statut == StatutAffichage.PENDING ? EN_ATTENTE + " " + avecMotif : avecMotif;
    }

    private static String motifOuVide(String motif) {
        return motif == null ? "" : motif;
    }
}
