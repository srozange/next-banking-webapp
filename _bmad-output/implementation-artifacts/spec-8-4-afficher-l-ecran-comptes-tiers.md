---
title: 'Afficher l''écran Comptes Tiers'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: 'da00dbd537fa574cee8866f0f5648efe0ae88f38'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-8-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-8-3-consulter-selectionner-rafraichir-et-deconnecter-ses-comptes.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** `/comptes-tiers` est un lien d'en-tête sans écran : le service `open-banking-conseiller` (8.1 à 8.3) sait connecter une banque, lire, sélectionner, rafraîchir et déconnecter les comptes externes, mais aucun utilisateur ne peut le voir ni l'utiliser (FR-012-01 à FR-012-16).

**Approach:** Ajouter au front hexagonal la route protégée `/comptes-tiers` et son écran : règles pures et cas d'usage (domain, application), port `ClientComptesTiers` et adaptateur HTTP, écran React. Même contenu pour CLIENT, CONSEILLER et ADMIN.

## Boundaries & Constraints

**Always:**
- Hexagone front : `src/domain` et `src/application` en TypeScript pur (aucun import npm, ni `Date.now()`/`new Date()`), couverture 100 % ; tout le React dans `src/adapters`.
- Montants : chaînes `"120.50"` du service, formatés par centimes entiers (`formaterCentimes`/`formaterMontantListe` de `domain/pret.ts`, format du legacy « EUR1,234.50 ») ; solde `null`/`soldeAbsent` : « Solde indisponible », jamais « 0.00 » ; le patrimoine total est celui de l'état (soldes absents déjà exclus).
- Textes legacy à l'identique (`docs/retro/features/FEAT-012/MESSAGES.md`, casse et absence d'accents comprises), messages de l'epic (`epic-8-context.md`) ; tous dans `TEXTES.comptesTiers` (`adapters/textes.ts`) ; rien d'inventé ; le bandeau technique « Open Banking — … EJB WildFly 14 » n'existe pas (B-29).
- Titre d'onglet « BankApp Demo - Comptes Tiers » ; `data-testid` `tiers-{élément}[-{id}]` sur tout élément interactif (`tiers-connecter-banque`, `tiers-deconnecter`, `tiers-rafraichir`, `tiers-message-erreur`, `tiers-compte-{compteId}`).
- Appels par `fetchAuthentifie` (tout 401 → `/login`), `Authorization: Bearer`, chemins `/api/open-banking-conseiller/powens` : `GET` (état), `POST /connexion` (`{adresse}`), `POST /comptes/{compteId}/selection`, `POST /rafraichissement`, `DELETE /connexion` (204). État : `{connecte, comptes[{id,nom,type,solde,soldeAbsent,iban}], compteSelectionne, transactions[{date,libelle,montant}], message, patrimoineTotal}`. Erreurs `{"message"}` ; 503 = Powens indisponible.
- Le message de l'état s'affiche tel quel dans `tiers-message-erreur`, au-dessus de l'état courant, absent s'il est vide ; aucun jeton Powens affiché ; rien de l'état Powens dans `localStorage`/`sessionStorage`.
- Couleurs vert/rouge uniquement par les variables de `tokens.css`.

**Never:**
- Pas de nouveau paquet npm ni de `vitest.config.*` ; pas de texte inventé ; pas de modification du service ni de l'écran du tableau de bord (8.5).
- Ne pas modifier `scripts/gates/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `sprint-status.yaml`, ni le golden master (`e2e/`, `docs/retro/golden-master/**`, `replay-mapping.json`) ; ne jamais le rejouer.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Non connecté | `connecte=false` | bloc « Agregez vos comptes bancaires », texte, 5 banques + « + 1800 banques », bouton « Connecter une banque » | — |
| Connexion | clic | `POST …/connexion` puis navigateur envoyé à `adresse` | 503/panne : « Impossible d initialiser la connexion Powens. » dans `tiers-message-erreur`, pas de changement d'adresse |
| Connecté | 2 comptes, premier sélectionné | bloc de connexion masqué, « Connected via Powens », « Deconnecter », « Rafraichir », « Patrimoine total (tous comptes) » + « via Powens Open Banking API », « Mes comptes externes (2) », « Transactions — {nom} » (Date, Libelle, Montant) | — |
| Cartes | types checking…market, absent, autre | « Banque externe », nom, type traduit (« Compte courant », « Epargne », « Carte », « Credit », « Bourse », « Compte », valeur brute), solde, IBAN si non vide | — |
| Soldes | sans solde, 0, 120,50 | « Solde indisponible », « EUR0.00 », « EUR120.50 » ; total 120,50 | — |
| Sélection | clic sur un autre compte | seule la section des transactions change (≤ 20 lignes, > 0 vert « + », sinon rouge) ; vide : « Aucune transaction disponible. » | 503 : message d'indisponibilité à la place, jamais « Aucune transaction disponible. » |
| Retour en échec | non connecté + message (user_cancelled, 503 Powens, session expirée, incomplet) | bloc de connexion + message ; ni « Mes comptes externes (0) » ni « Connexion reussie… » pour l'indisponibilité | — |
| Connecté sans compte | `comptes: []`, message « Connexion reussie… » | « Mes comptes externes (0) » et le message | — |
| Rafraîchir / Déconnecter | clics | rafraîchissement : état remplacé ; déconnexion : retour au bloc de connexion, sans comptes, transactions ni message | 503 au rafraîchissement : message d'indisponibilité, état conservé |
| Session perdue | 401 | jeton supprimé, `/login` | — |

</intent-contract>

## Code Map

- `frontend/src/adapters/routes/routes.tsx` -- ajouter la route `/comptes-tiers` comme `/bourse` (littéral `'/comptes-tiers'` cherché par `scripts/golden-master/verifier-scenario.sh SC-012-01`) ; `Dependances` reçoit `clientComptesTiers`. `ROUTES_PROTEGEES` inchangé.
- `frontend/src/infrastructure/main.tsx`, `adapters/test/rendreApplication.tsx` -- assemblage et aide de rendu (modèle : `clientBourse`).
- `frontend/src/domain/pret.ts` -- `formaterCentimes`, `formaterMontantListe`, `formaterDateEcheance` à réutiliser ; `domain/montant.ts` (`centimesDepuisChaine`).
- Modèles : `application/consulterBourse.ts` (`jetonOuFin`, port, résultats), `adapters/http/ClientBourseHttp.ts`, `adapters/ecrans/bourse/EcranBourse.{tsx,css}`, `adapters/ui/*`, `adapters/gabarit/identifiants.ts` (`nav-comptes-tiers` existant), `adapters/textes.ts` (`titresOnglet`).
- Tests d'architecture à garder verts : `src/{motifs-interdits,architecture-temps,couleurs-hors-tokens}.test.ts`, `adapters/accessibilite.test.tsx`, `adapters/routes/routes.test.tsx`.
- Contrat service (lecture seule) : `services/open-banking-conseiller/.../adapter/in/http/PointEntreePowens.java`.
- Exigences : `epics.md` « Story 8.4 » ; `docs/retro/features/FEAT-012/` (MESSAGES, RG) ; scénario `docs/retro/golden-master/derived/FEAT-012/SC-012-01` (start_url `/comptes-tiers`, textes « Agregez vos comptes bancaires », « BNP Paribas », « Credit Agricole », « OAuth2 PSD2 »). Tags de tests = `[FR-012-NN]`, `[BUG-0NN]` dans le titre des `it`/`describe`.

## Tasks & Acceptance

**Execution:**
- `frontend/src/domain/comptesTiers.ts` (+ test) -- types, libellé de type, sens et format d'un montant de transaction, format d'un solde -- 100 % couvert
- `frontend/src/application/comptesTiers.ts` (+ test) -- port `ClientComptesTiers`, cas d'usage charger / connecter / sélectionner / rafraîchir / déconnecter (401 → fin de session) -- 100 % couvert
- `frontend/src/adapters/http/ClientComptesTiersHttp.ts` (+ test) -- adaptateur HTTP, statuts traduits (401, 503, technique), jamais de jeton journalisé
- `frontend/src/adapters/ecrans/comptesTiers/EcranComptesTiers.{tsx,css}` (+ test) -- écran, tests par rôle, tags FR-012-01/03/04/05/07/08/09/10/11/12/13/14/15/16, BUG-040/042/043/044
- `frontend/src/adapters/{textes.ts,routes/routes.tsx,routes/routes.test.tsx,test/rendreApplication.tsx}`, `infrastructure/main.tsx` -- textes, route, assemblage
- Vérifier : `verifier-scenario.sh SC-012-01` code 0 ; route retirée → code non nul nommant SC-012-01, puis remise

**Acceptance Criteria:**
- Given un visiteur sans jeton, when `/comptes-tiers`, then `/login` ; Given CLIENT, CONSEILLER, ADMIN, then même contenu
- Given les cas de la matrice, when l'écran s'affiche ou réagit, then les textes, `data-testid` et appels HTTP attendus, sans rien dans `localStorage`/`sessionStorage` hors jeton de session
- Given la story terminée, when `cd frontend && npx vitest run -t 'FR-012-01|…|BUG-044'`, then code 0 et chaque identifiant a au moins un `[<identifiant>]` dans `frontend/src`
- Given la story terminée, when `scripts/gates/{build,check-gates-exist,golden-master,trace-smoke}.sh`, then code 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 5 findings — high 0, medium 0, low 0, false 5, maybe-false 0
- findings:
  - `false` `reject` Tests sur ports simulés, sans le vrai service — la story est front ; chemins et JSON de `ClientComptesTiersHttp` relus contre `PointEntreePowens.java` (mêmes chemins, `{adresse}`, `ReponseEtat`, montants à 2 décimales) : aucun écart.
  - `false` `reject` Redirection vers Powens jamais suivie réellement — le retour public relève de 8.2 ; `allerVers` est substituable et testé.
  - `false` `reject` Vérification golden master/gates non visible dans le diff — `verifier-scenario.sh SC-012-01` et les quatre contrôles rejoués : code 0 ; route retirée → code 1 nommant SC-012-01.
  - `false` `reject` Textes assertés depuis `TEXTES` et non depuis MESSAGES.md — relus contre MSG-012-040 à 063 : identiques, casse et absence d'accents comprises.
  - `false` `reject` Intégration au tableau de bord absente (lecture D) — exclue par l'intention (story 8.5).

## Design Notes

Format monétaire : celui de `domain/pret.ts` (anglo-saxon du legacy, « EUR1,234.50 »), pour rester cohérent avec les écrans livrés ; les critères de l'epic écrivent « 0,00 » sans précision de séparateur. La date d'une transaction s'affiche « jj/MM/aaaa ». La navigation vers l'adresse Powens passe par une propriété de l'écran (`window.location.assign` par défaut), substituable en test.

## Verification

**Commands:**
- `cd frontend && npx vitest run` -- expected: code 0, seuils de couverture tenus
- `scripts/golden-master/verifier-scenario.sh SC-012-01` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh && scripts/gates/trace-smoke.sh` -- expected: code 0


## Auto Run Result

Status: done

**Résumé :** l'écran `/comptes-tiers` est livré dans le front hexagonal : bloc de connexion (banques, « Connecter une banque » → navigateur envoyé à l'adresse Powens, 503 → « Impossible d initialiser la connexion Powens. »), état connecté (patrimoine total, cartes de comptes avec type traduit, « Solde indisponible », IBAN conditionnel), transactions du compte sélectionné (20 au plus, vert « + » / rouge, vide, 503 → message d'indisponibilité), rafraîchir, déconnecter, message d'état dans `tiers-message-erreur`. Même contenu pour CLIENT, CONSEILLER et ADMIN ; 401 → `/login`.

**Fichiers :** `domain/comptesTiers.ts`, `application/comptesTiers.ts`, `adapters/http/ClientComptesTiersHttp.ts`, `adapters/ecrans/comptesTiers/EcranComptesTiers.{tsx,css}` (avec leurs tests) ; `adapters/{textes.ts,routes/routes.tsx,routes/routes.test.tsx,test/rendreApplication.tsx,accessibilite.test.tsx}`, `ui/BoutonPrimaire.tsx` (propriété `onClick` optionnelle), `infrastructure/main.tsx`.

**Revue :** 5 constats, 0 correctif, 0 différé ; 5 rejetés comme faux (voir le journal).

**Recommandation de revue complémentaire :** false.

**Vérification :** `npx vitest run` 786 tests verts ; `verifier-scenario.sh SC-012-01` code 0 (route retirée : code 1 nommant SC-012-01) ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` code 0.

**Risques résiduels :** après un 503 à la sélection, le titre des transactions suit le compte cliqué alors que le service garde l'ancien compte sélectionné (rétabli au prochain clic réussi ou rafraîchissement) ; format monétaire anglo-saxon (« EUR1,234.50 ») aligné sur l'écran Prêts, alors que l'epic écrit « 0,00 ».

**Réparation après échec de vérification :** `build.sh` avait échoué sur un délai dépassé (5 s) de `src/infrastructure/main.test.ts` (import dynamique de tout l'arbre applicatif sous charge) ; le test passe seul et `build.sh` repasse à 0. Délai explicite de 30 s ajouté à ce test, sans toucher au contrat d'intention ni aux seuils.
