import { describe, expect, it, vi } from 'vitest';
import type { OperationBrute } from '../domain/releve';
import { chargerTableauDeBord, type ClientTableauDeBord, type ReponseTableauDeBord } from './consulterTableauDeBord';

const operation = (n: number): OperationBrute => ({ date: '2026-11-03T08:00:00Z', description: `op ${n}`, type: 'DEBIT', montant: '100.00', soldeApres: '2030.00' });
const client = (reponse: () => Promise<ReponseTableauDeBord>) => ({ lire: vi.fn(reponse) }) satisfies ClientTableauDeBord;

describe('chargerTableauDeBord', () => {
  it('[FR-002-03] somme les soldes, formate les comptes et limite à 5 lignes', async () => {
    const c = client(() =>
      Promise.resolve({
        statut: 'tableau',
        comptes: [
          { id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' },
          { id: 2, type: 'EPARGNE', numero: 'FR76-0001-0002', solde: '15200.00' },
        ],
        dernieresOperations: [1, 2, 3, 4, 5, 6].map(operation),
      }),
    );

    const tableau = await chargerTableauDeBord(c);

    expect(c.lire).toHaveBeenCalledTimes(1);
    expect(tableau?.soldeTotal).toBe('EUR17,330.00');
    expect(tableau?.comptes.map((compte) => compte.solde)).toEqual(['EUR2,130.00', 'EUR15,200.00']);
    expect(tableau?.lignes).toHaveLength(5);
    expect(tableau?.lignes[0]).toMatchObject({ type: 'DEBIT', montant: '-EUR100.00', soldeApres: 'EUR2,030.00' });
  });

  it('[FR-002-04] sans compte : solde nul, aucune ligne', async () => {
    const tableau = await chargerTableauDeBord(client(() => Promise.resolve({ statut: 'tableau', comptes: [], dernieresOperations: [] })));

    expect(tableau).toEqual({ soldeTotal: 'EUR0.00', comptes: [], lignes: [] });
  });

  it('[FR-002-02] rend null si le service est indisponible ou lève', async () => {
    expect(await chargerTableauDeBord(client(() => Promise.resolve({ statut: 'technique' })))).toBeNull();
    expect(await chargerTableauDeBord(client(() => Promise.reject(new Error('réseau'))))).toBeNull();
  });
});
