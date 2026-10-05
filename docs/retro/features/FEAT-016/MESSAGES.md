---
feature: FEAT-016
name: Administration — Gestion globale
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# Messages et libellés — FEAT-016 Administration — Gestion globale

Sources : `src/main/webapp/admin.xhtml` (libellés statiques et messages vides),
`src/main/java/com/accenture/bank/web/AdminBean.java` (messages FacesMessage),
`src/main/java/com/accenture/bank/web/FraudAlertBean.java` (libellés calculés colonne Client).

> Note d'encodage : `admin.xhtml` omet les accents sur plusieurs libellés visibles
> (« Prets », « Rafraichir », « systeme », « enregistre », etc.) — les textes ci-dessous
> reproduisent l'orthographe exacte de la source. Les messages FacesMessage produits par
> `AdminBean.java` conservent leur accentuation correcte.

---

## En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-016-001 | `BankApp Demo - Administration` | Titre du navigateur — toujours | |
| MSG-016-002 | `DB` | Logo (initiales dans disque rouge) — toujours | |
| MSG-016-003 | `Demo Bank` | Marque — toujours | |
| MSG-016-004 | `ADMIN` | Badge de rôle dans la barre de navigation — toujours | |
| MSG-016-005 | `Deconnexion` | Lien de déconnexion — toujours | |

### Liens de navigation

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-010 | `Administration` | |
| MSG-016-011 | `Espace Conseiller` | |
| MSG-016-012 | `Mon tableau de bord` | |

---

## Bandeau administration

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-020 | `Espace Administration — JEE 8 / JSF 2.3 / EJB 3.2 / RBAC custom — Role : ADMIN` | Toujours (rôle ADMIN requis par AppFilter) | |

---

## Indicateurs KPI

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-030 | `Total clients` | Libellé de la carte KPI clients — toujours | |
| MSG-016-031 | `{n} actifs` | Sous-libellé de la carte clients — toujours | |
| MSG-016-032 | `Conseillers` | Libellé de la carte KPI conseillers — toujours | |
| MSG-016-033 | `actifs` | Sous-libellé de la carte conseillers — toujours | |
| MSG-016-034 | `Prets en attente` | Libellé de la carte KPI prêts — toujours | |
| MSG-016-035 | `{montantTotalPending EUR} a traiter` | Sous-libellé de la carte prêts — toujours | |
| MSG-016-036 | `Total utilisateurs` | Libellé de la carte KPI utilisateurs — toujours | |
| MSG-016-037 | `tous roles confondus` | Sous-libellé de la carte utilisateurs — toujours | |

---

## Section « Pipeline Prets — En attente d approbation »

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-040 | `Pipeline Prets — En attente d approbation` | Titre de section — toujours | |
| MSG-016-041 | `Rafraichir` | Bouton de rechargement — toujours | |
| MSG-016-042 | `Aucun pret en attente` | Message état vide — quand `adminBean.pendingLoans` est vide | |
| MSG-016-043 | `Approuver` | Bouton d'approbation — une occurrence par prêt EN_ATTENTE | |
| MSG-016-044 | `Refuser` | Bouton de refus — une occurrence par prêt EN_ATTENTE | |

### En-têtes de colonnes

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-045 | `ID` | |
| MSG-016-046 | `Client` | |
| MSG-016-047 | `Type` | |
| MSG-016-048 | `Montant` | |
| MSG-016-049 | `Duree` | |
| MSG-016-049b | `Taux` | |
| MSG-016-049c | `Mensualite` | |
| MSG-016-049d | `Motif` | |
| MSG-016-049e | `Actions` | |

---

## Section « Gestion des clients »

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-050 | `Gestion des clients` | Titre de section — toujours | |
| MSG-016-051 | `Rechercher par nom, login, email...` | Placeholder du champ de recherche — toujours | |
| MSG-016-052 | `Filtrer` | Bouton de filtre — toujours | |
| MSG-016-053 | `Aucun client trouve` | Message état vide — quand `adminBean.filteredClients` est vide | |
| MSG-016-054 | `Actif` | Valeur du badge Statut — quand `client.active` = true | |
| MSG-016-055 | `Inactif` | Valeur du badge Statut — quand `client.active` = false | |
| MSG-016-056 | `Detail` | Bouton d'ouverture du panneau détail — une occurrence par ligne client | |
| MSG-016-057 | `Desactiver` | Bouton de désactivation — quand `client.active` = true | |
| MSG-016-058 | `Activer` | Bouton d'activation — quand `client.active` = false | |

### En-têtes de colonnes (table clients)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-059a | `Nom` | |
| MSG-016-059b | `Login` | |
| MSG-016-059c | `Email` | |
| MSG-016-059d | `Role` | |
| MSG-016-059e | `Statut` | |
| MSG-016-059f | `Conseiller` | |
| MSG-016-059g | `Actions` | |

### Panneau détail client

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-060 | `Detail : {adminBean.selectedClient.fullName}` | Titre du panneau — quand un client est sélectionné | |
| MSG-016-061 | `Fermer` | Bouton de fermeture du panneau — toujours visible dans le panneau | |
| MSG-016-062 | `Nom complet` | Libellé du champ — toujours dans le panneau | |
| MSG-016-063 | `Identifiant` | Libellé du champ — toujours dans le panneau | |
| MSG-016-064 | `Email` | Libellé du champ — toujours dans le panneau | |
| MSG-016-065 | `Telephone` | Libellé du champ — toujours dans le panneau | |
| MSG-016-066 | `Role` | Libellé du champ — toujours dans le panneau | |
| MSG-016-067 | `Statut` | Libellé du champ — toujours dans le panneau | |
| MSG-016-068 | `Conseiller assigne` | Libellé du champ — quand `selectedClient.advisor` != null | |
| MSG-016-069 | `Client depuis` | Libellé du champ — quand `selectedClient.advisorSince` != null | |

---

## Section « Historique de tous les prets »

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-070 | `Historique de tous les prets` | Titre de section — toujours | |
| MSG-016-071 | `Aucun pret en base` | Message état vide — quand `adminBean.allLoans` est vide | |

### En-têtes de colonnes (historique prêts)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-072a | `ID` | |
| MSG-016-072b | `Client` | |
| MSG-016-072c | `Type` | |
| MSG-016-072d | `Montant` | |
| MSG-016-072e | `Statut` | |
| MSG-016-072f | `Duree` | |
| MSG-016-072g | `Mensualite` | |
| MSG-016-072h | `Date debut` | |

---

## Section « Tous les utilisateurs du systeme »

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-080 | `Tous les utilisateurs du systeme` | Titre de section — toujours | |

### En-têtes de colonnes (tous utilisateurs)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-081a | `Nom` | |
| MSG-016-081b | `Login` | |
| MSG-016-081c | `Email` | |
| MSG-016-081d | `Role` | |
| MSG-016-081e | `Statut` | |

---

## Section « Alertes Fraude — Moteur de détection »

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-090 | `Alertes Fraude — Moteur de détection` | Titre de section — toujours | |
| MSG-016-091 | `{fraudAlertBean.openCount} ouverte(s)` | Badge compteur d'alertes — quand `openCount` > 0 | |
| MSG-016-092 | `Rafraichir` | Bouton de rechargement — toujours | |
| MSG-016-093 | `Aucune alerte fraude enregistrée` | Message état vide — quand `fraudAlertBean.allAlerts` est vide | |
| MSG-016-094 | `{alert.riskScore}/100` | Score de risque formaté — une occurrence par ligne d'alerte | |
| MSG-016-095 | `Confirmer` | Bouton de confirmation d'alerte — quand `alert.status` = OPEN | |
| MSG-016-096 | `Faux positif` | Bouton de rejet d'alerte — quand `alert.status` = OPEN | |
| MSG-016-097 | `Traité par {alert.reviewedBy}` | Libellé affiché à la place des boutons — quand `alert.status` != OPEN | |

### En-têtes de colonnes (alertes fraude)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-098a | `ID` | |
| MSG-016-098b | `Client` | |
| MSG-016-098c | `Règle déclenchée` | |
| MSG-016-098d | `Score risque` | |
| MSG-016-098e | `Statut` | |
| MSG-016-098f | `Description` | |
| MSG-016-098g | `Date` | |
| MSG-016-098h | `Actions` | |

### Libellés calculés — colonne « Client » (FraudAlertBean.getCustomerName)

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-099a | `—` | `customerId` est null | |
| MSG-016-099b | `Client #{customerId} (introuvable)` | Client absent de la base | |
| MSG-016-099c | `Client #{customerId} (erreur)` | Exception lors de la résolution du nom | |

---

## Section « Traitements nocturnes »

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-100 | `Traitements nocturnes (EJB Timer @Schedule)` | Titre de section — toujours | |
| MSG-016-101 | `Rafraichir` | Bouton de rechargement — toujours | |
| MSG-016-102 | `Aucun traitement nocturne enregistre` | Message état vide — quand `batchAdminBean.recentJobs` est vide | |

### En-têtes de colonnes (traitements nocturnes)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-103a | `Job` | |
| MSG-016-103b | `Debut` | |
| MSG-016-103c | `Fin` | |
| MSG-016-103d | `Duree (ms)` | |
| MSG-016-103e | `Enregistrements` | |
| MSG-016-103f | `Statut` | |
| MSG-016-103g | `Details` | |

---

## Section « Catalogue produits bancaires »

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-016-110 | `Catalogue produits bancaires` | Titre de section — toujours | |
| MSG-016-111 | `Voir la page Produits` | Lien vers `/produits.xhtml` (FEAT-011) — toujours | |
| MSG-016-112 | `Produits d epargne` | Sous-titre de catégorie épargne — toujours | |
| MSG-016-113 | `Produits de credit` | Sous-titre de catégorie crédit — toujours | |
| MSG-016-114 | `Produits d investissement` | Sous-titre de catégorie investissement — toujours | |
| MSG-016-115 | `Aucun produit` | Message état vide — quand `batchAdminBean.allProducts` est vide (répété pour chaque table) | |
| MSG-016-116 | `Oui` | Valeur booléenne — `capitalGuaranteed` ou `requiresInsurance` = true | |
| MSG-016-117 | `Non` | Valeur booléenne — `capitalGuaranteed` ou `requiresInsurance` = false | |
| MSG-016-118 | `Illimite` | Valeur de durée — quand `maxDurationMonths` est null | |
| MSG-016-119 | `{riskLevelLabel} ({riskLevel}/5)` | Niveau de risque formaté — table investissement | |

### En-têtes de colonnes — épargne

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-120a | `Nom` | |
| MSG-016-120b | `Taux` | |
| MSG-016-120c | `Depot min` | |
| MSG-016-120d | `Capital garanti` | |
| MSG-016-120e | `Duree max` | |

### En-têtes de colonnes — crédit

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-121a | `Nom` | |
| MSG-016-121b | `Taux` | |
| MSG-016-121c | `Duree max` | |
| MSG-016-121d | `LTV max` | |
| MSG-016-121e | `Assurance requise` | |

### En-têtes de colonnes — investissement

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-016-122a | `Nom` | |
| MSG-016-122b | `Secteur` | |
| MSG-016-122c | `Niveau de risque` | |
| MSG-016-122d | `Rendement attendu` | |

---

## Messages FacesMessage (AdminBean)

Les messages ci-dessous sont émis par `AdminBean.java` comme notifications globales
(`clientId=null`, visibles via `<h:messages globalOnly="true"/>` en haut de page).

| Identifiant | Sévérité | Texte exact | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-016-130 | INFO | `Client désactivé` | Désactivation d'un client réussie | |
| MSG-016-131 | INFO | `Client activé` | Activation d'un client réussie | |
| MSG-016-132 | INFO | `Prêt approuvé` | Approbation d'un prêt réussie | |
| MSG-016-133 | INFO | `Prêt refusé` | Refus d'un prêt réussi | |
| MSG-016-134 | ERROR | `Erreur : {e.getMessage()}` | Exception lors de l'approbation ou du refus d'un prêt | |

> **Dette-legacy** : MSG-016-134 expose directement le message de l'exception Java à l'utilisateur,
> sans filtrage ni traduction en message utilisateur. Voir RG-016-014 (dette-legacy).

---

## Règles associées

| Message | Règle RG.md |
|---|---|
| MSG-016-042 (Aucun pret en attente) | RG-016-008 |
| MSG-016-053 (Aucun client trouve) | RG-016-018 |
| MSG-016-095, MSG-016-096 (Confirmer / Faux positif) | RG-016-030, RG-016-031 |
| MSG-016-097 (Traité par) | RG-016-029 |
| MSG-016-130 (Client désactivé) | RG-016-021 |
| MSG-016-131 (Client activé) | RG-016-022 |
| MSG-016-132 (Prêt approuvé) | RG-016-009 |
| MSG-016-133 (Prêt refusé) | RG-016-010 |
| MSG-016-134 (Erreur : …) | RG-016-014 |
