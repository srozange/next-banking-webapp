package fr.nextbanking.identiteclients.domain;

/**
 * Utilisateur enregistré (table {@code customers}) : identifiant numérique, identifiant de connexion, nom complet,
 * rôle, état actif et empreinte du mot de passe. L'empreinte n'apparaît jamais dans {@link #toString()}.
 */
public record Client(long id, String identifiant, String nomComplet, Role role, boolean actif,
        String empreinteMotDePasse) {

    public Client {
        if (id <= 0) {
            throw new IllegalArgumentException("L'identifiant numérique du client doit être strictement positif.");
        }
        if (identifiant == null || identifiant.isBlank()) {
            throw new IllegalArgumentException("L'identifiant de connexion du client est obligatoire.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Le rôle du client est obligatoire.");
        }
    }

    @Override
    public String toString() {
        return "Client[id=" + id + ", identifiant=" + identifiant + ", role=" + role + ", actif=" + actif + "]";
    }
}
