---
title: 'Ouvrir le service open-banking-conseiller et exposer le catalogue des produits et le journal des traitements'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: 'a259f88b0ee63b997354a45ef87d90d145756444'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-7-context.md'
warnings: ['oversized']
deferred:
  - summary: >-
      Aucune commande outillée ne charge services/open-banking-conseiller/reference-data/reference-data.sql dans la pile compose : le catalogue reste vide après `docker compose up`.
    evidence: |-
      Même constat que pour bourse et identite-clients : le chargement est prévu par `make reset-data` (story « Socle du rejeu final »). Le chargement manuel par psql est décrit dans les Design Notes et a été vérifié sur la pile.
    location: >-
      services/open-banking-conseiller/reference-data/reference-data.sql ; Makefile
    severity: medium
---

<intent-contract>

## Intent

**Problem:** Le service `open-banking-conseiller` n'existe pas : aucun utilisateur authentifié ne peut lire le catalogue des produits (FEAT-011, FR-010-01), ni le journal des traitements nocturnes, que l'écran Produits (story 7.6) doit afficher. Le cœur bancaire n'a ni vérification de jeton, ni table `batch_job_logs`, ni lecture du journal (l'epic 4 n'est pas livré).

**Approach:** Créer le module hexagonal `services/open-banking-conseiller` (modèle : `services/bourse`, sans Kafka ni fournisseur) avec ses 4 tables, ses 13 produits de référence hors migrations et `GET /api/open-banking-conseiller/produits[?type=]` ; l'intégrer au reactor, à `compose.yaml`, à `compose.rejeu.yaml`, aux Dockerfiles et à `make secrets`. Dans `coeur-bancaire`, ajouter la vérification du jeton, la table `batch_job_logs` (migration) et `GET /api/coeur-bancaire/traitements?limite=50`.

## Boundaries & Constraints

**Always:**
- Hexagone (docs/quality-gates.md) : `domain` et `application` en Java pur ; paquets `fr.nextbanking.openbankingconseiller.{domain,application,adapter.in.http,adapter.out.persistance,adapter.out.horloge si besoin,infrastructure}` ; tests `HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest` copiés de bourse. Service d'écriture de temps : uniquement via le port `Horloge`, aucun `now()` ailleurs.
- Tout `/api/**` exige un jeton valide (clé publique `nb.jeton.cle-publique`, refus de démarrer sans) ; aucune restriction de rôle : CLIENT, CONSEILLER, ADMIN identiques. Sans jeton : 401 `{"message":"Authentification requise"}`. `/actuator/health` public. Erreurs au seul format `{"message"}` (textes `Textes`/`GestionErreurs` de bourse).
- Cœur bancaire : mêmes règles pour `/api/**`, **sauf** `POST /api/coeur-bancaire/temoin` qui reste public (utilisé sans jeton par `scripts/gates/trace-smoke.sh`, interdit de modification) ; `/actuator/health` public ; `compose.yaml` fournit `NB_JETON_CLE_PUBLIQUE` à `coeur-bancaire` ; les tests existants du cœur bancaire restent verts.
- Catalogue : seuls les produits `active = true` ; trois groupes `epargne`, `credit`, `investissement`, chacun trié par nom, en ordre binaire (`collate "C"`, comme bourse) ; taux, rendements, dépôt min et LTV transmis en chaîne décimale exacte à 4 décimales pour les taux (« 0.0300 »), 2 pour les montants (« 300.00 ») ; champs JSON français camelCase : épargne `id, nom, description, taux, depotMin, capitalGaranti, dureeMaxMois` ; crédit `id, nom, description, taux, dureeMaxMois, ltvMax, assuranceRequise` ; investissement `id, nom, description, secteur, niveauRisque, rendementAttendu`. Valeur absente = `null`. `?type=EPARGNE|CREDIT|INVESTISSEMENT` : même enveloppe, les deux autres groupes vides ; type inconnu → 400 « Requête invalide ». Le libellé du risque et la mise en forme restent au front (story 7.6).
- Valeurs par défaut du domaine (FR-011-07) : épargne sans précision de garantie → `capitalGaranti = true` ; crédit sans précision d'assurance → `assuranceRequise = false`.
- Journal : `GET /api/coeur-bancaire/traitements?limite=N`, `limite` défaut 50 et plafond 50 (valeur ≤ 0 ou non numérique → 400 « Requête invalide ») ; lignes triées par début décroissant (début absent en dernier) ; champs `job`, `debut`, `fin` (ISO-8601 avec décalage `Europe/Paris`, `null` si absent), `dureeMs` (fin − début, 0 si l'un manque), `enregistrements`, `statut`, `details`. `statut` ∈ SUCCESS, FAILED, PARTIAL (contrainte `CHECK` en base, dette legacy).
- Table `batch_job_logs` (colonnes legacy : `id` identity, `job_name` non nul, `start_time`, `end_time`, `records_processed`, `status` avec `CHECK`, `details`) créée par migration Flyway `V2` du cœur bancaire ; la story 4.5 la « reprend si elle existe ». Cette story ne l'alimente pas (aucun traitement).
- Données de référence dans `services/open-banking-conseiller/reference-data/reference-data.sql`, jamais en migration : 13 produits (voir Design Notes), ids stables, `setval` des séquences.
- Chaque FR a un test tagué `@Tag("<FR>")` : FR-010-01, FR-011-02, FR-011-03, FR-011-07 dans `open-banking-conseiller` ; FR-011-09 dans `coeur-bancaire` (et FR-011-02 si le chargement du journal y est testé).
- Couverture JaCoCo : 100 % lignes et branches sur domain/application, 80 % sur adapter/infrastructure, dans les deux services.

**Never:**
- Pas de Kafka, de fournisseur, de coupe-circuit, de cache ni d'appel HTTP entre services dans `open-banking-conseiller` (stories 7.7+). Pas de front (story 7.6). Pas de filtrage par rôle.
- Aucune alimentation de `batch_job_logs` ni traitement planifié (story 4.5). Pas de lecture de la base d'un autre service.
- Ne pas modifier `scripts/gates/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `sprint-status.yaml`, ni les tests générés / le golden master ; ne pas rejouer le golden master.
- Pas de texte inventé : textes repris de `Textes` de bourse.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Catalogue | jeton CLIENT, CONSEILLER ou ADMIN ; 13 produits chargés | 200, même corps ; 4 épargne, 4 crédit, 5 investissement, triés par nom ; « CEL - Compte Epargne Logement » et « Livret A » en épargne ; `taux` « 0.0300 » | — |
| Filtre | `?type=EPARGNE` (puis CREDIT, INVESTISSEMENT) | 200, seul ce groupe rempli, les autres `[]` | `?type=XXX` → 400 « Requête invalide » |
| Produit désactivé | un produit `active=false` ajouté | absent de tous les groupes | — |
| Valeurs par défaut | création domaine d'un produit d'épargne / crédit sans précision | `capitalGaranti=true` / `assuranceRequise=false` | — |
| Journal > 50 | 60 lignes `batch_job_logs` | 200, 50 entrées, la plus récente en tête | `limite=0`, `limite=abc` → 400 |
| Journal vide | aucune ligne | 200, `[]` | — |
| Durée | début ou fin absent | `dureeMs` 0 | — |
| Sans jeton | aucun / invalide / expiré | 401 `{"message":"Authentification requise"}` sur les deux services | pas de pile, pas de cookie |
| Témoin | `POST /api/coeur-bancaire/temoin` sans jeton | 201 comme avant | — |

</intent-contract>

## Code Map

- `services/bourse/` -- modèle direct : `pom.xml` (sans resilience4j/Caffeine/fournisseur), `Dockerfile`, `src/main/resources/application.properties`, `adapter/in/http/{ConfigurationSecurite,AttributsErreur,GestionErreurs,Textes}.java`, `adapter/out/persistance/DepotWatchlistJdbc.java` (JdbcClient, `collate "C"`), `infrastructure/{ApplicationBourse,ControleMotDePasseBase,AssemblageWatchlist}.java`, `adapter/out/horloge/HorlogeSysteme.java` + `application/port/out/Horloge.java` (seulement si une classe en a besoin ; sinon garder `HorlogeArchitectureTest`).
- `services/bourse/src/test/java/fr/nextbanking/bourse/` -- `*ArchitectureTest`, `adapter/{BourseSecuriteIT,Jetons,HorlogeFixe}.java`, `infrastructure/{Conteneurs,ContexteDemarre,ApplicationBourseTest,ControleMotDePasseBaseTest}.java`, `adapter/in/http/{ConfigurationSecuriteTest,GestionErreursTest}.java`, `referencedata/ReferenceDataIT.java` -- à reprendre et adapter.
- `services/bourse/reference-data/reference-data.sql` -- forme du fichier (en-tête, inserts, `setval`).
- `services/coeur-bancaire/` -- à compléter : `pom.xml` (ajouter `spring-boot-starter-security-oauth2-resource-server` comme bourse), `adapter/in/PointEntreeTemoin.java` (seul contrôleur ; reste public), `src/main/resources/application.properties`, `db/migration/V1__temoins_et_outbox.sql` (ajouter `V2__batch_job_logs.sql`), `infrastructure/{ApplicationCoeurBancaire,AssemblageTemoin}.java`, tests `adapter/FluxTemoinTest`, `infrastructure/{Conteneurs,ContexteDemarre,ApplicationCoeurBancaireTest}` -- fournir la clé publique de test.
- `pom.xml` (racine) -- `<modules>` : ajouter `services/open-banking-conseiller`.
- `services/*/Dockerfile` (audit, bourse, coeur-bancaire, identite-clients, notifications) -- copient chaque POM du reactor : ajouter `services/open-banking-conseiller/pom.xml`.
- `compose.yaml` -- motif `postgres-bourse`/`bourse` (lignes ~86 et ~279) ; commentaire d'en-tête des ports (8088, `NB_PORT_OPEN_BANKING_CONSEILLER`) ; volume `pgdata-open-banking-conseiller` ; `web.depends_on` ; `NB_JETON_CLE_PUBLIQUE` pour `coeur-bancaire` (service ~ligne 190).
- `compose.rejeu.yaml` -- ajouter `open-banking-conseiller` avec `SPRING_PROFILES_ACTIVE: rejeu`.
- `Makefile` (`MOTS_DE_PASSE`), `.env.example` -- `NB_OPEN_BANKING_CONSEILLER_BD_MOT_DE_PASSE`.
- `frontend/vite.config.ts` (8088) et `frontend/nginx.conf` -- renvoi `/api/open-banking-conseiller/**` déjà en place, lecture seule : ne pas modifier, vérifier seulement.
- `docs/retro/features/FEAT-011/FIELDS.md`, `RG.md` ; capture `docs/retro/features/FEAT-011/screens/SCR-011-01_charge.png` -- source des 13 produits.
- `scripts/gates/check-gates-exist.sh` -- lecture seule : boucle sur `services/*/pom.xml`, exige les lignes `compose : service open-banking-conseiller`, etc.

## Tasks & Acceptance

**Execution:**
- `services/open-banking-conseiller/**` -- créer le module d'après bourse : `pom.xml`, `Dockerfile`, `application.properties` (nom, port 8080, base `postgres-open-banking-conseiller:5432/open_banking_conseiller`, utilisateur `open_banking_conseiller`, tracing OTLP 1.0), migration `V1` (tables `bank_products` + `savings_products`, `credit_products`, `investment_products` à jointure sur `id`, `numeric` pour montants/taux, `product_type` avec `CHECK`), domaine (`ProduitEpargne`, `ProduitCredit`, `ProduitInvestissement`, `TypeProduit`, `Catalogue`), port in `ConsulterCatalogue`, port out `DepotProduits`, cas d'usage, `DepotProduitsJdbc` (actifs seulement, tri `collate "C"`), `PointEntreeProduits`, `GestionErreurs`, `ConfigurationSecurite`, `AttributsErreur`, `Textes`, `ApplicationOpenBankingConseiller`, `ControleMotDePasseBase`, assemblage -- livrer le service.
- `services/open-banking-conseiller/reference-data/reference-data.sql` -- 13 produits actifs, ids fixes, `setval` -- données de référence hors migrations.
- `services/open-banking-conseiller/src/test/**` -- trois `*ArchitectureTest` ; tests unitaires du domaine (valeurs par défaut) et du cas d'usage ; `ProduitsIT` (trois rôles, filtre, désactivé, 401) sur base chargée du `reference-data.sql` ; `ReferenceDataIT` (13 produits : 4/4/5) ; tests sécurité, erreurs, démarrage ; tags FR sur chaque comportement -- couvrir la matrice.
- `services/coeur-bancaire/**` -- `pom.xml` (resource server), `ConfigurationSecurite` + `Textes`/`AttributsErreur`/`GestionErreurs` (adapter.in.http), migration `V2__batch_job_logs.sql`, domaine `ExecutionTraitement`/`StatutTraitement`, port in `ConsulterTraitements`, port out `DepotTraitements`, `DepotTraitementsJdbc`, `PointEntreeTraitements`, assemblage -- lecture du journal, jeton obligatoire sauf témoin et santé.
- `services/coeur-bancaire/src/test/**` -- `TraitementsIT` (50 sur 60, ordre, vide, `limite` invalide, trois rôles, 401), tests unitaires (durée 0), adaptation des tests existants à la clé publique ; tag `FR-011-09`.
- `pom.xml`, `services/*/Dockerfile`, `compose.yaml`, `compose.rejeu.yaml`, `Makefile`, `.env.example` -- intégration du nouveau service et de la clé publique du cœur bancaire.

**Acceptance Criteria:**
- Given la story terminée, when `ls services/`, then `open-banking-conseiller` y figure avec `pom.xml`, `Dockerfile`, `reference-data/reference-data.sql` et les paquets `fr/nextbanking/openbankingconseiller/{domain,application,adapter/in,adapter/out,infrastructure}`.
- Given `compose.yaml` et `compose.rejeu.yaml`, when `docker compose -f compose.yaml -f compose.rejeu.yaml config --format json`, then `open-banking-conseiller` (8088) et `postgres-open-banking-conseiller` (volume `pgdata-open-banking-conseiller`) existent, `open-banking-conseiller` a `SPRING_PROFILES_ACTIVE=rejeu`, et chaque service a un healthcheck ; `/api/open-banking-conseiller/**` est renvoyé par le proxy Vite et par `nginx.conf`.
- Given le service, when `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dtest='*ArchitectureTest'`, then code 0.
- Given les données de référence, when un CLIENT, CONSEILLER ou ADMIN appelle `GET /api/open-banking-conseiller/produits`, then 200, corps identique, seuls les actifs, 4/4/5, triés par nom, « 0.0300 » non mis en forme ; `?type=` filtre ; un produit désactivé n'apparaît nulle part.
- Given plus de 50 lignes dans `batch_job_logs`, when un utilisateur authentifié appelle `GET /api/coeur-bancaire/traitements?limite=50`, then 200, au plus 50 entrées, de la plus récente à la plus ancienne ; sans jeton, 401 `{"message"}` sur les deux services.
- Given la story terminée, when `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='<FR>'` pour FR-010-01, FR-011-02, FR-011-03, FR-011-07 et `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-011-09'`, then chaque commande sort en code 0 avec au moins un test exécuté.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh` (lignes du nouveau service à OK), `trace-smoke.sh` et `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 7 findings — high 0, medium 1, low 0, false 6, maybe-false 0
- findings:
  - `[false]` `[reject]` Intent : les tests exercent chaque service sur son port direct, pas le chemin proxy/nginx — `frontend/vite.config.ts` (8088) et `frontend/nginx.conf` (liste fermée avec `open-banking-conseiller`) portent déjà le renvoi, la spec les met en lecture seule ; la pile compose a répondu 200 sur les deux points.
  - `[medium]` `[defer]` Intent : le `reference-data.sql` n'est chargé par aucune commande outillée, catalogue vide sur une pile neuve — convention existante (bourse, identite-clients), `make reset-data` livré par le socle du rejeu ; reporté au frontmatter `deferred`.
  - `[false]` `[reject]` Intent : le journal reste vide faute de traitement qui l'alimente — voulu : la story 4.5 alimente `batch_job_logs`, la spec interdit de le faire ici ; les tests insèrent leurs lignes.
  - `[false]` `[reject]` Intent : plafond de `limite` à 50 et 400 sur valeur non positive — décision écrite de la spec (contrat de l'API), pas de lecture divergente de l'AC (« au plus 50 »).
  - `[false]` `[reject]` Intent : champ `dureeMs` ajouté — prévu par la spec et par la story 4.5 (durée en ms) ; l'ajout ne contredit aucun critère.
  - `[false]` `[reject]` Intent : sécurité du cœur bancaire plus large que le seul point du journal — exigé par la spec (tout `/api/**` authentifié, témoin public pour `trace-smoke.sh` non modifiable) ; `NB_JETON_CLE_PUBLIQUE` ajouté au compose ; trace-smoke code 0.
  - `[false]` `[reject]` Intent : aucune trace des gates dans le diff — lancés en code 0 : `build.sh`, `check-gates-exist.sh` (0 KO), `golden-master.sh`, `trace-smoke.sh` (6 OK).

## Design Notes

- **Écart au prérequis « epic 4 terminé »** : le cœur bancaire livré ne contient que le flux témoin. La story crée donc le strict nécessaire (jeton, table `batch_job_logs`, lecture) ; la story 4.5 reprend la table existante (son prérequis le prévoit) et durcit l'accès de ce point à ADMIN seulement si l'arbitrage FR-011-09 (ouvert à tous) change — non fait ici.
- **Modèle de données** : héritage joint du legacy (`BANK_PRODUCTS` : `id, product_type, active, created_at, currency, description, interest_rate, max_amount, min_amount, name` ; tables enfant : épargne `capital_guaranteed, max_duration_months, min_opening_deposit` ; crédit `max_duration_months, max_ltv, requires_insurance` ; investissement `expected_return, market_sector, risk_level`). `max_amount`/`min_amount`/`currency` sont chargés mais non exposés.
- **13 produits (capture SCR-011-01)** — épargne : CEL - Compte Epargne Logement (« Compte reglemente pour projet immobilier. Permet d'obtenir un pret CEL bonifie. », 0.0175, dépôt 300, durée max vide) ; Livret A (« Livret reglemente garanti par l'Etat. Disponibilite immediate, exonere d'impots. », 0.0300, 10, vide) ; Livret Demo Bank (« Livret d'epargne a taux bonifie. Sans frais, sans duree minimum. », 0.0250, 100, vide) ; PEL - Plan Epargne Logement (« Plan reglemente sur 4 a 10 ans. Taux garanti, prime de l'Etat sous conditions. », 0.0225, 225, 120 mois) ; tous capital garanti. Crédit : Credit Auto Classique (« Financement vehicule neuf ou occasion. Taux preferentiel concessionnaire. », 0.0449, 72, LTV vide, assurance non) ; Credit Immobilier Classique (« Financement acquisition ou travaux. Taux fixe, avec ou sans apport. », 0.0365, 300, 0.8000, oui) ; Credit Personnel (« Credit affecte ou non. Financement projet personnel, travaux, permis, etc. », 0.0598, 84, vide, non) ; Rachat de Credits (« Regroupement de credits en cours pour reduire les mensualites. », 0.0489, 180, 0.7000, oui). Investissement : ETF Monde Diversifie (« Fonds indiciel mondial (MSCI World). Exposition actions internationales. », Actions mondiales, 3, 0.0720) ; Fonds Euro Garanti (« OPCVM obligataire a capital garanti. Rendement stable et securise. », Obligations souveraines, 1, 0.0280) ; PEA Actions Europe (« Plan Epargne en Actions. Investissement actions europeennes, fiscalite avantageuse. », Actions Europe, 4, 0.0950) ; Private Equity Croissance (« Fonds de capital investissement PME. Horizon 8-10 ans, potentiel eleve. », Capital investissement, 5, 0.1500) ; SCPI Rendement Plus (« Societe civile de placement immobilier diversifiee. Revenus locatifs trimestriels. », Immobilier tertiaire, 2, 0.0510). Devise EUR.
- **Taux** : stockés en fraction décimale, jamais multipliés par 100 côté serveur (BUG-039 se corrige à l'affichage, story 7.6). LTV stockée de la même façon (0.8000 = 80 %).
- **Contrat de l'API** (noms, enveloppe en trois groupes, `dureeMs` en plus des champs de l'AC) : choix de cette story, repris par la story 7.6.
- **Chargement des données de référence** : comme bourse, aucun chargement outillé avant `make reset-data` (story « Socle du rejeu final ») ; chargement manuel par `psql` pour vérifier sur la pile.

## Verification

**Commands:**
- `mvn -B -q -f services/open-banking-conseiller/pom.xml verify` et `mvn -B -q -f services/coeur-bancaire/pom.xml verify` -- expected: code 0 (tests, JaCoCo)
- boucle `-Dgroups=<FR>` sur FR-010-01, FR-011-02, FR-011-03, FR-011-07 (open-banking-conseiller) et FR-011-09 (coeur-bancaire) -- expected: aucun KO
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0
- pile compose isolée : chargement du `reference-data.sql`, connexion, `curl` des deux points par le conteneur web, puis `scripts/gates/trace-smoke.sh` -- expected: statuts de la matrice, code 0

## Auto Run Result

Status: done

**Résumé :** nouveau service hexagonal `open-banking-conseiller` (Spring Boot, PostgreSQL dédiée, Flyway, serveur de ressources JWT, sans Kafka) avec les 4 tables du catalogue, les 13 produits de référence hors migrations et `GET /api/open-banking-conseiller/produits[?type=]` ouvert aux trois rôles. Le cœur bancaire reçoit la vérification du jeton (témoin et santé restent publics), la table `batch_job_logs` (migration V2) et `GET /api/coeur-bancaire/traitements?limite=` (50 au plus, du plus récent au plus ancien).

**Fichiers :**
- `services/open-banking-conseiller/**` — module complet et ses tests (dont les trois `*ArchitectureTest`).
- `services/coeur-bancaire/**` — sécurité JWT, migration V2, lecture du journal, tests.
- `pom.xml`, `services/*/Dockerfile` — module au reactor et copie du POM dans les images.
- `compose.yaml`, `compose.rejeu.yaml`, `Makefile`, `.env.example` — base, volume et service (8088), profil `rejeu`, clé publique du cœur bancaire, mot de passe de base.

**Revue :** 7 constats ; 0 correctif ; 1 report (chargement outillé des données de référence) ; 6 rejets motivés dans le journal de triage. Correctifs par verdict : high 0, medium 0, low 0. Revue complémentaire recommandée : non.

**Vérification :**
- `scripts/gates/build.sh`, `check-gates-exist.sh` (0 KO), `golden-master.sh` : code 0 ; `trace-smoke.sh` : code 0 (6 OK) sur la pile reconstruite.
- `mvn verify` vert pour `open-banking-conseiller` (43 tests) et `coeur-bancaire` (67 tests), JaCoCo compris ; `*ArchitectureTest` vert ; `-Dgroups` vert pour FR-010-01 (5 tests), FR-011-02 (9), FR-011-03 (4), FR-011-07 (5) et FR-011-09 (17).
- `docker compose config` : `open-banking-conseiller` sur 8088 avec `rejeu` ; aucun `mode=reel` committé.
- Pile : catalogue, filtre, 400 sur type inconnu, journal 200 avec jeton et 401 sans, témoin 201 sans jeton.

**Risques résiduels :**
- Le prérequis « epic 4 terminé » n'était pas rempli : la table `batch_job_logs` est créée ici, et la story 4.5 doit la reprendre au lieu de la recréer.
- `?type=` vide vaut absent (trois groupes) et la casse est stricte (`epargne` donne 400) : non tranché par la spec.
- Le journal reste vide tant que la story 4.5 ne l'alimente pas.
