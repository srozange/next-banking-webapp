# Décisions à trancher

Mis à jour le 2026-10-05, après les stories 1.1 et 1.2 et l'installation de bmad-loop. Chaque décision indique quand elle doit être prise, les options, et la recommandation. Une décision tranchée se reporte dans le document cité (spine, `docs/quality-gates.md`, story), puis se retire d'ici.

## Avant de lancer la boucle sur l'epic 1 (ce soir ou demain matin)

### D1. Lancer `bmad-loop run --epic 1` sans le hook de protection des contrôles

La boucle lance ses agents en `--permission-mode bypassPermissions`. Le hook qui refuse les écritures sur `scripts/gates/**`, `docs/quality-gates.md` et les autres fichiers protégés n'arrive qu'à l'étape 11, après l'epic 1. Pendant les stories 1.3 à 1.6, la protection repose seulement sur les consignes d'`AGENTS.md` et sur la revue.

- **A. Lancer tel quel (prévu par le pilote).** Contrôle a posteriori : `git diff --name-only` sur les fichiers protégés après le run.
- **B. Avancer le hook (étape 11) avant le run.** Plus sûr, mais la story 1.4 doit créer `scripts/gates/trace-smoke.sh`, que le hook refuserait.

Recommandation : A, avec le contrôle `git diff` à la fin du run.

### D2. Story 1.6 (bloquer les dépendances vulnérables) : prérequis humains

La story a besoin de la clé NVD dans `~/.m2/settings.xml`, de Trivy 0.75.0 (poste en 0.74.0) et de bases Trivy et Dependency-Check à jour. La boucle ne peut pas les fournir et s'arrêtera sur cette story.

- **A. Préparer le poste avant le run** (mise à jour de Trivy, rafraîchissement des bases).
- **B. Lancer la boucle sur 1.3 à 1.5 seulement** (`--max-stories 3`), faire la 1.6 après.

Recommandation : B si le run démarre avant que le poste soit prêt.

### D3. `.bmad-loop/policy.toml` ignoré par git

`bmad-loop init` a ajouté `.bmad-loop/policy.toml` au `.gitignore`. Le pilote prévoit de le figer dans l'empreinte (étape 10), donc de le versionner.

- **A. Le versionner** (retirer la ligne du `.gitignore`). La configuration de la boucle est partagée et protégée. Inconvénient : la TUI y écrit la taille de ses panneaux (`[tui]`), ce qui produit des diffs.
- **B. Le laisser local.** L'empreinte le couvre quand même sur ce poste, mais la configuration n'est pas dans le dépôt.

Recommandation : A.

## Avant de figer l'empreinte des contrôles (étape 10, fin de l'epic 1)

Ces fichiers deviennent intouchables pour les agents après l'empreinte : tout élargissement se décide avant.

### D4. Élargir les interdits du temps

Aujourd'hui (AD-19, AD-23) : `now(..)` des types `java.time` (appels et références de méthode), `java.time.Clock`, `System.currentTimeMillis` ; côté front `Date.now()`, `new Date()`, `Temporal.Now`. Restent possibles sans échec : `System.nanoTime`, `new java.util.Date()`, `Calendar.getInstance`, `ZoneId.systemDefault()`, `TimeZone.getDefault()`, `InstantSource` ; côté front `Intl.DateTimeFormat().resolvedOptions().timeZone`.

- **A. Tout ajouter.** Les deux qui lisent le fuseau du poste (`ZoneId.systemDefault`, `TimeZone.getDefault`) contredisent directement « fuseau Europe/Paris, jamais lu sur TZ ».
- **B. Ajouter seulement les lectures de fuseau.**
- **C. Ne rien changer.**

Recommandation : A. Mise à jour d'AD-19 et AD-23 dans la spine, puis des tests `HorlogeArchitectureTest` et `architecture-temps.test.ts`.

### D5. Élargir les clients HTTP interdits hors de `adapter.out.fournisseur`

Aujourd'hui (AD-4) : `RestClient`, `WebClient`, `RestTemplate`, `java.net.http.HttpClient`. Restent possibles : `RestOperations`, `HttpURLConnection` / `URL.openConnection`, interfaces HTTP Spring (`HttpServiceProxyFactory`, `@HttpExchange`), OkHttp, Apache HttpClient, OpenFeign.

- **A. Tout ajouter** (par nom qualifié, pas de dépendance nouvelle).
- **B. Ne rien changer.**

Recommandation : A. Mise à jour d'AD-4, puis de `ClientsHttpArchitectureTest`.

### D6. dependency-cruiser : imports non résolus et cycles

`frontend/.dependency-cruiser.cjs` ne signale pas un import non résolu (paquet absent, alias faux) dans `src/domain` ou `src/application`, ni les cycles.

- **A. Ajouter `unknown` et `undetermined` aux types interdits, une règle `couldNotResolve`, et `no-circular`.**
- **B. Ne rien changer** (le typecheck, désormais dans `build.sh`, attrape les imports non résolus).

Recommandation : A pour `no-circular` ; B suffit pour les imports non résolus depuis l'ajout de `tsc --noEmit`.

### D7. Canaris pour les autres règles

`canary-arch.sh` ne prouve que la règle domaine → adaptateur. Les règles Horloge, clients HTTP et dependency-cruiser n'ont été prouvées qu'à la main, une fois. Si une règle s'affaiblit plus tard, rien ne le verra tant qu'aucune violation réelle n'existe.

- **A. Étendre `canary-arch.sh`** (une violation plantée par règle).
- **B. Un script `canary-front.sh`** pour dependency-cruiser et le test du temps.
- **C. Ne rien ajouter** (tests à la main consignés dans `docs/tests-des-controles.tsv`).

Recommandation : A et B, préparés par l'agent dans un dossier non suivi, copiés par vous.

## Environnement

### D8. Versions de Maven et de Node imposées

Retenues : Maven 3.9.16, Node 24.21.0. Poste : Maven 3.9.14, Node 24.14.0. Rien ne l'impose aujourd'hui.

- **A. Wrapper Maven (`mvnw`) et `engines` + `.nvmrc` côté front.** Les images Docker de la story 1.4 suivront les mêmes versions.
- **B. Mettre à jour le poste seulement.**

Recommandation : A, à porter par la story 1.4 (conteneurs).

### D9. Pousser `main`

`main` a des commits non poussés. Les critères des stories comparent les fichiers protégés à `origin/main` : tant que `main` n'est pas poussé, la comparaison remonte toute la planification et ne prouve rien.

- **A. Pousser `main`.**
- **B. Ne pas pousser** ; les specs comparent depuis leur `baseline_commit`.

Recommandation : A.

## Métier

### D10. Expiration de session sur inactivité (avant la story 2.5)

Le legacy expire la session après 30 minutes d'inactivité (NFR-SEC-07). Le jeton JWT de la cible n'a pas de notion d'inactivité : il faut choisir un mécanisme (renouvellement glissant du jeton, ou minuterie côté front qui déconnecte). Bloque la story 2.5 (se déconnecter et retrouver la connexion quand la session expire).

### D11. Police Graphik (avant la story 2.3)

Licence d'auto-hébergement à confirmer ; sinon Inter seule. La story 2.3 (écran de connexion au thème Demo Bank) livre le thème.
