---
feature: FEAT-025
name: "Système de notifications internes"
interface: Transversal
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/java/com/accenture/bank/mq/VirementProcessorMDB.java
  - src/main/java/com/accenture/bank/mq/LoanApprovalMDB.java
  - src/main/java/com/accenture/bank/mq/BankNotification.java
  - src/main/java/com/accenture/bank/mq/NotificationStore.java
  - src/main/java/com/accenture/bank/web/DashboardBean.java
  - src/main/java/com/accenture/bank/ejb/NotificationService.java
  - src/main/webapp/dashboard.xhtml
---

# MESSAGES — FEAT-025 Système de notifications internes

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Système de notifications internes » (pipeline in-memory JMS + pipeline JPA).
Les `{variables}` désignent des valeurs dynamiques interpolées à l'exécution.

> Note d'encodage : les messages produits par le pipeline in-memory (`VirementProcessorMDB`,
> `LoanApprovalMDB`) suppriment systématiquement les accents et les apostrophes typographiques
> (ex. « bloque » au lieu de « bloqué », « pret » au lieu de « prêt », « etes » au lieu
> de « êtes »). Le pipeline JPA (`NotificationService`) utilise des accents corrects.
> Les textes ci-dessous reproduisent l'orthographe exacte du source. La preuve `fichier:ligne`
> fait foi.

---

## 1. Messages produits par le pipeline in-memory (VirementProcessorMDB)

Ces messages sont stockés dans `NotificationStore` (CDI `@ApplicationScoped`) et affichés
au tableau de bord (`dashboard.xhtml`). Ils sont le seul pipeline actif et visible.

### Notifications de virement

| Identifiant | Type | Texte exact (source) | Condition d'affichage | Règle | Preuve |
|---|---|---|---|---|---|
| MSG-025-001 | `FRAUDE` | `Virement de {amount} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation.` | Montant >= SEUIL_FRAUDE_EUR (défaut 10 000 EUR) | RG-025-003 | 59 |
| MSG-025-002 | `ALERTE` | `Virement de {amount} EUR en attente de validation - transaction importante, verification en cours.` | Montant >= SEUIL_ALERTE_EUR (défaut 5 000 EUR) et < SEUIL_FRAUDE_EUR | RG-025-002 | 67 |
| MSG-025-003 | `CONFIRMATION` | `Virement de {amount} EUR vers le compte {targetAccountNumber} confirme` | Montant < SEUIL_ALERTE_EUR (défaut 5 000 EUR) et motif vide ou absent | RG-025-001 | 75 |
| MSG-025-004 | `CONFIRMATION` | `Virement de {amount} EUR vers le compte {targetAccountNumber} confirme (motif : {motif})` | Montant < SEUIL_ALERTE_EUR et motif non vide (`vm.getMotif()` non null et non blank) | RG-025-004 | 77 |

### Notifications de prêt

| Identifiant | Type | Texte exact (source) | Condition d'affichage | Règle | Preuve |
|---|---|---|---|---|---|
| MSG-025-005 | `CONFIRMATION` | `Votre demande de pret {loanType.libelle} de {montant} EUR a ete approuvee par le service credit.` | Demande de prêt approuvée par le scoring crédit (MDB) | RG-025-005 | 66 |
| MSG-025-006 | `ALERTE` | `Votre demande de pret {loanType.libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive).` | Demande de prêt refusée par le scoring crédit (MDB) | RG-025-006 | 73 |

---

## 2. Interface tableau de bord (dashboard.xhtml — section notifications)

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-025-010 | `Notifications temps reel (Message Broker)` | Séparateur de section — toujours affiché | |
| MSG-025-011 | `Centre de notifications` | Titre de la section notifications — toujours affiché | |
| MSG-025-012 | `{N} nouvelle(s)` | Badge compteur affiché si `dashboardBean.unreadCount > 0` ; `{N}` est le nombre de notifications non lues | |
| MSG-025-013 | `ActiveMQ Artemis - MDB JEE - queue/virements` | Sous-titre technique — toujours affiché | |
| MSG-025-014 | `Tout marquer lu` | Bouton action affiché si `dashboardBean.unreadCount > 0` — référencé par RG-025-009 | |
| MSG-025-015 | `Effacer tout` | Bouton action affiché si la liste de notifications est non vide — référencé par RG-025-010 | |
| MSG-025-016 | `Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel` | État vide — affiché quand `dashboardBean.notifications` est vide | 184 |
| MSG-025-017 | `Non lu` | Attribut `title` du point indicateur affiché sur chaque notification non lue (`not notif.read`) | |
| MSG-025-018 | `- publie via ActiveMQ Artemis (MDB)` | Suffixe ajouté après l'horodatage de chaque notification (format `dd/MM/yyyy HH:mm:ss`) | |

---

## 3. Types de notification (BankNotification.Type — rendus comme tags dans l'interface)

| Identifiant | Valeur affichée (`#{notif.type}`) | Icône (`#{notif.icon}`) | Couleur de fond | Preuve |
|---|---|---|---|---|
| MSG-025-020 | `CONFIRMATION` | `✅` | `#e8f5e9` (vert) | |
| MSG-025-021 | `ALERTE` | `⚠️` | `#fff8e1` (jaune) | |
| MSG-025-022 | `FRAUDE` | `🚨` | `#fce4ec` (rose) | |

---

## 4. Messages du pipeline JPA (NotificationService — non affichés aux utilisateurs)

> Ces messages sont persistés en base (table `notifications`) mais **jamais affichés** :
> le consommateur prévu (`NotificationBean`, FEAT-032) est du code mort — aucune page XHTML
> ne le référence. Ce corpus est documenté pour traçabilité ; la réécriture ne doit pas
> reproduire ces messages tels quels sans décision explicite sur l'unification des pipelines.

### Surveillance soldes faibles (checkLowBalances — @Schedule toutes les 30 min)

| Identifiant | Champ | Texte exact (source) | Condition | Règle | Preuve |
|---|---|---|---|---|---|
| MSG-025-030-T | Titre | `Solde insuffisant sur votre compte` | Solde < 100 EUR (seuil hardcodé) | RG-025-011 | |
| MSG-025-030-M | Message | `Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte.` | Solde < 100 EUR | RG-025-011 | 177 |

### Notifications de prêt (notifyLoanApproved / notifyLoanRefused)

| Identifiant | Champ | Texte exact (source) | Condition | Preuve |
|---|---|---|---|---|
| MSG-025-031-T | Titre | `Prêt approuvé` | Prêt approuvé (pipeline JPA) | |
| MSG-025-031-M | Message | `Votre demande de prêt (réf. {loanId}) a été approuvée. Votre conseiller vous contactera sous 48 h.` | Prêt approuvé (pipeline JPA) | 203 |
| MSG-025-032-T | Titre | `Prêt refusé` | Prêt refusé (pipeline JPA) | |
| MSG-025-032-M | Message | `Votre demande de prêt (réf. {loanId}) a été refusée. Vous pouvez contacter votre conseiller pour plus d'informations.` | Prêt refusé (pipeline JPA) | 228 |

### Titres génériques (méthodes createTransactionNotification / createLoanNotification — jamais appelées depuis le pipeline actif)

| Identifiant | Champ | Texte exact (source) | Preuve |
|---|---|---|---|
| MSG-025-033-T | Titre | `Alerte transaction` | |
| MSG-025-034-T | Titre | `Mise à jour de votre prêt` | |
