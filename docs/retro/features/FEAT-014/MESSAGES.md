---
feature: FEAT-014
name: Callback OAuth Powens
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/webapp/powens-callback.xhtml
  - src/main/java/com/accenture/bank/powens/PowensCallbackBean.java
  - src/main/java/com/accenture/bank/powens/PowensBean.java
---

# MESSAGES — FEAT-014 « Callback OAuth Powens »

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Callback OAuth Powens » (`/powens-callback.xhtml`). Les `{variables}` désignent des
valeurs dynamiques interpolées à l'exécution.

> Note : la page transit redirige côté serveur (`f:viewAction`) avant tout rendu HTML ;
> les textes de la page (`Connexion en cours…`) ne sont jamais vus par l'utilisateur en conditions normales.
> Les messages applicatifs sont transmis via `PowensBean.errorMessage` et affichés sur la page
> de destination `/comptes-tiers`.

---

## Page transit (powens-callback.xhtml)

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de page (onglet) | `Connexion bancaire en cours...` | `<title>` de la page — jamais visible (redirect serveur avant rendu) | |
| Titre de section | `Connexion en cours...` | `<h2>` dans la boîte centrale — jamais visible (redirect serveur avant rendu) | |
| Texte descriptif | `Recuperation de vos comptes via Powens Open Banking` | `<p>` sous le spinner — jamais visible (redirect serveur avant rendu) | |

---

## Messages d'erreur applicatifs (transmis à /comptes-tiers via PowensBean.errorMessage)

| Identifiant | Texte exact | Déclencheur | Règle | Preuve |
|---|---|---|---|---|
| MSG-014-001 | `Session expiree. Veuillez recommencer la connexion.` | `connection_id` (ou `code`) reçu dans le callback mais aucun `accessToken` en session | RG-014-006 | |
| MSG-014-002 | `Callback Powens incomplet. Parametres recus: {params.keySet()}` | Aucun paramètre `error`, `connection_id` ni `code` reçu dans le callback ; `{params.keySet()}` est la liste littérale des clés URL reçues (ex. `[]`) | RG-014-005 | |
| MSG-014-003 | `Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes.` | Callback traité avec succès mais Powens ne retourne aucun compte (synchronisation en cours) | RG-014-008 | |
