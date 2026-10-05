### FEAT-026 — Audit trail

**Description :** L'audit trail est une fonction transverse sans écran : il enregistre les actions significatives des utilisateurs CLIENT, CONSEILLER et ADMIN (connexion, échec de connexion, déconnexion, actions d'administration sur les clients, les prêts et les virements suspendus, virements), ainsi que le rejet automatique d'un virement suspendu et le rejet d'un virement libéré dont le solde est devenu insuffisant au moment du débit. Chaque entrée porte l'utilisateur, un code d'action, un détail, l'adresse IP d'origine et un horodatage serveur. Une entrée est conservée même si l'opération qui l'a déclenchée échoue. Le journal est en écriture seule : l'application n'offre aucun écran ni aucune fonction pour le consulter. Scénarios golden master : aucun.

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
- au refus d'une demande de prêt : action « ADMIN_LOAN_REFUSE », détail « Refus prêt id={loanId} » ;
- à la libération d'un virement suspendu (FR-016-19) : action « ADMIN_TRANSFER_RELEASE », détail « Liberation virement id={transferId} - Motif: {motif} - Issue: {EXECUTE|REJETE} », l'issue étant REJETE si le solde est insuffisant à la libération (FR-027-16, décision de finalisation B6) ; une libération acceptée (EXECUTE) puis rejetée au débit asynchrone est tracée ensuite par TRANSFER_REJECT_INSUFFICIENT_FUNDS (FR-026-03, décision de finalisation B7) ;
- au rejet d'un virement suspendu (FR-016-19) : action « ADMIN_TRANSFER_REJECT », détail « Rejet virement id={transferId} - Motif: {motif} ».

Les deux derniers codes sont ajoutés par la décision RG-027-003 (D6). [ASSUMPTION A-026-1 : noms des codes d'audit des virements et contenu de leur détail, choisis par cohérence avec les codes existants ; ni le corpus ni la table ronde ne les fixent]

Source : RG-026-004, RG-026-005, RG-026-006, RG-026-007

#### FR-026-03 : Audit des virements [ÉVOLUTION — décision RG-027-003 D5, D8] [ÉVOLUTION — décision de finalisation B7]

Le système audite un virement à chacune de ses étapes :
- à la soumission, quelle que soit l'issue du contrôle de fraude : identifiant de l'émetteur, action « VIREMENT », détail « Montant: {amount} - Motif: {motif} - Issue: {EXECUTE|SUSPENDU|REFUSE_ERREUR_CONTROLE} » (montant et motif du virement, issue de FR-027-02) ;
- au passage à l'état EXÉCUTÉ, à la soumission ou après libération : identifiant de l'émetteur, action « TRANSFER_EXECUTED », détail « Montant: {amount} - Motif: {motif} » (FR-003-11) ;
- à la libération et au rejet par l'administrateur : FR-026-02 ;
- au rejet automatique à l'heure butoir (FR-027-17) : utilisateur « SYSTEM », action « TRANSFER_AUTO_REJECT », détail « Rejet automatique virement id={transferId} » ;
- au rejet d'un virement libéré dont le solde est devenu insuffisant au moment du débit asynchrone (FR-027-16, décision de finalisation B7) : utilisateur « SYSTEM », action « TRANSFER_REJECT_INSUFFICIENT_FUNDS », détail « Rejet virement id={transferId} - Motif: solde insuffisant au debit » ; ce code est distinct du rejet par l'administrateur (ADMIN_TRANSFER_REJECT) et du rejet automatique à l'échéance (TRANSFER_AUTO_REJECT) (A-026-1).

Un virement immédiatement exécuté produit donc deux entrées (VIREMENT, TRANSFER_EXECUTED) ; un virement suspendu puis libéré en produit trois (VIREMENT, ADMIN_TRANSFER_RELEASE, TRANSFER_EXECUTED) ; un virement suspendu, libéré, puis rejeté au débit pour solde insuffisant en produit trois (VIREMENT, ADMIN_TRANSFER_RELEASE, TRANSFER_REJECT_INSUFFICIENT_FUNDS) ; un virement suspendu puis rejeté en produit deux (VIREMENT, puis ADMIN_TRANSFER_REJECT ou TRANSFER_AUTO_REJECT) ; un virement refusé pour erreur du contrôle en produit une. Une échéance de virement programmé (FR-021-10) produit les mêmes entrées au nom du titulaire du virement programmé (A-026-1). Le détail de RG-026-008 est complété par l'issue du contrôle.

Critère d'acceptation (test) : pour chacun des six parcours ci-dessus, le journal contient exactement les entrées attendues, dans l'ordre, avec les codes et l'utilisateur indiqués.

Source : RG-026-008, RG-027-003

#### FR-026-04 : Entrée d'audit indépendante de l'opération

Le système enregistre chaque entrée d'audit dans une transaction indépendante de celle de l'opération qui la déclenche : l'entrée est conservée même si l'opération est annulée.

Source : RG-026-009

#### FR-026-05 : Adresse IP d'origine pour toute action auditée

Lorsqu'une action déclenchée par une requête d'un utilisateur est auditée, quelle qu'elle soit (LOGIN, LOGIN_FAILED, LOGOUT, ADMIN_DEACTIVATE, ADMIN_ACTIVATE, ADMIN_LOAN_APPROVE, ADMIN_LOAN_REFUSE, ADMIN_TRANSFER_RELEASE, ADMIN_TRANSFER_REJECT, VIREMENT, TRANSFER_EXECUTED), le système enregistre l'adresse IP d'origine de cette requête dans l'entrée, résolue selon FR-001-13. Les entrées produites par un traitement planifié ou par le traitement asynchrone des virements (TRANSFER_AUTO_REJECT, TRANSFER_REJECT_INSUFFICIENT_FUNDS, entrées d'une échéance de virement programmé) n'ont pas d'adresse IP d'origine. Cette exigence remplace le comportement legacy RG-026-010 (adresse enregistrée seulement pour LOGIN et LOGIN_FAILED).

Critère d'acceptation (test) : pour chacun des onze codes d'action déclenchés par un utilisateur, une action déclenchée depuis une adresse IP connue produit une entrée d'audit dont l'adresse IP est renseignée et égale à cette adresse.

Source : RG-026-010, BUG-083

#### FR-026-06 : Journal en écriture seule

Le système conserve les entrées d'audit en écriture seule : aucun écran ni aucune fonction de l'application ne permet de consulter, filtrer ou afficher le journal d'audit.

Source : RG-026-011

**Notes :**
- RG-026-010 (Suspect, dette legacy) est couverte par la FR de correction FR-026-05 ; son comportement legacy n'est pas reproduit.
- Les entrées LOGIN, LOGIN_FAILED et LOGOUT sont aussi décrites dans FEAT-001 (RG-001-013 à RG-001-015, FR-001-12), sans fusion décidée ; les deux descriptions concordent (mêmes codes d'action et détails).
- Les traitements système (sans utilisateur) ne sont pas audités par cette fonctionnalité, le corpus ne décrivant que les actions des utilisateurs, sauf le rejet automatique d'un virement suspendu (FR-027-17), le rejet d'un virement libéré au débit pour solde insuffisant (FR-027-16, B7) et les échéances de virement programmé soumises au contrôle de fraude (FR-021-10), audités selon FR-026-03.
