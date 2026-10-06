// @vitest-environment jsdom
import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientVirement, Compte, ReponseVirement } from '../../../application/virement';
import type { VirementSuivi } from '../../../domain/virement';
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

const SUSPENDU: ReponseVirement = { statut: 'suspendu', heureButoir: '2026-11-06T09:15:00Z', montant: '10000.00', compteDestinataireId: 2 };

const SUIVI: VirementSuivi[] = [
  { virementId: 16, soumisLe: '2026-11-05T09:15:00Z', compteDestinataire: 'FR76-0001-0002', montant: '10000.00', etat: 'SUSPENDU', heureButoir: '2026-11-06T09:15:00Z' },
  { virementId: 15, soumisLe: '2026-11-05T08:00:00Z', compteDestinataire: 'FR76-0001-0002', montant: '500.00', etat: 'EXECUTE', heureButoir: '2026-11-06T08:00:00Z' },
  { virementId: 14, soumisLe: '2026-11-04T08:00:00Z', compteDestinataire: 'FR76-0001-0002', montant: '600.00', etat: 'REJETE', heureButoir: '2026-11-05T08:00:00Z' },
  { virementId: 13, soumisLe: '2026-11-03T08:00:00Z', compteDestinataire: 'FR76-0001-0002', montant: '700.00', etat: 'LIBERE', heureButoir: '2026-11-04T08:00:00Z' },
];

describe('écran de virement : suspension, refus et suivi', () => {
  it('[FR-027-04][BUG-076] virement suspendu : pas de redirection, avis role="status" focalisé en haut du formulaire, formulaire vidé', async () => {
    const utilisateur = userEvent.setup();
    const { routeur } = ouvrir({ executer: () => Promise.resolve(SUSPENDU) });
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await remplir(utilisateur, '1', '2', '10000', 'loyer');
    await utilisateur.click(screen.getByTestId('virement-executer'));

    const avis = await screen.findByTestId('virement-avis-suspension');
    expect(routeur.state.location.pathname).toBe('/virement');
    expect(avis.getAttribute('role')).toBe('status');
    expect(avis.className).toBe('alerte-avertissement');
    expect(document.activeElement).toBe(avis);
    expect(avis.textContent).toBe(
      "Votre virement de 10000.00 EUR vers le compte FR76-0001-0002 est en cours de vérification. Rien n'a été débité. " +
        'Vous aurez notre réponse avant le 06/11/2026 à 10:15. Sans décision à cette heure, il sera rejeté sans aucun débit.',
    );
    expect(screen.getByTestId('virement-avis-heure-butoir').textContent).toBe('06/11/2026 à 10:15');
    expect(avis.parentElement?.tagName).toBe('FORM');
    expect(avis.parentElement?.firstElementChild).toBe(avis);
    expect(liste('virement-compte-source').value).toBe('');
    expect(liste('virement-compte-destinataire').value).toBe('');
    expect(champ('virement-montant').value).toBe('');
    expect(champ('virement-motif').value).toBe('');
    expect(avis.textContent).not.toMatch(/fraude|score/i);
    expect(document.body.textContent).not.toMatch(/alerte\(s\)/);
  });

  it('[FR-027-04] une nouvelle soumission fait disparaître l’avis, et le suivi est rechargé après la suspension', async () => {
    const utilisateur = userEvent.setup();
    const reponses: ReponseVirement[] = [SUSPENDU, { statut: 'erreur', message: 'Solde insuffisant sur le compte source' }];
    const { lireSuivi } = ouvrir({ executer: () => Promise.resolve(reponses.shift() as ReponseVirement) });
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await remplir(utilisateur, '1', '2', '10000');
    await utilisateur.click(screen.getByTestId('virement-executer'));
    await screen.findByTestId('virement-avis-suspension');
    await waitFor(() => expect(lireSuivi).toHaveBeenCalledTimes(2));

    await remplir(utilisateur, '1', '2', '10');
    await utilisateur.click(screen.getByTestId('virement-executer'));
    await screen.findByTestId('virement-message-erreur');
    expect(screen.queryByTestId('virement-avis-suspension')).toBeNull();
  });

  it('[FR-003-27][BUG-084] refus pour erreur du contrôle : la vue de refus remplace le formulaire, référence sélectionnable, focus sur le titre', async () => {
    const utilisateur = userEvent.setup();
    const { routeur } = ouvrir({ executer: () => Promise.resolve({ statut: 'refuse', reference: '4bf92f3577b34da6a3ce929d0e0e4736' }) });
    await waitFor(() => expect(liste('virement-compte-source').options).toHaveLength(3));

    await remplir(utilisateur, '1', '2', '50');
    await utilisateur.click(screen.getByTestId('virement-executer'));

    const avis = await screen.findByTestId('virement-avis-refus');
    expect(routeur.state.location.pathname).toBe('/virement');
    expect(avis.getAttribute('role')).toBe('alert');
    expect(avis.className).toBe('alerte-avertissement');
    expect(avis.textContent).toBe(
      "Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence 4bf92f3577b34da6a3ce929d0e0e4736.",
    );
    const reference = screen.getByTestId('virement-refus-reference');
    expect(reference.textContent).toBe('4bf92f3577b34da6a3ce929d0e0e4736');
    expect(reference.className).toBe('ecran-virement__reference');
    expect(document.activeElement).toBe(screen.getByRole('heading', { name: 'Effectuer un virement' }));
    expect(screen.queryByTestId('virement-executer')).toBeNull();
    expect(screen.queryByTestId('virement-montant')).toBeNull();
    expect(avis.textContent).not.toMatch(/fraude/i);
  });

  it('[FR-027-16] le suivi liste les quatre virements passés en revue avec leurs badges d’état et l’heure butoir du suspendu', async () => {
    ouvrir({ lireSuivi: () => Promise.resolve({ statut: 'suivi', virements: SUIVI }) });

    const section = await screen.findByTestId('virement-virements-revue');
    await waitFor(() => expect(screen.getByTestId('virement-etat-16')).toBeTruthy());
    expect(section.textContent).toContain('Suivi de mes virements');
    expect(section.textContent).toContain(
      "Vous n'avez rien à faire : l'état de chaque virement se met à jour sur cette page. Un virement rejeté n'a pas été débité ; vous pouvez refaire votre demande.",
    );
    expect(Array.from(section.querySelectorAll('th')).map((th) => th.textContent)).toEqual([
      'Date',
      'Compte destinataire',
      'Montant',
      'Etat',
      'Réponse avant le',
    ]);
    expect(screen.getByTestId('virement-etat-16').textContent).toBe('En cours de vérification - non débité');
    expect(screen.getByTestId('virement-etat-15').textContent).toBe('Exécuté - débité');
    expect(screen.getByTestId('virement-etat-14').textContent).toBe('Rejeté - non débité');
    expect(screen.getByTestId('virement-etat-13').textContent).toBe('Accepté - pas encore débité');
    const lignes = section.querySelectorAll('tbody tr');
    expect(lignes).toHaveLength(4);
    expect(lignes[0]?.textContent).toContain('06/11/2026 à 10:15');
    expect(lignes[1]?.querySelectorAll('td')[4]?.textContent).toBe('');
    expect(section.compareDocumentPosition(screen.getByTestId('virement-montant')) & Node.DOCUMENT_POSITION_PRECEDING).toBeTruthy();
  });

  it('[FR-027-16] sans virement passé en revue : « Aucun virement à suivre », en-têtes conservés', async () => {
    ouvrir();

    expect(await screen.findByText('Aucun virement à suivre')).toBeTruthy();
    expect(screen.getByTestId('virement-virements-revue').querySelectorAll('th')).toHaveLength(5);
  });

  it('[FR-027-16] suivi indisponible : message technique dans la section', async () => {
    ouvrir({ lireSuivi: () => Promise.resolve({ statut: 'technique' }) });

    const erreur = await screen.findByTestId('virement-virements-revue-erreur');
    expect(erreur.textContent).toBe('Une erreur technique est survenue');
  });
});
