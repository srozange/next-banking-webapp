// @vitest-environment jsdom
import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ReponseConnexion } from '../../../application/seConnecter';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
  sessionStorage.clear();
});

const champ = (testId: string) => screen.getByTestId(testId) as HTMLInputElement;

describe('écran de connexion', () => {
  it('[FR-001-01] affiche l’onglet, les textes du legacy et un vrai formulaire', async () => {
    const { container } = rendreApplication('/login');

    await waitFor(() => expect(document.title).toBe('BankApp Demo - Connexion'));
    const texte = container.textContent ?? '';
    for (const attendu of ['DB', 'Demo Bank', 'Espace client — Connexion sécurisée', 'Identifiant', 'Mot de passe', 'Me connecter']) {
      expect(texte).toContain(attendu);
    }
    expect(texte).not.toContain('Système Legacy');

    const identifiant = champ('login-identifiant');
    const motDePasse = champ('login-mot-de-passe');
    expect(screen.getByLabelText('Identifiant')).toBe(identifiant);
    expect(screen.getByLabelText('Mot de passe')).toBe(motDePasse);
    expect(container.querySelector('label[for="login-identifiant"]')?.textContent).toBe('Identifiant');
    expect(container.querySelector('label[for="login-mot-de-passe"]')?.textContent).toBe('Mot de passe');
    expect(identifiant.form).toBeInstanceOf(HTMLFormElement);
    expect(identifiant.form?.noValidate).toBe(true);
    expect(container.querySelector('input[type="text"]')).toBe(identifiant);
    expect(identifiant.name).toBe('username');
    expect(container.querySelectorAll('input[type="password"]')).toHaveLength(1);
    expect(motDePasse.name).toBe('password');
    const bouton = screen.getByTestId('login-me-connecter') as HTMLButtonElement;
    expect(bouton.type).toBe('submit');
    expect(bouton.textContent).toBe('Me connecter');
    expect(identifiant.getAttribute('aria-required')).toBe('true');
    expect(screen.queryByTestId('login-message-erreur')).toBeNull();
  });

  it('[FR-001-03] signale les deux champs vides par Entrée, focus sur le premier, sans appel API', async () => {
    const utilisateur = userEvent.setup();
    const { connecter } = rendreApplication('/login');

    await utilisateur.type(champ('login-identifiant'), '   {Enter}');

    const identifiant = champ('login-identifiant');
    const motDePasse = champ('login-mot-de-passe');
    expect(screen.getByText('Identifiant requis').id).toBe('login-identifiant-erreur');
    expect(screen.getByText('Mot de passe requis').id).toBe('login-mot-de-passe-erreur');
    expect(identifiant.getAttribute('aria-invalid')).toBe('true');
    expect(identifiant.getAttribute('aria-describedby')).toBe('login-identifiant-erreur');
    expect(motDePasse.getAttribute('aria-invalid')).toBe('true');
    expect(motDePasse.getAttribute('aria-describedby')).toBe('login-mot-de-passe-erreur');
    expect(document.activeElement).toBe(identifiant);
    expect(connecter).not.toHaveBeenCalled();
  });

  it('[FR-001-03] signale le seul mot de passe vide et y place le focus', async () => {
    const utilisateur = userEvent.setup();
    const { connecter } = rendreApplication('/login');

    await utilisateur.type(champ('login-identifiant'), 'user1{Enter}');

    expect(screen.queryByText('Identifiant requis')).toBeNull();
    expect(screen.getByText('Mot de passe requis')).toBeTruthy();
    expect(champ('login-identifiant').hasAttribute('aria-invalid')).toBe(false);
    expect(document.activeElement).toBe(champ('login-mot-de-passe'));
    expect(connecter).not.toHaveBeenCalled();
  });

  it('[FR-001-06] reste sur /login au refus, affiche les deux lignes, garde les valeurs et focalise le message', async () => {
    const utilisateur = userEvent.setup();
    const { routeur, connecter } = rendreApplication('/login');

    await utilisateur.type(champ('login-identifiant'), 'user1');
    await utilisateur.type(champ('login-mot-de-passe'), 'faux{Enter}');

    const message = await screen.findByTestId('login-message-erreur');
    expect(message.getAttribute('role')).toBe('alert');
    expect([...message.querySelectorAll('p')].map((p) => p.textContent)).toEqual([
      'Identifiants incorrects',
      'Vérifiez votre login et mot de passe',
    ]);
    await waitFor(() => expect(document.activeElement).toBe(message));
    expect(connecter).toHaveBeenCalledWith('user1', 'faux');
    expect(routeur.state.location.pathname).toBe('/login');
    expect(champ('login-identifiant').value).toBe('user1');
    expect(champ('login-mot-de-passe').value).toBe('faux');
    expect(localStorage.getItem('nb.jeton')).toBeNull();

    // Une deuxième soumission refusée reprend le focus sur le message.
    await utilisateur.click(champ('login-mot-de-passe'));
    await utilisateur.keyboard('{Enter}');
    await waitFor(() => expect(connecter).toHaveBeenCalledTimes(2));
    await waitFor(() => expect(document.activeElement).toBe(screen.getByTestId('login-message-erreur')));
  });

  it('[FR-001-06] efface les erreurs de champ au refus et le message à l’erreur de champ suivante', async () => {
    const utilisateur = userEvent.setup();
    rendreApplication('/login');

    await utilisateur.type(champ('login-identifiant'), 'user1{Enter}');
    expect(screen.getByText('Mot de passe requis')).toBeTruthy();
    await utilisateur.type(champ('login-mot-de-passe'), 'faux{Enter}');
    await screen.findByTestId('login-message-erreur');
    expect(screen.queryByText('Mot de passe requis')).toBeNull();

    await utilisateur.clear(champ('login-mot-de-passe'));
    await utilisateur.keyboard('{Enter}');
    expect(screen.getByText('Mot de passe requis')).toBeTruthy();
    expect(screen.queryByTestId('login-message-erreur')).toBeNull();
  });

  it('affiche seulement « Une erreur technique est survenue » quand l’API est en panne', async () => {
    const utilisateur = userEvent.setup();
    rendreApplication('/login', () => Promise.resolve({ statut: 'technique' }));

    await utilisateur.type(champ('login-identifiant'), 'user1');
    await utilisateur.type(champ('login-mot-de-passe'), 'secret{Enter}');

    const message = await screen.findByTestId('login-message-erreur');
    expect(message.textContent).toBe('Une erreur technique est survenue');
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });

  it('désactive le bouton avec aria-busy pendant l’envoi et n’envoie qu’une requête', async () => {
    const utilisateur = userEvent.setup();
    let repondre: (reponse: ReponseConnexion) => void = () => {};
    const { connecter } = rendreApplication(
      '/login',
      () => new Promise<ReponseConnexion>((resoudre) => (repondre = resoudre)),
    );

    await utilisateur.type(champ('login-identifiant'), 'user1');
    await utilisateur.type(champ('login-mot-de-passe'), 'secret{Enter}');
    const bouton = screen.getByTestId('login-me-connecter') as HTMLButtonElement;
    await waitFor(() => expect(bouton.disabled).toBe(true));
    expect(bouton.getAttribute('aria-busy')).toBe('true');

    await utilisateur.keyboard('{Enter}');
    await utilisateur.click(bouton);
    expect(connecter).toHaveBeenCalledOnce();

    repondre({ statut: 'refusee' });
    await screen.findByTestId('login-message-erreur');
    expect(bouton.disabled).toBe(false);
    expect(bouton.hasAttribute('aria-busy')).toBe(false);
  });

  it.each([
    ['admin', 'ADMIN', '/administration'],
    ['sophie', 'CONSEILLER', '/espace-conseiller'],
    ['user1', 'CLIENT', '/tableau-de-bord'],
  ])(
    '[FR-001-04][FR-001-05][FR-030-03] %s (%s) : jeton dans localStorage nb.jeton, arrivée sur %s',
    async (login, role, route) => {
      const utilisateur = userEvent.setup();
      const jeton = jetonDeTest({ sub: '1', roles: [role] });
      const { routeur, connecter } = rendreApplication('/login', () =>
        Promise.resolve({ statut: 'acceptee', jeton, roles: [role] }),
      );

      await utilisateur.type(champ('login-identifiant'), login);
      await utilisateur.type(champ('login-mot-de-passe'), 'secret{Enter}');

      await waitFor(() => expect(routeur.state.location.pathname).toBe(route));
      expect(routeur.state.historyAction).toBe('REPLACE');
      expect(connecter).toHaveBeenCalledWith(login, 'secret');
      expect(localStorage.getItem('nb.jeton')).toBe(jeton);
      expect(sessionStorage.length).toBe(0);
      // Le routeur change d'état avant que React n'ait rendu la page suivante (en-tête compris).
      await waitFor(() => expect(screen.queryByTestId('login-identifiant')).toBeNull());
    },
  );

  it('[FR-001-05] envoie au clic de la souris aussi', async () => {
    const utilisateur = userEvent.setup();
    const jeton = jetonDeTest({ roles: ['CLIENT'] });
    const { routeur } = rendreApplication('/login', () => Promise.resolve({ statut: 'acceptee', jeton, roles: ['CLIENT'] }));

    await utilisateur.type(champ('login-identifiant'), 'user1');
    await utilisateur.type(champ('login-mot-de-passe'), 'secret');
    await utilisateur.click(screen.getByTestId('login-me-connecter'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/tableau-de-bord'));
  });

  it('se remplit et se soumet au seul clavier (Tab, Entrée)', async () => {
    const utilisateur = userEvent.setup();
    const { connecter } = rendreApplication('/login');

    await utilisateur.tab();
    expect(document.activeElement).toBe(screen.getByTestId('application-lien-evitement'));
    await utilisateur.tab();
    expect(document.activeElement).toBe(champ('login-identifiant'));
    await utilisateur.keyboard('user1');
    await utilisateur.tab();
    expect(document.activeElement).toBe(champ('login-mot-de-passe'));
    await utilisateur.keyboard('secret');
    await utilisateur.tab();
    expect(document.activeElement).toBe(screen.getByTestId('login-me-connecter'));
    await utilisateur.keyboard('{Enter}');

    await screen.findByTestId('login-message-erreur');
    expect(connecter).toHaveBeenCalledWith('user1', 'secret');
  });
});
