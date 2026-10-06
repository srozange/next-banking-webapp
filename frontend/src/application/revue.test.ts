import { describe, expect, it, vi } from 'vitest';
import type { VirementEnRevue, VirementTraite } from '../domain/virement';
import { chargerRevue, deciderVirement, type ClientRevue } from './revue';

const virement: VirementEnRevue = {
  virementId: 12,
  clientId: 7,
  nomClient: 'Claire Martin',
  montant: '12000.00',
  compteDestinataire: 'FR76-0001-0002',
  motifSuspension: 'Score cumulé 130 - signaux : LARGE_AMOUNT',
  heureButoir: '2026-11-06T09:15:00Z',
};
const traite: VirementTraite = { virement, issue: 'LIBERE', dateIssue: '2026-11-05T10:00:00Z' };

const client = (partiel: Partial<ClientRevue> = {}): ClientRevue => ({
  lireFile: vi.fn(() => Promise.resolve({ statut: 'liste' as const, virements: [virement] })),
  lireHistorique: vi.fn(() => Promise.resolve({ statut: 'liste' as const, virements: [traite] })),
  decider: vi.fn(() => Promise.resolve({ statut: 'enregistre' as const })),
  ...partiel,
});

describe('[FR-016-19] chargerRevue', () => {
  it('rend la file et l’historique', async () => {
    expect(await chargerRevue(client())).toEqual({ file: [virement], historique: [traite] });
  });

  it('rend null pour une partie technique ou en coupure, l’autre restant lisible', async () => {
    expect(await chargerRevue(client({ lireFile: () => Promise.resolve({ statut: 'technique' }) }))).toEqual({
      file: null,
      historique: [traite],
    });
    expect(await chargerRevue(client({ lireHistorique: () => Promise.reject(new Error('réseau')) }))).toEqual({
      file: [virement],
      historique: null,
    });
  });
});

describe('[FR-016-19] deciderVirement', () => {
  it('refuse un motif vide ou d’espaces sans appeler le service', async () => {
    const c = client();
    expect(await deciderVirement('liberer', 12, '', c)).toEqual({ type: 'motif' });
    expect(await deciderVirement('rejeter', 12, '   ', c)).toEqual({ type: 'motif' });
    expect(c.decider).not.toHaveBeenCalled();
  });

  it('transmet le motif rogné et rend la décision enregistrée', async () => {
    const c = client();
    expect(await deciderVirement('liberer', 12, ' vérifié par téléphone ', c)).toEqual({ type: 'enregistre', action: 'liberer' });
    expect(c.decider).toHaveBeenCalledWith('liberer', 12, 'vérifié par téléphone');
    expect(await deciderVirement('rejeter', 12, 'client injoignable', c)).toEqual({ type: 'enregistre', action: 'rejeter' });
  });

  it('rend le refus du service : solde insuffisant ou heure butoir', async () => {
    for (const issue of ['REFUSEE_SOLDE_INSUFFISANT', 'REFUSEE_HEURE_BUTOIR'] as const) {
      const c = client({ decider: () => Promise.resolve({ statut: 'refuse', issue }) });
      expect(await deciderVirement('liberer', 12, 'ok', c)).toEqual({ type: 'refuse', issue });
    }
  });

  it('rend le refus de motif du serveur, une erreur avec son message ou sans message', async () => {
    expect(await deciderVirement('liberer', 12, 'ok', client({ decider: () => Promise.resolve({ statut: 'motif' }) }))).toEqual({ type: 'motif' });
    expect(
      await deciderVirement('liberer', 12, 'ok', client({ decider: () => Promise.resolve({ statut: 'erreur', message: 'Accès refusé' }) })),
    ).toEqual({ type: 'erreur', message: 'Accès refusé' });
    expect(await deciderVirement('liberer', 12, 'ok', client({ decider: () => Promise.reject(new Error('réseau')) }))).toEqual({
      type: 'erreur',
      message: null,
    });
  });
});
