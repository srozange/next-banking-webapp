---
title: "PRD : next-banking-webapp (rénovation)"
status: draft
created: 2026-10-05
updated: 2026-10-05
inputDocuments:
  - "_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/brief.md"
  - "_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/addendum.md"
  - "docs/retro/ (corpus de rétro-ingénierie, commit c05512ecab8af8878d695ccd0f33fc3ece8811a4)"
  - "docs/quality-gates.md"
---

# PRD : next-banking-webapp (rénovation)

*Brouillon complet en mode Fast path, non finalisé. Les suppositions sont marquées `[ASSUMPTION]` et indexées en §10. Les exigences suspendues à la décision ouverte sur la règle RG-027-003 sont marquées `[EN ATTENTE RG-027-003]` (voir QO-1).*

## 0. Objet du document

Ce PRD décrit la rénovation de l'application bancaire existante (le « legacy », pile Java EE analysée au commit `c05512ecab8af8878d695ccd0f33fc3ece8811a4`) en une nouvelle application. Il s'adresse au PM, à l'architecte (`bmad-architecture`), à l'équipe qui découpera les epics et à la boucle de développement.

**Sources :**
- le brief produit (`_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/`) ;
- le corpus de rétro-ingénierie `docs/retro/`, qui fait foi pour le comportement fonctionnel ;
- `docs/quality-gates.md`, la référence unique de l'architecture cible et des contrôles.

**Structure :** un glossaire, puis les 30 fonctionnalités du périmètre (§4). Chaque fonctionnalité regroupe des exigences fonctionnelles numérotées `FR-NNN-xx`, où NNN est le numéro de la fonctionnalité du corpus (FEAT-NNN). La ligne `Source :` de chaque FR cite les règles de gestion (`RG-NNN-NNN`) et les bugs à corriger (`BUG-NNN`) qu'elle couvre. Les exigences non fonctionnelles transverses (§7) reprennent intégralement les contrôles de `docs/quality-gates.md`. L'architecture cible imposée et les détails techniques sont dans `addendum.md`.

## 1. Vision

La banque dispose d'une application en ligne fonctionnelle mais sans documentation, bâtie sur une pile Java EE vieillissante, sans API, sans conteneurisation ni intégration continue, et qui porte 50 comportements défectueux que le métier a décidé de corriger. La rénovation livre une application qui rend **exactement le même service** à ses trois populations (clients, conseillers, administrateurs), prouvé par le rejeu du golden master du legacy. Elle corrige ces 50 bugs, et chaque correction est démontrée par un test.

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

- **UJ-1. Claire, cliente, vire 1 200 € à son colocataire.** Elle se connecte, voit son tableau de bord (soldes, dernières opérations du compte courant), ouvre le virement, choisit le compte source et le bénéficiaire, saisit le montant et un motif, puis valide. Le contrôle de fraude s'exécute avant la validation finale. Elle reçoit une notification dans l'application. **Cas limite :** solde insuffisant → refus avec un message clair et non technique (BUG-008, BUG-011). Le sort d'un virement jugé suspect dépend de QO-1. Réalise FEAT-001, FEAT-002, FEAT-003, FEAT-025, FEAT-027.
- **UJ-2. Marc, conseiller, prépare un rendez-vous.** Il ouvre son espace conseiller, ne voit que les clients qui lui sont affectés, consulte la situation de l'un d'eux et la date à laquelle il est devenu client de la banque (BUG-055). Réalise FEAT-015, FEAT-030.
- **UJ-3. Sophie, administratrice, traite la file du matin.** Elle consulte les alertes de fraude de la nuit, toutes statuts confondus (BUG-056), approuve ou refuse les prêts en attente, désactive un client. Ce client ne peut plus se connecter (BUG-001). Chaque action est auditée avec l'adresse IP d'origine (BUG-083). Réalise FEAT-016, FEAT-019, FEAT-026.
- **UJ-4. Claire agrège son compte d'une autre banque.** Depuis « Comptes tiers », elle est redirigée vers le fournisseur d'open banking. Au retour, elle voit ses comptes externes et leurs 20 dernières opérations. Un solde absent est distingué d'un solde nul (BUG-042). En cas d'indisponibilité du fournisseur, elle en est informée (BUG-043). Réalise FEAT-012, FEAT-014, FEAT-028.

[ASSUMPTION A-G-06 : les quatre parcours ci-dessus sont illustratifs, reconstitués depuis le corpus ; aucun parcours n'a été raconté par le métier]

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
- **Virement programmé** : virement récurrent avec une fréquence et une prochaine date d'exécution, exécuté par un traitement planifié.
- **Bénéficiaire externe** : destinataire hors banque, identifié par un IBAN, enregistré par un client.
- **Contrôle de fraude** : évaluation d'un virement selon des signaux (LARGE_AMOUNT, ROUND_AMOUNT, RAPID_SUCCESSION, HIGH_FREQUENCY, heure inhabituelle…), chacun avec un score de risque.
- **Alerte de fraude** : enregistrement consultable par l'administrateur, produit par le contrôle de fraude en temps réel. Le traitement nocturne de détection de fraude (FEAT-019) ne crée pas d'alerte, il journalise son résultat (RG-019-007).
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

