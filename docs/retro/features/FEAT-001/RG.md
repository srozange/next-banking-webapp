---
feature: FEAT-001
name: Connexion
status: current
extracted_at: 2026-10-03
extractor: retro-rules
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# Règles de gestion — FEAT-001 « Connexion »

---

## Accès et redirection

RG-001-001 [Confirmé]
WHEN an unauthenticated user requests any `.xhtml` page
other than `login.xhtml`, `powens-callback.xhtml`, or a JSF static resource (`/javax.faces.resource/`)
THE SYSTEM SHALL redirect to `login.xhtml`.

RG-001-002 [Confirmé]
WHEN `login.xhtml`, `powens-callback.xhtml`, or a JSF static resource (`/javax.faces.resource/`) is requested
THE SYSTEM SHALL serve the page without requiring an authenticated session.

---

## Authentification — succès

RG-001-003 [À confirmer]
WHEN a user submits the login form with a username and password that match a registered customer
THE SYSTEM SHALL create an authenticated session storing the customer context and redirect to the role-specific home page.

RG-001-004 [À confirmer]
IF the authenticated customer's role is ADMIN THEN THE SYSTEM SHALL redirect to `/admin` after login.

RG-001-005 [À confirmer]
IF the authenticated customer's role is CONSEILLER THEN THE SYSTEM SHALL redirect to `/conseiller-workspace` after login.

RG-001-006 [À confirmer]
IF the authenticated customer's role is CLIENT THEN THE SYSTEM SHALL redirect to `/dashboard` after login.

---

## Authentification — échec

RG-001-007 [À confirmer]
WHEN a user submits the login form with credentials that do not match any registered customer
THE SYSTEM SHALL keep the user on the login page and display the error message
"Identifiants incorrects — Vérifiez votre login et mot de passe".

---

## Validation des champs

RG-001-008 [À confirmer]
IF the username field is empty when the login form is submitted
THEN THE SYSTEM SHALL reject the form and display "Identifiant requis".
  Champ   : loginForm:username (FEAT-001/SCR-001-01)

RG-001-009 [À confirmer]
IF the password field is empty when the login form is submitted
THEN THE SYSTEM SHALL reject the form and display "Mot de passe requis".
  Champ   : loginForm:password (FEAT-001/SCR-001-01)

---

## Déconnexion

RG-001-010 [À confirmer]
WHEN a user initiates logout THE SYSTEM SHALL invalidate the current session and redirect to `/login.xhtml`.

---

## Contrôle d'accès post-authentification

RG-001-011 [À confirmer]
WHEN an authenticated customer whose role is not ADMIN requests `/admin.xhtml`
THE SYSTEM SHALL redirect to `/dashboard.xhtml`.

RG-001-012 [À confirmer]
WHEN an authenticated customer whose role is CLIENT requests `/conseiller-workspace.xhtml`
THE SYSTEM SHALL redirect to `/dashboard.xhtml`.

---

## Audit

RG-001-013 [À confirmer]
WHEN a successful login occurs
THE SYSTEM SHALL record an audit entry with action LOGIN, detail "Connexion réussie", and the resolved client IP address.

RG-001-014 [À confirmer]
WHEN a failed login attempt occurs
THE SYSTEM SHALL record an audit entry with action LOGIN_FAILED, detail "Tentative échouée", and the client IP address.

RG-001-015 [À confirmer]
WHEN a user initiates logout
THE SYSTEM SHALL record an audit entry with action LOGOUT and detail "Déconnexion".

RG-001-016 [À confirmer]
WHEN recording the client IP address for an audit log entry
THE SYSTEM SHALL use the first segment of the `X-Forwarded-For` request header if present and non-empty,
otherwise the connection remote address.

---

## Dettes legacy (comportements à corriger dans la réécriture)

RG-001-017 [Suspect]
IF a customer with `active = false` submits valid credentials
THEN THE SYSTEM SHALL reject the login.

RG-001-018 [Suspect]
THE SYSTEM SHALL store and verify passwords using a secure one-way hashing function.

RG-001-019 [Suspect]
WHEN a user authenticates successfully
THE SYSTEM SHALL invalidate the existing HTTP session before writing the authenticated user context.
