// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-016 — Administration — Gestion globale", () => {
  test.describe("rôle ADMIN", () => {
    test.use({ storageState: ".auth/ADMIN.json" });

    test("SC-016-01 — L'ADMIN accède à la page d'administration avec les KPIs (clients, conseillers, prêts) et la liste des clients", { tag: ["@FEAT-016", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-016-003, RG-016-004, RG-016-005, RG-016-006"}, {"type": "Origine legacy", "description": "page /admin.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("administration");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("TOTAL CLIENTS", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("CONSEILLERS", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("PRETS EN ATTENTE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Pipeline Prets", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Gestion des clients", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Jean Dupont", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("user1", { useInnerText: true });
      });
    });
  });

  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-016-02 — Un CLIENT accédant à admin.xhtml est redirigé vers dashboard.xhtml (RBAC - accès refusé sans message d'erreur)", { tag: ["@FEAT-016", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-016-002"}, {"type": "Origine legacy", "description": "page /admin.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("administration");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Tableau de bord", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("MES COMPTES DEMO BANK", { useInnerText: true });
      });
    });
  });
});
