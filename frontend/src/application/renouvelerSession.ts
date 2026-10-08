import type { ClientDeconnexion } from './seDeconnecter';
import type { SessionLocale } from './session';

/** Réponse de `POST /api/identite-clients/renouvellement`, traduite par l'adaptateur HTTP, qui a déjà enregistré la session neuve. */
export type ReponseRenouvellement =
  | { statut: 'renouvelee' }
  | { statut: 'nonAuthentifie' }
  | { statut: 'technique' };

/** Port sortant : demande une session neuve. */
export interface ClientRenouvellement {
  renouveler(): Promise<ReponseRenouvellement>;
}

/** Port sortant du service identité et clients pour la session : déconnexion et renouvellement. */
export interface ClientSession extends ClientDeconnexion, ClientRenouvellement {}

/** La session est renouvelée quand il reste moins de 10 minutes avant son expiration. */
export const SEUIL_RENOUVELLEMENT_SECONDES = 600;

export type ResultatRenouvellement = 'aucun' | 'inutile' | 'renouvelee' | 'deconnecte' | 'echec';

/**
 * Cas d'usage : renouvelle la session sur activité (FR-030-05). Aucun appel sans session lisible ni tant qu'il
 * reste au moins 600 s ; la session neuve remplace l'ancienne ; sur 401 (client désactivé, session expirée) la
 * session est terminée.
 */
export async function renouvelerSession(
  client: ClientRenouvellement,
  session: SessionLocale,
): Promise<ResultatRenouvellement> {
  const restant = session.secondesRestantes();
  if (restant === null) {
    return 'aucun';
  }
  if (restant >= SEUIL_RENOUVELLEMENT_SECONDES) {
    return 'inutile';
  }
  try {
    const reponse = await client.renouveler();
    if (reponse.statut === 'nonAuthentifie') {
      session.terminer();
      return 'deconnecte';
    }
    if (reponse.statut === 'technique') {
      return 'echec';
    }
    return 'renouvelee';
  } catch {
    return 'echec';
  }
}
