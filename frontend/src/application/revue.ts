import { motifRenseigne, type VirementEnRevue, type VirementTraite } from '../domain/virement';

/** Décision de l'administrateur sur un virement suspendu. */
export type ActionRevue = 'liberer' | 'rejeter';

/** Refus d'une décision avec rejet du virement sans débit : solde devenu insuffisant, ou heure butoir atteinte. */
export type IssueRefusee = 'REFUSEE_SOLDE_INSUFFISANT' | 'REFUSEE_HEURE_BUTOIR';

/** Réponse du service à une décision, traduite par l'adaptateur HTTP. */
export type ReponseDecision =
  | { statut: 'enregistre' }
  | { statut: 'refuse'; issue: IssueRefusee }
  | { statut: 'motif' }
  | { statut: 'erreur'; message: string | null };

export type ReponseFile = { statut: 'liste'; virements: readonly VirementEnRevue[] } | { statut: 'technique' };
export type ReponseHistorique = { statut: 'liste'; virements: readonly VirementTraite[] } | { statut: 'technique' };

/** Port sortant : revue des virements suspendus (rôle ADMIN vérifié par le service, jamais par l'écran). */
export interface ClientRevue {
  lireFile(): Promise<ReponseFile>;
  lireHistorique(): Promise<ReponseHistorique>;
  decider(action: ActionRevue, virementId: number, motif: string): Promise<ReponseDecision>;
}

export interface ContenuRevue {
  file: readonly VirementEnRevue[] | null;
  historique: readonly VirementTraite[] | null;
}

export type ResultatDecision =
  | { type: 'motif' }
  | { type: 'enregistre'; action: ActionRevue }
  | { type: 'refuse'; issue: IssueRefusee }
  | { type: 'erreur'; message: string | null };

async function lire<T>(appel: () => Promise<{ statut: 'liste'; virements: readonly T[] } | { statut: 'technique' }>): Promise<readonly T[] | null> {
  try {
    const reponse = await appel();
    return reponse.statut === 'liste' ? reponse.virements : null;
  } catch {
    return null;
  }
}

/** Cas d'usage : file de revue et historique des virements traités ; null pour une partie indisponible. */
export async function chargerRevue(client: ClientRevue): Promise<ContenuRevue> {
  const [file, historique] = await Promise.all([lire(() => client.lireFile()), lire(() => client.lireHistorique())]);
  return { file, historique };
}

/**
 * Cas d'usage : libérer ou rejeter un virement. Un motif vide ou d'espaces est refusé sans appel ; le serveur décide
 * de tout le reste (rôle, état du virement, heure butoir, solde).
 */
export async function deciderVirement(
  action: ActionRevue,
  virementId: number,
  motif: string,
  client: ClientRevue,
): Promise<ResultatDecision> {
  if (!motifRenseigne(motif)) {
    return { type: 'motif' };
  }
  try {
    const reponse = await client.decider(action, virementId, motif.trim());
    switch (reponse.statut) {
      case 'enregistre':
        return { type: 'enregistre', action };
      case 'refuse':
        return { type: 'refuse', issue: reponse.issue };
      case 'motif':
        return { type: 'motif' };
      case 'erreur':
        return { type: 'erreur', message: reponse.message };
    }
  } catch {
    return { type: 'erreur', message: null };
  }
}
