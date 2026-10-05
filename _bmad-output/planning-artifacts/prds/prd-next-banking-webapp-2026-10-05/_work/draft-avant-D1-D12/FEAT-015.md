### FEAT-015 — Espace Conseiller — Portefeuille clients

**Description :** L'espace conseiller est utilisé par les rôles CONSEILLER et ADMIN ; un CLIENT qui tente d'y accéder est renvoyé sans message vers son tableau de bord. Il présente le portefeuille des clients affectés à l'utilisateur connecté : un bandeau d'accueil, trois indicateurs (nombre de clients, encours total, prêts en attente), puis une carte par client avec ses soldes, comptes et prêts. Un panneau de détail s'ouvre sur la même page pour consulter les coordonnées, les comptes et les prêts d'un client. Cas limites marquants : portefeuille vide, client sans compte ou sans prêt, client inactif ; le portefeuille est chargé une seule fois par session (pas de rafraîchissement, comportement legacy conservé). Scénarios golden master : SC-015-01, SC-015-02.

**Exigences fonctionnelles :**

#### FR-015-01 : Accès à l'espace conseiller selon le rôle

Un utilisateur authentifié de rôle CONSEILLER ou ADMIN qui demande l'espace conseiller voit la page « Espace Conseiller » (portefeuille). Un utilisateur de rôle CLIENT qui la demande est redirigé vers son tableau de bord, sans aucun message d'erreur.

Source : RG-015-002, RG-015-003

#### FR-015-02 : Chargement du portefeuille du conseiller connecté

À la première ouverture de l'espace conseiller dans une session, le système charge la liste des clients dont le conseiller affecté est l'utilisateur connecté, et seulement ceux-là, triée par ordre alphabétique du nom complet. Le bandeau d'accueil affiche « Espace Conseiller » et « Bonjour {fullName}, vous gerez {N} client(s) », où {fullName} est le nom complet du conseiller connecté et {N} le nombre de clients du portefeuille.

Source : RG-015-004, RG-015-005

#### FR-015-03 : Indicateurs du portefeuille

Une fois le portefeuille chargé, le système affiche trois indicateurs : « Mes clients » (nombre de clients du portefeuille, sous-libellé « clients dans mon portefeuille »), « Encours total » (somme des soldes de tous les comptes de tous les clients du portefeuille, sous-libellé « solde cumulé mes clients ») et « Prets en attente » (nombre total de prêts au statut EN_ATTENTE de tous les clients du portefeuille, sous-libellé « a traiter »).

Source : RG-015-006, RG-015-007, RG-015-008

#### FR-015-04 : Portefeuille vide

Si le portefeuille du conseiller ne contient aucun client, le système affiche le message « Aucun client dans votre portefeuille. » suivi de « Contactez l administrateur pour l affectation des clients. », à la place de la liste des clients.

Source : RG-015-009

#### FR-015-05 : Carte client

Quand le portefeuille n'est pas vide, le système affiche une carte par client avec : la première lettre du nom complet en avatar, le nom complet, l'identifiant précédé de « @ », un badge de statut « Actif » ou « Inactif », et quatre statistiques : « Solde total » (somme des soldes de ses comptes), « Comptes » (nombre de comptes bancaires), « Prets actifs » (nombre de prêts EN_COURS) et « Prets en attente » (nombre de prêts EN_ATTENTE). La carte d'un client dont le statut actif est faux est visuellement distinguée comme inactive. Chaque carte porte un bouton « Voir detail ».

Source : RG-015-010, RG-015-011, RG-015-014

#### FR-015-06 : Indicateur de prêts sur la carte client

Si le client a au moins un prêt EN_ATTENTE, sa carte affiche l'avertissement « ⚠ {N} pret(s) en attente », où {N} est le nombre de prêts EN_ATTENTE. Sinon, s'il a au moins un prêt EN_COURS, la carte affiche l'indicateur positif « ✓ {N} pret(s) en cours », où {N} est le nombre de prêts EN_COURS. L'avertissement des prêts en attente l'emporte : les deux indicateurs ne s'affichent jamais ensemble.

Source : RG-015-012, RG-015-013

#### FR-015-07 : Panneau de détail d'un client

Quand l'utilisateur clique sur « Voir detail » d'une carte, le système affiche le panneau de détail du client sur la même page, sans changer d'écran. Le panneau affiche le nom complet, l'identifiant précédé de « @ », l'email et le téléphone ; une section « Comptes bancaires » (colonnes « Numero », « Type », « Solde ») ; une section « Prets » (colonnes « Type », « Montant », « Mensualite », « Statut »). Le bouton « Fermer » masque le panneau et revient à la liste du portefeuille, sans changer d'écran.

Source : RG-015-015, RG-015-016, RG-015-017, RG-015-018, RG-015-021

#### FR-015-08 : Client sans compte ou sans prêt

Dans le panneau de détail, si le client sélectionné n'a aucun compte bancaire, la section des comptes affiche « Aucun compte ». S'il n'a aucun prêt, la section des prêts affiche « Aucun pret ».

Source : RG-015-019, RG-015-020

#### FR-015-09 : Valeurs de référence affichées

Les types de compte gérés sont COURANT et EPARGNE. Les types de prêt gérés sont IMMOBILIER, CONSOMMATION et TRAVAUX. Les statuts de prêt gérés sont EN_ATTENTE, EN_COURS, REMBOURSE, DEFAUT et REFUSE. Ces valeurs sont affichées telles quelles (codes, sans libellé traduit) ; les statuts EN_ATTENTE, EN_COURS, REFUSE et REMBOURSE sont affichés en badge, et le statut DEFAUT est affiché sans badge (comportement legacy conservé, voir Notes).

Source : RG-015-022, RG-015-023, RG-015-024

**Notes :**
- RG-015-001 : couverte par l'exigence fusionnée de FEAT-008 (règles voisines).
- RG-015-025 (Suspect, dette legacy) : non reproduite. Le rafraîchissement du portefeuille en cours de session n'est pas offert ; le portefeuille reste chargé une fois par session (FR-015-02). Bug conservé : BUG-052 (bouton de rafraîchissement jamais affiché, conservé tel quel lors de la rénovation).
- RG-015-026 (Suspect, dette legacy) : non reproduite comme exigence. Le statut DEFAUT reste affiché sans badge distinct (FR-015-09). Bug conservé : BUG-053 (statut de prêt « Défaut » sans mise en forme distincte).
- Le libellé de rôle « Conseiller » et les liens de menu « Mon portefeuille » et « Conseiller IA » figurent dans l'en-tête de l'écran (corpus MESSAGES.md), la déconnexion relève de l'exigence fusionnée de FEAT-001.
