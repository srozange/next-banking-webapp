import { centimesDepuisChaine } from './montant';
import { formaterCentimes, formaterMontantListe } from './pret';

/** Compte externe agrégé par Powens ; `solde` est null quand Powens n'en donne aucun (distinct de zéro, BUG-042). */
export interface CompteExterne {
  id: number;
  nom: string;
  type: string | null;
  solde: string | null;
  soldeAbsent: boolean;
  iban: string | null;
}

/** Transaction du compte sélectionné ; `montant` est une chaîne à deux décimales ("-12.50"). */
export interface TransactionExterne {
  date: string;
  libelle: string;
  montant: string;
}

/** État Powens du client tel que le rend le service ; `message` vide : aucun message. */
export interface EtatComptesTiers {
  connecte: boolean;
  comptes: readonly CompteExterne[];
  compteSelectionne: number | null;
  transactions: readonly TransactionExterne[];
  message: string;
  patrimoineTotal: string;
}

const LIBELLES_TYPE: Readonly<Record<string, string>> = {
  checking: 'Compte courant',
  savings: 'Epargne',
  card: 'Carte',
  loan: 'Credit',
  market: 'Bourse',
};

const LIBELLE_TYPE_ABSENT = 'Compte';

/** Texte affiché à la place d'un solde absent (BUG-042). */
export const SOLDE_INDISPONIBLE = 'Solde indisponible';

/** Libellé d'un type Powens (FR-012-11) : « Compte » si absent, la valeur brute si inconnu. */
export function libelleTypeCompte(type: string | null): string {
  if (type === null) {
    return LIBELLE_TYPE_ABSENT;
  }
  return Object.hasOwn(LIBELLES_TYPE, type) ? LIBELLES_TYPE[type] : type;
}

/** Solde d'une carte « EUR1,234.50 », ou « Solde indisponible » s'il est absent, jamais « 0.00 » (FR-012-12). */
export function formaterSolde(compte: Pick<CompteExterne, 'solde' | 'soldeAbsent'>): string {
  return compte.soldeAbsent || compte.solde === null ? SOLDE_INDISPONIBLE : formaterMontantListe(compte.solde);
}

/** Une transaction est un crédit (vert, préfixe « + ») si son montant est strictement positif (FR-012-13). */
export function estCredit(montant: string): boolean {
  return centimesDepuisChaine(montant) > 0;
}

/** Montant d'une transaction « +1,234.50 » (crédit) ou « -12.00 » / « 0.00 » (sinon). */
export function formaterMontantTransaction(montant: string): string {
  const centimes = centimesDepuisChaine(montant);
  return `${centimes > 0 ? '+' : ''}${formaterCentimes(centimes)}`;
}
