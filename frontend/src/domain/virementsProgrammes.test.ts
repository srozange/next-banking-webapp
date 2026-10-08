import { describe, expect, it } from 'vitest';
import {
  CHAMP_DU_REFUS,
  dateAffichee,
  FORMULAIRE_VIREMENT_PROGRAMME_VIDE,
  FREQUENCES,
  refusDeSaisie,
  type FormulaireVirementProgramme,
} from './virementsProgrammes';

const VALIDE: FormulaireVirementProgramme = {
  compteSourceId: '1',
  montant: '50',
  frequence: 'MENSUEL',
  numeroBeneficiaire: 'FR76-0001-0002',
  libelleBeneficiaire: '[retro] test virprog',
  datePremiereExecution: '2027-01-01',
  motif: '',
};

describe('virements programmés : règles du domaine', () => {
  it('[FR-004-08] propose six fréquences dans l’ordre du legacy, MENSUEL présélectionné', () => {
    expect(FREQUENCES).toEqual(['HEBDOMADAIRE', 'MENSUEL', 'BIMESTRIEL', 'TRIMESTRIEL', 'SEMESTRIEL', 'ANNUEL']);
    expect(FORMULAIRE_VIREMENT_PROGRAMME_VIDE.frequence).toBe('MENSUEL');
  });

  it('[FR-004-07] accepte la saisie nominale, un montant à virgule et une date passée', () => {
    expect(refusDeSaisie(VALIDE)).toBeNull();
    expect(refusDeSaisie({ ...VALIDE, montant: '12,5' })).toBeNull();
    expect(refusDeSaisie({ ...VALIDE, datePremiereExecution: '2020-01-01' })).toBeNull();
  });

  it('[FR-004-04] vérifie les règles dans l’ordre du legacy', () => {
    const tout = { ...FORMULAIRE_VIREMENT_PROGRAMME_VIDE, numeroBeneficiaire: ' ', libelleBeneficiaire: ' ' };
    expect(refusDeSaisie(tout)).toBe('compteSourceObligatoire');
    expect(refusDeSaisie({ ...tout, compteSourceId: '1' })).toBe('numeroObligatoire');
    expect(refusDeSaisie({ ...tout, compteSourceId: '1', numeroBeneficiaire: 'n' })).toBe('libelleObligatoire');
    expect(refusDeSaisie({ ...VALIDE, montant: '' })).toBe('montantInvalide');
    expect(refusDeSaisie({ ...VALIDE, montant: '', datePremiereExecution: '' })).toBe('montantInvalide');
    expect(refusDeSaisie({ ...VALIDE, datePremiereExecution: '  ' })).toBe('dateObligatoire');
    expect(refusDeSaisie({ ...VALIDE, datePremiereExecution: '01/01/2027' })).toBe('dateInvalide');
  });

  it.each(['', '0', '0,00', '-5', 'abc', '1.234'])('[FR-004-05] le montant « %s » est invalide', (montant) => {
    expect(refusDeSaisie({ ...VALIDE, montant })).toBe('montantInvalide');
  });

  it('[FR-004-04] chaque refus désigne son champ', () => {
    expect(CHAMP_DU_REFUS).toEqual({
      compteSourceObligatoire: 'compteSourceId',
      numeroObligatoire: 'numeroBeneficiaire',
      libelleObligatoire: 'libelleBeneficiaire',
      montantInvalide: 'montant',
      dateObligatoire: 'datePremiereExecution',
      dateInvalide: 'datePremiereExecution',
    });
  });

  it('[FR-004-03] affiche la date en dd/MM/yyyy', () => {
    expect(dateAffichee('2027-01-31')).toBe('31/01/2027');
  });
});
