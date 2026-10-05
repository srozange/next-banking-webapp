# FIELDS — FEAT-008 « Documents (relevés, attestations, IFU) »

_Généré par retro-ui — source : snapshots MCP Playwright, 3 rôles (CLIENT, ADMIN, CONSEILLER)._

## Champs de formulaire

**Aucun champ de formulaire.** La page `documents.xhtml` est entièrement en lecture seule : elle ne contient que des liens de téléchargement (`<a href>`). Aucun `<input>`, `<select>` ni `<textarea>` n'est présent.

## Liens de téléchargement — paramètres observés

Tous les liens pointent vers `DocumentDownloadServlet` (`/banking-legacy/docs/download`) avec les paramètres suivants :

| Type document | Paramètre `type` | Paramètres additionnels | Exemple URL observée |
|---|---|---|---|
| Relevé mensuel | `RELEVE` | `accountId` (entier), `year` (YYYY), `month` (M, sans zéro) | `/docs/download?type=RELEVE&accountId=1&year=2026&month=10` |
| RIB | `RIB` | `accountId` (entier) | `/docs/download?type=RIB&accountId=1` |
| Attestation de solde | `ATTESTATION` | `accountId` (entier) | `/docs/download?type=ATTESTATION&accountId=1` |
| Imprimé Fiscal Unique | `IFU` | `year` (YYYY) — pas d'`accountId` (cross-compte) | `/docs/download?type=IFU&year=2025` |

## Structure de la page (observée au crawl)

La page est structurée en sections :

1. **Documents fiscaux** (hors compte)
   - IFU — 2 années disponibles (2024, 2025) pour le client `user1`

2. **Par compte** (un bloc par compte du client connecté)
   - En-tête : type de compte + IBAN abrégé (ex. `Compte COURANT - FR76-0001-0001`)
   - RIB — 1 lien par compte
   - Attestation de solde — 1 lien par compte (solde au jour de la demande)
   - Relevés mensuels — liste des 12 derniers mois (lien par mois)

**Comptes observés pour `user1` (CLIENT) :**
- Compte COURANT `FR76-0001-0001` (accountId=1)
- Compte EPARGNE `FR76-0001-0002` (accountId=2)

## Différences par rôle

| Rôle | Contenu des documents | Navigation |
|---|---|---|
| CLIENT | Documents du client connecté | Liens standard (pas Espace Conseiller, pas Administration) |
| CONSEILLER | Documents du conseiller connecté (même structure) | + lien Espace Conseiller |
| ADMIN | Documents de l'admin connecté (même structure) | + liens Espace Conseiller + Administration |

Aucune restriction de contenu documentaire par rôle : chaque utilisateur voit ses propres documents, quelle que soit son rôle.

## États non observés

| État | Raison |
|---|---|
| Vide | Pas de compte sans documents dans les données de test |
| Erreur | Page en lecture seule — aucun formulaire soumis, erreur serveur non provoquable depuis l'UI |
| Modale | Aucune sur cette page |
| Confirmation | Aucune opération d'écriture |
| Chargement | JSF rendu serveur synchrone — pas d'état de chargement visible |
