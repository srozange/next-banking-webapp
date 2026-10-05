#!/bin/bash
# Assemble le PRD depuis _draft/ puis mesure la couverture.
set -e
W=_bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05
{ cat $W/_draft/00-head.md; for i in $(seq -w 1 30); do f=$W/_draft/FEAT-0$i.md; if [ -f $f ]; then cat $f; echo; else echo "### FEAT-0$i — (section manquante)"; echo; fi; done; cat $W/_draft/90-tail.md; [ -f $W/_draft/95-assumptions.md ] && cat $W/_draft/95-assumptions.md; } > $W/prd.md
python3 docs/retro/tools/check-prd-coverage.py --prd $W/prd.md --retro docs/retro --addendum $W/addendum.md > $W/_work/coverage.json || true
python3 -c "
import json;d=json.load(open('$W/_work/coverage.json'))
print('frs',d['frs'],'cov',d['coverage']);print('rules missing',len(d['rules']['missing']),d['rules']['missing'][:20]);print('bugs missing',d['bugs']['missing']);print('kept',d['bugs_kept']['missing'][:5],d['bugs_kept']['documented_by']);print('forbidden',d['forbidden_as_fr']);print('promoted',d['promoted_suspect']);print('removed',d['removed_in_prd'])"
