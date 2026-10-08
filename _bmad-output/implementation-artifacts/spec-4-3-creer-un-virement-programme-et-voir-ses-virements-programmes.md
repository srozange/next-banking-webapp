---
title: 'Créer un virement programmé et voir ses virements programmés'
type: 'feature'
created: '2026-10-08'
status: 'done'
baseline_revision: '2cca3d17b761f6409161b4cec9ca490286586270'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-2-verifier-un-iban-et-ajouter-un-beneficiaire.md'
  - '{project-root}/docs/quality-gates.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** La route `/virements-programmes` n'est pas déclarée (elle retombe sur l'accueil) et le cœur bancaire n'a ni table, ni domaine, ni API pour les virements programmés : le client ne peut ni en créer un ni les lister.

**Approach:** Cœur bancaire : domaine `VirementProgramme` + énumération de fréquence legacy, table (migration V6), `POST` et `GET /api/coeur-bancaire/virements-programmes`, contrôle d'existence du compte destinataire dès la création (BUG-015). Front : écran `/virements-programmes` (formulaire « Nouveau virement programme » + tableau « Mes virements programmes »), sélecteur de comptes alimenté par le client de comptes existant.

## Boundaries & Constraints

**Always:** Client déduit du jeton (`sub`), jamais d'un paramètre. Hexagone respecté (domain/application sans Spring). Temps uniquement par `Horloge` (création, fuseau `Europe/Paris`) ; montant `BigDecimal` 2 décimales, `numeric(19,2)`, chaîne `"50.00"` en JSON, front en centimes entiers. Fréquences : `HEBDOMADAIRE`, `MENSUEL`, `BIMESTRIEL`, `TRIMESTRIEL`, `SEMESTRIEL`, `ANNUEL` (options du `<select>` dans cet ordre, `MENSUEL` présélectionné, valeurs = noms de l'énumération, AR-79). Ordre de validation (identique front et API) : compte source, numéro bénéficiaire, libellé, montant, date vide, format de date. « Montant invalide » est le message unique pour montant vide, nul, négatif ou non numérique. Longueurs : numéro bénéficiaire ≤ 34, libellé ≤ 100, motif ≤ 255 (dépassement → 400 `Requête invalide` sans enregistrement, `maxlength` à l'écran). Contrôle d'existence du compte destinataire par **numéro de compte** (colonne `comptes.numero`), après les contrôles de saisie, du compte source puis de son appartenance ; la prochaine exécution enregistrée = la date saisie à 00:00 `Europe/Paris` (passée acceptée, exécution livrée par la story 4.6). Enregistré : `actif=true`, `nb_executions=0`, `cree_le` lu sur `Horloge`. Textes à l'identique de `docs/retro/features/FEAT-004/MESSAGES.md` (casse et absence d'accents comprises) et `vp.erreur.creation` de `textes-nouveaux.md` ; « Compte destinataire introuvable » (A-004-4) ; aucun autre texte inventé. Erreurs au format `{"message": …}` ; 401 sans jeton `Session expirée, veuillez vous reconnecter.` (règle portée par `fetchAuthentifie`, non dupliquée). `data-testid` : `vp-compte-debiteur|montant|frequence|numero-beneficiaire|libelle-beneficiaire|date-premiere-execution|motif|creer|message-erreur|message-succes`, et dans le tableau `vp-suspendre-{id}`, `vp-reactiver-{id}`, `vp-supprimer-{id}`. Titre d'onglet « BankApp Demo - Virements programmes ». Erreurs en `role="alert"` + `aria-invalid`, valeurs saisies conservées ; succès en `role="status"`. Tableau natif `<th scope="col">`, ligne vide sur toute la largeur. Dates `dd/MM/yyyy` à l'affichage, `yyyy-MM-dd` en saisie. Couleurs par tokens. Étiqueter les tests `@Tag("FR-004-01")` … `FR-004-08`, `FR-004-13`, `BUG-015` (back) et préfixer `[FR-004-0x]` (front).

**Never:** Pas de suspension, réactivation ni suppression (story 4.4) : les boutons « Suspendre/Reactiver/Supprimer » sont rendus avec leurs `data-testid` mais sans comportement. Pas d'exécution des échéances (4.6). Pas d'appel HTTP entre services, pas d'événement Kafka. Ne pas modifier scripts/gates/**, golden master, `replay-mapping.json`, ni rejouer le golden master. Aucun seuil abaissé, aucune exclusion de couverture. Pas de modale. Pas de `Date.now()`/`new Date()` dans `src/domain` et `src/application`. Pas de numéro de compte bénéficiaire dans les journaux.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Erreur |
|----------|--------------|----------------------|--------|
| Sans jeton | `/virements-programmes` ou `GET`/`POST` API | redirection `/login` ; API 401 | 401 |
| Écran `user1` | ouverture | titre d'onglet, libellés, exemples, `<select>` « -- Choisir -- » puis comptes de `user1` (valeur = id, « {numero} ({type}) - {solde} EUR »), fréquences dans l'ordre | — |
| CONSEILLER sans compte | ouverture | « -- Choisir -- » seul ; « Aucun virement programme configure » | — |
| Liste | `user1` + autre client | seuls ceux de `user1`, tri prochaine exécution croissante, colonnes Source, Beneficiaire, Montant, Frequence, Prochain, Nb executions, Etat (Actif/Inactif), Actions | — |
| Formulaire vide | clic « Creer le virement programme » | « Veuillez selectionner un compte source » dans `vp-message-erreur`, rien créé | message |
| Saisies invalides | compte choisi ; numéro blanc / libellé blanc / date vide / `01/01/2027` / montant vide, `0`, `-5` | « Veuillez saisir le numero de compte destinataire » / « Veuillez saisir le libelle du beneficiaire » / « Veuillez saisir la date de premiere execution » / « Format de date invalide (attendu : yyyy-MM-dd) » / « Montant invalide » (×3) ; valeurs gardées, rien créé ; idem sur appel direct de l'API (400) | 400 |
| Compte source inexistant / d'autrui | appel direct API | 404 « Erreur : Client ou compte introuvable » / 403 « Erreur : Compte non autorise », rien créé | 404 / 403 |
| Destinataire inexistant | numéro inconnu, reste valide | 404 « Compte destinataire introuvable », rien créé (`FR-004-13`, `BUG-015`) | 404 |
| Nominal SC-004-02 | compte `1`, `50`, `MENSUEL`, `FR76-0001-0002`, « [retro] test virprog », `2027-01-01`, « [retro] virement programm gm » | 201 ; actif, 0 exécution, `cree_le` par `Horloge` ; « Virement programme cree avec succes » (`vp-message-succes`), champs vidés (fréquence revenue à `MENSUEL`), liste rechargée avec la ligne | — |
| Date passée | `2020-01-01` | accepté, prochaine exécution = `2020-01-01` | — |
| Trop long | numéro 35, libellé 101, motif 256 caractères via l'API | 400 sans enregistrement | 400 |
| Erreur technique | échec d'enregistrement | 5xx sans détail ; « Erreur : le virement programmé n'a pas été créé. Rien n'a été débité. Veuillez réessayer. » | 5xx |

</intent-contract>

## Code Map

Chemins Java sous `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` (`M/`), tests sous `src/test/java/fr/nextbanking/coeurbancaire/` (`T/`), front sous `frontend/src/` (`F/`). Modèle direct : stories 4.1 et 4.2 (commits 9fe6710, 1451ebc).

- `M/domain/` -- créer `FrequenceVirement` (6 valeurs), `VirementProgramme` (record : id, clientId, compteSourceId, numeroBeneficiaire, libelleBeneficiaire, montant `Montant`, frequence, motif nullable, prochaineExecution `Instant`, actif, nbExecutions, creeLe) et `DemandeVirementProgramme` (validation + normalisation dans l'ordre ci-dessus, exception de saisie portant `champ`). `M/domain/Montant.java` et `M/domain/TextesVirement.java` (ligne 23 : `COMPTE_INTROUVABLE` existant, à ne pas détourner) ; `Compte.appartientA(client)` existe.
- `M/application/port/out/Comptes.java` + `M/adapter/out/persistance/ComptesJdbc.java` (SQL lignes ~30-47) -- ajouter `parNumero(String)` (inexistant) ; `Comptes.trouver(compteId)` pour le compte source ; faux en mémoire `T/application/MemoireCoeurBancaire.java` (~ligne 53) à étendre.
- `M/application/port/out/VirementsProgrammes.java` + `M/adapter/out/persistance/VirementsProgrammesJdbc.java` -- `enregistrer`, `duClient(clientId)` (tri `prochaine_execution` asc, puis id) ; JdbcClient, sur le modèle de `BeneficiairesJdbc` ; table dans `M`-resources `db/migration/V6__virements_programmes.sql` (dernière migration : V5 ; pas de `now()`/`CURRENT_TIMESTAMP` : le temps vient d'`Horloge`).
- `M/application/port/in/CreerVirementProgramme.java`, `ConsulterVirementsProgrammes.java` + cas d'usage associés -- création (source introuvable → 404, d'autrui → 403, destinataire par `Comptes.parNumero`, `Horloge`) et lecture ; la lecture renvoie le numéro du compte source (`Comptes.duClient`).
- `M/adapter/in/PointEntreeVirementsProgrammes.java` -- `GET`/`POST /api/coeur-bancaire/virements-programmes` (`@AuthenticationPrincipal Jwt`, `Long.parseLong(jeton.getSubject())` comme `PointEntreeBeneficiaires`) ; DTO internes (montant en chaîne) ; `M/adapter/in/GestionErreursVirements.java` -- y inscrire le nouveau contrôleur dans `assignableTypes` et les handlers 400/403/404 `{"message"}` ; `M/adapter/in/Textes.java` (`REQUETE_INVALIDE`). `M/infrastructure/` -- `@Bean` d'assemblage (modèle `AssemblageBeneficiaires`).
- `T/adapter/CoeurBancaireSecuriteIT.java` (liste `CHEMINS`, 401 sans jeton) -- ajouter le chemin ; modèles `T/adapter/BeneficiairesIT.java`, `T/adapter/out/persistance/BeneficiairesJdbcIT.java`, `DonneesDeReference.java`, `Jetons.java`, `HorlogeFixe.java`. `services/coeur-bancaire/reference-data/reference-data.sql` : comptes `1` (`FR76-0001-0001`) et `2` (`FR76-0001-0002`) de `user1` ; aucun virement programmé à ajouter.
- `F/adapters/routes/routes.tsx` (route `/beneficiaires` ligne ~127 ; `/virements-programmes` tombe aujourd'hui sur `*`) -- ajouter la route (littéral `'/virements-programmes'` exigé par `verifier-scenario.sh`), hors `ROUTES_PROTEGEES` (figée par `routes.test.tsx`) mais protégée comme `/beneficiaires` ; ajouter `clientVirementsProgrammes` à `Dependances`. `F/domain/navigation.ts:36` contient déjà le lien.
- `F/domain/virementsProgrammes.ts`, `F/application/gererVirementsProgrammes.ts` (port `ClientVirementsProgrammes`, cas d'usage `creerVirementProgramme`, `chargerVirementsProgrammes`) -- types, validation locale dans l'ordre (centimes via `F/domain/montant.ts`), réutiliser `chargerComptes` de `F/application/virement.ts:58` ; `F/adapters/http/ClientVirementsProgrammesHttp.ts` (modèle `ClientBeneficiairesHttp.ts`, `appeler` = `fetchAuthentifie`) ; `F/adapters/ecrans/virementsProgrammes/EcranVirementsProgrammes.tsx|.css` (modèle `EcranBeneficiaires.tsx`, `<title>` via `TEXTES.titresOnglet`) ; composants `F/adapters/ui/` : `ChampTexte` (pas de `type="date"` : saisie texte `yyyy-MM-dd` avec placeholder), `ChampListe`, `BoutonPrimaire`, `AlerteErreur`, `AlerteSucces`, `Tableau` ; `F/adapters/textes.ts` (bloc `virementsProgrammes` ; `titresOnglet` ~118-128 sans entrée aujourd'hui) ; `F/infrastructure/main.tsx:56` (câblage) ; `F/adapters/test/rendreApplication.tsx` (paramètre positionnel, `vi.fn` par défaut, retour).
- Tests front à garder verts : `architecture-temps.test.ts`, `confinement-jeton.test.ts`, `couleurs-hors-tokens.test.ts`, `motifs-interdits.test.ts`, `adapters/routes/routes.test.tsx` ; `adapters/accessibilite.test.tsx` (ajouter `/virements-programmes`).
- Lecture seule : `docs/retro/features/FEAT-004/{RG,MESSAGES,FIELDS}.md`, `docs/retro/golden-master/derived/FEAT-004/SC-004-0{1,2,3}/`, `scripts/golden-master/verifier-scenario.sh` (exige routes littérales, les 8 `data-testid` de saisie hors tests, et les textes dans `frontend/src` ou `reference-data.sql`).

## Tasks & Acceptance

**Execution:**
- `M/domain/` (fréquence, virement programmé, demande validée) -- règles pures -- 100 % lignes et branches
- `M/application/` + ports `in`/`out` (création, lecture, `Comptes.parNumero`) -- FR-004-01 à 08, 13, BUG-015
- `V6__virements_programmes.sql`, `VirementsProgrammesJdbc`, `ComptesJdbc` -- persistance -- tests IT (80 % adapter)
- `PointEntreeVirementsProgrammes`, `GestionErreursVirements`, assemblage -- `GET`/`POST`, 400/401/403/404/5xx `{"message"}`
- Tests back : domaine, cas d'usage, IT adaptateur, IT endpoints (toute la matrice, filtre par client, tri, 401, masquage de journal), `CoeurBancaireSecuriteIT`
- Front : domaine, application, adaptateur HTTP, écran, textes, route, câblage, `rendreApplication`, Vitest (libellés et exemples, `<select>` comptes et fréquences, les huit validations, valeurs conservées, succès + champs vidés + liste rechargée, liste vide, tri/colonnes/états/boutons, erreur technique, 401 → `/login`), accessibilité axe

**Acceptance Criteria:**
- Given un visiteur sans jeton, when il demande `/virements-programmes`, then redirection `/login`, et `GET /api/coeur-bancaire/virements-programmes` répond 401
- Given `user1` connecté, when il ouvre l'écran, then titre d'onglet, libellés, exemples, bouton `vp-creer`, `<select>` des comptes et des fréquences conformes à la matrice
- Given les cas de saisie invalide et de compte source/destinataire de la matrice, when il soumet ou appelle l'API, then message exact, rien créé, valeurs conservées
- Given le parcours SC-004-02 et une date passée, when il crée, then virement actif à 0 exécution, succès affiché, champs vidés, liste rechargée
- Given une erreur technique simulée, when il soumet, then `vp.erreur.creation` sans détail
- Given la story terminée, when `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-01|FR-004-02|FR-004-03|FR-004-04|FR-004-05|FR-004-06|FR-004-07|FR-004-08|FR-004-13|BUG-015'` et `cd frontend && npx vitest run -t 'FR-004-01|FR-004-02|FR-004-03|FR-004-04|FR-004-05|FR-004-07|FR-004-08'`, then code 0
- Given l'écran livré, when `scripts/golden-master/verifier-scenario.sh SC-004-01`, `SC-004-02` et `SC-004-03`, then code 0 (sans rejouer le golden master)
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, then code 0 (100 % domain/application, 80 % adapter/infrastructure)

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 2 findings — high 0, medium 0, low 2, false 0, maybe-false 0
- findings:
  - `[low]` `[reject]` `trace-smoke.sh` cité par la story mais non exécuté — la pile compose est requise, la story n'ajoute ni span créé à la main ni Kafka ; contrôle hors de portée de la boucle, résiduel noté.
  - `[low]` `[reject]` 5xx de l'API sans IT HTTP dédié — le 5xx passe par le gestionnaire général existant `GestionErreurs` (partagé, déjà couvert), et le message `vp.erreur.creation` est testé côté front ; corriger ajouterait un montage d'échec sans valeur nouvelle.

## Design Notes

Choix de planification : statuts HTTP non fixés par la story — 400 saisie, 403 compte d'autrui, 404 compte source ou destinataire introuvable. La colonne « Beneficiaire » affiche libellé (gras) et numéro de compte : le nom du titulaire du compte destinataire exigerait un appel inter-services, interdit. « Frequence » du tableau affiche le nom d'énumération comme le legacy. La date saisie est validée strictement côté API (ISO, jour réel) ; le front ne contrôle que le motif `\d{4}-\d{2}-\d{2}`.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-01|FR-004-02|FR-004-03|FR-004-04|FR-004-05|FR-004-06|FR-004-07|FR-004-08|FR-004-13|BUG-015'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-004-01|FR-004-02|FR-004-03|FR-004-04|FR-004-05|FR-004-07|FR-004-08'` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-004-01 && scripts/golden-master/verifier-scenario.sh SC-004-02 && scripts/golden-master/verifier-scenario.sh SC-004-03` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** création (`POST`) et lecture (`GET`) des virements programmés dans le cœur bancaire (domaine `VirementProgramme`, six fréquences, migration V6, contrôle d'existence du compte destinataire par numéro — BUG-015, validation dans l'ordre du legacy, prochaine exécution = date saisie à 00:00 Europe/Paris) ; écran `/virements-programmes` (formulaire + tableau, route déclarée, boutons d'action rendus sans comportement, réservés à la story 4.4).

**Fichiers :** back — `domain/` (`FrequenceVirement`, `VirementProgramme`, `LigneVirementProgramme`, `DemandeVirementProgramme`, `ErreurVirementProgramme`), cas d'usage et ports, `Comptes.parNumero`, `VirementsProgrammesJdbc`, `PointEntreeVirementsProgrammes`, `GestionErreursVirements`, `V6__virements_programmes.sql` et leurs tests ; front — domaine, application, `ClientVirementsProgrammesHttp`, `EcranVirementsProgrammes`, textes, route, câblage, `ChampListe` étendu (libellé vide, invalide, ref), tests dont axe.

**Revue :** 0 patch, 0 report, 2 constats rejetés (2 négligeables). Relance recommandée : non.

**Vérification :** `mvn … -Dgroups=FR-004-… BUG-015` code 0 (33 tests) ; `vitest -t FR-004-…` code 0, suite complète 1443 verts ; `verifier-scenario.sh SC-004-01/02/03` code 0 ; `build.sh`, `check-gates-exist.sh`, `golden-master.sh` code 0.

**Risques résiduels :** `trace-smoke.sh` non lancé (pile compose requise, aucun span ni Kafka ajouté) ; statuts HTTP 400/403/404 choisis à la planification ; textes en capitales listés « non trouvés » par `verifier-scenario.sh` (rendus par le CSS, sans échec) ; colonne « Beneficiaire » = libellé + numéro, sans nom du titulaire (appel inter-services interdit) ; montant affiché `EUR35.00` comme le legacy.
