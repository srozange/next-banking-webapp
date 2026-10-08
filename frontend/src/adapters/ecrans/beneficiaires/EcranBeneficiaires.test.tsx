// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientBeneficiaires, ReponseBeneficiaires, ReponseSuppression } from '../../../application/gererBeneficiaires';
import type { BeneficiaireBrut } from '../../../domain/beneficiaires';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const LOYER: BeneficiaireBrut = { id: 1, libelle: 'Loyer', titulaire: 'Marie Durand', iban: 'FR76***0189', bic: 'AGRIFRPP', banque: 'Banque Exemple', zoneSepa: true };
const NU: BeneficiaireBrut = { id: 2, libelle: 'Voyage', titulaire: 'John Doe', iban: 'FR35***9010', bic: null, banque: null, zoneSepa: false };

const liste = (...beneficiaires: BeneficiaireBrut[]) => Promise.resolve<ReponseBeneficiaires>({ statut: 'liste', beneficiaires });

function ouvrir(client: Partial<ClientBeneficiaires> = {}, roles = ['CLIENT']) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles }));
  const indefini = undefined;
  return rendreApplication('/beneficiaires', indefini, indefini, indefini, indefini, indefini, indefini, {}, {}, {}, {}, {}, {}, {}, {}, client);
}

describe('écran des bénéficiaires', () => {
  it('[FR-013-01] sans session, redirige vers /login', async () => {
    const { routeur } = rendreApplication('/beneficiaires');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-013-02] affiche l’onglet, la section, le compteur et les colonnes avec <th scope="col">', async () => {
    ouvrir({ lister: () => liste(LOYER, NU) });
    await screen.findByTestId('beneficiaires-ligne-1');

    expect(document.title).toBe('BankApp Demo - Beneficiaires');
    expect(screen.getByRole('heading', { name: 'Mes beneficiaires enregistres' })).toBeTruthy();
    expect(screen.getByTestId('beneficiaires-titre').textContent).toBe('Beneficiaires (2)');
    const entetes = within(screen.getByTestId('beneficiaires-tableau')).getAllByRole('columnheader');
    expect(entetes.map((entete) => entete.textContent)).toEqual(['Libelle', 'Titulaire', 'IBAN', 'BIC', 'Banque', 'Zone SEPA', 'Action']);
    entetes.forEach((entete) => expect(entete.getAttribute('scope')).toBe('col'));
  });

  it('[FR-013-02] garde l’ordre du service et un bouton Supprimer par bénéficiaire', async () => {
    ouvrir({ lister: () => liste(LOYER, NU) });
    await screen.findByTestId('beneficiaires-ligne-1');

    const lignes = screen.getAllByTestId(/^beneficiaires-ligne-/);
    expect(lignes.map((ligne) => ligne.getAttribute('data-testid'))).toEqual(['beneficiaires-ligne-1', 'beneficiaires-ligne-2']);
    expect(screen.getByTestId('beneficiaires-supprimer-1').textContent).toBe('Supprimer');
    expect(screen.getByTestId('beneficiaires-supprimer-2').textContent).toBe('Supprimer');
  });

  it('[FR-013-03] affiche l’IBAN masqué, « — » sans BIC ni banque, « ✓ SEPA » et « Hors SEPA »', async () => {
    ouvrir({ lister: () => liste(LOYER, NU) });
    await screen.findByTestId('beneficiaires-ligne-1');

    expect(screen.getByTestId('beneficiaires-iban-1').textContent).toBe('FR76***0189');
    expect(screen.getByTestId('beneficiaires-zone-1').textContent).toBe('✓ SEPA');
    expect(within(screen.getByTestId('beneficiaires-ligne-1')).getAllByRole('cell').map((c) => c.textContent)).toEqual([
      'Loyer', 'Marie Durand', 'FR76***0189', 'AGRIFRPP', 'Banque Exemple', '✓ SEPA', 'Supprimer',
    ]);
    expect(within(screen.getByTestId('beneficiaires-ligne-2')).getAllByRole('cell').map((c) => c.textContent)).toEqual([
      'Voyage', 'John Doe', 'FR35***9010', '—', '—', 'Hors SEPA', 'Supprimer',
    ]);
  });

  it('[FR-013-02] sans bénéficiaire, garde les en-têtes et une ligne pleine largeur', async () => {
    ouvrir();
    const vide = await screen.findByTestId('beneficiaires-aucun');

    expect(vide.textContent).toBe('Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un.');
    expect(vide.getAttribute('colspan')).toBe('7');
    expect(screen.getByTestId('beneficiaires-titre').textContent).toBe('Beneficiaires (0)');
    expect(screen.getAllByRole('columnheader')).toHaveLength(7);
  });

  it('[FR-013-02] une lecture en échec affiche le texte technique', async () => {
    ouvrir({ lister: () => Promise.resolve<ReponseBeneficiaires>({ statut: 'technique' }) });

    expect((await screen.findByTestId('beneficiaires-message-erreur')).textContent).toBe('Une erreur technique est survenue');
  });

  it('[FR-013-09] supprime sans recharger la page : la ligne disparaît et la liste est relue', async () => {
    let actifs = [LOYER, NU];
    const { supprimerBeneficiaire, listerBeneficiaires } = ouvrir({
      lister: () => liste(...actifs),
      supprimer: (id) => {
        actifs = actifs.filter((b) => b.id !== id);
        return Promise.resolve<ReponseSuppression>({ statut: 'supprime' });
      },
    });
    await screen.findByTestId('beneficiaires-ligne-1');

    await userEvent.setup().click(screen.getByTestId('beneficiaires-supprimer-1'));

    await waitFor(() => expect(screen.queryByTestId('beneficiaires-ligne-1')).toBeNull());
    expect(supprimerBeneficiaire).toHaveBeenCalledWith(1);
    expect(listerBeneficiaires).toHaveBeenCalledTimes(2);
    expect(screen.getByTestId('beneficiaires-titre').textContent).toBe('Beneficiaires (1)');
    expect(screen.queryByTestId('beneficiaires-message-erreur')).toBeNull();
  });

  it('[FR-013-10] un refus affiche « Erreur lors de la suppression. » et relit la liste', async () => {
    ouvrir({ lister: () => liste(LOYER), supprimer: () => Promise.resolve<ReponseSuppression>({ statut: 'refuse' }) });
    await screen.findByTestId('beneficiaires-ligne-1');

    await userEvent.setup().click(screen.getByTestId('beneficiaires-supprimer-1'));

    expect((await screen.findByTestId('beneficiaires-message-erreur')).textContent).toBe('Erreur lors de la suppression.');
    expect(screen.getByTestId('beneficiaires-ligne-1')).toBeTruthy();
  });

  it('[FR-013-09] une panne à la suppression affiche le texte technique et garde la liste', async () => {
    ouvrir({ lister: () => liste(LOYER), supprimer: () => Promise.resolve<ReponseSuppression>({ statut: 'technique' }) });
    await screen.findByTestId('beneficiaires-ligne-1');

    await userEvent.setup().click(screen.getByTestId('beneficiaires-supprimer-1'));

    expect((await screen.findByTestId('beneficiaires-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.getByTestId('beneficiaires-ligne-1')).toBeTruthy();
  });

  it('[FR-013-09] une suppression réussie dont la relecture échoue affiche le texte technique', async () => {
    let appels = 0;
    ouvrir({
      lister: () => (++appels === 1 ? liste(LOYER) : Promise.resolve<ReponseBeneficiaires>({ statut: 'technique' })),
      supprimer: () => Promise.resolve<ReponseSuppression>({ statut: 'supprime' }),
    });
    await screen.findByTestId('beneficiaires-ligne-1');

    await userEvent.setup().click(screen.getByTestId('beneficiaires-supprimer-1'));

    expect((await screen.findByTestId('beneficiaires-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.getByTestId('beneficiaires-ligne-1')).toBeTruthy();
  });

  it('[FR-013-01] le même écran sert CONSEILLER et ADMIN', async () => {
    ouvrir({ lister: () => liste(LOYER) }, ['ADMIN']);

    expect(await screen.findByTestId('beneficiaires-ligne-1')).toBeTruthy();
  });
});
