// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-013 — Gestion des bénéficiaires externes", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-013-01 — La page bénéficiaires affiche la liste des bénéficiaires actifs avec IBAN masqué et badge zone SEPA", { tag: ["@FEAT-013", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-013-003, RG-013-007, RG-013-010"}, {"type": "Origine legacy", "description": "page /beneficiaires.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("beneficiaires");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("AJOUTER UN BENEFICIAIRE", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("MES BENEFICIAIRES ENREGISTRES", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("LIBELLE TITULAIRE IBAN BIC BANQUE ZONE SEPA ACTION", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("✓ SEPA", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("FR76***", { useInnerText: true });
      });
    });

    test("SC-013-02 — Ajouter un bénéficiaire avec IBAN MOD-97 valide affiche 'Bénéficiaire ajouté avec succès' et l'entrée dans la liste", { tag: ["@FEAT-013", "@golden-master", "@ecriture"], annotation: [{"type": "Règles couvertes", "description": "RG-013-004, RG-013-005, RG-013-006"}, {"type": "Origine legacy", "description": "page /beneficiaires.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("beneficiaires");
      });
      await test.step('Rejouer les actions enregistrées', async () => {
        await page.locator("[data-testid='beneficiaires-libelle']").first().fill("[retro] test beneficiaire");
        await page.locator("[data-testid='beneficiaires-titulaire']").first().fill("Retro Test SA");
        await page.locator("[data-testid='beneficiaires-iban']").first().fill("FR3530004000000123456789010");
        await page.locator("[data-testid='beneficiaires-verifier-iban']").first().click();
        await expect(page.locator('body')).toContainText("IBAN valide", { ignoreCase: true, useInnerText: true });
        await page.locator("[data-testid='beneficiaires-bic']").first().fill("HSBCFRPPXXX");
        await page.locator("[data-testid='beneficiaires-banque']").first().fill("HSBC France");
        await page.locator("[data-testid='beneficiaires-ajouter']").first().click();
        await expect(page.locator("[data-testid='beneficiaires-message-info']").first()).toBeVisible();
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Bénéficiaire ajouté avec succès", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("[retro] test beneficiaire", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Retro Test SA", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("FR35***9010", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("HSBCFRPPXXX", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("HSBC France", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("✓ SEPA", { useInnerText: true });
      });
    });
  });
});
