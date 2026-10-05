- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: `scripts/gates/build.sh` ne vérifie pas les types du front (`tsc --noEmit`), une erreur de type passe tous les contrôles.
  evidence: le script lance seulement `npm ci`, depcruise et `vitest run --coverage` ; Vitest retire les types sans les vérifier. Script protégé : décision humaine avant l'empreinte (étape 10 du pilote).
- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: `.gitignore` ne couvre pas `.env` ni `.secrets/`, générés par `make secrets` et non versionnés selon AGENTS.md.
  evidence: risque de commit de secrets ; à ajouter au plus tard avec la story qui crée `make secrets` (1.4).
- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: aucune version de Node ni de Maven n'est imposée (`engines`, wrapper), le poste a Node 24.14.0 et Maven 3.9.14 pour 24.21.0 et 3.9.16 retenues.
  evidence: `versions-verifiees.md` ; `node -v`, `mvn -v` sur le poste.
- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: `frontend/index.html` porte un titre inventé ; le legacy impose `BankApp Demo - {titre de page}`.
  evidence: `docs/retro/00-global/MESSAGES.md` §1.1 ; à régler par la première story d'écran (2.3).
- source_spec: `_bmad-output/implementation-artifacts/spec-1-1-poser-le-squelette-des-deux-services-et-du-front.md`
  summary: le TODO d'AGENTS.md « commandes de build et de test unitaire par module » est à reporter (refresh de bmad-project-context).
  evidence: `mvn -B -q verify -DfailIfNoTests=true` ; `cd frontend && npm ci && npx vitest run && npm run typecheck`.
