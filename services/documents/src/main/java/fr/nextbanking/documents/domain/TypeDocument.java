package fr.nextbanking.documents.domain;

import java.util.Locale;
import java.util.Optional;

/** Types de documents demandables (paramètre {@code type}, insensible à la casse). */
public enum TypeDocument {
    RIB(true, false, false),
    ATTESTATION(true, false, false),
    RELEVE(true, true, true),
    IFU(false, true, false);

    private final boolean exigeCompte;
    private final boolean exigeAnnee;
    private final boolean exigeMois;

    TypeDocument(boolean exigeCompte, boolean exigeAnnee, boolean exigeMois) {
        this.exigeCompte = exigeCompte;
        this.exigeAnnee = exigeAnnee;
        this.exigeMois = exigeMois;
    }

    /** Vrai si le document est celui d'une année (paramètre {@code annee} obligatoire) : relevé et IFU. */
    public boolean exigeAnnee() {
        return exigeAnnee;
    }

    /** Vrai si le document est celui d'un mois (paramètre {@code mois} obligatoire) : le relevé. */
    public boolean exigeMois() {
        return exigeMois;
    }

    /** Vrai si le document est celui d'un compte (paramètre {@code compteId} obligatoire) ; l'IFU est celui du client. */
    public boolean exigeCompte() {
        return exigeCompte;
    }

    /** Type portant ce nom, majuscules ou minuscules indifféremment, s'il existe. */
    public static Optional<TypeDocument> depuis(String nom) {
        String majuscules = nom.toUpperCase(Locale.ROOT);
        for (TypeDocument type : values()) {
            if (type.name().equals(majuscules)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
