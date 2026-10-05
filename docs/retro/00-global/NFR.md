# NFR — Exigences Non Fonctionnelles du Legacy Banking

> Généré automatiquement par l'étape `retro-golden-master` — source : HAR `retro/interfaces/session.har`, `import.sql`, code EJB.

---

## 1. Volumétrie (données de test — import.sql)

| Entité | Enregistrements |
|--------|----------------|
| `transactions` | 45 |
| `loan_repayments` | 18 |
| `bank_products` | 13 |
| `watchlist_items` | 9 |
| `customers` | 7 |
| `bank_accounts` | 7 |
| `notifications` | 6 |
| `fraud_alerts` | 6 |
| `budgets` | 6 |
| `investment_products` | 5 |
| `external_beneficiaries` | 5 |
| `documents` | 5 |
| `savings_products` | 4 |
| `risk_profiles` | 4 |
| `credit_scores` | 4 |
| `credit_products` | 4 |
| `recurring_transfers` | 3 |
| `loans` | 2 |

**Total INSERT** : 153 lignes dans l'import.sql (environnement demo — données de volumétrie production inconnues).

---

## 2. Temps de réponse (HAR session.har — environnement local)

| Statistique | Valeur |
|-------------|--------|
| Requêtes capturées | 127 |
| Temps min | 1 ms |
| Temps max | 1 217 ms |
| Temps moyen | 22 ms |

### Pages les plus lentes

| Page | Temps (ms) | Cause |
|------|-----------|-------|
| `/bourse.xhtml` | 1 217 (chargement initial) | Appel AlphaVantage (API externe) — timeout réseau en mode local |
| `/bourse.xhtml` | 469 (rechargement) | Idem — délai API externe variable |
| `/bourse.xhtml` | 303 (3e appel) | Idem |
| `/admin.xhtml` | 48 | Chargement pipeline prêts + clients |
| `/beneficiaires.xhtml` | 38 | Validation IBAN MOD-97 AJAX |
| `/budget.xhtml` | 32 | Agrégation catégories par transaction |
| `/releve.xhtml` | 32 | Requête transactions filtrées |

### SLA recommandé pour la cible moderne (Azure Container Apps)

| Type de page | SLA cible p95 | Commentaire |
|-------------|--------------|-------------|
| Pages statiques / navigation | < 200 ms | Login, dashboard skeleton |
| Pages à calcul léger | < 500 ms | Relevé, budget, bénéficiaires |
| Pages avec API externe (bourse) | < 3 000 ms | AlphaVantage rate-limited en prod |
| Téléchargement PDF | < 2 000 ms | Génération synchrone côté serveur |

---

## 3. Plages horaires des batchs (EJB @Schedule — WildFly 14)

| Batch | Classe | Déclenchement | Description |
|-------|--------|---------------|-------------|
| `INTEREST_CALCULATION` | `NightlyBatchService.runInterestCalculation` | Tous les jours à **02h00** | Applique le taux mensuel 0,5%/12 sur tous les comptes EPARGNE |
| `FRAUD_DETECTION` | `NightlyBatchService.runFraudDetection` | Tous les jours à **02h30** | Détecte les transactions > 10 000 EUR sur 24h et génère des alertes fraude |
| `MONTHLY_STATEMENTS` | `NightlyBatchService.runMonthlyStatements` | Le **1er du mois à 03h00** | Génère les relevés mensuels pour tous les clients actifs |
| `RECURRING_TRANSFERS` | `RecurringTransferScheduler` | Toutes les **5 minutes** | Exécute les virements programmés arrivés à échéance |
| `NOTIFICATION_CHECK` | `NotificationService` | Toutes les **30 minutes** | Vérifie les seuils budgétaires et génère des notifications |

### Plage de maintenance implicite

La plage **02h00 – 03h30** est occupée par 3 batchs consécutifs. La cible moderne devra respecter cette fenêtre ou migrer vers des jobs asynchrones (Azure Functions, Service Bus scheduled messages).

---

## 4. Contraintes de disponibilité

- **Détection fraude** : active 24h/24 — les transactions hors horaire 06h00-23h00 sont signalées (`isUnusualHour()`, `FraudDetectionService`).
- **Sessions JSF** : timeout implicite WildFly (défaut 30 min). Pas de configuration explicite trouvée dans `web.xml`.
- **MDB scoring crédit** : traitement asynchrone ActiveMQ Artemis — SLA interne non documenté. La réponse visible en UI dépend du délai de traitement du MDB (queue `pretDemandesQueue`).
- **AlphaVantage API** : appel synchrone dans `BourseBean` — aucun circuit-breaker ni timeout configuré. En cas d'indisponibilité, la page `/bourse.xhtml` bloque jusqu'à timeout HTTP (1 217 ms observé en local).

---

## 5. Sécurité — constats rétro-engineering

| Constat | Criticité | Règle |
|---------|-----------|-------|
| Session fixation non corrigée (commentaire JIRA-9500 dans `LoginBean`) | HAUTE | CWE-384 |
| IDOR sur `/docs/download?accountId=N` — aucune vérification ownership | HAUTE | RG-017-014 (dette-legacy) |
| IDOR sur virements programmés (accès IDs arbitraires) | HAUTE | RG-004-066 (dette-legacy) |
| Message d'erreur JSF expose stack Java (`IllegalStateException: Solde insuffisant`) | MOYENNE | RG-003-007 (Suspect) |
| `h:messages globalOnly="true"` masque les erreurs de champs en mode Production | FAIBLE | RG-001-017 (dette-legacy) |
| Logs sans masquage IBAN/montant (dette `LOG_DEBT` détectée dans plusieurs services) | FAIBLE | Règle permanente CLAUDE.md |

---

*Source* : `retro/interfaces/session.har` (127 requêtes), `src/main/resources/import.sql`, code EJB (`NightlyBatchService`, `RecurringTransferScheduler`, `NotificationService`, `FraudDetectionService`).
