import { describe, expect, it } from 'vitest';
import { champsVides, comptesIdentiques, type SaisieVirement } from './virement';

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
