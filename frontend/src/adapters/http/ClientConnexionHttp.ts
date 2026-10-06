import type { ClientConnexion, DecodeurJeton, ReponseConnexion } from '../../application/seConnecter';

export const CHEMIN_CONNEXION = '/api/identite-clients/connexion';

/**
 * Adaptateur HTTP de l'API de connexion du service identité et clients.
 * 200 `{"jeton"}` : connexion acceptée, rôles lus dans le jeton ; 401 : refus ; tout le reste
 * (autre statut, réseau coupé, JSON illisible, jeton illisible) : erreur technique.
 * Ni le mot de passe ni le jeton ne sont journalisés.
 */
export function creerClientConnexionHttp(
  decodeur: DecodeurJeton,
  appeler: typeof fetch = (entree, options) => fetch(entree, options),
): ClientConnexion {
  return {
    async connecter(identifiant, motDePasse): Promise<ReponseConnexion> {
      try {
        const reponse = await appeler(CHEMIN_CONNEXION, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
          body: JSON.stringify({ identifiant, motDePasse }),
        });
        if (reponse.status === 401) {
          return { statut: 'refusee' };
        }
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        const jeton: unknown = (corps as { jeton?: unknown } | null)?.jeton;
        const roles = typeof jeton === 'string' ? decodeur.roles(jeton) : null;
        if (typeof jeton !== 'string' || roles === null) {
          return { statut: 'technique' };
        }
        return { statut: 'acceptee', jeton, roles };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
