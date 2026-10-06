# Epic 2 Context: Se connecter et accéder à son espace selon son rôle

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Permettre à un client, un conseiller ou un administrateur de se connecter sur `/login` avec identifiant et mot de passe haché, d'arriver sur l'écran d'accueil de son rôle, de naviguer dans un gabarit commun filtré par rôle et de se déconnecter, chaque connexion, échec et déconnexion étant tracé avec l'adresse IP d'origine. Les services refusent eux-mêmes les appels sans jeton ou au rôle insuffisant. L'epic pose les socles transverses dont dépendent tous les epics suivants : JWT, format d'erreur `{"message"}`, résolution de l'adresse IP, audit par outbox, thème généré depuis DESIGN.md, CSP et contrôle axe du conteneur web, `make secrets`, publication de l'état des clients (`identite-clients.client-modifie.v1`) et script `scripts/golden-master/verifier-scenario.sh`.

## Stories

- Story 2.1: Se connecter par l'API du service identité et clients
- Story 2.2: Tracer les connexions dans le journal d'audit avec l'adresse IP d'origine
- Story 2.3: Se connecter depuis l'écran de connexion au thème Demo Bank
- Story 2.4: Naviguer dans son espace selon son rôle
- Story 2.5: Se déconnecter et retrouver la connexion quand la session expire
- Story 2.6: Faire appliquer les rôles par les services eux-mêmes
- Story 2.7: Publier l'état des clients pour les autres services

## Requirements & Constraints

- Mots de passe stockés et vérifiés hachés (`{bcrypt}`) ; les hachés des données de référence sont générés par script depuis `RETRO_ADMIN_PASSWORD`, `RETRO_CONSEILLER_PASSWORD`, `RETRO_CLIENT_PASSWORD`, jamais en clair dans un fichier versionné. Les 7 utilisateurs du legacy sont repris (`admin`, `sophie`, `user1` « Jean Dupont »...).
- Échec d'authentification indistinct : compte inexistant, mauvais mot de passe et compte désactivé donnent tous 401 `{"message":"Identifiants incorrects"}`. Champs vides : 400 « Identifiant requis » / « Mot de passe requis ».
- Pas de verrouillage de compte ni de nouvelle politique de mot de passe (comportement legacy conservé).
- Aucune réponse publique ne pose de cookie ni ne divulgue de jeton ; deux connexions successives donnent des `jti` différents.
- Session : 30 min (60 min en profil `rejeu`), renouvelée sur activité quand il reste moins de 10 min ; sans activité, expiration et retour à `/login`. Déconnexion sans liste de révocation : le jeton reste valide jusqu'à son `exp` (risque accepté). Un client désactivé ne peut ni se connecter ni renouveler.
- Audit : codes `LOGIN` « Connexion réussie », `LOGIN_FAILED` « Tentative échouée » (identifiant saisi), `LOGOUT` « Déconnexion », `ADMIN_DEACTIVATE` / `ADMIN_ACTIVATE` « Désactivation|Activation client id={customerId} » ; écrit même si l'opération est annulée ; horodatage = enveloppe (Horloge du producteur), jamais l'heure de réception. Aucun écran ni API ne lit le journal.
- Messages d'erreur sans pile, nom de classe ni code interne ; textes génériques de l'API : 401 « Authentification requise », 403 « Accès refusé », 404 « Ressource introuvable », technique « Une erreur technique est survenue ».
- Journaux : jamais de mot de passe ni de jeton ; `traceId` et `spanId` sur chaque ligne.
- Secrets uniquement par variables d'environnement ; le service refuse de démarrer sans clé privée ; aucune clé par défaut.
- Chaque bug legacy corrigé (BUG-001, -002, -003, -083, -092, -093) est couvert par un test tagué ; les tests sont tagués par exigence pour être lancés par `-Dgroups` / `vitest -t`.
- Scénarios golden master concernés : SC-001-01 à SC-001-05 (HTTP couverts par tests d'intégration et `curl` ; SC-001-04 et SC-001-05 vérifiés statiquement par `verifier-scenario.sh`). Ne jamais rejouer le golden master.
- Fin de chaque story : `build.sh`, `check-gates-exist.sh`, `trace-smoke.sh`, `golden-master.sh` en code 0. L'empreinte des contrôles est reportée : l'epic démarre sans.

## Technical Decisions

- **Services** : `identite-clients` (port 8081, possède `CUSTOMERS`, seul détenteur de la clé privée JWT) et `audit` (8086, possède `AUDIT_LOGS`, seul à l'écrire, aucune API HTTP métier, `/api/audit/**` non routé par le conteneur web). Hexagone `domain/application/adapter.in/adapter.out/infrastructure`, une base PostgreSQL par service (`postgres-<id>`, volume `pgdata-<id>`, Flyway), healthcheck sur chaque conteneur compose.
- **JWT** : `POST /api/identite-clients/connexion` vérifie via `DelegatingPasswordEncoder`, signe RSA par `NimbusJwtEncoder` ; paire RSA générée par `make secrets` dans `.secrets/`. Revendications `sub`, `roles` (CLIENT, CONSEILLER, ADMIN), `jti`, `iat`, `exp`, calculés par une `java.time.Clock` système dans `adapter.out.jeton` (pas le port Horloge). Points : `/connexion`, `/moi`, `/renouvellement`, `/deconnexion`. Pas de passerelle, BFF ni Authorization Server.
- **RBAC** : tout `/api/**` authentifié par défaut ; chemins publics comparés en entier (`POST /api/identite-clients/connexion`, `POST /api/coeur-bancaire/temoin`, `GET /api/open-banking-conseiller/powens/retour`). Chaque service vérifie le jeton par `spring-boot-starter-security-oauth2-resource-server` (clé publique par configuration) et contrôle le rôle aussi dans le cas d'usage `application`. 401/403 sans redirection serveur. Une classe `*SecuriteIT` par service.
- **Erreurs** : format unique `{"message": "..."}` (+ `reference` si l'écran l'affiche). API en JSON camelCase français, préfixe `/api/{identifiant}/`.
- **Adresse IP** : nginx et proxy Vite écrasent `X-Forwarded-For` avec `$remote_addr` (jamais `$proxy_add_x_forwarded_for`) ; le service ne le retient que si l'adresse distante est dans `nb.reseau.mandataires-de-confiance` (`172.30.0.0/24`, supposition) ; résolution unique dans `adapter.in.http`.
- **Kafka / outbox** : topic `{producteur}.{evenement}.v{n}`, clé = identifiant numérique, enveloppe `idEvenement`, `type`, `version`, `horodatage` (UTC, Horloge), `producteur`, `donnees` ; `StringSerializer`, sans en-tête `__TypeId__` ; topics déclarés par bean `NewTopic` (3 partitions, réplication 1) dans `infrastructure`, auto-création désactivée. Ligne `outbox` dans la même transaction que l'écriture, publiée après validation dans le même fil, relais planifié chaque seconde dans l'ordre des identifiants. Transactions via le port `application.port.out.Transaction` (avec `executerIndependamment(...)` pour l'audit, table `outbox_audit`) ; aucune annotation transactionnelle dans `application`.
- **Audit** : topic `identite-clients.audit.v1`, `donnees` = `utilisateur`, `action` (catalogue fermé ; code inconnu rejeté et journalisé), `detail`, `adresseIp` ; clé = identifiant utilisateur. Consommateur idempotent via `evenements_traites` (écouteur unitaire), `group.id` = `audit`.
- **Projection clients** : `identite-clients.client-modifie.v1` porte l'état complet (`clientId`, `login`, `nomComplet`, `role`, `actif`, `conseillerId`, `dateCreation`, `supprime`, `versionAgregat` strictement croissant), clé `clientId`, consommé plus tard par `coeur-bancaire`, `credits`, `documents`.
- **Contrats** : `contrats/evenements/<topic>.schema.json` avec exemple, écrit par le producteur ; tests `*ContratEvenement*Test` côté producteur (champs exacts) et consommateur (désérialise l'exemple).
- **Front** : jeton en `localStorage` clé `nb.jeton` (jamais `sessionStorage`), envoyé en `Authorization: Bearer` ; tout 401 de tout service → suppression du jeton et `/login` sans message. Le calcul de temps du renouvellement vit dans `src/adapters` (seule exception à l'interdiction de `Date.now()`). Routeur `react-router` 8.4.0 (pas `react-router-dom`).
- **Thème** : pas de Tailwind ; `npm run tokens` = `npx @google/design.md export --format css-tailwind` puis `frontend/scripts/tokens-root.mjs` → `src/adapters/ui/theme/tokens.css` (généré, versionné, sans `@theme`) ; aucune couleur en dur hors tokens ; éléments natifs stylés seulement par leur composant. Police Inter seule, auto-hébergée (pile Inter, Arial, sans-serif), jamais Google Fonts.
- **Conteneur web** : nginx 8080, `/api/{id}/**` vers le service, repli SPA vers `index.html` ; CSP limitant scripts, styles et polices à l'origine ; test axe-core (version consignée dans `versions-verifiees.md` avant usage).
- Versions : uniquement celles de `versions-verifiees.md` (Spring Boot 4.1.1, Testcontainers 2.x `testcontainers-postgresql`/`-kafka`, TypeScript 5.9.3, vitest 5.0.3...).

## UX & Interaction Patterns

- `/login` : titre d'onglet « BankApp Demo - Connexion », « DB », « Demo Bank », « Espace client — Connexion sécurisée », vrai `<form>` soumis par Entrée avec `login-identifiant` (`name="username"`, premier champ texte), `login-mot-de-passe`, bouton `login-me-connecter` « Me connecter » ; bandeau legacy « Système Legacy » supprimé. Erreurs de champ sous le champ (`aria-describedby`, `aria-invalid`, focus sur le premier en erreur, aucun appel API) ; refus serveur dans `login-message-erreur` (`role="alert"`) : « Identifiants incorrects » / « Vérifiez votre login et mot de passe ». Bouton désactivé avec `aria-busy` pendant l'envoi, pas de double soumission.
- Lien d'évitement « Aller au contenu principal », premier élément au `Tab` sur toute page, visible au focus ; `index.html` en `lang="fr"`.
- Accueil par rôle : ADMIN → `/administration`, CONSEILLER → `/espace-conseiller`, CLIENT → `/tableau-de-bord` ; `/` et toute route non déclarée renvoient à l'accueil du rôle (ou `/login` sans jeton) ; route réservée demandée par un rôle insuffisant → `/tableau-de-bord` sans message, contenu jamais rendu.
- En-tête (72px) : logo à gauche, liens du legacy dans l'ordre et l'orthographe d'origine (sans accents : « Virements programmes », « Releve », « Prets »...), « Bonjour, {nom complet} » (lu via `/moi`) et « Deconnexion » (`nav-deconnexion`) à droite ; liens `nav-*`, lien courant en `aria-current="page"` souligné 2px primary ; « Espace Conseiller » pour CONSEILLER et ADMIN, « Administration » pour ADMIN ; badge de rôle. Pas de hamburger : les liens passent sur deux lignes sous 1 280 px. Zone de contenu en `aria-busy` jusqu'à la réponse de `/moi`, sans texte de chargement.
- Sous-en-tête (fond primary, 48px) sur `/espace-conseiller` et `/administration` seulement.
- Motifs interdits (à rappeler dans le README du front) : défilement infini, glisser-déposer, action visible au seul survol, plus d'une modale, plus de deux niveaux de navigation, contenu interactif dans un tooltip.
- Textes repris du legacy ou de `textes-nouveaux.md`, jamais inventés ; `data-testid` `{écran}-{élément}` figés ; toute section nouvelle après le contenu comparé.

## Cross-Story Dependencies

- Prérequis : epic 1 terminé (socle des deux services, flux témoin tracé, contrôles).
- 2.1 précède toutes les autres ; 2.2 et 2.3 dépendent de 2.1 ; 2.4 de 2.1 et 2.3 ; 2.5 de 2.2 et 2.4 ; 2.6 de 2.1 et 2.2 ; 2.7 de 2.6.
- 2.6 ajoute la vérification du jeton à `coeur-bancaire` (`GET /api/coeur-bancaire/tableau-de-bord` → 401) sans casser le témoin public ni `trace-smoke.sh` ; l'écran « Gestion des clients » viendra en epic 9 (seule l'API de désactivation/activation est livrée ici).
- 2.4 livre `scripts/golden-master/verifier-scenario.sh` (hors `scripts/gates/`, sans modifier le dérivé, les tests générés ni le mapping), réutilisé à chaque epic suivant ; les textes non trouvés vont dans `reports/golden-master/textes-non-trouves.md` sans faire échouer le script.
- Les routes `/tableau-de-bord`, `/espace-conseiller`, `/administration` sont déclarées ici ; leur contenu arrive dans les epics suivants. L'alerte d'échecs répétés (MULTIPLE_FAILED_AUTH, story 3.10) s'appuiera sur les échecs de connexion.
- `identite-clients.client-modifie.v1` alimente les projections clients des epics coeur bancaire, crédits et documents.
