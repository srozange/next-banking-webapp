# Revue « conditions qui se recouvrent » — PRD next-banking-webapp (2026-10-05)

Relecteur : conditions qui se recouvrent. Périmètre : tout `prd.md` (331 FR, 30 fonctionnalités), confronté à `docs/retro/features/FEAT-NNN/RG.md` (637 RG, dont 161 Confirmé, 71 Suspect, 405 À confirmer) et aux décisions RG-027-003 (§9, addendum A.3 et A.7). `prd.md` et `addendum.md` n'ont pas été modifiés.

## Verdict

**À reprendre avant finalisation.** 8 critical, 6 high, 4 medium, 7 low.

La décision RG-027-003 est bien reprise dans les FR qui la citent. Les contradictions viennent surtout des FR voisines restées écrites pour l'ancien modèle « un virement est exécuté puis remis au traitement asynchrone ». C'est le cas du traitement asynchrone (FEAT-023), des notifications (FEAT-025), de l'audit (FEAT-026), des virements programmés (FEAT-021) et de la revue des alertes (FEAT-016 / FEAT-027) : elles ne renvoient pas aux nouvelles FR, et ces nouvelles FR ne les citent pas non plus. Deux contradictions ne touchent pas la fraude : le solde faible (FEAT-022 contre FEAT-025) et l'auteur de la revue d'une alerte (FEAT-016 contre FEAT-027).

## Méthode

1. Script `parse.py` : extraction des 637 RG (identifiant, statut, texte, renvois vers d'autres RG). Trois RG seulement renvoient à une autre RG : RG-011-009 vers RG-011-008 (exception explicite, conforme), RG-026-003 vers RG-026-010 (note sans effet opposé) et RG-030-002 vers RG-030-001 (exclusion explicite, conforme). Elles ont été traitées en premier.
2. Script `fr.py` : index des 331 FR (sources RG, BUG et renvois FR-xxx-yy), table RG vers FR. Chaque RG Confirmé est citée par au moins une FR.
3. Comparaison deux à deux des 161 RG Confirmé, regroupées par déclencheur : accès, notifications, seuils, fraude, audit, Powens, documents, bourse et chat.
4. Lecture intégrale des sections FEAT-002, 003, 005, 010, 016, 019, 021, 022, 023, 024, 025, 026, 027 et 030, ainsi que de §6, §7 et §9. Pour les autres fonctionnalités, lecture ciblée des grappes de FR voisines : Powens (FR-012, FR-014, FR-028), documents (FR-008, FR-017), prêts (FR-007, FR-016, FR-024), traitements (FR-011-09, FR-016-17, FR-018-05).
5. Pour chaque paire candidate, vérification par script des renvois mutuels (« FR-A cite FR-B », « FR-B cite FR-A »).

Règle appliquée : deux règles dont les conditions peuvent être vraies en même temps et dont les effets s'opposent forment une contradiction. Elle est acceptée seulement si la règle générale énonce l'exception (« sauf », renvoi), ou si les deux FR se renvoient l'une à l'autre. Sinon, le finding est critical.

---

## Findings critical

### C1 — Un virement suspendu n'est pas remis au traitement asynchrone, mais c'est ce traitement qui doit lui envoyer la notification FRAUDE
- **Identifiants :** RG-023-001 (Confirmé, publication après validation) × RG-023-004 / RG-025-003 (Confirmé, notification FRAUDE à partir de 10 000 €). Dans le PRD : FR-023-01 et FR-003-13 × FR-023-03, FR-025-01 et FR-023-04.
- **Situation commune :** un virement est suspendu (score cumulé > 75 ou montant ≥ 10 000 €).
- **Contradiction :** FR-023-01 et FR-003-13 disent qu'un virement suspendu « n'est pas remis à ce traitement ». FR-023-03 dit que « le traitement asynchrone ajoute à l'émetteur […] virement suspendu : FRAUDE ». FR-023-04 et FR-027-04 exigent cette notification. Le traitement est censé produire une notification pour un virement qu'il ne reçoit jamais. Aucun renvoi entre FR-023-01 et FR-023-03, ni entre FR-023-01 et FR-025-01. L'addendum A.7 parle d'« événements […] vers les notifications et la file de revue », ce qui est un autre canal que celui de FR-023-03.
- **Correction proposée :** dans FR-023-03 et FR-025-01, nommer le producteur de la notification FRAUDE : l'événement de décision du contrôle (A.7), et non la remise de FR-023-01. Ajouter dans FR-023-01 : « sauf la notification FRAUDE d'un virement suspendu, produite à la décision (FR-027-04) ». Faire se renvoyer FR-023-01 et FR-023-03.

### C2 — Un virement libéré déclenche une seconde notification que FR-002-10 interdit
- **Identifiants :** RG-023-001 / RG-025-001 / RG-025-002 (Confirmé) × RG-027-003 (décision de finalisation, A-027-5). Dans le PRD : FR-023-01 + FR-023-03 / FR-025-01 × FR-002-10 et FR-027-16.
- **Situation commune :** l'administrateur libère un virement suspendu, qui passe à EXÉCUTÉ (FR-016-19, FR-027-16).
- **Contradiction :** FR-023-01 remet au traitement asynchrone tout virement exécuté, « immédiatement ou après libération ». Pour tout virement exécuté, FR-023-03 et FR-025-01 ajoutent une notification CONFIRMATION (moins de 5 000 €) ou ALERTE (à partir de 5 000 €). Or FR-002-10 dit que « la libération […] ne produi[t] pas de nouvelle notification », et FR-027-16 dit « sans nouvelle notification ». L'un ajoute une notification, l'autre l'interdit. FR-002-10 ne renvoie ni à FR-023-03 ni à FR-025-01.
- **Correction proposée :** dans FR-023-03 et FR-025-01, ajouter « sauf pour un virement exécuté après libération, qui ne produit aucune notification (FR-002-10) ». Dans FR-002-10, renvoyer à FR-023-01 et FR-023-03. Ajouter le cas au critère d'acceptation de FR-023-03.

### C3 — Un virement exécuté dont la remise échoue doit être annulé, mais EXÉCUTÉ est un état terminal
- **Identifiants :** RG-023-002 (Suspect, BUG-073) × RG-027-003 (décision D5). Dans le PRD : FR-023-02 × FR-027-16 (FR-003-13 renvoie à FR-023-02, mais FR-023-02 ne renvoie pas à FR-027-16).
- **Situation commune :** un virement vient d'être exécuté, à la soumission ou après libération, et sa remise au traitement asynchrone échoue.
- **Contradiction :** FR-023-02 (A-023-2) « refuse le virement » et annule son débit, son crédit et ses opérations. FR-027-16 dit qu'« EXÉCUTÉ […] [est] terminal ; toute autre transition est refusée », et aucun des cinq états ne correspond à « refusé pour échec de remise ». Dans le cas d'une libération, FR-023-02 informe aussi « le client sur l'écran de virement », alors que c'est l'administrateur qui agit, et que FR-002-10 ne prévoit aucune notification.
- **Correction proposée :** choisir une règle et l'écrire dans les deux FR. Soit la remise (outbox, A.7) fait partie de l'exécution, et son échec empêche le passage à EXÉCUTÉ : préciser l'état atteint (REFUSÉ_ERREUR_CONTRÔLE ou un sixième état). Soit l'outbox rend l'échec impossible, et FR-023-02 devient sans objet. Préciser aussi le cas « après libération » (qui est informé, et comment) et faire se renvoyer FR-023-02 et FR-027-16.

### C4 — Un virement programmé de 10 000 € ou plus est exécuté sans contrôle, alors qu'il devrait toujours être suspendu
- **Identifiants :** RG-023-009 / décision RG-027-003 D2 × RG-021-005 (débit à l'échéance). Dans le PRD : FR-027-02 (« Aucun virement n'est exécuté sans que le contrôle ait rendu son issue »), FR-023-04 et FR-003-14 (« toujours suspendu ») × FR-021-03.
- **Situation commune :** un virement programmé de 15 000 € arrive à échéance, avec un solde suffisant.
- **Contradiction :** FR-021-03 débite et crédite. FR-027-02 et FR-023-04 disent « un virement », sans restriction, et exigent le contrôle et la suspension. L'exception existe seulement dans une note de FEAT-021 (« Ce traitement n'appelle pas le contrôle de fraude »), du côté de l'exception. La règle générale ne la cite pas. FR-002-10 (« Lorsqu'un virement est traité […] exactement une notification ») ne dit pas non plus si une exécution programmée produit une notification.
- **Correction proposée :** soit restreindre FR-027-02, FR-023-04, FR-003-14 et FR-002-10 à « un virement soumis depuis l'écran de virement (FEAT-003) », avec « sauf les virements programmés (FEAT-021) » et un renvoi réciproque. Soit, si c'est l'intention métier, soumettre FR-021-03 au contrôle. C'est une décision à faire trancher : sans elle, le découpage (R-1) peut passer par les virements programmés.

### C5 — Solde faible : vingt notifications par jour (FEAT-022) ou une seule jusqu'au retour au-dessus du seuil (FEAT-025)
- **Identifiants :** RG-022-004 / RG-022-010 (BUG-071) × RG-025-011 (BUG-079). Dans le PRD : FR-022-02, FR-022-03 et FR-022-04 × FR-025-08 (A-025-1 et A-025-2).
- **Situation commune :** le solde d'un compte d'un client actif reste à 50 € pendant plusieurs passages du contrôle de 30 minutes.
- **Contradiction :** FR-022-02 et FR-022-04 créent une notification à chaque passage, dans la limite de 20 par jour. Le test attend « exactement 20 notifications » pour 48 passages. FR-025-08 (A-025-2) en crée une seule tant que le solde reste sous le seuil. Les effets sont opposés : notifier ou ne pas notifier au deuxième passage. Le type diffère aussi : ACCOUNT_BALANCE et priorité HIGH (FR-022-03) contre ALERTE (A-025-1). Aucune des FR ne cite l'autre.
- **Correction proposée :** fusionner en une seule règle, citée par les deux fonctionnalités : FR-025-08 renvoie à FR-022-02 à FR-022-04, ou l'inverse. Supprimer l'assomption qui contredit l'autre. Fixer un seul type affiché.

### C6 — Auteur de la revue d'une alerte : nom complet ou identifiant
- **Identifiants :** RG-016-030 / RG-016-031 (À confirmer, « full name ») × RG-027-015 / RG-027-016 (Confirmé, « reviewer username »). Dans le PRD : FR-016-15 × FR-027-13 et FR-027-14.
- **Situation commune :** l'ADMIN clique sur « Confirmer » ou « Faux positif » sur une alerte OPEN.
- **Contradiction :** FR-016-15 enregistre « le nom complet de l'administrateur ». FR-027-13 et FR-027-14 enregistrent « l'identifiant de l'ADMIN réviseur ». Le même champ, affiché dans « Traité par {reviewedBy} » (FR-016-14), reçoit deux valeurs incompatibles. Aucun renvoi entre ces FR, et aucune mention « Écart dans le corpus », alors que le PRD en écrit ailleurs.
- **Correction proposée :** trancher (la RG Confirmé RG-027-015 dit username) et fusionner FR-016-15 avec FR-027-13 et FR-027-14, ou les faire se renvoyer avec une note d'écart.

### C7 — « Exécute le virement dès la soumission » contre la suspension et le refus
- **Identifiants :** RG-003-009 × RG-027-003 (D1, D3). Dans le PRD : FR-003-08 × FR-027-02.
- **Situation commune :** tout virement soumis.
- **Contradiction :** FR-003-08 dit « le système exécute le virement dès la soumission du formulaire ». FR-027-02 suspend ou refuse certains virements soumis. FR-003-08 n'a pas été réécrite lors de la décision, et elle ne cite pas FR-027-02.
- **Correction proposée :** réécrire FR-003-08 : « Aucune étape de confirmation n'est demandée à l'utilisateur ; l'issue du virement (exécuté, suspendu, refusé) est fixée par FR-027-02. »

### C8 — Actions rapides du chat masquées « pour le reste de la session » mais réaffichées par « Nouvelle conversation » (contradiction du corpus)
- **Identifiants :** RG-010-007 (Confirmé) × RG-010-022 (Confirmé) avec RG-010-006 (Confirmé). Dans le PRD : FR-010-04 × FR-010-10.
- **Situation commune :** l'utilisateur clique sur une action rapide, puis sur « Nouvelle conversation ».
- **Contradiction :** dans le corpus, RG-010-007 masque les boutons « for the remainder of the session ». RG-010-022 et RG-010-006 ramènent une conversation qui ne contient que le message de bienvenue, donc les boutons s'affichent. Les effets sont opposés (masquer ou afficher) et aucune RG ne nomme l'exception. Le PRD résout le cas sans le dire : FR-010-04 remplace « session » par « conversation » sans note d'écart, et seule FR-010-10 renvoie à FR-010-04, pas l'inverse.
- **Correction proposée :** dans FR-010-04, écrire « pour le reste de la conversation, sauf “Nouvelle conversation” (FR-010-10) ». Ajouter une note « Écart dans le corpus : RG-010-007 dit “session”, en contradiction avec RG-010-022 ». La règle Confirmé est reformulée : la faire confirmer.

---

## Findings high

### H1 — Revue des alertes et revue du virement suspendu : deux décisions indépendantes sur la même suspicion
- **Identifiants :** RG-027-015 / RG-027-016 (Confirmé) et RG-016-030 / RG-016-031 × RG-027-003 (D6, D8). Dans le PRD : FR-027-13, FR-027-14 et FR-016-15 × FR-016-19 et FR-027-17.
- **Situation commune :** un virement suspendu par le score, dont les alertes OPEN figurent dans la même page d'administration.
- **Problème :** l'ADMIN peut classer toutes les alertes en FALSE_POSITIVE sans libérer le virement. Celui-ci est alors rejeté automatiquement 24 heures plus tard, alors que la suspicion a été levée. À l'inverse, l'ADMIN peut confirmer la fraude (CONFIRMED) puis libérer le virement. Aucune FR ne relie les deux actions. FR-016-19 cite les « signaux et score » dans la file sans renvoyer à FR-027-13 ou FR-027-14, et ces deux FR ne citent pas FR-016-19. Les effets ne s'opposent pas au sens strict, mais deux règles s'appliquent à la même situation sans lien explicite.
- **Correction proposée :** dire explicitement si la revue d'une alerte a un effet sur le virement (par exemple : « classer en faux positif ne libère pas le virement ; seule FR-016-19 le libère »). Sinon, lier les deux actions. Faire se renvoyer les FR.

### H2 — Libération après l'échéance de 24 heures, avant le passage du traitement planifié
- **Identifiants :** RG-027-003 (D6 : libération par l'ADMIN) × RG-027-003 (D7, D8 : rejet à 24 heures). Dans le PRD : FR-016-19 × FR-027-17 (A-027-8).
- **Situation commune :** à 24 h 05 min après la soumission, le traitement planifié n'est pas encore passé, et le virement est toujours SUSPENDU. L'ADMIN le libère.
- **Contradiction :** FR-016-19 permet de libérer « un virement suspendu ». FR-027-17 exige le rejet du virement « qui n'a été ni libéré ni rejeté 24 heures […] après sa soumission ». Les effets sont opposés : exécuter ou rejeter. A-027-8 ne couvre que le cas où les deux surviennent « au même moment », et FR-016-19 ne cite pas FR-027-17.
- **Correction proposée :** dans FR-016-19, ajouter « sauf si l'heure butoir est dépassée : la libération est alors refusée (FR-027-17) ». Ou l'inverse, mais une seule règle, et renvoi réciproque.

### H3 — Audit du virement : à la soumission (FEAT-026) ou à l'exécution (FEAT-003)
- **Identifiants :** RG-026-008 (Confirmé, « successfully submitted ») × RG-003-016 (« successfully executed »). Dans le PRD : FR-026-03 × FR-003-11.
- **Situation commune :** un virement suspendu puis libéré ou rejeté, ou un virement refusé pour erreur.
- **Problème :** selon FR-026-03, un virement suspendu (« soumis avec succès » ?) est audité à la soumission. Selon FR-003-11, il l'est seulement s'il est exécuté, donc au moment de la libération, avec un utilisateur non précisé. Le même virement peut produire 0, 1 ou 2 entrées VIREMENT. Aucun renvoi entre les deux FR.
- **Correction proposée :** fusionner les deux FR et fixer le moment (soumission ou exécution) pour chacun des cinq états de FR-027-16.

### H4 — Listes fermées de l'audit contre les nouvelles entrées d'audit de la décision RG-027-003
- **Identifiants :** RG-016-038 / RG-026-004 à RG-026-007 (Confirmé) × RG-027-003 (D6, A-027-9). Dans le PRD : FR-016-06 (quatre codes), FR-026-05 (« huit codes ») et la note FEAT-026 (« Les traitements système […] ne sont pas audités ») × FR-016-19 et FR-027-17.
- **Situation commune :** une libération ou un rejet par l'ADMIN, et le rejet automatique par le traitement planifié.
- **Contradiction :** la note de FEAT-026 exclut l'audit des traitements système, alors que FR-027-17 audite le rejet automatique. FR-026-05 énumère huit codes, sans code pour la libération, le rejet ou le rejet automatique. FR-016-06 limite les actions administratives auditées à quatre codes. Aucun renvoi vers FR-016-19 ni FR-027-17.
- **Correction proposée :** ajouter les codes (par exemple ADMIN_TRANSFER_RELEASE, ADMIN_TRANSFER_REJECT, TRANSFER_AUTO_REJECT) dans FR-026-02 et FR-026-05. Dans la note de FEAT-026, écrire « sauf le rejet automatique (FR-027-17) ». Faire renvoyer FR-016-06 à FR-016-19.

### H5 — Approbation ou refus d'un prêt par l'ADMIN contre la décision du service de scoring
- **Identifiants :** RG-016-009 / RG-016-010 × RG-024-003 à RG-024-013 et RG-007-031. Dans le PRD : FR-016-04 et FR-016-05 × FR-024-03, FR-024-04, FR-024-05 et FR-007-10 / FR-025-03.
- **Situation commune :** un prêt est EN_ATTENTE (par exemple IMMOBILIER de 900 000 €), et le scoring n'est pas encore passé.
- **Contradiction :** l'ADMIN peut l'approuver (FR-016-04) alors que le scoring le refuserait (FR-024-04). L'effet est opposé et l'issue dépend de l'ordre d'arrivée. Les gardes « ignoré si non EN_ATTENTE » évitent le double traitement, mais aucune FR ne dit laquelle des deux décisions prime. Pour les notifications : FR-007-10 notifie « quand un prêt est approuvé » (toute approbation), FR-025-03 seulement quand c'est le scoring (FEAT-024), et FR-016-04 ne dit rien.
- **Correction proposée :** dans FR-016-04 et FR-016-05, renvoyer à FEAT-024 et dire si l'ADMIN peut passer outre le scoring. Aligner FR-007-10 et FR-025-03 sur la notification d'une décision de l'ADMIN.

### H6 — Clé Azure OpenAI avec « valeur par défaut » contre l'absence de secret dans le code
- **Identifiants :** RG-010-013 × BUG-034 / BUG-091. Dans le PRD : FR-010-08 × NFR-SEC-06 (et le modèle de FR-009-14 / FR-029-05).
- **Situation commune :** la variable d'environnement et le paramètre système de la clé sont absents.
- **Contradiction :** FR-010-08 prévoit « priorité : variable d'environnement, puis paramètre système, puis valeur par défaut » pour la clé. NFR-SEC-06 interdit tout secret dans le code. Pour AlphaVantage, FR-009-14 et FR-029-05 écartent explicitement la valeur par défaut ; FR-010-08 ne le fait pas.
- **Correction proposée :** dans FR-010-08, ajouter « sauf la clé, sans valeur par défaut (NFR-SEC-06) », sur le modèle de FR-029-05.

---

## Findings medium

### M1 — La définition d'un virement « exécuté » n'exclut pas le dépassement du cumul glissant
- **Identifiants :** RG-027-002 / RG-027-003 (D1, D2) × RG-027-003 (D10). Dans le PRD : FR-027-02 (« exécuté : score ≤ 75 et montant < seuil ») et FR-002-10 (« virement suspendu (score > 75, ou montant ≥ seuil) ») × FR-027-18.
- **Situation commune :** un virement de 4 000 €, de score 0, après 6 500 € émis dans les 24 heures, une fois FR-027-18 livrée.
- **Problème :** les conditions « exécuté » de FR-027-02 sont vraies, et la condition « suspendu » par le cumul l'est aussi. FR-027-02 et FR-027-18 se renvoient l'une à l'autre, mais la liste « exécuté » n'énonce pas l'exception, et FR-002-10 ne cite pas le cumul.
- **Correction proposée :** dans FR-027-02, écrire « exécuté : aucune condition de suspension n'est remplie » ; dans FR-002-10, ajouter « ou dépassement du cumul (FR-027-18) ».

### M2 — « Une seule notification par virement » (FR-003-14) contre « aucune notification » (FR-003-27)
- **Identifiants :** RG-003-019 / RG-003-021 / RG-003-022 × RG-027-003 (D3, A-003-8). Dans le PRD : FR-003-14 × FR-003-27.
- **Situation commune :** un virement refusé pour erreur du contrôle.
- **Problème :** FR-003-14 dit « l'utilisateur […] reçoit […] une seule notification par virement », sans exception. FR-003-27 dit « aucune notification ». FR-002-10 énonce bien l'exception, mais FR-003-14 et FR-003-27 ne se citent pas.
- **Correction proposée :** dans FR-003-14, ajouter « sauf refus pour erreur du contrôle (FR-003-27) ».

### M3 — Le plafond de 20 notifications peut effacer l'avis de suspension « unique »
- **Identifiants :** RG-023-010 / RG-025-007 (Confirmé) × RG-027-003 (finalisation, A-027-5) et RG-022-010. Dans le PRD : FR-025-04 et FR-022-04 × FR-027-04 et FR-002-10.
- **Situation commune :** un client dont le virement est suspendu a aussi un compte sous 100 €.
- **Problème :** FR-022-04 permet jusqu'à 20 notifications de solde faible par compte et par jour. FR-025-04 supprime la plus ancienne au-delà de 20. L'unique notification FRAUDE, qui porte l'heure butoir et la règle du rejet automatique (FR-027-04), peut ainsi disparaître en quelques heures. Le PRD fait pourtant de cette notification le seul canal de la règle.
- **Correction proposée :** dire explicitement si l'avis de suspension échappe au plafond, ou renvoyer à C5 (une seule notification de solde faible) pour réduire le risque. Faire renvoyer FR-027-04 à FR-025-04.

### M4 — Portée de FR-002-10 face aux notifications des virements programmés
- **Identifiants :** RG-002-021 à RG-002-024 × RG-004-061 / RG-021-007 (BUG-014, BUG-068). Dans le PRD : FR-002-10 × FR-004-12 et FR-021-04.
- **Situation commune :** un virement programmé non exécuté (solde insuffisant ou compte introuvable), ou exécuté.
- **Problème :** FR-002-10 dit « Lorsqu'un virement est traité […] exactement une notification, de type CONFIRMATION, ALERTE ou FRAUDE », avec un type choisi selon l'issue du contrôle et le montant. FR-004-12 produit une ALERTE quel que soit le montant, FR-021-04 une notification de refus, et une exécution réussie ne produit rien. FR-002-10 ne restreint pas sa portée.
- **Correction proposée :** restreindre FR-002-10 aux virements de FEAT-003, en renvoyant à FR-004-12 et FR-021-04 (même correction que C4).

---

## Findings low

- **L1 — PENDING et texte ALERTE « en attente de validation » pour un virement exécuté.** RG-025-002 (Confirmé) et RG-005-017 (Confirmé) × RG-027-003 D1. Dans le PRD : FR-003-10, FR-005-07 et FR-002-10 × FR-027-16. L'écart est reconnu par A-003-7 ; il reste à faire trancher, parce qu'une seconde revue à l'écran (badge « En attente ») pourrait laisser croire à un état SUSPENDU.
- **L2 — Bornes du cumul et du seuil de fraude.** FR-027-18 (« au-delà », strictement supérieur) dit le plafond « aligné » sur le seuil de fraude (supérieur ou égal, FR-027-02). Aucune issue opposée, puisqu'un virement seul de 10 000 € est suspendu par le montant. Préciser que l'« alignement » porte sur la valeur, pas sur la borne.
- **L3 — Descriptions de FEAT-023 et FEAT-025 obsolètes.** « type dépend du montant […] FRAUDE au-delà » et « CONFIRMATION, ALERTE ou FRAUDE selon le montant » contredisent FR-023-03 et FR-025-01 (issue du contrôle d'abord). À réécrire.
- **L4 — Homonymie « suspendu ».** Les virements programmés « inactifs (suspendus) » (FR-021-02, FR-004, FR-021-04 / A-021-3) côtoient l'état SUSPENDU de FR-027-16. Renommer « inactif » pour les virements programmés.
- **L5 — Le traitement de rejet automatique est absent de NFR-BATCH-01.** NFR-BATCH-01 liste « les » traitements planifiés sans celui de FR-027-17. NFR-BATCH-02 l'inscrira au journal, ce qui ajoute des entrées dans FR-016-17 (50 entrées, déjà saturées d'après les notes de FEAT-016 et FEAT-022).
- **L6 — Compte désactivé et signal MULTIPLE_FAILED_AUTH.** FR-001-07 (A-001-1) produit LOGIN_FAILED pour un compte désactivé. FR-027-11 lève donc une alerte de score 80 au 5e essai d'un client désactivé par l'ADMIN. Dire si ces échecs comptent.
- **L7 — Écran après soumission d'un virement suspendu.** FR-003-12 affiche le tableau de bord « après l'exécution » ; FR-027-04 parle d'information « à l'écran » sans le nommer. Renvoyer FR-003-12 à FR-027-04.

---

## Paires examinées sans finding (liens explicites conformes)

- RG-011-008 × RG-011-009 : exception énoncée (« exception à RG-011-008 »), reprise dans FR-011-04.
- RG-030-001 × RG-030-002 : exclusion explicite ; FR-030-01 renvoie à FR-001-01 et FR-001-02.
- RG-027-014 (OPEN seulement) × BUG-056 : écart écrit dans FR-027-12, qui renvoie à FR-016-16.
- RG-019-007 × RG-027-002 : FR-019-05 renvoie au contrôle en temps réel (FEAT-027).
- FR-027-02 : erreur du contrôle contre montant de 10 000 € ou plus, réglé par « quel que soit le montant ».
- FR-003-10 (PENDING) × FR-027-02 (SUSPENDU) : renvoi explicite, avec un test aux bornes.
- FR-002-10 × FR-003-27 : aucune notification pour un refus, énoncé dans la règle générale.
- FR-016-04 et FR-016-05 : clause « Sauf si le prêt n'a plus le statut EN_ATTENTE » ; FR-022-04 « prime sur FR-022-02 ».
- Grappe Powens (FR-012-06, 07, 08, 09 et 14, FR-014-02 à FR-014-06, FR-028-03 à FR-028-06 et FR-028-11) : ordre de priorité unique (FR-014-02), renvois croisés complets.
- Documents (FR-008-04 et FR-008-11 × FR-017-02, 03, 08 et 09) : 403 contre 404 explicités dans les deux sens.
- RG-023-010 × RG-025-007 (plafond 20) et RG-023-011 × BUG-006 : écarts écrits (FR-023-05, FR-023-06).
- RG-027-002 × RG-027-013 (avertissement « N alerte(s) ») : remplacement explicite par FR-003-16 et FR-027-04.
- RG-001-001, RG-005-001, RG-008-001, RG-010-001, RG-015-001 et RG-016-001 (redirections vers la connexion) : effets identiques.
