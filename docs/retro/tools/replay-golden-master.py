#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10"
# dependencies = ["requests>=2.28", "playwright>=1.40"]
# ///
"""
Rejoue les scénarios du golden master contre une cible (legacy ou nouvelle application).

Pour chaque scénario dans --scenarios :
  1. Lit scenario.json pour connaître le capture_adapter et les expected_outputs
  2. Envoie les inputs via l'adaptateur configuré
  3. Compare les sorties avec les expected
  4. Produit un JSON de résultats {scenarios: [{id, status, outputs, diff}]}

Pour l'adaptateur http, le corps de la requête est encodé selon le
Content-Type déclaré dans input-request.json : JSON (comportement par
défaut, inchangé) si absent ou application/json (ou suffixe +json) ;
application/x-www-form-urlencoded (ou tout autre type texte) envoie le
corps tel quel. Un corps non représentable fidèlement (booléen, null ou
objet imbriqué dans un formulaire, multipart/form-data, objet avec un
type non JSON) produit une erreur explicite plutôt qu'un envoi
approximatif. Les redirections sont suivies par défaut ; déclarer
"follow_redirects": false dans input-request.json pour obtenir la
réponse de redirection elle-même. "form_tokens_from": "<chemin>" (corps
form-urlencoded seulement) : la page est d'abord lue dans la même session
HTTP, et ses champs cachés (jeton JSF javax.faces.ViewState, _csrf…)
remplacent ceux capturés — un jeton est lié à la session, jamais rejouable.

Mode de comparaison de http_response, lu dans scenario.json:compare.mode puis
replay-config.json:comparison.http_response.mode :
  - "exact" (défaut) : égalité stricte du dict adaptateur (après ignore_fields) ;
  - "status+json_subset" : status_code égal ET chaque clé/valeur de
    expected.body présente dans actual.body (récursif ; listes comparées par
    index, longueurs égales exigées ; body attendu null = statut seul) ;
  - "status+body_contains" : status_code égal ET chaque chaîne de
    expected.body_contains présente dans le corps reçu (page HTML dont le
    reste change à chaque appel, jeton compris).
Les autres types de sortie sont toujours comparés en mode exact.

Chaque type de sortie est comparé au champ correspondant de la réponse de
l'adaptateur (extract_output) : http_response = dict entier ; dom_text =
champ dom_text ; db_state = champ db_state (sinon dict entier) ; mq_message =
champ message (sinon dict entier) ; stdout = champ stdout ; exit_code = champ
exit_code. dom_text et stdout : expected-dom.txt / expected-stdout.txt lus
comme texte brut, les deux côtés tronqués à DOM_TEXT_MAX_CHARS (limite de
l'adaptateur) puis normalisés (blancs consécutifs réduits à un espace, lignes
vides supprimées) ; diff unifié ligne à ligne. Mode de comparaison lu dans
scenario.json:compare.<type>.mode — "exact" (défaut, diff unifié),
"contains" : chaque ligne non vide de l'expected est un fragment à retrouver
comme sous-chaîne d'une ligne de l'actual, dans n'importe quel ordre (retro-
skill-x5v, scénarios writes: true dont l'état partagé s'allonge à chaque rejeu),
"contains-ci" : idem mais insensible à la casse (text-transform CSS, retro-skill-5zyk).

Adaptateur playwright et connexion (retro-skill-ejv) : la session du rôle est
chargée par storage_state (`adapters.playwright.storage_state_dir` de
replay-config.json, préparée par `login-state.py`), jamais par une saisie
rejouée — `input-actions.json` ne contient donc aucune étape de connexion, et
un `fill` dont le sélecteur ressemble à un champ mot de passe fait échouer le
scénario en erreur avant même de lancer le navigateur
(`password_fill_actions`). Sans `storage_state_dir`, comportement inchangé
(pages publiques, sans connexion). Sonde de session expirée (retro-skill-w7w,
DEC-080) : si `session_looks_expired()` détecte une redirection vers /login
après `page.goto(start_url)`, un re-login automatique est tenté au plus 1 fois
via `login-state.py` — `adapters.playwright.accounts` doit être déclaré. Un
logout explicite dans un scénario n'invalide plus les scénarios suivants du
même rôle : le re-login automatique gère la cascade.
Interface rendue côté client (retro-skill-k9f5.8) : le texte attendu peut arriver après
la fin de l'activité réseau. Une action `wait_for` avec `text` attend que ce texte figure
dans la page (casse et blancs ignorés) ; le texte final est relu jusqu'à ce que
expected-dom.txt soit satisfait (même comparaison que le verdict). Délai :
`adapters.playwright.settle_timeout_ms`, 5000 par défaut, 0 = lecture unique ; un délai
écoulé n'est pas une erreur, le verdict tranche (un attendu absent coûte ce délai).

Adaptateur cli (retro-skill-8v9, `live_instance.kind: cli`) : chaque scénario
porte `input-command.json` — {"argv": [...], "stdin": str|null,
"env": {...}, "cwd": str|null} — exécuté par
`subprocess.run(argv, ..., check=False)`, jamais un shell. Rend {"stdout",
"stderr", "exit_code"}. `adapters.cli` de replay-config.json ne fournit qu'un
`timeout` (défaut 30 s), un `cwd` et des variables d'environnement communes —
le scénario garde la main sur l'argv complet et l'environnement propre à son cas.

Cohérence du mode de rejeu (retro-skill-343r) : `replay_input_problem` vérifie
que le fichier d'entrée du `replay_adapter` existe avant les préconditions et
le journal. Un mode incohérent (fichier absent) produit `status: error`, jamais
`skipped`. Les modes qui ne rejouent rien (mq, sql, batch — `NOT_REPLAYED_ADAPTERS`)
rendent leur `skipped` AVANT l'écriture du journal : pas de ligne de journal
pour une écriture qui n'a pas eu lieu.

Modification des données du legacy (retro-skill-ca4, DEC-043, retro-skill-n86) :
chaque scenario.json porte `writes` (bool, obligatoire) — true si le rejeu
crée, modifie ou supprime une donnée. Avec `--target legacy` (rejeu baseline) :
  - `writes` absent ou non booléen → status "error", rien n'est envoyé ;
  - `writes: true` sans `--mutation-log` → status "skipped_writes_not_allowed",
    rien n'est envoyé ; ce n'est pas un échec (exit inchangé), compté à part ;
  - `--mutation-log {project-root}/retro/runtime/mutations.jsonl` : la politique
    est relue via `mutation-policy.py show` sur le project_config.yaml voisin
    (`../project_config.yaml`), jamais recopiée (retro-skill-sas.8) ; le rejeu
    d'un scénario `writes: true` n'est autorisé que si `policy: allowed` — le
    pipeline ne travaille jamais sur la production, quelle que soit `policy`
    (mutation-policy.py show rejette déjà `environment: production`, cette
    relecture est une garde de second niveau ; generate-config.py le rejette
    en amont). Si autorisé, une ligne est ajoutée au journal AVANT chaque
    rejeu `writes: true` via `mutation-policy.py log` (seul écrivain de
    runtime/mutations.jsonl) ; sinon, ces scénarios restent
    "skipped_writes_not_allowed".
  `--target new-app` : aucun contrôle, la nouvelle application est une cible de test.

Ordre de rejeu (retro-skill-h7ez) : sans --only, tous les scénarios qui ne déclarent pas
writes: true sont rejoués d'abord, puis les writes: true, chacun dans l'ordre des chemins ;
la liste scenarios des résultats suit cet ordre. Avec --only, l'ordre des --only.

Préconditions (retro-skill-ztt7) : `scenario.json:preconditions`, optionnel, liste
de {"check": requête, "expect_status": 200, "on_miss": requête}. Avant le rejeu,
`check` est envoyé (base_url et auth de l'adapter http) ; statut différent →
`on_miss` (recréation par l'application) puis nouveau `check`. Donnée absente sans
`on_miss`, recréation refusée ou échouée → status "skipped" avec `replay_note`.
Sur le legacy, `on_miss` est une écriture : tentée seulement si la politique
l'autorise, et journalisée avant via `mutation-policy.py log`.

Sortie stdout : avec --output, résumé court (compteurs, échecs bornés à 15, 8 premières
lignes de diff, texte tronqué strictement à 10 000 caractères avec la mention
« tronqué ») — le fichier complet reste dans --output ; sans --output, résultat complet.
--summarize FICHIER : résumé (même borne stricte) d'un fichier de résultats existant, sans rejeu.
--summarize FICHIER --scenario-id ID : entrée complète d'un scénario.
Exit 0 = aucun scénario failed ni error, 1 = au moins un failed ou error, 2 = erreur critique
"""

import argparse
import base64
import json
import os
import re
import subprocess
import sys
from html.parser import HTMLParser
from pathlib import Path

LEGACY_TARGET = "legacy"
SKIPPED_WRITES = "skipped_writes_not_allowed"

# Correspondance expected_outputs → fichier expected-* (k9w.1c).
# Partagée avec check-step-output.py via --expected-outputs.
# file_output est un répertoire (not a file) — signalé dans directory_outputs.
# Dans replay_scenario, ext_map = EXPECTED_OUTPUT_FILES ; la valeur None de
# file_output est gérée par la logique de comparaison (aucune auto-comparaison).
EXPECTED_OUTPUT_FILES = {
    "http_response": "expected-response.json",
    "db_state": "expected-db.json",
    "file_output": "expected-files",
    "dom_text": "expected-dom.txt",
    "mq_message": "expected-message.json",
    "stdout": "expected-stdout.txt",
    "exit_code": "expected-exit-code.json",
}
# Types dont la comparaison automatique est supportée (tous sauf file_output).
_AUTO_COMPARED = ["http_response", "db_state", "dom_text", "mq_message", "stdout", "exit_code"]
# file_output est un dossier, pas un fichier.
_DIRECTORY_OUTPUTS = ["file_output"]

# retro-skill-96j : aucun agent ne charge plus retro/.env — REPLAY_TOKEN et
# connection_env sont résolus ici, dans l'environnement puis dans
# {--project-root}/retro/.env, jamais imprimés (les valeurs résolues sont
# masquées dans le résumé — voir _redact_secrets).
_PROJECT_ROOT: str | None = None
_DOTENV_CACHE: dict[str, str] | None = None
_RESOLVED_SECRETS: set[str] = set()


def browser_install_command() -> str:
    """Commande d'installation de Chromium pour la version de Playwright de cet
    environnement (sys.executable = interpréteur de l'env uv du script).
    Dupliqué de retro-features/login-state.py (DEC-029)."""
    return f'"{sys.executable}" -m playwright install chromium'


def read_dotenv(path: Path) -> dict[str, str]:
    """Dupliqué de retro-setup/check-env.py (DEC-029, pas d'import entre
    skills). Lit un fichier dotenv NOM=valeur (retro/.env). Ignore les lignes
    vides et celles qui commencent par #. NOM est débarrassé de ses espaces ;
    la valeur est prise telle quelle après le premier '=', sans guillemets et
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


def resolve_secret(name: str) -> str:
    """Résout `name` dans l'environnement, puis dans
    `{_PROJECT_ROOT}/retro/.env` s'il est défini. Toute valeur non vide
    résolue est retenue pour être masquée dans le résumé."""
    global _DOTENV_CACHE
    value = os.environ.get(name)
    if not value and _PROJECT_ROOT:
        if _DOTENV_CACHE is None:
            _DOTENV_CACHE = read_dotenv(Path(_PROJECT_ROOT) / "retro" / ".env")
        value = _DOTENV_CACHE.get(name)
    value = value or ""
    if value:
        _RESOLVED_SECRETS.add(value)
    return value


# ---------------------------------------------------------------------------
# Auth HTTP Basic auto-dérivée (retro-skill-56t)
# ---------------------------------------------------------------------------

def _http_accounts(cfg: dict) -> list[dict]:
    """Retourne la liste des comptes HTTP depuis adapters.http.accounts ou
    live_instance.accounts. Utilisé pour l'auth Basic auto-dérivée."""
    http_cfg = cfg.get("adapters", {}).get("http", {})
    accounts = http_cfg.get("accounts") or []
    if not accounts:
        accounts = cfg.get("live_instance", {}).get("accounts") or []
    return accounts


def basic_auth_value(login: str, password: str) -> str:
    """Encode login:password en Base64 pour un en-tête Authorization: Basic."""
    return base64.b64encode(f"{login}:{password}".encode()).decode()


def account_for_role(role: str, accounts: list[dict]) -> dict | None:
    """Retourne le compte pour le rôle donné, ou None si absent.
    Exposé publiquement (sans préfixe _) pour être réutilisé par d'autres
    scripts (ex. retro-skill-w7w)."""
    return next((a for a in accounts if a.get("role") == role), None)


def resolve_http_auth(scenario: dict, cfg: dict) -> dict | None:
    """Résout l'en-tête Authorization pour un scénario HTTP.

    Retourne un dict {"Header": "valeur"} ou None.

    Priorités :
    1. Si `adapters.http.auth_scheme == "basic"` et `scenario.roles` non vide :
       Basic auto-dérivée depuis les comptes de la config.
       Aucun rôle → None (valide les scénarios 401 attendus).
    2. Sinon, rétro-compat : `adapters.http.auth_header` (jeton REPLAY_TOKEN
       partagé entre tous les scénarios).

    Lève ValueError si le rôle est déclaré mais introuvable dans les comptes.
    """
    http_cfg = cfg.get("adapters", {}).get("http", {})
    auth_scheme = http_cfg.get("auth_scheme", "").lower()
    roles = scenario.get("roles") or []

    if auth_scheme == "basic":
        if not roles:
            # Pas de rôle → pas d'en-tête (test de 401, accès public)
            return None
        role = roles[0]
        accounts = _http_accounts(cfg)
        account = account_for_role(role, accounts)
        if account is None:
            raise ValueError(
                f"Compte introuvable pour le rôle {role!r} dans la config "
                f"(adapters.http.accounts ou live_instance.accounts)"
            )
        login = account["login"]
        password = resolve_secret(account["password_env"])
        return {"Authorization": f"Basic {basic_auth_value(login, password)}"}

    # Rétro-compat : auth_header dans la config (jeton REPLAY_TOKEN partagé)
    auth_header = http_cfg.get("auth_header", "")
    if auth_header:
        key, _, val = auth_header.partition(": ")
        val = val.replace("{token}", resolve_secret("REPLAY_TOKEN"))
        return {key: val}

    return None


def validate_config(config: dict) -> list[str]:
    """Valide la config replay (replay-config.json). Retourne la liste des erreurs.

    Contrôles :
    - Si `adapters.http.auth_scheme == "basic"` : vérifier que les comptes existent
      et ont chacun `role`, `login` et `password_env`.
    """
    errors: list[str] = []
    http_cfg = config.get("adapters", {}).get("http", {})
    auth_scheme = http_cfg.get("auth_scheme", "").lower()
    if auth_scheme == "basic":
        accounts = _http_accounts(config)
        if not accounts:
            errors.append(
                "auth_scheme: basic — aucun compte déclaré dans "
                "adapters.http.accounts ni live_instance.accounts"
            )
        for acc in accounts:
            role = acc.get("role") or ""
            if not role:
                errors.append("auth_scheme: basic — compte sans role")
            if not acc.get("login"):
                errors.append(f"auth_scheme: basic — compte {role or '?'!r} sans login")
            if not acc.get("password_env"):
                errors.append(f"auth_scheme: basic — compte {role or '?'!r} sans password_env")
    return errors


# ---------------------------------------------------------------------------
# Adaptateurs de replay
# ---------------------------------------------------------------------------

def prepare_request_kwargs(req_data: dict) -> dict:
    """Détermine les paramètres à passer à requests.request() pour envoyer le
    corps de input-request.json conformément au Content-Type déclaré.

    Retourne soit {"json"|"data": ..., "allow_redirects": bool}, soit
    {"error": "<motif lisible>"} si le corps ne peut pas être encodé
    fidèlement (jamais d'envoi approximatif).
    """
    headers = req_data.get("headers", {}) or {}
    content_type = ""
    for key, value in headers.items():
        if key.lower() == "content-type":
            content_type = str(value).split(";", 1)[0].strip().lower()
            break

    body = req_data.get("body")

    if body is None:
        kwargs: dict = {}
    elif not content_type or content_type == "application/json" or content_type.endswith("+json"):
        kwargs = {"json": body}
    elif content_type == "application/x-www-form-urlencoded":
        if isinstance(body, str):
            kwargs = {"data": body.encode("utf-8")}
        elif isinstance(body, dict):
            pairs = []
            for field_name, field_value in body.items():
                values = field_value if isinstance(field_value, list) else [field_value]
                for v in values:
                    if isinstance(v, bool) or v is None or isinstance(v, (dict, list)):
                        return {
                            "error": (
                                f"Corps form-urlencoded : valeur non représentable pour "
                                f"'{field_name}' ({type(v).__name__})"
                            )
                        }
                    pairs.append((field_name, str(v)))
            kwargs = {"data": pairs}
        else:
            return {"error": f"Corps structuré incompatible avec Content-Type '{content_type}'"}
    elif content_type == "multipart/form-data":
        return {"error": "Content-Type multipart/form-data non supporté par le replay http"}
    else:
        if isinstance(body, str):
            kwargs = {"data": body.encode("utf-8")}
        else:
            return {"error": f"Corps structuré incompatible avec Content-Type '{content_type}'"}

    kwargs["allow_redirects"] = bool(req_data.get("follow_redirects", True))
    return kwargs


class _HiddenInputs(HTMLParser):
    def __init__(self):
        super().__init__()
        self.fields: dict[str, str] = {}

    def handle_starttag(self, tag, attrs):
        a = dict(attrs)
        if (tag == "input" and (a.get("type") or "").lower() == "hidden"
                and a.get("name") and a["name"] not in self.fields):
            self.fields[a["name"]] = a.get("value") or ""


def hidden_form_fields(html: str) -> dict[str, str]:
    """Champs <input type="hidden"> d'une page (premier de chaque nom) :
    jeton JSF javax.faces.ViewState, _csrf Spring, etc."""
    parser = _HiddenInputs()
    parser.feed(html or "")
    return parser.fields


def replay_http(scenario_dir: Path, adapter_config: dict, scenario: dict,
               full_config: dict | None = None) -> dict:
    """Envoie input-request.json à la cible HTTP.

    full_config (retro-skill-56t) : contenu complet de replay-config.json.
    Quand présent, `resolve_http_auth` résout l'en-tête Authorization depuis
    `adapters.http.auth_scheme` / `auth_header`. Quand absent, construit un
    full_config minimal depuis adapter_config pour garantir la rétro-compat.
    """
    try:
        import requests as req_lib
    except ImportError:
        return {"error": "requests non installé"}

    req_file = scenario_dir / "input-request.json"
    if not req_file.is_file():
        return {"error": "input-request.json manquant"}

    req_data = json.loads(req_file.read_text(encoding="utf-8"))
    base_url = adapter_config.get("base_url", "").rstrip("/")
    url = base_url + req_data.get("path", "/")
    method = req_data.get("method", "GET").upper()
    headers = dict(req_data.get("headers", {}) or {})

    # Auth : Basic auto-dérivée (auth_scheme == "basic") ou rétro-compat auth_header.
    effective_cfg = full_config if full_config is not None else {"adapters": {"http": adapter_config}}
    try:
        resolved_auth = resolve_http_auth(scenario, effective_cfg)
        if resolved_auth:
            headers.update(resolved_auth)
    except ValueError as e:
        return {"error": str(e)}

    kwargs = prepare_request_kwargs(req_data)
    if "error" in kwargs:
        return {"error": kwargs["error"]}

    client = req_lib
    tokens_from = req_data.get("form_tokens_from")
    if tokens_from:
        if not isinstance(kwargs.get("data"), list):
            return {"error": "form_tokens_from : corps form-urlencoded (objet) requis"}
        client = req_lib.Session()
        try:
            page = client.get(base_url + tokens_from, headers=resolved_auth or {}, timeout=30)
        except (req_lib.RequestException, OSError, ValueError, TypeError) as e:
            return {"error": f"form_tokens_from : page {tokens_from} injoignable : {e}"}
        fresh = hidden_form_fields(page.text)
        if not fresh:
            return {"error": f"form_tokens_from : aucun champ caché dans {tokens_from} "
                             f"(statut {page.status_code})"}
        sent = {name for name, _ in kwargs["data"]}
        kwargs["data"] = ([(k, fresh.get(k, v)) for k, v in kwargs["data"]]
                          + [(k, v) for k, v in fresh.items() if k not in sent])

    try:
        resp = client.request(method, url, headers=headers, timeout=30, **kwargs)
        return {
            "status_code": resp.status_code,
            "body": _try_json(resp.text),
            "headers": dict(resp.headers),
        }
    except (req_lib.RequestException, OSError, ValueError, TypeError) as e:
        return {"error": str(e)}


# Limite de capture du texte visible (partagée par l'adaptateur et la comparaison).
DOM_TEXT_MAX_CHARS = 5000

# JavaScript extrayant les attributs value des input[type=submit] et input[type=button].
# Ces éléments portent leur libellé dans `value`, invisible pour inner_text().
_INPUT_VALUES_JS = (
    'Array.from(document.querySelectorAll(\'input[type="submit"], input[type="button"]\')'
    ').map(el => el.value).filter(v => v && v.trim()).join("\\n")'
)


def dom_text_with_inputs(page) -> str:
    """Retourne le texte DOM visible enrichi des attributs value des boutons input.

    `page.inner_text("body")` ignore les éléments <input type="submit"> et
    <input type="button"> dont le libellé est dans l'attribut `value`, pas dans
    un nœud texte (retro-skill-zx7q). Cette helper ajoute ces valeurs après le
    texte principal, en filtrant les doublons (cas où un framework aurait rendu
    la valeur dans un nœud texte enfant). Tronqué à DOM_TEXT_MAX_CHARS.
    """
    base_text = page.inner_text("body")
    try:
        input_values_raw: str = page.evaluate(_INPUT_VALUES_JS) or ""
    except Exception:  # noqa: BLE001 — page déjà fermée, navigateur coupé, etc.
        input_values_raw = ""

    if not input_values_raw.strip():
        return base_text[:DOM_TEXT_MAX_CHARS]

    # Construire la liste des valeurs non déjà présentes dans inner_text.
    base_lower = base_text.lower()
    extra_lines = [
        v for v in input_values_raw.splitlines()
        if v.strip() and v.strip().lower() not in base_lower
    ]
    if not extra_lines:
        return base_text[:DOM_TEXT_MAX_CHARS]

    combined = base_text + "\n" + "\n".join(extra_lines)
    return combined[:DOM_TEXT_MAX_CHARS]


def wait_for_dom_text(page, predicate, timeout_ms: int, step_ms: int = 200) -> str:
    """Relit le texte de la page jusqu'à ce que `predicate(texte)` soit vrai ou que le délai
    soit écoulé. Rend le dernier texte lu : un délai écoulé n'est pas une erreur, la
    comparaison qui suit tranche."""
    text = dom_text_with_inputs(page)
    waited = 0
    while not predicate(text) and waited < timeout_ms:
        page.wait_for_timeout(step_ms)
        waited += step_ms
        text = dom_text_with_inputs(page)
    return text


def settle_dom_text(page, scenario_dir: Path, scenario: dict, timeout_ms: int) -> str:
    """Texte final de la page. Interface rendue côté client : le texte attendu peut arriver
    après la fin de l'activité réseau — on relit jusqu'à ce que expected-dom.txt soit
    satisfait (même comparaison que le verdict) ou que le délai soit écoulé."""
    expected = scenario_dir / "expected-dom.txt"
    if timeout_ms <= 0 or not expected.is_file() or "dom_text" not in (scenario.get("expected_outputs") or []):
        return dom_text_with_inputs(page)
    try:
        mode = text_compare_mode(scenario, "dom_text")
    except ValueError:
        return dom_text_with_inputs(page)
    return wait_for_dom_text(page, lambda t: compare_dom_text(expected, t, mode)["match"] is True, timeout_ms)


PASSWORD_SELECTOR_RE = re.compile(
    r"type=['\"]?password|pass(word|wd)?|pwd|mdp|mot[_-]?de[_-]?passe", re.IGNORECASE)


def password_fill_actions(actions: list) -> list[int]:
    """Indices des actions `fill` dont le sélecteur ressemble à un champ mot
    de passe (retro-skill-ejv) — la connexion passe par storage_state
    (login-state.py), jamais par une saisie enregistrée dans
    input-actions.json. Motif dupliqué dans check-step-output.py --skill
    retro-golden-master (DEC-029, script d'un autre skill)."""
    return [i for i, action in enumerate(actions)
            if isinstance(action, dict) and action.get("type") == "fill"
            and PASSWORD_SELECTOR_RE.search(str(action.get("selector", "")))]


# ---------------------------------------------------------------------------
# Sonde de session expirée et re-login borné (retro-skill-w7w, DEC-080)
# ---------------------------------------------------------------------------

def session_looks_expired(page) -> bool:
    """Heuristique : True si Spring Security (ou tout autre mécanisme) a
    redirigé vers la page de login après le chargement de start_url.
    Détecte : URL contient 'login', 'signin' ou 'sign-in' ; ou présence
    d'un champ input[type='password'] visible sur la page."""
    url = page.url
    if "login" in url or "signin" in url or "sign-in" in url:
        return True
    return page.query_selector("input[type='password']") is not None


def _login_state_script() -> Path:
    """Chemin absolu vers login-state.py. Résout depuis l'emplacement de ce fichier (DEC-034) :
    d'abord à côté de lui (export dans le repo cible : outils livrés côte à côte dans
    docs/retro/tools/, retro-skill-k9f5.9), sinon dans retro-features/scripts/ du skill."""
    here = Path(__file__).resolve().parent
    sibling = here / "login-state.py"
    if sibling.is_file():
        return sibling
    return here.parent.parent / "retro-features" / "scripts" / "login-state.py"


def relogin_role(role: str, project_root: str | None, base_url: str,
                 storage_state_dir: str, accounts: list[dict]) -> str:
    """Re-connecte le rôle via login-state.py en sous-processus (DEC-029).
    Retourne '' si OK, sinon le message d'erreur (jamais de valeur de mot
    de passe dans ce message — seul le nom de la variable est visible)."""
    account = account_for_role(role, accounts)
    if account is None:
        return f"compte introuvable pour le rôle {role!r} dans adapters.playwright.accounts"
    script = str(_login_state_script())
    cmd = [
        "uv", "run", "--quiet", script,
        "--project-root", str(project_root or "."),
        "--base-url", base_url,
        "--role", role,
        "--force",
        "--output-dir", storage_state_dir,
        "--accounts", json.dumps([account]),
    ]
    if project_root:
        env_file = Path(project_root) / "retro" / ".env"
        if env_file.is_file():
            cmd += ["--env-file", str(env_file)]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=120, check=False)
    except (OSError, subprocess.SubprocessError) as e:  # noqa: BLE001
        return f"login-state.py non lancé : {e}"
    if result.returncode != 0:
        return f"login-state.py a échoué (exit {result.returncode}) : {result.stderr[:200]}"
    return ""


def replay_playwright(scenario_dir: Path, adapter_config: dict, scenario: dict) -> dict:
    """Rejoue input-actions.json via Playwright Python. La connexion, si le
    scénario en a besoin, passe par la session du rôle (storage_state,
    login-state.py — retro-skill-ejv), jamais par une saisie enregistrée :
    `adapter_config["storage_state_dir"]` donne le dossier des sessions
    (`{storage_state_dir}/{role}.json`, `role` lu dans input-actions.json).
    Sans `storage_state_dir`, comportement inchangé (pages publiques, sans
    connexion). Ces contrôles se font avant tout lancement de navigateur :
    une saisie de mot de passe ou une session absente ne dépendent pas de
    Playwright pour être détectées.

    Si la session est expirée (Spring Security session.invalidate() ou
    équivalent), `session_looks_expired()` le détecte après `page.goto()` et
    un re-login automatique est tenté via `login-state.py` (1 fois max,
    retro-skill-w7w, DEC-080). `adapters.playwright.accounts` doit être
    déclaré dans replay-config.json pour que le re-login fonctionne."""
    actions_file = scenario_dir / "input-actions.json"
    if not actions_file.is_file():
        return {"error": "input-actions.json manquant"}

    actions_data = json.loads(actions_file.read_text(encoding="utf-8"))
    base_url = adapter_config.get("base_url", "").rstrip("/")
    raw_start = actions_data.get("start_url", "/")
    start_url = raw_start if raw_start.startswith("http") else base_url + raw_start
    actions = actions_data.get("actions", [])

    bad_password_actions = password_fill_actions(actions)
    if bad_password_actions:
        return {"error": (
            f"input-actions.json contient une saisie de mot de passe (action {bad_password_actions[0]}) : "
            "la connexion passe par storage_state ; "
            "si le scénario porte sur la connexion elle-même (identifiants refusés), "
            "le capturer en http (golden-master.md, "
            "« Scénario qui passe par la saisie d'un mot de passe »)"
        )}

    storage_state_dir = adapter_config.get("storage_state_dir")
    storage_state_path = None
    role = None
    if storage_state_dir:
        role = actions_data.get("role")
        if not role:
            return {"error": "input-actions.json : role manquant (obligatoire avec storage_state_dir)"}
        storage_state_path = Path(storage_state_dir) / f"{role}.json"
        if not storage_state_path.is_file():
            return {"error": f"session absente pour le rôle {role} — lancer login-state.py"}

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        return {"error": "paquet Python playwright absent de l'environnement du script — relancer par uv run"}

    result: dict = {"actions_executed": 0, "final_url": None, "dom_text": None, "error": None}
    settle_ms = int(adapter_config.get("settle_timeout_ms", 5000))

    try:
        with sync_playwright() as pw:
            browser = pw.chromium.launch(headless=True)
            attempts = 0
            while True:
                ctx = None
                if storage_state_path is not None:
                    ctx = browser.new_context(storage_state=str(storage_state_path))
                    page = ctx.new_page()
                else:
                    page = browser.new_page()
                page.goto(start_url, wait_until="networkidle", timeout=20000)

                # Sonde de session expirée (scénarios authentifiés seulement —
                # retro-skill-w7w) : Spring Security session.invalidate() invalide
                # le JSESSIONID stocké dans storage_state ; les scénarios suivants
                # du même rôle seraient redirigés vers /login.
                if storage_state_path and session_looks_expired(page):
                    if attempts < 1:
                        if ctx:
                            ctx.close()
                        err = relogin_role(
                            role=role,
                            project_root=_PROJECT_ROOT,
                            base_url=base_url,
                            storage_state_dir=str(storage_state_path.parent),
                            accounts=adapter_config.get("accounts", []),
                        )
                        if err:
                            result["error"] = f"re-login échoué : {err}"
                            break
                        attempts += 1
                        continue  # recommencer avec le storage_state frais
                    else:
                        result["error"] = (
                            "session expirée pour le rôle, re-login épuisé (1 tentative)"
                        )
                        result["final_url"] = page.url
                        result["dom_text"] = dom_text_with_inputs(page)
                        if ctx:
                            ctx.close()
                        break

                for action in actions:
                    atype = action.get("type")
                    try:
                        if atype == "fill":
                            page.fill(action["selector"], action.get("value", ""))
                        elif atype == "click":
                            page.click(action["selector"])
                        elif atype == "wait_for":
                            if "selector" in action:
                                page.wait_for_selector(action["selector"], timeout=10000)
                            else:
                                page.wait_for_load_state("networkidle")
                                if action.get("text"):
                                    needle = " ".join(action["text"].split()).lower()
                                    wait_for_dom_text(page, lambda t, needle=needle:
                                                      needle in " ".join(t.split()).lower(), settle_ms)
                        elif atype == "select":
                            page.select_option(action["selector"], action.get("value", ""))
                        result["actions_executed"] += 1
                    except Exception as e:  # noqa: BLE001 — exceptions Playwright non énumérables
                        print(f"{scenario_dir}: action {atype} échouée : {e}", file=sys.stderr)
                        result["error"] = f"Action {atype} échouée : {e}"
                        break

                result["final_url"] = page.url
                result["dom_text"] = settle_dom_text(page, scenario_dir, scenario, settle_ms)
                if ctx:
                    ctx.close()
                break  # sortir normalement de la boucle while
            browser.close()
    except Exception as e:  # noqa: BLE001 — exceptions Playwright non énumérables
        print(f"{scenario_dir}: {e}", file=sys.stderr)
        if "Executable doesn't exist" in str(e) or "playwright install" in str(e):
            result["error"] = (
                "Navigateur Chromium de Playwright non installé. Lancez : "
                + browser_install_command()
            )
        else:
            result["error"] = str(e)

    return result


def replay_sql(scenario_dir: Path, adapter_config: dict, scenario: dict) -> dict:
    """Exécute input-query.sql avec input-params.json."""
    query_file = scenario_dir / "input-query.sql"
    if not query_file.is_file():
        return {"error": "input-query.sql manquant"}

    conn_env = adapter_config.get("connection_env", "REPLAY_DB_URL")
    if not resolve_secret(conn_env):
        return {"error": f"Variable {conn_env} non définie (ni dans l'environnement, ni dans retro/.env)"}

    return {"skipped": True, "reason": "Adaptateur SQL — connecteur à implémenter selon le SGBD cible (psycopg2, cx_Oracle, pyodbc…)"}


def replay_batch(scenario_dir: Path, adapter_config: dict, scenario: dict) -> dict:
    """Lance la commande batch avec les fichiers d'entrée."""
    command = scenario.get("command")
    if not command:
        return {"error": "Champ 'command' manquant dans scenario.json"}
    return {"skipped": True, "reason": f"Adaptateur batch — commande à exécuter : {command}"}


def replay_cli(scenario_dir: Path, adapter_config: dict, scenario: dict) -> dict:
    """Exécute `input-command.json` — {"argv": [...], "stdin": str|null,
    "env": {...}, "cwd": str|null} — contre une application
    `live_instance.kind: cli` (retro-skill-8v9). `argv[0]` est résolu tel quel
    (chemin ou nom sur PATH) : chaque scénario porte sa commande complète,
    capturée telle qu'observée — rien n'est recopié depuis `adapter_config`,
    qui ne fournit qu'un `timeout`, un `cwd` par défaut et des variables
    d'environnement communes, écrasées par celles propres au scénario.
    Rend {"stdout", "stderr", "exit_code"}."""
    cmd_file = scenario_dir / "input-command.json"
    if not cmd_file.is_file():
        return {"error": "input-command.json manquant"}

    cmd_data = json.loads(cmd_file.read_text(encoding="utf-8"))
    argv = cmd_data.get("argv")
    if not isinstance(argv, list) or not argv:
        return {"error": "input-command.json : 'argv' manquant ou vide"}

    cwd = cmd_data.get("cwd") or adapter_config.get("cwd")
    env = dict(os.environ)
    env.update(adapter_config.get("env") or {})
    env.update(cmd_data.get("env") or {})

    timeout = adapter_config.get("timeout", 30)
    try:
        result = subprocess.run(
            argv, cwd=cwd or None, env=env, input=cmd_data.get("stdin"),
            capture_output=True, text=True, timeout=timeout, check=False,
        )
    except subprocess.TimeoutExpired:
        return {"error": f"délai dépassé ({timeout}s)"}
    except OSError as e:  # commande introuvable, non exécutable, cwd absent
        return {"error": f"commande non exécutable : {e}"}

    return {"stdout": result.stdout, "stderr": result.stderr, "exit_code": result.returncode}


ADAPTERS = {
    "http": replay_http,
    "playwright": replay_playwright,
    "sql": replay_sql,
    "batch": replay_batch,
    "cli": replay_cli,
    "mq": lambda *_: {"skipped": True, "reason": "Adaptateur MQ non implémenté — dépend du broker cible"},
}

# Fichier d'entrée de chaque mode de rejeu (retro-skill-343r). Même table que
# INPUT_FILES de complete-gherkin-from-scenarios.py, plus cli ; dupliquée dans
# check-step-output.py (DEC-029, script d'un autre skill).
REPLAY_INPUT_FILES = {
    "http": "input-request.json",
    "playwright": "input-actions.json",
    "cli": "input-command.json",
    "mq": "input-message.json",
    "sql": "input-query.sql",
    "batch": "input-files",
}

# Modes qui ne rejouent rien aujourd'hui (skipped) : jamais de journal de modification.
NOT_REPLAYED_ADAPTERS = {"mq", "sql", "batch"}


def replay_input_problem(scenario_dir: Path, scenario: dict, adapter_key: str) -> str:
    """Vérifie que le fichier d'entrée du mode de rejeu existe, quand il diffère
    du mode de capture.

    Ne vérifie que si `replay_adapter` est explicitement déclaré ET différent de
    `capture_adapter` : dans ce cas, son fichier d'entrée doit aussi avoir été
    capturé. Quand ils sont identiques, l'adapter function vérifie elle-même
    l'existence du fichier.

    Renvoie une chaîne vide si tout est conforme. Sinon, renvoie un message
    d'erreur indiquant le fichier manquant et le remède — notamment la capture
    http pour un scénario qui passe par la saisie d'un mot de passe
    (retro-skill-343r). Les modes inconnus ne produisent pas d'erreur ici :
    c'est ADAPTERS.get() qui retourne None pour eux.
    """
    # Vérification seulement si replay_adapter est différent de capture_adapter.
    capture = scenario.get("capture_adapter", "")
    if adapter_key == capture or scenario.get("replay_adapter") is None:
        return ""
    input_name = REPLAY_INPUT_FILES.get(adapter_key)
    if input_name is None:
        return ""  # adaptateur inconnu : ADAPTERS.get() renverra None plus bas
    input_path = scenario_dir / input_name
    if input_path.exists():
        return ""
    return (
        f"replay_adapter {adapter_key!r} : {input_name} absent — "
        f"mode de rejeu incohérent avec la capture (capture_adapter {capture!r}). "
        f"Ne jamais choisir un mode de rejeu pour faire ignorer un scénario : "
        f"le recapturer (saisie de mot de passe : capture http, "
        f"golden-master.md « Scénario qui passe par la saisie d'un mot de passe »)"
    )


# ---------------------------------------------------------------------------
# Comparaison des sorties
# ---------------------------------------------------------------------------

def _try_json(text: str):
    try:
        return json.loads(text)
    except (ValueError, TypeError):
        return text


COMPARE_MODES = ("exact", "status+json_subset", "status+body_contains")

TEXT_COMPARE_MODES = ("exact", "contains", "contains-ci")


def text_compare_mode(scenario: dict, output_type: str) -> str:
    """Retourne le mode de comparaison pour un type de sortie (dom_text, stdout, etc.).

    Lit scenario.compare.<output_type>.mode ; défaut : "exact".
    Lève ValueError si le mode n'est pas dans TEXT_COMPARE_MODES.
    """
    mode = scenario.get("compare", {}).get(output_type, {}).get("mode", "exact")
    if mode not in TEXT_COMPARE_MODES:
        raise ValueError(
            f"Mode de comparaison inconnu: {mode!r} "
            f"(attendu: {', '.join(TEXT_COMPARE_MODES)})"
        )
    return mode


def _strip_fields(obj, fields):
    if isinstance(obj, dict):
        return {k: _strip_fields(v, fields) for k, v in obj.items() if k not in fields}
    if isinstance(obj, list):
        return [_strip_fields(i, fields) for i in obj]
    return obj


def json_subset_mismatches(expected, actual, path: str = "body") -> list[str]:
    """Liste les chemins où `expected` n'est pas un sous-ensemble de `actual`.

    dict : chaque clé attendue doit exister et être elle-même un sous-ensemble ;
    list : même longueur, comparaison élément par élément (par index) ;
    scalaire : égalité stricte (1 != True, "1" != 1).
    """
    if isinstance(expected, dict):
        if not isinstance(actual, dict):
            return [f"{path} : objet attendu, reçu {type(actual).__name__}"]
        out: list[str] = []
        for key, exp_val in expected.items():
            if key not in actual:
                out.append(f"{path}.{key} : clé absente")
            else:
                out.extend(json_subset_mismatches(exp_val, actual[key], f"{path}.{key}"))
        return out
    if isinstance(expected, list):
        if not isinstance(actual, list):
            return [f"{path} : liste attendue, reçu {type(actual).__name__}"]
        if len(expected) != len(actual):
            return [f"{path} : longueur attendue {len(expected)}, reçue {len(actual)}"]
        out = []
        for i, (e, a) in enumerate(zip(expected, actual)):
            out.extend(json_subset_mismatches(e, a, f"{path}[{i}]"))
        return out
    if type(expected) is not type(actual) or expected != actual:
        return [f"{path} : attendu {json.dumps(expected, ensure_ascii=False)}, "
                f"reçu {json.dumps(actual, ensure_ascii=False)[:200]}"]
    return []


def compare_output(expected_path: Path, actual: dict, output_type: str, comparison_config: dict,
                   mode: str = "exact") -> dict:
    """Compare une sortie réelle avec l'expected. Retourne {match, diff}.

    mode : "exact" (défaut, égalité stricte) ou "status+json_subset".
    """
    if not expected_path.is_file():
        return {"match": None, "reason": f"{expected_path.name} manquant — expected non capturé"}

    if mode not in COMPARE_MODES:
        return {"match": False, "diff": f"Mode de comparaison inconnu : {mode!r} (attendu : {', '.join(COMPARE_MODES)})"}

    expected = _try_json(expected_path.read_text(encoding="utf-8"))
    ignore_fields = comparison_config.get(output_type, {}).get("ignore_fields", [])

    exp_clean = _strip_fields(expected, ignore_fields)
    act_clean = _strip_fields(actual, ignore_fields)

    if mode == "status+json_subset":
        if not isinstance(exp_clean, dict) or "status_code" not in exp_clean:
            return {"match": False, "diff": "status+json_subset : expected sans status_code"}
        problems = []
        if exp_clean["status_code"] != act_clean.get("status_code"):
            problems.append(f"status_code : attendu {exp_clean['status_code']}, reçu {act_clean.get('status_code')}")
        if exp_clean.get("body") is not None:
            problems.extend(json_subset_mismatches(exp_clean["body"], act_clean.get("body")))
        if not problems:
            return {"match": True, "mode": mode}
        return {"match": False, "mode": mode, "diff": "\n".join(problems[:80])}

    if mode == "status+body_contains":
        fragments = exp_clean.get("body_contains") if isinstance(exp_clean, dict) else None
        if ("status_code" not in (exp_clean if isinstance(exp_clean, dict) else {})
                or not isinstance(fragments, list)
                or not all(isinstance(f, str) and f for f in fragments)):
            return {"match": False, "diff": "status+body_contains : expected sans status_code "
                                            "ou body_contains (liste de chaînes non vides)"}
        body = act_clean.get("body")
        text = body if isinstance(body, str) else json.dumps(body, ensure_ascii=False)
        problems = []
        if exp_clean["status_code"] != act_clean.get("status_code"):
            problems.append(f"status_code : attendu {exp_clean['status_code']}, reçu {act_clean.get('status_code')}")
        problems += [f"body : fragment absent : {f!r}" for f in fragments if f not in text]
        if not problems:
            return {"match": True, "mode": mode}
        return {"match": False, "mode": mode, "diff": "\n".join(problems)}

    exp_str = json.dumps(exp_clean, ensure_ascii=False, sort_keys=True, indent=2)
    act_str = json.dumps(act_clean, ensure_ascii=False, sort_keys=True, indent=2)

    if exp_str == act_str:
        return {"match": True}

    from difflib import unified_diff
    diff = list(unified_diff(exp_str.splitlines(), act_str.splitlines(),
                             fromfile="expected", tofile="actual", lineterm=""))
    return {"match": False, "diff": "\n".join(diff[:80])}


_MISSING = object()


def extract_output(actual: dict, output_type: str):
    """Extrait de la réponse de l'adaptateur la valeur à comparer à l'expected.

    Retourne _MISSING si l'adaptateur n'a pas produit la sortie attendue.
    """
    if output_type == "dom_text":
        value = actual.get("dom_text")
        return _MISSING if value is None else value
    if output_type == "db_state":
        return actual.get("db_state", actual)
    if output_type == "mq_message":
        return actual.get("message", actual)
    if output_type == "stdout":
        value = actual.get("stdout")
        return _MISSING if value is None else value
    if output_type == "exit_code":
        value = actual.get("exit_code")
        return _MISSING if value is None else value
    return actual


def normalize_dom_text(text: str) -> list[str]:
    """Tronque à DOM_TEXT_MAX_CHARS puis normalise : blancs consécutifs réduits
    à un espace, espaces de bord retirés, lignes vides supprimées."""
    lines = (" ".join(line.split()) for line in text[:DOM_TEXT_MAX_CHARS].splitlines())
    return [line for line in lines if line]


def compare_dom_text(expected_path: Path, actual_text: str, mode: str = "exact") -> dict:
    """Compare expected-dom.txt (texte brut) au dom_text de l'adaptateur.

    mode "exact" (défaut) : les lignes normalisées des deux côtés doivent être
    identiques dans le même ordre.
    mode "contains" : chaque ligne non vide de l'expected doit être une
    sous-chaîne d'au moins une ligne de l'actual (ordre non contraint). Adapté
    aux scénarios d'écriture (writes: true) dont le tableau de page s'allonge
    à chaque rejeu — on ne vérifie que les fragments produits par l'action,
    pas le snapshot complet de la page (retro-skill-x5v).
    mode "contains-ci" : identique à "contains" mais insensible à la casse —
    les deux côtés sont mis en minuscules avant comparaison. Utilisé quand la
    page applique text-transform CSS (ex. uppercase) : inner_text() retourne
    le texte DOM sans les transformations CSS (retro-skill-5zyk).
    """
    if not expected_path.is_file():
        return {"match": None, "reason": f"{expected_path.name} manquant — expected non capturé"}

    exp_lines = normalize_dom_text(expected_path.read_text(encoding="utf-8"))
    act_lines = normalize_dom_text(str(actual_text))

    if mode == "contains":
        missing = [
            exp_line for exp_line in exp_lines
            if not any(exp_line in act_line for act_line in act_lines)
        ]
        if not missing:
            return {"match": True, "mode": "contains"}
        return {
            "match": False,
            "mode": "contains",
            "diff": "Lignes attendues non trouvées dans le texte réel :\n"
                    + "\n".join(f"  - {line}" for line in missing),
        }

    if mode == "contains-ci":
        act_lines_lower = [act_line.lower() for act_line in act_lines]
        missing = [
            exp_line for exp_line in exp_lines
            if not any(exp_line.lower() in act_line_lower for act_line_lower in act_lines_lower)
        ]
        if not missing:
            return {"match": True, "mode": "contains-ci"}
        return {
            "match": False,
            "mode": "contains-ci",
            "diff": "Lignes attendues non trouvées dans le texte réel (insensible à la casse) :\n"
                    + "\n".join(f"  - {line}" for line in missing),
        }

    if exp_lines == act_lines:
        return {"match": True}

    from difflib import unified_diff
    diff = list(unified_diff(exp_lines, act_lines, fromfile="expected", tofile="actual", lineterm=""))
    return {"match": False, "diff": "\n".join(diff[:80])}


# ---------------------------------------------------------------------------
# Résumé stdout — borné pour ne jamais saturer le contexte du LLM
# ---------------------------------------------------------------------------

SUMMARY_MAX_ITEMS = 15       # entrées max par liste (failures, skipped_ids, unchecked)
SUMMARY_DIFF_HEAD_LINES = 8  # premières lignes de diff conservées par sortie en échec
SUMMARY_LINE_MAX_CHARS = 160  # longueur max d'une ligne de diff ou d'une raison
SUMMARY_MAX_CHARS = 10_000    # borne stricte de la sortie --summarize (retro-skill-sas.7) :
                              # les bornes ci-dessus limitent le contenu construit, pas le texte
                              # imprimé — pire cas théorique (SUMMARY_MAX_ITEMS échecs de
                              # SUMMARY_DIFF_HEAD_LINES lignes de SUMMARY_LINE_MAX_CHARS
                              # caractères) au-delà de 10 000 caractères sans cette troncature.


def _redact_secrets(text: str) -> str:
    """Masque, dans le résumé, toute valeur résolue par `resolve_secret`
    (REPLAY_TOKEN, connection_env) — jamais un jeton ou un mot de passe dans
    diff_head ou reason (retro-skill-96j)."""
    for secret in _RESOLVED_SECRETS:
        text = text.replace(secret, "***")
    return text


def _clip(text, limit: int = SUMMARY_LINE_MAX_CHARS) -> str:
    text = "" if text is None else str(text)
    text = _redact_secrets(text)
    return text if len(text) <= limit else text[: limit - 1] + "…"


def _bound_summary_text(summary: dict) -> str:
    """Sérialise `summary` et tronque strictement le texte à SUMMARY_MAX_CHARS
    caractères, avec la mention « tronqué » — jamais un texte plus long, quel
    que soit le nombre d'échecs ou la taille des diffs qu'il contient."""
    text = json.dumps(summary, ensure_ascii=False, indent=2)
    if len(text) <= SUMMARY_MAX_CHARS:
        return text
    marker = "\n… [tronqué : sortie au-delà de {} caractères]".format(SUMMARY_MAX_CHARS)
    return text[: SUMMARY_MAX_CHARS - len(marker)] + marker


def summarize_results(results: dict, output_path: str | None = None) -> dict:
    """Résumé court d'un résultat de rejeu (dict produit par main()).

    Les compteurs sont recalculés depuis results["scenarios"] (source de vérité),
    pas relus dans les champs de tête. Lève ValueError si results n'est pas un
    dict portant une liste "scenarios".
    """
    if not isinstance(results, dict) or not isinstance(results.get("scenarios"), list):
        raise ValueError("résultats de rejeu invalides : attendu un objet avec une liste 'scenarios'")

    scenarios = results["scenarios"]
    total = len(scenarios)
    passed = sum(1 for s in scenarios if isinstance(s, dict) and s.get("status") == "passed")
    failed = sum(1 for s in scenarios if isinstance(s, dict) and s.get("status") == "failed")
    skipped = sum(1 for s in scenarios if isinstance(s, dict) and s.get("status") == "skipped")
    errors = sum(1 for s in scenarios if isinstance(s, dict) and s.get("status") == "error")
    writes_not_allowed = [s.get("id") for s in scenarios
                          if isinstance(s, dict) and s.get("status") == SKIPPED_WRITES]

    failures = []
    failures_truncated = 0
    skipped_ids = []
    unchecked = []

    for s in scenarios:
        if not isinstance(s, dict):
            continue
        status = s.get("status")

        if status in ("failed", "error"):
            if len(failures) >= SUMMARY_MAX_ITEMS:
                failures_truncated += 1
                continue
            entry = {
                "id": s.get("id"),
                "feature_id": s.get("feature_id"),
                "status": status,
                "adapter": s.get("adapter"),
            }
            if status == "failed":
                mismatches = []
                comparisons = s.get("comparisons") or {}
                if isinstance(comparisons, dict):
                    for output_name, cmp in comparisons.items():
                        if not isinstance(cmp, dict) or cmp.get("match") is not False:
                            continue
                        diff_text = cmp.get("diff") or ""
                        diff_lines_all = diff_text.splitlines()
                        mismatches.append({
                            "output": output_name,
                            "diff_lines": len(diff_lines_all),
                            "diff_head": [_clip(line) for line in diff_lines_all[:SUMMARY_DIFF_HEAD_LINES]],
                        })
                entry["mismatches"] = mismatches
            else:
                entry["reason"] = _clip(s.get("reason"))
            failures.append(entry)

        elif status == "skipped":
            if len(skipped_ids) < SUMMARY_MAX_ITEMS:
                skipped_ids.append(s.get("id"))

        comparisons = s.get("comparisons") or {}
        if isinstance(comparisons, dict):
            for output_name, cmp in comparisons.items():
                if isinstance(cmp, dict) and cmp.get("match") is None and len(unchecked) < SUMMARY_MAX_ITEMS:
                    unchecked.append({"id": s.get("id"), "output": output_name})

    summary = {
        "target": results.get("target"),
        "output": output_path,
        "total": total,
        "passed": passed,
        "failed": failed,
        "skipped": skipped,
        "errors": errors,
        "failures": failures,
        "failures_truncated": failures_truncated,
        "skipped_ids": skipped_ids,
        "unchecked": unchecked,
        SKIPPED_WRITES: len(writes_not_allowed),
        "writes_not_allowed_ids": writes_not_allowed[:SUMMARY_MAX_ITEMS],
    }
    if failures:
        ref = output_path or "<results.json>"
        summary["hint"] = f"Détail d'un scénario : replay-golden-master.py --summarize {ref} --scenario-id <id>"

    return summary


def results_exit_code(results: dict) -> int:
    """0 si aucun scénario failed ni error, 1 sinon (compté depuis results["scenarios"])."""
    scenarios = results.get("scenarios") if isinstance(results, dict) else None
    if not isinstance(scenarios, list):
        return 0
    for s in scenarios:
        if isinstance(s, dict) and s.get("status") in ("failed", "error"):
            return 1
    return 0


# ---------------------------------------------------------------------------
# Boucle principale
# ---------------------------------------------------------------------------

def _mutation_policy_script() -> Path:
    return Path(__file__).resolve().parent.parent.parent / "retro-setup" / "scripts" / "mutation-policy.py"


def read_mutation_policy(config_yaml: Path) -> dict:
    """policy et environment de `live_instance.mutations`, via
    `mutation-policy.py show` — jamais recopié (retro-skill-sas.8). {} si le
    script échoue (config absente, ancien format, ou environment production :
    la production n'est jamais rejouée)."""
    try:
        result = subprocess.run(
            ["uv", "run", "--quiet", str(_mutation_policy_script()), "show",
             "--config", str(config_yaml)],
            capture_output=True, text=True, timeout=60, check=False,
        )
    except (OSError, subprocess.SubprocessError):
        return {}
    if result.returncode != 0 or not result.stdout.strip():
        return {}
    try:
        data = json.loads(result.stdout)
    except (ValueError, TypeError):
        return {}
    return {"policy": data.get("policy"), "environment": data.get("environment")}


def _journal(guard: dict, scenario: dict, scenario_id: str, action: str,
             method: str, target: str) -> str:
    args = ["uv", "run", "--quiet", str(_mutation_policy_script()), "log",
            "--config", str(guard["config"]), "--skill", "retro-golden-master",
            "--action", action, "--target", target or scenario_id,
            "--method", method, "--not-marked"]
    if scenario.get("feature_id"):
        args += ["--feature", str(scenario["feature_id"])]
    if scenario_id:
        args += ["--scenario", str(scenario_id)]
    try:
        result = subprocess.run(args, capture_output=True, text=True, timeout=30, check=False)
    except (OSError, subprocess.SubprocessError) as e:
        return str(e)
    return "" if result.returncode == 0 else (result.stderr.strip() or f"exit {result.returncode}")


def journal_write(guard: dict, scenario: dict, scenario_id: str, adapter: str) -> str:
    """Journalise AVANT le rejeu, via `mutation-policy.py log` — un seul
    écrivain de runtime/mutations.jsonl (retro-skill-sas.8). Rend "" si la
    ligne est écrite, sinon le message d'erreur du script : le scénario ne
    doit alors pas être rejoué."""
    method, target = "UI", scenario.get("feature_id") or ""
    if adapter == "http":
        req_path = guard["scenario_dir"] / "input-request.json"
        try:
            req = json.loads(req_path.read_text(encoding="utf-8"))
            method = str(req.get("method", "GET")).upper()
            target = req.get("path") or target
        except (OSError, ValueError, AttributeError):
            pass
    elif adapter == "cli":
        cmd_path = guard["scenario_dir"] / "input-command.json"
        try:
            cmd_data = json.loads(cmd_path.read_text(encoding="utf-8"))
            method = "CLI"
            target = " ".join(cmd_data.get("argv") or []) or target
        except (OSError, ValueError, AttributeError):
            pass
    return _journal(guard, scenario, scenario_id, "replay baseline", method, target)


def journal_precondition_write(guard: dict, scenario: dict, scenario_id: str, on_miss: dict) -> str:
    """Journalise AVANT la recréation d'une donnée manquante sur le legacy
    (même règle que journal_write : "" si écrit, sinon le message d'erreur)."""
    method = str(on_miss.get("method", "POST")).upper()
    return _journal(guard, scenario, scenario_id, "recréation précondition", method,
                    on_miss.get("path") or "")


def _precondition_request(spec: dict, base_url: str, auth: dict | None, default_method: str) -> dict:
    try:
        import requests as req_lib
    except ImportError:
        return {"error": "requests non installé"}
    headers = dict(spec.get("headers", {}) or {})
    if auth:
        headers.update(auth)
    kwargs = prepare_request_kwargs(spec)
    if "error" in kwargs:
        return {"error": kwargs["error"]}
    method = str(spec.get("method", default_method)).upper()
    try:
        resp = req_lib.request(method, base_url + spec.get("path", "/"), headers=headers,
                               timeout=30, **kwargs)
    except (req_lib.RequestException, OSError, ValueError, TypeError) as e:
        return {"error": str(e)}
    return {"status_code": resp.status_code}


def check_preconditions(scenario: dict, config: dict, guard: dict | None) -> str | None:
    """Vérifie les données dont le scénario a besoin et tente de les recréer
    via l'application si elles manquent. Rend None si tout est en place
    (ou aucune précondition), sinon la raison du skip (replay_note)."""
    preconditions = scenario.get("preconditions")
    if not preconditions:
        return None
    if not isinstance(preconditions, list):
        return "preconditions doit être une liste"
    base_url = config.get("adapters", {}).get("http", {}).get("base_url", "").rstrip("/")
    if not base_url:
        return "préconditions : adapters.http.base_url manquant dans la config de rejeu"
    try:
        auth = resolve_http_auth(scenario, config)
    except ValueError as e:
        return f"préconditions : {e}"
    scenario_id = scenario.get("id", "")

    for pre in preconditions:
        check = pre.get("check") or {}
        expect = pre.get("expect_status", 200)
        path = check.get("path", "/")
        r = _precondition_request(check, base_url, auth, "GET")
        if "error" in r:
            return f"précondition : vérification de {path} impossible : {r['error']}"
        if r["status_code"] == expect:
            continue
        on_miss = pre.get("on_miss")
        if not on_miss:
            return (f"données manquantes ({path} → {r['status_code']}, attendu {expect}) "
                    "et aucune recréation déclarée")
        if guard is not None:
            if not guard.get("allowed"):
                return (f"données manquantes ({path}), recréation refusée : modification "
                        "des données du legacy non autorisée")
            jerr = journal_precondition_write(guard, scenario, scenario_id, on_miss)
            if jerr:
                return f"données manquantes ({path}), recréation non journalisée donc non tentée : {jerr}"
        rec = _precondition_request(on_miss, base_url, auth, "POST")
        if "error" in rec or not 200 <= rec["status_code"] < 400:
            return (f"données manquantes ({path}), recréation échouée via l'appli : "
                    f"{rec.get('error') or rec.get('status_code')}")
        r2 = _precondition_request(check, base_url, auth, "GET")
        if r2.get("status_code") != expect:
            return f"données manquantes ({path}), recréation envoyée mais donnée toujours absente"
    return None


def scenario_writes(scenario_dir: Path) -> bool:
    """True seulement si scenario.json porte writes: true (booléen).
    Fichier absent, illisible, writes absent ou non booléen → False :
    le scénario reste dans le groupe des lectures, run_scenario le traitera
    (error) comme aujourd'hui (retro-skill-h7ez)."""
    scenario_file = scenario_dir / "scenario.json"
    try:
        data = json.loads(scenario_file.read_text(encoding="utf-8"))
        return isinstance(data, dict) and data.get("writes") is True
    except (OSError, ValueError):
        return False


def run_scenario(scenario_dir: Path, config: dict, target: str, guard: dict | None = None) -> dict:
    """guard (rejeu baseline sur le legacy uniquement) : {"allowed": bool,
    "log": Path | None, "environment": str | None} — voir la docstring du module."""
    scenario_file = scenario_dir / "scenario.json"
    if not scenario_file.is_file():
        return {"id": scenario_dir.name, "status": "error", "reason": "scenario.json manquant"}

    scenario = json.loads(scenario_file.read_text(encoding="utf-8"))
    scenario_id = scenario.get("id", scenario_dir.name)

    if guard is not None:
        writes = scenario.get("writes")
        if not isinstance(writes, bool):
            return {"id": scenario_id, "feature_id": scenario.get("feature_id"), "status": "error",
                    "reason": "writes absent de scenario.json — champ obligatoire (true si le "
                              "scénario crée, modifie ou supprime une donnée)"}

    replay_adapter_key = scenario.get("replay_adapter") or scenario.get("capture_adapter", "http")
    adapter_fn = ADAPTERS.get(replay_adapter_key)
    if not adapter_fn:
        return {"id": scenario_id, "status": "error", "reason": f"Adaptateur inconnu : {replay_adapter_key}"}

    adapter_config = config.get("adapters", {}).get(replay_adapter_key, {})
    comparison_config = config.get("comparison", {})

    # Vérifier que le fichier d'entrée du mode de rejeu existe (retro-skill-343r).
    # Un mode incohérent est une erreur qui prend la priorité sur skipped_writes_not_allowed :
    # c'est un problème de capture à corriger, pas un scénario à ignorer légitimement.
    problem = replay_input_problem(scenario_dir, scenario, replay_adapter_key)
    if problem:
        return {"id": scenario_id, "feature_id": scenario.get("feature_id"),
                "status": "error", "reason": problem}

    # Les modes non rejoués (mq, sql, batch) rendent skipped AVANT le journal :
    # pas de ligne de journal pour une écriture qui n'a pas eu lieu.
    # Ce bloc est aussi placé avant la garde writes pour éviter toute ligne de journal parasite.
    if replay_adapter_key in NOT_REPLAYED_ADAPTERS:
        actual = adapter_fn(scenario_dir, adapter_config, scenario)
        return {"id": scenario_id, "feature_id": scenario.get("feature_id"),
                "status": "skipped", "reason": actual.get("reason")}

    if guard is not None and scenario.get("writes") and not guard.get("allowed"):
        return {"id": scenario_id, "feature_id": scenario.get("feature_id"),
                "status": SKIPPED_WRITES,
                "reason": "modification des données du legacy non autorisée — scénario non rejoué"}

    pre_note = check_preconditions(scenario, config, guard)
    if pre_note:
        return {"id": scenario_id, "feature_id": scenario.get("feature_id"), "status": "skipped",
                "reason": "précondition non satisfaite", "replay_note": pre_note}

    if guard is not None and scenario.get("writes"):
        journal_error = journal_write({**guard, "scenario_dir": scenario_dir}, scenario,
                                      scenario_id, replay_adapter_key)
        if journal_error:
            return {"id": scenario_id, "feature_id": scenario.get("feature_id"), "status": "error",
                    "reason": f"journal des modifications non écrit, scénario non rejoué : {journal_error}"}

    # Pour l'adaptateur http, transmettre le config complet afin que
    # resolve_http_auth puisse dériver l'auth Basic depuis les comptes.
    if replay_adapter_key == "http":
        actual = replay_http(scenario_dir, adapter_config, scenario, full_config=config)
    else:
        actual = adapter_fn(scenario_dir, adapter_config, scenario)

    if actual.get("skipped"):
        return {"id": scenario_id, "status": "skipped", "reason": actual.get("reason")}
    if actual.get("error"):
        return {"id": scenario_id, "status": "error", "reason": actual["error"]}

    ext_map = EXPECTED_OUTPUT_FILES

    http_mode = (
        (scenario.get("compare") or {}).get("mode")
        or comparison_config.get("http_response", {}).get("mode")
        or "exact"
    )

    comparisons = {}
    all_match = True
    for output_type in scenario.get("expected_outputs", []):
        filename = ext_map.get(output_type)
        if not filename or output_type in _DIRECTORY_OUTPUTS:
            comparisons[output_type] = {"match": None, "reason": "Comparaison fichier non supportée automatiquement"}
            continue
        expected_path = scenario_dir / filename
        value = extract_output(actual, output_type)
        if value is _MISSING and expected_path.is_file():
            cmp = {"match": False, "diff": f"L'adaptateur n'a pas produit {output_type}"}
        elif output_type in ("dom_text", "stdout"):
            # Mêmes normalisations que dom_text (blancs consécutifs réduits,
            # lignes vides supprimées, diff unifié) appliquées à stdout — un
            # exécutable rejoué localement ou en CI peut faire varier des
            # espaces de fin de ligne sans que ce soit un vrai écart.
            # Le mode (exact|contains) est lu dans scenario.compare.<type>.mode
            # (retro-skill-x5v).
            try:
                txt_mode = text_compare_mode(scenario, output_type)
            except ValueError as exc:
                comparisons[output_type] = {"match": False, "diff": str(exc)}
                all_match = False
                continue
            cmp = compare_dom_text(expected_path, "" if value is _MISSING else value, txt_mode)
        else:
            mode = http_mode if output_type == "http_response" else "exact"
            cmp = compare_output(expected_path, value, output_type, comparison_config, mode)
        comparisons[output_type] = cmp
        if cmp.get("match") is False:
            all_match = False

    return {
        "id": scenario_id,
        "feature_id": scenario.get("feature_id"),
        "status": "passed" if all_match else "failed",
        "adapter": replay_adapter_key,
        "comparisons": comparisons,
    }


def _fail(message: str, **extra):
    print(json.dumps({"error": message, **extra}, ensure_ascii=False), file=sys.stderr)
    sys.exit(2)


def main():
    parser = argparse.ArgumentParser(
        description="Rejoue les scénarios du golden master contre une cible.",
        epilog=__doc__,
    )
    parser.add_argument("--scenarios", help="Répertoire retro/golden-master/scenarios/")
    parser.add_argument("--config", help="Chemin vers replay-config.json")
    parser.add_argument("--target", default="new-app", help="Identifiant de la cible (pour les logs)")
    parser.add_argument("--output", help="Fichier de résultats JSON (stdout si absent)")
    parser.add_argument("--mutation-log",
                        help="Journal retro/runtime/mutations.jsonl : autorise le rejeu baseline des "
                             "scénarios writes: true si live_instance.mutations.policy = allowed")
    parser.add_argument("--expected-outputs", action="store_true",
                        help="Imprime la table EXPECTED_OUTPUT_FILES et exit 0, sans réseau")
    parser.add_argument("--summarize", help="Relit un fichier de résultats et imprime son résumé, sans rejouer")
    parser.add_argument("--scenario-id", help="Avec --summarize : imprime l'entrée complète de ce scénario")
    parser.add_argument("--project-root", default=None,
                         help="racine du projet legacy — pour résoudre REPLAY_TOKEN et "
                              "connection_env dans {project-root}/retro/.env quand "
                              "l'environnement ne les porte pas")
    parser.add_argument("--only", action="append", metavar="SCENARIO_ID",
                        help="Rejouer seulement ce scénario (répétable). "
                             "Incompatible avec --output : résultats affichés sur stdout uniquement, "
                             "jamais écrits dans results-baseline.json / results-new-app.json. "
                             "L'ordre des scénarios rejoués suit l'ordre des --only.")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()

    global _PROJECT_ROOT
    _PROJECT_ROOT = args.project_root

    if args.expected_outputs:
        print(json.dumps({
            "expected_output_files": EXPECTED_OUTPUT_FILES,
            "directory_outputs": _DIRECTORY_OUTPUTS,
            "auto_compared": _AUTO_COMPARED,
        }, ensure_ascii=False, indent=2))
        sys.exit(0)

    resolve_secret("REPLAY_TOKEN")  # résolu tôt pour être masqué même en --summarize

    if args.summarize:
        if args.scenarios or args.config or args.output:
            _fail("--summarize est exclusif du rejeu")

        results_path = Path(args.summarize)
        if not results_path.is_file():
            _fail(f"Fichier introuvable : {args.summarize}")

        try:
            data = json.loads(results_path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            _fail(f"JSON invalide dans {args.summarize} : {e}")

        if args.scenario_id:
            scenarios = data.get("scenarios") if isinstance(data, dict) else None
            if not isinstance(scenarios, list):
                _fail("résultats de rejeu invalides : attendu un objet avec une liste 'scenarios'")
            match = next((s for s in scenarios if isinstance(s, dict) and s.get("id") == args.scenario_id), None)
            if match is None:
                known_ids = [s.get("id") for s in scenarios if isinstance(s, dict)][:SUMMARY_MAX_ITEMS]
                _fail(f"scénario inconnu : {args.scenario_id}", known_ids=known_ids)
            print(json.dumps(match, ensure_ascii=False, indent=2))
            sys.exit(0)

        try:
            summary = summarize_results(data, args.summarize)
        except ValueError as e:
            _fail(str(e))

        print(_bound_summary_text(summary))
        sys.exit(results_exit_code(data))

    if args.scenario_id:
        _fail("--scenario-id exige --summarize")

    only_ids: list[str] = args.only or []
    if only_ids and args.output:
        _fail("--only est incompatible avec --output (rejeu de vérification sans écriture de résultats)")

    if not args.scenarios or not args.config:
        _fail("--scenarios et --config sont requis pour rejouer")

    scenarios_dir = Path(args.scenarios)
    if not scenarios_dir.is_dir():
        print(json.dumps({"error": f"Répertoire introuvable : {args.scenarios}"}), file=sys.stderr)
        sys.exit(2)

    config_path = Path(args.config)
    if not config_path.is_file():
        print(json.dumps({"error": f"Config introuvable : {args.config}"}), file=sys.stderr)
        sys.exit(2)

    config = json.loads(config_path.read_text(encoding="utf-8"))

    # Validation de la config (auth_scheme: basic → comptes requis)
    config_errors = validate_config(config)
    if config_errors:
        _fail("replay-config.json invalide", errors=config_errors)

    guard = None
    if args.target == LEGACY_TARGET:
        guard = {"allowed": False, "log": None, "environment": None, "config": None}
        if args.mutation_log:
            log = Path(args.mutation_log).resolve()
            config_yaml = log.parent.parent / "project_config.yaml"
            if not config_yaml.is_file():
                _fail(f"project_config.yaml introuvable à côté du journal : {config_yaml}")
            policy = read_mutation_policy(config_yaml)
            # Seule policy == allowed reste (retro-skill-sas.8) : mutation-
            # policy.py show rejette déjà environment == production (la
            # production n'est jamais rejouée), pas de branche dédiée ici.
            if policy.get("policy") == "allowed":
                guard = {"allowed": True, "log": log, "environment": policy.get("environment"),
                         "config": config_yaml}
            else:
                print(json.dumps({"warning": "modification des données non autorisée (policy ≠ allowed, "
                                             "config invalide, ou production) — scénarios writes: true "
                                             "non rejoués"}, ensure_ascii=False), file=sys.stderr)
    elif args.mutation_log:
        _fail("--mutation-log ne s'applique qu'au rejeu baseline (--target legacy)")

    # Construction de la liste des répertoires de scénarios à rejouer
    all_scenario_files = sorted(scenarios_dir.rglob("scenario.json"))
    if only_ids:
        # Index par id pour respecter l'ordre des --only
        by_id: dict[str, Path] = {}
        for sf in all_scenario_files:
            try:
                sc_data = json.loads(sf.read_text(encoding="utf-8"))
                sid = sc_data.get("id")
                if sid:
                    by_id[sid] = sf.parent
            except (OSError, ValueError):
                pass
        unknown = [sid for sid in only_ids if sid not in by_id]
        if unknown:
            _fail(f"--only : scénario(s) inconnu(s) : {', '.join(unknown)}")
        scenario_dirs_to_run = [by_id[sid] for sid in only_ids]
    else:
        scenario_dirs_to_run = [sf.parent for sf in all_scenario_files]
        # Lectures avant écritures (retro-skill-h7ez) : tri stable, l'ordre
        # des chemins est gardé dans chaque groupe.
        scenario_dirs_to_run.sort(key=scenario_writes)

    results = []
    for scenario_dir_path in scenario_dirs_to_run:
        result = run_scenario(scenario_dir_path, config, args.target, guard)
        results.append(result)
        if args.verbose:
            print(f"[{result['status'].upper()}] {result['id']}", file=sys.stderr)

    total = len(results)
    passed = sum(1 for r in results if r["status"] == "passed")
    failed = sum(1 for r in results if r["status"] == "failed")
    skipped = sum(1 for r in results if r["status"] == "skipped")
    errors = sum(1 for r in results if r["status"] == "error")
    writes_not_allowed = sum(1 for r in results if r["status"] == SKIPPED_WRITES)

    output = {
        "target": args.target,
        "total": total,
        "passed": passed,
        "failed": failed,
        "skipped": skipped,
        "errors": errors,
        SKIPPED_WRITES: writes_not_allowed,
        "scenarios": results,
    }

    if only_ids:
        # --only : résultats stdout uniquement, jamais écrits dans results-*.json
        print(json.dumps(output, ensure_ascii=False, indent=2))
    elif args.output:
        output_str = json.dumps(output, ensure_ascii=False, indent=2)
        Path(args.output).write_text(output_str, encoding="utf-8")
        print(_bound_summary_text(summarize_results(output, args.output)))
    else:
        print(json.dumps(output, ensure_ascii=False, indent=2))

    sys.exit(results_exit_code(output))


if __name__ == "__main__":
    main()
