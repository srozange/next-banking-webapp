---
feature: FEAT-028
title: "Intégration Powens (Open Banking — agrégation)"
interface: Transversal
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# RG-028 — Intégration Powens (Open Banking — agrégation)

Périmètre : couche d'intégration Powens — PowensService (EJB @Stateless), PowensBean (@SessionScoped CDI), PowensCallbackBean (@RequestScoped CDI). L'interface utilisateur est portée par FEAT-012 (comptes-tiers.xhtml). FEAT-014 couvre la page powens-callback.xhtml comme feature distincte.

---

## Initiation de la connexion

RG-028-001 [À confirmer]
WHEN a user requests to connect an external bank account via Powens,
THE SYSTEM SHALL call the Powens /auth/init endpoint with HTTP Basic Auth (clientId:clientSecret, Base64-encoded), obtain a permanent auth token and a numeric user identifier.

RG-028-002 [À confirmer]
WHEN the Powens /auth/init call succeeds,
THE SYSTEM SHALL store the permanent auth token in the user's HTTP session before initiating any redirect.

RG-028-003 [À confirmer]
WHEN building the Powens Webview URL,
THE SYSTEM SHALL include the parameters domain, client_id, redirect_uri (URL-encoded), code (temporary token, URL-encoded), and connector_ids.
  (dependance-externe) La valeur de POWENS_CONNECTOR_ID (défaut sandbox : 59) détermine quels établissements bancaires sont proposés dans la Webview en production : quelle est la valeur cible de ce paramètre pour l'environnement de production ?
    Portée : information

RG-028-004 [À confirmer]
IF the Powens /auth/init call fails or does not return an auth_token,
THEN THE SYSTEM SHALL display the message "Impossible d initialiser la connexion Powens." and abort the connection without redirecting the user.

RG-028-005 [À confirmer]
WHEN initiating a Powens connection,
THE SYSTEM SHALL obtain a temporary code via GET /auth/token/code (Bearer permanent token) and embed it in the Webview URL.

---

## Traitement du callback Powens

RG-028-006 [À confirmer]
THE SYSTEM SHALL allow unauthenticated access to the Powens callback page (powens-callback.xhtml).

RG-028-007 [À confirmer]
WHEN the Powens Webview returns an explicit error parameter in the callback,
THE SYSTEM SHALL clear the Powens session state (disconnect) and redirect the user to /comptes-tiers.

RG-028-008 [À confirmer]
WHEN the Powens callback provides a connection_id parameter,
THE SYSTEM SHALL use it to trigger external account loading.

RG-028-009 [À confirmer]
WHEN the Powens callback provides no connection_id but provides a code parameter,
THE SYSTEM SHALL treat the code as the connection identifier and trigger external account loading.

RG-028-010 [À confirmer]
IF the Powens callback provides neither connection_id nor code,
THEN THE SYSTEM SHALL display an error message listing the names of the parameters actually received.

RG-028-011 [À confirmer]
WHEN processing a Powens callback,
THE SYSTEM SHALL always redirect to /comptes-tiers after processing (whether success or error).

---

## Chargement des comptes

RG-028-012 [Suspect]
WHEN loading external accounts after a Powens callback,
THE SYSTEM SHALL delay account retrieval by waiting before querying the Powens accounts API.

RG-028-013 [À confirmer]
IF the session's Powens auth token is null at callback processing time,
THEN THE SYSTEM SHALL display "Session expiree. Veuillez recommencer la connexion." and abort account loading.

RG-028-014 [À confirmer]
WHEN external accounts are successfully retrieved from Powens,
THE SYSTEM SHALL mark the Powens connection as established regardless of whether accounts are returned.

RG-028-015 [À confirmer]
IF no external accounts are returned after a successful Powens callback,
THEN THE SYSTEM SHALL display "Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes." without marking the connection as failed.

RG-028-016 [À confirmer]
WHEN external accounts are retrieved and at least one account exists,
THE SYSTEM SHALL automatically select the first account and load its transactions.

---

## Sélection de compte et transactions

RG-028-017 [À confirmer]
WHEN a user selects an external account,
THE SYSTEM SHALL load transactions for that account.

RG-028-018 [À confirmer]
WHEN loading transactions for an external account,
THE SYSTEM SHALL retrieve at most 20 transactions from the Powens API.

RG-028-019 [À confirmer]
IF a transaction's simplified_wording is absent or empty,
THEN THE SYSTEM SHALL use original_wording as the transaction label.

RG-028-020 [À confirmer]
IF an external account's balance field is absent or null in the Powens API response,
THEN THE SYSTEM SHALL represent that account's balance as zero.

RG-028-021 [À confirmer]
WHEN computing the total balance across all external accounts,
THE SYSTEM SHALL sum the balances of all accounts whose balance is not null, treating null as zero.

---

## Rafraîchissement et déconnexion

RG-028-022 [À confirmer]
WHEN a user refreshes external accounts,
THE SYSTEM SHALL reload the account list from Powens and, if no account was previously selected and at least one account is returned, automatically select the first account and load its transactions.

RG-028-023 [À confirmer]
WHEN a user disconnects from the Powens integration,
THE SYSTEM SHALL clear all Powens session state: auth token, accounts list, selected account, transaction list, connection flag, and error message.

---

## Infrastructure et configuration

RG-028-024 [À confirmer]
WHEN calling the Powens API,
THE SYSTEM SHALL apply a 15-second timeout for both connection establishment and response read.

RG-028-025 [Suspect]
IF the Powens API returns an HTTP 4xx or 5xx response for /users/me/accounts or /users/me/accounts/{id}/transactions,
THEN THE SYSTEM SHALL return an empty list to the caller without surfacing the HTTP error to the user.

RG-028-026 [À confirmer]
THE SYSTEM SHALL use HTTP Basic Auth (clientId:clientSecret, Base64-encoded) for POST /auth/init and HTTP Bearer token for all subsequent Powens API calls.

RG-028-027 [À confirmer]
THE SYSTEM SHALL configure the Powens integration entirely via environment variables (POWENS_DOMAIN, POWENS_CLIENT_ID, POWENS_CLIENT_SECRET, POWENS_REDIRECT_URI, POWENS_CONNECTOR_ID), with hardcoded defaults used when the variable is absent.
