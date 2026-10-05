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

test.describe("FEAT-001 — Connexion", () => {
  test.describe("sans connexion", () => {
    test.use({ storageState: { cookies: [], origins: [] } });

    test("SC-001-01 — La page de connexion est accessible sans authentification (HTTP 200)", { tag: ["@FEAT-001", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-001-002"}, {"type": "Origine legacy", "description": "GET /banking-legacy/login.xhtml"}] }, async ({ request }) => {
      const response = await request.fetch(api("login"), { method: "GET" });
      expect(response.status()).toBe(200);
    });

    test("SC-001-02 — Tout accès non authentifié à une page protégée redirige vers login.xhtml (HTTP 302)", { tag: ["@FEAT-001", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-001-001"}, {"type": "Origine legacy", "description": "GET /banking-legacy/dashboard.xhtml"}] }, async ({ request }) => {
      const response = await request.fetch(api("api/coeur-bancaire/tableau-de-bord"), { method: "GET", maxRedirects: 0 });
      expect(response.status()).toBe(401);
    });

    test("SC-001-03 — Des identifiants incorrects maintiennent l'utilisateur sur login.xhtml avec le message 'Identifiants incorrects'", { tag: ["@FEAT-001", "@golden-master", "@ecriture"], annotation: [{"type": "Règles couvertes", "description": "RG-001-007"}, {"type": "Origine legacy", "description": "POST /banking-legacy/login.xhtml"}] }, async ({ request }) => {
      const response = await request.fetch(api("api/identite-clients/connexion"), { method: "POST", data: {"identifiant": "retro-compte-inexistant", "motDePasse": "retro-mauvais-mot-de-passe"}, maxRedirects: 0 });
      expect(response.status()).toBe(401);
      expect(await response.json()).toMatchObject({"message": "Identifiants incorrects"});
    });
  });

  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-001-04 — Un CLIENT authentifié accède au tableau de bord (session valide — RG-001-003 et RG-001-006 testés via SC-002-01)", { tag: ["@FEAT-001", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-001-003, RG-001-006"}, {"type": "Origine legacy", "description": "page /dashboard.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("tableau-de-bord");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Tableau de bord", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Bonjour, Jean Dupont", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("MES COMPTES DEMO BANK", { useInnerText: true });
      });
    });

    test("SC-001-05 — La déconnexion invalide la session et redirige vers login.xhtml", { tag: ["@FEAT-001", "@golden-master", "@ecriture"], annotation: [{"type": "Règles couvertes", "description": "RG-001-010"}, {"type": "Origine legacy", "description": "page /dashboard.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("tableau-de-bord");
      });
      await test.step('Rejouer les actions enregistrées', async () => {
        await page.locator("[data-testid='nav-deconnexion']").first().click();
        await expect(page.locator('body')).toContainText("Identifiant", { ignoreCase: true, useInnerText: true });
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Identifiant", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Mot de passe", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Espace client", { useInnerText: true });
      });
    });
  });
});
