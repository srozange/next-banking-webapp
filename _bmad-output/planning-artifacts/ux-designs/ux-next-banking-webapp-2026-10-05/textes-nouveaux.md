---
title: "Textes nouveaux : next-banking-webapp"
status: validé par le décideur avec corrections (2026-10-05)
created: 2026-10-05
updated: 2026-10-05
sources:
  - EXPERIENCE.md (Points ouverts ; Écarts de texte avec le legacy ; Écrans nouveaux ou modifiés par les décisions)
  - prd.md (FR-003-04, FR-003-05, FR-003-27, FR-003-28, FR-003-29, FR-004-06, FR-004-10, FR-007 NFR, FR-013 notes, FR-016-19, FR-027-04, FR-027-16, §9 B1 révisée, B6 et B7, index §10 A-016-6, A-027-4)
  - docs/retro/00-global/MESSAGES.md
  - docs/retro/golden-master/scenarios/FEAT-NNN/SC-*/ ; docs/retro/golden-master/replay-config.json ; docs/retro/tools/replay-golden-master.py ; docs/retro/golden-master-contract.md (SC-003-03 et SC-010-02 exclus)
---

# Textes nouveaux

Lot des textes que le PRD ne fixe pas, pour les écrans nouveaux ou modifiés et pour les messages techniques retirés. Aucun texte du legacy n'est modifié. Les textes déjà proposés par le PRD (écran de refus D9, avis de suspension A-027-4, message d'heure butoir A-016-6) sont repris, avec l'orthographe correcte décidée le 2026-10-05 pour l'avis de suspension et le message d'heure butoir.

> **Périmètre du golden master** (décompte vérifié sur les fichiers `docs/retro/golden-master/scenarios/FEAT-NNN/SC-*/`, `replay-config.json`, `replay-golden-master.py` et `golden-master-contract.md`)
> - **33 scénarios capturés** : **28 d'écran** (adaptateur playwright : `input-actions.json` et `expected-dom.txt`) et **5 HTTP** (SC-001-01, SC-001-02, SC-001-03, SC-014-01, SC-017-01). SC-001-03 a aussi un `input-actions.json`, mais il est capturé et rejoué en HTTP (`capture_adapter: http`, comparaison `status+body_contains`) : 29 dossiers ont un `input-actions.json`, 28 seulement sont des scénarios d'écran.
> - **Exclus** : SC-003-03 (BUG-011, message technique de solde insuffisant) et SC-010-02 (BUG-038, « (HTTP 401) » du chat), deux scénarios d'écran liés à des bugs à corriger.
> - **31 scénarios applicables** : **26 d'écran** et **5 HTTP**.
> - **Mode de comparaison effectif** : les 26 scénarios d'écran applicables déclarent tous `compare.dom_text.mode = contains` dans leur `scenario.json` ; la valeur par défaut de `replay-config.json` (`contains`) n'entre donc pas en jeu (la dérivation ne l'utilise qu'en l'absence de mode déclaré, et le script de rejeu retombe sinon sur `exact`). Côté HTTP, SC-001-03 compare le statut 200 et le fragment « Identifiants incorrects » (`status+body_contains`) ; les 4 autres (`status+json_subset`) ne comparent que le code de statut. **27 des 31 scénarios comparent donc par inclusion de texte** (26 `contains` + 1 `body_contains`), 4 par le seul statut.
> - **Limite à surveiller** : le rejeu ne garde que les 5 000 premiers caractères du texte de la page (`DOM_TEXT_MAX_CHARS`). Un texte ajouté **avant** les lignes attendues peut les repousser hors de cette fenêtre ; les sections nouvelles se placent donc après le contenu comparé (voir divergences-golden-master.md).

Lecture de la colonne « scénario » : un texte **ajouté** sur un écran comparé en mode `contains` ne fait pas échouer le rejeu tant que les lignes attendues restent présentes (chaque ligne attendue doit se retrouver dans le texte de la page). Seuls les écrans et les messages réellement affichés pendant un scénario applicable, avec le jeu de données du legacy (aucun virement suspendu ni passé en revue), sont comptés. Aucun des textes de ce lot ne figure parmi les lignes attendues, les `wait_for` ou les sélecteurs d'un scénario : la correction d'orthographe ne touche aucun scénario.

| clé | écran | texte proposé (statut) | scénario du golden master concerné | traitement à l'étape G2 |
|---|---|---|---|---|
| virement.revue.titre | Virement ; Tableau de bord | « Suivi de mes virements » (texte nouveau) | SC-003-01 et SC-003-02 (écran Virement : comparent « Effectuer un virement », « Compte source »…, le message « Le compte source et destination doivent être différents ») ; SC-001-04, SC-002-01, SC-002-02, SC-003-04 (après redirection), SC-015-02, SC-016-02 (comparent le DOM du Tableau de bord) | Entrée groupée G2 « textes ajoutés » (ajout seul, sans effet en mode contains) |
| virement.revue.aide | Virement ; Tableau de bord | « Vous n'avez rien à faire : l'état de chaque virement se met à jour sur cette page. Un virement rejeté n'a pas été débité ; vous pouvez refaire votre demande. » (texte nouveau) | Mêmes scénarios que virement.revue.titre | Entrée groupée G2 « textes ajoutés » |
| virement.revue.vide | Virement ; Tableau de bord | « Aucun virement à suivre » (texte nouveau) | Mêmes scénarios que virement.revue.titre (affiché, puisque le jeu de données du legacy ne contient aucun virement passé en revue) | Entrée groupée G2 « textes ajoutés » |
| virement.revue.colonne.date | Virement ; Tableau de bord | « Date » (réutilisé tel quel : MSG-002-062, en-tête des « Dernieres operations ») | Mêmes scénarios que virement.revue.titre (l'en-tête reste affiché au-dessus de la ligne vide, CMP-003) | Entrée groupée G2 « textes ajoutés » |
| virement.revue.colonne.compte | Virement ; Tableau de bord | « Compte destinataire » (réutilisé tel quel : libellé du formulaire Virement, FEAT-003) | Mêmes scénarios que virement.revue.titre | Entrée groupée G2 « textes ajoutés » |
| virement.revue.colonne.montant | Virement ; Tableau de bord | « Montant » (réutilisé tel quel : MSG-002-065) | Mêmes scénarios que virement.revue.titre | Entrée groupée G2 « textes ajoutés » |
| virement.revue.colonne.etat | Virement ; Tableau de bord | « Etat » (réutilisé tel quel : en-tête de colonne de « Mes virements programmes », FEAT-004) | Mêmes scénarios que virement.revue.titre | Entrée groupée G2 « textes ajoutés » |
| virement.revue.colonne.butoir | Virement ; Tableau de bord | « Réponse avant le » (texte nouveau) | Mêmes scénarios que virement.revue.titre | Entrée groupée G2 « textes ajoutés » |
| virement.revue.butoir.valeur | Virement ; Tableau de bord (ligne SUSPENDU) | « {date} à {heure} » (fragment de l'avis de suspension A-027-4, FR-027-04) | aucun (affiché seulement sur une ligne SUSPENDU ; aucun scénario n'en crée) | — |
| virement.etat.execute | Virement ; Tableau de bord | « Exécuté - débité » (texte nouveau) | aucun (badge affiché seulement sur une ligne) | — |
| virement.etat.suspendu | Virement ; Tableau de bord | « En cours de vérification - non débité » (texte nouveau) | aucun | — |
| virement.etat.libere | Virement ; Tableau de bord | « Accepté - pas encore débité » (texte nouveau ; état LIBÉRÉ : solde contrôlé à la libération, débit effectué ensuite par le traitement asynchrone, décision B6, FR-016-19) | aucun | — |
| virement.etat.rejete | Virement ; Tableau de bord | « Rejeté - non débité » (texte nouveau) | aucun | — |
| virement.etat.refuse.erreur.controle | Virement (badge `transfer-status-badge-control-error`) | « Non vérifié - non débité » (texte nouveau ; la liste n'affiche pas cet état selon la supposition de EXPERIENCE §4, le badge sert là où l'état serait affiché) | aucun | — |
| virement.avis.suspension | Virement (avis après « Exécuter le virement ») | « Votre virement de {montant} EUR vers le compte {numCompte} est en cours de vérification. Rien n'a été débité. Vous aurez notre réponse avant le {date} à {heure}. Sans décision à cette heure, il sera rejeté sans aucun débit. » (texte de la notification FR-027-04, A-027-4 du PRD §10, orthographe corrigée le 2026-10-05) | aucun (aucun scénario applicable ne soumet un virement suspendu ; SC-003-03, seul à soumettre 99 999 EUR, est exclu) | — |
| virement.refus.erreur.controle | Virement (écran de refus) | « Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence {référence}. » (réutilisé tel quel : FR-003-27, décision D9) | aucun (aucun scénario ne force une erreur du contrôle) | — |
| virement.reprise.bouton | Virement (écran de refus, FR-003-28) | « Refaire ma demande » (texte nouveau) | aucun | — |
| virement.validation.comptes | Virement (compte source ou destinataire vide ; remplace `virementForm:source/target: Validation Error: Value is required.`) | « Veuillez sélectionner les deux comptes » (réutilisé tel quel : message serveur legacy de FEAT-003) | aucun (SC-003-02 passe sur l'écran mais remplit les deux comptes : le message n'est pas affiché) | — |
| virement.validation.montant | Virement (montant vide ; remplace `virementForm:amount: Validation Error: Value is required.`) | « Veuillez saisir le montant » (texte nouveau) | aucun (SC-003-02 remplit le montant) | — |
| vp.erreur.creation | Virements programmes (remplace `Erreur : {message}` à la création) | « Erreur : le virement programmé n'a pas été créé. Rien n'a été débité. Veuillez réessayer. » (texte nouveau ; préfixe legacy « Erreur : » conservé) | aucun (SC-004-01 à SC-004-03 passent sur l'écran sans provoquer d'erreur technique) | — |
| vp.erreur.etat | Virements programmes (remplace `Erreur : {message}` sur « Suspendre » / « Reactiver ») | « Erreur : l'état du virement programmé n'a pas changé. Rien n'a été débité. Veuillez réessayer. » (texte nouveau) | aucun | — |
| vp.erreur.suppression | Virements programmes (remplace `Erreur suppression : {message}`) | « Erreur suppression : le virement programmé n'a pas été supprimé et reste inchangé. Veuillez réessayer. » (texte nouveau ; préfixe legacy « Erreur suppression : » conservé) | aucun | — |
| vp.erreur.appartenance | Virements programmes (refus générique de FR-004-10, A-004-1) | « Erreur : action impossible. Rechargez la page. » (texte nouveau ; ne révèle pas l'existence du virement) | aucun | — |
| pret.erreur.simulation | Prets (remplace `Erreur simulation : {e.getMessage()}`, MSG-007-123) | « Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer. » (texte nouveau ; préfixe legacy « Erreur simulation : » conservé) | aucun (SC-007-02 lance une simulation valide : le message n'est pas affiché) | — |
| pret.erreur.demande | Prets (remplace `Erreur : {e.getMessage()}` de la demande, MSG-007-128 ; hors échec de transmission, déjà fixé par A-024-1) | « Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer. » (texte nouveau) | aucun | — |
| pret.erreur.paiement | Prets (remplace `Erreur : {e.getMessage()}` de « Marquer paye », MSG-007-129) | « Erreur : l'échéance n'a pas été marquée comme payée. Veuillez réessayer. » (texte nouveau ; « Marquer paye » ne débite aucun compte, FR-007) | aucun | — |
| pret.erreur.suppression | Prets (remplace `Erreur suppression : {e.getMessage()}`, MSG-007-130) | « Erreur suppression : le prêt n'a pas été supprimé. Veuillez réessayer. » (texte nouveau) | aucun | — |
| beneficiaire.erreur.ajout | Beneficiaires (remplace `Erreur lors de l'ajout du bénéficiaire : {message}`, MSG-013-077) | « Erreur lors de l'ajout du bénéficiaire. Veuillez réessayer. » (texte nouveau) | aucun (SC-013-02 réussit l'ajout : seul « Bénéficiaire ajouté avec succès » est comparé) | — |
| revue.titre | Administration (section avant « Alertes Fraude — Moteur de détection ») | « Virements suspendus — En attente de revue » (texte nouveau) | SC-016-01 (ADMIN charge Administration ; compare « TOTAL CLIENTS », « Pipeline Prets », « Gestion des clients »…) | Entrée groupée G2 « textes ajoutés » (section placée après « Gestion des clients », fenêtre de 5 000 caractères) |
| revue.colonne.client | Administration | « Client » (réutilisé tel quel : en-tête des tableaux Pipeline Prets et Alertes Fraude, FEAT-016) | SC-016-01 (l'en-tête reste affiché au-dessus de la ligne vide) | Entrée groupée G2 « textes ajoutés » |
| revue.colonne.montant | Administration | « Montant » (réutilisé tel quel : en-tête Pipeline Prets, FEAT-016) | SC-016-01 | Entrée groupée G2 « textes ajoutés » |
| revue.colonne.compte | Administration | « Compte destinataire » (réutilisé tel quel : libellé du formulaire Virement, FEAT-003) | SC-016-01 | Entrée groupée G2 « textes ajoutés » |
| revue.colonne.motif | Administration | « Motif de suspension » (texte nouveau) | SC-016-01 | Entrée groupée G2 « textes ajoutés » |
| revue.colonne.butoir | Administration | « Heure butoir » (texte nouveau ; terme du message A-016-6) | SC-016-01 | Entrée groupée G2 « textes ajoutés » |
| revue.colonne.actions | Administration | « Actions » (réutilisé tel quel : en-tête Pipeline Prets, FEAT-016) | SC-016-01 | Entrée groupée G2 « textes ajoutés » |
| revue.vide | Administration | « Aucun virement suspendu » (texte nouveau) | SC-016-01 (affiché, puisque le jeu de données du legacy ne contient aucun virement suspendu) | Entrée groupée G2 « textes ajoutés » |
| revue.motif.score | Administration (cellule motif) | « Score cumulé {score} - signaux : {signaux} » ({signaux} : codes de règle tels qu'affichés dans « Règle déclenchée », séparés par des virgules ; texte nouveau) | aucun (cellule affichée seulement sur une ligne) | — |
| revue.motif.seuil | Administration (cellule motif) | « Montant supérieur ou égal au seuil anti-fraude ({seuilFraudeEur} EUR) » (texte nouveau ; « seuil anti-fraude » repris de MSG-002-043) | aucun | — |
| revue.motif.cumul | Administration (cellule motif, après FR-027-18) | « Cumul sur 24 h glissantes de {cumul} EUR, supérieur au plafond de {plafond} EUR » (texte nouveau) | aucun | — |
| revue.bouton.liberer | Administration | « Libérer » (texte nouveau) | aucun (bouton affiché seulement sur une ligne) | — |
| revue.bouton.rejeter | Administration | « Rejeter » (texte nouveau) | aucun | — |
| revue.modale.titre.liberer | Administration (modale) | « Libérer le virement de {montant} EUR vers le compte {numCompte} » (texte nouveau) | aucun | — |
| revue.modale.titre.rejeter | Administration (modale) | « Rejeter le virement de {montant} EUR vers le compte {numCompte} » (texte nouveau) | aucun | — |
| revue.modale.champ.motif | Administration (modale) | « Motif de la décision * » (texte nouveau) | aucun | — |
| revue.modale.bouton.valider | Administration (modale) | « Valider » (texte nouveau) | aucun | — |
| revue.modale.bouton.annuler | Administration (modale) | « Annuler » (réutilisé tel quel : bouton du formulaire « + Ajouter une valeur » de Bourse, FEAT-009) | aucun | — |
| revue.erreur.motif.manquant | Administration (modale) | « Veuillez indiquer un motif » (réutilisé tel quel : MSG-007-127, Prets) | aucun | — |
| revue.succes.liberer | Administration | « Libération enregistrée : le virement est accepté, son débit va être effectué. » (texte nouveau ; à la libération, le solde est contrôlé et le virement passe à LIBÉRÉ ; le débit est effectué ensuite par le traitement asynchrone, qui le passe à EXÉCUTÉ, FR-016-19, B6) | aucun | — |
| revue.succes.rejeter | Administration | « Rejet enregistré : le virement est rejeté sans débit. » (texte nouveau) | aucun | — |
| revue.erreur.solde.insuffisant | Administration (libération demandée sur un solde devenu insuffisant) | « Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit. » (texte nouveau ; décision de finalisation B6 : la libération est refusée et le virement passe à REJETÉ sans aucun débit, FR-016-19) | aucun | — |
| revue.motif.rejet.solde.debit | Administration (historique des virements traités, motif en clair) | « Rejeté : solde insuffisant au moment du débit » (texte nouveau à valider ; décision de finalisation B7 : virement libéré puis rejeté sans aucun débit, le solde étant devenu insuffisant avant le débit asynchrone, FR-016-19, FR-027-16) | aucun (l'historique n'apparaît que s'il contient un virement ; le jeu de données du legacy n'en contient aucun) | — |
| revue.erreur.heure.butoir | Administration | « Heure butoir dépassée : le virement est rejeté. » (FR-016-19, A-016-6 du PRD §10, orthographe corrigée le 2026-10-05) | aucun | — |
| a11y.lien.evitement | Toutes les pages (lien d'évitement, visible au focus) | « Aller au contenu principal » (texte nouveau) | Les 26 scénarios d'écran applicables : SC-001-04, SC-001-05, SC-002-01, SC-002-02, SC-003-01, SC-003-02, SC-003-04, SC-004-01, SC-004-02, SC-004-03, SC-005-01, SC-006-01, SC-007-01, SC-007-02, SC-008-01, SC-009-01, SC-010-01, SC-011-01, SC-012-01, SC-013-01, SC-013-02, SC-015-01, SC-015-02, SC-016-01, SC-016-02, SC-017-02 (un lien placé hors écran entre dans `inner_text`, même invisible). SC-001-03, rejoué en HTTP, ne compare que le fragment « Identifiants incorrects » | Entrée groupée G2 « textes ajoutés » |
| a11y.nouvel.onglet | Documents (liens « Telecharger PDF » et relevés mensuels ; texte réservé aux lecteurs d'écran, ajouté au nom du lien) | « (nouvel onglet) » (texte nouveau) | SC-008-01 et SC-017-02 (comparent « Telecharger PDF » ; SC-017-02 l'attend aussi par wait_for : « Telecharger PDF (nouvel onglet) » le contient toujours) | Entrée groupée G2 « textes ajoutés » |

Hors tableau :
- Le motif d'un virement (`virementForm:motif: Validation Error: Value is required.`) ne reçoit **aucun** texte de remplacement : le champ est facultatif (FR-003-01).
- Un montant non numérique, nul ou négatif garde le message déjà fixé par le PRD (« Erreur lors du virement : Le montant doit etre positif », A-003-1) ; virement.validation.montant ne couvre que le champ vide.
- Virement important exécuté (statut PENDING, de 5 000 € à 9 999,99 €) : les textes du legacy (notification ALERTE « … en attente de validation - transaction importante, verification en cours. », mention « [EN ATTENTE] », badge « En attente » au relevé) sont gardés sans reformulation pour la démonstration (décision B1 révisée, BUG-009 conservé provisoirement). La reformulation « transaction importante » est **reportée après la démonstration (FR-003-29)** et ne fait pas partie de ce lot.
- Hors du lot : le texte de la notification de solde insuffisant à l'échéance d'un virement programmé (A-004-3) et le message « Compte destinataire introuvable » (A-004-4) restent des suppositions du PRD, non reprises ici.

## Entrée groupée de l'étape G2 « textes ajoutés »

Les lignes ci-dessus marquées « Entrée groupée G2 » ajoutent du texte à un écran comparé, sans modifier ni retirer aucune ligne attendue. Elles font l'objet d'une seule entrée dans la correspondance des textes (text_map) de l'étape G2 : ajouts uniquement, sans effet en mode `contains`, aucune ligne attendue remplacée. Le libellé de l'entrée est repris dans EXPERIENCE.md (section « Écarts de texte avec le legacy », partie C).

## Règle suivie

- **Aucun texte du legacy n'est modifié.** Un libellé legacy qui convient est réutilisé tel quel, avec son origine ; le texte D9 est repris mot pour mot.
- **Textes vus par le client** : neutres ; aucun mot sur la fraude, un signal ou un score (« vérification » reprend le vocabulaire de A-027-4) ; chaque texte qui porte sur un virement ou une échéance dit si quelque chose a été débité (badges d'état, avis, erreurs de Virements programmes) ; chaque texte dit ce que le client doit faire (« Veuillez réessayer », « Rechargez la page », « vous pouvez refaire votre demande ») ou qu'il n'a rien à faire (virement.revue.aide). Les messages de champ obligatoire, émis avant tout traitement, ne parlent pas de débit, comme leurs voisins legacy (« Veuillez sélectionner les deux comptes »). Aucun libellé nouveau n'emploie « en attente » pour un virement exécuté.
- **Côté administrateur** : le motif de suspension s'affiche en clair, sous l'une des trois formes (score cumulé et signaux ; montant supérieur ou égal au seuil anti-fraude ; cumul glissant au-delà du plafond). Les messages de décision disent l'issue et le débit, sur le modèle de A-016-6 ; une libération acceptée ne dit pas « débité », puisque le débit est effectué ensuite par le traitement asynchrone.

## Orthographe

- **Textes nouveaux : orthographe correcte ; textes du legacy : à l'identique.**
  - Tout texte nouveau s'écrit avec une orthographe correcte, accents compris, quel que soit l'écran et quels que soient ses voisins legacy (décision du 2026-10-05). Cela vaut pour l'avis de suspension (A-027-4) et le message d'heure butoir (A-016-6).
  - Tout texte repris du legacy garde son orthographe d'origine, sans exception (« Etat », « Deconnexion », « Telecharger PDF »…).
  - Les préfixes legacy « Erreur : », « Erreur suppression : », « Erreur simulation : » restent tels quels devant un texte nouveau.
- **Ponctuation** :
  - message d'une seule phrase sans point final, comme les voisins (« Veuillez indiquer un motif », « Aucun pret en attente ») ;
  - message de plusieurs phrases avec point final, comme A-006-1 et A-027-4 ;
  - séparateur « - » dans les badges ; tiret long « — » dans le titre de section admin, comme « Alertes Fraude — Moteur de détection » ;
  - astérisque des champs obligatoires, comme « Libelle * ».
- **Clés** : minuscules et points uniquement, préfixe par écran (`virement.`, `vp.`, `pret.`, `beneficiaire.`, `revue.`, `a11y.`).
