# VALIDATION — FEAT-002 Tableau de bord

_Généré par retro-validation — 2026-10-03_
_Source : DashboardBean.java, dashboard.xhtml, fields-raw.json (fields=[]), DATA-DICTIONARY.md_

---

## Contexte

Page purement affichage (lecture seule) sans formulaire de saisie.
`fields-raw.json` contient `"fields": []` — aucun champ de saisie n'a été détecté par retro-ui.

La page contient deux boutons d'action dans la section Centre de notifications :
- **Tout marquer lu** (`dashboardBean.markAllRead`) — rendu conditionnel si `unreadCount > 0`
- **Effacer tout** (`dashboardBean.clearNotifications`) — rendu conditionnel si `notifications` non vide

Ces boutons n'acceptent aucune valeur de saisie : ils sont hors périmètre de la validation de champs.

---

## Validation front (par champ)

Aucun champ de saisie. Aucune règle front.

| Champ | Règles front |
|---|---|
| — | Aucune (page affichage) |

---

## Validation back (schéma + triggers + applicatif)

Aucune contrainte de validation sur les données soumises par l'utilisateur (page lecture seule).

Règles serveur constatées dans le code (display-only, non comparées dans la matrice) :

| Élément | Règle serveur | Source | Preuve |
|---|---|---|---|
| `solde_total` | Retourne `BigDecimal.ZERO` si `accounts == null` — pas d'exception levée | applicatif | |
| `dernieres_operations` | Charge les 5 dernières transactions du **premier** compte uniquement (`accounts.get(0)`) — pas du compte courant explicitement | applicatif | |
| `notifications_jms` | Retourne liste vide si `currentCustomer == null` | applicatif | |
| Bouton "Tout marquer lu" | Rendu JSF conditionnel si `unreadCount > 0` — aucune vérification côté serveur dans `markAllRead()` | applicatif (front-only guard) | |
| Bouton "Effacer tout" | Rendu JSF conditionnel si `notifications` non vide — aucune vérification côté serveur dans `clearNotifications()` | applicatif (front-only guard) | |

### Règles EARS — Display (informatif, hors matrice de divergence)

```
VAL-002-01-solde_total-B1
THE SYSTEM SHALL return 0 (EUR0.00) as total balance when the authenticated user has no bank accounts.
  check   : {kind: "custom"}

VAL-002-01-dernieres_operations-B1
THE SYSTEM SHALL display at most 5 transactions from the first account in the user's account list.
  check   : {kind: "custom"}

VAL-002-01-notifications_jms-B1
THE SYSTEM SHALL return an empty notification list when no authenticated customer session exists.
  check   : {kind: "custom"}
```

---

## Matrice de divergence

Aucun champ de saisie — matrice vide.

| Champ | Verdict | Front | Back | Nature |
|---|---|---|---|---|
| — | aucune_règle | — | — | — |

_`divergence-matrix.json` : total_rows=0, by_verdict={}_

---

## Observations supplémentaires

### Boutons d'action sans champ de saisie

Les deux boutons d'action (markAllRead, clearNotifications) présentent un écart front/back :
le rendu JSF conditionnel empêche l'affichage du bouton quand la condition n'est pas remplie,
mais le code serveur n'effectue aucune vérification de la même condition.
Un contournement direct de l'écran (POST direct) invoquerait ces méthodes sans garde serveur.

Ces boutons ne comportent aucun champ de saisie et sont hors périmètre de la validation de champs.
La question de sécurité (contournement POST) relève de retro-permissions (FEAT-030 RBAC).

**Nature : dette-legacy** — les contrôles de pré-conditions manquent côté serveur.
