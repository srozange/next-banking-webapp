---
title: "Revue : conformité des écarts au corpus — PRD next-banking-webapp"
date: 2026-10-05
cible: prd.md (331 FR) et .memlog.md
corpus: docs/retro/features/FEAT-NNN/RG.md, docs/retro/00-global/LEGACY-BUGS.md (commit c05512ec)
---

# Revue : conformité des écarts au corpus

## Verdict

**NON CONFORME — à corriger avant finalisation.** Trois FR s'écartent d'une règle du corpus sans aucun marqueur d'écart volontaire dans l'exigence (3 critical). Huit autres écarts sont marqués, mais le marqueur ne nomme pas la règle modifiée ou décrit un autre écart que l'écart réel (8 high). Le reste porte sur la précision des marquages et des lignes Source.

Le gros du PRD est sain. 297 des 331 FR sont conformes sans réserve. Les corrections de bug suivent presque toutes la forme « remplace le comportement legacy RG-… » + BUG-NNN. Les décisions D1 à D12 sur RG-027-003 (le contrôle de fraude d'un virement) sont toutes appliquées. Les défauts sont concentrés à deux endroits :
- les **conséquences indirectes de la décision RG-027-003**. Le type de la notification dépend désormais de la suspension, et non plus du seul montant. Le statut PENDING est maintenu alors que le virement est débité. Les règles de FEAT-002, FEAT-003, FEAT-023 et FEAT-025 ainsi modifiées ne sont pas nommées ;
- la **branche « code » du retour Powens** (open banking), abandonnée dans FR-014-04 mais restée muette dans FR-012-06 et FR-028-04.

| Sévérité | Nombre |
|---|---|
| Critical | 3 |
| High | 8 |
| Medium | 13 |
| Low | 10 |
| **Total** | **34** |

## Méthode

1. Un script (`extract.py`, dans le dossier scratchpad de la session) a extrait les 331 FR `#### FR-NNN-xx` de `prd.md`, avec leur texte intégral et leur ligne « Source : ». Il a extrait aussi les 637 RG des 30 fichiers `RG.md` (texte EARS, statut, résolution) et les 93 lignes BUG de `LEGACY-BUGS.md`. Il a produit, pour chaque fonctionnalité, un fichier qui met côte à côte chaque FR, chaque RG citée et chaque BUG cité. Contrôles automatiques : 0 RG citée introuvable, 0 FR sans source, 0 RG du corpus jamais citée.
2. Revue manuelle en 7 lots (FEAT-001/002/005/006 ; 003/004 ; 007 à 010 ; 011 à 014 ; 015 à 020 ; 021 à 026 ; 027 à 030). Chaque FR est comparée à chacune de ses RG : seuil, condition, effet, message, acteur. Les textes affichés sont aussi recoupés avec les `MESSAGES.md` des fonctionnalités.
3. Contrôle du memlog : pour chaque décision qui mentionne une RG, conformité au texte de la RG, puis application dans le PRD.
4. Calibrage commun des sévérités, appliqué à tous les lots :
   - **critical** : écart à une RG sans aucun marqueur dans la FR, ou marqueur qui décrit un autre écart que l'écart réel ;
   - **high** : marqueur présent, mais RG modifiée non nommée, mauvaise décision citée ou contradiction avec une autre FR ;
   - **medium** : marquage incomplet ou déplacé ;
   - **low** : forme (ligne Source, formule normalisée).

Exhaustivité : 331 FR examinées sur 331 (détail en fin de document).

---

## Findings critical

### [CRITICAL] C-1 · FR-003-10 — le marqueur « remplace RG-003-014 » annonce une correction de BUG-009 qui n'a pas lieu, et la suspension à 10 000 € n'est pas marquée ÉVOLUTION
- **RG** : RG-003-014 (Suspect) + BUG-009 (un virement « En attente » est débité tout de suite, à corriger) ; RG-003-015 ; RG-027-003, décision D1, absente de la ligne Source.
- **Ligne PRD** : prd.md:350 (Source prd.md:356) ; note prd.md:471 ; A-003-7 prd.md:2845.
- **Écart constaté** :
  - Le constat de BUG-009 est que le statut « En attente » est trompeur, puisque le compte source est débité immédiatement. La note métier (LEGACY-BUGS.md:150) dit : « Le statut « En attente » ne doit pas débiter ».
  - La FR maintient ce débit : « PENDING est un statut d'affichage de l'état EXÉCUTÉ : le virement est débité et n'attend aucune revue ». A-003-7 reconnaît que « l'écart entre le texte et le solde n'est pas tranché ».
  - La phrase « Cette exigence remplace le comportement legacy RG-003-014 », appuyée sur BUG-009 en Source, décrit donc un écart qui n'existe pas. La note prd.md:471 (« leur comportement legacy n'est pas reproduit ») est fausse sur ce point.
  - L'écart réel avec RG-003-014 n'est pas marqué ÉVOLUTION et RG-027-003 n'est pas citée. Cet écart : à partir de 10 000 €, ou au-delà d'un score de 75, le virement n'est pas PENDING mais SUSPENDU sans débit. Le critère d'acceptation le dit (« 10 000,00 € … SUSPENDU »).
- **Correction proposée** :
  1. Titre : « FR-003-10 : Statut PENDING des virements importants [ÉVOLUTION — décision RG-027-003 D1, D2] ». Ajouter RG-027-003 à la ligne Source.
  2. Remplacer « Cette exigence remplace le comportement legacy RG-003-014. » par : « Écart à RG-003-014 : un virement de 10 000 € ou plus, ou de score cumulé strictement supérieur à 75, n'est pas PENDING mais SUSPENDU sans débit (décisions D1, D2). BUG-009 n'est pas corrigé par ce PRD : un virement PENDING reste débité immédiatement (A-003-7). »
  3. Faire trancher BUG-009 par le métier : correction réelle, ou reclassement explicite en bug conservé dans LEGACY-BUGS.md. Corriger la note prd.md:471 en conséquence. En attendant, retirer BUG-009 de la ligne Source, ou l'y garder avec la mention « non corrigé, voir A-003-7 ».

### [CRITICAL] C-2 · FR-012-06 — RG-012-011 (chargement des comptes avec le seul code Powens) est citée en Source mais n'est pas appliquée, sans marqueur
- **RG** : RG-012-011 (À confirmer), RG-012-008 (Confirmé) ; voir RG-014-004 (résolution « branche code non reprise »).
- **Ligne PRD** : prd.md:1227 (Source prd.md:1231).
- **Écart constaté** :
  - RG-012-011 dit : « WHEN le callback Powens est reçu avec le paramètre code OAuth et sans connection_id THE SYSTEM SHALL déclencher le chargement des comptes externes avec ce code ».
  - La FR dit seulement : « si seul un code est reçu, la branche « code » suit la décision portée par FR-014-04 ». Or FR-014-04 traite ce retour comme incomplet (A-014-1). Cela contredit RG-012-011, et cela élargit la condition de RG-012-008, qui ne vise que le retour « sans connection_id ni code ».
  - FR-012-06 ne porte aucun des trois marqueurs. A-014-1 ne nomme que RG-014-004, et RG-012-011 n'a pas de résolution propre dans le corpus.
- **Correction proposée** : « si seul un code est reçu, le chargement des comptes n'est pas lancé : le retour est traité comme incomplet (FR-014-04). [ASSUMPTION A-014-1 : écart à RG-012-011 (chargement avec le code), non reprise, et à RG-012-008, dont la condition est élargie au retour qui ne porte qu'un code, tant que Powens n'a pas confirmé l'usage du code (résolution de RG-014-004)] ».

### [CRITICAL] C-3 · FR-028-04 — même écart pour RG-028-009, sans marqueur
- **RG** : RG-028-009 (À confirmer).
- **Ligne PRD** : prd.md:2427.
- **Écart constaté** :
  - RG-028-009 dit : « WHEN the Powens callback provides no connection_id but provides a code parameter, THE SYSTEM SHALL treat the code as the connection identifier and trigger external account loading ».
  - La FR décrit la règle, puis dit « Cette branche suit la décision de FR-014-04 : non reprise tant que Powens n'a pas confirmé son usage ». La raison est donnée, mais il n'y a aucun marqueur, et le comportement cible n'est pas décrit : retour incomplet, message « Callback Powens incomplet. Parametres recus: [code] ».
  - A-014-1 ne nomme pas RG-028-009.
- **Correction proposée** : ajouter « [ASSUMPTION A-014-1 : écart à RG-028-009 — tant que Powens n'a pas confirmé l'usage du paramètre `code`, un retour qui ne porte qu'un `code` est traité comme incomplet (« Callback Powens incomplet. Parametres recus: [code] ») et ne déclenche pas le chargement des comptes] ». Dans FR-014-04 et au §10, compléter A-014-1 par « RG-012-011, RG-028-009 ».

---

## Findings high

### [HIGH] H-1 · FR-002-10 — l'ÉVOLUTION ne nomme ni D2 ni les règles RG-002-021 et RG-002-022, qu'elle modifie
- **RG** : RG-002-021, RG-002-022, RG-002-023 (À confirmer) ; décision RG-027-003 D2 ; BUG-007.
- **Ligne PRD** : prd.md:254.
- **Écart constaté** : dans le corpus, le type de notification dépend du seul montant : CONFIRMATION sous le seuil d'alerte, ALERTE entre le seuil d'alerte et le seuil de fraude, FRAUDE au-delà. Dans la FR, il dépend d'abord de la suspension : « score cumulé strictement supérieur à 75, ou montant ≥ seuil de fraude : FRAUDE ». Un virement de 100 € au score supérieur à 75 reçoit donc FRAUDE au lieu de CONFIRMATION. Le marqueur cite « D1, D3 », alors que le critère vient de D2 (§9, prd.md:2799). RG-002-021 et RG-002-022 ne sont nommées nulle part comme modifiées : seule RG-002-024 l'est, pour BUG-007, qui porte sur un autre écart.
- **Correction proposée** : titre « [ÉVOLUTION — décision RG-027-003 D1, D2, D3] », et ajouter : « Cette règle s'écarte de RG-002-021 et RG-002-022, qui fixaient le type par le seul montant : un virement suspendu pour score (D2) reçoit FRAUDE quel que soit son montant. RG-002-023 est conservée (montant ≥ seuil de fraude → FRAUDE, virement désormais suspendu, D1). »

### [HIGH] H-2 · FR-003-14 — même écart pour RG-003-019 et RG-003-021
- **RG** : RG-003-019, RG-003-021, RG-003-022 ; RG-027-003, absente de la ligne Source.
- **Ligne PRD** : prd.md:376.
- **Écart constaté** : un virement de moins de 10 000 €, suspendu parce que son score dépasse 75, reçoit FRAUDE au lieu de CONFIRMATION ou ALERTE. Le marqueur « [ÉVOLUTION — décision RG-027-003 D1] » ne cite pas D2 et ne nomme pas les RG modifiées.
- **Correction proposée** : marqueur « D1, D2 » ; ajouter « Remplace RG-003-019 et RG-003-021 pour un virement suspendu sur score : il reçoit FRAUDE » ; ajouter RG-027-003 à la ligne Source.

### [HIGH] H-3 · FR-023-03 — l'ÉVOLUTION ne nomme pas RG-023-004 et RG-023-005, ni D2, ni le remplacement du texte FRAUDE legacy
- **RG** : RG-023-004, RG-023-005 (Confirmé) ; décisions RG-027-003 D1, D2.
- **Ligne PRD** : prd.md:2019.
- **Écart constaté** : selon RG-023-004, la notification FRAUDE est déclenchée par le seul montant (≥ seuilFraudeEur), avec le texte legacy « Virement de {amount} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller … ». RG-023-005 réserve l'ALERTE aux montants inférieurs au seuil de fraude. La FR envoie FRAUDE pour tout virement suspendu, y compris sur score, et ce texte devient l'avis de FR-027-04. Le marqueur ne cite que D1. Les deux RG et le changement de texte, que compare le golden master, ne sont pas nommés.
- **Correction proposée** : titre « [ÉVOLUTION — décision RG-027-003 D1, D2] ». Ajouter : « Modifie RG-023-004 : FRAUDE suit la suspension (score > 75 ou montant ≥ seuil de fraude) et non le seul montant ; son texte legacy « Virement de {amount} EUR bloque pour verification (seuil anti-fraude : …) … » est remplacé par l'avis de FR-027-04. Modifie RG-023-005 : l'ALERTE ne concerne plus que les virements exécutés de 5 000 € à 9 999,99 €. »

### [HIGH] H-4 · FR-025-01 — même écart pour RG-025-003, RG-025-002 et MSG-025-001
- **RG** : RG-025-003, RG-025-002 (Confirmé) ; décisions D1, D2.
- **Ligne PRD** : prd.md:2132.
- **Écart constaté** : identique à H-3. La FR envoie FRAUDE pour tout virement suspendu et remplace MSG-025-001 (« blocked for fraud verification ») par l'avis de FR-027-04. Le marqueur ne cite que D1.
- **Correction proposée** : même ajout que H-3, avec RG-025-003, RG-025-002 et MSG-025-001 ; marqueur « D1, D2 ».

### [HIGH] H-5 · FR-025-08 — l'hypothèse A-025-2 (une seule alerte tant que le solde reste bas) contredit RG-022-004, RG-022-010 et FR-022-04
- **RG** : RG-022-004 (À confirmer) ; RG-022-010 (Suspect) + BUG-071 (plafond de 20 alertes par jour jamais appliqué, à corriger).
- **Ligne PRD** : prd.md:2179 ; FR-022-04.
- **Écart constaté** :
  - RG-022-004 crée une alerte à chaque analyse où le solde est strictement inférieur à 100 €. RG-022-010, comportement corrigé validé par le métier, dit « at most 20 low-balance notifications per account per day ». FR-022-04 l'applique : son critère d'acceptation attend 20 notifications pour 48 analyses dans la journée.
  - FR-025-08 pose au contraire « [ASSUMPTION A-025-2 : une seule notification par compte tant que le solde reste sous le seuil…] ». Ce n'est pas un silence du corpus mais une contradiction. Elle ne nomme pas les RG, et les deux critères d'acceptation sont incompatibles (20 contre 1).
- **Correction proposée** : supprimer A-025-2 et écrire « La fréquence suit FEAT-022 : une notification à chaque analyse où le solde est sous le seuil, dans la limite de 20 par compte et par jour (FR-022-02, FR-022-04). » Ajouter RG-022-004 et RG-022-010 à la ligne Source. Autre voie, si « une seule alerte » est voulue : [ÉVOLUTION …] nommant RG-022-004 et RG-022-010, avec la raison, et réécriture de FR-022-04.

### [HIGH] H-6 · FR-016-17 — titre « Traitements nocturnes » raccourci sans marquage dans la FR
- **RG** : RG-016-035, RG-016-036 ; MSG-016-100, MSG-018-030.
- **Ligne PRD** : prd.md:1639 ; marquage déporté en prd.md:1790.
- **Écart constaté** : le titre affiché par le legacy est « Traitements nocturnes (EJB Timer @Schedule) ». La FR affiche « Traitements nocturnes » sans le signaler. Le seul marqueur est [ASSUMPTION A-018-3], dans les notes de FEAT-018, une autre fonctionnalité, et il ne donne pas de raison. Ce n'est pas un silence du corpus mais la modification volontaire d'un texte comparé par le golden master.
- **Correction proposée** : dans FR-016-17, « (le titre legacy « Traitements nocturnes (EJB Timer @Schedule) » perd son suffixe technique, propre à la pile legacy et sans sens pour l'utilisateur — A-018-3) ». Requalifier A-018-3 avec MSG-018-030 et cette raison.

### [HIGH] H-7 · FR-003-24 — divergence entre RG-003-033 (« transactions ») et RG-027-012 (« virements ») non tranchée, contrairement à la décision de fusion du memlog
- **RG** : RG-003-033 ; RG-027-012, absente de la ligne Source.
- **Ligne PRD** : prd.md:438 ; FR-027-10 prd.md:2323.
- **Écart constaté** : pour le signal HIGH_FREQUENCY, RG-003-033 compte les « transactions » de plus de 500 €, et RG-027-012 compte les « transfers … executed ». FR-003-24 et FR-027-10 se renvoient l'une à l'autre avec des assiettes différentes, sans [ASSUMPTION]. La décision du memlog « Fusion des règles voisines » exige pourtant que toute différence reste visible et soit marquée [ASSUMPTION] (le cas analogue de RAPID_SUCCESSION est traité par A-003-5).
- **Correction proposée** : « [ASSUMPTION A-003-9 : l'assiette est celle des virements exécutés du client (RG-027-012), et non des opérations au relevé (RG-003-033)] », reprise à l'identique dans FR-027-10 ; ajouter RG-027-012 à la ligne Source ; indexer au §10.

### [HIGH] H-8 · Memlog — la décision « RG-027-003 précisée » présente le point (a) comme non tranché, alors que la RG le tranche (« ne bloque pas, signale »)
- **RG** : RG-027-003 (Suspect), résolution : « La détection de fraude ne bloque pas le virement : elle le signale (alertes ouvertes, revues par un administrateur) ».
- **Ligne** : `.memlog.md`, décision « RG-027-003 (BUG-084) précisée par l'utilisateur » ; §9 prd.md:2790-2810.
- **Écart constaté** :
  - La décision donne comme ouvert « (a) suspicion détectée → virement bloqué ou seulement signalé ». Le texte de la RG le tranche pourtant : signalé, non bloqué. Seuls le code legacy (JIRA-9202) et la note BUG-084 de LEGACY-BUGS.md:225 laissent la question ouverte.
  - La décision D1, qui suspend un virement suspect, renverse donc une résolution écrite du corpus. Le PRD le dit bien, mais seulement dans une note de FEAT-027 (prd.md:2397).
  - Le §9 (D1 et la note sur l'incohérence du corpus) ne parle que de l'incohérence de BUG-084. Il ne dit pas que D1 annule la résolution de RG-027-003.
  - La décision D1 elle-même a bien été appliquée.
- **Correction proposée** : dans §9, à la note sur l'incohérence du corpus, ajouter « La résolution portée par RG-027-003 dans `docs/retro/features/FEAT-027/RG.md` (« ne bloque pas le virement : elle le signale ») est expressément renversée par D1. » Dans le memlog, compléter l'entrée par une ligne (event) qui consigne cette contradiction.

---

## Findings medium

### [MEDIUM] M-1 · FR-003-09 — l'ÉVOLUTION ne nomme pas RG-003-010 et RG-003-011
- **RG** : RG-003-010, RG-003-011 (débit et crédit « WHEN a valid transfer is submitted ») ; RG-027-003.
- **Ligne PRD** : prd.md:342.
- **Écart constaté** : le débit et le crédit n'ont lieu qu'à l'exécution, pas à la soumission. Le marqueur [ÉVOLUTION — D1, D3] et la raison sont présents et les RG sont en Source, mais le texte ne dit pas qu'elles sont modifiées.
- **Correction proposée** : ajouter « Remplace RG-003-010 et RG-003-011 (débit et crédit dès la soumission) : un virement suspendu n'est débité qu'à sa libération. »

### [MEDIUM] M-2 · FR-027-04 — le texte FRAUDE legacy remplacé n'est pas rattaché à sa RG
- **RG** : RG-027-013 (nommée) ; RG-025-003 et RG-003-022 (MSG-025-001, MSG-002-043), non nommées.
- **Ligne PRD** : prd.md:2283.
- **Écart constaté** : A-027-4 dit seulement « il remplace les textes legacy de la notification FRAUDE ». Le texte « Virement de {montant} EUR bloque pour verification (seuil anti-fraude : …). Contactez votre conseiller… » n'est ni cité ni rattaché à ses RG.
- **Correction proposée** : compléter A-027-4 par « … remplace le texte FRAUDE legacy de RG-025-003 / RG-003-022 (« Virement de {montant} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation. »), qui annonçait un conseiller et un blocage sans heure butoir (D6, D7) » ; ajouter RG-025-003 à la ligne Source.

### [MEDIUM] M-3 · FR-027-12 — correction de BUG-056 sans formule normalisée ni citation en Source
- **RG** : RG-027-014 (Confirmé : alertes OPEN seulement) + BUG-056.
- **Ligne PRD** : prd.md:2335.
- **Écart constaté** : la FR liste toutes les alertes, quel que soit leur statut (« FR-016-16 s'applique et remplace cette limitation »). BUG-056 manque à la ligne Source, et la formule « remplace le comportement legacy » est absente.
- **Correction proposée** : « Cette exigence remplace le comportement legacy RG-027-014 (liste limitée aux alertes OPEN), correction BUG-056 » ; ligne Source « RG-027-014, BUG-056 ».

### [MEDIUM] M-4 · FR-028-03 — affichage du motif d'erreur ajouté à RG-028-007 sans marquage
- **RG** : RG-028-007 (retour silencieux vers /comptes-tiers) + BUG-040.
- **Ligne PRD** : prd.md:2421.
- **Écart constaté** : la FR ajoute « et informe l'utilisateur du motif (FR-012-07, correction BUG-040) », sans formule normalisée. BUG-040 manque à la ligne Source.
- **Correction proposée** : « … ; cela remplace le comportement legacy RG-028-007 (retour silencieux, BUG-040) » ; ajouter BUG-040 à la ligne Source.

### [MEDIUM] M-5 · FR-030-01 — A-030-1 restreint RG-030-001 sans la nommer
- **RG** : RG-030-001 (exemption de toute adresse qui *contient* `login.xhtml`, `powens-callback.xhtml`, `/javax.faces.resource/`).
- **Ligne PRD** : prd.md:2597.
- **Écart constaté** : l'exemption est limitée au chemin exact. L'écart et sa raison (contournement par une adresse forgée) sont donnés, mais pas l'identifiant de la règle.
- **Correction proposée** : « [ASSUMPTION A-030-1 : écart à RG-030-001 — exemption sur le chemin exact, et non sur toute adresse qui contient ces noms comme dans le legacy, afin qu'une adresse forgée ne contourne pas l'authentification] ».

### [MEDIUM] M-6 · FR-001-13 — A-001-2 restreint RG-001-016 sans le dire
- **RG** : RG-001-016 (premier segment de `X-Forwarded-For`, sans condition).
- **Ligne PRD** : prd.md:176.
- **Écart constaté** : l'hypothèse n'accepte l'en-tête que d'un mandataire de confiance. C'est une restriction de la règle, pas le comblement d'un silence. La première phrase de la FR reprend la RG telle quelle et contredit donc l'hypothèse.
- **Correction proposée** : « [ASSUMPTION A-001-2 : écart à RG-001-016, qui retient X-Forwarded-For sans condition : l'en-tête n'est retenu que s'il est posé par un mandataire de confiance, sinon l'adresse distante, pour qu'un client ne falsifie pas son adresse dans l'audit] » ; dans la première phrase, « … présent, non vide et posé par un mandataire de confiance (A-001-2) ».

### [MEDIUM] M-7 · FR-010-09 — extension au statut 403 de la correction de BUG-038, ni justifiée ni rattachée à RG-010-020
- **RG** : RG-010-020 (Confirmé : message dégradé avec le code HTTP) + BUG-038 (cas « authentication error »).
- **Ligne PRD** : prd.md:1095.
- **Écart constaté** : le marqueur « remplace … RG-010-020 » vaut pour 401. A-010-1 étend la suppression du code à 403 sans nommer RG-010-020 ni donner de raison.
- **Correction proposée** : « [ASSUMPTION A-010-1 : 401 et 403 sont traités comme une erreur d'authentification (clé ou droits du déploiement invalides, même cause pour l'utilisateur) ; pour 403, écart à RG-010-020, qui afficherait « (HTTP 403) »] », ou restreindre la correction à 401.

### [MEDIUM] M-8 · FR-008-12 — compteur de téléchargements : comportement absent du legacy introduit sans le dire
- **RG** : RG-008-018 (À confirmer, « intention-code-mort » : le compteur reste toujours à 0 ; question posée : faut-il l'implémenter ?).
- **Ligne PRD** : prd.md:919.
- **Écart constaté** : la FR reprend la RG, ce qui est conforme au texte. Mais elle tranche « oui » à la question du corpus sans le signaler, alors que le legacy ne le fait pas. Le §9 (QO-2) renvoie bien à FR-008-12, mais la FR ne le dit pas.
- **Correction proposée** : « [ASSUMPTION A-008-2 : RG-008-018 est implémentée ; le legacy ne l'appliquait pas (compteur toujours à 0) ; aucun texte affiché n'en dépend] ».

### [MEDIUM] M-9 · FR-014-04 — A-014-1 élargit la condition de RG-014-005 sans la nommer
- **RG** : RG-014-004 (résolution : branche « code » non reprise) ; RG-014-005 (Confirmé : retour sans « error nor connection_id nor code ») ; RG-012-011, RG-028-009.
- **Ligne PRD** : prd.md:1430.
- **Écart constaté** : abandonner la branche « code » est conforme à la résolution de RG-014-004. En revanche, afficher « Callback Powens incomplet. Parametres recus: [code] » sort de la condition de RG-014-005, et les règles jumelles ne sont pas nommées (voir C-2 et C-3).
- **Correction proposée** : compléter A-014-1 par « … élargit la condition de RG-014-005 et RG-012-008 au retour portant seulement un code ; RG-012-011 et RG-028-009 ne sont pas reprises pour la même raison » ; ajouter ces RG à la ligne Source.

### [MEDIUM] M-10 · FR-014-06 (et FR-012-09) — l'attente de synchronisation (RG-014-010, RG-012-014) est écartée dans une note, pas dans l'exigence concernée
- **RG** : RG-014-010 (Suspect) + BUG-051 ; RG-012-014 + BUG-041 (tous deux « ne pas corriger »).
- **Ligne PRD** : prd.md:1442 ; note sous FR-014-08 (≈ prd.md:1460) ; A-012-6.
- **Écart constaté** : la cible n'impose aucune attente. Elle ne garde ni la pause de 2 s du legacy (bugs pourtant conservés), ni l'attente de synchronisation de la RG. Le marquage est rangé sous FR-014-08 (le plafond de transactions), et FR-014-06, qui décrit le chargement, n'en dit rien.
- **Correction proposée** : déplacer dans FR-014-06 « [ASSUMPTION A-014-2 : RG-014-010 n'est pas reprise : aucune attente avant la lecture des comptes (legacy : pause bloquante de 2 s, BUG-051 conservé) ; une synchronisation non terminée donne « Connexion reussie mais aucun compte disponible encore… » et l'utilisateur rafraîchit (FR-012-16)] » ; idem dans FR-012-09 pour RG-012-014 et BUG-041 (A-012-6) ; ajouter ces RG aux lignes Source.

### [MEDIUM] M-11 · FR-011-09 — titres de section modifiés par une hypothèse sans raison
- **RG** : aucune RG ; MSG-011-100, MSG-011-101 (« … (EJB @Schedule) »).
- **Ligne PRD** : prd.md:1181.
- **Écart constaté** : A-011-2 marque le retrait de la mention technique, sans raison et sans nommer les messages. Ces textes sont comparés par le golden master.
- **Correction proposée** : « [ASSUMPTION A-011-2 : MSG-011-100 et MSG-011-101 perdent « (EJB @Schedule) », propre à la pile legacy et sans sens pour l'utilisateur ; écart de texte attendu au golden master] ».

### [MEDIUM] M-12 · FR-017-09 — 404 « Compte introuvable » alors que BUG-061 (erreur 500 sur compte inexistant) est déclaré conservé
- **RG** : RG-017-012 (« reject with HTTP 404 ») + BUG-061 (« Ne pas corriger lors de la rénovation »).
- **Ligne PRD** : prd.md:1722 ; note sous FR-017-11.
- **Écart constaté** : la FR est conforme à la RG, mais elle corrige de fait un bug déclaré conservé, et la note se contredit (« BUG-061 (conservé) […] FR-017-09 reprend la règle (404), qui fait foi »). Le golden master divergera sans que l'écart soit marqué.
- **Correction proposée** : ajouter « Ce comportement remplace le comportement legacy (erreur 500, BUG-061) » et BUG-061 à la ligne Source ; dans la note, écrire « BUG-061 corrigé par FR-017-09, contrairement à la décision initiale — à faire acter par le métier ».

### [MEDIUM] M-13 · FR-025-08 — type « ALERTE » posé par hypothèse, alors que le corpus fixe ACCOUNT_BALANCE
- **RG** : RG-022-005 (type `ACCOUNT_BALANCE`, priorité `HIGH`, MSG-022-003) ; FR-022-03.
- **Ligne PRD** : prd.md:2179.
- **Écart constaté** : A-025-1 est ambiguë. Si le type enregistré change, c'est une contradiction non nommée. Si seul l'affichage associe ce type à ALERTE, c'est le comblement d'un silence.
- **Correction proposée** : « [ASSUMPTION A-025-1 : la notification garde le type ACCOUNT_BALANCE et la priorité HIGH (RG-022-005, FR-022-03) ; le centre de notifications l'affiche comme une ALERTE, faute d'affichage prévu pour ce type ; elle compte dans le plafond (FR-025-04)] » ; ajouter RG-022-005 à la ligne Source.

---

## Findings low

| # | FR | RG / BUG | Écart constaté | Correction proposée |
|---|---|---|---|---|
| L-1 | FR-027-02 (prd.md:2264) | RG-027-003, RG-023-009 + BUG-076 | Marquage correct ([ÉVOLUTION D1 à D4] + BUG-084), mais : seuil de 10 000 € repris de RG-023-009/BUG-076, absents de la Source ; cas du cumul glissant relevant de D10, absent du marqueur ; rejet de la résolution « signale » expliqué seulement en note. | Marqueur « D1 à D4, D10 » ; Source « RG-027-003, RG-027-002, RG-023-009, BUG-084, BUG-076 » ; phrase sur la résolution renversée (voir H-8). |
| L-2 | FR-003-17 (prd.md:394) | RG-003-025, RG-027-004 | A-003-4 tranche correctement en faveur de RG-027-004, mais celle-ci n'est pas sur la ligne Source. | Source : RG-003-025, RG-003-026, RG-027-004, RG-027-005. |
| L-3 | FR-004-14 (prd.md:584) | RG-004-064 ; BUG-069 (RG-021-011) | Écart nommé par A-004-5, mais BUG-069 et RG-021-011 absents de la ligne Source, et formule normalisée absente. | Source + « Remplace le comportement legacy RG-004-064 (BUG-069). » |
| L-4 | FR-012-07, FR-012-12, FR-012-14 (prd.md:1233, 1265, 1279) | RG-012-009 + BUG-040/044 ; RG-012-018 + BUG-042 ; RG-012-027 + BUG-043 | Corrections fidèles et bugs cités, mais la formule normalisée « remplace le comportement legacy RG-… » manque (« au lieu de … »). | Ajouter la formule dans chacune, pour le contrôle automatique. |
| L-5 | FR-014-02 (prd.md:1414) | RG-014-002 ; RG-012-009 + BUG-040 | Affichage du motif (correction BUG-040) cité dans le texte, absent de la ligne Source. | Source « RG-014-002, RG-014-003, RG-014-005, RG-012-009, BUG-040 ». |
| L-6 | FR-013-07 (prd.md:1363) | RG-013-022, RG-013-023 (« store null ») | « l'enregistre vide s'il est blanc » : on ne sait pas s'il s'agit d'une chaîne vide ou d'une valeur absente. | « … ne l'enregistre pas (valeur absente) s'il est blanc après suppression des espaces ». |
| L-7 | FR-023-06 (prd.md:2044) | RG-023-011 + BUG-006 | Écart bien expliqué, mais BUG-006 absent de la ligne Source et formule non normalisée. | Source « RG-023-011, BUG-006 » ; « remplace le comportement legacy RG-023-011 ». |
| L-8 | FR-007-11 (prd.md:817) | RG-007-039 (« Paye / En cours ») | A-007-2 (« En attente ») marque l'écart avec sa raison, mais parle de « la règle » sans identifiant. | « … de préférence à « Paye / En cours » de RG-007-039 ». |
| L-9 | FR-018-04 (prd.md:1776) | RG-018-008, lien avec FR-018-01 (BUG-064) | Après la correction (crédit le 1er du mois seulement, A-018-1), rien n'est dit sur l'entrée de journal des autres nuits. | [ASSUMPTION A-018-4 : entrée SUCCESS avec N = 0, ou aucune entrée]. |
| L-10 | NFR de FEAT-006 (prd.md:743) | aucune RG ; MSG-006-073, MSG-006-074 | Messages d'erreur technique legacy « non reproduits » sans dire ce qui s'affiche à la place. | [ASSUMPTION A-006-x : message générique sans détail technique, texte fixé en UX]. |

---

## Contrôle du memlog

| Décision du memlog | RG citée | Texte conforme à la RG ? | Appliquée au PRD ? | Verdict |
|---|---|---|---|---|
| BUG-084 / RG-027-003 = BD-3, ouverte et bloquante | RG-027-003 | Oui pour l'objet (fraude avant validation, erreur non silencieuse) | Remplacée par D1-D12 ; plus aucun [EN ATTENTE BD-3] ni [EN ATTENTE RG-027-003] dans prd.md | Conforme (remplacée) |
| Incohérence du corpus BUG-084 (LEGACY-BUGS.md:129 contre :225) | — (BUG-084) | Vérifiée : les deux lignes disent bien cela | §9, note sur l'incohérence | Conforme |
| Fusion des règles voisines : toute différence visible et marquée [ASSUMPTION] | règles voisines (générique) | — | Appliquée dans la plupart des cas (A-003-4, A-003-5, FR-004-12, FR-003-26…) ; **non appliquée** à HIGH_FREQUENCY (RG-003-033 contre RG-027-012) | **H-7** |
| RG-027-003 précisée : acquis (avant validation finale, erreur journalisée) ; (a) et (b) ouverts | RG-027-003 | Acquis conformes ; **(a) présenté comme ouvert alors que la résolution de la RG le tranche (« ne bloque pas : signale »)** | Remplacée par D1-D12 ; renversement expliqué seulement en note de FEAT-027 | **H-8** |
| QO-1 / RG-027-003 tranchée, D1 à D12 | RG-027-003 ; seuil de fraude (RG-023-00x : 10 000 € par défaut) | D2 « montant ≥ seuil de fraude » conforme à RG-023-004 et RG-023-009 (10 000 €) ; score > 75 conforme à RG-027-002 | Oui : FR-027-02, FR-027-04, FR-027-16, FR-027-17, FR-027-18, FR-003-09, FR-003-13, FR-003-27, FR-003-28, FR-016-19, FR-023-01, FR-023-04, NFR-SEC-08, NFR-SEC-09, NFR-OBS-02, §9, addendum A.3 et A.7. Conséquences sur les notifications marquées incomplètement : H-1 à H-4 | Appliquée (réserves H-1 à H-4) |
| Finalisation : A-027-5 devenue décision (une seule notification, heure butoir et rejet sans débit annoncés) | RG-027-003 (indirect) | — | Oui : FR-027-04 (« La notification dit aussi qu'à défaut de décision… ») ; A-027-5 absente de prd.md | Conforme |
| Q6 : seuils fraude/alerte = configuration de déploiement, valeurs du code | RG-023-00x (indirect) | Conforme (SEUIL_FRAUDE_EUR 10 000, SEUIL_ALERTE_EUR 5 000, SEUIL_PENDING_EUR 5 000) | Addendum A.3, NFR-SEC-08 | Conforme |

Les autres entrées du memlog (mode Fast path, quality-gates, Q1 à Q5, Q7, changements, audit) ne mentionnent pas de RG. Elles sont hors du champ de cette revue.

---

## Bilan d'exhaustivité

| Fonctionnalité | FR examinées | Avec finding |
|---|---|---|
| FEAT-001 | 13 (FR-001-01 à 13) | FR-001-13 |
| FEAT-002 | 12 (FR-002-01 à 12) | FR-002-10 |
| FEAT-003 | 28 (FR-003-01 à 28) | FR-003-09, 10, 14, 17, 24 |
| FEAT-004 | 15 (FR-004-01 à 15) | FR-004-14 |
| FEAT-005 | 8 (FR-005-01 à 08) | — |
| FEAT-006 | 11 (FR-006-01 à 11) | (NFR de la fonctionnalité) |
| FEAT-007 | 13 (FR-007-01 à 13) | FR-007-11 |
| FEAT-008 | 12 (FR-008-01 à 12) | FR-008-12 |
| FEAT-009 | 14 (FR-009-01 à 14) | — |
| FEAT-010 | 11 (FR-010-01 à 11) | FR-010-09 |
| FEAT-011 | 9 (FR-011-01 à 09) | FR-011-09 |
| FEAT-012 | 17 (FR-012-01 à 17) | FR-012-06, 07, 12, 14 |
| FEAT-013 | 10 (FR-013-01 à 10) | FR-013-07 |
| FEAT-014 | 8 (FR-014-01 à 08) | FR-014-02, 04, 06 |
| FEAT-015 | 9 (FR-015-01 à 09) | — |
| FEAT-016 | 19 (FR-016-01 à 19) | FR-016-17 |
| FEAT-017 | 11 (FR-017-01 à 11) | FR-017-09 |
| FEAT-018 | 5 (FR-018-01 à 05) | FR-018-04 |
| FEAT-019 | 6 (FR-019-01 à 06) | — |
| FEAT-020 | 4 (FR-020-01 à 04) | — |
| FEAT-021 | 9 (FR-021-01 à 09) | — |
| FEAT-022 | 6 (FR-022-01 à 06) | — |
| FEAT-023 | 6 (FR-023-01 à 06) | FR-023-03, 06 |
| FEAT-024 | 9 (FR-024-01 à 09) | — |
| FEAT-025 | 8 (FR-025-01 à 08) | FR-025-01, 08 |
| FEAT-026 | 6 (FR-026-01 à 06) | — |
| FEAT-027 | 18 (FR-027-01 à 18) | FR-027-02, 04, 12 |
| FEAT-028 | 12 (FR-028-01 à 12) | FR-028-03, 04 |
| FEAT-029 | 14 (FR-029-01 à 14) | — |
| FEAT-030 | 8 (FR-030-01 à 08) | FR-030-01 |
| **Total** | **331** | **34 FR concernées** (le finding L-10 porte sur une NFR) |

### Écarts correctement marqués (preuve de revue)

- **Corrections de bug** avec la formule « remplace le comportement legacy RG-… » et le BUG cité :
  - FR-001-07 (BUG-001), FR-001-08 (BUG-002), FR-001-09 (BUG-003) ;
  - FR-002-02 (BUG-004), FR-002-05 (BUG-005), FR-002-09 (BUG-006) ;
  - FR-003-06 (BUG-008, BUG-011), FR-003-18 (BUG-010) ;
  - FR-004-10 (BUG-013), FR-004-12 (BUG-014), FR-004-13 (BUG-015) ;
  - FR-006-06 (BUG-023), FR-007-06 (BUG-025), FR-008-04 (BUG-027), FR-009-14 (BUG-034), FR-010-09 (BUG-038, pour le statut 401), FR-011-05 (BUG-039), FR-013-10 (BUG-047) ;
  - FR-016-07 (BUG-054), FR-016-10 (BUG-055), FR-016-16 (BUG-056), FR-017-08 (BUG-058), FR-017-10 (BUG-059), FR-018-01 (BUG-064), FR-019-03 (BUG-066) ;
  - FR-021-04 (BUG-068), FR-021-08 (BUG-069), FR-021-09 (BUG-070), FR-022-04 (BUG-071), FR-022-06 (BUG-072), FR-023-02 (BUG-073), FR-024-02 (BUG-077), FR-024-05 (BUG-078), FR-025-08 (BUG-079, partie RG-025-011), FR-026-05 (BUG-083) ;
  - FR-027-06 (BUG-085), FR-027-15 (BUG-086), FR-028-09 (BUG-042), FR-028-11 (BUG-089), FR-029-05 (BUG-091), FR-030-07 (BUG-093), FR-030-08 (BUG-092).
- **[ÉVOLUTION — décision RG-027-003 …]** : FR-003-13 (D1, D5), FR-003-15 (D1 à D4), FR-003-16 (D2, D6), FR-003-27 (D3, D9), FR-003-28 (D12), FR-016-19 (D6, D7), FR-023-01 (D1, D5), FR-023-04 (D2, avec BUG-076), FR-027-16 (D5), FR-027-17 (D7, D8), FR-027-18 (D10).
- **[ASSUMPTION] qui comblent un silence ou nomment la RG concernée** : A-001-1, A-002-1, A-002-2, A-002-3, A-003-1, A-003-2, A-003-3, A-003-4, A-003-5, A-004-2, A-004-3, A-004-4, A-004-5, A-005-1, A-007-1, A-008-1, A-011-1, A-012-1 à A-012-5, A-013-1, A-014-3, A-016-1 à A-016-3, A-017-1, A-018-1, A-018-2, A-019-1, A-021-1 à A-021-5, A-022-1, A-022-2, A-023-1, A-023-2, A-024-1, A-024-2, A-027-1 à A-027-3, A-027-6 à A-027-10, A-029-1, A-029-2.
- **Textes affichés** : recoupés avec les `MESSAGES.md` des 30 fonctionnalités. Les seules divergences non justifiées sont celles listées plus haut (H-6, M-11, ainsi que H-3, H-4 et M-2 pour le texte FRAUDE).
