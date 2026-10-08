import { describe, expect, it, vi } from 'vitest';
import type { HistoriqueCours, ValeurWatchlist } from '../domain/bourse';
import { chargerBourse, chargerHistorique, type ClientBourse } from './consulterBourse';
import type { SessionLocale } from './session';

function sessionEnMemoire(ouverte = true, roles: readonly string[] | null = null) {
  let enCours = ouverte;
  return {
    ouverte: vi.fn(() => enCours),
    roles: vi.fn(() => roles),
    terminer: vi.fn(() => {
      enCours = false;
    }),
    secondesRestantes: vi.fn(() => null),
  } satisfies SessionLocale;
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
      surcharge.lireHistorique ?? ((symbole) => Promise.resolve({ statut: 'ok', donnees: historiqueDe(symbole) })),
    ),
    ajouter: vi.fn<ClientBourse['ajouter']>(surcharge.ajouter ?? (() => Promise.resolve({ statut: 'technique' }))),
    retirer: vi.fn<ClientBourse['retirer']>(surcharge.retirer ?? (() => Promise.resolve({ statut: 'ok', donnees: null }))),
  };
}

describe('chargerBourse', () => {
  it('[FR-009-02][FR-029-06] sélectionne la première valeur et charge son historique', async () => {
    const client = clientBourse();

    const resultat = await chargerBourse(client, sessionEnMemoire(), null, '1M');

    expect(resultat).toEqual({
      type: 'etat',
      etat: { valeurs: [AIR_LIQUIDE, BNP], selection: 'AI.PA', historique: historiqueDe('AI.PA'), technique: false },
    });
    expect(client.lireWatchlist).toHaveBeenCalledWith();
    expect(client.lireHistorique).toHaveBeenCalledWith('AI.PA', '1M');
  });

  it('[FR-029-10] conserve la valeur souhaitée si elle figure dans la watchlist', async () => {
    const client = clientBourse();

    const resultat = await chargerBourse(client, sessionEnMemoire(), 'BNP.PA', '1Y');

    expect(resultat).toMatchObject({ type: 'etat', etat: { selection: 'BNP.PA' } });
    expect(client.lireHistorique).toHaveBeenCalledWith('BNP.PA', '1Y');
  });

  it('[FR-029-12] watchlist vide : aucune sélection, aucun historique demandé', async () => {
    const client = clientBourse({ lireWatchlist: () => Promise.resolve({ statut: 'ok', donnees: [] }) });

    await expect(chargerBourse(client, sessionEnMemoire(), 'AI.PA', '1M')).resolves.toEqual({
      type: 'etat',
      etat: { valeurs: [], selection: null, historique: null, technique: false },
    });
    expect(client.lireHistorique).not.toHaveBeenCalled();
  });

  it('[FR-009-09] historique sans point : rendu tel quel', async () => {
    const client = clientBourse({ lireHistorique: (_j, symbole) => Promise.resolve({ statut: 'ok', donnees: historiqueDe(symbole, false) }) });

    const resultat = await chargerBourse(client, sessionEnMemoire(), null, '1M');

    expect(resultat).toMatchObject({ type: 'etat', etat: { historique: { points: [] }, technique: false } });
  });

  it('sans session : session terminée, sans appel', async () => {
    const client = clientBourse();
    const session = sessionEnMemoire(false);

    await expect(chargerBourse(client, session, null, '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(client.lireWatchlist).not.toHaveBeenCalled();
  });

  it.each([
    ['la watchlist', { lireWatchlist: () => Promise.resolve({ statut: 'nonAuthentifie' as const }) }],
    ['l’historique', { lireHistorique: () => Promise.resolve({ statut: 'nonAuthentifie' as const }) }],
  ])('un 401 sur %s termine la session', async (_appel, surcharge) => {
    const session = sessionEnMemoire();

    await expect(chargerBourse(clientBourse(surcharge), session, null, '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-009-09] watchlist illisible : panne technique', async () => {
    const client = clientBourse({ lireWatchlist: () => Promise.resolve({ statut: 'technique' }) });

    await expect(chargerBourse(client, sessionEnMemoire(), null, '1M')).resolves.toEqual({ type: 'technique' });
  });

  it('[FR-009-09] historique illisible : watchlist et sélection rendues, panne signalée', async () => {
    const client = clientBourse({ lireHistorique: () => Promise.resolve({ statut: 'technique' }) });

    await expect(chargerBourse(client, sessionEnMemoire(), null, '1M')).resolves.toEqual({
      type: 'etat',
      etat: { valeurs: [AIR_LIQUIDE, BNP], selection: 'AI.PA', historique: null, technique: true },
    });
  });
});

describe('chargerHistorique', () => {
  it('[FR-009-03] recharge seulement l’historique de la valeur et de la période', async () => {
    const client = clientBourse();

    await expect(chargerHistorique(client, sessionEnMemoire(), 'BNP.PA', '5Y')).resolves.toEqual({
      type: 'historique',
      historique: historiqueDe('BNP.PA'),
    });
    expect(client.lireHistorique).toHaveBeenCalledWith('BNP.PA', '5Y');
    expect(client.lireWatchlist).not.toHaveBeenCalled();
  });

  it('sans session : session terminée, sans appel', async () => {
    const client = clientBourse();

    await expect(chargerHistorique(client, sessionEnMemoire(false), 'AI.PA', '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(client.lireHistorique).not.toHaveBeenCalled();
  });

  it('un 401 termine la session', async () => {
    const session = sessionEnMemoire();
    const client = clientBourse({ lireHistorique: () => Promise.resolve({ statut: 'nonAuthentifie' }) });

    await expect(chargerHistorique(client, session, 'AI.PA', '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-009-09] panne technique : rendue sans exception', async () => {
    const client = clientBourse({ lireHistorique: () => Promise.resolve({ statut: 'technique' }) });

    await expect(chargerHistorique(client, sessionEnMemoire(), 'AI.PA', '1M')).resolves.toEqual({ type: 'technique' });
  });
});
