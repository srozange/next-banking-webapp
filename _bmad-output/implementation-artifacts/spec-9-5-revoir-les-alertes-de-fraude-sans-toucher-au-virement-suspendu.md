---
title: 'Revoir les alertes de fraude sans toucher au virement suspendu'
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: '28f15305067a5c75cb346f4331f2f93e2ccc9f0f'
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

**Problem:** l'ADMIN ne voit aucune alerte de fraude sur `/administration` et ne peut ni confirmer une alerte ni la classer en faux positif ; aucun point du cœur bancaire n'expose ni ne fait évoluer les alertes `alertes_fraude` (le legacy ne listait que les `OPEN`, BUG-056).

**Approach:** dans `coeur-bancaire`, un `GET /api/coeur-bancaire/administration/alertes-fraude` (toutes les alertes, plus récentes d'abord, nom du client et nom du relecteur lus dans la projection locale `clients`) et deux `POST …/alertes-fraude/{alertId}/{confirmation|faux-positif}` (transition conditionnelle depuis `OPEN`, auteur = identifiant de l'ADMIN, heure du port Horloge, aucun effet sur le virement) ; côté front, une section « Alertes Fraude — Moteur de détection » placée après la revue des virements suspendus.

## Boundaries & Constraints

**Always:**
- Rôle ADMIN par le point unique `DecisionAdministrateur.exigerAdmin` ; CONSEILLER/CLIENT → 403 `{"message":"Accès refusé"}` (texte de `ErreurVirement.ROLE_INSUFFISANT`), sans jeton → 401 ; aucun paramètre de requête lu. Rôle vérifié avant tout autre traitement.
- Forme JSON du `GET` (camelCase) : `[{alerteId, clientId, nomClient, regle, score, statut, description, creeLe, revuPar, nomRelecteur}]`, triée `cree_le desc, id desc` ; `nomClient` = `clients.nom_complet` (null si absent de la projection) ; `revuPar` = valeur brute de `revu_par` (identifiant, null si jamais revue) ; `nomRelecteur` = `nom_complet` de la projection pour cet identifiant (null sinon) ; `creeLe` ISO-8601 UTC ; `regle` et `statut` = codes métier. Toutes les alertes, tous statuts (BUG-056).
- Revue : `UPDATE alertes_fraude SET statut = :issue, revu_par = :acteur, revu_le = :maintenant WHERE id = :id AND statut = 'OPEN'` ; `revu_par` = `Long.toString(acteurId)` (même convention que `passerEnRevue`), `revu_le` = `Horloge.instant()`. Zéro ligne (statut déjà `CONFIRMED`/`FALSE_POSITIVE`/`REVIEWED`, ou alerte inconnue) → aucun effet, sans erreur. Cas d'usage appelé avec un identifiant nul → ignoré sans erreur, aucun appel au port. Les deux `POST` rendent 204 sans corps dans tous ces cas.
- Aucune écriture hors `alertes_fraude` pendant une revue : ni virement, ni compte, ni opération, ni outbox, ni audit, ni notification. À la libération ultérieure du virement (code existant de `LiberationVirementCasUsage`, inchangé), seules les alertes encore `OPEN` passent à `REVIEWED`.
- Front : lecture des alertes ajoutée au `Promise.all` de `chargerAdministration` (cinquième partie, même règle 401/erreur par partie). Section après `SectionRevueVirements` : titre `<h2>` « Alertes Fraude — Moteur de détection », badge « {n} ouverte(s) » (`Etiquette`, `data-testid="admin-alertes-badge"`) seulement si n > 0 ; `Tableau` colonnes « ID », « Client », « Règle déclenchée », « Score risque », « Statut », « Description », « Date », « Actions » ; Client = `nomClient` sinon « Client #{clientId} (introuvable) » ; Score « {score}/100 » ; Statut en `Etiquette` (code tel quel) ; Date `dd/MM/yyyy HH:mm` Europe/Paris via `formaterDate` + `formaterHeure` de `domain/virement.ts` ; Actions : pour `OPEN`, `BoutonFantome` « Confirmer » (`admin-alerte-confirmer-{alertId}`) et « Faux positif » (`admin-alerte-faux-positif-{alertId}`), sinon « Traité par {nomRelecteur ?? revuPar ?? ''} » ; vide → ligne unique « Aucune alerte fraude enregistrée », en-têtes gardés.
- Action sans confirmation : bouton `aria-disabled` pendant la requête (garde synchrone contre le double clic, motif `SectionGestionClients`), puis relecture des alertes ; 401 → fin de session et retour à la connexion ; autre échec → `message` reçu (sinon `TEXTES.erreurTechnique`) dans `AlerteErreur testId="admin-message-erreur"` de la section, sans toucher aux autres sections.
- Textes legacy dans `TEXTES.administration.alertes` (JSDoc citant FEAT-016 `MESSAGES.md` MSG-016-090 à 099b). Tests JUnit tagués `FR-016-14`, `FR-016-15`, `FR-016-16`, `BUG-056`, `FR-027-12`, `FR-027-13`, `FR-027-14` ; Vitest titrés `[FR-016-14]`, `[FR-016-16]`, `[BUG-056]`, `[FR-027-12]`. Hexagone et couverture (100 % domain/application, ≥ 80 % adaptateurs).

**Never:**
- Ne pas modifier `LiberationVirementCasUsage`, `RejetVirementCasUsage`, `AlertesFraudeJdbc.passerEnRevue`, ni la machine d'états du virement ; pas de nouvelle lecture de `Role.ADMIN` (`RoleAdminArchitectureTest` inchangé) ; pas de migration Flyway (le schéma V3 suffit).
- Pas d'appel HTTP vers identite-clients ; pas de texte inventé (aucun message de succès ni de refus pour la revue d'une alerte, pas de bouton « Rafraichir », non exigé) ; pas de modale.
- Ne pas toucher `scripts/gates/**`, `docs/quality-gates.md`, `sprint-status.yaml`, `frontend/.dependency-cruiser.cjs`, le golden master (jamais rejoué) ; ne retirer aucune assertion des tests existants.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Liste BUG-056 | alertes OPEN (score 45), CONFIRMED et FALSE_POSITIVE revues par l'ADMIN 10 (« Alice Martin » en projection) | 3 lignes, plus récente d'abord ; « 45/100 » ; 2 lignes « Traité par Alice Martin » ; badge « 1 ouverte(s) » | — |
| Aucune OPEN | seulement des alertes traitées | aucun badge | — |
| Aucune alerte | table vide | en-têtes + « Aucune alerte fraude enregistrée » ; `GET` → `[]` | — |
| Confirmer | alerte OPEN, ADMIN 10, Horloge T | `statut=CONFIRMED`, `revu_par='10'`, `revu_le=T` ; 204 ; ligne relue « Traité par Alice Martin » | — |
| Faux positif | idem | `FALSE_POSITIVE`, même auteur et heure | — |
| Virement suspendu | alerte OPEN d'un virement `SUSPENDU` confirmée | virement `SUSPENDU`, soldes, opérations, outbox et audit inchangés ; libération ensuite → alerte reste `CONFIRMED`, une autre alerte OPEN du virement → `REVIEWED` | — |
| Déjà traitée | alerte `REVIEWED` (ou `CONFIRMED`, `FALSE_POSITIVE`) | 204, statut, `revu_par`, `revu_le` inchangés | — |
| Concurrence | « Confirmer » et « Faux positif » simultanés sur une OPEN | une seule mise à jour appliquée, statut final = l'une des deux issues | — |
| Sans identifiant | cas d'usage avec `alerteId` nul | rien, aucune exception | — |
| Client hors projection | `client_id` 99 absent de `clients` | « Client #99 (introuvable) » | — |
| Non-ADMIN | jeton CONSEILLER ou CLIENT sur les 3 points | 403 `{"message":"Accès refusé"}`, aucune alerte modifiée | sans jeton : 401 |
| Panne de la lecture | 500 `{"message":"X"}` | « X » dans la section alertes ; autres sections affichées | corps illisible → erreur technique |

</intent-contract>

## Code Map

**coeur-bancaire** (`services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/`)
- `resources/db/migration/V3__alertes_fraude_et_beneficiaires.sql` -- table `alertes_fraude` (`id, client_id, virement_id` nullable, `regle, score, description, statut, cree_le, revu_le, revu_par varchar(100)`) ; lecture seule.
- `domain/StatutAlerte.java`, `domain/TypeSignal.java`, `domain/AlerteFraude.java` -- statuts et règles existants ; `AlerteFraude` sert à l'écriture (virementId `long`) : créer un record de lecture distinct (ex. `domain/AlerteAdministree.java`).
- `application/port/out/AlertesFraude.java` + `adapter/out/persistance/AlertesFraudeJdbc.java` -- ajouter `toutes()` (jointures `left join clients c on c.client_id = a.client_id` et `left join clients r on r.client_id::text = a.revu_par`) et `revoir(long alerteId, StatutAlerte issue, String revuPar, Instant revueLe)` → booléen (1 ligne mise à jour). Ne pas toucher `enregistrer` ni `passerEnRevue`.
- `application/DecisionAdministrateur.java` -- `exigerAdmin(Set<Role>)` : à appeler, seul lecteur de `Role.ADMIN`.
- `application/ConsulterRevueCasUsage.java` + `port/in/ConsulterRevue.java` -- modèle d'un cas d'usage ADMIN ; nouveau port in `port/in/RevoirAlertesFraude.java` (`List<AlerteAdministree> toutes(Set<Role>)`, `boolean revoir(Long alerteId, StatutAlerte issue, long acteurId, Set<Role> roles)`, issue limitée à `CONFIRMED`/`FALSE_POSITIVE`) et `application/RevoirAlertesFraudeCasUsage.java` (Horloge injectée).
- `adapter/in/PointEntreeRevue.java` + `adapter/in/JetonRoles.java` -- modèle contrôleur (`JetonRoles.depuis`, `Long.parseLong(jeton.getSubject())`) ; nouveau `adapter/in/PointEntreeAlertesFraude.java` (GET + 2 POST → 204 `ResponseEntity.noContent()`).
- `adapter/in/GestionErreursVirements.java:144` -- `ROLE_INSUFFISANT` → 403 avec `{"message":"Accès refusé"}` déjà en place.
- `infrastructure/AssemblageVirement.java` -- câblage `@Bean` des cas d'usage (ajouter le nouveau, ou une petite classe `AssemblageAlertesFraude`).
- `reference-data/projection-clients.sql` -- l'ADMIN 10 « Alice Martin » est dans la projection : le nom du relecteur se lit localement.
- Tests modèles (`src/test/java/fr/nextbanking/coeurbancaire/`) : `application/ConsulterRevueCasUsageTest.java`, `application/MemoireCoeurBancaire.java` (faux ports en mémoire, à compléter pour `toutes`/`revoir`), `adapter/RevueVirementIT.java:155-180` (virement suspendu + `insert into alertes_fraude` + libération), `adapter/BeneficiaireTransitionConcurrenteTest.java` (modèle de `AlerteFraudeTransitionConcurrenteTest` : `@SpringBootTest`, `Conteneurs`, `CountDownLatch`), `adapter/CoeurBancaireSecuriteIT.java` (liste `CHEMINS` 401, cas 403), `adapter/Jetons.java`, `adapter/DonneesDeReference.java`, `RoleAdminArchitectureTest.java` (ne pas élargir).

**frontend** (`frontend/src/`)
- `application/consulterAdministration.ts` -- port `ClientAdministration` : ajouter `lireAlertesFraude()` et `revoirAlerte(alerteId, issue: 'confirmation' | 'faux-positif')` ; `ResultatAdministration` reçoit `alertes` ; nouveau cas d'usage `revoirAlerteFraude` sur le motif de `changerEtatClient` (session, 401, message).
- `adapters/http/ClientAdministrationHttp.ts` -- chemins exportés `CHEMIN_ADMIN_ALERTES_FRAUDE`, `cheminRevueAlerte(id, issue)` ; convertisseur gardé (`entier`, `texte`, `texteOuNul`) ; `POST` sans corps, tout 2xx = succès (comme `changerEtatClient`).
- `domain/administration.ts` -- types `AlerteFraudeAdministree`, `compterAlertesOuvertes`, libellé du client (`nomClient` ou repli), libellé « Traité par », score « {n}/100 » ; `domain/virement.ts:88-98` `formaterDate`/`formaterHeure` à réutiliser.
- `adapters/ecrans/administration/EcranAdministration.tsx` -- ajouter `<SectionAlertesFraude …/>` après `<SectionRevueVirements/>` ; `SectionGestionClients.tsx` -- modèle de section (prop `partie`, `client`, `session`, `surDeconnexion`, garde `demandes`, `BoutonFantome`, `AlerteErreur`).
- `adapters/textes.ts:161` (`administration`) -- ajouter `alertes` : titre, `ouvertes(n)`, vide, colonnes, `confirmer`, `fauxPositif`, `traitePar(nom)`, `clientIntrouvable(id)`.
- `adapters/test/rendreApplication.tsx`, `infrastructure/main.test.ts`, `adapters/accessibilite.test.tsx`, `ecrans/administration/EcranAdministration.test.tsx:329-337` (test d'ordre des sections : ajouter « Alertes Fraude — Moteur de détection » après la revue) -- faux client d'administration à compléter (défaut : liste vide).

## Tasks & Acceptance

**Execution:**
- `services/coeur-bancaire/.../domain/AlerteAdministree.java` -- record de lecture (alerteId, clientId, nomClient, regle `TypeSignal`, score, statut, description, creeLe, revuPar, nomRelecteur) -- donnée de la liste.
- `services/coeur-bancaire/.../application/port/out/AlertesFraude.java`, `adapter/out/persistance/AlertesFraudeJdbc.java` -- `toutes()` et `revoir(...)` conditionnel -- lecture complète (BUG-056) et transition `WHERE statut = 'OPEN'`.
- `services/coeur-bancaire/.../application/port/in/RevoirAlertesFraude.java`, `application/RevoirAlertesFraudeCasUsage.java` -- `exigerAdmin`, id nul ignoré, issue hors `CONFIRMED`/`FALSE_POSITIVE` refusée par `IllegalArgumentException`, horodatage Horloge -- logique en Java pur.
- `services/coeur-bancaire/.../adapter/in/PointEntreeAlertesFraude.java` + câblage `infrastructure` -- trois routes.
- Tests coeur-bancaire : `RevoirAlertesFraudeCasUsageTest` (refus non-ADMIN sans appel, id nul, issue, auteur/heure, tri délégué) ; `AlerteFraudeAdministrationIT` (I/O matrix : liste exacte tri/noms/BUG-056, confirmation, faux positif, déjà traitée inchangée, virement `SUSPENDU` sans aucun effet — comptages de `virements`, `operations`, `outbox`, audit et soldes avant/après —, libération ensuite : `CONFIRMED` gardé, autre OPEN → `REVIEWED`), tagué `FR-016-14 FR-016-15 FR-016-16 BUG-056 FR-027-12 FR-027-13 FR-027-14` selon les cas ; `AlerteFraudeTransitionConcurrenteTest` (plusieurs manches, une seule ligne mise à jour, tag `FR-016-15`) ; `CoeurBancaireSecuriteIT` : GET alertes dans `CHEMINS` 401, cas 403 CONSEILLER/CLIENT des trois chemins avec statut des alertes inchangé.
- `frontend/src/domain/administration.ts` (+ test) -- types et règles d'affichage.
- `frontend/src/application/consulterAdministration.ts` (+ test) -- cinquième lecture, `revoirAlerteFraude`.
- `frontend/src/adapters/http/ClientAdministrationHttp.ts` (+ test) -- lecture et revue, gardes, 401, message, réseau coupé.
- `frontend/src/adapters/ecrans/administration/SectionAlertesFraude.tsx` (+ `SectionAlertesFraude.test.tsx`) et `EcranAdministration.tsx` -- section, branchement, test d'ordre complété.
- `frontend/src/adapters/{textes.ts,test/rendreApplication.tsx,accessibilite.test.tsx}`, `frontend/src/infrastructure/main.test.ts` -- textes, faux client, cas axe avec alertes.
- Violations volontaires : (1) `toutes()` filtrée `where statut = 'OPEN'` → `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='BUG-056'` non nul ; (2) la confirmation passe aussi le virement lié à `LIBERE` → `-Dgroups='FR-027-13'` non nul ; vérifier chacune puis restaurer à l'identique.

**Acceptance Criteria:**
- Given l'ADMIN sur `/administration` avec les trois alertes de référence, when la page se charge, then la section « Alertes Fraude — Moteur de détection » suit « Virements suspendus — En attente de revue », affiche les trois lignes plus récente d'abord, « 45/100 », deux « Traité par {nom complet de l'ADMIN} » et le badge « 1 ouverte(s) ».
- Given une alerte OPEN affichée, when l'ADMIN clique « Confirmer » (ou « Faux positif »), then le bouton est `aria-disabled` pendant la requête, puis la ligne relue affiche « Traité par {nom} » sans autre message.
- Given `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-016-14 | FR-016-15 | FR-016-16 | BUG-056 | FR-027-12 | FR-027-13 | FR-027-14'`, then code 0.
- Given `cd frontend && npx vitest run -t 'FR-016-1'`, `-t 'FR-027-1'` et `-t 'BUG-056'`, then chaque commande sort en code 0.
- Given chacune des deux violations volontaires, when la commande `-Dgroups` correspondante est lancée, then code non nul.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- Couches lancées : alignement sur l'intention (seule couche configurée) ; couches cas limites et trous de vérification non configurées, sautées.
- verdicts: 5 findings — high 0, medium 0, low 0, false 5, maybe-false 0
- findings:
  - `[false]` `[reject]` Lecture « écran » (R3) non testée : aucun test front ne revoit une alerte puis vérifie que le virement reste dans « Virements suspendus » — `SectionAlertesFraude` n'appelle jamais `clientRevue` ni ne touche à `SectionRevueVirements` (client et état séparés, aucune relecture croisée) ; l'état en base est prouvé inchangé par `AlerteFraudeAdministrationIT.confirmer_l_alerte_d_un_virement_suspendu_…` (instantané virement, soldes, opérations, outbox, audit) puis libération réelle : le virement ne peut pas disparaître de la file. Aucun mauvais résultat.
  - `[false]` `[reject]` Aucun test de l'application lancée (front réel contre coeur-bancaire réel) — rejouer le golden master avant l'epic 10 est interdit ; chemins (`CHEMIN_ADMIN_ALERTES_FRAUDE`, `cheminRevueAlerte`) et forme JSON (`AlerteJson` ↔ `alerteDepuis`) concordent à la lecture ; aucun écart constaté.
  - `[false]` `[reject]` Cas « sans identifiant » testé seulement au niveau du cas d'usage — c'est exactement la surface de l'AC (« le cas d'usage de revue appelé sans identifiant d'alerte ») ; le chemin HTTP porte un `long` obligatoire, un id nul n'y est pas atteignable.
  - `[false]` `[reject]` `testId="admin-message-erreur"` réutilisé par plusieurs sections — exigé par le contexte de l'epic et la spec ; les tests cherchent dans la section ; déjà connu (risque résiduel de 9.3).
  - `[false]` `[reject]` Spec absente du diff, sans `operator_actions` — la spec est versionnée à part ; aucune action humaine hors dépôt n'est requise par les AC, donc pas d'`awaiting-operator`.

## Design Notes

- Nom du relecteur : la projection `clients` du cœur bancaire contient tous les utilisateurs du legacy, ADMIN compris (`projection-clients.sql` : 10 « Alice Martin »), alimentée par `client-modifie` ; la jointure `r.client_id::text = a.revu_par` affiche le nom sans appel HTTP (A-016-4 : identifiant enregistré, nom affiché). Repli sur l'identifiant brut si le relecteur est absent de la projection : aucun texte inventé.
- 204 pour toute revue acceptée par le rôle, appliquée ou non : aucun texte legacy ni nouveau n'existe pour un refus de transition ; la relecture de la section montre l'état réel (statut et auteur gagnants).
- Date : le legacy affichait `dd/MM/yy HH:mm` (capture SCR-016-01) ; l'application affiche déjà les instants de l'administration en `dd/MM/yyyy HH:mm` (revue des virements) ; la section est hors de la fenêtre de 5 000 caractères et aucun scénario ne la compare. Choix de cohérence avec la section voisine.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml verify` -- code 0
- la commande `-Dgroups` des critères -- code 0 ; les deux violations -- code non nul puis restauration
- `cd frontend && npx vitest run --coverage` -- code 0, seuils tenus ; les trois `-t` des critères -- code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` -- code 0

## Auto Run Result

Status: done

**Résumé :** l'ADMIN voit sur `/administration`, après la revue des virements suspendus, la section « Alertes Fraude — Moteur de détection » : toutes les alertes, tous statuts (BUG-056 corrigé), la plus récente d'abord, score « {n}/100 », badge « {n} ouverte(s) » si n > 0, « Traité par {nom complet} » pour les alertes traitées. « Confirmer » / « Faux positif » (alertes OPEN) passent l'alerte à `CONFIRMED` / `FALSE_POSITIVE` par `UPDATE … WHERE statut = 'OPEN'`, auteur = identifiant de l'ADMIN, heure du port Horloge, sans aucun effet sur le virement suspendu ; la libération ultérieure garde le statut revu et ne passe que les alertes encore OPEN à `REVIEWED`.

**Fichiers :**
- `services/coeur-bancaire/.../domain/AlerteAdministree.java` — alerte vue par l'ADMIN (règle en code brut, noms de la projection).
- `services/coeur-bancaire/.../application/port/in/RevoirAlertesFraude.java`, `application/RevoirAlertesFraudeCasUsage.java` — ADMIN exigé par `DecisionAdministrateur`, id nul ignoré, issue limitée, Horloge.
- `services/coeur-bancaire/.../application/port/out/AlertesFraude.java`, `adapter/out/persistance/AlertesFraudeJdbc.java` — `toutes()` (jointures projection client et relecteur) et `revoir()` conditionnel.
- `services/coeur-bancaire/.../adapter/in/PointEntreeAlertesFraude.java`, `GestionErreursVirements.java` (route ajoutée au conseil d'erreurs, 403 au format `{"message"}`), `infrastructure/AssemblageAlertesFraude.java` — trois routes et câblage.
- Tests coeur-bancaire : `RevoirAlertesFraudeCasUsageTest`, `AlerteFraudeAdministrationIT`, `AlerteFraudeTransitionConcurrenteTest`, `CoeurBancaireSecuriteIT` (401/403), `MemoireCoeurBancaire` (faux port).
- `frontend/src/domain/administration.ts`, `application/consulterAdministration.ts`, `adapters/http/ClientAdministrationHttp.ts` — type, règles d'affichage, cinquième lecture parallèle, `revoirAlerteFraude`, chemins et gardes.
- `frontend/src/adapters/ecrans/administration/SectionAlertesFraude.tsx` (+ test), `EcranAdministration.tsx/.css` (+ test d'ordre) — section et branchement.
- `frontend/src/adapters/{textes.ts,test/rendreApplication.tsx,accessibilite.test.tsx}`, `infrastructure/main.test.ts` — textes legacy MSG-016-090 à 099b, faux client, cas axe.

**Écart assumé à la Code Map :** `AlerteAdministree.regle` est une chaîne et non `TypeSignal` : la table accepte `MULTIPLE_FAILED_AUTH`, absent de `TypeSignal`, et une seule alerte de ce type aurait fait échouer toute la liste ; le code est renvoyé tel quel (codes métier tels quels, conforme au contrat JSON).

**Revue :** 5 constats (couche alignement sur l'intention), 0 correctif, 0 report, 5 rejetés comme faux : virement intact à l'écran (sections et clients séparés, état en base prouvé) ; pas de test d'application lancée (rejeu interdit avant l'epic 10, contrats concordants) ; id nul testé à la surface de l'AC ; `admin-message-erreur` partagé voulu ; pas d'action opérateur.

**Revue complémentaire recommandée :** non (correctifs : high 0, medium 0, low 0).

**Vérification :**
- `scripts/gates/build.sh` : code 0 (build propre, couverture JaCoCo et Vitest tenues ; front 1 978 tests, lignes 99,02 %).
- `scripts/gates/check-gates-exist.sh` (aucun KO), `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` : code 0. Golden master non rejoué.
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-016-14 | FR-016-15 | FR-016-16 | BUG-056 | FR-027-12 | FR-027-13 | FR-027-14'` : code 0.
- `cd frontend && npx vitest run -t 'FR-016-1'` (120 tests), `-t 'FR-027-1'` (13), `-t 'BUG-056'` (3) : code 0.
- Violations volontaires rejouées pendant la vérification : liste filtrée `where a.statut = 'OPEN'` → `-Dgroups='BUG-056'` code 1 ; confirmation qui passe aussi le virement à `LIBERE` → `-Dgroups='FR-027-13'` code 1 (attendu `etat=SUSPENDU`) ; fichier restauré à l'identique (`cmp`).

**Risques résiduels :**
- Le nom du relecteur dépend de la projection locale `clients` : un ADMIN absent de la projection est affiché par son identifiant brut.
- Les `POST` de revue rendent 204 même quand la transition n'a pas eu lieu (alerte déjà traitée ou inconnue) : seule la relecture montre l'état réel, faute de texte legacy pour un refus.
- La date est affichée `dd/MM/yyyy HH:mm` (cohérence avec la revue des virements) au lieu du `dd/MM/yy HH:mm` du legacy ; hors fenêtre du golden master.
