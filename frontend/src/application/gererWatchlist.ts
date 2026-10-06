import type { CodePeriode } from '../domain/bourse';
import {
  chargerBourse,
  DECONNECTE,
  jetonOuFin,
  TECHNIQUE,
  type ClientBourse,
  type DemandeAjout,
  type ResultatBourse,
} from './consulterBourse';
import type { StockageJeton } from './seConnecter';

export type ResultatAjout = ResultatBourse | { type: 'refuse'; message: string };

/**
 * Cas d'usage : ajoute une valeur à la watchlist commune (FR-009-11, FR-009-12, FR-029-09, FR-029-10). Refus du service :
 * son message, rien n'est rechargé. Ajout accepté : la watchlist est relue, la nouvelle valeur est sélectionnée et
 * son historique chargé.
 */
export async function ajouterValeur(
  client: ClientBourse,
  stockage: StockageJeton,
  demande: DemandeAjout,
  periode: CodePeriode,
): Promise<ResultatAjout> {
  const jeton = jetonOuFin(stockage);
  if (jeton === null) {
    return DECONNECTE;
  }
  const reponse = await client.ajouter(jeton, demande);
  if (reponse.statut === 'nonAuthentifie') {
    stockage.supprimer();
    return DECONNECTE;
  }
  if (reponse.statut === 'technique') {
    return TECHNIQUE;
  }
  if (reponse.statut === 'refusee') {
    return { type: 'refuse', message: reponse.message };
  }
  return chargerBourse(client, stockage, reponse.donnees.symbole, periode);
}

/**
 * Cas d'usage : retire une valeur sans confirmation (FR-009-13, FR-029-12). La watchlist est relue : si la valeur
 * sélectionnée a été retirée, la première restante est sélectionnée et son historique chargé ; la dernière
 * retirée, la sélection est effacée.
 */
export async function retirerValeur(
  client: ClientBourse,
  stockage: StockageJeton,
  id: number,
  selection: string | null,
  periode: CodePeriode,
): Promise<ResultatBourse> {
  const jeton = jetonOuFin(stockage);
  if (jeton === null) {
    return DECONNECTE;
  }
  const reponse = await client.retirer(jeton, id);
  if (reponse.statut === 'nonAuthentifie') {
    stockage.supprimer();
    return DECONNECTE;
  }
  if (reponse.statut === 'technique') {
    return TECHNIQUE;
  }
  return chargerBourse(client, stockage, selection, periode);
}
