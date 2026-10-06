// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ReponseProfil } from '../../application/chargerProfil';
import { jetonDeTest } from '../jeton/jetonDeTest';
import { rendreApplication } from '../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const LIENS_COMMUNS: ReadonlyArray<readonly [testId: string, texte: string, route: string]> = [
  ['nav-tableau-de-bord', 'Tableau de bord', '/tableau-de-bord'],
  ['nav-virement', 'Virement', '/virement'],
  ['nav-virements-programmes', 'Virements programmes', '/virements-programmes'],
  ['nav-releve', 'Releve', '/releve'],
  ['nav-budget', 'Budget', '/budget'],
  ['nav-prets', 'Prets', '/prets'],
  ['nav-documents', 'Documents', '/documents'],
  ['nav-bourse', 'Bourse', '/bourse'],
  ['nav-mon-conseiller', 'Mon Conseiller', '/mon-conseiller'],
  ['nav-produits', 'Produits', '/produits'],
  ['nav-comptes-tiers', 'Comptes Tiers', '/comptes-tiers'],
  ['nav-beneficiaires', 'Beneficiaires', '/beneficiaires'],
];
const ESPACE_CONSEILLER = ['nav-espace-conseiller', 'Espace Conseiller', '/espace-conseiller'] as const;
const ADMINISTRATION = ['nav-administration', 'Administration', '/administration'] as const;

function connecter(role: string) {
  localStorage.setItem('nb.jeton', jetonDeTest({ sub: '1', roles: [role] }));
}

/** Liens de l'en-tête : [data-testid, texte, href], dans l'ordre du document. */
function liensEnTete() {
  const navigation = within(screen.getByRole('banner')).getAllByRole('navigation')[0] as HTMLElement;
  return within(navigation)
    .getAllByRole('link')
    .map((lien) => [lien.dataset.testid, lien.textContent, lien.getAttribute('href')]);
}

/** Liens du sous-en-tête, ou [] s'il est absent. */
function liensSousEnTete() {
  return [...document.querySelectorAll<HTMLElement>('[data-testid^="nav-sous-"]')].map((lien) => [
    lien.dataset.testid,
    lien.textContent,
    lien.getAttribute('href'),
  ]);
}

async function attendreProfil() {
  await waitFor(() => expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBeNull());
}

describe('gabarit authentifié', () => {
  it('[FR-030-04] CLIENT : 12 liens dans l’ordre, sans badge ni sous-en-tête, lien courant marqué', async () => {
    connecter('CLIENT');
    rendreApplication('/tableau-de-bord');
    await attendreProfil();

    expect(liensEnTete()).toEqual(LIENS_COMMUNS);
    expect(screen.queryByTestId('nav-badge-role')).toBeNull();
    expect(liensSousEnTete()).toEqual([]);
    expect(screen.getAllByRole('navigation')).toHaveLength(1);
    expect(screen.getByTestId('nav-tableau-de-bord').getAttribute('aria-current')).toBe('page');
    expect(screen.getByTestId('nav-virement').getAttribute('aria-current')).toBeNull();
  });

  it('[FR-030-04] CONSEILLER : 13 liens, badge « Conseiller », sous-en-tête Mon portefeuille · Conseiller IA', async () => {
    connecter('CONSEILLER');
    rendreApplication('/espace-conseiller');
    await attendreProfil();

    expect(liensEnTete()).toEqual([...LIENS_COMMUNS, ESPACE_CONSEILLER]);
    expect(screen.getByTestId('nav-badge-role').textContent).toBe('Conseiller');
    expect(liensSousEnTete()).toEqual([
      ['nav-sous-mon-portefeuille', 'Mon portefeuille', '/espace-conseiller'],
      ['nav-sous-conseiller-ia', 'Conseiller IA', '/mon-conseiller'],
    ]);
    expect(screen.getByTestId('nav-sous-mon-portefeuille').getAttribute('aria-current')).toBe('page');
    expect(screen.getByTestId('nav-espace-conseiller').getAttribute('aria-current')).toBe('page');
    expect(screen.getByRole('navigation', { name: 'Espace Conseiller' })).toBeTruthy();
  });

  it('[FR-030-04] ADMIN : 14 liens, badge « ADMIN », sous-en-tête Administration · Espace Conseiller · Mon tableau de bord', async () => {
    connecter('ADMIN');
    rendreApplication('/administration');
    await attendreProfil();

    expect(liensEnTete()).toEqual([...LIENS_COMMUNS, ESPACE_CONSEILLER, ADMINISTRATION]);
    expect(screen.getByTestId('nav-badge-role').textContent).toBe('ADMIN');
    expect(liensSousEnTete()).toEqual([
      ['nav-sous-administration', 'Administration', '/administration'],
      ['nav-sous-espace-conseiller', 'Espace Conseiller', '/espace-conseiller'],
      ['nav-sous-mon-tableau-de-bord', 'Mon tableau de bord', '/tableau-de-bord'],
    ]);
    expect(screen.getByTestId('nav-sous-administration').getAttribute('aria-current')).toBe('page');
    expect(screen.getByRole('navigation', { name: 'Administration' })).toBeTruthy();
  });

  it('[FR-030-02] ADMIN ouvre /espace-conseiller sans redirection', async () => {
    connecter('ADMIN');
    const { routeur } = rendreApplication('/espace-conseiller');
    await attendreProfil();

    expect(routeur.state.location.pathname).toBe('/espace-conseiller');
    expect(liensSousEnTete().map(([testId]) => testId)).toEqual(['nav-sous-mon-portefeuille', 'nav-sous-conseiller-ia']);
  });

  it('[FR-030-04] data-testid uniques, tous préfixés nav-, et aucun texte en dehors des textes attendus', async () => {
    connecter('ADMIN');
    rendreApplication('/administration');
    await attendreProfil();

    const testIds = [...screen.getByRole('banner').querySelectorAll<HTMLElement>('[data-testid]')].map(
      (element) => element.dataset.testid as string,
    );
    expect(new Set(testIds).size).toBe(testIds.length);
    expect(testIds.every((testId) => testId.startsWith('nav-'))).toBe(true);
    expect(testIds).toEqual(expect.arrayContaining(['nav-badge-role', 'nav-salutation', 'nav-deconnexion']));

    const textes = [...screen.getByRole('banner').querySelectorAll('span, a, button')]
      .filter((element) => element.children.length === 0)
      .map((element) => element.textContent);
    expect(textes).toEqual([
      'DB',
      'Demo Bank',
      ...[...LIENS_COMMUNS, ESPACE_CONSEILLER, ADMINISTRATION].map(([, texte]) => texte),
      'ADMIN',
      'Bonjour, Jean Dupont',
      'Deconnexion',
      'Administration',
      'Espace Conseiller',
      'Mon tableau de bord',
    ]);
  });

  it('[FR-001-05] salue l’utilisateur avec le nom complet lu par /moi, avec le jeton de la session', async () => {
    connecter('CLIENT');
    const jeton = localStorage.getItem('nb.jeton');
    const { lire } = rendreApplication('/tableau-de-bord');

    expect((await screen.findByTestId('nav-salutation')).textContent).toBe('Bonjour, Jean Dupont');
    expect(lire).toHaveBeenCalledWith(jeton);
  });

  it('[FR-001-05] profil en attente : contenu aria-busy, sans salutation ni texte de chargement', () => {
    connecter('CLIENT');
    rendreApplication('/tableau-de-bord', undefined, () => new Promise<ReponseProfil>(() => {}));

    const contenu = screen.getByTestId('application-racine');
    expect(contenu.getAttribute('aria-busy')).toBe('true');
    expect(contenu.textContent).not.toMatch(/chargement/i);
    expect(screen.queryByTestId('nav-salutation')).toBeNull();
    expect(screen.getByTestId('nav-deconnexion')).toBeTruthy();
  });

  it('[FR-001-05] profil refusé (401) : jeton supprimé, /login par remplacement, sans message', async () => {
    connecter('CLIENT');
    const { routeur } = rendreApplication('/tableau-de-bord', undefined, () =>
      Promise.resolve<ReponseProfil>({ statut: 'nonAuthentifie' }),
    );

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(routeur.state.historyAction).toBe('REPLACE');
    expect(localStorage.getItem('nb.jeton')).toBeNull();
    expect(await screen.findByTestId('login-identifiant')).toBeTruthy();
    expect(screen.queryByTestId('login-message-erreur')).toBeNull();
  });

  it.each<[string, () => Promise<ReponseProfil>]>([
    ['technique', () => Promise.resolve({ statut: 'technique' })],
    ['exception', () => Promise.reject(new Error('réseau'))],
  ])('[FR-001-05] profil en panne (%s) : aria-busy levé, en-tête sans salutation, aucun texte affiché', async (_cas, lire) => {
    connecter('CLIENT');
    const { routeur } = rendreApplication('/tableau-de-bord', undefined, lire);
    await attendreProfil();

    expect(screen.queryByTestId('nav-salutation')).toBeNull();
    expect(screen.queryByRole('alert')).toBeNull();
    expect(screen.getByTestId('application-racine').textContent).not.toMatch(/chargement|erreur/i);
    expect(routeur.state.location.pathname).toBe('/tableau-de-bord');
    expect(localStorage.getItem('nb.jeton')).not.toBeNull();
  });

  it('[FR-030-04] Deconnexion est un bouton, seul bouton de l’en-tête : aucun menu repliable', async () => {
    connecter('CLIENT');
    rendreApplication('/tableau-de-bord');
    await attendreProfil();

    const boutons = within(screen.getByRole('banner')).getAllByRole('button');
    expect(boutons).toEqual([screen.getByTestId('nav-deconnexion')]);
    const deconnexion = boutons[0] as HTMLButtonElement;
    expect(deconnexion.type).toBe('button');
    expect(deconnexion.textContent).toBe('Deconnexion');
    expect(document.querySelector('[aria-expanded], [aria-haspopup]')).toBeNull();
  });

  it('[FR-001-10] un clic sur Deconnexion supprime le jeton, informe le service et mène à /login sans message', async () => {
    connecter('CLIENT');
    const jeton = localStorage.getItem('nb.jeton');
    const utilisateur = userEvent.setup();
    const { routeur, deconnecter } = rendreApplication('/tableau-de-bord');
    await attendreProfil();

    await utilisateur.click(screen.getByTestId('nav-deconnexion'));

    expect(localStorage.getItem('nb.jeton')).toBeNull();
    expect(deconnecter).toHaveBeenCalledExactlyOnceWith(jeton);
    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(routeur.state.historyAction).toBe('REPLACE');
    expect(await screen.findByTestId('login-identifiant')).toBeTruthy();
    expect(screen.queryByTestId('login-message-erreur')).toBeNull();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('[FR-001-10] service injoignable à la déconnexion : jeton supprimé et /login quand même', async () => {
    connecter('CLIENT');
    const utilisateur = userEvent.setup();
    const { routeur } = rendreApplication('/tableau-de-bord', undefined, undefined, undefined, {
      deconnecter: () => Promise.reject(new TypeError('Failed to fetch')),
    });
    await attendreProfil();

    await utilisateur.click(screen.getByTestId('nav-deconnexion'));

    expect(localStorage.getItem('nb.jeton')).toBeNull();
    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-030-05] tout 401 d’un service : jeton supprimé et /login sans message', async () => {
    connecter('CLIENT');
    const { routeur, signal } = rendreApplication('/tableau-de-bord');
    await attendreProfil();

    localStorage.removeItem('nb.jeton');
    signal.declencher();

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(routeur.state.historyAction).toBe('REPLACE');
    expect(screen.queryByRole('alert')).toBeNull();
  });

  describe('renouvellement sur activité', () => {
    const MAINTENANT_MS = Date.parse('2026-10-06T10:00:00Z');
    const horloge = { maintenant: () => MAINTENANT_MS };

    function jetonExpirant(secondesRestantes: number) {
      return jetonDeTest({ sub: '1', roles: ['CLIENT'], exp: MAINTENANT_MS / 1000 + secondesRestantes });
    }

    it('[FR-030-05] moins de 600 s restantes : un clic appelle le renouvellement et remplace le jeton', async () => {
      const ancien = jetonExpirant(599);
      const neuf = jetonExpirant(1800);
      localStorage.setItem('nb.jeton', ancien);
      const utilisateur = userEvent.setup();
      const { renouveler } = rendreApplication('/tableau-de-bord', undefined, undefined, horloge, {
        renouveler: () => Promise.resolve({ statut: 'renouvelee', jeton: neuf }),
      });
      await attendreProfil();

      await utilisateur.click(document.body);

      await waitFor(() => expect(localStorage.getItem('nb.jeton')).toBe(neuf));
      expect(renouveler).toHaveBeenCalledExactlyOnceWith(ancien);
    });

    it('[FR-030-05] 600 s restantes ou plus : aucun appel', async () => {
      localStorage.setItem('nb.jeton', jetonExpirant(600));
      const utilisateur = userEvent.setup();
      const { renouveler } = rendreApplication('/tableau-de-bord', undefined, undefined, horloge);
      await attendreProfil();

      await utilisateur.click(document.body);
      await utilisateur.keyboard('a');

      expect(renouveler).not.toHaveBeenCalled();
    });

    it('[FR-030-05] une frappe est aussi une activité, sans appels en double pendant un renouvellement', async () => {
      localStorage.setItem('nb.jeton', jetonExpirant(10));
      const utilisateur = userEvent.setup();
      let terminer: () => void = () => {};
      const { renouveler } = rendreApplication('/tableau-de-bord', undefined, undefined, horloge, {
        renouveler: () =>
          new Promise((resolve) => {
            terminer = () => resolve({ statut: 'technique' });
          }),
      });
      await attendreProfil();

      await utilisateur.keyboard('a');
      await utilisateur.keyboard('b');
      expect(renouveler).toHaveBeenCalledOnce();
      terminer();
      await waitFor(() => expect(renouveler).toHaveBeenCalledOnce());
      await utilisateur.keyboard('c');
      expect(renouveler).toHaveBeenCalledTimes(2);
    });

    it('[FR-030-05] client désactivé (401 au renouvellement) : jeton supprimé, /login sans message', async () => {
      localStorage.setItem('nb.jeton', jetonExpirant(30));
      const utilisateur = userEvent.setup();
      const { routeur } = rendreApplication('/tableau-de-bord', undefined, undefined, horloge, {
        renouveler: () => Promise.resolve({ statut: 'nonAuthentifie' }),
      });
      await attendreProfil();

      await utilisateur.click(document.body);

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
      expect(localStorage.getItem('nb.jeton')).toBeNull();
      expect(screen.queryByRole('alert')).toBeNull();
    });
  });

  it('[FR-030-04] l’en-tête précède le contenu principal et suit le lien d’évitement', async () => {
    connecter('CLIENT');
    const { container } = rendreApplication('/tableau-de-bord');
    await attendreProfil();

    expect([...container.children].map((element) => element.tagName)).toEqual(['A', 'HEADER', 'MAIN']);
  });

  it('[FR-030-04] un lien d’en-tête change de page sans recharger le profil', async () => {
    connecter('ADMIN');
    const utilisateur = userEvent.setup();
    const { routeur, lire } = rendreApplication('/administration');
    await attendreProfil();

    await utilisateur.click(screen.getByTestId('nav-sous-espace-conseiller'));
    expect(routeur.state.location.pathname).toBe('/espace-conseiller');
    expect(screen.getByTestId('nav-sous-mon-portefeuille').getAttribute('aria-current')).toBe('page');
    expect(lire).toHaveBeenCalledOnce();
  });
});
