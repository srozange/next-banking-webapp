---
title: 'Bloquer les dépendances vulnérables'
type: 'chore'
created: '2026-10-06'
status: done
baseline_revision: '95cc18cf78f376f0293c2c2eba904b813ad32157'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
warnings: ['oversized']
deferred:
  - summary: >-
      Les surcharges de sécurité tomcat.version, jackson-bom.version et kotlin.version du POM parent n'ont aucun déclencheur de retrait.
    evidence: |-
      Seuls un commentaire du POM et versions-verifiees.md disent de les retirer quand Boot embarque Tomcat >= 11.0.25, Jackson >= 3.1.7 et Kotlin >= 2.4.20. Lors d'une montée de Boot, ces surcharges figées pourraient rétrograder une version plus récente gérée par le BOM, sans qu'aucun contrôle ne le signale.
    location: >-
      pom.xml:30-33
    severity: medium
operator_actions:
  - "Valider ou refuser les trois surcharges de version de sécurité ajoutées au POM parent par la story 1.6 (tomcat.version 11.0.25, jackson-bom.version 3.1.7, kotlin.version 2.4.20), inscrites dans versions-verifiees.md en attente de validation : sans elles, scripts/gates/cve.sh sort en 1 sur Spring Boot 4.1.1 ; si refus, choisir l'alternative (autre version, attendre Boot 4.1.2) et relancer cve.sh."
---

<intent-contract>

## Intent

**Problem:** `scripts/gates/cve.sh` (protégé, déjà présent) lance Trivy puis `mvn -Psecurity org.owasp:dependency-check-maven:aggregate`, mais le profil `security` n'existe pas : Dependency-Check n'est pas configuré et `check-gates-exist.sh` affiche encore sept `KO` (quatre par service), les derniers de l'epic 1.

**Approach:** Déclarer `dependency-check-maven` 13.0.0 uniquement dans un profil Maven `security` du POM parent, configuré comme l'exige `docs/quality-gates.md`, pour que `cve.sh` sorte en 0 sur le dépôt et que les violations volontaires (dépendance Java CRITICAL, paquet npm HIGH, `<nvdApiKey>` dans le POM) fassent échouer le contrôle concerné.

## Boundaries & Constraints

**Always:**
- Version exacte `org.owasp:dependency-check-maven` 13.0.0, déclarée seulement dans `<profiles><profile><id>security</id>` du POM parent (configuration au niveau du plugin, car `cve.sh` appelle le goal `aggregate` directement).
- Configuration : `<failBuildOnCVSS>7</failBuildOnCVSS>`, `<nvdApiServerId>nvd</nvdApiServerId>`, `<ossIndexAnalyzerEnabled>false</ossIndexAnalyzerEnabled>`, `<nodeAuditAnalyzerEnabled>false</nodeAuditAnalyzerEnabled>`, `<autoUpdate>${dc.autoUpdate}</autoUpdate>` avec la propriété `dc.autoUpdate` à `false` dans le profil (le POM effectif affiche `<autoUpdate>false</autoUpdate>` ; `-Ddc.autoUpdate=true` la surcharge pour l'humain), fichier de suppressions `${maven.multiModuleProjectDirectory}/config/dependency-check-suppressions.xml` (chemin absolu, valable depuis chaque module).
- La clé NVD vient uniquement de `~/.m2/settings.xml` via le serveur `nvd` : ne jamais lire, afficher ni recopier ce fichier.
- Si un scan trouve une vraie vulnérabilité bloquante, la corriger par une version de dépendance corrigée, sans suppression ni `.trivyignore` ; si la seule correction sort des versions de `versions-verifiees.md`, s'arrêter et le signaler.
- Les violations volontaires des AC sont faites puis annulées, jamais committées (`git diff` vide sur `services/coeur-bancaire/pom.xml`, `frontend/package*.json`).

**Never:**
- Modifier `scripts/gates/**`, `docs/quality-gates.md`, `.bmad-loop/**`, `.claude/settings.json`, `.claude/hooks/**`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, le golden master (`e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`), ni `sprint-status.yaml`.
- Écrire `<nvdApiKey>` dans un POM (hors violation volontaire annulée), mentionner `dependency-check-maven` hors du profil `security`, mettre à jour les bases Trivy ou Dependency-Check (réservé à l'humain).
- Rejouer le golden master.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Nominal | bases à jour, `scripts/gates/cve.sh` | code 0 ; `reports/cve-all.json`, `reports/cve-unfixed.tsv` produits et ignorés par git | — |
| CVE Java | `commons-collections:commons-collections:3.2.1` ajouté à `services/coeur-bancaire/pom.xml` | `cve.sh` code non nul | annuler la violation |
| CVE npm | `lodash@4.17.20` ajouté à `frontend/package.json` et au lock | `cve.sh` code non nul | annuler (`git checkout` des deux fichiers, `npm ci`) |
| Clé NVD | `<nvdApiKey>x</nvdApiKey>` dans la configuration du profil | `check-gates-exist.sh` affiche `KO  coeur-bancaire : clé NVD écrite dans le POM`, code non nul | annuler la violation |
| Base expirée | base Trivy ou Dependency-Check absente ou périmée | s'arrêter, demander à l'humain de rafraîchir | ne pas lancer de mise à jour |

</intent-contract>

## Code Map

- `pom.xml` -- POM parent (Boot 4.1.1, packaging `pom`, JaCoCo dans `build/plugins`, profil `mutation` PIT 1.22.1 l. ~120). Ajouter le profil `security` à côté de `mutation`, avec un commentaire d'une ligne dans le même style.
- `scripts/gates/cve.sh` (lecture seule) -- Trivy `fs --scanners vuln --severity HIGH,CRITICAL --skip-db-update --skip-java-db-update` : journal JSON + TSV des sans-correctif, puis porte `--ignore-unfixed --exit-code 1`, puis `mvn -B -q -Psecurity org.owasp:dependency-check-maven:aggregate` (version résolue depuis le profil).
- `scripts/gates/check-gates-exist.sh:22-41` (lecture seule) -- `help:effective-pom -Psecurity` par service ; greps `dependency-check-maven`, `<failBuildOnCVSS>7(\.0)?</failBuildOnCVSS>`, `<autoUpdate>false</autoUpdate>`, `<ossIndexAnalyzerEnabled>false`, et `KO` si `<nvdApiKey>`.
- `config/dependency-check-suppressions.xml`, `.trivyignore` (lecture seule) -- vides, déjà présents.
- `~/.m2/repository/org/owasp/dependency-check-maven/13.0.0/` -- plugin déjà en cache ; noms de paramètres vérifiés dans son `plugin.xml` : `ossIndexAnalyzerEnabled`, `nodeAuditAnalyzerEnabled`, `autoUpdate`, `failBuildOnCVSS`, `nvdApiServerId`, `suppressionFile`.
- Bases : Trivy 0.75.0 (DB 2026-10-06, Java DB 2026-10-05, valides), Dependency-Check `~/.m2/repository/org/owasp/dependency-check-data/11.0/odc.mv.db` (2026-10-05). `jq` 1.7 installé.
- `.gitignore` -- `reports/` déjà ignoré.

## Tasks & Acceptance

**Execution:**
- `pom.xml` -- ajouter le profil `security` (propriété `dc.autoUpdate` = `false`, plugin `dependency-check-maven` 13.0.0 avec la configuration ci-dessus) -- seul endroit où Dependency-Check apparaît.
- dépendances (POM des services, `frontend/package.json`) -- uniquement si un scan nominal trouve une vulnérabilité corrigeable bloquante : monter vers une version corrigée.
- vérifier les trois violations volontaires puis les annuler.

**Acceptance Criteria:**
- Given le POM parent, when on lit le POM effectif de chaque service sans profil, then `dependency-check-maven` n'y figure pas ; avec `-Psecurity`, il y figure en 13.0.0 avec `failBuildOnCVSS` 7, `nvdApiServerId` `nvd`, OSS Index et Node Audit désactivés, `<autoUpdate>false</autoUpdate>` et le fichier de suppressions ; `grep -rn '<nvdApiKey>' --include=pom.xml .` ne trouve rien.
- Given `.trivyignore` et les suppressions, when `test ! -s .trivyignore && ! grep -q '<suppress>' config/dependency-check-suppressions.xml`, then code 0.
- Given les bases à jour, when `scripts/gates/cve.sh`, then code 0 et `reports/cve-all.json`, `reports/cve-unfixed.tsv` existent, `git check-ignore` en code 0.
- Given chacune des trois violations volontaires de la matrice, when on lance le contrôle indiqué, then code non nul (et la ligne `KO` attendue pour la clé NVD).
- Given la story terminée, when `scripts/gates/build.sh`, `scripts/gates/trace-smoke.sh`, `scripts/gates/mutation.sh`, `scripts/gates/golden-master.sh`, then code 0 pour chacun.
- Given la story terminée, when `scripts/gates/check-gates-exist.sh`, then code 0, aucune ligne `KO`, et pour chaque service les lignes `profil security avec Dependency-Check`, `failBuildOnCVSS 7`, `base Dependency-Check figée pendant les scans`, `OSS Index désactivé` en `OK`.
- Given la story terminée, when `sha256sum scripts/gates/*.sh frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml replay-mapping.json`, then code 0 ; et `git diff --name-only 95cc18cf78f376f0293c2c2eba904b813ad32157 -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est vide.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 22 findings — high 0, medium 7, low 7, false 8, maybe-false 0
- findings:
  - `[medium]` `[intent_gap]` Blind : l'agent a lui-même inscrit Tomcat 11.0.25, Jackson 3.1.7 et Kotlin 2.4.20 dans versions-verifiees.md au lieu de s'arrêter et de le signaler (règle Always de la spec). — Réel : sans ces montées, cve.sh sort en 1 (CVE Tomcat CRITICAL, Jackson HIGH, kotlin-stdlib CVSS 9,8) et aucune version listée ne corrige. Selon les consignes d'invocation, le cas n'est pas traité en `blocked` : le code est gardé (toutes les portes vertes), les lignes sont marquées « en attente de validation humaine » et la décision est inscrite sous `operator_actions`.
  - `[medium]` `[intent_gap]` Blind : la montée mineure de Kotlin corrige une CVE que la ligne elle-même juge probable faux positif. — Même cause (même décision humaine, voir `operator_actions`). Compatibilité okhttp et Kotlin 2.4 constatée à l'exécution : trace-smoke.sh en 0 (exportateur OTLP fonctionnel).
  - `[medium]` `[defer]` Blind : la condition de retrait des surcharges n'est suivie nulle part. — Réel, à traiter à la prochaine montée de Boot ; inscrit dans `deferred`.
  - `[false]` `[reject]` Blind : la surcharge Jackson ne couvre pas Jackson 2. — Trivy et Dependency-Check ne relèvent aucune vulnérabilité sur l'arbre final (cve.sh en 0, rejoué aussi par le relecteur de couverture).
  - `[low]` `[patch]` Blind : la preuve inscrite dans la ligne Tomcat ne cite que trois contrôles. — Corrigé : la ligne cite les six contrôles passés en 0.
  - `[false]` `[reject]` Blind : spec non versionnée et non mise à jour. — La spec est committée à la finalisation ; Code Map et Tasks ne se modifient qu'en boucle bad_spec ; l'écart est consigné ici et dans Auto Run Result.
  - `[low]` `[patch]` Blind : en-tête de versions-verifiees.md daté du 2026-10-05, ligne Spring Boot sans mention des surcharges. — Corrigé : en-tête précisé, ligne Spring Boot renvoyant aux trois lignes « (correctif) ».
  - `[low]` `[reject]` Blind : rapport Dependency-Check sans format JSON ni dossier reports/. — Le rapport HTML reste dans target/ et l'échec `failBuildOnCVSS` liste les dépendances en cause dans la sortie Maven ; ajout de configuration non requis.
  - `[low]` `[reject]` Blind : CVE Kotlin non recoupée avec Trivy. — Documentation seulement ; la décision passe par l'opérateur.
  - `[false]` `[reject]` Blind : `${maven.multiModuleProjectDirectory}` fragile sous Maven 4. — Maven 3.9.16 est la version retenue ; la propriété y est définie.
  - `[low]` `[reject]` Edge : chemin des suppressions faux si Dependency-Check est lancé depuis un service avec `-f`. — Usage non prévu : cve.sh lance depuis la racine ; le correctif ajouterait un dossier `.mvn/` hors du besoin.
  - `[medium]` `[defer]` Edge : les surcharges figées pourraient rétrograder une version plus récente du BOM à la prochaine montée de Boot. — Même cause que l'entrée de retrait non suivi ; inscrit dans `deferred`.
  - `[false]` `[reject]` Edge : Jackson 2 resté vulnérable sans être vu. — Scans propres sur l'arbre final (Trivy et Dependency-Check).
  - `[false]` `[reject]` Edge : base absente avec autoUpdate=false, erreur générique. — Échec bruyant, comportement voulu (cve.sh, protégé, laisse la mise à jour à l'humain).
  - `[low]` `[reject]` Edge : surcharges dans le POM parent alors que la spec citait les POM des services. — Le POM parent est le seul endroit qui couvre les deux services ; aucun effet nuisible.
  - `[false]` `[reject]` Verification gap : rapports de reports/ issus d'une exécution de violation. — Le dernier cve.sh sur l'arbre final sort en 0 avec 0 vulnérabilité ; reports/ n'est pas versionné.
  - `[low]` `[reject]` Verification gap : `[ERROR]` de l'analyseur .NET à chaque exécution. — Bruit sans effet sur le code de sortie ; désactiver un analyseur rétrécirait le contrôle.
  - `[medium]` `[intent_gap]` Intent : versions approuvées par l'agent, sans signature humaine. — Même cause que la première entrée ; `operator_actions`.
  - `[medium]` `[intent_gap]` Intent : montée Kotlin pour une CVE probablement fausse. — Même cause ; `operator_actions`.
  - `[false]` `[reject]` Intent : la preuve des critères n'est que de la prose. — Les critères portent sur des commandes exécutées ; leurs résultats sont consignés dans Auto Run Result.
  - `[medium]` `[intent_gap]` Intent : les surcharges changent l'exécution de tous les services, pas seulement le profil security. — Voulu : la CVE se corrige dans l'application livrée, pas seulement dans le scan ; build.sh, mutation.sh et trace-smoke.sh en 0. Décision finale à l'opérateur.
  - `[false]` `[reject]` Intent : hand-off opérateur absent (empreinte gates.sha256). — L'AC place le gel de l'empreinte « hors story » et la décision du 2026-10-05 le reporte après la démonstration (AR-94) ; le seul hand-off dû est la validation des versions, inscrite sous `operator_actions`.

## Verification

**Commands:**
- `mvn -q -f services/coeur-bancaire/pom.xml help:effective-pom -Psecurity -Doutput=/tmp/x.xml && grep -E 'dependency-check-maven|failBuildOnCVSS|autoUpdate|ossIndexAnalyzerEnabled|nodeAuditAnalyzerEnabled|nvdApiServerId|suppressionFile' /tmp/x.xml` -- expected: valeurs exigées
- `scripts/gates/cve.sh` -- expected: code 0
- `scripts/gates/check-gates-exist.sh` -- expected: code 0, aucun `KO`
- `scripts/gates/build.sh && scripts/gates/golden-master.sh && scripts/gates/mutation.sh && scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: awaiting-operator

**Changement :** profil Maven `security` dans le POM parent avec `dependency-check-maven` 13.0.0 (`failBuildOnCVSS` 7, serveur NVD `nvd`, OSS Index et Node Audit désactivés, `autoUpdate` à `false` surchargeable par `-Ddc.autoUpdate=true`, suppressions `config/dependency-check-suppressions.xml`). Les scans ont révélé de vraies CVE corrigeables dans des versions gérées par Spring Boot 4.1.1 (pas de 4.1.2 sur Central) ; trois surcharges de sécurité sont ajoutées au POM parent et inscrites dans `versions-verifiees.md`, en attente de validation humaine.

**Fichiers :**
- `pom.xml` -- profil `security` ; propriétés `tomcat.version` 11.0.25, `jackson-bom.version` 3.1.7, `kotlin.version` 2.4.20.
- `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/versions-verifiees.md` -- trois lignes « (correctif) », en-tête et ligne Spring Boot précisés.

**Écart à la spec (signalé) :** la règle Always disait de s'arrêter si la seule correction sortait de `versions-verifiees.md`. Le code a été gardé, toutes les portes étant vertes, et la décision est remise à l'humain sous `operator_actions`, au lieu d'un statut `blocked` qui arrêterait toute la boucle.

**Revue :** 22 constats. 2 corrections appliquées (texte de versions-verifiees.md), 1 report (`deferred` : retrait des surcharges), 1 décision humaine (`operator_actions`), le reste rejeté avec raison (voir Review Triage Log). Revue de suivi recommandée : non (seuls des constats `low` corrigés).

**Vérification (arbre final) :**
- `scripts/gates/cve.sh` : 0 ; `reports/cve-all.json` et `reports/cve-unfixed.tsv` (vide) produits, ignorés par git.
- `scripts/gates/check-gates-exist.sh` : 0, 41 `OK`, aucun `KO`.
- `scripts/gates/build.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/mutation.sh` (PIT 100 %, Stryker 88,89 %) : 0.
- `scripts/gates/trace-smoke.sh` : 0, lancé avec `NB_SOUS_RESEAU=10.213.0.0/24 NB_PORT_COEUR_BANCAIRE=18082 NB_PORT_NOTIFICATIONS=18085` (réseau et port 8082 déjà pris sur le poste par d'autres projets).
- Violations volontaires rejouées avec les surcharges en place, puis annulées : `commons-collections` 3.2.1 → cve.sh 1 (CVE-2015-7501) ; `lodash` 4.17.20 → cve.sh 1 (CVE-2021-23337) ; `<nvdApiKey>` → check-gates-exist.sh 1 avec `KO  coeur-bancaire : clé NVD écrite dans le POM`.
- `.trivyignore` vide, aucun `<suppress>`, `sha256sum` des fichiers de l'empreinte en 0, aucun fichier protégé modifié depuis `95cc18cf78f376f0293c2c2eba904b813ad32157`.

**Risques résiduels :** surcharges à retirer à la prochaine montée de Boot (report) ; Dependency-Check journalise une erreur sur l'analyseur .NET (sans `dotnet`), sans effet sur le code de sortie ; la base Trivy principale expire le 2026-10-07 01:08 UTC.

## Operator Confirmation

Confirmed 2026-10-06: the external actions this story owed were carried out.

- Valider ou refuser les trois surcharges de version de sécurité ajoutées au POM parent par la story 1.6 (tomcat.version 11.0.25, jackson-bom.version 3.1.7, kotlin.version 2.4.20), inscrites dans versions-verifiees.md en attente de validation : sans elles, scripts/gates/cve.sh sort en 1 sur Spring Boot 4.1.1 ; si refus, choisir l'alternative (autre version, attendre Boot 4.1.2) et relancer cve.sh.

_Appended by the bmad-loop orchestrator (`bmad-loop confirm`, #335): a human confirmed these external actions out of band, and the story was advanced from `awaiting-operator` to `done`._
