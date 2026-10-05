// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :
// modifier replay-mapping.json, relancer la dérivation puis cette génération.
import { test as setup, expect } from '@playwright/test';

// Une session par rôle ; les mots de passe sont lus dans l'environnement, jamais dans un fichier.
const accounts: { role: string; login: string; password_env: string; start: string }[] = [{"role": "ADMIN", "login": "admin", "password_env": "RETRO_ADMIN_PASSWORD", "start": "administration"}, {"role": "CLIENT", "login": "user1", "password_env": "RETRO_CLIENT_PASSWORD", "start": "tableau-de-bord"}, {"role": "CONSEILLER", "login": "sophie", "password_env": "RETRO_CONSEILLER_PASSWORD", "start": "espace-conseiller"}];
const usernameSelectors = ["input[type='email']", "input[name*='username' i]", "input[name*='login' i]",
  "input[name*='user' i]", "input[id*='username' i]", "input[id*='login' i]", "input[type='text']"];

for (const account of accounts) {
  setup(`connexion du rôle ${account.role}`, async ({ page }) => {
    const password = process.env[account.password_env];
    expect(password, `variable d'environnement ${account.password_env} absente`).toBeTruthy();
    await page.goto(account.start); // route protégée : la cible présente son formulaire de connexion
    const passwordField = page.locator("input[type='password']");
    await expect(passwordField).toBeVisible();
    await page.locator(usernameSelectors.join(', ')).first().fill(account.login);
    await passwordField.fill(password!);
    await passwordField.press('Enter');
    await expect(passwordField).toHaveCount(0);
    await page.context().storageState({ path: `.auth/${account.role}.json` });
  });
}
