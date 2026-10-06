import { describe, expect, it, vi } from 'vitest';
import type { Echange } from '../domain/conversation';
import { dialoguer, type ClientConversation, type ReponseConversation } from './dialoguerAvecSophie';
import type { StockageJeton } from './seConnecter';

function stockageEnMemoire(initial: string | null = 'jeton') {
  let jeton = initial;
  return {
    lire: vi.fn(() => jeton),
    enregistrer: vi.fn((valeur: string) => {
      jeton = valeur;
    }),
    supprimer: vi.fn(() => {
      jeton = null;
    }),
  } satisfies StockageJeton;
}

const REPLI = 'Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence.';
const HISTORIQUE: Echange[] = [
  { role: 'utilisateur', contenu: 'Bonjour' },
  { role: 'assistant', contenu: 'Bonjour Jean' },
];

function client(reponse: ReponseConversation) {
  return { envoyer: vi.fn<ClientConversation['envoyer']>(() => Promise.resolve(reponse)) };
}

describe('dialoguer', () => {
  it('[FR-010-05] envoie le message avec le jeton et l’historique, et rend la réponse de Sophie', async () => {
    const service = client({ statut: 'ok', reponse: 'Avec plaisir' });

    await expect(dialoguer(service, stockageEnMemoire(), HISTORIQUE, 'Merci', REPLI)).resolves.toEqual({
      type: 'reponse',
      texte: 'Avec plaisir',
    });
    expect(service.envoyer).toHaveBeenCalledWith('jeton', 'Merci', HISTORIQUE);
  });

  it('[FR-010-06] envoie un historique vide au premier message', async () => {
    const service = client({ statut: 'ok', reponse: 'Bonjour' });

    await dialoguer(service, stockageEnMemoire(), [], 'Bonjour', REPLI);

    expect(service.envoyer).toHaveBeenCalledWith('jeton', 'Bonjour', []);
  });

  it('[FR-010-09] rend le message du service indisponible tel quel', async () => {
    const message = 'Je suis momentanement indisponible (HTTP 500). Veuillez reessayer.';

    await expect(
      dialoguer(client({ statut: 'indisponible', message }), stockageEnMemoire(), [], 'x', REPLI),
    ).resolves.toEqual({ type: 'reponse', texte: message });
  });

  it('[BUG-038] une panne technique ou une exception rend le texte de repli, sans code technique', async () => {
    await expect(dialoguer(client({ statut: 'technique' }), stockageEnMemoire(), [], 'x', REPLI)).resolves.toEqual({
      type: 'reponse',
      texte: REPLI,
    });
    const qui = { envoyer: vi.fn<ClientConversation['envoyer']>(() => Promise.reject(new Error('boom'))) };
    await expect(dialoguer(qui, stockageEnMemoire(), [], 'x', REPLI)).resolves.toEqual({ type: 'reponse', texte: REPLI });
  });

  it('[FR-010-05] sur 401, supprime le jeton et termine la session', async () => {
    const stockage = stockageEnMemoire();

    await expect(dialoguer(client({ statut: 'nonAuthentifie' }), stockage, [], 'x', REPLI)).resolves.toEqual({
      type: 'deconnecte',
    });
    expect(stockage.supprimer).toHaveBeenCalled();
  });

  it('[FR-010-05] sans jeton, termine la session sans appel', async () => {
    const service = client({ statut: 'ok', reponse: 'x' });

    await expect(dialoguer(service, stockageEnMemoire(null), [], 'x', REPLI)).resolves.toEqual({ type: 'deconnecte' });
    expect(service.envoyer).not.toHaveBeenCalled();
  });
});
