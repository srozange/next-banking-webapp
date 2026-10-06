import { describe, expect, it, vi } from 'vitest';
import type { StockageJeton } from '../../application/seConnecter';
import { creerFetchAuthentifie } from './fetchAuthentifie';
import { creerSignalExpiration } from './signalExpiration';

function monter(statut: number) {
  const stockage: StockageJeton = { lire: () => 'j', enregistrer: vi.fn(), supprimer: vi.fn() };
  const signal = creerSignalExpiration();
  const rappel = vi.fn();
  signal.ecouter(rappel);
  const appeler = vi.fn<typeof fetch>(() => Promise.resolve(new Response('{}', { status: statut })));
  return { stockage, rappel, appeler, fetchAuthentifie: creerFetchAuthentifie(stockage, signal, appeler) };
}

describe('fetchAuthentifie', () => {
  it('[FR-030-05] tout 401 supprime le jeton, signale la fin de session et rend la réponse', async () => {
    const { stockage, rappel, appeler, fetchAuthentifie } = monter(401);

    const reponse = await fetchAuthentifie('/api/credits/prets', { method: 'GET' });

    expect(reponse.status).toBe(401);
    expect(appeler).toHaveBeenCalledWith('/api/credits/prets', { method: 'GET' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
    expect(rappel).toHaveBeenCalledOnce();
  });

  it.each([200, 403, 404, 500])('[FR-030-05] un %s ne touche ni au jeton ni à la session', async (statut) => {
    const { stockage, rappel, fetchAuthentifie } = monter(statut);

    expect((await fetchAuthentifie('/x')).status).toBe(statut);

    expect(stockage.supprimer).not.toHaveBeenCalled();
    expect(rappel).not.toHaveBeenCalled();
  });

  it('[FR-030-05] une panne réseau est rendue à l’appelant sans toucher à la session', async () => {
    const stockage: StockageJeton = { lire: () => 'j', enregistrer: vi.fn(), supprimer: vi.fn() };
    const fetchAuthentifie = creerFetchAuthentifie(
      stockage,
      creerSignalExpiration(),
      vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch'))),
    );

    await expect(fetchAuthentifie('/x')).rejects.toThrow('Failed to fetch');
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });

  it('appelle fetch global par défaut', async () => {
    const global = vi.fn<typeof fetch>(() => Promise.resolve(new Response('{}', { status: 200 })));
    vi.stubGlobal('fetch', global);
    try {
      const stockage: StockageJeton = { lire: () => 'j', enregistrer: vi.fn(), supprimer: vi.fn() };
      await creerFetchAuthentifie(stockage, creerSignalExpiration())('/x');
      expect(global).toHaveBeenCalledOnce();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
