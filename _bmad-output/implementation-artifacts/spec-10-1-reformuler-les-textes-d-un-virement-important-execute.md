---
title: 'Reformuler les textes d''un virement important exécuté'
type: 'bugfix'
created: '2026-10-09'
status: 'done'
baseline_revision: '5bd9897d8bcb84929355340a2688ca11af797572'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
warnings: ['oversized']
deferred: []
---

<intent-contract>

## Intent

**Problem:** Un virement de 5 000 à 9 999,99 € de score ≤ 75 est exécuté et débité (statut technique `PENDING`), mais il est annoncé « en attente de validation … verification en cours » et ses opérations portent « [EN ATTENTE] » : le client croit que l'argent n'est pas parti (BUG-009, conservé jusqu'ici pour la démonstration, décision B1 révisée).

**Approach:** Appliquer FR-003-29 : dans le seul fichier de textes du cœur bancaire, remplacer la notification ALERTE par « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. » et la mention des opérations par « [TRANSACTION IMPORTANTE] » ; côté front, le relevé reconnaît cette nouvelle mention (badge « Transaction importante », préfixe retiré) en gardant la reconnaissance de « [EN ATTENTE] » pour les opérations déjà en base.

## Boundaries & Constraints

**Always:**
- Statut enregistré inchangé : `PENDING` (valeur technique), débit immédiat (FR-003-10), une seule notification par virement (FR-002-10), publiée par le cœur bancaire dans `coeur-bancaire.virement-decide.v1`.
- Textes exacts de FR-003-29, sans accents : « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. », « [TRANSACTION IMPORTANTE] », « Transaction importante ». `{montant}` au même format que l'ancienne ALERTE (`Montant.texte()`, ex. `5000.00`) ; `{numCompte}` = numéro du compte destinataire (celui déjà passé à `pourExecution`).
- Côté back, les deux textes vivent dans `TextesVirement` uniquement (AR-68). Côté front, le badge vient de `textes.ts`.
- FR-005-07 inchangée : « [EN ATTENTE] » + espace (13 caractères) → badge « En attente », préfixe retiré, fond warning.
- Tests tagués `@Tag("FR-003-29")` et `@Tag("BUG-009")` (JUnit), titres `[FR-003-29]` et `[BUG-009]` (Vitest). Le test BUG-009 côté back doit échouer si le texte ALERTE legacy est rétabli dans `TextesVirement`.
- Nouveau `data-testid` au format `{écran}-{élément}[-{id}]` : `releve-transaction-importante-{index}` ; les `data-testid` existants restent identiques.
- Couverture 100 % lignes/branches sur domain et application (back et front).

**Never:**
- Ne pas toucher `replay-mapping.json`, `docs/retro/golden-master/derived/**`, `e2e/golden-master/**`, ni rejouer le golden master.
- Ne pas modifier les seuils (`nb.fraude.*`), ni le comportement SUCCESS (< 5 000 €) ou SUSPENDU (≥ 10 000 € ou score > 75).
- Ne pas migrer ni réécrire les opérations déjà en base ni les données de référence.
- Ne pas renommer `StatutAffichage.PENDING` ni la valeur `PENDING` renvoyée par l'API.
- Ne modifier aucun fichier interdit par `docs/quality-gates.md`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Important exécuté | 5 000,00 € de `FR76-0001-0001` vers `FR76-0001-0002`, motif `loyer`, score ≤ 75 | statut `PENDING`, débit fait ; ALERTE « Virement de 5000.00 EUR vers le compte FR76-0001-0002 execute - transaction importante. » ; DEBIT « [TRANSACTION IMPORTANTE] Virement vers FR76-0001-0002 - loyer », CREDIT « [TRANSACTION IMPORTANTE] Virement depuis FR76-0001-0001 - loyer » | aucune |
| Sans motif | 5 000,00 €, motif vide | DEBIT « [TRANSACTION IMPORTANTE] Virement vers FR76-0001-0002 » | aucune |
| Sous le seuil | 4 999,99 € | `SUCCESS`, CONFIRMATION, descriptions sans préfixe | aucune |
| Seuil de fraude | 10 000,00 € | `SUSPENDU`, aucun débit, seule notification FRAUDE | aucune |
| Libéré puis débité | virement PENDING suspendu puis libéré (`executerApresLiberation`) | opérations « [TRANSACTION IMPORTANTE] … » | aucune |
| Relevé, nouvelle mention | description « [TRANSACTION IMPORTANTE] Virement vers X » | badge « Transaction importante », libellé « Virement vers X », aucun « en attente » ni « verification » (insensible à la casse) | aucune |
| Relevé, ancienne mention | description « [EN ATTENTE] Virement vers X » | badge « En attente », libellé « Virement vers X », fond warning (inchangé) | aucune |
| Relevé, préfixe mal formé | « [TRANSACTION IMPORTANTE]sans espace », « [transaction importante] x » | ni badge ni retrait | aucune |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/TextesVirement.java` -- seul fichier de textes : `EN_ATTENTE` (l.35), `TRANSACTION_IMPORTANTE` (l.37-38, texte legacy), `transactionImportante(String montant)` (l.~70), `libelle(...)` privé qui préfixe quand `statut == PENDING`.
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/NotificationVirement.java:22-30` -- `pourExecution(virement, numeroDestinataire, seuils)` : branche ALERTE appelle `transactionImportante(montant)` ; `numeroDestinataire` déjà disponible.
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/Virement.java:110-114` -- `operations(...)` appelle `libelleDebit`/`libelleCredit` avec le statut ; aucun changement attendu.
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/StatutAffichage.java` -- javadoc « BUG-009 conservé provisoirement » à mettre à jour.
- `services/coeur-bancaire/src/main/resources/application.properties:41` -- commentaire « opérations « [EN ATTENTE] » » à mettre à jour.
- Tests back qui figent l'ancien texte (à mettre à jour) : `domain/ModelesDuDomaineTest.java:116,130-133`, `domain/VirementTest.java:124-125`, `domain/RevueVirementTest.java:109`, `application/ExecuterVirementCasUsageTest.java:101-102,170-171`, `application/TraiterVirementATraiterCasUsageTest.java:82-84`, `adapter/VirementIT.java:286-300` (tous sous `services/coeur-bancaire/src/test/java/fr/nextbanking/coeurbancaire/`). `adapter/ReleveIT.java:87,99` insère « [EN ATTENTE] » en base : à garder (donnée antérieure). `ExecuterEcheancesCasUsageTest.java:198` : à garder.
- `services/notifications/.../RecepteurVirementDecideTest.java` -- message arbitraire stocké tel quel : hors périmètre.
- `frontend/src/domain/releve.ts` -- `PREFIXE_EN_ATTENTE`, `LigneReleve.enAttente`, `ligneReleve()` : ajouter la reconnaissance de « [TRANSACTION IMPORTANTE] ».
- `frontend/src/domain/releve.test.ts:43-55` -- tests FR-005-07 existants, modèle pour les nouveaux.
- `frontend/src/adapters/ecrans/releve/EcranReleve.tsx:103-108` -- ligne, classe `ecran-releve__ligne--attente`, `<Etiquette data-testid="releve-en-attente-{i}">`.
- `frontend/src/adapters/ecrans/releve/EcranReleve.test.tsx:29,110,126-132` -- tests d'écran du relevé.
- `frontend/src/adapters/textes.ts:484` -- `releve.enAttente: 'En attente'` ; ajouter `transactionImportante`.
- `frontend/src/adapters/ecrans/tableauDeBord/EcranTableauDeBord.tsx:131` -- description brute des 5 dernières opérations et centre de notifications (message brut) : aucun changement, à couvrir par un test `[BUG-009]`.
- `frontend/src/adapters/ecrans/virement/SectionSuiviVirements.tsx:60` -- suivi : état `EXECUTE` → « Exécuté - débité » (`textes.ts:71`) ; aucun changement.

## Tasks & Acceptance

**Execution:**
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/TextesVirement.java` -- remplacer la mention « [EN ATTENTE] » par `MENTION_TRANSACTION_IMPORTANTE = "[TRANSACTION IMPORTANTE]"` et le texte ALERTE par « Virement de %s EUR vers le compte %s execute - transaction importante. » ; `transactionImportante(montant, numeroDestinataire)` ; javadoc citant FR-003-29 / BUG-009 corrigé -- AR-68, un seul fichier de textes.
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/NotificationVirement.java` -- passer `numeroDestinataire` à `transactionImportante` -- `{numCompte}`.
- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/StatutAffichage.java`, `services/coeur-bancaire/src/main/resources/application.properties` -- mettre à jour javadoc et commentaire -- plus de mention « BUG-009 conservé » ni « [EN ATTENTE] ».
- Tests back listés dans la Code Map -- remplacer les attentes legacy par les nouveaux textes ; ajouter `@Tag("FR-003-29")` et `@Tag("BUG-009")` aux tests du cas « Important exécuté » (domaine, cas d'usage, `VirementIT`) ; ajouter un test `@Tag("BUG-009")` qui vérifie sur la notification ALERTE et les deux descriptions qu'aucun texte ne contient « en attente » ni « verification » (insensible à la casse) et que le message vaut exactement le texte FR-003-29 -- échoue si le texte legacy est rétabli.
- `frontend/src/domain/releve.ts` -- ajouter `PREFIXE_TRANSACTION_IMPORTANTE = '[TRANSACTION IMPORTANTE] '` et `LigneReleve.transactionImportante: boolean` ; retirer le préfixe reconnu (l'un ou l'autre) du libellé -- FR-005-07 étendue par FR-003-29.
- `frontend/src/domain/releve.test.ts` -- tests `[FR-003-29]`/`[BUG-009]` des lignes « Relevé » de la matrice (nouvelle mention, ancienne mention, préfixes mal formés) -- couverture 100 % domaine.
- `frontend/src/adapters/textes.ts` -- `releve.transactionImportante: 'Transaction importante'` -- texte de FR-003-29.
- `frontend/src/adapters/ecrans/releve/EcranReleve.tsx` -- afficher `<Etiquette data-testid="releve-transaction-importante-{i}">` avant le libellé et appliquer le même fond que la ligne en attente -- même traitement que FR-005-07.
- `frontend/src/adapters/ecrans/releve/EcranReleve.test.tsx` -- tests `[FR-003-29]` et `[BUG-009]` : badge, libellé sans préfixe, `textContent` de la ligne sans « en attente » ni « verification » (insensible à la casse) ; l'ancienne mention garde son badge.
- `frontend/src/adapters/ecrans/tableauDeBord/EcranTableauDeBord.test.tsx` (et le test de `SectionSuiviVirements` s'il existe, sinon `EcranVirement.test.tsx`) -- test `[BUG-009]` : avec une opération « [TRANSACTION IMPORTANTE] … » et la notification ALERTE FR-003-29, le texte rendu ne contient ni « en attente » ni « verification » ; le suivi affiche « Exécuté - débité » pour ce virement.

**Acceptance Criteria:**
- Given le code livré, when `grep -rn "en attente de validation" services/coeur-bancaire/src/main` est lancé, then il ne trouve rien.
- Given les tests tagués, when `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-003-29 | BUG-009'` est lancé, then le code de sortie est 0 ; and, given le texte ALERTE legacy rétabli temporairement dans `TextesVirement`, `-Dgroups='BUG-009'` sort en code non nul (contre-épreuve faite puis annulée).
- Given le front, when `cd frontend && npx vitest run -t 'FR-003-29'` puis `-t 'BUG-009'` sont lancés, then chacun sort en code 0.
- Given le golden master, when `grep -rniE "en attente de validation|verification en cours|\[EN ATTENTE\]" docs/retro/golden-master/derived/*/*/expected-dom.txt` est lancé, then il ne trouve rien, and `git diff --exit-code -- replay-mapping.json docs/retro/golden-master/derived e2e/golden-master` sort en code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- verdicts: 8 findings — high 0, medium 0, low 0, false 8, maybe-false 0
- findings:
  - `[false]` `[reject]` Aucun test ne relie le texte produit par le cœur bancaire à l'écran rendu ; le centre de notifications passe par le service notifications, hors diff — `RecepteurVirementDecideTest` montre que `notifications` stocke le message reçu tel quel, et `VirementIT` vérifie le message exact publié dans l'outbox de `coeur-bancaire.virement-decide.v1` : le texte affiché est donc celui de FR-003-29, aucun « en attente » ne peut apparaître.
  - `[false]` `[reject]` Au tableau de bord, les 5 dernières opérations affichent la description brute « [TRANSACTION IMPORTANTE] … », sans badge — la story ne demande badge et retrait du préfixe qu'au relevé ; la description brute ne contient ni « en attente » ni « verification » (test `[BUG-009]` du tableau de bord).
  - `[false]` `[reject]` Le suivi des virements n'est testé que via le tableau de bord — `SectionSuiviVirements` est le même composant ; l'état `EXECUTE` affiche « Exécuté - débité » (code inchangé), vérifié par le test `[BUG-009]`.
  - `[false]` `[reject]` La ligne « Transaction importante » garde le fond warning de la ligne en attente — FR-003-29 demande que le relevé traite la nouvelle mention comme FR-005-07 traite « [EN ATTENTE] » ; aucun autre style n'est défini et le critère porte sur le texte, respecté.
  - `[false]` `[reject]` La nouvelle mention s'applique aussi à un virement PENDING ≥ 10 000 € libéré puis débité — FR-003-29 vise « un virement PENDING, exécuté et débité » : un virement libéré puis débité l'est, et lui garder « [EN ATTENTE] » reproduirait BUG-009.
  - `[false]` `[reject]` FR-003-10 du PRD décrit encore les textes legacy alors que ses tests attendent les nouveaux — FR-003-10 et FR-003-29 précisent que les textes legacy s'appliquent « d'ici là » (jusqu'à cette story) : le PRD est cohérent, aucun défaut de code.
  - `[false]` `[reject]` L'écart de texte avec le golden master « traité à l'étape G2 » n'apparaît pas dans le diff — l'intention et les règles du projet interdisent de toucher au golden master hors story 10.2 ; aucune ligne attendue ne contient les anciens textes (grep vide, `git diff --exit-code` à 0).
  - `[false]` `[reject]` Le diff seul ne prouve ni la contre-épreuve ni les contrôles — exécutés : contre-épreuve BUG-009 en échec (4 tests) puis annulée, `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` à 0 (voir Auto Run Result).

## Design Notes

Le fond de ligne du relevé pour « Transaction importante » reprend celui de « En attente » (classe existante) : FR-003-29 dit que le relevé traite la nouvelle mention « comme FR-005-07 le fait pour [EN ATTENTE] », et aucun autre style n'est défini. La classe peut être renommée en un nom neutre si le CSS est local à l'écran.

`releve.ts` attendu, à titre d'exemple :
```ts
const enAttente = operation.description.startsWith(PREFIXE_EN_ATTENTE);
const transactionImportante = operation.description.startsWith(PREFIXE_TRANSACTION_IMPORTANTE);
const prefixe = enAttente ? PREFIXE_EN_ATTENTE : transactionImportante ? PREFIXE_TRANSACTION_IMPORTANTE : '';
// libelle: operation.description.slice(prefixe.length)
```

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-003-29 | BUG-009'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-003-29' && npx vitest run -t 'BUG-009'` -- expected: code 0
- `scripts/gates/build.sh` -- expected: code 0 (couverture, ArchUnit, dependency-cruiser, tsc)
- `scripts/gates/check-gates-exist.sh` -- expected: code 0
- `scripts/gates/golden-master.sh` -- expected: code 0
- `scripts/gates/trace-smoke.sh` -- expected: code 0 (si l'environnement compose peut démarrer)

## Auto Run Result

Status: done

**Résumé :** BUG-009 corrigé (FR-003-29). Un virement PENDING exécuté et débité est annoncé « transaction importante » : notification ALERTE « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. », opérations préfixées « [TRANSACTION IMPORTANTE] », badge « Transaction importante » au relevé. Le statut technique `PENDING`, les seuils et les cas SUCCESS / SUSPENDU sont inchangés ; « [EN ATTENTE] » reste reconnu au relevé pour les opérations déjà en base.

**Fichiers modifiés :**
- `services/coeur-bancaire/.../domain/TextesVirement.java` — nouvelle mention et nouveau texte ALERTE (avec le numéro du destinataire), seul fichier de textes.
- `services/coeur-bancaire/.../domain/NotificationVirement.java` — passe le numéro du compte destinataire au texte ALERTE.
- `services/coeur-bancaire/.../domain/StatutAffichage.java`, `src/main/resources/application.properties` — javadoc et commentaire à jour.
- `services/coeur-bancaire/src/test/.../domain/TransactionImportanteTest.java` — nouveau test `FR-003-29` / `BUG-009` (texte exact, ni « en attente » ni « verification »).
- `ModelesDuDomaineTest`, `VirementTest`, `RevueVirementTest`, `ExecuterVirementCasUsageTest`, `TraiterVirementATraiterCasUsageTest`, `VirementIT` — attentes mises à jour et tags ajoutés ; `VirementIT` vérifie aussi la notification publiée dans l'outbox.
- `frontend/src/domain/releve.ts` (+ test) — reconnaissance de « [TRANSACTION IMPORTANTE] », préfixe retiré.
- `frontend/src/adapters/textes.ts` — texte « Transaction importante ».
- `frontend/src/adapters/ecrans/releve/EcranReleve.tsx` (+ test) — badge `releve-transaction-importante-{i}`.
- `frontend/src/adapters/ecrans/tableauDeBord/EcranTableauDeBord.test.tsx` — test `[BUG-009]` : opérations, notification, suivi.
- `frontend/src/application/consulterReleve.test.ts` — nouveau champ de la ligne.

**Revue :** 8 constats (couche d'alignement sur l'intention), tous `false` et rejetés (raisons dans le Review Triage Log) ; aucun correctif, aucun report.

**Revue complémentaire recommandée :** non (0 correctif appliqué : high 0, medium 0, low 0).

**Vérification :**
- `scripts/gates/build.sh` — code 0 (Maven clean verify, ArchUnit, couverture, dependency-cruiser, tsc, Vitest 1986/1986).
- `scripts/gates/check-gates-exist.sh` — code 0, aucun KO.
- `scripts/gates/golden-master.sh` — code 0 ; `git diff --exit-code -- replay-mapping.json docs/retro/golden-master/derived e2e/golden-master` — code 0 ; grep des anciens textes dans `expected-dom.txt` — aucun résultat.
- `scripts/gates/trace-smoke.sh` — code 0 (lancé par le sous-agent d'implémentation, sur une pile compose déjà démarrée).
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-003-29 | BUG-009'` — code 0 ; contre-épreuve (texte ALERTE legacy rétabli) : `-Dgroups='BUG-009'` en échec (4 tests), puis texte restauré.
- `npx vitest run -t 'FR-003-29'` (8 tests) et `-t 'BUG-009'` (3 tests) — code 0.
- `grep -rn "en attente de validation" services/coeur-bancaire/src/main` — aucun résultat.

**Risques résiduels :**
- `trace-smoke.sh` a tourné sur des images construites avant le changement : il prouve le tracing, pas les nouveaux textes.
- Un test d'accessibilité de `/administration` (`accessibilite.test.tsx`) a dépassé une fois son délai de 5 s sous charge pendant l'implémentation ; il passe seul et dans le `build.sh` final.
- Aucune action humaine n'est due (pas d'`operator_actions`).
