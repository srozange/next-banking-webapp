// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import type { ClientConversation, ReponseConversation } from '../../../application/dialoguerAvecSophie';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

beforeAll(() => {
  // jsdom ne calcule pas les hauteurs : le défilement se vérifie par la valeur posée sur scrollTop.
  Object.defineProperty(HTMLElement.prototype, 'scrollHeight', { configurable: true, get: () => 1234 });
});

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const BIENVENUE = (prenom: string) =>
  `Bonjour ${prenom} ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ?`;
const REPLI = 'Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence.';

type Envoyer = ClientConversation['envoyer'];

function ouvrir(role = 'CLIENT', envoyer: Envoyer = () => Promise.resolve<ReponseConversation>({ statut: 'ok', reponse: 'Réponse de Sophie' }), profil?: Parameters<typeof rendreApplication>[2]) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
  return rendreApplication('/mon-conseiller', undefined, profil, undefined, undefined, undefined, undefined, { envoyer });
}

const ecran = () => screen.getByTestId('chat-contenu');
const sophie = () => screen.getAllByTestId('chat-message-sophie');
const utilisateur = () => screen.queryAllByTestId('chat-message-utilisateur');
const saisie = () => screen.getByTestId<HTMLTextAreaElement>('chat-saisie');
const actions = ['optimiser-epargne', 'projet-immobilier', 'investir-bourse', 'preparer-retraite'];

describe('EcranConseiller', () => {
  describe('[FR-010-01]', () => {
    it.each(['CLIENT', 'CONSEILLER', 'ADMIN'])('%s ouvre l’écran de Sophie, avec son titre d’onglet', async (role) => {
      ouvrir(role);

      expect(await screen.findByText('Sophie — Conseillère Demo Bank')).toBeTruthy();
      expect(screen.getByText('En ligne · Répond en quelques secondes')).toBeTruthy();
      expect(screen.getByTestId('chat-nouvelle-conversation').textContent).toBe('Nouvelle conversation');
      await waitFor(() => expect(document.title).toBe('BankApp Demo - Mon Conseiller'));
      expect(screen.queryByText(/Conseiller IA/)).toBeNull();
    });

    it('sans jeton, renvoie vers /login', async () => {
      const { routeur } = rendreApplication('/mon-conseiller');

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    });

    it('un profil refusé (401) renvoie vers /login', async () => {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
      const { routeur } = rendreApplication('/mon-conseiller', undefined, () => Promise.resolve({ statut: 'nonAuthentifie' }));

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
      expect(localStorage.getItem('nb.jeton')).toBeNull();
    });
  });

  describe('[FR-010-02]', () => {
    it('salue par le prénom du profil déjà chargé, sans second appel au profil', async () => {
      const { lire } = ouvrir();

      await screen.findByText(BIENVENUE('Jean'));
      expect(lire).toHaveBeenCalledTimes(1);
      expect(sophie()).toHaveLength(1);
      expect(screen.getAllByRole('button').filter((b) => b.className === 'bouton-fantome')).toHaveLength(4);
    });

    it('salue « cher client » quand le profil est indisponible', async () => {
      ouvrir('CLIENT', undefined, () => Promise.resolve({ statut: 'technique' }));

      expect(await screen.findByText(BIENVENUE('cher client'))).toBeTruthy();
    });

    it('n’affiche rien avant la résolution du profil', async () => {
      ouvrir('CLIENT', undefined, () => new Promise(() => {}));

      expect(ecran().getAttribute('aria-busy')).toBe('true');
      expect(screen.queryByTestId('chat-messages')).toBeNull();
      expect(screen.queryByTestId('chat-saisie')).toBeNull();
    });
  });

  describe('[FR-010-03]', () => {
    it('une réouverture ne montre que la bienvenue', async () => {
      const envoyer = vi.fn<Envoyer>(() => Promise.resolve({ statut: 'ok', reponse: 'R' }));
      const { routeur } = ouvrir('CLIENT', envoyer);
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));
      await u.type(saisie(), 'Bonjour{Enter}');
      await screen.findByText('R');

      await routeur.navigate('/produits');
      await screen.findByTestId('produits-contenu');
      expect(screen.queryByTestId('chat-contenu')).toBeNull();
      await routeur.navigate('/mon-conseiller');

      await screen.findByText(BIENVENUE('Jean'));
      expect(sophie()).toHaveLength(1);
      expect(utilisateur()).toHaveLength(0);
      expect(saisie().value).toBe('');
    });
  });

  describe('[FR-010-04]', () => {
    it('une action rapide envoie son texte et masque les quatre boutons', async () => {
      const envoyer = vi.fn<Envoyer>(() => Promise.resolve({ statut: 'ok', reponse: 'Voici' }));
      ouvrir('CLIENT', envoyer);
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));
      expect(actions.every((a) => screen.queryByTestId(`chat-action-${a}`) !== null)).toBe(true);
      expect(screen.getByTestId('chat-action-projet-immobilier').textContent).toBe('Projet immobilier');

      await u.click(screen.getByTestId('chat-action-projet-immobilier'));

      await screen.findByText('Voici');
      expect(envoyer).toHaveBeenCalledWith(expect.any(String), 'Je souhaite acheter un appartement, comment preparer mon dossier ?', []);
      expect(utilisateur()[0]?.textContent).toBe('Je souhaite acheter un appartement, comment preparer mon dossier ?');
      expect(actions.some((a) => screen.queryByTestId(`chat-action-${a}`) !== null)).toBe(false);
    });
  });

  describe('[FR-010-05]', () => {
    it('envoie, garde la saisie, puis affiche la réponse de Sophie', async () => {
      const envoyer = vi.fn<Envoyer>(() => Promise.resolve({ statut: 'ok', reponse: 'Réponse de Sophie' }));
      ouvrir('CLIENT', envoyer);
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));

      await u.type(saisie(), 'Bonjour');
      await u.click(screen.getByTestId('chat-envoyer'));

      await screen.findByText('Réponse de Sophie');
      expect(utilisateur().map((b) => b.textContent)).toEqual(['Bonjour']);
      expect(envoyer).toHaveBeenCalledWith(expect.any(String), 'Bonjour', []);
      expect(saisie().value).toBe('Bonjour');
      expect(screen.getByTestId('chat-messages').scrollTop).toBe(1234);
    });

    it('ignore une saisie vide ou de blancs, au clic comme à Entrée', async () => {
      const envoyer = vi.fn<Envoyer>(() => Promise.resolve({ statut: 'ok', reponse: 'R' }));
      ouvrir('CLIENT', envoyer);
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));

      await u.click(screen.getByTestId('chat-envoyer'));
      await u.type(saisie(), '   {Enter}');
      await u.click(screen.getByTestId('chat-envoyer'));

      expect(envoyer).not.toHaveBeenCalled();
      expect(utilisateur()).toHaveLength(0);
      expect(sophie()).toHaveLength(1);
    });

    it('pendant l’attente, « Envoyer » est désactivé et affiche « ... », puis se rétablit', async () => {
      let liberer: (r: ReponseConversation) => void = () => {};
      const envoyer = vi.fn<Envoyer>(() => new Promise((resolu) => (liberer = resolu)));
      ouvrir('CLIENT', envoyer);
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));

      await u.type(saisie(), 'Bonjour{Enter}');

      const bouton = screen.getByTestId<HTMLButtonElement>('chat-envoyer');
      expect(bouton.disabled).toBe(true);
      expect(bouton.textContent).toBe('...');
      await u.type(saisie(), '{Enter}');
      expect(envoyer).toHaveBeenCalledTimes(1);

      liberer({ statut: 'ok', reponse: 'Fini' });
      await screen.findByText('Fini');
      expect(bouton.disabled).toBe(false);
      expect(bouton.textContent).toBe('Envoyer');
    });

    it('un 401 renvoie vers /login et supprime le jeton', async () => {
      const { routeur } = ouvrir('CLIENT', () => Promise.resolve({ statut: 'nonAuthentifie' }));
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));

      await u.type(saisie(), 'Bonjour{Enter}');

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
      expect(localStorage.getItem('nb.jeton')).toBeNull();
    });
  });

  describe('[FR-010-11]', () => {
    it('Entrée envoie, Maj+Entrée insère un saut de ligne', async () => {
      const envoyer = vi.fn<Envoyer>(() => Promise.resolve({ statut: 'ok', reponse: 'R' }));
      ouvrir('CLIENT', envoyer);
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));

      await u.type(saisie(), 'a{Shift>}{Enter}{/Shift}b');
      expect(envoyer).not.toHaveBeenCalled();
      expect(saisie().value).toBe('a\nb');

      await u.type(saisie(), '{Enter}');
      await screen.findByText('R');
      expect(envoyer).toHaveBeenCalledWith(expect.any(String), 'a\nb', []);
    });

    it('le champ a un nom accessible et la liste est annoncée poliment', async () => {
      ouvrir();
      await screen.findByText(BIENVENUE('Jean'));

      expect(screen.getByRole('textbox', { name: 'Posez votre question a Sophie...' })).toBe(saisie());
      expect(screen.getByTestId('chat-messages').getAttribute('aria-live')).toBe('polite');
    });
  });

  describe('[FR-010-06]', () => {
    it('le second envoi porte les deux premiers échanges, sans la bienvenue', async () => {
      let appels = 0;
      const envoyer = vi.fn<Envoyer>(() => Promise.resolve({ statut: 'ok', reponse: `R${++appels}` }));
      ouvrir('CLIENT', envoyer);
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));

      await u.type(saisie(), 'Un{Enter}');
      await screen.findByText('R1');
      await u.type(saisie(), '{Enter}');

      await screen.findByText('R2');
      expect(envoyer.mock.calls[0]?.[2]).toEqual([]);
      expect(envoyer.mock.calls[1]).toEqual([
        expect.any(String),
        'Un',
        [
          { role: 'utilisateur', contenu: 'Un' },
          { role: 'assistant', contenu: 'R1' },
        ],
      ]);
      expect(saisie().value).toBe('Un');
    });
  });

  describe('[FR-010-09]', () => {
    it('affiche le message du service (502) tel quel, dans une bulle de Sophie', async () => {
      const message = 'Je suis momentanement indisponible (HTTP 500). Veuillez reessayer.';
      ouvrir('CLIENT', () => Promise.resolve({ statut: 'indisponible', message }));
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));

      await u.type(saisie(), 'Bonjour{Enter}');

      const bulle = await screen.findByText(message);
      expect(within(screen.getByTestId('chat-messages')).getAllByTestId('chat-message-sophie').at(-1)?.contains(bulle)).toBe(true);
      expect(screen.queryByRole('alert')).toBeNull();
    });

    it.each([['réseau coupé', () => Promise.reject(new TypeError('Failed to fetch'))], ['technique', () => Promise.resolve<ReponseConversation>({ statut: 'technique' })]])(
      'sur panne (%s), Sophie répond le texte générique et l’envoi reste possible',
      async (_nom, envoyer) => {
        ouvrir('CLIENT', envoyer);
        const u = userEvent.setup();
        await screen.findByText(BIENVENUE('Jean'));

        await u.type(saisie(), 'Bonjour{Enter}');

        await screen.findByText(REPLI);
        expect((screen.getByTestId('chat-envoyer') as HTMLButtonElement).disabled).toBe(false);
      },
    );
  });

  describe('[BUG-038]', () => {
    it('un refus d’authentification de l’IA (503) n’affiche jamais « 401 »', async () => {
      ouvrir('CLIENT', () => Promise.resolve({ statut: 'indisponible', message: REPLI }));
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));

      await u.type(saisie(), 'Bonjour{Enter}');

      await screen.findByText(REPLI);
      expect(ecran().textContent).not.toMatch(/401/);
    });
  });

  describe('[FR-010-10]', () => {
    it('« Nouvelle conversation » efface les échanges, recalcule la bienvenue et réaffiche les actions', async () => {
      ouvrir();
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));
      await u.click(screen.getByTestId('chat-action-investir-bourse'));
      await screen.findByText('Réponse de Sophie');

      await u.click(screen.getByTestId('chat-nouvelle-conversation'));

      expect(utilisateur()).toHaveLength(0);
      expect(sophie()).toHaveLength(1);
      expect(screen.getByText(BIENVENUE('Jean'))).toBeTruthy();
      expect(actions.every((a) => screen.queryByTestId(`chat-action-${a}`) !== null)).toBe(true);
    });

    it('une réponse encore en vol est ignorée', async () => {
      let liberer: (r: ReponseConversation) => void = () => {};
      ouvrir('CLIENT', () => new Promise((resolu) => (liberer = resolu)));
      const u = userEvent.setup();
      await screen.findByText(BIENVENUE('Jean'));
      await u.type(saisie(), 'Bonjour{Enter}');

      await u.click(screen.getByTestId('chat-nouvelle-conversation'));
      liberer({ statut: 'ok', reponse: 'Trop tard' });
      await new Promise((resolu) => setTimeout(resolu, 0));

      expect(screen.queryByText('Trop tard')).toBeNull();
      expect(utilisateur()).toHaveLength(0);
      expect((screen.getByTestId('chat-envoyer') as HTMLButtonElement).disabled).toBe(false);
    });
  });
});
