import { describe, expect, it, vi } from 'vitest';
import type { HistoriqueCours, ValeurWatchlist } from '../domain/bourse';
import type { ClientBourse, DemandeAjout } from './consulterBourse';
import { ajouterValeur, retirerValeur } from './gererWatchlist';
import type { StockageJeton } from './seConnecter';

// Outils de test, répétés de consulterBourse.test.ts : un fichier de test n'en importe pas un autre.
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


const SG = { id: 10, symbole: 'SG.PA', nom: 'Société Générale', marche: 'Euronext Paris', devise: 'EUR' };
const DEMANDE: DemandeAjout = { symbole: 'sg.pa', nom: 'Société Générale', marche: 'Euronext Paris', devise: 'EUR' };

describe('ajouterValeur', () => {
  it('[FR-009-12][FR-029-10] envoie la saisie, relit la watchlist, sélectionne la nouvelle valeur et charge son historique', async () => {
    const client = clientBourse({
      ajouter: () => Promise.resolve({ statut: 'ok', donnees: SG }),
      lireWatchlist: () => Promise.resolve({ statut: 'ok', donnees: [AIR_LIQUIDE, BNP, SG] }),
    });

    const resultat = await ajouterValeur(client, stockageEnMemoire(), DEMANDE, '1Y');

    expect(client.ajouter).toHaveBeenCalledWith('jeton', DEMANDE);
    expect(client.lireHistorique).toHaveBeenCalledWith('jeton', 'SG.PA', '1Y');
    expect(resultat).toEqual({
      type: 'etat',
      etat: { valeurs: [AIR_LIQUIDE, BNP, SG], selection: 'SG.PA', historique: historiqueDe('SG.PA'), technique: false },
    });
  });

  it.each([
    ['[FR-009-11][FR-029-09] le symbole est vide', 'Le symbole est obligatoire (ex: AMZN, SG.PA)'],
    ['[FR-009-11][FR-029-09] le symbole existe déjà', 'Le symbole « BNP.PA » existe déjà dans votre watchlist'],
  ])('%s : le message du service est rendu, rien n’est relu', async (_cas, message) => {
    const client = clientBourse({ ajouter: () => Promise.resolve({ statut: 'refusee', message }) });

    await expect(ajouterValeur(client, stockageEnMemoire(), DEMANDE, '1M')).resolves.toEqual({ type: 'refuse', message });
    expect(client.lireWatchlist).not.toHaveBeenCalled();
  });

  it('une panne technique est rendue, rien n’est relu', async () => {
    const client = clientBourse();

    await expect(ajouterValeur(client, stockageEnMemoire(), DEMANDE, '1M')).resolves.toEqual({ type: 'technique' });
    expect(client.lireWatchlist).not.toHaveBeenCalled();
  });

  it('sans jeton : session terminée, sans appel', async () => {
    const client = clientBourse();

    await expect(ajouterValeur(client, stockageEnMemoire(null), DEMANDE, '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(client.ajouter).not.toHaveBeenCalled();
  });

  it('un 401 supprime le jeton et termine la session', async () => {
    const stockage = stockageEnMemoire();
    const client = clientBourse({ ajouter: () => Promise.resolve({ statut: 'nonAuthentifie' }) });

    await expect(ajouterValeur(client, stockage, DEMANDE, '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });
});

describe('retirerValeur', () => {
  it('[FR-009-13][FR-029-12] retire sans confirmation ; la valeur sélectionnée retirée, la première restante est sélectionnée', async () => {
    const client = clientBourse({ lireWatchlist: () => Promise.resolve({ statut: 'ok', donnees: [BNP] }) });

    const resultat = await retirerValeur(client, stockageEnMemoire(), 1, 'AI.PA', '1M');

    expect(client.retirer).toHaveBeenCalledWith('jeton', 1);
    expect(client.lireHistorique).toHaveBeenCalledWith('jeton', 'BNP.PA', '1M');
    expect(resultat).toMatchObject({ type: 'etat', etat: { valeurs: [BNP], selection: 'BNP.PA' } });
  });

  it('[FR-009-13] une autre valeur retirée : la sélection est conservée', async () => {
    const client = clientBourse({ lireWatchlist: () => Promise.resolve({ statut: 'ok', donnees: [AIR_LIQUIDE] }) });

    const resultat = await retirerValeur(client, stockageEnMemoire(), 2, 'AI.PA', '1M');

    expect(resultat).toMatchObject({ type: 'etat', etat: { selection: 'AI.PA' } });
  });

  it('[FR-009-13][FR-029-12] la dernière valeur retirée : sélection effacée, aucun historique', async () => {
    const client = clientBourse({ lireWatchlist: () => Promise.resolve({ statut: 'ok', donnees: [] }) });

    const resultat = await retirerValeur(client, stockageEnMemoire(), 1, 'AI.PA', '1M');

    expect(resultat).toEqual({ type: 'etat', etat: { valeurs: [], selection: null, historique: null, technique: false } });
    expect(client.lireHistorique).not.toHaveBeenCalled();
  });

  it('une panne technique est rendue, rien n’est relu', async () => {
    const client = clientBourse({ retirer: () => Promise.resolve({ statut: 'technique' }) });

    await expect(retirerValeur(client, stockageEnMemoire(), 1, 'AI.PA', '1M')).resolves.toEqual({ type: 'technique' });
    expect(client.lireWatchlist).not.toHaveBeenCalled();
  });

  it('sans jeton : session terminée, sans appel', async () => {
    const client = clientBourse();

    await expect(retirerValeur(client, stockageEnMemoire(null), 1, null, '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(client.retirer).not.toHaveBeenCalled();
  });

  it('un 401 supprime le jeton et termine la session', async () => {
    const stockage = stockageEnMemoire();
    const client = clientBourse({ retirer: () => Promise.resolve({ statut: 'nonAuthentifie' }) });

    await expect(retirerValeur(client, stockage, 1, 'AI.PA', '1M')).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });
});
