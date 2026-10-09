package fr.nextbanking.documents.domain;

/** Rôle d'un client dans la projection : il ne donne aucun droit sur les documents d'un autre client. */
public enum Role {
    CLIENT,
    CONSEILLER,
    ADMIN
}
