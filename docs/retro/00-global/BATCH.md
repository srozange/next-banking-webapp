# Traitements nocturnes (Batch Jobs)

> **Mécanisme** : EJB `@javax.ejb.Schedule` — timer EJB géré par WildFly 14 (Undertow / EJB Container).  
> `persistent = false` sur tous les jobs : les déclenchements ne sont pas persistés en base, l'historique des fires est en mémoire JVM ; un redémarrage serveur repart à l'heure suivante.  
> Fuseau horaire : celui de la JVM WildFly (non explicitement configuré dans le code — vérifier `standalone-full.xml`).

---

## JOB-001 — Calcul des intérêts d'épargne

| Attribut | Valeur |
|---|---|
| **Classe** | `com.accenture.bank.ejb.NightlyBatchService` |
| **Méthode** | `runInterestCalculation()` |
| **Ligne** | 29 |
| **Planification** | Quotidien à 02:00:00 — `@Schedule(hour="2", minute="0", second="0", persistent=false)` |
| **Description** | Parcourt tous les comptes de type EPARGNE et applique un taux annuel de 0,5 % / 12 par mois. Crédite chaque compte et insère une transaction de type CREDIT avec la description « Interets mensuels livret epargne (batch nocturne) ». |
| **Idempotence** | Non idempotent : si le job s'exécute deux fois dans le même mois, les intérêts sont doublés. Aucune vérification de doublon. |
| **Comportement en échec** | `catch(Exception)` global → status `FAILED` loggé en BATCH_JOB_LOGS + `LOG.error`. Pas de retry, pas d'alerte. |
| **Volumétrie** | nombre de comptes EPARGNE en production |
| **Alerting** | Aucun (log applicatif seulement) |

---

## JOB-002 — Détection de fraude nocturne

| Attribut | Valeur |
|---|---|
| **Classe** | `com.accenture.bank.ejb.NightlyBatchService` |
| **Méthode** | `runFraudDetection()` |
| **Ligne** | 71 |
| **Planification** | Quotidien à 02:30:00 — `@Schedule(hour="2", minute="30", second="0", persistent=false)` |
| **Description** | Sélectionne toutes les transactions des 24 dernières heures dont le montant est ≥ 10 000 EUR et les enregistre comme « suspectes » dans BATCH_JOB_LOGS. Pas de blocage ni d'alerte utilisateur — uniquement un comptage journalisé. |
| **Idempotence** | Oui (lecture seule + insert BATCH_JOB_LOGS). |
| **Comportement en échec** | `catch(Exception)` → `logBatchRun(..., "FAILED", ...)` + `LOG.error`. Pas de retry. |
| **Volumétrie** | volume de transactions > 10 000 EUR / jour |
| **Alerting** | Aucun (log applicatif seulement) |

> **Note** : ce batch est distinct du mécanisme de détection temps réel de `FraudDetectionService` (JMS, FEAT-023). Les deux opèrent indépendamment.

---

## JOB-003 — Génération mensuelle de relevés

| Attribut | Valeur |
|---|---|
| **Classe** | `com.accenture.bank.ejb.NightlyBatchService` |
| **Méthode** | `runMonthlyStatements()` |
| **Ligne** | 96 |
| **Planification** | Le 1er de chaque mois à 03:00:00 — `@Schedule(hour="3", minute="0", second="0", dayOfMonth="1", persistent=false)` |
| **Description** | Compte les clients actifs de rôle CLIENT et enregistre le résultat dans BATCH_JOB_LOGS (jobName=`MONTHLY_STATEMENTS`). Le code ne génère pas de fichier PDF — il journalise uniquement le nombre de clients traités. La génération effective de PDF est déléguée au servlet `/docs/download` (FEAT-017). |
| **Idempotence** | Oui (lecture + insert BATCH_JOB_LOGS uniquement). |
| **Comportement en échec** | `catch(Exception)` → `logBatchRun(..., "FAILED", ...)`. Pas de retry. |
| **Volumétrie** | nombre de clients actifs en production |
| **Alerting** | Aucun |

---

## JOB-004 — Alerte solde bas (récurrent)

| Attribut | Valeur |
|---|---|
| **Classe** | `com.accenture.bank.ejb.NotificationService` |
| **Méthode** | `checkLowBalances()` |
| **Ligne** | 326 |
| **Planification** | Toutes les 30 minutes — `@Schedule(hour="*", minute="*/30", second="0", persistent=false)` |
| **Description** | Parcourt TOUS les clients actifs (scalabilité non garantie — JIRA-7200). Pour chaque compte, si le solde est inférieur à 100 EUR (seuil hardcodé), insère une notification de type `ACCOUNT_BALANCE` / priorité `HIGH` dans la table NOTIFICATIONS. |
| **Idempotence** | Non idempotent : si le solde reste bas, une notification est émise à chaque fire. Pas de déduplication ni de délai de grâce. |
| **Comportement en échec** | `try/catch` par client et par compte — un échec unitaire ne bloque pas les autres. `LOG.warn`. Pas d'alerte globale. |
| **Volumétrie** | dette : `SELECT ALL customers` sans pagination (JIRA-7200) |
| **Alerting** | Aucun (log seulement) |
| **Dette** | Seuil de 100 EUR hardcodé (`lowBalanceThreshold = 100`) — non configurable par client (JIRA-7001) |

---

## JOB-005 — Exécution des virements programmés

| Attribut | Valeur |
|---|---|
| **Classe** | `com.accenture.bank.ejb.RecurringTransferScheduler` |
| **Méthode** | `verifierVirementsProgrammes()` |
| **Ligne** | 21 |
| **Planification** | Toutes les 5 minutes — `@Schedule(minute="*/5", hour="*", persistent=false)` |
| **Description** | Appelle `RecurringTransferService.executeAll()` : sélectionne les virements programmés dont la date d'exécution suivante est ≤ maintenant (`RecurringTransfer.findDue`) et exécute chaque virement (débit source, crédit cible si compte interne connu, insert TRANSACTIONS). |
| **Idempotence** | Non garanti : si le job s'exécute deux fois avant mise à jour de `nextExecutionDate`, un doublon de virement est possible. |
| **Comportement en échec** | `catch(Exception)` par virement → `LOG.warn`. Les virements suivants continuent. Solde insuffisant = abandon silencieux (`LOG.warn`). |
| **Volumétrie** | nombre de virements programmés actifs |
| **Alerting** | Aucun |

---

## Tableau de synthèse

| ID | Job | Fréquence | Classe | Idempotent |
|---|---|---|---|---|
| JOB-001 | Calcul intérêts épargne | Quotidien 02:00 | NightlyBatchService | Non |
| JOB-002 | Détection fraude nocturne | Quotidien 02:30 | NightlyBatchService | Oui |
| JOB-003 | Génération relevés mensuels | 1er du mois 03:00 | NightlyBatchService | Oui |
| JOB-004 | Alerte solde bas | Toutes les 30 min | NotificationService | Non |
| JOB-005 | Virements programmés | Toutes les 5 min | RecurringTransferScheduler | Non garanti |
