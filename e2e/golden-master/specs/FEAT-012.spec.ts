// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-012 — Comptes Tiers — Open Banking Powens", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-012-01 — La page comptes tiers charge l'interface de connexion Open Banking avec la liste des banques partenaires", { tag: ["@FEAT-012", "@golden-master"], annotation: [{"type": "Origine legacy", "description": "page /comptes-tiers.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("comptes-tiers");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Agregez vos comptes bancaires", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("BNP Paribas", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Credit Agricole", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("OAuth2 PSD2", { useInnerText: true });
      });
    });
  });
});
