---
title: 'Voir ses comptes et ses dernières opérations sur le tableau de bord'
type: 'feature'
created: '2026-10-06'
baseline_revision: '94477ca7b16750d284b83bbb09124bada80e0056'
status: 'done'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-3-context.md'
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/quality-gates.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** la route `/tableau-de-bord` ne rend que la section « Suivi de mes virements » (story 3.4) : le client ne voit ni son solde total, ni ses comptes, ni ses dernières opérations ; le cœur bancaire n'expose pas `GET /api/coeur-bancaire/tableau-de-bord`.

**Approach:** créer l'écran du tableau de bord (séparateur, carte « Solde total Demo Bank », cartes de comptes, section « Activite recente / Dernieres operations », « Suivi de mes virements » en dernier) et l'API `GET /api/coeur-bancaire/tableau-de-bord` (comptes avec solde, solde total, 5 dernières opérations du compte COURANT, identité prise du jeton). Le texte exact des critères est la story 3.8 de `_bmad-output/planning-artifacts/epics.md` (lignes 2739-2800), FEAT-002 du PRD et `docs/retro/features/FEAT-002/{MESSAGES,FIELDS,RG}.md` : les lire en entier.

## Boundaries & Constraints

**Always:** hexagone (`domain`/`application` Java pur et TypeScript pur, 100 % lignes et branches) ; montants `BigDecimal` 2 décimales, chaîne `"100.00"` en JSON, front en centimes entiers, jamais de flottant ; identité du client prise du jeton, jamais d'un paramètre ; COURANT choisi par son type (jamais « le premier compte », BUG-005), tri décidé et testé dans `application`, 5 opérations au plus ; les appels identité (`/moi` par le gabarit) et cœur bancaire partent en parallèle, jamais en cascade ; textes du legacy sans accent (« Mes comptes Demo Bank », « Solde total Demo Bank », « Tous comptes Demo Bank confondus », « N {numéro} », « Activite recente », « Dernieres operations », « Date », « Description », « Type », « Montant », « Solde apres », « Voir le releve complet ») dans un bloc `TEXTES.tableauDeBord` ; capitales par CSS `text-transform` (UX-DR156), jamais dans le texte source ; format des montants `EURx,xxx.xx` et date `dd/MM/yyyy HH:mm` en Europe/Paris (UX-DR157), nouveau formateur de domaine distinct de `formaterEuros` du relevé, `formaterDate` réutilisable ; couleurs par les tokens du thème (CREDIT `success`, DEBIT `error`, signe ASCII) ; `Carte attenuee` pour les cartes ; `data-testid` préfixe `dashboard-` ; `aria-busy` pendant le chargement, ni squelette ni texte de chargement ; aucun bandeau ni libellé technique (B-17/18/19) ; « Suivi de mes virements » reste la dernière section (UX-DR153) ; fiche supprimée : 401 de `/moi` donne jeton supprimé et `/login` sans message (BUG-004) ; tests tagués (Java `@Tag("FR-002-0x")`, `@Tag("BUG-005")` ; Vitest `[FR-002-0x]`, `[FR-003-12]`, `[BUG-004]` dans le titre) ; versions de `versions-verifiees.md` ; textes nouveaux : aucun, un texte manquant est un point ouvert.

**Never:** modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils, exclusions de couverture ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; appel HTTP entre services ; centre de notifications (3.9) et bloc Powens ; écrire `sprint-status.yaml` ; ajouter des données de référence relevant du « Socle du rejeu final ».

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Sans session | `GET /tableau-de-bord` sans jeton | 401 | — |
| Ouverture | `user1`, COURANT 1 et EPARGNE 2 | « Bonjour, {fullName} », Deconnexion, séparateur, solde total = somme, une carte par compte (type, `EUR2,130.00`, « N … »), 5 dernières opérations du COURANT, bouton vers `/releve`, « Suivi de mes virements » en dernier | — |
| EPARGNE créé avant COURANT | > 5 opérations sur le COURANT | les 5 plus récentes du COURANT, ordre décroissant | `-Dgroups='BUG-005'` échoue si on prend le premier compte |
| Sans compte / sans COURANT | CONSEILLER, ou client épargne seule | solde `EUR0.00` (ou somme des comptes), tableau vide (ligne pleine largeur), bouton « Voir le releve complet » affiché | — |
| Fiche supprimée | `/moi` répond 401 | jeton supprimé, `/login`, aucune trace | pas de texte technique |
| Après virement | 100 € du compte 1 au 2 | soldes à jour, ligne DEBIT en tête | — |
| Compte d'autrui en paramètre | `?compteId=` d'un autre client | seules les données de `user1` | — |
| Erreur du cœur bancaire | 5xx | texte technique du legacy, pas de trace | `AlerteErreur` existante |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/adapter/in/PointEntreeVirements.java` -- `GET /comptes`, `GET /comptes/{id}/operations` existent : ajouter `GET /tableau-de-bord` (identité `Long.parseLong(jeton.getSubject())`, paramètres ignorés).
- `.../application/{ConsulterReleveCasUsage,ConsulterComptesCasUsage,port/in,port/out/{Comptes,Operations}}` -- modèles ; ajouter port entrant `ConsulterTableauDeBord` + cas d'usage (COURANT par type, `Operations.duCompte` déjà trié date puis id décroissants, limite 5 décidée dans le cas d'usage, solde total en `BigDecimal`). `Comptes.duClient` trie par `id` : c'est le piège BUG-005.
- `.../infrastructure/AssemblageVirement.java` -- bean du nouveau cas d'usage (beans l. 63-69) ; `ConfigurationSecurite` couvre déjà `anyRequest().authenticated()`.
- `services/coeur-bancaire/src/test/**` -- modèles `ReleveIT`, `ConsulterReleveCasUsageTest`, `MemoireCoeurBancaire`, `DonneesDeReference`, `Jetons`. `CoeurBancaireSecuriteIT` : le test `avec_un_jeton_valide_une_route_inconnue_rend_404` utilise `/tableau-de-bord` qui n'est plus inconnu : lui donner une autre route inexistante (sans affaiblir le test) ; `/tableau-de-bord` reste dans `CHEMINS` (401).
- `frontend/src/adapters/routes/routes.tsx` -- route `/tableau-de-bord` (aujourd'hui `<title>` + `SectionSuiviVirements`) ; `routes.test.tsx` fige la liste des routes protégées et la présence de « Suivi de mes virements » sur cette route seulement.
- `frontend/src/adapters/gabarit/GabaritAuthentifie.tsx` -- en-tête, navigation (déjà par rôle, FR-002-11 testé), `chargerProfil`, 401 → `/login` ; profil passé par `Outlet context`.
- `frontend/src/adapters/ecrans/virement/SectionSuiviVirements.tsx` (`dashboard-virements-revue`) -- à placer en dernier. `adapters/ecrans/releve/formaterDate.ts` -- réutilisable. `adapters/ui/{Carte,Tableau,Etiquette,AlerteErreur}`, `adapters/ecrans/bourse/EcranBourse` (modèle de tableau).
- `frontend/src/adapters/http/{ClientVirementHttp,ClientReleveHttp}.ts`, `application/{virement,consulterReleve}.ts`, `domain/{montant,releve}.ts` -- modèles ; ajouter un client HTTP du tableau de bord (port + adaptateur), cas d'usage `application/consulterTableauDeBord.ts`, domaine pur (somme en centimes, formateur `EURx,xxx.xx`, signes).
- `frontend/src/adapters/textes.ts` -- ajouter `TEXTES.tableauDeBord` (pas de réutilisation de `TEXTES.releve.colonnes` : « Solde après » accentué ≠ « Solde apres »). `adapters/test/rendreApplication.tsx`, `infrastructure/main.tsx`, `Dependances` -- injection du nouveau client.
- `scripts/golden-master/verifier-scenario.sh SC-002-01 | SC-003-04` -- statique, déjà à 0 ; les textes absents sont seulement consignés dans `reports/golden-master/textes-non-trouves.md` : après livraison, ceux de SC-002-01 doivent être trouvés dans `frontend/src`. Ne rien modifier.
- `docs/retro/features/FEAT-002/{MESSAGES,FIELDS,RG}.md`, `docs/retro/00-global/LEGACY-BUGS.md` (BUG-004, BUG-005) -- textes, formats, règles.

## Tasks & Acceptance

**Execution:**
- `services/coeur-bancaire/**` (port in, cas d'usage, DTO, contrôleur, assemblage) -- API du tableau de bord -- ACs API, BUG-005, sécurité
- `services/coeur-bancaire/src/test/**` -- tests domaine/application (100 %), IT (401, isolation client, EPARGNE avant COURANT avec > 5 opérations, sans compte, sans COURANT), ajustement de `CoeurBancaireSecuriteIT`, tags `FR-002-03/04/05`, `BUG-005` -- matrice
- `frontend/src/{domain,application}` (+ tests) -- solde total, formateur `EURx,xxx.xx`, choix des 5 lignes, chargement en parallèle -- règles pures
- `frontend/src/adapters/**` (écran `EcranTableauDeBord`, client HTTP, textes, route, assemblage, `rendreApplication`) et tests Vitest `[FR-002-01]` à `[FR-002-06]`, `[FR-002-11]`, `[FR-003-12]`, `[BUG-004]` -- écran
- Ne toucher à aucun contrôle ni exclusion.

**Acceptance Criteria:**
- Given la story 3.8 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé
- Given `scripts/golden-master/verifier-scenario.sh SC-002-01` puis `SC-003-04`, when lancés, then code de sortie 0 pour chacun
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-002-03 | FR-002-04 | FR-002-05 | BUG-005'` et `cd frontend && npx vitest run -t 'FR-002-0[1-6]|FR-002-11|FR-003-12|BUG-004'`, when lancés, then code 0 chacun
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='BUG-005'` avec le premier compte volontairement repris, when lancé, then code non nul
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh`, when lancés, then code 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 0 findings — high 0, medium 0, low 0, false 0, maybe-false 0
- findings:
  - l'audit d'alignement (descriptif) retient la lecture A (API + écran de bout en bout) et ne relève aucun défaut ; ses remarques de surface (tests d'écran sur client simulé, parallélisme non observé en vol, scénario « virement puis redirection » couvert par l'ordre du tri, rejeu golden master hors périmètre) sont conformes à la spec ; la présence des tests `[FR-002-01]`, `[FR-002-11]`, `[BUG-004]` dans le front a été vérifiée par recherche.

## Design Notes

- Le tableau de bord est le seul écran des deux services appelé en parallèle : le gabarit charge `/moi`, l'écran charge `GET /tableau-de-bord` au même rendu (pas d'attente du profil). Un 401 de l'un ou de l'autre suit le chemin existant (signal d'expiration, jeton supprimé, `/login`).
- Format : `EUR` + entier groupé par virgule + `.` + 2 décimales (ex. `EUR2,130.00`) ; calcul en centimes entiers.
- Les données de référence ne contiennent aucune opération pour `user1` : ne pas les compléter (« Socle du rejeu final »).

## Verification

**Commands:**
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-002-01` et `SC-003-04` -- expected: code 0
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-002-03 | FR-002-04 | FR-002-05 | BUG-005'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-002-0[1-6]|FR-002-11|FR-003-12|BUG-004'` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** `GET /api/coeur-bancaire/tableau-de-bord` (identité du jeton, comptes avec solde, solde total, 5 dernières opérations du COURANT choisi par son type, BUG-005) ; écran `/tableau-de-bord` (séparateur, solde total, cartes de comptes, « Activite recente / Dernieres operations », bouton vers `/releve`, « Suivi de mes virements » en dernier), chargement en parallèle de `/moi`, formats `EURx,xxx.xx` et `dd/MM/yyyy HH:mm`.

**Fichiers :** `services/coeur-bancaire/**` (port `ConsulterTableauDeBord`, cas d'usage, contrôleur, assemblage, tests `ConsulterTableauDeBordCasUsageTest`, `ReleveIT`, `CoeurBancaireSecuriteIT` ajusté) ; `frontend/src/**` (`domain/tableauDeBord.ts`, `application/consulterTableauDeBord.ts`, `adapters/ecrans/tableauDeBord/`, `adapters/http/ClientTableauDeBordHttp.ts`, route, textes, assemblage, tests).

**Revue :** 0 patch, 0 différé, 0 rejet. Recommandation de nouvelle revue : `false`.

**Vérification (code 0) :** `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` ; `verifier-scenario.sh SC-002-01` et `SC-003-04` ; `mvn verify -Dgroups='FR-002-03 | FR-002-04 | FR-002-05 | BUG-005'` ; `vitest -t 'FR-002-0[1-6]|FR-002-11|FR-003-12|BUG-004'`. Prendre le premier compte à la place du COURANT fait échouer `-Dgroups='BUG-005'` (code 1), restauré ensuite.

**Risques résiduels :** « SOLDE TOTAL DEMO BANK », « ACTIVITE RECENTE » et l'en-tête en capitales du golden master restent consignés « non trouvés » par `verifier-scenario.sh` (non bloquant : capitales par CSS, source en casse normale) ; tableau vide sans texte (le legacy n'en définit pas, point ouvert) ; « [EN ATTENTE] » non traité sur le tableau de bord ; un premier `build.sh` a échoué sur la couverture JaCoCo d'`identite-clients` (non touché), vert au relancement : instabilité possible.
