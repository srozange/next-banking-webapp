import { describe, expect, it } from 'vitest';
import { champsVides, comptesIdentiques, formaterDate, formaterHeure, motifRenseigne, type SaisieVirement } from './virement';

const saisie = (partielle: Partial<SaisieVirement> = {}): SaisieVirement => ({
  compteSource: '1',
  compteDestinataire: '2',
  montant: '100',
  motif: '',
  ...partielle,
});

describe('[FR-003-02] champsVides', () => {
  it('ne signale rien quand tout est saisi, le motif étant facultatif', () => {
    expect(champsVides(saisie())).toEqual([]);
  });

  it('signale chaque champ obligatoire vide, dans l’ordre de l’écran', () => {
    expect(champsVides(saisie({ compteSource: '', compteDestinataire: '', montant: '  ' }))).toEqual([
      'compteSource',
      'compteDestinataire',
      'montant',
    ]);
    expect(champsVides(saisie({ compteDestinataire: '' }))).toEqual(['compteDestinataire']);
  });
});

describe('[FR-003-03] comptesIdentiques', () => {
  it('compare les deux comptes', () => {
    expect(comptesIdentiques(saisie({ compteDestinataire: '1' }))).toBe(true);
    expect(comptesIdentiques(saisie())).toBe(false);
  });
});

describe('[FR-027-04][FR-027-16] formats de date et d’heure en Europe/Paris', () => {
  it('formate la date dd/MM/yyyy et l’heure HH:mm de l’heure butoir', () => {
    expect(formaterDate('2026-11-06T09:15:00Z')).toBe('06/11/2026');
    expect(formaterHeure('2026-11-06T09:15:00Z')).toBe('10:15');
  });

  it('suit l’heure d’été : le 6 juillet à 08:05 UTC il est 10:05 à Paris, et minuit passe à la date suivante', () => {
    expect(formaterHeure('2026-07-06T08:05:00Z')).toBe('10:05');
    expect(formaterDate('2026-12-31T23:30:00Z')).toBe('01/01/2027');
    expect(formaterHeure('2026-12-31T23:30:00Z')).toBe('00:30');
  });
});

describe('[FR-016-19] motifRenseigne', () => {
  it('refuse un motif vide ou fait d’espaces', () => {
    expect(motifRenseigne('')).toBe(false);
    expect(motifRenseigne('   ')).toBe(false);
    expect(motifRenseigne(' vérifié ')).toBe(true);
  });
});
