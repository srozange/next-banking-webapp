import { describe, expect, it, vi } from 'vitest';
import type { StockageJeton } from './seConnecter';
import { seDeconnecter } from './seDeconnecter';

function stockage(jeton: string | null) {
  return { lire: () => jeton, enregistrer: vi.fn(), supprimer: vi.fn() } satisfies StockageJeton;
}

describe('seDeconnecter', () => {
  it('[FR-001-10] supprime le jeton, informe le service avec ce jeton et rend /login', () => {
    const jetons = stockage('jeton-user1');
    const deconnecter = vi.fn(() => Promise.resolve());

    expect(seDeconnecter({ deconnecter }, jetons)).toBe('/login');

    expect(jetons.supprimer).toHaveBeenCalledOnce();
    expect(deconnecter).toHaveBeenCalledExactlyOnceWith('jeton-user1');
  });

  it('[FR-001-10] sans jeton, rend /login sans appeler le service', () => {
    const deconnecter = vi.fn(() => Promise.resolve());

    expect(seDeconnecter({ deconnecter }, stockage(null))).toBe('/login');

    expect(deconnecter).not.toHaveBeenCalled();
  });

  it('[FR-001-10] une panne du service, asynchrone ou immédiate, ne retient pas l’utilisateur', async () => {
    const jetons = stockage('jeton');

    expect(seDeconnecter({ deconnecter: () => Promise.reject(new Error('réseau')) }, jetons)).toBe('/login');
    expect(
      seDeconnecter(
        {
          deconnecter: () => {
            throw new Error('immédiat');
          },
        },
        jetons,
      ),
    ).toBe('/login');

    await Promise.resolve();
    expect(jetons.supprimer).toHaveBeenCalledTimes(2);
  });
});
