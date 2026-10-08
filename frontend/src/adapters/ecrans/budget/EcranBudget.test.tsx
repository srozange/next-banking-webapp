// @vitest-environment jsdom
import { cleanup, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import type { ClientBudget, ReponseBudget } from '../../../application/consulterBudget';
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
};

const reponse = (budget: BudgetBrut) => Promise.resolve<ReponseBudget>({ statut: 'budget', budget });

function ouvrir(lire: ClientBudget['lire'] = () => reponse(BUDGET), roles = ['CLIENT']) {
  localStorage.setItem('nb.jeton', jetonDeTest({ roles }));
  return rendreApplication('/budget', undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, undefined, { lire });
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
    expect(screen.getByTestId('budget-ligne-logement').textContent).toBe('Logement1000.00 EUR');
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
