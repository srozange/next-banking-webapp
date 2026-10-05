---
feature: FEAT-019
name: Détection fraude nocturne par batch
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/ejb/NightlyBatchService.java
---

# Messages et libellés — FEAT-019 Détection fraude nocturne par batch

Sources : `src/main/java/com/accenture/bank/ejb/NightlyBatchService.java`
(méthode `runFraudDetection`, méthode `logBatchRun`).

Cette fonctionnalité est un traitement batch sans interface utilisateur : aucun
message ne s'affiche à l'écran. Les textes ci-dessous sont exclusivement des entrées
dans la table `BatchJobLog` (couche base de données) et des messages d'application
émis par le logger Log4j2.

---

## Entrée BatchJobLog (table d'audit)

L'entrée en base est produite par `logBatchRun("FRAUD_DETECTION", ...)` au terme de
chaque exécution (succès ou échec). Les champs `job_name` et `status` sont des valeurs
fixes ; le champ `details` est dynamique.

| Identifiant | Champ | Valeur exacte (source) | Condition | Preuve |
|---|---|---|---|---|
| MSG-019-001 | `job_name` | `FRAUD_DETECTION` | Toujours — nom technique du job dans la table d'audit | |
| MSG-019-002 | `status` | `SUCCESS` | Batch terminé sans exception — référencé par RG-019-006 | |
| MSG-019-003 | `status` | `FAILED` | Exception interceptée dans le bloc `catch` — référencé par RG-019-006, RG-019-008 | |
| MSG-019-004 | `details` | `Transactions suspectes detectees: {N} transaction(s) > 10000 EUR` | Succès — `{N}` est le nombre de transactions dont le montant est >= 10 000 EUR sur les 24 dernières heures — référencé par RG-019-005 (note : le texte écrit `> 10000` mais l'implémentation utilise `>= 10000`) | |
| MSG-019-005 | `details` | `Erreur: {message}` | Échec — `{message}` est `e.getMessage()` de l'exception interceptée — référencé par RG-019-008 | |

---

## Messages de log applicatif (Log4j2)

### Logs normaux (INFO) — produits par `logBatchRun`

Émis en fin d'exécution après persistance du `BatchJobLog`. Le gabarit complet est
`"Batch {job_name} completed: {status} - {details}"`.

| Identifiant | Gabarit exact (source) | Condition de déclenchement | Preuve |
|---|---|---|---|
| MSG-019-010 | `Batch FRAUD_DETECTION completed: SUCCESS - Transactions suspectes detectees: {N} transaction(s) > 10000 EUR` | BatchJobLog persisté — cas nominal — référencé par RG-019-006 | |
| MSG-019-011 | `Batch FRAUD_DETECTION completed: FAILED - Erreur: {message}` | BatchJobLog persisté — cas d'erreur — référencé par RG-019-006, RG-019-008 | |

### Logs d'erreur (ERROR)

| Identifiant | Gabarit exact (source) | Condition de déclenchement | Preuve |
|---|---|---|---|
| MSG-019-020 | `Batch fraud detection failed: {message}` | Exception interceptée dans `runFraudDetection` — `{message}` est `e.getMessage()` — émis dans le catch de la méthode principale, avant l'appel à `logBatchRun` — référencé par RG-019-008 | |
| MSG-019-021 | `Failed to log batch run for FRAUD_DETECTION: {message}` | Exception lors de la persistance du `BatchJobLog` lui-même — `{message}` est `e.getMessage()` — perte silencieuse de l'entrée d'audit | |
