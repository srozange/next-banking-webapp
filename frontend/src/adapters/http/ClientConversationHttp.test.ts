import { describe, expect, it, vi } from 'vitest';
import { CHEMIN_CHAT, creerClientConversationHttp } from './ClientConversationHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

const HISTORIQUE = [{ role: 'utilisateur', contenu: 'Bonjour' }, { role: 'assistant', contenu: 'Salut' }] as const;

describe('ClientConversationHttp', () => {
  it('[FR-010-05] poste le message et l’historique en JSON avec le jeton en Bearer', async () => {
    const appeler = repondre(200, '{"reponse":"Bonjour Jean"}');

    const reponse = await creerClientConversationHttp(appeler).envoyer('jeton', 'Merci', HISTORIQUE);

    expect(reponse).toEqual({ statut: 'ok', reponse: 'Bonjour Jean' });
    expect(CHEMIN_CHAT).toBe('/api/open-banking-conseiller/chat/messages');
    expect(appeler).toHaveBeenCalledWith(CHEMIN_CHAT, {
      method: 'POST',
      headers: { Authorization: 'Bearer jeton', 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ message: 'Merci', historique: HISTORIQUE }),
    });
  });

  it('[FR-010-05] 401 : non authentifié', async () => {
    await expect(creerClientConversationHttp(repondre(401, '{}')).envoyer('j', 'm', [])).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it('[FR-010-09] 503 et 502 avec message : indisponible, message tel quel', async () => {
    const message = 'Je suis momentanement indisponible (HTTP 500). Veuillez reessayer.';
    for (const statut of [502, 503]) {
      await expect(
        creerClientConversationHttp(repondre(statut, JSON.stringify({ message }))).envoyer('j', 'm', []),
      ).resolves.toEqual({ statut: 'indisponible', message });
    }
  });

  it('[BUG-038] tout autre cas est technique', async () => {
    const cas: [number, string][] = [
      [200, '{}'], [200, '{"reponse":3}'], [200, 'pas du json'], [200, '[]'], [200, 'null'],
      [503, '{}'], [502, 'illisible'], [400, '{"message":"Requête invalide"}'], [500, '{"message":"x"}'],
    ];
    for (const [statut, corps] of cas) {
      await expect(creerClientConversationHttp(repondre(statut, corps)).envoyer('j', 'm', [])).resolves.toEqual({
        statut: 'technique',
      });
    }
    const coupe = vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')));
    await expect(creerClientConversationHttp(coupe).envoyer('j', 'm', [])).resolves.toEqual({ statut: 'technique' });
  });
});
