---
title: "Divergences restantes avec le golden master : next-banking-webapp"
status: prévision validée par le décideur (2026-10-05), à confirmer au premier rejeu réel ; à reprendre à l'étape G2 (dérivation du golden master, architecture)
created: 2026-10-05
sources:
  - docs/retro/golden-master/scenarios/FEAT-NNN/SC-*/ (scenario.json, expected-dom.txt, input-actions.json, expected-response.json, input-request.json)
  - docs/retro/golden-master/replay-config.json ; docs/retro/tools/replay-golden-master.py ; docs/retro/tools/derive-golden-master.py ; docs/retro/golden-master-contract.md
  - EXPERIENCE.md (Écarts de texte avec le legacy, parties A, B et C) ; textes-nouveaux.md
  - _bmad-output/planning-artifacts/prds/prd-next-banking-webapp-2026-10-05/prd.md (FR ÉVOLUTION, corrections de bugs, §9 B1 révisée à B7, NFR-PAR-01)
---

# Divergences restantes avec le golden master

## Périmètre

- 33 scénarios capturés : 28 d'écran (playwright) et 5 HTTP (SC-001-01, SC-001-02, SC-001-03, SC-014-01, SC-017-01).
- Exclus : SC-003-03 (BUG-011) et SC-010-02 (BUG-038), liés à des bugs à corriger.
- **31 applicables : 26 d'écran et 5 HTTP.** Les 26 scénarios d'écran comparent en mode `contains` (déclaré dans chaque `scenario.json`) ; SC-001-03 compare le statut et un fragment du corps (`status+body_contains`) ; les 4 autres HTTP ne comparent que le code de statut.

## Méthode

Pour chaque scénario applicable, ce qu'il compare (lignes attendues, `wait_for`, sélecteurs actionnés, statut et fragment HTTP) est confronté à :
- (a) les écarts de texte restants de EXPERIENCE.md : partie A sans les textes « transaction importante » (B1 révisée, reporté après la démonstration, FR-003-29), partie B (corrections de bugs, retraits de bandeaux techniques), partie C (ajouts groupés) ;
- (b) les changements de comportement décidés : contrôle de fraude avant la validation (FR-003-15, FR-027-02), suspension si score cumulé > 75 ou montant ≥ 10 000 € (FR-027-02, FR-023-04), refus en cas d'erreur du contrôle (FR-003-27), retrait de l'avertissement « Attention : N alerte(s)… » (FR-003-16), contrôles de saisie et de solde avant le contrôle de fraude (FR-003-07), exemption d'authentification par chemin exact (FR-030-01, A-030-1), 5 dernières opérations du compte COURANT (FR-002-05, BUG-005), compte désactivé refusé (FR-001-07), contrôle d'appartenance (FR-004-10, FR-008, FR-013), bénéficiaire interne obligatoire (FR-004-13), notifications de solde faible affichées (FR-025-08), libération avec contrôle du solde (B6) et rejet sans débit d'un virement libéré dont le solde est devenu insuffisant au débit (B7, aucun scénario ne libère un virement) ;
- (c) les données de démonstration : aucun scénario applicable ne soumet un virement de 10 000 € ou plus (SC-003-03, seul à soumettre 99 999 EUR, est exclu ; le 10 000 EUR de SC-007-02 est une simulation de prêt, pas un virement).

Règle de lecture : en mode `contains`, un texte **ajouté** ne fait pas échouer un scénario ; une ligne attendue qui **disparaît ou change** le fait échouer, de même qu'un `wait_for` qui n'apparaît plus.

## Scénarios qui divergent encore

Aucune ligne attendue, aucun `wait_for` et aucun statut HTTP des 31 scénarios applicables n'est touché par un écart de texte restant (parties A, B, C) : les bandeaux techniques retirés (B-16 à B-31) et les messages corrigés (B-1 à B-15) ne figurent dans aucune ligne attendue d'un scénario applicable ; B-1 et B-4 ne touchent que les scénarios exclus SC-003-03 et SC-010-02.

Un seul scénario peut encore diverger, par un changement de comportement voulu, et seulement selon les conditions du rejeu :

| identifiant | fonctionnalité | ce qui diverge | raison | exigence | traitement G2 |
|---|---|---|---|---|---|
| SC-003-04 | FEAT-003 Effectuer un virement (100 EUR COURANT → EPARGNE, puis tableau de bord) | Divergence **conditionnelle**, de comportement. Le virement passe désormais par le contrôle de fraude avant la validation. Le compte EPARGNE n'est pas un bénéficiaire enregistré : NEW_BENEFICIARY (35) est levé. Si le rejeu tourne avant 06:00 ou après 23:00 (heure du serveur), UNUSUAL_HOURS (45) porte le score à 80. Il atteint 90 si un autre virement du client a été contrôlé moins de 30 s avant (RAPID_SUCCESSION, 55), et 85 avec plus de 8 opérations du jour (HIGH_FREQUENCY, 50). Au-delà de 75, le virement est SUSPENDU : pas de redirection vers le tableau de bord, et le `wait_for` « CONFIRMATION » ainsi que les lignes « Tableau de bord », « CONFIRMATION », « [retro] virement doré 100EUR », « DEBIT », « COURANT » et « EPARGNE » sont absents. En conditions nominales (score 35), le virement est exécuté et les 6 lignes restent identiques. | Changement de comportement voulu | FR-003-15, FR-027-02 (décision RG-027-003 D1 à D4), FR-003-19, FR-003-20, FR-003-23, FR-003-18 | **map**, sans text_map (aucune ligne attendue ne change). Exception documentée dans le rationale : rejeu entre 06:00 et 23:00, heure du serveur (ou horloge fixée), base remise au jeu de démonstration avant le rejeu, aucun virement du client contrôlé dans les 30 s qui précèdent, 8 opérations au plus le jour du rejeu. Un drop n'est pas justifié : le scénario prouve le parcours nominal du virement exécuté. |

**Scénarios divergents : 0 de façon certaine, 1 de façon conditionnelle (SC-003-04).** Statut : **prévision validée par le décideur** le 2026-10-05, à confirmer au premier rejeu réel du golden master ; d'autres écarts éventuels passent par l'étape G2 et ne sont pas des régressions tant qu'ils découlent des décisions D1 à D12.

Écart avec le PRD, résolu le 2026-10-05 : NFR-PAR-01 et l'addendum A.7 annonçaient que SC-003-01, SC-003-02 et SC-003-04 « divergent volontairement », annonce faite sans lire les fichiers des scénarios. Les fichiers ne le confirment que pour SC-003-04, et sous condition ; le PRD (NFR-PAR-01, SM-1, addendum A.7) est désormais aligné sur cette analyse. SC-003-01 (chargement du formulaire) ne soumet rien. Le virement de SC-003-02 (comptes identiques) est rejeté avant le contrôle de fraude (FR-003-07), avec le message legacy inchangé (FR-003-03).

## Scénarios applicables qui ne divergent pas (30)

| identifiant | fonctionnalité | ce qui est comparé | pourquoi rien ne diverge |
|---|---|---|---|
| SC-001-01 | FEAT-001 Connexion (HTTP) | GET `login.xhtml` → 200 | Page de connexion publique sur son chemin exact (FR-001-01, FR-030-01). |
| SC-001-02 | FEAT-001 Connexion (HTTP) | GET `dashboard.xhtml` sans session → 302 | Redirection vers la connexion conservée (FR-001-02). |
| SC-001-03 | FEAT-001 Connexion (HTTP) | POST du formulaire avec un identifiant inconnu → 200 et « Identifiants incorrects » | Message conservé (FR-001-06). B-10 ne concerne que le compte désactivé. |
| SC-001-04 | FEAT-001 Connexion | « Tableau de bord », « Bonjour, Jean Dupont », « MES COMPTES DEMO BANK » | Textes legacy inchangés ; la section « Suivi de mes virements » est un ajout (C1). |
| SC-001-05 | FEAT-001 Connexion | Clic « Deconnexion », puis « Identifiant », « Mot de passe », « Espace client » | Sous-titre « Espace client — Connexion sécurisée » conservé (FR-001-01) ; seul le bandeau B-16 disparaît, et il n'est pas comparé. |
| SC-002-01 | FEAT-002 Tableau de bord | Titres et séparateurs, en-tête « DATE DESCRIPTION TYPE MONTANT SOLDE APRES », « DEBIT » | Textes conservés (FR-002) ; B-17, B-18 et B-19 ne sont pas comparés ; les 5 opérations du compte COURANT (FR-002-05) restent celles du premier compte du jeu de données. |
| SC-002-02 | FEAT-002 Tableau de bord | « Centre de notifications » | Titre conservé (FR-002-07) ; seuls les libellés techniques B-18 disparaissent. |
| SC-003-01 | FEAT-003 Effectuer un virement | Titre, libellés des champs, « COURANT », « EPARGNE » | Formulaire inchangé (FR-003-01) ; B-20 n'est pas comparé ; « Suivi de mes virements » est un ajout. |
| SC-003-02 | FEAT-003 Effectuer un virement | « Le compte source et destination doivent être différents » | Rejet avant le contrôle de fraude (FR-003-07), message inchangé (FR-003-03). |
| SC-004-01 | FEAT-004 Virements programmés | Libellés du formulaire, « Hebdomadaire », « Mensuel » | Libellés conservés (FR-004-02, FR-004-08). |
| SC-004-02 | FEAT-004 Virements programmés | « Virement programme cree avec succes », « MES VIREMENTS PROGRAMMES » | 50 EUR vers FR76-0001-0002, compte interne existant (FR-004-13 satisfait) ; date `yyyy-MM-dd` conservée ; le contrôle de fraude n'intervient qu'à l'échéance (FR-021-10). |
| SC-004-03 | FEAT-004 Virements programmés | « Veuillez selectionner un compte source » | Message conservé (FR-004-04). |
| SC-005-01 | FEAT-005 Relevé de compte | « Relevé de compte », « COURANT », « FR76-0001-0001 », en-tête du tableau, « DEBIT » | Textes conservés ; B-22 n'est pas comparé ; les textes « [EN ATTENTE] » restent ceux du legacy (B1 révisée). |
| SC-006-01 | FEAT-006 Budget | Les 4 titres de section | Textes conservés ; B-23 n'est pas comparé. |
| SC-007-01 | FEAT-007 Prêts | Titres de section, « TYPE DE PRET », « Immobilier », « Automobile » | Textes conservés ; B-24 n'est pas comparé. |
| SC-007-02 | FEAT-007 Prêts | Résultat de simulation 10 000 EUR, 3,5 %, 36 mois (« 293.02 EUR »…) | Calcul et formats conservés (FR-007-03) ; simulation de prêt, pas un virement. |
| SC-008-01 | FEAT-008 Documents | Titres des documents, « Telecharger PDF » | Textes conservés ; « (nouvel onglet) » est un ajout qui contient toujours « Telecharger PDF » ; B-25 n'est pas comparé. |
| SC-009-01 | FEAT-009 Bourse | « Valeur », « BNP.PA », « AI.PA », « VARIATION SUR LA PÉRIODE », « PERFORMANCE % » | Textes conservés ; B-26 n'est pas comparé. Condition de rejeu : adaptateur AlphaVantage simulé qui renvoie des données (A-G-09). |
| SC-010-01 | FEAT-010 Mon Conseiller | « Sophie — Conseillère Demo Bank », « Bonjour Jean ! » | En-tête et bienvenue conservés (FR-010-02) ; B-27 (bandeau IA) n'est pas comparé. |
| SC-011-01 | FEAT-011 Produits | Titres de section, « CEL - Compte Epargne Logement », « Livret A » | Textes conservés ; B-14 et B-28 ne sont pas comparés ; le format des taux (BUG-039) n'est pas comparé. |
| SC-012-01 | FEAT-012 Comptes Tiers | « Agregez vos comptes bancaires », « BNP Paribas », « Credit Agricole », « OAuth2 PSD2 » | « OAuth2 PSD2 » vient du paragraphe fonctionnel MSG-012-051, conservé (FR-012), et non du bandeau B-29 retiré. |
| SC-013-01 | FEAT-013 Bénéficiaires | Titres, en-tête du tableau, « ✓ SEPA », « FR76*** » | Masquage et zone SEPA conservés (FR-013-03) ; B-30 n'est pas comparé. |
| SC-013-02 | FEAT-013 Bénéficiaires | Vérification IBAN, puis « Bénéficiaire ajouté avec succès » et la ligne ajoutée | Messages et masquage « FR35***9010 » conservés (FR-013-03). |
| SC-014-01 | FEAT-014 Retour Powens (HTTP) | GET `powens-callback.xhtml` sans paramètre → 302 | Accessible sans session sur son chemin exact, redirection 302 (FR-014-01, FR-030-01). |
| SC-015-01 | FEAT-015 Espace Conseiller | Indicateurs, « Jean Dupont », « @user1 », « Marie Lebrun », « @user2 » | Libellés conservés (FR-015-03) ; le portefeuille filtré sur le conseiller affecté contient ces deux clients dans le jeu de démonstration. |
| SC-015-02 | FEAT-015 Espace Conseiller | CLIENT redirigé : « Tableau de bord », « MES COMPTES DEMO BANK » | Redirection silencieuse conservée (FR-001-11, FR-030-02). |
| SC-016-01 | FEAT-016 Administration | Indicateurs, « Pipeline Prets », « Gestion des clients », « Jean Dupont », « user1 » | Textes conservés (FR-016-02, FR-016-03) ; B-15 et B-31 ne sont pas comparés ; la file de revue est un ajout (C1), placé après « Gestion des clients ». |
| SC-016-02 | FEAT-016 Administration | CLIENT redirigé : « Tableau de bord », « MES COMPTES DEMO BANK » | Redirection silencieuse conservée (FR-016-01). |
| SC-017-01 | FEAT-017 Téléchargement PDF (HTTP) | GET `docs/download` sans session → 302 | Redirection vers la connexion conservée (FR-017-01). |
| SC-017-02 | FEAT-017 Téléchargement PDF | `wait_for` « Telecharger PDF », titres RIB et attestation | Textes conservés ; « (nouvel onglet) » est un ajout. |

## Points de vigilance pour l'étape G2 (non liés aux décisions)

- **Fenêtre de 5 000 caractères** : `replay-golden-master.py` tronque le texte de la page à 5 000 caractères avant la comparaison (`DOM_TEXT_MAX_CHARS`). Les ajouts de la partie C (section « Suivi de mes virements » sur le Tableau de bord et le Virement, file de revue sur l'Administration) se placent après le contenu comparé. Sinon, ils peuvent repousser des lignes attendues hors de la fenêtre : « Centre de notifications » et « CONFIRMATION » pour SC-002-02 et SC-003-04, « Gestion des clients », « Jean Dupont » et « user1 » pour SC-016-01. Le retrait des bandeaux techniques libère de la place dans la fenêtre.
- **Scénarios HTTP à re-mapper** : les 5 scénarios HTTP visent les chemins `.xhtml` du legacy. SC-001-03 poste un formulaire JSF (`loginForm:*`, jetons tirés de `login.xhtml`). La route cible, la forme de la requête et le statut attendu (200 avec le message dans le corps, ou statut de refus de l'API) sont des décisions d'architecture de l'étape G2, pas des écarts décidés.
- **Données et adaptateurs** : le rejeu suppose la base remise au jeu de démonstration (soldes, bénéficiaire FR76*** de SC-013-01, portefeuille de sophie, watchlist BNP.PA et AI.PA) et les adaptateurs simulés AlphaVantage et Powens (A-G-09).
- **Casse CSS** : les lignes attendues en capitales (« MES COMPTES DEMO BANK », « TOTAL CLIENTS »…) supposent le même `text-transform` que le legacy (EXPERIENCE, Voice and Tone).
