# INDEX — carte du corpus de rétro-ingénierie

Le corpus décrit le comportement de l'application existante. Pour toute question sur ce comportement, ouvrir le fichier indiqué ci-dessous plutôt que répondre de mémoire, et citer le fichier lu. Chemins relatifs à ce dossier.

## Vue d'ensemble

| Fichier | Contenu |
|---|---|
| `GAPS.md` | manques connus du corpus, avec leur niveau (bloquant / important) |
| `MANIFEST.json` | liste des fichiers exportés (empreinte sha256) et commit de l'application analysée |
| `golden-master/` | golden master : scénarios de référence enregistrés sur l'application existante, rejouables sur la cible |
| `rules-index.json` | index machine de toutes les règles de gestion (id, fonctionnalité, statut, nature) |
| `screens/navigation-flow.md` | carte de navigation entre les écrans |
| `tools/` | outils autonomes : contrôle de couverture du PRD, consignes des relecteurs |

## 00-global/

| Fichier | Contenu |
|---|---|
| `00-global/API.md` | endpoints REST et soumissions de formulaires |
| `00-global/ARCHITECTURE.md` | architecture de l'application existante |
| `00-global/BATCH.md` | traitements planifiés (JOB-NNN) : planification, description, effets |
| `00-global/CONCERNS.md` | dettes et points d'attention techniques |
| `00-global/CONFIG.md` | paramètres d'environnement, constantes métier codées en dur |
| `00-global/CONVENTIONS.md` | conventions de code de l'application existante |
| `00-global/DATA-DICTIONARY.md` | tables, colonnes, types, contraintes de la base |
| `00-global/DOCUMENTS.md` | documents produits (DOC-NNN) : relevés, attestations, PDF |
| `00-global/ENUMS.md` | valeurs des énumérations (ENUM-table-colonne) |
| `00-global/FEATURES.md` | inventaire des fonctionnalités (FEAT-NNN) : nom, interface (Écran / API / Batch / Transversal), statut, point d'entrée, résumé |
| `00-global/INTEGRATIONS-OVERVIEW.md` | vue d'ensemble des intégrations |
| `00-global/INTEGRATIONS.md` | systèmes externes (INT-NNN) : protocole, données échangées, erreurs |
| `00-global/LEGACY-BUGS.md` | registre des bugs de l'application existante (BUG-NNN) et décision métier |
| `00-global/MESSAGES.md` | catalogue des messages affichés (MSG-…) : texte exact, déclencheur, fonctionnalité |
| `00-global/NFR.md` | volumétrie, temps de réponse, plages des batchs, disponibilité, sécurité (mesurés sur l'application existante) |
| `00-global/PERMISSIONS.md` | authentification, grille rôle × fonctionnalité, règles de niveau ligne, piste d'audit |
| `00-global/STACK.md` | pile technique de l'application existante |
| `00-global/TESTING.md` | tests existants de l'application existante |
| `00-global/UI-KIT.md` | charte et composants d'interface de l'application existante |
| `00-global/dead-features.json` | fonctionnalités mortes confirmées par le métier (à ne pas refaire) |
| `00-global/documents/` | — |
| `00-global/legacy-bugs.json` | registre des bugs, forme machine |
| `00-global/openapi.yaml` | contrat OpenAPI des endpoints observés |

## features/FEAT-NNN/ (un dossier par fonctionnalité)

| Fichier | Contenu |
|---|---|
| `RG.md` | règles de gestion (RG-NNN-NNN, EARS, statut Confirmé / À confirmer / Suspect) |
| `FIELDS.md` | champs des écrans : type, libellé, obligatoire, format attendu, comportement par rôle |
| `VALIDATION.md` | validations front / back (VAL-…) et messages d'erreur |
| `MESSAGES.md` | messages affichés par cette fonctionnalité |
| `FLOW.md` | parcours et transitions d'écran |
| `STATES.md` | états de l'écran (vide, chargé, erreur, confirmation…) |
| `scenarios.feature` | scénarios Gherkin |
| `test-harness.json` | jeu de données et préconditions des scénarios |
| `divergence-matrix.json` | écarts observés entre le code et l'application en fonctionnement |
| `screens/` | captures d'écran SCR-NNN-NN |

Liste des fonctionnalités : `00-global/FEATURES.md`.
