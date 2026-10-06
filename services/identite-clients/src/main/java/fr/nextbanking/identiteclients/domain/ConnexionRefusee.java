package fr.nextbanking.identiteclients.domain;

/**
 * Connexion refusée, avec son motif. Le message ne porte que le motif, jamais un identifiant ni un mot de passe.
 */
public class ConnexionRefusee extends RuntimeException {

    /** Motifs de refus. */
    public enum Motif {
        IDENTIFIANT_REQUIS,
        MOT_DE_PASSE_REQUIS,
        IDENTIFIANTS_INCORRECTS
    }

    private final Motif motif;

    public ConnexionRefusee(Motif motif) {
        super("Connexion refusée : " + motif);
        this.motif = motif;
    }

    public Motif motif() {
        return motif;
    }
}
