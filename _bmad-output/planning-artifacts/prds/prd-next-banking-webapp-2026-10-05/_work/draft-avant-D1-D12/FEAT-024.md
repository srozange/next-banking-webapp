### FEAT-024 — Traitement asynchrone approbation prêts

**Description :** Traitement en arrière-plan, sans écran propre, qui décide du sort d'une demande de prêt soumise depuis la page Prêts (FEAT-007) par un utilisateur authentifié (CLIENT, CONSEILLER ou ADMIN). La demande enregistrée au statut EN_ATTENTE est transmise à un service de scoring crédit, qui l'approuve ou la refuse selon la durée et un plafond de montant par type de prêt ; le prêt passe alors à EN_COURS (avec son échéancier) ou à REFUSE, et l'emprunteur reçoit une notification dans l'application. Cas limites marquants : durée supérieure à 300 mois, montant exactement égal au plafond (approuvé), type de prêt non reconnu (refusé), message reçu pour un prêt qui n'est plus EN_ATTENTE (ignoré), transmission impossible (demande refusée et client informé, au lieu d'un prêt bloqué en attente). Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-024-01 : Transmission de la demande au service de scoring crédit

Quand un utilisateur soumet une demande de prêt valide, le système enregistre le prêt au statut EN_ATTENTE, de sorte que son identifiant soit attribué et lisible par le service de scoring crédit, puis transmet la demande à ce service pour un traitement asynchrone, avant de rendre la main à l'utilisateur.

Source : RG-024-001, RG-024-002

#### FR-024-02 : Échec de la transmission de la demande

Si la demande de prêt ne peut pas être transmise au service de scoring crédit, le système refuse la demande et en informe l'utilisateur sur la page Prêts par un message d'erreur, au lieu du message de succès de FR-007-07 [ASSUMPTION A-024-1 : texte « Service credit temporairement indisponible, veuillez reessayer plus tard »] ; le prêt n'est pas laissé au statut EN_ATTENTE [ASSUMPTION A-024-2 : le prêt enregistré est annulé et n'apparaît pas dans la liste des emprunts]. Cette règle remplace le comportement legacy RG-024-020, où la demande était abandonnée sans prévenir l'utilisateur et restait indéfiniment en attente.

Critère d'acceptation (test) : avec un service de scoring crédit rendu indisponible, la soumission d'une demande valide affiche un message d'erreur d'indisponibilité, n'affiche pas « Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes) », et aucun prêt EN_ATTENTE n'existe pour l'utilisateur après la soumission.

Source : RG-024-020, BUG-077

#### FR-024-03 : Refus pour durée excessive

Si la durée du prêt dépasse 300 mois, le service de scoring crédit refuse le prêt, quels que soient son type et son montant. Ce contrôle est évalué avant les plafonds par type (FR-024-04).

Source : RG-024-003

#### FR-024-04 : Plafond de montant par type de prêt

Pour une durée de 300 mois au plus, le service de scoring crédit approuve le prêt si son montant est inférieur ou égal au plafond de son type, et le refuse s'il le dépasse : CONSOMMATION 25 000 EUR, AUTO 40 000 EUR, TRAVAUX 80 000 EUR, ETUDIANT 20 000 EUR, IMMOBILIER 600 000 EUR. Un montant exactement égal au plafond est approuvé.

Source : RG-024-004, RG-024-005, RG-024-006, RG-024-007, RG-024-008, RG-024-009, RG-024-010, RG-024-011, RG-024-012, RG-024-013

#### FR-024-05 : Refus d'un type de prêt non reconnu

Si le type du prêt est absent ou ne fait pas partie des cinq types connus (IMMOBILIER, AUTO, CONSOMMATION, TRAVAUX, ETUDIANT), le service de scoring crédit refuse le prêt, quel que soit son montant. Cette règle remplace le comportement legacy RG-024-021, qui approuvait un tel prêt si son montant était inférieur ou égal à 25 000 EUR.

Critère d'acceptation (test) : une demande de 10 000 EUR sur 60 mois, transmise au service de scoring crédit avec un type absent, aboutit au statut REFUSE et à la notification ALERTE de refus.

Source : RG-024-021, BUG-078

#### FR-024-06 : Effets de l'approbation

Quand le service de scoring crédit approuve un prêt, le statut du prêt passe à EN_COURS, l'échéancier complet est généré (une échéance par mois de la durée), et l'emprunteur reçoit une notification de type CONFIRMATION : « Votre demande de pret {libelle} de {montant} EUR a ete approuvee par le service credit. », où {libelle} est « Immobilier », « Automobile », « Consommation », « Travaux » ou « Etudiant », et {montant} le montant en EUR.

Source : RG-024-014, RG-024-016

#### FR-024-07 : Effets du refus

Quand le service de scoring crédit refuse un prêt, le statut du prêt passe à REFUSE et l'emprunteur reçoit une notification de type ALERTE : « Votre demande de pret {libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). »

Source : RG-024-015, RG-024-017

#### FR-024-08 : Demande reçue pour un prêt déjà traité

Quand le service de scoring crédit reçoit une demande d'approbation pour un prêt dont le statut n'est pas EN_ATTENTE, il l'ignore, sans modifier aucun état ni envoyer de notification.

Source : RG-024-018

#### FR-024-09 : Nombre maximal de notifications par utilisateur

Le système conserve au plus 20 notifications par utilisateur (valeur par défaut, configurable) ; quand ce nombre est atteint, l'ajout d'une notification supprime la plus ancienne.

Source : RG-024-019

**Notes :**
- RG-024-020 (Suspect, dette legacy) : remplacée par FR-024-02 (correction BUG-077).
- RG-024-021 (Suspect, dette legacy) : remplacée par FR-024-05 (correction BUG-078).
- Les règles de scoring sont aussi portées par FEAT-007 (FR-007-09, FR-007-10), avec les mêmes valeurs. Avec la correction BUG-025 (FR-007-06), une demande de 301 à 360 mois n'atteint plus le service de scoring ; FR-024-03 reste une garde.
- Les notifications s'affichent dans le centre de notifications du tableau de bord (FEAT-002).
