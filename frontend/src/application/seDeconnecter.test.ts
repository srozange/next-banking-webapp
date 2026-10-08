import { describe, expect, it, vi } from 'vitest';
import { seDeconnecter } from './seDeconnecter';
import type { SessionLocale } from './session';

function sessionEnMemoire(ouverte = true, roles: readonly string[] | null = null) {
  let enCours = ouverte;
  return {
    ouverte: vi.fn(() => enCours),
    roles: vi.fn(() => roles),
    terminer: vi.fn(() => {
      enCours = false;
    }),
    secondesRestantes: vi.fn(() => null),
  } satisfies SessionLocale;
}

describe('seDeconnecter', () => {
  it('[FR-001-10] informe le service avant de terminer la session, et rend /login', () => {
    const session = sessionEnMemoire(true);
    const ordre: string[] = [];
    const deconnecter = vi.fn(() => {
      ordre.push(session.ouverte() ? 'service, session encore ouverte' : 'service, session terminée');
      return Promise.resolve();
    });

    expect(seDeconnecter({ deconnecter }, session)).toBe('/login');

    expect(session.terminer).toHaveBeenCalledOnce();
    expect(deconnecter).toHaveBeenCalledOnce();
    expect(ordre).toEqual(['service, session encore ouverte']);
  });

  it('[FR-001-10] sans session, rend /login sans appeler le service', () => {
    const deconnecter = vi.fn(() => Promise.resolve());
    const session = sessionEnMemoire(false);

    expect(seDeconnecter({ deconnecter }, session)).toBe('/login');

    expect(deconnecter).not.toHaveBeenCalled();
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-001-10] une panne du service, asynchrone ou immédiate, ne retient pas l’utilisateur', async () => {
    const session = { ...sessionEnMemoire(true), ouverte: () => true };

    expect(seDeconnecter({ deconnecter: () => Promise.reject(new Error('réseau')) }, session)).toBe('/login');
    expect(
      seDeconnecter(
        {
          deconnecter: () => {
            throw new Error('immédiat');
          },
        },
        session,
      ),
    ).toBe('/login');

    await Promise.resolve();
    expect(session.terminer).toHaveBeenCalledTimes(2);
  });
});
