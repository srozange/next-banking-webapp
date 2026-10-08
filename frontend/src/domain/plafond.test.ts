import { describe, expect, it } from 'vitest';
import { CATEGORIES_PLAFONNABLES, lirePlafond, progression, refusDePlafond } from './plafond';

describe('[FR-006-06] catégories plafonnables', () => {
  it('propose les neuf catégories de dépenses, sans « Revenus »', () => {
    expect(CATEGORIES_PLAFONNABLES).toEqual(['Logement', 'Alimentation', 'Transport', 'Sante', 'Loisirs', 'Abonnements', 'Epargne', 'Assurances', 'Autres']);
    expect(CATEGORIES_PLAFONNABLES).not.toContain('Revenus');
  });

  it('[BUG-023] refuse la catégorie Revenus comme une catégorie inconnue', () => {
    expect(refusDePlafond({ categorie: 'Revenus', plafond: '500' })).toBe('categorie');
  });
});

describe('[FR-006-05] lecture du plafond saisi', () => {
  it.each([
    ['500', 50000],
    ['500,5', 50050],
    ['500.50', 50050],
    ['  12 ', 1200],
    ['0.01', 1],
    ['007', 700],
    ['10.005', 1001],
    ['10.004', 1000],
    ['9999999999999', 999999999999900],
    ['9999999999999.99', 999999999999999],
  ])('lit %s en %i centimes', (saisie, centimes) => {
    expect(lirePlafond(saisie)).toEqual({ centimes });
  });

  it.each(['', '   ', '0', '0.00', '0.001', '-10', 'abc', '1e3', '12,3,4', '5 €'])('refuse « %s » comme plafond invalide', (saisie) => {
    expect(lirePlafond(saisie)).toEqual({ refus: 'plafond' });
  });

  it.each(['10000000000000', '12345678901234.5', '9999999999999.995'])('refuse %s : plus de 13 chiffres entiers', (saisie) => {
    expect(lirePlafond(saisie)).toEqual({ refus: 'plafondTropGrand' });
  });
});

describe('[FR-006-05] refus du formulaire dans l’ordre du legacy', () => {
  it('contrôle la catégorie avant le plafond', () => {
    expect(refusDePlafond({ categorie: '', plafond: '' })).toBe('categorie');
    expect(refusDePlafond({ categorie: 'Logement', plafond: '' })).toBe('plafond');
    expect(refusDePlafond({ categorie: 'Logement', plafond: '-10' })).toBe('plafond');
    expect(refusDePlafond({ categorie: 'Logement', plafond: '12345678901234' })).toBe('plafondTropGrand');
    expect(refusDePlafond({ categorie: 'Logement', plafond: '500' })).toBeNull();
  });
});

describe('[FR-006-09] progression', () => {
  it.each([
    [39900, 79, 'success', false],
    [40000, 80, 'warning', false],
    [49999, 99, 'warning', false],
    [50000, 100, 'error', false],
    [50001, 100, 'error', true],
    [100000, 200, 'error', true],
    [0, 0, 'success', false],
  ] as const)('dépense %i sur 500,00 : %i %% %s, dépassement %s', (depense, pourcentage, couleur, depasse) => {
    expect(progression(depense, 50000)).toEqual({ pourcentage, remplissage: Math.min(pourcentage, 100), couleur, depasse });
  });

  it('arrondit le pourcentage vers le bas, sans flottant', () => {
    expect(progression(1, 3).pourcentage).toBe(33);
    expect(progression(2, 3).pourcentage).toBe(66);
  });
});

describe('[FR-006-10] dépassement strict', () => {
  it('500,00 sur 500,00 est rouge sans dépassement, 500,01 dépasse', () => {
    expect(progression(50000, 50000)).toMatchObject({ couleur: 'error', depasse: false });
    expect(progression(50001, 50000)).toMatchObject({ couleur: 'error', depasse: true, remplissage: 100 });
  });
});
