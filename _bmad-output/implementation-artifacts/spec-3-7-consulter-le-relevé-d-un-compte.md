---
title: 'Consulter le relevé d''un compte'
type: 'feature'
created: '2026-10-06'
baseline_revision: '41e9049851d3b533436f34340fbde8815ddd9e24'
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

**Problem:** le front n'a pas d'écran `/releve` (la route du lien « Releve » n'existe pas) et le cœur bancaire n'expose ni le solde des comptes ni la liste des opérations d'un compte : un client ne peut pas suivre ses débits et ses crédits.

**Approach:** livrer l'écran `/releve` (sélecteur « Compte : », panneau d'informations, tableau des opérations) et l'API qui l'alimente : `GET /api/coeur-bancaire/comptes` renvoie aussi `solde`, nouveau `GET /api/coeur-bancaire/comptes/{compteId}/operations`. Le texte exact des critères est la story 3.7 de `_bmad-output/planning-artifacts/epics.md` (lignes 2677-2737) et FEAT-005 du PRD (FR-005-01 à FR-005-08) : la lire en entier.

## Boundaries & Constraints

**Always:** hexagone (`domain`/`application` Java pur et TypeScript pur, 100 % lignes et branches) ; montants `BigDecimal` 2 décimales, chaîne `"100.00"` en JSON, front en centimes entiers (jamais de flottant) ; propriété du compte contrôlée dans le service (403 compte d'autrui, 404 compte inexistant, corps `{"message"}`, aucune opération renvoyée) ; tri par date décroissante décidé et testé dans `application` (pas seulement en SQL) ; tous les textes du legacy (`docs/retro/features/FEAT-005/{FIELDS,MESSAGES}.md`), aucun texte inventé ; couleurs par les tokens du thème (CREDIT `success`, DEBIT `error`, ligne en attente `warning-container`) ; `data-testid` `releve-*` ; date `dd/MM/yyyy HH:mm` en Europe/Paris sans `Date.now()`/`new Date()` dans `domain` et `application` du front ; tests tagués `FR-005-01` à `FR-005-08` (Java : `@Tag("FR-005-0x")` ; Vitest : `[FR-005-0x]` dans le titre) ; ajouter `solde` à `GET /comptes` sans casser les tests de la 3.1 ; versions de `versions-verifiees.md`.

**Never:** modifier `scripts/gates/**`, `docs/quality-gates.md`, `.claude/**`, `.bmad-loop/**`, `frontend/.dependency-cruiser.cjs`, seuils, exclusions de couverture ; désactiver un test ; toucher `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json` ; rejouer le golden master ; bandeau @NamedQuery (B-22) ; appel HTTP entre services ; tableau de bord (3.8) ; écrire `sprint-status.yaml`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / état | Comportement attendu | Erreur |
|----------|--------------|---------------------|--------|
| Sans session | `/releve` sans jeton ; `GET /comptes` sans jeton | redirection `/login` ; 401 | — |
| Ouverture | `user1`, comptes 1 et 2 | onglet « BankApp Demo - Relevé de compte », section « Relevé de compte », `<select>` « Compte : » limité à ses comptes, premier compte sélectionné, opérations chargées, « COURANT — N° FR76-0001-0001 — Solde actuel : … € », pas de bandeau | — |
| Sans compte | CONSEILLER/ADMIN | sélecteur sans option, aucune opération | — |
| Changement de compte | 1 → 2 | comptes rechargés (soldes à jour) puis opérations du 2, panneau et tableau rafraîchis sans rechargement, « {N} opération(s) » dans une région `aria-live="polite"` | — |
| Tableau | compte avec opérations | toutes, date décroissante, compteur, colonnes Date/Description/Type/Montant/Solde après (casse source, majuscules par CSS), `dd/MM/yyyy HH:mm` Europe/Paris, `+` CREDIT, `-` ASCII DEBIT, solde après sans signe | — |
| Compte vide | aucune opération | « Aucune transaction pour ce compte. » sous les en-têtes conservés | — |
| En attente | libellé « [EN ATTENTE] x » | badge « En attente », libellé sans les 13 premiers caractères, fond `warning-container` ; sans préfixe : ni badge ni fond | — |
| Compte d'autrui / inexistant | `user1` demande le compte d'un autre client, puis 999 | 403 puis 404 `{"message"}`, aucune opération | écran : texte technique, valeurs conservées |
| Navigation | CLIENT, CONSEILLER, ADMIN | « Espace Conseiller » : CONSEILLER et ADMIN seulement ; « Administration » : ADMIN seulement | — |
| Tri inversé | tri croissant volontaire | `mvn test -Dgroups='FR-005-05'` échoue | — |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/adapter/in/PointEntreeVirements.java` -- contient déjà `GET /comptes` (`CompteClient(id,type,numero)`) : ajouter `solde` (chaîne `toPlainString`) et `GET /comptes/{compteId}/operations` ; `GestionErreursVirements` (même paquet) à compléter pour la nouvelle erreur.
- `.../application/{ConsulterComptesCasUsage,port/in/ConsulterComptes,port/out/{Comptes,Operations}}` -- modèles ; ajouter port entrant `ConsulterReleve`, cas d'usage, `Comptes.parId`, `Operations.duCompte`.
- `.../domain/{Compte,Operation,TypeOperation,ErreurVirement,TextesVirement}` -- `Operation` n'a pas d'id : le tri par date décroissante est stable, le port rend déjà date puis id décroissants. Nouvelle erreur de domaine pour le relevé (causes introuvable → 404 « Ressource introuvable », étranger → 403 « Accès refusé », textes déjà présents dans le service).
- `.../adapter/out/persistance/{ComptesJdbc,OperationsJdbc}` + `V2__comptes_operations_virements.sql` (index `operations_par_compte (compte_id, date_operation desc, id desc)`) ; tests modèles : `ComptesJdbcIT`, `VirementIT`, `CoeurBancaireSecuriteIT`, `MemoireCoeurBancaire`, `ConsulterComptesCasUsageTest`, `GestionErreursVirementsTest`.
- `.../infrastructure/AssemblageVirement.java` -- bean du nouveau cas d'usage.
- `frontend/src/adapters/routes/routes.tsx` (`Dependances`, route `/releve`), `adapters/textes.ts` (`TEXTES.titresOnglet`, nouveau bloc `releve`), `adapters/http/ClientVirementHttp.ts` + `application/virement.ts` (`Compte` sans solde : ajouter `solde`), `adapters/ecrans/virement/EcranVirement.tsx` (modèle d'écran), `adapters/ui/{ListeDeroulante,Etiquette,Carte}`, `adapters/ecrans/bourse/EcranBourse.{tsx,css}` (modèle de tableau), `domain/montant.ts`, `adapters/test/rendreApplication.tsx`, `infrastructure/main.tsx`, `domain/navigation.ts` (lien « Releve » déjà là).
- `scripts/golden-master/verifier-scenario.sh SC-005-01` -- statique : route `/releve` littérale dans `routes/*.tsx` ; ne rien y modifier.
- `docs/retro/features/FEAT-005/{FIELDS,MESSAGES,RG}.md`, `_bmad-output/planning-artifacts/ux-designs/*/textes-nouveaux.md` -- textes et formats.

## Tasks & Acceptance

**Execution:**
- `services/coeur-bancaire/**` (ports, cas d'usage `ConsulterReleve` avec tri, erreur de domaine, JDBC, contrôleur, gestion d'erreurs, assemblage, solde dans `GET /comptes`) -- API du relevé -- ACs API et sécurité
- `services/coeur-bancaire/src/test/**` -- tests domaine/application (100 %), JDBC (IT), contrôleur/sécurité (401, 403, 404), `@Tag("FR-005-0x")` dont `FR-005-05` pour le tri -- matrice
- `frontend/src/{domain,application}` -- lecture des opérations (centimes, préfixe « [EN ATTENTE] », signes, formats) avec ses tests -- règles pures
- `frontend/src/adapters/**` (écran `EcranReleve`, client HTTP, textes, route `/releve`, assemblage, `rendreApplication`) et ses tests Vitest `[FR-005-0x]` -- écran
- Ne toucher à aucun contrôle ni exclusion.

**Acceptance Criteria:**
- Given la story 3.7 d'`epics.md`, when chacun de ses critères est exécuté, then il est satisfait tel que rédigé
- Given `scripts/golden-master/verifier-scenario.sh SC-005-01`, when lancé, then code de sortie 0
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-005-01 | FR-005-02 | FR-005-04 | FR-005-05'` et `cd frontend && npx vitest run -t 'FR-005-0[1-8]'`, when lancés, then code 0 chacun
- Given `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh`, when lancés, then code 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 0 findings — high 0, medium 0, low 0, false 0, maybe-false 0
- findings:
  - l'audit d'alignement (descriptif) retient la lecture A (écran + API de bout en bout) et n'a relevé aucun défaut ; les écarts de surface qu'il cite (scripts de gates non modifiables, rendu CSS non calculé en jsdom, tags `FR-005-03/06/07/08` portés par Vitest seulement) sont conformes à la spec. L'inversion du tri a été vérifiée à la main : `mvn test -Dgroups='FR-005-05'` sort en 1 tri inversé, 0 tri normal.

## Design Notes

- Date au format `dd/MM/yyyy HH:mm` : l'adaptateur formate l'instant ISO reçu avec `Intl.DateTimeFormat` en `Europe/Paris` ; le domaine/application du front ne manipule que des chaînes et des centimes. Montant affiché « € » après formatage fr (virgule), 2 décimales au moins.
- Le rechargement au changement de compte appelle d'abord `lireComptes` (soldes à jour) puis `lireOperations(compteId)`.

## Verification

**Commands:**
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh`, `scripts/gates/trace-smoke.sh` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-005-01` -- expected: code 0
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-005-01 | FR-005-02 | FR-005-04 | FR-005-05'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-005-0[1-8]'` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** relevé d'un compte : `GET /api/coeur-bancaire/comptes` renvoie aussi `solde` ; nouveau `GET /api/coeur-bancaire/comptes/{compteId}/operations` (403 compte d'autrui, 404 compte inexistant, format `{"message"}`, tri par date décroissante décidé dans `application`) ; écran `/releve` du front (sélecteur « Compte : », informations, compteur `aria-live`, tableau, badge « En attente », tokens du thème).

**Fichiers :** `services/coeur-bancaire/**` (port `ConsulterReleve`, `ConsulterReleveCasUsage`, `ErreurReleve`, `Comptes.trouver`, `Operations.duCompte`, JDBC, contrôleur, gestion d'erreurs, assemblage, tests dont `ReleveIT`) ; `frontend/src/**` (`domain/releve.ts`, `application/consulterReleve.ts`, `adapters/ecrans/releve/`, `adapters/http/ClientReleveHttp.ts`, route, textes, assemblage, tests).

**Revue :** 0 patch, 0 différé, 0 rejet. Recommandation de nouvelle revue : `false`.

**Vérification (relancée à neuf, code 0) :** `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` ; `verifier-scenario.sh SC-005-01` ; `mvn verify -Dgroups='FR-005-01 | FR-005-02 | FR-005-04 | FR-005-05'` ; `vitest -t 'FR-005-0[1-8]'` ; tri inversé : `mvn test -Dgroups='FR-005-05'` sort en code non nul, puis 0 après restauration.

**Risques résiduels :** méthode nommée `Comptes.trouver` (et non `parId`, conflit avec `DepotVirements.parId` dans `MemoireCoeurBancaire`) ; format des montants `+100,00 €` (espaces insécables) choisi sans précision de la spec ; l'en-tête « DATE DESCRIPTION … SOLDE APRÈS » n'existe qu'en capitales dans le dérivé, le script le consigne sans échouer ; `trace-smoke.sh` a tourné sur la pile déjà démarrée.
