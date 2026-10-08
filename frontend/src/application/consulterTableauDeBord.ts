import type { OperationBrute } from '../domain/releve';
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
    return {
      soldeTotal: formaterMontantTableau(soldeTotalCentimes(reponse.comptes)),
      comptes: reponse.comptes.map(compteTableau),
      lignes: dernieresOperations(reponse.dernieresOperations).map(ligneTableau),
    };
  } catch {
    return null;
  }
}
