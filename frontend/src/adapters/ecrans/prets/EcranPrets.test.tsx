// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { Pret, Simulation } from '../../../domain/pret';
import type { ReponsePrets, ReponseSimulation } from '../../../application/prets';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

function connecter(role = 'CLIENT') {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
}

const tableEmprunts = () => within(screen.getByRole('region', { name: 'Emprunts en cours' })).getByRole('table');
const champ = (testId: string) => screen.getByTestId(testId) as HTMLInputElement;

function ligne(numero: number, restant: string) {
  return { numero, capital: '263.85', interets: '29.17', total: '293.02', capitalRestant: restant };
}

const SIMULATION: Simulation = {
  mensualite: '293.02',
  coutTotal: '10548.72',
  totalInterets: '548.72',
  echeancier: Array.from({ length: 36 }, (_, i) => ligne(i + 1, i === 35 ? '0.00' : '9736.15')),
};

const simulationReussie = () => Promise.resolve<ReponseSimulation>({ statut: 'simulation', simulation: SIMULATION });

function pret(id: number, statut: Pret['statut'], extra: Partial<Pret> = {}): Pret {
  return {
    id,
    type: 'TRAVAUX',
    montant: '5000.00',
    tauxAnnuel: '4.9000',
    dureeMois: 24,
    mensualite: '218.63',
    coutTotal: '5247.12',
    totalInterets: '247.12',
    objet: null,
    statut,
    dateDebut: '2026-08-01',
    dateFin: '2028-08-01',
    ...extra,
  };
}

const listePrets = (prets: Pret[]) => () => Promise.resolve<ReponsePrets>({ statut: 'prets', prets });

describe('écran Prêts', () => {
  it.each(['CLIENT', 'CONSEILLER', 'ADMIN'])(
    '[FR-007-01] %s voit les trois séparateurs rendus en capitales par CSS et le titre d’onglet',
    async (role) => {
      connecter(role);
      const { container } = rendreApplication('/prets');

      await waitFor(() => expect(document.title).toBe('BankApp Demo - Prets'));
      const titres = Array.from(container.querySelectorAll('h2')).map((titre) => titre.textContent);
      expect(titres).toEqual(['Simulateur de pret', 'Demande de pret', 'Mes emprunts']);
      for (const titre of container.querySelectorAll('h2')) {
        expect(titre.className).toMatch(/separateur/);
      }
      expect(container.textContent).not.toContain('SIMULATEUR DE PRET');
    },
  );

  it('[FR-007-01] sans jeton, /prets mène à /login', async () => {
    const { routeur } = rendreApplication('/prets');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-007-01] une session perdue pendant la lecture des prêts mène à /login', async () => {
    connecter();
    const { routeur } = rendreApplication('/prets', undefined, undefined, undefined, undefined, {
      lister: () => Promise.resolve<ReponsePrets>({ statut: 'nonAuthentifie' }),
    });

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });

  it('[FR-007-02] le simulateur est prérempli à 10000, 3.50 et 36, chaque champ relié à son libellé', async () => {
    connecter();
    const { container } = rendreApplication('/prets');
    await screen.findByTestId('pret-sim-montant');

    expect(screen.getByText('Simuler un emprunt')).toBeTruthy();
    for (const [testId, libelle, valeur] of [
      ['pret-sim-montant', 'Montant (EUR)', '10000'],
      ['pret-sim-taux', 'Taux annuel (%)', '3.50'],
      ['pret-sim-duree', 'Duree (mois)', '36'],
    ]) {
      expect(champ(testId).value).toBe(valeur);
      expect(container.querySelector(`label[for="${testId}"]`)?.textContent).toBe(libelle);
      expect(champ(testId).id).toBe(testId);
    }
    const bouton = screen.getByTestId('pret-sim-calculer') as HTMLButtonElement;
    expect(bouton.textContent).toBe('Calculer');
    expect(bouton.type).toBe('submit');
    expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
    expect(container.querySelector('dt')).toBeNull();
  });

  it('[FR-007-03] « Calculer » affiche mensualité, coût total, intérêts et l’échéancier repliable', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const { simuler, container } = rendreApplication('/prets', undefined, undefined, undefined, undefined, { simuler: simulationReussie });

    await utilisateur.click(await screen.findByTestId('pret-sim-calculer'));

    expect(await screen.findByText('293.02 EUR')).toBeTruthy();
    expect(screen.getByText('10,548.72 EUR')).toBeTruthy();
    expect(screen.getByText('548.72 EUR')).toBeTruthy();
    for (const etiquette of ['Mensualite', 'Cout total', 'Total interets']) {
      expect(screen.getByText(etiquette, { selector: 'dt' })).toBeTruthy();
    }
    expect(simuler).toHaveBeenCalledWith(expect.any(String), { montant: '10000', tauxAnnuel: '3.50', dureeMois: '36' });

    const lien = screen.getByTestId('pret-sim-echeancier-lien');
    expect(lien.textContent).toBe('Voir l echeancier complet (36 echeances)');
    const idBloc = lien.getAttribute('aria-controls') as string;
    const bloc = container.ownerDocument.getElementById(idBloc) as HTMLElement;
    expect(lien.getAttribute('aria-expanded')).toBe('false');
    expect(bloc.hidden).toBe(true);

    await utilisateur.click(lien);

    expect(lien.getAttribute('aria-expanded')).toBe('true');
    expect(bloc.hidden).toBe(false);
    expect(within(bloc).getAllByRole('columnheader').map((c) => c.textContent)).toEqual([
      '#',
      'Capital',
      'Interets',
      'Total',
      'Capital restant',
    ]);
    expect(within(bloc).getAllByRole('row')).toHaveLength(37);
    expect(within(bloc).getAllByRole('row')[1]?.textContent).toBe('1263.8529.17293.029,736.15');

    await utilisateur.click(lien);
    expect(lien.getAttribute('aria-expanded')).toBe('false');
  });

  it('[FR-007-03] modifier la saisie change l’appel au service ; un nouveau calcul replie l’échéancier', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const { simuler } = rendreApplication('/prets', undefined, undefined, undefined, undefined, { simuler: simulationReussie });
    await screen.findByTestId('pret-sim-calculer');

    for (const [testId, valeur] of [
      ['pret-sim-montant', '12000'],
      ['pret-sim-taux', '0'],
      ['pret-sim-duree', '24'],
    ]) {
      await utilisateur.clear(champ(testId));
      await utilisateur.type(champ(testId), valeur as string);
    }
    await utilisateur.type(champ('pret-sim-duree'), '{Enter}');

    await screen.findByTestId('pret-sim-echeancier-lien');
    expect(simuler).toHaveBeenLastCalledWith(expect.any(String), { montant: '12000', tauxAnnuel: '0', dureeMois: '24' });
    await utilisateur.click(screen.getByTestId('pret-sim-echeancier-lien'));
    await utilisateur.click(screen.getByTestId('pret-sim-calculer'));
    await waitFor(() => expect(screen.getByTestId('pret-sim-echeancier-lien').getAttribute('aria-expanded')).toBe('false'));
  });

  it.each(['Taux invalide (0-30%)', 'Duree invalide (1-360 mois)', 'Montant invalide'])(
    '[FR-007-02][FR-004-05] le refus « %s » du service s’affiche sans résultat',
    async (message) => {
      connecter();
      const utilisateur = userEvent.setup();
      rendreApplication('/prets', undefined, undefined, undefined, undefined, {
        simuler: () => Promise.resolve<ReponseSimulation>({ statut: 'refusee', message }),
      });

      await utilisateur.click(await screen.findByTestId('pret-sim-calculer'));

      const alerte = await screen.findByTestId('pret-message-erreur');
      expect(alerte.getAttribute('role')).toBe('alert');
      expect(alerte.textContent).toBe(message);
      expect(screen.queryByTestId('pret-sim-echeancier-lien')).toBeNull();
      expect(screen.queryByText('Mensualite', { selector: 'dt' })).toBeNull();
    },
  );

  it('[FR-007-03] un refus efface le résultat précédent, un nouveau calcul efface le message', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const reponses: ReponseSimulation[] = [
      { statut: 'simulation', simulation: SIMULATION },
      { statut: 'refusee', message: 'Montant invalide' },
      { statut: 'simulation', simulation: SIMULATION },
    ];
    rendreApplication('/prets', undefined, undefined, undefined, undefined, { simuler: () => Promise.resolve(reponses.shift() as ReponseSimulation) });
    const calculer = await screen.findByTestId('pret-sim-calculer');

    await utilisateur.click(calculer);
    await screen.findByTestId('pret-sim-echeancier-lien');
    await utilisateur.click(calculer);
    await screen.findByTestId('pret-message-erreur');
    expect(screen.queryByTestId('pret-sim-echeancier-lien')).toBeNull();
    await utilisateur.click(calculer);
    await screen.findByTestId('pret-sim-echeancier-lien');
    expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
  });

  it('[FR-007-03] une panne du service affiche le texte fixe pret.erreur.simulation, sans détail', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const { container } = rendreApplication('/prets', undefined, undefined, undefined, undefined, {
      simuler: () => Promise.resolve<ReponseSimulation>({ statut: 'technique' }),
    });

    await utilisateur.click(await screen.findByTestId('pret-sim-calculer'));

    const alerte = await screen.findByTestId('pret-message-erreur');
    expect(alerte.textContent).toBe("Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer.");
    expect(container.textContent).not.toMatch(/Exception|TypeError|at \w+\.|EcranPrets/);
  });

  it('[FR-007-01] une session perdue pendant la simulation mène à /login', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const { routeur } = rendreApplication('/prets', undefined, undefined, undefined, undefined, {
      simuler: () => Promise.resolve<ReponseSimulation>({ statut: 'nonAuthentifie' }),
    });

    await utilisateur.click(await screen.findByTestId('pret-sim-calculer'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-007-04] le formulaire « Nouveau pret » propose les cinq types, Consommation par défaut, et reste inerte', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const { container, simuler } = rendreApplication('/prets');
    await screen.findByTestId('pret-type');

    expect(screen.getByText('Nouveau pret')).toBeTruthy();
    const type = screen.getByTestId('pret-type') as HTMLSelectElement;
    expect(type.tagName).toBe('SELECT');
    expect(Array.from(type.options).map((option) => option.textContent)).toEqual([
      'Immobilier',
      'Automobile',
      'Consommation',
      'Travaux',
      'Etudiant',
    ]);
    expect(type.value).toBe('CONSOMMATION');
    expect(container.querySelector('label[for="pret-type"]')?.textContent).toBe('Type de pret');
    for (const [testId, libelle, valeur] of [
      ['pret-montant', 'Montant (EUR)', ''],
      ['pret-taux', 'Taux annuel (%)', '3.50'],
      ['pret-duree', 'Duree (mois)', '36'],
      ['pret-objet', 'Objet du pret', ''],
    ]) {
      expect(champ(testId).value).toBe(valeur);
      expect(container.querySelector(`label[for="${testId}"]`)?.textContent).toBe(libelle);
    }
    const soumettre = screen.getByTestId('pret-soumettre') as HTMLButtonElement;
    expect(soumettre.textContent).toBe('Soumettre la demande');
    expect(soumettre.type).toBe('button');

    await utilisateur.selectOptions(type, 'TRAVAUX');
    await utilisateur.type(champ('pret-montant'), '5000');
    await utilisateur.type(champ('pret-objet'), 'Travaux{Enter}');
    await utilisateur.clear(champ('pret-taux'));
    await utilisateur.type(champ('pret-taux'), '4.9');
    await utilisateur.clear(champ('pret-duree'));
    await utilisateur.type(champ('pret-duree'), '24');
    await utilisateur.click(soumettre);

    expect(type.value).toBe('TRAVAUX');
    expect(champ('pret-montant').value).toBe('5000');
    expect(simuler).not.toHaveBeenCalled();
    expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
  });

  it('[FR-007-08] liste les prêts avec leurs colonnes, montants, taux, durées et statuts en étiquettes', async () => {
    connecter();
    const { lister } = rendreApplication('/prets', undefined, undefined, undefined, undefined, {
      lister: listePrets([pret(2, 'EN_ATTENTE', { type: 'AUTO', montant: '9000.00', tauxAnnuel: '3.9', dureeMois: 6 }), pret(1, 'EN_COURS')]),
    });

    const statut = await screen.findByTestId('pret-statut-1');
    expect(lister).toHaveBeenCalledOnce();
    expect(screen.getByText('Emprunts en cours')).toBeTruthy();
    const table = tableEmprunts();
    expect(within(table).getAllByRole('columnheader').map((c) => c.textContent)).toEqual([
      'Type',
      'Montant',
      'Taux',
      'Duree',
      'Mensualite',
      'Total interets',
      'Statut',
      'Echeancier',
      'Action',
    ]);
    const lignes = within(table).getAllByRole('row');
    expect(lignes).toHaveLength(3);
    expect(lignes[1]?.textContent).toBe('AutomobileEUR9,000.003.9000%6 moisEUR218.63EUR247.12En attente d approbationEn cours de traitement...');
    expect(lignes[2]?.textContent).toBe('TravauxEUR5,000.004.9000%24 moisEUR218.63EUR247.12En cours');
    expect(statut.textContent).toBe('En cours');
    expect(statut.className).toBe('etiquette');
    expect(screen.getByTestId('pret-statut-2').textContent).toBe('En attente d approbation');
    expect(screen.queryByText('Aucun emprunt enregistre')).toBeNull();
    expect(screen.getAllByText('En cours de traitement...')).toHaveLength(1);
  });

  it.each([
    ['REMBOURSE', 'Rembourse'],
    ['REFUSE', 'Refuse par le service credit'],
    ['DEFAUT', 'En defaut'],
  ] as const)('[FR-007-08] le statut %s est libellé « %s » en étiquette, sans texte de traitement', async (statut, libelle) => {
    connecter();
    rendreApplication('/prets', undefined, undefined, undefined, undefined, { lister: listePrets([pret(5, statut)]) });

    const etiquette = await screen.findByTestId('pret-statut-5');

    expect(etiquette.textContent).toBe(libelle);
    expect(screen.queryByText('En cours de traitement...')).toBeNull();
  });

  it('[FR-007-08] sans prêt : ligne « Aucun emprunt enregistre » sous les en-têtes conservés', async () => {
    connecter();
    rendreApplication('/prets');

    const texte = await screen.findByText('Aucun emprunt enregistre');

    expect(texte.tagName).toBe('TD');
    expect(texte.getAttribute('colspan')).toBe('9');
    const table = tableEmprunts();
    expect(within(table).getAllByRole('columnheader')).toHaveLength(9);
  });

  it('[FR-007-08] avant la réponse, ni lignes ni texte d’état vide', async () => {
    connecter();
    rendreApplication('/prets', undefined, undefined, undefined, undefined, { lister: () => new Promise(() => {}) });

    await screen.findByTestId('pret-sim-calculer');

    expect(screen.queryByText('Aucun emprunt enregistre')).toBeNull();
    expect(within(tableEmprunts()).getAllByRole('row')).toHaveLength(1);
  });

  it('[FR-007-08] une panne de la lecture affiche l’erreur technique sans détail, les en-têtes restent', async () => {
    connecter();
    rendreApplication('/prets', undefined, undefined, undefined, undefined, {
      lister: () => Promise.resolve<ReponsePrets>({ statut: 'technique' }),
    });

    const alerte = await screen.findByTestId('pret-message-erreur');

    expect(alerte.textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByText('Aucun emprunt enregistre')).toBeNull();
    expect(within(tableEmprunts()).getAllByRole('columnheader')).toHaveLength(9);
  });
});
