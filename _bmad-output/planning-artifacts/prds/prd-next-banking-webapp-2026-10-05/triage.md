# Journal de tri (finalisation, étape 4)

Date : 2026-10-05. Consigne du décideur : ne soumettre que les bloquants pour l'architecture ou les epics ; aucun bloquant retenu.

| # | Source | Élément | Décision | Où |
|---|---|---|---|---|
| 1 | review-rubric | medium — suppositions risquées sans propriétaire ni échéance | corrigé | §10 : propriétaire et reprise sur chaque entrée |
| 2 | review-rubric | medium — indicateurs sans mesure de la fraude refondue ; SM-5 limité à UJ-1 | corrigé | §8 : SM-5 étendu, SM-6 ajouté |
| 3 | review-rubric | medium — journal admin saturé (FR-016-17, 48 entrées par jour) | reporté | §9 RP-01 |
| 4 | review-rubric | medium — « transaction » ambigu dans HIGH_FREQUENCY / VELOCITY_CHECK | supposition | A-027-11 (FR-003-18, FR-027-06) ; glossaire « Transaction » |
| 5 | review-rubric | medium — classement de suppositions (A-027-6, A-027-7…) | reporté | §9 RP-02 (niveaux de risque non modifiés, consigne) |
| 6 | review-rubric | medium — A-027-9 fusionne deux sujets | corrigé | A-027-9 (audit, FR-027-17) et A-003-11 (reprise, FR-003-28) |
| 7 | review-rubric | medium — signaux dupliqués FEAT-003 / FEAT-027 | reporté | §9 RP-03 |
| 8 | review-rubric | medium — parcours admin de la file suspendue absent | déjà traité | UJ-5 (gate A9) |
| 9 | review-rubric | low — volumétrie A.8 sans usage | reporté | §9 RP-04 |
| 10 | review-rubric | low — seuils homonymes (alerte, mise en attente, fraude) | corrigé | §3 glossaire : 4 seuils + PENDING |
| 11 | review-rubric | low — dérive de vocabulaire (suspendu, termes absents, « huit signaux ») | corrigé | §3 glossaire ; FR-021-02, FR-021-04, A-021-3 (« inactif ») ; FR-027-01 |
| 12 | review-rubric | low — le PRD prescrit l'epic 1 | sans suite | décision de réconciliation validée (étape 2, memlog) |
| 13 | review-rubric | low — NFR banque absentes (accessibilité, verrouillage, mot de passe…) | reporté | §9 RP-05 |
| 14 | review-rubric | note mécanique — ordre A.5, A.7, A.8, A.6 de l'addendum | corrigé | addendum : A.6 replacée avant A.7 |
| 15 | review-recouvrements | M1 — « exécuté » n'exclut pas le cumul | déjà traité | FR-027-02, FR-002-10 |
| 16 | review-recouvrements | M2 — FR-003-14 sans exception du refus | déjà traité | FR-003-14 |
| 17 | review-recouvrements | M3 — plafond de 20 peut effacer l'avis de suspension | supposition | A-025-3 (FR-025-04) ; renvoi dans FR-027-04 |
| 18 | review-recouvrements | M4 — portée de FR-002-10 face aux virements programmés | déjà traité | FR-002-10 (B2) |
| 19 | review-recouvrements | L1 — PENDING « en attente » pour un virement exécuté | déjà traité | décision B1 |
| 20 | review-recouvrements | L2 — bornes du cumul et du seuil de fraude | corrigé | FR-027-18 |
| 21 | review-recouvrements | L3 — descriptions FEAT-023 et FEAT-025 obsolètes | déjà traité | descriptions FEAT-023, FEAT-025 |
| 22 | review-recouvrements | L4 — homonymie « suspendu » | corrigé | FR-021-02, FR-021-04, glossaire |
| 23 | review-recouvrements | L5 — rejet automatique absent de NFR-BATCH-01 | corrigé | NFR-BATCH-01 |
| 24 | review-recouvrements | L6 — compte désactivé et MULTIPLE_FAILED_AUTH | supposition | A-027-12 (FR-027-11) |
| 25 | review-recouvrements | L7 — écran après un virement suspendu | corrigé | FR-003-12 (renvoi FR-027-04, FR-003-27) |
| 26 | review-ecarts-corpus | M-1 — FR-003-09 ne nomme pas RG-003-010/011 | déjà traité | FR-003-09 |
| 27 | review-ecarts-corpus | M-2 — texte FRAUDE legacy non rattaché | corrigé | A-027-4 complétée, Source FR-027-04 |
| 28 | review-ecarts-corpus | M-3 — FR-027-12 sans formule ni BUG-056 | corrigé | FR-027-12 |
| 29 | review-ecarts-corpus | M-4 — FR-028-03 sans marquage BUG-040 | corrigé | FR-028-03 |
| 30 | review-ecarts-corpus | M-5 — A-030-1 ne nomme pas RG-030-001 | corrigé | FR-030-01 |
| 31 | review-ecarts-corpus | M-6 — A-001-2 restreint RG-001-016 sans le dire | corrigé | FR-001-13 |
| 32 | review-ecarts-corpus | M-7 — extension 403 de BUG-038 non rattachée | corrigé | A-010-1 (FR-010-09) |
| 33 | review-ecarts-corpus | M-8 — compteur de téléchargements introduit sans le dire | supposition | A-008-2 (FR-008-12) |
| 34 | review-ecarts-corpus | M-9 — A-014-1 élargit RG-014-005 sans la nommer | corrigé | FR-014-04 (texte et Source) |
| 35 | review-ecarts-corpus | M-10 — attente de synchronisation écartée en note seulement | corrigé | renvois dans FR-014-06 et FR-012-09 |
| 36 | review-ecarts-corpus | M-11 — A-011-2 sans raison ni MSG | corrigé | FR-011-09 |
| 37 | review-ecarts-corpus | M-12 — FR-017-09 404 contre bug conservé (500) | supposition | A-017-2 (FR-017-09), note FEAT-017 |
| 38 | review-ecarts-corpus | M-13 — A-025-1 type ambigu | déjà traité | FR-025-08 (B3) |
| 39 | review-ecarts-corpus | L-1 — FR-027-02 marqueur et Source incomplets | corrigé | FR-027-02 (D10, RG-023-009, BUG-076) |
| 40 | review-ecarts-corpus | L-2 — FR-003-17 sans RG-027-004 en Source | corrigé | FR-003-17 |
| 41 | review-ecarts-corpus | L-3 — FR-004-14 sans formule ni BUG-069 | corrigé | FR-004-14 |
| 42 | review-ecarts-corpus | L-4 — FR-012-07/12/14 sans formule normalisée | corrigé | FR-012-07, FR-012-12, FR-012-14 |
| 43 | review-ecarts-corpus | L-5 — FR-014-02 sans BUG-040 en Source | corrigé | FR-014-02 |
| 44 | review-ecarts-corpus | L-6 — FR-013-07 « vide » ambigu | corrigé | FR-013-07 (valeur absente) |
| 45 | review-ecarts-corpus | L-7 — FR-023-06 sans BUG-006 ni formule | corrigé | FR-023-06 |
| 46 | review-ecarts-corpus | L-8 — A-007-2 sans identifiant de règle | corrigé | FR-007-11 |
| 47 | review-ecarts-corpus | L-9 — journal des intérêts les autres nuits | supposition | A-018-4 (FR-018-04) |
| 48 | review-ecarts-corpus | L-10 — messages techniques FEAT-006 sans remplaçant | supposition | A-006-1 (NFR de FEAT-006) |
| 49 | §9 QO-2 | état DEFAUT des prêts | reporté | §9 RP-06 |
| 50 | §9 QO-2 | compteur de téléchargements | supposition | A-008-2 (même point que M-8) |
| 51 | §9 QO-2 | bouton « Rafraîchir » | déjà traité | A-012-5 (annotée) |
| 52 | §9 QO-2 | méthodes de catalogue par type sans appelant | reporté | §9 RP-07 |
| 53 | §9 QO-2 | evaluateSingleScore (bug conservé) | reporté | §9 RP-08 |
| 54 | §9 QO-2 | surcharge des seuils en production | reporté | §9 RP-09 |
| 55 | §9 QO-3 | POWENS_CONNECTOR_ID de production | reporté | §9 RP-10 |
| 56 | [NOTE FOR PM] | aucune occurrence | sans objet | — |
| 57 | §10 index | 79 entrées (89 identifiants), dont A-G-01 et A-G-02 | supposition annotée | §10 : propriétaire et reprise ajoutés, texte et niveau inchangés (sauf A-021-3 : « inactif » ; A-027-9 scindée) |
| 58 | §10 index | 8 nouvelles suppositions (A-006-1, A-008-2, A-017-2, A-018-4, A-025-3, A-027-11, A-027-12, A-003-11) | supposition ajoutée | §10, annotées |

Décompte : corrigé 27, reporté 10, supposition 8, déjà traité 9, sans suite 1, sans objet 1, supposition annotée 1, supposition ajoutée 1. Bloquants : 0.
