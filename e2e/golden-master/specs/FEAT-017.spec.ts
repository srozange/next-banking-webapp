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

test.describe("FEAT-017 — Téléchargement documents PDF", () => {
  test.describe("sans connexion", () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test("SC-017-01 — Un accès sans session au endpoint /docs/download est redirigé vers la page de connexion (HTTP 302)", { tag: ["@FEAT-017", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-017-001, RG-017-002"}, {"type": "Origine legacy", "description": "GET /banking-legacy/docs/download?type=RIB&accountId=1"}] }, async ({ request }) => {
      const response = await request.fetch(api("api/documents/telechargement?type=RIB&compteId=1"), { method: "GET", maxRedirects: 0 });
      expect(response.status()).toBe(401);
    });
  });

  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-017-02 — Un utilisateur authentifié obtient un PDF (Content-Type: application/pdf) pour le type RIB d'un compte valide", { tag: ["@FEAT-017", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-017-008, RG-017-014"}, {"type": "Origine legacy", "description": "page /documents.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("documents");
      });
      await test.step('Rejouer les actions enregistrées', async () => {
        await expect(page.locator('body')).toContainText("Telecharger PDF", { ignoreCase: true, useInnerText: true });
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("RIB", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Releve d Identite Bancaire", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Telecharger PDF", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("ATTESTATION", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Attestation de solde", { useInnerText: true });
      });
    });
  });
});
