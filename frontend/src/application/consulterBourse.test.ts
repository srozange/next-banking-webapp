import { describe, expect, it, vi } from 'vitest';
import type { HistoriqueCours, ValeurWatchlist } from '../domain/bourse';
import { chargerBourse, chargerHistorique, jetonOuFin, type ClientBourse } from './consulterBourse';
import type { StockageJeton } from './seConnecter';

function stockageEnMemoire(initial: string | null = 'jeton') {
  let jeton = initial;
  return {
    lire: vi.fn(() => jeton),
    enregistrer: vi.fn((valeur: string) => {
      jeton = valeur;
    }),
    supprimer: vi.fn(() => {
      jeton = null;
    }),
  } satisfies StockageJeton;
}

const AIR_LIQUIDE: ValeurWatchlist = { id: 1, symbole: 'AI.PA', nom: 'Air Liquide', marche: 'Euronext Paris', devise: 'EUR' };
const BNP: ValeurWatchlist = { id: 2, symbole: 'BNP.PA', nom: 'BNP Paribas', marche: 'Euronext Paris', devise: 'EUR' };

function historiqueDe(symbole: string, avecPoints = true): HistoriqueCours {
  return {
    symbole,
    periode: '1M',
    points: avecPoints
      ? [{ date: '2026-10-02', ouverture: '1.00', haut: '1.00', bas: '1.00', cloture: '1.00', volume: 1 }]
      : [],
    graphique: null,
  };
}

/** Client bourse simulé : chaque méthode rend la réponse donnée, par défaut une réponse nominale. */
function clientBourse(surcharge: Partial<ClientBourse> = {}) {
  return {
    lireWatchlist: vi.fn<ClientBourse['lireWatchlist']>(
      surcharge.lireWatchlist ?? (() => Promise.resolve({ statut: 'ok', donnees: [AIR_LIQUIDE, BNP] })),
    ),
    lireHistorique: vi.fn<ClientBourse['lireHistorique']>(
      surcharge.lireHistorique ?? ((_jeton, symbole) => Promise.resolve({ statut: 'ok', donnees: historiqueDe(symbole) })),
    ),
    ajouter: vi.fn<ClientBourse['ajouter']>(surcharge.ajouter ?? (() => Promise.resolve({ statut: 'technique' }))),
    retirer: vi.fn<ClientBourse['retirer']>(surcharge.retirer ?? (() => Promise.resolve({ statut: 'ok', donnees: null }))),
  };
}

describe('jetonOuFin', () => {
  it('rend le jeton de la session', () => {
    expect(jetonOuFin(stockageEnMemoire('abc'))).toBe('abc');
  });

  it('sans jeton, supprime ce qui reste et rend null', () => {
    const stockage = stockageEnMemoire(null);

    expect(jetonOuFin(stockage)).toBeNull();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });
});

describe('chargerBourse', () => {
  it('[FR-009-02][FR-029-06] sélectionne la première valeur et charge son historique avec le jeton', async () => {
    const client = clientBourse();

    const resultat = await chargerBourse(client, stockageEnMemoire(), null, '1M');

    expect(resultat).toEqual({
      type: 'etat',
      etat: { valeurs: [AIR_LIQUIDE, BNP], selection: 'AI.PA', historique: historiqueDe('AI.PA'), technique: false },
    });
    expect(client.lireWatchlist).toHaveBeenCalledWith('jeton');
    expect(client.lireHistorique).toHaveBeenCalledWith('jeton', 'AI.PA', '1M');
  });

  it('[FR-029-10] conserve la valeur souhaitée si elle figure dans la watchlist', async () => {
    const client = clientBourse();

    const resultat = await chargerBourse(client, stockageEnMemoire(), 'BNP.PA', '1Y');

    expect(resultat).toMatchObject({ type: 'etat', etat: { selection: 'BNP.PA' } });
    expect(client.lireHistorique).toHaveBeenCalledWith('jeton', 'BNP.PA', '1Y');
  });

  it('[FR-029-12] watchlist vide : aucune sélection, aucun historique demandé', async () => {
    const client = clientBourse({ lireWatchlist: () => Promise.resolve({ statut: 'ok', donnees: [] }) });

    await expect(chargerBourse(client, stockageEnMemoire(), 'AI.PA', '1M')).resolves.toEqual({
      type: 'etat',
      etat: { valeurs: [], selection: null, historique: null, technique: false },
    });
    expect(client.lireHistorique).not.toHaveBeenCalled();
  });

  it('[FR-009-09] historique sans point : rendu tel quel', async () => {
    const client = clientBourse({ lireHistorique: (_j, symbole) => Promise.resolve({ statut: 'ok', donnees: historiqueDe(symbole, false) }) });

    const resultat = await chargerBourse(client, stockageEnMemoire(), null, '1M');

    expect(resultat).toMatchObject({ type: 'etat', etat: { historique: { points: [] }, technique: false } });
  });

  it('sans jeton : session terminée, sans appel', async () => {
    const client = clientBourse();
    const stockage = stockageEnMemoire(null);

    await expect(chargerBourse(client, stockage, null, '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(client.lireWatchlist).not.toHaveBeenCalled();
  });

  it.each([
    ['la watchlist', { lireWatchlist: () => Promise.resolve({ statut: 'nonAuthentifie' as const }) }],
    ['l’historique', { lireHistorique: () => Promise.resolve({ statut: 'nonAuthentifie' as const }) }],
  ])('un 401 sur %s supprime le jeton et termine la session', async (_appel, surcharge) => {
    const stockage = stockageEnMemoire();

    await expect(chargerBourse(clientBourse(surcharge), stockage, null, '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-009-09] watchlist illisible : panne technique', async () => {
    const client = clientBourse({ lireWatchlist: () => Promise.resolve({ statut: 'technique' }) });

    await expect(chargerBourse(client, stockageEnMemoire(), null, '1M')).resolves.toEqual({ type: 'technique' });
  });

  it('[FR-009-09] historique illisible : watchlist et sélection rendues, panne signalée', async () => {
    const client = clientBourse({ lireHistorique: () => Promise.resolve({ statut: 'technique' }) });

    await expect(chargerBourse(client, stockageEnMemoire(), null, '1M')).resolves.toEqual({
      type: 'etat',
      etat: { valeurs: [AIR_LIQUIDE, BNP], selection: 'AI.PA', historique: null, technique: true },
    });
  });
});

describe('chargerHistorique', () => {
  it('[FR-009-03] recharge seulement l’historique de la valeur et de la période', async () => {
    const client = clientBourse();

    await expect(chargerHistorique(client, stockageEnMemoire(), 'BNP.PA', '5Y')).resolves.toEqual({
      type: 'historique',
      historique: historiqueDe('BNP.PA'),
    });
    expect(client.lireHistorique).toHaveBeenCalledWith('jeton', 'BNP.PA', '5Y');
    expect(client.lireWatchlist).not.toHaveBeenCalled();
  });

  it('sans jeton : session terminée, sans appel', async () => {
    const client = clientBourse();

    await expect(chargerHistorique(client, stockageEnMemoire(null), 'AI.PA', '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(client.lireHistorique).not.toHaveBeenCalled();
  });

  it('un 401 supprime le jeton et termine la session', async () => {
    const stockage = stockageEnMemoire();
    const client = clientBourse({ lireHistorique: () => Promise.resolve({ statut: 'nonAuthentifie' }) });

    await expect(chargerHistorique(client, stockage, 'AI.PA', '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-009-09] panne technique : rendue sans exception', async () => {
    const client = clientBourse({ lireHistorique: () => Promise.resolve({ statut: 'technique' }) });

    await expect(chargerHistorique(client, stockageEnMemoire(), 'AI.PA', '1M')).resolves.toEqual({ type: 'technique' });
  });
});
