import type { ClientSession, ReponseRenouvellement } from '../../application/renouvelerSession';

export const CHEMIN_DECONNEXION = '/api/identite-clients/deconnexion';
export const CHEMIN_RENOUVELLEMENT = '/api/identite-clients/renouvellement';

/**
 * Adaptateur HTTP de la session (service identité et clients).
 * Déconnexion : POST avec le jeton, toute issue est ignorée (le jeton local est déjà supprimé).
 * Renouvellement : 200 `{"jeton"}` : jeton neuf ; 401 : non authentifié ; tout le reste (autre statut, réseau
 * coupé, JSON illisible) : erreur technique. Aucun jeton n'est journalisé.
 */
export function creerClientSessionHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientSession {
  return {
    async deconnecter(jeton) {
      try {
        await appeler(CHEMIN_DECONNEXION, { method: 'POST', headers: { Authorization: `Bearer ${jeton}` } });
      } catch {
        // Panne réseau : la déconnexion locale suffit.
      }
    },
    async renouveler(jeton): Promise<ReponseRenouvellement> {
      try {
        const reponse = await appeler(CHEMIN_RENOUVELLEMENT, {
          method: 'POST',
          headers: { Authorization: `Bearer ${jeton}`, Accept: 'application/json' },
        });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        const neuf: unknown = (corps as { jeton?: unknown } | null)?.jeton;
        return typeof neuf === 'string' ? { statut: 'renouvelee', jeton: neuf } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
