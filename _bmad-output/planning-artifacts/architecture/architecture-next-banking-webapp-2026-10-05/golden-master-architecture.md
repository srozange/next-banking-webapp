---
title: "Golden master : décisions d'architecture du rejeu — next-banking-webapp"
type: architecture-companion
companion_of: ARCHITECTURE-SPINE.md
status: draft
created: 2026-10-05
updated: 2026-10-09
sources:
  - docs/retro/golden-master-contract.md
  - docs/retro/golden-master/replay-config.json
  - docs/retro/golden-master/scenarios/FEAT-NNN/SC-*/ (scenario.json, input-actions.json, input-request.json, expected-*)
  - docs/retro/screens/navigation-flow.md ; docs/retro/00-global/openapi.yaml (pages servies par le legacy)
  - docs/retro/tools/derive-golden-master.py (en-tête, _doc du squelette) ; docs/retro/tools/export-golden-master-tests.py (rendu Playwright Test)
  - docs/retro/00-global/DATA-DICTIONARY.md ; docs/retro/00-global/NFR.md (volumétrie import.sql) ; docs/retro/00-global/INTEGRATIONS-OVERVIEW.md
  - _bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/EXPERIENCE.md ; divergences-golden-master.md
  - _bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/prd.md (FR-001-02/05/06/10, FR-014-01, FR-017-01, FR-030-01, NFR-PAR-01 à 05, NFR-UI-02, A-G-05, A-G-09)
---

# Golden master : décisions d'architecture du rejeu

Ce document porte les **décisions globales** que le contrat du golden master (`docs/retro/golden-master-contract.md`, « Décisions globales ») attend de l'architecture. Il ne remplit pas `replay-mapping.json`. Statut au 2026-10-05 : l'étape G2 (`corpus-bmad-golden-master`) a confirmé ces valeurs et écrit `replay-mapping.json` ; les propositions ci-dessous sont donc **décidées**, sauf les points encore ouverts du §8.

Rappel des décisions déjà prises (spine, memlog) et non rediscutées ici : 8 services par domaine, pas de passerelle ni de BFF, SPA React qui appelle directement les services, connexion par formulaire auprès du service identité qui émet un JWT conservé en stockage local, conteneur web qui sert le front et renvoie les URL d'API vers chaque service, une seule URL de base, runner Playwright Test (tests générés dans `e2e/golden-master`), rejeu déterministe NFR-PAR-05.

Périmètre : 33 scénarios capturés, 2 exclus d'office (SC-003-03, BUG-011 ; SC-010-02, BUG-038), **31 applicables : 26 d'écran, 5 HTTP**. 5 scénarios écrivent (`writes: true`) : SC-001-03, SC-001-05, SC-003-04, SC-004-02, SC-013-02.

## 0. Identifiants de service et URL de base

| Service (domaine) | Identifiant | Préfixe d'API (renvoyé par le conteneur web) |
|---|---|---|
| Identité et clients | `identite-clients` | `/api/identite-clients/` |
| Cœur bancaire | `coeur-bancaire` | `/api/coeur-bancaire/` |
| Crédits | `credits` | `/api/credits/` |
| Documents | `documents` | `/api/documents/` |
| Notifications | `notifications` | `/api/notifications/` |
| Audit | `audit` | `/api/audit/` |
| Bourse | `bourse` | `/api/bourse/` |
| Open banking et conseiller | `open-banking-conseiller` | `/api/open-banking-conseiller/` |

- **Règle de renvoi** : `/api/{identifiant}/**` → le service `{identifiant}`, chemin transmis tel quel ; tout autre chemin → `index.html` de la SPA (repli de l'historique HTML5), sauf les ressources statiques. Même règle dans le proxy du serveur de développement et dans le conteneur web de `compose.yaml`. Le conteneur web n'a ni code métier ni contrôle d'accès propre.
- **URL de base unique** (front, appels HTTP du golden master, sessions navigateur) : `http://localhost:8080/` (port publié du conteneur web dans `compose.yaml`). Valeur de `target_base_url` (confirmée en G2) ; `browser.base_url` = `null` (= `target_base_url`) ; surcharge au rejeu par `GM_BASE_URL`.
- **`auth`** (authentification des scénarios HTTP) : `null`. Les 5 scénarios HTTP sont rejoués **sans session** : c'est ce qu'ils prouvent.

## 1. Table des routes

17 pages servies par le legacy (16 pages `.xhtml` + la servlet `/docs/download` ; la racine `/banking-legacy/` redirige vers la connexion et devient `/`, qui redirige vers `/login` ou vers l'écran d'accueil du rôle). Cible : **15 routes de la SPA** et **2 points d'API sans rendu** (retour Powens, téléchargement). Les noms suivent les écrans de EXPERIENCE.md (« Écrans par rôle ») ; EXPERIENCE.md ne fixe aucune URL, celles-ci, proposées ici, ont été confirmées en G2 et sont définitives (une route publiée ne change plus, comme un `data-testid`).

### 1.1 Pages d'écran (clé `routes` de `replay-mapping.json`)

Le squelette ne crée une clé `routes` que pour les 14 pages de départ des scénarios d'écran ; `login.xhtml` n'y figure pas (SC-001-03 est un scénario HTTP), mais sa route est fixée ici pour les sessions de test (§2).

| # | Page legacy | Route cible (SPA) | Rôles | Scénarios d'écran | API appelée par l'écran (service) |
|---|---|---|---|---|---|
| 1 | `/login.xhtml` | `/login` | public | (sessions de test ; SC-001-05 y arrive) | `POST /api/identite-clients/connexion` (identité et clients) |
| 2 | `/dashboard.xhtml` | `/tableau-de-bord` | tous | SC-001-04, SC-001-05, SC-002-01, SC-002-02 | comptes et opérations (cœur bancaire) ; centre de notifications (notifications) ; « Bonjour, {fullName} » (identité et clients) |
| 3 | `/virement.xhtml` | `/virement` | tous | SC-003-01, SC-003-02, SC-003-04 (SC-003-03 exclu) | cœur bancaire |
| 4 | `/virement-programme.xhtml` | `/virements-programmes` | tous | SC-004-01, SC-004-02, SC-004-03 | cœur bancaire |
| 5 | `/releve.xhtml` | `/releve` | tous | SC-005-01 | cœur bancaire |
| 6 | `/budget.xhtml` | `/budget` | tous | SC-006-01 | cœur bancaire |
| 7 | `/pret.xhtml` | `/prets` | tous | SC-007-01, SC-007-02 | crédits |
| 8 | `/documents.xhtml` | `/documents` | tous | SC-008-01, SC-017-02 | documents |
| 9 | `/bourse.xhtml` | `/bourse` | tous | SC-009-01 | bourse |
| 10 | `/conseiller.xhtml` | `/mon-conseiller` | tous | SC-010-01 (SC-010-02 exclu) | open banking et conseiller |
| 11 | `/produits.xhtml` | `/produits` | tous | SC-011-01 | open banking et conseiller (catalogue) |
| 12 | `/comptes-tiers.xhtml` | `/comptes-tiers` | tous | SC-012-01 | open banking et conseiller |
| 13 | `/beneficiaires.xhtml` | `/beneficiaires` | tous | SC-013-01, SC-013-02 | cœur bancaire |
| 14 | `/conseiller-workspace.xhtml` | `/espace-conseiller` | CONSEILLER, ADMIN (CLIENT redirigé vers `/tableau-de-bord`) | SC-015-01, SC-015-02 | identité et clients (portefeuille) ; cœur bancaire (encours) ; crédits (prêts en attente) |
| 15 | `/admin.xhtml` | `/administration` | ADMIN (autres rôles redirigés vers `/tableau-de-bord`) | SC-016-01, SC-016-02 | identité et clients (clients, conseillers) ; crédits (pipeline des prêts) ; cœur bancaire (alertes fraude, file de revue des virements suspendus, journal des traitements). Le service audit n'expose aucune API de lecture (FR-026-06, AD-17) |

La file de revue des virements suspendus (FR-016-19) est une section de `/administration`, sans route propre (EXPERIENCE.md). L'écran de refus pour erreur du contrôle (FR-003-27) n'a pas d'équivalent legacy : il n'entre pas dans cette table.

### 1.2 Pages sans rendu et scénarios HTTP

| # | Page legacy | Cible | Service | Scénario | Mapping retenu en G2 |
|---|---|---|---|---|---|
| 16 | `/powens-callback.xhtml` | `GET /api/open-banking-conseiller/powens/retour` — point d'API **public** (exemption sur le chemin exact, FR-030-01), adresse de retour déclarée chez Powens ; répond toujours par une redirection 302 vers `/comptes-tiers` (avec un paramètre de résultat), sans contenu propre (FR-014-01) | open banking et conseiller | SC-014-01 | `map` : `GET /api/open-banking-conseiller/powens/retour`, `status+json_subset`, statut **302**, `json_subset` `null`. Le navigateur qui revient de Powens n'envoie pas le JWT (stockage local) : l'utilisateur est retrouvé par le paramètre `state` signé que le service a émis au départ vers Powens. |
| 17 | `/docs/download` (servlet) | `GET /api/documents/telechargement?type={RELEVE\|RIB\|ATTESTATION\|IFU}&compteId={id}` | documents | SC-017-01 | `map` : `GET /api/documents/telechargement?type=RIB&compteId=1`, statut **401** sans jeton, `json_subset` `null`. Le front traite le 401 en renvoyant vers `/login` (FR-017-01 reste vrai pour l'utilisateur). Le PDF est récupéré par le front avec le jeton, puis ouvert dans un nouvel onglet (URL d'objet) : un onglet ouvert directement sur l'API n'aurait pas le jeton. |
| — | `GET /login.xhtml` | `GET /login` (page servie par le conteneur web) | conteneur web | SC-001-01 | `map` : `GET /login`, statut **200**, `json_subset` `null`. |
| — | `GET /dashboard.xhtml` sans session (302) | `GET /api/coeur-bancaire/tableau-de-bord` sans jeton | cœur bancaire | SC-001-02 | `map` : statut **401**. Une SPA dont le jeton vit en stockage local ne peut pas répondre 302 côté serveur : `GET /tableau-de-bord` renvoie toujours 200 (`index.html`), et la redirection vers `/login` est faite par le routeur du front. Le refus serveur (401) est ce que le scénario peut encore prouver ; la redirection de l'écran est prouvée par l'étape de connexion des sessions (§2) et par les tests de la nouvelle application. |
| — | `POST /login.xhtml` (formulaire JSF, mauvais identifiants) | `POST /api/identite-clients/connexion`, corps JSON | identité et clients | SC-001-03 | `map` : `body_format: json`, `field_map` : `loginForm:username` → `identifiant`, `loginForm:password` → `motDePasse`, `loginForm` → `null`, `loginForm:j_idt11` → `null` ; statut **401**, `json_subset` `{"message": "Identifiants incorrects"}`. Le mode legacy `status+body_contains` n'est pas dérivable en `map` (seul `status+json_subset` l'est) : le texte « Identifiants incorrects » passe dans le champ `message` de l'enveloppe d'erreur de l'API (FR-001-06). |

## 2. Connexion de test

### 2.1 Comptes

Comptes du golden master legacy (`replay-config.json`, `adapters.playwright.accounts`), repris dans le bloc `browser.accounts` du mapping :

| Rôle | Identifiant | Mot de passe lu dans | Route de départ (route protégée de l'étape de connexion) | Écran d'arrivée attendu (FR-001-05) |
|---|---|---|---|---|
| ADMIN | `admin` | `RETRO_ADMIN_PASSWORD` | `/administration` | `/administration` |
| CONSEILLER | `sophie` | `RETRO_CONSEILLER_PASSWORD` | `/espace-conseiller` | `/espace-conseiller` |
| CLIENT | `user1` | `RETRO_CLIENT_PASSWORD` | `/tableau-de-bord` | `/tableau-de-bord` |

Les trois comptes existent dans les données de référence (§5), avec le mot de passe de démonstration du legacy stocké **haché** (NFR-SEC-01). Le mot de passe en clair n'est jamais dans le dépôt : il vient de la variable d'environnement au moment du rejeu.

Bloc `browser` retenu en G2 : `base_url: null`, `storage_state_dir: ".auth"` (relatif au répertoire du rejeu, ignoré par git), `accounts` ci-dessus.

### 2.2 Déroulé (fichier `auth.setup.ts` généré par `export-golden-master-tests.py`)

Pour chaque rôle utilisé : le test de connexion ouvre la route protégée du rôle ; la SPA, sans jeton, redirige vers `/login` ; il remplit le premier champ identifiant, puis le champ mot de passe, valide par la touche Entrée, attend que le champ mot de passe disparaisse, puis enregistre l'état du navigateur (`storageState` : cookies **et stockage local** de l'origine) dans `.auth/{rôle}.json`. Chaque scénario d'écran charge ensuite l'état de son rôle (`test.use({ storageState })`) : le JWT revient dans le stockage local avant le premier script de la page, la SPA est donc connectée d'emblée.

### 2.3 Ce que l'application doit fournir

- **Page `/login`** : un vrai `<form>` dont la soumission se fait aussi par Entrée dans le champ mot de passe.
  - champ identifiant : `<input type="text" name="username" data-testid="login-identifiant">`, premier champ texte de la page dans l'ordre du DOM (le test de connexion prend le premier élément qui correspond à `input[type='email'], input[name*='username' i], input[name*='login' i], …, input[type='text']`) ;
  - champ mot de passe : `<input type="password" name="password" data-testid="login-mot-de-passe">`, seul champ de ce type sur la page ;
  - bouton : `<button type="submit" data-testid="login-me-connecter">Me connecter</button>`.
- **Redirection côté front** : toute route protégée ouverte sans jeton valide affiche `/login` (FR-001-02) ; après la connexion, la SPA quitte `/login` vers l'écran d'accueil du rôle (FR-001-05), ce qui fait disparaître le champ mot de passe.
- **Jeton en stockage local** (`localStorage`, clé unique, par exemple `nb.jeton`), sur l'origine de l'URL de base. Jamais en `sessionStorage` : il ne serait pas enregistré dans `storageState`.
- **Durée de vie du jeton** au moins égale à la durée d'un rejeu complet (proposition : 60 min en profil de rejeu). L'émission (`iat`, `exp`) et la vérification du jeton utilisent **l'horloge système** des services, jamais le port Horloge : sinon un service dont l'horloge métier est réglée (§5.3) jugerait le jeton pas encore valide ou expiré.
- **Contrôle des rôles** dans chaque service (BUG-093) et dans le routeur du front (redirection silencieuse vers `/tableau-de-bord`, FR-001-11, FR-016-01, FR-030-02), à partir des rôles portés par le jeton.
- **Déconnexion** (SC-001-05) : le lien `nav-deconnexion` retire le jeton du stockage local, appelle `POST /api/identite-clients/deconnexion` (audit LOGOUT, FR-001-12) puis affiche `/login`. Si l'invalidation de FR-001-10 révoque le jeton côté serveur, SC-001-05 invalide le jeton enregistré pour CLIENT : c'est pourquoi il est rejoué dans un lancement séparé, qui refait la connexion (§7.3).

## 3. Règle des sélecteurs

- Les sélecteurs cibles sont des attributs `data-testid`, selon la convention de EXPERIENCE.md (« Attributs de test (NFR-UI-02) ») : `{écran}-{élément}[-{identifiant métier}]`, minuscules ASCII sans accent, préfixes d'écran fixés (`login`, `nav`, `dashboard`, `virement`, `vp`, `releve`, `budget`, `pret`, `documents`, `bourse`, `chat`, `produits`, `tiers`, `beneficiaires`, `conseiller`, `admin`, `revue`), zones de message `{écran}-message-erreur|succes|info`.
- La correspondance sélecteur legacy → `data-testid` est **celle de EXPERIENCE.md** (tableau sous « Attributs de test »), sans exception ni ajout ici. Elle couvre tous les sélecteurs actionnés par les scénarios applicables : SC-001-05, SC-003-02, SC-003-04, SC-004-02, SC-004-03, SC-007-02, SC-013-02 (et les champs de `/login` pour les sessions de test).
- Forme écrite dans `target.selector_map` : sélecteur CSS d'attribut, `[data-testid='virement-compte-source']`, compris à la fois par le rejeu Python et par Playwright Test (le test généré prend le premier élément qui correspond).
- Listes déroulantes : `<select>` natifs, valeurs d'option identiques au legacy, parce que le golden master sélectionne par valeur : identifiant du compte (`1`, `2`, d'où l'obligation de garder les identifiants du legacy dans les données de référence, §5) et valeur d'énumération de la fréquence (`MENSUEL`).
- Saisies reprises telles quelles : `3.5` (taux, point décimal accepté), `2027-01-01` (date au format `yyyy-MM-dd`, compatible avec `<input type="date">`), IBAN sans espace.
- Un `data-testid` publié à l'étape G2 ne change plus.

## 4. Règle des textes

- **Repris à l'identique** du legacy : libellés, casse rendue, accents absents, ponctuation, emoji, formats de nombre et de date (EXPERIENCE.md, « Voice and Tone », règle n°1). Les scénarios d'écran comparent le **texte affiché** (`innerText`) en mode `contains` : la casse produite par `text-transform` compte, puisqu'elle est rendue dans `innerText`.
- **Écarts** : la liste fermée de EXPERIENCE.md, « Écarts de texte avec le legacy » (parties A et B). D'après divergences-golden-master.md, aucun ne touche une ligne attendue, un `wait_for` ou un statut des 31 scénarios applicables : **`text_map` vide pour tous les scénarios** ; seul SC-003-04 peut diverger, par comportement, et NFR-PAR-05 (§5.3) le rend déterministe.
- **Entrée groupée des ajouts** (partie C, C1) : section « Suivi de mes virements », section « Virements suspendus — En attente de revue », lien d'évitement « Aller au contenu principal », mention « (nouvel onglet) ». Ce sont des ajouts : sans effet en mode `contains`, ils ne demandent aucune clé `text_map` (le format ne sait que remplacer ou retirer une ligne attendue). L'entrée groupée est consignée une fois, dans la justification (`rationale`) des scénarios des écrans concernés.
- **Fenêtre de 5 000 caractères** : le rejeu ne lit que les 5 000 premiers caractères du texte affiché. Toute section nouvelle se place **après** le contenu comparé dans l'ordre du DOM (section « Suivi de mes virements » en bas de `/virement` et de `/tableau-de-bord` ; file de revue après « Gestion des clients » sur `/administration`). Seul le lien d'évitement précède le contenu (26 caractères, sans effet notable).
- **Boutons** : texte visible dans un `<button>` ou un `<a>`, pas dans `value` d'un `<input type="submit">` ; le test Playwright généré ne lit pas la valeur des `<input>` dans le texte comparé.

## 5. Données de référence et remise à zéro

### 5.1 Jeu de données par service (équivalent de `import.sql`, NFR-PAR-04)

Un fichier par service, `services/{identifiant}/reference-data/reference-data.sql`, qui reprend les lignes de `import.sql` des tables du service (153 lignes en tout), **avec les identifiants du legacy** (le golden master sélectionne les comptes `1` et `2`, télécharge le RIB du compte `1`). Les données de référence ne sont pas des migrations de schéma : elles ne vont jamais en production.

| Service | Tables du legacy (DATA-DICTIONARY.md) | Lignes `import.sql` |
|---|---|---|
| Identité et clients | `CUSTOMERS` (comptes de connexion, rôles, conseiller affecté) | 7 |
| Cœur bancaire | `BANK_ACCOUNTS`, `TRANSACTIONS`, `TRANSFERS`, `RECURRING_TRANSFERS`, `EXTERNAL_BENEFICIARIES`, `FRAUD_ALERTS`, `BUDGETS` | 7 + 45 + 0 + 3 + 5 + 6 + 6 |
| Crédits | `LOANS`, `LOAN_REPAYMENTS`, `CREDIT_SCORES`, `RISK_PROFILES` | 2 + 18 + 4 + 4 |
| Documents | `DOCUMENTS` | 5 |
| Notifications | `NOTIFICATIONS` | 6 |
| Audit | `AUDIT_LOGS` | 0 |
| Bourse | `WATCHLIST_ITEMS` | 9 |
| Open banking et conseiller | `BANK_PRODUCTS`, `SAVINGS_PRODUCTS`, `CREDIT_PRODUCTS`, `INVESTMENT_PRODUCTS` (catalogue) | 13 + 4 + 4 + 5 |
| (aucun) | `BATCH_JOB_LOGS` : table technique, pas de donnée de référence ; chaque service garde l'historique de ses propres traitements | — |

Les références entre services (`CUSTOMER_ID`, `ACCOUNT_ID`, `ACCOUNT_NUMBER`) sont des identifiants copiés, sans clé étrangère entre bases. Données que le golden master exige en particulier : soldes et 5 dernières opérations du compte COURANT de `user1`, bénéficiaire `FR76***` (SC-013-01), portefeuille de `sophie` (Jean Dupont / `user1`, Marie Lebrun / `user2`, SC-015-01), watchlist `BNP.PA` et `AI.PA` (SC-009-01), produits « CEL - Compte Epargne Logement » et « Livret A » (SC-011-01), compte interne `FR76-0001-0002` (SC-004-02). Les dates relatives éventuelles d'`import.sql` sont calculées à partir du port Horloge, jamais de `now()` de la base.

### 5.2 Commandes

| Commande | Effet |
|---|---|
| `make reset-data` (appelle `scripts/reset-reference-data.sh`) | Remise à zéro **globale** et chargement, idempotents (AD-11 de la spine) : arrête les consommateurs Kafka ; pour chaque service, vide toutes ses tables (`TRUNCATE … RESTART IDENTITY CASCADE`), y compris les projections, `outbox`, `outbox_audit` et `evenements_traites` ; supprime puis recrée les topics (offsets des groupes effacés avec eux) ; recharge `reference-data.sql` (projections comprises, générées depuis les données du producteur par `scripts/generer-projections-reference.sh`) et replace les séquences au-dessus du plus grand identifiant ; vide les caches métier éventuels ; relance les consommateurs. Sert aussi de commande de chargement initial (NFR-PAR-04 : « chargé par une commande », « une commande de remise à zéro le rétablit »). Code 0 = les 8 bases sont à l'état de référence. |
| `make rejeu-up` | Démarre l'ensemble (`docker compose -f compose.yaml -f compose.rejeu.yaml up -d`) en **profil de rejeu**, puis lance `make reset-data`. |
| `make rejeu-golden-master` (appelle `scripts/rejeu-golden-master.sh`) | Rejeu complet ordonné selon NFR-PAR-05 (§7.3). |

### 5.3 Profil de rejeu (NFR-PAR-05)

- Profil Spring `rejeu` sur les 8 services (`compose.rejeu.yaml`).
- **Port Horloge** : tout le temps métier passe par lui (aucun appel direct à l'horloge système dans le domaine). En profil `rejeu`, l'horloge est **réglée au démarrage puis avance normalement** : `make rejeu-up` lit la date de référence (`HORLOGE_DEPART`, valeur par défaut ci-dessous), calcule **un seul** décalage (`HORLOGE_DECALAGE` = date de référence − heure réelle) et le passe à tous les services, qui partagent donc la même heure métier. Une horloge figée est exclue (RAPID_SUCCESSION levé à tort).
- **Date de référence** : `2026-11-17T10:00:00` Europe/Paris (mardi, jour ouvré ; entre 06:00 et 23:00 ; antérieure au `2027-01-01` saisi comme première exécution par SC-004-02). **Réglé** (story 10.2) : `scripts/golden-master/verifier-date-reference.sh` vérifie, sur les données chargées, que toutes les dates sont antérieures à `HORLOGE_DEPART` (hors dates planifiées : `loans.date_fin`, `loan_repayments.date_echeance`, `virements_programmes.prochaine_execution`, `virements.heure_butoir`) et que `user1` a 8 opérations au plus ce jour-là (0 avec l'import). `TZ=Europe/Paris` dans chaque conteneur.
- **Navigateur du rejeu** : Playwright avec `timezoneId: 'Europe/Paris'` et `locale: 'fr-FR'` (AD-11, AD-23). **Réglé** (story 10.2) : le `playwright.config.ts` généré ne les porte pas et n'est pas modifié ; `scripts/rejeu-golden-master.sh` exporte `TZ=Europe/Paris` et `LANG=fr_FR.UTF-8` avant chaque lancement du runner.
- **Traitements planifiés** métier (virements programmés, fraude nocturne, intérêts, relevés, soldes faibles, rejet automatique des virements suspendus) : **désactivés** en profil `rejeu`, pour qu'aucune exécution ne modifie les soldes pendant le rejeu. Le relais de l'outbox, traitement technique, reste actif (AD-18).
- **Fournisseurs externes** : adaptateurs simulés (§6).
- **Remise à zéro** : avant le rejeu complet et avant chaque scénario qui écrit (§7.3). Elle ne touche pas l'horloge, qui continue d'avancer depuis la date de référence (un rejeu complet reste bien en deçà de 23:00).

## 6. Bouchons des fournisseurs externes

Architecture hexagonale : chaque fournisseur est derrière un port, avec un adaptateur réel et un **adaptateur simulé** sélectionné par le profil `rejeu` (et par les tests). Les simulateurs vivent dans le service, sans appel réseau, avec des réponses fixes (A-G-09).

| Fournisseur | Service | Scénarios concernés | Comportement du simulateur |
|---|---|---|---|
| Powens (open banking, OAuth2 PSD2) | open banking et conseiller | SC-012-01 (page Comptes Tiers : banques proposées) ; SC-014-01 (retour sans paramètre : aucune sollicitation de Powens, redirection 302) | Liste fixe de connecteurs (dont « BNP Paribas », « Credit Agricole »), comptes et transactions fixes ; modes d'erreur (refus, 5xx, solde absent) pour les tests des bugs corrigés FR-012-07, FR-012-12, FR-012-14, FR-014-04. |
| AlphaVantage (cotations) | bourse | SC-009-01 (« BNP.PA », « AI.PA », variation et performance sur la période) | Séries de cours fixes par symbole de la watchlist de référence ; dates calculées depuis le port Horloge. |
| Azure OpenAI (chat « Mon Conseiller ») | open banking et conseiller | SC-010-01 (en-tête et message de bienvenue, sans appel) ; SC-010-02 **exclu** du golden master (BUG-038) | Réponse fixe ; mode « refus d'authentification » (HTTP 401 et 403) exigé par A-G-09 pour le test de la nouvelle application qui prouve la correction de BUG-038 (texte B-4 de EXPERIENCE.md, FR-010-09). Ce test appartient à la suite de la nouvelle application, pas au golden master. |

Le legacy n'envoie ni courriel ni SMS (notifications dans l'application seulement) : aucun autre bouchon. Les échanges internes (Kafka, ex-files JMS `virements` et `pretDemandesQueue`) ne sont pas simulés : ils tournent pour de vrai pendant le rejeu. La notification CONFIRMATION de SC-003-04 traverse Kafka avant d'apparaître sur `/tableau-de-bord` : son délai doit rester sous l'attente du test (5 s par défaut).

## 7. Commandes de preuve

### 7.1 Dérivation à blanc (complétude du mapping)

Une fois `replay-mapping.json` (racine du dépôt) rempli par G2 :

```bash
uv run docs/retro/tools/derive-golden-master.py derive --gm-dir docs/retro/golden-master \
  --mapping replay-mapping.json --output-dir "$(mktemp -d)"
```

Attendu : **code de sortie 0**, `errors: []`, `derived` = 31, `dropped` = 2 (SC-003-03, SC-010-02, motif « corrigé dans le forward »), `rules.lost` = [RG-010-007]. Cette règle sort du golden master avec l'exclusion de SC-010-02, qui figeait le bug corrigé BUG-038. Elle est couverte par un test nouveau, celui de FR-010-09 (voir `ecarts-voulus-golden-master.md`). Code 1 = décision manquante ou invalide (`errors` la nomme) ; code 2 = registre des bugs ou fichier illisible.

Puis, après la dérivation réelle (`--output-dir docs/retro/golden-master/derived --force`) et la génération des tests :

```bash
uv run docs/retro/tools/export-golden-master-tests.py --runner playwright \
  --derived docs/retro/golden-master/derived --mapping replay-mapping.json \
  --gm-dir docs/retro/golden-master --output e2e/golden-master --check
```

Attendu : code 0 (tests générés à jour, non retouchés à la main).

### 7.2 Déterminisme de SC-003-04 (NFR-PAR-05)

1. **Aucun accès direct à l'horloge système dans le code métier** (seul l'adaptateur Horloge y a droit) :

   ```bash
   ! grep -rnE '\b(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime|OffsetTime|Year|YearMonth)\.now\(|Clock\.system|System\.currentTimeMillis\(' \
       services/*/src/main --include=*.java | grep -vE '/adapter/out/(horloge|jeton)/'
   ```

   (Même expression que AD-19 de la spine. La même règle est tenue par le test ArchUnit `HorlogeArchitectureTest` de chaque service, qui interdit aussi toute dépendance à `java.time.Clock` ; le jeton JWT, §2.3, est la seule exception, dans `adapter.out.jeton`. Côté front, AD-23 interdit `Date.now()` et `new Date()` sans argument dans `src/domain` et `src/application`.)
2. **Répétabilité** : trois rejeux successifs, chacun après remise à zéro, tous verts, à n'importe quelle heure réelle :

   ```bash
   make rejeu-up
   for i in 1 2 3; do
     make reset-data && (cd e2e/golden-master && npx playwright test --grep "SC-003-04") || exit 1
   done
   ```
3. **Contre-épreuve** : la même séquence, démarrée par `HORLOGE_DEPART=2026-11-17T23:30:00 make rejeu-up`, doit faire **échouer** SC-003-04 (virement suspendu, UNUSUAL_HOURS, FR-027-02). Elle prouve que c'est bien l'horloge réglée, et non l'heure réelle, qui décide.

### 7.3 Rejeu complet ordonné (`scripts/rejeu-golden-master.sh`)

Les tests générés ne contiennent pas de remise à zéro et ne doivent pas être édités. Le script enchaîne donc des lancements du runner, depuis `e2e/golden-master/` :

1. `make reset-data`, puis `npx playwright test --grep-invert "<scénarios qui écrivent>" --reporter=blob` ;
2. pour chaque scénario qui écrit (liste lue dans les `scenario.json` dérivés, `writes: true` : SC-001-03, SC-001-05, SC-003-04, SC-004-02, SC-013-02) : `make reset-data`, puis `npx playwright test --grep "<SC>" --reporter=blob` ; chaque lancement refait la connexion des rôles (projet `connexion`), ce qui isole aussi la déconnexion de SC-001-05 ;
3. `npx playwright merge-reports --reporter=html,json ./blob-report` : un seul rapport. Critère de bascule : 31 réussis, 0 en échec, 2 non rejoués.

## 8. Points tranchés en G2 et points encore ouverts

Statut au 2026-10-09 : les points 1 à 8 sont **réglés** (G2 du 2026-10-05 et AD-17 de la spine) ; ils restent listés pour mémoire. Les points 9 et 10, ainsi que la configuration `timezoneId`/`locale` du §5.3, sont **réglés** par la story « Socle du rejeu final du golden master » (10.2) ; le point 11 est décidé (Q1 (a)).

Décisions globales (en tête de `replay-mapping.json`) — réglées :

1. Confirmer `target_base_url` = `http://localhost:8080/`, `auth` = `null`, et le bloc `browser` (§2.1).
2. Confirmer les 14 clés `routes` (§1.1) ; l'architecture les publie, la confirmation G2 les fige.

Décisions par scénario — réglées (SC-001-02 et SC-017-01 adaptés en 401, SC-001-03 en 401 + `{"message": "Identifiants incorrects"}`, SC-001-01 et SC-014-01 conservés, SC-003-03 et SC-010-02 exclus ; registre `ecarts-voulus-golden-master.md`) :

3. **5 scénarios HTTP**, chacun `map` ou `drop` justifié, avec les propositions du §1.2 :
   - SC-001-01 (`GET /login` → 200) ;
   - SC-001-02 (accès protégé sans session : statut 401 de l'API au lieu du 302 de page, ou `drop` si l'équipe estime que la redirection d'écran est suffisamment prouvée ailleurs) ;
   - SC-001-03 (connexion refusée : 401 et `{"message": "Identifiants incorrects"}`, mode `status+body_contains` non dérivable en `map`) ;
   - SC-014-01 (retour Powens : 302 conservé) ;
   - SC-017-01 (téléchargement sans session : 401 au lieu de 302).
4. **26 scénarios d'écran** : `map` sur la route de leur page ; `selector_map` pour les 7 qui actionnent des éléments (SC-001-05, SC-003-02, SC-003-04, SC-004-02, SC-004-03, SC-007-02, SC-013-02), d'après la table de EXPERIENCE.md ; `text_map` vide ; mode `contains` repris du legacy.
5. **SC-003-04** : `map` sans `text_map`, avec dans la justification la condition NFR-PAR-05 (profil de rejeu, remise à zéro avant le scénario) ; l'écart permanent n'est retenu que si le profil de rejeu n'est pas livré.
6. **Entrée groupée C1** : consigner les ajouts dans la justification des scénarios des écrans concernés (Virement, Tableau de bord, Administration, Documents, lien d'évitement partout), puisque `text_map` ne représente pas un ajout.
7. **SC-003-03 et SC-010-02** : `drop` prérempli (bugs corrigés), à laisser tel quel.

Points hors mapping :

8. Enveloppe d'erreur des API (champ `message`), dont dépend le `json_subset` de SC-001-03 : **réglé**, format unique `{"message"}` (+ `reference`) de AD-17, décidé le 2026-10-05.
9. **Réglé (story 10.2)** : la date de référence du profil de rejeu est postérieure aux dates d'`import.sql` (hors dates planifiées) et `user1` y a 8 opérations au plus ; contrôle rejouable par `scripts/golden-master/verifier-date-reference.sh` (code non nul en nommant base, table, colonne et date).
10. **Réglé (story 10.2)** : remise à zéro avant chaque scénario qui écrit par `scripts/rejeu-golden-master.sh` (§7.3), qui lit la liste `writes: true` dans les `scenario.json` dérivés, lance `make reset-data` (`scripts/reset-reference-data.sh`) avant les lectures puis avant chaque scénario qui écrit, et fusionne les rapports blob ; les tests générés restent intouchés. Une option de l'outil d'export permettrait encore un seul lancement, sans être nécessaire.
11. FR-001-10 (invalidation à la déconnexion) avec un JWT sans état : **décidé** (Q1 (a) du 2026-10-05, AD-16) — un jeton reste valable jusqu'à son expiration, sans révocation immédiate ; à la déconnexion, le jeton est supprimé côté front, sa durée de vie est courte, sans liste de révocation. Repris comme critère de la story de FR-001-10. Le rejeu tient grâce au §7.3.
