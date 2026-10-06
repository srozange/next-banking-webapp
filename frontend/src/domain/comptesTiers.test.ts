import { describe, expect, it } from 'vitest';
import { estCredit, formaterMontantTransaction, formaterSolde, libelleTypeCompte, SOLDE_INDISPONIBLE } from './comptesTiers';

describe('comptes tiers : règles pures', () => {
  it.each([
    ['checking', 'Compte courant'],
    ['savings', 'Epargne'],
    ['card', 'Carte'],
    ['loan', 'Credit'],
    ['market', 'Bourse'],
    [null, 'Compte'],
    ['autre', 'autre'],
    ['toString', 'toString'],
  ])('[FR-012-11] le type %s s’affiche « %s »', (type, libelle) => {
    expect(libelleTypeCompte(type)).toBe(libelle);
  });

  it('[BUG-042][FR-012-12] un solde absent est « Solde indisponible », un solde nul « EUR0.00 »', () => {
    expect(formaterSolde({ solde: null, soldeAbsent: true })).toBe(SOLDE_INDISPONIBLE);
    expect(formaterSolde({ solde: null, soldeAbsent: false })).toBe(SOLDE_INDISPONIBLE);
    expect(formaterSolde({ solde: '0.00', soldeAbsent: true })).toBe(SOLDE_INDISPONIBLE);
    expect(formaterSolde({ solde: '0.00', soldeAbsent: false })).toBe('EUR0.00');
    expect(formaterSolde({ solde: '1234.50', soldeAbsent: false })).toBe('EUR1,234.50');
  });

  it('[FR-012-13] seul un montant strictement positif est un crédit, préfixé « + »', () => {
    expect(estCredit('0.01')).toBe(true);
    expect(estCredit('0.00')).toBe(false);
    expect(estCredit('-3.00')).toBe(false);
    expect(formaterMontantTransaction('1234.50')).toBe('+1,234.50');
    expect(formaterMontantTransaction('0.00')).toBe('0.00');
    expect(formaterMontantTransaction('-12.00')).toBe('-12.00');
  });
});
