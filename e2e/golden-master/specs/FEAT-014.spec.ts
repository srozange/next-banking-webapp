// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);
const withSlash = (u: string) => (u.endsWith('/') ? u : u + '/');
const apiBase = withSlash(process.env.GM_API_BASE_URL || process.env.GM_BASE_URL || "http://localhost:8080/");
const api = (path: string) => new URL(path, apiBase).toString();

test.describe("FEAT-014 — Callback OAuth Powens", () => {
  test.describe("sans connexion", () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test("SC-014-01 — La page de callback Powens est accessible sans authentification et redirige vers comptes-tiers.xhtml quand les paramètres sont absents", { tag: ["@FEAT-014", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-014-001"}, {"type": "Origine legacy", "description": "GET /banking-legacy/powens-callback.xhtml"}] }, async ({ request }) => {
      const response = await request.fetch(api("api/open-banking-conseiller/powens/retour"), { method: "GET", maxRedirects: 0 });
      expect(response.status()).toBe(302);
    });
  });
});
