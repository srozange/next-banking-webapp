// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-005 — Relevé de compte", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-005-01 — Le relevé de compte charge avec les transactions du premier compte, ordonnées par date décroissante", { tag: ["@FEAT-005", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-005-003, RG-005-007, RG-005-012, RG-005-013, RG-005-014"}, {"type": "Origine legacy", "description": "page /releve.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("releve");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Relevé de compte", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("COURANT", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("FR76-0001-0001", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("DATE DESCRIPTION TYPE MONTANT SOLDE APRÈS", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("DEBIT", { useInnerText: true });
      });
    });
  });
});
