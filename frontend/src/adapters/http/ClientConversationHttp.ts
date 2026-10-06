import type { ClientConversation, ReponseConversation } from '../../application/dialoguerAvecSophie';

export const CHEMIN_CHAT = '/api/open-banking-conseiller/chat/messages';

const TECHNIQUE: ReponseConversation = { statut: 'technique' };
const NON_AUTHENTIFIE: ReponseConversation = { statut: 'nonAuthentifie' };

/** Corps JSON de la réponse, ou null s'il est illisible ou n'est pas un objet. */
async function corpsDe(reponse: Response): Promise<Record<string, unknown> | null> {
  try {
    const corps: unknown = await reponse.json();
    return typeof corps === 'object' && corps !== null && !Array.isArray(corps) ? (corps as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

/**
 * Adaptateur HTTP de la conversation (open-banking-conseiller) par `fetchAuthentifie` (tout 401 supprime déjà
 * le jeton). 200 avec `reponse` texte : ok ; 401 : non authentifié ; 502 et 503 avec `message` texte : service
 * indisponible, message repris tel quel ; tout le reste (réseau coupé, autre statut, corps illisible) : technique.
 */
export function creerClientConversationHttp(appeler: typeof fetch = (entree, options) => fetch(entree, options)): ClientConversation {
  return {
    async envoyer(jeton, message, historique) {
      try {
        const reponse = await appeler(CHEMIN_CHAT, {
          method: 'POST',
          headers: { Authorization: `Bearer ${jeton}`, 'Content-Type': 'application/json', Accept: 'application/json' },
          body: JSON.stringify({ message, historique }),
        });
        if (reponse.status === 401) {
          return NON_AUTHENTIFIE;
        }
        if (reponse.status !== 200 && reponse.status !== 502 && reponse.status !== 503) {
          return TECHNIQUE;
        }
        const corps = await corpsDe(reponse);
        if (reponse.status === 200) {
          return typeof corps?.reponse === 'string' ? { statut: 'ok', reponse: corps.reponse } : TECHNIQUE;
        }
        return typeof corps?.message === 'string' ? { statut: 'indisponible', message: corps.message } : TECHNIQUE;
      } catch {
        return TECHNIQUE;
      }
    },
  };
}
