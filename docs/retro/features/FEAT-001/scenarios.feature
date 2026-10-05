Feature: Connexion
  # Généré par retro-golden-master depuis RG.md
  # Given / When complétés depuis les captures par complete-gherkin-from-scenarios.py

  # Accès et redirection
  @confirme
  Scenario: RG-001-001 — an unauthenticated user requests any .xhtml page other than login.xhtml …
    # Capture golden master : SC-001-02
    Given un utilisateur sans rôle déclaré dans la capture
    And l'état initial de la capture SC-001-02 : Tout accès non authentifié à une page protégée redirige vers login.xhtml (HTTP 302)
    When an unauthenticated user requests any `.xhtml` page other than `login.xhtml`, `powens-callback.xhtml`, or a JSF static resource (`/javax.faces.resource/`)
    And la requête GET /banking-legacy/dashboard.xhtml est envoyée
    Then redirect to `login.xhtml`.

  @confirme
  Scenario: RG-001-002 — login.xhtml, powens-callback.xhtml, or a JSF static resource …
    # Capture golden master : SC-001-01
    Given un utilisateur sans rôle déclaré dans la capture
    And l'état initial de la capture SC-001-01 : La page de connexion est accessible sans authentification (HTTP 200)
    When `login.xhtml`, `powens-callback.xhtml`, or a JSF static resource (`/javax.faces.resource/`) is requested
    And la requête GET /banking-legacy/login.xhtml est envoyée
    Then serve the page without requiring an authenticated session.

  # Authentification — succès
  @a-confirmer
  Scenario: RG-001-003 — a user submits the login form with a username and password that match a …
    # Capture golden master : SC-001-04
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-001-04 : Un CLIENT authentifié accède au tableau de bord (session valide — RG-001-003 et RG-001-006 testés via SC-002-01)
    When a user submits the login form with a username and password that match a registered customer
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then create an authenticated session storing the customer context and redirect to the role-specific home page.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  Scenario: RG-001-006 — the authenticated customer's role is CLIENT
    # Capture golden master : SC-001-04
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-001-04 : Un CLIENT authentifié accède au tableau de bord (session valide — RG-001-003 et RG-001-006 testés via SC-002-01)
    And the authenticated customer's role is CLIENT
    When l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    Then redirect to `/dashboard` after login.

  # Authentification — échec
  @a-confirmer
  Scenario: RG-001-007 — a user submits the login form with credentials that do not match any registered …
    # Capture golden master : SC-001-03
    Given un utilisateur sans rôle déclaré dans la capture
    And l'état initial de la capture SC-001-03 : Des identifiants incorrects maintiennent l'utilisateur sur login.xhtml avec le message 'Identifiants incorrects'
    When a user submits the login form with credentials that do not match any registered customer
    And la requête POST /banking-legacy/login.xhtml est envoyée
    And avec le corps (application/x-www-form-urlencoded) : {"loginForm": "loginForm", "loginForm:username": "retro-compte-inexistant", "loginForm:password": "retro-mauvais-mot-de-passe", "loginForm:j_idt11": "Me connecter"}
    Then keep the user on the login page and display the error message "Identifiants incorrects — Vérifiez votre login et mot de passe".

  @a-confirmer
  # Déconnexion
  @a-confirmer
  Scenario: RG-001-010 — a user initiates logout
    # Capture golden master : SC-001-05
    Given un utilisateur de rôle « CLIENT »
    And l'état initial de la capture SC-001-05 : La déconnexion invalide la session et redirige vers login.xhtml
    When a user initiates logout
    And l'utilisateur ouvre http://localhost:8080/banking-legacy/dashboard.xhtml
    And il clique sur a:has-text('Deconnexion')
    And il attend le texte « Identifiant »
    Then invalidate the current session and redirect to `/login.xhtml`.

  @a-confirmer
  @a-confirmer
  @a-confirmer
  @a-confirmer
  # Dettes legacy (comportements à corriger dans la réécriture)
  # RG-001-017 — comportement corrigé proposé (BUG-001, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-001 @proposition
  Scenario: RG-001-017 — a customer with active = false submits valid credentials
    Given the legacy context of rule RG-001-017
    And a customer with `active = false` submits valid credentials
    When the action described by rule RG-001-017 is performed
    Then reject the login.

  # RG-001-018 — comportement corrigé proposé (BUG-002, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-002 @proposition
  Scenario: RG-001-018 — store and verify passwords using a secure one-way hashing function
    Given the legacy context of rule RG-001-018
    When the action described by rule RG-001-018 is performed
    Then store and verify passwords using a secure one-way hashing function.

  # RG-001-019 — comportement corrigé proposé (BUG-003, décision « corriger ») : contrainte cible, non opposable à une autre correction ; hors golden master
  @suspect
  @BUG-003 @proposition
  Scenario: RG-001-019 — a user authenticates successfully
    Given the legacy context of rule RG-001-019
    When a user authenticates successfully
    Then invalidate the existing HTTP session before writing the authenticated user context.
