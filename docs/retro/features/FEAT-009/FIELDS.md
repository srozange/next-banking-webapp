# FIELDS — FEAT-009 Bourse et watchlist

_Source : crawl MCP Playwright, rôle CLIENT — 2026-10-03_

---

## Champs principaux (toujours visibles)

| # | Nom fonctionnel | Type | Label affiché | Obligatoire | Valeur par défaut | Options / Contraintes |
|---|---|---|---|---|---|---|
| 1 | Symbole boursier | select | Valeur | non | AI.PA — Air Liquide | 9 valeurs fixées par la watchlist : AI.PA, BNP.PA, MC.PA, OR.PA, SAN.PA, TTE.PA, AAPL, GOOGL, MSFT |
| 2 | Période | select | Période | non | 1 Mois | 1 Semaine, 1 Mois, 1 An, 5 Ans |

## Champs dynamiques — formulaire « Ajouter une valeur »

Apparaissent au clic sur le bouton « + Ajouter une valeur » ; disparaissent après soumission ou annulation.

| # | Nom fonctionnel | Type | Label affiché | Obligatoire | Valeur par défaut | Contraintes / Validation |
|---|---|---|---|---|---|---|
| 3 | Symbole Alpha Vantage | text | Symbole | oui | — | Erreur serveur si vide : « Le symbole est obligatoire (ex: AMZN, SG.PA) » |
| 4 | Nom de la valeur | text | Nom | oui | — | Validation serveur |
| 5 | Marché | select | Marché | non | Euronext Paris | Euronext Paris, NASDAQ, NYSE, London SE, Autre |
| 6 | Devise | select | Devise | non | EUR | EUR, USD, GBP |

## Actions (boutons)

| Bouton | Déclencheur | Effet |
|---|---|---|
| + Ajouter une valeur | clic depuis état Chargé | Affiche le formulaire d'ajout en ligne |
| Ajouter | clic depuis formulaire ouvert | POST → ajoute la valeur à la watchlist ; rafraîchit la page ; pas de message de confirmation explicite |
| Annuler | clic depuis formulaire ouvert | Ferme le formulaire sans écriture |
| ✕ (Supprimer) | clic sur bouton dans ligne watchlist | POST → suppression immédiate sans confirmation dialog ; sélection automatique du premier item restant |

## Données affichées (lecture seule)

| Zone | Données |
|---|---|
| Fiche cours | Nom de la valeur, Cours actuel (EUR), Variation sur la période (EUR), Performance % (▼/▲), Marché |
| Graphique | Évolution SVG — axe Y : prix, axe X : dates (format MM-DD) — légende : « 1 mois (données journalières) » |
| Tableau séances | Date (YYYY-MM-DD), Ouverture, Haut, Bas, Clôture, Volume, Var. J (%) |
| Watchlist | Symbole (gras), Nom, Marché, Devise, Bouton ✕ |

## Activations dynamiques observées

- Formulaire « Ajouter une valeur » (champs 3 à 6) : apparaît au clic sur « + Ajouter une valeur » — observé à l'écran SCR-009-03_modale_ajout.png
- Formulaire disparaît après soumission réussie ou clic « Annuler »
- Combobox « Valeur » : se met à jour après ajout/suppression (options = watchlist courante)
  - **Bug UI** : le label « Valeur (N valeurs) » ne se met pas à jour après suppression — reste affiché à 9 même après suppression d'un item

## Notes de comportement remarquables

- La watchlist est la même pour tous les rôles (CLIENT, CONSEILLER, ADMIN) — aucun filtrage par utilisateur constaté
- La suppression ne demande pas de confirmation (pas de dialog)
- L'ajout d'une valeur ne produit pas de message flash de succès — confirmation implicite via rafraîchissement
- Données provenant de l'API AlphaVantage (externe) — en cas d'indisponibilité API, comportement erreur non observé
