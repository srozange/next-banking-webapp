import { describe, expect, it, vi } from 'vitest';
import {
  renouvelerSession,
  SEUIL_RENOUVELLEMENT_SECONDES,
  type ClientRenouvellement,
  type ReponseRenouvellement,
} from './renouvelerSession';
import type { SessionLocale } from './session';

/** Session simulée : `secondes` est le temps restant lu par la session (null : aucune session lisible). */
function session(secondes: number | null) {
  return {
    ouverte: vi.fn(() => secondes !== null),
    roles: vi.fn(() => null),
    terminer: vi.fn(),
    secondesRestantes: vi.fn(() => secondes),
  } satisfies SessionLocale;
}

function client(reponse: ReponseRenouvellement | Error) {
  const renouveler = vi.fn(() => (reponse instanceof Error ? Promise.reject(reponse) : Promise.resolve(reponse)));
  return { renouveler } satisfies ClientRenouvellement;
}

describe('renouvelerSession', () => {
  it('[FR-030-05] le seuil est de 600 secondes', () => {
    expect(SEUIL_RENOUVELLEMENT_SECONDES).toBe(600);
  });

  it('[FR-030-05] sans session lisible, aucun appel', async () => {
    const appels = client({ statut: 'technique' });
    await expect(renouvelerSession(appels, session(null))).resolves.toBe('aucun');
    expect(appels.renouveler).not.toHaveBeenCalled();
  });

  it('[FR-030-05] 600 secondes restantes ou plus : aucun appel', async () => {
    const appels = client({ statut: 'technique' });
    await expect(renouvelerSession(appels, session(600))).resolves.toBe('inutile');
    expect(appels.renouveler).not.toHaveBeenCalled();
  });

  it('[FR-030-05] 599 secondes restantes : appel, la session neuve remplace l’ancienne', async () => {
    const courante = session(599);
    const appels = client({ statut: 'renouvelee' });

    await expect(renouvelerSession(appels, courante)).resolves.toBe('renouvelee');

    expect(appels.renouveler).toHaveBeenCalledExactlyOnceWith();
    expect(courante.terminer).not.toHaveBeenCalled();
  });

  it('[FR-030-05] session déjà expirée : le renouvellement est tenté', async () => {
    const appels = client({ statut: 'nonAuthentifie' });
    await expect(renouvelerSession(appels, session(-5))).resolves.toBe('deconnecte');
    expect(appels.renouveler).toHaveBeenCalledOnce();
  });

  it('[FR-030-05] 401 (client désactivé) : session terminée', async () => {
    const courante = session(10);
    await expect(renouvelerSession(client({ statut: 'nonAuthentifie' }), courante)).resolves.toBe('deconnecte');
    expect(courante.terminer).toHaveBeenCalledOnce();
  });

  it.each<[string, ReponseRenouvellement | Error]>([
    ['réponse technique', { statut: 'technique' }],
    ['exception', new Error('réseau')],
  ])('[FR-030-05] %s : la session est conservée', async (_cas, reponse) => {
    const courante = session(10);
    await expect(renouvelerSession(client(reponse), courante)).resolves.toBe('echec');
    expect(courante.terminer).not.toHaveBeenCalled();
  });
});
