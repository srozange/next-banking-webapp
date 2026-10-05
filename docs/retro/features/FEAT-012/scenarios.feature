Feature: Comptes Tiers — Open Banking Powens
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  @confirme
  @a-confirmer
  @confirme
  @a-confirmer
  # RG-012-009 — comportement corrigé proposé (BUG-040, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-040 @proposition
  Scenario: RG-012-009 — le callback Powens est reçu avec un paramètre error
    Given the legacy context of rule RG-012-009
    When le callback Powens est reçu avec un paramètre error
    Then déconnecter la session Powens et informer l'utilisateur de la raison du retour.

  @a-confirmer
  @a-confirmer
  @confirme
  # RG-012-014 exclu — dette technique legacy, hors scope réécriture

  @a-confirmer
  @a-confirmer
  # RG-012-018 — comportement corrigé proposé (BUG-042, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-042 @proposition
  Scenario: RG-012-018 — un compte externe renvoyé par l'API Powens a un solde manquant …
    Given the legacy context of rule RG-012-018
    And un compte externe renvoyé par l'API Powens a un solde manquant (champ absent ou null)
    When the action described by rule RG-012-018 is performed
    Then distinguer l'absence de solde d'un solde effectivement nul.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # RG-012-027 — comportement corrigé proposé (BUG-043, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-043 @proposition
  Scenario: RG-012-027 — l'API Powens retourne une réponse HTTP >= 400 lors du chargement des comptes ou …
    Given the legacy context of rule RG-012-027
    And l'API Powens retourne une réponse HTTP >= 400 lors du chargement des comptes ou des transactions
    When the action described by rule RG-012-027 is performed
    Then informer l'utilisateur de l'indisponibilité temporaire du service.

