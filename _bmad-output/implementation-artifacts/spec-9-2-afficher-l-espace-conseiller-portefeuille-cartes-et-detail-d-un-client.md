---
title: "Afficher l'Espace Conseiller (portefeuille, cartes et détail d'un client)"
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: '404a3e842b7258d94cee66108532e2351b0f8cb8'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-9-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** la route `/espace-conseiller` n'affiche qu'un titre d'onglet : le conseiller ne voit ni son portefeuille, ni ses indicateurs, ni le détail d'un client, alors que les trois points `…/conseiller/portefeuille` existent depuis la story 9.1.

**Approach:** un écran front `EcranEspaceConseiller` qui lit en parallèle les trois points (identité, comptes, prêts), les joint par `clientId` dans `src/domain`, affiche bandeau, trois indicateurs, une carte par client et un panneau de détail inline ; le résultat est gardé pour toute la session dans une mémoire portée par le gabarit authentifié (démonté à la déconnexion).

## Boundaries & Constraints

**Always:**
- Trois appels lancés ensemble (`Promise.all`), jamais en cascade ; un 401 de l'un termine la session (motif `chargerProduits`). Le jeton n'est posé que par `fetchAuthentifie` (câblage dans `main.tsx`).
- Chargement une fois par session : retour sur la route sans nouvel appel ; après déconnexion puis reconnexion, rechargé. Un résultat partiellement en erreur n'est pas mémorisé (nouvel essai au retour).
- Ordre des cartes = ordre de la réponse d'identité (déjà triée par `nomComplet` côté serveur) ; comptes et prêts joints par `clientId`, absents → listes vides.
- Calculs en centimes entiers dans `src/domain` (`centimesDepuisChaine`). Formats legacy : `EUR{N,NNN}` (encours, solde total de carte, montant de prêt) = virgule des milliers, décimales seulement si non nulles, au plus 2, zéro final retiré (`1733050` → `EUR17,330.5`, `0` → `EUR0`, négatif → `-EUR…`) ; `EUR{N,NNN.NN}` (solde d'un compte) = `formaterMontantTableau` ; `EUR{NNN.NN}` (mensualité) = `formaterMontantListe`.
- Textes repris du legacy (FEAT-015 `MESSAGES.md` l.917-976), dans `TEXTES.espaceConseiller` de `src/adapters/textes.ts` : « Espace Conseiller », « Bonjour {nomComplet}, vous gerez {N} client(s) » (bandeau legacy : « Espace Conseiller » en gras, ` — `, puis la salutation avec « {N} client(s) » en gras), « Mes clients »/« clients dans mon portefeuille », « Encours total »/« solde cumulé mes clients », « Prets en attente »/« a traiter », « Actif », « Inactif », « Solde total », « Comptes », « Prets actifs », « Prets en attente », « ⚠ {n} pret(s) en attente », « ✓ {n} pret(s) en cours », « Voir detail », « Fermer », « Comptes bancaires », « Numero », « Type », « Solde », « Prets », « Montant », « Mensualite », « Statut », « Aucun compte », « Aucun pret », « Aucun client dans votre portefeuille. », « Contactez l administrateur pour l affectation des clients. ». Libellés d'indicateurs en capitales par `text-transform` seulement.
- Titre du panneau = nom complet seul (legacy FEAT-015, pas de préfixe « Detail : ») ; sous-ligne `@{login} · {email} · {telephone}` (parties nulles omises) ; pas de « Client depuis ».
- Badges `Etiquette` (`tag`) : type de compte (`COURANT`, `EPARGNE`), statuts de prêt `EN_ATTENTE`, `EN_COURS`, `REFUSE`, `REMBOURSE` ; `DEFAUT` et tout autre statut en texte brut (BUG-053 conservé). Codes et types affichés tels quels (`AUTO`, type `null` → cellule vide).
- `data-testid` préfixe `conseiller-` : `conseiller-contenu` (racine, `aria-busy` jusqu'aux données), `conseiller-message-erreur`, `conseiller-voir-detail-{clientId}`, `conseiller-fermer-detail`, `conseiller-detail`, `conseiller-carte-{clientId}`, badges `conseiller-type-compte-{compteId}` / `conseiller-statut-pret-{pretId}`.
- Erreur : chaque appel non-401 en échec donne un message — `message` du corps JSON s'il est texte, sinon `TEXTES.erreurTechnique` ; messages distincts affichés dans `AlerteErreur testId="conseiller-message-erreur"`, sans pile ni code ; indicateurs et cartes affichés seulement si les trois réponses sont ok ; le bandeau « Espace Conseiller » reste affiché.
- Hexagone et couverture : `src/domain`/`src/application` purs, 100 % ; adaptateurs ≥ 80 %. Tests Vitest titrés `[FR-015-0x]` (et `[BUG-053]`).

**Never:**
- Aucun appel à `/conseiller/portefeuille` pour un CLIENT (redirection par `RouteProtegee`, déjà en place) ; aucun paramètre `conseillerId`/`clientId` envoyé.
- Pas de modale, pas de changement de route pour le détail ; pas de `sessionStorage`/`localStorage` pour le cache ; pas de texte inventé ; pas de modification back-end, de `scripts/gates/**`, `docs/quality-gates.md`, `sprint-status.yaml`, `frontend/.dependency-cruiser.cjs`, du golden master (jamais rejoué).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Portefeuille de sophie | CONSEILLER « Sophie Durand » ; identité [Jean Dupont (1, actif), Marie Lebrun (2)] ; comptes client 1 `"2130.00"` + `"15200.00"` | « Bonjour Sophie Durand, vous gerez 2 client(s) » ; Mes clients 2 ; Encours `EUR17,330` ; 2 cartes « @user1 », « @user2 » | — |
| Priorité des indicateurs de prêt | client avec 1 `EN_ATTENTE` + 2 `EN_COURS` / 0 + 2 / aucun prêt | « ⚠ 1 pret(s) en attente » seul / « ✓ 2 pret(s) en cours » / rien | — |
| Portefeuille vide | identité `[]` | « Aucun client dans votre portefeuille. » puis « Contactez l administrateur pour l affectation des clients. » ; indicateurs à 0 / `EUR0` | — |
| Client inactif | `actif: false` | carte distinguée (classe) et badge « Inactif » | — |
| Détail | clic « Voir detail » de Jean Dupont | panneau inline, focus sur le titre ; « Fermer » ou `Échap` → fermé, focus rendu au bouton | — |
| Client sans compte ni prêt | `comptes: []`, `prets: []` | « Aucun compte », « Aucun pret » | — |
| Retour sur la route | portefeuille chargé, navigation `/produits` → `/espace-conseiller` | aucun nouvel appel | — |
| Reconnexion | déconnexion puis connexion | trois nouveaux appels | — |
| Service en panne | credits 500 `{"message":"Service crédits indisponible"}` | ce message dans `conseiller-message-erreur`, ni indicateurs ni cartes | corps illisible → « Une erreur technique est survenue » |
| 401 | un des trois répond 401 | session terminée, retour `/login` | — |

</intent-contract>

## Code Map

- `frontend/src/adapters/routes/routes.tsx:68-78,127-142` -- `ROUTES_PROTEGEES` et `TITRES_ONGLET` ; `/espace-conseiller` rend aujourd'hui `<title>` seul : brancher l'écran (garder `ROUTES_PROTEGEES` inchangé et la structure `/administration` = fragment `<title>` + sections, lue par `main.test.ts:88-91`). `Dependances` L.44-66 + déstructuration L.83-104 : ajouter `clientPortefeuille`.
- `frontend/src/adapters/routes/routes.test.tsx:28-39` -- attend un contenu `''` pour `/espace-conseiller` en ADMIN : à adapter au nouvel écran (fournir un faux client), sans retirer les autres assertions.
- `frontend/src/adapters/routes/RouteProtegee.tsx:24-26`, `src/domain/navigation.ts:49-52,94-110` -- redirection CLIENT → `/tableau-de-bord` et sous-navigation « Mon portefeuille » · « Conseiller IA » déjà en place (rien à faire, tester l'absence d'appel).
- `frontend/src/adapters/gabarit/GabaritAuthentifie.tsx:95-97` -- monté pendant toute la navigation protégée, démonté à la déconnexion (`/login` hors gabarit) : y fournir la mémoire de session autour d'`<Outlet context={profil} />` (contexte inchangé = profil, lu par `useOutletContext<ResultatProfil | null>()` comme `EcranConseiller.tsx:30`).
- `frontend/src/application/consulterProduits.ts:29-46` -- motif cas d'usage (`sessionOuverteOuFin`, `Promise.all`, 401 → `session.terminer()`).
- `frontend/src/adapters/http/ClientProduitsHttp.ts:12-18,122-140` + `ClientConversationHttp.ts:8-16,42` -- motif adaptateur (chemins exportés, `GET` + `Accept`, gardes de forme → `null`, lecture de `message`) ; test modèle `ClientProduitsHttp.test.ts`.
- `frontend/src/infrastructure/main.tsx:50` et `main.test.ts:62-108,209-221` -- câblage par `fetchAuthentifie` imposé ; ajouter le client à la liste `authentifies` via `ecran('/espace-conseiller').client`.
- `frontend/src/adapters/test/rendreApplication.tsx:48-85,151-182` -- paramètres positionnels `Partial<Client…>` enveloppés dans `vi.fn` : ajouter `clientPortefeuille` en dernier (défaut : `technique`/listes vides), l'exposer, mettre la JSDoc à jour.
- `frontend/src/domain/montant.ts:7` (`centimesDepuisChaine`), `tableauDeBord.ts:21` (`formaterMontantTableau`), `pret.ts:140` (`formaterMontantListe`) -- réutiliser ; nouveau formateur `EUR{N,NNN}` à créer.
- `frontend/src/adapters/ui/{Carte (variante 'muted', attenuee), Etiquette, Tableau, BoutonSecondaire, BoutonTexte, AlerteErreur}.tsx` -- composants ; `prets/SectionEcheancier.tsx:22-37` -- motif panneau inline (`<h3 tabIndex={-1}>` focalisé) ; `administration/SectionRevueVirements.tsx:88-96` -- motif retour du focus au déclencheur.
- `frontend/src/adapters/textes.ts:159` (`titresOnglet.espaceConseiller`), `:9` (`erreurTechnique`) -- ajouter le bloc `espaceConseiller` (JSDoc citant FEAT-015 `MESSAGES.md`).
- Contraintes transverses : `src/motifs-interdits.test.ts` (pas de `<dialog`, `:hover` sur display/opacity), `couleurs-hors-tokens.test.ts` (seulement `var(--color-…)`), `confinement-jeton.test.ts` (aucun mot « jeton » dans domain/application, commentaires compris), `architecture-temps.test.ts`, `adapters/accessibilite.test.tsx:160-165` (axe sur `/espace-conseiller`).

## Tasks & Acceptance

**Execution:**
- `frontend/src/domain/portefeuille.ts` (+ test) -- types (`ClientDuPortefeuille`, `ComptesDuClient`, `PretsDuClient`), jointure `assemblerPortefeuille(identites, comptes, prets)` → cartes (solde total, nb comptes, nb `EN_COURS`, nb `EN_ATTENTE`, indicateur de prêt `attente|cours|aucun`), indicateurs (clients, encours, prêts en attente), `formaterEurosArrondi(centimes)` (`EUR{N,NNN}`), `initiale(nomComplet)`, `statutAvecBadge(statut)` -- règles pures 100 % couvertes.
- `frontend/src/application/consulterPortefeuille.ts` (+ test) -- port `ClientPortefeuille { lireIdentites(); lireComptes(); lirePrets() }` (réponses `ok|nonAuthentifie|erreur{message}`), cas d'usage parallèle → `{type:'etat', portefeuille | null, messages}` ou `deconnecte`.
- `frontend/src/adapters/http/ClientPortefeuilleHttp.ts` (+ test) -- trois chemins exportés, gardes de forme, 401, `message` texte ou `TEXTES.erreurTechnique`, réseau coupé.
- `frontend/src/adapters/gabarit/memoireSession.tsx` (+ test) et `GabaritAuthentifie.tsx` -- contexte React fournissant une mémoire clé → promesse (obtenir, oublier), vivant le temps du gabarit.
- `frontend/src/adapters/ecrans/espaceConseiller/EcranEspaceConseiller.tsx` + `.css` + `.test.tsx` -- écran complet ; CSS tokens seuls, libellés KPI `text-transform: uppercase`.
- `frontend/src/adapters/textes.ts`, `routes/routes.tsx`, `routes/routes.test.tsx`, `test/rendreApplication.tsx`, `infrastructure/main.tsx`, `infrastructure/main.test.ts`, `adapters/accessibilite.test.tsx` -- textes, branchement, câblage `fetchAuthentifie`, cas axe chargé/vide/détail ouvert.
- Violation volontaire : faire chaîner les trois appels (`await` successifs) → le test « en parallèle » doit échouer ; vérifier puis annuler.

**Acceptance Criteria:**
- Given `sophie` (CONSEILLER, « Sophie Durand »), when elle ouvre `/espace-conseiller`, then les trois appels partent avant qu'aucun ne soit résolu, et l'écran affiche « Espace Conseiller », « Bonjour Sophie Durand, vous gerez 2 client(s) », le sous-en-tête « Mon portefeuille » · « Conseiller IA » (lien `/mon-conseiller`) et les trois indicateurs en `card-muted` avec leurs sous-libellés.
- Given un CLIENT, when il ouvre `/espace-conseiller`, then il arrive sur `/tableau-de-bord` sans message et aucune méthode de `ClientPortefeuille` n'est appelée.
- Given `cd frontend && npx vitest run -t 'FR-015-0'`, then code 0 avec des tests de FR-015-01 à FR-015-09.
- Given l'écran livré, when `scripts/golden-master/verifier-scenario.sh SC-015-01` puis `SC-015-02`, then code 0.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- Couches lancées : alignement sur l'intention (seule couche configurée) ; couches cas limites et trous de vérification non configurées, sautées.
- verdicts: 5 findings — high 0, medium 0, low 0, false 5, maybe-false 0
- findings:
  - `[false]` `[reject]` Le contrat front/back (noms de champs) n'est exercé par aucun test contre les vrais services — les gardes de `ClientPortefeuilleHttp.ts` lisent exactement les champs des records Java de la story 9.1 (`clientId, nomComplet, login, email, telephone, actif` ; `compteId, numero, type, solde` ; `pretId, type, montant, mensualite, statut`), comme l'auditeur le constate lui-même ; le motif (adaptateur testé sur un faux `fetch`) est celui de tous les écrans, et le rejeu sur la pile réelle est réservé à l'epic 10. Aucun mauvais résultat.
  - `[false]` `[reject]` Les contrôles shell (vitest `FR-015-0`, `verifier-scenario.sh`, gates) ne laissent pas de trace dans le diff — ils ont été lancés hors diff (codes de sortie consignés dans « Auto Run Result ») ; un diff de code n'a pas à les contenir.
  - `[false]` `[reject]` « MES CLIENTS », « ENCOURS TOTAL », « PRETS EN ATTENTE » introuvables par la recherche statique — capitales rendues par `text-transform`, exigé par la story (« le texte source gardant sa casse ») et par AR-79 ; le rejeu Playwright lit `innerText`, qui applique `text-transform` ; `verifier-scenario.sh SC-015-01` sort en 0 (textes non trouvés seulement consignés, par conception du script).
  - `[false]` `[reject]` Titre du panneau sans « Detail : » malgré UX-DR58 — le legacy FEAT-015 n'affiche que le nom complet (source `conseiller-workspace.xhtml`), la story n'impose aucun libellé de titre, et « Detail : » n'existe pour cet écran ni dans `MESSAGES.md` ni dans `textes-nouveaux.md` : l'ajouter serait un texte inventé. Décision consignée dans Design Notes.
  - `[false]` `[reject]` Violation volontaire sans preuve visible — rejouée pendant la revue : `Promise.all` remplacé par trois `await` successifs dans `consulterPortefeuille.ts` → `consulterPortefeuille.test.ts` « lance les trois appels avant qu'aucun ne soit résolu » et `EcranEspaceConseiller.test.tsx` « les trois appels partent avant qu'aucun ne soit résolu » en échec ; fichier restauré à l'identique (`cmp`).

## Design Notes

- Mémoire de session : le gabarit authentifié est une route de mise en page sans chemin, parente de toutes les routes protégées ; il reste monté entre elles et se démonte à la déconnexion ou à l'expiration (retour à `/login`). Y loger une `Map` de promesses (via `useRef`) donne exactement « une fois par session » sans stockage navigateur. Mémoriser la **promesse** (pas le résultat) évite le double appel du mode strict ; l'écran l'oublie si le résultat n'est pas entièrement ok.
- Titre du panneau : l'UX-DR58 cite « Detail : {nom} », emprunté à l'administration (FEAT-016) ; le legacy FEAT-015 n'affiche que le nom complet et aucun texte nouveau n'est prévu : on garde le legacy.
- `EUR{N,NNN}` reproduit `f:convertNumber type="currency" minFractionDigits="0"` du legacy (au plus deux décimales, zéros finaux retirés) ; les données de démo sont rondes.

## Verification

**Commands:**
- `cd frontend && npx vitest run --coverage` -- code 0, seuils tenus
- `cd frontend && npx vitest run -t 'FR-015-0'` -- code 0
- `scripts/golden-master/verifier-scenario.sh SC-015-01` et `SC-015-02` -- code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` -- code 0

## Auto Run Result

Status: done

**Résumé :** l'écran `/espace-conseiller` (FEAT-015) lit en parallèle les trois points `…/conseiller/portefeuille` de la story 9.1, une fois par session (promesse gardée par une mémoire portée par le gabarit authentifié, oubliée si le résultat n'est pas entièrement ok), puis affiche le bandeau « Espace Conseiller — Bonjour {nom}, vous gerez N client(s) », trois indicateurs en `card-muted` (libellés en capitales par CSS), une carte par client et un panneau de détail inline (focus au titre, « Fermer » ou Échap rend le focus au bouton). Un CLIENT est redirigé par la garde existante sans aucun appel ; une panne affiche le `message` reçu dans `conseiller-message-erreur`.

**Fichiers (frontend/src) :**
- `domain/portefeuille.ts` — jointure par `clientId`, indicateurs en centimes, format `EUR{N,NNN}`, indicateur de prêts, statuts en badge (BUG-053 conservé), coordonnées.
- `application/consulterPortefeuille.ts` — port `ClientPortefeuille` et cas d'usage parallèle (401 → session terminée, messages d'erreur distincts).
- `adapters/http/ClientPortefeuilleHttp.ts` — trois `GET`, gardes de forme, `message` ou erreur technique.
- `adapters/gabarit/memoireSession.tsx`, `GabaritAuthentifie.tsx` — mémoire de session (clé → promesse) vivant le temps du gabarit.
- `adapters/ecrans/espaceConseiller/EcranEspaceConseiller.{tsx,css}` — l'écran.
- `adapters/textes.ts` (bloc `espaceConseiller`, textes legacy), `routes/routes.tsx` (branchement), `infrastructure/main.tsx` (câblage `fetchAuthentifie`).
- Tests : domaine, cas d'usage, adaptateur, mémoire, écran (`[FR-015-01]` à `[FR-015-09]`, `[BUG-053]`) ; `routes.test.tsx`, `main.test.ts`, `rendreApplication.tsx`, `accessibilite.test.tsx` complétés.

**Revue :** 5 constats (couche alignement sur l'intention), 0 correctif, 0 report, 5 rejetés comme faux : contrat front/back conforme aux records Java de 9.1 ; contrôles shell lancés hors diff ; capitales par CSS exigées par la story ; titre du panneau sans « Detail : » (legacy, pas de texte inventé) ; violation volontaire rejouée pendant la revue.

**Revue complémentaire recommandée :** non (correctifs : high 0, medium 0, low 0).

**Vérification :**
- `scripts/gates/build.sh` : code 0 (build propre de tous les services ; front 98 fichiers, 1 860 tests, couverture tenue).
- `cd frontend && npx vitest run -t 'FR-015-0'` : code 0 (84 tests).
- `scripts/golden-master/verifier-scenario.sh SC-015-01` et `SC-015-02` : code 0 (SC-015-01 : « ENCOURS TOTAL », « PRETS EN ATTENTE », « @user1 », « @user2 » consignés comme non trouvés statiquement — capitales CSS et `@` composé à l'exécution).
- `scripts/gates/check-gates-exist.sh` (aucun KO), `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` : code 0. Golden master non rejoué.
- Violation volontaire (trois `await` successifs au lieu de `Promise.all`) : deux tests `[FR-015-02]` rouges, puis restauration à l'identique.

**Risques résiduels :**
- Un CONSEILLER qui tente `/administration` est redirigé par une garde qui démonte le gabarit : la mémoire est perdue et le portefeuille rechargé au retour (cas marginal, un appel de plus).
- Profil indisponible : le bandeau n'affiche que « Espace Conseiller », sans salutation.
- Dans une application lancée sans chargement des projections de référence, le cœur bancaire et credits rendent des portefeuilles vides (report de la story 9.1, ADV-08).

