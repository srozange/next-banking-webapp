---
title: 'Ouvrir le service bourse et gérer la watchlist commune'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: '7335a796e40ee7169f38175acca44cb1ab423130'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-7-context.md'
warnings: ['oversized']
deferred:
  - summary: >-
      Aucune commande outillée ne charge services/bourse/reference-data/reference-data.sql dans la pile compose : la watchlist reste vide après `docker compose up`.
    evidence: |-
      Même constat que pour identite-clients (report de la story 2.1) : le chargement est prévu par `make reset-data` (spine AD-11, golden-master-architecture §5.2), livré par la story « Socle du rejeu final ». Le chargement manuel par psql est décrit dans les Design Notes et a été vérifié sur une pile isolée.
    location: >-
      services/bourse/reference-data/reference-data.sql ; Makefile
    severity: medium
---

<intent-contract>

## Intent

**Problem:** Le service `bourse` n'existe pas : aucun utilisateur authentifié ne peut consulter, compléter ni réduire la watchlist commune (FEAT-009, FEAT-029), prérequis des stories 7.2 à 7.4.

**Approach:** Créer le module hexagonal `services/bourse` (Spring Boot, PostgreSQL `postgres-bourse`, Flyway, serveur de ressources JWT, tracing), sa table `watchlist_items`, ses 9 valeurs de référence hors migrations, et l'API `GET`/`POST /api/bourse/watchlist`, `DELETE /api/bourse/watchlist/{id}` ; le brancher dans le reactor Maven, `compose.yaml`, un `compose.rejeu.yaml` initial, `make secrets` et les Dockerfiles.

## Boundaries & Constraints

**Always:**
- Hexagone : `domain` et `application` en Java pur ; `adapter.in.http`, `adapter.out.persistance`, `infrastructure` ; tests `HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest` copiés d'audit (paquet `fr.nextbanking.bourse`).
- Tout `/api/**` exige un jeton valide (clé publique `nb.jeton.cle-publique`, refus de démarrer sans) ; aucune restriction de rôle (CLIENT, CONSEILLER, ADMIN identiques, FR-009-01). `/actuator/health` public.
- Erreurs au seul format `{"message": "..."}`, sans pile ni nom de classe (NFR-SEC-04) : 401 « Authentification requise », 404 « Ressource introuvable », 400 « Requête invalide » par défaut, textes repris de `Textes` d'audit.
- Normalisation à l'ajout : symbole rogné et en majuscules (`Locale.ROOT`), nom et marché rognés, devise rognée et en majuscules.
- Ordre des contrôles à l'ajout : symbole vide/blanc/absent → « Le symbole est obligatoire (ex: AMZN, SG.PA) » ; puis nom vide/blanc/absent → « Le nom de la valeur est obligatoire » ; puis symbole > 20, nom > 100, marché ∉ {Euronext Paris, NASDAQ, NYSE, LSE, Autre} (> 50), devise ∉ {EUR, USD, GBP} (> 10) → « Requête invalide » ; puis doublon (insensible à la casse) → « Le symbole « {SYMBOLE} » existe déjà dans votre watchlist ». Tous en 400, rien d'enregistré.
- Liste triée par marché croissant puis symbole croissant ; champs JSON `id`, `symbole`, `nom`, `marche`, `devise`.
- Chaque FR de la story a au moins un test tagué `@Tag("<FR>")` : FR-009-01, FR-009-10, FR-009-11, FR-009-12, FR-009-13, FR-029-09, FR-029-10, FR-029-11, FR-029-13, FR-029-14.
- Couverture JaCoCo 100 % sur domain/application, 80 % sur adapter/infrastructure.

**Never:**
- Pas de fournisseur AlphaVantage, d'historique, de graphique, de port Horloge ni de front (stories 7.2 à 7.4).
- Pas de Kafka dans `bourse` (aucun événement publié ni consommé) ; aucun appel HTTP vers un autre service.
- Pas de filtrage par utilisateur ; pas de confirmation à la suppression.
- Ne pas modifier `scripts/gates/**`, `frontend/.dependency-cruiser.cjs`, `docs/quality-gates.md`, ni `sprint-status.yaml`.
- Données de référence jamais dans une migration Flyway.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Lecture | jeton CLIENT, CONSEILLER ou ADMIN ; 9 valeurs de référence | 200, même liste de 9, AI.PA en tête, MSFT en dernier | — |
| Sans jeton | aucun / invalide / expiré | 401 `{"message":"Authentification requise"}` | pas de pile, pas de cookie |
| Ajout normalisé | `{"symbole":"  sg.pa ","nom":" Société Générale ","marche":"Euronext Paris","devise":"eur"}` | 201, corps de la valeur créée (`SG.PA`, `Société Générale`, `Euronext Paris`, `EUR`) ; rang entre OR.PA et SAN.PA | — |
| Symbole manquant | `""`, `"   "`, absent | 400 message symbole | rien enregistré |
| Nom manquant | `""`, `"  "`, absent | 400 message nom | rien enregistré |
| Doublon | `"bnp.pa"` | 400 « Le symbole « BNP.PA » existe déjà dans votre watchlist » | course entre deux ajouts : contrainte unique → même message |
| Hors limites | symbole 21 car., nom 101 car., marché « Bourse de Tokyo », devise « JPY », marché/devise absents | 400 « Requête invalide » | rien enregistré |
| Corps illisible | JSON invalide | 400 « Requête invalide » | — |
| Suppression | `DELETE …/{id}` d'AI.PA | 204 sans corps ; AI.PA absente de la liste | — |
| Suppression inconnue | id inexistant / non numérique | 404 / 400 au format `{"message"}` | — |

</intent-contract>

## Code Map

- `services/audit/` -- modèle le plus proche (serveur de ressources JWT sans Kafka sortant) : `pom.xml`, `Dockerfile`, `adapter/in/http/{ConfigurationSecurite,AttributsErreur,Textes}.java`, `infrastructure/{ApplicationAudit,ControleMotDePasseBase}.java`, tests `*ArchitectureTest`, `adapter/AuditSecuriteIT.java` (fabrication de jetons RS256 par `NimbusJwtEncoder`), `infrastructure/{Conteneurs,ContexteDemarre,ApplicationAuditTest}.java`, `adapter/in/http/ConfigurationSecuriteTest.java`.
- `services/identite-clients/src/main/java/fr/nextbanking/identiteclients/adapter/in/http/GestionErreurs.java` -- motif `@RestControllerAdvice` (exception métier → message, `HttpMessageNotReadableException`/`TypeMismatchException` → 400, `ErrorResponse` → statut publié).
- `services/identite-clients/src/main/java/fr/nextbanking/identiteclients/domain/ConnexionRefusee.java` -- motif d'exception de domaine à motif énuméré.
- `services/identite-clients/src/test/java/.../referencedata/ReferenceDataIT.java` -- motif de test du `reference-data.sql` (schéma Flyway neuf, chargement, `setval`).
- `services/identite-clients/reference-data/reference-data.sql` -- en-tête et `select setval(pg_get_serial_sequence(...))`.
- `pom.xml` (racine) -- `<modules>` à compléter ; JaCoCo et profils mutation/security hérités.
- `services/*/Dockerfile` -- chacun copie le `pom.xml` de chaque module du reactor (`-pl … -am`) : ajouter `services/bourse/pom.xml` partout.
- `compose.yaml` -- motif `postgres-audit`/`audit` ; commentaire d'en-tête des ports ; `web.depends_on`.
- `Makefile` (`MOTS_DE_PASSE`), `.env.example` -- ajouter `NB_BOURSE_BD_MOT_DE_PASSE`.
- `frontend/vite.config.ts` (bourse: 8087) et `frontend/nginx.conf` (liste fermée avec `bourse`) -- renvoi déjà en place, lecture seule.
- `docs/retro/features/FEAT-009/screens/SCR-009-01_charge.png` -- source des 9 valeurs (noms « L Oreal » sans apostrophe, « Alphabet », marchés Euronext Paris/NASDAQ, devises EUR/USD).
- `scripts/gates/check-gates-exist.sh` -- lecture seule : boucle sur `services/*/pom.xml` et exige `compose : service bourse`.

## Tasks & Acceptance

**Execution:**
- `services/bourse/pom.xml` -- créer d'après audit sans `spring-boot-starter-kafka` ni `testcontainers-kafka` -- module du reactor.
- `pom.xml` -- ajouter `<module>services/bourse</module>`.
- `services/bourse/src/main/resources/application.properties` -- nom `bourse`, port 8080, datasource `postgres-bourse:5432/bourse` (utilisateur `bourse`), tracing OTLP 1.0, health seul exposé, corrélation des journaux -- conventions AD-5/AD-6.
- `services/bourse/src/main/resources/db/migration/V1__watchlist_items.sql` -- table `watchlist_items` (`id bigint identity`, `symbol varchar(20)`, `name varchar(100)`, `market varchar(50)`, `currency varchar(10)`, tous non nuls) + index unique sur `upper(symbol)`.
- `services/bourse/reference-data/reference-data.sql` -- 9 inserts aux id 1 à 9 (AI.PA Air Liquide, BNP.PA BNP Paribas, MC.PA LVMH, OR.PA L Oreal, SAN.PA Sanofi, TTE.PA TotalEnergies en Euronext Paris/EUR ; AAPL Apple, GOOGL Alphabet, MSFT Microsoft en NASDAQ/USD) + `setval`.
- `services/bourse/src/main/java/fr/nextbanking/bourse/domain/` -- `ValeurWatchlist` (record id, symbole, nom, marche, devise), `NouvelleValeur` (création normalisée et validée), `AjoutRefuse` (motifs SYMBOLE_REQUIS, NOM_REQUIS, REQUETE_INVALIDE, DOUBLON avec symbole), `Marche`/`Devise` ou listes fermées.
- `services/bourse/src/main/java/fr/nextbanking/bourse/application/` -- ports in `ConsulterWatchlist`, `AjouterValeur`, `RetirerValeur` ; port out `DepotWatchlist` (lister trié, existeSymbole, ajouter, supprimer → booléen) ; cas d'usage correspondants (`RetirerValeur` lève `ValeurIntrouvable` si rien supprimé ; `AjouterValeur` vérifie le doublon après validation).
- `services/bourse/src/main/java/fr/nextbanking/bourse/adapter/out/persistance/DepotWatchlistJdbc.java` -- `JdbcClient` ; `order by market, symbol` ; violation d'unicité → `AjoutRefuse` DOUBLON.
- `services/bourse/src/main/java/fr/nextbanking/bourse/adapter/in/http/` -- `PointEntreeWatchlist` (GET 200 liste, POST 201 corps de la valeur, DELETE 204), `GestionErreurs`, `ConfigurationSecurite`, `AttributsErreur`, `Textes` (avec les trois textes legacy).
- `services/bourse/src/main/java/fr/nextbanking/bourse/infrastructure/` -- `ApplicationBourse`, `ControleMotDePasseBase`, `AssemblageWatchlist`.
- `services/bourse/src/test/java/fr/nextbanking/bourse/` -- trois `*ArchitectureTest` ; tests unitaires domaine/application (I/O matrix) ; `adapter/BourseSecuriteIT` (401 ×4 jetons, santé publique) ; `adapter/WatchlistIT` (trois rôles, ajout normalisé, refus, suppression, 404) sur base chargée du `reference-data.sql` ; `referencedata/ReferenceDataIT` ; `infrastructure/{Conteneurs,ContexteDemarre,ApplicationBourseTest}` ; tests des adaptateurs HTTP -- tags FR sur chaque comportement.
- `services/bourse/Dockerfile` et `services/{audit,coeur-bancaire,identite-clients,notifications}/Dockerfile` -- Dockerfile bourse sur le modèle audit ; ajouter `COPY services/bourse/pom.xml services/bourse/` aux autres (et la copie des autres POMs dans celui de bourse).
- `compose.yaml` -- `postgres-bourse` (volume `pgdata-bourse`), `bourse` (port `${NB_PORT_BOURSE:-8087}:8080`, `NB_JETON_CLE_PUBLIQUE`, healthcheck), `web.depends_on` bourse, commentaire d'en-tête.
- `compose.rejeu.yaml` -- créer : surcouche qui pose `SPRING_PROFILES_ACTIVE: rejeu` sur `bourse` (fichier complété par la story « Socle du rejeu final »).
- `Makefile`, `.env.example` -- `NB_BOURSE_BD_MOT_DE_PASSE`.

**Acceptance Criteria:**
- Given la story terminée, when `ls services/`, then `bourse` y figure avec `pom.xml`, `Dockerfile`, `reference-data/reference-data.sql` et les paquets `fr/nextbanking/bourse/{domain,application,adapter/in,adapter/out,infrastructure}`.
- Given `compose.yaml` et `compose.rejeu.yaml`, when `docker compose -f compose.yaml -f compose.rejeu.yaml config --format json`, then `bourse` (8087) et `postgres-bourse` (volume `pgdata-bourse`) existent, `bourse` a `SPRING_PROFILES_ACTIVE=rejeu`, et chaque service a un healthcheck.
- Given le service bourse, when `mvn -B -q -f services/bourse/pom.xml test -Dtest='*ArchitectureTest'`, then code 0.
- Given la story terminée, when `mvn -B -q -f services/bourse/pom.xml test -Dgroups='<FR>'` pour chacune des 10 FR, then code 0 et au moins un test exécuté par FR.
- Given la pile démarrée et le `reference-data.sql` chargé dans `postgres-bourse`, when `GET http://localhost:8080/api/bourse/watchlist` avec un jeton obtenu par `POST /api/identite-clients/connexion`, then 200 et les 9 valeurs triées.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh` (lignes bourse à OK), `trace-smoke.sh`, `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 26 findings — high 0, medium 2, low 13, false 11, maybe-false 0
- findings:
  - `[false]` `[reject]` Blind : `_bmad/custom/bmad-build-auto.toml` mêlé à la story — commit 46b6592 de l'opérateur, postérieur à la ligne de base ; hors du changement de la story, non modifié par elle.
  - `[low]` `[reject]` Blind : le profil `rejeu` activé par `compose.rejeu.yaml` n'a aucun fichier de propriétés — voulu (Design Notes), l'AC n'exige que l'application du profil ; contenu livré par la story « Socle du rejeu final ».
  - `[medium]` `[defer]` Blind : aucune commande outillée ne charge `reference-data.sql` — convention existante (identite-clients), `make reset-data` prévu au socle du rejeu ; reporté au frontmatter `deferred`.
  - `[low]` `[reject]` Blind : `reference-data.sql` non rejouable et hors transaction — `make reset-data` vide les tables avant de recharger (spine) ; même forme que le fichier d'identite-clients.
  - `[low]` `[reject]` Blind : pas de contrainte `check` en base pour marchés et devises — seuls le Java et les données de référence (valides, testées par ReferenceDataIT) écrivent ; garde sans écrivain fautif démontré.
  - `[low]` `[reject]` Blind : contenu du symbole non contrôlé (format) — aucune règle de format au PRD ni au legacy ; en ajouter une inventerait un comportement ; l'affichage React échappe le texte.
  - `[false]` `[reject]` Blind : marché sensible à la casse, devise mise en majuscules — exactement FR-009-12/FR-029-10 (devise en majuscules, marché seulement rogné) ; NouvelleValeurTest le couvre.
  - `[low]` `[reject]` Blind : aucune trace de qui retire une valeur — non demandé (FR-009-13, aucune action d'audit au catalogue pour la bourse) ; ajout hors exigence.
  - `[low]` `[reject]` Blind : chemin relatif `reference-data/…` dans les tests — Surefire lance depuis le module, même motif qu'identite-clients ; aucun échec dans les contrôles.
  - `[low]` `[reject]` Blind : chemins d'erreur non testés (415, corps `null`, barre finale, `alg: none`) — 415 et corps illisible couverts par `refuse_un_corps_illisible` ; le reste relève de Spring/Nimbus, sans défaut observé.
  - `[false]` `[reject]` Blind : JSON d'erreur construit par concaténation dans `ConfigurationSecurite.ecrire` — seuls des textes constants sans guillemet y passent ; aucun JSON invalide possible aujourd'hui.
  - `[low]` `[reject]` Blind : 201 sans en-tête `Location` — non exigé par AD-17 ni l'AC ; le corps porte l'`id`.
  - `[low]` `[reject]` Blind : `refuse_de_demarrer_sans_cle_publique` vérifie peu — le texte « nb.jeton.cle-publique est obligatoire » n'est produit que par `lireClePublique` ; même motif qu'audit.
  - `[low]` `[patch]` Edge : tri dépendant de la collation de la base (en_US.utf8 ignore la ponctuation : SGO.PA avant SG.PA) — `order by market collate "C", symbol collate "C"` ; test `trie_les_symboles_dans_l_ordre_binaire_ponctuation_comprise` (FR-029-11) ajouté à WatchlistIT, vert.
  - `[low]` `[reject]` Edge : collision de clé primaire rendue comme doublon de symbole — n'arrive que si le `setval` des données de référence est sauté ; la parade ajoute une branche.
  - `[low]` `[reject]` Edge : statut 413/415 au dispatch d'erreur avec un texte de 400 — cas rares hors contrôleur, même motif qu'audit ; écart cosmétique.
  - `[low]` `[reject]` Edge : 405 sans en-tête `Allow` — cosmétique, même motif qu'identite-clients ; aucun client ne s'en sert.
  - `[low]` `[reject]` Edge : déconnexion du client journalisée en ERROR — bruit de journal rare ; parade = branche supplémentaire.
  - `[low]` `[reject]` Edge : données de référence chargées deux fois — même raison que le constat Blind correspondant.
  - `[medium]` `[patch]` Verification gap : `AttributsErreur` et le `permitAll` du dispatch d'erreur jamais traversés par une vraie requête — test `une_requete_rejetee_par_le_pare_feu_passe_par_le_dispatch_d_erreur_et_ne_rend_que_le_message` dans BourseSecuriteIT (`GET /api/bourse//watchlist` avec et sans jeton → 400 `{"message":"Requête invalide"}`), vert.
  - `[false]` `[reject]` Verification gap (autre) : la matrice place SG.PA « entre OR.PA et SAN.PA » — erreur de transcription de la spec ; la règle de l'AC (marché puis symbole) la place entre SAN.PA et TTE.PA, ce que code et test font ; correctif = éditer la spec, rejeté.
  - `[false]` `[reject]` Intent : critères de pile vérifiés seulement en processus — vérifiés sur une pile compose isolée (voir Auto Run Result) : connexion des trois rôles, GET/POST/DELETE par le conteneur web.
  - `[false]` `[reject]` Intent : aucune preuve des contrôles — `build.sh`, `check-gates-exist.sh`, `golden-master.sh` lancés en code 0, `trace-smoke` rejoué sur la pile isolée.
  - `[false]` `[reject]` Intent : tags FR couverts par des `*IT` qui exigent Docker — Surefire inclut `*IT` (POM parent), convention des autres services ; chaque groupe sort en code 0.
  - `[false]` `[reject]` Intent : fichier toml hors intention — même constat que le premier constat Blind.
  - `[false]` `[reject]` Intent : petits écarts de portée (405, `/actuator/info`) — cohérents avec la spec, aucun défaut.

## Design Notes

- **Contrôles de longueur, marché, devise** : FR-029-13 (A-029-2) veut une limite de saisie côté écran, sans message ; l'API refuse tout de même (« Requête invalide », clé `api.erreur.400`) pour ne jamais tronquer (BUG-036) ni rendre 500 (BUG-035). Longueurs comptées après rognage.
- **Doublon** : vérifié par le cas d'usage, garanti par l'index unique `upper(symbol)` ; l'adaptateur traduit `DuplicateKeyException` en `AjoutRefuse` DOUBLON pour qu'une course rende le même 400.
- **`compose.rejeu.yaml`** : l'AC exige le profil `rejeu` pour bourse dès maintenant ; le fichier est créé minimal, la story « Socle du rejeu final » y ajoutera les autres services.
- **Données de référence** : non chargées automatiquement par compose (comme identite-clients, `make reset-data` viendra avec le socle du rejeu) ; la vérification sur la pile les charge par `docker compose exec -T postgres-bourse psql -U bourse -d bourse < services/bourse/reference-data/reference-data.sql`.

## Verification

**Commands:**
- `mvn -B -q -f services/bourse/pom.xml verify` -- expected: code 0 (tests, JaCoCo)
- `for t in FR-009-01 FR-009-10 FR-009-11 FR-009-12 FR-009-13 FR-029-09 FR-029-10 FR-029-11 FR-029-13 FR-029-14; do mvn -B -q -f services/bourse/pom.xml test -Dgroups=$t -Dsurefire.failIfNoSpecifiedTests=true || echo KO $t; done` -- expected: aucun KO
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0
- `docker compose up -d --build --wait` puis chargement du `reference-data.sql`, connexion `user1`, `curl` GET/POST/DELETE par `localhost:8080`, puis `scripts/gates/trace-smoke.sh` -- expected: statuts de la matrice, trace-smoke code 0

## Auto Run Result

Status: done

**Résumé :** nouveau service hexagonal `bourse` (Spring Boot, PostgreSQL `postgres-bourse`, Flyway, serveur de ressources JWT, tracing, sans Kafka) avec la table `watchlist_items`, les 9 valeurs de référence hors migrations et l'API `GET`/`POST /api/bourse/watchlist`, `DELETE /api/bourse/watchlist/{id}`, ouverte aux trois rôles ; branché dans le reactor Maven, `compose.yaml`, un `compose.rejeu.yaml` initial, `make secrets` et les Dockerfiles.

**Fichiers :**
- `services/bourse/**` — module complet (domaine `NouvelleValeur`/`AjoutRefuse`/`ValeurIntrouvable`/`ValeurWatchlist`, trois cas d'usage, `DepotWatchlistJdbc`, `PointEntreeWatchlist`, `GestionErreurs`, sécurité, migration V1, `reference-data.sql`, 52 tests dont les trois `*ArchitectureTest`).
- `pom.xml` — module `services/bourse`.
- `compose.yaml` — `postgres-bourse` (`pgdata-bourse`), `bourse` (8087), dépendance de `web`.
- `compose.rejeu.yaml` — créé : profil `rejeu` sur `bourse`.
- `services/{audit,coeur-bancaire,identite-clients,notifications}/Dockerfile` — copie du POM bourse (reactor).
- `Makefile`, `.env.example` — `NB_BOURSE_BD_MOT_DE_PASSE`.

**Revue :** 26 constats ; 2 correctifs appliqués (tri en collation "C", test d'intégration du dispatch `/error`) ; 1 report (chargement outillé des données de référence) ; 23 rejets motivés dans le journal de triage. Correctifs par verdict : medium 1, low 1, high 0. Revue complémentaire recommandée : non.

**Vérification :**
- `scripts/gates/build.sh`, `check-gates-exist.sh` (lignes bourse à OK), `golden-master.sh` : code 0 (avant et après correctifs).
- `mvn -f services/bourse/pom.xml test -Dtest='*ArchitectureTest'` : code 0 ; chacune des 10 FR en `-Dgroups` : code 0, 2 à 10 tests par FR.
- Pile : une autre session occupe `localhost:8080`/`16686` avec le même nom de projet compose ; pour ne pas la perturber, pile complète montée sous le projet `nbw-fluxb-verif` (ports 18080/16687, sous-réseau 172.31.251.0/24, surcouche jetable dans le scratchpad), avec `compose.rejeu.yaml` (`SPRING_PROFILES_ACTIVE=rejeu` constaté). Données de référence chargées (identite-clients générée avec des mots de passe jetables). Connexion de user1, sophie et admin → `GET /api/bourse/watchlist` 200, même liste de 9 valeurs, AI.PA en tête. Matrice rejouée par le conteneur web : 401 sans jeton, 201 SG.PA normalisé (rangé entre SAN.PA et TTE.PA), 400 avec les trois textes legacy, 400 « Requête invalide » (21 caractères, Bourse de Tokyo, JPY), 204, 404, 400 sur id non numérique. Traces bourse dans Jaeger avec spans JDBC. Logique de `trace-smoke.sh` rejouée sur cette pile (copie avec ports adaptés, script d'origine intact) : 6 OK, code 0. Pile isolée supprimée (`down -v`).

**Risques résiduels :**
- `scripts/gates/trace-smoke.sh` lui-même n'a pas été lancé sur les images de ce checkout (ports figés pris par la pile d'une autre session) ; seule sa copie à ports adaptés l'a été.
- La matrice de l'intent-contract place SG.PA « entre OR.PA et SAN.PA » : erreur de transcription ; la règle de l'AC (marché puis symbole) donne SAN.PA < SG.PA < TTE.PA, ce qu'implémentent le code et les tests.
- Le profil `rejeu` n'a pas encore de propriétés ; la watchlist reste vide sur la pile tant que `make reset-data` n'existe pas (report).
