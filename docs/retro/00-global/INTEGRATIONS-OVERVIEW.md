# INTEGRATIONS-OVERVIEW

## Intention

Aucune documentation des intégrations externes présente dans le dépôt.

## Réalité

**CI/CD :** Aucun pipeline CI/CD détecté (ni `.github/workflows`, ni Jenkinsfile, ni `.gitlab-ci.yml`).

**Conteneurs :** Aucun Dockerfile ni `docker-compose.yml` détecté dans le projet.

**Messagerie interne (ActiveMQ Artemis embarqué WildFly) :**

| Queue | Producteur | Consommateur | Usage |
|---|---|---|---|
| `java:/queue/virements` | `VirementProducer` (`mq/VirementProducer.java`) | `VirementProcessorMDB` (`mq/VirementProcessorMDB.java`) | Traitement asynchrone des virements |
| `java:/queue/pretDemandesQueue` | `LoanApprovalProducer` (`mq/LoanApprovalProducer.java`) | `LoanApprovalMDB` (`mq/LoanApprovalMDB.java`) | Approbation asynchrone de prêts |

**Intégrations externes HTTP :**

| Système | Classe | Protocole | Endpoint | Usage |
|---|---|---|---|---|
| Powens (open banking) | `powens/PowensService.java` | HTTPS / Basic Auth | `appConfig.getPowensBaseUrl()` — base URL configurable | Agrégation de comptes bancaires externes, transactions, webview OAuth |
| AlphaVantage (cotations) | `bourse/AlphaVantageService.java` | HTTPS | `appConfig.getAlphaVantageBaseUrl()` — base URL configurable | Données de marché boursier (cours, historiques) |

- [RÉSOLU] (dependance-externe) La configuration Powens (domaine, client_id, redirect_uri, connector_id) dépend de credentials externes inaccessibles ici : sont-ils fournis en variable d'environnement, en fichier de propriétés WildFly, ou en base de données de configuration ?
  Portée : information
  Résolution : Variables d'environnement WildFly — `POWENS_BASE_URL`, `POWENS_DOMAIN`, `POWENS_CLIENT_ID`, `POWENS_REDIRECT_URI`, `POWENS_CONNECTOR_ID` lues via `AppConfig.resolve()` (src/main/java/com/accenture/bank/config/AppConfig.java). Aucune valeur par défaut fonctionnelle ; sans credentials valides, la webview OAuth échoue et la page affiche l'état non connecté.

**Transport HTTP :** `HttpURLConnection` natif Java (pas de client HTTP tiers). Appels synchrones bloquants.

**Aucun appel de composant vers composant** : monolithe, pas d'échange inter-services (confirmé par `components-graph.json:edges = []`).

## Écarts

- Aucune documentation des intégrations ne permet de comparer intention et réalité.
- La clé API AlphaVantage est présente en fallback hardcodé dans le code source (`FALLBACK_API_KEY = "NH55CH0ZLQH8RFIH"`) — preuve : `bourse/AlphaVantageService.java` — risque de sécurité (JIRA-9300).
- Aucun mécanisme de retry ni circuit-breaker sur les appels HTTP externes.
- Pas de timeout explicite configuré sur `HttpURLConnection`.
