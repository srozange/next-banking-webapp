# ARCHITECTURE

## Intention

Aucun document d'architecture (README, ADR, ARCHITECTURE.md) n'est présent dans le dépôt.

## Réalité

Application bancaire monolithique déployée en WAR sur WildFly 14. Elle combine présentation JSF, services EJB, persistance JPA, messagerie JMS et deux intégrations externes HTTP.

**Structure des répertoires :**

```
src/main/java/com/accenture/bank/
  web/          ← Backing beans JSF (@Named + scopes CDI)
  ejb/          ← Services métier EJB 3.2 (@Stateless)
  entity/       ← Entités JPA (@Entity)
  mq/           ← Producteurs JMS et MDBs (@MessageDriven)
  powens/       ← Intégration Powens open banking
  bourse/       ← Intégration AlphaVantage cotations
  conseiller/   ← Module conseiller (chat, service)
  config/       ← Configuration applicative
  util/         ← Utilitaires (SepaValidator…)
src/main/webapp/
  *.xhtml       ← 16 vues JSF Facelets
  WEB-INF/      ← web.xml, faces-config.xml, beans.xml, bankds-ds.xml, jboss-web.xml
src/main/resources/
  import.sql    ← Données initiales H2 (430 LOC)
  log4j2.xml    ← Configuration logging
src/main/wildfly/config/
  standalone-full.xml ← Configuration WildFly (queues JMS incluses)
```

**Couches identifiables :**

| Couche | Technologie | Rôle |
|---|---|---|
| Présentation | JSF 2.3 Facelets + backing beans `@Named` | Rendu HTML, navigation, validation UI |
| Service | EJB 3.2 `@Stateless` | Logique métier, transactions JTA |
| Persistance | JPA 2.2, JPQL | Accès données H2 |
| Messagerie | JMS / ActiveMQ Artemis | Virements asynchrones, approbation de prêts |
| Sécurité | JAAS (`@RolesAllowed`) | Contrôle d'accès par rôle |
| Intégrations | HTTP `HttpURLConnection` | Powens, AlphaVantage |

**Injection de dépendances :** champ injection via `@EJB` (backing beans → services EJB) et `@Resource` (JMS ConnectionFactory, Queue). Pas d'injection constructeur.

---

## Composants et échanges internes

Ce découpage décrit l'application existante telle qu'elle est. Il n'est ni une exigence ni une recommandation pour la nouvelle application, qui choisira son propre découpage.

**Composants :**

| Nom | Répertoire | Pile | Base | Rôle |
|---|---|---|---|---|
| legacy | `.` (racine) | JSF 2.3 / EJB 3.2 / JPA 2.2 / WildFly 14 | H2 `/tmp/bankdb` | Application bancaire complète : tableau de bord, virements, prêts, documents, fraude, notifications, conseiller, bourse |

Aucun échange interne : un seul composant.

## Écarts

- Aucune documentation d'architecture ne permet de comparer intention et réalité.
- La logique de validation métier est partiellement dans les backing beans (`VirementBean`) au lieu d'être exclusivement dans la couche EJB — signalé par deux TODO JIRA-9200 dans le code.
- Pas de couche API REST : toute la communication est UI-to-EJB directe via CDI.
