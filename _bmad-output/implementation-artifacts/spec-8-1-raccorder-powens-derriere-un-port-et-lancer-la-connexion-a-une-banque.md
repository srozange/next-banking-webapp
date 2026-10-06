---
title: 'Raccorder Powens derrière un port et lancer la connexion à une banque'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: 'a5fdef98c801d20c2da2255a52959cc1c175d1c4'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-8-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-7-2-raccorder-le-fournisseur-de-cotations-alphavantage-derriere-un-port-simule-avec-coupe-circuit-et-cache.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** Le service `open-banking-conseiller` ne sait pas appeler Powens : un utilisateur ne peut pas connecter une banque externe, et rien ne garantit qu'en local, en test et dans les contrôles aucun appel réseau ni aucune clé ne soit nécessaire.

**Approach:** Port sortant Powens (5 opérations) avec `PowensSimule` actif par défaut et un adaptateur réel (délais 15 s, coupe-circuit Resilience4j), calqués sur le fournisseur AlphaVantage du service `bourse`. État Powens par client en base (migration V2), `state` signé à usage unique émis au lancement, et deux points d'API authentifiés : `POST /api/open-banking-conseiller/powens/connexion` et `GET /api/open-banking-conseiller/powens`.

## Boundaries & Constraints

**Always:** Hexagone (domain/application Java pur ; `RestClient` uniquement dans `adapter.out.fournisseur`, `ClientsHttpArchitectureTest` vert) ; temps seulement par le port `Horloge` (copie du modèle `bourse`) ou `adapter.out.jeton` ; réglages `nb.fournisseurs.powens.*` validés au démarrage (défauts : domaine `demi-sandbox.biapi.pro`, client `99961298`, banques `59`) ; adresse de retour et secret client = paramètres de déploiement lus hors fichier (comme `ParametresDeploiement` de `bourse`), secret sans défaut ; jeton permanent enregistré en base AVANT de construire l'adresse ; versions Resilience4j 2.4.0 / Caffeine 3.2.4 déjà vérifiées (pas de nouveau choix) ; textes à l'identique : `{"message": "Impossible d initialiser la connexion Powens."}` ; JSON français camelCase ; clé de l'état = `sub` du JWT ; code, tests et messages de commit en français.

**Never:** `mode=reel` dans `src/main/resources` ou `compose*.yaml` ; secret client ou jeton Powens dans une réponse, un journal, un fichier versionné ; appel réseau externe dans un test ; avaler une panne en liste vide (une panne = erreur d'indisponibilité) ; implémenter le retour public `GET .../powens/retour` (8.2), la sélection, le rafraîchissement, la déconnexion (8.3) ou le front ; toucher à `scripts/gates/**`, `docs/quality-gates.md`, golden master, `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Connexion simulée | JWT valide, sans état Powens | 200 `{adresse}` = retour simulé + `connection_id` fixe + `state` ; jeton persisté avant l'adresse | — |
| Connexion réelle (constructeur d'adresse) | domaine, client_id, retour, code, banques, state | URL avec retour et code encodés | — |
| Refus de création / réponse sans jeton | `PowensSimule` en mode erreur | 503 `{"message":"Impossible d initialiser la connexion Powens."}`, pas d'adresse | état client inchangé (non connecté) |
| État initial | JWT valide, jamais connecté | 200 non connecté, sans compte, sans message | aucun appel à Powens |
| Sans JWT | POST connexion / GET état | 401 `{"message":"Authentification requise"}` | aucun appel à Powens |
| Mode réel sans secret | `mode=reel`, secret absent | échec du démarrage | `IllegalArgumentException` à la configuration |
| Coupe-circuit ouvert / 5xx / délai | adaptateur réel contre bouchon local | erreur d'indisponibilité (jamais liste vide) | délais 15 s connexion et lecture vérifiés |

</intent-contract>

## Code Map

- `services/bourse/src/main/java/fr/nextbanking/bourse/adapter/out/fournisseur/` -- modèle : `ProprietesAlphaVantage` (record `@ConfigurationProperties`, défauts + validation), `ParametresDeploiement` (env puis propriétés système, blanc = absent), `ConfigurationAlphaVantage` (bean simulé si `!reel()`, `RestClient` + `JdkClientHttpRequestFactory` délais), `AlphaVantageCotations` (CircuitBreaker COUNT_BASED L64-70, `executeCallable`, `CallNotPermittedException`, `EchecFournisseur`) ; différence : ici une panne lève une erreur, pas de liste vide.
- `services/bourse/src/test/.../adapter/out/fournisseur/BouchonAlphaVantage.java`, `ConfigurationAlphaVantageTest`, `AlphaVantageCotationsTest`, `SecretsTest` -- modèles de tests (bouchon `HttpServer` local, `ApplicationContextRunner`, `OutputCaptureExtension`, scan des `application*`).
- `services/bourse/src/main/java/.../application/port/out/Horloge.java`, `adapter/out/horloge/HorlogeSysteme.java`, `nb.fuseau=Europe/Paris` -- à copier dans OBC (absent) pour l'expiration du `state`.
- `services/bourse/pom.xml` L57-64 -- dépendances resilience4j (et caffeine si utile) à reprendre dans `services/open-banking-conseiller/pom.xml`.
- `services/open-banking-conseiller/src/main/java/fr/nextbanking/openbankingconseiller/` -- `adapter/in/http/{PointEntreeProduits (modèle contrôleur), GestionErreurs (+ Textes.statutPublie qui écrase 503 → handler 503 explicite), ConfigurationSecurite (tout authentifié par défaut : aucun changement)}`, `infrastructure/AssemblageCatalogue` (modèle d'assemblage `@Bean`), `adapter/out/persistance/DepotProduitsJdbc` (modèle JDBC).
- `services/open-banking-conseiller/src/main/resources/{application.properties, db/migration/V1__produits.sql}` -- ajouter `nb.fuseau`, `V2__powens.sql` (état par client `sub`, jeton permanent, indicateur connecté, message, tables des `state` à usage unique avec expiration).
- `services/open-banking-conseiller/src/test/.../adapter/{ProduitsSecuriteIT, Jetons}` et `infrastructure/Conteneurs` -- ajouter les deux requêtes Powens à la liste des 401 ; modèle d'IT (Postgres Testcontainers, JWT RSA).
- `services/open-banking-conseiller/src/test/.../{ClientsHttpArchitectureTest, HexagonalArchitectureTest, HorlogeArchitectureTest}` -- doivent rester verts sans modification.
- `compose.yaml` (service OBC L374-398) et `.env.example` -- variables vides `POWENS_CLIENT_SECRET`, `POWENS_URL_RETOUR`, `POWENS_DOMAINE`, `POWENS_CLIENT_ID`, `POWENS_CONNECTOR_ID` (sur le modèle `ALPHA_VANTAGE_*`), jamais de `_MODE`.
- Lecture seule : `docs/retro/features/FEAT-028/RG.md` (RG-028-001..005, 024, 026, 027), `FEAT-012/RG.md` (connexion), `ARCHITECTURE-SPINE.md` L231, L254, L309-312 (AD-20), `epics.md` L4655-4716.

## Tasks & Acceptance

**Execution:**
- `services/open-banking-conseiller/pom.xml` -- ajouter `resilience4j-circuitbreaker` (et caffeine si utilisé) -- coupe-circuit AR-102
- `.../domain/` (état Powens non connecté/connecté, `StatePowens`) et `.../application/` (`ConnecterPowensCasUsage`, `ConsulterEtatPowensCasUsage`, ports `in` ; ports `out` : `Powens` (5 opérations), `DepotEtatPowens`, `EmetteurState`, `Horloge`) -- logique pure, 100 % couvert
- `.../adapter/out/fournisseur/` -- `ProprietesPowens`, `ParametresDeploiement`, `ConfigurationPowens`, `PowensSimule` (connecteurs « BNP Paribas », « Credit Agricole », comptes/transactions fixes, modes d'erreur AR-61), `PowensReel` (délais 15 s, coupe-circuit, Basic pour `/auth/init`, Bearer ensuite, 20 transactions max, libellé simplifié sinon d'origine), constructeur d'adresse -- port derrière adaptateurs
- `.../adapter/out/{horloge,jeton}` et `adapter/out/persistance/DepotEtatPowensJdbc` -- `HorlogeSysteme`, signature HMAC du `state` à usage unique, persistance ; `resources/db/migration/V2__powens.sql`
- `.../adapter/in/http/PointEntreePowens` + handler 503 dans `GestionErreurs` + `infrastructure/AssemblagePowens` -- `POST .../powens/connexion`, `GET .../powens`
- `application.properties`, `compose.yaml`, `.env.example` -- fuseau, variables de déploiement vides
- Tests tagués `@Tag("FR-028-01")`, `FR-028-02`, `FR-028-12`, `FR-012-17`, `FR-012-05` (au moins un fichier chacun) : unitaires domaine/application, `ConfigurationPowensTest` (simulé par défaut, mode réel sans secret échoue), `SecretsTest` (aucun `mode=reel`, aucun secret), `PowensReelTest` contre bouchon local (délais, coupe-circuit ouvert = indisponibilité), constructeur d'adresse, `PowensIT` (200/503/401, état non connecté, `OutputCaptureExtension` sans jeton ni secret), `ProduitsSecuriteIT` étendu

**Acceptance Criteria:**
- Given un contexte sans `nb.fournisseurs.*`, when il démarre, then le bean du port Powens est `PowensSimule` et `! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' services/*/src/main/resources compose*.yaml` sort en 0
- Given un utilisateur authentifié, when `POST .../powens/connexion`, then 200 avec l'adresse, jeton persisté avant sa construction, `state` signé à usage unique émis ; en mode refus ou sans jeton, 503 au message exact et état inchangé
- Given `GET .../powens` sans connexion, when appelé, then 200 non connecté sans appel Powens ; sans JWT, 401 sur les deux chemins
- Given `mode=reel` sans secret, when le contexte démarre, then échec ; l'adaptateur réel applique 15 s/15 s et un coupe-circuit ouvert lève l'indisponibilité
- Given la story terminée, when `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-028-01,FR-028-02,FR-028-12,FR-012-17,FR-012-05'`, then code 0 ; puis `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` verts (code 0)

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 0, false 3, maybe-false 0 (+1 rejeté hors périmètre)
- findings:
  - `false` `reject` Les contrôles `-Dgroups`, `build.sh`, `trace-smoke.sh` ne sont pas prouvés par le diff — exécutés après l'implémentation : tous en code 0 (mvn test, build, check-gates-exist, golden-master, trace-smoke).
  - `false` `reject` L'adaptateur réel n'est pas parcouru par l'API HTTP — le critère le veut au niveau adaptateur (bouchon local : délais, coupe-circuit) et configuration (`ConfigurationPowensTest`), tous deux couverts.
  - `false` `reject` `GET .../powens` renvoie des listes vides — c'est le comportement demandé (état non connecté, sans compte, sans message) ; comptes et transactions arrivent en 8.3.
  - `low` `reject` L'adresse simulée pointe vers `/powens/retour`, non implémenté — le retour public est la story 8.2 (prérequis explicite de 8.2) ; aucun correctif ici.

## Design Notes

L'état Powens n'a pas de session serveur : une ligne par `sub` ; le `state` = `<sub>.<nonce>.<expiration>` signé HMAC (clé dérivée d'un paramètre de déploiement, jamais versionnée), nonce enregistré pour usage unique (consommé par la story 8.2). `PowensSimule` renvoie `…/api/open-banking-conseiller/powens/retour?connection_id=<fixe>&state=<state>`. Le secret n'est jamais une propriété Spring (lecture à la main comme `ParametresDeploiement`).

## Verification

**Commands:**
- `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-028-01,FR-028-02,FR-028-12,FR-012-17,FR-012-05'` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0
- `scripts/gates/trace-smoke.sh` -- expected: code 0
- `grep -rl '@Tag("FR-028-01")' services/open-banking-conseiller/src/test` (idem pour les quatre autres) -- expected: au moins un fichier

## Auto Run Result

Status: done

**Résumé :** le service `open-banking-conseiller` appelle Powens derrière un port à 5 opérations (`PowensSimule` par défaut, `PowensReel` avec délais 15 s/15 s et coupe-circuit Resilience4j). `POST /api/open-banking-conseiller/powens/connexion` crée l'utilisateur Powens, enregistre le jeton avant de construire l'adresse, émet un `state` HMAC à usage unique et répond avec l'adresse ; `GET /api/open-banking-conseiller/powens` rend l'état lu en base. Échec ou absence de jeton : 503 « Impossible d initialiser la connexion Powens. ».

**Fichiers :** port, adaptateurs, configuration et `ConstructeurAdresse` dans `adapter/out/fournisseur` ; `EmetteurStateHmac` (`adapter/out/jeton`) ; `HorlogeSysteme` ; `DepotEtatPowensJdbc` et `V2__powens.sql` ; cas d'usage et domaine ; `PointEntreePowens`, `GestionErreurs`, `Textes` ; `pom.xml`, `application.properties`, `compose.yaml`, `.env.example` ; tests tagués FR-028-01, FR-028-02, FR-028-12, FR-012-17, FR-012-05.

**Revue :** 4 constats, 0 correctif, 0 différé ; 3 rejetés comme faux (contrôles verts, adaptateur réel couvert au bon niveau, listes vides voulues), 1 rejeté hors périmètre (retour public = story 8.2).

**Recommandation de revue complémentaire :** false.

**Vérification :** `mvn -f services/open-banking-conseiller/pom.xml test -Dgroups=…` code 0 ; les 5 tags présents ; grep `mode=reel` code 0 ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` code 0.

**Risques résiduels :** l'adresse de la page web réelle (`https://webview.powens.com/connect`) et les formes JSON de l'API Powens viennent de la connaissance de l'API, vérifiées seulement contre un bouchon ; `POWENS_URL_RETOUR` vaut un chemin relatif par défaut, à renseigner pour un déploiement réel ; sans secret (mode simulé) la clé du `state` est aléatoire par processus.
