---
title: 'Raccorder le fournisseur de cotations AlphaVantage derrière un port simulé, avec coupe-circuit et cache'
type: 'feature'
created: '2026-10-06'
baseline_revision: '2e4b120f52bc38ce5f9792cbeff6a62c70548f6b'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-7-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** Le service `bourse` ne sait pas encore fournir l'historique des cours d'une valeur (FEAT-009, FEAT-029) ; aucun fournisseur externe n'est raccordé et la bibliothèque de coupe-circuit et de cache (AR-102) n'est pas choisie.

**Approach:** Ajouter `GET /api/bourse/historique?symbole=&periode=`, un port sortant `Cotations` avec l'adaptateur simulé `AlphaVantageSimule` (actif par défaut, séries fixes datées par le port `Horloge`) et l'adaptateur réel `AlphaVantageCotations` (délai d'attente, coupe-circuit Resilience4j, cache Caffeine, clé et adresse lues hors des fichiers versionnés), et inscrire ces deux bibliothèques dans `versions-verifiees.md`.

## Boundaries & Constraints

**Always:**
- Réponse 200 `{"symbole":"AI.PA","periode":"1M","points":[{"date":"2026-10-19","ouverture":"…","haut":"…","bas":"…","cloture":"…","volume":123}]}` : prix en chaîne décimale exacte (`BigDecimal#toPlainString`, jamais de flottant), `volume` entier, `date` `yyyy-MM-dd`, points triés par date croissante. La story 7.3 ajoutera `graphique` à cet objet.
- Périodes : `1W` et `1M` → série journalière (compacte, 100 points), date limite aujourd'hui − 1 semaine / − 1 mois ; `1Y` et `5Y` → série hebdomadaire, − 1 an / − 5 ans ; période absente ou inconnue → série journalière et date limite d'un mois. Point exclu si sa date est strictement antérieure à la date limite. « Aujourd'hui » = `Horloge#aujourdhui()` (Europe/Paris).
- `symbole` absent ou blanc → 400 `{"message":"Requête invalide"}` ; symbole rogné, casse inchangée.
- Port `Horloge` (`application.port.out`, copie de celui du cœur bancaire) et `adapter.out.horloge.HorlogeSysteme` (`nb.fuseau=Europe/Paris` dans `application.properties`).
- Fournisseur dans `adapter.out.fournisseur` uniquement (clients HTTP, Resilience4j, Caffeine). Mode par `nb.fournisseurs.alphavantage.mode` (`simule` par défaut, `reel`) ; réglages `nb.fournisseurs.alphavantage.{delai-attente, seuil-echecs, duree-ouverture, duree-cache}` dont les valeurs par défaut sont dans le code (aucune propriété `nb.fournisseurs.*` dans les fichiers versionnés) ; mode inconnu, durée nulle ou négative, seuil < 1 → le contexte refuse de démarrer.
- Clé `ALPHA_VANTAGE_API_KEY` et adresse `ALPHA_VANTAGE_BASE_URL` lues : variable d'environnement, puis propriété système (valeur blanche = absente) ; adresse par défaut `https://www.alphavantage.co/query` ; la clé n'a aucune valeur par défaut et n'apparaît jamais dans un journal (ni l'URL qui la porte).
- Mode réel, toute indisponibilité → 200 liste vide, jamais de 5xx ni de détail technique : sans clé (aucune requête émise), statut non 2xx, champ `Information`, `Note` ou `Error Message`, corps illisible, délai dépassé, coupe-circuit ouvert. Échec journalisé (WARN/ERROR) dans la requête HTTP tracée, ligne portant un `traceId` non vide.
- Coupe-circuit : s'ouvre quand `seuil-echecs` appels consécutifs échouent (exception, non 2xx, illisible, délai, `Information`/`Note`) ; `Error Message` et série vide ne sont pas des échecs du coupe-circuit. Cache : clé (symbole, série), durée `duree-cache`, seules les séries non vides réussies sont mises en cache ; 1W et 1M partagent la même série journalière.
- Tests tagués `@Tag` : FR-009-08, FR-009-09, FR-009-14, FR-029-01, FR-029-02, FR-029-03, FR-029-04, FR-029-05, BUG-034, BUG-091 ; mode réel testé contre un bouchon local (`com.sun.net.httpserver.HttpServer`, compteur de requêtes), sans réseau ni clé réelle.

**Never:**
- Pas de graphique SVG ni d'écran (7.3, 7.4) ; pas de limiteur de débit en plus du cache ; pas d'appel HTTP entre services.
- Jamais `mode=reel` ni clé dans un fichier versionné ; aucun `now()` hors `adapter.out.horloge` (ArchUnit `HorlogeArchitectureTest`).
- Pas de Spring Cache ni de starter Resilience4j : usage programmatique dans l'adaptateur.
- Ne pas modifier `scripts/gates/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `sprint-status.yaml`, ni le golden master.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Simulé 1M | Horloge 2026-11-17, `AI.PA`, `1M` | 200, série journalière fixe d'AI.PA, premier point ≥ 2026-10-17, tri croissant | — |
| Simulé 1W / 1Y / 5Y | idem | journalière ≥ 2026-11-10 ; hebdomadaire ≥ 2025-11-17 ; hebdomadaire ≥ 2021-11-17 (la série simulée remonte avant ces dates) | — |
| Période inconnue | `periode=XX` ou absente | journalière, ≥ 2026-10-17 | — |
| Symbole inconnu | `INCONNU.XX` | 200 `points: []` | — |
| Symbole manquant | absent / `"  "` | 400 « Requête invalide » | — |
| Réel, échec fournisseur | bouchon : `Information`, `Note`, `Error Message`, 500, réponse après le délai | 200 `points: []` dans le délai | WARN/ERROR avec `traceId` |
| Réel, coupe-circuit | bouchon en 500 répétés, `seuil-echecs` atteint | appels suivants : compteur du bouchon inchangé, 200 vide | — |
| Réel, cache | 1M, 1M, 1W pour AI.PA | bouchon : 1 requête (`TIME_SERIES_DAILY`, `outputsize=compact`) | — |
| Réel, sans clé | aucune clé | bouchon : 0 requête ; 200 vide | WARN sans clé |

</intent-contract>

## Code Map

- `services/bourse/src/main/java/fr/nextbanking/bourse/adapter/in/http/{PointEntreeWatchlist,GestionErreurs,Textes}.java` -- motif de contrôleur et d'erreurs ; `MissingServletRequestParameterException` est déjà rendue 400 « Requête invalide » par `autreErreur` (ErrorResponse).
- `services/bourse/src/main/java/fr/nextbanking/bourse/infrastructure/AssemblageWatchlist.java` -- motif d'assemblage des cas d'usage en `@Bean`.
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/application/port/out/Horloge.java` et `.../adapter/out/horloge/HorlogeSysteme.java` (+ `HorlogeSystemeTest`) -- à copier dans bourse.
- `services/bourse/src/test/java/fr/nextbanking/bourse/{ClientsHttpArchitectureTest,HorlogeArchitectureTest,HexagonalArchitectureTest}.java` -- lecture seule : clients HTTP seulement dans `..adapter.out.fournisseur..` (donc le `RestClient`/`HttpClient` est construit dans ce paquet, pas dans `infrastructure`) ; onion architecture (un adaptateur ne dépend pas d'un autre adaptateur).
- `services/identite-clients/src/test/java/fr/nextbanking/identiteclients/adapter/ConnexionIT.java` -- motif `RANDOM_PORT` + `OutputCaptureExtension` + regex `traceId=(\S*) spanId=(\S*)\]` pour prouver le `traceId` d'une ligne de journal.
- `services/bourse/src/test/java/fr/nextbanking/bourse/adapter/{Jetons,WatchlistIT}.java`, `infrastructure/{Conteneurs,ContexteDemarre}.java` -- fabrication des jetons et contexte de test (PostgreSQL Testcontainers).
- `services/bourse/src/main/resources/application.properties` -- ajouter `nb.fuseau=Europe/Paris` ; rien sous `nb.fournisseurs.*`.
- `pom.xml` (racine) -- `<properties>` et `<dependencyManagement>` : ajouter `resilience4j.version` 2.4.0 (Caffeine 3.2.4 est géré par le BOM Boot 4.1.1).
- `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/versions-verifiees.md` -- tableau ; vérifié : Resilience4j 2.4.0 publié le 2026-03-14 (https://repo1.maven.org/maven2/io/github/resilience4j/resilience4j-circuitbreaker/maven-metadata.xml), dépend seulement de `resilience4j-core` et `slf4j-api` (aucune dépendance Spring) ; Caffeine 3.2.4 = `caffeine.version` du BOM https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom (dernière : 3.3.0).
- `.../ARCHITECTURE-SPINE.md` ligne ~626 -- point `[OPEN] Coupe-circuit et cache des fournisseurs` à marquer réglé (renvoi à versions-verifiees.md).
- `compose.yaml` (service `bourse`, ~l.279) et `.env.example` -- passer `ALPHA_VANTAGE_API_KEY`, `ALPHA_VANTAGE_BASE_URL` (vides par défaut), sans aucun mode.

## Tasks & Acceptance

**Execution:**
- `pom.xml`, `services/bourse/pom.xml` -- `resilience4j-circuitbreaker` 2.4.0 (géré à la racine) et `com.github.ben-manes.caffeine:caffeine` dans bourse.
- `services/bourse/src/main/java/fr/nextbanking/bourse/domain/` -- `PointCotation` (record date, ouverture, haut, bas, cloture `BigDecimal`, volume `long`), `TypeSerie` (JOURNALIERE, HEBDOMADAIRE), `Periode` (code → série et date limite depuis une `LocalDate`, inconnue → 1 mois journalière), filtrage/tri des points.
- `services/bourse/src/main/java/fr/nextbanking/bourse/application/` -- `port/out/Horloge`, `port/out/Cotations` (`serie(symbole, TypeSerie)` → liste), `port/in/ConsulterHistorique`, `ConsulterHistoriqueCasUsage` (symbole blanc → exception de domaine rendue 400).
- `services/bourse/src/main/java/fr/nextbanking/bourse/adapter/out/horloge/HorlogeSysteme.java` -- copie du cœur bancaire.
- `services/bourse/src/main/java/fr/nextbanking/bourse/adapter/out/fournisseur/` -- `ProprietesAlphaVantage` (`@ConfigurationProperties("nb.fournisseurs.alphavantage")`, record, défauts dans le code : mode simule, délai 5 s, seuil 5, ouverture 60 s, cache 12 h ; validation dans le constructeur compact), `ParametresDeploiement` (lecture variable d'environnement puis propriété système, sources injectables pour les tests), `AlphaVantageSimule` (séries fixes pour les 9 symboles de référence : 100 jours ouvrés finissant à `aujourdhui`, 270 semaines finissant au dernier vendredi, valeurs déterministes à 4 décimales ; inconnu → vide), `AlphaVantageCotations` (requête `function=TIME_SERIES_DAILY&outputsize=compact` ou `TIME_SERIES_WEEKLY`, `symbol`, `apikey` ; lecture de `Time Series (Daily)` / `Weekly Time Series`, champs `1. open`…`5. volume`), `ConfigurationAlphaVantage` (bean `Cotations` choisi par le mode, `RestClient` sur `JdkClientHttpRequestFactory` avec délais de connexion et de lecture).
- `services/bourse/src/main/java/fr/nextbanking/bourse/adapter/in/http/PointEntreeHistorique.java` -- `GET /api/bourse/historique` ; prix en chaîne ; `GestionErreurs` traduit l'exception de symbole manquant en 400.
- `services/bourse/src/main/java/fr/nextbanking/bourse/infrastructure/` -- assemblage de `ConsulterHistorique` ; `@ConfigurationPropertiesScan` ou équivalent.
- `services/bourse/src/test/java/...` -- tests unitaires domaine/application (matrice) ; `adapter/out/fournisseur/` : simulateur, propriétés (refus de démarrer sur valeur invalide), `ParametresDeploiement` (env prioritaire, propriété système, défaut), test de configuration sans `nb.fournisseurs.*` → bean `AlphaVantageSimule` ; `adapter/HistoriqueIT` (Horloge fixe au 2026-11-17 en `@Primary`, trois rôles, périodes, inconnu, 400) ; `adapter/AlphaVantageReelIT` (bouchon local en mode réel : échecs, délai, coupe-circuit, cache, sans clé, adresse, `traceId`) ; `SecretsTest` BUG-091/BUG-034/FR-009-14/FR-029-05 qui lit tous les `src/main/resources/application*.{properties,yml,yaml}` et échoue sur toute propriété dont le nom contient `api-key`, `apikey`, `api_key` ou `cle-api` (casse ignorée) avec une valeur non vide ou un défaut `${…:valeur}`, ainsi que sur `mode=reel`.
- `compose.yaml`, `.env.example` -- variables AlphaVantage passées au service `bourse`, vides par défaut.
- `versions-verifiees.md`, `ARCHITECTURE-SPINE.md` -- deux lignes AR-102 ; point ouvert marqué réglé.

**Acceptance Criteria:**
- Given `versions-verifiees.md`, when je le lis, then une ligne Resilience4j et une ligne Caffeine donnent version, URL, date de vérification et compatibilité Boot 4.1.1, en citant AR-102.
- Given un contexte bourse sans aucune propriété `nb.fournisseurs.*`, when le test de configuration s'exécute, then le bean `Cotations` est `AlphaVantageSimule` ; et `! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' services/*/src/main/resources compose*.yaml` sort en 0.
- Given une clé ajoutée en dur dans `services/bourse/src/main/resources/application.yml`, when `mvn -B -q -f services/bourse/pom.xml test -Dgroups='BUG-091'`, then code non nul (vérifié puis fichier retiré).
- Given la story terminée, when `mvn … test -Dgroups='<tag>'` pour chacun des 10 tags, then code 0 avec au moins un test par tag, sans clé ni réseau ; `git grep -nEi 'apikey *[:=] *[A-Za-z0-9]{4,}'` ne trouve rien.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 6 findings — high 0, medium 0, low 2, false 4, maybe-false 0
- findings:
  - `[false]` `[reject]` Intent : lectures où la story attendrait un geste humain (clé AlphaVantage réelle, validation des versions) — aucun critère n'exige de clé ni d'appel au vrai service (mode réel testé contre un bouchon local, `-Dgroups` « sans clé et sans réseau ») ; les lignes de versions n'exigent qu'une présence ; la validation humaine de la story 1.6 portait sur des surcharges de sécurité du BOM, absentes ici. Pas d'`operator_actions`.
  - `[false]` `[reject]` Intent : spec en `in-review`, sans Auto Run Result — état intermédiaire attendu pendant la revue ; finalisation faite à la suite de ce triage.
  - `[low]` `[patch]` Intent : aucun test ne démarre le contexte en mode réel avec la lecture réelle des paramètres de déploiement (l'ordre n'est prouvé qu'avec des sources injectées) — test `en_mode_reel_l_adresse_et_la_cle_sont_lues_dans_les_proprietes_systeme_au_demarrage` (FR-029-04) ajouté à `ConfigurationAlphaVantageTest` : propriétés système vers un bouchon local, bean `ParametresDeploiement` par défaut, une requête reçue avec la clé de test ; vert.
  - `[low]` `[reject]` Intent : `SecretsTest` (BUG-091) contrôle les fichiers par nom de propriété, pas ce que lirait le service — la clé n'est jamais lue comme propriété Spring, donc une clé versionnée sous un autre nom resterait sans effet ; la contre-épreuve de l'AC (clé puis défaut `${ALPHA_VANTAGE_API_KEY:demo}` dans `application.yml`) donne bien code 1 ; durcir l'analyse ajouterait de la complexité pour un cas improbable.
  - `[false]` `[reject]` Intent : aucune preuve d'exécution des commandes dans le diff — commandes lancées en vérification (voir Auto Run Result), toutes en code 0.
  - `[false]` `[reject]` Intent : choix de conception propres à la spec (`Error Message` hors coupe-circuit, cache des seules séries non vides, 400 sur symbole manquant) — constat descriptif, cohérent avec les critères ; aucun défaut.

## Design Notes

- **Bibliothèques (AR-102)** : Resilience4j `circuitbreaker` seul (Java pur, sans Spring, donc indépendant de la version de Boot ; le starter `resilience4j-spring-boot4` n'est pas nécessaire) et Caffeine géré par Boot. La story 7.7 les réutilise de la même façon.
- **Coupe-circuit « N échecs consécutifs »** : `COUNT_BASED`, `slidingWindowSize = minimumNumberOfCalls = seuil-echecs`, `failureRateThreshold = 100`, `waitDurationInOpenState = duree-ouverture`. `CallNotPermittedException` → liste vide.
- **Priorité env puis propriété système** : Spring donne la priorité inverse ; d'où `ParametresDeploiement` lu à la main, hors des fichiers de configuration. La clé n'étant jamais une propriété Spring, le test BUG-091 contrôle les fichiers versionnés eux-mêmes.
- **Période inconnue** : RG-029-003 ne fixe que la date limite ; la série journalière est retenue (le legacy ne demande l'hebdomadaire que pour 1Y et 5Y).

## Verification

**Commands:**
- `mvn -B -q -f services/bourse/pom.xml verify` -- expected: code 0 (tests, JaCoCo)
- `for t in FR-009-08 FR-009-09 FR-009-14 FR-029-01 FR-029-02 FR-029-03 FR-029-04 FR-029-05 BUG-034 BUG-091; do mvn -B -q -f services/bourse/pom.xml test -Dgroups=$t -Dsurefire.failIfNoSpecifiedTests=true || echo KO $t; done` -- expected: aucun KO
- `mvn -B -q -o -f services/bourse/pom.xml verify` (cache rempli, sans `ALPHA_VANTAGE_API_KEY`) -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` puis `scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `GET /api/bourse/historique?symbole=&periode=` dans le service bourse, derrière le port sortant `Cotations` : adaptateur simulé `AlphaVantageSimule` actif par défaut (séries fixes des 9 valeurs de référence, datées par le nouveau port `Horloge`) et adaptateur réel `AlphaVantageCotations` (délai d'attente, coupe-circuit Resilience4j 2.4.0, cache Caffeine 3.2.4, clé et adresse lues dans l'environnement puis les propriétés système, sans valeur par défaut pour la clé). AR-102 réglé dans `versions-verifiees.md` et la spine. Aucun geste humain requis : pas d'`operator_actions`.

**Fichiers :**
- `services/bourse/src/main/java/.../domain/{Periode,PointCotation,TypeSerie,HistoriqueCours,SymboleManquant}.java` — périodes, date limite, filtrage et tri.
- `.../application/{ConsulterHistoriqueCasUsage, port/in/ConsulterHistorique, port/out/Cotations, port/out/Horloge}.java` — cas d'usage et ports.
- `.../adapter/out/horloge/HorlogeSysteme.java` — horloge Europe/Paris (`nb.fuseau`).
- `.../adapter/out/fournisseur/{AlphaVantageSimule,AlphaVantageCotations,ConfigurationAlphaVantage,ProprietesAlphaVantage,ParametresDeploiement}.java` — fournisseur simulé/réel, réglages validés au démarrage.
- `.../adapter/in/http/{PointEntreeHistorique,GestionErreurs}.java` — point d'API, 400 sur symbole manquant.
- `.../infrastructure/AssemblageHistorique.java`, `application.properties` — assemblage, `nb.fuseau`.
- Tests : `PeriodeTest`, `ConsulterHistoriqueTest`, `HorlogeSystemeTest`, `ProprietesAlphaVantageTest`, `ParametresDeploiementTest`, `AlphaVantageSimuleTest`, `ConfigurationAlphaVantageTest`, `AlphaVantageCotationsTest`, `HistoriqueIT`, `AlphaVantageReelIT`, `AlphaVantageSansCleIT`, `SecretsTest`, bouchon `BouchonAlphaVantage`, `HorlogeFixe`.
- `pom.xml`, `services/bourse/pom.xml` — Resilience4j (géré à la racine) et Caffeine.
- `compose.yaml`, `.env.example` — `ALPHA_VANTAGE_API_KEY`, `ALPHA_VANTAGE_BASE_URL` vides par défaut.
- `versions-verifiees.md`, `ARCHITECTURE-SPINE.md` — lignes AR-102, point ouvert tranché.

**Revue :** 6 constats (couche d'alignement sur l'intention seule, configuration accélérée) ; 1 correctif (test de démarrage en mode réel, low) ; 0 report ; 5 rejets motivés dans le journal de triage. Correctifs par verdict : high 0, medium 0, low 1. Revue complémentaire recommandée : non.

**Vérification :**
- `mvn -B -q -o -f services/bourse/pom.xml verify` sans `ALPHA_VANTAGE_API_KEY` : code 0 (avant et après correctif).
- Les 10 tags en `-Dgroups` : code 0, de 3 à 13 cas par tag.
- Contre-épreuve BUG-091 : clé en dur puis défaut `${ALPHA_VANTAGE_API_KEY:demo}` dans `application.yml` → code 1 ; fichier retiré.
- `! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' …` : 0 ; `git grep -nEi 'apikey *[:=] *[A-Za-z0-9]{4,}'` : aucun résultat.
- `build.sh`, `check-gates-exist.sh` (aucun KO), `golden-master.sh`, `trace-smoke.sh` : code 0 ; image Docker `bourse` construite.
- Matrice : chaque ligne couverte par `HistoriqueIT`, `AlphaVantageReelIT` ou `AlphaVantageSansCleIT`, tous verts.

**Risques résiduels :**
- `trace-smoke.sh` a tourné sur la pile compose d'un autre checkout (sans bourse) ; le service bourse n'a pas été démarré par compose dans cette story (image construite seulement).
- La réponse renvoie la période appliquée : `periode=XX` ou absente donne `"periode":"1M"`.
- `AlphaVantageReelIT` ordonne ses tests (coupe-circuit en dernier, il reste ouvert pour le contexte).
