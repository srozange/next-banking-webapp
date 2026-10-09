package fr.nextbanking.documents.domain;

import java.util.Locale;
import java.util.Optional;

/** Types de documents demandables (paramètre {@code type}, insensible à la casse). */
public enum TypeDocument {
    RIB(true),
    ATTESTATION(true),
    RELEVE(true),
    IFU(false);

    private final boolean exigeCompte;

    TypeDocument(boolean exigeCompte) {
        this.exigeCompte = exigeCompte;
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
