Feature: Intégration Powens (Open Banking — agrégation)
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
  # Chargement des comptes
  # RG-028-012 exclu — dette technique legacy, hors scope réécriture

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-028-025 — comportement corrigé proposé (BUG-089, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-089 @proposition
  Scenario: RG-028-025 — the Powens API returns an HTTP 4xx or 5xx response for /users/me/accounts or …
    Given the legacy context of rule RG-028-025
    And the Powens API returns an HTTP 4xx or 5xx response for /users/me/accounts or /users/me/accounts/{id}/transactions,
    When the action described by rule RG-028-025 is performed
    Then propagate an error indicator to the caller so that the user can be informed of the service unavailability.

  @a-confirmer
  @a-confirmer
