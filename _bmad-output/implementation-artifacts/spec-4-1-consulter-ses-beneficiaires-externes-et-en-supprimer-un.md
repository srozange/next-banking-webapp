---
title: 'Consulter ses bénéficiaires externes et en supprimer un'
type: 'feature'
created: '2026-10-08'
status: 'done'
baseline_revision: '19f64b5b806a3773705e451d6f7bf1c9d8dc6a4a'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/docs/quality-gates.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** Le client n'a aucun écran `/beneficiaires` ni API pour lister ses bénéficiaires externes SEPA ni en supprimer un (la table `beneficiaires_externes` n'est lue que par le signal NEW_BENEFICIARY). Le legacy laissait supprimer le bénéficiaire d'autrui (BUG-047).

**Approach:** Cœur bancaire : `GET /api/coeur-bancaire/beneficiaires` (actifs du client du jeton, triés par libellé, IBAN masqué `FR35***9010`) et `DELETE /api/coeur-bancaire/beneficiaires/{id}` (désactivation logique par mise à jour conditionnelle, 403 « Erreur lors de la suppression. » pour un bénéficiaire d'autrui). Front : écran `/beneficiaires` protégé (tableau + bouton Supprimer, liste rafraîchie sans rechargement).

## Boundaries & Constraints

**Always:** Client déduit du jeton (`sub`), jamais d'un paramètre. Hexagone respecté (domain/application sans Spring). Textes à l'identique du legacy (`docs/retro/00-global/MESSAGES.md`, FEAT-013), aucun texte inventé. Désactivation = `actif=false`, ligne conservée. IBAN en clair jamais dans les journaux (objet-valeur `Iban`, forme `FR35****9010`). Masque d'affichage (3 étoiles : 4 premiers + `***` + 4 derniers) distinct de `Iban.toString()` (4 étoiles). Zone SEPA calculée depuis le code pays (RG-013-010). Montants/temps : sans objet ici. `data-testid` `beneficiaires-supprimer-{id}`. Couleurs par tokens uniquement.

**Never:** Pas de formulaire d'ajout ni de vérification d'IBAN (story 4.2) ; pas de modale de confirmation ; ne pas modifier scripts/gates/**, tests golden master ni `replay-mapping.json` ; ne pas rejouer le golden master ; pas d'appel HTTP entre services ; aucun seuil abaissé ni exclusion de couverture.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Erreur |
|----------|--------------|----------------------|--------|
| Liste nominale | `user1`, bénéficiaires actifs, un désactivé, un d'un autre client | Seuls les actifs de `user1`, triés par libellé croissant ; titre « Beneficiaires ({n}) » | — |
| Liste vide | Client sans bénéficiaire actif | En-têtes conservés + ligne pleine largeur « Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un. » | — |
| Masquage / affichage | IBAN `FR3530004000000123456789010`, sans BIC ni banque ; IBAN hors zone SEPA | `FR35***9010`, BIC et Banque « — », « ✓ SEPA » ; l'autre « Hors SEPA » | — |
| Sans jeton | GET/DELETE sans jeton ; `/beneficiaires` sans session | 401 `{"message":…}` ; front redirige vers `/login` | 401 |
| Suppression nominale | `user1` supprime un de ses bénéficiaires actifs | 204, `actif=false`, ligne conservée, liste rafraîchie sans rechargement | — |
| Bénéficiaire d'autrui | Client A supprime un bénéficiaire actif de B | 403 `{"message":"Erreur lors de la suppression."}` ; B inchangé (BUG-047) | 403 |
| Concurrence | Deux DELETE simultanés du même bénéficiaire | Une seule transition actif→inactif ; la perdante reçoit 404 `{"message":"Erreur lors de la suppression."}` | 404 |
| Identifiant inexistant / déjà désactivé | DELETE sur id absent ou inactif | 404 `{"message":"Erreur lors de la suppression."}` | 404 |
| Journaux | Suppression d'un bénéficiaire d'IBAN `FR3530004000000123456789010` | Aucune ligne de journal ne contient l'IBAN en clair ; `FR35****9010` seulement | — |

</intent-contract>

## Code Map

Chemins Java sous `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` (`M/`), tests sous `src/test/java/fr/nextbanking/coeurbancaire/` (`T/`).

- `M/application/port/out/Beneficiaires.java` + `M/adapter/out/persistance/BeneficiairesJdbc.java` -- port/adaptateur existants (`estActif` pour NEW_BENEFICIARY) à étendre : liste des actifs d'un client, lecture par id, désactivation conditionnelle (`update … set actif=false where id=? and client_id=? and actif`). JdbcClient, pas de JPA.
- `services/coeur-bancaire/src/main/resources/db/migration/V3__alertes_fraude_et_beneficiaires.sql` -- table `beneficiaires_externes` (id, client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif, cree_le) ; dernière migration V5, prochaine V6 seulement si utile (index `(client_id, libelle)`, défaut `actif`).
- `M/domain/Iban.java` -- record, `toString()` masque 4 étoiles ; ajouter une forme d'affichage 3 étoiles sans casser `IbanTest` (`T/domain/IbanTest.java`).
- `M/adapter/in/PointEntreeVirements.java` -- modèle de contrôleur (`clientId(Jwt)` ligne ~107, DTO `record` internes) ; créer `PointEntreeBeneficiaires` dans `M/adapter/in/` sur ce modèle.
- `M/application/ConsulterReleveCasUsage.java`, `port/in/ConsulterReleve.java` -- modèle de cas d'usage avec décision de propriété ; créer ports `ConsulterBeneficiaires` / `SupprimerBeneficiaire` et leurs cas d'usage.
- `M/adapter/in/GestionErreursVirements.java` -- modèle de handler `{"message"}` avec `assignableTypes` ; prévoir un handler pour `ErreurBeneficiaire` (nouvelle exception de domaine, `ETRANGER`→403, `INTROUVABLE`→404, texte « Erreur lors de la suppression. » dans le domaine comme `TextesVirement`).
- `M/infrastructure/AssemblageVirement.java` (ligne ~70) -- modèle de câblage `@Bean` ; créer `AssemblageBeneficiaires`.
- `services/coeur-bancaire/reference-data/reference-data.sql` -- ajouter le bénéficiaire `FR76***` de `user1` (client 1, IBAN commençant par `FR76`, SEPA) ; `T/adapter/DonneesDeReference.java` le recharge et tronque déjà `beneficiaires_externes`.
- `T/adapter/ReleveIT.java` (modèle `@SpringBootTest`/Testcontainers/`Jetons`), `T/application/ConsulterReleveCasUsageTest.java` + `T/application/MemoireCoeurBancaire.java` (faux en mémoire à étendre), `T/adapter/out/persistance/ComptesJdbcIT.java`, `T/adapter/CoeurBancaireSecuriteIT.java` (liste de chemins protégés ligne ~63 : ajouter `/api/coeur-bancaire/beneficiaires`), `T/adapter/VirementTransitionConcurrenteTest.java` (modèle de `BeneficiaireTransitionConcurrenteTest`), `T/application/ControleFraudeCasUsageTest.java` lignes ~28-100 (modèle de capture de journal pour le masquage).
- Front (`frontend/src/`) : `domain/releve.ts`, `application/consulterReleve.ts` (+ `.test.ts`), `adapters/http/ClientReleveHttp.ts` (+ test), `adapters/ecrans/releve/EcranReleve.tsx|.css|.test.tsx` -- modèles par couche ; créer les équivalents `beneficiaires` (domaine : masque/zone/tri si besoin côté front, application : charger/supprimer avec port `ClientBeneficiaires`, adaptateur HTTP via `fetchAuthentifie`, écran `EcranBeneficiaires`).
- `frontend/src/adapters/routes/routes.tsx` -- interface `Dependances` (ajouter `clientBeneficiaires`) et route `/beneficiaires` sous `<RouteProtegee>` (le littéral `'/beneficiaires'` est exigé par `scripts/golden-master/verifier-scenario.sh`) ; `frontend/src/infrastructure/main.tsx` (ligne ~54) pour l'instanciation ; `adapters/test/rendreApplication.tsx` pour les tests.
- `frontend/src/adapters/textes.ts` -- `TEXTES.titresOnglet` (ajouter `beneficiaires: 'BankApp Demo - Beneficiaires'`) et bloc `beneficiaires` (titre, colonnes, vide, « — », « ✓ SEPA », « Hors SEPA », « Supprimer ») ; le lien de menu `nav-beneficiaires` existe déjà (`domain/navigation.ts` ~46).
- Lecture seule : `docs/retro/features/FEAT-013/` (RG-013-003/004/005/007/010, MESSAGES MSG-013-080), `docs/retro/golden-master/derived/FEAT-013/SC-013-01/` (expected-dom : `MES BENEFICIAIRES ENREGISTRES`, `LIBELLE TITULAIRE IBAN BIC BANQUE ZONE SEPA ACTION`, `✓ SEPA`, `FR76***` ; `AJOUTER UN BENEFICIAIRE` est de la 4.2, non bloquant).
- Tests transverses front à garder verts : `architecture-temps.test.ts`, `confinement-jeton.test.ts`, `couleurs-hors-tokens.test.ts`, `motifs-interdits.test.ts`, `adapters/accessibilite.test.tsx`.

## Tasks & Acceptance

**Execution:**
- `M/domain/` (Iban, ErreurBeneficiaire, texte d'erreur, zone SEPA/masque d'affichage) -- ajouter objets-valeur et règles pures -- hexagone, 100 % couverture
- `M/application/` + ports `in`/`out` -- cas d'usage consulter/supprimer, propriété décidée dans le cas d'usage, journalisation via `Iban` -- BUG-047, FR-013-10
- `M/adapter/out/persistance/BeneficiairesJdbc.java` (+ migration V6 si utile) -- liste triée par libellé, lecture, désactivation conditionnelle -- transition unique
- `M/adapter/in/PointEntreeBeneficiaires.java`, handler d'erreurs, `M/infrastructure/AssemblageBeneficiaires.java` -- endpoints GET/DELETE, 401/403/404 `{"message"}`
- `reference-data.sql` -- bénéficiaire `FR76***` de `user1`
- Tests back (`@Tag("FR-013-01|02|03|09|10")`, `BUG-047`) : cas d'usage, adaptateur IT, endpoint IT, `CoeurBancaireSecuriteIT`, `BeneficiaireTransitionConcurrenteTest`, masquage du journal -- couvrir toute la matrice
- Front : domaine/application/adaptateur HTTP/écran/route/textes/câblage + tests Vitest préfixés `[FR-013-01]`… (liste, vide, masque, suppression sans rechargement, redirection `/login`, titre d'onglet, accessibilité `<th scope="col">`)

**Acceptance Criteria:**
- Given un visiteur sans jeton, when il demande `/beneficiaires` ou l'API, then redirection `/login` côté front et 401 `{"message":…}` côté API
- Given `user1` connecté, when il ouvre `/beneficiaires`, then titre d'onglet « BankApp Demo - Beneficiaires », section « Beneficiaires ({n}) », colonnes Libelle/Titulaire/IBAN/BIC/Banque/Zone SEPA/Action, seuls ses bénéficiaires actifs triés par libellé, chacun avec `beneficiaires-supprimer-{id}`
- Given un bénéficiaire sans BIC ni banque ou hors SEPA, when la liste s'affiche, then « — », `FR35***9010`, « ✓ SEPA » / « Hors SEPA »
- Given une suppression par le propriétaire ou par un autre client, when elle est traitée, then désactivation logique ou 403 « Erreur lors de la suppression. » sans effet sur le bénéficiaire d'autrui
- Given la story terminée, when `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-013-01|FR-013-02|FR-013-03|FR-013-09|FR-013-10|BUG-047'` et `cd frontend && npx vitest run -t 'FR-013-01|FR-013-02|FR-013-03|FR-013-09|FR-013-10'`, then code 0
- Given l'écran livré, when `scripts/golden-master/verifier-scenario.sh SC-013-01`, then code 0 (ne pas rejouer le golden master)
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh`, then tous verts (couverture 100 % domain/application, 80 % adapter/infrastructure)

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 3 findings — high 0, medium 0, low 0, false 3, maybe-false 0
- findings:
  - `[false]` `[reject]` Golden master SC-013-01 non exercé par le diff — `scripts/golden-master/verifier-scenario.sh SC-013-01` relancé en revue : code 0 ; `scripts/gates/golden-master.sh` : code 0. Le rejeu du golden master est interdit avant l'epic 10.
  - `[false]` `[reject]` Masque à 3 étoiles testé sans lien de bout en bout — le masque est produit par le service (`Iban.affichage()`), vérifié en IT HTTP `BeneficiairesIT` (FR-013-03) ; le front affiche la chaîne reçue, sans calcul.
  - `[false]` `[reject]` Journal capté sur un seul logger — `BeneficiairesIT` (BUG-086) capte le logger de `SupprimerBeneficiaireCasUsage`, seul émetteur de la suppression ; l'IBAN en clair n'est journalisé nulle part ailleurs.

## Design Notes

Le DELETE d'un bénéficiaire inexistant, déjà désactivé ou perdant d'une course concurrente répond 404 avec le même message (le legacy n'impose rien de plus) ; le 403 est réservé au bénéficiaire actif d'un autre client. Le masque d'affichage (3 étoiles) est calculé côté service, pour que le front ne manipule jamais l'IBAN en clair.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-013-01|FR-013-02|FR-013-03|FR-013-09|FR-013-10|BUG-047'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-013-01|FR-013-02|FR-013-03|FR-013-09|FR-013-10'` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-013-01` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** API `GET`/`DELETE /api/coeur-bancaire/beneficiaires` du cœur bancaire (liste des actifs du client du jeton triés par libellé, IBAN masqué `FR35***9010`, zone SEPA, désactivation logique conditionnelle, 403 « Erreur lors de la suppression. » pour un bénéficiaire d'autrui, 404 pour inexistant/déjà désactivé) et écran front `/beneficiaires` protégé.

**Fichiers :** back — `PointEntreeBeneficiaires`, `ConsulterBeneficiairesCasUsage`, `SupprimerBeneficiaireCasUsage`, ports, `BeneficiaireExterne`, `ErreurBeneficiaire`, `Iban` (affichage, zoneSepa), `BeneficiairesJdbc`, `AssemblageBeneficiaires`, `GestionErreursVirements`, `reference-data.sql` et leurs tests (dont `BeneficiaireTransitionConcurrenteTest`) ; front — domaine/application/adaptateur HTTP/`EcranBeneficiaires`, route, textes, câblage et tests.

**Revue :** 0 patch, 0 report, 3 constats rejetés (faux, raisons au journal de triage). Relance recommandée : non.

**Vérification :** `mvn … -Dgroups=FR-013…|BUG-047` 24 tests verts ; `vitest -t FR-013…` 44 verts, suite front 1314 verts ; `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `verifier-scenario.sh SC-013-01` code 0.

**Risques résiduels :** la liste des pays SEPA est un choix de l'implémentation (le corpus ne la fournit pas), à confirmer ; tri par libellé sensible à la casse ; `trace-smoke.sh` non lancé (pile compose requise, story sans Kafka ni spans) ; textes de SC-013-01 « non trouvés » non bloquants (en-têtes en capitales via CSS, « AJOUTER UN BENEFICIAIRE » relève de la 4.2).
