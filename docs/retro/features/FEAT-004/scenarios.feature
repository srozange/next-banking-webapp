Feature: Virements programmés
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @a-confirmer
  Scenario: RG-004-002 — the recurring transfers page loads for an authenticated customer
    # Capture golden master : SC-004-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-004-01 : La page virements programmés charge avec le formulaire de création et la liste des virements du client
    When the recurring transfers page loads for an authenticated customer
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement-programme.xhtml
    Then display only the bank accounts belonging to that customer in the source account selector.

  @a-confirmer
  Scenario: RG-004-003 — the recurring transfers page loads for an authenticated customer
    # Capture golden master : SC-004-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-004-01 : La page virements programmés charge avec le formulaire de création et la liste des virements du client
    When the recurring transfers page loads for an authenticated customer
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement-programme.xhtml
    Then display only the recurring transfers belonging to that customer, ordered by next execution date ascending.

  # Validation du formulaire de création
  @a-confirmer
  Scenario: RG-004-010 — the source account field (srcAcc) is not selected
    # Capture golden master : SC-004-03
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-004-03 : Soumettre le formulaire vide affiche 'Veuillez selectionner un compte source'
    And the source account field (`srcAcc`) is not selected
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/virement-programme.xhtml
    And il clique sur input[type='submit'][value='Creer le virement programme']
    And il attend le texte « Veuillez selectionner un compte source »
    Then reject the submission and display "Veuillez selectionner un compte source".

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-004-016 exclu — dette technique legacy, hors scope réécriture

  @a-confirmer
  @a-confirmer
  # Création réussie
  @a-confirmer
  Scenario: RG-004-023 — a recurring transfer is successfully created
    # Capture golden master : SC-004-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-004-02 : Créer un virement programmé mensuel avec données valides affiche 'Virement programme cree avec succes'
    And les données saisies : #vpForm\:srcAcc = « 1 »; #vpForm\:montant = « 50 »; #vpForm\:freq = « MENSUEL »; #vpForm\:targetNum = « FR76-0001-0002 »; #vpForm\:targetLabel = « [retro] test virprog »; #vpForm\:dateExec = « 2027-01-01 »; #vpForm\:motif = « [retro] virement programm gm »
    When a recurring transfer is successfully created
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement-programme.xhtml
    And il sélectionne « 1 » dans #vpForm\:srcAcc
    And il saisit « 50 » dans #vpForm\:montant
    And il sélectionne « MENSUEL » dans #vpForm\:freq
    And il saisit « FR76-0001-0002 » dans #vpForm\:targetNum
    And il saisit « [retro] test virprog » dans #vpForm\:targetLabel
    And il saisit « 2027-01-01 » dans #vpForm\:dateExec
    And il saisit « [retro] virement programm gm » dans #vpForm\:motif
    And il clique sur input[name='vpForm:j_idt29']
    And il attend le texte « cree avec succes »
    Then persist it with actif=true, executionCount=0, and a creation timestamp, then display "Virement programme cree avec succes".

  @a-confirmer
  Scenario: RG-004-024 — a recurring transfer is successfully created
    # Capture golden master : SC-004-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-004-02 : Créer un virement programmé mensuel avec données valides affiche 'Virement programme cree avec succes'
    And les données saisies : #vpForm\:srcAcc = « 1 »; #vpForm\:montant = « 50 »; #vpForm\:freq = « MENSUEL »; #vpForm\:targetNum = « FR76-0001-0002 »; #vpForm\:targetLabel = « [retro] test virprog »; #vpForm\:dateExec = « 2027-01-01 »; #vpForm\:motif = « [retro] virement programm gm »
    When a recurring transfer is successfully created
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/virement-programme.xhtml
    And il sélectionne « 1 » dans #vpForm\:srcAcc
    And il saisit « 50 » dans #vpForm\:montant
    And il sélectionne « MENSUEL » dans #vpForm\:freq
    And il saisit « FR76-0001-0002 » dans #vpForm\:targetNum
    And il saisit « [retro] test virprog » dans #vpForm\:targetLabel
    And il saisit « 2027-01-01 » dans #vpForm\:dateExec
    And il saisit « [retro] virement programm gm » dans #vpForm\:motif
    And il clique sur input[name='vpForm:j_idt29']
    And il attend le texte « cree avec succes »
    Then reset all form fields and reload the list of recurring transfers.

  @a-confirmer
  @a-confirmer
  # RG-004-043 — comportement corrigé proposé (BUG-013, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-013 @proposition
  Scenario: RG-004-043 — toggle (suspend/reactivate) or delete is requested for a recurring transfer
    Given the legacy context of rule RG-004-043
    When toggle (suspend/reactivate) or delete is requested for a recurring transfer
    Then verify that the transfer belongs to the authenticated customer before executing the action.

  @a-confirmer
  @a-confirmer
  # RG-004-061 — comportement corrigé proposé (BUG-014, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-014 @proposition
  Scenario: RG-004-061 — the source account balance is strictly less than the transfer amount at …
    Given the legacy context of rule RG-004-061
    And the source account balance is strictly less than the transfer amount at execution time
    When the action described by rule RG-004-061 is performed
    Then skip the execution and notify the customer that the scheduled transfer could not be executed due to insufficient funds.

  @suspect
  # RG-004-063 — comportement corrigé proposé (BUG-015, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-015 @proposition
  Scenario: RG-004-063 — a recurring transfer is executed and the target account number does not match …
    Given the legacy context of rule RG-004-063
    When a recurring transfer is executed and the target account number does not match any internal account
    Then reject the recurring transfer before any debit, and inform the customer with a clear error message.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  # Contraintes de champ (DB)
  # RG-004-070 exclu — dette technique legacy, hors scope réécriture

  # RG-004-071 exclu — dette technique legacy, hors scope réécriture

  # RG-004-072 exclu — dette technique legacy, hors scope réécriture
