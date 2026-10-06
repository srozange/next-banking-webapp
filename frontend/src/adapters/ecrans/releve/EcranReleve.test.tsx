// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientReleve, ReponseOperations } from '../../../application/consulterReleve';
import type { ClientVirement, Compte, ReponseComptes } from '../../../application/virement';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const COMPTES: Compte[] = [
  { id: 1, type: 'COURANT', numero: 'FR76-0001-0001', solde: '2130.00' },
  { id: 2, type: 'EPARGNE', numero: 'FR76-0001-0002', solde: '15200.00' },
];

const operation = (description: string, type: 'CREDIT' | 'DEBIT', montant: string, soldeApres: string, date = '2026-11-03T08:05:00Z') => ({
  date,
  description,
  type,
  montant,
  soldeApres,
});

const OPERATIONS_COMPTE_1 = [
  operation('[EN ATTENTE] Virement vers FR76-0001-0002 - loyer', 'DEBIT', '6000.00', '2130.00'),
  operation('Virement depuis FR76-0001-0002', 'CREDIT', '1200.50', '8130.00', '2026-11-01T22:30:00Z'),
];

function ouvrir(
  { comptes = COMPTES, operations, lireComptes }: { comptes?: Compte[]; operations?: ClientReleve['lireOperations']; lireComptes?: ClientVirement['lireComptes'] } = {},
  roles = ['CLIENT'],
) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles }));
  const parDefaut = (id: number) =>
    Promise.resolve<ReponseOperations>({ statut: 'operations', operations: id === 1 ? OPERATIONS_COMPTE_1 : [] });
  return rendreApplication(
    '/releve',
    undefined,
    undefined,
    undefined,
    undefined,
    { lireComptes: lireComptes ?? (() => Promise.resolve<ReponseComptes>({ statut: 'comptes', comptes })) },
    undefined,
    {},
    {},
    {},
    { lireOperations: operations ?? parDefaut },
  );
}

const liste = () => screen.getByTestId('releve-compte') as HTMLSelectElement;

describe('écran du relevé', () => {
  it('[FR-005-01] sans session, redirige vers /login', async () => {
    const { routeur } = rendreApplication('/releve');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-005-02] affiche l’onglet, la section, le sélecteur limité aux comptes du client, sans bandeau technique', async () => {
    const { lireOperations } = ouvrir();
    await waitFor(() => expect(liste().options).toHaveLength(2));

    expect(document.title).toBe('BankApp Demo - Relevé de compte');
    expect(screen.getByRole('heading', { name: 'Relevé de compte' })).toBeTruthy();
    expect(screen.getByLabelText('Compte :')).toBe(liste());
    expect(Array.from(liste().options).map((option) => [option.value, option.text])).toEqual([
      ['1', 'COURANT FR76-0001-0001'],
      ['2', 'EPARGNE FR76-0001-0002'],
    ]);
    expect(liste().value).toBe('1');
    await waitFor(() => expect(lireOperations).toHaveBeenCalledWith(1));
    expect(document.body.textContent).not.toContain('NamedQuery');
    expect(document.body.textContent).not.toContain('Legacy');
  });

  it('[FR-005-04] affiche le type, le numéro et le solde actuel en euros', async () => {
    ouvrir();

    await waitFor(() => expect(screen.getByTestId('releve-informations').textContent).toBe('COURANT — N° FR76-0001-0001 — Solde actuel : 2 130,00 €'));
  });

  it('[FR-005-02] sans compte : sélecteur sans option, aucune opération, aucun appel au relevé', async () => {
    const { lireComptes, lireOperations } = ouvrir({ comptes: [] }, ['CONSEILLER']);
    await waitFor(() => expect(lireComptes).toHaveBeenCalled());

    expect(liste().options).toHaveLength(0);
    expect(screen.queryByTestId('releve-informations')).toBeNull();
    expect(screen.getByTestId('releve-aucune-transaction').textContent).toBe('Aucune transaction pour ce compte.');
    expect(screen.queryByTestId('releve-ligne-0')).toBeNull();
    expect(lireOperations).not.toHaveBeenCalled();
  });

  it('[FR-005-06] liste les opérations dans l’ordre reçu avec compteur, colonnes, date Europe/Paris, signes et soldes', async () => {
    ouvrir();
    await screen.findByTestId('releve-ligne-1');

    const entetes = within(screen.getByTestId('releve-tableau'))
      .getAllByRole('columnheader')
      .map((entete) => entete.textContent);
    expect(entetes).toEqual(['Date', 'Description', 'Type', 'Montant', 'Solde après']);
    expect(screen.getByTestId('releve-compteur').textContent).toBe('2 opération(s)');
    expect(screen.getByTestId('releve-compteur').getAttribute('aria-live')).toBe('polite');
    const lignes = ['releve-ligne-0', 'releve-ligne-1'].map((id) => Array.from(screen.getByTestId(id).querySelectorAll('td')).map((cellule) => cellule.textContent));
    expect(lignes).toEqual([
      ['03/11/2026 09:05', 'En attente Virement vers FR76-0001-0002 - loyer', 'DEBIT', '-6 000,00 €', '2 130,00 €'],
      ['01/11/2026 23:30', 'Virement depuis FR76-0001-0002', 'CREDIT', '+1 200,50 €', '8 130,00 €'],
    ]);
    expect(screen.getByTestId('releve-type-0').className).toContain('--debit');
    expect(screen.getByTestId('releve-type-1').className).toContain('--credit');
  });

  it('[FR-005-06] un compte sans opération affiche la ligne vide sous les en-têtes conservés, sans compteur', async () => {
    ouvrir({ operations: () => Promise.resolve({ statut: 'operations', operations: [] }) });
    await screen.findByTestId('releve-aucune-transaction');

    expect(screen.getByTestId('releve-aucune-transaction').textContent).toBe('Aucune transaction pour ce compte.');
    expect(within(screen.getByTestId('releve-tableau')).getAllByRole('columnheader')).toHaveLength(5);
    expect(screen.getByTestId('releve-compteur').textContent).toBe('');
  });

  it('[FR-005-07] une opération en attente porte le badge et le fond d’avertissement, une autre non', async () => {
    ouvrir();
    await screen.findByTestId('releve-ligne-1');

    expect(screen.getByTestId('releve-en-attente-0').textContent).toBe('En attente');
    expect(screen.getByTestId('releve-ligne-0').className).toContain('ecran-releve__ligne--attente');
    expect(screen.getByTestId('releve-ligne-0').textContent).not.toContain('[EN ATTENTE]');
    expect(screen.queryByTestId('releve-en-attente-1')).toBeNull();
    expect(screen.getByTestId('releve-ligne-1').className).toBe('');
  });

  it('[FR-005-03] au changement de compte, recharge les comptes (soldes à jour) puis les opérations du compte 2', async () => {
    const u = userEvent.setup();
    const ordre: string[] = [];
    let appel = 0;
    ouvrir({
      lireComptes: () => {
        ordre.push('comptes');
        appel++;
        return Promise.resolve<ReponseComptes>({
          statut: 'comptes',
          comptes: appel === 1 ? COMPTES : [COMPTES[0] as Compte, { ...(COMPTES[1] as Compte), solde: '15300.00' }],
        });
      },
      operations: (id) => {
        ordre.push(`operations ${id}`);
        return Promise.resolve({ statut: 'operations', operations: id === 1 ? OPERATIONS_COMPTE_1 : [operation('Dépôt', 'CREDIT', '10.00', '15300.00')] });
      },
    });
    await screen.findByTestId('releve-ligne-1');

    await u.selectOptions(liste(), '2');

    await waitFor(() => expect(screen.getByTestId('releve-compteur').textContent).toBe('1 opération(s)'));
    expect(ordre).toEqual(['comptes', 'operations 1', 'comptes', 'operations 2']);
    expect(liste().value).toBe('2');
    expect(screen.getByTestId('releve-informations').textContent).toBe('EPARGNE — N° FR76-0001-0002 — Solde actuel : 15 300,00 €');
    expect(screen.queryByTestId('releve-ligne-1')).toBeNull();
  });

  it('[FR-005-05] une erreur du service (403, 404, 5xx) affiche le texte technique et conserve le compte choisi', async () => {
    const u = userEvent.setup();
    ouvrir({ operations: (id) => Promise.resolve<ReponseOperations>(id === 1 ? { statut: 'operations', operations: OPERATIONS_COMPTE_1 } : { statut: 'technique' }) });
    await screen.findByTestId('releve-ligne-1');

    await u.selectOptions(liste(), '2');

    expect((await screen.findByTestId('releve-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(liste().value).toBe('2');
    expect(screen.getByTestId('releve-ligne-0')).toBeTruthy();
  });

  it('[FR-005-05] des comptes indisponibles à l’ouverture affichent le texte technique', async () => {
    ouvrir({ lireComptes: () => Promise.resolve({ statut: 'technique' }) });

    expect((await screen.findByTestId('releve-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(liste().options).toHaveLength(0);
  });

  it('[FR-005-03] ignore la réponse d’une demande périmée', async () => {
    const u = userEvent.setup();
    const attentes = new Map<number, (reponse: ReponseOperations) => void>();
    ouvrir({
      operations: (id) => (id === 1 ? Promise.resolve({ statut: 'operations', operations: OPERATIONS_COMPTE_1 }) : new Promise((resoudre) => attentes.set(id, resoudre))),
    });
    await screen.findByTestId('releve-ligne-1');

    await u.selectOptions(liste(), '2');
    await u.selectOptions(liste(), '1');
    await waitFor(() => expect(screen.getByTestId('releve-compteur').textContent).toBe('2 opération(s)'));
    attentes.get(2)?.({ statut: 'operations', operations: [] });
    await Promise.resolve();

    expect(liste().value).toBe('1');
    expect(screen.getByTestId('releve-compteur').textContent).toBe('2 opération(s)');
  });

  describe('[FR-005-08] navigation selon le rôle', () => {
    it.each([
      ['CLIENT', false, false],
      ['CONSEILLER', true, false],
      ['ADMIN', true, true],
    ])('%s : Espace Conseiller %s, Administration %s', async (role, conseiller, administration) => {
      ouvrir({ comptes: [] }, [role]);
      await screen.findByTestId('releve-aucune-transaction');

      expect(screen.getByTestId('nav-releve').textContent).toBe('Releve');
      expect(screen.queryByTestId('nav-espace-conseiller') !== null).toBe(conseiller);
      expect(screen.queryByTestId('nav-administration') !== null).toBe(administration);
    });
  });
});
