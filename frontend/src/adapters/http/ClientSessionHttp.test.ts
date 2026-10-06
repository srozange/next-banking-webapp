import { describe, expect, it, vi } from 'vitest';
import { CHEMIN_DECONNEXION, CHEMIN_RENOUVELLEMENT, creerClientSessionHttp } from './ClientSessionHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

describe('ClientSessionHttp', () => {
  it('[FR-001-10] POST /deconnexion avec le jeton en Bearer', async () => {
    const appeler = repondre(204, '');

    await creerClientSessionHttp(appeler).deconnecter('jeton');

    expect(CHEMIN_DECONNEXION).toBe('/api/identite-clients/deconnexion');
    expect(appeler).toHaveBeenCalledExactlyOnceWith(CHEMIN_DECONNEXION, {
      method: 'POST',
      headers: { Authorization: 'Bearer jeton' },
    });
  });

  it('[FR-001-10] une panne réseau à la déconnexion est ignorée', async () => {
    const appeler = vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')));
    await expect(creerClientSessionHttp(appeler).deconnecter('j')).resolves.toBeUndefined();
  });

  it('[FR-030-05] POST /renouvellement avec le jeton et rend le jeton neuf', async () => {
    const appeler = repondre(200, '{"jeton":"neuf"}');

    await expect(creerClientSessionHttp(appeler).renouveler('ancien')).resolves.toEqual({
      statut: 'renouvelee',
      jeton: 'neuf',
    });
    expect(CHEMIN_RENOUVELLEMENT).toBe('/api/identite-clients/renouvellement');
    expect(appeler).toHaveBeenCalledWith(CHEMIN_RENOUVELLEMENT, {
      method: 'POST',
      headers: { Authorization: 'Bearer ancien', Accept: 'application/json' },
    });
  });

  it('[FR-030-05] 401 : non authentifié', async () => {
    await expect(
      creerClientSessionHttp(repondre(401, '{"message":"Authentification requise"}')).renouveler('j'),
    ).resolves.toEqual({ statut: 'nonAuthentifie' });
  });

  it.each([
    ['500', repondre(500, '{}')],
    ['200 au JSON illisible', repondre(200, 'pas du json')],
    ['200 sans jeton', repondre(200, '{}')],
    ['200 au jeton non textuel', repondre(200, '{"jeton":3}')],
    ['200 au corps null', repondre(200, 'null')],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('[FR-030-05] erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientSessionHttp(appeler).renouveler('j')).resolves.toEqual({ statut: 'technique' });
  });

  it('appelle fetch global par défaut', async () => {
    const global = repondre(204, '');
    vi.stubGlobal('fetch', global);
    try {
      await creerClientSessionHttp().deconnecter('j');
      expect(global).toHaveBeenCalledOnce();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
