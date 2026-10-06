import { describe, expect, it, vi } from 'vitest';
import type { SaisieVirement } from '../domain/virement';
import { chargerComptes, chargerSuivi, executerVirement, type ClientVirement } from './virement';

const saisie = (partielle: Partial<SaisieVirement> = {}): SaisieVirement => ({
  compteSource: '1',
  compteDestinataire: '2',
  montant: '100,00',
  motif: 'loyer',
  ...partielle,
});

const client = (
  executer: ClientVirement['executer'],
  lireComptes: ClientVirement['lireComptes'] = () => Promise.resolve({ statut: 'technique' }),
  lireSuivi: ClientVirement['lireSuivi'] = () => Promise.resolve({ statut: 'technique' }),
) => ({
  executer: vi.fn(executer),
  lireComptes: vi.fn(lireComptes),
  lireSuivi: vi.fn(lireSuivi),
});

describe('chargerComptes', () => {
  it('rend les comptes, ou null si le service est indisponible', async () => {
    const comptes = [{ id: 1, type: 'COURANT', numero: 'FR76-0001-0001' }];
    expect(await chargerComptes(client(() => Promise.reject(new Error()), () => Promise.resolve({ statut: 'comptes', comptes })))).toEqual(comptes);
    expect(await chargerComptes(client(() => Promise.reject(new Error())))).toBeNull();
    expect(await chargerComptes(client(() => Promise.reject(new Error()), () => Promise.reject(new Error('réseau'))))).toBeNull();
  });
});

describe('[FR-003-02][FR-003-03][FR-003-08] executerVirement', () => {
  it('[FR-003-02] signale les champs vides sans appeler le service', async () => {
    const c = client(() => Promise.resolve({ statut: 'execute' }));
    expect(await executerVirement(saisie({ compteSource: '', montant: '' }), c)).toEqual({
      type: 'champs',
      champs: ['compteSource', 'montant'],
    });
    expect(c.executer).not.toHaveBeenCalled();
  });

  it('[FR-003-03] refuse deux comptes identiques sans appeler le service', async () => {
    const c = client(() => Promise.resolve({ statut: 'execute' }));
    expect(await executerVirement(saisie({ compteDestinataire: '1' }), c)).toEqual({ type: 'identiques' });
    expect(c.executer).not.toHaveBeenCalled();
  });

  it('[FR-003-08] soumet directement, le montant en chaîne à deux décimales', async () => {
    const c = client(() => Promise.resolve({ statut: 'execute' }));
    expect(await executerVirement(saisie(), c)).toEqual({ type: 'execute' });
    expect(c.executer).toHaveBeenCalledWith({ compteSourceId: 1, compteDestinataireId: 2, montant: '100.00', motif: 'loyer' });
  });

  it('[FR-003-04] transmet telle quelle une saisie illisible, que le service refuse', async () => {
    const c = client(() => Promise.resolve({ statut: 'erreur', message: 'Erreur lors du virement : Le montant doit etre positif' }));
    expect(await executerVirement(saisie({ montant: ' abc ' }), c)).toEqual({
      type: 'erreur',
      message: 'Erreur lors du virement : Le montant doit etre positif',
    });
    expect(c.executer).toHaveBeenCalledWith(expect.objectContaining({ montant: 'abc' }));
  });

  it('[FR-023-02] rend une erreur sans message quand l’appel échoue', async () => {
    expect(await executerVirement(saisie(), client(() => Promise.reject(new Error('réseau'))))).toEqual({
      type: 'erreur',
      message: null,
    });
  });
});

describe('[FR-027-04][FR-003-27] issues de la soumission', () => {
  it('[FR-027-04] rend la suspension avec l’heure butoir et l’écho du virement', async () => {
    const c = client(() =>
      Promise.resolve({ statut: 'suspendu', heureButoir: '2026-11-06T09:15:00Z', montant: '10000.00', compteDestinataireId: 2 }),
    );
    expect(await executerVirement(saisie(), c)).toEqual({
      type: 'suspendu',
      heureButoir: '2026-11-06T09:15:00Z',
      montant: '10000.00',
      compteDestinataireId: 2,
    });
  });

  it('[FR-003-27] rend le refus pour erreur du contrôle avec sa référence de trace', async () => {
    const c = client(() => Promise.resolve({ statut: 'refuse', reference: '4bf92f3577b34da6a3ce929d0e0e4736' }));
    expect(await executerVirement(saisie(), c)).toEqual({ type: 'refuse', reference: '4bf92f3577b34da6a3ce929d0e0e4736' });
  });
});

describe('[FR-027-16] chargerSuivi', () => {
  it('rend les virements passés en revue, ou null si le service est indisponible', async () => {
    const virements = [
      { virementId: 12, soumisLe: '2026-11-05T09:15:00Z', compteDestinataire: 'FR76-0001-0002', montant: '10000.00', etat: 'SUSPENDU' as const, heureButoir: '2026-11-06T09:15:00Z' },
    ];
    const executer = () => Promise.reject(new Error());
    expect(await chargerSuivi(client(executer, undefined, () => Promise.resolve({ statut: 'suivi', virements })))).toEqual(virements);
    expect(await chargerSuivi(client(executer))).toBeNull();
    expect(await chargerSuivi(client(executer, undefined, () => Promise.reject(new Error('réseau'))))).toBeNull();
  });
});
