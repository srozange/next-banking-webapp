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
