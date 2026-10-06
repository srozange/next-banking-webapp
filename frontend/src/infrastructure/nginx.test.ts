import { describe, expect, it } from 'vitest';

/** [AR-54] Le conteneur web pose une CSP qui limite scripts, styles et polices à l'origine de l'application. */

interface SystemeDeFichiers {
  readFileSync(chemin: URL, encodage: 'utf-8'): string;
}
const fs = (
  globalThis as unknown as { process: { getBuiltinModule(id: 'node:fs'): SystemeDeFichiers } }
).process.getBuiltinModule('node:fs');

const configuration = fs.readFileSync(new URL('../../nginx.conf', import.meta.url), 'utf-8');

function directivesCsp(): Map<string, string> {
  const entete = /^\s*add_header\s+Content-Security-Policy\s+"([^"]+)"\s+always;/m.exec(configuration);
  expect(entete, 'en-tête Content-Security-Policy (add_header … always) absent de nginx.conf').not.toBeNull();
  return new Map(
    (entete?.[1] ?? '')
      .split(';')
      .map((directive) => directive.trim())
      .filter((directive) => directive !== '')
      .map((directive) => {
        const [nom, ...valeurs] = directive.split(/\s+/);
        return [nom as string, valeurs.join(' ')];
      }),
  );
}

describe('nginx : Content-Security-Policy', () => {
  it('pose la CSP au niveau server, sur toutes les réponses', () => {
    const csp = directivesCsp();
    const indexEntete = configuration.indexOf('add_header Content-Security-Policy');
    expect(indexEntete).toBeLessThan(configuration.search(/^\s*location\s/m));
    expect(csp.size).toBeGreaterThan(0);
  });

  it('aucun bloc location ne pose son propre add_header, qui ferait perdre la CSP héritée du server', () => {
    const blocsLocation = [...configuration.matchAll(/^\s*location\b([^{]*)\{([^}]*)\}/gm)];
    expect(blocsLocation.length).toBeGreaterThan(0);
    for (const [, motif, corps] of blocsLocation) {
      expect(corps, `location${motif}`).not.toMatch(/\badd_header\b/);
    }
  });

  it('limite scripts, styles, polices et connexions à l’origine', () => {
    const csp = directivesCsp();
    for (const directive of ['default-src', 'script-src', 'style-src', 'font-src', 'connect-src']) {
      expect(csp.get(directive), directive).toBe("'self'");
    }
    expect(csp.get('img-src')).toBe("'self' data:");
    expect(csp.get('object-src')).toBe("'none'");
    expect(csp.get('base-uri')).toBe("'self'");
    expect(csp.get('form-action')).toBe("'self'");
    expect(csp.get('frame-ancestors')).toBe("'none'");
  });

  it('n’autorise ni unsafe-inline, ni unsafe-eval, ni Google Fonts', () => {
    expect(configuration).not.toMatch(/unsafe-(inline|eval)/);
    expect(configuration).not.toMatch(/fonts\.(googleapis|gstatic)\.com/);
  });
});
