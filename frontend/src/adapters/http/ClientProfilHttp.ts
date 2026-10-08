import type { ClientProfil, ReponseProfil } from '../../application/chargerProfil';

export const CHEMIN_PROFIL = '/api/identite-clients/moi';

/**
 * Adaptateur HTTP du profil de l'utilisateur connecté (service identité et clients).
 * 200 `{"nomComplet"}` : profil ; 401 : non authentifié ; tout le reste (autre statut, réseau coupé,
 * JSON illisible ou sans `nomComplet` textuel) : erreur technique. Le jeton n'est jamais journalisé.
 */
export function creerClientProfilHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientProfil {
  return {
    async lire(): Promise<ReponseProfil> {
      try {
        const reponse = await appeler(CHEMIN_PROFIL, {
          method: 'GET',
          headers: { Accept: 'application/json' },
        });
        if (reponse.status === 401) {
          return { statut: 'nonAuthentifie' };
        }
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        const nomComplet: unknown = (corps as { nomComplet?: unknown } | null)?.nomComplet;
        return typeof nomComplet === 'string' ? { statut: 'profil', nomComplet } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
