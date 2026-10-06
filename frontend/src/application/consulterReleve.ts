import { ligneReleve, type LigneReleve, type OperationBrute } from '../domain/releve';
import { chargerComptes, type ClientVirement, type Compte } from './virement';

/** Réponse du service des opérations d'un compte, traduite par l'adaptateur HTTP. */
export type ReponseOperations = { statut: 'operations'; operations: readonly OperationBrute[] } | { statut: 'technique' };

/** Port sortant : opérations d'un compte du client (le service décide de la propriété du compte). */
export interface ClientReleve {
  lireOperations(compteId: number): Promise<ReponseOperations>;
}

/** Relevé chargé : comptes à jour, compte sélectionné (null sans compte) et ses opérations, de la plus récente à la plus ancienne. */
export interface Releve {
  comptes: readonly Compte[];
  compteId: number | null;
  lignes: readonly LigneReleve[];
}

/**
 * Cas d'usage (FR-005-02, FR-005-03) : recharge la liste des comptes (soldes à jour), puis les opérations du compte
 * demandé, ou du premier compte quand aucun n'est demandé (ou quand il a disparu). Sans compte : aucune opération,
 * aucun appel. Null si le service est indisponible ou rend une réponse inexploitable.
 */
export async function chargerReleve(compteDemande: number | null, comptes: ClientVirement, releve: ClientReleve): Promise<Releve | null> {
  const lus = await chargerComptes(comptes);
  if (lus === null) {
    return null;
  }
  const choisi = lus.find((compte) => compte.id === compteDemande) ?? lus[0];
  if (choisi === undefined) {
    return { comptes: lus, compteId: null, lignes: [] };
  }
  try {
    const reponse = await releve.lireOperations(choisi.id);
    return reponse.statut === 'operations'
      ? { comptes: lus, compteId: choisi.id, lignes: reponse.operations.map(ligneReleve) }
      : null;
  } catch {
    return null;
  }
}
