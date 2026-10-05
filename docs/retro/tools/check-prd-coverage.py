#!/usr/bin/env python3
# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///
"""
Couverture RG / BUG ↔ FR d'un PRD BMAD, contre le corpus retro exporté.

Autonome (stdlib seule) : copié dans {project_knowledge}/retro/tools/ par l'export,
exécuté par le reviewer `retro-coverage-review.md` au reviewer gate de bmad-prd, sur le PRD
que bmad-prd écrit lui-même depuis le brief et le corpus.

Lit dans --retro (export docs/retro) : rules-index.json (rg, feature, status, nature),
00-global/legacy-bugs.json (décisions), 00-global/FEATURES.md (features « Mort » ou
« Conservé » exclues, retro-skill-4nj), GAPS.md (lignes `[TODO bloquant]`, listées seulement).
Lit dans --prd : les blocs FR (titre `FR-N` de niveau 2 à 6 — `#### FR-1:` du gabarit BMAD,
`##### FR-026 :` ou `FR-005b` — jusqu'au titre suivant de niveau
égal ou supérieur, ou à la section **Notes:** / **Feature-specific NFRs:** de la feature, jamais
rattachée à une FR) et les identifiants RG-NNN-NNN / BUG-NNN qu'ils citent ; une plage
« RG-013-021 à RG-013-026 » (ou « – », « au ») d'une même feature vaut chacune de ses règles. --addendum (défaut : addendum.md à côté
du PRD, s'il existe) : identifiants BUG-NNN cités.

Attendu :
  - chaque RG `Confirmé`, nature autre que dette-legacy, d'une feature ni morte ni conservée
    dans l'application existante, citée par au moins une FR ;
  - chaque BUG « corriger » cité par au moins une FR ;
  - chaque BUG « ne pas corriger » cité dans l'addendum, ou un renvoi du PRD ou de l'addendum
    à LEGACY-BUGS.md, qui les liste tous (jamais exigé en FR ; retro-skill-x85h.7).
Retiré volontairement (retro-skill-rjsq.3) : une feature FEAT-NNN ou une règle RG-NNN-NNN
citée dans la section Non-Goals du PRD (titre contenant « Non-Goals » ou « Hors périmètre »)
n'est plus attendue — le métier l'a retirée du périmètre de la rénovation ; elle est listée
dans removed_in_prd pour la relecture.
Interdit (forbidden_as_fr) : FR qui ne cite que des RG `Suspect` sans marqueur de décision ;
FR qui cite un BUG « ne pas corriger » (ni exigence ni test).
Décision de séance (promoted_suspect, information, n'échoue pas — retro-skill-x85h.7) : FR qui
ne cite que des RG `Suspect` mais porte un marqueur de décision (`[ÉVOLUTION …]`,
`[CORRECTION …]`, « promu(e) ») : la règle suspecte a été reprise volontairement.
Manques bloquants (blocking_gaps, information, n'échoue pas) : lignes `[TODO bloquant]` de
GAPS.md ; savoir si le PRD y répond est un jugement, confirmé par le relecteur.

Sortie stdout : JSON
  {prd, addendum, frs,
   rules: {expected, covered, missing: [{rg, feature}]},
   bugs: {expected, covered, missing: [BUG-…]},
   bugs_kept: {expected, cited, missing: [BUG-…], documented_by},   # "LEGACY-BUGS.md" | "addendum" | null
   forbidden_as_fr: [{fr, ids, reason}],
   promoted_suspect: [{fr, ids}],
   blocking_gaps: [{file, text}],
   removed_in_prd: {features: [FEAT-…], rules: [RG-…]},
   coverage: {rules, bugs}}          # ratios 0..1 (1.0 si rien d'attendu)

Exit codes : 0 couverture complète ; 1 manque (rules/bugs/bugs_kept) ou FR interdite ;
2 PRD ou fichiers du corpus illisibles, ou format de PRD non reconnu (aucun titre FR alors
que le PRD cite des RG : jamais un 0 % trompeur).
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

RG_RE = re.compile(r"\bRG-\d{3}-\d{3}\b")
BUG_RE = re.compile(r"\bBUG-\d{3,}\b")
FR_RE = re.compile(r"^(#{2,6})\s+(FR-\d+[A-Za-z]?(?:[.-]\d+[A-Za-z]?)*)\b")
HEAD_RE = re.compile(r"^(#{1,6})\s")
RG_RANGE_RE = re.compile(r"\bRG-(\d{3})-(\d{3})\s*(?:à|au|–|—)\s*(?:RG-(\d{3})-)?(\d{3})\b")
MAX_RANGE = 100
# Sections de fin de feature du gabarit BMAD (après la dernière FR) : jamais rattachées à
# une FR (retro-skill-mz7k). Ce script est copié seul dans le repo cible : stdlib seule.
TAIL_RE = re.compile(r"^\*\*(Feature-specific NFRs|Notes)\b")
FEAT_ROW_RE = re.compile(r"^\|\s*(FEAT-\d+)\s*\|")
# Marqueurs de décision de séance constatés dans le PRD du POC 55m1 (retro-skill-x85h.7) :
# une FR fondée sur une RG Suspect qui en porte un reprend la règle volontairement.
DECISION_RE = re.compile(r"\[(?:ÉVOLUTION|EVOLUTION|CORRECTION)\b[^\]]*\]|\bpromue?s?\b", re.I)
# Renvoi à la liste des bugs du legacy : documente tous les bugs « ne pas corriger ».
BUGS_DOC_RE = re.compile(r"\bLEGACY-BUGS\.md\b")
GAP_CELL_RE = re.compile(r"(?<!\\)\|")


class Fail(Exception):
    pass


def rg_ids(text: str) -> set[str]:
    """RG citées, plages d'une même feature dépliées (« RG-013-021 à RG-013-026 »)."""
    ids = set(RG_RE.findall(text))
    for feat, start, feat_end, end in RG_RANGE_RE.findall(text):
        lo, hi = int(start), int(end)
        if (feat_end and feat_end != feat) or not lo < hi <= lo + MAX_RANGE:
            continue
        ids |= {f"RG-{feat}-{n:03d}" for n in range(lo, hi + 1)}
    return ids


def fr_blocks(text: str) -> list[tuple[str, str]]:
    """Blocs FR : un titre FR de niveau L court jusqu'au titre suivant de niveau ≤ L ; les
    sous-titres plus profonds restent dans le bloc. La section **Notes:** / **Feature-specific
    NFRs:** de la feature ferme le bloc : ses lignes ne sont rattachées à aucune FR jusqu'au
    titre suivant (retro-skill-mz7k)."""
    blocks, cur, level, buf, fence = [], None, 0, [], False
    for line in text.split("\n"):
        if line.startswith("```"):
            fence = not fence
        head = None if fence else HEAD_RE.match(line)
        if head and (cur is None or len(head.group(1)) <= level):
            if cur:
                blocks.append((cur, "\n".join(buf)))
            m = FR_RE.match(line)
            cur, level = (m.group(2), len(m.group(1))) if m else (None, 0)
            buf = [line]
            continue
        if cur and not fence and TAIL_RE.match(line):
            blocks.append((cur, "\n".join(buf)))
            cur, level, buf = None, 0, []
            continue
        if cur:
            buf.append(line)
    if cur:
        blocks.append((cur, "\n".join(buf)))
    return blocks


EXCLUDED_STATUSES = ("Mort", "Conservé")


def excluded_features(text: str) -> set[str]:
    """Features de statut exactement « Mort » ou « Conservé » dans FEATURES.md
    (colonne Statut des tables) : ni l'une ni l'autre n'est refaite, donc
    aucune RG confirmée n'est attendue en FR pour elles (retro-skill-4nj)."""
    dead, status_col = set(), None
    for line in text.split("\n"):
        if line.strip().startswith("|"):
            raw_cells = [c.strip() for c in line.strip().strip("|").split("|")]
            lower_cells = [c.lower() for c in raw_cells]
            if "statut" in lower_cells or "status" in lower_cells:
                status_col = (
                    lower_cells.index("statut") if "statut" in lower_cells
                    else lower_cells.index("status")
                )
                continue
            m = FEAT_ROW_RE.match(line.strip())
            if m and status_col is not None and status_col < len(raw_cells):
                if raw_cells[status_col] in EXCLUDED_STATUSES:
                    dead.add(m.group(1))
        else:
            status_col = None if not line.strip() else status_col
    return dead


def blocking_gaps(retro: Path) -> list[dict]:
    """Lignes `[TODO bloquant]` du tableau de GAPS.md exporté ([] si le fichier n'existe pas)."""
    path = retro / "GAPS.md"
    if not path.is_file():
        return []
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as exc:
        raise Fail(f"{path} illisible ({exc})")
    gaps, cols = [], None
    for line in text.split("\n"):
        line = line.strip()
        if not line.startswith("|"):
            cols = None
            continue
        cells = [c.strip() for c in GAP_CELL_RE.split(line.strip("|"))]
        lower = [c.lower() for c in cells]
        if "marqueur" in lower:
            cols = (lower.index("fichier") if "fichier" in lower else 0, lower.index("marqueur"),
                    lower.index("texte") if "texte" in lower else len(cells) - 1)
            continue
        if cols and max(cols) < len(cells) and cells[cols[1]].lower() == "[todo bloquant]":
            gaps.append({"file": cells[cols[0]], "text": cells[cols[2]].replace("\\|", "|")})
    return gaps


NON_GOALS_RE = re.compile(r"^(#{1,6})\s.*(?:non[- ]goals|hors[- ]p[ée]rim[èe]tre)", re.I | re.M)
FEAT_RE = re.compile(r"\bFEAT-\d{3}\b")


def non_goals(text: str) -> tuple[set[str], set[str]]:
    """(features, règles) citées dans la section Non-Goals du PRD : retirées volontairement du
    périmètre par le métier, donc hors couverture (retro-skill-rjsq.3)."""
    feats, rules = set(), set()
    for m in NON_GOALS_RE.finditer(text):
        level = len(m.group(1))
        end = re.compile(rf"^#{{1,{level}}}\s", re.M).search(text, m.end())
        body = text[m.end():end.start() if end else len(text)]
        feats |= set(FEAT_RE.findall(body))
        rules |= rg_ids(body)
    return feats, rules


def load_json(path: Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise Fail(f"{path} illisible ({exc})")


def check(prd: Path, retro: Path, addendum: Path | None) -> dict:
    try:
        text = prd.read_text(encoding="utf-8")
    except OSError as exc:
        raise Fail(f"PRD illisible : {prd} ({exc})")
    index = load_json(retro / "rules-index.json")
    if not isinstance(index, list):
        raise Fail("rules-index.json : liste attendue")
    bugs_path = retro / "00-global" / "legacy-bugs.json"
    bugs = [b for b in ((load_json(bugs_path).get("bugs") or []) if bugs_path.is_file() else [])
            # entrées remplacées (superseded_by, retro-skill-837) ignorées
            if not b.get("superseded_by")]
    feats_md = retro / "00-global" / "FEATURES.md"
    dead = excluded_features(feats_md.read_text(encoding="utf-8")) if feats_md.is_file() else set()
    if addendum is None and (prd.parent / "addendum.md").is_file():
        addendum = prd.parent / "addendum.md"
    add_text = addendum.read_text(encoding="utf-8") if addendum and addendum.is_file() else ""

    status = {r["rg"]: r for r in index if isinstance(r, dict) and r.get("rg")}
    removed_feats, removed_rules = non_goals(text)
    expected_rules = [r for r in status.values()
                      if str(r.get("status", "")).strip() == "Confirmé"
                      and str(r.get("nature") or "").strip().lower() != "dette-legacy"
                      and r.get("feature") not in dead
                      and r.get("feature") not in removed_feats
                      and r["rg"] not in removed_rules]
    decision = {b.get("id"): str(b.get("decision") or "").strip().lower() for b in bugs}
    fix = sorted(i for i, d in decision.items() if d == "corriger")
    kept = sorted(i for i, d in decision.items() if d == "ne pas corriger")

    blocks = fr_blocks(text)
    if not blocks and RG_RE.search(text):
        raise Fail("format de PRD non reconnu : le PRD cite des RG mais aucun titre de FR "
                   "(« FR-N » en titre de niveau 2 à 6) n'a été trouvé")
    cited_rg, cited_bug, forbidden, promoted = set(), set(), [], []
    for fr, body in blocks:
        rgs, bgs = rg_ids(body), set(BUG_RE.findall(body))
        cited_rg |= rgs
        cited_bug |= bgs
        if rgs and not bgs and all(str(status.get(r, {}).get("status", "")).strip() == "Suspect" for r in rgs):
            if DECISION_RE.search(body):
                promoted.append({"fr": fr, "ids": sorted(rgs)})
            else:
                forbidden.append({"fr": fr, "ids": sorted(rgs), "reason": "ne cite que des RG « Suspect »"})
        kept_here = sorted(b for b in bgs if b in kept)
        if kept_here:
            forbidden.append({"fr": fr, "ids": kept_here,
                              "reason": "cite un bug « ne pas corriger » (ni exigence ni test)"})
    add_bugs = set(BUG_RE.findall(add_text))

    missing_rules = [{"rg": r["rg"], "feature": r.get("feature")} for r in expected_rules
                     if r["rg"] not in cited_rg]
    missing_bugs = [b for b in fix if b not in cited_bug]
    if BUGS_DOC_RE.search(text + "\n" + add_text):
        missing_kept, documented_by = [], "LEGACY-BUGS.md"
    else:
        missing_kept = [b for b in kept if b not in add_bugs]
        documented_by = "addendum" if kept and not missing_kept else None

    def ratio(expected: int, missing: int) -> float:
        return round((expected - missing) / expected, 4) if expected else 1.0

    return {"prd": str(prd), "addendum": str(addendum) if add_text else None, "frs": len(blocks),
            "rules": {"expected": len(expected_rules), "covered": len(expected_rules) - len(missing_rules),
                      "missing": missing_rules},
            "bugs": {"expected": len(fix), "covered": len(fix) - len(missing_bugs), "missing": missing_bugs},
            "bugs_kept": {"expected": len(kept), "cited": len(kept) - len(missing_kept),
                          "missing": missing_kept, "documented_by": documented_by},
            "forbidden_as_fr": forbidden,
            "promoted_suspect": promoted,
            "blocking_gaps": blocking_gaps(retro),
            "removed_in_prd": {"features": sorted(removed_feats), "rules": sorted(removed_rules)},
            "coverage": {"rules": ratio(len(expected_rules), len(missing_rules)),
                         "bugs": ratio(len(fix), len(missing_bugs))}}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Couverture RG / BUG d'un PRD BMAD contre le corpus retro.",
                                     epilog=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--prd", required=True, type=Path, help="prd.md")
    parser.add_argument("--retro", required=True, type=Path, help="export du corpus (docs/retro)")
    parser.add_argument("--addendum", type=Path, help="addendum.md (défaut : à côté du PRD)")
    args = parser.parse_args(argv)
    try:
        out = check(args.prd, args.retro, args.addendum)
    except Fail as exc:
        print(f"check-prd-coverage : {exc}", file=sys.stderr)
        print(json.dumps({"status": "error", "error": str(exc)}, ensure_ascii=False, indent=2))
        return 2
    print(json.dumps(out, ensure_ascii=False, indent=2))
    failed = (out["rules"]["missing"] or out["bugs"]["missing"] or out["bugs_kept"]["missing"]
              or out["forbidden_as_fr"])
    if failed:
        print(f"check-prd-coverage : RG manquantes {len(out['rules']['missing'])}, BUG à corriger manquants "
              f"{len(out['bugs']['missing'])}, BUG conservés non documentés {len(out['bugs_kept']['missing'])}, "
              f"FR interdites {len(out['forbidden_as_fr'])}", file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
