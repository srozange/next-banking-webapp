Feature: Bourse et watchlist
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # Chargement et navigation
  @a-confirmer
  Scenario: RG-009-001 — the user navigates to /bourse.xhtml
    # Capture golden master : SC-009-01
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-009-01 : La page bourse charge la watchlist et affiche les données de marché AlphaVantage pour le premier symbole
    When the user navigates to `/bourse.xhtml`
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/bourse.xhtml
    Then load the watchlist, automatically select the first item (ordered by market then symbol), and fetch its historical data before rendering the page.

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
  # Dettes techniques legacy
  # RG-009-020 exclu — dette technique legacy, hors scope réécriture

  # RG-009-021 — comportement corrigé proposé (BUG-034, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-034 @proposition
  Scenario: RG-009-021 — retrieve the AlphaVantage API key exclusively from a secure configuration source …
    Given the legacy context of rule RG-009-021
    When the action described by rule RG-009-021 is performed
    Then retrieve the AlphaVantage API key exclusively from a secure configuration source (environment variable or system property) and never fall back to a value committed to source code.
