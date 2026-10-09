// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientAdministration, ReponseAdministration } from '../../../application/consulterAdministration';
import type { AlerteFraudeAdministree } from '../../../domain/administration';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const ok = <T,>(donnees: T): Promise<ReponseAdministration<T>> => Promise.resolve({ statut: 'ok', donnees });

// Les trois alertes de référence : une OPEN (score 45), une CONFIRMED et une FALSE_POSITIVE revues par l'ADMIN 10.
const OUVERTE: AlerteFraudeAdministree = {
  alerteId: 3,
  clientId: 1,
  nomClient: 'Jean Dupont',
  regle: 'LARGE_AMOUNT',
  score: 45,
  statut: 'OPEN',
  description: 'Montant superieur au seuil',
  creeLe: '2026-11-17T08:05:00Z',
  revuPar: null,
  nomRelecteur: null,
};
const CONFIRMEE: AlerteFraudeAdministree = {
  ...OUVERTE,
  alerteId: 2,
  regle: 'ROUND_AMOUNT',
  score: 20,
  statut: 'CONFIRMED',
  creeLe: '2026-11-16T08:00:00Z',
  revuPar: '10',
  nomRelecteur: 'Alice Martin',
};
const FAUX_POSITIF: AlerteFraudeAdministree = {
  ...CONFIRMEE,
  alerteId: 1,
  clientId: 2,
  nomClient: 'Marie Lebrun',
  regle: 'FOREIGN_IBAN',
  statut: 'FALSE_POSITIVE',
  creeLe: '2026-11-15T08:00:00Z',
};
const REFERENCE = [OUVERTE, CONFIRMEE, FAUX_POSITIF];

function ouvrir(administration: Partial<ClientAdministration> = {}) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
  return rendreApplication(
    '/administration',
    undefined,
    () => Promise.resolve({ statut: 'profil' as const, nomComplet: 'Alice Martin' }),
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    undefined,
    { lireAlertesFraude: () => ok(REFERENCE), ...administration },
  );
}

const section = () => screen.getByTestId('admin-alertes-section');
const lignes = () => within(screen.getByTestId('admin-alertes')).getAllByRole('row').slice(1);
const cellules = (alerteId: number) => within(screen.getByTestId(`admin-alerte-${alerteId}`)).getAllByRole('cell').map((td) => td.textContent);

/** Promesse résolue à la demande, pour observer l'état pendant la requête. */
function differee<T>() {
  let resoudre: (valeur: T) => void = () => {};
  const promesse = new Promise<T>((r) => {
    resoudre = r;
  });
  return { promesse, resoudre };
}

describe('section des alertes de fraude', () => {
  it('[FR-016-14][BUG-056] liste toutes les alertes, plus récente d’abord, score sur 100, « Traité par » et badge des ouvertes', async () => {
    ouvrir();

    const tableau = await screen.findByTestId('admin-alertes');
    expect(within(section()).getByRole('heading', { name: 'Alertes Fraude — Moteur de détection' })).toBeTruthy();
    expect(within(tableau).getAllByRole('columnheader').map((th) => th.textContent)).toEqual([
      'ID',
      'Client',
      'Règle déclenchée',
      'Score risque',
      'Statut',
      'Description',
      'Date',
      'Actions',
    ]);
    expect(lignes()).toHaveLength(3);
    expect(lignes().map((ligne) => ligne.getAttribute('data-testid'))).toEqual(['admin-alerte-3', 'admin-alerte-2', 'admin-alerte-1']);
    expect(cellules(3)).toEqual(['3', 'Jean Dupont', 'LARGE_AMOUNT', '45/100', 'OPEN', 'Montant superieur au seuil', '17/11/2026 09:05', 'ConfirmerFaux positif']);
    expect(cellules(2)).toEqual(['2', 'Jean Dupont', 'ROUND_AMOUNT', '20/100', 'CONFIRMED', 'Montant superieur au seuil', '16/11/2026 09:00', 'Traité par Alice Martin']);
    expect(cellules(1)[7]).toBe('Traité par Alice Martin');
    expect(within(section()).getAllByText('Traité par Alice Martin')).toHaveLength(2);
    expect(screen.getByTestId('admin-alerte-statut-1').textContent).toBe('FALSE_POSITIVE');
    expect(screen.getByTestId('admin-alertes-badge').textContent).toBe('1 ouverte(s)');
    expect(screen.queryByTestId('admin-alerte-confirmer-2')).toBeNull();
    expect(screen.queryByTestId('admin-alerte-faux-positif-1')).toBeNull();
    expect(within(section()).queryByText('Rafraichir')).toBeNull();
  });

  it('[FR-016-14] sans alerte ouverte, aucun badge', async () => {
    ouvrir({ lireAlertesFraude: () => ok([CONFIRMEE, FAUX_POSITIF]) });

    await screen.findByTestId('admin-alertes');
    expect(lignes()).toHaveLength(2);
    expect(screen.queryByTestId('admin-alertes-badge')).toBeNull();
  });

  it('[FR-016-14] sans alerte, les en-têtes restent avec « Aucune alerte fraude enregistrée »', async () => {
    ouvrir({ lireAlertesFraude: () => ok([]) });

    const tableau = await screen.findByTestId('admin-alertes');
    expect(within(tableau).getAllByRole('columnheader')).toHaveLength(8);
    expect(lignes()).toHaveLength(1);
    expect(lignes()[0]?.textContent).toBe('Aucune alerte fraude enregistrée');
    expect(screen.queryByTestId('admin-alertes-badge')).toBeNull();
  });

  it('[FR-016-14][FR-016-16] client hors projection et relecteur sans nom : repli sur l’identifiant', async () => {
    ouvrir({
      lireAlertesFraude: () =>
        ok([
          { ...OUVERTE, clientId: 99, nomClient: null, description: null },
          { ...CONFIRMEE, revuPar: '77', nomRelecteur: null },
          { ...FAUX_POSITIF, revuPar: null, nomRelecteur: null },
        ]),
    });

    await screen.findByTestId('admin-alertes');
    expect(cellules(3)[1]).toBe('Client #99 (introuvable)');
    expect(cellules(3)[5]).toBe('');
    expect(cellules(2)[7]).toBe('Traité par 77');
    expect(cellules(1)[7]).toBe('Traité par ');
  });

  it('[FR-016-15][FR-016-16][FR-027-12] Confirmer : un seul POST, bouton aria-disabled jusqu’à la réponse, puis la ligne relue « Traité par », sans message', async () => {
    const utilisateur = userEvent.setup();
    const reponse = differee<ReponseAdministration<null>>();
    let lectures = 0;
    const { administration } = ouvrir({
      lireAlertesFraude: () => {
        lectures += 1;
        return ok(lectures === 1 ? REFERENCE : [{ ...OUVERTE, statut: 'CONFIRMED', revuPar: '10', nomRelecteur: 'Alice Martin' }, CONFIRMEE, FAUX_POSITIF]);
      },
      revoirAlerte: () => reponse.promesse,
    });

    const bouton = await screen.findByTestId('admin-alerte-confirmer-3');
    await utilisateur.click(bouton);
    expect(bouton.getAttribute('aria-disabled')).toBe('true');
    expect(screen.getByTestId('admin-alerte-faux-positif-3').getAttribute('aria-disabled')).toBe('true');
    await utilisateur.click(bouton);
    await utilisateur.click(screen.getByTestId('admin-alerte-faux-positif-3'));
    reponse.resoudre({ statut: 'ok', donnees: null });

    await waitFor(() => expect(cellules(3)[7]).toBe('Traité par Alice Martin'));
    expect(administration.revoirAlerte).toHaveBeenCalledExactlyOnceWith(3, 'confirmation');
    expect(administration.lireAlertesFraude).toHaveBeenCalledTimes(2);
    expect(cellules(3)[4]).toBe('CONFIRMED');
    expect(screen.queryByTestId('admin-alerte-confirmer-3')).toBeNull();
    expect(screen.queryByTestId('admin-alertes-badge')).toBeNull();
    expect(screen.queryByTestId('admin-message-succes')).toBeNull();
    expect(screen.queryByTestId('admin-message-erreur')).toBeNull();
  });

  it('[FR-016-15][FR-027-12] Faux positif envoie l’issue faux-positif puis relit les alertes', async () => {
    const utilisateur = userEvent.setup();
    const { administration } = ouvrir();

    await utilisateur.click(await screen.findByTestId('admin-alerte-faux-positif-3'));

    await waitFor(() => expect(administration.lireAlertesFraude).toHaveBeenCalledTimes(2));
    expect(administration.revoirAlerte).toHaveBeenCalledExactlyOnceWith(3, 'faux-positif');
    expect(screen.getByTestId('admin-alerte-faux-positif-3').getAttribute('aria-disabled')).toBeNull();
  });

  it('[FR-016-15] une revue refusée affiche le message reçu dans la section, sans toucher aux autres sections', async () => {
    const utilisateur = userEvent.setup();
    ouvrir({ revoirAlerte: () => Promise.resolve({ statut: 'erreur', message: 'Accès refusé' }) });

    await utilisateur.click(await screen.findByTestId('admin-alerte-confirmer-3'));

    expect((await within(section()).findByTestId('admin-message-erreur')).textContent).toBe('Accès refusé');
    expect(screen.getAllByTestId('admin-message-erreur')).toHaveLength(1);
    expect(cellules(3)[4]).toBe('OPEN');
    expect(screen.getByTestId('admin-alerte-confirmer-3').getAttribute('aria-disabled')).toBeNull();
  });

  it('[FR-016-15] une revue en échec sans message affiche l’erreur technique', async () => {
    const utilisateur = userEvent.setup();
    ouvrir({ revoirAlerte: () => Promise.resolve({ statut: 'erreur', message: '' }) });

    await utilisateur.click(await screen.findByTestId('admin-alerte-confirmer-3'));

    expect((await within(section()).findByTestId('admin-message-erreur')).textContent).toBe('Une erreur technique est survenue');
  });

  it('[FR-016-15] un 401 à la revue termine la session et ramène à la connexion', async () => {
    const utilisateur = userEvent.setup();
    const { routeur } = ouvrir({ revoirAlerte: () => Promise.resolve({ statut: 'nonAuthentifie' }) });

    await utilisateur.click(await screen.findByTestId('admin-alerte-confirmer-3'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });

  it('[FR-016-14] une panne de la lecture affiche son message dans la section, les autres sections s’affichent', async () => {
    ouvrir({ lireAlertesFraude: () => Promise.resolve({ statut: 'erreur', message: 'X' }) });

    expect((await within(section()).findByTestId('admin-message-erreur')).textContent).toBe('X');
    expect(screen.queryByTestId('admin-alertes')).toBeNull();
    expect(screen.queryByTestId('admin-alertes-badge')).toBeNull();
    expect(await screen.findByTestId('admin-clients-section')).toBeTruthy();
    expect(screen.getAllByTestId('admin-message-erreur')).toHaveLength(1);
  });
});
