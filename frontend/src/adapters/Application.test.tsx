// @vitest-environment jsdom
import { cleanup, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { jetonDeTest } from './jeton/jetonDeTest';
import { rendreApplication } from './test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

describe('Application', () => {
  it('garde le data-testid figé application-racine sur le contenu principal', () => {
    rendreApplication('/login');

    const racine = screen.getByTestId('application-racine');
    expect(racine.tagName).toBe('MAIN');
    expect(racine.id).toBe('contenu-principal');
    expect(racine.getAttribute('tabindex')).toBe('-1');
  });

  it.each([
    ['/login', null],
    ['/tableau-de-bord', 'CLIENT'],
  ])('sur %s, le premier Tab donne le focus au lien d’évitement, qui mène au contenu principal', async (chemin, role) => {
    if (role !== null) {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
    }
    const utilisateur = userEvent.setup();
    const { container } = rendreApplication(chemin);

    const lien = screen.getByTestId('application-lien-evitement');
    expect(lien.textContent).toBe('Aller au contenu principal');
    expect(lien.getAttribute('href')).toBe('#contenu-principal');
    expect(container.firstElementChild).toBe(lien);

    await utilisateur.tab();
    expect(document.activeElement).toBe(lien);

    await utilisateur.keyboard('{Enter}');
    expect(document.activeElement).toBe(screen.getByTestId('application-racine'));
  });
});
