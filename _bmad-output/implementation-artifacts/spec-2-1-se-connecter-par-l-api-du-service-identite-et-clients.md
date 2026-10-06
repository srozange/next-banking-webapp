---
title: "Se connecter par l'API du service identité et clients"
type: 'feature'
created: '2026-10-06'
status: 'awaiting-operator'
baseline_revision: 'f6ffdb47ea3beba04b2e9288c408b15d065c150e'
review_loop_iteration: 0
followup_review_recommended: true
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
warnings: ['oversized']
deferred:
  - summary: >-
      reference-data.sql n'est chargé par aucune commande : sur la pile compose, la table customers reste vide et aucun utilisateur du legacy ne peut se connecter.
    evidence: |-
      Aucun make, init compose ni migration ne charge services/identite-clients/reference-data/reference-data.sql (Blind Hunter, Verification Gap). Le chargement est prévu par `make reset-data` (AR-56, AR-72, golden-master-architecture §5.2), pas encore livré ; la story 2.3 (écran /login) en aura besoin pour une démonstration locale.
    location: >-
      services/identite-clients/reference-data/reference-data.sql ; Makefile
    severity: medium
  - summary: >-
      Le comportement de `make secrets` (idempotence des mots de passe, forme DER des clés, cohérence de la paire) n'a aucun contrôle automatique.
    evidence: |-
      Aucun test ne lance la cible ; build.sh ne lance pas make ; trace-smoke.sh ne lance make secrets que si .env manque. Vérifié à la main (deux lancements, .env identique ; échec simulé d'openssl rand). Il faudrait un petit script de test (hors scripts/gates/, protégé).
    location: >-
      Makefile (cible secrets)
    severity: medium
  - summary: >-
      Un .env antérieur à la story 2.1 n'a pas les variables NB_IDENTITE_CLIENTS_BD_MOT_DE_PASSE ni NB_JETON_CLE_* : identite-clients refuse de démarrer et trace-smoke.sh échoue (« la pile ne démarre pas ») tant que `make secrets` n'est pas relancé.
    evidence: |-
      scripts/gates/trace-smoke.sh:21 ne lance `make secrets` que si .env est absent ; script protégé. Le .env de ce poste a été complété ; les autres postes doivent relancer `make secrets` (inscrit en operator_actions).
    location: >-
      scripts/gates/trace-smoke.sh:21 ; Makefile
    severity: medium
operator_actions:
  - "Trancher le critère Trivy de la story 2.1 : `trivy fs --scanners secret --exit-code 1 .` sort en 1 uniquement à cause de la clé privée .secrets/jeton-prive.pem que `make secrets` doit poser (fichier ignoré par git ; `trivy fs --scanners secret --exit-code 1 --skip-dirs .secrets .` sort en 0). Choisir : exclure .secrets/ par une configuration Trivy versionnée, déplacer la paire RSA hors de l'arbre analysé, ou amender le critère d'acceptation ; l'agent n'a pas le droit de modifier le contrôle."
  - "Sur chaque poste qui a un .env créé avant la story 2.1, relancer `make secrets` (ajoute NB_IDENTITE_CLIENTS_BD_MOT_DE_PASSE, NB_JETON_CLE_PRIVEE et NB_JETON_CLE_PUBLIQUE sans toucher aux mots de passe existants) avant `docker compose up` ou scripts/gates/trace-smoke.sh."
---

<intent-contract>

## Intent

**Problem:** Aucun service ne sait authentifier un utilisateur : `identite-clients` n'existe pas, les mots de passe de démonstration du legacy sont en clair (BUG-002), et rien n'émet ni ne vérifie de jeton. Toutes les stories suivantes dépendent de la connexion.

**Approach:** Créer le service hexagonal `identite-clients` (table `customers`, Flyway, données de référence hachées générées par script), exposer `POST /api/identite-clients/connexion` (JWT RS256 émis par `NimbusJwtEncoder`) et `GET /api/identite-clients/moi`, protéger tout le reste par `spring-boot-starter-security-oauth2-resource-server` avec des erreurs `{"message"}`, et brancher le service dans `compose.yaml` et `make secrets`.

## Boundaries & Constraints

**Always:**
- Hexagone et paquets `fr.nextbanking.identiteclients.{domain,application,adapter.in,adapter.out,infrastructure}` ; `domain` et `application` en Java pur, 100 % lignes et branches ; adaptateurs et infrastructure ≥ 80 %.
- Mots de passe vérifiés par `DelegatingPasswordEncoder` (`{bcrypt}`) ; jamais de mot de passe ni de jeton dans un journal, un `toString()`, une réponse d'erreur ou un fichier versionné.
- Échec indistinct : identifiant inconnu, mauvais mot de passe et compte `active = false` donnent tous 401 `{"message":"Identifiants incorrects"}` ; le mot de passe est comparé même quand l'identifiant est inconnu (empreinte factice) pour ne pas trahir l'existence du compte par le temps de réponse.
- Jeton : `sub` = identifiant numérique du client (`"1"` pour `user1`, AR-36), `roles` = `[role]`, `jti` UUID neuf, `iat`, `exp = iat + nb.jeton.duree-de-vie` (PT30M) ; `iat` lu sur une `java.time.Clock` système dans `adapter.out.jeton` (seul endroit, avec `adapter.out.horloge`, où une `Clock` est permise).
- Chemins publics comparés en entier : `POST /api/identite-clients/connexion` et `GET /actuator/health` (santé compose, hors `/api`) ; un en-tête `Authorization` présenté à la connexion est ignoré (un jeton périmé n'empêche pas de se connecter).
- Sans état : aucune session, aucun `Set-Cookie`, CSRF désactivé (API à jeton porteur).
- Textes d'erreur : seulement ceux du legacy ou de `textes-nouveaux.md` (lot « Stories ») : 400 « Requête invalide » / « Identifiant requis » / « Mot de passe requis », 401 « Authentification requise » / « Identifiants incorrects », 403 « Accès refusé », 404 et 405 « Ressource introuvable », 5xx « Une erreur technique est survenue ».
- Clés du jeton uniquement par variables d'environnement : `NB_JETON_CLE_PRIVEE` (PKCS#8 DER en base64 sur une ligne) et `NB_JETON_CLE_PUBLIQUE` (X.509 DER en base64), dérivées par `make secrets` de la paire PEM de `.secrets/` ; le service refuse de démarrer si l'une manque. Les tests génèrent leur paire en mémoire : aucune clé dans `src/test/resources`.
- Tests JUnit tagués par exigence et par bug (`@Tag("FR-001-06")`, `@Tag("BUG-002")`…) pour `-Dgroups`.

**Never:**
- Audit, adresse IP, renouvellement, déconnexion, contrôle 403 par rôle, publication Kafka, écran `/login` (stories 2.2 à 2.7) ; pas de dépendance Kafka dans ce service pour l'instant.
- Port Horloge pour le jeton ; `Authorization Server`, BFF, passerelle ; verrouillage de compte (RP-05).
- Modifier un fichier protégé (`scripts/gates/**`, `.trivyignore`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, golden master, `.bmad-loop/**`, `.claude/settings.json`, `.claude/hooks/**`, `config/dependency-check-suppressions.xml`), ajouter une configuration Trivy qui exclurait un dossier, ou écrire `sprint-status.yaml`.
- Rejouer le golden master ; afficher ou écrire en clair un mot de passe de démonstration.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Connexion | `{"identifiant":"user1","motDePasse":<bon>}`, `user1` actif | 200 `{"jeton":"<JWT>"}`, sans `Set-Cookie` | — |
| Deux connexions | même requête deux fois | deux `jti` différents | — |
| Inconnu (SC-001-03) | `{"identifiant":"retro-compte-inexistant","motDePasse":"retro-mauvais-mot-de-passe"}` | 401 `{"message":"Identifiants incorrects"}` | réponse identique au mauvais mot de passe |
| Désactivé | bons identifiants, `active = false` | 401 `{"message":"Identifiants incorrects"}` ; 200 après `active = true` | — |
| Champs vides | identifiant vide (ou blanc, ou absent) ; puis mot de passe vide | 400 « Identifiant requis » ; 400 « Mot de passe requis » ; identifiant contrôlé en premier | — |
| JSON invalide | corps non JSON, ou sans `Content-Type` JSON | 400 `{"message":"Requête invalide"}`, seul champ | aucune pile ni nom de classe |
| Profil | `GET /moi` avec jeton de `user1` | 200 `{"identifiant":"user1","nomComplet":"Jean Dupont","roles":["CLIENT"]}` | client absent → 401 « Authentification requise » |
| Sans jeton | `GET /moi`, `GET /connexion`, `POST /connexion/x` | 401 `{"message":"Authentification requise"}` | jeton expiré, mal signé ou illisible : idem |
| Route inconnue | avec jeton, `GET /api/identite-clients/inconnu` | 404 `{"message":"Ressource introuvable"}` | — |

</intent-contract>

## Code Map

- `services/coeur-bancaire/` -- modèle à reproduire : `pom.xml` (starters webmvc, jdbc, flyway, actuator, opentelemetry, datasource-micrometer, testcontainers-postgresql, archunit), `Dockerfile` (COPY de chaque POM du réacteur), `application.properties` (motif de corrélation `traceId`/`spanId`, OTLP, santé seule exposée), `infrastructure/ControleMotDePasseBase` (refus sans mot de passe de base), `infrastructure/Conteneurs` et `ContexteDemarre` (tests), `adapter/out/persistance/TransactionSpring`.
- `services/coeur-bancaire/src/test/java/fr/nextbanking/coeurbancaire/{Hexagonal,Horloge,ClientsHttp}ArchitectureTest.java` -- à recopier avec le paquet `identiteclients` ; l'onion déclare `infrastructure` comme adaptateur : un adaptateur ne dépend ni d'un autre adaptateur ni de l'infrastructure, d'où la configuration de sécurité dans `adapter.in.http` et la lecture des clés séparée dans `adapter.in.http` (publique) et `adapter.out.jeton` (privée).
- `pom.xml` (racine) -- modules ; JaCoCo et profils mutation et security hérités ; Surefire n'inclut pas `*IT.java` par défaut.
- `compose.yaml`, `Makefile`, `.env.example` -- pile et secrets de la story 1.4 ; `make secrets` conserve un `.env` existant en bloc (à rendre idempotent variable par variable, sinon un `.env` d'avant la story n'a jamais les nouvelles variables).
- `frontend/nginx.conf`, `frontend/vite.config.ts` (lecture seule) -- renvoient déjà `/api/identite-clients/**` vers `identite-clients:8080` (port de dev 8081).
- `AppModernizationDemonstrateur/legacy/src/main/resources/import.sql` (dépôt voisin, commit c05512ecab8af8878d695ccd0f33fc3ece8811a4, lecture seule) -- 7 lignes `customers` : 10 `admin` Alice Martin ADMIN ; 11 `sophie` Sophie Durand CONSEILLER ; 12 `marc` Marc Lefevre CONSEILLER ; 1 `user1` Jean Dupont, 2 `user2` Marie Lebrun (conseiller 11) ; 3 `user3` Pierre Bernard, 4 `user4` Lucie Martin (conseiller 12) ; tous actifs ; colonnes `email`, `phone`, `advisor_id`, `advisor_since` (DATE) à reprendre à l'identique, mots de passe exclus.
- `docs/retro/golden-master/replay-config.json` (lecture seule) -- comptes du rejeu : `admin`, `sophie`, `user1` et leurs variables `RETRO_*_PASSWORD`.

## Tasks & Acceptance

**Execution:**
- `pom.xml` -- ajouter le module `services/identite-clients` ; configurer Surefire pour inclure aussi `**/*IT.java` (sinon `*SecuriteIT` ne tourne pas dans `mvn verify`).
- `services/identite-clients/pom.xml`, `Dockerfile` -- module calqué sur coeur-bancaire, sans Kafka, avec `spring-boot-starter-security-oauth2-resource-server` (géré par Boot 4.1.1) ; Dockerfile qui copie les trois POM.
- `services/coeur-bancaire/Dockerfile`, `services/notifications/Dockerfile` -- copier aussi `services/identite-clients/pom.xml` (réacteur complet requis par `-am`).
- `services/identite-clients/src/main/resources/db/migration/V1__customers.sql` -- table `customers` (colonnes du legacy, `username` unique, `role` contraint à CLIENT/CONSEILLER/ADMIN, `advisor_id` vers `customers`, identité `generated by default`).
- `.../domain/` -- `Role`, `Client`, `Identifiants` (validation des champs requis, `toString` sans mot de passe), `ConnexionRefusee` (motifs IDENTIFIANT_REQUIS, MOT_DE_PASSE_REQUIS, IDENTIFIANTS_INCORRECTS).
- `.../application/` -- ports entrants `SeConnecter`, `ConsulterProfil` ; ports sortants `DepotClients`, `MotsDePasse`, `EmetteurJeton` ; cas d'usage `SeConnecterCasUsage` (valide, cherche, compare toujours, refuse inconnu/inactif/faux) et `ConsulterProfilCasUsage`.
- `.../adapter/out/persistance/DepotClientsJdbc`, `.../adapter/out/motdepasse/MotsDePasseSpring`, `.../adapter/out/jeton/EmetteurJetonNimbus` -- JDBC ; `DelegatingPasswordEncoder` avec empreinte factice ; `NimbusJwtEncoder` RS256, `Clock` système, refus de démarrer sans `nb.jeton.cle-privee`.
- `.../adapter/in/http/` -- `PointEntreeConnexion`, `PointEntreeProfil`, `GestionErreurs` (`@RestControllerAdvice`, statut → texte), `ConfigurationSecurite` (chaîne sans état, chemins publics exacts, décodeur depuis `nb.jeton.cle-publique`, résolveur de jeton qui ignore la connexion, réponses 401/403 JSON UTF-8, dispatch d'erreur permis) ; journal « Connexion refusée »/« Connexion réussie » sans secret.
- `.../infrastructure/` -- `ApplicationIdentiteClients`, `ControleMotDePasseBase`, `AssemblageConnexion` ; `application.properties` (`nb.jeton.duree-de-vie=PT30M`, base, OTLP, corrélation).
- `services/identite-clients/src/test/java/.../referencedata/GenerateurReferenceData.java` et `services/identite-clients/reference-data/generer-reference-data.sh` -- générateur (portée test, lancé par `exec:java`) qui écrit `reference-data.sql` : 7 lignes du legacy, mot de passe haché `{bcrypt}` depuis `RETRO_ADMIN_PASSWORD` (admin), `RETRO_CONSEILLER_PASSWORD` (conseillers), `RETRO_CLIENT_PASSWORD` (clients), puis `setval` de l'identité ; variable absente → « variable d'environnement RETRO_… absente », code non nul.
- `services/identite-clients/reference-data/reference-data.sql` -- généré par le script, committé.
- Tests -- unitaires domaine/application (matrice I/O), adaptateurs (jeton, mots de passe, clés, erreurs), `IdentiteClientsSecuriteIT`, `ConnexionIT`, `ReferenceDataIT`, `ApplicationIdentiteClientsTest` (démarrage refusé sans clé privée, sans clé publique, sans mot de passe de base ; `main`), journaux capturés ; trois `*ArchitectureTest`.
- `compose.yaml` -- `postgres-identite-clients` (volume `pgdata-identite-clients`, `pg_isready`) et `identite-clients` (port `${NB_PORT_IDENTITE_CLIENTS:-8081}:8080`, `NB_JETON_CLE_*` et mot de passe de base en `${…:-}`, santé `/actuator/health`) ; `web` dépend de `identite-clients`.
- `Makefile`, `.env.example` -- `make secrets` ajoute chaque variable manquante sans écraser les autres (`NB_IDENTITE_CLIENTS_BD_MOT_DE_PASSE` compris) et réécrit `NB_JETON_CLE_PRIVEE`/`NB_JETON_CLE_PUBLIQUE` depuis `.secrets/` (`openssl … -outform DER | openssl base64 -A`) ; `.env.example` liste les nouvelles variables sans valeur.

**Acceptance Criteria:**
- Given un dépôt sans `.env` ni `.secrets/`, when `make secrets`, then `.env` et la paire RSA de `.secrets/` existent, `git ls-files .env .secrets` est vide et `.env.example` n'a aucune valeur ; relancé, il ne change aucun mot de passe existant.
- Given le contexte Spring démarré sans `nb.jeton.cle-privee`, when il démarre, then il échoue avec un message qui nomme la propriété ; `git grep -nE 'BEGIN (RSA )?PRIVATE KEY'` ne trouve rien.
- Given `reference-data.sql` chargé dans une base Testcontainers, when on lit `customers`, then 7 lignes avec les identifiants du legacy (`admin`, `sophie`, `user1` « Jean Dupont »…), chaque `password` commence par `{bcrypt}` ; et le générateur, lancé avec des mots de passe connus, produit des empreintes qui les vérifient sans les contenir.
- Given `user1` actif, when il se connecte, then le JWT vérifié par la clé publique porte `sub` = `"1"`, `roles` = `["CLIENT"]`, `jti`, `iat`, `exp` = `iat` + 1800 s.
- Given une connexion réussie puis une refusée, when on lit la sortie console capturée, then aucune ligne ne contient le mot de passe ni le jeton, et chaque ligne de journal porte `traceId=` et `spanId=` (identifiant de trace non vide sur les lignes de connexion).
- Given `compose.yaml`, when `docker compose -f compose.yaml config --services` et le décompte des services sans `healthcheck`, then `identite-clients` et `postgres-identite-clients` sont listés et le décompte vaut `0` ; pile démarrée, le `curl` de SC-001-03 sur `localhost:8080` rend 401 `{"message":"Identifiants incorrects"}`.
- Given la story terminée, when `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-001-03,FR-001-04,FR-001-06,FR-001-07,FR-001-08,FR-001-09,FR-030-01,FR-030-08'` puis `-Dgroups='BUG-001,BUG-002,BUG-003,BUG-092'`, then les deux sortent en 0.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 36 findings — high 0, medium 7, low 15, false 14, maybe-false 0
- findings:
  - `[medium]` `[defer]` Blind : reference-data.sql chargé par aucune commande (pile locale sans utilisateurs) — chargement prévu par `make reset-data` (AR-56/AR-72) ; reporté au frontmatter `deferred`.
  - `[false]` `[reject]` Blind : un compte désactivé lit encore /moi avec un jeton déjà émis — décision Q1 d'AD-16 : un jeton émis reste valable jusqu'à son expiration, risque accepté R-5 ; /moi ne rend 401 que pour un client absent.
  - `[false]` `[reject]` Blind : jeton sans `iss` ni `aud` — AD-16 fixe les revendications (sub, roles, jti, iat, exp) et un émetteur unique dont tous les services vérifient la clé publique : comportement voulu.
  - `[low]` `[reject]` Blind : `roles` non converti en autorités Spring — aucun contrôle par rôle dans cette story (story 2.6) ; aucun effet observable aujourd'hui, le convertisseur ajoute de la configuration.
  - `[low]` `[reject]` Blind : pas de contrôle au démarrage que les deux clés forment une paire — `make secrets` dérive les deux de la même paire PEM ; l'écart demande une édition manuelle, le garde ajouterait du code.
  - `[low]` `[reject]` Blind : générateur lancé par `dependency:build-classpath` + `java` au lieu d'`exec:java` — écart voulu (version d'exec-maven-plugin absente de versions-verifiees.md) ; la seule correction serait d'éditer la spec. Noté dans le résultat.
  - `[medium]` `[patch]` Blind : rien ne vérifie que le reference-data.sql committé correspond aux vraies variables RETRO_* — ajout de `ReferenceDataIT.le_fichier_committe_a_ete_genere_avec_les_variables_retro`, actif si les trois variables sont posées ; passé avec les valeurs de démonstration du legacy.
  - `[low]` `[patch]` Blind : tests de refus de démarrage sensibles aux variables exportées — les tests « sans » passent une valeur vide explicite.
  - `[low]` `[patch]` Blind : test « clé privée vide » en doublon — remplacé par un test de clé illisible qui vérifie que la valeur n'est pas reprise dans la trace.
  - `[false]` `[reject]` Blind : mot de passe de plus de 72 octets → 500 ou faux avertissement — essayé sur la pile : 100 caractères donnent 401 « Identifiants incorrects » pour un compte inconnu comme pour user1, sans erreur au journal.
  - `[low]` `[reject]` Blind : deux tests de comportement public sans tag — aucune exigence de la liste ne porte ces cas ; ils tournent dans `mvn verify` ; aucun effet.
  - `[low]` `[reject]` Blind : corps 401/403 construit par concaténation — textes constants sans guillemet ; risque seulement pour un futur texte, correctif plus large qu'une correction directe.
  - `[low]` `[patch]` Blind : `make secrets` annonce « générée » si `openssl rand` échoue — valeur capturée, vide → message et `exit 1` ; testé à la main avec un faux openssl.
  - `[false]` `[reject]` Edge : /moi d'un compte désactivé — même refutation que ci-dessus (AD-16, Q1, R-5).
  - `[low]` `[reject]` Edge : clés de paires différentes — même raison que le constat Blind correspondant.
  - `[low]` `[reject]` Edge : clé RSA de moins de 2048 bits → 500 à la première connexion — `make secrets` génère 2048 bits ; cas improbable, garde supplémentaire.
  - `[low]` `[reject]` Edge : `nb.jeton.duree-de-vie` nulle ou négative — valeur fixée dans application.properties, aucune variable d'environnement ne la change aujourd'hui.
  - `[low]` `[reject]` Edge : tolérance de 60 s du validateur de dates par défaut — tolérance standard de Spring Security, négligeable devant 30 min.
  - `[low]` `[reject]` Edge : `roles` non converti — même raison que le constat Blind (story 2.6).
  - `[low]` `[reject]` Edge : identifiant avec espaces autour refusé — comportement classique, aucun texte du legacy n'indique un rognage ; correctif non demandé.
  - `[low]` `[patch]` Edge : `openssl rand` en échec dans `make secrets` — même correctif que le constat Blind.
  - `[low]` `[patch]` Edge : tests de refus sensibles à l'environnement — même correctif que le constat Blind.
  - `[low]` `[reject]` Edge : écart exec:java — même raison que le constat Blind.
  - `[medium]` `[patch]` Verification gap : rien ne prouve que le client inconnu passe par une vraie comparaison bcrypt — constructeur package-private avec encodeur, test à encodeur compteur (`matches` appelé une fois sur l'empreinte factice).
  - `[medium]` `[defer]` Verification gap : `make secrets` sans contrôle automatique — pas de banc de test shell, scripts/gates protégé ; reporté.
  - `[medium]` `[defer]` Verification gap (autre) : un .env antérieur fait échouer trace-smoke.sh (script protégé qui ne relance pas `make secrets`) — reporté et inscrit en operator_actions.
  - `[medium]` `[defer]` Verification gap (autre) : reference-data.sql non chargé sur la pile — même entrée que le premier constat Blind.
  - `[false]` `[reject]` Intent : rien n'est committé ni finalisé — la revue précède la finalisation ; commit et frontmatter faits à la finalisation.
  - `[medium]` `[defer]` Intent : critère Trivy remplacé par une variante `--skip-dirs .secrets` — contradiction du critère (la clé privée exigée dans .secrets/ fait sortir Trivy en 1) ; décision humaine inscrite en operator_actions, aucun contrôle modifié.
  - `[medium]` `[patch]` Intent : correspondance du fichier committé avec les vraies valeurs RETRO_* non vérifiée — même correctif que le constat Blind (test conditionnel, passé).
  - `[false]` `[reject]` Intent : `Set-Cookie` sur `GET /login` non vérifié — `curl -sI http://localhost:8080/login` : aucun en-tête Set-Cookie.
  - `[low]` `[reject]` Intent : refus de démarrage testé par la ligne de commande et non par la variable, absence de clé versionnée non testée — le binding souple rend les deux équivalents ; `git grep -nE 'BEGIN (RSA )?PRIVATE KEY'` vide (vérifié).
  - `[false]` `[reject]` Intent : compose et curl par 8080 non couverts — vérifiés pendant la vérification : `config --services` liste les deux services, 0 sans healthcheck, curl SC-001-03 → 401 « Identifiants incorrects ».
  - `[false]` `[reject]` Intent : ambiguïté de `sub` — AR-36 et AD-16 fixent `clientId = sub` : identifiant numérique, conforme.
  - `[false]` `[reject]` Intent : `iat`/`exp` vérifiés seulement à 60 s près en intégration — le test unitaire de l'émetteur fige la Clock et vérifie `iat` exact et `exp - iat` = 1800 s.
  - `[false]` `[reject]` Intent : traceId non vide exigé seulement sur les lignes de connexion — les autres lignes horodatées hors requête n'ont pas de trace par nature ; le critère demande que chaque ligne porte les champs, ce que le test vérifie.

## Design Notes

- **Clés par variables d'environnement, pas par fichier monté** : le conteneur tourne sous l'uid 10001 et `.secrets/*.pem` est en mode 600 pour l'utilisateur du poste ; un montage serait illisible. Les PEM de `.secrets/` restent la source, `.env` porte leur forme DER base64 (une ligne, compatible `.env`). Lecture : `KeyFactory.getInstance("RSA")` sur `PKCS8EncodedKeySpec` / `X509EncodedKeySpec` après `Base64.getMimeDecoder()` ; les lignes `-----BEGIN/END …-----` éventuelles sont retirées, pour accepter aussi un PEM collé.
- **Mots de passe des quatre comptes sans variable** (`marc`, `user2`, `user3`, `user4`) : la variable de leur rôle (conseiller ou client). Les trois comptes du rejeu gardent leur variable ; tous les comptes restent utilisables en démonstration ; aucun mot de passe du legacy n'entre dans le dépôt.
- **Génération du fichier committé** : les variables `RETRO_*` ne sont pas posées dans l'environnement de l'agent ; elles valent par construction les mots de passe de démonstration du legacy (le golden master s'y est connecté). Le script est lancé avec ces valeurs exportées dans le seul shell de génération, sans les afficher ni les écrire.
- **Contrôle Trivy de l'AC `make secrets`** : `trivy fs --scanners secret --exit-code 1 .` analyse aussi les fichiers ignorés par git ; la clé privée PEM que `make secrets` doit poser dans `.secrets/` (exigée par le même critère) le fait sortir en 1 (`AsymmetricPrivateKey`, déjà le cas depuis la story 1.4). Sans `.secrets/`, il sort en 0. Exclure le dossier serait modifier le contrôle : décision laissée à l'humain (operator_actions).

## Verification

**Commands:**
- `scripts/gates/build.sh` -- expected: code 0 (couverture, ArchUnit, front).
- `scripts/gates/check-gates-exist.sh` -- expected: code 0, lignes `identite-clients` à `OK`.
- `make secrets && docker compose -f compose.yaml up -d --build --wait && scripts/gates/trace-smoke.sh` -- expected: code 0.
- `curl -s -w ' %{http_code}' http://localhost:8080/api/identite-clients/connexion -H 'Content-Type: application/json' -d '{"identifiant":"x","motDePasse":"y"}'` -- expected: `{"message":"Identifiants incorrects"} 401`.
- `scripts/gates/golden-master.sh` -- expected: code 0.
- `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups=…` (les deux listes de l'AC) -- expected: code 0.
- `git ls-files .env .secrets` -- expected: vide ; `grep -c . .env.example` sans `=` suivi d'une valeur.
- `trivy fs --scanners secret --exit-code 1 --skip-dirs .secrets .` -- expected: code 0 (aucun secret versionné ni ailleurs hors `.secrets/`).

## Auto Run Result

Status: awaiting-operator

**Résumé :** nouveau service hexagonal `identite-clients` : `POST /api/identite-clients/connexion` (JWT RS256 par `NimbusJwtEncoder`, `sub` = identifiant numérique, `roles`, `jti`, `iat`, `exp` = `iat` + 30 min, `Clock` système dans `adapter.out.jeton`) et `GET /api/identite-clients/moi` ; tout le reste authentifié (chemins publics exacts : connexion et `/actuator/health`), sans session ni cookie ; erreurs `{"message"}` aux textes du legacy et de `textes-nouveaux.md` ; mots de passe `{bcrypt}`, refus indistinct (inconnu, mauvais mot de passe, désactivé) avec comparaison factice ; refus de démarrer sans clé privée, clé publique ou mot de passe de base. Données de référence : 7 utilisateurs du legacy, générés par `reference-data/generer-reference-data.sh` depuis `RETRO_*_PASSWORD` (variable du rôle pour `marc`, `user2` à `user4`).

**Fichiers :**
- `pom.xml` — module ajouté ; Surefire inclut `*IT.java`.
- `services/identite-clients/**` — service (POM, Dockerfile, migration `V1__customers.sql`, domaine, application, adaptateurs, infrastructure, `application.properties`), tests (unitaires, `ConnexionIT`, `IdentiteClientsSecuriteIT`, `ReferenceDataIT`, trois `*ArchitectureTest`), générateur et `reference-data.sql`.
- `services/coeur-bancaire/Dockerfile`, `services/notifications/Dockerfile` — copient aussi le POM d'identite-clients.
- `compose.yaml` — `postgres-identite-clients` et `identite-clients` (8081, healthchecks), `web` en dépend.
- `Makefile` — `make secrets` idempotent variable par variable, clés DER base64 dérivées de `.secrets/`.
- `.env.example`, `.gitignore` — nouvelles variables sans valeur ; `.env.tmp` ignoré.

**Revue :** 36 constats ; 6 entrées corrigées (2 medium : comparaison factice prouvée par un encodeur compteur, test conditionnel du fichier committé contre les vraies variables ; 4 low : tests de démarrage indépendants de l'environnement, doublon remplacé par un test de clé illisible, échec d'`openssl rand` dans `make secrets`, plus le littéral de test qui reprenait par hasard le mot de passe de démonstration de `user1`, corrigé avant la revue) ; 4 reports (chargement des données de référence, contrôle de `make secrets`, `.env` antérieur, critère Trivy) ; rejets motivés dans le journal de tri ci-dessus.

**Relecture complémentaire recommandée :** oui (premier passage, deux entrées `medium` corrigées, aucune `high` ; corrigées par entrée : high 0, medium 2, low 4). Risque non vérifié nommé : le test qui confronte `reference-data.sql` aux vraies variables `RETRO_*` est ignoré dans `scripts/gates/build.sh` (variables absentes) ; la correspondance du fichier committé n'a été prouvée qu'une fois, sur ce poste.

**Vérification :** `scripts/gates/build.sh` 0 ; `scripts/gates/check-gates-exist.sh` 0, aucun KO, lignes identite-clients à OK ; `scripts/gates/golden-master.sh` 0 ; les deux `mvn … -Dgroups=` de l'AC 0 ; `scripts/gates/trace-smoke.sh` 0 ; curl SC-001-03 et `{"identifiant":"x",…}` par `localhost:8080` → 401 `{"message":"Identifiants incorrects"}` ; `docker compose config` : deux services listés, 0 sans healthcheck ; `git ls-files .env .secrets` vide ; `.env.example` sans valeur ; `git grep` de clé privée vide ; `trivy fs --scanners secret --exit-code 1 --skip-dirs .secrets .` 0, sans `--skip-dirs` 1 (clé de `.secrets/`, operator_actions) ; connexion de `user1` avec le mot de passe de démonstration sur la pile (reference-data chargé à la main) : 200 ; aucun mot de passe de démonstration du legacy dans les fichiers du service.

**Risques résiduels :** critère Trivy en attente de décision humaine ; données de référence non chargées automatiquement avant `make reset-data` ; sur ce poste, la pile locale tourne avec `NB_SOUS_RESEAU=172.31.30.0/24` et `NB_PORT_COEUR_BANCAIRE=18082` (réseau 172.30.0.0/16 et port 8082 pris par d'autres projets), ce qui touchera aussi la plage de confiance supposée par la story 2.2 ; les mots de passe de démonstration du legacy ont servi à générer `reference-data.sql` (exportés dans un shell, jamais écrits) ; `docs/retro/00-global/PERMISSIONS.md` (corpus existant) contient déjà le mot de passe de démonstration d'`admin`, ce qui fera échouer un futur `git grep "$RETRO_ADMIN_PASSWORD"` (story 10.2).
