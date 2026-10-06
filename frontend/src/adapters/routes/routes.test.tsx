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
  it('déclare les trois routes protégées', () => {
    expect(ROUTES_PROTEGEES).toEqual(['/tableau-de-bord', '/espace-conseiller', '/administration']);
  });

  it.each(['/', '/tableau-de-bord', '/espace-conseiller', '/administration', '/route-inconnue'])(
    '[FR-001-02][FR-030-01] sans jeton, %s mène à /login par remplacement',
    async (chemin) => {
      const { routeur } = rendreApplication(chemin);

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
      expect(routeur.state.historyAction).toBe('REPLACE');
      expect(await screen.findByTestId('login-identifiant')).toBeTruthy();
    },
  );

  it.each(ROUTES_PROTEGEES)('avec un jeton, %s reste affichée, sans contenu pour l’instant', async (chemin) => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
    const { routeur } = rendreApplication(chemin);

    expect(routeur.state.location.pathname).toBe(chemin);
    expect(screen.getByTestId('application-racine').textContent).toBe('');
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
