---
feature: FEAT-018
title: "Calcul des intérêts nocturnes"
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/ejb/NightlyBatchService.java
  - src/main/webapp/admin.xhtml
---

# Messages — FEAT-018 Calcul des intérêts nocturnes

Inventaire exhaustif des textes produits ou affichés par le système dans le périmètre
de la fonctionnalité « Calcul des intérêts nocturnes » (batch EJB Timer).

> Feature batch sans interface utilisateur directe. Les textes se répartissent en trois
> catégories : textes persistés en base (visibles dans le relevé de compte et dans les
> logs batch), journaux système (non visibles par l'utilisateur), et libellés de l'écran
> d'administration ADMIN qui affiche les logs de batch (référencé par RG-018-010).
> Les `{variables}` désignent des valeurs dynamiques interpolées à l'exécution.

---

## Textes persistés en base de données

### Description de transaction (visible dans le relevé)

| Identifiant | Texte exact | Contexte | Preuve |
|---|---|---|---|
| MSG-018-001 | `Interets mensuels livret epargne (batch nocturne)` | Description de chaque transaction CREDIT créée lors du calcul des intérêts — visible dans le relevé de compte | |

### Champs du journal de traitement batch (BatchJobLog)

| Identifiant | Texte exact | Contexte | Preuve |
|---|---|---|---|
| MSG-018-010 | `INTEREST_CALCULATION` | Valeur du champ `jobName` enregistrée dans BATCH_JOB_LOGS pour ce traitement | |
| MSG-018-011 | `SUCCESS` | Valeur du champ `status` en cas de succès | |
| MSG-018-012 | `FAILED` | Valeur du champ `status` en cas d'exception non gérée | |
| MSG-018-013 | `Interets calcules sur {N} comptes epargne` | Valeur du champ `details` en cas de succès ; `{N}` = nombre total de comptes EPARGNE évalués (y compris ceux ignorés car intérêt nul) | |
| MSG-018-014 | `Erreur: {message}` | Valeur du champ `details` en cas d'exception ; `{message}` = `e.getMessage()` de l'exception levée | |

---

## Journaux système (non visibles par l'utilisateur)

| Identifiant | Niveau | Gabarit du message | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-018-020 | `ERROR` | `Batch interest failed: {message}` | Exception levée durant le calcul des intérêts ; `{message}` = `e.getMessage()` | |
| MSG-018-021 | `INFO` | `Batch {jobName} completed: {status} - {details}` | Après chaque appel à `logBatchRun`, qu'il y ait succès ou échec du batch ; `{jobName}` = `INTEREST_CALCULATION`, `{status}` = `SUCCESS` ou `FAILED`, `{details}` = MSG-018-013 ou MSG-018-014 | |
| MSG-018-022 | `ERROR` | `Failed to log batch run for {jobName}: {message}` | Exception lors de la persistance du BatchJobLog lui-même ; `{jobName}` = `INTEREST_CALCULATION`, `{message}` = `e.getMessage()` | |

---

## Libellés de l'interface d'administration (écran ADMIN — section traitements nocturnes)

Libellés affichés dans la section « Traitements nocturnes » de l'écran d'administration,
qui liste les entrées de BATCH_JOB_LOGS incluant INTEREST_CALCULATION (→ RG-018-010).

### En-tête de section

| Identifiant | Texte exact | Contexte | Preuve |
|---|---|---|---|
| MSG-018-030 | `Traitements nocturnes (EJB Timer @Schedule)` | Titre de la section traitements nocturnes dans l'écran d'administration | |
| MSG-018-031 | `Rafraichir` | Bouton de rechargement de la liste des jobs récents | |

### En-têtes de colonnes du tableau

| Identifiant | Texte exact | Preuve |
|---|---|---|
| MSG-018-032 | `Job` | |
| MSG-018-033 | `Debut` | |
| MSG-018-034 | `Fin` | |
| MSG-018-035 | `Duree (ms)` | |
| MSG-018-036 | `Enregistrements` | |
| MSG-018-037 | `Statut` | |
| MSG-018-038 | `Details` | |

### État vide

| Identifiant | Texte exact | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-018-039 | `Aucun traitement nocturne enregistre` | Affiché à la place du tableau si `batchAdminBean.recentJobs` est vide | |
