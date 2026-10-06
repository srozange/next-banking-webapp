package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

/**
 * Virement immédiat entre deux comptes d'un même client. Les contrôles de saisie s'appliquent dans l'ordre de
 * FR-003-07 (comptes, comptes distincts, montant, motif) avant tout accès aux comptes ; puis compte introuvable (par
 * l'application), propriété des comptes (NFR-SEC-03) et solde.
 */
public record Virement(long id, long clientId, long compteSourceId, long compteDestinataireId, BigDecimal montant,
        String motif, EtatVirement etat, StatutAffichage statut, Instant soumisLe, Instant heureButoir,
        String motifSuspension) {

    /** Longueur maximale du motif (FR-003-01). */
    public static final int LONGUEUR_MAX_MOTIF = 100;
    /** Délai de revue d'un virement suspendu : 24 heures à compter de la soumission (FR-027-17). */
    public static final Duration DELAI_DE_REVUE = Duration.ofHours(24);

    private static final int DECIMALES = 2;
    private static final int CHIFFRES_ENTIERS_MAX = 17;

    /** Virement exécuté et ses deux opérations au relevé. */
    public record Execution(Virement virement, Operation debit, Operation credit) {
    }

    /**
     * Contrôle la saisie et rend le montant à 2 décimales.
     *
     * @throws ErreurVirement comptes absents ou identiques, montant illisible, nul, négatif ou trop précis, motif trop long
     */
    public static BigDecimal verifierSaisie(Long compteSourceId, Long compteDestinataireId, String montant,
            String motif) {
        if (compteSourceId == null || compteDestinataireId == null) {
            throw new ErreurVirement(Cause.COMPTES_OBLIGATOIRES);
        }
        if (compteSourceId.equals(compteDestinataireId)) {
            throw new ErreurVirement(Cause.COMPTES_IDENTIQUES);
        }
        BigDecimal lu = lireMontant(montant);
        if (motif != null && motif.length() > LONGUEUR_MAX_MOTIF) {
            throw new ErreurVirement(Cause.MOTIF_TROP_LONG);
        }
        return lu;
    }

    /**
     * Contrôle la propriété des deux comptes puis le solde du compte source, avant tout contrôle de fraude.
     *
     * @throws ErreurVirement un compte n'appartient pas au client, ou solde insuffisant
     */
    public static void verifierComptes(long clientId, Compte source, Compte destinataire, BigDecimal montant) {
        if (!source.appartientA(clientId) || !destinataire.appartientA(clientId)) {
            throw new ErreurVirement(Cause.COMPTE_ETRANGER);
        }
        if (source.solde().compareTo(montant) < 0) {
            throw new ErreurVirement(Cause.SOLDE_INSUFFISANT);
        }
    }

    /**
     * Exécute le virement : propriété des deux comptes, solde suffisant, statut d'affichage selon le seuil de mise en
     * attente, opérations DEBIT et CREDIT avec leur solde après.
     *
     * @throws ErreurVirement un compte n'appartient pas au client, ou solde insuffisant
     */
    public static Execution executer(long id, long clientId, Compte source, Compte destinataire, BigDecimal montant,
            String motif, SeuilsFraude seuils, Instant soumisLe) {
        verifierComptes(clientId, source, destinataire, montant);
        Virement virement = naitre(id, clientId, source.id(), destinataire.id(), montant, motif, seuils,
                EtatVirement.EXECUTE, soumisLe, null, null);
        return virement.operations(source, destinataire, soumisLe);
    }

    /**
     * Virement suspendu avant tout débit : l'heure butoir est la soumission plus 24 heures, et le motif de suspension
     * est celui de la décision du contrôle.
     */
    public static Virement suspendre(long id, long clientId, Compte source, Compte destinataire, BigDecimal montant,
            String motif, SeuilsFraude seuils, Instant soumisLe, String motifSuspension) {
        return naitre(id, clientId, source.id(), destinataire.id(), montant, motif, seuils, EtatVirement.SUSPENDU,
                soumisLe, soumisLe.plus(DELAI_DE_REVUE), motifSuspension);
    }

    /** Virement refusé pour erreur de contrôle : persisté, terminal, sans aucun effet sur les soldes. */
    public static Virement refuser(long id, long clientId, long compteSourceId, long compteDestinataireId,
            BigDecimal montant, String motif, SeuilsFraude seuils, Instant soumisLe) {
        return naitre(id, clientId, compteSourceId, compteDestinataireId, montant, motif, seuils,
                EtatVirement.REFUSE_ERREUR_CONTROLE, soumisLe, null, null);
    }

    /**
     * Débit d'un virement libéré : il passe à EXÉCUTÉ avec ses deux opérations, au statut d'affichage de la soumission,
     * datées de l'instant du débit.
     */
    public Execution executerApresLiberation(Compte source, Compte destinataire, Instant instant) {
        Virement execute = new Virement(id, clientId, compteSourceId, compteDestinataireId, montant, motif,
                EtatVirement.EXECUTE, statut, soumisLe, heureButoir, motifSuspension);
        return execute.operations(source, destinataire, instant);
    }

    /** Vrai à l'heure butoir et après : l'heure butoir fait foi (décision B5). */
    public boolean heureButoirAtteinte(Instant maintenant) {
        return heureButoir != null && !maintenant.isBefore(heureButoir);
    }

    private Execution operations(Compte source, Compte destinataire, Instant date) {
        Operation debit = new Operation(source.id(), TypeOperation.DEBIT, montant, source.solde().subtract(montant),
                TextesVirement.libelleDebit(destinataire.numero(), motif, statut), date, id);
        Operation credit = new Operation(destinataire.id(), TypeOperation.CREDIT, montant,
                destinataire.solde().add(montant),
                TextesVirement.libelleCredit(source.numero(), motif, statut), date, id);
        return new Execution(this, debit, credit);
    }

    private static Virement naitre(long id, long clientId, long compteSourceId, long compteDestinataireId,
            BigDecimal montant, String motif, SeuilsFraude seuils, EtatVirement etat, Instant soumisLe,
            Instant heureButoir, String motifSuspension) {
        String motifRetenu = motif == null || motif.isBlank() ? null : motif;
        StatutAffichage statut = montant.compareTo(seuils.seuilAttente()) >= 0
                ? StatutAffichage.PENDING : StatutAffichage.SUCCESS;
        return new Virement(id, clientId, compteSourceId, compteDestinataireId, montant, motifRetenu, etat, statut,
                soumisLe, heureButoir, motifSuspension);
    }

    private static BigDecimal lireMontant(String texte) {
        if (texte == null) {
            throw new ErreurVirement(Cause.MONTANT_INVALIDE);
        }
        BigDecimal lu;
        try {
            lu = new BigDecimal(texte.strip());
        } catch (NumberFormatException e) {
            throw new ErreurVirement(Cause.MONTANT_INVALIDE);
        }
        if (lu.signum() <= 0 || lu.scale() > DECIMALES || lu.precision() - lu.scale() > CHIFFRES_ENTIERS_MAX) {
            throw new ErreurVirement(Cause.MONTANT_INVALIDE);
        }
        return lu.setScale(DECIMALES);
    }
}
