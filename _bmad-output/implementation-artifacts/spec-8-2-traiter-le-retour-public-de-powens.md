---
title: 'Traiter le retour public de Powens'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: '90f27e0a1a912d75f247bbaf24e004a88e83eea0'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-8-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-8-1-raccorder-powens-derriere-un-port-et-lancer-la-connexion-a-une-banque.md'
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** Au retour de la page web de Powens, le navigateur n'envoie pas le jeton JWT : le service `open-banking-conseiller` n'a aucun point d'entrée public pour traiter le résultat de la connexion (erreur, `connection_id`, `code` seul), ni pour lire les comptes et ramener l'utilisateur sur « Comptes Tiers ».

**Approach:** `GET /api/open-banking-conseiller/powens/retour` (chemin exact seul public) répond toujours 302 vers `/comptes-tiers`, sans corps. Le `state` (signature HMAC, expiration, nonce consommé une fois) retrouve le client ; seul un `state` valide a un effet : remise à zéro sur `error`, lecture immédiate des comptes sur `connection_id` (premier compte sélectionné, ≤ 20 transactions), message d'état sinon.

## Boundaries & Constraints

**Always:** Hexagone (domain/application Java pur, `RestClient` seulement dans `adapter.out.fournisseur`) ; temps par le port `Horloge` ; ordre de traitement `error` > `connection_id` > reste (`code` seul = incomplet) ; un paramètre vide compte comme absent ; remise à zéro (jeton, comptes, compte sélectionné, transactions, connecté, message) AVANT de poser le message d'erreur ; `Parametres recus` ne liste que les paramètres reçus non vides autres que `state`, dans l'ordre reçu, au format `[a, b]` ; une panne Powens n'est jamais une liste vide ; textes à l'identique (casse, sans accents) : « La connexion a votre banque n a pas abouti ({motif}). Veuillez recommencer la connexion. », « Session expiree. Veuillez recommencer la connexion. », « Callback Powens incomplet. Parametres recus: [...] », « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. », « Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants. » ; jeton Powens jamais dans une réponse ni un journal ; aucun appel Powens pour un `state` invalide ou un jeton absent ; code, tests, commits en français.

**Never:** rendre public un autre chemin que le chemin exact du retour (ni `/retour/x`, ni `/retour-x`, ni `/powens`) ; attente imposée avant la lecture des comptes ; appel réseau externe dans un test ; implémenter sélection, rafraîchissement, déconnexion, lecture des comptes par l'API ou le front (8.3, 8.4) ; modifier `scripts/gates/**`, `docs/quality-gates.md`, golden master, `sprint-status.yaml` ; `mode=reel` committé.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Sans paramètre, sans JWT | `GET .../retour` | 302 `Location: /comptes-tiers`, corps vide, aucun appel Powens | — |
| `state` absent / mal signé / expiré / déjà utilisé | `?connection_id=1&state=…` | 302, aucun effet, aucun appel Powens, état de chaque client inchangé | — |
| `error=user_cancelled` | `state` valide (± `connection_id`, `code`) | remise à zéro puis message d'échec contenant `user_cancelled` ; 302 ; `GET .../powens` non connecté + message | — |
| `connection_id`, nominal | `state` valide, `PowensSimule` nominal | comptes lus aussitôt, connecté, 1er compte sélectionné, ≤ 20 transactions (25 simulées → 20 en base) ; 302 | — |
| `connection_id`, aucun compte | `AUCUN_COMPTE` | connecté + message « Connexion reussie mais aucun compte… » | — |
| `connection_id`, Powens 503 puis 500 | `COMPTES_503`, `COMPTES_500` | reste non connecté, message d'indisponibilité, ni message « aucun compte » ni liste vide | — |
| `connection_id`, jeton effacé | `state` valide, jeton NULL en base | aucun appel Powens, message « Session expiree… » | — |
| `code` seul / aucun autre / vides | `?code=abc` ; rien ; `error=&connection_id=&code=` | message « Callback Powens incomplet. Parametres recus: [code] » / `[]` / `[]` ; non connecté ; 302 | — |
| Chemins voisins sans JWT | `/retour/x`, `/retour-x`, `/powens` | 401 « Authentification requise » | — |

</intent-contract>

## Code Map

- `services/open-banking-conseiller/src/main/java/fr/nextbanking/openbankingconseiller/adapter/in/http/ConfigurationSecurite.java` -- `SANTE` (`PathPatternRequestMatcher.pathPattern(GET, …)` L483) : ajouter le même matcher pour le retour en `permitAll`.
- `.../adapter/in/http/PointEntreePowens.java` -- ajouter `GET .../powens/retour` (récupère tous les paramètres, `state` compris) ; 302 + `Location`, sans corps.
- `.../application/` -- nouveau cas d'usage `TraiterRetourPowensCasUsage` + port `in` `TraiterRetourPowens` ; réutilise `Powens`, `EmetteurState` (`verifier`), `Horloge`, `DepotEtatPowens`. `ConnecterPowensCasUsage` : modèle de cas d'usage.
- `.../application/port/out/DepotEtatPowens.java` + `adapter/out/persistance/DepotEtatPowensJdbc.java` (modèle `JdbcClient`) -- ajouter `consommerState(state, maintenant)` (`update states_powens set utilise = true where nonce=? and sub=? and not utilise and expire_a > ?`, atomique), `remettreAZero(sub)`, `poserMessage(sub, message)` (upsert), `enregistrerConnexion(sub, comptes, selectionne, transactions, message)`.
- `.../resources/db/migration/V2__powens.sql` (`etat_powens`, `states_powens`) -- lecture seule ; créer `V3__comptes_externes.sql` : `comptes_externes (sub, compte_id, nom, type, solde numeric(19,2) null, iban)`, `transactions_externes (sub, compte_id, id, date, libelle, montant numeric(19,2))`, colonne `etat_powens.compte_selectionne`.
- `.../domain/{CompteExterne, TransactionExterne, EtatPowens, StatePowens}` -- existants ; `.../adapter/out/fournisseur/PowensSimule.java` -- ajouter un mode `TRANSACTIONS_25` (25 transactions) ; modes `COMPTES_503`, `COMPTES_500`, `AUCUN_COMPTE` déjà présents.
- `.../infrastructure/AssemblagePowens.java` -- déclarer le bean du nouveau cas d'usage.
- Tests : `src/test/.../adapter/{PowensIT (modèle : JDBC, `Jetons`, `Conteneurs`, `emetteur.emettre`), ProduitsSecuriteIT (401 + chemins voisins), Jetons}`, `adapter/in/http/ConfigurationSecuriteTest`, `application/ConnecterPowensTest` (modèle de test unitaire), `ArchUnit` tests à garder verts.
- Lecture seule : `docs/retro/features/FEAT-014/RG.md`, `docs/retro/golden-master/derived/FEAT-014/SC-014-01/expected-response.json` (302, corps null), `epic-8-context.md`.

## Tasks & Acceptance

**Execution:**
- `.../application/TraiterRetourPowensCasUsage.java`, `port/in/TraiterRetourPowens.java`, `port/out/DepotEtatPowens.java` -- logique du retour (ordre `error` > `connection_id` > incomplet, troncature à 20 transactions, messages) -- 100 % couvert
- `.../adapter/out/persistance/DepotEtatPowensJdbc.java`, `db/migration/V3__comptes_externes.sql` -- consommation atomique du `state`, remise à zéro, message, enregistrement de la connexion
- `.../adapter/out/fournisseur/PowensSimule.java` -- mode `TRANSACTIONS_25`
- `.../adapter/in/http/{ConfigurationSecurite,PointEntreePowens}.java`, `infrastructure/AssemblagePowens.java` -- chemin public exact et point d'entrée 302
- Tests : unitaires du cas d'usage (matrice complète, `DepotEtatPowens` factice) ; `RetourPowensIT` (302 sans corps, `state` invalide/expiré/rejoué, `error`, nominal + 25 transactions, aucun compte, 503/500, jeton effacé, `code` seul, vides, aucun appel Powens vérifié) ; `ProduitsSecuriteIT` étendu (chemins voisins 401) ; `ConfigurationSecuriteTest` ajusté. Tags `FR-014-01..08`, `FR-012-02`, `FR-012-06`, `FR-012-07`, `FR-012-08`, `FR-012-09`, `FR-028-03..06`, `BUG-040`, `BUG-044`, `BUG-089` (au moins un fichier chacun)

**Acceptance Criteria:**
- Given aucune session ni jeton, when `GET .../powens/retour` sans paramètre sans suivre la redirection, then 302, `Location` = `/comptes-tiers`, corps vide, aucun appel Powens
- Given un chemin voisin du retour sans jeton, when appelé, then 401 (seul le chemin exact est public)
- Given un `state` absent, mal signé, expiré ou déjà utilisé, when le retour est appelé, then 302 sans effet ni appel Powens
- Given un `state` valide, when `error=user_cancelled`, then remise à zéro puis message d'échec, `GET .../powens` non connecté avec « user_cancelled »
- Given un `state` valide, when `connection_id` (nominal / sans compte / 503 puis 500 / jeton effacé), then comportements de la matrice
- Given un `state` valide, when `code` seul ou paramètres vides, then message « Callback Powens incomplet. Parametres recus: [...] », non connecté
- Given la story terminée, when `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-014-01,FR-014-02,FR-014-03,FR-014-04,FR-014-05,FR-014-06,FR-014-07,FR-014-08,FR-012-02,FR-012-06,FR-012-07,FR-012-08,FR-012-09,FR-028-03,FR-028-04,FR-028-05,FR-028-06,BUG-040,BUG-044,BUG-089'`, then code 0, puis `scripts/gates/{build,check-gates-exist,golden-master,trace-smoke}.sh` en code 0

## Spec Change Log

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 1, false 3, maybe-false 0
- findings:
  - `false` `reject` Comptes et transactions non exposés à l'utilisateur — l'API de lecture des comptes est la story 8.3 et l'écran la 8.4 (epic 8, dépendances 8.2 → 8.3 → 8.4) ; le critère de 8.2 vise la réponse 302 et l'état persisté, tous deux testés.
  - `false` `reject` `Location` relatif non prouvé jusqu'au front — le critère impose littéralement un 302 vers `/comptes-tiers` (golden master SC-014-01 : 302, corps nul) ; la résolution par la passerelle relève de l'epic 2 / du front.
  - `low` `reject` `ConfigurationSecuriteTest` ne compare que deux constantes et `PowensSimule.nombreAppels()` est un point d'observation de test dans le code de production — le comportement public est couvert par `RetourPowensIT` et `ProduitsSecuriteIT` (401 sur `/retour/x`, `/retour-x`) ; corriger demanderait une refonte sans bénéfice pour l'utilisateur.
  - `false` `reject` « 503 puis 500 » testés dans une seule boucle — chaque tour utilise un `state` neuf et vérifie l'état, donc les deux lignes de la matrice sont couvertes et exécutées.

## Auto Run Result

Status: done

**Résumé :** `GET /api/open-banking-conseiller/powens/retour` est public sur son chemin exact seul et répond toujours 302 vers `/comptes-tiers`, sans corps. Le `state` signé (HMAC, expiration, nonce consommé atomiquement) retrouve le client ; sans `state` valide, aucun effet ni appel Powens. `error` remet l'état à zéro puis pose le message d'échec ; `connection_id` lit les comptes aussitôt (premier compte sélectionné, ≤ 20 transactions) ; le reste donne « Callback Powens incomplet… ». Aucun compte, panne Powens (503/500) et jeton absent ont leur message.

**Fichiers :** `TraiterRetourPowensCasUsage` et `TraiterRetourPowens` (application) ; `DepotEtatPowens`/`DepotEtatPowensJdbc` (consommation du `state`, remise à zéro, message, enregistrement de la connexion) ; `V3__comptes_externes.sql` ; `PowensSimule` (mode `TRANSACTIONS_25`, compteur d'appels) ; `PointEntreePowens`, `ConfigurationSecurite`, `AssemblagePowens` ; tests `TraiterRetourPowensTest`, `RetourPowensIT`, `ProduitsSecuriteIT`, `ConfigurationSecuriteTest` et deux tests existants adaptés.

**Revue :** 4 constats, 0 correctif, 0 différé ; 3 rejetés comme faux (comptes exposés en 8.3/8.4, `Location` relatif conforme au critère, boucle 503/500 couverte), 1 rejeté comme négligeable (test de constantes, compteur d'appels de test).

**Recommandation de revue complémentaire :** false.

**Vérification :** `mvn -f services/open-banking-conseiller/pom.xml test -Dgroups=…` code 0 ; les 20 tags présents dans au moins un fichier ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh`, `trace-smoke.sh` code 0.

**Risques résiduels :** une erreur d'infrastructure (base) pendant le retour n'est pas convertie en 302 ; si les transactions du premier compte échouent, la connexion est établie avec le message d'indisponibilité (choix consigné dans les notes de conception) ; la résolution de `/comptes-tiers` par la passerelle n'est pas testée ici.

## Design Notes

Le jeton d'accès JWT n'accompagne pas le retour : le client vient du `state` signé (`sub`), jamais d'un paramètre libre. Si `connection_id` est traité et que les comptes sont lus mais que les transactions du premier compte échouent (`PowensIndisponible`), la connexion est marquée établie, le compte sélectionné, sans transaction, avec le message d'indisponibilité (jamais présenté comme « aucune transaction »). `GET .../powens` ne rend pas encore comptes ni transactions (8.3) : les tests lisent la base.

## Verification

**Commands:**
- `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-014-01,FR-014-02,FR-014-03,FR-014-04,FR-014-05,FR-014-06,FR-014-07,FR-014-08,FR-012-02,FR-012-06,FR-012-07,FR-012-08,FR-012-09,FR-028-03,FR-028-04,FR-028-05,FR-028-06,BUG-040,BUG-044,BUG-089'` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh && scripts/gates/trace-smoke.sh` -- expected: code 0
- `grep -rl '@Tag("<identifiant>")' services/open-banking-conseiller/src/test` pour chaque identifiant -- expected: au moins un fichier
