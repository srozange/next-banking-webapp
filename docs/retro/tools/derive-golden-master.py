#!/usr/bin/env python3
# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///
"""
Dérive le golden master legacy vers une cible rénovée (agnostique du framework).

Les scénarios capturés sur le legacy (retro/golden-master/scenarios/**/) ne sont
pas rejouables tels quels sur une application réécrite : réponses HTML, bugs
legacy, contrats d'API éventuellement modifiés. Ce script classe les
scénarios, produit un squelette de décisions de mapping à faire remplir par
l'architecte, applique ces décisions (jamais d'approximation) et génère le
contrat des décisions attendues.

Bugs legacy : ce script ne les qualifie pas, il lit le registre produit et
tranché à la gate retro-verify (build-legacy-bugs.py) :
--legacy-bugs, défaut <gm-dir>/../00-global/legacy-bugs.json. Un scénario est
lié à un bug uniquement si son id figure dans `scenario_ids` du bug (rempli par
build-legacy-bugs.py à partir des scénarios tagués `bug` ou 5xx) ; les `rg_ids`
ne lient pas : un scénario nominal qui cite une RG dette-legacy marche avant ET
après, il reste dans le golden master. Registre absent ou illisible, ou bug lié dont la
décision est vide / « à qualifier » / invalide -> exit 2 (gate non passée ;
stdout : {status: "gate_not_passed", registry, undecided: [{id, decision,
scenarios}]}).
  - corriger (« Corriger lors de la rénovation ») : scénario classé legacy_bug,
                   exclu du golden master rejoué (skeleton le préremplit en drop,
                   justification « BUG-NNN — corrigé dans le forward… »).
  - ne pas corriger (bug connu toléré) : scénario classé legacy_bug, exclu du
                   golden master comme un bug « corriger » — on ne teste pas que
                   le bug est toujours là (drop prérempli, justification
                   « BUG-NNN — bug connu toléré, non testé dans la cible »).
  - faux positif : le scénario garde sa classe normale (gardé dans le golden
                   master). L'ancienne valeur « reproduire » est invalide -> exit 2.

Sous-commandes (toutes acceptent --legacy-bugs) :
  classify  --gm-dir DIR [--api-contract preserved|unknown]
      Classe chaque scénario (legacy_bug | api | ui_html | db_state | dom_text | unknown).
      Règle : lié à un bug « corriger » ou « ne pas corriger » -> legacy_bug ; sinon, pour http_response,
      content-type json/xml -> api, text/html -> ui_html (quel que soit le statut,
      5xx compris : la gate l'a qualifié), 3xx sans Content-Type (redirection
      après POST de formulaire) -> ui_html ; sans http_response : db_state /
      dom_text selon expected_outputs.
      stdout : {total, legacy_bugs_registry (absolu, couche travail),
                legacy_bugs_registry_rel (relatif à la racine du corpus), by_class, excluded: [{id, bugs, reasons}], scenarios: [{id,
                feature_id, class, portability, legacy_bugs: [{id, decision}],
                status_code, response_content_type,
                legacy: {method, path, content_type, fields, query_params}}]}
      Scénario d'écran (dom_text avec input-actions.json, ou legacy_bug capturé à
      l'écran) : legacy = {start_page (relative à adapters.playwright.base_url du
      replay-config legacy), role, selectors, wait_texts, expected_lines}.
  skeleton  --gm-dir DIR --output replay-mapping.json [--api-contract …]
      Écrit le squelette de mapping (une entrée par scénario, slots cible vides,
      [ASK ARCHITECT]). Préremplissages : legacy_bug -> decision drop +
      rationale selon la décision du bug (« BUG-NNN — corrigé dans le forward… »
      ou « BUG-NNN — bug connu toléré, non testé dans la cible » ; décision
      métier, pas de "proposed") ; api -> decision keep avec "proposed": true.
      Scénario d'écran : target = {start_url, selector_map: {sélecteur legacy: null}},
      expected = {mode, text_map} ; en tête, si le corpus en contient : routes
      ({page legacy: [ASK ARCHITECT]}) et browser ({base_url, storage_state_dir: .auth,
      accounts repris de adapters.playwright du legacy}). Le format complet est dans _doc.
      Fichier existant -> exit 2 (jamais écrasé).
      stdout : {output, entries, excluded, proposed}
  derive    --gm-dir DIR --mapping replay-mapping.json --output-dir DIR [--force]
      Valide strictement chaque entrée (un scénario lié à un bug « corriger » ou
      « ne pas corriger » doit rester en drop) puis écrit DIR/<FEAT>/<SC>/ (scenario.json,
      input-request.json, expected-response.json) et DIR/replay-config.json.
      map HTTP : form_tokens_from du legacy retiré (sauf target.form_tokens_from, page
      cible) ; target.body, s'il est présent (null compris), remplace le corps.
      Scénario d'écran : map ou drop ; écrit scenario.json (replay_adapter playwright,
      compare.dom_text.mode = expected.mode, sinon mode legacy du scénario, sinon de
      la configuration legacy, sinon exact), input-actions.json (route cible relative,
      sélecteurs et textes d'attente remplacés, actions à null retirées) et
      expected-dom.txt (lignes remplacées par text_map, null retirées) ; le bloc
      browser du mapping devient adapters.playwright du replay-config dérivé
      (storage_state_dir relatif au répertoire du rejeu, ignoré par git).
      stdout : {output_dir, portable, derived, dropped: [{id, rationale}],
                written: [id], errors: [{id, field, message}],
                rules: {kept, lost: [RG], lost_by_scenario: {SC: [RG]}}}
      Rejeu : replay-golden-master.py --scenarios DIR --config DIR/replay-config.json --target new-app
  contract  --gm-dir DIR [--mapping FILE] --output golden-master-contract.md [--questions-json FILE]
      Génère le contrat des décisions (markdown : scénarios exclus listés avec
      leur motif) et, en option, la liste des
      questions [{type: "question", text}] consommée par le memlog. Le contrat
      est livré au repo cible : aucun chemin absolu local n'y figure. Le registre
      y est cité par un chemin relatif à la racine du corpus, calculé par le
      script (legacy_bugs_registry_rel de classify, ex. 00-global/legacy-bugs.json).
      stdout : {output, scenario_decisions, global_decisions, questions_json}

Sortie stdout : un objet JSON par sous-commande (voir ci-dessus).
Exit 0 = OK ; 1 = échec métier (mapping incomplet ou invalide,
aucun scénario) ; 2 = bloquant / usage (répertoire introuvable, JSON illisible,
fichier de sortie déjà présent, registre des bugs absent ou gate non passée).
Diagnostics sur stderr.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
import urllib.parse
from collections import Counter
from pathlib import Path

ASK = "[ASK ARCHITECT]"
DECISIONS = ("keep", "map", "drop")
BUG_DECISIONS = ("corriger", "ne pas corriger", "faux positif")
BUG_DECISION_ALIASES = {"faux-positif": "faux positif", "faux_positif": "faux positif"}
FIXED_RATIONALE = "corrigé dans le forward, validé par les tests de la nouvelle application"
TOLERATED_RATIONALE = "bug connu toléré, non testé dans la cible"
# Décisions de bug qui excluent le scénario lié du golden master, avec leur motif.
EXCLUDING_DECISIONS = {"corriger": FIXED_RATIONALE, "ne pas corriger": TOLERATED_RATIONALE}
MODES = ("exact", "status+json_subset")
TEXT_MODES = ("exact", "contains", "contains-ci")  # modes dom_text de replay-golden-master.py
BODY_FORMATS = ("json", "form")
NO_AUTH = (None, "", "none")
# Format des scénarios d'écran, ajouté au champ _doc du squelette quand le corpus en contient.
DOC_SCREEN = (
    "Scénario d'écran (class dom_text) : decision map | drop (keep refusé). map = route de départ "
    "(target.start_url, sinon routes[legacy.start_page]) ; target.selector_map : {sélecteur_legacy: "
    "sélecteur_cible | null (action retirée, rationale exigée)}, une clé par sélecteur de legacy.selectors ; "
    "expected.mode : exact | contains | contains-ci | null (= mode legacy) ; expected.text_map : "
    "{ligne_ou_texte_legacy: texte_cible | null (ligne retirée)}, les autres lignes sont reprises telles quelles. "
    "routes : {page_legacy: route_cible}. browser (sessions navigateur des scénarios d'écran) : base_url "
    "(null = target_base_url), storage_state_dir (relatif au répertoire du rejeu, ignoré par git), "
    "accounts ({role, login, password_env}). "
)
# Sélecteur de champ mot de passe : le rejeu refuse une action fill qui le vise (la connexion passe
# par les sessions enregistrées). Motif dupliqué de replay-golden-master.py (PASSWORD_SELECTOR_RE).
PASSWORD_SELECTOR_RE = re.compile(
    r"type=['\"]?password|pass(word|wd)?|pwd|mdp|mot[_-]?de[_-]?passe", re.IGNORECASE)

PORTABILITY = {
    "api": "portable_if_contract_preserved",
    "ui_html": "needs_mapping",
    "legacy_bug": "excluded",
    "db_state": "portable_if_model_preserved",
    "dom_text": "needs_mapping",
    "cli": "portable_if_contract_preserved",
    "unknown": "needs_decision",
}


class UsageError(Exception):
    """Erreur bloquante -> exit 2."""


class GateNotPassed(UsageError):
    """Registre des bugs legacy non tranché pour des scénarios du golden master -> exit 2."""

    def __init__(self, registry: Path, undecided: list[dict]):
        self.registry = registry
        self.undecided = undecided
        ids = ", ".join(f"{u['id']} ({u['decision'] or 'vide'})" for u in undecided)
        super().__init__(f"gate retro-verify non passée — bugs liés sans décision valide : {ids}")


# ---------------------------------------------------------------------------
# Lecture du corpus
# ---------------------------------------------------------------------------

def _load_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return None
    except (json.JSONDecodeError, UnicodeDecodeError) as exc:
        raise UsageError(f"JSON illisible : {path} ({exc})") from exc


def _header(headers: dict | None, name: str) -> str | None:
    for key, value in (headers or {}).items():
        if key.lower() == name.lower():
            return str(value)
    return None


def _media_type(value: str | None) -> str | None:
    if not value:
        return None
    return value.split(";", 1)[0].strip().lower() or None


def load_scenarios(gm_dir: Path) -> list[dict]:
    """Retourne [{dir, rel, scenario, request, expected}] triés par chemin."""
    scen_root = gm_dir / "scenarios"
    if not scen_root.is_dir():
        raise UsageError(f"Répertoire de scénarios introuvable : {scen_root}")
    out = []
    for sfile in sorted(scen_root.rglob("scenario.json")):
        sdir = sfile.parent
        out.append({
            "dir": sdir,
            "rel": sdir.relative_to(scen_root),
            "scenario": _load_json(sfile) or {},
            "request": _load_json(sdir / "input-request.json"),
            "expected": _load_json(sdir / "expected-response.json"),
            "actions": _load_json(sdir / "input-actions.json"),
            "expected_dom": _read_text(sdir / "expected-dom.txt"),
        })
    return out


def _read_text(path: Path) -> str | None:
    try:
        return path.read_text(encoding="utf-8")
    except FileNotFoundError:
        return None


def legacy_playwright_base(gm_dir: Path) -> str:
    """URL de base du rejeu navigateur sur le legacy (replay-config.json), sans « / » final."""
    config = _load_json(gm_dir / "replay-config.json") or {}
    adapters = config.get("adapters", {}) if isinstance(config, dict) else {}
    return str((adapters.get("playwright") or {}).get("base_url") or "").rstrip("/")


def legacy_page(start_url: str | None, base: str) -> str:
    """Page de départ legacy, relative à l'URL de base du rejeu navigateur.
    URL hors de cette base -> chemin (et requête) de l'URL."""
    start_url = start_url or "/"
    # Préfixe retiré seulement à une limite de chemin : base /app ne couvre pas /app2/… (k9f5.14).
    if base and (start_url == base or start_url.startswith((base + "/", base + "?"))):
        return "/" + start_url[len(base):].lstrip("/")
    parsed = urllib.parse.urlsplit(start_url)
    if parsed.scheme:
        return (parsed.path or "/") + (f"?{parsed.query}" if parsed.query else "")
    return start_url if start_url.startswith("/") else "/" + start_url


def expected_lines(text: str | None) -> list[str]:
    """Lignes non vides de expected-dom.txt, blancs normalisés comme le fait le rejeu."""
    return [line for line in (" ".join(raw.split()) for raw in (text or "").splitlines()) if line]


def legacy_screen(item: dict, base: str) -> dict:
    """Ce que l'architecte doit faire correspondre pour un scénario d'écran."""
    data = item.get("actions") or {}
    actions = data.get("actions") or []
    selectors = list(dict.fromkeys(a["selector"] for a in actions if isinstance(a, dict) and a.get("selector")))
    wait_texts = list(dict.fromkeys(a["text"] for a in actions if isinstance(a, dict)
                                    and a.get("type") == "wait_for" and a.get("text")))
    return {
        "start_page": legacy_page(data.get("start_url"), base),
        "role": data.get("role") or None,
        "selectors": selectors,
        "wait_texts": wait_texts,
        "expected_lines": expected_lines(item.get("expected_dom")),
    }


def legacy_entry_point(request: dict | None) -> dict:
    request = request or {}
    raw_path = request.get("path")
    query_params: list[str] = []
    if raw_path and "?" in raw_path:
        query = raw_path.split("?", 1)[1]
        query_params = list(dict.fromkeys(k for k, _ in urllib.parse.parse_qsl(query, keep_blank_values=True)))
    body = request.get("body")
    fields = list(body.keys()) if isinstance(body, dict) else []
    return {
        "method": (request.get("method") or "GET").upper() if request else None,
        "path": raw_path,
        "content_type": _media_type(_header(request.get("headers"), "Content-Type")),
        "fields": fields,
        "query_params": query_params,
    }


def default_legacy_bugs_path(gm_dir: Path) -> Path:
    return gm_dir.resolve().parent / "00-global" / "legacy-bugs.json"


def registry_display_path(registry: Path, gm_dir: Path) -> str:
    """Chemin du registre relatif à la racine du corpus (<gm-dir>/..), en POSIX.
    Registre hors du corpus -> nom de fichier seul (jamais de chemin absolu)."""
    try:
        return registry.resolve().relative_to(gm_dir.resolve().parent).as_posix()
    except ValueError:
        return registry.name


def load_legacy_bugs(path: Path) -> list[dict]:
    """Lit le registre (build-legacy-bugs.py). Absent ou illisible -> UsageError."""
    if not path.is_file():
        raise UsageError(f"registre des bugs legacy absent : {path} "
                         "(gate retro-verify non passée — lancer build-legacy-bugs.py)")
    data = _load_json(path)
    if not isinstance(data, dict) or not isinstance(data.get("bugs"), list):
        raise UsageError(f"registre des bugs legacy invalide (liste 'bugs' attendue) : {path}")
    return [b for b in data["bugs"]
            # entrées remplacées (superseded_by, retro-skill-837) ignorées
            if not (isinstance(b, dict) and b.get("superseded_by"))]


def _bug_decision(bug: dict) -> str:
    raw = str(bug.get("decision") or "").strip().lower()
    return BUG_DECISION_ALIASES.get(raw, raw)


def link_legacy_bugs(items: list[dict], bugs: list[dict], registry: Path) -> dict[str, list[dict]]:
    """Retourne {scenario_id: [{id, decision}]}. Lève GateNotPassed si un bug lié
    à un scénario n'a pas de décision valide."""
    links: dict[str, list[dict]] = {}
    undecided: dict[str, dict] = {}
    for it in items:
        sid = it["scenario"].get("id", it["dir"].name)
        for bug in bugs:
            if sid in (bug.get("scenario_ids") or []):
                decision = _bug_decision(bug)
                links.setdefault(sid, []).append({"id": bug.get("id"), "decision": decision})
                if decision not in BUG_DECISIONS:
                    u = undecided.setdefault(bug.get("id"), {"id": bug.get("id"),
                                                              "decision": decision, "scenarios": []})
                    u["scenarios"].append(sid)
    if undecided:
        raise GateNotPassed(registry, sorted(undecided.values(), key=lambda u: str(u["id"])))
    return links


def _is_cli(scenario: dict) -> bool:
    """Scénario de l'adaptateur cli (retro-skill-8v9, live_instance.kind: cli) :
    le contrat d'une commande (sous-commande, options, code de retour) est
    l'équivalent d'un contrat d'API — portable si la nouvelle application
    garde la même commande."""
    outputs = scenario.get("expected_outputs") or []
    return "stdout" in outputs or "exit_code" in outputs


def classify_one(item: dict, api_contract: str, bug_links: list[dict] | None = None,
                 pw_base: str = "") -> dict:
    scenario = item["scenario"]
    expected = item["expected"]
    outputs = scenario.get("expected_outputs") or []
    bug_links = bug_links or []
    status = None
    resp_ct = None

    if "http_response" in outputs and isinstance(expected, dict):
        status = expected.get("status_code", expected.get("status"))
        resp_ct = _media_type(_header(expected.get("headers"), "Content-Type"))
        if resp_ct and (resp_ct.endswith("json") or resp_ct.endswith("xml")):
            cls = "api"
        elif resp_ct == "text/html":
            cls = "ui_html"
        elif resp_ct is None and isinstance(status, int) and 300 <= status < 400:
            cls = "ui_html"
        else:
            cls = "unknown"
    elif "db_state" in outputs:
        cls = "db_state"
    elif "dom_text" in outputs:
        cls = "dom_text"
    elif _is_cli(scenario):
        cls = "cli"
    else:
        cls = "unknown"

    screen_class = cls == "dom_text"
    if any(b["decision"] in EXCLUDING_DECISIONS for b in bug_links):
        cls = "legacy_bug"

    portability = PORTABILITY[cls]
    if cls == "api" and api_contract == "preserved":
        portability = "portable"

    return {
        "id": scenario.get("id", item["dir"].name),
        "feature_id": scenario.get("feature_id"),
        "description": scenario.get("description"),
        "class": cls,
        "portability": portability,
        "legacy_bugs": bug_links,
        "status_code": status,
        "response_content_type": resp_ct,
        # Bloc d'écran seulement pour un scénario d'écran (classe dom_text avant l'exclusion pour bug) :
        # un scénario dom_text + db_state reste un scénario db_state (retro-skill-k9f5.14).
        "legacy": (legacy_screen(item, pw_base) if screen_class and item.get("actions") is not None
                   else legacy_entry_point(item["request"])),
    }


def excluding_bug_ids(row: dict) -> list[str]:
    """Bugs « corriger » / « ne pas corriger » liés au scénario (motifs d'exclusion)."""
    return [b["id"] for b in row["legacy_bugs"] if b["decision"] in EXCLUDING_DECISIONS]


def exclusion_reasons(row: dict) -> list[str]:
    """Une justification par décision excluante : « BUG-NNN[, …] — motif »."""
    reasons = []
    for decision, motive in EXCLUDING_DECISIONS.items():
        ids = [b["id"] for b in row["legacy_bugs"] if b["decision"] == decision]
        if ids:
            reasons.append(f"{', '.join(ids)} — {motive}")
    return reasons


def classify(gm_dir: Path, api_contract: str = "unknown", legacy_bugs: Path | None = None) -> dict:
    items = load_scenarios(gm_dir)
    registry = legacy_bugs or default_legacy_bugs_path(gm_dir)
    links = link_legacy_bugs(items, load_legacy_bugs(registry), registry)
    pw_base = legacy_playwright_base(gm_dir)
    rows = [classify_one(it, api_contract, links.get(it["scenario"].get("id", it["dir"].name)), pw_base)
            for it in items]
    by_class = Counter(r["class"] for r in rows)
    return {
        "total": len(rows),
        "legacy_bugs_registry": str(registry),
        "legacy_bugs_registry_rel": registry_display_path(registry, gm_dir),
        "by_class": dict(sorted(by_class.items())),
        "excluded": [{"id": r["id"], "bugs": excluding_bug_ids(r), "reasons": exclusion_reasons(r)}
                     for r in rows if r["class"] == "legacy_bug"],
        "scenarios": rows,
    }


# ---------------------------------------------------------------------------
# skeleton
# ---------------------------------------------------------------------------

def build_skeleton(classification: dict, gm_dir: Path | None = None) -> dict:
    """Construit le squelette replay-mapping.json.

    gm_dir : répertoire du golden master legacy (retro/golden-master). Si fourni,
    lit `replay-config.json` pour détecter `auth_scheme: basic` et le propager
    dans le squelette (retro-skill-56t).
    """
    # Détection de l'auth_scheme du legacy (retro-skill-56t)
    legacy_auth_scheme = ""
    legacy_http_accounts: list[dict] = []
    legacy_pw_accounts: list[dict] = []
    if gm_dir is not None:
        legacy_config = _load_json(gm_dir / "replay-config.json") or {}
        if isinstance(legacy_config, dict):
            legacy_pw_accounts = (legacy_config.get("adapters", {}).get("playwright") or {}).get("accounts") or []
        http_cfg = legacy_config.get("adapters", {}).get("http", {}) if isinstance(legacy_config, dict) else {}
        legacy_auth_scheme = http_cfg.get("auth_scheme", "").lower()
        legacy_http_accounts = http_cfg.get("accounts") or []

    entries = []
    for row in classification["scenarios"]:
        entry = {
            "id": row["id"],
            "feature_id": row["feature_id"],
            "class": row["class"],
            "legacy_bugs": row["legacy_bugs"],
            "legacy": row["legacy"],
            "decision": ASK,
            "target": {"method": None, "path": None, "body_format": None, "field_map": {}},
            "expected": {"mode": None, "status": None, "json_subset": None},
            "rationale": None,
        }
        if "start_page" in row["legacy"]:  # scénario d'écran (dom_text, ou legacy_bug capturé à l'écran)
            entry["target"] = {"start_url": None,
                               "selector_map": {sel: None for sel in row["legacy"]["selectors"]}}
            entry["expected"] = {"mode": None, "text_map": {}}
        if row["class"] == "legacy_bug":
            entry["decision"] = "drop"
            entry["rationale"] = " ; ".join(exclusion_reasons(row))
        elif row["class"] == "api":
            entry["decision"] = "keep"
            entry["proposed"] = True
        elif row["class"] == "cli":
            # La dérivation cli (commande cible) n'est pas encore supportée
            # (retro-skill-8v9) : laisser ASK plutôt que proposer un "keep"
            # que `derive` refuserait — voir la note dans `derive`.
            entry["rationale"] = (
                "scénario cli — dérivation vers une nouvelle commande non supportée par ce "
                "script ; décider à la main (map, avec la commande cible) ou drop"
            )
        entries.append(entry)

    result: dict = {
        "schema": 1,
        "target_base_url": ASK,
    }
    if legacy_auth_scheme == "basic":
        result["auth_scheme"] = "basic"
        result["accounts"] = legacy_http_accounts
        doc_auth = (
            "auth_scheme : \"basic\" (auto-dérivé depuis replay-config.json du legacy ; "
            "renseigner accounts avec role, login, password_env pour chaque rôle de la cible). "
        )
    else:
        result["auth"] = ASK
        doc_auth = "auth : null/\"none\" ou en-tête \"Nom: valeur\" ({token} <- REPLAY_TOKEN). "

    pages = list(dict.fromkeys(r["legacy"]["start_page"] for r in classification["scenarios"]
                               if r["class"] == "dom_text" and "start_page" in r["legacy"]))
    if pages:
        result["routes"] = {page: ASK for page in pages}
        result["browser"] = {"base_url": None, "storage_state_dir": ".auth", "accounts": legacy_pw_accounts}

    result["_doc"] = (
        "decision : keep | map | drop. keep = requête et réponse legacy inchangées ; "
        "map = target.method/path + expected.mode (status+json_subset) requis ; "
        "drop = rationale. Les scénarios legacy_bug (bug « corriger » ou « ne pas corriger » du registre) restent en drop. "
        "expected.mode : exact | status+json_subset (status requis). "
        "target.body_format : json | form | null (= format legacy). "
        "target.field_map : {champ_legacy: champ_cible | null (supprimé)}. "
        "target.body (facultatif) : corps cible qui remplace le corps legacy (null = sans corps), incompatible "
        "avec field_map. target.form_tokens_from (facultatif) : page cible des jetons de formulaire ; sur map, "
        "ceux du legacy sont retirés. "
        + doc_auth + (DOC_SCREEN if pages else "") +
        "Retirer \"proposed\": true pour confirmer une proposition."
    )
    result["scenarios"] = entries
    return result


# ---------------------------------------------------------------------------
# derive
# ---------------------------------------------------------------------------

def _contains_ask(obj) -> bool:
    if isinstance(obj, str):
        return ASK in obj
    if isinstance(obj, dict):
        return any(_contains_ask(v) for v in obj.values())
    if isinstance(obj, list):
        return any(_contains_ask(v) for v in obj)
    return False


def screen_start_url(entry: dict, legacy: dict, mapping: dict):
    """Route de départ cible : target.start_url, sinon routes[page legacy]."""
    url = (entry.get("target") or {}).get("start_url")
    if not url:
        routes = mapping.get("routes")
        url = routes.get(legacy["start_page"]) if isinstance(routes, dict) else None
    return url


def fill_selectors(item: dict) -> set[str]:
    """Sélecteurs legacy visés par une action fill (saisie)."""
    actions = (item.get("actions") or {}).get("actions") or []
    return {a["selector"] for a in actions
            if isinstance(a, dict) and a.get("type") == "fill" and a.get("selector")}


def validate_screen(sid: str, entry: dict, legacy: dict, mapping: dict, err, has_rationale: bool,
                    fills: set[str] = frozenset()) -> None:
    """Décision d'un scénario d'écran (class dom_text) : route, sélecteurs, textes."""
    if entry.get("decision") != "map":
        err(sid, "decision", "scénario d'écran : map ou drop (keep refusé, la page legacy n'existe pas sur la cible)")
        return
    target = entry.get("target") or {}
    expected = entry.get("expected") or {}
    url = screen_start_url(entry, legacy, mapping)
    if not isinstance(url, str) or not url.strip() or ASK in url:
        err(sid, "target.start_url",
            f"route cible absente : renseigner target.start_url ou routes[{legacy['start_page']!r}]")
    elif not url.startswith("/"):
        err(sid, "target.start_url", "route cible relative à l'URL de base attendue (commence par « / »)")
    smap = target.get("selector_map") or {}
    if not isinstance(smap, dict):
        err(sid, "target.selector_map", "selector_map attendu : objet")
        smap = {}
    for sel in legacy["selectors"]:
        if sel not in smap:
            err(sid, f"target.selector_map.{sel}", "sélecteur legacy sans décision")
        elif smap[sel] is None:
            if not has_rationale:
                err(sid, f"target.selector_map.{sel}", "action retirée (null) : rationale exigée")
        elif not isinstance(smap[sel], str) or not smap[sel].strip():
            err(sid, f"target.selector_map.{sel}", "sélecteur cible attendu : chaîne non vide ou null")
        elif sel in fills and PASSWORD_SELECTOR_RE.search(smap[sel]):
            err(sid, f"target.selector_map.{sel}",
                "saisie dans un champ mot de passe refusée par le rejeu : la connexion passe par les "
                "sessions de rôle (bloc browser), retirer l'action (null) ou viser un autre champ")
    for sel in smap:
        if sel not in legacy["selectors"]:
            err(sid, f"target.selector_map.{sel}", "sélecteur absent du scénario legacy")
    mode = expected.get("mode")
    if mode is not None and mode not in TEXT_MODES:
        err(sid, "expected.mode", f"mode attendu parmi {', '.join(TEXT_MODES)} ou null (reçu {mode!r})")
    tmap = expected.get("text_map") or {}
    if not isinstance(tmap, dict):
        err(sid, "expected.text_map", "text_map attendu : objet")
        tmap = {}
    known = set(legacy["expected_lines"]) | set(legacy["wait_texts"])
    for text, new in tmap.items():
        if text not in known:
            err(sid, f"expected.text_map.{text}", "texte absent du scénario legacy")
        if new is not None and (not isinstance(new, str) or not new.strip()):
            err(sid, f"expected.text_map.{text}", "texte cible attendu : chaîne non vide ou null")
    if not legacy["expected_lines"]:
        err(sid, "expected-dom.txt", "aucune ligne attendue capturée : le scénario ne vérifie rien")
    elif not [line for line in legacy["expected_lines"] if tmap.get(line, line) is not None]:
        err(sid, "expected.text_map", "toutes les lignes attendues sont retirées : le scénario ne vérifierait plus rien")


def validate_mapping(mapping: dict, items_by_id: dict[str, dict],
                     excluded: dict[str, list[str]] | None = None,
                     screens: dict[str, dict] | None = None) -> list[dict]:
    """Retourne la liste des erreurs [{id, field, message}] (vide si valide)."""
    errors: list[dict] = []

    def err(sid, field, message):
        errors.append({"id": sid, "field": field, "message": message})

    if not isinstance(mapping, dict) or not isinstance(mapping.get("scenarios"), list):
        err(None, "scenarios", "mapping sans liste 'scenarios'")
        return errors

    base = mapping.get("target_base_url")
    if not isinstance(base, str) or not base.strip() or ASK in base:
        err(None, "target_base_url", "URL de la cible non renseignée")

    auth_scheme = str(mapping.get("auth_scheme") or "").lower()
    auth = mapping.get("auth")
    if auth_scheme == "basic":
        # auth et auth_scheme: basic sont mutuellement exclusifs
        if auth not in NO_AUTH:
            err(None, "auth", "auth_scheme: basic et auth sont mutuellement exclusifs — "
                "retirer auth ou le passer à null")
        # Validation des comptes
        accounts = mapping.get("accounts") or []
        if not accounts:
            err(None, "accounts",
                "auth_scheme: basic — accounts requis (liste de {role, login, password_env})")
        for acc in (accounts if isinstance(accounts, list) else []):
            if not isinstance(acc, dict):
                err(None, "accounts", "auth_scheme: basic — chaque compte doit être un objet")
                continue
            role = acc.get("role") or ""
            if not role:
                err(None, "accounts", "auth_scheme: basic — compte sans role")
            if not acc.get("login"):
                err(None, "accounts", f"auth_scheme: basic — compte {role or '?'!r} sans login")
            if not acc.get("password_env"):
                err(None, "accounts", f"auth_scheme: basic — compte {role or '?'!r} sans password_env")
    else:
        if auth not in NO_AUTH and (not isinstance(auth, str) or ASK in auth or ": " not in auth):
            err(None, "auth", "auth attendu : null, \"none\" ou un en-tête \"Nom: valeur\"")

    seen: set[str] = set()
    screen_roles: set = set()
    for entry in mapping["scenarios"]:
        sid = entry.get("id")
        if sid in seen:
            err(sid, "id", "entrée en double")
            continue
        seen.add(sid)
        if sid not in items_by_id:
            err(sid, "id", "scénario absent du golden master")
            continue
        decision = entry.get("decision")
        if decision not in DECISIONS:
            err(sid, "decision", f"décision attendue parmi {', '.join(DECISIONS)} (reçu {decision!r})")
            continue
        rationale = entry.get("rationale")
        has_rationale = isinstance(rationale, str) and rationale.strip() and ASK not in rationale
        if excluded and sid in excluded and decision != "drop":
            err(sid, "decision", f"bug {', '.join(excluded[sid])} (corriger / ne pas corriger) : "
                "le scénario est exclu du golden master et doit rester en drop")
            continue
        if decision == "drop":
            if not has_rationale:
                err(sid, "rationale", "drop exige une justification")
            continue
        if entry.get("proposed"):
            err(sid, "proposed", "proposition non confirmée (retirer \"proposed\": true)")
        if _contains_ask(entry):
            err(sid, "decision", f"{ASK} restant dans l'entrée")
        if screens and sid in screens:
            validate_screen(sid, entry, screens[sid], mapping, err, bool(has_rationale),
                            fill_selectors(items_by_id[sid]))
            screen_roles.add(screens[sid].get("role"))
            continue

        target = entry.get("target") or {}
        expected = entry.get("expected") or {}
        mode = expected.get("mode")
        legacy_fields = set(legacy_entry_point(items_by_id[sid]["request"])["fields"])

        if decision == "map":
            if not target.get("method"):
                err(sid, "target.method", "map exige la méthode cible")
            if not target.get("path"):
                err(sid, "target.path", "map exige le chemin cible")
        if bool(target.get("method")) != bool(target.get("path")):
            err(sid, "target", "target.method et target.path vont ensemble")

        if decision == "map":
            if mode is None:
                err(sid, "expected.mode", "map exige expected.mode")
            elif mode != "status+json_subset":
                err(sid, "expected.mode",
                    "map : seul status+json_subset est dérivable (la réponse legacy ne fait pas foi)")
        elif mode is not None and mode not in MODES:
            err(sid, "expected.mode", f"mode attendu parmi {', '.join(MODES)} (reçu {mode!r})")

        if mode == "status+json_subset":
            if not isinstance(expected.get("status"), int):
                err(sid, "expected.status", "status+json_subset exige expected.status (entier)")
            subset = expected.get("json_subset")
            if subset is not None and not isinstance(subset, (dict, list)):
                err(sid, "expected.json_subset", "json_subset attendu : objet, liste ou null")

        body_format = target.get("body_format")
        if body_format is not None and body_format not in BODY_FORMATS:
            err(sid, "target.body_format", f"body_format attendu parmi {', '.join(BODY_FORMATS)} ou null")
        field_map = target.get("field_map") or {}
        if not isinstance(field_map, dict):
            err(sid, "target.field_map", "field_map attendu : objet")
        else:
            for legacy_name, target_name in field_map.items():
                if legacy_name not in legacy_fields:
                    err(sid, f"target.field_map.{legacy_name}", "champ absent du corps legacy")
                if target_name is not None and not isinstance(target_name, str):
                    err(sid, f"target.field_map.{legacy_name}", "nom cible attendu : chaîne ou null")
        if decision == "keep" and (field_map or body_format or target.get("method")
                                   or "body" in target or target.get("form_tokens_from")):
            err(sid, "target", "keep n'accepte aucune transformation (utiliser map)")
        if "body" in target and field_map:
            err(sid, "target.body", "target.body remplace le corps : incompatible avec field_map")
        tokens_from = target.get("form_tokens_from")
        if tokens_from is not None and (not isinstance(tokens_from, str) or not tokens_from.startswith("/")):
            err(sid, "target.form_tokens_from", "page cible des jetons de formulaire : chemin commençant par « / »")

    browser = mapping.get("browser") if isinstance(mapping.get("browser"), dict) else {}
    if screen_roles:  # au moins un scénario d'écran gardé
        pw_base = browser.get("base_url")
        if pw_base is not None and (not isinstance(pw_base, str) or ASK in pw_base
                                    or not pw_base.startswith(("http://", "https://"))):
            err(None, "browser.base_url", "URL absolue (http:// ou https://) ou null (= target_base_url) attendue")
    if screen_roles - {None}:
        state_dir = browser.get("storage_state_dir")
        if not isinstance(state_dir, str) or not state_dir.strip() or ASK in state_dir:
            err(None, "browser.storage_state_dir",
                "scénarios d'écran sous un rôle : dossier des sessions requis")
        declared = {a.get("role") for a in (browser.get("accounts") or []) if isinstance(a, dict)}
        for role in sorted(screen_roles - {None} - declared):
            err(None, "browser.accounts", f"aucun compte pour le rôle {role!r} (role, login, password_env)")
        for acc in browser.get("accounts") or []:
            if isinstance(acc, dict) and acc.get("role") in screen_roles and not (
                    acc.get("login") and acc.get("password_env")):
                err(None, "browser.accounts", f"compte du rôle {acc.get('role')!r} : login et password_env requis")

    for sid in items_by_id:
        if sid not in seen:
            err(sid, "id", "scénario sans entrée dans le mapping")
    return errors


def derive_request(request: dict, target: dict) -> dict:
    """Requête cible d'une entrée map (retro-skill-k9f5.7) : les jetons de formulaire legacy
    (form_tokens_from) sont retirés, sauf target.form_tokens_from (page cible) ; target.body,
    s'il est présent (null compris), remplace le corps."""
    new = json.loads(json.dumps(request))  # copie profonde
    if target.get("method"):
        new["method"] = target["method"].upper()
        new["path"] = target["path"]
    new.pop("form_tokens_from", None)
    if target.get("form_tokens_from"):
        new["form_tokens_from"] = target["form_tokens_from"]
    if "body" in target:
        new["body"] = json.loads(json.dumps(target["body"]))
    body = new.get("body")
    field_map = target.get("field_map") or {}
    if isinstance(body, dict) and field_map:
        renamed = {}
        for key, value in body.items():
            if key in field_map:
                if field_map[key] is None:
                    continue
                renamed[field_map[key]] = value
            else:
                renamed[key] = value
        new["body"] = renamed
    body_format = target.get("body_format")
    if body_format:
        headers = {k: v for k, v in (new.get("headers") or {}).items() if k.lower() != "content-type"}
        if new.get("body") is not None:
            headers["Content-Type"] = (
                "application/json" if body_format == "json" else "application/x-www-form-urlencoded"
            )
        new["headers"] = headers
    return new


def derive_actions(actions: list, smap: dict, tmap: dict) -> list[dict]:
    """Actions legacy transposées : sélecteurs et textes d'attente remplacés, actions retirées (null) omises."""
    out = []
    for action in actions:
        new = dict(action)
        sel = action.get("selector")
        if sel is not None:
            if smap.get(sel) is None:
                continue
            new["selector"] = smap[sel]
        if action.get("type") == "wait_for" and action.get("text"):
            text = tmap.get(action["text"], action["text"])
            if text is None:
                continue
            new["text"] = text
        out.append(new)
    return out


def derive_screen(item: dict, entry: dict, legacy: dict, mapping: dict, default_mode: str) -> tuple[dict, dict, str]:
    """→ (scenario.json, input-actions.json, expected-dom.txt) d'un scénario d'écran dérivé."""
    target = entry.get("target") or {}
    expected = entry.get("expected") or {}
    smap = target.get("selector_map") or {}
    tmap = expected.get("text_map") or {}
    legacy_mode = ((item["scenario"].get("compare") or {}).get("dom_text") or {}).get("mode")
    mode = expected.get("mode") or legacy_mode or default_mode
    scenario = json.loads(json.dumps(item["scenario"]))
    scenario["derived_from"] = {"id": entry["id"], "decision": entry["decision"],
                                "legacy": {"start_page": legacy["start_page"], "role": legacy["role"]}}
    if entry.get("rationale"):
        scenario["derived_from"]["rationale"] = entry["rationale"]
    scenario["compare"] = {"dom_text": {"mode": mode}}
    scenario["expected_outputs"] = ["dom_text"]
    scenario["replay_adapter"] = "playwright"
    legacy_actions = item.get("actions") or {}
    actions = {"start_url": screen_start_url(entry, legacy, mapping), "role": legacy_actions.get("role") or "",
               "actions": derive_actions(legacy_actions.get("actions") or [], smap, tmap)}
    lines = [tmap.get(line, line) for line in legacy["expected_lines"]]
    return scenario, actions, "\n".join(line for line in lines if line is not None) + "\n"


def derive(gm_dir: Path, mapping: dict, output_dir: Path, force: bool = False,
           legacy_bugs: Path | None = None) -> tuple[dict, int]:
    items = load_scenarios(gm_dir)
    items_by_id = {it["scenario"].get("id", it["dir"].name): it for it in items}
    classification = classify(gm_dir, legacy_bugs=legacy_bugs)
    excluded = {e["id"]: e["bugs"] for e in classification["excluded"]}
    # Scénarios d'écran : classe dom_text et input-actions.json présent (bloc legacy d'écran).
    screen_ids = {r["id"] for r in classification["scenarios"] if r["class"] == "dom_text"}
    screens = {r["id"]: r["legacy"] for r in classification["scenarios"]
               if r["id"] in screen_ids and "start_page" in r["legacy"]}
    report = {"output_dir": str(output_dir), "portable": 0, "derived": 0,
              "dropped": [], "written": [], "errors": []}

    errors = validate_mapping(mapping, items_by_id, excluded, screens)
    for sid, it in items_by_id.items():
        kept = any(
            e.get("id") == sid and e.get("decision") != "drop"
            for e in mapping.get("scenarios", []) if isinstance(e, dict)
        )
        if not kept:
            continue
        if _is_cli(it["scenario"]):
            # Dérivation cli non supportée (retro-skill-8v9) : classify/skeleton
            # reconnaissent la classe (rejeu baseline sur le legacy couvert par
            # replay_cli), mais construire une commande cible depuis la commande
            # legacy attend un ticket ultérieur — erreur franche plutôt qu'un
            # input-request.json vide écrit en silence.
            errors.append({"id": sid, "field": "expected_outputs",
                           "message": "scénario cli — dérivation vers une nouvelle commande non "
                                      "supportée, garder ce scénario en 'drop' pour l'instant"})
        elif sid in screen_ids and sid not in screens:
            errors.append({"id": sid, "field": "input-actions.json", "message": "input-actions.json manquant"})
        elif sid not in screens and it["request"] is None:
            errors.append({"id": sid, "field": "input-request.json", "message": "input-request.json manquant"})
    if errors:
        report["errors"] = errors
        return report, 1

    if output_dir.exists() and any(output_dir.iterdir()):
        if not force:
            raise UsageError(f"Répertoire de sortie non vide : {output_dir} (utiliser --force pour le régénérer)")
        shutil.rmtree(output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    legacy_config = _load_json(gm_dir / "replay-config.json") or {}
    comparison = json.loads(json.dumps(legacy_config.get("comparison") or {}))
    comparison.setdefault("http_response", {})
    comparison["http_response"]["mode"] = "exact"
    http_adapter: dict = {"base_url": mapping["target_base_url"]}
    auth_scheme = str(mapping.get("auth_scheme") or "").lower()
    if auth_scheme == "basic":
        # Auth Basic auto-dérivée (retro-skill-56t) : propager auth_scheme et accounts
        http_adapter["auth_scheme"] = "basic"
        accounts = mapping.get("accounts") or []
        if accounts:
            http_adapter["accounts"] = accounts
    elif mapping.get("auth") not in NO_AUTH:
        # Rétro-compat : jeton opaque partagé
        http_adapter["auth_header"] = mapping["auth"]
    adapters: dict = {"http": http_adapter}
    kept_screens = [e["id"] for e in mapping["scenarios"] if e["id"] in screens and e["decision"] != "drop"]
    if kept_screens:
        # Bloc « browser » du mapping -> adaptateur navigateur du rejeu (adapters.playwright).
        browser = mapping.get("browser") if isinstance(mapping.get("browser"), dict) else {}
        pw_adapter: dict = {"base_url": browser.get("base_url") or mapping["target_base_url"]}
        # Le rejeu exige un rôle par scénario dès que storage_state_dir est fixé : seulement si un
        # scénario d'écran gardé est joué sous un rôle.
        if browser.get("storage_state_dir") and any(screens[sid].get("role") for sid in kept_screens):
            pw_adapter["storage_state_dir"] = browser["storage_state_dir"]
        if browser.get("accounts"):
            pw_adapter["accounts"] = browser["accounts"]
        adapters["playwright"] = pw_adapter
    default_text_mode = (comparison.get("dom_text") or {}).get("mode") or "exact"
    (output_dir / "replay-config.json").write_text(json.dumps({
        "target_base_url": mapping["target_base_url"],
        "adapters": adapters,
        "comparison": comparison,
        "_note": "Généré par derive-golden-master.py — ne pas éditer, modifier replay-mapping.json puis relancer derive.",
    }, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    kept_rules: set[str] = set()
    dropped_rules: dict[str, list[str]] = {}
    for entry in mapping["scenarios"]:
        sid = entry["id"]
        decision = entry["decision"]
        it = items_by_id[sid]
        rg_ids = list(it["scenario"].get("rg_ids") or [])
        if decision == "drop":
            report["dropped"].append({"id": sid, "rationale": entry["rationale"]})
            dropped_rules[sid] = rg_ids
            continue
        kept_rules.update(rg_ids)
        if sid in screens:
            scenario, actions, text = derive_screen(it, entry, screens[sid], mapping, default_text_mode)
            dest = output_dir / it["rel"]
            dest.mkdir(parents=True, exist_ok=True)
            for name, data in (("scenario.json", scenario), ("input-actions.json", actions)):
                (dest / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            (dest / "expected-dom.txt").write_text(text, encoding="utf-8")
            report["written"].append(sid)
            report["derived"] += 1
            continue
        target = entry.get("target") or {}
        expected_cfg = entry.get("expected") or {}
        mode = expected_cfg.get("mode") or "exact"

        dest = output_dir / it["rel"]
        dest.mkdir(parents=True, exist_ok=True)

        scenario = json.loads(json.dumps(it["scenario"]))
        scenario["derived_from"] = {"id": sid, "decision": decision, "legacy": legacy_entry_point(it["request"])}
        if entry.get("rationale"):
            scenario["derived_from"]["rationale"] = entry["rationale"]
        scenario["compare"] = {"mode": mode, "json_subset": expected_cfg.get("json_subset")}
        scenario["expected_outputs"] = ["http_response"]
        scenario["replay_adapter"] = "http"

        request = derive_request(it["request"], target) if decision != "keep" else it["request"]

        if mode == "exact":
            if it["expected"] is None:
                report["errors"].append({"id": sid, "field": "expected-response.json",
                                         "message": "keep exact exige expected-response.json legacy"})
                continue
            expected = it["expected"]
        else:
            expected = {"status_code": expected_cfg["status"], "body": expected_cfg.get("json_subset")}

        for name, data in (("scenario.json", scenario), ("input-request.json", request),
                           ("expected-response.json", expected)):
            (dest / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        report["written"].append(sid)
        if decision == "keep":
            report["portable"] += 1
        else:
            report["derived"] += 1

    lost = sorted({rg for ids in dropped_rules.values() for rg in ids} - kept_rules)
    report["rules"] = {"kept": len(kept_rules), "lost": lost,
                       "lost_by_scenario": {sid: sorted(set(ids) - kept_rules)
                                            for sid, ids in dropped_rules.items() if set(ids) - kept_rules}}
    return report, (1 if report["errors"] else 0)


# ---------------------------------------------------------------------------
# contract
# ---------------------------------------------------------------------------

def _expected_decision(row: dict) -> tuple[str, str]:
    cls = row["class"]
    if cls == "api":
        return ("Contrat d'API préservé ? keep, sinon map (point d'entrée + sous-ensemble JSON)",
                "bmad-architecture")
    if cls == "ui_html":
        return ("Point d'entrée cible et sortie qui fait foi : map (status+json_subset) ou drop",
                "bmad-architecture")
    if cls == "db_state":
        return ("Modèle de données préservé ? adapter sql à implémenter", "bmad-architecture")
    if cls == "dom_text":
        return ("Route cible, correspondance des sélecteurs et des textes : map, ou drop justifié",
                "bmad-architecture")
    if cls == "cli":
        return ("Commande cible : dérivation cli non supportée par cet outil, drop justifié en attendant",
                "bmad-architecture")
    if row.get("status_code") is not None:  # unknown avec http_response
        return ("Réponse HTTP sans type de contenu capturé : map (status+json_subset) ou drop",
                "bmad-architecture")
    return ("Classe indéterminée : qualifier le scénario puis décider", "bmad-architecture")


REFERENCE_DATA = "00-global/reference-data.json"  # produit par retro-data, relatif à la racine du corpus
GLOBAL_DECISIONS = [
    "URL de base et mode d'authentification de la cible (`target_base_url`, `auth` de replay-mapping.json)",
    "Sortie qui fait foi pour une feature UI : état base (db_state), extrait de réponse JSON (status+json_subset) "
    "ou texte affiché (scénario d'écran)",
    "Table des routes : page de l'application existante vers route cible (`routes` de replay-mapping.json)",
    "Sessions navigateur de test par rôle : connexion par formulaire, comptes et dossier des sessions "
    "(`browser` de replay-mapping.json)",
    f"Chargement de `{REFERENCE_DATA}` dans la base cible avant tout rejeu",
    "Remise à zéro de la base cible avant un rejeu complet",
    "Bouchons des intégrations tierces (messagerie, paiement, services externes) pendant le rejeu",
    "Adapters `sql`, `batch`, `mq` du rejeu (stubs) à implémenter selon la stack cible",
    "Critère de cutover : le golden master dérivé rejoue vert (`failed: 0`, `errors: 0`) sur la cible",
]
NO_REFERENCE_DATA = (f"Données de référence non produites par la rétro (pas de `{REFERENCE_DATA}`) : "
                     "définir le jeu de données de la cible et son chargement avant tout rejeu")


def global_decisions(corpus_root: Path | None) -> list[str]:
    """Décisions globales ; la ligne des données de référence ne cite le fichier que s'il existe."""
    if corpus_root is None or (corpus_root / REFERENCE_DATA).is_file():
        return list(GLOBAL_DECISIONS)
    return [NO_REFERENCE_DATA if REFERENCE_DATA in d and d.startswith("Chargement") else d
            for d in GLOBAL_DECISIONS]


def build_contract(classification: dict, mapping: dict | None,
                   corpus_root: Path | None = None) -> tuple[str, list[dict]]:
    registry_display = classification["legacy_bugs_registry_rel"]
    current = {}
    if mapping:
        for entry in mapping.get("scenarios", []):
            if isinstance(entry, dict) and entry.get("id"):
                current[entry["id"]] = entry.get("decision")

    lines = [
        "# Contrat de dérivation du golden master",
        "",
        "Document généré par `derive-golden-master.py contract` — ne pas éditer à la main.",
        "",
        f"Scénarios : {classification['total']} — "
        + ", ".join(f"{k} {v}" for k, v in classification["by_class"].items()),
        "",
        f"Registre des bugs legacy : `{registry_display}`",
        "",
        "## Décisions par scénario",
        "",
        "| Scénario | Feature | Classe | Décision attendue | Qui/quand | Décision actuelle |",
        "|---|---|---|---|---|---|",
    ]
    questions: list[dict] = []
    for row in classification["scenarios"]:
        if row["class"] == "legacy_bug":
            continue
        text, who = _expected_decision(row)
        cls = row["class"]
        decision = current.get(row["id"]) or ASK
        lines.append(f"| {row['id']} | {row['feature_id'] or '—'} | {cls} | {text} | {who} | {decision} |")
        if decision in (None, ASK) or (mapping and _entry_proposed(mapping, row["id"])):
            questions.append({
                "type": "question",
                "text": f"Golden master {row['id']} ({row['feature_id']}, {cls}, "
                        f"{_legacy_label(row['legacy'])}) : {text} — {who}",
            })

    lines += [
        "",
        "## Scénarios exclus du golden master (pour information)",
        "",
        "Bugs legacy tranchés à la gate retro-verify : « corriger » — le comportement legacy "
        "n'est pas rejoué, la correction est validée par les tests de la nouvelle application ; "
        "« ne pas corriger » — bug connu toléré, ni exigence ni test : on ne vérifie pas "
        "qu'il est toujours là. « faux positif » reste dans le golden master.",
        "",
    ]
    if classification["excluded"]:
        lines += ["| Scénario | Feature | Bug(s) | Motif |", "|---|---|---|---|"]
        feats = {r["id"]: r["feature_id"] for r in classification["scenarios"]}
        for e in classification["excluded"]:
            motives = " ; ".join(r.split(" — ", 1)[1] for r in e["reasons"])
            lines.append(f"| {e['id']} | {feats.get(e['id']) or '—'} | {', '.join(e['bugs'])} | {motives} |")
    else:
        lines.append("Aucun.")

    lines += ["", "## Décisions globales", ""]
    for text in global_decisions(corpus_root):
        lines.append(f"- [ ] {text}")
        questions.append({"type": "question", "text": f"Golden master — {text}"})

    lines += [
        "",
        "## Procédure",
        "",
        "0. (Optionnel) Revoir la classification sans rien écrire : "
        "`uv run docs/retro/tools/derive-golden-master.py classify --gm-dir <golden-master>`.",
        "1. Écrire le squelette de décisions, puis le remplir (c'est lui, `replay-mapping.json`, "
        "qui porte les décisions de l'équipe — jamais réécrasé) : "
        "`uv run docs/retro/tools/derive-golden-master.py skeleton --gm-dir <golden-master> "
        "--output replay-mapping.json`",
        "2. `uv run docs/retro/tools/derive-golden-master.py derive --gm-dir <golden-master> "
        "--mapping replay-mapping.json --output-dir <golden-master>/derived`",
        "3. `uv run docs/retro/tools/replay-golden-master.py --scenarios <golden-master>/derived "
        "--config <golden-master>/derived/replay-config.json --target new-app`",
        "",
    ]
    return "\n".join(lines), questions


def _legacy_label(legacy: dict) -> str:
    """Point d'entrée legacy en clair : page de départ (écran) ou méthode et chemin (HTTP)."""
    if "start_page" in legacy:
        return f"page {legacy['start_page']}"
    return f"{legacy.get('method')} {legacy.get('path')}"


def _entry_proposed(mapping: dict, sid: str) -> bool:
    for entry in mapping.get("scenarios", []):
        if isinstance(entry, dict) and entry.get("id") == sid:
            return bool(entry.get("proposed"))
    return False


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def _emit(data: dict) -> None:
    print(json.dumps(data, ensure_ascii=False, indent=2))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Classe, mappe et dérive le golden master legacy vers une cible rénovée.",
        epilog=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    sub = parser.add_subparsers(dest="command", required=True)

    p = sub.add_parser("classify", help="Classe les scénarios")
    p.add_argument("--gm-dir", required=True)
    p.add_argument("--api-contract", choices=("preserved", "unknown"), default="unknown")

    p = sub.add_parser("skeleton", help="Écrit le squelette replay-mapping.json")
    p.add_argument("--gm-dir", required=True)
    p.add_argument("--output", required=True)
    p.add_argument("--api-contract", choices=("preserved", "unknown"), default="unknown")

    p = sub.add_parser("derive", help="Applique le mapping et écrit le golden master dérivé")
    p.add_argument("--gm-dir", required=True)
    p.add_argument("--mapping", required=True)
    p.add_argument("--output-dir", required=True)
    p.add_argument("--force", action="store_true", help="Régénère un répertoire de sortie non vide")

    p = sub.add_parser("contract", help="Génère golden-master-contract.md")
    p.add_argument("--gm-dir", required=True)
    p.add_argument("--mapping")
    p.add_argument("--output", required=True)
    p.add_argument("--questions-json")
    p.add_argument("--api-contract", choices=("preserved", "unknown"), default="unknown")

    for name, sp in sub.choices.items():
        sp.add_argument("--legacy-bugs",
                        help="Registre des bugs legacy (défaut <gm-dir>/../00-global/legacy-bugs.json)")

    args = parser.parse_args(argv)
    gm_dir = Path(args.gm_dir)
    legacy_bugs = Path(args.legacy_bugs) if args.legacy_bugs else None

    try:
        if args.command == "classify":
            result = classify(gm_dir, args.api_contract, legacy_bugs)
            _emit(result)
            return 0 if result["total"] else 1

        if args.command == "skeleton":
            out = Path(args.output)
            if out.exists():
                raise UsageError(f"{out} existe déjà — jamais écrasé (il porte les décisions de l'équipe)")
            classification = classify(gm_dir, args.api_contract, legacy_bugs)
            if not classification["total"]:
                print("Aucun scénario trouvé", file=sys.stderr)
                return 1
            skeleton = build_skeleton(classification, gm_dir=gm_dir)
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps(skeleton, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            _emit({"output": str(out), "entries": len(skeleton["scenarios"]),
                   "excluded": classification["excluded"],
                   "proposed": [e["id"] for e in skeleton["scenarios"] if e.get("proposed")]})
            return 0

        if args.command == "derive":
            mapping = _load_json(Path(args.mapping))
            if mapping is None:
                raise UsageError(f"Mapping introuvable : {args.mapping}")
            report, code = derive(gm_dir, mapping, Path(args.output_dir), args.force, legacy_bugs)
            for e in report["errors"]:
                print(f"[mapping] {e['id']} · {e['field']} : {e['message']}", file=sys.stderr)
            _emit(report)
            return code

        if args.command == "contract":
            mapping = None
            if args.mapping:
                mapping = _load_json(Path(args.mapping))
                if mapping is None:
                    raise UsageError(f"Mapping introuvable : {args.mapping}")
            classification = classify(gm_dir, args.api_contract, legacy_bugs)
            text, questions = build_contract(classification, mapping, gm_dir.resolve().parent)
            out = Path(args.output)
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(text, encoding="utf-8")
            if args.questions_json:
                qpath = Path(args.questions_json)
                qpath.parent.mkdir(parents=True, exist_ok=True)
                qpath.write_text(json.dumps(questions, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
            _emit({"output": str(out),
                   "scenario_decisions": classification["total"],
                   "global_decisions": len(GLOBAL_DECISIONS),
                   "questions": len(questions),
                   "questions_json": args.questions_json})
            return 0 if classification["total"] else 1
    except GateNotPassed as exc:
        print(f"Erreur : {exc}", file=sys.stderr)
        _emit({"status": "gate_not_passed", "registry": str(exc.registry), "undecided": exc.undecided})
        return 2
    except UsageError as exc:
        print(f"Erreur : {exc}", file=sys.stderr)
        return 2
    return 2


if __name__ == "__main__":
    sys.exit(main())
