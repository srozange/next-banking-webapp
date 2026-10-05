---
feature: FEAT-008
title: "Documents (relevés, attestations, IFU)"
generated_by: retro-validation
---

# VALIDATION — FEAT-008 « Documents (relevés, attestations, IFU) »

## Contexte

La page `documents.xhtml` est entièrement en lecture seule : aucun formulaire, aucun `<input>`, aucun `<select>`. Tous les éléments interactifs sont des liens `<a href>` vers `DocumentDownloadServlet` (`/docs/download`). La validation front est vide ; toute la validation porte sur les paramètres URL reçus par le servlet : `type`, `accountId`, `year`, `month`.

Les méthodes `genererReleve`, `genererRIB`, `genererAttestationSolde` et `genererIFU` sont les méthodes dites « legacy PDF-only » : elles ne persistent pas dans la table `DOCUMENTS` (voir). Les contraintes de schéma de la table `PUBLIC.DOCUMENTS` (notamment `DOWNLOAD_COUNT NOT NULL`) ne s'appliquent pas au chemin de téléchargement direct.

---

## Bloc 1 — Validation front

**Aucun champ de formulaire.** La page est en lecture seule ; les liens de téléchargement sont générés côté serveur avec les paramètres corrects. Aucune validation front-end applicable.

---

## Bloc 2 — Validation back

Sources : code applicatif uniquement (`DocumentDownloadServlet.java`, `DocumentService.java`). Aucun trigger ni contrainte de schéma directement impliqués dans ce chemin.

### Champ : `type`

VAL-008-01-type-B1
IF type parameter is absent from the download request THEN THE SYSTEM SHALL reject the request.
  Source   : applicatif
  Message  : "Parametre type manquant" (HTTP 400)
  Observed : {"outcome": "rejected", "http_status": 400}

VAL-008-01-type-B2
IF type parameter is present but not one of RELEVE, RIB, ATTESTATION, or IFU (case-insensitive) THEN THE SYSTEM SHALL reject the request.
  Source   : applicatif
  Message  : "Type inconnu : {type}" (HTTP 400)
  Observed : {"outcome": "rejected", "http_status": 400}

### Champ : `accountId`

VAL-008-01-accountId-B1
IF type is RELEVE, RIB, or ATTESTATION and accountId parameter is absent, empty, or non-numeric THEN THE SYSTEM SHALL reject the request.
  Source   : applicatif
  Message  : "accountId manquant" (HTTP 400)
  Observed : {"outcome": "rejected", "http_status": 400}

VAL-008-01-accountId-B2
IF accountId does not match an existing bank account THEN THE SYSTEM SHALL reject the request.
  Source   : applicatif
  Message  : "Compte introuvable" (HTTP 404)
  Observed : {"outcome": "rejected", "http_status": 404}

### Champ : `year`

VAL-008-01-year-B1
IF RELEVE or IFU is requested with a year parameter that is absent, non-numeric, or outside a valid calendar range THEN THE SYSTEM SHALL reject the request.
  Source   : applicatif
  Observed : {"outcome": "accepted", "http_status": 200}

### Champ : `month`

VAL-008-01-month-B1
IF RELEVE is requested with a month parameter that is absent, non-numeric, or less than 1 THEN THE SYSTEM SHALL reject the request.
  Source   : applicatif
  Observed : {"outcome": "accepted", "http_status": 200}

VAL-008-01-month-B2
IF RELEVE is requested with a month parameter greater than 12 THEN THE SYSTEM SHALL reject the request.
  Source   : applicatif
  Observed : {"outcome": "accepted", "http_status": 200}

---

## Bloc 3 — Matrice de divergence

_Générée par `build-divergence-matrix.py`. La page est en lecture seule (aucun formulaire), donc toutes les contraintes sont `back_seul`. Les deux entrées `a_interpreter` (kind `custom`) sont tranchées ci-dessous._

| Champ | Kind | Verdict | Front | Back | Observed | Nature | Conséquence |
|---|---|---|---|---|---|---|---|
| `type` | `required` | `back_seul` | — | VAL-008-01-type-B1 : absent → HTTP 400 | rejected/400 | métier | Liens générés côté serveur avec `type` toujours présent — contrôle serveur en cas d'appel direct |
| `type` | `enum` | `back_seul` | — | VAL-008-01-type-B2 : valeur inconnue → HTTP 400 | rejected/400 | métier | Contrôle serveur en cas d'appel direct avec type invalide |
| `accountId` | `custom` | `back_seul` | — | VAL-008-01-accountId-B1 : manquant pour RELEVE/RIB/ATTESTATION → HTTP 400 | rejected/400 | métier | Voir résolution ci-dessous |
| `accountId` | `reference` | `back_seul` | — | VAL-008-01-accountId-B2 : compte inexistant → HTTP 404 | rejected/404 | métier | Contrôle correct côté serveur |
| `year` | `custom` | `back_seul` | — | VAL-008-01-year-B1 : invalide → devrait rejeter | accepted/200 | dette-legacy | Voir résolution ci-dessous |
| `month` | `min` | `back_seul` | — | VAL-008-01-month-B1 : month < 1 → devrait rejeter | accepted/200 | dette-legacy | Legacy génère un PDF vide — à corriger dans la cible |
| `month` | `max` | `back_seul` | — | VAL-008-01-month-B2 : month > 12 → devrait rejeter | accepted/200 | dette-legacy | Legacy génère un PDF vide — à corriger dans la cible |

### Résolution des lignes `a_interpreter`

**`accountId` / `custom` (VAL-008-01-accountId-B1) :**
- Contrainte conditionnelle : `accountId` est obligatoire uniquement quand `type` est `RELEVE`, `RIB` ou `ATTESTATION` (pas pour `IFU` qui utilise `customerId` issu de la session).
- Le legacy l'implémente correctement : `parseLong(req.getParameter("accountId"))` retourne `null` → `sendError(400, "accountId manquant")`.
- Verdict retenu : `back_seul`, `Nature : métier` — à reproduire dans la cible avec la même logique conditionnelle (la condition est le type du document).

**`year` / `custom` (VAL-008-01-year-B1) :**
- Le legacy n'implémente aucune validation de plage (observed : accepted, HTTP 200 avec PDF vide).
- `parseInt(null)` → 0 ; `genererReleve(accountId, 0, month)` exécute une requête JPQL `YEAR(date) = 0` qui ne correspond à aucune transaction → PDF vide servi sans erreur.
- Verdict retenu : `back_seul`, `Nature : dette-legacy` — la cible doit valider que `year` est un entier dans une plage valide (ex. 1900 ≤ year ≤ année courante) et retourner HTTP 400 hors plage.

---

## Note sécurité — IDOR (hors validation de paramètre)

Le servlet ne vérifie pas l'appartenance du compte (`accountId`) au client connecté. Tout `accountId` valide est servi sans contrôle d'appartenance. Cette dette de sécurité est documentée dans `RG-008-004` (Nature : dette-legacy — IDOR). Elle ne s'intègre pas dans le modèle de validation des paramètres (c'est un contrôle d'accès, pas une validation d'entrée) mais doit être corrigée dans la cible.

---

## Harness

Harness vide (0 cas) : `fields-raw.json` ne contient aucun champ de formulaire — la page est en lecture seule. La validation des paramètres URL du servlet (`type`, `accountId`, `year`, `month`) est documentée en Bloc 2 et dans la matrice. Les tests d'intégration de ces paramètres relèvent du périmètre `FEAT-017` (API).
