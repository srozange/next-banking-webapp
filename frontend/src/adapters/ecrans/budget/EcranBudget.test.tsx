// @vitest-environment jsdom
import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientBudget, ReponseBudget } from '../../../application/consulterBudget';
import type { ClientPlafonds } from '../../../application/gererPlafonds';
import type { BudgetBrut } from '../../../domain/budget';
import { jetonDeTest } from '../../jeton/jetonDeTest';
import { rendreApplication } from '../../test/rendreApplication';

afterEach(() => {
  cleanup();
  localStorage.clear();
});

const BUDGET: BudgetBrut = {
  aujourdhui: '2026-11-17',
  periode: '2026-03',
  periodes: ['2026-03', '2026-02'],
  totalDepenses: '1234.50',
  totalRevenus: '2000.00',
  plafondsDefinis: 2,
  repartition: [
    { categorie: 'Logement', montant: '1000.00' },
    { categorie: 'Autres', montant: '234.50' },
  ],
  plafonds: [],
};

const reponse = (budget: BudgetBrut) => Promise.resolve<ReponseBudget>({ statut: 'budget', budget });

function ouvrir(lire: ClientBudget['lire'] = () => reponse(BUDGET), roles = ['CLIENT'], plafonds: Partial<ClientPlafonds> = {}) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles }));
  return rendreApplication('/budget', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { lire }, plafonds);
}

const liste = () => screen.getByTestId('budget-periode') as HTMLSelectElement;

describe('écran du budget', () => {
  it('[FR-006-01] sans session, redirige vers /login', async () => {
    const { routeur } = rendreApplication('/budget');

    await waitFor(() => expect(routeur.state.location.pathname).toBe('/login'));
  });

  it('[FR-006-01] affiche l’onglet et les quatre titres de section rendus en capitales par le CSS, sans bandeau technique', async () => {
    ouvrir();
    await waitFor(() => expect(liste().options).toHaveLength(2));

    expect(document.title).toBe('BankApp Demo - Budget');
    for (const titre of ['Total depenses', 'Total revenus', 'Repartition par categorie', 'Plafonds budgetaires']) {
      expect(screen.getByRole('heading', { name: titre }).className).toContain('ecran-budget__titre');
    }
    expect(screen.getByRole('heading', { name: 'Plafonds definis' })).toBeTruthy();
    expect(document.body.textContent).not.toContain('Legacy');
    expect(document.body.textContent).not.toContain('Categorisation automatique');
  });

  it('[FR-006-02] propose les périodes du service et sélectionne la plus récente', async () => {
    const { lireBudget } = ouvrir();
    await waitFor(() => expect(liste().options).toHaveLength(2));

    expect(screen.getByLabelText('Periode :')).toBe(liste());
    expect(Array.from(liste().options).map((option) => option.value)).toEqual(['2026-03', '2026-02']);
    expect(liste().value).toBe('2026-03');
    expect(lireBudget).toHaveBeenCalledWith(null);
  });

  it('[FR-006-02] sans opération, la période est le mois de la date du jour fournie par le service', async () => {
    ouvrir(() => reponse({ ...BUDGET, periode: '2026-11', periodes: [], totalDepenses: '0.00', totalRevenus: '0.00', repartition: [] }));
    await waitFor(() => expect(liste().options).toHaveLength(1));

    expect(liste().value).toBe('2026-11');
    expect(screen.getByTestId('budget-repartition-titre').textContent).toBe('Depenses du mois 2026-11');
  });

  it('[FR-006-02] le changement de période recharge aussitôt, puis « Afficher » recharge à nouveau', async () => {
    const u = userEvent.setup();
    const { lireBudget } = ouvrir();
    await waitFor(() => expect(liste().options).toHaveLength(2));

    await u.selectOptions(liste(), '2026-02');
    await waitFor(() => expect(lireBudget).toHaveBeenCalledTimes(2));
    expect(lireBudget).toHaveBeenLastCalledWith('2026-02');
    expect(liste().value).toBe('2026-03');

    await u.click(screen.getByTestId('budget-afficher'));
    await waitFor(() => expect(lireBudget).toHaveBeenCalledTimes(3));
    expect(lireBudget).toHaveBeenLastCalledWith('2026-03');
  });

  it('[FR-006-02] « Afficher » avant tout chargement laisse le service choisir la période', async () => {
    const u = userEvent.setup();
    const { lireBudget } = ouvrir(() => Promise.resolve({ statut: 'technique' }));
    await screen.findByTestId('budget-message-erreur');

    await u.click(screen.getByTestId('budget-afficher'));

    await waitFor(() => expect(lireBudget).toHaveBeenCalledTimes(2));
    expect(lireBudget).toHaveBeenLastCalledWith(null);
  });

  it('[FR-006-03] liste une ligne par catégorie avec son montant, sous « Depenses du mois {AAAA-MM} »', async () => {
    ouvrir();
    await screen.findByTestId('budget-ligne-logement');

    expect(screen.getByTestId('budget-repartition-titre').textContent).toBe('Depenses du mois 2026-03');
    expect(screen.getByTestId('budget-ligne-logement').textContent).toBe('Logement1000.00 EURAucun plafond defini');
    expect(screen.getByTestId('budget-montant-autres').textContent).toBe('234.50 EUR');
    expect(screen.queryByTestId('budget-aucune-depense')).toBeNull();
  });

  it('[FR-006-03] sans débit, affiche « Aucune depense enregistree sur cette periode »', async () => {
    ouvrir(() => reponse({ ...BUDGET, repartition: [] }));
    await screen.findByTestId('budget-aucune-depense');

    expect(screen.getByTestId('budget-aucune-depense').textContent).toBe('Aucune depense enregistree sur cette periode');
  });

  it('[FR-006-04] affiche les totaux à deux décimales et le nombre de plafonds', async () => {
    ouvrir();
    await waitFor(() => expect(screen.getByTestId('budget-total-depenses').textContent).toBe('1234.50 EUR'));

    expect(screen.getByTestId('budget-total-revenus').textContent).toBe('2000.00 EUR');
    expect(screen.getByTestId('budget-plafonds-definis').textContent).toBe('2');
  });

  it('[FR-006-04] une panne du service affiche le texte technique générique, sans détail, et conserve l’écran', async () => {
    ouvrir(() => Promise.reject(new Error('détail secret')));
    await screen.findByTestId('budget-message-erreur');

    expect(screen.getByTestId('budget-message-erreur').textContent).toBe('Une erreur technique est survenue');
    expect(document.body.textContent).not.toContain('détail secret');
    expect(screen.getByTestId('budget-afficher')).toBeTruthy();
    expect(screen.getByTestId('budget-total-depenses').textContent).toBe('');
  });

  it('[FR-006-04] une panne après un chargement garde les données affichées', async () => {
    const u = userEvent.setup();
    let appel = 0;
    ouvrir(() => (++appel === 1 ? reponse(BUDGET) : Promise.resolve({ statut: 'technique' })));
    await waitFor(() => expect(screen.getByTestId('budget-total-depenses').textContent).toBe('1234.50 EUR'));

    await u.click(screen.getByTestId('budget-afficher'));

    await screen.findByTestId('budget-message-erreur');
    expect(screen.getByTestId('budget-total-depenses').textContent).toBe('1234.50 EUR');
  });

  it('[FR-006-01] une réponse tardive d’une demande remplacée est ignorée', async () => {
    const u = userEvent.setup();
    let liberer: (r: ReponseBudget) => void = () => undefined;
    let appel = 0;
    ouvrir(() => {
      appel += 1;
      if (appel === 1) {
        return new Promise<ReponseBudget>((resolve) => {
          liberer = resolve;
        });
      }
      return reponse({ ...BUDGET, totalDepenses: '5.00' });
    });
    await waitFor(() => expect(appel).toBe(1));

    await u.click(screen.getByTestId('budget-afficher'));
    await waitFor(() => expect(screen.getByTestId('budget-total-depenses').textContent).toBe('5.00 EUR'));
    liberer({ statut: 'budget', budget: BUDGET });
    await new Promise((resolve) => setTimeout(resolve, 10));

    expect(screen.getByTestId('budget-total-depenses').textContent).toBe('5.00 EUR');
  });
});

const VIDE_PLAFOND = { ...BUDGET, plafondsDefinis: 0 };
const progressions = (depense: string) => ({
  ...BUDGET,
  repartition: [
    { categorie: 'Logement', montant: depense },
    { categorie: 'Autres', montant: '234.50' },
  ],
  plafondsDefinis: 1,
  plafonds: [{ id: 5, categorie: 'Logement', plafond: '500.00', depense }],
});

describe('formulaire de plafond', () => {
  it('[FR-006-05] affiche le formulaire sous « Plafonds budgetaires » avec ses libellés et le bouton', async () => {
    ouvrir(() => reponse(VIDE_PLAFOND));
    await screen.findByTestId('budget-ligne-logement');

    const categorie = screen.getByLabelText('Categorie') as HTMLSelectElement;
    expect(categorie).toBe(screen.getByTestId('budget-categorie'));
    expect(Array.from(categorie.options).map((option) => option.textContent)).toEqual([
      '-- Choisir --', 'Logement', 'Alimentation', 'Transport', 'Sante', 'Loisirs', 'Abonnements', 'Epargne', 'Assurances', 'Autres',
    ]);
    expect(Array.from(categorie.options).map((option) => option.textContent)).not.toContain('Revenus');
    const plafond = screen.getByLabelText('Plafond mensuel (EUR)') as HTMLInputElement;
    expect(plafond).toBe(screen.getByTestId('budget-plafond'));
    expect(plafond.placeholder).toBe('ex: 500');
    expect(screen.getByTestId('budget-enregistrer').textContent).toBe('Enregistrer le plafond');
    const titre = screen.getByRole('heading', { name: 'Plafonds budgetaires' });
    expect(titre.compareDocumentPosition(categorie) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('[FR-006-05] refuse sans appel : sans catégorie, puis plafond vide, nul, négatif, non numérique, trop grand', async () => {
    const u = userEvent.setup();
    const { enregistrerPlafond } = ouvrir(() => reponse(VIDE_PLAFOND));
    await screen.findByTestId('budget-ligne-logement');
    const saisie = screen.getByTestId('budget-plafond') as HTMLInputElement;

    await u.click(screen.getByTestId('budget-enregistrer'));
    expect(screen.getByRole('alert').textContent).toBe('Veuillez selectionner une categorie');
    expect(screen.getByTestId('budget-categorie').getAttribute('aria-invalid')).toBe('true');

    await u.selectOptions(screen.getByTestId('budget-categorie'), 'Logement');
    for (const valeur of ['', '0', '-10', 'abc']) {
      await u.clear(saisie);
      if (valeur !== '') {
        await u.type(saisie, valeur);
      }
      await u.click(screen.getByTestId('budget-enregistrer'));
      expect(screen.getByRole('alert').textContent).toBe('Veuillez saisir un plafond valide');
      expect(saisie.getAttribute('aria-invalid')).toBe('true');
      expect(saisie.value).toBe(valeur);
    }
    await u.clear(saisie);
    await u.type(saisie, '12345678901234');
    await u.click(screen.getByTestId('budget-enregistrer'));
    expect(screen.getByRole('alert').textContent).toBe('Le plafond ne doit pas dépasser 13 chiffres avant la virgule');
    expect(enregistrerPlafond).not.toHaveBeenCalled();
  });

  it('[FR-006-07] enregistre, annonce le succès, vide les champs et recharge le budget de la période affichée', async () => {
    const u = userEvent.setup();
    let appel = 0;
    const { enregistrerPlafond, lireBudget } = ouvrir(
      () => reponse(++appel === 1 ? VIDE_PLAFOND : progressions('400.00')),
      ['CLIENT'],
      { enregistrer: () => Promise.resolve({ statut: 'enregistre' }) },
    );
    await screen.findByTestId('budget-ligne-logement');

    await u.selectOptions(screen.getByTestId('budget-categorie'), 'Logement');
    await u.type(screen.getByTestId('budget-plafond'), '500');
    await u.click(screen.getByTestId('budget-enregistrer'));

    expect(enregistrerPlafond).toHaveBeenCalledWith({ categorie: 'Logement', mois: '2026-03', plafond: '500.00' });
    expect((await screen.findByRole('status')).textContent).toBe('Plafond enregistre pour Logement');
    expect((screen.getByTestId('budget-categorie') as HTMLSelectElement).value).toBe('');
    expect((screen.getByTestId('budget-plafond') as HTMLInputElement).value).toBe('');
    await waitFor(() => expect(screen.getByTestId('budget-plafonds-definis').textContent).toBe('1'));
    expect(lireBudget).toHaveBeenLastCalledWith('2026-03');
    expect(screen.getByTestId('budget-plafond-ligne-5')).toBeTruthy();
  });

  it('[FR-006-05] un refus du service s’affiche en alerte, saisie conservée', async () => {
    const u = userEvent.setup();
    ouvrir(() => reponse(VIDE_PLAFOND), ['CLIENT'], {
      enregistrer: () => Promise.resolve({ statut: 'refuse', message: 'Veuillez selectionner une categorie' }),
    });
    await screen.findByTestId('budget-ligne-logement');
    await u.selectOptions(screen.getByTestId('budget-categorie'), 'Logement');
    await u.type(screen.getByTestId('budget-plafond'), '500');

    await u.click(screen.getByTestId('budget-enregistrer'));

    expect((await screen.findByRole('alert')).textContent).toBe('Veuillez selectionner une categorie');
    expect(screen.getByTestId('budget-categorie').getAttribute('aria-invalid')).toBe('true');
    expect((screen.getByTestId('budget-plafond') as HTMLInputElement).value).toBe('500');
  });

  it('[FR-006-05] un autre refus du service désigne le champ du plafond', async () => {
    const u = userEvent.setup();
    ouvrir(() => reponse(VIDE_PLAFOND), ['CLIENT'], {
      enregistrer: () => Promise.resolve({ statut: 'refuse', message: 'Veuillez saisir un plafond valide' }),
    });
    await screen.findByTestId('budget-ligne-logement');
    await u.selectOptions(screen.getByTestId('budget-categorie'), 'Logement');
    await u.type(screen.getByTestId('budget-plafond'), '500');

    await u.click(screen.getByTestId('budget-enregistrer'));

    await screen.findByRole('alert');
    expect(screen.getByTestId('budget-plafond').getAttribute('aria-invalid')).toBe('true');
  });

  it('[FR-006-05] une erreur technique à l’enregistrement affiche le texte générique sans détail', async () => {
    const u = userEvent.setup();
    ouvrir(() => reponse(VIDE_PLAFOND), ['CLIENT'], { enregistrer: () => Promise.reject(new Error('détail secret')) });
    await screen.findByTestId('budget-ligne-logement');
    await u.selectOptions(screen.getByTestId('budget-categorie'), 'Logement');
    await u.type(screen.getByTestId('budget-plafond'), '500');

    await u.click(screen.getByTestId('budget-enregistrer'));

    expect((await screen.findByRole('alert')).textContent).toBe('Une erreur est survenue. Veuillez reessayer.');
    expect(document.body.textContent).not.toContain('détail secret');
    expect(document.body.textContent).not.toContain('Erreur :');
    expect((screen.getByTestId('budget-plafond') as HTMLInputElement).value).toBe('500');
  });

  it('[FR-006-05] le bouton est inactif tant que le budget n’est pas chargé', async () => {
    ouvrir(() => Promise.resolve({ statut: 'technique' }));
    await screen.findByTestId('budget-message-erreur');

    expect((screen.getByTestId('budget-enregistrer') as HTMLButtonElement).disabled).toBe(true);
  });
});

describe('progression par catégorie', () => {
  it.each([
    ['399.00', '79', 'success', false],
    ['400.00', '80', 'warning', false],
    ['500.00', '100', 'error', false],
    ['500.01', '100', 'error', true],
  ])('[FR-006-09] plafond 500, dépense %s : %s %% %s, badge %s', async (depense, pourcentage, couleur, badge) => {
    ouvrir(() => reponse(progressions(depense)));
    const barre = await screen.findByTestId('budget-barre-logement');

    expect(barre.getAttribute('role')).toBe('progressbar');
    expect(barre.getAttribute('aria-valuenow')).toBe(pourcentage);
    expect(barre.getAttribute('aria-valuemin')).toBe('0');
    expect(barre.getAttribute('aria-valuemax')).toBe('100');
    expect(screen.getByTestId('budget-pourcentage-logement').textContent).toBe(`${pourcentage}% du plafond`);
    const remplissage = barre.firstElementChild as HTMLElement;
    expect(remplissage.className).toContain(`ecran-budget__remplissage--${couleur}`);
    expect(remplissage.style.width).toBe(`${pourcentage}%`);
    expect(screen.queryByTestId('budget-depassement-logement') !== null).toBe(badge);
  });

  it('[FR-006-10] un dépassement porte le badge « Depassement », le fond rose et le montant en rouge', async () => {
    ouvrir(() => reponse(progressions('500.01')));
    await screen.findByTestId('budget-barre-logement');

    expect(screen.getByTestId('budget-depassement-logement').textContent).toBe('Depassement');
    expect(screen.getByTestId('budget-ligne-logement').className).toContain('ecran-budget__ligne--depassement');
    expect(screen.getByTestId('budget-montant-logement').className).toContain('ecran-budget__montant--depasse');
  });

  it('[FR-006-10] à l’égalité, ni badge, ni fond de dépassement, ni montant rouge', async () => {
    ouvrir(() => reponse(progressions('500.00')));
    await screen.findByTestId('budget-barre-logement');

    expect(screen.getByTestId('budget-ligne-logement').className).not.toContain('depassement');
    expect(screen.getByTestId('budget-montant-logement').className).toBe('');
  });

  it('[FR-006-09] le remplissage est plafonné à 100 pour une forte dépense, le libellé garde le pourcentage', async () => {
    ouvrir(() => reponse(progressions('1000.00')));
    const barre = await screen.findByTestId('budget-barre-logement');

    expect((barre.firstElementChild as HTMLElement).style.width).toBe('100%');
    expect(screen.getByTestId('budget-pourcentage-logement').textContent).toBe('200% du plafond');
  });

  it('[FR-006-09] une catégorie sans plafond affiche « Aucun plafond defini » et pas de barre', async () => {
    ouvrir(() => reponse(progressions('100.00')));
    await screen.findByTestId('budget-barre-logement');

    expect(screen.getByTestId('budget-aucun-plafond-autres').textContent).toBe('Aucun plafond defini');
    expect(screen.queryByTestId('budget-barre-autres')).toBeNull();
  });
});

describe('tableau des plafonds du mois', () => {
  it('[FR-006-11] sans plafond, le tableau n’est pas affiché', async () => {
    ouvrir(() => reponse(VIDE_PLAFOND));
    await screen.findByTestId('budget-ligne-logement');

    expect(screen.queryByTestId('budget-tableau-plafonds')).toBeNull();
    expect(screen.queryByTestId('budget-plafonds-titre')).toBeNull();
  });

  it('[FR-006-11] liste les plafonds sous « Plafonds du mois {AAAA-MM} » avec écarts rouge et vert', async () => {
    ouvrir(() =>
      reponse({
        ...BUDGET,
        plafondsDefinis: 2,
        plafonds: [
          { id: 5, categorie: 'Logement', plafond: '500.00', depense: '1000.00' },
          { id: 6, categorie: 'Transport', plafond: '80.00', depense: '0.00' },
        ],
      }),
    );
    await screen.findByTestId('budget-tableau-plafonds');

    expect(screen.getByTestId('budget-plafonds-titre').textContent).toBe('Plafonds du mois 2026-03');
    expect(screen.getAllByRole('columnheader').map((colonne) => colonne.textContent)).toEqual(['Categorie', 'Plafond', 'Depense', 'Ecart', 'Action']);
    expect(screen.getAllByRole('columnheader').every((colonne) => colonne.getAttribute('scope') === 'col')).toBe(true);
    expect(screen.getByTestId('budget-plafond-ligne-5').textContent).toBe('Logement500.00 EUR1000.00 EUR-500.00 EURSupprimer');
    expect(screen.getByTestId('budget-ecart-5').className).toContain('ecran-budget__ecart--negatif');
    expect(screen.getByTestId('budget-ecart-6').textContent).toBe('80.00 EUR');
    expect(screen.getByTestId('budget-ecart-6').className).toContain('ecran-budget__ecart--positif');
  });

  it('[FR-006-08] « Supprimer » supprime sans confirmation puis recharge la liste', async () => {
    const u = userEvent.setup();
    let appel = 0;
    const avec = { ...BUDGET, plafondsDefinis: 1, plafonds: [{ id: 5, categorie: 'Logement', plafond: '500.00', depense: '1000.00' }] };
    const { supprimerPlafond, lireBudget } = ouvrir(() => reponse(++appel === 1 ? avec : VIDE_PLAFOND), ['CLIENT'], {
      supprimer: () => Promise.resolve({ statut: 'supprime' }),
    });
    await screen.findByTestId('budget-supprimer-5');

    await u.click(screen.getByTestId('budget-supprimer-5'));

    expect(supprimerPlafond).toHaveBeenCalledWith(5);
    await waitFor(() => expect(screen.queryByTestId('budget-tableau-plafonds')).toBeNull());
    expect(lireBudget).toHaveBeenCalledTimes(2);
    expect(screen.queryByRole('alert')).toBeNull();
  });

  it('[FR-006-08] une erreur technique à la suppression affiche le texte générique sans détail', async () => {
    const u = userEvent.setup();
    ouvrir(() => reponse({ ...BUDGET, plafondsDefinis: 1, plafonds: [{ id: 5, categorie: 'Logement', plafond: '500.00', depense: '1.00' }] }), ['CLIENT'], {
      supprimer: () => Promise.reject(new Error('détail secret')),
    });
    await screen.findByTestId('budget-supprimer-5');

    await u.click(screen.getByTestId('budget-supprimer-5'));

    expect((await screen.findByRole('alert')).textContent).toBe('Une erreur est survenue. Veuillez reessayer.');
    expect(document.body.textContent).not.toContain('Erreur suppression');
    expect(screen.getByTestId('budget-plafond-ligne-5')).toBeTruthy();
  });
});
