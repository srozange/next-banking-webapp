---
feature: FEAT-012
title: "Comptes Tiers — Open Banking Powens"
status: current
source_commit: c05512ecab8af8878d695ccd0f33fc3ece8811a4
---

# RG — FEAT-012 Comptes Tiers — Open Banking Powens

_Extrait par retro-rules · 2026-10-03_
_Sources : powens/PowensBean.java · powens/PowensService.java · powens/PowensCallbackBean.java · powens/ExternalAccount.java · powens/ExternalTransaction.java · webapp/comptes-tiers.xhtml · web/AppFilter.java · config/AppConfig.java_

---

## Sécurité et contrôle d'accès

RG-012-001 [Confirmé]
WHEN un utilisateur non authentifié accède à comptes-tiers.xhtml THE SYSTEM SHALL rediriger vers login.xhtml.

RG-012-002 [Confirmé]
THE SYSTEM SHALL autoriser l'accès à comptes-tiers.xhtml à tous les rôles authentifiés (CLIENT, CONSEILLER, ADMIN) sans restriction de contenu différenciée.

RG-012-003 [À confirmer]
THE SYSTEM SHALL autoriser l'accès à powens-callback.xhtml sans session authentifiée.

---

## États de l'interface

RG-012-004 [Confirmé]
WHEN la session est dans l'état non-connecté (powensBean.connected = false) THE SYSTEM SHALL afficher le bloc de connexion avec les logos des banques partenaires (BNP Paribas, Crédit Agricole, La Banque Postale, CIC, LCL, +1800 banques) et le bouton "Connecter une banque".

RG-012-005 [Confirmé]
WHEN la session est dans l'état connecté (powensBean.connected = true) THE SYSTEM SHALL masquer le bloc de connexion et afficher le patrimoine total, la liste des comptes externes, la section transactions et le bouton "Déconnecter".

---

## Initiation de la connexion Powens

RG-012-006 [Confirmé]
WHEN l'utilisateur clique "Connecter une banque" THE SYSTEM SHALL (1) appeler POST /auth/init avec Basic Auth (client_id:client_secret) pour créer un utilisateur Powens anonyme et obtenir un auth_token permanent, (2) appeler GET /auth/token/code avec Bearer auth_token pour obtenir un code temporaire, (3) stocker l'auth_token en session, (4) construire l'URL Webview Powens avec les paramètres domain, client_id, redirect_uri, code et connector_ids, et (5) rediriger le navigateur vers cette URL externe.

RG-012-007 [À confirmer]
IF l'appel à l'API Powens lors de l'initialisation échoue (réponse absente, HTTP >= 400, ou champ auth_token manquant) THEN THE SYSTEM SHALL afficher le message "Impossible d initialiser la connexion Powens." et rester sur l'état non-connecté.

---

## Traitement du callback Powens

RG-012-008 [Confirmé]
WHEN le callback Powens est reçu sans les paramètres connection_id ni code THE SYSTEM SHALL afficher le message "Callback Powens incomplet. Parametres recus: [<liste des paramètres reçus>]" et rester sur l'état non-connecté.

RG-012-009 [Suspect]
WHEN le callback Powens est reçu avec un paramètre error THE SYSTEM SHALL déconnecter la session Powens et informer l'utilisateur de la raison du retour.

RG-012-010 [À confirmer]
WHEN le callback Powens est reçu avec le paramètre connection_id THE SYSTEM SHALL déclencher le chargement des comptes externes avec cet identifiant de connexion.

RG-012-011 [À confirmer]
WHEN le callback Powens est reçu avec le paramètre code OAuth et sans connection_id (flow alternatif) THE SYSTEM SHALL déclencher le chargement des comptes externes avec ce code.

---

## Chargement des comptes après callback

RG-012-012 [À confirmer]
IF la session ne contient pas d'auth_token Powens lors du traitement du callback THEN THE SYSTEM SHALL afficher le message "Session expiree. Veuillez recommencer la connexion." et rester sur l'état non-connecté.

RG-012-013 [Confirmé]
WHEN le chargement des comptes après callback réussit mais retourne une liste vide THE SYSTEM SHALL passer à l'état connecté et afficher le message "Connexion reussie mais aucun compte disponible encore. Reessayez dans quelques secondes."

RG-012-014 [À confirmer]
WHEN le chargement des comptes après callback retourne au moins un compte THE SYSTEM SHALL passer à l'état connecté, sélectionner automatiquement le premier compte de la liste et charger ses transactions.

---

## Affichage du patrimoine et des comptes externes

RG-012-015 [À confirmer]
THE SYSTEM SHALL calculer le patrimoine total comme la somme des soldes non-nuls de tous les comptes externes.

RG-012-016 [À confirmer]
WHEN un compte externe est affiché THE SYSTEM SHALL traduire son type Powens en libellé français selon la correspondance : checking → "Compte courant", savings → "Epargne", card → "Carte", loan → "Credit", market → "Bourse" ; tout autre type est affiché tel quel (valeur brute renvoyée par l'API).

RG-012-017 [À confirmer]
WHEN un compte externe est affiché THE SYSTEM SHALL afficher l'IBAN uniquement si la valeur renvoyée par l'API Powens est non vide.

RG-012-018 [Suspect]
IF un compte externe renvoyé par l'API Powens a un solde manquant (champ absent ou null) THEN THE SYSTEM SHALL distinguer l'absence de solde d'un solde effectivement nul.

---

## Transactions

RG-012-019 [À confirmer]
THE SYSTEM SHALL récupérer au maximum 20 transactions par compte externe depuis l'API Powens.

RG-012-020 [À confirmer]
WHEN une transaction externe est chargée depuis l'API Powens THE SYSTEM SHALL utiliser simplified_wording comme libellé ; si simplified_wording est vide, utiliser original_wording.

RG-012-021 [À confirmer]
IF la valeur d'une transaction externe est strictement positive THEN THE SYSTEM SHALL l'afficher en crédit, avec la couleur verte et le préfixe "+".

RG-012-022 [À confirmer]
IF la valeur d'une transaction externe est nulle ou négative THEN THE SYSTEM SHALL l'afficher en débit, avec la couleur rouge et sans préfixe "+".

RG-012-023 [À confirmer]
WHEN l'utilisateur sélectionne un compte externe THE SYSTEM SHALL charger les transactions de ce compte et mettre à jour la section transactions par AJAX sans rechargement de page.

---

## Déconnexion

RG-012-024 [À confirmer]
WHEN l'utilisateur clique "Déconnecter" THE SYSTEM SHALL effacer le token d'accès Powens, la liste des comptes, le compte sélectionné, les transactions et le message d'erreur, et passer à l'état non-connecté.

---

## Configuration et fiabilité

RG-012-025 [À confirmer]
THE SYSTEM SHALL lire la configuration Powens (domain, client_id, client_secret, redirect_uri, connector_id) depuis les variables d'environnement, avec des valeurs par défaut correspondant à l'environnement sandbox Powens (demi-sandbox.biapi.pro, client_id 99961298, connector_ids 59).

RG-012-026 [À confirmer]
THE SYSTEM SHALL appliquer un timeout de 15 secondes (connexion et lecture) pour chaque appel HTTP vers l'API Powens.

RG-012-027 [Suspect]
IF l'API Powens retourne une réponse HTTP >= 400 lors du chargement des comptes ou des transactions THEN THE SYSTEM SHALL informer l'utilisateur de l'indisponibilité temporaire du service.

---

## Fonctionnalité partiellement câblée

RG-012-028 [À confirmer]
THE SYSTEM SHALL permettre à l'utilisateur de rafraîchir manuellement la liste de ses comptes externes sans se déconnecter et se reconnecter.
  (intention-code-mort) La méthode refreshAccounts() est implémentée dans PowensBean mais n'est câblée à aucun bouton ni lien dans comptes-tiers.xhtml : le bouton "Rafraîchir" doit-il être exposé dans la version cible ?
    Portée : information
