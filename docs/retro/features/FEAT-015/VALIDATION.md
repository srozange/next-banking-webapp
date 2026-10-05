---
feature: FEAT-015
name: "Espace Conseiller — Portefeuille clients"
step: retro-validation
generated: 2026-10-03
---

# VALIDATION — FEAT-015 Espace Conseiller — Portefeuille clients

> **Page en lecture seule.** Aucun champ de saisie HTML (input/select/textarea).
> Tous les éléments sont des données affichées. Il n'existe aucune validation de formulaire
> front ni de contrainte de schéma à tester par soumission. Les seuls contrôles serveur
> sont le contrôle d'accès (AppFilter) et le filtrage du portefeuille (AccountService).

---

## 1. Validation front — par champ

**Aucune règle front.** La page est intégralement en lecture seule. Les 23 champs d'affichage
(KPIs, cartes clients, panneau de détail) n'ont aucun `<input>`, `<select>` ni `<textarea>` :
aucune règle front ne peut être définie. Tous apparaissent avec verdict `aucune_règle` dans
la matrice de divergence.

---

## 2. Validation back — contrôles serveur

### 2.1 Contrôle d'accès (applicatif — AppFilter.java)

VAL-015-01-page_access-B1
IF an unauthenticated request is received for /conseiller-workspace.xhtml THEN THE SYSTEM SHALL redirect to /login.xhtml
  Source   : applicatif
  Observed : {"outcome": "rejected"} — vérifié targeted-har.py (HTTP 302 → login.xhtml), RG-015-001

VAL-015-01-page_access-B2
IF a request authenticated with role CLIENT is received for /conseiller-workspace.xhtml THEN THE SYSTEM SHALL redirect silently to /dashboard.xhtml without displaying an error message
  Source   : applicatif
  Observed : {"outcome": "rejected"} — vérifié live (CLIENT user1 → dashboard.xhtml, aucun message d'erreur)

VAL-015-01-page_access-B3
IF a request authenticated with role CONSEILLER or ADMIN is received for /conseiller-workspace.xhtml THEN THE SYSTEM SHALL display the portfolio page
  Source   : applicatif
  Observed : {"outcome": "accepted"} — vérifié live (CONSEILLER Sophie Durand → page chargée, 2 clients ; ADMIN Alice Martin → page chargée, 0 clients)

### 2.2 Filtrage du portefeuille (applicatif — AccountService.java)

VAL-015-01-page_access-B4
THE SYSTEM SHALL filter the portfolio to include only clients whose advisor field references the authenticated user's customer record
  Source   : applicatif
  Observed : {"outcome": "accepted"} — vérifié live (Sophie Durand voit 2 clients ; Alice Martin voit 0 clients, portefeuille vide)

### 2.3 Contraintes de schéma

Les contraintes schéma (NOT NULL, FK, CHECK) portent sur les tables CUSTOMERS, BANK_ACCOUNTS,
LOANS lues par cette feature, mais elle n'écrit rien : elles ne sont pas reportées ici
(déjà couvertes dans DATA-DICTIONARY.md — règle methodology).

---

## 3. Matrice de divergence

Script : build-divergence-matrix.py
Résultat : total_rows=24 — a_interpreter=1, aucune_règle=23

### Ligne a_interpreter — page_access (4 contraintes custom)

| Champ | Kind | Verdict | Front | Back | Nature | Conséquence |
|---|---|---|---|---|---|---|
| `page_access` | custom | `a_interpreter` | — | B1 (unauth→login), B2 (CLIENT→dashboard), B3 (CONSEILLER/ADMIN→portfolio), B4 (advisor filter) | **métier** | Contrôle d'accès RBAC + filtrage métier — à reproduire dans la cible |

**Qualification Étape 4b :**
Les 4 contraintes du groupe `page_access` sont de source `applicatif`, vérifiées live.
Aucune n'est une dette legacy : le comportement (redirect CLIENT sans message, filtrage
par advisor) est intentionnel et documenté dans le code. Toutes 4 ont `Nature : métier`.
Aucune question `ASK USER` n'est nécessaire — le code au commit analysé est non ambigu.

### Champs display — aucune_règle (23 champs)

| Champs | Verdict |
|---|---|
| nbClients, encodesTotal, pendingLoansTotal, clientAvatar, clientFullName, clientUsername, clientStatus, clientTotalBalance, clientNbAccounts, clientActiveLoans, clientPendingLoans, pendingLoanAlert, detailClientName, detailClientLogin, detailClientEmail, detailClientPhone, accountNumber, accountType, accountBalance, loanType, loanAmount, loanMonthly, loanStatus | `aucune_règle` |

Ces champs sont en affichage pur. Aucune validation front ni back ne s'applique à eux
individuellement. Les règles d'affichage conditionnel (pendingLoanAlert, inactive CSS,
panneau de détail) relèvent de RG.md (RG-015-012 à RG-015-014), non de la validation.

---

## 4. Règles EARS complètes

```
VAL-015-01-page_access-B1
IF an unauthenticated request is received for /conseiller-workspace.xhtml THEN THE SYSTEM SHALL redirect to /login.xhtml

VAL-015-01-page_access-B2
IF a request authenticated with role CLIENT is received for /conseiller-workspace.xhtml THEN THE SYSTEM SHALL redirect silently to /dashboard.xhtml without displaying an error message

VAL-015-01-page_access-B3
IF a request authenticated with role CONSEILLER or ADMIN is received for /conseiller-workspace.xhtml THEN THE SYSTEM SHALL display the portfolio page

VAL-015-01-page_access-B4
THE SYSTEM SHALL filter the portfolio to include only clients whose advisor field references the authenticated user's customer record
```

---

## 5. Règles non couvertes par le harness

Les règles `VAL-015-01-page_access-B1` à `B4` sont de kind `custom` et ne peuvent pas être
couvertes par un cas de harness de type input/output. Elles sont à couvrir dans
`retro-golden-master` via des scenarios de navigation par rôle (vérification d'URL cible
après accès à /conseiller-workspace.xhtml).

---

## 6. Dette technique legacy (pour information, hors scope réécriture)

- `page_access` [back_seul] : RG-015-025 — `rafraichir()` définie dans mais non câblée à aucun élément JSF dans `conseiller-workspace.xhtml` ; le bean est `@SessionScoped` donc le portefeuille n'est jamais rafraîchi en cours de session sans reconnexion — ne pas reproduire (la cible devra implémenter le rafraîchissement)
- `loanStatus` [aucune_règle] : RG-015-026 — `LoanStatus.DEFAUT` n'a pas de classe CSS badge dans ne pas reproduire (la cible devra couvrir tous les statuts)
