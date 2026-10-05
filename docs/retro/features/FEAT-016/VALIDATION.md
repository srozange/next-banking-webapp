---
feature: FEAT-016
name: Administration — Gestion globale
step: retro-validation
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# Validation — FEAT-016 Administration — Gestion globale

---

## Contrôle d'accès (RBAC)

VAL-016-01-page_access-B1 [Confirmé]
WHEN an unauthenticated user requests admin.xhtml
THE SYSTEM SHALL redirect to login.xhtml.
  Source  : applicatif
  check   : {"kind": "custom"}

VAL-016-01-page_access-B2 [Confirmé]
WHEN a user with role CONSEILLER or CLIENT requests admin.xhtml
THE SYSTEM SHALL redirect to dashboard.xhtml without displaying the administration page.
  Source  : applicatif
  check   : {"kind": "custom"}

VAL-016-01-page_access-B3 [Confirmé]
WHEN a user with role ADMIN requests admin.xhtml
THE SYSTEM SHALL grant access to all administration sections on the page.
  Source  : applicatif
  check   : {"kind": "custom"}

---

## Contexte

La page `admin.xhtml` est un tableau de bord d'administration à rôle ADMIN exclusif.
Elle contient **un seul champ de saisie utilisateur** (`j_idt18:j_idt19` — filtre de recherche clients),
tous les autres éléments étant des boutons d'action (Approuver, Refuser, Desactiver/Activer,
Confirmer alerte fraude, Faux positif) ou des champs en lecture seule (panneau détail client).

Les boutons d'action transmettent des identifiants liés par JSF (loanId, customerId, alertId) —
ce ne sont pas des champs de saisie libre ; aucune validation de données utilisateur ne s'y applique.

---

## Bloc 1 — Validation front (par champ)

### Champ `j_idt18:j_idt19` — Filtre de recherche clients

| Attribut | Valeur |
|---|---|
| Type DOM | `text` |
| Libellé | (sans libellé visible) |
| Obligatoire | Non |
| Maxlength | Non posé |
| Pattern | Non posé |
| Déclencheur | Soumission du formulaire (bouton "Filtrer") |

**Aucune règle front identifiée.** Le champ accepte toute valeur (y compris vide) sans contrainte HTML5.

---

## Bloc 2 — Validation back (schéma + triggers + applicatif)

### Champ `j_idt18:j_idt19` — Filtre de recherche clients

**Source applicative :** méthode `getFilteredClients()`.

Le filtre est appliqué **en mémoire** sur la liste `allCustomers` (déjà chargée en session) :
```
String f = clientFilter != null ? clientFilter.toLowerCase().trim() : "";
return allCustomers.stream()
    .filter(c -> c.getRole() == CustomerRole.CLIENT)
    .filter(c -> f.isEmpty()
            || c.getFullName().toLowerCase().contains(f)
            || c.getUsername().toLowerCase().contains(f)
            || c.getEmail().toLowerCase().contains(f))
    .collect(Collectors.toList());
```

**Aucune écriture en base.** Aucune contrainte schéma ne s'applique à ce champ. Aucun trigger.

**Aucune règle back identifiée.**

### Boutons d'action — contrôles serveur documentés (hors périmètre validation champs)

Ces contrôles sont traités dans `RG.md` (règles RG-016-009 à RG-016-032). Ils ne portent pas
sur des données saisies par l'utilisateur :

| Action | Contrôle serveur | Source |
|---|---|---|
| Approuver prêt | Early return si prêt non EN_ATTENTE ; try/catch → message erreur | |
| Refuser prêt | Early return si prêt non EN_ATTENTE ; try/catch → message erreur | |
| Désactiver/Activer client | Aucune validation (setActive direct) | |
| Confirmer alerte fraude | Guard alertId != null | |
| Faux positif | Guard alertId != null | |

---

## Bloc 3 — Matrice de divergence

### Règles EARS

*Aucune règle de validation identifiée côté front ni côté back pour le champ `j_idt18:j_idt19`.*
*La grille de sondes (test-harness.json) liste 156 cas en statut `probe` — aucun `verified`.*

### Tableau de divergence

| Champ | Verdict | Front | Back | Nature |
|---|---|---|---|---|
| `j_idt18:j_idt19` (filtre clients) | `aucune_règle` | aucune règle | aucune contrainte | — |

**Synthèse :** identique 0 | divergent 0 | front_seul 0 | back_seul 0 | a_interpreter 0 | aucune_règle 1

---

## Notes

- L'écran admin est une interface d'action / consultation, pas de saisie de données métier.
- Le seul champ de saisie (filtre clients) est un filtre in-memory sans persistance.
- Les boutons d'action ne portent aucune donnée utilisateur libre : les identifiants sont liés par JSF.
- Dette-legacy déjà documentée dans RG.md : exposé de `e.getMessage()` (RG-016-014),
  ambiguïté `advisorSince` / "Client depuis" (RG-016-025), alertes CONFIRMED/FALSE_POSITIVE
  invisibles dans l'UI (RG-016-034).
