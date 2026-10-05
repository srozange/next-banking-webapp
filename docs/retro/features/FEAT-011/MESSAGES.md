---
feature: FEAT-011
name: Catalogue produits bancaires
status: current
generated_by: retro-rules
date: 2026-10-03
---

# Messages et libellés — FEAT-011 Catalogue produits bancaires

Sources : `src/main/webapp/produits.xhtml` (libellés statiques et gabarits d'affichage),
`src/main/java/com/accenture/bank/entity/InvestmentProduct.java` (libellés de niveau de risque générés).

> Note d'encodage : le fichier `produits.xhtml` utilise des caractères accentués absents dans certains libellés
> (ex. « Produits d epargne », « Rafraichir », « Duree » sans accent — orthographe reproduite telle quelle depuis le source).
> La marque de tiret long « — » est le caractère typographique U+2014 utilisé pour les valeurs nulles.

---

## En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-011-001 | `BankApp Demo - Produits bancaires` | Titre du navigateur — toujours | |
| MSG-011-002 | `Demo Bank` | Marque — toujours | |
| MSG-011-003 | `Deconnexion` | Lien de déconnexion — toujours | 71 |
| MSG-011-004 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 — JPA JOINED Inheritance (BankProduct → SavingsProduct, CreditProduct, InvestmentProduct)` | Bandeau technique — toujours | |

### Liens de navigation (toujours visibles)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-011-010 | `Tableau de bord` | |
| MSG-011-011 | `Virement` | |
| MSG-011-012 | `Virements programmes` | |
| MSG-011-013 | `Releve` | |
| MSG-011-014 | `Budget` | |
| MSG-011-015 | `Prets` | |
| MSG-011-016 | `Documents` | |
| MSG-011-017 | `Bourse` | |
| MSG-011-018 | `Mon Conseiller` | |
| MSG-011-019 | `Produits` (actif) | |
| MSG-011-020 | `Comptes Tiers` | |
| MSG-011-021 | `Beneficiaires` | |

### Liens de navigation conditionnels

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-011-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true (rôles CONSEILLER ou ADMIN) | |
| MSG-011-023 | `Administration` | `securityBean.admin` = true (rôle ADMIN uniquement) | |

---

## Catalogue des produits — structure de page

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-011-030 | `Catalogue des produits` | Séparateur de section — toujours | |
| MSG-011-031 | `Catalogue des produits` | En-tête de section (h2) — toujours | |

---

## Produits d'épargne

### En-têtes de colonnes

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-011-040 | `Produits d epargne` | |
| MSG-011-041 | `Nom` | |
| MSG-011-042 | `Description` | |
| MSG-011-043 | `Taux` | |
| MSG-011-044 | `Depot min` | |
| MSG-011-045 | `Capital garanti` | |
| MSG-011-046 | `Duree max` | |

### Messages d'état et valeurs affichées

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-011-047 | `Aucun produit d epargne disponible` | État vide — si `allProducts` vide (référencé par RG-011-014) | |
| MSG-011-048 | `Oui` | `capitalGuaranteed` = true | |
| MSG-011-049 | `Non` | `capitalGuaranteed` = false | |
| MSG-011-050 | `{maxDurationMonths} mois` | `maxDurationMonths` non null — gabarit dynamique | |
| MSG-011-051 | `Illimite` | `maxDurationMonths` = null pour un produit d'épargne (référencé par RG-011-009) | |

---

## Produits de crédit

### En-têtes de colonnes

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-011-060 | `Produits de credit` | |
| MSG-011-061 | `Nom` | |
| MSG-011-062 | `Description` | |
| MSG-011-063 | `Taux` | |
| MSG-011-064 | `Duree max` | |
| MSG-011-065 | `LTV max` | |
| MSG-011-066 | `Assurance requise` | |

### Messages d'état et valeurs affichées

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-011-067 | `Aucun produit de credit disponible` | État vide — si `allProducts` vide (référencé par RG-011-014) | |
| MSG-011-068 | `{maxDurationMonths} mois` | `maxDurationMonths` non null — gabarit dynamique | |
| MSG-011-069 | `Oui` | `requiresInsurance` = true | |
| MSG-011-070 | `Non` | `requiresInsurance` = false | |

---

## Produits d'investissement

### En-têtes de colonnes

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-011-080 | `Produits d investissement` | |
| MSG-011-081 | `Nom` | |
| MSG-011-082 | `Description` | |
| MSG-011-083 | `Secteur` | |
| MSG-011-084 | `Niveau de risque` | |
| MSG-011-085 | `Rendement attendu` | |

### Messages d'état et valeurs affichées

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-011-086 | `Aucun produit d investissement disponible` | État vide — si `allProducts` vide (référencé par RG-011-014) | |
| MSG-011-087 | `{riskLevelLabel} ({riskLevel}/5)` | Gabarit dynamique — niveau de risque — toujours (référencé par RG-011-011) | |

### Libellés de niveau de risque (générés par InvestmentProduct.getRiskLevelLabel())

| Identifiant | Texte exact (source) | Valeur `riskLevel` | Preuve |
|---|---|---|---|
| MSG-011-090 | `Tres faible` | 1 | |
| MSG-011-091 | `Faible` | 2 | |
| MSG-011-092 | `Moyen` | 3 | |
| MSG-011-093 | `Eleve` | 4 | |
| MSG-011-094 | `Tres eleve` | 5 | |
| MSG-011-095 | `N/A` | toute valeur hors [1–5] — cas dégradé | |

---

## Valeur de substitution pour champs optionnels null

| Identifiant | Texte exact (source) | Champs concernés | Preuve |
|---|---|---|---|
| MSG-011-096 | `—` | `interestRate`, `minOpeningDeposit`, `maxLtv`, `expectedReturn`, `marketSector`, `maxDurationMonths` (crédit uniquement) — tout champ optionnel null sauf `maxDurationMonths` épargne (référencé par RG-011-008) | |

---

## Bouton d'action

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-011-097 | `Rafraichir` | Bouton — toujours visible | |

---

## Historique des traitements nocturnes

### En-têtes de section

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-011-100 | `Traitements nocturnes (EJB @Schedule)` | Séparateur de section — toujours | |
| MSG-011-101 | `Historique des traitements nocturnes (EJB @Schedule)` | En-tête (h2) — toujours | |

### En-têtes de colonnes

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-011-102 | `Job` | |
| MSG-011-103 | `Debut` | |
| MSG-011-104 | `Fin` | |
| MSG-011-105 | `Duree (ms)` | |
| MSG-011-106 | `Enregistrements` | |
| MSG-011-107 | `Statut` | |
| MSG-011-108 | `Details` | |

### Messages d'état et valeurs affichées

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-011-109 | `Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00` | État vide — si `recentJobs` vide (référencé par RG-011-015) | 291 |
| MSG-011-110 | `SUCCESS` | Statut de job affiché dans le badge (référencé par RG-011-017) | |
| MSG-011-111 | `FAILED` | Statut de job affiché dans le badge (référencé par RG-011-017) | |
| MSG-011-112 | `PARTIAL` | Statut de job affiché dans le badge (référencé par RG-011-017) | |
