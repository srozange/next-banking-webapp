// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { Echeance, Pret, Simulation } from '../../../domain/pret';
import type {
  ReponseAction,
  ReponseDepot,
  ReponseEcheances,
  ReponsePrets,
  ReponseSimulation,
} from '../../../application/prets';
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
    const { routeur } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
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
    const { simuler, container } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { simuler: simulationReussie });

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
    const { simuler } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { simuler: simulationReussie });
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
      rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
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
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { simuler: () => Promise.resolve(reponses.shift() as ReponseSimulation) });
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
    const { container } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
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
    const { routeur } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      simuler: () => Promise.resolve<ReponseSimulation>({ statut: 'nonAuthentifie' }),
    });

    await utilisateur.click(await screen.findByTestId('pret-sim-calculer'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-007-04] le formulaire « Nouveau pret » propose les cinq types, Consommation par défaut, et ses libellés', async () => {
    connecter();
    const { container } = rendreApplication('/prets');
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
    expect(soumettre.type).toBe('submit');
    expect(soumettre.disabled).toBe(false);
    expect(screen.queryByTestId('pret-message-info')).toBeNull();
    expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
  });

  async function remplirDemande(utilisateur: ReturnType<typeof userEvent.setup>) {
    await utilisateur.selectOptions(await screen.findByTestId('pret-type'), 'TRAVAUX');
    await utilisateur.type(champ('pret-montant'), '5000');
    await utilisateur.type(champ('pret-objet'), 'Travaux');
    await utilisateur.clear(champ('pret-taux'));
    await utilisateur.type(champ('pret-taux'), '4.9');
    await utilisateur.clear(champ('pret-duree'));
    await utilisateur.type(champ('pret-duree'), '300');
  }

  const DEMANDE_SAISIE = { type: 'TRAVAUX', montant: '5000', tauxAnnuel: '4.9', dureeMois: '300', objet: 'Travaux' };

  it('[FR-007-07] soumettre envoie la saisie, affiche le message d’information et relit la liste', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    let listes = 0;
    const { deposer, lister } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      lister: () => {
        listes += 1;
        return Promise.resolve<ReponsePrets>({
          statut: 'prets',
          prets: listes === 1 ? [] : [pret(3, 'EN_ATTENTE', { objet: 'Travaux' })],
        });
      },
      deposer: () => Promise.resolve<ReponseDepot>({ statut: 'deposee', pret: pret(3, 'EN_ATTENTE') }),
    });
    await remplirDemande(utilisateur);

    await utilisateur.click(screen.getByTestId('pret-soumettre'));

    const info = await screen.findByTestId('pret-message-info');
    expect(info.textContent).toBe(
      'Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes)',
    );
    expect(info.getAttribute('role')).toBe('status');
    expect(deposer).toHaveBeenCalledWith(expect.any(String), DEMANDE_SAISIE);
    expect(await screen.findByTestId('pret-statut-3')).toBeTruthy();
    expect(lister).toHaveBeenCalledTimes(2);
    expect(within(tableEmprunts()).getByText('En attente d approbation')).toBeTruthy();
    expect(within(tableEmprunts()).getByText('En cours de traitement...')).toBeTruthy();
    expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
  });

  it('[FR-007-07] pendant l’envoi le bouton est désactivé avec aria-busy', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    let terminer: (reponse: ReponseDepot) => void = () => undefined;
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      deposer: () => new Promise<ReponseDepot>((resolve) => (terminer = resolve)),
    });
    await remplirDemande(utilisateur);

    await utilisateur.click(screen.getByTestId('pret-soumettre'));

    const bouton = screen.getByTestId('pret-soumettre') as HTMLButtonElement;
    expect(bouton.disabled).toBe(true);
    expect(bouton.getAttribute('aria-busy')).toBe('true');

    terminer({ statut: 'deposee', pret: pret(3, 'EN_ATTENTE') });

    await waitFor(() => expect(bouton.disabled).toBe(false));
    expect(bouton.getAttribute('aria-busy')).toBeNull();
  });

  it.each([
    ['400', 'Duree invalide'],
    ['503', 'Service credit temporairement indisponible, veuillez reessayer plus tard'],
  ])('[FR-007-05] un refus %s affiche le message du service, conserve la saisie, sans message d’information', async (_statut, message) => {
    connecter();
    const utilisateur = userEvent.setup();
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      deposer: () => Promise.resolve<ReponseDepot>({ statut: 'refusee', message }),
    });
    await remplirDemande(utilisateur);

    await utilisateur.click(screen.getByTestId('pret-soumettre'));

    const erreur = await screen.findByTestId('pret-message-erreur');
    expect(erreur.textContent).toBe(message);
    expect(screen.queryByTestId('pret-message-info')).toBeNull();
    expect((screen.getByTestId('pret-type') as HTMLSelectElement).value).toBe('TRAVAUX');
    expect(champ('pret-montant').value).toBe('5000');
    expect(champ('pret-taux').value).toBe('4.9');
    expect(champ('pret-duree').value).toBe('300');
    expect(champ('pret-objet').value).toBe('Travaux');
  });

  it('[FR-024-02] une panne technique affiche le texte fixe de la demande, sans détail', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    rendreApplication('/prets');
    await remplirDemande(utilisateur);

    await utilisateur.click(screen.getByTestId('pret-soumettre'));

    expect((await screen.findByTestId('pret-message-erreur')).textContent).toBe(
      "Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer.",
    );
    expect(screen.queryByTestId('pret-message-info')).toBeNull();
  });

  it('[FR-024-02] un échec après un succès remplace le message d’information par l’erreur, et inversement', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const reponses: ReponseDepot[] = [
      { statut: 'deposee', pret: pret(3, 'EN_ATTENTE') },
      { statut: 'refusee', message: 'Taux invalide' },
      { statut: 'deposee', pret: pret(4, 'EN_ATTENTE') },
    ];
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      deposer: () => Promise.resolve(reponses.shift() as ReponseDepot),
    });
    await remplirDemande(utilisateur);

    await utilisateur.click(screen.getByTestId('pret-soumettre'));
    await screen.findByTestId('pret-message-info');
    await utilisateur.click(screen.getByTestId('pret-soumettre'));
    await screen.findByTestId('pret-message-erreur');
    expect(screen.queryByTestId('pret-message-info')).toBeNull();
    await utilisateur.click(screen.getByTestId('pret-soumettre'));
    await screen.findByTestId('pret-message-info');

    expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
  });

  it('[FR-007-01] une session perdue pendant l’envoi purge le jeton et mène à /login', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const { routeur } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      deposer: () => Promise.resolve<ReponseDepot>({ statut: 'nonAuthentifie' }),
    });
    await remplirDemande(utilisateur);

    await utilisateur.click(screen.getByTestId('pret-soumettre'));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
  });

  it('[FR-007-07] une relecture de liste en panne après un dépôt affiche l’erreur technique de la liste, puis la liste revenue l’efface', async () => {
    connecter();
    const utilisateur = userEvent.setup();
    const listes: ReponsePrets[] = [
      { statut: 'prets', prets: [] },
      { statut: 'technique' },
      { statut: 'prets', prets: [pret(3, 'EN_ATTENTE')] },
    ];
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      lister: () => Promise.resolve(listes.shift() as ReponsePrets),
      deposer: () => Promise.resolve<ReponseDepot>({ statut: 'deposee', pret: pret(3, 'EN_ATTENTE') }),
    });
    await remplirDemande(utilisateur);

    await utilisateur.click(screen.getByTestId('pret-soumettre'));
    await waitFor(() => expect(screen.getAllByTestId('pret-message-erreur')).toHaveLength(1));
    await utilisateur.click(screen.getByTestId('pret-soumettre'));

    expect(await screen.findByTestId('pret-statut-3')).toBeTruthy();
    expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
  });

  it('[FR-007-08] liste les prêts avec leurs colonnes, montants, taux, durées et statuts en étiquettes', async () => {
    connecter();
    const { lister } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
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
    expect(lignes[1]?.textContent).toBe('AutomobileEUR9,000.003.9000%6 moisEUR218.63EUR247.12En attente d approbationEn cours de traitement...Supprimer');
    expect(lignes[2]?.textContent).toBe('TravauxEUR5,000.004.9000%24 moisEUR218.63EUR247.12En coursVoirSupprimer');
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
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { lister: listePrets([pret(5, statut)]) });

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
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { lister: () => new Promise(() => {}) });

    await screen.findByTestId('pret-sim-calculer');

    expect(screen.queryByText('Aucun emprunt enregistre')).toBeNull();
    expect(within(tableEmprunts()).getAllByRole('row')).toHaveLength(1);
  });

  it('[FR-007-08] une panne de la lecture affiche l’erreur technique sans détail, les en-têtes restent', async () => {
    connecter();
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      lister: () => Promise.resolve<ReponsePrets>({ statut: 'technique' }),
    });

    const alerte = await screen.findByTestId('pret-message-erreur');

    expect(alerte.textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByText('Aucun emprunt enregistre')).toBeNull();
    expect(within(tableEmprunts()).getAllByRole('columnheader')).toHaveLength(9);
  });

  describe('échéancier, paiement et suppression', () => {
    function echeance(id: number, numero: number, paye: boolean, extra: Partial<Echeance> = {}): Echeance {
      return {
        id,
        numero,
        dateEcheance: `2026-${String(numero + 6).padStart(2, '0')}-01`,
        capital: '407.39',
        interets: '20.42',
        total: '427.81',
        capitalRestant: '4592.61',
        paye,
        datePaiement: paye ? '2026-07-01T08:00:00Z' : null,
        ...extra,
      };
    }

    const reponseEcheances = (echeances: Echeance[]) => () =>
      Promise.resolve<ReponseEcheances>({ statut: 'echeances', echeances });
    const faite = () => Promise.resolve<ReponseAction>({ statut: 'faite' });
    const panne = () => Promise.resolve<ReponseAction>({ statut: 'technique' });
    const ECHEANCES = [echeance(1, 1, true), echeance(2, 2, false), echeance(3, 3, false, { total: '1517.11' })];

    it('[FR-007-11] « Voir » n’existe que pour les prêts EN_COURS et REMBOURSE, « Supprimer » sur chaque ligne', async () => {
      connecter();
      rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: listePrets([
          pret(1, 'EN_COURS'),
          pret(2, 'REMBOURSE'),
          pret(3, 'EN_ATTENTE'),
          pret(4, 'REFUSE'),
          pret(5, 'DEFAUT'),
        ]),
      });

      await screen.findByTestId('pret-statut-1');

      expect(screen.getByTestId('pret-voir-1').textContent).toBe('Voir');
      expect(screen.getByTestId('pret-voir-2')).toBeTruthy();
      for (const id of [3, 4, 5]) {
        expect(screen.queryByTestId(`pret-voir-${id}`)).toBeNull();
      }
      for (const id of [1, 2, 3, 4, 5]) {
        expect(screen.getByTestId(`pret-supprimer-${id}`).textContent).toBe('Supprimer');
      }
      expect(screen.getByText('En cours de traitement...')).toBeTruthy();
    });

    it('[FR-007-11] « Voir » ouvre la section sous la liste, focus au titre, colonnes, dates jj/MM/aaaa et statuts', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      const { container, echeances } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: listePrets([pret(1, 'EN_COURS')]),
        echeances: reponseEcheances(ECHEANCES),
      });
      expect(container.querySelector('.section-echeancier')).toBeNull();

      await utilisateur.click(await screen.findByTestId('pret-voir-1'));

      const titre = await screen.findByRole('heading', { name: 'Detail des echeances' });
      expect(echeances).toHaveBeenCalledWith(expect.any(String), 1);
      expect(document.activeElement).toBe(titre);
      expect(container.querySelector('.card-muted')?.contains(titre)).toBe(true);
      const separateurs = Array.from(container.querySelectorAll('h2')).map((h) => h.textContent);
      expect(separateurs).toEqual(['Simulateur de pret', 'Demande de pret', 'Mes emprunts', 'Echeancier du pret selectionne']);
      const tableau = within(screen.getByRole('region', { name: 'Detail des echeances' })).getByRole('table');
      expect(within(tableau).getAllByRole('columnheader').map((th) => th.textContent)).toEqual([
        '#',
        'Date echeance',
        'Capital',
        'Interets',
        'Total',
        'Capital restant',
        'Statut',
        'Action',
      ]);
      const lignes = within(tableau).getAllByRole('row').slice(1);
      expect(lignes.map((l) => within(l).getAllByRole('cell').map((c) => c.textContent))).toEqual([
        ['1', '01/07/2026', '407.39', '20.42', '427.81', '4,592.61', 'Paye', ''],
        ['2', '01/08/2026', '407.39', '20.42', '427.81', '4,592.61', 'En attente', 'Marquer paye'],
        ['3', '01/09/2026', '407.39', '20.42', '1,517.11', '4,592.61', 'En attente', 'Marquer paye'],
      ]);
      expect(screen.queryByTestId('pret-marquer-paye-1')).toBeNull();
      expect(screen.getByTestId('pret-marquer-paye-2')).toBeTruthy();
    });

    it('[FR-007-11] une lecture d’échéancier en panne affiche l’erreur technique sans ouvrir la section', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { lister: listePrets([pret(1, 'EN_COURS')]) });

      await utilisateur.click(await screen.findByTestId('pret-voir-1'));

      expect((await screen.findByTestId('pret-message-erreur')).textContent).toBe('Une erreur technique est survenue');
      expect(screen.queryByText('Detail des echeances')).toBeNull();
    });

    it('[FR-007-11] une session perdue à la lecture de l’échéancier mène à /login', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      const { routeur } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: listePrets([pret(1, 'EN_COURS')]),
        echeances: () => Promise.resolve<ReponseEcheances>({ statut: 'nonAuthentifie' }),
      });

      await utilisateur.click(await screen.findByTestId('pret-voir-1'));

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
      expect(localStorage.getItem('nb.jeton')).toBeNull();
    });

    it('[FR-007-12] « Marquer paye » paie l’échéance, affiche le succès et relit l’échéancier et la liste', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      const echeancier = [
        [echeance(1, 1, false)],
        [echeance(1, 1, true, { datePaiement: '2026-10-06T08:30:01Z' })],
      ];
      const listes = [[pret(1, 'EN_COURS')], [pret(1, 'REMBOURSE')]];
      const { payer, lister, echeances } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: () => Promise.resolve<ReponsePrets>({ statut: 'prets', prets: listes.shift() as Pret[] }),
        echeances: () => Promise.resolve<ReponseEcheances>({ statut: 'echeances', echeances: echeancier.shift() as Echeance[] }),
        payer: faite,
      });
      await utilisateur.click(await screen.findByTestId('pret-voir-1'));

      await utilisateur.click(await screen.findByTestId('pret-marquer-paye-1'));

      expect((await screen.findByTestId('pret-message-succes')).textContent).toBe('Echeance marquee comme payee');
      expect(payer).toHaveBeenCalledWith(expect.any(String), 1);
      await waitFor(() => expect(screen.queryByTestId('pret-marquer-paye-1')).toBeNull());
      expect(screen.getByTestId('pret-statut-1').textContent).toBe('Rembourse');
      expect(echeances).toHaveBeenCalledTimes(2);
      expect(lister).toHaveBeenCalledTimes(2);
      expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
    });

    it('[FR-007-12] une erreur de paiement affiche le texte fixe, sans succès, et laisse l’échéancier', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      const { lister } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: listePrets([pret(1, 'EN_COURS')]),
        echeances: reponseEcheances(ECHEANCES),
        payer: panne,
      });
      await utilisateur.click(await screen.findByTestId('pret-voir-1'));

      await utilisateur.click(await screen.findByTestId('pret-marquer-paye-2'));

      expect((await screen.findByTestId('pret-message-erreur')).textContent).toBe(
        'Erreur : l\'échéance n\'a pas été marquée comme payée. Veuillez réessayer.',
      );
      expect(screen.queryByTestId('pret-message-succes')).toBeNull();
      expect(screen.getByTestId('pret-marquer-paye-2')).toBeTruthy();
      expect(lister).toHaveBeenCalledTimes(1);
    });

    it('[FR-007-12] une session perdue au paiement mène à /login', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      const { routeur } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: listePrets([pret(1, 'EN_COURS')]),
        echeances: reponseEcheances(ECHEANCES),
        payer: () => Promise.resolve<ReponseAction>({ statut: 'nonAuthentifie' }),
      });
      await utilisateur.click(await screen.findByTestId('pret-voir-1'));

      await utilisateur.click(await screen.findByTestId('pret-marquer-paye-2'));

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    });

    it('[FR-007-12] un succès de paiement puis une erreur de suppression remplacent le message', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: listePrets([pret(1, 'EN_COURS')]),
        echeances: reponseEcheances(ECHEANCES),
        payer: faite,
        supprimer: panne,
      });
      await utilisateur.click(await screen.findByTestId('pret-voir-1'));
      await utilisateur.click(await screen.findByTestId('pret-marquer-paye-2'));
      await screen.findByTestId('pret-message-succes');

      await utilisateur.click(screen.getByTestId('pret-supprimer-1'));

      expect((await screen.findByTestId('pret-message-erreur')).textContent).toBe(
        'Erreur suppression : le prêt n\'a pas été supprimé. Veuillez réessayer.',
      );
      expect(screen.queryByTestId('pret-message-succes')).toBeNull();
    });

    it('[FR-007-13] « Supprimer » relit la liste et ferme la section de ce prêt', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      const listes = [[pret(1, 'EN_COURS'), pret(2, 'EN_COURS')], [pret(2, 'EN_COURS')]];
      const { supprimer } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: () => Promise.resolve<ReponsePrets>({ statut: 'prets', prets: listes.shift() as Pret[] }),
        echeances: reponseEcheances(ECHEANCES),
        supprimer: faite,
      });
      await utilisateur.click(await screen.findByTestId('pret-voir-1'));
      await screen.findByText('Detail des echeances');

      await utilisateur.click(screen.getByTestId('pret-supprimer-1'));

      await waitFor(() => expect(screen.queryByTestId('pret-statut-1')).toBeNull());
      expect(supprimer).toHaveBeenCalledWith(expect.any(String), 1);
      expect(screen.getByTestId('pret-statut-2')).toBeTruthy();
      expect(screen.queryByText('Detail des echeances')).toBeNull();
      expect(screen.queryByTestId('pret-message-erreur')).toBeNull();
    });

    it('[FR-007-13] supprimer un autre prêt laisse l’échéancier ouvert', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      const listes = [[pret(1, 'EN_COURS'), pret(2, 'EN_COURS')], [pret(1, 'EN_COURS')]];
      rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: () => Promise.resolve<ReponsePrets>({ statut: 'prets', prets: listes.shift() as Pret[] }),
        echeances: reponseEcheances(ECHEANCES),
        supprimer: faite,
      });
      await utilisateur.click(await screen.findByTestId('pret-voir-1'));
      await screen.findByText('Detail des echeances');

      await utilisateur.click(screen.getByTestId('pret-supprimer-2'));

      await waitFor(() => expect(screen.queryByTestId('pret-statut-2')).toBeNull());
      expect(screen.getByText('Detail des echeances')).toBeTruthy();
    });

    it('[FR-007-13] une erreur de suppression affiche le texte fixe et garde la ligne', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: listePrets([pret(1, 'EN_ATTENTE')]),
        supprimer: panne,
      });

      await utilisateur.click(await screen.findByTestId('pret-supprimer-1'));

      expect((await screen.findByTestId('pret-message-erreur')).textContent).toBe(
        'Erreur suppression : le prêt n\'a pas été supprimé. Veuillez réessayer.',
      );
      expect(screen.getByTestId('pret-statut-1')).toBeTruthy();
    });

    it('[FR-007-13] une session perdue à la suppression mène à /login', async () => {
      connecter();
      const utilisateur = userEvent.setup();
      const { routeur } = rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lister: listePrets([pret(1, 'EN_ATTENTE')]),
        supprimer: () => Promise.resolve<ReponseAction>({ statut: 'nonAuthentifie' }),
      });

      await utilisateur.click(await screen.findByTestId('pret-supprimer-1'));

      await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    });
  });
});
