import { describe, expect, it, vi } from 'vitest';
import {
  renouvelerSession,
  SEUIL_RENOUVELLEMENT_SECONDES,
  type ClientRenouvellement,
  type ReponseRenouvellement,
} from './renouvelerSession';
import type { StockageJeton } from './seConnecter';

function stockage(jeton: string | null) {
  return { lire: vi.fn(() => jeton), enregistrer: vi.fn(), supprimer: vi.fn() } satisfies StockageJeton;
}

function client(reponse: ReponseRenouvellement | Error) {
  const renouveler = vi.fn(() => (reponse instanceof Error ? Promise.reject(reponse) : Promise.resolve(reponse)));
  return { renouveler } satisfies ClientRenouvellement;
}

const restant = (secondes: number | null) => ({ secondesRestantes: () => secondes });

describe('renouvelerSession', () => {
  it('[FR-030-05] le seuil est de 600 secondes', () => {
    expect(SEUIL_RENOUVELLEMENT_SECONDES).toBe(600);
  });

  it('[FR-030-05] sans jeton, aucun appel', async () => {
    const appels = client({ statut: 'technique' });
    await expect(renouvelerSession(appels, stockage(null), restant(0))).resolves.toBe('aucun');
    expect(appels.renouveler).not.toHaveBeenCalled();
  });

  it('[FR-030-05] expiration illisible, aucun appel', async () => {
    const appels = client({ statut: 'technique' });
    await expect(renouvelerSession(appels, stockage('j'), restant(null))).resolves.toBe('aucun');
    expect(appels.renouveler).not.toHaveBeenCalled();
  });

  it('[FR-030-05] 600 secondes restantes ou plus : aucun appel', async () => {
    const appels = client({ statut: 'technique' });
    await expect(renouvelerSession(appels, stockage('j'), restant(600))).resolves.toBe('inutile');
    expect(appels.renouveler).not.toHaveBeenCalled();
  });

  it('[FR-030-05] 599 secondes restantes : appel, le jeton neuf remplace l’ancien', async () => {
    const jetons = stockage('ancien');
    const appels = client({ statut: 'renouvelee', jeton: 'neuf' });

    await expect(renouvelerSession(appels, jetons, restant(599))).resolves.toBe('renouvelee');

    expect(appels.renouveler).toHaveBeenCalledExactlyOnceWith('ancien');
    expect(jetons.enregistrer).toHaveBeenCalledExactlyOnceWith('neuf');
  });

  it('[FR-030-05] jeton déjà expiré : le renouvellement est tenté', async () => {
    const appels = client({ statut: 'nonAuthentifie' });
    await expect(renouvelerSession(appels, stockage('j'), restant(-5))).resolves.toBe('deconnecte');
    expect(appels.renouveler).toHaveBeenCalledOnce();
  });

  it('[FR-030-05] 401 (client désactivé) : jeton supprimé, session terminée', async () => {
    const jetons = stockage('j');
    await expect(renouvelerSession(client({ statut: 'nonAuthentifie' }), jetons, restant(10))).resolves.toBe('deconnecte');
    expect(jetons.supprimer).toHaveBeenCalledOnce();
    expect(jetons.enregistrer).not.toHaveBeenCalled();
  });

  it.each<[string, ReponseRenouvellement | Error]>([
    ['réponse technique', { statut: 'technique' }],
    ['exception', new Error('réseau')],
  ])('[FR-030-05] %s : le jeton est conservé', async (_cas, reponse) => {
    const jetons = stockage('j');
    await expect(renouvelerSession(client(reponse), jetons, restant(10))).resolves.toBe('echec');
    expect(jetons.supprimer).not.toHaveBeenCalled();
    expect(jetons.enregistrer).not.toHaveBeenCalled();
  });
});
