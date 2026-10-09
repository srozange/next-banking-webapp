import { describe, expect, it } from 'vitest';
import type { OperationBrute } from './releve';
import { compteTableau, dernieresOperations, formaterMontantTableau, ligneTableau, patrimoineTotalCentimes, soldeTotalCentimes } from './tableauDeBord';

const operation = (n: number): OperationBrute => ({ date: '2026-11-03T08:00:00Z', description: `op ${n}`, type: 'CREDIT', montant: '1.00', soldeApres: '2.00' });

describe('tableau de bord, règles pures', () => {
  it.each([
    [0, 'EUR0.00'],
    [5, 'EUR0.05'],
    [213000, 'EUR2,130.00'],
    [1733000, 'EUR17,330.00'],
    [123456789, 'EUR1,234,567.89'],
    [-1050, '-EUR10.50'],
  ])('[FR-002-05] %i centimes donnent %s', (centimes, attendu) => {
    expect(formaterMontantTableau(centimes)).toBe(attendu);
  });

  it('[FR-002-03] le solde total est la somme en centimes de tous les comptes', () => {
    const comptes = [
      { id: 1, type: 'COURANT', numero: 'a', solde: '2130.10' },
      { id: 2, type: 'EPARGNE', numero: 'b', solde: '15200.20' },
    ];

    expect(soldeTotalCentimes(comptes)).toBe(1733030);
  });

  it('[FR-002-04] sans compte, le solde total est nul', () => {
    expect(soldeTotalCentimes([])).toBe(0);
  });

  it('[FR-002-06] seules les 5 premières opérations sont gardées, dans l’ordre reçu', () => {
    const operations = [1, 2, 3, 4, 5, 6, 7].map(operation);

    expect(dernieresOperations(operations).map((o) => o.description)).toEqual(['op 1', 'op 2', 'op 3', 'op 4', 'op 5']);
    expect(dernieresOperations(operations.slice(0, 2))).toHaveLength(2);
  });

  it('[FR-002-05] un compte est prêt à afficher', () => {
    expect(compteTableau({ id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' })).toEqual({
      id: 1,
      type: 'COURANT',
      numero: 'FR76-0001-0001',
      solde: 'EUR2,130.00',
    });
  });

  it('[FR-002-06] un CREDIT porte « + », un DEBIT « - » ASCII', () => {
    const base = { date: '2026-11-03T08:00:00Z', description: 'x', montant: '1200.50', soldeApres: '8130.00' };

    expect(ligneTableau({ ...base, type: 'CREDIT' })).toEqual({ date: base.date, description: 'x', type: 'CREDIT', montant: '+EUR1,200.50', soldeApres: 'EUR8,130.00' });
    expect(ligneTableau({ ...base, type: 'DEBIT' }).montant).toBe('-EUR1,200.50');
  });
});

describe('patrimoine total', () => {
  it('[FR-002-12] additionne le total Demo Bank et les comptes externes en centimes entiers', () => {
    expect(patrimoineTotalCentimes(100000, '120.50')).toBe(112050);
    expect(patrimoineTotalCentimes(100000, '0.00')).toBe(100000);
    expect(patrimoineTotalCentimes(0, '0.10') + patrimoineTotalCentimes(0, '0.20')).toBe(30);
  });
});
