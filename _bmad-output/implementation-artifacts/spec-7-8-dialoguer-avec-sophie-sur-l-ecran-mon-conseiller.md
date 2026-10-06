---
title: 'Dialoguer avec Sophie sur l''écran Mon Conseiller'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: '3c034ef42fb05afe7ac6c1b525182d75ccc034f9'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-7-context.md'
  - '{project-root}/docs/retro/features/FEAT-010/MESSAGES.md'
warnings: ['oversized']
deferred:
  - summary: >-
      trace-smoke.sh non exécuté pour la story 7.8.
    evidence: |-
      Le script exige la pile compose ; non lancé dans cette session. Story purement front : aucune trace de service modifiée.
    severity: low (unverified)
---

<intent-contract>

## Intent

**Problem:** `/mon-conseiller` est un lien d'en-tête sans écran : l'échange avec Sophie, exposé par le service open-banking-conseiller (story 7.7), n'est utilisable par aucun utilisateur (FEAT-010, FR-010-01 à 11).

**Approach:** Ajouter au front hexagonal la route protégée `/mon-conseiller` et son écran de conversation : règles pures (domain : prénom, saisie vide), port `ClientConversation` et cas d'usage `dialoguer` (application), adaptateur HTTP et écran React (adapters). Conversation tenue par le front (aucun état serveur), même écran pour CLIENT, CONSEILLER et ADMIN.

## Boundaries & Constraints

**Always:**
- Hexagone front : `src/domain` et `src/application` en TypeScript pur (aucun import npm, ni `Date.now()`/`new Date()`), couverture 100 % ; tout le React dans `src/adapters`. Textes uniquement dans `TEXTES` (`adapters/textes.ts`) : `titresOnglet.monConseiller` « BankApp Demo - Mon Conseiller » (FEAT-010 MESSAGES) et un bloc `conseiller`, repris à l'identique du legacy (casse, absence d'accents) ; rien d'inventé.
- Appel via `fetchAuthentifie` : `POST /api/open-banking-conseiller/chat/messages`, corps `{message, historique:[{role:"utilisateur"|"assistant", contenu}]}`, réponse 200 `{reponse}` ; `Authorization: Bearer`. Tout 401 → `/login` (jeton supprimé).
- Historique envoyé = échanges déjà affichés, **sans** le message de bienvenue ni le message en cours (vide au premier message). Le message de bienvenue n'est jamais dans la liste des échanges.
- Bienvenue : « Bonjour {prénom} ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ? » ; prénom = premier mot du `nomComplet` du profil déjà chargé par `GabaritAuthentifie` (transmis par `<Outlet context>`, aucun second appel `/moi`) ; profil indisponible : « cher client ». Rien n'est affiché avant que le profil soit résolu.
- En-tête de carte : « Sophie — Conseillère Demo Bank », « En ligne · Répond en quelques secondes », bouton « Nouvelle conversation ». Bandeau IA supprimé.
- `data-testid` : `chat-saisie`, `chat-envoyer`, `chat-message-utilisateur`, `chat-message-sophie`, `chat-nouvelle-conversation`, `chat-action-{optimiser-epargne|projet-immobilier|investir-bourse|preparer-retraite}`, `chat-messages`, `chat-contenu`. Couleurs uniquement par les variables de `tokens.css`.
- Bulles : Sophie en `card-muted` (`surface-alt`), client en `card` ; liste `aria-live="polite"` ; saisie avec nom accessible (le texte du placeholder en `aria-label`). Zone de contenu `aria-busy="true"` jusqu'à la résolution du profil (déjà géré par le gabarit).
- Une panne est une bulle de Sophie, sans zone d'erreur distincte : 503/502 `{message}` → ce message tel quel ; autre échec (réseau, 5xx autre, corps illisible) → « Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. » ; jamais de « 401 » affiché (BUG-038).

**Never:**
- Pas de nouveau paquet npm, pas de `vitest.config.*`, pas de texte inventé, aucune persistance de la conversation (ni localStorage ni serveur) ; champ de saisie **non vidé** après l'envoi (A-010-2).
- Ne pas modifier `scripts/gates/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `sprint-status.yaml`, ni le golden master ; ne jamais le rejouer ; pas de modification des services (7.7 livrée).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Ouverture CLIENT | profil « Jean Dupont » | « Sophie — Conseillère Demo Bank », « Bonjour Jean ! … », 4 actions rapides | — |
| Sans client | profil indisponible | « Bonjour cher client ! … » | — |
| Non authentifié / 401 | pas de jeton ou 401 | `/login`, jeton supprimé | — |
| Envoi | « Bonjour » | bulle client, appel avec historique `[]`, bulle de Sophie ; saisie conservée ; 2e envoi : historique = les deux premiers échanges | — |
| Saisie vide | vide ou blancs ; clic ou Entrée | rien : ni message, ni appel, ni erreur | — |
| Attente | réponse non arrivée | « Envoyer » désactivé affichant « ... », rétabli après ; Entrée envoie, Maj+Entrée insère un saut de ligne ; défilement au dernier message | — |
| Action rapide | clic « Projet immobilier » | envoie « Je souhaite acheter un appartement, comment preparer mon dossier ? », 4 boutons masqués | idem envoi |
| 503 refus 401 | `{"message":"Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence."}` | bulle de Sophie = ce texte, sans « 401 » | — |
| 502 | `{"message":"Je suis momentanement indisponible (HTTP 500). Veuillez reessayer."}` | bulle de Sophie = ce texte | — |
| Réseau coupé / corps illisible | exception, 400, 500 | bulle de Sophie : texte générique | non bloquant |
| Nouvelle conversation | après échanges | échanges effacés, bienvenue recalculée, 4 actions réaffichées ; une réponse en vol est ignorée | — |
| Réouverture | quitter puis revenir | seulement la bienvenue | — |

</intent-contract>

## Code Map

- `frontend/src/adapters/routes/routes.tsx` -- ajouter `/mon-conseiller` (le littéral `'/mon-conseiller'` est cherché par `verifier-scenario.sh`) ; `Dependances.clientConversation`. Modèle : routes `/bourse`, `/produits`.
- `frontend/src/adapters/gabarit/GabaritAuthentifie.tsx` -- passer `profil` en `<Outlet context={profil} />` (aucun autre changement ; `GabaritAuthentifie.test.tsx` à garder vert).
- `frontend/src/infrastructure/main.tsx`, `frontend/src/adapters/test/rendreApplication.tsx` -- injection du client, aide de rendu (8e paramètre `clientConversation`, défaut : panne technique).
- `frontend/src/application/consulterProduits.ts` (+ `consulterBourse.ts` : `jetonOuFin`) -- modèle de port, résultats `deconnecte`, jamais d'exception ; `adapters/http/ClientProduitsHttp.ts` (+ test) -- modèle d'adaptateur (`fetchAuthentifie`, `statut: 'nonAuthentifie' | 'technique'`).
- `frontend/src/application/chargerProfil.ts` -- `ResultatProfil` (`profil` / `indisponible` / `deconnecte`) ; `adapters/ecrans/produits/EcranProduits.{tsx,css}` -- modèle d'écran (`aria-busy`, `<title>`), `adapters/ui/{Carte,BoutonPrimaire,BoutonSecondaire}` (ajouter `BoutonFantome` = `button-ghost` : contour ink, fond transparent, 48 px, sur le modèle de `BoutonSecondaire`).
- Tests d'architecture à garder verts : `src/{motifs-interdits,architecture-temps,couleurs-hors-tokens}.test.ts`, `accessibilite.test.tsx` (axe : ajouter `/mon-conseiller`, conversation neuve et avec échanges).
- Contrat service (lecture seule) : `services/open-banking-conseiller/.../adapter/in/http/{PointEntreeChat,GestionErreurs,Textes}.java`.
- Sources : `docs/retro/features/FEAT-010/{MESSAGES,RG,FIELDS}.md` ; scénario `docs/retro/golden-master/derived/FEAT-010/SC-010-01` (start_url `/mon-conseiller`, textes « Sophie — Conseillère Demo Bank », « Bonjour Jean ! ») ; `epics.md` « Story 7.8 » ; PRD FR-010-01…11.

## Tasks & Acceptance

**Execution:**
- `frontend/src/domain/conversation.ts` (+ test) -- types `Echange`/`Role`, `prenomDe(nomComplet|null)` (premier mot, sinon null), `saisieVide(saisie)` (blancs inclus) -- règles pures, 100 %, tests tagués FR-010-02, FR-010-05
- `frontend/src/application/dialoguerAvecSophie.ts` (+ test) -- port `ClientConversation.envoyer(jeton, message, historique)` (`ok` / `indisponible` avec message / `nonAuthentifie` / `technique`), cas d'usage `dialoguer(client, stockage, historique, message, repli)` → `{type:'reponse', texte}` ou `{type:'deconnecte'}` ; 401 supprime le jeton -- sans React, tags FR-010-05, FR-010-06, FR-010-09, BUG-038
- `frontend/src/adapters/http/ClientConversationHttp.ts` (+ test) -- POST JSON via `fetchAuthentifie`, 200 → `ok` (réponse texte exigée), 401 → `nonAuthentifie`, 502/503 avec `message` texte → `indisponible`, tout autre cas → `technique` -- ≥ 80 %
- `frontend/src/adapters/ui/BoutonFantome.{tsx,css}` -- composant `button-ghost`
- `frontend/src/adapters/ecrans/conseiller/EcranConseiller.{tsx,css}` (+ test) -- écran, bulles, actions rapides, saisie (Entrée / Maj+Entrée, « ... », défilement par `scrollTop`), « Nouvelle conversation » ; `adapters/textes.ts` -- `titresOnglet.monConseiller`, `conseiller`
- `routes.tsx`, `GabaritAuthentifie.tsx`, `main.tsx`, `rendreApplication.tsx`, `accessibilite.test.tsx` -- câblage décrit au Code Map
- tests tagués : un `describe`/`it` portant chacun de FR-010-01, 02, 03, 04, 05, 06, 09, 10, 11 et BUG-038, lançables par `npx vitest run -t '<tag>'`

**Acceptance Criteria:**
- Given `user1` (CLIENT, « Jean Dupont »), when il ouvre `/mon-conseiller`, then l'écran affiche l'en-tête de Sophie, « Bonjour Jean ! … » et quatre `button-ghost` ; CONSEILLER et ADMIN y accèdent aussi ; non authentifié → `/login`.
- Given les cas de la matrice (envoi, vide, attente, action rapide, 503, 502, réseau, nouvelle conversation, réouverture), when je les rejoue en test DOM, then les comportements listés sont observés.
- Given l'écran livré, when je lance `scripts/golden-master/verifier-scenario.sh SC-010-01`, then code 0.
- Given la story terminée, when je lance `npx vitest run -t '<tag>'` pour chaque tag, puis `scripts/gates/build.sh`, `check-gates-exist.sh` et `golden-master.sh`, then code 0 (couverture 100 % domain/application, 80 % adapters).

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 1, false 2, maybe-false 1
- findings:
  - `[false]` `[reject]` Contrat HTTP du service testé seulement par des doublures côté client — le corps posté (`message`, `historique[{role,contenu}]`), les statuts 200/401/502/503 et les `{message}` correspondent à `PointEntreeChat` et `GestionErreurs` (lus) ; `ClientConversationHttp.test.ts` vérifie le corps réellement posté.
  - `[false]` `[reject]` SC-010-01 vérifié statiquement et non rejoué — voulu : `verifier-scenario.sh` est statique et le golden master ne se rejoue pas avant l'epic 10 (AGENTS.md) ; le script sort en code 0.
  - `[low]` `[reject]` « Bonjour Jean ! » listé « texte non trouvé » par le script (message bâti par gabarit) — le script n'échoue pas sur ce point, le texte est couvert par les tests d'écran ; réécrire le gabarit pour le satisfaire compliquerait le code sans bénéfice.
  - `[maybe-false]` `[defer]` `trace-smoke.sh` non exécuté (pile compose requise) — à confirmer par un passage humain ou la CI.

## Auto Run Result

Status: done

- Résumé : route `/mon-conseiller` et écran de conversation avec Sophie : bienvenue personnalisée (profil transmis par `Outlet context`), quatre actions rapides, saisie (Entrée / Maj+Entrée, « ... », saisie conservée), pannes en bulle de Sophie sans « 401 » (BUG-038), « Nouvelle conversation » qui ignore une réponse en vol ; conversation tenue par le front, sans persistance.
- Fichiers (tous sous `frontend/src`) : `domain/conversation.ts`, `application/dialoguerAvecSophie.ts`, `adapters/http/ClientConversationHttp.ts`, `adapters/ecrans/conseiller/EcranConseiller.{tsx,css}`, `adapters/ui/BoutonFantome.{tsx,css}`, `Carte` (variante `card-muted`), `textes.ts`, `routes.tsx`, `GabaritAuthentifie.tsx`, `main.tsx`, `rendreApplication.tsx`, tests (dont deux cas axe).
- Revue : 0 patch, 1 report (trace-smoke), 3 rejets (voir journal).
- followup_review_recommended : false.
- Vérification : vitest complet (623 tests, seuils de couverture respectés), `tsc --noEmit`, tags FR-010-01 à 11 et BUG-038, `verifier-scenario.sh SC-010-01`, `build.sh`, `check-gates-exist.sh` et `golden-master.sh` en code 0 ; `trace-smoke.sh` non exécuté.
- Risques : le message envoyé n'est pas rogné (seule une saisie blanche est ignorée) ; « Nouvelle conversation » conserve la saisie en cours ; « Bonjour Jean ! » est construit par gabarit, donc signalé « texte non trouvé » par le script statique (sans échec) ; `trace-smoke.sh` non exécuté.

## Design Notes

L'état de la conversation (liste d'échanges, `occupe`, compteur de conversation) vit dans le composant : un compteur incrémenté par « Nouvelle conversation » permet d'ignorer une réponse en vol. Actions rapides visibles ⇔ liste d'échanges vide (A-010-3). Le profil arrive par `useOutletContext<ResultatProfil | null>()` ; `null` = en cours, `indisponible` = « cher client ».

## Verification

**Commands:**
- `cd frontend && npx vitest run && npx tsc --noEmit` -- expected: succès, seuils de couverture respectés
- `scripts/golden-master/verifier-scenario.sh SC-010-01` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0 (`trace-smoke.sh` exige la pile compose : à lancer si disponible, sinon à signaler)
