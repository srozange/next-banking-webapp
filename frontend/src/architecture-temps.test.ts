import { describe, expect, it } from 'vitest';

/**
 * Le temps métier ne se lit jamais dans le navigateur depuis src/domain et src/application :
 * il arrive par un port. Interdits : Date.now(), new Date() sans argument, Temporal.Now.
 */

// Lecture par le système de fichiers, pas par import.meta.glob('?raw') : un module « ?raw » serait compté
// par la couverture v8 comme le fichier source lui-même, chargé et vide, et masquerait un fichier non testé.
interface SystemeDeFichiers {
  readdirSync(chemin: URL, options: { recursive: true }): string[];
  readFileSync(chemin: URL, encodage: 'utf-8'): string;
}
const fs = (
  globalThis as unknown as { process: { getBuiltinModule(id: 'node:fs'): SystemeDeFichiers } }
).process.getBuiltinModule('node:fs');

function lireSources(...couches: string[]): Record<string, string> {
  const resultat: Record<string, string> = {};
  for (const couche of couches) {
    const dossier = new URL(`./${couche}/`, import.meta.url);
    for (const chemin of fs.readdirSync(dossier, { recursive: true })) {
      const relatif = chemin.split('\\').join('/');
      if (/\.tsx?$/.test(relatif) && !/\.test\.tsx?$/.test(relatif)) {
        resultat[`./${couche}/${relatif}`] = fs.readFileSync(new URL(relatif, dossier), 'utf-8');
      }
    }
  }
  return resultat;
}

const sources = lireSources('domain', 'application');

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
