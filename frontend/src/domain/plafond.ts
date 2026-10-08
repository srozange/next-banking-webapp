/** Règles pures des plafonds de dépenses (FEAT-006) : saisie, progression, écart. Tout est calculé en centimes entiers. */

/** Catégories de dépenses proposées pour un plafond, dans l'ordre du formulaire : « Revenus » n'y figure pas (FR-006-06). */
export const CATEGORIES_PLAFONNABLES = [
  'Logement',
  'Alimentation',
  'Transport',
  'Sante',
  'Loisirs',
  'Abonnements',
  'Epargne',
  'Assurances',
  'Autres',
] as const;

/** Nombre maximal de chiffres avant la virgule d'un plafond (capacité du montant, NFR-F006). */
export const CHIFFRES_ENTIERS_MAX = 13;

/** Plus grand plafond représentable en centimes : 13 chiffres entiers, 2 décimales. */
const CENTIMES_MAX = 999_999_999_999_999;

/** Saisie du formulaire de plafond. */
export interface FormulairePlafond {
  categorie: string;
  plafond: string;
}

export const FORMULAIRE_PLAFOND_VIDE: FormulairePlafond = { categorie: '', plafond: '' };

/** Motif de refus local d'un plafond, avant tout appel au service. */
export type RefusPlafond = 'categorie' | 'plafond' | 'plafondTropGrand';

const FORMAT_SAISIE = /^(\d+)(?:[.,](\d+))?$/;

/** Résultat de la lecture d'un plafond saisi : centimes entiers, ou le motif du refus. */
export type LecturePlafond = { centimes: number } | { refus: 'plafond' | 'plafondTropGrand' };

/**
 * Lit un plafond saisi (« 500 », « 500,5 », « 500.123 ») en centimes entiers : strictement positif, arrondi à deux
 * décimales (moitié vers le haut) comme le service, au plus 13 chiffres entiers. Vide, nul, négatif ou non
 * numérique : refus « plafond » ; 14 chiffres entiers ou plus : refus « plafondTropGrand ».
 */
export function lirePlafond(saisie: string): LecturePlafond {
  const lecture = FORMAT_SAISIE.exec(saisie.trim());
  if (lecture === null) {
    return { refus: 'plafond' };
  }
  const entiers = lecture[1].replace(/^0+(?=\d)/, '');
  const decimales = (lecture[2] ?? '').padEnd(3, '0');
  if (entiers.length > CHIFFRES_ENTIERS_MAX) {
    return { refus: 'plafondTropGrand' };
  }
  const arrondi = Number(decimales.slice(2, 3)) >= 5 ? 1 : 0;
  const centimes = Number(entiers) * 100 + Number(decimales.slice(0, 2)) + arrondi;
  if (centimes === 0) {
    return { refus: 'plafond' };
  }
  return centimes > CENTIMES_MAX ? { refus: 'plafondTropGrand' } : { centimes };
}

/** Premier refus de la saisie dans l'ordre du legacy (catégorie, puis plafond), ou null si elle est acceptable. */
export function refusDePlafond(formulaire: FormulairePlafond): RefusPlafond | null {
  if (!CATEGORIES_PLAFONNABLES.includes(formulaire.categorie as (typeof CATEGORIES_PLAFONNABLES)[number])) {
    return 'categorie';
  }
  const lecture = lirePlafond(formulaire.plafond);
  return 'refus' in lecture ? lecture.refus : null;
}

/** Couleur de la barre, par les tokens du thème : vert sous 80 %, orange de 80 à 99 %, rouge dès 100 %. */
export type CouleurProgression = 'success' | 'warning' | 'error';

/** Progression des dépenses d'une catégorie par rapport à son plafond. */
export interface Progression {
  /** Pourcentage entier (division entière, jamais de flottant) : peut dépasser 100. */
  pourcentage: number;
  /** Remplissage de la barre, plafonné à 100. */
  remplissage: number;
  couleur: CouleurProgression;
  /** Vrai seulement si la dépense est strictement supérieure au plafond (badge « Depassement »). */
  depasse: boolean;
}

/** Progression de `depenseCentimes` sur `plafondCentimes` (strictement positif), FR-006-09 et FR-006-10. */
export function progression(depenseCentimes: number, plafondCentimes: number): Progression {
  const pourcentage = Number((BigInt(depenseCentimes) * 100n) / BigInt(plafondCentimes));
  return {
    pourcentage,
    remplissage: Math.min(pourcentage, 100),
    couleur: pourcentage >= 100 ? 'error' : pourcentage >= 80 ? 'warning' : 'success',
    depasse: depenseCentimes > plafondCentimes,
  };
}

/** Plafond tel que rendu par le service pour la période : montants en chaîne à point et deux décimales. */
export interface PlafondBrut {
  id: number;
  categorie: string;
  plafond: string;
  depense: string;
}
