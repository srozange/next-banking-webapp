package fr.nextbanking.documents.application;

/** Paramètres de la demande invalides : type absent ou inconnu, identifiant du compte absent ou non numérique. */
public class DemandeDocumentInvalide extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Motif du refus. */
    public enum Motif {
        /** Paramètre {@code type} absent. */
        TYPE_MANQUANT,
        /** Valeur de {@code type} hors RIB, ATTESTATION, RELEVE et IFU ; {@link #valeur()} la porte telle que reçue. */
        TYPE_INCONNU,
        /** Paramètre {@code compteId} absent, vide ou non numérique. */
        COMPTE_ID_MANQUANT
    }

    private final Motif motif;
    private final String valeur;

    private DemandeDocumentInvalide(Motif motif, String valeur) {
        super(motif.name());
        this.motif = motif;
        this.valeur = valeur;
    }

    static DemandeDocumentInvalide typeManquant() {
        return new DemandeDocumentInvalide(Motif.TYPE_MANQUANT, null);
    }

    static DemandeDocumentInvalide typeInconnu(String type) {
        return new DemandeDocumentInvalide(Motif.TYPE_INCONNU, type);
    }

    static DemandeDocumentInvalide compteIdManquant() {
        return new DemandeDocumentInvalide(Motif.COMPTE_ID_MANQUANT, null);
    }

    public Motif motif() {
        return motif;
    }

    /** Valeur du type reçu, pour {@link Motif#TYPE_INCONNU} ; nulle sinon. */
    public String valeur() {
        return valeur;
    }
}
