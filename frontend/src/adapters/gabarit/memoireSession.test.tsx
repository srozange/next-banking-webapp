// @vitest-environment jsdom
import { cleanup, render, renderHook } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { FournisseurMemoireSession, useMemoireSession, type MemoireSession } from './memoireSession';

afterEach(cleanup);

function memoireRendue() {
  let memoire: MemoireSession | null = null;
  function Lecteur() {
    memoire = useMemoireSession();
    return null;
  }
  const rendu = render(
    <FournisseurMemoireSession>
      <Lecteur />
    </FournisseurMemoireSession>,
  );
  return { rendu, memoire: () => memoire as unknown as MemoireSession };
}

describe('memoireSession', () => {
  it('[FR-015-02] garde la promesse par clé : un seul chargement', async () => {
    const { memoire } = memoireRendue();
    const charger = vi.fn(() => Promise.resolve(42));

    const premiere = memoire().obtenir('portefeuille', charger);
    const seconde = memoire().obtenir('portefeuille', charger);

    expect(seconde).toBe(premiere);
    await expect(seconde).resolves.toBe(42);
    expect(charger).toHaveBeenCalledOnce();
  });

  it('[FR-015-02] oublier une clé fait recharger au prochain appel ; les autres clés restent', async () => {
    const { memoire } = memoireRendue();
    const charger = vi.fn(() => Promise.resolve('a'));
    const autre = vi.fn(() => Promise.resolve('b'));
    void memoire().obtenir('portefeuille', charger);
    void memoire().obtenir('autre', autre);

    memoire().oublier('portefeuille');
    void memoire().obtenir('portefeuille', charger);
    void memoire().obtenir('autre', autre);

    expect(charger).toHaveBeenCalledTimes(2);
    expect(autre).toHaveBeenCalledOnce();
  });

  it('[FR-015-02] la mémoire survit aux rendus du fournisseur et repart à neuf après son démontage', () => {
    const { rendu, memoire } = memoireRendue();
    const premiere = memoire();
    rendu.rerender(
      <FournisseurMemoireSession>
        <span />
      </FournisseurMemoireSession>,
    );
    rendu.unmount();
    const charger = vi.fn(() => Promise.resolve(1));
    void premiere.obtenir('portefeuille', charger);

    const { memoire: nouvelle } = memoireRendue();
    void nouvelle().obtenir('portefeuille', charger);

    expect(nouvelle()).not.toBe(premiere);
    expect(charger).toHaveBeenCalledTimes(2);
  });

  it('lève une erreur hors du fournisseur', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => renderHook(() => useMemoireSession())).toThrow('useMemoireSession hors de FournisseurMemoireSession');
    vi.restoreAllMocks();
  });
});
