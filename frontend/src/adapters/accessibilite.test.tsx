// @vitest-environment jsdom
import { cleanup, render, screen, waitFor } from '@testing-library/react';
import axe from 'axe-core';
import { afterEach, beforeAll, describe, expect, it, vi } from 'vitest';
import type { ReponseSimulation } from '../application/prets';
import { jetonDeTest } from './jeton/jetonDeTest';
import { rendreApplication } from './test/rendreApplication';
// index.html est hors de src : son import brut ne fausse pas la couverture.
import indexHtml from '../../index.html?raw';

beforeAll(() => {
  // Le document réel est index.html : sa langue est reprise telle quelle dans le document de test.
  const langue = /<html[^>]*\slang="([^"]+)"/.exec(indexHtml)?.[1];
  expect(langue).toBe('fr');
  document.documentElement.lang = langue as string;
  // jsdom n'a pas de canvas : axe s'en passe, sans le message « Not implemented » à chaque appel.
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
});

afterEach(() => {
  cleanup();
  localStorage.clear();
});

/** Identifiants des règles axe violées par le document courant. */
async function reglesViolees(): Promise<string[]> {
  const resultat = await axe.run(document, { resultTypes: ['violations'] });
  return resultat.violations.map((violation) => violation.id);
}

describe('accessibilité (axe)', () => {
  it('[axe] /login ne relève aucune violation', async () => {
    rendreApplication('/login');
    await screen.findByTestId('login-identifiant');

    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /login avec les erreurs de champ et le message de refus ne relève aucune violation', async () => {
    const { default: userEvent } = await import('@testing-library/user-event');
    const utilisateur = userEvent.setup();
    rendreApplication('/login');

    await utilisateur.type(screen.getByTestId('login-identifiant'), ' {Enter}');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.type(screen.getByTestId('login-identifiant'), 'user1');
    await utilisateur.type(screen.getByTestId('login-mot-de-passe'), 'faux{Enter}');
    await screen.findByTestId('login-message-erreur');
    expect(await reglesViolees()).toEqual([]);
  });

  it.each([
    ['CLIENT', '/tableau-de-bord'],
    ['ADMIN', '/administration'],
  ])('[axe] %s sur %s (en-tête, badge, sous-en-tête) ne relève aucune violation', async (role, chemin) => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: [role] }));
    rendreApplication(chemin);
    await screen.findByTestId('nav-salutation');
    await waitFor(() => expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBeNull());

    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /administration avec pipeline, historique et message de décision des prêts ne relève aucune violation', async () => {
    const { default: userEvent } = await import('@testing-library/user-event');
    const utilisateur = userEvent.setup();
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
    const pret = {
      id: 42,
      clientId: 1,
      nomClient: 'Jean Dupont',
      type: 'CONSOMMATION' as const,
      montant: '10000.00',
      statut: 'EN_ATTENTE' as const,
      dureeMois: 36,
      tauxAnnuel: '3.5000',
      mensualite: '293.02',
      objet: 'Voiture',
      dateDebut: '2026-10-01',
    };
    rendreApplication('/administration', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      lireEnAttente: () => Promise.resolve({ statut: 'liste' as const, prets: [pret] }),
      lireHistorique: () => Promise.resolve({ statut: 'liste' as const, prets: [pret] }),
      decider: () => Promise.resolve({ statut: 'technique' as const }),
    });
    await screen.findByTestId('admin-approuver-42');
    await waitFor(() => expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBeNull());
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('admin-approuver-42'));
    await screen.findByTestId('admin-message-erreur');
    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /administration avec indicateurs, gestion des clients, détail ouvert et tous les utilisateurs ne relève aucune violation', async () => {
    const { default: userEvent } = await import('@testing-library/user-event');
    const utilisateur = userEvent.setup();
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
    const ok = <T,>(donnees: T) => Promise.resolve({ statut: 'ok' as const, donnees });
    rendreApplication('/administration', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      lireIndicateursIdentite: () => ok({ totalClients: 4, clientsActifs: 3, conseillers: 2, totalUtilisateurs: 7 }),
      lireIndicateursPrets: () => Promise.resolve({ statut: 'erreur' as const, message: 'Service crédits indisponible' }),
      lireClients: () =>
        ok([
          {
            clientId: 1,
            nomComplet: 'Jean Dupont',
            login: 'user1',
            email: 'jean.dupont@example.com',
            telephone: null,
            role: 'CLIENT',
            actif: false,
            nomConseiller: 'Sophie Durand',
            dateCreation: '2024-02-01',
          },
        ]),
      lireUtilisateurs: () => ok([{ utilisateurId: 10, nomComplet: 'Alice Martin', login: 'admin', email: 'alice.martin@demobank.fr', role: 'ADMIN', actif: true }]),
    });
    await screen.findByTestId('admin-detail-1');
    await waitFor(() => expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBeNull());
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('admin-detail-1'));
    await screen.findByTestId('admin-panneau-detail');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('admin-activer-1'));
    await screen.findByTestId('admin-message-succes');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.type(screen.getByTestId('admin-recherche'), 'zzz{Enter}');
    await screen.findByText('Aucun client trouve');
    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /tableau-de-bord avec des notifications lues et non lues (badge, pastille, boutons) ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    rendreApplication('/tableau-de-bord', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      lister: () =>
        Promise.resolve({
          statut: 'liste' as const,
          notifications: [
            { id: 2, type: 'FRAUDE', titre: null, message: 'Virement suspendu', creeLe: '2026-11-17T11:00:00Z', lue: false },
            { id: 1, type: 'ALERTE', titre: null, message: 'Virement important', creeLe: '2026-11-17T10:00:00Z', lue: true },
          ],
        }),
    });
    await screen.findByTestId('notifications-non-lu-2');
    await waitFor(() => expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBeNull());

    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /prets avec résultat de simulation, échéancier déplié, liste de prêts et erreur ne relève aucune violation', async () => {
    const { default: userEvent } = await import('@testing-library/user-event');
    const utilisateur = userEvent.setup();
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    const pret = {
      id: 1,
      type: 'TRAVAUX' as const,
      montant: '5000.00',
      tauxAnnuel: '4.9000',
      dureeMois: 24,
      mensualite: '218.63',
      coutTotal: '5247.12',
      totalInterets: '247.12',
      objet: null,
      statut: 'EN_ATTENTE' as const,
      dateDebut: '2026-08-01',
      dateFin: '2028-08-01',
    };
    const reponses = [
      { statut: 'simulation' as const, simulation: { mensualite: '293.02', coutTotal: '10548.72', totalInterets: '548.72', echeancier: [{ numero: 1, capital: '263.85', interets: '29.17', total: '293.02', capitalRestant: '0.00' }] } },
      { statut: 'technique' as const },
    ];
    rendreApplication('/prets', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      simuler: () => Promise.resolve(reponses.shift() as ReponseSimulation),
      lister: () => Promise.resolve({ statut: 'prets' as const, prets: [pret] }),
    });
    await screen.findByTestId('pret-statut-1');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('pret-sim-calculer'));
    await utilisateur.click(await screen.findByTestId('pret-sim-echeancier-lien'));
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('pret-sim-calculer'));
    await screen.findByTestId('pret-message-erreur');
    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /prets sans prêt (état vide) ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['ADMIN'] }));
    rendreApplication('/prets');
    await screen.findByText('Aucun emprunt enregistre');

    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] profil en attente (aria-busy) ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CONSEILLER'] }));
    rendreApplication('/espace-conseiller', undefined, () => new Promise(() => {}));

    expect(screen.getByTestId('application-racine').getAttribute('aria-busy')).toBe('true');
    expect(await reglesViolees()).toEqual([]);
  });

  describe('/espace-conseiller', () => {
    const client = (clientId: number, nomComplet: string, actif: boolean) => ({
      clientId, nomComplet, login: `user${clientId}`, email: `user${clientId}@example.com`, telephone: null, actif,
    });

    function ouvrirEspaceConseiller(vide: boolean) {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CONSEILLER'] }));
      const ok = <T,>(donnees: T) => Promise.resolve({ statut: 'ok' as const, donnees });
      return rendreApplication(
        '/espace-conseiller',
        undefined,
        () => Promise.resolve({ statut: 'profil', nomComplet: 'Sophie Durand' }),
        undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined,
        undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined,
        {
          lireIdentites: () => ok(vide ? [] : [client(1, 'Jean Dupont', true), client(2, 'Marie Lebrun', false)]),
          lireComptes: () => ok(vide ? [] : [{ clientId: 1, comptes: [{ compteId: 10, numero: 'FR76-0001-0001', type: 'COURANT', solde: '2130.00' }] }]),
          lirePrets: () =>
            ok(vide ? [] : [{ clientId: 1, prets: [{ pretId: 5, type: null, montant: '5000.00', mensualite: '146.51', statut: 'DEFAUT' }] }]),
        },
      );
    }

    async function charge() {
      await screen.findByTestId('nav-salutation');
      await waitFor(() => expect(screen.getByTestId('conseiller-contenu').getAttribute('aria-busy')).toBeNull());
    }

    it('[axe] /espace-conseiller chargé (indicateurs, cartes, client inactif) ne relève aucune violation', async () => {
      ouvrirEspaceConseiller(false);
      await charge();

      expect(await reglesViolees()).toEqual([]);
    });

    it('[axe] /espace-conseiller vide ne relève aucune violation', async () => {
      ouvrirEspaceConseiller(true);
      await charge();
      await screen.findByTestId('conseiller-vide');

      expect(await reglesViolees()).toEqual([]);
    });

    it('[axe] /espace-conseiller avec le panneau de détail ouvert ne relève aucune violation', async () => {
      const { default: userEvent } = await import('@testing-library/user-event');
      const utilisateur = userEvent.setup();
      ouvrirEspaceConseiller(false);
      await charge();

      await utilisateur.click(screen.getByTestId('conseiller-voir-detail-1'));
      await screen.findByTestId('conseiller-detail');
      expect(await reglesViolees()).toEqual([]);

      await utilisateur.click(screen.getByTestId('conseiller-voir-detail-2'));
      await screen.findByText('Aucun compte');
      expect(await reglesViolees()).toEqual([]);
    });
  });

  describe('/bourse', () => {
    const SVG = '<svg viewBox="0 0 800 300"><text x="790" y="292">1 mois (données journalières)</text></svg>';
    const valeur = (id: number, symbole: string, nom: string) => ({ id, symbole, nom, marche: 'Euronext Paris', devise: 'EUR' });
    const point = (date: string, cloture: string) => ({ date, ouverture: '100.0000', haut: '110.0000', bas: '90.0000', cloture, volume: 10 });

    function ouvrirBourse(points: ReturnType<typeof point>[]) {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
      return rendreApplication('/bourse', undefined, undefined, undefined, undefined, undefined, undefined, {
        lireWatchlist: () => Promise.resolve({ statut: 'ok', donnees: [valeur(1, 'AI.PA', 'Air Liquide'), valeur(2, 'BNP.PA', 'BNP Paribas')] }),
        lireHistorique: (symbole, periode) =>
          Promise.resolve({ statut: 'ok', donnees: { symbole, periode, points, graphique: points.length > 0 ? SVG : null } }),
        ajouter: () => Promise.resolve({ statut: 'refusee', message: 'Le symbole est obligatoire (ex: AMZN, SG.PA)' }),
      });
    }

    it('[axe] /bourse avec indicateurs, graphique, séances et watchlist ne relève aucune violation', async () => {
      ouvrirBourse([point('2026-09-30', '100.0000'), point('2026-10-01', '98.5000'), point('2026-10-02', '101.0000')]);
      await screen.findByTestId('bourse-valeur');
      await waitFor(() => expect(screen.getByTestId('bourse-contenu').getAttribute('aria-busy')).toBeNull());

      expect(await reglesViolees()).toEqual([]);
    });

    it('[axe] /bourse sans données et avec le formulaire d’ajout refusé ne relève aucune violation', async () => {
      const { default: userEvent } = await import('@testing-library/user-event');
      const utilisateur = userEvent.setup();
      ouvrirBourse([]);
      await screen.findByTestId('bourse-aucune-donnee');

      await utilisateur.click(screen.getByTestId('bourse-ajouter'));
      await utilisateur.click(screen.getByTestId('bourse-formulaire-ajouter'));
      await screen.findByTestId('bourse-ajout-erreur');

      expect(await reglesViolees()).toEqual([]);
    });
  });

  describe('/mon-conseiller', () => {
    function ouvrirConseiller() {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
      rendreApplication('/mon-conseiller', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        envoyer: () => Promise.resolve({ statut: 'ok', reponse: 'Réponse de Sophie' }),
      });
    }

    it('[axe] /mon-conseiller avec une conversation neuve ne relève aucune violation', async () => {
      ouvrirConseiller();
      await screen.findByTestId('chat-saisie');
      await waitFor(() => expect(screen.getByTestId('chat-contenu').getAttribute('aria-busy')).toBeNull());

      expect(await reglesViolees()).toEqual([]);
    });

    it('[axe] /mon-conseiller avec des échanges ne relève aucune violation', async () => {
      const { default: userEvent } = await import('@testing-library/user-event');
      const utilisateur = userEvent.setup();
      ouvrirConseiller();
      await screen.findByTestId('chat-saisie');

      await utilisateur.type(screen.getByTestId('chat-saisie'), 'Bonjour{Enter}');
      await screen.findByText('Réponse de Sophie');

      expect(await reglesViolees()).toEqual([]);
    });
  });

  describe('/produits', () => {
    const CATALOGUE = {
      epargne: [{ id: 1, nom: 'Livret A', description: 'Epargne', taux: '0.0300', depotMin: '10.00', capitalGaranti: true, dureeMaxMois: null }],
      credit: [],
      investissement: [{ id: 3, nom: 'SCPI', description: null, secteur: null, niveauRisque: 4, rendementAttendu: '0.0650' }],
    };
    const JOURNAL = [
      { job: 'CLOTURE', debut: '2026-10-06T02:00+02:00', fin: null, dureeMs: 0, enregistrements: 3, statut: 'SUCCESS', details: null },
      { job: 'INTERETS', debut: null, fin: null, dureeMs: 0, enregistrements: null, statut: 'FAILED', details: 'Echec' },
      { job: 'ARCHIVAGE', debut: null, fin: null, dureeMs: 0, enregistrements: null, statut: 'PARTIAL', details: null },
    ];

    async function ouvrirProduits(journal: typeof JOURNAL) {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
      rendreApplication('/produits', undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lireCatalogue: () => Promise.resolve({ statut: 'ok', donnees: CATALOGUE }),
        lireTraitements: () => Promise.resolve({ statut: 'ok', donnees: journal }),
      });
      await screen.findByTestId('produits-rafraichir');
      await waitFor(() => expect(screen.getByTestId('produits-contenu').getAttribute('aria-busy')).toBeNull());
    }

    it('[axe] /produits avec catalogue, section vide et journal ne relève aucune violation', async () => {
      await ouvrirProduits(JOURNAL);

      expect(await reglesViolees()).toEqual([]);
    });

    it('[axe] /produits avec un journal vide ne relève aucune violation', async () => {
      await ouvrirProduits([]);

      expect(await reglesViolees()).toEqual([]);
    });

    it('[axe] /produits en panne technique ne relève aucune violation', async () => {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
      rendreApplication('/produits');
      await screen.findByTestId('produits-erreur-technique');

      expect(await reglesViolees()).toEqual([]);
    });
  });

  describe('/comptes-tiers', () => {
    const compte = (id: number, solde: string | null) => ({ id, nom: `Compte ${id}`, type: 'checking', solde, soldeAbsent: solde === null, iban: 'FR7612' });
    const etat = (connecte: boolean) => ({
      connecte,
      comptes: connecte ? [compte(1, '10.00'), compte(2, null)] : [],
      compteSelectionne: connecte ? 1 : null,
      transactions: connecte ? [{ date: '2026-10-01', libelle: 'Cafe', montant: '-3.50' }] : [],
      message: 'Session expiree. Veuillez recommencer la connexion.',
      patrimoineTotal: '10.00',
    });

    it.each([false, true])('[axe] /comptes-tiers (connecté : %s) ne relève aucune violation', async (connecte) => {
      localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
      rendreApplication('/comptes-tiers', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
        lireEtat: () => Promise.resolve({ statut: 'ok', donnees: etat(connecte) }),
      });
      await screen.findByTestId('tiers-message-erreur');

      expect(await reglesViolees()).toEqual([]);
    });
  });

  it('[axe] cas témoin : un champ sans <label for> viole la règle label', async () => {
    render(
      <main>
        <h1>Témoin</h1>
        <form>
          <input type="text" name="username" />
        </form>
      </main>,
    );

    expect(await reglesViolees()).toContain('label');
  });

  it('[axe] /virement, vierge puis avec ses erreurs, ne relève aucune violation', async () => {
    const { default: userEvent } = await import('@testing-library/user-event');
    const utilisateur = userEvent.setup();
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    rendreApplication('/virement', undefined, undefined, undefined, undefined, {
      lireComptes: () => Promise.resolve({ statut: 'comptes', comptes: [{ id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' }] }),
    });
    await screen.findByTestId('virement-executer');
    await waitFor(() => expect((screen.getByTestId('virement-compte-source') as HTMLSelectElement).options).toHaveLength(2));
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('virement-executer'));
    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /beneficiaires, avec bénéficiaires puis vide, ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    const { default: userEvent } = await import('@testing-library/user-event');
    let actifs = [{ id: 1, libelle: 'Loyer', titulaire: 'Marie', iban: 'FR76***0189', bic: null, banque: null, zoneSepa: true }];
    rendreApplication('/beneficiaires', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      lister: () => Promise.resolve({ statut: 'liste' as const, beneficiaires: actifs }),
      supprimer: () => {
        actifs = [];
        return Promise.resolve({ statut: 'refuse' as const });
      },
    });
    await screen.findByTestId('beneficiaires-ligne-1');
    expect(await reglesViolees()).toEqual([]);

    await userEvent.setup().click(screen.getByTestId('beneficiaires-supprimer-1'));
    await screen.findByTestId('beneficiaires-message-erreur');
    await screen.findByTestId('beneficiaires-aucun');
    expect(await reglesViolees()).toEqual([]);
  });

  it('[axe] /beneficiaires, formulaire d’ajout avec résultat de vérification, erreur de champ puis succès, ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    const { default: userEvent } = await import('@testing-library/user-event');
    const reponses = [
      { statut: 'refuse' as const, message: 'Le libellé est obligatoire.', champ: 'libelle' as const },
      { statut: 'ajoute' as const },
    ];
    rendreApplication('/beneficiaires', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, {
      verifierIban: () => Promise.resolve({ statut: 'verifiee' as const, verification: { statut: 'valide' as const, codePays: 'FR', zoneSepa: true } }),
      ajouter: () => Promise.resolve(reponses.shift() ?? { statut: 'technique' as const }),
    });
    await screen.findByTestId('beneficiaires-aucun');
    expect(await reglesViolees()).toEqual([]);
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('beneficiaires-verifier-iban'));
    await screen.findByText('IBAN valide — FR (Zone SEPA)');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('beneficiaires-ajouter'));
    await screen.findByRole('alert');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.click(screen.getByTestId('beneficiaires-ajouter'));
    await screen.findByTestId('beneficiaires-message-info');
    expect(await reglesViolees()).toEqual([]);
  });
  it('[axe] /virements-programmes, formulaire, liste, erreur de champ puis succès, ne relève aucune violation', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    const { default: userEvent } = await import('@testing-library/user-event');
    const virement = {
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
      nbExecutions: 0,
    };
    rendreApplication(
      '/virements-programmes',
      undefined,
      undefined,
      undefined,
      undefined,
      { lireComptes: () => Promise.resolve({ statut: 'comptes' as const, comptes: [{ id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' }] }) },
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
      {
        lister: () => Promise.resolve({ statut: 'liste' as const, virements: [virement, { ...virement, id: 8, actif: false }] }),
        creer: () => Promise.resolve({ statut: 'cree' as const }),
      },
    );
    await screen.findByTestId('vp-ligne-7');
    expect(await reglesViolees()).toEqual([]);
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('vp-creer'));
    await screen.findByRole('alert');
    expect(await reglesViolees()).toEqual([]);

    await utilisateur.selectOptions(screen.getByTestId('vp-compte-debiteur'), '1');
    await utilisateur.type(screen.getByTestId('vp-montant'), '50');
    await utilisateur.type(screen.getByTestId('vp-numero-beneficiaire'), 'FR76-0001-0002');
    await utilisateur.type(screen.getByTestId('vp-libelle-beneficiaire'), 'Epargne');
    await utilisateur.type(screen.getByTestId('vp-date-premiere-execution'), '2027-01-01');
    await utilisateur.click(screen.getByTestId('vp-creer'));
    await screen.findByTestId('vp-message-succes');
    expect(await reglesViolees()).toEqual([]);
  });
});
