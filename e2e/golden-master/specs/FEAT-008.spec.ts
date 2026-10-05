// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-008 — Documents (relevés, attestations, IFU)", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-008-01 — La page documents affiche les liens de téléchargement IFU, RIB, attestation et relevés mensuels", { tag: ["@FEAT-008", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-008-003, RG-008-005, RG-008-006"}, {"type": "Origine legacy", "description": "page /documents.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("documents");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("DOCUMENTS FISCAUX", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Imprime Fiscal Unique (IFU)", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("RIB", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Attestation de solde", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Releves mensuels", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Telecharger PDF", { useInnerText: true });
      });
    });
  });
});
