---
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - _bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/prd.md
  - _bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/addendum.md
  - _bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/ARCHITECTURE-SPINE.md
  - _bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/golden-master-architecture.md
  - _bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/versions-verifiees.md
  - _bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/ecarts-voulus-golden-master.md
  - _bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/DESIGN.md
  - _bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/EXPERIENCE.md
  - _bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/textes-nouveaux.md
  - _bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/divergences-golden-master.md
  - _bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/SYNTHESE.md
  - docs/quality-gates.md
  - docs/retro/golden-master/derived/ (31 scénarios dérivés)
---

# next-banking-webapp - Découpage en epics

## Overview

Ce document découpe en epics et stories les exigences du PRD, du contrat UX et de l'architecture de next-banking-webapp (rénovation de l'application bancaire legacy), avec la traçabilité vers le golden master dérivé.

## Arbitrages du 2026-10-05

Arbitrages pris le 2026-10-05 par délégation du propriétaire, pour lever les prérequis « à confirmer » des stories :

- Toutes les suppositions A-xxx de « Suppositions à reprendre avant une story » sont confirmées telles qu'elles sont rédigées.
- RP-01 : le journal des traitements de l'administration garde les 50 dernières entrées et ajoute un filtre par traitement (liste des traitements, « Tous » par défaut) ; critère ajouté à FR-016-17.
- RP-02 : aucun reclassement en risque élevé.
- RP-05 : pas de verrouillage du compte ni de politique de mot de passe nouvelle (comportement du legacy conservé) ; l'alerte MULTIPLE_FAILED_AUTH (story 3.10) couvre les échecs répétés. Accessibilité, navigateurs et mobile restent hors démonstrateur.
- Mots-clés des catégories du budget (story 4.12) : la proposition du lot « Stories » de `textes-nouveaux.md` est confirmée.
- Champ de `credits.pret-demande.v1` (story 5.2) : la proposition `pretId` est retenue ; la story l'inscrit au catalogue AD-22.
- AR-94 : l'empreinte des contrôles (`.bmad-loop/gates.sha256`) et le hook de protection sont reportés après la démonstration ; les stories de l'epic 2 démarrent sans.
- AR-106 (D10) : mécanisme d'AD-16 confirmé (jeton de 30 minutes renouvelé sur activité, expiration sans activité).
- AR-107 (D11) : Inter seule, Graphik non utilisée.
- Story 10.1 : le PRD (FR-003-29) fait foi pour les trois textes ; la story reste après la démonstration (décision B1).

## Requirements Inventory

### Functional Requirements

333 exigences fonctionnelles (FR-001-01 à FR-030-08), reprises du PRD §4 dans l'ordre. `{GM: …}` : scénarios du golden master cités dans le bloc de l'exigence ; `GM FEAT` : scénarios cités par la fonctionnalité.

##### FEAT-001 — Connexion
GM FEAT : SC-001-01, SC-001-02, SC-001-03, SC-001-04, SC-001-05
- FR-001-01 : Écrans accessibles sans authentification — La page de connexion, la page de retour open banking et les ressources statiques sont servies sans session ; la page de connexion affiche l'onglet « BankApp Demo - Connexion », le logo « DB », le titre « Demo Bank », le sous-titre « Espace client — Connexion sécurisée », les libellés « Identifiant » et « Mot de passe » et le bouton « Me connecter ». {GM: aucun}
- FR-001-02 : Redirection des accès non authentifiés vers la connexion — Tout autre écran demandé sans authentification redirige vers la page de connexion (voir FR-030-01). {GM: aucun}
- FR-001-03 : Champs obligatoires du formulaire de connexion — Identifiant vide → rejet et « Identifiant requis » ; mot de passe vide → « Mot de passe requis » ; messages visibles à côté du champ, contrôle aussi côté serveur. {GM: aucun}
- FR-001-04 : Authentification réussie et ouverture de session — Identifiant et mot de passe correspondant à un utilisateur enregistré et actif ouvrent une session authentifiée portant son contexte, puis redirigent vers l'écran d'accueil de son rôle (FR-001-05). {GM: aucun}
- FR-001-05 : Écran d'accueil selon le rôle — Après connexion, ADMIN → administration, CONSEILLER → espace conseiller, CLIENT → tableau de bord (voir FR-030-03). {GM: aucun}
- FR-001-06 : Échec d'authentification — Identifiant inconnu ou mot de passe faux : reste sur la page de connexion avec « Identifiants incorrects » et le détail « Vérifiez votre login et mot de passe », message identique dans les deux cas. {GM: aucun}
- FR-001-07 : Refus de connexion d'un compte désactivé — Un compte désactivé par l'admin avec identifiants valides n'ouvre aucune session et reste sur la page de connexion (A-001-1 : même message que FR-001-06 et audit LOGIN_FAILED) ; reconnexion possible après réactivation. Remplace RG-001-017. [BUG-001 corrigé] {GM: aucun}
- FR-001-08 : Mots de passe protégés par hachage — Mots de passe stockés hachés par fonction sécurisée à sens unique et vérifiés par comparaison à l'empreinte, jamais en clair (y compris données de référence) ; la valeur stockée diffère du mot de passe et la connexion réussit/échoue selon le bon/mauvais mot de passe. Remplace RG-001-018. [BUG-002 corrigé] {GM: aucun}
- FR-001-09 : Nouvelle session à la connexion — À l'authentification réussie, la session existante est invalidée : l'identifiant de session avant connexion diffère de celui d'après et l'ancien redirige vers la connexion. Remplace RG-001-019 (voir FR-030-08). [BUG-003 corrigé] {GM: aucun}
- FR-001-10 : Déconnexion — La déconnexion invalide la session courante et redirige vers la page de connexion (fusion RG-001-010 + RG-030-010). {GM: aucun}
- FR-001-11 : Accès refusé aux écrans réservés à un autre rôle — Un non-ADMIN demandant l'administration est redirigé vers le tableau de bord sans message ; un CLIENT demandant l'espace conseiller est redirigé vers le tableau de bord (voir FR-030-02). {GM: aucun}
- FR-001-12 : Journalisation des connexions et déconnexions — Connexion réussie → audit LOGIN « Connexion réussie » avec IP ; échec → LOGIN_FAILED « Tentative échouée » avec IP ; déconnexion → LOGOUT « Déconnexion » (voir FR-026-01, FR-026-05). {GM: aucun}
- FR-001-13 : Résolution de l'adresse IP du client — L'IP d'audit est le premier élément de « X-Forwarded-For » s'il est présent, non vide et posé par un mandataire de confiance (A-001-2, écart à RG-001-016), sinon l'adresse distante de la connexion. {GM: aucun}

##### FEAT-002 — Tableau de bord
GM FEAT : SC-002-01, SC-002-02
- FR-002-01 : Données de l'utilisateur relues à chaque ouverture — À chaque ouverture, les données de l'utilisateur sont relues depuis l'identifiant de session et le contexte mis à jour ; l'en-tête affiche « Bonjour, {fullName} » et le lien « Deconnexion ». {GM: aucun}
- FR-002-02 : Utilisateur supprimé pendant sa session — Si l'utilisateur en session n'existe plus, aucune erreur technique ni trace n'est affichée ; (A-002-1) la session est terminée et redirection vers la page de connexion sans session authentifiée. Remplace RG-002-002. [BUG-004 corrigé] {GM: aucun}
- FR-002-03 : Solde total — Sous « Mes comptes Demo Bank », la carte « Solde total Demo Bank » (sous-titre « Tous comptes Demo Bank confondus ») affiche la somme des soldes de tous les comptes de l'utilisateur, EUR 0.00 s'il n'en a aucun. {GM: aucun}
- FR-002-04 : Liste des comptes — Pour chaque compte : libellé du type (COURANT, EPARGNE…), solde en EUR à 2 décimales avec séparateur de milliers, et « N {numéro de compte} » ; rendu exact des montants repris du legacy capturé par le golden master (A-002-2). {GM: aucun}
- FR-002-05 : Dernières opérations du compte courant — Affiche les 5 opérations les plus récentes du compte COURANT, de la plus récente à la plus ancienne, quel que soit l'ordre de création des comptes ; tableau vide sans compte (et sans compte COURANT, A-002-3). Remplace RG-002-006/RG-002-007 (premier compte quel que soit son type). [BUG-005 corrigé] {GM: aucun}
- FR-002-06 : Présentation des opérations — La section « Activite recente » / « Dernieres operations » a les colonnes « Date », « Description », « Type », « Montant », « Solde apres » ; date au format dd/MM/yyyy HH:mm, type DEBIT ou CREDIT ; bouton « Voir le releve complet » toujours affiché. {GM: aucun}
- FR-002-07 : Centre de notifications de l'utilisateur — « Centre de notifications » n'affiche que les notifications de l'utilisateur connecté, plus récente en premier, les non lues marquées « Non lu » ; sans notification : « Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel ». {GM: aucun}
- FR-002-08 : Badge et actions sur les notifications — Avec au moins une non lue : badge « {N} nouvelle(s) » (N = nombre de non lues) et bouton « Tout marquer lu » ; avec au moins une notification : bouton « Effacer tout » ; « Tout marquer lu » marque tout lu, « Effacer tout » supprime toutes les notifications de l'utilisateur. {GM: aucun}
- FR-002-09 : Conservation des notifications — Nouvelle notification en tête de liste ; limite configurable de 20 par défaut par utilisateur, la plus ancienne supprimée au-delà ; notifications (ordre et état de lecture) conservées après redémarrage du serveur. Remplace RG-002-020. [BUG-006 corrigé] {GM: aucun}
- FR-002-10 : Une seule notification par virement traité — Exactement une notification par virement FEAT-003, produite à la décision du contrôle de fraude : FRAUDE si suspendu (score > 75, montant ≥ seuil fraude 10 000 EUR, ou cumul glissant FR-027-18), CONFIRMATION « Virement de {montant} EUR vers le compte {numCompte} confirme » (+ « (motif : {motif}) ») si exécuté < 5 000 EUR, ALERTE « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. » si exécuté ≥ 5 000 EUR ; aucune notification pour un refus pour erreur du contrôle (A-003-8) ni pour libération/rejet/rejet automatique. Remplace RG-002-024. [ÉVOLUTION — décision RG-027-003 D1, D2, D3] [BUG-007 corrigé] {GM: aucun}
- FR-002-11 : Navigation du tableau de bord selon le rôle — Tous voient « Tableau de bord », « Virement », « Virements programmes », « Releve », « Budget », « Prets », « Documents », « Bourse », « Mon Conseiller », « Produits », « Comptes Tiers », « Beneficiaires » ; « Espace Conseiller » seulement CONSEILLER et ADMIN, « Administration » seulement ADMIN (voir FR-030-04). {GM: aucun}
- FR-002-12 : Patrimoine total avec les comptes d'autres banques — Avec connexion d'agrégation active : section « Mes comptes externes (Open Banking) », carte « Patrimoine total » (sous-titre « Demo Bank + comptes externes ») = solde total Demo Bank + soldes externes, « Comptes externes via Powens Open Banking ({nombre} comptes) » et bouton « Voir le detail et les transactions » ; sinon section « Open Banking », texte « Agregez vos comptes d autres banques pour une vue complete de votre patrimoine » et bouton « Connecter mes autres banques via Powens » ; appel conforme à NFR-INT. {GM: aucun}

##### FEAT-003 — Effectuer un virement
GM FEAT : SC-003-01, SC-003-02, SC-003-03, SC-003-04
- FR-003-01 : Accès au formulaire et comptes proposés — Tout rôle authentifié accède à l'écran « Effectuer un virement » ; « Compte source » et « Compte destinataire » ne proposent que les comptes de l'utilisateur après « -- Choisir un compte -- » (seule option sans compte) ; champs « Montant (€) » (indice « Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude »), « Motif » facultatif ≤ 100 caractères (contrôlé côté serveur), bouton « Exécuter le virement », lien « Virement SEPA externe — Gérer mes bénéficiaires ». {GM: aucun}
- FR-003-02 : Les deux comptes sont obligatoires — Compte source ou destinataire non sélectionné → rejet avec « Veuillez sélectionner les deux comptes ». {GM: aucun}
- FR-003-03 : Comptes source et destinataire différents — Même compte en source et destination → rejet avec « Le compte source et destination doivent être différents », valeurs saisies conservées. {GM: aucun}
- FR-003-04 : Montant strictement positif — Montant absent, non numérique, nul ou négatif → rejet (aussi côté serveur), sans détail technique (NFR-SEC-04) ; (A-003-1) message « Erreur lors du virement : Le montant doit etre positif ». {GM: aucun}
- FR-003-05 : Compte introuvable — Compte source ou destinataire inexistant → rejet sans débit ni crédit, (A-003-1) message « Erreur lors du virement : Compte introuvable » sans détail technique. {GM: aucun}
- FR-003-06 : Solde insuffisant : refus avec un message clair — Solde source < montant → rejet, aucun solde modifié, message (A-003-2) « Solde insuffisant sur le compte source » sans nom d'erreur interne (test : 2 130,00 € et virement de 9 999,00 €). Remplace RG-003-007. [BUG-008 et BUG-011 corrigés] {GM: aucun}
- FR-003-07 : Ordre des contrôles — Les contrôles FR-003-02 à FR-003-06 s'appliquent avant le contrôle de fraude ; un virement rejeté par l'un d'eux n'est ni exécuté ni soumis au contrôle de fraude (A-003-3). {GM: aucun}
- FR-003-08 : Soumission sans étape de confirmation — La soumission déclenche directement le traitement, sans confirmation ; l'issue (exécuté, suspendu, refusé pour erreur) est fixée par FR-027-02. {GM: aucun}
- FR-003-09 : Débit, crédit et opérations au relevé — Débit et crédit atomiques uniquement à l'exécution (immédiate ou après libération FR-016-19), avec opération DEBIT « Virement vers {compte destinataire} - {motif} » et CREDIT « Virement depuis {compte source} - {motif} » avec solde après, préfixées « [EN ATTENTE] » si PENDING ; suspendu, rejeté ou refusé → aucun solde modifié, aucune opération. Remplace RG-003-010/RG-003-011. [ÉVOLUTION — décision RG-027-003 D1, D3] {GM: aucun}
- FR-003-10 : Virements importants exécutés (statut PENDING) — Virement exécuté ≥ seuil de mise en attente (5 000 € par défaut) → PENDING, exécuté et débité immédiatement, notification ALERTE « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. » et opérations préfixées « [EN ATTENTE] » ; 4 999,99 € → SUCCESS ; 10 000,00 € ou score > 75 → SUSPENDU sans débit. [ÉVOLUTION — décision RG-027-003 D1, D2] [décision de finalisation B1 révisée : BUG-009 conservé provisoirement] {GM: aucun}
- FR-003-11 : Journal d'audit de l'exécution du virement — Au passage à l'état EXÉCUTÉ (immédiat ou après libération), une entrée d'audit d'exécution avec identifiant de l'émetteur, montant et motif (code FR-026-03) ; soumission, libération, rejet et rejet automatique ont leurs propres entrées. {GM: aucun}
- FR-003-12 : Retour au tableau de bord — Après exécution, le tableau de bord s'affiche avec soldes à jour ; pour un virement suspendu ou refusé pour erreur, l'affichage suit FR-027-04 et FR-003-27. {GM: aucun}
- FR-003-13 : Remise au traitement asynchrone — Seul un virement exécuté (immédiatement ou après libération) est remis au traitement asynchrone (FR-023-01), sans notification ; suspendu → file de revue admin, refusé pour erreur → aucun traitement ; la remise est enregistrée avec la validation et son échec fait échouer la validation (FR-023-02). [ÉVOLUTION — décision RG-027-003 D1, D5] {GM: aucun}
- FR-003-14 : Notification selon l'issue du contrôle et le montant — Une seule notification par virement, à la décision du contrôle : FRAUDE si suspendu (quel que soit le montant), CONFIRMATION si exécuté < 5 000 €, ALERTE (texte legacy « en attente de validation - transaction importante, verification en cours ») si exécuté ≥ 5 000 € ; ≥ 10 000 € toujours suspendu ; aucune nouvelle notification après libération, aucune pour refus pour erreur. [ÉVOLUTION — décision RG-027-003 D1, D2] {GM: aucun}
- FR-003-15 : Contrôle de fraude avant la validation finale — Le contrôle de fraude temps réel (FR-027-01) s'exécute pour chaque virement avant validation finale, toute erreur est journalisée, l'issue est fixée par FR-027-02. Remplace RG-003-023. [ÉVOLUTION — décision RG-027-003 D1 à D4] {GM: aucun}
- FR-003-16 : Fin de l'avertissement « N alerte(s) » — Le message « Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié. » n'est plus affiché ; un virement de score > 75 est suspendu et le client voit l'avis FR-027-04 ; écart golden master traité à l'étape G2. [ÉVOLUTION — décision RG-027-003 D2, D6] {GM: aucun}
- FR-003-17 : Signal LARGE_AMOUNT (règles voisines) — Un seul signal LARGE_AMOUNT par virement (A-003-4) : score 40 de 5 000 € inclus à 15 000 € exclus (« Transaction amount {amount} exceeds the large threshold of {threshold} »), score 80 à partir de 15 000 € (« … exceeds the very-large threshold of {threshold} »). {GM: aucun}
- FR-003-18 : Signal HIGH_FREQUENCY sur les transactions du jour — Signal HIGH_FREQUENCY de score 50 si le client a plus de 8 transactions exécutées le jour courant (débits et crédits au relevé, A-027-11), jours précédents exclus : 100 anciennes + 8 du jour → pas de signal, 9 du jour → signal. Remplace RG-003-027. [BUG-010 corrigé] {GM: aucun}
- FR-003-19 : Signal NEW_BENEFICIARY — Signal NEW_BENEFICIARY de score 35 si le compte destinataire n'est pas un bénéficiaire actif du client (= FR-027-07). {GM: aucun}
- FR-003-20 : Signal UNUSUAL_HOURS — Signal UNUSUAL_HOURS de score 45 si le virement est initié avant 06:00 ou après 23:00, heure serveur (= FR-027-08). {GM: aucun}
- FR-003-21 : Signal ROUND_AMOUNT (règles voisines) — Signal ROUND_AMOUNT de score 20 si montant ≥ 1 000 € et multiple exact de 1 000, description « Transaction amount {amount} is a suspicious round figure (multiple of 1000) ». {GM: aucun}
- FR-003-22 : Signal FOREIGN_IBAN — Signal FOREIGN_IBAN de score 30 si le numéro du compte destinataire ne commence pas par « FR » (insensible à la casse, FR-027-09). {GM: aucun}
- FR-003-23 : Signal RAPID_SUCCESSION (règles voisines) — Signal RAPID_SUCCESSION de score 55 si le précédent virement du client soumis au contrôle (A-003-5) date de moins de 30 secondes, description « Previous transaction was only {elapsedSeconds} seconds ago (threshold: {rapidSuccessionSeconds}s) ». {GM: aucun}
- FR-003-24 : Signal VELOCITY_CHECK (règles voisines) — Signal VELOCITY_CHECK de score 65 si le client a effectué 5 virements exécutés ou plus de plus de 500 € dans les 60 dernières minutes (A-003-9, = FR-027-10). {GM: aucun}
- FR-003-25 : Enregistrement des alertes au-delà du score 75 — Si la somme des scores des signaux dépasse strictement 75, toutes les alertes sont enregistrées (une par signal, statut OPEN, = FR-027-03). {GM: aucun}
- FR-003-26 : Motif dans la notification de confirmation (règles voisines) — Si le motif est renseigné, la CONFIRMATION devient « Virement de {montant} EUR vers le compte {n°_compte_destinataire} confirme (motif : {motif}) » ; motif absent, vide ou uniquement d'espaces → message sans motif. {GM: aucun}
- FR-003-27 : Écran de refus pour erreur du contrôle — Sur refus pour erreur du contrôle, l'écran affiche « Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence {référence}. » (référence = identifiant de trace), sans mention de fraude ni signal ; aucun solde modifié, aucune notification. [ÉVOLUTION — décision RG-027-003 D3, D9] {GM: aucun}
- FR-003-28 : Reprise préremplie après un refus — Après un refus pour erreur du contrôle, le client peut refaire son virement depuis un formulaire prérempli (compte source, destinataire, montant, motif — A-003-11) ; hors socle, ressaisie d'ici là. [ÉVOLUTION — décision RG-027-003 D12] [STORY SUIVANTE : livrée après le socle RG-027-003 et après le cumul glissant (FR-027-18)] {GM: aucun}
- FR-003-29 : Reformulation des textes de transaction importante — Après la démonstration, un virement PENDING ≤ score 75 produit l'ALERTE « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. » et des opérations préfixées « [TRANSACTION IMPORTANTE] » (badge relevé « Transaction importante », « [EN ATTENTE] » toujours reconnu pour l'existant) ; aucun texte ne contient « en attente » ni « verification » ; statut technique PENDING conservé. [STORY SUIVANTE : après la démonstration] [BUG-009 corrigé par cette story] {GM: aucun}

##### FEAT-004 — Virements programmés
GM FEAT : SC-004-01, SC-004-02, SC-004-03
- FR-004-01 : Accès à l'écran des virements programmés — Sans session authentifiée, la demande de l'écran redirige vers la page de connexion. {GM: aucun}
- FR-004-02 : Formulaire de création et comptes source proposés — Onglet « BankApp Demo - Virements programmes », section « Nouveau virement programme » avec libellés « Compte debiteur », « Montant (EUR) », « Frequence », « IBAN / N compte beneficiaire », « Libelle beneficiaire », « Date de premiere execution », « Motif (optionnel) », exemples « ex: 200.00 », « ex: FR76-0001-0002 », « ex: Compte epargne », « yyyy-MM-dd », « ex: Epargne mensuelle », bouton « Creer le virement programme » ; « Compte debiteur » commence par « -- Choisir -- » et ne propose que les comptes du client, libellés « {numéro de compte} ({type de compte}) - {solde} EUR ». {GM: aucun}
- FR-004-03 : Liste des virements programmés du client — « Mes virements programmes » liste uniquement ceux du client, triés par prochaine exécution croissante, colonnes « Source », « Beneficiaire », « Montant », « Frequence », « Prochain » (dd/MM/yyyy), « Nb executions », « Etat » (« Actif »/« Inactif »), « Actions » (« Suspendre »+« Supprimer » si actif, « Reactiver »+« Supprimer » si inactif) ; vide : « Aucun virement programme configure ». {GM: aucun}
- FR-004-04 : Champs obligatoires et format de date du formulaire — Rejet avec « Veuillez selectionner un compte source », « Veuillez saisir le numero de compte destinataire », « Veuillez saisir le libelle du beneficiaire », « Veuillez saisir la date de premiere execution » (champs vides/blancs) ou « Format de date invalide (attendu : yyyy-MM-dd) » ; le formulaire garde les valeurs saisies. {GM: aucun}
- FR-004-05 : Montant invalide (virement programmé, simulation et demande de prêt) — Montant absent ou ≤ 0 → rejet avec exactement « Montant invalide » pour la création de virement programmé, la simulation de prêt et la demande de prêt (fusion RG-004-013, RG-007-003, RG-007-012). {GM: aucun}
- FR-004-06 : Contrôles à l'enregistrement du virement programmé — À l'enregistrement, rejet sans création avec « Erreur : Client ou compte introuvable », « Erreur : Compte non autorise » (compte source d'un autre client) ou « Erreur : Montant invalide » (montant absent ou ≤ 0). {GM: aucun}
- FR-004-07 : Création réussie — Le virement est enregistré actif, nb d'exécutions 0, horodatage de création, message « Virement programme cree avec succes », champs vidés et liste rechargée ; une date de première exécution passée est acceptée et exécutée au prochain passage (≤ 5 minutes). {GM: aucun}
- FR-004-08 : Fréquences proposées — Six fréquences dans l'ordre : « Hebdomadaire » (7 jours), « Mensuel » (1 mois, par défaut), « Bimestriel » (2 mois), « Trimestriel » (3 mois), « Semestriel » (6 mois), « Annuel » (1 an). {GM: aucun}
- FR-004-09 : Suspension, réactivation et suppression — « Suspendre » passe un virement actif à inactif, « Reactiver » le repasse actif (liste rechargée), « Supprimer » le supprime définitivement ; uniquement sur les virements du client connecté. {GM: aucun}
- FR-004-10 : Contrôle d'appartenance avant suspension, réactivation ou suppression — Une action sur le virement programmé d'un autre client est rejetée avec un message générique ne révélant pas son existence (A-004-1), le virement gardant état, prochaine date et existence. Remplace RG-004-043. [BUG-013 corrigé] {GM: aucun}
- FR-004-11 : Exécution automatique des virements dus — Toutes les 5 minutes, les virements actifs échus sont exécutés l'un après l'autre, chaque échéance passant par le contrôle de fraude (FR-021-10, décision B2) ; compte bénéficiaire interne et solde suffisant → débit, crédit, opérations DEBIT et CREDIT. {GM: aucun}
- FR-004-12 : Solde insuffisant à l'échéance — Solde < montant à l'échéance : aucune exécution ni changement (état, nb d'exécutions, prochaine date), retentative 5 minutes plus tard, journal « Solde insuffisant pour virement programme id={id} » et une seule notification ALERTE par échéance (A-004-2, A-004-3) indiquant l'échec faute de solde (test : 500 € pour 499,99 €). Remplace RG-004-061. [BUG-014 corrigé] {GM: aucun}
- FR-004-13 : Bénéficiaire obligatoirement interne à la banque — Création vers un numéro de compte absent de la banque rejetée avec un message clair (A-004-4 : « Compte destinataire introuvable »), sans création ; à l'exécution, rejet avant tout débit (FR-021-04). Remplace RG-004-063. [BUG-015 corrigé] {GM: aucun}
- FR-004-14 : Mise à jour du virement après exécution — Après exécution réussie : horodatage enregistré, nb d'exécutions +1, prochaine date calculée depuis la date d'échéance prévue et la fréquence (FR-021-08), pas depuis l'heure réelle (A-004-5). Remplace RG-004-064. [BUG-069 corrigé] {GM: aucun}
- FR-004-15 : Échec isolé d'un virement pendant un passage — L'échec d'un virement programmé est journalisé en avertissement et n'empêche pas l'exécution des autres virements dus du passage (FR-021-09). {GM: aucun}

##### FEAT-005 — Relevé de compte
GM FEAT : SC-005-01
- FR-005-01 : Accès à l'écran du relevé — Sans session active, la demande du relevé redirige vers la page de connexion. {GM: aucun}
- FR-005-02 : Comptes proposés et sélection initiale — Onglet « BankApp Demo - Relevé de compte », section « Relevé de compte », sélecteur « Compte : » limité aux comptes du client ; le premier compte est sélectionné et ses opérations chargées ; sans compte, sélecteur sans option et aucune opération. {GM: aucun}
- FR-005-03 : Changement de compte — Au changement de compte, la liste des comptes (soldes à jour) puis les opérations du compte choisi sont rechargées, panneau d'informations et tableau rafraîchis sans recharger la page. {GM: aucun}
- FR-005-04 : Informations du compte sélectionné — Affiche « {type de compte} — N° {numéro de compte} — Solde actuel : {solde} », solde en euros (« € ») avec au moins 2 décimales. {GM: aucun}
- FR-005-05 : Chargement de toutes les opérations — Toutes les opérations du compte, sans limite, triées par date décroissante. {GM: aucun}
- FR-005-06 : Tableau des opérations — Sans opération : « Aucune transaction pour ce compte. » ; sinon compteur « {N} opération(s) » et colonnes « Date », « Description », « Type », « Montant », « Solde après » ; date dd/MM/yyyy HH:mm, « CREDIT » en vert (#2e7d32) / « DEBIT » en rouge (#E3000F), montant préfixé « + » ou « - » (A-005-1) en euros ≥ 2 décimales, solde après sans signe. {GM: aucun}
- FR-005-07 : Opérations en attente — Libellé commençant par « [EN ATTENTE] » + espace (13 caractères) : badge « En attente », 13 caractères retirés du libellé affiché, ligne sur fond jaune ; sans préfixe, ni badge ni fond jaune (textes legacy conservés, décision B1 révisée). {GM: aucun}
- FR-005-08 : Liens de navigation selon le rôle — Sur le relevé, « Espace Conseiller » seulement pour CONSEILLER ou ADMIN, « Administration » seulement pour ADMIN. {GM: aucun}

##### FEAT-006 — Budget et catégories de dépenses
GM FEAT : SC-006-01
- FR-006-01 : Données limitées à l'utilisateur connecté — Plafonds, ventilation, totaux et périodes portent exclusivement sur le compte client de l'utilisateur authentifié ; jamais les opérations ni plafonds d'un autre client. {GM: aucun}
- FR-006-02 : Choix de la période — Le sélecteur « Periode : » propose au format AAAA-MM les 24 mois les plus récents ayant au moins une opération ; à l'ouverture, le mois le plus récent avec opérations (sinon le mois civil en cours) ; tout changement recharge les données sans validation, le bouton « Afficher » recharge aussi. {GM: aucun}
- FR-006-03 : Ventilation des dépenses par catégorie — « Repartition par categorie » / « Depenses du mois {AAAA-MM} » ventile les seuls débits du mois, chaque opération dans la première catégorie dont un mot-clé apparaît dans le libellé (insensible à la casse), sinon « Autres » ; seules les catégories non nulles sont affichées ; sans dépense : « Aucune depense enregistree sur cette periode ». {GM: aucun}
- FR-006-04 : Totaux du mois — Affiche « Total depenses » (somme des débits du mois) et « Total revenus » (somme des crédits du mois) en EUR à deux décimales, et « Plafonds definis » (nombre de plafonds du mois). {GM: aucun}
- FR-006-05 : Contrôles du formulaire de plafond — Formulaire « Definir un plafond » : « Categorie » (option « -- Choisir -- »), « Plafond mensuel (EUR) » (« ex: 500 »), bouton « Enregistrer le plafond » ; sans catégorie : « Veuillez selectionner une categorie » ; plafond vide, nul ou négatif : « Veuillez saisir un plafond valide » ; saisie non numérique refusée. {GM: aucun}
- FR-006-06 : Catégories proposées pour un plafond, sans la catégorie Revenus — Le champ « Categorie » propose « -- Choisir -- » et Logement, Alimentation, Transport, Sante, Loisirs, Abonnements, Epargne, Assurances, Autres, sans « Revenus » ; un plafond pour Revenus est refusé. Remplace RG-006-028. [BUG-023 corrigé] {GM: aucun}
- FR-006-07 : Enregistrement d'un plafond — Au plus un plafond par (client, catégorie, mois) : un plafond existant est remplacé, sinon créé ; succès : « Plafond enregistre pour {categorie} », champs vidés, liste rafraîchie. {GM: aucun}
- FR-006-08 : Suppression d'un plafond — Chaque ligne du tableau des plafonds a un bouton « Supprimer » qui supprime immédiatement le plafond, sans confirmation. {GM: aucun}
- FR-006-09 : Barre de progression par catégorie — Pour une catégorie avec plafond : barre et « {pourcentage}% du plafond » (dépenses / plafond × 100, entier), verte < 80 %, orange de 80 % à 99 %, rouge ≥ 100 %, remplissage plafonné à 100 % ; sans plafond : « Aucun plafond defini ». {GM: aucun}
- FR-006-10 : Signalement d'un dépassement — Dépenses strictement supérieures au plafond : badge « Depassement », ligne sur fond rose, montant dépensé en rouge ; dépenses égales au plafond : barre rouge sans badge. {GM: aucun}
- FR-006-11 : Tableau des plafonds du mois — Le tableau « Plafonds du mois {AAAA-MM} » n'apparaît qu'avec au moins un plafond du mois, colonnes « Categorie », « Plafond », « Depense », « Ecart », « Action » ; écart = plafond − dépenses, rouge si négatif, vert si positif ou nul. {GM: aucun}

##### FEAT-007 — Demande et suivi de prêts
GM FEAT : SC-007-01, SC-007-02
- FR-007-01 : Accès à la page Prêts — Sans utilisateur authentifié, la page Prêts redirige vers la connexion ; tout rôle authentifié (CLIENT, CONSEILLER, ADMIN) y accède avec la même interface, présentant les sections « Simulateur de pret », « Demande de pret » et « Mes emprunts ». {GM: aucun}
- FR-007-02 : Formulaire du simulateur et contrôles — Le simulateur (« Simuler un emprunt ») a les champs « Montant (EUR) », « Taux annuel (%) », « Duree (mois) » pré-remplis à 10 000 EUR, 3,50 % et 36 mois, et le bouton « Calculer » ; un taux < 0 ou > 30 est refusé avec « Taux invalide (0-30%) », une durée ≤ 0 ou > 360 avec « Duree invalide (1-360 mois) » (montant : exigence fusionnée « Montant invalide » de FEAT-004). {GM: aucun}
- FR-007-03 : Calcul de la simulation — Avec des valeurs valides, le système affiche « Mensualite », « Cout total » et « Total interets » (taux nul : montant / mois ; sinon annuité constante) et un lien « Voir l echeancier complet ({N} echeances) » listant « # », « Capital », « Interets », « Total », « Capital restant » ; référence : 10 000 EUR à 3,5 % sur 36 mois donnent 293.02 EUR, 10,548.72 EUR et 548.72 EUR. {GM: aucun}
- FR-007-04 : Types de prêt proposés — Le champ « Type de pret » propose dans l'ordre « Immobilier », « Automobile », « Consommation », « Travaux », « Etudiant », avec « Consommation » sélectionné par défaut. {GM: aucun}
- FR-007-05 : Contrôles de la demande de prêt — Le formulaire « Nouveau pret » (« Type de pret », « Montant (EUR) », « Taux annuel (%) », « Duree (mois) », « Objet du pret », bouton « Soumettre la demande ») refuse un taux < 0 ou > 30 avec « Taux invalide », une durée ≤ 0 ou > 360 avec « Duree invalide », et un objet vide ou blanc avec « Veuillez indiquer un motif » (montant : exigence fusionnée de FEAT-004). {GM: aucun}
- FR-007-06 : Refus dès la demande d'une durée de 301 à 360 mois — Une demande de durée 301 à 360 mois inclus est refusée dès la soumission avec « Duree invalide » (A-007-1), sans prêt enregistré ni transmission au scoring ; 300 mois est accepté et transmis ; le simulateur garde 1 à 360 mois. [BUG-025 corrigé ; remplace RG-007-028] {GM: aucun}
- FR-007-07 : Enregistrement et transmission d'une demande valide — Une demande valide crée un prêt EN_ATTENTE (« En attente d approbation ») avec mensualité, coût total, intérêts, début au 1er du mois en cours et fin = début + durée en mois, transmis au scoring asynchrone (FEAT-024) ; la page affiche « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) » et rafraîchit la liste (échec de transmission : FR-024-02). {GM: aucun}
- FR-007-08 : Liste des emprunts — « Emprunts en cours » n'affiche que les prêts de l'utilisateur, triés par date de début décroissante, colonnes « Type », « Montant », « Taux », « Duree », « Mensualite », « Total interets », « Statut », « Echeancier », « Action », statuts « En attente d approbation », « En cours », « Rembourse », « Refuse par le service credit », « En defaut » ; EN_ATTENTE affiche « En cours de traitement... » ; sans prêt : « Aucun emprunt enregistre ». {GM: aucun}
- FR-007-09 : Règles de scoring crédit — Le scoring refuse si durée > 300 mois, ou si le montant dépasse 25 000 EUR (CONSOMMATION), 40 000 (AUTO), 80 000 (TRAVAUX), 20 000 (ETUDIANT), 600 000 (IMMOBILIER), et approuve sinon (détail FR-024-03 à FR-024-05). {GM: aucun}
- FR-007-10 : Approbation, refus et notification — Approbation : EN_ATTENTE → EN_COURS et échéancier de N échéances mensuelles ; refus : EN_ATTENTE → REFUSE ; notification « Votre demande de pret {type} de {montant} EUR a ete approuvee par le service credit. » (CONFIRMATION) ou « Votre demande de pret {type} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). » (ALERTE) ; une décision sur un prêt non EN_ATTENTE est ignorée sans erreur. {GM: aucun}
- FR-007-11 : Consultation de l'échéancier d'un prêt — Pour un prêt EN_COURS ou REMBOURSE, le bouton « Voir » ouvre « Echeancier du pret selectionne » (« Detail des echeances ») avec « # », « Date echeance » (jj/MM/aaaa), « Capital », « Interets », « Total », « Capital restant », « Statut » valant « Paye » ou « En attente » (A-007-2). {GM: aucun}
- FR-007-12 : Paiement d'une échéance — Une échéance non payée a un bouton « Marquer paye » qui la marque payée, enregistre la date et affiche « Echeance marquee comme payee » ; une échéance déjà payée n'est pas repayée (sans erreur) ; toutes payées → prêt « Rembourse ». {GM: aucun}
- FR-007-13 : Suppression d'un prêt — Le bouton « Supprimer » de chaque ligne supprime définitivement le prêt et ses échéances, sans confirmation. {GM: aucun}

##### FEAT-008 — Documents (relevés, attestations, IFU)
GM FEAT : SC-008-01
- FR-008-01 : Redirection vers la connexion des écrans réservés aux utilisateurs authentifiés — Un utilisateur non authentifié demandant la page documents, le chat conseiller, l'espace conseiller ou l'administration est redirigé vers la page de connexion. [exigence fusionnée RG-008-001, RG-010-001, RG-015-001, RG-016-001] {GM: aucun}
- FR-008-02 : Téléchargement sans session — Une demande de téléchargement sans session authentifiée redirige vers la connexion sans générer de document (cf. FR-017-01). {GM: aucun}
- FR-008-03 : Accès à la page documents pour tous les rôles — Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) accède à la page documents et télécharge ses propres documents. {GM: aucun}
- FR-008-04 : Refus d'un document portant sur le compte d'un autre client — Un relevé, RIB ou attestation demandé sur un compte existant d'un autre client est refusé sans document servi (A-008-1 : HTTP 403), un compte inexistant restant en 404 ; la même demande sur son propre compte renvoie le PDF. [BUG-027 corrigé ; remplace RG-008-004] {GM: aucun}
- FR-008-05 : Documents proposés par compte — Pour chaque compte, une section "Compte {type de compte} - {numéro de compte}" contient une carte RIB ("Bancaire", "RIB", "Releve d Identite Bancaire", "Telecharger PDF"), une carte attestation ("Attestation", "Attestation de solde", "Solde au jour de la demande", "Telecharger PDF") et "Releves mensuels — {numéro de compte}" avec un lien "{mois} {année}" pour les 12 mois les plus récents (mois "Janvier" … "Décembre"). {GM: aucun}
- FR-008-06 : Documents fiscaux (IFU N-1 et N-2) — Une section "Documents fiscaux" titrée "Imprime Fiscal Unique (IFU)" affiche les cartes "IFU {N-1}" et "IFU {N-2}" (N = année en cours), étiquette "Fiscal", sous-titre "Revenus de capitaux mobiliers", lien "Telecharger PDF". {GM: aucun}
- FR-008-07 : Relevé mensuel — Un relevé demandé pour un compte, une année et un mois sert un PDF contenant toutes les opérations de la période triées par date croissante (détail FR-017-04). {GM: aucun}
- FR-008-08 : RIB — Un RIB sert un PDF avec numéro de compte, IBAN, BIC/SWIFT, code banque, code guichet et nom du titulaire (détail FR-017-05). {GM: aucun}
- FR-008-09 : Attestation de solde — Une attestation sert un PDF certifiant le solde courant du seul compte demandé, daté du moment de la demande (détail FR-017-06). {GM: aucun}
- FR-008-10 : IFU de l'utilisateur connecté — L'IFU est généré pour le client de la session, jamais pour un client passé en paramètre ; le revenu d'intérêts est le total des crédits dont la description contient "interet", avec un PFU de 30 % (détail FR-017-07). {GM: aucun}
- FR-008-11 : Demandes de téléchargement invalides — Rejet en 400 "Parametre type manquant" si type absent, 400 "accountId manquant" si compte manquant pour RELEVE/RIB/ATTESTATION, 400 "Type inconnu : {type}" pour un type hors RELEVE, RIB, ATTESTATION, IFU, et 404 "Compte introuvable" si le compte n'existe pas. {GM: aucun}
- FR-008-12 : Compteur de téléchargements — Chaque téléchargement d'un document incrémente son compteur de téléchargements (A-008-2 : RG-008-018 implémentée, contrairement au legacy). {GM: aucun}

##### FEAT-009 — Bourse et watchlist
GM FEAT : SC-009-01
- FR-009-01 : Accès à la page bourse — Tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), accède à la page bourse sans autre restriction. {GM: aucun}
- FR-009-02 : Chargement initial — À l'ouverture, le système charge la watchlist, sélectionne sa première valeur (tri marché puis symbole) et récupère son historique avant d'afficher la page. {GM: aucun}
- FR-009-03 : Changement de valeur ou de période — Changer "Valeur" ou "Période" ("1 Semaine", "1 Mois", "1 An", "5 Ans") récupère et affiche l'historique sans recharger toute la page. {GM: aucun}
- FR-009-04 : Indicateurs de marché — Avec un historique, le système affiche "{nom} — {symbole}", le cours actuel (dernière clôture), "Variation sur la période" (dernière − première clôture, "+" si positive), "Performance %" ((dernière − première) / première × 100) et "Marché". {GM: aucun}
- FR-009-05 : Couleur selon la performance — Performance ≥ 0 : graphique et indicateurs en vert (#2e7d32) avec préfixe "▲ +" ; strictement négative : rouge (#E3000F) avec "▼ ". {GM: aucun}
- FR-009-06 : Graphique d'évolution du cours — Sous "📈 Évolution du cours", un graphique vectoriel serveur avec cinq dates espacées au format MM-JJ, cinq lignes de grille et un badge "1 semaine (données journalières)", "1 mois (données journalières)", "1 an (données hebdomadaires)" ou "5 ans (données hebdomadaires)". {GM: aucun}
- FR-009-07 : Dernières séances — Sous "📋 Dernières séances", les 10 derniers points du plus récent au plus ancien avec "Date", "Ouverture", "Haut", "Bas", "Clôture" (2 décimales), "Volume", "Var. J" ((clôture − ouverture) / ouverture × 100, "+" si clôture ≥ ouverture), ligne verte si clôture ≥ ouverture, rouge sinon. {GM: aucun}
- FR-009-08 : Données demandées au fournisseur selon la période — "1W" et "1M" : série journalière compacte (100 jours) filtrée à aujourd'hui − 1 semaine / − 1 mois ; "1Y" et "5Y" : série hebdomadaire filtrée à − 1 an / − 5 ans ; accès selon NFR-INT. {GM: aucun}
- FR-009-09 : Absence de données du fournisseur — Sur statut non succès, message d'information/limite/erreur ou série vide, le graphique et le tableau sont masqués et le message "⚠️ Aucune donnée reçue pour « {SYMBOLE} » sur {libellé de période}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur." est affiché. {GM: aucun}
- FR-009-10 : Watchlist unique et partagée — Une seule watchlist commune à tous les utilisateurs, triée par marché puis symbole croissants, affichée sous "⭐ Ma Watchlist ({N} valeurs)" avec "Symbole", "Nom", "Marché", "Devise", "Suppr.", et "({N} valeurs)" à côté du sélecteur. {GM: aucun}
- FR-009-11 : Formulaire d'ajout et contrôles — "+ Ajouter une valeur" ouvre "➕ Ajouter une valeur à la watchlist" ("Symbole *", "Nom *", "Marché" défaut "Euronext Paris"/"NASDAQ"/"NYSE"/"London SE"/"Autre", "Devise" défaut "EUR"/"USD"/"GBP", "Ajouter", "Annuler") ; la soumission est rejetée, formulaire ouvert, avec "Le symbole est obligatoire (ex: AMZN, SG.PA)", "Le nom de la valeur est obligatoire" ou "Le symbole « {SYMBOLE} » existe déjà dans votre watchlist" (doublon insensible à la casse). {GM: aucun}
- FR-009-12 : Enregistrement d'une nouvelle valeur — Le symbole est enregistré en majuscules sans espaces de bord (≤ 20), le nom (≤ 100), le marché (≤ 50), la devise en majuscules (≤ 10) ; le système affiche "Valeur ajoutée !", sélectionne la valeur, ferme le formulaire et récupère son historique (refus des longueurs : FR-029-13). {GM: aucun}
- FR-009-13 : Suppression d'une valeur — Le bouton "✕" (infobulle "Supprimer {nom} de la watchlist") supprime sans confirmation ; si la valeur était sélectionnée, la première restante est sélectionnée et chargée ; watchlist vide : sélection effacée, graphique et tableau masqués. {GM: aucun}
- FR-009-14 : Clé d'accès au fournisseur de cotations hors du code source — La clé AlphaVantage n'est lue que depuis une configuration sécurisée (variable d'environnement ou propriété système), sans valeur par défaut dans le code ni fichiers versionnés ; sans clé, aucun appel réel n'est émis. [BUG-034 corrigé ; remplace RG-009-021] {GM: aucun}

##### FEAT-010 — Chat avec le conseiller
GM FEAT : SC-010-01, SC-010-02
- FR-010-01 : Accès aux écrans ouverts à tout utilisateur authentifié — « Mon Conseiller », le catalogue des produits et les bénéficiaires sont accessibles à tout rôle authentifié (CLIENT, CONSEILLER, ADMIN) sans différence de contenu ; un non-authentifié est redirigé vers la connexion. [exigence fusionnée RG-010-002, RG-011-001, RG-013-002] {GM: aucun}
- FR-010-02 : Message de bienvenue personnalisé — À l'ouverture, en-tête « Sophie — Conseillère Demo Bank », indicateur « En ligne · Répond en quelques secondes » et message « Bonjour {prenom} ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ? » ({prenom} = premier mot du nom complet, ou « cher client » sans client associé). {GM: aucun}
- FR-010-03 : Conversation neuve à chaque ouverture — Chaque ouverture de l'écran réinitialise la conversation, qui ne contient que le message de bienvenue. {GM: aucun}
- FR-010-04 : Actions rapides — Tant que seule la bienvenue est présente, quatre boutons (« Optimiser mon epargne », « Projet immobilier », « Investir en bourse », « Preparer ma retraite ») envoient leur texte prédéfini (ex. « Je voudrais optimiser mon epargne, que me conseillez-vous ? ») puis sont masqués pour la conversation en cours seulement, « Nouvelle conversation » les réaffichant (A-010-3, écart à RG-010-007). {GM: aucun}
- FR-010-05 : Envoi d'un message — Zone de saisie « Posez votre question a Sophie... » et bouton « Envoyer » ; un message non vide est ajouté, le service d'IA interrogé et la réponse ajoutée comme message de Sophie ; un message vide ou blanc est ignoré sans erreur ni appel. {GM: aucun}
- FR-010-06 : Historique transmis au service d'IA — Le premier message part avec un historique vide ; les suivants avec tous les échanges précédents, hors message de bienvenue et message en cours. {GM: aucun}
- FR-010-07 : Consignes de comportement de Sophie — Chaque requête porte une consigne fixe (persona Sophie, six domaines d'expertise) imposant : pas de conseil d'investissement ferme et mention des risques (MiFID II), réponse exclusivement en français, 3 à 5 phrases sauf demande explicite ; limite de 500 tokens, température 0,7. {GM: aucun}
- FR-010-08 : Raccordement au service d'IA — Appel au service de complétion Azure OpenAI ; adresse, déploiement, version d'API et clé sont configurables (variable d'environnement, puis paramètre système, puis défaut), la clé sans défaut dans le code (NFR-SEC-06 ; A-010-4 : sans clé, aucun appel réel, comportement d'indisponibilité) ; délais de connexion 15 s et de lecture 30 s. {GM: aucun}
- FR-010-09 : Indisponibilité du service d'IA (correction BUG-038) — Les erreurs s'affichent dans une bulle de Sophie : refus d'authentification (401/403, A-010-1) → « Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. » sans code ; autre statut → « Je suis momentanement indisponible (HTTP {status}). Veuillez reessayer. » ; échec sans réponse → même message que l'authentification ; test : avec un 401 simulé, « Quel est mon solde courant ? » donne une bulle sans « 401 ». [BUG-038 corrigé ; remplace RG-010-020] {GM: aucun}
- FR-010-10 : Nouvelle conversation — « Nouvelle conversation » efface tous les messages, réinitialise avec un nouveau message de bienvenue personnalisé et réaffiche les actions rapides. {GM: aucun}
- FR-010-11 : Comportement de la zone de saisie — Pendant l'envoi, « Envoyer » est désactivé et affiche « ... » puis rétabli ; Entrée sans Maj envoie, Maj+Entrée insère un saut de ligne ; à la réponse, la liste défile jusqu'au dernier message. {GM: aucun}

##### FEAT-011 — Catalogue produits bancaires
GM FEAT : SC-011-01
- FR-011-01 : Catalogue en lecture seule — Le catalogue, titré "Catalogue des produits", est en lecture seule, sans aucun champ de saisie. {GM: aucun}
- FR-011-02 : Chargement et rafraîchissement — À l'ouverture de session, le catalogue et l'historique des traitements sont chargés ; le bouton "Rafraichir" les recharge en restant sur la même page. {GM: aucun}
- FR-011-03 : Produits affichés, tri et sections — Seuls les produits actifs, triés par nom, sont répartis en "Produits d epargne" ("Nom", "Description", "Taux", "Depot min", "Capital garanti", "Duree max"), "Produits de credit" ("Nom", "Description", "Taux", "Duree max", "LTV max", "Assurance requise") et "Produits d investissement" ("Nom", "Description", "Secteur", "Niveau de risque", "Rendement attendu") ; booléens "Oui"/"Non", durée "{n} mois" ; récupération possible par type. {GM: aucun}
- FR-011-04 : Valeurs absentes — Un champ facultatif vide (taux, dépôt min, LTV max, secteur, rendement, durée max crédit) affiche "—", sauf la durée max vide d'un produit d'épargne qui affiche "Illimite". {GM: aucun}
- FR-011-05 : Taux et rendements en pourcentage — Taux et rendement s'affichent en pourcentage à deux décimales avec virgule : 0,0300 → "3,00 %" (colonne "Taux"), 0,0650 → "6,50 %" (colonne "Rendement attendu"). [BUG-039 corrigé ; remplace RG-011-010] {GM: aucun}
- FR-011-06 : Niveau de risque d'un produit d'investissement — Affiché "{libellé} ({score}/5)" avec 1 "Tres faible", 2 "Faible", 3 "Moyen", 4 "Eleve", 5 "Tres eleve" ; score hors 1-5 → "N/A". {GM: aucun}
- FR-011-07 : Valeurs par défaut des produits — À la création, un produit d'épargne a la garantie du capital à vrai et un produit de crédit l'assurance requise à faux. {GM: aucun}
- FR-011-08 : Sections vides — Un type sans produit affiche "Aucun produit d epargne disponible", "Aucun produit de credit disponible" ou "Aucun produit d investissement disponible" (A-011-1) ; sans traitement enregistré : "Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00". {GM: aucun}
- FR-011-09 : Historique des traitements nocturnes — Sous "Historique des traitements nocturnes", au plus les 50 dernières exécutions, de la plus récente à la plus ancienne, colonnes "Job", "Debut", "Fin", "Duree (ms)", "Enregistrements", "Statut" ("SUCCESS", "FAILED", "PARTIAL"), "Details" ; durée = fin − début en ms, 0 si l'une manque ; séparateur "Traitements nocturnes" (A-011-2 : sans « (EJB @Schedule) »). {GM: aucun}

##### FEAT-012 — Comptes Tiers — Open Banking Powens
GM FEAT : SC-012-01
- FR-012-01 : Accès à l'écran Comptes Tiers — Un utilisateur non authentifié demandant « Comptes Tiers » est redirigé vers la connexion ; tous les rôles authentifiés (CLIENT, CONSEILLER, ADMIN) y accèdent avec le même contenu. {GM: aucun}
- FR-012-02 : Retour de Powens accessible sans authentification — Le point de retour de Powens est accessible sans session authentifiée (traitement : FR-014-01 et suivantes). {GM: aucun}
- FR-012-03 : État non connecté — Sans connexion Powens en session, l'écran affiche « Agregez vos comptes bancaires », le texte « Connectez vos comptes d autres banques pour avoir une vue complete de votre patrimoine financier. Connexion securisee via le protocole OAuth2 PSD2. », les banques « BNP Paribas », « Credit Agricole », « La Banque Postale », « CIC », « LCL », « + 1800 banques » et le bouton « Connecter une banque ». {GM: aucun}
- FR-012-04 : État connecté — Connecté, le bloc de connexion est masqué et l'écran affiche le badge « Connected via Powens », le bouton « Deconnecter », la carte « Patrimoine total (tous comptes) » avec « via Powens Open Banking API », la section « Mes comptes externes ({N}) » et la section des transactions. {GM: aucun}
- FR-012-05 : Lancement de la connexion à une banque — « Connecter une banque » crée un utilisateur anonyme Powens (jeton permanent puis code temporaire), conserve le jeton en session et redirige vers la page web Powens (domaine, client, retour, code, banques ; FR-028-01) ; en cas d'échec (pas de réponse, erreur, jeton absent), affiche « Impossible d initialiser la connexion Powens. » et reste non connecté sans redirection. {GM: aucun}
- FR-012-06 : Aiguillage du retour de Powens — Avec `connection_id`, les comptes sont chargés ; avec seulement un code, le retour est traité comme incomplet (A-014-1, écart à RG-012-011) ; sans aucun des deux ni erreur, le système affiche « Callback Powens incomplet. Parametres recus: [<liste des paramètres reçus>] » et reste non connecté (priorité : FR-014-02). {GM: aucun}
- FR-012-07 : Retour de Powens en erreur (correction BUG-040, BUG-044) — Un retour avec paramètre d'erreur déconnecte la session Powens, revient à l'état non connecté et affiche « La connexion a votre banque n a pas abouti ({motif}). Veuillez recommencer la connexion. » (A-012-1) ; test : avec `error=user_cancelled`, l'utilisateur voit « Agregez vos comptes bancaires » et un message contenant « user_cancelled ». [BUG-040 et BUG-044 corrigés ; remplace RG-012-009] {GM: aucun}
- FR-012-08 : Session Powens expirée — Si la session ne contient plus le jeton Powens au retour, le système affiche « Session expiree. Veuillez recommencer la connexion. » et reste non connecté. {GM: aucun}
- FR-012-09 : Résultat du chargement des comptes après le retour — Au moins un compte : état connecté, premier compte sélectionné et ses transactions chargées ; aucun compte : état connecté et « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. » ; erreur : FR-012-14 ; aucune attente imposée avant la lecture (A-012-6). {GM: aucun}
- FR-012-10 : Patrimoine total — Le patrimoine total est la somme des soldes connus des comptes externes (solde absent exclu), affichée en euros avec séparateur de milliers et deux décimales. {GM: aucun}
- FR-012-11 : Carte d'un compte externe — Chaque carte affiche « Banque externe », le nom, le type traduit (checking « Compte courant », savings « Epargne », card « Carte », loan « Credit », market « Bourse », absent « Compte », autre tel quel), le solde (2 décimales, séparateur de milliers) et l'IBAN seulement s'il est non vide. {GM: aucun}
- FR-012-12 : Solde absent (correction BUG-042) — Un solde absent ou vide s'affiche « Solde indisponible » (A-012-2) au lieu de « 0,00 EUR », un solde nul reste « 0,00 », et le patrimoine total ne somme que les soldes renvoyés. [BUG-042 corrigé ; remplace RG-012-018 et RG-028-020] {GM: aucun}
- FR-012-13 : Transactions d'un compte externe — Le clic sur un compte met à jour la seule section « Transactions — {nom du compte} » (« Date », « Libelle », « Montant ») avec au plus 20 transactions, libellé simplifié sinon d'origine, montant > 0 en vert préfixé « + », ≤ 0 en rouge sans « + » ; sans transaction : « Aucune transaction disponible. ». {GM: aucun}
- FR-012-14 : Service Powens indisponible (correction BUG-043) — Sur une erreur Powens (statut ≥ 400) aux comptes ou transactions, le système affiche « Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants. » (A-012-3) au lieu d'une liste vide ; test : 503 sur les comptes n'affiche ni « Connexion reussie mais aucun compte disponible encore » ni « Mes comptes externes (0) » (A-012-4, reste non connecté), 500 sur les transactions n'affiche pas « Aucune transaction disponible. ». [BUG-043 corrigé ; remplace RG-012-027] {GM: aucun}
- FR-012-15 : Déconnexion de Powens — « Deconnecter » efface le jeton, les comptes, le compte sélectionné, les transactions et le message d'erreur, et repasse à l'état non connecté. {GM: aucun}
- FR-012-16 : Rafraîchissement des comptes externes — À l'état connecté, l'utilisateur rafraîchit ses comptes externes sans se reconnecter, via un bouton « Rafraichir » à côté de « Deconnecter » (A-012-5), selon FR-028-08. {GM: aucun}
- FR-012-17 : Paramètres de raccordement à Powens — Domaine, identifiant client, secret, adresse de retour et banques sont configurables, avec pour défauts l'environnement de test (domaine demi-sandbox.biapi.pro, client 99961298, banques 59) ; chaque appel applique un délai de 15 secondes en connexion et en lecture (reste : NFR-INT). {GM: aucun}

##### FEAT-013 — Gestion des bénéficiaires externes
GM FEAT : SC-013-01, SC-013-02
- FR-013-01 : Accès à l'écran des bénéficiaires — Un utilisateur non authentifié demandant l'écran des bénéficiaires est refusé et redirigé vers la page de connexion. {GM: aucun}
- FR-013-02 : Liste des bénéficiaires actifs du client — L'écran (onglet « BankApp Demo - Beneficiaires ») affiche « Mes beneficiaires enregistres » titrée « Beneficiaires ({nombre}) », colonnes « Libelle », « Titulaire », « IBAN », « BIC », « Banque », « Zone SEPA », « Action » et un bouton « Supprimer », avec les seuls bénéficiaires actifs du client triés par libellé croissant ; sinon « Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un. ». {GM: aucun}
- FR-013-03 : Affichage masqué de l'IBAN, BIC, banque et zone SEPA — L'IBAN s'affiche 4 premiers caractères + « *** » + 4 derniers (ex. « FR35***9010 ») ; BIC ou banque absents → « — » ; « Zone SEPA » affiche « ✓ SEPA » ou « Hors SEPA » selon le code pays. {GM: aucun}
- FR-013-04 : Vérification de l'IBAN avant l'ajout — Dans « Nouveau beneficiaire SEPA », le bouton « Verifier l'IBAN » vérifie le seul champ « IBAN * » sans soumettre ni recharger et affiche « Veuillez saisir un IBAN. », « IBAN invalide (echec MOD-97). Verifiez la saisie. », « IBAN valide — {code pays} (Zone SEPA) » ou « IBAN valide — {code pays} (Hors zone SEPA) ». {GM: aucun}
- FR-013-05 : Contrôles à l'ajout d'un bénéficiaire — Le formulaire (« Libelle * », « Nom du titulaire * », « IBAN * », « BIC (optionnel) », « Banque (optionnel) », bouton « Ajouter le beneficiaire ») rejette l'ajout avec « Session expirée, veuillez vous reconnecter. », « Le libellé est obligatoire. », « L'IBAN est obligatoire. », « IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97). », « Le nom du titulaire est obligatoire. » ou « BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques. » (4 lettres banque, 2 lettres pays, 2 alphanumériques, 3 facultatifs). {GM: aucun}
- FR-013-06 : Longueur maximale des champs texte — Un libellé, un nom de titulaire ou un nom de banque de plus de 100 caractères est rejeté sans enregistrement, et la saisie de ces champs est limitée à 100 caractères. {GM: aucun}
- FR-013-07 : Normalisation et enregistrement du bénéficiaire — À l'enregistrement, l'IBAN est sans espaces et en majuscules, le BIC rogné et en majuscules (absent si blanc), la banque rognée (absente si blanche), la zone SEPA calculée depuis le code pays, la date du jour enregistrée en création et le bénéficiaire actif. {GM: aucun}
- FR-013-08 : Ajout réussi — Après ajout, le système affiche « Bénéficiaire ajouté avec succès. », recharge la liste et vide le formulaire. {GM: aucun}
- FR-013-09 : Suppression logique d'un bénéficiaire — « Supprimer » désactive le bénéficiaire sans l'effacer et rafraîchit la liste sans recharger la page, le bénéficiaire disparaissant de la liste ; possible seulement pour un bénéficiaire du client connecté. {GM: aucun}
- FR-013-10 : Contrôle d'appartenance avant suppression — La suppression d'un bénéficiaire d'un autre client est rejetée avec « Erreur lors de la suppression. » (A-013-1) et le bénéficiaire reste actif et visible chez son propriétaire. [BUG-047 corrigé ; remplace RG-013-035] {GM: aucun}

##### FEAT-014 — Callback OAuth Powens
GM FEAT : SC-014-01
- FR-014-01 : Accès sans authentification — Le point de retour Powens est accessible sans authentification et répond toujours par une redirection vers « Comptes Tiers » sans contenu propre ; sans paramètre, il répond en 302. {GM: aucun}
- FR-014-02 : Aiguillage par ordre de priorité — Seul le premier cas applicable est traité (paramètre vide = absent) : 1. `error` → remise à zéro de la session, motif affiché (FR-012-07), redirection ; 2. `connection_id` → chargement des comptes ; 3. `code` → FR-014-04 ; 4. sinon message « Callback Powens incomplet. Parametres recus: [<liste>] » (ex. « … Parametres recus: [] ») et état non connecté. [BUG-040 corrigé] {GM: aucun}
- FR-014-03 : Redirection systématique — Après traitement, succès ou erreur, l'utilisateur est toujours redirigé vers « Comptes Tiers », qui affiche l'état et les messages. {GM: aucun}
- FR-014-04 : Retour portant seulement un code — La branche « code » n'est pas reprise tant que Powens ne l'a pas confirmée : un retour ne portant qu'un `code` est traité comme incomplet avec « Callback Powens incomplet. Parametres recus: [code] » (A-014-1). {GM: aucun}
- FR-014-05 : Session Powens expirée — Si le chargement des comptes est lancé sans jeton Powens en session, il est abandonné et « Comptes Tiers » affiche « Session expiree. Veuillez recommencer la connexion. ». {GM: aucun}
- FR-014-06 : Connexion établie — Avec un jeton valide, au moins un compte : connexion établie, premier compte sélectionné, transactions chargées ; aucun compte : connexion établie et « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. » ; une erreur Powens suit FR-012-14 ; aucune attente imposée (A-014-2). {GM: aucun}
- FR-014-07 : Remise à zéro de la session Powens — La déconnexion Powens efface jeton, comptes, compte sélectionné, transactions, indicateur de connexion et message d'erreur (A-014-3) ; dans le cas `error`, le message du motif est posé après cette remise à zéro. {GM: aucun}
- FR-014-08 : Vingt transactions au plus par compte externe — Au plus 20 transactions par compte externe sont récupérées auprès de Powens (même valeur RG-014-011 et RG-028-018, cf. FR-012-13). {GM: aucun}

##### FEAT-015 — Espace Conseiller — Portefeuille clients
GM FEAT : SC-015-01, SC-015-02
- FR-015-01 : Accès à l'espace conseiller selon le rôle — CONSEILLER ou ADMIN voit la page « Espace Conseiller » ; un CLIENT est redirigé vers son tableau de bord sans message d'erreur. {GM: aucun}
- FR-015-02 : Chargement du portefeuille du conseiller connecté — À la première ouverture dans la session, seuls les clients affectés à l'utilisateur connecté sont chargés, triés par nom complet, avec le bandeau « Espace Conseiller » et « Bonjour {fullName}, vous gerez {N} client(s) ». {GM: aucun}
- FR-015-03 : Indicateurs du portefeuille — Trois indicateurs : « Mes clients » (« clients dans mon portefeuille »), « Encours total » (somme des soldes de tous les comptes, « solde cumulé mes clients ») et « Prets en attente » (nombre de prêts EN_ATTENTE, « a traiter »). {GM: aucun}
- FR-015-04 : Portefeuille vide — Sans client, le système affiche « Aucun client dans votre portefeuille. » suivi de « Contactez l administrateur pour l affectation des clients. » à la place de la liste. {GM: aucun}
- FR-015-05 : Carte client — Une carte par client avec avatar (première lettre), nom complet, « @ » + identifiant, badge « Actif »/« Inactif », « Solde total », « Comptes », « Prets actifs » (EN_COURS), « Prets en attente » (EN_ATTENTE), distinction visuelle des inactifs et bouton « Voir detail ». {GM: aucun}
- FR-015-06 : Indicateur de prêts sur la carte client — Avec au moins un prêt EN_ATTENTE, la carte affiche « ⚠ {N} pret(s) en attente » ; sinon, avec au moins un EN_COURS, « ✓ {N} pret(s) en cours » ; jamais les deux ensemble. {GM: aucun}
- FR-015-07 : Panneau de détail d'un client — « Voir detail » ouvre sur la même page un panneau avec nom complet, « @ » + identifiant, email, téléphone, « Comptes bancaires » (« Numero », « Type », « Solde ») et « Prets » (« Type », « Montant », « Mensualite », « Statut ») ; « Fermer » le masque sans changer d'écran. {GM: aucun}
- FR-015-08 : Client sans compte ou sans prêt — Dans le panneau, un client sans compte affiche « Aucun compte » et sans prêt « Aucun pret ». {GM: aucun}
- FR-015-09 : Valeurs de référence affichées — Types de compte COURANT, EPARGNE ; types de prêt IMMOBILIER, CONSOMMATION, TRAVAUX ; statuts EN_ATTENTE, EN_COURS, REMBOURSE, DEFAUT, REFUSE affichés tels quels (codes), en badge sauf DEFAUT sans badge. [BUG-053 conservé (voir Notes)] {GM: aucun}

##### FEAT-016 — Administration — Gestion globale
GM FEAT : SC-016-01, SC-016-02
- FR-016-01 : Accès à l'administration réservé au rôle ADMIN — Un CONSEILLER ou CLIENT demandant l'administration est redirigé vers son tableau de bord sans affichage de la page ; un ADMIN accède à toutes les sections. {GM: aucun}
- FR-016-02 : Indicateurs de l'administration — La page affiche « Total clients » (sous-libellé « {n} actifs »), « Conseillers » (« actifs »), « Prets en attente » (« {montant} EUR a traiter », cumul des prêts EN_ATTENTE) et « Total utilisateurs » (« tous roles confondus »). {GM: aucun}
- FR-016-03 : Pipeline des prêts vide — « Pipeline Prets — En attente d approbation » liste les prêts EN_ATTENTE (« ID », « Client », « Type », « Montant », « Duree », « Taux », « Mensualite », « Motif », « Actions ») et affiche « Aucun pret en attente » s'il n'y en a aucun. {GM: aucun}
- FR-016-04 : Approbation d'un prêt — « Approuver » sur un prêt EN_ATTENTE le passe EN_COURS, génère l'échéancier complet et affiche « Prêt approuvé » ; sur un prêt non EN_ATTENTE, la demande est ignorée sans erreur. {GM: aucun}
- FR-016-05 : Refus d'un prêt — « Refuser » sur un prêt EN_ATTENTE le passe REFUSE immédiatement, sans confirmation, et affiche « Prêt refusé » ; sur un prêt non EN_ATTENTE, la demande est ignorée sans erreur. {GM: aucun}
- FR-016-06 : Journal d'audit des actions administratives — Chaque action est auditée avec son identifiant : « ADMIN_LOAN_APPROVE », « ADMIN_LOAN_REFUSE » (prêt), « ADMIN_DEACTIVATE », « ADMIN_ACTIVATE » (client), ainsi que libération/rejet de virement suspendu (FR-026-02) ; l'entrée d'audit est conservée même si l'action échoue et est annulée. {GM: aucun}
- FR-016-07 : Erreur lors de l'approbation ou du refus d'un prêt — Une erreur à l'approbation ou au refus affiche « Erreur lors du traitement du prêt. Veuillez réessayer. » (A-016-1) sans détail technique, et le prêt reste EN_ATTENTE. [BUG-054 corrigé ; remplace RG-016-014] {GM: aucun}
- FR-016-08 : Liste et recherche des clients — « Gestion des clients » n'affiche que les CLIENT (« Nom », « Login », « Email », « Role », « Statut » Actif/Inactif, « Conseiller », « Actions ») ; recherche vide = tous ; « Filtrer » garde ceux dont nom, identifiant ou email contient le terme (partiel, insensible à la casse) ; vide : « Aucun client trouve ». {GM: aucun}
- FR-016-09 : Panneau de détail d'un client — « Detail » affiche sur la même page « Detail : {nom complet} » en lecture seule avec « Nom complet », « Identifiant », « Email », « Telephone », « Role », « Statut », « Conseiller assigne » (si affecté) et « Client depuis » (jj/MM/aaaa) ; « Fermer » le ferme. {GM: aucun}
- FR-016-10 : Date « Client depuis » — « Client depuis » affiche la date d'entrée en relation (création de la fiche, A-016-2), pas celle d'affectation du conseiller, pour tout client ; test : client créé le 01/02/2024 et affecté le 15/06/2025 → 01/02/2024. [BUG-055 corrigé ; remplace RG-016-025] {GM: aucun}
- FR-016-11 : Activation et désactivation d'un client — « Desactiver » rend un client actif inactif et affiche « Client désactivé » ; « Activer » rend un client inactif actif et affiche « Client activé » ; chaque action est auditée (FR-016-06). {GM: aucun}
- FR-016-12 : Historique de tous les prêts — « Historique de tous les prets », en lecture seule, affiche tous les prêts triés par date de début décroissante (« ID », « Client », « Type », « Montant », « Statut », « Duree », « Mensualite », « Date debut ») ou « Aucun pret en base ». {GM: aucun}
- FR-016-13 : Tous les utilisateurs du système — « Tous les utilisateurs du systeme », en lecture seule, affiche tous les utilisateurs de tous les rôles avec « Nom », « Login », « Email », « Role », « Statut ». {GM: aucun}
- FR-016-14 : Tableau des alertes fraude et actions selon le statut — « Alertes Fraude — Moteur de détection » liste les alertes (« ID », « Client », « Règle déclenchée », « Score risque » « {score}/100 », « Statut », « Description », « Date », « Actions ») ; OPEN : boutons « Confirmer » et « Faux positif », sinon « Traité par {reviewedBy} » ; badge « {n} ouverte(s) » seulement si OPEN > 0 ; aucune alerte : « Aucune alerte fraude enregistrée ». {GM: aucun}
- FR-016-15 : Revue d'une alerte fraude — « Confirmer » passe une alerte OPEN à CONFIRMED, « Faux positif » à FALSE_POSITIVE, en enregistrant l'identifiant de l'admin et l'horodatage, « Traité par » affichant son nom complet (A-016-4) ; une demande sans identifiant d'alerte est ignorée ; la revue n'a aucun effet sur le virement suspendu (seule FR-016-19 le fait, passant les alertes OPEN à REVIEWED, décision B4). {GM: aucun}
- FR-016-16 : Affichage de toutes les alertes fraude — Sans filtre, le tableau affiche toutes les alertes (OPEN, CONFIRMED, FALSE_POSITIVE, REVIEWED) ; test : une OPEN, une CONFIRMED, une FALSE_POSITIVE → trois lignes, deux « Traité par {reviewedBy} », badge « 1 ouverte(s) ». [BUG-056 corrigé ; remplace RG-016-034] {GM: aucun}
- FR-016-17 : Journal des traitements périodiques — « Traitements nocturnes », en lecture seule, affiche les 50 entrées les plus récentes (« Job », « Debut », « Fin », « Duree (ms) », « Enregistrements », « Statut », « Details ») ou « Aucun traitement nocturne enregistre ». {GM: aucun}
- FR-016-18 : Catalogue des produits bancaires — « Catalogue produits bancaires », en lecture seule, regroupe les produits actifs en « Produits d epargne », « Produits de credit », « Produits d investissement », avec le lien « Voir la page Produits » ; une catégorie vide affiche « Aucun produit ». {GM: aucun}
- FR-016-19 : Revue des virements suspendus — Seul l'ADMIN dispose d'une file des virements suspendus (client, montant, destinataire, motif, heure butoir ; A-016-3) qu'il libère ou rejette avec motif obligatoire audité (FR-026-02) ; libération strictement avant l'heure butoir (à l'heure butoir : REJETÉ, « Heure butoir dépassée : le virement est rejeté. », A-016-6), solde contrôlé (insuffisant → REJETÉ sans débit, sinon LIBÉRÉ puis EXÉCUTÉ par le traitement asynchrone FR-023-01) ; solde devenu insuffisant avant le débit → REJETÉ avec « solde insuffisant au moment du débit » dans l'historique (A-016-7) ; les alertes OPEN passent à REVIEWED dans la même action (A-016-5) ; CONSEILLER/CLIENT refusés. [ÉVOLUTION — décision RG-027-003 D6, D7] [ÉVOLUTION — décision de finalisation B4, B5, B6, B7] [BUG-076 corrigé] {GM: aucun}

##### FEAT-017 — Téléchargement documents PDF
GM FEAT : SC-017-01, SC-017-02
- FR-017-01 : Session obligatoire pour télécharger — Une demande de téléchargement sans session authentifiée valide est redirigée vers la page de connexion, ce contrôle étant fait par la fonction de téléchargement elle-même, indépendamment du contrôle d'accès général des écrans. {GM: aucun}
- FR-017-02 : Type de document — Type absent : code 400 et message "Parametre type manquant" ; type hors RELEVE, RIB, ATTESTATION, IFU (comparaison insensible à la casse) : code 400 et message "Type inconnu : {type}". {GM: aucun}
- FR-017-03 : Identifiant de compte — Pour RELEVE, RIB ou ATTESTATION, un identifiant de compte absent, vide ou non numérique est rejeté avec un code 400 et le message "accountId manquant". {GM: aucun}
- FR-017-04 : PDF du relevé de compte — Pour RELEVE, PDF de toutes les opérations du compte pour l'année et le mois demandés, triées par date croissante, renvoyé en pièce jointe `releve_{accountId}_{année}_{mois sur 2 chiffres}.pdf`, titré "Demo Bank — Releve de compte", avec "Compte", "Titulaire", "Periode", "Solde actuel" ("{montant} EUR") et un tableau "Date", "Description", "Type", "Montant", "Solde apres" (type "CREDIT"/"DEBIT", montants "+{montant} EUR"/"-{montant} EUR") ; sans opération, "Aucune operation sur cette periode.". {GM: aucun}
- FR-017-05 : PDF du RIB — Pour RIB, PDF titré "Demo Bank — Releve d Identite Bancaire (RIB)" avec "Titulaire", "Banque" : "Demo Bank SA", "Code banque" : "30004", "Code guichet" : "00001", "Numero de compte", "Cle RIB" : "42", "IBAN", "BIC / SWIFT" : "DEMOFR21XXX", "Domiciliation" : "Demo Bank - Agence Paris Centre" et la note "Document genere le {jj/mm/aaaa} - Demo Bank SA, 1 Place Vendome, 75001 Paris", renvoyé en pièce jointe `rib_{accountId}.pdf`. {GM: aucun}
- FR-017-06 : PDF de l'attestation de solde — Pour ATTESTATION, PDF titré "Demo Bank — Attestation de Solde" attestant le solde courant à la date de génération, avec les textes exacts "Demo Bank SA, dont le siege social est situe 1 Place Vendome, 75001 Paris, atteste par la presente que :", "M. / Mme {nom complet}", "est titulaire du compte n° {numéro de compte}", "Le solde de ce compte, arrete au {jj mois aaaa}, est de :", "{montant} EUR", "Cette attestation est delivree a la demande de l interesse(e) pour faire valoir ce que de droit." et "Fait a Paris, le {jj mois aaaa}", renvoyé en `attestation_solde_{accountId}.pdf`. {GM: aucun}
- FR-017-07 : PDF de l'IFU — Pour IFU, PDF du client de la session (jamais d'un client désigné par la demande) pour l'année demandée, totalisant les crédits dont la description contient "interet" avec PFU de 30 % arrondi au demi supérieur à 2 décimales ; titré "Demo Bank — Imprime Fiscal Unique (IFU) {année}", avec "Etablissement" : "Demo Bank SA", "SIREN" : "123 456 789 00012", "Titulaire", "Adresse" : "1 rue de la Paix, 75001 Paris", "Annee fiscale", tableau "Nature des revenus"/"Montant brut" (lignes "Interets et produits de placement", "Prelevement Forfaitaire Unique (30%)"), la note "Ce document est fourni a titre informatif. Il recense les revenus de capitaux mobiliers soumis au PFU pour l annee {année}." et "Genere le {jj/mm/aaaa}", renvoyé en `ifu_{customerId}_{année}.pdf`. {GM: aucun}
- FR-017-08 : Refus d'un compte appartenant à un autre client — Une demande de document sur un compte n'appartenant pas au client de la session est refusée sans génération (HTTP 403 selon A-017-1), après les contrôles FR-017-02/03 ; test : client A sur compte du client B → aucun PDF, refus ; sur son propre compte → PDF. [BUG-058 corrigé (Source), remplace RG-017-011] {GM: aucun}
- FR-017-09 : Compte ou client introuvable — Compte ou client inexistant : code 404 et message "Compte introuvable" (compte) ou "Client introuvable" (client, type IFU) ; écart attendu au golden master (legacy en 500, A-017-2). {GM: aucun}
- FR-017-10 : Échec de génération sans détail interne — Une erreur inattendue de génération renvoie un code 500 dont le corps vaut exactement "Erreur lors de la generation du document", sans message d'exception, trace ni nom de composant. [BUG-059 corrigé (Source), remplace RG-017-013] {GM: aucun}
- FR-017-11 : Format de la réponse PDF — Tout document généré est renvoyé avec `application/pdf`, `attachment; filename="{nom de fichier}"` et `no-cache, no-store, must-revalidate`, et tous les PDF portent le pied de page "Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012 — Établissement de crédit agréé par l'ACPR". {GM: aucun}

##### FEAT-018 — Calcul des intérêts nocturnes
- FR-018-01 : Déclenchement mensuel du crédit d'intérêts — Le traitement « INTEREST_CALCULATION » se déclenche à 2 h et applique le taux mensuel 0,5 % / 12 au solde de chaque compte EPARGNE une seule fois par mois civil (crédit le 1er du mois selon A-018-1, pas de double crédit en cas de relance selon A-018-2) ; test : compte EPARGNE de 12 000,00 EUR, après un mois complet d'exécutions, exactement une opération « Interets mensuels livret epargne (batch nocturne) » de 5,00 EUR et solde 12 005,00 EUR. [BUG-064 corrigé (Source), remplace RG-018-003] {GM: aucun}
- FR-018-02 : Comptes concernés et calcul de l'intérêt — Tous les comptes EPARGNE sont sélectionnés, l'intérêt est arrondi à 2 décimales (moitié vers le haut) et un intérêt nul ou négatif fait ignorer le compte (solde inchangé, aucune opération). {GM: aucun}
- FR-018-03 : Crédit du compte et opération d'intérêts — Lors d'un crédit d'intérêts, le solde augmente du montant et une opération au crédit est enregistrée avec ce montant, le solde résultant, la date et l'heure, et le libellé « Interets mensuels livret epargne (batch nocturne) ». {GM: aucun}
- FR-018-04 : Journal d'exécution du traitement — En succès, entrée « INTEREST_CALCULATION », statut « SUCCESS », heures de début et fin, nombre de comptes EPARGNE évalués (ignorés compris) et détail « Interets calcules sur {N} comptes epargne » ; en erreur, statut « FAILED », détail « Erreur: {message} » et log ERROR ; les nuits sans intérêt dû, entrée SUCCESS à 0 (A-018-4). {GM: aucun}
- FR-018-05 : Consultation des exécutions par l'ADMIN — L'ADMIN voit dans la section des traitements nocturnes les exécutions récentes triées par début décroissant, colonnes « Job », « Debut », « Fin », « Duree (ms) », « Enregistrements », « Statut », « Details », un bouton « Rafraichir », et « Aucun traitement nocturne enregistre » si aucune exécution. {GM: aucun}

##### FEAT-019 — Détection fraude nocturne par batch
- FR-019-01 : Exécution quotidienne à 02:30, sans rattrapage — Le traitement s'exécute chaque jour à 02:30 dans le fuseau configuré (NFR-BATCH-01, NFR-BATCH-03), n'est pas relancé si le serveur redémarre après 02:30, et s'exécute une seule fois par jour calendaire y compris aux changements d'heure (A-019-1). {GM: aucun}
- FR-019-02 : Transactions retenues — Sont retenues toutes les transactions d'un montant supérieur ou égal à 10 000 € (borne incluse) datées des 24 heures précédant l'exécution, débits et crédits confondus. {GM: aucun}
- FR-019-03 : Fenêtre de 24 heures exactes — La fenêtre est calculée depuis une référence à décalage fixe et dure toujours exactement 24 heures, y compris aux changements d'heure ; test : une transaction de 10 000 € à 24 h 30 avant l'exécution n'est pas comptée, une à 23 h 30 l'est, les jours de passage heure d'été et d'hiver. [BUG-066 corrigé (Source), remplace RG-019-009] {GM: aucun}
- FR-019-04 : Une entrée de journal par exécution — Chaque exécution écrit exactement une entrée de journal « FRAUD_DETECTION » avec le nombre de transactions retenues et le statut « SUCCESS » ou « FAILED » ; en succès, détail « Transactions suspectes detectees: {N} transaction(s) > 10000 EUR » (texte conservé malgré la borne inclusive, BUG-065 conservé selon les notes). {GM: aucun}
- FR-019-05 : Aucune alerte de fraude créée — Le traitement nocturne ne crée aucune alerte de fraude individuelle et ne produit que le compte rendu de FR-019-04 (alertes réservées au contrôle temps réel FEAT-027). {GM: aucun}
- FR-019-06 : Erreur pendant le traitement — Une erreur est interceptée, l'entrée de journal est écrite avec « FAILED » et le détail « Erreur: {message} », l'erreur est journalisée et non propagée, sans affecter les autres traitements ni l'exécution du lendemain. {GM: aucun}

##### FEAT-020 — Relevés mensuels — batch stub
- FR-020-01 : Déclenchement mensuel — Le 1er de chaque mois à 03h00, le système déclenche le traitement des relevés mensuels. {GM: aucun}
- FR-020-02 : Journal d'une exécution réussie — En succès, exécution enregistrée avec le nom "MONTHLY_STATEMENTS", le nombre de clients actifs de rôle CLIENT, le statut "SUCCESS" et le détail "Releves mensuels generes pour {N} clients" (comportement conservé, BUG-067 conservé selon les notes). {GM: aucun}
- FR-020-03 : Journal d'une exécution en échec — En erreur, exécution enregistrée avec "MONTHLY_STATEMENTS", zéro enregistrement, le statut "FAILED" et le détail "Erreur: {message de l'erreur}". {GM: aucun}
- FR-020-04 : Pas de rattrapage des exécutions manquées — Les exécutions manquées pendant l'arrêt de l'application ne sont pas rejouées. {GM: aucun}

##### FEAT-021 — Exécution des virements programmés
- FR-021-01 : Déclenchement périodique — Le traitement se déclenche toutes les 5 minutes à toute heure, journalise « Verification virements programmes dus » à chaque passage et, après un redémarrage, exécute les virements échus pendant l'arrêt au prochain cycle (au plus 5 minutes de retard après le démarrage). {GM: aucun}
- FR-021-02 : Sélection des virements dus — À chaque passage, sont sélectionnés tous les virements programmés actifs dont la prochaine exécution est antérieure ou égale à l'instant courant, triés par date croissante et traités un à un ; les inactifs ne sont pas sélectionnés (distinct de l'état SUSPENDU, FR-027-16). {GM: aucun}
- FR-021-03 : Débit du compte source et crédit du compte bénéficiaire — Si le solde est suffisant et le contrôle de fraude laisse passer, le compte source est débité et le compte interne bénéficiaire crédité du même montant (sauf bénéficiaire hors banque FR-021-04 ou solde insuffisant FR-004-12, sans débit), avec le log « Virement programme execute id={id} montant={montant} ». {GM: aucun}
- FR-021-04 : Compte bénéficiaire hors de la banque refusé avant tout débit — Un bénéficiaire absent de la banque est rejeté avant tout débit (aucune opération), contrôle placé avant le solde (A-021-2), le client reçoit une notification (texte proposé « Virement programme vers {libellé bénéficiaire} refuse : compte destinataire introuvable », A-021-1) et le virement programmé passe à l'état inactif (A-021-3) ; test : solde source inchangé, aucune opération, notification de refus, virement inactif. [BUG-068 corrigé (Source), remplace RG-021-007] {GM: aucun}
- FR-021-05 : Opération DEBIT sur le compte source — Une opération DEBIT est enregistrée sur le compte source avec le libellé « Virement programme vers {libellé bénéficiaire} », ou « Virement programme vers {libellé bénéficiaire} - {motif} » si un motif est saisi. {GM: aucun}
- FR-021-06 : Opération CREDIT sur le compte bénéficiaire interne — Une opération CREDIT est enregistrée sur le compte bénéficiaire interne avec le libellé « Virement programme de {numéro du compte source} », ou « Virement programme de {numéro du compte source} - {motif} » si un motif est saisi. {GM: aucun}
- FR-021-07 : Suivi des exécutions — Après une exécution réussie, l'horodatage de dernière exécution est enregistré et le nombre d'exécutions (colonne « Nb executions », FR-004-03) augmente de 1. {GM: aucun}
- FR-021-08 : Calcul de la prochaine échéance sans dérive — La prochaine exécution est calculée depuis l'échéance prévue (pas l'heure réelle) : +7 jours « Hebdomadaire », +1 mois « Mensuel », +2 mois « Bimestriel », +3 mois « Trimestriel », +6 mois « Semestriel », +1 an « Annuel » (une seule exécution par passage en cas de retard, A-021-4) ; test : mensuel prévu le 15/01 08:00 exécuté à 08:04 → prochaine le 15/02 08:00, et toujours le 15 à 08:00 après douze exécutions. [BUG-069 corrigé (Source), remplace RG-021-011] {GM: aucun}
- FR-021-09 : Échec d'un virement isolé des autres — Un virement en échec voit ses effets entièrement annulés (ni débit, ni crédit, ni opération, A-021-5), le log « Echec execution virement programme id={id} » est écrit et les autres virements du passage sont exécutés ; test : sur trois virements dus dont le deuxième échoue, le premier et le troisième sont exécutés, le deuxième reste inchangé avec un message d'échec. [BUG-070 corrigé (Source), remplace RG-021-013] {GM: aucun}
- FR-021-10 : Contrôle de fraude à l'exécution de chaque échéance — Chaque échéance passe, à l'exécution et après les contrôles bénéficiaire et solde, par le même contrôle de fraude que le virement immédiat (FR-027-02) : exécutée (FR-021-03 à 08), suspendue (score cumulé > 75, montant ≥ seuil de fraude ou dépassement du cumul FR-027-18 → état SUSPENDU sans débit, avis FR-027-04, prochaine échéance avancée selon A-021-6) ou refusée pour erreur (rien débité, retentée au passage suivant, A-021-7) ; test : échéance de 15 000 € → SUSPENDU, solde inchangé, notification FRAUDE avec heure butoir, échéance avancée d'une période ; libérée → exécutée ; non traitée → REJETÉE à l'heure butoir ; échéance de 100 € à score faible → exécutée sans notification. [ÉVOLUTION — décision de finalisation B2] {GM: aucun}

##### FEAT-022 — Surveillance soldes faibles et génération d'alertes
- FR-022-01 : Analyse périodique des comptes des clients actifs — Toutes les 30 minutes, le système analyse tous les comptes (tous types) des seuls clients dont l'indicateur actif est vrai. {GM: aucun}
- FR-022-02 : Détection d'un solde faible — Un compte de solde strictement inférieur à 100 € génère une notification de solde faible pour son titulaire, sauf plafond FR-022-04 atteint ; un compte sans solde est ignoré. {GM: aucun}
- FR-022-03 : Contenu de la notification de solde faible — Notification de type ACCOUNT_BALANCE, priorité HIGH, titre « Solde insuffisant sur votre compte » et texte « Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte. » (seuil 100). {GM: aucun}
- FR-022-04 : Plafond de 20 notifications de solde faible par compte et par jour — Au plus 20 notifications de solde faible par compte et par jour calendaire (heure de Paris, A-022-1), plafond prioritaire sur FR-022-02 et commun avec FR-025-08 (décision B3) ; test : solde à 50 € sur 48 exécutions d'une journée → exactement 20 notifications ACCOUNT_BALANCE, et une nouvelle à la première exécution du lendemain. [BUG-071 corrigé (Source), remplace RG-022-010] {GM: aucun}
- FR-022-05 : Tolérance aux erreurs de l'analyse — Si la liste des clients actifs ne se charge pas, toute l'exécution est interrompue et l'erreur journalisée ; si les comptes d'un client ne se chargent pas, ce client est ignoré avec un avertissement ; si la création d'une notification échoue, ce compte est ignoré avec un avertissement, et l'analyse continue. {GM: aucun}
- FR-022-06 : Trace de chaque exécution dans le journal des traitements — Chaque exécution (y compris interrompue, en échec avec le détail de l'erreur) est enregistrée dans le journal des traitements consultable par l'ADMIN (FEAT-016, « Traitements nocturnes »), avec début, fin, durée, nombre de notifications et statut (A-022-2) ; test : une nouvelle entrée après chaque exécution, une entrée en échec après une exécution interrompue faute de charger les clients. [BUG-072 corrigé (Source), remplace RG-022-011] {GM: aucun}

##### FEAT-023 — Traitement asynchrone des virements
- FR-023-01 : Remise du virement au traitement asynchrone — Un virement exécuté ou libéré (LIBÉRÉ, FR-016-19) est remis au traitement asynchrone avec identifiant, montant, émetteur, compte destinataire et motif, sans notification ; un virement suspendu, rejeté ou refusé pour erreur n'est pas remis ; un virement libéré est débité/crédité et passe EXÉCUTÉ (B6), ou passe REJETÉ sans débit ni notification si le solde est devenu insuffisant (B7). [ÉVOLUTION — décision RG-027-003 D1, D5] {GM: aucun}
- FR-023-02 : Échec de la remise : la validation du virement échoue et le client est informé — La remise est enregistrée dans la même écriture que la validation ; si elle ne peut être enregistrée, la validation échoue (aucun virement, débit, crédit ni opération) et le client voit un message (proposé « Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard », A-023-1) ; pour une libération, le virement reste SUSPENDU (A-023-3) ; test : virement valide de 100 € avec remise impossible → message d'échec, pas de redirection vers le tableau de bord, soldes inchangés, aucune opération ni virement. [BUG-073 corrigé (Source), remplace RG-023-002] {GM: aucun}
- FR-023-03 : Notification selon l'issue du contrôle et le montant — Une seule notification par virement, produite à la décision du contrôle de fraude : suspendu → FRAUDE (avis FR-027-04) ; exécuté < seuil d'alerte (5 000 € par défaut) → CONFIRMATION « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme » ; exécuté ≥ seuil d'alerte → ALERTE « Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. » ; un virement libéré n'en produit aucune ; test : notification ajoutée dès la décision, avant la remise, et aucune à la libération. [ÉVOLUTION — décision RG-027-003 D1, D2] {GM: aucun}
- FR-023-04 : Virement au-delà du seuil de fraude — Un virement ≥ seuil de fraude (10 000 € par défaut) est suspendu avant toute modification de solde, quel que soit son score, avec une seule notification FRAUDE (avis FR-027-04) ; test : 10 000,00 € de score nul → SUSPENDU, solde inchangé, une notification FRAUDE avec heure butoir ; 9 999,99 € de score ≤ 75 → exécuté. [ÉVOLUTION — décision RG-027-003 D2] [BUG-076 corrigé (Source), remplace RG-023-009] {GM: aucun}
- FR-023-05 : Plafond de notifications par utilisateur — Au plus N notifications par utilisateur (configuration de déploiement, 20 par défaut), la nouvelle en tête de liste et la plus ancienne supprimée à la limite (même règle que FR-025-04, FR-002-09). {GM: aucun}
- FR-023-06 : Conservation des notifications — Les notifications sont conservées après un redémarrage du serveur, selon FR-002-09. [BUG-006 corrigé (Source), remplace RG-023-011] {GM: aucun}

##### FEAT-024 — Traitement asynchrone approbation prêts
- FR-024-01 : Transmission de la demande au service de scoring crédit — Une demande de prêt valide est enregistrée au statut EN_ATTENTE (identifiant attribué et lisible) puis transmise au service de scoring crédit pour traitement asynchrone, avant de rendre la main à l'utilisateur. {GM: aucun}
- FR-024-02 : Échec de la transmission de la demande — Si la transmission échoue, la demande est refusée avec un message d'erreur sur la page Prêts (proposé « Service credit temporairement indisponible, veuillez reessayer plus tard », A-024-1) et le prêt est annulé, non laissé EN_ATTENTE (A-024-2) ; test : service indisponible → message d'indisponibilité, pas de « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) », aucun prêt EN_ATTENTE. [BUG-077 corrigé (Source), remplace RG-024-020] {GM: aucun}
- FR-024-03 : Refus pour durée excessive — Une durée supérieure à 300 mois entraîne le refus du prêt quels que soient type et montant, contrôle évalué avant les plafonds par type. {GM: aucun}
- FR-024-04 : Plafond de montant par type de prêt — Pour une durée ≤ 300 mois, le prêt est approuvé si son montant est inférieur ou égal au plafond du type (CONSOMMATION 25 000 EUR, AUTO 40 000 EUR, TRAVAUX 80 000 EUR, ETUDIANT 20 000 EUR, IMMOBILIER 600 000 EUR), refusé au-delà ; un montant égal au plafond est approuvé. {GM: aucun}
- FR-024-05 : Refus d'un type de prêt non reconnu — Un type absent ou hors IMMOBILIER, AUTO, CONSOMMATION, TRAVAUX, ETUDIANT entraîne le refus quel que soit le montant ; test : 10 000 EUR sur 60 mois sans type → statut REFUSE et notification ALERTE de refus. [BUG-078 corrigé (Source), remplace RG-024-021] {GM: aucun}
- FR-024-06 : Effets de l'approbation — À l'approbation, le prêt passe EN_COURS, l'échéancier complet est généré (une échéance par mois) et l'emprunteur reçoit une CONFIRMATION « Votre demande de pret {libelle} de {montant} EUR a ete approuvee par le service credit. » ({libelle} : « Immobilier », « Automobile », « Consommation », « Travaux » ou « Etudiant »). {GM: aucun}
- FR-024-07 : Effets du refus — Au refus, le prêt passe REFUSE et l'emprunteur reçoit une ALERTE « Votre demande de pret {libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). ». {GM: aucun}
- FR-024-08 : Demande reçue pour un prêt déjà traité — Une demande d'approbation pour un prêt dont le statut n'est pas EN_ATTENTE est ignorée, sans changement d'état ni notification. {GM: aucun}
- FR-024-09 : Nombre maximal de notifications par utilisateur — Au plus 20 notifications par utilisateur (défaut, configurable), l'ajout au-delà supprimant la plus ancienne. {GM: aucun}

##### FEAT-025 — Système de notifications internes
- FR-025-01 : Notification de virement selon l'issue du contrôle et le montant — À la décision du contrôle de fraude, l'émetteur reçoit une seule notification par virement : FRAUDE si suspendu (avis FR-027-04), CONFIRMATION « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme » si exécuté et montant < seuil d'alerte (5 000 € par défaut), ALERTE « Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. » si exécuté et montant ≥ seuil d'alerte (seuil de fraude 10 000 € par défaut). Un virement exécuté après libération puis remis au traitement asynchrone ne produit aucune nouvelle notification. [ÉVOLUTION — décision RG-027-003 D1, D2] {GM: aucun}
- FR-025-02 : Motif dans la notification de confirmation — Pour une notification CONFIRMATION avec motif renseigné (ni absent, ni vide, ni uniquement des espaces), le message devient « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme (motif : {motif}) ». {GM: aucun}
- FR-025-03 : Notifications de décision sur une demande de prêt — À l'approbation d'une demande de prêt, le demandeur reçoit une CONFIRMATION « Votre demande de pret {loanType.libelle} de {montant} EUR a ete approuvee par le service credit. » ; au refus, une ALERTE « Votre demande de pret {loanType.libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). ». {GM: aucun}
- FR-025-04 : Plafond de notifications par utilisateur — Quand un ajout ferait dépasser la limite configurée (20 par défaut), la notification la plus ancienne est supprimée ; le plafond s'applique aussi à l'avis de suspension (A-025-3). {GM: aucun}
- FR-025-05 : Affichage sur le tableau de bord — La section « Centre de notifications » affiche les notifications de la plus récente à la plus ancienne, avec type (« CONFIRMATION » ✅, « ALERTE » ⚠️, « FRAUDE » 🚨), message, horodatage dd/MM/yyyy HH:mm:ss et l'indication « Non lu » pour une non lue ; ACCOUNT_BALANCE s'affiche selon FR-025-08. {GM: aucun}
- FR-025-06 : Tout marquer comme lu — Avec au moins une notification non lue, le badge « {N} nouvelle(s) » et le bouton « Tout marquer lu » sont affichés ; le clic marque toutes les notifications lues et le badge disparaît. {GM: aucun}
- FR-025-07 : Tout effacer — Avec au moins une notification, le bouton « Effacer tout » est affiché ; le clic supprime toutes les notifications de l'utilisateur. {GM: aucun}
- FR-025-08 : Alerte de solde faible affichée au client — Toutes les 30 minutes, pour tout compte d'un client actif au solde strictement < 100 €, une notification ACCOUNT_BALANCE priorité HIGH « Solde insuffisant sur votre compte » / « Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte. » est affichée dans le centre de notifications (icône ⚠️), limitée à 20 par compte et par jour. Test : 99,99 € → notifiée, 100,00 € → non ; 50 € pendant 48 passages → exactement 20 notifications. (Corrige BUG-079, BUG-071.) [ÉVOLUTION — décision de finalisation B3] {GM: aucun}

##### FEAT-026 — Audit trail
- FR-026-01 : Audit des connexions, échecs de connexion et déconnexions — Une entrée d'audit est enregistrée : connexion réussie (« LOGIN », « Connexion réussie », IP, horodatage serveur), échec (identifiant saisi, « LOGIN_FAILED », « Tentative échouée », IP, horodatage), déconnexion (« LOGOUT », « Déconnexion », horodatage, IP exigée par FR-026-05). {GM: aucun}
- FR-026-02 : Audit des actions d'administration — Avec l'identifiant de l'administrateur : « ADMIN_DEACTIVATE » (« Désactivation client id={customerId} »), « ADMIN_ACTIVATE » (« Activation client id={customerId} »), « ADMIN_LOAN_APPROVE » (« Approbation prêt id={loanId} »), « ADMIN_LOAN_REFUSE » (« Refus prêt id={loanId} »), « ADMIN_TRANSFER_RELEASE » (« Liberation virement id={transferId} - Motif: {motif} - Issue: {EXECUTE|REJETE} », REJETE si solde insuffisant à la libération), « ADMIN_TRANSFER_REJECT » (« Rejet virement id={transferId} - Motif: {motif} »). {GM: aucun}
- FR-026-03 : Audit des virements — Entrées : « VIREMENT » à la soumission (« Montant: {amount} - Motif: {motif} - Issue: {EXECUTE|SUSPENDU|REFUSE_ERREUR_CONTROLE} »), « TRANSFER_EXECUTED » (« Montant: {amount} - Motif: {motif} »), « TRANSFER_AUTO_REJECT » par « SYSTEM » (« Rejet automatique virement id={transferId} »), « TRANSFER_REJECT_INSUFFICIENT_FUNDS » par « SYSTEM » (« Rejet virement id={transferId} - Motif: solde insuffisant au debit »). Test : pour les six parcours (exécuté = 2 entrées ; suspendu+libéré = 3 ; suspendu+libéré+rejet au débit = 3 ; suspendu+rejeté admin ou auto = 2 ; refusé erreur contrôle = 1), le journal contient exactement les entrées attendues dans l'ordre. [ÉVOLUTION — décision RG-027-003 D5, D8] [ÉVOLUTION — décision de finalisation B7] {GM: aucun}
- FR-026-04 : Entrée d'audit indépendante de l'opération — Chaque entrée d'audit est enregistrée dans une transaction indépendante et est conservée même si l'opération est annulée. {GM: aucun}
- FR-026-05 : Adresse IP d'origine pour toute action auditée — Toute entrée déclenchée par une requête utilisateur (onze codes : LOGIN, LOGIN_FAILED, LOGOUT, ADMIN_DEACTIVATE, ADMIN_ACTIVATE, ADMIN_LOAN_APPROVE, ADMIN_LOAN_REFUSE, ADMIN_TRANSFER_RELEASE, ADMIN_TRANSFER_REJECT, VIREMENT, TRANSFER_EXECUTED) porte l'IP d'origine (FR-001-13) ; les entrées de traitements planifiés/asynchrones n'en ont pas. (Corrige BUG-083.) {GM: aucun}
- FR-026-06 : Journal en écriture seule — Aucun écran ni fonction ne permet de consulter, filtrer ou afficher le journal d'audit. {GM: aucun}

##### FEAT-027 — Détection de fraude en temps réel
- FR-027-01 : Évaluation des huit signaux pour chaque virement — Pour chaque virement soumis, les huit signaux LARGE_AMOUNT, HIGH_FREQUENCY, NEW_BENEFICIARY, UNUSUAL_HOURS, ROUND_AMOUNT, FOREIGN_IBAN, RAPID_SUCCESSION et VELOCITY_CHECK sont évalués ; MULTIPLE_FAILED_AUTH est évalué à la connexion, hors virement. {GM: aucun}
- FR-027-02 : Contrôle avant la validation finale et issue du virement — Le contrôle s'exécute de façon synchrone avant la validation finale de tout virement (immédiat, ou à chaque échéance programmée) et rend : exécuté (score ≤ 75, montant < seuil de fraude 10 000 €, cumul glissant dans le plafond), suspendu sans débit (score > 75 OU montant ≥ seuil de fraude OU dépassement du cumul glissant), ou refusé pour erreur du contrôle (rien débité, erreur journalisée, écran FR-003-27). Test : score 75 et 9 999,99 € → exécuté ; score 76 → suspendu ; 10 000,00 € score nul → suspendu ; chaque erreur simulée → refusé, aucun solde changé, erreur journalisée. (Corrige BUG-084.) [ÉVOLUTION — décision RG-027-003 D1 à D4, D10] [ÉVOLUTION — décision de finalisation B2] {GM: aucun}
- FR-027-03 : Enregistrement des alertes au-delà du score 75 — Si la somme des scores levés est strictement > 75, une alerte par signal levé est enregistrée au statut OPEN avec score et description ; à 75 ou moins, aucune alerte. {GM: aucun}
- FR-027-04 : Avis au client d'un virement suspendu — Le client est informé par l'unique notification FRAUDE et à l'écran : vérification en cours, rien débité, réponse avant une heure butoir = soumission + 24 h, rejet sans débit à défaut de décision ; le texte n'évoque ni fraude ni signaux (texte A-027-4 : « Votre virement de {montant} EUR vers le compte {numCompte} est en cours de vérification. Rien n'a été débité. Vous aurez notre réponse avant le {date} à {heure}. Sans décision à cette heure, il sera rejeté sans aucun débit. »). Test : suspendu à 10 h 15 le 5 → réponse avant 10 h 15 le 6 ; pas de « fraude » ni nom de signal. [ÉVOLUTION — décision RG-027-003 D2, D6, D7] {GM: aucun}
- FR-027-05 : Signal LARGE_AMOUNT de score 40 — Signal de score 40 si 5 000 € ≤ montant < 15 000 €, description « Transaction amount {amount} exceeds the large threshold of {threshold} ». {GM: aucun}
- FR-027-06 : Signal HIGH_FREQUENCY sur les transactions du jour — Signal de score 50 si le nombre de transactions du client du jour courant dépasse 8 (débits et crédits confondus), description « Customer has {count} transactions today, exceeding the daily limit of {maxDailyTransactions} ». Test : 100 anciennes + 8 du jour → pas de signal ; 9 du jour → signal 50. (Corrige BUG-085.) {GM: aucun}
- FR-027-07 : Signal NEW_BENEFICIARY — Signal de score 35 si l'IBAN destinataire n'est pas un bénéficiaire actif du client, description « Target IBAN {iban} is not registered as a trusted beneficiary ». {GM: aucun}
- FR-027-08 : Signal UNUSUAL_HOURS — Signal de score 45 si l'heure serveur (fuseau configuré de l'application) est avant 06:00 ou après 23:00, description « Transaction initiated at hour {hourOfDay} which is outside normal operating hours (06:00-23:00) ». {GM: aucun}
- FR-027-09 : Signal FOREIGN_IBAN — Signal de score 30 si l'IBAN destinataire ne commence pas par « FR » (insensible à la casse), description « Target IBAN {iban} does not start with FR (possible foreign transfer) ». {GM: aucun}
- FR-027-10 : Signal VELOCITY_CHECK — Signal de score 65 si au moins 5 virements exécutés de plus de 500 € dans les 60 dernières minutes, description « Customer executed {highValueCount} transactions above 500 EUR in the last {velocityWindowMinutes} minutes (limit: {velocityMaxCount}) ». {GM: aucun}
- FR-027-11 : Signal MULTIPLE_FAILED_AUTH après échecs de connexion — Signal de score 80 au 5e échec de connexion consécutif sur un compte client (succès = remise à zéro, échecs d'un compte désactivé comptés), produisant une alerte OPEN « Customer account has {count} consecutive failed authentication attempts », sans s'ajouter au score d'un virement. {GM: aucun}
- FR-027-12 : Consultation des alertes par l'ADMIN — Le panneau ADMIN « Alertes Fraude — Moteur de détection » liste toutes les alertes, quel que soit leur statut, par date de création décroissante, avec le badge « {N} ouverte(s) » s'il existe des alertes OPEN. (Corrige BUG-056.) {GM: aucun}
- FR-027-13 : Confirmation d'une alerte — « Confirmer » sur une alerte OPEN la passe en CONFIRMED, enregistre l'ADMIN réviseur et l'heure serveur ; la ligne affiche « Traité par {reviewedBy} » (nom complet) ; aucun effet sur le virement. {GM: aucun}
- FR-027-14 : Classement en faux positif — « Faux positif » sur une alerte OPEN la passe en FALSE_POSITIVE, enregistre l'ADMIN réviseur et l'heure serveur ; la ligne affiche « Traité par {reviewedBy} » ; le virement n'est pas libéré. {GM: aucun}
- FR-027-15 : IBAN masqué dans les journaux — L'IBAN destinataire est masqué dans tous les journaux serveur du contrôle de fraude (ex. « FR76****1234 »). Test : avec FR7630006000011234567890189, aucune ligne ne contient l'IBAN en clair, seuls les 4 premiers et 4 derniers caractères apparaissent. (Corrige BUG-086.) {GM: aucun}
- FR-027-16 : États d'un virement — Cinq états (EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE) ; transitions permises : soumission → EXÉCUTÉ/SUSPENDU/REFUSÉ_ERREUR_CONTRÔLE, SUSPENDU → LIBÉRÉ/REJETÉ (REJETÉ direct si solde insuffisant à la libération), LIBÉRÉ → EXÉCUTÉ ou REJETÉ avec motif « solde insuffisant au débit » ; états terminaux, autres transitions refusées. Test : aucun solde ne change hors passage à EXÉCUTÉ ; rejet au débit sans débit/crédit ni notification, avec audit TRANSFER_REJECT_INSUFFICIENT_FUNDS. [ÉVOLUTION — décision RG-027-003 D5] [ÉVOLUTION — décision de finalisation B6] [ÉVOLUTION — décision de finalisation B7] {GM: aucun}
- FR-027-17 : Échéance de revue et rejet automatique — Un virement suspendu non traité 24 h calendaires après soumission est rejeté automatiquement sans débit (audité) ; une libération n'est acceptée que strictement avant l'heure butoir, l'horloge tranchant la concurrence (une seule transition, pas de double débit). Test : SUSPENDU à 23 h 59 min 59 s, REJETÉ à 24 h et 24 h + 1 s ; libération à butoir − 1 s acceptée, à butoir refusée. [ÉVOLUTION — décision RG-027-003 D7, D8] [ÉVOLUTION — décision de finalisation B5] {GM: aucun}
- FR-027-18 : Cumul glissant des virements d'un client — Le total des virements émis (exécutés et suspendus) sur les 24 h glissantes, virement courant inclus, strictement supérieur au plafond (10 000 € par défaut) suspend le virement qui le franchit. Test : avec 6 000 € déjà émis, 4 000 € (total 10 000,00 €) non suspendu, 4 000,01 € suspendu ; un virement de plus de 24 h n'est pas compté. [ÉVOLUTION — décision RG-027-003 D10] [STORY SUIVANTE : livrée par la story qui suit immédiatement le socle RG-027-003] {GM: aucun}

##### FEAT-028 — Intégration Powens (Open Banking — agrégation)
- FR-028-01 : Initiation de la connexion Powens — Le système crée un utilisateur Powens anonyme (identifiant et secret client), conserve le jeton permanent en session avant toute redirection, obtient un code temporaire avec ce jeton et construit l'adresse de la page web de connexion (domaine, identifiant client, adresse de retour encodée, code encodé, banques proposées) ; les autres appels s'authentifient avec le jeton permanent. {GM: aucun}
- FR-028-02 : Échec de l'initiation — Si la création de l'utilisateur Powens échoue ou ne renvoie pas de jeton, le système affiche « Impossible d initialiser la connexion Powens. » et abandonne sans redirection. {GM: aucun}
- FR-028-03 : Traitement du retour de Powens — Le point de retour est public ; un paramètre d'erreur remet à zéro l'état Powens de session et informe du motif (corrige BUG-040), un `connection_id` lance le chargement des comptes, sans `connection_id` ni `code` le message « Callback Powens incomplet. Parametres recus: [<liste>] » s'affiche ; redirection ensuite vers « Comptes Tiers ». {GM: aucun}
- FR-028-04 : Retour portant seulement un code — Un retour qui ne porte qu'un `code` ne lance pas le chargement des comptes et affiche « Callback Powens incomplet. Parametres recus: [code] ». {GM: aucun}
- FR-028-05 : Jeton absent au retour — Si le jeton Powens est absent de la session au retour, le système affiche « Session expiree. Veuillez recommencer la connexion. » et abandonne le chargement. {GM: aucun}
- FR-028-06 : Résultat du chargement des comptes — Une lecture réussie marque la connexion établie ; avec au moins un compte, le premier est sélectionné et ses transactions chargées ; sans compte, le message « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. » s'affiche sans échec ; une lecture en erreur suit FR-028-11. {GM: aucun}
- FR-028-07 : Transactions d'un compte sélectionné — À la sélection d'un compte externe, au plus 20 transactions sont chargées ; le libellé est le libellé simplifié, ou le libellé d'origine s'il est absent ou vide. {GM: aucun}
- FR-028-08 : Rafraîchissement des comptes — Le rafraîchissement relit la liste des comptes ; si aucun compte n'était sélectionné et qu'au moins un est renvoyé, le premier est sélectionné et ses transactions chargées. {GM: aucun}
- FR-028-09 : Solde absent et patrimoine total — Un solde non renvoyé est représenté « absent » (distinct de zéro) et affiché indisponible ; le patrimoine total additionne les seuls soldes renvoyés. Test : un compte sans solde + un compte à 120,50 → premier marqué « solde absent », patrimoine 120,50. [correction BUG-042] {GM: aucun}
- FR-028-10 : Déconnexion — À la déconnexion de Powens, tout l'état Powens de session est effacé : jeton, comptes, compte sélectionné, transactions, indicateur de connexion, message d'erreur. {GM: aucun}
- FR-028-11 : Réponse en erreur de Powens — Une réponse 4xx/5xx à la lecture des comptes ou transactions produit une indication d'erreur distincte d'une liste vide. Test : 500 sur les comptes puis 404 sur les transactions → erreurs d'indisponibilité, ni « Mes comptes externes (0) » ni « Aucune transaction disponible. ». [correction BUG-089] {GM: aucun}
- FR-028-12 : Configuration et délais — Domaine, identifiant client, secret client, adresse de retour et banques proposées sont des paramètres de déploiement avec valeurs par défaut ; chaque appel Powens applique un délai de 15 secondes pour la connexion et la lecture. {GM: aucun}

##### FEAT-029 — Intégration AlphaVantage (cotations boursières)
- FR-029-01 : Série demandée selon la période — Série hebdomadaire pour "1Y" et "5Y", journalière compacte (100 jours) pour "1W" et "1M" ; pour toute autre période, date limite par défaut d'un mois. {GM: aucun}
- FR-029-02 : Filtrage et tri des points — Les points strictement antérieurs à la date limite sont exclus et les points sont triés par date croissante. {GM: aucun}
- FR-029-03 : Réponses du fournisseur en échec — Champ information/note/erreur, statut non succès ou erreur d'appel : journalisation et traitement comme absence de données sans propagation, avec le message "⚠️ Aucune donnée reçue pour « {symbole} » sur {libellé de période}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur." (libellés "1 semaine (données journalières)", "1 mois (données journalières)", "1 an (données hebdomadaires)", "5 ans (données hebdomadaires)"). {GM: aucun}
- FR-029-04 : Adresse du fournisseur configurable — Au démarrage, l'adresse est lue dans ALPHA_VANTAGE_BASE_URL (variable d'environnement puis propriété système), défaut "https://www.alphavantage.co/query". {GM: aucun}
- FR-029-05 : Clé d'accès protégée — La clé est lue dans ALPHA_VANTAGE_API_KEY (variable d'environnement puis propriété système), sans repli sur une valeur codée en dur ; sans clé, aucun appel réel n'est émis. Test : aucune clé dans le code ni les fichiers versionnés ; pas d'appel avec clé par défaut. (Corrige BUG-091.) {GM: aucun}
- FR-029-06 : Période par défaut et première valeur — Au premier chargement, la période "1M" ("1 Mois") est présélectionnée et, si la watchlist n'est pas vide, les données de sa première valeur sont chargées. {GM: aucun}
- FR-029-07 : Calculs et indicateur de variation — Cours actuel = dernière clôture ; variation = dernière − première clôture ; pourcentage = (variation / première clôture) × 100 ; affichage positif (vert, "▲") si pourcentage ≥ 0, négatif (rouge, "▼") sinon. {GM: aucun}
- FR-029-08 : Dernières séances et graphique — Le tableau affiche au plus les 10 points les plus récents, du plus récent au plus ancien ; le graphique, généré côté serveur, fait 800 × 300 avec 5 lignes horizontales de grille et 5 dates. {GM: aucun}
- FR-029-09 : Contrôles à l'ajout d'une valeur — Rejet avec "Le symbole est obligatoire (ex: AMZN, SG.PA)" (symbole vide/absent), "Le nom de la valeur est obligatoire" (nom vide/absent), "Le symbole « {SYMBOLE} » existe déjà dans votre watchlist" (doublon insensible à la casse). {GM: aucun}
- FR-029-10 : Normalisation et sélection après ajout — Symbole enregistré en majuscules sans espaces de bord, nom et marché sans espaces de bord ; après "Valeur ajoutée !", la nouvelle valeur est sélectionnée et ses données chargées. {GM: aucun}
- FR-029-11 : Ordre de la watchlist — La watchlist est triée par marché puis par symbole. {GM: aucun}
- FR-029-12 : Suppression d'une valeur sélectionnée — Après suppression de la valeur sélectionnée, la première valeur restante est sélectionnée et chargée ; si c'était la dernière, sélection, historique et graphique sont effacés. {GM: aucun}
- FR-029-13 : Longueur du symbole et du nom — Symbole > 20 caractères et nom > 100 caractères refusés via la limitation de saisie des champs "Symbole *" et "Nom *", sans message dédié. {GM: aucun}
- FR-029-14 : Marchés et devises autorisés — Marchés acceptés : "Euronext Paris", "NASDAQ", "NYSE", "LSE" (affiché "London SE"), "Autre" ; devises : "EUR", "USD", "GBP". {GM: aucun}

##### FEAT-030 — Contrôle d'accès RBAC
- FR-030-01 : Accès public limité et redirection des accès non authentifiés — Seuls la page de connexion, la page de retour open banking et les ressources statiques sont servies sans session (exemption sur chemin exact) ; toute autre requête d'écran non authentifiée est redirigée vers la connexion. {GM: aucun}
- FR-030-02 : Espace conseiller réservé aux rôles CONSEILLER et ADMIN — Un utilisateur authentifié ni CONSEILLER ni ADMIN demandant l'espace conseiller est redirigé vers le tableau de bord, sans message. {GM: aucun}
- FR-030-03 : Écran d'accueil selon le rôle après connexion — Après authentification, ADMIN → administration, CONSEILLER → espace conseiller, CLIENT → tableau de bord. {GM: aucun}
- FR-030-04 : Liens de navigation conditionnés par le rôle — Le lien « Espace Conseiller » n'est affiché que pour CONSEILLER ou ADMIN, le lien « Administration » que pour ADMIN, sur tous les écrans authentifiés. {GM: aucun}
- FR-030-05 : Page expirée — Une action soumise depuis une page dont l'état de session a expiré redirige vers la page de connexion. {GM: aucun}
- FR-030-06 : Téléchargement de documents réservé aux utilisateurs authentifiés — Une demande de téléchargement sans utilisateur authentifié est redirigée vers la connexion ; tout utilisateur authentifié, quel que soit son rôle, y a accès. {GM: aucun}
- FR-030-07 : Contrôle des rôles dans les services métier — Les services métier appliquent eux-mêmes le contrôle par rôle. Test : appel direct à une fonction d'administration (ex. désactivation d'un client) par CLIENT ou CONSEILLER, hors contrôle d'entrée, refusé sans modification ; réussi pour ADMIN. (Corrige BUG-093.) {GM: aucun}
- FR-030-08 : Nouvelle session après authentification — Après authentification réussie, la session existante est invalidée et une nouvelle créée avant d'y enregistrer le contexte. Test : identifiants de session avant/après différents ; l'ancien ne donne accès à aucun écran protégé. (Corrige BUG-092.) {GM: aucun}

### NonFunctional Requirements

36 exigences numérotées du PRD §7, plus 5 exigences propres à une fonctionnalité.

**Identifiants créés ici :** NFR-F006, NFR-F007, NFR-F010, NFR-F012 et NFR-F028 n'existent pas dans le PRD. Ils ont été créés par ce découpage pour nommer les paragraphes « Feature-specific NFRs » des fonctionnalités, qui n'ont pas d'identifiant dans le PRD (`prds/prd-next-banking-webapp-2026-10-05/prd.md`) :
- NFR-F006 : FEAT-006 Budget, ligne 779 ;
- NFR-F007 : FEAT-007 Prêts, ligne 871 ;
- NFR-F010 : FEAT-010 Chat avec le conseiller, ligne 1157 ;
- NFR-F012 : FEAT-012 Comptes Tiers Powens, ligne 1345 ;
- NFR-F028 : FEAT-028 Intégration Powens, ligne 2563.

- NFR-QG-00 : Contrôles d'abord — les contrôles sont livrés et verts avant toute story fonctionnelle ; livrés par l'epic 1 (fondations), chaque story de l'epic 1 qui livre un contrôle le rend vert à sa fin (trace-smoke et ArchUnit ne passent qu'une fois le squelette et deux services en place) ; aucune story fonctionnelle ne démarre tant que les contrôles bloquants (NFR-QG-01 à 04, NFR-QG-07 à 09) ne sont pas livrés et verts et que les contrôles à la demande (NFR-QG-05, NFR-QG-06) ne sont pas livrés et exécutables.
- NFR-QG-01 : Hexagone back — outil : test ArchUnit `*ArchitectureTest` dans chaque service ; seuil : deux règles, le sens des dépendances, et aucun framework dans domain et application ; moment : bloquant à chaque build.
- NFR-QG-02 : Hexagone front — outil : dependency-cruiser, configuration `frontend/.dependency-cruiser.cjs` ; seuil : règles en sévérité error, le sens des dépendances, et aucun paquet npm dans `src/domain` et `src/application` ; moment : bloquant.
- NFR-QG-03 : Couverture back — outil : JaCoCo check ; seuil : 100 % lignes et branches sur domain et application, 80 % sur adapter et infrastructure ; moment : phase verify, bloquant.
- NFR-QG-04 : Couverture front — outil : Vitest `coverage.thresholds` par glob ; seuil : mêmes seuils (100 % lignes et branches sur `src/domain` et `src/application`, 80 % sur `src/adapters` et `src/infrastructure`) ; moment : bloquant.
- NFR-QG-05 : Mutation back — outil : PIT dans le profil Maven `mutation`, sur domain et application ; seuil : `mutationThreshold` 85, historique activé ; moment : à la demande.
- NFR-QG-06 : Mutation front — outil : StrykerJS sur `src/domain` et `src/application` ; seuil : thresholds break 85 / high 90, mode incremental ; moment : à la demande.
- NFR-QG-07 : CVE back — outil : OWASP Dependency-Check dans le profil Maven `security` ; seuil : `failBuildOnCVSS` 7 ; configuration : clé d'API NVD via `settings.xml`, OSS Index et Node Audit désactivés, mise à jour de la base désactivée pendant les scans ; moment : lancé par `scripts/gates/cve.sh`, hors du build Maven par défaut, à chaque story dans la boucle de développement et à la demande.
- NFR-QG-08 : CVE back et front — outil : Trivy fs ; seuil : sévérités HIGH et CRITICAL, porte bloquante avec `--ignore-unfixed` ; vulnérabilités sans correctif journalisées dans `reports/` sans bloquer ; moment : porte bloquante.
- NFR-QG-09 : Tracing prouvé — outil : `scripts/gates/trace-smoke.sh` ; seuil : prouve qu'une trace traverse deux services, avec un span JDBC et un span Kafka ; moment : avant de déclarer terminée toute story qui touche un échange entre services [A-G-03].
- NFR-QG-10 : Les contrôles ne se modifient pas — un contrôle rouge se corrige dans le code ou les tests, jamais en modifiant le contrôle ; fichiers interdits de modification : `scripts/gates/**`, `.bmad-loop/policy.toml`, `.bmad-loop/gates.sha256`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml` ; interdit d'abaisser un seuil, d'ajouter une exclusion de couverture ou de mutation, de désactiver un test ; seuil inatteignable : s'arrêter et le signaler ; moment : en permanence.
- NFR-PAR-01 : Parité par le golden master — outil : golden master du legacy dérivé (contrat `docs/retro/golden-master-contract.md`) ; seuil : rejoue sans échec avant la bascule, tous les scénarios retenus à l'étape G2 passent, 31 au plus (33 capturés moins SC-003-03 et SC-010-02) ; un seul scénario divergent prévu, SC-003-04, sous condition (score > 75 : rejeu avant 06:00 ou après 23:00 +45, < 30 s après un autre virement +55, > 8 opérations du jour +50), rejoué selon NFR-PAR-05 sinon écart permanent ; décompte prévisionnel à confirmer au premier rejeu, autres écarts découlant de D1–D12 traités en G2 et non comme régressions ; moment : une seule fois, développement terminé, pas entre stories ni epics.
- NFR-PAR-02 : Bugs corrigés prouvés — chacun des 50 bugs à corriger est couvert par un test automatisé qui vérifie le comportement attendu (critère d'acceptation de la FR qui le cite) ; moment : dans la story de la FR.
- NFR-PAR-03 : Règles couvertes — chaque règle de gestion reprise est couverte par au moins une exigence du PRD et par au moins un test (mesure SM-2 : `docs/retro/tools/check-prd-coverage.py`, 100 %).
- NFR-PAR-04 : Données de référence — un jeu équivalent au legacy (`import.sql` : 7 clients, 7 comptes, 45 transactions…) chargé par une commande ; une commande de remise à zéro le rétablit ; sert au développement local, aux tests de bout en bout et au rejeu du golden master.
- NFR-PAR-05 : Rejeu déterministe — rejeu avec horloge fixée et état de données réinitialisé ; port Horloge dans le domaine ; profil de rejeu avec horloge réglée au démarrage sur une date et heure fixes puis qui avance normalement (pas figée) ; remise à zéro (NFR-PAR-04) avant le rejeu complet et avant chaque scénario `writes: true` (dont SC-003-04) ; heure de référence entre 06:00 et 23:00 (ex. 10:00 Europe/Paris), jour ouvré ; `replay-golden-master.py` ne fournit ni horloge ni remise à zéro ; à défaut, SC-003-04 en écart permanent ; moment : à appliquer à l'étape G2.
- NFR-SEC-01 : Hachage des mots de passe — mots de passe stockés et vérifiés par une fonction de hachage à sens unique sûre (FEAT-001, BUG-002).
- NFR-SEC-02 : Fixation de session — à l'authentification, la session existante est invalidée et une nouvelle créée avant de stocker le contexte d'authentification ; aucune fixation possible (BUG-003, BUG-092).
- NFR-SEC-03 : Propriété des ressources — toute action ou lecture sur une ressource d'un client (compte, document, virement programmé, bénéficiaire) vérifie qu'elle appartient à l'utilisateur authentifié ; contrôle d'accès par rôle appliqué dans les services, indépendamment du filtrage des requêtes (BUG-013, BUG-027, BUG-047, BUG-058, BUG-093).
- NFR-SEC-04 : Messages d'erreur — aucun message affiché à un utilisateur n'expose de détail technique (pile d'appels, code d'erreur interne).
- NFR-SEC-05 : Masquage IBAN — IBAN masqués dans tous les journaux du serveur (ex. `FR76****1234`) (BUG-086) ; montants non masqués, écart accepté (risque R-3).
- NFR-SEC-06 : Secrets — aucun secret (clé d'API, mot de passe) dans le code source ni dans un fichier versionné ; secrets issus de la configuration d'exécution (BUG-034, BUG-091).
- NFR-SEC-07 : Expiration de session — session expirée après 30 minutes d'inactivité, comme le legacy (`docs/retro/00-global/PERMISSIONS.md`).
- NFR-SEC-08 : Seuils du contrôle de fraude — seuil de score (75), seuil de fraude (10 000 €), plafond du cumul glissant (10 000 €), seuil d'alerte et seuil de mise en attente (5 000 €) en configuration de déploiement, validés au démarrage ; configuration invalide = le service ne démarre pas (D2).
- NFR-SEC-09 : Expiration technique de l'accès aux données du contrôle — l'accès aux données d'historique du contrôle de fraude a un délai d'expiration technique ; son expiration est une erreur du contrôle et entraîne le refus du virement (FR-027-02) ; valeur fixée dans la décision d'architecture, en configuration validée au démarrage (D4) ; 500 ms proposé si le contrôle devenait externe.
- NFR-INT-01 : Port et adaptateur simulé — chaque fournisseur externe (Azure OpenAI, Powens, AlphaVantage) appelé derrière un port ; adaptateur simulé actif par défaut en local, dans les tests et les contrôles ; vrais appels en option de configuration ; aucun test ni contrôle ne dépend du réseau ni d'une clé externe.
- NFR-INT-02 : Résilience — chaque appel réel a un délai d'attente et passe par un coupe-circuit ; cotations mises en cache pour respecter la limite AlphaVantage (25 requêtes par jour, 5 par minute) ; indisponibilité d'un fournisseur signalée à l'utilisateur sans bloquer le reste.
- NFR-INT-03 : Mêmes fournisseurs — Azure OpenAI, Powens, AlphaVantage, comme le legacy.
- NFR-PERF-01 : Performance — au 95e centile, en local, avec adaptateurs simulés : pages de navigation < 200 ms ; pages à calcul léger (relevé, budget, bénéficiaires) < 500 ms ; page bourse < 3 000 ms ; téléchargement PDF < 2 000 ms [A-G-04] ; mesure SM-5 sur le parcours UJ-1 et chaque type de page.
- NFR-BATCH-01 : Horaires des traitements planifiés — intérêts tous les jours à 02h00 ; détection de fraude nocturne tous les jours à 02h30 ; relevés mensuels le 1er du mois à 03h00 ; virements programmés toutes les 5 minutes ; soldes faibles et seuils toutes les 30 minutes ; nouveau : rejet automatique des virements suspendus à l'heure butoir (FR-027-17), fréquence fixée en architecture, l'heure butoir fait foi (B5).
- NFR-BATCH-02 : Journal d'exécution — chaque exécution d'un traitement planifié est enregistrée (heure, résultat) ; exception (décision du 2026-10-05) : le rejet automatique tourne toutes les minutes et n'écrit une entrée que s'il rejette au moins un virement ou échoue (écran admin limité aux 50 dernières entrées) ; chaque virement rejeté garde son audit TRANSFER_AUTO_REJECT (FEAT-026).
- NFR-BATCH-03 : Fuseau horaire — fuseau des traitements configuré explicitement [A-G-07 : Europe/Paris].
- NFR-OBS-01 : Tracing de bout en bout — chaque requête et chaque message entre services tracé selon l'architecture de tracing imposée (addendum A.1) ; preuve : NFR-QG-09.
- NFR-OBS-02 : Contrôle de fraude visible dans la trace — la trace d'un virement montre le contrôle, son issue (exécuté, suspendu, refusé pour erreur) et un statut d'erreur le cas échéant ; l'identifiant de trace est la référence affichée au client en cas de refus (FR-003-27, D9) ; moyen sans span créé à la main, décidé en architecture.
- NFR-UI-01 : Langue et libellés — interface en français ; libellés et messages repris à l'identique du corpus (`docs/retro/00-global/MESSAGES.md` et `MESSAGES.md` de chaque fonctionnalité), sauf correction d'un bug à corriger.
- NFR-UI-02 : Attributs de test — chaque champ et bouton actionné par un scénario du golden master porte un attribut de test stable [A-G-08 : forme décidée en architecture].

- NFR-F006 (FEAT-006, NFR propre à la fonctionnalité) : En cas d'erreur inattendue lors de l'enregistrement ou de la suppression d'un plafond, aucun message technique interne n'est montré à l'utilisateur (le legacy affiche « Erreur : {message technique} » et « Erreur suppression : {message technique} », MSG-006-073 et MSG-006-074, non reproduits). [ASSUMPTION A-006-1 : à leur place s'affiche le message générique « Une erreur est survenue. Veuillez reessayer. », sans détail technique ; texte proposé, à valider avant le rejeu] Un plafond qui dépasse la capacité de stockage du montant (13 chiffres entiers, 2 décimales) est refusé par une validation explicite, pas par une erreur technique.
- NFR-F007 (FEAT-007, NFR propre à la fonctionnalité) : En cas d'erreur inattendue lors de la simulation, de la demande, du paiement ou de la suppression, aucun message technique interne n'est montré à l'utilisateur (le legacy affiche « Erreur simulation : {message technique} », « Erreur : {message technique} » et « Erreur suppression : {message technique} », non reproduits).
- NFR-F010 (FEAT-010, NFR propre à la fonctionnalité) : chaque appel au service d'IA applique un délai de connexion de 15 secondes et un délai de lecture de 30 secondes (FR-010-08) ; le reste (port, simulation, coupe-circuit, cache) relève de NFR-INT.
- NFR-F012 (FEAT-012, NFR propre à la fonctionnalité) : le jeton Powens n'est conservé que dans la session de l'utilisateur et n'est jamais affiché ; délai de 15 secondes par appel à Powens (FR-012-17), le reste relevant de NFR-INT.
- NFR-F028 (FEAT-028, NFR propre à la fonctionnalité) : le secret client Powens et le jeton permanent ne sont jamais affichés ni journalisés en clair ; délai de 15 secondes par appel (FR-028-12), le reste relevant de NFR-INT.

### Additional Requirements

Dossier source : `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/`.
Abréviations de fichier : `spine` = ARCHITECTURE-SPINE.md ; `gm` = golden-master-architecture.md ; `ecarts` = ecarts-voulus-golden-master.md ; `versions` = versions-verifiees.md.

#### Epic 1 imposé

- AR-1 : L'epic 1 « Fondations contrôlées » suit l'ordre du pilote, six stories exactement, pas de story 1.7 : 1.1 squelette ; 1.2 contrôles d'architecture ; 1.3 couverture ; 1.4 flux témoin tracé ; 1.5 mutation ; 1.6 CVE. Aucune story fonctionnelle ne démarre avant que les contrôles bloquants soient livrés et verts ; `scripts/gates/check-gates-exist.sh` en code 0 à la fin de l'epic 1 (source : spine:242-243, AD-15)
- AR-2 : L'epic 1 ne porte que deux services : `coeur-bancaire` et `notifications` (2 conteneurs `postgres-` pendant l'epic 1, 8 ensuite) (source : spine:88, spine:242, AD-3, AD-15)
- AR-3 : Story 1.1 — squelette : POM parent `pom.xml` à la racine, deux services hexagonaux minimaux `coeur-bancaire` et `notifications` (paquets `fr.nextbanking.coeurbancaire`, `fr.nextbanking.notifications`), front hexagonal minimal, build vert ; les cinq paquets (`domain`, `application`, `adapter.in`, `adapter.out`, `infrastructure`) contiennent chacun au moins une classe, dont un type `public` sous `adapter` (exigé par `canary-arch.sh`, `failOnEmptyShould` à `true`) (source : spine:30, spine:73, spine:242, AD-1, AD-15)
- AR-4 : Story 1.1 — `frontend/package-lock.json` versionné dès la 1.1 (`build.sh` lance `npm ci`) ; `frontend/vite.config.ts` créé par la 1.1 (complété par la 1.3 pour les seuils, par la 1.4 pour le proxy) ; la 1.1 ne porte rien du thème (AD-13) (source : spine:80, spine:131, spine:212, AD-2, AD-7, AD-13)
- AR-5 : Story 1.2 — test ArchUnit `HexagonalArchitectureTest` par service (deux règles exactement : `Architectures.onionArchitecture()` et aucun framework dans domain/application) ; classes `*ArchitectureTest` séparées pour `HorlogeArchitectureTest` (AD-19) et `ClientsHttpArchitectureTest` (AD-4) ; preuve `canary-arch.sh services/coeur-bancaire fr/nextbanking/coeurbancaire` et `canary-arch.sh services/notifications fr/nextbanking/notifications` → `OK  ArchUnit détecte la dépendance domaine -> adaptateur` (source : spine:73-74, spine:95, spine:302, AD-1, AD-4, AD-19)
- AR-6 : Story 1.2 — crée le fichier protégé `frontend/.dependency-cruiser.cjs` (règles `domain-ne-depend-de-rien`, `application-ne-depend-pas-des-adaptateurs`, `hexagone-sans-paquet-npm`, `options.tsPreCompilationDeps: true`, sévérité error) ; test négatif `import { useState } from "react"` dans `src/application` → code non nul (source : spine:80-81, spine:160, AD-2, AD-10)
- AR-7 : Story 1.2 — règle front du temps métier : test Vitest `frontend/src/architecture-temps.test.ts` interdisant `Date.now()`, `new Date()` sans argument et `Temporal.Now` dans `src/domain` et `src/application` [ASSUMPTION : affectation] (source : spine:357-358, AD-23)
- AR-8 : Story 1.2 — port Horloge et `HorlogeArchitectureTest` [ASSUMPTION : affectation] (source : spine:302, AD-19)
- AR-9 : Story 1.3 — JaCoCo (0.8.15) dans le POM parent une seule fois (`prepare-agent`, `report`, `check`) : 100 % lignes et branches sur `*.domain*`/`*.application*`, 80 % sur `*.adapter*`/`*.infrastructure*`, `haltOnFailure` jamais `false` ; Vitest `test.coverage` (provider `v8`) dans le seul `frontend/vite.config.ts` : `src/domain/**` et `src/application/**` à 100, `src/adapters/**` et `src/infrastructure/**` à 80 ; aucun `vitest.config.*` (source : spine:129-131, AD-7)
- AR-10 : Story 1.4 — flux témoin : `POST /api/coeur-bancaire/temoin` → `coeur-bancaire` → écriture en base → publication Kafka par l'outbox sur `coeur-bancaire.temoin-ecrit.v1` (clé `temoinId`) → `notifications` consomme → écriture en base ; le point témoin est dans la liste fermée des chemins publics [ASSUMPTION] ; schéma `contrats/evenements/coeur-bancaire.temoin-ecrit.v1.schema.json` livré par la 1.4 (source : spine:242, spine:254, spine:344, spine:351, AD-15, AD-16, AD-22)
- AR-11 : Story 1.4 — crée le fichier protégé `scripts/gates/trace-smoke.sh` : échoue si la trace du flux témoin ne contient pas deux services distincts, au moins un span JDBC, un span d'émission et un span de consommation Kafka sous le même identifiant de trace (source : spine:107, spine:160, spine:243, AD-5, AD-10, AD-15)
- AR-12 : Story 1.4 — `compose.yaml` (2 bases, Kafka, collecteur `config/otelcol/config.yaml`, Jaeger v2, conteneur `web` + `frontend/Dockerfile` + `nginx.conf`), proxy `/api` dans `vite.config.ts`, renvoi `/api/{identifiant}/**`, outbox et consommateur idempotent du flux témoin ; vérifier le tag `node:24.21.0-alpine` et les noms d'artefacts Flyway Boot 4 (`spring-boot-starter-flyway`, `flyway-database-postgresql`) (source : spine:87, spine:115, spine:122, spine:276, spine:290, versions:48, AD-3, AD-6, AD-17, AD-18)
- AR-13 : Story 1.5 — PIT (`pitest-maven` 1.30.0 + `pitest-junit5-plugin` 1.2.3) dans le seul profil Maven `mutation` du POM parent, `targetClasses` `fr.nextbanking.*.domain.*` et `fr.nextbanking.*.application.*`, `mutationThreshold` 85, historique `${user.home}/.cache/next-banking-webapp/pit/${project.artifactId}.history` ; StrykerJS 10.0.0 `frontend/stryker.config.json` (`testRunner` vitest, break 85 / high 90 / low 85, `incrementalFile` `reports/stryker-incremental.json`) ; preuve `scripts/gates/mutation.sh` (source : spine:138-141, AD-8)
- AR-14 : Story 1.6 — OWASP Dependency-Check 13.0.0 dans le profil Maven `security` (failBuildOnCVSS 7, `nvdApiServerId` `nvd`, OSS Index et Node Audit désactivés, `autoUpdate` false, suppressions `config/dependency-check-suppressions.xml`) ; Trivy fs 0.75.0 HIGH/CRITICAL `--ignore-unfixed`, `reports/cve-all.json`, `reports/cve-unfixed.tsv` ; preuve `scripts/gates/cve.sh` (source : spine:148-152, AD-9)
- AR-15 : Fichiers protégés et qui les crée : `scripts/gates/*.sh` (hors `trace-smoke.sh`) déjà installés par le pilote ; `.trivyignore` et `config/dependency-check-suppressions.xml` déjà créés vides (étape 5 du pilote) ; `frontend/.dependency-cruiser.cjs` par la story 1.2 ; `scripts/gates/trace-smoke.sh` par la story 1.4. Aucune autre story ne modifie `scripts/gates/**`, `.bmad-loop/policy.toml`, `.bmad-loop/gates.sha256`, `.claude/settings.json`, `.claude/hooks/**`, `docs/quality-gates.md`, `frontend/.dependency-cruiser.cjs`, `.trivyignore`, `config/dependency-check-suppressions.xml` ; interdit d'abaisser un seuil, d'ajouter une exclusion ou de désactiver un test (source : spine:159-162, AD-10)
- AR-16 : Empreinte `.bmad-loop/gates.sha256` figée par l'humain à l'étape 10 du pilote après l'epic 1 (inclut `replay-mapping.json` et `e2e/golden-master`), hook installé à l'étape 11 ; à partir de l'epic 2, `sha256sum -c .bmad-loop/gates.sha256` est la première commande `[verify]` (source : spine:161-162, AD-10)

#### Découpage en services et propriété des données

- AR-17 : 8 microservices Java 21 / Spring Boot 4 / Maven, un module par service sous `services/<identifiant>/` (pom.xml, Dockerfile, `reference-data/reference-data.sql`, `src/main/java/fr/nextbanking/<id>/{domain,application,adapter/in,adapter/out,infrastructure}/`), POM parent racine ; `ls services/` liste exactement les 8 identifiants une fois le découpage complet (source : spine:219-236, spine:443-450, AD-14)
- AR-18 : `identite-clients` (préfixe `/api/identite-clients/`, port dev 8081) possède `CUSTOMERS` (connexion, rôles, conseiller affecté) ; seul détenteur de la clé privée du JWT (source : spine:224, spine:251, spine:275, gm:31, AD-14, AD-16)
- AR-19 : `coeur-bancaire` (8082) possède `BANK_ACCOUNTS`, `TRANSACTIONS`, `TRANSFERS`, `RECURRING_TRANSFERS`, `EXTERNAL_BENEFICIARIES`, `FRAUD_ALERTS`, `BUDGETS`, `BATCH_JOB_LOGS` ; seul service qui modifie un solde (`UPDATE bank_accounts` interdit ailleurs) ; débit et crédit atomiques dans sa seule base ; tous les traitements planifiés y vivent (source : spine:225, spine:233-236, spine:319, AD-14, AD-21)
- AR-20 : `credits` (8083) possède `LOANS`, `LOAN_REPAYMENTS`, `CREDIT_SCORES`, `RISK_PROFILES` ; ni paiement d'échéance ni approbation de prêt ne déplacent d'argent (FR-007-12, FR-024-06) (source : spine:226, spine:234, AD-14)
- AR-21 : `documents` (8084) possède `DOCUMENTS` ; seul producteur de PDF (relevé, RIB, attestation, IFU) ; l'attestation porte le solde de la projection et la date du port Horloge de documents ; bibliothèque PDF choisie par la première story documents après vérification dans `versions-verifiees.md` (source : spine:227, spine:235, spine:619, AD-14)
- AR-22 : `notifications` (8085) possède `NOTIFICATIONS` ; stocke, applique le plafond par utilisateur `nb.notifications.plafond=20` (FR-025-04), ne lit aucun seuil métier (`! grep -rn 'nb\.fraude\.' services/notifications/src/main`) et ne compose aucun texte (source : spine:228, spine:371-372, AD-14, AD-25)
- AR-23 : `audit` (8086) possède `AUDIT_LOGS`, seul à l'écrire ; aucune API HTTP métier (`/api/audit/**` non renvoyé par le conteneur web) ; ses tests lisent `AUDIT_LOGS` par JDBC ; rejette et journalise un code inconnu (source : spine:229, spine:268, spine:288, AD-14, AD-17, AD-18)
- AR-24 : `bourse` (8087) possède `WATCHLIST_ITEMS` ; cache des cotations AlphaVantage (25 requêtes/jour, 5/minute) (source : spine:230, spine:309, AD-14, AD-20)
- AR-25 : `open-banking-conseiller` (8088) possède `BANK_PRODUCTS`, `SAVINGS_PRODUCTS`, `CREDIT_PRODUCTS`, `INVESTMENT_PRODUCTS` et l'état Powens par client (jeton Powens, comptes externes, compte sélectionné, message de résultat) (source : spine:231, spine:310, AD-14, AD-20)
- AR-26 : Une base PostgreSQL par service : conteneur `postgres-<identifiant>`, utilisateur et mot de passe propres, volume nommé `pgdata-<identifiant>` (survit à `docker compose down` sans `-v`, FR-002-09, FR-023-06) ; `SPRING_DATASOURCE_URL` `jdbc:postgresql://postgres-<identifiant>:5432/...` ; migrations Flyway par service ; aucune lecture de la base d'un autre service ; références entre services = identifiants numériques copiés sans clé étrangère (source : spine:87-88, spine:233, AD-3, AD-14)
- AR-27 : Kafka seul bus inter-services ; aucun appel HTTP entre services ; seules les classes de `adapter.out.fournisseur` dépendent d'un client HTTP (`RestClient`, `WebClient`, `RestTemplate`, `java.net.http.HttpClient`) ; écrans multi-domaines : appels parallèles depuis le front, aucune projection de soldes pour affichage (source : spine:94-95, AD-4)
- AR-28 : Projections décidées : clients (`identite-clients.client-modifie.v1`) dans `coeur-bancaire`, `credits` et `documents` ; comptes (`coeur-bancaire.compte-modifie.v1`) et opérations (`coeur-bancaire.operation-enregistree.v1`) dans `documents` (trois projections dans documents : comptes, opérations, clients) (source : spine:94, spine:235, AD-4, AD-22)
- AR-29 : Catalogue des topics inter-services (un seul producteur par topic, déclaré par un bean `NewTopic` dans `infrastructure`, 3 partitions, réplication 1) : `identite-clients.client-modifie.v1` (→ coeur-bancaire, credits, documents ; clé `clientId` ; `clientId`, `login`, `nomComplet`, `role`, `actif`, `conseillerId`, `dateCreation`, `supprime`, `versionAgregat`) ; `identite-clients.echecs-connexion-repetes.v1` (→ coeur-bancaire ; `clientId`, `nombreEchecs`) ; `coeur-bancaire.compte-modifie.v1` (→ documents ; `compteId`, `clientId`, `numero`, `type`, `iban`, `bic`, `solde`, `versionAgregat`) ; `coeur-bancaire.operation-enregistree.v1` (→ documents ; clé `compteId` ; `operationId`, `compteId`, `instant`, `libelle`, `type`, `montant`, `soldeApres`, `versionAgregat`) ; `coeur-bancaire.virement-decide.v1` (→ notifications ; `virementId`, `clientId`, `etat`, `notification`) ; `coeur-bancaire.solde-faible-detecte.v1` (→ notifications ; `compteId`, `notification`) ; `coeur-bancaire.echeance-non-executee.v1` (→ notifications ; `virementProgrammeId`, `notification`) ; `credits.pret-decide.v1` (→ notifications ; `pretId`, `clientId`, `statut`, `notification`) ; `{service}.audit.v1` (→ audit) ; `coeur-bancaire.temoin-ecrit.v1` (→ notifications ; `temoinId`) (source : spine:283, spine:331-344, AD-18, AD-22)
- AR-30 : Topics internes (producteur = consommateur) : `coeur-bancaire.virement-a-traiter.v1` (remplace la file JMS `virements`) et `credits.pret-demande.v1` (remplace `pretDemandesQueue`) (source : spine:190, spine:346, gm:179, AD-12, AD-22)
- AR-31 : Chaque topic livré a son schéma `contrats/evenements/<topic>.schema.json` avec exemple, écrit par le seul producteur, livré par la story qui produit le topic ; tests de contrat `*ContratEvenement*Test` côté producteur (champs exacts) et côté consommateur (désérialise l'exemple) ; un changement incompatible crée `v{n+1}` ; un topic ou un champ nouveau s'ajoute à AD-22 avant la story qui le produit (source : spine:331, spine:347-351, AD-22)
- AR-32 : Événements de projection = état complet de l'agrégat + `versionAgregat` croissant ; le consommateur n'applique que si `versionAgregat` est strictement supérieur (en plus de `evenements_traites`) (source : spine:348, AD-22)

#### Socles transverses

- AR-33 : Auth — `POST /api/identite-clients/connexion` vérifie le mot de passe haché (`DelegatingPasswordEncoder`, `{bcrypt}`) et émet un JWT signé par `NimbusJwtEncoder` (`spring-security-oauth2-jose` 7.1.1) ; pas de passerelle, pas de BFF, pas d'Authorization Server ; paire RSA générée par `make secrets` dans `.secrets/` [ASSUMPTION : RSA] ; les hachés de `reference-data.sql` d'identité sont générés par un script depuis les variables d'environnement (`RETRO_ADMIN_PASSWORD`, `RETRO_CONSEILLER_PASSWORD`, `RETRO_CLIENT_PASSWORD`), jamais en clair (source : spine:250-251, gm:88-94, AD-16)
- AR-34 : JWT — revendications `sub`, `roles` (CLIENT, CONSEILLER, ADMIN), `jti` unique, `iat`, `exp` ; durée 30 minutes (NFR-SEC-07), 60 minutes en profil `rejeu` ; renouvellement `POST /api/identite-clients/renouvellement` sur activité sous 10 minutes restantes ; `GET /api/identite-clients/moi` ; `iat`/`exp` par une `java.time.Clock` système dans `adapter.out.jeton`, jamais le port Horloge ; jeton en `localStorage` clé `nb.jeton`, jamais `sessionStorage`, envoyé en `Authorization: Bearer` ; tout 401 → suppression de `nb.jeton` et `/login` (source : spine:252-258, gm:109-110, AD-16)
- AR-35 : Déconnexion (FR-001-10, décision Q1 (a)) : le front supprime le jeton, appelle `POST /api/identite-clients/deconnexion` (audit LOGOUT), affiche `/login` ; pas de liste de révocation. Client désactivé : bloque nouvelle connexion et renouvellement, jeton émis valable jusqu'à expiration (risque R-5) (source : spine:259-260, gm:259, AD-16)
- AR-36 : RBAC — tout `/api/**` authentifié par défaut ; liste fermée des chemins publics comparés en entier : `POST /api/identite-clients/connexion`, `GET /api/open-banking-conseiller/powens/retour`, `POST /api/coeur-bancaire/temoin` ; chaque service vérifie le jeton (`spring-boot-starter-security-oauth2-resource-server` 4.1.1, pas l'ancien nom obsolète) et les rôles ; appartenance décidée dans le service détenteur (CLIENT : `clientId = sub` ; CONSEILLER : `conseillerId = sub` dans la projection `identite-clients.client-modifie.v1` ; ADMIN : tout) ; portefeuille déduit côté serveur `GET /api/<service>/conseiller/portefeuille` ; autre client 403, inexistant 404, `{"message"}` (source : spine:254-255, versions:16, AD-16)
- AR-37 : RBAC preuve — une classe `*SecuriteIT` par service (sans jeton → 401, autre rôle → 403, ressource d'un autre client → 403, conseiller hors portefeuille → 403) ; livrée par la story de connexion (FEAT-001) pour identité, puis par la première story de chaque service suivant (source : spine:261, AD-16)
- AR-38 : Outbox — ligne `outbox` écrite dans la même transaction que l'écriture métier, publiée après validation par `KafkaTemplate` dans le même fil, puis relais planifié toutes les secondes qui republie dans l'ordre des identifiants [ASSUMPTION] ; relais actif en profil `rejeu`, non journalisé ; délai bout en bout visé 5 s ; transactions par le port `application.port.out.Transaction` (implémenté dans `adapter.out.persistance`), avec `executerIndependamment(...)` ; aucune annotation transactionnelle dans `application` (source : spine:73, spine:287, AD-1, AD-18)
- AR-39 : Enveloppe et sérialisation Kafka — topic `{service-producteur}.{evenement-au-participe}.v{n}` ; clé = identifiant numérique de l'agrégat ; enveloppe `idEvenement` (UUID), `type`, `version`, `horodatage` (ISO 8601 UTC, port Horloge), `producteur`, `donnees` ; `StringSerializer`/`StringDeserializer`, jamais d'en-tête `__TypeId__` ; `group.id` = identifiant du service consommateur ; `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` (source : spine:117, spine:283-286, AD-6, AD-18)
- AR-40 : Audit — chaque service publie sur `{service}.audit.v1` via la table `outbox_audit`, par `Transaction.executerIndependamment(...)` (validée quelle que soit l'issue, FR-026-04) ; `donnees` : `utilisateur`, `action` (catalogue fermé : LOGIN, LOGIN_FAILED, LOGOUT, ADMIN_DEACTIVATE, ADMIN_ACTIVATE, ADMIN_LOAN_APPROVE, ADMIN_LOAN_REFUSE, ADMIN_TRANSFER_RELEASE, ADMIN_TRANSFER_REJECT, VIREMENT, TRANSFER_EXECUTED, TRANSFER_AUTO_REJECT, TRANSFER_REJECT_INSUFFICIENT_FUNDS), `detail`, `adresseIp` (absente pour une action système) ; clé = identifiant du virement pour les codes d'un virement, de l'utilisateur sinon ; test par service émetteur « opération annulée → entrée présente dans `outbox_audit` » (source : spine:288, spine:290, AD-18)
- AR-41 : Idempotence consommateurs — table `evenements_traites` (`idEvenement` en clé primaire) écrite dans la même transaction que l'effet ; écouteurs unitaires, jamais batch ; test Testcontainers (`testcontainers-kafka`, `testcontainers-postgresql` 2.0.5) qui livre deux fois un message brut et constate un seul effet (source : spine:289-290, versions:41, AD-18)
- AR-42 : Idempotence HTTP du virement — en-tête `Idempotency-Key` (UUID par soumission) enregistré avec le virement sous contrainte d'unicité ; une deuxième soumission renvoie le premier résultat [ASSUMPTION] (source : spine:192, AD-12)
- AR-43 : Transitions concurrentes — machine d'états dans `domain` pour toute entité à statut (virement, virement programmé et échéance, prêt, alerte de fraude, bénéficiaire) ; mise à jour conditionnelle `UPDATE … SET statut = ? WHERE id = ? AND statut = ?`, zéro ligne = refus sans effet ; test `*TransitionConcurrente*Test` livré avec la story de chaque entité (source : spine:364-365, AD-24)
- AR-44 : Horloge — port `application.port.out.Horloge` (`instant()`, `aujourdhui()`, `fuseau()`) implémenté dans `adapter.out.horloge` (système ou réglée par `HORLOGE_DECALAGE`) ; `nb.fuseau=Europe/Paris` validé au démarrage, jamais lu sur `TZ` ; interdits hors `adapter.out.horloge`/`adapter.out.jeton` : `now(..)` de `java.time`, `java.time.Clock`, `System.currentTimeMillis()` ; aucun `now()`, `CURRENT_TIMESTAMP`, `CURRENT_DATE` dans les migrations (source : spine:297-302, AD-19)
- AR-45 : Temps métier du front — toute date métier d'un écran fournie par le service dans un champ `aujourdhui` (`yyyy-MM-dd`) ; formateurs `src/domain` par `Intl.DateTimeFormat('fr-FR', { timeZone: 'Europe/Paris' })` ; seule exception : renouvellement du jeton dans `src/adapters` (source : spine:357, AD-23)
- AR-46 : Erreurs — format unique `{"message": "<texte affichable>"}` + `reference` (identifiant de trace) si l'écran l'affiche ; `message` toujours un texte du legacy ou de textes-nouveaux.md, y compris sur 5xx, sans pile ni code interne (NFR-SEC-04) ; pas de redirection serveur pour l'authentification (401/403), seule exception le 302 de `powens/retour` (source : spine:271-272, spine:383, AD-17)
- AR-47 : Conventions d'API — préfixe `/api/{identifiant}/**`, JSON en camelCase français (`identifiant`, `motDePasse`, `compteId`) ; écran multi-services : appels parallèles, jamais en cascade (NFR-PERF-01) ; patrimoine total calculé par le front en centimes [ASSUMPTION] ; validation IBAN (MOD-97, SEPA) seulement au cœur bancaire via `POST /api/coeur-bancaire/beneficiaires/verification-iban` [ASSUMPTION] ; masquage `FR76****1234` avec test de référence commun par service, sans bibliothèque partagée (source : spine:270-274, AD-17)
- AR-48 : Adresse IP d'origine — conteneur web et proxy Vite écrasent `X-Forwarded-For` (`$remote_addr`, jamais `$proxy_add_x_forwarded_for`) ; un service ne le retient que si l'adresse distante est dans `nb.reseau.mandataires-de-confiance` (sous-réseau compose fixe [ASSUMPTION : `172.30.0.0/24`]) ; résolution unique dans `adapter.in.http` ; test d'intégration par service émetteur d'audit ; livré par la story de connexion (source : spine:118, spine:269, spine:276, AD-6, AD-17)
- AR-49 : Montants — `BigDecimal` 2 décimales, `numeric(19,2)`, chaîne décimale à point en JSON, `RoundingMode.HALF_UP` à la dernière étape ; front en centimes entiers, contrôles métier par le service seul ; dates instants en UTC (`timestamptz`), dates seules `yyyy-MM-dd` (source : spine:381-382, Conventions)
- AR-50 : Configuration — propriétés métier sous `nb.` validées au démarrage par les objets-valeurs du domaine (valeur invalide = pas de démarrage, NFR-SEC-08) ; secrets en variables d'environnement seulement ; seuils fraude `nb.fraude.*` (score 75, fraude 10 000 €, cumul glissant 10 000 €, alerte 5 000 €, mise en attente 5 000 €) ; `nb.fraude.acces-historique.delai=2s` [ASSUMPTION : chiffre] (source : spine:197-198, spine:386, AD-12)
- AR-51 : Journaux — sortie console, `traceId` et `spanId` dans chaque ligne ; IBAN toujours masqué (objet-valeur `Iban` dont `toString()` rend `FR76****1234`, NFR-SEC-05) ; montants en clair (R-3) ; jamais de mot de passe ni de jeton (source : spine:120, spine:385, AD-6)
- AR-52 : Tracing — `spring-boot-starter-opentelemetry` 4.1.1 et `datasource-micrometer-spring-boot` 2.3.0 ; propriétés explicites dans chaque service : `management.opentelemetry.tracing.export.otlp.endpoint=http://otel-collector:4318/v1/traces`, `management.tracing.sampling.probability=1.0` (défaut 0.1), `spring.kafka.template.observation-enabled=true` et `spring.kafka.listener.observation-enabled=true` (défaut false) ; jamais `management.otlp.tracing.export.enabled` ; aucun span créé à la main (`Observation.createNotStarted`, `Observation.start`, `@Observed`, `@NewSpan`, `spanBuilder(`, `nextSpan(` interdits) (source : spine:101-106, spine:199, spine:201, versions:11-14, AD-5, AD-12)
- AR-53 : Conteneurs compose — `postgres:18.6`, `apache/kafka:4.2.2` (KRaft, réplication 1), `otel/opentelemetry-collector:0.162.0` (`/etc/otelcol/config.yaml`, ports 4317/4318), `cr.jaegertracing.io/jaegertracing/jaeger:2.21.0` (16686 ; jamais `jaegertracing/all-in-one`), services `eclipse-temurin:21` → `eclipse-temurin:21-jre`, `web` `nginx:1.30.5-alpine` port 8080 (construction `node:24.21.0-alpine` [ASSUMPTION : tag à vérifier]) ; `TZ=Europe/Paris` partout ; nom de service compose = identifiant ; `healthcheck` sur chaque conteneur (`pg_isready`, `kafka-broker-api-versions.sh`, `/actuator/health` seul point actuator exposé) et `depends_on: condition: service_healthy` ; publiés : web 8080, jaeger 16686, services 8081-8088 (source : spine:114-116, spine:275, spine:530, AD-6, AD-17)
- AR-54 : Conteneur web / proxy — nginx et proxy Vite : `/api/{identifiant}/**` vers le service (chemin tel quel), sauf `/api/audit/**` ; tout autre chemin → `index.html` (repli SPA) ; ni code métier ni contrôle d'accès ; en-têtes de sécurité (CSP) et contrôle d'accessibilité automatique (axe) à fixer avec la story du conteneur web, outil vérifié avant usage (source : spine:268, spine:618, gm:40, AD-17)
- AR-55 : Secrets et compose — `make secrets` génère `.env` (mots de passe aléatoires des bases) et la paire RSA dans `.secrets/` (ignorés par git) ; `.env.example` versionné sans valeur ; interpolation `${NB_..._MOT_DE_PASSE:-}`, jamais `${VAR:?}` ; le service refuse de démarrer sans valeur ; preuves `git ls-files .env .secrets` vide et `trivy fs --scanners secret --exit-code 1 .` (source : spine:119, spine:122, AD-6)
- AR-56 : Makefile — cibles `secrets`, `reset-data` (appelle `scripts/reset-reference-data.sh`), `rejeu-up` (`compose.yaml` + `compose.rejeu.yaml`, puis `make reset-data`, attend que tout soit sain), `rejeu-golden-master` (appelle `scripts/rejeu-golden-master.sh`) (source : spine:116, spine:436, gm:153-157, AD-6, AD-11)
- AR-57 : Données de référence — `services/<identifiant>/reference-data/reference-data.sql` par service, identifiants du legacy, hors migrations (153 lignes `import.sql` : identite 7 ; coeur-bancaire 7+45+0+3+5+6+6 ; credits 2+18+4+4 ; documents 5 ; notifications 6 ; audit 0 ; bourse 9 ; open-banking-conseiller 13+4+4+5) ; dates relatives calculées depuis le port Horloge (source : spine:174, gm:135-149, AD-11)
- AR-58 : Projections de référence générées — le `reference-data.sql` d'un consommateur contient ses projections, générées depuis celui du producteur par `scripts/generer-projections-reference.sh` [ASSUMPTION : nom], jamais écrites à la main (source : spine:174, gm:155, AD-11)
- AR-59 : Design tokens — pas de Tailwind ; `npm run tokens` = `npx @google/design.md export --format css-tailwind` (0.4.0, alpha) puis `frontend/scripts/tokens-root.mjs` (`@theme` → `:root`) [ASSUMPTION] → `frontend/src/adapters/ui/theme/tokens.css`, généré jamais retouché ; écrans sur ces seules variables, éléments natifs stylés seulement par leur composant du design system ; `lang="fr"` ; rôles `alert`/`status`, `aria-*`, focus portés par les composants ; polices auto-hébergées (Inter en repli de Graphik [ASSUMPTION]) ; livré par la première story d'écran (connexion, FEAT-001) [ASSUMPTION : affectation] (source : spine:208-212, AD-13)
- AR-60 : Fournisseurs simulés — Powens et Azure OpenAI (open-banking-conseiller), AlphaVantage (bourse), chacun derrière un port sortant avec adaptateur réel et simulé dans `adapter.out.fournisseur` (nommage `<Techno><Port>`, ex. `PowensSimule`) ; simulé actif par défaut, réel par `nb.fournisseurs.<nom>.mode=reel` ; délai d'attente et coupe-circuit sur l'appel réel ; test de configuration (contexte sans `nb.fournisseurs.*` → bean simulé) ; livré avec la première story de chaque fournisseur (source : spine:309, spine:312, spine:379, AD-20)
- AR-61 : Comportement des simulateurs — Powens : liste fixe de connecteurs (dont « BNP Paribas », « Credit Agricole »), modes d'erreur (refus, 5xx, solde absent) pour FR-012-07, FR-012-12, FR-012-14, FR-014-04 ; AlphaVantage : séries fixes par symbole, dates depuis le port Horloge ; Azure OpenAI : réponse fixe + mode « refus d'authentification » 401/403 pour le test de FR-010-09 (BUG-038) (source : gm:173-177, ecarts:12, AD-20)
- AR-62 : État Powens et retour — état stocké en base `open-banking-conseiller` par client ; `state` signé à usage unique émis au lancement ; `GET /api/open-banking-conseiller/powens/retour` public, toujours 302 vers `/comptes-tiers` ; `state` invalide ou réutilisé → 302 sans effet ; remise à zéro FR-014-07 vide l'état [ASSUMPTION] ; historique du chat côté front, aucun historique serveur [ASSUMPTION] (source : spine:310-312, gm:76, AD-20)
- AR-63 : Traitements planifiés — déclencheurs dans `adapter.in.planification`, `@Scheduled` cron + zone `Europe/Paris` explicite, `@EnableScheduling` (observation automatique `tasks.scheduled.execution`) ; horaires : intérêts 02:00, fraude nocturne 02:30, relevés le 1er à 03:00, virements programmés toutes les 5 minutes, soldes faibles toutes les 30 minutes, rejet automatique toutes les minutes [ASSUMPTION] ; tous au cœur bancaire ; tous désactivés en profil `rejeu` (relais outbox excepté) ; aucun verrou distribué (source : spine:106, spine:319, spine:323, AD-5, AD-21)
- AR-64 : Garde et journal des traitements — une exécution au plus par jour calendaire Europe/Paris (garde par date, y compris changement d'heure) ; pas de rattrapage sauf virements programmés (FR-021-01) ; garde « mois déjà crédité » dans le domaine ; une ligne par exécution dans `BATCH_JOB_LOGS` (Job, Debut, Fin, Duree (ms), Enregistrements, Statut SUCCESS/FAILED/PARTIAL, Details), noms INTEREST_CALCULATION, FRAUD_DETECTION, MONTHLY_STATEMENTS, RECURRING_TRANSFERS, NOTIFICATION_CHECK, `SUSPENDED_TRANSFERS_EXPIRY` [ASSUMPTION] (ligne seulement si au moins un rejet ou échec, décision Q2) ; lecture `GET /api/<service>/traitements?limite=50` (FR-011-09 tout authentifié, FR-016-17 ADMIN), fusion par le front (source : spine:320-322, spine:324, AD-21)
- AR-65 : Notification décidée par le producteur — `donnees.notification` : `clientId`, `type` (CONFIRMATION, ALERTE, FRAUDE, ACCOUNT_BALANCE…), `titre`, `message` rendus ; plafonds dépendant d'un autre agrégat comptés par le producteur (20 par compte et par jour FR-022-04 ; une par échéance non exécutée FR-004-12) ; preuves : 48 analyses d'un compte à 50 € → 20 événements `solde-faible-detecte` ; 21 notifications → la plus ancienne supprimée (source : spine:371-372, AD-25)
- AR-66 : Contrôle de fraude (socle RG-027-003) — décision synchrone locale au cœur bancaire, même cas d'usage pour virement immédiat et échéance programmée (B2) ; port unique `HistoriqueVirements` ; `pg_advisory_xact_lock(clientId)` [ASSUMPTION] ; machine à 5 états EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE ; débit dans la transaction de soumission si EXÉCUTÉ, par le consommateur de `virement-a-traiter.v1` si LIBÉRÉ ; heure butoir = soumission + 24 h ; réponse `POST /api/coeur-bancaire/virements` avec `etat`, `heureButoir`, `reference`, écho des champs ; attribut `virement.controle.issue` sur l'observation courante ; tests `MachineEtatsVirement*Test`, `ControleFraude*Test`, `EcheanceRevue*Test`, `LiberationVirement*Test`, `AuditVirement*Test`, `ConfigurationFraude*Test` (source : spine:187-201, AD-12)
- AR-67 : MULTIPLE_FAILED_AUTH — identite-clients compte les échecs consécutifs par client existant (remise à zéro au succès) et publie `identite-clients.echecs-connexion-repetes.v1` au 5e échec puis à chaque suivant ; le cœur bancaire crée l'alerte OPEN (seul propriétaire de `FRAUD_ALERTS`) (source : spine:196, AD-12)
- AR-68 : Textes et traçabilité des tests — textes repris du legacy, textes nouveaux depuis textes-nouveaux.md, aucun texte inventé ; « [EN ATTENTE] » (BUG-009) et « transaction importante » dans un seul fichier de textes du cœur bancaire ; `@Tag("FR-xxx-yy")` et `@Tag("BUG-nnn")` en JUnit 5, `[FR-xxx-yy]`/`[BUG-nnn]` dans les titres Vitest (source : spine:200, spine:384, spine:388, AD-12, Conventions)
- AR-69 : Pièges de versions — TypeScript 5.9.3 (pas 7.x) ; paquet `react-router` 8.4.0 (plus `react-router-dom`) ; starters `spring-boot-starter-security-oauth2-resource-server` (ancien nom déprécié) ; Testcontainers 2.x artefacts `testcontainers-postgresql`/`testcontainers-kafka` ; Spring Boot 4.1.1 GA (pas 4.2.0-M2) ; `vitest` et `@vitest/coverage-v8` exactement 5.0.3 ; Trivy 0.75.0 (jamais 0.69.4 à 0.69.6) ; image Jaeger `cr.jaegertracing.io/...` ; Maven 3.9.16 ; surefire géré par Boot 3.5.6 ; Tailwind non retenu (source : versions:53-65, spine:150, spine:391-427)

#### Golden master

- AR-70 : Story « Socle du rejeu final du golden master » dans le dernier epic (pas l'epic 1) : livre `make reset-data`, `make rejeu-up`, `compose.rejeu.yaml`, l'installation de Playwright (`@playwright/test` 1.63.0) et les données de référence complètes des 8 services (source : spine:180, AD-11)
- AR-71 : Profil de rejeu — `compose.rejeu.yaml`, profil Spring `rejeu` sur les 8 services (preuve jq = `8`) ; Horloge réglée au démarrage sur `HORLOGE_DEPART` (défaut `2026-11-17T10:00:00` Europe/Paris) puis qui avance ; un seul `HORLOGE_DECALAGE` = date de référence − heure réelle, calculé par `make rejeu-up` et passé à tous les services ; horloge figée exclue (RAPID_SUCCESSION) ; traitements métier désactivés ; fournisseurs simulés ; JWT 60 min (source : spine:176, spine:324, gm:159-167, AD-11, AD-21)
- AR-72 : `make reset-data` idempotent : arrête les consommateurs Kafka, `TRUNCATE … RESTART IDENTITY CASCADE` des tables métier, projections, `outbox`, `outbox_audit`, `evenements_traites` ; supprime puis recrée les topics (offsets effacés) ; recharge les 8 `reference-data.sql` ; replace les séquences ; vide les caches ; relance les consommateurs ; ne touche pas l'horloge ; code 0 = 8 bases à l'état de référence ; preuve : trois `make reset-data` → comptages identiques (source : spine:175, spine:180, gm:155, gm:167, AD-11)
- AR-73 : Rejeu complet `scripts/rejeu-golden-master.sh` : `make reset-data` puis `npx playwright test --grep-invert "<écrivants>" --reporter=blob` ; pour chacun des 5 scénarios `writes: true` (SC-001-03, SC-001-05, SC-003-04, SC-004-02, SC-013-02) : `make reset-data` puis `--grep "<SC>"` ; `npx playwright merge-reports --reporter=html,json ./blob-report` ; critère : 31 réussis, 0 échec, 2 non rejoués (source : gm:224-230, AD-11)
- AR-74 : Périmètre : 33 scénarios, 2 exclus (SC-003-03 BUG-011, SC-010-02 BUG-038), 31 applicables (26 d'écran, 5 HTTP) ; dérivation à blanc `derive-golden-master.py` en code 0 avec `derived` 31, `dropped` 2, `rules.lost` = [RG-010-007] ; `export-golden-master-tests.py ... --check` en code 0 ; tests générés dans `e2e/golden-master`, jamais retouchés ; `replay-mapping.json` à la racine (source : spine:179-180, gm:25, gm:192, AD-11)
- AR-75 : Écarts voulus adaptés en `map` : SC-001-02 → `GET /api/coeur-bancaire/tableau-de-bord` sans jeton 401 ; SC-017-01 → `GET /api/documents/telechargement?type=RIB&compteId=1` sans jeton 401 ; SC-001-03 → `POST /api/identite-clients/connexion` 401 + `{"message":"Identifiants incorrects"}` ; SC-001-01 `GET /login` 200 ; SC-014-01 302 ; SC-010-02 exclu, couvert par le test de FR-010-09 (simulateur Azure 401/403) (source : ecarts:9-12, gm:76-80, AD-11, AD-17)
- AR-76 : URL de base unique `http://localhost:8080/` (surcharge `GM_BASE_URL`) ; 15 routes SPA définitives : `/login`, `/tableau-de-bord`, `/virement`, `/virements-programmes`, `/releve`, `/budget`, `/prets`, `/documents`, `/bourse`, `/mon-conseiller`, `/produits`, `/comptes-tiers`, `/beneficiaires`, `/espace-conseiller` (CONSEILLER, ADMIN), `/administration` (ADMIN) ; 2 points sans rendu (`powens/retour`, `telechargement`) ; `/` redirige vers `/login` ou l'écran d'accueil du rôle (source : gm:41, gm:46-77, AD-11)
- AR-77 : Connexion de test : `/login` avec vrai `<form>` soumis par Entrée ; `<input type="text" name="username" data-testid="login-identifiant">` premier champ texte, `<input type="password" name="password" data-testid="login-mot-de-passe">` seul de ce type, `<button type="submit" data-testid="login-me-connecter">Me connecter</button>` ; comptes `admin` → `/administration`, `sophie` → `/espace-conseiller`, `user1` → `/tableau-de-bord` ; `storage_state_dir: ".auth"` ; lien `nav-deconnexion` (SC-001-05) (source : gm:86-112, AD-11)
- AR-78 : `data-testid` figés après G2 : `{écran}-{élément}[-{identifiant métier}]`, minuscules ASCII sans accent, préfixes `login`, `nav`, `dashboard`, `virement`, `vp`, `releve`, `budget`, `pret`, `documents`, `bourse`, `chat`, `produits`, `tiers`, `beneficiaires`, `conseiller`, `admin`, `revue` ; messages `{écran}-message-erreur|succes|info` ; correspondance legacy → `data-testid` = tableau de EXPERIENCE.md ; sélecteurs écrits `[data-testid='…']` ; tout élément interactif en porte un (source : spine:170, spine:389, gm:114-121, AD-11)
- AR-79 : Règles de rendu pour le rejeu : textes identiques au legacy, casse rendue par `text-transform` (comparaison `innerText` en mode `contains`) ; `text_map` vide ; fenêtre de 5 000 caractères — toute section nouvelle après le contenu comparé (« Suivi de mes virements » en bas de `/virement` et `/tableau-de-bord`, file de revue après « Gestion des clients » sur `/administration`), seul le lien d'évitement « Aller au contenu principal » précède ; `<select>` natifs avec valeurs du legacy (`1`, `2`, `MENSUEL`) ; texte des boutons dans `<button>`/`<a>`, jamais `value` d'`<input type="submit">` ; saisies `3.5`, `2027-01-01`, IBAN sans espace (source : spine:171-173, gm:119-129, AD-11)
- AR-80 : Données exigées par le golden master : soldes et 5 dernières opérations du COURANT de `user1`, bénéficiaire `FR76***` (SC-013-01), portefeuille de `sophie` (Jean Dupont / `user1`, Marie Lebrun / `user2`, SC-015-01), watchlist `BNP.PA` et `AI.PA` (SC-009-01), « CEL - Compte Epargne Logement » et « Livret A » (SC-011-01), compte interne `FR76-0001-0002` (SC-004-02), comptes `1` et `2`, RIB du compte `1` (source : gm:135, gm:149)
- AR-81 : Déterminisme SC-003-04 : `make rejeu-up` puis trois fois `make reset-data && (cd e2e/golden-master && npx playwright test --grep "SC-003-04")` verts ; contre-épreuve `HORLOGE_DEPART=2026-11-17T23:30:00 make rejeu-up` qui fait échouer SC-003-04 (UNUSUAL_HOURS) ; notification CONFIRMATION via Kafka sous 5 s (source : spine:180, gm:179, gm:204-222, AD-11, AD-18)
- AR-82 : Critère de la story socle (point ouvert §8.9) : vérifier que `2026-11-17T10:00:00` est postérieure à toutes les dates d'`import.sql` et que `user1` a au plus 8 opérations ce jour-là, avant la première exécution de SC-003-04 (source : gm:163, gm:257, spine:628, AD-11)
- AR-83 : Critère de la story socle (point ouvert §8.10) : remise à zéro avant chaque scénario qui écrit — tenue par `scripts/rejeu-golden-master.sh` faute de point d'accroche dans les tests générés ; option de l'outil d'export (reset avant les `writes: true`) pour un lancement unique, à évaluer (source : gm:258, AD-11)
- AR-84 : Critère de la story socle (timezone/locale) : navigateur Playwright `timezoneId: 'Europe/Paris'` et `locale: 'fr-FR'` ; `playwright.config.ts` généré (protégé) ne les porte pas : option de `export-golden-master-tests.py`, sinon `TZ=Europe/Paris` et `LANG=fr_FR.UTF-8` posés par `scripts/rejeu-golden-master.sh` (source : spine:177, spine:627, gm:164, AD-11, AD-23)

#### Capability → Architecture Map

- AR-85 : FEAT-001 Connexion → identite-clients, front `/login` (AD-16, AD-17 adresse IP, AD-11, AD-18 audit) ; FEAT-002 Tableau de bord → front `/tableau-de-bord` + coeur-bancaire, notifications, identite-clients, open-banking-conseiller (patrimoine total) (AD-17, AD-11, AD-25) (source : spine:561-562)
- AR-86 : FEAT-003 Virement → coeur-bancaire, `/virement` (AD-12, AD-18, AD-19, AD-24, AD-11) ; FEAT-004 Virements programmés → coeur-bancaire, `/virements-programmes` (AD-12 B2, AD-21, AD-19, AD-23, AD-25) ; FEAT-005 Relevé → coeur-bancaire, `/releve` ; FEAT-006 Budget → coeur-bancaire, `/budget` (AD-14, AD-23, Conventions) (source : spine:563-566)
- AR-87 : FEAT-007 Prêts → credits, `/prets` (AD-14, AD-18, AD-24) ; FEAT-008 Documents → documents, `/documents` (AD-14 seul producteur de PDF, AD-22 projections comptes/opérations/clients, AD-16 403/404, AD-23) ; FEAT-017 Téléchargement PDF → documents `/api/documents/telechargement` (AD-14, AD-16, AD-17 401) (source : spine:567-568, spine:577)
- AR-88 : FEAT-009 Bourse → bourse, `/bourse` (AD-20, AD-14) ; FEAT-029 AlphaVantage → bourse (AD-20) ; FEAT-010 Chat conseiller → open-banking-conseiller, `/mon-conseiller`, historique front (AD-20) ; FEAT-011 Catalogue produits → open-banking-conseiller, `/produits`, journal des traitements lu au cœur bancaire (AD-14, AD-21) ; FEAT-012 Comptes tiers Powens → open-banking-conseiller, `/comptes-tiers` (AD-20) ; FEAT-014 Callback Powens → `/api/open-banking-conseiller/powens/retour` (AD-17 exception 302, AD-20 `state` signé) ; FEAT-028 Powens → open-banking-conseiller (AD-20) (source : spine:569-574, spine:588-589)
- AR-89 : FEAT-013 Bénéficiaires externes → coeur-bancaire, `/beneficiaires` (AD-14, AD-17 IBAN, Conventions journaux) ; FEAT-015 Espace conseiller → identite-clients, coeur-bancaire, credits, `/espace-conseiller` (AD-16 portefeuille côté serveur, AD-22, AD-17) ; FEAT-016 Administration → identite-clients, credits, coeur-bancaire (file de revue, journal), `/administration` (AD-12, AD-16, AD-17, AD-21, AD-24) (source : spine:573, spine:575-576)
- AR-90 : Traitements : FEAT-018 Intérêts nocturnes, FEAT-019 Fraude nocturne, FEAT-020 Relevés mensuels (journal seul), FEAT-021 Exécution des virements programmés, FEAT-022 Soldes faibles (→ notifications) — tous coeur-bancaire (AD-21, AD-19, AD-12, AD-14, AD-22, AD-25) ; FEAT-023 Traitement asynchrone des virements → coeur-bancaire `virement-a-traiter.v1` (AD-12, AD-18) ; FEAT-024 Traitement asynchrone des prêts → credits `pret-demande.v1` (AD-18, AD-24) (source : spine:578-584)
- AR-91 : FEAT-025 Notifications internes → notifications (AD-25, AD-22, AD-18) ; FEAT-026 Audit trail → audit, aucune API de lecture (AD-18, AD-17, AD-14) ; FEAT-027 Fraude temps réel → coeur-bancaire + identite-clients (MULTIPLE_FAILED_AUTH) (AD-12, AD-5, AD-19, AD-22) ; FEAT-030 RBAC → tous les services + routeur du front (AD-16) (source : spine:585-590)
- AR-92 : Transverses : contrôles qualité NFR-QG-00 à 10 → `scripts/gates/`, POM parent, `frontend/` (AD-1, AD-2, AD-5, AD-7, AD-8, AD-9, AD-10, AD-15) ; parité NFR-PAR-01 à 05 → `e2e/golden-master/`, `Makefile`, `reference-data/`, traçabilité des tests (AD-11, AD-19, AD-23) ; performance NFR-PERF-01 → front (appels parallèles), bourse (cache, 3 000 ms), traces (AD-17, AD-20, AD-5) (source : spine:591-593, spine:306)

#### Dépendances d'ordre

- AR-93 : Epic 1 dans l'ordre strict 1.1 → 1.2 → 1.3 → 1.4 → 1.5 → 1.6 ; aucune story fonctionnelle avant que tous les contrôles bloquants soient livrés et verts (source : spine:242, AD-15)
- AR-94 : Epic 1 terminé → l'humain fige `.bmad-loop/gates.sha256` (étape 10 du pilote) et installe le hook (étape 11) → epic 2 ; les deux fichiers protégés créés par des stories (`.dependency-cruiser.cjs` en 1.2, `trace-smoke.sh` en 1.4) doivent l'être avant l'empreinte (source : spine:160-162, AD-10)
- AR-95 : `vite.config.ts` : créé en 1.1, seuils ajoutés en 1.3, proxy ajouté en 1.4 (source : spine:131, AD-7)
- AR-96 : Story de connexion (FEAT-001) avant toute story d'un autre service exposant une API protégée : elle livre la vérification JWT d'identité, le format d'erreur, l'adresse IP, l'audit (LOGIN, LOGIN_FAILED, LOGOUT) ; chaque service suivant pose sa vérification du jeton avec sa première story ; elle porte aussi le thème `npm run tokens` (première story d'écran) [ASSUMPTION] (source : spine:212, spine:261, spine:276, spine:290, AD-13, AD-16, AD-17, AD-18)
- AR-97 : Le producteur d'un topic (schéma + test producteur) précède les stories consommatrices ; un topic ou champ nouveau s'ajoute au catalogue AD-22 avant la story qui le produit ; donc `identite-clients.client-modifie.v1` avant les projections clients de coeur-bancaire, credits, documents (et avant tout contrôle CONSEILLER par portefeuille), `compte-modifie.v1`/`operation-enregistree.v1` avant documents (source : spine:255, spine:331, spine:351, AD-16, AD-22)
- AR-98 : Socle RG-027-003 (contrôle de fraude AD-12, epic fonctionnelle des virements) → story du cumul glissant FR-027-18 juste après (lève R-1, D10) → reprise préremplie FR-003-28 (D12) ; FR-003-29 (correction BUG-009, R-4) après la démonstration (source : spine:201, spine:599, spine:602, spine:617, AD-12)
- AR-99 : Choix de la bibliothèque de coupe-circuit/cache des fournisseurs avant la première story de fournisseur ; chaque fournisseur livre port + simulateur avec sa première story (source : spine:312, spine:626, AD-20)
- AR-100 : Story « Socle du rejeu final du golden master » dans le dernier epic, après que les 8 services et les 15 routes existent (elle charge les 8 `reference-data.sql` et rejoue 31 scénarios) (source : spine:180, AD-11)
- AR-101 : Première story de documents choisit la bibliothèque PDF avant tout PDF ; story du conteneur web fixe CSP et contrôle d'accessibilité (axe) (source : spine:618-619)

#### Questions ouvertes de la spine

- AR-102 : [OPEN] Coupe-circuit et cache des fournisseurs (AD-20, NFR-INT-02) : bibliothèque commune à choisir et vérifier (compatibilité Boot 4.1.1) dans `versions-verifiees.md`, réglages sous `nb.fournisseurs.<nom>.*` → à régler avant ou par la première story de fournisseur (source : spine:626, AD-20)
- AR-103 : [OPEN] Configuration Playwright générée (`timezoneId`, `locale` absents de `playwright.config.ts`) → story « Socle du rejeu final du golden master » (source : spine:627, AD-11, AD-23)
- AR-104 : [OPEN] Date de référence du rejeu `2026-11-17T10:00:00` (postérieure à `import.sql`, `user1` ≤ 8 opérations ce jour-là) → story « Socle du rejeu final du golden master » (source : spine:628, AD-11)
- AR-105 : [OPEN] Observation Micrometer et « span créé à la main » : confirmer que l'enrichissement de l'observation courante est admis et la création d'une Observation interdite → story du socle RG-027-003 (contrôle de fraude) (source : spine:629, AD-12)
- AR-106 : [TRANCHÉ 2026-10-05, D10 : mécanisme de la spine confirmé] Inactivité et renouvellement du jeton : mécanisme d'expiration sur inactivité à confirmer par le métier → story de connexion / renouvellement du jeton (FEAT-001, FR-030-05) (source : spine:630, spine:257, AD-16)
- AR-107 : [TRANCHÉ 2026-10-05, D11 : Inter seule] Police Graphik : licence d'auto-hébergement à confirmer, sinon Inter seule → première story d'écran (connexion, FEAT-001) qui livre le thème (source : spine:631, AD-13)

### Contraintes du PRD (périmètre, décisions, suppositions, addendum)

#### Source : `_bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/prd.md` (l. 2734–3063) et `addendum.md`.


#### Décisions qui contraignent le découpage

##### Périmètre (§6, §5)
- Contrôles d'abord : contrôles de `docs/quality-gates.md` livrés et verts avant toute story fonctionnelle (NFR-QG-00, epic 1).
- Ordre des stories RG-027-003 (contrôle de fraude d'un virement) : 1) socle (contrôle synchrone avant validation, suspension avant débit, refus sur erreur, états, revue admin, échéance et rejet automatique, écran de refus — D1 à D9) ; 2) cumul glissant FR-027-18 (D10), story qui suit immédiatement le socle ; 3) reprise préremplie après refus FR-003-28 (D12).
- FR-003-29 (reformulation des textes « transaction importante », B1 révisée) : story livrée après la démonstration et après les stories RG-027-003 ; d'ici là les textes legacy de FR-003-10 s'appliquent.
- BUG-009 (virement 5 000–10 000 € débité mais annoncé « en attente ») : conservé provisoirement pour la démonstration, corrigé par FR-003-29 (risque R-4).
- 50 bugs à corriger : chacun couvert par un test (NFR-PAR-02, SM-3 50/50).
- Évolutions incluses en plus de la parité FEAT-001 à FEAT-030 : signal d'échecs de connexion répétés (FR-027-11), bouton « Rafraîchir » des comptes tiers (A-012-5), exemption d'authentification par chemin exact (A-030-1).
- Adaptateurs simulés des trois fournisseurs externes ; golden master dérivé et rejeu final unique.
- Hors périmètre (§5) : FEAT-031 (analytique client, code mort) et FEAT-032 (bean de notifications d'interface, code mort) non refaites ; aucune migration de données ; aucune notification hors application ; pas d'engagement réglementaire de production ; local uniquement (`compose.yaml`) ; bugs « ne pas corriger » ni exigences ni tests.

##### Décisions RG-027-003 (D1–D12)
- D1 : virement suspect suspendu avant tout débit, en attente de revue ; sinon exécuté — FR-027-02, FR-003-09.
- D2 : suspension si score cumulé strictement > 75 OU montant ≥ 10 000 € ; seuils en configuration validée au démarrage — FR-027-02, FR-023-04, NFR-SEC-08.
- D3 : erreur du contrôle = refus pur quel que soit le montant, rien débité, erreur journalisée — FR-027-02, FR-003-27.
- D4 : pas de délai métier (calcul local) ; expiration technique de l'accès aux données = erreur, valeur fixée en architecture — NFR-SEC-09.
- D5 : décision synchrone et locale ; cinq états du virement — FR-027-16, FR-003-13, FR-023-01.
- D6 : l'administrateur seul libère ou rejette, motif obligatoire, identité tracée ; pas de rôle conseiller — FR-016-19.
- D7 : revue sous 24 heures calendaires depuis la soumission ; heure butoir précise affichée au client — FR-027-04, FR-027-17.
- D8 : à l'expiration, rejet automatique sans débit — FR-027-17.
- D9 : texte minimal de l'écran de refus fixé ; référence affichée = identifiant de trace — FR-003-27, NFR-OBS-02.
- D10 : cumul glissant (24 h, tous bénéficiaires, plafond 10 000 €) dans le projet, story qui suit immédiatement le socle — FR-027-18.
- D11 : risque de découpage ouvert jusqu'à D10 — risque R-1.
- D12 : reprise préremplie après refus, story après le socle et le cumul — FR-003-28.

##### Décisions de finalisation (B1–B7)
- B1 (révisée) : BUG-009 conservé pour la démo, textes PENDING et ALERTE legacy gardés ; PENDING toujours dès 5 000 €, virement exécuté et débité ; reformulation = story après la démo — FR-003-10, FR-003-29, FR-003-09, FR-002-10, FR-003-14, FR-023-03, FR-025-01, FR-005-07.
- B2 : virement programmé soumis au même contrôle et à la même règle de suspension à chaque échéance — FR-021-10, FR-027-02.
- B3 : solde faible, au plus 20 alertes par compte et par jour (BUG-071), une seule règle pour FEAT-022 et FEAT-025, type ACCOUNT_BALANCE — FR-022-04, FR-025-08.
- B4 : libérer ou rejeter un virement suspendu passe ses alertes de fraude à REVIEWED dans la même action — FR-016-19, FR-016-15, FR-027-13, FR-027-14.
- B5 : l'heure butoir fait foi ; libération acceptée strictement avant, refusée et virement rejeté à l'heure butoir ou après, traitement planifié passé ou non — FR-027-17, FR-016-19.
- B6 : libération sur solde insuffisant refusée, virement REJETÉ sans débit ; solde contrôlé à l'instant de la libération, sinon LIBÉRÉ puis débit asynchrone et EXÉCUTÉ — FR-016-19, FR-027-16, FR-023-01, FR-026-02.
- B7 : solde devenu insuffisant entre libération et débit asynchrone : LIBÉRÉ → REJETÉ sans débit, audit TRANSFER_REJECT_INSUFFICIENT_FUNDS acteur SYSTEM, client voit « rejeté, non débité » sans notification, admin voit le motif dans l'historique — FR-027-16, FR-023-01, FR-026-03, FR-026-05, FR-016-19, FR-002-10.

##### Risques connus
- R-1 (découpage d'un virement sous les seuils) : accepté pour la durée du socle seulement, levé par FR-027-18 (D11).
- R-2 (rejet automatique la nuit et le week-end sans revue humaine) : accepté pour le démonstrateur (D8).
- R-3 (montants non masqués dans les journaux) : accepté pour le démonstrateur (NFR-SEC-05).
- R-4 (BUG-009 conservé provisoirement) : corrigé par FR-003-29 après la démonstration (B1 révisée).

##### Questions ouvertes restantes (reports)
- RP-01 (journal des traitements saturé, 48 entrées par jour du contrôle des soldes faibles contre 50 affichées : choisir pagination, filtre ou nombre et ajouter un critère à FR-016-17) — propriétaire : PM — reprise : avant la story de FR-016-17.
- RP-02 (reclassement possible en risque élevé de A-027-7, A-003-3, A-021-5, A-019-1, A-027-10) — propriétaire : PM — reprise : avant la story du socle RG-027-003.
- RP-03 (signaux de fraude décrits deux fois, FR-003-17 à 25 et FR-027-03, 05 à 10 : règle complète dans FEAT-027, renvoi dans FEAT-003) — propriétaire : PM — reprise : au découpage en stories du contrôle de fraude ; d'ici là, une story qui touche un signal modifie les deux FR.
- RP-04 (volumétrie de `import.sql` dans l'addendum A.8 sans exigence hors NFR-PAR-04) — propriétaire : PM — reprise : à la prochaine révision du PRD.
- RP-05 (NFR bancaires absentes : accessibilité, navigateurs, mobile, verrouillage du compte après échecs, politique de mot de passe) — propriétaire : PM — reprise : avant la story d'authentification (FEAT-001) pour verrouillage et mot de passe ; hors démonstrateur pour le reste.
- RP-06 (état DEFAUT des prêts sans transition, impayés hors périmètre — QO-2) — propriétaire : métier — reprise : hors démonstrateur.
- RP-07 (méthodes de catalogue par type sans appelant — QO-2) — propriétaire : métier — reprise : si un écran en a besoin, hors démonstrateur.
- RP-08 (point d'entrée `evaluateSingleScore`, BUG-087 conservé — QO-2) — propriétaire : architecte — reprise : à l'architecture du contrôle de fraude, sans point d'entrée public d'évaluation isolée.
- RP-09 (surcharge des seuils de fraude, d'alerte et de mise en attente en production — QO-2) — propriétaire : métier — reprise : hors démonstrateur.
- RP-10 (valeur de production de `POWENS_CONNECTOR_ID` — QO-3) — propriétaire : équipe de dev — reprise : au premier branchement réel de Powens, hors démonstrateur.


#### Suppositions à reprendre avant une story

- A-018-1 : intérêts mensuels crédités à l'exécution de 2 h du 1er du mois, rien les autres nuits (BUG-064) — reprise : avant la story FEAT-018.
- A-027-2 : MULTIPLE_FAILED_AUTH évalué à la connexion au 5e échec consécutif, hors score du virement — reprise : avant la story de FR-027-11.
- A-030-1 : écrans publics exemptés d'authentification sur leur chemin exact seulement — reprise : à l'architecture (filtrage des requêtes), avant la story FEAT-030.
- A-021-3 : virement programmé refusé pour compte destinataire introuvable désactivé — reprise : avant la story FEAT-021.
- A-014-1 : retour Powens avec seul `code` traité comme incomplet — reprise : avant la story FEAT-014, ou au premier branchement réel de Powens.
- A-021-6 : pendant la suspension d'une échéance programmée, la prochaine date avance, le compteur ne change qu'après libération, échéance rejetée non rattrapée — reprise : avant la story de FR-021-10.
- A-021-7 : échéance exécutée sans notification, seule une échéance suspendue notifie (FRAUDE), échéance refusée pour erreur retentée au passage suivant sans notification — reprise : avant la story de FR-021-10.
- A-016-6 : à l'heure butoir ou après, un rejet admin est sans objet, message « Heure butoir dépassée : le virement est rejeté. » — reprise : avant la story de FR-016-19.
- A-012-1 : texte du retour Powens en erreur — reprise : avant la story FEAT-012, au plus tard à l'étape G2.
- A-012-4 : message après échec de chargement des comptes — reprise : avant la story FEAT-012, au plus tard à l'étape G2.
- A-016-1 : texte d'erreur de traitement d'un prêt — reprise : avant la story FEAT-016, au plus tard à l'étape G2.
- A-021-1 : texte de refus d'un virement programmé, compte introuvable — reprise : avant la story FEAT-021, au plus tard à l'étape G2.
- A-023-1 : texte d'échec de remise d'un virement — reprise : avant la story FEAT-023, au plus tard à l'étape G2.
- A-024-1 : texte service de crédit indisponible — reprise : avant la story FEAT-024, au plus tard à l'étape G2.
- A-004-3 : notification de solde insuffisant d'un virement programmé — reprise : avant la story FEAT-004, au plus tard à l'étape G2.
- A-004-4 : texte « Compte destinataire introuvable » — reprise : avant la story FEAT-004, au plus tard à l'étape G2.
- A-006-1 : erreur d'enregistrement ou de suppression d'un plafond de budget — reprise : avant la story FEAT-006, au plus tard à l'étape G2.
- A-016-4 : auteur de la revue d'une alerte enregistré par identifiant, affiché par nom complet — reprise : avant la story de FR-016-15.
- A-016-5 : à la libération ou au rejet, seules les alertes OPEN passent à REVIEWED — reprise : avant la story de FR-016-19.
- A-026-1 : noms et détails des codes d'audit des virements (dont TRANSFER_REJECT_INSUFFICIENT_FUNDS acteur SYSTEM), échéances programmées auditées au nom du titulaire sans IP — reprise : à l'architecture (codes d'audit), avant la story FEAT-026.
- A-010-3 : actions rapides du chat masquées pour la conversation en cours, réaffichées par « Nouvelle conversation » — reprise : avant la story FEAT-010.
- A-027-4 : texte de l'avis de suspension (notification FRAUDE), orthographe correcte, remplace les textes legacy — reprise : avant la story du socle RG-027-003, au plus tard à l'étape G2.
- A-027-10 : le client voit l'état de ses virements suspendus, libérés ou rejetés sur l'écran de virement et le tableau de bord — reprise : avant la story du socle RG-027-003.
- A-027-7 : le cumul glissant compte les virements exécutés et suspendus, pas les rejetés ni refusés — reprise : avant la story de FR-027-18.
- A-025-3 : le plafond de notifications s'applique à l'avis de suspension, état et heure butoir restent visibles — reprise : avant la story du socle RG-027-003.
- A-027-11 : HIGH_FREQUENCY compte les opérations au relevé, débits et crédits confondus — reprise : avant la story du contrôle de fraude (signaux).
- A-001-1 : compte désactivé refusé avec le message générique, audit LOGIN_FAILED — reprise : avant la story FEAT-001.
- A-008-1, A-017-1 : document d'un autre client refusé en HTTP 403 sans message métier — reprise : avant les stories FEAT-008 et FEAT-017.
- A-008-2 : compteur de téléchargements implémenté (absent du legacy) — reprise : avant la story FEAT-008.
- A-017-2 : compte inexistant = 404 et non 500 du legacy, écart attendu au golden master — reprise : avant la story FEAT-017, au plus tard à l'étape G2.
- A-010-1 : « erreur d'authentification » du service d'IA couvre 401 et 403 — reprise : avant la story FEAT-010.
- A-011-1 : message « aucun produit » par type de produit vide — reprise : avant la story FEAT-011.
- A-012-5 : bouton « Rafraichir » exposé (absent du legacy) — reprise : avant la story FEAT-012.
- A-016-2 : « Client depuis » = date de création de la fiche client — reprise : avant la story FEAT-016.
- A-018-2 : une relance dans le même mois ne recrédite pas d'intérêts — reprise : avant la story FEAT-018.
- A-021-4 : virement programmé en retard de plusieurs périodes exécuté une fois par passage, rattrapage aux passages suivants — reprise : avant la story FEAT-021.
- A-024-2 : demande de prêt non transmise annulée, absente de la liste — reprise : avant la story FEAT-024.
- A-025-1 : notification de solde faible type ACCOUNT_BALANCE, priorité HIGH, icône ⚠️, comptée dans le plafond — reprise : avant la story de FR-025-08.
- A-004-2 : une seule notification par échéance programmée non exécutée — reprise : avant la story FEAT-004.
- A-027-1 : signal d'heure inhabituelle au fuseau de l'application, pas du client — reprise : avant la story du contrôle de fraude (signaux).
- A-029-2 : symbole ou nom trop long bloqué par la limite du champ, sans message — reprise : avant la story FEAT-029.
- A-002-3 : client sans compte COURANT voit un tableau d'opérations vide — reprise : avant la story FEAT-002.
- A-003-1, A-003-2 : messages non techniques pour montant invalide et solde insuffisant — reprise : avant la story FEAT-003.
- A-003-3 : contrôles de saisie et de solde avant le contrôle de fraude — reprise : avant la story du socle RG-027-003.
- A-003-4 : un seul signal LARGE_AMOUNT, score 40 de 5 000 € inclus à 15 000 € exclus, 80 à partir de 15 000 € — reprise : avant la story du contrôle de fraude (signaux).
- A-003-5 : RAPID_SUCCESSION compare au précédent virement — reprise : avant la story du contrôle de fraude (signaux).
- A-003-9 : VELOCITY_CHECK compte les virements exécutés du client — reprise : avant la story du contrôle de fraude (signaux).
- A-004-1, A-013-1 : messages de refus génériques sur la ressource d'un autre client — reprise : avant les stories FEAT-004 et FEAT-013.
- A-004-5 : la correction de BUG-069 s'applique aussi à RG-004-064 — reprise : avant la story FEAT-004.
- A-007-1 : durée de 301 à 360 mois refusée avec « Duree invalide » — reprise : avant la story FEAT-007.
- A-010-2 : champ du chat non vidé après l'envoi, comme le legacy — reprise : avant la story FEAT-010.
- A-012-2, A-012-3 : « Solde indisponible » et message d'indisponibilité de Powens — reprise : avant la story FEAT-012, au plus tard à l'étape G2.
- A-021-2 : refus « compte introuvable » avant le report pour solde insuffisant — reprise : avant la story FEAT-021.
- A-022-1 : plafond de 20 alertes de solde faible par jour calendaire, heure de Paris — reprise : avant la story FEAT-022.
- A-003-8 : virement refusé pour erreur du contrôle sans notification, seul l'écran de refus l'annonce — reprise : avant la story du socle RG-027-003.
- A-027-9 : rejet automatique tracé au journal d'audit — reprise : avant la story de FR-027-17.
- A-003-11 : reprise préremplie = compte source, compte destinataire, montant et motif — reprise : avant la story de FR-003-28.
- A-027-12 : échecs de connexion d'un compte désactivé comptent pour MULTIPLE_FAILED_AUTH — reprise : avant la story de FR-027-11.
- A-016-3 : file de revue triée par heure butoir, colonnes proposées en table ronde — reprise : avant la story de FR-016-19.
- A-016-7 : historique des virements traités sous la file (libérés ou rejetés, état et motif), affiché s'il n'est pas vide ; profondeur, titre et colonnes non fixés — reprise : avant la story de FR-016-19.


#### Addendum

- A.1 : architecture imposée, `docs/quality-gates.md` fait foi en cas d'écart.
- A.1 : microservices Java 21, Spring Boot 4, Maven ; un module par service sous `services/<nom>/`, POM parent à la racine.
- A.1 : chaque service hexagonal, packages `domain`, `application`, `adapter.in`, `adapter.out`, `infrastructure`.
- A.1 : `domain` et `application` en Java pur (seulement `java.*` et le code du service) : aucun import Spring, Jakarta, Jackson ni autre bibliothèque, aucune dépendance vers `adapter` ou `infrastructure`.
- A.1 : une base PostgreSQL par service, aucun service ne lit la base d'un autre.
- A.1 : bus entre services Kafka.
- A.1 : front React + TypeScript sous `frontend/`, hexagonal : `src/domain`, `src/application`, `src/adapters`, `src/infrastructure`.
- A.1 : `src/domain` et `src/application` en TypeScript pur, aucun import de paquet npm hors fichiers de test ; tout React dans `src/adapters`.
- A.1 : tracing `spring-boot-starter-opentelemetry`, export OTLP vers un collecteur OpenTelemetry, backend Jaeger v2.
- A.1 : aucun span créé à la main ; spans JDBC via datasource-micrometer 2.x.
- A.1 : contexte propagé sur HTTP et Kafka (observation activée sur KafkaTemplate et les listeners) ; échantillonnage 1.0 en local.
- A.1 : `compose.yaml` à la racine construit les images et lance services, PostgreSQL, Kafka, collecteur et Jaeger.
- A.1 : EJB et JMS (ActiveMQ Artemis) remplacés par Kafka pour les traitements asynchrones des virements et de l'approbation des prêts (FEAT-023, FEAT-024).
- A.1 : timers `@Schedule` remplacés par des traitements planifiés des services (NFR-BATCH-01).
- A.1 : le front passe par une API interne (A-G-05).
- A.2 : Azure OpenAI (FEAT-010) derrière un port chat conseiller, adaptateur simulé déterministe par défaut, vrai appel en option (endpoint, deployment, clé).
- A.2 : Powens (FEAT-012, FEAT-014, FEAT-028) derrière un port open banking, adaptateur simulé (webview, callback, comptes) par défaut, vrai appel en option (domain, client_id, secret, connector_id).
- A.2 : AlphaVantage (FEAT-009, FEAT-029) derrière un port cotations, adaptateur simulé (séries fixes) par défaut, vrai appel en option (clé) avec cache.
- A.2 : aucun test ni contrôle ne dépend du réseau ni d'une clé externe ; vrais appels avec délai d'attente et coupe-circuit (NFR-INT-02).
- A.2 : les adaptateurs simulés servent au rejeu du golden master des fonctionnalités à fournisseur externe (A-G-09, décision en G2).
- A.3 : `SEUIL_FRAUDE_EUR` = 10 000, `SEUIL_ALERTE_EUR` = 5 000, `SEUIL_PENDING_EUR` = 5 000, seuil de score 75, plafond du cumul glissant 10 000 € : paramètres de configuration de déploiement, non modifiables depuis l'interface, validés au démarrage (NFR-SEC-08).
- A.4 : bugs « ne pas corriger » ni exigences ni tests, listés dans `docs/retro/00-global/LEGACY-BUGS.md`.
- A.5 : contrat de dérivation `docs/retro/golden-master-contract.md` ; décisions par scénario prises en `bmad-architecture` avec `corpus-bmad-golden-master` (étape G2).
- A.5 : décisions globales à prendre en architecture : URL de base et mode d'authentification, sortie qui fait foi pour une fonctionnalité d'interface, table des routes, sessions de test par rôle, jeu de données de référence et son chargement (NFR-PAR-04).
- A.5 : rejeu une seule fois, développement terminé (NFR-PAR-01).
- A.5 : rejeu déterministe (NFR-PAR-05) : port Horloge dans le domaine, profil de rejeu avec horloge réglée au démarrage sur une heure fixe de journée (pas figée), commande de remise à zéro avant le rejeu et avant chaque scénario qui écrit, fournis par l'application et l'orchestration ; sinon SC-003-04 en écart permanent.
- A.7 : décision de fraude synchrone et locale au service de virement ; score calculé dans le domaine derrière un port, adaptateur simulé, sans appel réseau ni délai métier.
- A.7 : machine d'états du virement EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE (FR-027-16).
- A.7 : événements publiés sur Kafka après la décision via outbox, vers notifications et file de revue ; l'unique notification d'un virement vient de l'événement de décision du contrôle, pas du traitement asynchrone (FR-002-10, FR-023-03).
- A.7 : remise d'un virement exécuté au traitement asynchrone enregistrée dans la même écriture que sa validation ; son échec fait échouer la validation (FR-023-02, A-023-3).
- A.7 : commandes Libérer et Rejeter réservées à l'administrateur (FR-016-19) ; libération contrôle le solde à l'instant (insuffisant : REJETÉ sans débit, B6) ; sinon LIBÉRÉ puis débit asynchrone et EXÉCUTÉ ; solde insuffisant au débit : LIBÉRÉ → REJETÉ, TRANSFER_REJECT_INSUFFICIENT_FUNDS, acteur SYSTEM (B7) ; alertes ouvertes passées à REVIEWED dans la même action (B4).
- A.7 : traitement planifié du rejet automatique à 24 h (FR-027-17), testé avec horloge injectée ; l'heure butoir fait foi, Libérer refuse à l'heure butoir ou après ; concurrence libération/expiration tranchée par l'horloge (B5).
- A.7 : chaque échéance de virement programmé appelle le même cas d'usage de contrôle que le virement immédiat (FR-021-10, B2).
- A.7 : à fixer dans la décision d'architecture : valeur de l'expiration technique d'accès aux données d'historique (NFR-SEC-09) et moyen d'observer le contrôle dans la trace sans span manuel (NFR-OBS-02).
- A.7 : golden master, un seul scénario divergent prévu (SC-003-04) sous condition ; traitement en G2 par exception documentée (rejeu entre 06:00 et 23:00, base remise à zéro, aucun virement dans les 30 s précédentes) ; autres écarts découlant de D1–D12 via G2, pas des régressions.
- A.8 : horaires legacy repris (INTEREST_CALCULATION 02h00, FRAUD_DETECTION 02h30, MONTHLY_STATEMENTS le 1er à 03h00, RECURRING_TRANSFERS toutes les 5 min, NOTIFICATION_CHECK toutes les 30 min) ; volumétrie et temps legacy = références mesurées, pas des cibles.

### UX Design Requirements

Sources : `_bmad-output/planning-artifacts/ux-designs/ux-next-banking-webapp-2026-10-05/` (DESIGN.md + EXPERIENCE.md = contrat UX unique ; textes-nouveaux.md ; divergences-golden-master.md ; SYNTHESE.md). Les compléments marqués « AD-13 » viennent de `architecture/.../ARCHITECTURE-SPINE.md` (thème du front), cités parce que la tâche les demande et que DESIGN.md/SYNTHESE.md y renvoient (export `design.md`).

#### Design tokens et thème

- UX-DR1 : Reprendre tels quels les tokens de couleur du design system « Purple Consulting » dans DESIGN.md (primary #A100FF, primary-hover #7500C0, primary-active #4E007F, primary-container, on-primary-container, accent-tint, ink, surface, surface-alt, surface-inverse, on-surface-inverse, text-secondary, border, border-strong, disabled-bg/-text, success/-container, warning/-container, error/-container, info/-container) ; aucune couleur nouvelle, aucun dégradé ; violet réservé à l'action principale et à la navigation, jamais pour une erreur ou un avertissement. Test : aucune valeur de couleur hors tokens dans le CSS des écrans. (source : DESIGN.md:frontmatter colors, Colors, Do's and Don'ts)
- UX-DR2 : Typographie : échelle display, h1–h4, body-lg/md/sm, label, button, eyebrow (capitales, letter-spacing 0.08em), caption ; famille Graphik avec repli Inter, Arial, sans-serif ; taille minimale 12px. (source : DESIGN.md:frontmatter typography, Typography ; SYNTHESE.md:§3.1)
- UX-DR3 : Chiffres tabulaires (`font-variant-numeric: tabular-nums`) pour tous les montants et colonnes numériques. (source : DESIGN.md:Typography, components.amount)
- UX-DR4 : Tokens d'arrondis (none 0, sm 2px, md 4px, pill 999px) : angles vifs partout, pilule réservée aux tags et badges d'état. (source : DESIGN.md:rounded, Shapes)
- UX-DR5 : Tokens d'espacement (xxs 2px … 4xl 96px) et breakpoints (sm 640, md 768, lg 1024, xl 1280) exposés comme variables. (source : DESIGN.md:spacing, breakpoints, Layout & Spacing)
- UX-DR6 : Tokens de mouvement (duration-fast 120ms, base 200ms, slow 350ms ; easings standard/enter/exit) ; survols en duration-fast, modale en duration-slow ; tout passe à 0ms sous `prefers-reduced-motion`. (source : DESIGN.md:motion, Layout & Spacing ; EXPERIENCE.md:Interaction Primitives)
- UX-DR7 : Tokens de z-index (header 100, sub-header 90, popover 200, modal-backdrop 300, modal 310, toast 400). (source : DESIGN.md:z-index)
- UX-DR8 : Élévation plate : aucune ombre sur les cartes ; ombre uniquement sur modale (0 12px 32px rgba(0,0,0,.18)) et tooltip. (source : DESIGN.md:Elevation & Depth)
- UX-DR9 : Générer `frontend/src/adapters/ui/theme/tokens.css` depuis DESIGN.md par `npm run tokens` (`npx @google/design.md export --format css-tailwind` puis `frontend/scripts/tokens-root.mjs` qui convertit `@theme {…}` en `:root {…}`) ; fichier généré, jamais retouché, versionné à jour (`git diff --exit-code`), sans `@theme`. Livré par la première story d'écran (connexion, FEAT-001). (source : SYNTHESE.md:§7.1 ; AD-13)
- UX-DR10 : Pas de Tailwind : les écrans n'utilisent que les variables CSS de tokens.css via les composants ; aucun écran ne style un élément natif (`select`, `table`, `input`, `button`, `a`, `th`, `td`) hors des composants de `src/adapters/ui/`. (source : EXPERIENCE.md:Foundation « Composants » ; AD-13)
- UX-DR11 : Polices auto-hébergées, servies par le conteneur web, jamais par un CDN (ni dans le front, ni au rejeu ; aucune référence à fonts.googleapis/gstatic) ; Inter en repli de Graphik (Graphik sous licence). (source : DESIGN.md:typography ; AD-13)
- UX-DR12 : Thème clair uniquement, pas de mode sombre. (source : EXPERIENCE.md:Foundation [ASSUMPTION], Points ouverts 12)
- UX-DR13 : Réserve de marque : ne recréer ni logo ni wordmark ni asset de marque ; n'utiliser que les assets officiels autorisés ; logo texte « DB » + « Demo Bank » dans le header. (source : DESIGN.md:Brand & Style, Do's and Don'ts ; SYNTHESE.md:§4 ; EXPERIENCE.md:Navigation)
- UX-DR14 : Le style ne change jamais un texte affiché ni un `data-testid` ; toute évolution de token passe par DESIGN.md. (source : DESIGN.md:Do's and Don'ts)

#### Composants réutilisables

- UX-DR15 : `button-primary` : fond primary, texte on-primary, typo button, angles vifs, 48px de haut, padding 12px 24px ; états hover (primary-hover) et active (primary-active) ; un seul par vue ; désactivé avec `aria-busy` pendant l'envoi (anti double soumission, critique pour « Exécuter le virement ») ; pas de modale de confirmation. (source : DESIGN.md:components.button-primary* ; EXPERIENCE.md:Component Patterns #15 ; CMP-008)
- UX-DR16 : `button-secondary` : fond ink, texte on-surface-inverse, 48px ; utilisé pour « Filtrer » (admin) et « + Ajouter une valeur » (Bourse). (source : DESIGN.md:components.button-secondary ; EXPERIENCE.md:Component Patterns #22, #25)
- UX-DR17 : `button-ghost` : fond transparent, bordure et texte ink, 48px ; utilisé pour les actions inline de tableau (Supprimer, Suspendre/Reactiver, Approuver/Refuser, Desactiver/Activer, Confirmer/Faux positif), les actions rapides du chat et Libérer/Rejeter de la file de revue ; désactivé (`aria-disabled`) pendant la requête ; variante compacte 40px interdite. (source : DESIGN.md:components.button-ghost ; EXPERIENCE.md:Component Patterns #5, #20 ; Écrans nouveaux §3)
- UX-DR18 : `button-ghost-inverse` : transparent, bordure et texte blancs, 48px, pour fonds inverses. (source : DESIGN.md:components.button-ghost-inverse)
- UX-DR19 : `button-icon` : carré 48×48 fond primary, icône 24px centrée ; variante 40×40 interdite dans l'application (cible 44px). (source : DESIGN.md:components.button-icon ; EXPERIENCE.md:Accessibility Floor « Cibles »)
- UX-DR20 : `button-disabled` : fond disabled-bg, texte disabled-text, sans bordure ; porté avec `aria-disabled`/`aria-busy`. (source : DESIGN.md:components.button-disabled ; EXPERIENCE.md:Component Patterns #5)
- UX-DR21 : `link` : texte primary-hover, body-md, sans soulignement, souligné au survol. (source : DESIGN.md:components.link)
- UX-DR22 : `input` (CMP-008 Champ de formulaire) : fond surface, bordure 1px border-strong, 48px, padding 12px 16px ; `<label for>` relié ; placeholder legacy conservé mais jamais en guise de libellé ; `aria-required` en plus de l'astérisque legacy (« Libelle * »). (source : DESIGN.md:components.input ; EXPERIENCE.md:Component Patterns #12, Accessibility Floor)
- UX-DR23 : `input-helper` : texte d'aide text-secondary body-sm sous le champ (ex. « Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude », conservé tel quel). (source : DESIGN.md:components.input-helper ; EXPERIENCE.md:Points ouverts 10 ; FR-003-01)
- UX-DR24 : `input-error` (erreur de champ, `h:message for`) : fond error-container, texte et bordure error ; message sous le champ relié par `aria-describedby`, champ en `aria-invalid="true"`. (source : DESIGN.md:components.input-error ; EXPERIENCE.md:Component Patterns #13)
- UX-DR25 : `input-search-hero` : variante grand format (fond transparent, bordure basse 2px ink, h2 centré) pour pages de recherche / états vides (composant du design system disponible, aucun usage imposé). (source : DESIGN.md:components.input-search-hero)
- UX-DR26 : `checkbox` : case custom 20px, bordure 2px border-strong, coche blanche sur fond primary, label body-md à 10px, compteur caption optionnel. (source : DESIGN.md:components.checkbox)
- UX-DR27 : `select` (CMP-004 Combobox avec option vide) : `<select>` natif obligatoire (le golden master sélectionne par valeur), stylé 48px, chevron text-secondary ; valeur d'option = identifiant du compte comme le legacy ; première option legacy (« -- Choisir un compte -- », « -- Choisir -- », vide sur le Relevé). (source : DESIGN.md:components.select ; EXPERIENCE.md:Attributs de test, Component Patterns #7)
- UX-DR28 : `card` : fond surface, bordure 1px border, padding 24px, sans ombre. (source : DESIGN.md:components.card)
- UX-DR29 : `card-muted` : fond surface-alt, padding 24px, sans bordure ; sert aux KPI, aux sous-tableaux et aux bulles de Sophie. (source : DESIGN.md:components.card-muted ; EXPERIENCE.md:Component Patterns #6, #11, #19)
- UX-DR30 : `card-inverse` : fond surface-inverse, texte blanc, padding 32px. (source : DESIGN.md:components.card-inverse)
- UX-DR31 : `card-result` : carte de liste fond inverse, titre h4 blanc, métadonnées body-sm séparées par « | », `button-icon` à droite, hover éclairci. (source : DESIGN.md:components.card-result)
- UX-DR32 : `header` : fond surface, 72px, z-index header ; logo à gauche, nav centrée, action à droite ; actif souligné 2px primary. (source : DESIGN.md:components.header)
- UX-DR33 : `sub-header` : fond primary, texte blanc, 48px, sous le header, liens sans bordure. (source : DESIGN.md:components.sub-header)
- UX-DR34 : `app-nav` (CMP-001 Barre de navigation) : header 72px (niveau 1) + sub-header 48px (niveau 2), 2 niveaux maximum ; items en typo label, écart 24px ; actif souligné 2px primary (header) ou on-primary (sub-header) + `aria-current="page"` ; entrées filtrées par rôle côté serveur et côté client ; « Deconnexion » ferme la session et revient à la Connexion. (source : DESIGN.md:components.app-nav ; EXPERIENCE.md:Component Patterns #1)
- UX-DR35 : `tabs` : onglets horizontaux sans fond, actif ink + soulignement 2px primary, gap 32px, 2 à 6 onglets. (source : DESIGN.md:components.tabs)
- UX-DR36 : `pagination` : numérotée, page active soulignée 2px primary, flèches ← →, ellipsis, centrée (défilement infini interdit). (source : DESIGN.md:components.pagination ; EXPERIENCE.md:Interaction Primitives)
- UX-DR37 : `filter-panel` : sidebar 240px, sections repliables (+/−), checkboxes avec compteur, lien « Effacer ». (source : DESIGN.md:components.filter-panel)
- UX-DR38 : `footer` : fond inverse, padding 48px 24px. (source : DESIGN.md:components.footer)
- UX-DR39 : `eyebrow` : texte primary-hover, typo eyebrow en capitales ; porte les séparateurs de section du legacy rendus en capitales (« MES COMPTES DEMO BANK »), le texte source gardant sa casse d'origine (transformation CSS). (source : DESIGN.md:components.eyebrow ; EXPERIENCE.md:Voice and Tone « Casse rendue »)
- UX-DR40 : `tag` (CMP-006 Badge de statut) : pilule primary-container / on-primary-container, typo label ; valeur affichée telle quelle (CREDIT/DEBIT, Actif/Inactif, statuts de prêt, SEPA, OPEN/CONFIRMED…, badges de rôle « ADMIN »/« Conseiller », « En attente » du Relevé) ; la couleur double le texte ; DEFAUT sans badge (BUG-053 conservé). (source : DESIGN.md:components.tag ; EXPERIENCE.md:Component Patterns #9)
- UX-DR41 : `tag-accent` : pilule accent-tint, texte ink. (source : DESIGN.md:components.tag-accent)
- UX-DR42 : `alert-success` : fond success-container, texte success, barre gauche 4px, `role="status"` ; messages de succès legacy (« Virement programme cree avec succes », « Bénéficiaire ajouté avec succès. »). (source : DESIGN.md:components.alert-success ; EXPERIENCE.md:Component Patterns #14, State Patterns)
- UX-DR43 : `alert-warning` : fond warning-container, texte warning, barre gauche 4px ; avis de suspension (`role="status"`) et écran de refus (`role="alert"`). (source : DESIGN.md:components.alert-warning ; EXPERIENCE.md:Écrans nouveaux §1, §2)
- UX-DR44 : `alert-error` : fond error-container, texte error, barre 4px, `role="alert"` ; messages globaux d'erreur en haut du formulaire. (source : DESIGN.md:components.alert-error ; EXPERIENCE.md:Component Patterns #14)
- UX-DR45 : `alert-info` : fond info-container, texte info, barre 4px, `role="status"`. (source : DESIGN.md:components.alert-info ; EXPERIENCE.md:Component Patterns #14)
- UX-DR46 : `tooltip` : fond ink, caption blanc, rayon md, max 240px, ombre, z popover ; apparaît sur :hover/:focus-visible, disparaît en ≤120ms ; jamais de contenu interactif ; déclenché seulement par une icône ou un libellé tronqué (ex. pastille « Non lu »). (source : DESIGN.md:components.tooltip ; EXPERIENCE.md:Interaction Primitives)
- UX-DR47 : Tableau de données (CMP-003) `table-header` / `table-cell` / `table-row-hover` / `table-row-selected` : `<table>` natif, `<th scope="col">`, `<caption>` (éventuellement masqué) ou `aria-labelledby` vers le titre legacy ; en-tête surface-alt typo label, cellule body-sm bordure basse ; montants alignés à droite ; en-têtes en capitales sur les écrans où le legacy le faisait (« DATE DESCRIPTION TYPE MONTANT SOLDE APRES ») ; jamais de tableau de mise en page. (source : DESIGN.md:components.table-* ; EXPERIENCE.md:Component Patterns #3, Accessibility Floor)
- UX-DR48 : État vide de tableau (CMP-003) : une ligne `table-cell` sur toute la largeur avec le texte legacy exact (« Aucune transaction pour ce compte. », « Aucun pret en attente »…), en-têtes conservés, sans `empty-state` à icône ni bouton. (source : EXPERIENCE.md:Component Patterns #4)
- UX-DR49 : Sous-tableau échéancier (CMP-003) : tableau dans une `card-muted`, ouvert sous la ligne par « Voir » ; le focus va au titre du sous-tableau. (source : EXPERIENCE.md:Component Patterns #6)
- UX-DR50 : `modal` : fond surface, max 560px, padding 32px, ombre, backdrop rgba(0,0,0,.5), z modal ; une seule à la fois ; piège du focus, `Échap` ferme, focus rendu au déclencheur ; seul usage : saisie du motif de revue (FR-016-19). (source : DESIGN.md:components.modal ; EXPERIENCE.md:Component Patterns #27, Interaction Primitives)
- UX-DR51 : `focus-ring` : outline 2px primary, offset 2px, sur `:focus-visible` de tout élément interactif. (source : DESIGN.md:components.focus-ring ; EXPERIENCE.md:Accessibility Floor)
- UX-DR52 : `empty-state` : bloc centré max 480px (icône 48px, titre h3, texte, un bouton primaire optionnel) ; disponible mais non utilisé pour les listes vides du legacy (pas de texte ajouté). (source : DESIGN.md:components.empty-state ; EXPERIENCE.md:Component Patterns #4)
- UX-DR53 : `transfer-status-badge` (FR-027-16) : pilule label, padding 4px 12px, toujours un libellé texte, icône 16px optionnelle ; 5 variantes : executed (success/success-container, 4,78:1), suspended (warning, 4,98:1), released (info, 6,98:1), rejected (error, 5,15:1), control-error (fond surface, texte et bordure 1px error, 6,04:1). Libellés `virement.etat.*`. (source : DESIGN.md:components.transfer-status-badge* ; EXPERIENCE.md:Component Patterns #10 ; FR-027-16)
- UX-DR54 : `amount` (+ `amount-debit`, `amount-credit`, `amount-neutral`) : tabular-nums, aligné à droite, devise après le nombre au format exact du legacy écran par écran (« 10,548.72 EUR », `EURx,xxx.xx`), jamais normalisé ; débit en error semi-gras avec préfixe « - » (tiret ASCII, A-005-1) ; crédit en success semi-gras avec « + » ; solde/totaux en ink sans signe. (source : DESIGN.md:components.amount* ; EXPERIENCE.md:Voice and Tone « Formats »)
- UX-DR55 : `notification` (CMP-009 Centre de notifications) : ligne body-sm, barre gauche 4px currentColor, icône 20px + type en label + texte + horodatage `dd/MM/yyyy HH:mm:ss` en caption ; pastille « non lu » primary 8px avec infobulle « Non lu » ; compteur « {N} nouvelle(s) » (`aria-live="polite"`), « Tout marquer lu », « Effacer tout » ; emoji legacy (✅ ⚠️ 🚨) conservés dans le texte, icône éventuelle en `aria-hidden`. Variantes : confirmation (success), alert (warning), fraud (error, semi-gras), low-balance (warning, distinguée par icône et libellé). (source : DESIGN.md:components.notification* ; EXPERIENCE.md:Component Patterns #16 ; FR-002-08)
- UX-DR56 : Carte KPI (CMP-007) : `card-muted` + `amount-neutral`, valeur/libellé/sous-libellé legacy, lecture seule. (source : EXPERIENCE.md:Component Patterns #11 ; DESIGN.md:Components)
- UX-DR57 : Messages globaux de formulaire (CMP-008, `h:messages`) : `alert-error`/`alert-success`/`alert-info` en haut du formulaire, comme le legacy ; `role="alert"` (erreur) / `role="status"` (succès, info). (source : EXPERIENCE.md:Component Patterns #14)
- UX-DR58 : Panneau de détail inline (CMP-005) : `card` sous le tableau, ouvert par « Voir detail » / « Detail », fermé par « Fermer » et `Échap` ; focus au titre « Detail : {nom} » à l'ouverture, retour au bouton déclencheur à la fermeture ; pas de modale. (source : EXPERIENCE.md:Component Patterns #8)
- UX-DR59 : Carte de document (CMP-010) : `card` + `tag` + `link` ; « Telecharger PDF » ouvre un nouvel onglet, signalé par le texte lecteur d'écran « (nouvel onglet) » (`a11y.nouvel.onglet`). (source : EXPERIENCE.md:Component Patterns #17 ; textes-nouveaux.md)
- UX-DR60 : Simulateur à résultat repliable (CMP-011) : `card` + bouton de dépliage `link` « Voir l echeancier complet ({n} echeances) » avec `aria-expanded` et `aria-controls`. (source : EXPERIENCE.md:Component Patterns #18)
- UX-DR61 : Bulles de chat (FEAT-010) : Sophie en `card-muted`, client en `card` ; nouvelle bulle annoncée `aria-live="polite"` ; pendant l'attente « Envoyer » affiche « ... » (texte legacy) et se désactive. (source : EXPERIENCE.md:Component Patterns #19)
- UX-DR62 : Actions rapides du chat : `button-ghost`, visibles seulement sur une conversation neuve. (source : EXPERIENCE.md:Component Patterns #20)
- UX-DR63 : Vérification d'IBAN (Ajax) : résultat en ligne `alert-success`/`alert-error` sous le champ, `aria-live="polite"`, sans soumettre le formulaire. (source : EXPERIENCE.md:Component Patterns #21 ; FEAT-013)
- UX-DR64 : Formulaire repliable « + Ajouter une valeur » (Bourse) : `button-secondary` + `card` inline ; « Annuler » referme et rend le focus au bouton. (source : EXPERIENCE.md:Component Patterns #22 ; FEAT-009)
- UX-DR65 : Graphique de cours (SVG serveur) : SVG dans une `card`, `role="img"` avec nom accessible (texte à fixer avec la story) ; le tableau « 📋 Dernières séances » sert d'alternative textuelle. (source : EXPERIENCE.md:Component Patterns #23, Points ouverts 6 ; FEAT-009)
- UX-DR66 : Jauge de plafond (Budget) : barre primary, error au-delà du plafond ; `role="progressbar"` + `aria-valuenow` ; texte « {n}% du plafond » toujours affiché, « Depassement » en texte. (source : EXPERIENCE.md:Component Patterns #24, Accessibility Floor ; FR-006-09)
- UX-DR67 : Filtre clients (Administration) : `input` + `button-secondary` « Filtrer », placeholder « Rechercher par nom, login, email... ». (source : EXPERIENCE.md:Component Patterns #25 ; FEAT-016)
- UX-DR68 : Avatar initiale (cartes clients du conseiller) : pastille `rounded.pill`, décorative, `aria-hidden`. (source : EXPERIENCE.md:Component Patterns #26 ; FEAT-015)
- UX-DR69 : Bandeau technique legacy (CMP-002) : aucun composant ; supprimé partout (A-001-3, A-002-4, A-003-6, A-011-2, A-018-3). (source : EXPERIENCE.md:Component Patterns #2)

#### Gabarit, navigation, lien d'évitement, routes

- UX-DR70 : Gabarit authentifié : `app-nav` sur toutes les pages authentifiées ; header avec à gauche « DB » « Demo Bank », au centre les 12 liens legacy dans l'ordre et l'orthographe exacts (« Tableau de bord », « Virement », « Virements programmes », « Releve », « Budget », « Prets », « Documents », « Bourse », « Mon Conseiller », « Produits », « Comptes Tiers », « Beneficiaires »), puis « Espace Conseiller » (CONSEILLER, ADMIN) et « Administration » (ADMIN) ; à droite « Bonjour, {fullName} » et « Deconnexion » (`nav-deconnexion`) ; badge de rôle en `tag`. (source : EXPERIENCE.md:Navigation ; FEAT-001)
- UX-DR71 : Sub-header (niveau 2) uniquement dans Espace Conseiller (« Mon portefeuille » · « Conseiller IA ») et Administration (« Administration » · « Espace Conseiller » · « Mon tableau de bord ») ; pas de sub-header ni de libellé de regroupement sur les pages client. (source : EXPERIENCE.md:Navigation)
- UX-DR72 : Élément de navigation actif : soulignement 2px primary + `aria-current="page"`. (source : EXPERIENCE.md:Navigation)
- UX-DR73 : Lien d'évitement « Aller au contenu principal » (`a11y.lien.evitement`) sur toutes les pages, visible au focus, menant au contenu principal ; il entre dans `innerText` (entrée groupée C1). (source : EXPERIENCE.md:Accessibility Floor, Écarts partie C ; textes-nouveaux.md)
- UX-DR74 : Routes et écrans : 17 écrans cibles (Connexion, Tableau de bord, Virement, Refus pour erreur du contrôle sans entrée de menu, Virements programmes, Releve, Budget, Prets, Documents, Bourse, Mon Conseiller, Produits, Comptes Tiers, Beneficiaires, Retour Powens en transit sans rendu, Espace Conseiller, Administration) ; SPA appelant l'API interne (A-G-05). (source : EXPERIENCE.md:Information Architecture, Foundation)
- UX-DR75 : Écran d'arrivée après connexion selon le rôle : CLIENT → Tableau de bord, CONSEILLER → Espace Conseiller, ADMIN → Administration. (source : EXPERIENCE.md:Écrans par rôle ; FR-001-05)
- UX-DR76 : Accès non authentifié ou session expirée (30 min) → redirection vers la Connexion ; accès à un écran réservé à un autre rôle → redirection sans message vers le Tableau de bord ; écrans client ouverts à tous les rôles, chacun ne voyant que ses données. (source : EXPERIENCE.md:Écrans par rôle, State Patterns ; FR-001-02, FR-001-11, FR-016-01, FR-003-01, NFR-SEC-07)
- UX-DR77 : Liens internes legacy conservés comme points d'entrée : « Voir le releve complet » (→ Releve), « Voir la page Produits » (Administration → Produits), liens du Tableau de bord vers Comptes Tiers, « Virement SEPA externe — Gérer mes bénéficiaires » (→ Beneficiaires), sub-header « Conseiller IA » (→ Mon Conseiller). (source : EXPERIENCE.md:Écrans par rôle)
- UX-DR78 : Titre d'onglet « BankApp Demo - {page} » repris du legacy ; `lang="fr"`. (source : EXPERIENCE.md:Accessibility Floor)
- UX-DR79 : Navigation : 2 niveaux au plus, pas de menu hamburger (aucun nom accessible nouveau). (source : EXPERIENCE.md:Navigation, Interaction Primitives)
- UX-DR80 : Erreurs de téléchargement de documents (« Parametre type manquant »…) : réponses HTTP en texte brut comme le legacy, pas un écran conçu. (source : EXPERIENCE.md:Information Architecture ; FR-017)

#### Accessibilité

- UX-DR81 : Plancher WCAG 2.1 AA sur les comportements, sans audit formel ; règles appliquées dès la première story ; rôles `alert`/`status`, `aria-*` et gestion du focus portés par les composants, jamais ajoutés écran par écran. (source : EXPERIENCE.md:Accessibility Floor ; SYNTHESE.md:§3.1 ; AD-13)
- UX-DR82 : Contrastes : 4,5:1 texte, 3:1 composants et bordures ; ratios calculés pour chaque paire ajoutée (badges, montants, notifications, nav : de 4,5:1 à 21:1) et à conserver. (source : DESIGN.md:Components, notes des tokens ; SYNTHESE.md:§3.1)
- UX-DR83 : Clavier : tout utilisable au clavier, `Tab`/`Maj+Tab` dans l'ordre de lecture, `Entrée` soumet le formulaire focalisé, `Espace`/`Entrée` actionnent un bouton, `Échap` ferme modale ou panneau ; aucun raccourci ; aucun piège hors modale. (source : EXPERIENCE.md:Interaction Primitives, Accessibility Floor)
- UX-DR84 : Focus visible `focus-ring` sur `:focus-visible` de chaque élément interactif. (source : EXPERIENCE.md:Accessibility Floor)
- UX-DR85 : Libellés : chaque champ a un `<label for>` ; obligatoires en `aria-required` + astérisque legacy. (source : EXPERIENCE.md:Accessibility Floor)
- UX-DR86 : Erreurs : `aria-invalid="true"` + `aria-describedby` ; erreurs globales `role="alert"`, succès `role="status"` ; après une soumission en erreur, focus au premier champ en erreur, sinon au message global. (source : EXPERIENCE.md:Accessibility Floor)
- UX-DR87 : `aria-live="polite"` sur le compteur d'opérations du Relevé, le résultat d'IBAN, les bulles du chat et le badge « {N} nouvelle(s) ». (source : EXPERIENCE.md:Accessibility Floor)
- UX-DR88 : Cibles ≥ 44×44px (hauteur 48px des boutons, champs, listes) ; variantes compactes 40px interdites. (source : EXPERIENCE.md:Accessibility Floor)
- UX-DR89 : Tableaux accessibles : `<th scope="col">`, `<caption>` ou `aria-labelledby`, pas de tableau de mise en page. (source : EXPERIENCE.md:Accessibility Floor)
- UX-DR90 : Couleur jamais seule : libellé, signe (« + »/« - ») ou texte (« Depassement ») obligatoire pour tout état. (source : EXPERIENCE.md:Accessibility Floor ; DESIGN.md:Colors)
- UX-DR91 : Emoji conservés dans le texte ; icône d'accompagnement en `aria-hidden="true"` ; avatar décoratif `aria-hidden`. (source : EXPERIENCE.md:Voice and Tone, Component Patterns #26)
- UX-DR92 : `prefers-reduced-motion` respecté (0ms). (source : EXPERIENCE.md:Accessibility Floor ; DESIGN.md:Layout & Spacing)
- UX-DR93 : Ouverture d'un PDF dans un nouvel onglet signalée par « (nouvel onglet) » réservé aux lecteurs d'écran, ajouté au nom du lien. (source : EXPERIENCE.md:Component Patterns #17 ; textes-nouveaux.md:a11y.nouvel.onglet)
- UX-DR94 : Gestion du focus aux changements de vue : panneau de détail (titre puis retour au déclencheur), sous-tableau (titre), formulaire Bourse (retour au bouton), avis de suspension (focus sur l'avis), écran de refus (titre « Effectuer un virement »), modale (piège puis retour). (source : EXPERIENCE.md:Component Patterns #6, #8, #22, #27 ; Écrans nouveaux §1, §2)
- UX-DR95 : Hors démonstrateur : EAA (directive (UE) 2019/882, EN 301 549) et RGAA à traiter seulement en mise en production (audit, déclaration) ; WCAG 2.2 AA notée comme cible produit ; axe-core en E2E possible mais hors quality gates actuels. (source : EXPERIENCE.md:Accessibility Floor ; SYNTHESE.md:§3.2)

#### Responsive

- UX-DR96 : Cible de référence desktop ≥ lg (1024px) ; sous lg, l'application reste utilisable pour consulter et remplir des formulaires simples, sans optimisation mobile ni test mobile. (source : EXPERIENCE.md:Foundation [ASSUMPTION])
- UX-DR97 : Sous xl (1280px), les liens du header passent sur deux lignes et le header grandit, sans menu repliable. (source : EXPERIENCE.md:Navigation ; Points ouverts 12) — note : DESIGN.md:app-nav évoque un repli en menu déroulant sous 768px ; EXPERIENCE.md l'emporte sur le comportement (pas de hamburger).
- UX-DR98 : Tableaux dans un conteneur à défilement horizontal sous lg. (source : EXPERIENCE.md:Component Patterns #3)

#### États et motifs d'interaction

- UX-DR99 : Chargement d'une page : pas de squelette ni de texte de chargement ; zone de contenu en `aria-busy="true"` jusqu'aux données ; cible < 200 ms en navigation. (source : EXPERIENCE.md:State Patterns ; NFR-PERF-01)
- UX-DR100 : Envoi de formulaire : bouton de soumission désactivé avec `aria-busy` ; valeurs saisies conservées en cas d'erreur. (source : EXPERIENCE.md:State Patterns ; FR-003-03)
- UX-DR101 : État vide : texte legacy exact dans la ligne de tableau ou le bloc, sans icône ni action (ex. « Aucun virement programme configure », « Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel »). (source : EXPERIENCE.md:State Patterns ; FR-002-07)
- UX-DR102 : Erreur de validation : message legacy ou PRD sous le champ (`input-error`) ou en haut du formulaire (`alert-error`) selon l'emplacement du legacy, annoncé. (source : EXPERIENCE.md:State Patterns)
- UX-DR103 : Erreur technique : jamais de détail technique ; texte fixé par la FR (écarts partie B, textes-nouveaux). (source : EXPERIENCE.md:State Patterns ; NFR-SEC-04)
- UX-DR104 : Fournisseur indisponible (Powens, IA, AlphaVantage) : message de la FR dans la zone d'erreur de l'écran, le reste de l'application fonctionne. (source : EXPERIENCE.md:State Patterns ; FR-012-14, FR-010-09, NFR-INT-02)
- UX-DR105 : Succès : message legacy en `alert-success` `role="status"` ; un virement exécuté redirige vers le Tableau de bord. (source : EXPERIENCE.md:State Patterns)
- UX-DR106 : Zones de message par écran avec `data-testid` `{écran}-message-erreur`, `{écran}-message-succes`, `{écran}-message-info` (ex. `beneficiaires-message-info` attendu par SC-013-02). (source : EXPERIENCE.md:Attributs de test)
- UX-DR107 : Confirmations : aucune modale de confirmation (virement, actions inline, comme le legacy) ; seule la libération/le rejet d'un virement suspendu ouvre une modale, pour saisir le motif obligatoire. (source : EXPERIENCE.md:Interaction Primitives, Component Patterns #5, #15)
- UX-DR108 : Soumission explicite : aucun formulaire ne se soumet à la saisie, sauf trois exceptions legacy : changement de compte du Relevé, « Verifier l'IBAN », sélecteur de valeur et de période de la Bourse. (source : EXPERIENCE.md:Interaction Primitives)
- UX-DR109 : Saisie : montants au format legacy (virgule décimale acceptée, « Ex : 100,00 ») ; date de première exécution `yyyy-MM-dd` (BUG-012 conservé) ; motif de virement facultatif, 100 caractères max. (source : EXPERIENCE.md:Interaction Primitives ; FR-003-01)
- UX-DR110 : Interdits : défilement infini, glisser-déposer, actions visibles seulement au survol, plus d'une modale, plus de 2 niveaux de navigation, contenu interactif dans un tooltip. (source : EXPERIENCE.md:Interaction Primitives)
- UX-DR111 : États d'un virement côté UI (FR-027-16) : EXÉCUTÉ/SUCCESS (redirection Tableau de bord, notification CONFIRMATION, badge executed) ; EXÉCUTÉ/PENDING 5 000–9 999,99 € (textes legacy ALERTE et « [EN ATTENTE] » gardés, B1 révisée) ; SUSPENDU (avis + heure butoir, notification FRAUDE unique, badge suspended, pas de débit) ; LIBÉRÉ (badge « Accepté - pas encore débité », sans notification) ; REJETÉ (badge « Rejeté - non débité » quelle que soit la cause, aucune notification, FR-002-10) ; REFUSÉ_ERREUR_CONTRÔLE (écran de refus, aucune notification, A-003-8). Changements vus au rechargement, sans push. (source : EXPERIENCE.md:États d'un virement)
- UX-DR112 : Échéance programmée suspendue → virement SUSPENDU (même notification FRAUDE, même heure butoir) ; le virement programmé reste « Actif » dans Virements programmes (ne pas confondre avec SUSPENDU). (source : EXPERIENCE.md:État d'une échéance programmée ; FR-021-10)
- UX-DR113 : Type de notification selon l'issue du contrôle puis le montant (suspendu sur score → FRAUDE quel que soit le montant) ; libellé de type « FRAUDE » conservé tant que le PM n'a pas tranché. (source : EXPERIENCE.md:Écarts A8, Points ouverts 7 ; FR-002-10, FR-025-05)

#### Écrans

Règle commune : tout champ et bouton actionné par un scénario porte un `data-testid` stable `{écran}-{élément}[-{identifiant métier}]` (minuscules ASCII sans accent, `-` comme séparateur ; préfixes `login`, `nav`, `dashboard`, `virement`, `vp`, `releve`, `budget`, `pret`, `documents`, `bourse`, `chat`, `produits`, `tiers`, `beneficiaires`, `conseiller`, `admin`, `revue` ; élément = nom métier FIELDS.md francisé, jamais le texte ni une position ; identifiant métier de ligne dans les listes) ; tous les autres éléments interactifs en portent aussi ; un `data-testid` publié en G2 ne change plus jamais (NFR-UI-02).

- UX-DR114 : Connexion (FEAT-001, public) : « Identifiant », « Mot de passe », « Me connecter », sous-titre « Espace client — Connexion sécurisée » conservé ; bandeau « ⚠️ Système Legacy — JEE 8 / WildFly 14 » supprimé (B-16) ; compte désactivé refusé (« Identifiants incorrects » / « Vérifiez votre login et mot de passe », B-10, FR-001-07) ; `data-testid` `login-identifiant`, `login-mot-de-passe`, `login-me-connecter`. (source : EXPERIENCE.md:Attributs de test, UJ-1, Écarts B-10, B-16 ; divergences:SC-001-05)
- UX-DR115 : Tableau de bord (FEAT-002, 025, 027) : « Solde total Demo Bank », séparateur « MES COMPTES DEMO BANK » (eyebrow), comptes, KPI, « Dernieres operations » (5 dernières du compte COURANT, en-tête « DATE DESCRIPTION TYPE MONTANT SOLDE APRES » en capitales, format `EURx,xxx.xx`), « Centre de notifications » (CMP-009) avec notification de solde faible enfin affichée (A9, FR-025-08) ; bandeaux et libellés techniques retirés (B-17, B-18, B-19) ; section « Suivi de mes virements » en bas de page (`dashboard-virements-revue`) ; préfixe `dashboard-`. (source : EXPERIENCE.md:Information Architecture, UJ-1, Écarts A9, B-17 à B-19, §4 ; divergences:SC-002-01/02)
- UX-DR116 : Virement (FEAT-003, 027) : titre « Effectuer un virement », `select` natifs « Compte source » / « Compte destinataire », « Montant (€) » avec indice legacy, « Motif » facultatif (100 car.), bouton « Exécuter le virement » désactivé pendant l'envoi ; redirection vers le Tableau de bord si EXÉCUTÉ ; erreurs « Solde insuffisant sur le compte source » (B-1), « Erreur lors du virement : Le montant doit etre positif » (B-2), « Erreur lors du virement : Compte introuvable » (B-3), « Le compte source et destination doivent être différents » (legacy), validations `virement.validation.*` (B-21) ; avertissement « Attention : {N} alerte(s) fraude… » supprimé (A5) ; bandeaux EJB/ActiveMQ supprimés (B-20) ; section « Suivi de mes virements » en bas (`virement-virements-revue`) ; `data-testid` `virement-compte-source`, `virement-compte-destinataire`, `virement-montant`, `virement-motif`, `virement-executer`. (source : EXPERIENCE.md:Attributs de test, UJ-1, Écarts A5, B-1 à B-3, B-20, B-21 ; FR-003-01, FR-003-03 à 06)
- UX-DR117 : Avis de virement suspendu (FR-027-04, FR-023-04, FR-021-10) : sur l'écran Virement (pas de redirection), `alert-warning` en haut du formulaire, `role="status"`, focus placé dessus, formulaire vidé ; texte `virement.avis.suspension` ; aucune mention de fraude ni de signal ; une seule notification FRAUDE au même texte ; `data-testid` `virement-avis-suspension`, `virement-avis-heure-butoir`. (source : EXPERIENCE.md:Écrans nouveaux §1)
- UX-DR118 : Écran de refus pour erreur du contrôle (FR-003-27, NFR-OBS-02) : vue de résultat dans le parcours Virement à la place du formulaire, sans entrée de menu ; `alert-warning` en `role="alert"` portant le texte D9 mot pour mot ; référence = identifiant de trace, sélectionnable, police à chasse fixe, `data-testid="virement-refus-reference"` ; focus sur le titre « Effectuer un virement » ; aucune mention de fraude, aucune notification, aucun solde modifié ; retour par le header ; reprise préremplie « Refaire ma demande » (FR-003-28) dans une story suivante. (source : EXPERIENCE.md:Écrans nouveaux §2 ; textes-nouveaux.md)
- UX-DR119 : Suivi des états d'un virement côté client (FR-027-16, A-027-10) : section « Suivi de mes virements » sur Virement et Tableau de bord, en bas de page après le contenu legacy ; liste des virements passés par une revue (SUSPENDU, LIBÉRÉ, REJETÉ, EXÉCUTÉ après libération ; ni les exécutés directs ni les refus du contrôle) ; colonnes Date / Compte destinataire / Montant / Etat (`transfer-status-badge-*`) / Réponse avant le (SUSPENDU) ; aide `virement.revue.aide` ; vide « Aucun virement à suivre » ; aucun libellé « en attente » pour un exécuté, aucune mention de fraude ; `data-testid` `dashboard-virements-revue`, `virement-virements-revue`, `virement-etat-{transferId}`. (source : EXPERIENCE.md:Écrans nouveaux §4 ; textes-nouveaux.md:virement.revue.*, virement.etat.*)
- UX-DR120 : Virements programmes (FEAT-004, 021) : formulaire « Compte débiteur », montant, fréquence (« Hebdomadaire », « Mensuel »), numéro et libellé bénéficiaire (bénéficiaire interne obligatoire, FR-004-13), date `yyyy-MM-dd`, motif, bouton « Creer le virement programme » ; tableau « MES VIREMENTS PROGRAMMES » avec colonne « Etat » Actif/Inactif, actions inline Suspendre/Reactiver/Supprimer ; messages « Virement programme cree avec succes », « Veuillez selectionner un compte source », vide « Aucun virement programme configure », erreurs `vp.erreur.*` ; `data-testid` `vp-compte-debiteur`, `vp-montant`, `vp-frequence`, `vp-numero-beneficiaire`, `vp-libelle-beneficiaire`, `vp-date-premiere-execution`, `vp-motif`, `vp-creer`. (source : EXPERIENCE.md:Attributs de test, State Patterns ; divergences:SC-004-01..03 ; FR-004-02, 04, 08, 10)
- UX-DR121 : Releve (FEAT-005) : titre « Relevé de compte », `select` de compte (option vide) qui recharge les opérations sans recharger la page, compteur d'opérations `aria-live`, tableau avec montants signés ; lignes « [EN ATTENTE] » : badge `tag` « En attente » avant le libellé, 13 caractères retirés, fond warning-container (FR-005-07) ; bandeau @NamedQuery supprimé (B-22) ; préfixe `releve-`. (source : EXPERIENCE.md:Component Patterns #7, Écrans nouveaux §5, Écarts B-22 ; divergences:SC-005-01)
- UX-DR122 : Budget (FEAT-006) : 4 titres de section legacy, jauge de plafond (`progressbar`, « {n}% du plafond », error au-delà, « Depassement ») ; erreurs techniques remplacées par « Une erreur est survenue. Veuillez reessayer. » (B-12) ; bandeau supprimé (B-23) ; préfixe `budget-`. (source : EXPERIENCE.md:Component Patterns #24, Écarts B-12, B-23 ; divergences:SC-006-01 ; FR-006-09)
- UX-DR123 : Prets (FEAT-007, 024) : sections legacy, « TYPE DE PRET », « Immobilier », « Automobile » ; simulateur (montant, taux, durée, « Calculer », résultat au format legacy « 293.02 EUR », « 10,548.72 EUR ») avec échéancier repliable ; sous-tableau échéancier par « Voir » ; « Marquer paye » ; échec de transmission « Service credit temporairement indisponible, veuillez reessayer plus tard » (B-11) ; erreurs `pret.erreur.*` ; bandeaux supprimés (B-24) ; `data-testid` `pret-sim-montant`, `pret-sim-taux`, `pret-sim-duree`, `pret-sim-calculer`. (source : EXPERIENCE.md:Attributs de test, Component Patterns #6, #18, Écarts B-11, B-24 ; divergences:SC-007-01/02)
- UX-DR124 : Documents (FEAT-008, 017) : cartes de document (`card` + `tag` + `link`), « Telecharger PDF » et relevés mensuels ouvrant un nouvel onglet avec « (nouvel onglet) » pour lecteurs d'écran ; titres RIB et attestation conservés ; bandeau OpenPDF/Servlet supprimé (B-25) ; préfixe `documents-`. (source : EXPERIENCE.md:Component Patterns #17, Écarts B-25 ; divergences:SC-008-01, SC-017-02)
- UX-DR125 : Bourse (FEAT-009, 029) : watchlist (« Valeur », « BNP.PA », « AI.PA »), « VARIATION SUR LA PÉRIODE », « PERFORMANCE % », sélecteur de valeur et de période à soumission immédiate, graphique SVG `role="img"` + tableau « 📋 Dernières séances », formulaire repliable « + Ajouter une valeur » / « Annuler », message d'indisponibilité legacy ; bandeau API supprimé (B-26) ; préfixe `bourse-`. (source : EXPERIENCE.md:Component Patterns #22, #23, Interaction Primitives, Écarts B-26 ; divergences:SC-009-01)
- UX-DR126 : Mon Conseiller / chat (FEAT-010) : en-tête « Sophie — Conseillère Demo Bank », bienvenue « Bonjour Jean ! », bulles (Sophie `card-muted`, client `card`, `aria-live`), actions rapides sur conversation neuve, « Envoyer » → « ... » désactivé en attente ; indisponibilité « Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. » (B-4) ; bandeau IA supprimé (B-27) ; `data-testid` `chat-saisie`, `chat-envoyer`, `chat-message-utilisateur`. (source : EXPERIENCE.md:Attributs de test, Component Patterns #19, #20, Écarts B-4, B-27 ; FR-010-02, FR-010-09)
- UX-DR127 : Produits (FEAT-011, 018) : sections legacy, « CEL - Compte Epargne Logement », « Livret A » ; titres « Traitements nocturnes » / « Historique des traitements nocturnes » sans « (EJB @Schedule) » (B-14) ; bandeau JPA supprimé (B-28) ; préfixe `produits-`. (source : EXPERIENCE.md:Écarts B-14, B-28 ; divergences:SC-011-01)
- UX-DR128 : Comptes Tiers (FEAT-012, 028) : état non connecté « Agregez vos comptes bancaires » (banques, « OAuth2 PSD2 » du paragraphe fonctionnel conservé), « Connecter une banque » → Powens ; état connecté « Connected via Powens », « Patrimoine total (tous comptes) », « Mes comptes externes ({N}) », premier compte sélectionné avec ses transactions ; « Solde indisponible » (B-6), indisponibilité Powens (B-7), annulation avec motif (B-5), bouton « Rafraichir » (B-8), callback incomplet (B-9) ; bandeau Open Banking supprimé (B-29) ; préfixe `tiers-`. (source : EXPERIENCE.md:UJ-4, Écarts B-5 à B-9, B-29 ; FR-012-07, 12, 14, 16)
- UX-DR129 : Retour Powens (FEAT-014) : page de transit sans rendu, redirection vers Comptes Tiers ; accessible sans session sur son chemin exact (302). (source : EXPERIENCE.md:Information Architecture #15, UJ-4 ; divergences:SC-014-01 ; FR-014-01, FR-030-01)
- UX-DR130 : Beneficiaires (FEAT-013) : formulaire Libelle */titulaire/IBAN/BIC/banque, « Verifier l'IBAN » (Ajax, `aria-live`, sans soumission), « Ajouter le beneficiaire », succès « Bénéficiaire ajouté avec succès », tableau avec IBAN masqué (« FR76*** », « FR35***9010 ») et « ✓ SEPA » ; erreur `beneficiaire.erreur.ajout` ; bandeau IBAN MOD-97 supprimé (B-30) ; `data-testid` `beneficiaires-libelle`, `beneficiaires-titulaire`, `beneficiaires-iban`, `beneficiaires-bic`, `beneficiaires-banque`, `beneficiaires-verifier-iban`, `beneficiaires-ajouter`, `beneficiaires-message-info`. (source : EXPERIENCE.md:Attributs de test, Component Patterns #21 ; divergences:SC-013-01/02 ; FR-013-03)
- UX-DR131 : Espace Conseiller (FEAT-015, CONSEILLER/ADMIN) : sub-header « Mon portefeuille » · « Conseiller IA » ; bandeau « Bonjour {fullName}, vous gerez {N} client(s) », 3 KPI, cartes clients (avatar initiale `aria-hidden`, seuls les clients affectés), « Voir detail » ouvrant le panneau inline (Comptes bancaires, Prets ; pas de « Client depuis ») avec gestion du focus ; CLIENT redirigé sans message ; préfixe `conseiller-`. (source : EXPERIENCE.md:Navigation, UJ-2, Component Patterns #8, #26 ; divergences:SC-015-01/02)
- UX-DR132 : Administration (FEAT-016, 018, 019, 026, 027, ADMIN) : titre et badge « ADMIN » (bandeau RBAC supprimé, B-31), sub-header, indicateurs (« TOTAL CLIENTS »), « Pipeline Prets — En attente d approbation » (Approuver/Refuser, « Prêt approuvé », erreur B-13, vide « Aucun pret en attente »), « Gestion des clients » (filtre, Desactiver/Activer, « Client désactivé », badge Actif/Inactif, panneau « Detail » avec « Client depuis »), file de revue (UX-DR133), « Alertes Fraude — Moteur de détection » (toutes les alertes, Confirmer/Faux positif, « Traité par {nom} »), « Traitements nocturnes » (B-15), lien « Voir la page Produits » ; actions inline sans confirmation ; `data-testid` du type `admin-desactiver-{customerId}`. (source : EXPERIENCE.md:Navigation, UJ-3, Écarts B-13, B-15, B-31, Attributs de test ; divergences:SC-016-01/02)
- UX-DR133 : File de revue des virements suspendus (FR-016-19, FR-027-17, B4–B7) : section de l'Administration réservée ADMIN, titre « Virements suspendus — En attente de revue », placée après « Gestion des clients » et avant « Alertes Fraude », sans entrée de menu ; triée par heure butoir la plus proche (A-016-3) ; colonnes Client / Montant (`amount-neutral`) / Compte destinataire / Motif de suspension (`revue.motif.score|seuil|cumul`) / Heure butoir / Actions ; boutons `button-ghost` « Libérer » / « Rejeter » (`revue-liberer-{transferId}`, `revue-rejeter-{transferId}`) ouvrant une modale (titre `revue.modale.titre.*`, champ « Motif de la décision * » `revue-motif` `aria-required`, « Valider » `revue-valider`, « Annuler ») ; motif vide refusé client et serveur (« Veuillez indiquer un motif », `aria-invalid`) ; après décision la ligne quitte la file, alertes OPEN → REVIEWED « Traité par {nom} » après rechargement de la section (CONFIRMED/FALSE_POSITIVE inchangées) ; messages `revue.succes.*`, `revue.erreur.solde.insuffisant` (B6), « Heure butoir dépassée : le virement est rejeté. » en `alert-error` (B5, le serveur décide, actions jamais masquées côté client) ; vide « Aucun virement suspendu » ; API refusée aux autres rôles. (source : EXPERIENCE.md:Écrans nouveaux §3, UJ-5 ; textes-nouveaux.md:revue.*)
- UX-DR134 : Historique des virements traités (FR-016-19, B7, A-016-7) : sous la file de revue, liste des virements libérés ou rejetés avec état et motif d'issue (dont « Rejeté : solde insuffisant au moment du débit », distinct des rejets admin et heure butoir) ; n'apparaît que s'il contient au moins un virement ; titre, colonnes et profondeur restent à fixer (A-016-7). (source : EXPERIENCE.md:Écrans nouveaux §3 ; textes-nouveaux.md:revue.motif.rejet.solde.debit)

#### Textes nouveaux

- UX-DR135 : Règles d'écriture : wording legacy intouchable (casse rendue, accents absents, ponctuation, tirets, emoji, formats) ; textes nouveaux en orthographe correcte, textes legacy à l'identique, préfixes « Erreur : », « Erreur suppression : », « Erreur simulation : » conservés ; une phrase sans point final, plusieurs phrases avec point final ; « - » dans les badges, « — » dans les titres de section admin ; clés en minuscules et points préfixées par écran ; textes client neutres (pas de fraude/signal/score), disant si quelque chose a été débité et quoi faire ; aucun texte inventé hors du lot. (source : EXPERIENCE.md:Voice and Tone ; textes-nouveaux.md:Règle suivie, Orthographe)
- UX-DR136 : Suivi client `virement.revue.titre` « Suivi de mes virements », `virement.revue.aide`, `virement.revue.vide` « Aucun virement à suivre », colonnes `virement.revue.colonne.date|compte|montant|etat|butoir` (« Date », « Compte destinataire », « Montant », « Etat », « Réponse avant le »), `virement.revue.butoir.valeur` « {date} à {heure} » — écrans Virement et Tableau de bord (FR-027-16 ; entrée groupée C1 sauf butoir.valeur). (source : textes-nouveaux.md)
- UX-DR137 : Badges `virement.etat.execute` « Exécuté - débité », `.suspendu` « En cours de vérification - non débité », `.libere` « Accepté - pas encore débité », `.rejete` « Rejeté - non débité », `.refuse.erreur.controle` « Non vérifié - non débité » — Virement, Tableau de bord (FR-027-16, B6). (source : textes-nouveaux.md)
- UX-DR138 : Avis `virement.avis.suspension` (texte A-027-4 orthographe corrigée) — Virement et notification FRAUDE (FR-027-04, FR-023-04, FR-021-10 ; écart A2). (source : textes-nouveaux.md ; EXPERIENCE.md:Écarts A2)
- UX-DR139 : Refus `virement.refus.erreur.controle` (texte D9 mot pour mot, A6) et bouton `virement.reprise.bouton` « Refaire ma demande » — écran de refus (FR-003-27, FR-003-28). (source : textes-nouveaux.md)
- UX-DR140 : Validations `virement.validation.comptes` « Veuillez sélectionner les deux comptes » et `virement.validation.montant` « Veuillez saisir le montant » ; aucun message pour le motif facultatif ; montant non positif garde « Erreur lors du virement : Le montant doit etre positif » — Virement (A-003-6, B-21, FR-003-04). (source : textes-nouveaux.md)
- UX-DR141 : Erreurs Virements programmes `vp.erreur.creation`, `vp.erreur.etat`, `vp.erreur.suppression`, `vp.erreur.appartenance` « Erreur : action impossible. Rechargez la page. » (FR-004-06, FR-004-10, A-004-1, NFR-SEC-04). (source : textes-nouveaux.md)
- UX-DR142 : Erreurs Prets `pret.erreur.simulation`, `pret.erreur.demande`, `pret.erreur.paiement`, `pret.erreur.suppression` (MSG-007-123/128/129/130, NFR-SEC-04). (source : textes-nouveaux.md)
- UX-DR143 : Erreur Beneficiaires `beneficiaire.erreur.ajout` « Erreur lors de l'ajout du bénéficiaire. Veuillez réessayer. » (MSG-013-077, NFR-SEC-04). (source : textes-nouveaux.md)
- UX-DR144 : File de revue, structure : `revue.titre`, `revue.colonne.client|montant|compte|motif|butoir|actions`, `revue.vide` « Aucun virement suspendu » — Administration (FR-016-19 ; entrée groupée C1). (source : textes-nouveaux.md)
- UX-DR145 : File de revue, motifs de suspension `revue.motif.score` « Score cumulé {score} - signaux : {signaux} », `revue.motif.seuil`, `revue.motif.cumul` (après FR-027-18). (source : textes-nouveaux.md)
- UX-DR146 : File de revue, actions et modale : `revue.bouton.liberer|rejeter`, `revue.modale.titre.liberer|rejeter`, `revue.modale.champ.motif` « Motif de la décision * », `revue.modale.bouton.valider|annuler`, `revue.erreur.motif.manquant` « Veuillez indiquer un motif » (FR-016-19). (source : textes-nouveaux.md)
- UX-DR147 : File de revue, issues : `revue.succes.liberer` (ne dit pas « débité »), `revue.succes.rejeter`, `revue.erreur.solde.insuffisant` (B6, A10), `revue.erreur.heure.butoir` (A-016-6, A7), `revue.motif.rejet.solde.debit` (B7, A11, historique, à valider) (FR-016-19, FR-027-16). (source : textes-nouveaux.md ; EXPERIENCE.md:Écarts A7, A10, A11)
- UX-DR148 : Accessibilité `a11y.lien.evitement` « Aller au contenu principal » (toutes pages) et `a11y.nouvel.onglet` « (nouvel onglet) » (Documents) — entrée groupée C1. (source : textes-nouveaux.md)
- UX-DR149 : Notification de solde faible (A9) : titre « Solde insuffisant sur votre compte », message « Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte. » — Tableau de bord (FR-025-08, B3). (source : EXPERIENCE.md:Écarts A9)
- UX-DR150 : Textes legacy PENDING gardés pour la démo (B1 révisée, BUG-009 provisoire) : notification ALERTE « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. », mention « [EN ATTENTE] », badge « En attente » ; reformulation « transaction importante » reportée (FR-003-29), hors lot. (source : EXPERIENCE.md:Écrans nouveaux §5 ; textes-nouveaux.md:Hors tableau)
- UX-DR151 : Textes de remplacement des corrections de bugs et retraits techniques (partie B, B-1 à B-31) appliqués écran par écran, liste fermée ; les textes A-004-3 (solde insuffisant à l'échéance) et A-004-4 (« Compte destinataire introuvable ») restent des suppositions du PRD hors lot ; noms accessibles des graphiques à fixer avec leur story. (source : EXPERIENCE.md:Écarts partie B, Points ouverts 6, 11 ; textes-nouveaux.md:Hors tableau)

#### Contraintes golden master côté UI

- UX-DR152 : Le golden master compare le texte rendu (`innerText`) en mode `contains` sur 26 scénarios d'écran (31 applicables = 26 écran + 5 HTTP ; SC-003-03 et SC-010-02 exclus) ; un texte ajouté ne casse rien, un texte legacy modifié ou retiré casse le scénario ; le style est libre. (source : EXPERIENCE.md:Foundation ; textes-nouveaux.md:Périmètre ; SYNTHESE.md:§7.4)
- UX-DR153 : Fenêtre de 5 000 caractères (`DOM_TEXT_MAX_CHARS`) : toute section ou texte ajouté se place après le contenu comparé (« Suivi de mes virements » en bas du Tableau de bord et du Virement ; file de revue après « Gestion des clients ») pour ne pas repousser « Centre de notifications », « CONFIRMATION », « Gestion des clients », « Jean Dupont », « user1 » hors fenêtre. (source : divergences-golden-master.md:Points de vigilance ; EXPERIENCE.md:Écrans nouveaux §3, §4)
- UX-DR154 : Listes déroulantes en `<select>` natifs (sélection par valeur), valeur d'option = identifiant du compte comme le legacy. (source : EXPERIENCE.md:Attributs de test)
- UX-DR155 : Correspondance sélecteurs legacy → `data-testid` cibles (table SC-001-03 à SC-013-02) à implémenter à l'identique, alimentant le `replay-mapping` G2 ; `data-testid` gelés après G2. (source : EXPERIENCE.md:Attributs de test)
- UX-DR156 : Casse CSS : reproduire le `text-transform: uppercase` du legacy sur séparateurs (eyebrow) et en-têtes de tableau concernés (« MES COMPTES DEMO BANK », « TOTAL CLIENTS », « DATE DESCRIPTION TYPE MONTANT SOLDE APRES »), texte source dans sa casse. (source : EXPERIENCE.md:Voice and Tone, Points ouverts 9 ; divergences:Points de vigilance)
- UX-DR157 : Formats numériques et dates du legacy repris écran par écran (anglo-saxon compris), jamais normalisés en « 1 234,56 € » ; emoji conservés. (source : EXPERIENCE.md:Voice and Tone, Points ouverts 8 ; DESIGN.md:components.amount)
- UX-DR158 : Écarts de texte validés, liste fermée, alimentant le `text_map` G2 : partie A (A2, A5–A11), partie B (B-1 à B-31), partie C (entrée groupée unique « textes ajoutés » C1 : « Suivi de mes virements », file de revue, lien d'évitement, « (nouvel onglet) »). Aucun ne touche une ligne attendue, un `wait_for` ou un statut des 31 scénarios applicables. (source : EXPERIENCE.md:Écarts de texte ; divergences-golden-master.md:Scénarios qui divergent encore)
- UX-DR159 : « Telecharger PDF (nouvel onglet) » doit toujours contenir « Telecharger PDF » (`wait_for` de SC-017-02, SC-008-01). (source : textes-nouveaux.md:a11y.nouvel.onglet ; divergences:SC-017-02)
- UX-DR160 : Divergence conditionnelle unique SC-003-04 (virement 100 EUR COURANT → EPARGNE) : en nominal (score 35) le virement est exécuté et redirige avec les 6 lignes attendues ; rejeu déterministe exigé (horloge fixée entre 06:00 et 23:00, données réinitialisées, port Horloge + profil de rejeu, NFR-PAR-05) ; traitement G2 « map » sans text_map, sinon écart permanent. (source : divergences-golden-master.md)
- UX-DR161 : Conditions de rejeu côté UI : base remise au jeu de démonstration (bénéficiaire FR76***, portefeuille de sophie, watchlist BNP.PA/AI.PA), adaptateurs AlphaVantage et Powens simulés ; textes « [EN ATTENTE] » legacy gardés (SC-005-01) ; « OAuth2 PSD2 » du paragraphe fonctionnel conservé (SC-012-01). (source : divergences-golden-master.md:Points de vigilance, SC-005-01, SC-009-01, SC-012-01)
- UX-DR162 : Scénarios HTTP à re-mapper en G2 (chemins `.xhtml`, POST JSF de SC-001-03 avec fragment « Identifiants incorrects » ; 302 sans session sur dashboard, powens-callback, docs/download) : route cible, forme de requête et statut attendu sont des décisions d'architecture. (source : divergences-golden-master.md:Points de vigilance, SC-001-01..03, SC-014-01, SC-017-01)

### FR Coverage Map

Chaque exigence fonctionnelle est rattachée à un seul epic, celui qui la livre. Les stories d'autres epics peuvent la citer.

- FR-001-01 : Epic 2 — Écrans accessibles sans authentification
- FR-001-02 : Epic 2 — Redirection des accès non authentifiés vers la connexion
- FR-001-03 : Epic 2 — Champs obligatoires du formulaire de connexion
- FR-001-04 : Epic 2 — Authentification réussie et ouverture de session
- FR-001-05 : Epic 2 — Écran d'accueil selon le rôle
- FR-001-06 : Epic 2 — Échec d'authentification
- FR-001-07 : Epic 2 — Refus de connexion d'un compte désactivé
- FR-001-08 : Epic 2 — Mots de passe protégés par hachage
- FR-001-09 : Epic 2 — Nouvelle session à la connexion
- FR-001-10 : Epic 2 — Déconnexion
- FR-001-11 : Epic 2 — Accès refusé aux écrans réservés à un autre rôle
- FR-001-12 : Epic 2 — Journalisation des connexions et déconnexions
- FR-001-13 : Epic 2 — Résolution de l'adresse IP du client
- FR-002-01 : Epic 3 — Données de l'utilisateur relues à chaque ouverture
- FR-002-02 : Epic 3 — Utilisateur supprimé pendant sa session
- FR-002-03 : Epic 3 — Solde total
- FR-002-04 : Epic 3 — Liste des comptes
- FR-002-05 : Epic 3 — Dernières opérations du compte courant
- FR-002-06 : Epic 3 — Présentation des opérations
- FR-002-07 : Epic 3 — Centre de notifications de l'utilisateur
- FR-002-08 : Epic 3 — Badge et actions sur les notifications
- FR-002-09 : Epic 3 — Conservation des notifications
- FR-002-10 : Epic 3 — Une seule notification par virement traité
- FR-002-11 : Epic 3 — Navigation du tableau de bord selon le rôle
- FR-002-12 : Epic 8 — Patrimoine total avec les comptes d'autres banques
- FR-003-01 : Epic 3 — Accès au formulaire et comptes proposés
- FR-003-02 : Epic 3 — Les deux comptes sont obligatoires
- FR-003-03 : Epic 3 — Comptes source et destinataire différents
- FR-003-04 : Epic 3 — Montant strictement positif
- FR-003-05 : Epic 3 — Compte introuvable
- FR-003-06 : Epic 3 — Solde insuffisant : refus avec un message clair
- FR-003-07 : Epic 3 — Ordre des contrôles
- FR-003-08 : Epic 3 — Soumission sans étape de confirmation
- FR-003-09 : Epic 3 — Débit, crédit et opérations au relevé
- FR-003-10 : Epic 3 — Virements importants exécutés (statut PENDING)
- FR-003-11 : Epic 3 — Journal d'audit de l'exécution du virement
- FR-003-12 : Epic 3 — Retour au tableau de bord
- FR-003-13 : Epic 3 — Remise au traitement asynchrone
- FR-003-14 : Epic 3 — Notification selon l'issue du contrôle et le montant
- FR-003-15 : Epic 3 — Contrôle de fraude avant la validation finale
- FR-003-16 : Epic 3 — Fin de l'avertissement « N alerte(s) »
- FR-003-17 : Epic 3 — Signal LARGE_AMOUNT (règles voisines)
- FR-003-18 : Epic 3 — Signal HIGH_FREQUENCY sur les transactions du jour
- FR-003-19 : Epic 3 — Signal NEW_BENEFICIARY
- FR-003-20 : Epic 3 — Signal UNUSUAL_HOURS
- FR-003-21 : Epic 3 — Signal ROUND_AMOUNT (règles voisines)
- FR-003-22 : Epic 3 — Signal FOREIGN_IBAN
- FR-003-23 : Epic 3 — Signal RAPID_SUCCESSION (règles voisines)
- FR-003-24 : Epic 3 — Signal VELOCITY_CHECK (règles voisines)
- FR-003-25 : Epic 3 — Enregistrement des alertes au-delà du score 75
- FR-003-26 : Epic 3 — Motif dans la notification de confirmation (règles voisines)
- FR-003-27 : Epic 3 — Écran de refus pour erreur du contrôle
- FR-003-28 : Epic 3 — Reprise préremplie après un refus
- FR-003-29 : Epic 10 — Reformulation des textes de transaction importante
- FR-004-01 : Epic 4 — Accès à l'écran des virements programmés
- FR-004-02 : Epic 4 — Formulaire de création et comptes source proposés
- FR-004-03 : Epic 4 — Liste des virements programmés du client
- FR-004-04 : Epic 4 — Champs obligatoires et format de date du formulaire
- FR-004-05 : Epic 4 — Montant invalide (virement programmé, simulation et demande de prêt)
- FR-004-06 : Epic 4 — Contrôles à l'enregistrement du virement programmé
- FR-004-07 : Epic 4 — Création réussie
- FR-004-08 : Epic 4 — Fréquences proposées
- FR-004-09 : Epic 4 — Suspension, réactivation et suppression
- FR-004-10 : Epic 4 — Contrôle d'appartenance avant suspension, réactivation ou suppression
- FR-004-11 : Epic 4 — Exécution automatique des virements dus
- FR-004-12 : Epic 4 — Solde insuffisant à l'échéance
- FR-004-13 : Epic 4 — Bénéficiaire obligatoirement interne à la banque
- FR-004-14 : Epic 4 — Mise à jour du virement après exécution
- FR-004-15 : Epic 4 — Échec isolé d'un virement pendant un passage
- FR-005-01 : Epic 3 — Accès à l'écran du relevé
- FR-005-02 : Epic 3 — Comptes proposés et sélection initiale
- FR-005-03 : Epic 3 — Changement de compte
- FR-005-04 : Epic 3 — Informations du compte sélectionné
- FR-005-05 : Epic 3 — Chargement de toutes les opérations
- FR-005-06 : Epic 3 — Tableau des opérations
- FR-005-07 : Epic 3 — Opérations en attente
- FR-005-08 : Epic 3 — Liens de navigation selon le rôle
- FR-006-01 : Epic 4 — Données limitées à l'utilisateur connecté
- FR-006-02 : Epic 4 — Choix de la période
- FR-006-03 : Epic 4 — Ventilation des dépenses par catégorie
- FR-006-04 : Epic 4 — Totaux du mois
- FR-006-05 : Epic 4 — Contrôles du formulaire de plafond
- FR-006-06 : Epic 4 — Catégories proposées pour un plafond, sans la catégorie Revenus
- FR-006-07 : Epic 4 — Enregistrement d'un plafond
- FR-006-08 : Epic 4 — Suppression d'un plafond
- FR-006-09 : Epic 4 — Barre de progression par catégorie
- FR-006-10 : Epic 4 — Signalement d'un dépassement
- FR-006-11 : Epic 4 — Tableau des plafonds du mois
- FR-007-01 : Epic 5 — Accès à la page Prêts
- FR-007-02 : Epic 5 — Formulaire du simulateur et contrôles
- FR-007-03 : Epic 5 — Calcul de la simulation
- FR-007-04 : Epic 5 — Types de prêt proposés
- FR-007-05 : Epic 5 — Contrôles de la demande de prêt
- FR-007-06 : Epic 5 — Refus dès la demande d'une durée de 301 à 360 mois
- FR-007-07 : Epic 5 — Enregistrement et transmission d'une demande valide
- FR-007-08 : Epic 5 — Liste des emprunts
- FR-007-09 : Epic 5 — Règles de scoring crédit
- FR-007-10 : Epic 5 — Approbation, refus et notification
- FR-007-11 : Epic 5 — Consultation de l'échéancier d'un prêt
- FR-007-12 : Epic 5 — Paiement d'une échéance
- FR-007-13 : Epic 5 — Suppression d'un prêt
- FR-008-01 : Epic 6 — Redirection vers la connexion des écrans réservés aux utilisateurs authentifiés
- FR-008-02 : Epic 6 — Téléchargement sans session
- FR-008-03 : Epic 6 — Accès à la page documents pour tous les rôles
- FR-008-04 : Epic 6 — Refus d'un document portant sur le compte d'un autre client
- FR-008-05 : Epic 6 — Documents proposés par compte
- FR-008-06 : Epic 6 — Documents fiscaux (IFU N-1 et N-2)
- FR-008-07 : Epic 6 — Relevé mensuel
- FR-008-08 : Epic 6 — RIB
- FR-008-09 : Epic 6 — Attestation de solde
- FR-008-10 : Epic 6 — IFU de l'utilisateur connecté
- FR-008-11 : Epic 6 — Demandes de téléchargement invalides
- FR-008-12 : Epic 6 — Compteur de téléchargements
- FR-009-01 : Epic 7 — Accès à la page bourse
- FR-009-02 : Epic 7 — Chargement initial
- FR-009-03 : Epic 7 — Changement de valeur ou de période
- FR-009-04 : Epic 7 — Indicateurs de marché
- FR-009-05 : Epic 7 — Couleur selon la performance
- FR-009-06 : Epic 7 — Graphique d'évolution du cours
- FR-009-07 : Epic 7 — Dernières séances
- FR-009-08 : Epic 7 — Données demandées au fournisseur selon la période
- FR-009-09 : Epic 7 — Absence de données du fournisseur
- FR-009-10 : Epic 7 — Watchlist unique et partagée
- FR-009-11 : Epic 7 — Formulaire d'ajout et contrôles
- FR-009-12 : Epic 7 — Enregistrement d'une nouvelle valeur
- FR-009-13 : Epic 7 — Suppression d'une valeur
- FR-009-14 : Epic 7 — Clé d'accès au fournisseur de cotations hors du code source
- FR-010-01 : Epic 7 — Accès aux écrans ouverts à tout utilisateur authentifié
- FR-010-02 : Epic 7 — Message de bienvenue personnalisé
- FR-010-03 : Epic 7 — Conversation neuve à chaque ouverture
- FR-010-04 : Epic 7 — Actions rapides
- FR-010-05 : Epic 7 — Envoi d'un message
- FR-010-06 : Epic 7 — Historique transmis au service d'IA
- FR-010-07 : Epic 7 — Consignes de comportement de Sophie
- FR-010-08 : Epic 7 — Raccordement au service d'IA
- FR-010-09 : Epic 7 — Indisponibilité du service d'IA (correction BUG-038)
- FR-010-10 : Epic 7 — Nouvelle conversation
- FR-010-11 : Epic 7 — Comportement de la zone de saisie
- FR-011-01 : Epic 7 — Catalogue en lecture seule
- FR-011-02 : Epic 7 — Chargement et rafraîchissement
- FR-011-03 : Epic 7 — Produits affichés, tri et sections
- FR-011-04 : Epic 7 — Valeurs absentes
- FR-011-05 : Epic 7 — Taux et rendements en pourcentage
- FR-011-06 : Epic 7 — Niveau de risque d'un produit d'investissement
- FR-011-07 : Epic 7 — Valeurs par défaut des produits
- FR-011-08 : Epic 7 — Sections vides
- FR-011-09 : Epic 7 — Historique des traitements nocturnes
- FR-012-01 : Epic 8 — Accès à l'écran Comptes Tiers
- FR-012-02 : Epic 8 — Retour de Powens accessible sans authentification
- FR-012-03 : Epic 8 — État non connecté
- FR-012-04 : Epic 8 — État connecté
- FR-012-05 : Epic 8 — Lancement de la connexion à une banque
- FR-012-06 : Epic 8 — Aiguillage du retour de Powens
- FR-012-07 : Epic 8 — Retour de Powens en erreur (correction BUG-040, BUG-044)
- FR-012-08 : Epic 8 — Session Powens expirée
- FR-012-09 : Epic 8 — Résultat du chargement des comptes après le retour
- FR-012-10 : Epic 8 — Patrimoine total
- FR-012-11 : Epic 8 — Carte d'un compte externe
- FR-012-12 : Epic 8 — Solde absent (correction BUG-042)
- FR-012-13 : Epic 8 — Transactions d'un compte externe
- FR-012-14 : Epic 8 — Service Powens indisponible (correction BUG-043)
- FR-012-15 : Epic 8 — Déconnexion de Powens
- FR-012-16 : Epic 8 — Rafraîchissement des comptes externes
- FR-012-17 : Epic 8 — Paramètres de raccordement à Powens
- FR-013-01 : Epic 4 — Accès à l'écran des bénéficiaires
- FR-013-02 : Epic 4 — Liste des bénéficiaires actifs du client
- FR-013-03 : Epic 4 — Affichage masqué de l'IBAN, BIC, banque et zone SEPA
- FR-013-04 : Epic 4 — Vérification de l'IBAN avant l'ajout
- FR-013-05 : Epic 4 — Contrôles à l'ajout d'un bénéficiaire
- FR-013-06 : Epic 4 — Longueur maximale des champs texte
- FR-013-07 : Epic 4 — Normalisation et enregistrement du bénéficiaire
- FR-013-08 : Epic 4 — Ajout réussi
- FR-013-09 : Epic 4 — Suppression logique d'un bénéficiaire
- FR-013-10 : Epic 4 — Contrôle d'appartenance avant suppression
- FR-014-01 : Epic 8 — Accès sans authentification
- FR-014-02 : Epic 8 — Aiguillage par ordre de priorité
- FR-014-03 : Epic 8 — Redirection systématique
- FR-014-04 : Epic 8 — Retour portant seulement un code
- FR-014-05 : Epic 8 — Session Powens expirée
- FR-014-06 : Epic 8 — Connexion établie
- FR-014-07 : Epic 8 — Remise à zéro de la session Powens
- FR-014-08 : Epic 8 — Vingt transactions au plus par compte externe
- FR-015-01 : Epic 9 — Accès à l'espace conseiller selon le rôle
- FR-015-02 : Epic 9 — Chargement du portefeuille du conseiller connecté
- FR-015-03 : Epic 9 — Indicateurs du portefeuille
- FR-015-04 : Epic 9 — Portefeuille vide
- FR-015-05 : Epic 9 — Carte client
- FR-015-06 : Epic 9 — Indicateur de prêts sur la carte client
- FR-015-07 : Epic 9 — Panneau de détail d'un client
- FR-015-08 : Epic 9 — Client sans compte ou sans prêt
- FR-015-09 : Epic 9 — Valeurs de référence affichées
- FR-016-01 : Epic 2 — Accès à l'administration réservé au rôle ADMIN
- FR-016-02 : Epic 9 — Indicateurs de l'administration
- FR-016-03 : Epic 5 — Pipeline des prêts vide
- FR-016-04 : Epic 5 — Approbation d'un prêt
- FR-016-05 : Epic 5 — Refus d'un prêt
- FR-016-06 : Epic 9 — Journal d'audit des actions administratives
- FR-016-07 : Epic 5 — Erreur lors de l'approbation ou du refus d'un prêt
- FR-016-08 : Epic 9 — Liste et recherche des clients
- FR-016-09 : Epic 9 — Panneau de détail d'un client
- FR-016-10 : Epic 9 — Date « Client depuis »
- FR-016-11 : Epic 9 — Activation et désactivation d'un client
- FR-016-12 : Epic 5 — Historique de tous les prêts
- FR-016-13 : Epic 9 — Tous les utilisateurs du système
- FR-016-14 : Epic 9 — Tableau des alertes fraude et actions selon le statut
- FR-016-15 : Epic 9 — Revue d'une alerte fraude
- FR-016-16 : Epic 9 — Affichage de toutes les alertes fraude
- FR-016-17 : Epic 9 — Journal des traitements périodiques
- FR-016-18 : Epic 9 — Catalogue des produits bancaires
- FR-016-19 : Epic 3 — Revue des virements suspendus
- FR-017-01 : Epic 6 — Session obligatoire pour télécharger
- FR-017-02 : Epic 6 — Type de document
- FR-017-03 : Epic 6 — Identifiant de compte
- FR-017-04 : Epic 6 — PDF du relevé de compte
- FR-017-05 : Epic 6 — PDF du RIB
- FR-017-06 : Epic 6 — PDF de l'attestation de solde
- FR-017-07 : Epic 6 — PDF de l'IFU
- FR-017-08 : Epic 6 — Refus d'un compte appartenant à un autre client
- FR-017-09 : Epic 6 — Compte ou client introuvable
- FR-017-10 : Epic 6 — Échec de génération sans détail interne
- FR-017-11 : Epic 6 — Format de la réponse PDF
- FR-018-01 : Epic 4 — Déclenchement mensuel du crédit d'intérêts
- FR-018-02 : Epic 4 — Comptes concernés et calcul de l'intérêt
- FR-018-03 : Epic 4 — Crédit du compte et opération d'intérêts
- FR-018-04 : Epic 4 — Journal d'exécution du traitement
- FR-018-05 : Epic 4 — Consultation des exécutions par l'ADMIN
- FR-019-01 : Epic 4 — Exécution quotidienne à 02:30, sans rattrapage
- FR-019-02 : Epic 4 — Transactions retenues
- FR-019-03 : Epic 4 — Fenêtre de 24 heures exactes
- FR-019-04 : Epic 4 — Une entrée de journal par exécution
- FR-019-05 : Epic 4 — Aucune alerte de fraude créée
- FR-019-06 : Epic 4 — Erreur pendant le traitement
- FR-020-01 : Epic 4 — Déclenchement mensuel
- FR-020-02 : Epic 4 — Journal d'une exécution réussie
- FR-020-03 : Epic 4 — Journal d'une exécution en échec
- FR-020-04 : Epic 4 — Pas de rattrapage des exécutions manquées
- FR-021-01 : Epic 4 — Déclenchement périodique
- FR-021-02 : Epic 4 — Sélection des virements dus
- FR-021-03 : Epic 4 — Débit du compte source et crédit du compte bénéficiaire
- FR-021-04 : Epic 4 — Compte bénéficiaire hors de la banque refusé avant tout débit
- FR-021-05 : Epic 4 — Opération DEBIT sur le compte source
- FR-021-06 : Epic 4 — Opération CREDIT sur le compte bénéficiaire interne
- FR-021-07 : Epic 4 — Suivi des exécutions
- FR-021-08 : Epic 4 — Calcul de la prochaine échéance sans dérive
- FR-021-09 : Epic 4 — Échec d'un virement isolé des autres
- FR-021-10 : Epic 4 — Contrôle de fraude à l'exécution de chaque échéance
- FR-022-01 : Epic 4 — Analyse périodique des comptes des clients actifs
- FR-022-02 : Epic 4 — Détection d'un solde faible
- FR-022-03 : Epic 4 — Contenu de la notification de solde faible
- FR-022-04 : Epic 4 — Plafond de 20 notifications de solde faible par compte et par jour
- FR-022-05 : Epic 4 — Tolérance aux erreurs de l'analyse
- FR-022-06 : Epic 4 — Trace de chaque exécution dans le journal des traitements
- FR-023-01 : Epic 3 — Remise du virement au traitement asynchrone
- FR-023-02 : Epic 3 — Échec de la remise : la validation du virement échoue et le client est informé
- FR-023-03 : Epic 3 — Notification selon l'issue du contrôle et le montant
- FR-023-04 : Epic 3 — Virement au-delà du seuil de fraude
- FR-023-05 : Epic 3 — Plafond de notifications par utilisateur
- FR-023-06 : Epic 3 — Conservation des notifications
- FR-024-01 : Epic 5 — Transmission de la demande au service de scoring crédit
- FR-024-02 : Epic 5 — Échec de la transmission de la demande
- FR-024-03 : Epic 5 — Refus pour durée excessive
- FR-024-04 : Epic 5 — Plafond de montant par type de prêt
- FR-024-05 : Epic 5 — Refus d'un type de prêt non reconnu
- FR-024-06 : Epic 5 — Effets de l'approbation
- FR-024-07 : Epic 5 — Effets du refus
- FR-024-08 : Epic 5 — Demande reçue pour un prêt déjà traité
- FR-024-09 : Epic 5 — Nombre maximal de notifications par utilisateur
- FR-025-01 : Epic 3 — Notification de virement selon l'issue du contrôle et le montant
- FR-025-02 : Epic 3 — Motif dans la notification de confirmation
- FR-025-03 : Epic 5 — Notifications de décision sur une demande de prêt
- FR-025-04 : Epic 3 — Plafond de notifications par utilisateur
- FR-025-05 : Epic 3 — Affichage sur le tableau de bord
- FR-025-06 : Epic 3 — Tout marquer comme lu
- FR-025-07 : Epic 3 — Tout effacer
- FR-025-08 : Epic 4 — Alerte de solde faible affichée au client
- FR-026-01 : Epic 2 — Audit des connexions, échecs de connexion et déconnexions
- FR-026-02 : Epic 3 — Audit des actions d'administration (déplacée de l'epic 9 : la libération et le rejet d'un virement suspendu, FR-016-19, sont audités avec leur motif ; les actions d'administration des epics 5 et 9 ajoutent leurs codes)
- FR-026-03 : Epic 3 — Audit des virements
- FR-026-04 : Epic 2 — Entrée d'audit indépendante de l'opération
- FR-026-05 : Epic 2 — Adresse IP d'origine pour toute action auditée
- FR-026-06 : Epic 2 — Journal en écriture seule
- FR-027-01 : Epic 3 — Évaluation des huit signaux pour chaque virement
- FR-027-02 : Epic 3 — Contrôle avant la validation finale et issue du virement
- FR-027-03 : Epic 3 — Enregistrement des alertes au-delà du score 75
- FR-027-04 : Epic 3 — Avis au client d'un virement suspendu
- FR-027-05 : Epic 3 — Signal LARGE_AMOUNT de score 40
- FR-027-06 : Epic 3 — Signal HIGH_FREQUENCY sur les transactions du jour
- FR-027-07 : Epic 3 — Signal NEW_BENEFICIARY
- FR-027-08 : Epic 3 — Signal UNUSUAL_HOURS
- FR-027-09 : Epic 3 — Signal FOREIGN_IBAN
- FR-027-10 : Epic 3 — Signal VELOCITY_CHECK
- FR-027-11 : Epic 3 — Signal MULTIPLE_FAILED_AUTH après échecs de connexion
- FR-027-12 : Epic 9 — Consultation des alertes par l'ADMIN
- FR-027-13 : Epic 9 — Confirmation d'une alerte
- FR-027-14 : Epic 9 — Classement en faux positif
- FR-027-15 : Epic 3 — IBAN masqué dans les journaux
- FR-027-16 : Epic 3 — États d'un virement
- FR-027-17 : Epic 3 — Échéance de revue et rejet automatique
- FR-027-18 : Epic 3 — Cumul glissant des virements d'un client
- FR-028-01 : Epic 8 — Initiation de la connexion Powens
- FR-028-02 : Epic 8 — Échec de l'initiation
- FR-028-03 : Epic 8 — Traitement du retour de Powens
- FR-028-04 : Epic 8 — Retour portant seulement un code
- FR-028-05 : Epic 8 — Jeton absent au retour
- FR-028-06 : Epic 8 — Résultat du chargement des comptes
- FR-028-07 : Epic 8 — Transactions d'un compte sélectionné
- FR-028-08 : Epic 8 — Rafraîchissement des comptes
- FR-028-09 : Epic 8 — Solde absent et patrimoine total
- FR-028-10 : Epic 8 — Déconnexion
- FR-028-11 : Epic 8 — Réponse en erreur de Powens
- FR-028-12 : Epic 8 — Configuration et délais
- FR-029-01 : Epic 7 — Série demandée selon la période
- FR-029-02 : Epic 7 — Filtrage et tri des points
- FR-029-03 : Epic 7 — Réponses du fournisseur en échec
- FR-029-04 : Epic 7 — Adresse du fournisseur configurable
- FR-029-05 : Epic 7 — Clé d'accès protégée
- FR-029-06 : Epic 7 — Période par défaut et première valeur
- FR-029-07 : Epic 7 — Calculs et indicateur de variation
- FR-029-08 : Epic 7 — Dernières séances et graphique
- FR-029-09 : Epic 7 — Contrôles à l'ajout d'une valeur
- FR-029-10 : Epic 7 — Normalisation et sélection après ajout
- FR-029-11 : Epic 7 — Ordre de la watchlist
- FR-029-12 : Epic 7 — Suppression d'une valeur sélectionnée
- FR-029-13 : Epic 7 — Longueur du symbole et du nom
- FR-029-14 : Epic 7 — Marchés et devises autorisés
- FR-030-01 : Epic 2 — Accès public limité et redirection des accès non authentifiés
- FR-030-02 : Epic 2 — Espace conseiller réservé aux rôles CONSEILLER et ADMIN
- FR-030-03 : Epic 2 — Écran d'accueil selon le rôle après connexion
- FR-030-04 : Epic 2 — Liens de navigation conditionnés par le rôle
- FR-030-05 : Epic 2 — Page expirée
- FR-030-06 : Epic 6 — Téléchargement de documents réservé aux utilisateurs authentifiés
- FR-030-07 : Epic 2 — Contrôle des rôles dans les services métier
- FR-030-08 : Epic 2 — Nouvelle session après authentification

### Exigences UX sans story (absences justifiées)

- UX-DR18, UX-DR19, UX-DR25, UX-DR26, UX-DR30, UX-DR31, UX-DR35, UX-DR36, UX-DR37, UX-DR38, UX-DR41, UX-DR52 : composants du design system (DESIGN.md) qu'aucun écran du périmètre n'utilise. UX-DR25 et UX-DR52 sont décrits comme « disponibles, aucun usage imposé ». Chacun sera livré par la première story qui en aura besoin, selon sa définition dans DESIGN.md.
- UX-DR162 : décisions de re-mapping des scénarios HTTP, prises à l'étape G2 et inscrites dans `replay-mapping.json` (ecarts-voulus-golden-master.md). Aucun travail de story.

## Epic List

<!-- Découpage approuvé le 2026-10-05 -->

Epic | Titre | FR | Scénarios golden master
--- | --- | --- | ---
0 | Redressement d'architecture | 0 | aucun
1 | Fondations contrôlées | 0 | aucun
2 | Se connecter et accéder à son espace selon son rôle | 25 | SC-001-01 à SC-001-05
3 | Consulter ses comptes et virer de l'argent en sécurité | 77 | SC-002-01, SC-002-02, SC-003-01, SC-003-02, SC-003-04, SC-005-01
4 | Programmer ses virements, gérer bénéficiaires et budget, traitements de nuit | 68 | SC-004-01 à SC-004-03, SC-006-01, SC-013-01, SC-013-02
5 | Demander et suivre un prêt | 28 | SC-007-01, SC-007-02
6 | Obtenir ses documents bancaires | 24 | SC-008-01, SC-017-01, SC-017-02
7 | Bourse, conseiller virtuel et catalogue des produits | 48 | SC-009-01, SC-010-01, SC-011-01
8 | Agréger ses comptes d'autres banques (Powens) | 38 | SC-012-01, SC-014-01
9 | Espace conseiller et administration | 24 | SC-015-01, SC-015-02, SC-016-01, SC-016-02
10 | Recette finale et bascule | 1 | les 31 scénarios (rejeu final) ; SC-003-03 et SC-010-02 exclus (BUG-011, BUG-038)

## Epic 0: Redressement d'architecture

<!-- Ajouté le 2026-10-08 par bmad-correct-course, voir sprint-change-proposal-2026-10-08.md -->

Cet epic corrige deux faiblesses relevées par arch-eval sur le commit `d23bf7d` (tag `v0-demo`) avant que les epics suivants ne les amplifient. Il ne porte aucune exigence fonctionnelle : aucun écran, aucun point d'API et aucun scénario du golden master ne change de comportement. Il s'exécute avant l'epic 4, sur `main`, un seul flux. À sa fin, les critères 1.2 et 1.3 d'arch-eval remontent sans qu'aucun autre critère baisse.

Trois recommandations d'arch-eval ont été **écartées en connaissance de cause**, parce qu'elles reviennent sur des décisions prises exprès. Elles ne doivent pas être rouvertes par une story :

- **Module technique partagé entre services** : AD-17 pose « pas de bibliothèque partagée entre services », avec la duplication et un test de référence commun comme garde-fou retenu. Sortir `HorlogeSysteme` du périmètre scanné affaiblirait de plus la preuve d'AD-19.
- **Verrous multi-instance sur l'outbox et les `@Scheduled`** : AD-21 pose « une seule instance par service, aucun verrou distribué » et la section *Deferred* de la spine reporte la haute disponibilité. Le double envoi est déjà absorbé par les consommateurs idempotents d'AD-18 et le `versionAgregat` d'AD-22.
- **Révocation du jeton et limitation des tentatives** : contredit RP-05, tranché le 2026-10-05 (comportement du legacy conservé, l'alerte `MULTIPLE_FAILED_AUTH` couvre les échecs répétés). Les revendications `iss` et `aud` restent hors du contrat du jeton d'AD-16.

### Story 0.1: Confiner le jeton dans le décorateur fetch du front

En tant que développeur de l'équipe,
je veux que le jeton d'authentification ne soit porté que par le décorateur `fetchAuthentifie`,
afin qu'un client HTTP ne puisse plus être câblé sans jeton, comme c'est arrivé sur le tableau de bord, le virement, le relevé et la revue.

**Exigences :** NFR-SEC-03, NFR-SEC-07, AR-33, AR-34, AR-106, AR-95
**Scénarios golden master :** aucun
**Prérequis :** epics 2 et 3 terminés ; correctif `d23bf7d` en place (`fetchAuthentifie` pose déjà l'en-tête `Authorization` sur tous les appels authentifiés)

**Acceptance Criteria:**

**Given** le dépôt
**When** je cherche le jeton dans `frontend/src/domain` et `frontend/src/application`
**Then** aucun type, aucun port, aucun cas d'usage et aucune signature de ces deux dossiers n'accepte, ne lit, ne stocke ni ne transporte le jeton
**And** aucun de ces fichiers ne mentionne `Authorization`, `Bearer` ni `nb.jeton`

**Given** le dépôt
**When** je cherche quels fichiers posent l'en-tête `Authorization`
**Then** `frontend/src/adapters/session/fetchAuthentifie.ts` est le seul

**Given** la composition du front
**When** je lis `frontend/src/infrastructure/main.tsx`
**Then** tout client HTTP qui appelle un service authentifié reçoit `fetchAuthentifie`, et un test de la composition échoue si l'un d'eux reçoit `fetch` directement

**Given** une violation introduite volontairement : un client HTTP de `main.tsx` câblé avec `fetch` au lieu de `fetchAuthentifie`
**When** je lance `cd frontend && npx vitest run`
**Then** le code de sortie est non nul et le test nomme le client fautif

**Given** une violation introduite volontairement : un cas d'usage de `frontend/src/application` dont la signature reçoit le jeton
**When** je lance `cd frontend && npx vitest run`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run --coverage` puis `cd frontend && npx tsc --noEmit`
**Then** les deux commandes sortent en code 0, et les seuils de couverture de `docs/quality-gates.md` sont tenus sans exclusion ajoutée

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** les quatre commandes sortent en code 0
**And** `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est vide

### Story 0.2: Porter les montants et les identifiants par des types du domaine

En tant que développeur de l'équipe,
je veux des types `Montant`, `CompteId` et `Motif` et un objet de demande pour les fabriques de `Virement`,
afin que les règles de montant et d'identifiant soient portées une fois par le domaine au lieu d'être répétées à chaque appel.

**Exigences :** NFR-SEC-03, AR-34, AR-50, AR-66, FR-027-16
**Scénarios golden master :** aucun (comportement inchangé)
**Prérequis :** epic 3 terminé ; story 0.1 terminée

**Acceptance Criteria:**

**Given** le domaine de `coeur-bancaire`
**When** je lis le type `Montant`
**Then** il enveloppe un `BigDecimal` à 2 décimales, refuse à la construction une échelle différente ou une valeur négative là où le métier l'interdit, et n'expose aucun constructeur contournant ces règles
**And** la représentation JSON reste la chaîne `"100.00"` et la colonne reste `numeric(19,2)`

**Given** le domaine de `coeur-bancaire`
**When** je lis les types `CompteId` et `Motif`
**Then** `CompteId` enveloppe l'identifiant numérique échangé entre services et `Motif` porte les règles du libellé, sans qu'aucun des deux n'expose le numéro de compte

**Given** les fabriques de `Virement`
**When** je lis leur signature
**Then** elles reçoivent un objet de demande et non une liste de paramètres primitifs, et aucune fabrique de `Virement` ne prend plus de 4 paramètres

**Given** un service qui vérifie le rôle `ADMIN`
**When** je lis ses cas d'usage
**Then** le contrôle du rôle est appelé depuis un point unique du service, chaque service continuant de vérifier lui-même les rôles comme l'impose AD-16, sans aucune bibliothèque partagée entre services

**Given** la story terminée
**When** je lance `mvn -B -q clean verify -DfailIfNoTests=true`
**Then** le code de sortie est 0, et les seuils de 100 % sur `domain` et `application` sont tenus sans exclusion ajoutée

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** les quatre commandes sortent en code 0
**And** `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est vide

## Epic 1: Fondations contrôlées

À la fin de cet epic, l'équipe dispose d'un squelette à deux services hexagonaux (`coeur-bancaire`, `notifications`) et d'un front hexagonal, sur lequel tous les contrôles de `docs/quality-gates.md` sont livrés : les contrôles bloquants (architecture, couverture, tracing, CVE) sont verts, les contrôles à la demande (mutation) sont exécutables et verts. Un flux témoin traverse les deux services par Kafka et sa trace est vérifiée dans Jaeger. Aucune story fonctionnelle ne démarre avant la fin de cet epic ; à sa fin, `scripts/gates/check-gates-exist.sh` sort en code 0 et l'humain peut figer l'empreinte des contrôles (étapes 10 et 11 du pilote).

### Story 1.1: Poser le squelette des deux services et du front

En tant que développeur de l'équipe,
je veux un POM parent, deux services hexagonaux minimaux et un front hexagonal minimal qui se construisent et se testent,
afin que les contrôles des stories suivantes s'appliquent à du code réel et que le build soit vert dès le départ.

**Exigences :** NFR-QG-00, AR-1, AR-2, AR-3, AR-4, AR-93, AR-95
**Scénarios golden master :** aucun
**Prérequis :** aucun

**Acceptance Criteria:**

**Given** le dépôt
**When** je liste les fichiers
**Then** `pom.xml` (POM parent, Java 21, Spring Boot 4.1.1 selon `versions-verifiees.md`) existe à la racine et déclare les deux modules `services/coeur-bancaire` et `services/notifications`, et rien d'autre sous `services/`
**And** chaque service a pour paquet racine `fr.nextbanking.coeurbancaire` ou `fr.nextbanking.notifications`, et ses cinq paquets `domain`, `application`, `adapter.in`, `adapter.out`, `infrastructure` contiennent chacun au moins une classe, dont au moins un type `public` sous `adapter` (exigé par `scripts/gates/canary-arch.sh`)
**And** `services/<service>/pom.xml` hérite du POM parent, de sorte que `mvn -q -f services/<service>/pom.xml help:effective-pom` sort en code 0 pour chacun des deux services

**Given** le dépôt
**When** je lance `mvn -B -q verify -DfailIfNoTests=true` à la racine
**Then** le code de sortie est 0, chaque module ayant au moins un test exécuté

**Given** un module de service dont on retire volontairement tous les tests
**When** je lance `mvn -B -q verify -DfailIfNoTests=true`
**Then** le code de sortie est non nul

**Given** un test unitaire volontairement faux ajouté dans le `domain` de `coeur-bancaire`
**When** je lance `mvn -B -q verify -DfailIfNoTests=true`
**Then** le code de sortie est non nul

**Given** le dépôt
**When** je liste `frontend/`
**Then** `frontend/src/domain`, `frontend/src/application`, `frontend/src/adapters` et `frontend/src/infrastructure` existent et contiennent chacun au moins un fichier TypeScript ; tout ce qui est React est sous `src/adapters`, le point d'entrée sous `src/infrastructure`
**And** `frontend/package-lock.json` est versionné (`git ls-files frontend/package-lock.json` non vide)
**And** `frontend/vite.config.ts` existe (avec `@vitejs/plugin-react`), sans seuil de couverture ni proxy (ajoutés par les stories 1.3 et 1.4), et aucun `frontend/vitest.config.*` n'existe (`ls frontend/vitest.config.* 2>/dev/null` vide)
**And** `vitest` et `@vitest/coverage-v8` sont installés à la même version (5.0.3 selon `versions-verifiees.md`)
**And** la story ne porte rien du thème (pas de `npm run tokens`, AD-13)

**Given** le dépôt
**When** je lance `cd frontend && npm ci --silent && npx vitest run`
**Then** le code de sortie est 0, au moins un test étant exécuté

**Given** un test Vitest volontairement faux ajouté dans `frontend/src/domain`
**When** je lance `cd frontend && npx vitest run`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `scripts/gates/golden-master.sh`
**Then** le code de sortie est 0
**And** `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est vide

### Story 1.2: Rendre l'architecture hexagonale vérifiable et bloquante

En tant que responsable de la qualité,
je veux qu'un test ArchUnit dans chaque service et dependency-cruiser sur le front bloquent le build dès qu'une dépendance viole l'hexagone,
afin qu'aucun framework ni adaptateur n'entre dans le domaine ou l'application, sans dépendre de la relecture d'un agent.

**Exigences :** NFR-QG-01, NFR-QG-02, NFR-QG-10, AR-5, AR-6, AR-7, AR-8, AR-15, AR-27, AR-44, AR-45, AR-94
**Scénarios golden master :** aucun
**Prérequis :** story 1.1 terminée. Le port `application.port.out.Horloge` (AD-19 : `instant()`, `aujourdhui()`, `fuseau()`, fuseau `nb.fuseau=Europe/Paris` validé au démarrage) est livré ici avec son adaptateur `adapter.out.horloge` en horloge système, dans les deux services (AR-8). Le profil `rejeu` (horloge réglée par `HORLOGE_DEPART` / `HORLOGE_DECALAGE`) est livré par le virement de base de l'epic 3.

**Acceptance Criteria:**

**Given** chaque service
**When** je liste `services/<service>/src/test`
**Then** une classe `HexagonalArchitectureTest` porte exactement deux règles : `Architectures.onionArchitecture()` (`domainModels("..domain..")`, `applicationServices("..application..")`, adaptateurs `adapter.in`, `adapter.out`, `infrastructure`) et aucun framework dans domain et application (`resideOutsideOfPackages("java..", "<racine>.domain..", "<racine>.application..")`)
**And** les classes séparées `HorlogeArchitectureTest` (aucun `now(..)` d'un type `java.time`, aucune dépendance à `java.time.Clock` hors de `..adapter.out.horloge..` et `..adapter.out.jeton..`) et `ClientsHttpArchitectureTest` (aucune classe hors de `..adapter.out.fournisseur..` ne dépend de `RestClient`, `WebClient`, `RestTemplate`, `java.net.http.HttpClient`) existent
**And** `archRule.failOnEmptyShould` reste à `true` et la dépendance `archunit-junit5` (1.5.1) est déclarée

**Given** le dépôt
**When** je lance `mvn -B -q test -Dtest='*ArchitectureTest'`
**Then** le code de sortie est 0

**Given** le dépôt
**When** je lance `scripts/gates/canary-arch.sh services/coeur-bancaire fr/nextbanking/coeurbancaire` puis `scripts/gates/canary-arch.sh services/notifications fr/nextbanking/notifications`
**Then** chacun sort en code 0 avec la ligne `OK  ArchUnit détecte la dépendance domaine -> adaptateur`

**Given** une violation introduite volontairement : un import `org.springframework.stereotype.Component` dans une classe de `domain` de `notifications`
**When** je lance `mvn -B -q test -Dtest='*ArchitectureTest'`
**Then** le code de sortie est non nul

**Given** une violation introduite volontairement : un appel `Instant.now()` dans une classe de `domain` de `coeur-bancaire`
**When** je lance `mvn -B -q test -Dtest='*ArchitectureTest'`
**Then** le code de sortie est non nul (échec de `HorlogeArchitectureTest`)

**Given** une violation introduite volontairement : une classe de `adapter.in` de `coeur-bancaire` qui dépend de `org.springframework.web.client.RestClient`
**When** je lance `mvn -B -q test -Dtest='*ArchitectureTest'`
**Then** le code de sortie est non nul (échec de `ClientsHttpArchitectureTest`)

**Given** le dépôt
**When** je lis `frontend/.dependency-cruiser.cjs` (créé par cette story, seule story autorisée à le créer)
**Then** il porte les règles `domain-ne-depend-de-rien`, `application-ne-depend-pas-des-adaptateurs` et `hexagone-sans-paquet-npm` (types `npm`, `npm-dev`, `npm-optional`, `npm-peer`, `npm-bundled`, `npm-no-pkg`, `npm-unknown`, `core` ; fichiers `*.test.ts(x)` exclus), toutes en sévérité `error`, avec `options.tsPreCompilationDeps: true`
**And** `dependency-cruiser` (18.5.0) figure dans `frontend/package.json`

**Given** le dépôt
**When** je lance `cd frontend && npx depcruise --config .dependency-cruiser.cjs src`
**Then** le code de sortie est 0

**Given** une violation introduite volontairement : `import { useState } from "react"` dans un fichier de `frontend/src/application`
**When** je lance `cd frontend && npx depcruise --config .dependency-cruiser.cjs src`
**Then** le code de sortie est non nul

**Given** une violation introduite volontairement : un fichier de `frontend/src/domain` qui importe un module de `src/adapters`
**When** je lance `cd frontend && npx depcruise --config .dependency-cruiser.cjs src`
**Then** le code de sortie est non nul

**Given** le dépôt
**When** je lance `cd frontend && npx vitest run src/architecture-temps.test.ts`
**Then** le code de sortie est 0 ; le test parcourt les sources de `src/domain` et `src/application` (hors fichiers de test) et échoue sur `Date.now()`, `new Date()` sans argument et `Temporal.Now`

**Given** une violation introduite volontairement : `const d = new Date();` dans un fichier de `frontend/src/domain`
**When** je lance `cd frontend && npx vitest run src/architecture-temps.test.ts`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/check-gates-exist.sh`
**Then** la sortie contient, pour `coeur-bancaire` et `notifications`, `OK  <service> : test ArchUnit présent` et `OK  <service> : dépendance archunit-junit5`, ainsi que `OK  front : règles dependency-cruiser en error`, `OK  front : règle sans paquet npm dans l'hexagone` et `OK  front : dependency-cruiser installé` (les autres lignes peuvent rester KO jusqu'à la story 1.6)

**Given** la story terminée
**When** je lance `scripts/gates/golden-master.sh`
**Then** le code de sortie est 0
**And** `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est vide (seul fichier protégé créé : `frontend/.dependency-cruiser.cjs`)

### Story 1.3: Imposer la couverture par couche

En tant que responsable de la qualité,
je veux que JaCoCo et Vitest refusent le build quand la couverture d'une couche passe sous son seuil,
afin que le domaine et l'application restent entièrement testés et les adaptateurs à 80 %.

**Exigences :** NFR-QG-03, NFR-QG-04, NFR-QG-10, AR-9, AR-95
**Scénarios golden master :** aucun
**Prérequis :** stories 1.1 et 1.2 terminées

**Acceptance Criteria:**

**Given** le dépôt
**When** je lis le POM parent
**Then** `jacoco-maven-plugin` (0.8.15) y est configuré une seule fois, avec les exécutions `prepare-agent`, `report` et `check` (phase verify) : `<element>PACKAGE</element>`, `<include>*.domain*</include>` et `<include>*.application*</include>` à `<minimum>1.00</minimum>` en lignes et en branches, `*.adapter*` et `*.infrastructure*` à `<minimum>0.80</minimum>`
**And** aucun POM de service ne redéclare JaCoCo, `haltOnFailure` n'est jamais à `false`, et aucune exclusion de couverture n'est déclarée

**Given** le dépôt
**When** je lis `frontend/vite.config.ts`
**Then** le bloc `test.coverage` (provider `v8`) porte des `thresholds` par glob : `src/domain/**` et `src/application/**` à 100 (lines, branches, functions, statements), `src/adapters/**` et `src/infrastructure/**` à 80
**And** `ls frontend/vitest.config.* 2>/dev/null` est vide

**Given** le dépôt
**When** je lance `mvn -B -q verify -DfailIfNoTests=true`
**Then** le code de sortie est 0

**Given** une violation introduite volontairement : une branche non testée ajoutée à une classe de `domain` de `notifications` (un `if` dont une issue n'est couverte par aucun test)
**When** je lance `mvn -B -q verify -DfailIfNoTests=true`
**Then** le code de sortie est non nul et le rapport JaCoCo nomme le paquet sous le seuil

**Given** le dépôt
**When** je lance `cd frontend && npx vitest run --coverage`
**Then** le code de sortie est 0

**Given** une violation introduite volontairement : une fonction non testée ajoutée dans `frontend/src/application`
**When** je lance `cd frontend && npx vitest run --coverage`
**Then** le code de sortie est non nul

**Given** une violation introduite volontairement : `<haltOnFailure>false</haltOnFailure>` ajouté à la configuration JaCoCo
**When** je lance `scripts/gates/check-gates-exist.sh`
**Then** la sortie contient `KO  coeur-bancaire : haltOnFailure désactivé` et `KO  notifications : haltOnFailure désactivé`, et le code de sortie est non nul

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/check-gates-exist.sh`
**Then** la sortie contient, pour chaque service, `OK  <service> : plugin JaCoCo`, `OK  <service> : goal JaCoCo check lié au build`, `OK  <service> : seuil 100 % présent`, `OK  <service> : seuil 80 % présent`, aucune ligne `haltOnFailure désactivé`, ainsi que `OK  front : seuils de couverture Vitest` et `OK  front : seuil dédié au domaine`

**Given** la story terminée
**When** je lance `scripts/gates/golden-master.sh`
**Then** le code de sortie est 0
**And** `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est vide

### Story 1.4: Faire traverser les deux services par un flux témoin tracé

En tant que responsable de la qualité,
je veux un flux témoin `POST /api/coeur-bancaire/temoin` → base de `coeur-bancaire` → outbox → Kafka → `notifications` → base de `notifications`, lancé par `compose.yaml` et dont la trace est vérifiée par `scripts/gates/trace-smoke.sh`,
afin de prouver, avant toute story fonctionnelle, que le contexte de trace se propage sur HTTP, JDBC et Kafka entre deux services.

**Exigences :** NFR-QG-09, NFR-OBS-01, NFR-SEC-06, AR-10, AR-11, AR-12, AR-15, AR-26, AR-29, AR-31, AR-38, AR-39, AR-41, AR-51, AR-52, AR-53, AR-54, AR-55, AR-94, AR-95
**Scénarios golden master :** aucun
**Prérequis :** stories 1.1 à 1.3 terminées. Supposition de la spine (AD-15) : `POST /api/coeur-bancaire/temoin` figure dans la liste fermée des chemins publics (AR-36), pour que `trace-smoke.sh` reste vert quand la vérification du jeton arrive. L'enveloppe Kafka (AR-39) date `horodatage` par le port Horloge livré en story 1.2.

**Acceptance Criteria:**

**Given** le dépôt
**When** je lance `docker compose -f compose.yaml config --services`
**Then** le code de sortie est 0, sans `.env` présent, et la liste contient `coeur-bancaire`, `notifications`, `postgres-coeur-bancaire`, `postgres-notifications`, Kafka, le collecteur OpenTelemetry (nom contenant `otel` ou `collector`), `jaeger` et `web`
**And** `docker compose -f compose.yaml config --services | grep -c '^postgres-'` vaut `2`
**And** `docker compose -f compose.yaml config --format json | jq '[.services[] | select(.healthcheck == null)] | length'` vaut `0`
**And** `! grep -nE 'jaegertracing/all-in-one' compose.yaml` sort en code 0
**And** les images sont celles d'AR-53 (`postgres:18.6`, `apache/kafka:4.2.2`, `otel/opentelemetry-collector:0.162.0` avec `config/otelcol/config.yaml` monté sur `/etc/otelcol/config.yaml`, `cr.jaegertracing.io/jaegertracing/jaeger:2.21.0`, `eclipse-temurin:21` / `eclipse-temurin:21-jre`, `nginx:1.30.5-alpine`), `TZ=Europe/Paris` dans chaque conteneur, `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`
**And** le tag `node:24.21.0-alpine` de `frontend/Dockerfile` et les noms d'artefacts Flyway Boot 4 (`spring-boot-starter-flyway`, `flyway-database-postgresql`) sont vérifiés et consignés dans `versions-verifiees.md`

**Given** le dépôt
**When** je résous la configuration compose
**Then** chaque service applicatif a un `SPRING_DATASOURCE_URL` en `jdbc:postgresql://postgres-<identifiant>:5432/...` vers sa seule base : `docker compose -f compose.yaml config --format json | jq -r '.services | to_entries[] | select(.key|test("^postgres-")|not) | select(.value.environment.SPRING_DATASOURCE_URL) | "\(.key) \(.value.environment.SPRING_DATASOURCE_URL)"' | awk '$2 !~ "//postgres-"$1"[:/]" {print "KO", $0}'` n'affiche rien
**And** chaque base a un utilisateur et un mot de passe propres et un volume nommé `pgdata-<identifiant>`

**Given** le dépôt
**When** je lance `make secrets` puis `git ls-files .env .secrets`
**Then** `.env` est généré (mots de passe aléatoires des bases), `.env.example` est versionné sans valeur, et `git ls-files .env .secrets` est vide
**And** les variables secrètes de `compose.yaml` s'interpolent en `${NB_..._MOT_DE_PASSE:-}`, jamais `${VAR:?}`

**Given** une violation introduite volontairement : le mot de passe de la base de `coeur-bancaire` absent de l'environnement
**When** je démarre `coeur-bancaire`
**Then** le service refuse de démarrer (code de sortie du conteneur non nul)

**Given** chaque service
**When** je lis sa configuration
**Then** elle pose explicitement `management.opentelemetry.tracing.export.otlp.endpoint=http://otel-collector:4318/v1/traces`, `management.tracing.sampling.probability=1.0`, `spring.kafka.template.observation-enabled=true` et `spring.kafka.listener.observation-enabled=true`, dépend de `spring-boot-starter-opentelemetry` et de `datasource-micrometer-spring-boot` (2.3.0), et n'utilise jamais `management.otlp.tracing.export.enabled`
**And** `! grep -rnE 'Observation\.createNotStarted|Observation\.start|@Observed|@NewSpan|spanBuilder\(|nextSpan\(' services/*/src/main` sort en code 0 (aucun span créé à la main)
**And** chaque ligne de journal porte `traceId` et `spanId`, et `/actuator/health` est le seul point d'actuator exposé

**Given** `coeur-bancaire`
**When** il reçoit `POST /api/coeur-bancaire/temoin`
**Then** il écrit le témoin dans sa base et, dans la même transaction, une ligne `outbox` ; après validation, la ligne est publiée par `KafkaTemplate` dans le même fil sur `coeur-bancaire.temoin-ecrit.v1` (clé `temoinId`), puis marquée publiée ; un relais planifié republie chaque seconde les lignes restées non publiées, dans l'ordre de leur identifiant
**And** le message est l'enveloppe `idEvenement` (UUID), `type`, `version`, `horodatage` (ISO 8601 UTC), `producteur`, `donnees` (`temoinId`), sérialisée par `StringSerializer`, sans en-tête `__TypeId__`
**And** le topic est déclaré par un bean `NewTopic` dans `infrastructure` de `coeur-bancaire` (3 partitions, réplication 1)

**Given** une écriture du témoin dont la transaction est annulée (test d'intégration)
**When** je lis la table `outbox` de `coeur-bancaire`
**Then** aucune ligne n'a été écrite pour ce témoin

**Given** le dépôt
**When** je lance `test -f contrats/evenements/coeur-bancaire.temoin-ecrit.v1.schema.json`
**Then** le code de sortie est 0 ; le schéma porte un exemple, et un test `*ContratEvenement*Test` côté producteur (`coeur-bancaire`, champs exacts) et côté consommateur (`notifications`, désérialise l'exemple) s'exécutent dans `mvn verify`

**Given** une violation introduite volontairement : le champ `temoinId` renommé dans l'événement produit par `coeur-bancaire`
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify`
**Then** le code de sortie est non nul (échec du test de contrat producteur)

**Given** `notifications`
**When** il consomme `coeur-bancaire.temoin-ecrit.v1` (écouteur unitaire, jamais batch, `group.id` = `notifications`)
**Then** il écrit le témoin dans sa base et `idEvenement` dans `evenements_traites`, dans la même transaction

**Given** un test Testcontainers (`testcontainers-kafka`, `testcontainers-postgresql` 2.0.5) qui publie deux fois le même message brut (chaîne JSON de l'enveloppe, sans en-tête de type)
**When** je lance `mvn -B -q -f services/notifications/pom.xml verify`
**Then** le code de sortie est 0 et le test constate un seul effet en base de `notifications`

**Given** le dépôt
**When** je lis `frontend/vite.config.ts`, `frontend/Dockerfile` et `nginx.conf`
**Then** le proxy de développement et le conteneur `web` renvoient `/api/{identifiant}/**` vers le service (chemin transmis tel quel), sauf `/api/audit/**`, et tout autre chemin vers `index.html` ; ils écrasent `X-Forwarded-For` par `$remote_addr`, jamais `$proxy_add_x_forwarded_for`

**Given** la pile démarrée (`docker compose -f compose.yaml up -d --build --wait`, après `make secrets`)
**When** je lance `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/tableau-de-bord`
**Then** la réponse est `200`
**And** `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/audit/` renvoie un code différent de 2xx

**Given** le dépôt
**When** je lis `scripts/gates/trace-smoke.sh` (créé par cette story, seule story autorisée à le créer)
**Then** il est exécutable ; il démarre la pile si elle ne tourne pas, envoie `POST /api/coeur-bancaire/temoin` par le conteneur `web` (port 8080), puis interroge Jaeger (port 16686) ; il écrit des lignes `OK  …` / `KO  …` lisibles et échoue si la trace du témoin ne contient pas, sous le même identifiant de trace, deux services distincts, au moins un span JDBC, un span d'émission Kafka et un span de consommation Kafka

**Given** la story terminée
**When** je lance `scripts/gates/trace-smoke.sh`
**Then** le code de sortie est 0

**Given** une violation introduite volontairement : `spring.kafka.listener.observation-enabled=false` dans la configuration de `notifications` (pile reconstruite)
**When** je lance `scripts/gates/trace-smoke.sh`
**Then** le code de sortie est non nul et la sortie nomme le span de consommation Kafka manquant

**Given** une violation introduite volontairement : le service `notifications` arrêté (`docker compose -f compose.yaml stop notifications`)
**When** je lance `scripts/gates/trace-smoke.sh`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`
**Then** le code de sortie est 0 (seuils de couverture de la story 1.3 compris)

**Given** la story terminée
**When** je lance `scripts/gates/check-gates-exist.sh`
**Then** la sortie contient `OK  compose : collecteur OpenTelemetry`, `OK  compose : Jaeger`, `OK  compose : service coeur-bancaire`, `OK  compose : service notifications` et `OK  scripts/gates/trace-smoke.sh exécutable`

**Given** la story terminée
**When** je lance `scripts/gates/golden-master.sh`
**Then** le code de sortie est 0
**And** `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` ne liste que `scripts/gates/trace-smoke.sh`

### Story 1.5: Mesurer la mutation du domaine et de l'application à la demande

En tant que responsable de la qualité,
je veux PIT dans un profil Maven `mutation` et StrykerJS en mode incremental, lancés par `scripts/gates/mutation.sh`,
afin de détecter des tests qui couvrent le code sans rien vérifier, sans alourdir le build par défaut.

**Exigences :** NFR-QG-05, NFR-QG-06, NFR-QG-00, AR-13
**Scénarios golden master :** aucun
**Prérequis :** stories 1.1 à 1.4 terminées

**Acceptance Criteria:**

**Given** le dépôt
**When** je lis le POM parent
**Then** `pitest-maven` (1.30.0) et `pitest-junit5-plugin` (1.2.3) ne figurent que dans le profil `mutation`, avec `targetClasses` `fr.nextbanking.*.domain.*` et `fr.nextbanking.*.application.*`, `<mutationThreshold>85</mutationThreshold>`, `historyInputFile` et `historyOutputFile` à `${user.home}/.cache/next-banking-webapp/pit/${project.artifactId}.history`
**And** aucune exclusion de mutation n'est déclarée

**Given** le dépôt
**When** je lis `frontend/stryker.config.json`
**Then** il porte `testRunner` `vitest`, `mutate` sur `src/domain` et `src/application` (fichiers de test exclus), `thresholds` `break` 85, `high` 90, `low` 85, et `incrementalFile` `reports/stryker-incremental.json`
**And** `@stryker-mutator/core` et `@stryker-mutator/vitest-runner` (10.0.0) figurent dans `frontend/package.json`

**Given** le dépôt
**When** je lance `scripts/gates/mutation.sh`
**Then** le code de sortie est 0
**And** `test -f ~/.cache/next-banking-webapp/pit/coeur-bancaire.history` et `test -f ~/.cache/next-banking-webapp/pit/notifications.history` sortent en code 0, et `frontend/reports/stryker-incremental.json` existe (Stryker est lancé depuis `frontend/`) et n'est pas versionné (`git check-ignore frontend/reports/stryker-incremental.json` en code 0)

**Given** une violation introduite volontairement : les assertions retirées d'un test de `domain` de `coeur-bancaire` (test conservé, sans assertion)
**When** je lance `scripts/gates/mutation.sh`
**Then** le code de sortie est non nul (score de mutation sous 85)

**Given** une violation introduite volontairement : les assertions retirées d'un test de `frontend/src/domain`
**When** je lance `cd frontend && npx stryker run --incremental`
**Then** le code de sortie est non nul (score sous le seuil `break` 85)

**Given** le dépôt
**When** je lance `mvn -B -q verify -DfailIfNoTests=true` (profil par défaut)
**Then** PIT ne s'exécute pas

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/check-gates-exist.sh`
**Then** la sortie contient, pour chaque service, `OK  <service> : profil mutation avec PIT`, `OK  <service> : seuil de mutation 85`, `OK  <service> : historique PIT configuré`, aucune ligne `PIT actif hors profil mutation`, ainsi que `OK  front : StrykerJS installé` et `OK  front : Stryker break 85`

**Given** la story terminée
**When** je lance `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** les deux sortent en code 0
**And** `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est vide

### Story 1.6: Bloquer les dépendances vulnérables

En tant que responsable de la qualité,
je veux OWASP Dependency-Check dans un profil Maven `security`, lancé avec Trivy par `scripts/gates/cve.sh`, avec des fichiers de suppression vides,
afin qu'aucune vulnérabilité HIGH ou CRITICAL corrigeable n'entre dans le code, et que l'epic 1 se termine avec tous les contrôles présents.

**Exigences :** NFR-QG-07, NFR-QG-08, NFR-QG-00, NFR-QG-10, AR-1, AR-14, AR-15, AR-16, AR-94
**Scénarios golden master :** aucun
**Prérequis :** stories 1.1 à 1.5 terminées. À la charge de l'humain, avant la story : clé d'API NVD dans `~/.m2/settings.xml` (serveur `nvd`, étape 5 du pilote) ; `trivy` 0.75.0 (jamais 0.69.4 à 0.69.6 ; 0.74.0 constaté sur le poste le 2026-10-05) et `jq` installés ; bases à jour avant le run (étapes 9 et 10 du pilote) : `trivy fs --download-db-only .` et `mvn -Psecurity org.owasp:dependency-check-maven:update-only -Ddc.autoUpdate=true`.

**Acceptance Criteria:**

**Given** le dépôt
**When** je lis le POM parent
**Then** `dependency-check-maven` (13.0.0) ne figure que dans le profil `security`, avec `<failBuildOnCVSS>7</failBuildOnCVSS>`, `nvdApiServerId` `nvd`, OSS Index désactivé (`<ossIndexAnalyzerEnabled>false`), Node Audit désactivé, `autoUpdate` résolu à `false` (surchargeable par `-Ddc.autoUpdate=true` pour la seule mise à jour de l'humain), et `config/dependency-check-suppressions.xml` comme fichier de suppressions
**And** aucun POM ne contient `<nvdApiKey>`

**Given** le dépôt
**When** je lis les fichiers de suppression
**Then** `.trivyignore` est vide (`test ! -s .trivyignore` en code 0) et `config/dependency-check-suppressions.xml` ne contient aucun élément `<suppress>` (`! grep -q '<suppress>' config/dependency-check-suppressions.xml` en code 0) ; ni l'un ni l'autre n'est modifié par la story
**And** `scripts/gates/cve.sh` n'est pas modifié

**Given** les bases Trivy et Dependency-Check à jour (prérequis)
**When** je lance `scripts/gates/cve.sh`
**Then** le code de sortie est 0
**And** `reports/cve-all.json` et `reports/cve-unfixed.tsv` sont produits, non versionnés

**Given** une violation introduite volontairement : la dépendance `commons-collections:commons-collections:3.2.1` (vulnérabilité CRITICAL corrigée en 3.2.2) ajoutée au POM de `coeur-bancaire`
**When** je lance `scripts/gates/cve.sh`
**Then** le code de sortie est non nul

**Given** une violation introduite volontairement : `lodash@4.17.20` (vulnérabilité HIGH corrigée en 4.17.21) ajouté à `frontend/package.json` et `frontend/package-lock.json`
**When** je lance `scripts/gates/cve.sh`
**Then** le code de sortie est non nul

**Given** une violation introduite volontairement : `<nvdApiKey>` écrit dans le profil `security`
**When** je lance `scripts/gates/check-gates-exist.sh`
**Then** la sortie contient `KO  coeur-bancaire : clé NVD écrite dans le POM` et le code de sortie est non nul

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/trace-smoke.sh`, `scripts/gates/mutation.sh` et `scripts/gates/golden-master.sh`
**Then** chacun sort en code 0

**Given** l'epic 1 terminé
**When** je lance `scripts/gates/check-gates-exist.sh`
**Then** le code de sortie est 0 et aucune ligne ne commence par `KO`, dont `OK  <service> : profil security avec Dependency-Check`, `OK  <service> : failBuildOnCVSS 7`, `OK  <service> : base Dependency-Check figée pendant les scans`, `OK  <service> : OSS Index désactivé` pour chaque service, `OK  config/dependency-check-suppressions.xml présent`, `OK  .trivyignore présent`, `OK  scripts/gates/cve.sh exécutable`

**Given** l'epic 1 terminé
**When** je lance `sha256sum scripts/gates/*.sh frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml replay-mapping.json`
**Then** le code de sortie est 0 : tous les fichiers que l'humain fige dans `.bmad-loop/gates.sha256` (étape 10 du pilote, hors story) existent
**And** `git diff --name-only "$(git merge-base origin/main HEAD)"...HEAD -- scripts/gates .bmad-loop/policy.toml .bmad-loop/gates.sha256 .claude/settings.json .claude/hooks docs/quality-gates.md frontend/.dependency-cruiser.cjs .trivyignore config/dependency-check-suppressions.xml e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` est vide pour cette story

## Epic 2: Se connecter et accéder à son espace selon son rôle

À la fin de l'epic, un client, un conseiller ou un administrateur se connecte sur `/login` avec son identifiant et son mot de passe (haché), arrive sur l'écran d'accueil de son rôle, navigue dans un gabarit commun (thème issu de DESIGN.md, lien d'évitement, liens filtrés par rôle) et se déconnecte. Les accès sans jeton ou avec un rôle insuffisant sont refusés par les services eux-mêmes et redirigés par le front. Chaque connexion, échec et déconnexion est tracé dans le service `audit` avec l'adresse IP d'origine. L'epic pose aussi les socles transverses réutilisés par tous les epics suivants : JWT, format d'erreur `{"message"}`, adresse IP, audit par outbox, thème `npm run tokens`, CSP et contrôle axe du conteneur web, `make secrets`, publication de `identite-clients.client-modifie.v1` et script `scripts/golden-master/verifier-scenario.sh`.

**Contrôle de fin d'epic (à la charge de l'humain) :** lire, dans `reports/golden-master/textes-non-trouves.md`, les sections des scénarios de l'epic (SC-001-04, SC-001-05) ; lancer `scripts/golden-master/verifier-scenario.sh` pour chacun (SC-001-04, SC-001-05). Les scénarios HTTP de l'epic (SC-001-01, SC-001-02, SC-001-03) sont couverts par des tests d'intégration et des commandes `curl` dans les stories 2.1, 2.3 et 2.6.

### Story 2.1: Se connecter par l'API du service identité et clients

En tant qu'utilisateur de Demo Bank (client, conseiller ou administrateur),
je veux obtenir un jeton d'accès en présentant mon identifiant et mon mot de passe,
afin d'ouvrir une session authentifiée sans que mon mot de passe soit jamais stocké en clair.

**Exigences :** FR-001-03, FR-001-04, FR-001-06, FR-001-07, FR-001-08, FR-001-09, FR-030-01, FR-030-08, NFR-SEC-01, NFR-SEC-02, NFR-SEC-04, NFR-SEC-06, NFR-PAR-02, NFR-PAR-04, AR-17, AR-18, AR-26, AR-33, AR-34, AR-36, AR-37, AR-46, AR-47, AR-51, AR-53, AR-55, AR-56, AR-57, AR-69, AR-85, AR-96
**Scénarios golden master :** SC-001-03
**Prérequis :** epic 1 terminé (empreinte `.bmad-loop/gates.sha256` reportée après la démonstration, décision du 2026-10-05 ; AR-94) ; A-001-1 (compte désactivé refusé avec le message générique et audit LOGIN_FAILED) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; A-030-1 (écrans publics exemptés sur leur chemin exact seulement) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; RP-05 (verrouillage du compte après échecs, politique de mot de passe) — tranché le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** un dépôt sans `.env` ni `.secrets/`
**When** je lance `make secrets`
**Then** `.env` (mots de passe aléatoires des bases) et la paire RSA du jeton dans `.secrets/` existent
**And** `git ls-files .env .secrets` n'affiche rien, `.env.example` est versionné sans aucune valeur, et `trivy fs --scanners secret --exit-code 1 .` sort en code 0

**Given** le service `identite-clients` lancé sans la variable d'environnement de la clé privée du jeton
**When** son contexte Spring démarre
**Then** le démarrage échoue (test de configuration qui constate l'échec), et aucune clé par défaut n'existe dans le code ni dans un fichier versionné

**Given** `services/identite-clients/reference-data/reference-data.sql`, généré par le script de hachage depuis `RETRO_ADMIN_PASSWORD`, `RETRO_CONSEILLER_PASSWORD` et `RETRO_CLIENT_PASSWORD`
**When** je le charge dans une base de test (Testcontainers)
**Then** la table `CUSTOMERS` contient les 7 utilisateurs du legacy avec leurs identifiants (dont `admin`, `sophie`, `user1` « Jean Dupont »), chaque mot de passe stocké commence par `{bcrypt}` et diffère du mot de passe de démonstration
**And** `grep -F "$RETRO_CLIENT_PASSWORD" services/identite-clients/reference-data/reference-data.sql` ne trouve rien (FR-001-08, BUG-002)

**Given** l'utilisateur `user1` actif
**When** `POST /api/identite-clients/connexion` avec `{"identifiant":"user1","motDePasse":<bon mot de passe>}`
**Then** la réponse est 200 avec un JWT signé RSA dont les revendications sont `sub` (identifiant du client), `roles` = `["CLIENT"]`, `jti`, `iat` et `exp` = `iat` + 1 800 s ; `iat` et `exp` sont calculés avec la `java.time.Clock` système de `adapter.out.jeton` (FR-001-04)

**Given** `user1` actif
**When** il se connecte deux fois de suite
**Then** les deux jetons ont des `jti` différents
**And** ni `GET /login`, ni une connexion refusée, ni aucune autre réponse publique ne contient d'en-tête `Set-Cookie` ou de jeton (FR-001-09, FR-030-08, BUG-003, BUG-092)

**Given** l'entrée exacte de SC-001-03 (`{"identifiant":"retro-compte-inexistant","motDePasse":"retro-mauvais-mot-de-passe"}`), puis `user1` avec un mauvais mot de passe
**When** chacune est envoyée à `POST /api/identite-clients/connexion`
**Then** les deux réponses sont identiques : statut 401 et corps `{"message":"Identifiants incorrects"}` (FR-001-06, SC-001-03, test d'intégration du service)

**Given** un utilisateur dont la colonne `ACTIVE` vaut `false` en base, avec ses identifiants corrects
**When** il appelle `POST /api/identite-clients/connexion`
**Then** la réponse est 401 `{"message":"Identifiants incorrects"}`, sans jeton
**And** après remise de `ACTIVE` à `true` en base, la même requête renvoie 200 et un jeton (FR-001-07, BUG-001 ; la réactivation par l'administrateur est prouvée en story 2.6)

**Given** une connexion avec `identifiant` vide et `motDePasse` rempli, puis `identifiant` rempli et `motDePasse` vide
**When** elles sont envoyées à l'API
**Then** les réponses sont 400 `{"message":"Identifiant requis"}` puis 400 `{"message":"Mot de passe requis"}` (contrôle serveur de FR-001-03)

**Given** un jeton valide de `user1`
**When** `GET /api/identite-clients/moi`
**Then** la réponse est 200 avec l'identifiant, le nom complet « Jean Dupont » et les rôles
**And** sans jeton, la même requête renvoie 401 au format `{"message": …}` (textes décidés, `textes-nouveaux.md`, lot « Stories » : 401 « Authentification requise », 403 « Accès refusé », 404 « Ressource introuvable », erreur technique « Une erreur technique est survenue » ; aucun texte du legacy n'existe pour ces cas génériques de l'API)

**Given** la liste fermée des chemins publics (AR-36)
**When** j'appelle sans jeton `POST /api/identite-clients/connexion/x` et `GET /api/identite-clients/connexion`
**Then** les deux réponses sont 401 : seul `POST /api/identite-clients/connexion` est public, comparé sur le chemin exact (FR-030-01)

**Given** un corps JSON invalide envoyé à `POST /api/identite-clients/connexion`
**When** le service répond
**Then** le statut est 400 et le corps ne contient que le champ `message`, sans pile d'appels, nom de classe Java ni code interne (NFR-SEC-04)

**Given** une connexion réussie puis une connexion refusée
**When** je lis la sortie console du service capturée par le test
**Then** aucune ligne ne contient le mot de passe saisi ni le jeton émis, et chaque ligne porte `traceId` et `spanId` (AR-51)

**Given** `compose.yaml` complété par `identite-clients` (port 8081) et `postgres-identite-clients` (volume `pgdata-identite-clients`)
**When** je lance `docker compose -f compose.yaml config --services` et `docker compose -f compose.yaml config --format json | jq '[.services[] | select(.healthcheck == null)] | length'`
**Then** les deux services sont listés et le second résultat vaut `0`
**And** application démarrée, `curl -s http://localhost:8080/api/identite-clients/connexion -H 'Content-Type: application/json' -d '{"identifiant":"x","motDePasse":"y"}'` renvoie 401 et `{"message":"Identifiants incorrects"}`

**Given** le service `identite-clients`
**When** je lance `find services/identite-clients/src/test -name '*SecuriteIT.java'`
**Then** une classe est trouvée ; elle prouve qu'une requête sans jeton sur un point protégé reçoit 401 (les cas 403 sont ajoutés en story 2.6)

**Given** la story terminée
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-001-03,FR-001-04,FR-001-06,FR-001-07,FR-001-08,FR-001-09,FR-030-01,FR-030-08'` puis `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='BUG-001,BUG-002,BUG-003,BUG-092'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 2.2: Tracer les connexions dans le journal d'audit avec l'adresse IP d'origine

En tant qu'administrateur de Demo Bank,
je veux que chaque connexion réussie et chaque tentative échouée soient enregistrées avec l'adresse IP réelle du client, dans un journal que personne ne peut consulter ni modifier par l'application,
afin de disposer d'une trace fiable des accès, conservée même si l'opération échoue.

**Exigences :** FR-001-12, FR-001-13, FR-026-01, FR-026-04, FR-026-05, FR-026-06, NFR-OBS-01, NFR-QG-09, NFR-PAR-02, AR-17, AR-23, AR-26, AR-29, AR-31, AR-37, AR-38, AR-39, AR-40, AR-41, AR-48, AR-53, AR-54, AR-91
**Scénarios golden master :** aucun
**Prérequis :** story 2.1 ; A-026-1 (noms et détails des codes d'audit) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** `user1` qui se connecte avec succès depuis l'adresse `203.0.113.7`
**When** l'événement publié sur `identite-clients.audit.v1` est consommé par le service `audit`
**Then** `AUDIT_LOGS` contient une ligne `ACTION` = `LOGIN`, `DETAILS` = « Connexion réussie », `USERNAME` = identifiant du client, `IPADDRESS` = `203.0.113.7`, `ACTION_DATE` = horodatage de l'enveloppe (port Horloge du producteur), jamais l'heure de réception (FR-001-12, FR-026-01)

**Given** une tentative avec l'identifiant saisi `retro-compte-inexistant`, puis une tentative d'un compte désactivé avec ses bons identifiants
**When** les entrées sont consommées
**Then** chacune donne une ligne `LOGIN_FAILED`, « Tentative échouée », `USERNAME` = identifiant saisi, avec l'adresse IP (FR-001-12, A-001-1)

**Given** le cas d'usage de connexion exécuté avec un double du port `Transaction` qui annule l'opération après l'écriture d'audit
**When** le test lit `outbox_audit`
**Then** l'entrée d'audit est présente : elle a été écrite par `Transaction.executerIndependamment(...)` (FR-026-04)

**Given** une requête dont l'adresse distante appartient à `nb.reseau.mandataires-de-confiance` (`172.30.0.0/24` [ASSUMPTION]) et qui porte `X-Forwarded-For: 203.0.113.7, 10.0.0.1`
**When** l'adresse IP est résolue dans `adapter.in.http`
**Then** l'adresse retenue est `203.0.113.7`
**And** la même requête venant d'une adresse distante hors de la plage retient l'adresse distante, et un `X-Forwarded-For` vide ou absent retient l'adresse distante (FR-001-13, FR-026-05, BUG-083, test d'intégration)

**Given** la configuration du conteneur web et du proxy Vite
**When** je lance `grep -n 'proxy_set_header X-Forwarded-For \$remote_addr' frontend/nginx.conf` et `! grep -rn 'proxy_add_x_forwarded_for' frontend/nginx.conf frontend/vite.config.ts`
**Then** les deux commandes sortent en code 0 ; le proxy Vite écrase aussi `X-Forwarded-For` avec l'adresse du client (AR-48)

**Given** l'application démarrée par `compose.yaml`
**When** je lance `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/audit/`
**Then** le code affiché n'est pas 2xx
**And** `! grep -rn '@RestController\|@Controller' services/audit/src/main` sort en code 0 : aucun écran ni point d'API ne lit le journal (FR-026-06)

**Given** le service `audit` (module `services/audit/`, base `postgres-audit`, `group.id` `audit`, migration Flyway de `AUDIT_LOGS` avec `ID`, `ACTION`, `ACTION_DATE`, `DETAILS`, `IPADDRESS`, `USERNAME`)
**When** un message brut de `identite-clients.audit.v1` (chaîne JSON de l'enveloppe, sans en-tête `__TypeId__`) est livré deux fois (Testcontainers Kafka et PostgreSQL)
**Then** `AUDIT_LOGS` ne contient qu'une ligne et `evenements_traites` contient l'`idEvenement` (AR-41)

**Given** un message dont `action` vaut `CODE_INCONNU`
**When** le service `audit` le consomme
**Then** aucune ligne n'est écrite dans `AUDIT_LOGS` et une ligne de journal nomme le code rejeté (AR-23, critère négatif)

**Given** le topic `identite-clients.audit.v1`
**When** je lance les tests de contrat
**Then** `contrats/evenements/identite-clients.audit.v1.schema.json` existe avec un exemple ; le test producteur `*ContratEvenement*Test` vérifie les champs exacts `utilisateur`, `action`, `detail`, `adresseIp` et l'enveloppe (`idEvenement`, `type`, `version`, `horodatage`, `producteur`, `donnees`), clé = identifiant de l'utilisateur ; le test consommateur du service `audit` désérialise l'exemple ; le topic est déclaré par un bean `NewTopic` (3 partitions, réplication 1) dans `infrastructure` d'`identite-clients`

**Given** le service `audit`
**When** je lance `find services/audit/src/test -name '*SecuriteIT.java'`
**Then** une classe est trouvée ; elle prouve qu'une requête sans jeton sur `/api/audit/**` reçoit 401 et qu'aucun point d'API métier ne répond avec un jeton valide

**Given** la story terminée
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-001-12,FR-001-13,FR-026-01,FR-026-04,FR-026-05,BUG-083'` et `mvn -B -q -f services/audit/pom.xml test -Dgroups='FR-026-01,FR-026-06'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 2.3: Se connecter depuis l'écran de connexion au thème Demo Bank

En tant qu'utilisateur de Demo Bank,
je veux un écran de connexion accessible, aux textes du legacy et au thème du design system,
afin de me connecter au clavier ou à la souris et d'arriver directement sur l'écran de mon rôle.

**Exigences :** FR-001-01, FR-001-02, FR-001-03, FR-001-04, FR-001-05, FR-001-06, FR-030-01, FR-030-03, NFR-UI-01, NFR-UI-02, NFR-SEC-04, AR-45, AR-54, AR-59, AR-76, AR-77, AR-78, AR-79, AR-101, AR-107, UX-DR1, UX-DR2, UX-DR3, UX-DR4, UX-DR5, UX-DR6, UX-DR7, UX-DR8, UX-DR9, UX-DR10, UX-DR11, UX-DR12, UX-DR13, UX-DR14, UX-DR15, UX-DR20, UX-DR22, UX-DR24, UX-DR42, UX-DR44, UX-DR45, UX-DR51, UX-DR57, UX-DR69, UX-DR73, UX-DR78, UX-DR81, UX-DR82, UX-DR83, UX-DR84, UX-DR85, UX-DR86, UX-DR88, UX-DR92, UX-DR95, UX-DR100, UX-DR102, UX-DR103, UX-DR106, UX-DR114, UX-DR135, UX-DR148, UX-DR152, UX-DR155
**Scénarios golden master :** SC-001-01
**Prérequis :** story 2.1. AR-107 tranché le 2026-10-05 (décision D11) : **Inter seule**, auto-hébergée ; Graphik n'est pas utilisée (pas de licence d'auto-hébergement). La pile de polices devient Inter, Arial, sans-serif.

**Acceptance Criteria:**

**Given** DESIGN.md
**When** je lance `cd frontend && npm run tokens && git diff --exit-code src/adapters/ui/theme/tokens.css`
**Then** le code de sortie est 0 : `tokens.css` est généré par `npx @google/design.md export --format css-tailwind` puis `frontend/scripts/tokens-root.mjs`, et versionné à jour
**And** `! grep -n '@theme' frontend/src/adapters/ui/theme/tokens.css`, `! grep -rnE 'fonts\.(googleapis|gstatic)\.com' frontend/src frontend/index.html` et `! grep -rnE '^\s*(select|table|input|button|a|th|td)\s*[,{]' frontend/src --include=*.css | grep -v 'src/adapters/ui/'` sortent en code 0 ; aucune dépendance Tailwind dans `frontend/package.json`

**Given** une valeur de couleur écrite en dur (`#ff0000`) introduite dans le CSS d'un écran hors de `tokens.css`
**When** je lance le test Vitest `[UX-DR1]` qui cherche les couleurs hors tokens dans `frontend/src`
**Then** le test échoue en nommant le fichier (critère négatif)

**Given** `/login` ouvert sans jeton
**When** la page s'affiche
**Then** l'onglet s'intitule « BankApp Demo - Connexion », la page affiche « DB », « Demo Bank », « Espace client — Connexion sécurisée », les libellés « Identifiant » et « Mot de passe » reliés par `<label for>`, et le bouton « Me connecter » ; « ⚠️ Système Legacy — JEE 8 / WildFly 14 » n'apparaît pas (FR-001-01, UX-DR114) ; `index.html` porte `lang="fr"`
**And** le formulaire est un vrai `<form>` : `<input type="text" name="username" data-testid="login-identifiant">` est le premier champ texte du DOM, `<input type="password" name="password" data-testid="login-mot-de-passe">` le seul champ mot de passe, `<button type="submit" data-testid="login-me-connecter">Me connecter</button>` (AR-77)

**Given** `/login` ou toute autre page
**When** j'appuie une première fois sur `Tab`
**Then** le premier élément focalisé est le lien « Aller au contenu principal », visible au focus avec l'anneau `focus-ring`, placé avant tout autre contenu, et son activation déplace le focus sur le contenu principal (UX-DR73, UX-DR148)

**Given** le formulaire soumis avec « Identifiant » vide, puis avec « Mot de passe » vide
**When** l'utilisateur appuie sur Entrée
**Then** « Identifiant requis » (puis « Mot de passe requis ») s'affiche sous le champ concerné, relié par `aria-describedby`, le champ porte `aria-invalid="true"`, le focus va au premier champ en erreur et aucun appel à l'API n'est fait (FR-001-03)

**Given** des identifiants refusés par l'API (401)
**When** l'utilisateur soumet le formulaire
**Then** il reste sur `/login` et la zone `login-message-erreur` (`alert-error`, `role="alert"`) affiche « Identifiants incorrects » et « Vérifiez votre login et mot de passe » ; aucun détail technique n'est affiché (FR-001-06)

**Given** une soumission en cours
**When** la réponse de l'API n'est pas encore arrivée
**Then** « Me connecter » est désactivé avec `aria-busy="true"` et un deuxième appui n'envoie pas de seconde requête (UX-DR15, UX-DR100)

**Given** `admin`, `sophie` puis `user1` avec leurs bons mots de passe
**When** chacun soumet le formulaire par Entrée
**Then** le jeton est enregistré dans `localStorage` sous la clé `nb.jeton` (jamais dans `sessionStorage`) et la SPA quitte `/login` pour `/administration`, `/espace-conseiller` puis `/tableau-de-bord` respectivement (FR-001-04, FR-001-05, FR-030-03)

**Given** aucun jeton dans `localStorage`
**When** j'ouvre `/tableau-de-bord`, `/espace-conseiller`, `/administration` ou `/`
**Then** la SPA affiche `/login` (FR-001-02, FR-030-01) ; ces trois routes protégées sont déclarées dans le routeur de `frontend/src` (leur contenu est livré par les stories et epics suivants)

**Given** l'application démarrée par `compose.yaml`
**When** je lance `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/login`
**Then** le code affiché est `200` (SC-001-01)
**And** `curl -sI http://localhost:8080/login` contient un en-tête `Content-Security-Policy` qui n'autorise scripts, styles et polices que depuis l'origine de l'application ; les polices (Inter en repli de Graphik) sont servies par le conteneur web (AR-54, UX-DR11)

**Given** l'outil axe-core, version vérifiée et consignée dans `versions-verifiees.md` avant usage (AR-101)
**When** je lance `cd frontend && npx vitest run -t 'axe'`
**Then** le test d'accessibilité de `/login` ne relève aucune violation et sort en code 0
**And** si un `<label for>` du formulaire est retiré, la même commande sort en code non nul en nommant la règle violée (critère négatif)

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t 'FR-001-01|FR-001-02|FR-001-03|FR-001-04|FR-001-05|FR-001-06|FR-030-01|FR-030-03'`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 2.4: Naviguer dans son espace selon son rôle

En tant qu'utilisateur connecté,
je veux un en-tête commun qui me salue, ne me montre que les liens auxquels mon rôle donne droit et me ramène à mon tableau de bord si je demande un écran réservé,
afin de circuler dans l'application sans jamais voir un écran qui ne m'est pas destiné.

**Exigences :** FR-001-05, FR-001-11, FR-016-01, FR-030-02, FR-030-03, FR-030-04, NFR-UI-01, NFR-UI-02, AR-34, AR-76, AR-78, AR-79, UX-DR14, UX-DR21, UX-DR32, UX-DR33, UX-DR34, UX-DR40, UX-DR70, UX-DR71, UX-DR72, UX-DR74, UX-DR75, UX-DR76, UX-DR79, UX-DR90, UX-DR96, UX-DR97, UX-DR99, UX-DR153, UX-DR156, UX-DR110
**Scénarios golden master :** SC-001-04
**Prérequis :** stories 2.1 et 2.3.

**Acceptance Criteria:**

**Given** le gabarit et la navigation livrés
**When** je relis les écrans de `frontend/src/adapters`
**Then** aucun ne recourt à un motif interdit par UX-DR110 : défilement infini, glisser-déposer, action visible seulement au survol, plus d'une modale ouverte, plus de deux niveaux de navigation, contenu interactif dans un tooltip
**And** la règle est rappelée dans le README du front pour les stories suivantes

**Given** `user1` connecté sur `/tableau-de-bord`
**When** la page s'affiche
**Then** l'en-tête montre à gauche « DB » « Demo Bank », au centre, dans cet ordre et cette orthographe, « Tableau de bord », « Virement », « Virements programmes », « Releve », « Budget », « Prets », « Documents », « Bourse », « Mon Conseiller », « Produits », « Comptes Tiers », « Beneficiaires », et à droite « Bonjour, Jean Dupont » (nom complet lu par `GET /api/identite-clients/moi`) et « Deconnexion » (`data-testid="nav-deconnexion"`, action livrée en story 2.5) (UX-DR70)
**And** chaque lien porte un `data-testid` préfixé `nav-` ; le lien de l'écran courant porte `aria-current="page"` et le soulignement 2px primary ; « Espace Conseiller » et « Administration » sont absents (FR-030-04)

**Given** `sophie` (CONSEILLER) puis `admin` (ADMIN) connectés
**When** l'en-tête s'affiche sur n'importe quel écran authentifié
**Then** `sophie` voit « Espace Conseiller » sans « Administration » et le badge de rôle « Conseiller » ; `admin` voit « Espace Conseiller » et « Administration » et le badge « ADMIN » (`tag`) (FR-030-04, UX-DR70)

**Given** `sophie` sur `/espace-conseiller`, puis `admin` sur `/administration`
**When** la page s'affiche
**Then** le sous-en-tête (fond primary, 48px) affiche « Mon portefeuille » · « Conseiller IA » sur `/espace-conseiller` et « Administration » · « Espace Conseiller » · « Mon tableau de bord » sur `/administration` ; `/tableau-de-bord` n'a pas de sous-en-tête (UX-DR71)

**Given** `user1` (CLIENT) connecté
**When** il ouvre `/administration`, puis `/espace-conseiller`
**Then** la SPA affiche `/tableau-de-bord` dans les deux cas, sans message, et le contenu de la page demandée n'est jamais rendu (FR-001-11, FR-016-01, FR-030-02)

**Given** `sophie` (CONSEILLER) connectée
**When** elle ouvre `/administration`
**Then** la SPA affiche `/tableau-de-bord` sans message (FR-016-01) ; `admin` ouvre `/administration` et `/espace-conseiller` sans redirection

**Given** un jeton valide de chaque rôle
**When** j'ouvre `/`
**Then** la SPA affiche l'écran d'accueil du rôle (`/administration`, `/espace-conseiller`, `/tableau-de-bord`) (FR-030-03, AR-76)

**Given** un utilisateur connecté
**When** il ouvre une route qui n'est pas encore déclarée (par exemple `/virement` avant l'epic 3)
**Then** la SPA redirige vers l'écran d'accueil du rôle si l'utilisateur est connecté, vers `/login` sinon, sans écran ni texte nouveau (décision du 2026-10-05)

**Given** l'en-tête rendu
**When** je cherche un bouton de menu repliable (hamburger)
**Then** il n'en existe aucun ; sous 1 280 px les liens passent sur deux lignes et le header grandit (UX-DR79, UX-DR97) ; la zone de contenu porte `aria-busy="true"` jusqu'à la réponse de `GET /api/identite-clients/moi`, sans texte de chargement (UX-DR99)

**Given** le golden master dérivé
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-001-04`
**Then** le script lit `docs/retro/golden-master/derived/FEAT-001/SC-001-04/input-actions.json`, vérifie que `start_url` (`/tableau-de-bord`) est déclarée dans le routeur de `frontend/src` et que chaque `data-testid` des sélecteurs `[data-testid='…']` est présent dans `frontend/src`, sans aucune liste recopiée
**And** il cherche chaque ligne de `expected-dom.txt` dans `frontend/src`, puis dans `services/*/reference-data/reference-data.sql`, puis dans les valeurs saisies de `input-actions.json`, écrit les lignes trouvées nulle part dans la section « SC-001-04 » de `reports/golden-master/textes-non-trouves.md` (par exemple « MES COMPTES DEMO BANK » tant que l'epic 3 n'est pas livré), et sort en code 0 : les textes ne font jamais échouer le script

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-001-04`
**Then** le code de sortie est 0

**Given** `data-testid="nav-deconnexion"` retiré de `frontend/src`
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-001-05`
**Then** le code de sortie est non nul et la sortie nomme `SC-001-05` et `nav-deconnexion` (critère négatif du script)
**And** `scripts/golden-master/verifier-scenario.sh SC-003-01` (route `/virement` non encore déclarée) sort en code non nul en nommant `SC-003-01` et `/virement` ; un identifiant de scénario inconnu ou un scénario HTTP (sans `input-actions.json`, ex. `SC-001-03`) sort en code non nul en nommant le scénario

**Given** le script exécuté sur SC-001-04 et SC-001-05
**When** je lance `git status --porcelain docs/retro/golden-master/derived e2e/golden-master replay-mapping.json`
**Then** rien n'est affiché : le script ne modifie ni le golden master dérivé, ni les tests générés, ni le mapping ; il vit dans `scripts/golden-master/`, hors de `scripts/gates/`

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t 'FR-001-05|FR-001-11|FR-016-01|FR-030-02|FR-030-03|FR-030-04'`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 2.5: Se déconnecter et retrouver la connexion quand la session expire

En tant qu'utilisateur connecté,
je veux me déconnecter d'un clic et être renvoyé vers la connexion quand ma session a expiré,
afin que personne ne puisse continuer ma session sur le poste après mon départ.

**Exigences :** FR-001-10, FR-001-12, FR-026-01, FR-026-05, FR-030-05, NFR-SEC-02, NFR-SEC-07, AR-34, AR-35, AR-45, AR-106, UX-DR34, UX-DR76
**Scénarios golden master :** SC-001-05
**Prérequis :** stories 2.2 et 2.4. AR-106 tranché le 2026-10-05 (décision D10) : mécanisme de la spine confirmé — jeton de 30 minutes, renouvelé par `POST /api/identite-clients/renouvellement` sur activité quand il reste moins de 10 minutes ; sans activité, le jeton expire et le front affiche `/login` (comme le legacy : 30 minutes d'inactivité).

**Acceptance Criteria:**

**Given** `user1` connecté sur `/tableau-de-bord`
**When** il clique sur « Deconnexion » (`nav-deconnexion`)
**Then** `nb.jeton` est supprimé de `localStorage`, `POST /api/identite-clients/deconnexion` est appelé avec le jeton, et `/login` s'affiche avec « Identifiant », « Mot de passe » et « Espace client — Connexion sécurisée » (FR-001-10)
**And** une ouverture de `/tableau-de-bord` après la déconnexion affiche `/login`

**Given** une déconnexion depuis l'adresse `203.0.113.7`
**When** l'entrée est consommée par le service `audit`
**Then** `AUDIT_LOGS` contient `LOGOUT`, « Déconnexion », l'identifiant du client et `IPADDRESS` = `203.0.113.7` (FR-001-12, FR-026-01, FR-026-05)

**Given** un jeton présenté à `POST /api/identite-clients/deconnexion`
**When** la déconnexion est traitée
**Then** aucune liste de révocation n'est écrite et le même jeton reste accepté par `GET /api/identite-clients/moi` jusqu'à son `exp`, 30 minutes au plus après `iat` (décision Q1 (a), point 11 de golden-master-architecture.md, AR-35, risque R-5) ; le test le documente explicitement

**Given** un jeton dont il reste moins de 10 minutes et une action de l'utilisateur
**When** le front détecte l'activité
**Then** il appelle `POST /api/identite-clients/renouvellement` et remplace `nb.jeton` par un jeton neuf (nouveau `jti`, `exp` = nouvel `iat` + 1 800 s) ; ce calcul de temps vit dans `src/adapters`, seule exception de AR-45 (test Vitest à horloge simulée)

**Given** un utilisateur dont la colonne `ACTIVE` vaut `false`
**When** il demande `POST /api/identite-clients/renouvellement` avec son jeton encore valide
**Then** la réponse est 401 et aucun jeton n'est émis (critère négatif, AR-35)

**Given** un jeton expiré dans `localStorage`
**When** l'utilisateur soumet une action et que le service répond 401
**Then** le front supprime `nb.jeton` et affiche `/login`, sans message (FR-030-05, NFR-SEC-07) ; tout 401 de n'importe quel service suit la même règle (AR-34)

**Given** un jeton expiré, ou signé par une autre clé, présenté à `GET /api/identite-clients/moi`
**When** le service le vérifie
**Then** la réponse est 401

**Given** le profil Spring `rejeu`
**When** un jeton est émis
**Then** `exp` = `iat` + 3 600 s (test de configuration) ; hors profil `rejeu`, `exp` = `iat` + 1 800 s

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-001-05`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t 'FR-001-10|FR-030-05'` et `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-001-10,FR-001-12,FR-026-01,FR-026-05,FR-030-05'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 2.6: Faire appliquer les rôles par les services eux-mêmes

En tant qu'administrateur de Demo Bank,
je veux que chaque service refuse lui-même un appel sans jeton ou d'un rôle insuffisant, et pouvoir désactiver puis réactiver un client par l'API,
afin qu'aucune fonction réservée ne dépende du seul contrôle du front.

**Exigences :** FR-030-07, FR-001-07, FR-016-01, FR-030-01, FR-016-11 (API de désactivation et d'activation seulement ; écran « Gestion des clients » en epic 9), FR-016-06 et FR-026-02 (codes ADMIN_DEACTIVATE et ADMIN_ACTIVATE seulement), FR-026-04, FR-026-05, NFR-SEC-03, NFR-PAR-02, AR-36, AR-37, AR-40, AR-46
**Scénarios golden master :** SC-001-02
**Prérequis :** stories 2.1 et 2.2.

**Acceptance Criteria:**

**Given** le service `coeur-bancaire` équipé de la vérification du jeton (`spring-boot-starter-security-oauth2-resource-server`, clé publique par configuration)
**When** un test d'intégration appelle `GET /api/coeur-bancaire/tableau-de-bord` sans jeton
**Then** la réponse est 401, sans redirection (SC-001-02)
**And** `POST /api/coeur-bancaire/temoin` reste public et `scripts/gates/trace-smoke.sh` reste en code 0

**Given** l'application démarrée par `compose.yaml`
**When** je lance `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/coeur-bancaire/tableau-de-bord`
**Then** le code affiché est `401`

**Given** le cas d'usage de désactivation d'un client, appelé directement dans `application` (sans filtre HTTP) avec un acteur CLIENT puis CONSEILLER
**When** il s'exécute sur `user1`
**Then** il est refusé et la colonne `ACTIVE` de `user1` est inchangée
**And** le même appel avec un acteur ADMIN réussit (FR-030-07, BUG-093)

**Given** les points d'API de désactivation et d'activation d'un client, sous `/api/identite-clients/` (chemins fixés par la story)
**When** ils sont appelés avec un jeton CLIENT ou CONSEILLER
**Then** la réponse est 403 au format `{"message"}` et rien n'est modifié ; avec un jeton ADMIN la désactivation rend le client inactif et l'activation le rend actif ; un client inexistant donne 404 (texte du `message` : voir l'arbitrage de la story 2.1)

**Given** `user1` désactivé par `admin` via l'API
**When** il se connecte avec ses bons identifiants
**Then** la réponse est 401 `{"message":"Identifiants incorrects"}` ; après réactivation par `admin` via l'API, la connexion réussit (FR-001-07, BUG-001)

**Given** une désactivation puis une activation de `user1` (identifiant `{customerId}`) par `admin` depuis `203.0.113.7`
**When** les entrées sont consommées par le service `audit`
**Then** `AUDIT_LOGS` contient `ADMIN_DEACTIVATE` « Désactivation client id={customerId} » puis `ADMIN_ACTIVATE` « Activation client id={customerId} », avec l'identifiant de l'administrateur et `IPADDRESS` = `203.0.113.7` (FR-026-02, FR-026-05)
**And** si la désactivation est annulée après l'écriture d'audit, l'entrée est présente dans `outbox_audit` (FR-016-06, FR-026-04)

**Given** les services `identite-clients` et `coeur-bancaire`
**When** je lance `for s in services/identite-clients/ services/coeur-bancaire/ services/audit/; do find "$s/src/test" -name '*SecuriteIT.java' | grep -q . || echo "KO $s"; done`
**Then** aucun `KO` n'est affiché ; la classe d'`identite-clients` prouve en plus « jeton d'un autre rôle → 403 » sur la désactivation

**Given** la story terminée
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-030-07,FR-001-07,FR-016-01,FR-016-11,FR-026-02,FR-026-04,FR-026-05,BUG-093,BUG-001'` et `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-030-01'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 2.7: Publier l'état des clients pour les autres services

En tant qu'équipe des services cœur bancaire, crédits et documents,
je veux recevoir l'état complet d'un client à chaque changement fait par le service identité et clients,
afin de tenir une projection locale des clients (appartenance, portefeuille du conseiller) sans jamais lire la base d'un autre service.

**Exigences :** FR-016-11 (événement de changement d'état), FR-030-02 (projection du portefeuille, préparée), AR-27, AR-28, AR-29, AR-31, AR-32, AR-38, AR-39, AR-97
**Scénarios golden master :** aucun
**Prérequis :** story 2.6.

**Acceptance Criteria:**

**Given** le topic `identite-clients.client-modifie.v1`
**When** je lis `contrats/evenements/identite-clients.client-modifie.v1.schema.json`
**Then** il existe avec un exemple et décrit l'enveloppe (`idEvenement`, `type`, `version`, `horodatage`, `producteur`, `donnees`) et les champs `clientId`, `login`, `nomComplet`, `role`, `actif`, `conseillerId`, `dateCreation`, `supprime`, `versionAgregat`
**And** le topic est déclaré par un bean `NewTopic` dans `infrastructure` d'`identite-clients` (3 partitions, réplication 1), seul producteur

**Given** la désactivation de `user1` par `admin` (story 2.6)
**When** la transaction est validée
**Then** une ligne `outbox` écrite dans la même transaction est publiée par `KafkaTemplate` dans le même fil, sur la clé `clientId`, sans en-tête `__TypeId__`, avec l'état complet du client (`actif` = `false`) et un `versionAgregat` strictement supérieur au précédent ; l'activation qui suit publie `actif` = `true` et un `versionAgregat` encore supérieur

**Given** une désactivation dont la transaction est annulée
**When** le test lit la table `outbox` et le topic (Testcontainers)
**Then** aucune ligne ni aucun événement n'existe (critère négatif)

**Given** une ligne `outbox` restée non publiée (Kafka indisponible au moment de la validation)
**When** le relais planifié s'exécute (toutes les secondes)
**Then** la ligne est publiée puis marquée publiée, dans l'ordre des identifiants

**Given** le test producteur `*ContratEvenement*Test`
**When** un champ attendu (par exemple `conseillerId`) est retiré de l'événement produit
**Then** le test échoue en nommant le champ (critère négatif) ; avec l'événement complet il passe

**Given** la story terminée
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml test -Dtest='*ContratEvenement*Test'` et `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-11'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

## Epic 3: Consulter ses comptes et virer de l'argent en sécurité

À la fin de l'epic, un client vire de l'argent entre ses comptes. Chaque virement passe par un contrôle de fraude synchrone avant tout débit : il est exécuté, suspendu en attente de la revue de l'administrateur (24 h au plus, puis rejet automatique sans débit), ou refusé sans débit si le contrôle tombe en erreur. Le client consulte ses comptes, ses dernières opérations, son relevé, ses notifications et l'état de ses virements passés en revue. L'administrateur libère ou rejette un virement suspendu, avec un motif obligatoire et audité.

**Contrôle de fin d'epic (à la charge de l'humain) :** lire, dans `reports/golden-master/textes-non-trouves.md`, les sections des scénarios de l'epic (SC-002-01, SC-002-02, SC-003-01, SC-003-02, SC-003-04, SC-005-01) ; lancer `scripts/golden-master/verifier-scenario.sh` pour chacun.

### Story 3.1: Exécuter un virement entre ses comptes

En tant que client,
je veux virer un montant d'un de mes comptes vers un autre de mes comptes,
afin de déplacer mon argent sans passer par mon agence.

**Exigences :** FR-003-01, FR-003-02, FR-003-03, FR-003-04, FR-003-05, FR-003-06, FR-003-07, FR-003-08, FR-003-09, FR-003-10, FR-003-11, FR-003-12, FR-003-13, FR-023-01, FR-023-02, FR-026-03, FR-030-07, NFR-SEC-03, NFR-SEC-04, NFR-SEC-08, NFR-PAR-02, NFR-PAR-04, NFR-PAR-05, NFR-UI-01, NFR-UI-02, NFR-BATCH-03, AR-19, AR-26, AR-29, AR-30, AR-31, AR-37, AR-38, AR-39, AR-40, AR-41, AR-42, AR-43, AR-44, AR-46, AR-49, AR-50, AR-57, AR-68, AR-71, AR-86, UX-DR15, UX-DR22, UX-DR23, UX-DR24, UX-DR27, UX-DR44, UX-DR57, UX-DR85, UX-DR86, UX-DR100, UX-DR102, UX-DR103, UX-DR105, UX-DR109, UX-DR116, UX-DR140, UX-DR150, UX-DR154
**Scénarios golden master :** SC-003-01, SC-003-02, SC-003-04
**Prérequis :** A-003-1 et A-003-2 (messages non techniques pour montant invalide et solde insuffisant) et A-023-1 (texte d'échec de remise d'un virement), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; epic 2 terminé (connexion par JWT, format d'erreur `{"message"}`, adresse IP, service `audit`, gabarit, routeur et route `/tableau-de-bord`, `scripts/golden-master/verifier-scenario.sh`) ; story 1.2 (port `application.port.out.Horloge` et son adaptateur en horloge système, AD-19, AR-8) ; refus d'un motif de plus de 100 caractères : 400 « Le motif ne doit pas dépasser 100 caractères » (clé `virement.validation.motif.longueur`, `textes-nouveaux.md`, lot « Stories »).

**Acceptance Criteria:**

**Given** `user1` (CLIENT) authentifié et les données de référence du cœur bancaire chargées (`services/coeur-bancaire/reference-data/reference-data.sql` : comptes et opérations du legacy avec leurs identifiants, dont le COURANT `1` « FR76-0001-0001 » et l'EPARGNE `2` de `user1`)
**When** il ouvre `/virement`
**Then** l'écran affiche le titre « Effectuer un virement », l'onglet « BankApp Demo - {page} » repris du legacy, les champs « Compte source » (`virement-compte-source`) et « Compte destinataire » (`virement-compte-destinataire`) en `<select>` natifs dont la première option est « -- Choisir un compte -- », suivie des seuls comptes de `user1` (valeur d'option = identifiant du compte, libellé repris de `docs/retro/features/FEAT-003/FIELDS.md`, contenant le type « COURANT » ou « EPARGNE »), le champ « Montant (€) » (`virement-montant`) avec l'indice « Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude », le champ « Motif » facultatif (`virement-motif`, 100 caractères au plus), le bouton « Exécuter le virement » (`virement-executer`) et le lien « Virement SEPA externe — Gérer mes bénéficiaires »
**And** aucun bandeau technique EJB ou ActiveMQ n'est affiché (B-20)

**Given** un CONSEILLER ou un ADMIN authentifié, sans compte propre
**When** il ouvre `/virement`
**Then** les deux listes ne contiennent que l'option « -- Choisir un compte -- »

**Given** l'écran de virement
**When** l'utilisateur soumet sans compte source ou sans compte destinataire, puis sans montant
**Then** le message « Veuillez sélectionner les deux comptes », puis « Veuillez saisir le montant » s'affiche, le champ en erreur porte `aria-invalid="true"` et `aria-describedby`, le focus va au premier champ en erreur
**And** `POST /api/coeur-bancaire/virements` appelé directement sans l'un des deux comptes répond 400 avec `{"message":"Veuillez sélectionner les deux comptes"}`

**Given** le compte `1` choisi en source et en destination, un montant de 100 et le motif « [retro] test comptes identiques »
**When** l'utilisateur clique « Exécuter le virement »
**Then** le message « Le compte source et destination doivent être différents » s'affiche dans `virement-message-erreur`, les valeurs saisies sont conservées, aucun solde ni aucune opération ne change

**Given** un montant nul, négatif (-5) ou non numérique (« abc »)
**When** `POST /api/coeur-bancaire/virements` est appelé
**Then** la réponse est 400 avec `{"message":"Erreur lors du virement : Le montant doit etre positif"}`, sans pile d'appels ni nom d'exception, et aucun solde ne change

**Given** un compte source ou destinataire inexistant (identifiant 999)
**When** `POST /api/coeur-bancaire/virements` est appelé
**Then** la réponse est 404 avec `{"message":"Erreur lors du virement : Compte introuvable"}`, sans débit ni crédit
**And** un compte source appartenant à un autre client donne 403, sans débit ni crédit (NFR-SEC-03), contrôle fait dans le service

**Given** un compte source à 2 130,00 €
**When** un virement de 9 999,00 € est soumis
**Then** la réponse est 4xx avec `{"message":"Solde insuffisant sur le compte source"}`, aucun nom d'erreur interne n'apparaît, aucun solde ni aucune opération ne change (BUG-008, BUG-011)

**Given** une saisie rejetée par l'un des contrôles précédents
**When** la soumission est traitée
**Then** aucun virement n'est enregistré, aucune ligne n'est écrite dans `outbox` (FR-003-07), et l'entrée suivante n'est traitée qu'après ces contrôles

**Given** `user1`, un virement de 100 € du compte `1` vers le compte `2` avec le motif « loyer »
**When** il clique « Exécuter le virement », sans étape de confirmation, le bouton passant en `aria-busy` pendant l'envoi
**Then** la réponse est 201 avec `etat` EXECUTE et l'écho des champs saisis, le compte `1` est débité et le compte `2` crédité dans la même transaction (mise à jour conditionnelle `solde >= montant`), deux opérations sont enregistrées avec leur solde après : DEBIT « Virement vers {compte destinataire} - loyer » et CREDIT « Virement depuis {compte source} - loyer », statut d'affichage SUCCESS
**And** le front redirige vers `/tableau-de-bord`

**Given** un virement exécuté de 5 000,00 €, puis un de 4 999,99 €
**When** chacun est traité
**Then** le premier est exécuté et débité immédiatement avec le statut PENDING et des opérations préfixées « [EN ATTENTE] » (textes legacy gardés, BUG-009 conservé provisoirement) ; le second a le statut SUCCESS, sans préfixe
**And** les textes « [EN ATTENTE] » et « transaction importante » vivent dans un seul fichier de textes du cœur bancaire (`grep -rln "EN ATTENTE" services/coeur-bancaire/src/main` ne renvoie qu'un fichier)

**Given** un virement exécuté
**When** sa transaction est validée
**Then** une ligne `outbox` du topic interne `coeur-bancaire.virement-a-traiter.v1` (identifiant, montant, émetteur, compte destinataire, motif) est écrite dans la même transaction que le débit (FR-023-01), son consommateur dans le cœur bancaire ne débite pas un virement déjà EXÉCUTÉ et n'émet aucune notification
**And** livré deux fois, le même message brut ne produit qu'un effet (table `evenements_traites`, test Testcontainers)

**Given** l'écriture de la remise au traitement asynchrone rendue impossible (ligne `outbox` refusée)
**When** un virement valide de 100 € est soumis
**Then** la validation échoue : aucun virement, aucun débit, aucun crédit, aucune opération ; la réponse est 5xx avec `{"message":"Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard"}` ; le front reste sur `/virement` et affiche ce message ; une entrée VIREMENT est présente dans `outbox_audit` (FR-023-02, BUG-073)

**Given** un virement de 100 € exécuté avec le motif « loyer »
**When** le journal d'audit est lu par JDBC dans `AUDIT_LOGS` du service `audit`
**Then** il contient exactement, dans cet ordre, VIREMENT « Montant: {amount} - Motif: loyer - Issue: EXECUTE » puis TRANSFER_EXECUTED « Montant: {amount} - Motif: loyer », avec l'identifiant de l'émetteur et l'adresse IP d'origine, publiés sur `coeur-bancaire.audit.v1` par `outbox_audit` (transaction indépendante), clé = identifiant du virement
**And** `contrats/evenements/coeur-bancaire.audit.v1.schema.json` existe et les tests `*ContratEvenement*Test` du producteur (cœur bancaire) et du consommateur (audit) passent

**Given** deux soumissions portant le même en-tête `Idempotency-Key`
**When** elles sont traitées
**Then** un seul virement, un seul débit, et la deuxième réponse est identique à la première (AR-42)

**Given** un compte source dont le solde ne couvre qu'un des deux virements soumis en même temps
**When** les deux soumissions sont traitées en concurrence
**Then** un seul virement est exécuté, l'autre reçoit « Solde insuffisant sur le compte source », aucun solde n'est négatif (test `*TransitionConcurrente*Test`)

**Given** le port `application.port.out.Horloge` livré par la story 1.2 (adaptateur en horloge système) et `nb.fuseau=Europe/Paris`
**When** le cœur bancaire démarre en profil `rejeu` avec un seul `HORLOGE_DECALAGE` égal à (`HORLOGE_DEPART`, défaut `2026-11-17T10:00:00` Europe/Paris, − instant réel), décalage commun à tous les services
**Then** un test unitaire de l'horloge réglée de `adapter.out.horloge` constate que `instant()` vaut `2026-11-17T10:00:00` Europe/Paris à la seconde près, puis avance de la même durée que l'horloge système injectée (horloge non figée), et que `aujourdhui()` vaut `2026-11-17`
**And** sans profil `rejeu`, le bean actif reste l'horloge système de la story 1.2 ; avec un `HORLOGE_DECALAGE` illisible en profil `rejeu`, le contexte ne démarre pas

**Given** le profil `rejeu` actif
**When** le contexte du cœur bancaire démarre
**Then** un test constate qu'aucun traitement planifié métier n'est actif (le relais de l'outbox reste actif) ; le rejeu de SC-003-04 lui-même reste en epic 10

**Given** un `Instant.now()` ajouté volontairement dans une classe de `application` du cœur bancaire
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dtest='*ArchitectureTest'`
**Then** le code de sortie est non nul (`HorlogeArchitectureTest`)

**Given** un seuil de mise en attente ou un seuil d'alerte `nb.fraude.*` invalide (négatif ou nul)
**When** le service démarre
**Then** il refuse de démarrer (test `ConfigurationFraude*Test`, NFR-SEC-08)

**Given** l'API du virement
**When** `POST /api/coeur-bancaire/virements` est appelé sans jeton, puis avec un compte source d'un autre client
**Then** les réponses sont 401 puis 403 (classe `CoeurBancaireSecuriteIT`, créée ici si l'epic 2 ne l'a pas créée, complétée sinon)

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-003-01`, puis `scripts/golden-master/verifier-scenario.sh SC-003-02`, puis `scripts/golden-master/verifier-scenario.sh SC-003-04`
**Then** le code de sortie est 0 pour chacun

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-003-02 | FR-003-03 | FR-003-04 | FR-003-05 | FR-003-06 | FR-003-07 | FR-003-08 | FR-003-09 | FR-003-10 | FR-003-11 | FR-003-13 | FR-023-01 | FR-023-02 | FR-026-03'`, `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='BUG-008 | BUG-011 | BUG-073'` et `cd frontend && npx vitest run -t 'FR-003-0[1-8]|FR-003-12|FR-023-02'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.2: Évaluer les signaux de fraude de chaque virement

En tant que client,
je veux que chaque virement soit évalué sur les signaux de fraude avant sa validation,
afin que les virements suspects soient repérés avant que mon argent parte.

**Exigences :** FR-027-01, FR-027-03, FR-027-05, FR-027-06, FR-027-07, FR-027-08, FR-027-09, FR-027-10, FR-027-15, FR-003-07, FR-003-15, FR-003-17, FR-003-18, FR-003-19, FR-003-20, FR-003-21, FR-003-22, FR-003-23, FR-003-24, FR-003-25, NFR-SEC-05, NFR-SEC-08, NFR-BATCH-03, NFR-PAR-02, AR-44, AR-47, AR-50, AR-51, AR-66, AR-68
**Scénarios golden master :** aucun
**Prérequis :** A-027-11 (HIGH_FREQUENCY compte les opérations au relevé), A-027-1 (heure inhabituelle au fuseau de l'application), A-003-4 (un seul signal LARGE_AMOUNT, 40 puis 80), A-003-5 (RAPID_SUCCESSION compare au précédent virement), A-003-9 (VELOCITY_CHECK compte les virements exécutés), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; RP-03 appliqué : chaque signal est livré avec sa FR de FEAT-003 et sa FR de FEAT-027 ; story 3.1.

**Acceptance Criteria:**

**Given** un virement qui passe les contrôles de saisie et de solde
**When** il est soumis
**Then** les huit signaux LARGE_AMOUNT, HIGH_FREQUENCY, NEW_BENEFICIARY, UNUSUAL_HOURS, ROUND_AMOUNT, FOREIGN_IBAN, RAPID_SUCCESSION et VELOCITY_CHECK sont évalués de façon synchrone dans le domaine, avant la validation finale, sans appel réseau ; les signaux d'historique sont lus par le seul port `HistoriqueVirements` ; le contrôle et l'enregistrement d'un même client sont sérialisés par `pg_advisory_xact_lock(clientId)`
**And** aucun point d'entrée public n'évalue un score isolé (RP-08)

**Given** un virement rejeté par un contrôle de saisie ou de solde (comptes identiques, montant -5, solde insuffisant)
**When** il est traité
**Then** aucun signal n'est évalué, aucune alerte n'est écrite, et il n'entre pas dans le calcul de RAPID_SUCCESSION du virement suivant (FR-003-07)

**Given** des montants de 4 999,99 €, 5 000,00 €, 14 999,99 € et 15 000,00 €
**When** LARGE_AMOUNT est évalué
**Then** il n'est pas levé pour 4 999,99 €, il est levé une seule fois avec le score 40 et « Transaction amount {amount} exceeds the large threshold of {threshold} » pour 5 000,00 € et 14 999,99 €, avec le score 80 et « Transaction amount {amount} exceeds the very-large threshold of {threshold} » pour 15 000,00 € (un seul signal LARGE_AMOUNT par virement)

**Given** un client avec 100 opérations des jours précédents et 8 opérations au relevé le jour courant (Europe/Paris), puis 9
**When** HIGH_FREQUENCY est évalué
**Then** il n'est pas levé avec 8 opérations du jour ; il est levé avec le score 50 et « Customer has {count} transactions today, exceeding the daily limit of {maxDailyTransactions} » avec 9 (débits et crédits confondus, jours précédents exclus ; BUG-010, BUG-085)

**Given** un compte destinataire qui n'est pas un bénéficiaire actif du client
**When** NEW_BENEFICIARY est évalué
**Then** il est levé avec le score 35 et « Target IBAN {iban} is not registered as a trusted beneficiary » ; il n'est pas levé pour un bénéficiaire actif
**And** la table des bénéficiaires externes du cœur bancaire est créée par cette story (lecture seule ici, gestion en epic 4) avec ses lignes de référence du legacy

**Given** l'horloge injectée à 05:59, 10:00 et 23:30 Europe/Paris (fuseau `nb.fuseau`, pas `TZ`)
**When** UNUSUAL_HOURS est évalué
**Then** il est levé avec le score 45 et « Transaction initiated at hour {hourOfDay} which is outside normal operating hours (06:00-23:00) » à 05:59 et à 23:30, et pas à 10:00

**Given** des montants de 999 €, 1 000 €, 1 500 € et 3 000 €
**When** ROUND_AMOUNT est évalué
**Then** il est levé avec le score 20 et « Transaction amount {amount} is a suspicious round figure (multiple of 1000) » pour 1 000 € et 3 000 € seulement

**Given** des comptes destinataires dont le numéro commence par « DE89 », « fr76 » et « FR76 »
**When** FOREIGN_IBAN est évalué
**Then** il est levé avec le score 30 et « Target IBAN {iban} does not start with FR (possible foreign transfer) » pour « DE89 » seulement (comparaison insensible à la casse)

**Given** un précédent virement du client soumis au contrôle 29 s puis 31 s avant (horloge injectée)
**When** RAPID_SUCCESSION est évalué
**Then** il est levé avec le score 55 et « Previous transaction was only {elapsedSeconds} seconds ago (threshold: {rapidSuccessionSeconds}s) » à 29 s, et pas à 31 s

**Given** un client avec 4, puis 5 virements exécutés de plus de 500 € dans les 60 dernières minutes
**When** VELOCITY_CHECK est évalué
**Then** il n'est pas levé avec 4 ; il est levé avec le score 65 et « Customer executed {highValueCount} transactions above 500 EUR in the last {velocityWindowMinutes} minutes (limit: {velocityMaxCount}) » avec 5

**Given** un virement dont la somme des scores vaut 75, puis un dont elle vaut 80
**When** le contrôle se termine
**Then** à 75 aucune alerte n'est écrite ; à 80 une alerte par signal levé est enregistrée dans `FRAUD_ALERTS` (table créée par cette story) au statut OPEN, avec son score et sa description, dans la transaction du virement
**And** jusqu'à la story 3.4 le virement reste exécuté quel que soit son score

**Given** les données de référence et l'horloge à `2026-11-17T10:00:00` Europe/Paris, sans virement dans les 30 s précédentes et avec 8 opérations au plus ce jour-là
**When** `user1` vire 100 € du compte `1` (COURANT) vers le compte `2` (EPARGNE)
**Then** seul NEW_BENEFICIARY (35) est levé, aucune alerte n'est écrite et le virement est exécuté (parcours nominal de SC-003-04)

**Given** un virement vers « FR7630006000011234567890189 »
**When** le contrôle de fraude journalise son évaluation
**Then** aucune ligne du journal ne contient l'IBAN en clair ; seule la forme masquée « FR76****0189 » apparaît (objet-valeur `Iban` dont `toString()` masque, test de référence commun du masquage ; BUG-086)

**Given** un seuil de score `nb.fraude.*` invalide (négatif)
**When** le service démarre
**Then** il refuse de démarrer (`ConfigurationFraude*Test`)

**Given** l'IBAN en clair réintroduit volontairement dans un message de journal du contrôle
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-027-15'`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-027-01 | FR-027-03 | FR-027-05 | FR-027-06 | FR-027-07 | FR-027-08 | FR-027-09 | FR-027-10 | FR-027-15 | FR-003-07 | FR-003-15 | FR-003-17 | FR-003-18 | FR-003-19 | FR-003-20 | FR-003-21 | FR-003-22 | FR-003-23 | FR-003-24 | FR-003-25'` et `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='BUG-010 | BUG-085 | BUG-086'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.3: Notifier le client de l'issue de son virement

En tant que client,
je veux recevoir une seule notification par virement, à la décision du contrôle,
afin de savoir ce qu'il est advenu de mon virement sans doublon.

**Exigences :** FR-002-09, FR-002-10, FR-003-10, FR-003-14, FR-003-26, FR-023-03, FR-023-05, FR-023-06, FR-025-01, FR-025-02, FR-025-04, NFR-PAR-02, AR-22, AR-26, AR-29, AR-31, AR-38, AR-39, AR-41, AR-65, UX-DR150
**Scénarios golden master :** aucun
**Prérequis :** stories 3.1 et 3.2.

**Acceptance Criteria:**

**Given** un virement exécuté de 100 € vers le compte « FR76-0001-0002 », sans motif, puis avec le motif « loyer », puis avec un motif fait d'espaces
**When** le contrôle décide de son issue
**Then** le cœur bancaire publie, par l'outbox et dans la transaction de la décision, un événement `coeur-bancaire.virement-decide.v1` (clé `virementId` ; `virementId`, `clientId`, `etat`, `notification`) dont le bloc `notification` porte le type CONFIRMATION et le message rendu « Virement de {montant} EUR vers le compte {numCompte} confirme », « … confirme (motif : loyer) », puis le message sans motif
**And** `contrats/evenements/coeur-bancaire.virement-decide.v1.schema.json` existe avec un exemple, et les tests `*ContratEvenement*Test` du producteur et du consommateur passent

**Given** un virement exécuté de 5 000,00 €, puis de 4 999,99 €
**When** le contrôle décide
**Then** le premier produit une ALERTE « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. » ; le second une CONFIRMATION

**Given** l'événement de décision publié
**When** le service `notifications` le consomme
**Then** il stocke la notification telle que rendue par le producteur, en tête de liste de l'utilisateur, non lue ; il ne compose aucun texte et ne lit aucun seuil métier
**And** `! grep -rn 'nb\.fraude\.' services/notifications/src/main` sort en code 0

**Given** le même événement brut livré deux fois
**When** le consommateur le traite
**Then** une seule notification existe pour ce virement (table `evenements_traites`, test Testcontainers ; BUG-007)

**Given** un virement exécuté remis au traitement asynchrone
**When** le consommateur de `coeur-bancaire.virement-a-traiter.v1` le traite
**Then** aucune notification n'est produite ; la notification existe déjà dès la décision, avant la remise (FR-023-03)

**Given** un utilisateur qui a déjà 20 notifications (`nb.notifications.plafond=20`)
**When** une 21e arrive
**Then** la plus ancienne est supprimée et la nouvelle est en tête ; l'utilisateur en a 20

**Given** des notifications lues et non lues d'un utilisateur
**When** le service `notifications` redémarre sur la même base (volume `pgdata-notifications`)
**Then** les notifications, leur ordre et leur état de lecture sont identiques (BUG-006)

**Given** `nb.notifications.plafond` invalide (0 ou négatif)
**When** le service `notifications` démarre
**Then** il refuse de démarrer

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-002-10 | FR-003-10 | FR-003-14 | FR-003-26 | FR-023-03 | FR-025-01 | FR-025-02'`, `mvn -B -q -f services/notifications/pom.xml verify -Dgroups='FR-002-09 | FR-023-05 | FR-023-06 | FR-025-04'` et `mvn -B -q -f services/notifications/pom.xml verify -Dgroups='BUG-006 | BUG-007'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.4: Suspendre, refuser, revoir et rejeter à l'échéance un virement (socle RG-027-003)

En tant que client et en tant qu'administrateur,
je veux qu'un virement suspect soit suspendu avant tout débit et revu par l'administrateur sous 24 heures, et qu'un virement non vérifiable soit refusé sans débit,
afin qu'aucun argent ne parte sur un virement suspect ou non contrôlé.

**Exigences :** FR-027-02, FR-027-04, FR-027-16, FR-027-17, FR-016-19, FR-026-02, FR-026-03, FR-023-01, FR-023-02, FR-023-04, FR-002-10, FR-003-07, FR-003-08, FR-003-09, FR-003-12, FR-003-13, FR-003-15, FR-003-16, FR-003-27, FR-030-07, NFR-SEC-03, NFR-SEC-04, NFR-SEC-08, NFR-SEC-09, NFR-OBS-02, NFR-BATCH-01, NFR-BATCH-02, NFR-PAR-02, AR-28, AR-32, AR-41, AR-43, AR-46, AR-52, AR-63, AR-64, AR-66, AR-89, AR-105, UX-DR17, UX-DR43, UX-DR50, UX-DR53, UX-DR94, UX-DR107, UX-DR111, UX-DR117, UX-DR118, UX-DR119, UX-DR133, UX-DR134, UX-DR136, UX-DR137, UX-DR138, UX-DR139, UX-DR144, UX-DR145, UX-DR146, UX-DR147, UX-DR153
**Scénarios golden master :** aucun
**Prérequis :** A-003-3 (contrôles de saisie et de solde avant le contrôle de fraude), A-003-8 (refus pour erreur sans notification), A-025-3 (plafond de notifications appliqué à l'avis de suspension), A-027-4 (texte de l'avis de suspension), A-027-10 (le client voit l'état de ses virements passés en revue), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; A-016-3 (file triée par heure butoir), A-016-5 (seules les alertes OPEN passent à REVIEWED), A-016-6 (rejet admin sans objet à l'heure butoir), A-016-7 (historique des virements traités), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») (reprise avant la story de FR-016-19) ; A-027-9 (rejet automatique tracé au journal d'audit), confirmé le 2026-10-05 (reprise avant la story de FR-027-17) ; RP-02 (reclassement possible en risque élevé de A-027-7, A-003-3, A-021-5, A-019-1, A-027-10), tranché le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; AR-105 réglé par cette story ; stories 3.1 à 3.3 ; epic 2 (route `/administration` réservée à l'ADMIN, topic `identite-clients.client-modifie.v1` produit) ; (format décidé : `{date}` en `dd/MM/yyyy` et `{heure}` en `HH:mm`, comme les dates du legacy, FR-002-06 ; heure en Europe/Paris) ; historique des virements traités décidé le 2026-10-05 (voir le critère de l'historique).

**Acceptance Criteria:**

**Given** une saisie invalide (comptes identiques, montant -5, solde insuffisant)
**When** elle est soumise
**Then** elle est rejetée avec son message de la story 3.1 avant tout contrôle de fraude, sans virement enregistré ni alerte (A-003-3)

**Given** un virement de score 75 et de 9 999,99 €
**When** le contrôle décide
**Then** il est EXÉCUTÉ, débité dans la transaction de soumission, avec l'audit VIREMENT « … - Issue: EXECUTE » puis TRANSFER_EXECUTED, et une seule notification (CONFIRMATION ou ALERTE selon le montant)

**Given** un virement de score 76, puis un virement de 10 000,00 € de score nul (horloge injectée : soumission le 5 à 10:15)
**When** le contrôle décide
**Then** chacun passe à SUSPENDU avant toute modification de solde : aucun débit, aucun crédit, aucune opération, aucune remise au traitement asynchrone ; l'heure butoir vaut le 6 à 10:15 (soumission + 24 h calendaires, port Horloge) ; une seule notification FRAUDE porte le texte « Votre virement de {montant} EUR vers le compte {numCompte} est en cours de vérification. Rien n'a été débité. Vous aurez notre réponse avant le {date} à {heure}. Sans décision à cette heure, il sera rejeté sans aucun débit. » ; l'audit contient VIREMENT « … - Issue: SUSPENDU » (BUG-076)
**And** la réponse de `POST /api/coeur-bancaire/virements` porte `etat` SUSPENDU et `heureButoir`

**Given** un virement suspendu, sur l'écran `/virement`
**When** la réponse arrive
**Then** le front reste sur `/virement` (pas de redirection), affiche en haut du formulaire un `alert-warning` `role="status"` (`virement-avis-suspension`, heure butoir dans `virement-avis-heure-butoir`) qui reçoit le focus, au texte `virement.avis.suspension`, et vide le formulaire
**And** ni l'avis ni la notification ne contiennent « fraude », « score » ni un nom de signal ; le message « Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié. » n'existe plus : `! grep -rn "alerte(s) fraude" frontend/src services/*/src/main` sort en code 0 (FR-003-16)

**Given** la story terminée (test de suspension)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dtest='ControleFraudeSuspension*Test'` puis `cd frontend && npx vitest run -t 'FR-027-04'`
**Then** chaque commande sort en code 0 ; et si la condition de suspension est ramenée volontairement à « score >= 75 », la première commande sort en code non nul (cas « score 75 → exécuté »)

**Given** l'accès à l'historique du contrôle qui dépasse `nb.fraude.acces-historique.delai` (test d'intégration avec `nb.fraude.acces-historique.delai=1ms` ou un verrou tenu sur la table de l'historique), puis une erreur simulée du contrôle, pour un montant de 50 € puis de 50 000 €
**When** le virement est soumis
**Then** il est persisté à l'état REFUSÉ_ERREUR_CONTRÔLE, aucun solde ne change, aucune opération, aucune remise, aucune notification ; l'erreur est journalisée ; l'audit contient la seule entrée VIREMENT « … - Issue: REFUSE_ERREUR_CONTROLE » ; la réponse est une erreur `{"message": "…", "reference": "<identifiant de trace>"}` (BUG-084)
**And** l'adaptateur entrant enrichit l'observation courante de l'attribut `virement.controle.issue` (`EXECUTE`, `SUSPENDU`, `REFUSE_ERREUR_CONTROLE`) et lui signale l'erreur ; `curl -s http://localhost:16686/api/traces/<reference>` renvoie une trace qui contient `virement.controle.issue` et un statut d'erreur

**Given** un refus pour erreur du contrôle, sur l'écran `/virement`
**When** la réponse arrive
**Then** la vue de refus remplace le formulaire, sans entrée de menu : `alert-warning` `role="alert"` avec, mot pour mot, « Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence {référence}. », la référence sélectionnable en police à chasse fixe dans `virement-refus-reference`, le focus sur le titre « Effectuer un virement », aucune mention de fraude

**Given** la story terminée (test du refus sur erreur du contrôle)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dtest='ControleFraudeErreur*Test'` puis `cd frontend && npx vitest run -t 'FR-003-27'`
**Then** chaque commande sort en code 0 ; et si l'erreur d'accès à l'historique est volontairement avalée (virement exécuté malgré l'erreur), la première commande sort en code non nul

**Given** la règle « aucun span créé à la main » et la question ouverte AR-105
**When** je lance `! grep -rnE 'Observation\.(createNotStarted|start)\(|@Observed|@NewSpan|spanBuilder\(|nextSpan\(' services/*/src/main --include=*.java`
**Then** le code de sortie est 0, et la trace du refus ci-dessus prouve que l'enrichissement de l'observation courante suffit : AR-105 est close, la réponse (enrichissement admis, création d'une Observation interdite) étant consignée dans le compte rendu de la story

**Given** la machine à 5 états du domaine (EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE)
**When** une transition hors de soumission → EXÉCUTÉ | SUSPENDU | REFUSÉ_ERREUR_CONTRÔLE, SUSPENDU → LIBÉRÉ | REJETÉ, LIBÉRÉ → EXÉCUTÉ | REJETÉ est demandée (ex. REJETÉ → LIBÉRÉ, EXÉCUTÉ → SUSPENDU)
**Then** elle est refusée sans effet (mise à jour conditionnelle `UPDATE … WHERE id = ? AND statut = ?`, zéro ligne) ; aucun solde ne change hors passage à EXÉCUTÉ (tests `MachineEtatsVirement*Test`, `*TransitionConcurrente*Test`)

**Given** un ADMIN, un virement SUSPENDU avec deux alertes OPEN et une alerte CONFIRMED, un solde suffisant, l'horloge à l'heure butoir − 1 s
**When** il libère le virement avec le motif « vérifié par téléphone »
**Then** le virement passe à LIBÉRÉ, est remis sur `coeur-bancaire.virement-a-traiter.v1` dans la même transaction, puis le consommateur le débite et le passe à EXÉCUTÉ avec ses deux opérations ; les deux alertes OPEN passent à REVIEWED dans la même action, la CONFIRMED reste CONFIRMED ; aucune nouvelle notification ; l'audit contient, dans cet ordre, VIREMENT, ADMIN_TRANSFER_RELEASE « Liberation virement id={transferId} - Motif: vérifié par téléphone - Issue: EXECUTE » (identifiant de l'administrateur, adresse IP), TRANSFER_EXECUTED ; l'écran affiche « Libération enregistrée : le virement est accepté, son débit va être effectué. »

**Given** un virement SUSPENDU dont le solde source est devenu insuffisant
**When** l'ADMIN le libère avant l'heure butoir
**Then** la libération est refusée et le virement passe à REJETÉ sans aucun débit, ADMIN_TRANSFER_RELEASE porte « Issue: REJETE », l'écran affiche « Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit. » (B6)

**Given** un virement LIBÉRÉ dont le solde devient insuffisant avant le débit asynchrone
**When** le consommateur de `coeur-bancaire.virement-a-traiter.v1` le traite
**Then** il passe à REJETÉ sans débit ni crédit ni notification, avec l'audit TRANSFER_REJECT_INSUFFICIENT_FUNDS par « SYSTEM », sans adresse IP, « Rejet virement id={transferId} - Motif: solde insuffisant au debit » (B7)

**Given** l'horloge à l'heure butoir exacte, puis à l'heure butoir + 1 s
**When** l'ADMIN libère le virement
**Then** la libération est refusée, le virement passe à REJETÉ sans débit et l'écran affiche « Heure butoir dépassée : le virement est rejeté. » en `alert-error`, que le traitement planifié soit passé ou non (B5)

**Given** l'écriture de la remise impossible au moment d'une libération
**When** l'ADMIN libère
**Then** la libération échoue et le virement reste SUSPENDU, sans débit (A-023-3)

**Given** la story terminée (test de libération)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dtest='LiberationVirement*Test'`
**Then** le code de sortie est 0 ; et si la comparaison à l'heure butoir est volontairement rendue large (libération acceptée à l'heure butoir), la commande sort en code non nul

**Given** un ADMIN et un virement SUSPENDU avec une alerte OPEN, avant l'heure butoir
**When** il le rejette avec le motif « client injoignable »
**Then** le virement passe à REJETÉ sans débit, l'alerte OPEN passe à REVIEWED, aucune notification, l'audit contient ADMIN_TRANSFER_REJECT « Rejet virement id={transferId} - Motif: client injoignable », l'écran affiche « Rejet enregistré : le virement est rejeté sans débit. »
**And** à l'heure butoir ou après, le rejet est sans objet et l'écran affiche « Heure butoir dépassée : le virement est rejeté. » (A-016-6)

**Given** la story terminée (test du rejet par l'administrateur)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dtest='RejetVirement*Test'`
**Then** le code de sortie est 0 ; et si le passage des alertes OPEN à REVIEWED est volontairement retiré, la commande sort en code non nul

**Given** un motif vide ou fait d'espaces
**When** l'ADMIN valide la libération ou le rejet
**Then** la modale affiche « Veuillez indiquer un motif » sur le champ « Motif de la décision * » (`revue-motif`, `aria-invalid="true"`) ; appelée directement, l'API répond 400 avec `{"message":"Veuillez indiquer un motif"}` ; le virement ne change pas

**Given** un CLIENT ou un CONSEILLER authentifié
**When** il appelle l'API de la file de revue, de libération ou de rejet
**Then** la réponse est 403, rôle vérifié dans le service, et le virement ne change pas (D6)

**Given** un virement SUSPENDU soumis le 5 à 10:15, l'horloge injectée
**When** le traitement planifié du rejet automatique tourne le 6 à 10:14:59, puis à 10:15:00 et à 10:15:01
**Then** le virement est encore SUSPENDU à 10:14:59 (23 h 59 min 59 s), puis REJETÉ à 10:15:00, sans débit ni notification, avec l'audit TRANSFER_AUTO_REJECT par « SYSTEM » « Rejet automatique virement id={transferId} »
**And** le traitement, déclenché par `@Scheduled` dans `adapter.in.planification` toutes les minutes en zone `Europe/Paris`, écrit dans `BATCH_JOB_LOGS` une ligne `SUSPENDED_TRANSFERS_EXPIRY` seulement s'il rejette au moins un virement ou échoue ; il est désactivé en profil `rejeu`

**Given** une libération et un rejet automatique lancés en concurrence sur le même virement à l'heure butoir
**When** les deux transitions s'exécutent
**Then** une seule s'applique : le virement est REJETÉ, aucun débit, une seule entrée d'audit de décision (horloge qui tranche, B5)

**Given** la story terminée (test du rejet automatique à l'échéance)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dtest='EcheanceRevue*Test'`
**Then** le code de sortie est 0 ; et si l'heure butoir est volontairement calculée à soumission + 25 h, la commande sort en code non nul

**Given** les six parcours (exécuté ; suspendu puis libéré ; suspendu, libéré puis rejeté au débit ; suspendu puis rejeté par l'administrateur ; suspendu puis rejeté automatiquement ; refusé pour erreur du contrôle)
**When** le journal d'audit est lu par JDBC dans `AUDIT_LOGS`
**Then** il contient exactement 2, 3, 3, 2, 2 et 1 entrées, dans l'ordre de FR-026-03, chacune écrite par `outbox_audit` hors de la transaction du virement (tests `AuditVirement*Test`)

**Given** un ADMIN sur `/administration`
**When** la section de revue se charge
**Then** elle a pour titre « Virements suspendus — En attente de revue », liste les virements SUSPENDU triés par heure butoir la plus proche, avec les colonnes « Client » (nom complet lu dans la projection clients du cœur bancaire, alimentée par `identite-clients.client-modifie.v1`, test consommateur `*ContratEvenement*Test` et `versionAgregat`), « Montant », « Compte destinataire », « Motif de suspension » (« Score cumulé {score} - signaux : {signaux} » ou « Montant supérieur ou égal au seuil anti-fraude ({seuilFraudeEur} EUR) »), « Heure butoir », « Actions » ; boutons « Libérer » (`revue-liberer-{transferId}`) et « Rejeter » (`revue-rejeter-{transferId}`) qui ouvrent une modale (titre « Libérer le virement de {montant} EUR vers le compte {numCompte} » ou « Rejeter le virement de {montant} EUR vers le compte {numCompte} », champ « Motif de la décision * », « Valider » `revue-valider`, « Annuler ») avec piège du focus et `Échap` ; sans virement suspendu : « Aucun virement suspendu »
**And** après une décision, la ligne quitte la file au rechargement de la section ; les boutons ne sont jamais masqués côté client, le serveur décide ; la section est un composant autonome que l'epic 9 placera après « Gestion des clients » et avant « Alertes Fraude — Moteur de détection »

**Given** des virements libérés ou rejetés
**When** l'ADMIN consulte `/administration`
**Then** l'historique des virements traités, sous la file, liste leur état et le motif de leur issue, dont « Rejeté : solde insuffisant au moment du débit » pour un rejet au débit ; il n'apparaît que s'il contient au moins un virement (décision du 2026-10-05 : titre « Virements traités » ; colonnes de la file des virements suspendus — « Client », « Montant », « Compte destinataire », « Motif de suspension », « Heure butoir » — plus « Issue » et « Date de l'issue » ; issues « Libéré », « Rejeté », « Rejeté - heure butoir dépassée », « Rejeté : solde insuffisant au moment du débit » ; les 50 derniers, du plus récent au plus ancien ; clés `revue.historique.*` et `revue.issue.*`, `textes-nouveaux.md`, lot « Stories »)

**Given** `user1` avec un virement SUSPENDU, un LIBÉRÉ, un REJETÉ, un EXÉCUTÉ après libération, un exécuté directement et un refusé pour erreur du contrôle
**When** il ouvre `/virement` puis `/tableau-de-bord`
**Then** chaque page affiche en bas, après le contenu existant, la section « Suivi de mes virements » (`virement-virements-revue`, `dashboard-virements-revue`) avec l'aide « Vous n'avez rien à faire : l'état de chaque virement se met à jour sur cette page. Un virement rejeté n'a pas été débité ; vous pouvez refaire votre demande. », les colonnes « Date », « Compte destinataire », « Montant », « Etat », « Réponse avant le » ; seuls les quatre premiers virements y figurent, avec les badges « En cours de vérification - non débité », « Accepté - pas encore débité », « Rejeté - non débité », « Exécuté - débité » (`virement-etat-{transferId}`), et « {date} à {heure} » pour le SUSPENDU
**And** sans virement passé en revue, la section affiche « Aucun virement à suivre » ; si la notification FRAUDE a été supprimée par le plafond, l'état et l'heure butoir restent visibles (A-025-3)

**Given** un seuil de fraude `nb.fraude.*` ou `nb.fraude.acces-historique.delai` invalide (nul ou négatif)
**When** le cœur bancaire démarre
**Then** il refuse de démarrer (`ConfigurationFraude*Test`)

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-027-02 | FR-027-04 | FR-027-16 | FR-027-17 | FR-016-19 | FR-026-02 | FR-026-03 | FR-023-01 | FR-023-02 | FR-023-04 | FR-002-10 | FR-003-09 | FR-003-13 | FR-003-15 | FR-003-16 | FR-003-27'`, `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='BUG-076 | BUG-084 | BUG-007'` et `cd frontend && npx vitest run -t 'FR-027-04|FR-027-16|FR-016-19|FR-003-12|FR-003-27'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.5: Suspendre un virement qui fait dépasser le cumul glissant sur 24 heures

En tant que client,
je veux qu'un virement qui fait dépasser 10 000 € émis sur 24 heures glissantes soit suspendu,
afin qu'un découpage en petits virements ne contourne pas le contrôle.

**Exigences :** FR-027-18, FR-027-02, NFR-SEC-08, NFR-PAR-02, AR-50, AR-66, UX-DR145
**Scénarios golden master :** aucun
**Prérequis :** A-027-7 (le cumul compte les virements exécutés et suspendus, pas les rejetés ni les refusés), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; story 3.4 (cette story la suit immédiatement, D10, risque R-1 levé).

**Acceptance Criteria:**

**Given** un client qui a déjà émis 6 000 € dans les 24 dernières heures (horloge injectée)
**When** il soumet 4 000,00 € (total 10 000,00 €), puis, dans un autre cas, 4 000,01 €
**Then** le virement de 4 000,00 € n'est pas suspendu par le cumul ; celui de 4 000,01 € est SUSPENDU avant tout débit, avec la notification FRAUDE et l'heure butoir de la story 3.4

**Given** un virement émis il y a 24 h et 1 s
**When** le cumul est calculé
**Then** ce virement n'est pas compté (fenêtre glissante de 24 heures, virement courant inclus)

**Given** des virements EXÉCUTÉ et SUSPENDU, puis REJETÉ et REFUSÉ_ERREUR_CONTRÔLE dans la fenêtre
**When** le cumul est calculé
**Then** seuls les EXÉCUTÉ et SUSPENDU sont comptés, quel que soit le bénéficiaire

**Given** l'accès au cumul qui dépasse `nb.fraude.acces-historique.delai`
**When** le virement est soumis
**Then** il est REFUSÉ_ERREUR_CONTRÔLE comme à la story 3.4 (lecture par le port `HistoriqueVirements`, sous `pg_advisory_xact_lock(clientId)`)

**Given** un virement suspendu par le cumul
**When** l'ADMIN ouvre la file de revue
**Then** la cellule « Motif de suspension » affiche « Cumul sur 24 h glissantes de {cumul} EUR, supérieur au plafond de {plafond} EUR »

**Given** un plafond de cumul `nb.fraude.*` invalide (nul ou négatif)
**When** le cœur bancaire démarre
**Then** il refuse de démarrer (`ConfigurationFraude*Test`)

**Given** la comparaison au plafond volontairement rendue large (`>=` au lieu de `>`)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-027-18'`
**Then** le code de sortie est non nul (cas 10 000,00 € non suspendu)

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-027-18 | FR-027-02'` et `cd frontend && npx vitest run -t 'FR-027-18'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.6: Refaire un virement refusé depuis un formulaire prérempli

En tant que client,
je veux refaire un virement refusé pour erreur du contrôle sans tout ressaisir,
afin de renouveler ma demande facilement.

**Exigences :** FR-003-28, FR-003-27, NFR-PAR-02, AR-42, AR-66, UX-DR139
**Scénarios golden master :** aucun
**Prérequis :** A-003-11 (la reprise reprend compte source, compte destinataire, montant et motif), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; stories 3.4 et 3.5 (D12).

**Acceptance Criteria:**

**Given** l'écran de refus pour erreur du contrôle d'un virement de 150 € du compte `1` vers le compte `2` avec le motif « loyer »
**When** il s'affiche
**Then** il propose le bouton « Refaire ma demande » (`virement-reprise`)

**Given** l'écran de refus
**When** le client clique « Refaire ma demande »
**Then** le formulaire « Effectuer un virement » s'affiche prérempli avec le compte source `1`, le compte destinataire `2`, le montant 150 et le motif « loyer », repris de l'écho de la réponse de `POST /api/coeur-bancaire/virements`
**And** rien n'est soumis tant que le client n'a pas cliqué « Exécuter le virement » ; la nouvelle soumission porte une nouvelle `Idempotency-Key` et repasse par tous les contrôles

**Given** un virement exécuté ou suspendu
**When** sa réponse arrive
**Then** aucun bouton « Refaire ma demande » n'est affiché

**Given** le préremplissage volontairement privé du motif
**When** je lance `cd frontend && npx vitest run -t 'FR-003-28'`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t 'FR-003-28'` et `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-003-28'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.7: Consulter le relevé d'un compte

En tant que client,
je veux consulter toutes les opérations d'un de mes comptes,
afin de suivre mes débits et mes crédits.

**Exigences :** FR-005-01, FR-005-02, FR-005-03, FR-005-04, FR-005-05, FR-005-06, FR-005-07, FR-005-08, NFR-SEC-03, NFR-PERF-01, NFR-UI-01, NFR-UI-02, AR-36, AR-45, AR-47, AR-49, AR-86, UX-DR27, UX-DR40, UX-DR47, UX-DR48, UX-DR54, UX-DR78, UX-DR87, UX-DR89, UX-DR90, UX-DR98, UX-DR108, UX-DR121, UX-DR150, UX-DR156, UX-DR157
**Scénarios golden master :** SC-005-01
**Prérequis :** story 3.1 (comptes, opérations et données de référence du cœur bancaire) ; Couleurs (décision du 2026-10-05 : uniquement les tokens du thème) : « CREDIT » #2e7d32 → `success` ; « DEBIT » #E3000F → `error` ; fond jaune des opérations en attente → `warning-container`.

**Acceptance Criteria:**

**Given** aucune session authentifiée
**When** `/releve` est demandé
**Then** le routeur redirige vers `/login` ; `GET /api/coeur-bancaire/comptes` sans jeton répond 401

**Given** `user1` authentifié
**When** il ouvre `/releve`
**Then** l'onglet est « BankApp Demo - Relevé de compte », la section « Relevé de compte » propose le sélecteur natif « Compte : » limité à ses comptes, le premier compte est sélectionné et ses opérations sont chargées ; l'information affiche « {type de compte} — N° {numéro de compte} — Solde actuel : {solde} » avec le solde en « € » à 2 décimales au moins (ex. « COURANT — N° FR76-0001-0001 — Solde actuel : … »)
**And** aucun bandeau @NamedQuery n'est affiché (B-22)

**Given** un utilisateur sans compte
**When** il ouvre `/releve`
**Then** le sélecteur n'a aucune option et aucune opération n'est affichée

**Given** le compte `1` sélectionné
**When** l'utilisateur choisit le compte `2`
**Then** la liste des comptes (soldes à jour) puis les opérations du compte `2` sont rechargées sans recharger la page, le panneau d'informations et le tableau sont rafraîchis, et le compteur « {N} opération(s) » est annoncé (`aria-live="polite"`)

**Given** un compte avec des opérations
**When** le tableau s'affiche
**Then** toutes les opérations sont listées sans limite par date décroissante, avec le compteur « {N} opération(s) » et les colonnes « Date », « Description », « Type », « Montant », « Solde après » (en-têtes rendus en capitales par CSS, texte source dans sa casse) ; date au format dd/MM/yyyy HH:mm en Europe/Paris ; montant préfixé « + » pour un CREDIT et « - » (tiret ASCII) pour un DEBIT, en euros à 2 décimales au moins ; solde après sans signe
**And** un compte sans opération affiche la ligne « Aucune transaction pour ce compte. » sous les en-têtes conservés

**Given** une opération dont le libellé commence par « [EN ATTENTE] » suivi d'une espace
**When** elle est affichée
**Then** elle porte le badge « En attente », son libellé est affiché sans ses 13 premiers caractères, et la ligne est sur fond d'avertissement ; une opération sans ce préfixe n'a ni badge ni fond

**Given** `user1`
**When** il demande les opérations d'un compte d'un autre client, puis d'un compte inexistant
**Then** les réponses sont 403, puis 404, au format `{"message"}`, sans aucune opération renvoyée

**Given** un CLIENT, un CONSEILLER, un ADMIN sur `/releve`
**When** la navigation s'affiche
**Then** « Espace Conseiller » n'apparaît que pour CONSEILLER et ADMIN, « Administration » que pour ADMIN

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-005-01`
**Then** le code de sortie est 0

**Given** le tri volontairement inversé (date croissante)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-005-05'`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-005-01 | FR-005-02 | FR-005-04 | FR-005-05'` et `cd frontend && npx vitest run -t 'FR-005-0[1-8]'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.8: Voir ses comptes et ses dernières opérations sur le tableau de bord

En tant que client,
je veux voir mon solde total, mes comptes et les dernières opérations de mon compte courant,
afin d'avoir une vue d'ensemble dès ma connexion.

**Exigences :** FR-002-01, FR-002-02, FR-002-03, FR-002-04, FR-002-05, FR-002-06, FR-002-11, FR-003-12, NFR-SEC-03, NFR-PERF-01, NFR-PAR-02, NFR-UI-01, AR-27, AR-45, AR-47, AR-49, AR-85, UX-DR29, UX-DR39, UX-DR47, UX-DR48, UX-DR54, UX-DR56, UX-DR70, UX-DR77, UX-DR99, UX-DR115, UX-DR153, UX-DR156, UX-DR157
**Scénarios golden master :** SC-002-01, SC-003-04
**Prérequis :** A-002-3 (client sans compte COURANT : tableau d'opérations vide), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; stories 3.1, 3.4 et 3.7 ; epic 2 (gabarit, « Bonjour, {fullName} », lien « Deconnexion »).

**Acceptance Criteria:**

**Given** `user1` authentifié
**When** il ouvre `/tableau-de-bord`
**Then** ses données sont relues à chaque ouverture (`GET /api/identite-clients/moi`) et l'en-tête affiche « Bonjour, {fullName} » et le lien « Deconnexion » ; les appels aux services (identité, cœur bancaire) partent en parallèle, jamais en cascade

**Given** un utilisateur dont la fiche a été supprimée pendant sa session
**When** il ouvre `/tableau-de-bord`
**Then** aucune erreur technique ni trace n'est affichée, le jeton est supprimé et le front affiche `/login` sans session authentifiée (BUG-004)

**Given** `user1` et ses comptes de référence
**When** le tableau de bord se charge
**Then** le séparateur « Mes comptes Demo Bank » (rendu « MES COMPTES DEMO BANK ») précède la carte « Solde total Demo Bank » (rendue en capitales), sous-titre « Tous comptes Demo Bank confondus », qui affiche la somme des soldes de tous ses comptes ; chaque compte affiche son type (COURANT, EPARGNE…), son solde en EUR à 2 décimales avec séparateur de milliers au format du legacy, et « N {numéro de compte} »
**And** un utilisateur sans compte voit EUR 0.00 au solde total

**Given** un client dont le compte EPARGNE a été créé avant le compte COURANT, avec plus de 5 opérations sur le COURANT
**When** le tableau de bord se charge
**Then** la section « Activite recente » / « Dernieres operations » affiche les 5 opérations les plus récentes du compte COURANT, de la plus récente à la plus ancienne, avec les colonnes « Date », « Description », « Type », « Montant », « Solde apres » (rendues « DATE DESCRIPTION TYPE MONTANT SOLDE APRES »), la date au format dd/MM/yyyy HH:mm, le type DEBIT ou CREDIT, les montants au format `EURx,xxx.xx`, et le bouton « Voir le releve complet » vers `/releve` (BUG-005)
**And** sans compte, ou sans compte COURANT, le tableau est vide ; le bouton « Voir le releve complet » reste affiché

**Given** `user1` venant d'exécuter 100 € du compte `1` vers le compte `2` avec le motif « [retro] virement doré 100EUR »
**When** le front le redirige vers `/tableau-de-bord`
**Then** les soldes sont à jour et la ligne DEBIT du virement figure en tête des dernières opérations
**And** aucun bandeau ni libellé technique n'est affiché (B-17, B-18, B-19)

**Given** un CLIENT, un CONSEILLER, un ADMIN
**When** la navigation du tableau de bord s'affiche
**Then** tous voient « Tableau de bord », « Virement », « Virements programmes », « Releve », « Budget », « Prets », « Documents », « Bourse », « Mon Conseiller », « Produits », « Comptes Tiers », « Beneficiaires » ; « Espace Conseiller » seulement pour CONSEILLER et ADMIN, « Administration » seulement pour ADMIN

**Given** le tableau de bord complet
**When** son texte rendu est lu
**Then** la section « Suivi de mes virements » (story 3.4) reste la dernière section de la page, après le contenu legacy

**Given** `GET /api/coeur-bancaire/tableau-de-bord` appelé par `user1` avec le compte d'un autre client en paramètre, puis sans jeton
**When** la requête est traitée
**Then** seules les données de `user1` sont renvoyées (identifiant pris du jeton), puis la réponse est 401

**Given** le premier compte (quel que soit son type) volontairement repris à la place du COURANT
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='BUG-005'`
**Then** le code de sortie est non nul

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-002-01`, puis `scripts/golden-master/verifier-scenario.sh SC-003-04`
**Then** le code de sortie est 0 pour chacun

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-002-03 | FR-002-04 | FR-002-05 | BUG-005'` et `cd frontend && npx vitest run -t 'FR-002-0[1-6]|FR-002-11|FR-003-12|BUG-004'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.9: Lire et gérer ses notifications sur le tableau de bord

En tant que client,
je veux lire mes notifications, les marquer comme lues et les effacer,
afin de suivre ce qui se passe sur mes comptes.

**Exigences :** FR-002-07, FR-002-08, FR-025-05, FR-025-06, FR-025-07, NFR-SEC-03, NFR-UI-01, AR-22, AR-36, AR-37, AR-45, UX-DR46, UX-DR55, UX-DR87, UX-DR91, UX-DR101, UX-DR113, UX-DR153
**Scénarios golden master :** SC-002-02
**Prérequis :** stories 3.3 et 3.8.

**Acceptance Criteria:**

**Given** `user1` avec des notifications CONFIRMATION, ALERTE et FRAUDE, et `user2` avec les siennes
**When** `user1` ouvre `/tableau-de-bord`
**Then** la section « Centre de notifications » n'affiche que les notifications de `user1`, de la plus récente à la plus ancienne, chacune avec son type (« CONFIRMATION » ✅, « ALERTE » ⚠️, « FRAUDE » 🚨), son message, son horodatage dd/MM/yyyy HH:mm:ss en Europe/Paris et, si elle est non lue, la pastille « Non lu » (infobulle) ; l'icône éventuelle est `aria-hidden="true"`

**Given** au moins une notification non lue
**When** le centre se charge
**Then** le badge « {N} nouvelle(s) » (N = nombre de non lues, `aria-live="polite"`) et le bouton « Tout marquer lu » sont affichés ; le clic marque toutes les notifications lues et le badge disparaît

**Given** au moins une notification
**When** l'utilisateur clique « Effacer tout »
**Then** toutes ses notifications sont supprimées, celles des autres utilisateurs restent

**Given** aucune notification
**When** le centre se charge
**Then** il affiche « Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel », sans badge, sans « Tout marquer lu » ni « Effacer tout »

**Given** l'API du service `notifications`
**When** elle est appelée sans jeton, puis par `user1` sur une notification de `user2`
**Then** les réponses sont 401, puis 403 (classe `NotificationsSecuriteIT`) ; l'utilisateur est toujours déduit du jeton

**Given** le tableau de bord complet
**When** son texte rendu est lu
**Then** « Centre de notifications » apparaît avant la section « Suivi de mes virements »

**Given** le filtre par utilisateur volontairement retiré de la lecture
**When** je lance `mvn -B -q -f services/notifications/pom.xml verify -Dgroups='FR-002-07'`
**Then** le code de sortie est non nul

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-002-02`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `mvn -B -q -f services/notifications/pom.xml verify -Dgroups='FR-002-07 | FR-002-08 | FR-025-06 | FR-025-07'` et `cd frontend && npx vitest run -t 'FR-002-07|FR-002-08|FR-025-05|FR-025-06|FR-025-07'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 3.10: Lever une alerte de fraude après des échecs de connexion répétés

En tant qu'administrateur,
je veux qu'une alerte de fraude soit levée quand un compte client subit cinq échecs de connexion consécutifs,
afin de repérer une tentative d'accès frauduleux.

**Exigences :** FR-027-11, FR-027-01, FR-027-03, AR-29, AR-31, AR-39, AR-41, AR-67
**Scénarios golden master :** aucun
**Prérequis :** A-027-2 (MULTIPLE_FAILED_AUTH évalué à la connexion au 5e échec consécutif, hors score du virement) et A-027-12 (échecs d'un compte désactivé comptés), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; story 3.2 (`FRAUD_ALERTS`) ; epic 2 (connexion, LOGIN_FAILED).

**Acceptance Criteria:**

**Given** un client existant
**When** il subit 4 échecs de connexion consécutifs
**Then** aucun événement n'est publié et aucune alerte n'est créée

**Given** un client existant avec 4 échecs consécutifs
**When** le 5e échec survient, puis un 6e
**Then** `identite-clients` publie par l'outbox un événement `identite-clients.echecs-connexion-repetes.v1` (clé `clientId` ; `clientId`, `nombreEchecs` = 5, puis 6) ; le cœur bancaire, seul propriétaire de `FRAUD_ALERTS`, crée à chaque réception une alerte OPEN MULTIPLE_FAILED_AUTH de score 80 « Customer account has {count} consecutive failed authentication attempts »
**And** `contrats/evenements/identite-clients.echecs-connexion-repetes.v1.schema.json` existe, et les tests `*ContratEvenement*Test` du producteur (identité) et du consommateur (cœur bancaire) passent ; le même message livré deux fois ne crée qu'une alerte

**Given** 4 échecs, une connexion réussie, puis 4 échecs
**When** le dernier échec survient
**Then** aucun événement n'est publié (le succès remet le compteur à zéro)

**Given** un compte désactivé avec des identifiants valides
**When** 5 tentatives consécutives échouent
**Then** l'événement est publié au 5e échec (A-027-12)

**Given** un identifiant inconnu
**When** 5 tentatives échouent
**Then** aucun compteur n'est tenu et aucun événement n'est publié

**Given** un client avec une alerte MULTIPLE_FAILED_AUTH
**When** il soumet un virement
**Then** le score du virement ne comprend pas MULTIPLE_FAILED_AUTH (huit signaux seulement)

**Given** la remise à zéro du compteur au succès volontairement retirée
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml verify -Dgroups='FR-027-11'`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml verify -Dgroups='FR-027-11'` et `mvn -B -q -f services/coeur-bancaire/pom.xml verify -Dgroups='FR-027-11'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

## Epic 4: Programmer ses virements, gérer bénéficiaires et budget, traitements de nuit

À la fin de l'epic, le client gère ses bénéficiaires externes SEPA (vérification d'IBAN, ajout, suppression logique), crée, suspend, réactive et supprime des virements programmés, et suit son budget mensuel par catégorie avec des plafonds. Le cœur bancaire exécute toutes les 5 minutes les échéances dues, chacune passant par le même contrôle de fraude que le virement immédiat (décision B2), et prévient le client d'une échéance non exécutée. Les traitements de nuit et périodiques (intérêts mensuels, fraude nocturne, relevés mensuels, soldes faibles) tournent à leurs horaires dans le fuseau Europe/Paris, sont journalisés, et l'administrateur consulte leur journal dans la section « Traitements nocturnes ».

**Contrôle de fin d'epic (à la charge de l'humain) :** lire, dans `reports/golden-master/textes-non-trouves.md`, les sections des scénarios de l'epic (SC-004-01, SC-004-02, SC-004-03, SC-006-01, SC-013-01, SC-013-02) ; lancer `scripts/golden-master/verifier-scenario.sh` pour chacun.

### Story 4.1: Consulter ses bénéficiaires externes et en supprimer un

En tant que client,
je veux voir la liste de mes bénéficiaires externes actifs, avec l'IBAN masqué et la zone SEPA, et pouvoir en supprimer un,
afin de garder à jour mes destinataires de virements SEPA sans exposer leurs coordonnées complètes.

**Exigences :** FR-013-01, FR-013-02, FR-013-03, FR-013-09, FR-013-10, NFR-SEC-03, NFR-SEC-04, NFR-SEC-05, NFR-PAR-02, NFR-UI-01, NFR-UI-02, AR-19, AR-36, AR-43, AR-46, AR-47, AR-51, AR-78, AR-79, AR-80, UX-DR17, UX-DR40, UX-DR47, UX-DR48, UX-DR78, UX-DR89, UX-DR101, UX-DR106, UX-DR130
**Scénarios golden master :** SC-013-01
**Prérequis :** A-013-1 (message de refus générique « Erreur lors de la suppression. » sur le bénéficiaire d'un autre client) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; epics 2 et 3 terminés (gabarit, navigation, route `/beneficiaires` protégée, vérification du jeton et `*SecuriteIT` du cœur bancaire, table des bénéficiaires lue par le signal NEW_BENEFICIARY, FR-027-07 — la story la crée par migration Flyway si elle n'existe pas encore) ; bénéficiaire `FR76***` de `user1` présent dans `services/coeur-bancaire/reference-data/reference-data.sql` (AR-80).

**Acceptance Criteria:**

**Given** un visiteur sans jeton
**When** il demande `/beneficiaires`
**Then** le routeur du front le redirige vers `/login`
**And** `GET /api/coeur-bancaire/beneficiaires` sans jeton répond 401 avec un corps `{"message": …}`

**Given** `user1` connecté, avec des bénéficiaires actifs et un bénéficiaire désactivé
**When** il ouvre `/beneficiaires`
**Then** l'onglet a pour titre « BankApp Demo - Beneficiaires », la section « Mes beneficiaires enregistres » est titrée « Beneficiaires ({nombre}) » et le tableau (`<table>` natif, `<th scope="col">`) a les colonnes « Libelle », « Titulaire », « IBAN », « BIC », « Banque », « Zone SEPA », « Action »
**And** seuls les bénéficiaires actifs de `user1` sont listés, triés par libellé croissant, chacun avec un bouton « Supprimer » (`data-testid` `beneficiaires-supprimer-{id}`) ; le bénéficiaire désactivé et ceux d'un autre client n'apparaissent pas

**Given** un client sans bénéficiaire actif
**When** il ouvre `/beneficiaires`
**Then** le tableau garde ses en-têtes et affiche sur toute la largeur « Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un. »

**Given** un bénéficiaire d'IBAN `FR3530004000000123456789010` sans BIC ni banque, et un bénéficiaire dont le code pays de l'IBAN est hors zone SEPA
**When** la liste s'affiche
**Then** l'IBAN du premier s'affiche « FR35***9010 » (4 premiers caractères, « *** », 4 derniers), ses colonnes BIC et Banque affichent « — » et sa zone « ✓ SEPA »
**And** le second affiche « Hors SEPA »

**Given** `user1` connecté sur `/beneficiaires`
**When** il clique « Supprimer » sur un de ses bénéficiaires
**Then** le bénéficiaire est désactivé en base (ligne conservée, indicateur actif à faux), la liste se rafraîchit sans rechargement de la page et il n'y figure plus

**Given** le client A connecté et un bénéficiaire actif du client B
**When** A appelle la suppression de ce bénéficiaire par son identifiant
**Then** la réponse est 403 avec `{"message":"Erreur lors de la suppression."}` (A-013-1)
**And** le bénéficiaire de B reste actif et visible dans la liste de B (test `@Tag("FR-013-10")` et `@Tag("BUG-047")`)

**Given** deux demandes de suppression concurrentes du même bénéficiaire actif
**When** elles s'exécutent en même temps
**Then** une seule transition actif → inactif s'applique (mise à jour conditionnelle), prouvée par un test `*TransitionConcurrente*Test` exécuté par `mvn verify`

**Given** une suppression traitée par le cœur bancaire
**When** je cherche l'IBAN `FR3530004000000123456789010` en clair dans les journaux du service pendant le test
**Then** aucune ligne ne le contient ; seule la forme masquée `FR35****9010` de l'objet-valeur `Iban` apparaît (test de référence commun du masquage)

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-013-01|FR-013-02|FR-013-03|FR-013-09|FR-013-10|BUG-047'` puis `cd frontend && npx vitest run -t 'FR-013-01|FR-013-02|FR-013-03|FR-013-09|FR-013-10'`
**Then** les deux commandes sortent en code 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-013-01`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.2: Vérifier un IBAN et ajouter un bénéficiaire

En tant que client,
je veux vérifier un IBAN avant de l'enregistrer, puis ajouter un bénéficiaire SEPA,
afin de ne garder que des coordonnées valides pour mes virements externes.

**Exigences :** FR-013-04, FR-013-05, FR-013-06, FR-013-07, FR-013-08, NFR-SEC-04, NFR-SEC-05, NFR-UI-01, NFR-UI-02, AR-34, AR-46, AR-47, AR-51, AR-78, AR-79, UX-DR22, UX-DR24, UX-DR42, UX-DR57, UX-DR63, UX-DR85, UX-DR86, UX-DR87, UX-DR100, UX-DR106, UX-DR108, UX-DR130, UX-DR143
**Scénarios golden master :** SC-013-02
**Prérequis :** story 4.1.

**Acceptance Criteria:**

**Given** `user1` sur `/beneficiaires`, section « Nouveau beneficiaire SEPA »
**When** l'écran s'affiche
**Then** le formulaire porte les champs « Libelle * » (`beneficiaires-libelle`), « Nom du titulaire * » (`beneficiaires-titulaire`), « IBAN * » (`beneficiaires-iban`), « BIC (optionnel) » (`beneficiaires-bic`), « Banque (optionnel) » (`beneficiaires-banque`), chacun relié à son `<label for>`, les obligatoires en `aria-required`, et les boutons « Verifier l'IBAN » (`beneficiaires-verifier-iban`) et « Ajouter le beneficiaire » (`beneficiaires-ajouter`)

**Given** le champ « IBAN * » rempli tour à tour avec rien, `FR3530004000000123456789011` (clé fausse), `FR3530004000000123456789010`, puis un IBAN valide d'un pays hors zone SEPA
**When** il clique « Verifier l'IBAN »
**Then** `POST /api/coeur-bancaire/beneficiaires/verification-iban` est appelé seul, sans soumettre le formulaire ni recharger la page, et le résultat s'affiche sous le champ en `aria-live="polite"` : « Veuillez saisir un IBAN. », « IBAN invalide (echec MOD-97). Verifiez la saisie. », « IBAN valide — FR (Zone SEPA) », « IBAN valide — {code pays} (Hors zone SEPA) »
**And** le front ne contient aucun calcul MOD-97 (validation au seul cœur bancaire, AR-47)

**Given** le formulaire soumis avec, tour à tour, un libellé vide, un IBAN vide, un IBAN qui échoue au MOD-97, un nom de titulaire vide, un BIC `HSBCFR` (6 caractères)
**When** il clique « Ajouter le beneficiaire »
**Then** l'ajout est refusé (400, rien d'enregistré) avec, respectivement, « Le libellé est obligatoire. », « L'IBAN est obligatoire. », « IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97). », « Le nom du titulaire est obligatoire. », « BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques. »
**And** le message est annoncé (`role="alert"`), le champ en erreur porte `aria-invalid="true"` et les valeurs saisies sont conservées

**Given** une requête d'ajout sans jeton valide
**When** elle atteint le cœur bancaire
**Then** la réponse est 401 avec `{"message":"Session expirée, veuillez vous reconnecter."}` et le front supprime `nb.jeton` et affiche `/login` (AR-34)

**Given** un libellé, un nom de titulaire ou un nom de banque de 101 caractères envoyé directement à l'API
**When** l'ajout est demandé
**Then** il est refusé en 400 sans enregistrement
**And** à l'écran ces trois champs limitent la saisie à 100 caractères (`maxlength="100"`)

**Given** un ajout valide avec IBAN ` fr35 3000 4000 0001 2345 6789 010 `, BIC ` hsbcfrppxxx `, banque `   ` (blanche)
**When** le bénéficiaire est enregistré
**Then** l'IBAN est enregistré `FR3530004000000123456789010`, le BIC `HSBCFRPPXXX`, la banque absente, la zone SEPA calculée depuis le code pays, la date de création = `aujourdhui()` du port Horloge, et le bénéficiaire est actif

**Given** le parcours de SC-013-02 (libellé « [retro] test beneficiaire », titulaire « Retro Test SA », IBAN `FR3530004000000123456789010`, vérification, BIC `HSBCFRPPXXX`, banque « HSBC France »)
**When** il clique « Ajouter le beneficiaire »
**Then** « Bénéficiaire ajouté avec succès. » s'affiche dans l'élément `data-testid="beneficiaires-message-info"` (`role="status"`), la liste est rechargée et montre « FR35***9010 », « HSBCFRPPXXX », « HSBC France », « ✓ SEPA », et le formulaire est vidé

**Given** une erreur technique simulée à l'enregistrement
**When** il clique « Ajouter le beneficiaire »
**Then** l'écran affiche « Erreur lors de l'ajout du bénéficiaire. Veuillez réessayer. » (`beneficiaire.erreur.ajout`), sans détail technique, et rien n'est enregistré

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-013-04|FR-013-05|FR-013-06|FR-013-07|FR-013-08'` puis `cd frontend && npx vitest run -t 'FR-013-04|FR-013-05|FR-013-06|FR-013-07|FR-013-08'`
**Then** les deux commandes sortent en code 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-013-02`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.3: Créer un virement programmé et voir ses virements programmés

En tant que client,
je veux créer un virement récurrent vers un compte de la banque et voir la liste de mes virements programmés,
afin d'automatiser mes versements réguliers.

**Exigences :** FR-004-01, FR-004-02, FR-004-03, FR-004-04, FR-004-05, FR-004-06, FR-004-07, FR-004-08, FR-004-13, NFR-SEC-03, NFR-SEC-04, NFR-PAR-02, NFR-UI-01, NFR-UI-02, AR-19, AR-44, AR-46, AR-49, AR-78, AR-79, AR-80, UX-DR15, UX-DR27, UX-DR47, UX-DR48, UX-DR57, UX-DR78, UX-DR100, UX-DR101, UX-DR102, UX-DR106, UX-DR109, UX-DR120, UX-DR141, UX-DR154
**Scénarios golden master :** SC-004-01, SC-004-02, SC-004-03
**Prérequis :** A-004-4 (texte « Compte destinataire introuvable » et contrôle dès la création) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; epics 2 et 3 terminés (route `/virements-programmes` protégée, comptes du cœur bancaire) ; comptes `1` de `user1` et compte interne `FR76-0001-0002` présents dans les données de référence (AR-80).

**Acceptance Criteria:**

**Given** un visiteur sans jeton
**When** il demande `/virements-programmes`
**Then** il est redirigé vers `/login`, et `GET /api/coeur-bancaire/virements-programmes` sans jeton répond 401

**Given** `user1` connecté
**When** il ouvre `/virements-programmes`
**Then** l'onglet a pour titre « BankApp Demo - Virements programmes » et la section « Nouveau virement programme » affiche les libellés « Compte debiteur », « Montant (EUR) », « Frequence », « IBAN / N compte beneficiaire », « Libelle beneficiaire », « Date de premiere execution », « Motif (optionnel) », les exemples « ex: 200.00 », « ex: FR76-0001-0002 », « ex: Compte epargne », « yyyy-MM-dd », « ex: Epargne mensuelle » et le bouton « Creer le virement programme » (`vp-creer`)
**And** le `<select>` natif « Compte debiteur » (`vp-compte-debiteur`) commence par « -- Choisir -- » puis ne propose que les comptes de `user1`, valeur = identifiant du compte, libellé « {numéro de compte} ({type de compte}) - {solde} EUR »

**Given** un CONSEILLER sans compte
**When** il ouvre `/virements-programmes`
**Then** « Compte debiteur » ne propose que « -- Choisir -- » et la liste affiche « Aucun virement programme configure »

**Given** l'écran ouvert
**When** j'inspecte le `<select>` « Frequence » (`vp-frequence`)
**Then** il propose dans cet ordre « Hebdomadaire », « Mensuel », « Bimestriel », « Trimestriel », « Semestriel », « Annuel », « Mensuel » présélectionné, valeurs d'option = noms de l'énumération legacy (dont `MENSUEL`, AR-79)

**Given** `user1` avec des virements programmés et un autre client avec les siens
**When** la section « Mes virements programmes » s'affiche
**Then** seuls ceux de `user1` sont listés, triés par prochaine exécution croissante, colonnes « Source », « Beneficiaire », « Montant », « Frequence », « Prochain » (`dd/MM/yyyy`, Europe/Paris), « Nb executions », « Etat » (« Actif » ou « Inactif »), « Actions » (« Suspendre » et « Supprimer » si actif, « Reactiver » et « Supprimer » si inactif)
**And** un client sans virement programmé voit « Aucun virement programme configure »

**Given** le formulaire soumis vide (parcours SC-004-03)
**When** il clique « Creer le virement programme »
**Then** « Veuillez selectionner un compte source » s'affiche dans `vp-message-erreur` et rien n'est créé

**Given** un compte choisi et, tour à tour, un numéro de compte bénéficiaire blanc, un libellé blanc, une date vide, une date `01/01/2027`, un montant vide, `0`, `-5`
**When** il soumet
**Then** la création est refusée avec, respectivement, « Veuillez saisir le numero de compte destinataire », « Veuillez saisir le libelle du beneficiaire », « Veuillez saisir la date de premiere execution », « Format de date invalide (attendu : yyyy-MM-dd) », « Montant invalide » (trois fois), rien n'est créé et le formulaire garde les valeurs saisies
**And** le message renvoyé et affiché est « Montant invalide » (FR-004-05), à l'écran comme sur appel direct de l'API ; aucun scénario du golden master ne montre ce cas, et FR-004-05 est l'exigence qui fixe ce message, identique pour le virement programmé, la simulation et la demande de prêt (décision du 2026-10-05)

**Given** des appels directs à `POST /api/coeur-bancaire/virements-programmes`
**When** le compte source est inexistant, ou appartient à un autre client
**Then** la création est refusée sans rien créer avec, respectivement, « Erreur : Client ou compte introuvable » et « Erreur : Compte non autorise »

**Given** un numéro de compte bénéficiaire qui n'existe pas dans la banque
**When** `user1` soumet un formulaire par ailleurs valide
**Then** la création est refusée avec « Compte destinataire introuvable » (A-004-4) et aucun virement programmé n'est créé (test `@Tag("FR-004-13")` et `@Tag("BUG-015")`)

**Given** le parcours de SC-004-02 (compte `1`, montant `50`, fréquence `MENSUEL`, bénéficiaire `FR76-0001-0002`, libellé « [retro] test virprog », date `2027-01-01`, motif « [retro] virement programm gm »)
**When** il clique « Creer le virement programme »
**Then** le virement est enregistré actif, nombre d'exécutions 0, horodatage de création lu sur le port Horloge, « Virement programme cree avec succes » s'affiche (`vp-message-succes`, `role="status"`), tous les champs sont vidés et la liste « Mes virements programmes » est rechargée avec la nouvelle ligne

**Given** une date de première exécution passée (`2020-01-01`)
**When** `user1` crée le virement
**Then** la création est acceptée et la prochaine exécution enregistrée est cette date (exécution au prochain passage, livrée par la story 4.6)

**Given** une erreur technique simulée à la création
**When** il soumet
**Then** l'écran affiche « Erreur : le virement programmé n'a pas été créé. Rien n'a été débité. Veuillez réessayer. » (`vp.erreur.creation`), sans détail technique

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-01|FR-004-02|FR-004-03|FR-004-04|FR-004-05|FR-004-06|FR-004-07|FR-004-08|FR-004-13|BUG-015'` puis `cd frontend && npx vitest run -t 'FR-004-01|FR-004-02|FR-004-03|FR-004-04|FR-004-05|FR-004-07|FR-004-08'`
**Then** les deux commandes sortent en code 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-004-01`, `scripts/golden-master/verifier-scenario.sh SC-004-02` et `scripts/golden-master/verifier-scenario.sh SC-004-03`
**Then** le code de sortie est 0 pour chacun

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.4: Suspendre, réactiver ou supprimer un virement programmé

En tant que client,
je veux suspendre, réactiver ou supprimer un de mes virements programmés,
afin d'interrompre ou d'arrêter un versement récurrent sans passer par la banque.

**Exigences :** FR-004-09, FR-004-10, NFR-SEC-03, NFR-SEC-04, NFR-PAR-02, AR-36, AR-43, AR-46, AR-78, UX-DR17, UX-DR40, UX-DR107, UX-DR141
**Scénarios golden master :** aucun
**Prérequis :** A-004-1 (message de refus générique sur le virement programmé d'un autre client) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; story 4.3.

**Acceptance Criteria:**

**Given** un virement programmé actif de `user1`
**When** il clique « Suspendre » (`vp-suspendre-{id}`)
**Then** le virement passe à l'état inactif, sans modale de confirmation, la liste est rechargée et la ligne affiche « Inactif » avec « Reactiver » (`vp-reactiver-{id}`) et « Supprimer » (`vp-supprimer-{id}`)

**Given** un virement programmé inactif de `user1`
**When** il clique « Reactiver »
**Then** le virement repasse actif et la liste rechargée affiche « Actif » avec « Suspendre » et « Supprimer »

**Given** un virement programmé de `user1`
**When** il clique « Supprimer »
**Then** le virement est supprimé définitivement (ligne absente de la base) et disparaît de la liste

**Given** le client A connecté et un virement programmé actif du client B
**When** A demande sa suspension, sa réactivation ou sa suppression par son identifiant
**Then** la demande est refusée avec `{"message":"Erreur : action impossible. Rechargez la page."}` (`vp.erreur.appartenance`), message identique à celui d'un identifiant inexistant
**And** le virement de B garde son état, sa date de prochaine exécution et son existence (test `@Tag("FR-004-10")` et `@Tag("BUG-013")`)
**And** le refus d'une action sur le virement programmé d'un autre client est un 404 strictement identique (statut et corps `{"message": "Ressource introuvable"}`) à la réponse pour un virement programmé inexistant ; l'écran affiche « Erreur : action impossible. Rechargez la page. » (`vp.erreur.appartenance`) dans les deux cas (décision du 2026-10-05, FR-004-10)

**Given** une erreur technique simulée sur « Suspendre » ou « Reactiver », puis sur « Supprimer »
**When** il clique
**Then** l'écran affiche « Erreur : l'état du virement programmé n'a pas changé. Rien n'a été débité. Veuillez réessayer. » (`vp.erreur.etat`), puis « Erreur suppression : le virement programmé n'a pas été supprimé et reste inchangé. Veuillez réessayer. » (`vp.erreur.suppression`), et le virement est inchangé

**Given** deux demandes concurrentes de suspension et de suppression du même virement actif
**When** elles s'exécutent en même temps
**Then** une seule transition s'applique (mise à jour conditionnelle sur l'état), prouvée par un test `*TransitionConcurrente*Test` exécuté par `mvn verify`

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-09|FR-004-10|BUG-013'` puis `cd frontend && npx vitest run -t 'FR-004-09|FR-004-10'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.5: Journaliser les traitements planifiés, relevés mensuels et section « Traitements nocturnes »

En tant qu'administrateur,
je veux que chaque exécution d'un traitement planifié soit journalisée et consultable dans l'administration, en commençant par les relevés mensuels,
afin de vérifier que les traitements de nuit ont tourné et avec quel résultat.

**Exigences :** FR-020-01, FR-020-02, FR-020-03, FR-020-04, FR-018-05, NFR-BATCH-01, NFR-BATCH-02, NFR-BATCH-03, AR-19, AR-44, AR-63, AR-64, AR-71, AR-78, UX-DR47, UX-DR48, UX-DR89, UX-DR132
**Scénarios golden master :** aucun
**Prérequis :** epics 2 et 3 terminés (projection des clients `identite-clients.client-modifie.v1` dans le cœur bancaire, écran `/administration` réservé ADMIN ; la table `batch_job_logs` est reprise si le rejet automatique `SUSPENDED_TRANSFERS_EXPIRY` de l'epic 3 l'a créée, sinon créée ici par migration Flyway).

**Acceptance Criteria:**

**Given** le cœur bancaire démarré hors profil `rejeu`
**When** j'inspecte ses déclencheurs
**Then** le traitement des relevés mensuels est un `@Scheduled` de `adapter.in.planification`, cron du 1er du mois à 03:00 avec la zone `Europe/Paris` explicite, qui appelle un cas d'usage

**Given** une horloge injectée au 1er du mois à 03:00 Europe/Paris et 3 clients actifs de rôle CLIENT, 1 client inactif et 1 CONSEILLER dans la projection des clients
**When** le traitement s'exécute
**Then** une ligne `MONTHLY_STATEMENTS` est écrite dans `batch_job_logs` avec début, fin, durée, 3 enregistrements, statut « SUCCESS » et détail « Releves mensuels generes pour 3 clients », et aucun document n'est généré

**Given** une erreur simulée pendant le traitement
**When** il s'exécute
**Then** une ligne `MONTHLY_STATEMENTS` est écrite avec 0 enregistrement, statut « FAILED » et détail « Erreur: {message de l'erreur} »

**Given** le service démarré le 1er du mois à 04:00 (horloge injectée), après l'heure du déclenchement
**When** la journée se déroule
**Then** aucune exécution de rattrapage n'a lieu ce mois-là, et une deuxième exécution le même jour calendaire Europe/Paris est refusée par la garde par date

**Given** le profil `rejeu` actif
**When** le contexte démarre
**Then** aucun traitement métier planifié n'est enregistré (test de configuration), le relais de l'outbox excepté

**Given** un ADMIN authentifié et des lignes de journal
**When** il appelle `GET /api/coeur-bancaire/traitements?limite=50`
**Then** la réponse 200 contient au plus 50 entrées triées par début décroissant avec job, début, fin, durée (ms), enregistrements, statut et détails

**Given** un CLIENT, puis un appel sans jeton
**When** il appelle `GET /api/coeur-bancaire/traitements?limite=50`
**Then** la réponse est 403, puis 401 (l'ouverture à tout utilisateur authentifié pour la page Produits, FR-011-09, relève de l'epic 7)

**Given** `admin` connecté sur `/administration`
**When** la section « Traitements nocturnes » s'affiche
**Then** elle porte ce titre sans suffixe technique, un tableau aux colonnes « Job », « Debut », « Fin », « Duree (ms) », « Enregistrements », « Statut », « Details », trié par début décroissant, et un bouton « Rafraichir » (`admin-traitements-rafraichir`) qui recharge la section
**And** sans aucune exécution, le tableau affiche « Aucun traitement nocturne enregistre »

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-020-01|FR-020-02|FR-020-03|FR-020-04|FR-018-05'` puis `cd frontend && npx vitest run -t 'FR-018-05'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.6: Exécuter automatiquement les échéances dues des virements programmés

En tant que client,
je veux que mes virements programmés actifs soient exécutés à chaque échéance, après le contrôle de fraude du virement immédiat,
afin que mes versements réguliers partent à la date prévue sans dériver.

**Exigences :** FR-004-07, FR-004-11, FR-004-14, FR-021-01, FR-021-02, FR-021-03, FR-021-05, FR-021-06, FR-021-07, FR-021-08, FR-021-10, FR-026-03, FR-026-05, NFR-BATCH-01, NFR-BATCH-02, NFR-BATCH-03, NFR-PAR-02, AR-38, AR-40, AR-44, AR-63, AR-64, AR-66
**Scénarios golden master :** aucun
**Prérequis :** A-021-4 (une seule exécution par passage pour un virement en retard de plusieurs périodes), A-004-5 (correction de BUG-069 appliquée à RG-004-064), A-021-7 (échéance exécutée sans notification), A-026-1 (échéances auditées au nom du titulaire, sans IP) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; stories 4.3 et 4.5 ; socle RG-027-003 et cumul glissant FR-027-18 livrés en epic 3 (cas d'usage de contrôle de fraude, machine à 5 états, `virement-a-traiter.v1`).

**Acceptance Criteria:**

**Given** le cœur bancaire hors profil `rejeu`
**When** j'inspecte ses déclencheurs
**Then** le traitement des virements programmés est un `@Scheduled` de `adapter.in.planification` toutes les 5 minutes, zone `Europe/Paris`, et chaque passage journalise « Verification virements programmes dus »

**Given** une horloge injectée à 10:00 et trois virements programmés : actif dû à 09:55, actif dû à 08:00, inactif dû à 07:00
**When** un passage s'exécute
**Then** seuls les deux actifs sont sélectionnés, traités un à un dans l'ordre 08:00 puis 09:55 ; l'inactif n'est pas exécuté et son compte source est inchangé

**Given** un virement programmé mensuel actif et dû de 100 € vers le compte interne `FR76-0001-0002`, motif « Epargne mensuelle », solde source suffisant, horloge à 10:00
**When** le passage l'exécute
**Then** l'échéance passe par le même cas d'usage de contrôle de fraude que le virement immédiat (B2), après les contrôles bénéficiaire et solde, avec l'issue EXÉCUTÉ
**And** le compte source est débité et le compte bénéficiaire crédité de 100 €, une opération DEBIT « Virement programme vers {libellé bénéficiaire} - Epargne mensuelle » et une opération CREDIT « Virement programme de {numéro du compte source} - Epargne mensuelle » sont enregistrées (sans motif : « Virement programme vers {libellé bénéficiaire} » et « Virement programme de {numéro du compte source} »), et le journal contient « Virement programme execute id={id} montant=100.00 »
**And** l'horodatage de dernière exécution est enregistré, « Nb executions » augmente de 1, et aucune notification n'est produite (aucun événement `coeur-bancaire.virement-decide.v1` porteur d'une notification, A-021-7)
**And** l'audit contient « VIREMENT » (« Montant: 100.00 - Motif: Epargne mensuelle - Issue: EXECUTE ») et « TRANSFER_EXECUTED », au nom du titulaire, sans `adresseIp`

**Given** un virement mensuel prévu le 15/01 à 08:00, exécuté le 15/01 à 08:04
**When** l'exécution réussit
**Then** la prochaine exécution est le 15/02 à 08:00, et après douze exécutions mensuelles successives l'échéance reste le 15 du mois à 08:00 (test `@Tag("FR-021-08")`, `@Tag("FR-004-14")` et `@Tag("BUG-069")`)
**And** les fréquences avancent de +7 jours (« Hebdomadaire »), +1, +2, +3, +6 mois et +1 an

**Given** un virement en retard de trois périodes
**When** un passage s'exécute
**Then** une seule échéance est exécutée, les suivantes l'étant aux passages suivants (A-021-4)

**Given** un virement créé avec une date de première exécution passée, ou dû pendant un arrêt du service
**When** le service tourne (ou redémarre)
**Then** l'échéance est exécutée au prochain passage, au plus 5 minutes après la création ou le démarrage

**Given** chaque passage du traitement
**When** il se termine
**Then** une ligne `RECURRING_TRANSFERS` est écrite dans `batch_job_logs` avec le nombre d'échéances exécutées et le statut (NFR-BATCH-02)

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-07|FR-004-11|FR-004-14|FR-021-01|FR-021-02|FR-021-03|FR-021-05|FR-021-06|FR-021-07|FR-021-08|FR-021-10|BUG-069'`
**Then** la commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.7: Traiter les échéances non exécutées et prévenir le client

En tant que client,
je veux être prévenu quand une échéance de virement programmé n'a pas pu partir, faute de solde ou parce que le compte destinataire n'existe plus,
afin de réagir au lieu de découvrir trop tard qu'un versement n'a pas eu lieu.

**Exigences :** FR-004-12, FR-004-13, FR-004-15, FR-021-04, FR-021-09, NFR-PAR-02, NFR-QG-09, AR-29, AR-31, AR-38, AR-39, AR-41, AR-65, UX-DR55
**Scénarios golden master :** aucun
**Prérequis :** A-004-2 (une seule notification par échéance non exécutée), A-004-3 (notification ALERTE de solde insuffisant à l'échéance), A-021-1 (texte de refus, compte introuvable), A-021-2 (refus « compte introuvable » avant le report pour solde insuffisant), A-021-3 (virement programmé désactivé après ce refus) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; textes décidés (`textes-nouveaux.md`, lot « Stories ») : solde insuffisant — type ALERTE, titre « Virement programmé non exécuté », message « Virement programmé vers {beneficiaire} non exécuté : solde insuffisant, rien n'a été débité » ; compte introuvable — type ALERTE, titre « Virement programmé refusé », message « Virement programmé vers {beneficiaire} refusé : compte destinataire introuvable, rien n'a été débité » ; story 4.6 ; centre de notifications du tableau de bord livré en epic 3.

**Acceptance Criteria:**

**Given** le schéma `contrats/evenements/coeur-bancaire.echeance-non-executee.v1.schema.json` (clé `virementProgrammeId`, champs `virementProgrammeId`, `notification`) avec un exemple
**When** je lance `test -f contrats/evenements/coeur-bancaire.echeance-non-executee.v1.schema.json` et les tests `*ContratEvenement*Test` du cœur bancaire (producteur) et de notifications (consommateur)
**Then** tout sort en code 0, le topic est déclaré par un bean `NewTopic` du cœur bancaire et publié par l'outbox

**Given** un virement programmé actif et dû de 500 € sur un compte source à 499,99 €
**When** un passage s'exécute
**Then** solde, opérations, état, nombre d'exécutions et date de prochaine exécution sont inchangés, le journal contient « Solde insuffisant pour virement programme id={id} », le contrôle de fraude n'est pas appelé, et exactement un événement `echeance-non-executee.v1` porteur d'une notification ALERTE est publié (test `@Tag("FR-004-12")` et `@Tag("BUG-014")`)
**And** un second passage sur la même échéance n'en publie pas d'autre (compté par le cœur bancaire dans sa base, AD-25), et le passage suivant où le solde suffit exécute l'échéance

**Given** un virement programmé actif et dû vers un numéro de compte absent de la banque, avec un solde source insuffisant
**When** un passage s'exécute
**Then** le refus « compte introuvable » est appliqué avant le contrôle du solde : solde source inchangé, aucune opération, une notification de refus publiée sur `echeance-non-executee.v1`, virement programmé passé à l'état inactif (test `@Tag("FR-021-04")`, `@Tag("BUG-068")` et `@Tag("BUG-015")`)

**Given** trois virements dus dans le même passage, dont le deuxième provoque une erreur simulée
**When** le passage s'exécute
**Then** le premier et le troisième sont exécutés (soldes, opérations, compteurs, échéances), le deuxième reste inchangé sans débit, crédit ni opération (annulation complète), et un avertissement « Echec execution virement programme id={id} » est journalisé (test `@Tag("FR-021-09")`, `@Tag("FR-004-15")` et `@Tag("BUG-070")`)

**Given** un message brut `echeance-non-executee.v1` (enveloppe JSON sans en-tête de type) livré deux fois au service notifications
**When** il est consommé
**Then** une seule notification est stockée pour le `clientId` du bloc `notification` (test Testcontainers d'idempotence), et elle apparaît dans le « Centre de notifications » du tableau de bord du client avec son titre et son texte rendus par le producteur

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-004-12|FR-004-13|FR-004-15|FR-021-04|FR-021-09|BUG-014|BUG-015|BUG-068|BUG-070'` puis `mvn -B -q -f services/notifications/pom.xml test -Dgroups='FR-004-12|FR-021-04'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.8: Suspendre une échéance suspecte et la soumettre à la revue

En tant que client et administrateur,
je veux qu'une échéance programmée suspecte soit suspendue sans débit et suive la même revue qu'un virement immédiat,
afin qu'un virement programmé ne contourne pas le contrôle de fraude (décision B2).

**Exigences :** FR-021-10, FR-004-11, FR-027-02, FR-027-04, FR-027-16, FR-027-17, FR-026-03, NFR-OBS-02, AR-43, AR-66, UX-DR112
**Scénarios golden master :** aucun
**Prérequis :** A-021-6 (pendant la suspension, la prochaine date avance, le compteur ne change qu'après libération, échéance rejetée non rattrapée), A-021-7 (seule une échéance suspendue notifie ; une échéance refusée pour erreur du contrôle est retentée au passage suivant, sans notification) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; stories 4.6 et 4.7 ; file de revue de l'administrateur, rejet automatique et cumul glissant livrés en epic 3.

**Acceptance Criteria:**

**Given** une échéance programmée mensuelle de 15 000 € due à 10:00 (horloge injectée), solde source suffisant
**When** le passage s'exécute
**Then** un virement à l'état SUSPENDU est enregistré sans débit (solde source inchangé), avec une heure butoir = instant de l'échéance + 24 h
**And** une seule notification FRAUDE est publiée sur `coeur-bancaire.virement-decide.v1`, au texte de A-027-4 portant l'heure butoir, sans mention de fraude ni de signal
**And** la date de prochaine exécution avance d'une période, le nombre d'exécutions ne change pas, et le virement programmé reste « Actif » dans `/virements-programmes`
**And** le virement apparaît dans la file « Virements suspendus — En attente de revue » de l'administrateur

**Given** cette échéance SUSPENDU libérée par l'administrateur avant l'heure butoir
**When** le consommateur de `coeur-bancaire.virement-a-traiter.v1` la traite
**Then** elle passe EXÉCUTÉ : débit, crédit, opérations DEBIT et CREDIT aux libellés de FR-021-05 et FR-021-06, nombre d'exécutions +1 et horodatage de dernière exécution enregistré, sans notification

**Given** une échéance SUSPENDU non traitée
**When** l'heure butoir est atteinte
**Then** le rejet automatique la passe REJETÉ sans débit, avec l'audit « TRANSFER_AUTO_REJECT » ; elle n'est pas rattrapée (nombre d'exécutions inchangé, prochaine date déjà avancée)

**Given** des échéances dont l'issue serait suspendue pour un score cumulé > 75, et pour un dépassement du cumul glissant de 10 000 € sur 24 h
**When** le passage s'exécute
**Then** chacune est SUSPENDU sans débit, comme le virement immédiat

**Given** une erreur du contrôle provoquée (`nb.fraude.acces-historique.delai=1ms` ou verrou tenu sur l'historique)
**When** le passage traite une échéance due
**Then** rien n'est débité, aucune notification n'est publiée, l'erreur est journalisée, un virement REFUSÉ_ERREUR_CONTRÔLE est persisté, la date de prochaine exécution ne change pas, et l'échéance est retentée au passage suivant
**And** l'observation de la tâche planifiée porte l'attribut `virement.controle.issue` à `REFUSE_ERREUR_CONTROLE` et un statut d'erreur, sans span créé à la main

**Given** une échéance de 100 € de score faible
**When** le passage s'exécute
**Then** elle est exécutée sans notification

**Given** deux passages concurrents sur la même échéance due
**When** ils s'exécutent en même temps
**Then** un seul virement est créé et un seul débit a lieu, prouvé par un test `*TransitionConcurrente*Test` exécuté par `mvn verify`

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-021-10|FR-004-11'`
**Then** la commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.9: Créditer les intérêts mensuels des comptes épargne

En tant que client titulaire d'un compte épargne,
je veux recevoir mes intérêts une fois par mois,
afin que mon épargne soit rémunérée au bon taux, sans le crédit quotidien erroné du legacy.

**Exigences :** FR-018-01, FR-018-02, FR-018-03, FR-018-04, NFR-BATCH-01, NFR-BATCH-02, NFR-BATCH-03, NFR-PAR-02, AR-44, AR-49, AR-63, AR-64
**Scénarios golden master :** aucun
**Prérequis :** A-018-1 (crédit à l'exécution de 2 h du 1er du mois, rien les autres nuits), A-018-2 (pas de second crédit dans le même mois) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; story 4.5.

**Acceptance Criteria:**

**Given** le cœur bancaire hors profil `rejeu`
**When** j'inspecte ses déclencheurs
**Then** `INTEREST_CALCULATION` est un `@Scheduled` quotidien à 02:00, zone `Europe/Paris`

**Given** un compte EPARGNE de 12 000,00 EUR et une horloge injectée qui simule toutes les exécutions nocturnes d'un mois civil complet
**When** les exécutions se succèdent
**Then** le compte a reçu exactement une opération CREDIT « Interets mensuels livret epargne (batch nocturne) » de 5,00 EUR, datée de l'exécution de 02:00 du 1er, avec le solde résultant, et son solde vaut 12 005,00 EUR (test `@Tag("FR-018-01")` et `@Tag("BUG-064")`)

**Given** le traitement relancé dans un mois déjà crédité
**When** il s'exécute
**Then** aucun compte n'est crédité une seconde fois (garde « mois déjà crédité » dans le domaine)

**Given** des comptes EPARGNE à 1 000,00 EUR et à 0,00 EUR le 1er du mois
**When** le traitement s'exécute
**Then** le premier reçoit 0,42 EUR (0,4166… arrondi à 2 décimales, moitié vers le haut), le second est ignoré (solde inchangé, aucune opération), et l'entrée de journal compte les deux comptes évalués : statut « SUCCESS », détail « Interets calcules sur 2 comptes epargne »

**Given** une nuit autre que le 1er du mois
**When** le traitement s'exécute
**Then** aucun compte n'est crédité et une entrée « SUCCESS » à 0 enregistrement est écrite

**Given** une erreur simulée pendant le traitement
**When** il s'exécute
**Then** l'entrée « INTEREST_CALCULATION » est écrite avec le statut « FAILED » et le détail « Erreur: {message} », l'erreur est journalisée au niveau ERROR, et aucun solde n'est modifié

**Given** les jours de changement d'heure (heure d'été et d'hiver)
**When** les exécutions de la nuit sont simulées
**Then** le traitement s'exécute une seule fois par jour calendaire Europe/Paris

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-018-01|FR-018-02|FR-018-03|FR-018-04|BUG-064'`
**Then** la commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.10: Compter chaque nuit les transactions importantes

En tant qu'administrateur,
je veux un compte rendu quotidien des transactions de 10 000 € ou plus des dernières 24 heures,
afin de surveiller les gros mouvements sans créer d'alerte individuelle.

**Exigences :** FR-019-01, FR-019-02, FR-019-03, FR-019-04, FR-019-05, FR-019-06, NFR-BATCH-01, NFR-BATCH-02, NFR-BATCH-03, NFR-PAR-02, AR-44, AR-63, AR-64
**Scénarios golden master :** aucun
**Prérequis :** story 4.5.

**Acceptance Criteria:**

**Given** le cœur bancaire hors profil `rejeu`
**When** j'inspecte ses déclencheurs
**Then** `FRAUD_DETECTION` est un `@Scheduled` quotidien à 02:30, zone `Europe/Paris`

**Given** le service démarré à 03:00 (horloge injectée), après l'heure du déclenchement
**When** la journée se déroule
**Then** le traitement n'est pas relancé pour ce jour, et aux jours de changement d'heure il s'exécute une seule fois par jour calendaire

**Given** des transactions des dernières 24 heures de 10 000,00 € (débit), 10 000,00 € (crédit), 9 999,99 € et 15 000,00 €
**When** le traitement s'exécute
**Then** 3 transactions sont retenues (borne de 10 000 € incluse, débits et crédits confondus), une seule entrée « FRAUD_DETECTION » est écrite avec 3 enregistrements, statut « SUCCESS » et détail « Transactions suspectes detectees: 3 transaction(s) > 10000 EUR »

**Given** une exécution le jour du passage à l'heure d'été, puis le jour du passage à l'heure d'hiver, avec une transaction de 10 000 € datée de 24 h 30 avant l'exécution et une datée de 23 h 30 avant
**When** le traitement s'exécute
**Then** la fenêtre couvre exactement 24 heures écoulées : la première n'est pas comptée, la seconde l'est (test `@Tag("FR-019-03")` et `@Tag("BUG-066")`)

**Given** le traitement exécuté avec des transactions retenues
**When** je compte les alertes de fraude
**Then** aucune alerte n'a été créée

**Given** une erreur simulée pendant le traitement
**When** il s'exécute
**Then** l'erreur est interceptée et journalisée, l'entrée est écrite avec « FAILED » et « Erreur: {message} », l'erreur n'est pas propagée, et l'exécution du lendemain a lieu normalement

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-019-01|FR-019-02|FR-019-03|FR-019-04|FR-019-05|FR-019-06|BUG-066'`
**Then** la commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.11: Alerter le client d'un solde faible

En tant que client,
je veux être alerté dans mon centre de notifications quand un de mes comptes passe sous 100 €, sans être submergé,
afin d'approvisionner mon compte à temps.

**Exigences :** FR-022-01, FR-022-02, FR-022-03, FR-022-04, FR-022-05, FR-022-06, FR-025-08, NFR-BATCH-01, NFR-BATCH-02, NFR-BATCH-03, NFR-PAR-02, NFR-QG-09, AR-29, AR-31, AR-38, AR-39, AR-41, AR-44, AR-63, AR-64, AR-65, UX-DR55, UX-DR91, UX-DR115, UX-DR149
**Scénarios golden master :** aucun
**Prérequis :** A-022-1 (plafond de 20 par jour calendaire, heure de Paris), A-025-1 (type ACCOUNT_BALANCE, priorité HIGH, icône ⚠️, comptée dans le plafond par utilisateur) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; story 4.5 ; centre de notifications et plafond par utilisateur livrés en epic 3 ; RP-01 (saturation du journal par 48 entrées par jour) reste ouvert jusqu'à la story de FR-016-17 (epic 9).

**Acceptance Criteria:**

**Given** le schéma `contrats/evenements/coeur-bancaire.solde-faible-detecte.v1.schema.json` (clé `compteId`, champs `compteId`, `notification`) avec un exemple
**When** je lance `test -f contrats/evenements/coeur-bancaire.solde-faible-detecte.v1.schema.json` et les tests `*ContratEvenement*Test` du cœur bancaire (producteur) et de notifications (consommateur)
**Then** tout sort en code 0, le topic est déclaré par un bean `NewTopic` du cœur bancaire et publié par l'outbox

**Given** le cœur bancaire hors profil `rejeu`
**When** j'inspecte ses déclencheurs
**Then** `NOTIFICATION_CHECK` est un `@Scheduled` toutes les 30 minutes, zone `Europe/Paris`

**Given** un client actif avec un compte COURANT à 99,99 €, un EPARGNE à 100,00 € et un compte sans solde, et un client inactif avec un compte à 10 €
**When** l'analyse s'exécute
**Then** un seul événement `solde-faible-detecte.v1` est publié, pour le compte à 99,99 € ; le compte à 100,00 €, le compte sans solde et le client inactif n'en produisent pas

**Given** un compte à 50,00 €
**When** l'événement est publié
**Then** son bloc `notification` porte le `clientId` du titulaire, le type ACCOUNT_BALANCE, la priorité HIGH, le titre « Solde insuffisant sur votre compte » et le texte « Votre solde (50.00 €) est passé sous le seuil d'alerte de 100 €. Veuillez approvisionner votre compte. »

**Given** un compte resté à 50 € pendant 48 exécutions d'une même journée (horloge injectée)
**When** les exécutions se succèdent
**Then** exactement 20 événements ACCOUNT_BALANCE sont publiés pour ce compte ce jour-là (compte tenu par le cœur bancaire dans sa base), et la première exécution du jour suivant en publie un nouveau (test `@Tag("FR-022-04")`, `@Tag("FR-025-08")` et `@Tag("BUG-071")`)
**And** le jour est le jour calendaire Europe/Paris : une exécution à 23:30 et une à 00:30 heure de Paris comptent sur deux jours différents

**Given** la liste des clients actifs impossible à charger
**When** l'analyse s'exécute
**Then** toute l'exécution est interrompue, l'erreur est journalisée, et une entrée « NOTIFICATION_CHECK » au statut « FAILED » avec le détail de l'erreur est écrite dans `batch_job_logs` (test `@Tag("FR-022-06")` et `@Tag("BUG-072")`)

**Given** un client dont les comptes ne se chargent pas, puis un compte dont la création de la notification échoue
**When** l'analyse s'exécute
**Then** ce client, puis ce compte, sont ignorés avec un avertissement, et l'analyse continue avec les suivants

**Given** une exécution sans erreur
**When** elle se termine
**Then** une nouvelle entrée « NOTIFICATION_CHECK » est écrite avec début, fin, durée, nombre de notifications créées et statut « SUCCESS »

**Given** un message brut `solde-faible-detecte.v1` livré deux fois au service notifications
**When** il est consommé
**Then** une seule notification est stockée (test Testcontainers d'idempotence) et elle compte dans le plafond par utilisateur `nb.notifications.plafond=20`

**Given** `user1` avec une notification ACCOUNT_BALANCE
**When** il ouvre `/tableau-de-bord`
**Then** le « Centre de notifications » l'affiche avec l'icône ⚠️ (icône d'accompagnement en `aria-hidden`), le titre « Solde insuffisant sur votre compte », le texte rendu et l'horodatage `dd/MM/yyyy HH:mm:ss` (test Vitest `[FR-025-08]` et `[BUG-079]`)

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-022-01|FR-022-02|FR-022-03|FR-022-04|FR-022-05|FR-022-06|FR-025-08|BUG-071|BUG-072'`, `mvn -B -q -f services/notifications/pom.xml test -Dgroups='FR-025-08'` puis `cd frontend && npx vitest run -t 'FR-025-08|BUG-079'`
**Then** les trois commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.12: Consulter ses dépenses du mois par catégorie

En tant que client,
je veux voir mes dépenses et revenus d'un mois et leur répartition par catégorie,
afin de comprendre où va mon argent.

**Exigences :** FR-006-01, FR-006-02, FR-006-03, FR-006-04, NFR-SEC-03, NFR-UI-01, NFR-UI-02, AR-19, AR-44, AR-45, AR-49, AR-78, AR-79, UX-DR27, UX-DR39, UX-DR56, UX-DR78, UX-DR101, UX-DR108, UX-DR122, UX-DR154, UX-DR156
**Scénarios golden master :** SC-006-01
**Prérequis :** mots-clés des catégories proposés dans `textes-nouveaux.md`, lot « Stories » (clés `budget.motscles.*`), confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; epics 2 et 3 terminés (route `/budget`, opérations du cœur bancaire).

**Acceptance Criteria:**

**Given** `user1` connecté
**When** il ouvre `/budget`
**Then** l'onglet a pour titre « BankApp Demo - Budget » et la page affiche les quatre titres de section legacy, rendus en capitales par CSS (« TOTAL DEPENSES », « TOTAL REVENUS », « REPARTITION PAR CATEGORIE », « PLAFONDS BUDGETAIRES »), sans bandeau technique

**Given** `user1` avec des opérations sur 30 mois distincts
**When** la page s'ouvre
**Then** le `<select>` natif « Periode : » propose au format AAAA-MM les 24 mois les plus récents ayant au moins une opération, et le mois le plus récent avec opérations est sélectionné

**Given** un utilisateur sans aucune opération et un service dont l'horloge est au 2026-11-17
**When** il ouvre `/budget`
**Then** la période sélectionnée est `2026-11`, fournie par le service dans le champ `aujourdhui` (aucune lecture de l'horloge du navigateur)

**Given** la page ouverte
**When** il change la valeur du sélecteur de période, puis clique « Afficher »
**Then** les données sont rechargées pour la nouvelle période dès le changement, sans validation, et à nouveau au clic

**Given** un mois avec des débits dont les libellés correspondent à des mots-clés de catégories (casse différente), un débit sans mot-clé reconnu et des crédits
**When** la section « Repartition par categorie » s'affiche
**Then** elle est titrée « Depenses du mois {AAAA-MM} », ne ventile que les débits, rattache chaque débit à la première catégorie dont un mot-clé apparaît dans son libellé (insensible à la casse), le débit sans mot-clé à « Autres », et n'affiche que les catégories de montant non nul

**Given** un mois sans débit
**When** la section s'affiche
**Then** elle affiche « Aucune depense enregistree sur cette periode »

**Given** un mois avec 1 234,50 € de débits, 2 000,00 € de crédits et 2 plafonds
**When** les indicateurs s'affichent
**Then** « Total depenses » vaut 1234.50 EUR, « Total revenus » 2000.00 EUR (deux décimales, format du legacy de l'écran) et « Plafonds definis » 2

**Given** le client A connecté et des opérations et plafonds du client B sur le même mois
**When** A appelle l'API du budget, y compris avec un identifiant de client ou de compte de B en paramètre
**Then** aucune opération ni plafond de B n'est compté ni renvoyé (le client est déduit du jeton, `clientId = sub`)

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-006-01|FR-006-02|FR-006-03|FR-006-04'` puis `cd frontend && npx vitest run -t 'FR-006-01|FR-006-02|FR-006-03|FR-006-04'`
**Then** les deux commandes sortent en code 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-006-01`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 4.13: Définir et suivre des plafonds de dépenses par catégorie

En tant que client,
je veux fixer un plafond mensuel par catégorie de dépenses et voir ma consommation et mes dépassements,
afin de tenir mon budget.

**Exigences :** FR-006-05, FR-006-06, FR-006-07, FR-006-08, FR-006-09, FR-006-10, FR-006-11, NFR-F006, NFR-SEC-04, NFR-PAR-02, NFR-UI-01, NFR-UI-02, AR-49, AR-78, UX-DR17, UX-DR27, UX-DR42, UX-DR47, UX-DR57, UX-DR66, UX-DR90, UX-DR107, UX-DR122
**Scénarios golden master :** aucun
**Prérequis :** A-006-1 (message générique « Une erreur est survenue. Veuillez reessayer. » à la place des erreurs techniques du plafond) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; story 4.12.

**Acceptance Criteria:**

**Given** `user1` sur `/budget`
**When** le formulaire « Definir un plafond » s'affiche
**Then** il porte le `<select>` « Categorie » (`budget-categorie`) avec « -- Choisir -- » puis Logement, Alimentation, Transport, Sante, Loisirs, Abonnements, Epargne, Assurances, Autres, sans « Revenus », le champ « Plafond mensuel (EUR) » (`budget-plafond`, exemple « ex: 500 ») et le bouton « Enregistrer le plafond » (`budget-enregistrer`)

**Given** un plafond pour la catégorie Revenus envoyé directement à l'API
**When** l'enregistrement est demandé
**Then** il est refusé en 400 et aucun plafond n'est créé (test `@Tag("FR-006-06")` et `@Tag("BUG-023")`)

**Given** le formulaire soumis sans catégorie, puis avec un plafond vide, `0`, `-10`, puis `abc`
**When** il clique « Enregistrer le plafond »
**Then** l'enregistrement est refusé avec « Veuillez selectionner une categorie », puis « Veuillez saisir un plafond valide » (vide, nul, négatif), et la saisie non numérique est refusée

**Given** un plafond de 14 chiffres entiers
**When** l'enregistrement est demandé
**Then** il est refusé par une validation explicite en 400, jamais par une erreur technique ni un 500 avec le message « Le plafond ne doit pas dépasser 13 chiffres avant la virgule » (clé `budget.erreur.plafond.max`, `textes-nouveaux.md`, lot « Stories »)

**Given** aucun plafond Alimentation pour le mois sélectionné
**When** il enregistre 500 pour Alimentation, puis 300 pour Alimentation
**Then** un seul plafond (client, Alimentation, mois) existe, de valeur 300, chaque succès affiche « Plafond enregistre pour Alimentation », vide les deux champs et rafraîchit la liste des plafonds

**Given** une ligne du tableau des plafonds
**When** il clique « Supprimer » (`budget-supprimer-{id}`)
**Then** le plafond est supprimé immédiatement, sans confirmation

**Given** une erreur technique simulée à l'enregistrement puis à la suppression
**When** il agit
**Then** l'écran affiche « Une erreur est survenue. Veuillez reessayer. », sans détail technique (ni « Erreur : {message technique} » ni « Erreur suppression : {message technique} »)

**Given** un plafond de 500 € et des dépenses du mois de 399 €, 400 €, 500 €, 500,01 €, et une catégorie dépensée sans plafond
**When** la répartition s'affiche
**Then** la barre (`role="progressbar"`, `aria-valuenow`) et le libellé « {pourcentage}% du plafond » valent 79 % (vert), 80 % (orange), 100 % (rouge, sans badge) et 100 % (rouge, remplissage plafonné à 100 %) ; à 500,01 € la ligne porte le badge texte « Depassement », un fond rose et le montant dépensé en rouge ; la catégorie sans plafond affiche « Aucun plafond defini »
**And** les couleurs n'utilisent que les tokens du thème (décision du 2026-10-05) : barre verte → `success`, orange → `warning`, rouge → `error`, fond rose de la ligne en dépassement → `error-container`, montant dépensé en rouge → `error`

**Given** au moins un plafond pour le mois sélectionné, l'un dépassé et l'autre non
**When** la page s'affiche
**Then** le tableau « Plafonds du mois {AAAA-MM} » a les colonnes « Categorie », « Plafond », « Depense », « Ecart », « Action », l'écart (plafond − dépenses) en rouge s'il est négatif, en vert s'il est positif ou nul
**And** sans plafond pour le mois, ce tableau n'est pas affiché

**Given** la story terminée
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-006-05|FR-006-06|FR-006-07|FR-006-08|FR-006-09|FR-006-10|FR-006-11|BUG-023'` puis `cd frontend && npx vitest run -t 'FR-006-05|FR-006-06|FR-006-07|FR-006-08|FR-006-09|FR-006-10|FR-006-11|BUG-023'`
**Then** les deux commandes sortent en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

## Epic 5: Demander et suivre un prêt

À la fin de l'epic, tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) simule un emprunt, dépose une demande de prêt traitée en arrière-plan par le scoring crédit, reçoit la décision dans son centre de notifications, consulte l'échéancier de ses prêts, marque une échéance payée et supprime un prêt. L'administrateur voit le pipeline des prêts en attente, les approuve ou les refuse (actions auditées) et consulte l'historique de tous les prêts. Le service `credits` existe avec sa base, son topic interne `credits.pret-demande.v1`, son topic `credits.pret-decide.v1` consommé par `notifications` et sa projection des clients.

**Contrôle de fin d'epic (à la charge de l'humain) :** lire, dans `reports/golden-master/textes-non-trouves.md`, les sections des scénarios de l'epic (SC-007-01, SC-007-02) ; lancer `scripts/golden-master/verifier-scenario.sh` pour chacun (`scripts/golden-master/verifier-scenario.sh SC-007-01`, `scripts/golden-master/verifier-scenario.sh SC-007-02`).

### Story 5.1: Ouvrir la page Prêts et simuler un emprunt

En tant qu'utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN),
je veux ouvrir la page Prêts, simuler un emprunt et voir la liste de mes emprunts,
afin de connaître la mensualité et le coût d'un prêt avant de le demander.

**Exigences :** FR-007-01, FR-007-02, FR-007-03, FR-007-04, FR-007-08, FR-004-05 (contexte simulation), NFR-F007 (simulation), NFR-SEC-03, NFR-SEC-04, NFR-UI-01, NFR-UI-02, NFR-PAR-04, AR-17, AR-20, AR-26, AR-36, AR-37, AR-44, AR-45, AR-46, AR-47, AR-49, AR-57, AR-76, AR-78, AR-79, AR-87, UX-DR123, UX-DR60, UX-DR22, UX-DR27, UX-DR39, UX-DR40, UX-DR47, UX-DR48, UX-DR54, UX-DR57, UX-DR78, UX-DR142, UX-DR156, UX-DR157
**Scénarios golden master :** SC-007-01, SC-007-02
**Prérequis :** A-007-1 (durée de 301 à 360 mois refusée avec « Duree invalide ») — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; epic 2 terminé (vérification du jeton, format d'erreur `{"message"}`, gabarit, navigation, garde des routes du front, `scripts/golden-master/verifier-scenario.sh`) ; story de l'epic 4 qui livre FR-004-05 (message « Montant invalide »).

**Acceptance Criteria:**

**Given** le dépôt après la story
**When** je liste `services/`
**Then** le module `services/credits/` existe (pom.xml déclaré dans le POM parent, Dockerfile, `reference-data/reference-data.sql`, paquets `fr.nextbanking.credits.{domain,application,adapter.in,adapter.out,infrastructure}`), avec sa base `postgres-credits` (utilisateur, mot de passe et volume `pgdata-credits` propres) et son service `credits` dans `compose.yaml` (port de développement 8083, `healthcheck`, `depends_on: condition: service_healthy`)
**And** le conteneur web et le proxy Vite renvoient `/api/credits/**` vers `credits`, chemin transmis tel quel
**And** les classes `HexagonalArchitectureTest`, `HorlogeArchitectureTest` et `ClientsHttpArchitectureTest` existent dans `services/credits/src/test` et passent

**Given** le service `credits` démarré
**When** j'appelle `GET /api/credits/prets` sans jeton
**Then** la réponse est 401 (pas de redirection serveur)
**And** la classe `CreditsSecuriteIT` prouve : sans jeton → 401 ; ressource d'un autre client → 403 avec `{"message"}` ; ressource inexistante → 404 avec `{"message"}`

**Given** un utilisateur non authentifié
**When** il ouvre `/prets`
**Then** le routeur du front l'envoie sur `/login`
**And** connecté en CLIENT, CONSEILLER ou ADMIN, il voit la même page avec les séparateurs « Simulateur de pret », « Demande de pret », « Mes emprunts » rendus en capitales par CSS (texte source dans sa casse), et le titre d'onglet « BankApp Demo - Prets »

**Given** la page `/prets` ouverte
**When** je regarde le simulateur « Simuler un emprunt »
**Then** les champs « Montant (EUR) » (`pret-sim-montant`), « Taux annuel (%) » (`pret-sim-taux`), « Duree (mois) » (`pret-sim-duree`) sont préremplis à 10000, 3.50 et 36 et le bouton « Calculer » porte `pret-sim-calculer`
**And** chaque champ a un `<label for>` relié

**Given** le simulateur rempli avec 10000, 3.5 et 36
**When** je clique « Calculer »
**Then** le service `credits` calcule la simulation (`POST /api/credits/simulations`) et la page affiche « Mensualite » 293.02 EUR, « Cout total » 10,548.72 EUR, « Total interets » 548.72 EUR (étiquettes rendues en capitales par CSS, montants au format du legacy)
**And** le lien « Voir l echeancier complet (36 echeances) » (`aria-expanded`, `aria-controls`) déplie un tableau de 36 lignes aux colonnes « # », « Capital », « Interets », « Total », « Capital restant »
**And** avec un taux de 0, la mensualité vaut le montant divisé par la durée

**Given** le simulateur
**When** je soumets un taux de -1 ou 30.01, puis une durée de 0 ou 361, puis un montant vide ou 0
**Then** le service répond 400 et la page affiche respectivement « Taux invalide (0-30%) », « Duree invalide (1-360 mois) » et « Montant invalide », sans résultat de simulation
**And** un taux de 30 et une durée de 360 sont acceptés

**Given** une erreur inattendue provoquée pendant la simulation (port de calcul en échec dans le test)
**When** je clique « Calculer »
**Then** la réponse est un 5xx dont le `message` vaut « Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer. » (`pret.erreur.simulation`), affiché en `alert-error` dans `pret-message-erreur`, sans texte d'exception, pile ni nom de composant

**Given** la page `/prets`
**When** je regarde le formulaire « Nouveau pret »
**Then** le champ « Type de pret » est un `<select>` natif proposant dans l'ordre « Immobilier », « Automobile », « Consommation », « Travaux », « Etudiant », « Consommation » sélectionné par défaut, son libellé étant rendu « TYPE DE PRET » par CSS
**And** les champs « Montant (EUR) », « Taux annuel (%) », « Duree (mois) », « Objet du pret » et le bouton « Soumettre la demande » sont affichés (la soumission est livrée par la story 5.2)

**Given** les données de référence de `credits` chargées (`services/credits/reference-data/reference-data.sql` : lignes `LOANS` et `LOAN_REPAYMENTS` d'`import.sql` avec les identifiants du legacy) et `user1` connecté
**When** j'ouvre `/prets`
**Then** « Emprunts en cours » liste seulement les prêts de `user1`, triés par date de début décroissante, avec les colonnes « Type », « Montant », « Taux », « Duree », « Mensualite », « Total interets », « Statut », « Echeancier », « Action » et les libellés de statut « En attente d approbation », « En cours », « Rembourse », « Refuse par le service credit », « En defaut » en `tag`
**And** un prêt EN_ATTENTE affiche « En cours de traitement... » dans « Echeancier »
**And** un utilisateur sans prêt voit la ligne « Aucun emprunt enregistre » sous les en-têtes conservés

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-007-01`
**Then** le code de sortie est 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-007-02`
**Then** le code de sortie est 0

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-01|FR-007-02|FR-007-03|FR-007-08|FR-004-05'` puis `mvn -B -q -f services/credits/pom.xml verify`
**Then** les deux commandes sortent en code 0, chaque tag cité est porté par au moins un test exécuté, et `CreditsSecuriteIT` est exécutée

**Given** les tests de la story
**When** je lance `cd frontend && npx vitest run -t 'FR-007-01'`, puis avec `-t 'FR-007-02'`, `-t 'FR-007-03'`, `-t 'FR-007-04'`, `-t 'FR-007-08'`
**Then** chaque commande sort en code 0 avec au moins un test exécuté (dont le formateur de `src/domain` : 10548.72 → « 10,548.72 EUR »)

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 5.2: Déposer une demande de prêt et la transmettre au scoring

En tant qu'utilisateur authentifié,
je veux soumettre une demande de prêt valide et savoir qu'elle a été transmise au service crédit,
afin d'obtenir une décision sans attendre devant l'écran.

**Exigences :** FR-007-05, FR-007-06, FR-007-07, FR-024-01, FR-024-02, FR-004-05 (contexte demande de prêt), NFR-F007 (demande), NFR-PAR-02, NFR-SEC-04, AR-30, AR-31, AR-38, AR-39, AR-44, AR-46, AR-49, AR-90, AR-97, UX-DR15, UX-DR57, UX-DR100, UX-DR102, UX-DR123, UX-DR142
**Scénarios golden master :** aucun
**Prérequis :** A-007-1 (durée de 301 à 360 mois refusée avec « Duree invalide »), A-024-1 (texte « Service credit temporairement indisponible, veuillez reessayer plus tard ») et A-024-2 (demande non transmise annulée, absente de la liste) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; champs de `credits.pret-demande.v1` inscrits au catalogue AD-22 de la spine avant la story (AR-31 ; proposition : `pretId`) ; story 5.1.

**Acceptance Criteria:**

**Given** le formulaire « Nouveau pret » rempli avec « Consommation », 10000, 3.5, 300 mois et l'objet « Voiture »
**When** je clique « Soumettre la demande » (`pret-soumettre`, désactivé avec `aria-busy` pendant l'envoi)
**Then** `POST /api/credits/prets` crée dans la même transaction un prêt EN_ATTENTE (identifiant attribué et renvoyé dans la réponse) avec mensualité, coût total et total des intérêts calculés comme la simulation, date de début au 1er du mois en cours (port Horloge, Europe/Paris) et date de fin = date de début + 300 mois civils, et une ligne `outbox` du topic `credits.pret-demande.v1` (clé `pretId`)
**And** la page affiche « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) » dans `pret-message-info` et rafraîchit la liste, où le prêt apparaît « En attente d approbation » avec « En cours de traitement... »

**Given** le topic `credits.pret-demande.v1`
**When** je regarde le dépôt
**Then** `contrats/evenements/credits.pret-demande.v1.schema.json` existe avec un exemple, le bean `NewTopic` est déclaré dans `infrastructure` de `credits` (3 partitions), et un test `PretDemandeContratEvenementProducteurTest` vérifie les champs exacts de l'enveloppe (`idEvenement`, `type`, `version`, `horodatage` lu sur le port Horloge, `producteur`, `donnees`) sans en-tête `__TypeId__`

**Given** une demande valide en tout point sauf la durée
**When** je soumets 301 mois, puis 360 mois
**Then** la réponse est 400 avec `{"message":"Duree invalide"}`, la page affiche « Duree invalide », aucune ligne n'est ajoutée à `LOANS` ni à `outbox` et aucune ligne n'apparaît dans « Emprunts en cours » (test `@Tag("FR-007-06")` et `@Tag("BUG-025")`)
**And** la même demande avec 300 mois est acceptée et sa ligne `outbox` est écrite

**Given** le formulaire « Nouveau pret »
**When** je soumets un taux de -1 ou 31, une durée de 0 ou 361, un objet vide ou composé d'espaces, un montant vide ou 0
**Then** la réponse est 400 et la page affiche respectivement « Taux invalide », « Duree invalide », « Veuillez indiquer un motif », « Montant invalide », les valeurs saisies sont conservées et aucun prêt n'est créé

**Given** l'écriture de la ligne `outbox` de `credits.pret-demande.v1` en échec (adaptateur de persistance de l'outbox en panne dans le test)
**When** je soumets une demande valide
**Then** la transaction est annulée : aucun prêt EN_ATTENTE n'existe, la réponse est un 5xx avec `{"message":"Service credit temporairement indisponible, veuillez reessayer plus tard"}`, la page l'affiche dans `pret-message-erreur` et n'affiche pas « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) » (test `@Tag("FR-024-02")` et `@Tag("BUG-077")`)

**Given** une erreur inattendue pendant l'enregistrement de la demande (hors échec de transmission)
**When** je soumets une demande valide
**Then** le `message` affiché vaut « Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer. » (`pret.erreur.demande`), sans détail technique

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-05|FR-007-06|FR-007-07|FR-024-01|FR-024-02|BUG-025|BUG-077|FR-004-05'`
**Then** le code de sortie est 0 et chaque tag cité est porté par au moins un test exécuté

**Given** les tests de la story
**When** je lance `cd frontend && npx vitest run -t 'FR-007-05'`, puis avec `-t 'FR-007-07'` et `-t 'FR-024-02'`
**Then** chaque commande sort en code 0 avec au moins un test exécuté

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 5.3: Approuver ou refuser automatiquement une demande par le scoring crédit

En tant que client ayant déposé une demande de prêt,
je veux que le service crédit l'approuve ou la refuse selon les règles de scoring,
afin d'obtenir un prêt en cours avec son échéancier, ou un refus clair.

**Exigences :** FR-007-09, FR-007-10 (transitions et garde), FR-024-03, FR-024-04, FR-024-05, FR-024-06 (statut et échéancier), FR-024-07 (statut), FR-024-08, NFR-PAR-02, AR-29, AR-30, AR-31, AR-38, AR-39, AR-41, AR-43, AR-65, AR-90
**Scénarios golden master :** aucun
**Prérequis :** story 5.2 (producteur de `credits.pret-demande.v1`).

**Acceptance Criteria:**

**Given** un prêt EN_ATTENTE publié sur `credits.pret-demande.v1`
**When** le consommateur de `credits` (groupe `credits`, écouteur unitaire) traite le message
**Then** le scoring du domaine décide dans cet ordre : durée > 300 mois → refus quels que soient type et montant ; type absent ou hors IMMOBILIER, AUTO, CONSOMMATION, TRAVAUX, ETUDIANT → refus ; montant > plafond du type (CONSOMMATION 25 000, AUTO 40 000, TRAVAUX 80 000, ETUDIANT 20 000, IMMOBILIER 600 000 EUR) → refus ; sinon approbation
**And** un test paramétré prouve, pour chaque type, qu'un montant égal au plafond est approuvé et qu'un montant au plafond + 0.01 est refusé

**Given** un prêt EN_ATTENTE de 36 mois approuvé par le scoring
**When** la décision est appliquée
**Then** le prêt passe EN_COURS par `UPDATE … SET statut = 'EN_COURS' WHERE id = ? AND statut = 'EN_ATTENTE'` et 36 échéances mensuelles non payées sont créées dans `LOAN_REPAYMENTS`, dans la même transaction que la ligne `evenements_traites`
**And** la liste « Emprunts en cours » affiche le prêt « En cours » avec le bouton « Voir » après rechargement

**Given** un prêt EN_ATTENTE refusé par le scoring
**When** la décision est appliquée
**Then** le prêt passe REFUSE (« Refuse par le service credit »), sans échéance créée

**Given** un prêt de 10 000 EUR sur 60 mois sans type, enregistré EN_ATTENTE (insertion directe dans le test)
**When** son message est consommé
**Then** le prêt passe REFUSE (test `@Tag("FR-024-05")` et `@Tag("BUG-078")`)

**Given** un prêt dont le statut n'est plus EN_ATTENTE (EN_COURS, REFUSE, REMBOURSE)
**When** un message `credits.pret-demande.v1` le concernant est consommé
**Then** il est ignoré sans erreur : statut inchangé, aucune échéance créée, aucun événement `credits.pret-decide.v1` écrit

**Given** le même message brut (chaîne JSON de l'enveloppe, sans en-tête de type) livré deux fois
**When** le test Testcontainers (`testcontainers-kafka`, `testcontainers-postgresql`) du consommateur s'exécute
**Then** un seul effet est constaté (une décision, un seul échéancier)
**And** le test `PretTransitionConcurrenteTest` lance deux transitions concurrentes depuis EN_ATTENTE (approbation et refus) et constate une seule transition gagnante, sans second échéancier

**Given** une décision appliquée (approbation ou refus)
**When** la transaction se valide
**Then** une ligne `outbox` du topic `credits.pret-decide.v1` (clé `pretId`) est écrite dans la même transaction, avec `donnees` = `pretId`, `clientId`, `statut`, `notification` ; `notification` porte `clientId`, `type` CONFIRMATION et le `message` « Votre demande de pret {libelle} de {montant} EUR a ete approuvee par le service credit. » à l'approbation, `type` ALERTE et le `message` « Votre demande de pret {libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). » au refus, `{libelle}` valant « Immobilier », « Automobile », « Consommation », « Travaux » ou « Etudiant » (type absent ou inconnu : `{libelle}` vaut « (type non reconnu) », clé `pret.libelle.inconnu`, `textes-nouveaux.md`, lot « Stories ») ; `titre` « Demande de prêt approuvée » à l'approbation, « Demande de prêt refusée » au refus (clés `pret.notif.titre.*`, `textes-nouveaux.md`, lot « Stories »)
**And** `contrats/evenements/credits.pret-decide.v1.schema.json` existe avec un exemple, le bean `NewTopic` est déclaré, et `PretDecideContratEvenementProducteurTest` vérifie les champs exacts

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-09|FR-007-10|FR-024-03|FR-024-04|FR-024-05|FR-024-06|FR-024-07|FR-024-08|BUG-078'` puis `mvn -B -q -f services/credits/pom.xml verify`
**Then** les deux commandes sortent en code 0, chaque tag cité est porté par au moins un test exécuté, et le test d'idempotence et `PretTransitionConcurrenteTest` sont exécutés

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 5.4: Recevoir la décision sur sa demande de prêt dans le centre de notifications

En tant que client ayant déposé une demande de prêt,
je veux trouver la décision du service crédit dans mon centre de notifications,
afin de savoir sans chercher si mon prêt est accordé ou refusé.

**Exigences :** FR-025-03, FR-024-09, FR-007-10 (notification), FR-024-06 (notification), FR-024-07 (notification), FR-025-04 (plafond appliqué aux décisions de prêt), AR-22, AR-29, AR-31, AR-39, AR-41, AR-65, AR-91
**Scénarios golden master :** aucun
**Prérequis :** story 5.3 (producteur de `credits.pret-decide.v1`) ; story de l'epic 3 qui livre le centre de notifications et le plafond par utilisateur (FR-025-04, FR-025-05).

**Acceptance Criteria:**

**Given** un événement `credits.pret-decide.v1` d'approbation pour `user1` (prêt Consommation)
**When** le consommateur de `notifications` (groupe `notifications`) le traite
**Then** une notification CONFIRMATION « Votre demande de pret Consommation de {montant} EUR a ete approuvee par le service credit. » est enregistrée pour `user1`, avec le texte reçu tel quel (le service `notifications` ne compose aucun texte), et le tableau de bord de `user1` l'affiche dans « Centre de notifications » avec le type « CONFIRMATION » ✅ et la mention « Non lu »
**And** le montant est rendu par `credits` au format du legacy (un écart de format est corrigé dans `credits`, jamais dans `notifications`)

**Given** un événement de refus
**When** il est consommé
**Then** une notification ALERTE « Votre demande de pret {libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). » est affichée avec le type « ALERTE » ⚠️

**Given** le consommateur de `notifications`
**When** le test de contrat consommateur `PretDecideContratEvenementConsommateurTest` désérialise l'exemple de `contrats/evenements/credits.pret-decide.v1.schema.json`
**Then** il réussit ; un exemple dont un champ obligatoire est retiré est refusé et journalisé sans notification créée

**Given** le même message brut livré deux fois
**When** le test Testcontainers du consommateur s'exécute
**Then** une seule notification est créée

**Given** `user1` avec 20 notifications
**When** une 21e notification arrive par `credits.pret-decide.v1`
**Then** la plus ancienne est supprimée et `user1` garde exactement 20 notifications (`nb.notifications.plafond=20`)

**Given** une demande de prêt valide soumise par l'API avec la pile démarrée (`compose.yaml`)
**When** j'attends le traitement asynchrone
**Then** la trace Jaeger de la demande contient, sous le même identifiant de trace, le span HTTP de `credits`, l'émission et la consommation de `credits.pret-demande.v1`, l'émission de `credits.pret-decide.v1` et sa consommation par `notifications`, avec des spans JDBC (lu par l'API de Jaeger, `GET http://localhost:16686/api/traces?service=credits`)

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/notifications/pom.xml test -Dgroups='FR-025-03|FR-024-09'` puis `mvn -B -q -f services/notifications/pom.xml verify`
**Then** les deux commandes sortent en code 0 et chaque tag cité est porté par au moins un test exécuté

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 5.5: Consulter l'échéancier, marquer une échéance payée et supprimer un prêt

En tant qu'emprunteur,
je veux voir le détail des échéances d'un prêt, marquer une échéance payée et supprimer un prêt,
afin de suivre mon remboursement.

**Exigences :** FR-007-11, FR-007-12, FR-007-13, NFR-F007 (paiement, suppression), NFR-SEC-03, NFR-SEC-04, AR-20, AR-36, AR-43, AR-44, AR-45, UX-DR17, UX-DR49, UX-DR57, UX-DR94, UX-DR142
**Scénarios golden master :** aucun
**Prérequis :** story 5.3 (échéancier généré à l'approbation).

**Acceptance Criteria:**

**Given** `user1` avec un prêt EN_COURS ou REMBOURSE
**When** il clique « Voir » (`pret-voir-{loanId}`)
**Then** la section « Echeancier du pret selectionne », titrée « Detail des echeances », s'ouvre dans une `card-muted` sous la liste, le focus va à son titre, et le tableau affiche « # », « Date echeance » (jj/MM/aaaa, Europe/Paris), « Capital », « Interets », « Total », « Capital restant », « Statut » (« Paye » ou « En attente »), « Action »
**And** un prêt EN_ATTENTE ou REFUSE n'a pas de bouton « Voir »

**Given** une échéance non payée
**When** `user1` clique « Marquer paye » (`pret-marquer-paye-{repaymentId}`)
**Then** l'échéance est marquée payée avec la date du port Horloge, la page affiche « Echeance marquee comme payee » dans `pret-message-succes`, et aucun compte n'est débité (aucun événement vers `coeur-bancaire`)

**Given** une échéance déjà payée
**When** une demande de paiement la concerne
**Then** elle n'est pas payée une seconde fois (date inchangée), sans message d'erreur
**And** deux paiements concurrents de la même échéance (`EcheanceTransitionConcurrenteTest`) ne produisent qu'un seul effet

**Given** un prêt EN_COURS dont il ne reste qu'une échéance non payée
**When** elle est marquée payée
**Then** le prêt passe REMBOURSE (« Rembourse ») par une mise à jour conditionnelle sur EN_COURS

**Given** un prêt de `user1`
**When** il clique « Supprimer » (`pret-supprimer-{loanId}`)
**Then** le prêt et toutes ses échéances sont supprimés définitivement, sans demande de confirmation, et la ligne disparaît

**Given** `user1` connecté
**When** il appelle le paiement d'une échéance ou la suppression d'un prêt appartenant à `user2`
**Then** la réponse est 403 avec `{"message"}` portant respectivement « Erreur : l'échéance n'a pas été marquée comme payée. Veuillez réessayer. » ou « Erreur suppression : le prêt n'a pas été supprimé. Veuillez réessayer. », et rien n'est modifié ; un prêt ou une échéance inexistant donne 404

**Given** une erreur inattendue pendant le paiement ou la suppression
**When** l'action est demandée
**Then** la page affiche « Erreur : l'échéance n'a pas été marquée comme payée. Veuillez réessayer. » (`pret.erreur.paiement`) ou « Erreur suppression : le prêt n'a pas été supprimé. Veuillez réessayer. » (`pret.erreur.suppression`), sans détail technique

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-007-11|FR-007-12|FR-007-13'` puis `mvn -B -q -f services/credits/pom.xml verify`
**Then** les deux commandes sortent en code 0, chaque tag cité est porté par au moins un test exécuté, et `EcheanceTransitionConcurrenteTest` est exécuté

**Given** les tests de la story
**When** je lance `cd frontend && npx vitest run -t 'FR-007-11'`, puis avec `-t 'FR-007-12'` et `-t 'FR-007-13'`
**Then** chaque commande sort en code 0 avec au moins un test exécuté

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 5.6: Traiter le pipeline des prêts en attente depuis l'administration

En tant qu'administrateur,
je veux voir les prêts en attente, les approuver ou les refuser, et consulter l'historique de tous les prêts,
afin de traiter manuellement les demandes et de garder une vue complète du portefeuille de crédits.

**Exigences :** FR-016-03, FR-016-04, FR-016-05, FR-016-07, FR-016-12, FR-016-06 (codes ADMIN_LOAN_APPROVE et ADMIN_LOAN_REFUSE), FR-026-02 (codes des prêts), FR-026-04, FR-026-05 (IP des actions de prêt), FR-030-07 (actions de prêt de l'administration), NFR-PAR-02, NFR-SEC-03, NFR-SEC-04, AR-28, AR-29, AR-31, AR-32, AR-36, AR-40, AR-41, AR-43, AR-48, AR-58, AR-89, AR-97, UX-DR17, UX-DR47, UX-DR48, UX-DR57, UX-DR132, UX-DR153
**Scénarios golden master :** aucun
**Prérequis :** A-016-1 (texte « Erreur lors du traitement du prêt. Veuillez réessayer. ») — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; stories 5.3 et 5.5 ; epic 2 (page `/administration` réservée à ADMIN, producteur de `identite-clients.client-modifie.v1`, service `audit`) ; story de l'epic 3 qui ajoute la file de revue à `/administration`.

**Acceptance Criteria:**

**Given** le topic `identite-clients.client-modifie.v1`
**When** `credits` consomme un événement client
**Then** sa projection locale des clients (`clientId`, `login`, `nomComplet`, `role`, `actif`, `conseillerId`, `dateCreation`, `supprime`, `versionAgregat`) est mise à jour seulement si `versionAgregat` est strictement supérieur à celui détenu, dans la même transaction que `evenements_traites`
**And** `ClientModifieContratEvenementConsommateurTest` désérialise l'exemple du schéma, le test Testcontainers de double livraison constate un seul effet, et la projection de référence de `credits` est générée depuis le `reference-data.sql` d'`identite-clients` par `scripts/generer-projections-reference.sh` (créé par cette story s'il n'existe pas), jamais écrite à la main

**Given** `admin` connecté et un prêt EN_ATTENTE en base
**When** il ouvre `/administration`
**Then** la section « Pipeline Prets — En attente d approbation » liste les prêts EN_ATTENTE avec « ID », « Client » (nom complet lu dans la projection), « Type », « Montant », « Duree », « Taux », « Mensualite », « Motif », « Actions » et les boutons « Approuver » (`admin-approuver-{loanId}`) et « Refuser » (`admin-refuser-{loanId}`)
**And** la section est placée après les indicateurs et avant « Gestion des clients » et la file de revue des virements suspendus (fenêtre de 5 000 caractères)
**And** sans prêt EN_ATTENTE, le tableau affiche la ligne « Aucun pret en attente » sous les en-têtes conservés

**Given** un prêt EN_ATTENTE de 36 mois
**When** `admin` clique « Approuver »
**Then** le prêt passe EN_COURS par mise à jour conditionnelle, 36 échéances sont générées, la page affiche « Prêt approuvé » dans `admin-message-succes` et le prêt quitte le pipeline, sans demande de confirmation
**And** une ligne `outbox_audit` du topic `credits.audit.v1` est écrite par `Transaction.executerIndependamment(...)` avec `utilisateur` = identifiant de l'administrateur, `action` « ADMIN_LOAN_APPROVE », `detail` « Approbation prêt id={loanId} » et `adresseIp` résolue dans `adapter.in.http`, et le service `audit` l'enregistre dans `AUDIT_LOGS`

**Given** un prêt EN_ATTENTE
**When** `admin` clique « Refuser »
**Then** le prêt passe REFUSE immédiatement, sans confirmation, la page affiche « Prêt refusé », et l'audit « ADMIN_LOAN_REFUSE » « Refus prêt id={loanId} » est écrit avec l'IP

**Given** un prêt qui n'est plus EN_ATTENTE (déjà décidé par le scoring ou par un autre administrateur)
**When** `admin` demande son approbation ou son refus
**Then** la demande est ignorée sans erreur ni changement : aucun échéancier, aucune entrée d'audit de succès

**Given** une erreur provoquée pendant l'approbation, puis pendant le refus, d'un prêt EN_ATTENTE
**When** `admin` clique « Approuver » ou « Refuser »
**Then** la page affiche « Erreur lors du traitement du prêt. Veuillez réessayer. » dans `admin-message-erreur`, sans texte d'exception ni nom technique, le prêt reste EN_ATTENTE (test `@Tag("FR-016-07")` et `@Tag("BUG-054")`)
**And** l'entrée d'audit est présente dans `outbox_audit` bien que l'action soit annulée (FR-026-04)

**Given** `user1` (CLIENT) ou `sophie` (CONSEILLER) avec un jeton valide
**When** il appelle directement l'approbation, le refus, le pipeline ou l'historique des prêts de l'administration dans `credits`
**Then** la réponse est 403 avec `{"message"}` et aucun prêt n'est modifié ; la même demande par `admin` réussit

**Given** `admin` sur `/administration`
**When** il regarde « Historique de tous les prets »
**Then** la section, en lecture seule, liste tous les prêts quel que soit leur statut, triés par date de début décroissante, avec « ID », « Client », « Type », « Montant », « Statut », « Duree », « Mensualite », « Date debut »
**And** sans aucun prêt en base, elle affiche « Aucun pret en base »

**Given** une décision de l'administrateur
**When** elle est appliquée
**Then** aucun événement `credits.pret-decide.v1` n'est publié (FR-016-04 et FR-016-05 ne prévoient pas de notification, comme le legacy)

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-016-03|FR-016-04|FR-016-05|FR-016-07|FR-016-12|FR-016-06|FR-026-02|FR-026-05|FR-030-07|BUG-054'` puis `mvn -B -q -f services/credits/pom.xml verify`
**Then** les deux commandes sortent en code 0, chaque tag cité est porté par au moins un test exécuté, et le test d'IP (`X-Forwarded-For` hors plage ignoré, dans la plage retenu) et le test « opération annulée → entrée présente dans `outbox_audit` » sont exécutés

**Given** les tests de la story
**When** je lance `cd frontend && npx vitest run -t 'FR-016-03'`, puis avec `-t 'FR-016-04'`, `-t 'FR-016-05'`, `-t 'FR-016-07'`, `-t 'FR-016-12'`
**Then** chaque commande sort en code 0 avec au moins un test exécuté

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

## Epic 6: Obtenir ses documents bancaires

À la fin de l'epic, tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) ouvre la page Documents et télécharge, pour chacun de ses comptes, son RIB, une attestation de solde et ses relevés mensuels des 12 derniers mois, ainsi que ses IFU des deux dernières années fiscales. Le service `documents` est le seul producteur de PDF ; il tient trois projections (comptes, opérations, clients) alimentées par `coeur-bancaire.compte-modifie.v1` et `coeur-bancaire.operation-enregistree.v1`, que cet epic fait produire par `coeur-bancaire`, et par `identite-clients.client-modifie.v1`. Toute demande sans jeton est refusée (401) et le front ramène à la connexion ; un compte d'un autre client n'est jamais servi.

**Contrôle de fin d'epic (à la charge de l'humain) :** lire, dans `reports/golden-master/textes-non-trouves.md`, les sections des scénarios de l'epic (SC-008-01, SC-017-02) ; lancer `scripts/golden-master/verifier-scenario.sh` pour chacun (`scripts/golden-master/verifier-scenario.sh SC-008-01`, `scripts/golden-master/verifier-scenario.sh SC-017-02`). SC-017-01 est un scénario HTTP : il est couvert par le test d'intégration du service `documents` de la story 6.1, pas par ce script.

### Story 6.1: Télécharger son RIB et son attestation de solde depuis le service documents

En tant qu'utilisateur authentifié,
je veux télécharger le RIB et l'attestation de solde d'un de mes comptes en PDF,
afin de les fournir à un tiers, sans qu'aucun autre client ne puisse obtenir les miens.

**Exigences :** FR-017-01, FR-017-02, FR-017-03, FR-017-05, FR-017-06, FR-017-08, FR-017-09, FR-017-10, FR-017-11, FR-008-02, FR-008-04, FR-008-08, FR-008-09, FR-008-11, FR-008-12, FR-030-06 (contrôle côté service), NFR-PAR-02, NFR-PAR-04, NFR-SEC-03, NFR-SEC-04, NFR-UI-01, AR-17, AR-19, AR-21, AR-26, AR-28, AR-29, AR-31, AR-32, AR-36, AR-37, AR-38, AR-39, AR-41, AR-44, AR-46, AR-49, AR-57, AR-58, AR-75, AR-87, AR-97, AR-101, UX-DR80
**Scénarios golden master :** SC-017-01
**Prérequis :** A-008-1 et A-017-1 (document d'un autre client refusé en HTTP 403 sans message métier), A-008-2 (compteur de téléchargements implémenté), A-017-2 (compte inexistant = 404 et non 500, écart attendu au golden master) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; AR-101 (bibliothèque PDF choisie et vérifiée par cette story avant tout PDF) ; epic 2 (producteur de `identite-clients.client-modifie.v1`, vérification du jeton, format d'erreur) ; epics 3 et 4 (chemins d'écriture des comptes du cœur bancaire).

**Acceptance Criteria:**

**Given** la story démarrée
**When** je choisis la bibliothèque PDF du service `documents`
**Then** `versions-verifiees.md` gagne une ligne pour elle (version retenue, URL du registre officiel consulté, date de vérification, compatibilité Java 21 et Spring Boot 4.1.1, licence) avant que le premier PDF soit écrit
**And** la bibliothèque n'est utilisée que dans `adapter.out` de `documents` (`HexagonalArchitectureTest` vert : aucun framework dans `domain` et `application`)

**Given** le dépôt après la story
**When** je liste `services/`
**Then** le module `services/documents/` existe (pom.xml déclaré dans le POM parent, Dockerfile, `reference-data/reference-data.sql`, paquets `fr.nextbanking.documents.{domain,application,adapter.in,adapter.out,infrastructure}`), avec sa base `postgres-documents` et son service `documents` dans `compose.yaml` (port de développement 8084, `healthcheck`), renvoyé par le conteneur web et le proxy Vite pour `/api/documents/**`
**And** `HexagonalArchitectureTest`, `HorlogeArchitectureTest` et `ClientsHttpArchitectureTest` existent dans `services/documents/src/test` et passent

**Given** `coeur-bancaire`
**When** une écriture modifie un compte de `BANK_ACCOUNTS` (création, changement de solde par un virement exécuté, par le débit asynchrone d'un virement libéré, par une échéance de virement programmé, par les intérêts)
**Then** une ligne `outbox` du topic `coeur-bancaire.compte-modifie.v1` (clé `compteId`) est écrite dans la même transaction, avec l'état complet du compte (`compteId`, `clientId`, `numero`, `type`, `iban`, `bic`, `solde`) et un `versionAgregat` strictement croissant
**And** `contrats/evenements/coeur-bancaire.compte-modifie.v1.schema.json` existe avec un exemple, le bean `NewTopic` est déclaré dans `infrastructure` de `coeur-bancaire`, et `CompteModifieContratEvenementProducteurTest` vérifie les champs exacts ; un test par chemin d'écriture constate un événement par compte modifié (un virement exécuté → deux événements, source et destination)
**And** `coeur-bancaire` reste le seul service qui modifie un solde (`UPDATE bank_accounts` absent de `services/documents`)

**Given** `documents` démarré
**When** il consomme `coeur-bancaire.compte-modifie.v1` et `identite-clients.client-modifie.v1`
**Then** ses projections des comptes et des clients sont mises à jour seulement si `versionAgregat` est strictement supérieur à celui détenu, dans la même transaction que `evenements_traites`
**And** les tests de contrat consommateur désérialisent l'exemple de chaque schéma, et le test Testcontainers de chaque consommateur livre deux fois le même message brut et constate un seul effet
**And** `services/documents/reference-data/reference-data.sql` contient les 5 lignes `DOCUMENTS` d'`import.sql` et les projections comptes et clients générées par `scripts/generer-projections-reference.sh` depuis les `reference-data.sql` de `coeur-bancaire` et d'`identite-clients`, jamais écrites à la main

**Given** aucune session
**When** j'appelle `GET /api/documents/telechargement?type=RIB&compteId=1` sans jeton
**Then** la réponse est 401 et aucun document n'est généré ni compté (test d'intégration `@Tag("FR-017-01")` reprenant `docs/retro/golden-master/derived/FEAT-017/SC-017-01/input-request.json` et le statut de `expected-response.json`)
**And** `DocumentsSecuriteIT` prouve : sans jeton → 401 ; compte existant d'un autre client → 403 ; compte inexistant → 404 ; CLIENT, CONSEILLER et ADMIN accèdent chacun à leurs propres comptes (FR-030-06)

**Given** `user1` connecté
**When** il demande `GET /api/documents/telechargement` sans `type`, puis avec `type=FACTURE`, puis `type=RIB` sans `compteId`, avec `compteId=` vide et avec `compteId=abc`
**Then** les réponses sont respectivement 400 « Parametre type manquant », 400 « Type inconnu : FACTURE », 400 « accountId manquant » (trois fois), sans PDF (décision du 2026-10-05 : corps `{"message": "…"}` dont la valeur est le texte du legacy ; FR-017-10 : `{"message": "Erreur lors de la generation du document"}` ; SC-017-01 ne compare pas le corps (`body: null`), aucun écart voulu à inscrire)
**And** `type=rib` et `type=Rib` sont acceptés (comparaison insensible à la casse)
**And** `type=RELEVE` et `type=IFU` passent la validation du type ; leur génération est livrée par la story 6.2

**Given** `user1` connecté
**When** il demande `type=RIB&compteId=999999` (compte inexistant)
**Then** la réponse est 404 « Compte introuvable », sans PDF

**Given** `user1` connecté (client A) et un compte existant de `user2` (client B)
**When** `user1` demande `type=RIB`, puis `type=ATTESTATION` sur ce compte
**Then** la réponse est 403, aucun PDF n'est généré ni compté, les contrôles de type et de `compteId` ayant été faits avant (test `@Tag("FR-017-08")`, `@Tag("FR-008-04")`, `@Tag("BUG-058")` et `@Tag("BUG-027")`) avec le corps `{"message": "Accès refusé"}` ; seul le titulaire du compte télécharge ses documents, quel que soit son rôle — CONSEILLER et ADMIN compris (décision du 2026-10-05, conforme au PRD ; AD-16 de la spine est à corriger)
**And** la même demande sur un compte de `user1` renvoie le PDF

**Given** `user1` connecté et son compte `1`
**When** il demande `type=RIB&compteId=1`
**Then** la réponse est 200, `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="rib_1.pdf"`, `Cache-Control: no-cache, no-store, must-revalidate`
**And** le texte extrait du PDF contient « Demo Bank — Releve d Identite Bancaire (RIB) », « Titulaire » et le nom complet de la projection clients, « Banque » « Demo Bank SA », « Code banque » « 30004 », « Code guichet » « 00001 », « Numero de compte », « Cle RIB » « 42 », « IBAN », « BIC / SWIFT » « DEMOFR21XXX », « Domiciliation » « Demo Bank - Agence Paris Centre », « Document genere le {jj/mm/aaaa} - Demo Bank SA, 1 Place Vendome, 75001 Paris » (date du port Horloge, Europe/Paris) et le pied de page « Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012 — Établissement de crédit agréé par l'ACPR »

**Given** `user1` connecté et son compte `1`
**When** il demande `type=ATTESTATION&compteId=1`
**Then** la réponse est un PDF `attestation_solde_1.pdf` aux mêmes en-têtes, dont le texte contient « Demo Bank — Attestation de Solde », « Demo Bank SA, dont le siege social est situe 1 Place Vendome, 75001 Paris, atteste par la presente que : », « M. / Mme {nom complet} », « est titulaire du compte n° {numéro de compte} », « Le solde de ce compte, arrete au {jj mois aaaa}, est de : », « {montant} EUR » (solde de la projection des comptes, de ce seul compte), « Cette attestation est delivree a la demande de l interesse(e) pour faire valoir ce que de droit. », « Fait a Paris, le {jj mois aaaa} » (date du port Horloge) et le pied de page

**Given** une erreur inattendue provoquée pendant la génération (adaptateur PDF en échec dans le test)
**When** `user1` demande son RIB
**Then** la réponse est 500 avec le seul texte « Erreur lors de la generation du document », sans message d'exception, trace ni nom de composant (test `@Tag("FR-017-10")` et `@Tag("BUG-059")`)

**Given** un document téléchargé avec succès
**When** le même document (même client, même type, mêmes paramètres) est téléchargé une deuxième fois
**Then** sa ligne `DOCUMENTS` a un compteur de téléchargements égal à 2 ; une demande refusée (400, 403, 404, 500) ne l'incrémente pas

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-017-01|FR-017-02|FR-017-03|FR-017-05|FR-017-06|FR-017-08|FR-017-09|FR-017-10|FR-017-11|FR-008-02|FR-008-04|FR-008-08|FR-008-09|FR-008-11|FR-008-12|FR-030-06|BUG-027|BUG-058|BUG-059'`, `mvn -B -q -f services/documents/pom.xml verify` et `mvn -B -q -f services/coeur-bancaire/pom.xml verify`
**Then** les trois commandes sortent en code 0, chaque tag cité est porté par au moins un test exécuté, et `DocumentsSecuriteIT` est exécutée

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 6.2: Télécharger ses relevés mensuels et ses IFU

En tant qu'utilisateur authentifié,
je veux télécharger le relevé mensuel d'un de mes comptes et mon IFU d'une année,
afin de justifier mes opérations et de déclarer mes revenus de capitaux mobiliers.

**Exigences :** FR-017-04, FR-017-07, FR-017-09 (client introuvable), FR-017-11, FR-008-07, FR-008-10, NFR-SEC-03, AR-21, AR-28, AR-29, AR-31, AR-32, AR-38, AR-41, AR-44, AR-49, AR-58, AR-97
**Scénarios golden master :** aucun
**Prérequis :** story 6.1.

**Acceptance Criteria:**

**Given** `coeur-bancaire`
**When** une opération est enregistrée dans `TRANSACTIONS` (virement exécuté, débit asynchrone d'un virement libéré, échéance de virement programmé, intérêts)
**Then** une ligne `outbox` du topic `coeur-bancaire.operation-enregistree.v1` (clé `compteId`) est écrite dans la même transaction, avec `operationId`, `compteId`, `instant` (UTC), `libelle`, `type`, `montant`, `soldeApres`, `versionAgregat`
**And** `contrats/evenements/coeur-bancaire.operation-enregistree.v1.schema.json` existe avec un exemple, le bean `NewTopic` est déclaré, et `OperationEnregistreeContratEvenementProducteurTest` vérifie les champs exacts ; un test par chemin d'écriture constate un événement par opération

**Given** `documents`
**When** il consomme `coeur-bancaire.operation-enregistree.v1`
**Then** sa projection des opérations est alimentée dans la même transaction que `evenements_traites` ; le test de contrat consommateur et le test Testcontainers de double livraison (un seul effet) passent
**And** les opérations de référence de la projection sont générées par `scripts/generer-projections-reference.sh` depuis le `reference-data.sql` de `coeur-bancaire`

**Given** `user1` connecté, son compte `1` et des opérations en mars 2026 et en avril 2026
**When** il demande `type=RELEVE&compteId=1&annee=2026&mois=3`
**Then** la réponse est un PDF `releve_1_2026_03.pdf` (en-têtes de FR-017-11) titré « Demo Bank — Releve de compte », avec « Compte », « Titulaire », « Periode », « Solde actuel » (« {montant} EUR ») et un tableau « Date », « Description », « Type », « Montant », « Solde apres » qui ne liste que les opérations de mars 2026 (mois découpé en Europe/Paris), triées par date croissante, types « CREDIT »/« DEBIT », montants « +{montant} EUR »/« -{montant} EUR »
**And** pour un mois sans opération, le PDF affiche « Aucune operation sur cette periode. »
**And** pour un compte de `user2`, la réponse est 403 sans PDF

**Given** `user1` connecté avec des crédits dont la description contient « interet » en 2025 (dont un total donnant un PFU à arrondir au demi supérieur) et d'autres crédits et débits
**When** il demande `type=IFU&annee=2025`
**Then** la réponse est un PDF `ifu_{customerId}_2025.pdf` du client de la session, titré « Demo Bank — Imprime Fiscal Unique (IFU) 2025 », avec « Etablissement » « Demo Bank SA », « SIREN » « 123 456 789 00012 », « Titulaire », « Adresse » « 1 rue de la Paix, 75001 Paris », « Annee fiscale », le tableau « Nature des revenus » / « Montant brut » aux lignes « Interets et produits de placement » (total des seuls crédits dont la description contient « interet ») et « Prelevement Forfaitaire Unique (30%) » (30 % arrondi `HALF_UP` à 2 décimales), la note « Ce document est fourni a titre informatif. Il recense les revenus de capitaux mobiliers soumis au PFU pour l annee 2025. », « Genere le {jj/mm/aaaa} » et le pied de page

**Given** `user1` connecté
**When** il demande `type=IFU&annee=2025` en ajoutant un paramètre désignant le client de `user2`
**Then** le PDF porte le titulaire et les revenus de `user1`, jamais ceux de `user2`

**Given** un jeton valide dont le client est absent de la projection des clients
**When** il demande `type=IFU&annee=2025`
**Then** la réponse est 404 « Client introuvable », sans PDF

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-017-04|FR-017-07|FR-017-09|FR-008-07|FR-008-10'`, `mvn -B -q -f services/documents/pom.xml verify` et `mvn -B -q -f services/coeur-bancaire/pom.xml verify`
**Then** les trois commandes sortent en code 0 et chaque tag cité est porté par au moins un test exécuté

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 6.3: Consulter la page Documents et ouvrir ses PDF

En tant qu'utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN),
je veux voir, sur la page Documents, les documents disponibles pour chacun de mes comptes et mes IFU,
afin d'ouvrir le document voulu en un clic.

**Exigences :** FR-008-01, FR-008-02 (redirection côté front), FR-008-03, FR-008-05, FR-008-06, FR-030-06 (redirection côté front), NFR-UI-01, NFR-UI-02, AR-45, AR-76, AR-78, AR-79, AR-87, UX-DR21, UX-DR28, UX-DR39, UX-DR40, UX-DR59, UX-DR78, UX-DR93, UX-DR124, UX-DR148, UX-DR156, UX-DR159
**Scénarios golden master :** SC-008-01, SC-017-02
**Prérequis :** stories 6.1 et 6.2 ; epic 2 (garde des routes, gabarit, `scripts/golden-master/verifier-scenario.sh`).

**Acceptance Criteria:**

**Given** un utilisateur non authentifié
**When** il ouvre `/documents`, `/mon-conseiller`, `/espace-conseiller` ou `/administration`
**Then** le routeur du front l'envoie sur `/login` sans afficher l'écran demandé (test Vitest sur les quatre chemins)

**Given** `user1`, `sophie` ou `admin` connecté
**When** il ouvre `/documents`
**Then** la page (titre d'onglet « BankApp Demo - Documents ») est affichée pour les trois rôles, avec seulement les comptes de l'utilisateur connecté, lus par `documents` dans sa projection des comptes, et la date `aujourdhui` (`yyyy-MM-dd`, Europe/Paris) fournie par `documents`

**Given** `user1` et son compte COURANT
**When** la page est affichée
**Then** une section « Compte {type de compte} - {numéro de compte} » contient une carte RIB (étiquette « Bancaire », titre « RIB », sous-titre « Releve d Identite Bancaire », lien « Telecharger PDF »), une carte attestation (étiquette « Attestation », titre « Attestation de solde », sous-titre « Solde au jour de la demande », lien « Telecharger PDF ») et la liste « Releves mensuels — {numéro de compte} » avec un lien « {mois} {année} » pour chacun des 12 mois les plus récents calculés depuis `aujourdhui` (libellés « Janvier », « Février », « Mars », « Avril », « Mai », « Juin », « Juillet », « Août », « Septembre », « Octobre », « Novembre », « Décembre »)
**And** les étiquettes sont des `tag` et les séparateurs des `eyebrow` rendus en capitales par CSS, le texte source gardant sa casse

**Given** `aujourdhui` = 2026-11-17
**When** la page est affichée
**Then** la section « Documents fiscaux » titrée « Imprime Fiscal Unique (IFU) » affiche les cartes « IFU 2025 » et « IFU 2024 », chacune avec l'étiquette « Fiscal », le sous-titre « Revenus de capitaux mobiliers » et le lien « Telecharger PDF »
**And** le front ne lit jamais l'horloge du navigateur pour ces valeurs (`frontend/src/architecture-temps.test.ts` vert)

**Given** la page affichée
**When** j'active un lien « Telecharger PDF » ou un lien mensuel
**Then** le front demande `GET /api/documents/telechargement` avec `Authorization: Bearer` et les paramètres du document (`type`, `compteId`, `annee`, `mois`), puis ouvre le PDF reçu dans un nouvel onglet
**And** chaque lien porte un `data-testid` (`documents-rib-{compteId}`, `documents-attestation-{compteId}`, `documents-releve-{compteId}-{aaaa}-{mm}`, `documents-ifu-{annee}`) et un nom accessible qui contient « Telecharger PDF » suivi de « (nouvel onglet) » réservé aux lecteurs d'écran (`a11y.nouvel.onglet`)

**Given** un jeton expiré ou supprimé
**When** j'active un lien de téléchargement et que `documents` répond 401
**Then** le front supprime `nb.jeton` et affiche `/login`, sans ouvrir d'onglet

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-008-01`
**Then** le code de sortie est 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-017-02`
**Then** le code de sortie est 0

**Given** les tests de la story
**When** je lance `cd frontend && npx vitest run -t 'FR-008-01'`, puis avec `-t 'FR-008-02'`, `-t 'FR-008-03'`, `-t 'FR-008-05'`, `-t 'FR-008-06'`, `-t 'FR-030-06'`
**Then** chaque commande sort en code 0 avec au moins un test exécuté

**Given** les tests de la story
**When** je lance `mvn -B -q -f services/documents/pom.xml test -Dgroups='FR-008-03|FR-008-05|FR-008-06'`
**Then** le code de sortie est 0 et chaque tag cité est porté par au moins un test exécuté

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

## Epic 7: Bourse, conseiller virtuel et catalogue des produits

À la fin de l'epic, tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) suit les cotations des valeurs de la watchlist commune (indicateurs, graphique généré par le serveur, dernières séances), ajoute et retire des valeurs. Il échange avec Sophie, la conseillère virtuelle ; si le service d'IA est indisponible, il en est averti dans la conversation sans code technique (BUG-038 corrigé). Il consulte le catalogue des produits bancaires et l'historique des traitements nocturnes. Les fournisseurs AlphaVantage et Azure OpenAI sont appelés derrière un port, avec un adaptateur simulé actif par défaut, un délai d'attente, un coupe-circuit et un cache des cotations (AD-20).

**Contrôle de fin d'epic (à la charge de l'humain) :** lire, dans `reports/golden-master/textes-non-trouves.md`, les sections des scénarios de l'epic (SC-009-01, SC-010-01, SC-011-01) ; lancer `scripts/golden-master/verifier-scenario.sh` pour chacun. SC-010-02 est exclu du golden master (BUG-038) : rien à lire ni à lancer pour lui, il est remplacé par les tests de FR-010-09 (stories 7.7 et 7.8).

### Story 7.1: Ouvrir le service bourse et gérer la watchlist commune

En tant qu'utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN),
je veux consulter, compléter et réduire la watchlist commune par l'API du service bourse,
afin de suivre les valeurs qui m'intéressent.

**Exigences :** FR-009-01, FR-009-10, FR-009-11, FR-009-12, FR-009-13, FR-029-09, FR-029-10, FR-029-11, FR-029-13, FR-029-14, NFR-SEC-04, NFR-PAR-04, NFR-QG-01, NFR-QG-03, AR-17, AR-24, AR-26, AR-36, AR-46, AR-47, AR-57, AR-71, AR-88
**Scénarios golden master :** aucun
**Prérequis :** Epic 2 terminé (story de connexion : vérification du jeton, format d'erreur `{"message"}`, renvoi `/api/{identifiant}/**` par le proxy Vite et le conteneur web) ; A-029-2 (symbole ou nom trop long bloqué par la limite du champ, sans message) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** la story terminée
**When** je lance `ls services/`
**Then** `bourse` figure dans la liste, avec `services/bourse/pom.xml`, `Dockerfile`, `reference-data/reference-data.sql` et les paquets `fr/nextbanking/bourse/{domain,application,adapter/in,adapter/out,infrastructure}`
**And** `compose.yaml` déclare `bourse` (port de développement 8087) et `postgres-bourse` (volume nommé `pgdata-bourse`), `compose.rejeu.yaml` lui applique le profil Spring `rejeu`, et `/api/bourse/**` est renvoyé vers le service par le proxy de `frontend/vite.config.ts` et par le conteneur web

**Given** le service bourse
**When** je lance `mvn -B -q -f services/bourse/pom.xml test -Dtest='*ArchitectureTest'`
**Then** le code de sortie est 0 (`HexagonalArchitectureTest`, `HorlogeArchitectureTest`, `ClientsHttpArchitectureTest` présents et verts)

**Given** les données de référence chargées (9 valeurs : AI.PA, BNP.PA, MC.PA, OR.PA, SAN.PA, TTE.PA, AAPL, GOOGL, MSFT)
**When** un utilisateur CLIENT, puis CONSEILLER, puis ADMIN appelle `GET /api/bourse/watchlist`
**Then** chacun reçoit 200 et la même liste de 9 valeurs (symbole, nom, marché, devise), triée par marché croissant puis par symbole croissant, la première étant AI.PA

**Given** aucun jeton valide
**When** j'appelle `GET /api/bourse/watchlist`
**Then** la réponse est 401 au format `{"message": …}` d'AD-17, sans pile d'appels

**Given** la watchlist de référence
**When** j'envoie `POST /api/bourse/watchlist` avec `{"symbole":"  sg.pa ","nom":" Société Générale ","marche":"Euronext Paris","devise":"eur"}`
**Then** la réponse est 201 et la valeur est enregistrée avec le symbole "SG.PA", le nom "Société Générale", le marché "Euronext Paris" et la devise "EUR"
**And** `GET /api/bourse/watchlist` la place à son rang (marché puis symbole)

**Given** la watchlist de référence
**When** j'envoie `POST /api/bourse/watchlist` avec un symbole vide ou blanc, puis un nom vide ou blanc, puis le symbole "bnp.pa"
**Then** les réponses sont 400 avec respectivement `{"message":"Le symbole est obligatoire (ex: AMZN, SG.PA)"}`, `{"message":"Le nom de la valeur est obligatoire"}` et `{"message":"Le symbole « BNP.PA » existe déjà dans votre watchlist"}`
**And** la watchlist compte toujours 9 valeurs

**Given** la watchlist de référence
**When** j'envoie `POST /api/bourse/watchlist` avec un symbole de 21 caractères, puis un nom de 101 caractères, puis le marché "Bourse de Tokyo", puis la devise "JPY"
**Then** chaque requête est refusée en 400 sans enregistrement (marchés acceptés : "Euronext Paris", "NASDAQ", "NYSE", "LSE", "Autre" ; devises : "EUR", "USD", "GBP" ; marché ≤ 50 et devise ≤ 10 caractères)
**And** le `message` de ces refus est « Requête invalide » (400, clé `api.erreur.400`, décidé, `textes-nouveaux.md`, lot « Stories »)

**Given** la valeur AI.PA dans la watchlist
**When** j'appelle `DELETE /api/bourse/watchlist/{id}` avec son identifiant
**Then** la réponse est 204, sans confirmation, et AI.PA n'apparaît plus dans `GET /api/bourse/watchlist`
**And** `DELETE` sur un identifiant inexistant répond 404 au format `{"message": …}`

**Given** la story terminée
**When** je lance `mvn -B -q -f services/bourse/pom.xml test -Dgroups='<FR>'` pour chacune de FR-009-01, FR-009-10, FR-009-11, FR-009-12, FR-009-13, FR-029-09, FR-029-10, FR-029-11, FR-029-13, FR-029-14
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 7.2: Raccorder le fournisseur de cotations AlphaVantage derrière un port simulé, avec coupe-circuit et cache

En tant qu'utilisateur authentifié,
je veux obtenir l'historique des cours d'une valeur pour la période choisie, même quand le fournisseur est lent, refuse ou n'a pas de données,
afin de suivre l'évolution d'une valeur sans que la page bourse se bloque.

**Exigences :** FR-009-08, FR-009-09, FR-009-14, FR-029-01, FR-029-02, FR-029-03, FR-029-04, FR-029-05, NFR-INT-01, NFR-INT-02, NFR-INT-03, NFR-SEC-04, NFR-SEC-06, AR-24, AR-27, AR-50, AR-60, AR-61, AR-99, AR-102
**Scénarios golden master :** aucun
**Prérequis :** Story 7.1 ; AR-102 (point ouvert : bibliothèque de coupe-circuit et de cache des fournisseurs) réglé par cette story, première story de fournisseur.

**Acceptance Criteria:**

**Given** la story terminée
**When** je lis `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/versions-verifiees.md`
**Then** il contient une ligne pour la bibliothèque de coupe-circuit et une pour le cache des fournisseurs retenues (version, URL source, date de vérification, compatibilité avec Spring Boot 4.1.1), qui règlent AR-102
**And** les réglages communs (délai d'attente, seuils du coupe-circuit, durée du cache) sont sous `nb.fournisseurs.alphavantage.*` et validés au démarrage : une valeur invalide empêche le démarrage

**Given** un contexte du service bourse démarré sans aucune propriété `nb.fournisseurs.*`
**When** le test de configuration s'exécute
**Then** le bean actif du port de cotations est l'adaptateur simulé `AlphaVantageSimule` (dans `adapter.out.fournisseur`), sans appel réseau
**And** `! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' services/*/src/main/resources compose*.yaml` sort en code 0

**Given** l'adaptateur simulé et l'Horloge réglée au 2026-11-17
**When** j'appelle `GET /api/bourse/historique?symbole=AI.PA&periode=1M`
**Then** la réponse est 200 avec la série journalière fixe d'AI.PA (dates calculées depuis le port Horloge), sans point antérieur au 2026-10-17, triée par date croissante (date, ouverture, haut, bas, clôture, volume)
**And** `periode=1W` garde la série journalière à partir du 2026-11-10, `1Y` et `5Y` la série hebdomadaire à partir du 2025-11-17 et du 2021-11-17, et une période inconnue applique la date limite par défaut d'un mois

**Given** l'adaptateur simulé
**When** j'appelle `GET /api/bourse/historique` pour un symbole sans série (ex. "INCONNU.XX")
**Then** la réponse est 200 avec une liste de points vide, et non une erreur

**Given** le mode réel pointé vers un serveur de bouchon local (aucun accès réseau externe)
**When** le bouchon répond par un champ "Information", "Note" ou "Error Message", par un statut 500, ou au-delà du délai d'attente configuré
**Then** `GET /api/bourse/historique` répond 200 avec une liste de points vide dans le délai configuré, l'échec est journalisé (avec `traceId`), et aucune erreur 5xx ni détail technique n'est renvoyé

**Given** le mode réel et un bouchon qui échoue de façon répétée
**When** le nombre d'échecs atteint le seuil du coupe-circuit
**Then** les appels suivants n'atteignent plus le bouchon (compteur de requêtes inchangé) et répondent 200 avec une liste vide

**Given** le mode réel et un bouchon qui compte les requêtes
**When** j'appelle deux fois `GET /api/bourse/historique?symbole=AI.PA&periode=1M`, puis `periode=1W`, dans la durée du cache
**Then** le bouchon n'a reçu qu'une requête (série journalière compacte partagée par 1W et 1M), conformément à la limite AlphaVantage de 25 requêtes par jour et 5 par minute

**Given** `ALPHA_VANTAGE_BASE_URL` absent puis défini (variable d'environnement, puis propriété système)
**When** le service démarre en mode réel
**Then** l'adresse utilisée est "https://www.alphavantage.co/query" par défaut, sinon la valeur configurée

**Given** le mode réel sans `ALPHA_VANTAGE_API_KEY` (ni variable d'environnement ni propriété système)
**When** j'appelle `GET /api/bourse/historique?symbole=AI.PA&periode=1M`
**Then** le bouchon ne reçoit aucune requête et la réponse est 200 avec une liste vide (absence de données)
**And** `git grep -nEi 'apikey *[:=] *[A-Za-z0-9]{4,}'` ne trouve aucune clé dans les fichiers versionnés

**Given** une clé écrite en dur ajoutée volontairement comme valeur par défaut dans `services/bourse/src/main/resources/application.yml`
**When** je lance `mvn -B -q -f services/bourse/pom.xml test -Dgroups='BUG-091'`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `mvn -B -q -f services/bourse/pom.xml test -Dgroups='<tag>'` pour chacun de FR-009-08, FR-009-09, FR-009-14, FR-029-01, FR-029-02, FR-029-03, FR-029-04, FR-029-05, BUG-034, BUG-091
**Then** chaque commande sort en code 0, sans clé définie et sans accès réseau (`mvn -o verify` passe sur un cache déjà rempli)

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 7.3: Générer le graphique d'évolution du cours côté serveur

En tant qu'utilisateur authentifié,
je veux recevoir le graphique d'évolution du cours avec l'historique,
afin de voir d'un coup d'œil la tendance d'une valeur sur la période.

**Exigences :** FR-009-05, FR-009-06, FR-029-08, NFR-PERF-01, AR-92
**Scénarios golden master :** aucun
**Prérequis :** Story 7.2.

**Acceptance Criteria:**

**Given** l'adaptateur simulé et un historique non vide
**When** j'appelle `GET /api/bourse/historique?symbole=AI.PA&periode=1M`
**Then** la réponse porte, en plus des points, un graphique vectoriel SVG de 800 × 300 avec 5 lignes horizontales de grille et 5 dates régulièrement espacées en abscisse au format MM-JJ

**Given** un historique dont la dernière clôture est supérieure ou égale à la première
**When** le graphique est généré
**Then** la courbe est tracée en vert `#2e7d32`

**Given** un historique dont la dernière clôture est strictement inférieure à la première
**When** le graphique est généré
**Then** la courbe est tracée en rouge `#E3000F` et le SVG ne contient pas `#2e7d32`

**Given** un historique vide
**When** j'appelle `GET /api/bourse/historique`
**Then** la réponse ne porte aucun graphique

**Given** l'adaptateur simulé, en local
**When** le test de performance appelle 20 fois `GET /api/bourse/historique` (périodes 1W, 1M, 1Y, 5Y)
**Then** le 95e centile du temps de réponse est inférieur à 3 000 ms (NFR-PERF-01, page bourse)

**Given** la story terminée
**When** je lance `mvn -B -q -f services/bourse/pom.xml test -Dgroups='<FR>'` pour chacune de FR-009-05, FR-009-06, FR-029-08
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 7.4: Afficher l'écran Bourse

En tant qu'utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN),
je veux un écran Bourse qui présente la watchlist, les indicateurs, le graphique et les dernières séances de la valeur choisie, et qui permet d'ajouter ou de retirer une valeur,
afin de suivre les marchés depuis mon espace bancaire.

**Exigences :** FR-009-01, FR-009-02, FR-009-03, FR-009-04, FR-009-05, FR-009-06, FR-009-07, FR-009-09, FR-009-10, FR-009-11, FR-009-12, FR-009-13, FR-029-03, FR-029-06, FR-029-07, FR-029-08, FR-029-09, FR-029-10, FR-029-11, FR-029-12, FR-029-13, FR-029-14, NFR-UI-01, NFR-UI-02, NFR-INT-02, AR-76, AR-78, AR-88, UX-DR16, UX-DR64, UX-DR65, UX-DR76, UX-DR78, UX-DR94, UX-DR99, UX-DR104, UX-DR108, UX-DR125, UX-DR151, UX-DR161
**Scénarios golden master :** SC-009-01
**Prérequis :** Stories 7.1, 7.2 et 7.3 ; A-029-2 (symbole ou nom trop long bloqué par la limite du champ, sans message) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** un utilisateur CLIENT, CONSEILLER ou ADMIN connecté
**When** il ouvre `/bourse` (lien « Bourse » du header)
**Then** l'écran s'affiche pour chacun avec le même contenu, et un utilisateur non authentifié est redirigé vers `/login`

**Given** la watchlist de référence
**When** l'écran s'ouvre
**Then** la période "1 Mois" (1M) est présélectionnée, la première valeur (AI.PA) est sélectionnée et son historique est chargé avant l'affichage (zone de contenu en `aria-busy="true"` jusqu'aux données)
**And** les textes "Valeur", "BNP.PA", "AI.PA", "Variation sur la période" et "Performance %" sont présents

**Given** un historique chargé
**When** l'écran l'affiche
**Then** il montre "{nom} — {symbole}", le cours actuel (dernière clôture), "Variation sur la période" (dernière − première clôture, préfixée "+" si positive), "Performance %" ((dernière − première) / première × 100) et "Marché"
**And** si la performance est ≥ 0, indicateurs en vert `#2e7d32` préfixés "▲ +" ; si elle est strictement négative, en rouge `#E3000F` préfixés "▼ "

**Given** un historique chargé
**When** l'écran l'affiche
**Then** sous "📈 Évolution du cours" figure le graphique SVG du service dans une `card`, `role="img"`, avec le badge "1 semaine (données journalières)", "1 mois (données journalières)", "1 an (données hebdomadaires)" ou "5 ans (données hebdomadaires)" selon la période
**And** le nom accessible du graphique est « Évolution du cours de {symbole} sur {periode} » (clé `bourse.graphique.nom`, `textes-nouveaux.md`, lot « Stories », formé sur le titre legacy « 📈 Évolution du cours »)

**Given** un historique chargé
**When** l'écran l'affiche
**Then** sous "📋 Dernières séances" figurent au plus les 10 points les plus récents, du plus récent au plus ancien, colonnes "Date", "Ouverture", "Haut", "Bas", "Clôture" (2 décimales), "Volume", "Var. J" ((clôture − ouverture) / ouverture × 100, préfixée "+" si clôture ≥ ouverture), ligne verte si clôture ≥ ouverture, rouge sinon

**Given** l'écran affiché
**When** je change "Valeur" ou "Période" ("1 Semaine", "1 Mois", "1 An", "5 Ans")
**Then** seul l'historique est récupéré et réaffiché, sans rechargement de la page ni bouton de validation

**Given** une valeur sans données (liste de points vide renvoyée par le service)
**When** elle est sélectionnée pour la période 1M
**Then** le graphique et le tableau sont masqués et le message "⚠️ Aucune donnée reçue pour « {SYMBOLE} » sur 1 mois (données journalières). Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur." s'affiche ; le reste de l'écran reste utilisable

**Given** la watchlist de référence
**When** l'écran l'affiche
**Then** elle apparaît sous "⭐ Ma Watchlist (9 valeurs)" avec les colonnes "Symbole", "Nom", "Marché", "Devise", "Suppr.", triée par marché puis symbole, et "(9 valeurs)" s'affiche à côté du sélecteur

**Given** l'écran affiché
**When** je clique sur "+ Ajouter une valeur"
**Then** le formulaire "➕ Ajouter une valeur à la watchlist" s'ouvre dans la page avec "Symbole *" (exemple "ex: AMZN", aide "Symbole Alpha Vantage", 20 caractères au plus), "Nom *" (exemple "ex: Amazon", 100 caractères au plus), "Marché" ("Euronext Paris" par défaut, "NASDAQ", "NYSE", "London SE" pour la valeur LSE, "Autre"), "Devise" ("EUR" par défaut, "USD", "GBP"), "Ajouter" et "Annuler"
**And** "Annuler" referme le formulaire et rend le focus au bouton "+ Ajouter une valeur"

**Given** le formulaire ouvert
**When** je soumets un symbole vide, puis un nom vide, puis le symbole "bnp.pa"
**Then** le formulaire reste ouvert et affiche respectivement "Le symbole est obligatoire (ex: AMZN, SG.PA)", "Le nom de la valeur est obligatoire" et "Le symbole « BNP.PA » existe déjà dans votre watchlist", sans ajout à la watchlist

**Given** le formulaire ouvert
**When** je saisis un symbole de 25 caractères et un nom de 120 caractères
**Then** les champs n'acceptent que 20 et 100 caractères, sans message

**Given** le formulaire ouvert
**When** je soumets "sg.pa", "Société Générale", "Euronext Paris", "EUR"
**Then** "Valeur ajoutée !" s'affiche, le formulaire se ferme, SG.PA est sélectionnée et son historique chargé

**Given** la valeur sélectionnée AI.PA
**When** je clique sur son bouton "✕" (infobulle "Supprimer Air Liquide de la watchlist")
**Then** elle est supprimée sans confirmation, la première valeur restante est sélectionnée et son historique chargé
**And** quand la dernière valeur est supprimée, la sélection est effacée et le graphique et le tableau sont masqués

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-009-01`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t '<FR>'` pour chacune de FR-009-01, FR-009-02, FR-009-03, FR-009-04, FR-009-05, FR-009-06, FR-009-07, FR-009-09, FR-009-10, FR-009-11, FR-009-12, FR-009-13, FR-029-03, FR-029-06, FR-029-07, FR-029-08, FR-029-09, FR-029-10, FR-029-11, FR-029-12, FR-029-13, FR-029-14
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 7.5: Ouvrir le service open-banking-conseiller et exposer le catalogue des produits et le journal des traitements

En tant qu'utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN),
je veux lire par l'API les produits actifs du catalogue et les dernières exécutions des traitements nocturnes,
afin que l'écran Produits puisse les afficher.

**Exigences :** FR-010-01, FR-011-02, FR-011-03, FR-011-07, FR-011-09, NFR-PAR-04, NFR-BATCH-02, NFR-QG-01, NFR-QG-03, AR-17, AR-25, AR-26, AR-36, AR-46, AR-47, AR-57, AR-64, AR-71, AR-88
**Scénarios golden master :** aucun
**Prérequis :** Epic 2 terminé (vérification du jeton, format d'erreur) ; Epic 4 terminé (traitements nocturnes du cœur bancaire et table `BATCH_JOB_LOGS`).

**Acceptance Criteria:**

**Given** la story terminée
**When** je lance `ls services/`
**Then** `open-banking-conseiller` figure dans la liste, avec `pom.xml`, `Dockerfile`, `reference-data/reference-data.sql` et les paquets `fr/nextbanking/openbankingconseiller/{domain,application,adapter/in,adapter/out,infrastructure}`
**And** `compose.yaml` déclare `open-banking-conseiller` (port de développement 8088) et `postgres-open-banking-conseiller` (volume `pgdata-open-banking-conseiller`), `compose.rejeu.yaml` lui applique le profil `rejeu`, et `/api/open-banking-conseiller/**` est renvoyé vers le service par le proxy Vite et le conteneur web

**Given** le service open-banking-conseiller
**When** je lance `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dtest='*ArchitectureTest'`
**Then** le code de sortie est 0

**Given** les données de référence chargées (produits du legacy : 13 produits, 4 d'épargne, 4 de crédit, 5 d'investissement)
**When** un utilisateur CLIENT, CONSEILLER ou ADMIN appelle `GET /api/open-banking-conseiller/produits`
**Then** la réponse est 200, identique pour les trois rôles, avec les seuls produits actifs, répartis en épargne, crédit et investissement, chaque groupe trié par nom en ordre alphabétique, dont "CEL - Compte Epargne Logement" et "Livret A" en épargne
**And** les taux et rendements sont transmis en décimal exact (ex. "0.0300"), sans mise en forme

**Given** les données de référence
**When** j'appelle `GET /api/open-banking-conseiller/produits?type=EPARGNE` (puis `CREDIT`, `INVESTISSEMENT`)
**Then** seuls les produits actifs de ce type sont renvoyés

**Given** un produit désactivé ajouté aux données de test
**When** j'appelle `GET /api/open-banking-conseiller/produits`
**Then** il n'apparaît dans aucun groupe

**Given** le domaine du catalogue
**When** un produit d'épargne est créé sans préciser la garantie du capital, et un produit de crédit sans préciser l'assurance
**Then** le produit d'épargne a la garantie du capital à vrai et le produit de crédit l'assurance requise à faux

**Given** plus de 50 exécutions dans `BATCH_JOB_LOGS` du cœur bancaire
**When** un utilisateur CLIENT, CONSEILLER ou ADMIN appelle `GET /api/coeur-bancaire/traitements?limite=50`
**Then** la réponse est 200 avec au plus les 50 dernières exécutions, de la plus récente à la plus ancienne (heure de début), avec job, début, fin, enregistrements, statut (SUCCESS, FAILED ou PARTIAL) et détails

**Given** aucun jeton valide
**When** j'appelle `GET /api/open-banking-conseiller/produits` ou `GET /api/coeur-bancaire/traitements?limite=50`
**Then** la réponse est 401 au format `{"message": …}`

**Given** la story terminée
**When** je lance `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='<FR>'` pour chacune de FR-010-01, FR-011-02, FR-011-03, FR-011-07, et `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-011-09'`
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 7.6: Afficher l'écran Produits

En tant qu'utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN),
je veux consulter le catalogue des produits et l'historique des traitements nocturnes,
afin de connaître l'offre de la banque et l'état des traitements de nuit.

**Exigences :** FR-010-01, FR-011-01, FR-011-02, FR-011-03, FR-011-04, FR-011-05, FR-011-06, FR-011-08, FR-011-09, NFR-UI-01, NFR-UI-02, NFR-PERF-01, AR-47, AR-76, AR-78, AR-88, UX-DR76, UX-DR78, UX-DR99, UX-DR127
**Scénarios golden master :** SC-011-01
**Prérequis :** Story 7.5 ; A-011-1 (message « aucun produit » par type de produit vide) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** un utilisateur CLIENT, CONSEILLER ou ADMIN connecté
**When** il ouvre `/produits` (lien « Produits » du header)
**Then** l'écran titré "Catalogue des produits" s'affiche avec le même contenu pour les trois rôles, sans aucun champ de saisie, et un utilisateur non authentifié est redirigé vers `/login`
**And** le catalogue et l'historique sont demandés en parallèle aux deux services (jamais en cascade)

**Given** l'écran affiché
**When** je clique sur "Rafraichir"
**Then** le catalogue et l'historique sont rechargés et l'écran reste `/produits`

**Given** les données de référence
**When** l'écran s'affiche
**Then** les sections "Produits d epargne" ("Nom", "Description", "Taux", "Depot min", "Capital garanti", "Duree max"), "Produits de credit" ("Nom", "Description", "Taux", "Duree max", "LTV max", "Assurance requise") et "Produits d investissement" ("Nom", "Description", "Secteur", "Niveau de risque", "Rendement attendu") listent les produits actifs triés par nom, dont "CEL - Compte Epargne Logement" et "Livret A"
**And** les booléens s'affichent "Oui" ou "Non" et une durée renseignée "{n} mois"

**Given** un produit dont un champ facultatif est vide (taux, dépôt min, LTV max, secteur, rendement, durée max d'un crédit)
**When** il s'affiche
**Then** la cellule affiche "—", sauf la durée max vide d'un produit d'épargne qui affiche "Illimite"

**Given** un taux de 0,0300 et un rendement de 0,0650
**When** ils s'affichent
**Then** la colonne "Taux" montre "3,00 %" et la colonne "Rendement attendu" "6,50 %"
**And** le test `[BUG-039]` échoue si l'affichage rend "0.03", "0,03 %" ou toute forme non multipliée par 100

**Given** des produits d'investissement de score 1 à 5, et un score 0 ou 6
**When** ils s'affichent
**Then** "Niveau de risque" vaut "Tres faible (1/5)", "Faible (2/5)", "Moyen (3/5)", "Eleve (4/5)", "Tres eleve (5/5)", et "N/A" pour un score hors 1-5

**Given** un type sans produit actif
**When** l'écran s'affiche
**Then** la section affiche "Aucun produit d epargne disponible", "Aucun produit de credit disponible" ou "Aucun produit d investissement disponible", même si les autres types ont des produits

**Given** des exécutions de traitements enregistrées
**When** l'écran s'affiche
**Then** après le séparateur "Traitements nocturnes", la section "Historique des traitements nocturnes" (sans « (EJB @Schedule) ») liste au plus 50 exécutions, de la plus récente à la plus ancienne, colonnes "Job", "Debut", "Fin", "Duree (ms)", "Enregistrements", "Statut", "Details", la durée valant fin − début en millisecondes et 0 si l'une manque

**Given** aucune exécution enregistrée
**When** l'écran s'affiche
**Then** la section affiche "Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00"

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-011-01`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t '<tag>'` pour chacun de FR-010-01, FR-011-01, FR-011-02, FR-011-03, FR-011-04, FR-011-05, FR-011-06, FR-011-08, FR-011-09, BUG-039
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 7.7: Raccorder Azure OpenAI derrière un port simulé et exposer l'échange avec Sophie

En tant qu'utilisateur authentifié,
je veux que mes questions à Sophie soient transmises au service d'IA avec la conversation en cours, et qu'une panne de ce service me soit annoncée sans code technique,
afin d'obtenir une réponse ou de savoir que je dois réessayer.

**Exigences :** FR-010-05, FR-010-06, FR-010-07, FR-010-08, FR-010-09, NFR-F010, NFR-INT-01, NFR-INT-02, NFR-INT-03, NFR-SEC-04, NFR-SEC-06, NFR-PAR-02, AR-27, AR-46, AR-60, AR-61, AR-62, AR-75
**Scénarios golden master :** aucun (SC-010-02 exclu du golden master, remplacé par le test de FR-010-09 avec l'adaptateur simulé en mode refus 401/403, AR-75)
**Prérequis :** Stories 7.2 (bibliothèque de coupe-circuit retenue pour AR-102, réutilisée ici) et 7.5 (service open-banking-conseiller) ; A-010-1 (« erreur d'authentification » du service d'IA couvre 401 et 403) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** un contexte du service open-banking-conseiller démarré sans aucune propriété `nb.fournisseurs.*`
**When** le test de configuration s'exécute
**Then** le bean actif du port de conversation est l'adaptateur simulé `AzureOpenAiSimule` (dans `adapter.out.fournisseur`), qui renvoie une réponse fixe sans appel réseau
**And** `! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' services/*/src/main/resources compose*.yaml` sort en code 0

**Given** l'adaptateur simulé nominal
**When** j'envoie `POST /api/open-banking-conseiller/chat/messages` avec un message et un historique vide
**Then** la réponse est 200 avec la réponse de Sophie
**And** le service n'enregistre aucun historique de conversation (aucune table de chat)

**Given** le mode réel pointé vers un serveur de bouchon local
**When** j'envoie un premier message avec un historique vide, puis un second avec l'historique des échanges précédents
**Then** la première requête reçue par le bouchon contient la consigne système et le seul message courant ; la seconde contient la consigne, les échanges précédents dans l'ordre (sans le message de bienvenue) puis le message courant
**And** chaque requête porte une limite de 500 tokens et une température de 0,7

**Given** la consigne système de Sophie
**When** le test de FR-010-07 la lit
**Then** elle définit la persona Sophie et ses six domaines d'expertise, interdit le conseil d'investissement ferme, impose la mention des risques (MiFID II), la réponse exclusivement en français et la concision en 3 à 5 phrases sauf demande explicite
**And** son libellé exact est celui de la clé `conseiller.consigne` (`textes-nouveaux.md`, lot « Stories »), à valider par le métier avant de démarrer

**Given** `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_DEPLOYMENT`, `AZURE_OPENAI_API_VERSION` définis en variable d'environnement et en propriété système
**When** le service démarre en mode réel
**Then** la variable d'environnement l'emporte, puis la propriété système, puis la valeur par défaut du legacy
**And** l'adaptateur réel applique un délai de connexion de 15 secondes et un délai de lecture de 30 secondes, et passe par le coupe-circuit retenu en 7.2 (réglages sous `nb.fournisseurs.azure-openai.*`)

**Given** le mode réel sans `AZURE_OPENAI_API_KEY`
**When** j'envoie un message
**Then** le bouchon ne reçoit aucune requête et la réponse porte le message d'indisponibilité « Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence. »
**And** `git grep -n 'AZURE_OPENAI_API_KEY'` ne montre aucune valeur de clé dans les fichiers versionnés

**Given** l'adaptateur simulé en mode « refus d'authentification » (401, puis 403)
**When** j'envoie `POST /api/open-banking-conseiller/chat/messages` avec « Quel est mon solde courant ? »
**Then** la réponse est 503 avec `{"message":"Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence."}`, qui ne contient ni « HTTP 401 » ni « 401 » (ni « 403 »)

**Given** le mode réel et un bouchon qui répond 500
**When** j'envoie un message
**Then** la réponse est 502 avec `{"message":"Je suis momentanement indisponible (HTTP 500). Veuillez reessayer."}`

**Given** le mode réel et un bouchon qui ne répond pas dans le délai, ou dont la réponse est illisible, ou un coupe-circuit ouvert
**When** j'envoie un message
**Then** la réponse est 503 avec `{"message":"Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence."}`, sans pile d'appels

**Given** une valeur par défaut de clé ajoutée volontairement dans `services/open-banking-conseiller/src/main/resources/application.yml`
**When** je lance `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-010-08'`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='<tag>'` pour chacun de FR-010-05, FR-010-06, FR-010-07, FR-010-08, FR-010-09, BUG-038
**Then** chaque commande sort en code 0, sans clé définie et sans accès réseau

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 7.8: Dialoguer avec Sophie sur l'écran Mon Conseiller

En tant qu'utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN),
je veux un écran de conversation avec Sophie, avec actions rapides et nouvelle conversation,
afin de poser mes questions sur mes comptes, mes placements ou mes projets.

**Exigences :** FR-010-01, FR-010-02, FR-010-03, FR-010-04, FR-010-05, FR-010-06, FR-010-09, FR-010-10, FR-010-11, NFR-UI-01, NFR-UI-02, NFR-SEC-04, NFR-PAR-02, AR-62, AR-76, AR-78, AR-88, UX-DR17, UX-DR61, UX-DR62, UX-DR76, UX-DR78, UX-DR87, UX-DR104, UX-DR126
**Scénarios golden master :** SC-010-01
**Prérequis :** Story 7.7 ; A-010-1 (« erreur d'authentification » couvre 401 et 403), A-010-2 (champ du chat non vidé après l'envoi, comme le legacy) et A-010-3 (actions rapides masquées pour la conversation en cours, réaffichées par « Nouvelle conversation ») — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** l'utilisateur `user1` (CLIENT, prénom « Jean ») connecté
**When** il ouvre `/mon-conseiller` (lien « Mon Conseiller » du header)
**Then** l'écran affiche « Sophie — Conseillère Demo Bank », « En ligne · Répond en quelques secondes » et le message de Sophie « Bonjour Jean ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ? »
**And** un CONSEILLER ou un ADMIN y accède aussi ; sans client associé, le message commence par « Bonjour cher client ! » ; un utilisateur non authentifié est redirigé vers `/login`

**Given** une conversation avec plusieurs échanges
**When** je quitte l'écran puis l'ouvre à nouveau
**Then** la conversation ne contient que le message de bienvenue (historique tenu par le front, par conversation)

**Given** une conversation neuve
**When** l'écran s'affiche
**Then** quatre boutons `button-ghost` sont visibles : « Optimiser mon epargne », « Projet immobilier », « Investir en bourse », « Preparer ma retraite »
**And** un clic sur « Projet immobilier » envoie « Je souhaite acheter un appartement, comment preparer mon dossier ? » comme message de l'utilisateur et masque les quatre boutons pour la conversation en cours (de même « Je voudrais optimiser mon epargne, que me conseillez-vous ? », « Je veux commencer a investir en bourse, par ou commencer ? », « Comment preparer ma retraite efficacement ? »)

**Given** la zone de saisie « Posez votre question a Sophie... » (`chat-saisie`)
**When** j'envoie un message non vide par « Envoyer » (`chat-envoyer`)
**Then** le message est ajouté (`chat-message-utilisateur`), le service est appelé avec l'historique des échanges précédents, hors message de bienvenue et message en cours (historique vide au premier message), et la réponse s'ajoute comme bulle de Sophie (`card-muted`, annoncée `aria-live="polite"`)
**And** le champ n'est pas vidé après l'envoi (A-010-2)

**Given** la zone de saisie vide ou ne contenant que des blancs
**When** je clique sur « Envoyer » ou appuie sur Entrée
**Then** aucun message n'est ajouté, aucun appel n'est émis et aucune erreur n'est affichée

**Given** un envoi en cours
**When** la réponse n'est pas encore arrivée
**Then** « Envoyer » est désactivé et affiche « ... », puis est rétabli à la fin ; Entrée sans Maj envoie, Maj+Entrée insère un saut de ligne ; à la réponse, la liste défile jusqu'au dernier message

**Given** le service qui répond 503 avec `{"message":"Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence."}` (adaptateur simulé en refus 401)
**When** j'envoie « Quel est mon solde courant ? » (ou une action rapide)
**Then** la conversation affiche ce message de l'utilisateur, suivi d'une bulle de Sophie contenant « Je suis momentanement indisponible » et ne contenant ni « HTTP 401 » ni « 401 », sans zone d'erreur distincte
**And** sur une réponse 502 `{"message":"Je suis momentanement indisponible (HTTP 500). Veuillez reessayer."}`, la bulle de Sophie reprend ce texte

**Given** une conversation avec des échanges
**When** je clique sur « Nouvelle conversation »
**Then** tous les messages sont effacés, un nouveau message de bienvenue personnalisé s'affiche et les quatre actions rapides réapparaissent

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-010-01`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t '<tag>'` pour chacun de FR-010-01, FR-010-02, FR-010-03, FR-010-04, FR-010-05, FR-010-06, FR-010-09, FR-010-10, FR-010-11, BUG-038
**Then** chaque commande sort en code 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

## Epic 8: Agréger ses comptes d'autres banques (Powens)

À la fin de l'epic, tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) ouvre « Comptes Tiers », connecte une banque externe par la page web de Powens, revient dans l'application par le retour public de Powens, voit son patrimoine total, ses comptes externes et les transactions du compte sélectionné, rafraîchit ou déconnecte ses comptes ; le tableau de bord affiche le patrimoine total Demo Bank + comptes externes. Powens est appelé derrière un port du service `open-banking-conseiller`, avec un adaptateur simulé actif par défaut (connecteurs fixes, modes d'erreur) ; l'état Powens est conservé en base par client et le retour public retrouve le client par un `state` signé à usage unique. Les retours en erreur, le solde absent et l'indisponibilité de Powens sont signalés à l'utilisateur (BUG-040, BUG-042, BUG-043, BUG-044, BUG-089 corrigés).

**Contrôle de fin d'epic (à la charge de l'humain) :** lire, dans `reports/golden-master/textes-non-trouves.md`, la section du scénario d'écran de l'epic (SC-012-01) ; lancer `scripts/golden-master/verifier-scenario.sh SC-012-01`. SC-014-01 est un scénario HTTP, sans section dans ce rapport ni passage par ce script : il est couvert par le test d'intégration de la story 8.2. Comme la story 8.5 modifie le tableau de bord, relancer aussi `scripts/golden-master/verifier-scenario.sh` pour SC-001-04, SC-001-05, SC-002-01 et SC-002-02 et relire leurs sections.

### Story 8.1: Raccorder Powens derrière un port et lancer la connexion à une banque

En tant qu'utilisateur authentifié,
je veux que l'application crée ma connexion Powens et me donne l'adresse de la page web de Powens,
afin de pouvoir connecter une banque externe sans que l'application dépende du réseau en local, dans les tests et dans les contrôles.

**Exigences :** FR-028-01, FR-028-02, FR-028-12, FR-012-17, FR-012-05 (partie service), NFR-F012, NFR-F028, NFR-INT-01, NFR-INT-02, NFR-INT-03, NFR-SEC-04, NFR-SEC-06, AR-25, AR-27, AR-36, AR-47, AR-60, AR-61, AR-62 (émission du `state`), AR-88, AR-99, AR-102
**Scénarios golden master :** aucun
**Prérequis :** service `open-banking-conseiller` et sa vérification du jeton JWT (`SecuriteIT`) livrés par l'epic 7 (chat conseiller, catalogue des produits) ; bibliothèque de coupe-circuit et de cache choisie et vérifiée dans `versions-verifiees.md` par la première story de fournisseur de l'epic 7 (AR-102), réutilisée ici sans nouveau choix ; RP-10 (valeur de production de `POWENS_CONNECTOR_ID`) reste hors démonstrateur : la valeur par défaut 59 suffit.

**Acceptance Criteria:**

**Given** le service `open-banking-conseiller`
**When** je lis son code
**Then** Powens est appelé derrière un port sortant de `application.port.out` (création de l'utilisateur anonyme, code temporaire, adresse de la page web, lecture des comptes, lecture des transactions), avec deux adaptateurs dans `adapter.out.fournisseur` : `PowensSimule` (aucun appel réseau, réponses fixes) et un adaptateur réel
**And** seules les classes de `adapter.out.fournisseur` dépendent d'un client HTTP (`ClientsHttpArchitectureTest` vert)

**Given** un contexte Spring démarré sans aucune propriété `nb.fournisseurs.*`
**When** le test de configuration s'exécute
**Then** le bean actif du port Powens est `PowensSimule`
**And** `! grep -rnE 'mode *[:=] *reel|_MODE *[:=] *reel' services/*/src/main/resources compose*.yaml` sort en code 0

**Given** l'adaptateur simulé
**When** un test l'interroge
**Then** il expose une liste fixe de connecteurs, dont « BNP Paribas » et « Credit Agricole », des comptes et des transactions fixes, et des modes d'erreur sélectionnables par les tests : refus de création de l'utilisateur, réponse sans jeton, réponse 5xx (dont 500 et 503) aux comptes, réponse 4xx/5xx (dont 404 et 500) aux transactions, compte sans solde, compte au solde nul, aucun compte (AR-61)

**Given** la configuration `nb.fournisseurs.powens.*`
**When** aucune valeur n'est fournie
**Then** les valeurs par défaut sont le domaine `demi-sandbox.biapi.pro`, l'identifiant client `99961298` et les banques proposées `59` ; l'adresse de retour et le secret client sont des paramètres de déploiement, et le secret n'a aucune valeur dans le code ni dans un fichier versionné (NFR-SEC-06)
**And** l'adaptateur réel applique un délai de 15 secondes à l'établissement de la connexion et un délai de 15 secondes à la lecture, et chaque appel réel passe par le coupe-circuit choisi en epic 7 ; un test de l'adaptateur réel contre un serveur simulé local au test (sans réseau externe) vérifie ces deux délais et qu'un coupe-circuit ouvert produit une erreur d'indisponibilité, pas une liste vide

**Given** un utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN) sans connexion Powens
**When** il appelle `POST /api/open-banking-conseiller/powens/connexion`
**Then** le service crée un utilisateur Powens anonyme avec l'identifiant et le secret client, enregistre le jeton permanent dans l'état Powens du client (base `open-banking-conseiller`, clé `sub` du JWT) avant de construire l'adresse, obtient un code temporaire avec ce jeton, émet un `state` signé à usage unique lié au client, et répond 200 avec l'adresse de la page web
**And** avec l'adaptateur réel, l'adresse porte le domaine, `client_id`, l'adresse de retour encodée, le code temporaire encodé, les banques proposées et le `state` (test unitaire du constructeur d'adresse) ; avec `PowensSimule`, l'adresse est celle du retour `GET /api/open-banking-conseiller/powens/retour` avec un `connection_id` fixe et le `state`, ce qui permet de parcourir la connexion en local sans réseau
**And** tous les appels à Powens autres que la création de l'utilisateur s'authentifient avec le jeton permanent

**Given** un `PowensSimule` en mode « refus de création » puis en mode « réponse sans jeton »
**When** l'utilisateur appelle `POST /api/open-banking-conseiller/powens/connexion`
**Then** la réponse est 503 avec le corps `{"message": "Impossible d initialiser la connexion Powens."}`, sans adresse, et l'état Powens du client reste non connecté

**Given** un utilisateur authentifié
**When** il appelle `GET /api/open-banking-conseiller/powens` sans avoir jamais connecté de banque
**Then** la réponse est 200 et décrit l'état non connecté, sans compte, sans message
**And** aucune réponse de l'API ne contient le jeton Powens ni le secret client, et les journaux capturés pendant la connexion (`OutputCaptureExtension`) ne contiennent ni le jeton ni le secret (NFR-F012, NFR-F028)

**Given** une requête sans jeton JWT
**When** elle appelle `POST /api/open-banking-conseiller/powens/connexion` ou `GET /api/open-banking-conseiller/powens`
**Then** la réponse est 401 (`SecuriteIT` du service étendu à ces deux chemins), et aucun appel à Powens n'est émis

**Given** le mode réel demandé (`nb.fournisseurs.powens.mode=reel`) sans secret client fourni
**When** le contexte démarre
**Then** le démarrage échoue (configuration validée au démarrage), et le test qui le prouve est vert

**Given** la story terminée
**When** je lance `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-028-01,FR-028-02,FR-028-12,FR-012-17,FR-012-05'`
**Then** le code de sortie est 0
**And** pour chacun de ces cinq identifiants, `grep -rl '@Tag("<identifiant>")' services/open-banking-conseiller/src/test` renvoie au moins un fichier

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 8.2: Traiter le retour public de Powens

En tant qu'utilisateur qui revient de la page web de Powens,
je veux que l'application traite le résultat de ma connexion et me ramène toujours sur « Comptes Tiers »,
afin de voir mes comptes externes ou la raison de l'échec, même si mon navigateur n'envoie pas mon jeton au retour.

**Exigences :** FR-014-01, FR-014-02, FR-014-03, FR-014-04, FR-014-05, FR-014-06, FR-014-07, FR-014-08, FR-012-02, FR-012-06, FR-012-07 (partie service), FR-012-08, FR-012-09 (partie service), FR-028-03, FR-028-04, FR-028-05, FR-028-06, FR-028-11 (lecture des comptes), FR-030-01 (chemin public exact), NFR-PAR-02, AR-36, AR-62, AR-76, UX-DR129
**Scénarios golden master :** SC-014-01
**Prérequis :** story 8.1 ; A-014-1 (retour Powens avec seul `code` traité comme incomplet) confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; A-012-1 (texte du retour Powens en erreur), A-012-3 (message d'indisponibilité de Powens) et A-012-4 (message après échec de chargement des comptes) confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; A-030-1 (exemption sur le chemin exact) déjà reprise par l'architecture (AD-16, liste fermée des chemins publics).

**Acceptance Criteria:**

**Given** aucune session ni aucun jeton
**When** je lance `GET /api/open-banking-conseiller/powens/retour` sans paramètre, sans suivre la redirection
**Then** la réponse est 302, son en-tête `Location` désigne `/comptes-tiers`, sans corps (statut et corps de `docs/retro/golden-master/derived/FEAT-014/SC-014-01/expected-response.json` : `status_code` 302, `body` null), et aucun appel à Powens n'est émis (test d'intégration du service, tagué `FR-014-01`)

**Given** la liste fermée des chemins publics (AD-16)
**When** une requête sans jeton vise un chemin voisin du retour (`/api/open-banking-conseiller/powens/retour/x`, `/api/open-banking-conseiller/powens/retour-x`, `/api/open-banking-conseiller/powens`)
**Then** la réponse est 401 : seul le chemin exact `GET /api/open-banking-conseiller/powens/retour` est public (FR-030-01)

**Given** un `state` absent, mal signé, ou déjà utilisé une première fois
**When** le retour est appelé avec ce `state` et un `connection_id`
**Then** la réponse est 302 vers `/comptes-tiers` sans aucun effet : aucun appel à Powens, état Powens de chaque client inchangé (AR-62)

**Given** un `state` valide émis par `POST /api/open-banking-conseiller/powens/connexion`
**When** le retour porte `error=user_cancelled` (avec ou sans `connection_id` ni `code`)
**Then** seul le cas `error` est traité : l'état Powens du client est remis à zéro (jeton, comptes, compte sélectionné, transactions, indicateur de connexion, message), puis le message « La connexion a votre banque n a pas abouti (user_cancelled). Veuillez recommencer la connexion. » est posé après cette remise à zéro, et la réponse est 302 vers `/comptes-tiers`
**And** `GET /api/open-banking-conseiller/powens` renvoie ensuite l'état non connecté et un message qui contient « user_cancelled » (tests tagués `BUG-040` et `BUG-044`)

**Given** un `state` valide et un `PowensSimule` nominal
**When** le retour porte `connection_id`
**Then** les comptes sont lus aussitôt, sans aucune attente imposée, la connexion est marquée établie, le premier compte est sélectionné et ses transactions sont chargées avec une demande d'au plus 20 transactions, et la réponse est 302 vers `/comptes-tiers`
**And** avec un compte simulé portant 25 transactions, au plus 20 sont enregistrées dans l'état (FR-014-08)

**Given** un `state` valide et un `PowensSimule` qui ne renvoie aucun compte
**When** le retour porte `connection_id`
**Then** la connexion est marquée établie et l'état porte le message « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. »

**Given** un `state` valide et un `PowensSimule` qui répond 503, puis 500, à la lecture des comptes
**When** le retour porte `connection_id`
**Then** l'état reste non connecté, porte le message « Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants. » et ne porte ni le message « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. » ni une liste de comptes vide présentée comme un résultat (A-012-4 ; test tagué `BUG-089`)

**Given** un `state` valide dont le client n'a plus de jeton Powens dans son état (jeton effacé directement dans la base par le test)
**When** le retour porte `connection_id`
**Then** le chargement est abandonné sans appel à Powens et l'état porte le message « Session expiree. Veuillez recommencer la connexion. »

**Given** un `state` valide
**When** le retour ne porte que `code=abc`
**Then** le chargement des comptes n'est pas lancé et l'état porte le message « Callback Powens incomplet. Parametres recus: [code] » ; l'état reste non connecté
**And** la liste « Parametres recus » ne nomme que les paramètres reçus autres que `state` : un retour portant `state` et `code` donne exactement « Callback Powens incomplet. Parametres recus: [code] » (FR-014-04 ; décision du 2026-10-05) » et contredit FR-014-04 ; proposition : la liste ne nomme que les paramètres reçus autres que `state`]

**Given** un `state` valide
**When** le retour ne porte aucun autre paramètre, ou porte `error`, `connection_id` et `code` vides
**Then** un paramètre vide compte comme absent, l'état porte « Callback Powens incomplet. Parametres recus: [] » (avec la même règle de liste que le critère précédent) et la réponse est 302 vers `/comptes-tiers`

**Given** la story terminée
**When** je lance `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-014-01,FR-014-02,FR-014-03,FR-014-04,FR-014-05,FR-014-06,FR-014-07,FR-014-08,FR-012-02,FR-012-06,FR-012-07,FR-012-08,FR-012-09,FR-028-03,FR-028-04,FR-028-05,FR-028-06,BUG-040,BUG-044,BUG-089'`
**Then** le code de sortie est 0
**And** pour chacun de ces identifiants, `grep -rl '@Tag("<identifiant>")' services/open-banking-conseiller/src/test` renvoie au moins un fichier

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 8.3: Consulter, sélectionner, rafraîchir et déconnecter ses comptes externes côté service

En tant qu'utilisateur connecté à Powens,
je veux lire mes comptes externes et mon patrimoine total, choisir un compte, rafraîchir la liste et me déconnecter,
afin de suivre mes comptes d'autres banques sans me reconnecter, en distinguant un solde absent d'un solde nul et une panne de Powens d'une liste vide.

**Exigences :** FR-028-07, FR-028-08, FR-028-09, FR-028-10, FR-028-11, FR-012-10, FR-012-12 (partie service), FR-012-13 (partie service), FR-012-14 (partie service), FR-012-15 (partie service), FR-012-16 (partie service), FR-014-07, FR-014-08, NFR-SEC-03, NFR-INT-02, NFR-PAR-02, AR-47, AR-49, AR-62
**Scénarios golden master :** aucun
**Prérequis :** story 8.2 ; A-012-2 (« Solde indisponible ») et A-012-3 (message d'indisponibilité de Powens) confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** un client connecté à Powens (état écrit par le retour de la story 8.2)
**When** il appelle `GET /api/open-banking-conseiller/powens`
**Then** la réponse est 200 et porte, lus dans l'état en base et sans appel à Powens : l'indicateur de connexion, les comptes (nom, type brut, solde ou absence de solde, IBAN s'il est renvoyé), le compte sélectionné, ses transactions (date, libellé, montant), le message en cours et le patrimoine total des comptes externes, en chaîne décimale à deux décimales (`"120.50"`)

**Given** un `PowensSimule` qui renvoie un compte A sans solde (champ absent), un compte B au solde vide, un compte C au solde 0 et un compte D au solde 120,50
**When** les comptes sont lus
**Then** A et B sont marqués « solde absent », distincts de zéro, C vaut `"0.00"`, D vaut `"120.50"`, et le patrimoine total vaut `"120.50"` (test tagué `BUG-042`)

**Given** un client connecté
**When** il appelle `POST /api/open-banking-conseiller/powens/comptes/{compteId}/selection` pour l'un de ses comptes externes
**Then** le compte devient le compte sélectionné, au plus 20 transactions sont demandées à Powens, chaque transaction porte le libellé simplifié, ou le libellé d'origine si le simplifié est absent ou vide, et la réponse est 200 avec ces transactions

**Given** un client connecté
**When** il appelle la sélection avec un `compteId` absent de ses propres comptes externes (dont un compte d'un autre client)
**Then** la réponse est 404 au format `{"message"}`, sans appel à Powens, et son état est inchangé
**And** le corps est `{"message": "Ressource introuvable"}`

**Given** un `PowensSimule` qui répond 404, puis 500, à la lecture des transactions
**When** le client sélectionne un compte
**Then** la réponse est 503 avec `{"message": "Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants."}`, et non une liste de transactions vide (tests tagués `BUG-089` et `BUG-043`)

**Given** un client connecté sans compte sélectionné et un `PowensSimule` qui renvoie désormais deux comptes
**When** il appelle `POST /api/open-banking-conseiller/powens/rafraichissement`
**Then** la liste des comptes est relue auprès de Powens, le premier compte est sélectionné et ses transactions sont chargées ; si un compte était déjà sélectionné, il le reste
**And** sur une réponse 5xx de Powens au rafraîchissement, la réponse est 503 avec le message d'indisponibilité ci-dessus, et non une liste vide

**Given** un client connecté avec comptes, compte sélectionné, transactions et message
**When** il appelle `DELETE /api/open-banking-conseiller/powens/connexion`
**Then** la réponse est 204 et l'état Powens du client ne contient plus ni jeton, ni comptes, ni compte sélectionné, ni transactions, ni indicateur de connexion, ni message ; `GET /api/open-banking-conseiller/powens` renvoie l'état non connecté

**Given** une requête sans jeton JWT
**When** elle appelle la sélection, le rafraîchissement ou la déconnexion
**Then** la réponse est 401 et l'état Powens de chaque client est inchangé

**Given** la story terminée
**When** je lance `mvn -B -q -f services/open-banking-conseiller/pom.xml test -Dgroups='FR-028-07,FR-028-08,FR-028-09,FR-028-10,FR-028-11,FR-012-10,FR-014-07,FR-014-08,BUG-042,BUG-043,BUG-089'`
**Then** le code de sortie est 0
**And** pour chacun de ces identifiants, `grep -rl '@Tag("<identifiant>")' services/open-banking-conseiller/src/test` renvoie au moins un fichier

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 8.4: Afficher l'écran « Comptes Tiers »

En tant qu'utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN),
je veux un écran « Comptes Tiers » qui me propose de connecter une banque puis me montre mes comptes externes, leurs transactions et les messages de Powens,
afin d'agréger mes comptes d'autres banques et de comprendre ce qui se passe quand la connexion échoue.

**Exigences :** FR-012-01, FR-012-03, FR-012-04, FR-012-05, FR-012-07, FR-012-08, FR-012-09, FR-012-10, FR-012-11, FR-012-12, FR-012-13, FR-012-14, FR-012-15, FR-012-16, NFR-UI-01, NFR-UI-02, NFR-PAR-02, AR-76, AR-78, UX-DR74, UX-DR104, UX-DR128, UX-DR161
**Scénarios golden master :** SC-012-01
**Prérequis :** stories 8.1, 8.2 et 8.3 ; gabarit, navigation et routeur du front livrés par l'epic 2 ; `scripts/golden-master/verifier-scenario.sh` livré par l'epic 2 ; A-012-1 (texte du retour Powens en erreur), A-012-2 (« Solde indisponible »), A-012-3 (message d'indisponibilité de Powens), A-012-4 (message après échec de chargement des comptes) et A-012-5 (bouton « Rafraichir » exposé) confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 »).

**Acceptance Criteria:**

**Given** un visiteur non authentifié
**When** il demande `/comptes-tiers`
**Then** le routeur du front affiche `/login`
**And** un CLIENT, un CONSEILLER et un ADMIN authentifiés voient le même contenu de l'écran (test Vitest par rôle)

**Given** un utilisateur sans connexion Powens
**When** il ouvre `/comptes-tiers`
**Then** l'écran affiche « Agregez vos comptes bancaires », « Connectez vos comptes d autres banques pour avoir une vue complete de votre patrimoine financier. Connexion securisee via le protocole OAuth2 PSD2. », « BNP Paribas », « Credit Agricole », « La Banque Postale », « CIC », « LCL », « + 1800 banques » et le bouton « Connecter une banque » (`data-testid` `tiers-connecter-banque`)
**And** le bandeau technique « 🏦 Open Banking — Agregation de comptes via Powens API (PSD2) — OAuth2 depuis un EJB WildFly 14 » n'est pas affiché (B-29)

**Given** l'écran non connecté
**When** l'utilisateur actionne « Connecter une banque »
**Then** le front appelle `POST /api/open-banking-conseiller/powens/connexion` et envoie le navigateur à l'adresse renvoyée
**And** sur une réponse 503, l'écran affiche « Impossible d initialiser la connexion Powens. » dans `tiers-message-erreur`, reste non connecté et ne change pas d'adresse

**Given** un état connecté avec deux comptes dont le premier est sélectionné
**When** l'écran s'affiche
**Then** le bloc de connexion est masqué ; l'écran affiche « Connected via Powens », « Deconnecter » (`tiers-deconnecter`), « Rafraichir » (`tiers-rafraichir`) à côté de « Deconnecter », la carte « Patrimoine total (tous comptes) » avec « via Powens Open Banking API », « Mes comptes externes (2) », et « Transactions — {nom du premier compte} » avec les colonnes « Date », « Libelle », « Montant »

**Given** des comptes de types `checking`, `savings`, `card`, `loan`, `market`, absent et `autre`
**When** leurs cartes s'affichent (`tiers-compte-{compteId}`)
**Then** chacune porte « Banque externe », le nom, le type « Compte courant », « Epargne », « Carte », « Credit », « Bourse », « Compte » et « autre » respectivement, le solde à deux décimales avec séparateur de milliers au format du legacy, et l'IBAN seulement s'il est non vide

**Given** un compte sans solde, un compte au solde 0 et un compte au solde 120,50
**When** l'écran s'affiche
**Then** la carte du premier affiche « Solde indisponible » et non « 0,00 », la carte du deuxième affiche « 0,00 », et le patrimoine total affiché vaut 120,50 (test tagué `[BUG-042]`)

**Given** l'écran connecté
**When** l'utilisateur clique sur un autre compte
**Then** seule la section des transactions est mise à jour, sans rechargement de l'écran, avec au plus 20 lignes ; un montant strictement positif s'affiche en vert préfixé « + », un montant nul ou négatif en rouge sans « + » ; sans transaction, la section affiche « Aucune transaction disponible. »

**Given** une sélection de compte qui répond 503
**When** la section des transactions se met à jour
**Then** elle affiche « Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants. » et n'affiche pas « Aucune transaction disponible. » (test tagué `[BUG-043]`)

**Given** un état non connecté portant le message d'indisponibilité de Powens écrit au retour
**When** l'écran s'affiche
**Then** il affiche « Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants. » dans `tiers-message-erreur`, le bloc « Agregez vos comptes bancaires », et n'affiche ni « Connexion reussie mais aucun compte disponible encore » ni « Mes comptes externes (0) » (test tagué `[BUG-043]`)

**Given** un état non connecté portant le message « La connexion a votre banque n a pas abouti (user_cancelled). Veuillez recommencer la connexion. »
**When** l'écran s'affiche
**Then** il affiche le bloc « Agregez vos comptes bancaires » et ce message, qui contient « user_cancelled » (tests tagués `[BUG-040]` et `[BUG-044]`)
**And** les messages « Session expiree. Veuillez recommencer la connexion. », « Callback Powens incomplet. Parametres recus: [code] » et « Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes. » (ce dernier avec l'état connecté et « Mes comptes externes (0) ») s'affichent tels que renvoyés par l'état

**Given** l'écran connecté
**When** l'utilisateur actionne « Rafraichir », puis « Deconnecter »
**Then** « Rafraichir » appelle `POST /api/open-banking-conseiller/powens/rafraichissement` et met à jour les comptes sans reconnexion ; « Deconnecter » appelle `DELETE /api/open-banking-conseiller/powens/connexion` et l'écran revient au bloc « Agregez vos comptes bancaires », sans comptes, sans transactions ni message

**Given** l'écran, connecté ou non
**When** il s'affiche
**Then** aucun jeton Powens n'est affiché (NFR-F012) et le front n'écrit rien de l'état Powens dans `localStorage` ni dans `sessionStorage`

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-012-01`
**Then** le code de sortie est 0

**Given** la route `/comptes-tiers` retirée volontairement du routeur de `frontend/src`
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-012-01`
**Then** le code de sortie est non nul et la sortie nomme SC-012-01 ; la route remise, le code de sortie redevient 0

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t 'FR-012-01|FR-012-03|FR-012-04|FR-012-05|FR-012-07|FR-012-08|FR-012-09|FR-012-10|FR-012-11|FR-012-12|FR-012-13|FR-012-14|FR-012-15|FR-012-16|BUG-040|BUG-042|BUG-043|BUG-044'`
**Then** le code de sortie est 0
**And** pour chacun de ces identifiants, `grep -rl '\[<identifiant>\]' frontend/src` renvoie au moins un fichier de test

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 8.5: Afficher le patrimoine total avec les comptes externes sur le tableau de bord

En tant qu'utilisateur authentifié,
je veux voir sur mon tableau de bord le patrimoine total Demo Bank + comptes externes, ou une invitation à connecter mes autres banques,
afin d'avoir une vue complète de mon patrimoine dès l'accueil.

**Exigences :** FR-002-12, NFR-INT-02, NFR-PERF-01, AR-47, AR-49, AR-85, UX-DR77, UX-DR104
**Scénarios golden master :** aucun (le tableau de bord est comparé par SC-001-04, SC-001-05, SC-002-01 et SC-002-02, livrés par les epics 2 et 3 : ils doivent rester verts au contrôle statique)
**Prérequis :** story 8.3 (`GET /api/open-banking-conseiller/powens` avec le patrimoine total externe) et story 8.4 (route `/comptes-tiers`) ; tableau de bord et « Solde total Demo Bank » (FR-002-03) livrés par l'epic 3.

**Acceptance Criteria:**

**Given** un utilisateur connecté à Powens, au solde total Demo Bank de 1 000,00 et aux comptes externes de 120,50 et 0,00 plus un compte sans solde
**When** il ouvre `/tableau-de-bord`
**Then** le tableau de bord affiche la section « Mes comptes externes (Open Banking) », la carte « Patrimoine total » au sous-titre « Demo Bank + comptes externes » d'une valeur de 1 120,50 au format du legacy, « Comptes externes via Powens Open Banking (3 comptes) » et le bouton « Voir le detail et les transactions », qui mène à `/comptes-tiers`
**And** le total est calculé par le front en centimes entiers, à partir du total renvoyé par le cœur bancaire et du total renvoyé par `open-banking-conseiller`, par un formateur de `src/domain`

**Given** un utilisateur sans connexion Powens
**When** il ouvre `/tableau-de-bord`
**Then** le tableau de bord affiche la section « Open Banking », le texte « Agregez vos comptes d autres banques pour une vue complete de votre patrimoine » et le bouton « Connecter mes autres banques via Powens », qui mène à `/comptes-tiers`

**Given** le tableau de bord
**When** il se charge
**Then** l'appel à `GET /api/open-banking-conseiller/powens` part en parallèle des appels au cœur bancaire, jamais après eux (test Vitest sur l'ordre des appels), et n'entraîne aucun appel à Powens

**Given** `GET /api/open-banking-conseiller/powens` en échec (statut 503 ou service injoignable)
**When** le tableau de bord se charge
**Then** le reste du tableau de bord (« Solde total Demo Bank », comptes, « Dernieres operations », centre de notifications) s'affiche normalement et la carte « Patrimoine total » n'affiche pas une somme fausse (UX-DR104)

**Given** une requête sans jeton JWT
**When** elle appelle `GET /api/open-banking-conseiller/powens`
**Then** la réponse est 401, et le tableau de bord n'est pas affiché à un visiteur non authentifié : le routeur du front affiche `/login`

**Given** l'écran modifié
**When** je lance `scripts/golden-master/verifier-scenario.sh` avec SC-001-04, SC-001-05, SC-002-01 puis SC-002-02
**Then** le code de sortie est 0 pour chacun

**Given** la story terminée
**When** je lance `cd frontend && npx vitest run -t 'FR-002-12'`
**Then** le code de sortie est 0
**And** `grep -rl '\[FR-002-12\]' frontend/src` renvoie au moins un fichier de test

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/golden-master.sh` et `scripts/gates/trace-smoke.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

## Epic 9: Espace conseiller et administration

À la fin de cet epic, un conseiller ouvre son Espace Conseiller et y voit son portefeuille (seulement ses clients, décidés côté serveur), ses trois indicateurs, une carte par client et le détail d'un client sur la même page. L'administrateur dispose de toute la page d'administration : indicateurs globaux, gestion des clients (recherche, détail avec « Client depuis », activation et désactivation auditées), liste de tous les utilisateurs, revue des alertes de fraude (toutes les alertes, sans effet sur un virement suspendu), journal des traitements et catalogue des produits. Les sections déjà livrées sur `/administration` (file des virements suspendus de l'epic 3, file et historique des prêts de l'epic 5) restent en place et vertes.

**Contrôle de fin d'epic (à la charge de l'humain) :** lire, dans `reports/golden-master/textes-non-trouves.md`, les sections des scénarios de l'epic (SC-015-01, SC-015-02, SC-016-01, SC-016-02) ; lancer `scripts/golden-master/verifier-scenario.sh` pour chacun.

### Story 9.1: Exposer le portefeuille du conseiller décidé côté serveur

En tant que conseiller,
je veux que chaque service me renvoie seulement les données des clients qui me sont affectés, d'après mon jeton,
afin qu'aucun conseiller ne puisse lire les données d'un client hors de son portefeuille.

**Exigences :** FR-015-01, FR-015-02, FR-015-09, FR-030-02, FR-030-07, NFR-SEC-03, AR-18, AR-19, AR-20, AR-27, AR-28, AR-36, AR-37, AR-46, AR-47, AR-49, AR-89, AR-97
**Scénarios golden master :** aucun
**Prérequis :** topic `identite-clients.client-modifie.v1` produit (epic 2, avec `conseillerId`) ; projection clients du cœur bancaire (epic 3) et des crédits (epic 5) alimentées par ce topic ; vérification du jeton en place dans `identite-clients`, `coeur-bancaire` et `credits` (premières stories de ces services).

**Acceptance Criteria:**

**Given** le conseiller `sophie` affecté à deux clients (Jean Dupont / `user1`, Marie Lebrun / `user2`) et un autre conseiller affecté à un troisième client
**When** `sophie` appelle `GET /api/identite-clients/conseiller/portefeuille` avec son jeton
**Then** la réponse est 200 et contient exactement ses deux clients, triés par nom complet, avec pour chacun `clientId`, `nomComplet`, `login`, `email`, `telephone` et `actif`
**And** aucun client affecté à l'autre conseiller n'y figure

**Given** les mêmes données
**When** `sophie` appelle `GET /api/coeur-bancaire/conseiller/portefeuille`
**Then** la réponse est 200 et contient, pour chacun de ses deux clients et seulement pour eux, la liste de ses comptes (`compteId`, `numero`, `type` en code `COURANT` ou `EPARGNE`, `solde` en chaîne décimale à point, par exemple `"2130.00"`)
**And** l'appartenance est décidée par `conseillerId = sub` dans la projection `identite-clients.client-modifie.v1` du cœur bancaire, jamais par une liste d'identifiants envoyée par le front

**Given** les mêmes données
**When** `sophie` appelle `GET /api/credits/conseiller/portefeuille`
**Then** la réponse est 200 et contient, pour chacun de ses deux clients et seulement pour eux, la liste de ses prêts (`pretId`, `type` en code `IMMOBILIER`, `CONSOMMATION` ou `TRAVAUX`, `montant`, `mensualite`, `statut` en code `EN_ATTENTE`, `EN_COURS`, `REMBOURSE`, `DEFAUT` ou `REFUSE`), codes renvoyés tels quels

**Given** un client de la projection dont le `conseillerId` vient de changer (événement `identite-clients.client-modifie.v1` de `versionAgregat` supérieure)
**When** l'événement est consommé par le cœur bancaire et par les crédits
**Then** le client apparaît dans le portefeuille de son nouveau conseiller et disparaît de celui de l'ancien (latence de projection acceptée)

**Given** un ADMIN à qui aucun client n'est affecté
**When** il appelle chacun des trois points `…/conseiller/portefeuille`
**Then** chaque réponse est 200 avec une liste vide

**Given** un jeton de rôle CLIENT (`user1`)
**When** il appelle `GET /api/identite-clients/conseiller/portefeuille`, `GET /api/coeur-bancaire/conseiller/portefeuille` ou `GET /api/credits/conseiller/portefeuille`
**Then** chaque service répond 403 avec le corps `{"message": …}` de AD-17, sans pile ni code interne
**And** sans jeton, chaque service répond 401

**Given** `sophie` qui ajoute un paramètre `?conseillerId=<identifiant d'un autre conseiller>` ou `?clientId=<client hors portefeuille>` à l'un des trois points
**When** le service traite la requête
**Then** le paramètre est ignoré : la réponse ne contient que les clients de `sophie`

**Given** les classes `*SecuriteIT` de `identite-clients`, `coeur-bancaire` et `credits`
**When** elles sont complétées pour ces trois points (sans jeton → 401, CLIENT → 403, conseiller : seulement son portefeuille)
**Then** `mvn -B -q -f services/identite-clients/pom.xml verify`, `mvn -B -q -f services/coeur-bancaire/pom.xml verify` et `mvn -B -q -f services/credits/pom.xml verify` sortent en code 0

**Given** les tests JUnit de la story portant `@Tag("FR-015-01")`, `@Tag("FR-015-02")` et `@Tag("FR-015-09")`
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-015-02'`, `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-015-02'`, `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-015-02'`, puis les mêmes commandes avec `-Dgroups='FR-015-01'` et `-Dgroups='FR-015-09'` sur le service concerné
**Then** chaque commande sort en code 0

**Given** une violation introduite volontairement (le cœur bancaire filtre le portefeuille d'après un paramètre `conseillerId` de la requête au lieu du `sub` du jeton)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml verify`
**Then** le code de sortie est non nul (le test « paramètre ignoré » et `CoeurBancaireSecuriteIT` échouent)

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 9.2: Afficher l'Espace Conseiller (portefeuille, cartes et détail d'un client)

En tant que conseiller,
je veux voir mon portefeuille, ses indicateurs, une carte par client et le détail d'un client sur la même page,
afin de préparer un rendez-vous sans quitter mon espace.

**Exigences :** FR-015-01, FR-015-02, FR-015-03, FR-015-04, FR-015-05, FR-015-06, FR-015-07, FR-015-08, FR-015-09, FR-030-02, NFR-UI-01, NFR-UI-02, NFR-PERF-01, AR-47, AR-49, AR-76, AR-78, AR-79, AR-89, UX-DR40, UX-DR47, UX-DR48, UX-DR54, UX-DR56, UX-DR58, UX-DR68, UX-DR71, UX-DR83, UX-DR94, UX-DR99, UX-DR131, UX-DR153
**Scénarios golden master :** SC-015-01, SC-015-02
**Prérequis :** story 9.1 ; route `/espace-conseiller` et redirection des rôles par le routeur du front (epic 2, FR-030-02) ; `scripts/golden-master/verifier-scenario.sh` (epic 2).

**Acceptance Criteria:**

**Given** `sophie` connectée (CONSEILLER, nom complet « Sophie Durand » dans le jeu de test)
**When** elle ouvre `/espace-conseiller`
**Then** le front appelle en parallèle les trois points `…/conseiller/portefeuille` (jamais en cascade) et affiche le bandeau « Espace Conseiller » et « Bonjour Sophie Durand, vous gerez 2 client(s) »
**And** le sub-header affiche « Mon portefeuille » · « Conseiller IA » (lien vers `/mon-conseiller`)

**Given** le portefeuille chargé
**When** la page s'affiche
**Then** trois indicateurs (carte KPI en `card-muted`) sont affichés : « Mes clients » (2, sous-libellé « clients dans mon portefeuille »), « Encours total » (somme des soldes de tous les comptes des deux clients, au format legacy `EUR{N,NNN}`, sous-libellé « solde cumulé mes clients ») et « Prets en attente » (nombre de prêts `EN_ATTENTE`, sous-libellé « a traiter »)
**And** leurs libellés sont rendus en capitales par CSS (« MES CLIENTS », « ENCOURS TOTAL », « PRETS EN ATTENTE »), le texte source gardant sa casse
**And** l'encours est calculé en centimes entiers par un formateur de `src/domain`

**Given** un portefeuille non vide
**When** la liste s'affiche
**Then** chaque client a une carte avec l'initiale de son nom complet en avatar (`aria-hidden`), le nom complet, « @ » + identifiant (« @user1 »), un badge « Actif » ou « Inactif », « Solde total » (`EUR{N,NNN}`), « Comptes », « Prets actifs » (nombre `EN_COURS`), « Prets en attente » (nombre `EN_ATTENTE`) et un bouton « Voir detail » (`data-testid="conseiller-voir-detail-{clientId}"`)
**And** la carte d'un client inactif est visuellement distinguée, avec le texte « Inactif » (la couleur ne porte jamais seule l'état)

**Given** un client avec 1 prêt `EN_ATTENTE` et 2 prêts `EN_COURS`
**When** sa carte s'affiche
**Then** elle porte « ⚠ 1 pret(s) en attente » et pas « ✓ 2 pret(s) en cours »
**And** un client avec 0 prêt `EN_ATTENTE` et 2 `EN_COURS` porte « ✓ 2 pret(s) en cours » ; un client sans prêt n'en porte aucun

**Given** un conseiller sans client
**When** il ouvre `/espace-conseiller`
**Then** à la place de la liste s'affichent « Aucun client dans votre portefeuille. » puis « Contactez l administrateur pour l affectation des clients. »

**Given** la carte de Jean Dupont
**When** je clique « Voir detail »
**Then** un panneau inline s'ouvre sur la même page (pas de modale, pas de changement de route) avec nom complet, « @user1 », email, téléphone, la section « Comptes bancaires » (colonnes « Numero », « Type », « Solde » au format `EUR{N,NNN.NN}`) et la section « Prets » (colonnes « Type », « Montant » `EUR{N,NNN}`, « Mensualite » `EUR{NNN.NN}`, « Statut »)
**And** le focus va au titre du panneau ; « Fermer » (`data-testid="conseiller-fermer-detail"`) ou `Échap` le referme et rend le focus au bouton « Voir detail » déclencheur
**And** le panneau n'affiche pas « Client depuis »

**Given** un client sans compte et sans prêt
**When** son panneau de détail s'ouvre
**Then** la section des comptes affiche « Aucun compte » et celle des prêts « Aucun pret »

**Given** des prêts de types `IMMOBILIER`, `CONSOMMATION`, `TRAVAUX` et de statuts `EN_ATTENTE`, `EN_COURS`, `REMBOURSE`, `DEFAUT`, `REFUSE`, et des comptes `COURANT` et `EPARGNE`
**When** le panneau les affiche
**Then** les codes sont affichés tels quels, les statuts `EN_ATTENTE`, `EN_COURS`, `REFUSE` et `REMBOURSE` en badge `tag`, et `DEFAUT` sans badge (BUG-053 conservé)

**Given** le portefeuille déjà chargé dans la session
**When** `sophie` quitte puis rouvre `/espace-conseiller` sans s'être déconnectée
**Then** aucun nouvel appel aux trois points n'est émis (chargement une fois par session, FR-015-02)
**And** après une déconnexion puis une nouvelle connexion, le portefeuille est rechargé

**Given** un utilisateur CLIENT connecté
**When** il ouvre `/espace-conseiller`
**Then** il est redirigé vers `/tableau-de-bord` sans aucun message d'erreur (« Tableau de bord » et « MES COMPTES DEMO BANK » affichés)
**And** aucun appel aux points `…/conseiller/portefeuille` n'est émis

**Given** l'un des trois services répond 500 avec `{"message": …}`
**When** la page se charge
**Then** l'écran n'affiche aucun détail technique (pile, code interne) et le `message` reçu est affiché dans la zone `conseiller-message-erreur`

**Given** les tests Vitest de la story, dont le titre contient `[FR-015-01]` à `[FR-015-09]`
**When** je lance `cd frontend && npx vitest run -t 'FR-015-0'`
**Then** le code de sortie est 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-015-01`
**Then** le code de sortie est 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-015-02`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 9.3: Afficher les indicateurs, la gestion des clients et tous les utilisateurs sur l'administration

En tant qu'administrateur,
je veux voir les indicateurs globaux, rechercher un client, consulter son détail et voir tous les utilisateurs du système,
afin de piloter la banque depuis une seule page.

**Exigences :** FR-016-02, FR-016-08, FR-016-09, FR-016-10, FR-016-13, FR-016-01, FR-030-07, NFR-SEC-03, NFR-UI-01, NFR-UI-02, NFR-PERF-01, AR-18, AR-20, AR-36, AR-37, AR-46, AR-47, AR-49, AR-76, AR-78, AR-79, AR-89, UX-DR16, UX-DR47, UX-DR48, UX-DR56, UX-DR58, UX-DR67, UX-DR94, UX-DR132, UX-DR153, UX-DR156
**Scénarios golden master :** SC-016-01, SC-016-02
**Prérequis :** A-016-2 (« Client depuis » = date de création de la fiche client, affichée pour tout client) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; route `/administration` réservée ADMIN (epic 2, FR-016-01) ; section « Pipeline Prets — En attente d approbation » (epic 5) et section de revue des virements suspendus (epic 3) déjà sur la page.

**Acceptance Criteria:**

**Given** un jeu de test de 7 utilisateurs (1 ADMIN, 2 CONSEILLER, 4 CLIENT dont 3 actifs) et 2 prêts `EN_ATTENTE` de 5 000,00 € et 10 000,00 €
**When** l'ADMIN ouvre `/administration`
**Then** les indicateurs affichent « Total clients » 4 avec « 3 actifs », « Conseillers » 2 avec « actifs », « Prets en attente » 2 avec « {montant} EUR a traiter » (cumul 15 000,00 €, au format du legacy) et « Total utilisateurs » 7 avec « tous roles confondus »
**And** les libellés sont rendus en capitales par CSS (« TOTAL CLIENTS », « CONSEILLERS », « PRETS EN ATTENTE ») ; les comptages viennent de `GET /api/identite-clients/administration/indicateurs` et le nombre et le cumul des prêts `EN_ATTENTE` de `GET /api/credits/administration/indicateurs`, appelés en parallèle

**Given** la section « Gestion des clients »
**When** elle s'affiche avec une recherche vide
**Then** elle liste les 4 personnes de rôle CLIENT et elles seules (jamais un CONSEILLER ni un ADMIN), colonnes « Nom », « Login », « Email », « Role », « Statut » (« Actif »/« Inactif » en badge), « Conseiller » (nom complet du conseiller affecté), « Actions »

**Given** la saisie « DUPONT » dans le champ de recherche (texte indicatif « Rechercher par nom, login, email... », `data-testid="admin-recherche"`)
**When** l'ADMIN clique « Filtrer » (`button-secondary`, `data-testid="admin-filtrer"`)
**Then** seuls les clients dont le nom complet, l'identifiant ou l'email contient le terme, sans distinction de casse, sont affichés (Jean Dupont)
**And** une saisie partielle « user » garde les clients dont l'identifiant contient « user »

**Given** un terme qui ne correspond à aucun client
**When** l'ADMIN clique « Filtrer »
**Then** le tableau garde ses en-têtes et affiche une ligne « Aucun client trouve »

**Given** la ligne de Jean Dupont
**When** l'ADMIN clique « Detail » (`data-testid="admin-detail-{customerId}"`)
**Then** un panneau inline « Detail : Jean Dupont », en lecture seule, affiche « Nom complet », « Identifiant », « Email », « Telephone », « Role », « Statut », « Conseiller assigne » et « Client depuis » au format `jj/MM/aaaa`
**And** le focus va au titre ; « Fermer » (`data-testid="admin-fermer-detail"`) ou `Échap` le ferme et rend le focus au bouton déclencheur

**Given** un client dont la fiche a été créée le 01/02/2024 et affecté à un conseiller le 15/06/2025
**When** son panneau de détail s'ouvre
**Then** « Client depuis » affiche 01/02/2024 (date de création de la fiche), pas 15/06/2025 (test `@Tag("BUG-055")`)
**And** pour un client sans conseiller, « Conseiller assigne » n'est pas affiché et « Client depuis » est affiché avec sa date de création

**Given** la section « Tous les utilisateurs du systeme »
**When** elle s'affiche
**Then** elle liste en lecture seule les 7 utilisateurs de tous les rôles (CLIENT, CONSEILLER, ADMIN), colonnes « Nom », « Login », « Email », « Role », « Statut », depuis `GET /api/identite-clients/administration/utilisateurs`

**Given** la page complète
**When** son texte rendu est lu dans l'ordre du DOM
**Then** l'ordre des sections est : indicateurs, « Pipeline Prets — En attente d approbation », « Gestion des clients », « Historique de tous les prets », « Tous les utilisateurs du systeme », « Virements suspendus — En attente de revue », puis les sections suivantes ; « Gestion des clients », « Jean Dupont » et « user1 » se trouvent dans les 5 000 premiers caractères du texte rendu (test Vitest sur un jeu équivalent aux données de référence)
**And** les tests existants de la file des prêts (epic 5) et de la file de revue des virements suspendus (epic 3) restent verts

**Given** un jeton CONSEILLER ou CLIENT
**When** il appelle `GET /api/identite-clients/administration/indicateurs`, `GET /api/identite-clients/administration/clients`, `GET /api/identite-clients/administration/utilisateurs` ou `GET /api/credits/administration/indicateurs`
**Then** le service répond 403 avec `{"message": …}` ; sans jeton, 401 (cas ajoutés aux `*SecuriteIT` des deux services)

**Given** un CLIENT ou un CONSEILLER connecté
**When** il ouvre `/administration`
**Then** il est redirigé vers `/tableau-de-bord` sans message (« Tableau de bord » et « MES COMPTES DEMO BANK » affichés), sans aucun appel aux points `…/administration/…`

**Given** les tests JUnit `@Tag("FR-016-02")`, `@Tag("FR-016-08")`, `@Tag("FR-016-09")`, `@Tag("FR-016-10")`, `@Tag("BUG-055")`, `@Tag("FR-016-13")`
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-02 | FR-016-08 | FR-016-09 | FR-016-10 | BUG-055 | FR-016-13'` et `mvn -B -q -f services/credits/pom.xml test -Dgroups='FR-016-02'`
**Then** chaque commande sort en code 0

**Given** les tests Vitest dont le titre contient `[FR-016-02]`, `[FR-016-08]`, `[FR-016-09]`, `[FR-016-10]`, `[BUG-055]`, `[FR-016-13]`
**When** je lance `cd frontend && npx vitest run -t 'FR-016-0'`, `cd frontend && npx vitest run -t 'FR-016-1'` et `cd frontend && npx vitest run -t 'BUG-055'`
**Then** chaque commande sort en code 0

**Given** une violation introduite volontairement (« Client depuis » lu sur la date d'affectation du conseiller)
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='BUG-055'`
**Then** le code de sortie est non nul

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-016-01`
**Then** le code de sortie est 0

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-016-02`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 9.4: Activer et désactiver un client depuis la gestion des clients

En tant qu'administrateur,
je veux désactiver ou réactiver un client depuis sa ligne dans « Gestion des clients »,
afin de bloquer ou rétablir l'accès d'un client sans quitter la page d'administration.

**Exigences :** FR-016-11, FR-016-06, FR-026-02, FR-001-07, NFR-SEC-04, AR-35, AR-46, AR-78, UX-DR17, UX-DR42, UX-DR57, UX-DR107, UX-DR132
**Scénarios golden master :** aucun
**Prérequis :** story 9.3 ; API ADMIN de désactivation et de réactivation d'un client d'`identite-clients`, avec les audits `ADMIN_DEACTIVATE` / `ADMIN_ACTIVATE` (story 2.6) et la publication de `identite-clients.client-modifie.v1` (story 2.7) : cette story livre l'écran et réutilise cette API sans la refaire ni la modifier.

**Acceptance Criteria:**

**Given** le client actif Pierre Bernard (`user3`) dans « Gestion des clients »
**When** l'ADMIN clique « Desactiver » (`button-ghost`, `data-testid="admin-desactiver-{customerId}"`), sans modale de confirmation
**Then** le front appelle l'API de désactivation de la story 2.6, le message « Client désactivé » s'affiche en `alert-success` (`role="status"`, `data-testid="admin-message-succes"`), et la ligne affiche le badge « Inactif » et le bouton « Activer »
**And** le bouton est désactivé (`aria-disabled`) pendant la requête

**Given** le client inactif `user3`
**When** l'ADMIN clique « Activer » (`data-testid="admin-activer-{customerId}"`)
**Then** le message « Client activé » s'affiche et la ligne affiche le badge « Actif » et le bouton « Desactiver »

**Given** une désactivation puis une activation faites depuis l'écran, application démarrée
**When** je lis `AUDIT_LOGS` du service `audit`
**Then** on y trouve une entrée `ADMIN_DEACTIVATE` puis une entrée `ADMIN_ACTIVATE` avec l'identifiant du client, celui de l'ADMIN et l'adresse IP d'origine (audit produit par l'API de la story 2.6, que l'écran ne double pas : aucune autre entrée d'audit n'est émise par le front)

**Given** `user3` désactivé depuis l'écran
**When** il tente de se connecter avec un mot de passe correct
**Then** la connexion est refusée avec le message générique de FR-001-07 ; un jeton déjà émis reste valable jusqu'à son expiration (décision Q1, risque R-5)

**Given** l'API de désactivation qui répond en erreur (403 ou 500 avec `{"message": …}`)
**When** l'ADMIN clique « Desactiver »
**Then** l'écran affiche le `message` reçu dans `admin-message-erreur`, sans détail technique ; le badge reste « Actif » et « Client désactivé » n'est pas affiché

**Given** les tests Vitest dont le titre contient `[FR-016-11]`
**When** je lance `cd frontend && npx vitest run -t 'FR-016-11'`
**Then** le code de sortie est 0

**Given** les tests JUnit existants de l'API (story 2.6) `@Tag("FR-016-11")` et `@Tag("FR-016-06")`
**When** je lance `mvn -B -q -f services/identite-clients/pom.xml test -Dgroups='FR-016-11 | FR-016-06'`
**Then** le code de sortie est 0 (non-régression ; la story ne modifie pas ces tests)

**Given** une violation introduite volontairement (le front affiche « Client désactivé » sans attendre la réponse de l'API)
**When** je lance `cd frontend && npx vitest run -t 'FR-016-11'`
**Then** le code de sortie est non nul (le test « API en erreur → badge inchangé, pas de message de succès » échoue)

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 9.5: Revoir les alertes de fraude sans toucher au virement suspendu

En tant qu'administrateur,
je veux voir toutes les alertes de fraude et confirmer une alerte ou la classer en faux positif,
afin de qualifier les alertes du moteur sans libérer ni rejeter un virement par erreur.

**Exigences :** FR-016-14, FR-016-15, FR-016-16, FR-027-12, FR-027-13, FR-027-14, FR-016-19, NFR-PAR-02, NFR-SEC-03, AR-19, AR-36, AR-37, AR-43, AR-44, AR-46, AR-78, UX-DR17, UX-DR40, UX-DR47, UX-DR48, UX-DR132
**Scénarios golden master :** aucun
**Prérequis :** A-016-4 (auteur de la revue enregistré par identifiant, affiché par nom complet) — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; alertes `FRAUD_ALERTS` et machine d'états de l'alerte (OPEN → REVIEWED) livrées par le socle RG-027-003 (epic 3) ; story 9.3.

**Acceptance Criteria:**

**Given** trois alertes en base : une `OPEN` (score 45), une `CONFIRMED` et une `FALSE_POSITIVE` revues par l'ADMIN connecté
**When** l'ADMIN ouvre `/administration`
**Then** la section « Alertes Fraude — Moteur de détection » affiche les trois lignes, triées par date de création décroissante, colonnes « ID », « Client », « Règle déclenchée », « Score risque » (« 45/100 »), « Statut », « Description », « Date », « Actions »
**And** les deux alertes traitées affichent « Traité par {nom complet de l'ADMIN} » à la place des boutons, et le badge affiche « 1 ouverte(s) » (test `@Tag("BUG-056")`)

**Given** aucune alerte `OPEN`
**When** la section s'affiche
**Then** aucun badge « {n} ouverte(s) » n'est affiché
**And** sans aucune alerte en base, la section affiche « Aucune alerte fraude enregistrée »

**Given** une alerte `OPEN`
**When** l'ADMIN clique « Confirmer » (`button-ghost`, `data-testid="admin-alerte-confirmer-{alertId}"`)
**Then** l'alerte passe à `CONFIRMED`, l'identifiant de l'ADMIN et l'heure du port Horloge sont enregistrés comme auteur et horodatage de la revue, et la ligne affiche « Traité par {nom complet de l'ADMIN} » après rechargement de la section

**Given** une alerte `OPEN`
**When** l'ADMIN clique « Faux positif » (`data-testid="admin-alerte-faux-positif-{alertId}"`)
**Then** l'alerte passe à `FALSE_POSITIVE`, avec l'identifiant de l'ADMIN et l'heure de la revue, et la ligne affiche « Traité par {nom complet de l'ADMIN} »

**Given** une alerte `OPEN` levée par un virement `SUSPENDU`
**When** l'ADMIN la confirme, ou la classe en faux positif
**Then** le virement reste `SUSPENDU` : aucun débit, aucune libération, aucun rejet, aucun audit de virement, aucune notification
**And** si l'ADMIN libère ensuite ce virement depuis la file de revue (FR-016-19, epic 3), l'alerte `CONFIRMED` (ou `FALSE_POSITIVE`) garde son statut et seules les autres alertes encore `OPEN` du virement passent à `REVIEWED` (A-016-5)

**Given** une alerte déjà `CONFIRMED`, `FALSE_POSITIVE` ou `REVIEWED`
**When** une demande de confirmation ou de classement en faux positif arrive
**Then** la transition est refusée sans effet (`UPDATE … WHERE statut = 'OPEN'`, zéro ligne) : statut, auteur et horodatage restent inchangés
**And** deux demandes concurrentes (« Confirmer » et « Faux positif ») sur la même alerte `OPEN` ne laissent qu'un seul effet (`AlerteFraudeTransitionConcurrenteTest`)

**Given** le cas d'usage de revue appelé sans identifiant d'alerte
**When** il s'exécute
**Then** la demande est ignorée sans erreur et aucune alerte n'est modifiée

**Given** un jeton CONSEILLER ou CLIENT
**When** il appelle `GET /api/coeur-bancaire/administration/alertes-fraude`, `POST /api/coeur-bancaire/administration/alertes-fraude/{alertId}/confirmation` ou `POST /api/coeur-bancaire/administration/alertes-fraude/{alertId}/faux-positif`
**Then** la réponse est 403 avec `{"message": …}` et aucune alerte n'est modifiée (cas ajoutés à `CoeurBancaireSecuriteIT`)

**Given** les tests JUnit `@Tag("FR-016-14")`, `@Tag("FR-016-15")`, `@Tag("FR-016-16")`, `@Tag("BUG-056")`, `@Tag("FR-027-12")`, `@Tag("FR-027-13")`, `@Tag("FR-027-14")`
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-016-14 | FR-016-15 | FR-016-16 | BUG-056 | FR-027-12 | FR-027-13 | FR-027-14'`
**Then** le code de sortie est 0

**Given** les tests Vitest dont le titre contient `[FR-016-14]`, `[FR-016-16]`, `[BUG-056]`, `[FR-027-12]`
**When** je lance `cd frontend && npx vitest run -t 'FR-016-1'`, `cd frontend && npx vitest run -t 'FR-027-1'` et `cd frontend && npx vitest run -t 'BUG-056'`
**Then** chaque commande sort en code 0

**Given** une violation introduite volontairement (la liste des alertes filtrée sur le statut `OPEN`, comportement legacy RG-016-034)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='BUG-056'`
**Then** le code de sortie est non nul

**Given** une violation introduite volontairement (« Confirmer » passe aussi le virement lié à `LIBÉRÉ`)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-027-13'`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 9.6: Afficher le journal des traitements et le catalogue des produits sur l'administration

En tant qu'administrateur,
je veux consulter le journal des traitements périodiques et le catalogue des produits bancaires depuis l'administration,
afin de vérifier les traitements de la nuit et l'offre active sans changer d'écran.

**Exigences :** FR-016-17, FR-016-18, FR-016-01, NFR-BATCH-02, NFR-UI-01, NFR-PERF-01, AR-25, AR-47, AR-64, AR-79, UX-DR47, UX-DR48, UX-DR77, UX-DR132, UX-DR153
**Scénarios golden master :** SC-016-01
**Prérequis :** RP-01 (journal saturé par les 48 entrées par jour du contrôle des soldes faibles : pagination, filtre par traitement ou nombre d'entrées, et critère à ajouter à FR-016-17) — tranché le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; lecture du journal `GET /api/coeur-bancaire/traitements?limite=50` ouverte à l'ADMIN (epic 4, FR-018-05) ; catalogue des produits du service `open-banking-conseiller` (epic 7, FEAT-011) ; stories 9.3 à 9.5.

**Acceptance Criteria:**

**Given** 60 entrées dans `BATCH_JOB_LOGS` du cœur bancaire
**When** l'ADMIN ouvre `/administration`
**Then** la section « Traitements nocturnes » (sans « (EJB Timer @Schedule) », écart B-15) affiche en lecture seule les 50 entrées les plus récentes, colonnes « Job », « Debut », « Fin », « Duree (ms) », « Enregistrements », « Statut », « Details »
**And** les heures sont affichées en Europe/Paris par un formateur de `src/domain`

**Given** aucune entrée de journal
**When** la section s'affiche
**Then** elle garde ses en-têtes et affiche « Aucun traitement nocturne enregistre »

**Given** le catalogue de référence (produits actifs d'épargne, de crédit et d'investissement) et un produit inactif
**When** la section « Catalogue produits bancaires » s'affiche
**Then** elle regroupe en lecture seule les produits actifs sous « Produits d epargne », « Produits de credit » et « Produits d investissement », sans le produit inactif, avec le lien « Voir la page Produits » (`data-testid="admin-voir-produits"`) qui mène à `/produits`

**Given** une catégorie sans produit actif
**When** la section s'affiche
**Then** cette catégorie affiche « Aucun produit »

**Given** la page d'administration complète
**When** son texte rendu est lu dans l'ordre du DOM (test Vitest sur un jeu équivalent aux données de référence : 4 clients, 7 utilisateurs, 2 prêts, 6 alertes, 50 entrées de journal, catalogue complet)
**Then** l'ordre des sections est : indicateurs, « Pipeline Prets — En attente d approbation », « Gestion des clients », « Historique de tous les prets », « Tous les utilisateurs du systeme », « Virements suspendus — En attente de revue », « Alertes Fraude — Moteur de détection », « Traitements nocturnes », « Catalogue produits bancaires »
**And** « TOTAL CLIENTS », « CONSEILLERS », « PRETS EN ATTENTE », « Pipeline Prets », « Gestion des clients », « Jean Dupont » et « user1 » se trouvent dans les 5 000 premiers caractères du texte rendu

**Given** le service `open-banking-conseiller` ou le cœur bancaire qui répond en erreur
**When** la page se charge
**Then** seule la section concernée affiche le `message` reçu, sans détail technique ; les autres sections restent affichées

**Given** les tests Vitest dont le titre contient `[FR-016-17]` et `[FR-016-18]`
**When** je lance `cd frontend && npx vitest run -t 'FR-016-17'` et `cd frontend && npx vitest run -t 'FR-016-18'`
**Then** chaque commande sort en code 0

**Given** les tests JUnit `@Tag("FR-016-17")` du cœur bancaire (limite de 50 et tri par heure de début décroissante)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-016-17'`
**Then** le code de sortie est 0

**Given** une violation introduite volontairement (section « Traitements nocturnes » placée avant « Gestion des clients »)
**When** je lance `cd frontend && npx vitest run -t 'FR-016-17'`
**Then** le code de sortie est non nul (le test d'ordre des sections échoue)

**Given** l'écran livré
**When** je lance `scripts/golden-master/verifier-scenario.sh SC-016-01`
**Then** le code de sortie est 0

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

## Epic 10: Recette finale et bascule

À la fin de cet epic, un virement important exécuté et débité est annoncé au client comme une « transaction importante », sans les mots « en attente » ni « verification » (BUG-009 corrigé). L'équipe dispose d'un banc de rejeu reproductible : application démarrée en profil de rejeu, horloge réglée, données de référence des 8 services rechargées par une commande, comptes de test par rôle et fournisseurs simulés. Le golden master du legacy est rejoué une seule fois, en entier, et le critère de bascule est mesuré : 31 scénarios réussis, 0 en échec, 2 non rejoués (SC-003-03, SC-010-02).

### Story 10.1: Reformuler les textes d'un virement important exécuté

En tant que client,
je veux qu'un virement important déjà débité soit annoncé comme une « transaction importante » et non comme « en attente »,
afin de ne pas croire que mon argent n'est pas encore parti.

**Exigences :** FR-003-29, FR-003-10, FR-003-09, FR-003-14, FR-005-07, FR-023-03, FR-025-01, FR-002-10, NFR-PAR-02, NFR-UI-01, AR-65, AR-68, AR-98, UX-DR111, UX-DR121, UX-DR150
**Scénarios golden master :** aucun
**Prérequis :** démonstration faite (décision B1 révisée, risque R-4) ; formulation des textes de FR-003-29 (« Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. », « [TRANSACTION IMPORTANTE] », badge « Transaction importante ») — confirmé le 2026-10-05 (voir « Arbitrages du 2026-10-05 ») ; (arbitré le 2026-10-05 : le PRD, FR-003-29, fait foi pour ces trois textes) ; toutes les stories des epics 3 à 9 terminées.

**Acceptance Criteria:**

**Given** un virement de 5 000,00 € entre deux comptes de `user1`, de score cumulé inférieur ou égal à 75 (exécuté, statut technique `PENDING`)
**When** il est soumis
**Then** le virement est exécuté et débité immédiatement comme avant (FR-003-10), son statut enregistré reste `PENDING`
**And** l'unique notification, de type ALERTE, publiée par le cœur bancaire dans `coeur-bancaire.virement-decide.v1`, a pour message « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. » ({montant} au même format que l'ancienne notification ALERTE, {numCompte} = numéro du compte destinataire)

**Given** le même virement
**When** ses opérations sont enregistrées
**Then** les descriptions de l'opération DEBIT et de l'opération CREDIT commencent par « [TRANSACTION IMPORTANTE] » à la place de « [EN ATTENTE] » (« [TRANSACTION IMPORTANTE] Virement vers {compte destinataire} - {motif} », « [TRANSACTION IMPORTANTE] Virement depuis {compte source} - {motif} »)
**And** « [TRANSACTION IMPORTANTE] » et « transaction importante » sont définis dans le seul fichier de textes du cœur bancaire (AR-68), plus aucun texte du cœur bancaire ne contient « en attente de validation » : `! grep -rn "en attente de validation" services/coeur-bancaire/src/main`

**Given** ce virement affiché au relevé, au tableau de bord (5 dernières opérations, centre de notifications) et dans le suivi du client
**When** les écrans sont rendus
**Then** le relevé affiche le badge `tag` « Transaction importante » avant le libellé, le préfixe « [TRANSACTION IMPORTANTE] » étant retiré du libellé affiché comme FR-005-07 le fait pour « [EN ATTENTE] »
**And** aucun texte affiché pour ce virement ne contient « en attente » ni « verification » (comparaison insensible à la casse, test `@Tag("BUG-009")` côté cœur bancaire et `[BUG-009]` côté front)

**Given** une opération déjà en base dont la description commence par « [EN ATTENTE] » (données de référence ou virement antérieur à la story)
**When** le relevé l'affiche
**Then** le badge « En attente » est toujours affiché et les 13 caractères « [EN ATTENTE] » sont toujours retirés du libellé (FR-005-07 inchangée)

**Given** un virement de 4 999,99 €
**When** il est exécuté
**Then** il reste `SUCCESS` avec une notification CONFIRMATION, sans « [TRANSACTION IMPORTANTE] » ni badge « Transaction importante »
**And** un virement de 10 000,00 € reste `SUSPENDU` sans débit, avec la seule notification FRAUDE (FR-027-02 inchangée)

**Given** les scénarios dérivés du golden master
**When** je lance `! grep -rniE "en attente de validation|verification en cours|\[EN ATTENTE\]" docs/retro/golden-master/derived/*/*/expected-dom.txt`
**Then** le code de sortie est 0 : aucune ligne attendue n'est touchée, `replay-mapping.json`, `docs/retro/golden-master/derived/**` et `e2e/golden-master/**` ne sont pas modifiés (`git diff --exit-code -- replay-mapping.json docs/retro/golden-master/derived e2e/golden-master` sort en code 0)

**Given** les tests JUnit `@Tag("FR-003-29")` et `@Tag("BUG-009")`
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='FR-003-29 | BUG-009'`
**Then** le code de sortie est 0

**Given** les tests Vitest dont le titre contient `[FR-003-29]` et `[BUG-009]`
**When** je lance `cd frontend && npx vitest run -t 'FR-003-29'` et `cd frontend && npx vitest run -t 'BUG-009'`
**Then** chaque commande sort en code 0

**Given** une violation introduite volontairement (le texte ALERTE legacy « … en attente de validation - transaction importante, verification en cours. » rétabli dans le fichier de textes du cœur bancaire)
**When** je lance `mvn -B -q -f services/coeur-bancaire/pom.xml test -Dgroups='BUG-009'`
**Then** le code de sortie est non nul

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 10.2: Monter le socle du rejeu final du golden master

En tant qu'équipe de recette,
je veux démarrer l'application en profil de rejeu, remettre ses données à l'état de référence par une commande et lancer le runner Playwright sur les tests générés,
afin de rejouer le golden master de façon reproductible, quelle que soit l'heure réelle.

**Exigences :** NFR-PAR-04, NFR-PAR-05, NFR-INT-01, NFR-SEC-06, AR-16, AR-33, AR-56, AR-57, AR-58, AR-60, AR-61, AR-63, AR-70, AR-71, AR-72, AR-74, AR-76, AR-77, AR-80, AR-81, AR-82, AR-83, AR-84, AR-100, AR-103, AR-104, UX-DR160, UX-DR161
**Scénarios golden master :** SC-003-04 (preuve de déterminisme) ; les 31 scénarios applicables pour le lancement du runner
**Prérequis :** story 10.1 ; les 8 services et les 15 routes de la SPA livrés (AR-100) ; port Horloge et profil `rejeu` du cœur bancaire (epic 3) ; simulateurs Powens, AlphaVantage et Azure OpenAI (epics 7 et 8) ; variables `RETRO_ADMIN_PASSWORD`, `RETRO_CONSEILLER_PASSWORD`, `RETRO_CLIENT_PASSWORD` fournies par l'humain ; exception décidée le 2026-10-05 (règle de `docs/quality-gates.md`) : cette story, et elle seule, peut créer `e2e/golden-master/package.json` et son fichier de verrouillage, et corriger en autonomie `replay-mapping.json` puis redériver et régénérer avec les outils, en inscrivant chaque correction au registre des écarts voulus ; jamais d'édition à la main des tests générés ni du dérivé, jamais de scénario retiré ni de texte attendu supprimé ; le golden master reste hors de l'empreinte et du hook (décision a) ; l'exception est à reporter dans `AGENTS.md` par un refresh de `bmad-project-context` avant la story.

**Acceptance Criteria:**

**Given** le dépôt
**When** la story installe Playwright Test dans `e2e/golden-master`
**Then** `e2e/golden-master/package.json` déclare `@playwright/test` en version exacte `1.63.0` (versions-verifiees.md) et `e2e/golden-master/package-lock.json` est versionné ; `cd e2e/golden-master && npm ci && npx playwright install chromium` sort en code 0
**And** les tests générés et le golden master dérivé ne sont jamais édités à la main : toute différence avec `HEAD` sous `e2e/golden-master/` (hors `package.json` et son fichier de verrouillage), `docs/retro/golden-master/derived/` ou `replay-mapping.json` provient d'une correction de `replay-mapping.json` suivie de `uv run docs/retro/tools/derive-golden-master.py derive … --force` et de `uv run docs/retro/tools/export-golden-master-tests.py …`, et `scripts/gates/golden-master.sh` sort en code 0

**Given** une correction de `replay-mapping.json` faite par la story
**When** je relis `_bmad-output/planning-artifacts/architecture/architecture-next-banking-webapp-2026-10-05/ecarts-voulus-golden-master.md`
**Then** chaque scénario corrigé y a une ligne (scénario, ce qui change, raison)
**And** le nombre de scénarios rejoués reste 31 et aucune ligne attendue n'est supprimée : `uv run docs/retro/tools/derive-golden-master.py derive --gm-dir docs/retro/golden-master --mapping replay-mapping.json --output-dir /tmp/gm-check` annonce `derived` = 31 et `dropped` = 2

**Given** une ligne attendue retirée volontairement d'un `expected-dom.txt` du dérivé, à la main
**When** je lance `scripts/gates/golden-master.sh`
**Then** le code de sortie est non nul (édition à la main détectée)

**Given** `compose.rejeu.yaml` et la cible `make rejeu-up`
**When** je lance `docker compose -f compose.yaml -f compose.rejeu.yaml config --format json | jq '[.services[] | select(.environment.SPRING_PROFILES_ACTIVE // "" | test("rejeu"))] | length'`
**Then** la valeur affichée est `8`
**And** en profil `rejeu`, tous les traitements planifiés métier sont désactivés (relais de l'outbox excepté), le jeton dure 60 minutes, aucun fournisseur n'est en `mode=reel` (`docker compose -f compose.yaml -f compose.rejeu.yaml config --format json | jq` ne renvoie aucune valeur `reel` pour une variable `NB_FOURNISSEURS_*_MODE`), et `TZ=Europe/Paris` est posé sur chaque conteneur

**Given** `HORLOGE_DEPART` absent
**When** je lance `make rejeu-up`
**Then** la cible calcule un seul `HORLOGE_DECALAGE` = `2026-11-17T10:00:00` (Europe/Paris) − heure réelle, le passe aux 8 services, démarre l'ensemble, lance `make reset-data`, attend que tous les conteneurs soient sains et sort en code 0
**And** `GET /api/coeur-bancaire/tableau-de-bord` avec le jeton de `user1` renvoie un champ `aujourdhui` égal à `2026-11-17`

**Given** l'application démarrée en profil `rejeu`
**When** je lance trois fois `make reset-data` (qui appelle `scripts/reset-reference-data.sh`)
**Then** chaque exécution sort en code 0 ; elle arrête les consommateurs Kafka, vide les tables métier, les projections, `outbox`, `outbox_audit` et `evenements_traites` (`TRUNCATE … RESTART IDENTITY CASCADE`), supprime puis recrée les topics, recharge les 8 `services/<identifiant>/reference-data/reference-data.sql` (projections comprises, générées par `scripts/generer-projections-reference.sh`), replace les séquences, vide les caches et relance les consommateurs, sans toucher à l'horloge
**And** les comptages par table des 8 bases sont identiques après chacune des trois exécutions et égaux aux 153 lignes de référence (identite-clients 7 ; coeur-bancaire 7 + 45 + 0 + 3 + 5 + 6 + 6 ; credits 2 + 18 + 4 + 4 ; documents 5 ; notifications 6 ; audit 0 ; bourse 9 ; open-banking-conseiller 13 + 4 + 4 + 5)

**Given** un virement effectué puis `make reset-data`
**When** je relis les comptages
**Then** ils sont revenus à l'état de référence (aucune ligne ajoutée par le virement ne subsiste)

**Given** les données de référence chargées
**When** je vérifie la date de référence (point ouvert §8.9, AR-82) par `scripts/golden-master/verifier-date-reference.sh`
**Then** le script sort en code 0 : `2026-11-17T10:00:00` (Europe/Paris) est postérieure à toutes les dates des 8 jeux de référence chargés (équivalent d'`import.sql`), et le compte de `user1` a au plus 8 opérations datées du 17/11/2026, avant la première exécution de SC-003-04
**And** lancé avec `HORLOGE_DEPART=2020-01-01T10:00:00`, le même script sort en code non nul en nommant la table et la date postérieures à la date de référence

**Given** les données de référence chargées
**When** je les interroge
**Then** elles contiennent ce qu'exige le golden master : comptes `1` et `2`, soldes et 5 dernières opérations du COURANT de `user1`, bénéficiaire `FR76***` (SC-013-01), portefeuille de `sophie` (Jean Dupont / `user1`, Marie Lebrun / `user2`, SC-015-01), watchlist `BNP.PA` et `AI.PA` (SC-009-01), « CEL - Compte Epargne Logement » et « Livret A » (SC-011-01), compte interne `FR76-0001-0002` (SC-004-02) ; et un compte de test par rôle (`admin` ADMIN, `sophie` CONSEILLER, `user1` CLIENT) dont le mot de passe n'est stocké que haché, généré depuis `RETRO_ADMIN_PASSWORD`, `RETRO_CONSEILLER_PASSWORD` et `RETRO_CLIENT_PASSWORD` (`git grep -n "$RETRO_CLIENT_PASSWORD"` ne trouve rien)

**Given** les variables `RETRO_*_PASSWORD` posées et l'application démarrée en profil `rejeu`
**When** je lance `cd e2e/golden-master && npx playwright test --project=connexion`
**Then** le code de sortie est 0 et `.auth/ADMIN.json`, `.auth/CONSEILLER.json`, `.auth/CLIENT.json` sont créés (dossier ignoré par git)
**And** sans `RETRO_ADMIN_PASSWORD`, la même commande sort en code non nul avec « variable d'environnement RETRO_ADMIN_PASSWORD absente »

**Given** l'application démarrée et les données remises à zéro
**When** je lance npx playwright test dans e2e/golden-master
**Then** le runner produit son rapport (`e2e/golden-master/report/index.html` et `e2e/golden-master/results.json`)

**Given** `scripts/rejeu-golden-master.sh` et la cible `make rejeu-golden-master`
**When** je lis le script
**Then** il exporte `TZ=Europe/Paris` et `LANG=fr_FR.UTF-8` avant tout lancement du runner (le `playwright.config.ts` généré ne porte ni `timezoneId: 'Europe/Paris'` ni `locale: 'fr-FR'` et n'est pas modifié ; point ouvert AR-103 réglé ainsi, sauf si une option de `export-golden-master-tests.py` est ajoutée et régénérée par la voie du skill `corpus-bmad-golden-master`)
**And** il enchaîne, depuis `e2e/golden-master` : `make reset-data`, puis `npx playwright test --grep-invert "<scénarios qui écrivent>" --reporter=blob` ; puis, pour chaque scénario `writes: true` lu dans les `scenario.json` dérivés (jamais une liste recopiée : SC-001-03, SC-001-05, SC-003-04, SC-004-02, SC-013-02 aujourd'hui), `make reset-data` puis `npx playwright test --grep "<SC>" --reporter=blob` ; enfin `npx playwright merge-reports --reporter=html,json ./blob-report` (point ouvert §8.10, AR-83)
**And** `e2e/golden-master/blob-report/` est ignoré par git (`.gitignore` racine)

**Given** `make reset-data` qui échoue (base `postgres-credits` arrêtée volontairement)
**When** je lance `make rejeu-golden-master`
**Then** le script s'arrête avant le lancement du runner suivant et sort en code non nul en nommant l'étape en échec

**Given** `make rejeu-up` (horloge de départ par défaut)
**When** je lance trois fois `make reset-data && (cd e2e/golden-master && npx playwright test --grep "SC-003-04")`
**Then** les trois rejeux de SC-003-04 sont verts (virement de 100 EUR exécuté, redirection et lignes attendues, notification CONFIRMATION reçue par Kafka sous 5 s)

**Given** `HORLOGE_DEPART=2026-11-17T23:30:00 make rejeu-up` (contre-épreuve, §7.2)
**When** je lance `make reset-data && (cd e2e/golden-master && npx playwright test --grep "SC-003-04")`
**Then** SC-003-04 échoue (virement suspendu par UNUSUAL_HOURS, FR-027-02) et la commande sort en code non nul : c'est l'horloge réglée, et non l'heure réelle, qui décide
**And** l'ensemble est ensuite redémarré avec `make rejeu-up` sans `HORLOGE_DEPART`

**Given** le code des services
**When** je lance `! grep -rnE '\b(Instant|LocalDate|LocalDateTime|LocalTime|ZonedDateTime|OffsetDateTime|OffsetTime|Year|YearMonth)\.now\(|Clock\.system|System\.currentTimeMillis\(' services/*/src/main --include=*.java | grep -vE '/adapter/out/(horloge|jeton)/'`
**Then** le code de sortie est 0

**Given** tous les critères ci-dessus verts
**When** l'humain relit `golden-master-architecture.md`
**Then** les points ouverts 9 et 10 du §8 et la configuration `timezoneId`/`locale` du §5.3 sont réglés, et le document peut passer en `status: final`

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0

### Story 10.3: Rejouer le golden master en entier et mesurer le critère de bascule

En tant que responsable de la bascule,
je veux rejouer une seule fois tout le golden master du legacy sur la nouvelle application et obtenir un rapport chiffré,
afin de décider la bascule sur une preuve de parité.

**Exigences :** NFR-PAR-01, NFR-PAR-05, AR-73, AR-74, AR-75, UX-DR152, UX-DR158, NFR-PAR-03
**Scénarios golden master :** SC-001-01, SC-001-02, SC-001-03, SC-001-04, SC-001-05, SC-002-01, SC-002-02, SC-003-01, SC-003-02, SC-003-04, SC-004-01, SC-004-02, SC-004-03, SC-005-01, SC-006-01, SC-007-01, SC-007-02, SC-008-01, SC-009-01, SC-010-01, SC-011-01, SC-012-01, SC-013-01, SC-013-02, SC-014-01, SC-015-01, SC-015-02, SC-016-01, SC-016-02, SC-017-01, SC-017-02 ; non rejoués : SC-003-03 (BUG-011), SC-010-02 (BUG-038)
**Prérequis :** story 10.2 ; développement terminé (aucune story en cours) ; variables `RETRO_*_PASSWORD` posées.

**Acceptance Criteria:**

**Given** le PRD et le corpus de rétro-ingénierie
**When** je lance `uv run docs/retro/tools/check-prd-coverage.py --prd _bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/prd.md --retro docs/retro --addendum _bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/addendum.md`
**Then** le code de sortie est 0 et la couverture des règles reprises est de 100 % (mesure SM-2, NFR-PAR-03)
**And** chaque FR du PRD est portée par au moins un test tagué `@Tag("FR-xxx-yy")` ou titré `[FR-xxx-yy]` : la liste des FR sans test, calculée par recherche des tags dans `services/*/src/test` et `frontend/src`, est vide

**Given** l'application démarrée par `make rejeu-up`
**When** je lance `make rejeu-golden-master`
**Then** le code de sortie est 0 et le rapport fusionné (HTML et JSON) compte, parmi les tests tagués `@golden-master`, 31 réussis, 0 en échec et 2 non rejoués (SC-003-03 et SC-010-02, annotés « Non rejoué » avec leur raison)
**And** le rapport JSON fusionné indique `failed: 0` (aucun test en échec, `stats.unexpected` = 0) et `errors: 0` (tableau `errors` vide)

**Given** les 5 scénarios HTTP
**When** ils sont rejoués sans session
**Then** SC-001-01 obtient 200 sur `GET /login`, SC-001-02 obtient 401 sur `GET /api/coeur-bancaire/tableau-de-bord`, SC-001-03 obtient 401 et `{"message":"Identifiants incorrects"}` sur `POST /api/identite-clients/connexion`, SC-014-01 obtient 302 sur `GET /api/open-banking-conseiller/powens/retour`, SC-017-01 obtient 401 sur `GET /api/documents/telechargement?type=RIB&compteId=1` (écarts voulus de `ecarts-voulus-golden-master.md`)

**Given** le rejeu terminé
**When** je compare l'arbre de travail
**Then** `git diff --exit-code -- e2e/golden-master docs/retro/golden-master/derived replay-mapping.json` sort en code 0 : aucun test généré, aucune sortie attendue ni aucune décision de rejeu n'a été modifié pour faire passer un scénario ; tout écart a été corrigé dans l'application, dans les données de référence ou dans la remise à zéro

**Given** un texte attendu retiré volontairement de l'écran d'administration (« Gestion des clients » renommé)
**When** je relance `make rejeu-golden-master`
**Then** le code de sortie est non nul et le rapport désigne SC-016-01 en échec, avec le texte attendu et le texte trouvé
**And** la violation est retirée et le rejeu complet relancé redonne 31 réussis, 0 en échec, 2 non rejoués

**Given** le rapport final
**When** le skill `corpus-bmad-golden-master` (« rejoue le golden master ») le lit
**Then** il restitue 31 réussis, 0 en échec, 2 non rejoués avec leur raison acceptée (bugs corrigés BUG-011 et BUG-038, couverts par les tests de FR-003-06 et FR-010-09), et le critère de bascule de NFR-PAR-01 est atteint

**Given** la story terminée
**When** je lance `scripts/gates/build.sh`, `scripts/gates/check-gates-exist.sh`, `scripts/gates/trace-smoke.sh` et `scripts/gates/golden-master.sh`
**Then** tous les contrôles de docs/quality-gates.md sont verts, et scripts/gates/golden-master.sh sort en code 0
