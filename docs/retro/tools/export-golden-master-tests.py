#!/usr/bin/env python3
# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///
"""
Génère les tests d'un runner du marché depuis le golden master dérivé, pour le rejouer
sur la nouvelle application et obtenir le rapport d'exécution du runner.

Le golden master dérivé (derive-golden-master.py derive) ne dépend d'aucun runner. Ce script
le lit vers un modèle neutre (fonctionnalités, rôles, scénarios, actions, attendus, scénarios
non rejoués), puis le rend pour le runner choisi. Un nouveau runner ajoute un rendu, sans
toucher à la lecture. Il ne décide rien : toute décision est dans replay-mapping.json.

  export-golden-master-tests.py --runner playwright --derived DIR --mapping replay-mapping.json
                                [--gm-dir DIR] --output DIR [--check]

Lit   : DIR/replay-config.json, DIR/<FEAT>/<SC>/{scenario.json, input-actions.json |
        input-request.json, expected-dom.txt | expected-response.json} ;
        --mapping : scénarios supprimés (decision drop) et leur raison ;
        --gm-dir (scénarios capturés, optionnel) : description des scénarios supprimés.
Écrit (runner playwright, Playwright Test) : <output>/playwright.config.ts,
        <output>/auth.setup.ts (si un rôle est utilisé), <output>/specs/<FEAT>.spec.ts,
        <output>/.gitignore (sessions, rapport, résultats). Fichiers marqués « ne pas éditer » ;
        une spec générée qui ne correspond plus à aucune fonctionnalité est supprimée.
        Génération déterministe (aucune date) : mêmes entrées, mêmes fichiers.
--check : régénère en mémoire et compare au disque, sans rien écrire ; code 1 si un fichier
        généré manque, a été modifié à la main ou est périmé.

Correspondances (Playwright Test) :
  fonctionnalité -> specs/<FEAT>.spec.ts, un test.describe ; rôle -> test.describe avec
  test.use({ storageState }) (sans rôle : état vide) ; scénario -> test('<SC> — <description>')
  annoté des règles couvertes et de la page d'origine ; fill / click / select -> locator ;
  wait_for sélecteur -> toBeVisible ; wait_for texte -> toContainText (casse ignorée) ;
  dom_text contains / contains-ci -> un expect.soft(...).toContainText par ligne attendue ;
  dom_text exact -> expect.poll sur le texte affiché normalisé ; toute comparaison de texte
  porte useInnerText (texte affiché, comme le rejeu de référence) ;
  requête HTTP -> request.fetch (form_tokens_from ignoré : sans objet sur la cible) ;
  status+json_subset -> status + toMatchObject ; status+body_contains -> status + toContain ;
  exact -> status + corps (JSON sans les ignore_fields de comparison.http_response, ou texte) ;
  scénario supprimé -> test.skip annoté « Non rejoué » avec sa raison.
  Écritures après lectures dans chaque rôle, un seul worker. Connexion (auth.setup.ts) : pour
  chaque rôle utilisé, route protégée, formulaire (premier champ identifiant, champ mot de
  passe), mot de passe lu dans la variable d'environnement du compte, état enregistré dans
  <output>/.auth/<rôle>.json. Lancer le runner depuis <output> : `npx playwright test`
  (@playwright/test installé dans le dépôt cible).
URL : routes et chemins relatifs à l'URL de base terminée par « / » (préfixe /app/… conservé),
  GM_BASE_URL la surcharge ; appels HTTP vers adapters.http.base_url (GM_API_BASE_URL ; même
  base que le navigateur : GM_BASE_URL aussi). Authentification HTTP reprise de la configuration
  dérivée : auth_header ({token} <- REPLAY_TOKEN) ou Basic par rôle (comptes adapters.http).
  Locators : premier élément (.first()), comme le rejeu Python. form_tokens_from : champs cachés
  lus sur la page cible, dans la même session, puis fusionnés au corps.
Limites : en-têtes HTTP non comparés en mode exact ; libellés des boutons <input> non ajoutés
  au texte comparé (le rejeu Python les ajoute) ; session conservée en cookie ou stockage
  local seulement (pas en stockage de session).

Sortie stdout : JSON {runner, output, specs, tests, skipped, roles, files, check?, changed?}
Exit 0 = OK ; 1 = aucun scénario dérivé, ou --check en écart ; 2 = entrée illisible,
runner inconnu, action inconnue. Diagnostics sur stderr.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

HEADER = ("// Généré par export-golden-master-tests.py depuis le golden master dérivé — ne pas éditer :\n"
          "// modifier replay-mapping.json, relancer la dérivation puis cette génération.\n")
ACTION_TYPES = ("fill", "click", "select", "wait_for")
STATE_DIR = ".auth"


class UsageError(Exception):
    """Erreur bloquante -> exit 2."""


# ---------------------------------------------------------------------------
# Lecture : golden master dérivé -> modèle neutre
# ---------------------------------------------------------------------------

def _load_json(path: Path, required: bool = False):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        if required:
            raise UsageError(f"fichier introuvable : {path}") from None
        return None
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        raise UsageError(f"JSON illisible : {path} ({exc})") from exc


def _expected_lines(path: Path) -> list[str]:
    """Lignes non vides de expected-dom.txt, blancs normalisés comme le rejeu de référence."""
    if not path.is_file():
        return []
    return [line for line in (" ".join(raw.split()) for raw in path.read_text(encoding="utf-8").splitlines())
            if line]


def _strip_fields(obj, fields):
    if isinstance(obj, dict):
        return {k: _strip_fields(v, fields) for k, v in obj.items() if k not in fields}
    if isinstance(obj, list):
        return [_strip_fields(i, fields) for i in obj]
    return obj


def _title(scenario: dict) -> str:
    return f"{scenario.get('id')} — {scenario.get('description') or ''}".strip(" —")


def _screen(sdir: Path, scenario: dict) -> dict:
    data = _load_json(sdir / "input-actions.json", required=True)
    steps = []
    for action in data.get("actions") or []:
        kind = action.get("type")
        if kind not in ACTION_TYPES:
            raise UsageError(f"{sdir / 'input-actions.json'} : type d'action inconnu {kind!r} "
                             f"(attendu : {', '.join(ACTION_TYPES)})")
        steps.append({k: action[k] for k in ("type", "selector", "value", "text") if action.get(k) is not None})
    mode = ((scenario.get("compare") or {}).get("dom_text") or {}).get("mode") or "exact"
    return {"start_url": data.get("start_url") or "/", "role": data.get("role") or "", "steps": steps,
            "mode": mode, "lines": _expected_lines(sdir / "expected-dom.txt")}


def _http(sdir: Path, scenario: dict, ignore_fields: list[str]) -> dict:
    req = _load_json(sdir / "input-request.json", required=True)
    exp = _load_json(sdir / "expected-response.json", required=True)
    headers = req.get("headers") or {}
    ctype = next((str(v).lower() for k, v in headers.items() if k.lower() == "content-type"), "")
    mode = (scenario.get("compare") or {}).get("mode") or "exact"
    body = exp.get("body")
    if mode == "exact":
        body = _strip_fields(body, ignore_fields)
    return {
        "method": (req.get("method") or "GET").upper(),
        "path": req.get("path") or "/",
        "headers": {k: v for k, v in sorted(headers.items()) if k.lower() != "content-type"},
        "body": req.get("body"),
        "body_kind": "form" if "x-www-form-urlencoded" in ctype else "data",
        "follow_redirects": req.get("follow_redirects", True),
        "form_tokens_from": req.get("form_tokens_from") if "x-www-form-urlencoded" in ctype else None,
        "role": (scenario.get("roles") or [""])[0] or "",
        "mode": mode,
        "status": exp.get("status_code", exp.get("status")),
        "expected_body": body,
        "fragments": exp.get("body_contains") or [],
    }


def _origin(scenario: dict) -> str:
    legacy = (scenario.get("derived_from") or {}).get("legacy") or {}
    if "start_page" in legacy:
        return f"page {legacy['start_page']}"
    if legacy.get("path"):
        return f"{legacy.get('method') or 'GET'} {legacy['path']}"
    return "—"


def _dropped(mapping: dict, gm_dir: Path | None) -> list[dict]:
    """Scénarios supprimés du mapping : {id, feature_id, feature_name, title, reason}."""
    captured: dict[str, dict] = {}
    if gm_dir is not None and (gm_dir / "scenarios").is_dir():
        for sfile in sorted((gm_dir / "scenarios").rglob("scenario.json")):
            data = _load_json(sfile) or {}
            captured[data.get("id", sfile.parent.name)] = data
    out = []
    for entry in mapping.get("scenarios") or []:
        if not isinstance(entry, dict) or entry.get("decision") != "drop":
            continue
        legacy = captured.get(entry.get("id")) or {"id": entry.get("id"), "feature_id": entry.get("feature_id")}
        out.append({"id": entry.get("id"), "feature_id": legacy.get("feature_id") or "Non rattaché",
                    "feature_name": legacy.get("feature_name") or "", "title": _title(legacy),
                    "reason": entry.get("rationale") or ""})
    return out


def read_model(derived: Path, mapping: dict, gm_dir: Path | None) -> dict:
    """Golden master dérivé -> modèle neutre, indépendant du runner."""
    config = _load_json(derived / "replay-config.json", required=True)
    adapters = config.get("adapters") or {}
    browser = adapters.get("playwright") or {}
    ignore_fields = ((config.get("comparison") or {}).get("http_response") or {}).get("ignore_fields") or []
    accounts = {a["role"]: a for a in browser.get("accounts") or [] if isinstance(a, dict) and a.get("role")}
    http_cfg = adapters.get("http") or {}
    if str(http_cfg.get("auth_scheme") or "").lower() == "basic":
        http_auth = {"kind": "basic", "accounts": {
            a["role"]: {"login": a.get("login") or "", "password_env": a.get("password_env") or ""}
            for a in http_cfg.get("accounts") or [] if isinstance(a, dict) and a.get("role")}}
    elif http_cfg.get("auth_header"):
        name, _, value = str(http_cfg["auth_header"]).partition(": ")
        http_auth = {"kind": "header", "name": name, "value": value}
    else:
        http_auth = None

    features: dict[str, dict] = {}
    login_routes: dict[str, str] = {}   # rôle -> première route protégée (formulaire de connexion)
    first_screen = ""                   # repli pour un rôle qui n'a que des scénarios HTTP
    for sfile in sorted(derived.rglob("scenario.json")):
        scenario = _load_json(sfile)
        sdir = sfile.parent
        is_screen = scenario.get("replay_adapter") == "playwright"
        body = _screen(sdir, scenario) if is_screen else _http(sdir, scenario, ignore_fields)
        # Un rôle sans compte navigateur ne peut pas ouvrir de session : scénario rejoué sans connexion.
        role = body["role"] if body["role"] in accounts else ""
        if role and is_screen:
            login_routes.setdefault(role, body["start_url"])
        if is_screen:
            first_screen = first_screen or body["start_url"]
        else:
            # Basic par rôle, comme le rejeu Python : le rôle du scénario désigne le compte HTTP.
            body["basic_role"] = (body["role"] if http_auth and http_auth["kind"] == "basic"
                                  and body["role"] in http_auth["accounts"] else "")
        feat = features.setdefault(scenario.get("feature_id") or "Non rattaché", {
            "id": scenario.get("feature_id") or "Non rattaché", "name": scenario.get("feature_name") or "",
            "scenarios": [], "skipped": []})
        feat["scenarios"].append({
            "id": scenario.get("id", sdir.name), "title": _title(scenario), "kind": "screen" if is_screen else "http",
            "role": role, "writes": bool(scenario.get("writes")), "rules": list(scenario.get("rg_ids") or []),
            "origin": _origin(scenario), "feature_id": feat["id"], "detail": body,
            "http_auth": not is_screen and http_auth is not None,
        })
    for drop in _dropped(mapping, gm_dir):
        feat = features.setdefault(drop["feature_id"], {"id": drop["feature_id"], "name": drop["feature_name"],
                                                        "scenarios": [], "skipped": []})
        feat["skipped"].append(drop)

    roles = sorted({s["role"] for f in features.values() for s in f["scenarios"] if s["role"]})
    return {
        "base_url": browser.get("base_url") or config.get("target_base_url") or "",
        "http_base_url": http_cfg.get("base_url") or config.get("target_base_url") or "",
        "http_auth": http_auth,
        "accounts": [{"role": r, "login": accounts[r].get("login") or "",
                      "password_env": accounts[r].get("password_env") or "",
                      "start": relative(login_routes.get(r) or first_screen or "/")} for r in roles],
        "features": [features[k] for k in sorted(features)],
    }


# ---------------------------------------------------------------------------
# Rendu : Playwright Test
# ---------------------------------------------------------------------------

def js(value) -> str:
    """Littéral JavaScript d'une valeur JSON."""
    return json.dumps(value, ensure_ascii=False)


def relative(path: str) -> str:
    """Chemin relatif à l'URL de base (qui se termine par « / ») : Playwright résout comme le
    navigateur, un chemin commençant par « / » perdrait le préfixe de la base (/app/…)."""
    return path.lstrip("/") or "./"


def _details(s: dict) -> str:
    tags = [f"@{s['feature_id']}", "@golden-master"] + (["@ecriture"] if s["writes"] else [])
    annotations = []
    if s["rules"]:
        annotations.append({"type": "Règles couvertes", "description": ", ".join(s["rules"])})
    annotations.append({"type": "Origine legacy", "description": s["origin"]})
    return f"{{ tag: {js(tags)}, annotation: {js(annotations)} }}"


def _pw_screen(s: dict, ind: str) -> list[str]:
    d = s["detail"]
    out = [f"{ind}test({js(s['title'])}, {_details(s)}, async ({{ page }}) => {{",
           f"{ind}  await test.step('Ouvrir la page', async () => {{",
           f"{ind}    await page.goto({js(relative(d['start_url']))});",
           f"{ind}  }});"]
    steps = []
    for a in d["steps"]:
        sel = a.get("selector")
        # .first() : comme le rejeu Python, le premier élément quand le sélecteur en désigne plusieurs.
        if a["type"] == "fill":
            steps.append(f"await page.locator({js(sel)}).first().fill({js(a.get('value', ''))});")
        elif a["type"] == "click":
            steps.append(f"await page.locator({js(sel)}).first().click();")
        elif a["type"] == "select":
            steps.append(f"await page.locator({js(sel)}).first().selectOption({js(a.get('value', ''))});")
        elif sel:
            steps.append(f"await expect(page.locator({js(sel)}).first()).toBeVisible();")
        elif a.get("text"):
            steps.append(f"await expect(page.locator('body')).toContainText({js(a['text'])}, "
                         "{ ignoreCase: true, useInnerText: true });")
        else:
            steps.append("await page.waitForLoadState('networkidle');")
    if steps:
        out.append(f"{ind}  await test.step('Rejouer les actions enregistrées', async () => {{")
        out += [f"{ind}    {line}" for line in steps]
        out.append(f"{ind}  }});")
    out.append(f"{ind}  await test.step('Comparer au texte attendu (legacy)', async () => {{")
    if d["mode"] in ("contains", "contains-ci"):
        opt = "{ ignoreCase: true, useInnerText: true }" if d["mode"] == "contains-ci" else "{ useInnerText: true }"
        out += [f"{ind}    await expect.soft(page.locator('body')).toContainText({js(line)}, {opt});"
                for line in d["lines"]]
    else:
        out.append(f"{ind}    await expect.poll(async () => normalize(await page.locator('body').innerText()))"
                   f".toEqual({js(d['lines'])});")
    out += [f"{ind}  }});", f"{ind}}});"]
    return out


def _pw_http(s: dict, ind: str) -> list[str]:
    d = s["detail"]
    options = [f"method: {js(d['method'])}"]
    auth = f"...authHeaders({js(d.get('basic_role') or '')}), " if s.get("http_auth") else ""
    if d["headers"] or auth:
        options.append(f"headers: {{ {auth}...{js(d['headers'])} }}" if auth else f"headers: {js(d['headers'])}")
    if d["body"] is not None and d.get("form_tokens_from") and isinstance(d["body"], dict):
        # Jetons de formulaire lus sur la page cible dans la même session (form_tokens_from).
        options.append(f"form: {{ ...{js(d['body'])}, ...await hiddenFields(request, "
                       f"{js(relative(d['form_tokens_from']))}) }}")
    elif d["body"] is not None:
        options.append(f"{d['body_kind']}: {js(d['body'])}")
    if d["follow_redirects"] is False:
        options.append("maxRedirects: 0")
    out = [f"{ind}test({js(s['title'])}, {_details(s)}, async ({{ request }}) => {{",
           f"{ind}  const response = await request.fetch(api({js(relative(d['path']))}), {{ {', '.join(options)} }});",
           f"{ind}  expect(response.status()).toBe({js(d['status'])});"]
    if d["mode"] == "status+json_subset" and d["expected_body"] is not None:
        out.append(f"{ind}  expect(await response.json()).toMatchObject({js(d['expected_body'])});")
    elif d["mode"] == "status+body_contains":
        out += [f"{ind}  expect(await response.text()).toContain({js(f)});" for f in d["fragments"]]
    elif d["mode"] == "exact" and isinstance(d["expected_body"], (dict, list)):
        out.append(f"{ind}  expect(stripFields(await response.json())).toEqual({js(d['expected_body'])});")
    elif d["mode"] == "exact" and d["expected_body"] is not None:
        out.append(f"{ind}  expect(await response.text()).toBe({js(d['expected_body'])});")
    out.append(f"{ind}}});")
    return out


HIDDEN_FIELDS = r"""// Champs cachés d'une page cible (jetons de formulaire), lus dans la même session (form_tokens_from).
const hiddenFields = async (request: any, path: string): Promise<Record<string, string>> => {
  const html = await (await request.get(api(path))).text();
  const attr = (tag: string, name: string) => {
    const m = tag.match(new RegExp(`\b${name}\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))`, 'i'));
    return m ? (m[1] ?? m[2] ?? m[3]).replace(/&quot;/g, '"').replace(/&#39;/g, "'").replace(/&lt;/g, '<')
      .replace(/&gt;/g, '>').replace(/&amp;/g, '&') : undefined;
  };
  const out: Record<string, string> = {};
  for (const tag of html.match(/<input[^>]*>/gi) ?? []) {
    const name = attr(tag, 'name');
    if ((attr(tag, 'type') ?? '').toLowerCase() === 'hidden' && name && !(name in out)) out[name] = attr(tag, 'value') ?? '';
  }
  return out;
};"""


def _pw_http_helpers(model: dict, feat: dict) -> list[str]:
    """Base des appels HTTP, authentification et jetons de formulaire, si la spec en a besoin."""
    http = [s for s in feat["scenarios"] if s["kind"] == "http"]
    if not http:
        return []
    same = model["http_base_url"].rstrip("/") == model["base_url"].rstrip("/")
    env = "process.env.GM_API_BASE_URL || process.env.GM_BASE_URL" if same else "process.env.GM_API_BASE_URL"
    out = ["const withSlash = (u: string) => (u.endsWith('/') ? u : u + '/');",
           f"const apiBase = withSlash({env} || {js(model['http_base_url'])});",
           "const api = (path: string) => new URL(path, apiBase).toString();"]
    auth = model.get("http_auth")
    if auth and auth["kind"] == "header":
        out.append(f"const authHeaders = (_role: string): Record<string, string> => ({{ {js(auth['name'])}: "
                   f"{js(auth['value'])}.replace('{{token}}', process.env.REPLAY_TOKEN ?? '') }});")
    elif auth and auth["kind"] == "basic":
        out += [f"const basicAccounts: Record<string, {{ login: string; password_env: string }}> = "
                f"{js(auth['accounts'])};",
                "const authHeaders = (role: string): Record<string, string> => {",
                "  const a = basicAccounts[role];",
                "  return a ? { Authorization: 'Basic ' + Buffer.from(`${a.login}:${process.env[a.password_env] ?? ''}`)"
                ".toString('base64') } : {};",
                "};"]
    if any(s["detail"].get("form_tokens_from") for s in http):
        out.append(HIDDEN_FIELDS)
    return out


def _pw_spec(feat: dict, ignore_fields: list[str], model: dict | None = None) -> str:
    model = model or {"base_url": "", "http_base_url": "", "http_auth": None}
    title = f"{feat['id']} — {feat['name']}" if feat["name"] else feat["id"]
    out = [HEADER + "import { test, expect } from '@playwright/test';", "",
           "// Texte affiché normalisé comme le rejeu de référence : tronqué à 5000 caractères, blancs",
           "// réduits, lignes vides retirées.",
           "const normalize = (text: string) =>",
           "  text.slice(0, 5000).split('\\n').map((l) => l.split(/\\s+/).filter(Boolean).join(' ')).filter(Boolean);"]
    out += _pw_http_helpers(model, feat)
    if any(s["kind"] == "http" and s["detail"]["mode"] == "exact" for s in feat["scenarios"]):
        out += [f"const ignoreFields = new Set<string>({js(sorted(ignore_fields))});",
                "const stripFields = (value: any): any => Array.isArray(value) ? value.map(stripFields)",
                "  : value && typeof value === 'object'",
                "    ? Object.fromEntries(Object.entries(value).filter(([k]) => !ignoreFields.has(k))",
                "        .map(([k, v]) => [k, stripFields(v)]))",
                "    : value;"]
    out += ["", f"test.describe({js(title)}, () => {{"]
    by_role: dict[str, list[dict]] = {}
    for s in feat["scenarios"]:
        by_role.setdefault(s["role"], []).append(s)
    blocks = []
    for role in sorted(by_role):
        label = f"rôle {role}" if role else "sans connexion"
        state = js(f"{STATE_DIR}/{role}.json") if role else "{ cookies: [], origins: [] }"
        block = [f"  test.describe({js(label)}, () => {{", f"    test.use({{ storageState: {state} }});"]
        # Lectures avant écritures : l'ordre de déclaration est l'ordre d'exécution (un seul worker).
        for s in sorted(by_role[role], key=lambda x: x["writes"]):
            block.append("")
            block += _pw_screen(s, "    ") if s["kind"] == "screen" else _pw_http(s, "    ")
        block.append("  });")
        blocks.append(block)
    for skipped in feat["skipped"]:
        tags = [f"@{skipped['feature_id']}", "@non-rejoue"]
        note = {"type": "Non rejoué", "description": skipped["reason"]}
        blocks.append([f"  test.skip({js(skipped['title'])}, {{ tag: {js(tags)}, annotation: {js(note)} }}, "
                       "async () => {});"])
    for i, block in enumerate(blocks):
        if i:
            out.append("")
        out += block
    out += ["});", ""]
    return "\n".join(out)


PW_CONFIG = """import { defineConfig } from '@playwright/test';

const withSlash = (u: string) => (u.endsWith('/') ? u : u + '/');

export default defineConfig({
  testDir: '.',
  fullyParallel: false,
  workers: 1, // état partagé : dans chaque rôle, les scénarios qui écrivent passent après les lectures
  reporter: [['list'], ['html', { outputFolder: 'report', open: 'never' }], ['json', { outputFile: 'results.json' }]],
  // URL de base terminée par « / » : les routes générées lui sont relatives (préfixe /app/… conservé).
  use: { baseURL: withSlash(process.env.GM_BASE_URL || __BASE_URL__), screenshot: 'on', trace: 'retain-on-failure' },
  projects: [
__PROJECTS__
  ],
});
"""

PW_AUTH = """import { test as setup, expect } from '@playwright/test';

// Une session par rôle ; les mots de passe sont lus dans l'environnement, jamais dans un fichier.
const accounts: { role: string; login: string; password_env: string; start: string }[] = __ACCOUNTS__;
const usernameSelectors = ["input[type='email']", "input[name*='username' i]", "input[name*='login' i]",
  "input[name*='user' i]", "input[id*='username' i]", "input[id*='login' i]", "input[type='text']"];

for (const account of accounts) {
  setup(`connexion du rôle ${account.role}`, async ({ page }) => {
    const password = process.env[account.password_env];
    expect(password, `variable d'environnement ${account.password_env} absente`).toBeTruthy();
    await page.goto(account.start); // route protégée : la cible présente son formulaire de connexion
    const passwordField = page.locator("input[type='password']");
    await expect(passwordField).toBeVisible();
    await page.locator(usernameSelectors.join(', ')).first().fill(account.login);
    await passwordField.fill(password!);
    await passwordField.press('Enter');
    await expect(passwordField).toHaveCount(0);
    await page.context().storageState({ path: `__STATE_DIR__/${account.role}.json` });
  });
}
"""

PW_GITIGNORE = f"""# Généré par export-golden-master-tests.py — ne pas éditer.
{STATE_DIR}/
report/
results.json
test-results/
"""


def render_playwright(model: dict, ignore_fields: list[str] | None = None) -> dict[str, str]:
    """Modèle neutre -> {chemin relatif: contenu} pour Playwright Test."""
    files: dict[str, str] = {}
    for feat in model["features"]:
        files[f"specs/{feat['id']}.spec.ts"] = _pw_spec(feat, ignore_fields or [], model)
    # Nom des projets : distinct des étiquettes (@golden-master), affichées à côté dans le rapport.
    if model["accounts"]:
        projects = ("    { name: 'connexion', testMatch: /auth\\.setup\\.ts/ },\n"
                    "    { name: 'rejeu', testMatch: /specs\\/.*\\.spec\\.ts/, dependencies: ['connexion'] },")
        files["auth.setup.ts"] = HEADER + PW_AUTH.replace("__ACCOUNTS__", js(model["accounts"])).replace(
            "__STATE_DIR__", STATE_DIR)
    else:
        projects = "    { name: 'rejeu', testMatch: /specs\\/.*\\.spec\\.ts/ },"
    files["playwright.config.ts"] = HEADER + PW_CONFIG.replace("__BASE_URL__", js(model["base_url"])).replace(
        "__PROJECTS__", projects)
    files[".gitignore"] = PW_GITIGNORE
    return files


RUNNERS = {"playwright": render_playwright}


# ---------------------------------------------------------------------------
# Écriture / contrôle
# ---------------------------------------------------------------------------

def _generated_specs(output: Path) -> list[str]:
    """Specs déjà présentes sur disque et marquées comme générées."""
    specs = output / "specs"
    if not specs.is_dir():
        return []
    return sorted(f"specs/{p.name}" for p in specs.glob("*.spec.ts")
                  if p.read_text(encoding="utf-8").startswith(HEADER))


def build(runner: str, derived: Path, mapping_path: Path, gm_dir: Path | None, output: Path,
          check: bool = False) -> tuple[dict, int]:
    mapping = _load_json(mapping_path, required=True)
    if not isinstance(mapping, dict):
        raise UsageError(f"mapping illisible (objet attendu) : {mapping_path}")
    if not derived.is_dir():
        raise UsageError(f"golden master dérivé introuvable : {derived}")
    model = read_model(derived, mapping, gm_dir)
    config = _load_json(derived / "replay-config.json") or {}
    ignore_fields = ((config.get("comparison") or {}).get("http_response") or {}).get("ignore_fields") or []
    files = RUNNERS[runner](model, ignore_fields)

    tests = sum(len(f["scenarios"]) for f in model["features"])
    report = {"runner": runner, "output": str(output), "specs": len(model["features"]), "tests": tests,
              "skipped": sum(len(f["skipped"]) for f in model["features"]),
              "roles": [a["role"] for a in model["accounts"]], "files": sorted(files)}
    stale = [rel for rel in _generated_specs(output) if rel not in files]

    if check:
        changed = sorted([rel for rel, text in files.items()
                          if not (output / rel).is_file() or (output / rel).read_text(encoding="utf-8") != text]
                         + stale)
        report.update(check=True, changed=changed)
        return report, (1 if changed or not tests else 0)

    for rel, text in files.items():
        path = output / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
    for rel in stale:
        (output / rel).unlink()
    return report, (0 if tests else 1)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Génère les tests d'un runner (Playwright Test) depuis le golden master dérivé.",
        epilog=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--runner", required=True, choices=sorted(RUNNERS),
                        help="Runner cible (connu : " + ", ".join(sorted(RUNNERS)) + ")")
    parser.add_argument("--derived", required=True, type=Path, help="Golden master dérivé (sortie de derive)")
    parser.add_argument("--mapping", required=True, type=Path, help="replay-mapping.json (scénarios supprimés)")
    parser.add_argument("--gm-dir", type=Path, help="Golden master capturé (description des supprimés)")
    parser.add_argument("--output", required=True, type=Path, help="Dossier des tests générés")
    parser.add_argument("--check", action="store_true", help="Compare au disque sans écrire (code 1 si écart)")
    args = parser.parse_args(argv)
    try:
        report, code = build(args.runner, args.derived, args.mapping, args.gm_dir, args.output, args.check)
    except UsageError as exc:
        print(f"Erreur : {exc}", file=sys.stderr)
        return 2
    if not report["tests"]:
        print("Aucun scénario dérivé : rien à rejouer", file=sys.stderr)
    for rel in report.get("changed") or []:
        print(f"[check] fichier généré absent, modifié ou périmé : {rel}", file=sys.stderr)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return code


if __name__ == "__main__":
    sys.exit(main())
