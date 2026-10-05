### FEAT-026 — Audit trail

**Description :** L'audit trail est une fonction transverse sans écran : il enregistre les actions significatives des utilisateurs CLIENT, CONSEILLER et ADMIN (connexion, échec de connexion, déconnexion, actions d'administration sur les clients et les prêts, virements). Chaque entrée porte l'utilisateur, un code d'action, un détail, l'adresse IP d'origine et un horodatage serveur. Une entrée est conservée même si l'opération qui l'a déclenchée échoue. Le journal est en écriture seule : l'application n'offre aucun écran ni aucune fonction pour le consulter. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-026-01 : Audit des connexions, échecs de connexion et déconnexions

Le système enregistre une entrée d'audit :
- à chaque connexion réussie : identifiant de l'utilisateur, action « LOGIN », détail « Connexion réussie », adresse IP du client et horodatage serveur ;
- à chaque tentative de connexion échouée : identifiant saisi, action « LOGIN_FAILED », détail « Tentative échouée », adresse IP du client et horodatage serveur ;
- à chaque fin de session par déconnexion d'un utilisateur authentifié : identifiant de l'utilisateur, action « LOGOUT », détail « Déconnexion » et horodatage serveur (l'adresse IP est exigée par FR-026-05).

Voir aussi FR-001-12 (déclenchement depuis la connexion).

Source : RG-026-001, RG-026-002, RG-026-003

#### FR-026-02 : Audit des actions d'administration

Le système enregistre une entrée d'audit portant l'identifiant de l'administrateur :
- à la désactivation d'un client : action « ADMIN_DEACTIVATE », détail « Désactivation client id={customerId} » ;
- à l'activation d'un client : action « ADMIN_ACTIVATE », détail « Activation client id={customerId} » ;
- à l'approbation d'une demande de prêt : action « ADMIN_LOAN_APPROVE », détail « Approbation prêt id={loanId} » ;
- au refus d'une demande de prêt : action « ADMIN_LOAN_REFUSE », détail « Refus prêt id={loanId} ».

Source : RG-026-004, RG-026-005, RG-026-006, RG-026-007

#### FR-026-03 : Audit des virements

Lorsqu'un virement est soumis avec succès, le système enregistre une entrée d'audit portant l'identifiant de l'utilisateur, l'action « VIREMENT » et le détail « Montant: {amount} - Motif: {motif} » (montant et motif du virement).

Source : RG-026-008

#### FR-026-04 : Entrée d'audit indépendante de l'opération

Le système enregistre chaque entrée d'audit dans une transaction indépendante de celle de l'opération qui la déclenche : l'entrée est conservée même si l'opération est annulée.

Source : RG-026-009

#### FR-026-05 : Adresse IP d'origine pour toute action auditée

Lorsqu'une action est auditée, quelle qu'elle soit (LOGIN, LOGIN_FAILED, LOGOUT, ADMIN_DEACTIVATE, ADMIN_ACTIVATE, ADMIN_LOAN_APPROVE, ADMIN_LOAN_REFUSE, VIREMENT), le système enregistre l'adresse IP d'origine du client dans l'entrée, résolue selon FR-001-13. Cette exigence remplace le comportement legacy RG-026-010 (adresse enregistrée seulement pour LOGIN et LOGIN_FAILED).

Critère d'acceptation (test) : pour chacun des huit codes d'action, une action déclenchée depuis une adresse IP connue produit une entrée d'audit dont l'adresse IP est renseignée et égale à cette adresse.

Source : RG-026-010, BUG-083

#### FR-026-06 : Journal en écriture seule

Le système conserve les entrées d'audit en écriture seule : aucun écran ni aucune fonction de l'application ne permet de consulter, filtrer ou afficher le journal d'audit.

Source : RG-026-011

**Notes :**
- RG-026-010 (Suspect, dette legacy) est couverte par la FR de correction FR-026-05 ; son comportement legacy n'est pas reproduit.
- Les entrées LOGIN, LOGIN_FAILED et LOGOUT sont aussi décrites dans FEAT-001 (RG-001-013 à RG-001-015, FR-001-12), sans fusion décidée ; les deux descriptions concordent (mêmes codes d'action et détails).
- Les traitements système (sans utilisateur) ne sont pas audités par cette fonctionnalité : le corpus ne décrit que les actions des utilisateurs.
