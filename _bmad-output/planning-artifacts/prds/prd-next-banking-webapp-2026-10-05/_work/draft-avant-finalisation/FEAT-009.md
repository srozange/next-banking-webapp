### FEAT-009 — Bourse et watchlist

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) consulte la page bourse : il choisit une valeur de la watchlist et une période ("1 Semaine", "1 Mois", "1 An", "5 Ans"), et voit le cours, la variation, la performance, un graphique d'évolution et les dix dernières séances, issus du fournisseur de cotations AlphaVantage. La watchlist est unique et partagée par tous les utilisateurs ; on y ajoute une valeur par un formulaire intégré à la page et on la supprime sans confirmation. Cas limites : absence de données du fournisseur (message d'erreur contextuel), symbole en double, watchlist vidée. L'intégration avec le fournisseur est détaillée dans FEAT-029. Scénarios golden master : SC-009-01.

**Exigences fonctionnelles :**

#### FR-009-01 : Accès à la page bourse

Le système autorise l'accès à la page bourse à tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), sans autre restriction de rôle.

Source : RG-009-019

#### FR-009-02 : Chargement initial

Quand l'utilisateur ouvre la page bourse, le système charge la watchlist, sélectionne automatiquement sa première valeur (ordre : marché, puis symbole) et récupère son historique avant d'afficher la page.

Source : RG-009-001

#### FR-009-03 : Changement de valeur ou de période

Quand l'utilisateur change la valeur sélectionnée ("Valeur") ou la période ("Période" : "1 Semaine", "1 Mois", "1 An", "5 Ans"), le système récupère et affiche l'historique mis à jour sans recharger toute la page.

Source : RG-009-002

#### FR-009-04 : Indicateurs de marché

Quand un historique est disponible, le système affiche le libellé "{nom} — {symbole}" avec le cours actuel (clôture du point le plus récent), "Variation sur la période" (dernière clôture − première clôture, préfixée "+" si positive), "Performance %" ((dernière − première) / première × 100) et "Marché".

Source : RG-009-003

#### FR-009-05 : Couleur selon la performance

Quand la performance en pourcentage est supérieure ou égale à zéro, le système affiche le graphique et les indicateurs en vert (#2e7d32) avec le préfixe "▲ +" ; quand elle est strictement négative, en rouge (#E3000F) avec le préfixe "▼ ".

Source : RG-009-004

#### FR-009-06 : Graphique d'évolution du cours

Quand un historique est disponible, le système affiche, sous le titre "📈 Évolution du cours", un graphique vectoriel généré côté serveur avec cinq dates régulièrement espacées en abscisse au format MM-JJ, cinq lignes horizontales de grille et un badge de période : "1 semaine (données journalières)", "1 mois (données journalières)", "1 an (données hebdomadaires)" ou "5 ans (données hebdomadaires)".

Source : RG-009-005

#### FR-009-07 : Dernières séances

Quand un historique est disponible, le système affiche sous "📋 Dernières séances" les 10 derniers points, du plus récent au plus ancien, avec les colonnes "Date", "Ouverture", "Haut", "Bas", "Clôture" (2 décimales), "Volume" et "Var. J" ((clôture − ouverture) / ouverture × 100, préfixée "+" si clôture ≥ ouverture) ; chaque ligne est verte si clôture ≥ ouverture, rouge sinon.

Source : RG-009-006

#### FR-009-08 : Données demandées au fournisseur selon la période

Pour les périodes "1W" et "1M", le système demande au fournisseur la série journalière compacte (100 derniers jours) et ne garde que les points datés au plus tôt d'aujourd'hui moins 1 semaine ou moins 1 mois. Pour "1Y" et "5Y", il demande la série hebdomadaire et ne garde que les points datés au plus tôt d'aujourd'hui moins 1 an ou moins 5 ans. L'accès au fournisseur suit NFR-INT.

Source : RG-009-007, RG-009-008

#### FR-009-09 : Absence de données du fournisseur

Si le fournisseur répond avec un statut autre que succès, ou par un message d'information, de limite ou d'erreur, ou par une série vide, le système masque le graphique et le tableau et affiche "⚠️ Aucune donnée reçue pour « {SYMBOLE} » sur {libellé de période}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur.", où le libellé de période est celui de FR-009-06.

Source : RG-009-009

#### FR-009-10 : Watchlist unique et partagée

Le système gère une seule watchlist commune à tous les utilisateurs authentifiés, quel que soit leur rôle, sans filtrage par utilisateur, et la présente triée par marché croissant puis par symbole croissant. Elle s'affiche sous le titre "⭐ Ma Watchlist ({N} valeurs)" avec les colonnes "Symbole", "Nom", "Marché", "Devise", "Suppr.", et son nombre de valeurs apparaît à côté du sélecteur sous la forme "({N} valeurs)".

Source : RG-009-010, RG-009-011

#### FR-009-11 : Formulaire d'ajout et contrôles

Le bouton "+ Ajouter une valeur" ouvre dans la page le formulaire "➕ Ajouter une valeur à la watchlist" : "Symbole *" (exemple "ex: AMZN", aide "Symbole Alpha Vantage"), "Nom *" (exemple "ex: Amazon"), "Marché" ("Euronext Paris" par défaut, "NASDAQ", "NYSE", "London SE", "Autre"), "Devise" ("EUR" par défaut, "USD", "GBP"), boutons "Ajouter" et "Annuler". À la soumission, le formulaire reste ouvert et la soumission est rejetée avec le message "Le symbole est obligatoire (ex: AMZN, SG.PA)" si le symbole est vide ou blanc, "Le nom de la valeur est obligatoire" si le nom est vide ou blanc, et "Le symbole « {SYMBOLE} » existe déjà dans votre watchlist" (symbole en majuscules) si le symbole existe déjà, sans tenir compte de la casse.

Source : RG-009-012, RG-009-013, RG-009-014

#### FR-009-12 : Enregistrement d'une nouvelle valeur

Quand une valeur est ajoutée, le système enregistre le symbole en majuscules sans espaces de bord (20 caractères au plus), le nom sans espaces de bord (100 au plus), le marché sans espaces de bord (50 au plus) et la devise en majuscules sans espaces de bord (10 au plus), affiche "Valeur ajoutée !", sélectionne la nouvelle valeur, ferme le formulaire et récupère son historique. Le refus des symboles de plus de 20 caractères et des noms de plus de 100 est décrit par FR-029-13.

Source : RG-009-015, RG-009-016

#### FR-009-13 : Suppression d'une valeur

Le bouton "✕" (infobulle "Supprimer {nom} de la watchlist") supprime immédiatement la valeur, sans demande de confirmation. Si la valeur supprimée était sélectionnée, le système sélectionne la première valeur restante et récupère ses données ; si la watchlist devient vide, il efface la sélection et masque le graphique et le tableau.

Source : RG-009-017, RG-009-018

#### FR-009-14 : Clé d'accès au fournisseur de cotations hors du code source

Le système lit la clé d'accès AlphaVantage uniquement depuis une configuration sécurisée (variable d'environnement ou propriété système) et n'utilise jamais de valeur par défaut présente dans le code source. Ce comportement remplace le comportement legacy RG-009-021 (clé par défaut écrite en clair dans le code).

Critère d'acceptation (test) : aucune clé d'accès ne figure dans le code source ni dans les fichiers versionnés ; sans clé configurée, aucun appel réel n'est émis avec une clé par défaut.

Source : RG-009-021, BUG-034

**Notes :**
- RG-009-020 (dette legacy) : non reproduite. Bug conservé associé : BUG-033 (appel au fournisseur sans délai d'attente). Le délai d'attente est de toute façon imposé par NFR-INT.
- BUG-035 (nom de plus de 100 caractères : erreur technique) et BUG-036 (symbole de plus de 20 caractères tronqué sans avertissement) sont conservés ; le champ de saisie legacy limite déjà la longueur (voir FR-029-13).
- Recouvrement avec FEAT-029 (aucune fusion décidée) : FR-009-02 à FR-009-13 et FR-029-01 à FR-029-14 décrivent le même écran et doivent rester identiques.
- Le bandeau technique de la page legacy (pile technique et base de données) n'est pas repris.
