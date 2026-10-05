---
feature: FEAT-029
name: "Intégration AlphaVantage (cotations boursières)"
status: current
generated_by: retro-rules
date: 2026-10-03
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
sources:
  - src/main/webapp/bourse.xhtml
  - src/main/java/com/accenture/bank/bourse/BourseBean.java
  - src/main/java/com/accenture/bank/bourse/AlphaVantageService.java
---

# Messages et libellés — FEAT-029 Intégration AlphaVantage (cotations boursières)

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité Bourse (`/bourse.xhtml`). Les `{variables}` désignent des valeurs dynamiques
interpolées à l'exécution. Les messages de log (section 10) sont techniques et ne sont jamais
présentés à l'utilisateur.

---

## 1. En-tête et navigation

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-029-001 | `BankApp Demo - Bourse` | Titre du navigateur — toujours | |
| MSG-029-002 | `DB` | Disque logo en en-tête — toujours | |
| MSG-029-003 | `Demo Bank` | Marque dans la barre de navigation — toujours | |
| MSG-029-004 | `Déconnexion` | Lien de déconnexion — toujours | |

### Liens de navigation (toujours visibles)

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-029-010 | `Tableau de bord` | |
| MSG-029-011 | `Virement` | |
| MSG-029-012 | `Virements programmes` | |
| MSG-029-013 | `Releve` | |
| MSG-029-014 | `Budget` | |
| MSG-029-015 | `Prets` | |
| MSG-029-016 | `Documents` | |
| MSG-029-017 | `Bourse` (actif) | |
| MSG-029-018 | `Mon Conseiller` | |
| MSG-029-019 | `Produits` | |
| MSG-029-020 | `Comptes Tiers` | |
| MSG-029-021 | `Beneficiaires` | |

### Liens de navigation conditionnels

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-029-022 | `Espace Conseiller` | `securityBean.adminOrConseiller` = true (rôles CONSEILLER ou ADMIN) | |
| MSG-029-023 | `Administration` | `securityBean.admin` = true (rôle ADMIN uniquement) | |

---

## 2. Bandeau technique

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-029-025 | `🌐 API externe — Données temps réel via Alpha Vantage REST API — EJB Stateless → HTTP → JSON → SVG \| 📦 Watchlist persistée en base H2 via JPA` | Toujours affiché sous la navigation ; non fonctionnel | 143 |

---

## 3. Barre d'outils

| Identifiant | Texte exact (source) | Condition d'affichage | Preuve |
|---|---|---|---|
| MSG-029-030 | `Valeur` | Label du sélecteur de symbole — toujours | |
| MSG-029-031 | `({N} valeurs)` | Compteur en regard du label Valeur ; `{N}` = taille du catalogue — toujours | |
| MSG-029-032 | `Période` | Label du sélecteur de période — toujours | |
| MSG-029-033 | `1 Semaine` | Option de période — toujours | |
| MSG-029-034 | `1 Mois` | Option de période — toujours | |
| MSG-029-035 | `1 An` | Option de période — toujours | |
| MSG-029-036 | `5 Ans` | Option de période — toujours | |
| MSG-029-037 | `+ Ajouter une valeur` | Bouton d'ouverture du formulaire — toujours | |

---

## 4. Formulaire d'ajout de valeur à la watchlist

Affiché conditionnellement quand `bourseBean.showAddForm` = true (après clic sur MSG-029-037).

### Titre et champs

| Identifiant | Texte exact (source) | Type | Preuve |
|---|---|---|---|
| MSG-029-040 | `➕ Ajouter une valeur à la watchlist` | Titre du formulaire | |
| MSG-029-041 | `Symbole *` | Label du champ symbole | |
| MSG-029-042 | `ex: AMZN` | Placeholder du champ symbole | |
| MSG-029-043 | `Symbole Alpha Vantage` | Texte d'aide sous le champ symbole | |
| MSG-029-044 | `Nom *` | Label du champ nom | |
| MSG-029-045 | `ex: Amazon` | Placeholder du champ nom | |
| MSG-029-046 | `Marché` | Label du sélecteur de marché | |
| MSG-029-047 | `Euronext Paris` | Option de marché (valeur et libellé identiques) | |
| MSG-029-048 | `NASDAQ` | Option de marché | |
| MSG-029-049 | `NYSE` | Option de marché | |
| MSG-029-050 | `London SE` | Option de marché (libellé) pour valeur `LSE` | |
| MSG-029-051 | `Autre` | Option de marché | |
| MSG-029-052 | `Devise` | Label du sélecteur de devise | |
| MSG-029-053 | `EUR` | Option de devise | |
| MSG-029-054 | `USD` | Option de devise | |
| MSG-029-055 | `GBP` | Option de devise | |

### Boutons

| Identifiant | Texte exact (source) | Type | Preuve |
|---|---|---|---|
| MSG-029-056 | `Ajouter` | Bouton de soumission du formulaire | |
| MSG-029-057 | `Annuler` | Bouton de fermeture du formulaire | |

---

## 5. Messages d'erreur et de confirmation (watchlist et données)

Ces messages sont produits par `BourseBean` et affichés côté utilisateur via JSF Faces Messages
(erreurs) ou dans le panneau d'erreur de données.

### Erreurs de validation (ajout watchlist)

Affichés dans `bourseForm:addPanel` avec sévérité `SEVERITY_ERROR` — référencent RG-029-022, RG-029-023, RG-029-024.

| Identifiant | Texte exact (source) | Déclencheur | Preuve |
|---|---|---|---|
| MSG-029-060 | `Le symbole est obligatoire (ex: AMZN, SG.PA)` | Symbole vide ou nul à la soumission — référencé par RG-029-022 | |
| MSG-029-061 | `Le nom de la valeur est obligatoire` | Nom vide ou nul à la soumission — référencé par RG-029-023 | |
| MSG-029-062 | `Le symbole « {SYMBOLE} » existe déjà dans votre watchlist` | Symbole déjà présent (comparaison insensible à la casse) — référencé par RG-029-024 ; `{SYMBOLE}` est affiché en majuscules | |

### Message de confirmation (ajout réussi)

Affiché dans `bourseForm` avec sévérité `SEVERITY_INFO`.

| Identifiant | Texte exact (source) | Déclencheur | Preuve |
|---|---|---|---|
| MSG-029-063 | `Valeur ajoutée !` | Ajout d'une valeur à la watchlist réussi | |

### Message d'absence de données

Affiché dans le panneau `errorPanel` précédé de `⚠️` — référencé par RG-029-015.

| Identifiant | Texte exact (source) | Déclencheur | Preuve |
|---|---|---|---|
| MSG-029-064 | `Aucune donnée reçue pour « {symbole} » sur {périodeLabel}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur.` | Historique vide ou nul retourné par l'API — `{périodeLabel}` est l'un des libellés de la section 6 | 75 |

> Note : le préfixe `⚠️` est injecté par le gabarit et ne fait pas partie de
> la valeur de `errorMessage`. Le message final affiché est `⚠️ ` + MSG-029-064.

---

## 6. Libellés de période (dynamiques — utilisés dans les messages et dans le graphique SVG)

Ces libellés sont générés par `BourseBean.getPeriodeLabel()` et intégrés dans MSG-029-064
(message d'absence de données) et comme badge dans le graphique SVG (237).

| Identifiant | Texte exact (source) | Valeur de période | Preuve |
|---|---|---|---|
| MSG-029-070 | `1 semaine (données journalières)` | `1W` | |
| MSG-029-071 | `1 mois (données journalières)` | `1M` | |
| MSG-029-072 | `1 an (données hebdomadaires)` | `1Y` | |
| MSG-029-073 | `5 ans (données hebdomadaires)` | `5Y` | |

---

## 7. Panneau KPI

Affiché conditionnellement quand `bourseBean.errorMessage` est vide et `bourseBean.history` non vide.

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-029-080 | `{name} — {symbol}` | Label principal du KPI — `{name}` et `{symbol}` issus du `WatchlistItem` sélectionné | |
| MSG-029-081 | `Variation sur la période` | Label KPI variation absolue — toujours quand KPI visible | |
| MSG-029-082 | `Performance %` | Label KPI performance en pourcentage — toujours quand KPI visible | |
| MSG-029-083 | `Marché` | Label KPI marché — toujours quand KPI visible | |
| MSG-029-084 | `+` | Préfixe affiché devant la variation absolue quand `variationPct >= 0` — référencé par RG-029-019 | |
| MSG-029-085 | `▲ +` | Préfixe affiché devant la performance % quand `variationPct >= 0` — référencé par RG-029-019 | |
| MSG-029-086 | `▼ ` | Préfixe affiché devant la performance % quand `variationPct < 0` — référencé par RG-029-019 | |

---

## 8. Panneau graphique

Affiché conditionnellement quand `bourseBean.errorMessage` est vide et `bourseBean.svgChart` non vide.

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-029-090 | `📈 Évolution du cours` | Titre de section du graphique SVG | |

> Le badge de période affiché dans le graphique SVG est l'un des libellés MSG-029-070 à MSG-029-073,
> injecté par `BourseBean.generateSvg()` — 237.

---

## 9. Tableau des dernières séances

Affiché conditionnellement quand `bourseBean.errorMessage` est vide et `bourseBean.lastTenHistory` non vide.

| Identifiant | Texte exact (source) | Preuve |
|---|---|---|
| MSG-029-100 | `📋 Dernières séances` | Titre de la section tableau | |
| MSG-029-101 | `Date` | En-tête de colonne | |
| MSG-029-102 | `Ouverture` | En-tête de colonne | |
| MSG-029-103 | `Haut` | En-tête de colonne | |
| MSG-029-104 | `Bas` | En-tête de colonne | |
| MSG-029-105 | `Clôture` | En-tête de colonne | |
| MSG-029-106 | `Volume` | En-tête de colonne | |
| MSG-029-107 | `Var. J` | En-tête de colonne (variation journalière) | |

---

## 10. Section Watchlist

| Identifiant | Texte exact (source) | Condition | Preuve |
|---|---|---|---|
| MSG-029-110 | `⭐ Ma Watchlist ({N} valeurs)` | Titre de section — `{N}` = taille du catalogue — toujours | |
| MSG-029-111 | `Symbole` | En-tête de colonne | |
| MSG-029-112 | `Nom` | En-tête de colonne | |
| MSG-029-113 | `Marché` | En-tête de colonne | |
| MSG-029-114 | `Devise` | En-tête de colonne | |
| MSG-029-115 | `Suppr.` | En-tête de colonne (suppression) | |
| MSG-029-116 | `✕` | Texte du bouton de suppression par ligne | |
| MSG-029-117 | `Supprimer {name} de la watchlist` | Attribut `title` (infobulle) du bouton de suppression ; `{name}` = nom de la valeur | |

---

## 11. Messages de log technique (non visibles à l'utilisateur)

Émis par `AlphaVantageService`. Ne sont jamais exposés à l'interface.

| Identifiant | Texte exact (source) | Niveau | Déclencheur | Preuve |
|---|---|---|---|---|
| MSG-029-900 | `Erreur Alpha Vantage pour {symbol}` | ERROR | Exception lors de l'appel API — référencé par RG-029-008 | |
| MSG-029-901 | `Alpha Vantage message pour {symbol}: {message}` | WARN | Champ `Information`, `Note` ou `Error Message` présent dans la réponse JSON — référencé par RG-029-006 | |
| MSG-029-902 | `Alpha Vantage HTTP {status} ({elapsed}ms) pour {url}` | WARN | Statut HTTP ≠ 200 — référencé par RG-029-007 | |
| MSG-029-903 | `Alpha Vantage HTTP {status} en {elapsed}ms` | DEBUG | Statut HTTP = 200 (appel réussi) | |
| MSG-029-904 | `Alpha Vantage DAILY: {N} points pour {symbol} periode={period}` | INFO | Récupération journalière réussie — référencé par RG-029-002 | |
| MSG-029-905 | `Alpha Vantage WEEKLY: {N} points pour {symbol} periode={period}` | INFO | Récupération hebdomadaire réussie — référencé par RG-029-001 | |
