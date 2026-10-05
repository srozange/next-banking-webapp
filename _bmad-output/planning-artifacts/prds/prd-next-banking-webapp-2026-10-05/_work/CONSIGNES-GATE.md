# Consignes : appliquer les corrections du reviewer gate (blocs A et B validés par le décideur)

Projet : `/home/sebastienrozange/workspace/acn-fs/next-banking-webapp`. Dossier du PRD : `_bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/` (appelé W ci-dessous).

## Règles de travail

- **Ne modifie jamais `W/prd.md` directement.** Le PRD est assemblé par `bash W/_work/assemble.sh` à partir de :
  - `W/_draft/00-head.md` (§0 à §4, intro) ;
  - `W/_draft/FEAT-001.md` à `FEAT-030.md` (§4) ;
  - `W/_draft/90-tail.md` (§5 à §9) ;
  - `W/_draft/95-assumptions.md` (§10, l'index des suppositions).

  Modifie ces fichiers, puis `W/addendum.md` si besoin.
- **Lis d'abord les trois revues**, qui donnent l'emplacement exact et la correction proposée pour chaque constat :
  - `W/review-recouvrements.md` (constats C1 à C8, H1 à H6) ;
  - `W/review-ecarts-corpus.md` ;
  - `W/review-rubric.md`.
- **Format des FR** : `#### FR-NNN-xx : titre`, avec une ligne `Source :` qui cite les RG et BUG. Les numéros des FR existantes ne changent pas. Une FR nouvelle prend le numéro suivant de sa fonctionnalité.
- **Marqueurs d'écart au corpus** :
  - `[ÉVOLUTION — décision RG-027-003 Dn]` pour un écart issu d'une décision D1 à D12 ;
  - `[ÉVOLUTION — décision de finalisation Bn]` pour un écart issu d'une décision B1 à B5 ;
  - `[ASSUMPTION A-NNN-k : …]` pour un choix que tu fais toi-même.

  Toute nouvelle supposition va dans `95-assumptions.md`, au bon niveau de risque. Une supposition retirée en est enlevée.
- **Écriture** : en français, des phrases complètes et testables. Les textes affichés sont entre guillemets, sans accents s'ils viennent du legacy.
- **À la fin, lance `bash W/_work/assemble.sh`.** La couverture doit rester à 100 % pour les règles et pour les bugs, avec `forbidden []`. Vérifie aussi par script :
  - que chaque `[ASSUMPTION A-…]` du texte est dans l'index, et inversement (sauf A-G-01 et A-G-02, qui n'existent que dans l'index) ;
  - que chaque renvoi `FR-NNN-xx` mène à une FR existante.

## Bloc A : corrections de cohérence (validées)

1. **Notifications et traitement asynchrone (C1, C2).**
   - L'unique notification est produite au moment de la décision du contrôle de fraude :
     - FRAUDE (avis de suspension, FR-027-04) si le virement est suspendu ;
     - CONFIRMATION ou ALERTE (voir B1) s'il est exécuté.
   - Un virement libéré passe ensuite par le traitement asynchrone, mais sans nouvelle notification (une seule par virement, FR-002-10).
   - Mets en cohérence FR-023-01, FR-023-03, FR-025-01, FR-003-13, FR-003-14, FR-002-10 et la description de FEAT-023.
2. **Échec de la remise au traitement (C3, BUG-073, FR-023-02).**
   - La remise est enregistrée avec la validation du virement : sa publication suit l'enregistrement, comme une écriture unique.
   - Si cette remise ne peut être enregistrée, la validation échoue : le virement n'est pas enregistré, rien n'est débité, et le client est informé (texte de A-023-1).
   - Aucun état n'est ajouté à FR-027-16, puisque le virement n'existe pas.
   - Marque ce choix `[ASSUMPTION A-023-3 : …]`.
3. **FR-003-08** : retire « exécuté dès la soumission » et renvoie aux issues de FR-027-02 (C7).
4. **Audit (H3, H4).** Le virement est audité à la soumission (avec l'issue du contrôle), puis à l'exécution, à la libération, au rejet et au rejet automatique.
   - Ajoute dans FEAT-026 les codes d'audit correspondants : `TRANSFER_SUBMITTED`, `TRANSFER_RELEASED`, `TRANSFER_REJECTED`, `TRANSFER_AUTO_REJECTED`, ou les noms cohérents avec les codes existants, en supposition.
   - Corrige les listes fermées et la note « pas d'audit des traitements système ».
5. **Auteur de la revue d'une alerte (C6).** On enregistre l'identifiant de l'administrateur et on affiche son nom complet. Ajoute une supposition, et un renvoi croisé entre FR-016-15 et FR-027-13/FR-027-14.
6. **Chat (C8).** Explicite dans FR-010-04 et FR-010-10 le lien entre les deux :
   - les actions rapides sont masquées pour la conversation, pas pour la session ;
   - « Nouvelle conversation » les réaffiche.

   Cite RG-010-007 et RG-010-022, et marque l'écart comme supposition.
7. **Clé Azure OpenAI (H6, FR-010-08).** Pas de valeur par défaut dans le code (NFR-SEC-06). Marque l'écart au corpus `[ASSUMPTION …]` en citant la règle concernée.
8. **Marquage des écarts au corpus** :
   - FR-002-10, FR-003-14, FR-023-03 et FR-025-01 : ajoute D2 dans le marqueur et cite les règles modifiées (RG-002-021, RG-002-022, RG-003-019, RG-003-021, RG-023-004, RG-023-005, RG-025-002, RG-025-003, selon la FR).
   - FR-012-06 et FR-028-04 : marque l'abandon de la branche « code seul » en renvoyant à A-014-1, et étends le texte de A-014-1 à RG-012-011 et RG-028-009.
   - FR-003-24 (HIGH_FREQUENCY) : ajoute `[ASSUMPTION A-003-9 : …]` pour le choix entre « transactions » (RG-003-033) et « virements » (RG-027-012), comme A-003-5 pour RAPID_SUCCESSION.
   - Dans `90-tail.md`, section « Décision RG-027-003 » : ajoute que D1 renverse la mention de résolution du corpus sous RG-027-003, « ne bloque pas le virement : elle le signale ».
9. **Rubrique** :
   - §1 Vision, §5 et §6 : remplace « exactement le même service » ou « aucune évolution au-delà de la parité » par « parité, plus les évolutions décidées ». Liste-les : décision RG-027-003 (suspension, revue, cumul, reprise), décisions B1 à B5, FR-027-11 (signal d'échecs de connexion), le bouton « Rafraîchir » (A-012-5) et l'exemption d'authentification par chemin exact (A-030-1).
   - SM-1 : « tous les scénarios retenus à l'étape G2, 31 au plus, passent au rejeu final ».
   - Ajoute dans `00-head.md` un parcours **UJ-5** : un administrateur (prénom au choix, cohérent avec UJ-3) traite la file des virements suspendus. Il voit l'heure butoir et le motif, libère un virement avant l'heure butoir et en rejette un autre, avec motif. Les alertes passent en REVIEWED (B4).

## Bloc B : décisions du décideur

1. **B1, BUG-009 (FR-003-10, FR-002-10, FR-003-14, FR-023-03, FR-025-01, FR-003-09).**
   - Les textes PENDING et ALERTE sont reformulés en « transaction importante », sans « en attente » ni « vérification ».
   - Propose des textes sans accents, dans le style du legacy, et marque leur formulation exacte comme supposition. Exemple : notification ALERTE « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. » ; mention au relevé « [TRANSACTION IMPORTANTE] » à la place de « [EN ATTENTE] ».
   - Le virement de 5 000 à 10 000 € reste exécuté et débité (D2).
   - Écris dans FR-003-10 que l'écart de texte avec le golden master est voulu (décision B1), et retire A-003-7 de l'index.
   - Vérifie FEAT-005 (relevé), qui traite la mention « [EN ATTENTE] » (FR-005-xx, RG-005-…). Mets-le en cohérence : soit l'écran du relevé reconnaît aussi « [TRANSACTION IMPORTANTE] », soit tu expliques ce qu'il fait des anciennes opérations. Fais un choix raisonnable, en supposition.
2. **B2, virements programmés (FEAT-021, FEAT-004).**
   - Un virement programmé passe par le même contrôle de fraude et la même règle de suspension qu'un virement immédiat.
   - Le contrôle s'exécute au moment de l'exécution de l'échéance, pas à la programmation, par le même cas d'usage que le virement immédiat.
   - Une échéance suspendue suit FR-027-16, FR-027-17 et FR-016-19. Ce que devient la date de la prochaine échéance pendant la suspension est à marquer en supposition.
   - Mets à jour FR-021-xx (ajoute une FR si besoin) et la note de FEAT-021 qui disait le contraire, avec le marqueur `[ÉVOLUTION — décision de finalisation B2]`.
   - Mets à jour FR-027-02 : « tout virement, immédiat ou programmé ».
3. **B3, solde faible.** Alignement sur BUG-071 : au plus 20 alertes par compte et par jour (FR-022-04).
   - Mets FR-025-08 en cohérence avec FEAT-022 : même plafond ; et le type de notification de FEAT-022, s'il diffère de FEAT-025 (ALERTE ou ACCOUNT_BALANCE), est aligné sur celui du corpus, avec une supposition si nécessaire.
   - Retire A-025-2 du texte et de l'index.
4. **B4.** Dans FR-016-19, et avec un renvoi dans FR-016-15 et FR-027-13 : libérer ou rejeter un virement suspendu passe ses alertes de fraude au statut REVIEWED, dans la même action. Ajoute-le au critère d'acceptation.
5. **B5, l'heure butoir fait foi (FR-027-17, FR-016-19).**
   - La libération est acceptée strictement avant l'heure butoir. À l'heure butoir ou après, elle est refusée et le virement est traité comme rejeté, que le traitement planifié soit passé ou non.
   - La course entre libération et expiration se tranche par l'horloge, pas par l'ordre de passage du traitement.
   - Réécris A-027-8 dans ce sens. Ce n'est plus une supposition mais une décision : retire A-027-8 de l'index et écris la règle avec le marqueur B5.
   - Critère d'acceptation : libération à heure butoir − 1 s acceptée ; libération à l'heure butoir refusée, virement REJETÉ, même si le traitement planifié n'est pas encore passé.

## Compte rendu (ta seule réponse, court)

- La liste des fichiers modifiés.
- Pour chaque point A1 à A9 et B1 à B5 : fait, ou non fait et pourquoi.
- Les suppositions ajoutées et retirées.
- Le résultat de `assemble.sh` (couverture, forbidden) et des deux vérifications par script.
