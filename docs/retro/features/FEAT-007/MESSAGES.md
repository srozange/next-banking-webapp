---
feature: FEAT-007
name: Demande et suivi de prêts
status: current
generated_by: retro-rules
date: 2026-10-03
sources:
  - src/main/webapp/pret.xhtml
  - src/main/java/com/accenture/bank/web/LoanBean.java
  - src/main/java/com/accenture/bank/mq/LoanApprovalMDB.java
  - src/main/java/com/accenture/bank/entity/LoanStatus.java
  - src/main/java/com/accenture/bank/entity/LoanType.java
---

# Messages et libellés — FEAT-007 Demande et suivi de prêts

Sources : `src/main/webapp/pret.xhtml` (libellés statiques),
`src/main/java/com/accenture/bank/web/LoanBean.java` (messages dynamiques),
`src/main/java/com/accenture/bank/mq/LoanApprovalMDB.java` (notifications asynchrones),
`src/main/java/com/accenture/bank/entity/LoanStatus.java` et `LoanType.java` (libellés d'énumération).

> Note d'encodage : le fichier `pret.xhtml` et `LoanBean.java` ne contiennent pas systématiquement
> de caractères accentués (ex. « enregistre » au lieu de « enregistré », « credit » au lieu de
> « crédit ») — les textes ci-dessous reproduisent l'orthographe réelle du source. Le crawl
> enregistré dans `FIELDS.md` reflète parfois le rendu navigateur avec accents ; la preuve
> `fichier:ligne` fait foi.

---

## En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-007-001 | `BankApp Demo - Prets` | Titre du navigateur — toujours | |
| MSG-007-002 | `Demo Bank` | Marque — toujours | (texte h1) |
| MSG-007-003 | `Bonjour, {loanBean.currentCustomer.fullName}` | Toujours (utilisateur connecté) | |
| MSG-007-004 | `Deconnexion` | Toujours | |
| MSG-007-005 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Scoring credit via ActiveMQ Artemis (MDB) — simule le traitement mainframe` | Bandeau technique — toujours | 119 |

---

## Liens de navigation (toujours visibles)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-007-010 | `Tableau de bord` | |
| MSG-007-011 | `Virement` | |
| MSG-007-012 | `Virements programmes` | |
| MSG-007-013 | `Releve` | |
| MSG-007-014 | `Budget` | |
| MSG-007-015 | `Prets` | (lien actif sur cette page) |
| MSG-007-016 | `Documents` | |
| MSG-007-017 | `Bourse` | |
| MSG-007-018 | `Mon Conseiller` | |
| MSG-007-019 | `Produits` | |
| MSG-007-020 | `Comptes Tiers` | |
| MSG-007-021 | `Beneficiaires` | |

### Liens de navigation conditionnels

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true (rôles CONSEILLER ou ADMIN) | |
| MSG-007-023 | `Administration` | `securityBean.admin` = true (rôle ADMIN uniquement) | |

---

## Section Simulateur de prêt

### Séparateur et titres

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-030 | `Simulateur de pret` | Séparateur de section — toujours | |
| MSG-007-031 | `Simuler un emprunt` | Titre de section (h2) — toujours | |

### Libellés du formulaire de simulation

| Identifiant | Texte exact (source) | Contexte | Preuve |
|---|---|---|---|
| MSG-007-032 | `Montant (EUR)` | Libellé du champ `simMontant` | |
| MSG-007-033 | `Taux annuel (%)` | Libellé du champ `simTaux` | |
| MSG-007-034 | `Duree (mois)` | Libellé du champ `simDuree` | |
| MSG-007-035 | `Calculer` | Bouton de déclenchement de simulation | |

### Résultat de simulation (conditionnel)

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-036 | `Mensualite` | Étiquette de carte — si `simulationResult != null` | |
| MSG-007-037 | `Cout total` | Étiquette de carte — si `simulationResult != null` | |
| MSG-007-038 | `Total interets` | Étiquette de carte — si `simulationResult != null` | |
| MSG-007-039 | `Voir l echeancier complet ({loanBean.simDuree} echeances)` | Lien collapsible — si `simulationResult != null` | |

### En-têtes du tableau d'échéancier simulateur

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-007-040 | `#` | |
| MSG-007-041 | `Capital` | |
| MSG-007-042 | `Interets` | |
| MSG-007-043 | `Total` | |
| MSG-007-044 | `Capital restant` | |

---

## Section Demande de prêt

### Séparateur et titres

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-050 | `Demande de pret` | Séparateur de section — toujours | |
| MSG-007-051 | `Nouveau pret` | Titre de section (h2) — toujours | |

### Libellés du formulaire de demande

| Identifiant | Texte exact (source) | Contexte | Preuve |
|---|---|---|---|
| MSG-007-052 | `Type de pret` | Libellé du champ `typePret` | |
| MSG-007-053 | `Montant (EUR)` | Libellé du champ `montant` | |
| MSG-007-054 | `Taux annuel (%)` | Libellé du champ `taux` | |
| MSG-007-055 | `Duree (mois)` | Libellé du champ `duree` | |
| MSG-007-056 | `Objet du pret` | Libellé du champ `objetPret` | |
| MSG-007-057 | `Soumettre la demande` | Bouton de soumission | |
| MSG-007-058 | `La demande sera envoyee sur queue/pretDemandesQueue et traitee de facon asynchrone par le service de scoring credit (MDB WildFly).` | Texte informatif sous le bouton — toujours | 239 |

---

## Section Emprunts en cours

### Séparateur et titres

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-060 | `Mes emprunts` | Séparateur de section — toujours | |
| MSG-007-061 | `Emprunts en cours` | Titre de section (h2) — toujours | |

### État vide

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-062 | `Aucun emprunt enregistre` | `loanBean.mesEmprunts` vide (référencé par RG-007-020) | |

### En-têtes du tableau des emprunts

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-007-063 | `Type` | |
| MSG-007-064 | `Montant` | |
| MSG-007-065 | `Taux` | |
| MSG-007-066 | `Duree` | |
| MSG-007-067 | `Mensualite` | |
| MSG-007-068 | `Total interets` | |
| MSG-007-069 | `Statut` | |
| MSG-007-070 | `Echeancier` | |
| MSG-007-071 | `Action` | |

### Boutons et textes inline du tableau des emprunts

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-072 | `Voir` | Bouton — si `loanBean.hasEcheancier(loan)` = true | |
| MSG-007-073 | `En cours de traitement...` | Texte inline — si `loan.status == EN_ATTENTE` | |
| MSG-007-074 | `Supprimer` | Bouton — toujours visible par ligne | |

---

## Section Écheancier du prêt sélectionné (conditionnelle)

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-080 | `Echeancier du pret selectionne` | Séparateur — si `echeancierSelectionne != null` | |
| MSG-007-081 | `Detail des echeances` | Titre de section (h2) — si `echeancierSelectionne != null` | |

### En-têtes du tableau d'échéancier

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-007-082 | `#` | |
| MSG-007-083 | `Date echeance` | |
| MSG-007-084 | `Capital` | |
| MSG-007-085 | `Interets` | |
| MSG-007-086 | `Total` | |
| MSG-007-087 | `Capital restant` | |
| MSG-007-088 | `Statut` | |
| MSG-007-089 | `Action` | |

### Valeurs de statut d'échéance

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-007-090 | `Paye` | Échéance payée (`rep.paye = true`) | |
| MSG-007-091 | `En attente` | Échéance non payée (`rep.paye = false`) | |
| MSG-007-092 | `Marquer paye` | Bouton — si `rep.paye = false` | |

---

## Libellés des énumérations

### Statuts de prêt (LoanStatus)

Affiché dans la colonne Statut du tableau des emprunts (`#{loan.status.libelle}`).

| Identifiant | Valeur enum | Libelle exact (source) | Preuve |
|---|---|---|---|
| MSG-007-100 | `EN_COURS` | `En cours` | |
| MSG-007-101 | `REMBOURSE` | `Rembourse` | |
| MSG-007-102 | `DEFAUT` | `En defaut` | |
| MSG-007-103 | `EN_ATTENTE` | `En attente d approbation` | |
| MSG-007-104 | `REFUSE` | `Refuse par le service credit` | |

### Types de prêt (LoanType)

Affiché dans le menu déroulant « Type de prêt » et dans la colonne Type du tableau des emprunts.

| Identifiant | Valeur enum | Libelle exact (source) | Preuve |
|---|---|---|---|
| MSG-007-110 | `IMMOBILIER` | `Immobilier` | |
| MSG-007-111 | `AUTO` | `Automobile` | |
| MSG-007-112 | `CONSOMMATION` | `Consommation` | |
| MSG-007-113 | `TRAVAUX` | `Travaux` | |
| MSG-007-114 | `ETUDIANT` | `Etudiant` | |

---

## Messages dynamiques — LoanBean (FacesMessage)

Les messages ci-dessous sont émis par `addError(...)` (SEVERITY_ERROR) ou `addInfo(...)` (SEVERITY_INFO) dans `LoanBean.java` et affichés via `<h:messages>`.

### Messages d'erreur — simulateur de prêt

| Identifiant | Texte exact (source) | Condition de déclenchement | Preuve |
|---|---|---|---|
| MSG-007-120 | `Montant invalide` | `simMontant` null ou ≤ 0 (référencé par RG-007-003) | |
| MSG-007-121 | `Taux invalide (0-30%)` | `simTaux` < 0 ou > 30 (référencé par RG-007-004) | |
| MSG-007-122 | `Duree invalide (1-360 mois)` | `simDuree` ≤ 0 ou > 360 (référencé par RG-007-005) | |
| MSG-007-123 | `Erreur simulation : {e.getMessage()}` | Exception imprévue dans `simuler()` | |

### Messages d'erreur — demande de prêt

> Note : les messages de validation de la demande de prêt sont distincts de ceux du simulateur —
> la plage n'est pas répétée dans le texte (ex. « Taux invalide » seul, sans « (0-30%) »).

| Identifiant | Texte exact (source) | Condition de déclenchement | Preuve |
|---|---|---|---|
| MSG-007-124 | `Montant invalide` | `formMontant` null ou ≤ 0 (référencé par RG-007-012) | |
| MSG-007-125 | `Taux invalide` | `formTaux` < 0 ou > 30 (référencé par RG-007-013) | |
| MSG-007-126 | `Duree invalide` | `formDuree` ≤ 0 ou > 360 (référencé par RG-007-014) | |
| MSG-007-127 | `Veuillez indiquer un motif` | `formMotif` null ou vide (référencé par RG-007-015) | |
| MSG-007-128 | `Erreur : {e.getMessage()}` | Exception imprévue dans `demanderPret()` | |

### Messages d'erreur — paiement et suppression

| Identifiant | Texte exact (source) | Condition de déclenchement | Preuve |
|---|---|---|---|
| MSG-007-129 | `Erreur : {e.getMessage()}` | Exception imprévue dans `payerEcheance()` | |
| MSG-007-130 | `Erreur suppression : {e.getMessage()}` | Exception imprévue dans `supprimerPret()` | |

### Messages d'information

| Identifiant | Texte exact (source) | Condition de déclenchement | Preuve |
|---|---|---|---|
| MSG-007-131 | `Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes)` | Demande de prêt soumise avec succès (référencé par RG-007-019) | |
| MSG-007-132 | `Echeance marquee comme payee` | Paiement d'une échéance enregistré avec succès (référencé par RG-007-036) | |

---

## Messages de notification asynchrones (LoanApprovalMDB)

Les messages ci-dessous sont générés par le MDB après traitement du scoring et publiés dans le `NotificationStore`. Ils s'affichent dans le centre de notifications du tableau de bord (FEAT-002), pas sur la page `pret.xhtml`. Les `{…}` désignent des valeurs interpolées.

| Identifiant | Type | Gabarit du message | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-007-140 | `CONFIRMATION` | `Votre demande de pret {type.libelle} de {montant} EUR a ete approuvee par le service credit.` | Scoring retourne `true` — prêt approuvé | 66 |
| MSG-007-141 | `ALERTE` | `Votre demande de pret {type.libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive).` | Scoring retourne `false` — prêt refusé | 73 |

> Règles associées : RG-007-021 (scoring), RG-007-029 (approbation), RG-007-030 (refus), RG-007-031 (notification).
