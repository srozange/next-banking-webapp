// Convertit l'export `design.md export --format css-tailwind` (bloc Tailwind v4 `@theme { … }`, lu sur l'entrée
// standard) en variables CSS `:root { … }`, écrites dans le fichier passé en argument. Sortie déterministe ;
// échoue sans rien écrire si aucun bloc `@theme` n'est trouvé.
//
// Seule autre transformation : l'export met une pile de polices entière entre guillemets
// (`--font-body-md: "Inter, Arial, sans-serif";`), ce qui désignerait une seule famille nommée
// « Inter, Arial, sans-serif ». La pile est rendue en liste CSS (`Inter, Arial, sans-serif`).
import { readFileSync, writeFileSync } from 'node:fs';

const sortie = process.argv[2];
if (!sortie) {
  console.error('Usage : … | node scripts/tokens-root.mjs <fichier de sortie>');
  process.exit(2);
}

const entree = readFileSync(0, 'utf-8');
const bloc = /@theme\s*\{([^}]*)\}/.exec(entree);
if (bloc === null) {
  console.error('tokens-root : aucun bloc @theme dans l’entrée (export design.md en échec ?)');
  process.exit(1);
}

const FAMILLES_GENERIQUES = /^(serif|sans-serif|monospace|cursive|fantasy|system-ui|math|emoji|fangsong|ui-[a-z-]+)$/;

function pileDePolices(valeur) {
  return valeur
    .split(',')
    .map((famille) => famille.trim())
    .filter((famille) => famille !== '')
    .map((famille) => (FAMILLES_GENERIQUES.test(famille) || /^[A-Za-z][\w-]*$/.test(famille) ? famille : `"${famille}"`))
    .join(', ');
}

const declarations = bloc[1]
  .split('\n')
  .map((ligne) => ligne.trim())
  .filter((ligne) => ligne !== '')
  .map((ligne) => {
    const police = /^(--font-(?!weight-)[\w-]+):\s*"([^"\\]*)";$/.exec(ligne);
    return police === null ? ligne : `${police[1]}: ${pileDePolices(police[2])};`;
  });

if (declarations.length === 0) {
  console.error('tokens-root : bloc @theme vide');
  process.exit(1);
}

const css = [
  '/* Généré par `npm run tokens` depuis DESIGN.md (design.md export --format css-tailwind, puis scripts/tokens-root.mjs).',
  '   Ne pas modifier à la main : corriger DESIGN.md et relancer `npm run tokens`. */',
  ':root {',
  ...declarations.map((declaration) => `  ${declaration}`),
  '}',
  '',
].join('\n');

writeFileSync(sortie, css, 'utf-8');
