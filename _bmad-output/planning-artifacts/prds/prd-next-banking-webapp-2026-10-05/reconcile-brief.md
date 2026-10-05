# Réconciliation du brief avec le PRD (input reconciliation)

Entrée : `_bmad-output/planning-artifacts/briefs/brief-next-banking-webapp-2026-10-05/brief.md` (+ `addendum.md`).
Cible : `prd.md` et `addendum.md` du présent dossier. La couverture RG/BUG (637 RG, 50 bugs) est validée par script et n'est pas recomptée. Le PRD et son addendum ne sont pas modifiés.

| # | Élément du brief | Où dans le PRD | État | Correction proposée |
|---|---|---|---|---|
| 1 | Critère « 31 scénarios applicables sur 33, 2 exclus car ils reproduisent des bugs » | NFR-PAR-01, SM-1 (§7.2, §8) | partiel / contradiction interne | Présent (31 / 2 exclus : SC-003-03, SC-010-02). Mais l'addendum A.7 dit que SC-003-01, -02 et -04 divergent aussi du legacy (exception ou exclusion à décider en G2), et la ligne 1121 du PRD dit que SC-010-02 reste vrai après BUG-038. Aligner : soit 31 devient « 31 sous réserve de G2 », soit lister les exclusions réelles dans NFR-PAR-01 et SM-1. |
| 2 | Chiffre « 637 RG couvertes par une exigence et un test » | NFR-PAR-03, SM-2 | partiel | Le PRD dit « chaque règle reprise » sans citer 637. Ajouter le chiffre, ou préciser l'écart (règles « dette legacy » non reproduites, A-G-02) entre 637 et les règles reprises. |
| 3 | Session fixation non corrigée (HAUTE) | NFR-SEC-02, FR-001-09 | présent | Aucune. |
| 4 | IDOR `/docs/download?accountId=N` (HAUTE) | NFR-SEC-03, FR-008 (BUG-027), FR-017 (BUG-058) | présent | Aucune. Ajouter le mot « IDOR » et les niveaux de gravité (HAUTE / MOYENNE / FAIBLE) du brief dans §7.3 pour la traçabilité. |
| 5 | IDOR sur virements programmés, IDs arbitraires (HAUTE) | NFR-SEC-03, FR-004 (BUG-013) | présent | Aucune. |
| 6 | Message d'erreur JSF expose la pile Java (MOYENNE) | NFR-SEC-04 | présent | Aucune (la pile est citée : « pile d'appels »). |
| 7 | `h:messages globalOnly="true"` masque les erreurs de champ en mode Production (FAIBLE) | FR-001-03 (note l.182), FR-003 (l.473) | partiel | Les cas Connexion et Virement sont traités, mais aucune exigence transverse. Ajouter à §7.3 ou §7.8 : « les erreurs de validation de champ sont visibles à l'utilisateur dans tous les modes de déploiement ». |
| 8 | Logs sans masquage IBAN/montant (FAIBLE) | NFR-SEC-05, FR-027 (l.2354) | partiel | NFR-SEC-05 ne couvre que l'IBAN ; le brief cite aussi le montant. Étendre NFR-SEC-05 au montant, ou noter explicitement que le montant n'est pas masqué (décision). |
| 9 | Temps de réponse mesurés sur le legacy : 127 requêtes, min 1 ms, max 1 217 ms, moyenne 22 ms ; bourse 1 217 / 469 / 303 ms ; admin 48, bénéficiaires 38, budget 32, relevé 32 ms | §7.5 : « les temps du legacy sont une référence mesurée » | absent (chiffres) | Le PRD invoque la référence sans la donner. Ajouter une ligne ou un renvoi : mesures de référence (127 requêtes, 1 / 22 / 1 217 ms, pages ci-contre). |
| 10 | Seuils de temps cibles (200 / 500 / 3 000 / 2 000 ms) | NFR-PERF-01 | présent | Aucune. Noter que le brief les présente aussi comme « référence, pas cible » alors que le PRD les adopte en cible (A-G-04, déjà tracé). |
| 11 | Volumétrie de référence (transactions 45, loan_repayments 18, bank_products 13, … loans 2) | NFR-PAR-04 (cite seulement 7 clients, 7 comptes, 45 transactions) | partiel | Compléter NFR-PAR-04 : le jeu de données de référence couvre les 18 tables du brief avec leurs volumes (renvoi à `import.sql`), ou joindre le tableau en addendum. |
| 12 | Plages des batchs : INTEREST_CALCULATION, FRAUD_DETECTION, MONTHLY_STATEMENTS (NightlyBatchService), RECURRING_TRANSFERS (RecurringTransferScheduler), NOTIFICATION_CHECK (NotificationService) | NFR-BATCH-01 | partiel | Le brief ne donne pas d'horaires (seulement les noms) ; le PRD en donne (02h00, 02h30, 1er à 03h00, 5 min, 30 min), ce qui est cohérent avec le corpus. Ajouter dans NFR-BATCH-01 les noms de job du brief (clés du journal d'exécution) en face de chaque horaire. Les horaires du PRD ne sont pas vérifiables à partir du brief seul : à confirmer contre `docs/retro/00-global/NFR.md`. |
| 13 | Question ouverte : état DEFAUT sans transition (2 formulations dont `hasEcheancier`) | §4 FEAT-007 (l.839), FEAT-015 (l.1527), QO-2 | partiel | Traité par supposition. `hasEcheancier` (échéancier d'un prêt DEFAUT) n'est pas nommé : ajouter la décision (conserver ou retirer l'accès à l'échéancier en DEFAUT) dans FEAT-007 et dans l'index des suppositions. |
| 14 | Question ouverte : `downloadCount` jamais incrémenté | FR-008-12 (compteur de téléchargements), QO-2 | présent | Le PRD tranche (incrémenter). Vérifier qu'une supposition `A-008-x` est indexée au §10 ; sinon l'ajouter. |
| 15 | Question ouverte : `findSavingsProducts` / `findCreditProducts` / `findInvestmentProducts` sans appelant | QO-2 (« méthodes de catalogue sans appelant ») | partiel | Aucune FR ni supposition nommée. Ajouter dans FEAT-011 : « méthodes mortes non reproduites » avec supposition indexée au §10. |
| 16 | Question ouverte : `refreshAccounts()` non câblé | FR-012-16, A-012-5 | présent | Aucune. |
| 17 | Question ouverte : seuils SEUIL_FRAUDE_EUR / ALERTE / PENDING surchargés en prod ? | Addendum A.3, NFR-SEC-08, D2 | partiel | Les valeurs par défaut sont retenues en configuration. Consigner explicitement que la question « surchargés en production ? » reste sans réponse du métier (à ajouter à QO-2 ou à une nouvelle QO). |
| 18 | Question ouverte : `evaluateSingleScore()` déprécié, consommateur externe ? | QO-2 uniquement (nommé) | partiel | Aucune FR ni supposition associée. Ajouter la décision (non reproduite) dans FEAT-024 et au §10. |
| 19 | Question ouverte : valeur de `POWENS_CONNECTOR_ID` en production | QO-3, FR-028-12, l.2489 | présent | Aucune. |
| 20 | Questions « règles voisines » (logout, accès admin, motif de notification, LARGE_AMOUNT, ROUND_AMOUNT, RAPID_SUCCESSION, « Montant invalide », solde insuffisant programmé, redirection non authentifié, accès tous rôles, 20 transactions Powens) | FR fusionnées (« règles voisines » : FR-001-10/11, FR-004 l.569, l.1457) ; A-003-4 | partiel | Fusion faite pour les cas vus (RG-001-010, RG-030-010, RG-004-061, RG-021-004, RG-014-011, RG-028-018, LARGE_AMOUNT). Pour les autres (motif RG-003-020 / RG-023-007, « Montant invalide » RG-004-013 / RG-007-003 / RG-007-012, redirections non authentifiées, accès tous rôles) vérifier qu'une mention « fusionnée » ou « différence voulue » existe. Attention : RG-004-061 du corpus dit « sans notifier le client » alors que BUG-014 impose une notification ; le PRD suit BUG-014 (cohérent). |
| 21 | « 63 lacunes documentées » (`docs/retro/GAPS.md`) | Absent (seul A-G-07 cite GAPS.md) | absent | Ajouter en QO ou en §10 : « 63 lacunes du corpus (`docs/retro/GAPS.md`) : non bloquantes, traitées par suppositions A-G-xx » (à vérifier que chacune l'est). |
| 22 | Hors périmètre FEAT-031 et FEAT-032 | §5 Non-Goals | présent | Aucune. |
| 23 | Langue française de l'interface | NFR-UI-01 | présent | Aucune. |
| 24 | Périmètre : 32 features inventoriées (1 intégration, 10 transversales, 21 utilisateur), 30 dans le périmètre | §0 (30 fonctionnalités), §4, §5 | partiel | La décomposition 1/10/21 et le chiffre 32 ne figurent pas ; non bloquant. Ajouter une phrase en §0 si on veut la traçabilité. |
| 25 | Problème : pile Java EE 8 sur WildFly 14 (Java EE 7), pas de conteneurisation ni CI/CD, validation métier dans les backing beans (2 TODO JIRA-9200), pas d'API REST | §1 Vision (partiel : « pile vieillissante, sans API, sans conteneurisation ni intégration continue »), addendum A.1 | partiel (idées qualitatives) | Perte des détails WildFly 14 / Java EE 7 / JIRA-9200 (logique métier dans les backing beans). Ajouter en addendum une courte section « Problème du legacy » ; utile pour justifier la validation métier côté service (cf. BUG-093). |
| 26 | Absence de documentation du legacy ; corpus `docs/retro/` comme spécification de référence ; commit analysé `c05512ec…` | §0 (corpus cité) | partiel | Le commit de référence n'apparaît pas dans le PRD. Le citer en §0 pour la traçabilité. |
| 27 | « Les RG et validations font foi ; le golden master en est la preuve ; seule exception : les 50 bugs » | §1, NFR-PAR-01/02 | présent | Aucune. |
| 28 | Vision au-delà de la parité (cible 2-3 ans) « à définir à la découverte de bmad-prd » | §1 Vision (démonstrateur réaliste) | partiel | Le PRD limite la portée à un démonstrateur sans traiter l'horizon 2-3 ans. Ajouter une phrase « évolutions hors v1 : non définies » ou la renseigner. |
| 29 | Rôles ADMIN, CONSEILLER, CLIENT | §2 | présent | Aucune. |
| 30 | Addendum du brief : « chaque exigence cite la RG qu'elle couvre » | Lignes « Source : RG-… » de chaque FR | présent | Aucune. |
| 31 | Bug BUG-084 : « validé par le métier » mais dépend d'une question bloquante absente de GAPS.md | §9 note (l.2807) | présent | Aucune (incohérence déjà documentée). |

## Contradictions à retenir

- Cohérence golden master (lignes 1) : NFR-PAR-01 / SM-1 (31 scénarios, 2 exclus) contre addendum A.7 (SC-003-01, -02, -04 divergent) et PRD l.1121 (SC-010-02 reste vrai après BUG-038).
- Aucune contradiction directe PRD / brief sur les constats de sécurité, les temps de réponse cibles, les hors-périmètre ou la langue.

## Pertes principales (à corriger en priorité)

1. Mesures de référence de temps de réponse et volumétrie complète : annoncées mais non reprises (lignes 9, 11).
2. Gravités et deux constats sécurité partiels : `h:messages` en production et masquage du montant dans les logs (lignes 7, 8).
3. Questions ouvertes nommées dans QO-2 sans décision traçable : `findXxxProducts`, `evaluateSingleScore`, `hasEcheancier`, surcharge des SEUIL_* (lignes 13, 15, 17, 18).
4. 63 lacunes `GAPS.md` et commit de référence absents (lignes 21, 26).
