# TESTING

## Intention

Aucune documentation de stratégie de tests présente dans le dépôt.

## Réalité

**Répertoires de tests :**

```
src/test/java/com/accenture/bank/util/
  SepaValidatorTest.java    ← 1 fichier de test unitaire
```

**Framework :** JUnit Jupiter 5.10.2 (déclaré dans `pom.xml`, runner : `maven-surefire-plugin 3.2.5`)

**Couverture approximative :**

- 1 fichier de test pour 90 fichiers Java source → ratio ≈ 1 %
- Seule classe testée : `SepaValidator` (utilitaire de validation d'IBAN)
- Zéro test pour : services EJB, backing beans JSF, entités JPA, producteurs JMS, MDBs, intégrations externes

**Tests d'intégration / end-to-end :**

- Aucun test d'intégration dans le projet legacy lui-même
- Des tests Playwright golden master existent dans le répertoire parent du projet (`../tests/banking-legacy.spec.ts`) — hors périmètre de ce composant

## Écarts

- Aucune documentation de tests ne permet de comparer intention et réalité.
- Couverture de tests extrêmement faible (≈ 1 %) : la quasi-totalité de la logique métier (virements, fraude, prêts, notifications) est non testée unitairement.
- Aucun mock ni stub pour les appels externes (Powens, AlphaVantage) : les tests unitaires éventuels dépendraient de services tiers réels.
- Pas de profil Maven dédié `test` ni de base de données H2 in-memory configurée pour les tests.
