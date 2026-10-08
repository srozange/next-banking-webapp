// @vitest-environment jsdom
import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ReponseActionNotifications, ReponseNotifications } from '../../../application/gererNotifications';
import type { NotificationBrute } from '../../../domain/notifications';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const VIDE = 'Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel';
// De la plus récente à la plus ancienne, comme le service les rend.
const NOTIFICATIONS: readonly NotificationBrute[] = [
  { id: 3, type: 'FRAUDE', message: 'Virement suspendu', creeLe: '2026-11-17T11:00:00Z', lue: false },
  { id: 2, type: 'ALERTE', message: 'Virement important', creeLe: '2026-11-17T10:00:00Z', lue: true },
  { id: 1, type: 'CONFIRMATION', message: 'Virement confirmé', creeLe: '2026-07-01T10:00:00Z', lue: false },
];
const liste = (notifications: readonly NotificationBrute[]): Promise<ReponseNotifications> =>
  Promise.resolve({ statut: 'liste', notifications });
const effectuee = (): Promise<ReponseActionNotifications> => Promise.resolve({ statut: 'effectuee' });

function ouvrir(clientNotifications: Parameters<typeof rendreApplication>[14] = {}) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
  return rendreApplication('/tableau-de-bord', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, clientNotifications);
}

describe('section Centre de notifications', () => {
  it('[FR-002-07] [FR-025-05] affiche les notifications du client, de la plus récente à la plus ancienne, avec type, icône, message et horodatage', async () => {
    const { listerNotifications } = ouvrir({ lister: () => liste(NOTIFICATIONS) });

    expect((await screen.findByTestId('notifications-titre')).textContent).toBe('Centre de notifications');
    expect(listerNotifications).toHaveBeenCalledTimes(1);
    const lignes = screen.getAllByTestId(/^notifications-notification-\d+$/);
    expect(lignes.map((ligne) => ligne.getAttribute('data-testid'))).toEqual([
      'notifications-notification-3',
      'notifications-notification-2',
      'notifications-notification-1',
    ]);
    expect(screen.getByTestId('notifications-type-3').textContent).toBe('FRAUDE');
    expect(screen.getByTestId('notifications-type-2').textContent).toBe('ALERTE');
    expect(screen.getByTestId('notifications-type-1').textContent).toBe('CONFIRMATION');
    expect(lignes[0]?.textContent).toContain('🚨');
    expect(lignes[1]?.textContent).toContain('⚠️');
    expect(lignes[2]?.textContent).toContain('✅');
    expect(screen.getByTestId('notifications-message-3').textContent).toBe('Virement suspendu');
    expect(screen.getByTestId('notifications-horodatage-3').textContent).toBe('17/11/2026 12:00:00');
    expect(screen.getByTestId('notifications-horodatage-1').textContent).toBe('01/07/2026 12:00:00');
  });

  it('[FR-025-05] l’icône est décorative (aria-hidden) et le titre technique n’est pas affiché', async () => {
    ouvrir({ lister: () => liste(NOTIFICATIONS) });
    await screen.findByTestId('notifications-notification-3');

    const icones = document.querySelectorAll('.centre-notifications__icone');
    expect(icones).toHaveLength(3);
    icones.forEach((icone) => expect(icone.getAttribute('aria-hidden')).toBe('true'));
    expect(document.body.textContent).not.toContain('Notifications temps reel');
    expect(document.body.textContent).not.toContain('publie via ActiveMQ Artemis (MDB)');
  });

  it('[FR-002-07] seule une notification non lue porte la pastille « Non lu », avec son infobulle', async () => {
    ouvrir({ lister: () => liste(NOTIFICATIONS) });
    await screen.findByTestId('notifications-notification-3');

    for (const id of [3, 1]) {
      const pastille = screen.getByTestId(`notifications-non-lu-${id}`);
      expect(pastille.getAttribute('title')).toBe('Non lu');
      expect(pastille.getAttribute('aria-label')).toBe('Non lu');
    }
    expect(screen.queryByTestId('notifications-non-lu-2')).toBeNull();
  });

  it('[FR-002-08] avec des non lues : badge « {N} nouvelle(s) » dans une zone aria-live polite, « Tout marquer lu » et « Effacer tout »', async () => {
    ouvrir({ lister: () => liste(NOTIFICATIONS) });

    expect((await screen.findByTestId('notifications-badge')).textContent).toBe('2 nouvelle(s)');
    expect(screen.getByTestId('notifications-badge-zone').getAttribute('aria-live')).toBe('polite');
    expect(screen.getByTestId('notifications-tout-marquer-lu').textContent).toBe('Tout marquer lu');
    expect(screen.getByTestId('notifications-effacer-tout').textContent).toBe('Effacer tout');
    expect(screen.queryByTestId('notifications-vide')).toBeNull();
  });

  it('[FR-025-06] « Tout marquer lu » marque tout lu puis relit : le badge et le bouton disparaissent', async () => {
    const utilisateur = userEvent.setup();
    let lues = false;
    const { toutMarquerLu, listerNotifications } = ouvrir({
      lister: () => liste(NOTIFICATIONS.map((n) => ({ ...n, lue: lues || n.lue }))),
      toutMarquerLu: () => {
        lues = true;
        return effectuee();
      },
    });
    await utilisateur.click(await screen.findByTestId('notifications-tout-marquer-lu'));

    await waitFor(() => expect(screen.queryByTestId('notifications-badge')).toBeNull());
    expect(toutMarquerLu).toHaveBeenCalledTimes(1);
    expect(listerNotifications).toHaveBeenCalledTimes(2);
    expect(screen.queryByTestId('notifications-tout-marquer-lu')).toBeNull();
    expect(screen.queryByTestId(/^notifications-non-lu-/)).toBeNull();
    expect(screen.getByTestId('notifications-effacer-tout')).toBeTruthy();
    expect(screen.getAllByTestId(/^notifications-notification-\d+$/)).toHaveLength(3);
  });

  it('[FR-002-08] avec des notifications toutes lues : « Effacer tout » seul, sans badge', async () => {
    ouvrir({ lister: () => liste(NOTIFICATIONS.map((n) => ({ ...n, lue: true }))) });

    expect(await screen.findByTestId('notifications-effacer-tout')).toBeTruthy();
    expect(screen.queryByTestId('notifications-badge')).toBeNull();
    expect(screen.queryByTestId('notifications-tout-marquer-lu')).toBeNull();
  });

  it('[FR-025-07] « Effacer tout » efface puis relit : l’état vide apparaît, sans bouton ni badge', async () => {
    const utilisateur = userEvent.setup();
    let efface = false;
    const { toutEffacer } = ouvrir({
      lister: () => liste(efface ? [] : NOTIFICATIONS),
      toutEffacer: () => {
        efface = true;
        return effectuee();
      },
    });
    await utilisateur.click(await screen.findByTestId('notifications-effacer-tout'));

    expect((await screen.findByTestId('notifications-vide')).textContent).toBe(VIDE);
    expect(toutEffacer).toHaveBeenCalledTimes(1);
    expect(screen.queryByTestId('notifications-effacer-tout')).toBeNull();
    expect(screen.queryByTestId('notifications-tout-marquer-lu')).toBeNull();
    expect(screen.queryByTestId('notifications-badge')).toBeNull();
    expect(screen.queryByTestId(/^notifications-notification-\d+$/)).toBeNull();
  });

  it('[FR-002-07] sans notification : le message d’état vide exact, ni badge ni boutons', async () => {
    ouvrir();

    expect((await screen.findByTestId('notifications-vide')).textContent).toBe(VIDE);
    expect(screen.queryByTestId('notifications-badge')).toBeNull();
    expect(screen.queryByTestId('notifications-tout-marquer-lu')).toBeNull();
    expect(screen.queryByTestId('notifications-effacer-tout')).toBeNull();
  });

  it('[FR-002-07] erreur du service : le texte technique du legacy, sans trace ni état vide ni bouton', async () => {
    ouvrir({ lister: () => Promise.resolve({ statut: 'technique' }) });

    const erreur = await screen.findByTestId('notifications-message-erreur');
    expect(erreur.textContent).toBe('Une erreur technique est survenue');
    expect(erreur.getAttribute('role')).toBe('alert');
    expect(screen.queryByTestId('notifications-vide')).toBeNull();
    expect(screen.queryByTestId('notifications-effacer-tout')).toBeNull();
  });

  it('[FR-025-06] une action en échec affiche le texte technique', async () => {
    const utilisateur = userEvent.setup();
    ouvrir({ lister: () => liste(NOTIFICATIONS), toutMarquerLu: () => Promise.resolve({ statut: 'technique' }) });
    await utilisateur.click(await screen.findByTestId('notifications-tout-marquer-lu'));

    expect((await screen.findByTestId('notifications-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByTestId('notifications-badge')).toBeNull();
  });

  it('[FR-002-07] la section des notifications n’empêche ni n’attend le tableau de bord : elle s’affiche même si celui-ci est en erreur', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    rendreApplication('/tableau-de-bord', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { lire: () => Promise.resolve({ statut: 'technique' }) }, { lister: () => liste(NOTIFICATIONS) });

    expect(await screen.findByTestId('dashboard-message-erreur')).toBeTruthy();
    expect(await screen.findByTestId('notifications-notification-3')).toBeTruthy();
  });

  it('[UX-DR153] « Centre de notifications » est placé avant « Suivi de mes virements », qui reste la dernière section', async () => {
    ouvrir({ lister: () => liste(NOTIFICATIONS) });
    await screen.findByTestId('notifications-notification-3');
    await screen.findByTestId('dashboard-virements-revue-tableau');

    const texte = screen.getByTestId('dashboard-ecran').textContent ?? '';
    const centre = texte.indexOf('Centre de notifications');
    const suivi = texte.indexOf('Suivi de mes virements');
    expect(centre).toBeGreaterThan(texte.indexOf('Dernieres operations'));
    expect(suivi).toBeGreaterThan(centre);
    const sections = [...screen.getByTestId('dashboard-ecran').children].map((enfant) => enfant.getAttribute('data-testid') ?? enfant.tagName);
    expect(sections.at(-2)).toBe('notifications-section');
    expect(sections.at(-1)).toBe('dashboard-virements-revue');
  });
});
