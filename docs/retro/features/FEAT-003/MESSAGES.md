---
feature: FEAT-003
feature_name: Effectuer un virement
extraction_date: 2026-10-03
skill: retro-rules
sources:
  - src/main/webapp/virement.xhtml
  - src/main/java/com/accenture/bank/web/VirementBean.java
  - src/main/java/com/accenture/bank/ejb/TransferService.java
  - src/main/java/com/accenture/bank/mq/VirementProcessorMDB.java
  - retro/features/FEAT-003/FIELDS.md
  - retro/features/FEAT-003/VALIDATION.md
---

# MESSAGES — FEAT-003 Effectuer un virement

Inventaire exhaustif des textes visibles ou émis par le système dans le périmètre de la
fonctionnalité « Effectuer un virement » (`/virement.xhtml`).

Les templates de notification JMS (CONFIRMATION, ALERTE, FRAUDE) sont des chaînes produites
côté serveur (`VirementProcessorMDB`) et non affichées directement sur la page du formulaire ;
ils sont inventoriés ici car ce sont des textes émis par le système dans le périmètre de FEAT-003.

---

## Page et navigation

| Type | Texte exact | Contexte observé |
|---|---|---|
| Titre de page (onglet) | `BankApp Demo - Virement` | `<title>` |
| Marque / logo | `Demo Bank` | En-tête barre de navigation |
| Action | `Déconnexion` | Lien de déconnexion en en-tête |
| Lien de navigation | `Tableau de bord` | Menu principal |
| Lien de navigation (actif) | `Virement` | Menu principal |
| Lien de navigation | `Virements programmes` | Menu principal |
| Lien de navigation | `Releve` | Menu principal |
| Lien de navigation | `Budget` | Menu principal |
| Lien de navigation | `Prets` | Menu principal |
| Lien de navigation | `Documents` | Menu principal |
| Lien de navigation | `Bourse` | Menu principal |
| Lien de navigation | `Mon Conseiller` | Menu principal |
| Lien de navigation | `Produits` | Menu principal |
| Titre de section | `Effectuer un virement` | `<h2>` sur la page du formulaire |

---

## Bandeaux d'information

| Type | Texte exact | Contexte observé |
|---|---|---|
| Bandeau info (legacy) | `Système Legacy — Virement géré par EJB CMT (Container Managed Transaction)` | Bandeau violet affiché au-dessus du formulaire |
| Bandeau info (JMS) | `Après validation, une notification sera publiée sur queue/virements (ActiveMQ Artemis) et visible dans le tableau de bord.` | Bandeau violet affiché au-dessus du formulaire |
| Lien | `Virement SEPA externe — Gérer mes bénéficiaires` | Lien vers `/beneficiaires.xhtml` |

---

## Libellés du formulaire

| Type | Texte exact | Contexte observé |
|---|---|---|
| Libellé | `Compte source` | `<h:outputLabel>` du champ `compteSource` |
| Valeur par défaut | `-- Choisir un compte --` | Option `noSelectionOption` de `compteSource` |
| Libellé | `Compte destinataire` | `<h:outputLabel>` du champ `compteDestinataire` |
| Valeur par défaut | `-- Choisir un compte --` | Option `noSelectionOption` de `compteDestinataire` |
| Libellé | `Montant (€)` | `<h:outputLabel>` du champ `montant` |
| Indice | `Ex : 100,00 — Saisir plus de 5 000 € pour tester l'alerte fraude` | Texte d'aide sous le champ montant |
| Libellé | `Motif` | `<h:outputLabel>` du champ `motif` |
| Action | `Exécuter le virement` | Bouton de soumission du formulaire |

---

## Messages de validation front (JSF)

Ces messages sont produits par le cycle de vie JSF en phase « Process Validations » lors de la
soumission du formulaire avec un champ requis vide. Le préfixe inclut l'identifiant du
composant JSF rendu dans le DOM.

| Type | Texte exact | Contexte observé |
|---|---|---|
| Erreur de validation | `virementForm:source: Validation Error: Value is required.` | Soumission sans sélection de compte source — VALIDATION.md VAL-003-01-compteSource-F1 |
| Erreur de validation | `virementForm:target: Validation Error: Value is required.` | Soumission sans sélection de compte destinataire — VALIDATION.md VAL-003-01-compteDestinataire-F1 |
| Erreur de validation | `virementForm:amount: Validation Error: Value is required.` | Soumission sans montant — VALIDATION.md VAL-003-01-montant-F1 |
| Erreur de validation | `virementForm:motif: Validation Error: Value is required.` | Soumission sans motif — VALIDATION.md VAL-003-01-motif-F1 (dette-legacy : le serveur accepte motif null) |

---

## Messages d'erreur serveur (back — affichés sur la page)

| Type | Texte exact | Contexte observé |
|---|---|---|
| Erreur métier | `Veuillez sélectionner les deux comptes` | Aucun compte sélectionné (compteSource ou compteDestinataire null) — RG-003-003/004 |
| Erreur métier | `Le compte source et destination doivent être différents` | Mêmes comptes dans les deux combobox — RG-003-005 |
| Erreur technique exposée | `Erreur lors du virement : java.lang.IllegalStateException: Solde insuffisant` | Solde du compte source insuffisant ; catch(Exception) effectif (dette-legacy : trace Java exposée, RG-003-007) |
| Erreur technique exposée | `Erreur lors du virement : java.lang.IllegalArgumentException: Compte introuvable` | Compte source ou destinataire introuvable en base (dette-legacy : trace Java exposée) |
| Erreur technique exposée | `Erreur lors du virement : java.lang.IllegalArgumentException: Le montant doit etre positif` | Montant ≤ 0 soumis en contournant le front (dette-legacy : trace Java exposée) |

Note — message mort : `Solde insuffisant sur le compte source` est dans
un bloc `catch(IllegalStateException)` qui ne s'exécute jamais, car l'EJB CMT enveloppe
l'exception dans `EJBException` avant qu'elle n'atteigne ce catch. Ce message n'est jamais affiché.

---

## Message d'avertissement (fraude — affiché sur la page)

| Type | Texte exact | Contexte observé |
|---|---|---|
| Avertissement | `Attention : {N} alerte(s) fraude détectée(s) sur ce virement. Votre conseiller a été notifié.` | Affiché après exécution d'un virement si au moins une alerte fraude est générée — RG-003-024 ; `{N}` est le nombre d'alertes |

---

## Notifications asynchrones JMS (système → utilisateur)

Ces messages sont produits par `VirementProcessorMDB` après traitement du message JMS et stockés
dans `NotificationStore` ; ils sont visibles dans le tableau de bord, non sur la page du formulaire.
Le déclenchement dépend du montant comparé aux seuils configurables (défauts : 5 000 EUR et 10 000 EUR).

| Type | Template exact | Contexte observé |
|---|---|---|
| Notification CONFIRMATION | `Virement de {montant} EUR vers le compte {n°_compte_destinataire} confirme` | Montant < 5 000 EUR, motif absent — RG-003-019 |
| Notification CONFIRMATION (avec motif) | `Virement de {montant} EUR vers le compte {n°_compte_destinataire} confirme (motif : {motif})` | Montant < 5 000 EUR, motif non vide — RG-003-020 |
| Notification ALERTE | `Virement de {montant} EUR en attente de validation - transaction importante, verification en cours.` | 5 000 EUR ≤ montant < 10 000 EUR — RG-003-021 |
| Notification FRAUDE | `Virement de {montant} EUR bloque pour verification (seuil anti-fraude : {seuil} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation.` | Montant ≥ 10 000 EUR — RG-003-022 |
