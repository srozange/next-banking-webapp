---
feature: FEAT-006
name: Budget et catégories de dépenses
status: current
generated_by: retro-rules
date: 2026-10-03
---

# Messages et libellés — FEAT-006 Budget et catégories de dépenses

Sources : `src/main/webapp/budget.xhtml` (libellés statiques et messages état vide),
`src/main/java/com/accenture/bank/web/BudgetBean.java` (messages dynamiques d'erreur et de confirmation).

> Note d'encodage : `budget.xhtml` n'utilise pas de caractères accentués dans ses textes
> statiques (ex. « depenses » au lieu de « dépenses ») — les textes ci-dessous reproduisent
> l'orthographe réelle du source.

---

## En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-006-001 | `BankApp Demo - Budget` | Titre du navigateur — toujours | |
| MSG-006-002 | `Demo Bank` | Marque — toujours | |
| MSG-006-003 | `Bonjour, {budgetBean.currentCustomer.fullName}` | Toujours (utilisateur connecté) | |
| MSG-006-004 | `Deconnexion` | Toujours | |
| MSG-006-005 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Categorisation automatique par mots-cles` | Toujours (bandeau technique) | |

---

## Liens de navigation (toujours visibles)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-006-010 | `Tableau de bord` | |
| MSG-006-011 | `Virement` | |
| MSG-006-012 | `Virements programmes` | |
| MSG-006-013 | `Releve` | |
| MSG-006-014 | `Budget` | |
| MSG-006-015 | `Prets` | |
| MSG-006-016 | `Documents` | |
| MSG-006-017 | `Bourse` | |
| MSG-006-018 | `Mon Conseiller` | |
| MSG-006-019 | `Produits` | |
| MSG-006-020 | `Comptes Tiers` | |
| MSG-006-021 | `Beneficiaires` | |

## Liens de navigation conditionnels

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-006-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true (rôles CONSEILLER ou ADMIN) | |
| MSG-006-023 | `Administration` | `securityBean.admin` = true (rôle ADMIN uniquement) | |

---

## Sélecteur de période

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-006-030 | `Periode :` | Label du sélecteur — toujours | |
| MSG-006-031 | `Afficher` | Bouton de soumission du sélecteur — toujours | |

---

## Cartes de résumé

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-006-035 | `Total depenses` | En-tête de carte rouge — toujours | |
| MSG-006-036 | `Total revenus` | En-tête de carte verte — toujours | |
| MSG-006-037 | `Plafonds definis` | En-tête de carte bleue — toujours | |

---

## Répartition par catégorie

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-006-040 | `Repartition par categorie` | Séparateur de section — toujours | |
| MSG-006-041 | `Depenses du mois {budgetBean.selectedMonth}` | En-tête de section — toujours ; `{selectedMonth}` interpolé (format YYYY-MM) | |
| MSG-006-042 | `Aucune depense enregistree sur cette periode` | État vide — si `empty budgetBean.categoriesAvecDepenses` (référencé par RG-006-010) | |
| MSG-006-043 | `{pourcentage}% du plafond` | Libellé de progression — si plafond défini pour la catégorie ; `{pourcentage}` = valeur entière calculée | |
| MSG-006-044 | `Aucun plafond defini` | Libellé italie gris — si aucun plafond pour la catégorie | |
| MSG-006-045 | `Depassement` | Badge rouge — si `budgetBean.depassement[cat]` = true (référencé par RG-006-024) | |

---

## Gestion des plafonds

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-006-050 | `Plafonds budgetaires` | Séparateur de section — toujours | |
| MSG-006-051 | `Definir un plafond` | En-tête de section (h2) — toujours | |
| MSG-006-052 | `Categorie` | Label du champ de sélection — toujours | |
| MSG-006-053 | `-- Choisir --` | Option vide par défaut du sélecteur de catégorie | |
| MSG-006-054 | `Plafond mensuel (EUR)` | Label du champ plafond — toujours | |
| MSG-006-055 | `ex: 500` | Placeholder du champ plafond — toujours | |
| MSG-006-056 | `Enregistrer le plafond` | Bouton de soumission — toujours | |

---

## Tableau des plafonds existants

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-006-060 | `Plafonds du mois {budgetBean.selectedMonth}` | En-tête de section (h2) — si `not empty budgetBean.budgets` | |
| MSG-006-061 | `Categorie` | En-tête de colonne | |
| MSG-006-062 | `Plafond` | En-tête de colonne | |
| MSG-006-063 | `Depense` | En-tête de colonne | |
| MSG-006-064 | `Ecart` | En-tête de colonne | |
| MSG-006-065 | `Action` | En-tête de colonne | |
| MSG-006-066 | `Supprimer` | Bouton — par ligne de budget (référencé par RG-006-019) | |

---

## Messages dynamiques (erreurs et confirmations)

Les messages ci-dessous sont produits en Java à l'exécution via `FacesContext.addMessage` ; `{…}` désigne une valeur interpolée.

| Identifiant | Type | Gabarit du message | Condition de déclenchement | Preuve |
|---|---|---|---|---|
| MSG-006-070 | Erreur | `Veuillez selectionner une categorie` | Soumission du formulaire plafond sans catégorie sélectionnée (référencé par RG-006-013) | |
| MSG-006-071 | Erreur | `Veuillez saisir un plafond valide` | Plafond nul ou inférieur ou égal à zéro (référencé par RG-006-014) | |
| MSG-006-072 | Info | `Plafond enregistre pour {cat.libelle}` | Sauvegarde réussie — `{cat.libelle}` = libellé de la catégorie sélectionnée (référencé par RG-006-018) | |
| MSG-006-073 | Erreur | `Erreur : {e.getMessage()}` | Exception non gérée lors de l'appel à `BudgetService.upsertBudget` | |
| MSG-006-074 | Erreur | `Erreur suppression : {e.getMessage()}` | Exception non gérée lors de l'appel à `BudgetService.deleteBudget` | |

> MSG-006-073 et MSG-006-074 exposent le message technique de l'exception au client — nature dette-legacy (correspondant aux dettes consignées dans RG-006-016 / RG-006-019).
