---
title: 'Ouvrir la page Prêts et simuler un emprunt'
type: 'feature'
created: '2026-10-06'
baseline_revision: '8705f6d98ef20f7b0e6c3d0084ee648964299de2'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-5-context.md'
warnings: ['oversized']
deferred:
  - summary: "Valeurs des lignes LOANS / LOAN_REPAYMENTS du legacy (import.sql) absentes du dépôt : reference-data.sql de credits est reconstitué (2 prêts EN_COURS de user1, 18 échéances) et à confirmer contre le legacy."
    evidence: "docs/retro/00-global/NFR.md (2 loans, 18 loan_repayments), DATA-DICTIONARY.md (colonnes), FIELDS.md (exemple 5 000 EUR / 4.9 % / 24 mois)."
  - summary: "Prérequis de l'epic 4 (FR-004-05, « Montant invalide ») non livré : le message est livré ici pour la seule simulation ; la création de virement programmé et la demande de prêt (5.2) le reprendront."
    evidence: "sprint-status.yaml : epic 4 en backlog."
---

<intent-contract>

## Intent

**Problème :** `/prets` n'a pas d'écran (lien d'en-tête sans page) et le service `credits` n'existe pas : un utilisateur ne peut ni simuler un emprunt ni voir ses prêts.

**Approche :** Créer le module `services/credits` (hexagonal, base `postgres-credits`, compose, sécurité JWT) exposant la simulation (`POST /api/credits/simulations`) et la liste des prêts du demandeur (`GET /api/credits/prets`), et l'écran `/prets` du front (simulateur, formulaire « Nouveau pret » inerte, liste « Emprunts en cours »).

## Boundaries & Constraints

**Always :**
- Textes du legacy à l'identique, casse et absence d'accents compris (`docs/retro/00-global/MESSAGES.md` §FEAT-007, `textes-nouveaux.md` clé `pret.erreur.simulation`) : séparateurs « Simulateur de pret », « Demande de pret », « Mes emprunts » (capitales par CSS `text-transform`, jamais dans le source) ; titre d'onglet « BankApp Demo - Prets » (`<title>` React 19, comme `EcranConnexion`) ; « Simuler un emprunt », « Montant (EUR) », « Taux annuel (%) », « Duree (mois) », « Calculer », « Mensualite », « Cout total », « Total interets », « Voir l echeancier complet ({n} echeances) », colonnes « # », « Capital », « Interets », « Total », « Capital restant » ; « Nouveau pret », « Type de pret » (`<select>` natif : Immobilier, Automobile, Consommation (défaut), Travaux, Etudiant), « Objet du pret », « Soumettre la demande » ; « Emprunts en cours », colonnes « Type », « Montant », « Taux », « Duree », « Mensualite », « Total interets », « Statut », « Echeancier », « Action » ; statuts « En attente d approbation », « En cours », « Rembourse », « Refuse par le service credit », « En defaut » (tous en `Etiquette`, DEFAUT compris : l'AC prime sur EXPERIENCE.md) ; « En cours de traitement... » (EN_ATTENTE, colonne Echeancier) ; « Aucun emprunt enregistre » (ligne unique sous les en-têtes conservés) ; erreurs « Montant invalide », « Taux invalide (0-30%) », « Duree invalide (1-360 mois) », et 5xx : « Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer. » (apostrophe droite, `pret.erreur.simulation`) en `AlerteErreur` `pret-message-erreur`, jamais d'exception, pile ni nom de composant.
- Simulation côté service : `POST /api/credits/simulations`, corps `{"montant","tauxAnnuel","dureeMois"}` (chaînes), réponse `{"mensualite","coutTotal","totalInterets","echeancier":[{"numero","capital","interets","total","capitalRestant"}]}` (montants `BigDecimal` 2 décimales en chaîne). Validation dans l'ordre montant (null, vide, non numérique ou ≤ 0), taux (non numérique, < 0 ou > 30), durée (non entière, ≤ 0 ou > 360) ; 400 `{"message"}` ; 30 et 360 acceptés. Taux 0 → mensualité = montant / durée ; sinon annuité constante PMT. Arrondi HALF_UP à 2 décimales ; coût total = mensualité × durée ; total intérêts = coût − montant ; intérêts de ligne = arrondi(restant × taux/12), capital = mensualité − intérêts, dernière ligne : capital = restant dû, capital restant 0. Référence : 10000 / 3.5 / 36 → 293.02, 10548.72, 548.72. Le calcul passe par un port de sortie (le test provoque l'échec → 5xx au `message` fixe, via `GestionErreurs`).
- `GET /api/credits/prets` : prêts du client dont l'id est le `sub` du jeton, tri `dateDebut` décroissant ; `GET /api/credits/prets/{pretId}` : 200 si propriétaire, 403 `{"message"}` si prêt d'un autre client, 404 `{"message"}` si inexistant (sert à `CreditsSecuriteIT`). Sans jeton : 401 `{"message":"Authentification requise"}`. Tout rôle authentifié y accède.
- Front : formatage des montants en `src/domain` (`10548.72` → « 10,548.72 EUR » ; liste : « EUR5,000.00 », taux « 4.9000% », durée « 24 mois », cf. FIELDS.md), champs préremplis 10000, 3.50, 36, chaque champ `<label for>` ; montants jamais en flottant côté domaine (centimes entiers). `data-testid` littéraux `pret-{élément}` : `pret-sim-montant`, `pret-sim-taux`, `pret-sim-duree`, `pret-sim-calculer` (figés, SC-007-02), `pret-sim-echeancier-lien` (`aria-expanded`, `aria-controls`), `pret-type`, `pret-montant`, `pret-taux`, `pret-duree`, `pret-objet`, `pret-soumettre`, `pret-message-erreur`.
- Module `credits` : paquets `fr.nextbanking.credits.{domain,application,adapter.in,adapter.out,infrastructure}` ; `domain`/`application` en Java pur, 100 % lignes/branches ; temps uniquement par le port `Horloge` ; IBAN et secrets jamais journalisés ; table `loans` / `loan_repayments` (colonnes de DATA-DICTIONARY, `numeric(19,2)`), migration `V1` sans clé étrangère inter-service ; `reference-data/reference-data.sql` hors migrations, identifiants numériques, généré par un script modelé sur `identite-clients`.
- Infra : `compose.yaml` (`postgres-credits` + volume `pgdata-credits`, service `credits` 8083, `healthcheck`, `depends_on: service_healthy`, `web.depends_on` credits) ; `NB_CREDITS_BD_MOT_DE_PASSE` dans `.env.example` et `Makefile` ; `COPY services/credits/pom.xml` dans les 4 Dockerfile existants.

**Never :**
- Aucune modification de `scripts/gates/**`, `.bmad-loop/**`, `.claude/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, seuils de couverture ; rien sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; ne jamais rejouer le golden master.
- Pas de soumission de demande, de scoring, d'échéancier de prêt existant, de bouton Voir/Supprimer/Marquer paye, d'outbox ni de Kafka (stories 5.2 à 5.6) ; pas d'appel HTTP entre services ; pas de texte inventé ; pas de nouvelle dépendance npm ; pas de `vitest.config.*`.
- Ne pas écrire `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Gestion d'erreur |
|----------|---------------|----------------------|------------------|
| Simulation nominale | 10000 / 3.5 / 36 | 293.02, 10548.72, 548.72, 36 lignes | — |
| Taux nul | 12000 / 0 / 24 | mensualité 500.00 | — |
| Bornes | taux 30, durée 360 | acceptés | — |
| Taux hors plage | -1, 30.01 | 400 « Taux invalide (0-30%) » | aucun résultat affiché |
| Durée hors plage | 0, 361 | 400 « Duree invalide (1-360 mois) » | idem |
| Montant invalide | vide, 0 | 400 « Montant invalide » | idem |
| Panne du calcul | port en échec | 5xx, message fixe `pret.erreur.simulation` | pas de détail technique |
| Liste user1 | 2 prêts de référence | seuls ses prêts, début décroissant | — |
| Sans prêt | autre client | ligne « Aucun emprunt enregistre » | — |
| Prêt EN_ATTENTE | statut EN_ATTENTE | tag « En attente d approbation », « En cours de traitement... » | — |
| Sans jeton | GET /prets | 401 | `/prets` dans le front → `/login` |
| Prêt d'autrui / inconnu | GET /prets/{id} | 403 / 404 `{"message"}` | — |

</intent-contract>

## Code Map

- `pom.xml` (racine) -- ajouter `<module>services/credits</module>` ; JaCoCo/PIT/Dependency-Check hérités, rien à redéclarer.
- `services/audit/` -- modèle du module (pom, Dockerfile, `ConfigurationSecurite`, `Conteneurs`, `ContexteDemarre`, `AuditSecuriteIT`, `application.properties`, `ControleMotDePasseBase`) ; `services/identite-clients/` -- `GestionErreurs`, `AttributsErreur`/`Textes`, 403 `AccessDeniedHandler`, port `Horloge` + `HorlogeSysteme`, `PointEntreeProfil` (`@AuthenticationPrincipal Jwt`), `reference-data/` + `generer-reference-data.sh` + `ReferenceDataIT`.
- `services/*/Dockerfile` (4) -- ajouter la ligne `COPY services/credits/pom.xml services/credits/` ; `compose.yaml`, `.env.example`, `Makefile` -- credits.
- `frontend/vite.config.ts` (proxy `credits: 8083`) et `frontend/nginx.conf` (`credits` dans la regex) -- déjà prêts, vérifier seulement.
- `frontend/src/adapters/routes/routes.tsx` -- `ROUTES_PROTEGEES` (littéraux lus par `verifier-scenario.sh`) : ajouter `'/prets'` ; `Dependances` : `clientPrets` ; `RouteProtegee.tsx`, `adapters/gabarit/`.
- `frontend/src/adapters/textes.ts` -- `TEXTES.titresOnglet.prets`, section `prets` (commentaire de clé `pret.erreur.simulation`).
- `frontend/src/adapters/ecrans/connexion/EcranConnexion.tsx` -- modèle d'écran ; `adapters/ui/` (`ChampTexte`, `BoutonPrimaire`, `AlerteErreur`, `Etiquette`, `Carte`), `adapters/http/ClientProfilHttp.ts` -- modèle d'adaptateur `fetch`.
- `frontend/src/domain/montant.ts` -- `centimesDepuisChaine` ; nouveau formateur ; `domain/navigation.ts` (lien `prets` déjà là).
- `frontend/src/application/chargerProfil.ts` -- modèle de cas d'usage à ports ; `adapters/test/rendreApplication.tsx`, `infrastructure/main.tsx` (+ `main.test.ts`) -- assemblage.
- `scripts/golden-master/verifier-scenario.sh SC-007-01|02` -- lecture seule ; exige `'/prets'` littéral dans `routes/*.tsx` et les testids entre guillemets dans `frontend/src` (hors tests).
- `docs/retro/features/FEAT-007/{RG,FIELDS,MESSAGES}.md`, `docs/retro/golden-master/derived/FEAT-007/SC-007-0{1,2}/` -- sources, lecture seule.

## Tasks & Acceptance

**Execution :**
- `services/credits/pom.xml`, `Dockerfile`, `src/main/resources/{application.properties,db/migration/V1__loans.sql}`, paquets `fr.nextbanking.credits.*` -- module hexagonal : `domain` (`Simulation`, `LignePret`, `Pret`, `StatutPret`, `TypePret`, validations), `application` (`SimulerEmprunt`, `ListerPrets`, `ConsulterPret`, ports `CalculSimulation`, `PretRepository`, `Horloge`), `adapter.in.http` (contrôleurs, `ConfigurationSecurite`, `GestionErreurs`), `adapter.out.persistance` (JDBC), `adapter.out.horloge`, `infrastructure` (application, `ControleMotDePasseBase`) -- FR-007-01..03, 08, FR-004-05.
- `services/credits/src/test/**` -- `HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest`, `CreditsSecuriteIT` (401/403/404 en `{"message"}`), tests unitaires du domaine et de l'application (matrice I/O, `@Tag("FR-007-01")`, `FR-007-02`, `FR-007-03`, `FR-007-08`, `FR-004-05`), IT HTTP + persistance (Testcontainers), `ReferenceDataIT`, démarrage ; 100 % domain/application, 80 % reste.
- `services/credits/reference-data/{reference-data.sql,generer-reference-data.sh}` -- 2 prêts EN_COURS de `user1` (id client 1) et 18 échéances, dates relatives par `Horloge` (valeurs reconstituées : voir `deferred`).
- `pom.xml`, 4 Dockerfile, `compose.yaml`, `.env.example`, `Makefile` -- intégration du module (cf. Always).
- `frontend/src/domain/pret.ts` (+ test) -- formatage montant/taux/durée, libellés de statut et de type, `[FR-007-0x]` dans les titres de test ; `frontend/src/application/` (+ test) -- ports `ClientPrets` (`simuler`, `lister`), cas d'usage `simulerEmprunt` / `listerPrets` (erreur 400 → message du service, 5xx → texte fixe, 401 → purge du jeton et `deconnecte`).
- `frontend/src/adapters/http/ClientPretsHttp.ts` (+ test), `adapters/ecrans/prets/` (écran, CSS par composant, tests jsdom + axe), `routes.tsx`, `textes.ts`, `rendreApplication.tsx`, `infrastructure/main.tsx` (+ test) -- écran `/prets`, route protégée tous rôles.

**Acceptance Criteria :** reprendre les AC de la story 5.1 (`_bmad-output/planning-artifacts/epics.md` §Story 5.1, 14 blocs Given/When/Then), notamment :
- Given le dépôt, when je liste `services/`, then `services/credits/` existe avec pom, Dockerfile, reference-data, paquets, base `postgres-credits`, service compose 8083, et les trois `*ArchitectureTest` passent.
- Given `credits` démarré, when `GET /api/credits/prets` sans jeton, then 401 ; `CreditsSecuriteIT` prouve 401/403/404.
- Given `/prets` ouvert non authentifié, then redirection `/login` ; connecté (CLIENT, CONSEILLER, ADMIN) la page affiche les trois séparateurs et le titre d'onglet.
- Given 10000/3.5/36 et « Calculer », then « Mensualite » 293.02 EUR, « Cout total » 10,548.72 EUR, « Total interets » 548.72 EUR, lien d'échéancier à 36 lignes ; taux 0 → montant/durée.
- Given taux/durée/montant invalides, then 400 et les trois messages exacts sans résultat ; 30 et 360 acceptés ; panne → texte `pret.erreur.simulation`.
- Given `user1` et les données de référence, then liste triée, statuts en `Etiquette`, « En cours de traitement... », « Aucun emprunt enregistre ».
- Given l'écran livré, then `scripts/golden-master/verifier-scenario.sh SC-007-01` et `SC-007-02` sortent en 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 8 findings — high 0, medium 0, low 0, false 6, maybe-false 0 (2 déjà consignés en `deferred`)
- findings:
  - `[false]` `[reject]` verifier-scenario.sh SC-007-01/02 sans preuve dans le diff — les deux scripts ont été exécutés (code 0) à la vérification.
  - `[false]` `[reject]` trace-smoke.sh absent — exécuté après revue (code 0, trace sur 2 services, spans JDBC et Kafka).
  - `[false]` `[reject]` proxy Vite et nginx non modifiés — `credits` y figure déjà (vite.config.ts, nginx.conf) ; aucun changement requis.
  - `[medium]` `[defer]` données de référence reconstituées faute d'import.sql — déjà consigné en `deferred` (non vérifiable sans le legacy).
  - `[low]` `[defer]` prérequis FR-004-05 (epic 4) non livré — message livré ici pour la simulation, déjà en `deferred`.
  - `[false]` `[reject]` preuve en jsdom seulement — les tests Java (`SimulationHttpIT`) couvrent la chaîne HTTP du service ; l'AC ne demande pas de test bout en bout.
  - `[false]` `[reject]` test de panne non vérifié — `SimulationHttpIT:126` et `GestionErreursTest:42` vérifient le 5xx et le message fixe via le port de calcul en échec.
  - `[false]` `[reject]` critères du front non contrôlés par l'auditeur — vérifiés par les tests vitest (356 verts) et par axe ; constat sans défaut cité.

## Design Notes

Les montants circulent en chaînes (`"293.02"`) ; le front ne recalcule rien (formatage par centimes entiers). Le formulaire « Nouveau pret » est rendu complet mais inerte (`type="button"`) : la soumission appartient à la story 5.2. `reference-data.sql` est reconstitué faute d'`import.sql` : l'écart est consigné dans `deferred`.

## Verification

**Commands :**
- `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-01|FR-007-02|FR-007-03|FR-007-08|FR-004-05'` puis `mvn -B -q -f services/credits/pom.xml verify` -- expected: code 0, `CreditsSecuriteIT` exécutée
- `cd frontend && npx vitest run -t 'FR-007-01'` (puis `FR-007-02`, `FR-007-03`, `FR-007-04`, `FR-007-08`) -- expected: code 0, au moins un test chaque
- `scripts/golden-master/verifier-scenario.sh SC-007-01` et `SC-007-02` -- expected: code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` -- expected: code 0 (`KO` d'epic 1 hors périmètre tolérés selon AGENTS.md)

## Auto Run Result

Status: done

**Résumé :** module `services/credits` (hexagonal, base `postgres-credits`, JWT, simulation, liste et consultation des prêts, données de référence) et écran `/prets` du front.

**Fichiers :** `services/credits/**` (module complet et tests), `pom.xml`, 4 Dockerfile, `compose.yaml`, `.env.example`, `Makefile` (intégration du module) ; `frontend/src/{domain/pret.ts,application/prets.ts,adapters/ecrans/prets/**,adapters/http/ClientPretsHttp.ts,adapters/ui/*,routes.tsx,textes.ts}`, `infrastructure/main.tsx` (écran et assemblage).

**Revue :** 0 patch, 2 reports (`deferred` : données de référence reconstituées, FR-004-05 de l'epic 4), 6 rejets motivés (cf. Review Triage Log). Revue complémentaire recommandée : `false`.

**Vérification :** `mvn verify` credits (97 tests) et ciblé par tags (66) OK ; `vitest` 356 OK ; `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh`, `verifier-scenario.sh SC-007-01` et `SC-007-02` : code 0.

**Risques résiduels :** valeurs du `reference-data.sql` à confirmer contre le legacy ; garde-fou ajouté sur le montant (17 chiffres entiers) hors spec ; dates du fichier de référence relatives au jour de génération.
