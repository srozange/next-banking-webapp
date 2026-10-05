# GAPS — manques connus du corpus

Marqueurs `[TODO]` retirés à l'export de la couche distillée. Chaque ligne est un manque connu du corpus, à combler ou à assumer.

| Fichier | Marqueur | Texte |
|---|---|---|
| 00-global/ARCHITECTURE.md | [TODO important] | Aucun document d'architecture (README, ADR, ARCHITECTURE.md) n'est présent dans le dépôt. |
| 00-global/BATCH.md | [TODO important] | > Fuseau horaire : celui de la JVM WildFly (non explicitement configuré dans le code — vérifier `standalone-full.xml`). |
| 00-global/BATCH.md | [TODO important] | \| **Volumétrie** \| nombre de comptes EPARGNE en production \| |
| 00-global/BATCH.md | [TODO important] | \| **Volumétrie** \| volume de transactions > 10 000 EUR / jour \| |
| 00-global/BATCH.md | [TODO important] | \| **Volumétrie** \| nombre de clients actifs en production \| |
| 00-global/BATCH.md | [TODO important] | \| **Volumétrie** \| dette : `SELECT ALL customers` sans pagination (JIRA-7200) \| |
| 00-global/BATCH.md | [TODO important] | \| **Volumétrie** \| nombre de virements programmés actifs \| |
| 00-global/CONCERNS.md | [TODO important] | Aucune documentation de dette technique ou de concerns connues présente dans le dépôt. |
| 00-global/CONVENTIONS.md | [TODO important] | Aucune documentation de conventions (style guide, coding standards) présente dans le dépôt. |
| 00-global/CONVENTIONS.md | [TODO important] | \| Tables/colonnes JPA \| Conventions non vérifiées ici ; voir `retro-data` \| |
| 00-global/ENUMS.md | [TODO important] | \| `COURANT` \| \| 4 \| |
| 00-global/ENUMS.md | [TODO important] | \| `EPARGNE` \| \| 3 \| |
| 00-global/ENUMS.md | [TODO important] | \| `CREDIT` \| \| 4 \| |
| 00-global/ENUMS.md | [TODO important] | \| `INVESTMENT` \| \| 5 \| |
| 00-global/ENUMS.md | [TODO important] | \| `SAVINGS` \| \| 4 \| |
| 00-global/ENUMS.md | [TODO important] | \| `ABONNEMENTS` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `ALIMENTATION` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `LOGEMENT` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `LOISIRS` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `SANTE` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `TRANSPORT` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `EXCELLENT` \| \| 2 \| |
| 00-global/ENUMS.md | [TODO important] | \| `GOOD` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `MEDIUM` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `ATTESTATION_SOLDE` \| \| 2 \| |
| 00-global/ENUMS.md | [TODO important] | \| `RELEVE_MENSUEL` \| \| 2 \| |
| 00-global/ENUMS.md | [TODO important] | \| `TABLEAU_AMORTISSEMENT` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `application/pdf` \| \| 5 \| |
| 00-global/ENUMS.md | [TODO important] | \| `CONFIRMED` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `FALSE_POSITIVE` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `OPEN` \| \| 3 \| |
| 00-global/ENUMS.md | [TODO important] | \| `REVIEWED` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `EN_COURS` \| \| 2 \| |
| 00-global/ENUMS.md | [TODO important] | \| `AUTO` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `TRAVAUX` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `ACCOUNT_BALANCE` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `FRAUD_ALERT` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `LOAN_STATUS` \| \| 2 \| |
| 00-global/ENUMS.md | [TODO important] | \| `SYSTEM` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `TRANSACTION_ALERT` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `FRAUD-VELOCITY` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `MAINT-20260601` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `EN_COURS` \| \| 1 \| |
| 00-global/ENUMS.md | [TODO important] | \| `CREDIT` \| \| 14 \| |
| 00-global/ENUMS.md | [TODO important] | \| `DEBIT` \| \| 34 \| |
| 00-global/ENUMS.md | [TODO important] | \| \| \| Valeurs non extraites : seconde passe generate-sql-introspection.py --intermediate \| |
| 00-global/ENUMS.md | [TODO important] | \| \| \| Valeurs non extraites : seconde passe generate-sql-introspection.py --intermediate \| |
| 00-global/FEATURES.md | [TODO important] | _Usage réel non disponible — logs d'accès absents._ |
| 00-global/INTEGRATIONS-OVERVIEW.md | [TODO important] | Aucune documentation des intégrations externes présente dans le dépôt. |
| 00-global/INTEGRATIONS.md | [TODO important] | \| **SLA** \| \| |
| 00-global/INTEGRATIONS.md | [TODO important] | \| **SLA** \| \| |
| 00-global/INTEGRATIONS.md | [TODO important] | \| **SLA** \| plan gratuit : 25 requêtes/jour, 5 req/min \| |
| 00-global/PERMISSIONS.md | [TODO confort] | \| Exigence légale \| exigence légale à traiter au cadrage de la nouvelle application (rien dans le code de l'existant) \| |
| 00-global/STACK.md | [TODO important] | Aucun README, ADR, ARCHITECTURE ni CHANGELOG trouvé dans le projet. Aucune documentation formelle ne décrit la pile cible. |
| 00-global/TESTING.md | [TODO important] | Aucune documentation de stratégie de tests présente dans le dépôt. |
| features/FEAT-004/divergence-matrix.json | [TODO LIVE] | back accepte ce que front refuse (appel direct non testé) |
| features/FEAT-007/divergence-matrix.json | [TODO LIVE] | back accepte ce que front refuse (appel direct non testé) |
| features/FEAT-009/divergence-matrix.json | [TODO LIVE] | front accepte ce que back rejette (aucune observation live de la réponse serveur) |
| features/FEAT-009/divergence-matrix.json | [TODO LIVE] | front accepte ce que back rejette (aucune observation live de la réponse serveur) |
| features/FEAT-009/divergence-matrix.json | [TODO LIVE] | front accepte ce que back rejette (aucune observation live de la réponse serveur) |
| features/FEAT-009/divergence-matrix.json | [TODO LIVE] | front accepte ce que back rejette (aucune observation live de la réponse serveur) |
| features/FEAT-013/divergence-matrix.json | [TODO LIVE] | back accepte ce que front refuse (appel direct non testé) |
| features/FEAT-013/divergence-matrix.json | [TODO LIVE] | back accepte ce que front refuse (appel direct non testé) |
