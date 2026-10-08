# Proposition de changement de sprint — dette relevée par arch-eval

- **Date :** 2026-10-08
- **Déclencheur :** rapport arch-eval du 2026-10-06 sur le commit `d23bf7d` (tag `v0-demo`)
- **Auteur :** passe `bmad-correct-course`
- **Statut :** approuvée et appliquée le 2026-10-08

## 0. Décisions rendues

Le propriétaire a tranché les trois arbitrages le 2026-10-08, tous dans le sens du maintien des
décisions d'architecture existantes :

| Arbitrage | Décision | Report |
|---|---|---|
| Module technique partagé (n° 5) | **Écarté**, AD-17 maintenue | Note d'écart inscrite dans AD-17 |
| Verrous multi-instance (n° 6) | **Écarté**, AD-21 maintenue | Note d'écart inscrite dans AD-21 |
| Émettre et vérifier `iss`/`aud` (n° 4) | **Écarté**, contrat du jeton inchangé | Note d'écart inscrite dans AD-16 |

**L'epic 0 se réduit donc aux stories 0.1 et 0.2.** La story 0.3 n'est pas rédigée. Les trois écarts
sont inscrits dans la spine et dans l'introduction de l'epic 0, pour qu'un prochain passage
d'arch-eval les retrouve documentés au lieu de les resignaler comme de la dette.

## 1. Le problème

arch-eval a noté l'application **4,02/5** sur 29 critères et recommande de traiter trois chantiers
**avant les prochains epics**, au motif qu'ils s'aggravent à chaque story ajoutée :

1. **Front, jeton** — 7 à 9 cas d'usage reçoivent et traitent eux-mêmes le jeton alors que
   `fetchAuthentifie` le pose aussi. C'est la cause du bug 401 corrigé en `d23bf7d`, passé au travers
   de 99,5 % de couverture.
2. **Back, socle recopié et multi-instance** — `ConfigurationSecurite`, `GestionErreurs`,
   `HorlogeSysteme`, `ResolveurAdresseIp` et le relais d'outbox sont dupliqués dans les 7 services
   (6,3 % de duplication inter-modules mesurée par jscpd) ; le relais d'outbox n'a pas de verrou de
   ligne et les tâches `@Scheduled` pas de verrou.
3. **Types primitifs et sécurité** — montants et identifiants en primitives, fabriques de `Virement`
   à 8-11 paramètres, rôle `ADMIN` vérifié à la main dans chaque cas d'usage, `iss`/`aud` non
   contrôlés, pas de révocation ni de limitation des tentatives.

**Le constat de cette passe : deux de ces recommandations visent des décisions prises exprès.**
arch-eval juge le code sans lire la spine ni les arbitrages. Ce qu'il lit comme de la dette est,
pour une partie, un choix enregistré. La proposition sépare donc ce qui est une vraie correction de
ce qui demande de revenir sur une décision.

## 2. Analyse d'impact

### PRD — aucun impact

Vérifié sur l'ensemble du PRD : aucune exigence fonctionnelle ou non fonctionnelle n'est touchée.
Il n'existe aucune NFR de multi-instance, de disponibilité ou de mise à l'échelle. `NFR-SEC-03`
(contrôle d'accès par rôle appliqué dans les services) est **déjà tenue** — la centraliser est un
refactoring, pas un changement d'exigence. **Le changement est technique, pas fonctionnel.**

### Collisions avec des décisions enregistrées

| Recommandation arch-eval | Décision existante | Nature |
|---|---|---|
| Extraire un module technique partagé | **AD-17** : « pas de bibliothèque partagée entre services » | Renversement |
| Verrouiller outbox et `@Scheduled` | **AD-21** : « une seule instance par service, aucun verrou distribué » + section *Deferred* : scalabilité et haute disponibilité reportées | Renversement |
| Révocation et limitation des tentatives | **RP-05** (tranché le 2026-10-05) : « pas de verrouillage du compte ni de politique de mot de passe nouvelle ; l'alerte `MULTIPLE_FAILED_AUTH` couvre les échecs répétés » | Contradiction |
| Vérifier `iss`/`aud` | **AD-16** : revendications `sub`, `roles`, `jti`, `iat`, `exp` — `iss` et `aud` ne sont pas émis | Modification du contrat du jeton |

### Décisions à préciser, sans renversement

- **AD-16** et **Design Paradigm** disent où le jeton est *stocké* (`src/adapters`), pas qui l'envoie.
  C'est ce vide qui a laissé passer le bug. Un AR est à créer.
- **AD-23** appuie déjà le chantier 1 : l'exception d'horloge du renouvellement est située
  « dans `src/adapters` ».
- Les objets-valeurs du domaine sont **déjà le patron retenu** (`Iban` masque dans `toString()`,
  la configuration est validée par des objets-valeurs). `Montant`, `CompteId` et `Motif` sont dans
  la continuité, aucune rupture.

### Contraintes dures si le module partagé est retenu

- `domain` et `application` ne peuvent dépendre que de `java.*` **et du code du service**
  (`docs/quality-gates.md`). Un module partagé n'est donc consommable que depuis `adapter.*` et
  `infrastructure.*`.
- **`HorlogeSysteme` est le cas douloureux** : le port `application.port.out.Horloge` reste propre à
  chaque service, et `HorlogeArchitectureTest` exempte `..adapter.out.horloge..` **par service**.
  Sortir l'implémentation du périmètre scanné **affaiblit la preuve d'AD-19**.
- **AD-14** (« un module par service sous `services/<nom>/` ») et le *Structural Seed* ne prévoient
  aucun emplacement pour un module non-service.
- **AD-7** (seuils JaCoCo par couche) et **AD-1** (`*ArchitectureTest` dans chaque service) ne
  couvrent pas un module technique partagé : seuil et contrôle à décider, **jamais à exclure**.

### Ce qui amortit déjà le risque du multi-instance

**AD-18** impose `evenements_traites` (consommateurs idempotents) et **AD-22** un `versionAgregat`
croissant qui « neutralise un ancien message republié par le relais ». Un double envoi par le relais
est donc déjà absorbé côté consommateur. Le verrou apporte peu tant qu'on reste mono-instance.

## 3. Approche recommandée

**Ajustement direct** pour ce qui est conforme à l'architecture, **arbitrage** pour le reste.

| # | Chantier | Recommandation |
|---|---|---|
| 1 | Jeton confiné dans `fetchAuthentifie` | **Retenir.** Conforme à l'intention de la spine, corrige une classe de bug avérée. |
| 2 | Types métier et objet de demande `Virement` | **Retenir.** Patron déjà adopté, aucune décision contredite. |
| 3 | Centraliser le contrôle des rôles *dans* chaque service | **Retenir.** Conforme à AD-16. Entre services, retombe sur AD-17. |
| 4 | Émettre puis vérifier `iss`/`aud` | **Arbitrage.** Petit, mais modifie le contrat du jeton (AD-16). |
| 5 | Module technique partagé | **Arbitrage.** Renverse AD-17. |
| 6 | Verrous outbox et `@Scheduled` | **Arbitrage, recommandation : écarter.** Renverse AD-21 et la section *Deferred* ; le risque est déjà amorti par AD-18 et AD-22 ; le démonstrateur est mono-instance. |
| 7 | Révocation et limitation des tentatives | **Écarter.** Contredit RP-05, tranché le 2026-10-05. |

### Placement : un epic 0

Les stories retenues forment un **epic 0 « Redressement d'architecture »**, placé avant l'epic 4.

Motifs : l'epic 1 est le précédent d'un epic purement technique (FR = 0, scénarios « aucun ») ;
l'epic 1 est fermé par **AR-1** (« six stories exactement, pas de story 1.7 ») ; rattacher ailleurs
fausserait la colonne FR du tableau des epics. Le numéro **0** plutôt que 11 parce que l'epic 10 est
la recette finale : un epic 11 se lirait comme de la dette traitée *après* la bascule, l'inverse de
la décision prise. `bmad-loop run --epic 0` porte alors le séquencement sans commentaire.

## 4. Changements proposés

### 4.1 Nouvelles stories (`epics.md`)

#### Story 0.1 : Confiner le jeton dans le décorateur fetch du front

> En tant que développeur de l'application,
> je veux que le jeton d'authentification ne soit porté que par le décorateur `fetchAuthentifie`,
> afin qu'un cas d'usage ne puisse plus oublier de l'envoyer.

**Exigences :** NFR-SEC-03, NFR-SEC-07, AR-33, AR-34, AR-106, AD-16, AD-23
**Scénarios golden master :** aucun
**Prérequis :** epics 2 et 3 terminés ; correctif `d23bf7d` en place.

Critères : aucun port, cas d'usage ou signature de `frontend/src/application` et
`frontend/src/domain` n'accepte, ne lit ni ne transporte le jeton ; `fetchAuthentifie.ts` est le seul
endroit qui pose l'en-tête `Authorization` ; la composition de `main.tsx` est couverte par un test de
bout en bout qui échoue si un client HTTP est câblé sans le décorateur ; `build.sh`,
`check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh` sortent en code 0.

#### Story 0.2 : Introduire les types métier du virement et centraliser le contrôle des rôles

> En tant que développeur de l'application,
> je veux des types `Montant`, `CompteId` et `Motif` et un objet de demande pour `Virement`,
> afin que les règles de montant et d'identifiant soient portées par le domaine et non répétées.

**Exigences :** NFR-SEC-03, AR-34, AR-50, AR-66, FR-027-16
**Scénarios golden master :** aucun (comportement inchangé)
**Prérequis :** epic 3 terminé.

Critères : `Montant` encapsule un `BigDecimal` à 2 décimales et reste représenté `"100.00"` en JSON ;
les fabriques de `Virement` passent par un objet de demande ; le contrôle du rôle `ADMIN` est appelé
depuis un point unique **par service** (AD-16 maintenu : chaque service vérifie lui-même) ; aucun
scénario du golden master ne change de sortie ; seuils de couverture et de mutation inchangés.

#### Story 0.3 : Extraire le socle technique partagé entre les services

**Conditionnelle à l'arbitrage n° 5.** Non rédigée tant que AD-17 n'est pas tranchée.

### 4.2 Modifications de la spine

| Décision | Changement |
|---|---|
| **AD-16** | Préciser que le jeton n'est porté que par le décorateur de `fetch` et ne traverse aucun port applicatif. Si l'arbitrage n° 4 est retenu : ajouter `iss` et `aud` aux revendications émises et à la vérification. |
| **Design Paradigm** | Préciser que `src/application` ne reçoit jamais le jeton. |
| **AD-17** | Inchangée si l'arbitrage n° 5 est écarté. Sinon : réécrire, et traiter le cas `HorlogeSysteme` sans affaiblir AD-19. |
| **AD-21** et *Deferred* | Inchangées si l'arbitrage n° 6 est écarté, en inscrivant que le risque est couvert par AD-18 et AD-22. |

### 4.3 Note sur la duplication

Aucun seuil de duplication n'existe dans `docs/quality-gates.md` et jscpd n'y figure pas. Les
contrôles Sonar analysent module par module, ce qui rend la duplication inter-modules invisible.
**En ajouter un suppose de modifier `docs/quality-gates.md`, interdit aux agents.** C'est une
décision humaine, hors de cette proposition.

## 5. Passation

**Portée : modérée**, et **majeure** si l'arbitrage n° 5 ou n° 6 est retenu, puisqu'il faudrait
alors revenir sur une décision d'architecture enregistrée.

- Arbitrages n° 4, 5 et 6 : **propriétaire**.
- Rédaction des stories 0.1 et 0.2 dans `epics.md`, mise à jour de la spine, régénération de
  `sprint-status.yaml` par `bmad-sprint-planning` : une fois les arbitrages rendus.
- Développement : `bmad-loop run --epic 0`, un seul flux sur `main`, Sonnet 5.5.

**Critère de réussite :** un nouveau passage d'arch-eval sur l'epic 0 terminé relève les critères
1.2 et 1.3 (aujourd'hui à 3) sans faire baisser aucun autre critère, et `scripts/gates/build.sh`,
`check-gates-exist.sh`, `golden-master.sh` et `trace-smoke.sh` restent verts.
