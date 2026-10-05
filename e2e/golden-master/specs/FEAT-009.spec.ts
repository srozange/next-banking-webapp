// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-009 — Bourse et watchlist", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-009-01 — La page bourse charge la watchlist et affiche les données de marché AlphaVantage pour le premier symbole", { tag: ["@FEAT-009", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-009-001"}, {"type": "Origine legacy", "description": "page /bourse.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("bourse");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Valeur", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("BNP.PA", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("AI.PA", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("VARIATION SUR LA PÉRIODE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("PERFORMANCE %", { useInnerText: true });
      });
    });
  });
});
