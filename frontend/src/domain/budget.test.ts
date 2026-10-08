import { describe, expect, it } from 'vitest';
import { budgetAffichable, montantEuros, periodeDeLaDate, type BudgetBrut } from './budget';

const brut = (partiel: Partial<BudgetBrut> = {}): BudgetBrut => ({
  aujourdhui: '2026-11-17',
  periode: '2026-03',
  periodes: ['2026-03', '2026-02'],
  totalDepenses: '1234.50',
  totalRevenus: '2000.00',
  plafondsDefinis: 2,
  repartition: [{ categorie: 'Logement', montant: '1000.00' }],
  plafonds: [],
  ...partiel,
});

describe('[FR-006-02] période', () => {
  it('prend le mois de la date du jour du service par découpe de chaîne', () => {
    expect(periodeDeLaDate('2026-11-17')).toBe('2026-11');
  });

  it('sans aucune période proposée, la période est le mois de la date du jour du service', () => {
    const budget = budgetAffichable(brut({ periodes: [], periode: '2026-10' }));

    expect(budget.periode).toBe('2026-11');
    expect(budget.periodes).toEqual(['2026-11']);
  });

  it('garde la période du service et ses options quand elle y figure', () => {
    const budget = budgetAffichable(brut());

    expect(budget.periode).toBe('2026-03');
    expect(budget.periodes).toEqual(['2026-03', '2026-02']);
  });

  it('ajoute en tête une période du service absente des options', () => {
    expect(budgetAffichable(brut({ periode: '2025-01' })).periodes).toEqual(['2025-01', '2026-03', '2026-02']);
  });
});

describe('[FR-006-04] montants', () => {
  it.each([
    ['1234.50', '1234.50 EUR'],
    ['0.00', '0.00 EUR'],
    ['2000.00', '2000.00 EUR'],
  ])('affiche %s en %s', (chaine, attendu) => {
    expect(montantEuros(chaine)).toBe(attendu);
  });

  it('rend les totaux, le nombre de plafonds et les lignes de répartition', () => {
    const budget = budgetAffichable(brut());

    expect(budget.totalDepenses).toBe('1234.50 EUR');
    expect(budget.totalRevenus).toBe('2000.00 EUR');
    expect(budget.plafondsDefinis).toBe(2);
    expect(budget.repartition).toEqual([{ categorie: 'Logement', montant: '1000.00 EUR', progression: null }]);
    expect(budget.plafonds).toEqual([]);
  });
});

describe('[FR-006-09] progression des lignes de répartition', () => {
  it('lie le plafond à la ligne de sa catégorie et laisse les autres sans progression', () => {
    const budget = budgetAffichable(
      brut({
        repartition: [
          { categorie: 'Logement', montant: '400.00' },
          { categorie: 'Autres', montant: '10.00' },
        ],
        plafonds: [{ id: 3, categorie: 'Logement', plafond: '500.00', depense: '400.00' }],
      }),
    );

    expect(budget.repartition[0].progression).toEqual({ pourcentage: 80, remplissage: 80, couleur: 'warning', depasse: false });
    expect(budget.repartition[1].progression).toBeNull();
  });
});

describe('[FR-006-11] lignes du tableau des plafonds', () => {
  it('calcule l’écart plafond − dépense en centimes, négatif en cas de dépassement', () => {
    const budget = budgetAffichable(
      brut({
        plafonds: [
          { id: 1, categorie: 'Logement', plafond: '500.00', depense: '500.01' },
          { id: 2, categorie: 'Transport', plafond: '80.00', depense: '0.00' },
          { id: 3, categorie: 'Autres', plafond: '10.00', depense: '10.00' },
        ],
      }),
    );

    expect(budget.plafonds).toEqual([
      { id: 1, categorie: 'Logement', plafond: '500.00 EUR', depense: '500.01 EUR', ecart: '-0.01 EUR', ecartNegatif: true },
      { id: 2, categorie: 'Transport', plafond: '80.00 EUR', depense: '0.00 EUR', ecart: '80.00 EUR', ecartNegatif: false },
      { id: 3, categorie: 'Autres', plafond: '10.00 EUR', depense: '10.00 EUR', ecart: '0.00 EUR', ecartNegatif: false },
    ]);
  });
});
