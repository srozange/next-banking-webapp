import { centimesDepuisChaine } from './montant';
import type { OperationBrute, TypeOperation } from './releve';

/** Règles pures du tableau de bord (FEAT-002) : solde total, format `EURx,xxx.xx`, 5 dernières opérations. */

/** Compte du client tel que rendu par le service ; `solde` est une chaîne à point et deux décimales. */
export interface CompteTableauBrut {
  id: number;
  type: string;
  numero: string;
  solde: string;
}

/** Nombre d'opérations affichées (RG-002-006). */
export const NOMBRE_OPERATIONS_AFFICHEES = 5;

/**
 * Montant au format du legacy d'un nombre de centimes entiers : `EUR` collé, milliers séparés par une virgule,
 * point décimal et deux décimales (« EUR2,130.00 ») ; aucun flottant (UX-DR157).
 */
export function formaterMontantTableau(centimes: number): string {
  const absolu = Math.abs(centimes);
  const entiers = String(Math.floor(absolu / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const reste = String(absolu % 100).padStart(2, '0');
  return `${centimes < 0 ? '-' : ''}EUR${entiers}.${reste}`;
}

/** Somme des soldes de tous les comptes, en centimes entiers ; 0 sans compte (RG-002-003, RG-002-004). */
export function soldeTotalCentimes(comptes: readonly CompteTableauBrut[]): number {
  return comptes.reduce((somme, compte) => somme + centimesDepuisChaine(compte.solde), 0);
}

/**
 * Patrimoine total en centimes entiers : total Demo Bank + patrimoine des comptes externes (chaîne `"120.50"`,
 * soldes absents déjà exclus par le service) (FR-002-12).
 */
export function patrimoineTotalCentimes(soldeDemoBankCentimes: number, patrimoineExterne: string): number {
  return soldeDemoBankCentimes + centimesDepuisChaine(patrimoineExterne);
}

/** Les 5 premières opérations, déjà de la plus récente à la plus ancienne selon le service. */
export function dernieresOperations(operations: readonly OperationBrute[]): readonly OperationBrute[] {
  return operations.slice(0, NOMBRE_OPERATIONS_AFFICHEES);
}

/** Compte prêt à afficher. */
export interface CompteTableau {
  id: number;
  type: string;
  numero: string;
  solde: string;
}

/** Ligne d'opération prête à afficher ; `date` reste l'instant ISO (formaté par l'adaptateur, en Europe/Paris). */
export interface LigneTableau {
  date: string;
  description: string;
  type: TypeOperation;
  /** Montant signé : « + » pour un CREDIT, « - » (tiret ASCII) pour un DEBIT. */
  montant: string;
  soldeApres: string;
}

export function compteTableau(compte: CompteTableauBrut): CompteTableau {
  return { id: compte.id, type: compte.type, numero: compte.numero, solde: formaterMontantTableau(centimesDepuisChaine(compte.solde)) };
}

export function ligneTableau(operation: OperationBrute): LigneTableau {
  return {
    date: operation.date,
    description: operation.description,
    type: operation.type,
    montant: `${operation.type === 'CREDIT' ? '+' : '-'}${formaterMontantTableau(centimesDepuisChaine(operation.montant))}`,
    soldeApres: formaterMontantTableau(centimesDepuisChaine(operation.soldeApres)),
  };
}
