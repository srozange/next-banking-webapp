import { describe, expect, it, vi } from 'vitest';
import { chargerDocuments, telechargerDocument, type ClientDocuments } from './consulterDocuments';

const COMPTE = { compteId: 1, numero: 'FR76-0001-0001', type: 'COURANT' };

function client(partiel: Partial<ClientDocuments>): ClientDocuments {
  return {
    lirePage: partiel.lirePage ?? (() => Promise.resolve({ statut: 'technique' })),
    telecharger: partiel.telecharger ?? (() => Promise.resolve({ statut: 'technique' })),
  };
}

describe('cas d’usage de la page Documents', () => {
  it('[FR-008-03] charge les comptes avec leurs 12 relevés et les deux années d’IFU de aujourdhui', async () => {
    const page = await chargerDocuments(client({ lirePage: () => Promise.resolve({ statut: 'page', aujourdhui: '2026-11-17', comptes: [COMPTE] }) }));

    expect(page?.anneesIfu).toEqual([2025, 2024]);
    expect(page?.comptes).toHaveLength(1);
    expect(page?.comptes[0]).toMatchObject(COMPTE);
    expect(page?.comptes[0]?.releves[0]?.libelle).toBe('Novembre 2026');
    expect(page?.comptes[0]?.releves).toHaveLength(12);
  });

  it('[FR-008-05] sans compte, garde les années d’IFU', async () => {
    const page = await chargerDocuments(client({ lirePage: () => Promise.resolve({ statut: 'page', aujourdhui: '2026-02-05', comptes: [] }) }));

    expect(page).toEqual({ comptes: [], anneesIfu: [2025, 2024] });
  });

  it.each([
    ['une panne technique', () => Promise.resolve({ statut: 'technique' as const })],
    ['une date inexploitable', () => Promise.resolve({ statut: 'page' as const, aujourdhui: 'demain', comptes: [] })],
    ['une exception', () => Promise.reject(new Error('coupure'))],
  ])('[FR-008-01] rend null sur %s', async (_cas, lirePage) => {
    expect(await chargerDocuments(client({ lirePage }))).toBeNull();
  });

  it('[FR-008-06] rend le PDF reçu', async () => {
    const pdf = new Blob(['%PDF']);
    const telecharger = vi.fn(() => Promise.resolve({ statut: 'pdf' as const, contenu: pdf }));

    expect(await telechargerDocument(client({ telecharger }), { type: 'RIB', compteId: 1 })).toBe(pdf);
    expect(telecharger).toHaveBeenCalledWith({ type: 'RIB', compteId: 1 });
  });

  it.each([
    ['une panne technique', () => Promise.resolve({ statut: 'technique' as const })],
    ['une exception', () => Promise.reject(new Error('coupure'))],
  ])('[FR-008-06] rend null, sans PDF, sur %s', async (_cas, telecharger) => {
    expect(await telechargerDocument(client({ telecharger }), { type: 'IFU', annee: 2025 })).toBeNull();
  });
});
