package fr.nextbanking.coeurbancaire.domain;

import fr.nextbanking.coeurbancaire.domain.ErreurVirement.Cause;

/**
 * Libellé libre d'un virement (FR-003-01) : absent ({@link #AUCUN}), ou non blanc de 100 caractères au plus.
 */
public record Motif(String texte) {

    /** Longueur maximale du motif (FR-003-01). */
    public static final int LONGUEUR_MAX = 100;
    /** Pas de motif retenu. */
    public static final Motif AUCUN = new Motif(null);

    public Motif {
        if (texte != null && (texte.isBlank() || texte.length() > LONGUEUR_MAX)) {
            throw new IllegalArgumentException("Un motif est absent, ou non blanc de " + LONGUEUR_MAX
                    + " caractères au plus.");
        }
    }

    /**
     * Lit le motif saisi : absent ou blanc, aucun motif retenu ; sinon le texte tel que saisi.
     *
     * @throws ErreurVirement {@code MOTIF_TROP_LONG} au-delà de 100 caractères
     */
    public static Motif lire(String saisi) {
        if (saisi != null && saisi.length() > LONGUEUR_MAX) {
            throw new ErreurVirement(Cause.MOTIF_TROP_LONG);
        }
        return depuis(saisi);
    }

    /** Motif relu d'un stockage ou d'un événement : absent ou blanc, aucun motif retenu. */
    public static Motif depuis(String texte) {
        return texte == null || texte.isBlank() ? AUCUN : new Motif(texte);
    }

    /** Vrai si un motif est retenu. */
    public boolean present() {
        return texte != null;
    }
}
