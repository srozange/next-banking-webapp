---
feature: FEAT-010
feature_name: "Chat avec le conseiller"
generated_by: retro-validation
date: 2026-10-03
---

# VALIDATION — FEAT-010 « Chat avec le conseiller »

_Sources : `ConseillerBean.java`, `ConseillerService.java`, `conseiller.xhtml`,
`fields-raw.json`, `DATA-DICTIONARY.md` — commit c05512e._

---

## Champs validés

Un seul champ de formulaire : `chatForm:userInput` (textarea libre, saisie du message utilisateur).
Aucune persistance en base — la conversation est stockée en mémoire (`@SessionScoped`).

---

## Étape 2 — Contrôles du serveur

### `chatForm:userInput`

**Source applicatif :**

Seul contrôle présent dans le code :
`if (userInput == null || userInput.trim().isEmpty()) return;`

Le serveur effectue un retour anticipé silencieux (aucun message d'erreur, aucune mise à jour
de la conversation) lorsque l'entrée est nulle ou vide après trim.

Pas d'annotation Bean Validation (`@NotNull`, `@Size`).
Pas de contrainte schéma DB (la conversation n'est pas persistée).
Pas de trigger.

Contenu non sanitisé avant transmission à l'API Azure OpenAI — aucune injection SQL possible
(aucune requête SQL sur ce champ). Le rendu JSF (`#{msg.content}` en template text Facelets)
auto-échappe le HTML via le ResponseWriter → protection XSS en lecture.

---

## Étape 3b — Règles de validation en format EARS

### Règles front — `chatForm:userInput`

Aucune règle front identifiée : `required` absent en HTML5, pas de `maxlength`, pas de `pattern`,
aucun listener `blur`/`focusout`, aucun attribut de validation HTML5. Le bouton « Envoyer »
reste actif même avec le champ vide.

### Règles back — `chatForm:userInput`

VAL-010-01-userInput-B1 [Confirmé]
IF userInput is null or blank THEN THE SYSTEM SHALL reject the send action.
  Source  : applicatif
  check   : {"kind": "required"}
  message : null (aucun message d'erreur affiché — retour silencieux)

**Note dette-legacy** : le retour silencieux sans feedback utilisateur est `dette-legacy` — la
cible devrait désactiver le bouton « Envoyer » tant que le champ est vide, ou afficher un
message de validation.

---

## Étape 4 — Matrice de divergence

| Champ | Verdict | Front | Back | Consequence | Nature |
|---|---|---|---|---|---|
| `chatForm:userInput` | `back_seul` | — (aucune règle HTML5) | `VAL-010-01-userInput-B1` — rejet silencieux si vide (applicatif) | front accepte ce que back rejette — rejet par le serveur (contrôle présent dans le code) | dette-legacy — le front devrait signaler que le champ est obligatoire (bouton désactivé ou message) ; le back, lui, applique correctement la contrainte |

**Détail des verdicts :** back_seul : 1 | identique : 0 | divergent : 0 | front_seul : 0 | a_interpreter : 0 | aucune_règle : 0

---

## Étape 4b — Qualification de la divergence

### `chatForm:userInput` / `back_seul` / `required`

Une réécriture propre reproduirait la règle métier (pas d'envoi vide) — c'est `métier`.
Mais le mismatch front/back (front sans contrainte, back avec retour silencieux) est un
défaut d'implémentation UX : `dette-legacy` pour la partie manque de feedback.

**Décision de tri** : la divergence est annotée `Nature : dette-legacy` au niveau de la
matrice, car le défaut à corriger est l'absence de validation côté front — pas la règle métier
elle-même qui, elle, sera reproduite dans la cible. La contrainte `VAL-010-01-userInput-B1`
est `Nature : métier` en tant que règle isolée.

Aucune question `ASK USER` — aucun motif légitime : le comportement est entièrement lisible
dans le code (source `applicatif`), sans chemin contradictoire ni dépendance externe.

---

## Cas particuliers documentés depuis le code (hors matrice front/back)

### Rendu XSS

`<div class="bubble">#{msg.content}</div>` — EL en template text
Facelets → ResponseWriter auto-échappe les entités HTML. Entrée `<script>alert(1)</script>`
rendue comme `&lt;script&gt;...` — aucune exécution côté client.
Statut harness : `probe` (aucune règle de rejet sur ce champ pour les caractères spéciaux).

### Injection SQL

Le champ n'est pas utilisé dans une requête SQL (conversation en mémoire uniquement). Toute
valeur non vide est transmise en JSON à l'API Azure OpenAI via `javax.json.Json.createObjectBuilder`.
Statut harness : `probe`.

### Longueur maximale

Aucune contrainte de longueur dans le code (`maxlength` absent côté HTML5, aucun `@Size` côté
bean, aucun VARCHAR sur ce champ en base). L'API Azure OpenAI peut rejeter des messages trop
longs, mais ce n'est pas documenté dans le code. Statut harness : `probe`.

---

## Étape 5 — Harness réconcilié

| Champ | Scénario | Statut | Outcome attendu | Source |
|---|---|---|---|---|
| `chatForm:userInput` | vide | `verified` | `rejected` | `VAL-010-01-userInput-B1` |
| `chatForm:userInput` | valeur_valide | `probe` | — | à observer |
| `chatForm:userInput` | caractères_spéciaux `<script>` | `probe` | — | à observer |
| `chatForm:userInput` | caractères_spéciaux `'--` | `probe` | — | à observer |
| `chatForm:userInput` | caractères_spéciaux `\0` | `probe` | — | à observer |
| `chatForm:userInput` | injection_sql | `probe` | — | à observer |

Règles non couvertes : aucune.
Avertissements harness : aucun.

---

## Validation live exécutée

Aucun essai live requis pour cette feature : la seule contrainte back (`VAL-010-01-userInput-B1`)
est de source `applicatif` (visible dans), et la réponse Azure OpenAI
(HTTP 401 en local) est documentée depuis le crawl retro-ui (SCR-010-02_erreur.png, RG-010-020).
Aucune écriture journalisée.
