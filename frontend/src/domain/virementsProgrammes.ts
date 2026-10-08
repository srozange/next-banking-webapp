import { centimesDepuisSaisie } from './montant';

/** Règles pures de l'écran des virements programmés (FEAT-004) : fréquences, validation locale, date d'affichage. */

/** Fréquences proposées, dans l'ordre du legacy (FR-004-08) ; la valeur est le nom de l'énumération du service. */
export const FREQUENCES = ['HEBDOMADAIRE', 'MENSUEL', 'BIMESTRIEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL'] as const;

export type Frequence = (typeof FREQUENCES)[number];

/** Fréquence présélectionnée. */
export const FREQUENCE_PAR_DEFAUT: Frequence = 'MENSUEL';

/** Champs du formulaire, nommés comme dans le corps JSON du service. */
export type ChampVirementProgramme =
  | 'compteSourceId'
  | 'montant'
  | 'frequence'
  | 'numeroBeneficiaire'
  | 'libelleBeneficiaire'
  | 'datePremiereExecution'
  | 'motif';

export const CHAMPS_VIREMENT_PROGRAMME: readonly ChampVirementProgramme[] = [
  'compteSourceId',
  'montant',
  'frequence',
  'numeroBeneficiaire',
  'libelleBeneficiaire',
  'datePremiereExecution',
  'motif',
];

/** Saisie du formulaire de création, telle que tapée ; `compteSourceId` est vide tant qu'aucun compte n'est choisi. */
export interface FormulaireVirementProgramme {
  compteSourceId: string;
  montant: string;
  frequence: string;
  numeroBeneficiaire: string;
  libelleBeneficiaire: string;
  datePremiereExecution: string;
  motif: string;
}

export const FORMULAIRE_VIREMENT_PROGRAMME_VIDE: FormulaireVirementProgramme = {
  compteSourceId: '',
  montant: '',
  frequence: FREQUENCE_PAR_DEFAUT,
  numeroBeneficiaire: '',
  libelleBeneficiaire: '',
  datePremiereExecution: '',
  motif: '',
};

/** Longueurs maximales (FR-004-04) ; le service les contrôle aussi. */
export const LONGUEUR_MAX_NUMERO = 34;
export const LONGUEUR_MAX_LIBELLE = 100;
export const LONGUEUR_MAX_MOTIF = 255;

/** Règle de saisie violée : la première, dans l'ordre compte source, numéro, libellé, montant, date vide, format de date. */
export type RefusSaisie =
  | 'compteSourceObligatoire'
  | 'numeroObligatoire'
  | 'libelleObligatoire'
  | 'montantInvalide'
  | 'dateObligatoire'
  | 'dateInvalide';

/** Champ du formulaire désigné par un refus de saisie. */
export const CHAMP_DU_REFUS: Readonly<Record<RefusSaisie, ChampVirementProgramme>> = {
  compteSourceObligatoire: 'compteSourceId',
  numeroObligatoire: 'numeroBeneficiaire',
  libelleObligatoire: 'libelleBeneficiaire',
  montantInvalide: 'montant',
  dateObligatoire: 'datePremiereExecution',
  dateInvalide: 'datePremiereExecution',
};

const FORMAT_DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Premier refus de la saisie, ou null si elle peut être envoyée (le service valide aussi, et plus strictement la date). */
export function refusDeSaisie(formulaire: FormulaireVirementProgramme): RefusSaisie | null {
  if (formulaire.compteSourceId === '') {
    return 'compteSourceObligatoire';
  }
  if (formulaire.numeroBeneficiaire.trim() === '') {
    return 'numeroObligatoire';
  }
  if (formulaire.libelleBeneficiaire.trim() === '') {
    return 'libelleObligatoire';
  }
  const centimes = centimesDepuisSaisie(formulaire.montant);
  if (centimes === null || centimes <= 0) {
    return 'montantInvalide';
  }
  const date = formulaire.datePremiereExecution.trim();
  if (date === '') {
    return 'dateObligatoire';
  }
  return FORMAT_DATE.test(date) ? null : 'dateInvalide';
}

/** Virement programmé tel que rendu par le service : montant en chaîne, prochaine exécution en `yyyy-MM-dd`. */
export interface LigneVirementProgramme {
  id: number;
  compteSourceId: number;
  numeroCompteSource: string;
  numeroBeneficiaire: string;
  libelleBeneficiaire: string;
  montant: string;
  frequence: string;
  motif: string | null;
  prochaineExecution: string;
  actif: boolean;
  nbExecutions: number;
}

/** Date `yyyy-MM-dd` rendue `dd/MM/yyyy` (texte seulement : aucun calcul de date). */
export function dateAffichee(date: string): string {
  const [annee, mois, jour] = date.split('-');
  return `${jour}/${mois}/${annee}`;
}
