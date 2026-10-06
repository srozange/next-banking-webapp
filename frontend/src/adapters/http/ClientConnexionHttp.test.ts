import { describe, expect, it, vi } from 'vitest';
import { decodeurJetonJwt } from '../jeton/DecodeurJetonJwt';
import { jetonDeTest } from '../jeton/jetonDeTest';
import { CHEMIN_CONNEXION, creerClientConnexionHttp } from './ClientConnexionHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

describe('ClientConnexionHttp', () => {
  it('envoie identifiant et mot de passe en JSON par POST', async () => {
    const appeler = repondre(401, '{"message":"Identifiants incorrects"}');

    await creerClientConnexionHttp(decodeurJetonJwt, appeler).connecter('user1', 'secret');

    expect(appeler).toHaveBeenCalledWith(CHEMIN_CONNEXION, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: '{"identifiant":"user1","motDePasse":"secret"}',
    });
    expect(CHEMIN_CONNEXION).toBe('/api/identite-clients/connexion');
  });

  it('[FR-001-04] rend le jeton et les rôles qu’il porte sur 200', async () => {
    const jeton = jetonDeTest({ roles: ['CONSEILLER'] });
    const client = creerClientConnexionHttp(decodeurJetonJwt, repondre(200, JSON.stringify({ jeton })));

    await expect(client.connecter('sophie', 'x')).resolves.toEqual({ statut: 'acceptee', jeton, roles: ['CONSEILLER'] });
  });

  it('[FR-001-06] rend un refus sur 401', async () => {
    const client = creerClientConnexionHttp(decodeurJetonJwt, repondre(401, '{"message":"Identifiants incorrects"}'));

    await expect(client.connecter('user1', 'faux')).resolves.toEqual({ statut: 'refusee' });
  });

  it.each([
    ['500', repondre(500, '{"message":"Une erreur technique est survenue"}')],
    ['400 inattendu', repondre(400, '{"message":"Identifiant requis"}')],
    ['200 au JSON illisible', repondre(200, 'pas du json')],
    ['200 sans jeton', repondre(200, '{}')],
    ['200 au corps null', repondre(200, 'null')],
    ['200 au jeton illisible', repondre(200, '{"jeton":"illisible"}')],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('rend une erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientConnexionHttp(decodeurJetonJwt, appeler).connecter('u', 'p')).resolves.toEqual({
      statut: 'technique',
    });
  });

  it('appelle fetch global par défaut', async () => {
    const appeler = repondre(401, '{}');
    vi.stubGlobal('fetch', appeler);
    try {
      await expect(creerClientConnexionHttp(decodeurJetonJwt).connecter('u', 'p')).resolves.toEqual({
        statut: 'refusee',
      });
      expect(appeler).toHaveBeenCalledOnce();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
