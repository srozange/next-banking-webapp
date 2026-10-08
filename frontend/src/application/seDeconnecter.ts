import { ROUTE_CONNEXION } from '../domain/connexion';
import type { SessionLocale } from './session';

/** Port sortant : informe le service identité et clients de la déconnexion (audit LOGOUT). */
export interface ClientDeconnexion {
  deconnecter(): Promise<void>;
}

/**
 * Cas d'usage : déconnexion d'un clic (FR-001-10). Le service est informé sans l'attendre (l'appel part avant la
 * fin de la session, pour que l'adaptateur HTTP porte encore la session), puis la session locale est terminée ;
 * une panne du service ne retient pas l'utilisateur. Rend l'écran de connexion.
 */
export function seDeconnecter(client: ClientDeconnexion, session: SessionLocale): string {
  if (session.ouverte()) {
    try {
      client.deconnecter().catch(() => undefined);
    } catch {
      // Le service n'est pas joignable : la session locale est terminée juste après.
    }
  }
  session.terminer();
  return ROUTE_CONNEXION;
}
