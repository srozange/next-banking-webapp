---
title: "Suspendre, refuser, revoir et rejeter à l'échéance un virement"
type: 'feature'
created: '2026-10-06'
baseline_revision: 'c0cbab4596e95901174b2e705387e3a250297649'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/quality-gates.md'
warnings: ['oversized']
deferred:
  - "Texte manquant : décision demandée sur un virement inconnu ou qui n'est plus suspendu (déjà libéré, rejeté, exécuté). Repris le texte générique api.erreur.404 « Ressource introuvable » (404) ; à faire valider par le métier."
  - "Texte manquant : cellule « Client » de la file de revue tant que le client n'est pas dans la projection locale (identite-clients.client-modifie.v1 pas encore reçu). L'identifiant numérique du client est affiché à la place du nom."
  - "Choix non tranché par la story : à l'heure butoir ou après, le rejet de l'administrateur est exécuté comme le rejet de l'échéance (REJETÉ, audit TRANSFER_AUTO_REJECT par SYSTEM) ; la libération refusée à l'heure butoir est auditée ADMIN_TRANSFER_RELEASE « Issue: REJETE »."
  - "Choix non tranché par la story : « les quatre premiers virements passés en revue » du suivi client = les quatre plus récents virements suspendus à la soumission (SUSPENDU, LIBÉRÉ, REJETÉ ou EXÉCUTÉ après libération), du plus récent au plus ancien."
  - "Non rejoué ici : `curl -s http://localhost:16686/api/traces/<reference>` demande la pile compose démarrée ; couvert en test par la référence de trace (32 caractères hexadécimaux) et l'enrichissement de l'observation courante."
  - "AR-105 close : l'enrichissement de l'observation courante (EnrichissementTrace : virement.controle.issue et erreur) est admis, la création d'une Observation reste interdite."
---

<intent-contract>

## Intent

**Problem:** après les stories 3.1 à 3.3, tout virement est exécuté quel que soit son score ; il n'existe ni suspension avant débit, ni refus pour erreur du contrôle, ni revue par l'administrateur, ni rejet à l'échéance, et l'écran `/virement` ne sait afficher ni avis de suspension ni refus.

**Approach:** compléter la machine à 5 états du cœur bancaire (EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE) par un contrôle de fraude qui décide avant tout débit (suspension si score > 75 ou montant ≥ seuil de fraude ; refus si l'accès à l'historique échoue ou dépasse `nb.fraude.acces-historique.delai`), des cas d'usage de libération, de rejet administrateur et de rejet automatique planifié, la file de revue et l'historique côté ADMIN, puis les écrans front correspondants. Le texte exact de chaque critère est la story 3.4 de `_bmad-output/planning-artifacts/epics.md` (lignes 2453-2590) : elle fait foi, la lire en entier ; les clés de textes viennent de `_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/textes-nouveaux.md` (lots « Stories », `virement.*`, `revue.*`) et de `docs/retro/00-global/MESSAGES.md`.

## Boundaries & Constraints

**Always:** reprendre les critères de la story 3.4 d'`epics.md` tels que rédigés (textes mot pour mot, états, audits dans l'ordre FR-026-03, bornes d'heure butoir) ; `domain` et `application` en Java pur, 100 % lignes et branches (le `check` JaCoCo s'applique même avec `-Dgroups`) ; `src/domain` et `src/application` du front en TypeScript pur, mêmes seuils ; temps uniquement par le port `Horloge` (heure butoir = soumission + 24 h calendaires, `Europe/Paris`, elle fait foi : libération refusée à l'heure butoir ou après) ; transition par mise à jour conditionnelle `UPDATE … WHERE id = ? AND statut = ?` (la première gagne, zéro ligne = refusée) ; aucun solde ne change hors passage à EXÉCUTÉ ; décision de contrôle et transition dans la transaction de soumission, avant toute modification de solde ; un seul événement `virement-decide` par virement, à la décision, avec notification FRAUDE pour la suspension (schéma et `etat`/`type` à élargir dans `contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json` et ses deux tests de contrat) ; libération : LIBÉRÉ + remise sur `coeur-bancaire.virement-a-traiter.v1` dans la même transaction (échec d'écriture de la remise : le virement reste SUSPENDU), puis le consommateur existant (`TraiterVirementATraiterCasUsage`) débite et passe à EXÉCUTÉ avec deux opérations, ou REJETÉ « solde insuffisant au débit » (audit TRANSFER_REJECT_INSUFFICIENT_FUNDS par `SYSTEM`, sans IP) ; libérer et rejeter : rôle ADMIN vérifié dans le service (403 sinon, `{"message"}`), motif obligatoire (« Veuillez indiquer un motif », 400), identité et IP tracées ; alertes OPEN du virement passent à REVIEWED (les autres statuts restent) ; audit par `outbox_audit` via `Transaction.executerIndependamment` : codes ADMIN_TRANSFER_RELEASE, ADMIN_TRANSFER_REJECT, TRANSFER_AUTO_REJECT, TRANSFER_REJECT_INSUFFICIENT_FUNDS à ajouter à `ActionAudit` et au contrat/schéma d'audit et au service `audit` si son catalogue est fermé (AR-40) ; rejet automatique par `@Scheduled` dans `adapter.in.planification`, toutes les minutes, zone `Europe/Paris`, ligne `BATCH_JOB_LOGS` `SUSPENDED_TRANSFERS_EXPIRY` seulement s'il rejette au moins un virement ou échoue, désactivé en profil `rejeu` ; refus pour erreur : virement persisté REFUSÉ_ERREUR_CONTRÔLE, ni solde, ni opération, ni remise, ni notification ; erreur journalisée ; réponse `{"message","reference"}` (référence = identifiant de trace courant) ; l'adaptateur entrant enrichit l'observation courante de `virement.controle.issue` (EXECUTE, SUSPENDU, REFUSE_ERREUR_CONTROLE) et signale l'erreur, sans créer de span (`! grep -rnE 'Observation\.(createNotStarted|start)\(|@Observed|@NewSpan|spanBuilder\(|nextSpan\(' services/*/src/main --include=*.java`) ; `POST /api/coeur-bancaire/virements` renvoie `etat`, `heureButoir` (SUSPENDU) et l'écho des champs ; `nb.fraude.*` et `nb.fraude.acces-historique.delai` invalides (nuls ou négatifs) : démarrage refusé ; nom complet du client lu dans une projection locale du cœur bancaire alimentée par `identite-clients.client-modifie.v1` (consommateur idempotent `evenements_traites`, `versionAgregat` pour écarter un message ancien) ; front : `data-testid` `{écran}-{élément}[-{id}]`, couleurs par les tokens, textes par `textes.ts` depuis les sources ci-dessus, aucun texte inventé (un texte manquant est consigné dans `deferred`), sections nouvelles placées après le contenu comparé ; chaque test tagué `@Tag` (ou `-t` côté Vitest) avec les identifiants cités par les `-Dgroups` / `-t` de la story ; versions de `versions-verifiees.md`.

**Never:** cumul glissant sur 24 h (story 3.5), formulaire prérempli de refaire (3.6), relevé (3.7), liste complète des comptes et opérations du tableau de bord (3.8), lecture/gestion des notifications (3.9), alerte MULTIPLE_FAILED_AUTH (3.10) ; aucune notification pour la remise asynchrone, la libération, le rejet, le rejet automatique ni le refus pour erreur ; mot « fraude », « score » ou nom de signal dans l'avis ou la notification client ; message « alerte(s) fraude » (`! grep -rn "alerte(s) fraude" frontend/src services/*/src/main`) ; appel HTTP entre services ; créer un span à la main ; modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils de couverture ou de mutation, exclusions ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; écrire `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Score 75 | 9 999,99 €, score 75 | EXÉCUTÉ, débité, audit VIREMENT « … - Issue: EXECUTE » puis TRANSFER_EXECUTED, une notification | — |
| Score 76 / montant 10 000,00 | soumission le 5 à 10:15 | SUSPENDU, aucun débit/crédit/opération/remise, heure butoir le 6 à 10:15, notification FRAUDE, audit « Issue: SUSPENDU », réponse `etat` + `heureButoir` | — |
| Erreur du contrôle | délai d'historique dépassé puis erreur simulée, 50 € et 50 000 € | REFUSÉ_ERREUR_CONTRÔLE persisté, rien débité, aucune opération/remise/notification, audit seul « Issue: REFUSE_ERREUR_CONTROLE », `{"message","reference"}` | erreur journalisée |
| Transition illégale | REJETÉ → LIBÉRÉ, EXÉCUTÉ → SUSPENDU | refusée sans effet (zéro ligne) | refus |
| Libération | ADMIN, SUSPENDU, solde suffisant, butoir − 1 s, motif « vérifié par téléphone » | LIBÉRÉ + remise ; consommateur : EXÉCUTÉ + 2 opérations ; 2 alertes OPEN → REVIEWED, CONFIRMED inchangée ; audit VIREMENT, ADMIN_TRANSFER_RELEASE « … - Issue: EXECUTE », TRANSFER_EXECUTED ; « Libération enregistrée : le virement est accepté, son débit va être effectué. » | — |
| Libération, solde devenu insuffisant | avant butoir | REJETÉ sans débit, audit « Issue: REJETE », « Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit. » | — |
| Rejet au débit asynchrone | LIBÉRÉ, solde insuffisant | REJETÉ, sans débit/crédit/notification, TRANSFER_REJECT_INSUFFICIENT_FUNDS par SYSTEM sans IP | — |
| Libération ou rejet à l'heure butoir ou après | butoir exact, butoir + 1 s | REJETÉ sans débit, « Heure butoir dépassée : le virement est rejeté. » (`alert-error`) | — |
| Remise impossible à la libération | écriture de remise en échec | libération échoue, virement reste SUSPENDU | erreur |
| Rejet admin | motif « client injoignable », avant butoir | REJETÉ sans débit, alerte OPEN → REVIEWED, aucune notification, audit ADMIN_TRANSFER_REJECT, « Rejet enregistré : le virement est rejeté sans débit. » | — |
| Motif vide ou d'espaces | libération ou rejet | 400 `{"message":"Veuillez indiquer un motif"}` ; modale : champ `revue-motif` `aria-invalid="true"` ; virement inchangé | 400 |
| Rôle insuffisant | CLIENT ou CONSEILLER sur file, libération, rejet | 403, virement inchangé | 403 |
| Rejet automatique | soumis le 5 à 10:15 ; planif. le 6 à 10:14:59, 10:15:00, 10:15:01 | SUSPENDU, puis REJETÉ sans débit ni notification, audit TRANSFER_AUTO_REJECT par SYSTEM | — |
| Concurrence | libération et rejet auto à l'heure butoir | une seule transition ; REJETÉ ; une seule entrée d'audit de décision | — |
| Parcours d'audit | six parcours | exactement 2, 3, 3, 2, 2 et 1 entrées, dans l'ordre | — |
| File et historique admin | ADMIN sur `/administration` | file triée par heure butoir croissante, colonnes et modale du texte de la story ; historique « Virements traités » (50 derniers) seulement s'il en existe ; « Aucun virement suspendu » sinon | — |
| Suivi client | `user1` sur `/virement` et `/tableau-de-bord` | section « Suivi de mes virements » (quatre premiers virements passés en revue, badges d'état) ou « Aucun virement à suivre » | — |
| Front suspension | réponse SUSPENDU sur `/virement` | pas de redirection, `alert-warning` `role="status"` focalisé, formulaire vidé | — |
| Front refus | réponse d'erreur du contrôle | vue de refus remplace le formulaire, `alert-warning` `role="alert"`, référence sélectionnable, focus sur le titre | — |
| Configuration | seuil `nb.fraude.*` ou délai d'historique nul/négatif | le cœur bancaire refuse de démarrer | démarrage refusé |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/EtatVirement.java` -- ne contient que `EXECUTE` : ajouter SUSPENDU, LIBERE, REJETE, REFUSE_ERREUR_CONTROLE et la table des transitions autorisées ; `Virement.java` (record, `executer` l.~57 construit toujours EXECUTE) à scinder : décision de contrôle (EXECUTE/SUSPENDU), heure butoir ; `StatutAffichage`, `TextesVirement` (seul fichier de textes de virement), `ActionAudit` (2 valeurs), `TypeNotification` (FRAUDE à ajouter), `NotificationVirement`, `VirementDecide`, `ErreurVirement.Cause`.
- `.../application/ExecuterVirementCasUsage.java` -- `executer`/`ecrire` : le contrôle (`ControleFraudeCasUsage.evaluer`, `Evaluation`) est déjà appelé avant débit mais n'influence pas l'état ; brancher suspension (aucun `comptes.debiter`, pas d'opérations, pas de `VirementATraiter`, notification FRAUDE), refus pour erreur (échec/délai de `HistoriqueVirements` : enregistrer REFUSÉ, hors annulation de transaction), audits `Issue: SUSPENDU` / `REFUSE_ERREUR_CONTROLE` ; `auditer(...)` existant (transaction indépendante). `ControleFraude.java`, `ControleFraudeCasUsage.java`, `ParametresControleFraude`, `SeuilsFraude` (seuil d'attente, seuil de fraude), `HistoriqueVirementsJdbc` (verrou `pg_advisory_xact_lock`, ajouter `lock_timeout`/délai `nb.fraude.acces-historique.delai`).
- `.../application/TraiterVirementATraiterCasUsage.java` -- aujourd'hui « déjà exécuté » seulement : traiter un LIBÉRÉ (débit conditionnel, EXÉCUTÉ + opérations, ou REJETÉ avec audit SYSTEM) ; modèle de test `ReceptionVirementATraiterIT`, `RecepteurVirementATraiter`.
- `.../application/port/out/DepotVirements.java`, `adapter/out/persistance/DepotVirementsJdbc.java`, `V2__comptes_operations_virements.sql` -- ajouter colonnes (statut/état, heure butoir, motif de suspension, décideur, motif de décision, date de l'issue), `transiter(id, de, vers)` conditionnel, file des SUSPENDU triée, virements à échéance, historique des 50 derniers traités, quatre premiers virements d'un client ; nouvelle migration `V4__…sql` (jamais modifier V1-V3) ; table `BATCH_JOB_LOGS` ; `AlertesFraude`/`AlertesFraudeJdbc` : passage OPEN → REVIEWED par virement ; projection `clients` (nom complet, `versionAgregat`) alimentée par un récepteur Kafka sur `identite-clients.client-modifie.v1` (modèle : `RecepteurVirementATraiter`, `EvenementsTraitesJdbc`, contrat `contrats/evenements/identite-clients.client-modifie.v1.schema.json`, test consommateur `*ContratEvenement*Test`).
- `.../adapter/in/PointEntreeVirements.java` -- `VirementExecute` à étendre (`heureButoir`), enrichissement de l'observation courante (`virement.controle.issue`) ; `GestionErreursVirements.java` + `AttributsErreur.java` (format d'erreur, ajouter `reference` = identifiant de trace) ; `ConfigurationSecurite.java` (JWT sans rôles par chemin : appliquer ADMIN dans le service pour `/api/coeur-bancaire/revue/**`, lire le rôle du jeton comme le fait l'epic 2, story 2.6) ; nouveau point d'entrée de la revue (file, libérer, rejeter, historique) et du suivi client ; `adapter/in/planification/` (à créer, `@Scheduled`, profil `rejeu` désactivé) ; `adapter/out/horloge/` seul endroit autorisé pour l'heure (`HorlogeArchitectureTest`).
- `contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json` et `VirementDecideContratEvenementTest` (producteur, cœur bancaire) / consommateur `services/notifications` -- élargir `etat` (SUSPENDU) et `type` (FRAUDE) ; `coeur-bancaire.audit.v1.schema.json` et `services/audit` -- nouveaux codes d'audit si le catalogue est fermé.
- `frontend/src/` -- hexagonal : `domain/virement.ts`, `domain/montant.ts`, `application/virement.ts` + `lireMontant.ts` (TypeScript pur), `adapters/http/ClientVirementHttp.ts`, `adapters/ecrans/virement/`, `adapters/routes/routes.tsx` (route `/administration` réservée ADMIN déjà posée à l'epic 2, route `/tableau-de-bord`), `adapters/gabarit/`, `adapters/ui/` (composants du design system, modale), `textes.ts` ; ajouter avis de suspension (`virement-avis-suspension`, `virement-avis-heure-butoir`), vue de refus (`virement-refus-reference`), section de revue autonome et historique (`revue-liberer-{id}`, `revue-rejeter-{id}`, `revue-motif`, `revue-valider`), section « Suivi de mes virements » (`virement-virements-revue`, `dashboard-virements-revue`, `virement-etat-{id}`) ; tests Vitest tagués `FR-027-04`, `FR-027-16`, `FR-016-19`, `FR-003-12`, `FR-003-27` ; `frontend/vite.config.ts` seul fichier de config Vitest ; `couleurs-hors-tokens.test.ts`, `motifs-interdits.test.ts`, `architecture-temps.test.ts` doivent rester verts.
- Sources de vérité : PRD FR-027-02/04/16/17, FR-016-19, FR-026-02/03, FR-023-01/02/04, FR-003-07..16, FR-003-27 ; `ARCHITECTURE-SPINE.md` AD-12, AD-18, AD-22, AD-25 ; `docs/retro/features/FEAT-027`, `docs/retro/00-global/{MESSAGES,LEGACY-BUGS}.md` (BUG-076, BUG-084, BUG-007) ; `textes-nouveaux.md`.

## Tasks & Acceptance

**Execution:**
- `services/coeur-bancaire/src/main/java/**/domain/**` -- états, transitions, suspension, heure butoir, motif de suspension, notification FRAUDE, nouveaux codes d'audit, textes -- machine à états pure
- `services/coeur-bancaire/src/main/java/**/application/**` -- cas d'usage de soumission (suspension, refus), `LiberationVirementCasUsage`, `RejetVirementCasUsage`, `EcheanceRevueCasUsage`, file/historique/suivi, traitement du LIBÉRÉ, ports -- règles de revue et d'échéance
- `services/coeur-bancaire/src/main/java/**/adapter/**`, `infrastructure/**`, `application.properties`, `V4__…sql` -- JDBC conditionnel, projection clients, API de revue (rôle ADMIN), planification, observation, `reference` -- accès et exposition
- `contrats/evenements/*.json`, `services/notifications/**`, `services/audit/**` -- élargissement des contrats et des catalogues -- cohérence inter-services
- `frontend/src/**` -- avis de suspension, vue de refus, section de revue et historique (ADMIN), section « Suivi de mes virements », textes -- surface utilisateur
- Tests : `ControleFraudeSuspension*Test`, `ControleFraudeErreur*Test`, `MachineEtatsVirement*Test`, `*TransitionConcurrente*Test`, `LiberationVirement*Test`, `RejetVirement*Test`, `EcheanceRevue*Test`, `AuditVirement*Test`, `ConfigurationFraude*Test`, tests de contrat, tests front tagués ; toute la matrice, 100 % lignes et branches sur `domain` et `application` (back et front)

**Acceptance Criteria:**
- Given la story 3.4 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dtest='ControleFraudeSuspension*Test'` puis `verify -Dtest='ControleFraudeErreur*Test'`, `-Dtest='LiberationVirement*Test'`, `-Dtest='RejetVirement*Test'`, `-Dtest='EcheanceRevue*Test'`, `-Dtest='MachineEtatsVirement*Test,*TransitionConcurrente*Test'`, `-Dtest='AuditVirement*Test'` et `-Dtest='ConfigurationFraude*Test'`, when lancés, then code de sortie 0 chacun
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-027-02 | FR-027-04 | FR-027-16 | FR-027-17 | FR-016-19 | FR-026-02 | FR-026-03 | FR-023-01 | FR-023-02 | FR-023-04 | FR-002-10 | FR-003-09 | FR-003-13 | FR-003-15 | FR-003-16 | FR-003-27'`, puis `-Dgroups='BUG-076 | BUG-084 | BUG-007'`, et `cd frontend && npx vitest run -t 'FR-027-04|FR-027-16|FR-016-19|FR-003-12|FR-003-27'`, when lancés, then code de sortie 0 chacun
- Given `! grep -rn "alerte(s) fraude" frontend/src services/*/src/main` et le grep des spans manuels, when lancés, then code de sortie 0
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh`, when lancés, then code de sortie 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 6 findings — high 0, medium 0, low 1, false 4, maybe-false 0 (1 déjà consigné)
- findings:
  - `[false]` `[reject]` notifications et audit sans changement de code principal — `notifications` stocke `type` et `etat` en chaînes sans énumération (grep `CONFIRMATION|ALERTE|enum` vide dans `src/main`) et le catalogue `ActionAudit` du service `audit` contenait déjà les quatre codes ; seuls les schémas devaient s'élargir.
  - `[low]` `[defer]` contrôle Jaeger `curl /api/traces/<reference>` non rejoué — demande la pile compose reconstruite ; couvert en test par la référence de trace et l'enrichissement de l'observation, déjà consigné dans `deferred`.
  - `[false]` `[reject]` comportements navigateur (focus, piège du focus, `Échap`, rôles ARIA) testés sous jsdom — les critères sont des assertions de rendu vérifiables par Vitest ; aucun critère n'exige un navigateur réel.
  - `[false]` `[reject]` planificateur testé avec horloge injectée et non en temps réel — les critères fixent des instants précis (10:14:59, 10:15:00, 10:15:01) qu'une horloge injectée seule peut reproduire.
  - `[false]` `[reject]` deux choix laissés ouverts par la story (rejet admin à l'heure butoir, « quatre premiers virements ») — déjà consignés dans `deferred` avec leur lecture retenue.
  - `[false]` `[reject]` textes de remplacement (« Ressource introuvable », identifiant client) — aucun texte inventé, déjà consignés dans `deferred` comme points ouverts.

## Design Notes

- Un seul cas d'usage de contrôle sert le virement immédiat et, plus tard, l'échéance programmée : aucune évaluation isolée du score exposée.
- La suspension est décidée avant `comptes.debiter` : un virement SUSPENDU n'écrit ni opération, ni remise ; seule l'issue (alertes déjà écrites par 3.2, notification FRAUDE, audit) est produite.
- Le refus pour erreur doit survivre à l'annulation de la transaction de soumission : l'enregistrement REFUSÉ est écrit dans une transaction indépendante (comme l'audit) ou par `executerIndependamment`.
- Si un texte exigé par un critère est absent de `MESSAGES.md` et de `textes-nouveaux.md`, il n'est pas inventé : il est consigné dans `deferred`.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify`, `mvn -B -q -f services/notifications/pom.xml verify` et `mvn -B -q -f services/audit/pom.xml verify` -- expected: code 0
- les commandes `-Dtest`, `-Dgroups` et `npx vitest run -t` de la section Acceptance -- expected: code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0
- `! grep -rnE 'Observation\.(createNotStarted|start)\(|@Observed|@NewSpan|spanBuilder\(|nextSpan\(' services/*/src/main --include=*.java` et `! grep -rn "alerte(s) fraude" frontend/src services/*/src/main` -- expected: aucune ligne

## Auto Run Result

Status: done

**Résumé :** le cœur bancaire décide avant tout débit : EXÉCUTÉ, SUSPENDU (score > 75 ou montant ≥ seuil de fraude, heure butoir à 24 h, notification FRAUDE, aucun solde ni opération) ou REFUSÉ_ERREUR_CONTRÔLE (accès à l'historique en erreur ou trop long, réponse `{message, reference}`, enrichissement de l'observation `virement.controle.issue`). Machine à 5 états avec transitions conditionnelles ; libération et rejet par l'ADMIN (rôle vérifié dans le service, motif obligatoire, audit) ; rejet automatique planifié à l'échéance ; traitement du virement LIBÉRÉ par le consommateur ; file de revue, historique, suivi client ; projection des clients ; front : avis de suspension, vue de refus, section de revue sur `/administration`, « Suivi de mes virements ».

**Fichiers :** `services/coeur-bancaire/**` (domaine, cas d'usage, JDBC, API de revue, planification, migration `V4`, tests) ; `contrats/evenements/coeur-bancaire.{virement-decide,audit}.v1.schema.json` ; `services/notifications` (test de contrat) ; `frontend/src/**` (revue, suivi, avis, refus) ; `compose.yaml` (le cœur bancaire attend `identite-clients`).

**Revue :** 0 patch ; 1 élément différé (contrôle Jaeger non rejoué) en plus des points ouverts déjà consignés ; 5 rejetés, motifs au journal. Recommandation de nouvelle revue : `false`.

**Vérification :** relancés ici : `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` code 0 ; deux `mvn verify -Dgroups` et `vitest -t` code 0 ; greps des spans manuels et de « alerte(s) fraude » vides ; la sélection `-Dtest` combinée a échoué une fois sur un interblocage au `truncate` de `VirementTransitionConcurrenteTest.nettoyer`, puis code 0 deux fois de suite. Les mutations volontaires des critères n'ont pas été rejouées.

**Risques résiduels :** interblocage intermittent au nettoyage de base entre tests d'intégration partageant la même base ; trace Jaeger d'un refus réel à valider après reconstruction des images compose ; points ouverts du `deferred` à trancher par le métier.
