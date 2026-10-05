---
feature: FEAT-027
name: Détection de fraude en temps réel
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/web/VirementBean.java
  - src/main/webapp/admin.xhtml
  - src/main/java/com/accenture/bank/ejb/FraudDetectionService.java
  - src/main/java/com/accenture/bank/web/FraudAlertBean.java
  - src/main/java/com/accenture/bank/entity/FraudStatus.java
---

# MESSAGES — FEAT-027 « Détection de fraude en temps réel »

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Détection de fraude en temps réel » (transversal : déclenché au moment
du virement ; panneau d'administration dans `admin.xhtml`).
Les `{variables}` désignent des valeurs dynamiques interpolées à l'exécution.

---

## 1 — Message client (VirementBean — interface virement)

Affiché au CLIENT après exécution du virement lorsqu'au moins une alerte fraude est
déclenchée. Sévérité WARN (bannière d'avertissement JSF). Référencé par **RG-027-013**.

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Avertissement (SEVERITY_WARN) | `Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié.` | Affiché dans le panneau de messages JSF après la soumission du virement si `alerts.size() > 0` ; `{N}` est le nombre d'alertes retournées par `checkTransaction()` | |

---

## 2 — Panneau d'administration (admin.xhtml — section Alertes Fraude)

### En-tête de section

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de section | `Alertes Fraude — Moteur de détection` | En-tête fixe du panneau fraude, toujours affiché | |
| Badge compteur (conditionnel) | `{N} ouverte(s)` | Badge rouge (`#E3000F`) ajouté au titre uniquement si `fraudAlertBean.openCount > 0` ; `{N}` est le nombre d'alertes en statut OPEN | |
| Action | `Rafraichir` | Bouton de rechargement des alertes | |

### Tableau des alertes — en-têtes de colonnes

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| En-tête de colonne | `ID` | Identifiant interne de l'alerte | |
| En-tête de colonne | `Client` | Nom du client associé à l'alerte | |
| En-tête de colonne | `Règle déclenchée` | Code de la règle fraude (valeur enum `FraudRule`) | |
| En-tête de colonne | `Score risque` | Score de risque de la règle | |
| En-tête de colonne | `Statut` | Statut courant de l'alerte (valeur enum `FraudStatus`) | |
| En-tête de colonne | `Description` | Description générée par `FraudDetectionService` au moment de la détection | |
| En-tête de colonne | `Date` | Date de création de l'alerte (format `dd/MM/yy HH:mm`) | |
| En-tête de colonne | `Actions` | Boutons d'action disponibles pour les alertes OPEN | |

### Tableau des alertes — état vide

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Message état vide | `Aucune alerte fraude enregistrée` | Affiché dans la cellule unique (`colspan="8"`) lorsque `fraudAlertBean.allAlerts` est vide | |

### Tableau des alertes — cellules dynamiques (ligne par alerte)

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Score (format) | `{N}/100` | Score de risque de la règle affiché en gras ; couleur rouge (`#E3000F`) si ≥ 70, orange si ≥ 40, vert sinon | |
| Statut (valeur enum) | `OPEN` | Valeur brute de `FraudStatus` affichée dans un badge rouge (`#E3000F`) | 506 |
| Statut (valeur enum) | `CONFIRMED` | Valeur brute de `FraudStatus` affichée dans un badge orange (`#ff6600`) | 506 |
| Statut (valeur enum) | `FALSE_POSITIVE` | Valeur brute de `FraudStatus` affichée dans un badge vert (`#4caf50`) | 506 |
| Statut (valeur enum) | `REVIEWED` | Valeur brute de `FraudStatus` ; couleur de badge gris (`#888`) — aucun traitement visuel dédié | 506 |
| Bouton d'action | `Confirmer` | Affiché uniquement si `alert.status == 'OPEN'` ; déclenche `fraudAlertBean.confirmAlert(alert.id)` — référencé par **RG-027-015** | |
| Bouton d'action | `Faux positif` | Affiché uniquement si `alert.status == 'OPEN'` ; déclenche `fraudAlertBean.dismissAlert(alert.id)` — référencé par **RG-027-016** | |
| Texte alerte traitée | `Traité par {reviewedBy}` | Affiché à la place des boutons d'action lorsque `alert.status != 'OPEN'` ; `{reviewedBy}` est le nom complet ou l'identifiant du réviseur | |

### Noms de client (fallbacks)

Valeurs renvoyées par `FraudAlertBean.getCustomerName()` dans la colonne `Client` lorsque
le compte client n'est pas trouvé ou qu'une erreur survient.

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Valeur de repli (client introuvable) | `Client #{customerId} (introuvable)` | Affiché dans la colonne `Client` si `accountService.findById()` renvoie `null` | |
| Valeur de repli (erreur) | `Client #{customerId} (erreur)` | Affiché dans la colonne `Client` si `accountService.findById()` lève une exception | |
| Valeur de repli (identifiant null) | `—` | Affiché dans la colonne `Client` si `customerId` est `null` | |

---

## 3 — Descriptions d'alertes (FraudDetectionService — colonne Description)

Textes stockés dans le champ `FraudAlert.description` et affichés dans la colonne
`Description` du panneau d'administration. Générés en anglais par `FraudDetectionService`.
Les `{variables}` sont interpolées à l'exécution par concaténation Java.

| Règle | Texte exact (patron) | Preuve |
|---|---|---|
| `LARGE_AMOUNT` (montant ≥ 15 000 EUR) | `Transaction amount {amount} exceeds the very-large threshold of {threshold}` | 158 |
| `LARGE_AMOUNT` (montant ≥ 5 000 EUR et < 15 000 EUR) | `Transaction amount {amount} exceeds the large threshold of {threshold}` | 166 |
| `HIGH_FREQUENCY` | `Customer has {count} transactions today, exceeding the daily limit of {maxDailyTransactions}` | 210 |
| `NEW_BENEFICIARY` | `Target IBAN {iban} is not registered as a trusted beneficiary` | |
| `UNUSUAL_HOURS` | `Transaction initiated at hour {hourOfDay} which is outside normal operating hours (06:00-23:00)` | 277 |
| `ROUND_AMOUNT` | `Transaction amount {amount} is a suspicious round figure (multiple of 1000)` | 300 |
| `FOREIGN_IBAN` | `Target IBAN {iban} does not start with FR (possible foreign transfer)` | |
| `RAPID_SUCCESSION` | `Previous transaction was only {elapsedSeconds} seconds ago (threshold: {rapidSuccessionSeconds}s)` | 359 |
| `VELOCITY_CHECK` | `Customer executed {highValueCount} transactions above 500 EUR in the last {velocityWindowMinutes} minutes (limit: {velocityMaxCount})` | 428 |

> Les descriptions sont en anglais alors que l'interface est en français. C'est une dette
> identifiée dans le legacy — à homogénéiser dans la cible.
