// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-015 — Espace Conseiller — Portefeuille clients", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-015-02 — Un CLIENT accédant à conseiller-workspace.xhtml est redirigé silencieusement vers dashboard.xhtml (RBAC)", { tag: ["@FEAT-015", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-015-002"}, {"type": "Origine legacy", "description": "page /conseiller-workspace.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("espace-conseiller");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Tableau de bord", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("MES COMPTES DEMO BANK", { useInnerText: true });
      });
    });
  });

  test.describe("rôle CONSEILLER", () => {
    test.use({ storageState: ".auth/CONSEILLER.json" });

    test("SC-015-01 — Le conseiller voit son portefeuille avec les KPIs (clients, encours total, prêts en attente)", { tag: ["@FEAT-015", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-015-003, RG-015-004, RG-015-006, RG-015-007"}, {"type": "Origine legacy", "description": "page /conseiller-workspace.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("espace-conseiller");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("MES CLIENTS", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("ENCOURS TOTAL", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("PRETS EN ATTENTE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Jean Dupont", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("@user1", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Marie Lebrun", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("@user2", { useInnerText: true });
      });
    });
  });
});
