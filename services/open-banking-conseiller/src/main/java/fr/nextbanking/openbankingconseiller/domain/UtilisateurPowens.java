package fr.nextbanking.openbankingconseiller.domain;

/**
 * Utilisateur anonyme créé chez Powens. Le jeton peut manquer si la réponse de Powens n'en porte pas.
 *
 * @param idUtilisateur identifiant numérique chez Powens
 * @param jeton         jeton permanent, jamais rendu ni journalisé
 */
public record UtilisateurPowens(long idUtilisateur, String jeton) {
}
