---
feature: FEAT-009
name: Bourse et watchlist
status: current
step: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/webapp/bourse.xhtml
  - src/main/java/com/accenture/bank/bourse/BourseBean.java
  - src/main/java/com/accenture/bank/bourse/AlphaVantageService.java
---

# MESSAGES — FEAT-009 Bourse et watchlist

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Bourse et watchlist » (`/bourse.xhtml`). Les `{variables}` désignent des
valeurs dynamiques interpolées à l'exécution.

---

## Page et navigation

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de page (onglet) | `BankApp Demo - Bourse` | `<title>` de la page | |
| Logo (initiales) | `DB` | Disque rouge en en-tête de barre de navigation | |
| Marque | `Demo Bank` | Nom de l'application dans la barre de navigation | |
| Action | `Déconnexion` | Lien de déconnexion en en-tête (commandLink) | |
| Lien de navigation | `Tableau de bord` | Menu principal — toujours visible | |
| Lien de navigation | `Virement` | Menu principal — toujours visible | |
| Lien de navigation | `Virements programmes` | Menu principal — toujours visible | |
| Lien de navigation | `Releve` | Menu principal — toujours visible | |
| Lien de navigation | `Budget` | Menu principal — toujours visible | |
| Lien de navigation | `Prets` | Menu principal — toujours visible | |
| Lien de navigation | `Documents` | Menu principal — toujours visible | |
| Lien de navigation (actif) | `Bourse` | Menu principal — page courante | |
| Lien de navigation | `Mon Conseiller` | Menu principal — toujours visible | |
| Lien de navigation | `Produits` | Menu principal — toujours visible | |
| Lien de navigation | `Comptes Tiers` | Menu principal — toujours visible | |
| Lien de navigation | `Beneficiaires` | Menu principal — toujours visible | |
| Lien de navigation conditionnel | `Espace Conseiller` | Menu principal — si `securityBean.adminOrConseiller` (rôles CONSEILLER ou ADMIN) | |
| Lien de navigation conditionnel | `Administration` | Menu principal — si `securityBean.admin` (rôle ADMIN uniquement) | |

---

## Bandeau technique

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Bandeau technique | `🌐 API externe — Données temps réel via Alpha Vantage REST API — EJB Stateless → HTTP → JSON → SVG \| 📦 Watchlist persistée en base H2 via JPA` | Toujours affiché sous la navigation ; non fonctionnel | |

---

## Barre d'outils

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Libellé | `Valeur` | Label du sélecteur de symbole | |
| Compteur watchlist | `({N} valeurs)` | Affiché à côté du label « Valeur » ; `{N}` = taille du catalogue — référencé par RG-009-010 | |
| Libellé | `Période` | Label du sélecteur de période | |
| Option de période | `1 Semaine` | Sélecteur de période — valeur `1W` | |
| Option de période | `1 Mois` | Sélecteur de période — valeur `1M` (sélectionnée par défaut) | |
| Option de période | `1 An` | Sélecteur de période — valeur `1Y` | |
| Option de période | `5 Ans` | Sélecteur de période — valeur `5Y` | |
| Bouton | `+ Ajouter une valeur` | Ouvre le formulaire d'ajout à la watchlist | |

---

## Formulaire d'ajout à la watchlist

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de formulaire | `➕ Ajouter une valeur à la watchlist` | En-tête du panneau d'ajout (`addPanel`) — affiché quand `showAddForm = true` | |
| Libellé de champ | `Symbole *` | Label du champ `newSymbol` | |
| Placeholder | `ex: AMZN` | Placeholder du champ `newSymbol` | |
| Aide contextuelle | `Symbole Alpha Vantage` | Texte d'aide sous le champ `newSymbol` | |
| Libellé de champ | `Nom *` | Label du champ `newName` | |
| Placeholder | `ex: Amazon` | Placeholder du champ `newName` | |
| Libellé de champ | `Marché` | Label du sélecteur de marché | |
| Option de marché | `Euronext Paris` | Sélecteur `newMarket` — valeur par défaut | |
| Option de marché | `NASDAQ` | Sélecteur `newMarket` | |
| Option de marché | `NYSE` | Sélecteur `newMarket` | |
| Option de marché | `London SE` | Sélecteur `newMarket` — valeur interne `LSE` | |
| Option de marché | `Autre` | Sélecteur `newMarket` | |
| Libellé de champ | `Devise` | Label du sélecteur de devise | |
| Option de devise | `EUR` | Sélecteur `newCurrency` — valeur par défaut | |
| Option de devise | `USD` | Sélecteur `newCurrency` | |
| Option de devise | `GBP` | Sélecteur `newCurrency` | |
| Bouton d'action | `Ajouter` | Soumet le formulaire d'ajout | |
| Bouton d'annulation | `Annuler` | Ferme le formulaire sans modification | |

---

## Messages d'erreur de validation (formulaire d'ajout)

Émis via `FacesMessage.SEVERITY_ERROR` sur le composant `bourseForm:addPanel`, affichés en zone `.alert-error`.

| Type | Texte exact | Condition déclenchante | Preuve |
|---|---|---|---|
| Erreur de saisie | `Le symbole est obligatoire (ex: AMZN, SG.PA)` | Champ `newSymbol` vide ou blanc — référencé par RG-009-012 | |
| Erreur de saisie | `Le nom de la valeur est obligatoire` | Champ `newName` vide ou blanc — référencé par RG-009-013 | |
| Erreur de doublon | `Le symbole « {SYMBOL} » existe déjà dans votre watchlist` | Symbole déjà présent en watchlist (casse insensible) — référencé par RG-009-014 ; `{SYMBOL}` = valeur saisie convertie en majuscules | |

---

## Message de confirmation (ajout réussi)

Émis via `FacesMessage.SEVERITY_INFO` sur le composant `bourseForm`.

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Confirmation | `Valeur ajoutée !` | Affiché après ajout réussi d'une valeur à la watchlist | |

---

## Panneau d'erreur de données de marché

Affiché dans la zone `.error-box` du panneau `errorPanel` quand `errorMessage` est non nul ; le préfixe emoji `⚠️` est rendu par le template XHTML.

| Type | Texte exact | Condition déclenchante | Preuve |
|---|---|---|---|
| Erreur de données | `⚠️ Aucune donnée reçue pour « {SYMBOL} » sur {PERIOD_LABEL}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur.` | Historique vide (HTTP non-200, champ API `Information`/`Note`/`Error Message`, ou série temporelle vide) — référencé par RG-009-009 | |

> `{PERIOD_LABEL}` est calculé par `getPeriodeLabel()` :
>
> | Valeur `selectedPeriod` | `{PERIOD_LABEL}` |
> |---|---|
> | `1W` | `1 semaine (données journalières)` |
> | `1M` | `1 mois (données journalières)` |
> | `1Y` | `1 an (données hebdomadaires)` |
> | `5Y` | `5 ans (données hebdomadaires)` |

---

## KPIs (panneau d'indicateurs)

Affiché dans `kpiPanel` quand `errorMessage` est vide et `history` non vide.

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Libellé KPI principal (dynamique) | `{name} — {symbol}` | Intitulé du KPI principal (fond sombre) ; `{name}` et `{symbol}` sont les champs de l'item sélectionné en watchlist | |
| Libellé KPI | `Variation sur la période` | Libellé du KPI de variation absolue | |
| Libellé KPI | `Performance %` | Libellé du KPI de performance en pourcentage | |
| Libellé KPI | `Marché` | Libellé du KPI de marché | |
| Indicateur directionnel | `▲ +` | Préfixe affiché devant la performance quand `variationPct >= 0` | |
| Indicateur directionnel | `▼ ` | Préfixe affiché devant la performance quand `variationPct < 0` | |
| Préfixe variation absolue | `+` | Préfixe affiché devant la variation absolue quand positive | |

---

## Section graphique

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de section | `📈 Évolution du cours` | En-tête de la section graphique SVG — affiché quand `svgChart` non vide | |
| Badge période (SVG) | `{PERIOD_LABEL}` | Badge texte en bas à droite du graphique SVG généré côté serveur ; même valeurs que la table ci-dessus | |

---

## Section « Dernières séances »

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de section | `📋 Dernières séances` | En-tête du tableau des 10 dernières séances — affiché quand `lastTenHistory` non vide | |
| En-tête de colonne | `Date` | Colonne date de séance | |
| En-tête de colonne | `Ouverture` | Colonne cours d'ouverture | |
| En-tête de colonne | `Haut` | Colonne cours le plus haut | |
| En-tête de colonne | `Bas` | Colonne cours le plus bas | |
| En-tête de colonne | `Clôture` | Colonne cours de clôture | |
| En-tête de colonne | `Volume` | Colonne volume échangé | |
| En-tête de colonne | `Var. J` | Colonne variation journalière (close − open) / open × 100 | |
| Préfixe variation journalière | `+` | Préfixe affiché quand close ≥ open | |

---

## Section watchlist

| Type | Texte exact | Contexte observé | Preuve |
|---|---|---|---|
| Titre de section | `⭐ Ma Watchlist ({N} valeurs)` | En-tête du tableau de la watchlist ; `{N}` = taille du catalogue — référencé par RG-009-010 | |
| En-tête de colonne | `Symbole` | Colonne symbole boursier | |
| En-tête de colonne | `Nom` | Colonne nom de la valeur | |
| En-tête de colonne | `Marché` | Colonne marché de cotation | |
| En-tête de colonne | `Devise` | Colonne devise | |
| En-tête de colonne | `Suppr.` | Colonne bouton de suppression | |
| Bouton de suppression | `✕` | Bouton de suppression par ligne — référencé par RG-009-017 | |
| Infobulle de suppression (dynamique) | `Supprimer {name} de la watchlist` | Attribut `title` du bouton de suppression ; `{name}` = nom de la valeur | |
