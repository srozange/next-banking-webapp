---
feature: FEAT-012
title: "Comptes Tiers — Open Banking Powens"
status: current
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
generated_by: retro-validation
---

# VALIDATION — FEAT-012 Comptes Tiers — Open Banking Powens

_Généré par retro-validation · 2026-10-03_
_Sources : PowensBean.java · PowensCallbackBean.java · PowensService.java · comptes-tiers.xhtml_

---

## Note sur les champs

Cette feature ne contient **aucun champ de saisie utilisateur**. Toutes les interactions passent par des boutons d'action (`Connecter une banque`, `Déconnecter`) et par le flux OAuth Powens (Webview externe + callback URL). La saisie de données bancaires est entièrement déléguée à la Webview Powens externe.

Les champs présents dans `fields-raw.json` sont tous des champs JSF internes (tokens anti-CSRF `javax.faces.ViewState`, identifiants de formulaires JSF générés) ou un bouton `submit`. Aucun ne porte de règle de validation front ou back : verdict `aucune_règle` pour tous.

---

## Règles EARS — Contrôles applicatifs back-end

Les contrôles serveur de cette feature portent sur trois domaines : les paramètres du callback OAuth, l'état de session, et la réponse de l'API Powens. Tous sont de source `applicatif` (visibles dans le code).

### Callback OAuth (PowensCallbackBean.processCallback)

VAL-012-02-callback_params-B1
IF request to powens-callback.xhtml contains neither connection_id nor code nor error parameter THEN THE SYSTEM SHALL display error message indicating incomplete callback.
  Source : applicatif
  Message : "Callback Powens incomplet. Parametres recus: []"
  observed: {"outcome": "rejected"}

VAL-012-02-callback_params-B2
IF request to powens-callback.xhtml contains error parameter THEN THE SYSTEM SHALL inform the user of the reason for the Powens connection failure.
  Source : applicatif
  observed: {"outcome": "accepted"}

VAL-012-02-callback_params-B3
IF request to powens-callback.xhtml contains connection_id parameter THEN THE SYSTEM SHALL trigger account loading using that connection identifier.
  Source : applicatif

VAL-012-02-callback_params-B4
IF request to powens-callback.xhtml contains code parameter without connection_id THEN THE SYSTEM SHALL trigger account loading using that code as fallback identifier.
  Source : applicatif

### État de session (PowensBean.handleCallback)

VAL-012-02-accessToken-B1
IF accessToken session attribute is null when processCallback executes THEN THE SYSTEM SHALL reject the callback and display a session expiration message.
  Source : applicatif
  Message : "Session expiree. Veuillez recommencer la connexion."

### Réponse API Powens (PowensBean.connectBank)

VAL-012-01-apiResponse-B1
IF initAndGetWebviewUrl returns null THEN THE SYSTEM SHALL display an error and remain in non-connected state.
  Source : applicatif
  Message : "Impossible d initialiser la connexion Powens."

---

## Matrice de divergence

Générée par `build-divergence-matrix.py` · 8 lignes · 3 `a_interpreter` + 5 `aucune_règle`.

### Champs JSF internes (aucune_règle)

| Champ | Verdict | Remarque |
|---|---|---|
| `j_idt6` | `aucune_règle` | Identifiant de formulaire JSF interne |
| `javax.faces.ViewState` | `aucune_règle` | Token anti-CSRF JSF — géré par le framework |
| `j_idt20` | `aucune_règle` | Identifiant de formulaire JSF interne |
| `j_idt20:j_idt21` | `aucune_règle` | Bouton submit "Deconnecter" — aucun champ texte associé |
| `accountsForm` | `aucune_règle` | Identifiant de formulaire JSF interne |

### Contrôles applicatifs (a_interpreter)

Les trois lignes `a_interpreter` portent des contraintes `custom` : contrôles de routage OAuth et de session, non réductibles mécaniquement au vocabulaire de validation de champs.

| Champ | Verdict | Front | Back (IDs) | Nature | Interprétation |
|---|---|---|---|---|---|
| `callback_params` | `a_interpreter` | — | B1, B3, B4 | métier | Règles de routage du callback OAuth (erreur si manque de paramètres, déclenchement du chargement selon le paramètre présent) — aucun équivalent front, la page ne reçoit pas de saisie utilisateur. À reproduire dans la cible. |
| `callback_params` | `a_interpreter` | — | B2 | dette-legacy | Le retour Powens avec `error` (user_cancelled, erreur serveur) provoque un disconnect silencieux — aucun message explicatif. La cible doit afficher un message indiquant la raison du retour. |
| `accessToken` | `a_interpreter` | — | B1 | métier | Contrôle de session garantissant que l'auth_token Powens est disponible avant traitement du callback. À reproduire dans la cible. |
| `apiResponse` | `a_interpreter` | — | B1 | métier | Gestion d'erreur de l'API Powens lors de l'initiation de la webview. Message d'erreur affiché correctement. À reproduire dans la cible. |

---

## Validation live

| # | Action | Paramètres | Résultat observé | Règle vérifiée |
|---|---|---|---|---|
| 1 | GET `comptes-tiers.xhtml` (rôle CLIENT) | — | Page non-connectée : logos banques + bouton "Connecter une banque" visible | RG-012-004 |
| 2 | GET `powens-callback.xhtml` (sans paramètres) | aucun | Redirect vers `comptes-tiers.xhtml` · message "⚠️ Callback Powens incomplet. Parametres recus: []" affiché | VAL-012-02-callback_params-B1 |
| 3 | GET `powens-callback.xhtml?error=retro_test` | error=retro_test | Redirect vers `comptes-tiers.xhtml` · état non-connecté · aucun message d'erreur affiché | VAL-012-02-callback_params-B2 (dette confirmée) |

2 vérifications live (GET sans écriture persistante — paramètre error=retro_test = valeur inventée, aucun compte de test réel utilisé).
Écritures journalisées : 0 (GET uniquement, aucune modification de données persistantes).

---

## Dette technique legacy

| Règle | Défaut | Conséquence |
|---|---|---|
| VAL-012-02-callback_params-B2 | Retour Powens avec paramètre `error` → disconnect silencieux sans message utilisateur | L'utilisateur (ex. après annulation dans la Webview) retourne à l'état non-connecté sans comprendre pourquoi — à corriger dans la cible |
| RG-012-027 (de RG.md) | Erreur HTTP 4xx/5xx Powens dans `getAccounts()` / `getTransactions()` → null retourné silencieusement | L'utilisateur voit "aucun compte" ou "aucune transaction" sans distinction d'une défaillance API — à corriger dans la cible |
| RG-012-014 (de RG.md) | `Thread.sleep(2000)` bloquant dans un bean CDI `@SessionScoped` avant `getAccounts()` | Antipatterne CDI ; à remplacer par un mécanisme de retry asynchrone dans la cible |
