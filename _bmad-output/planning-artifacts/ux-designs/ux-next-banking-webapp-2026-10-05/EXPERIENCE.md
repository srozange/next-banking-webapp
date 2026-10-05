---
title: "EXPERIENCE : next-banking-webapp"
status: final
created: 2026-10-05
updated: 2026-10-05
sources:
  - _bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/prd.md
---

# next-banking-webapp : comportement de l'interface

> Rénovation à parité du legacy. Écrit en mode Fast path : aucune question posée, les suppositions sont marquées `[ASSUMPTION]`. Les textes affichés ne sont pas des choix de conception : ils viennent du legacy (`docs/retro/00-global/MESSAGES.md`, `docs/retro/features/FEAT-NNN/FIELDS.md`) ou d'une exigence du PRD. Quand un nouvel écran a besoin d'un texte que le PRD ne fixe pas, ce document le signale comme **point ouvert** et ne l'invente pas. Identité visuelle : `DESIGN.md`, qui hérite du design system importé `imports/design-system-purple-consulting.md`. En cas de conflit sur un comportement, ce document l'emporte ; sur un texte, MESSAGES.md et le PRD l'emportent.

## Foundation

- **Produit** : application bancaire web pour trois rôles (CLIENT, CONSEILLER, ADMIN). Le service est le même que celui du legacy (pile Java EE / JSF), plus les évolutions décidées dans le PRD : décision RG-027-003 (D1 à D12), décisions de finalisation B1 à B7 (B1 révisée : textes PENDING du legacy conservés pour la démonstration), et les FR marquées ÉVOLUTION.
- **Preuve de parité** : le golden master du legacy, rejoué une seule fois à la fin (NFR-PAR-01). 33 scénarios capturés (28 d'écran, 5 HTTP) ; SC-003-03 et SC-010-02, liés à des bugs à corriger, sont exclus, d'où **31 scénarios applicables : 26 d'écran et 5 HTTP**. Les 26 scénarios d'écran comparent le **texte rendu** (`innerText`) en mode « contient » ; parmi les 5 HTTP, SC-001-03 compare un fragment du corps (« Identifiants incorrects ») et les 4 autres le seul code de statut. Décompte détaillé : textes-nouveaux.md (« Périmètre du golden master ») ; divergences restantes : divergences-golden-master.md. La casse produite par le CSS du legacy, les accents absents, la ponctuation et les emoji font donc partie du contrat.
- **Surface** : web, desktop d'abord. Le design system fournit les breakpoints `sm 640` · `md 768` · `lg 1024` · `xl 1280`. [ASSUMPTION : la cible de référence est desktop ≥ `lg` ; sous `lg`, l'application reste utilisable pour consulter et pour remplir des formulaires simples, sans être optimisée pour le mobile ; aucun test sur mobile n'est prévu.]
- **Langue** : français uniquement (NFR-UI-01).
- **Thème** : clair uniquement. [ASSUMPTION : pas de mode sombre ; le design system n'en définit pas.]
- **Composants** : uniquement ceux du design system, plus les extensions de `DESIGN.md` (`transfer-status-badge-*`, `amount-*`, `notification-*`, `app-nav`). Aucun écran ne stylise directement un élément natif.
- **Rendu** : application monopage (SPA) qui appelle l'API interne (A-G-05). [ASSUMPTION : la forme exacte (React, routeur) est fixée par l'architecture ; ce document n'en dépend pas.]

### Attributs de test (NFR-UI-02)

Tout champ et tout bouton actionné par un scénario du golden master porte un `data-testid` stable. Tous les autres éléments interactifs en portent un aussi, ce qui ne coûte rien et prépare les scénarios des nouveaux écrans.

**Convention** : `data-testid="{écran}-{élément}[-{identifiant métier}]"`
- minuscules ASCII, sans accent, mots séparés par `-` ;
- `{écran}` est l'un des préfixes suivants : `login`, `nav`, `dashboard`, `virement`, `vp` (virements programmés), `releve`, `budget`, `pret`, `documents`, `bourse`, `chat`, `produits`, `tiers`, `beneficiaires`, `conseiller`, `admin`, `revue` (file de revue des virements suspendus) ;
- `{élément}` reprend le nom métier du champ (celui du FIELDS.md legacy, francisé), et jamais le texte affiché ni une position ;
- dans une liste, on ajoute l'identifiant métier de la ligne : `revue-liberer-{transferId}`, `admin-desactiver-{customerId}` ;
- zones de message : `{écran}-message-erreur`, `{écran}-message-succes`, `{écran}-message-info` ;
- un `data-testid` publié à l'étape G2 ne change plus jamais.

Correspondance avec les sélecteurs que le golden master actionne (elle alimente le `replay-mapping` de l'étape G2) :

| Scénario | Sélecteur legacy | `data-testid` cible |
|---|---|---|
| SC-001-03 (rejoué en HTTP : ces sélecteurs servent à la connexion des sessions de test, pas au rejeu de SC-001-03) | `#loginForm:username` | `login-identifiant` |
| SC-001-03 | `#loginForm:password` | `login-mot-de-passe` |
| SC-001-03 | `input[name='loginForm:j_idt11']` | `login-me-connecter` |
| SC-001-05 | `a:has-text('Deconnexion')` | `nav-deconnexion` |
| SC-003-02/04 (SC-003-03 exclu) | `#virementForm:source` / `:target` / `:amount` / `:motif` | `virement-compte-source` / `virement-compte-destinataire` / `virement-montant` / `virement-motif` |
| SC-003-02/04 (SC-003-03 exclu) | `input[name='virementForm:j_idt23']` | `virement-executer` |
| SC-004-02 | `#vpForm:srcAcc` / `:montant` / `:freq` / `:targetNum` / `:targetLabel` / `:dateExec` / `:motif` | `vp-compte-debiteur` / `vp-montant` / `vp-frequence` / `vp-numero-beneficiaire` / `vp-libelle-beneficiaire` / `vp-date-premiere-execution` / `vp-motif` |
| SC-004-02/03 | `input[name='vpForm:j_idt29']`, `input[value='Creer le virement programme']` | `vp-creer` |
| SC-007-02 | `#simForm:simMontant` / `:simTaux` / `:simDuree` | `pret-sim-montant` / `pret-sim-taux` / `pret-sim-duree` |
| SC-007-02 | `input[value='Calculer']` | `pret-sim-calculer` |
| SC-010-02 (exclu, BUG-038 ; gardé pour les tests de la nouvelle application) | `#chatForm:userInput` / `#chatForm:sendBtn` | `chat-saisie` / `chat-envoyer` |
| SC-010-02 (exclu) | `.msg.user` (attente) | `chat-message-utilisateur` |
| SC-013-02 | `#addForm:newLabel` / `:newHolderName` / `:newIban` / `:newBic` / `:newBank` | `beneficiaires-libelle` / `beneficiaires-titulaire` / `beneficiaires-iban` / `beneficiaires-bic` / `beneficiaires-banque` |
| SC-013-02 | `input[value="Verifier l'IBAN"]` / `input[value='Ajouter le beneficiaire']` | `beneficiaires-verifier-iban` / `beneficiaires-ajouter` |
| SC-013-02 | `.alert-info` (attente) | `beneficiaires-message-info` |

Les listes déroulantes restent des `<select>` natifs, parce que le golden master fait des `select` par valeur. La valeur d'une option est l'identifiant du compte, comme dans le legacy.

## Information Architecture

### Écrans par rôle

Les écrans « client » s'ouvrent à tous les rôles, mais chacun n'y voit que ses propres données (FR-003-01, glossaire « Administrateur »). Un CONSEILLER ou un ADMIN y voit donc le plus souvent des listes vides. Écran d'arrivée après la connexion (FR-001-05) : CLIENT → Tableau de bord, CONSEILLER → Espace Conseiller, ADMIN → Administration. L'accès à un écran réservé à un autre rôle redirige, sans message, vers le tableau de bord (FR-001-11, FR-016-01).

| # | Écran cible | Écran legacy | FEAT | Rôles | Accès |
|---|---|---|---|---|---|
| 1 | Connexion | `login.xhtml` | FEAT-001 | public | URL d'entrée, redirection des accès non authentifiés |
| 2 | Tableau de bord | `dashboard.xhtml` | FEAT-002, FEAT-025, FEAT-027 | CLIENT, CONSEILLER, ADMIN | header « Tableau de bord » |
| 3 | Virement | `virement.xhtml` | FEAT-003, FEAT-027 | tous | header « Virement » |
| 4 | Refus pour erreur du contrôle | (nouveau) | FEAT-003 (FR-003-27) | tous | résultat de « Exécuter le virement », sans entrée de menu |
| 5 | Virements programmes | `virement-programme.xhtml` | FEAT-004, FEAT-021 | tous | header |
| 6 | Releve | `releve.xhtml` | FEAT-005 | tous | header, lien « Voir le releve complet » |
| 7 | Budget | `budget.xhtml` | FEAT-006 | tous | header |
| 8 | Prets | `pret.xhtml` | FEAT-007, FEAT-024 | tous | header |
| 9 | Documents | `documents.xhtml` | FEAT-008, FEAT-017 | tous | header ; les PDF s'ouvrent dans un nouvel onglet |
| 10 | Bourse | `bourse.xhtml` | FEAT-009, FEAT-029 | tous | header |
| 11 | Mon Conseiller (chat) | `conseiller.xhtml` | FEAT-010 | tous | header, sub-header « Conseiller IA » de l'espace conseiller |
| 12 | Produits | `produits.xhtml` | FEAT-011, FEAT-018 | tous | header, lien « Voir la page Produits » de l'administration |
| 13 | Comptes Tiers | `comptes-tiers.xhtml` | FEAT-012, FEAT-028 | tous | header, liens du tableau de bord |
| 14 | Beneficiaires | `beneficiaires.xhtml` | FEAT-013 | tous | header, lien « Virement SEPA externe — Gérer mes bénéficiaires » |
| 15 | Retour Powens (transit, sans rendu) | `powens-callback.xhtml` | FEAT-014 | authentifié | redirection du fournisseur, puis vers Comptes Tiers |
| 16 | Espace Conseiller | `conseiller-workspace.xhtml` | FEAT-015 | CONSEILLER, ADMIN | header « Espace Conseiller » (conditionnel) |
| 17 | Administration, avec la nouvelle section de revue des virements suspendus | `admin.xhtml` | FEAT-016, FEAT-018, FEAT-019, FEAT-026, FEAT-027 | ADMIN | header « Administration » (conditionnel) |

Hors écran : les erreurs de téléchargement de documents (FR-017, « Parametre type manquant »…) sont des réponses HTTP en texte brut, comme dans le legacy, et non un écran conçu.

### Navigation (2 niveaux au plus : `{components.header}` + `{components.sub-header}`, via `{components.app-nav}`)

- **Header (niveau 1)**, sur toutes les pages authentifiées :
  - à gauche, le logo texte « DB » et « Demo Bank » ;
  - au centre, les 12 liens du legacy dans leur ordre et avec leur orthographe : « Tableau de bord », « Virement », « Virements programmes », « Releve », « Budget », « Prets », « Documents », « Bourse », « Mon Conseiller », « Produits », « Comptes Tiers », « Beneficiaires » ; puis « Espace Conseiller » (CONSEILLER, ADMIN) et « Administration » (ADMIN) ;
  - à droite, « Bonjour, {fullName} » et « Deconnexion » ;
  - le badge de rôle (« ADMIN », « Conseiller ») est un `{components.tag}`.
- **Sub-header (niveau 2)**, uniquement dans les deux espaces qui avaient leur propre menu dans le legacy :
  - Espace Conseiller : « Mon portefeuille » · « Conseiller IA » ;
  - Administration : « Administration » · « Espace Conseiller » · « Mon tableau de bord ».
- **Pages client** : pas de sub-header. Aucun libellé de regroupement n'est créé, car ce serait un texte nouveau.
- **Élément actif** : soulignement 2px `{colors.primary}`, et `aria-current="page"`.
- **Sous `xl`** : les liens du header passent sur deux lignes ; le header grandit, sans menu repliable. [ASSUMPTION : un menu « hamburger » demanderait un nom accessible nouveau (« Menu ») ; il est écarté. Voir les points ouverts.]
- **Bandeaux techniques** du legacy (CMP-002) : supprimés. Voir la section « Écarts de texte avec le legacy », partie B.

## Voice and Tone

**Règle n°1 : le wording du legacy est intouchable.** Aucun libellé, message, titre, bouton, placeholder ni état vide ne change par rapport au legacy (`docs/retro/00-global/MESSAGES.md`, `docs/retro/features/FEAT-NNN/MESSAGES.md` et `FIELDS.md` font foi). Cela vaut aussi pour :
- la casse affichée ;
- les accents absents (« Deconnexion », « Releve », « Prets », « Beneficiaires ») ;
- la ponctuation, les espaces et les tirets (« — » ou « - ») ;
- les emoji (✅ ⚠️ 🚨 📈 📋 ⭐ ➕ 🌐) ;
- les formats de nombre et de date.

Le golden master est rejoué à la fin. Un texte modifié en cours de route n'est découvert qu'à ce moment-là.

Les seuls écarts autorisés sont ceux décidés par une exigence du PRD. Ils sont tous listés dans la section « Écarts de texte avec le legacy », et la liste est fermée. Aucun texte n'est amélioré, même en supposition. Un texte qui manque à un nouvel écran est un **point ouvert**, à trancher par le PM avant la story concernée.

Conséquences pratiques :
- **Casse rendue** : le legacy met en capitales, par CSS, ses séparateurs de section et ses en-têtes de tableau (« MES COMPTES DEMO BANK », « DATE DESCRIPTION TYPE MONTANT SOLDE APRES »), et le golden master capture ce rendu. Ces séparateurs utilisent donc `{components.eyebrow}`, qui est en capitales. Les en-têtes de tableau (`{components.table-header}`) passent en capitales sur les écrans où le legacy le faisait. Le texte source reste dans sa casse d'origine : seul le CSS le transforme.
- **Formats** : les montants et les dates gardent le format du legacy écran par écran, même là où il est anglo-saxon (simulateur de prêt : « 293.02 EUR », « 10,548.72 EUR » ; tableau de bord : `EURx,xxx.xx`). Le format « 1 234,56 € » de `{components.amount}` (DESIGN.md) ne s'applique pas à un texte comparé par le golden master. Voir les points ouverts.
- **Emoji** : ils restent dans le texte. Une icône du design system peut les accompagner, avec `aria-hidden="true"`.
- **Orthographe** (décision du 2026-10-05) : textes nouveaux, orthographe correcte, accents compris, quel que soit l'écran (y compris l'avis de suspension A-027-4 et le message d'heure butoir A-016-6) ; textes du legacy, même cités par le PRD (par exemple les textes PENDING de FR-003-10), à l'identique, sans exception. Lot des textes nouveaux : textes-nouveaux.md.

## Component Patterns

Correspondance entre le kit du legacy (`UI-KIT.md`) et le design system. Les tokens visuels sont dans DESIGN.md ; ce tableau fixe le comportement.

| # | Composant legacy | Composant du design system | Règles de comportement |
|---|---|---|---|
| 1 | CMP-001 Barre de navigation | `{components.app-nav}` = `{components.header}` + `{components.sub-header}` | Entrées filtrées par rôle côté serveur et côté client ; lien actif `aria-current` ; « Deconnexion » ferme la session et revient à la Connexion. |
| 2 | CMP-002 Bandeau technique | aucun | Supprimé (A-001-3, A-002-4, A-003-6, A-011-2, A-018-3). |
| 3 | CMP-003 Tableau de données | `{components.table-header}`, `{components.table-cell}`, `{components.table-row-hover}` | `<table>` natif avec `<th scope="col">` ; montants alignés à droite (`{components.amount}`) ; dans un conteneur à défilement horizontal sous `lg`. |
| 4 | CMP-003 État vide d'un tableau | une ligne `{components.table-cell}` sur toute la largeur | Texte legacy exact (« Aucune transaction pour ce compte. », « Aucun pret en attente »…). Pas d'`{components.empty-state}` avec icône et bouton, qui ajouterait du texte. |
| 5 | CMP-003 Actions inline (Supprimer, Suspendre/Reactiver, Approuver/Refuser, Desactiver/Activer, Confirmer/Faux positif) | `{components.button-ghost}` (48px) | Action directe sans modale de confirmation, comme dans le legacy (FIELDS FEAT-016) ; bouton désactivé (`{components.button-disabled}`, `aria-disabled`) pendant la requête ; la variante compacte de 40px est interdite (cible 44px minimum). |
| 6 | CMP-003 Sous-tableau (échéancier) | tableau dans une `{components.card-muted}` | S'ouvre sous la ligne par « Voir » ; le focus va au titre du sous-tableau. |
| 7 | CMP-004 Combobox avec option vide | `{components.select}` (`<select>` natif) | Première option legacy (« -- Choisir un compte -- », « -- Choisir -- », vide sur le Relevé) ; sur le Relevé, le changement de compte recharge les opérations sans recharger la page. |
| 8 | CMP-005 Panneau de détail inline | `{components.card}` inline sous le tableau | « Voir detail » / « Detail » l'ouvre ; « Fermer » et `Échap` le referment ; le focus va au titre « Detail : {nom} » et revient au bouton déclencheur à la fermeture. [ASSUMPTION : panneau inline et non modale, comme dans le legacy] |
| 9 | CMP-006 Badge de statut (CREDIT/DEBIT, Actif/Inactif, statuts de prêt, SEPA, OPEN/CONFIRMED…) | `{components.tag}` | Valeur affichée telle quelle ; la couleur double le texte, elle ne le remplace jamais ; DEFAUT sans badge (BUG-053 conservé). |
| 10 | Badge d'état d'un virement (nouveau, FR-027-16) | `{components.transfer-status-badge-*}` | Voir les états d'un virement dans State Patterns. |
| 11 | CMP-007 Carte KPI | `{components.card-muted}` + `{components.amount-neutral}` | Valeur, libellé et sous-libellé legacy ; lecture seule. |
| 12 | CMP-008 Champ de formulaire | `{components.input}` + `{components.input-helper}` | `<label for>` relié ; placeholder legacy conservé, mais jamais en guise de libellé. |
| 13 | CMP-008 Erreur de champ (`h:message for`) | `{components.input-error}` | Message sous le champ, relié par `aria-describedby` ; `aria-invalid="true"`. |
| 14 | CMP-008 Messages globaux (`h:messages`, FacesMessage) | `{components.alert-error}` / `{components.alert-success}` / `{components.alert-info}` | Placés en haut du formulaire, comme dans le legacy ; `role="alert"` pour une erreur, `role="status"` pour un succès ou une info. |
| 15 | CMP-008 Bouton de soumission | `{components.button-primary}` | Un seul par vue ; désactivé avec `aria-busy` pendant l'envoi, ce qui empêche une double soumission (critique pour « Exécuter le virement ») ; pas de modale de confirmation (parité FIELDS FEAT-003). |
| 16 | CMP-009 Centre de notifications | `{components.notification}` + `-confirmation` / `-alert` / `-fraud` / `-low-balance` | Type, icône, message, horodatage `dd/MM/yyyy HH:mm:ss` ; pastille non lu avec l'infobulle « Non lu » ; « {N} nouvelle(s) », « Tout marquer lu », « Effacer tout » (FR-002-08). |
| 17 | CMP-010 Carte de document | `{components.card}` + `{components.tag}` + `{components.link}` | « Telecharger PDF » ouvre un nouvel onglet ; l'ouverture est signalée par du texte réservé aux lecteurs d'écran [voir les points ouverts : ce texte n'existe pas dans le legacy]. |
| 18 | CMP-011 Simulateur avec résultat repliable | `{components.card}` + bouton de dépliage (`{components.link}`) | « Voir l echeancier complet ({n} echeances) » porte `aria-expanded` et `aria-controls`. |
| 19 | Bulles de chat (FEAT-010) | Sophie : `{components.card-muted}` ; client : `{components.card}` | Nouvelle bulle annoncée (`aria-live="polite"`) ; pendant l'attente, le bouton « Envoyer » affiche « ... » (texte legacy) et se désactive. |
| 20 | Actions rapides du chat | `{components.button-ghost}` | Visibles seulement sur une conversation neuve. |
| 21 | Vérification d'IBAN (Ajax) | résultat en ligne `{components.alert-success}` / `{components.alert-error}` sous le champ | Annoncé par `aria-live="polite"` ; ne soumet pas le formulaire. |
| 22 | Formulaire repliable « + Ajouter une valeur » (Bourse) | `{components.button-secondary}` + `{components.card}` inline | « Annuler » referme et rend le focus au bouton. |
| 23 | Graphique de cours (SVG serveur, FEAT-009) | SVG dans une `{components.card}` | `role="img"` avec un nom accessible ; le tableau « 📋 Dernières séances » sert d'alternative textuelle. |
| 24 | Jauge de plafond (Budget) | barre `{colors.primary}`, `{colors.error}` au-delà du plafond (FR-006-09) | `role="progressbar"` avec `aria-valuenow` ; le texte « {n}% du plafond » reste affiché. |
| 25 | Filtre clients (Administration) | `{components.input}` + `{components.button-secondary}` « Filtrer » | Placeholder « Rechercher par nom, login, email... ». |
| 26 | Avatar initiale (cartes clients du conseiller) | pastille `{rounded.pill}` | Décoratif, `aria-hidden`. |
| 27 | Saisie du motif de revue (nouveau, FR-016-19) | `{components.modal}` | Une seule modale à la fois ; piège du focus ; `Échap` ferme. Voir la file de revue. |

## State Patterns

### États génériques

| État | Traitement |
|---|---|
| Chargement d'une page | Pas de squelette ni de texte de chargement, puisqu'aucun texte n'existe dans le legacy. La zone de contenu porte `aria-busy="true"` jusqu'à l'arrivée des données. Cible de moins de 200 ms en navigation (NFR-PERF-01). [ASSUMPTION] |
| Envoi d'un formulaire | Bouton de soumission désactivé avec `aria-busy` ; valeurs saisies conservées en cas d'erreur (FR-003-03). |
| Vide | Texte legacy exact, dans la ligne du tableau ou dans le bloc, sans icône ni action ajoutée. Exemples : « Aucun virement programme configure », « Aucune notification - effectuez un virement pour voir les messages publies via ActiveMQ Artemis en temps reel » (repris malgré le nom technique, FR-002-07). |
| Erreur de validation | Message du legacy ou du PRD, sous le champ ou en haut du formulaire selon l'emplacement du legacy, avec `{components.input-error}` ou `{components.alert-error}`, annoncé. |
| Erreur technique | Jamais de détail technique (NFR-SEC-04). Texte fixé par l'exigence du PRD concernée ; voir les écarts, partie B. |
| Fournisseur indisponible | Message de la FR concernée, dans la zone d'erreur de l'écran (FR-012-14, FR-010-09, message Bourse legacy) ; le reste de l'application fonctionne (NFR-INT-02). |
| Succès | Message legacy (« Virement programme cree avec succes », « Bénéficiaire ajouté avec succès. ») en `{components.alert-success}`, `role="status"`. Un virement exécuté redirige vers le tableau de bord, comme dans le legacy. |
| Session expirée (30 min, NFR-SEC-07) ou accès non authentifié | Redirection vers la Connexion (FR-001-02). |
| Rôle non autorisé | Redirection sans message vers le tableau de bord (FR-016-01). |

### États d'un virement (FR-027-16)

| État | Ce que voit le client | Composant | Débit |
|---|---|---|---|
| **EXÉCUTÉ**, statut SUCCESS | Redirection vers le tableau de bord ; notification CONFIRMATION ; deux opérations au relevé. | `{components.notification-confirmation}`, `{components.transfer-status-badge-executed}` | oui |
| **EXÉCUTÉ**, statut PENDING (de 5 000 € à 9 999,99 €) | Comme ci-dessus, avec les textes du legacy gardés sans reformulation pour la démonstration (B1 révisée, BUG-009 conservé provisoirement) : notification ALERTE « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. », mention « [EN ATTENTE] » et badge « En attente » au relevé. Reformulation reportée après la démonstration (FR-003-29). | `{components.notification-alert}` | oui |
| **SUSPENDU** | Avis de suspension avec heure butoir (FR-027-04), voir ci-dessous ; notification FRAUDE unique. | `{components.alert-warning}`, `{components.notification-fraud}`, `{components.transfer-status-badge-suspended}` | non |
| **LIBÉRÉ** | État pris à la libération, une fois le solde contrôlé (B6, FR-016-19) ; le débit est effectué ensuite par le traitement asynchrone, qui passe le virement à EXÉCUTÉ, ou à REJETÉ sans débit si le solde est devenu insuffisant entre-temps (B7). Visible dans la liste des virements du client, badge « Accepté - pas encore débité » ; aucune notification. | `{components.transfer-status-badge-released}` | pas encore (débit par le traitement asynchrone) |
| **REJETÉ** (par l'administrateur, à l'heure butoir, à la libération si le solde est insuffisant : la libération est alors refusée, décision B6 ; ou au débit asynchrone d'un virement LIBÉRÉ dont le solde est devenu insuffisant, décision B7) | État visible dans la liste, badge « Rejeté - non débité » (`virement.etat.rejete`) quelle que soit la cause ; aucune nouvelle notification (FR-002-10) ; aucun débit. | `{components.transfer-status-badge-rejected}` | non |
| **REFUSÉ_ERREUR_CONTRÔLE** | Écran de refus (FR-003-27) ; aucune notification (A-003-8). | `{components.alert-warning}`, `{components.transfer-status-badge-control-error}` | non |

Transitions visibles : de SUSPENDU vers LIBÉRÉ (solde suffisant à la libération) puis EXÉCUTÉ (débit par le traitement asynchrone) ou REJETÉ (solde devenu insuffisant au moment du débit, aucun débit, B7) ; de SUSPENDU vers REJETÉ (rejet, heure butoir, ou solde insuffisant à la libération, B6). Le client découvre le changement en rechargeant l'écran, sans push. [ASSUMPTION : pas de rafraîchissement automatique ; le legacy n'en a pas.] Libellés des badges : textes-nouveaux.md (`virement.etat.*`).

### État d'une échéance programmée (FR-021-10)

Une échéance suspendue devient un virement SUSPENDU, avec la même notification FRAUDE et la même heure butoir. Le virement programmé lui-même reste « Actif » dans le tableau de l'écran Virements programmes : il ne faut pas confondre cet « Actif »/« Inactif » avec l'état SUSPENDU.

## Interaction Primitives

- **Souris et clavier, sans raccourcis** : comme dans le legacy. `Tab` / `Maj+Tab` dans l'ordre de lecture ; `Entrée` soumet le formulaire qui a le focus ; `Espace` ou `Entrée` actionne un bouton ; `Échap` ferme la modale ou le panneau de détail ouvert.
- **Soumission explicite** : aucun formulaire ne se soumet à la saisie. Il y a trois exceptions, toutes reprises du legacy : le changement de compte du Relevé, « Verifier l'IBAN », et le sélecteur de valeur et de période de la Bourse.
- **Pas de confirmation avant le virement** (parité) ; la protection passe par la désactivation du bouton pendant l'envoi. Seules la libération et le rejet d'un virement suspendu ouvrent une modale, qui sert à saisir le motif obligatoire, pas à confirmer.
- **Saisie** :
  - montants au format du legacy (virgule décimale acceptée, « Ex : 100,00 ») ;
  - date de première exécution au format `yyyy-MM-dd` (BUG-012 conservé) ;
  - le motif de virement est facultatif, avec 100 caractères au plus (FR-003-01).
- **Mouvements** : durées du design system (`{motion.duration-fast}` pour les survols, `{motion.duration-slow}` pour la modale). Avec `prefers-reduced-motion`, tout passe à 0 ms.
- **Interdits** : défilement infini, glisser-déposer, actions visibles seulement au survol, plus d'une modale à la fois, navigation sur plus de 2 niveaux, contenu interactif dans un `{components.tooltip}`.

## Accessibility Floor

Plancher **WCAG 2.1 niveau AA**, sur les comportements. Les contrastes sont vérifiés dans DESIGN.md. Pas d'audit de conformité formel.

- **Clavier** : tout est utilisable au clavier, sans piège en dehors de la modale, qui piège le focus volontairement et le rend à la fermeture. Un lien d'évitement mène au contenu principal. [voir les points ouverts : son texte n'existe pas dans le legacy]
- **Focus visible** : `{components.focus-ring}` (2px `{colors.primary}`, offset 2px) sur `:focus-visible` pour chaque élément interactif.
- **Libellés** : chaque champ a un `<label for>` relié ; les champs obligatoires portent `aria-required` en plus de l'astérisque du legacy (« Libelle * »).
- **Erreurs** : le champ en erreur porte `aria-invalid="true"` et `aria-describedby` vers son message ; les messages globaux d'erreur sont en `role="alert"`, les succès en `role="status"`. Après une soumission en erreur, le focus va au premier champ en erreur, ou au message global s'il n'y en a pas.
- **Contenus qui changent** : `aria-live="polite"` pour le compteur d'opérations du Relevé, le résultat d'IBAN, les bulles du chat et le badge « {N} nouvelle(s) ».
- **Cibles** : 44 × 44 px au minimum, garantis par la hauteur de 48px des boutons, champs et listes du design system ; les variantes compactes de 40px sont interdites.
- **Tableaux** : `<th scope="col">` ; un `<caption>` (éventuellement masqué visuellement) ou un `aria-labelledby` vers le titre de section legacy ; pas de tableau pour la mise en page.
- **Couleur** : un état n'est jamais porté par la seule couleur. Il y a toujours un libellé, un signe (« + » / « - ») ou un texte (« Depassement »).
- **Mouvement** : `prefers-reduced-motion` est respecté.
- **Langue** : `lang="fr"` ; titre d'onglet « BankApp Demo - {page} » repris du legacy.
- **Point à confirmer (hors démonstrateur)** : l'Acte européen sur l'accessibilité (directive (UE) 2019/882), applicable aux services bancaires aux consommateurs depuis le 28 juin 2025 avec la norme harmonisée EN 301 549, et le RGAA ne s'imposeraient qu'à une mise en production, hors du périmètre du démonstrateur (§5 du PRD). Une mise en production exigerait un audit de conformité et une déclaration d'accessibilité.

## Key Flows

Les parcours sont ceux du PRD (§2.3). Seuls les écrans et les moments clés sont ajoutés ici.

### UJ-1 : Claire vire 1 200 € à son colocataire (FEAT-001, 002, 003, 025, 027)

1. Connexion : « Identifiant », « Mot de passe », « Me connecter ».
2. Tableau de bord : « Solde total Demo Bank », comptes, « Dernieres operations ».
3. Elle clique sur « Virement » dans le header.
4. Elle choisit le « Compte source » et le « Compte destinataire », saisit le « Montant (€) » et le « Motif ».
5. Elle actionne « Exécuter le virement ». Le bouton se désactive ; le contrôle de fraude s'exécute avant la validation (FR-027-02).
6. **Moment clé** : retour au tableau de bord. La notification CONFIRMATION « Virement de 1200.00 EUR vers le compte … confirme (motif : …) » est en tête du centre de notifications, marquée « Non lu », et la nouvelle opération DEBIT figure dans les « Dernieres operations ».

Variantes :
- **Solde insuffisant** : elle reste sur l'écran, les valeurs sont conservées, et le message « Solde insuffisant sur le compte source » s'affiche (FR-003-06).
- **Suspendu** : l'avis de suspension s'affiche avec l'heure butoir (voir les écrans nouveaux) ; rien n'est débité.
- **Erreur du contrôle** : l'écran de refus s'affiche (FR-003-27) ; elle refait sa demande plus tard en ressaisissant le virement, la reprise préremplie de FR-003-28 n'arrivant que dans une story suivante.

### UJ-2 : Marc prépare un rendez-vous (FEAT-015, 030)

1. Connexion, avec arrivée directe sur l'Espace Conseiller.
2. Il voit le bandeau « Bonjour {fullName}, vous gerez {N} client(s) » et les trois KPI.
3. Il parcourt les cartes clients : seuls ses clients affectés apparaissent.
4. Il actionne « Voir detail » sur la carte d'un client.
5. **Moment clé** : le panneau de détail s'ouvre sur la même page, et le focus va à son titre. Il y lit les « Comptes bancaires » et les « Prets » de son client sans quitter son portefeuille. La date « Client depuis » (BUG-055, FR-016-10) figure dans le panneau de l'administration, pas dans celui du conseiller (note du PRD sur FR-016-19).

### UJ-3 : Sophie traite la file du matin (FEAT-016, 019, 026)

1. Connexion, avec arrivée sur Administration.
2. Section « Alertes Fraude — Moteur de détection » : toutes les alertes, tous statuts confondus (BUG-056). Elle actionne « Confirmer » ou « Faux positif » ; la ligne affiche alors « Traité par {nom} ».
3. Section « Pipeline Prets — En attente d approbation » : « Approuver » ou « Refuser » ; le retour s'affiche dans un message (« Prêt approuvé »).
4. Section « Gestion des clients » : « Filtrer », puis « Desactiver » sur un client ; le message « Client désactivé » s'affiche.
5. **Moment clé** : le badge du client passe à « Inactif » dans la même ligne. Ce client ne peut plus se connecter (FR-001-07), et chaque action est auditée avec l'adresse IP (BUG-083), sans rien à faire de plus pour elle.

### UJ-4 : Claire agrège un compte d'une autre banque (FEAT-012, 014, 028)

1. Elle ouvre « Comptes Tiers », état non connecté (« Agregez vos comptes bancaires »).
2. Elle actionne « Connecter une banque » : redirection vers Powens.
3. Retour Powens : page de transit sans rendu, puis Comptes Tiers.
4. **Moment clé** : l'écran affiche « Connected via Powens », « Patrimoine total (tous comptes) » et « Mes comptes externes ({N}) », avec le premier compte sélectionné et ses transactions.

Variantes :
- **Solde absent** : « Solde indisponible » (FR-012-12).
- **Fournisseur en erreur** : message d'indisponibilité (FR-012-14).
- **Annulation** : message avec le motif (FR-012-07).
- **Mise à jour** : le bouton « Rafraichir » relit les comptes (FR-012-16).

### UJ-5 : Sophie traite les virements suspendus (FEAT-016, 026, 027)

1. Elle ouvre Administration, puis la section de revue des virements suspendus, triée par heure butoir la plus proche (A-016-3).
2. Elle lit la ligne du virement de 12 000 € de Claire : client, montant, compte destinataire, motif de la suspension, heure butoir.
3. Elle actionne l'action de libération. Une modale s'ouvre ; elle saisit le motif « Client joint par telephone » et valide.
4. **Moment clé** : le solde du compte source est contrôlé ; il suffit, la ligne quitte la file avec « Libération enregistrée : le virement est accepté, son débit va être effectué. ». Le virement passe à LIBÉRÉ, puis le traitement asynchrone le débite et le passe à EXÉCUTÉ, sans notification au client (B6, FR-016-19). Dans la section des alertes, les alertes OPEN de ce virement passent à REVIEWED avec « Traité par {son nom} », dans la même action (B4).
5. Elle rejette un autre virement avec le motif « Beneficiaire inconnu du client » : il passe à REJETÉ, sans débit.

Variantes :
- **Heure butoir atteinte** : « Heure butoir dépassée : le virement est rejeté. » ; l'heure butoir fait foi, que le traitement planifié soit passé ou non (B5).
- **Solde devenu insuffisant** : la libération est refusée et le virement passe à REJETÉ, sans aucun débit : « Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit. » (décision B6).
- **Solde devenu insuffisant après la libération** : le traitement asynchrone ne débite rien et passe le virement de LIBÉRÉ à REJETÉ (décision B7). Sophie le retrouve dans l'historique des virements traités, avec le motif « Rejeté : solde insuffisant au moment du débit » (`revue.motif.rejet.solde.debit`) ; Claire voit « Rejeté - non débité », sans notification.
- **Motif vide** : l'action est refusée (FR-016-19) avec « Veuillez indiquer un motif ».

## Écrans nouveaux ou modifiés par les décisions

Les textes cités viennent du PRD. Tout texte que le PRD ne fixe pas est signalé « [texte non fixé : point ouvert] » et ne doit pas être inventé à l'implémentation. Ces textes sont désormais fixés par le lot textes-nouveaux.md (validé avec corrections le 2026-10-05) : chaque mention « point ouvert » de cette section renvoie à la clé correspondante de ce lot.

### 1. Avis de virement suspendu (FR-027-04, FR-023-04, FR-021-10)

- **Où** : sur l'écran Virement, après « Exécuter le virement », et non par une redirection vers le tableau de bord. Un virement suspendu n'est pas exécuté ; la redirection reste réservée à EXÉCUTÉ. [ASSUMPTION] L'avis prend la forme d'un `{components.alert-warning}` en haut du formulaire, en `role="status"`, et le focus y est placé. Le formulaire est vidé. [ASSUMPTION]
- **Contenu requis par le PRD** :
  - le virement est en cours de vérification ;
  - rien n'a été débité ;
  - une réponse sera donnée avant l'heure butoir (date et heure, soumission + 24 h) ;
  - à défaut de décision à cette heure, le virement sera rejeté sans débit ;
  - ni le mot « fraude » ni le nom d'un signal.
- **Texte** : celui de la notification (A-027-4), écrit avec l'orthographe correcte (décision du 2026-10-05), repris à l'identique pour l'avis à l'écran (`virement.avis.suspension`) : « Votre virement de {montant} EUR vers le compte {numCompte} est en cours de vérification. Rien n'a été débité. Vous aurez notre réponse avant le {date} à {heure}. Sans décision à cette heure, il sera rejeté sans aucun débit. »
- **Notification** : une seule, FRAUDE, dans le centre de notifications, avec le texte ci-dessus (`{components.notification-fraud}`).
- **Heure butoir** : elle reste visible ensuite dans la liste des virements du client (voir le point 4).
- **`data-testid`** : `virement-avis-suspension`, `virement-avis-heure-butoir`.

### 2. Écran de refus pour erreur du contrôle (FR-003-27, NFR-OBS-02)

- **Où** : une vue de résultat dédiée dans le parcours Virement (écran n°4 de l'IA), affichée à la place du formulaire. Un `{components.alert-warning}` porte le texte : il ne s'agit ni d'une erreur de saisie ni d'un refus de la banque. [ASSUMPTION sur le choix de l'alerte avertissement]
- **Texte imposé, mot pour mot** : « Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence {référence}. »
- **Référence** : l'identifiant de trace du virement. Le texte est sélectionnable et dans une police à chasse fixe [ASSUMPTION], avec `data-testid="virement-refus-reference"`.
- **Ce que l'écran ne contient pas** : aucune mention de fraude ni de signal, aucune notification, aucun solde modifié.
- **Retour au formulaire** : le header (« Virement ») suffit et n'ajoute aucun texte. La reprise préremplie (FR-003-28, story suivante) demandera un bouton dont le libellé n'est pas fixé. [texte non fixé : point ouvert]
- **Focus** : il va sur le titre de l'écran (« Effectuer un virement », texte legacy), et l'alerte est en `role="alert"`.

### 3. File de revue des virements suspendus (FR-016-19, FR-027-17, B4, B5)

- **Où** : une nouvelle section de l'écran Administration, réservée à ADMIN, placée avant « Alertes Fraude — Moteur de détection » et donc après « Gestion des clients » (contenu comparé par SC-016-01, à garder dans les 5 000 premiers caractères de la page), sans entrée de menu nouvelle. [ASSUMPTION : une section plutôt qu'un écran, pour ne créer aucun libellé de navigation.] Titre de section : [texte non fixé : point ouvert].
- **Contenu** (FR-016-19) : les virements suspendus, immédiats ou issus d'une échéance programmée, triés par heure butoir la plus proche (A-016-3).
- **Colonnes** (`{components.table-header}`, `<th scope="col">`) :
  - client ;
  - montant (`{components.amount-neutral}`) ;
  - compte destinataire ;
  - motif de la suspension : signaux et score cumulé, OU montant supérieur ou égal au seuil de fraude, OU dépassement du cumul glissant (après FR-027-18) ;
  - heure butoir (date et heure) ;
  - actions.

  Les intitulés des colonnes et la forme du motif de suspension ne sont pas fixés. [texte non fixé : point ouvert]
- **Actions** : libérer et rejeter, en `{components.button-ghost}` 48px, avec `data-testid` `revue-liberer-{transferId}` et `revue-rejeter-{transferId}`. Leurs libellés ne sont pas fixés (le PRD n'emploie que les verbes « libérer » et « rejeter »). [texte non fixé : point ouvert]
- **Motif obligatoire** :
  - chaque action ouvre une `{components.modal}` avec un champ motif (`{components.input}`, `aria-required`, `data-testid="revue-motif"`) et un bouton de validation (`revue-valider`) ;
  - un motif vide est refusé côté client et côté serveur, avec `aria-invalid` et un message [texte non fixé : point ouvert] ;
  - les libellés du champ, du titre de la modale et des boutons ne sont pas fixés [point ouvert].
- **Après la décision** :
  - la ligne quitte la file ;
  - les alertes OPEN du virement passent à REVIEWED dans la même action (B4), et la section des alertes affiche « Traité par {nom} » sur ces lignes après rechargement de la section ;
  - les alertes déjà CONFIRMED ou FALSE_POSITIVE ne changent pas (A-016-5) ;
  - la décision est auditée avec l'identité de l'administrateur et le motif ;
  - le message de succès n'est pas fixé [texte non fixé : point ouvert].
- **Contrôle du solde à la libération** (décision B6, FR-016-19) : à l'instant de la libération, le solde du compte source est contrôlé.
  - Solde insuffisant : la libération est refusée et le virement passe à REJETÉ, sans aucun débit ; message `revue.erreur.solde.insuffisant` « Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit. ».
  - Solde suffisant : le virement passe à LIBÉRÉ et le traitement asynchrone effectue ensuite le débit et le crédit, puis le passe à EXÉCUTÉ ; message `revue.succes.liberer` « Libération enregistrée : le virement est accepté, son débit va être effectué. » (il ne dit pas « débité »).
  - Solde devenu insuffisant entre la libération et le débit asynchrone (décision B7, FR-027-16) : aucun débit, le virement passe de LIBÉRÉ à REJETÉ, sans notification ; l'audit le trace TRANSFER_REJECT_INSUFFICIENT_FUNDS, acteur SYSTEM (FR-026-03).
- **Historique des virements traités** (FR-016-19, B7, A-016-7) : sous la file, la liste des virements libérés ou rejetés, avec leur état et le motif de leur issue. Un virement rejeté au débit y porte le motif `revue.motif.rejet.solde.debit` « Rejeté : solde insuffisant au moment du débit », distinct d'un rejet par l'administrateur et d'un rejet à l'heure butoir. La liste n'apparaît que si elle contient au moins un virement : avec le jeu de données du legacy, elle est absente de l'écran comparé par SC-016-01. Titre, colonnes et profondeur : [texte non fixé : point ouvert, A-016-7].
- **L'heure butoir fait foi (B5)** :
  - le serveur décide, jamais l'horloge du navigateur ;
  - une libération ou un rejet demandé à l'heure butoir ou après renvoie « Heure butoir dépassée : le virement est rejeté. » (A-016-6) en `{components.alert-error}` ; la ligne quitte la file au statut REJETÉ ;
  - une ligne dont l'heure butoir est passée mais que le traitement planifié n'a pas encore rejetée garde ses actions actives, et c'est le serveur qui répond par ce message. [ASSUMPTION : ne pas masquer l'action côté client, pour ne pas dépendre de l'horloge locale.]
- **File vide** : texte non fixé [point ouvert].
- **Accès** : aucun autre rôle ne voit la section, et l'API refuse les tentatives (FR-016-19).

### 4. Affichage des états d'un virement pour le client (FR-027-16)

- **Où** : sur l'écran Virement et sur le Tableau de bord (A-027-10), en bas de page, après le contenu repris du legacy : le rejeu du golden master ne lit que les 5 000 premiers caractères de la page, et les lignes attendues ne doivent pas en être repoussées. C'est une liste des virements du client passés par une revue : SUSPENDU, LIBÉRÉ, REJETÉ, ainsi qu'EXÉCUTÉ après une libération. [ASSUMPTION : les virements exécutés directement n'y figurent pas, puisqu'ils sont déjà visibles au relevé ; les refus pour erreur du contrôle n'y figurent pas non plus, puisqu'ils ont leur propre écran.]
- **Colonnes** : date de soumission, compte destinataire, montant, état (`{components.transfer-status-badge-*}`) et, pour SUSPENDU, l'heure butoir.
- **Mise à jour** : le changement d'état n'envoie aucune notification ; le client le voit au rechargement.
- **Textes** : titre de la section, intitulés des colonnes, libellés des badges des cinq états et état vide sont fixés dans textes-nouveaux.md (`virement.revue.*`, `virement.etat.*`). Contrainte : aucun libellé nouveau n'emploie « en attente » pour un virement exécuté, et aucun ne mentionne la fraude (FR-027-04).
- **`data-testid`** : `dashboard-virements-revue`, `virement-virements-revue`, `virement-etat-{transferId}`.

### 5. Virement important exécuté (statut PENDING) : textes du legacy gardés pour la démonstration (B1 révisée : FR-003-10, FR-005-07, FR-002-10)

- **Pour la démonstration**, les textes PENDING et ALERTE du legacy sont gardés sans reformulation (BUG-009 conservé provisoirement, risque R-4 du PRD) :
  - notification ALERTE (`{components.notification-alert}`) : « Virement de {montant} EUR en attente de validation - transaction importante, verification en cours. » ;
  - mention « [EN ATTENTE] » en tête de la description des opérations (débit et crédit) ;
  - Relevé (FR-005-07) : si la description commence par « [EN ATTENTE] » suivi d'une espace, l'écran affiche le badge « En attente » (`{components.tag}`) avant le libellé, retire les 13 caractères et place la ligne sur fond jaune. [ASSUMPTION : le fond jaune du legacy est rendu par `{colors.warning-container}`.]
- **Reformulation « transaction importante »** (notification « … execute - transaction importante. », mention « [TRANSACTION IMPORTANTE] », badge « Transaction importante ») : reportée après la démonstration (FR-003-29), elle fera l'objet d'une story ; elle ne fait pas partie des écarts de texte ci-dessous.

## Écarts de texte avec le legacy

Liste fermée. Chaque ligne est justifiée par une exigence du PRD, et elle alimentera le `text_map` du golden master à l'étape G2. Tout autre texte est identique au legacy (règle n°1 de Voice and Tone). « (absent) » désigne un texte nouveau sans équivalent legacy ; « (supprimé) » un texte legacy qui n'est plus affiché.

### A. Évolutions décidées (décision RG-027-003 D1-D12, décisions B2-B7, FR marquées ÉVOLUTION)

Les écarts « transaction importante » (anciens A1, A3 et A4 : notification ALERTE reformulée, mention « [TRANSACTION IMPORTANTE] », badge « Transaction importante ») sont retirés : reporté après la démonstration (FR-003-29). Les textes PENDING et ALERTE du legacy restent à l'identique (décision B1 révisée). Les numéros des autres lignes sont conservés.

| # | Texte legacy | Nouveau texte | Écran | Exigence |
|---|---|---|---|---|
| A2 | `Virement de {montant} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation.` (MSG-002-043 / MSG-025-001, FRAUDE) | `Votre virement de {montant} EUR vers le compte {numCompte} est en cours de vérification. Rien n'a été débité. Vous aurez notre réponse avant le {date} à {heure}. Sans décision à cette heure, il sera rejeté sans aucun débit.` | Tableau de bord (notification) ; Virement (avis `virement.avis.suspension`) | FR-027-04 (D2, D6, D7, A-027-4), FR-023-04, FR-021-10 (B2) |
| A5 | `Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié.` | (supprimé) ; remplacé par l'avis A2 | Virement | FR-003-16 (D2, D6), FR-027-04 |
| A6 | (absent) | `Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence {référence}.` | Refus pour erreur du contrôle | FR-003-27 (D3, D9) |
| A7 | (absent) | `Heure butoir dépassée : le virement est rejeté.` | Administration (file de revue) | FR-016-19 (B5, A-016-6) |
| A8 | Notification FRAUDE ou ALERTE choisie selon le seul montant (ex. ALERTE pour 5 000 € à 9 999,99 €, même suspect) | Type selon l'issue du contrôle puis le montant (un virement suspendu sur score reçoit FRAUDE quel que soit son montant) | Tableau de bord | FR-002-10 (D1, D2) |
| A9 | (absent) | Titre `Solde insuffisant sur votre compte`, message `Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte.` (notification enfin affichée) | Tableau de bord (centre de notifications) | FR-025-08 (B3) |
| A10 | (absent) | `Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit.` (libération refusée, virement REJETÉ sans débit) | Administration (file de revue) | FR-016-19 (B6) |
| A11 | (absent) | `Rejeté : solde insuffisant au moment du débit` (motif d'un virement libéré puis rejeté au débit, texte nouveau à valider) | Administration (historique des virements traités) | FR-016-19, FR-027-16 (B7) |

### B. Corrections de bugs et retraits de mentions techniques décidés par une FR du PRD (NFR-UI-01 : « sauf correction d'un bug à corriger » ; NFR-SEC-04)

| # | Texte legacy | Nouveau texte | Écran | Exigence |
|---|---|---|---|---|
| B-1 | `Erreur lors du virement : java.lang.IllegalStateException: Solde insuffisant` | `Solde insuffisant sur le compte source` | Virement | FR-003-06 (BUG-008, BUG-011, A-003-2) |
| B-2 | `Erreur lors du virement : java.lang.IllegalArgumentException: Le montant doit etre positif` | `Erreur lors du virement : Le montant doit etre positif` | Virement | FR-003-04 (A-003-1) |
| B-3 | `Erreur lors du virement : java.lang.IllegalArgumentException: Compte introuvable` | `Erreur lors du virement : Compte introuvable` | Virement | FR-003-05 (A-003-1) |
| B-4 | `Je suis momentanement indisponible (HTTP 401). Veuillez reessayer.` (et 403) | `Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence.` | Mon Conseiller | FR-010-09 (BUG-038, A-010-1) |
| B-5 | (retour silencieux) | `La connexion a votre banque n a pas abouti ({motif}). Veuillez recommencer la connexion.` | Comptes Tiers | FR-012-07 (BUG-040, BUG-044, A-012-1) |
| B-6 | `0,00 EUR` (solde absent) | `Solde indisponible` | Comptes Tiers | FR-012-12, FR-028-09 (BUG-042, A-012-2) |
| B-7 | `Aucune transaction disponible.` / liste vide sur une erreur Powens | `Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants.` | Comptes Tiers | FR-012-14, FR-028-11 (BUG-043, BUG-089, A-012-3) |
| B-8 | (absent) | Bouton `Rafraichir` | Comptes Tiers | FR-012-16 (A-012-5) |
| B-9 | (chargement des comptes avec `code`) | `Callback Powens incomplet. Parametres recus: [code]` | Comptes Tiers | FR-014-04, FR-028 (A-014-1) |
| B-10 | (connexion acceptée) | `Identifiants incorrects` / `Vérifiez votre login et mot de passe` (compte désactivé) | Connexion | FR-001-07 (BUG-001, A-001-1) |
| B-11 | `Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes)` (affiché à tort en cas d'échec) | `Service credit temporairement indisponible, veuillez reessayer plus tard` | Prets | FR-024-02 (BUG-077, A-024-1) |
| B-12 | `Erreur : {e.getMessage()}` / `Erreur suppression : {e.getMessage()}` (MSG-006-073/074) | `Une erreur est survenue. Veuillez reessayer.` | Budget | FEAT-006 NFR (A-006-1) |
| B-13 | `Erreur : {e.getMessage()}` (MSG-016-134, prêt) | `Erreur lors du traitement du prêt. Veuillez réessayer.` | Administration | FR-016-07 (A-016-1) |
| B-14 | `Traitements nocturnes (EJB @Schedule)` / `Historique des traitements nocturnes (EJB @Schedule)` | `Traitements nocturnes` / `Historique des traitements nocturnes` | Produits | FR-011 (A-011-2) |
| B-15 | `Traitements nocturnes (EJB Timer @Schedule)` | `Traitements nocturnes` | Administration | FR-018 (A-018-3) |
| B-16 | `⚠️ Système Legacy — JEE 8 / WildFly 14` | (supprimé) | Connexion | A-001-3 |
| B-17 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 / ActiveMQ Artemis (MDB)` | (supprimé) | Tableau de bord | A-002-4 |
| B-18 | `Notifications temps reel (Message Broker)` ; `ActiveMQ Artemis - MDB JEE - queue/virements` ; suffixe `- publie via ActiveMQ Artemis (MDB)` | (supprimés) | Tableau de bord | FR-025-05, A-002-4 |
| B-19 | Texte explicatif des combinaisons de notifications (CONFIRMATION + ALERTE, CONFIRMATION + FRAUDE ; texte exact : `docs/retro/features/FEAT-002/`) | (supprimé) | Tableau de bord | FR-002-10 (BUG-007) |
| B-20 | `Système Legacy — Virement géré par EJB CMT (Container Managed Transaction)` ; `Après validation, une notification sera publiée sur queue/virements (ActiveMQ Artemis) et visible dans le tableau de bord.` | (supprimés) | Virement | A-003-6 |
| B-21 | `virementForm:source: Validation Error: Value is required.` (et `:target`, `:amount`, `:motif`) | `Veuillez sélectionner les deux comptes` (comptes) ; `Veuillez saisir le montant` (montant) ; aucun message pour le motif, facultatif (textes-nouveaux.md) | Virement | A-003-6, NFR-SEC-04 |
| B-22 | `Système Legacy — Requête via @NamedQuery JPA` | (supprimé) | Relevé | note FEAT-005 |
| B-23 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Categorisation automatique par mots-cles` | (supprimé) | Budget | note FEAT-006 |
| B-24 | Bandeau `Legacy - … Scoring credit via ActiveMQ Artemis (MDB) …` et `La demande sera envoyee sur queue/pretDemandesQueue et traitee de facon asynchrone par le service de scoring credit (MDB WildFly).` | (supprimés) | Prets | note FEAT-007 |
| B-25 | `Legacy - … Generation PDF via OpenPDF (LGPL) + Servlet de telechargement` ; `DocumentDownloadServlet : /banking-legacy/docs/download?type=RELEVE\|RIB\|ATTESTATION\|IFU` | (supprimés) | Documents | note FEAT-008 |
| B-26 | `🌐 API externe — Données temps réel via Alpha Vantage REST API — EJB Stateless → HTTP → JSON → SVG \| 📦 Watchlist persistée en base H2 via JPA` | (supprimé) | Bourse | note FEAT-009 |
| B-27 | Bandeau IA `Conseiller IA` / `GPT-5.4-nano` / `Azure AI Foundry` / `EJB Stateless WildFly 14` | (supprimé) | Mon Conseiller | note FEAT-010 |
| B-28 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 — JPA JOINED Inheritance (…)` | (supprimé) | Produits | note FEAT-011 |
| B-29 | `🏦 Open Banking — Agregation de comptes via Powens API (PSD2) — OAuth2 depuis un EJB WildFly 14` | (supprimé) | Comptes Tiers | note FEAT-012 |
| B-30 | `Legacy - JEE 8 / JSF 2.3 / EJB 3.2 / WildFly 14 — Validation IBAN MOD-97 + BIC SEPA (zoneSEPA auto-detectee)` | (supprimé) | Beneficiaires | note FEAT-013 |
| B-31 | `Espace Administration — JEE 8 / JSF 2.3 / EJB 3.2 / RBAC custom — Role : ADMIN` | (supprimé ; le titre et le badge « ADMIN » restent) | Administration | note FEAT-016 |

Les textes de remplacement des messages techniques retirés (`Erreur : {message}` et `Erreur suppression : {message}` de Virements programmes ; `Erreur simulation : …`, `Erreur : …` et `Erreur suppression : …` de Prets ; `Erreur lors de l'ajout du bénéficiaire : {message}` de Beneficiaires ; validations JSF du virement, B-21) sont fixés dans textes-nouveaux.md (`vp.erreur.*`, `pret.erreur.*`, `beneficiaire.erreur.ajout`, `virement.validation.*`). Aucun n'est affiché pendant un scénario applicable du golden master.

### C. Textes ajoutés sur des écrans comparés (entrée groupée de l'étape G2)

| # | Texte legacy | Nouveau texte | Écrans | Exigence |
|---|---|---|---|---|
| C1 | (absent) | **Entrée groupée G2 « textes ajoutés »** : textes nouveaux ou réutilisés ajoutés sur un écran comparé, sans modifier ni retirer aucune ligne attendue — section « Suivi de mes virements » (`virement.revue.*`, état vide compris), section « Virements suspendus — En attente de revue » (`revue.titre`, `revue.colonne.*`, `revue.vide`), lien d'évitement « Aller au contenu principal », mention « (nouvel onglet) ». Une seule entrée dans la correspondance des textes (text_map) de l'étape G2 : ajouts uniquement, sans effet en mode `contains`, aucune ligne attendue remplacée. Condition : ces textes sont placés après le contenu comparé, pour que les lignes attendues restent dans les 5 000 premiers caractères lus par le rejeu. | Virement, Tableau de bord, Administration, Documents, toutes les pages (lien d'évitement) ; scénarios concernés : textes-nouveaux.md | FR-027-16, FR-016-19, Accessibility Floor |

Scénarios du golden master qui divergent encore après ces choix : divergences-golden-master.md.

## Points ouverts

Textes des écrans nouveaux : **fermés** par textes-nouveaux.md (lot validé avec corrections par le décideur le 2026-10-05). Les points 1 à 6 ci-dessous sont conservés pour l'historique ; chacun renvoie à ce lot.

Textes qui étaient à fixer avant la story concernée, conformément à la règle n°1 :

1. **File de revue (FR-016-19)** : titre de section, intitulés de colonnes, forme du motif de suspension, libellés des actions libérer et rejeter, titre et champ de la modale, message de motif manquant, messages de succès, message de libération refusée sur solde insuffisant (décision B6), état vide. → `revue.*`
2. **États d'un virement côté client (FR-027-16)** : titre de section, colonnes, libellés des cinq badges, état vide. → `virement.revue.*`, `virement.etat.*`
3. **Avis à l'écran d'un virement suspendu (FR-027-04)** : reprendre le texte de la notification (A-027-4) ? Tranché : oui, à l'identique, avec l'orthographe correcte (`virement.avis.suspension`).
4. **Reprise préremplie (FR-003-28)** : libellé du bouton. → `virement.reprise.bouton`
5. **Messages techniques retirés sans texte de remplacement** : validations JSF du virement (A-003-6), erreurs de Virements programmes, de Prets et de Beneficiaires (NFR-SEC-04). → `virement.validation.*`, `vp.erreur.*`, `pret.erreur.*`, `beneficiaire.erreur.ajout`
6. **Textes d'accessibilité sans équivalent legacy** :
   - le lien d'évitement ;
   - la mention « nouvel onglet » des PDF ;
   - un éventuel nom de menu repliable ;
   - les noms accessibles des graphiques.

   Ils sont invisibles ou réservés aux lecteurs d'écran. Le lien d'évitement et la mention « nouvel onglet » sont fixés (`a11y.*`) ; ils entrent dans le texte de la page lu par le rejeu, et relèvent de l'entrée groupée C1 (ajout, sans effet en mode `contains`). Le nom d'un menu repliable reste sans objet (pas de menu repliable) ; les noms accessibles des graphiques restent à fixer avec leur story.

Autres points non fermés :

7. **Type « FRAUDE » affiché sur la notification de suspension** : FR-025-05 garde le libellé de type « FRAUDE » (parité), alors que FR-027-04 interdit que le texte évoque la fraude. Le libellé de type reste affiché tant que le PM n'a pas tranché.
8. **Formats numériques** : le legacy mélange les formats anglo-saxons (« 10,548.72 EUR », `EURx,xxx.xx`) et français. Par parité, ils sont repris écran par écran, ce qui contredit le format « 1 234,56 € » de DESIGN.md (`{components.amount}`). À arbitrer avec l'auteur de DESIGN.md et à l'étape G2.
9. **Casse rendue** : reproduire les capitales CSS du legacy (eyebrow, en-têtes de tableau) suppose que le golden master compare `innerText`, ce qui est le cas dans `replay-config.json`. Une normalisation de casse au rejeu (G2) éviterait cette contrainte. À décider en architecture.
10. **Indice du champ montant conservé** : « Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude » est repris (FR-003-01), alors qu'à partir de 5 000 € le virement est exécuté et débité (textes « en attente » du legacy gardés pour la démonstration, B1 révisée), et suspendu à partir de 10 000 €. Le texte trompe, mais il n'est pas modifié sans exigence.
11. **Classement des écarts de la partie B** : le décideur a limité les écarts autorisés aux décisions RG-027-003, B1-B6 et aux FR ÉVOLUTION. Les corrections de bugs et les retraits techniques de la partie B sont portés par des FR du PRD (NFR-UI-01, NFR-SEC-04), mais ne sont pas marqués ÉVOLUTION. Il faut confirmer qu'ils entrent dans le périmètre autorisé ; sinon, il faut les retirer.
12. **Support mobile et navigation sous `xl`** : le header de 14 liens passe sur deux lignes (supposition). Pas de mode sombre (supposition).
13. **Accessibilité réglementaire** : l'Acte européen sur l'accessibilité (EN 301 549) et le RGAA ne s'imposent qu'à une mise en production, hors du démonstrateur. Voir Accessibility Floor.
