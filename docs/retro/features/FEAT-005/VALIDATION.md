---
feature: FEAT-005
name: Relevé de compte
step: retro-validation
doc_status: current
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# VALIDATION — FEAT-005 Relevé de compte

_Sources analysées : `ReleveBean.java`, `AccountService.java`, `AuditService.java`, `releve.xhtml`, `DATA-DICTIONARY.md`, `fields-raw.json`._

---

## Champ interactif

FEAT-005 est un écran de consultation. Il contient **un seul champ de saisie** : le sélecteur de compte `releveForm:accountSelect`. Tous les autres éléments (`accountInfo`, colonnes du tableau) sont en lecture seule.

---

## Validation front — `releveForm:accountSelect`

Aucune contrainte de validation HTML5 explicite (pas de `required`, pas de `maxlength`, pas de `pattern`). La liste des options du `h:selectOneMenu` est générée dynamiquement depuis `releveBean.accounts` (comptes du client courant uniquement). La sélection déclenche un appel AJAX `change` vers `releveBean.onAccountChange()`.

```
(aucune règle front explicite pour ce champ)
```

---

## Validation back — `releveForm:accountSelect`

### Règles EARS

VAL-005-01-accountSelect-B1
IF submitted selectedAccountId is not parseable as a Long integer
THEN THE SYSTEM SHALL reject the request.
  Source  : applicatif — JSF type conversion (String → Long) sur `releveBean.selectedAccountId`
  Message : "Conversion Error setting value for 'releveForm:accountSelect'"

VAL-005-01-accountSelect-B2
IF submitted selectedAccountId is not among the authenticated customer's account IDs
THEN THE SYSTEM SHALL reject the selection.
  Source  : applicatif — JSF UISelectOne server-side validation (valeur doit être dans les options rendues)
            + filtre `accounts.stream().filter(a -> a.getId().equals(selectedAccountId))`
  Message : "Validation Error: Value is not valid"
  Note IDOR : si la validation JSF UISelectOne était contournée, le filtre de ReleveBean.onAccountChange()
              constitue une deuxième barrière : le compte doit figurer dans `accounts` (comptes du client courant).
              Les transactions ne sont chargées que si `ifPresent` trouve le compte — protection IDOR
              en profondeur.

---

## Matrice de divergence

_Générée par `build-divergence-matrix.py` — 2 lignes, 2 `back_seul`, 0 `front_seul`, 0 `divergent`, 0 `identique`, 0 `aucune_règle`._

| Champ | Verdict | Front | Back | Conséquence | Nature |
|---|---|---|---|---|---|
| `releveForm:accountSelect` | `back_seul` | — (select implicite) | B1 — format Long | front accepte ce que back rejette — rejet par le serveur (contrôle présent dans le code) | métier — la cible doit valider que la valeur est un identifiant Long avant traitement |
| `releveForm:accountSelect` | `back_seul` | — (select implicite) | B2 — référence comptes client | front accepte ce que back rejette — rejet par le serveur (contrôle présent dans le code) | métier — la cible doit valider que le compte appartient à l'utilisateur authentifié |

**Qualification** :

- `back_seul / format` : Nature **métier**. La contrainte de type (Long) n'est pas posée explicitement côté front car le `h:selectOneMenu` rend directement des `Long` comme options. Elle doit être maintenue côté back dans la réécriture (contrôle de type avant traitement de la sélection de compte).

- `back_seul / reference` : Nature **métier**. La validation UISelectOne JSF + le filtre de ReleveBean constituent une protection IDOR : la cible doit reproduire ce contrôle de propriété. L'absence de règle front explicite est normale pour un select à options dynamiques ; la contrainte métier reste côté back.

---

## Harness de test

_Généré par `generate-test-harness.py` — 5 cas, 0 `verified`, 5 `probe`._

Les règles B1 et B2 figurent dans `uncovered_rule_ids` : le générateur ne peut pas lier statiquement des règles `format: Long` et `reference` aux cas de test génériques d'un select à options dynamiques (les valeurs valides ne sont connues qu'à l'exécution). Aucun cas `verified` automatique n'est possible sans une liste de comptes de test injectée dans le harness.

Les cas `probe` suivants restent à observer manuellement :

| Cas | Input | Comportement attendu |
|---|---|---|
| `vide` | `""` | JSF `required=false` : null accepté, `onAccountChange()` ne s'exécute pas (null guard) |
| `valeur_valide` | (ID valide du client) | accepté — compte chargé, transactions affichées |
| `caractères_spéciaux` | `<script>alert(1)</script>` | rejeté — B1 (conversion Long) |
| `caractères_spéciaux` | `'--` | rejeté — B1 (conversion Long) |
| `caractères_spéciaux` | `\0` | rejeté — B1 (conversion Long) |

---

## Contraintes schéma — tables affichées (lecture seule)

Ces colonnes sont affichées par FEAT-005 mais ne font pas l'objet de saisie.
Documentées ici pour référence de la réécriture.

**TABLE BANK_ACCOUNTS** (panneaux `accountInfo`) :
- `ACCOUNT_NUMBER` : NOT NULL
- `ACCOUNT_TYPE` : NOT NULL (⚠️ enum candidate — à typer en cible)
- `BALANCE` : NOT NULL (affiché formaté EUR 2 décimales)

**TABLE TRANSACTIONS** (colonnes du tableau) :
- `AMOUNT` : NOT NULL
- `BALANCE_AFTER` : nullable — si null, la colonne "Solde après" affiche une valeur vide (JSF `f:convertNumber` sur null → vide)
- `DESCRIPTION` : NOT NULL
- `TRANSACTION_DATE` : NOT NULL
- `TYPE` : NOT NULL (⚠️ enum candidate — valeurs : CREDIT, DEBIT)

---

## Questions métier à trancher

Aucune.

---

## Blocages

Aucun.
