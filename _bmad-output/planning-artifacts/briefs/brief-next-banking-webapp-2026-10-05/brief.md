---
title: "Product Brief: next-banking-webapp"
status: "final"
created: "2026-10-05"
updated: "2026-10-05"
source: "retro-forward-bmad"
source_commit: "c05512ecab8af8878d695ccd0f33fc3ece8811a4"
inputDocuments:
  - "docs/retro/00-global/FEATURES.md"
  - "docs/retro/00-global/PERMISSIONS.md"
  - "docs/retro/00-global/NFR.md"
  - "docs/retro/00-global/STACK.md"
  - "docs/retro/00-global/ARCHITECTURE.md"
  - "docs/retro/00-global/LEGACY-BUGS.md"
  - "docs/retro/00-global/legacy-bugs.json"
coverage_score: 0.88
---

# Product Brief: next-banking-webapp

## Executive Summary

Ce projet rénove une application bancaire existante, analysée au commit `c05512ecab8af8878d695ccd0f33fc3ece8811a4` (branche `main`) et bâtie sur une pile Java EE : JSF 2.3, EJB 3.2, JPA 2.2 et JAAS. L'architecture cible n'est pas imposée par l'existant ; le périmètre fonctionnel de départ est celui de l'application actuelle, à confirmer ou à revoir avec le métier dans le PRD.

La rétro-ingénierie a inventorié 32 features (1 intégration, 10 transversales, 21 utilisateur), dont 30 entrent dans le périmètre. Elle a documenté 637 règles de gestion et capturé 33 scénarios de golden master. Le métier a décidé que 50 bugs du legacy seront corrigés dans la nouvelle application.

## The Problem

L'application actuelle n'a aucune documentation de pile ni d'architecture : l'intention de conception ne peut pas être comparée à la réalité du code. La pile s'appuie sur Java EE 8 alors que WildFly 14 ne supporte que Java EE 7, Java EE 8 étant apporté par une dépendance déclarée explicitement. Le dépôt ne contient aucun outil de conteneurisation ni de CI/CD.

Côté architecture, la logique de validation métier est en partie dans les backing beans plutôt qu'exclusivement dans la couche EJB, ce que signalent deux TODO JIRA-9200 du code. Il n'existe pas de couche API REST : toute la communication passe directement de l'interface aux EJB via CDI. Enfin, 50 comportements défectueux du legacy sont à corriger dans la cible.

## The Solution

La nouvelle application reprend les 30 features du périmètre : FEAT-001 Connexion, FEAT-002 Tableau de bord, FEAT-003 Effectuer un virement, FEAT-004 Virements programmés, FEAT-005 Relevé de compte, FEAT-006 Budget et catégories de dépenses, FEAT-007 Demande et suivi de prêts, FEAT-008 Documents, FEAT-009 Bourse et watchlist, FEAT-010 Chat avec le conseiller, FEAT-011 Catalogue produits bancaires, FEAT-012 Comptes Tiers — Open Banking Powens, FEAT-013 Gestion des bénéficiaires externes, FEAT-014 Callback OAuth Powens, FEAT-015 Espace Conseiller — Portefeuille clients, FEAT-016 Administration — Gestion globale, FEAT-017 Téléchargement documents PDF, FEAT-018 Calcul des intérêts nocturnes, FEAT-019 Détection fraude nocturne par batch, FEAT-020 Relevés mensuels — batch stub, FEAT-021 Exécution des virements programmés, FEAT-022 Surveillance soldes faibles et génération d'alertes, FEAT-023 Traitement asynchrone des virements, FEAT-024 Traitement asynchrone approbation prêts, FEAT-025 Système de notifications internes, FEAT-026 Audit trail, FEAT-027 Détection de fraude en temps réel, FEAT-028 Intégration Powens, FEAT-029 Intégration AlphaVantage, FEAT-030 Contrôle d'accès RBAC.

Le legacy n'a laissé aucune documentation (README, ADR, ARCHITECTURE, CHANGELOG) décrivant sa pile ou son architecture : la spécification de référence est le corpus `docs/retro/`, en particulier les règles `docs/retro/features/*/RG.md`. L'interface du legacy est en français.

## What Makes This Different

La nouvelle application reproduit le comportement fonctionnel du legacy tel qu'il est documenté dans `docs/retro/` (commit analysé `c05512ecab8af8878d695ccd0f33fc3ece8811a4`) : les règles de gestion (`docs/retro/features/*/RG.md`) et les validations font foi, le golden master (`docs/retro/golden-master/`) en est la preuve exécutable. Seule exception : les 50 bugs du legacy à corriger (section « Bugs du legacy à corriger »), dont le comportement attendu remplace le comportement observé.

## Who This Serves

Rôles utilisateurs observés sur le legacy : **ADMIN**, **CONSEILLER**, **CLIENT**.

## Success Criteria

- Les 31 scénarios du golden master applicables passent au vert sur la nouvelle application (2 exclus : ils reproduisent des bugs à corriger) — contrat : `docs/retro/golden-master-contract.md`.
- Chacune des 637 règles de gestion (RG) est couverte par au moins une exigence et un test.
- Chacun des 50 bugs du legacy à corriger est couvert par un test qui vérifie le comportement attendu.
- 1. Volumétrie — référence mesurée sur le legacy (pas une cible) :
  - `transactions` : 45
  - `loan_repayments` : 18
  - `bank_products` : 13
  - `watchlist_items` : 9
  - `customers` : 7
  - `bank_accounts` : 7
  - `notifications` : 6
  - `fraud_alerts` : 6
  - `budgets` : 6
  - `investment_products` : 5
  - `external_beneficiaries` : 5
  - `documents` : 5
  - `savings_products` : 4
  - `risk_profiles` : 4
  - `credit_scores` : 4
  - `credit_products` : 4
  - `recurring_transfers` : 3
  - `loans` : 2
- 2. Temps de réponse — référence mesurée sur le legacy (pas une cible) :
  - Requêtes capturées : 127
  - Temps min : 1 ms
  - Temps max : 1 217 ms
  - Temps moyen : 22 ms
- 2. Temps de réponse — référence mesurée sur le legacy (pas une cible) :
  - `/bourse.xhtml` : 1 217 (chargement initial)
  - `/bourse.xhtml` : 469 (rechargement)
  - `/bourse.xhtml` : 303 (3e appel)
  - `/admin.xhtml` : 48
  - `/beneficiaires.xhtml` : 38
  - `/budget.xhtml` : 32
  - `/releve.xhtml` : 32
- 2. Temps de réponse — référence mesurée sur le legacy (pas une cible) :
  - Pages statiques / navigation : < 200 ms
  - Pages à calcul léger : < 500 ms
  - Pages avec API externe (bourse) : < 3 000 ms
  - Téléchargement PDF : < 2 000 ms
- 3. Plages horaires des batchs — référence mesurée sur le legacy (pas une cible) :
  - `INTEREST_CALCULATION` : `NightlyBatchService.runInterestCalculation`
  - `FRAUD_DETECTION` : `NightlyBatchService.runFraudDetection`
  - `MONTHLY_STATEMENTS` : `NightlyBatchService.runMonthlyStatements`
  - `RECURRING_TRANSFERS` : `RecurringTransferScheduler`
  - `NOTIFICATION_CHECK` : `NotificationService`
- 5. Sécurité — constats rétro-engineering — référence mesurée sur le legacy (pas une cible) :
  - Session fixation non corrigée : HAUTE
  - IDOR sur `/docs/download?accountId=N` — aucune vérification ownership : HAUTE
  - IDOR sur virements programmés (accès IDs arbitraires) : HAUTE
  - Message d'erreur JSF expose stack Java : MOYENNE
  - `h:messages globalOnly="true"` masque les erreurs de champs en mode Production : FAIBLE
  - Logs sans masquage IBAN/montant : FAIBLE

## Scope

**Dans le périmètre**

- FEAT-001 — Connexion
- FEAT-002 — Tableau de bord
- FEAT-003 — Effectuer un virement
- FEAT-004 — Virements programmés
- FEAT-005 — Relevé de compte
- FEAT-006 — Budget et catégories de dépenses
- FEAT-007 — Demande et suivi de prêts
- FEAT-008 — Documents
- FEAT-009 — Bourse et watchlist
- FEAT-010 — Chat avec le conseiller
- FEAT-011 — Catalogue produits bancaires
- FEAT-012 — Comptes Tiers — Open Banking Powens
- FEAT-013 — Gestion des bénéficiaires externes
- FEAT-014 — Callback OAuth Powens
- FEAT-015 — Espace Conseiller — Portefeuille clients
- FEAT-016 — Administration — Gestion globale
- FEAT-017 — Téléchargement documents PDF
- FEAT-018 — Calcul des intérêts nocturnes
- FEAT-019 — Détection fraude nocturne par batch
- FEAT-020 — Relevés mensuels — batch stub
- FEAT-021 — Exécution des virements programmés
- FEAT-022 — Surveillance soldes faibles et génération d'alertes
- FEAT-023 — Traitement asynchrone des virements
- FEAT-024 — Traitement asynchrone approbation prêts
- FEAT-025 — Système de notifications internes
- FEAT-026 — Audit trail
- FEAT-027 — Détection de fraude en temps réel
- FEAT-028 — Intégration Powens
- FEAT-029 — Intégration AlphaVantage
- FEAT-030 — Contrôle d'accès RBAC

**Hors périmètre**

- FEAT-031 — Analytique client et segmentation (fonctionnalité abandonnée, confirmée par le métier — à ne pas refaire)
- FEAT-032 — Bean gestion notifications UI (fonctionnalité abandonnée, confirmée par le métier — à ne pas refaire)

## Vision

Vision de la rénovation au-delà de la parité fonctionnelle (évolutions, cible à 2-3 ans) : à définir lors de la découverte de `bmad-prd`. La rétro ne décrit que le legacy.

## Bugs du legacy à corriger

Décision métier (gate retro-verify) : ces comportements du legacy sont corrigés dans la cible. Détail : `docs/retro/00-global/LEGACY-BUGS.md`.

| Bug | Règle | Comportement attendu |
|---|---|---|
| BUG-001 | RG-001-017 | IF a customer with `active = false` submits valid credentials THEN THE SYSTEM SHALL reject the login. |
| BUG-002 | RG-001-018 | THE SYSTEM SHALL store and verify passwords using a secure one-way hashing function. |
| BUG-003 | RG-001-019 | WHEN a user authenticates successfully THE SYSTEM SHALL invalidate the existing HTTP session before writing the authenticated user context. |
| BUG-004 | RG-002-002 | IF the database no longer contains the user whose username is stored in the HTTP session THEN THE SYSTEM SHALL handle the absence gracefully without exposing an error to the user. |
| BUG-005 | RG-002-007 | WHEN displaying recent transactions, THE SYSTEM SHALL draw them from the COURANT account. |
| BUG-006 | RG-002-020 | THE SYSTEM SHALL persist notification data across server restarts. |
| BUG-007 | RG-002-024 | WHEN a transfer is processed, THE SYSTEM SHALL generate exactly one notification per transfer (CONFIRMATION, ALERTE, or FRAUDE — mutually exclusive, never a combination). |
| BUG-008 | RG-003-007 | IF the source account balance is insufficient for the requested amount THEN THE SYSTEM SHALL reject the transfer and inform the user that the balance is insufficient. |
| BUG-009 | RG-003-014 | IF the transfer amount is greater than or equal to 5000 EUR THEN THE SYSTEM SHALL mark the transfer status as PENDING. |
| BUG-010 | RG-003-027 | IF the number of transactions executed by the customer on the current day exceeds 8 THEN THE SYSTEM SHALL raise a HIGH_FREQUENCY fraud alert with a risk score of 50. |
| BUG-011 | SC-003-03 | IF the source account balance is insufficient for the requested transfer amount THEN THE SYSTEM SHALL reject the transfer and inform the user with a clear, non-technical message that the balance is insufficient. |
| BUG-013 | RG-004-043 | WHEN toggle (suspend/reactivate) or delete is requested for a recurring transfer THE SYSTEM SHALL verify that the transfer belongs to the authenticated customer before executing the action. |
| BUG-014 | RG-004-061 | IF the source account balance is strictly less than the recurring transfer amount at execution time THEN THE SYSTEM SHALL skip the execution and notify the customer that the scheduled transfer could not be executed due to insufficient funds. |
| BUG-015 | RG-004-063 | IF the target account number does not correspond to any internal account THEN THE SYSTEM SHALL reject the recurring transfer before any debit, and inform the customer with a clear error message. |
| BUG-023 | RG-006-028 | THE SYSTEM SHALL exclude income categories such as 'Revenus' from the budget ceiling selection, as income categories cannot meaningfully be capped as spending. |
| BUG-025 | RG-007-028 | IF formDuree is between 301 and 360 months (inclusive) THEN THE SYSTEM SHALL reject the loan request as invalid. |
| BUG-027 | RG-008-004 | IF a logged-in user requests a RELEVE, RIB, or ATTESTATION document with an `accountId` that does not belong to them THEN THE SYSTEM SHALL reject the request. |
| BUG-034 | RG-009-021 | THE SYSTEM SHALL retrieve the AlphaVantage API key exclusively from a secure configuration source (environment variable or system property) and never fall back to a value committed to source code. |
| BUG-038 | RG-010-007 | IF the AI advisor service returns an authentication error THEN THE SYSTEM SHALL inform the user that the advisor is temporarily unavailable instead of exposing an authentication failure code. |
| BUG-039 | RG-011-010 | WHEN displaying a product's interest rate or expected return, THE SYSTEM SHALL display the value as a percentage (e.g. 3,00 %). |
| BUG-040 | RG-012-009 | WHEN le callback Powens est reçu avec un paramètre error THE SYSTEM SHALL déconnecter la session Powens et informer l'utilisateur de la raison du retour. |
| BUG-042 | RG-012-018 | IF un compte externe renvoyé par l'API Powens a un solde manquant (champ absent ou null) THEN THE SYSTEM SHALL distinguer l'absence de solde d'un solde effectivement nul. |
| BUG-043 | RG-012-027 | IF l'API Powens retourne une réponse HTTP >= 400 lors du chargement des comptes ou des transactions THEN THE SYSTEM SHALL informer l'utilisateur de l'indisponibilité temporaire du service. |
| BUG-044 | VAL-012-02-callback_params-B2 | IF request to powens-callback.xhtml contains error parameter THEN THE SYSTEM SHALL inform the user of the reason for the Powens connection failure. |
| BUG-047 | RG-013-035 | WHEN the user deletes a beneficiary THE SYSTEM SHALL verify that the beneficiary belongs to the authenticated user before executing the deletion. |
| BUG-054 | RG-016-014 | IF an exception occurs during loan approval or refusal THEN THE SYSTEM SHALL display a user-friendly error notification to the admin without exposing internal exception details. |
| BUG-055 | RG-016-025 | WHEN a client is displayed in the advisor detail panel THE SYSTEM SHALL display under the 'Client depuis' label the date the customer became a client of the bank, which may differ from the date the advisor was assigned to that customer. |
| BUG-056 | RG-016-034 | WHEN the admin views the fraud alert table without a status filter THE SYSTEM SHALL display all fraud alerts regardless of status. |
| BUG-058 | RG-017-011 | IF an authenticated session customer requests a document for an `accountId` that does not belong to them THEN THE SYSTEM SHALL reject the request. |
| BUG-059 | RG-017-013 | IF an unexpected error occurs during PDF generation THEN THE SYSTEM SHALL inform the client that document generation failed, without exposing internal error details. |
| BUG-064 | RG-018-003 | WHEN the interest calculation batch runs THE SYSTEM SHALL apply a monthly interest rate of 0.5% / 12 to each selected EPARGNE account once per month. |
| BUG-066 | RG-019-009 | WHEN the nightly fraud-detection batch computes the 24-hour lookback window THE SYSTEM SHALL define the boundary using a fixed-offset reference to ensure the window is always exactly 24 hours regardless of daylight saving time transitions. |
| BUG-068 | RG-021-007 | IF the beneficiary account number does not correspond to any internal account THEN THE SYSTEM SHALL reject the transfer before any debit, and inform the customer with a clear error message. |
| BUG-069 | RG-021-011 | WHEN computing the next execution date after a successful execution THE SYSTEM SHALL advance the next scheduled execution date computed from the originally scheduled date and the transfer frequency. |
| BUG-070 | RG-021-013 | WHEN the execution of an individual recurring transfer raises an exception THE SYSTEM SHALL isolate the failure and continue processing the remaining due transfers in the same batch run. |
| BUG-071 | RG-022-010 | WHEN a customer's account balance is below the threshold THE SYSTEM SHALL generate at most 20 low-balance notifications per account per day. |
| BUG-072 | RG-022-011 | WHEN the periodic low-balance scan completes THE SYSTEM SHALL record the execution result in the batch job execution log. |
| BUG-073 | RG-023-002 | IF the transfer cannot be handed over for processing THEN THE SYSTEM SHALL reject the transfer and inform the customer instead of reporting it as submitted. |
| BUG-076 | RG-023-009 | IF virement amount >= seuilFraudeEur (default: 10 000 EUR) THEN THE SYSTEM SHALL suspend the transfer pending manual fraud verification before applying any balance change. |
| BUG-077 | RG-024-020 | IF the loan request cannot be handed over for approval THEN THE SYSTEM SHALL reject the request and inform the customer instead of leaving it pending forever. |
| BUG-078 | RG-024-021 | IF the loan type is not recognized by the credit scorer THEN THE SYSTEM SHALL refuse the loan request. |
| BUG-079 | RG-025-011 | WHEN the periodic low-balance scan detects that an active customer's account balance is strictly less than 100 EUR THE SYSTEM SHALL display a low-balance notification to the customer in their account dashboard. |
| BUG-083 | RG-026-010 | WHEN any user action is audited THE SYSTEM SHALL record the originating client IP address in the audit entry. |
| BUG-084 | RG-027-003 | WHEN a transfer is submitted THE SYSTEM SHALL execute fraud detection before the transfer is finally committed; IF the fraud check raises an error THE SYSTEM SHALL log the error and the transfer SHALL NOT proceed silently. |
| BUG-085 | RG-027-006 | IF the number of transactions executed by the customer on the current day exceeds 8 THEN THE SYSTEM SHALL raise a HIGH_FREQUENCY fraud signal with risk score 50. |
| BUG-086 | RG-027-018 | THE SYSTEM SHALL mask the target IBAN in all server log entries (e.g. FR76****1234) to avoid exposing sensitive payment data. |
| BUG-089 | RG-028-025 | IF the Powens API returns an error response when loading accounts or transactions THE SYSTEM SHALL propagate an error indicator to the caller so that the user can be informed of the service unavailability. |
| BUG-091 | RG-029-012 | THE SYSTEM SHALL protect the AlphaVantage API key from being exposed in the source code. |
| BUG-092 | RG-030-014 | WHEN a user successfully authenticates THE SYSTEM SHALL invalidate the existing HTTP session and create a fresh session before storing the authentication context. |
| BUG-093 | RG-030-015 | THE SYSTEM SHALL enforce role-based access control at the service layer independently of the HTTP filter. |

## Questions ouvertes

- La méthode hasEcheancier inclut explicitement l'état DEFAUT () mais aucune transition du code ne mène à cet état : la fonctionnalité « voir l'échéancier d'un prêt DEFAUT » est-elle destinée à une future gestion des impayés ou peut-elle être retirée de la réécriture ?
- L'état `DEFAUT` est prévu dans l'enum et l'UI mais sans transition dans le code : était-il implémenté dans une version antérieure ou est-ce une fonctionnalité à venir pour la gestion des impayés ?
- La méthode `incrementDownloadCount()` et le champ `downloadCount` sont déclarés dans DocumentService et Document.java mais jamais appelés depuis DocumentDownloadServlet ni depuis DocumentBean : le compteur reste toujours à 0. La fonctionnalité de comptage des téléchargements doit-elle être implémentée dans la réécriture ?
- Les méthodes findSavingsProducts(), findCreditProducts() et findInvestmentProducts() de BankProductService existent mais n'ont aucun appelant connu dans le code — sont-elles mortes (à ne pas reproduire dans la réécriture) ou désignent-elles une API externe ou future à conserver ?
- La méthode refreshAccounts() est implémentée dans PowensBean mais n'est câblée à aucun bouton ni lien dans comptes-tiers.xhtml : le bouton "Rafraîchir" doit-il être exposé dans la version cible ?
- Les seuils SEUIL_FRAUDE_EUR, SEUIL_ALERTE_EUR et SEUIL_PENDING_EUR sont-ils surchargés en production (valeurs par défaut code : 10 000, 5 000, 5 000 EUR) ?
- evaluateSingleScore() est déclarée @Deprecated et utilisée par des « legacy batch reports » selon son Javadoc, mais aucun caller n'existe dans ce dépôt — existe-t-il un consommateur externe (rapport batch ou outil tiers) qui en dépend, ou cette méthode est-elle morte ?
- La valeur de POWENS_CONNECTOR_ID (défaut sandbox : 59) détermine quels établissements bancaires sont proposés dans la Webview en production : quelle est la valeur cible de ce paramètre pour l'environnement de production ?
- Règles voisines entre fonctionnalités — RG-001-010 (FEAT-001) « WHEN a user initiates logout THE SYSTEM SHALL invalidate the current session and redirect to `/login.xhtml`. » ; RG-030-010 (FEAT-030) « WHEN a user logs out THE SYSTEM SHALL invalidate the HTTP session and redirect to `login.xhtml`. » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-001-011 (FEAT-001) « WHEN an authenticated customer whose role is not ADMIN requests `/admin.xhtml` THE SYSTEM SHALL redirect to `/dashboard.xhtml`. » ; RG-030-003 (FEAT-030) « IF an authenticated user whose role is not ADMIN requests `admin.xhtml` THEN THE SYSTEM SHALL redirect the request to `dashboard.xhtml`. » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-003-020 (FEAT-003) « WHEN the CONFIRMATION notification is generated AND the motif field is not empty THE SYSTEM SHALL include the motif in the notification message. » ; RG-023-007 (FEAT-023) « WHEN a CONFIRMATION notification is generated AND the transfer motif is non-null and non-empty THE SYSTEM SHALL include the motif text in the notification message. » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-003-025 (FEAT-003) « IF the transfer amount is greater than or equal to 5000 EUR THEN THE SYSTEM SHALL raise a LARGE_AMOUNT fraud alert with a risk score of 40. » ; RG-003-026 (FEAT-003) « IF the transfer amount is greater than or equal to 15000 EUR THEN THE SYSTEM SHALL raise a LARGE_AMOUNT fraud alert with a risk score of 80. » ; RG-027-005 (FEAT-027) « IF the transfer amount is greater than or equal to 15 000 EUR THEN THE SYSTEM SHALL raise a LARGE_AMOUNT fraud signal with risk score 80. » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-003-030 (FEAT-003) « IF the transfer amount is greater than or equal to 1000 EUR AND is an exact multiple of 1000 THEN THE SYSTEM SHALL raise a ROUND_AMOUNT fraud alert with a risk score of 20. » ; RG-027-009 (FEAT-027) « IF the transfer amount is greater than or equal to 1 000 EUR and is an exact multiple of 1 000 THEN THE SYSTEM SHALL raise a ROUND_AMOUNT fraud signal with risk score 20. » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-003-032 (FEAT-003) « IF a previous transaction from the same customer occurred less than 30 seconds ago THEN THE SYSTEM SHALL raise a RAPID_SUCCESSION fraud alert with a risk score of 55. » ; RG-027-011 (FEAT-027) « IF the previous transfer from the same customer occurred less than 30 seconds before the current one THEN THE SYSTEM SHALL raise a RAPID_SUCCESSION fraud signal with risk score 55. » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-004-013 (FEAT-004) « IF the amount (`montant`) is null or less than or equal to zero THEN THE SYSTEM SHALL reject the submission and display "Montant invalide". » ; RG-007-003 (FEAT-007) « IF simMontant is null or less than or equal to zero THEN THE SYSTEM SHALL reject the simulation and display "Montant invalide". » ; RG-007-012 (FEAT-007) « IF formMontant is null or less than or equal to zero THEN THE SYSTEM SHALL reject the loan request and display "Montant invalide". » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-004-061 (FEAT-004) « IF the source account balance is strictly less than the transfer amount at execution time THEN THE SYSTEM SHALL skip the execution without changing the transfer state or notifying the customer. » ; RG-021-004 (FEAT-021) « IF the source account balance is strictly less than the transfer amount THEN THE SYSTEM SHALL skip the transfer without advancing the next execution date. » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-008-001 (FEAT-008) « WHEN an unauthenticated user requests `documents.xhtml` THE SYSTEM SHALL redirect to the login page. » ; RG-010-001 (FEAT-010) « WHEN an unauthenticated user requests /conseiller.xhtml, THE SYSTEM SHALL redirect to /login.xhtml. » ; RG-015-001 (FEAT-015) « IF an unauthenticated user requests conseiller-workspace.xhtml THEN THE SYSTEM SHALL redirect to login.xhtml. » ; RG-016-001 (FEAT-016) « WHEN an unauthenticated user requests admin.xhtml THE SYSTEM SHALL redirect to login.xhtml. » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-010-002 (FEAT-010) « THE SYSTEM SHALL grant access to /conseiller.xhtml to any authenticated user regardless of role (CLIENT, CONSEILLER, ADMIN). » ; RG-011-001 (FEAT-011) « THE SYSTEM SHALL grant access to the product catalogue page to any authenticated user regardless of role (CLIENT, CONSEILLER, ADMIN). » ; RG-013-002 (FEAT-013) « THE SYSTEM SHALL grant access to /beneficiaires.xhtml to all authenticated users regardless of role (CLIENT, CONSEILLER, ADMIN). » : même règle, ou différence voulue entre ces fonctionnalités ?
- Règles voisines entre fonctionnalités — RG-014-011 (FEAT-014) « WHEN loading transactions for an external account THE SYSTEM SHALL retrieve at most 20 transactions. » ; RG-028-018 (FEAT-028) « WHEN loading transactions for an external account, THE SYSTEM SHALL retrieve at most 20 transactions from the Powens API. » : même règle, ou différence voulue entre ces fonctionnalités ?
- 63 lacunes documentées dans le corpus (`docs/retro/GAPS.md`).
