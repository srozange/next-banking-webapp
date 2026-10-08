import { describe, expect, it } from 'vitest';

/**
 * Le jeton d'authentification ne vit que dans les adaptateurs. Gardes :
 * - src/domain et src/application ne mentionnent ni `jeton`, ni `Authorization`, ni `Bearer`, ni `nb.jeton`
 *   (type, port, paramètre ou commentaire, casse ignorée) : l'application ne parle que d'une session ;
 * - `adapters/session/fetchAuthentifie.ts` est le seul fichier source à écrire `Authorization`.
 * (Que chaque client de main.tsx reçoive fetchAuthentifie est vérifié par infrastructure/main.test.ts.)
 */

// Lecture par le système de fichiers, pas par import.meta.glob('?raw') (voir architecture-temps.test.ts).
interface SystemeDeFichiers {
  readdirSync(chemin: URL, options: { recursive: true }): string[];
  readFileSync(chemin: URL, encodage: 'utf-8'): string;
}
const fs = (
  globalThis as unknown as { process: { getBuiltinModule(id: 'node:fs'): SystemeDeFichiers } }
).process.getBuiltinModule('node:fs');

/** Sources (hors tests) des couches données, ou de tout src quand aucune n'est donnée, par chemin « ./couche/fichier ». */
function lireSources(...couches: string[]): Record<string, string> {
  const resultat: Record<string, string> = {};
  for (const couche of couches.length > 0 ? couches : ['']) {
    const dossier = new URL(couche === '' ? './' : `./${couche}/`, import.meta.url);
    for (const chemin of fs.readdirSync(dossier, { recursive: true })) {
      const relatif = chemin.split('\\').join('/');
      if (/\.tsx?$/.test(relatif) && !/\.test\.tsx?$/.test(relatif)) {
        resultat[`./${couche === '' ? '' : `${couche}/`}${relatif}`] = fs.readFileSync(new URL(relatif, dossier), 'utf-8');
      }
    }
  }
  return resultat;
}

const MOTIFS_DU_JETON = /jeton|Authorization|Bearer|nb\.jeton/i;
const ECRITURE_AUTHORIZATION = /Authorization/i;
const SEUL_AUTORISE = './adapters/session/fetchAuthentifie.ts';

/** Fichiers dont le texte contient le motif. */
const fichiersContenant = (sources: Record<string, string>, motif: RegExp) =>
  Object.keys(sources).filter((fichier) => motif.test(sources[fichier] as string));

describe('confinement du jeton', () => {
  it('domain et application ne mentionnent jamais le jeton, Authorization ni Bearer', () => {
    const sources = lireSources('domain', 'application');

    expect(Object.keys(sources).length).toBeGreaterThan(20);
    expect(fichiersContenant(sources, MOTIFS_DU_JETON)).toEqual([]);
  });

  it('fetchAuthentifie.ts est le seul fichier source à écrire Authorization', () => {
    const sources = lireSources();

    expect(sources[SEUL_AUTORISE]).toBeDefined();
    expect(fichiersContenant(sources, ECRITURE_AUTHORIZATION)).toEqual([SEUL_AUTORISE]);
  });

  describe('les détecteurs repèrent les signatures fautives', () => {
    it.each([
      ['un paramètre', 'lire(jeton: string): Promise<void>;'],
      ['un type', 'export interface StockageJeton {}'],
      ['une casse différente', 'const JETON = 1;'],
      ['un commentaire', '// le Jeton circule ici'],
      ['la clé de stockage', "localStorage.getItem('nb.jeton')"],
      ['un en-tête', "headers: { Authorization: 'x' }"],
      ['un schéma', 'const entete = `Bearer ${x}`;'],
    ])('%s', (_cas, texte) => {
      expect(fichiersContenant({ './application/fautif.ts': texte }, MOTIFS_DU_JETON)).toEqual(['./application/fautif.ts']);
    });

    it('un autre fichier que fetchAuthentifie qui écrit authorization est repéré', () => {
      const sources = {
        [SEUL_AUTORISE]: "entetes.set('Authorization', valeur)",
        './adapters/http/ClientFautifHttp.ts': "headers: { authorization: 'Bearer x' }",
      };

      expect(fichiersContenant(sources, ECRITURE_AUTHORIZATION)).toEqual([SEUL_AUTORISE, './adapters/http/ClientFautifHttp.ts']);
    });

    it('un texte sans rien de tel passe', () => {
      expect(fichiersContenant({ './application/sain.ts': 'export function lire(session: SessionLocale) {}' }, MOTIFS_DU_JETON)).toEqual([]);
    });
  });
});
