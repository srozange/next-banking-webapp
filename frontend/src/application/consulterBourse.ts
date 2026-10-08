import { selectionParmi, type CodePeriode, type HistoriqueCours, type ValeurWatchlist } from '../domain/bourse';
import { sessionOuverteOuFin, type SessionLocale } from './session';

/** Réponse d'un appel au service bourse, traduite par l'adaptateur HTTP (jamais d'exception). */
export type ReponseBourse<T> = { statut: 'ok'; donnees: T } | { statut: 'nonAuthentifie' } | { statut: 'technique' };

/** Saisie du formulaire d'ajout d'une valeur. */
export interface DemandeAjout {
  symbole: string;
  nom: string;
  marche: string;
  devise: string;
}

/** Ajout : accepté, ou refusé par le service (400) avec son texte `{"message"}`. */
export type ReponseAjout = ReponseBourse<ValeurWatchlist> | { statut: 'refusee'; message: string };

/** Port sortant : service bourse (watchlist commune et historique des cours). */
export interface ClientBourse {
  lireWatchlist(): Promise<ReponseBourse<readonly ValeurWatchlist[]>>;
  lireHistorique(symbole: string, periode: CodePeriode): Promise<ReponseBourse<HistoriqueCours>>;
  ajouter(demande: DemandeAjout): Promise<ReponseAjout>;
  retirer(id: number): Promise<ReponseBourse<null>>;
}

/** Ce que montre l'écran : la watchlist, la valeur sélectionnée et son historique (absent si non reçu). */
export interface EtatBourse {
  valeurs: readonly ValeurWatchlist[];
  selection: string | null;
  historique: HistoriqueCours | null;
  /** Une panne technique a empêché de tout charger : l'écran reste utilisable et l'annonce. */
  technique: boolean;
}

export type ResultatBourse = { type: 'etat'; etat: EtatBourse } | { type: 'technique' } | { type: 'deconnecte' };

export const DECONNECTE: ResultatBourse = { type: 'deconnecte' };
export const TECHNIQUE: ResultatBourse = { type: 'technique' };

/**
 * Cas d'usage : lit la watchlist, retient la valeur souhaitée (à défaut la première) et charge son historique
 * (FR-009-02, FR-029-06). 401 : session terminée. Watchlist illisible : panne technique.
 * Historique illisible : watchlist et sélection rendues, sans historique, panne signalée.
 */
export async function chargerBourse(
  client: ClientBourse,
  session: SessionLocale,
  souhaitee: string | null,
  periode: CodePeriode,
): Promise<ResultatBourse> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  const watchlist = await client.lireWatchlist();
  if (watchlist.statut === 'nonAuthentifie') {
    session.terminer();
    return DECONNECTE;
  }
  if (watchlist.statut === 'technique') {
    return TECHNIQUE;
  }
  const valeurs = watchlist.donnees;
  const selection = selectionParmi(valeurs, souhaitee);
  if (selection === null) {
    return { type: 'etat', etat: { valeurs, selection, historique: null, technique: false } };
  }
  const historique = await client.lireHistorique(selection, periode);
  if (historique.statut === 'nonAuthentifie') {
    session.terminer();
    return DECONNECTE;
  }
  return {
    type: 'etat',
    etat: {
      valeurs,
      selection,
      historique: historique.statut === 'ok' ? historique.donnees : null,
      technique: historique.statut === 'technique',
    },
  };
}

export type ResultatHistorique = { type: 'historique'; historique: HistoriqueCours } | { type: 'technique' } | { type: 'deconnecte' };

/** Cas d'usage : recharge seulement l'historique d'une valeur et d'une période (FR-009-03). */
export async function chargerHistorique(
  client: ClientBourse,
  session: SessionLocale,
  symbole: string,
  periode: CodePeriode,
): Promise<ResultatHistorique> {
  if (!sessionOuverteOuFin(session)) {
    return { type: 'deconnecte' };
  }
  const reponse = await client.lireHistorique(symbole, periode);
  if (reponse.statut === 'nonAuthentifie') {
    session.terminer();
    return { type: 'deconnecte' };
  }
  return reponse.statut === 'ok' ? { type: 'historique', historique: reponse.donnees } : { type: 'technique' };
}
