// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { defineConfig } from '@playwright/test';

const withSlash = (u: string) => (u.endsWith('/') ? u : u + '/');

export default defineConfig({
  testDir: '.',
  fullyParallel: false,
  workers: 1, // état partagé : dans chaque rôle, les scénarios qui écrivent passent après les lectures
  reporter: [['list'], ['html', { outputFolder: 'report', open: 'never' }], ['json', { outputFile: 'results.json' }]],
  // URL de base terminée par « / » : les routes générées lui sont relatives (préfixe /app/… conservé).
  use: { baseURL: withSlash(process.env.GM_BASE_URL || "http://localhost:8080/"), screenshot: 'on', trace: 'retain-on-failure' },
  projects: [
    { name: 'connexion', testMatch: /auth\.setup\.ts/ },
    { name: 'rejeu', testMatch: /specs\/.*\.spec\.ts/, dependencies: ['connexion'] },
  ],
});
