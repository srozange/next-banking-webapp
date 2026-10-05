// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-004 — Virements programmés", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-004-01 — La page virements programmés charge avec le formulaire de création et la liste des virements du client", { tag: ["@FEAT-004", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-004-002, RG-004-003"}, {"type": "Origine legacy", "description": "page /virement-programme.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("virements-programmes");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("NOUVEAU VIREMENT PROGRAMME", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("COMPTE DEBITEUR", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("MONTANT (EUR)", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("FREQUENCE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Hebdomadaire", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Mensuel", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("IBAN / N COMPTE BENEFICIAIRE", { useInnerText: true });
      });
    });

    test("SC-004-03 — Soumettre le formulaire vide affiche 'Veuillez selectionner un compte source'", { tag: ["@FEAT-004", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-004-010"}, {"type": "Origine legacy", "description": "page /virement-programme.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("virements-programmes");
      });
      await test.step('Rejouer les actions enregistrées', async () => {
        await page.locator("[data-testid='vp-creer']").first().click();
        await expect(page.locator('body')).toContainText("Veuillez selectionner un compte source", { ignoreCase: true, useInnerText: true });
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Veuillez selectionner un compte source", { useInnerText: true });
      });
    });

    test("SC-004-02 — Créer un virement programmé mensuel avec données valides affiche 'Virement programme cree avec succes'", { tag: ["@FEAT-004", "@golden-master", "@ecriture"], annotation: [{"type": "Règles couvertes", "description": "RG-004-023, RG-004-024"}, {"type": "Origine legacy", "description": "page /virement-programme.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("virements-programmes");
      });
      await test.step('Rejouer les actions enregistrées', async () => {
        await page.locator("[data-testid='vp-compte-debiteur']").first().selectOption("1");
        await page.locator("[data-testid='vp-montant']").first().fill("50");
        await page.locator("[data-testid='vp-frequence']").first().selectOption("MENSUEL");
        await page.locator("[data-testid='vp-numero-beneficiaire']").first().fill("FR76-0001-0002");
        await page.locator("[data-testid='vp-libelle-beneficiaire']").first().fill("[retro] test virprog");
        await page.locator("[data-testid='vp-date-premiere-execution']").first().fill("2027-01-01");
        await page.locator("[data-testid='vp-motif']").first().fill("[retro] virement programm gm");
        await page.locator("[data-testid='vp-creer']").first().click();
        await expect(page.locator('body')).toContainText("cree avec succes", { ignoreCase: true, useInnerText: true });
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Virement programme cree avec succes", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("MES VIREMENTS PROGRAMMES", { useInnerText: true });
      });
    });
  });
});
