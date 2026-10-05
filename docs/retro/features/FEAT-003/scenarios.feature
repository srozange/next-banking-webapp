Feature: Effectuer un virement
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # Accès et chargement du formulaire
  @a-confirmer
  Scenario: RG-003-001 — grant access to the transfer form to any authenticated user regardless of role
    # Capture golden master : SC-003-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-01 : Le formulaire de virement charge avec les comptes du client authentifié
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    Then grant access to the transfer form to any authenticated user regardless of role.

  @a-confirmer
  Scenario: RG-003-002 — the transfer form is loaded
    # Capture golden master : SC-003-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-01 : Le formulaire de virement charge avec les comptes du client authentifié
    When the transfer form is loaded
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    Then display only the bank accounts belonging to the connected customer.

  @a-confirmer
  @a-confirmer
  Scenario: RG-003-005 — the source account and the target account are the same
    # Capture golden master : SC-003-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-02 : Sélectionner le même compte en source et destination affiche 'Le compte source et destination doivent être différents'
    And les données saisies : #virementForm\:source = « 1 »; #virementForm\:target = « 1 »; #virementForm\:amount = « 100 »; #virementForm\:motif = « [retro] test comptes identiques »
    And the source account and the target account are the same
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    And il sélectionne « 1 » dans #virementForm\:source
    And il sélectionne « 1 » dans #virementForm\:target
    And il saisit « 100 » dans #virementForm\:amount
    And il saisit « [retro] test comptes identiques » dans #virementForm\:motif
    And il clique sur input[name='virementForm:j_idt23']
    And il attend le texte « compte source et destination »
    Then reject the transfer and inform the user that the two accounts must differ.

  @a-confirmer
  # RG-003-007 — comportement corrigé proposé (BUG-008, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-008 @proposition
  Scenario: RG-003-007 — the source account balance is insufficient for the requested amount
    Given the legacy context of rule RG-003-007
    And the source account balance is insufficient for the requested amount
    When the action described by rule RG-003-007 is performed
    Then reject the transfer and inform the user that the balance is insufficient.

  @a-confirmer
  @a-confirmer
  Scenario: RG-003-010 — a valid transfer is submitted
    # Capture golden master : SC-003-04
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-04 : Un virement valide (100 EUR, COURANT→EPARGNE) est exécuté et redirige vers le tableau de bord avec une notification CONFIRMATION
    And les données saisies : #virementForm\:source = « 1 »; #virementForm\:target = « 2 »; #virementForm\:amount = « 100 »; #virementForm\:motif = « [retro] virement doré 100EUR »
    When a valid transfer is submitted
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    And il sélectionne « 1 » dans #virementForm\:source
    And il sélectionne « 2 » dans #virementForm\:target
    And il saisit « 100 » dans #virementForm\:amount
    And il saisit « [retro] virement doré 100EUR » dans #virementForm\:motif
    And il clique sur input[name='virementForm:j_idt23']
    And il attend le texte « CONFIRMATION »
    Then debit the source account by the transfer amount within the same database transaction.

  @a-confirmer
  Scenario: RG-003-011 — a valid transfer is submitted
    # Capture golden master : SC-003-04
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-04 : Un virement valide (100 EUR, COURANT→EPARGNE) est exécuté et redirige vers le tableau de bord avec une notification CONFIRMATION
    And les données saisies : #virementForm\:source = « 1 »; #virementForm\:target = « 2 »; #virementForm\:amount = « 100 »; #virementForm\:motif = « [retro] virement doré 100EUR »
    When a valid transfer is submitted
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    And il sélectionne « 1 » dans #virementForm\:source
    And il sélectionne « 2 » dans #virementForm\:target
    And il saisit « 100 » dans #virementForm\:amount
    And il saisit « [retro] virement doré 100EUR » dans #virementForm\:motif
    And il clique sur input[name='virementForm:j_idt23']
    And il attend le texte « CONFIRMATION »
    Then credit the target account by the transfer amount within the same database transaction.

  @a-confirmer
  @a-confirmer
  Scenario: RG-003-013 — a transfer is executed
    # Capture golden master : SC-003-04
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-04 : Un virement valide (100 EUR, COURANT→EPARGNE) est exécuté et redirige vers le tableau de bord avec une notification CONFIRMATION
    And les données saisies : #virementForm\:source = « 1 »; #virementForm\:target = « 2 »; #virementForm\:amount = « 100 »; #virementForm\:motif = « [retro] virement doré 100EUR »
    When a transfer is executed
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    And il sélectionne « 1 » dans #virementForm\:source
    And il sélectionne « 2 » dans #virementForm\:target
    And il saisit « 100 » dans #virementForm\:amount
    And il saisit « [retro] virement doré 100EUR » dans #virementForm\:motif
    And il clique sur input[name='virementForm:j_idt23']
    And il attend le texte « CONFIRMATION »
    Then record the transfer with its timestamp.

  # RG-003-014 — comportement corrigé proposé (BUG-009, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-009 @proposition
  Scenario: RG-003-014 — the transfer amount is greater than or equal to 5000 EUR
    Given the legacy context of rule RG-003-014
    And the transfer amount is greater than or equal to 5000 EUR
    When the action described by rule RG-003-014 is performed
    Then mark the transfer status as PENDING.

  @a-confirmer
  Scenario: RG-003-015 — the transfer amount is strictly less than 5000 EUR
    # Capture golden master : SC-003-04
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-04 : Un virement valide (100 EUR, COURANT→EPARGNE) est exécuté et redirige vers le tableau de bord avec une notification CONFIRMATION
    And les données saisies : #virementForm\:source = « 1 »; #virementForm\:target = « 2 »; #virementForm\:amount = « 100 »; #virementForm\:motif = « [retro] virement doré 100EUR »
    And the transfer amount is strictly less than 5000 EUR
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    And il sélectionne « 1 » dans #virementForm\:source
    And il sélectionne « 2 » dans #virementForm\:target
    And il saisit « 100 » dans #virementForm\:amount
    And il saisit « [retro] virement doré 100EUR » dans #virementForm\:motif
    And il clique sur input[name='virementForm:j_idt23']
    And il attend le texte « CONFIRMATION »
    Then mark the transfer status as SUCCESS.

  @a-confirmer
  @a-confirmer
  Scenario: RG-003-017 — a transfer is successfully executed
    # Capture golden master : SC-003-04
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-04 : Un virement valide (100 EUR, COURANT→EPARGNE) est exécuté et redirige vers le tableau de bord avec une notification CONFIRMATION
    And les données saisies : #virementForm\:source = « 1 »; #virementForm\:target = « 2 »; #virementForm\:amount = « 100 »; #virementForm\:motif = « [retro] virement doré 100EUR »
    When a transfer is successfully executed
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    And il sélectionne « 1 » dans #virementForm\:source
    And il sélectionne « 2 » dans #virementForm\:target
    And il saisit « 100 » dans #virementForm\:amount
    And il saisit « [retro] virement doré 100EUR » dans #virementForm\:motif
    And il clique sur input[name='virementForm:j_idt23']
    And il attend le texte « CONFIRMATION »
    Then redirect the user to the dashboard page.

  @a-confirmer
  Scenario: RG-003-019 — the transfer amount is strictly less than 5000 EUR
    # Capture golden master : SC-003-04
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-003-04 : Un virement valide (100 EUR, COURANT→EPARGNE) est exécuté et redirige vers le tableau de bord avec une notification CONFIRMATION
    And les données saisies : #virementForm\:source = « 1 »; #virementForm\:target = « 2 »; #virementForm\:amount = « 100 »; #virementForm\:motif = « [retro] virement doré 100EUR »
    And the transfer amount is strictly less than 5000 EUR
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/virement.xhtml
    And il sélectionne « 1 » dans #virementForm\:source
    And il sélectionne « 2 » dans #virementForm\:target
    And il saisit « 100 » dans #virementForm\:amount
    And il saisit « [retro] virement doré 100EUR » dans #virementForm\:motif
    And il clique sur input[name='virementForm:j_idt23']
    And il attend le texte « CONFIRMATION »
    Then send a CONFIRMATION notification to the connected user.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-003-027 — comportement corrigé proposé (BUG-010, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-010 @proposition
  Scenario: RG-003-027 — the customer's total transaction count exceeds 8
    Given the legacy context of rule RG-003-027
    And the customer's total transaction count exceeds 8
    When the action described by rule RG-003-027 is performed
    Then raise a HIGH_FREQUENCY fraud alert with a risk score of 50.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # BUG-011 — comportement corrigé proposé (bug d'origine scénario SC-003-03, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @BUG-011 @proposition
  Scenario: BUG-011 — comportement corrigé (SC-003-03)
    Given the context of golden master capture SC-003-03
    And the source account balance is insufficient for the requested transfer amount
    When the action of golden master capture SC-003-03 is performed
    Then reject the transfer and inform the user with a clear, non-technical message that the balance is insufficient.
