import { centimesDepuisChaine } from './montant';

/**
 * Règles pures de la page Prêts (FEAT-007) : types et statuts du legacy, libellés de MESSAGES.md
 * (§FEAT-007, « Libellés d'énumérations »), formatage des montants, taux et durées à l'anglo-saxonne du legacy.
 * Les montants circulent en chaînes à deux décimales ; seul le formatage passe par des centimes entiers.
 */

export type TypePret = 'IMMOBILIER' | 'AUTO' | 'CONSOMMATION' | 'TRAVAUX' | 'ETUDIANT';
export type StatutPret = 'EN_ATTENTE' | 'EN_COURS' | 'REMBOURSE' | 'REFUSE' | 'DEFAUT';

/** Valeurs par défaut du simulateur (RG-007-006) et de la demande (FEAT-007 FIELDS). */
export const SAISIE_SIMULATION_DEFAUT = { montant: '10000', tauxAnnuel: '3.50', dureeMois: '36' } as const;
export const TYPE_PRET_DEFAUT: TypePret = 'CONSOMMATION';

/** Types proposés, dans l'ordre du legacy (RG-007-011). */
export const TYPES_PRET: ReadonlyArray<{ valeur: TypePret; libelle: string }> = [
  { valeur: 'IMMOBILIER', libelle: 'Immobilier' },
  { valeur: 'AUTO', libelle: 'Automobile' },
  { valeur: 'CONSOMMATION', libelle: 'Consommation' },
  { valeur: 'TRAVAUX', libelle: 'Travaux' },
  { valeur: 'ETUDIANT', libelle: 'Etudiant' },
];

export const LIBELLES_STATUT: Readonly<Record<StatutPret, string>> = {
  EN_ATTENTE: 'En attente d approbation',
  EN_COURS: 'En cours',
  REMBOURSE: 'Rembourse',
  REFUSE: 'Refuse par le service credit',
  DEFAUT: 'En defaut',
};

/** Saisie du simulateur : chaînes envoyées telles quelles au service. */
export interface SaisieSimulation {
  montant: string;
  tauxAnnuel: string;
  dureeMois: string;
}

export interface LigneEcheancier {
  numero: number;
  capital: string;
  interets: string;
  total: string;
  capitalRestant: string;
}

export interface Simulation {
  mensualite: string;
  coutTotal: string;
  totalInterets: string;
  echeancier: readonly LigneEcheancier[];
}

export interface Pret {
  id: number;
  type: TypePret;
  montant: string;
  tauxAnnuel: string;
  dureeMois: number;
  mensualite: string;
  coutTotal: string;
  totalInterets: string;
  objet: string | null;
  statut: StatutPret;
  dateDebut: string;
  dateFin: string;
}

/** Libellé d'un type de prêt. */
export function libelleType(type: TypePret): string {
  return (TYPES_PRET.find(({ valeur }) => valeur === type) as { libelle: string }).libelle;
}

/** Centimes entiers en « 10,548.72 » : milliers séparés par des virgules, point décimal, deux décimales. */
export function formaterCentimes(centimes: number): string {
  const absolu = Math.abs(centimes);
  const entiers = String(Math.floor(absolu / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const decimales = String(absolu % 100).padStart(2, '0');
  return `${centimes < 0 ? '-' : ''}${entiers}.${decimales}`;
}

/** Montant de la simulation : « 10,548.72 EUR » (depuis « 10548.72 »). */
export function formaterMontantSimulation(montant: string): string {
  return `${formaterCentimes(centimesDepuisChaine(montant))} EUR`;
}

/** Montant de la liste des emprunts : « EUR5,000.00 » (depuis « 5000.00 »). */
export function formaterMontantListe(montant: string): string {
  return `EUR${formaterCentimes(centimesDepuisChaine(montant))}`;
}

/** Montant d'une ligne d'échéancier de la simulation, sans devise : « 1,263.85 ». */
export function formaterMontantLigne(montant: string): string {
  return formaterCentimes(centimesDepuisChaine(montant));
}

/** Taux annuel à quatre décimales suivi de « % » : « 4.9 » ou « 4.9000 » donnent « 4.9000% ». */
export function formaterTaux(taux: string): string {
  const [entiers, decimales = ''] = taux.split('.');
  return `${entiers}.${decimales.padEnd(4, '0')}%`;
}

/** Durée en mois : « 24 mois ». */
export function formaterDuree(dureeMois: number): string {
  return `${dureeMois} mois`;
}
