import type { ClientSession, ReponseRenouvellement } from '../../application/renouvelerSession';
import type { StockageJeton } from '../stockage/StockageJetonLocal';

export const CHEMIN_DECONNEXION = '/api/identite-clients/deconnexion';
export const CHEMIN_RENOUVELLEMENT = '/api/identite-clients/renouvellement';

/**
 * Adaptateur HTTP de la session (service identité et clients).
 * Déconnexion : POST (le jeton est porté par `fetchAuthentifie`), toute issue est ignorée.
 * Renouvellement : 200 `{"jeton"}` : jeton neuf, enregistré ici ; 401 : non authentifié ; tout le reste (autre statut, réseau
 * coupé, JSON illisible) : erreur technique. Aucun jeton n'est journalisé.
 */
export function creerClientSessionHttp(
  stockage: StockageJeton,
  appeler: typeof fetch = (entree, options) => fetch(entree, options),
): ClientSession {
  return {
    async deconnecter() {
      try {
        await appeler(CHEMIN_DECONNEXION, { method: 'POST' });
      } catch {
        // Panne réseau : la déconnexion locale suffit.
      }
    },
    async renouveler(): Promise<ReponseRenouvellement> {
      try {
        const reponse = await appeler(CHEMIN_RENOUVELLEMENT, {
          method: 'POST',
          headers: { Accept: 'application/json' },
        });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        const neuf: unknown = (corps as { jeton?: unknown } | null)?.jeton;
        if (typeof neuf !== 'string') {
          return { statut: 'technique' };
        }
        stockage.enregistrer(neuf);
        return { statut: 'renouvelee' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
