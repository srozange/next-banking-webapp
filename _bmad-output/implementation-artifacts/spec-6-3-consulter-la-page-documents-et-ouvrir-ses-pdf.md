---
title: 'Consulter la page Documents et ouvrir ses PDF'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: 'ac53045758de1ab2a559c3d8cd9ef004dfde17fd'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/AGENTS.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-6-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-6-2-telecharger-ses-releves-mensuels-et-ses-ifu.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problème :** la route `/documents` n'a aucun écran (entrée de navigation seule) et `documents` n'expose ni la liste des comptes du connecté ni la date `aujourdhui` ; l'utilisateur ne peut pas ouvrir ses PDF (RIB, attestation, relevés, IFU) en un clic.

**Approche :** ajouter à `documents` un point d'API de lecture (comptes du jeton, depuis la projection locale, plus `aujourdhui` de l'`Horloge`), puis un écran React `/documents` (cartes RIB/attestation, 12 relevés mensuels, 2 IFU) dont les liens appellent `GET /api/documents/telechargement` avec le jeton et ouvrent le PDF reçu dans un nouvel onglet.

## Boundaries & Constraints

**Always :**
- Spécification de référence : Story 6.3 de `_bmad-output/planning-artifacts/epics.md` (l. 4192-4250, tous les critères font foi) ; textes : `docs/retro/features/FEAT-008/MESSAGES.md` (MSG-008-001, 040-046, 050-060) ; règles `FEAT-008/RG.md` (RG-008-003, -005, -006) ; scénarios `docs/retro/golden-master/derived/FEAT-008/SC-008-01` et `FEAT-017/SC-017-02` ; textes nouveaux : `a11y.nouvel.onglet` (« (nouvel onglet) ») de `ux-designs/…/textes-nouveaux.md`. Textes à l'identique du legacy (casse, absence d'accents : « Telecharger PDF », « Releve d Identite Bancaire », « Releves mensuels — {numero} », « Imprime Fiscal Unique (IFU) »), mois avec accents, aucun texte inventé.
- Point d'API ajouté à `documents` (le corpus n'en nomme aucun, point ouvert à consigner) : `GET /api/documents/comptes`, tout rôle authentifié, réponse `200 {"aujourdhui":"yyyy-MM-dd","comptes":[{"compteId":1,"numero":"…","type":"COURANT"}]}` ; comptes dont `clientId = sub` du jeton dans la projection locale (jamais d'identifiant fourni par le front, aucun contrôle de rôle : CONSEILLER et ADMIN ne voient que leurs comptes), triés par `compteId` ; `aujourdhui` = `Horloge.aujourdhui()` (Europe/Paris). Sans jeton : 401 `{"message":"Authentification requise"}` (déjà la sécurité du service). Hexagone : port d'entrée, cas d'usage `application` pur Java, `ProjectionComptes` étendu (comptes d'un client), adaptateur HTTP en `adapter.in.http`, ArchUnit et JaCoCo inchangés (100 % `domain`/`application`, 80 % `adapter`/`infrastructure`).
- Front hexagonal : `domain` (mois en français, 12 mois récents depuis `aujourdhui` du plus récent au plus ancien — `aujourdhui` inclus —, années IFU `année(aujourdhui)-1` et `-2`, paramètres de téléchargement, noms de test-id) et `application` (port `ClientDocuments`, cas d'usage charger / télécharger) en TypeScript pur, 100 % couverts, sans `Date.now()` / `new Date()` (l'`aujourdhui` est parsé en entiers) ; adaptateur `adapters/http/ClientDocumentsHttp.ts` (`fetchAuthentifie` : tout 401 supprime `nb.jeton` et ramène à `/login`, sans ouvrir d'onglet), écran `adapters/ecrans/documents/`. Le PDF reçu (blob) s'ouvre par `window.open` sur une URL d'objet, substituable en test (propriété `ouvrirPdf`) ; toute autre réponse que 200 (403, 404, 5xx, coupure) → texte technique `TEXTES.erreurTechnique`, aucun onglet.
- Écran : titre d'onglet « BankApp Demo - Documents » (`TEXTES.titresOnglet.documents`), même contenu pour CLIENT, CONSEILLER, ADMIN ; par compte une section (séparateur `eyebrow`) « Compte {type} - {numero} », carte RIB (étiquette `tag` « Bancaire », titre « RIB », sous-titre « Releve d Identite Bancaire », lien « Telecharger PDF »), carte attestation (« Attestation » / « Attestation de solde » / « Solde au jour de la demande » / lien), liste « Releves mensuels — {numero} » de liens « {Mois} {annee} » ; section « Documents fiscaux » (`eyebrow`), titre « Imprime Fiscal Unique (IFU) », cartes « IFU {annee} » (« Fiscal », « Revenus de capitaux mobiliers », lien). Chaque lien : `data-testid` `documents-rib-{compteId}`, `documents-attestation-{compteId}`, `documents-releve-{compteId}-{aaaa}-{mm}`, `documents-ifu-{annee}`, nom accessible « Telecharger PDF » (ou « {Mois} {annee} ») suivi d'un texte « (nouvel onglet) » réservé aux lecteurs d'écran. Le DOM garde l'ordre legacy (SC-008-01 : Documents fiscaux, IFU, RIB, Attestation, Releves mensuels, Telecharger PDF ; comparaison `contains`).
- Demande de téléchargement : `GET /api/documents/telechargement` avec `Authorization: Bearer` (porté par `fetchAuthentifie`) et `type` (`RIB`, `ATTESTATION`, `RELEVE`, `IFU`), `compteId` (RIB, ATTESTATION, RELEVE), `annee` et `mois` (RELEVE), `annee` (IFU) ; aucun `clientId` envoyé.
- Route : `/documents` ajoutée au routeur sous `RouteProtegee` (sans jeton → `/login` sans afficher l'écran ; test Vitest sur `/documents`, `/mon-conseiller`, `/espace-conseiller`, `/administration`) ; assemblage dans `infrastructure/main.tsx`, `adapters/test/rendreApplication.tsx`, `routes.test.tsx`. Rien dans `localStorage`/`sessionStorage` hors le jeton. Aucun couleur, ombre ni police hors tokens ; `data-testid` au format `{écran}-{élément}[-{id}]`.
- Tests étiquetés par exigence (`[FR-008-01]`, `[FR-008-02]`, `[FR-008-03]`, `[FR-008-05]`, `[FR-008-06]`, `[FR-030-06]` dans le titre des `it`/`describe` du front ; `@Tag("FR-008-03")`, `@Tag("FR-008-05")`, `@Tag("FR-008-06")` côté `documents`), chaque tag porté par au moins un test exécuté.

**Never :**
- Aucune modification de `scripts/gates/**`, `.bmad-loop/**`, `.claude/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, seuils, exclusions de couverture ou de mutation ; rien sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; jamais de rejeu du golden master ; ne pas écrire `sprint-status.yaml`.
- Pas de modification de la génération des PDF ni de `GET /api/documents/telechargement` (6.1/6.2), aucune nouvelle dépendance (back et front), pas de `vitest.config.*`, pas d'appel HTTP entre services, pas de lecture de l'horloge du navigateur, pas de texte inventé, pas de `mode=reel`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Gestion d'erreur |
|----------|---------------|----------------------|------------------|
| Page nominale | `user1`, 1 compte COURANT, `aujourdhui=2026-11-17` | section « Compte COURANT - … », RIB, attestation, 12 relevés « Novembre 2026 » … « Décembre 2025 », IFU 2025 et 2024 | — |
| Passage d'année | `aujourdhui=2026-02-05` | relevés « Février 2026 » … « Mars 2025 » (aaaa-mm corrects) | — |
| Trois rôles | `user1`, `sophie`, `admin` | même écran, comptes du seul connecté | — |
| Sans jeton | `/documents`, `/mon-conseiller`, `/espace-conseiller`, `/administration` | redirection `/login` | écran non affiché |
| Sans compte | aucun compte projeté | pas de section compte, section IFU présente | — |
| Clic RIB | lien `documents-rib-1` | GET `type=RIB&compteId=1` Bearer, PDF ouvert dans un nouvel onglet | — |
| Clic relevé | `documents-releve-1-2026-03` | GET `type=RELEVE&compteId=1&annee=2026&mois=3` | — |
| Clic IFU | `documents-ifu-2025` | GET `type=IFU&annee=2025` | — |
| 401 au clic | jeton expiré | `nb.jeton` supprimé, `/login`, aucun `window.open` | — |
| 403/404/5xx/coupure | refus du service | texte technique sur l'écran, aucun onglet | pas de détail technique |
| Page indisponible | `GET /api/documents/comptes` en échec | texte technique, pas de section | — |

</intent-contract>

## Code Map

- `services/documents/src/main/java/fr/nextbanking/documents/adapter/in/http/PointEntreeTelechargement.java` -- modèle du contrôleur (`@AuthenticationPrincipal Jwt`, `clientId(jeton)` → `AuthentificationRequise`) ; nouveau `PointEntreeComptes` (même paquet) pour `GET /api/documents/comptes`.
- `.../application/port/in/TelechargerDocument.java`, `.../application/TelechargerDocumentCasUsage.java` -- modèles de port d'entrée et de cas d'usage ; créer `ConsulterPageDocuments` (port `in`) + `ConsulterPageDocumentsCasUsage` (`ProjectionComptes`, `Horloge`), enregistré dans `.../infrastructure/AssemblageDocuments.java`.
- `.../application/port/out/ProjectionComptes.java` + `.../adapter/out/persistance/ProjectionComptesJdbc.java` -- ajouter `parClient(long clientId)` (`select … from comptes where client_id = :id order by compte_id`) ; `.../domain/CompteProjete.java` (`numero`, `type`).
- `.../adapter/in/http/{ConfigurationSecurite,GestionErreurs,Textes}.java` -- 401/403 JSON déjà en place, rien à changer.
- Tests modèles : `services/documents/src/test/java/fr/nextbanking/documents/adapter/{TelechargementHttpIT,DocumentsSecuriteIT,ConfigurationHorlogeFixe}.java`, `application/TelechargerDocumentCasUsageTest.java`, doubles de `ProjectionComptes` à compléter.
- `frontend/src/adapters/routes/routes.tsx` (+ `routes.test.tsx`, `RouteProtegee`) -- ajouter `/documents` (`Dependances.clientDocuments`) ; `frontend/src/infrastructure/main.tsx` -- assemblage ; `frontend/src/adapters/test/rendreApplication.tsx` -- client simulé (nouveau paramètre en fin de liste, défaut : panne technique).
- `frontend/src/adapters/textes.ts` (`titresOnglet.documents`, nouveau bloc `documents`, `a11y.nouvelOnglet`), `frontend/src/domain/navigation.ts` (entrée `documents` → `/documents` déjà présente).
- Modèles front : `application/consulterReleve.ts` + `domain/releve.ts`, `adapters/http/ClientReleveHttp.ts` (+ test), `adapters/ecrans/releve/EcranReleve.{tsx,css,test.tsx}`, `adapters/ui/{Carte,Etiquette}.tsx`, `adapters/session/fetchAuthentifie.ts` (401), `src/architecture-temps.test.ts`, `couleurs-hors-tokens.test.ts`, `motifs-interdits.test.ts`.
- Vérification de scénario : `scripts/golden-master/verifier-scenario.sh SC-008-01` et `SC-017-02` (lecture seule ; route `'/documents'` littérale dans `routes.tsx`, textes cherchés dans `frontend/src`).
- Lecture seule : `scripts/gates/**`, `docs/retro/golden-master/**`, `replay-mapping.json`, `services/documents/.../adapter/out/pdf/**`.

## Tasks & Acceptance

**Execution :**
- `services/documents/src/main/**` -- port `ConsulterPageDocuments`, cas d'usage, `ProjectionComptes.parClient` (+ JDBC), `PointEntreeComptes`, assemblage -- FR-008-03, FR-008-05 (comptes du jeton, `aujourdhui`)
- `services/documents/src/test/**` -- tests du cas d'usage, de l'adaptateur JDBC (Testcontainers), IT HTTP (200 par rôle avec comptes du seul connecté, 401 sans jeton, `aujourdhui` figé par `ConfigurationHorlogeFixe`), tags `FR-008-03`, `FR-008-05`, `FR-008-06`
- `frontend/src/domain/documents.ts` (+ test), `frontend/src/application/consulterDocuments.ts` (+ test) -- mois, années IFU, port et cas d'usage -- 100 % couverts
- `frontend/src/adapters/http/ClientDocumentsHttp.ts` (+ test) -- liste des comptes et téléchargement authentifié, statuts traduits
- `frontend/src/adapters/ecrans/documents/EcranDocuments.{tsx,css}` (+ test) -- écran, tests par rôle et par ligne de la matrice, tags FR-008-01/02/03/05/06, FR-030-06
- `frontend/src/adapters/{textes.ts,routes/routes.tsx,routes/routes.test.tsx,test/rendreApplication.tsx}`, `frontend/src/infrastructure/main.tsx` -- textes, route, assemblage, redirection des quatre chemins sans jeton

**Acceptance Criteria :**
- Voir les 11 critères Given/When/Then de la Story 6.3 dans `epics.md` : redirection `/login` des quatre chemins, page identique pour les trois rôles, section par compte, IFU de `aujourdhui`, téléchargement Bearer avec ouverture d'onglet, 401 sans onglet, `verifier-scenario.sh SC-008-01` et `SC-017-02` en code 0, commandes `npx vitest run -t 'FR-008-01'` (… `FR-030-06`) et `mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-008-03|FR-008-05|FR-008-06'` en code 0 avec chaque tag exécuté.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 5 findings — high 0, medium 0, low 1, false 4, maybe-false 0
- findings:
  - `[false]` `[reject]` Le 401 n'est pas exercé par le vrai `fetchAuthentifie` dans le test de l'écran — `ClientDocumentsHttp` reçoit `fetchAuthentifie` par injection (`main.tsx`), dont la règle « 401 → suppression de `nb.jeton` + signal » a ses propres tests ; l'adaptateur ne doit que traduire le statut en `technique` (testé), l'écran n'ouvrant aucun onglet.
  - `[false]` `[reject]` Aucune preuve du golden master, du proxy ni de nginx dans le diff — `verifier-scenario.sh SC-008-01` et `SC-017-02`, `build.sh`, `check-gates-exist.sh`, `golden-master.sh` rejoués : code 0 ; `/api/documents/` est déjà dans `portsDesServices` de `vite.config.ts` et dans nginx (hors périmètre, story 6.1).
  - `[false]` `[reject]` Test de redirection des quatre chemins dédoublé entre l'écran et `routes.test.tsx` — redondance sans effet ; chaque chemin est couvert.
  - `[low]` `[reject]` Pas de test dédié de `ProjectionComptesJdbc.parClient` — la requête est exécutée par `PageDocumentsHttpIT` (PostgreSQL réel) et la couverture JaCoCo passe ; ajouter un test redondant n'apporte rien de visible.
  - `[false]` `[reject]` Chemin `GET /api/documents/comptes` absent du corpus — décision de la spec, consignée comme point ouvert dans les Design Notes et ci-dessous.

## Design Notes

Le corpus et l'architecture ne nomment pas le point d'API qui fournit les comptes et `aujourdhui` à l'écran (AD-22 : projection des comptes dans `documents`) : `GET /api/documents/comptes` est retenu, au même préfixe que `telechargement`, donc déjà renvoyé par le proxy Vite et nginx (point ouvert à signaler). Les 12 mois partent du mois de `aujourdhui` (legacy `getRecentMonths` : mois courant puis `-1` mois en boucle). La réponse 200 du téléchargement est lue en `blob` ; l'ouverture passe par `window.open(URL.createObjectURL(blob), '_blank')`, révoquée après usage ; le filtre « 12 mois / 2 années » se calcule en entiers depuis la chaîne `aujourdhui`, jamais depuis `Date`.

## Verification

**Commands:**
- `mvn -B -q -f services/documents/pom.xml verify` -- expected: code 0
- `mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-008-03|FR-008-05|FR-008-06'` -- expected: code 0, chaque tag exécuté
- `cd frontend && npx vitest run` puis `-t 'FR-008-01'`, `-t 'FR-008-02'`, `-t 'FR-008-03'`, `-t 'FR-008-05'`, `-t 'FR-008-06'`, `-t 'FR-030-06'` -- expected: code 0, au moins un test exécuté chacun
- `scripts/golden-master/verifier-scenario.sh SC-008-01` et `SC-017-02` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh && scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `documents` expose `GET /api/documents/comptes` (comptes du seul titulaire du jeton, tout rôle, plus `aujourdhui` de l'Horloge) et le front affiche l'écran `/documents` : IFU des deux années précédentes, par compte RIB, attestation et 12 relevés mensuels ; chaque lien appelle `GET /api/documents/telechargement` avec le jeton et ouvre le PDF dans un nouvel onglet ; 401 → `nb.jeton` supprimé et retour à `/login` sans onglet.

**Fichiers :** `services/documents/**` (`PointEntreeComptes`, `ConsulterPageDocuments` + cas d'usage, `ProjectionComptes.parClient` + JDBC, assemblage, `PageDocumentsHttpIT`, test du cas d'usage) ; `frontend/src/{domain/documents,application/consulterDocuments,adapters/http/ClientDocumentsHttp,adapters/ecrans/documents/EcranDocuments}` avec leurs tests ; `routes.tsx`, `routes.test.tsx`, `main.tsx`, `rendreApplication.tsx`, `textes.ts`.

**Revue :** 0 patch, 0 report, 5 constats rejetés (voir journal de triage). Relecture de suivi recommandée : false.

**Vérification :** `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` code 0 ; `verifier-scenario.sh SC-008-01` et `SC-017-02` code 0 ; `trace-smoke.sh` code 0 (lancé par l'implémentation) ; `mvn -f services/documents/pom.xml verify` et test ciblé `FR-008-03|FR-008-05|FR-008-06` code 0 ; `npx vitest run` (1743 tests) et `-t` pour `FR-008-01/02/03/05/06` et `FR-030-06` code 0.

**Risques résiduels / points ouverts :** le point d'API `GET /api/documents/comptes` n'est nommé par aucun document du corpus ni de l'architecture (choix de la spec, à entériner) ; le lien porte `href="#telecharger"` et le clic est intercepté (pas de clic milieu / Ctrl+clic) ; `window.open` après un appel asynchrone peut être bloqué par un bloqueur de popups ; « DOCUMENTS FISCAUX » est listé « texte non trouvé » par `verifier-scenario.sh` (mis en capitales par le CSS) ; le rejeu réel des scénarios reste à la charge de l'humain en fin d'epic.
