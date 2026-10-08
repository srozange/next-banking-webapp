import { describe, expect, it } from 'vitest';
import { ligneBeneficiaire, type BeneficiaireBrut } from './beneficiaires';

const brut: BeneficiaireBrut = { id: 4, libelle: 'Loyer', titulaire: 'Marie', iban: 'FR35***9010', bic: 'AGRIFRPP', banque: 'Banque', zoneSepa: true };

describe('ligneBeneficiaire', () => {
  it('[FR-013-03] conserve les valeurs renseignées', () => {
    expect(ligneBeneficiaire(brut)).toEqual(brut);
  });

  it.each([null, '', '   '])('[FR-013-03] rend nul un BIC ou une banque absent (%j)', (vide) => {
    expect(ligneBeneficiaire({ ...brut, bic: vide, banque: vide })).toEqual({ ...brut, bic: null, banque: null });
  });
});
