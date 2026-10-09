// @vitest-environment jsdom
import { cleanup, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { jetonDeTest } from '../jeton/jetonDeTest';
import { rendreApplication } from '../test/rendreApplication';
import { ROUTES_PROTEGEES } from './routes';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

describe('routes', () => {
  it('déclare les quatre routes protégées hors /bourse, /produits et /comptes-tiers, qui ont leur écran', () => {
    expect(ROUTES_PROTEGEES).toEqual(['/tableau-de-bord', '/espace-conseiller', '/administration', '/prets']);
  });

  it.each(['/', '/tableau-de-bord', '/espace-conseiller', '/administration', '/prets', '/bourse', '/produits', '/comptes-tiers', '/documents', '/mon-conseiller', '/route-inconnue'])(
    '[FR-001-02][FR-030-01] sans jeton, %s mène à /login par remplacement',
    async (chemin) => {
      const { routeur } = rendreApplication(chemin);

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
      expect(routeur.state.historyAction).toBe('REPLACE');
      expect(await screen.findByTestId('login-identifiant')).toBeTruthy();
    },
  );

  it.each(ROUTES_PROTEGEES.filter((route) => route !== '/prets'))('avec un jeton, %s reste affichée, sans autre contenu que ses sections de revue', async (chemin) => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
    const { routeur } = rendreApplication(chemin);

    expect(routeur.state.location.pathname).toBe(chemin);
    const contenu = screen.getByTestId('application-racine').textContent;
    expect(contenu?.includes('Suivi de mes virements')).toBe(chemin === '/tableau-de-bord');
    expect(contenu?.includes('Virements suspendus — En attente de revue')).toBe(chemin === '/administration');
    if (chemin === '/espace-conseiller') {
      // L'écran du portefeuille (FEAT-015), sans section de revue.
      expect(screen.getByTestId('conseiller-contenu').textContent?.startsWith('Espace Conseiller')).toBe(true);
    }
  });

  it('[FR-027-16] le tableau de bord porte la section « Suivi de mes virements » (dashboard-virements-revue)', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    rendreApplication('/tableau-de-bord');

    expect(await screen.findByTestId('dashboard-virements-revue')).toBeTruthy();
    expect(await screen.findByText('Aucun virement à suivre')).toBeTruthy();
  });

  it.each([
    ['/tableau-de-bord', 'BankApp Demo - Tableau de bord'],
    ['/espace-conseiller', 'BankApp Demo - Espace Conseiller'],
    ['/administration', 'BankApp Demo - Administration'],
    ['/bourse', 'BankApp Demo - Bourse'],
    ['/produits', 'BankApp Demo - Produits bancaires'],
    ['/comptes-tiers', 'BankApp Demo - Comptes Tiers'],
  ])('%s porte le titre d’onglet du legacy', async (chemin, titre) => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
    rendreApplication(chemin);

    await waitFor(() => expect(document.title).toBe(titre));
  });

  it.each([
    ['ADMIN', '/administration'],
    ['CONSEILLER', '/espace-conseiller'],
    ['CLIENT', '/tableau-de-bord'],
  ])('[FR-030-03] avec un jeton %s, / et une route inconnue mènent à %s', async (role, route) => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
    for (const chemin of ['/', '/route-inconnue']) {
      const { routeur, unmount } = rendreApplication(chemin);
      await waitFor(() => expect(routeur.state.location.pathname).toBe(route));
      expect(routeur.state.historyAction).toBe('REPLACE');
      unmount();
    }
  });

  it.each([
    ['illisible', 'illisible'],
    ['sans rôle connu', jetonDeTest({ roles: ['INVITE'] })],
  ])('[FR-030-01] avec un jeton %s, /tableau-de-bord mène à /login et le jeton est supprimé', async (_cas, jeton) => {
    localStorage.setItem('nb.jeton', jeton);
    const { routeur } = rendreApplication('/tableau-de-bord');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(routeur.state.historyAction).toBe('REPLACE');
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });

  it('supprime un jeton illisible et mène à /login', async () => {
    localStorage.setItem('nb.jeton', 'illisible');
    const { routeur } = rendreApplication('/');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });
});
