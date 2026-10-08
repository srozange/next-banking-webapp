import { centimesDepuisChaine, chaineDepuisCentimes } from './montant';
import { progression, type PlafondBrut, type Progression } from './plafond';

/** Règles pures du budget (FEAT-006) : période, montants en centimes entiers, lignes de la répartition. */

/** Ligne de répartition telle que rendue par le service : montant en chaîne à point et deux décimales. */
export interface LigneRepartitionBrute {
  categorie: string;
  montant: string;
}

/** Budget tel que rendu par `GET /api/coeur-bancaire/budget` : dates `AAAA-MM-JJ` et `AAAA-MM`. */
export interface BudgetBrut {
  aujourdhui: string;
  periode: string;
  periodes: readonly string[];
  totalDepenses: string;
  totalRevenus: string;
  plafondsDefinis: number;
  repartition: readonly LigneRepartitionBrute[];
  /** Plafonds de la période avec la dépense de leur catégorie (ordre des catégories). */
  plafonds: readonly PlafondBrut[];
}

/** Ligne de répartition prête à afficher : montant « 1234.50 EUR » et progression sur son plafond, nulle sans plafond. */
export interface LigneRepartition {
  categorie: string;
  montant: string;
  progression: Progression | null;
}

/** Ligne du tableau des plafonds : montants « 500.00 EUR », écart plafond − dépense. */
export interface LignePlafond {
  id: number;
  categorie: string;
  plafond: string;
  depense: string;
  ecart: string;
  ecartNegatif: boolean;
}

/** Budget prêt à afficher. */
export interface Budget {
  /** Période sélectionnée (`AAAA-MM`). */
  periode: string;
  /** Périodes proposées, la période sélectionnée comprise. */
  periodes: readonly string[];
  totalDepenses: string;
  totalRevenus: string;
  plafondsDefinis: number;
  repartition: readonly LigneRepartition[];
  plafonds: readonly LignePlafond[];
}

const LONGUEUR_PERIODE = 'AAAA-MM'.length;

/** Mois (`AAAA-MM`) d'une date du service (`AAAA-MM-JJ`) : découpe de chaîne, aucune horloge du navigateur. */
export function periodeDeLaDate(date: string): string {
  return date.slice(0, LONGUEUR_PERIODE);
}

/** Montant affiché à deux décimales (« 1234.50 EUR »), calculé en centimes entiers (FR-006-04). */
export function montantEuros(montant: string): string {
  return `${chaineDepuisCentimes(centimesDepuisChaine(montant))} EUR`;
}

/**
 * Budget affichable (FR-006-02, FR-006-04) : sans aucune période proposée (client sans opération), la période est le
 * mois de la date du jour fournie par le service ; sinon c'est celle choisie par le service. La période sélectionnée
 * figure toujours parmi les options.
 */
export function budgetAffichable(brut: BudgetBrut): Budget {
  const periode = brut.periodes.length === 0 ? periodeDeLaDate(brut.aujourdhui) : brut.periode;
  return {
    periode,
    periodes: brut.periodes.includes(periode) ? brut.periodes : [periode, ...brut.periodes],
    totalDepenses: montantEuros(brut.totalDepenses),
    totalRevenus: montantEuros(brut.totalRevenus),
    plafondsDefinis: brut.plafondsDefinis,
    repartition: brut.repartition.map((ligne) => {
      const plafond = brut.plafonds.find((candidat) => candidat.categorie === ligne.categorie);
      return {
        categorie: ligne.categorie,
        montant: montantEuros(ligne.montant),
        progression: plafond === undefined ? null : progression(centimesDepuisChaine(ligne.montant), centimesDepuisChaine(plafond.plafond)),
      };
    }),
    plafonds: brut.plafonds.map(lignePlafond),
  };
}

/** Ligne du tableau des plafonds (FR-006-11) : l'écart est plafond − dépense, calculé en centimes entiers. */
function lignePlafond(brut: PlafondBrut): LignePlafond {
  const ecart = centimesDepuisChaine(brut.plafond) - centimesDepuisChaine(brut.depense);
  return {
    id: brut.id,
    categorie: brut.categorie,
    plafond: montantEuros(brut.plafond),
    depense: montantEuros(brut.depense),
    ecart: `${chaineDepuisCentimes(ecart)} EUR`,
    ecartNegatif: ecart < 0,
  };
}
