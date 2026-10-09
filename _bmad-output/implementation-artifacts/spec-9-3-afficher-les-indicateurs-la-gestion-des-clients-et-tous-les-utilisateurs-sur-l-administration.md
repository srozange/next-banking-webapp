---
title: "Afficher les indicateurs, la gestion des clients et tous les utilisateurs sur l'administration"
type: 'feature'
created: '2026-10-09'
status: 'done'
baseline_revision: '396051cfdc324fc8249fafffb20a7c4bd7cf4a96'
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

**Problem:** la page `/administration` n'affiche que le pipeline et l'historique des prêts puis la revue des virements suspendus : l'ADMIN n'a ni indicateurs globaux, ni gestion des clients (recherche, détail avec « Client depuis »), ni liste de tous les utilisateurs, et aucun service n'expose ces données.

**Approach:** trois points ADMIN dans `identite-clients` (`GET /api/identite-clients/administration/{indicateurs|clients|utilisateurs}`) et un dans `credits` (`GET /api/credits/administration/indicateurs`) ; côté front, un écran `EcranAdministration` qui lit les quatre en parallèle et insère, dans l'ordre figé, indicateurs → pipeline → « Gestion des clients » (filtre local, panneau de détail inline) → historique → « Tous les utilisateurs du systeme » → revue des virements.

## Boundaries & Constraints

**Always:**
- Rôle ADMIN exigé par le point unique déjà en place (`DroitsRoles.exigerAdmin` dans identite-clients, `DroitsAdministrateur.exigerAdmin` dans credits) ; CONSEILLER/CLIENT → 403 `{"message":"Accès refusé"}` (corps exact du service), sans jeton → 401. Aucun paramètre de requête lu.
- Formes JSON (camelCase, utilisateurs non supprimés seulement) :
  - `…/administration/indicateurs` (identité) : `{totalClients, clientsActifs, conseillers, totalUtilisateurs}` — comptes par rôle CLIENT, CLIENT actifs, CONSEILLER (tous), tous rôles.
  - `…/administration/clients` : `[{clientId, nomComplet, login, email, telephone, role, actif, nomConseiller, dateCreation}]`, rôle CLIENT seulement, trié par `nomComplet` puis `clientId` ; `telephone` et `nomConseiller` peuvent être `null` ; `dateCreation` = `customers.date_creation` au format `yyyy-MM-dd` (BUG-055 corrigé, jamais `advisor_since`).
  - `…/administration/utilisateurs` : `[{utilisateurId, nomComplet, login, email, role, actif}]`, tous rôles, même tri.
  - credits `…/administration/indicateurs` : `{pretsEnAttente, montantEnAttente}` — nombre et cumul des prêts `EN_ATTENTE`, montant en chaîne à 2 décimales (`"15000.00"`, `"0.00"` si aucun).
- Front : quatre appels lancés ensemble (`Promise.all`), par `fetchAuthentifie` seulement (câblage `main.tsx`) ; un 401 termine la session (motif `chargerPortefeuille`). Une réponse en échec n'empêche pas les autres parties : son `message` texte (sinon `TEXTES.erreurTechnique`) s'affiche dans `AlerteErreur testId="admin-message-erreur"` de la section concernée ; les cartes KPI d'identité dépendent du seul appel identité, la carte « Prets en attente » du seul appel credits.
- Indicateurs (`card-muted`, libellés en capitales par `text-transform` seulement) : « Total clients » / « {n} actifs », « Conseillers » / « actifs », « Prets en attente » / « {montant} a traiter » avec `{montant}` = `formaterEurosArrondi` de `domain/portefeuille.ts` (format legacy `EUR15,000`, `EUR0`), « Total utilisateurs » / « tous roles confondus ».
- « Gestion des clients » : champ sans libellé visible (`aria-label` et `placeholder` « Rechercher par nom, login, email... », `data-testid="admin-recherche"`), bouton « Filtrer » (`BoutonSecondaire`, `admin-filtrer`) dans un `<form>` (Entrée = Filtrer) ; filtre appliqué au clic seulement, sur `nomComplet`, `login`, `email`, partiel, sans casse, terme rogné, vide → tous ; aucune ligne → ligne unique « Aucun client trouve », en-têtes gardés. Colonnes « Nom », « Login », « Email », « Role », « Statut », « Conseiller », « Actions » ; bouton « Detail » (`admin-detail-{clientId}`) seul dans « Actions » (« Desactiver »/« Activer » = story 9.4).
- Panneau de détail inline (sous le tableau, pas de modale ni de route) : titre `<h3 tabIndex={-1}>` « Detail : {nomComplet} » focalisé à l'ouverture ; champs « Nom complet », « Identifiant », « Email », « Telephone », « Role », « Statut », « Conseiller assigne » (absent si `nomConseiller` nul), « Client depuis » `jj/MM/aaaa` ; « Fermer » (`admin-fermer-detail`) ou `Échap` ferme et rend le focus au bouton « Detail » déclencheur ; un nouveau filtrage ferme le panneau.
- Rôle affiché tel quel en `Etiquette` (`CLIENT`, `CONSEILLER`, `ADMIN`) ; Statut en `Etiquette` « Actif »/« Inactif ». Textes legacy dans `TEXTES.administration` de `textes.ts` (JSDoc citant FEAT-016 `MESSAGES.md` l.977-1080).
- Hexagone, couverture (100 % domain/application, ≥ 80 % adaptateurs), tests JUnit `@Tag("FR-016-02")` (indicateurs, deux services), `FR-016-08` (liste CLIENT seule), `FR-016-09` (champs du détail), `FR-016-10` + `BUG-055` (date de création), `FR-016-13` (utilisateurs), `FR-016-01`/`FR-030-07` (403/401) ; tests Vitest titrés `[FR-016-02]`, `[FR-016-08]`, `[FR-016-09]`, `[FR-016-10]`, `[BUG-055]`, `[FR-016-13]`.

**Never:**
- Pas d'appel HTTP entre services, pas de bibliothèque partagée, pas de nouvelle lecture de `Role.ADMIN` hors du point unique (`RoleAdminArchitectureTest` inchangé).
- Pas d'activation/désactivation (9.4), d'alertes (9.5), de journal ni de catalogue (9.6) ; pas de texte inventé (pas de libellé « Client »/« Administrateur » pour les rôles) ; pas de `<dialog>` ni de modale.
- Ne pas toucher `scripts/gates/**`, `docs/quality-gates.md`, `sprint-status.yaml`, `frontend/.dependency-cruiser.cjs`, le golden master (jamais rejoué) ; ne retirer aucune assertion des tests existants (pipeline, revue, routes, main).

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Indicateurs de référence | 7 utilisateurs (1 ADMIN, 2 CONSEILLER, 4 CLIENT dont 3 actifs) ; prêts `EN_ATTENTE` 5000.00 et 10000.00 | identité `{4,3,2,7}` ; credits `{2,"15000.00"}` ; écran « 4 »/« 3 actifs », « 2 »/« actifs », « 2 »/« EUR15,000 a traiter », « 7 »/« tous roles confondus » | — |
| Aucun prêt en attente | credits `{0,"0.00"}` | « 0 » / « EUR0 a traiter » | — |
| Filtre « DUPONT » | 4 clients | seule la ligne Jean Dupont | — |
| Filtre « user » | idem | les 4 clients (login `user1`…`user4`) | — |
| Filtre sans résultat | « zzz » | en-têtes + « Aucun client trouve » | — |
| BUG-055 | `date_creation` 2024-02-01, `advisor_since` 2025-06-15 | `dateCreation:"2024-02-01"` ; panneau « Client depuis » 01/02/2024 | — |
| Client sans conseiller | `advisor_id` nul | `nomConseiller:null` ; ni libellé ni valeur « Conseiller assigne » ; « Client depuis » affiché | — |
| Panne credits | 500 `{"message":"Service crédits indisponible"}` | ce message dans la section indicateurs ; les trois autres KPI, les clients et les utilisateurs affichés | corps illisible → « Une erreur technique est survenue » |
| Non-ADMIN | jeton CONSEILLER ou CLIENT sur un des 4 points | 403 `{"message":"Accès refusé"}` | sans jeton : 401 |
| Route pour un CLIENT/CONSEILLER | ouvre `/administration` | redirigé vers `/tableau-de-bord`, aucune méthode du client d'administration appelée | — |

</intent-contract>

## Code Map

**identite-clients** (`services/identite-clients/src/main/java/fr/nextbanking/identiteclients/`)
- `application/DroitsRoles.java` -- `exigerAdmin(Set<Role>)` (lève `AccesRefuse` → 403) : à appeler, ne pas dupliquer.
- `application/ConsulterPortefeuilleCasUsage.java`, `application/port/{in/ConsulterPortefeuille,out/PortefeuilleClients}.java`, `domain/ClientDuPortefeuille.java`, `adapter/out/persistance/PortefeuilleClientsJdbc.java`, `adapter/in/http/PointEntreePortefeuille.java` -- chaîne modèle (port in → cas d'usage → port out JDBC → contrôleur `JetonActeur.acteurId/roles`).
- `infrastructure/AssemblageConnexion.java` -- câblage `@Bean` des cas d'usage.
- Table `customers` (V1, V3) : `id, username, full_name, email, phone, role, active, advisor_id, advisor_since, date_creation, supprime` ; jointure `left join customers a on a.id = c.advisor_id` pour `nomConseiller`.
- Tests : `src/test/.../adapter/PortefeuilleIT.java` (modèle IT : `OutilsHttp.chargerDonnees`, `executer`, `avecJeton`, `jetonValide(sub, role)`), `adapter/IdentiteClientsSecuriteIT.java:108-140` (refus 403/401 tagués), `application/ConsulterPortefeuilleCasUsageTest.java` (faux port), `RoleAdminArchitectureTest.java` (ne pas élargir).

**credits** (`services/credits/src/main/java/fr/nextbanking/credits/`)
- `application/ConsulterPipelinePretsCasUsage.java` + `application/port/in/ConsulterPipelinePrets.java` -- ajouter `indicateurs(Set<Role>)` (exigerAdmin, depuis `PretRepository.enAttente()`) ; `domain/` nouveau record `IndicateursPrets(long nombre, BigDecimal montant)` (somme à 2 décimales, zéro = `0.00`).
- `adapter/in/http/PointEntreeAdministrationPrets.java` -- ajouter `GET /api/credits/administration/indicateurs` (`JetonRoles.depuis`, `toPlainString`).
- Tests : `src/test/.../application/ConsulterPipelinePretsCasUsageTest.java`, `adapter/AdministrationPretsHttpIT.java` (seed SQL clients + prêts), `adapter/CreditsSecuriteIT.java`.

**frontend** (`frontend/src/`)
- `adapters/routes/routes.tsx:85-90,159-167` -- `/administration` rend aujourd'hui `<title>` + `SectionsPretsAdministration` + `SectionRevueVirements` : remplacer par `<EcranAdministration …/>` ; ajouter `clientAdministration` à `Dependances` et à la déstructuration.
- `adapters/ecrans/administration/SectionsPretsAdministration.tsx` -- rend pipeline puis historique en un fragment : ajouter une prop optionnelle (ex. `entrePipelineEtHistorique?: ReactNode`) rendue entre les deux sections, sans changer son état ni ses tests.
- `application/consulterPortefeuille.ts` + `adapters/http/ClientPortefeuilleHttp.ts` -- motif à reproduire (port à réponses `ok|nonAuthentifie|erreur{message}`, `sessionOuverteOuFin`, `Promise.all`, 401 → `session.terminer()`, gardes de forme, `message` texte du corps).
- `adapters/ecrans/espaceConseiller/EcranEspaceConseiller.tsx:57-99,246-260` + `.css` -- motifs KPI `card-muted` + `text-transform`, panneau inline (focus titre, `Échap`, carte des déclencheurs `Map<number, HTMLButtonElement>`).
- `domain/portefeuille.ts:122` (`formaterEurosArrondi`), `domain/montant.ts` (`centimesDepuisChaine`), `domain/pret.ts:116` (`formaterDateEcheance` `yyyy-MM-dd` → `jj/MM/aaaa`) -- réutiliser.
- `adapters/ui/{Tableau,Etiquette,Carte,BoutonSecondaire,AlerteErreur}.tsx` -- composants ; `Tableau` garde les en-têtes au-dessus d'un corps vide.
- `adapters/textes.ts:117-150` (`administrationPrets`) -- ajouter le bloc `administration` (KPI, gestion des clients, détail, utilisateurs).
- `infrastructure/main.tsx` + `main.test.ts:88-91` -- câblage `creerClientAdministrationHttp(fetchAuthentifie)` ; `main.test` lit aujourd'hui les sections par position dans le fragment : l'adapter aux props de `EcranAdministration` et ajouter le nouveau client à la liste `authentifies`.
- `adapters/test/rendreApplication.tsx:48-85,130-182` -- ajouter `clientAdministration` en dernier paramètre positionnel (défaut : réponses ok vides), l'exposer, JSDoc à jour.
- Tests transverses touchés : `adapters/routes/routes.test.tsx`, `adapters/accessibilite.test.tsx` (axe sur `/administration`), `ecrans/administration/*.test.tsx` (doivent rester verts), `motifs-interdits.test.ts`, `couleurs-hors-tokens.test.ts`, `confinement-jeton.test.ts`.

## Tasks & Acceptance

**Execution:**
- `services/identite-clients/.../domain/FicheUtilisateur.java` (record lecture : id, nomComplet, login, email, telephone, role, actif, nomConseiller, dateCreation `LocalDate`) et `domain/IndicateursUtilisateurs.java` -- données de l'administration.
- `services/identite-clients/.../application/port/in/ConsulterAdministration.java`, `application/ConsulterAdministrationCasUsage.java`, `application/port/out/AnnuaireUtilisateurs.java` (`tous()` : non supprimés), `adapter/out/persistance/AnnuaireUtilisateursJdbc.java` (`date_creation`, jointure conseiller), `adapter/in/http/PointEntreeAdministration.java` (trois `GET`, `dateCreation.toString()`), bean dans `AssemblageConnexion` -- indicateurs calculés, clients filtrés au rôle CLIENT et tris faits dans le cas d'usage (Java pur).
- Tests identite-clients : `ConsulterAdministrationCasUsageTest` (comptes, filtrage CLIENT, tri, refus non-ADMIN, liste vide), `PointEntreeAdministrationTest` si utile à la couverture, `AdministrationIT` (réponses exactes sur les données de référence, `BUG-055` avec dates distinctes, client sans conseiller, supprimé exclu, paramètre ignoré), cas 403/401 des trois chemins dans `IdentiteClientsSecuriteIT`.
- `services/credits/...` -- `IndicateursPrets`, `ConsulterPipelinePrets.indicateurs`, point `GET …/administration/indicateurs` ; tests cas d'usage (somme, zéro, refus), IT HTTP (2 prêts EN_ATTENTE 5000.00 + 10000.00 parmi d'autres statuts → `{"pretsEnAttente":2,"montantEnAttente":"15000.00"}`), `CreditsSecuriteIT` 403/401.
- `frontend/src/domain/administration.ts` (+ test) -- types, `filtrerClients(clients, terme)`, sous-libellés KPI (via `formaterEurosArrondi(centimesDepuisChaine(…))`), date « Client depuis ».
- `frontend/src/application/consulterAdministration.ts` (+ test) -- port `ClientAdministration { lireIndicateursIdentite(); lireIndicateursPrets(); lireClients(); lireUtilisateurs() }`, cas d'usage parallèle → résultat par partie (`donnees | null` + message) ou `deconnecte`.
- `frontend/src/adapters/http/ClientAdministrationHttp.ts` (+ test) -- quatre chemins exportés, gardes de forme, 401, `message` ou erreur technique, réseau coupé.
- `frontend/src/adapters/ecrans/administration/{EcranAdministration,SectionIndicateursAdministration,SectionGestionClients,SectionUtilisateurs}.tsx` + `.css` + tests -- écran et sections ; `SectionsPretsAdministration.tsx` reçoit la section clients entre pipeline et historique.
- `frontend/src/adapters/{textes.ts,routes/routes.tsx,routes/routes.test.tsx,test/rendreApplication.tsx,accessibilite.test.tsx}`, `infrastructure/{main.tsx,main.test.ts}` -- textes, branchement, câblage, cas axe (chargé, détail ouvert).
- Test d'ordre `[FR-016-13]`/`[FR-016-08]` sur un jeu équivalent aux données de référence : `textContent` de la page → indices croissants des sections (indicateurs, « Pipeline Prets — En attente d approbation », « Gestion des clients », « Historique de tous les prets », « Tous les utilisateurs du systeme », « Virements suspendus — En attente de revue ») et « Gestion des clients », « Jean Dupont », « user1 » dans les 5 000 premiers caractères.
- Violation volontaire : `AnnuaireUtilisateursJdbc` lit `advisor_since` pour `dateCreation` → `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='BUG-055'` non nul ; vérifier puis annuler.

**Acceptance Criteria:**
- Given l'ADMIN connecté, when il ouvre `/administration`, then les quatre appels partent avant qu'aucun ne soit résolu et la page affiche les quatre indicateurs, « Gestion des clients » avec les 4 clients (jamais un CONSEILLER ni un ADMIN) et « Tous les utilisateurs du systeme » avec les 7 utilisateurs, dans l'ordre figé, les tests existants du pipeline et de la revue restant verts.
- Given la ligne de Jean Dupont, when l'ADMIN clique « Detail », then le panneau « Detail : Jean Dupont » s'ouvre avec le focus sur son titre ; « Fermer » ou `Échap` le referme et rend le focus au bouton.
- Given les commandes `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-02 | FR-016-08 | FR-016-09 | FR-016-10 | BUG-055 | FR-016-13'` et `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-016-02'`, then code 0.
- Given `cd frontend && npx vitest run -t 'FR-016-0'`, `-t 'FR-016-1'` et `-t 'BUG-055'`, then code 0.
- Given l'écran livré, when `scripts/golden-master/verifier-scenario.sh SC-016-01` puis `SC-016-02`, then code 0.
- Given la story terminée, when `scripts/gates/build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh`, then code 0.

## Spec Change Log

## Review Triage Log

### 2026-10-09 — Review pass
- Couches lancées : alignement sur l'intention (seule couche configurée) ; couches cas limites et trous de vérification non configurées, sautées.
- verdicts: 11 findings — high 0, medium 0, low 0, false 11, maybe-false 0
- findings:
  - `[false]` `[reject]` Capitales des libellés KPI non prouvées au rendu (jsdom) — la story exige les capitales « par CSS » ; `text-transform: uppercase` est posé sur `.ecran-administration__libelle-indicateur`, le texte source garde sa casse (UX-DR156, AR-79), comme en 9.2 ; le rejeu lit `innerText`, qui applique le CSS. Aucun mauvais résultat.
  - `[false]` `[reject]` Sous-libellé « EUR15,000 a traiter » au lieu d'une forme « 15 000,00 EUR » — l'AC impose « au format du legacy » : `MESSAGES.md` FEAT-016 (`{montantTotalPending EUR} a traiter`, `f:convertNumber type="currency"`) et la capture SCR-016-03 (« EUR0 a traiter ») donnent bien `EUR{N,NNN}`. Décision consignée dans Design Notes.
  - `[false]` `[reject]` Parallélisme prouvé au niveau du port, pas des requêtes HTTP — chaque méthode de `ClientAdministrationHttp` appelle `appeler(…)` immédiatement, sans `await` préalable : appeler les quatre méthodes avant toute résolution émet les quatre requêtes ensemble ; même preuve qu'en 9.2.
  - `[false]` `[reject]` Redirection d'un CLIENT/CONSEILLER : textes du tableau de bord et absence de message non vérifiés par le nouveau test — déjà couverts : `RouteProtegee.test.tsx` (« dans l'application, la redirection se fait sans message », aucune `role="alert"`), les textes du tableau de bord par `EcranTableauDeBord.test.tsx` et `verifier-scenario.sh SC-016-02` (2/2 textes trouvés) ; le nouveau test prouve l'absence d'appel d'administration.
  - `[false]` `[reject]` 403/401 sur les quatre points — l'auditeur constate lui-même la couverture HTTP réelle (`IdentiteClientsSecuriteIT`, `CreditsSecuriteIT`, corps exact) ; aucun défaut.
  - `[false]` `[reject]` Tests front sur un faux `ClientAdministration`, IT credits avec ses propres prêts — motif de tous les écrans (adaptateur testé sur faux `fetch`, rejeu réservé à l'epic 10) ; l'IT credits fixe 5000 + 10000 en `EN_ATTENTE` parmi d'autres statuts et attend `{"pretsEnAttente":2,"montantEnAttente":"15000.00"}`. Aucun mauvais résultat.
  - `[false]` `[reject]` Ordre des sections vérifié par Vitest et non par rejeu — c'est exactement ce que l'AC demande (« test Vitest sur un jeu équivalent aux données de référence ») ; rejouer le golden master avant l'epic 10 est interdit.
  - `[false]` `[reject]` Violation volontaire BUG-055 sans trace — rejouée pendant la revue : `AnnuaireUtilisateursJdbc` lisant `advisor_since` → `mvn … test -Dgroups='BUG-055'` code 1 (2 tests de `AdministrationIT` rouges), puis fichier restauré à l'identique (`cmp`).
  - `[false]` `[reject]` Commandes (gates, `verifier-scenario`) sans trace dans le diff — lancées hors diff, codes de sortie consignés dans « Auto Run Result » ; un diff de code n'a pas à les contenir.
  - `[false]` `[reject]` Colonne « Role » affichant le code au lieu du libellé legacy — l'epic ne fixe pas la valeur ; « Administrateur » n'existe ni dans `MESSAGES.md` ni dans `textes-nouveaux.md` (texte inventé interdit) ; SC-016-01 ne compare aucun libellé de rôle. Décision consignée dans Design Notes.
  - `[false]` `[reject]` Carte « Conseillers » comptant tous les conseillers malgré « actifs » — conforme à RG-016-006 (« count of all customers whose role is CONSEILLER ») et à FR-016-02 ; « actifs » est le sous-libellé legacy, pas un filtre.

## Design Notes

- Filtrage côté front : la liste des clients (rôle CLIENT, quelques centaines au plus) est lue une fois ; le filtre du legacy (`contains` sans casse sur nom, login, email) est une règle pure de `src/domain`, testée à 100 %, appliquée au clic « Filtrer ». Le serveur reste seul juge du rôle et du périmètre.
- Rôle affiché par son code : le legacy montrait « Client », « Conseiller », « Administrateur », mais « Administrateur » n'existe ni dans `MESSAGES.md` ni dans `textes-nouveaux.md` ; afficher le code métier tel quel (comme les types de compte) évite un texte inventé. SC-016-01 ne compare aucun libellé de rôle.
- Sous-libellé « Prets en attente » : le legacy rend `{montantTotalPending}` par `f:convertNumber type="currency"` (« EUR0 a traiter » sur la capture SCR-016-03) ; `formaterEurosArrondi` reproduit déjà ce format pour l'Espace Conseiller.
- « Client depuis » des données de référence : `reference-data.sql` ne fixe pas `date_creation`, qui prend la date de chargement ; aucun scénario du golden master ne compare cette date.

## Verification

**Commands:**
- `mvn -B -q -f services/identite-clients/pom.xml verify` et `mvn -B -q -f services/credits/pom.xml verify` -- code 0
- les deux commandes `-Dgroups` des critères -- code 0
- `cd frontend && npx vitest run --coverage` -- code 0, seuils tenus
- `scripts/golden-master/verifier-scenario.sh SC-016-01` et `SC-016-02` -- code 0
- `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` -- code 0

## Auto Run Result

Status: done

**Résumé :** la page `/administration` (FEAT-016) affiche désormais, dans l'ordre figé, les quatre indicateurs (« Total clients », « Conseillers », « Prets en attente », « Total utilisateurs », libellés en capitales par CSS), le pipeline des prêts, « Gestion des clients » (filtre au clic sur « Filtrer » ou Entrée, panneau de détail inline « Detail : {nom} » avec « Client depuis » = date de création de la fiche, BUG-055 corrigé), l'historique des prêts, « Tous les utilisateurs du systeme », puis la revue des virements suspendus. Les données viennent de trois points ADMIN d'identite-clients et d'un point ADMIN de credits, lus en parallèle ; une panne n'affiche son message que dans sa section, un 401 ramène à la connexion.

**Fichiers :**
- `services/identite-clients/.../domain/{FicheUtilisateur,IndicateursUtilisateurs}.java` — fiche d'administration et indicateurs calculés.
- `services/identite-clients/.../application/{ConsulterAdministrationCasUsage,port/in/ConsulterAdministration,port/out/AnnuaireUtilisateurs}.java` — ADMIN exigé par `DroitsRoles`, clients filtrés au rôle CLIENT, tri par nom puis identifiant.
- `services/identite-clients/.../adapter/out/persistance/AnnuaireUtilisateursJdbc.java` — utilisateurs non supprimés, nom du conseiller, `date_creation`.
- `services/identite-clients/.../adapter/in/http/PointEntreeAdministration.java`, `infrastructure/AssemblageConnexion.java` — trois `GET …/administration/…` et câblage.
- `services/credits/.../domain/IndicateursPrets.java`, `ConsulterPipelinePrets(CasUsage)`, `PointEntreeAdministrationPrets.java` — `GET /api/credits/administration/indicateurs`.
- `frontend/src/domain/administration.ts` — types, filtre, montant « a traiter », date « Client depuis ».
- `frontend/src/application/consulterAdministration.ts` — port et cas d'usage parallèle par partie.
- `frontend/src/adapters/http/ClientAdministrationHttp.ts` — quatre lectures, gardes de forme, `message` ou erreur technique.
- `frontend/src/adapters/ecrans/administration/{EcranAdministration,SectionIndicateursAdministration,SectionGestionClients,SectionUtilisateurs}.tsx` + `EcranAdministration.css` — écran et sections ; `SectionsPretsAdministration.tsx` reçoit la gestion des clients entre pipeline et historique.
- `frontend/src/adapters/{textes.ts,routes/routes.tsx,test/rendreApplication.tsx,accessibilite.test.tsx}`, `frontend/src/infrastructure/{main.tsx,main.test.ts}` — textes legacy, branchement, câblage `fetchAuthentifie`, cas axe.
- Tests : cas d'usage, domaine, contrôleurs, `AdministrationIT`, `AdministrationPretsHttpIT`, `*SecuriteIT` (401/403) des deux services ; Vitest domaine, cas d'usage, adaptateur, écran (`[FR-016-02]`, `[FR-016-08]`, `[FR-016-09]`, `[FR-016-10]`, `[BUG-055]`, `[FR-016-13]`, `[FR-016-01]`).

**Revue :** 11 constats (couche alignement sur l'intention), 0 correctif, 0 report, 11 rejetés comme faux : capitales exigées par CSS ; format monétaire legacy `EUR15,000` ; parallélisme du port = parallélisme HTTP (appel immédiat) ; redirection sans message et textes du tableau de bord déjà couverts ; 401/403 couverts ; motif des faux ports commun à tous les écrans ; ordre par Vitest exigé par l'AC ; violation volontaire rejouée ; commandes lancées hors diff ; rôle par son code (pas de texte inventé) ; « Conseillers » compte tous les conseillers (RG-016-006). Hors revue : virgule ajoutée dans la JSDoc d'`AssemblageConnexion`.

**Revue complémentaire recommandée :** non (correctifs : high 0, medium 0, low 0).

**Vérification :**
- `scripts/gates/build.sh` : code 0 (build propre de tous les services, couverture et ArchUnit tenus ; front 1 927 tests, lignes 99,05 %).
- `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-02 | FR-016-08 | FR-016-09 | FR-016-10 | BUG-055 | FR-016-13'` et `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-016-02'` : code 0.
- `cd frontend && npx vitest run -t 'FR-016-0'` (71 tests), `-t 'FR-016-1'` (70), `-t 'BUG-055'` (3) : verts.
- `scripts/golden-master/verifier-scenario.sh SC-016-01` : code 0 (« CONSEILLERS », « PRETS EN ATTENTE » consignés comme non trouvés statiquement : capitales CSS) ; `SC-016-02` : code 0 (2/2).
- `scripts/gates/check-gates-exist.sh` (aucun KO), `scripts/gates/trace-smoke.sh`, `scripts/gates/golden-master.sh` : code 0. Golden master non rejoué.
- Violation volontaire (`advisor_since` lu pour `dateCreation`) : `-Dgroups='BUG-055'` code 1 (2 tests rouges), puis restauration à l'identique.

**Risques résiduels :**
- `reference-data.sql` d'identite-clients ne fixe pas `date_creation` : dans une application lancée, « Client depuis » des clients de référence vaut la date de chargement (aucun scénario ne la compare).
- L'écran relit ses quatre points à chaque ouverture de `/administration` (pas de cache de session, non demandé) ; les indicateurs ne sont pas relus après une décision sur un prêt.
- Le `data-testid` `admin-message-erreur` peut apparaître dans plusieurs sections à la fois (déjà utilisé par le message de décision du pipeline), comme le prévoit le contexte de l'epic.
