import type { Echeance, Pret, SaisieDemande, SaisieSimulation, Simulation } from '../domain/pret';
import { sessionOuverteOuFin, type SessionLocale } from './session';

/** Réponse de `POST /api/credits/simulations`, traduite par l'adaptateur HTTP ; `refusee` porte le message du service (400). */
export type ReponseSimulation =
  | { statut: 'simulation'; simulation: Simulation }
  | { statut: 'refusee'; message: string }
  | { statut: 'nonAuthentifie' }
  | { statut: 'technique' };

/** Réponse de `GET /api/credits/prets`, traduite par l'adaptateur HTTP. */
export type ReponsePrets = { statut: 'prets'; prets: readonly Pret[] } | { statut: 'nonAuthentifie' } | { statut: 'technique' };

/**
 * Réponse de `POST /api/credits/prets`, traduite par l'adaptateur HTTP : `deposee` (201, prêt lu) ; `refusee` porte le
 * message du service (400 refus de la demande, 503 transmission impossible) ; 401 ; tout le reste est `technique`.
 */
export type ReponseDepot =
  | { statut: 'deposee'; pret: Pret }
  | { statut: 'refusee'; message: string }
  | { statut: 'nonAuthentifie' }
  | { statut: 'technique' };

/** Réponse de `GET /api/credits/prets/{id}/echeances`, traduite par l'adaptateur HTTP. */
export type ReponseEcheances =
  | { statut: 'echeances'; echeances: readonly Echeance[] }
  | { statut: 'nonAuthentifie' }
  | { statut: 'technique' };

/** Réponse du paiement d'une échéance ou de la suppression d'un prêt : `faite` (succès), 401, ou tout le reste `technique`. */
export type ReponseAction = { statut: 'faite' } | { statut: 'nonAuthentifie' } | { statut: 'technique' };

/** Port sortant : service credits. */
export interface ClientPrets {
  simuler(saisie: SaisieSimulation): Promise<ReponseSimulation>;
  lister(): Promise<ReponsePrets>;
  deposer(saisie: SaisieDemande): Promise<ReponseDepot>;
  echeances(pretId: number): Promise<ReponseEcheances>;
  payer(echeanceId: number): Promise<ReponseAction>;
  supprimer(pretId: number): Promise<ReponseAction>;
}

export type ResultatSimulation =
  | { type: 'simulation'; simulation: Simulation }
  | { type: 'refus'; message: string }
  | { type: 'deconnecte' }
  | { type: 'indisponible' };

export type ResultatPrets = { type: 'prets'; prets: readonly Pret[] } | { type: 'deconnecte' } | { type: 'indisponible' };

export type ResultatDepot =
  | { type: 'deposee'; pret: Pret }
  | { type: 'refus'; message: string }
  | { type: 'deconnecte' }
  | { type: 'indisponible' };

export type ResultatEcheances =
  | { type: 'echeances'; echeances: readonly Echeance[] }
  | { type: 'deconnecte' }
  | { type: 'indisponible' };

/** Issue d'une action sur un prêt : faite, session perdue, ou panne quelle qu'en soit la cause (403, 404, 500, réseau). */
export type ResultatAction = { type: 'faite' } | { type: 'deconnecte' } | { type: 'indisponible' };

const DECONNECTE = { type: 'deconnecte' } as const;
const INDISPONIBLE = { type: 'indisponible' } as const;

/**
 * Cas d'usage : simule un emprunt par le service credits (FR-007-02, FR-007-03). Sans session ou sur 401, la session
 * est terminée ; un refus 400 rend le message du service ; toute autre panne est
 * « indisponible » (le texte fixe est celui de l'écran).
 */
export async function simulerEmprunt(
  client: ClientPrets,
  session: SessionLocale,
  saisie: SaisieSimulation,
): Promise<ResultatSimulation> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  try {
    const reponse = await client.simuler(saisie);
    if (reponse.statut === 'nonAuthentifie') {
      session.terminer();
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
export async function listerPrets(client: ClientPrets, session: SessionLocale): Promise<ResultatPrets> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  try {
    const reponse = await client.lister();
    if (reponse.statut === 'nonAuthentifie') {
      session.terminer();
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

/**
 * Cas d'usage : dépose une demande de prêt auprès du service credits (FR-007-05, FR-007-07, FR-024-02). Sans session ou
 * sur 401, la session est terminée ; un refus (400, 503) rend le message du service ; toute
 * autre panne est « indisponible » (le texte fixe est celui de l'écran).
 */
export async function deposerDemande(
  client: ClientPrets,
  session: SessionLocale,
  saisie: SaisieDemande,
): Promise<ResultatDepot> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  try {
    const reponse = await client.deposer(saisie);
    if (reponse.statut === 'nonAuthentifie') {
      session.terminer();
      return DECONNECTE;
    }
    if (reponse.statut === 'technique') {
      return INDISPONIBLE;
    }
    if (reponse.statut === 'refusee') {
      return { type: 'refus', message: reponse.message };
    }
    return { type: 'deposee', pret: reponse.pret };
  } catch {
    return INDISPONIBLE;
  }
}

/** Cas d'usage : échéancier d'un prêt de l'utilisateur (FR-007-11), même traitement de la session que la liste. */
export async function consulterEcheances(
  client: ClientPrets,
  session: SessionLocale,
  pretId: number,
): Promise<ResultatEcheances> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  try {
    const reponse = await client.echeances(pretId);
    if (reponse.statut === 'nonAuthentifie') {
      session.terminer();
      return DECONNECTE;
    }
    return reponse.statut === 'technique' ? INDISPONIBLE : { type: 'echeances', echeances: reponse.echeances };
  } catch {
    return INDISPONIBLE;
  }
}

async function agir(
  session: SessionLocale,
  action: () => Promise<ReponseAction>,
): Promise<ResultatAction> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  try {
    const reponse = await action();
    if (reponse.statut === 'nonAuthentifie') {
      session.terminer();
      return DECONNECTE;
    }
    return reponse.statut === 'technique' ? INDISPONIBLE : { type: 'faite' };
  } catch {
    return INDISPONIBLE;
  }
}

/** Cas d'usage : marque une échéance payée (FR-007-12). Sans session ou sur 401, la session est terminée. */
export function payerEcheance(client: ClientPrets, session: SessionLocale, echeanceId: number): Promise<ResultatAction> {
  return agir(session, () => client.payer(echeanceId));
}

/** Cas d'usage : supprime un prêt de l'utilisateur (FR-007-13). Sans session ou sur 401, la session est terminée. */
export function supprimerPret(client: ClientPrets, session: SessionLocale, pretId: number): Promise<ResultatAction> {
  return agir(session, () => client.supprimer(pretId));
}
