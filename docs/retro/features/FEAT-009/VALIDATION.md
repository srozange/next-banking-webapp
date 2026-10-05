---
feature: FEAT-009
name: Bourse et watchlist
status: current
generated_by: retro-validation
date: 2026-10-03
---

# Validation — FEAT-009 « Bourse et watchlist »

Périmètre : formulaire d'ajout à la watchlist (`bourse.xhtml` — panneau dynamique SCR-009-03).
Les champs `symboleValeur` et `periode` sont des sélecteurs de navigation (AJAX affichage) sans contrainte de validation : aucune règle VAL ne leur est associée.

---

## Bloc 1 — Validation front (par champ)

### `newSymbole` — Symbole Alpha Vantage (texte libre, obligatoire)

```
VAL-009-03-newSymbole-F1
WHEN user submits the add form with empty or blank newSymbole THE SYSTEM SHALL display
"Le symbole est obligatoire (ex: AMZN, SG.PA)" and keep the form open
  check   : {kind: required}
  trigger : soumission
  message : "Le symbole est obligatoire (ex: AMZN, SG.PA)"
```

### `newNom` — Nom de la valeur (texte libre, obligatoire)

```
VAL-009-03-newNom-F1
WHEN user submits the add form with empty or blank newNom THE SYSTEM SHALL display
"Le nom de la valeur est obligatoire" and keep the form open
  check   : {kind: required}
  trigger : soumission
  message : "Le nom de la valeur est obligatoire"
```

### `newMarche`, `newDevise`, `symboleValeur`, `periode` — aucune règle front

Champs de type select avec valeurs par défaut — aucune validation front identifiée.

---

## Bloc 2 — Validation back (serveur)

### `newSymbole`

```
VAL-009-03-newSymbole-B1
IF newSymbole is empty or blank THEN THE SYSTEM SHALL reject the submission and display
"Le symbole est obligatoire (ex: AMZN, SG.PA)"
  check   : {kind: required}
  source  : applicatif
  message : "Le symbole est obligatoire (ex: AMZN, SG.PA)"
```

```
VAL-009-03-newSymbole-B2
THE SYSTEM SHALL reject symbol values longer than 20 characters
  check    : {kind: max_length, value: 20}
  source   : schema
  column : SYMBOL — @Column(length=20)
  observed : {outcome: "accepted"} — legacy silently truncates to 20 chars (H2 VARCHAR(20))
             perte de données sans notification (ex: "TOOLONGSYMBOL12345678901" → "TOOLONGSYMBOL1234567")
```

```
VAL-009-03-newSymbole-B3
IF newSymbole matches an existing symbol (case-insensitive) THEN THE SYSTEM SHALL reject the submission
  check   : {kind: unique}
  source  : applicatif
  message : "Le symbole « {SYMBOL} » existe déjà dans votre watchlist"
  Note    : le message inclut le symbole en majuscules (newSymbol.toUpperCase())
```

### `newNom`

```
VAL-009-03-newNom-B1
IF newNom is empty or blank THEN THE SYSTEM SHALL reject the submission and display
"Le nom de la valeur est obligatoire"
  check   : {kind: required}
  source  : applicatif
  message : "Le nom de la valeur est obligatoire"
```

```
VAL-009-03-newNom-B2
THE SYSTEM SHALL reject name values longer than 100 characters
  check    : {kind: max_length, value: 100}
  source   : schema
  column : NAME — @Column(length=100)
  observed : {outcome: "rejected"}
             (dialog navigateur "serverError: class javax.faces.el.EvaluationException
             javax.persistence.PersistenceException: org.hibernate.exception.DataException:
             could not execute statement") — aucun message métier affiché
```

### `newMarche`

```
VAL-009-03-newMarche-B1
THE SYSTEM SHALL reject market values longer than 50 characters
  check   : {kind: max_length, value: 50}
  source  : schema
  column : MARKET — @Column(length=50)
  Note    : pratiquement inatteignable via l'UI (select — option la plus longue : "Euronext Paris" = 13 chars)
```

```
VAL-009-03-newMarche-B2
THE SYSTEM SHALL store a non-null market value
  check   : {kind: required}
  source  : schema
  column : MARKET — @Column(nullable=false)
  Note : garanti par la valeur par défaut "Euronext Paris" — inatteignable via l'UI
```

### `newDevise`

```
VAL-009-03-newDevise-B1
THE SYSTEM SHALL reject currency values longer than 10 characters
  check   : {kind: max_length, value: 10}
  source  : schema
  column : CURRENCY — @Column(length=10)
  Note    : pratiquement inatteignable via l'UI (select — options EUR/USD/GBP = 3 chars)
```

```
VAL-009-03-newDevise-B2
THE SYSTEM SHALL store a non-null currency value
  check   : {kind: required}
  source  : schema
  column : CURRENCY — @Column(nullable=false)
  Note : garanti par la valeur par défaut "EUR" — inatteignable via l'UI
```

---

## Bloc 3 — Matrice de divergence

| Champ | Kind | Verdict | Front | Back | Consequence (script) | Nature |
|---|---|---|---|---|---|---|
| `symboleValeur` | — | `aucune_règle` | — | — | — | — |
| `periode` | — | `aucune_règle` | — | — | — | — |
| `newSymbole` | `required` | `identique` | VAL-009-03-newSymbole-F1 | VAL-009-03-newSymbole-B1 | — | — |
| `newSymbole` | `max_length(20)` | `back_seul` | — | VAL-009-03-newSymbole-B2 | front accepte ce que back devrait rejeter — legacy tronque silencieusement (observé live) | **dette-legacy** |
| `newSymbole` | `unique` | `back_seul` | — | VAL-009-03-newSymbole-B3 | front accepte ce que back rejette — rejet applicatif côté serveur | **métier** |
| `newNom` | `required` | `identique` | VAL-009-03-newNom-F1 | VAL-009-03-newNom-B1 | — | — |
| `newNom` | `max_length(100)` | `back_seul` | — | VAL-009-03-newNom-B2 | front accepte ce que back rejette — PersistenceException (observé live) | **dette-legacy** |
| `newMarche` | `max_length(50)` | `back_seul` | — | VAL-009-03-newMarche-B1 | front accepte ce que back rejette — inféré (champ select, bypass UI requis) : colonne MARKET VARCHAR(50), rejet par contrainte schéma si > 50 | **métier** |
| `newMarche` | `required` | `back_seul` | — | VAL-009-03-newMarche-B2 | front accepte ce que back rejette — inféré (champ select, bypass UI requis) : colonne MARKET NOT NULL, rejet par contrainte schéma si absent | **métier** |
| `newDevise` | `max_length(10)` | `back_seul` | — | VAL-009-03-newDevise-B1 | front accepte ce que back rejette — inféré (champ select, bypass UI requis) : colonne CURRENCY VARCHAR(10), rejet par contrainte schéma si > 10 | **métier** |
| `newDevise` | `required` | `back_seul` | — | VAL-009-03-newDevise-B2 | front accepte ce que back rejette — inféré (champ select, bypass UI requis) : colonne CURRENCY NOT NULL, rejet par contrainte schéma si absent | **métier** |

### Divergences qualifiées

**`newSymbole` / `max_length(20)` / `back_seul` / `dette-legacy`**
Le legacy tronque silencieusement le symbole à 20 caractères au lieu de rejeter la soumission.
La réécriture doit valider en amont et retourner une erreur métier explicite.
Ne pas reproduire le comportement de troncature.

**`newSymbole` / `unique` / `back_seul` / `métier`**
Le contrôle de doublon est exclusivement côté serveur (case-insensitive, `UPPER` SQL).
La réécriture doit reproduire ce contrôle côté serveur avec le même algorithme case-insensitive.

**`newNom` / `max_length(100)` / `back_seul` / `dette-legacy`**
Le legacy expose une `PersistenceException` brute (dialog navigateur) au lieu d'un message métier.
La réécriture doit valider en amont et retourner "Le nom ne peut pas dépasser 100 caractères" (ou équivalent).
Ne pas reproduire l'exposition de l'exception.

**`newMarche` / `max_length(50)` et `required`, `newDevise` / `max_length(10)` et `required` — `back_seul` / `métier`**
Contraintes de schéma jamais déclenchées via l'UI normale (select + default).
La réécriture doit documenter les contraintes dans le schéma cible mais n'a pas besoin de message d'erreur dédié (le select garantit la conformité).

### Avertissement harness (warning script)

> `newSymbole/trop_long_21_chars` : observation legacy `accepted` contraire à l'attendu `rejected` (VAL-009-03-newSymbole-B2)

Tranché : **dette-legacy** — le legacy tronque silencieusement, la règle exprime le comportement attendu (rejet). La réécriture doit rejeter.

---

## Règles non couvertes par le harness

Les règles suivantes ne sont pas citées comme `source_ids` dans un cas `verified` du harness :

| Règle | Raison |
|---|---|
| `VAL-009-03-newSymbole-F1` | Cas vide tranché par B1 (priorité back) — F1 non cité séparément |
| `VAL-009-03-newNom-F1` | Cas vide tranché par B1 (priorité back) — F1 non cité séparément |
| `VAL-009-03-newSymbole-B3` | Unicité — le harness ne peut pas générer un cas avec un symbole existant connu |
| `VAL-009-03-newMarche-B2` | Select avec default — le harness ne génère pas de cas "vide" pour un select |
| `VAL-009-03-newDevise-B2` | Select avec default — idem |

Ces règles restent valides et doivent être testées manuellement ou via retro-golden-master.
