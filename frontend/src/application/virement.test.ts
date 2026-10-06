import { describe, expect, it, vi } from 'vitest';
import type { SaisieVirement } from '../domain/virement';
import { chargerComptes, executerVirement, type ClientVirement } from './virement';

const saisie = (partielle: Partial<SaisieVirement> = {}): SaisieVirement => ({
  compteSource: '1',
  compteDestinataire: '2',
  montant: '100,00',
  motif: 'loyer',
  ...partielle,
});

const client = (executer: ClientVirement['executer'], lireComptes: ClientVirement['lireComptes'] = () => Promise.resolve({ statut: 'technique' })) => ({
  executer: vi.fn(executer),
  lireComptes: vi.fn(lireComptes),
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
