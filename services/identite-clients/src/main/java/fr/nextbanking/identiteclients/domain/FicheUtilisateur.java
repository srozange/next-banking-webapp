package fr.nextbanking.identiteclients.domain;

import java.time.LocalDate;

/**
 * Utilisateur vu par l'administration, en lecture : identifiant numérique, nom complet, identifiant de connexion,
 * adresse électronique, téléphone (absent possible), rôle, état actif, nom complet du conseiller affecté (absent
 * possible) et date de création de la fiche (« Client depuis », BUG-055 : jamais la date d'affectation).
 */
public record FicheUtilisateur(long id, String nomComplet, String login, String email, String telephone, Role role,
        boolean actif, String nomConseiller, LocalDate dateCreation) {

    /** Vrai pour un utilisateur de rôle CLIENT. */
    public boolean estClient() {
        return role == Role.CLIENT;
    }

    /** Vrai pour un utilisateur de rôle CONSEILLER. */
    public boolean estConseiller() {
        return role == Role.CONSEILLER;
    }
}
