Feature: Demande et suivi de prêts
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # Accès
  @a-confirmer
  Scenario: RG-007-001 — the session has no authenticated customer
    # Capture golden master : SC-007-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-007-01 : La page de prêts charge avec le simulateur, le formulaire de demande et la liste des emprunts en cours
    And the session has no authenticated customer
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/pret.xhtml
    Then redirect to /login.

  @a-confirmer
  Scenario: RG-007-002 — grant access to the loan page to all authenticated roles …
    # Capture golden master : SC-007-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-007-01 : La page de prêts charge avec le simulateur, le formulaire de demande et la liste des emprunts en cours
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/pret.xhtml
    Then grant access to the loan page to all authenticated roles (CLIENT, CONSEILLER, ADMIN) with the same interface and no role-specific restrictions.

  # Simulateur de prêt — Validation
  @a-confirmer
  Scenario: RG-007-003 — simMontant is null or less than or equal to zero
    # Capture golden master : SC-007-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-007-02 : Le simulateur calcule mensualite, cout total et interets pour 10000 EUR à 3,5% sur 36 mois
    And les données saisies : #simForm\:simMontant = « 10000 »; #simForm\:simTaux = « 3.5 »; #simForm\:simDuree = « 36 »
    And simMontant is null or less than or equal to zero
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/pret.xhtml
    And il saisit « 10000 » dans #simForm\:simMontant
    And il saisit « 3.5 » dans #simForm\:simTaux
    And il saisit « 36 » dans #simForm\:simDuree
    And il clique sur input[type='submit'][value='Calculer']
    And il attend le texte « Mensualite »
    Then reject the simulation and display "Montant invalide".

  @a-confirmer
  Scenario: RG-007-004 — simTaux is less than zero or greater than 30
    # Capture golden master : SC-007-02
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-007-02 : Le simulateur calcule mensualite, cout total et interets pour 10000 EUR à 3,5% sur 36 mois
    And les données saisies : #simForm\:simMontant = « 10000 »; #simForm\:simTaux = « 3.5 »; #simForm\:simDuree = « 36 »
    And simTaux is less than zero or greater than 30
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/pret.xhtml
    And il saisit « 10000 » dans #simForm\:simMontant
    And il saisit « 3.5 » dans #simForm\:simTaux
    And il saisit « 36 » dans #simForm\:simDuree
    And il clique sur input[type='submit'][value='Calculer']
    And il attend le texte « Mensualite »
    Then reject the simulation and display "Taux invalide (0-30%)".

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-007-016 exclu — dette technique legacy, hors scope réécriture

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-007-028 — comportement corrigé proposé (BUG-025, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-025 @proposition
  Scenario: RG-007-028 — formDuree is between 301 and 360 months (inclusive)
    Given the legacy context of rule RG-007-028
    And formDuree is between 301 and 360 months (inclusive)
    When the action described by rule RG-007-028 is performed
    Then reject the loan request as invalid.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
