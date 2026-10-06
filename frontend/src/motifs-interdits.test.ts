import { describe, expect, it } from 'vitest';

/**
 * [UX-DR110] Motifs d'interaction interdits, rendus contrôlables : aucun glisser-déposer, défilement infini,
 * action portée par le seul survol ni élément <dialog> dans les composants de src/adapters (hors tests) ;
 * le README du front rappelle les six motifs interdits.
 */

interface SystemeDeFichiers {
  readdirSync(chemin: URL, options: { recursive: true }): string[];
  readFileSync(chemin: URL, encodage: 'utf-8'): string;
}
const fs = (
  globalThis as unknown as { process: { getBuiltinModule(id: 'node:fs'): SystemeDeFichiers } }
).process.getBuiltinModule('node:fs');

const ADAPTATEURS = new URL('./adapters/', import.meta.url);

function lireAdaptateurs(extension: RegExp): Record<string, string> {
  const sources: Record<string, string> = {};
  for (const chemin of fs.readdirSync(ADAPTATEURS, { recursive: true })) {
    const relatif = chemin.split('\\').join('/');
    if (extension.test(relatif) && !/\.test\.tsx?$/.test(relatif)) {
      sources[`src/adapters/${relatif}`] = fs.readFileSync(new URL(relatif, ADAPTATEURS), 'utf-8');
    }
  }
  return sources;
}

const MOTIFS_DES_COMPOSANTS: ReadonlyArray<{ libelle: string; motif: RegExp }> = [
  { libelle: 'draggable', motif: /\bdraggable\b/ },
  { libelle: 'onDrag*', motif: /\bonDrag\w*/ },
  { libelle: 'onDrop', motif: /\bonDrop\b/ },
  { libelle: 'IntersectionObserver', motif: /\bIntersectionObserver\b/ },
  { libelle: 'action au survol', motif: /\bon(?:MouseEnter|MouseOver|PointerEnter|PointerOver)\b/ },
  { libelle: '<dialog', motif: /<dialog\b/ },
];

/** Une règle :hover qui fait apparaître un élément (display, visibility, opacity) porte une action au seul survol. */
const SURVOL_REVELATEUR = /:hover[^{]*\{[^}]*\b(?:display|visibility|opacity)\s*:/;

/** Libellés des motifs interdits trouvés dans un composant. */
export function motifsDuComposant(source: string): string[] {
  return MOTIFS_DES_COMPOSANTS.filter(({ motif }) => motif.test(source)).map(({ libelle }) => libelle);
}

export function revelationAuSurvol(css: string): boolean {
  return SURVOL_REVELATEUR.test(css);
}

const MOTIFS_DU_README = [
  'défilement infini',
  'glisser-déposer',
  'action visible au seul survol',
  "plus d'une modale",
  'plus de deux niveaux de navigation',
  'contenu interactif dans un tooltip',
];

const composants = lireAdaptateurs(/\.tsx$/);
const styles = lireAdaptateurs(/\.css$/);

describe('[UX-DR110] motifs interdits', () => {
  it('[UX-DR110] lit les composants et les styles de src/adapters, sans les tests', () => {
    expect(Object.keys(composants)).toContain('src/adapters/gabarit/EnTete.tsx');
    expect(Object.keys(styles)).toContain('src/adapters/ui/LienNavigation.css');
    expect(Object.keys(composants).some((fichier) => fichier.endsWith('.test.tsx'))).toBe(false);
  });

  it('[UX-DR110] cas négatifs : chaque motif est détecté', () => {
    expect(motifsDuComposant('<li draggable onDragStart={f} onDrop={g}>')).toEqual(['draggable', 'onDrag*', 'onDrop']);
    expect(motifsDuComposant('new IntersectionObserver(charger)')).toEqual(['IntersectionObserver']);
    expect(motifsDuComposant('<div onMouseEnter={ouvrir}>')).toEqual(['action au survol']);
    expect(motifsDuComposant('<dialog open>')).toEqual(['<dialog']);
    expect(motifsDuComposant('<button type="button" onClick={f}>')).toEqual([]);
    expect(revelationAuSurvol('.ligne:hover .actions { display: block; }')).toBe(true);
    expect(revelationAuSurvol('.bouton:hover {\n  background-color: var(--color-primary-hover);\n}')).toBe(false);
  });

  it.each(Object.entries(composants))('[UX-DR110] %s n’utilise aucun motif interdit', (_fichier, source) => {
    expect(motifsDuComposant(source)).toEqual([]);
  });

  it.each(Object.entries(styles))('[UX-DR110] %s ne fait rien apparaître au seul survol', (_fichier, css) => {
    expect(revelationAuSurvol(css)).toBe(false);
  });

  it('[UX-DR110] le README du front rappelle les six motifs interdits', () => {
    const readme = fs.readFileSync(new URL('../README.md', import.meta.url), 'utf-8');
    for (const motif of MOTIFS_DU_README) {
      expect(readme).toContain(motif);
    }
  });
});
