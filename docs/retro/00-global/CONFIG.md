# CONFIG — Application bancaire legacy

_Généré lors de l'étape retro-config. Sources : `AppConfig.java`, `web.xml`, `standalone-full.xml`, `NotificationService.java`, `LoanBean.java`._

---

## 1. Paramètres d'environnement (System.getenv / System.getProperty)

Ordre de résolution : variable d'environnement → propriété système JVM → valeur par défaut.  
Chargement unique au démarrage de l'application (singleton statique `@Vetoed`).

---

### PARAM-001 : ALPHA_VANTAGE_API_KEY
| Attribut | Valeur |
|---|---|
| Clé | `ALPHA_VANTAGE_API_KEY` |
| Valeur par défaut | *(démo — ne pas utiliser en production)* |
| Type | chaîne (clé API) |
| Sensible | oui |
| Effet | Clé d'authentification vers l'API AlphaVantage pour les cotations boursières en temps réel. Sans clé valide, les cotations renvoient une erreur JSON. |
| Features | FEAT-009 (Bourse et watchlist) |

---

### PARAM-002 : ALPHA_VANTAGE_BASE_URL
| Attribut | Valeur |
|---|---|
| Clé | `ALPHA_VANTAGE_BASE_URL` |
| Valeur par défaut | `https://www.alphavantage.co/query` |
| Type | chaîne (URL) |
| Sensible | non |
| Effet | URL de base pour les appels REST AlphaVantage. Surchargeable pour pointer vers un stub de test. |
| Features | FEAT-009 |

---

### PARAM-003 : AZURE_OPENAI_ENDPOINT
| Attribut | Valeur |
|---|---|
| Clé | `AZURE_OPENAI_ENDPOINT` |
| Valeur par défaut | *(endpoint démo — non fonctionnel hors environnement Accenture)* |
| Type | chaîne (URL) |
| Sensible | non |
| Effet | Endpoint Azure Cognitive Services pour le chat IA du conseiller. |
| Features | FEAT-010 (Chat avec le conseiller) |

---

### PARAM-004 : AZURE_OPENAI_DEPLOYMENT
| Attribut | Valeur |
|---|---|
| Clé | `AZURE_OPENAI_DEPLOYMENT` |
| Valeur par défaut | `gpt-5.4-nano` |
| Type | chaîne |
| Sensible | non |
| Effet | Nom du déploiement Azure OpenAI (modèle). Changer pour basculer entre versions du modèle. |
| Features | FEAT-010 |

---

### PARAM-005 : AZURE_OPENAI_API_VERSION
| Attribut | Valeur |
|---|---|
| Clé | `AZURE_OPENAI_API_VERSION` |
| Valeur par défaut | `2024-12-01-preview` |
| Type | chaîne |
| Sensible | non |
| Effet | Version de l'API Azure OpenAI utilisée dans les requêtes HTTP. |
| Features | FEAT-010 |

---

### PARAM-006 : AZURE_OPENAI_API_KEY
| Attribut | Valeur |
|---|---|
| Clé | `AZURE_OPENAI_API_KEY` |
| Valeur par défaut | *(clé démo)* |
| Type | chaîne (clé API) |
| Sensible | oui |
| Effet | Clé d'authentification vers Azure OpenAI. En local sans clé valide : HTTP 401, message d'erreur affiché. |
| Features | FEAT-010 |

---

### PARAM-007 : POWENS_DOMAIN
| Attribut | Valeur |
|---|---|
| Clé | `POWENS_DOMAIN` |
| Valeur par défaut | `demi-sandbox.biapi.pro` |
| Type | chaîne (domaine) |
| Sensible | non |
| Effet | Domaine Powens utilisé pour construire `POWENS_BASE_URL`. En prod : remplacer par le domaine production Powens. |
| Features | FEAT-012 (Comptes tiers — Open Banking), FEAT-014 (Callback OAuth Powens) |

---

### PARAM-008 : POWENS_CLIENT_ID
| Attribut | Valeur |
|---|---|
| Clé | `POWENS_CLIENT_ID` |
| Valeur par défaut | *(identifiant sandbox démo)* |
| Type | chaîne |
| Sensible | non |
| Effet | Identifiant client OAuth Powens. Utilisé dans les appels d'agrégation bancaire. |
| Features | FEAT-012, FEAT-014 |

---

### PARAM-009 : POWENS_CLIENT_SECRET
| Attribut | Valeur |
|---|---|
| Clé | `POWENS_CLIENT_SECRET` |
| Valeur par défaut | *(secret sandbox démo)* |
| Type | chaîne (secret OAuth) |
| Sensible | oui |
| Effet | Secret OAuth Powens pour obtenir les tokens d'accès. |
| Features | FEAT-012, FEAT-014 |

---

### PARAM-010 : POWENS_REDIRECT_URI
| Attribut | Valeur |
|---|---|
| Clé | `POWENS_REDIRECT_URI` |
| Valeur par défaut | `http://localhost:8080/banking-legacy/powens-callback.xhtml` |
| Type | chaîne (URL) |
| Sensible | non |
| Effet | URI de redirection OAuth après autorisation Powens. Doit correspondre exactement à l'URL enregistrée sur le portail Powens. |
| Features | FEAT-012, FEAT-014 |

---

### PARAM-011 : POWENS_CONNECTOR_ID
| Attribut | Valeur |
|---|---|
| Clé | `POWENS_CONNECTOR_ID` |
| Valeur par défaut | `59` |
| Type | chaîne (identifiant numérique) |
| Sensible | non |
| Effet | Identifiant du connecteur bancaire Powens utilisé pour l'agrégation (détermine la banque partenaire). |
| Features | FEAT-012 |

---

### PARAM-012 : SEUIL_PENDING_EUR
| Attribut | Valeur |
|---|---|
| Clé | `SEUIL_PENDING_EUR` |
| Valeur par défaut | `5000` |
| Type | numérique (EUR) |
| Sensible | non |
| Effet | Montant en EUR au-delà duquel un virement est placé en statut PENDING au lieu d'être exécuté immédiatement. |
| Features | FEAT-002 (Virements), FEAT-023 (Traitement JMS) |

---

### PARAM-013 : SEUIL_ALERTE_EUR
| Attribut | Valeur |
|---|---|
| Clé | `SEUIL_ALERTE_EUR` |
| Valeur par défaut | `5000` |
| Type | numérique (EUR) |
| Sensible | non |
| Effet | Seuil bas de l'intervalle d'alerte : `SEUIL_ALERTE_EUR ≤ montant < SEUIL_FRAUDE_EUR` → notification de type ALERTE. |
| Features | FEAT-023 |

---

### PARAM-014 : SEUIL_FRAUDE_EUR
| Attribut | Valeur |
|---|---|
| Clé | `SEUIL_FRAUDE_EUR` |
| Valeur par défaut | `10000` |
| Type | numérique (EUR) |
| Sensible | non |
| Effet | Seuil de fraude : `montant ≥ SEUIL_FRAUDE_EUR` → notification de type FRAUDE + alerte fraude (FEAT-016 gestion des alertes). |
| Features | FEAT-023, FEAT-016 (Administration) |

---

### PARAM-015 : MAX_NOTIFS_PAR_USER
| Attribut | Valeur |
|---|---|
| Clé | `MAX_NOTIFS_PAR_USER` |
| Valeur par défaut | `20` |
| Type | entier |
| Sensible | non |
| Effet | Nombre maximal de notifications conservées par utilisateur en mémoire (LIFO). Au-delà, la plus ancienne est supprimée. |
| Features | FEAT-006 (Notifications), FEAT-023 |

---

## 2. Paramètres JNDI / WildFly (standalone-full.xml)

Ces paramètres ne sont pas surchargés par variable d'environnement : ils sont fixés dans la configuration WildFly.

| Paramètre | Valeur | Description |
|---|---|---|
| Datasource JNDI | `java:jboss/datasources/ExampleDS` | Source de données H2 in-memory (développement/démo uniquement) |
| JMS Factory | `java:jboss/DefaultJMSConnectionFactory` | Factory JMS par défaut (HornetQ/ActiveMQ Artemis embarqué) |
| Queue virements | `java:/queue/virements` | File JMS des virements à traiter (FEAT-023) |
| Queue prêts | `java:/queue/pretDemandesQueue` | File JMS des demandes de prêt |
| DLQ | `jms.queue.DLQ` | File morte pour les messages non traités (non fonctionnelle — bug dette-legacy FEAT-023) |
| ExpiryQueue | `jms.queue.ExpiryQueue` | File d'expiration |
| Session timeout | `30 min` | Timeout de session JSF |
| JSF State Saving | `client` | État ViewState côté client (évite ViewExpiredException) |
| JSF Project Stage | `Development` | Mode développement (traces détaillées) — à changer en `Production` |

---

## 3. Constantes métier hardcodées (non configurables par env)

Ces valeurs sont figées dans le code source et ne peuvent être modifiées sans recompilation.

| Constante | Valeur | Fichier:ligne | Effet | Features |
|---|---|---|---|---|
| Seuil solde bas | 100 EUR | | Seuil en dessous duquel une notification ACCOUNT_BALANCE est émise toutes les 30 min | FEAT-006, FEAT-004 |
| Max notifs/jour | 20 | | Limite journalière d'envoi de notifications de type `ACCOUNT_BALANCE` (déduplication) | FEAT-006 |
| Taux annuel défaut (simulation) | 3,5 % | | Taux pré-rempli dans les formulaires de simulation et de demande de prêt | FEAT-007 (Prêts) |
| Durée défaut (simulation) | 36 mois | | Durée pré-remplie dans les formulaires de simulation et de demande | FEAT-007 |
| Durée maximale prêt | 360 mois | | Durée maximale acceptée par le formulaire (30 ans) | FEAT-007 |
| Taux annuel maximum | 30 % | | Limite haute de taux rejetée par LoanService (validation back) | FEAT-007 |
| Seuil notification grand virement | 1 000 EUR | | Seuil au-delà duquel une notification de virement reçu est émise au destinataire | FEAT-002, FEAT-006 |

---

## 4. Notes de réécriture

- Les paramètres PARAM-001 à PARAM-015 doivent être externalisés dans Azure App Configuration ou des secrets Azure Key Vault dans la cible.
- Les constantes hardcodées (§3) doivent devenir des paramètres de configuration dans la réécriture — en particulier le seuil solde bas (100 EUR) et la durée max prêt (360 mois).
- `JSF Project Stage = Development` → à remplacer par `Production` dans la cible.
- Le datasource H2 in-memory est uniquement pour la démo locale — la cible utilise PostgreSQL Azure.
