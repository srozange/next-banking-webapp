---
feature: FEAT-017
feature_name: Téléchargement documents PDF
validation_date: 2026-10-03
interface: API
endpoint: "GET /docs/download"
sources:
  - src/main/java/com/accenture/bank/web/DocumentDownloadServlet.java
  - src/main/java/com/accenture/bank/ejb/DocumentService.java
---

# VALIDATION — FEAT-017 Téléchargement documents PDF

_Interface : API — `GET /docs/download`_
_Validation front : sans objet (pas d'écran)_
_Divergence matrix : sans objet (API)_
_Test harness : sans objet (API)_

---

## Contrôles serveur des paramètres

| Paramètre | Requis pour | Validation code | Source | Comportement observé | Nature |
|---|---|---|---|---|---|
| session | tous les types | `currentCustomer` absent → redirect vers `/login.xhtml` | applicatif | HTTP 302 (redirect confirmé live) | métier |
| `type` | tous | absent → HTTP 400 "Parametre type manquant" | applicatif | HTTP 400 (confirmé live) | métier |
| `type` | tous | valeur hors {RELEVE, RIB, ATTESTATION, IFU} (insensible à la casse) → HTTP 400 | applicatif | HTTP 400 (confirmé live) | métier |
| `accountId` | RELEVE, RIB, ATTESTATION | absent ou vide → HTTP 400 "accountId manquant" | applicatif | HTTP 400 (confirmé live) | métier |
| `accountId` | RELEVE, RIB, ATTESTATION | non entier → HTTP 400 (parseLong retourne null) | applicatif | HTTP 400 (confirmé live) | métier |
| `accountId` | RELEVE, RIB, ATTESTATION | compte inexistant → HTTP 404 (intent code), HTTP 500 (live) | applicatif | HTTP 500 — EJBException enveloppe IllegalArgumentException ; catch(IllegalArgumentException) dans la servlet ne l'attrape jamais | dette-legacy |
| `year` | RELEVE, IFU | absent ou non numérique → parseInt retourne 0 silencieusement, jamais rejeté | applicatif | IFU : HTTP 200 (year=0 accepté, IFU vide généré) ; RELEVE : non observable (bug lazy-loading indépendant) | dette-legacy |
| `month` | RELEVE | absent ou non numérique → parseInt retourne 0, moisLibelle clamp → "Janvier" | applicatif | non observable (RELEVE systématiquement HTTP 500 — voir note ci-dessous) | dette-legacy |

> **Note — Défaut bloquant RELEVE / RIB / ATTESTATION** : Les méthodes `genererReleve`, `genererRIB` et `genererAttestationSolde` portent `@TransactionAttribute(SUPPORTS)`. Appelées depuis la servlet sans transaction JTA englobante, elles s'exécutent sans contexte transactionnel actif. L'accès à `account.getCustomer()` (association JPA lazy non initialisée) déclenche une `LazyInitializationException`, capturée en `RuntimeException` ( / /), puis enveloppée dans `EJBException` par WildFly 14 — résultat : HTTP 500 systématique pour tout appel RELEVE, RIB ou ATTESTATION, quels que soient les paramètres. Seul IFU fonctionne (`genererIFU` charge `Customer` directement via `em.find`, sans navigation lazy). Source : journaux serveur (`retro/runtime/app.log`). Ce défaut empêche toute observation live de génération normale pour ces trois types.

---

## Règles EARS — Contrôles serveur

### Session

```
VAL-017-01-session-B1
IF request HTTP session does not contain a valid `currentCustomer` attribute
THEN THE SYSTEM SHALL redirect the client to the login page.
  Source : applicatif
  Observed: {"outcome": "rejected", "http_status": 302}
```

---

### Paramètre `type`

```
VAL-017-02-type-B1
IF the `type` query parameter is absent from the request
THEN THE SYSTEM SHALL reject the request.
  Source : applicatif
  Message : "Parametre type manquant"
  Observed: {"outcome": "rejected", "http_status": 400}
```

```
VAL-017-02-type-B2
IF the `type` query parameter value (case-insensitive) is not one of RELEVE, RIB, ATTESTATION or IFU
THEN THE SYSTEM SHALL reject the request.
  Source : applicatif (toUpperCase() + switch/default)
  Message : "Type inconnu : {type}"
  Observed: {"outcome": "rejected", "http_status": 400}
```

---

### Paramètre `accountId`

```
VAL-017-03-accountId-B1
IF type is RELEVE, RIB or ATTESTATION and the `accountId` parameter is absent or empty
THEN THE SYSTEM SHALL reject the request.
  Source : applicatif (parseLong retourne null)
  Message : "accountId manquant"
  Observed: {"outcome": "rejected", "http_status": 400}
```

```
VAL-017-03-accountId-B2
IF type is RELEVE, RIB or ATTESTATION and the `accountId` parameter is not a valid integer
THEN THE SYSTEM SHALL reject the request.
  Source : applicatif (parseLong retourne null pour non-numérique)
  Message : "accountId manquant"
  Observed: {"outcome": "rejected", "http_status": 400}
```

```
VAL-017-03-accountId-B3 [Suspect]
IF `accountId` does not reference an existing BankAccount
THEN THE SYSTEM SHALL reject the request.
  Source : applicatif (IllegalArgumentException) ;
            (catch → sendError 404)
  Observed: {"outcome": "rejected", "http_status": 500}
            À reproduire en réécriture comme HTTP 404 (comportement intent).
```

---

### Paramètre `year`

```
VAL-017-04-year-B1 [Suspect]
IF type is RELEVE or IFU and the `year` parameter is absent or non-numeric
THEN THE SYSTEM SHALL reject the request.
  Source : applicatif (parseInt retourne 0 silencieusement)
  Observed: {"outcome": "accepted", "http_status": 200}
            (cas IFU : year=0 → IFU de l'année zéro avec totalInterets=0,00 EUR renvoyé ;
             cas RELEVE : non observable — défaut lazy-loading indépendant retourne 500)
```

---

### Paramètre `month`

```
VAL-017-05-month-B1 [Suspect]
IF type is RELEVE and the `month` parameter is absent or non-numeric
THEN THE SYSTEM SHALL reject the request.
  Source : applicatif (parseInt retourne 0 ;
            moisLibelle clamp : Math.max(0, Math.min(11, month-1)) → index 0 = "Janvier")
  Observed: non observable (RELEVE systématiquement HTTP 500 — défaut @TransactionAttribute(SUPPORTS))
```

---

## Lignes a_interpreter (contraintes `custom`)

| Identifiant | Champ | Interprétation |
|---|---|---|
| VAL-017-01-session-B1 | session | Contrôle de session applicatif non capturé par AppFilter (*.xhtml only) ; servlet autonome. À reproduire côté API comme un Bearer JWT ou cookie de session validé par un filtre dédié. |
| VAL-017-04-year-B1 | year | Pas de contrainte de format/plage en code legacy ; à définir en réécriture : entier 4 chiffres, plage raisonnable (ex. 2000–2100). |
| VAL-017-05-month-B1 | month | Pas de contrainte de plage en code legacy ; à définir en réécriture : entier 1–12. |
