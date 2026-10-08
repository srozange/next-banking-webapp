// @vitest-environment jsdom
import { cleanup, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientBeneficiaires, ReponseAjout, ReponseBeneficiaires, ReponseSuppression, ReponseVerificationIban } from '../../../application/gererBeneficiaires';
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
    expect(screen.getByRole('heading', { name: 'Ajouter un beneficiaire' })).toBeTruthy();
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

  it('[FR-013-05] la section « Nouveau beneficiaire SEPA » porte cinq champs reliés à leur label et deux boutons', async () => {
    ouvrir();
    await screen.findByTestId('beneficiaires-aucun');

    expect(screen.getByRole('heading', { name: 'Nouveau beneficiaire SEPA' })).toBeTruthy();
    const attendus: [string, string, boolean, string | null][] = [
      ['beneficiaires-libelle', 'Libelle', true, null],
      ['beneficiaires-titulaire', 'Nom du titulaire', true, null],
      ['beneficiaires-iban', 'IBAN', true, 'ex: FR76 3000 1007 9412 3456 7890 185'],
      ['beneficiaires-bic', 'BIC (optionnel)', false, 'ex: BNPAFRPPXXX'],
      ['beneficiaires-banque', 'Banque (optionnel)', false, 'ex: BNP Paribas'],
    ];
    for (const [testId, libelle, obligatoire, exemple] of attendus) {
      const champ = screen.getByTestId(testId);
      expect(screen.getByLabelText(libelle)).toBe(champ);
      expect(champ.getAttribute('aria-required')).toBe(obligatoire ? 'true' : null);
      expect(champ.getAttribute('placeholder')).toBe(exemple);
    }
    ['libelle', 'titulaire', 'banque'].forEach((nom) => expect(screen.getByTestId(`beneficiaires-${nom}`).getAttribute('maxlength')).toBe('100'));
    expect(screen.getByTestId('beneficiaires-iban').getAttribute('maxlength')).toBeNull();
    expect(screen.getByTestId('beneficiaires-verifier-iban').textContent).toBe("Verifier l'IBAN");
    expect(screen.getByTestId('beneficiaires-verifier-iban').getAttribute('type')).toBe('button');
    expect(screen.getByTestId('beneficiaires-ajouter').textContent).toBe('Ajouter le beneficiaire');
  });

  it.each([
    ['vide', { statut: 'vide' }, 'Veuillez saisir un IBAN.'],
    ['invalide', { statut: 'invalide' }, 'IBAN invalide (echec MOD-97). Verifiez la saisie.'],
    ['valide en zone SEPA', { statut: 'valide', codePays: 'FR', zoneSepa: true }, 'IBAN valide — FR (Zone SEPA)'],
    ['valide hors zone SEPA', { statut: 'valide', codePays: 'TR', zoneSepa: false }, 'IBAN valide — TR (Hors zone SEPA)'],
  ] as const)('[FR-013-04] la vérification %s affiche son message sous le champ, sans rien soumettre', async (_nom, verification, texte) => {
    const { verifierIbanBeneficiaire, ajouterBeneficiaire, listerBeneficiaires } = ouvrir({
      verifierIban: () => Promise.resolve<ReponseVerificationIban>({ statut: 'verifiee', verification }),
    });
    await screen.findByTestId('beneficiaires-aucun');
    const utilisateur = userEvent.setup();
    await utilisateur.type(screen.getByTestId('beneficiaires-iban'), 'FR35 3000');

    await utilisateur.click(screen.getByTestId('beneficiaires-verifier-iban'));

    const resultat = await screen.findByText(texte);
    expect(resultat.getAttribute('aria-live')).toBe('polite');
    expect(resultat.getAttribute('data-testid')).toBe('beneficiaires-resultat-iban');
    expect(verifierIbanBeneficiaire).toHaveBeenCalledExactlyOnceWith('FR35 3000');
    expect(ajouterBeneficiaire).not.toHaveBeenCalled();
    expect(listerBeneficiaires).toHaveBeenCalledTimes(1);
    expect((screen.getByTestId('beneficiaires-iban') as HTMLInputElement).value).toBe('FR35 3000');
  });

  it('[FR-013-04] un service de vérification indisponible affiche le texte technique', async () => {
    ouvrir();
    await screen.findByTestId('beneficiaires-aucun');

    await userEvent.setup().click(screen.getByTestId('beneficiaires-verifier-iban'));

    expect((await screen.findByTestId('beneficiaires-resultat-iban')).textContent).toBe('Une erreur technique est survenue');
  });

  it.each([
    ['libelle', 'Le libellé est obligatoire.'],
    ['iban', "L'IBAN est obligatoire."],
    ['iban', 'IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97).'],
    ['titulaire', 'Le nom du titulaire est obligatoire.'],
    ['bic', 'BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques.'],
  ] as const)('[FR-013-05] un refus sur %s affiche « %s » en alerte, champ en erreur, valeurs conservées', async (champ, message) => {
    const { ajouterBeneficiaire } = ouvrir({ ajouter: () => Promise.resolve<ReponseAjout>({ statut: 'refuse', message, champ }) });
    await screen.findByTestId('beneficiaires-aucun');
    const utilisateur = userEvent.setup();
    await utilisateur.type(screen.getByTestId('beneficiaires-libelle'), 'Loyer');
    await utilisateur.type(screen.getByTestId('beneficiaires-iban'), 'FR35');

    await utilisateur.click(screen.getByTestId('beneficiaires-ajouter'));

    const alerte = await screen.findByRole('alert');
    expect(alerte.textContent).toBe(message);
    expect(screen.getByTestId(`beneficiaires-${champ}`).getAttribute('aria-invalid')).toBe('true');
    expect(document.activeElement).toBe(screen.getByTestId(`beneficiaires-${champ}`));
    expect(screen.getAllByRole('textbox').filter((c) => c.getAttribute('aria-invalid') === 'true')).toHaveLength(1);
    expect((screen.getByTestId('beneficiaires-libelle') as HTMLInputElement).value).toBe('Loyer');
    expect((screen.getByTestId('beneficiaires-iban') as HTMLInputElement).value).toBe('FR35');
    expect(screen.queryByTestId('beneficiaires-message-info')).toBeNull();
    expect(ajouterBeneficiaire).toHaveBeenCalledWith({ libelle: 'Loyer', titulaire: '', iban: 'FR35', bic: '', banque: '' });
  });

  it('[FR-013-06] un refus sans champ affiche l’alerte sans marquer de champ', async () => {
    ouvrir({ ajouter: () => Promise.resolve<ReponseAjout>({ statut: 'refuse', message: 'Requête invalide', champ: null }) });
    await screen.findByTestId('beneficiaires-aucun');

    await userEvent.setup().click(screen.getByTestId('beneficiaires-ajouter'));

    expect((await screen.findByRole('alert')).textContent).toBe('Requête invalide');
    expect(screen.queryByTestId('beneficiaires-libelle')?.getAttribute('aria-invalid')).toBeNull();
  });

  it('[FR-013-08] un ajout réussi affiche le succès, recharge la liste et vide le formulaire', async () => {
    let actifs: BeneficiaireBrut[] = [];
    const { ajouterBeneficiaire, listerBeneficiaires } = ouvrir({
      lister: () => liste(...actifs),
      ajouter: () => {
        actifs = [{ id: 5, libelle: '[retro] test beneficiaire', titulaire: 'Retro Test SA', iban: 'FR35***9010', bic: 'HSBCFRPPXXX', banque: 'HSBC France', zoneSepa: true }];
        return Promise.resolve<ReponseAjout>({ statut: 'ajoute' });
      },
    });
    await screen.findByTestId('beneficiaires-aucun');
    const utilisateur = userEvent.setup();
    await utilisateur.type(screen.getByTestId('beneficiaires-libelle'), '[[retro] test beneficiaire');
    await utilisateur.type(screen.getByTestId('beneficiaires-titulaire'), 'Retro Test SA');
    await utilisateur.type(screen.getByTestId('beneficiaires-iban'), 'FR3530004000000123456789010');
    await utilisateur.type(screen.getByTestId('beneficiaires-bic'), 'HSBCFRPPXXX');
    await utilisateur.type(screen.getByTestId('beneficiaires-banque'), 'HSBC France');

    await utilisateur.click(screen.getByTestId('beneficiaires-ajouter'));

    const info = await screen.findByTestId('beneficiaires-message-info');
    expect(info.textContent).toBe('Bénéficiaire ajouté avec succès.');
    expect(info.getAttribute('role')).toBe('status');
    expect(await screen.findByTestId('beneficiaires-ligne-5')).toBeTruthy();
    expect(within(screen.getByTestId('beneficiaires-ligne-5')).getAllByRole('cell').map((c) => c.textContent)).toEqual([
      '[retro] test beneficiaire', 'Retro Test SA', 'FR35***9010', 'HSBCFRPPXXX', 'HSBC France', '✓ SEPA', 'Supprimer',
    ]);
    expect(ajouterBeneficiaire).toHaveBeenCalledWith({
      libelle: '[retro] test beneficiaire', titulaire: 'Retro Test SA', iban: 'FR3530004000000123456789010', bic: 'HSBCFRPPXXX', banque: 'HSBC France',
    });
    expect(listerBeneficiaires).toHaveBeenCalledTimes(2);
    for (const nom of ['libelle', 'titulaire', 'iban', 'bic', 'banque']) {
      expect((screen.getByTestId(`beneficiaires-${nom}`) as HTMLInputElement).value).toBe('');
    }
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('[FR-013-08] un ajout réussi dont la relecture échoue affiche le succès et le texte technique', async () => {
    let appels = 0;
    ouvrir({
      lister: () => (++appels === 1 ? liste() : Promise.resolve<ReponseBeneficiaires>({ statut: 'technique' })),
      ajouter: () => Promise.resolve<ReponseAjout>({ statut: 'ajoute' }),
    });
    await screen.findByTestId('beneficiaires-aucun');

    await userEvent.setup().click(screen.getByTestId('beneficiaires-ajouter'));

    expect((await screen.findByTestId('beneficiaires-message-erreur')).textContent).toBe('Une erreur technique est survenue');
    expect(screen.getByTestId('beneficiaires-message-info')).toBeTruthy();
  });

  it('[FR-013-08] une erreur technique affiche le message unique sans détail et garde la saisie', async () => {
    ouvrir({ ajouter: () => Promise.resolve<ReponseAjout>({ statut: 'technique' }) });
    await screen.findByTestId('beneficiaires-aucun');
    const utilisateur = userEvent.setup();
    await utilisateur.type(screen.getByTestId('beneficiaires-libelle'), 'Loyer');

    await utilisateur.click(screen.getByTestId('beneficiaires-ajouter'));

    expect((await screen.findByRole('alert')).textContent).toBe("Erreur lors de l'ajout du bénéficiaire. Veuillez réessayer.");
    expect((screen.getByTestId('beneficiaires-libelle') as HTMLInputElement).value).toBe('Loyer');
    expect(screen.queryByTestId('beneficiaires-message-info')).toBeNull();
  });

  it('[FR-013-08] un refus après un succès efface le message de succès', async () => {
    let reponses: ReponseAjout[] = [{ statut: 'ajoute' }, { statut: 'refuse', message: 'Le libellé est obligatoire.', champ: 'libelle' }];
    ouvrir({ ajouter: () => Promise.resolve(reponses.shift() as ReponseAjout) });
    await screen.findByTestId('beneficiaires-aucun');
    const utilisateur = userEvent.setup();

    await utilisateur.click(screen.getByTestId('beneficiaires-ajouter'));
    await screen.findByTestId('beneficiaires-message-info');
    await utilisateur.click(screen.getByTestId('beneficiaires-ajouter'));

    await screen.findByRole('alert');
    expect(screen.queryByTestId('beneficiaires-message-info')).toBeNull();
    reponses = [];
  });

  it('[FR-013-08] une suppression efface le succès et l’erreur de champ précédents', async () => {
    ouvrir({
      lister: () => liste(LOYER),
      ajouter: () => Promise.resolve<ReponseAjout>({ statut: 'refuse', message: 'Le libellé est obligatoire.', champ: 'libelle' }),
      supprimer: () => Promise.resolve<ReponseSuppression>({ statut: 'supprime' }),
    });
    await screen.findByTestId('beneficiaires-ligne-1');
    const utilisateur = userEvent.setup();
    await utilisateur.click(screen.getByTestId('beneficiaires-ajouter'));
    await screen.findByRole('alert');

    await utilisateur.click(screen.getByTestId('beneficiaires-supprimer-1'));

    await waitFor(() => expect(screen.queryByRole('alert')).toBeNull());
    expect(screen.getByTestId('beneficiaires-libelle').getAttribute('aria-invalid')).toBeNull();
  });
});
