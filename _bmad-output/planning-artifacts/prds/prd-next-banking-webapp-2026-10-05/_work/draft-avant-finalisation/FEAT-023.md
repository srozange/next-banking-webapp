### FEAT-023 — Traitement asynchrone des virements

**Description :** Traitement en arrière-plan, sans écran propre, qui prend le relais d'un virement enregistré depuis l'écran de virement (FEAT-003) par un utilisateur authentifié. Il produit pour l'émetteur une notification dans l'application, dont le type dépend du montant : CONFIRMATION sous le seuil d'alerte, ALERTE entre le seuil d'alerte et le seuil de fraude, FRAUDE au-delà. Les notifications de chaque utilisateur sont plafonnées (20 par défaut). Cas limites marquants : remise impossible au traitement (désormais virement refusé et client informé), virement d'un montant supérieur ou égal au seuil de fraude, suspendu avant tout débit (décision RG-027-003, D2). Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-023-01 : Remise du virement au traitement asynchrone [ÉVOLUTION — décision RG-027-003 D1, D5]

Une fois le virement exécuté (contrôle de fraude passé, ou libération par l'administrateur), et son identifiant attribué, le système le remet au traitement asynchrone des virements, avec son identifiant, son montant, son émetteur, le numéro du compte destinataire et son motif. Un virement suspendu, rejeté ou refusé pour erreur du contrôle n'est pas remis à ce traitement. Voir FR-003-13.

Source : RG-023-001, RG-027-003

#### FR-023-02 : Échec de la remise : virement refusé et client informé

Si le virement ne peut pas être remis au traitement asynchrone, le système refuse le virement et en informe le client sur l'écran de virement, au lieu de le présenter comme pris en compte ; aucun solde n'est modifié. [ASSUMPTION A-023-1 : texte du message « Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard » ; le corpus ne donne aucun texte] [ASSUMPTION A-023-2 : le refus annule l'enregistrement du virement, son débit, son crédit et ses opérations au relevé] Cette exigence remplace le comportement legacy RG-023-002, où l'échec était ignoré en silence et le virement jamais traité.

Critère d'acceptation (test) : avec le traitement asynchrone rendu indisponible, la soumission d'un virement valide de 100 € affiche le message d'échec, ne redirige pas vers le tableau de bord, laisse les soldes des deux comptes inchangés et n'enregistre aucun virement.

Source : RG-023-002, BUG-073

#### FR-023-03 : Notification selon l'issue du contrôle et le montant [ÉVOLUTION — décision RG-027-003 D1]

Le traitement asynchrone ajoute à l'émetteur une seule notification par virement (FR-002-10), choisie selon l'issue du contrôle de fraude puis le montant, avec le seuil d'alerte (5 000 € par défaut) et le seuil de fraude (10 000 € par défaut) de la configuration de déploiement :
- virement suspendu : FRAUDE, avec l'avis de suspension de FR-027-04 ;
- virement exécuté d'un montant strictement inférieur au seuil d'alerte : CONFIRMATION, « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme » (avec le motif selon FR-003-26) ;
- virement exécuté d'un montant supérieur ou égal au seuil d'alerte : ALERTE, « Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. » (voir FR-003-10).

Même règle que FR-003-14 et FR-025-01.

Source : RG-023-004, RG-023-005, RG-023-006

#### FR-023-04 : Virement au-delà du seuil de fraude [ÉVOLUTION — décision RG-027-003 D2]

Un virement d'un montant supérieur ou égal au seuil de fraude (10 000 € par défaut, configuration de déploiement) est suspendu avant toute modification de solde, quel que soit son score, en attente de la revue de l'administrateur (FR-016-19). Le client reçoit l'unique notification FRAUDE avec l'avis de suspension de FR-027-04. Le legacy débitait immédiatement le compte source tout en annonçant un virement « bloque pour verification » ; ce comportement n'est pas reproduit. Cette exigence remplace le comportement legacy RG-023-009.

Critère d'acceptation (test) : après un virement de 10 000,00 € de score cumulé nul, le virement est à l'état SUSPENDU, le solde du compte source est inchangé et une seule notification FRAUDE porte l'heure butoir ; un virement de 9 999,99 € de score cumulé inférieur ou égal à 75 est exécuté.

Source : RG-023-009, BUG-076

#### FR-023-05 : Plafond de notifications par utilisateur

Le système conserve au plus un nombre de notifications par utilisateur fixé par la configuration de déploiement, 20 par défaut ; une nouvelle notification est placée en tête de liste et, lorsque la limite est atteinte, la plus ancienne est supprimée. Même règle que FR-025-04 et FR-002-09.

Source : RG-023-010

#### FR-023-06 : Conservation des notifications

Les notifications sont conservées après un redémarrage du serveur, selon FR-002-09. Écart dans le corpus : RG-023-011 (Confirmé) décrit des notifications en mémoire, perdues au redémarrage ; le bug à corriger BUG-006 (FEAT-002) remplace ce comportement legacy.

Source : RG-023-011

**Notes :**
- RG-023-002 et RG-023-009 (Suspect, dette legacy) sont couvertes par les FR de correction FR-023-02 (BUG-073) et FR-023-04 (BUG-076) ; leur comportement legacy n'est pas reproduit.
- RG-023-003 (dette legacy) : non reproduite. BUG-074 (conservé, non corrigé) : un message de virement dans un format inattendu est écarté sans être dirigé vers une file de récupération.
- RG-023-008 (dette legacy) : non reproduite. BUG-075 (conservé, non corrigé) : une erreur pendant le traitement d'un virement écarte le message sans le diriger vers une file de récupération.
- RG-023-007 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-26.
