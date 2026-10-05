Feature: Callback OAuth Powens
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # Règles d'accès et de transit
  @confirme
  Scenario: RG-014-001 — allow access to /powens-callback.xhtml without requiring user authentication
    # Capture golden master : SC-014-01
    Given un utilisateur sans rôle déclaré dans la capture
    And l'état initial de la capture SC-014-01 : La page de callback Powens est accessible sans authentification et redirige vers comptes-tiers.xhtml quand les paramètres sont absents
    When la requête GET /banking-legacy/powens-callback.xhtml est envoyée
    Then allow access to `/powens-callback.xhtml` without requiring user authentication.

  @confirme
  @confirme
  @a-confirmer
  @confirme
  @a-confirmer
  @a-confirmer
  @confirme
  # Règles de chargement des données externes
  # RG-014-010 exclu — dette technique legacy, hors scope réécriture

