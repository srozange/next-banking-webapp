// @vitest-environment jsdom
import { afterEach, describe, expect, it } from 'vitest';
import { CLE_JETON, creerStockageJetonLocal } from './StockageJetonLocal';

describe('StockageJetonLocal', () => {
  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  it('[FR-001-04] écrit, lit et supprime le jeton sous nb.jeton du localStorage, jamais du sessionStorage', () => {
    const stockage = creerStockageJetonLocal();
    expect(stockage.lire()).toBeNull();

    stockage.enregistrer('jeton');
    expect(CLE_JETON).toBe('nb.jeton');
    expect(localStorage.getItem('nb.jeton')).toBe('jeton');
    expect(stockage.lire()).toBe('jeton');
    expect(sessionStorage.length).toBe(0);

    stockage.supprimer();
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });
});
