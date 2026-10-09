---
title: 'Activer et désactiver un client depuis la gestion des clients'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: '58459ad234fe8174949286bede2888e68a4e4db4'
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

**Problem:** la section « Gestion des clients » de `/administration` (story 9.3) n'a que le bouton « Detail » : l'ADMIN ne peut pas bloquer ni rétablir l'accès d'un client depuis l'écran, alors que l'API ADMIN d'`identite-clients` (story 2.6, audits `ADMIN_DEACTIVATE`/`ADMIN_ACTIVATE`, publication `client-modifie`) existe déjà.

**Approach:** front seulement. Le port `ClientAdministration` gagne une action `changerEtatClient(clientId, actif)` (adaptateur : `POST /api/identite-clients/clients/{id}/desactivation` ou `/activation`, 204), un cas d'usage la traduit (succès, message d'erreur, fin de session), et chaque ligne de « Gestion des clients » affiche « Desactiver » ou « Activer » (`BoutonFantome`) après « Detail » ; le badge ne change et le succès ne s'affiche qu'après la réponse 2xx.

## Boundaries & Constraints

**Always:**
- Aucune modification de l'API ni de ses tests (`PointEntreeClients`, `ChangerEtatClientCasUsage`, `ActivationClientIT`) : l'audit, l'adresse IP, la publication `client-modifie` et le refus de connexion d'un client désactivé restent ceux de la story 2.6 ; le front n'émet aucun audit et n'envoie ni corps ni paramètre.
- Appel par `fetchAuthentifie` seulement (le client HTTP d'administration est déjà câblé dans `main.tsx`) ; méthode `POST`, en-tête `Accept: application/json`.
- Réponse : 2xx → succès ; 401 → `session.terminer()` puis redirection vers la connexion (motif de `EcranAdministration`) ; autre statut → `message` texte du corps `{"message": …}`, sinon `TEXTES.erreurTechnique` ; réseau coupé → `TEXTES.erreurTechnique`.
- Ligne d'un client actif : « Desactiver », `data-testid="admin-desactiver-{clientId}"` ; inactif : « Activer », `admin-activer-{clientId}` ; un seul bouton à la même position (le focus reste sur l'élément après bascule). Composant `BoutonFantome` (`button-ghost`) enrichi d'une prop optionnelle `occupe` → `aria-disabled="true"` et clic ignoré, sans changer l'usage existant (`EcranConseiller`).
- Pendant la requête : le bouton du client concerné est `aria-disabled="true"` et un second clic n'envoie rien ; il redevient actif à la réponse.
- Succès : badge « Actif »/« Inactif » de la ligne et du panneau de détail ouvert basculés localement (`actif` inversé dans l'état de la section), puis « Client désactivé » / « Client activé » (MSG-016-130/131) dans `AlerteSucces testId="admin-message-succes"` de la section « Gestion des clients ». Échec : `AlerteErreur testId="admin-message-erreur"` de la même section avec le message ; badge et bouton inchangés ; aucun message de succès. Chaque nouvelle action remplace le message précédent.
- Textes dans `TEXTES.administration.gestionClients` (`desactiver: 'Desactiver'`, `activer: 'Activer'`, `succesDesactive: 'Client désactivé'`, `succesActive: 'Client activé'`), JSDoc citant `MESSAGES.md` l.1023 et l.1090-1091.
- Hexagone (rien de React ni de `fetch` dans `src/application`), couverture 100 % domain/application et ≥ 80 % adaptateurs ; tests Vitest titrés `[FR-016-11]` (et `[FR-001-07]` pour le test adaptateur qui prouve que seule l'API est appelée).

**Never:**
- Pas de modale de confirmation, pas de `<dialog>`, pas de rechargement de la page ni des indicateurs (non demandé), pas de texte inventé.
- Pas d'alertes de fraude (9.5), de journal ni de catalogue (9.6).
- Ne pas toucher `scripts/gates/**`, `docs/quality-gates.md`, `sprint-status.yaml`, `frontend/.dependency-cruiser.cjs`, le golden master (jamais rejoué), le back ; ne retirer aucune assertion des tests existants.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Désactiver | `user3` (id 3) actif, clic « Desactiver » | `POST …/clients/3/desactivation` ; à la réponse 204 : « Client désactivé », badge « Inactif », bouton « Activer » (`admin-activer-3`) | — |
| Activer | `user3` inactif, clic « Activer » | `POST …/clients/3/activation` ; « Client activé », badge « Actif », bouton « Desactiver » | — |
| Requête en cours | réponse non encore arrivée | bouton `aria-disabled="true"`, badge « Actif », aucun message de succès ; 2e clic sans nouvel appel | — |
| Erreur métier | 403 `{"message":"Accès refusé"}` ou 500 `{"message":"Une erreur technique est survenue"}` | ce message dans `admin-message-erreur` ; badge « Actif » ; pas de « Client désactivé » | corps illisible / sans `message` texte / réseau coupé → `TEXTES.erreurTechnique` |
| Session expirée | 401 | session terminée, retour à `/connexion` | — |
| Panneau ouvert | détail de `user3` ouvert, désactivation réussie | statut du panneau « Inactif » | — |

</intent-contract>

## Code Map

**API réutilisée (lecture seule)** — `services/identite-clients/src/main/java/fr/nextbanking/identiteclients/adapter/in/http/PointEntreeClients.java` : `POST /api/identite-clients/clients/{clientId}/desactivation` et `/activation`, 204 sans corps ; erreurs `{"message"}` par `GestionErreurs` (403 « Accès refusé », 404 « Ressource introuvable », 500). Couverture existante : `src/test/.../adapter/ActivationClientIT.java` (tags `FR-016-11`, `FR-026-02`, `FR-001-07` : audit avec acteur, client, IP ; connexion refusée « Identifiants incorrects » après désactivation). `FR-016-06` n'a aucun test dans identite-clients (seulement dans credits) : la commande `-Dgroups='FR-016-11 | FR-016-06'` passe par les tests `FR-016-11`.

**frontend** (`frontend/src/`)
- `application/consulterAdministration.ts` -- port `ClientAdministration` (4 lectures, `ReponseAdministration<T>` = `ok|nonAuthentifie|erreur{message}`) : ajouter `changerEtatClient(clientId: number, actif: boolean): Promise<ReponseAdministration<null>>` et le cas d'usage `changerEtatClient(client, session, clientId, actif)` → `{type:'effectue'} | {type:'erreur', message} | {type:'deconnecte'}` (`sessionOuverteOuFin` d'abord ; 401 → `session.terminer()`).
- `adapters/http/ClientAdministrationHttp.ts` -- ajouter `cheminEtatClient(clientId, actif)` exporté (`/api/identite-clients/clients/${id}/desactivation|activation`) et la méthode ; réutiliser `corpsDe`, `enregistrement`, `texte`, `TECHNIQUE`, `NON_AUTHENTIFIE` ; succès sur `reponse.ok` sans lire le corps.
- `adapters/ecrans/administration/SectionGestionClients.tsx` -- reçoit aujourd'hui `partie` seule ; ajouter les props `client: ClientAdministration`, `session: SessionLocale`, `surDeconnexion: () => void` ; état local `clients` initialisé depuis `partie.donnees` (resynchronisé si `partie` change), ensemble des `clientId` en cours, message `{type, texte}` ; bouton d'état après « Detail » dans la cellule « Actions » ; alerte au-dessus du formulaire de filtre, sous l'éventuelle erreur de chargement.
- `adapters/ecrans/administration/EcranAdministration.tsx` -- passe `client`, `session` et `surDeconnexion` (= `naviguer(ROUTE_CONNEXION, { replace: true })`) à `SectionGestionClients`.
- `adapters/ui/BoutonFantome.tsx` (+ `.css` : `[aria-disabled='true']` curseur `not-allowed`, opacité via token existant) -- prop `occupe?: boolean`.
- `adapters/textes.ts` (bloc `administration.gestionClients`, ~l.150-170) -- quatre textes.
- `adapters/test/rendreApplication.tsx:140-150` -- ajouter `changerEtatClient` au faux par défaut (`() => Promise.resolve({ statut: 'ok', donnees: null })`).
- `application/consulterAdministration.test.ts`, `adapters/http/ClientAdministrationHttp.test.ts`, `adapters/ecrans/administration/EcranAdministration.test.tsx` (`REFERENCE`, `ouvrir()`, `lignesClients()`, client `user3` = id 3 « Pierre Bernard » actif) -- étendre ; le faux de `EcranAdministration.test.tsx` et `accessibilite.test.tsx:110-120` doivent rester valides en TypeScript (ajouter la méthode si l'objet est typé `ClientAdministration` complet).
- Motif de référence pour une action d'administration : `SectionsPretsAdministration.tsx:52-90` (`AlerteSucces`/`AlerteErreur`, `admin-message-succes`).

## Tasks & Acceptance

**Execution:**
- `frontend/src/application/consulterAdministration.ts` (+ test) -- méthode du port et cas d'usage `changerEtatClient` -- les trois issues, session fermée avant appel, 401, message transmis tel quel.
- `frontend/src/adapters/http/ClientAdministrationHttp.ts` (+ test) -- chemins exacts, `POST` sans corps, 204 → ok, 401, 403/500 avec message, corps illisible, réseau coupé -- un test `[FR-016-11] [FR-001-07]` vérifie qu'un seul appel part, vers l'API d'identite-clients, sans corps.
- `frontend/src/adapters/ui/BoutonFantome.tsx` + `.css` -- prop `occupe`.
- `frontend/src/adapters/textes.ts` -- textes legacy.
- `frontend/src/adapters/ecrans/administration/SectionGestionClients.tsx`, `EcranAdministration.tsx` -- boutons, état local, messages, déconnexion.
- `frontend/src/adapters/test/rendreApplication.tsx`, `frontend/src/adapters/accessibilite.test.tsx` -- faux par défaut ; cas axe après une désactivation réussie (message affiché).
- `frontend/src/adapters/ecrans/administration/EcranAdministration.test.tsx` -- tests `[FR-016-11]` : désactiver `user3` (appel, `aria-disabled` pendant une promesse différée, badge inchangé avant résolution, puis message et badge « Inactif », bouton `admin-activer-3`) ; activer un inactif ; « API en erreur → badge inchangé, pas de message de succès » (403 et 500 avec message) ; 401 → connexion ; panneau ouvert mis à jour ; les tests existants (ordre des sections, fenêtre 5 000 caractères) restent verts.
- Violation volontaire : basculer le badge et afficher « Client désactivé » avant l'`await` de l'API → `npx vitest run -t 'FR-016-11'` non nul ; vérifier puis annuler.

**Acceptance Criteria:**
- Given l'ADMIN sur `/administration` et `user3` actif, when il clique « Desactiver », then une seule requête `POST /api/identite-clients/clients/3/desactivation` part, le bouton est `aria-disabled` jusqu'à la réponse, puis « Client désactivé » s'affiche en `role="status"` (`admin-message-succes`), la ligne montre « Inactif » et « Activer ».
- Given `user3` inactif, when l'ADMIN clique « Activer », then « Client activé », badge « Actif », bouton « Desactiver ».
- Given l'API qui répond 403 ou 500 avec `{"message": …}`, when l'ADMIN clique « Desactiver », then ce message s'affiche dans `admin-message-erreur` de « Gestion des clients », le badge reste « Actif » et « Client désactivé » est absent.
- Given `cd frontend && npx vitest run -t 'FR-016-11'`, then code 0 ; with the violation volontaire, then code non nul.
- Given `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-11 | FR-016-06'`, then code 0 (audit `ADMIN_DEACTIVATE`/`ADMIN_ACTIVATE` avec acteur, client et IP ; connexion refusée « Identifiants incorrects » après désactivation — tests de la story 2.6 non modifiés).
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 7 findings — high 0, medium 0, low 1, false 6, maybe-false 0
- findings:
  - `[false]` `[reject]` Les tests s'arrêtent aux faux (port simulé, `fetch` simulé) : rien ne prouve que l'action fonctionne de bout en bout — chemins et verbe identiques aux `@PostMapping` de `PointEntreeClients.java` l.33 et l.42 (204) ; proxy `identite-clients` (8081) déjà utilisé par les lectures de la même classe ; audit, IP et refus de connexion prouvés par `ActivationClientIT`, non modifié et vert dans `build.sh`.
  - `[false]` `[reject]` 401 renvoie vers `/login` (`ROUTE_CONNEXION`) et non vers le `/connexion` littéral du tableau — la règle Always impose le motif d'`EcranAdministration`, qui utilise `ROUTE_CONNEXION` ; c'est la vraie route de connexion. Corriger le tableau reviendrait à éditer la spec.
  - `[low]` `[reject]` Deux clients basculés en même temps : le message affiché est celui de la dernière réponse arrivée, pas de la dernière action lancée — les badges restent justes client par client ; cas rare (deux clics rapides sur des lignes différentes avec réponses inversées) ; la correction demande un compteur de séquence, plus qu'une correction directe.
  - `[false]` `[reject]` Un message de la section des prêts reste affiché à côté de celui de « Gestion des clients » — l'intention borne le message à la section « Gestion des clients » ; comportement de la section des prêts antérieur à la story.
  - `[false]` `[reject]` Certains tests cherchent `admin-message-succes` dans toute la page — `findByTestId` échoue bruyamment s'il trouve deux éléments : aucun faux vert possible.
  - `[false]` `[reject]` La resynchronisation de l'état local quand `partie` change n'a pas de test dédié — `partie` n'est pas rechargée pendant ces actions ; les seuils de couverture sont tenus (`build.sh` code 0).
  - `[false]` `[reject]` `BoutonFantome` n'a pas de fichier de test propre — la prop `occupe` est couverte par les tests de la section, l'usage par défaut par `EcranConseiller` ; seuils de couverture tenus (`build.sh` code 0).

## Design Notes

- Bascule locale plutôt que relecture : l'API renvoie 204 sans corps ; relire les quatre points ferait clignoter la page et pourrait masquer le message. Le serveur reste juge (rôle, existence) ; le badge ne change qu'après sa réponse 2xx. Les indicateurs (« n actifs ») ne sont pas recalculés avant la prochaine ouverture de la page, comme les indicateurs de prêts en 9.3.
- `aria-disabled` plutôt que `disabled` : le bouton reste focalisable pendant la requête (exigence UX), le clic est ignoré par le composant.
- Audit et refus de connexion : produits par l'API existante et prouvés par `ActivationClientIT` ; l'écran n'a rien à auditer. Aucune action humaine hors dépôt n'est requise.

## Verification

**Commands:**
- `cd frontend && npx vitest run -t 'FR-016-11'` -- code 0
- `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-11 | FR-016-06'` -- code 0
- `cd frontend && npx vitest run --coverage` -- code 0, seuils tenus
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` -- code 0

## Auto Run Result

Status: in-review

**Implémentation faite, revue non faite** : la session s'est arrêtée (limite d'usage atteinte) après l'implémentation et la vérification. Une relance de `bmad-build-auto` sur cette story reprend à l'étape de revue (step-04).

**Vérification (rapport du sous-agent d'implémentation, `FR-016-11` relancé ensuite) :**
- `cd frontend && npx vitest run -t 'FR-016-11'` : 18 tests verts, code 0.
- Violation volontaire (badge et « Client désactivé » avant la réponse de l'API) : code 1, 4 tests rouges, puis annulée.
- `npx vitest run --coverage` : 1 945 tests, seuils tenus.
- `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-11 | FR-016-06'` : code 0.
- `scripts/gates/build.sh`, `check-gates-exist.sh` (aucun KO), `trace-smoke.sh`, `golden-master.sh` : code 0. Golden master non rejoué.

**Points à surveiller :** `data-testid` `admin-panneau-statut` présent deux fois dans le panneau de détail (défaut antérieur à la story) ; indicateurs « n actifs » non recalculés avant le prochain chargement (choix de la spec).

### Revue (reprise du 2026-10-09)

**Résumé :** la section « Gestion des clients » de `/administration` affiche « Desactiver » / « Activer » sur chaque ligne ; l'action appelle l'API ADMIN existante d'`identite-clients` (`POST …/clients/{id}/desactivation|activation`) et le statut ne bascule, avec « Client désactivé » / « Client activé », qu'après la réponse 2xx. Front seulement.

**Fichiers modifiés (commit `9091676122906e1c6e23a5da575032ab29ba177b`) :**
- `frontend/src/application/consulterAdministration.ts` (+ test) — méthode `changerEtatClient` du port et cas d'usage (effectué, erreur, déconnecté).
- `frontend/src/adapters/http/ClientAdministrationHttp.ts` (+ test) — chemins d'activation et de désactivation, `POST` sans corps, traduction des statuts.
- `frontend/src/adapters/ecrans/administration/SectionGestionClients.tsx` — bouton d'état, verrou par client, bascule locale, messages.
- `frontend/src/adapters/ecrans/administration/EcranAdministration.tsx` (+ `.css`, + test) — câblage client, session, déconnexion ; tests `[FR-016-11]`.
- `frontend/src/adapters/ui/BoutonFantome.tsx` (+ `.css`) — prop optionnelle `occupe` (`aria-disabled`, clic ignoré).
- `frontend/src/adapters/textes.ts` — quatre textes legacy (MSG-016-130/131).
- `frontend/src/adapters/test/rendreApplication.tsx`, `frontend/src/adapters/accessibilite.test.tsx` — faux par défaut, cas axe après désactivation.

**Revue :** une seule couche active (alignement sur l'intention). 7 constats : 0 correctif, 0 report, 7 rejetés (6 faux, 1 mineur) — raisons dans le Review Triage Log. Revue complémentaire recommandée : non (aucun correctif appliqué : 0 high, 0 medium).

**Vérification refaite pendant la revue :**
- `cd frontend && npx vitest run -t 'FR-016-11'` : 18 verts, code 0.
- `npx vitest run --coverage` : 1 945 tests verts, seuils tenus.
- `scripts/gates/build.sh` : code 0 (back et front, couverture tenue).
- `scripts/gates/check-gates-exist.sh` : code 0, aucun KO.
- `scripts/gates/trace-smoke.sh` : code 0.
- `scripts/gates/golden-master.sh` : code 0 (statique, aucun rejeu).

**Risques résiduels :** message de la dernière réponse arrivée si deux clients sont basculés en même temps ; indicateurs « n actifs » non recalculés avant le prochain chargement (choix de la spec) ; `data-testid` `admin-panneau-statut` en double dans le panneau de détail (antérieur à la story).

Status: done
