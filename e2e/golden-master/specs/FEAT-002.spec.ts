// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-002 — Tableau de bord", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-002-01 — Le tableau de bord CLIENT charge avec les comptes, le solde total et les dernières opérations", { tag: ["@FEAT-002", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-002-003, RG-002-005, RG-002-006, RG-002-008, RG-002-009, RG-002-010"}, {"type": "Origine legacy", "description": "page /dashboard.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("tableau-de-bord");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Tableau de bord", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("MES COMPTES DEMO BANK", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("SOLDE TOTAL DEMO BANK", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("ACTIVITE RECENTE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Dernieres operations", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("DATE DESCRIPTION TYPE MONTANT SOLDE APRES", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("DEBIT", { useInnerText: true });
      });
    });

    test("SC-002-02 — Le centre de notifications affiche les notifications de l'utilisateur avec un badge de décompte", { tag: ["@FEAT-002", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-002-011, RG-002-013"}, {"type": "Origine legacy", "description": "page /dashboard.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("tableau-de-bord");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Centre de notifications", { useInnerText: true });
      });
    });
  });
});
