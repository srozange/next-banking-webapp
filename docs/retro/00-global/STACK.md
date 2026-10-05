# STACK

## Intention

Aucun README, ADR, ARCHITECTURE ni CHANGELOG trouvé dans le projet. Aucune documentation formelle ne décrit la pile cible.

## Réalité

Composant unique : **legacy**

| Dimension | Valeur |
|---|---|
| Langage principal | Java (90 fichiers, 10 101 LOC) |
| Vue | JSF 2.3 (Facelets, 16 vues `.xhtml`) |
| Services métier | EJB 3.2 Stateless |
| Persistance | JPA 2.2 (javax.persistence), JPQL |
| Sécurité | JAAS + `@RolesAllowed` (rôles : ADMIN, CONSEILLER, CLIENT) |
| Messagerie | JMS via ActiveMQ Artemis embarqué dans WildFly |
| Serveur d'application | WildFly 14 (déployé par cargo-maven3-plugin) |
| Base de données | H2 fichier (`/tmp/bankdb`) |
| Outil de build | Maven 3 (packaging WAR) |
| API Java EE | `javax.javaee-api:8.0` |
| Génération PDF | OpenPDF 1.3.30 |
| Logging | Log4j2 2.14.1 |
| Tests | JUnit Jupiter 5.10.2 |

**Locale découverte dans le code :**
- Langue : français (libellés UI, messages, commentaires)
- Format date : `dd/MM/yyyy` — preuve :
- Format nombre : virgule décimale (ex : 100,00) — preuve :
- Devise : EUR — preuve : (`currencySymbol="EUR"`)

**Intégrations externes (pile logicielle additionnelle) :**
- Powens (agrégation bancaire open banking) — HTTP via `HttpURLConnection`
- AlphaVantage (cotations boursières) — HTTP via `HttpURLConnection`

## Écarts

- Aucune documentation de pile ne permet de comparer intention et réalité.
- La pile utilise Java EE 8 (`javax.*`) bien que la version WildFly soit 14 (qui supporte Java EE 7 ; Java EE 8 est assuré par la dépendance déclarée explicitement).
- Absence de tout outil de conteneurisation ou CI/CD dans le dépôt.
