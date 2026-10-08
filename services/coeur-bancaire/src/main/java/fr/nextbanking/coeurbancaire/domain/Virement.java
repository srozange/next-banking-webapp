package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;
import java.time.Duration;
import java.time.Instant;

/**
 * Virement immédiat entre deux comptes d'un même client. Les contrôles de saisie s'appliquent dans l'ordre de
 * FR-003-07 (comptes, comptes distincts, montant, motif) avant tout accès aux comptes ; puis compte introuvable (par
 * l'application), propriété des comptes (NFR-SEC-03) et solde.
 */
public record Virement(long id, long clientId, CompteId compteSourceId, CompteId compteDestinataireId,
        Montant montant, Motif motif, EtatVirement etat, StatutAffichage statut, Instant soumisLe, Instant heureButoir,
        String motifSuspension) {

    /** Délai de revue d'un virement suspendu : 24 heures à compter de la soumission (FR-027-17). */
    public static final Duration DELAI_DE_REVUE = Duration.ofHours(24);

    /** Virement exécuté et ses deux opérations au relevé. */
    public record Execution(Virement virement, Operation debit, Operation credit) {
    }

    /**
     * Contrôle la propriété des deux comptes puis le solde du compte source, avant tout contrôle de fraude.
     *
     * @throws ErreurVirement un compte n'appartient pas au client, ou solde insuffisant
     */
    public static void verifierComptes(long clientId, Compte source, Compte destinataire, Montant montant) {
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
    public static Execution executer(Soumission soumission, Compte source, Compte destinataire,
            SeuilsFraude seuils) {
        verifierComptes(soumission.clientId(), source, destinataire, soumission.demande().montant());
        Virement virement = naitre(soumission, seuils, EtatVirement.EXECUTE, null, null);
        return virement.operations(source, destinataire, soumission.soumisLe());
    }

    /**
     * Virement suspendu avant tout débit : l'heure butoir est la soumission plus 24 heures, et le motif de suspension
     * est celui de la décision du contrôle.
     */
    public static Virement suspendre(Soumission soumission, SeuilsFraude seuils, String motifSuspension) {
        return naitre(soumission, seuils, EtatVirement.SUSPENDU, soumission.soumisLe().plus(DELAI_DE_REVUE),
                motifSuspension);
    }

    /** Virement refusé pour erreur de contrôle : persisté, terminal, sans aucun effet sur les soldes. */
    public static Virement refuser(Soumission soumission, SeuilsFraude seuils) {
        return naitre(soumission, seuils, EtatVirement.REFUSE_ERREUR_CONTROLE, null, null);
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
        Operation debit = new Operation(source.id(), TypeOperation.DEBIT, montant, source.solde().soustraire(montant),
                TextesVirement.libelleDebit(destinataire.numero(), motif.texte(), statut), date, id);
        Operation credit = new Operation(destinataire.id(), TypeOperation.CREDIT, montant,
                destinataire.solde().ajouter(montant),
                TextesVirement.libelleCredit(source.numero(), motif.texte(), statut), date, id);
        return new Execution(this, debit, credit);
    }

    private static Virement naitre(Soumission soumission, SeuilsFraude seuils, EtatVirement etat,
            Instant heureButoir, String motifSuspension) {
        DemandeVirement demande = soumission.demande();
        StatutAffichage statut = demande.montant().atteint(seuils.seuilAttente())
                ? StatutAffichage.PENDING : StatutAffichage.SUCCESS;
        return new Virement(soumission.id(), soumission.clientId(), demande.source(), demande.destinataire(),
                demande.montant(), demande.motif(), etat, statut, soumission.soumisLe(), heureButoir,
                motifSuspension);
    }
}
