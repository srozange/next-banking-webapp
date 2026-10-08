import { describe, expect, it, vi } from 'vitest';
import type { StockageJeton } from '../stockage/StockageJetonLocal';
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
    expect(appeler).toHaveBeenCalledWith('/api/credits/prets', expect.objectContaining({ method: 'GET' }));
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

  it('[FR-030-05] ajoute le jeton stocké quand l’appel n’en porte pas', async () => {
    const { appeler, fetchAuthentifie } = monter(200);

    await fetchAuthentifie('/api/coeur-bancaire/tableau-de-bord', { headers: { Accept: 'application/json' } });

    const entetes = new Headers(appeler.mock.calls[0]?.[1]?.headers);
    expect(entetes.get('Authorization')).toBe('Bearer j');
    expect(entetes.get('Accept')).toBe('application/json');
  });

  it('[FR-030-05] garde l’en-tête Authorization déjà posé par l’appelant', async () => {
    const { appeler, fetchAuthentifie } = monter(200);

    await fetchAuthentifie('/x', { headers: { Authorization: 'Bearer autre' } });

    expect(new Headers(appeler.mock.calls[0]?.[1]?.headers).get('Authorization')).toBe('Bearer autre');
  });

  it.each([
    ['un objet Headers', new Headers({ Accept: 'application/json' })],
    ['un tableau', [['Accept', 'application/json']] as [string, string][]],
  ])('[FR-030-05] ajoute le jeton à des en-têtes fournis sous forme d’%s', async (_forme, entetes) => {
    const { appeler, fetchAuthentifie } = monter(200);

    await fetchAuthentifie('/x', { headers: entetes });

    const recus = new Headers(appeler.mock.calls[0]?.[1]?.headers);
    expect(recus.get('Authorization')).toBe('Bearer j');
    expect(recus.get('Accept')).toBe('application/json');
  });

  it('[FR-030-05] sans jeton stocké, n’ajoute aucun en-tête Authorization', async () => {
    const appeler = vi.fn<typeof fetch>(() => Promise.resolve(new Response('{}', { status: 200 })));
    const stockage: StockageJeton = { lire: () => null, enregistrer: vi.fn(), supprimer: vi.fn() };

    await creerFetchAuthentifie(stockage, creerSignalExpiration(), appeler)('/x');

    expect(new Headers(appeler.mock.calls[0]?.[1]?.headers).has('Authorization')).toBe(false);
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
