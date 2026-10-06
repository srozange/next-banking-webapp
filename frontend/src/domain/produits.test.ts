import { describe, expect, it } from 'vitest';
import {
  formaterBooleen,
  formaterDepotMin,
  formaterDuree,
  formaterInstant,
  formaterNombre,
  formaterPourcentage,
  formaterRisque,
  valeurOuAbsent,
} from './produits';

describe('produits : taux en pourcentage', () => {
  it.each([
    ['0.0300', '3,00 %'],
    ['0.0650', '6,50 %'],
    ['0.8000', '80,00 %'],
    ['0.0175', '1,75 %'],
    ['0.0225', '2,25 %'],
    ['0.15', '15,00 %'],
    ['0', '0,00 %'],
    ['1', '100,00 %'],
    ['0.0005', '0,05 %'],
  ])('[FR-011-05][BUG-039] %s s’affiche %s', (taux, attendu) => {
    const affiche = formaterPourcentage(taux);

    expect(affiche).toBe(attendu);
    expect(affiche).not.toContain('0.03');
    expect(affiche).not.toBe('0,03 %');
  });

  it('[FR-011-04] un taux absent s’affiche « — »', () => {
    expect(formaterPourcentage(null)).toBe('—');
  });

  it.each(['abc', '0,03', '0.12345', '-0.01', ''])('[FR-011-05] rejette le taux invalide %j', (taux) => {
    expect(() => formaterPourcentage(taux)).toThrow(`Taux invalide : ${taux}`);
  });
});

describe('produits : autres valeurs', () => {
  it('[FR-011-04] dépôt minimal en euros, sans flottant, ou « — »', () => {
    expect(formaterDepotMin('300.00')).toBe('300,00 EUR');
    expect(formaterDepotMin('10.05')).toBe('10,05 EUR');
    expect(formaterDepotMin('0.00')).toBe('0,00 EUR');
    expect(formaterDepotMin(null)).toBe('—');
  });

  it('[FR-011-03] booléens « Oui » et « Non »', () => {
    expect(formaterBooleen(true)).toBe('Oui');
    expect(formaterBooleen(false)).toBe('Non');
  });

  it('[FR-011-03][FR-011-04] durée « {n} mois », « Illimite » pour l’épargne, « — » pour le crédit', () => {
    expect(formaterDuree(120, 'Illimite')).toBe('120 mois');
    expect(formaterDuree(null, 'Illimite')).toBe('Illimite');
    expect(formaterDuree(null, '—')).toBe('—');
  });

  it('[FR-011-04] valeur absente (null ou vide) : « — »', () => {
    expect(valeurOuAbsent('Immobilier tertiaire')).toBe('Immobilier tertiaire');
    expect(valeurOuAbsent(null)).toBe('—');
    expect(valeurOuAbsent('')).toBe('—');
  });

  it.each([
    [1, 'Tres faible (1/5)'],
    [2, 'Faible (2/5)'],
    [3, 'Moyen (3/5)'],
    [4, 'Eleve (4/5)'],
    [5, 'Tres eleve (5/5)'],
    [0, 'N/A'],
    [6, 'N/A'],
    [2.5, 'N/A'],
    [null, 'N/A'],
  ])('[FR-011-06] niveau de risque %s : %s', (niveau, attendu) => {
    expect(formaterRisque(niveau)).toBe(attendu);
  });
});

describe('produits : journal des traitements', () => {
  it('[FR-011-09] découpe l’instant ISO sans conversion de fuseau', () => {
    expect(formaterInstant('2026-10-06T02:00:05+02:00')).toBe('06/10/2026 02:00:05');
    expect(formaterInstant('2026-01-01T03:00:00.123456+01:00')).toBe('01/01/2026 03:00:00');
    expect(formaterInstant('2026-10-06T23:59Z')).toBe('06/10/2026 23:59:00');
  });

  it('[FR-011-09] la seconde omise par le service vaut 00 ; un instant absent est vide', () => {
    expect(formaterInstant('2026-10-06T02:30+02:00')).toBe('06/10/2026 02:30:00');
    expect(formaterInstant(null)).toBe('');
  });

  it('[FR-011-09] rejette un instant illisible', () => {
    expect(() => formaterInstant('hier')).toThrow('Instant invalide : hier');
  });

  it('[FR-011-09] un nombre absent est une cellule vide', () => {
    expect(formaterNombre(12)).toBe('12');
    expect(formaterNombre(0)).toBe('0');
    expect(formaterNombre(null)).toBe('');
  });
});
