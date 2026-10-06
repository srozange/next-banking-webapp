import type { Pret, SaisieSimulation, Simulation } from '../domain/pret';
import type { StockageJeton } from './seConnecter';

/** Réponse de `POST /api/credits/simulations`, traduite par l'adaptateur HTTP ; `refusee` porte le message du service (400). */
export type ReponseSimulation =
  | { statut: 'simulation'; simulation: Simulation }
  | { statut: 'refusee'; message: string }
  | { statut: 'nonAuthentifie' }
  | { statut: 'technique' };

/** Réponse de `GET /api/credits/prets`, traduite par l'adaptateur HTTP. */
export type ReponsePrets = { statut: 'prets'; prets: readonly Pret[] } | { statut: 'nonAuthentifie' } | { statut: 'technique' };

/** Port sortant : service credits. */
export interface ClientPrets {
  simuler(jeton: string, saisie: SaisieSimulation): Promise<ReponseSimulation>;
  lister(jeton: string): Promise<ReponsePrets>;
}

export type ResultatSimulation =
  | { type: 'simulation'; simulation: Simulation }
  | { type: 'refus'; message: string }
  | { type: 'deconnecte' }
  | { type: 'indisponible' };

export type ResultatPrets = { type: 'prets'; prets: readonly Pret[] } | { type: 'deconnecte' } | { type: 'indisponible' };

const DECONNECTE = { type: 'deconnecte' } as const;
const INDISPONIBLE = { type: 'indisponible' } as const;

/**
 * Cas d'usage : simule un emprunt par le service credits (FR-007-02, FR-007-03). Sans jeton ou sur 401, le jeton est
 * supprimé et la session est terminée ; un refus 400 rend le message du service ; toute autre panne est
 * « indisponible » (le texte fixe est celui de l'écran).
 */
export async function simulerEmprunt(
  client: ClientPrets,
  stockage: StockageJeton,
  saisie: SaisieSimulation,
): Promise<ResultatSimulation> {
  const jeton = stockage.lire();
  if (jeton === null) {
    stockage.supprimer();
    return DECONNECTE;
  }
  try {
    const reponse = await client.simuler(jeton, saisie);
    if (reponse.statut === 'nonAuthentifie') {
      stockage.supprimer();
      return DECONNECTE;
    }
    if (reponse.statut === 'technique') {
      return INDISPONIBLE;
    }
    if (reponse.statut === 'refusee') {
      return { type: 'refus', message: reponse.message };
    }
    return { type: 'simulation', simulation: reponse.simulation };
  } catch {
    return INDISPONIBLE;
  }
}

/** Cas d'usage : prêts de l'utilisateur connecté (FR-007-08), même traitement de la session que la simulation. */
export async function listerPrets(client: ClientPrets, stockage: StockageJeton): Promise<ResultatPrets> {
  const jeton = stockage.lire();
  if (jeton === null) {
    stockage.supprimer();
    return DECONNECTE;
  }
  try {
    const reponse = await client.lister(jeton);
    if (reponse.statut === 'nonAuthentifie') {
      stockage.supprimer();
      return DECONNECTE;
    }
    if (reponse.statut === 'technique') {
      return INDISPONIBLE;
    }
    return { type: 'prets', prets: reponse.prets };
  } catch {
    return INDISPONIBLE;
  }
}
