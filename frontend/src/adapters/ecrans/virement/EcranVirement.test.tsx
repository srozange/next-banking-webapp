// @vitest-environment jsdom
import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientVirement, Compte, ReponseVirement } from '../../../application/virement';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const COMPTES: Compte[] = [
  { id: 1, type: 'COURANT', numero: 'FR76-0001-0001' },
  { id: 2, type: 'EPARGNE', numero: 'FR76-0001-0002' },
];

function ouvrir(client: Partial<ClientVirement> = {}, roles = ['CLIENT']) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles }));
  return rendreApplication('/virement', undefined, undefined, undefined, undefined, {
    lireComptes: () => Promise.resolve({ statut: 'comptes', comptes: roles[0] === 'CLIENT' ? COMPTES : [] }),
    ...client,
  });
}

const liste = (testId: string) => screen.getByTestId(testId) as HTMLSelectElement;
const champ = (testId: string) => screen.getByTestId(testId) as HTMLInputElement;

async function remplir(utilisateur: ReturnType<typeof userEvent.setup>, source: string, destinataire: string, montant: string, motif = '') {
  await utilisateur.selectOptions(liste('virement-compte-source'), source);
  await utilisateur.selectOptions(liste('virement-compte-destinataire'), destinataire);
  if (montant !== '') {
    await utilisateur.type(champ('virement-montant'), montant);
  }
  if (motif !== '') {
    await utilisateur.type(champ('virement-motif'), motif.replaceAll('[', '[['));
  }
}

describe('écran de virement', () => {
  it('[FR-003-01] affiche le formulaire du legacy avec les comptes du client, sans bandeau technique', async () => {
    const { container } = ouvrir();
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    expect(document.title).toBe('BankApp Demo - Virement');
    expect(screen.getByRole('heading', { name: 'Effectuer un virement' })).toBeTruthy();
    for (const testId of ['virement-compte-source', 'virement-compte-destinataire']) {
      const options = Array.from(liste(testId).options).map((option) => [option.value, option.text]);
      expect(options).toEqual([
        ['', '-- Choisir un compte --'],
        ['1', 'COURANT FR76-0001-0001'],
        ['2', 'EPARGNE FR76-0001-0002'],
      ]);
    }
    expect(screen.getByLabelText('Compte source')).toBe(liste('virement-compte-source'));
    expect(screen.getByLabelText('Compte destinataire')).toBe(liste('virement-compte-destinataire'));
    expect(screen.getByLabelText('Montant (€)')).toBe(champ('virement-montant'));
    expect(screen.getByLabelText('Motif')).toBe(champ('virement-motif'));
    expect(champ('virement-motif').maxLength).toBe(100);
    expect(champ('virement-motif').hasAttribute('aria-required')).toBe(false);
    expect(champ('virement-montant').getAttribute('aria-describedby')).toBe('virement-montant-indice');
    expect(screen.getByText("Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude").id).toBe('virement-montant-indice');
    expect(screen.getByTestId('virement-executer').textContent).toBe('Exécuter le virement');
    const lien = screen.getByText('Virement SEPA externe — Gérer mes bénéficiaires');
    expect(lien.getAttribute('href')).toBe('/beneficiaires');
    expect(container.textContent).not.toMatch(/EJB|ActiveMQ|Artemis|Legacy/);
    expect(screen.queryByTestId('virement-message-erreur')).toBeNull();
  });

  it.each(['CONSEILLER', 'ADMIN'])('[FR-003-01] %s sans compte propre : les listes ne proposent que « -- Choisir un compte -- »', async (role) => {
    const { lireComptes } = ouvrir({}, [role]);
    await waitFor(() => expect(lireComptes).toHaveBeenCalled());

    for (const testId of ['virement-compte-source', 'virement-compte-destinataire']) {
      expect(Array.from(liste(testId).options).map((option) => option.text)).toEqual(['-- Choisir un compte --']);
    }
  });

  it('[FR-003-01] comptes indisponibles : message technique, listes vides', async () => {
    ouvrir({ lireComptes: () => Promise.resolve({ statut: 'technique' }) });

    expect((await screen.findByTestId('virement-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(liste('virement-compte-source').options).toHaveLength(1);
  });

  it('[FR-003-02] signale comptes puis montant manquants, aria-invalid, aria-describedby, focus, sans appel', async () => {
    const utilisateur = userEvent.setup();
    const { executer } = ouvrir();
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await utilisateur.click(screen.getByTestId('virement-executer'));

    const messages = screen.getAllByText('Veuillez sélectionner les deux comptes');
    expect(messages.map((message) => message.id)).toEqual(['virement-compte-source-erreur', 'virement-compte-destinataire-erreur']);
    expect(screen.getByText('Veuillez saisir le montant').id).toBe('virement-montant-erreur');
    for (const testId of ['virement-compte-source', 'virement-compte-destinataire', 'virement-montant']) {
      expect(screen.getByTestId(testId).getAttribute('aria-invalid')).toBe('true');
    }
    expect(champ('virement-montant').getAttribute('aria-describedby')).toBe('virement-montant-indice virement-montant-erreur');
    expect(document.activeElement).toBe(liste('virement-compte-source'));
    expect(executer).not.toHaveBeenCalled();
  });

  it('[FR-003-02] seul le montant manque : focus sur le montant', async () => {
    const utilisateur = userEvent.setup();
    ouvrir();
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await remplir(utilisateur, '1', '2', '');
    await utilisateur.click(screen.getByTestId('virement-executer'));
    expect(screen.queryByText('Veuillez sélectionner les deux comptes')).toBeNull();
    expect(document.activeElement).toBe(champ('virement-montant'));

    await utilisateur.selectOptions(liste('virement-compte-destinataire'), '');
    await utilisateur.click(screen.getByTestId('virement-executer'));
    expect(document.activeElement).toBe(liste('virement-compte-destinataire'));
  });

  it('[FR-003-03] comptes identiques : message dans virement-message-erreur, valeurs conservées, aucun appel', async () => {
    const utilisateur = userEvent.setup();
    const { executer } = ouvrir();
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await remplir(utilisateur, '1', '1', '100', '[retro] test comptes identiques');
    await utilisateur.click(screen.getByTestId('virement-executer'));

    const alerte = screen.getByTestId('virement-message-erreur');
    expect(alerte.textContent).toBe('Le compte source et destination doivent être différents');
    expect(document.activeElement).toBe(alerte);
    expect(liste('virement-compte-source').value).toBe('1');
    expect(liste('virement-compte-destinataire').value).toBe('1');
    expect(champ('virement-montant').value).toBe('100');
    expect(champ('virement-motif').value).toBe('[retro] test comptes identiques');
    expect(executer).not.toHaveBeenCalled();
  });

  it.each([
    ['FR-003-04', 'abc', 400, 'Erreur lors du virement : Le montant doit etre positif'],
    ['FR-003-05', '100', 404, 'Erreur lors du virement : Compte introuvable'],
    ['FR-003-06', '9999', 400, 'Solde insuffisant sur le compte source'],
    ['FR-003-01', '100', 400, 'Le motif ne doit pas dépasser 100 caractères'],
    ['FR-003-05', '100', 403, 'Accès refusé'],
  ])('[%s] affiche le message du service, reste sur /virement, valeurs conservées', async (_fr, montant, _statut, message) => {
    const utilisateur = userEvent.setup();
    const { routeur, executer } = ouvrir({ executer: () => Promise.resolve<ReponseVirement>({ statut: 'erreur', message }) });
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await remplir(utilisateur, '1', '2', montant, 'loyer');
    await utilisateur.click(screen.getByTestId('virement-executer'));

    expect((await screen.findByTestId('virement-message-erreur')).textContent).toBe(message);
    expect(routeur.state.location.pathname).toBe('/virement');
    expect(champ('virement-montant').value).toBe(montant);
    expect(champ('virement-motif').value).toBe('loyer');
    expect(executer).toHaveBeenCalledOnce();
  });

  it('[FR-003-08] virement exécuté : soumission directe, bouton aria-busy pendant l’envoi, redirection vers /tableau-de-bord', async () => {
    const utilisateur = userEvent.setup();
    let terminer: (reponse: ReponseVirement) => void = () => {};
    const { routeur, executer } = ouvrir({
      executer: () => new Promise<ReponseVirement>((resolve) => (terminer = resolve)),
    });
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await remplir(utilisateur, '1', '2', '100,00', 'loyer');
    await utilisateur.click(screen.getByTestId('virement-executer'));

    const bouton = screen.getByTestId('virement-executer') as HTMLButtonElement;
    expect(bouton.getAttribute('aria-busy')).toBe('true');
    expect(bouton.disabled).toBe(true);
    expect(executer).toHaveBeenCalledWith({ compteSourceId: 1, compteDestinataireId: 2, montant: '100.00', motif: 'loyer' });

    terminer({ statut: 'execute' });
    await waitFor(() => expect(routeur.state.location.pathname).toBe('/tableau-de-bord'));
  });

  it('[FR-003-12][FR-023-02] remise impossible : le message du service s’affiche, pas de redirection, nouvel essai possible', async () => {
    const utilisateur = userEvent.setup();
    const reponses: ReponseVirement[] = [
      { statut: 'erreur', message: "Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard" },
      { statut: 'erreur', message: null },
    ];
    const { routeur, executer } = ouvrir({ executer: () => Promise.resolve(reponses.shift() as ReponseVirement) });
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await remplir(utilisateur, '1', '2', '100');
    await utilisateur.click(screen.getByTestId('virement-executer'));
    expect((await screen.findByTestId('virement-message-erreur')).textContent).toBe(
      "Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard",
    );
    expect(routeur.state.location.pathname).toBe('/virement');
    expect((screen.getByTestId('virement-executer') as HTMLButtonElement).disabled).toBe(false);

    await utilisateur.click(screen.getByTestId('virement-executer'));
    await waitFor(() => expect(screen.getByTestId('virement-message-erreur').textContent).toBe('Une erreur technique est survenue'));
    expect(executer).toHaveBeenCalledTimes(2);
  });
});
