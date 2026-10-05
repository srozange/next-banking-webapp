<!-- bmad:context -->
<!-- Vérifié le 2026-10-05 sur 63d380d. Géré par bmad-project-context ; tout ce qui est entre ces marqueurs est remplacé à chaque refresh. Ce qui doit être conservé va hors des marqueurs. -->

## next-banking-webapp

Réécriture de l'application bancaire legacy : microservices Java/Spring Boot sous `services/`, front React/TypeScript sous `frontend/`, Kafka entre services. Planification : `_bmad-output/planning-artifacts/` (PRD, spine d'architecture, UX). Comportement actuel du legacy : corpus `docs/retro/` (index `docs/retro/MANIFEST.json`), référence de l'existant et non spécification à recopier.

## Policy

Règles pour les agents (reprises de `docs/quality-gates.md`) :
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

Exception, jusqu'à la fin de l'epic 1 (avant l'empreinte de l'étape 10) : la story 1.2 crée `frontend/.dependency-cruiser.cjs` et la story 1.4 crée `scripts/gates/trace-smoke.sh`. Aucune autre story ne les modifie ; une fois l'empreinte figée, seul l'humain les modifie.

Golden master et secrets :
- Hors de la story « Socle du rejeu final », si un scénario paraît faux : s'arrêter et le signaler ; les décisions de rejeu passent par le skill `corpus-bmad-golden-master`.
- Ne jamais rejouer le golden master avant l'epic 10 : seules les stories « Socle du rejeu final » (preuve de déterminisme de SC-003-04) et « Rejeu complet » le rejouent.
- Secrets uniquement en variables d'environnement (`make secrets` génère `.env` et `.secrets/`, non versionnés). Ne jamais lire, afficher ni écrire dans un POM la clé NVD de `~/.m2/settings.xml`.
- Ne jamais committer `mode=reel` pour un fournisseur externe : l'adaptateur simulé reste actif par défaut.

## Where things are

- Avant d'ajouter un topic Kafka, une table, un point d'API, un écran ou un service : lire la section concernée de `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/ARCHITECTURE-SPINE.md`. En cas d'écart, `docs/quality-gates.md` fait foi, puis le PRD.
- Règles d'une fonctionnalité du legacy : `docs/retro/features/FEAT-NNN/` ; bugs à corriger, chacun couvert par un test : `docs/retro/00-global/LEGACY-BUGS.md`.
- Contrôles et seuils : `docs/quality-gates.md`.

## Running and verifying

- Avant de déclarer un travail terminé : `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh` et `scripts/gates/golden-master.sh` (statique, ne rejoue rien), code de sortie 0. Pendant l'epic 1, `check-gates-exist.sh` affiche des `KO` pour les contrôles des stories pas encore livrées ; ceux de la story en cours doivent passer à `OK`.
- `scripts/gates/mutation.sh` et `scripts/gates/cve.sh` : à la demande. `cve.sh` ne met pas à jour les bases Trivy et NVD : si elles manquent ou ont expiré, s'arrêter et demander à l'humain de les rafraîchir.
- TODO après la story 1.4 : ajouter `scripts/gates/trace-smoke.sh` à la liste ci-dessus.
- Versions : uniquement celles de `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/versions-verifiees.md`, jamais celles de l'environnement local ni « la dernière » (ex. TypeScript 5.9.3, pas 7.x).
- TODO après la story 1.1 : commandes de build et de test unitaire par module, à vérifier au premier refresh.

## Conventions that differ from defaults

- Code métier en français (`Virement`, `Horloge`) ; paquets `fr.nextbanking.<identifiantsanstiret>`, modules `services/<identifiant-kebab>/` ; champs JSON en français, camelCase.
- Montants : `BigDecimal` à 2 décimales, `numeric(19,2)`, chaîne `"100.00"` en JSON, jamais de flottant ; le front calcule en centimes entiers.
- Temps : uniquement par le port `application.port.out.Horloge`, fuseau `Europe/Paris`. Aucun `now()`, `Clock` ou `System.currentTimeMillis()` hors de `adapter.out.horloge` et `adapter.out.jeton` ; ni `Date.now()` / `new Date()` dans `frontend/src/domain` et `src/application`.
- Les services ne s'appellent jamais en HTTP : projections locales alimentées par Kafka. Toute publication liée à une écriture passe par l'outbox ; consommateurs idempotents (`evenements_traites`).
- Entre services, toujours l'identifiant numérique (`compteId`), jamais le numéro de compte.
- Textes repris à l'identique du legacy (`docs/retro/00-global/MESSAGES.md`, casse comprise), nouveaux depuis `_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/textes-nouveaux.md` ; aucun texte inventé dans le code : un texte manquant est un point ouvert.
- `data-testid` au format `{écran}-{élément}[-{id}]` sur tout élément interactif ; valeurs et routes existantes figées. Une section nouvelle se place après le contenu comparé (fenêtre de 5 000 caractères du golden master).
- Journaux : IBAN toujours masqué (`FR76****1234`), jamais de mot de passe ni de jeton.
- Front : un seul `frontend/vite.config.ts` (Vitest, proxy, plugin React), jamais de `vitest.config.*` — `check-gates-exist.sh` lirait le mauvais fichier. `tokens.css` est généré par `npm run tokens`.
- Messages de commit en français, format `type : description` (espace avant les deux-points) ; commits sur `main` autorisés.

## Known pitfalls

- Ne pas relancer `retro-forward-bmad` : il réécrit `_bmad/custom/bmad-*.toml` et efface l'entrée `file:{project-root}/docs/quality-gates.md`.
- Compter les scénarios du golden master avec les outils de `docs/retro/tools/`, jamais à la main : deux décomptes manuels ont déjà été faux.

<!-- /bmad:context -->
