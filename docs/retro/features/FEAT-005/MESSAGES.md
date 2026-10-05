---
feature: FEAT-005
name: Relevé de compte
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/webapp/releve.xhtml
---

# MESSAGES — FEAT-005 Relevé de compte

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Relevé de compte » (`/releve.xhtml`). Les `{variables}` désignent des
valeurs dynamiques interpolées à l'exécution.

---

## Page et navigation

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de page (onglet) | `BankApp Demo - Relevé de compte` | `<title>` de la page | |
| Logo (initiales) | `DB` | Disque rouge en en-tête de barre de navigation | |
| Marque | `Demo Bank` | Nom de l'application dans la barre de navigation | |
| Action | `Déconnexion` | Lien de déconnexion en en-tête (commandLink) | |
| Lien de navigation | `Tableau de bord` | Menu principal — toujours visible | |
| Lien de navigation | `Virement` | Menu principal — toujours visible | |
| Lien de navigation | `Virements programmes` | Menu principal — toujours visible | |
| Lien de navigation (actif) | `Releve` | Menu principal — page courante | |
| Lien de navigation | `Budget` | Menu principal — toujours visible | |
| Lien de navigation | `Prets` | Menu principal — toujours visible | |
| Lien de navigation | `Documents` | Menu principal — toujours visible | |
| Lien de navigation | `Bourse` | Menu principal — toujours visible | |
| Lien de navigation | `Mon Conseiller` | Menu principal — toujours visible | |
| Lien de navigation | `Produits` | Menu principal — toujours visible | |
| Lien de navigation | `Comptes Tiers` | Menu principal — toujours visible | |
| Lien de navigation | `Beneficiaires` | Menu principal — toujours visible | |
| Lien de navigation conditionnel | `Espace Conseiller` | Menu principal — si `securityBean.adminOrConseiller` (rôles CONSEILLER ou ADMIN) — référencé par RG-005-018 | |
| Lien de navigation conditionnel | `Administration` | Menu principal — si `securityBean.admin` (rôle ADMIN uniquement) — référencé par RG-005-019 | |

---

## Bandeau technique

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Bandeau technique | `Système Legacy — Requête via @NamedQuery JPA` | Toujours affiché sous la navigation ; non fonctionnel | 96 |

---

## Section relevé de compte

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de section | `Relevé de compte` | En-tête de la section principale | |
| Libellé | `Compte :` | Label du sélecteur de compte (`releveForm:accountSelect`) | |

---

## Panneau d'informations du compte (dynamique)

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Libellé dynamique | `{accountType} — N° {accountNumber} — Solde actuel : {balance}` | Affiché dans le panneau `accountInfo` si un compte est sélectionné ; `{balance}` est formaté en devise EUR avec 2 décimales minimum | 124 |

> `{accountType}` et `{accountNumber}` sont des champs de l'entité `BankAccount`.
> `{balance}` est converti par `f:convertNumber type="currency" currencySymbol="€" minFractionDigits="2"`.

---

## Panneau des transactions

### État vide

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Message état vide | `Aucune transaction pour ce compte.` | Affiché dans le panneau `transactionPanel` lorsque la liste des transactions est vide — référencé par RG-005-011 | |

### Compteur

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Compteur | `{N} opération(s)` | Affiché au-dessus du tableau quand la liste est non vide ; `{N}` est le nombre total de transactions chargées — référencé par RG-005-012 | |

### En-têtes de tableau

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| En-tête de colonne | `Date` | Colonne date de transaction | |
| En-tête de colonne | `Description` | Colonne description | |
| En-tête de colonne | `Type` | Colonne type (CREDIT / DEBIT) | |
| En-tête de colonne | `Montant` | Colonne montant | |
| En-tête de colonne | `Solde après` | Colonne solde après opération | |

### Cellules dynamiques (ligne par transaction)

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Badge type | `CREDIT` | Affiché en vert (#2e7d32) dans la colonne Type pour les transactions créditrices | |
| Badge type | `DEBIT` | Affiché en rouge (#E3000F) dans la colonne Type pour les transactions débitrices | |
| Badge état | `En attente` | Badge orange affiché avant la description lorsque `tx.description` commence par `[EN ATTENTE] ` — référencé par RG-005-017 | |
| Préfixe montant | `+` | Préfixe affiché devant le montant pour les transactions CREDIT | |
| Préfixe montant | `-` | Préfixe affiché devant le montant pour les transactions DEBIT | |
