---
feature: FEAT-030
name: Contrôle d'accès RBAC
interface: Transversal
status: current
generated_by: retro-rules
date: 2026-10-03
---

# RG — FEAT-030 « Contrôle d'accès RBAC »

## Périmètre

Préoccupation transversale. Contrôle toute requête HTTP vers les pages `.xhtml` (via `AppFilter`) et la servlet de téléchargement (`/docs/download`). Expose un bean CDI (`SecurityBean`) pour la visibilité conditionnelle des liens de navigation dans les gabarits JSF.

Rôles déclarés (enum `CustomerRole`) : `CLIENT`, `CONSEILLER`, `ADMIN`.
Le rôle par défaut d'un `Customer` à la création est `CLIENT`.

---

## Règles

RG-030-001 [À confirmer]
WHEN a request targets a URL containing `login.xhtml`, `powens-callback.xhtml`, or `/javax.faces.resource/`
THE SYSTEM SHALL serve the resource without requiring an authenticated session.

RG-030-002 [À confirmer]
WHEN a request to a `*.xhtml` URL arrives (excluding those in RG-030-001)
and no `currentCustomer` object is found in the HTTP session
THE SYSTEM SHALL redirect the request to `login.xhtml`.

RG-030-003 [À confirmer]
IF an authenticated user whose role is not ADMIN requests `admin.xhtml`
THEN THE SYSTEM SHALL redirect the request to `dashboard.xhtml`.

RG-030-004 [À confirmer]
IF an authenticated user whose role is neither CONSEILLER nor ADMIN requests `conseiller-workspace.xhtml`
THEN THE SYSTEM SHALL redirect the request to `dashboard.xhtml`.

RG-030-005 [À confirmer]
WHEN a user with role ADMIN successfully authenticates
THE SYSTEM SHALL redirect to `admin.xhtml`.

RG-030-006 [À confirmer]
WHEN a user with role CONSEILLER successfully authenticates
THE SYSTEM SHALL redirect to `conseiller-workspace.xhtml`.

RG-030-007 [À confirmer]
WHEN a user with role CLIENT successfully authenticates
THE SYSTEM SHALL redirect to `dashboard.xhtml`.

RG-030-008 [À confirmer]
WHEN a navigation menu is rendered
THE SYSTEM SHALL display the "Espace Conseiller" navigation link only if the current user's role is CONSEILLER or ADMIN.

RG-030-009 [À confirmer]
WHEN a navigation menu is rendered
THE SYSTEM SHALL display the "Administration" navigation link only if the current user's role is ADMIN.

RG-030-010 [À confirmer]
WHEN a user logs out
THE SYSTEM SHALL invalidate the HTTP session and redirect to `login.xhtml`.

RG-030-011 [À confirmer]
IF a JSF ViewExpiredException occurs
THEN THE SYSTEM SHALL redirect to `login.xhtml`.

RG-030-012 [À confirmer]
IF a request to `/docs/download` arrives and no `currentCustomer` is present in the HTTP session
THEN THE SYSTEM SHALL redirect to `login.xhtml`.

RG-030-013 [À confirmer]
THE SYSTEM SHALL allow any authenticated user, regardless of role, to access the `/docs/download` endpoint.

RG-030-014 [Suspect]
WHEN a user successfully authenticates
THE SYSTEM SHALL invalidate the existing HTTP session and create a fresh session before storing the authentication context.

RG-030-015 [Suspect]
THE SYSTEM SHALL enforce role-based access control at the service layer independently of the HTTP filter.
            grep exhaustif : aucun @RolesAllowed, @DenyAll, @PermitAll, isCallerInRole dans src/main/java/
