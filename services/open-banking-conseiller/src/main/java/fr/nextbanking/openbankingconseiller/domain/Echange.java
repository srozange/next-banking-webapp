package fr.nextbanking.openbankingconseiller.domain;

/**
 * Un tour de la conversation avec Sophie, fourni par l'appelant (le service ne garde aucun historique).
 *
 * @param role    auteur du tour
 * @param contenu texte du tour
 */
public record Echange(RoleEchange role, String contenu) {

    public Echange {
        if (role == null || contenu == null) {
            throw new IllegalArgumentException("Un échange porte un rôle et un contenu");
        }
    }
}
