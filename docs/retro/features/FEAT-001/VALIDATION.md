# VALIDATION — FEAT-001 « Connexion »

_Généré par retro-validation — 2026-10-03._
_Source : `src/main/webapp/login.xhtml`, `src/main/java/com/accenture/bank/web/LoginBean.java`, `src/main/java/com/accenture/bank/ejb/AccountService.java`._

---

## 1. Validation front

### Champ `loginForm:username` (Identifiant)

**VAL-001-01-username-F1**
WHEN user submits loginForm with empty "Identifiant" field THE SYSTEM SHALL display "Identifiant requis"
  Trigger  : soumission
  Check    : required
  Message  : "Identifiant requis"
  Observed : outcome=rejected — vérifié live (JSF PROCESS_VALIDATIONS phase, message affiché dans zone développement)

> **[Dette-legacy]** En mode Production (JSF stage != Development), le message "Identifiant requis" n'est PAS affiché. La page contient `h:messages globalOnly="true"` qui ne capture que les messages globaux (clientId=null). La validation `required` ajoute un message par champ (clientId="loginForm:username"), donc invisible pour l'utilisateur réel. En développement, la zone "Project Stage[Development]: Unhandled Messages" rattrape ces messages.
> Preuve : (`h:messages globalOnly="true"`), FLOW.md SCR-001-02 (messages dans zone développement).

---

### Champ `loginForm:password` (Mot de passe)

**VAL-001-01-password-F1**
WHEN user submits loginForm with empty "Mot de passe" field THE SYSTEM SHALL display "Mot de passe requis"
  Trigger  : soumission
  Check    : required
  Message  : "Mot de passe requis"
  Observed : outcome=rejected — vérifié live (JSF PROCESS_VALIDATIONS phase)

> **[Dette-legacy]** Même problème de visibilité que pour `username` : message invisible en mode Production via `h:messages globalOnly="true"`.

---

## 2. Validation back

### Contrôles applicatifs (`LoginBean.login()` + `AccountService.validatePassword()`)

Aucune validation `required` explicite dans la couche applicative. Le flux est :

1. `login()` appelle `accountService.validatePassword(username, password)`
2. `validatePassword` appelle `findByUsername(username)` → retourne `null` si username vide ou inconnu
3. Si résultat null → `false` → `login()` ajoute message global "Identifiants incorrects" et retourne `null`

**Conséquence d'un username vide passé à l'API back (sans JSF lifecycle)** : `findByUsername("")` retourne `null` → `validatePassword` retourne `false` → affichage "Identifiants incorrects". Pas de message spécifique "Identifiant requis".

**Conséquence d'un password vide passé à l'API back** : client trouvé → `c.getPassword().equals("")` → `false` → "Identifiants incorrects".

### Contrôles schéma

CUSTOMERS.USERNAME : VARCHAR, NOT NULL — contrainte INSERT/UPDATE uniquement, sans effet sur le SELECT d'authentification.
CUSTOMERS.PASSWORD : VARCHAR, NOT NULL — idem.
Source : DATA-DICTIONARY.md (PUBLIC.CUSTOMERS).

### Triggers et procédures stockées

Aucun trigger ni procédure stockée affectant la validation de connexion identifié dans DATA-DICTIONARY.md.

---

## 3. Matrice de divergence

| Champ | Kind | Verdict | Front | Back | Conséquence (générée par script) | Nature |
|---|---|---|---|---|---|---|
| `loginForm:username` | required | **front_seul** | VAL-001-01-username-F1 | — | back accepte ce que front refuse — rejetée par le serveur (observé à l'écran) | métier — la réécriture doit ajouter une validation required côté serveur (ex. `@NotBlank` sur LoginRequest DTO) |
| `loginForm:password` | required | **front_seul** | VAL-001-01-password-F1 | — | back accepte ce que front refuse — rejetée par le serveur (observé à l'écran) | métier — idem |

> **Note sur la conséquence :** Le script indique "rejetée par le serveur (observé à l'écran)" parce que le bypass HTML5 (`removeAttribute('required')` + `form.noValidate=true`) ne contourne pas la validation JSF server-side (PROCESS_VALIDATIONS). La véritable couche applicative (`login()`) ne valide pas required explicitement — ce comportement (message générique "Identifiants incorrects" pour champ vide) est le comportement de la couche business, pas du cycle JSF.

---

## 4. Résumé des dettes techniques

| Champ | Défaut | Preuve | Ne pas reproduire |
|---|---|---|---|
| Tous | Messages required invisibles en production (`h:messages globalOnly=true` ne capture pas les per-field messages JSF) | SCR-001-02 | La réécriture doit retourner des erreurs par champ depuis l'API (HTTP 422 + `errors: [{field: "username", message: "..."}]`) |
| `loginForm:password` | Mots de passe comparés en clair — `c.getPassword().equals(password)` sans hachage | | La réécriture doit utiliser BCrypt ou Argon2 |
| Session | Session fixation (CWE-384) : session non invalidée avant écriture du contexte authentifié | JIRA-9500 | La réécriture JWT n'a pas cette vulnérabilité (stateless) |
