---
title: 'Télécharger ses relevés mensuels et ses IFU'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: 'add7ab16029b2b96c800174773ecfe457b4ddaac'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/AGENTS.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-6-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-6-1-telecharger-son-rib-et-son-attestation-de-solde-depuis-le-service-documents.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problème :** `GET /api/documents/telechargement` reconnaît `type=RELEVE` et `type=IFU` mais aboutit à l'erreur 500 (livrés « par la 6.2 ») ; `documents` n'a aucune projection des opérations et `coeur-bancaire` ne publie aucun événement sur les opérations enregistrées.

**Approche :** faire publier `coeur-bancaire.operation-enregistree.v1` par l'outbox (une opération = un événement), alimenter dans `documents` une projection locale des opérations (consommateur idempotent), et générer le relevé mensuel d'un compte du titulaire et l'IFU du client du jeton avec OpenPDF (`adapter.out.pdf`).

## Boundaries & Constraints

**Always :**
- Spécification de référence : Story 6.2 de `_bmad-output/planning-artifacts/epics.md` (l. 4144-4190, tous les critères d'acceptation font foi) ; textes PDF : `docs/retro/features/FEAT-017/MESSAGES.md` (sections « Relevé de compte », « IFU », pied de page commun), règles `FEAT-017/RG.md` (RG-017-007, -010), `FEAT-008/RG.md`, PRD FR-017-04/-07, FR-008-07/-10. Mois en français avec accents (`LibellesMois`), le reste des textes à l'identique du legacy (graphie sans accent). Disposition, polices, formats de date (`dd/MM/yyyy`) et de montant du PDF : reprendre `DocumentService.genererReleve` (~l. 465-520) et `genererIFU` (~l. 600-665) du legacy (`/home/sebastienrozange/workspace/acn-fs/AppModernizationDemonstrateur/legacy/src/main/java/com/accenture/bank/ejb/DocumentService.java`), en réutilisant les helpers déjà présents dans `GenerateurPdfOpenPdf` (`entete`, `ligne`, `pied`, `montant`). Le montant s'imprime comme en 6.1 (`2130.00 EUR`, point décimal, sans séparateur de milliers). Ligne « Compte » du relevé : `{numero} ({type})` comme le legacy.
- Paramètres de `GET /api/documents/telechargement` : ajouter `annee` et `mois`, reçus en `String` (comme `type` et `compteId`). Ordre des refus étendu : jeton (401), `type` absent, `type` inconnu, `compteId` (RELEVE, RIB, ATTESTATION), puis **`annee`** (RELEVE et IFU : absent, vide, non numérique ou hors 2000-2100 → 400) puis **`mois`** (RELEVE : absent, vide, non numérique ou hors 1-12 → 400), tous **avant** le 404 compte, le 403 appartenance et toute génération. Texte du 400 pour `annee`/`mois` : le corpus n'en définit aucun ; réutiliser le texte existant `Textes.REQUETE_INVALIDE` (« Requête invalide »), sans en inventer un nouveau (le consigner comme point ouvert dans le compte rendu).
- RELEVE : seul le titulaire du compte télécharge (aucun contrôle de rôle). Opérations du compte dont l'instant est dans `[premier jour du mois 00:00, premier jour du mois suivant 00:00[` en **Europe/Paris** (`Horloge.fuseau()`), triées par instant croissant puis `operationId` ; sans opération : « Aucune operation sur cette periode. ». Colonnes « Date » (`dd/MM/yyyy`, Europe/Paris), « Description », « Type » (`CREDIT`/`DEBIT`), « Montant » (`+{m} EUR`/`-{m} EUR`), « Solde apres » (`{m} EUR`) ; « Solde actuel » = solde de la projection des comptes ; « Titulaire » = nom complet de la projection des clients ; « Periode » = `{Mois} {annee}`. Fichier `releve_{compteId}_{aaaa}_{mm}.pdf` ; en-têtes de 6.1 inchangés (`application/pdf`, `attachment`, `Cache-Control: no-cache, no-store, must-revalidate`).
- IFU : le client est **toujours** celui du jeton ; tout paramètre `clientId`/`customerId`/`compteId` de la requête est ignoré (NFR-SEC-03). Total des **seuls crédits** de **tous les comptes du client** dont la description contient « interet » (même sémantique de comparaison que le legacy : reproduire sa requête `LIKE '%interet%'`) et dont l'instant est dans l'année civile `annee` en Europe/Paris ; PFU = total × 0,30 arrondi `HALF_UP` à 2 décimales (`BigDecimal`, jamais de flottant) ; « Annee fiscale » = `annee`, « Genere le {jj/mm/aaaa} » par `Horloge`. Fichier `ifu_{clientId}_{annee}.pdf`. Client du jeton absent de la projection des clients → 404 `{"message":"Client introuvable"}` sans PDF (déjà le comportement du cas d'usage pour tout type : le conserver et le tester pour IFU).
- Compteur de téléchargements (table `documents`) : incrémenté seulement après un succès, paramètres de la ligne : `compteId=…;annee=…;mois=…` (RELEVE), `annee=…` (IFU) ; jamais sur 400/403/404/500.
- `coeur-bancaire` : chaque `operations.enregistrer` (virement exécuté `ExecuterVirementCasUsage`, débit asynchrone d'un virement libéré `TraiterVirementATraiterCasUsage`, échéance de virement programmé `ExecuterEcheancesCasUsage`) consigne dans `outbox`, **dans la même transaction**, une ligne du topic `coeur-bancaire.operation-enregistree.v1` (clé `compteId`) ; `Operations.enregistrer` rend l'identifiant généré (`operationId`). Enveloppe sur le modèle d'`EnveloppeCompteModifie` (`type` `operation-enregistree`, `version` 1, `producteur` `coeur-bancaire`), `donnees` : `operationId`, `compteId`, `instant` (UTC, ISO-8601), `libelle` (description), `type` (`CREDIT`/`DEBIT`), `montant` et `soldeApres` (chaînes à 2 décimales), `versionAgregat` (version du compte relue après l'écriture, la même que celle du `compte-modifie` de la même écriture). Les lignes d'outbox sont publiées après validation comme celles de `compte-modifie` (réutiliser `EvenementsComptes` et la liste `lignesComptes` déjà publiée). Aucun chemin de création de compte ni de calcul d'intérêts n'existe dans le code : rien à instrumenter, ne rien inventer (le noter au compte rendu). `NewTopic` dans `ConfigurationEvenements`, `contrats/evenements/coeur-bancaire.operation-enregistree.v1.schema.json` avec exemple (modèle : `coeur-bancaire.compte-modifie.v1.schema.json`), `OperationEnregistreeContratEvenementProducteurTest` (champs exacts, comparaison à `examples[0]`), un test par chemin d'écriture constatant un événement par opération (deux par virement), rollback = aucun événement.
- `documents` : migration `V4` créant la projection `operations` (`operation_id` clé primaire, `compte_id`, `instant timestamptz`, `libelle`, `type` CREDIT/DEBIT, `montant`/`solde_apres` `numeric(19,2)`, `version_agregat`, index par `(compte_id, instant)`) ; consommateur `@KafkaListener` (groupe `documents`) `RecepteurOperationEnregistree` + cas d'usage `RecevoirOperationEnregistreeCasUsage` (même transaction que `evenements_traites`, insertion `on conflict (operation_id) do nothing`, type/version d'événement inattendu ignoré avec avertissement, jamais de libellé ni de numéro en clair dans les journaux au-delà de l'identifiant) sur le modèle de `RecepteurCompteModifie`. Ports de sortie : `ProjectionOperations` (enregistrer, opérations d'un compte sur `[debut, fin[`, total des intérêts crédités d'un client sur `[debut, fin[`). `domain` et `application` restent du Java pur ; OpenPDF reste confiné à `adapter.out.pdf` ; `documents` ne modifie jamais un solde ni n'appelle un autre service en HTTP.
- Données de référence : étendre `scripts/generer-projections-reference.sh` pour générer, dans le bloc « PROJECTIONS GENEREES » de `services/documents/reference-data/reference-data.sql`, les opérations de `services/coeur-bancaire/reference-data/reference-data.sql` (si ce fichier ne contient aucune opération, le bloc des opérations est vide et le script le gère sans erreur), jamais écrites à la main ; relancer le script et vérifier qu'une seconde exécution ne change rien.
- Tests (chaque tag exécuté au moins une fois, format `@Tag("FR-017-04")`) : `FR-017-04`, `FR-017-07`, `FR-017-09` (client introuvable), `FR-008-07`, `FR-008-10`, `NFR-SEC-03` ; PDF extraits par pdfbox (scope test déjà présent) : relevé de mars 2026 sans les opérations d'avril, tri, mois vide, bornes de mois en Europe/Paris (opération à 23:30 UTC le dernier jour du mois = mois suivant), 403 compte d'autrui, 400 `annee`/`mois`, IFU avec arrondi `HALF_UP` au demi supérieur, crédits/débits non-intérêt exclus, autre année exclue, paramètre désignant `user2` ignoré, 404 client absent, compteur ; contrat consommateur sur l'exemple du schéma, double livraison Testcontainers = un seul effet, `ReferenceDataIT`. Couverture JaCoCo inchangée (100 % `domain`/`application`, 80 % `adapter`/`infrastructure`) sur `documents` et `coeur-bancaire`.
- Code métier en français, JSON en français camelCase, montants `BigDecimal` 2 décimales, IBAN masqué dans les journaux, aucun jeton journalisé, temps uniquement par le port `Horloge`.

**Never :**
- Aucune modification de `scripts/gates/**`, `.bmad-loop/**`, `.claude/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, seuils, exclusions de couverture ou de mutation ; rien sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; jamais de rejeu du golden master ; ne pas écrire `sprint-status.yaml`.
- Pas d'écran Documents ni de modification de `frontend/` (story 6.3), aucune nouvelle dépendance, pas de texte inventé, pas de lecture de la clé NVD de `~/.m2/settings.xml`, pas de relance de `retro-forward-bmad`, pas de `mode=reel`, pas d'appel HTTP entre services, pas de calcul d'intérêts ni de création de compte.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Gestion d'erreur |
|----------|---------------|----------------------|------------------|
| Relevé nominal | `user1`, `type=RELEVE&compteId=1&annee=2026&mois=3`, opérations en mars et avril 2026 | 200 PDF `releve_1_2026_03.pdf`, seules les opérations de mars, croissantes, `+`/`-`, compteur +1 | — |
| Mois vide | même demande, mois sans opération | PDF avec « Aucune operation sur cette periode. » | — |
| Bord de mois | opération le 31/03/2026 à 22:30 UTC (01/04 00:30 Paris) | listée en avril, pas en mars | — |
| Relevé d'autrui | compte de `user2`, jeton `user1` (tout rôle) | 403 « Accès refusé », rien généré ni compté | sans PDF |
| `annee`/`mois` invalides | absent, vide, `abc`, `mois=13`, `mois=0`, `annee=1999` | 400 « Requête invalide » avant tout 404/403 | sans PDF |
| Compte inexistant | `compteId=999999` + paramètres valides | 404 « Compte introuvable » | sans PDF |
| IFU nominal | `user1`, `type=IFU&annee=2025`, crédits « interet » en 2025 | 200 PDF `ifu_1_2025.pdf`, total des seuls crédits « interet » de 2025, PFU 30 % `HALF_UP` | — |
| Arrondi | total d'intérêts 12,35 | PFU 3,705 → 3,71 | — |
| IFU sans intérêt | aucun crédit « interet » sur l'année | total 0.00 EUR, PFU 0.00 EUR | — |
| IFU, client forgé | `type=IFU&annee=2025&clientId=2` | PDF de `user1`, jamais de `user2` | — |
| IFU, client absent | jeton valide, client absent de la projection | 404 « Client introuvable » | sans PDF, rien compté |
| Événement opération | nouvel `operationId` / déjà reçu / double livraison | projection alimentée / ignoré / un seul effet | — |
| Producteur | virement exécuté | deux `operation-enregistree` (débit, crédit), `compteId` pour clé | rollback : aucun événement |

</intent-contract>

## Code Map

- `services/documents/src/main/java/fr/nextbanking/documents/application/TelechargerDocumentCasUsage.java` -- ordre des refus et `parametres(...)` à étendre (`annee`, `mois`), calcul des bornes via `Horloge.fuseau()`, chargement des opérations/intérêts ; le contrôle `clients.nomComplet` donne déjà le 404 « Client introuvable ».
- `.../application/port/in/TelechargerDocument.java` -- `Commande(clientId, type, compteId)` à compléter par `annee`, `mois` (chaînes).
- `.../application/DemandeDocumentInvalide.java` + `.../adapter/in/http/GestionErreurs.java` + `Textes.java` -- ajouter un motif « période invalide » rendu par `Textes.REQUETE_INVALIDE` ; `PointEntreeTelechargement.java` -- deux `@RequestParam(required = false)` de plus.
- `.../application/port/out/{GenerateurPdf,Documents,ProjectionComptes}.java`, `.../domain/{DonneesDocument,TypeDocument,CompteProjete,LibellesMois}.java` -- `DonneesDocument` à étendre (période, opérations, total des intérêts, PFU) ; `TypeDocument.exigeCompte()` : RELEVE vrai, IFU faux.
- `.../adapter/out/pdf/GenerateurPdfOpenPdf.java` (`case RELEVE, IFU -> UnsupportedOperationException` à remplacer) -- helpers `entete`, `ligne`, `pied`, `montant`, `ecrire`.
- `.../adapter/in/evenements/RecepteurCompteModifie.java`, `.../application/RecevoirCompteModifieCasUsage.java`, `.../port/in/RecevoirCompteModifie.java`, `.../adapter/out/persistance/{ProjectionComptesJdbc,EvenementsTraitesJdbc,TransactionSpring}.java`, `.../infrastructure/AssemblageDocuments.java` -- modèles à imiter pour la projection des opérations.
- `services/documents/src/main/resources/db/migration/` (V1 `documents`, V2 `evenements_traites`, V3 projections) -- ajouter `V4`. `services/documents/reference-data/reference-data.sql` (bloc `DEBUT/FIN PROJECTIONS GENEREES`), `scripts/generer-projections-reference.sh` -- à étendre.
- Tests modèles : `services/documents/src/test/java/fr/nextbanking/documents/adapter/{TelechargementHttpIT,PdfTexte,DocumentsSecuriteIT,CompteModifieKafkaIT,ConfigurationHorlogeFixe}.java`, `adapter/in/evenements/CompteModifieContratEvenementConsommateurTest.java`, `application/RecevoirCompteModifieCasUsageTest.java`, `referencedata/ReferenceDataIT.java`, `adapter/out/pdf/GenerateurPdfOpenPdfTest.java`.
- `services/coeur-bancaire/.../application/{ExecuterVirementCasUsage (l. ~158-161), TraiterVirementATraiterCasUsage (l. ~103-107), ExecuterEcheancesCasUsage (l. ~193-197), EvenementsComptes}.java` -- appelants de `operations.enregistrer(debit/credit)` suivis de `evenementsComptes.consigner(...)` ; `.../application/port/out/{Operations,EvenementsSortants}.java`, `.../adapter/out/persistance/OperationsJdbc.java` (table `operations(id, compte_id, type, montant, solde_apres, description, date_operation, virement_id)`), `.../adapter/out/evenements/{EvenementsSortantsKafka,EnveloppeCompteModifie}.java`, `.../infrastructure/ConfigurationEvenements.java`, `.../domain/{Operation,CompteModifie,EtatCompte}.java`, tests `CompteModifieContratEvenementProducteurTest`, `VirementIT`, `EcheancesIT`, `TraiterVirementATraiterCasUsageTest`.
- `contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json` -- modèle du nouveau schéma. Corpus : `docs/retro/features/FEAT-017/{RG,MESSAGES,VALIDATION}.md`, `FEAT-008/RG.md` ; legacy hors dépôt : `DocumentService.java` (relevé, IFU).
- Lecture seule : `frontend/**`, `replay-mapping.json`, `docs/retro/golden-master/**`, `scripts/gates/**`.

## Tasks & Acceptance

**Execution :**
- `services/coeur-bancaire/**`, `contrats/evenements/coeur-bancaire.operation-enregistree.v1.schema.json` -- `Operations.enregistrer` rend l'id, événement `operation-enregistree.v1` par outbox sur les trois chemins d'écriture, `NewTopic`, schéma avec exemple, tests producteur et par chemin d'écriture -- producteur côté cœur bancaire.
- `services/documents/src/main/**` -- migration V4, ports et cas d'usage, récepteur Kafka, adaptateurs JDBC, extension de la commande/du contrôleur/des erreurs, génération PDF du relevé et de l'IFU -- FR-017-04, FR-017-07, FR-017-09, FR-008-07, FR-008-10.
- `scripts/generer-projections-reference.sh`, `services/documents/reference-data/reference-data.sql` -- bloc des opérations généré (idempotent) -- jamais écrit à la main.
- `services/documents/src/test/**` -- tests unitaires (matrice I/O), ITs HTTP avec extraction PDF, contrat consommateur, double livraison, `ReferenceDataIT`, sécurité -- tags de la story.

**Acceptance Criteria :**
- Voir les 8 critères « Given/When/Then » de la Story 6.2 dans `epics.md` (l. 4154-4190) : événement `operation-enregistree` produit (une ligne d'outbox par opération, même transaction), projection des opérations idempotente, relevé de mars 2026 / mois vide / compte d'autrui, IFU avec PFU arrondi, paramètre client forgé ignoré, 404 « Client introuvable ».
- Given les commandes du critère « tests de la story », when `mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-017-04|FR-017-07|FR-017-09|FR-008-07|FR-008-10'`, `mvn -B -q -f services/documents/pom.xml verify` et `mvn -B -q -f services/coeur-bancaire/pom.xml verify`, then code 0 et chaque tag exécuté.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh` (si utilisable sans rejeu du golden master), then verts (code 0).

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 1, false 3, maybe-false 0
- findings:
  - `[false]` `[reject]` Chaîne asynchrone jamais exercée de bout en bout (coeur-bancaire → documents) — la spec fixe un test producteur et un test consommateur reliés par le schéma de contrat ; `OperationEnregistreeKafkaIT` et le contrat consommateur sur l'exemple du schéma couvrent chaque moitié.
  - `[false]` `[reject]` Rien dans `frontend/` ni la passerelle — l'écran est la story 6.3 ; le proxy Vite/nginx de `/api/documents/**` existe déjà.
  - `[low]` `[reject]` « interet » sensible à la casse (`LIKE '%interet%'` du legacy) — comportement imposé par la spec (sémantique du legacy), corriger ajouterait un écart non décidé ; non rencontré au quotidien.
  - `[false]` `[reject]` Texte du 400 pour `annee`/`mois` choisi sans critère — réutilise `Textes.REQUETE_INVALIDE`, déjà présent, point ouvert consigné dans la spec et ici.

## Design Notes

Le corpus ne définit aucun texte de refus pour `annee`/`mois` invalides (RG-017-006 « validation error », VAL-017-04/05 « à définir en réécriture : entier 4 chiffres 2000-2100, mois 1-12 ») : plages retenues d'après VALIDATION.md, texte `Textes.REQUETE_INVALIDE` déjà présent dans `documents` (point ouvert à signaler). Les bornes de mois et d'année se calculent avec `Horloge.fuseau()` dans l'application, jamais avec un fuseau écrit en dur ni `now()`. `versionAgregat` d'une opération est celle du compte après l'écriture : elle n'ordonne pas les opérations (clé d'idempotence = `operationId`), elle permet seulement au consommateur de tracer l'ordre de livraison.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify` -- expected: code 0
- `mvn -B -q -f services/documents/pom.xml verify` -- expected: code 0
- `mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-017-04|FR-017-07|FR-017-09|FR-008-07|FR-008-10'` -- expected: code 0, chaque tag exécuté
- `scripts/generer-projections-reference.sh && git status --short services/documents/reference-data` -- expected: seconde exécution sans nouveau changement
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0 (les `KO` de contrôles de stories non livrées sont tolérés par `check-gates-exist.sh`, ceux de cette story doivent être `OK`)

## Auto Run Result

Status: done

**Résumé :** `coeur-bancaire` publie `coeur-bancaire.operation-enregistree.v1` par l'outbox (un événement par opération, même transaction) sur les trois chemins d'écriture ; `documents` projette les opérations de façon idempotente (migration V4) et sert `type=RELEVE` (`annee`, `mois`) et `type=IFU` (`annee`, client du jeton, PFU 30 % `HALF_UP`) avec OpenPDF.

**Fichiers :** `services/coeur-bancaire/**` (événement, enveloppe, `NewTopic`, `Operations.enregistrer` rend l'id), `contrats/evenements/coeur-bancaire.operation-enregistree.v1.schema.json`, `services/documents/**` (projection, récepteur Kafka, cas d'usage, PDF relevé et IFU, validation `annee`/`mois`), `scripts/generer-projections-reference.sh` (bloc des opérations généré, vide aujourd'hui), tests associés.

**Revue :** 0 patch, 0 report, 4 constats rejetés (voir journal de triage). Relecture de suivi recommandée : false.

**Vérification :** `scripts/gates/build.sh` code 0, `check-gates-exist.sh` code 0, `golden-master.sh` code 0, `trace-smoke.sh` code 0, test ciblé `-Dgroups='FR-017-04|FR-017-07|FR-017-09|FR-008-07|FR-008-10'` code 0 (chaque tag porté par au moins un test).

**Risques résiduels / points ouverts :** texte du 400 pour `annee`/`mois` invalides non défini par le corpus (« Requête invalide » réutilisé) ; comparaison « interet » sensible à la casse comme le legacy ; création de compte et calcul d'intérêts inexistants dans `coeur-bancaire` (rien instrumenté) ; aucun test de bout en bout entre les deux services ; bloc des opérations de référence vide (aucune opération dans le `reference-data.sql` de `coeur-bancaire`).
