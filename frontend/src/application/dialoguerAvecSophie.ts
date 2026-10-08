import type { Echange } from '../domain/conversation';
import { sessionOuverteOuFin, type SessionLocale } from './session';

/** Réponse du service de conversation, traduite par l'adaptateur HTTP (jamais d'exception). */
export type ReponseConversation =
  | { statut: 'ok'; reponse: string }
  | { statut: 'indisponible'; message: string }
  | { statut: 'nonAuthentifie' }
  | { statut: 'technique' };

/** Port sortant : envoie le message et l'historique à Sophie (service open-banking-conseiller). */
export interface ClientConversation {
  envoyer(message: string, historique: readonly Echange[]): Promise<ReponseConversation>;
}

export type ResultatDialogue = { type: 'reponse'; texte: string } | { type: 'deconnecte' };

const DECONNECTE: ResultatDialogue = { type: 'deconnecte' };

/**
 * Cas d'usage (FR-010-05, FR-010-06, FR-010-09, BUG-038) : envoie le message avec l'historique des échanges déjà
 * affichés. Une panne est une réponse de Sophie : le message du service tel quel, sinon `repli` ; aucun code
 * technique n'est jamais rendu. Sans session ou sur 401, la session est terminée.
 */
export async function dialoguer(
  client: ClientConversation,
  session: SessionLocale,
  historique: readonly Echange[],
  message: string,
  repli: string,
): Promise<ResultatDialogue> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  let reponse: ReponseConversation;
  try {
    reponse = await client.envoyer(message, historique);
  } catch {
    return { type: 'reponse', texte: repli };
  }
  switch (reponse.statut) {
    case 'ok':
      return { type: 'reponse', texte: reponse.reponse };
    case 'indisponible':
      return { type: 'reponse', texte: reponse.message };
    case 'nonAuthentifie':
      session.terminer();
      return DECONNECTE;
    case 'technique':
      return { type: 'reponse', texte: repli };
  }
}
