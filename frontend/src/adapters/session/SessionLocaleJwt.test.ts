import { describe, expect, it, vi } from 'vitest';
import type { DecodeurJeton } from '../jeton/DecodeurJetonJwt';
import type { StockageJeton } from '../stockage/StockageJetonLocal';
import { creerSessionLocale } from './SessionLocaleJwt';

function stockageEnMemoire(initial: string | null = null) {
  let jeton = initial;
  return {
    lire: vi.fn(() => jeton),
    enregistrer: vi.fn((valeur: string) => {
      jeton = valeur;
    }),
    supprimer: vi.fn(() => {
      jeton = null;
    }),
  } satisfies StockageJeton;
}

const decodeur = (roles: readonly string[] | null): DecodeurJeton => ({ roles: vi.fn(() => roles) });
const expiration = (secondes: number | null) => ({ secondesRestantes: vi.fn(() => secondes) });

describe('creerSessionLocale', () => {
  it('[FR-030-02] rend les rôles du jeton stocké', () => {
    const stockage = stockageEnMemoire('jeton');
    const lecteur = decodeur(['CONSEILLER']);

    expect(creerSessionLocale(stockage, lecteur, expiration(1)).roles()).toEqual(['CONSEILLER']);
    expect(lecteur.roles).toHaveBeenCalledWith('jeton');
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });

  it('[FR-030-02] rend null sans jeton, sans rien supprimer', () => {
    const stockage = stockageEnMemoire();

    expect(creerSessionLocale(stockage, decodeur(['CLIENT']), expiration(1)).roles()).toBeNull();
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });

  it.each([
    ['illisible', null],
    ['sans rôle connu', ['INVITE']],
  ])('[FR-030-02] supprime un jeton %s et rend null', (_cas, roles) => {
    const stockage = stockageEnMemoire('jeton');

    expect(creerSessionLocale(stockage, decodeur(roles), expiration(1)).roles()).toBeNull();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('est ouverte tant qu’un jeton est stocké, et se termine en le supprimant', () => {
    const stockage = stockageEnMemoire('jeton');
    const session = creerSessionLocale(stockage, decodeur(['CLIENT']), expiration(1));

    expect(session.ouverte()).toBe(true);
    session.terminer();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
    expect(session.ouverte()).toBe(false);
  });

  it('[FR-030-05] rend les secondes restantes du jeton stocké, null sans jeton', () => {
    const stockage = stockageEnMemoire('jeton');
    const calcul = expiration(42);
    const session = creerSessionLocale(stockage, decodeur(['CLIENT']), calcul);

    expect(session.secondesRestantes()).toBe(42);
    expect(calcul.secondesRestantes).toHaveBeenCalledWith('jeton');
    session.terminer();
    expect(session.secondesRestantes()).toBeNull();
  });

  it('[FR-030-05] expiration illisible : null', () => {
    expect(creerSessionLocale(stockageEnMemoire('jeton'), decodeur(['CLIENT']), expiration(null)).secondesRestantes()).toBeNull();
  });
});
