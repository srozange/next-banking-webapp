# Architecture cible et contrôles qualité (référence unique)

## Architecture
- Microservices Java 21, Spring Boot 4, Maven. Un module par service sous services/<nom>/, POM parent à la racine.
- Chaque service est hexagonal : packages domain, application, adapter.in, adapter.out, infrastructure.
  domain et application sont du Java pur : seules dépendances autorisées, java.* et le code du service.
  Aucun import Spring, Jakarta, Jackson ni autre bibliothèque ; aucune dépendance vers adapter ou infrastructure.
- Une base PostgreSQL par service. Aucun service ne lit la base d'un autre.
- Bus entre services : Kafka.
- Front React + TypeScript sous frontend/, hexagonal : src/domain, src/application, src/adapters, src/infrastructure.
  src/domain et src/application sont du TypeScript pur : aucun import de paquet npm (ni React, ni bibliothèque d'état),
  hors fichiers de test. Tout ce qui est React vit dans src/adapters.
- Tracing : spring-boot-starter-opentelemetry, export OTLP vers un collecteur OpenTelemetry, backend Jaeger v2.
  Aucun span créé à la main. Spans JDBC via datasource-micrometer 2.x.
  Contexte propagé sur HTTP et sur Kafka (observation activée sur KafkaTemplate et sur les listeners).
  Échantillonnage à 1.0 en local.
- compose.yaml à la racine : construit les images et lance les services, PostgreSQL, Kafka, le collecteur et Jaeger.

## Contrôles (exécutés par des outils, jamais évalués par un agent)
- Hexagone back : test ArchUnit *ArchitectureTest dans chaque service, avec deux règles : sens des dépendances,
  et aucun framework dans domain et application. Bloquant à chaque build.
- Hexagone front : dependency-cruiser, config frontend/.dependency-cruiser.cjs, règles en sévérité error :
  sens des dépendances, et aucun paquet npm dans src/domain et src/application. Bloquant.
- Couverture back : JaCoCo check en phase verify. 100 % lignes et branches sur domain et application,
  80 % sur adapter et infrastructure. Bloquant.
- Couverture front : Vitest coverage.thresholds par glob, mêmes seuils. Bloquant.
- Types front : tsc --noEmit sur frontend/ (tsconfig.json en mode strict), exécuté par scripts/gates/build.sh. Bloquant.
- Build back : scripts/gates/build.sh part d'un build propre (mvn clean verify -DfailIfNoTests=true),
  pour qu'un module sans test ou un test supprimé ne soit jamais masqué par des classes déjà compilées. Bloquant.
- Mutation back : PIT dans le profil Maven mutation, sur domain et application, mutationThreshold 85,
  historique activé. À la demande.
- Mutation front : StrykerJS sur src/domain et src/application, thresholds break 85 / high 90,
  mode incremental. À la demande.
- CVE back : OWASP Dependency-Check dans le profil Maven security, failBuildOnCVSS 7, clé d'API NVD via settings.xml,
  OSS Index et Node Audit désactivés, mise à jour de la base désactivée pendant les scans.
- CVE back et front : Trivy fs, sévérités HIGH et CRITICAL. Porte bloquante avec --ignore-unfixed ;
  les vulnérabilités sans correctif sont journalisées dans reports/, sans bloquer.
- Tracing : scripts/gates/trace-smoke.sh prouve qu'une trace traverse deux services avec un span JDBC et un span Kafka.
- Golden master : scripts/gates/golden-master.sh, contrôle statique sans rejeu. replay-mapping.json se dérive sans erreur,
  et le golden master dérivé et les tests générés sont identiques à une dérivation et à une génération fraîches. Bloquant.
  Le rejeu lui-même n'a lieu qu'une fois, développement terminé.

## Règles pour les agents
- Un contrôle rouge se corrige dans le code ou dans les tests, jamais en modifiant le contrôle.
- Fichiers interdits de modification : scripts/gates/**, .bmad-loop/policy.toml, .bmad-loop/gates.sha256,
  .claude/settings.json, .claude/hooks/**, docs/quality-gates.md,
  frontend/.dependency-cruiser.cjs, .trivyignore, config/dependency-check-suppressions.xml.
- Interdit d'abaisser un seuil, d'ajouter une exclusion de couverture ou de mutation, de désactiver un test.
- Si un seuil paraît inatteignable : s'arrêter et le signaler, ne pas contourner.
- Golden master : fichiers interdits de modification, sauf dans la story « Socle du rejeu final du golden master » :
  e2e/golden-master/** (tests générés), docs/retro/golden-master/derived/**, replay-mapping.json.
- Dans la story « Socle du rejeu final », l'agent peut, en autonomie :
  - créer e2e/golden-master/package.json et son fichier de verrouillage ;
  - corriger replay-mapping.json, puis redériver et régénérer avec les outils
    (docs/retro/tools/derive-golden-master.py derive --force, puis export-golden-master-tests.py) ;
  - inscrire chaque correction dans
    _bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/ecarts-voulus-golden-master.md :
    scénario, ce qui change, raison.
- Même dans cette story : jamais d'édition à la main des tests générés ni du dérivé,
  jamais de scénario retiré ni de texte attendu supprimé pour obtenir du vert ;
  scripts/gates/golden-master.sh doit sortir en code 0.
