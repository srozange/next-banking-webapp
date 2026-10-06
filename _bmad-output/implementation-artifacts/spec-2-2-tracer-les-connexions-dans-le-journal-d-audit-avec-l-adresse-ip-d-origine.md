---
title: "Tracer les connexions dans le journal d'audit avec l'adresse IP d'origine"
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: 'f7e88bdb776542f73c494b94f06df12db9d5e9e3'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
warnings: ['oversized']
deferred:
  - summary: >-
      Une connexion tient deux connexions du pool à la fois (transaction de la connexion, puis executerIndependamment en REQUIRES_NEW) : au-delà de 10 connexions simultanées (pool Hikari par défaut), les requêtes s'attendent jusqu'au délai d'obtention (30 s) puis rendent 500 sans audit.
    evidence: |-
      DataSourceTransactionManager prend la connexion dès l'ouverture de la transaction externe, qui reste tenue pendant la comparaison bcrypt ; REQUIRES_NEW en demande une seconde. L'imbrication est imposée par AR-40/AD-18 (et par le critère « opération annulée ») : la parade (pool dédié à l'audit, dimensionnement, borne de concurrence) est une décision d'architecture qui vaut pour tous les services émetteurs d'audit (Blind Hunter, Edge Case Hunter, Verification Gap).
    location: >-
      services/identite-clients/src/main/java/fr/nextbanking/identiteclients/application/SeConnecterCasUsage.java ; adapter/out/persistance/TransactionSpring.java
    severity: medium
  - summary: >-
      Le consommateur audit garde le gestionnaire d'erreurs Kafka par défaut (10 essais sans délai puis abandon) : pendant une panne de sa base de plus de quelques minutes, des entrées d'audit sont perdues définitivement.
    evidence: |-
      Aucun DefaultErrorHandler configuré ; chaque essai attend le délai Hikari (30 s), puis l'offset est validé. Même motif que le consommateur notifications (story 1.4) : la politique de reprise (attente exponentielle, topic de rejet, exceptions non rejouables) est à décider pour tous les consommateurs (Edge Case Hunter).
    location: >-
      services/audit/src/main/java/fr/nextbanking/audit/adapter/in/evenements/RecepteurAudit.java ; services/audit/src/main/resources/application.properties
    severity: medium
---

<intent-contract>

## Intent

**Problem:** Les connexions réussies et refusées ne laissent aucune trace : pas de journal d'audit, pas de service `audit`, et l'adresse IP d'origine n'est résolue nulle part (BUG-083 : le legacy ne la garde que pour certains codes, et sans contrôle du mandataire).

**Approach:** `identite-clients` résout l'adresse IP une fois dans `adapter.in.http` (en-tête `X-Forwarded-For` retenu seulement depuis une plage de confiance), écrit l'entrée d'audit dans `outbox_audit` par `Transaction.executerIndependamment(...)` et la publie sur `identite-clients.audit.v1` ; un nouveau service `audit` (Kafka → `AUDIT_LOGS`, idempotent, sans API HTTP métier) l'enregistre avec l'horodatage de l'enveloppe.

## Boundaries & Constraints

**Always:**
- Hexagone des deux services (`fr.nextbanking.identiteclients`, `fr.nextbanking.audit`) ; `domain` et `application` en Java pur à 100 % lignes et branches ; adaptateurs et infrastructure ≥ 80 %.
- Entrée d'audit = enveloppe AR-39 (`idEvenement`, `type` = `audit`, `version` = 1, `horodatage` ISO 8601 UTC lu sur le port `Horloge` d'`identite-clients`, `producteur` = `identite-clients`, `donnees`) ; `donnees` = exactement `utilisateur`, `action`, `detail`, `adresseIp`. `LOGIN` : `utilisateur` = `clientId` en texte (`"1"` pour `user1`), détail « Connexion réussie ». `LOGIN_FAILED` (inconnu, mauvais mot de passe, compte désactivé) : `utilisateur` = identifiant saisi, détail « Tentative échouée ». Clé Kafka = `utilisateur`. `StringSerializer`, aucun en-tête `__TypeId__`.
- Ligne `outbox_audit` écrite par `Transaction.executerIndependamment(...)` à l'intérieur de la transaction de la connexion, publiée après validation dans le même fil, relais planifié chaque seconde dans l'ordre des identifiants ; un échec Kafka ne change pas la réponse HTTP.
- Adresse IP : résolue une seule fois dans `adapter.in.http` et passée au cas d'usage ; premier élément de `X-Forwarded-For` (rogné) seulement si l'adresse distante est dans `nb.reseau.mandataires-de-confiance` (liste de CIDR séparés par des virgules, défaut `172.30.0.0/24`) et si cet élément est un littéral IPv4/IPv6 non vide ; sinon l'adresse distante. Plage absente ou illisible → refus de démarrer. Aucune résolution DNS.
- Service `audit` : seul à écrire `audit_logs` ; écouteur unitaire, `group.id` `audit` ; `evenements_traites` écrit dans la même transaction que la ligne ; `action_date` = `horodatage` de l'enveloppe ; code hors catalogue fermé AR-40 (13 codes) → aucune ligne, journal « Code d'audit inconnu rejeté : {code} » ; type ou version inattendus → ignoré et journalisé. Aucun `@RestController`/`@Controller`/`@RestControllerAdvice` dans `services/audit/src/main` ; tout `/api/**` authentifié par jeton (clé publique `nb.jeton.cle-publique`, refus de démarrer sans) ; 401 `{"message":"Authentification requise"}` sans jeton, 404 `{"message":"Ressource introuvable"}` avec jeton.
- Tests tagués par exigence et bug (`FR-001-12`, `FR-001-13`, `FR-026-01`, `FR-026-04`, `FR-026-05`, `FR-026-06`, `BUG-083`).
- Journaux : jamais de mot de passe ni de jeton ; motif de corrélation `traceId`/`spanId` dans les deux services.

**Never:**
- LOGOUT, déconnexion, renouvellement, contrôle 403 par rôle, écran `/login`, projection clients, signal MULTIPLE_FAILED_AUTH (stories 2.3 à 2.7 et 3.10).
- Audit d'une requête refusée pour champ requis manquant (400) : aucune tentative d'authentification n'a eu lieu, aucun identifiant saisi.
- Lecture de `audit_logs` par une API ou un écran ; renvoi de `/api/audit/**` par nginx ou Vite ; appel HTTP entre services.
- `server.forward-headers-strategy`, `RemoteIpValve` ou toute résolution d'adresse hors de `adapter.in.http`.
- Modifier un fichier protégé (`scripts/gates/**`, `.trivyignore`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, golden master, `.bmad-loop/**`, `.claude/settings.json`, `.claude/hooks/**`, `config/dependency-check-suppressions.xml`), écrire `sprint-status.yaml`, rejouer le golden master.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Connexion réussie | `user1`, distante dans la plage, `X-Forwarded-For: 203.0.113.7, 10.0.0.1` | 200 ; `outbox_audit` : `LOGIN`, `utilisateur` `"1"`, « Connexion réussie », `adresseIp` `203.0.113.7`, clé `"1"` | — |
| Compte inconnu | `retro-compte-inexistant` | 401 « Identifiants incorrects » ; `LOGIN_FAILED`, `utilisateur` = identifiant saisi, « Tentative échouée » | — |
| Compte désactivé | `user1` bons identifiants, `active = false` | 401 ; `LOGIN_FAILED`, `utilisateur` `user1` | — |
| Champ vide | identifiant ou mot de passe vide | 400 ; aucune ligne `outbox_audit` | — |
| Hors plage | distante hors de la plage, `X-Forwarded-For: 203.0.113.7` | `adresseIp` = adresse distante | — |
| En-tête vide ou absent | distante dans la plage, `X-Forwarded-For` absent, `""` ou `" , 10.0.0.1"` ou `pas-une-ip` | `adresseIp` = adresse distante | — |
| Opération annulée | `Transaction.executer` annule après l'écriture d'audit | ligne présente dans `outbox_audit` | — |
| Livraison double | même message brut livré deux fois au service `audit` | une ligne `audit_logs`, `idEvenement` dans `evenements_traites` | — |
| Code inconnu | `action` = `CODE_INCONNU` | aucune ligne `audit_logs` | journal nommant `CODE_INCONNU` |
| Horodatage | enveloppe `horodatage` `2026-10-05T08:30:00Z` consommée plus tard | `action_date` = `2026-10-05T08:30:00Z` | — |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/adapter/out/evenements/{EvenementsSortantsKafka,RelaisOutbox,LigneOutbox,EnveloppeTemoinEcrit}.java` -- modèle de l'outbox (insert `returning id`, `publier`, `nonPubliees` `order by id limit 100`, `envoyer` avec `get(10 s)` puis `publie = true`) à reproduire pour `outbox_audit`.
- `services/coeur-bancaire/.../adapter/out/persistance/TransactionSpring.java`, `.../application/port/out/{Transaction,Horloge}.java`, `.../adapter/out/horloge/HorlogeSysteme.java`, `.../infrastructure/ConfigurationEvenements.java` (bean `NewTopic` + `@EnableScheduling`) -- à recopier dans `identite-clients` (qui n'a encore ni Horloge, ni Transaction, ni Kafka).
- `services/coeur-bancaire/src/main/resources/application.properties` -- propriétés Kafka producteur (sérialiseurs chaîne, délais bornés, observation) ; `services/notifications/src/main/resources/application.properties` -- propriétés consommateur (`group-id`, `auto-offset-reset=earliest`, `listener.type=single`).
- `services/notifications/` -- modèle complet du consommateur : `adapter/in/RecepteurTemoin` (`@KafkaListener`, lecture d'enveloppe par `JsonMapper`, contrôle type/version), `application/RecevoirTemoinCasUsage` (idempotence dans `transaction.executer`), `adapter/out/persistance/{EvenementsTraitesJdbc,TransactionSpring}`, migration, `pom.xml`, `Dockerfile`, tests `ReceptionTemoinTest` (message brut livré deux fois), `TemoinEcritContratEvenementTest` (consommateur), `infrastructure/{Conteneurs,ContexteDemarre,ApplicationNotificationsTest}`, trois `*ArchitectureTest`.
- `services/coeur-bancaire/src/test/.../adapter/{LecteurTopic,FluxTemoinTest}.java`, `.../adapter/out/evenements/TemoinEcritContratEvenementTest.java`, `contrats/evenements/coeur-bancaire.temoin-ecrit.v1.schema.json` -- lecture d'un topic dans les tests, test de contrat producteur (champs exacts = `required` = `properties`, exemple identique), forme du schéma.
- `services/identite-clients/src/main/java/fr/nextbanking/identiteclients/application/SeConnecterCasUsage.java:159` -- connexion à étendre (adresse IP, transaction, audit) ; `adapter/in/http/PointEntreeConnexion.java:670` -- passe l'adresse résolue ; `adapter/in/http/{ConfigurationSecurite,Textes,AttributsErreur}.java` -- sécurité et erreurs JSON à recopier dans `audit` (sans chemin public de connexion, sans `GestionErreurs`, qui est un `@RestControllerAdvice`).
- `services/identite-clients/src/test/.../infrastructure/Conteneurs.java` -- ajouter Kafka (Testcontainers) et ses propriétés ; `adapter/OutilsHttp.java` (`envoyer` accepte des en-têtes : `X-Forwarded-For`), `adapter/ConnexionIT.java`, `infrastructure/ApplicationIdentiteClientsTest.java` (`avec(...)`, `demarrer(...)`), `HorlogeArchitectureTest` (`Clock` permise dans `adapter.out.horloge` et `adapter.out.jeton`).
- `pom.xml` (racine) -- modules ; Surefire inclut `*IT.java` ; JaCoCo, profils mutation et security hérités. Tous les `services/*/Dockerfile` copient chaque POM du réacteur (`-am`).
- `compose.yaml`, `Makefile` (`MOTS_DE_PASSE`), `.env.example` -- pile et secrets ; réseau `nb` au sous-réseau `${NB_SOUS_RESEAU:-172.30.0.0/24}`.
- `frontend/nginx.conf`, `frontend/vite.config.ts` (lecture seule, déjà conformes) -- `X-Forwarded-For` écrasé par `$remote_addr` / `socket.remoteAddress`, `/api/audit/**` → 404 / absent du proxy.
- `docs/retro/features/FEAT-026/MESSAGES.md`, `docs/retro/00-global/DATA-DICTIONARY.md` (`AUDIT_LOGS` : `ID`, `ACTION`, `ACTION_DATE` non nul, `DETAILS`, `IPADDRESS`, `USERNAME` non nul) -- codes et détails du legacy.

## Tasks & Acceptance

**Execution:**
- `contrats/evenements/identite-clients.audit.v1.schema.json` -- schéma de l'enveloppe (`type` const `audit`, `version` const 1, `producteur` const `identite-clients`, `donnees` aux quatre champs requis, `adresseIp` chaîne ou `null`, `action` enum des 13 codes) avec un exemple `LOGIN`.
- `services/identite-clients/pom.xml` -- `spring-boot-starter-kafka`, `testcontainers-kafka` (test).
- `services/identite-clients/src/main/resources/application.properties` -- `nb.fuseau=Europe/Paris`, `nb.reseau.mandataires-de-confiance=172.30.0.0/24`, propriétés Kafka producteur et observation (calquées sur coeur-bancaire).
- `services/identite-clients/src/main/resources/db/migration/V2__outbox_audit.sql` -- table `outbox_audit` (même forme que `outbox` de coeur-bancaire) et index des lignes non publiées.
- `.../identiteclients/domain/{ActionAudit,EntreeAudit}.java` -- `ActionAudit` : `LOGIN` « Connexion réussie », `LOGIN_FAILED` « Tentative échouée » ; `EntreeAudit(idEvenement, utilisateur, action, adresseIp, horodatage)` validée, `detail()` tiré de l'action, fabriques `connexionReussie(clientId, …)` et `tentativeEchouee(identifiantSaisi, …)`.
- `.../application/port/out/{Horloge,Transaction,JournalAudit}.java`, `.../application/port/in/SeConnecter.java`, `.../application/SeConnecterCasUsage.java` -- `seConnecter(identifiant, motDePasse, adresseIp)` : champs validés d'abord (400 sans audit) ; puis dans `transaction.executer` : recherche, comparaison, entrée `LOGIN` ou `LOGIN_FAILED` consignée par `transaction.executerIndependamment(() -> journal.consigner(entree))` ; après validation `journal.publier(ligne)` ; refus ou jeton ensuite.
- `.../adapter/out/horloge/HorlogeSysteme.java`, `.../adapter/out/persistance/TransactionSpring.java` -- recopiés de coeur-bancaire.
- `.../adapter/out/evenements/{JournalAuditKafka,RelaisOutboxAudit,LigneOutboxAudit,EnveloppeAudit}.java` -- outbox `outbox_audit`, topic `identite-clients.audit.v1`, clé = `utilisateur`.
- `.../adapter/in/http/ResolveurAdresseIp.java`, `PointEntreeConnexion.java` -- résolution unique (matrice I/O), plages CIDR lues au démarrage ; le point d'entrée passe l'adresse au cas d'usage.
- `.../infrastructure/{ConfigurationEvenements,AssemblageConnexion}.java` -- bean `NewTopic` `identite-clients.audit.v1` (3 partitions, réplication 1), `@EnableScheduling` ; assemblage avec les nouveaux ports.
- Tests `identite-clients` -- unitaires domaine et cas d'usage (matrice, Horloge fixe, double de Transaction qui annule) ; `ResolveurAdresseIpTest` ; `AuditConnexionIT` (cas d'usage réel, double de `Transaction` qui annule après l'écriture d'audit, lecture de `outbox_audit`) ; `AdresseIpIT` (plage `127.0.0.0/8` : en-tête retenu, vide/absent/illisible → distante) et `AdresseIpHorsPlageIT` (plage par défaut : en-tête ignoré) ; IT HTTP → topic (LOGIN, deux LOGIN_FAILED, aucun pour 400, pas de `__TypeId__`) ; `AuditContratEvenementTest` (producteur) ; relais ; `Conteneurs` avec Kafka ; démarrage refusé sur plage illisible.
- `services/audit/` -- module neuf calqué sur `notifications` (`pom.xml` avec `spring-boot-starter-security-oauth2-resource-server`, `Dockerfile`, `application.properties` avec `group-id=audit`), migration `V1__audit_logs_et_evenements_traites.sql` (`audit_logs` : `id` identité, `action` non nul, `action_date timestamptz` non nul, `details`, `ipaddress`, `username` non nul ; `evenements_traites`) ; `domain/{ActionAudit (13 codes),EntreeAudit}`, `application/{EnregistrerEntreeAuditCasUsage, port/in/EnregistrerEntreeAudit, port/out/{JournalAudit,EvenementsTraites,Transaction}}` (issue : enregistrée, déjà traitée, code inconnu), `adapter/in/evenements/RecepteurAudit`, `adapter/in/http/{ConfigurationSecurite,AttributsErreur,Textes}`, `adapter/out/persistance/{JournalAuditJdbc,EvenementsTraitesJdbc,TransactionSpring}`, `infrastructure/{ApplicationAudit,ControleMotDePasseBase,AssemblageEnregistrementAudit}`.
- Tests `audit` -- unitaires domaine/cas d'usage/récepteur ; `ReceptionAuditIT` (Testcontainers : double livraison, `CODE_INCONNU` avec journal capturé, `action_date` = horodatage de l'enveloppe) ; `AuditContratEvenementTest` (désérialise l'exemple du schéma) ; `AuditSecuriteIT` ; `ApplicationAuditTest` ; trois `*ArchitectureTest`.
- `pom.xml` -- module `services/audit` ; `services/*/Dockerfile` -- copier `services/audit/pom.xml`.
- `compose.yaml` -- `postgres-audit` (volume `pgdata-audit`, `pg_isready`), `audit` (port `${NB_PORT_AUDIT:-8086}:8080`, mot de passe de base, `NB_JETON_CLE_PUBLIQUE`, Kafka, santé, dépend de `identite-clients` sain) ; `identite-clients` : Kafka et `NB_RESEAU_MANDATAIRES_DE_CONFIANCE: ${NB_SOUS_RESEAU:-172.30.0.0/24}`, dépend de `kafka`.
- `Makefile`, `.env.example` -- `NB_AUDIT_BD_MOT_DE_PASSE` ajouté aux mots de passe générés et listé sans valeur.

**Acceptance Criteria:**
- Given `user1` qui se connecte avec une adresse retenue `203.0.113.7`, when l'événement de `identite-clients.audit.v1` est consommé par `audit`, then `audit_logs` contient `action` `LOGIN`, `details` « Connexion réussie », `username` `1`, `ipaddress` `203.0.113.7`, `action_date` = horodatage de l'enveloppe (test producteur à Horloge fixe + test consommateur à horodatage passé, puis vérification sur la pile).
- Given le service `audit`, when `find services/audit/src/test -name '*SecuriteIT.java'` et `! grep -rn '@RestController\|@Controller' services/audit/src/main`, then une classe est trouvée (401 sans jeton sur `/api/audit/**`, 404 avec jeton valide) et le grep sort en 0.
- Given la pile démarrée par `compose.yaml`, when `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/audit/`, then le code n'est pas 2xx ; `docker compose config --services` liste `audit` et `postgres-audit`, et aucun service sans `healthcheck`.
- Given `grep -n 'proxy_set_header X-Forwarded-For \$remote_addr' frontend/nginx.conf` et `! grep -rn 'proxy_add_x_forwarded_for' frontend/nginx.conf frontend/vite.config.ts`, when lancés, then les deux sortent en 0.
- Given la story terminée, when `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-001-12,FR-001-13,FR-026-01,FR-026-04,FR-026-05,BUG-083'` et `mvn -B -q -f services/audit/pom.xml test -Dgroups='FR-026-01,FR-026-06'`, then les deux sortent en 0.
- Given la story terminée, when `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`, then tous sortent en 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 26 findings — high 2, medium 6, low 12, false 6, maybe-false 0
- findings:
  - `[medium]` `[defer]` Blind : deux connexions du pool par connexion utilisateur (REQUIRES_NEW imbriqué), blocage au-delà de 10 connexions simultanées — imbrication imposée par AR-40/AD-18 ; parade d'architecture reportée au frontmatter `deferred`.
  - `[low]` `[reject]` Blind : aucun test d'intégration de connexion avec Kafka arrêté — `publier` rattrape toute exception et laisse la ligne au relais (JournalAuditKafkaTest), même motif que KafkaInjoignableTest du cœur bancaire ; ajout de test sans défaut observable.
  - `[low]` `[reject]` Blind : relais et publication immédiate peuvent publier deux fois la même ligne — doublon absorbé par `evenements_traites` ; motif AR-38 identique au cœur bancaire ; verrouillage = complexité ajoutée.
  - `[high]` `[patch]` Blind : un identifiant saisi géant (> 1 Mo) devient clé et charge Kafka, l'envoi échoue toujours et le relais reste bloqué sur cette ligne — `EntreeAudit.tentativeEchouee` borne l'utilisateur à 255 caractères (longueur de `customers.username`) ; test domaine ; vérifié sur la pile (identifiant de 300 000 caractères : 401, ligne publiée, clé de 255).
  - `[low]` `[reject]` Blind : message malformé sans traitement dédié dans le récepteur audit — le producteur est notre service, couvert par les tests de contrat ; l'erreur est journalisée par le gestionnaire par défaut ; branches de garde en plus.
  - `[false]` `[reject]` Blind : le consommateur ne vérifie ni le producteur ni le sous-ensemble LOGIN/LOGIN_FAILED — le catalogue fermé exigé est celui des 13 codes (AR-23/AR-40) ; identite-clients publiera aussi ADMIN_DEACTIVATE/ADMIN_ACTIVATE sur ce topic (story 2.6).
  - `[low]` `[reject]` Blind : formats IPv6 différents entre l'en-tête et l'adresse distante — pile compose en IPv4, nginx pose déjà une forme canonique ; cas improbable.
  - `[medium]` `[reject]` Blind : la plage par défaut couvre la passerelle, donc un appel direct au port publié peut imposer l'adresse — plage par défaut fixée par AR-48 et le contrat d'intention ; le corriger reviendrait à modifier la spec ; risque consigné dans les Design Notes et les risques résiduels.
  - `[false]` `[reject]` Blind : clés Kafka différentes entre LOGIN et LOGIN_FAILED d'un même compte — clé = utilisateur fixée par AD-18 ; aucun consommateur ne dépend de l'ordre (la chronologie est `action_date`) ; MULTIPLE_FAILED_AUTH (story 3.10) se calcule dans identite-clients, pas depuis le topic d'audit.
  - `[low]` `[reject]` Blind : pas de purge de `outbox_audit` ni de `evenements_traites` — politique de rétention hors story (motif outbox existant, `make reset-data` prévu) ; aucun défaut d'usage courant.
  - `[low]` `[reject]` Blind : LOGIN audité avant l'émission du jeton — l'émission n'échoue que sur clé illisible, refusée dès le démarrage ; cas improbable.
  - `[low]` `[reject]` Blind : chaîne complète vérifiée seulement à la main — vérification faite sur la pile (LOGIN `1` `203.0.113.7`, LOGIN_FAILED) et consignée ; un test multi-services ajouterait un outillage nouveau.
  - `[false]` `[reject]` Blind : injection de lignes de journal par le code d'action — `action`, `type` et `idEvenement` viennent de nos producteurs (énumérations), aucun chemin utilisateur n'y écrit.
  - `[medium]` `[defer]` Edge : épuisement du pool par REQUIRES_NEW imbriqué — même entrée que le premier constat Blind.
  - `[high]` `[patch]` Edge : une ligne qui échoue toujours bloque le relais — même correctif (troncature à 255 caractères).
  - `[low]` `[reject]` Edge : erreur de base pendant `publier` → 500 alors que l'audit est validé — suppose la base tombée entre deux requêtes ; la connexion échouerait de toute façon.
  - `[low]` `[reject]` Edge : message malformé → essais puis abandon — même raison que le constat Blind correspondant.
  - `[medium]` `[defer]` Edge : panne de la base audit → 10 essais sans délai puis perte de l'entrée — politique de reprise commune aux consommateurs (notifications compris) ; reportée au frontmatter `deferred`.
  - `[low]` `[reject]` Edge : ordre par clé non garanti entre relais et publication immédiate — aucun consommateur ne dépend de l'ordre ; `action_date` porte la chronologie.
  - `[low]` `[reject]` Edge : double publication simultanée — idempotence du consommateur ; même raison que le constat Blind.
  - `[medium]` `[patch]` Verification gap : atomicité `evenements_traites` + `audit_logs` non prouvée sur une vraie base — test `une_transaction_annulee_ne_marque_pas_l_evenement_ni_n_ecrit_la_ligne` ajouté à ReceptionAuditIT (passe).
  - `[medium]` `[defer]` Verification gap (autre) : épuisement du pool — même entrée reportée.
  - `[low]` `[reject]` Verification gap (autre) : messages invalides non gérés — même raison que le constat Blind.
  - `[false]` `[reject]` Intent : critères de pile (AC1, curl, compose) sans preuve — exécutés pendant la vérification : LOGIN `1` `203.0.113.7` dans `audit_logs`, `/api/audit/` → 404, `config --services` liste `audit` et `postgres-audit`, 0 service sans healthcheck, quatre contrôles à 0.
  - `[false]` `[reject]` Intent : lecture « actions de l'opérateur » — aucun critère n'exige d'action humaine hors du dépôt ; statut `done`.
  - `[false]` `[reject]` Intent : chemin réel par nginx jusqu'à la plage de confiance non vérifié — sur la pile, un appel par `localhost:8080` arrive depuis le conteneur web (dans la plage) et l'adresse retenue est celle posée par nginx (passerelle `172.31.250.1`).

### 2026-10-06 — Review pass
- verdicts: 32 findings — high 0, medium 7, low 17, false 8, maybe-false 0
- findings:
  - `[medium]` `[defer]` Blind : consommateur audit sans gestionnaire d'erreurs dédié, entrées perdues pendant une panne de sa base — carried : même constat que le report « politique de reprise du consommateur » du premier passage, déjà au frontmatter `deferred` (non ajouté une seconde fois).
  - `[low]` `[reject]` Blind : messages hors contrat (JSON invalide, `donnees` ou `horodatage` absents) non traités dans `RecepteurAudit` — carried : le producteur est notre service, couvert par les tests de contrat ; erreur journalisée par le gestionnaire par défaut.
  - `[medium]` `[defer]` Blind : deux connexions du pool par connexion utilisateur (REQUIRES_NEW imbriqué) — carried : déjà reporté au frontmatter `deferred`.
  - `[low]` `[reject]` Blind : la connexion attend Kafka (`max.block.ms`, `get(10 s)`), aucun test d'intégration avec Kafka injoignable — carried : `publier` rattrape toute exception et laisse la ligne au relais ; délais bornés sous celui de nginx.
  - `[low]` `[reject]` Blind : ordre du relais non tenu, double publication relais/publication immédiate, plusieurs instances — carried : doublons absorbés par `evenements_traites`, aucun consommateur ne dépend de l'ordre (`action_date` porte la chronologie).
  - `[false]` `[reject]` Blind : une ligne en échec permanent bloque l'outbox pour d'autres causes que la taille — les autres refus permanents cités (topic supprimé, autorisation) touchent toutes les lignes, pas une seule ; seul l'identifiant saisi était non borné, corrigé au premier passage.
  - `[medium]` `[reject]` Blind : adresse IP falsifiable par un appel direct sur le port publié (passerelle dans la plage par défaut) — carried : plage par défaut fixée par AR-48 et le contrat d'intention ; risque consigné dans les Design Notes.
  - `[low]` `[reject]` Blind : schéma sans `maxLength` ni format d'adresse, 13 codes recopiés sans test de cohérence, `producteur` non contrôlé — le producteur reste conforme au schéma ; un test de cohérence schéma/énumération serait un ajout, pour un catalogue fermé et figé (AR-40) ; contrôle du `producteur` déjà rejeté au premier passage.
  - `[low]` `[reject]` Blind : LOGIN consigné avant l'émission du jeton — carried : l'émission n'échoue que sur clé illisible, refusée dès le démarrage.
  - `[low]` `[reject]` Blind : `audit_logs` non protégée en écriture (pas de REVOKE ni de déclencheur), pas d'index de consultation — base dédiée au seul service `audit` ; rôles ou déclencheurs = complexité ajoutée hors intention ; aucune lecture de `audit_logs` dans cette story.
  - `[low]` `[reject]` Blind : pas de rétention pour `outbox_audit` ni `evenements_traites` — carried : politique de rétention hors story.
  - `[low]` `[reject]` Blind : la troncature peut couper une paire de substitution UTF-16 — ne touche qu'un identifiant de plus de 255 caractères avec un caractère hors BMP à la frontière, qui ne désigne aucun compte ; la parade ajoute une branche.
  - `[medium]` `[defer]` Edge : épuisement du pool par REQUIRES_NEW imbriqué — carried : même report.
  - `[medium]` `[patch]` Edge : un identifiant de 255 blancs suivis d'un caractère passe `Identifiants` mais devient blanc une fois tronqué : `EntreeAudit` lève `IllegalArgumentException`, la connexion rend 500 sans entrée d'audit — `EntreeAudit.tentativeEchouee` rogne l'identifiant avant de le tronquer quand il dépasse 255 caractères ; deux cas ajoutés à `EntreeAuditTest` ; vérifié sur la pile (401, `LOGIN_FAILED`, utilisateur `x`).
  - `[low]` `[reject]` Edge : troncature au milieu d'une paire de substitution — même raison que le constat Blind correspondant.
  - `[low]` `[reject]` Edge : erreur de base pendant `publier` → 500 alors que l'audit est validé — carried : suppose la base tombée entre deux requêtes.
  - `[low]` `[reject]` Edge : `emetteur.emettre` échoue après un LOGIN validé — carried : même raison que le constat Blind correspondant.
  - `[low]` `[reject]` Edge : message malformé → essais puis abandon — carried.
  - `[low]` `[reject]` Edge : relais et publication immédiate lisent la même ligne — carried : idempotence du consommateur.
  - `[low]` `[reject]` Verification gap : aucun test HTTP avec Kafka injoignable (modèle `KafkaInjoignableTest` du cœur bancaire) — carried : même constat et même place que la ligne « aucun test d'intégration de connexion avec Kafka arrêté » du premier passage.
  - `[low]` `[reject]` Intent : jeton émis hors de la transaction de la connexion (lecture étroite) — carried : même constat que LOGIN avant l'émission du jeton.
  - `[low]` `[reject]` Intent : `utilisateur` tronqué à 255 caractères au lieu de l'identifiant saisi intact — ne touche qu'un identifiant qui ne peut désigner aucun compte (`customers.username` ≤ 255) ; la retirer rétablirait le blocage du relais corrigé (high) au premier passage.
  - `[false]` `[reject]` Intent : `/actuator/info` aussi en 401 (lecture large de « tout `/api/**` authentifié ») — l'intention exige l'authentification de `/api/**` et n'interdit pas d'en protéger davantage ; `/actuator/health` reste ouvert pour compose.
  - `[medium]` `[reject]` Intent : lecture BUG-083 « seul le vrai mandataire » non tenue par la pile compose — carried : même constat que la passerelle dans la plage par défaut.
  - `[false]` `[reject]` Intent : « dans la plage » simulé par `127.0.0.0/8` dans `AdresseIpIT` — le résolveur ne dépend pas de la plage configurée ; le chemin par nginx dans la plage réelle est vérifié sur la pile.
  - `[false]` `[reject]` Intent : « Opération annulée » atteinte seulement par injection — la ligne de la matrice définit elle-même le cas par l'annulation de `Transaction.executer`.
  - `[low]` `[reject]` Intent : « un échec Kafka ne change pas la réponse HTTP » vérifié au niveau unitaire seulement — carried : même constat que le test Kafka injoignable.
  - `[false]` `[reject]` Intent : ordre du relais vérifié en unitaire seulement — aucun défaut allégué ; `RelaisOutboxAuditTest` couvre l'ordre et l'arrêt au premier échec, `AuditKafkaIT` la publication réelle.
  - `[false]` `[reject]` Intent : atomicité `evenements_traites` + `audit_logs` prouvée par les ports et non par le cas d'usage — les tests unitaires du cas d'usage montrent les deux écritures dans un seul `transaction.executer`, `ReceptionAuditIT` montre l'annulation des deux sur une vraie base.
  - `[false]` `[reject]` Intent : type ou version inattendus vérifiés en unitaire seulement — aucun défaut allégué ; `RecepteurAuditTest` couvre les deux cas.
  - `[false]` `[reject]` Intent : pas de règle ArchUnit contre les contrôleurs dans `audit` — l'AC le vérifie par `grep`, sorti en 0.
  - `[medium]` `[reject]` Intent : objectif BUG-083 non testé dans la topologie déployée — carried : même constat que la passerelle dans la plage par défaut.

## Design Notes

- **`utilisateur` = `clientId` pour LOGIN** : l'AC dit « identifiant du client » ; AD-18 (spine:288) précise « identifiant saisi pour LOGIN_FAILED, `clientId` sinon » et AR-39 veut une clé numérique quand elle existe. Le legacy écrivait le login : écart voulu de l'architecture (identifiant numérique entre services).
- **Transaction imbriquée** : la connexion n'écrit rien d'autre, mais l'entrée d'audit est consignée *dans* `transaction.executer(...)` par `executerIndependamment(...)`, pour que le test « opération annulée » ait un sens et que les stories suivantes (déconnexion, administration) suivent le même motif.
- **Plage de confiance et port publié** : un appel direct sur `localhost:8081` arrive par la passerelle du réseau compose (dans `172.30.0.0/24`), donc son `X-Forwarded-For` est retenu ; c'est ce qui permet la vérification sur la pile, et un risque limité au développement local (ports de dev publiés), à signaler.
- **Résolveur** : littéral IPv4 par expression (octets ≤ 255) ; IPv6 seulement si la chaîne ne contient que `[0-9a-fA-F:.]` et au moins un `:` avant `InetAddress.getByName` (aucune requête DNS possible) ; appartenance CIDR par comparaison des octets de l'adresse et du préfixe ; IPv4 et IPv6 ne se comparent pas entre eux.

## Verification

**Commands:**
- `scripts/gates/build.sh` -- expected: code 0 (couverture, ArchUnit, front).
- `scripts/gates/check-gates-exist.sh` -- expected: code 0, lignes `audit` à `OK`.
- `make secrets && docker compose -f compose.yaml up -d --build --wait && scripts/gates/trace-smoke.sh` -- expected: code 0.
- `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/audit/` -- expected: non 2xx.
- Connexion de `user1` sur `localhost:${NB_PORT_IDENTITE_CLIENTS:-8081}` avec `X-Forwarded-For: 203.0.113.7` (données de référence chargées à la main), puis `docker compose exec postgres-audit psql -U audit -d audit -c 'select action, details, username, ipaddress, action_date from audit_logs order by id desc limit 3'` -- expected: ligne `LOGIN`, `1`, `203.0.113.7`.
- `scripts/gates/golden-master.sh` -- expected: code 0.
- Les deux `mvn … -Dgroups=` de l'AC -- expected: code 0.
- Les deux `grep` de l'AC nginx/Vite et le `grep` `@RestController|@Controller` sur `services/audit/src/main` -- expected: code 0.

## Auto Run Result

Status: done

**Résumé :** passe de revue complémentaire de la story 2.2. Chaque connexion réussie (`LOGIN`, utilisateur = `clientId`) ou refusée (`LOGIN_FAILED`, utilisateur = identifiant saisi) est écrite par `identite-clients` dans `outbox_audit` par `Transaction.executerIndependamment(...)`, avec l'adresse IP résolue une fois dans `adapter.in.http`, puis publiée sur `identite-clients.audit.v1` ; le service `audit` l'enregistre dans `audit_logs` avec l'horodatage de l'enveloppe. Cette passe corrige un défaut introduit par la troncature du premier passage : un identifiant de plus de 255 caractères commençant par 255 blancs devenait blanc une fois tronqué et la connexion rendait 500 sans entrée d'audit ; il est désormais rogné avant d'être tronqué.

**Fichiers modifiés dans cette passe :**
- `services/identite-clients/src/main/java/fr/nextbanking/identiteclients/domain/EntreeAudit.java` — `tentativeEchouee` rogne l'identifiant saisi avant de le tronquer quand il dépasse 255 caractères.
- `services/identite-clients/src/test/java/fr/nextbanking/identiteclients/domain/EntreeAuditTest.java` — deux cas : 255 blancs + `x` → `x` ; 10 blancs + 300 caractères → 255 caractères.

**Revue :** 32 constats (Blind Hunter 12, Edge Case Hunter 7, Verification Gap 1, Intent Alignment 12). Corrigé : 1 (medium, identifiant blanc après troncature). Reportés : aucun nouveau (épuisement du pool et politique de reprise du consommateur, déjà au frontmatter, reconduits). Rejetés : 30 constats, chacun avec sa raison dans le journal de tri ; 18 reprennent tels quels un verdict du premier passage, les autres sont réfutés (`false`) ou de faible portée.

**Relecture complémentaire recommandée :** non. Passe complémentaire sans constat `high` corrigé ; corrigés par verdict : high 0, medium 1, low 0.

**Vérification :** `EntreeAuditTest` 5/5 ; `scripts/gates/build.sh` 0 ; `check-gates-exist.sh` 0 (lignes `audit` à OK) ; `golden-master.sh` 0 (statique, aucun rejeu) ; les deux `mvn … -Dgroups=` de l'AC à 0 ; greps nginx/Vite et absence de contrôleur dans `services/audit/src/main` à 0 ; pile reconstruite, 12 services sains ; `trace-smoke.sh` 0 ; `curl localhost:8080/api/audit/` → 404 ; sur la pile, identifiant « 255 blancs + x » avec `X-Forwarded-For: 203.0.113.11` → 401 et `audit_logs` : `LOGIN_FAILED | Tentative échouée | x | 203.0.113.11`.

**Risques résiduels :** sur ce poste, la pile tourne avec `NB_SOUS_RESEAU=172.31.250.0/24` et `NB_PORT_COEUR_BANCAIRE=18082` (conflits locaux, non committés) ; une relance de compose sans ces variables a recréé le réseau et laissé un volume Kafka anonyme incohérent : le conteneur `kafka` et son volume anonyme ont été supprimés puis recréés (données Kafka locales seulement). Toujours vrais : un appel direct au port publié 8081 voit son `X-Forwarded-For` retenu (passerelle dans la plage) ; les `.env` antérieurs à la story doivent repasser par `make secrets` ; les deux reports du frontmatter.
