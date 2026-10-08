import { describe, expect, it, vi } from 'vitest';
import { decodeurJetonJwt } from '../jeton/DecodeurJetonJwt';
import { jetonDeTest } from '../jeton/jetonDeTest';
import type { StockageJeton } from '../stockage/StockageJetonLocal';
import { CHEMIN_CONNEXION, creerClientConnexionHttp } from './ClientConnexionHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

function stockageEnMemoire() {
  return { lire: vi.fn(() => null), enregistrer: vi.fn(), supprimer: vi.fn() } satisfies StockageJeton;
}

describe('ClientConnexionHttp', () => {
  it('envoie identifiant et mot de passe en JSON par POST', async () => {
    const appeler = repondre(401, '{"message":"Identifiants incorrects"}');

    await creerClientConnexionHttp(stockageEnMemoire(), decodeurJetonJwt, appeler).connecter('user1', 'secret');

    expect(appeler).toHaveBeenCalledWith(CHEMIN_CONNEXION, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: '{"identifiant":"user1","motDePasse":"secret"}',
    });
    expect(CHEMIN_CONNEXION).toBe('/api/identite-clients/connexion');
  });

  it('[FR-001-04] enregistre le jeton et rend les rôles qu’il porte sur 200', async () => {
    const jeton = jetonDeTest({ roles: ['CONSEILLER'] });
    const stockage = stockageEnMemoire();
    const client = creerClientConnexionHttp(stockage, decodeurJetonJwt, repondre(200, JSON.stringify({ jeton })));

    await expect(client.connecter('sophie', 'x')).resolves.toEqual({ statut: 'acceptee', roles: ['CONSEILLER'] });
    expect(stockage.enregistrer).toHaveBeenCalledExactlyOnceWith(jeton);
  });

  it('rend une erreur technique, sans rien enregistrer, pour un rôle inconnu', async () => {
    const stockage = stockageEnMemoire();
    const jeton = jetonDeTest({ roles: ['INVITE'] });
    const client = creerClientConnexionHttp(stockage, decodeurJetonJwt, repondre(200, JSON.stringify({ jeton })));

    await expect(client.connecter('u', 'p')).resolves.toEqual({ statut: 'technique' });
    expect(stockage.enregistrer).not.toHaveBeenCalled();
  });

  it('[FR-001-06] rend un refus sur 401', async () => {
    const client = creerClientConnexionHttp(stockageEnMemoire(), decodeurJetonJwt, repondre(401, '{"message":"Identifiants incorrects"}'));

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
    await expect(creerClientConnexionHttp(stockageEnMemoire(), decodeurJetonJwt, appeler).connecter('u', 'p')).resolves.toEqual({
      statut: 'technique',
    });
  });

  it('appelle fetch global par défaut', async () => {
    const appeler = repondre(401, '{}');
    vi.stubGlobal('fetch', appeler);
    try {
      await expect(creerClientConnexionHttp(stockageEnMemoire(), decodeurJetonJwt).connecter('u', 'p')).resolves.toEqual({
        statut: 'refusee',
      });
      expect(appeler).toHaveBeenCalledOnce();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
