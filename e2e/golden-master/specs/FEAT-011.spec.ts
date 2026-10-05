// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-011 — Catalogue produits bancaires", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-011-01 — La page produits affiche le catalogue avec produits d'épargne, crédit et investissement", { tag: ["@FEAT-011", "@golden-master"], annotation: [{"type": "Origine legacy", "description": "page /produits.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("produits");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("CATALOGUE DES PRODUITS", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("PRODUITS D EPARGNE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("CEL - Compte Epargne Logement", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Livret A", { useInnerText: true });
      });
    });
  });
});
