---
title: 'Lire et gérer ses notifications sur le tableau de bord'
type: 'feature'
created: '2026-10-08'
baseline_revision: '5c28f6e32274c1821fa80044958e3c56bbee486e'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/quality-gates.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** les notifications de virement (story 3.3) sont stockées par le service `notifications`, mais celui-ci n'expose aucune API HTTP ni vérification du jeton, et le tableau de bord n'a pas de section « Centre de notifications » : le client ne peut ni les lire, ni les marquer lues, ni les effacer.

**Approach:** ajouter au service `notifications` une API authentifiée par le jeton (liste du client, lecture d'une notification, tout marquer lu, tout effacer, identité toujours prise du jeton) et, sur `/tableau-de-bord`, la section « Centre de notifications » (types, icônes, horodatage, « Non lu », badge, boutons, état vide) placée avant « Suivi de mes virements ». Le texte exact des critères est la story 3.9 de `_bmad-output/planning-artifacts/epics.md` (à partir de la ligne 2895) ; FEAT-002 et FEAT-025 du PRD (FR-002-07, FR-002-08, FR-025-05 à FR-025-07) et `docs/retro/features/FEAT-002/MESSAGES.md` (MSG-002-031 à MSG-002-037) : les lire en entier.

## Boundaries & Constraints

**Always:** hexagone (`domain`/`application` Java pur et TypeScript pur, 100 % lignes et branches, le `check` JaCoCo s'applique même sous `-Dgroups`) ; utilisateur toujours déduit du jeton (`sub` numérique, sinon 401), jamais d'un paramètre ni d'un corps ; liste filtrée par `client_id`, plus récente d'abord (ordre de l'identifiant décroissant) ; notification d'un autre client : 403 `{"message":"Accès refusé"}`, inexistante 404, sans jeton 401 `{"message":"Authentification requise"}` (format `{"message"}` unique, AR-46) ; tout `/api/**` authentifié sauf `GET /actuator/health` (AR-36), clé publique `nb.jeton.cle-publique` (`NB_JETON_CLE_PUBLIQUE`, refus de démarrer sans elle), copier le modèle du service `credits` (`ConfigurationSecurite`, `Textes`, `GestionErreurs`, `AttributsErreur`, `AuthentificationRequise`, `CreditsSecuriteIT`, `OutilsHttp`, `Conteneurs`) ; dépendance `spring-boot-starter-security-oauth2-resource-server` (version de `versions-verifiees.md`) ; classe de preuve `NotificationsSecuriteIT` ; horodatage servi en instant ISO 8601 UTC et formaté par le front en `dd/MM/yyyy HH:mm:ss` via `Intl.DateTimeFormat('fr-FR', { timeZone: 'Europe/Paris' })` dans `src/domain` (AR-45) ; textes du legacy repris à l'identique, casse comprise (« Centre de notifications », « {N} nouvelle(s) », « Tout marquer lu », « Effacer tout », « Non lu », « Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel »), types « CONFIRMATION » ✅, « ALERTE » ⚠️, « FRAUDE » 🚨 ; le type ACCOUNT_BALANCE (FR-025-08) est hors de cette story ; étiquettes techniques du legacy (séparateur « Notifications temps reel (Message Broker) », suffixe « - publie via ActiveMQ Artemis (MDB) ») non reprises (A-002-4) ; icône `aria-hidden="true"`, badge `aria-live="polite"`, pastille « Non lu » avec infobulle (UX-DR46, UX-DR55) ; couleurs par les tokens du thème, pas d'élément natif stylé hors composants du design system ; `data-testid` préfixe `notifications-` ; « Centre de notifications » placé après le contenu comparé et avant « Suivi de mes virements », qui reste la dernière section (UX-DR153) ; montants et identifiants selon les types du domaine posés par les stories 0.1 et 0.2 ; jeton envoyé par le décorateur `fetch` existant (story 0.1), jamais lu dans un client ; tests tagués (Java `@Tag("FR-002-07")`, `FR-002-08`, `FR-025-06`, `FR-025-07`, `FR-025-05` ; Vitest `[FR-002-07]`… dans le titre) ; versions de `versions-verifiees.md` ; texte manquant = point ouvert, aucun texte inventé (le `titre` des notifications de virement reste non affiché).

**Never:** modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils, exclusions de couverture ou de mutation ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; appel HTTP entre services ; seuil métier ou `nb.fraude.*` dans `notifications` ; composer un texte de notification dans `notifications` ; créer un span à la main ; écrire `sprint-status.yaml` ; ajouter des données de référence relevant du « Socle du rejeu final » ; notification ACCOUNT_BALANCE, alertes de fraude (story 3.10), tri ou filtre non demandé.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Sans jeton | `GET /api/notifications/notifications` | 401 | `{"message":"Authentification requise"}` |
| Liste | `user1` (3 types) et `user2` | seules celles de `user1`, plus récente d'abord, avec `id`, `type`, `message`, `creeLe`, `lue` | filtre retiré : `-Dgroups='FR-002-07'` échoue |
| Notification d'autrui | `user1` lit celle de `user2` | 403 | `{"message":"Accès refusé"}` |
| Notification inexistante | identifiant inconnu | 404 | `{"message":"Ressource introuvable"}` |
| Tout marquer lu | `user1`, N non lues | toutes ses notifications lues, celles de `user2` inchangées | — |
| Tout effacer | `user1` | toutes ses notifications supprimées, celles de `user2` intactes | — |
| Écran avec non lues | au moins une non lue | badge « {N} nouvelle(s) » (`aria-live="polite"`), « Tout marquer lu », « Effacer tout » ; clic « Tout marquer lu » : badge disparu | — |
| Écran sans notification | liste vide | message d'état vide exact, ni badge, ni « Tout marquer lu », ni « Effacer tout » | — |
| Écran avec lues seulement | liste non vide, tout lu | « Effacer tout » seul | — |
| Erreur du service | 5xx ou réseau | `AlerteErreur` existante avec le texte technique du legacy, sans trace | pas de texte inventé |
| Jeton expiré | 401 | chemin existant : jeton supprimé, `/login` | — |

</intent-contract>

## Code Map

- `services/notifications/src/main/java/fr/nextbanking/notifications/{domain/Notification,application/port/out/Notifications,adapter/out/persistance/NotificationsJdbc}.java`, `db/migration/V2__notifications.sql` -- existent : l'enregistrement `Notification` n'a pas d'`id` ; ajouter un modèle de lecture porteur de l'`id` et les opérations du port (`duClient`, `parId`, `marquerToutesLues(clientId)`, `supprimerToutes(clientId)`). Pas de nouvelle migration attendue (colonnes `id`, `client_id`, `lue`, `cree_le` existent, index `(client_id, id desc)`).
- `services/notifications/src/main/java/fr/nextbanking/notifications/application/port/in/`, `application/` -- créer les ports entrants et cas d'usage (lister, consulter avec contrôle d'appartenance, tout marquer lu, tout effacer) et exceptions d'application (introuvable, accès refusé), sur le modèle de `credits` (`ListerPrets`, `ConsulterPret`, `PretIntrouvable`, `AccesPretRefuse`) ; `infrastructure/` : assemblage des beans (modèle `AssemblageReceptionVirementDecide`).
- `services/notifications/src/main/java/fr/nextbanking/notifications/adapter/in/http/` (à créer) -- `PointEntreeNotifications` (`/api/notifications/notifications` : `GET` liste, `GET /{id}`, `POST /lecture` pour tout marquer lu, `DELETE` pour tout effacer ; `@AuthenticationPrincipal Jwt`, `Long.parseLong(sub)` sinon `AuthentificationRequise`), `ConfigurationSecurite`, `Textes`, `GestionErreurs`, `AttributsErreur`, DTO JSON (`creeLe` ISO UTC). `adapter/in/` contient déjà `RecepteurTemoin` et `RecepteurVirementDecide` (Kafka) : le paquet `http` s'ajoute à côté. `HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest` doivent rester verts.
- `services/notifications/pom.xml` -- ajouter `spring-boot-starter-security-oauth2-resource-server` ; `src/main/resources/application.properties` -- commentaire de la clé publique (modèle `credits`, jamais de valeur) ; `compose.yaml` (service `notifications`) -- `NB_JETON_CLE_PUBLIQUE: ${NB_JETON_CLE_PUBLIQUE:-}` comme `coeur-bancaire` (déjà proxifié par `frontend/nginx.conf` et `vite.config.ts`, port 8085).
- `services/notifications/src/test/**` -- `infrastructure/Conteneurs.java` (ajouter paire RSA et `nb.jeton.cle-publique`, `management.tracing.export.enabled=false`, comme `credits`), `ContexteDemarre`/`ApplicationNotificationsTest` (le démarrage exige maintenant la clé), modèle `OutilsHttp` à créer ; `NotificationsSecuriteIT` (401 sans jeton et chemins voisins, jeton expiré/mal signé/illisible, sujet non numérique, 403 sur la notification d'autrui, 404, santé publique), IT de lecture/marquage/effacement avec `user1`/`user2`, tests des cas d'usage (100 % `application` et `domain`).
- `frontend/src/domain/` -- créer `notifications.ts` (+ test) : libellé et icône par type, comptage des non lues, formateur `dd/MM/yyyy HH:mm:ss` en Europe/Paris (nouveau, distinct de `formaterDate` du relevé et de `tableauDeBord.ts`). `frontend/src/application/` -- `gererNotifications.ts` (+ test) : port `ClientNotifications` (`lister`, `toutMarquerLu`, `toutEffacer`) et cas d'usage ; modèle `consulterTableauDeBord.ts`.
- `frontend/src/adapters/http/ClientTableauDeBordHttp.ts` (modèle) -- créer `ClientNotificationsHttp.ts` (+ test) ; `adapters/textes.ts` -- bloc `TEXTES.notifications` (textes MSG-002-031 à MSG-002-037) ; `adapters/ui/{Carte,Etiquette,BoutonSecondaire,BoutonTexte,AlerteErreur}` -- composants à réutiliser ; `adapters/ecrans/notifications/SectionCentreNotifications.tsx` (+ CSS, test) à créer.
- `frontend/src/adapters/ecrans/tableauDeBord/EcranTableauDeBord.tsx` -- insérer la section avant `SectionSuiviVirements` ; `adapters/routes/routes.tsx`, `routes.test.tsx`, `adapters/test/rendreApplication.tsx`, `infrastructure/main.tsx` et `Dependances` -- injecter le client des notifications ; `Application.test.tsx`, `accessibilite.test.tsx` à étendre si la route est couverte.
- `scripts/golden-master/verifier-scenario.sh SC-002-02` -- statique ; ne rien modifier ; après livraison les textes de SC-002-02 doivent être trouvés dans `frontend/src` (ceux non trouvés sont consignés dans `reports/golden-master/textes-non-trouves.md`).
- Sources : `docs/retro/features/FEAT-002/{MESSAGES,FIELDS,RG}.md` (RG-002-011 à RG-002-017), `docs/retro/features/FEAT-025/`, `ARCHITECTURE-SPINE.md` (AD-16, AD-17, AD-25), `DESIGN.md`/`EXPERIENCE.md` du UX (composant `notification`, CMP-009).

## Tasks & Acceptance

**Execution:**
- `services/notifications/pom.xml`, `compose.yaml`, `adapter/in/http/**` -- sécurité JWT et API HTTP des notifications -- ACs API et sécurité (401, 403, `NotificationsSecuriteIT`)
- `services/notifications/**` (`domain`, `application`, port sortant, `NotificationsJdbc`, assemblage) -- lire, consulter, tout marquer lu, tout effacer, par client -- FR-002-07, FR-002-08, FR-025-06, FR-025-07
- `services/notifications/src/test/**` -- tests domaine/application (100 %), IT HTTP (isolation `user1`/`user2`, ordre, marquage, effacement), `NotificationsSecuriteIT`, adaptation de `Conteneurs` et des tests de démarrage, tags `FR-002-07`, `FR-002-08`, `FR-025-06`, `FR-025-07`
- `frontend/src/{domain,application}` (+ tests) -- règles pures : types et icônes, comptage, horodatage -- FR-025-05
- `frontend/src/adapters/**` (client HTTP, textes, section, écran, route, assemblage) et tests Vitest `[FR-002-07]`, `[FR-002-08]`, `[FR-025-05]`, `[FR-025-06]`, `[FR-025-07]` -- écran
- Ne toucher à aucun contrôle ni exclusion.

**Acceptance Criteria:**
- Given la story 3.9 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé
- Given `mvn -B -q -f services/notifications/pom.xml verify -Dgroups='FR-002-07 | FR-002-08 | FR-025-06 | FR-025-07'` et `cd frontend && npx vitest run -t 'FR-002-07|FR-002-08|FR-025-05|FR-025-06|FR-025-07'`, when lancés, then code 0 chacun
- Given le filtre par utilisateur volontairement retiré de la lecture, when `mvn -B -q -f services/notifications/pom.xml verify -Dgroups='FR-002-07'` est lancé, then code non nul (puis code restauré)
- Given `scripts/golden-master/verifier-scenario.sh SC-002-02`, when lancé, then code 0
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh`, when lancés, then code 0

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 2, false 2, maybe-false 0
- findings:
  - `[low]` `[reject]` aucun test ne traverse le vrai client HTTP et le vrai service ensemble — le contrat (chemins, `/lecture`, champs `id/type/message/creeLe/lue`) a été relu : `ClientNotificationsHttp.ts` et `PointEntreeNotifications.java` concordent ; un montage de bout en bout dépasse les critères de la story 3.9 et le rejeu est réservé à l'epic 10.
  - `[low]` `[reject]` l'infobulle « Non lu » est un attribut `title` natif, pas un composant stylé (UX-DR46) — le dépôt n'a pas de composant d'infobulle, le texte et l'accessibilité (`aria-label`) sont conformes ; en créer un ajoute une surface publique non demandée.
  - `[false]` `[reject]` `verifier-scenario.sh SC-002-02` est statique et ne prouve pas le rendu — c'est sa définition dans la story ; le rendu est couvert par les tests Vitest de la section et de l'écran.
  - `[false]` `[reject]` `GET /{id}` non utilisé par l'écran — prévu par la spec (Design Notes) pour prouver l'appartenance 403 (NFR-SEC-03).

## Design Notes

- Chemins retenus (`/api/notifications/notifications…`) : préfixe du service (AR-47) puis ressource ; le front calcule le nombre de non lues à partir de la liste. L'identifiant dans `GET /{id}` n'est pas utilisé par l'écran : il existe pour que l'appartenance (NFR-SEC-03, 403) soit vérifiée et prouvée.
- Le plafond de 20 notifications par utilisateur (story 3.3) fait que la liste tient en une réponse : pas de pagination.
- « Tout marquer lu » / « Effacer tout » ne touchent que les lignes du client du jeton (`where client_id = ?`) ; « Effacer tout » ne réinitialise pas le plafond d'un autre agrégat.
- Les données de référence ne contiennent pas de notification : ne pas en ajouter (« Socle du rejeu final »).

## Verification

**Commands:**
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-002-02` -- expected: code 0
- `mvn -B -q -f services/notifications/pom.xml verify -Dgroups='FR-002-07 | FR-002-08 | FR-025-06 | FR-025-07'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-002-07|FR-002-08|FR-025-05|FR-025-06|FR-025-07'` -- expected: code 0
- `! grep -rn 'nb\.fraude\.' services/notifications/src/main` -- expected: code 0
- `! grep -rnE 'Observation\.(createNotStarted|start)\(|@Observed|@NewSpan|spanBuilder\(|nextSpan\(' services/*/src/main --include=*.java` -- expected: aucune ligne

## Auto Run Result

Status: done

**Résumé :** le service `notifications` vérifie désormais le jeton (clé publique `NB_JETON_CLE_PUBLIQUE`, refus de démarrer sans elle) et expose `GET /api/notifications/notifications`, `GET …/{id}` (403 pour une notification d'autrui, 404 si inconnue), `POST …/lecture` (tout marquer lu) et `DELETE …` (tout effacer), toujours pour l'utilisateur du jeton. Le tableau de bord affiche la section « Centre de notifications » (types et icônes, horodatage `dd/MM/yyyy HH:mm:ss` Europe/Paris, pastille « Non lu », badge `aria-live`, boutons, état vide exact) avant « Suivi de mes virements ».

**Fichiers :** `services/notifications/**` (sécurité, API `adapter/in/http`, cas d'usage, port et `NotificationsJdbc`, `NotificationDuClient`, tests `NotificationsSecuriteIT`, `NotificationsHttpIT`, `Conteneurs` adapté), `compose.yaml` (clé publique), `frontend/src/**` (`domain/notifications.ts`, `application/gererNotifications.ts`, `ClientNotificationsHttp.ts`, `adapters/ecrans/notifications/`, textes, écran, routes, assemblage, tests).

**Revue :** 0 patch, 0 différé, 4 rejetés (2 low, 2 faux), motifs au journal de triage. Recommandation de nouvelle revue : `false`.

**Vérification (code 0) :** `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh`, `verifier-scenario.sh SC-002-02` ; `mvn verify -Dgroups='FR-002-07 | FR-002-08 | FR-025-06 | FR-025-07'` ; `vitest -t 'FR-002-07|FR-002-08|FR-025-05|FR-025-06|FR-025-07'` ; greps `nb.fraude.` et spans manuels vides. Filtre par utilisateur retiré : `-Dgroups='FR-002-07'` échoue (constaté par le sous-agent), puis code restauré.

**Risques résiduels :** le test `[FR-003-12]` de `EcranTableauDeBord.test.tsx` a été ajusté pour ignorer le message d'état vide exact (qui contient « Artemis », exigé par le legacy) avant de chercher d'autres libellés techniques ; infobulle en `title` natif (pas de composant d'infobulle) ; le conteneur `notifications` de la pile compose locale a été reconstruit par le sous-agent ; type ACCOUNT_BALANCE hors périmètre (affiché sans icône).
