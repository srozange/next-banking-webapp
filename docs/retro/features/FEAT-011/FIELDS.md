# FIELDS — FEAT-011 Catalogue produits bancaires

_Généré depuis fields-raw.json + produits.xhtml. Source : snapshot ADMIN-charge.snap._

## Champs éditables

Aucun — la page est 100 % lecture seule. Aucun champ de saisie utilisateur.

## Action disponible

| Élément | Type | Libellé | Effet |
|---|---|---|---|
| h:commandButton | bouton | Rafraîchir | Recharge `allProducts` et `recentJobs` depuis le serveur — reste sur la même page. Pas de mutation. |

## Colonnes affichées — Produits d'épargne (SavingsProduct)

| Colonne | Type données | Valeurs observées | Remarque |
|---|---|---|---|
| Nom | texte (strong) | CEL - Compte Epargne Logement, Livret A, Livret Demo Bank, PEL - Plan Epargne Logement | |
| Description | texte | texte libre | |
| Taux | décimal formaté `f:convertNumber` 2-4 décimales + % | 0,0175 %, 0,03 %, 0,025 %, 0,0225 % | `null` → `—` |
| Dépôt min | montant formaté `f:convertNumber type="currency" currencySymbol="EUR"` | EUR300, EUR10, EUR100, EUR225 | `null` → `—` |
| Capital garanti | booléen | Oui (badge-yes vert) / Non (badge-no rouge) | |
| Durée max | entier (mois) ou Illimité | Illimité (3 produits), 120 mois (PEL) | `null` → `Illimite` |

**Produits seedés (4)** : CEL - Compte Epargne Logement, Livret A, Livret Demo Bank, PEL - Plan Epargne Logement

## Colonnes affichées — Produits de crédit (CreditProduct)

| Colonne | Type données | Valeurs observées | Remarque |
|---|---|---|---|
| Nom | texte (strong) | Credit Auto Classique, Credit Immobilier Classique, Credit Personnel, Rachat de Credits | |
| Description | texte | texte libre | |
| Taux | décimal formaté 2-4 décimales + % | 0,0449 %, 0,0365 %, 0,0598 %, 0,0489 % | `null` → `—` |
| Durée max | entier (mois) | 72, 300, 84, 180 mois | `null` → `—` |
| LTV max | décimal % | — (2 produits), 80 %, 70 % | `null` → `—` |
| Assurance requise | booléen | Oui (badge-yes vert) / Non (badge-no rouge) | |

**Produits seedés (4)** : Credit Auto Classique, Credit Immobilier Classique, Credit Personnel, Rachat de Credits

## Colonnes affichées — Produits d'investissement (InvestmentProduct)

| Colonne | Type données | Valeurs observées | Remarque |
|---|---|---|---|
| Nom | texte (strong) | ETF Monde Diversifie, Fonds Euro Garanti, PEA Actions Europe, Private Equity Croissance, SCPI Rendement Plus | |
| Description | texte | texte libre | |
| Secteur | texte | Actions mondiales, Obligations souveraines, Actions Europe, Capital investissement, Immobilier tertiaire | `null` → `—` |
| Niveau de risque | libellé + score /5 | Moyen (3/5), Tres faible (1/5), Eleve (4/5), Tres eleve (5/5), Faible (2/5) | format : `#{p.riskLevelLabel} (#{p.riskLevel}/5)` |
| Rendement attendu | décimal formaté 2-4 décimales + % | 0,072 %, 0,028 %, 0,095 %, 0,15 %, 0,051 % | `null` → `—` |

**Produits seedés (5)** : ETF Monde Diversifie, Fonds Euro Garanti, PEA Actions Europe, Private Equity Croissance, SCPI Rendement Plus

## Colonnes affichées — Historique des traitements nocturnes (BatchJobLog)

| Colonne | Type données | Format | Remarque |
|---|---|---|---|
| Job | texte (strong) | texte libre | nom du job |
| Début | date-heure | `dd/MM/yyyy HH:mm:ss` (`f:convertDateTime`) | |
| Fin | date-heure | `dd/MM/yyyy HH:mm:ss` | |
| Durée (ms) | entier | millisecondes | |
| Enregistrements | entier | nb d'enregistrements traités | |
| Statut | enum | SUCCESS (badge vert), FAILED (badge rouge), PARTIAL (badge orange) | classe CSS `status-{job.status}` |
| Détails | texte | texte libre | |

**État au crawl** : section vide — "Aucun traitement nocturne enregistré — les jobs s'exécutent à 02h00, 02h30 et le 1er du mois à 03h00". Aucun batch n'a encore tourné dans l'environnement de crawl.

## Activation dynamique

Aucune — le catalogue est entièrement statique côté client. Le bouton Rafraîchir déclenche un rechargement serveur (PostConstruct équivalent : `chargerDonnees()` rechargé à la demande).

## Filtrage par type de produit

Le filtrage épargne / crédit / investissement est fait côté serveur via `p.productTypeLabel` dans la JSTL `c:if` — pas de filtre client-side observable.
