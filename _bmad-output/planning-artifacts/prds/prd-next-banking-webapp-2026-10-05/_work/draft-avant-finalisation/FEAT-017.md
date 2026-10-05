### FEAT-017 — Téléchargement documents PDF

**Description :** Fonction de téléchargement appelée depuis la page documents (FEAT-008) par tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN). Selon le type demandé (RELEVE, RIB, ATTESTATION, IFU), elle génère un PDF et le renvoie en pièce jointe avec un nom de fichier normalisé. Elle contrôle elle-même la session, valide le type et l'identifiant de compte, refuse les comptes d'autres clients et masque les détails internes en cas d'échec de génération. Scénarios golden master : SC-017-01, SC-017-02.

**Exigences fonctionnelles :**

#### FR-017-01 : Session obligatoire pour télécharger

Si une demande de téléchargement arrive sans session authentifiée valide, le système redirige vers la page de connexion. Ce contrôle est effectué par la fonction de téléchargement elle-même, indépendamment du contrôle d'accès général des écrans.

Source : RG-017-001, RG-017-002

#### FR-017-02 : Type de document

Si le type de document est absent, le système rejette la demande avec un code 400 et le message "Parametre type manquant". Si le type ne correspond à aucune des valeurs RELEVE, RIB, ATTESTATION ou IFU, comparées sans tenir compte de la casse, le système rejette la demande avec un code 400 et le message "Type inconnu : {type}".

Source : RG-017-003, RG-017-004

#### FR-017-03 : Identifiant de compte

Si le type est RELEVE, RIB ou ATTESTATION et que l'identifiant de compte est absent, vide ou non numérique, le système rejette la demande avec un code 400 et le message "accountId manquant".

Source : RG-017-005

#### FR-017-04 : PDF du relevé de compte

Pour le type RELEVE, le système génère un PDF qui contient toutes les opérations du compte pour l'année et le mois demandés, triées par date d'opération croissante, et le renvoie en pièce jointe nommée `releve_{accountId}_{année}_{mois sur 2 chiffres}.pdf`. Le PDF est titré "Demo Bank — Releve de compte", porte les lignes "Compte", "Titulaire", "Periode", "Solde actuel" (montant au format "{montant} EUR"), et un tableau aux colonnes "Date", "Description", "Type", "Montant", "Solde apres" (type "CREDIT" ou "DEBIT", montants "+{montant} EUR" ou "-{montant} EUR") ; sans opération sur la période, il affiche "Aucune operation sur cette periode.".

Source : RG-017-007

#### FR-017-05 : PDF du RIB

Pour le type RIB, le système génère un PDF titré "Demo Bank — Releve d Identite Bancaire (RIB)", avec le nom complet du titulaire ("Titulaire"), "Banque" : "Demo Bank SA", "Code banque" : "30004", "Code guichet" : "00001", "Numero de compte", "Cle RIB" : "42", "IBAN", "BIC / SWIFT" : "DEMOFR21XXX", "Domiciliation" : "Demo Bank - Agence Paris Centre", et la note "Document genere le {jj/mm/aaaa} - Demo Bank SA, 1 Place Vendome, 75001 Paris". Il le renvoie en pièce jointe nommée `rib_{accountId}.pdf`.

Source : RG-017-008

#### FR-017-06 : PDF de l'attestation de solde

Pour le type ATTESTATION, le système génère un PDF titré "Demo Bank — Attestation de Solde" qui atteste le solde courant du compte à la date de génération, avec les textes "Demo Bank SA, dont le siege social est situe 1 Place Vendome, 75001 Paris, atteste par la presente que :", "M. / Mme {nom complet}", "est titulaire du compte n° {numéro de compte}", "Le solde de ce compte, arrete au {jj mois aaaa}, est de :", "{montant} EUR", "Cette attestation est delivree a la demande de l interesse(e) pour faire valoir ce que de droit." et "Fait a Paris, le {jj mois aaaa}". Il le renvoie en pièce jointe nommée `attestation_solde_{accountId}.pdf`.

Source : RG-017-009

#### FR-017-07 : PDF de l'IFU

Pour le type IFU, le système génère l'Imprimé Fiscal Unique du client de la session authentifiée (jamais d'un client désigné par la demande) pour l'année demandée : il totalise les opérations au crédit dont la description contient "interet" et applique un Prélèvement Forfaitaire Unique de 30 %, arrondi au demi supérieur à 2 décimales. Le PDF est titré "Demo Bank — Imprime Fiscal Unique (IFU) {année}", porte "Etablissement" : "Demo Bank SA", "SIREN" : "123 456 789 00012", "Titulaire", "Adresse" : "1 rue de la Paix, 75001 Paris", "Annee fiscale", un tableau "Nature des revenus" / "Montant brut" avec les lignes "Interets et produits de placement" et "Prelevement Forfaitaire Unique (30%)", la note "Ce document est fourni a titre informatif. Il recense les revenus de capitaux mobiliers soumis au PFU pour l annee {année}." et "Genere le {jj/mm/aaaa}". Il est renvoyé en pièce jointe nommée `ifu_{customerId}_{année}.pdf`.

Source : RG-017-010

#### FR-017-08 : Refus d'un compte appartenant à un autre client

Si le client de la session demande un document pour un compte qui ne lui appartient pas, le système refuse la demande et ne génère aucun document. Ce contrôle remplace le comportement legacy RG-017-011 (aucune vérification d'appartenance). Il s'applique après les contrôles de FR-017-02 et FR-017-03 ; un compte inexistant relève de FR-017-09. [ASSUMPTION A-017-1 : le refus est signalé comme un accès interdit (HTTP 403), sans message métier défini par le corpus ; même choix que FEAT-008.]

Critère d'acceptation (test) : connecté en tant que client A, une demande RELEVE, RIB ou ATTESTATION sur un compte existant du client B ne renvoie aucun PDF et est refusée ; la même demande sur un compte du client A renvoie un PDF.

Source : RG-017-011, BUG-058

#### FR-017-09 : Compte ou client introuvable

Si le compte ou le client visé n'existe pas, le système rejette la demande avec un code 404 et le message "Compte introuvable" (compte) ou "Client introuvable" (client, type IFU).

Source : RG-017-012

#### FR-017-10 : Échec de génération sans détail interne

Si une erreur inattendue survient pendant la génération du PDF, le système répond par un code 500 avec le seul message "Erreur lors de la generation du document", sans aucun détail technique interne (message d'exception, trace, nom de composant). Ce comportement remplace le comportement legacy RG-017-013.

Critère d'acceptation (test) : en provoquant une erreur de génération, la réponse a le code 500, son corps vaut exactement "Erreur lors de la generation du document" et ne contient ni message d'exception ni trace.

Source : RG-017-013, BUG-059

#### FR-017-11 : Format de la réponse PDF

Pour tout document généré, le système renvoie le type de contenu `application/pdf`, la disposition `attachment; filename="{nom de fichier}"` et le contrôle de cache `no-cache, no-store, must-revalidate`. Tous les PDF portent le pied de page "Demo Bank SA — Capital 500 000 000 EUR — RCS Paris 123 456 789 00012 — Établissement de crédit agréé par l'ACPR".

Source : RG-017-014

**Notes :**
- RG-017-006 (dette legacy) : non reproduite. Bugs conservés associés (année ou mois absent, non numérique ou hors plage) : BUG-057, BUG-062, BUG-063.
- RG-017-015 (dette legacy) : non reproduite. Bug conservé associé : BUG-060 (SIRET écrit en dur).
- BUG-061 (conservé) : le legacy renvoie une erreur 500 au lieu de 404 pour un compte inexistant ; FR-017-09 reprend la règle (404), qui fait foi.
- Écart corpus : en exécution, le legacy renvoie une erreur 500 pour tout RELEVE, RIB ou ATTESTATION (défaut de chargement des données) ; seul l'IFU fonctionne. Les FR décrivent le comportement des règles.
- Recouvrement avec FEAT-008 (aucune fusion décidée) : FR-017-01 et FR-008-02, FR-017-02/03/09 et FR-008-11, FR-017-08 et FR-008-04 décrivent les mêmes contrôles et doivent rester identiques.
- Les libellés de la page documents sont portés par FEAT-008 (FR-008-05, FR-008-06).
