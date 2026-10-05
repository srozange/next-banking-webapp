// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test, expect } from '@playwright/test';

// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs
// réduits, lignes vides retirées.
const normalize = (text: string) =>
  text.slice(0, 5000).split('\n').map((l) => l.split(/\s+/).filter(Boolean).join(' ')).filter(Boolean);

test.describe("FEAT-010 — Chat avec le conseiller", () => {
  test.describe("rôle CLIENT", () => {
    test.use({ storageState: ".auth/CLIENT.json" });

    test("SC-010-01 — La page de chat charge avec le message de bienvenue de Sophie adressé au prénom de l'utilisateur", { tag: ["@FEAT-010", "@golden-master"], annotation: [{"type": "Règles couvertes", "description": "RG-010-003, RG-010-005, RG-010-006"}, {"type": "Origine legacy", "description": "page /conseiller.xhtml"}] }, async ({ page }) => {
      await test.step('Ouvrir la page', async () => {
        await page.goto("mon-conseiller");
      });
      await test.step('Comparer au texte attendu (legacy)', async () => {
        await expect.soft(page.locator('body')).toContainText("Sophie — Conseillère Demo Bank", { useInnerText: true });
        await expect.soft(page.locator('body')).toContainText("Bonjour Jean !", { useInnerText: true });
      });
    });
  });

  test.skip("SC-010-02 — Envoyer un message au chat affiche le message de l'utilisateur et une réponse de Sophie (HTTP 401 en local sans Azure)", { tag: ["@FEAT-010", "@non-rejoue"], annotation: {"type": "Non rejoué", "description": "BUG-038 — corrigé dans le forward, validé par les tests de la nouvelle application"} }, async () => {});
});
