### FEAT-013 — Gestion des bénéficiaires externes

**Description :** L'écran « Beneficiaires » permet à tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) d'enregistrer des bénéficiaires externes SEPA (libellé, titulaire, IBAN, BIC et banque facultatifs), de vérifier un IBAN avant l'ajout, de consulter la liste de ses bénéficiaires actifs avec IBAN masqué et badge de zone SEPA, et de supprimer un bénéficiaire. L'IBAN est contrôlé par l'algorithme MOD-97, le BIC par son format, et la zone SEPA est déduite du code pays de l'IBAN. La suppression est logique (le bénéficiaire est désactivé, pas effacé). Les cas limites marquants sont la liste vide, l'IBAN hors zone SEPA, la session expirée à la soumission et la suppression du bénéficiaire d'un autre client (désormais refusée). Scénarios golden master : SC-013-01, SC-013-02.

**Exigences fonctionnelles :**

#### FR-013-01 : Accès à l'écran des bénéficiaires

Lorsqu'un utilisateur non authentifié demande l'écran des bénéficiaires, le système lui refuse l'accès et le redirige vers la page de connexion.

Source : RG-013-001

#### FR-013-02 : Liste des bénéficiaires actifs du client

L'écran affiche le titre d'onglet « BankApp Demo - Beneficiaires », puis la section « Mes beneficiaires enregistres », titrée « Beneficiaires ({nombre}) », avec un tableau aux colonnes « Libelle », « Titulaire », « IBAN », « BIC », « Banque », « Zone SEPA », « Action » et un bouton « Supprimer » par ligne. La liste ne contient que les bénéficiaires du client connecté qui sont actifs, triés par libellé alphabétique croissant. Si le client n'a aucun bénéficiaire actif, l'écran affiche « Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un. ».

Source : RG-013-003, RG-013-004, RG-013-005, RG-013-006

#### FR-013-03 : Affichage masqué de l'IBAN, BIC, banque et zone SEPA

Dans le tableau, l'IBAN est affiché sous la forme de ses 4 premiers caractères, suivis de « *** », suivis de ses 4 derniers caractères (par exemple « FR35***9010 »). Si le BIC n'est pas renseigné, la colonne « BIC » affiche « — » ; si la banque n'est pas renseignée, la colonne « Banque » affiche « — ». La colonne « Zone SEPA » affiche « ✓ SEPA » si le code pays de l'IBAN appartient à la zone SEPA, « Hors SEPA » sinon.

Source : RG-013-007, RG-013-008, RG-013-009, RG-013-010

#### FR-013-04 : Vérification de l'IBAN avant l'ajout

La section « Ajouter un beneficiaire » (titre « Nouveau beneficiaire SEPA ») propose, à côté du champ « IBAN * » (exemple « ex: FR76 3000 1007 9412 3456 7890 185 »), le bouton « Verifier l'IBAN ». Au clic, le système vérifie le seul champ IBAN, sans soumettre le formulaire d'ajout ni recharger la page, et affiche :
- « Veuillez saisir un IBAN. » si l'IBAN est vide ;
- « IBAN invalide (echec MOD-97). Verifiez la saisie. » si l'IBAN échoue au contrôle MOD-97 ;
- « IBAN valide — {code pays} (Zone SEPA) » si l'IBAN est valide et que son code pays appartient à la zone SEPA, « IBAN valide — {code pays} (Hors zone SEPA) » sinon.

Source : RG-013-011, RG-013-012, RG-013-013, RG-013-014

#### FR-013-05 : Contrôles à l'ajout d'un bénéficiaire

Le formulaire d'ajout comporte les champs « Libelle * », « Nom du titulaire * », « IBAN * », « BIC (optionnel) » (exemple « ex: BNPAFRPPXXX ») et « Banque (optionnel) » (exemple « ex: BNP Paribas »), et le bouton « Ajouter le beneficiaire ». À la soumission, le système rejette l'ajout et affiche :
- « Session expirée, veuillez vous reconnecter. » si la session ne porte plus de client ;
- « Le libellé est obligatoire. » si le libellé est vide ;
- « L'IBAN est obligatoire. » si l'IBAN est vide ;
- « IBAN invalide. Veuillez saisir un IBAN correct (algorithme MOD-97). » si l'IBAN échoue au contrôle MOD-97 ;
- « Le nom du titulaire est obligatoire. » si le nom du titulaire est vide ;
- « BIC invalide. Format attendu : 8 ou 11 caractères alphanumériques. » si un BIC est saisi et ne respecte pas ce format (4 lettres de banque, 2 lettres de pays, 2 caractères alphanumériques de localisation, puis 3 caractères alphanumériques facultatifs).

Source : RG-013-015, RG-013-016, RG-013-017, RG-013-018, RG-013-019, RG-013-020

#### FR-013-06 : Longueur maximale des champs texte

Le système rejette l'ajout si le libellé, le nom du titulaire ou le nom de la banque dépasse 100 caractères ; aucun bénéficiaire n'est alors enregistré. La saisie de ces champs est limitée à 100 caractères.

Source : RG-013-028, RG-013-030, RG-013-032

#### FR-013-07 : Normalisation et enregistrement du bénéficiaire

À l'enregistrement, le système :
- normalise l'IBAN en supprimant tous les espaces et en le passant en majuscules ;
- normalise le BIC en supprimant les espaces de début et de fin et en le passant en majuscules, et ne l'enregistre pas (valeur absente) s'il est blanc après suppression des espaces ;
- supprime les espaces de début et de fin du nom de la banque, et ne l'enregistre pas (valeur absente) s'il est blanc après suppression des espaces ;
- calcule et enregistre l'appartenance à la zone SEPA à partir du code pays de l'IBAN normalisé ;
- enregistre la date du jour comme date de création ;
- enregistre le bénéficiaire comme actif.

Source : RG-013-021, RG-013-022, RG-013-023, RG-013-024, RG-013-025, RG-013-026

#### FR-013-08 : Ajout réussi

Lorsqu'un bénéficiaire est ajouté, le système affiche « Bénéficiaire ajouté avec succès. », recharge la liste des bénéficiaires et vide le formulaire d'ajout.

Source : RG-013-027

#### FR-013-09 : Suppression logique d'un bénéficiaire

Lorsque l'utilisateur clique sur « Supprimer », le système désactive le bénéficiaire sans l'effacer, puis rafraîchit aussitôt la liste affichée, sans recharger toute la page ; le bénéficiaire n'apparaît plus dans la liste (FR-013-02). La suppression n'est possible que pour un bénéficiaire du client connecté (FR-013-10).

Source : RG-013-033, RG-013-034

#### FR-013-10 : Contrôle d'appartenance avant suppression

Avant toute suppression, le système vérifie que le bénéficiaire appartient au client connecté. Sinon, la suppression est rejetée, le bénéficiaire reste actif, et le message « Erreur lors de la suppression. » est affiché [ASSUMPTION A-013-1 : le corpus ne fixe pas de message pour ce refus ; le message d'erreur de suppression existant est réutilisé, il ne révèle pas l'existence du bénéficiaire]. Remplace le comportement legacy RG-013-035 (aucune vérification d'appartenance).

Critère d'acceptation (test) : connecté en tant que client A, une demande de suppression portant sur l'identifiant d'un bénéficiaire du client B est refusée, et ce bénéficiaire reste actif et visible dans la liste de B.

Source : RG-013-035, BUG-047

**Notes :**
- RG-013-002 : couverte par l'exigence fusionnée de FEAT-010 (règles voisines) — accès pour tout utilisateur authentifié, quel que soit son rôle.
- RG-013-029 (dette legacy, Suspect) : non reproduite — information de l'utilisateur en cas de libellé de plus de 100 caractères ; bugs conservés BUG-045 et BUG-048.
- RG-013-031 (dette legacy, Suspect) : non reproduite — information de l'utilisateur en cas de nom du titulaire de plus de 100 caractères ; bugs conservés BUG-046 et BUG-049.
- Bug conservé BUG-050 : nom de banque de plus de 100 caractères (le rejet reste exigé par FR-013-06).
- En legacy, une erreur inattendue à l'ajout affichait « Erreur lors de l'ajout du bénéficiaire : {message} » avec le message technique brut ; ce détail n'est plus exposé (NFR-SEC-04).
