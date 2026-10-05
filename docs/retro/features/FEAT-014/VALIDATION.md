---
feature: FEAT-014
title: Callback OAuth Powens
generated_by: retro-validation
date: 2026-10-03
---

# VALIDATION — FEAT-014 « Callback OAuth Powens »

_Source : `PowensCallbackBean.java`, `PowensBean.java`, `AppFilter.java`, `powens-callback.xhtml`._
_Interface : Écran — page transit uniquement, aucun champ de formulaire (`fields-raw.json` : 0 champ)._

---

## Particularité de cette feature

FEAT-014 est classée Écran mais ne contient aucun champ de formulaire interactif.
La page est un intermédiaire de redirection côté serveur : `f:viewAction processCallback()` redirecte vers `/comptes-tiers` avant tout rendu HTML.
Il n'y a donc ni validation front (HTML5), ni grille de sondes.
Les entrées du serveur sont trois paramètres GET : `error`, `connection_id`, `code` — traités par ordre de priorité dans `processCallback()`.

---

## Contrôles serveur des paramètres URL

| Paramètre | Présence | Traitement serveur | Source |
|---|---|---|---|
| `error` | Non vide (notEmpty) | `disconnect()` + redirect `/comptes-tiers` | applicatif |
| `connection_id` | Non vide, `error` absent | `handleCallback(connection_id)` + redirect | applicatif |
| `code` | Non vide, `error` et `connection_id` absents | `handleCallback(code)` (flow alternatif) + redirect | applicatif |
| — | Aucun paramètre non vide | message d'erreur « Callback Powens incomplet » + redirect | applicatif |

**Note** : tous les contrôles sont de source `applicatif` (logique dans). Aucun déclencheur front, aucune contrainte schéma, aucun trigger DB.

---

## Règles EARS

### Paramètre `error`

VAL-014-01-error-B1
IF the request contains a non-empty `error` parameter THEN THE SYSTEM SHALL call disconnect() to reset all Powens session state and redirect to /comptes-tiers.
  Source  : applicatif

### Paramètre `connection_id`

VAL-014-02-connection_id-B1
IF no error parameter is present and the request contains a non-empty `connection_id` parameter THEN THE SYSTEM SHALL invoke handleCallback(connection_id) and redirect to /comptes-tiers.
  Source  : applicatif

VAL-014-02-connection_id-B2
IF handleCallback is invoked and the session access token is null THEN THE SYSTEM SHALL abort account loading and set error message "Session expiree. Veuillez recommencer la connexion."
  Source  : applicatif

### Paramètre `code`

VAL-014-03-code-B1
IF no error parameter and no connection_id parameter are present and the request contains a non-empty `code` parameter THEN THE SYSTEM SHALL invoke handleCallback(code) as an alternative OAuth flow and redirect to /comptes-tiers.
  Source  : applicatif
  Note : Ce chemin est qualifié « flow alternatif » dans le code. Voir RG-014-004 (dependance-externe) — l'activation de ce chemin dépend du comportement réel de Powens Webview.

### Aucun paramètre

VAL-014-04-params-B1
IF none of the error, connection_id, or code parameters are present and non-empty THEN THE SYSTEM SHALL set an error message "Callback Powens incomplet. Parametres recus: [...]" and redirect to /comptes-tiers.
  Source  : applicatif

---

## Matrice de divergence

_Toutes les contraintes sont de kind `custom` (logique conditionnelle sur les paramètres URL, non réductible à un type comparable). Le script les classe `a_interpreter` — interprétation ci-dessous._

| Champ | Verdict | Front | Back (règle) | Nature | Interprétation agent |
|---|---|---|---|---|---|
| `error` | a_interpreter | — | VAL-014-01-error-B1 | métier | Routing OAuth explicite — à reproduire dans la cible. Côté front : aucun (transit serveur pur). |
| `connection_id` | a_interpreter | — | VAL-014-02-connection_id-B1, VAL-014-02-connection_id-B2 | métier | Chemin nominal Powens Webview + garde session. Les deux règles s'enchaînent : la B1 route, la B2 protège l'absence de token. À reproduire. |
| `code` | a_interpreter | — | VAL-014-03-code-B1 | métier | Fallback documenté dans le code. Voir RG-014-004 (dependance-externe) sur l'usage réel. À reproduire sauf réponse bloquante à RG-014-004. |
| `params` | a_interpreter | — | VAL-014-04-params-B1 | métier | Cas de sécurité défensif (callback inattendu). À reproduire. |

**Récapitulatif matrice** : 4 lignes — a_interpreter : 4 — Nature : métier 4 | dette-legacy 0

---

## Questions métier à trancher

Aucune nouvelle question dans cette étape.

La question ouverte sur le flow `code` est déjà enregistrée dans RG.md :
- RG-014-004 (dependance-externe) — Le paramètre `code` en callback est-il effectivement envoyé par Powens Webview dans la configuration actuelle ?
  Portée : bloquant — Preuve :

---

## Dette technique legacy

Aucune dette technique liée à la validation des paramètres URL de ce callback.
(RG-014-010 sur `Thread.sleep(2000)` est documentée dans RG.md — hors scope validation.)
