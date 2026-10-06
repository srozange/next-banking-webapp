---
title: 'Faire appliquer les rôles par les services eux-mêmes'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: '2e65cf513e2ac1d0f970d5a7128b5fc23a1e105d'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** `coeur-bancaire` accepte tout appel sans jeton, et `identite-clients` n'offre aucun moyen de désactiver ou réactiver un client ; aucune fonction réservée ne doit dépendre du seul contrôle du front.

**Approach:** `coeur-bancaire` vérifie le jeton (resource server, clé publique par configuration, témoin public). `identite-clients` expose `POST /api/identite-clients/clients/{clientId}/desactivation` et `/activation` (204) ; le rôle ADMIN est contrôlé dans le cas d'usage `application`, qui consigne l'audit ADMIN_DEACTIVATE / ADMIN_ACTIVATE dans une transaction indépendante avant de modifier `customers.active`.

## Boundaries & Constraints

**Always:** format d'erreur `{"message"}` ; 401 « Authentification requise », 403 « Accès refusé », 404 « Ressource introuvable » ; `application` en Java pur, transactions par le port `Transaction` ; horodatage par le port `Horloge` ; adresse IP résolue par `ResolveurAdresseIp` ; détails d'audit « Désactivation client id={clientId} » / « Activation client id={clientId} », utilisateur = identifiant de l'administrateur ; `trace-smoke.sh` et `POST /api/coeur-bancaire/temoin` public restent verts.

**Never:** modifier `scripts/gates/**`, les seuils ou exclusions de couverture ; rejouer le golden master ; écran « Gestion des clients » (epic 9) ; audit écrit pour un refus de rôle ou un client inexistant ; texte inventé.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Désactivation ADMIN | jeton ADMIN, `user1` actif | 204, `active=false`, audit ADMIN_DEACTIVATE publié avec IP | — |
| Activation ADMIN | jeton ADMIN, `user1` inactif | 204, `active=true`, audit ADMIN_ACTIVATE | — |
| Rôle insuffisant | jeton CLIENT ou CONSEILLER | `active` inchangé, aucun audit | 403 `Accès refusé` |
| Client inexistant | jeton ADMIN, id inconnu | rien modifié | 404 `Ressource introuvable` |
| Sans jeton | aucun | — | 401 |
| Désactivation annulée | échec de l'écriture de `active` | entrée présente dans `outbox_audit` | l'erreur remonte |
| Client désactivé | `user1` inactif, bons identifiants | — | 401 `Identifiants incorrects` ; connexion OK après réactivation |

</intent-contract>

## Code Map

- `services/coeur-bancaire/pom.xml` -- ajouter `spring-boot-starter-security-oauth2-resource-server` (cf. `services/audit/pom.xml:24`)
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/adapter/in/` -- `PointEntreeTemoin` ; ajouter `ConfigurationSecurite` (modèle : `services/audit/.../adapter/in/http/ConfigurationSecurite.java`), `Textes`
- `services/coeur-bancaire/src/test/.../infrastructure/Conteneurs.java` -- doit fournir `nb.jeton.cle-publique` (proprietes + arguments), comme `identite-clients`
- `services/audit/src/test/.../adapter/AuditSecuriteIT.java` -- modèle de `CoeurBancaireSecuriteIT`
- `compose.yaml` (service `coeur-bancaire`) -- variable `NB_JETON_CLE_PUBLIQUE`, `.env.example`
- `services/identite-clients/.../application/SeDeconnecterCasUsage.java` -- modèle des cas d'usage d'audit ; `AssemblageConnexion` -- assemblage
- `services/identite-clients/.../domain/{ActionAudit,EntreeAudit}.java` -- ajouter les 2 codes et fabriques avec détail paramétré
- `services/identite-clients/.../application/port/out/DepotClients.java` + `adapter/out/persistance/DepotClientsJdbc.java` -- ajouter `changerActif`
- `services/identite-clients/.../adapter/in/http/{GestionErreurs,PointEntreeSession}.java` -- mapper les exceptions de domaine ; nouveau `PointEntreeClients`
- `services/identite-clients/src/test/.../adapter/{OutilsHttp,IdentiteClientsSecuriteIT}.java` -- jeton avec rôle au choix
- `contrats/evenements/identite-clients.audit.v1.schema.json` -- description : le service produit aussi ADMIN_*

## Tasks & Acceptance

**Execution:**
- `services/coeur-bancaire/**` -- vérification du jeton, témoin public, santé publique, 401 JSON sans redirection ; `CoeurBancaireSecuriteIT` (tag FR-030-01)
- `compose.yaml`, `.env.example` -- clé publique fournie à `coeur-bancaire`
- `services/identite-clients/**` -- cas d'usage `ChangerEtatClient` (acteur + rôle, 404, audit indépendant puis écriture, publication), points d'API, erreurs 403/404, `IdentiteClientsSecuriteIT` enrichi (jeton d'un autre rôle → 403), `ActivationClientIT` (tags FR-030-07, FR-001-07, FR-016-01, FR-016-11, FR-026-02, FR-026-04, FR-026-05, BUG-093, BUG-001), tests unitaires à 100 % lignes/branches

**Acceptance Criteria:**
- Given coeur-bancaire, when `GET /api/coeur-bancaire/tableau-de-bord` sans jeton, then 401 sans redirection ; le témoin reste public
- Given le cas d'usage appelé avec un acteur CLIENT ou CONSEILLER, then refus et `active` inchangé ; ADMIN réussit
- Given les points d'API, then 403 (CLIENT/CONSEILLER), 204 (ADMIN), 404 (inconnu) au format `{"message"}`
- Given `user1` désactivé puis réactivé, then connexion 401 `Identifiants incorrects` puis réussie
- Given une désactivation puis une activation depuis `203.0.113.7`, then entrées d'audit publiées avec l'IP et le détail attendus ; annulation → ligne dans `outbox_audit`
- Given la story terminée, then `build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh` en code 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 0, false 4, maybe-false 0
- findings:
  - `[false]` `[reject]` `/tableau-de-bord` sans contrôleur : le 401 vient de la chaîne de sécurité — voulu par la story (« tout /api/** authentifié par défaut »), l'écran et la route métier viennent plus tard.
  - `[false]` `[reject]` annulation d'audit testée au niveau cas d'usage et non par HTTP — l'AC vise le cas d'usage et la ligne `outbox_audit`, aucune erreur HTTP ne peut être provoquée sans fausser l'adaptateur.
  - `[false]` `[reject]` pas de règle de rôle dans coeur-bancaire — hors périmètre : seul « jeton requis » y est exigé (AC 1).
  - `[false]` `[reject]` ordre rôle puis existence (CLIENT + id inconnu → 403) — comportement voulu, ne divulgue pas l'existence d'un client à un rôle insuffisant.

## Verification

**Commands:**
- `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-030-07,FR-001-07,FR-016-01,FR-016-11,FR-026-02,FR-026-04,FR-026-05,BUG-093,BUG-001'` -- expected: code 0
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-030-01'` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0


## Auto Run Result

Status: done

**Résumé :** reprise après l'échec de vérification (`rc=-15`, signal d'arrêt, sans erreur de test dans la trace : `scripts/gates/build.sh` dure environ 7 min 45 s). Aucun changement de code nécessaire : l'arbre de travail contient déjà l'implémentation validée au commit 9476920.

**Vérification :** `scripts/gates/build.sh` relancé en entier, code de sortie 0 (couverture front : lignes 99,35 %, branches 98,15 %).

**Risques résiduels :** le délai de la commande de vérification de l'orchestrateur est probablement inférieur à la durée du build ; à relever si l'arrêt se reproduit.
