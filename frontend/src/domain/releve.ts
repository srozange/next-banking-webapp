import { centimesDepuisChaine } from './montant';

/** Règles pures du relevé de compte (FEAT-005) : mentions d'attente et de transaction importante, signes et montants en euros, en centimes entiers. */

export type TypeOperation = 'CREDIT' | 'DEBIT';

/** Opération telle que rendue par le service : date ISO-8601, montants en chaîne à point et deux décimales. */
export interface OperationBrute {
  date: string;
  description: string;
  type: TypeOperation;
  montant: string;
  soldeApres: string;
}

/** Ligne du relevé prête à afficher ; `date` reste l'instant ISO (formaté par l'adaptateur, en Europe/Paris). */
export interface LigneReleve {
  date: string;
  libelle: string;
  enAttente: boolean;
  /** Virement important exécuté et débité (FR-003-29) : badge « Transaction importante ». */
  transactionImportante: boolean;
  type: TypeOperation;
  /** Montant signé : « + » pour un CREDIT, « - » (tiret ASCII) pour un DEBIT. */
  montant: string;
  /** Solde après l'opération, sans signe. */
  soldeApres: string;
}

/** Mention posée par le virement (FR-003-09) : « [EN ATTENTE] » suivi d'une espace, soit 13 caractères. */
export const PREFIXE_EN_ATTENTE = '[EN ATTENTE] ';

/**
 * Mention d'un virement important exécuté et débité (FR-003-29, BUG-009 corrigé) : « [TRANSACTION IMPORTANTE] »
 * suivi d'une espace, traitée comme « [EN ATTENTE] » (FR-005-07). Les opérations déjà en base gardent l'ancienne.
 */
export const PREFIXE_TRANSACTION_IMPORTANTE = '[TRANSACTION IMPORTANTE] ';

const SEPARATEUR_MILLIERS = ' ';
const ESPACE_EURO = ' ';

/** Euros à la française (« 2 130,00 € ») d'un nombre de centimes entiers ; aucun flottant. */
export function formaterEuros(centimes: number): string {
  const absolu = Math.abs(centimes);
  const entiers = String(Math.floor(absolu / 100)).replace(/\B(?=(\d{3})+(?!\d))/g, SEPARATEUR_MILLIERS);
  const reste = String(absolu % 100).padStart(2, '0');
  return `${centimes < 0 ? '-' : ''}${entiers},${reste}${ESPACE_EURO}€`;
}

/** Euros à la française d'un montant JSON (« "2130.00" » donne « 2 130,00 € »). */
export function eurosDepuisChaine(montant: string): string {
  return formaterEuros(centimesDepuisChaine(montant));
}

/** Ligne affichée d'une opération (FR-005-06, FR-005-07, FR-003-29). */
export function ligneReleve(operation: OperationBrute): LigneReleve {
  const enAttente = operation.description.startsWith(PREFIXE_EN_ATTENTE);
  const transactionImportante = operation.description.startsWith(PREFIXE_TRANSACTION_IMPORTANTE);
  const prefixe = enAttente ? PREFIXE_EN_ATTENTE : transactionImportante ? PREFIXE_TRANSACTION_IMPORTANTE : '';
  return {
    date: operation.date,
    libelle: operation.description.slice(prefixe.length),
    enAttente,
    transactionImportante,
    type: operation.type,
    montant: `${operation.type === 'CREDIT' ? '+' : '-'}${eurosDepuisChaine(operation.montant)}`,
    soldeApres: eurosDepuisChaine(operation.soldeApres),
  };
}
