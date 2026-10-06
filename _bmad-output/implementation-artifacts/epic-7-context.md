# Epic 7 Context: Bourse, conseiller virtuel et catalogue des produits

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Donner à tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) trois écrans communs, identiques pour les trois rôles : `/bourse` (watchlist commune, indicateurs, graphique SVG généré par le serveur, dernières séances, ajout et retrait de valeurs), `/mon-conseiller` (conversation avec Sophie, conseillère virtuelle ; une panne du service d'IA s'annonce dans la conversation sans code technique, BUG-038 corrigé) et `/produits` (catalogue des produits actifs et historique des traitements nocturnes). L'epic ouvre deux nouveaux services, `bourse` et `open-banking-conseiller`, et pose le premier raccordement de fournisseurs externes (AlphaVantage, Azure OpenAI) derrière un port, avec adaptateur simulé actif par défaut, délai d'attente, coupe-circuit et cache des cotations ; ce socle sera réutilisé par Powens.

## Stories

- Story 7.1: Ouvrir le service bourse et gérer la watchlist commune
- Story 7.2: Raccorder le fournisseur de cotations AlphaVantage derrière un port simulé, avec coupe-circuit et cache
- Story 7.3: Générer le graphique d'évolution du cours côté serveur
- Story 7.4: Afficher l'écran Bourse
- Story 7.5: Ouvrir le service open-banking-conseiller et exposer le catalogue des produits et le journal des traitements
- Story 7.6: Afficher l'écran Produits
- Story 7.7: Raccorder Azure OpenAI derrière un port simulé et exposer l'échange avec Sophie
- Story 7.8: Dialoguer avec Sophie sur l'écran Mon Conseiller

## Requirements & Constraints

- Aucun test ni contrôle ne dépend du réseau ni d'une clé : `mvn -o verify` doit passer sans clé définie. Le mode réel n'est testé que contre un bouchon local.
- Jamais de `mode=reel` committé : `! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' services/*/src/main/resources compose*.yaml` doit sortir en code 0.
- Secrets (`ALPHA_VANTAGE_API_KEY`, `AZURE_OPENAI_API_KEY`) uniquement en variable d'environnement ou propriété système, jamais en valeur par défaut versionnée. Des tests tagués (BUG-091 pour la bourse, FR-010-08 pour le conseiller) doivent échouer si une clé par défaut est ajoutée dans `application.yml`. Sans clé, aucun appel n'est émis : la bourse renvoie une liste vide, le chat renvoie le message d'indisponibilité.
- Un fournisseur indisponible (lent, 5xx, quota, réponse illisible, coupe-circuit ouvert) ne bloque jamais l'écran. Côté bourse : 200 avec liste de points vide, échec journalisé avec `traceId`. Côté chat : 503 ou 502 `{"message"}` avec les textes legacy « Je suis momentanement indisponible… », sans « 401 », « 403 » ni pile d'appels.
- Limite AlphaVantage : 25 requêtes par jour et 5 par minute. Le cache sert 1W et 1M avec la même série journalière compacte.
- Performance au 95e centile, en local, avec les simulateurs : page bourse < 3 000 ms, navigation < 200 ms.
- Les montants, taux et rendements sont transmis en décimal exact sous forme de chaîne (ex. `"0.0300"`) ; le front les met en forme (« 3,00 % », BUG-039).
- Textes repris à l'identique du legacy, casse et absence d'accents comprises (« Produits d epargne », « Illimite », « Tres eleve (5/5) », « momentanement »). Les textes nouveaux viennent de `textes-nouveaux.md`, lot « Stories » : `api.erreur.400` « Requête invalide », `bourse.graphique.nom`, `conseiller.consigne` (consigne système à valider par le métier).
- Données de référence : bourse 9 valeurs ; open-banking-conseiller 13 produits (4 d'épargne, 4 de crédit, 5 d'investissement). Les dates relatives sont calculées depuis le port Horloge.
- Arbitrages confirmés le 2026-10-05 : un symbole ou un nom trop long est bloqué par la limite du champ, sans message (A-029-2) ; un type sans produit affiche son message « aucun produit » (A-011-1) ; « erreur d'authentification » de l'IA couvre 401 et 403 (A-010-1) ; le champ du chat n'est pas vidé après l'envoi (A-010-2) ; les actions rapides sont masquées après usage et réaffichées par « Nouvelle conversation » (A-010-3).
- Chaque FR et chaque bug corrigé (BUG-034, BUG-038, BUG-039, BUG-091) est couvert par des tests tagués, lancés par `-Dgroups='<tag>'` (Maven) ou `npx vitest run -t '<tag>'` (front).

## Technical Decisions

- Nouveaux services hexagonaux. `bourse` : port 8087, possède `WATCHLIST_ITEMS`, base `postgres-bourse`. `open-banking-conseiller` : port 8088, possède `BANK_PRODUCTS`, `SAVINGS_PRODUCTS`, `CREDIT_PRODUCTS` et `INVESTMENT_PRODUCTS`, base `postgres-open-banking-conseiller`. Chacun a son volume `pgdata-<id>`, ses migrations Flyway, son `reference-data/reference-data.sql` hors migrations, le profil `rejeu` dans `compose.rejeu.yaml`, et un renvoi `/api/<id>/**` par le proxy Vite et le conteneur web. Chacun livre les tests `HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest` et `SecuriteIT`, et sa vérification du jeton JWT (resource server). Tout `/api/**` est authentifié.
- Fournisseurs : un port sortant par fournisseur, avec un adaptateur réel et un adaptateur simulé dans `adapter.out.fournisseur`, nommés `<Techno><Port>` (`AlphaVantageSimule`, `AzureOpenAiSimule`). L'adaptateur simulé est actif sans aucune propriété `nb.fournisseurs.*`, ce que prouve un test de configuration ; le mode réel s'active par `nb.fournisseurs.<nom>.mode=reel`. Seules les classes de `adapter.out.fournisseur` peuvent dépendre d'un client HTTP.
- Coupe-circuit et cache (AR-102, point encore ouvert) : la story 7.2 choisit la bibliothèque, l'inscrit dans `versions-verifiees.md` (version, URL, date, compatibilité Spring Boot 4.1.1), et la story 7.7 la réutilise. Les réglages se placent sous `nb.fournisseurs.alphavantage.*` et `nb.fournisseurs.azure-openai.*`, validés au démarrage : une valeur invalide empêche le démarrage.
- Simulateurs. AlphaVantage renvoie des séries fixes par symbole, datées depuis le port Horloge ; un symbole inconnu donne une liste vide. Azure OpenAI renvoie une réponse fixe et offre un mode « refus d'authentification » 401/403, qui remplace le scénario SC-010-02, exclu du golden master.
- Adresses des fournisseurs : `ALPHA_VANTAGE_BASE_URL` vaut `https://www.alphavantage.co/query` par défaut. Pour `AZURE_OPENAI_ENDPOINT`, `_DEPLOYMENT` et `_API_VERSION`, la variable d'environnement l'emporte, puis la propriété système, puis la valeur par défaut du legacy. Azure : connexion 15 s, lecture 30 s, 500 tokens, température 0,7.
- Historique : 1W et 1M en série journalière, 1Y et 5Y en série hebdomadaire. La date limite est calculée depuis l'Horloge ; une période inconnue vaut un mois. Le graphique SVG (800 × 300, courbe verte `#2e7d32` ou rouge `#E3000F`) est produit par le service bourse et absent si l'historique est vide.
- Chat sans état serveur : aucune table de chat. Le front envoie l'historique de la conversation, sans le message de bienvenue ni le message en cours.
- Journal des traitements : lu directement au cœur bancaire par `GET /api/coeur-bancaire/traitements?limite=50`, ouvert à tout utilisateur authentifié, du plus récent au plus ancien. Aucun service ne projette ce journal.
- Pas d'appel HTTP entre services. L'écran Produits appelle les deux services en parallèle, jamais en cascade.
- Temps : uniquement par le port Horloge (fuseau Europe/Paris) ; aucun `now()` hors de `adapter.out.horloge`.

## UX & Interaction Patterns

- Routes figées : `/bourse`, `/mon-conseiller`, `/produits`, ouvertes par les liens du header. Un utilisateur non authentifié est renvoyé vers `/login`. Titre d'onglet « BankApp Demo - {page} ».
- `data-testid` aux préfixes `bourse-`, `chat-` et `produits-` (`chat-saisie`, `chat-envoyer`, `chat-message-utilisateur`). Une section nouvelle se place après le contenu comparé par le golden master.
- Chargement sans squelette : la zone de contenu reste en `aria-busy="true"` jusqu'à l'arrivée des données.
- Bourse :
  - les sélecteurs de valeur et de période se soumettent immédiatement (exception legacy), avec 1M et la première valeur présélectionnées ;
  - le formulaire « + Ajouter une valeur » (`button-secondary`) s'ouvre dans la page, dans une `card` ; « Annuler » le referme et rend le focus au bouton ;
  - le graphique s'affiche dans une `card` `role="img"` avec un nom accessible ; le tableau « 📋 Dernières séances » lui sert d'alternative textuelle ;
  - quand une valeur n'a pas de données, le message legacy remplace le graphique et le tableau ;
  - le bandeau API est supprimé.
- Chat :
  - bulles de Sophie en `card-muted` et du client en `card`, annoncées par `aria-live="polite"` ;
  - pendant l'attente, « Envoyer » affiche « ... » et se désactive ; Entrée envoie, Maj+Entrée insère un saut de ligne ;
  - les actions rapides (`button-ghost`) n'apparaissent que sur une conversation neuve ;
  - une panne s'affiche comme une bulle de Sophie, sans zone d'erreur distincte ;
  - le bandeau IA est supprimé.
- Produits : aucun champ de saisie ; bouton « Rafraichir » ; sections et colonnes legacy ; titres « Traitements nocturnes » et « Historique des traitements nocturnes », sans « (EJB @Schedule) » ; bandeau JPA supprimé.

## Cross-Story Dependencies

- Prérequis : l'epic 2 (jeton, format d'erreur `{"message"}`, renvoi par le proxy et le conteneur web), et l'epic 4 pour la story 7.5 (traitements nocturnes et `BATCH_JOB_LOGS` du cœur bancaire, auquel 7.5 ajoute la lecture du journal).
- Chaîne bourse : 7.1 → 7.2 → 7.3 → 7.4. La story 7.2 règle AR-102 pour tous les fournisseurs.
- Chaîne conseiller : 7.5 → 7.6, puis 7.5 et 7.2 → 7.7 → 7.8.
- Les stories d'écran (7.4, 7.6, 7.8) doivent faire passer `scripts/golden-master/verifier-scenario.sh` pour leur scénario (SC-009-01, SC-011-01, SC-010-01) ; SC-010-02 est remplacé par les tests de FR-010-09 (7.7 et 7.8).
- Chaque story se termine par `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh` et `golden-master.sh` en code 0.
- La story Powens d'un epic ultérieur réutilisera, dans `open-banking-conseiller`, le socle de fournisseurs posé ici (port, simulateur, coupe-circuit).
