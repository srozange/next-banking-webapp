/**
 * Règles pures de l'écran Bourse (FEAT-009, FEAT-029) : périodes, indicateurs du cours, dernières séances et choix
 * de la valeur sélectionnée. Les prix arrivent en chaînes décimales du service ; tout calcul se fait en centimes
 * entiers, jamais en flottant.
 */

/** Périodes de l'historique (FR-009-03), « 1M » présélectionnée (FR-029-06). */
export const PERIODES = ['1W', '1M', '1Y', '5Y'] as const;
export type CodePeriode = (typeof PERIODES)[number];
export const PERIODE_PAR_DEFAUT: CodePeriode = '1M';

export const SEANCES_AFFICHEES = 10;

/** Valeur de la watchlist commune, telle que rendue par le service. */
export interface ValeurWatchlist {
  id: number;
  symbole: string;
  nom: string;
  marche: string;
  devise: string;
}

/** Séance cotée : prix en chaînes décimales (le service peut en rendre plus de deux décimales). */
export interface PointCotation {
  date: string;
  ouverture: string;
  haut: string;
  bas: string;
  cloture: string;
  volume: number;
}

/** Historique d'une valeur : points par date croissante, graphique SVG du service absent s'il n'y a rien à tracer. */
export interface HistoriqueCours {
  symbole: string;
  periode: string;
  points: readonly PointCotation[];
  graphique: string | null;
}

const FORMAT_PRIX = /^(\d+)(?:\.(\d+))?$/;

/** Prix décimal ("170.1600") en centimes entiers, arrondi à la demi-unité supérieure sur le troisième décimal. */
export function centimesDepuisPrix(prix: string): number {
  const decoupe = FORMAT_PRIX.exec(prix);
  if (decoupe === null) {
    throw new Error(`Prix invalide : ${prix}`);
  }
  const decimales = (decoupe[2] ?? '').padEnd(3, '0');
  const arrondi = decimales.charAt(2) >= '5' ? 1 : 0;
  return Number(decoupe[1]) * 100 + Number(decimales.slice(0, 2)) + arrondi;
}

/** Centimes entiers en texte à deux décimales : -260 donne "-2.60". */
export function texteDepuisCentimes(centimes: number): string {
  const absolu = Math.abs(centimes);
  const entiers = Math.floor(absolu / 100);
  const decimales = String(absolu % 100).padStart(2, '0');
  return `${centimes < 0 ? '-' : ''}${entiers}.${decimales}`;
}

/**
 * Pourcentage (numérateur / dénominateur × 100) en centièmes de pour cent entiers, arrondi à la demi-unité en
 * s'éloignant de zéro, par division entière. Un dénominateur nul donne 0.
 */
export function pourcentageEnCentiemes(numerateur: number, denominateur: number): number {
  if (denominateur === 0) {
    return 0;
  }
  const quotient = Math.floor((2 * Math.abs(numerateur) * 10000 + Math.abs(denominateur)) / (2 * Math.abs(denominateur)));
  return numerateur * denominateur < 0 ? -quotient : quotient;
}

/** Centièmes de pour cent en texte à deux décimales : -150 donne "-1.50". */
export function texteDepuisCentiemes(centiemes: number): string {
  return texteDepuisCentimes(centiemes);
}

export interface Indicateurs {
  /** Dernière clôture. */
  cours: string;
  /** Dernière clôture moins première clôture. */
  variation: string;
  /** (dernière − première) / première × 100, en pourcentage. */
  performance: string;
  /** Performance supérieure ou égale à zéro : vert, « ▲ + » ; sinon rouge, « ▼ ». */
  hausse: boolean;
}

/** Indicateurs de la période (FR-009-04, FR-009-05, FR-029-07), ou null sans point. */
export function calculerIndicateurs(points: readonly PointCotation[]): Indicateurs | null {
  const premier = points[0];
  const dernier = points[points.length - 1];
  if (premier === undefined || dernier === undefined) {
    return null;
  }
  const debut = centimesDepuisPrix(premier.cloture);
  const fin = centimesDepuisPrix(dernier.cloture);
  return {
    cours: texteDepuisCentimes(fin),
    variation: texteDepuisCentimes(fin - debut),
    performance: texteDepuisCentiemes(pourcentageEnCentiemes(fin - debut, debut)),
    hausse: fin >= debut,
  };
}

export interface Seance {
  date: string;
  ouverture: string;
  haut: string;
  bas: string;
  cloture: string;
  volume: number;
  /** (clôture − ouverture) / ouverture × 100, en pourcentage. */
  variationJour: string;
  /** Clôture supérieure ou égale à l'ouverture : ligne verte et préfixe « + » ; sinon rouge. */
  hausse: boolean;
}

/** Les 10 séances les plus récentes, de la plus récente à la plus ancienne (FR-009-07, FR-029-08). */
export function dernieresSeances(points: readonly PointCotation[]): Seance[] {
  return points
    .slice(-SEANCES_AFFICHEES)
    .reverse()
    .map((point) => {
      const ouverture = centimesDepuisPrix(point.ouverture);
      const cloture = centimesDepuisPrix(point.cloture);
      return {
        date: point.date,
        ouverture: texteDepuisCentimes(ouverture),
        haut: texteDepuisCentimes(centimesDepuisPrix(point.haut)),
        bas: texteDepuisCentimes(centimesDepuisPrix(point.bas)),
        cloture: texteDepuisCentimes(cloture),
        volume: point.volume,
        variationJour: texteDepuisCentiemes(pourcentageEnCentiemes(cloture - ouverture, ouverture)),
        hausse: cloture >= ouverture,
      };
    });
}

/**
 * Valeur sélectionnée dans la watchlist : la valeur souhaitée si elle y figure (valeur ajoutée, sélection conservée),
 * sinon la première (ouverture de l'écran, valeur retirée), sinon aucune (FR-009-02, FR-009-12, FR-009-13, FR-029-10, FR-029-12).
 */
export function selectionParmi(valeurs: readonly ValeurWatchlist[], souhaitee: string | null): string | null {
  const retenue = valeurs.find((valeur) => valeur.symbole === souhaitee) ?? valeurs[0];
  return retenue?.symbole ?? null;
}
