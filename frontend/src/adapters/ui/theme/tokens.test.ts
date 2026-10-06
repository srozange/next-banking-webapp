import { afterAll, describe, expect, it } from 'vitest';

/**
 * scripts/tokens-root.mjs : conversion `@theme` → `:root` vérifiée sur des entrées témoins, et tokens.css
 * versionné identique, octet pour octet, à une génération fraîche depuis DESIGN.md (même chaîne que `npm run tokens`).
 */

interface ResultatProcessus {
  status: number | null;
  stdout: string;
  stderr: string;
}
interface Noeud {
  execPath: string;
  getBuiltinModule(id: 'node:child_process'): {
    spawnSync(commande: string, args: string[], options: { input?: string; encoding: 'utf-8' }): ResultatProcessus;
  };
  getBuiltinModule(id: 'node:fs'): {
    mkdtempSync(prefixe: string): string;
    readFileSync(chemin: string, encodage: 'utf-8'): string;
    existsSync(chemin: string): boolean;
    rmSync(chemin: string, options: { recursive: true; force: true }): void;
  };
  getBuiltinModule(id: 'node:os'): { tmpdir(): string };
}
const noeud = (globalThis as unknown as { process: Noeud }).process;
const { spawnSync } = noeud.getBuiltinModule('node:child_process');
const fs = noeud.getBuiltinModule('node:fs');
const { tmpdir } = noeud.getBuiltinModule('node:os');

const chemin = (relatif: string) => decodeURIComponent(new URL(relatif, import.meta.url).pathname);
const SCRIPT = chemin('../../../../scripts/tokens-root.mjs');
const CLI_DESIGN_MD = chemin('../../../../node_modules/@google/design.md/dist/index.js');
const DESIGN_MD = chemin(
  '../../../../../_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/DESIGN.md',
);
const TOKENS_CSS = chemin('./tokens.css');

const dossier = fs.mkdtempSync(`${tmpdir()}/tokens-root-`);
let numero = 0;

/** Lance le script sur l'entrée donnée ; rend son code de sortie et le fichier écrit (null s'il n'existe pas). */
function convertir(entree: string): { code: number | null; sortie: string | null } {
  const fichier = `${dossier}/sortie-${(numero += 1)}.css`;
  const { status } = spawnSync(noeud.execPath, [SCRIPT, fichier], { input: entree, encoding: 'utf-8' });
  return { code: status, sortie: fs.existsSync(fichier) ? fs.readFileSync(fichier, 'utf-8') : null };
}

afterAll(() => {
  fs.rmSync(dossier, { recursive: true, force: true });
});

describe('tokens-root.mjs', () => {
  it('renomme @theme en :root et rend les piles de polices en liste CSS, sans toucher aux graisses', () => {
    const { code, sortie } = convertir(
      [
        '@theme {',
        '  --color-primary: #a100ff;',
        '  --font-body-md: "Inter, Arial, sans-serif";',
        '  --font-titre: "Open Sans, serif";',
        '  --font-weight-body-md: 400;',
        '  --spacing-md: 16px;',
        '}',
        '',
      ].join('\n'),
    );

    expect(code).toBe(0);
    expect(sortie).not.toContain('@theme');
    expect(sortie).toMatch(/\n:root \{\n/);
    expect(sortie).toContain('  --color-primary: #a100ff;\n');
    expect(sortie).toContain('  --font-body-md: Inter, Arial, sans-serif;\n');
    expect(sortie).toContain('  --font-titre: "Open Sans", serif;\n');
    expect(sortie).toContain('  --font-weight-body-md: 400;\n');
    expect(sortie).toContain('  --spacing-md: 16px;\n}\n');
  });

  it('échoue sans rien écrire si aucun bloc @theme n’est trouvé', () => {
    expect(convertir('')).toEqual({ code: 1, sortie: null });
    expect(convertir(':root { --color-primary: #a100ff; }')).toEqual({ code: 1, sortie: null });
  });

  it('échoue sans rien écrire si le bloc @theme est vide', () => {
    expect(convertir('@theme {\n}\n')).toEqual({ code: 1, sortie: null });
  });

  it('tokens.css versionné est identique à une génération fraîche depuis DESIGN.md', () => {
    const exportCss = spawnSync(noeud.execPath, [CLI_DESIGN_MD, 'export', DESIGN_MD, '--format', 'css-tailwind'], {
      encoding: 'utf-8',
    });
    expect(exportCss.status, exportCss.stderr).toBe(0);

    const { code, sortie } = convertir(exportCss.stdout);

    expect(code).toBe(0);
    expect(sortie).toBe(fs.readFileSync(TOKENS_CSS, 'utf-8'));
  });
});
