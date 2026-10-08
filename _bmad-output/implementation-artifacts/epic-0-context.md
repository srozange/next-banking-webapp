# Epic 0 Context: Redressement d'architecture

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Corriger deux faiblesses d'architecture relevées par arch-eval sur le tag `v0-demo` avant que les epics suivants (à partir de l'epic 4) ne les amplifient : le jeton d'authentification porté par trop de couches du front (cause d'un bug 401 passé au travers de la couverture), et des types métier absents côté back (montants, identifiants, motif en primitives, fabriques de `Virement` à 8-11 paramètres, contrôle du rôle `ADMIN` répété). Epic purement technique : aucune exigence fonctionnelle, aucun écran, aucun point d'API, aucun scénario du golden master ne change. Exécuté sur `main`, un seul flux.

## Stories

- Story 0.1: Confiner le jeton dans le décorateur fetch du front
- Story 0.2: Porter les montants et les identifiants par des types du domaine

## Requirements & Constraints

- Comportement observable inchangé : aucun scénario du golden master ne change de sortie ; montants toujours en chaîne `"100.00"` en JSON, colonne `numeric(19,2)`.
- Le contrôle d'accès par rôle reste tenu par chaque service (exigence déjà satisfaite ; la centralisation est un refactoring).
- Fin d'epic : `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh` sortent en code 0. Seuils de couverture et de mutation inchangés, aucune exclusion ajoutée (100 % sur `domain` et `application` côté back).
- Le diff de la story ne doit toucher aucun fichier interdit (scripts/gates, policy, empreinte, `.claude`, `docs/quality-gates.md`, config dependency-cruiser, suppressions CVE, golden master et dérivés).
- Critère de réussite : un nouveau passage d'arch-eval relève les critères 1.2 et 1.3 sans en faire baisser d'autre.
- Hors périmètre, écartés en connaissance de cause, à ne pas rouvrir par une story : module technique partagé entre services, verrous multi-instance (outbox, `@Scheduled`), `iss`/`aud` dans le jeton, révocation du jeton et limitation des tentatives. Pas de seuil de duplication inter-modules (modifierait `docs/quality-gates.md`, décision humaine).

## Technical Decisions

- Front : le jeton n'est porté que par le décorateur `frontend/src/adapters/session/fetchAuthentifie.ts`, seul endroit posant `Authorization: Bearer`. Rien dans `src/domain` ni `src/application` (types, ports, cas d'usage, signatures) ne l'accepte, ne le lit ni ne le transporte ; pas de mention de `Authorization`, `Bearer`, `nb.jeton` dans ces dossiers. Le jeton reste en `localStorage` (`nb.jeton`). La composition de `frontend/src/infrastructure/main.tsx` doit donner `fetchAuthentifie` à tout client HTTP d'un service authentifié, avec un test de composition qui échoue (en nommant le client) sinon.
- Un seul `frontend/vite.config.ts` pour Vitest ; jamais de `vitest.config.*`. Vérifications front : `cd frontend && npx vitest run --coverage` et `npx tsc --noEmit`.
- Back : les objets-valeurs du domaine sont le patron déjà retenu (comme `Iban`, masqué dans `toString()`). `Montant` encapsule un `BigDecimal` à 2 décimales et refuse échelle ou signe invalides sans constructeur contournant ; `CompteId` enveloppe l'identifiant numérique échangé entre services (jamais le numéro de compte) ; `Motif` porte les règles du libellé. Les fabriques de `Virement` reçoivent un objet de demande (4 paramètres au plus).
- Contrôle du rôle `ADMIN` appelé depuis un point unique par service. Pas de bibliothèque partagée entre services : chaque service garde sa propre copie, la duplication est assumée, le garde-fou étant un test de référence commun.
- Contraintes de couches : `domain` et `application` ne dépendent que de `java.*` et du code du service, sans framework. Le temps passe uniquement par le port `Horloge` ; le jeton utilise l'horloge système dans `adapter.out.jeton` seulement.
- Build back : `mvn -B -q clean verify -DfailIfNoTests=true`. Code métier en français, paquets `fr.nextbanking.<identifiant>`.

## Cross-Story Dependencies

- Prérequis communs : epics 2 et 3 terminés, correctif `d23bf7d` en place (`fetchAuthentifie` pose déjà l'en-tête).
- Story 0.2 suppose la story 0.1 terminée.
- L'epic 0 s'exécute avant l'epic 4 ; il ne crée aucune dépendance fonctionnelle.
