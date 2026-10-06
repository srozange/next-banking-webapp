import { accueilDuRole } from '../domain/connexion';
import type { DecodeurJeton, StockageJeton } from './seConnecter';

/** Réponse de `GET /api/identite-clients/moi`, traduite par l'adaptateur HTTP. */
export type ReponseProfil = { statut: 'profil'; nomComplet: string } | { statut: 'nonAuthentifie' } | { statut: 'technique' };

/** Port sortant : lit le profil de l'utilisateur connecté. */
export interface ClientProfil {
  lire(jeton: string): Promise<ReponseProfil>;
}

export type ResultatProfil = { type: 'profil'; nomComplet: string } | { type: 'deconnecte' } | { type: 'indisponible' };

const DECONNECTE: ResultatProfil = { type: 'deconnecte' };
const INDISPONIBLE: ResultatProfil = { type: 'indisponible' };

/**
 * Cas d'usage : charge le profil affiché dans l'en-tête (FR-001-05). Sans jeton ou sur 401, le jeton est
 * supprimé et la session est terminée ; toute autre panne laisse la page sans salutation.
 */
export async function chargerProfil(client: ClientProfil, stockage: StockageJeton): Promise<ResultatProfil> {
  const jeton = stockage.lire();
  if (jeton === null) {
    stockage.supprimer();
    return DECONNECTE;
  }
  try {
    const reponse = await client.lire(jeton);
    if (reponse.statut === 'nonAuthentifie') {
      stockage.supprimer();
      return DECONNECTE;
    }
    if (reponse.statut === 'technique') {
      return INDISPONIBLE;
    }
    return { type: 'profil', nomComplet: reponse.nomComplet };
  } catch {
    return INDISPONIBLE;
  }
}

/**
 * Cas d'usage : rôles de la session en cours, lus dans le jeton stocké. Sans jeton, null ; un jeton
 * illisible ou sans rôle connu est supprimé et donne aussi null. Un résultat non nul porte un rôle connu.
 */
export function rolesDeLaSession(stockage: StockageJeton, decodeur: DecodeurJeton): readonly string[] | null {
  const jeton = stockage.lire();
  if (jeton === null) {
    return null;
  }
  const roles = decodeur.roles(jeton);
  if (roles === null || accueilDuRole(roles) === null) {
    stockage.supprimer();
    return null;
  }
  return roles;
}
