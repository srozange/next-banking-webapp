package fr.nextbanking.coeurbancaire.domain;

import java.util.Set;

/**
 * Les cinq états d'un virement (AD-12) : EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ et REFUSÉ_ERREUR_CONTRÔLE. La soumission
 * mène à EXÉCUTÉ, SUSPENDU ou REFUSÉ_ERREUR_CONTRÔLE ; SUSPENDU mène à LIBÉRÉ ou REJETÉ ; LIBÉRÉ mène à EXÉCUTÉ ou
 * REJETÉ. Toute autre transition est refusée. EXÉCUTÉ, REJETÉ et REFUSÉ_ERREUR_CONTRÔLE sont terminaux.
 */
public enum EtatVirement {
    EXECUTE,
    SUSPENDU,
    LIBERE,
    REJETE,
    REFUSE_ERREUR_CONTROLE;

    private static final Set<EtatVirement> DE_SOUMISSION = Set.of(EXECUTE, SUSPENDU, REFUSE_ERREUR_CONTROLE);

    /** Vrai si un virement soumis peut naître dans cet état. */
    public boolean estIssueDeSoumission() {
        return DE_SOUMISSION.contains(this);
    }

    /** Vrai si la transition de cet état vers l'état donné est autorisée. */
    public boolean peutTransiterVers(EtatVirement vers) {
        return switch (this) {
            case SUSPENDU -> vers == LIBERE || vers == REJETE;
            case LIBERE -> vers == EXECUTE || vers == REJETE;
            default -> false;
        };
    }
}
