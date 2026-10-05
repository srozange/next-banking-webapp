import { describe, expect, it } from 'vitest';

/**
 * Le temps métier ne se lit jamais dans le navigateur depuis src/domain et src/application :
 * il arrive par un port. Interdits : Date.now(), new Date() sans argument, Temporal.Now.
 */
const sources = import.meta.glob<string>(
  [
    './domain/**/*.{ts,tsx}',
    './application/**/*.{ts,tsx}',
    '!./**/*.test.{ts,tsx}',
  ],
  { query: '?raw', import: 'default', eager: true },
);

const LECTURES_INTERDITES: ReadonlyArray<{ libelle: string; motif: RegExp }> = [
  { libelle: 'Date.now()', motif: /\bDate\s*\.\s*now\s*\(/ },
  { libelle: 'new Date() sans argument', motif: /\bnew\s+Date\s*\(\s*\)/ },
  { libelle: 'new Date sans parenthèses', motif: /\bnew\s+Date\b(?!\s*\()/ },
  { libelle: 'Temporal.Now', motif: /\bTemporal\s*\.\s*Now\b/ },
];

function lecturesDuTemps(source: string): string[] {
  return LECTURES_INTERDITES.filter(({ motif }) => motif.test(source)).map(({ libelle }) => libelle);
}

describe('temps métier', () => {
  it('trouve les sources de src/domain et src/application', () => {
    const fichiers = Object.keys(sources);
    expect(fichiers.some((f) => f.startsWith('./domain/'))).toBe(true);
    expect(fichiers.some((f) => f.startsWith('./application/'))).toBe(true);
    expect(fichiers.some((f) => /\.test\.tsx?$/.test(f))).toBe(false);
  });

  it('détecte chaque lecture interdite et admet une date construite', () => {
    expect(lecturesDuTemps('const t = Date.now();')).toEqual(['Date.now()']);
    expect(lecturesDuTemps('const d = new Date();')).toEqual(['new Date() sans argument']);
    expect(lecturesDuTemps('const d = new Date;')).toEqual(['new Date sans parenthèses']);
    expect(lecturesDuTemps('const j = Temporal.Now.plainDateISO();')).toEqual(['Temporal.Now']);
    expect(lecturesDuTemps('const d = new Date("2026-10-05");')).toEqual([]);
  });

  it.each(Object.entries(sources))('%s ne lit pas l’heure du navigateur', (_fichier, source) => {
    expect(lecturesDuTemps(source)).toEqual([]);
  });
});
