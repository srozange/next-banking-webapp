// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-003 — Effectuer un virement", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-003-01 — Le formulaire de virement charge avec les comptes du client authentifié", { tag: ["@FEAT-003", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-003-001, RG-003-002"}, {"type": "Origine legacy", "description": "page /virement.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("virement");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Effectuer un virement", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Compte source", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Compte destinataire", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Montant", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Motif", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("COURANT", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("EPARGNE", { useInnerText: true });
      });
    });

    test("SC-003-02 — Sélectionner le même compte en source et destination affiche 'Le compte source et destination doivent être différents'", { tag: ["@FEAT-003", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-003-005"}, {"type": "Origine legacy", "description": "page /virement.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("virement");
      });
      await test.step('Rejouer les actions enregistrées', async () => {
        await page.locator("[data-testid='virement-compte-source']").first().selectOption("1");
        await page.locator("[data-testid='virement-compte-destinataire']").first().selectOption("1");
        await page.locator("[data-testid='virement-montant']").first().fill("100");
        await page.locator("[data-testid='virement-motif']").first().fill("[retro] test comptes identiques");
        await page.locator("[data-testid='virement-executer']").first().click();
        await expect(page.locator('body')).toContainText("compte source et destination", { ignoreCase: true, useInnerText: true });
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Le compte source et destination doivent être différents", { useInnerText: true });
      });
    });

    test("SC-003-04 — Un virement valide (100 EUR, COURANT→EPARGNE) est exécuté et redirige vers le tableau de bord avec une notification CONFIRMATION", { tag: ["@FEAT-003", "@golden-master", "@ecriture"], annotation: [{"type": "Règles couvertes", "description": "RG-003-010, RG-003-011, RG-003-013, RG-003-015, RG-003-017, RG-003-019"}, {"type": "Origine legacy", "description": "page /virement.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("virement");
      });
      await test.step('Rejouer les actions enregistrées', async () => {
        await page.locator("[data-testid='virement-compte-source']").first().selectOption("1");
        await page.locator("[data-testid='virement-compte-destinataire']").first().selectOption("2");
        await page.locator("[data-testid='virement-montant']").first().fill("100");
        await page.locator("[data-testid='virement-motif']").first().fill("[retro] virement doré 100EUR");
        await page.locator("[data-testid='virement-executer']").first().click();
        await expect(page.locator('body')).toContainText("CONFIRMATION", { ignoreCase: true, useInnerText: true });
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Tableau de bord", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("CONFIRMATION", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("[retro] virement doré 100EUR", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("DEBIT", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("COURANT", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("EPARGNE", { useInnerText: true });
      });
    });
  });

  test.skip("SC-003-03 — Un montant supérieur au solde affiche 'Erreur lors du virement : java.lang.IllegalStateException: Solde insuffisant' (dette-legacy : message technique exposé)", { tag: ["@FEAT-003", "@non-rejoue"], annotation: {"type": "Non rejoué", "description": "BUG-011 — corrigé dans le forward, validé par les tests de la nouvelle application"} }, async () => {});
});
