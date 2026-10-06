import { describe, expect, it } from 'vitest';
import { eurosDepuisChaine, formaterEuros, ligneReleve, PREFIXE_EN_ATTENTE, type OperationBrute } from './releve';

const operation = (partielle: Partial<OperationBrute> = {}): OperationBrute => ({
  date: '2026-11-03T08:00:00Z',
  description: 'Virement vers FR76-0001-0002',
  type: 'DEBIT',
  montant: '30.00',
  soldeApres: '2100.50',
  ...partielle,
});

describe('[FR-005-04] formaterEuros', () => {
  it.each([
    [0, '0,00 €'],
    [5, '0,05 €'],
    [10050, '100,50 €'],
    [213000, '2 130,00 €'],
    [123456789, '1 234 567,89 €'],
    [-250, '-2,50 €'],
  ])('%i centimes donnent %s', (centimes, attendu) => {
    expect(formaterEuros(centimes)).toBe(attendu);
  });

  it('formate un montant JSON à deux décimales', () => {
    expect(eurosDepuisChaine('15200.00')).toBe('15 200,00 €');
  });
});

describe('ligneReleve', () => {
  it('[FR-005-06] préfixe un DEBIT du tiret ASCII et un CREDIT de « + », le solde après sans signe', () => {
    const debit = ligneReleve(operation());
    const credit = ligneReleve(operation({ type: 'CREDIT', montant: '1200.00' }));

    expect(debit.montant).toBe('-30,00 €');
    expect(debit.montant.charCodeAt(0)).toBe(45);
    expect(debit.soldeApres).toBe('2 100,50 €');
    expect(credit.montant).toBe('+1 200,00 €');
    expect(credit.type).toBe('CREDIT');
    expect(credit.date).toBe('2026-11-03T08:00:00Z');
  });

  it('[FR-005-07] retire les 13 caractères de « [EN ATTENTE] » et marque la ligne en attente', () => {
    const ligne = ligneReleve(operation({ description: '[EN ATTENTE] Virement vers X' }));

    expect(PREFIXE_EN_ATTENTE).toHaveLength(13);
    expect(ligne.enAttente).toBe(true);
    expect(ligne.libelle).toBe('Virement vers X');
  });

  it.each(['Virement vers X', '[EN ATTENTE]sans espace', '  [EN ATTENTE] décalé', '[en attente] casse'])(
    '[FR-005-07] « %s » n’est pas une opération en attente',
    (description) => {
      const ligne = ligneReleve(operation({ description }));

      expect(ligne.enAttente).toBe(false);
      expect(ligne.libelle).toBe(description);
    },
  );
});
