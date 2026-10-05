
## 5. Non-Goals (hors périmètre explicite)

- **FEAT-031 — Analytique client et segmentation** : non refaite. C'est du code mort du legacy, sans aucun appelant, et le métier a confirmé que la fonctionnalité est abandonnée (`docs/retro/00-global/dead-features.json`, gate retro-features).
- **FEAT-032 — Bean de gestion des notifications côté interface** : non refaite. Code mort, référencé par aucun écran, et le métier a confirmé que la fonctionnalité est abandonnée. Les notifications internes restent dans le périmètre (§4, notifications internes).
- **Migration des données du legacy** : aucune reprise de clients, de comptes ni d'historique réels. Les données de référence sont rechargées (décision de cadrage).
- **Notifications hors de l'application** : ni e-mail, ni SMS, ni push (décision de cadrage).
- **Engagement réglementaire de production** (DSP2, RGPD, durée légale de conservation de l'audit) : hors de cette version, qui est un démonstrateur (décision de cadrage). Les exigences de sécurité d'une vraie banque restent dues (§7.3).
- **Environnements autres que le local** : seul le lancement local par `compose.yaml` est dans le périmètre. Aucun hébergement cible n'est spécifié (décision de cadrage).
- **Évolutions fonctionnelles au-delà de la parité** : aucune, en dehors des 50 bugs à corriger et des décisions tracées dans ce PRD.
- **Bugs conservés** : les bugs du legacy décidés « ne pas corriger » ne sont ni des exigences ni des tests. Ils sont listés dans `docs/retro/00-global/LEGACY-BUGS.md`.

## 6. Périmètre de la version

### 6.1 Dans le périmètre

- Les 30 fonctionnalités FEAT-001 à FEAT-030 (§4), en parité avec le legacy.
- La correction des 50 bugs à corriger, chacune couverte par un test.
- Les contrôles de `docs/quality-gates.md`, livrés et verts avant toute story fonctionnelle (NFR-QG-00).
- Les adaptateurs simulés des trois fournisseurs externes.
- Le golden master dérivé et son rejeu final.
- Le contrôle de fraude d'un virement selon la décision RG-027-003 (§9), livré en trois stories ordonnées :
  1. le socle RG-027-003 : contrôle synchrone avant la validation, suspension avant débit, refus en cas d'erreur, états du virement, revue par l'administrateur, échéance et rejet automatique, écran de refus (décisions D1 à D9) ;
  2. le cumul glissant des virements d'un client (FR-027-18, décision D10), dans la story qui suit immédiatement le socle ;
  3. la reprise préremplie après un refus (FR-003-28, décision D12).

### 6.2 Hors périmètre de la version

Voir §5.

## 7. Exigences non fonctionnelles transverses

### 7.1 Contrôles qualité (repris intégralement de `docs/quality-gates.md`)

Chaque contrôle est exécuté par un outil, jamais évalué par un agent. Les seuils sont ceux de `docs/quality-gates.md`, sans reformulation. Pour le tracing, le moment d'exécution est précisé quand la référence ne le donne pas (A-G-03).

- **NFR-QG-00 — Contrôles d'abord.** Les contrôles sont livrés et verts avant toute story fonctionnelle.
  - Ils sont livrés par l'epic 1 (fondations). Chaque story de l'epic 1 qui livre un contrôle le rend vert à sa fin : trace-smoke et ArchUnit ne peuvent passer qu'une fois le squelette et deux services en place.
  - Aucune story fonctionnelle ne démarre tant que les contrôles bloquants (NFR-QG-01 à NFR-QG-04, NFR-QG-07 à NFR-QG-09) ne sont pas livrés et verts, et que les contrôles à la demande (NFR-QG-05, NFR-QG-06) ne sont pas livrés et exécutables.
- **NFR-QG-01 — Hexagone back.** Outil : test ArchUnit `*ArchitectureTest` dans chaque service. Seuil : deux règles, le sens des dépendances, et aucun framework dans domain et application. Moment : bloquant à chaque build.
- **NFR-QG-02 — Hexagone front.** Outil : dependency-cruiser, configuration `frontend/.dependency-cruiser.cjs`. Seuil : règles en sévérité error, le sens des dépendances, et aucun paquet npm dans `src/domain` et `src/application`. Moment : bloquant.
- **NFR-QG-03 — Couverture back.** Outil : JaCoCo check. Seuil : 100 % lignes et branches sur domain et application ; 80 % sur adapter et infrastructure. Moment : phase verify, bloquant.
- **NFR-QG-04 — Couverture front.** Outil : Vitest `coverage.thresholds` par glob. Seuil : mêmes seuils (100 % lignes et branches sur `src/domain` et `src/application` ; 80 % sur `src/adapters` et `src/infrastructure`). Moment : bloquant.
- **NFR-QG-05 — Mutation back.** Outil : PIT dans le profil Maven `mutation`, sur domain et application. Seuil : `mutationThreshold` 85, historique activé. Moment : à la demande.
- **NFR-QG-06 — Mutation front.** Outil : StrykerJS sur `src/domain` et `src/application`. Seuil : thresholds break 85 / high 90, mode incremental. Moment : à la demande.
- **NFR-QG-07 — CVE back.** Outil : OWASP Dependency-Check dans le profil Maven `security`. Seuil : `failBuildOnCVSS` 7. Configuration : clé d'API NVD via `settings.xml`, OSS Index et Node Audit désactivés, mise à jour de la base désactivée pendant les scans. Moment : lancé par `scripts/gates/cve.sh`, hors du build Maven par défaut, à chaque story dans la boucle de développement et à la demande.
- **NFR-QG-08 — CVE back et front.** Outil : Trivy fs. Seuil : sévérités HIGH et CRITICAL ; porte bloquante avec `--ignore-unfixed`. Les vulnérabilités sans correctif sont journalisées dans `reports/`, sans bloquer. Moment : porte bloquante.
- **NFR-QG-09 — Tracing prouvé.** Outil : `scripts/gates/trace-smoke.sh`. Seuil : il prouve qu'une trace traverse deux services, avec un span JDBC et un span Kafka. Moment : avant de déclarer terminée toute story qui touche un échange entre services. [ASSUMPTION A-G-03 : moment non précisé dans `docs/quality-gates.md`]
- **NFR-QG-10 — Les contrôles ne se modifient pas.**
  - Un contrôle rouge se corrige dans le code ou dans les tests, jamais en modifiant le contrôle.
  - Fichiers interdits de modification : `scripts/gates/**`, `.bmad-loop/policy.toml`, `.bmad-loop/gates.sha256`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml`.
  - Interdit d'abaisser un seuil, d'ajouter une exclusion de couverture ou de mutation, de désactiver un test.
  - Si un seuil paraît inatteignable : s'arrêter et le signaler, ne pas contourner.

### 7.2 Parité et tests

- **NFR-PAR-01 — Parité par le golden master.**
  - Le golden master du legacy, dérivé pour la nouvelle application, rejoue sans échec avant la bascule.
  - Il se rejoue une seule fois, développement terminé, pas entre les stories ni les epics.
  - Les 31 scénarios applicables doivent passer : 33 scénarios capturés, moins SC-003-03 et SC-010-02, qui reproduisent des bugs à corriger. Contrat : `docs/retro/golden-master-contract.md`.
  - Trois scénarios applicables, SC-003-01, SC-003-02 et SC-003-04, divergent volontairement du legacy (contrôle de fraude avant la validation, suspension, refus en cas d'erreur : décision RG-027-003). Leur décision de rejeu, reprise avec une exception documentée ou exclusion justifiée, est prise à l'étape G2 pendant l'architecture. C'est elle qui fixe le nombre final de scénarios rejoués.
- **NFR-PAR-02 — Bugs corrigés prouvés.** Chacun des 50 bugs à corriger est couvert par un test automatisé qui vérifie le comportement attendu (critère d'acceptation de la FR qui le cite).
- **NFR-PAR-03 — Règles couvertes.** Chaque règle de gestion reprise est couverte par au moins une exigence de ce PRD et par au moins un test.
- **NFR-PAR-04 — Données de référence.**
  - Un jeu de données de référence, équivalent à celui du legacy (`import.sql` : 7 clients, 7 comptes, 45 transactions…), est chargé par une commande.
  - Une commande de remise à zéro le rétablit.
  - Il sert au développement local, aux tests de bout en bout et au rejeu du golden master.

### 7.3 Sécurité

- **NFR-SEC-01** : les mots de passe sont stockés et vérifiés par une fonction de hachage à sens unique sûre (FR de FEAT-001, BUG-002).
- **NFR-SEC-02** : à l'authentification, la session existante est invalidée et une nouvelle session est créée avant de stocker le contexte d'authentification. Aucune fixation de session n'est possible (BUG-003, BUG-092).
- **NFR-SEC-03** : toute action ou lecture portant sur une ressource d'un client (compte, document, virement programmé, bénéficiaire) vérifie que la ressource appartient à l'utilisateur authentifié. Le contrôle d'accès par rôle est appliqué dans les services, indépendamment du filtrage des requêtes (BUG-013, BUG-027, BUG-047, BUG-058, BUG-093).
- **NFR-SEC-04** : aucun message d'erreur affiché à un utilisateur n'expose de détail technique (pile d'appels, code d'erreur interne).
- **NFR-SEC-05** : les IBAN sont masqués dans tous les journaux du serveur (par exemple `FR76****1234`) (BUG-086). Les montants ne sont pas masqués : c'est un écart connu avec le brief (« logs sans masquage IBAN/montant »), accepté pour le démonstrateur (risque R-3).
- **NFR-SEC-06** : aucun secret (clé d'API, mot de passe) n'est écrit dans le code source ni dans un fichier versionné. Les secrets viennent de la configuration d'exécution (BUG-034, BUG-091).
- **NFR-SEC-07** : la session utilisateur expire après 30 minutes d'inactivité, comme dans le legacy (`docs/retro/00-global/PERMISSIONS.md`).
- **NFR-SEC-08 — Seuils du contrôle de fraude.** Le seuil de score (75), le seuil de fraude (10 000 €), le plafond du cumul glissant (10 000 €), le seuil d'alerte et le seuil de mise en attente (5 000 €) sont des paramètres de configuration de déploiement, validés au démarrage : une configuration invalide empêche le démarrage du service (décision D2).
- **NFR-SEC-09 — Expiration technique de l'accès aux données du contrôle.** L'accès aux données d'historique utilisées par le contrôle de fraude a un délai d'expiration technique. Son expiration est une erreur du contrôle et entraîne le refus du virement (FR-027-02). La valeur n'est pas une règle métier : elle est fixée dans la décision d'architecture, en configuration validée au démarrage (décision D4). Si le contrôle devenait un jour un service externe, un délai métier serait à rétablir (500 ms proposé en table ronde).

### 7.4 Fournisseurs externes

- **NFR-INT-01 — Port et adaptateur simulé.**
  - Chaque fournisseur externe (Azure OpenAI, Powens, AlphaVantage) est appelé derrière un port.
  - Un adaptateur simulé est actif par défaut en local, dans les tests et dans les contrôles. Les vrais appels sont une option de configuration.
  - Aucun test ni aucun contrôle ne dépend du réseau ni d'une clé externe.
- **NFR-INT-02 — Résilience.**
  - Chaque appel réel à un fournisseur externe a un délai d'attente et passe par un coupe-circuit.
  - Les cotations boursières sont mises en cache, pour respecter la limite du plan AlphaVantage (25 requêtes par jour, 5 par minute).
  - L'indisponibilité d'un fournisseur est signalée à l'utilisateur, sans bloquer le reste de l'application.
- **NFR-INT-03 — Mêmes fournisseurs.** Les fournisseurs sont ceux du legacy : Azure OpenAI, Powens, AlphaVantage.

### 7.5 Performance

Les temps du legacy sont une référence mesurée, pas une cible (brief). Cibles au 95e centile, en local, avec les adaptateurs simulés :

- **NFR-PERF-01** : pages de navigation inférieures à 200 ms ; pages à calcul léger (relevé, budget, bénéficiaires) inférieures à 500 ms ; page bourse inférieure à 3 000 ms ; téléchargement PDF inférieur à 2 000 ms. [ASSUMPTION A-G-04 : ces valeurs, recommandées par `docs/retro/00-global/NFR.md`, sont adoptées comme cibles]

### 7.6 Traitements planifiés

- **NFR-BATCH-01** : les traitements planifiés gardent les horaires du legacy :
  - calcul des intérêts tous les jours à 02h00 ;
  - détection de fraude nocturne tous les jours à 02h30 ;
  - relevés mensuels le 1er du mois à 03h00 ;
  - virements programmés toutes les 5 minutes ;
  - contrôle des soldes faibles et des seuils toutes les 30 minutes.
- **NFR-BATCH-02** : chaque exécution d'un traitement planifié est enregistrée dans un journal d'exécution (heure, résultat).
- **NFR-BATCH-03** : le fuseau horaire des traitements est configuré explicitement. [ASSUMPTION A-G-07 : Europe/Paris ; le legacy ne le configure pas (`docs/retro/GAPS.md`, BATCH.md)]

### 7.7 Observabilité

- **NFR-OBS-01** : chaque requête et chaque message entre services est tracé de bout en bout, selon l'architecture de tracing imposée (addendum, §A.1). La preuve est NFR-QG-09.
- **NFR-OBS-02** : la trace d'un virement montre le contrôle de fraude, son issue (exécuté, suspendu, refusé pour erreur) et, en cas d'erreur, un statut d'erreur. L'identifiant de trace du virement est la référence affichée au client en cas de refus (FR-003-27, décision D9). Le moyen retenu respecte la règle « aucun span créé à la main » de `docs/quality-gates.md` ; il est décidé en architecture.

### 7.8 Langue et interface

- **NFR-UI-01** : l'interface est en français. Les libellés et les messages affichés sont repris à l'identique du corpus (`docs/retro/00-global/MESSAGES.md` et `MESSAGES.md` de chaque fonctionnalité), sauf correction d'un bug à corriger.
- **NFR-UI-02** : chaque champ et chaque bouton actionné par un scénario du golden master porte un attribut de test stable. [ASSUMPTION A-G-08 : exigence posée ici pour rendre le rejeu possible ; la forme exacte est décidée en architecture]

## 8. Indicateurs de succès

**Principaux**
- **SM-1** : rejeu du golden master. Les 31 scénarios applicables passent, lors du rejeu final unique. Valide NFR-PAR-01 et l'ensemble du §4.
- **SM-2** : couverture des règles. 100 % des règles de gestion reprises sont citées par une FR (mesure : `docs/retro/tools/check-prd-coverage.py`) et couvertes par un test. Valide NFR-PAR-03.
- **SM-3** : bugs corrigés. 50 sur 50 bugs à corriger ont un test vert du comportement attendu. Valide NFR-PAR-02.

**Secondaires**
- **SM-4** : contrôles. Les contrôles bloquants de §7.1 (NFR-QG-01 à NFR-QG-04, NFR-QG-07 à NFR-QG-09) sont verts sur la branche principale à la fin de chaque story ; les contrôles à la demande (NFR-QG-05, NFR-QG-06) sont exécutables. Valide NFR-QG-00 à NFR-QG-09.
- **SM-5** : performance. Les cibles NFR-PERF-01 sont tenues sur le parcours UJ-1.

**Contre-indicateurs (à ne pas optimiser)**
- **SM-C1** : nombre de stories livrées par epic. Accélérer au prix d'un contrôle rouge ou contourné est un échec. Contrebalance SM-4.
- **SM-C2** : taux de couverture au-delà des seuils. Il ne sert à rien d'écrire des tests sans assertion pour atteindre 100 % ; la mutation (NFR-QG-05 et NFR-QG-06) en est le garde-fou. Contrebalance SM-2.
- **SM-C3** : rejeux intermédiaires du golden master. Rejouer en cours de route ne prouve rien sur une application partielle et pousse à adapter les tests. Contrebalance SM-1.

## 9. Questions ouvertes

### Décision RG-027-003 — contrôle de fraude d'un virement (tranchée le 2026-10-05)

L'ancienne question bloquante QO-1, points (a) et (b), est tranchée par le décideur en table ronde. Le transcript complet, avec les options écartées, est `_bmad-output/party-rg-027-003-script.md`. Option retenue : P2, la proposition équilibrée.

| # | Décision | Exigences |
|---|---|---|
| D1 | (a) Un virement suspect est **suspendu avant tout débit**, en attente de revue ; sinon, il est exécuté. | FR-027-02, FR-003-09 |
| D2 | Suspension si le **score cumulé est strictement supérieur à 75 OU si le montant est supérieur ou égal à 10 000 €**. C'est une décision prise ici, pas un fait du corpus ; les seuils sont en configuration validée au démarrage. | FR-027-02, FR-023-04, NFR-SEC-08 |
| D3 | (b) Une erreur du contrôle entraîne un **refus pur, quel que soit le montant** : rien n'est débité, l'erreur est journalisée. | FR-027-02, FR-003-27 |
| D4 | Le contrôle n'a pas de délai métier (calcul local, sans appel réseau). Une **expiration technique de l'accès aux données** est une erreur ; sa valeur est fixée en architecture. | NFR-SEC-09 |
| D5 | La décision est synchrone et locale. Un virement passe par **cinq états**. | FR-027-16, FR-003-13, FR-023-01 |
| D6 | **L'administrateur seul** libère ou rejette, avec un motif obligatoire et son identité tracée ; pas de rôle pour le conseiller. | FR-016-19 |
| D7 | La revue a **24 heures calendaires** à partir de la soumission ; le client voit une **heure butoir** précise. | FR-027-04, FR-027-17 |
| D8 | À l'expiration, le virement est **rejeté automatiquement, sans débit**. | FR-027-17 |
| D9 | Le texte minimal de l'**écran de refus** est fixé ; la référence affichée est l'identifiant de trace du virement. | FR-003-27, NFR-OBS-02 |
| D10 | Le **cumul glissant** (24 heures, tous bénéficiaires, plafond de 10 000 €) est dans le projet, dans la story qui suit immédiatement le socle. | FR-027-18 |
| D11 | Le risque de découpage reste ouvert jusqu'à D10. | Risque R-1 |
| D12 | La **reprise préremplie** après un refus est une story après le socle et le cumul. | FR-003-28 |

*Note : le corpus était incohérent sur ce point. `docs/retro/00-global/LEGACY-BUGS.md` présente BUG-084 comme « validé par le métier », mais sa note dit qu'il dépend de la question bloquante FEAT-027, absente de `docs/retro/GAPS.md`. Le code legacy laissait la question en « TODO » (JIRA-9202). La décision ci-dessus tranche cette question.*

### Risques connus

- **R-1 — Découpage d'un virement sous les seuils.** Tant que le cumul glissant (FR-027-18) n'est pas livré, un client peut émettre plusieurs virements sous les seuils de score et de montant, sans suspension. Ce risque est accepté pour la durée du socle seulement (décision D11).
- **R-2 — Rejet automatique la nuit et le week-end.** L'échéance de 24 heures calendaires peut rejeter un virement suspendu sans revue humaine si personne n'est disponible. C'est accepté pour le démonstrateur, et à revoir si le projet en sort (décision D8).
- **R-3 — Montants non masqués dans les journaux.** Seuls les IBAN sont masqués (NFR-SEC-05). Les montants restent en clair dans les journaux du serveur. Accepté pour le démonstrateur, à revoir si le projet en sort (décision de finalisation du 2026-10-05).

### Autres questions ouvertes (non bloquantes)

- **QO-2** : questions ouvertes du brief, non tranchées par le métier. Le PRD a fait un choix pour chacune, à confirmer :
  - état `DEFAUT` des prêts : aucune transition n'y mène dans le legacy ; l'échéancier n'est consultable que pour les prêts EN_COURS ou REMBOURSE (FR-007-11), et la gestion des impayés n'est pas dans le périmètre ;
  - compteur de téléchargements : voir FR-008-12 ;
  - bouton « Rafraîchir » des comptes tiers : exposé (FR-012-16, A-012-5) ;
  - méthodes de catalogue par type (épargne, crédit, investissement) sans appelant : non reprises comme capacités distinctes ; le catalogue (FEAT-011) affiche ses trois sections ;
  - `evaluateSingleScore`, point d'entrée d'évaluation d'un score isolé (BUG-087, conservé) : ni repris ni supprimé par une exigence ;
  - surcharge éventuelle des seuils de fraude, d'alerte et de mise en attente en production : sans objet pour le démonstrateur, qui utilise les valeurs du code en configuration de déploiement (addendum A.3).

  Voir aussi l'index §10.
- **QO-3** : valeur de production du connecteur Powens (`POWENS_CONNECTOR_ID`). Sans objet pour le démonstrateur, qui tourne avec l'adaptateur simulé par défaut.

