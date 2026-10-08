import type { PretAdministration } from '../domain/pret';

/** Décision de l'administrateur sur un prêt EN_ATTENTE. */
export type ActionPret = 'approuver' | 'refuser';

/** Réponse du service à une lecture (pipeline ou historique), traduite par l'adaptateur HTTP. */
export type ReponseListePrets = { statut: 'liste'; prets: readonly PretAdministration[] } | { statut: 'technique' };

/** Réponse du service à une décision : toute réponse qui n'est pas un succès est technique (le texte affiché est fixe). */
export type ReponseDecisionPret = { statut: 'effectuee' } | { statut: 'technique' };

/** Port sortant : prêts de l'administration (rôle ADMIN vérifié par le service `credits`, jamais par l'écran). */
export interface ClientPipelinePrets {
  lireEnAttente(): Promise<ReponseListePrets>;
  lireHistorique(): Promise<ReponseListePrets>;
  decider(action: ActionPret, pretId: number): Promise<ReponseDecisionPret>;
}

export interface ContenuPipelinePrets {
  enAttente: readonly PretAdministration[] | null;
  historique: readonly PretAdministration[] | null;
}

async function lire(appel: () => Promise<ReponseListePrets>): Promise<readonly PretAdministration[] | null> {
  try {
    const reponse = await appel();
    return reponse.statut === 'liste' ? reponse.prets : null;
  } catch {
    return null;
  }
}

/** Cas d'usage : pipeline des prêts EN_ATTENTE et historique de tous les prêts ; null pour une partie indisponible. */
export async function chargerPipelinePrets(client: ClientPipelinePrets): Promise<ContenuPipelinePrets> {
  const [enAttente, historique] = await Promise.all([lire(() => client.lireEnAttente()), lire(() => client.lireHistorique())]);
  return { enAttente, historique };
}

/**
 * Cas d'usage : approuver ou refuser un prêt, sans confirmation. Le serveur décide de tout (rôle, état du prêt) ; tout
 * échec, quelle qu'en soit la cause, est le même résultat « erreur ».
 */
export async function deciderPret(action: ActionPret, pretId: number, client: ClientPipelinePrets): Promise<'effectuee' | 'erreur'> {
  try {
    const reponse = await client.decider(action, pretId);
    return reponse.statut === 'effectuee' ? 'effectuee' : 'erreur';
  } catch {
    return 'erreur';
  }
}
