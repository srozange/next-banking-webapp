package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Virement immédiat entre deux comptes d'un même client. Les contrôles de saisie s'appliquent dans l'ordre de
 * FR-003-07 (comptes, comptes distincts, montant, motif) avant tout accès aux comptes ; puis compte introuvable (par
 * l'application), propriété des comptes (NFR-SEC-03) et solde.
 */
public record Virement(long id, long clientId, long compteSourceId, long compteDestinataireId, BigDecimal montant,
        String motif, EtatVirement etat, StatutAffichage statut, Instant soumisLe) {

    /** Longueur maximale du motif (FR-003-01). */
    public static final int LONGUEUR_MAX_MOTIF = 100;

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
     * Exécute le virement : propriété des deux comptes, solde suffisant, statut d'affichage selon le seuil de mise en
     * attente, opérations DEBIT et CREDIT avec leur solde après.
     *
     * @throws ErreurVirement un compte n'appartient pas au client, ou solde insuffisant
     */
    public static Execution executer(long id, long clientId, Compte source, Compte destinataire, BigDecimal montant,
            String motif, SeuilsFraude seuils, Instant soumisLe) {
        if (!source.appartientA(clientId) || !destinataire.appartientA(clientId)) {
            throw new ErreurVirement(Cause.COMPTE_ETRANGER);
        }
        if (source.solde().compareTo(montant) < 0) {
            throw new ErreurVirement(Cause.SOLDE_INSUFFISANT);
        }
        String motifRetenu = motif == null || motif.isBlank() ? null : motif;
        StatutAffichage statut = montant.compareTo(seuils.seuilAttente()) >= 0
                ? StatutAffichage.PENDING : StatutAffichage.SUCCESS;
        Virement virement = new Virement(id, clientId, source.id(), destinataire.id(), montant, motifRetenu,
                EtatVirement.EXECUTE, statut, soumisLe);
        Operation debit = new Operation(source.id(), TypeOperation.DEBIT, montant, source.solde().subtract(montant),
                TextesVirement.libelleDebit(destinataire.numero(), motifRetenu, statut), soumisLe, id);
        Operation credit = new Operation(destinataire.id(), TypeOperation.CREDIT, montant,
                destinataire.solde().add(montant),
                TextesVirement.libelleCredit(source.numero(), motifRetenu, statut), soumisLe, id);
        return new Execution(virement, debit, credit);
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
