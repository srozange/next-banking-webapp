#!/usr/bin/env python3
# /// script
# requires-python = ">=3.10"
# dependencies = ["playwright>=1.40"]
# ///
"""
Ouvre une session par rôle, sans jamais faire passer un mot de passe par un
agent (retro-skill-ejv).

Se connecte lui-même (logique de crawl-ui.py, factorisée dans auth_session.py)
avec le mot de passe lu dans retro/.env, puis enregistre la session ouverte
(storageState Playwright : cookies + localStorage) dans {output-dir}/{role}.json,
mode 0600. L'agent charge ensuite cette session dans le navigateur MCP :

  mcp__playwright__browser_run_code_unsafe(code="async (page) => { await
    page.context().setStorageState('{storage_state}'); return 'ok'; }")

Un contexte par rôle : aucune fuite de session entre rôles. Les fichiers ne
sont jamais nettoyés par ce script (décision utilisateur du 2026-09-25) ;
{output-dir} doit être ignoré par git — contrôlé avant toute écriture.

Cas où aucune session utilisable n'est enregistrée (logged_in: false, reason) :
  - no_login_form        : aucun formulaire de connexion à --base-url ;
  - still_on_login_form  : le formulaire est encore là après soumission ;
  - http_basic_ui        : la première navigation répond 401 + WWW-Authenticate
                           (fenêtre d'authentification du navigateur) ;
  - session_storage_only : le jeton vit en sessionStorage, absent du storageState.

**Aucune valeur n'est jamais imprimée** : ni mot de passe, ni cookie, ni contenu
de storageState. Tout avertissement passe par redact() avant d'aller sur stderr.

Arguments :
  --project-root     requis — racine du dépôt (legacy, ou repo cible quand le script est exporté)
  --base-url         requis — page de connexion (ou URL qui y redirige)
  --accounts         défaut {project-root}/retro/.crawl-accounts.json
                      (chemin JSON ou JSON inline) : [{role, login, password_env}]
  --role             répétable ; défaut : tous les rôles de --accounts
  --env-file         défaut {project-root}/retro/.env
  --output-dir       défaut {project-root}/retro/.auth
  --max-age-minutes  défaut 30 — un fichier plus récent est réutilisé (reused: true)
  --force            ignore l'âge du fichier existant, reconnecte

Sortie stdout : JSON {status: "ok"|"partial"|"error",
  sessions: [{role, storage_state, login_url, logged_in, reused, created_at[, reason]}],
  warnings: [...]}
Exit 0 = toutes les sessions ouvertes, 1 = au moins une connexion en échec,
2 = arguments invalides, mot de passe non résolu, ou {output-dir} non ignoré par git.
"""

import argparse
import inspect
import json
import os
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(inspect.currentframe().f_code.co_filename).resolve().parent))
from auth_session import (  # noqa: E402
    detect_session_storage_only,
    login_succeeded,
    read_dotenv,
    redact,
    resolve_accounts,
    resolve_password,
    try_login,
    validate_accounts,
)

WARNINGS: list[str] = []


def browser_install_command() -> str:
    """Commande d'installation de Chromium pour la version de Playwright de cet
    environnement (sys.executable = interpréteur de l'env uv du script)."""
    return f'"{sys.executable}" -m playwright install chromium'


def _make_note_warning(secrets: list[str]):
    def note_warning(where: str, exc: Exception) -> None:
        msg = redact(f"{where}: {exc}", secrets)
        print(msg, file=sys.stderr)
        WARNINGS.append(msg)

    return note_warning


def _rel_dir(path: Path, root: Path) -> str:
    """Dossier relatif à la racine du dépôt quand c'est possible, sinon tel quel."""
    try:
        return Path(path).resolve().relative_to(Path(root).resolve()).as_posix()
    except ValueError:
        return str(path)


def auth_dir_ignored(project_root: Path, output_dir: Path) -> bool | None:
    """True/False = réponse de `git check-ignore` ; None = pas un dépôt git
    (rien à ignorer). Même logique que retro-setup/check-env.py, dupliquée
    (DEC-029). Interroge un fichier *à l'intérieur* de output_dir : un motif
    de répertoire (`.auth/`) n'est reconnu contre le dossier lui-même que
    s'il existe déjà sur disque."""
    probe = output_dir / ".probe"
    try:
        rel = probe.resolve().relative_to(project_root.resolve())
    except ValueError:
        rel = probe

    def git(*args: str):
        return subprocess.run(["git", "-C", str(project_root), *args],
                              capture_output=True, text=True, timeout=10, check=False)

    try:
        if git("rev-parse", "--is-inside-work-tree").returncode != 0:
            return None
        return git("check-ignore", "-q", "--", str(rel)).returncode == 0
    except (OSError, subprocess.SubprocessError):
        return None


def _fail(payload: dict, exit_code: int) -> None:
    print(json.dumps(payload, ensure_ascii=False), file=sys.stderr)
    sys.exit(exit_code)


def main():
    parser = argparse.ArgumentParser(
        description="Ouvre une session par rôle (storageState Playwright), sans jamais faire "
                    "passer un mot de passe par un agent.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("--project-root", required=True, help="Racine du dépôt (legacy, ou repo cible quand le script est exporté)")
    parser.add_argument("--base-url", required=True, help="Page de connexion (ou URL qui y redirige)")
    parser.add_argument("--accounts", default=None,
                        help="Chemin JSON ou JSON inline : [{role, login, password_env}] "
                             "(défaut : {project-root}/retro/.crawl-accounts.json)")
    parser.add_argument("--role", action="append", default=None,
                        help="Répétable ; défaut : tous les rôles de --accounts")
    parser.add_argument("--env-file", default=None, help="Défaut : {project-root}/retro/.env")
    parser.add_argument("--output-dir", default=None, help="Défaut : {project-root}/retro/.auth")
    parser.add_argument("--max-age-minutes", type=float, default=30.0,
                        help="Un fichier de session plus récent est réutilisé (défaut 30)")
    parser.add_argument("--force", action="store_true", help="Ignore l'âge du fichier existant")
    args = parser.parse_args()

    project_root = Path(args.project_root)
    if not project_root.is_dir():
        _fail({"error": f"project-root introuvable : {project_root}"}, 2)

    accounts_arg = args.accounts or str(project_root / "retro" / ".crawl-accounts.json")
    env_file = Path(args.env_file) if args.env_file else project_root / "retro" / ".env"
    output_dir = Path(args.output_dir) if args.output_dir else project_root / "retro" / ".auth"

    try:
        accounts = resolve_accounts(accounts_arg)
    except (OSError, ValueError) as e:
        _fail({"error": f"--accounts invalide : {e}"}, 2)

    account_errors = validate_accounts(accounts)
    if account_errors:
        _fail({"error": "comptes non résolus — session non ouverte", "accounts": account_errors}, 2)

    if args.role:
        wanted = set(args.role)
        known = {a.get("role", "user") for a in accounts}
        unknown = sorted(wanted - known)
        if unknown:
            _fail({"error": f"rôle(s) inconnu(s) : {', '.join(unknown)} — "
                            f"rôles disponibles : {sorted(known)}"}, 2)
        accounts = [a for a in accounts if a.get("role", "user") in wanted]

    dotenv = read_dotenv(env_file)
    resolved: list[tuple[dict, str]] = []
    missing = []
    for account in accounts:
        password = resolve_password(account, os.environ, dotenv)
        if not password:
            missing.append(f"role {account.get('role', 'user')} : variable d'environnement "
                           f"{account.get('password_env')} non définie ou vide — la renseigner "
                           f"dans {env_file} ou l'exporter avant de lancer claude")
        resolved.append((account, password))
    if missing:
        _fail({"error": "comptes non résolus — session non ouverte", "accounts": missing}, 2)

    if auth_dir_ignored(project_root, output_dir) is False:
        _fail({
            "error": f"{output_dir} n'est pas ignoré par git — rien n'a été écrit",
            # Remède générique : le script est aussi exporté dans le repo cible (docs/retro/tools/),
            # où retro-setup n'existe pas (retro-skill-k9f5.9).
            "remedy": f"ajouter {_rel_dir(output_dir, project_root)}/ au .gitignore du dépôt ; "
                      "dans le dépôt legacy d'une rétro, uv run {skill-root}/../retro-setup/scripts/"
                      f"check-env.py --project-root {project_root} --prepare le fait",
        }, 2)

    note_warning = _make_note_warning([pw for _account, pw in resolved])

    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        _fail({"error": "Paquet Python playwright absent de l'environnement du script — relancer par uv run (dépendances du script) ; il ne s'installe pas à la main"}, 2)

    output_dir.mkdir(parents=True, exist_ok=True)

    sessions = []
    with sync_playwright() as pw:
        try:
            browser = pw.chromium.launch(headless=True)
        except Exception as e:
            if "Executable doesn't exist" in str(e) or "playwright install" in str(e):
                _fail({"error": "Navigateur Chromium de Playwright non installé. Lancez : "
                                + browser_install_command()}, 2)
            raise

        for account, password in resolved:
            role = account.get("role", "user")
            state_path = output_dir / f"{role}.json"

            if not args.force and state_path.is_file():
                mtime = state_path.stat().st_mtime
                if (time.time() - mtime) / 60 <= args.max_age_minutes:
                    sessions.append({
                        "role": role,
                        "storage_state": str(state_path),
                        "login_url": None,
                        "logged_in": True,
                        "reused": True,
                        "created_at": datetime.fromtimestamp(mtime, tz=timezone.utc).isoformat(),
                    })
                    continue

            ctx = browser.new_context()
            page = ctx.new_page()
            page.set_default_timeout(15000)

            resp = None
            try:
                resp = page.goto(args.base_url, wait_until="networkidle", timeout=20000)
            except Exception as exc:  # noqa: BLE001 — erreur Playwright/tierce non typable
                note_warning(f"navigation initiale ({role})", exc)

            login_url = page.url
            try:
                attempted, _field = try_login(page, account.get("login", ""), password, False, note_warning)
            except Exception as exc:  # noqa: BLE001 — erreur Playwright/tierce non typable
                note_warning(f"tentative de connexion ({role})", exc)
                attempted = False

            logged_in, reason = login_succeeded(page, resp)
            if logged_in and not attempted:
                logged_in, reason = False, "no_login_form"
            if logged_in and detect_session_storage_only(page):
                logged_in, reason = False, "session_storage_only"

            entry = {
                "role": role,
                "storage_state": None,
                "login_url": login_url,
                "logged_in": logged_in,
                "reused": False,
                "created_at": datetime.now(timezone.utc).isoformat(),
            }
            if logged_in:
                ctx.storage_state(path=str(state_path))
                os.chmod(state_path, 0o600)
                entry["storage_state"] = str(state_path)
            else:
                entry["reason"] = reason

            sessions.append(entry)
            ctx.close()

        browser.close()

    all_ok = all(s["logged_in"] for s in sessions)
    any_ok = any(s["logged_in"] for s in sessions)
    status = "ok" if all_ok else ("partial" if any_ok else "error")
    print(json.dumps({"status": status, "sessions": sessions, "warnings": WARNINGS}, ensure_ascii=False))
    sys.exit(0 if all_ok else 1)


if __name__ == "__main__":
    main()
