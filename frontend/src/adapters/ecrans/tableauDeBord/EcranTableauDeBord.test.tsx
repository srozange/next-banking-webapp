// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ReponseTableauDeBord } from '../../../application/consulterTableauDeBord';
import type { ReponseProfil } from '../../../application/chargerProfil';
import type { ClientComptesTiers, ReponseTiers } from '../../../application/comptesTiers';
import type { EtatComptesTiers } from '../../../domain/comptesTiers';
import type { OperationBrute } from '../../../domain/releve';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const MESSAGE_NOTIFICATIONS_VIDE =
  'Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel';
const COMPTES = [
  { id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' },
  { id: 2, type: 'EPARGNE', numero: 'FR76-0001-0002', solde: '15200.00' },
];
const operation = (description: string, type: 'CREDIT' | 'DEBIT', montant: string, soldeApres: string, date = '2026-11-03T08:05:00Z'): OperationBrute => ({
  date,
  description,
  type,
  montant,
  soldeApres,
});
const OPERATIONS = [operation('[retro] virement doré 100EUR', 'DEBIT', '100.00', '2130.00'), operation('Virement reçu', 'CREDIT', '1200.50', '2230.00', '2026-11-01T22:30:00Z')];

function ouvrir(
  reponse: () => Promise<ReponseTableauDeBord> = () => Promise.resolve({ statut: 'tableau', comptes: COMPTES, dernieresOperations: OPERATIONS }),
  roles = ['CLIENT'],
  lireProfil?: () => Promise<ReponseProfil>,
  lireEtat?: ClientComptesTiers['lireEtat'],
) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles }));
  return rendreApplication('/tableau-de-bord', undefined, lireProfil, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, lireEtat === undefined ? {} : { lireEtat }, { lire: reponse });
}

const ETAT: EtatComptesTiers = { connecte: true, comptes: [], compteSelectionne: null, transactions: [], message: '', patrimoineTotal: '120.50' };
const COMPTE_EXTERNE = { id: 1, nom: 'Compte', type: null, solde: '40.00', soldeAbsent: false, iban: null };
const etatOk = (surcharge: Partial<EtatComptesTiers> = {}) => (): Promise<ReponseTiers<EtatComptesTiers>> =>
  Promise.resolve({ statut: 'ok', donnees: { ...ETAT, ...surcharge } });
const COMPTES_DEMO_BANK = [{ id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '1000.00' }];
const tableauMille = () => Promise.resolve<ReponseTableauDeBord>({ statut: 'tableau', comptes: COMPTES_DEMO_BANK, dernieresOperations: [] });

describe('écran du tableau de bord', () => {
  it('[FR-002-01] relit /moi et appelle le cœur bancaire dès le premier rendu, sans attendre l’identité (parallèle)', async () => {
    let rendreProfil: (profil: ReponseProfil) => void = () => undefined;
    const attente = new Promise<ReponseProfil>((resolu) => {
      rendreProfil = resolu;
    });
    const { lire, lireTableauDeBord } = ouvrir(undefined, ['CLIENT'], () => attente);

    expect(lire).toHaveBeenCalledTimes(1);
    expect(lireTableauDeBord).toHaveBeenCalledTimes(1);
    expect(await screen.findByTestId('dashboard-solde-total')).toBeTruthy();
    expect(screen.queryByTestId('nav-salutation')).toBeNull();
    rendreProfil({ statut: 'profil', nomComplet: 'Jean Dupont' });
    expect((await screen.findByTestId('nav-salutation')).textContent).toBe('Bonjour, Jean Dupont');
    expect(screen.getByTestId('nav-deconnexion').textContent).toBe('Deconnexion');
  });

  it('[FR-002-02] aria-busy pendant le chargement, sans squelette ni texte', async () => {
    let rendre: (reponse: ReponseTableauDeBord) => void = () => undefined;
    ouvrir(() => new Promise<ReponseTableauDeBord>((resolu) => (rendre = resolu)));

    const ecran = screen.getByTestId('dashboard-ecran');
    expect(ecran.getAttribute('aria-busy')).toBe('true');
    expect(screen.queryByTestId('dashboard-solde-total')).toBeNull();
    rendre({ statut: 'tableau', comptes: [], dernieresOperations: [] });
    await waitFor(() => expect(ecran.getAttribute('aria-busy')).toBeNull());
  });

  it('[FR-002-03] affiche le séparateur, le solde total et une carte par compte', async () => {
    ouvrir();

    expect((await screen.findByTestId('dashboard-comptes-titre')).textContent).toBe('Mes comptes Demo Bank');
    expect(screen.getByTestId('dashboard-solde-total-titre').textContent).toBe('Solde total Demo Bank');
    expect(screen.getByTestId('dashboard-solde-total').textContent).toBe('EUR17,330.00');
    expect(screen.getByText('Tous comptes Demo Bank confondus')).toBeTruthy();
    expect(screen.getByTestId('dashboard-compte-type-1').textContent).toBe('COURANT');
    expect(screen.getByTestId('dashboard-compte-solde-1').textContent).toBe('EUR2,130.00');
    expect(screen.getByTestId('dashboard-compte-numero-1').textContent).toBe('N FR76-0001-0001');
    expect(screen.getByTestId('dashboard-compte-type-2').textContent).toBe('EPARGNE');
    expect(screen.getByTestId('dashboard-compte-solde-2').textContent).toBe('EUR15,200.00');
  });

  it('[FR-002-04] un utilisateur sans compte voit EUR0.00', async () => {
    ouvrir(() => Promise.resolve({ statut: 'tableau', comptes: [], dernieresOperations: [] }), ['CONSEILLER']);

    expect((await screen.findByTestId('dashboard-solde-total')).textContent).toBe('EUR0.00');
    expect(screen.queryByTestId('dashboard-compte-1')).toBeNull();
  });

  it('[FR-002-05] affiche les opérations, le tableau vide en ligne pleine largeur et le bouton vers /releve', async () => {
    const { routeur, unmount } = ouvrir();

    expect((await screen.findByTestId('dashboard-activite-titre')).textContent).toBe('Activite recente');
    expect(screen.getByRole('heading', { name: 'Dernieres operations' })).toBeTruthy();
    const tableau = screen.getByTestId('dashboard-operations');
    expect(within(tableau).getAllByRole('columnheader').map((th) => th.textContent)).toEqual(['Date', 'Description', 'Type', 'Montant', 'Solde apres']);
    expect(within(screen.getByTestId('dashboard-operation-0')).getAllByRole('cell').map((td) => td.textContent)).toEqual([
      '03/11/2026 09:05',
      '[retro] virement doré 100EUR',
      'DEBIT',
      '-EUR100.00',
      'EUR2,130.00',
    ]);
    expect(screen.getByTestId('dashboard-operation-type-0').className).toContain('debit');
    expect(screen.getByTestId('dashboard-operation-montant-1').textContent).toBe('+EUR1,200.50');
    expect(screen.getByTestId('dashboard-operation-type-1').className).toContain('credit');
    await userEvent.click(screen.getByTestId('dashboard-voir-releve'));
    expect(routeur.state.location.pathname).toBe('/releve');
    unmount();

    ouvrir(() => Promise.resolve({ statut: 'tableau', comptes: [], dernieresOperations: [] }));
    const vide = await screen.findByTestId('dashboard-operations-vide');
    expect(vide.getAttribute('colspan')).toBe('5');
    expect(screen.getByTestId('dashboard-voir-releve').textContent).toBe('Voir le releve complet');
  });

  it('[FR-002-06] au plus 5 opérations, de la plus récente à la plus ancienne', async () => {
    const sept = [1, 2, 3, 4, 5, 6, 7].map((n) => operation(`op ${n}`, 'CREDIT', '1.00', '2.00'));
    ouvrir(() => Promise.resolve({ statut: 'tableau', comptes: COMPTES, dernieresOperations: sept }));

    await screen.findByTestId('dashboard-operation-0');
    expect(screen.queryByTestId('dashboard-operation-5')).toBeNull();
    expect(screen.getByTestId('dashboard-operation-4').textContent).toContain('op 5');
  });

  it('[FR-003-12] « Suivi de mes virements » est la dernière section, sans bandeau ni libellé technique', async () => {
    ouvrir();

    await screen.findByTestId('dashboard-operations');
    const sections = Array.from(screen.getByTestId('dashboard-ecran').children).filter((enfant) => enfant.tagName === 'SECTION');
    expect(sections.at(-1)?.getAttribute('data-testid')).toBe('dashboard-virements-revue');
    const complet = screen.getByTestId('application-racine').textContent ?? '';
    expect(complet.lastIndexOf('Suivi de mes virements')).toBeGreaterThan(complet.indexOf('Dernieres operations'));
    // Seul le message d'état vide du centre de notifications cite « Artemis », repris à l'identique (FR-002-07).
    const texte = complet.replace(MESSAGE_NOTIFICATIONS_VIDE, '');
    expect(texte.length).toBeLessThan(complet.length);
    for (const technique of ['Legacy', 'JEE', 'Artemis', 'NamedQuery', 'JPA']) {
      expect(texte).not.toContain(technique);
    }
  });

  it('[BUG-009] [FR-003-29] un virement important exécuté n’est annoncé ni « en attente » ni « verification » : opérations, notification et suivi', async () => {
    localStorage.setItem('nb.jeton', jetonDeTest({ roles: ['CLIENT'] }));
    const message = 'Virement de 5000.00 EUR vers le compte FR76-0001-0002 execute - transaction importante.';
    rendreApplication(
      '/tableau-de-bord',
      undefined,
      undefined,
      undefined,
      undefined,
      {
        lireSuivi: () =>
          Promise.resolve({
            statut: 'suivi',
            virements: [{ virementId: 12, soumisLe: '2026-11-17T09:00:00Z', compteDestinataire: 'FR76-0001-0002', montant: '5000.00', etat: 'EXECUTE', heureButoir: '2026-11-18T09:00:00Z' }],
          }),
      },
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      undefined,
      {
        lire: () =>
          Promise.resolve({
            statut: 'tableau',
            comptes: COMPTES,
            dernieresOperations: [operation('[TRANSACTION IMPORTANTE] Virement vers FR76-0001-0002 - loyer', 'DEBIT', '5000.00', '2130.00')],
          }),
      },
      {
        lister: () =>
          Promise.resolve({ statut: 'liste', notifications: [{ id: 7, type: 'ALERTE', titre: null, message, creeLe: '2026-11-17T09:00:00Z', lue: false }] }),
      },
    );

    const etat = await screen.findByTestId('virement-etat-12');
    expect(etat.textContent).toBe('Exécuté - débité');
    expect((await screen.findByTestId('notifications-message-7')).textContent).toBe(message);
    const operationRendue = await screen.findByTestId('dashboard-operation-0');
    expect(operationRendue.textContent).toContain('[TRANSACTION IMPORTANTE] Virement vers FR76-0001-0002 - loyer');
    const textes = [operationRendue, screen.getByTestId('notifications-notification-7'), etat.closest('tr')].map((element) => (element?.textContent ?? '').toLowerCase());
    for (const texte of textes) {
      expect(texte).not.toMatch(/en attente|verification/);
    }
  });

  it('[FR-002-11] CLIENT, CONSEILLER et ADMIN voient la navigation du tableau de bord selon leur rôle', async () => {
    const attendus = (conseiller: boolean, administration: boolean) => [
      'Tableau de bord', 'Virement', 'Virements programmes', 'Releve', 'Budget', 'Prets', 'Documents', 'Bourse', 'Mon Conseiller', 'Produits', 'Comptes Tiers', 'Beneficiaires',
      ...(conseiller ? ['Espace Conseiller'] : []),
      ...(administration ? ['Administration'] : []),
    ];
    for (const [role, conseiller, administration] of [['CLIENT', false, false], ['CONSEILLER', true, false], ['ADMIN', true, true]] as const) {
      const { unmount } = ouvrir(undefined, [role]);
      await screen.findByTestId('dashboard-solde-total');
      const liens = within(screen.getByRole('navigation')).getAllByRole('link').map((lien) => lien.textContent);
      expect(liens.filter((texte) => attendus(conseiller, administration).includes(texte ?? ''))).toEqual(attendus(conseiller, administration));
      expect(liens.includes('Espace Conseiller')).toBe(conseiller);
      expect(liens.includes('Administration')).toBe(administration);
      unmount();
      cleanup();
    }
  });

  it('[FR-002-03] une erreur du cœur bancaire affiche le texte technique, sans trace, et garde le suivi', async () => {
    ouvrir(() => Promise.resolve({ statut: 'technique' }));

    expect((await screen.findByTestId('dashboard-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByTestId('dashboard-solde-total')).toBeNull();
    expect(screen.getByTestId('dashboard-virements-revue')).toBeTruthy();
  });

  it('[BUG-004] fiche supprimée : jeton supprimé, /login, aucune trace technique', async () => {
    const { routeur } = ouvrir(undefined, ['CLIENT'], () => Promise.resolve<ReponseProfil>({ statut: 'nonAuthentifie' }));

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
    expect(localStorage.getItem('nb.jeton')).toBeNull();
    expect(document.body.textContent).not.toContain('erreur technique');
  });

  it('[FR-002-12] Powens connecté : section, patrimoine total, nombre de comptes et bouton vers /comptes-tiers', async () => {
    const { routeur } = ouvrir(tableauMille, ['CLIENT'], undefined, etatOk({ comptes: [COMPTE_EXTERNE, COMPTE_EXTERNE, COMPTE_EXTERNE] }));

    expect((await screen.findByTestId('dashboard-externe-titre')).textContent).toBe('Mes comptes externes (Open Banking)');
    expect(screen.getByTestId('dashboard-patrimoine-titre').textContent).toBe('Patrimoine total');
    expect(screen.getByTestId('dashboard-patrimoine-total').textContent).toBe('EUR1,120.50');
    expect(screen.getByText('Demo Bank + comptes externes')).toBeTruthy();
    expect(screen.getByTestId('dashboard-externe-resume').textContent).toBe('Comptes externes via Powens Open Banking (3 comptes)');
    expect(screen.queryByTestId('dashboard-openbanking-titre')).toBeNull();
    // La section se place après le contenu comparé (« Voir le releve complet ») et avant le centre de notifications.
    const texte = screen.getByTestId('dashboard-ecran').textContent ?? '';
    expect(texte.indexOf('Mes comptes externes')).toBeGreaterThan(texte.indexOf('Voir le releve complet'));
    expect(texte.indexOf('Mes comptes externes')).toBeLessThan(texte.indexOf('Centre de notifications'));
    expect(screen.getByTestId('dashboard-externe-detail').textContent).toBe('Voir le detail et les transactions');
    await userEvent.click(screen.getByTestId('dashboard-externe-detail'));
    expect(routeur.state.location.pathname).toBe('/comptes-tiers');
  });

  it('[FR-002-12] connecté sans compte : le patrimoine est le total Demo Bank', async () => {
    ouvrir(tableauMille, ['CLIENT'], undefined, etatOk({ patrimoineTotal: '0.00' }));

    expect((await screen.findByTestId('dashboard-patrimoine-total')).textContent).toBe('EUR1,000.00');
    expect(screen.getByTestId('dashboard-externe-resume').textContent).toBe('Comptes externes via Powens Open Banking (0 comptes)');
  });

  it('[FR-002-12] Powens non connecté : invitation et bouton vers /comptes-tiers', async () => {
    const { routeur } = ouvrir(tableauMille, ['CLIENT'], undefined, etatOk({ connecte: false, patrimoineTotal: '0.00' }));

    expect((await screen.findByTestId('dashboard-openbanking-titre')).textContent).toBe('Open Banking');
    expect(screen.getByTestId('dashboard-openbanking-invitation').textContent).toBe(
      'Agregez vos comptes d autres banques pour une vue complete de votre patrimoine',
    );
    expect(screen.queryByTestId('dashboard-patrimoine-total')).toBeNull();
    expect(screen.getByTestId('dashboard-openbanking-connecter').textContent).toBe('Connecter mes autres banques via Powens');
    await userEvent.click(screen.getByTestId('dashboard-openbanking-connecter'));
    expect(routeur.state.location.pathname).toBe('/comptes-tiers');
  });

  it.each([
    ['503', () => Promise.resolve<ReponseTiers<EtatComptesTiers>>({ statut: 'indisponible' })],
    ['injoignable', () => Promise.reject<ReponseTiers<EtatComptesTiers>>(new Error('réseau'))],
    ['illisible', () => Promise.resolve<ReponseTiers<EtatComptesTiers>>({ statut: 'technique' })],
  ])('[FR-002-12] Powens en échec (%s) : tableau de bord normal, ni section ni somme', async (_nom, lireEtat) => {
    ouvrir(tableauMille, ['CLIENT'], undefined, lireEtat);

    expect((await screen.findByTestId('dashboard-solde-total')).textContent).toBe('EUR1,000.00');
    await waitFor(() => expect(screen.getByTestId('dashboard-ecran').getAttribute('aria-busy')).toBeNull());
    expect(screen.queryByTestId('dashboard-patrimoine-total')).toBeNull();
    expect(screen.queryByTestId('dashboard-externe-titre')).toBeNull();
    expect(screen.queryByTestId('dashboard-openbanking-titre')).toBeNull();
    expect(screen.queryByTestId('dashboard-message-erreur')).toBeNull();
  });

  it('[FR-002-12] cœur bancaire en échec, Powens ok : message technique, pas de patrimoine', async () => {
    ouvrir(() => Promise.resolve({ statut: 'technique' }), ['CLIENT'], undefined, etatOk());

    expect((await screen.findByTestId('dashboard-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.queryByTestId('dashboard-patrimoine-total')).toBeNull();
    expect(screen.queryByTestId('dashboard-externe-titre')).toBeNull();
  });

  it('[FR-002-12] l’état Powens est demandé au rendu, avant toute résolution', async () => {
    let rendre: (reponse: ReponseTiers<EtatComptesTiers>) => void = () => undefined;
    const attente = new Promise<ReponseTiers<EtatComptesTiers>>((resolu) => (rendre = resolu));
    const { lire, tiers } = ouvrir(tableauMille, ['CLIENT'], undefined, () => attente);

    expect(lire).toHaveBeenCalledTimes(1);
    expect(tiers.lireEtat).toHaveBeenCalledTimes(1);
    expect(await screen.findByTestId('dashboard-solde-total')).toBeTruthy();
    expect(screen.queryByTestId('dashboard-patrimoine-total')).toBeNull();
    rendre({ statut: 'ok', donnees: ETAT });
    expect((await screen.findByTestId('dashboard-patrimoine-total')).textContent).toBe('EUR1,120.50');
  });

  it('[FR-002-12] sans jeton, /tableau-de-bord renvoie vers /login', async () => {
    const { routeur } = rendreApplication('/tableau-de-bord');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });
});
