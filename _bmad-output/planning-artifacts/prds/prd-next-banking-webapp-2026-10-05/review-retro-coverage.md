# Revue de couverture du corpus retro

Verdict : **pass**

Mesure (script `check-prd-coverage.py`, exit 0) : `coverage.rules` = 100 % (161/161), `coverage.bugs` = 100 % (50/50). 331 FR analysées.

## Résultats

- `rules.missing` : aucune. `bugs.missing` : aucun.
- `bugs_kept.missing` : aucun (43/43 cités), documentés par `00-global/LEGACY-BUGS.md`.
- `forbidden_as_fr` : aucune.
- `blocking_gaps` : aucun.

## Findings

- **low** : retraits documentés, pour mémoire. FEAT-031 (analytique client et segmentation) et FEAT-032 (bean de notifications côté interface) sont retirées en section 5 Non-Goals, chacune avec sa raison en clair (code mort, abandon confirmé par le métier). Pas de « retrait sans raison ».
- **low** : règle « Suspect » promue par décision, pour mémoire. FR-003-28 (RG-027-003) et FR-027-17 (RG-023-009, RG-027-003) reprennent volontairement des règles suspectes, avec marqueur de décision (`reconcile-decisions-rg-027-003.md`).

Aucun finding critical, major ou medium.
