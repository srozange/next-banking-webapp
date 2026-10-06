- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: `scripts/gates/build.sh` ne vérifie pas les types du front (`tsc --noEmit`), une erreur de type passe tous les contrôles.
  evidence: le script lance seulement `npm ci`, depcruise et `vitest run --coverage` ; Vitest retire les types sans les vérifier. Script protégé : décision humaine avant l'empreinte (étape 10 du pilote).
- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: `.gitignore` ne couvre pas `.env` ni `.secrets/`, générés par `make secrets` et non versionnés selon AGENTS.md.
  evidence: risque de commit de secrets ; à ajouter au plus tard avec la story qui crée `make secrets` (1.4).
- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: aucune version de Node ni de Maven n'est imposée (`engines`, wrapper), le poste a Node 24.14.0 et Maven 3.9.14 pour 24.21.0 et 3.9.16 retenues.
  evidence: `versions-verifiees.md` ; `node -v`, `mvn -v` sur le poste.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: `frontend/index.html` porte un titre inventé ; le legacy impose `BankApp Demo - {titre de page}`.
  evidence: `docs/retro/00-global/MESSAGES.md` §1.1 ; à régler par la première story d'écran (2.3).
- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: le TODO d'AGENTS.md « commandes de build et de test unitaire par module » est à reporter (refresh de bmad-project-context).
  evidence: `mvn -B -q verify -DfailIfNoTests=true` ; `cd frontend && npm ci && npx vitest run && npm run typecheck`.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-rendre-l-architecture-hexagonale-verifiable-et-bloquante.md`
  summary: seules la règle onion (`canary-arch.sh`) et les règles dont la story a fait un test négatif à la main ont prouvé qu'elles mordent ; aucun canari automatique pour Horloge, clients HTTP et dependency-cruiser.
  evidence: une règle affaiblie plus tard resterait verte tant qu'aucune violation réelle n'existe. Scripts protégés : décision humaine avant l'empreinte (étape 10 du pilote).
- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-rendre-l-architecture-hexagonale-verifiable-et-bloquante.md`
  summary: élargir les interdits du temps (`System.nanoTime`, `new java.util.Date()`, `Calendar.getInstance`, `ZoneId.systemDefault`, `TimeZone.getDefault`, `InstantSource` ; front : `Intl.DateTimeFormat().resolvedOptions().timeZone`) et des clients HTTP (`RestOperations`, `HttpURLConnection`, interfaces HTTP Spring, OkHttp, Apache HttpClient).
  evidence: AD-19, AD-4 et AD-23 fixent des listes plus courtes ; chaque ajout est une décision d'architecture (spine), puis un ajout de règle dans les tests `*ArchitectureTest`.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-2-rendre-l-architecture-hexagonale-verifiable-et-bloquante.md`
  summary: `frontend/.dependency-cruiser.cjs` ne signale ni les imports non résolus (`unknown`, `undetermined`, `couldNotResolve`) ni les cycles (`no-circular`).
  evidence: AD-2 fixe trois règles et la liste des types ; le fichier devient protégé à l'empreinte : à trancher par l'humain avant l'étape 10.

### DW-1: Aucun canari automatique ne prouve que les seuils JaCoCo et Vitest mordent encore : une faute dans un include ou un glob les désactiverait sans alerte.
origin: spec-deferred 6a1bfb56b92c
location: scripts/gates/
source_spec: `spec-1-3-imposer-la-couverture-par-couche.md`
severity: medium
reason: check-gates-exist.sh ne fait que des grep (jacoco-maven-plugin, <goal>check</goal>, <minimum>…, thresholds, src/domain) ; `<include>*.domaine*</include>` ou `'src/domain/*.tsx'` laissent tout vert. Les tests négatifs de la story sont manuels. Correctif : un canary-coverage.sh calqué sur canary-arch.sh, sous scripts/gates/ (protégé) : décision humaine avant l'empreinte (étape 10 du pilote).
status: open

### DW-2: Un fichier du front hors des quatre dossiers de l'hexagone (ex. src/outils.ts) est compté par coverage.include mais aucun seuil ne s'y applique.
origin: spec-deferred 0535d008019f
location: frontend/vite.config.ts
source_spec: `spec-1-3-imposer-la-couverture-par-couche.md`
severity: low
reason: Les seuils Vitest ne portent que sur les quatre globs de couche. Côté back, ensureAllClassesAreContainedInArchitecture refuse déjà un paquet hors couche ; côté front, rien n'interdit un dossier hors couche (dependency-cruiser, protégé). À trancher : règle dependency-cruiser ou seuil global de repli.
status: open

### DW-3: Si un futur POM fixe `argLine` dans Surefire sans `@{argLine}`, l'agent JaCoCo disparaît, `jacoco.exec` n'est plus écrit et `check` saute en restant vert.
origin: spec-deferred 750a02c74c16
location: pom.xml (jacoco-maven-plugin, prepare-agent)
source_spec: `spec-1-3-imposer-la-couverture-par-couche.md`
severity: medium
reason: prepare-agent passe par la propriété argLine ; aucun POM ne la fixe aujourd'hui (grep argLine vide), donc pas de défaut actuel. Mais check-gates-exist.sh ne lit que le POM effectif (plugin, goal check, minimum) et resterait OK ; JaCoCo journalise « Skipping JaCoCo execution due to missing execution data file » et sort en 0. À couvrir par le canari de couverture déjà reporté (scripts/gates/, protégé) : vérifier aussi la présence de target/jacoco.exec.
status: open

### DW-4: Les règles de routage du conteneur web (nginx) et du proxy Vite n'ont aucune vérification automatique en dehors du POST témoin de coeur-bancaire.
origin: spec-deferred c370b9856cac
location: frontend/nginx.conf ; frontend/vite.config.ts
source_spec: `spec-1-4-faire-traverser-les-deux-services-par-un-flux-temoin-trace.md`
severity: low
reason: trace-smoke.sh n'envoie que POST /api/coeur-bancaire/temoin. Retirer le bloc /api/audit de nginx.conf, passer X-Forwarded-For à $proxy_add_x_forwarded_for, ou retirer le bypass du proxy Vite ne ferait échouer aucun contrôle. Les curl /tableau-de-bord (200) et /api/audit/ (404) des critères sont manuels. Piste : un test Vitest qui importe vite.config.ts et vérifie server.proxy (bypass compris) ; côté nginx, une porte au niveau de la pile (scripts/gates/, protégé : décision humaine).
status: open

### DW-5: AGENTS.md porte encore « TODO après la story 1.4 : ajouter scripts/gates/trace-smoke.sh à la liste ci-dessus ».
origin: spec-deferred e53d5257b768
location: AGENTS.md (section Running and verifying)
source_spec: `spec-1-4-faire-traverser-les-deux-services-par-un-flux-temoin-trace.md`
severity: low
reason: La liste « Avant de déclarer un travail terminé » d'AGENTS.md ne cite pas trace-smoke.sh. Fichier de contexte des agents : modification réservée (report obligatoire), à faire au prochain rafraîchissement (bmad-project-context).
status: open

### DW-6: Les surcharges de sécurité tomcat.version, jackson-bom.version et kotlin.version du POM parent n'ont aucun déclencheur de retrait.
origin: spec-deferred 8ed597c2f7aa
location: pom.xml:30-33
source_spec: `spec-1-6-bloquer-les-dependances-vulnerables.md`
severity: medium
reason: Seuls un commentaire du POM et versions-verifiees.md disent de les retirer quand Boot embarque Tomcat >= 11.0.25, Jackson >= 3.1.7 et Kotlin >= 2.4.20. Lors d'une montée de Boot, ces surcharges figées pourraient rétrograder une version plus récente gérée par le BOM, sans qu'aucun contrôle ne le signale.
status: open

### DW-7: reference-data.sql n'est chargé par aucune commande : sur la pile compose, la table customers reste vide et aucun utilisateur du legacy ne peut se connecter.
origin: spec-deferred 2df70ae16762
location: services/identite-clients/reference-data/reference-data.sql ; Makefile
source_spec: `spec-2-1-se-connecter-par-l-api-du-service-identite-et-clients.md`
severity: medium
reason: Aucun make, init compose ni migration ne charge services/identite-clients/reference-data/reference-data.sql (Blind Hunter, Verification Gap). Le chargement est prévu par `make reset-data` (AR-56, AR-72, golden-master-architecture §5.2), pas encore livré ; la story 2.3 (écran /login) en aura besoin pour une démonstration locale.
status: open

### DW-8: Le comportement de `make secrets` (idempotence des mots de passe, forme DER des clés, cohérence de la paire) n'a aucun contrôle automatique.
origin: spec-deferred 142d9f67ffb1
location: Makefile (cible secrets)
source_spec: `spec-2-1-se-connecter-par-l-api-du-service-identite-et-clients.md`
severity: medium
reason: Aucun test ne lance la cible ; build.sh ne lance pas make ; trace-smoke.sh ne lance make secrets que si .env manque. Vérifié à la main (deux lancements, .env identique ; échec simulé d'openssl rand). Il faudrait un petit script de test (hors scripts/gates/, protégé).
status: open

### DW-9: Un .env antérieur à la story 2.1 n'a pas les variables NB_IDENTITE_CLIENTS_BD_MOT_DE_PASSE ni NB_JETON_CLE_* : identite-clients refuse de démarrer et trace-smoke.sh échoue (« la pile ne démarre pas »)
origin: spec-deferred 99c0a025169c
location: scripts/gates/trace-smoke.sh:21 ; Makefile
source_spec: `spec-2-1-se-connecter-par-l-api-du-service-identite-et-clients.md`
severity: medium
reason: scripts/gates/trace-smoke.sh:21 ne lance `make secrets` que si .env est absent ; script protégé. Le .env de ce poste a été complété ; les autres postes doivent relancer `make secrets` (inscrit en operator_actions).
status: open

### DW-10: Une connexion tient deux connexions du pool à la fois (transaction de la connexion, puis executerIndependamment en REQUIRES_NEW) : au-delà de 10 connexions simultanées (pool Hikari par défaut), les
origin: spec-deferred 7054a18b9e87
location: services/identite-clients/src/main/java/fr/nextbanking/identiteclients/application/SeConnecterCasUsage.java ; adapter/out/persistance/TransactionSpring.java
source_spec: `spec-2-2-tracer-les-connexions-dans-le-journal-d-audit-avec-l-adresse-ip-d-origine.md`
severity: medium
reason: DataSourceTransactionManager prend la connexion dès l'ouverture de la transaction externe, qui reste tenue pendant la comparaison bcrypt ; REQUIRES_NEW en demande une seconde. L'imbrication est imposée par AR-40/AD-18 (et par le critère « opération annulée ») : la parade (pool dédié à l'audit, dimensionnement, borne de concurrence) est une décision d'architecture qui vaut pour tous les services émetteurs d'audit (Blind Hunter, Edge Case Hunter, Verification Gap).
status: open

### DW-11: Le consommateur audit garde le gestionnaire d'erreurs Kafka par défaut (10 essais sans délai puis abandon) : pendant une panne de sa base de plus de quelques minutes, des entrées d'audit sont perdues
origin: spec-deferred 743a6bbb73c0
location: services/audit/src/main/java/fr/nextbanking/audit/adapter/in/evenements/RecepteurAudit.java ; services/audit/src/main/resources/application.properties
source_spec: `spec-2-2-tracer-les-connexions-dans-le-journal-d-audit-avec-l-adresse-ip-d-origine.md`
severity: medium
reason: Aucun DefaultErrorHandler configuré ; chaque essai attend le délai Hikari (30 s), puis l'offset est validé. Même motif que le consommateur notifications (story 1.4) : la politique de reprise (attente exponentielle, topic de rejet, exceptions non rejouables) est à décider pour tous les consommateurs (Edge Case Hunter).
status: open

### DW-12: Valeurs des lignes LOANS / LOAN_REPAYMENTS du legacy (import.sql) absentes du dépôt : reference-data.sql de credits est reconstitué (2 prêts EN_COURS de user1, 18 échéances) et à confirmer contre le
origin: spec-deferred 09799692e09b
location: n/a
source_spec: `spec-5-1-ouvrir-la-page-prets-et-simuler-un-emprunt.md`
reason: docs/retro/00-global/NFR.md (2 loans, 18 loan_repayments), DATA-DICTIONARY.md (colonnes), FIELDS.md (exemple 5 000 EUR / 4.9 % / 24 mois).
status: open

### DW-13: Prérequis de l'epic 4 (FR-004-05, « Montant invalide ») non livré : le message est livré ici pour la seule simulation ; la création de virement programmé et la demande de prêt (5.2) le reprendront.
origin: spec-deferred 93fe4e36160d
location: n/a
source_spec: `spec-5-1-ouvrir-la-page-prets-et-simuler-un-emprunt.md`
reason: sprint-status.yaml : epic 4 en backlog.
status: open
