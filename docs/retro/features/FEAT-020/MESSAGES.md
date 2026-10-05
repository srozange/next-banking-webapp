---
feature: FEAT-020
name: "Relevés mensuels — batch stub (dette-legacy)"
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/ejb/NightlyBatchService.java
---

# MESSAGES — FEAT-020 Relevés mensuels — batch stub (dette-legacy)

Inventaire exhaustif des textes émis par le système dans le périmètre de la
fonctionnalité « Relevés mensuels — batch stub (dette-legacy) ». Cette feature
ne comporte aucun écran ni champ de saisie ; les seuls textes produits sont les
messages journalisés et le contenu du champ `details` persisté dans `BatchJobLog`.

---

## Messages journalisés (server log)

| Identifiant | Niveau | Texte exact | Condition d'émission | Preuve |
|---|---|---|---|---|
| MSG-020-001 | INFO | `Batch MONTHLY_STATEMENTS completed: SUCCESS - Releves mensuels generes pour {N} clients` | Fin de batch sans exception — `{N}` = nombre de clients actifs de rôle CLIENT | |
| MSG-020-002 | INFO | `Batch MONTHLY_STATEMENTS completed: FAILED - Erreur: {e.getMessage()}` | Exception levée pendant le batch — `{e.getMessage()}` est le message de l'exception | |
| MSG-020-003 | ERROR | `Failed to log batch run for MONTHLY_STATEMENTS: {e.getMessage()}` | Exception levée pendant l'appel à `em.persist()` dans `logBatchRun()` — `{e.getMessage()}` est le message de l'exception | |

> **Note** : MSG-020-002 et MSG-020-003 exposent directement le message de l'exception Java
> dans le log. Voir RG-020-002 (dette-legacy).

---

## Champs `details` persistés dans BatchJobLog

| Identifiant | Texte exact | Condition de persistance | Preuve |
|---|---|---|---|
| MSG-020-010 | `Releves mensuels generes pour {N} clients` | Statut SUCCESS — `{N}` = `customerCount` (Long compté par la requête JPA) | 105 |
| MSG-020-011 | `Erreur: {e.getMessage()}` | Statut FAILED — `{e.getMessage()}` est le message de l'exception levée | |

> **Dette-legacy** : MSG-020-010 est trompeur — il indique « générés » alors qu'aucun document
> n'est produit (le batch est un stub qui compte les clients sans appeler
> `DocumentService.generateReleveDocument()`). Voir RG-020-002 (dette-legacy).

---

## Règles associées

| Message | Règle RG.md |
|---|---|
| MSG-020-001, MSG-020-010 (SUCCESS) | RG-020-003 |
| MSG-020-002, MSG-020-011 (FAILED) | RG-020-004 |
| MSG-020-003 (Failed to log) | RG-020-003, RG-020-004 (couche logBatchRun) |
| MSG-020-010 (libellé trompeur) | RG-020-002 |
