// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { ClientVirementsProgrammes, ReponseAction, ReponseCreation, ReponseVirementsProgrammes } from '../../../application/gererVirementsProgrammes';
import type { ClientVirement } from '../../../application/virement';
import type { LigneVirementProgramme } from '../../../domain/virementsProgrammes';
import { TEXTES } from '../../textes';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const COMPTES: ClientVirement['lireComptes'] = () =>
  Promise.resolve({
    statut: 'comptes',
    comptes: [
      { id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' },
      { id: 2, type: 'EPARGNE', numero: 'FR76-0001-0002', solde: '15200.00' },
    ],
  });
const ACTIF: LigneVirementProgramme = {
  id: 7,
  compteSourceId: 1,
  numeroCompteSource: 'FR76-0001-0001',
  numeroBeneficiaire: 'FR76-0001-0002',
  libelleBeneficiaire: 'Epargne',
  montant: '35.00',
  frequence: 'MENSUEL',
  motif: null,
  prochaineExecution: '2027-01-01',
  actif: true,
  nbExecutions: 2,
};
const INACTIF: LigneVirementProgramme = { ...ACTIF, id: 8, libelleBeneficiaire: 'Loyer', frequence: 'ANNUEL', prochaineExecution: '2027-03-15', actif: false, nbExecutions: 0 };

const liste = (...virements: LigneVirementProgramme[]) => Promise.resolve<ReponseVirementsProgrammes>({ statut: 'liste', virements });

function ouvrir(client: Partial<ClientVirementsProgrammes> = {}, lireComptes: ClientVirement['lireComptes'] = COMPTES) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
  const indefini = undefined;
  return rendreApplication('/virements-programmes', indefini, indefini, indefini, indefini, { lireComptes }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, client);
}

/** user-event lit « [ » comme une touche spéciale : il se double pour être tapé tel quel. */
const litteral = (texte: string) => texte.replaceAll('[', '[[');

async function remplir(utilisateur: ReturnType<typeof userEvent.setup>, valeurs: Partial<Record<string, string>> = {}) {
  const saisie = { montant: '50', numero: 'FR76-0001-0002', libelle: '[retro] test virprog', date: '2027-01-01', motif: '[retro] virement programm gm', ...valeurs };
  await utilisateur.selectOptions(await screen.findByTestId('vp-compte-debiteur'), valeurs.compte ?? '1');
  await utilisateur.type(screen.getByTestId('vp-montant'), litteral(saisie.montant));
  await utilisateur.type(screen.getByTestId('vp-numero-beneficiaire'), litteral(saisie.numero));
  await utilisateur.type(screen.getByTestId('vp-libelle-beneficiaire'), litteral(saisie.libelle));
  await utilisateur.type(screen.getByTestId('vp-date-premiere-execution'), litteral(saisie.date));
  await utilisateur.type(screen.getByTestId('vp-motif'), litteral(saisie.motif));
}

describe('écran des virements programmés', () => {
  it('[FR-004-01] sans session, redirige vers /login', async () => {
    const { routeur } = rendreApplication('/virements-programmes');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-004-02] affiche l’onglet, les sections, les libellés reliés et les exemples', async () => {
    ouvrir();
    await screen.findByTestId('vp-aucun');

    expect(document.title).toBe('BankApp Demo - Virements programmes');
    expect(screen.getByRole('heading', { name: 'Nouveau virement programme' })).toBeTruthy();
    expect(screen.getByRole('heading', { name: 'Mes virements programmes' })).toBeTruthy();
    expect(screen.getByLabelText('Compte debiteur')).toBe(screen.getByTestId('vp-compte-debiteur'));
    expect(screen.getByLabelText('Montant (EUR)')).toBe(screen.getByTestId('vp-montant'));
    expect(screen.getByLabelText('Frequence')).toBe(screen.getByTestId('vp-frequence'));
    expect(screen.getByLabelText('IBAN / N compte beneficiaire')).toBe(screen.getByTestId('vp-numero-beneficiaire'));
    expect(screen.getByLabelText('Libelle beneficiaire')).toBe(screen.getByTestId('vp-libelle-beneficiaire'));
    expect(screen.getByLabelText('Date de premiere execution')).toBe(screen.getByTestId('vp-date-premiere-execution'));
    expect(screen.getByLabelText('Motif (optionnel)')).toBe(screen.getByTestId('vp-motif'));
    expect(screen.getByTestId('vp-montant').getAttribute('placeholder')).toBe('ex: 200.00');
    expect(screen.getByTestId('vp-numero-beneficiaire').getAttribute('placeholder')).toBe('ex: FR76-0001-0002');
    expect(screen.getByTestId('vp-libelle-beneficiaire').getAttribute('placeholder')).toBe('ex: Compte epargne');
    expect(screen.getByTestId('vp-date-premiere-execution').getAttribute('placeholder')).toBe('yyyy-MM-dd');
    expect(screen.getByTestId('vp-motif').getAttribute('placeholder')).toBe('ex: Epargne mensuelle');
    expect(screen.getByTestId('vp-creer').textContent).toBe('Creer le virement programme');
  });

  it('[FR-004-02] limite les longueurs à 34, 100 et 255 caractères', async () => {
    ouvrir();
    await screen.findByTestId('vp-aucun');

    expect(screen.getByTestId('vp-numero-beneficiaire').getAttribute('maxlength')).toBe('34');
    expect(screen.getByTestId('vp-libelle-beneficiaire').getAttribute('maxlength')).toBe('100');
    expect(screen.getByTestId('vp-motif').getAttribute('maxlength')).toBe('255');
  });

  it('[FR-004-02] propose « -- Choisir -- » puis les comptes du client, valeur = identifiant', async () => {
    ouvrir();
    const liste = await screen.findByTestId('vp-compte-debiteur');
    await within(liste).findByText('FR76-0001-0001 (COURANT) - 2130.00 EUR');

    const options = within(liste).getAllByRole('option');
    expect(options.map((o) => [o.getAttribute('value'), o.textContent])).toEqual([
      ['', '-- Choisir --'],
      ['1', 'FR76-0001-0001 (COURANT) - 2130.00 EUR'],
      ['2', 'FR76-0001-0002 (EPARGNE) - 15200.00 EUR'],
    ]);
  });

  it('[FR-004-08] propose les six fréquences dans l’ordre, MENSUEL présélectionné', async () => {
    ouvrir();
    const liste = await screen.findByTestId('vp-frequence');

    expect(within(liste).getAllByRole('option').map((o) => [o.getAttribute('value'), o.textContent])).toEqual([
      ['HEBDOMADAIRE', 'Hebdomadaire'],
      ['MENSUEL', 'Mensuel'],
      ['BIMESTRIEL', 'Bimestriel'],
      ['TRIMESTRIEL', 'Trimestriel'],
      ['SEMESTRIEL', 'Semestriel'],
      ['ANNUEL', 'Annuel'],
    ]);
    expect((liste as HTMLSelectElement).value).toBe('MENSUEL');
  });

  it('[FR-004-02] sans compte, « -- Choisir -- » reste seul', async () => {
    ouvrir({}, () => Promise.resolve({ statut: 'comptes', comptes: [] }));
    const liste = await screen.findByTestId('vp-compte-debiteur');
    await screen.findByTestId('vp-aucun');

    expect(within(liste).getAllByRole('option').map((o) => o.textContent)).toEqual(['-- Choisir --']);
  });

  it('[FR-004-03] affiche les colonnes, les lignes dans l’ordre du service, les états et les boutons', async () => {
    ouvrir({ lister: () => liste(ACTIF, INACTIF) });
    await screen.findByTestId('vp-ligne-7');

    const entetes = within(screen.getByTestId('vp-tableau')).getAllByRole('columnheader');
    expect(entetes.map((e) => e.textContent)).toEqual(['Source', 'Beneficiaire', 'Montant', 'Frequence', 'Prochain', 'Nb executions', 'Etat', 'Actions']);
    entetes.forEach((e) => expect(e.getAttribute('scope')).toBe('col'));
    expect(screen.getAllByTestId(/^vp-ligne-/).map((l) => l.getAttribute('data-testid'))).toEqual(['vp-ligne-7', 'vp-ligne-8']);
    expect(within(screen.getByTestId('vp-ligne-7')).getAllByRole('cell').map((c) => c.textContent)).toEqual([
      'FR76-0001-0001',
      'EpargneFR76-0001-0002',
      'EUR35.00',
      'MENSUEL',
      '01/01/2027',
      '2',
      'Actif',
      'SuspendreSupprimer',
    ]);
    expect(within(screen.getByTestId('vp-ligne-8')).getAllByRole('cell').map((c) => c.textContent)).toEqual([
      'FR76-0001-0001',
      'LoyerFR76-0001-0002',
      'EUR35.00',
      'ANNUEL',
      '15/03/2027',
      '0',
      'Inactif',
      'ReactiverSupprimer',
    ]);
    expect(screen.getByTestId('vp-suspendre-7').textContent).toBe('Suspendre');
    expect(screen.queryByTestId('vp-reactiver-7')).toBeNull();
    expect(screen.getByTestId('vp-reactiver-8').textContent).toBe('Reactiver');
    expect(screen.queryByTestId('vp-suspendre-8')).toBeNull();
    expect(screen.getByTestId('vp-supprimer-7').textContent).toBe('Supprimer');
    expect(screen.getByTestId('vp-supprimer-8').textContent).toBe('Supprimer');
  });

  it('[FR-004-09] suspendre appelle le service, recharge la liste sans modale et affiche Inactif avec Reactiver et Supprimer', async () => {
    const lister = vi.fn().mockReturnValueOnce(liste(ACTIF)).mockReturnValue(liste({ ...ACTIF, actif: false }));
    const { suspendreVirementProgramme } = ouvrir({ lister, suspendre: () => Promise.resolve({ statut: 'ok' }) });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));

    await screen.findByTestId('vp-reactiver-7');
    expect(suspendreVirementProgramme).toHaveBeenCalledWith(7);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('[FR-004-10] supprimer appelle le service et retire la ligne, dernière ligne : « Aucun virement programme configure »', async () => {
    const lister = vi.fn().mockReturnValueOnce(liste(ACTIF)).mockReturnValue(liste());
    const { supprimerVirementProgramme } = ouvrir({ lister, supprimer: () => Promise.resolve({ statut: 'ok' }) });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-supprimer-7'));

    await screen.findByTestId('vp-aucun');
    expect(supprimerVirementProgramme).toHaveBeenCalledWith(7);
    expect(screen.queryByTestId('vp-ligne-7')).toBeNull();
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('[FR-004-09] un virement suspendu se réactive : Actif, Suspendre et Supprimer', async () => {
    const lister = vi.fn().mockReturnValueOnce(liste(INACTIF)).mockReturnValue(liste({ ...INACTIF, actif: true }));
    const { reactiverVirementProgramme } = ouvrir({ lister, reactiver: () => Promise.resolve({ statut: 'ok' }) });
    await screen.findByTestId('vp-ligne-8');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-reactiver-8'));

    await screen.findByTestId('vp-suspendre-8');
    expect(reactiverVirementProgramme).toHaveBeenCalledWith(8);
    expect(within(screen.getByTestId('vp-ligne-8')).getByText('Actif')).toBeTruthy();
    expect(screen.queryByTestId('vp-reactiver-8')).toBeNull();
    expect(screen.getByTestId('vp-supprimer-8')).toBeTruthy();
  });

  it('[FR-004-09] une suspension réussie recharge la liste : Inactif, Reactiver et Supprimer', async () => {
    const lister = vi.fn().mockReturnValueOnce(liste(ACTIF)).mockReturnValue(liste({ ...ACTIF, actif: false }));
    ouvrir({ lister, suspendre: () => Promise.resolve({ statut: 'ok' }) });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));

    await screen.findByTestId('vp-reactiver-7');
    expect(within(screen.getByTestId('vp-ligne-7')).getByText('Inactif')).toBeTruthy();
    expect(screen.queryByTestId('vp-suspendre-7')).toBeNull();
    expect(screen.getByTestId('vp-supprimer-7')).toBeTruthy();
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it.each([
    ['vp-suspendre-7', 'suspendre'],
    ['vp-supprimer-7', 'supprimer'],
  ] as const)('[FR-004-10] un 404 (%s) affiche vp.erreur.appartenance, ligne inchangée', async (testId, action) => {
    ouvrir({ lister: () => liste(ACTIF), [action]: () => Promise.resolve({ statut: 'introuvable' }) });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId(testId));

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe('Erreur : action impossible. Rechargez la page.');
    expect(screen.getByTestId('vp-ligne-7')).toBeTruthy();
  });

  it('[FR-004-10] un 404 sur Reactiver affiche vp.erreur.appartenance', async () => {
    ouvrir({ lister: () => liste(INACTIF), reactiver: () => Promise.resolve({ statut: 'introuvable' }) });
    await screen.findByTestId('vp-ligne-8');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-reactiver-8'));

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe('Erreur : action impossible. Rechargez la page.');
  });

  it('[FR-004-09] un échec technique de Suspendre puis de Reactiver affiche vp.erreur.etat, ligne inchangée', async () => {
    ouvrir({ lister: () => liste(ACTIF, INACTIF), suspendre: () => Promise.reject(new Error('coupure')) });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));
    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe(
      "Erreur : l'état du virement programmé n'a pas changé. Rien n'a été débité. Veuillez réessayer.",
    );
    expect(within(screen.getByTestId('vp-ligne-7')).getByText('Actif')).toBeTruthy();

    await utilisateur.click(screen.getByTestId('vp-reactiver-8'));
    await waitFor(() => expect(screen.getByTestId('vp-message-erreur').textContent).toContain("l'état du virement programmé n'a pas changé"));
    expect(within(screen.getByTestId('vp-ligne-8')).getByText('Inactif')).toBeTruthy();
  });

  it('[FR-004-10] un échec technique de Supprimer affiche vp.erreur.suppression, ligne conservée', async () => {
    ouvrir({ lister: () => liste(ACTIF) });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-supprimer-7'));

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe(
      "Erreur suppression : le virement programmé n'a pas été supprimé et reste inchangé. Veuillez réessayer.",
    );
    expect(screen.getByTestId('vp-ligne-7')).toBeTruthy();
  });

  it('[FR-004-09] une action réussie efface l’erreur précédente et le succès de création', async () => {
    const lister = vi.fn().mockReturnValueOnce(liste(ACTIF)).mockReturnValue(liste({ ...ACTIF, actif: false }));
    const suspendre = vi.fn().mockResolvedValueOnce({ statut: 'technique' }).mockResolvedValue({ statut: 'ok' });
    ouvrir({ lister, suspendre });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));
    await screen.findByTestId('vp-message-erreur');
    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));

    await screen.findByTestId('vp-reactiver-7');
    expect(screen.queryByTestId('vp-message-erreur')).toBeNull();
    expect(screen.queryByTestId('vp-message-succes')).toBeNull();
  });

  it('[FR-004-09] une relecture en échec après l’action affiche le texte technique', async () => {
    const lister = vi.fn().mockReturnValueOnce(liste(ACTIF)).mockResolvedValue({ statut: 'technique' });
    ouvrir({ lister, suspendre: () => Promise.resolve({ statut: 'ok' }) });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe(TEXTES.erreurTechnique);
  });

  it('[FR-004-09] les boutons sont désactivés pendant l’action : pas de double envoi', async () => {
    let rendre: (reponse: ReponseAction) => void = () => {};
    const { suspendreVirementProgramme } = ouvrir({
      lister: () => liste(ACTIF, INACTIF),
      suspendre: () => new Promise<ReponseAction>((resolve) => (rendre = resolve)),
    });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));
    await waitFor(() => expect(screen.getByTestId('vp-suspendre-7').hasAttribute('disabled')).toBe(true));
    expect(screen.getByTestId('vp-supprimer-7').hasAttribute('disabled')).toBe(true);
    expect(screen.getByTestId('vp-reactiver-8').hasAttribute('disabled')).toBe(true);
    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));
    rendre({ statut: 'ok' });

    await waitFor(() => expect(screen.getByTestId('vp-suspendre-7').hasAttribute('disabled')).toBe(false));
    expect(suspendreVirementProgramme).toHaveBeenCalledOnce();
  });

  it('[FR-004-09] un 401 du service pendant une action ramène à /login', async () => {
    let rendu: ReturnType<typeof ouvrir> | undefined;
    rendu = ouvrir({
      lister: () => liste(ACTIF),
      suspendre: () => {
        rendu?.signal.declencher();
        return Promise.resolve({ statut: 'technique' });
      },
    });
    await screen.findByTestId('vp-ligne-7');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-suspendre-7'));

    await waitFor(() => expect(rendu?.routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-004-03] sans virement, garde les en-têtes et une ligne pleine largeur', async () => {
    ouvrir();
    const vide = await screen.findByTestId('vp-aucun');

    expect(vide.textContent).toBe('Aucun virement programme configure');
    expect(vide.getAttribute('colspan')).toBe('8');
    expect(screen.getAllByRole('columnheader')).toHaveLength(8);
  });

  it('[FR-004-03] une lecture en échec affiche le texte technique', async () => {
    ouvrir({ lister: () => Promise.resolve({ statut: 'technique' }) });

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe('Une erreur technique est survenue');
  });

  it('[FR-004-02] des comptes illisibles affichent le texte technique', async () => {
    ouvrir({}, () => Promise.resolve({ statut: 'technique' }));

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe('Une erreur technique est survenue');
  });

  it('[FR-004-04] le formulaire vide affiche « Veuillez selectionner un compte source » sans appeler le service', async () => {
    const { creerVirementProgramme } = ouvrir();
    await screen.findByTestId('vp-aucun');

    await userEvent.setup().click(screen.getByTestId('vp-creer'));

    const alerte = await screen.findByTestId('vp-message-erreur');
    expect(alerte.getAttribute('role')).toBe('alert');
    expect(alerte.textContent).toBe('Veuillez selectionner un compte source');
    expect(screen.getByTestId('vp-compte-debiteur').getAttribute('aria-invalid')).toBe('true');
    expect(creerVirementProgramme).not.toHaveBeenCalled();
  });

  it.each([
    ['numéro blanc', { numero: ' ' }, 'Veuillez saisir le numero de compte destinataire', 'vp-numero-beneficiaire'],
    ['libellé blanc', { libelle: ' ' }, 'Veuillez saisir le libelle du beneficiaire', 'vp-libelle-beneficiaire'],
    ['date vide', { date: ' ' }, 'Veuillez saisir la date de premiere execution', 'vp-date-premiere-execution'],
    ['date au mauvais format', { date: '01/01/2027' }, 'Format de date invalide (attendu : yyyy-MM-dd)', 'vp-date-premiere-execution'],
    ['montant nul', { montant: '0' }, 'Montant invalide', 'vp-montant'],
    ['montant négatif', { montant: '-5' }, 'Montant invalide', 'vp-montant'],
    ['montant illisible', { montant: 'abc' }, 'Montant invalide', 'vp-montant'],
  ])('[FR-004-05] %s : message exact, champ invalide, valeurs conservées, rien envoyé', async (_nom, saisie, message, testId) => {
    const { creerVirementProgramme } = ouvrir();
    const utilisateur = userEvent.setup();
    await remplir(utilisateur, saisie);

    await utilisateur.click(screen.getByTestId('vp-creer'));

    const alerte = await screen.findByTestId('vp-message-erreur');
    expect(alerte.textContent).toBe(message);
    expect(screen.getByTestId(testId).getAttribute('aria-invalid')).toBe('true');
    expect(document.activeElement).toBe(screen.getByTestId(testId));
    expect((screen.getByTestId('vp-motif') as HTMLInputElement).value).toBe('[retro] virement programm gm');
    expect((screen.getByTestId('vp-compte-debiteur') as HTMLSelectElement).value).toBe('1');
    expect(creerVirementProgramme).not.toHaveBeenCalled();
  });

  it('[FR-004-07] la création nominale vide les champs, annonce le succès et recharge la liste', async () => {
    let creees = false;
    const lister = () => (creees ? liste(ACTIF) : liste());
    const creer = () => {
      creees = true;
      return Promise.resolve<ReponseCreation>({ statut: 'cree' });
    };
    const { creerVirementProgramme, listerVirementsProgrammes } = ouvrir({ lister, creer });
    const utilisateur = userEvent.setup();
    await screen.findByTestId('vp-aucun');
    await remplir(utilisateur);
    await utilisateur.selectOptions(screen.getByTestId('vp-frequence'), 'MENSUEL');

    await utilisateur.click(screen.getByTestId('vp-creer'));

    const succes = await screen.findByTestId('vp-message-succes');
    expect(succes.getAttribute('role')).toBe('status');
    expect(succes.textContent).toBe('Virement programme cree avec succes');
    expect(creerVirementProgramme).toHaveBeenCalledWith({
      compteSourceId: 1,
      numeroBeneficiaire: 'FR76-0001-0002',
      libelleBeneficiaire: '[retro] test virprog',
      montant: '50.00',
      frequence: 'MENSUEL',
      motif: '[retro] virement programm gm',
      datePremiereExecution: '2027-01-01',
    });
    await screen.findByTestId('vp-ligne-7');
    expect(listerVirementsProgrammes).toHaveBeenCalledTimes(2);
    expect((screen.getByTestId('vp-compte-debiteur') as HTMLSelectElement).value).toBe('');
    expect((screen.getByTestId('vp-montant') as HTMLInputElement).value).toBe('');
    expect((screen.getByTestId('vp-frequence') as HTMLSelectElement).value).toBe('MENSUEL');
    expect((screen.getByTestId('vp-numero-beneficiaire') as HTMLInputElement).value).toBe('');
    expect((screen.getByTestId('vp-libelle-beneficiaire') as HTMLInputElement).value).toBe('');
    expect((screen.getByTestId('vp-date-premiere-execution') as HTMLInputElement).value).toBe('');
    expect((screen.getByTestId('vp-motif') as HTMLInputElement).value).toBe('');
    expect(screen.queryByTestId('vp-message-erreur')).toBeNull();
  });

  it('[FR-004-08] la fréquence choisie est envoyée puis revient à MENSUEL', async () => {
    const { creerVirementProgramme } = ouvrir({ creer: () => Promise.resolve({ statut: 'cree' }) });
    const utilisateur = userEvent.setup();
    await screen.findByTestId('vp-aucun');
    await remplir(utilisateur);
    await utilisateur.selectOptions(screen.getByTestId('vp-frequence'), 'TRIMESTRIEL');

    await utilisateur.click(screen.getByTestId('vp-creer'));

    await screen.findByTestId('vp-message-succes');
    expect(creerVirementProgramme).toHaveBeenCalledWith(expect.objectContaining({ frequence: 'TRIMESTRIEL' }));
    expect((screen.getByTestId('vp-frequence') as HTMLSelectElement).value).toBe('MENSUEL');
  });

  it('[FR-004-07] une relecture en échec après la création affiche le texte technique avec le succès', async () => {
    let appels = 0;
    const lister = () => (++appels === 1 ? liste() : Promise.resolve<ReponseVirementsProgrammes>({ statut: 'technique' }));
    ouvrir({ lister, creer: () => Promise.resolve({ statut: 'cree' }) });
    const utilisateur = userEvent.setup();
    await screen.findByTestId('vp-aucun');
    await remplir(utilisateur);

    await utilisateur.click(screen.getByTestId('vp-creer'));

    await screen.findByTestId('vp-message-succes');
    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe('Une erreur technique est survenue');
  });

  it('[FR-004-13] un refus du service s’affiche tel quel, champ invalide, valeurs conservées', async () => {
    ouvrir({ creer: () => Promise.resolve({ statut: 'refuse', message: 'Compte destinataire introuvable', champ: 'numeroBeneficiaire' }) });
    const utilisateur = userEvent.setup();
    await screen.findByTestId('vp-aucun');
    await remplir(utilisateur, { numero: 'FR76-9999-9999' });

    await utilisateur.click(screen.getByTestId('vp-creer'));

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe('Compte destinataire introuvable');
    expect(screen.getByTestId('vp-numero-beneficiaire').getAttribute('aria-invalid')).toBe('true');
    expect((screen.getByTestId('vp-numero-beneficiaire') as HTMLInputElement).value).toBe('FR76-9999-9999');
    expect(screen.queryByTestId('vp-message-succes')).toBeNull();
  });

  it('[FR-004-06] un refus du service sans champ ne marque aucun champ', async () => {
    ouvrir({ creer: () => Promise.resolve({ statut: 'refuse', message: 'Erreur : Compte non autorise', champ: null }) });
    const utilisateur = userEvent.setup();
    await screen.findByTestId('vp-aucun');
    await remplir(utilisateur);

    await utilisateur.click(screen.getByTestId('vp-creer'));

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe('Erreur : Compte non autorise');
    expect(document.querySelectorAll('[aria-invalid="true"]')).toHaveLength(0);
  });

  it('[FR-004-07] une erreur technique affiche vp.erreur.creation sans détail, valeurs conservées', async () => {
    ouvrir({ creer: () => Promise.resolve({ statut: 'technique' }) });
    const utilisateur = userEvent.setup();
    await screen.findByTestId('vp-aucun');
    await remplir(utilisateur);

    await utilisateur.click(screen.getByTestId('vp-creer'));

    expect((await screen.findByTestId('vp-message-erreur')).textContent).toBe(
      "Erreur : le virement programmé n'a pas été créé. Rien n'a été débité. Veuillez réessayer.",
    );
    expect((screen.getByTestId('vp-montant') as HTMLInputElement).value).toBe('50');
    expect(screen.queryByTestId('vp-message-succes')).toBeNull();
  });

  it('[FR-004-01] un 401 du service ramène à /login', async () => {
    let rendu: ReturnType<typeof ouvrir> | undefined;
    rendu = ouvrir({
      creer: () => {
        rendu?.signal.declencher();
        return Promise.resolve({ statut: 'technique' });
      },
    });
    const utilisateur = userEvent.setup();
    await screen.findByTestId('vp-aucun');
    await remplir(utilisateur);

    await utilisateur.click(screen.getByTestId('vp-creer'));

    await waitFor(() => expect(rendu?.routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-004-07] ignore un second envoi pendant qu’un envoi est en cours', async () => {
    let rendre: (reponse: ReponseCreation) => void = () => {};
    const { creerVirementProgramme } = ouvrir({ creer: () => new Promise<ReponseCreation>((resolve) => (rendre = resolve)) });
    const utilisateur = userEvent.setup();
    await screen.findByTestId('vp-aucun');
    await remplir(utilisateur);

    await utilisateur.click(screen.getByTestId('vp-creer'));
    await waitFor(() => expect(screen.getByTestId('vp-creer').hasAttribute('disabled')).toBe(true));
    screen.getByTestId('vp-creer').closest('form')?.requestSubmit();
    rendre({ statut: 'cree' });

    await screen.findByTestId('vp-message-succes');
    expect(creerVirementProgramme).toHaveBeenCalledOnce();
  });
});
