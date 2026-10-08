import { describe, expect, it, vi } from 'vitest';
import type { PretAdministration } from '../domain/pret';
import { chargerPipelinePrets, deciderPret, type ClientPipelinePrets } from './pipelinePrets';

const pret: PretAdministration = {
  id: 42,
  clientId: 1,
  nomClient: 'Jean Dupont',
  type: 'CONSOMMATION',
  montant: '10000.00',
  statut: 'EN_ATTENTE',
  dureeMois: 36,
  tauxAnnuel: '3.5000',
  mensualite: '293.02',
  objet: 'Voiture',
  dateDebut: '2026-10-01',
};

const client = (partiel: Partial<ClientPipelinePrets> = {}): ClientPipelinePrets => ({
  lireEnAttente: vi.fn(() => Promise.resolve({ statut: 'liste' as const, prets: [pret] })),
  lireHistorique: vi.fn(() => Promise.resolve({ statut: 'liste' as const, prets: [pret, { ...pret, id: 41, statut: 'EN_COURS' as const }] })),
  decider: vi.fn(() => Promise.resolve({ statut: 'effectuee' as const })),
  ...partiel,
});

describe('chargerPipelinePrets', () => {
  it('[FR-016-03][FR-016-12] rend le pipeline et l’historique', async () => {
    const contenu = await chargerPipelinePrets(client());

    expect(contenu.enAttente).toEqual([pret]);
    expect(contenu.historique).toHaveLength(2);
  });

  it('[FR-016-03][FR-016-12] rend null pour une partie technique ou en coupure, l’autre restant lisible', async () => {
    expect(await chargerPipelinePrets(client({ lireEnAttente: () => Promise.resolve({ statut: 'technique' }) }))).toEqual({
      enAttente: null,
      historique: expect.any(Array),
    });
    expect(await chargerPipelinePrets(client({ lireHistorique: () => Promise.reject(new Error('réseau')) }))).toEqual({
      enAttente: [pret],
      historique: null,
    });
  });
});

describe('deciderPret', () => {
  it('[FR-016-04] approuve le prêt sans confirmation', async () => {
    const c = client();

    expect(await deciderPret('approuver', 42, c)).toBe('effectuee');
    expect(c.decider).toHaveBeenCalledWith('approuver', 42);
  });

  it('[FR-016-05] refuse le prêt sans confirmation', async () => {
    const c = client();

    expect(await deciderPret('refuser', 42, c)).toBe('effectuee');
    expect(c.decider).toHaveBeenCalledWith('refuser', 42);
  });

  it('[FR-016-07] toute réponse technique ou coupure est le même résultat d’erreur', async () => {
    expect(await deciderPret('approuver', 42, client({ decider: () => Promise.resolve({ statut: 'technique' }) }))).toBe('erreur');
    expect(await deciderPret('refuser', 42, client({ decider: () => Promise.reject(new Error('réseau')) }))).toBe('erreur');
  });
});
