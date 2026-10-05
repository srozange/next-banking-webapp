---
feature: FEAT-011
name: Catalogue produits bancaires
step: retro-validation
generated_at: 2026-10-03
---

# VALIDATION — FEAT-011 « Catalogue produits bancaires »

## Synthèse

Page 100 % lecture seule : aucun champ de saisie, aucune soumission de données utilisateur.  
Le bouton Rafraîchir déclenche un rechargement interne (`batchAdminBean.rafraichir()`) sans transporter de paramètre métier — il n'est pas une mutation.

Les étapes 1 (grille de sondes), 4 (matrice de divergence) et 5 (réconciliation harness) s'exécutent normalement mais produisent des structures vides ; les fichiers `test-harness.json`, `validation-front.json`, `validation-back.json` et `divergence-matrix.json` sont générés avec des tableaux vides. Aucune règle EARS de validation front/back à écrire : aucune donnée utilisateur n'est reçue par le serveur.

---

## Contrôle d'accès (RBAC)

VAL-011-01-page_access-B1 [Confirmé]
WHEN an unauthenticated user requests produits.xhtml
THE SYSTEM SHALL redirect to login.xhtml.
  Source  : applicatif
  check   : {"kind": "custom"}

VAL-011-01-page_access-B2 [Confirmé]
WHEN a user with role CLIENT, CONSEILLER, or ADMIN requests produits.xhtml
THE SYSTEM SHALL grant access to the product catalogue page.
  Source  : applicatif
  check   : {"kind": "custom"}

---

## Étape 1 — Validation front (par champ)

Aucun champ de saisie (`fields-raw.json : fields = []`, confirmé par `produits.xhtml` — RG-011-002).

---

## Étape 2 — Validation back (contrôles serveur)

Aucun paramètre soumis par l'utilisateur depuis cet écran. Les tables ci-dessous sont lues, non écrites, par cette feature.

### Observations schéma (tables lues)

| Table | Colonne | NOT NULL | Contrainte DB | Rôle dans la feature |
|---|---|---|---|---|
| BANK_PRODUCTS | ACTIVE | OUI | — | Filtre WHERE p.active = true (RG-011-005) |
| BANK_PRODUCTS | NAME | OUI | — | Tri ORDER BY p.name (RG-011-006) |
| BANK_PRODUCTS | PRODUCT_TYPE | OUI | — | Section épargne / crédit / investissement (RG-011-007) |
| BATCH_JOB_LOGS | JOB_NAME | OUI | — | Affiché dans la colonne « Job » |
| BATCH_JOB_LOGS | STATUS | NON | Aucun CHECK | Valeurs contraintes par convention code (voir note) |

**Note — STATUS (BATCH_JOB_LOGS)** : les valeurs attendues (SUCCESS, FAILED, PARTIAL) sont documentées dans un commentaire de code uniquement. Aucun `CHECK` schéma ne l'impose. Cette absence de contrainte formelle est une dette-legacy : la réécriture doit introduire un enum ou un `CHECK` explicite.

---

## Étape 3 — Vérifications live

Aucune — conditions (a), (b), (c) de la méthode ne s'appliquent : aucun champ front sans contrôle back, aucune contrainte back à observer depuis l'UI, aucun déclencheur de message introuvable.  
Section batch vide au moment du crawl (les EJB `@Schedule` s'exécutent à 02h00 / 02h30 / 1er du mois 03h00) : le statut d'un job réel (SUCCESS / FAILED / PARTIAL) n'est pas observable en test unitaire.

---

## Étape 4 — Matrice de divergence

Aucune divergence front / back — aucun champ de saisie.

| Champ | Verdict | Front | Back | Nature |
|---|---|---|---|---|
| — | aucune_règle | — | — | — |

---

## Étape 4b — Qualification

Aucune divergence à qualifier.

---

## Étape 5 — Harness réconcilié

0 cas (aucun champ de saisie). Fichiers générés à titre de cohérence de pipeline :
- `test-harness.json` : 0 cas
- `divergence-matrix.json` : 0 ligne

---

## Dette technique legacy (pour information, hors scope réécriture)

- `interest_rate` / `expected_return` [dette-legacy] : valeur stockée en fraction décimale (ex. 0,0300 ≙ 3 %) affichée avec suffixe « % » brut sans multiplication ×100 — l'affichage retourne 100× trop petit (ex. « 0,0300 % » au lieu de « 3,00 % »). Source : `133, 190–192, 251–253`. Ne pas reproduire dans la réécriture — corriger en stockant la valeur en pourcentage entier ou en appliquant `f:convertNumber type="percent"` (RG-011-010).

- `BATCH_JOB_LOGS.STATUS` [dette-legacy] : absence de contrainte `CHECK` en base — les valeurs admissibles sont définies uniquement par convention dans. Une valeur hors plage peut être persistée sans erreur. À corriger dans la réécriture par un enum typé ou une contrainte `CHECK IN ('SUCCESS', 'FAILED', 'PARTIAL')`.

---

## Blocages

- aucun
