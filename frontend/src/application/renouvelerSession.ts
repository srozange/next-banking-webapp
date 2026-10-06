import type { ClientDeconnexion } from './seDeconnecter';
import type { StockageJeton } from './seConnecter';

/** Réponse de `POST /api/identite-clients/renouvellement`, traduite par l'adaptateur HTTP. */
export type ReponseRenouvellement =
  | { statut: 'renouvelee'; jeton: string }
  | { statut: 'nonAuthentifie' }
  | { statut: 'technique' };

/** Port sortant : demande un jeton neuf. */
export interface ClientRenouvellement {
  renouveler(jeton: string): Promise<ReponseRenouvellement>;
}

/** Port sortant du service identité et clients pour la session : déconnexion et renouvellement. */
export interface ClientSession extends ClientDeconnexion, ClientRenouvellement {}

/** Port sortant : temps restant avant l'expiration d'un jeton (calcul de temps dans les adaptateurs), null si illisible. */
export interface ExpirationJeton {
  secondesRestantes(jeton: string): number | null;
}

/** Le jeton est renouvelé quand il reste moins de 10 minutes avant son expiration. */
export const SEUIL_RENOUVELLEMENT_SECONDES = 600;

export type ResultatRenouvellement = 'aucun' | 'inutile' | 'renouvelee' | 'deconnecte' | 'echec';

/**
 * Cas d'usage : renouvelle la session sur activité (FR-030-05). Aucun appel sans jeton lisible ni tant qu'il
 * reste au moins 600 s ; le jeton neuf remplace l'ancien ; sur 401 (client désactivé, jeton expiré) le jeton
 * est supprimé et la session est terminée.
 */
export async function renouvelerSession(
  client: ClientRenouvellement,
  stockage: StockageJeton,
  expiration: ExpirationJeton,
): Promise<ResultatRenouvellement> {
  const jeton = stockage.lire();
  if (jeton === null) {
    return 'aucun';
  }
  const restant = expiration.secondesRestantes(jeton);
  if (restant === null) {
    return 'aucun';
  }
  if (restant >= SEUIL_RENOUVELLEMENT_SECONDES) {
    return 'inutile';
  }
  try {
    const reponse = await client.renouveler(jeton);
    if (reponse.statut === 'nonAuthentifie') {
      stockage.supprimer();
      return 'deconnecte';
    }
    if (reponse.statut === 'technique') {
      return 'echec';
    }
    stockage.enregistrer(reponse.jeton);
    return 'renouvelee';
  } catch {
    return 'echec';
  }
}
