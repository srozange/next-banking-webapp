import type { EtatComptesTiers } from '../domain/comptesTiers';
import { centimesDepuisChaine } from '../domain/montant';
import type { OperationBrute } from '../domain/releve';
import type { ClientComptesTiers } from './comptesTiers';
import {
  compteTableau,
  dernieresOperations,
  formaterMontantTableau,
  ligneTableau,
  soldeTotalCentimes,
  type CompteTableau,
  type CompteTableauBrut,
  type LigneTableau,
} from '../domain/tableauDeBord';

/** Réponse de `GET /api/coeur-bancaire/tableau-de-bord`, traduite par l'adaptateur HTTP. */
export type ReponseTableauDeBord =
  | { statut: 'tableau'; comptes: readonly CompteTableauBrut[]; dernieresOperations: readonly OperationBrute[] }
  | { statut: 'technique' };

/** Port sortant : comptes et dernières opérations du client de la session. */
export interface ClientTableauDeBord {
  lire(): Promise<ReponseTableauDeBord>;
}

/** Tableau de bord prêt à afficher. */
export interface TableauDeBord {
  soldeTotal: string;
  /** Total Demo Bank en centimes entiers, base du patrimoine total (FR-002-12). */
  soldeTotalCentimes: number;
  comptes: readonly CompteTableau[];
  lignes: readonly LigneTableau[];
}

/**
 * Cas d'usage (FR-002-03 à FR-002-06) : un seul appel au cœur bancaire, lancé sans attendre l'identité (les deux
 * services sont appelés en parallèle, jamais en cascade). Solde total sommé en centimes entiers, 5 lignes au plus.
 * Null si le service est indisponible ou rend une réponse inexploitable.
 */
export async function chargerTableauDeBord(client: ClientTableauDeBord): Promise<TableauDeBord | null> {
  try {
    const reponse = await client.lire();
    if (reponse.statut !== 'tableau') {
      return null;
    }
    const total = soldeTotalCentimes(reponse.comptes);
    return {
      soldeTotal: formaterMontantTableau(total),
      soldeTotalCentimes: total,
      comptes: reponse.comptes.map(compteTableau),
      lignes: dernieresOperations(reponse.dernieresOperations).map(ligneTableau),
    };
  } catch {
    return null;
  }
}

/** Comptes externes Powens tels que le tableau de bord les présente (FR-002-12). */
export interface PatrimoineExterne {
  connecte: boolean;
  nombreComptes: number;
  /** Chaîne à deux décimales (« 120.50 »), validée. */
  patrimoineTotal: string;
}

/**
 * Cas d'usage (FR-002-12) : lit l'état Powens (lecture en base, aucun appel à Powens). Null sur tout échec
 * (indisponible, technique, session finie, réponse illisible) : le tableau de bord n'affiche alors rien de Powens.
 */
export async function chargerPatrimoineExterne(client: ClientComptesTiers): Promise<PatrimoineExterne | null> {
  try {
    const reponse = await client.lireEtat();
    if (reponse.statut !== 'ok') {
      return null;
    }
    const etat: EtatComptesTiers = reponse.donnees;
    centimesDepuisChaine(etat.patrimoineTotal);
    return { connecte: etat.connecte, nombreComptes: etat.comptes.length, patrimoineTotal: etat.patrimoineTotal };
  } catch {
    return null;
  }
}
