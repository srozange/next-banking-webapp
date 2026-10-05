// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-007 — Demande et suivi de prêts", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-007-01 — La page de prêts charge avec le simulateur, le formulaire de demande et la liste des emprunts en cours", { tag: ["@FEAT-007", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-007-001, RG-007-002"}, {"type": "Origine legacy", "description": "page /pret.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("prets");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("SIMULATEUR DE PRET", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("DEMANDE DE PRET", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("MES EMPRUNTS", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("TYPE DE PRET", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Immobilier", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Automobile", { useInnerText: true });
      });
    });

    test("SC-007-02 — Le simulateur calcule mensualite, cout total et interets pour 10000 EUR à 3,5% sur 36 mois", { tag: ["@FEAT-007", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-007-003, RG-007-004"}, {"type": "Origine legacy", "description": "page /pret.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("prets");
      });
      await test.step('Rejouer les actions enregistrées', async () => {
        await page.locator("[data-testid='pret-sim-montant']").first().fill("10000");
        await page.locator("[data-testid='pret-sim-taux']").first().fill("3.5");
        await page.locator("[data-testid='pret-sim-duree']").first().fill("36");
        await page.locator("[data-testid='pret-sim-calculer']").first().click();
        await expect(page.locator('body')).toContainText("Mensualite", { ignoreCase: true, useInnerText: true });
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("MENSUALITE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("293.02 EUR", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("COUT TOTAL", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("10,548.72 EUR", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("TOTAL INTERETS", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("548.72 EUR", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("echeancier complet (36 echeances)", { useInnerText: true });
      });
    });
  });
});
