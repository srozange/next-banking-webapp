---
title: 'Afficher le patrimoine total avec les comptes externes sur le tableau de bord'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: '3011e0c0735c92026511126fdbc2930f3dc2a8a5'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-8-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-8-4-afficher-l-ecran-comptes-tiers.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** le tableau de bord n'affiche que Demo Bank : l'utilisateur ne voit pas son patrimoine total (Demo Bank + comptes externes Powens), ni d'invitation à connecter ses autres banques (FR-002-12).

**Approach:** le front lit l'état Powens (`GET /api/open-banking-conseiller/powens`, lecture en base, aucun appel à Powens) en parallèle de l'appel au cœur bancaire, calcule le patrimoine en centimes entiers dans `src/domain`, et ajoute une section après le contenu comparé du tableau de bord.

## Boundaries & Constraints

**Always:**
- Hexagone front : `domain` et `application` en TypeScript pur (ni npm, ni `Date.now()`/`new Date()`), couverture 100 % ; React seulement dans `adapters`.
- Réutiliser le port `ClientComptesTiers.lireEtat` (8.4) ; l'appel part au rendu, dans le même instant que l'appel du cœur bancaire, jamais après lui.
- Patrimoine = total Demo Bank (somme des soldes du cœur bancaire, centimes entiers) + `patrimoineTotal` d'`open-banking-conseiller` (chaîne `"120.50"`, soldes absents déjà exclus), formaté au format du tableau de bord (`formaterMontantTableau`, « EUR1,120.50 »).
- Textes legacy à l'identique (casse, absence d'accents) dans `TEXTES.tableauDeBord` : « Mes comptes externes (Open Banking) », « Patrimoine total », « Demo Bank + comptes externes », « Comptes externes via Powens Open Banking (N comptes) », « Voir le detail et les transactions » ; non connecté : « Open Banking », « Agregez vos comptes d autres banques pour une vue complete de votre patrimoine », « Connecter mes autres banques via Powens ». Rien d'inventé.
- `data-testid` `dashboard-{élément}` sur tout élément interactif ; boutons menant à `/comptes-tiers`.
- Les sections se placent après le contenu comparé (après « Voir le releve complet »), avant « Centre de notifications » ; aucun texte ni élément inséré avant (fenêtre de 5 000 caractères du golden master).

**Never:**
- Pas de nouveau paquet npm, pas de `vitest.config.*`, pas de modification du service, ni de `scripts/gates/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `sprint-status.yaml`, ni du golden master (`e2e/`, `docs/retro/golden-master/**`, `replay-mapping.json`) ; ne jamais le rejouer.
- Aucune somme fausse : si l'état Powens échoue ou si le cœur bancaire échoue, la carte « Patrimoine total » et la section n'apparaissent pas.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Connecté | Demo Bank 1 000,00 ; Powens `connecte`, `patrimoineTotal "120.50"`, 3 comptes | section « Mes comptes externes (Open Banking) », carte « Patrimoine total » « EUR1,120.50 », « Demo Bank + comptes externes », « Comptes externes via Powens Open Banking (3 comptes) », bouton → `/comptes-tiers` | — |
| Non connecté | `connecte=false` | séparateur « Open Banking », invitation, bouton « Connecter mes autres banques via Powens » → `/comptes-tiers` | — |
| Powens en échec | 503, injoignable, réponse illisible | reste du tableau de bord normal ; ni section ni somme | rien d'affiché pour Powens |
| Cœur bancaire en échec | Powens ok | message technique habituel, pas de patrimoine | — |
| Ordre des appels | rendu | `lireEtat` appelé avec `lire` avant toute résolution | — |
| Connecté sans compte | `comptes: []`, total "0.00" | patrimoine = total Demo Bank, « (0 comptes) » | — |

</intent-contract>

## Code Map

- `frontend/src/domain/tableauDeBord.ts` -- `formaterMontantTableau`, `soldeTotalCentimes` ; ajouter `patrimoineTotalCentimes(soldeDemoBankCentimes, patrimoineExterne)` (`centimesDepuisChaine` de `domain/montant.ts`).
- `frontend/src/application/consulterTableauDeBord.ts` -- `chargerTableauDeBord` ; `TableauDeBord` reçoit `soldeTotalCentimes` ; ajouter le cas d'usage `chargerPatrimoineExterne(client: ClientComptesTiers)` (état → `{connecte, nombreComptes, patrimoineTotal}` ou null sur tout échec ; lecture directe de `lireEtat`, 401 géré par `fetchAuthentifie`).
- `frontend/src/application/comptesTiers.ts`, `domain/comptesTiers.ts` -- port `ClientComptesTiers`, `EtatComptesTiers` (lecture seule).
- `frontend/src/adapters/ecrans/tableauDeBord/EcranTableauDeBord.{tsx,css,test.tsx}` -- nouvelle propriété `clientComptesTiers` ; deux effets lancés au rendu ; nouvelle section.
- `frontend/src/adapters/routes/routes.tsx` (ligne ~134) -- passer `clientComptesTiers` à l'écran ; `adapters/test/rendreApplication.tsx` (`tiers`, positionnel `clientComptesTiers`) ; `adapters/textes.ts` (`tableauDeBord`).
- Tests d'architecture à garder verts : `src/{motifs-interdits,architecture-temps,couleurs-hors-tokens}.test.ts`, `adapters/accessibilite.test.tsx`, `routes.test.tsx`.
- Exigences : `epics.md` « Story 8.5 » ; `docs/retro/features/FEAT-002/MESSAGES.md` (MSG-002-070 à 077) ; tags de test `[FR-002-12]`.

## Tasks & Acceptance

**Execution:**
- `frontend/src/domain/tableauDeBord.ts` (+ test) -- `patrimoineTotalCentimes` en centimes entiers -- 100 % couvert
- `frontend/src/application/consulterTableauDeBord.ts` (+ test) -- `soldeTotalCentimes` dans `TableauDeBord`, `chargerPatrimoineExterne` -- 100 % couvert
- `frontend/src/adapters/textes.ts` -- textes MSG-002-070 à 077
- `frontend/src/adapters/ecrans/tableauDeBord/EcranTableauDeBord.{tsx,css}` -- section Powens après le contenu comparé, boutons vers `/comptes-tiers`
- `frontend/src/adapters/routes/routes.tsx`, `adapters/test/rendreApplication.tsx` -- câblage
- `frontend/src/adapters/ecrans/tableauDeBord/EcranTableauDeBord.test.tsx` -- cas de la matrice, titres taggés `[FR-002-12]`, test d'ordre des appels, clic vers `/comptes-tiers`

**Acceptance Criteria:**
- Given la matrice ci-dessus, when `/tableau-de-bord` s'affiche, then textes, montants, `data-testid` et navigation attendus
- Given un visiteur sans jeton, when `/tableau-de-bord`, then `/login`
- Given la story terminée, when `cd frontend && npx vitest run -t 'FR-002-12'`, then code 0 et `grep -rl '\[FR-002-12\]' frontend/src` renvoie un fichier de test
- Given `scripts/golden-master/verifier-scenario.sh` pour SC-001-04, SC-001-05, SC-002-01, SC-002-02, then code 0
- Given la story terminée, when `scripts/gates/{build,check-gates-exist,golden-master,trace-smoke}.sh`, then code 0

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 6 findings — high 0, medium 0, low 1, false 5, maybe-false 0
- findings:
  - `low` `patch` Commentaires de `Proprietes` entremêlés (le commentaire de `clientNotifications` placé au-dessus de `clientComptesTiers`) — réordonnés dans `EcranTableauDeBord.tsx`.
  - `false` `reject` Réponse 401 de l'API non assertée dans le diff — le point d'API relève de 8.3 (`ConfigurationSecuriteTest`, `PowensIT` couvrent `/powens`) ; ici, le routeur redirige vers `/login`, testé.
  - `false` `reject` « Aucun appel à Powens » non vérifié côté front — propriété du service (lecture en base, 8.3), hors de ce diff.
  - `false` `reject` Ordre des appels non relatif — le test vérifie que `lire` et `lireEtat` partent au rendu avant toute résolution : jamais en cascade.
  - `false` `reject` Fixture 120,50 + 0,00 + sans solde non reconstruite — le total exclut déjà les soldes absents côté service (contrat 8.3) ; le front additionne le total reçu.
  - `false` `reject` Contrôle golden master hors diff — `verifier-scenario.sh` SC-001-04, SC-001-05, SC-002-01, SC-002-02 : code 0.

## Verification

**Commands:**
- `cd frontend && npx vitest run` -- expected: code 0, seuils de couverture tenus
- `for s in SC-001-04 SC-001-05 SC-002-01 SC-002-02; do scripts/golden-master/verifier-scenario.sh $s; done` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh && scripts/gates/trace-smoke.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** le tableau de bord lit l'état Powens en parallèle du cœur bancaire et, après le contenu comparé, affiche soit « Mes comptes externes (Open Banking) » avec la carte « Patrimoine total » (Demo Bank + comptes externes, centimes entiers) et le bouton vers `/comptes-tiers`, soit l'invitation « Connecter mes autres banques via Powens ». Si Powens ou le cœur bancaire échoue, aucune somme n'est montrée.

**Fichiers :** `frontend/src/domain/tableauDeBord.ts` (`patrimoineTotalCentimes`), `application/consulterTableauDeBord.ts` (`soldeTotalCentimes`, `chargerPatrimoineExterne`), `adapters/ecrans/tableauDeBord/EcranTableauDeBord.tsx` (section), `adapters/routes/routes.tsx` (câblage), `adapters/textes.ts` (MSG-002-070 à 077), et les tests correspondants taggés `[FR-002-12]`.

**Revue :** 6 constats, 1 correctif (commentaires), 0 différé, 5 rejetés comme faux (voir le journal). Revue complémentaire recommandée : false.

**Vérification :** `vitest -t FR-002-12` 17 tests verts ; `verifier-scenario.sh` SC-001-04, SC-001-05, SC-002-01, SC-002-02 code 0 ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` code 0.

**Risques résiduels :** `data-testid` choisis (`dashboard-externe-*`, `dashboard-patrimoine-*`, `dashboard-openbanking-*`) faute de valeurs imposées ; format monétaire « EUR1,120.50 » aligné sur le tableau de bord existant.
