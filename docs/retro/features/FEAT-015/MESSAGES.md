---
feature: FEAT-015
name: "Espace Conseiller — Portefeuille clients"
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/webapp/conseiller-workspace.xhtml
---

# MESSAGES — FEAT-015 Espace Conseiller — Portefeuille clients

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Espace Conseiller — Portefeuille clients » (`/conseiller-workspace.xhtml`).
Les `{variables}` désignent des valeurs dynamiques interpolées à l'exécution.

---

## Page et navigation

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de page (onglet) | `BankApp Demo - Espace Conseiller` | `<title>` de la page | |
| Logo (initiale) | `C` | Disque vert en en-tête de barre de navigation | |
| Marque | `Demo Bank` | Nom de l'application dans la barre de navigation | |
| Libellé de rôle | `Conseiller` | Affiché en vert à droite du nom de l'utilisateur dans la barre de navigation | |
| Action | `Deconnexion` | Lien de déconnexion en en-tête (commandLink) | |
| Lien de navigation (actif) | `Mon portefeuille` | Menu latéral conseiller — page courante | |
| Lien de navigation | `Conseiller IA` | Menu latéral conseiller | |

---

## Bandeau d'accueil conseiller

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Libellé de section | `Espace Conseiller` | Bandeau vert en tête du contenu principal (en gras) | |
| Message d'accueil | `Bonjour {fullName}, vous gerez {N} client(s)` | Bandeau vert — `{fullName}` est le nom complet du conseiller connecté, `{N}` est le nombre de clients dans son portefeuille | 134 |

---

## Tableau de bord KPI

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Libellé KPI | `Mes clients` | En-tête de la tuile KPI du nombre de clients — référencé par RG-015-006 | |
| Sous-libellé KPI | `clients dans mon portefeuille` | Sous-titre de la tuile KPI « Mes clients » | |
| Libellé KPI | `Encours total` | En-tête de la tuile KPI du solde cumulé — référencé par RG-015-007 | |
| Sous-libellé KPI | `solde cumulé mes clients` | Sous-titre de la tuile KPI « Encours total » | |
| Libellé KPI | `Prets en attente` | En-tête de la tuile KPI des prêts en attente (bordure ambre) — référencé par RG-015-008 | |
| Sous-libellé KPI | `a traiter` | Sous-titre de la tuile KPI « Prets en attente » | |

---

## Portefeuille clients — état vide

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Message état vide | `Aucun client dans votre portefeuille.` | Affiché dans la section principale lorsque le portefeuille est vide — référencé par RG-015-009 | |
| Message état vide | `Contactez l administrateur pour l affectation des clients.` | Suite du message état vide, affiché sur la même carte — référencé par RG-015-009 | |

---

## Cartes clients (portefeuille non vide)

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Badge statut client | `Actif` | Badge vert affiché sur la carte client lorsque `customer.active = true` — référencé par RG-015-010 | |
| Badge statut client | `Inactif` | Badge rouge affiché sur la carte client lorsque `customer.active = false` — référencé par RG-015-010, RG-015-014 | |
| Libellé statistique | `Solde total` | Libellé de la tuile statistique solde agrégé du client — référencé par RG-015-011 | |
| Libellé statistique | `Comptes` | Libellé de la tuile statistique nombre de comptes du client — référencé par RG-015-011 | |
| Libellé statistique | `Prets actifs` | Libellé de la tuile statistique prêts EN_COURS du client — référencé par RG-015-011 | |
| Libellé statistique | `Prets en attente` | Libellé de la tuile statistique prêts EN_ATTENTE du client — référencé par RG-015-011 | |
| Indicateur prêts en attente | `⚠ {N} pret(s) en attente` | Affiché en orange sur le pied de carte lorsque `pendingLoansCount > 0` — `{N}` est le nombre de prêts EN_ATTENTE — référencé par RG-015-012 | |
| Indicateur prêts actifs | `✓ {N} pret(s) en cours` | Affiché en vert sur le pied de carte lorsque `pendingLoansCount = 0` et `activeLoansCount > 0` — `{N}` est le nombre de prêts EN_COURS — référencé par RG-015-013 | |
| Bouton action | `Voir detail` | Bouton bleu sur le pied de chaque carte client — déclenche l'affichage du panneau de détail — référencé par RG-015-015 | |

---

## Panneau de détail client

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Bouton fermeture | `Fermer` | Bouton gris dans l'en-tête du panneau de détail — referme le panneau — référencé par RG-015-021 | |
| Titre sous-section comptes | `Comptes bancaires` | En-tête de la colonne gauche du panneau de détail — référencé par RG-015-017 | |
| Message état vide comptes | `Aucun compte` | Affiché dans la colonne comptes lorsque le client n'a aucun compte bancaire — référencé par RG-015-019 | |
| En-tête de colonne | `Numero` | Colonne numéro de compte dans le tableau des comptes | |
| En-tête de colonne | `Type` | Colonne type de compte dans le tableau des comptes | |
| En-tête de colonne | `Solde` | Colonne solde dans le tableau des comptes | |
| Titre sous-section prêts | `Prets` | En-tête de la colonne droite du panneau de détail — référencé par RG-015-018 | |
| Message état vide prêts | `Aucun pret` | Affiché dans la colonne prêts lorsque le client n'a aucun prêt — référencé par RG-015-020 | |
| En-tête de colonne | `Type` | Colonne type de prêt dans le tableau des prêts | |
| En-tête de colonne | `Montant` | Colonne montant dans le tableau des prêts | |
| En-tête de colonne | `Mensualite` | Colonne mensualité dans le tableau des prêts | |
| En-tête de colonne | `Statut` | Colonne statut dans le tableau des prêts | |

---

## Valeurs d'énumération affichées (dynamiques)

Les valeurs suivantes sont affichées telles quelles (sans libellé localisé) depuis les énumérations Java.

### Statuts de prêt (`loan.status`)

| Valeur affichée | Mise en forme | Preuve |
|---|---|---|
| `EN_ATTENTE` | Badge fond jaune pâle, texte orange — CSS `.loan-EN_ATTENTE` | 319 |
| `EN_COURS` | Badge fond vert pâle, texte vert — CSS `.loan-EN_COURS` | 319 |
| `REFUSE` | Badge fond rose pâle, texte rouge foncé — CSS `.loan-REFUSE` | 319 |
| `REMBOURSE` | Badge fond mauve pâle, texte violet — CSS `.loan-REMBOURSE` | 319 |
| `DEFAUT` | Pas de classe CSS dédiée — affiché sans badge — référencé par RG-015-026 | 86, 318–319 |

### Types de compte (`acc.accountType`)

| Valeur affichée | Mise en forme | Preuve |
|---|---|---|
| `COURANT` | Badge fond bleu pâle, texte bleu — CSS `.account-type-badge` | 88, 270 |
| `EPARGNE` | Badge fond bleu pâle, texte bleu — CSS `.account-type-badge` | 88, 270 |
