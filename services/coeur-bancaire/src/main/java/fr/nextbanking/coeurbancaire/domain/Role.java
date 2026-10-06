package fr.nextbanking.coeurbancaire.domain;

import java.util.Optional;

/** Rôle porté par le jeton ; seul ADMIN libère et rejette un virement suspendu. */
public enum Role {
    CLIENT,
    CONSEILLER,
    ADMIN;

    /** Rôle portant exactement ce nom, s'il existe. */
    public static Optional<Role> depuis(String nom) {
        for (Role role : values()) {
            if (role.name().equals(nom)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
