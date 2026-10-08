import type { ClientConnexion, ReponseConnexion } from '../../application/seConnecter';
import { accueilDuRole } from '../../domain/connexion';
import type { DecodeurJeton } from '../jeton/DecodeurJetonJwt';
import type { StockageJeton } from '../stockage/StockageJetonLocal';

export const CHEMIN_CONNEXION = '/api/identite-clients/connexion';

/**
 * Adaptateur HTTP de l'API de connexion du service identité et clients.
 * 200 `{"jeton"}` : connexion acceptée, rôles lus dans le jeton, jeton enregistré (seulement si un rôle mène à un
 * écran d'accueil connu) ; 401 : refus ; tout le reste (autre statut, réseau coupé, JSON illisible, jeton illisible
 * ou sans rôle connu) : erreur technique.
 * Ni le mot de passe ni le jeton ne sont journalisés.
 */
export function creerClientConnexionHttp(
  stockage: StockageJeton,
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
        if (typeof jeton !== 'string' || roles === null || accueilDuRole(roles) === null) {
          return { statut: 'technique' };
        }
        stockage.enregistrer(jeton);
        return { statut: 'acceptee', roles };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
