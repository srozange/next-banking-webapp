### FEAT-007 — Demande et suivi de prêts

**Description :** La page Prêts est accessible à tout utilisateur authentifié (CLIENT, CONSEILLER, ADMIN), avec la même interface pour tous ; chacun n'y voit que ses propres emprunts. Elle réunit un simulateur (mensualité, coût total, intérêts et échéancier), un formulaire de demande de prêt et la liste des emprunts de l'utilisateur avec leur échéancier. Une demande valide est enregistrée « En attente d approbation » puis traitée en arrière-plan par le service de scoring crédit (FEAT-024), qui l'approuve (statut « En cours », échéancier généré) ou la refuse (« Refuse par le service credit ») et notifie le client. Cas limites marquants : durée de 301 à 360 mois désormais refusée dès la demande, échéance déjà payée, dernière échéance payée (prêt « Rembourse »), suppression d'un prêt sans confirmation. Scénarios golden master : SC-007-01, SC-007-02.

**Exigences fonctionnelles :**

#### FR-007-01 : Accès à la page Prêts

Si la session n'a pas d'utilisateur authentifié, la demande de la page Prêts redirige vers la page de connexion. Tout utilisateur authentifié, quel que soit son rôle (CLIENT, CONSEILLER, ADMIN), accède à la page Prêts avec la même interface, sans restriction propre au rôle. La page présente les sections « Simulateur de pret », « Demande de pret » et « Mes emprunts ».

Source : RG-007-001, RG-007-002

#### FR-007-02 : Formulaire du simulateur et contrôles

La section « Simulateur de pret » (titre « Simuler un emprunt ») comporte les champs « Montant (EUR) », « Taux annuel (%) » et « Duree (mois) », pré-remplis avec 10 000 EUR, 3,50 % et 36 mois, et le bouton « Calculer ». Si le taux est inférieur à 0 ou supérieur à 30, la simulation est refusée avec le message « Taux invalide (0-30%) ». Si la durée est inférieure ou égale à 0 ou supérieure à 360, la simulation est refusée avec le message « Duree invalide (1-360 mois) ». Le contrôle du montant suit l'exigence fusionnée « Montant invalide » de FEAT-004.

Source : RG-007-004, RG-007-005, RG-007-006

#### FR-007-03 : Calcul de la simulation

Quand la simulation est demandée avec des valeurs valides (montant > 0, taux de 0 à 30, durée de 1 à 360 mois), le système calcule et affiche « Mensualite », « Cout total » et « Total interets ». Si le taux annuel est nul, la mensualité est le montant divisé par le nombre de mois ; sinon, elle suit la formule standard d'annuité constante. Le lien « Voir l echeancier complet ({N} echeances) » donne l'échéancier complet, avec pour chaque échéance les colonnes « # », « Capital », « Interets », « Total » et « Capital restant ». Exemple de référence : 10 000 EUR à 3,5 % sur 36 mois donnent une mensualité de 293.02 EUR, un coût total de 10,548.72 EUR et un total d'intérêts de 548.72 EUR.

Source : RG-007-007, RG-007-008, RG-007-009, RG-007-010

#### FR-007-04 : Types de prêt proposés

Le champ « Type de pret » du formulaire « Nouveau pret » propose, dans cet ordre, « Immobilier », « Automobile », « Consommation », « Travaux » et « Etudiant », avec « Consommation » sélectionné par défaut.

Source : RG-007-011

#### FR-007-05 : Contrôles de la demande de prêt

Le formulaire « Nouveau pret » comporte les champs « Type de pret », « Montant (EUR) », « Taux annuel (%) », « Duree (mois) » et « Objet du pret », et le bouton « Soumettre la demande ». Si le taux est inférieur à 0 ou supérieur à 30, la demande est refusée avec le message « Taux invalide ». Si la durée est inférieure ou égale à 0 ou supérieure à 360, la demande est refusée avec le message « Duree invalide » (voir aussi FR-007-06 pour les durées de 301 à 360 mois). Si l'objet du prêt est vide ou ne contient que des espaces, la demande est refusée avec le message « Veuillez indiquer un motif ». Le contrôle du montant suit l'exigence fusionnée « Montant invalide » de FEAT-004.

Source : RG-007-013, RG-007-014, RG-007-015

#### FR-007-06 : Refus dès la demande d'une durée de 301 à 360 mois

Si la durée saisie dans la demande de prêt est comprise entre 301 et 360 mois inclus, la demande est refusée comme invalide dès la soumission, avec le message « Duree invalide » [ASSUMPTION A-007-1 : le message est celui du contrôle de durée de la demande, « Duree invalide »], aucun prêt n'est enregistré et rien n'est transmis au service de scoring crédit. Cette règle remplace le comportement legacy RG-007-028, où la demande était acceptée puis systématiquement refusée au scoring (RG-007-027) sans message différencié. Le simulateur conserve la plage de 1 à 360 mois (FR-007-02).

Critère d'acceptation (test) : une demande de prêt valide en tout point sauf une durée de 301 mois (puis de 360 mois) est refusée à la soumission avec le message « Duree invalide », et aucune ligne n'apparaît dans « Emprunts en cours » ; une demande identique avec 300 mois est acceptée et transmise.

Source : RG-007-028, BUG-025, RG-007-014

#### FR-007-07 : Enregistrement et transmission d'une demande valide

Quand une demande valide est soumise, le système enregistre un prêt au statut EN_ATTENTE (libellé « En attente d approbation »), avec la mensualité, le coût total, le total des intérêts, une date de début égale au premier jour du mois en cours et une date de fin égale à la date de début plus la durée en mois civils, puis le transmet au service de scoring crédit pour un traitement asynchrone (FEAT-024). La page affiche « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) » et rafraîchit la liste des emprunts. L'échec de la transmission est traité par l'exigence FR-024-02.

Source : RG-007-017, RG-007-018, RG-007-019

#### FR-007-08 : Liste des emprunts

La section « Mes emprunts » (titre « Emprunts en cours ») affiche uniquement les prêts de l'utilisateur connecté, triés par date de début décroissante, avec les colonnes « Type », « Montant », « Taux », « Duree », « Mensualite », « Total interets », « Statut », « Echeancier » et « Action ». Les statuts sont affichés avec les libellés « En attente d approbation », « En cours », « Rembourse », « Refuse par le service credit » et « En defaut ». Pour un prêt au statut EN_ATTENTE, la colonne « Echeancier » affiche « En cours de traitement... ». Si l'utilisateur n'a aucun prêt, la page affiche « Aucun emprunt enregistre ».

Source : RG-007-020, RG-007-037

#### FR-007-09 : Règles de scoring crédit

Le service de scoring crédit approuve le prêt s'il respecte les règles suivantes, et le refuse sinon : refus si la durée dépasse 300 mois, quels que soient le type et le montant ; refus si le montant dépasse 25 000 EUR pour CONSOMMATION, 40 000 EUR pour AUTO, 80 000 EUR pour TRAVAUX, 20 000 EUR pour ETUDIANT, 600 000 EUR pour IMMOBILIER. Ces règles sont détaillées dans FR-024-03 à FR-024-05.

Source : RG-007-021, RG-007-022, RG-007-023, RG-007-024, RG-007-025, RG-007-026, RG-007-027

#### FR-007-10 : Approbation, refus et notification

Quand un prêt est approuvé, son statut passe de EN_ATTENTE à EN_COURS et un échéancier de N échéances mensuelles est généré (N = durée en mois). Quand un prêt est refusé, son statut passe de EN_ATTENTE à REFUSE. Dans les deux cas, le client est notifié dans le centre de notifications de l'application : « Votre demande de pret {type} de {montant} EUR a ete approuvee par le service credit. » (type CONFIRMATION) ou « Votre demande de pret {type} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). » (type ALERTE), où {type} est le libellé du type de prêt. Une demande d'approbation ou de refus portant sur un prêt qui n'est pas au statut EN_ATTENTE est ignorée sans erreur ni changement.

Source : RG-007-029, RG-007-030, RG-007-031, RG-007-032

#### FR-007-11 : Consultation de l'échéancier d'un prêt

Pour un prêt au statut EN_COURS ou REMBOURSE, la colonne « Echeancier » affiche le bouton « Voir », qui ouvre la section « Echeancier du pret selectionne » (titre « Detail des echeances »). Pour chaque échéance, elle affiche « # », « Date echeance » (jj/MM/aaaa), « Capital », « Interets », « Total », « Capital restant » et « Statut », ce dernier valant « Paye » pour une échéance payée et « En attente » pour une échéance non payée [ASSUMPTION A-007-2 : libellés du relevé des messages du code, « Paye » / « En attente », retenus de préférence à « Paye / En cours » de la règle].

Source : RG-007-038, RG-007-039

#### FR-007-12 : Paiement d'une échéance

Pour une échéance non payée, la colonne « Action » affiche le bouton « Marquer paye ». Son activation marque l'échéance comme payée, enregistre la date de paiement et affiche « Echeance marquee comme payee ». Une échéance déjà payée n'est pas payée une seconde fois, sans message d'erreur. Quand toutes les échéances du prêt sont payées, le prêt passe au statut REMBOURSE (« Rembourse »).

Source : RG-007-033, RG-007-034, RG-007-035, RG-007-036

#### FR-007-13 : Suppression d'un prêt

Chaque ligne de la liste des emprunts porte un bouton « Supprimer ». Son activation supprime définitivement le prêt et toutes ses échéances, sans demande de confirmation.

Source : RG-007-040

**Feature-specific NFRs :** En cas d'erreur inattendue lors de la simulation, de la demande, du paiement ou de la suppression, aucun message technique interne n'est montré à l'utilisateur (le legacy affiche « Erreur simulation : {message technique} », « Erreur : {message technique} » et « Erreur suppression : {message technique} », non reproduits).

**Notes :**
- RG-007-003 : couverte par l'exigence fusionnée de FEAT-004 (règles voisines).
- RG-007-012 : couverte par l'exigence fusionnée de FEAT-004 (règles voisines).
- RG-007-016 (dette legacy) : non reproduite. Le legacy ne contrôle pas la longueur de l'objet du prêt (255 caractères au plus en stockage) ; un dépassement provoque une erreur technique. BUG-024 (objet de prêt trop long, erreur technique affichée) et BUG-026 (objet de plus de 255 caractères en contournant la saisie) sont des bugs conservés : leur correction n'est pas demandée.
- RG-007-028 (Suspect, dette legacy) : remplacée par FR-007-06 (correction BUG-025).
- Le statut DEFAUT (« En defaut ») existe dans la liste des statuts mais aucune transition n'y mène dans le legacy.
- Le corpus donne « Aucun emprunt enregistré » dans la règle RG-007-020 et « Aucun emprunt enregistre » dans le relevé des messages ; le relevé des messages fait foi.
- Le texte informatif legacy sous le bouton « Soumettre la demande » et le bandeau technique nomment des technologies : ils ne sont pas reproduits.
- L'approbation ou le refus manuel d'un prêt par un ADMIN relève de l'écran d'administration (autre fonctionnalité) ; la garde de FR-007-10 s'y applique aussi.
