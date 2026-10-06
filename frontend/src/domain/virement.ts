/** Règles pures de la saisie d'un virement (FEAT-003) : champs obligatoires et comptes distincts. */

export type ChampVirement = 'compteSource' | 'compteDestinataire' | 'montant';

export interface SaisieVirement {
  /** Identifiant du compte en texte ; vide tant qu'aucun compte n'est choisi. */
  compteSource: string;
  compteDestinataire: string;
  montant: string;
  motif: string;
}

/** Champs obligatoires vides (FR-003-02), dans l'ordre de l'écran. */
export function champsVides(saisie: SaisieVirement): ChampVirement[] {
  const vides: ChampVirement[] = [];
  if (saisie.compteSource === '') {
    vides.push('compteSource');
  }
  if (saisie.compteDestinataire === '') {
    vides.push('compteDestinataire');
  }
  if (saisie.montant.trim() === '') {
    vides.push('montant');
  }
  return vides;
}

/** Même compte en source et en destination (FR-003-03). */
export function comptesIdentiques(saisie: SaisieVirement): boolean {
  return saisie.compteSource === saisie.compteDestinataire;
}

/** États d'un virement passé en revue, tels que rendus par le service. */
export type EtatVirement = 'EXECUTE' | 'SUSPENDU' | 'LIBERE' | 'REJETE';

/** Issue d'un virement traité par l'administrateur ou par le système (historique). */
export type IssueRevue = 'LIBERE' | 'REJETE' | 'REJETE_HEURE_BUTOIR' | 'REJETE_SOLDE_AU_DEBIT';

/** Virement SUSPENDU de la file de revue (montant en chaîne à deux décimales, heure butoir en ISO-8601 UTC). */
export interface VirementEnRevue {
  virementId: number;
  clientId: number;
  /** Nom complet lu dans la projection du cœur bancaire ; null tant que le client n'y est pas. */
  nomClient: string | null;
  montant: string;
  compteDestinataire: string;
  motifSuspension: string;
  heureButoir: string;
}

/** Virement libéré ou rejeté de l'historique de l'administrateur. */
export interface VirementTraite {
  virement: VirementEnRevue;
  issue: IssueRevue;
  dateIssue: string;
}

/** Virement passé en revue vu par son client (« Suivi de mes virements »). */
export interface VirementSuivi {
  virementId: number;
  soumisLe: string;
  compteDestinataire: string;
  montant: string;
  etat: EtatVirement;
  heureButoir: string;
}

const FUSEAU_METIER = 'Europe/Paris';

const formatDateHeure = new Intl.DateTimeFormat('fr-FR', {
  timeZone: FUSEAU_METIER,
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
});

function parties(instantIso: string): Record<string, string> {
  const lues: Record<string, string> = {};
  for (const partie of formatDateHeure.formatToParts(new Date(instantIso))) {
    lues[partie.type] = partie.value;
  }
  return lues;
}

/** Date `dd/MM/yyyy` d'un instant ISO-8601, en Europe/Paris (format.date). */
export function formaterDate(instantIso: string): string {
  const { day, month, year } = parties(instantIso);
  return `${day}/${month}/${year}`;
}

/** Heure `HH:mm` d'un instant ISO-8601, en Europe/Paris (format.heure). */
export function formaterHeure(instantIso: string): string {
  const { hour, minute } = parties(instantIso);
  return `${hour}:${minute}`;
}

/** Vrai si le motif d'une décision est renseigné : vide ou fait d'espaces, il est refusé (FR-016-19). */
export function motifRenseigne(motif: string): boolean {
  return motif.trim() !== '';
}
