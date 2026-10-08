---
title: 'Vérifier un IBAN et ajouter un bénéficiaire'
type: 'feature'
created: '2026-10-08'
status: 'done'
baseline_revision: '9fe6710c835acdc6a2df1316533fe7f54d598400'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-4-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-1-consulter-ses-beneficiaires-externes-et-en-supprimer-un.md'
  - '{project-root}/docs/quality-gates.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** L'écran `/beneficiaires` (story 4.1) liste et supprime mais n'offre ni vérification d'IBAN ni ajout : aucune API ne valide un IBAN (MOD-97), ne le normalise ni n'enregistre un bénéficiaire externe.

**Approach:** Cœur bancaire : `POST /api/coeur-bancaire/beneficiaires/verification-iban` (résultat structuré, sans texte) et `POST /api/coeur-bancaire/beneficiaires` (validation, normalisation, enregistrement actif, zone SEPA calculée, date de création par `Horloge`). Front : section « Nouveau beneficiaire SEPA » de l'écran, qui appelle ces deux routes et compose les textes ; aucun calcul MOD-97 côté front.

## Boundaries & Constraints

**Always:** Client déduit du jeton (`sub`). Hexagone respecté (domain/application sans Spring). MOD-97 uniquement dans le domaine du cœur bancaire. Textes à l'identique de `docs/retro/features/FEAT-013/MESSAGES.md` (MSG-013-030 à 043, 060 à 063, 070 à 076) et `beneficiaire.erreur.ajout` de `textes-nouveaux.md` ; aucun texte inventé. Normalisation : IBAN sans espaces et en majuscules, BIC rogné et en majuscules (vide → absent), banque rognée (vide → absente). Ordre de validation de l'ajout : libellé, IBAN vide, IBAN MOD-97, titulaire, BIC `^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$` ; libellé, titulaire, banque ≤ 100 caractères (101 → 400 sans enregistrement). Enregistré : `actif=true`, `zone_sepa` depuis le code pays, `cree_le` dérivé de `Horloge.aujourdhui()` (jamais `now()`). IBAN en clair jamais dans les journaux (objet-valeur `Iban`). Erreur technique : message unique `beneficiaire.erreur.ajout`, sans détail, rien d'enregistré. Champs en erreur : `role="alert"`, `aria-invalid="true"`, valeurs conservées ; résultat de vérification en `aria-live="polite"` ; `maxlength="100"` sur libellé, titulaire, banque ; succès en `role="status"` dans `beneficiaires-message-info`, liste rechargée, formulaire vidé. `data-testid` : `beneficiaires-libelle|titulaire|iban|bic|banque|verifier-iban|ajouter|message-info`. Couleurs par tokens.

**Never:** Pas de MOD-97 ni de liste de pays côté front ; le bouton « Verifier l'IBAN » n'envoie pas le formulaire (`type="button"`). Ne pas modifier scripts/gates/**, golden master, `replay-mapping.json`, ni rejouer le golden master. Pas d'appel HTTP entre services. Aucun seuil abaissé, aucune exclusion de couverture. Pas de modale.

## I/O & Edge-Case Matrix

| Scénario | Entrée / État | Comportement attendu | Erreur |
|----------|--------------|----------------------|--------|
| Vérification IBAN vide | `iban` absent ou blanc | 200, statut « vide » → « Veuillez saisir un IBAN. » | — |
| Vérification clé fausse | `FR3530004000000123456789011` | 200, statut « invalide » → « IBAN invalide (echec MOD-97). Verifiez la saisie. » | — |
| Vérification SEPA / hors SEPA | `FR3530004000000123456789010` ; `TR330006100519786457841326` (Turquie, hors zone SEPA) | 200, valide + code pays + zone → « IBAN valide — FR (Zone SEPA) » / « IBAN valide — TR (Hors zone SEPA) » | — |
| Ajout nominal | libellé « [retro] test beneficiaire », titulaire « Retro Test SA », IBAN ` fr35 3000 4000 0001 2345 6789 010 `, BIC ` hsbcfrppxxx `, banque `   ` | 201 ; IBAN `FR3530004000000123456789010`, BIC `HSBCFRPPXXX`, banque absente, SEPA, actif, `cree_le` = jour de `Horloge` ; liste rechargée, « Bénéficiaire ajouté avec succès. », formulaire vidé | — |
| Champ obligatoire vide | libellé vide / IBAN vide / titulaire vide | 400, rien d'enregistré, « Le libellé est obligatoire. » / « L'IBAN est obligatoire. » / « Le nom du titulaire est obligatoire. », champ en `aria-invalid`, valeurs conservées | 400 |
| IBAN invalide à l'ajout | clé fausse | 400 « IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97). » | 400 |
| BIC invalide | `HSBCFR` | 400 « BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques. » | 400 |
| Trop long | libellé, titulaire ou banque de 101 caractères envoyé à l'API | 400, rien enregistré ; à l'écran `maxlength=100` | 400 |
| Sans jeton | POST sans jeton valide | 401 `{"message":"Session expirée, veuillez vous reconnecter."}` ; le front supprime `nb.jeton` et affiche `/login` | 401 |
| Erreur technique | échec de l'enregistrement | 5xx sans détail ; écran « Erreur lors de l'ajout du bénéficiaire. Veuillez réessayer. » ; rien enregistré | 5xx |
| Journaux | ajout d'un IBAN en clair | aucune ligne de journal ne contient l'IBAN ; `FR35****9010` seulement | — |

</intent-contract>

## Code Map

Chemins Java sous `services/coeur-bancaire/src/main/java/fr/nextbanking/coeurbancaire/` (`M/`), tests sous `src/test/java/fr/nextbanking/coeurbancaire/` (`T/`). Modèle direct : la story 4.1 (commit 9fe6710).

- `M/domain/Iban.java` -- record existant (`affichage()`, `zoneSepa()`, `toString()` masqué) ; y ajouter normalisation, `codePays()` et contrôle MOD-97 (pur). Aucun MOD-97 dans le dépôt aujourd'hui. Test : `T/domain/IbanTest.java`.
- `M/domain/BeneficiaireExterne.java`, `M/domain/ErreurBeneficiaire.java` -- l'exception existante porte le texte de suppression ; créer une exception de saisie distincte (texte du domaine + champ concerné + cause longueur) plutôt que de la détourner. Créer un objet-valeur de demande d'ajout qui valide et normalise (BIC, banque, libellé, titulaire, longueurs).
- `M/application/port/out/Beneficiaires.java` + `M/adapter/out/persistance/BeneficiairesJdbc.java` -- ajouter l'insertion (`insert … (client_id, titulaire, iban, libelle, banque, bic, zone_sepa, actif, cree_le)`), JdbcClient ; table `beneficiaires_externes` dans `services/coeur-bancaire/src/main/resources/db/migration/V3__alertes_fraude_et_beneficiaires.sql` (`cree_le timestamptz`, `bic varchar(11)`, colonnes sans défaut) ; pas de migration nécessaire sauf besoin avéré (prochaine : V6). Tests : `T/adapter/out/persistance/BeneficiairesJdbcIT.java`.
- `M/application/port/out/Horloge.java` -- `aujourdhui()` et `fuseau()` ; injecter dans le cas d'usage d'ajout (`ControleFraudeCasUsage` montre le câblage).
- `M/application/port/in/` + `M/application/` -- créer `VerifierIban` et `AjouterBeneficiaire` (ports) et leurs cas d'usage, sur le modèle de `SupprimerBeneficiaireCasUsage` ; journalisation via `Iban` masqué.
- `M/adapter/in/PointEntreeBeneficiaires.java` -- ajouter `POST …/beneficiaires/verification-iban` (corps `{iban}` → statut vide|invalide|valide, `codePays`, `zoneSepa`, sans texte) et `POST …/beneficiaires` (corps `{libelle, titulaire, iban, bic, banque}` → 201) ; records DTO internes ; client = `clientId(jeton)`.
- `M/adapter/in/GestionErreursVirements.java` -- `assignableTypes` inclut déjà `PointEntreeBeneficiaires` ; y ajouter le handler de l'exception de saisie : 400 `{"message", "champ"}` (le `champ` permet au front de marquer le champ en erreur sans analyser le texte). `Textes.REQUETE_INVALIDE` (`M/adapter/in/Textes.java`) pour le cas longueur.
- `M/infrastructure/AssemblageBeneficiaires.java` -- ajouter les deux `@Bean` (avec `Horloge`).
- `T/adapter/BeneficiairesIT.java` (modèle `@SpringBootTest`/Testcontainers, capture de journal BUG-086), `T/adapter/CoeurBancaireSecuriteIT.java` (ligne ~63 : ajouter les deux POST aux chemins protégés), `T/application/MemoireCoeurBancaire.java` + `T/application/BeneficiairesTestes.java` (faux en mémoire à étendre), `T/application/SupprimerBeneficiaireCasUsageTest.java` (modèle de test de cas d'usage). Étiqueter `@Tag("FR-013-04")` … `@Tag("FR-013-08")`.
- Front (`frontend/src/`) : `domain/beneficiaires.ts` (types de formulaire, résultat de vérification, champs en erreur — sans MOD-97), `application/gererBeneficiaires.ts` (port `ClientBeneficiaires` : ajouter `verifierIban` et `ajouter` ; cas d'usage `verifierIban`, `ajouterBeneficiaire` qui recharge la liste), `adapters/http/ClientBeneficiairesHttp.ts` (appel via `appeler` = `fetchAuthentifie`, qui porte la règle du 401 : ne pas la dupliquer), `adapters/ecrans/beneficiaires/EcranBeneficiaires.tsx|.css` (formulaire au-dessus du tableau ; composants `adapters/ui/ChampTexte.tsx` — `facultatif`, `maxLength`, `erreur`, `placeholder` —, `BoutonPrimaire` (`type="button"` pour la vérification), `BoutonSecondaire`, `AlerteErreur`, `AlerteSucces`, `AlerteInfo`), `adapters/textes.ts` (bloc `beneficiaires` ~ligne 163 : libellés, placeholders, messages de vérification et de validation, succès, `beneficiaire.erreur.ajout`), `adapters/test/rendreApplication.tsx`. Tests Vitest préfixés `[FR-013-04]` … `[FR-013-08]`.
- Lecture seule : `docs/retro/features/FEAT-013/RG.md` (RG-013-011 à 033), `MESSAGES.md`, `docs/retro/golden-master/derived/FEAT-013/SC-013-02/` (parcours : les `data-testid` et les textes `Bénéficiaire ajouté avec succès`, `FR35***9010`, `HSBCFRPPXXX`, `HSBC France`, `✓ SEPA` doivent exister dans `frontend/src` ou `reference-data.sql`), `scripts/golden-master/verifier-scenario.sh`.
- Tests transverses front à garder verts : `architecture-temps.test.ts`, `confinement-jeton.test.ts`, `couleurs-hors-tokens.test.ts`, `motifs-interdits.test.ts`, `adapters/accessibilite.test.tsx` (y ajouter le formulaire).

## Tasks & Acceptance

**Execution:**
- `M/domain/` (Iban : normalisation, code pays, MOD-97 ; demande d'ajout validée et normalisée ; exception de saisie) -- règles pures -- hexagone, 100 % couverture lignes et branches
- `M/application/` + ports `in`/`out` (`VerifierIban`, `AjouterBeneficiaire`, insertion dans `Beneficiaires`) -- cas d'usage avec `Horloge`, journal masqué -- FR-013-04 à 08
- `M/adapter/out/persistance/BeneficiairesJdbc.java` -- insertion -- zone SEPA, actif, date de création
- `M/adapter/in/PointEntreeBeneficiaires.java`, `GestionErreursVirements.java`, `M/infrastructure/AssemblageBeneficiaires.java` -- deux POST, 400/401/5xx `{"message"}`
- Tests back (`@Tag("FR-013-04|05|06|07|08")`) : domaine (MOD-97, normalisation, BIC, longueurs), cas d'usage, adaptateur IT, endpoints IT, `CoeurBancaireSecuriteIT`, masquage du journal -- couvrir toute la matrice
- Front : domaine/application/adaptateur HTTP/écran/textes/câblage + tests Vitest (champs et labels, vérification sans soumission ni rechargement, quatre messages de vérification, cinq validations avec `role="alert"` et `aria-invalid`, valeurs conservées, `maxlength`, succès + liste rechargée + formulaire vidé, erreur technique, 401 → `/login`, accessibilité)

**Acceptance Criteria:**
- Given `user1` sur `/beneficiaires`, when l'écran s'affiche, then la section « Nouveau beneficiaire SEPA » porte les cinq champs reliés à leur `<label for>` (obligatoires en `aria-required`) et les boutons `beneficiaires-verifier-iban` et `beneficiaires-ajouter`
- Given un IBAN vide, faux, valide SEPA ou valide hors SEPA, when il clique « Verifier l'IBAN », then seule la route de vérification est appelée, sans soumission ni rechargement, et le message s'affiche sous le champ en `aria-live="polite"`
- Given les cinq cas de validation de la matrice, when il clique « Ajouter le beneficiaire », then 400, rien d'enregistré, message en `role="alert"`, champ en `aria-invalid`, valeurs conservées
- Given une requête sans jeton valide, when elle atteint le cœur bancaire, then 401 `{"message":"Session expirée, veuillez vous reconnecter."}` et le front revient à `/login` en supprimant `nb.jeton`
- Given l'ajout nominal normalisé de la matrice, when il est enregistré, then IBAN, BIC, banque, zone SEPA, actif et date de création conformes
- Given le parcours de SC-013-02, when il clique « Ajouter le beneficiaire », then « Bénéficiaire ajouté avec succès. » dans `beneficiaires-message-info`, liste rechargée avec `FR35***9010`, `HSBCFRPPXXX`, `HSBC France`, `✓ SEPA`, formulaire vidé
- Given une erreur technique simulée, when il clique « Ajouter le beneficiaire », then « Erreur lors de l'ajout du bénéficiaire. Veuillez réessayer. » sans détail et rien enregistré
- Given la story terminée, when `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-013-04|FR-013-05|FR-013-06|FR-013-07|FR-013-08'` et `cd frontend && npx vitest run -t 'FR-013-04|FR-013-05|FR-013-06|FR-013-07|FR-013-08'`, then code 0
- Given l'écran livré, when `scripts/golden-master/verifier-scenario.sh SC-013-02`, then code 0 (sans rejouer le golden master)
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, then code 0 (couverture 100 % domain/application, 80 % adapter/infrastructure)

## Spec Change Log

## Review Triage Log

### 2026-10-08 — Review pass
- verdicts: 4 findings — high 0, medium 0, low 1, false 3, maybe-false 0
- findings:
  - `[false]` `[reject]` 401 → suppression de `nb.jeton` et retour `/login` non implémenté ni testé dans le diff — la règle vit dans `fetchAuthentifie` (testé par `adapters/session/fetchAuthentifie.test.ts`, `confinement-jeton.test.ts`), que l'adaptateur reçoit par `appeler` ; la spec interdit de la dupliquer ; le 401 des deux POST est prouvé côté serveur (`CoeurBancaireSecuriteIT`).
  - `[false]` `[reject]` Tests front contre des clients factices, sans test de bout en bout — contrat `{message, champ}` couvert côté serveur par `BeneficiairesIT` et côté front par les tests d'adaptateur ; le bout en bout relève du golden master, interdit de rejeu avant l'epic 10 (`verifier-scenario.sh SC-013-02` code 0).
  - `[false]` `[reject]` IBAN en clair possiblement journalisé — le cas d'usage ne journalise que l'objet-valeur `Iban` (`toString()` masqué) ; test de capture de journal dans `BeneficiairesIT`.
  - `[low]` `[reject]` Cinquième résultat « technique » à la vérification et titres h1/h2/h3 remaniés — affichage du texte d'erreur technique existant sans détail, hiérarchie validée par le test axe ; rien à corriger.

## Design Notes

La vérification renvoie un résultat structuré (statut, code pays, zone SEPA), sans texte : le front compose les messages MSG-013-060 à 063, ce qui garde MOD-97 et la liste des pays SEPA au seul cœur bancaire. Les 400 de l'ajout portent `{"message", "champ"}` pour que l'écran désigne le champ fautif sans analyser le texte. Le cas « libellé/titulaire/banque > 100 » répond 400 `Requête invalide` (RG-013-029/031 sont « Suspect » : pas de texte inventé). Choisir un IBAN hors SEPA de test dont la clé MOD-97 est vérifiée par le test du domaine.

## Verification

**Commands:**
- `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-013-04|FR-013-05|FR-013-06|FR-013-07|FR-013-08'` -- expected: code 0
- `cd frontend && npx vitest run -t 'FR-013-04|FR-013-05|FR-013-06|FR-013-07|FR-013-08'` -- expected: code 0
- `scripts/golden-master/verifier-scenario.sh SC-013-02` -- expected: code 0
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Auto Run Result

Status: done

**Résumé :** vérification d'IBAN (`POST …/beneficiaires/verification-iban`, résultat structuré sans texte) et ajout d'un bénéficiaire (`POST …/beneficiaires`, validation dans l'ordre du legacy, normalisation, zone SEPA, actif, date de création par `Horloge`) dans le cœur bancaire ; section « Nouveau beneficiaire SEPA » de l'écran `/beneficiaires`, sans MOD-97 côté front.

**Fichiers :** back — `Iban` (MOD-97, normalisation), `DemandeBeneficiaire`, `VerificationIban`, `ErreurSaisieBeneficiaire`, cas d'usage `VerifierIban`/`AjouterBeneficiaire`, `BeneficiairesJdbc`, `PointEntreeBeneficiaires`, `GestionErreursVirements`, `AssemblageBeneficiaires` et leurs tests ; front — domaine, `gererBeneficiaires`, `ClientBeneficiairesHttp`, `EcranBeneficiaires`, `ChampTexte` (option `invalide`), textes, tests.

**Revue :** 0 patch, 0 report, 4 constats rejetés (3 faux, 1 négligeable). Relance recommandée : non.

**Vérification :** `mvn … -Dgroups=FR-013-04…08` vert ; `vitest -t FR-013-04…08` 43 verts ; `verifier-scenario.sh SC-013-02` code 0 (8/8 textes) ; `build.sh`, `check-gates-exist.sh` (0 KO), `golden-master.sh` code 0.

**Risques résiduels :** erreur 5xx testée au niveau cas d'usage et front, pas en IT HTTP ; `trace-smoke.sh` non lancé (pile compose requise, aucun Kafka ni span ajouté) ; BIC de test hors SEPA `TR…` à valeur de MOD-97 vérifiée par les tests du domaine.
