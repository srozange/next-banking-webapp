// @vitest-environment jsdom
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import axe from 'axe-core';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
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

  it('[axe] profil en attente (aria-busy) ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CONSEILLER'] }));
    rendreApplication('/espace-conseiller', undefined, () => new Promise(() => {}));

    expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBe('true');
    expect(await reglesViolees()).toEqual([]);
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
