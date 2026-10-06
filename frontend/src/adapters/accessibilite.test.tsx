// @vitest-environment jsdom
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import axe from 'axe-core';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import type { ReponseSimulation } from '../application/prets';
import { jetonDeTest } from './jeton/jetonDeTest';
import { rendreApplication } from './test/rendreApplication';
// index.html est hors de src : son import brut ne fausse pas la couverture.
import indexHtml from '../../index.html?raw';

beforeAll(() => {
  // Le document réel est index.html : sa langue est reprise telle quelle dans le document de test.
  const langue = /<html[^>]*\slang="([^"]+)"/.exec(indexHtml)?.[1];
  expect(langue).toBe('fr');
  document.documentElement.lang = langue as string;
  // jsdom n'a pas de canvas : axe s'en passe, sans le message « Not implemented » à chaque appel.
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
});

afterEach(() => {
  cleanup();
  localStorage.clear();
});

/** Identifiants des règles axe violées par le document courant. */
async function reglesViolees(): Promise<string[]> {
  const resultat = await axe.run(document, { resultTypes: ['violations'] });
  return resultat.violations.map((violation) => violation.id);
}

describe('accessibilité (axe)', () => {
  it('[axe] /login ne relève aucune violation', async () => {
    rendreApplication('/login');
    await screen.findByTestId('login-identifiant');

    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /login avec les erreurs de champ et le message de refus ne relève aucune violation', async () => {
    const { default: userEvent } = await import('@testing-library/user-event');
    const utilisateur = userEvent.setup();
    rendreApplication('/login');

    await utilisateur.type(screen.getByTestId('login-identifiant'), ' {Enter}');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.type(screen.getByTestId('login-identifiant'), 'user1');
    await utilisateur.type(screen.getByTestId('login-mot-de-passe'), 'faux{Enter}');
    await screen.findByTestId('login-message-erreur');
    expect(await reglesViolees()).toEqual([]);
  });

  it.each([
    ['CLIENT', '/tableau-de-bord'],
    ['ADMIN', '/administration'],
  ])('[axe] %s sur %s (en-tête, badge, sous-en-tête) ne relève aucune violation', async (role, chemin) => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
    rendreApplication(chemin);
    await screen.findByTestId('nav-salutation');
    await waitFor(() => expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBeNull());

    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /prets avec résultat de simulation, échéancier déplié, liste de prêts et erreur ne relève aucune violation', async () => {
    const { default: userEvent } = await import('@testing-library/user-event');
    const utilisateur = userEvent.setup();
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    const pret = {
      id: 1,
      type: 'TRAVAUX' as const,
      montant: '5000.00',
      tauxAnnuel: '4.9000',
      dureeMois: 24,
      mensualite: '218.63',
      coutTotal: '5247.12',
      totalInterets: '247.12',
      objet: null,
      statut: 'EN_ATTENTE' as const,
      dateDebut: '2026-08-01',
      dateFin: '2028-08-01',
    };
    const reponses = [
      { statut: 'simulation' as const, simulation: { mensualite: '293.02', coutTotal: '10548.72', totalInterets: '548.72', echeancier: [{ numero: 1, capital: '263.85', interets: '29.17', total: '293.02', capitalRestant: '0.00' }] } },
      { statut: 'technique' as const },
    ];
    rendreApplication('/prets', undefined, undefined, undefined, undefined, {
      simuler: () => Promise.resolve(reponses.shift() as ReponseSimulation),
      lister: () => Promise.resolve({ statut: 'prets' as const, prets: [pret] }),
    });
    await screen.findByTestId('pret-statut-1');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('pret-sim-calculer'));
    await utilisateur.click(await screen.findByTestId('pret-sim-echeancier-lien'));
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('pret-sim-calculer'));
    await screen.findByTestId('pret-message-erreur');
    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /prets sans prêt (état vide) ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
    rendreApplication('/prets');
    await screen.findByText('Aucun emprunt enregistre');

    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] profil en attente (aria-busy) ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CONSEILLER'] }));
    rendreApplication('/espace-conseiller', undefined, () => new Promise(() => {}));

    expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBe('true');
    expect(await reglesViolees()).toEqual([]);
  });

  describe('/bourse', () => {
    const SVG = '<svg viewBox="0 0 800 300"><text x="790" y="292">1 mois (données journalières)</text></svg>';
    const valeur = (id: number, symbole: string, nom: string) => ({ id, symbole, nom, marche: 'Euronext Paris', devise: 'EUR' });
    const point = (date: string, cloture: string) => ({ date, ouverture: '100.0000', haut: '110.0000', bas: '90.0000', cloture, volume: 10 });

    function ouvrirBourse(points: ReturnType<typeof point>[]) {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
      return rendreApplication('/bourse', undefined, undefined, undefined, undefined, {
        lireWatchlist: () => Promise.resolve({ statut: 'ok', donnees: [valeur(1, 'AI.PA', 'Air Liquide'), valeur(2, 'BNP.PA', 'BNP Paribas')] }),
        lireHistorique: (_jeton, symbole, periode) =>
          Promise.resolve({ statut: 'ok', donnees: { symbole, periode, points, graphique: points.length > 0 ? SVG : null } }),
        ajouter: () => Promise.resolve({ statut: 'refusee', message: 'Le symbole est obligatoire (ex: AMZN, SG.PA)' }),
      });
    }

    it('[axe] /bourse avec indicateurs, graphique, séances et watchlist ne relève aucune violation', async () => {
      ouvrirBourse([point('2026-09-30', '100.0000'), point('2026-10-01', '98.5000'), point('2026-10-02', '101.0000')]);
      await screen.findByTestId('bourse-valeur');
      await waitFor(() => expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBeNull());

      expect(await reglesViolees()).toEqual([]);
    });

    it('[axe] /bourse sans données et avec le formulaire d’ajout refusé ne relève aucune violation', async () => {
      const { default: userEvent } = await import('@testing-library/user-event');
      const utilisateur = userEvent.setup();
      ouvrirBourse([]);
      await screen.findByTestId('bourse-aucune-donnee');

      await utilisateur.click(screen.getByTestId('bourse-ajouter'));
      await utilisateur.click(screen.getByTestId('bourse-formulaire-ajouter'));
      await screen.findByTestId('bourse-ajout-erreur');

      expect(await reglesViolees()).toEqual([]);
    });
  });

  it('[axe] cas témoin : un champ sans <label for> viole la règle label', async () => {
    render(
      <main>
        <h1>Témoin</h1>
        <form>
          <input type="text" name="username" />
        </form>
      </main>,
    );

    expect(await reglesViolees()).toContain('label');
  });
});
