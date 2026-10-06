---
title: 'Consulter l’échéancier, marquer une échéance payée et supprimer un prêt'
type: 'feature'
created: '2026-10-06'
baseline_revision: 'e61baa9f6061bb73712a0d0a98635fb637b1bd39'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-5-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problème :** la liste « Emprunts en cours » n'a ni bouton « Voir », ni échéancier, ni « Marquer paye », ni « Supprimer » ; `credits` génère l'échéancier à l'approbation (story 5.3) mais ne l'expose pas et ne gère ni paiement ni suppression.

**Approche :** trois routes de `credits` (échéances d'un prêt, paiement d'une échéance, suppression d'un prêt), propriétaire seulement ; côté écran Prêts, bouton « Voir » qui ouvre une section d'échéancier, bouton « Marquer paye » par échéance non payée, bouton « Supprimer » par ligne.

## Boundaries & Constraints

**Always :**
- Routes (toutes authentifiées, tout rôle, propriétaire = sujet du jeton) : `GET /api/credits/prets/{pretId}/echeances` (200, tableau trié par numéro : `id`, `numero`, `dateEcheance` `yyyy-MM-dd`, `capital`, `interets`, `total`, `capitalRestant` en chaînes à 2 décimales, `paye` booléen, `datePaiement` instant ISO UTC ou `null`) ; `POST /api/credits/echeances/{echeanceId}/paiement` (200, renvoie l'échéance à jour) ; `DELETE /api/credits/prets/{pretId}` (204).
- Lecture des échéances : prêt inexistant 404 « Ressource introuvable », prêt d'un autre 403 « Accès refusé » (comportement de `GET /prets/{id}`), prêt non EN_COURS/REMBOURSE : tableau vide.
- Paiement et suppression : appartenance d'un autre client → 403 `{"message"}` « Erreur : l'échéance n'a pas été marquée comme payée. Veuillez réessayer. » (`pret.erreur.paiement`) / « Erreur suppression : le prêt n'a pas été supprimé. Veuillez réessayer. » (`pret.erreur.suppression`), rien modifié ; échéance ou prêt inexistant → 404 (message « Ressource introuvable ») ; erreur inattendue → 500 avec ces mêmes textes, sans détail technique. Les 403/404 de ces deux routes portent donc leurs textes propres (`Textes`, `GestionErreurs`) sans changer ceux de `GET /prets/{id}`.
- Paiement : cas d'usage Java pur dans `Transaction.executer` ; `UPDATE loan_repayments SET paye = true, date_paiement = ? WHERE id = ? AND paye = false` (date = `Horloge.instant()`), zéro ligne = déjà payée : date inchangée, succès sans erreur ni effet ; s'il ne reste aucune échéance non payée, `PretRepository.transitionner(pretId, EN_COURS, REMBOURSE)` (conditionnelle). Écueil à éviter : deux paiements concurrents de deux dernières échéances ne doivent pas laisser le prêt EN_COURS — verrouiller la ligne du prêt (`select … for update`) en début de transaction. Aucun événement émis, aucun appel à `coeur-bancaire`, aucun débit.
- Suppression : `DELETE FROM loans` (cascade `on delete cascade` sur `loan_repayments`), tout statut, sans confirmation ; aucun événement.
- Front : bouton « Voir » (`pret-voir-{loanId}`) dans la colonne « Echeancier » seulement pour EN_COURS et REMBOURSE ; « Supprimer » (`pret-supprimer-{loanId}`) dans « Action » sur chaque ligne ; section « Echeancier du pret selectionne » (séparateur, majuscules par CSS comme les autres) contenant une `card-muted` titrée « Detail des echeances » (le focus y va, titre `tabIndex={-1}`), colonnes « # », « Date echeance » (jj/MM/aaaa produit par manipulation de chaîne, jamais `Date`), « Capital », « Interets », « Total », « Capital restant », « Statut » (« Paye » / « En attente »), « Action » ; « Marquer paye » (`pret-marquer-paye-{repaymentId}`, `repaymentId` = `id` de l'échéance) seulement si non payée. Montants des échéances au format de `formaterMontantLigne`. Succès du paiement : « Echeance marquee comme payee » dans `pret-message-succes` ; l'échéancier et la liste sont relus (le prêt peut passer « Rembourse »). Suppression : liste relue, section fermée si elle concernait ce prêt. Toute erreur (403, 404, 500, réseau, JSON illisible) : le texte fixe `pret.erreur.paiement` / `pret.erreur.suppression` dans `pret-message-erreur`. 401 : jeton supprimé, retour `/login` (comme les autres sections).
- `domain`/`application` back et front sans framework, 100 % lignes/branches ; temps par le port `Horloge`, jamais `new Date()` dans `frontend/src/domain` et `application` ; textes nouveaux depuis `textes-nouveaux.md`, textes legacy à l'identique (`MESSAGES.md` §FEAT-007), aucun texte inventé ; montants en chaînes, front en centimes.
- Tags : back `@Tag("FR-007-11")`, `FR-007-12`, `FR-007-13` (au moins un test chacun) ; front : le nom de chaque test commence par `[FR-007-11]`, `[FR-007-12]` ou `[FR-007-13]` (convention de `EcranPrets.test.tsx`) pour que `npx vitest run -t 'FR-007-1x'` en exécute au moins un ; test `EcheanceTransitionConcurrenteTest` (modèle `PretTransitionConcurrenteTest`).

**Never :**
- Aucune modification de `scripts/gates/**`, `.bmad-loop/**`, `.claude/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, seuils, exclusions de couverture ; rien sous `e2e/golden-master/`, `docs/retro/golden-master/derived/`, `replay-mapping.json` ; jamais de rejeu du golden master. Ne pas écrire `sprint-status.yaml`.
- Pas de consommateur `notifications` (5.4), pas de pipeline admin (5.6), pas de nouvelle dépendance, pas de confirmation de suppression, pas d'événement Kafka, pas d'appel HTTP entre services, pas de texte inventé, pas de relance de `retro-forward-bmad`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Gestion d'erreur |
|----------|---------------|----------------------|------------------|
| Voir un prêt EN_COURS | `user1`, 36 échéances | 36 lignes, « En attente », dates `jj/MM/aaaa` | — |
| Prêt EN_ATTENTE ou REFUSE | liste | pas de « Voir » ; API : tableau vide | — |
| Payer une échéance | non payée | `paye`, `datePaiement` = horloge, message succès, aucun événement | — |
| Payer deux fois | déjà payée | 200, date inchangée | aucune erreur |
| Dernière échéance | une seule restante, prêt EN_COURS | prêt REMBOURSE | transition conditionnelle |
| Deux paiements concurrents (même échéance) | 2 threads | un seul effet | — |
| Payer d'un prêt déjà REMBOURSE | échéance déjà payée | inchangé | — |
| Supprimer | prêt de `user1` | prêt et échéances supprimés, ligne disparue | — |
| Prêt/échéance de `user2` | `user1` appelle | 403 avec texte propre, rien modifié | — |
| Inexistant | id inconnu | 404 | — |

</intent-contract>

## Code Map

- `services/credits/src/main/java/fr/nextbanking/credits/domain/` -- `Pret`, `StatutPret`, `EcheancePret` (création d'échéancier, sans `id`) ; à créer `Echeance` (record lu : `id`, `pretId`, numéro, date, montants, `paye`, `datePaiement`).
- `.../application/` -- `ConsulterPretCasUsage` (contrôle propriétaire, `PretIntrouvable`, `AccesPretRefuse`), `DecisionPretCasUsage` (modèle `Transaction.executer` + transition conditionnelle) ; à créer cas d'usage et ports entrants (échéances, paiement, suppression) ; `port/out/PretRepository` à étendre (échéances d'un prêt, échéance par id, paiement conditionnel, nombre d'échéances non payées, verrou du prêt, suppression) ; `port/out/Horloge`.
- `.../adapter/out/persistance/PretRepositoryJdbc.java` -- `JdbcClient`, `transitionner`, `creerEcheancier` ; tables `loans`, `loan_repayments(id, loan_id, numero_echeance, date_echeance, montant_*, capital_restant, paye, date_paiement timestamptz)` (`V1__loans.sql`, cascade existante).
- `.../adapter/in/http/{PointEntreePrets,Textes,GestionErreurs,ConfigurationSecurite}.java` -- routes (tout authentifié, `anyRequest().authenticated()` suffit), textes d'erreur propres (A-024/`textes-nouveaux.md` l.53-54), 403/404 par exceptions d'application.
- `.../infrastructure/AssemblageCredits.java` -- beans des nouveaux cas d'usage.
- `services/credits/src/test/java/fr/nextbanking/credits/adapter/{PretTransitionConcurrenteTest,PretsHttpIT,DemandePretHttpIT,OutilsHttp}.java`, `application/{CasUsageTest,DecisionPretCasUsageTest}.java`, `infrastructure/Conteneurs.java` -- modèles de tests (IT Testcontainers, concurrence, doubles de ports) ; `OutilsHttp.chargerDonnees` vide les tables.
- `frontend/src/domain/pret.ts` (types `Pret`, `LIBELLES_STATUT`, `formaterMontantLigne`) -- ajouter type `Echeance`, mise en forme de la date `jj/MM/aaaa`, règle « Voir » (EN_COURS/REMBOURSE).
- `frontend/src/application/prets.ts` -- port `ClientPrets` (`simuler`, `lister`, `deposer`) et cas d'usage `listerPrets` (modèle : jeton absent/401 → déconnecté) ; ajouter échéances, paiement, suppression.
- `frontend/src/adapters/http/ClientPretsHttp.ts` -- validation de forme (`pret`, `estMontant`) ; ajouter les trois appels.
- `frontend/src/adapters/ecrans/prets/{EcranPrets,SectionEmprunts}.tsx` (+ `.css`) -- colonnes « Echeancier » et « Action » vides à remplir ; créer `SectionEcheancier.tsx/.css` ; `frontend/src/adapters/textes.ts` (`TEXTES.prets`) -- ajouter les textes ; `frontend/src/adapters/ui/{Carte,AlerteInfo,AlerteErreur,Tableau,BoutonTexte,BoutonPrimaire}.tsx` -- primitives (pas de variante `card-muted` ni d'alerte de succès : les créer à partir des jetons existants, `tokens.css` généré par `npm run tokens`, jamais édité à la main).
- Tests front : `EcranPrets.test.tsx`, `ClientPretsHttp.test.ts`, `application/prets.test.ts`, `domain/pret.test.ts`.
- Lecture seule : `docs/retro/features/FEAT-007/{RG,MESSAGES}.md` (RG-007-033 à 040, MSG-007-072 à 092, 129, 130, 132), `textes-nouveaux.md` l.53-54, PRD FR-007-11 à 13, `epic-5-context.md`.

## Tasks & Acceptance

**Execution :**
- `services/credits/src/main/java/**/domain`, `**/application` -- record `Echeance`, cas d'usage et ports d'échéances/paiement/suppression -- FR-007-11, FR-007-12, FR-007-13.
- `services/credits/src/main/java/**/adapter`, `**/infrastructure` -- méthodes JDBC (dont verrou et paiement conditionnel), routes, exceptions et textes d'erreur, assemblage -- FR-007-11..13.
- `services/credits/src/test/**` -- tests unitaires (matrice I/O, doubles de ports), IT Testcontainers des trois routes (propriétaire, autre client 403, inexistant 404, double paiement, dernière échéance → REMBOURSE, suppression en cascade, aucun événement dans l'outbox), `EcheanceTransitionConcurrenteTest` -- tags de la story.
- `frontend/src/domain`, `application`, `adapters/http`, `adapters/ecrans/prets`, `adapters/textes.ts`, `adapters/ui` -- types, cas d'usage, adaptateur HTTP, section d'échéancier, boutons, messages -- FR-007-11..13.
- `frontend/src/**/*.test.*` -- tests Vitest (affichage, focus au titre, absence de « Voir », paiement et message, relecture de la liste après paiement/suppression, erreurs fixes, 401), seuils de couverture inchangés.

**Acceptance Criteria :**
- Given `user1` avec un prêt EN_COURS ou REMBOURSE, when il clique « Voir » (`pret-voir-{loanId}`), then la section « Echeancier du pret selectionne » titrée « Detail des echeances » s'ouvre dans une `card-muted` sous la liste, le focus va au titre, le tableau porte les colonnes et statuts attendus, et un prêt EN_ATTENTE ou REFUSE n'a pas de « Voir ».
- Given une échéance non payée, when `user1` clique « Marquer paye », then elle est payée avec la date de l'`Horloge`, « Echeance marquee comme payee » s'affiche dans `pret-message-succes`, et rien n'est émis ni débité ; une échéance déjà payée n'est pas repayée (date inchangée, sans erreur) ; `EcheanceTransitionConcurrenteTest` constate un seul effet pour deux paiements concurrents.
- Given un prêt EN_COURS à une échéance non payée, when elle est payée, then le prêt passe REMBOURSE par une mise à jour conditionnelle sur EN_COURS, y compris pour deux paiements concurrents des deux dernières échéances.
- Given un prêt de `user1`, when il clique « Supprimer », then le prêt et ses échéances sont supprimés définitivement, sans confirmation, et la ligne disparaît.
- Given `user1`, when il paie une échéance ou supprime un prêt de `user2`, then 403 `{"message"}` avec le texte propre à l'action, rien modifié ; id inexistant : 404.
- Given une erreur inattendue au paiement ou à la suppression, then la page affiche `pret.erreur.paiement` / `pret.erreur.suppression`, sans détail technique.
- Given `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-11|FR-007-12|FR-007-13'` puis `mvn -B -q -f services/credits/pom.xml verify`, then code 0, chaque tag porté par au moins un test, `EcheanceTransitionConcurrenteTest` exécuté ; `cd frontend && npx vitest run -t 'FR-007-11'` (puis `-t 'FR-007-12'`, `-t 'FR-007-13'`) code 0 avec au moins un test exécuté.

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 0, false 4, maybe-false 0
- findings:
  - `[false]` `[reject]` aucun test de bout en bout (navigateur, proxy, vrai back) — les critères de la story sont des commandes Maven, Vitest et des contrôles ; aucun critère ne demande de parcours de bout en bout, et le rejeu du golden master est interdit avant l'epic 10.
  - `[false]` `[reject]` contrat client HTTP / serveur confronté seulement à des réponses simulées — relecture du diff : les champs de `EcheanceJson` (`id`, `numero`, `dateEcheance`, `capital`, `interets`, `total`, `capitalRestant`, `paye`, `datePaiement`) sont ceux de `echeance()` dans `ClientPretsHttp.ts` ; `EcheancesHttpIT` fige la forme réelle côté serveur.
  - `[false]` `[reject]` « aucun événement » vérifié seulement par l'outbox vide — c'est le seul canal d'émission du service (toute publication passe par l'outbox) ; aucun cas d'usage de paiement ou de suppression ne reçoit le port `EvenementsSortants`.
  - `[false]` `[reject]` conversion Europe/Paris non testée pour `jj/MM/aaaa` — `dateEcheance` est une date seule (`LocalDate`, `yyyy-MM-dd`) sans fuseau ; seule la date de paiement est un instant (port `Horloge`), non affichée.

## Design Notes

Les routes et leur forme ne sont fixées ni par le PRD ni par l'architecture : elles prolongent `/api/credits/prets` (proxy `/api/credits/**` inchangé). Le paiement est adressé par l'identifiant de l'échéance seul (l'identifiant `{repaymentId}` du `data-testid` y correspond) et le propriétaire est déduit par jointure sur `loans`. Un paiement concurrent est sans danger pour l'échéance (`WHERE paye = false`) ; c'est le décompte « plus aucune non payée » qui exige le verrou de la ligne du prêt pour que le prêt passe REMBOURSE quand les deux dernières échéances sont payées en parallèle. Le front ne distingue pas 403, 404 et 500 : un seul texte fixe par action.

## Verification

**Commands :**
- `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-11|FR-007-12|FR-007-13'` puis `mvn -B -q -f services/credits/pom.xml verify` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-007-11'` (puis `FR-007-12`, `FR-007-13`), puis `npx tsc --noEmit` -- expected: code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `credits` expose l'échéancier d'un prêt EN_COURS ou REMBOURSE, le paiement d'une échéance (mise à jour conditionnelle, verrou de la ligne du prêt, passage à REMBOURSE quand tout est payé) et la suppression d'un prêt avec ses échéances, pour le propriétaire seulement ; l'écran Prêts affiche « Voir », la section « Echeancier du pret selectionne » avec « Marquer paye », et « Supprimer ».

**Fichiers :** `services/credits/**` (record `Echeance`, cas d'usage `ConsulterEcheances`, `PayerEcheance`, `SupprimerPret` et leurs exceptions, `PretRepositoryJdbc`, `PointEntreePrets`, `GestionErreurs`, `Textes`, `AssemblageCredits`, tests dont `EcheanceTransitionConcurrenteTest` et `EcheancesHttpIT`) ; `frontend/src/**` (domaine, cas d'usage, `ClientPretsHttp`, `SectionEcheancier`, `SectionEmprunts`, `EcranPrets`, `AlerteSucces`, `Carte` variante muted, textes, tests).

**Revue :** 0 patch, 0 report, 4 rejets motivés (cf. Review Triage Log). Revue complémentaire recommandée : `false` (patches : high 0, medium 0).

**Vérification :** `mvn test -Dgroups='FR-007-11|FR-007-12|FR-007-13'` et `mvn verify` credits en code 0 ; `npx vitest run -t` pour FR-007-11, 12 et 13 et `tsc --noEmit` en code 0 ; `build.sh`, `check-gates-exist.sh` (0 KO), `golden-master.sh`, `trace-smoke.sh` en code 0.

**Risques résiduels :** aucun parcours de bout en bout dans un navigateur (les tests front utilisent un client simulé) ; si la lecture de l'échéancier échoue, la page affiche le texte technique générique, faute de texte dédié.
