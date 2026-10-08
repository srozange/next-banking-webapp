import { describe, expect, it, vi } from 'vitest';
import type { StockageJeton } from '../stockage/StockageJetonLocal';
import { CHEMIN_DECONNEXION, CHEMIN_RENOUVELLEMENT, creerClientSessionHttp } from './ClientSessionHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

function stockageEnMemoire() {
  return { lire: vi.fn(() => 'ancien'), enregistrer: vi.fn(), supprimer: vi.fn() } satisfies StockageJeton;
}

describe('ClientSessionHttp', () => {
  it('[FR-001-10] POST /deconnexion, sans porter le jeton lui-même', async () => {
    const appeler = repondre(204, '');

    await creerClientSessionHttp(stockageEnMemoire(), appeler).deconnecter();

    expect(CHEMIN_DECONNEXION).toBe('/api/identite-clients/deconnexion');
    expect(appeler).toHaveBeenCalledExactlyOnceWith(CHEMIN_DECONNEXION, { method: 'POST' });
  });

  it('[FR-001-10] une panne réseau à la déconnexion est ignorée', async () => {
    const appeler = vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')));
    await expect(creerClientSessionHttp(stockageEnMemoire(), appeler).deconnecter()).resolves.toBeUndefined();
  });

  it('[FR-030-05] POST /renouvellement, enregistre le jeton neuf et rend « renouvelée »', async () => {
    const appeler = repondre(200, '{"jeton":"neuf"}');
    const stockage = stockageEnMemoire();

    await expect(creerClientSessionHttp(stockage, appeler).renouveler()).resolves.toEqual({ statut: 'renouvelee' });
    expect(stockage.enregistrer).toHaveBeenCalledExactlyOnceWith('neuf');
    expect(CHEMIN_RENOUVELLEMENT).toBe('/api/identite-clients/renouvellement');
    expect(appeler).toHaveBeenCalledWith(CHEMIN_RENOUVELLEMENT, {
      method: 'POST',
      headers: { Accept: 'application/json' },
    });
  });

  it('[FR-030-05] 401 : non authentifié', async () => {
    await expect(
      creerClientSessionHttp(stockageEnMemoire(), repondre(401, '{"message":"Authentification requise"}')).renouveler(),
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
    const stockage = stockageEnMemoire();
    await expect(creerClientSessionHttp(stockage, appeler).renouveler()).resolves.toEqual({ statut: 'technique' });
    expect(stockage.enregistrer).not.toHaveBeenCalled();
  });

  it('appelle fetch global par défaut', async () => {
    const global = repondre(204, '');
    vi.stubGlobal('fetch', global);
    try {
      await creerClientSessionHttp(stockageEnMemoire()).deconnecter();
      expect(global).toHaveBeenCalledOnce();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
