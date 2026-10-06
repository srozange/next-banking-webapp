import { describe, expect, it } from 'vitest';
import {
  formaterCentimes,
  formaterDuree,
  formaterMontantLigne,
  formaterMontantListe,
  formaterMontantSimulation,
  formaterTaux,
  libelleType,
  LIBELLES_STATUT,
  SAISIE_SIMULATION_DEFAUT,
  TYPE_PRET_DEFAUT,
  TYPES_PRET,
} from './pret';

describe('formatage des montants', () => {
  it('[FR-007-03] formate le coût total de référence : 10548.72 devient « 10,548.72 EUR »', () => {
    expect(formaterMontantSimulation('10548.72')).toBe('10,548.72 EUR');
    expect(formaterMontantSimulation('293.02')).toBe('293.02 EUR');
    expect(formaterMontantSimulation('548.72')).toBe('548.72 EUR');
  });

  it('[FR-007-08] formate un montant de la liste : « EUR5,000.00 »', () => {
    expect(formaterMontantListe('5000.00')).toBe('EUR5,000.00');
    expect(formaterMontantListe('218.63')).toBe('EUR218.63');
  });

  it('[FR-007-03] formate un montant de ligne sans devise', () => {
    expect(formaterMontantLigne('1263.85')).toBe('1,263.85');
  });

  it.each([
    [0, '0.00'],
    [5, '0.05'],
    [99, '0.99'],
    [100, '1.00'],
    [29302, '293.02'],
    [100000, '1,000.00'],
    [123456789, '1,234,567.89'],
    [-123456, '-1,234.56'],
  ])('formate %i centimes en « %s »', (centimes, attendu) => {
    expect(formaterCentimes(centimes)).toBe(attendu);
  });

  it('refuse un montant qui n’a pas deux décimales', () => {
    expect(() => formaterMontantSimulation('10548.7')).toThrow();
  });
});

describe('formatage des taux et durées', () => {
  it('[FR-007-08] rend le taux à quatre décimales', () => {
    expect(formaterTaux('4.9000')).toBe('4.9000%');
    expect(formaterTaux('4.9')).toBe('4.9000%');
    expect(formaterTaux('30')).toBe('30.0000%');
  });

  it('[FR-007-08] rend la durée en mois', () => {
    expect(formaterDuree(24)).toBe('24 mois');
  });
});

describe('libellés du legacy', () => {
  it('[FR-007-04] propose les cinq types dans l’ordre, Consommation par défaut', () => {
    expect(TYPES_PRET.map(({ libelle }) => libelle)).toEqual(['Immobilier', 'Automobile', 'Consommation', 'Travaux', 'Etudiant']);
    expect(TYPE_PRET_DEFAUT).toBe('CONSOMMATION');
    expect(libelleType('AUTO')).toBe('Automobile');
  });

  it('[FR-007-08] libelle les cinq statuts comme le legacy', () => {
    expect(LIBELLES_STATUT).toEqual({
      EN_ATTENTE: 'En attente d approbation',
      EN_COURS: 'En cours',
      REMBOURSE: 'Rembourse',
      REFUSE: 'Refuse par le service credit',
      DEFAUT: 'En defaut',
    });
  });

  it('[FR-007-02] préremplit le simulateur à 10000, 3.50 et 36', () => {
    expect(SAISIE_SIMULATION_DEFAUT).toEqual({ montant: '10000', tauxAnnuel: '3.50', dureeMois: '36' });
  });
});
