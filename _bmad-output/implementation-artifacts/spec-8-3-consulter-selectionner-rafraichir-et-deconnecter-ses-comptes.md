---
title: 'Consulter, sélectionner, rafraîchir et déconnecter ses comptes externes côté service'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: '602f6f9988d20d26306374a726b0944576446d08'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-8-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-8-2-traiter-le-retour-public-de-powens.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** Le service `open-banking-conseiller` enregistre les comptes lus au retour de Powens (8.2), mais `GET .../powens` ne les rend pas et aucun point d'API ne permet de choisir un compte, de rafraîchir la liste ni de se déconnecter ; un solde absent n'est pas distingué de zéro (BUG-042) et une panne de Powens risque de passer pour une liste vide (BUG-043, BUG-089).

**Approach:** `GET .../powens` lit l'état en base (sans appel à Powens) et rend connexion, comptes, compte sélectionné, transactions, message et patrimoine total externe (`"120.50"`). Trois points authentifiés s'ajoutent : `POST .../powens/comptes/{compteId}/selection`, `POST .../powens/rafraichissement`, `DELETE .../powens/connexion` (204). Toute lecture Powens est faite AVANT d'écrire en base : en cas de panne, 503 et état inchangé.

## Boundaries & Constraints

**Always:** hexagone (domain/application Java pur, `RestClient` seulement dans `adapter.out.fournisseur`) ; au plus 20 transactions enregistrées ; libellé simplifié sinon d'origine (déjà fait par l'adaptateur) ; solde absent (`null`) distinct de `"0.00"` et exclu du patrimoine total ; montants en chaîne à 2 décimales ; compte absent des comptes externes du client (dont celui d'un autre client) : 404 `{"message": "Ressource introuvable"}` sans appel Powens et état inchangé ; panne Powens (404 transactions, 5xx) : 503 `{"message": "Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants."}`, jamais une liste vide ; sans JWT : 401 et états inchangés ; le jeton Powens ne sort jamais de l'API ni des journaux ; le rafraîchissement garde le compte déjà sélectionné s'il existe encore, sinon sélectionne le premier ; code, tests, commits en français.

**Never:** appel Powens depuis `GET .../powens` ; écran ou front (8.4, 8.5) ; modifier `scripts/gates/**`, `docs/quality-gates.md`, golden master, `sprint-status.yaml` ; appel réseau dans un test ; `mode=reel` committé.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Lecture de l'état | client connecté avec comptes | 200 : `connecte`, `comptes[{id,nom,type,solde,soldeAbsent,iban}]`, `compteSelectionne`, `transactions[{date,libelle,montant}]`, `message`, `patrimoineTotal`; aucun appel Powens | — |
| Soldes | A sans solde, B vide, C = 0, D = 120,50 | A,B `soldeAbsent=true`, `solde=null` ; C `"0.00"` ; D `"120.50"` ; total `"120.50"` | — |
| Sélection | compte du client | sélectionné ; ≤ 20 transactions lues chez Powens ; 200 avec transactions | — |
| Sélection inconnue | `compteId` absent / d'un autre client | 404 « Ressource introuvable », aucun appel Powens, état inchangé | — |
| Sélection en panne | transactions 404 puis 500 | 503 message d'indisponibilité, état inchangé | — |
| Rafraîchissement | connecté, Powens renvoie 2 comptes | comptes relus ; sélectionné conservé s'il existe, sinon le premier ; transactions rechargées ; message effacé ; 200 avec l'état | Powens 5xx : 503, état inchangé |
| Rafraîchissement sans connexion | pas de jeton | aucun appel Powens, 400 « Requête invalide » | — |
| Déconnexion | connecté | 204 ; plus de jeton, comptes, sélection, transactions, connexion ni message ; `GET` non connecté | — |
| Sans JWT | sélection, rafraîchissement, déconnexion | 401 « Authentification requise », états inchangés | — |

</intent-contract>

## Code Map

- `services/open-banking-conseiller/src/main/java/fr/nextbanking/openbankingconseiller/domain/` -- `CompteExterne` (solde `null` = absent), `TransactionExterne`, `EtatPowens` existants ; ajouter `SituationPowens` (état + comptes + sélection + transactions, `patrimoineTotal()`).
- `.../application/port/out/DepotEtatPowens.java` + `adapter/out/persistance/DepotEtatPowensJdbc.java` -- `lire`, `remettreAZero`, `enregistrerConnexion` à réutiliser.
- `.../application/TraiterRetourPowensCasUsage.java` -- `TRANSACTIONS_MAX`, `AUCUN_COMPTE`, motif de lecture comptes + transactions à réutiliser (même package).
- `.../application/ConsulterEtatPowensCasUsage.java` + port `in/ConsulterEtatPowens` -- passent de `EtatPowens` à `SituationPowens`.
- `.../adapter/in/http/{PointEntreePowens,GestionErreurs,Textes}.java` -- nouveaux points, 404/400 ; `PowensIndisponible` → 503 déjà géré.
- `.../infrastructure/AssemblagePowens.java` -- beans des nouveaux cas d'usage.
- Tests : `adapter/{PowensIT (JSON exact de l'état initial à ajuster), RetourPowensIT, ProduitsSecuriteIT, Jetons}`, `application/{ConsulterEtatPowensTest, TraiterRetourPowensTest}` (modèles), ArchUnit à garder verts.
- Lecture seule : `docs/retro/features/FEAT-012`, `FEAT-014`, `epic-8-context.md`.

## Tasks & Acceptance

**Execution:**
- `.../domain/SituationPowens.java` -- état agrégé et patrimoine total exclu des soldes absents -- 100 % couvert
- `.../application/port/out/DepotComptesExternes.java`, `adapter/out/persistance/DepotComptesExternesJdbc.java` -- lire comptes, sélection, transactions ; contrôler qu'un compte appartient au client ; enregistrer la sélection avec ses transactions
- `.../application/{ConsulterEtatPowensCasUsage,SelectionnerCompteExterneCasUsage,RafraichirComptesExternesCasUsage,DeconnecterPowensCasUsage}.java` + ports `in` + exceptions `CompteExterneIntrouvable`, `PowensNonConnecte` -- logique, lecture avant écriture
- `.../adapter/in/http/{PointEntreePowens,GestionErreurs}.java`, `infrastructure/AssemblagePowens.java` -- points d'API, statuts, assemblage
- Tests : unitaires des cas d'usage et de `SituationPowens` ; `ComptesExternesIT` (matrice complète, 401 sans JWT, isolement entre clients, aucun appel Powens à la lecture) ; `PowensIT` ajusté. Tags `FR-028-07..11`, `FR-012-10`, `FR-014-07`, `FR-014-08`, `BUG-042`, `BUG-043`, `BUG-089` (au moins un fichier chacun)

**Acceptance Criteria:**
- Given un client connecté, when `GET .../powens`, then 200 avec comptes, sélection, transactions, message et patrimoine total, sans appel Powens
- Given des comptes A (sans solde), B (vide), C (0), D (120,50), when ils sont lus, then A et B « solde absent », C `"0.00"`, D `"120.50"`, total `"120.50"`
- Given un compte du client, when sélection, then 200 avec ≤ 20 transactions ; Given un compte inconnu ou d'un autre client, then 404 sans appel Powens ; Given Powens 404 puis 500, then 503 et état inchangé
- Given un rafraîchissement, when Powens répond, then comptes relus, sélection conservée sinon premier ; when 5xx, then 503
- Given un client connecté, when `DELETE .../connexion`, then 204 et état remis à zéro
- Given aucun JWT, when sélection, rafraîchissement ou déconnexion, then 401 et états inchangés
- Given la story terminée, when `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-028-07,FR-028-08,FR-028-09,FR-028-10,FR-028-11,FR-012-10,FR-014-07,FR-014-08,BUG-042,BUG-043,BUG-089'`, then code 0, puis `scripts/gates/{build,check-gates-exist,golden-master,trace-smoke}.sh` en code 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 1, false 3, maybe-false 0
- findings:
  - `false` `reject` Cas non décrits par l'intention (rafraîchissement sans jeton → 400, `compteId` non numérique → 400) — l'intention ne les exclut pas et ne dit rien d'autre ; le choix est consigné dans les notes de conception et couvert par les tests.
  - `false` `reject` Contrat JSON de `GET .../powens` modifié (PowensIT ajusté) — c'est l'objet de la story (rendre comptes, sélection, transactions, total) ; l'ancienne forme était un gabarit vide annoncé pour 8.3.
  - `false` `reject` « État inchangé » sur 503 sans transaction englobant lecture et écriture — la lecture Powens précède toute écriture et une panne lève avant la première écriture ; `ComptesExternesIT` compare l'état en base avant et après.
  - `low` `reject` Le rafraîchissement n'exige que le jeton, pas `connecte` : un client en cours de connexion qui rafraîchit devient connecté — action volontaire de l'utilisateur sur son propre état, sans effet sur un autre client ; corriger ajouterait une branche sans bénéfice visible.

## Auto Run Result

Status: done

**Résumé :** `GET /api/open-banking-conseiller/powens` rend désormais, lus en base sans appel à Powens, la connexion, les comptes (solde `null` + `soldeAbsent` quand Powens n'en donne pas, `"0.00"` pour un solde nul), le compte sélectionné, ses transactions, le message et le patrimoine total externe (`"120.50"`, soldes absents exclus). Trois points authentifiés s'ajoutent : sélection d'un compte (404 si le compte n'est pas au client, sans appel Powens), rafraîchissement (sélection conservée, sinon premier compte) et déconnexion (204, remise à zéro). Toute lecture Powens précède l'écriture : une panne donne 503 et laisse l'état inchangé.

**Fichiers :** `SituationPowens` (domaine) ; `DepotComptesExternes`/`DepotComptesExternesJdbc` ; cas d'usage `SelectionnerCompteExterne`, `RafraichirComptesExternes`, `DeconnecterPowens` et leurs ports, `ConsulterEtatPowens` adapté ; exceptions `CompteExterneIntrouvable` (404) et `PowensNonConnecte` (400) ; `PointEntreePowens`, `GestionErreurs`, `AssemblagePowens` ; tests `SituationPowensTest`, `GestionComptesExternesTest`, `ComptesExternesIT`, `ConsulterEtatPowensTest`, `PowensIT` et `Jetons` ajustés.

**Revue :** 4 constats, 0 correctif, 0 différé ; 3 rejetés comme faux (cas non décrits, contrat JSON de l'état, état inchangé sur 503), 1 rejeté comme négligeable (rafraîchissement d'un client en cours de connexion).

**Recommandation de revue complémentaire :** false.

**Vérification :** `mvn -f services/open-banking-conseiller/pom.xml test -Dgroups=…` code 0 ; les 11 tags présents dans au moins un fichier ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` code 0.

**Risques résiduels :** les transactions sont rendues triées par date puis identifiant décroissants (la table ne conserve pas l'ordre d'origine de Powens) ; le rafraîchissement d'un client avec jeton mais pas encore revenu de Powens le marque connecté.

## Design Notes

Le rafraîchissement sans connexion (jeton absent) n'est pas décrit par l'epic : 400 « Requête invalide » (seul statut publié adapté), sans appel Powens. Après une sélection réussie le message en cours est conservé (seule la section des transactions change) ; le rafraîchissement le remplace (effacé, ou « aucun compte » si la liste est vide). Forme JSON de l'état : champs français camelCase, `solde` en chaîne ou `null` avec `soldeAbsent`.

## Verification

**Commands:**
- `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-028-07,FR-028-08,FR-028-09,FR-028-10,FR-028-11,FR-012-10,FR-014-07,FR-014-08,BUG-042,BUG-043,BUG-089'` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh && scripts/gates/trace-smoke.sh` -- expected: code 0
- `grep -rl '@Tag("<identifiant>")' services/open-banking-conseiller/src/test` pour chaque identifiant -- expected: au moins un fichier
