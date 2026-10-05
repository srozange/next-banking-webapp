---
feature: FEAT-003
feature_name: Effectuer un virement
extraction_date: 2026-10-03
skill: retro-validation
sources:
  - src/main/webapp/virement.xhtml
  - src/main/java/com/accenture/bank/web/VirementBean.java
  - src/main/java/com/accenture/bank/ejb/TransferService.java
  - src/main/java/com/accenture/bank/entity/Transfer.java
  - retro/00-global/DATA-DICTIONARY.md
  - retro/features/FEAT-003/fields-raw.json (live DOM: MCP Playwright, rôle CLIENT, user1)
---

# VALIDATION — FEAT-003 Effectuer un virement

Champs couverts : 4 (`compteSource`, `compteDestinataire`, `montant`, `motif`).  
Formulaire unique : `/virement.xhtml` — un seul écran, pas d'étapes.

**Note retro-ui ↔ code** : `fields-raw.json` déclare `motif.required=false` et `motif.maxlength=null`.
Le code source a `required="true" maxlength="100"` sur le champ `motif`.
JSF `required="true"` ne génère pas d'attribut HTML5 `required`, ce qui explique l'absence dans le DOM
capturé par retro-ui. La valeur du code fait foi — les règles F1 et F2 de `motif` sont réelles.

---

## 1. Règles front (EARS)

Toutes les validations front sont déclenchées par la **soumission** du formulaire (cycle de vie JSF —
phase Process Validations / Apply Request Values), sauf `motif-F2` (HTML `maxlength` → **frappe**).

### compteSource

```
VAL-003-01-compteSource-F1
WHEN user submits without selecting a source account THE SYSTEM SHALL reject the transfer
and display a required validation error.
  Trigger  : soumission (JSF)
  Message  : "virementForm:source: Validation Error: Value is required."
  check    : {kind: "required"}
  observed : {outcome: "rejected"} — vérifié live (soumission vide, rôle CLIENT)
```

### compteDestinataire

```
VAL-003-01-compteDestinataire-F1
WHEN user submits without selecting a target account THE SYSTEM SHALL reject the transfer
and display a required validation error.
  Trigger  : soumission (JSF)
  Message  : "virementForm:target: Validation Error: Value is required."
  check    : {kind: "required"}
  observed : {outcome: "rejected"} — vérifié live (soumission vide, rôle CLIENT)
```

### montant

```
VAL-003-01-montant-F1
WHEN user submits with empty Montant field THE SYSTEM SHALL reject the transfer
and display a required validation error.
  Trigger  : soumission (JSF)
  Message  : "virementForm:amount: Validation Error: Value is required."
  check    : {kind: "required"}
  observed : {outcome: "rejected"} — vérifié live (soumission vide, rôle CLIENT)

VAL-003-01-montant-F2
WHEN user submits a non-numeric value in the Montant field THE SYSTEM SHALL reject
the submission with a JSF conversion error.
  Trigger  : soumission (JSF)
  Message  : JSF default conversion error (non documenté dans les sources)
  check    : {kind: "format", value: "decimal"}
  observed : {outcome: "rejected"} — dérivé du code (NPE côté serveur si bypass JSF →
             catch(Exception) → "Erreur lors du virement : ..." ; la contrainte front est la bonne
             UX, le rejet back est accidentel — voir Nature dette-legacy dans la matrice)
```

### motif

```
VAL-003-01-motif-F1
WHEN user submits with empty Motif field THE SYSTEM SHALL reject the transfer
and display a required validation error.
             (JSF required ne génère pas d'attribut HTML5 required — absent du DOM capturé)
  Trigger  : soumission (JSF)
  Message  : "virementForm:motif: Validation Error: Value is required."
  check    : {kind: "required"}
  observed : {outcome: "accepted"} — dérivé du code (bypass JSF → TransferService accepte motif
             null car Transfer.motif est @Column sans nullable=false)
  Note     : RG-003-020 cite "si motif n'est pas vide" — la logique métier accepte un motif null ;
             la contrainte front est probablement une erreur de développement (voir Nature dette-legacy)

VAL-003-01-motif-F2
WHEN the Motif field contains more than 100 characters THE SYSTEM SHALL prevent further
input beyond the 100-character limit.
  Trigger  : frappe (attribut HTML maxlength — le navigateur bloque la saisie)
  Message  : (aucun — le navigateur empêche silencieusement)
  check    : {kind: "max_length", value: 100}
  observed : {outcome: "accepted"} — dérivé du code (bypass → Transfer.@Column sans length →
             Hibernate default VARCHAR(255) → serveur accepte jusqu'à 255 chars)
```

---

## 2. Règles back (EARS)

Sources : code applicatif (`VirementBean.java`, `TransferService.java`) + schéma (`DATA-DICTIONARY.md` +
entité `Transfer.java`). Aucun trigger ni procédure stockée identifié pour FEAT-003.

### compteSource

```
VAL-003-01-compteSource-B1
IF the source account referenced by sourceAccountId does not exist in the database
THEN THE SYSTEM SHALL reject the transfer.
  Source   : applicatif
  check    : {kind: "reference"}

VAL-003-01-compteSource-B2
THE SYSTEM SHALL store the source account reference as a non-null foreign key.
  Source   : schema
  check    : {kind: "required"}
```

### compteDestinataire

```
VAL-003-01-compteDestinataire-B1
IF the target account referenced by targetAccountId does not exist in the database
THEN THE SYSTEM SHALL reject the transfer.
  Source   : applicatif
  check    : {kind: "reference"}

VAL-003-01-compteDestinataire-B2
THE SYSTEM SHALL store the target account reference as a non-null foreign key.
  Source   : schema
  check    : {kind: "required"}

VAL-003-01-compteDestinataire-B3
IF the target account identifier equals the source account identifier THEN THE SYSTEM SHALL
reject the transfer.
  Source   : applicatif
  Message  : "Le compte source et destination doivent être différents"
  check    : {kind: "custom"}
  observed : {outcome: "rejected"} — confirmé FIELDS.md (crawl Playwright rôle CLIENT)
```

### montant

```
VAL-003-01-montant-B1
THE SYSTEM SHALL store the transfer amount as a non-null numeric value.
  Source   : schema
  check    : {kind: "required"}

VAL-003-01-montant-B2
IF the transfer amount is less than or equal to zero THEN THE SYSTEM SHALL reject the transfer.
  Source   : applicatif
  check    : {kind: "custom"}
  Note     : exception EJB-wrappée → catch(Exception) → message brut exposé (dette-legacy)

VAL-003-01-montant-B3
IF the source account balance is less than the requested transfer amount THEN THE SYSTEM SHALL
reject the transfer.
  Source   : applicatif
  Message  : "Erreur lors du virement : java.lang.IllegalStateException: Solde insuffisant"
  check    : {kind: "custom"}
  observed : {outcome: "rejected"} — confirmé FIELDS.md (crawl Playwright rôle CLIENT, montant 9999 > solde 2130)
  Note : catch(IllegalStateException) dans est mort (EJB CMT enveloppe dans
             EJBException) ; catch(Exception) effectif expose "java.lang.IllegalStateException: ..." —
             dette-legacy (message technique exposé)
```

### motif

Aucune contrainte back identifiée : `Transfer.motif` est `@Column` sans `nullable=false` ni `length`
(Hibernate default VARCHAR(255)) ; pas de contrôle applicatif sur la valeur du motif dans
`TransferService.java`. Le champ motif est nullable en base.

---

## 3. Matrice de divergence

Générée par `build-divergence-matrix.py` depuis `validation-front.json` + `validation-back.json`.

| Champ | Kind | Verdict | Front | Back | Conséquence | Nature |
|---|---|---|---|---|---|---|
| `compteSource` | required | **identique** | VAL-003-01-compteSource-F1 | VAL-003-01-compteSource-B2 | — | — |
| `compteSource` | reference | **back_seul** | — | VAL-003-01-compteSource-B1 | Front accepte ce que back rejette (contrôle présent dans le code) | métier |
| `compteDestinataire` | required | **identique** | VAL-003-01-compteDestinataire-F1 | VAL-003-01-compteDestinataire-B2 | — | — |
| `compteDestinataire` | reference | **back_seul** | — | VAL-003-01-compteDestinataire-B1 | Front accepte ce que back rejette (contrôle présent dans le code) | métier |
| `compteDestinataire` | custom | **back_seul** ¹ | — | VAL-003-01-compteDestinataire-B3 | Le formulaire JSF permet de sélectionner le même compte dans les deux combobox ; seul le serveur rejette | métier |
| `montant` | required | **identique** | VAL-003-01-montant-F1 | VAL-003-01-montant-B1 | — | — |
| `montant` | format | **front_seul** | VAL-003-01-montant-F2 | — | Si JSF contourné : null amount → NPE → EJBException → message brut "Erreur lors du virement : ..." | dette-legacy |
| `montant` | custom | **back_seul** ¹ | — | VAL-003-01-montant-B2, VAL-003-01-montant-B3 | Aucun contrôle front pour montant ≤ 0 ni solde insuffisant ; seul le serveur rejette | métier |
| `motif` | required | **front_seul** | VAL-003-01-motif-F1 | — | Bypass JSF : le serveur accepte un motif null (Transfer.motif nullable) — données invalides possibles | dette-legacy |
| `motif` | max_length | **front_seul** | VAL-003-01-motif-F2 | — | Bypass HTML : le serveur accepte jusqu'à 255 chars (Hibernate default VARCHAR) | métier |

¹ Ligne `a_interpreter` dans `divergence-matrix.json` (contrainte `custom`) — tranchée ici :
- `compteDestinataire | custom` → **back_seul** ; Nature : métier (contrainte "source ≠ cible" à reproduire dans la cible, côté serveur)
- `montant | custom` → **back_seul** (deux contraintes B2 + B3 regroupées) ; Nature : métier (montant > 0 et solde suffisant à reproduire dans la cible) + dette-legacy sur le message d'erreur brut exposé

### Qualifications Nature

| Champ | Verdict | Nature | Détail |
|---|---|---|---|
| `compteSource` | back_seul (reference) | métier | Contrôle FK à reproduire |
| `compteDestinataire` | back_seul (reference) | métier | Contrôle FK à reproduire |
| `compteDestinataire` | back_seul (custom) | métier | Contrainte source ≠ cible à reproduire |
| `montant` | front_seul (format) | dette-legacy | Le back rejette accidentellement (NPE) au lieu d'une validation explicite ; la cible doit valider le format côté serveur proprement |
| `montant` | back_seul (custom) | métier | Montant > 0 et solde suffisant à reproduire ; +dette-legacy : messages d'erreur bruts à masquer |
| `motif` | front_seul (required) | dette-legacy | `required="true"` JSF probablement erroné (RG-003-020 accepte motif null) ; la cible ne doit pas enforcer required sur motif |
| `motif` | front_seul (max_length) | métier | Borne 100 à aligner côté serveur dans la cible (max_length=100 partout) |

### Warnings harness

Deux warnings générés par le script de l'Étape 5 :

1. **`motif/vide` : observation legacy `accepted` contraire à l'attendu `rejected` (VAL-003-01-motif-F1)**
   Résolution : l'attendu `rejected` est fondé sur la règle front F1 (JSF rejects). L'observation
   `accepted` est le comportement serveur quand JSF est contourné. C'est la divergence `front_seul`
   documentée dans la matrice (Nature : dette-legacy). La cible doit décider si elle enforce required
   sur motif côté serveur ; le legacy ne le fait pas.

2. **`motif/trop_long_101_chars` : observation legacy `accepted` contraire à l'attendu `rejected` (VAL-003-01-motif-F2)**
   Résolution : la règle front F2 (`max_length=100`) est fondée sur l'attribut HTML. Le serveur accepte
   jusqu'à 255 chars (Hibernate default). La cible doit aligner la contrainte sur 100 chars des deux
   côtés (Nature : métier).

### Règles non couvertes par le harness

Les règles suivantes n'ont pas de cas de test généré automatiquement depuis `fields-raw.json`
(champs combobox sans cas de valeur invalide ni hors-borne générable mécaniquement) :

- `VAL-003-01-compteSource-B1`, `B2` — cas "compte inexistant" impossible à générer depuis la liste d'options dynamique
- `VAL-003-01-compteDestinataire-B1`, `B2`, `B3` — idem + cas "même compte" à tester manuellement
- `VAL-003-01-montant-B1`, `B2`, `B3` — cas "montant ≤ 0" et "solde insuffisant" à ajouter manuellement
- `VAL-003-01-montant-F2` — cas "valeur non numérique" absent du harness généré

Ces cas font partie du harness comme probes ouvertes (status: probe) ou comme `uncovered_rule_ids`.

---

## Questions métier

Aucune question `ASK USER` — tous les comportements sont déterminables depuis le code au commit analysé.
