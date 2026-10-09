import { describe, expect, it, vi } from 'vitest';
import type { OperationBrute } from '../domain/releve';
import type { ClientComptesTiers, ReponseTiers } from './comptesTiers';
import type { EtatComptesTiers } from '../domain/comptesTiers';
import { chargerPatrimoineExterne, chargerTableauDeBord, type ClientTableauDeBord, type ReponseTableauDeBord } from './consulterTableauDeBord';

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
    expect(tableau?.soldeTotalCentimes).toBe(1733000);
    expect(tableau?.comptes.map((compte) => compte.solde)).toEqual(['EUR2,130.00', 'EUR15,200.00']);
    expect(tableau?.lignes).toHaveLength(5);
    expect(tableau?.lignes[0]).toMatchObject({ type: 'DEBIT', montant: '-EUR100.00', soldeApres: 'EUR2,030.00' });
  });

  it('[FR-002-04] sans compte : solde nul, aucune ligne', async () => {
    const tableau = await chargerTableauDeBord(client(() => Promise.resolve({ statut: 'tableau', comptes: [], dernieresOperations: [] })));

    expect(tableau).toEqual({ soldeTotal: 'EUR0.00', soldeTotalCentimes: 0, comptes: [], lignes: [] });
  });

  it('[FR-002-02] rend null si le service est indisponible ou lève', async () => {
    expect(await chargerTableauDeBord(client(() => Promise.resolve({ statut: 'technique' })))).toBeNull();
    expect(await chargerTableauDeBord(client(() => Promise.reject(new Error('réseau'))))).toBeNull();
  });
});

const etat = (surcharge: Partial<EtatComptesTiers> = {}): EtatComptesTiers => ({
  connecte: true,
  comptes: [],
  compteSelectionne: null,
  transactions: [],
  message: '',
  patrimoineTotal: '120.50',
  ...surcharge,
});
const tiers = (reponse: () => Promise<ReponseTiers<EtatComptesTiers>>) => ({ lireEtat: vi.fn(reponse) }) as unknown as ClientComptesTiers;

describe('chargerPatrimoineExterne', () => {
  it('[FR-002-12] rend l’état de connexion, le nombre de comptes et le patrimoine externe', async () => {
    const compte = { id: 1, nom: 'A', type: null, solde: '1.00', soldeAbsent: false, iban: null };
    const c = tiers(() => Promise.resolve({ statut: 'ok', donnees: etat({ comptes: [compte, compte, compte] }) }));

    expect(await chargerPatrimoineExterne(c)).toEqual({ connecte: true, nombreComptes: 3, patrimoineTotal: '120.50' });
    expect(c.lireEtat).toHaveBeenCalledTimes(1);
  });

  it('[FR-002-12] non connecté', async () => {
    const lu = await chargerPatrimoineExterne(tiers(() => Promise.resolve({ statut: 'ok', donnees: etat({ connecte: false, patrimoineTotal: '0.00' }) })));

    expect(lu).toMatchObject({ connecte: false, nombreComptes: 0 });
  });

  it.each([
    ['indisponible', () => Promise.resolve({ statut: 'indisponible' as const })],
    ['technique', () => Promise.resolve({ statut: 'technique' as const })],
    ['non authentifié', () => Promise.resolve({ statut: 'nonAuthentifie' as const })],
    ['exception', () => Promise.reject(new Error('réseau'))],
    ['montant illisible', () => Promise.resolve({ statut: 'ok' as const, donnees: etat({ patrimoineTotal: 'abc' }) })],
  ])('[FR-002-12] %s : rien de Powens', async (_nom, reponse) => {
    expect(await chargerPatrimoineExterne(tiers(reponse))).toBeNull();
  });
});
