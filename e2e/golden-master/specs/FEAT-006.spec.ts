// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-006 — Budget et catégories de dépenses", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-006-01 — La page budget charge avec la répartition des dépenses par catégorie pour la période courante", { tag: ["@FEAT-006", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-006-001, RG-006-003"}, {"type": "Origine legacy", "description": "page /budget.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("budget");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("TOTAL DEPENSES", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("TOTAL REVENUS", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("REPARTITION PAR CATEGORIE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("PLAFONDS BUDGETAIRES", { useInnerText: true });
      });
    });
  });
});
