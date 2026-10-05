### FEAT-011 — Catalogue produits bancaires

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) consulte, en lecture seule, le catalogue des produits bancaires actifs, réparti en trois sections (épargne, crédit, investissement) et trié par nom, ainsi que l'historique des traitements nocturnes. Un bouton "Rafraichir" recharge ces données. Cas marquants : valeurs absentes remplacées par "—" (ou "Illimite" pour la durée d'un produit d'épargne), sections vides, taux affichés en pourcentage, historique de traitements vide. Scénarios golden master : SC-011-01.

**Exigences fonctionnelles :**

#### FR-011-01 : Catalogue en lecture seule

Le système présente le catalogue des produits sous le titre "Catalogue des produits" comme une vue en lecture seule, sans aucun champ de saisie.

Source : RG-011-002

#### FR-011-02 : Chargement et rafraîchissement

À l'ouverture de la session d'un utilisateur authentifié, le système charge le catalogue des produits et l'historique des traitements. Quand l'utilisateur actionne le bouton "Rafraichir", le système recharge le catalogue et l'historique et reste sur la même page.

Source : RG-011-003, RG-011-004

#### FR-011-03 : Produits affichés, tri et sections

Le système n'affiche que les produits actifs, triés par nom en ordre alphabétique, répartis selon leur type en trois sections : "Produits d epargne" (colonnes "Nom", "Description", "Taux", "Depot min", "Capital garanti", "Duree max"), "Produits de credit" ("Nom", "Description", "Taux", "Duree max", "LTV max", "Assurance requise") et "Produits d investissement" ("Nom", "Description", "Secteur", "Niveau de risque", "Rendement attendu"). Le système sait récupérer les produits d'un seul type (épargne, crédit ou investissement). Les colonnes booléennes affichent "Oui" ou "Non" ; une durée renseignée s'affiche "{n} mois".

Source : RG-011-005, RG-011-006, RG-011-007, RG-011-020

#### FR-011-04 : Valeurs absentes

Si un champ facultatif d'un produit est vide (taux d'intérêt, dépôt minimal d'ouverture, LTV maximale, secteur, rendement attendu, durée maximale d'un produit de crédit), le système affiche "—" à sa place, sauf la durée maximale vide d'un produit d'épargne, qui s'affiche "Illimite".

Source : RG-011-008, RG-011-009

#### FR-011-05 : Taux et rendements en pourcentage

Le système affiche le taux d'intérêt et le rendement attendu d'un produit comme un pourcentage à deux décimales avec virgule (par exemple, une valeur stockée de 0,0300 s'affiche "3,00 %"). Ce comportement remplace le comportement legacy RG-011-010, qui affichait la fraction brute suivie de "%" (valeur cent fois trop petite).

Critère d'acceptation (test) : un produit dont le taux stocké vaut 0,0300 affiche "3,00 %" dans la colonne "Taux" ; un produit d'investissement dont le rendement attendu stocké vaut 0,0650 affiche "6,50 %" dans la colonne "Rendement attendu".

Source : RG-011-010, BUG-039

#### FR-011-06 : Niveau de risque d'un produit d'investissement

Le système affiche le niveau de risque sous la forme "{libellé} ({score}/5)", avec l'échelle 1 = "Tres faible", 2 = "Faible", 3 = "Moyen", 4 = "Eleve", 5 = "Tres eleve" ; un score hors de 1 à 5 affiche le libellé "N/A".

Source : RG-011-011

#### FR-011-07 : Valeurs par défaut des produits

À sa création, un produit d'épargne a la garantie du capital à vrai par défaut, et un produit de crédit a l'assurance requise à faux par défaut.

Source : RG-011-012, RG-011-013

#### FR-011-08 : Sections vides

Si aucun produit d'un type n'est disponible, la section correspondante affiche "Aucun produit d epargne disponible", "Aucun produit de credit disponible" ou "Aucun produit d investissement disponible". Si aucun traitement n'a été enregistré, la section d'historique affiche "Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00". [ASSUMPTION A-011-1 : le message vide d'une section s'affiche dès que ce type n'a aucun produit, comme le dit la règle ; la page legacy ne l'affiche que si tout le catalogue est vide.]

Source : RG-011-014, RG-011-015

#### FR-011-09 : Historique des traitements nocturnes

Le système affiche, sous le titre "Historique des traitements nocturnes", au plus les 50 dernières exécutions, de la plus récente à la plus ancienne (heure de début), avec les colonnes "Job", "Debut", "Fin", "Duree (ms)", "Enregistrements", "Statut", "Details". Le statut enregistré vaut "SUCCESS", "FAILED" ou "PARTIAL". La durée vaut fin moins début, en millisecondes, et vaut 0 si le début ou la fin est absent. Le séparateur de section affiche "Traitements nocturnes". [ASSUMPTION A-011-2 : les deux titres de section legacy (MSG-011-100, MSG-011-101) perdent la mention technique « (EJB @Schedule) », propre à la pile legacy et sans sens pour l'utilisateur ; écart de texte attendu au golden master]

Source : RG-011-016, RG-011-017, RG-011-018, RG-011-019

**Notes :**
- RG-011-001 : couverte par l'exigence fusionnée de FEAT-010 (règles voisines).
- Le bandeau technique de la page legacy (pile technique, modèle d'héritage des produits) n'est pas repris.
