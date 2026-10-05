### FEAT-008 — Documents (relevés, attestations, IFU)

**Description :** Tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN) consulte la page documents, en lecture seule, qui propose pour chacun de ses comptes un RIB, une attestation de solde et les relevés mensuels des 12 derniers mois, ainsi que l'IFU des deux dernières années fiscales. Chaque lien déclenche le téléchargement d'un PDF généré à la demande (le contenu détaillé des PDF est décrit dans FEAT-017). La page ne comporte aucun formulaire ni saisie. Les cas limites portent sur l'accès sans session, les paramètres de téléchargement manquants ou inconnus, le compte introuvable et le compte qui n'appartient pas à l'utilisateur. Scénarios golden master : SC-008-01.

**Exigences fonctionnelles :**

#### FR-008-01 : Redirection vers la connexion des écrans réservés aux utilisateurs authentifiés

Quand un utilisateur non authentifié demande la page documents, la page de chat avec le conseiller, l'espace conseiller (portefeuille clients) ou la page d'administration, le système le redirige vers la page de connexion. Les quatre règles d'origine ont le même effet (redirection vers la page de connexion), sans différence de condition ni de valeur.

Source : RG-008-001, RG-010-001, RG-015-001, RG-016-001

#### FR-008-02 : Téléchargement sans session

Quand une demande de téléchargement de document arrive sans session authentifiée, le système redirige vers la page de connexion, sans générer de document (même exigence côté téléchargement : FR-017-01).

Source : RG-008-002

#### FR-008-03 : Accès à la page documents pour tous les rôles

Le système autorise tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), à accéder à la page documents et à télécharger ses propres documents.

Source : RG-008-003

#### FR-008-04 : Refus d'un document portant sur le compte d'un autre client

Si un utilisateur connecté demande un relevé, un RIB ou une attestation pour un compte qui ne lui appartient pas, le système refuse la demande et ne sert aucun document. Ce contrôle remplace le comportement legacy RG-008-004, où tout identifiant de compte existant était servi sans vérification d'appartenance. Le refus est indépendant du compte introuvable (FR-008-11) : un compte existant mais d'un autre client est refusé, un compte inexistant reste en 404. [ASSUMPTION A-008-1 : le refus est signalé comme un accès interdit (HTTP 403) ; le corpus ne donne ni code ni message.]

Critère d'acceptation (test) : connecté en tant que client A, une demande de relevé, de RIB ou d'attestation sur un compte existant du client B est refusée et aucun PDF n'est renvoyé ; la même demande sur un compte du client A renvoie le PDF.

Source : RG-008-004, BUG-027

#### FR-008-05 : Documents proposés par compte

Quand un utilisateur authentifié affiche la page documents, le système affiche, pour chacun de ses comptes, une section "Compte {type de compte} - {numéro de compte}" contenant : une carte RIB (étiquette "Bancaire", titre "RIB", sous-titre "Releve d Identite Bancaire", lien "Telecharger PDF"), une carte attestation (étiquette "Attestation", titre "Attestation de solde", sous-titre "Solde au jour de la demande", lien "Telecharger PDF"), et une liste "Releves mensuels — {numéro de compte}" avec un lien "{mois} {année}" par mois pour les 12 mois les plus récents. Les libellés de mois sont "Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre".

Source : RG-008-005

#### FR-008-06 : Documents fiscaux (IFU N-1 et N-2)

Quand un utilisateur authentifié affiche la page documents, le système affiche une section "Documents fiscaux" titrée "Imprime Fiscal Unique (IFU)" avec deux cartes, "IFU {N-1}" et "IFU {N-2}" (N étant l'année civile en cours), chacune avec l'étiquette "Fiscal", le sous-titre "Revenus de capitaux mobiliers" et le lien "Telecharger PDF".

Source : RG-008-006

#### FR-008-07 : Relevé mensuel

Quand un relevé est demandé pour un compte, une année et un mois, le système génère et sert un PDF qui contient toutes les opérations du compte sur cette période, triées par date d'opération croissante (contenu détaillé : FR-017-04).

Source : RG-008-007

#### FR-008-08 : RIB

Quand un RIB est demandé pour un compte, le système génère et sert un PDF qui contient le numéro de compte, l'IBAN, le BIC/SWIFT, le code banque, le code guichet et le nom du titulaire (contenu détaillé : FR-017-05).

Source : RG-008-008

#### FR-008-09 : Attestation de solde

Quand une attestation est demandée pour un compte, le système génère et sert un PDF qui certifie le solde courant de ce seul compte, daté du moment de la demande (contenu détaillé : FR-017-06).

Source : RG-008-009

#### FR-008-10 : IFU de l'utilisateur connecté

Quand un IFU est demandé pour une année, le système le génère pour le client de la session authentifiée, jamais pour un client désigné dans les paramètres de la demande. Le revenu d'intérêts déclaré est le total des opérations au crédit dont la description contient "interet", et le système lui applique un Prélèvement Forfaitaire Unique (PFU) de 30 % (calcul et arrondi détaillés : FR-017-07).

Source : RG-008-010, RG-008-011

#### FR-008-11 : Demandes de téléchargement invalides

Le système rejette une demande de téléchargement : avec un code 400 et le message "Parametre type manquant" si le type de document est absent ; avec un code 400 et le message "accountId manquant" si l'identifiant de compte manque pour un relevé, un RIB ou une attestation ; avec un code 400 et le message "Type inconnu : {type}" si le type n'est ni RELEVE, ni RIB, ni ATTESTATION, ni IFU ; avec un code 404 et le message "Compte introuvable" si le compte demandé n'existe pas.

Source : RG-008-012, RG-008-013, RG-008-014, RG-008-015

#### FR-008-12 : Compteur de téléchargements

Quand un document est téléchargé, le système incrémente le compteur de téléchargements de ce document.

Source : RG-008-018

**Notes :**
- RG-008-016 (dette legacy) : non reproduite. Bugs conservés associés à la validation de l'année et du mois d'un relevé ou d'un IFU : BUG-028, BUG-030, BUG-031, BUG-032 (le legacy génère un PDF vide sans erreur).
- RG-008-017 (dette legacy) : non reproduite. Bug conservé associé : BUG-029 (SIRET de la banque écrit en dur).
- RG-010-001, RG-015-001, RG-016-001 : intégrées dans l'exigence fusionnée FR-008-01, dont FEAT-008 est porteuse (règles voisines).
- Les messages de génération de documents enregistrés ("Session expirée.", "Relevé {mois} {année} généré avec succès.", "Impossible de générer le relevé.", etc.) ne sont jamais affichés dans le parcours de la page documents : non repris.
- Le bandeau technique de la page legacy (pile technique, adresse du service de téléchargement) n'est pas repris.
- Recouvrement avec FEAT-017 (même service de téléchargement, aucune fusion décidée) : les FR de FEAT-008 décrivent la page et l'effet attendu, celles de FEAT-017 le contenu des PDF et les réponses de téléchargement.
- Écart corpus : le legacy observé en exécution renvoie une erreur 500 pour tout relevé, RIB ou attestation (défaut de chargement des données) et une erreur 500 au lieu de 404 pour un compte inexistant (BUG-061, conservé, voir FEAT-017). Les FR reprennent le comportement décrit par les règles.
