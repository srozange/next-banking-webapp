---
title: "Revue rubrique (good spine) — ARCHITECTURE-SPINE next-banking-webapp"
reviewer: rubric walker (reviewer gate)
reviewed: ARCHITECTURE-SPINE.md (draft, 2026-10-05) + compagnons (golden-master-architecture.md, ecarts-voulus-golden-master.md, versions-verifiees.md)
inputs: prd.md (FEAT-001 à FEAT-030, §7 NFR), docs/quality-gates.md, scripts/gates/*.sh, docs/retro/tools/*
date: 2026-10-05
---

# Revue rubrique — ARCHITECTURE-SPINE next-banking-webapp

## Verdict

**À reprendre avant le passage en final (pas de blocage de fond).** Le socle est solide : paradigme clair, 21 AD avec Rule et Preuve, Stack entièrement adossée à `versions-verifiees.md`, table Capability → Architecture complète (FEAT-001 à FEAT-030, plus contrôles qualité et parité), contrôles de `docs/quality-gates.md` fidèlement repris, et une preuve AD-11 réellement exécutée avec succès pendant cette revue (dérivation à blanc : `errors: []`, `rules.kept` 67, `rules.lost` [RG-010-007]). Mais plusieurs vrais points de divergence entre unités ne sont pas fixés : contrats des événements entre services (renvoyés aux stories dans Deferred), transaction de l'audit (en contradiction avec FR-026-04), résolution de l'adresse IP derrière le conteneur web, sérialisation Kafka. S'y ajoutent une règle Horloge trouée (`LocalTime.now()` absent), alors que c'est elle qui garantit le déterminisme de SC-003-04, et une enveloppe opérationnelle incomplète (sauvegarde, persistance, santé, journaux : ni décidés, ni différés, ni en question ouverte).

| Sévérité | Nombre |
|---|---|
| critical | 0 |
| high | 6 |
| medium | 14 |
| low | 12 |

## Grille « good spine »

| Critère | Résultat | Constats |
|---|---|---|
| Fixe les vrais points de divergence, n'en manque aucun | Partiel | H-2, H-3, H-4, M-5, M-6, M-7, M-8, M-13 |
| La Rule de chaque AD est applicable et empêche la divergence annoncée | Partiel | H-1 (AD-12/AD-18 contraires à FR-026-04), H-5 (AD-19), M-3 (AD-13) |
| Rien sous Deferred ne permet à deux unités de diverger | Non | H-2 (« catalogue complet des topics décidé par les stories ») |
| Technologie nommée vérifiée | Oui, avec des trous | M-4 (Node, image web), M-5 (coupe-circuit, cache), L-7, L-8 |
| Couvre les capacités du PRD (table Capability → Architecture) | Oui sur la forme (30/30) ; incomplet sur le fond | H-2, H-3 (FEAT-001, FEAT-015, FEAT-026), M-6 (FEAT-027), L-2 |
| Dimensions de l'altitude : décidées, différées ou ouvertes | Partiel | H-6, M-12, M-13 |
| Preuves exécutables et probantes | Partiel | M-1, M-2, M-11, M-14, H-5, L-4, L-11 |

Vérifications faites : lecture intégrale de la spine et des trois compagnons ; §7 du PRD et FR de FEAT-001, FEAT-016, FEAT-023 à FEAT-027 ; `docs/quality-gates.md` ; contenu de `scripts/gates/build.sh`, `canary-arch.sh`, `check-gates-exist.sh`, `cve.sh` et `mutation.sh` (les libellés cités par les preuves de AD-1 et AD-6 à AD-9 existent bien dans `check-gates-exist.sh`) ; options CLI de `derive-golden-master.py` et `export-golden-master-tests.py` (les options `derive --gm-dir --mapping --output-dir` et `--check` existent) ; exécution de la dérivation à blanc de AD-11.

---

## Findings

### High

#### H-1 — L'audit dans la transaction métier contredit FR-026-04
- **Emplacement :** AD-12, puce « Outbox Kafka (AD-18) » (« les entrées d'audit sont écrites dans la même transaction que le virement ») ; AD-18, puces « Outbox transactionnelle » et « Audit ».
- **Constat :** FR-026-04 exige que chaque entrée d'audit soit enregistrée « dans une transaction indépendante de celle de l'opération qui la déclenche : l'entrée est conservée même si l'opération est annulée ». FR-026-03 exige aussi une entrée VIREMENT « quelle que soit l'issue ». Avec la règle actuelle, un virement dont la validation échoue (FR-023-02, rollback) perd son entrée d'audit, et toute opération annulée perd la sienne. La Rule impose donc un comportement contraire au PRD, et chaque équipe le « corrigera » à sa façon.
- **Correction :** dans AD-18, distinguer deux cas. (a) Effet métier validé : ligne outbox d'audit dans la transaction métier (inchangé). (b) Opération refusée ou annulée : l'entrée d'audit est écrite dans une transaction séparée, ouverte par le port `Transaction` avec une méthode explicite (par exemple `Transaction.executerIndependamment(...)`, qui ouvre une nouvelle transaction), *avant* de propager l'échec. Ajouter dans AD-12 : « en cas d'échec de la validation (FR-023-02), l'entrée VIREMENT est écrite dans une transaction indépendante ». Preuve : test d'intégration « remise impossible → aucun virement, une entrée d'audit VIREMENT présente dans l'outbox ».

#### H-2 — Contrats des événements entre services renvoyés aux stories (Deferred), projections non définies
- **Emplacement :** Deferred, puce « Schéma détaillé des tables et catalogue complet des topics : décidés par les stories » ; AD-4 (« projection locale […] [ASSUMPTION] ») ; AD-18 (enveloppe seulement) ; table Capability → Architecture (FEAT-008, FEAT-015, FEAT-026).
- **Constat :** le producteur et le consommateur d'un topic appartiennent à des services, et donc à des stories, différents. Renvoyer leur contrat aux stories permet exactement la divergence que AD-18 dit empêcher. Trois cas concrets :
  1. **Audit** : 8 producteurs, 1 consommateur. Les champs de `donnees` (utilisateur, action, détail, adresse IP, horodatage) et les codes d'action (A-026-1, explicitement « propriétaire : architecte ; reprise : à l'architecture ») ne sont pas fixés.
  2. **Projections nécessaires au contrôle d'accès (NFR-SEC-03, BUG-093)** : pour `/espace-conseiller`, cœur bancaire et crédits doivent vérifier qu'un client est affecté au conseiller connecté, donnée possédée par identité et clients (`CUSTOMERS`). Le service documents doit vérifier qu'un compte appartient au client (FR-008-04), donnée possédée par le cœur bancaire. Ni les événements sources, ni les projections, ni le service qui les tient ne sont nommés.
  3. **Décision de virement** consommée par notifications : le contenu de `donnees` n'est pas fixé (voir M-7).
- **Correction :** retirer « catalogue complet des topics » de Deferred, ne différer que le schéma des tables *internes*. Ajouter un compagnon `evenements.md` (ou une table dans AD-18) qui liste chaque topic inter-services, avec son producteur, ses consommateurs, sa clé et les champs de `donnees`. Au minimum : `{service}.audit.v1` (champs et liste fermée des codes de FR-026-01 à 03), `identite-clients.client-modifie.v1` (affectation du conseiller, statut actif), `coeur-bancaire.compte-ouvert.v1` (ou `compte-modifie`, avec le titulaire), `coeur-bancaire.virement-decide.v1`, `credits.pret-decide.v1`, `coeur-bancaire.solde-faible.v1`. Dans AD-4, nommer les projections : affectation conseiller → cœur bancaire et crédits ; titulaire des comptes → documents. Règle : un nouveau topic inter-services, ou un nouveau champ, s'ajoute à ce catalogue avant la story qui le produit.

#### H-3 — Résolution de l'adresse IP derrière le conteneur web non décidée (A-001-2, FR-001-13, FR-026-05)
- **Emplacement :** AD-16, AD-17 (conteneur web et proxy Vite) ; table Capability → Architecture (FEAT-001, FEAT-026) ; Open questions (absente).
- **Constat :** toutes les requêtes passent par le conteneur `web` ou par le proxy Vite. Sans règle, chaque service enregistre l'adresse du relais, ou fait confiance à un `X-Forwarded-For` falsifiable, au choix de chaque équipe. Le PRD confie explicitement cette décision à l'architecte (A-001-2 : « propriétaire : architecte ; reprise : à l'architecture (mandataires de confiance) »). FR-026-05 exige l'IP exacte pour onze codes d'action, émis par au moins trois services (identité et clients, cœur bancaire, crédits).
- **Correction :** ajouter à AD-17 une règle explicite. (1) Le conteneur `web` et le proxy Vite remplacent `X-Forwarded-For` par l'adresse du client (`proxy_set_header X-Forwarded-For $remote_addr`, et non `$proxy_add_x_forwarded_for`). (2) Chaque service ne retient l'en-tête que si l'adresse distante appartient au réseau compose déclaré dans `nb.reseau.mandataires-de-confiance` (CIDR), sinon il retient l'adresse distante. (3) La résolution est faite une seule fois, dans `adapter.in.http`, et passée au cas d'usage comme paramètre. Preuve : test d'intégration par service, un `X-Forwarded-For` venant d'une adresse non fiable est ignoré, un autre venant d'un mandataire est retenu.

#### H-4 — Mécanique Kafka entre unités non fixée : sérialisation et groupes de consommateurs
- **Emplacement :** AD-18 ; Stack (Spring Kafka 4.1.1).
- **Constat :** l'enveloppe JSON est fixée, mais pas la façon de la (dé)sérialiser. Par défaut, le `JsonSerializer` (ou `JacksonJsonSerializer` sous Jackson 3 et Boot 4) ajoute un en-tête `__TypeId__` qui porte le nom de la classe du producteur. Un consommateur d'un autre service, qui n'a pas cette classe, échoue alors à la désérialisation. C'est la cassure classique entre deux équipes. De même, rien ne fixe le `group.id` : deux consommateurs de services différents qui prendraient le même groupe se partageraient les messages au lieu de les recevoir chacun.
- **Correction :** ajouter à AD-18 : sérialisation `StringSerializer` et `StringDeserializer` ; l'adaptateur mappe l'enveloppe vers ses propres types (ou utilise le sérialiseur JSON avec en-têtes de type désactivés et type cible fixé côté consommateur) ; `group.id` = identifiant du service consommateur (`spring.kafka.consumer.group-id=<identifiant>`), jamais partagé entre services. Preuve : le test d'intégration Testcontainers du consommateur publie un message *brut* (chaîne JSON de l'enveloppe, sans en-tête de type) et constate l'effet.

#### H-5 — AD-19 (Horloge) : la règle et sa preuve laissent passer `LocalTime.now()`, `Clock` et `now()` SQL
- **Emplacement :** AD-19, Rule et Preuve ; golden-master-architecture.md §7.2.1 (même expression).
- **Constat :** la liste interdite omet `LocalTime.now()`, l'appel le plus probable pour UNUSUAL_HOURS, le signal même qui fait diverger SC-003-04. Elle omet aussi `Year.now()`, `YearMonth.now()`, `Clock.systemUTC()` et `Clock.systemDefaultZone()`, et l'injection d'une `java.time.Clock` n'importe où, alors que l'exception JWT en installe une dans le contexte Spring. Elle omet enfin les `DEFAULT now()` et `CURRENT_TIMESTAMP` des migrations Flyway. À l'inverse, l'exception JWT écrite naturellement (`Instant.now(clock)`) fait échouer le grep. Le contenu de `HorlogeArchitectureTest` n'est pas spécifié : chaque service écrira le sien.
- **Correction :** Rule : interdire toute méthode statique `now(..)` des types `java.time.*`, et toute dépendance à `java.time.Clock`, hors `adapter.out.horloge` et hors un paquet nommé pour le jeton (par exemple `adapter.out.jeton`) ; interdire `now()`, `CURRENT_TIMESTAMP` et `CURRENT_DATE` dans les migrations. Spécifier `HorlogeArchitectureTest` : `noClasses().that().resideOutsideOfPackages("..adapter.out.horloge..", "..adapter.out.jeton..").should().callMethodWhere(target(name("now")).and(target(owner(resideInAPackage("java.time"))))).orShould().dependOnClassesThat().areAssignableTo(Clock.class)`. Preuve grep corrigée : `! grep -rnE '\b(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime|OffsetTime|Year|YearMonth)\.now\(|Clock\.system|System\.currentTimeMillis\(' services/*/src/main --include=*.java | grep -vE '/adapter/out/(horloge|jeton)/'`, plus `! grep -rniE '\b(now\(\)|current_timestamp|current_date)\b' services/*/src/main/resources/db/migration`. Reporter la même expression dans golden-master-architecture.md §7.2.1.

#### H-6 — Enveloppe opérationnelle : sauvegarde, persistance, santé et journaux ni décidés, ni différés, ni ouverts
- **Emplacement :** AD-6 ; Deferred ; Open questions ; Structural Seed (déploiement local).
- **Constat :** Deferred traite les environnements autres que le local et la haute disponibilité. Rien, en revanche, sur :
  - la **sauvegarde et la restauration** des 8 bases (le mot n'apparaît pas) ;
  - la **persistance des données** : volumes nommés ou non. FR-002-09 et FR-023-06 exigent des notifications conservées après redémarrage, et `docker compose down` supprime les volumes anonymes ;
  - les **sondes de santé** et l'ordre de démarrage (`healthcheck` et `depends_on: condition: service_healthy` ; exposition de l'actuator `health`). Sans elles, `make rejeu-up` suivi de `make reset-data`, et `trace-smoke.sh`, deviennent instables ;
  - l'**observabilité en exploitation hors traces** : format des journaux, présence de l'identifiant de trace dans chaque ligne, métriques. Jaeger v2 garde les traces en mémoire, perdues au redémarrage, ce qui n'est dit nulle part.
  Pour un démonstrateur local, la plupart de ces points peuvent être différés, mais ils doivent l'être explicitement.
- **Correction :** ajouter à AD-6 : un volume nommé `pgdata-<identifiant>` par base ; un `healthcheck` sur chaque conteneur (`pg_isready`, `kafka-broker-api-versions.sh`, `/actuator/health` exposé seul) ; `depends_on` avec `service_healthy` ; `make rejeu-up` attend que tout soit sain. Ajouter aux Conventions une ligne Journaux : sortie console, `traceId` et `spanId` dans chaque ligne (corrélation Micrometer, par défaut en Boot 4, à vérifier). Ajouter à Deferred : « sauvegarde et restauration : aucune (démonstrateur local, données reconstruites par `make reset-data`) » ; « métriques et alertes : hors démonstrateur » ; « rétention des traces : en mémoire dans Jaeger, perdue au redémarrage ».

### Medium

#### M-1 — Preuve de AD-3 sans portée : la recherche dans `src/main/resources` ne voit pas les adresses de base passées par `compose.yaml`
- **Emplacement :** AD-3, Preuve (boucle `grep -rhoE 'postgres-[a-z-]+'`).
- **Constat :** les adresses de base viendront très probablement de variables d'environnement (`SPRING_DATASOURCE_URL`) déclarées dans `compose.yaml`. La boucle passe alors à vide. Un service branché par compose sur la base d'un autre ne serait pas détecté.
- **Correction :** vérifier la configuration résolue de compose : `docker compose -f compose.yaml config --format json | jq -r '.services | to_entries[] | select(.key|test("^postgres-")|not) | select(.value.environment.SPRING_DATASOURCE_URL) | "\(.key) \(.value.environment.SPRING_DATASOURCE_URL)"' | awk '$2 !~ "//postgres-"$1"[:/]" {print "KO", $0}'` ne doit rien afficher. Conserver la boucle sur `src/main/resources` en complément.

#### M-2 — Preuve de AD-4 faible : un appel HTTP entre services passerait sous le grep
- **Emplacement :** AD-4, Preuve.
- **Constat :** le grep ne cherche que des URL littérales `http://<service>`. Une base d'URL injectée (`${NB_CREDITS_URL}`), un `RestClient` ou un `WebClient` y échappent. `trace-smoke.sh` prouve la présence de spans Kafka, pas l'absence d'appels HTTP.
- **Correction :** règle ArchUnit dans `*ArchitectureTest` : aucune classe hors de `..adapter.out.fournisseur..` ne dépend de `RestClient`, `WebClient`, `RestTemplate` ou `java.net.http.HttpClient`. En complément : aucune variable d'environnement de `compose.yaml` ne contient `http://<identifiant d'un autre service>`.

#### M-3 — AD-13 : la Rule nomme une commande dont le résultat est en question ouverte
- **Emplacement :** AD-13 ; Open questions (« Bloc `@theme` de l'export `css-tailwind` »).
- **Constat :** `export --format css-tailwind` produit un bloc `@theme` qui n'a de sens qu'avec Tailwind v4, absent de la Stack. En l'état, `tokens.css` n'est pas utilisable comme variables CSS, et la Rule « les écrans n'utilisent que ces variables CSS » n'est pas applicable.
- **Correction :** trancher avant la story 1.1. Option sans dépendance nouvelle : `npm run tokens` = export `dtcg`, puis un script versionné qui écrit des propriétés `:root { --… }`. Sinon, vérifier Tailwind v4, l'ajouter à `versions-verifiees.md` et à la Stack. Réécrire la Rule en conséquence.

#### M-4 — Stack incomplète pour le front et les images
- **Emplacement :** Stack ; Open questions (« Image du conteneur web et version de Node ») ; `versions-verifiees.md` (ligne Temurin : « tag non vérifié individuellement »).
- **Constat :** Node (requis par Vite, react-router, Vitest, Stryker, Playwright et `npx`), l'image du conteneur web et le tag `eclipse-temurin:21` ne sont pas vérifiés. `eclipse-temurin:21` est un tag flottant, une image JDK complète, alors que l'exécution n'a besoin que d'un JRE. Ces points bloquent la story 1.1. Ils sont signalés en question ouverte, mais la Stack se présente comme complète.
- **Correction :** vérifier et ajouter à `versions-verifiees.md` puis à la Stack : Node 22 LTS (au moins 22.22.0, exigé par react-router 8.4.0) ou 24 LTS ; image du conteneur web (par exemple `nginx:<version>-alpine`, avec la configuration de renvoi de AD-17) ; tags `eclipse-temurin:21.0.12_…-jdk` pour la construction et `…-jre` pour l'exécution, épinglés.

#### M-5 — Coupe-circuit et cache des fournisseurs : technologie ni choisie ni vérifiée
- **Emplacement :** AD-20 (« passe par un coupe-circuit ; cotations mises en cache ») ; Stack.
- **Constat :** deux services (bourse ; open banking et conseiller) implémenteront NFR-INT-02 chacun de son côté. Spring Framework 7 n'offre pas de coupe-circuit. Sans choix commun, deux bibliothèques ou deux implémentations maison, avec des réglages et des comportements d'erreur différents.
- **Correction :** nommer la bibliothèque (par exemple Resilience4j, à vérifier pour Boot 4.1.1), la vérifier dans `versions-verifiees.md`, et fixer dans AD-20 les réglages communs : délai d'attente, seuil d'ouverture, préfixe `nb.fournisseurs.<nom>.*`. Fixer aussi le cache des cotations (par exemple Caffeine, versions vérifiées).

#### M-6 — FR-027-11 (MULTIPLE_FAILED_AUTH) traverse deux services sans règle
- **Emplacement :** AD-12 ; AD-14 (`FRAUD_ALERTS` au cœur bancaire) ; table Capability → Architecture (FEAT-027 : « coeur-bancaire » seul).
- **Constat :** les échecs de connexion sont constatés par identité et clients, mais les alertes OPEN vivent dans `FRAUD_ALERTS`, au cœur bancaire. Deux équipes, aucun contrat : compteur tenu où ? événement à chaque échec ou seulement au 5e ?
- **Correction :** ajouter à AD-12 ou au catalogue de H-2 : identité et clients tient le compteur d'échecs consécutifs et publie `identite-clients.echecs-connexion-repetes.v1` au 5e échec ; le cœur bancaire consomme cet événement et crée l'alerte. Mettre à jour la ligne FEAT-027 de la table (« coeur-bancaire ; identite-clients »).

#### M-7 — Décision du type de notification : seuils et texte en deux endroits
- **Emplacement :** AD-12 (seuils `nb.fraude.*` au cœur bancaire ; « consommé par notifications (CONFIRMATION, ALERTE ou FRAUDE) ») ; FR-023-03, FR-023-05, FR-025-01.
- **Constat :** le choix entre CONFIRMATION et ALERTE dépend du seuil d'alerte (5 000 €), configuré au cœur bancaire. Si le service notifications choisit le type, il lui faut une seconde copie du seuil, qui pourra diverger. Rien ne dit non plus qui compose le texte, ni où se configure le plafond de 20 notifications.
- **Correction :** Rule. Le cœur bancaire décide du type et le met dans `donnees` de `virement-decide.v1`, avec les paramètres du texte (montant, compte destinataire, motif, heure butoir). Le service notifications compose le texte à partir de textes-nouveaux.md et du legacy, et applique seul le plafond (`nb.notifications.plafond=20`). Même schéma pour `credits.pret-decide.v1`.

#### M-8 — Moment du débit selon le parcours non explicité
- **Emplacement :** AD-12 (machine à états, outbox) ; FEAT-023.
- **Constat :** les FR imposent un débit synchrone à la soumission pour un virement exécuté (FR-023-02 : « rien n'est débité » si la remise échoue) et un débit asynchrone pour un virement LIBÉRÉ (B7). Le rôle du consommateur de `virement-a-traiter.v1` (débit, ou simple post-traitement) n'est pas écrit. Les stories « virement » et « traitement asynchrone » peuvent donc débiter deux fois, ou pas du tout.
- **Correction :** ajouter à AD-12 un tableau par parcours. Immédiat exécuté : débit et crédit dans la transaction de soumission ; le consommateur ne débite pas. LIBÉRÉ : le consommateur débite par une transition conditionnelle LIBÉRÉ → EXÉCUTÉ, ou rejette pour solde insuffisant. Échéance programmée : comme l'immédiat.

#### M-9 — FR de session (fixation, expiration, page expirée) non transposées au JWT
- **Emplacement :** AD-16 ; FR-001-09, FR-030-08, NFR-SEC-02, FR-030-05, NFR-SEC-07.
- **Constat :** les critères d'acceptation de FR-001-09 parlent d'« identifiant de session avant et après la connexion ». Avec un JWT sans état, ils ne sont pas testables tels quels. Chaque story inventera son équivalent. FR-030-05 (page expirée) n'est pas reliée à l'expiration du jeton. La règle d'inactivité reste une [ASSUMPTION].
- **Correction :** ajouter à AD-16 une table de correspondance. Fixation de session : sans objet, aucun identifiant n'est émis avant l'authentification ; preuve, aucune réponse publique ne pose de cookie ni de jeton. Nouvelle session : chaque connexion émet un nouveau jeton (`jti` unique). Page expirée : jeton expiré → 401 → message de FR-030-05. Inscrire l'inactivité en question ouverte, à faire confirmer par le métier.

#### M-10 — Aucune convention de traçabilité des tests vers les FR et les BUG (NFR-PAR-02, NFR-PAR-03, SM-2, SM-3)
- **Emplacement :** `binds` du front-matter (NFR-PAR-02 et NFR-PAR-03 absents) ; Consistency Conventions.
- **Constat :** SM-3 (« 50 sur 50 bugs ont un test vert ») et SM-2 doivent être mesurés sur 8 services et un front. Sans convention commune (tag, nom de test), la mesure est impossible ou manuelle, et chaque équipe nomme ses tests à sa façon.
- **Correction :** ajouter aux Conventions : chaque test d'acceptation porte `@Tag("FR-xxx-yy")` et, s'il prouve un bug corrigé, `@Tag("BUG-nnn")` (JUnit 5) ; côté front, le titre Vitest contient `[FR-xxx-yy]` et `[BUG-nnn]`. Ajouter NFR-PAR-02 et NFR-PAR-03 aux `binds`, et prévoir un script de comptage dans `scripts/` (hors `gates/`).

#### M-11 — Preuve de AD-20 non exécutable telle qu'écrite
- **Emplacement :** AD-20, Preuve.
- **Constat :** « `build.sh` passe sans réseau (`docker compose` arrêté côté fournisseurs) » ne peut pas tourner tel quel. `build.sh` fait `npm ci` et résout les dépendances Maven, ce qui demande le réseau. Les fournisseurs ne sont pas des conteneurs. Testcontainers exige Docker. Le grep `mode *[:=] *reel` ne voit pas une variable d'environnement `NB_FOURNISSEURS_*_MODE=reel` dans `compose.yaml`.
- **Correction :** preuve en trois parties. (1) Un test de configuration par service démarre le contexte sans aucune variable `nb.fournisseurs.*` et constate que le bean actif est l'adaptateur simulé. (2) `! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' services/*/src/main/resources compose*.yaml`. (3) Sur un cache déjà rempli, `mvn -o verify` et `npm ci --offline` passent sans clé définie.

#### M-12 — Secrets : inventaire, provisionnement et preuve absents
- **Emplacement :** AD-6 (« Les secrets viennent de variables d'environnement ») ; AD-16 ([ASSUMPTION] paire RSA) ; Conventions (Configuration).
- **Constat :** la règle « jamais dans un fichier versionné » est posée, mais rien ne dit d'où viennent les secrets en local, ni comment le prouver. Ils sont nombreux : 8 mots de passe de base, paire RSA, clé NVD, `RETRO_*_PASSWORD`, clés des fournisseurs. Faute de mécanisme commun, chaque story inventera le sien, avec le risque d'un `application-local.properties` committé.
- **Correction :** Rule : fichier `.env` non versionné, généré par `make secrets` (mots de passe aléatoires, paire RSA dans `.secrets/`, tous deux dans `.gitignore`) ; `.env.example` versionné, sans valeur. Preuve : `trivy fs --scanners secret --exit-code 1 .`, déjà dans la Stack ; `git ls-files .env .secrets` vide.

#### M-13 — Infrastructure Kafka : création des topics et réglages mono-broker non décidés
- **Emplacement :** AD-6, AD-18 ; Deferred (« partitionnement Kafka »).
- **Constat :** rien ne dit si les topics sont créés automatiquement ou déclarés, par qui, ni avec combien de partitions. Sur un broker unique, les facteurs de réplication des topics internes (`offsets`, `transaction state`) doivent valoir 1, sinon des fonctions échouent en silence. Chaque service pourrait déclarer ses `NewTopic` différemment.
- **Correction :** Rule : `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` ; chaque producteur déclare ses topics par un bean `NewTopic` dans `infrastructure` (3 partitions, réplication 1 en local) ; `KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1` et `KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1` dans `compose.yaml`.

#### M-14 — AD-16 : preuve non exécutable et statut du refus d'appartenance non fixé
- **Emplacement :** AD-16, Preuve (« tests d'intégration de chaque service ») ; AD-17 (401 et 403).
- **Constat :** aucune commande ni convention de nommage ne permet de vérifier que chaque service a ses tests de sécurité. Le statut d'une ressource d'un autre client (403 ou 404) n'est pas fixé, alors que le front et le golden master en dépendent (FR-008-04).
- **Correction :** fixer « ressource d'un autre client → 403 avec `message` » (ou 404, mais une seule règle). Preuve : une classe `SecuriteIT` par service, vérifiée par `for s in services/*/; do find "$s/src/test" -name '*SecuriteIT.java' | grep -q . || echo "KO $s"; done` (aucun `KO`) et exécutée par `mvn verify`.

### Low

- **L-1 — Question ouverte périmée.** Open questions, « Chemin de l'adaptateur Horloge dans `golden-master-architecture.md` §7.2 » : le compagnon exclut déjà `/adapter/out/horloge/`. *Correction :* retirer la question, ou la remplacer par l'alignement demandé en H-5.
- **L-2 — `binds` incomplet.** NFR-PERF-01, NFR-PAR-02 et NFR-PAR-03 ne sont liés à aucune AD (NFR-PERF-01 n'apparaît que dans Deferred, « au-delà de NFR-PERF-01 »). *Correction :* ajouter NFR-PERF-01 à la ligne « Contrôles qualité » de la table ou à AD-20 (le cache des cotations sert la cible de 3 000 ms), et voir M-10.
- **L-3 — AD-9 annonce les « images » dans Binds,** alors que Trivy fs n'analyse pas d'image. *Correction :* retirer « images » de Binds, ou ajouter `trivy image` sur les images construites (en différé, si ce n'est pas exigé par quality-gates).
- **L-4 — AD-10, `<base>` non défini** dans `git diff --name-only <base>...HEAD`. *Correction :* `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- …`.
- **L-5 — AD-13, grep CSS incomplet.** Il manque les sélecteurs imbriqués, les CSS modules, `style={{…}}` en TSX et les sélecteurs indentés autrement. *Correction :* règle stylelint (`selector-max-type: 0` hors `src/adapters/ui/**`), ou admettre la limite dans la Preuve.
- **L-6 — Relais de l'outbox et traitements planifiés.** AD-21 désactive « tous » les traitements planifiés en profil `rejeu`, alors que AD-18 introduit un relais planifié de l'outbox. Rien ne dit s'il est désactivé, ni s'il écrit dans le journal d'exécution affiché par FR-016-17 (50 entrées, déjà saturé par les soldes faibles). *Correction :* préciser que le relais est technique : il reste actif en `rejeu` et n'écrit pas dans le journal d'exécution.
- **L-7 — Dépendances Boot 4 à expliciter.** Flyway sous Boot 4 demande `spring-boot-starter-flyway` et `flyway-database-postgresql`. JaCoCo : les `<include>` d'une règle `check` exigent `<element>PACKAGE</element>` (ou `CLASS`), sinon ils sont ignorés en `BUNDLE`. *Correction :* l'écrire dans AD-3 et AD-7.
- **L-8 — Technologies d'un seul service non nommées :** génération des PDF (FEAT-008, FEAT-017, FEAT-020) et fonction de hachage des mots de passe (NFR-SEC-01 ; les `reference-data.sql` contiennent les hachés). *Correction :* fixer le hachage (par exemple `DelegatingPasswordEncoder` bcrypt, préfixe `{bcrypt}`), car les données de référence en dépendent ; la bibliothèque PDF peut être différée explicitement à la story, avec obligation de passer par `versions-verifiees.md`.
- **L-9 — Trivy 0.75.0 en Stack, 0.74.0 installé.** *Correction :* préciser qui épingle la version utilisée par `cve.sh` (image `aquasec/trivy:0.75.0`, ou installation contrôlée).
- **L-10 — `replay-mapping.json` non suivi par git** (`??` au moment de la revue), alors que les preuves de AD-11 en dépendent. *Correction :* le committer avec la spine.
- **L-11 — AD-21, preuve vague** (« `config` montre le profil `rejeu` »). *Correction :* `docker compose -f compose.yaml -f compose.rejeu.yaml config --format json | jq '[.services[] | select(.environment.SPRING_PROFILES_ACTIVE // "" | test("rejeu"))] | length'` vaut 8.
- **L-12 — AD-12, preuve de la trace du refus sans mode opératoire.** Il faut provoquer l'expiration de l'accès à l'historique. *Correction :* décrire le déclencheur (par exemple `nb.fraude.acces-historique.delai=1ms` en test d'intégration, ou un verrou tenu sur la table), puis `curl …/api/traces/<reference>`.

---

## Couverture PRD (table Capability → Architecture)

Les 30 FEAT sont présentes, chacune avec un emplacement et au moins une AD. Les manques de fond sont rattachés aux findings :

| FEAT | Manque | Finding |
|---|---|---|
| FEAT-001 Connexion | IP de l'audit (FR-001-13), session et JWT (FR-001-09) | H-3, M-9 |
| FEAT-002 Tableau de bord | plafond et composition des notifications | M-7 |
| FEAT-003, FEAT-023 Virement et traitement asynchrone | moment du débit ; audit en cas d'échec | M-8, H-1 |
| FEAT-008, FEAT-015, FEAT-016 | projections nécessaires au contrôle d'appartenance et d'affectation | H-2 |
| FEAT-009, FEAT-010, FEAT-012, FEAT-028, FEAT-029 | coupe-circuit et cache | M-5 |
| FEAT-026 Audit trail | contrat d'événement, codes (A-026-1), transaction indépendante, IP | H-1, H-2, H-3 |
| FEAT-027 Fraude temps réel | MULTIPLE_FAILED_AUTH entre identité et cœur bancaire | M-6 |

## Dimensions de l'altitude (initiative, système entier)

| Dimension | État | Commentaire |
|---|---|---|
| Paradigme et découpage | Décidé | AD-1, AD-2, AD-14 |
| Données et propriété | Décidé, contrats incomplets | AD-3, AD-14 ; H-2 |
| Intégration entre services | Partiel | AD-4, AD-18 ; H-2, H-4, M-13 |
| API HTTP et erreurs | Décidé | AD-17 ; M-14 |
| Sécurité et identité | Partiel | AD-16 ; H-3, M-9, M-12 |
| Temps et déterminisme | Décidé, règle trouée | AD-19 ; H-5 |
| Traitements planifiés | Décidé | AD-21 ; L-6 |
| Fournisseurs externes | Décidé, technologie manquante | AD-20 ; M-5 |
| Front et thème | Décidé, une question bloquante | AD-2, AD-13 ; M-3, M-4 |
| Qualité et contrôles | Décidé | AD-1, AD-2, AD-5, AD-7 à AD-10 |
| Déploiement et environnements | Local décidé (AD-6), autres différés | ok |
| Infrastructure (compose, Kafka) | Partiel | H-6, M-13 |
| Exploitation (santé, démarrage) | **Absent** | H-6 |
| Sauvegarde et persistance | **Absent** | H-6 |
| Secrets | Principe seulement | M-12 |
| Observabilité en exploitation | Traces seulement | H-6 |
