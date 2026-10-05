Feature: Gestion des bénéficiaires externes
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @a-confirmer
  # Affichage de la liste des bénéficiaires
  @a-confirmer
  Scenario: RG-013-003 — the beneficiaires page loads
    # Capture golden master : SC-013-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-013-01 : La page bénéficiaires affiche la liste des bénéficiaires actifs avec IBAN masqué et badge zone SEPA
    When the beneficiaires page loads
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/beneficiaires.xhtml
    Then display only the beneficiaries associated with the authenticated user's customer account.

  @a-confirmer
  Scenario: RG-013-004 — exclude beneficiaries with active flag set to false from the displayed list
    # Capture golden master : SC-013-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-013-02 : Ajouter un bénéficiaire avec IBAN MOD-97 valide affiche 'Bénéficiaire ajouté avec succès' et l'entrée dans la liste
    And les données saisies : #addForm\:newLabel = « [retro] test beneficiaire »; #addForm\:newHolderName = « Retro Test SA »; #addForm\:newIban = « FR3530004000000123456789010 »; #addForm\:newBic = « HSBCFRPPXXX »; #addForm\:newBank = « HSBC France »
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/beneficiaires.xhtml
    And il saisit « [retro] test beneficiaire » dans #addForm\:newLabel
    And il saisit « Retro Test SA » dans #addForm\:newHolderName
    And il saisit « FR3530004000000123456789010 » dans #addForm\:newIban
    And il clique sur input[value="Verifier l'IBAN"]
    And il attend le texte « IBAN valide »
    And il saisit « HSBCFRPPXXX » dans #addForm\:newBic
    And il saisit « HSBC France » dans #addForm\:newBank
    And il clique sur input[value='Ajouter le beneficiaire']
    And il attend le texte « None »
    Then exclude beneficiaries with active flag set to false from the displayed list.

  @a-confirmer
  Scenario: RG-013-005 — display the beneficiaries list sorted alphabetically ascending by label
    # Capture golden master : SC-013-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-013-02 : Ajouter un bénéficiaire avec IBAN MOD-97 valide affiche 'Bénéficiaire ajouté avec succès' et l'entrée dans la liste
    And les données saisies : #addForm\:newLabel = « [retro] test beneficiaire »; #addForm\:newHolderName = « Retro Test SA »; #addForm\:newIban = « FR3530004000000123456789010 »; #addForm\:newBic = « HSBCFRPPXXX »; #addForm\:newBank = « HSBC France »
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/beneficiaires.xhtml
    And il saisit « [retro] test beneficiaire » dans #addForm\:newLabel
    And il saisit « Retro Test SA » dans #addForm\:newHolderName
    And il saisit « FR3530004000000123456789010 » dans #addForm\:newIban
    And il clique sur input[value="Verifier l'IBAN"]
    And il attend le texte « IBAN valide »
    And il saisit « HSBCFRPPXXX » dans #addForm\:newBic
    And il saisit « HSBC France » dans #addForm\:newBank
    And il clique sur input[value='Ajouter le beneficiaire']
    And il attend le texte « None »
    Then display the beneficiaries list sorted alphabetically ascending by label.

  @a-confirmer
  Scenario: RG-013-006 — the authenticated user has no active beneficiaries
    # Capture golden master : SC-013-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-013-02 : Ajouter un bénéficiaire avec IBAN MOD-97 valide affiche 'Bénéficiaire ajouté avec succès' et l'entrée dans la liste
    And les données saisies : #addForm\:newLabel = « [retro] test beneficiaire »; #addForm\:newHolderName = « Retro Test SA »; #addForm\:newIban = « FR3530004000000123456789010 »; #addForm\:newBic = « HSBCFRPPXXX »; #addForm\:newBank = « HSBC France »
    When the authenticated user has no active beneficiaries
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/beneficiaires.xhtml
    And il saisit « [retro] test beneficiaire » dans #addForm\:newLabel
    And il saisit « Retro Test SA » dans #addForm\:newHolderName
    And il saisit « FR3530004000000123456789010 » dans #addForm\:newIban
    And il clique sur input[value="Verifier l'IBAN"]
    And il attend le texte « IBAN valide »
    And il saisit « HSBCFRPPXXX » dans #addForm\:newBic
    And il saisit « HSBC France » dans #addForm\:newBank
    And il clique sur input[value='Ajouter le beneficiaire']
    And il attend le texte « None »
    Then display the message "Aucun beneficiaire enregistre. Utilisez le formulaire ci-dessus pour en ajouter un."

  @a-confirmer
  Scenario: RG-013-007 — displaying an IBAN in the beneficiaries table
    # Capture golden master : SC-013-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-013-01 : La page bénéficiaires affiche la liste des bénéficiaires actifs avec IBAN masqué et badge zone SEPA
    When displaying an IBAN in the beneficiaries table
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/beneficiaires.xhtml
    Then show the first 4 characters followed by "***" followed by the last 4 characters of the stored IBAN.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  Scenario: RG-013-010 — displaying a beneficiary
    # Capture golden master : SC-013-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-013-01 : La page bénéficiaires affiche la liste des bénéficiaires actifs avec IBAN masqué et badge zone SEPA
    When displaying a beneficiary
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/beneficiaires.xhtml
    Then show a SEPA zone badge computed from the IBAN country code: "✓ SEPA" if the country code belongs to the SEPA zone, "Hors SEPA" otherwise.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-013-029 exclu — dette technique legacy, hors scope réécriture

  # RG-013-031 exclu — dette technique legacy, hors scope réécriture

  @a-confirmer
  # RG-013-035 — comportement corrigé proposé (BUG-047, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-047 @proposition
  Scenario: RG-013-035 — the user deletes a beneficiary
    Given the legacy context of rule RG-013-035
    When the user deletes a beneficiary
    Then verify that the beneficiary belongs to the authenticated user before executing the deletion.
