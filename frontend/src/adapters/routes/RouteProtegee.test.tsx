// @vitest-environment jsdom
import { cleanup, render, waitFor } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { decodeurJetonJwt } from '../jeton/DecodeurJetonJwt';
import { jetonDeTest } from '../jeton/jetonDeTest';
import { creerExpirationJeton } from '../session/ExpirationJetonJwt';
import { horlogeSysteme } from '../session/horloge';
import { creerSessionLocale } from '../session/SessionLocaleJwt';
import { creerSignalExpiration } from '../session/signalExpiration';
import { creerStockageJetonLocal } from '../stockage/StockageJetonLocal';
import { rendreApplication } from '../test/rendreApplication';
import { RouteProtegee } from './RouteProtegee';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

/** Garde réelle devant des pages sentinelles, pour prouver que le contenu refusé n'est jamais rendu. */
function rendreGarde(chemin: string) {
  const pages = {
    tableauDeBord: vi.fn(() => <p>tableau</p>),
    espaceConseiller: vi.fn(() => <p>conseiller</p>),
    administration: vi.fn(() => <p>administration</p>),
  };
  const { tableauDeBord: TableauDeBord, espaceConseiller: EspaceConseiller, administration: Administration } = pages;
  const routeur = createMemoryRouter(
    [
      {
        element: (
          <RouteProtegee
            clientProfil={{ lire: () => Promise.resolve({ statut: 'profil', nomComplet: 'Jean Dupont' }) }}
            session={{
              client: { deconnecter: () => Promise.resolve(), renouveler: () => Promise.resolve({ statut: 'technique' }) },
              locale: creerSessionLocale(creerStockageJetonLocal(), decodeurJetonJwt, creerExpirationJeton(horlogeSysteme)),
              signal: creerSignalExpiration(),
            }}
          />
        ),
        children: [
          { path: '/tableau-de-bord', element: <TableauDeBord /> },
          { path: '/espace-conseiller', element: <EspaceConseiller /> },
          { path: '/administration', element: <Administration /> },
        ],
      },
      { path: '/login', element: <p>connexion</p> },
    ],
    { initialEntries: [chemin] },
  );
  render(<RouterProvider router={routeur} />);
  return { routeur, pages };
}

describe('RouteProtegee', () => {
  it.each([
    ['CLIENT', '/administration', 'administration'],
    ['CLIENT', '/espace-conseiller', 'espaceConseiller'],
    ['CONSEILLER', '/administration', 'administration'],
  ] as const)(
    '[FR-001-11][FR-016-01][FR-030-02] %s sur %s : remplacement vers /tableau-de-bord, contenu jamais rendu',
    async (role, chemin, page) => {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
      const { routeur, pages } = rendreGarde(chemin);

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/tableau-de-bord'));
      expect(routeur.state.historyAction).toBe('REPLACE');
      expect(pages[page]).not.toHaveBeenCalled();
      expect(pages.tableauDeBord).toHaveBeenCalled();
    },
  );

  it.each([
    ['CONSEILLER', '/espace-conseiller', 'espaceConseiller'],
    ['ADMIN', '/espace-conseiller', 'espaceConseiller'],
    ['ADMIN', '/administration', 'administration'],
    ['CLIENT', '/tableau-de-bord', 'tableauDeBord'],
  ] as const)('[FR-030-02] %s ouvre %s', (role, chemin, page) => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
    const { routeur, pages } = rendreGarde(chemin);

    expect(routeur.state.location.pathname).toBe(chemin);
    expect(pages[page]).toHaveBeenCalled();
  });

  it('[FR-001-11][FR-016-01] dans l’application, la redirection se fait sans message', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    const { routeur } = rendreApplication('/administration');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/tableau-de-bord'));
    expect(routeur.state.historyAction).toBe('REPLACE');
    expect(document.querySelector('[role="alert"]')).toBeNull();
    expect(document.querySelector('[data-testid^="nav-sous-"]')).toBeNull();
  });
});
