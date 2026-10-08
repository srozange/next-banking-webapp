---
title: 'Traiter le pipeline des prêts en attente depuis l’administration'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: 'c62db5b41451a4bb8b9fe6ba84fece12fa65f83f'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-5-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problème :** `/administration` ne montre que la file de revue des virements suspendus ; un administrateur ne peut ni voir les prêts EN_ATTENTE, ni les approuver ou refuser à la main, ni consulter l'historique des prêts. `credits` n'a ni notion de rôle, ni projection des clients, ni audit, ni résolution d'IP.

**Approche :** `credits` consomme `identite-clients.client-modifie.v1` (projection locale), expose pour l'ADMIN seul trois routes (pipeline, historique, approbation/refus) et écrit l'audit `ADMIN_LOAN_APPROVE` / `ADMIN_LOAN_REFUSE` dans `outbox_audit` (topic `credits.audit.v1`, consommé par `audit`) ; `/administration` affiche deux sections nouvelles, « Pipeline Prets — En attente d approbation » et « Historique de tous les prets ».

## Boundaries & Constraints

**Always :**
- Routes (`credits`, jeton obligatoire, rôle `ADMIN` exigé dans le cas d'usage par la revendication `roles`, modèle `JetonRoles` + `DecisionAdministrateur.exigerAdmin` de `coeur-bancaire`) : `GET /api/credits/administration/prets/en-attente`, `GET /api/credits/administration/prets` (historique, tri `date_debut` décroissant, tous statuts), `POST /api/credits/administration/prets/{pretId}/approbation` et `/refus` (200). Rôle autre : 403 `{"message"}` « Accès refusé » (`Textes.ACCES_REFUSE`), rien modifié. Lignes : `id`, `clientId`, `nomClient` (projection ; repli sur `clientId` en texte si le client est absent), `type` (peut être `null`, BUG-078), `montant`, `statut`, `dureeMois`, `tauxAnnuel`, `mensualite`, `objet`, `dateDebut` ; montants en chaînes à 2 décimales.
- Décision admin : nouveau cas d'usage Java pur (pas de `idEvenement`, pas de `evenements_traites`, aucun événement `credits.pret-decide.v1`, aucune notification), dans `Transaction.executer` : `PretRepository.transitionner(id, EN_ATTENTE, EN_COURS|REFUSE)` conditionnelle ; approbation → `creerEcheancier(id, EcheancePret.echeancier(pret))` (réutiliser la logique de `DecisionPretCasUsage`, extraire un helper commun si utile, sans changer le comportement de 5.3). Prêt inexistant ou non EN_ATTENTE : ignoré, 200 sans erreur, aucun échéancier, aucune ligne d'audit. Transition réussie : l'audit est écrit par `Transaction.executerIndependamment(...)` dans `outbox_audit` (`utilisateur` = identifiant de l'admin, `action` ADMIN_LOAN_APPROVE|ADMIN_LOAN_REFUSE, `detail` « Approbation prêt id={loanId} » / « Refus prêt id={loanId} », `adresseIp`) **avant** la fin de la transaction métier, de sorte qu'une erreur ultérieure (échéancier) annule la décision mais laisse la ligne d'audit (FR-026-04) ; publication Kafka après coup, un échec de publication ne défait rien (modèle `EcritureAudit`, `JournalAuditKafka`, table `outbox_audit` de `coeur-bancaire`). Erreur inattendue : 500 `{"message"}` « Erreur lors du traitement du prêt. Veuillez réessayer. » (nouvelle clé `pret.erreur.traitement` à inscrire à `textes-nouveaux.md`), prêt resté EN_ATTENTE, sans détail technique.
- IP : résolue dans `adapter.in.http` (modèle `ResolveurAdresseIp` : `X-Forwarded-For` retenu seulement si l'appelant direct est dans `nb.reseau.mandataires-de-confiance`, littéral IP, sinon adresse distante) ; ajouter la propriété à `credits` (`application.properties`, `compose.yaml` comme `coeur-bancaire`, tests).
- Audit : nouveau topic `credits.audit.v1` (déclaré par `NewTopic` dans `ConfigurationEvenements`, enveloppe `{idEvenement, type:"audit", version:1, horodatage, producteur:"credits", donnees:{utilisateur, action, detail, adresseIp}}`, clé = identifiant du prêt), schéma `contrats/evenements/credits.audit.v1.schema.json`, catalogue d'actions fermé (ADMIN_LOAN_APPROVE, ADMIN_LOAN_REFUSE) ; le service `audit` ajoute le topic à `RecepteurAudit` et enregistre dans `AUDIT_LOGS`.
- Projection des clients : table `clients` (`client_id`, `login`, `nom_complet`, `role`, `actif`, `conseiller_id`, `date_creation`, `supprime`, `version_agregat`) en migration V4 avec `outbox_audit` ; consommateur `@KafkaListener` sur `identite-clients.client-modifie.v1` (modèle `RecepteurClientModifie` de `coeur-bancaire`), mise à jour seulement si `versionAgregat` strictement supérieur (insertion si client inconnu), dans la même transaction que `evenements_traites` ; données de référence générées depuis `services/identite-clients/reference-data/reference-data.sql` par `scripts/generer-projections-reference.sh` (créé s'il n'existe pas), jamais écrites à la main. Tests : `ClientModifieContratEvenementConsommateurTest` (exemple du schéma), double livraison Testcontainers = un seul effet.
- Front : nouveau port `ClientPipelinePrets` (domaine/application purs, 100 % lignes/branches), client HTTP vers `/api/credits/administration/...`, section pipeline (titre « Pipeline Prets — En attente d approbation » ; colonnes « ID », « Client », « Type », « Montant », « Duree », « Taux », « Mensualite », « Motif », « Actions » ; `admin-approuver-{loanId}`, `admin-refuser-{loanId}` ; ligne « Aucun pret en attente » sous les en-têtes) et section historique (« Historique de tous les prets » ; colonnes « ID », « Client », « Type », « Montant », « Statut », « Duree », « Mensualite », « Date debut » ; « Aucun pret en base »), lecture seule. Messages : « Prêt approuvé » / « Prêt refusé » dans `admin-message-succes`, texte d'erreur fixe dans `admin-message-erreur` quel que soit l'échec (403, 404, 500, réseau, JSON illisible) ; aucune confirmation ; liste et historique relus après l'action ; 401 : jeton supprimé, retour `/login`. Dans `/administration` les deux sections se placent avant la file de revue des virements suspendus (les indicateurs et « Gestion des clients » n'existent pas encore : ordre pipeline, historique, revue). Textes legacy à l'identique (`MESSAGES.md` FEAT-016, MSG-016-042/071/132/133) ; graphie sans accent des en-têtes ci-dessus ; `data-testid` `admin-{élément}[-{id}]`.
- Domaine/application back et front sans framework, 100 % lignes/branches ; temps par `Horloge` ; mutation/couverture inchangées.
- Tags back `@Tag` : `FR-016-03`, `FR-016-04`, `FR-016-05`, `FR-016-07`, `FR-016-12`, `FR-016-06`, `FR-026-02`, `FR-026-05`, `FR-030-07`, `BUG-054` (au moins un test exécuté chacun) ; test d'IP (hors plage ignorée, dans la plage retenue) ; test « opération annulée → ligne présente dans `outbox_audit` » ; test d'intégration de transition concurrente de la décision admin. Front : le nom de chaque test commence par `[FR-016-03]`, `[FR-016-04]`, `[FR-016-05]`, `[FR-016-07]` ou `[FR-016-12]`.

**Never :**
- Aucune modification de `scripts/gates/**`, `.bmad-loop/**`, `.claude/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, seuils, exclusions ; rien sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; jamais de rejeu du golden master ; ne pas écrire `sprint-status.yaml`.
- Pas de `credits.pret-decide.v1` sur décision admin, pas d'appel HTTP entre services, pas de nouvelle dépendance, pas de confirmation, pas de texte inventé hors `pret.erreur.traitement`, pas de relance de `retro-forward-bmad`, pas de création des indicateurs ni de « Gestion des clients » (epic 9), pas de consommateur `notifications` (5.4).

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Gestion d'erreur |
|----------|---------------|----------------------|------------------|
| Pipeline | `admin`, prêts EN_ATTENTE | liste avec nom complet de la projection | — |
| Pipeline vide | aucun EN_ATTENTE | « Aucun pret en attente » | — |
| Approuver | EN_ATTENTE 36 mois | EN_COURS, 36 échéances, « Prêt approuvé », audit ADMIN_LOAN_APPROVE avec IP, aucun pret-decide | — |
| Refuser | EN_ATTENTE | REFUSE, « Prêt refusé », audit ADMIN_LOAN_REFUSE | — |
| Déjà décidé | non EN_ATTENTE | ignoré, 200, ni échéancier ni audit | pas d'erreur |
| Décisions concurrentes | 2 threads | un seul effet, une seule ligne d'audit | — |
| Erreur en cours de décision | échéancier en échec | prêt EN_ATTENTE, ligne `outbox_audit` présente, texte fixe | 500 sans détail |
| Rôle insuffisant | CLIENT/CONSEILLER | 403 `{"message"}`, rien modifié | — |
| Historique | tous statuts / aucun prêt | tri date début décroissant / « Aucun pret en base » | — |
| Événement client | version supérieure / égale ou inférieure / double livraison | mis à jour / ignoré / un seul effet | — |
| IP | `X-Forwarded-For` hors/dans plage | adresse distante / valeur retenue | — |

</intent-contract>

## Code Map

- `services/credits/src/main/java/fr/nextbanking/credits/{domain,application}` -- `Pret`, `StatutPret`, `EcheancePret.echeancier`, `DecisionPretCasUsage` (modèle : filtre EN_ATTENTE + `transitionner` + `creerEcheancier`, couplé à l'événement : ne pas le réutiliser tel quel), ports `PretRepository` (ajouter `enAttente()`, `tous()` triés, jointure/lookup du nom client), `Transaction` (`executer`, `executerIndependamment` existent) ; à créer `Role`, exigence d'ADMIN, cas d'usage pipeline/historique/décision admin, ports `ProjectionClients`, `JournalAudit`.
- `.../adapter/out/persistance/PretRepositoryJdbc.java` (`JdbcClient`, `COLONNES`), `TransactionSpring` ; `.../adapter/in/http/{PointEntreePrets,ConfigurationSecurite,GestionErreurs,Textes}` (aucun rôle ni IP aujourd'hui ; `Textes.ACCES_REFUSE` existe) ; `.../adapter/in/evenements/RecepteurPretDemande` (modèle de récepteur de `credits`) ; `.../adapter/out/evenements/{RelaisOutbox,EvenementsSortantsKafka}` ; `.../infrastructure/{AssemblageCredits,ConfigurationEvenements}` ; `src/main/resources/db/migration/` (V1 à V3 ; V4 à créer ; `evenements_traites(id_evenement)`).
- Modèles dans `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` : `adapter/in/{JetonRoles,ResolveurAdresseIp}`, `application/{DecisionAdministrateur,EcritureAudit}`, `adapter/in/evenements/RecepteurClientModifie`, `adapter/out/evenements/{JournalAuditKafka,EnveloppeAudit,LigneOutboxAudit}`, migration `V2__comptes_operations_virements.sql` (`outbox_audit`) et `V4` (`clients`) ; tests `ResolveurAdresseIpTest`, `AuditVirementTest`, `RecepteurClientModifieTest`, `JournalAuditKafkaTest`.
- `contrats/evenements/` -- `identite-clients.client-modifie.v1.schema.json` (exemple et 9 champs), `identite-clients.audit.v1.schema.json` (modèle du nouveau schéma `credits.audit.v1`) ; `services/audit/**` `adapter/in/evenements/RecepteurAudit` (`@KafkaListener` à étendre), `domain/ActionAudit` (contient déjà ADMIN_LOAN_*).
- `compose.yaml` (lignes ~206/237 : `NB_RESEAU_MANDATAIRES_DE_CONFIANCE` pour coeur-bancaire et identite-clients ; absent de `credits`) ; `services/credits/reference-data/`, `services/identite-clients/reference-data/reference-data.sql`, `scripts/` (n'a que `gates/` et `golden-master/`).
- Tests modèles `credits` : `PretTransitionConcurrenteTest`, `PretsHttpIT`, `CreditsSecuriteIT`, `DecisionPretKafkaIT`, `OutilsHttp`, `Conteneurs`, `*ArchitectureTest`, `application/*CasUsageTest`.
- Front : `frontend/src/adapters/routes/routes.tsx` (`/administration` ne rend que `SectionRevueVirements`), `adapters/ecrans/administration/*`, `application/revue.ts`, `adapters/http/ClientRevueHttp.ts`, `infrastructure/main.tsx` (câblage), `adapters/textes.ts` (`TEXTES.revue`), `adapters/ui/{Tableau,BoutonTexte,AlerteSucces,AlerteErreur}`, `domain/pret.ts` (`formaterMontantLigne`), tests `routes.test.tsx`, `accessibilite.test.tsx`.
- Lecture seule : `docs/retro/features/FEAT-016/{RG,MESSAGES}.md`, `docs/retro/00-global/MESSAGES.md`, `EXPERIENCE.md` (B-13), `_bmad-output/planning-artifacts/epics.md` Story 5.6 (l. 3993-4050), `ARCHITECTURE-SPINE.md`.

## Tasks & Acceptance

**Execution :**
- `services/credits/src/main/{java,resources}`, `contrats/evenements/credits.audit.v1.schema.json`, `scripts/generer-projections-reference.sh`, `compose.yaml` (propriété d'IP seulement), `services/audit/**` (récepteur, topic) -- projection des clients, rôle ADMIN, audit transactionnel indépendant, IP, décision admin, pipeline et historique, migration V4, topic, textes -- FR-016-03..07, FR-016-12, FR-016-06, FR-026-02/04/05, FR-030-07.
- `services/credits/src/test/**`, `services/audit/src/test/**` -- tests unitaires (matrice), IT Testcontainers des routes (admin vs CLIENT/CONSEILLER, déjà décidé, erreur → EN_ATTENTE + audit présent, aucun événement `pret-decide`), concurrence, consommateur client (contrat, version, double livraison), IP, audit de bout en bout vers `AUDIT_LOGS` -- tags de la story.
- `_bmad-output/planning-artifacts/ux-designs/**/textes-nouveaux.md` -- inscrire `pret.erreur.traitement` -- traçabilité du texte.
- `frontend/src/**` -- port, client HTTP, sections pipeline et historique, câblage `routes.tsx` / `main.tsx`, textes, tests Vitest (affichage, vide, approuver/refuser, relecture, erreurs, 401, ordre des sections, axe) -- FR-016-03/04/05/07/12.

**Acceptance Criteria :**
- Given `admin` et un prêt EN_ATTENTE, when il ouvre `/administration`, then la section pipeline liste le prêt avec le nom complet du client, avant la file de revue ; sans prêt, « Aucun pret en attente ».
- Given un prêt EN_ATTENTE de 36 mois, when `admin` clique « Approuver » (resp. « Refuser »), then EN_COURS + 36 échéances (resp. REFUSE), « Prêt approuvé » (resp. « Prêt refusé ») dans `admin-message-succes`, sans confirmation, audit avec IP, aucun `credits.pret-decide.v1`.
- Given un prêt non EN_ATTENTE, when l'admin le décide, then rien ne change, sans erreur ni audit de succès.
- Given une erreur pendant la décision, then « Erreur lors du traitement du prêt. Veuillez réessayer. » dans `admin-message-erreur`, prêt EN_ATTENTE, ligne d'audit présente.
- Given `user1` ou `sophie`, when ils appellent les routes d'administration, then 403 `{"message"}` et rien modifié ; `admin` réussit.
- Given l'historique, then tous les prêts, tri date de début décroissante, colonnes attendues, « Aucun pret en base » si vide.
- Given un événement `client-modifie`, then la projection est mise à jour seulement si `versionAgregat` est strictement supérieur ; double livraison = un effet.
- Given `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-016-03|FR-016-04|FR-016-05|FR-016-07|FR-016-12|FR-016-06|FR-026-02|FR-026-05|FR-030-07|BUG-054'` puis `verify` (credits et audit), then code 0, chaque tag porté par un test exécuté ; `cd frontend && npx vitest run -t 'FR-016-03'` (puis `-04`, `-05`, `-07`, `-12`) code 0 avec au moins un test.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 6 findings — high 0, medium 0, low 2, false 4, maybe-false 0
- findings:
  - `[low]` `[reject]` un prêt déjà décidé affiche quand même « Prêt approuvé » / « Prêt refusé » — le serveur renvoie 200 sans corps (spec : « ignoré, 200 sans erreur »), le legacy affiche aussi le message sans distinguer ; le corriger demanderait un nouveau contrat de réponse que la spec n'impose pas.
  - `[false]` `[reject]` enveloppement de toute `RuntimeException` en `DecisionAdministrateurEnEchec` — voulu par la spec (texte fixe, sans détail technique) ; `exigerAdmin` est levé avant le `try`, donc le 403 n'est pas masqué.
  - `[low]` `[reject]` exception de publication de l'audit avalée sans journal dans `DecisionAdministrateurPretCasUsage.publier` — la ligne reste `not publie` en base et `RelaisOutboxAudit` la republie ; ajouter un journal serait de la complexité pour un cas rare.
  - `[false]` `[reject]` 401/403/404/500 tous rendus par le texte d'erreur fixe — c'est la consigne de la spec ; le 401 est géré en amont par `appeler`.
  - `[false]` `[reject]` `ResolveurAdresseIp` casse le démarrage si la propriété manque — comportement identique à `coeur-bancaire`, propriété ajoutée à `application.properties` et `compose.yaml`, et `mvn verify` de `credits` (tous les IT) sort en code 0.
  - `[false]` `[reject]` identifiant de prêt de test par `nanoTime % 100000` — risque de collision négligeable, aucun échec observé sur `mvn verify` ; sans effet sur le comportement livré.

## Design Notes

Écarts constatés au code : `/administration` n'a ni indicateurs ni « Gestion des clients » (epic 9) → ordre retenu pipeline, historique, revue. Audit : il est écrit via `executerIndependamment` **après** une transition réussie mais **avant** la fin de la transaction métier ; ainsi « ignoré » n'écrit rien, et une erreur d'échéancier annule la décision en laissant l'audit (BUG-054, FR-026-04). Le rôle est vérifié par le cas d'usage (décision D6), pas par le chemin. `traite_le` n'est pas requis dans `evenements_traites` de `credits`. La clé du message d'audit est l'identifiant du prêt.

## Verification

**Commands :**
- `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-016-03|FR-016-04|FR-016-05|FR-016-07|FR-016-12|FR-016-06|FR-026-02|FR-026-05|FR-030-07|BUG-054'` puis `mvn -B -q -f services/credits/pom.xml verify` et `mvn -B -q -f services/audit/pom.xml verify` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-016-03'` (puis `FR-016-04`, `-05`, `-07`, `-12`), `npx tsc --noEmit` -- expected: code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `credits` consomme `identite-clients.client-modifie.v1` (projection des clients), expose pour l'ADMIN seul le pipeline, l'historique et l'approbation/refus des prêts (conditionnels, sans événement `pret-decide`), et écrit l'audit `ADMIN_LOAN_APPROVE` / `ADMIN_LOAN_REFUSE` avec l'IP dans `outbox_audit` (topic `credits.audit.v1`, enregistré par `audit`) ; `/administration` affiche les sections « Pipeline Prets — En attente d approbation » et « Historique de tous les prets » avant la file de revue.

**Fichiers :** `services/credits/**` (migration V4, rôle ADMIN, cas d'usage pipeline et décision, audit Kafka, résolveur d'IP, récepteur client, points d'entrée, tests dont `DecisionAdministrateurTransitionConcurrenteTest`), `services/audit/**` (écoute de `credits.audit.v1`), `contrats/evenements/credits.audit.v1.schema.json`, `scripts/generer-projections-reference.sh`, `compose.yaml`, `textes-nouveaux.md` (`pret.erreur.traitement`), `frontend/src/**` (port, client HTTP, `SectionsPretsAdministration`, câblage, textes, tests).

**Revue :** 0 patch, 0 report, 6 rejets motivés (cf. Review Triage Log). Revue complémentaire recommandée : `false`.

**Vérification :** `mvn verify` de `credits` et `audit`, tags de la story, `vitest run --coverage`, `tsc --noEmit` en code 0 (par l'implémenteur) ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` relancés en code 0.

**Risques résiduels :** pas de test de bout en bout navigateur ↔ vrai `credits` (formes JSON dupliquées côté back et front, chacune testée) ; un prêt déjà décidé affiche tout de même le message de succès ; pas d'indicateurs ni de « Gestion des clients » sur `/administration` (epic 9), ordre pipeline, historique, revue ; une panne d'écriture de l'audit fait échouer la décision (500).
