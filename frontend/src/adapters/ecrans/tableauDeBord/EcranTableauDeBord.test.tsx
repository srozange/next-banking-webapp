// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ReponseTableauDeBord } from '../../../application/consulterTableauDeBord';
import type { ReponseProfil } from '../../../application/chargerProfil';
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
) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles }));
  return rendreApplication('/tableau-de-bord', undefined, lireProfil, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { lire: reponse });
}

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
});
