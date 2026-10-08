---
title: 'Porter les montants et les identifiants par des types du domaine'
type: 'refactor'
created: '2026-10-08'
status: 'done'
baseline_revision: 'e0f5fb9a1e9f5b6c959182673d21627b83f7679a'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/quality-gates.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-0-context.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** Dans `coeur-bancaire`, le chemin du virement porte montants (`BigDecimal`), identifiants de compte (`long`) et libellé (`String`) en primitives : les règles (2 décimales, montant positif, identifiants distincts, motif ≤ 100 caractères) sont recalculées dans `Virement`, et les fabriques `executer`/`suspendre`/`refuser` prennent 8 à 9 paramètres.

**Approach:** Introduire dans `domain` les types `Montant`, `CompteId` et `Motif` qui valident à la construction, et un objet de demande qui remplace les listes de primitives des fabriques de `Virement`. Le comportement observable ne change pas (JSON `"100.00"`, colonnes `numeric(19,2)`, erreurs, textes, golden master). Le contrôle du rôle `ADMIN` est déjà appelé depuis un point unique par service : on le garde et on l'épingle par un test d'architecture.

## Boundaries & Constraints

**Always:**
- `Montant` : enveloppe un `BigDecimal` d'échelle exactement 2, refuse à la construction une échelle différente, une valeur nulle de référence (`null`) ou négative ; aucun constructeur ni fabrique ne contourne ces règles. `Montant.lire(String)` reprend à l'identique la lecture actuelle de `Virement.lireMontant` (strip, > 0, ≤ 2 décimales, ≤ 17 chiffres entiers, mise à l'échelle 2, sinon `ErreurVirement(MONTANT_INVALIDE)`). Texte JSON/audit = `toPlainString()` (`"100.00"`).
- `CompteId` : enveloppe l'identifiant numérique (`long`) échangé entre services ; n'expose jamais le numéro de compte. `Motif` : porte les règles du libellé (absent/blanc → pas de motif retenu, longueur max 100 = `LONGUEUR_MAX_MOTIF`, sinon `MOTIF_TROP_LONG`).
- Ordre des contrôles de saisie inchangé (FR-003-07) : comptes obligatoires, comptes distincts, montant, motif.
- Fabriques de `Virement` (`executer`, `suspendre`, `refuser`, `verifierComptes`, saisie) : au plus 4 paramètres, via un objet de demande (ex. `Soumission(id, clientId, demande, soumisLe)` où `demande` porte `CompteId` source/destinataire, `Montant`, `Motif`). Le constructeur canonique de `Virement` (réhydratation JDBC) n'est pas une fabrique.
- Types adoptés au minimum sur : `Virement` (montant, comptes, motif), `Compte.solde`, `Operation.montant/soldeApres`, ports `Comptes.debiter/crediter`, `VirementATraiter`. Extension aux autres usages de `BigDecimal` (contrôle de fraude, lignes de lecture) permise seulement si elle ne change aucun comportement.
- Frontières inchangées : JSON, colonnes `numeric(19,2)`, événements Kafka (champs numériques/chaînes identiques), `Commande`/`Resultat` du port entrant ; la conversion vers/depuis les primitives se fait dans `adapter.*` ou en entrée du cas d'usage.
- Rôle `ADMIN` : un seul point d'appel par service — `DecisionAdministrateur.exigerAdmin` (coeur-bancaire) et `ChangerEtatClientCasUsage.changer` (identite-clients) ; un test ArchUnit par service échoue si une autre classe accède à `Role.ADMIN`. Aucune bibliothèque partagée entre services (AD-17).
- Code en français, `domain`/`application` en Java pur, aucun `now()` ; couverture 100 % lignes/branches sur `domain` et `application`, 80 % ailleurs, seuils inchangés.

**Never:**
- Modifier `scripts/gates/**`, `.bmad-loop/**`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`, `e2e/golden-master/**`, `docs/retro/golden-master/derived/**`, `replay-mapping.json`, `sprint-status.yaml`, ni les `HexagonalArchitectureTest`/`HorlogeArchitectureTest`/`ClientsHttpArchitectureTest` existants.
- Abaisser un seuil, ajouter une exclusion de couverture ou de mutation, désactiver ou supprimer un test (les tests existants sont adaptés aux nouvelles signatures, mêmes cas).
- Rejouer le golden master ; toucher au front, à une migration SQL, à un texte, à un `data-testid` ; créer un module partagé.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Saisie valide | `"100.5"`, comptes 1→2, motif `"Loyer"` | `Montant` 100.50, `CompteId` 1 et 2, `Motif` « Loyer » | — |
| Montant illisible/≤0/3 décimales/>17 chiffres | `"abc"`, `"0"`, `"-1"`, `"1.234"` | refus | `ErreurVirement(MONTANT_INVALIDE)` |
| `Montant` hors règle | échelle ≠ 2 ou négatif ou `null` | construction refusée | `IllegalArgumentException` |
| Comptes absents / identiques | `null` ou même identifiant | refus avant lecture du montant | `COMPTES_OBLIGATOIRES` / `COMPTES_IDENTIQUES` |
| Motif blanc / 101 caractères | `"  "` / 101 car. | pas de motif retenu / refus | — / `MOTIF_TROP_LONG` |
| Rôle non ADMIN | rôles sans `ADMIN` | refus inchangé | `ROLE_INSUFFISANT` / `AccesRefuse` |

</intent-contract>

## Code Map

- `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/domain/Virement.java` -- record à 11 composants ; `verifierSaisie`, `verifierComptes`, `executer` (8 param.), `suspendre` (9), `refuser` (8), `naitre` (11), `lireMontant` (à déplacer dans `Montant.lire`) ; `LONGUEUR_MAX_MOTIF` ; `executerApresLiberation`, `operations`.
- `domain/Compte.java`, `domain/Operation.java`, `domain/VirementATraiter.java` -- portent `BigDecimal` / `long` ; `domain/Iban.java` -- patron d'objet-valeur existant (masque dans `toString`), modèle de style.
- `application/ExecuterVirementCasUsage.java` -- appelle les fabriques, `comptes.debiter/crediter(long, BigDecimal)`, `montant.toPlainString()` pour audit/notifications ; `application/port/in/ExecuterVirement.java` (`Commande`/`Resultat` restent en primitives) ; `application/port/out/Comptes.java`.
- `application/TraiterVirementATraiterCasUsage.java`, `LiberationVirementCasUsage.java`, `RejetVirementCasUsage.java`, `EcheanceRevueCasUsage.java`, `ConsulterTableauDeBordCasUsage.java` -- consommateurs de `Virement`/`Compte`/`Operation`.
- `application/DecisionAdministrateur.java` -- point unique `exigerAdmin` (utilisé par `ConsulterRevueCasUsage`, `LiberationVirementCasUsage`, `RejetVirementCasUsage`) ; seule référence à `Role.ADMIN` côté coeur-bancaire.
- `adapter/out/persistance/{DepotVirementsJdbc,ComptesJdbc,OperationsJdbc,HistoriqueVirementsJdbc}.java` -- `getBigDecimal`/`getLong` à envelopper ; `adapter/in/PointEntreeVirements.java` -- `toPlainString()` vers JSON ; `adapter/out/evenements/Enveloppe*.java`.
- `services/identite-clients/.../application/ChangerEtatClientCasUsage.java:54` -- seul contrôle `Role.ADMIN` du service (`changer`, partagé par `desactiver`/`activer`) ; `adapter/in/http/PointEntreeClients.java` ne fait qu'extraire les rôles.
- Tests : `coeur-bancaire/src/test/.../{domain/VirementTest,RevueVirementTest,ModelesDuDomaineTest,application/MemoireCoeurBancaire,ExecuterVirementCasUsageTest,adapter/*IT}` ; modèle de test ArchUnit : `coeur-bancaire/src/test/java/fr/nextbanking/coeurbancaire/HorlogeArchitectureTest.java` et `identite-clients/.../HorlogeArchitectureTest.java`.
- Lecture seule : `docs/quality-gates.md`, `scripts/gates/**`.

## Tasks & Acceptance

**Execution:**
- `coeur-bancaire/.../domain/{Montant,CompteId,Motif}.java` (+ tests dédiés, 100 % lignes/branches) -- créer les objets-valeurs, `Montant.lire`, opérations utiles (`ajouter`, `soustraire`, comparaison, `texte`) -- règles portées une fois.
- `coeur-bancaire/.../domain/` objet de demande (`DemandeVirement`/`Soumission`) -- remplacer les listes de primitives ; `Virement` : fabriques ≤ 4 paramètres, plus de `lireMontant`.
- `coeur-bancaire/.../domain/{Compte,Operation,VirementATraiter}.java`, `application/**`, `adapter/out/persistance/**`, `adapter/in/PointEntreeVirements.java` -- adopter les types, convertir aux frontières, comportement et sorties inchangés.
- tests existants de `coeur-bancaire` -- adaptés aux nouvelles signatures, mêmes cas conservés.
- `coeur-bancaire/src/test/java/fr/nextbanking/coeurbancaire/RoleAdminArchitectureTest.java` et `identite-clients/src/test/java/fr/nextbanking/identiteclients/RoleAdminArchitectureTest.java` -- ArchUnit : seule la classe point unique accède à `Role.ADMIN` ; règle prouvée sur un exemple fautif si le patron des tests existants le fait.

**Acceptance Criteria:**
- Given `Montant`, when je le construis avec une échelle ≠ 2, une valeur négative ou `null`, then la construction est refusée ; and `Montant.lire("100")` rend 100.00 dont le texte est `"100.00"`.
- Given `CompteId` et `Motif`, when je lis leurs sources, then ni l'un ni l'autre ne référence le numéro de compte et `Motif` applique la limite de 100 caractères.
- Given les fabriques de `Virement`, when je lis leurs signatures, then aucune ne prend plus de 4 paramètres et aucune ne reçoit de `long`/`BigDecimal`/`String` de montant, compte ou motif en liste.
- Given le dépôt, when je cherche `Role.ADMIN` dans les sources principales de `coeur-bancaire` et `identite-clients`, then une seule classe par service y accède, et un test ArchUnit le garantit.
- Given la story terminée, when `mvn -B -q clean verify -DfailIfNoTests=true`, then code 0, seuils 100 % sur `domain`/`application` tenus sans exclusion.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh`, then les quatre sortent en code 0 et `git diff --name-only` sur les chemins interdits est vide.

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 3, false 1, maybe-false 0
- findings:
  - `[low]` `reject` Règle ArchUnit du rôle plus étroite que « appelé depuis un point unique » — les tests de cas d'usage existants (revue, libération, rejet, changement d'état client) vérifient chacun `ROLE_INSUFFISANT`/`AccesRefuse` ; le garde-fou ajouté empêche une recopie de `Role.ADMIN`, une garde plus large ajouterait de la complexité sans préjudice nommé.
  - `[low]` `reject` Pas d'exemple fautif prouvant que la règle peut échouer — les `*ArchitectureTest` existants n'en ont pas non plus ; la spec ne l'impose que « si le patron le fait ».
  - `[low]` `reject` Critère « 4 paramètres au plus » sans contrôle automatique — vérifié à la lecture (`executer` 4, `suspendre` 3, `refuser` 2, `verifierComptes` 4) ; un test de réflexion serait disproportionné.
  - `[false]` `reject` Types confinés à `coeur-bancaire` — la story (AC et spec) vise le domaine de `coeur-bancaire` ; `identite-clients` ne porte ni montant ni virement.

## Auto Run Result

Status: done

- Résumé : `Montant` (échelle 2, jamais négatif, `lire` reprend l'ancienne lecture), `CompteId`, `Motif`, `DemandeVirement` et `Soumission` ajoutés au domaine de `coeur-bancaire` ; `Virement` les porte et ses fabriques `executer`/`suspendre`/`refuser`/`verifierComptes` ont au plus 4 paramètres ; `Compte.solde`, `Operation`, `VirementATraiter`, `Comptes.debiter/crediter` adoptent `Montant`. JSON, colonnes, événements et textes inchangés. Le contrôle `ADMIN` était déjà à un point unique par service (`DecisionAdministrateur.exigerAdmin`, `ChangerEtatClientCasUsage.changer`) : épinglé par un test ArchUnit `RoleAdminArchitectureTest` par service.
- Fichiers : 51 sous `services/coeur-bancaire` (domain, application, adapters JDBC/HTTP/événements, tests adaptés, `MontantTest`, `MotifTest`, `CompteIdTest`, `RoleAdminArchitectureTest`) et `services/identite-clients/.../RoleAdminArchitectureTest.java`.
- Revue : patches 0, reports 0, 4 constats rejetés (raisons ci-dessus). Relecture de suivi recommandée : false.
- Vérification : `scripts/gates/build.sh` (mvn clean verify, JaCoCo tenu), `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` = 0 ; diff des chemins interdits vide.
- Risques résiduels : le contrôle de fraude garde `BigDecimal` (hors périmètre minimal) ; `Montant.soustraire` lève `IllegalArgumentException` si le résultat est négatif, cas écarté en amont par le contrôle de solde.

## Design Notes

Esquisse (à ajuster, tant que les contraintes ci-dessus tiennent) :

```java
public record Montant(BigDecimal valeur) {  // constructeur compact : échelle 2, non négatif
    public static Montant lire(String texte) { /* ex-lireMontant, > 0 */ }
}
public record DemandeVirement(CompteId source, CompteId destinataire, Montant montant, Motif motif) {
    public static DemandeVirement lire(Long source, Long dest, String montant, String motif) { /* ordre FR-003-07 */ }
}
public record Soumission(long id, long clientId, DemandeVirement demande, Instant soumisLe) {}
// Virement.executer(Soumission, Compte source, Compte dest, SeuilsFraude) ; suspendre(Soumission, SeuilsFraude, String motifSuspension) ; refuser(Soumission, SeuilsFraude)
```

Le rôle : rien à déplacer — les deux services appellent déjà le contrôle depuis un unique point ; la story l'épingle par test pour qu'un cas d'usage futur ne le recopie pas.

## Verification

**Commands:**
- `cd services && mvn -B -q clean verify -DfailIfNoTests=true` (ou la commande de `scripts/gates/build.sh`) -- expected: code 0, seuils JaCoCo tenus
- `scripts/gates/build.sh` -- expected: code 0
- `scripts/gates/check-gates-exist.sh` -- expected: code 0
- `scripts/gates/golden-master.sh` -- expected: code 0
- `scripts/gates/trace-smoke.sh` -- expected: code 0
- `git diff --name-only -- scripts/gates .bmad-loop docs/quality-gates.md e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` -- expected: aucune ligne
