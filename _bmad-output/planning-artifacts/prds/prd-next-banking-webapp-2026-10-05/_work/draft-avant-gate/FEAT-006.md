### FEAT-006 — Budget et catégories de dépenses

**Description :** La page Budget est accessible à tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN), avec la même interface pour tous ; chacun n'y voit que ses propres données. Elle ventile les dépenses (opérations au débit) d'un mois choisi par catégorie, à partir de mots-clés reconnus dans le libellé des opérations, et affiche les totaux de dépenses et de revenus du mois. L'utilisateur peut définir un plafond mensuel par catégorie, suivre sa consommation par une barre de progression colorée (vert, orange, rouge) et voir les dépassements. Cas limites marquants : utilisateur sans historique (mois courant affiché), mois sans dépense, catégorie sans plafond, dépenses égales ou supérieures au plafond, suppression d'un plafond sans confirmation. Scénarios golden master : SC-006-01.

**Exigences fonctionnelles :**

#### FR-006-01 : Données limitées à l'utilisateur connecté

Toutes les données de la page Budget (plafonds, ventilation des dépenses, totaux, liste des périodes) portent exclusivement sur le compte client de l'utilisateur authentifié. Un utilisateur ne voit jamais les opérations ni les plafonds d'un autre client.

Source : RG-006-001

#### FR-006-02 : Choix de la période

Le sélecteur « Periode : » propose, au format AAAA-MM, les 24 mois les plus récents qui comptent au moins une opération de l'utilisateur connecté. À l'ouverture de la page, la période sélectionnée est le mois le plus récent comportant des opérations ; si l'utilisateur n'a aucune opération, c'est le mois civil en cours. Dès que l'utilisateur change la valeur du sélecteur, toutes les données de budget et de dépenses sont rechargées pour la nouvelle période, sans action de validation ; le bouton « Afficher » permet aussi de recharger.

Source : RG-006-002, RG-006-003, RG-006-004, RG-006-005

#### FR-006-03 : Ventilation des dépenses par catégorie

La section « Repartition par categorie », titrée « Depenses du mois {AAAA-MM} », ventile uniquement les opérations au débit de l'utilisateur pour le mois sélectionné. Chaque opération est rattachée à la première catégorie dont un mot-clé apparaît dans son libellé, sans tenir compte de la casse ; une opération qui ne correspond à aucun mot-clé est rattachée à la catégorie « Autres ». Seules les catégories dont le montant de dépenses est non nul sont affichées. Si l'utilisateur n'a aucune dépense sur la période, la page affiche « Aucune depense enregistree sur cette periode ».

Source : RG-006-006, RG-006-007, RG-006-008, RG-006-009, RG-006-010

#### FR-006-04 : Totaux du mois

La page affiche trois indicateurs : « Total depenses » (somme des montants des opérations au débit de l'utilisateur pour le mois sélectionné), « Total revenus » (somme des montants des opérations au crédit de l'utilisateur pour le mois sélectionné), tous deux en EUR avec deux décimales, et « Plafonds definis » (nombre de plafonds définis pour le mois sélectionné).

Source : RG-006-011, RG-006-012

#### FR-006-05 : Contrôles du formulaire de plafond

Le formulaire « Definir un plafond » comporte le champ « Categorie » (option vide « -- Choisir -- ») et le champ « Plafond mensuel (EUR) » (exemple affiché « ex: 500 »), et le bouton « Enregistrer le plafond ». Si aucune catégorie n'est choisie, l'enregistrement est refusé avec le message « Veuillez selectionner une categorie ». Si le plafond est vide, nul ou négatif, l'enregistrement est refusé avec le message « Veuillez saisir un plafond valide ». Une saisie non numérique du plafond est refusée.

Source : RG-006-013, RG-006-014

#### FR-006-06 : Catégories proposées pour un plafond, sans la catégorie Revenus

Le sélecteur de catégorie du formulaire de plafond propose uniquement des catégories de dépenses : Logement, Alimentation, Transport, Sante, Loisirs, Abonnements, Epargne, Assurances, Autres. La catégorie de revenus « Revenus » n'est pas proposée, car un plafond de dépenses ne peut pas s'appliquer à des revenus (remplace le comportement legacy RG-006-028, qui proposait « Revenus » et produisait un plafond jamais atteint).

Critère d'acceptation (test) : à l'ouverture de la page Budget, la liste des options du champ « Categorie » contient « -- Choisir -- » et les neuf catégories de dépenses ci-dessus, et ne contient pas « Revenus » ; une tentative d'enregistrer un plafond pour la catégorie Revenus est refusée.

Source : RG-006-028, BUG-023

#### FR-006-07 : Enregistrement d'un plafond

Il existe au plus un plafond par combinaison (client, catégorie, mois). À l'enregistrement d'un plafond valide pour le mois sélectionné : si un plafond existe déjà pour ce client, cette catégorie et ce mois, sa valeur est remplacée par la nouvelle ; sinon, un nouveau plafond est créé. En cas de succès, la page affiche « Plafond enregistre pour {categorie} » ({categorie} = libellé de la catégorie choisie), vide les champs catégorie et plafond, et rafraîchit la liste des plafonds.

Source : RG-006-015, RG-006-016, RG-006-017, RG-006-018

#### FR-006-08 : Suppression d'un plafond

Chaque ligne du tableau des plafonds porte un bouton « Supprimer ». Son activation supprime immédiatement le plafond, sans demande de confirmation.

Source : RG-006-019

#### FR-006-09 : Barre de progression par catégorie

Pour chaque catégorie affichée qui a un plafond défini, la page affiche une barre de progression et le libellé « {pourcentage}% du plafond » ({pourcentage} = dépenses / plafond × 100, en entier). La barre est verte en dessous de 80 % du plafond, orange de 80 % à 99 % inclus, rouge à partir de 100 %. Le remplissage affiché de la barre est plafonné à 100 %, même quand les dépenses dépassent le plafond. Pour une catégorie sans plafond, la zone de progression affiche « Aucun plafond defini ».

Source : RG-006-020, RG-006-021, RG-006-022, RG-006-023, RG-006-025

#### FR-006-10 : Signalement d'un dépassement

Quand les dépenses d'une catégorie sont strictement supérieures à son plafond, la ligne affiche le badge « Depassement », est surlignée sur fond rose et le montant dépensé est affiché en rouge. Des dépenses exactement égales au plafond donnent une barre rouge (FR-006-09) sans badge « Depassement ».

Source : RG-006-024, RG-006-022

#### FR-006-11 : Tableau des plafonds du mois

Le tableau « Plafonds du mois {AAAA-MM} » n'est affiché que si au moins un plafond est défini pour le mois sélectionné. Ses colonnes sont « Categorie », « Plafond », « Depense », « Ecart » et « Action ». L'écart vaut plafond moins dépenses ; il est affiché en rouge quand il est négatif et en vert quand il est positif ou nul.

Source : RG-006-026, RG-006-027

**Feature-specific NFRs :** En cas d'erreur inattendue lors de l'enregistrement ou de la suppression d'un plafond, aucun message technique interne n'est montré à l'utilisateur (le legacy affiche « Erreur : {message technique} » et « Erreur suppression : {message technique} », non reproduits). Un plafond qui dépasse la capacité de stockage du montant (13 chiffres entiers, 2 décimales) est refusé par une validation explicite, pas par une erreur technique.

**Notes :**
- RG-006-028 (Suspect, dette legacy) : remplacée par FR-006-06 (correction BUG-023).
- Le bandeau technique du legacy (mention des technologies) n'est pas reproduit.
