# Revue golden master — ARCHITECTURE-SPINE.md (avec golden-master-architecture.md et ecarts-voulus-golden-master.md)

**Verdict : pass**

Dérivation à blanc exécutée depuis la racine du projet (`replay-mapping.json`), code de sortie 0, `errors: []`.

## Compteurs (JSON du script)

- derived : 31
- portable : 0
- dropped : 2
- rules.kept : 67
- rules.lost : 1 (RG-010-007)

## Suppressions et raisons

| Scénario | Raison | Règles perdues | Recevable |
|---|---|---|---|
| SC-003-03 | BUG-011, corrigé dans le forward, validé par les tests de la nouvelle application | aucune | oui (bug tranché à la rétro) |
| SC-010-02 | BUG-038, corrigé dans le forward, validé par les tests de la nouvelle application | RG-010-007 | oui (bug tranché à la rétro) |

## Décisions globales (contrat)

| Décision | Où | État |
|---|---|---|
| URL de base et authentification | companion §0 ; AD-11 ; mapping (`http://localhost:8080/`, `auth: null`) | tranchée |
| Sortie qui fait foi pour une feature UI | companion §4 (texte affiché `innerText`, mode `contains`) | tranchée |
| Table des routes | companion §1 ; mapping (14 routes) | tranchée |
| Sessions navigateur par rôle | companion §2 ; mapping (`browser`, 3 comptes) | tranchée |
| Données de référence | companion §5.1 (un `reference-data.sql` par service, identifiants du legacy) | tranchée |
| Remise à zéro avant rejeu | companion §5.2 et §7.3 ; AD-11 (`make reset-data`) | tranchée |
| Bouchons des tiers | companion §6 ; AD-20 (Powens, AlphaVantage, Azure OpenAI) | tranchée |
| Adapters sql / batch / mq | aucun scénario de ces classes (31 = 26 écran + 5 HTTP) | sans objet |
| Critère de cutover | companion §7.3 (31 réussis, 0 échec) ; AD-11 (preuve) | tranchée |

## Findings

- critical : 0
- high : 0
- medium : 1
  - M1. Date de référence du rejeu (`2026-11-17T10:00:00`) : la vérification « postérieure aux dates d'`import.sql`, `user1` a 8 opérations au plus ce jour-là » reste ouverte (companion §8, point 9). À faire avant la première exécution de SC-003-04.
- low : 3
  - L1. `make reset-data`, `make rejeu-up` et `compose.rejeu.yaml` n'existent pas encore (story 1.7, numéro en [ASSUMPTION], AD-11). Décision prise, mise en oeuvre à livrer.
  - L2. Le companion est en `status: draft`, présente les valeurs comme « propositions » et liste en §8 des points déjà réglés (mapping rempli en G2, enveloppe d'erreur fixée par AD-17). À mettre à jour pour qu'il ne contredise pas le mapping.
  - L3. FR-001-10 (invalidation du jeton à la déconnexion) non tranchée (companion §8, point 11) ; le rejeu tient dans les deux cas grâce à l'isolation de SC-001-05 (§7.3).

## Notes

- RG-010-007 sort du golden master avec SC-010-02 (BUG-038) ; elle est couverte par le test de la nouvelle application pour FR-010-09 (NFR-PAR-02), avec l'adaptateur Azure OpenAI simulé en mode refus.
- Écarts voulus (SC-001-02, SC-001-03, SC-017-01 adaptés en `map` ; SC-010-02 exclu) consignés avec raison et couverture dans `ecarts-voulus-golden-master.md`.
- Aucun rejeu effectué : l'application cible n'existe pas encore.
