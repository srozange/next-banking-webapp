import { selectionParmi, type CodePeriode, type HistoriqueCours, type ValeurWatchlist } from '../domain/bourse';
import type { StockageJeton } from './seConnecter';

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
  lireWatchlist(jeton: string): Promise<ReponseBourse<readonly ValeurWatchlist[]>>;
  lireHistorique(jeton: string, symbole: string, periode: CodePeriode): Promise<ReponseBourse<HistoriqueCours>>;
  ajouter(jeton: string, demande: DemandeAjout): Promise<ReponseAjout>;
  retirer(jeton: string, id: number): Promise<ReponseBourse<null>>;
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

/** Jeton de la session ; sans jeton, la session est terminée (jeton supprimé) et le résultat est null. */
export function jetonOuFin(stockage: StockageJeton): string | null {
  const jeton = stockage.lire();
  if (jeton === null) {
    stockage.supprimer();
  }
  return jeton;
}

/**
 * Cas d'usage : lit la watchlist, retient la valeur souhaitée (à défaut la première) et charge son historique
 * (FR-009-02, FR-029-06). 401 : jeton supprimé, session terminée. Watchlist illisible : panne technique.
 * Historique illisible : watchlist et sélection rendues, sans historique, panne signalée.
 */
export async function chargerBourse(
  client: ClientBourse,
  stockage: StockageJeton,
  souhaitee: string | null,
  periode: CodePeriode,
): Promise<ResultatBourse> {
  const jeton = jetonOuFin(stockage);
  if (jeton === null) {
    return DECONNECTE;
  }
  const watchlist = await client.lireWatchlist(jeton);
  if (watchlist.statut === 'nonAuthentifie') {
    stockage.supprimer();
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
  const historique = await client.lireHistorique(jeton, selection, periode);
  if (historique.statut === 'nonAuthentifie') {
    stockage.supprimer();
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
  stockage: StockageJeton,
  symbole: string,
  periode: CodePeriode,
): Promise<ResultatHistorique> {
  const jeton = jetonOuFin(stockage);
  if (jeton === null) {
    return { type: 'deconnecte' };
  }
  const reponse = await client.lireHistorique(jeton, symbole, periode);
  if (reponse.statut === 'nonAuthentifie') {
    stockage.supprimer();
    return { type: 'deconnecte' };
  }
  return reponse.statut === 'ok' ? { type: 'historique', historique: reponse.donnees } : { type: 'technique' };
}
