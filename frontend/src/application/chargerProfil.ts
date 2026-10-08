import { sessionOuverteOuFin, type SessionLocale } from './session';

/** Réponse de `GET /api/identite-clients/moi`, traduite par l'adaptateur HTTP. */
export type ReponseProfil = { statut: 'profil'; nomComplet: string } | { statut: 'nonAuthentifie' } | { statut: 'technique' };

/** Port sortant : lit le profil de l'utilisateur connecté. */
export interface ClientProfil {
  lire(): Promise<ReponseProfil>;
}

export type ResultatProfil = { type: 'profil'; nomComplet: string } | { type: 'deconnecte' } | { type: 'indisponible' };

const DECONNECTE: ResultatProfil = { type: 'deconnecte' };
const INDISPONIBLE: ResultatProfil = { type: 'indisponible' };

/**
 * Cas d'usage : charge le profil affiché dans l'en-tête (FR-001-05). Sans session ou sur 401, la
 * session est terminée ; toute autre panne laisse la page sans salutation.
 */
export async function chargerProfil(client: ClientProfil, session: SessionLocale): Promise<ResultatProfil> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  try {
    const reponse = await client.lire();
    if (reponse.statut === 'nonAuthentifie') {
      session.terminer();
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
