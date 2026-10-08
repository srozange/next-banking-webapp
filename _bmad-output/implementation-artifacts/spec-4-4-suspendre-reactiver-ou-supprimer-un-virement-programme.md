---
title: 'Suspendre, réactiver ou supprimer un virement programmé'
type: 'feature'
created: '2026-10-08'
status: 'done'
baseline_revision: '6897016c860b35c87a6f737cc0c7270b3bd1212c'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-3-creer-un-virement-programme-et-voir-ses-virements-programmes.md'
  - '{project-root}/docs/quality-gates.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** Les boutons « Suspendre », « Reactiver » et « Supprimer » de l'écran `/virements-programmes` sont rendus sans comportement (story 4.3) et le cœur bancaire n'a aucune API pour changer l'état d'un virement programmé ou le supprimer ; le client ne peut pas interrompre un versement récurrent.

**Approach:** Cœur bancaire : trois actions sur un virement programmé du client du jeton (suspension, réactivation, suppression définitive), chacune en mise à jour conditionnelle, avec propriété contrôlée (BUG-013) et refus 404 identique pour un virement d'autrui, inexistant ou plus dans l'état attendu. Front : câbler les trois boutons, recharger la liste sans recharger la page, afficher les trois messages d'erreur dédiés.

## Boundaries & Constraints

**Always:** Client déduit du jeton (`sub`). Hexagone respecté (domain/application sans Spring). API : `POST /api/coeur-bancaire/virements-programmes/{id}/suspension`, `POST …/{id}/reactivation`, `DELETE …/{id}` (204 sans corps). Suspension = `actif` true → false ; réactivation = false → true ; chacune par `update … where id = :id and client_id = :clientId and actif = :etatAttendu` (zéro ligne modifiée = refus) ; suppression = `delete … where id = :id and client_id = :clientId` (ligne absente de la base, quel que soit l'état). Seul `actif` change : `prochaine_execution`, `nb_executions` et le reste sont inchangés. Virement d'un autre client, identifiant inexistant, virement déjà supprimé ou déjà dans l'état demandé : **404 strictement identique** `{"message":"Ressource introuvable"}` (constante `Textes.RESSOURCE_INTROUVABLE`), sans indice d'existence ; le journal (warning) consigne le refus avec identifiants numériques, sans numéro de compte bénéficiaire. 401 sans jeton `Session expirée, veuillez vous reconnecter.` (porté par `fetchAuthentifie`, non dupliqué). Textes à l'identique de `textes-nouveaux.md` : `vp.erreur.appartenance` « Erreur : action impossible. Rechargez la page. » (affiché pour tout 404 des trois actions), `vp.erreur.etat` « Erreur : l'état du virement programmé n'a pas changé. Rien n'a été débité. Veuillez réessayer. » (échec technique de Suspendre/Reactiver), `vp.erreur.suppression` « Erreur suppression : le virement programmé n'a pas été supprimé et reste inchangé. Veuillez réessayer. » (échec technique de Supprimer) ; aucun autre texte inventé. Aucune modale de confirmation ; après succès, liste rechargée (état « Actif »/« Inactif », boutons : Suspendre + Supprimer si actif, Reactiver + Supprimer si inactif). Erreurs en `role="alert"` (`vp-message-erreur`), annoncées ; une action réussie efface l'erreur et le succès de création précédents. Boutons désactivés pendant une action (pas de double envoi). Tests étiquetés `@Tag("FR-004-09")`, `@Tag("FR-004-10")`, `@Tag("BUG-013")` (back) et préfixés `[FR-004-09]`, `[FR-004-10]` (front). Concurrence : test `*TransitionConcurrente*Test` exécuté par `mvn verify`.

**Never:** Pas d'exécution des échéances ni de recalcul de date (story 4.6) ; pas de suspension/restauration de la date de prochaine exécution à la réactivation. Pas d'appel HTTP entre services, pas d'événement Kafka, pas d'audit nouveau. Ne pas modifier scripts/gates/**, golden master, `replay-mapping.json`, ni rejouer le golden master. Aucun seuil abaissé, aucune exclusion de couverture. Pas de modale. Pas de distinction de statut ou de corps entre « d'autrui » et « inexistant ». Pas de `Date.now()`/`new Date()` dans `src/domain` et `src/application`.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Erreur |
|----------|--------------|----------------------|--------|
| Suspendre | virement actif de `user1`, clic `vp-suspendre-{id}` | 204 ; `actif=false`, date et compteur inchangés ; liste rechargée : « Inactif », `vp-reactiver-{id}` + `vp-supprimer-{id}` | — |
| Réactiver | virement inactif de `user1`, clic `vp-reactiver-{id}` | 204 ; `actif=true` ; liste rechargée : « Actif », `vp-suspendre-{id}` + `vp-supprimer-{id}` | — |
| Supprimer | virement (actif ou inactif) de `user1`, clic `vp-supprimer-{id}` | 204 ; ligne absente de la base ; disparaît de la liste (ligne « Aucun virement programme configure » si dernier) | — |
| D'autrui (BUG-013) | client A, virement actif de B, les trois actions par l'API | 404 `{"message":"Ressource introuvable"}` ; virement de B intact (état, prochaine exécution, existence) ; écran A : `vp.erreur.appartenance` | 404 |
| Inexistant | identifiant inconnu ou déjà supprimé, trois actions | même 404 octet pour octet que « d'autrui » ; même texte à l'écran | 404 |
| État déjà atteint | suspendre un inactif, réactiver un actif | 404 identique (transition non appliquée), état inchangé ; écran : `vp.erreur.appartenance` | 404 |
| Erreur technique | échec simulé (5xx, coupure) sur Suspendre/Reactiver puis Supprimer | `vp.erreur.etat` puis `vp.erreur.suppression` dans `vp-message-erreur`, ligne inchangée | 5xx |
| Concurrence | suspensions et suppressions simultanées du même virement actif | une seule suppression gagne, au plus une suspension gagne, les autres 404 ; ligne absente à la fin | 404 |
| Sans jeton | trois routes API | 401 | 401 |

</intent-contract>

## Code Map

Chemins Java sous `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` (`M/`), tests sous `src/test/java/fr/nextbanking/coeurbancaire/` (`T/`), front sous `frontend/src/` (`F/`). Modèle direct : story 4.1 (suppression de bénéficiaire, commit 9fe6710) et story 4.3.

- `M/application/port/out/VirementsProgrammes.java` + `M/adapter/out/persistance/VirementsProgrammesJdbc.java` -- ajouter `changerEtat(id, clientId, actifAttendu, actifVoulu)`-like conditionnel (vrai si une ligne modifiée) et `supprimer(id, clientId)` (vrai si une ligne supprimée) ; JdbcClient, modèle `BeneficiairesJdbc.desactiver` (ligne 58). Aucune migration (colonnes existantes).
- `M/application/port/in/` -- créer `SuspendreVirementProgramme`, `ReactiverVirementProgramme`, `SupprimerVirementProgramme` (ou un port unique `GererEtatVirementProgramme`, au choix) ; cas d'usage dans `M/application/` modèle `SupprimerBeneficiaireCasUsage` : refus par `ErreurVirementProgramme` (nouvelle cause `INTROUVABLE`, texte « Ressource introuvable ») quand le port sortant renvoie faux ; journal `java.util.logging` en warning sans numéro bénéficiaire.
- `M/domain/ErreurVirementProgramme.java` -- ajouter la cause (sans champ). Attention : le handler `virementProgrammeRefuse` de `M/adapter/in/GestionErreursVirements.java` doit répondre 404 `{"message":"Ressource introuvable"}` (sans `champ`) pour cette cause ; `M/adapter/in/Textes.java` porte `RESSOURCE_INTROUVABLE`.
- `M/adapter/in/PointEntreeVirementsProgrammes.java` -- trois routes `{id}/suspension`, `{id}/reactivation` (POST), `{id}` (DELETE), 204 ; `Long.parseLong(jeton.getSubject())`. `M/infrastructure/AssemblageVirementsProgrammes.java` -- beans des nouveaux cas d'usage.
- `T/application/MemoireCoeurBancaire.java` (~ligne 56, implémente `VirementsProgrammes`) -- étendre le faux ; tests `T/application/*CasUsageTest` (modèle `SupprimerBeneficiaireCasUsageTest`), `T/adapter/out/persistance/VirementsProgrammesJdbcIT.java`, `T/adapter/VirementsProgrammesIT.java` (matrice complète, 401, corps 404 identiques octet pour octet, intégrité du virement d'autrui), `T/adapter/CoeurBancaireSecuriteIT.java` (401 des nouvelles routes), nouveau `T/adapter/VirementProgrammeTransitionConcurrenteTest.java` (modèle `BeneficiaireTransitionConcurrenteTest`, `@SpringBootTest`, fils concurrents, `DonneesDeReference`). Un virement programmé doit être inséré par le test (aucun dans `reference-data.sql`).
- `F/domain/virementsProgrammes.ts` et `F/application/gererVirementsProgrammes.ts` -- étendre le port `ClientVirementsProgrammes` (`suspendre(id)`, `reactiver(id)`, `supprimer(id)`) avec réponses `{statut:'ok'} | {statut:'introuvable'} | {statut:'technique'}` ; cas d'usage (`changerEtat…`/`supprimer…`) qui recharge la liste après succès (`chargerVirementsProgrammes`) et rend l'issue (liste rechargée nulle si relecture échouée).
- `F/adapters/http/ClientVirementsProgrammesHttp.ts` -- trois appels (`appeler` = `fetchAuthentifie`) : 204 ok, 404 introuvable, tout autre statut/coupure technique ; modèle des tests `ClientVirementsProgrammesHttp.test.ts`.
- `F/adapters/ecrans/virementsProgrammes/EcranVirementsProgrammes.tsx` (boutons lignes ~226-231, doc-commentaire à mettre à jour) -- brancher `BoutonTexte` (`onClick`, désactivé pendant l'action) ; erreur dans `vp-message-erreur` (réutiliser l'`AlerteErreur`) ; `F/adapters/textes.ts` (bloc `virementsProgrammes`, ligne ~210+) -- ajouter `erreurAppartenance`, `erreurEtat`, `erreurSuppression` ; tests `EcranVirementsProgrammes.test.tsx` (`[FR-004-09]`, `[FR-004-10]`) et `adapters/accessibilite.test.tsx` si l'écran change de structure. Mocks `vi.fn` du port dans `F/adapters/test/rendreApplication.tsx` et tout test qui construit un `ClientVirementsProgrammes`.
- Tests front à garder verts : `architecture-temps.test.ts`, `confinement-jeton.test.ts`, `couleurs-hors-tokens.test.ts`, `motifs-interdits.test.ts`, `adapters/routes/routes.test.tsx`.
- Lecture seule : `docs/retro/features/FEAT-004/{RG,MESSAGES}.md` (RG-004-040 à 043, 050), `scripts/golden-master/verifier-scenario.sh`, golden master.

## Tasks & Acceptance

**Execution:**
- `M/application/port/out/VirementsProgrammes.java`, `VirementsProgrammesJdbc`, `MemoireCoeurBancaire` -- changement d'état et suppression conditionnels -- FR-004-09, concurrence
- `M/domain/ErreurVirementProgramme.java`, ports `in`, cas d'usage, assemblage -- refus unique INTROUVABLE pour autrui/inexistant/état déjà atteint -- FR-004-10, BUG-013
- `PointEntreeVirementsProgrammes`, `GestionErreursVirements` -- 3 routes, 204, 404 `Ressource introuvable` -- NFR-SEC-04
- Tests back : cas d'usage, IT adaptateur, IT endpoints (toute la matrice), `CoeurBancaireSecuriteIT`, `VirementProgrammeTransitionConcurrenteTest` -- 100 % domain/application, 80 % adapter
- Front : port, cas d'usage, adaptateur HTTP, écran, textes, tests (suspendre, réactiver, supprimer, liste rechargée, 404 → appartenance, technique → état/suppression, boutons désactivés, 401 → `/login`), accessibilité

**Acceptance Criteria:**
- Given un virement actif de `user1`, when il clique « Suspendre », then 204, état inactif, liste rechargée avec « Inactif », Reactiver et Supprimer, sans modale
- Given un virement inactif, when il clique « Reactiver », then actif, « Actif », Suspendre et Supprimer ; date de prochaine exécution et compteur inchangés
- Given un virement, when il clique « Supprimer », then ligne absente de la base et de la liste
- Given le client A et le virement de B, when A suspend, réactive ou supprime par identifiant (appel direct), then 404 `{"message":"Ressource introuvable"}` identique à celui d'un identifiant inexistant, virement de B intact (`FR-004-10`, `BUG-013`) ; l'écran affiche `vp.erreur.appartenance` dans les deux cas
- Given une erreur technique simulée, when il clique Suspendre/Reactiver puis Supprimer, then `vp.erreur.etat` puis `vp.erreur.suppression`, virement inchangé
- Given des demandes concurrentes de suspension et de suppression du même virement actif, when elles s'exécutent, then chaque transition s'applique au plus une fois (`*TransitionConcurrente*Test` vert sous `mvn verify`)
- Given la story terminée, when `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-09|FR-004-10|BUG-013'` puis `cd frontend && npx vitest run -t 'FR-004-09|FR-004-10'`, then code 0
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, then code 0 (100 % domain/application, 80 % adapter/infrastructure) ; `trace-smoke.sh` ajouté à la liste si la pile compose est disponible

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 5 findings — high 0, medium 0, low 3, false 2, maybe-false 0
- findings:
  - `[low]` `[reject]` front et back jamais testés ensemble — chaque côté est testé contre le même contrat (204 / 404 / technique) ; aucun scénario golden master pour 4.4 ; un test croisé ajouterait un montage sans valeur nouvelle.
  - `[low]` `[reject]` journal du refus (identifiants numériques) non vérifié par un test — le message ne contient que des identifiants du client et du virement, aucune donnée sensible ; corriger ajouterait un capteur de journal sans risque réel.
  - `[low]` `[reject]` corps 404 comparé à un littéral local et non à `Textes.RESSOURCE_INTROUVABLE` — le littéral est la valeur exigée par la story ; la constante vaut la même chaîne.
  - `[false]` `[reject]` `@Tag("BUG-013")` sur un seul test d'IT — la story exige un test BUG-013 (virement d'autrui intact), il existe et s'exécute dans `-Dgroups=…BUG-013`.
  - `[false]` `[reject]` `*TransitionConcurrenteTest` peut ne pas tourner sous `mvn verify` — même nom et même mécanisme que `BeneficiaireTransitionConcurrenteTest` (story 4.1), exécuté par surefire.

## Design Notes

Statuts et chemins non fixés par la story (choix de planification) : `POST …/suspension`, `POST …/reactivation`, `DELETE …/{id}`, 204. La suppression ne dépend pas de l'état (legacy RG-004-042), la suspension et la réactivation si ; « une seule transition s'applique » se lit transition par transition : de deux requêtes identiques simultanées une seule gagne ; suspension puis suppression en séquence sont deux transitions légitimes de l'ordre sériel, jamais deux fois la même. Le 404 « état déjà atteint » (suspendre un inactif) reprend le message d'appartenance : « Rechargez la page » est précisément le bon conseil devant une liste périmée. La réactivation ne recalcule pas la date (l'exécution est livrée par 4.6).

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-09|FR-004-10|BUG-013'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-004-09|FR-004-10'` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** suspension, réactivation et suppression définitive d'un virement programmé. Cœur bancaire : `POST …/{id}/suspension`, `POST …/{id}/reactivation`, `DELETE …/{id}` (204), mises à jour conditionnelles sur le client et l'état attendu ; autrui, inexistant ou état déjà atteint donnent le même 404 `{"message":"Ressource introuvable"}` (BUG-013). Front : boutons câblés sans modale, liste rechargée, textes `vp.erreur.appartenance`, `vp.erreur.etat`, `vp.erreur.suppression`, boutons désactivés pendant l'action.

**Fichiers :** back — `GererEtatVirementProgramme` (port) et son cas d'usage, `VirementsProgrammes` / `VirementsProgrammesJdbc` (`changerEtat`, `supprimer`), cause `INTROUVABLE`, `PointEntreeVirementsProgrammes`, `GestionErreursVirements`, assemblage, tests (cas d'usage, IT JDBC, IT endpoints, sécurité, `VirementProgrammeTransitionConcurrenteTest`) ; front — port et cas d'usage `agirSurVirementProgramme`, `ClientVirementsProgrammesHttp`, `EcranVirementsProgrammes`, textes, `BoutonTexte` (prop `desactive`), `rendreApplication`, tests.

**Revue :** 0 patch, 0 report, 5 constats rejetés (3 négligeables, 2 faux). Relance recommandée : non.

**Vérification :** `mvn … -Dgroups='FR-004-09|FR-004-10|BUG-013'` code 0 ; `vitest -t 'FR-004-09|FR-004-10'` 26 verts ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh` code 0.

**Risques résiduels :** `trace-smoke.sh` non lancé (pile compose requise, aucun span ni Kafka ajouté) ; chemins et statuts d'API choisis à la planification ; `.bmad-loop/policy.toml` a été modifié par ailleurs pendant la session (changement de modèle, hors de cette story) et n'est pas commité.
