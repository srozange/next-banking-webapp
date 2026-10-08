import type { ClientPlafonds, ReponseEnregistrementPlafond, ReponseSuppressionPlafond } from '../../application/gererPlafonds';

export const CHEMIN_PLAFONDS = '/api/coeur-bancaire/budget/plafonds';

/**
 * Adaptateur HTTP des plafonds (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie).
 * Enregistrement (PUT) : 200 enregistré ; 400 avec `message` refusé ; tout autre statut, corps illisible ou coupure,
 * technique. Suppression (DELETE) : 204 supprimé ; tout autre statut (404 compris) ou coupure, technique.
 */
export function creerClientPlafondsHttp(appeler: typeof fetch): ClientPlafonds {
  return {
    async enregistrer(commande): Promise<ReponseEnregistrementPlafond> {
      try {
        const reponse = await appeler(CHEMIN_PLAFONDS, {
          method: 'PUT',
          headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
          body: JSON.stringify(commande),
        });
        if (reponse.status === 200) {
          return { statut: 'enregistre' };
        }
        if (reponse.status !== 400) {
          return { statut: 'technique' };
        }
        const corps = (await reponse.json()) as { message?: unknown } | null;
        return typeof corps?.message === 'string' ? { statut: 'refuse', message: corps.message } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
    async supprimer(id): Promise<ReponseSuppressionPlafond> {
      try {
        const reponse = await appeler(`${CHEMIN_PLAFONDS}/${id}`, { method: 'DELETE', headers: { Accept: 'application/json' } });
        return reponse.status === 204 ? { statut: 'supprime' } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
