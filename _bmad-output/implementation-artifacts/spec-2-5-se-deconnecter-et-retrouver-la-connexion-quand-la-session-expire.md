---
title: 'Se déconnecter et retrouver la connexion quand la session expire'
type: 'feature'
created: '2026-10-06'
baseline_revision: '9089c8ef93cf7a5a4d25e6605017cd4c1da482df'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** `nav-deconnexion` est sans effet, aucune déconnexion n'est tracée dans l'audit, le jeton de 30 minutes n'est jamais renouvelé et un 401 en cours de session laisse l'utilisateur sur une page cassée.

**Approach:** Service `identite-clients` : `POST /deconnexion` (audit `LOGOUT`, sans liste de révocation) et `POST /renouvellement` (jeton neuf, 401 si le client est inactif ou inconnu), durée de vie 3 600 s en profil `rejeu`. Front : déconnexion d'un clic, renouvellement sur activité sous 10 minutes restantes, et règle unique « tout 401 → jeton supprimé, `/login` sans message ».

## Boundaries & Constraints

**Always:**
- Audit `LOGOUT` / « Déconnexion », utilisateur = `clientId` en texte, `adresseIp` résolue par `ResolveurAdresseIp` (adapter.in.http), horodatage = port `Horloge`, publié par l'outbox d'audit (`journal.consigner` dans une transaction, puis `publier`). Le catalogue du service `audit` contient déjà `LOGOUT`.
- Aucune liste de révocation : le jeton reste accepté par `GET /moi` jusqu'à son `exp` (test explicite, décision Q1 (a), AR-35, R-5).
- Renouvellement : jeton neuf (nouveau `jti`, `exp` = nouvel `iat` + durée de vie) ; client `actif = false` ou inconnu → 401 `{"message":"Authentification requise"}`, aucun jeton émis. Hors profil `rejeu` : 1 800 s ; profil `rejeu` : 3 600 s.
- Front : calcul de temps du renouvellement uniquement dans `src/adapters` (horloge injectable, test à horloge simulée) ; `domain` et `application` sans `Date.now()`. Seuil : il reste moins de 600 s avant `exp`.
- Textes existants uniquement (« Deconnexion », `/login`) ; aucun texte nouveau, aucun message sur `/login` après expiration ou déconnexion.
- Jamais de jeton ni de mot de passe dans les journaux ou la console.

**Never:**
- Ne pas modifier `scripts/gates/**`, `frontend/.dependency-cruiser.cjs`, `docs/quality-gates.md`, les seuils, `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; ne pas rejouer le golden master.
- Pas de liste de révocation, de cookie, de session serveur, de nouvelle dépendance.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|---------------|----------------------|--------|
| Déconnexion | jeton `user1`, clic `nav-deconnexion` | `nb.jeton` supprimé, `POST /deconnexion` avec le jeton, `/login` | panne réseau : jeton supprimé quand même, `/login` |
| Audit | `POST /deconnexion` depuis `203.0.113.7` | ligne `outbox_audit` LOGOUT, `Déconnexion`, `utilisateur`=`1`, `adresseIp`=`203.0.113.7` | — |
| Après déconnexion | même jeton sur `/moi` avant `exp` | 200 | — |
| Renouvellement | jeton < 10 min restantes + activité | `POST /renouvellement`, `nb.jeton` remplacé | — |
| Plus de 10 min restantes | activité | aucun appel | — |
| Client désactivé | `/renouvellement`, jeton valide | 401, aucun jeton | front : jeton supprimé, `/login` |
| Jeton expiré | action utilisateur, service répond 401 | `nb.jeton` supprimé, `/login`, sans message | tout 401 de tout service |

</intent-contract>

## Code Map

- `services/identite-clients/.../domain/{ActionAudit,EntreeAudit}.java` -- ajouter `LOGOUT("Déconnexion")` et `EntreeAudit.deconnexion(...)`.
- `.../application/SeConnecterCasUsage.java` -- modèle d'audit : `transaction.executer(() -> journal.consigner(entree))` puis `journal.publier(id)`.
- `.../application/port/in/`, `.../application/port/out/{DepotClients,EmetteurJeton,Horloge,JournalAudit,Transaction}` -- ports existants à réutiliser ; nouveaux ports entrants `SeDeconnecter`, `RenouvelerSession`.
- `.../adapter/in/http/{PointEntreeConnexion,PointEntreeProfil,ConfigurationSecurite,AuthentificationRequise}.java` -- modèle d'adaptateur ; `anyRequest().authenticated()` couvre déjà les deux nouveaux chemins.
- `.../infrastructure/AssemblageConnexion.java` -- assemblage des cas d'usage.
- `src/main/resources/application.properties` (`nb.jeton.duree-de-vie=PT30M`) + nouveau `application-rejeu.properties` (`PT60M`).
- `src/test/.../adapter/{IdentiteClientsSecuriteIT,AuditConnexionIT,OutilsHttp,LignesAudit}.java` -- modèles des IT (tags `@Tag("FR-…")`).
- `contrats/evenements/identite-clients.audit.v1.schema.json` -- description à compléter (LOGOUT / « Déconnexion »).
- `frontend/src/application/{seConnecter,chargerProfil}.ts` -- ports `StockageJeton`, `DecodeurJeton` ; nouveaux cas d'usage `seDeconnecter`, `renouvelerSession`.
- `frontend/src/adapters/gabarit/{GabaritAuthentifie,EnTete}.tsx` -- bouton `nav-deconnexion` sans effet, chargement du profil.
- `frontend/src/adapters/http/{ClientProfilHttp,ClientConnexionHttp}.ts`, `adapters/jeton/DecodeurJetonJwt.ts`, `adapters/routes/routes.tsx`, `adapters/test/rendreApplication.tsx`, `infrastructure/main.tsx` -- adaptateurs et assemblage à étendre.

## Tasks & Acceptance

**Execution:**
- Domaine/application Java (+ tests unitaires tagués `FR-001-10`, `FR-001-12`, `FR-026-01`, `FR-026-05`, `FR-030-05`) -- `LOGOUT`, `SeDeconnecter`, `RenouvelerSession`.
- `PointEntreeSession.java` + `AssemblageConnexion.java` -- `POST /deconnexion` (204), `POST /renouvellement` (`{"jeton"}`).
- `application-rejeu.properties` + test de configuration -- 3 600 s / 1 800 s.
- ITs -- audit LOGOUT depuis `203.0.113.7`, jeton accepté après déconnexion, renouvellement (jeton neuf), client inactif 401, jeton expiré/mauvaise clé 401 sur `/moi`.
- Front : `application/{seDeconnecter,renouvelerSession}.ts`, `adapters/http/ClientSessionHttp.ts`, `adapters/http/fetchAuthentifie.ts`, `adapters/session/*` (horloge, expiration du jeton, signal d'expiration), `GabaritAuthentifie`, `EnTete`, `routes`, `main`, tests tagués `FR-001-10`, `FR-030-05`.
- Schéma d'audit -- description mise à jour.

**Acceptance Criteria:** celles de la story 2.5 de `epics.md` (déconnexion, audit, absence de révocation, renouvellement, client désactivé, 401 global, jeton expiré ou mal signé, profil `rejeu`, `verifier-scenario.sh SC-001-05` à 0, `vitest -t 'FR-001-10|FR-030-05'`, `mvn … -Dgroups=…`, contrôles de fin).

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 2, false 2, maybe-false 0
- findings:
  - `[low]` `[reject]` (Intent Alignment) `AUDIT_LOGS` côté service `audit` non lu par `SessionIT` (lecture de `outbox_audit` seulement) — la consommation est déjà couverte par `ReceptionAuditIT` du service `audit`, dont le catalogue contient `LOGOUT` ; `trace-smoke.sh` à 0 ; un test inter-services ajouterait de l'outillage nouveau.
  - `[low]` `[reject]` (Intent Alignment) Règle « tout 401 » prouvée par un test unitaire de `fetchAuthentifie` et par le gabarit, pas par chaque client HTTP — seuls `ClientProfilHttp` (401 traité localement) et `ClientConnexionHttp` (401 = refus de connexion) existent ; les écrans à venir passeront par `fetchAuthentifie`.
  - `[false]` `[reject]` (Intent Alignment) `sprint-status.yaml` apparaît dans le diff — non modifié par cette story : `git status` ne le liste pas, il appartient à l'orchestrateur.
  - `[false]` `[reject]` (Intent Alignment) Diff sans fichiers `services/**` — lecture partielle du relecteur : les fichiers Java sont dans l'arbre, `mvn -Dgroups=… test` sort à 0 (dont `SessionIT`) et `build.sh` à 0.

## Verification

**Commands:**
- `cd frontend && npx vitest run -t 'FR-001-10|FR-030-05'` -- expected: 0.
- `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-001-10,FR-001-12,FR-026-01,FR-026-05,FR-030-05'` -- expected: 0.
- `scripts/golden-master/verifier-scenario.sh SC-001-05` -- expected: 0.
- `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` -- expected: 0.

## Auto Run Result

Status: done

**Résumé :** déconnexion et renouvellement de session.
- Service `identite-clients` : `POST /api/identite-clients/deconnexion` (204, audit `LOGOUT` « Déconnexion » avec l'adresse IP résolue, sans liste de révocation) et `POST /api/identite-clients/renouvellement` (jeton neuf, 401 pour un client inactif ou inconnu). Durée de vie 1 800 s, 3 600 s en profil `rejeu`.
- Front : `nav-deconnexion` supprime `nb.jeton`, prévient le service et mène à `/login` ; renouvellement sur activité quand il reste moins de 600 s (calcul dans `src/adapters/session`, horloge injectable) ; règle unique « 401 → jeton supprimé, `/login` sans message » via `fetchAuthentifie` et le signal d'expiration.

**Fichiers :** voir le commit de cette story — domaine `ActionAudit`/`EntreeAudit`, cas d'usage `SeDeconnecter`/`RenouvelerSession`, `PointEntreeSession`, `application-rejeu.properties`, `SessionIT`, `DureeDeVieJetonTest` ; front `application/{seDeconnecter,renouvelerSession}.ts`, `adapters/{http/ClientSessionHttp,session/*,gabarit/*,routes/*}`, `infrastructure/main.tsx` ; schéma d'audit.

**Revue :** 4 constats (Intent Alignment). Corrigés : aucun. Reportés : aucun. Rejetés : 4 (2 `low` sans intérêt à corriger, 2 réfutés).

**Relecture complémentaire recommandée :** non.

**Vérification :** `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` à 0 ; `vitest -t 'FR-001-10|FR-030-05'` 0 ; `mvn … -Dgroups='FR-001-10,FR-001-12,FR-026-01,FR-026-05,FR-030-05'` 0 ; `verifier-scenario.sh SC-001-05` 0 ; `git status --porcelain` du golden master vide.

**Risques résiduels :**
- Le profil `rejeu` n'est pas activé dans `compose.yaml` : définir `SPRING_PROFILES_ACTIVE=rejeu` au moment du rejeu (epic 10).
- Un 401 tardif arrivé après une reconnexion supprimerait le jeton neuf (cas limite accepté).
- Salutation et déconnexion jamais vues avec un vrai compte (mots de passe `RETRO_*` absents), comme en 2.3 et 2.4.
