---
feature: FEAT-014
title: Callback OAuth Powens
status: current
generated_by: retro-rules
date: 2026-10-03
---

# RG — FEAT-014 « Callback OAuth Powens »

_Source principale : `PowensCallbackBean.java`, `PowensBean.java`, `PowensService.java`, `AppFilter.java`, `powens-callback.xhtml`._
_Entrées : FIELDS.md (aucun champ formulaire — page transit uniquement), FLOW.md (5 états observés)._

---

## Règles d'accès et de transit

RG-014-001 [Confirmé]
THE SYSTEM SHALL allow access to `/powens-callback.xhtml` without requiring user authentication.

RG-014-002 [Confirmé]
IF the `error` parameter received in the callback request is present and non-empty
THEN THE SYSTEM SHALL disconnect the Powens session and redirect to `/comptes-tiers?faces-redirect=true`.

RG-014-003 [Confirmé]
IF no `error` parameter is present and the `connection_id` parameter is present and non-empty
THEN THE SYSTEM SHALL initiate account loading via handleCallback(connection_id) and redirect to `/comptes-tiers?faces-redirect=true`.

RG-014-004 [À confirmer]
IF no `error` parameter and no `connection_id` parameter are present and the `code` parameter is present and non-empty
THEN THE SYSTEM SHALL initiate account loading via handleCallback(code) as an alternative OAuth flow and redirect to `/comptes-tiers?faces-redirect=true`.
    Portée : bloquant
    Résolution : Inconnu à ce jour : à vérifier auprès de Powens (documentation de la Webview ou essai sur le sandbox). Tant que ce n'est pas confirmé, la cible ne traite que le connection_id ; la branche « code » n'est pas reprise.

RG-014-005 [Confirmé]
IF neither `error` nor `connection_id` nor `code` parameter is present in the callback request
THEN THE SYSTEM SHALL set an error message "Callback Powens incomplet. Parametres recus: [...]" and redirect to `/comptes-tiers?faces-redirect=true`.

---

## Règles de traitement du callback (handleCallback)

RG-014-006 [Confirmé]
IF handleCallback is invoked and the session Powens access token is absent
THEN THE SYSTEM SHALL abort account loading and inform the user that the session has expired.

RG-014-007 [À confirmer]
WHEN handleCallback is called with a valid session access token and Powens returns at least one account
THE SYSTEM SHALL mark the Powens connection as established, auto-select the first account, and load its transactions.

RG-014-008 [À confirmer]
WHEN handleCallback is called with a valid session access token and Powens returns no accounts
THE SYSTEM SHALL mark the Powens connection as established and inform the user that no accounts are available yet.

RG-014-009 [Confirmé]
WHEN disconnect() is called
THE SYSTEM SHALL reset all Powens session state: access token, external accounts list, selected account, transactions list, and connection status flag.

---

## Règles de chargement des données externes

RG-014-010 [Suspect]
WHEN Powens account synchronization is pending immediately after a callback
THE SYSTEM SHALL wait for synchronization to complete before retrieving accounts.

RG-014-011 [À confirmer]
WHEN loading transactions for an external account
THE SYSTEM SHALL retrieve at most 20 transactions.
