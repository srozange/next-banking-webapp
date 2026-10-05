### FEAT-029 — Intégration AlphaVantage (cotations boursières)

**Description :** Capacité transverse qui alimente la page bourse (FEAT-009), utilisée par tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) : récupération de l'historique des cours auprès du fournisseur AlphaVantage selon la période, filtrage et tri des points, calculs affichés et gestion de la watchlist. Toute réponse en échec du fournisseur est traitée comme une absence de données, sans erreur propagée à l'utilisateur. La clé d'accès au fournisseur ne figure jamais dans le code source. L'accès au fournisseur (port, adaptateur simulé, délai d'attente, coupe-circuit, cache) suit NFR-INT. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-029-01 : Série demandée selon la période

Pour les périodes "1Y" et "5Y", le système demande au fournisseur la série hebdomadaire ; pour "1W" et "1M", la série journalière compacte (100 jours). Pour une période qui n'est aucune des quatre, le système applique une date limite par défaut d'un mois. Voir NFR-INT pour l'accès au fournisseur.

Source : RG-029-001, RG-029-002, RG-029-003

#### FR-029-02 : Filtrage et tri des points

Le système exclut tout point dont la date est strictement antérieure à la date limite de la période et renvoie les points triés par date croissante.

Source : RG-029-004, RG-029-005

#### FR-029-03 : Réponses du fournisseur en échec

Si la réponse contient un champ d'information, de note ou de message d'erreur, si le statut n'est pas un succès, ou si une erreur quelconque survient pendant l'appel, le système journalise un avertissement ou une erreur et traite le résultat comme une absence de données, sans propager l'erreur. Une absence de données affiche le message "⚠️ Aucune donnée reçue pour « {symbole} » sur {libellé de période}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur." (libellés de période : "1 semaine (données journalières)", "1 mois (données journalières)", "1 an (données hebdomadaires)", "5 ans (données hebdomadaires)").

Source : RG-029-006, RG-029-007, RG-029-008, RG-029-015

#### FR-029-04 : Adresse du fournisseur configurable

Au démarrage, le système lit l'adresse du fournisseur dans le paramètre ALPHA_VANTAGE_BASE_URL (variable d'environnement, puis propriété système), avec la valeur par défaut "https://www.alphavantage.co/query".

Source : RG-029-010

#### FR-029-05 : Clé d'accès protégée

Au démarrage, le système lit la clé d'accès dans le paramètre ALPHA_VANTAGE_API_KEY (variable d'environnement, puis propriété système) et ne se replie jamais sur une valeur écrite dans le code source : la clé n'est jamais exposée dans le code. Ce comportement remplace le comportement legacy RG-029-009 (repli sur une clé par défaut écrite en dur). [ASSUMPTION A-029-1 : sans clé configurée, aucun appel réel n'est émis et la page se comporte comme une absence de données (FR-029-03).]

Critère d'acceptation (test) : aucune clé d'accès ne figure dans le code source ni dans les fichiers versionnés ; au démarrage sans clé configurée, aucun appel n'est émis avec une clé par défaut.

Source : RG-029-009, RG-029-012, BUG-091

#### FR-029-06 : Période par défaut et première valeur

Au premier chargement de la page bourse, le système présélectionne la période "1M" ("1 Mois") et, si la watchlist n'est pas vide, charge automatiquement les données de sa première valeur.

Source : RG-029-013, RG-029-014

#### FR-029-07 : Calculs et indicateur de variation

Quand un historique est disponible, le cours actuel est la clôture du dernier point, la variation est (dernière clôture − première clôture) et la variation en pourcentage est ((dernière clôture − première clôture) / première clôture) × 100. Le système affiche la variation en positif (vert, "▲") quand la variation en pourcentage est supérieure ou égale à zéro, en négatif (rouge, "▼") sinon (même règle que FR-009-05).

Source : RG-029-016, RG-029-017, RG-029-018, RG-029-019

#### FR-029-08 : Dernières séances et graphique

Le tableau des dernières séances affiche au plus les 10 points les plus récents, du plus récent au plus ancien. Le graphique est généré côté serveur et intégré à la page, au format 800 × 300, avec 5 lignes horizontales de grille et 5 dates (même exigence que FR-009-06 et FR-009-07).

Source : RG-029-020, RG-029-021

#### FR-029-09 : Contrôles à l'ajout d'une valeur

Le système rejette un ajout à la watchlist avec "Le symbole est obligatoire (ex: AMZN, SG.PA)" si le symbole est vide ou absent, "Le nom de la valeur est obligatoire" si le nom est vide ou absent, et "Le symbole « {SYMBOLE} » existe déjà dans votre watchlist" (symbole en majuscules) si le symbole existe déjà, sans tenir compte de la casse.

Source : RG-029-022, RG-029-023, RG-029-024

#### FR-029-10 : Normalisation et sélection après ajout

Le système enregistre le symbole en majuscules sans espaces de bord, le nom et le marché sans espaces de bord ; après un ajout réussi ("Valeur ajoutée !"), il sélectionne la nouvelle valeur et charge immédiatement ses données.

Source : RG-029-025, RG-029-026

#### FR-029-11 : Ordre de la watchlist

Le système renvoie les valeurs de la watchlist triées par marché puis par symbole.

Source : RG-029-027

#### FR-029-12 : Suppression d'une valeur sélectionnée

Quand l'utilisateur supprime la valeur sélectionnée et que la watchlist n'est pas vide, le système sélectionne la première valeur restante et charge ses données. Quand il supprime la dernière valeur, le système efface la sélection, l'historique et le graphique.

Source : RG-029-028, RG-029-029

#### FR-029-13 : Longueur du symbole et du nom

Le système refuse un symbole de plus de 20 caractères et un nom de plus de 100 caractères ; les champs "Symbole *" et "Nom *" limitent la saisie à ces longueurs. [ASSUMPTION A-029-2 : le refus passe par la limitation de saisie du champ, sans message d'erreur dédié ; le corpus n'en donne aucun.]

Source : RG-029-030, RG-029-031

#### FR-029-14 : Marchés et devises autorisés

Le système n'accepte comme marché que "Euronext Paris", "NASDAQ", "NYSE", "LSE" (libellé affiché "London SE") ou "Autre", et comme devise que "EUR", "USD" ou "GBP".

Source : RG-029-032, RG-029-033

**Notes :**
- RG-029-011 (dette legacy) : non reproduite. Bug conservé associé : BUG-090 (appel sans délai d'attente). Le délai d'attente est imposé par NFR-INT.
- Les messages de journalisation techniques (par exemple "Erreur Alpha Vantage pour {symbol}") ne sont jamais affichés à l'utilisateur : non repris comme exigence d'affichage.
- Recouvrement avec FEAT-009 (aucune fusion décidée) : les FR de cette fonctionnalité et celles de FEAT-009 décrivent le même écran et doivent rester identiques.
- Écart corpus : la validation de FEAT-009 indique qu'aucune limite de longueur n'est appliquée à la saisie, alors que la page legacy limite le symbole à 20 caractères et le nom à 100 ; les bugs BUG-035 et BUG-036 (conservés) décrivent le comportement serveur au-delà de ces limites.
