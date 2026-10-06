---
title: 'Raccorder Azure OpenAI derrière un port simulé et exposer l''échange avec Sophie'
type: 'feature'
created: '2026-10-06'
status: 'done'
baseline_revision: 'e0009ee7d0a0e09f3d5249042c5e6cb4fb82d372'
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/docs/retro/features/FEAT-010/RG.md'
  - '{project-root}/docs/retro/00-global/INTEGRATIONS.md'
warnings: []
deferred:
  - summary: >-
      trace-smoke.sh non exécuté pour la story 7.7.
    evidence: |-
      Le script exige la pile compose ; non lancé dans cette session.
    severity: medium (unverified)
---

<intent-contract>

## Intent

**Problem:** Le service open-banking-conseiller n'expose aucun échange avec Sophie : les questions ne partent pas vers le service d'IA, et une panne de ce service n'est pas annoncée sans code technique (BUG-038).

**Approach:** Un port sortant de conversation, avec un adaptateur simulé par défaut (`AzureOpenAiSimule`) et un adaptateur réel Azure OpenAI (Chat Completions, délais 15 s / 30 s, coupe-circuit Resilience4j comme en 7.2) ; `POST /api/open-banking-conseiller/chat/messages` sans état (historique fourni par l'appelant, aucune table).

## Boundaries & Constraints

**Always:** adaptateur simulé actif sans aucune propriété `nb.fournisseurs.*` ; jamais `mode=reel` dans un fichier versionné ; clé uniquement par `AZURE_OPENAI_API_KEY` (aucune valeur par défaut) ; endpoint, déploiement, version : variable d'environnement, puis propriété système, puis défaut du legacy ; domain/application en Java pur ; clients HTTP seulement dans `adapter.out.fournisseur` ; la clé n'est jamais journalisée ; textes d'erreur du legacy à l'identique ; consigne système = `conseiller.consigne` de `textes-nouveaux.md`.

**Never:** aucun historique de conversation enregistré ; pas de message de bienvenue dans le contexte envoyé ; aucun code 401/403 dans le message utilisateur ; pas de retry ; ne pas toucher à scripts/gates/**, au golden master ni à sprint-status.yaml.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Nominal simulé | message, historique vide | 200 `{"reponse": ...}` fixe, sans réseau | — |
| Contexte | 1er message puis 2e avec historique | bouchon : consigne + message ; puis consigne + échanges + message ; `max_tokens` 500, `temperature` 0,7 | — |
| Refus 401/403 (réel ou simulé) | clé refusée | 503 `Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence.` | pas de « 401 »/« 403 » |
| Autre statut non 2xx | bouchon 500 | 502 `Je suis momentanement indisponible (HTTP 500). Veuillez reessayer.` | corps du fournisseur non exposé |
| Délai / illisible / coupe-circuit ouvert / clé absente | — | 503 texte générique, sans pile ; clé absente : aucune requête émise | journal sans clé |
| Message vide | `message` blanc ou absent | 400 `Requête invalide` | — |

</intent-contract>

## Code Map

- `services/bourse/src/main/java/fr/nextbanking/bourse/adapter/out/fournisseur/{AlphaVantageCotations,ConfigurationAlphaVantage,ProprietesAlphaVantage,ParametresDeploiement}.java` -- modèle à reproduire (coupe-circuit, client à délais, mode simulé/réel, lecture env puis propriété système).
- `services/bourse/pom.xml` -- dépendance `resilience4j-circuitbreaker` (version déjà gérée au POM parent).
- `services/open-banking-conseiller/.../adapter/in/http/{PointEntreeProduits,GestionErreurs,Textes}.java` -- contrôleur, erreurs `{"message"}`, textes.
- `services/open-banking-conseiller/.../infrastructure/AssemblageCatalogue.java` -- assemblage du cas d'usage.
- `ClientsHttpArchitectureTest`, `HexagonalArchitectureTest` -- clients HTTP limités à `adapter.out.fournisseur`.
- `compose.yaml`, `.env.example` -- variables `AZURE_OPENAI_*` vides par défaut.
- `docs/retro/features/FEAT-010/RG.md` (RG-010-008 à 021), `textes-nouveaux.md` (`conseiller.consigne`).

## Tasks & Acceptance

**Execution:**
- `services/open-banking-conseiller/pom.xml` -- ajouter `resilience4j-circuitbreaker` -- coupe-circuit.
- `application/port/out/ConversationIa.java`, `application/ServiceIaIndisponible.java`, `domain/{Echange,RoleEchange,ConsigneSophie}.java` -- port, erreur typée (authentification / statut / indisponible), consigne.
- `application/port/in/Dialoguer.java`, `application/DialoguerCasUsage.java`, `infrastructure/AssemblageConversation.java` -- cas d'usage Java pur.
- `adapter/in/http/PointEntreeChat.java`, `GestionErreurs.java`, `Textes.java` -- endpoint POST et mapping 400/502/503.
- `adapter/out/fournisseur/{AzureOpenAiSimule,AzureOpenAiReel,ConfigurationAzureOpenAi,ProprietesAzureOpenAi,ParametresDeploiementAzure}.java` -- adaptateurs et réglages `nb.fournisseurs.azure-openai.*`.
- `compose.yaml`, `.env.example` -- variables vides.
- tests tagués FR-010-05 à FR-010-09 et BUG-038 -- bouchon local, simulé, configuration, consigne, absence de clé versionnée.

**Acceptance Criteria:**
- Given aucune propriété `nb.fournisseurs.*`, when le contexte démarre, then le port est `AzureOpenAiSimule`.
- Given les cas de la matrice, when j'appelle l'endpoint, then statuts et messages sont ceux listés.
- Given la story terminée, when je lance les tests par tag et `scripts/gates/build.sh`, `check-gates-exist.sh`, `golden-master.sh`, then code 0.

## Design Notes

Le rôle des échanges côté API : `utilisateur` / `assistant`, traduits en `user` / `assistant` pour Azure. Le refus simulé se règle par `nb.fournisseurs.azure-openai.refus-simule` (401 ou 403).

## Verification

**Commands:**
- `mvn -B -q -f services/open-banking-conseiller/pom.xml verify` -- expected: succès, JaCoCo respecté
- `scripts/gates/build.sh && scripts/gates/check-gates-exist.sh && scripts/gates/golden-master.sh` -- expected: code 0

## Review Triage Log

### 2026-10-06 — Review pass
- verdicts: 5 findings — high 0, medium 1, low 3, false 0, maybe-false 1
- findings:
  - `[medium]` `[patch]` Aucun test de bout en bout du mode réel jusqu'aux statuts HTTP — ajout de `ChatReelIT` (500→502, 401/403/délai/illisible/clé absente/coupe-circuit→503, contenu des requêtes).
  - `[low]` `[reject]` `SecretsTest` ne mute pas réellement `application.yml` — le détecteur est testé unitairement et scanne les fichiers versionnés ; une mutation réelle ajouterait une complexité disproportionnée.
  - `[low]` `[reject]` Délais 15 s / 30 s vérifiés au niveau de l'adaptateur et des propriétés, non au niveau endpoint — couvert par les tests de propriétés ; réglage non observable sans attendre 30 s.
  - `[maybe-false]` `[defer]` `trace-smoke.sh` non exécuté (nécessite la pile compose) — à confirmer par un passage humain ou la CI.
  - `[low]` `[reject]` Aucun `operator_actions` : aucune action humaine requise (clé fournie par variable d'environnement, tests sans clé ni réseau).

## Auto Run Result

Status: done

- Résumé : port `ConversationIa` avec adaptateur simulé par défaut (`AzureOpenAiSimule`, refus 401/403 réglable) et adaptateur réel Azure OpenAI (délais 15 s/30 s, coupe-circuit Resilience4j, aucun retry) ; `POST /api/open-banking-conseiller/chat/messages` sans état ; erreurs 400/502/503 aux textes du legacy, sans code 401/403 (BUG-038).
- Fichiers : domaine (`ConsigneSophie`, `Echange`, `RoleEchange`), application (`DialoguerCasUsage`, `ServiceIaIndisponible`, `MessageVide`, ports), adaptateurs (`PointEntreeChat`, `GestionErreurs`, `Textes`, `AzureOpenAiSimule`, `AzureOpenAiReel`, configuration/propriétés/paramètres), `AssemblageConversation`, `pom.xml`, `compose.yaml`, `.env.example`, tests tagués FR-010-05 à 09 et BUG-038.
- Revue : 1 patch appliqué (medium, `ChatReelIT`), 1 report (trace-smoke), 3 rejets (voir journal).
- followup_review_recommended : false.
- Vérification : `scripts/gates/build.sh`, `check-gates-exist.sh` (0 KO) et `golden-master.sh` en code 0 ; JaCoCo respecté.
- Risques : endpoint par défaut `https://demo-bank.cognitiveservices.azure.com` (substitut, le legacy n'en fournit pas d'exploitable) ; réponse simulée « Réponse simulée de Sophie. » de rédaction propre ; `max_tokens` pourrait devoir devenir `max_completion_tokens` sur certains déploiements ; adaptateur réel jamais testé contre Azure ; `trace-smoke.sh` non exécuté.
