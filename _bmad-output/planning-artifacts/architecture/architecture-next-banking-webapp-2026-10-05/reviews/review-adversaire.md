---
review: adversaire
target: ARCHITECTURE-SPINE.md (brouillon du 2026-10-05, AD-1 à AD-21)
reviewer: relecteur adverse (reviewer gate)
date: 2026-10-05
method: "construire deux unités d'un niveau en dessous (epics ou équipes) qui respectent chaque AD à la lettre et construisent pourtant de façon incompatible"
inputs:
  - ARCHITECTURE-SPINE.md
  - prds/prd-next-banking-webapp-2026-10-05/prd.md (lecture ciblée)
---

# Revue adverse de la spine next-banking-webapp

## Verdict

**À reprendre avant le passage en final (changements demandés).**

La spine est solide là où elle a été pensée en détail : le virement (AD-12 : machine d'états, mise à jour conditionnelle, outbox, notification unique) et les contrôles qualité (AD-1 à AD-10). Elle est en revanche **muette sur tout ce qui traverse deux services hors du virement** : forme des événements, contenu des projections locales, propriétaire d'une règle quand deux services la touchent, temps métier côté front. AD-4 impose les projections locales sans dire qui publie quoi, avec quels champs, ni comment une projection est remise à zéro. Résultat : 15 paires d'unités qui respectent toutes les AD et construisent pourtant de façon incompatible.

| Sévérité | Nombre | Constats |
| --- | --- | --- |
| Critical | 2 | ADV-01, ADV-02 |
| High | 7 | ADV-03 à ADV-09 |
| Medium | 5 | ADV-10 à ADV-14 |
| Low | 2 | ADV-15, ADV-16 |

Les deux constats critiques sont des contradictions directes : la spine contre le PRD (ADV-01), et la spine contre son propre objectif de rejeu déterministe (ADV-02). Les high se ramènent à une cause commune : **il manque une AD « contrats d'événements et projections »** (ADV-03), dont dépendent ADV-04 à ADV-08.

---

## Critical

### ADV-01 — Audit : même transaction (AD-12) contre transaction indépendante (FR-026-04)

- **Paire :** epic Virements (cœur bancaire) contre epic Audit trail (audit + chaque producteur, dont l'équipe Connexion d'identité et clients).
- **Divergence :** AD-12 impose d'écrire les entrées d'audit dans l'outbox, **dans la même transaction que le virement**. FR-026-04 exige l'inverse : « chaque entrée d'audit dans une transaction indépendante de celle de l'opération : l'entrée est conservée même si l'opération est annulée ». L'équipe Virements suit AD-12 : si la validation échoue (FR-023-02), l'entrée VIREMENT disparaît avec le rollback. L'équipe Audit, qui écrit le test d'acceptation de FR-026-04, attend l'entrée. Les deux respectent un texte qui fait foi pour elles, et le test FR-026-03 « six parcours, exactement les entrées attendues » ne peut pas passer chez les deux. Même trou côté Connexion : LOGIN_FAILED n'a aucune écriture métier, donc AD-18 (« outbox pour toute publication liée à une écriture ») laisse le choix entre une outbox sans écriture et un `KafkaTemplate.send` direct. Deux équipes, deux choix.
- **AD à resserrer :** AD-12 et AD-18 (nouvelle puce « Audit »), à arbitrer par le décideur.
- **Rule proposée :** « Une entrée d'audit est toujours publiée par l'outbox, jamais par un envoi direct. Sa ligne `outbox` est écrite dans une **transaction indépendante**, ouverte par `Transaction.executerIndependamment(...)` (port `application.port.out.Transaction`) **avant** la transaction métier, et validée quelle que soit l'issue de celle-ci (FR-026-04). Une action sans écriture métier (LOGIN_FAILED, LOGOUT) n'écrit que cette ligne. AD-12 est corrigée en ce sens : la décision et la remise au traitement asynchrone restent dans la transaction du virement, l'audit n'y est plus. » Si le décideur préfère garder l'audit dans la transaction métier, c'est FR-026-04 qui doit être amendée. Dans les deux cas, une seule des deux phrases survit.

### ADV-02 — Temps métier du front : horloge du navigateur contre port Horloge en rejeu

- **Paire :** front (écrans Budget, Documents, Relevé, Virements programmés) contre services (cœur bancaire, documents) en profil `rejeu`.
- **Divergence :** AD-19 lie « tous les services » ; le front n'a aucun port Horloge. Une équipe front qui calcule le mois proposé par défaut au budget (FR-006-02), les années IFU N-1 et N-2 (FR-008-06), les 12 derniers mois de relevés (FR-008-05) ou la date minimale d'un virement programmé (FR-004-04) lit `new Date()` dans `src/domain` : c'est du TypeScript pur, autorisé par AD-2. En rejeu, les services vivent le 2026-11-17 (`HORLOGE_DEPART`) et le navigateur à la date réelle : le front propose octobre quand le service répond pour novembre. Le golden master devient faux ou dépend du jour du rejeu, ce qui contredit NFR-PAR-05 et la contre-épreuve de AD-11. Même trou pour le fuseau : un formateur qui lit `getHours()` dépend du fuseau de la machine Playwright, alors que les conteneurs sont à `TZ=Europe/Paris`.
- **AD à resserrer :** AD-19 (étendue au front) et AD-11 (configuration Playwright).
- **Rule proposée :** « Le front ne lit jamais l'heure du navigateur pour une valeur métier : `Date.now()`, `new Date()` sans argument et `Temporal.Now` sont interdits dans `frontend/src/domain` et `frontend/src/application` (règle dependency-cruiser ou ESLint `no-restricted-syntax`, bloquante). Toute date métier dont un écran a besoin (date du jour, mois courant, années IFU, liste des mois de relevés) est fournie par le service propriétaire dans sa réponse (champ `aujourdhui` en `yyyy-MM-dd`, calculé par son port Horloge en Europe/Paris). Les formateurs de `src/domain` convertissent un instant UTC par `Intl.DateTimeFormat('fr-FR', { timeZone: 'Europe/Paris' })`, jamais par le fuseau du navigateur. `e2e/golden-master` fixe `timezoneId: 'Europe/Paris'` et `locale: 'fr-FR'` dans la configuration Playwright. Seule exception : le renouvellement du jeton (AD-16), qui lit `exp` contre l'horloge du navigateur. »

---

## High

### ADV-03 — Forme des événements : `donnees` n'a ni propriétaire ni contrat

- **Paire :** tout couple producteur / consommateur, par exemple identité et clients (producteur des clients) contre cœur bancaire et crédits (consommateurs), ou cœur bancaire contre documents.
- **Divergence :** AD-18 fixe l'enveloppe mais pas `donnees`. Le producteur publie `{"clientId": 12, "actif": "true"}`, le consommateur attend `{"idClient": "12", "estActif": true}`. Chacun a son test Testcontainers vert (AD-18 n'exige que l'idempotence), et l'intégration casse au premier déploiement. Rien ne dit non plus si un événement porte l'état complet de l'agrégat (*event-carried state transfer*) ou seulement le delta, ni quelle clé désigne un compte (`ACCOUNT_ID` ou `ACCOUNT_NUMBER`, les deux étant cités par AD-14), ni comment le consommateur écarte un événement plus ancien que l'état qu'il détient. Or le relais planifié de AD-18 peut republier une ligne ancienne **après** une ligne plus récente déjà publiée par le fil de la requête : l'ordre par clé n'est plus garanti.
- **AD à créer :** AD-22 « Contrats d'événements ».
- **Rule proposée :** « Chaque topic a un schéma JSON `contrats/evenements/<topic>.schema.json`, écrit et versionné par le **seul** service producteur ; un changement incompatible crée `v{n+1}`. Les événements consommés pour une projection portent l'**état complet** de l'agrégat (pas de delta) et un entier `versionAgregat` croissant ; la clé du message et `donnees.id` sont l'identifiant numérique (`compteId`, `clientId`…), jamais le numéro de compte. Un consommateur n'applique un événement que si `versionAgregat` est strictement supérieur à celui qu'il détient (sinon ignoré, en plus de `evenements_traites`). Chaque producteur a un test qui valide ses messages contre le schéma ; chaque consommateur a un test qui désérialise un exemple conforme au schéma. Preuve : `scripts/gates/contrats-evenements.sh` (à livrer, protégé par AD-10). »

### ADV-04 — Notification unique : qui décide du type, du texte et des plafonds ?

- **Paire :** cœur bancaire (virements, soldes faibles, virements programmés) et crédits (décision de prêt) contre notifications.
- **Divergence :** AD-12 dit que `virement-decide.v1` est « consommé par notifications (CONFIRMATION, ALERTE ou FRAUDE) » sans dire qui choisit le type. L'équipe Notifications le recalcule d'après le montant, avec sa propre copie des seuils 5 000 / 10 000 : un virement de 300 € **suspendu par le score** (D2) reçoit alors CONFIRMATION au lieu de FRAUDE, et les seuils `nb.fraude.*` existent en deux exemplaires qui divergent au premier réglage. L'équipe Virements, elle, publie le type. Même ambiguïté pour le texte : qui formate `{montant}` (« 100.00 », « 100,0 », « 100 ») et l'heure butoir de FR-027-04 (fuseau Paris) ? Enfin, deux plafonds se superposent : 20 par utilisateur (FR-025-04, notifications) et 20 par **compte** et par jour pour les soldes faibles (FR-022-04). Notifications ne connaît pas le compte ; si c'est elle qui compte, « Effacer tout » (FR-025-07) remet le compteur à zéro et le client reçoit plus de 20 alertes dans la journée. FR-004-12 (« une seule notification par échéance non exécutée ») pose la même question.
- **AD à resserrer :** AD-12, puce « Unique notification », et AD-18.
- **Rule proposée :** « Le service producteur décide de **tout** le contenu d'une notification : destinataire (`clientId`), `type` (CONFIRMATION, ALERTE, FRAUDE, ACCOUNT_BALANCE), `priorite`, `titre` et `message` déjà rendu (textes du legacy ou de textes-nouveaux.md, montants et dates formatés par le producteur au format du legacy, heures en Europe/Paris). Il le place dans un bloc `donnees.notification` de son événement (`coeur-bancaire.virement-decide.v1`, `credits.pret-decide.v1`, `coeur-bancaire.solde-faible-detecte.v1`, `coeur-bancaire.echeance-non-executee.v1`). Le service notifications stocke, applique le plafond **par utilisateur** (FR-025-04) et affiche ; il ne lit aucun seuil métier et ne compose aucun texte. Les plafonds et les dédoublonnages qui dépendent d'un autre agrégat que l'utilisateur (20 par compte et par jour, une notification par échéance) sont appliqués et comptés par le producteur, dans sa base ; un événement n'est publié que si la notification doit exister. »

### ADV-05 — Désactivation d'un client : projections et jetons encore valides

- **Paire :** identité et clients (FR-016-11, FR-001-07) contre cœur bancaire (FR-022-01 soldes faibles des seuls clients actifs, FR-020-02 nombre de clients actifs) et crédits.
- **Divergence :** le cœur bancaire a besoin de `actif` et de `role` des clients, et rien ne dit quel événement les porte. Deux comportements opposés respectent pourtant AD-16 : l'équipe Cœur bancaire refuse en 403 toute requête d'un client que sa projection dit inactif, tandis que l'équipe Crédits ne regarde que le jeton. Un client désactivé peut donc encore faire un virement pendant 30 minutes mais plus consulter ses prêts, ou l'inverse. Pire : AD-16 ne dit pas que `POST /api/identite-clients/renouvellement` refuse un client inactif. Un client désactivé qui reste actif sur l'écran renouvelle son jeton indéfiniment.
- **AD à resserrer :** AD-16 et AD-22 (ADV-03).
- **Rule proposée :** « Identité et clients publie `identite-clients.client-modifie.v1` (clé `clientId`, état complet : `clientId`, `login`, `nomComplet`, `role`, `actif`, `conseillerId`, `dateCreation`, `versionAgregat`), à la création, à l'activation, à la désactivation, à l'affectation d'un conseiller et à la suppression (`supprime: true`). Le renouvellement du jeton refuse un client inactif ou supprimé (401), comme la connexion. Les autres services **ne refusent pas** une requête porteuse d'un jeton valide au motif que leur projection dit le client inactif : la désactivation prend effet à l'expiration du jeton (au plus 30 minutes), ce qui est assumé [ASSUMPTION à faire confirmer]. La projection ne sert qu'aux règles métier qui citent l'indicateur actif (FR-022-01, FR-020-02). »

### ADV-06 — Portefeuille du conseiller : qui contrôle l'appartenance ?

- **Paire :** identité et clients (propriétaire de `conseiller affecté`, FR-015-02) contre cœur bancaire et crédits (soldes, comptes et prêts des cartes, FR-015-03 à FR-015-07).
- **Divergence :** AD-16 exige que chaque service vérifie « l'appartenance des ressources », mais pour un CONSEILLER l'appartenance, c'est le portefeuille, une donnée d'identité et clients. Équipe A (Cœur bancaire) : `GET /api/coeur-bancaire/conseiller/comptes?clients=3,7,9`, le front passe la liste reçue d'identité et le service la croit, ce qui permet à un conseiller de lire n'importe quel client (BUG-093 recréé). Équipe B (Crédits) : projection `clientId → conseillerId` et contrôle côté serveur. Les deux respectent la lettre de AD-4, AD-16 et AD-17. S'y ajoute la latence : une affectation récente apparaît chez identité mais pas encore dans la projection du cœur bancaire, et la carte affiche « Aucun compte » à tort (FR-015-08).
- **AD à resserrer :** AD-16, puce « appartenance ».
- **Rule proposée :** « L'appartenance d'une ressource se décide **dans le service qui détient la ressource**, d'après le jeton et sa propre projection, jamais d'après une liste d'identifiants fournie par le front : CLIENT, ressources dont `clientId = sub` ; CONSEILLER, ressources des clients dont `conseillerId = sub` dans la projection `identite-clients.client-modifie.v1` ; ADMIN, toutes. Les points d'API du conseiller déduisent le portefeuille côté serveur (`GET /api/<service>/conseiller/portefeuille`). La latence de projection est acceptée et n'est pas masquée ; en rejeu, les projections sont chargées par les données de référence (ADV-08). »

### ADV-07 — Signal MULTIPLE_FAILED_AUTH : les échecs sont chez identité, les alertes au cœur bancaire

- **Paire :** identité et clients (connexion, LOGIN_FAILED, FR-001-06, FR-001-07) contre cœur bancaire (propriétaire de `FRAUD_ALERTS`, FR-027-11, FR-027-03).
- **Divergence :** le signal naît d'un comptage qui a lieu chez identité, et l'alerte doit vivre dans `FRAUD_ALERTS`, chez le cœur bancaire. Équipe Identité : elle compte les échecs consécutifs et publie un événement au 5e. Équipe Cœur bancaire : elle consomme `identite-clients.audit.v1` (le topic d'audit, car c'est le seul qui porte LOGIN_FAILED) et compte de son côté. On obtient deux compteurs, des remises à zéro différentes (un succès remet-il à zéro ? A-027-2 dit oui) et un couplage au topic d'audit, détourné de son rôle. En plus, LOGIN_FAILED porte l'**identifiant saisi** (chaîne), alors que `FRAUD_ALERTS` attend un `CUSTOMER_ID`.
- **AD à créer :** puce dans AD-12, ou AD dédiée.
- **Rule proposée :** « Identité et clients compte les échecs consécutifs de connexion par client existant (remise à zéro au succès, échecs d'un compte désactivé compris, A-027-12) et publie `identite-clients.echecs-connexion-repetes.v1` (clé `clientId`, `nombreEchecs`) au 5e échec, puis à chaque échec suivant. Le cœur bancaire, seul propriétaire de `FRAUD_ALERTS`, crée l'alerte OPEN de score 80 à la réception. Aucun service ne consomme un topic `{service}.audit.v1` hormis le service audit. »

### ADV-08 — Projections et données de référence : `make reset-data` contre Kafka

- **Paire :** cœur bancaire (`reference-data.sql` des comptes) contre documents, crédits, notifications (projections des comptes et des clients), au moment du rejeu (AD-11).
- **Divergence :** AD-11 remet les 8 bases à l'état de référence, mais ni Kafka ni les outbox. Deux façons de remplir une projection respectent toutes deux les AD : Équipe Documents écrit sa projection des comptes en dur dans son `reference-data.sql` (copie à la main des données du cœur bancaire, qui divergera à la première modification) ; Équipe Crédits compte sur un rejeu des topics depuis l'origine, ce que `reset-data` ne déclenche pas, puisque les offsets sont déjà validés. Et les événements encore en vol d'un rejeu précédent (outbox non publiée, messages non consommés) arrivent après la remise à zéro : `evenements_traites` a été vidé, donc ils sont appliqués. Le scénario suivant n'est plus déterministe (NFR-PAR-05).
- **AD à resserrer :** AD-11, puce « Données de référence ».
- **Rule proposée :** « Les projections font partie des données de référence : le `reference-data.sql` d'un service consommateur est **généré** à partir du `reference-data.sql` du producteur par `scripts/generer-projections-reference.sh` (jamais écrit à la main), et un contrôle vérifie qu'il est à jour. `make reset-data` arrête les consommateurs Kafka, vide les tables `outbox` et `evenements_traites`, supprime puis recrée les topics (ou repositionne les offsets des groupes à la fin), recharge les 8 bases, puis relance les consommateurs. Idempotence vérifiée par trois `reset-data` successifs. »

### ADV-09 — Documents : relevé, attestation et propriétaire des PDF

- **Paire :** documents (`DOCUMENTS`, FR-008-04 à FR-008-12, FR-017) contre cœur bancaire (comptes, opérations, FEAT-020 relevés mensuels).
- **Divergence :** documents doit contrôler l'appartenance d'un compte (FR-008-04 : 403 pour un compte d'un autre client, 404 pour un compte inexistant), lister toutes les opérations d'un mois (FR-008-07), les crédits d'intérêts d'une année (FR-008-10) et le **solde courant** (FR-008-09), plus IBAN, BIC et titulaire pour le RIB. AD-4 ne laisse qu'une projection, et rien ne dit laquelle. Équipe Documents : projection complète des opérations, soit une deuxième copie du grand livre. Équipe Cœur bancaire : elle publie seulement `compte-modifie` sans les opérations. Résultat : un relevé vide. Deuxième divergence : FEAT-020 « relevés mensuels » est placé au cœur bancaire (Capability Map) alors que `DOCUMENTS` appartient à documents. Une équipe Cœur bancaire peut écrire des lignes de relevé, ou générer des PDF, dans sa propre base, ce qui donne deux propriétaires du relevé. Troisième divergence : le format d'un montant dans un PDF (legacy) et le titulaire (nom chez identité) n'ont pas de source désignée.
- **AD à resserrer :** AD-14 (ligne documents) et AD-22.
- **Rule proposée :** « Documents est le seul service qui produit un PDF et le seul propriétaire de `DOCUMENTS`. Il tient trois projections : comptes (`coeur-bancaire.compte-modifie.v1` : `compteId`, `clientId`, `numero`, `type`, `iban`, `bic`, `solde`, `versionAgregat`), opérations (`coeur-bancaire.operation-enregistree.v1` : état complet de l'opération, solde après compris) et clients (`identite-clients.client-modifie.v1`, pour le titulaire et l'appartenance). Le traitement FEAT-020 du cœur bancaire ne fait que journaliser (FR-020-02) : il n'écrit ni document ni PDF. L'attestation porte le solde de la projection et la date du port Horloge de documents. »

---

## Medium

### ADV-10 — États d'un prêt : approbation par l'administrateur contre scoring asynchrone

- **Paire :** epic Administration (FR-016-04, FR-016-05 : approuver ou refuser un prêt EN_ATTENTE) contre epic Crédits asynchrones (FR-024-06 à FR-024-08 : le scoring consomme `credits.pret-demande.v1`).
- **Divergence :** les deux font passer `LOANS.STATUS` de EN_ATTENTE à EN_COURS ou à REFUSE, et les deux génèrent l'échéancier. La mise à jour conditionnelle n'est imposée que pour le virement (AD-12). Une équipe lit puis écrit : en concurrence, deux échéanciers et deux notifications. FR-024-02 (« échec de transmission, la demande est refusée ») ne peut pas être détecté avec l'outbox de AD-18, qui valide toujours : une équipe publie alors en direct pour pouvoir échouer, ce qui viole AD-18, et une autre ne peut jamais satisfaire le test.
- **Rule proposée (AD-18 ou nouvelle AD « Machines d'états ») :** « Toute entité à statut (virement, prêt, échéance, alerte de fraude, bénéficiaire) a sa machine d'états dans `domain` et chaque transition s'écrit par une mise à jour conditionnelle sur l'état courant ; zéro ligne modifiée = transition ignorée, sans effet de bord (pas d'échéancier, pas d'événement). Une demande de prêt est « transmise » quand sa ligne d'outbox est validée ; FR-024-02 couvre l'échec de cette écriture, pas l'indisponibilité de Kafka. » La deuxième phrase est à faire confirmer par le décideur.

### ADV-11 — Échéance de prêt : débit ou simple marquage ?

- **Paire :** crédits (FR-007-12 « Marquer paye ») contre cœur bancaire.
- **Divergence :** FR-007-12 marque l'échéance payée sans mouvement d'argent. Une équipe qui pense « vraie banque » publie `credits.echeance-payee.v1` pour que le cœur bancaire débite le compte, et l'équipe Cœur bancaire, qui ne l'attend pas, ne le consomme pas : un événement orphelin. Ou elle le consomme, et on a un débit que le golden master ne connaît pas. Même question pour l'approbation d'un prêt : le compte est-il crédité ?
- **Rule proposée (AD-14) :** « Seul le cœur bancaire modifie un solde, et seulement par ses propres cas d'usage. Ni le paiement d'une échéance ni l'approbation d'un prêt ne déplacent d'argent (FR-007-12, FR-024-06) ; tout mouvement d'argent futur demandé par un autre service passera par un événement de commande consommé par le cœur bancaire et par une machine d'états, et demandera une nouvelle AD. »

### ADV-12 — Patrimoine total et IBAN : open banking contre cœur bancaire

- **Paire :** open banking et conseiller (comptes Powens, FR-012-10, FR-002-12) contre cœur bancaire (solde total, FR-002-03 ; bénéficiaires externes, FR-013-04 à FR-013-07).
- **Divergence :** le « Patrimoine total » du tableau de bord additionne le solde du cœur bancaire et celui des comptes Powens. L'équipe Open banking ajoute une projection des soldes du cœur bancaire pour renvoyer le total ; l'équipe Front additionne les deux réponses. Deux totaux, deux arrondis. Pour l'IBAN, chaque service a son objet-valeur `Iban` dans son `domain` (Conventions, journaux) : contrôle MOD-97, liste des pays SEPA et masquage sont recopiés dans le cœur bancaire, l'open banking et le front (« Verifier l'IBAN » sans soumettre le formulaire, FR-013-04, peut être fait en TypeScript). Trois listes SEPA, trois masquages.
- **Rule proposée (AD-17 et Conventions) :** « Un total qui combine deux services est calculé par le front, en centimes, à partir du total renvoyé par chaque service ; aucun service ne projette les soldes d'un autre pour un affichage. La validation d'un IBAN (MOD-97, zone SEPA) n'existe qu'au cœur bancaire, exposée par `POST /api/coeur-bancaire/beneficiaires/verification-iban` ; le front ne la reproduit pas. Le masquage `FR76****1234` est une règle unique, avec un test de référence commun (mêmes entrées, mêmes sorties) dans chaque service qui manipule un IBAN. Pas de bibliothèque partagée entre services [ASSUMPTION à confirmer : sinon, un module `shared-kernel` limité aux objets-valeurs]. »

### ADV-13 — Journal des traitements : affiché par l'administration et par le catalogue

- **Paire :** open banking et conseiller (page Produits, FR-011-09 « Historique des traitements nocturnes ») contre cœur bancaire (traitements FEAT-018 à FEAT-022 et leur journal, AD-14, AD-21).
- **Divergence :** AD-14 dit que chaque service garde le journal de ses propres traitements, mais deux écrans montrent « les 50 dernières exécutions » : l'administration (FR-016-17) et le catalogue (FR-011-09), ce dernier porté par l'open banking. L'équipe Catalogue crée un topic pour projeter le journal du cœur bancaire ; l'équipe Front appelle le point d'API d'administration du cœur bancaire, réservé à ADMIN, depuis une page ouverte à tous les rôles, et prend un 403. Si un autre service a un jour un traitement planifié, « les 50 dernières » ne savent plus mélanger deux journaux.
- **Rule proposée (AD-21) :** « Le journal des traitements est exposé par chaque service propriétaire en lecture (`GET /api/<service>/traitements?limite=50`), ouvert aux rôles des écrans qui l'affichent (FR-011-09 : tout utilisateur authentifié ; FR-016-17 : ADMIN). Le front appelle directement ce point d'API ; aucun service ne projette le journal d'un autre. FEAT-018 à FEAT-022 sont tous au cœur bancaire ; un traitement planifié dans un autre service n'apparaît dans ces écrans qu'après une décision explicite. »

### ADV-14 — Montants, dates seules et statuts d'erreur : front contre services

- **Paire :** front (formateurs de `src/domain`, gestion des erreurs) contre chaque service.
- **Divergence :** (a) Arrondi : `BigDecimal` à 2 décimales, mais sans mode d'arrondi. Les intérêts (FEAT-018), la mensualité (FEAT-007) et le PFU de 30 % (FR-008-10) sont arrondis en HALF_UP dans un service et en HALF_EVEN dans un autre, ce qui donne un centime d'écart avec le legacy. (b) Saisie : le front envoie « 1234,5 » tel quel ou « 1234.50 » ; le message « Montant invalide » (FR-004-05) est produit soit par le front, soit par le service, en double ou en contradiction. (c) Instant ou date seule : une opération est un instant pour le cœur bancaire et une date pour documents ; « jour » (FR-022-04, HIGH_FREQUENCY) et « année » (IFU) sont calculés en UTC par l'un et en Europe/Paris par l'autre, ce qui produit un écart entre 22 h et minuit. (d) Ressource d'un autre client : 403 chez documents (FR-008-04), 404 « pour ne pas révéler » chez le cœur bancaire ; le front affiche deux messages.
- **Rule proposée (Conventions, lignes Montants, Dates, Erreurs) :** « Arrondi `RoundingMode.HALF_UP` partout (Java et front), au centime, à la dernière étape du calcul. Le front envoie un montant en chaîne décimale à point (`"1234.50"`) après une normalisation purement syntaxique (virgule → point) ; les contrôles métier et leurs messages sont faits par le service seul, le front n'en ajoute pas. Une opération, une soumission ou une notification est un instant ; une échéance, une date de prêt ou un plafond de budget est une date seule. Tout découpage en jour, mois ou année se fait en Europe/Paris à partir du port Horloge (`Horloge.aujourdhui()` renvoie une `LocalDate` en Europe/Paris). Ressource existante d'un autre client : 403 ; ressource inexistante : 404 ; dans les deux cas, `{"message"}` sans détail. »

---

## Low

### ADV-15 — Le service audit expose une route que le PRD interdit

- **Paire :** audit contre front.
- **Divergence :** le schéma des conteneurs et AD-17 renvoient `/api/audit/**` vers le service audit ; FR-026-06 interdit tout écran ou fonction qui consulte le journal. Une équipe Audit ajoute un `GET` de diagnostic « pour les tests », une autre n'expose rien.
- **Rule proposée (AD-17) :** « Le service audit n'expose aucun point d'API HTTP métier ; le conteneur web ne renvoie pas `/api/audit/**`. Les tests lisent `AUDIT_LOGS` par JDBC, dans les tests d'intégration du service audit. » Côté contenu, fixer aussi dans AD-18 : `donnees` d'un événement d'audit = `utilisateur` (chaîne : identifiant de connexion saisi pour LOGIN_FAILED, `clientId` en texte sinon, `SYSTEM` pour un traitement), `action` (liste fermée de FR-026-01 à FR-026-03, énumération dans le domaine du service audit, valeur inconnue rejetée et journalisée), `detail`, `adresseIp` (nullable, résolue par FR-001-13 avec une liste `nb.audit.mandataires-de-confiance` identique dans tous les services : le conteneur web et le proxy Vite), et l'horodatage de l'enveloppe (producteur, port Horloge), jamais l'heure de réception.

### ADV-16 — Utilisateur supprimé pendant sa session

- **Paire :** identité et clients contre front (FR-002-02).
- **Divergence :** un jeton valide d'un client supprimé obtient 404 chez identité, 200 avec des listes vides ailleurs ; le front redirige vers `/login` dans un cas et affiche un tableau de bord vide dans l'autre.
- **Rule proposée (AD-16) :** « `GET /api/identite-clients/moi` renvoie 401 pour un client supprimé ou inexistant ; le front traite tout 401 en supprimant `nb.jeton` et en affichant `/login`. Les autres services ne vérifient pas l'existence du client. »

---

## Récapitulatif des AD à créer ou resserrer

| AD | Action | Constats |
| --- | --- | --- |
| AD-22 (nouvelle) — Contrats d'événements et projections | créer | ADV-03, ADV-05, ADV-09 |
| AD-12 | resserrer : audit hors transaction, notification décidée par le producteur, MULTIPLE_FAILED_AUTH | ADV-01, ADV-04, ADV-07 |
| AD-18 | resserrer : audit en outbox indépendante, `donnees` d'audit, transmission de prêt | ADV-01, ADV-10, ADV-15 |
| AD-19 | étendre au front ; `aujourdhui()` en Europe/Paris | ADV-02, ADV-14 |
| AD-11 | resserrer : Playwright `timezoneId`, remise à zéro de Kafka et des projections | ADV-02, ADV-08 |
| AD-16 | resserrer : renouvellement refusé si inactif, appartenance côté serveur pour CONSEILLER, 401 si supprimé | ADV-05, ADV-06, ADV-16 |
| AD-14 | resserrer : documents seul producteur de PDF, seul le cœur bancaire déplace de l'argent | ADV-09, ADV-11 |
| AD-17, AD-21, Conventions | resserrer : totaux combinés par le front, journal des traitements, arrondi, 403/404 | ADV-12, ADV-13, ADV-14, ADV-15 |
| Nouvelle AD « Machines d'états » (ou AD-12 généralisée) | créer | ADV-10 |

Décisions à faire trancher par le décideur avant d'écrire les Rules : ADV-01 (laquelle des deux phrases survit), ADV-05 (désactivation effective à l'expiration du jeton), ADV-10 (sens de « transmission » pour FR-024-02), ADV-12 (pas de bibliothèque partagée).
