import { describe, expect, it } from 'vitest';

/**
 * [UX-DR1] Couleurs uniquement par les variables de tokens.css : aucune valeur de couleur écrite en dur
 * (hexadécimale, rgb(), rgba(), hsl(), hsla()) dans frontend/src, hors tokens.css (généré) et hors tests.
 */

interface SystemeDeFichiers {
  readdirSync(chemin: URL, options: { recursive: true }): string[];
  readFileSync(chemin: URL, encodage: 'utf-8'): string;
}
const fs = (
  globalThis as unknown as { process: { getBuiltinModule(id: 'node:fs'): SystemeDeFichiers } }
).process.getBuiltinModule('node:fs');

const RACINE = new URL('./', import.meta.url);
const FICHIER_DES_TOKENS = 'adapters/ui/theme/tokens.css';

function lireSourcesStylees(): Record<string, string> {
  const sources: Record<string, string> = {};
  for (const chemin of fs.readdirSync(RACINE, { recursive: true })) {
    const relatif = chemin.split('\\').join('/');
    if (/\.(css|tsx?)$/.test(relatif) && !/\.test\.tsx?$/.test(relatif) && relatif !== FICHIER_DES_TOKENS) {
      sources[`src/${relatif}`] = fs.readFileSync(new URL(relatif, RACINE), 'utf-8');
    }
  }
  return sources;
}

const COULEUR_EN_DUR = /(?<![\w&-])#(?:[0-9a-f]{8}|[0-9a-f]{6}|[0-9a-f]{3,4})(?![\w-])|\b(?:rgba?|hsla?)\s*\(/gi;

/** Une ligne « fichier : valeur » par couleur écrite en dur. */
export function couleursHorsTokens(sources: Record<string, string>): string[] {
  return Object.entries(sources).flatMap(([fichier, source]) =>
    [...source.matchAll(COULEUR_EN_DUR)].map(([valeur]) => `${fichier} : ${valeur}`),
  );
}

const sources = lireSourcesStylees();

describe('[UX-DR1] couleurs hors tokens', () => {
  it('[UX-DR1] lit les CSS et les composants de frontend/src, sans tokens.css ni tests', () => {
    const fichiers = Object.keys(sources);
    expect(fichiers).toContain('src/adapters/ecrans/connexion/EcranConnexion.css');
    expect(fichiers).toContain('src/adapters/ui/theme/base.css');
    expect(fichiers).toContain('src/adapters/ecrans/connexion/EcranConnexion.tsx');
    expect(fichiers).not.toContain(`src/${FICHIER_DES_TOKENS}`);
    expect(fichiers.some((fichier) => /\.test\.tsx?$/.test(fichier))).toBe(false);
  });

  it('[UX-DR1] cas négatif : #ff0000 dans le CSS d’un écran est signalé en nommant le fichier', () => {
    expect(
      couleursHorsTokens({
        'src/adapters/ecrans/connexion/EcranConnexion.css': '.titre {\n  color: #ff0000;\n}\n',
      }),
    ).toEqual(['src/adapters/ecrans/connexion/EcranConnexion.css : #ff0000']);
  });

  it('[UX-DR1] reconnaît chaque forme de couleur et ignore les variables et les ancres', () => {
    expect(couleursHorsTokens({ 'a.css': 'a{color:#FFF;b:#abcd;c:#a1b2c3d4;d:rgb(0 0 0);e:hsla(0,0%,0%,.5)}' })).toEqual([
      'a.css : #FFF',
      'a.css : #abcd',
      'a.css : #a1b2c3d4',
      'a.css : rgb(',
      'a.css : hsla(',
    ]);
    expect(
      couleursHorsTokens({ 'b.tsx': 'color: var(--color-primary); href="#contenu-principal"; &#160; #fade-out' }),
    ).toEqual([]);
  });

  it.each(Object.entries(sources))('[UX-DR1] %s n’écrit aucune couleur en dur', (fichier, source) => {
    expect(couleursHorsTokens({ [fichier]: source })).toEqual([]);
  });
});
