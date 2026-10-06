import { describe, expect, it, vi } from 'vitest';
import type { OperationBrute } from '../domain/releve';
import { chargerReleve, type ClientReleve, type ReponseOperations } from './consulterReleve';
import type { ClientVirement, Compte, ReponseComptes } from './virement';

const COMPTES: Compte[] = [
  { id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' },
  { id: 2, type: 'EPARGNE', numero: 'FR76-0001-0002', solde: '15200.00' },
];
const OPERATION: OperationBrute = { date: '2026-11-03T08:00:00Z', description: '[EN ATTENTE] x', type: 'CREDIT', montant: '5.00', soldeApres: '2135.00' };

const clientComptes = (reponse: () => Promise<ReponseComptes>) => ({ lireComptes: vi.fn(reponse), executer: vi.fn() }) as unknown as ClientVirement;
const comptesLus = (comptes: Compte[] = COMPTES) => clientComptes(() => Promise.resolve({ statut: 'comptes', comptes }));
const clientOperations = (reponse: (id: number) => Promise<ReponseOperations>) => ({ lireOperations: vi.fn(reponse) }) satisfies ClientReleve;

describe('chargerReleve', () => {
  it('[FR-005-02] sélectionne le premier compte et charge ses opérations', async () => {
    const releve = clientOperations(() => Promise.resolve({ statut: 'operations', operations: [OPERATION] }));

    const resultat = await chargerReleve(null, comptesLus(), releve);

    expect(releve.lireOperations).toHaveBeenCalledWith(1);
    expect(resultat?.comptes).toEqual(COMPTES);
    expect(resultat?.compteId).toBe(1);
    expect(resultat?.lignes).toEqual([
      { date: OPERATION.date, libelle: 'x', enAttente: true, type: 'CREDIT', montant: '+5,00 €', soldeApres: '2 135,00 €' },
    ]);
  });

  it('[FR-005-03] recharge les comptes puis les opérations du compte choisi', async () => {
    const ordre: string[] = [];
    const comptes = clientComptes(() => {
      ordre.push('comptes');
      return Promise.resolve({ statut: 'comptes', comptes: COMPTES });
    });
    const releve = clientOperations((id) => {
      ordre.push(`operations ${id}`);
      return Promise.resolve({ statut: 'operations', operations: [] });
    });

    const resultat = await chargerReleve(2, comptes, releve);

    expect(ordre).toEqual(['comptes', 'operations 2']);
    expect(resultat).toEqual({ comptes: COMPTES, compteId: 2, lignes: [] });
  });

  it('[FR-005-03] retombe sur le premier compte quand le compte demandé n’existe plus', async () => {
    const releve = clientOperations(() => Promise.resolve({ statut: 'operations', operations: [] }));

    expect((await chargerReleve(99, comptesLus(), releve))?.compteId).toBe(1);
  });

  it('[FR-005-02] sans compte : aucune opération et aucun appel au relevé', async () => {
    const releve = clientOperations(() => Promise.resolve({ statut: 'operations', operations: [OPERATION] }));

    expect(await chargerReleve(null, comptesLus([]), releve)).toEqual({ comptes: [], compteId: null, lignes: [] });
    expect(releve.lireOperations).not.toHaveBeenCalled();
  });

  it.each([
    ['comptes indisponibles', clientComptes(() => Promise.resolve({ statut: 'technique' })), () => Promise.resolve<ReponseOperations>({ statut: 'operations', operations: [] })],
    ['opérations indisponibles', comptesLus(), () => Promise.resolve<ReponseOperations>({ statut: 'technique' })],
    ['coupure sur les opérations', comptesLus(), () => Promise.reject(new Error('réseau'))],
  ])('[FR-005-05] rend null : %s', async (_nom, comptes, operations) => {
    expect(await chargerReleve(1, comptes, clientOperations(operations))).toBeNull();
  });
});
