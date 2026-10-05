# FIELDS — FEAT-001 « Connexion »

_Source : `src/main/webapp/login.xhtml`, observé le 2026-10-03 via retro-ui._

## Formulaire `loginForm`

| Champ | Type | Label | Obligatoire | Message validation | Longueur max | Valeur par défaut | Notes |
|---|---|---|---|---|---|---|---|
| `loginForm:username` | text | Identifiant | Oui | « Identifiant requis » | — | — | `h:inputText` lié à `#{loginBean.username}` |
| `loginForm:password` | password | Mot de passe | Oui | « Mot de passe requis » | — | — | `h:inputSecret` lié à `#{loginBean.password}` — valeur non exposée dans l'arbre d'accessibilité |

## Boutons

| Bouton | Action | Type |
|---|---|---|
| « Me connecter » | `#{loginBean.login}` | submit |

## Messages

| Zone | Type | Portée | Message d'erreur d'authentification |
|---|---|---|---|
| `h:messages` (classe `.error`) | FacesMessage SEVERITY_ERROR | Globale (globalOnly=true) | « Identifiants incorrects — Vérifiez votre login et mot de passe » |

## Activations dynamiques

Aucune.

## Observations

- La redirection post-connexion dépend du rôle : CLIENT → `/dashboard`, CONSEILLER → `/conseiller-workspace`, ADMIN → `/admin` (faces-redirect=true).
- La page reste accessible à un utilisateur déjà authentifié (pas de redirection vers dashboard). `AppFilter.java` exclut vraisemblablement `/login.xhtml` de ses vérifications.
- `[Dette-legacy]` Mots de passe stockés et comparés en clair : `AccountService.validatePassword` appelle `c.getPassword().equals(password)` sans hachage.
- `[Dette-legacy]` Session fixation (CWE-384) : la session n'est pas invalidée avant l'écriture du contexte utilisateur authentifié (référence JIRA-9500).
