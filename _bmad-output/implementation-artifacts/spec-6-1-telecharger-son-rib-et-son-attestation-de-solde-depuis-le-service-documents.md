---
title: 'Télécharger son RIB et son attestation de solde depuis le service documents'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: 'c19db16fe9d07c1428dbe9d3bebf406d47e29474'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/AGENTS.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-6-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problème :** il n'existe aucun service `documents` : un utilisateur ne peut télécharger ni son RIB ni son attestation de solde, et `coeur-bancaire` ne publie aucun événement sur les comptes modifiés.

**Approche :** créer `services/documents/` (hexagonal, base `postgres-documents`, projections locales des comptes et des clients alimentées par Kafka, seul producteur de PDF) servant `GET /api/documents/telechargement?type=RIB|ATTESTATION&compteId=` ; faire produire à `coeur-bancaire` `coeur-bancaire.compte-modifie.v1` via l'outbox. Le titulaire seul télécharge ses documents, quel que soit son rôle.

## Boundaries & Constraints

**Always :**
- Spécification de référence : Story 6.1 de `_bmad-output/planning-artifacts/epics.md` (l. 4059-4170, tous les critères d'acceptation font foi) ; textes PDF dans `docs/retro/features/FEAT-017/MESSAGES.md` (sections RIB, ATTESTATION, pied de page commun), règles dans `FEAT-017/RG.md` et `FEAT-008/RG.md`/`MESSAGES.md`. Libellés de mois en français avec accents (« Janvier », « Février »…), le reste des textes PDF à l'identique du legacy (graphie sans accent comme listée dans l'epic).
- Bibliothèque PDF : choisie et vérifiée **avant** le premier PDF ; ajouter une ligne dans `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/versions-verifiees.md` (colonnes : composant, version retenue, URL du registre officiel consulté, date de vérification 2026-10-09, remarque : compatibilité Java 21 et Spring Boot 4.1.1, licence). Candidate : OpenPDF (`com.github.librepdf:openpdf`, la 1.3.30 du legacy est dans `~/.m2` ; réseau Maven disponible pour vérifier `maven-metadata.xml`). Utilisée seulement dans `adapter.out` (le test ArchUnit interdit tout framework dans `domain`/`application`). `org.apache.pdfbox:pdfbox` 3.0.5 (déjà dans `~/.m2`) en scope **test** pour extraire le texte des PDF.
- Squelette de `documents` copié de `services/credits` (pom, Dockerfile, `application.properties`, `Horloge`/`HorlogeSysteme`, `ConfigurationSecurite` + `JwtDecoder`, `JetonRoles`, `GestionErreurs`/`Textes`, `RecepteurClientModifie` + cas d'usage + `ProjectionClientsJdbc` + `EvenementsTraitesJdbc` + `TransactionSpring`, `Conteneurs`, `OutilsHttp`, `HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest`, `ControleMotDePasseBase`). Paquets `fr.nextbanking.documents.{domain,application,adapter.in,adapter.out,infrastructure}`. Déclaration : `<module>services/documents</module>` dans `pom.xml` racine ; `compose.yaml` (`postgres-documents` volume `pgdata-documents`, service `documents` port `${NB_PORT_DOCUMENTS:-8084}:8080`, `healthcheck`, `depends_on`, ajout dans le `depends_on` de `web` et dans l'en-tête des ports) ; `Makefile` + `.env.example` (`NB_DOCUMENTS_BD_MOT_DE_PASSE`, secrets uniquement par variables d'environnement) ; **un `COPY services/documents/pom.xml` dans les 7 Dockerfiles existants** (le reactor Maven casserait sinon). Proxy nginx et Vite déjà prêts pour `/api/documents/**` (ne rien changer). Mêmes seuils JaCoCo (100 % domain/application, 80 % adapter/infrastructure).
- Route `GET /api/documents/telechargement`, jeton obligatoire (401 `{"message":"Authentification requise"}`). `type` et `compteId` reçus en `String`, validation **dans l'ordre** : `type` absent → 400 « Parametre type manquant » ; type hors RIB/ATTESTATION/RELEVE/IFU (insensible à la casse) → 400 « Type inconnu : {type tel que reçu} » ; `compteId` absent, vide ou non numérique → 400 « accountId manquant » ; compte absent de la projection → 404 « Compte introuvable » ; compte d'un autre client (`clientId` ≠ `sub`) → 403 `{"message":"Accès refusé"}` (aucun contrôle de rôle : CLIENT, CONSEILLER, ADMIN ne téléchargent que leurs comptes) ; sinon PDF. `RELEVE` et `IFU` passent la validation du type mais leur génération est livrée par 6.2 : renvoyer une réponse sans PDF ni effet de bord (400 « Type inconnu : … » interdit pour eux ; utiliser l'erreur générique 500 de FR-017-10 via un port de génération non implémenté pour ces types). Toute exception inattendue → 500 `{"message":"Erreur lors de la generation du document"}` seul, sans trace ni nom de composant.
- Réponse 200 : `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="rib_{compteId}.pdf"` (resp. `attestation_solde_{compteId}.pdf`), `Cache-Control: no-cache, no-store, must-revalidate`. Contenu RIB et ATTESTATION exactement comme les critères de l'epic ; nom complet du titulaire depuis la projection des clients ; solde de la projection des comptes (de ce seul compte, `"{montant} EUR"`, 2 décimales) ; dates par le port `Horloge` (Europe/Paris, jamais `now()` hors `adapter.out.horloge`) ; « Numero de compte » = `numero` sans préfixe `FR76-` ni tirets, IBAN = `numero`, BIC `DEMOFR21XXX`, code banque `30004`, code guichet `00001`, clé RIB `42` (valeurs du legacy `DocumentService`).
- Compteur de téléchargements : table `documents` (colonnes du legacy : `id, customer_id, document_type, filename, mime_type, file_size_bytes, generated_at, download_count, checksum, parametres`) ; chaque téléchargement réussi incrémente `download_count` de la ligne (client, type, paramètres) — créée à la première génération —, une demande refusée (400/403/404/500) n'incrémente rien ; deuxième téléchargement identique → 2. Les 5 lignes `DOCUMENTS` de `import.sql` du legacy (`/home/sebastienrozange/workspace/acn-fs/AppModernizationDemonstrateur/legacy/src/main/resources/import.sql` l. 405-415) sont reprises dans `services/documents/reference-data/reference-data.sql` (dates en `timestamptz`).
- Projections `comptes` (`compte_id`, `client_id`, `numero`, `type`, `iban`, `bic`, `solde`, `version_agregat`) et `clients` (comme `credits`), `evenements_traites` ; consommateurs `@KafkaListener` (groupe `documents`) sur `coeur-bancaire.compte-modifie.v1` et `identite-clients.client-modifie.v1` : mise à jour seulement si `versionAgregat` strictement supérieur (insertion si inconnu), dans la même transaction que `evenements_traites` ; événement de type/version inattendu ignoré avec avertissement. Données de référence des projections **générées** par `scripts/generer-projections-reference.sh` (l'étendre : projections clients et comptes de `documents` depuis les `reference-data.sql` d'`identite-clients` et de `coeur-bancaire`), jamais écrites à la main.
- `coeur-bancaire` : migration ajoutant `comptes.version_agregat bigint not null default 1` ; `ComptesJdbc.debiter`/`crediter` incrémentent `version_agregat` ; nouvel événement `compte-modifie.v1` (clé `compteId`, enveloppe sur le modèle d'`EnveloppeVirementDecide`, `donnees` : `compteId, clientId, numero, type, iban (=numero), bic ("DEMOFR21XXX"), solde (chaîne à 2 décimales), versionAgregat`) consigné dans `outbox` **dans la même transaction** que la modification, pour chaque compte modifié : `ExecuterVirementCasUsage` (deux événements, source et destination), `TraiterVirementATraiterCasUsage` (le résultat `Traitement` doit porter les lignes d'outbox pour publication après commit), `ExecuterEcheancesCasUsage`. Aucune création de compte ni calcul d'intérêts n'existe dans le code actuel : ces clauses n'ont aucun chemin à instrumenter (le noter dans le compte rendu, ne rien inventer). `NewTopic` dans `ConfigurationEvenements`, `contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json` avec exemple (modèle : `coeur-bancaire.virement-decide.v1.schema.json`), `CompteModifieContratEvenementProducteurTest` (champs exacts, comparaison à `examples[0]`), un test par chemin d'écriture. `coeur-bancaire` reste seul à modifier un solde (aucun `UPDATE` de solde dans `services/documents`).
- Tests : `DocumentsSecuriteIT` (401 sans jeton/expiré, 403 compte d'autrui, 404 inexistant, CLIENT/CONSEILLER/ADMIN sur leurs propres comptes) ; test `@Tag("FR-017-01")` reprenant `docs/retro/golden-master/derived/FEAT-017/SC-017-01/input-request.json` et le statut de `expected-response.json` (401, aucun document généré ni compté) ; tests de validation 400 (`FR-017-02`, `FR-017-03`, `FR-017-11`), 404 (`FR-017-09`), 403 (`FR-017-08`, `FR-008-04`, `BUG-058`, `BUG-027`), RIB (`FR-017-05`, `FR-008-08`), attestation (`FR-017-06`, `FR-008-09`), 500 (`FR-017-10`, `BUG-059`, adaptateur PDF en échec), compteur (`FR-008-12`), type invalide (`FR-008-11`), sans session (`FR-008-02`), rôles (`FR-030-06`) ; contrats consommateur (exemples des schémas), double livraison Testcontainers = un seul effet par consommateur, `ReferenceDataIT`. Chaque tag cité exécuté au moins une fois. `@Tag` au format `FR-017-01` etc.
- Code métier en français, JSON en français camelCase, montants `BigDecimal` 2 décimales / `numeric(19,2)`, IBAN masqué dans les journaux (`FR76****1234`), aucun jeton ni mot de passe journalisé, services jamais appelés en HTTP.

**Never :**
- Aucune modification de `scripts/gates/**`, `.bmad-loop/**`, `.claude/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, seuils, exclusions de couverture ou de mutation ; rien sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; jamais de rejeu du golden master ; ne pas écrire `sprint-status.yaml`.
- Pas de relevé mensuel ni d'IFU (story 6.2), pas de projection des opérations ni de `operation-enregistree.v1`, pas d'écran Documents ni de modification du front (6.3), pas de relance de `retro-forward-bmad`, pas de texte inventé en dehors du corpus, pas de lecture de la clé NVD de `~/.m2/settings.xml`, pas de nouvelle dépendance autre que la bibliothèque PDF (+ pdfbox en test), pas de `mode=reel`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Gestion d'erreur |
|----------|---------------|----------------------|------------------|
| RIB | `user1`, `type=RIB&compteId=1` (casse indifférente) | 200 PDF `rib_1.pdf`, en-têtes, textes de l'epic, compteur +1 | — |
| Attestation | `type=ATTESTATION&compteId=1` | 200 PDF `attestation_solde_1.pdf`, solde de la projection, date Horloge | — |
| Sans jeton | aucun jeton | 401, rien généré ni compté | — |
| `type` absent / inconnu | pas de `type` / `FACTURE` | 400 « Parametre type manquant » / « Type inconnu : FACTURE » | sans PDF |
| `compteId` invalide | absent, vide, `abc` | 400 « accountId manquant » (×3) | sans PDF |
| Compte inexistant | `compteId=999999` | 404 « Compte introuvable » | sans PDF |
| Compte d'autrui | compte de `user2`, `user1` (tout rôle) | 403 « Accès refusé », compteur inchangé, contrôles 400 faits avant | sans PDF |
| Panne de génération | adaptateur PDF en échec | 500 « Erreur lors de la generation du document » seul | sans détail |
| Deuxième téléchargement | même client/type/paramètres | compteur = 2 | refus : pas d'incrément |
| Événement compte | version supérieure / égale ou inférieure / double livraison | projection mise à jour / ignorée / un seul effet | — |
| Producteur | virement exécuté | deux `compte-modifie` (source, destination), `versionAgregat` croissant | rollback : aucun événement |

</intent-contract>

## Code Map

- `services/credits/**` -- modèle complet du squelette (pom, Dockerfile, `application.properties`, sécurité JWT, `GestionErreurs`/`Textes`, récepteur `RecepteurClientModifie`, `ProjectionClientsJdbc`, `EvenementsTraitesJdbc`, `Conteneurs`, `OutilsHttp`, trois `*ArchitectureTest`, `ClientModifieContratEvenementConsommateurTest`, `ClientModifieKafkaIT`, `CreditsSecuriteIT`, `ReferenceDataIT`), migrations V3 (`evenements_traites`) et V4 (`clients`).
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/adapter/out/persistance/ComptesJdbc.java` -- `debiter` (l. ~65) et `crediter` (l. ~73), seuls `UPDATE comptes` ; table `comptes(id, numero, type, solde, client_id)`, migration `V2__comptes_operations_virements.sql` ; pas de `iban`/`bic`/version.
- `.../application/{ExecuterVirementCasUsage (ecrire l. ~151), TraiterVirementATraiterCasUsage (l. ~88-94, `Traitement` sans ligne d'outbox), ExecuterEcheancesCasUsage (ecrire l. ~186)}` -- appelants à instrumenter ; `LiberationVirementCasUsage` ne touche pas aux soldes.
- `.../adapter/out/evenements/{EvenementsSortantsKafka,RelaisOutbox,EnveloppeVirementDecide,LigneOutbox}`, `.../application/port/out/EvenementsSortants`, `.../infrastructure/ConfigurationEvenements` (`NewTopic`), test modèle `VirementDecideContratEvenementTest`, `adapter/DonneesDeReference`, `Jetons` -- modèle outbox/topic/schéma.
- `contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json`, `identite-clients.client-modifie.v1.schema.json` -- modèles de schémas (AD-22 : `compte-modifie.v1` clé `compteId`).
- `pom.xml` (modules l. 19-27), `compose.yaml` (modèle `postgres-credits`/`credits`, en-tête des ports, `web.depends_on` ~l. 425), `Makefile` (`MOTS_DE_PASSE`), `.env.example`, `services/*/Dockerfile` (7 `COPY` de pom à compléter), `scripts/generer-projections-reference.sh` (ne produit que `credits/reference-data/projection-clients.sql`), `scripts/gates/{build,check-gates-exist}.sh` (lecture seule : exigent un `*ArchitectureTest`, ArchUnit, JaCoCo, nom du service dans `docker compose config --services`).
- `frontend/nginx.conf`, `frontend/vite.config.ts` (port 8084), `replay-mapping.json` (SC-017-01) -- déjà prêts, lecture seule.
- Corpus : `docs/retro/features/FEAT-017/{RG,MESSAGES,VALIDATION}.md`, `FEAT-008/{RG,MESSAGES}.md`, `docs/retro/golden-master/derived/FEAT-017/SC-017-01/`, `docs/retro/00-global/LEGACY-BUGS.md` (BUG-027/058/059) ; legacy hors dépôt : `/home/sebastienrozange/workspace/acn-fs/AppModernizationDemonstrateur/legacy/src/main/java/com/accenture/bank/ejb/DocumentService.java` (RIB ~l. 525-560, OpenPDF 1.3.30), `.../web/DocumentDownloadServlet.java`, `import.sql`.
- `ARCHITECTURE-SPINE.md` (AD-14, AD-16, AD-22, AR-101), `versions-verifiees.md` -- format de ligne à imiter. AD-16 : la ligne d'exception « documents d'un compte : titulaire seul » existe peut-être déjà ; ne pas modifier la spine sauf coquille manifeste.

## Tasks & Acceptance

**Execution :**
- `_bmad-output/planning-artifacts/architecture/.../versions-verifiees.md` -- ligne de la bibliothèque PDF (vérifiée sur le registre) -- AR-101, avant tout PDF.
- `services/coeur-bancaire/**`, `contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json` -- migration `version_agregat`, événement `compte-modifie.v1` par outbox sur les chemins d'écriture, `NewTopic`, schéma + tests producteur et par chemin d'écriture -- producteur côté cœur bancaire.
- `services/documents/**`, `pom.xml`, `compose.yaml`, `Makefile`, `.env.example`, `services/*/Dockerfile`, `scripts/generer-projections-reference.sh` -- nouveau service : domaine/application purs (cas d'usage de téléchargement, ports `Comptes`, `Clients`, `Documents`, `GenerateurPdf`, `Horloge`), adaptateurs HTTP/Kafka/JDBC/PDF, migrations, `reference-data`, assemblage, `application.properties` -- FR-017-*, FR-008-*, FR-030-06.
- `services/documents/src/test/**` -- tests unitaires (matrice), `DocumentsSecuriteIT`, ITs HTTP avec extraction PDF (pdfbox test), contrats consommateur, double livraison, `ReferenceDataIT`, tests d'architecture -- tags de la story.

**Acceptance Criteria :**
- Voir les 13 critères « Given/When/Then » de la Story 6.1 dans `epics.md` (l. 4075-4160) : bibliothèque PDF vérifiée, module `services/documents/` complet, `compte-modifie.v1` produit, projections idempotentes, 401 sans jeton, validations 400/404/403, RIB et attestation, 500 sans détail, compteur.
- Given les commandes du critère « tests de la story » (`mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-017-01|…|BUG-059'`, `mvn -B -q -f services/documents/pom.xml verify`, `mvn -B -q -f services/coeur-bancaire/pom.xml verify`), then code 0 et chaque tag exécuté.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` (et `trace-smoke.sh` si présent et utilisable sans rejeu), then verts (code 0).

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 3 findings — high 0, medium 0, low 0, false 3, maybe-false 0
- findings:
  - `[false]` `[reject]` Aucun test ne passe par nginx, le proxy Vite ou la pile compose complète — `frontend/nginx.conf` et `vite.config.ts` routent déjà `/api/documents/**` (port 8084) ; l'intention fixe le test d'intégration du service `documents` comme preuve de SC-017-01.
  - `[false]` `[reject]` Chemins « création de compte » et « intérêts » non instrumentés — ces chemins n'existent pas dans `coeur-bancaire` (comptes uniquement issus de `reference-data.sql`, aucun calcul d'intérêts) ; rien à instrumenter, consigné dans le compte rendu.
  - `[false]` `[reject]` Événement branché par cas d'usage et non en un point central — les trois cas d'usage qui modifient un solde sont couverts chacun par un test (`VirementIT`, `EcheancesIT`, `TraiterVirementATraiterCasUsageTest`) ; aucun défaut démontré.

## Design Notes

`RELEVE` et `IFU` : le type est reconnu (pas de 400 « Type inconnu ») mais le générateur n'est livré qu'en 6.2 ; en 6.1 la demande aboutit à l'erreur générique 500 (« Erreur lors de la generation du document ») sans incrément du compteur. Le contrôle d'appartenance du compte précède toute génération ; l'ordre des refus est : jeton, `type` manquant, `type` inconnu, `compteId`, compte introuvable, appartenance.

## Verification

**Commands:**
- `mvn -B -q -f services/documents/pom.xml verify` -- expected: code 0
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify` -- expected: code 0
- `mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-017-01|FR-017-02|FR-017-03|FR-017-05|FR-017-06|FR-017-08|FR-017-09|FR-017-10|FR-017-11|FR-008-02|FR-008-04|FR-008-08|FR-008-09|FR-008-11|FR-008-12|FR-030-06|BUG-027|BUG-058|BUG-059'` -- expected: code 0, chaque tag exécuté
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0 (les `KO` de contrôles de stories non livrées sont tolérés par `check-gates-exist.sh`, ceux de cette story doivent être `OK`)

## Auto Run Result

Status: done

**Résumé :** création du service `services/documents/` (hexagonal, base `postgres-documents`, OpenPDF 2.4.0 dans `adapter.out.pdf`, projections comptes/clients idempotentes, `GET /api/documents/telechargement` pour RIB et attestation, 401/400/403/404/500, compteur de téléchargements) ; `coeur-bancaire` publie `coeur-bancaire.compte-modifie.v1` par l'outbox (migration V10 `version_agregat`).

**Fichiers :** `services/documents/**` (service et 99 tests), `services/coeur-bancaire/**` (événement, migration V10, instrumentation de 3 cas d'usage), `contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json`, `pom.xml`, `compose.yaml`, `Makefile`, `.env.example`, 7 `Dockerfile` (COPY du pom), `scripts/generer-projections-reference.sh`, `versions-verifiees.md` (OpenPDF, PDFBox test).

**Revue :** 0 patch, 0 report, 3 constats rejetés (voir journal de triage). Relecture de suivi recommandée : false.

**Vérification :** `check-gates-exist.sh` (tous OK pour `documents`), `golden-master.sh` code 0, `mvn -f services/documents/pom.xml verify` code 0 ; l'implémenteur a rapporté `build.sh`, `trace-smoke.sh` et `verify` de `coeur-bancaire` en code 0.

**Risques résiduels :** mois imprimés avec majuscule et accents (« Octobre ») selon la spec, le legacy sortait « octobre » ; montants au format `2130.00 EUR` ; création de compte et intérêts inexistants dans `coeur-bancaire` ; `.env` local sans `NB_DOCUMENTS_BD_MOT_DE_PASSE` (lancer `make secrets`) ; RELEVE et IFU aboutissent à 500 jusqu'à la story 6.2.
