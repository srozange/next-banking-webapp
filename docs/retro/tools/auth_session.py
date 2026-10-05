#!/usr/bin/env python3
"""
Module partagé — connexion par script, sans jamais afficher de mot de passe.

Déplacé depuis crawl-ui.py, sans changer son comportement (retro-skill-ejv) :
LOGIN_USERNAME_SELECTORS, LOGIN_SUBMIT_SELECTORS, try_login, read_dotenv,
validate_accounts, resolve_password, resolve_accounts, ENV_PREFIX, ENV_NAME_RE. crawl-ui.py et
login-state.py l'importent (sys.path.insert(0, …), comme targeted-har.py
importe har_sanitize.py) — script du même skill, pas d'import entre skills
(DEC-029).

try_login() ne journalise plus lui-même ses erreurs Playwright absorbées :
il reçoit `note_warning(where, exc)` en paramètre, fourni par l'appelant
(crawl-ui.py garde le sien, qui alimente son propre `warnings` de sortie ;
login-state.py fournit le sien, qui passe par `redact()`). Comportement
inchangé pour crawl-ui.py, qui continue de passer sa propre fonction.

Ajouts de ce module (retro-skill-ejv, connexion MCP sans saisie de mot de
passe) :
  - login_succeeded(page, goto_response=None) : constate si la
    connexion a abouti, et distingue les deux cas qu'un script ne peut pas
    résoudre seul (HTTP Basic côté navigateur, jeton en sessionStorage).
  - detect_session_storage_only(page) : indication seulement (jamais
    bloquante à elle seule), écrite dans warnings par l'appelant.
  - redact(text, secrets) : masque toute valeur de secrets (longueur >= 4)
    dans un message avant de l'écrire sur stderr — un message d'exception
    Playwright peut citer une URL de connexion en GET qui contient le mot de
    passe.
"""

import json
import re
import sys
from pathlib import Path

ENV_NAME_RE = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")
ENV_PREFIX = "env:"

# Sélecteurs de champs de login username
LOGIN_USERNAME_SELECTORS = [
    "input[type='email']",
    "input[name*='username' i]",
    "input[name*='login' i]",
    "input[name*='user' i]",
    "input[id*='username' i]",
    "input[id*='login' i]",
    "input[id*='email' i]",
    "input[type='text']",  # fallback générique
]

# Boutons de soumission de login
LOGIN_SUBMIT_SELECTORS = [
    "input[type='submit']",
    "button[type='submit']",
    "button:text-matches('(login|sign.?in|connexion|connect|se connecter|entrer)', 'i')",
    "[value]:text-matches('(login|sign.?in|connexion)', 'i')",
]


def try_login(page, username: str, password: str, verbose: bool, note_warning) -> tuple[bool, str | None]:
    """Détecte et remplit un formulaire de login. Retourne (tentative faite,
    nom du champ identifiant rempli — attribut `name` sinon `id`, ou `None`).
    Le nom de champ sert à l'exempter du masquage par valeur dans le HAR
    (retro-skill-sas.4) : SENSITIVE_KEY_PATTERN ne reconnaît pas ce champ,
    donc un mot de passe faible identique au login le ferait sinon masquer.

    `note_warning(where, exc)` est fourni par l'appelant — jamais afficher ni
    journaliser une valeur ici."""
    # Chercher champ username
    username_el = None
    for sel in LOGIN_USERNAME_SELECTORS:
        try:
            el = page.query_selector(sel)
            if el and el.is_visible():
                username_el = el
                break
        except Exception as exc:  # noqa: BLE001 — erreur Playwright/tierce non typable
            note_warning('recherche du champ identifiant', exc)
            continue

    # Chercher champ password
    password_el = None
    try:
        password_el = page.query_selector("input[type='password']")
    except Exception as exc:  # noqa: BLE001 — erreur Playwright/tierce non typable
        note_warning('recherche du champ mot de passe', exc)

    if not username_el or not password_el:
        if verbose:
            print(f"[login] Pas de formulaire de login sur {page.url}", file=sys.stderr)
        return False, None

    username_field = None
    try:
        username_field = username_el.get_attribute("name") or username_el.get_attribute("id")
    except Exception as exc:  # noqa: BLE001 — erreur Playwright/tierce non typable
        note_warning("lecture du nom du champ identifiant", exc)

    username_el.fill(username)
    if verbose:
        print(f"[login] Remplissage username={username}", file=sys.stderr)

    password_el.fill(password)

    # Soumettre
    submitted = False
    for sel in LOGIN_SUBMIT_SELECTORS:
        try:
            el = page.query_selector(sel)
            if el and el.is_visible():
                el.click()
                submitted = True
                break
        except Exception as exc:  # noqa: BLE001 — erreur Playwright/tierce non typable
            note_warning('clic du bouton de login', exc)
            continue

    if not submitted:
        password_el.press("Enter")

    try:
        page.wait_for_load_state("networkidle", timeout=12000)
    except Exception as exc:  # noqa: BLE001 — erreur Playwright/tierce non typable
        note_warning('attente networkidle après login', exc)

    if verbose:
        print(f"[login] Après soumission → {page.url}", file=sys.stderr)
    return True, username_field


def login_succeeded(page, goto_response=None) -> tuple[bool, str | None]:
    """Constate si la connexion a abouti, après try_login. Retourne
    (logged_in, reason) :
      - (False, "http_basic_ui") si la première navigation (`goto_response`,
        la réponse de `page.goto`) répond 401 avec l'en-tête WWW-Authenticate —
        fenêtre d'authentification du navigateur, hors de portée de
        storageState (aucun cookie, aucun formulaire à remplir) ;
      - (False, "still_on_login_form") si un `input[type=password]` est
        encore visible sur la page ;
      - sinon (True, None).
    Ne lit ni n'affiche jamais de valeur — seulement des en-têtes et un
    compte d'éléments."""
    if goto_response is not None:
        try:
            status = goto_response.status
            headers = goto_response.headers or {}
        except Exception:  # noqa: BLE001 — objet Playwright/fake non typable
            status, headers = None, {}
        if status == 401 and any(str(k).lower() == "www-authenticate" for k in headers):
            return False, "http_basic_ui"

    try:
        password_visible = page.locator("input[type='password']").count() > 0
    except Exception:  # noqa: BLE001 — erreur Playwright/tierce non typable
        password_visible = False
    if password_visible:
        return False, "still_on_login_form"

    return True, None


def detect_session_storage_only(page) -> bool:
    """Indication seulement (jamais bloquante à elle seule ici — l'appelant
    l'écrit dans warnings et, pour login-state.py, la transforme en
    logged_in: false / reason: session_storage_only) : True si le
    sessionStorage de l'origine contient des clés après la connexion, alors
    que le storage_state du contexte (cookies + localStorage) n'a rien à
    montrer — signe que le jeton de session vit dans sessionStorage, absent
    de storageState Playwright."""
    try:
        session_keys = page.evaluate("Object.keys(window.sessionStorage || {}).length")
    except Exception:  # noqa: BLE001 — erreur Playwright/tierce non typable
        return False
    if not session_keys:
        return False
    try:
        state = page.context().storage_state()
    except Exception:  # noqa: BLE001 — erreur Playwright/tierce non typable
        return False
    cookies = state.get("cookies") or []
    origins = state.get("origins") or []
    local_storage_present = any((o.get("localStorage") or []) for o in origins)
    return not cookies and not local_storage_present


def redact(text: str, secrets: list[str]) -> str:
    """Remplace chaque valeur de `secrets` (longueur >= 4, pour ne jamais
    masquer un caractère isolé ou une chaîne vide) par '***' dans `text`.
    Appelé par tout message écrit sur stderr par crawl-ui.py et
    login-state.py (dont leurs note_warning respectifs) : un message
    d'exception Playwright peut citer une URL de connexion en GET qui
    contient le mot de passe."""
    out = text
    for secret in secrets:
        if secret and len(secret) >= 4:
            out = out.replace(secret, "***")
    return out


def resolve_accounts(accounts_arg: str) -> list:
    """--accounts : chemin d'un fichier JSON ou JSON inline."""
    p = Path(accounts_arg)
    if p.is_file():
        return json.loads(p.read_text(encoding="utf-8"))
    return json.loads(accounts_arg)


def read_dotenv(path) -> dict[str, str]:
    """Lit un fichier dotenv NOM=valeur (retro/.env). Ignore les lignes vides
    et celles qui commencent par #. NOM est débarrassé de ses espaces ; la
    valeur est prise telle quelle après le premier '=', sans guillemets et
    sans interpolation. Fichier absent -> {}. Ne jamais imprimer ni
    journaliser une valeur."""
    if not path.is_file():
        return {}
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        name, _, value = stripped.partition("=")
        values[name.strip()] = value
    return values


def validate_accounts(accounts) -> list[str]:
    """Contrôle la forme de --accounts, sans résoudre aucune valeur : aucun
    mot de passe en clair (clé 'password' interdite — retro-skill-zs4),
    password_env obligatoire et nom brut de variable valide, sans préfixe
    'env:' (retro-skill-1fz). Retourne la liste des erreurs (vide = OK). Les
    messages ne citent jamais une valeur, seulement le rôle et le nom de la
    variable."""
    if not isinstance(accounts, list):
        return ["--accounts doit être une liste de comptes {role, login, password_env}"]
    errors = []
    for i, account in enumerate(accounts):
        if not isinstance(account, dict):
            errors.append(f"accounts[{i}] : doit être un objet {{role, login, password_env}}")
            continue
        where = f"accounts[{i}] ({account.get('role', 'user')})"
        if "password" in account:
            errors.append(f"{where} : 'password' interdit — déclarer password_env, "
                          "la valeur va dans retro/.env")
            continue
        name = account.get("password_env")
        if not name:
            errors.append(f"{where} : password_env manquant — aucun mot de passe déclaré")
            continue
        if not isinstance(name, str):
            errors.append(f"{where} : password_env doit être un nom de variable (chaîne)")
            continue
        if name.startswith(ENV_PREFIX):
            bare = name[len(ENV_PREFIX):]
            hint = f" : écrire {bare!r}" if ENV_NAME_RE.match(bare) else ""
            errors.append(f"{where} : préfixe 'env:' interdit dans password_env — "
                          f"nom brut de la variable attendu{hint}")
            continue
        if not ENV_NAME_RE.match(name):
            errors.append(f"{where} : password_env n'est pas un nom de variable valide "
                          "([A-Za-z_][A-Za-z0-9_]*)")
    return errors


def resolve_password(account, env, dotenv) -> str:
    """Résout le mot de passe d'un compte déjà validé (validate_accounts) :
    l'environnement d'abord, retro/.env (--env-file) ensuite. Ne lit jamais
    'password' — la clé est refusée en amont."""
    name = account.get("password_env")
    return env.get(name) or dotenv.get(name, "") or ""
