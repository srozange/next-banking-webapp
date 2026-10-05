Feature: Contrôle d'accès RBAC
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

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
  # RG-030-014 — comportement corrigé proposé (BUG-092, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-092 @proposition
  Scenario: RG-030-014 — a user successfully authenticates
    Given the legacy context of rule RG-030-014
    When a user successfully authenticates
    Then invalidate the existing HTTP session and create a fresh session before storing the authentication context.

  # RG-030-015 — comportement corrigé proposé (BUG-093, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-093 @proposition
  Scenario: RG-030-015 — enforce role-based access control at the service layer independently of the HTTP …
    Given the legacy context of rule RG-030-015
    When the action described by rule RG-030-015 is performed
    Then enforce role-based access control at the service layer independently of the HTTP filter.
