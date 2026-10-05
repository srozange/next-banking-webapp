# Réconciliation : décisions RG-027-003 (transcript de la table ronde) contre le PRD

Entrée : `_bmad-output/party-rg-027-003-script.md` (tour 4, blocs « Moi » en deux interventions, synthèse « Décisions prises » D1 à D12).
Cibles : `prd.md` (FR-002-10, FR-003-09/13 à 16/27/28, FR-016-19, FR-023-01/03/04, FR-025-01, FR-027-02/04/16/17/18, NFR-SEC-08/09, NFR-OBS-02, §6.1, §9, risques connus) et `addendum.md` (A.3, A.7). Aucun fichier modifié.
Verdict global : toutes les valeurs décidées sont reprises fidèlement. Aucune contradiction. Quelques écarts de forme et trois ajouts non décidés non marqués [ASSUMPTION].

## 1. Décisions D1 à D12

| Réf. | Décision | Où dans le PRD | Verdict | Remarque |
|---|---|---|---|---|
| D1 | Sous les seuils exécuté, au-dessus suspendu avant tout débit | FR-027-02, FR-003-09, FR-023-04, §9 D1 | fidèle | |
| D2 | Score strictement > 75 OU montant ≥ 10 000 € ; décision d'ici, config validée au démarrage | FR-027-02 (score <= 75 et montant < seuil = exécuté), FR-023-04 (test 10 000,00 suspendu, 9 999,99 exécuté), FR-027-03, NFR-SEC-08, §9 D2, A.3 | fidèle | Le « score cumulé » est une précision du PRD, cohérente avec FR-027-03. |
| D3 | Erreur du contrôle : refus pur quel que soit le montant, REFUSÉ_ERREUR_CONTRÔLE, rien débité, erreur journalisée ; option de John écartée | FR-027-02, FR-003-15, FR-003-09, A.7, §9 D3 | fidèle | L'option de John est citée comme écartée en A.7. |
| D4 | Pas de délai métier ; expiration technique de l'accès base = erreur qui retombe sur D3 ; valeur en architecture, pas dans le PRD ; config validée au démarrage | NFR-SEC-09, FR-027-02, A.7 (« à fixer dans la décision d'architecture »), §9 D4 | fidèle | Aucune valeur chiffrée dans le PRD (500 ms absent). §9 D4 ne dit pas « config validée » mais NFR-SEC-09 le dit : sans gravité. |
| D5 | Décision synchrone et locale, score derrière un port, cinq états, événements Kafka outbox | FR-027-16, FR-003-13, FR-023-01, A.7 | fidèle | D5 vient de Winston, non de « Moi » ; couvert par l'option P2 retenue. Transitions détaillées (SUSPENDU vers LIBÉRÉ ou REJETÉ, etc.) : déduction du PRD, cohérente. |
| D6 | ADMIN seul, motif obligatoire, identité tracée, pas de rôle conseiller ; libération refait le contrôle de solde | FR-016-19 (test CONSEILLER et CLIENT refusés, sans motif refusé, audit avec identité), FR-003-16, FR-027-16, §9 D6 | fidèle | |
| D7 | 24 h calendaires depuis la soumission, sans jours fériés, heure butoir précise affichée au client | FR-027-17, FR-027-04 (date et heure, soumission + 24 h), FR-016-19, §9 D7 | fidèle | Test aux bornes 23:59:59 / 24 h / 24 h + 1 s : détail de l'équipe (Amelia), non contraire. |
| D8 | Rejet automatique sans débit, traitement planifié ; nuit et week-end acceptés pour le démonstrateur, à revoir si le projet en sort | FR-027-17, §9 Risques connus R-2, A.7 | fidèle | Le « à revoir hors démonstrateur » est écrit en R-2. |
| D9 | Texte exact de l'écran de refus, rien sur la fraude, référence = identifiant de trace | FR-003-27 (texte repris mot pour mot avec {référence}), NFR-OBS-02, §9 D9 | fidèle | Le texte est identique à D9, y compris la ponctuation. |
| D10 | Cumul dans le projet, hors socle, story immédiatement après le socle ; 24 h glissantes, tous bénéficiaires ; au-delà de 10 000 €, le virement qui franchit le plafond est suspendu ; plafond en config aligné sur BUG-076 | FR-027-18 (marqué STORY SUIVANTE), §6.1 point 2, NFR-SEC-08, A.3, §9 D10 | fidèle | Test à 10 000,00 non suspendu / 10 000,01 suspendu : « au-delà » = strictement supérieur, lecture correcte. La base de calcul (A-027-7) est marquée [ASSUMPTION]. |
| D11 | Risque de découpage écrit comme tel dans le PRD | Risques connus R-1, FR-027-18, §9 D11 | fidèle | R-1 dit « accepté pour la durée du socle seulement ». |
| D12 | Reprise préremplie : story nommée, après le socle et après le cumul, hors socle | FR-003-28 (marqué STORY SUIVANTE), §6.1 point 3, §9 D12 | fidèle | |

## 2. Précisions des deux blocs « Moi »

| Précision | Où | Verdict | Remarque |
|---|---|---|---|
| Option P2 retenue | §9 intro, A.7 | fidèle | |
| Cumul : tous bénéficiaires, 24 h glissantes, plafond en config aligné BUG-076, juste après le socle | FR-027-18, §6.1 | fidèle | |
| Seuil : « strictement supérieur à 75 » et « supérieur ou égal à 10 000 € », décision d'ici, config validée au démarrage | FR-027-02, NFR-SEC-08 | fidèle | |
| ADMIN seul, motif obligatoire, identité tracée, pas de rôle conseiller | FR-016-19 | fidèle | |
| 24 h calendaires, heure butoir précise | FR-027-17, FR-027-04 | fidèle | |
| Rejet automatique sans débit à l'expiration | FR-027-17 | fidèle | |
| Refus pur quel que soit le montant, option de John écartée | FR-027-02, A.7 | fidèle | |
| Pas de délai de contrôle, calcul local ; 500 ms d'Amelia écartés (reprise seulement si service externe) | NFR-SEC-09, §9 D4, A.7 | partiel | La mention « si le contrôle devient un service externe, on reprendra 500 ms » n'est pas portée. Sans conséquence pour le démonstrateur ; à noter seulement si on veut garder l'historique. |
| Délai technique sur l'accès base, en architecture, chiffre dans la décision d'architecture et pas dans le PRD, simulé par l'adaptateur | NFR-SEC-09, FR-027-02 (cause d'erreur simulée), A.7 | fidèle | |
| Texte de l'écran de refus tel quel, minimum du socle ; référence = identifiant de trace pour retrouver le span fraud.check | FR-003-27, NFR-OBS-02 | fidèle | Le lien vers « span fraud.check » n'est pas cité : NFR-OBS-02 le remplace par « aucun span à la main, décidé en architecture » (contrainte quality-gates). Acceptable. |
| Reprise préremplie : story nommée après socle et après cumul | FR-003-28 | fidèle | La demande de Sally (champs saisis conservés au retour, propriétaire de la story) n'est pas une décision de « Moi » et n'est pas reprise : correct. |
| Rejet nuit/week-end noté à revoir hors démonstrateur | R-2, FR-027-17 | fidèle | |
| Découpage écrit noir sur blanc, story du cumul nommée juste derrière le socle | R-1, FR-027-18, §6.1 | fidèle | |
| Section « Ce que la décision change » : 13 exigences [EN ATTENTE RG-027-003] réécrites ; nouvelles exigences (machine d'états, Libérer/Rejeter, échéance, écran de refus, cumul) | 0 occurrence restante du marqueur ; FR-027-16/17/18, FR-016-19, FR-003-27 | fidèle | |

## 3. Ajouts du PRD non décidés par « Moi »

| Ajout | Où | Marqué [ASSUMPTION] ? | Verdict |
|---|---|---|---|
| Aucune notification pour un virement refusé pour erreur (écran seul) | FR-002-10, FR-003-27, FR-003-09 | non | ajout non décidé non marqué. Moi n'a rien décidé sur la notification d'un refus. À marquer ou à faire trancher. |
| Pas de seconde notification après libération, rejet ou rejet automatique | FR-002-10, FR-027-16, A-027-5 | oui (A-027-5) | conforme à la règle, mais attention : John a dit à la table que le client doit lire le rejet automatique « dans sa notification unique » ; l'assomption va à l'inverse (état vu à l'écran). À faire confirmer. |
| Concurrence libération contre expiration : « la première enregistrée gagne », sans double débit | FR-027-17 | non | ajout non décidé non marqué. Amelia a cité le cas de test, personne n'a tranché la règle. Règle de bon sens, mais c'est une décision. |
| Libération sur solde insuffisant : le virement passe REJETÉ | FR-027-16 | oui (A-027-6) | conforme |
| Rejet automatique tracé au journal d'audit | FR-027-17 | non | ajout mineur non marqué (Moi : identité tracée pour l'administrateur seulement). |
| Heure, décision et motif enregistrés au journal d'audit FEAT-026 | FR-016-19 | non | ajout mineur non marqué, extension raisonnable de « identité tracée ». |
| Colonnes et tri de la file de revue | FR-016-19 | oui (A-016-3) | conforme |
| Texte de l'avis de suspension | FR-027-04 | oui (A-027-4) | conforme |
| Base du cumul : virements exécutés et suspendus, pas les rejetés ni refusés | FR-027-18 | oui (A-027-7) | conforme |
| Champs préremplis (compte source, destinataire, montant, motif) | FR-003-28 | non | ajout mineur : la décision dit « reprise préremplie » sans la liste. |
| Suspension par dépassement du cumul comme motif affiché dans la file | FR-016-19, FR-027-02 | non | cohérent avec D10, sans objet. |
| Seuil de mise en attente 5 000 € cité dans NFR-SEC-08 | NFR-SEC-08, A.3 | non | pas un ajout RG-027-003 : valeur legacy (SEUIL_PENDING_EUR). RAS. |
| Mise à jour de §9 D4 | §9 | | RAS |

## 4. Écarts principaux à traiter

1. Notification d'un refus pour erreur : « aucune notification » (FR-002-10, FR-003-27) est une décision prise à la place de Moi, sans [ASSUMPTION].
2. A-027-5 (aucune notification après la revue ou l'expiration) va à l'opposé de la remarque de John sur « la notification unique » pour le rejet automatique de nuit : à faire arbitrer par le décideur, ou à garder marqué.
3. Règle « la première enregistrée gagne » entre libération et expiration (FR-027-17) : non marquée [ASSUMPTION].
4. Mineurs : audit du rejet automatique, liste des champs préremplis, mention des 500 ms conditionnels non portée.
