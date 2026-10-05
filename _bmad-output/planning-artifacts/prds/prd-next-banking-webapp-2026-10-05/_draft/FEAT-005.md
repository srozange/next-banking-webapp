### FEAT-005 — Relevé de compte

**Description :** L'écran « Relevé de compte » permet à tout utilisateur authentifié (CLIENT, et CONSEILLER ou ADMIN pour leurs propres comptes) de consulter, compte par compte, toutes les opérations d'un de ses comptes, de la plus récente à la plus ancienne. Le premier compte est sélectionné d'office ; changer de compte rafraîchit le solde et les opérations sans recharger toute la page. Les opérations en attente (libellé préfixé « [EN ATTENTE] », texte legacy conservé pour la démonstration, décision de finalisation B1 révisée) sont signalées par un badge et une ligne surlignée. Les cas limites marquants sont l'utilisateur sans compte (sélecteur vide, aucune opération), le compte sans opération et l'absence de limite au nombre d'opérations affichées. Scénarios golden master : SC-005-01.

**Exigences fonctionnelles :**

#### FR-005-01 : Accès à l'écran du relevé

Lorsqu'un utilisateur sans session active demande l'écran du relevé de compte, le système le redirige vers la page de connexion.

Source : RG-005-001

#### FR-005-02 : Comptes proposés et sélection initiale

À l'ouverture, l'écran affiche le titre d'onglet « BankApp Demo - Relevé de compte », la section « Relevé de compte » et le sélecteur libellé « Compte : », qui ne propose que les comptes du client connecté. Si le client a au moins un compte, le premier compte de la liste est sélectionné d'office et ses opérations sont chargées. Si le client n'a aucun compte, le sélecteur est affiché sans option et aucune opération n'est affichée.

Source : RG-005-002, RG-005-003, RG-005-004

#### FR-005-03 : Changement de compte

Lorsque l'utilisateur change de compte dans le sélecteur, le système recharge la liste de ses comptes (soldes à jour), puis charge les opérations du compte choisi, et rafraîchit le panneau d'informations du compte et le tableau des opérations sans recharger toute la page.

Source : RG-005-005, RG-005-006

#### FR-005-04 : Informations du compte sélectionné

Lorsqu'un compte est sélectionné, le système affiche « {type de compte} — N° {numéro de compte} — Solde actuel : {solde} », le solde étant formaté en euros (symbole « € ») avec au moins 2 décimales.

Source : RG-005-007, RG-005-008

#### FR-005-05 : Chargement de toutes les opérations

Le système charge toutes les opérations du compte sélectionné, sans limite de nombre, triées par date d'opération décroissante (la plus récente en premier).

Source : RG-005-009, RG-005-010

#### FR-005-06 : Tableau des opérations

Si le compte n'a aucune opération, le système affiche « Aucune transaction pour ce compte. ». Sinon, il affiche au-dessus du tableau le compteur « {N} opération(s) », où N est le nombre total d'opérations chargées, puis un tableau aux colonnes « Date », « Description », « Type », « Montant », « Solde après ». Pour chaque opération :
- la date est au format `dd/MM/yyyy HH:mm` ;
- le type est affiché « CREDIT » en vert (#2e7d32) ou « DEBIT » en rouge (#E3000F) ;
- le montant est précédé de « + » pour un CREDIT et d'un signe moins pour un DEBIT [ASSUMPTION A-005-1 : le signe moins est le tiret ASCII « - » relevé dans l'inventaire des messages, et non le caractère « − » cité par RG-005-014], et formaté en euros avec au moins 2 décimales ;
- le solde après opération est formaté en euros avec au moins 2 décimales, sans signe.

Source : RG-005-011, RG-005-012, RG-005-013, RG-005-014, RG-005-015, RG-005-016

#### FR-005-07 : Opérations en attente

Si le libellé d'une opération commence par « [EN ATTENTE] » suivi d'une espace (13 caractères), mention posée par FR-003-09 et FR-003-10, le système affiche le badge « En attente » avant le libellé, retire ces 13 caractères du libellé affiché et affiche la ligne sur fond jaune. Les textes legacy sont conservés pour la démonstration (décision de finalisation B1 révisée) ; la reconnaissance de la nouvelle mention de FR-003-29 arrive avec cette story, après la démonstration.

Critère d'acceptation (test) : une opération « [EN ATTENTE] Virement vers X » s'affiche avec le badge « En attente », le libellé « Virement vers X » et un fond jaune ; une opération sans préfixe n'a ni badge ni fond jaune.

Source : RG-005-017

#### FR-005-08 : Liens de navigation selon le rôle

Sur l'écran du relevé, le lien de navigation « Espace Conseiller » n'est affiché qu'aux utilisateurs de rôle CONSEILLER ou ADMIN, et le lien « Administration » qu'aux utilisateurs de rôle ADMIN.

Source : RG-005-018, RG-005-019

**Notes :**
- Le bandeau technique de l'application legacy, affiché sous la navigation, n'est pas une règle de gestion et n'est pas repris.
- Contradiction mineure dans le corpus : RG-005-014 cite le signe « − » pour les débits, l'inventaire des messages le tiret « - » ; tranché par A-005-1.
