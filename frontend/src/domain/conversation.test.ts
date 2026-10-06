import { describe, expect, it } from 'vitest';
import { prenomDe, saisieVide } from './conversation';

describe('prenomDe', () => {
  it('[FR-010-02] rend le premier mot du nom complet', () => {
    expect(prenomDe('Jean Dupont')).toBe('Jean');
    expect(prenomDe('  Marie   Claire Martin ')).toBe('Marie');
    expect(prenomDe('Cher')).toBe('Cher');
  });

  it('[FR-010-02] ne rend rien sans nom exploitable', () => {
    expect(prenomDe(null)).toBeNull();
    expect(prenomDe('')).toBeNull();
    expect(prenomDe('   ')).toBeNull();
  });
});

describe('saisieVide', () => {
  it('[FR-010-05] une saisie vide ou faite de blancs est vide', () => {
    expect(saisieVide('')).toBe(true);
    expect(saisieVide('  \n\t ')).toBe(true);
  });

  it('[FR-010-05] une saisie avec du texte n’est pas vide', () => {
    expect(saisieVide('Bonjour')).toBe(false);
    expect(saisieVide('  a ')).toBe(false);
  });
});
