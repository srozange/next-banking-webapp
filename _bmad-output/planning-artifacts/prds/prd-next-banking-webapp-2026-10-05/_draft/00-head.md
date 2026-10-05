---
title: "PRD : next-banking-webapp (rénovation)"
status: final
created: 2026-10-05
updated: 2026-10-05
inputDocuments:
  - "_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/brief.md"
  - "_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/addendum.md"
  - "docs/retro/ (corpus de rétro-ingénierie, commit c05512ecab8af8878d695ccd0f33fc3ece8811a4)"
  - "docs/quality-gates.md"
  - "_bmad-output/party-rg-027-003-script.md (décisions D1 à D12 sur RG-027-003)"
---

# PRD : next-banking-webapp (rénovation)

*Brouillon complet en mode Fast path, non finalisé. Les suppositions sont marquées `[ASSUMPTION]` et indexées en §10. La décision sur la règle RG-027-003 (contrôle de fraude d'un virement) a été prise le 2026-10-05 (§9, décisions D1 à D12) ; les exigences qui en découlent et s'écartent du corpus sont marquées `[ÉVOLUTION — décision RG-027-003 …]`, et celles livrées après le socle `[STORY SUIVANTE]`. Les décisions de finalisation B1 à B7, prises au gate de relecture (§9), sont marquées `[ÉVOLUTION — décision de finalisation Bn]` ; B1 a été révisée (textes legacy conservés pour la démonstration).*

## 0. Objet du document

Ce PRD décrit la rénovation de l'application bancaire existante (le « legacy », pile Java EE analysée au commit `c05512ecab8af8878d695ccd0f33fc3ece8811a4`) en une nouvelle application. Il s'adresse au PM, à l'architecte (`bmad-architecture`), à l'équipe qui découpera les epics et à la boucle de développement.

**Sources :**
- le corpus a été produit sur le legacy au commit `c05512ecab8af8878d695ccd0f33fc3ece8811a4` ; ses 63 lacunes connues sont listées dans `docs/retro/GAPS.md` (aucune n'est bloquante) ;
- le brief produit (`_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/`) ;
- le corpus de rétro-ingénierie `docs/retro/`, qui fait foi pour le comportement fonctionnel ;
- `docs/quality-gates.md`, la référence unique de l'architecture cible et des contrôles.

**Structure :** un glossaire, puis les 30 fonctionnalités du périmètre (§4). Chaque fonctionnalité regroupe des exigences fonctionnelles numérotées `FR-NNN-xx`, où NNN est le numéro de la fonctionnalité du corpus (FEAT-NNN). La ligne `Source :` de chaque FR cite les règles de gestion (`RG-NNN-NNN`) et les bugs à corriger (`BUG-NNN`) qu'elle couvre. Les exigences non fonctionnelles transverses (§7) reprennent intégralement les contrôles de `docs/quality-gates.md`. L'architecture cible imposée et les détails techniques sont dans `addendum.md`.

## 1. Vision

La banque dispose d'une application en ligne fonctionnelle mais sans documentation, bâtie sur une pile Java EE vieillissante, sans API, sans conteneurisation ni intégration continue, et qui porte 50 comportements défectueux que le métier a décidé de corriger. La rénovation livre à ses trois populations (clients, conseillers, administrateurs) **la parité, plus les évolutions décidées** : le même service que le legacy, prouvé par le rejeu du golden master, et la correction de ces 50 bugs, chacune démontrée par un test. Les évolutions décidées, qui s'écartent du legacy sans qu'un bug les couvre, sont :
- la décision RG-027-003 sur le contrôle de fraude d'un virement (§9, D1 à D12) : suspension avant débit, revue par l'administrateur, cumul glissant, reprise préremplie après un refus ;
- les décisions de finalisation B1 à B7 (§9) : textes legacy « en attente » conservés pour la démonstration (B1 révisée), contrôle de fraude des virements programmés, plafond des alertes de solde faible, passage des alertes en REVIEWED, heure butoir qui fait foi, rejet sans débit d'une libération sur solde insuffisant, rejet sans débit d'un virement libéré dont le solde est devenu insuffisant au débit ;
- après la démonstration, la reformulation des textes de transaction importante (FR-003-29, B1 révisée) ;
- le signal d'échecs de connexion répétés (FR-027-11) ;
- le bouton « Rafraîchir » des comptes tiers (A-012-5) ;
- l'exemption d'authentification des écrans publics sur leur chemin exact (A-030-1).

La nouvelle application est construite pour être vérifiée par des outils et non par des relectures : architecture hexagonale contrôlée automatiquement, couverture et mutation chiffrées, CVE bloquées, traces distribuées prouvées. Chaque story est livrée avec ces contrôles au vert. C'est ce qui permet de la développer en grande partie avec des agents, sans dérive.

La portée de cette version est un **démonstrateur réaliste** : le comportement et les exigences de sécurité d'une vraie banque, sans engagement réglementaire de production ni migration de données réelles.

## 2. Utilisateurs cibles

### 2.1 Jobs To Be Done

- **Client** : consulter ses comptes et son activité, virer de l'argent en sécurité (ponctuellement ou de façon programmée), gérer ses bénéficiaires, suivre son budget, demander un prêt, récupérer ses documents, suivre la bourse, agréger ses comptes d'autres banques, poser une question à un conseiller.
- **Conseiller** : voir son portefeuille de clients et leur situation, pour les accompagner.
- **Administrateur** : piloter l'ensemble (clients, prêts à approuver, alertes de fraude, traitements planifiés) et réagir aux anomalies.
- **Système** (sans rôle utilisateur) : traitements planifiés (intérêts, fraude nocturne, virements programmés, soldes faibles, relevés) et traitements asynchrones (virements, approbation des prêts, notifications, audit).

### 2.2 Non-utilisateurs (v1)

- Les utilisateurs anonymes, en dehors de la page de connexion et du retour du fournisseur d'open banking.
- Les applications tierces : aucune API publique n'est exposée à des partenaires. [ASSUMPTION A-G-05 : l'API REST de la nouvelle application est interne au front ; aucune exposition à des tiers en v1]

### 2.3 Parcours utilisateurs clés

- **UJ-1. Claire, cliente, vire 1 200 € à son colocataire.** Elle se connecte, voit son tableau de bord (soldes, dernières opérations du compte courant), ouvre le virement, choisit le compte source et le bénéficiaire, saisit le montant et un motif, puis valide. Le contrôle de fraude s'exécute avant la validation finale. Elle reçoit une notification dans l'application. **Cas limite :** solde insuffisant → refus avec un message clair et non technique (BUG-008, BUG-011). Un virement jugé suspect est suspendu avant tout débit : Claire voit l'heure butoir avant laquelle la banque répondra. Si le contrôle tombe en erreur, le virement est refusé, rien n'est débité, et elle peut le refaire quelques minutes plus tard. Réalise FEAT-001, FEAT-002, FEAT-003, FEAT-025, FEAT-027.
- **UJ-2. Marc, conseiller, prépare un rendez-vous.** Il ouvre son espace conseiller, ne voit que les clients qui lui sont affectés, consulte la situation de l'un d'eux et la date à laquelle il est devenu client de la banque (BUG-055). Réalise FEAT-015, FEAT-030.
- **UJ-3. Sophie, administratrice, traite la file du matin.** Elle consulte les alertes de fraude de la nuit, toutes statuts confondus (BUG-056), approuve ou refuse les prêts en attente, désactive un client. Ce client ne peut plus se connecter (BUG-001). Chaque action est auditée avec l'adresse IP d'origine (BUG-083). Réalise FEAT-016, FEAT-019, FEAT-026.
- **UJ-4. Claire agrège son compte d'une autre banque.** Depuis « Comptes tiers », elle est redirigée vers le fournisseur d'open banking. Au retour, elle voit ses comptes externes et leurs 20 dernières opérations. Un solde absent est distingué d'un solde nul (BUG-042). En cas d'indisponibilité du fournisseur, elle en est informée (BUG-043). Réalise FEAT-012, FEAT-014, FEAT-028.
- **UJ-5. Sophie, administratrice, traite la file des virements suspendus.** Dans l'administration, elle ouvre la file des virements suspendus. Pour chacun, elle voit le client, le montant, le motif de la suspension (signaux et score, ou montant supérieur ou égal au seuil de fraude) et l'heure butoir. Elle libère, avant son heure butoir, le virement de 12 000 € de Claire, avec le motif « Client joint par telephone » : le virement est exécuté et débité, sans nouvelle notification. Elle rejette un autre virement, dont le score dépasse 75, avec le motif « Beneficiaire inconnu du client » : il passe à REJETÉ, sans débit. Dans les deux cas, les alertes de fraude encore ouvertes du virement passent au statut REVIEWED dans la même action (décision B4), et la décision est auditée avec son identité. **Cas limite :** un virement dont l'heure butoir est atteinte ne peut plus être libéré ; il est rejeté (décision B5). Réalise FEAT-016, FEAT-026, FEAT-027.

[ASSUMPTION A-G-06 : les cinq parcours ci-dessus sont illustratifs, reconstitués depuis le corpus ; aucun parcours n'a été raconté par le métier]

## 3. Glossaire

- **Legacy** : l'application existante, décrite par le corpus `docs/retro/`.
- **Corpus** : le dossier `docs/retro/`, référence du comportement du legacy (règles RG, validations VAL, messages MSG, scénarios SC).
- **Règle de gestion (RG)** : règle identifiée `RG-NNN-NNN` dans `docs/retro/features/FEAT-NNN/RG.md`, de statut Confirmé, À confirmer ou Suspect.
- **Bug à corriger** : comportement défectueux du legacy identifié `BUG-NNN`, que le métier a décidé de corriger. Son comportement attendu remplace le comportement observé.
- **Bug conservé** : bug du legacy décidé « ne pas corriger ». Il n'est ni une exigence ni un test (liste dans `docs/retro/00-global/LEGACY-BUGS.md`).
- **Client** : personne titulaire de comptes, de rôle CLIENT. Un client a un ou plusieurs comptes.
- **Conseiller** : utilisateur de rôle CONSEILLER, affecté à un portefeuille de clients.
- **Administrateur** : utilisateur de rôle ADMIN. Les écrans « client » (tableau de bord, virement…) ne lui montrent que ses propres données, jamais celles des autres clients ; dans les données de référence, il n'a pas de compte personnel.
- **Compte** : compte bancaire interne d'un client, de type COURANT, EPARGNE, CREDIT, etc.
- **Virement** : transfert d'un montant d'un compte source vers un compte cible ou un bénéficiaire externe.
- **Virement programmé** : virement récurrent avec une fréquence et une prochaine date d'exécution, exécuté par un traitement planifié. Un virement programmé **inactif** n'est plus exécuté (FR-021-02, FR-021-04) ; le corpus le dit parfois « suspendu », mais ce n'est pas un virement suspendu au sens ci-dessous.
- **Bénéficiaire externe** : destinataire hors banque, identifié par un IBAN, enregistré par un client.
- **Bénéficiaire actif** : bénéficiaire externe enregistré par le client et non désactivé (FR-013-07) ; un virement vers un IBAN qui n'est pas un bénéficiaire actif du client lève le signal NEW_BENEFICIARY.
- **Transaction (ou opération)** : ligne du relevé d'un compte, débit ou crédit. Un virement exécuté produit deux opérations, un débit et un crédit (FR-003-09) ; les signaux de fraude qui comptent des « transactions » ou des « virements » précisent leur assiette (A-003-5, A-003-9, A-027-11).
- **Contrôle de fraude** : évaluation d'un virement selon des signaux (LARGE_AMOUNT, ROUND_AMOUNT, RAPID_SUCCESSION, HIGH_FREQUENCY, heure inhabituelle…), chacun avec un score de risque.
- **Seuil de score** : 75 par défaut. Un score cumulé strictement supérieur suspend le virement (FR-027-02).
- **Seuil de fraude** : 10 000 € par défaut. Un virement d'un montant supérieur ou égal est suspendu (FR-027-02) et sa notification est FRAUDE.
- **Seuil d'alerte** : 5 000 € par défaut. Un virement exécuté d'un montant supérieur ou égal reçoit la notification ALERTE au lieu de CONFIRMATION (FR-002-10).
- **Seuil de mise en attente** : 5 000 € par défaut, même valeur que le seuil d'alerte mais effet distinct : un virement exécuté d'un montant supérieur ou égal est enregistré au statut PENDING (FR-003-10). Les deux seuils sont deux paramètres séparés (NFR-SEC-08) et ne doivent pas être fusionnés.
- **PENDING** : valeur technique du statut enregistré d'un virement exécuté et débité dont le montant atteint le seuil de mise en attente ; pour la démonstration, elle reste annoncée « en attente » avec les textes legacy (BUG-009 conservé provisoirement, décision B1 révisée, corrigé par FR-003-29). Ce n'est pas l'état SUSPENDU.
- **Cumul glissant** : total des virements émis par un client sur les 24 heures glissantes, nouveau virement inclus ; au-delà du plafond (10 000 € par défaut), le virement est suspendu (FR-027-18).
- **Alerte de fraude** : enregistrement consultable par l'administrateur, produit par le contrôle de fraude en temps réel. Le traitement nocturne de détection de fraude (FEAT-019) ne crée pas d'alerte, il journalise son résultat (RG-019-007).
- **Virement suspendu** : virement que le contrôle de fraude a arrêté avant tout débit, en attente de la revue de l'administrateur, qui le libère ou le rejette avant l'heure butoir.
- **Revue** : action de l'administrateur qui libère ou rejette un virement suspendu, avec motif (FR-016-19), ou qui classe une alerte de fraude (FR-027-13, FR-027-14).
- **Heure butoir** : date et heure, 24 heures calendaires après la soumission d'un virement suspendu, avant laquelle il est libéré ou rejeté ; à défaut, il est rejeté automatiquement.
- **Notification** : message interne affiché au client dans l'application (CONFIRMATION, ALERTE, FRAUDE, solde faible…).
- **Prêt** : demande de crédit d'un client, avec un état (en attente, approuvé, refusé…) et un échéancier.
- **Compte tiers** : compte d'une autre banque, agrégé par le fournisseur d'open banking.
- **Fournisseur externe** : service tiers appelé par l'application : Azure OpenAI (chat conseiller), Powens (open banking), AlphaVantage (bourse).
- **Adaptateur simulé** : implémentation locale d'un fournisseur externe, sans réseau. Elle est active par défaut en local, dans les tests et dans les contrôles.
- **Traitement planifié** : traitement exécuté par le système à heure fixe ou à intervalle régulier.
- **Golden master** : ensemble des 33 scénarios enregistrés sur le legacy (`docs/retro/golden-master/`), dérivés pour la nouvelle application et rejoués une seule fois, développement terminé.
- **Contrôle** : vérification exécutée par un outil (test d'architecture, couverture, mutation, CVE, trace), jamais évaluée par un agent. La liste fait foi dans `docs/quality-gates.md`.

## 4. Fonctionnalités

Les 30 fonctionnalités du périmètre sont présentées dans l'ordre du corpus. Les règles marquées « À confirmer » dans le corpus sont reprises telles quelles (voir A-G-01).

