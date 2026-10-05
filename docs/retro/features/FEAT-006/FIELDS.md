# FEAT-006 — Budget et catégories de dépenses : champs de l'interface

_Source : snap MCP Playwright (CLIENT-charge.snap, CLIENT-erreur.snap, CLIENT-confirmation.snap) + budget.xhtml (XHTML source). Crawl : CLIENT (user1), CONSEILLER (sophie), ADMIN (admin)._

---

## Formulaire — Sélecteur de période (`monthForm`)

| Champ | Type | Label | Obligatoire | Valeur par défaut | Contraintes |
|---|---|---|---|---|---|
| `selectedMonth` | combobox | Periode : | non | mois courant (dynamique) | Options serveur (`budgetBean.availableMonths`) |

**Comportement** : le changement de valeur déclenche une soumission automatique via `onchange=submit()` avant même le clic sur « Afficher ». Le bouton « Afficher » (`changerMois`) est un second chemin de soumission.

**Options observées** (CLIENT/user1) : 2026-10 (sélectionné), 2025-11, 2025-10, 2025-09, 2025-08.  
**Options observées** (CONSEILLER/sophie, ADMIN/admin) : 2026-10 uniquement (aucune transaction historique pour ces comptes).

---

## Formulaire — Définir un plafond (`budgetForm`)

| Champ | JSF ID | Type | Label | Obligatoire | Défaut | Contraintes | Erreur |
|---|---|---|---|---|---|---|---|
| `editCategory` | `editCat` | combobox | Categorie | oui | -- Choisir -- | Enum `BudgetCategory` (libelle) | « Veuillez selectionner une categorie » |
| `editPlafond` | `editPlafond` | textbox | Plafond mensuel (EUR) | oui | (vide) | Numérique — `f:convertNumber` min/maxFractionDigits=2 ; placeholder « ex: 500 » | (non observé séparément) |

**Options de catégorie (statiques)** : Logement, Alimentation, Transport, Sante, Loisirs, Abonnements, Epargne, Revenus, Assurances, Autres (+ « -- Choisir -- » = valeur vide).

**Validation** : la soumission sans catégorie affiche le message JSF `h:messages globalOnly="true"` « Veuillez selectionner une categorie ». Aucune validation client-side (HTML5) observée.

**Action de succès** : message JSF « Plafond enregistre pour {Categorie} » ; compteur « Plafonds définis » incrémenté ; section « Plafonds du mois » créée ou mise à jour.

---

## Tableau — Plafonds du mois (lecture + action)

Conditionnel : `c:if not empty budgetBean.budgets`.

| Colonne | Source | Type |
|---|---|---|
| Categorie | `b.category.libelle` | texte |
| Plafond | `b.plafond` — `f:convertNumber type=currency currencySymbol=EUR` | montant EUR |
| Depense | `budgetBean.depenseCategorie[b.category]` — même converter | montant EUR |
| Ecart | `b.plafond - depenseCategorie` — `f:convertNumber` 2 décimales | montant EUR (couleur rouge si dépassement) |
| Action | Bouton « Supprimer » → `budgetBean.supprimerBudget(b.id)` | POST sans modal |

---

## Champs en lecture seule (résumé)

| Champ | Label affiché | Format |
|---|---|---|
| `totalDepenses` | Total depenses | `f:convertNumber type=currency currencySymbol=EUR minFractionDigits=2` |
| `totalRevenus` | Total revenus | idem |
| `budgets.size()` | Plafonds definis | entier |

---

## Affichage conditionnel par catégorie

Pour chaque catégorie dans `categoriesAvecDepenses` :
- **Sans plafond** : label « Aucun plafond defini » (italique gris).
- **Avec plafond** : barre de progression (`pourcentageCategorie`) colorée : vert < 80 %, orange 80–99 %, rouge ≥ 100 %.
- **Dépassement** (`depassement[cat] = true`) : tag « Depassement » (badge rouge), ligne en fond rosé, montant en rouge.
- **État vide** : si `empty budgetBean.categoriesAvecDepenses` → « Aucune depense enregistree sur cette periode ».

---

## Droits par rôle

| Rôle | Accès budget.xhtml | Formulaire plafond | Données affichées |
|---|---|---|---|
| CLIENT | oui | oui — plein | Transactions et plafonds du compte utilisateur |
| CONSEILLER | oui | oui — plein | Données propres (généralement vides pour sophie) |
| ADMIN | oui | oui — plein | Données propres (généralement vides pour admin) |

Aucune restriction fonctionnelle par rôle sur cette page : pas de `@RolesAllowed` spécifique au budget — accès contrôlé uniquement par AppFilter (utilisateur authentifié).
