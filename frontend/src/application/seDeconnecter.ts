import { ROUTE_CONNEXION } from '../domain/connexion';
import type { StockageJeton } from './seConnecter';

/** Port sortant : informe le service identité et clients de la déconnexion (audit LOGOUT). */
export interface ClientDeconnexion {
  deconnecter(jeton: string): Promise<void>;
}

/**
 * Cas d'usage : déconnexion d'un clic (FR-001-10). Le jeton est supprimé tout de suite, puis le service en est
 * informé sans l'attendre ; une panne du service ne retient pas l'utilisateur. Rend l'écran de connexion.
 */
export function seDeconnecter(client: ClientDeconnexion, stockage: StockageJeton): string {
  const jeton = stockage.lire();
  stockage.supprimer();
  if (jeton !== null) {
    try {
      client.deconnecter(jeton).catch(() => undefined);
    } catch {
      // Le service n'est pas joignable : la session locale est déjà terminée.
    }
  }
  return ROUTE_CONNEXION;
}
