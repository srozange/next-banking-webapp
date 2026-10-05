### FEAT-023 — Traitement asynchrone des virements

**Description :** Traitement en arrière-plan, sans écran propre, qui prend le relais d'un virement enregistré depuis l'écran de virement (FEAT-003) par un utilisateur authentifié. Il produit pour l'émetteur une notification dans l'application, dont le type dépend du montant : CONFIRMATION sous le seuil d'alerte, ALERTE entre le seuil d'alerte et le seuil de fraude, FRAUDE au-delà. Les notifications de chaque utilisateur sont plafonnées (20 par défaut). Cas limites marquants : remise impossible au traitement (désormais virement refusé et client informé), virement d'un montant supérieur ou égal au seuil de fraude, dont la suspension avant tout débit dépend de la question ouverte QO-1. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-023-01 : Remise du virement au traitement asynchrone [EN ATTENTE RG-027-003 (a)(b)]

Acquis : une fois le virement enregistré, de sorte que son identifiant soit attribué, le système le remet au traitement asynchrone des virements, avec son identifiant, son montant, son émetteur, le numéro du compte destinataire et son motif. Ouvert : le legacy ne remet que des virements déjà validés ; la remise d'un virement que le contrôle de fraude juge suspect (a) ou dont le contrôle est en erreur (b) dépend de la question QO-1 du PRD. Voir FR-003-13.

Source : RG-023-001

#### FR-023-02 : Échec de la remise : virement refusé et client informé

Si le virement ne peut pas être remis au traitement asynchrone, le système refuse le virement et en informe le client sur l'écran de virement, au lieu de le présenter comme pris en compte ; aucun solde n'est modifié. [ASSUMPTION A-023-1 : texte du message « Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard » ; le corpus ne donne aucun texte] [ASSUMPTION A-023-2 : le refus annule l'enregistrement du virement, son débit, son crédit et ses opérations au relevé] Cette exigence remplace le comportement legacy RG-023-002, où l'échec était ignoré en silence et le virement jamais traité.

Critère d'acceptation (test) : avec le traitement asynchrone rendu indisponible, la soumission d'un virement valide de 100 € affiche le message d'échec, ne redirige pas vers le tableau de bord, laisse les soldes des deux comptes inchangés et n'enregistre aucun virement.

Source : RG-023-002, BUG-073

#### FR-023-03 : Notification selon le montant [EN ATTENTE RG-027-003 (a)]

Acquis : le traitement asynchrone ajoute une seule notification à l'émetteur du virement (FR-002-10), choisie selon le montant, avec le seuil d'alerte (5 000 € par défaut) et le seuil de fraude (10 000 € par défaut) de la configuration de déploiement :
- montant strictement inférieur au seuil d'alerte : CONFIRMATION, « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme » (avec le motif selon FR-003-26) ;
- montant supérieur ou égal au seuil d'alerte et strictement inférieur au seuil de fraude : ALERTE, « Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. » ;
- montant supérieur ou égal au seuil de fraude : FRAUDE, « Virement de {amount} EUR bloque pour verification (seuil anti-fraude : {seuilFraudeEur} EUR). Contactez votre conseiller si vous n etes pas a l origine de cette operation. ».

Ouvert : une notification FRAUDE à la place de la CONFIRMATION pour un virement jugé suspect par le contrôle de fraude, et la véracité des mentions « en attente » et « bloque », dépendent de la question QO-1 du PRD. Même règle que FR-003-14 et FR-025-01.

Source : RG-023-004, RG-023-005, RG-023-006

#### FR-023-04 : Virement au-delà du seuil de fraude [EN ATTENTE RG-027-003 (a)]

Acquis : un virement d'un montant supérieur ou égal au seuil de fraude (10 000 € par défaut) donne lieu à la notification FRAUDE de FR-023-03, qui annonce un virement « bloque pour verification ». Le legacy débitait pourtant le compte source immédiatement : ce désaccord entre le message et le solde n'est pas reproduit. Ouvert, selon la réponse à la question QO-1 (a) du PRD :
- si un virement suspect est bloqué ou suspendu, le virement au-delà du seuil de fraude est suspendu en attente de vérification manuelle avant toute modification de solde (comportement attendu par BUG-076) ;
- s'il est seulement signalé, l'articulation avec le comportement attendu par BUG-076 (exception explicite ou conflit) est à trancher dans QO-1.

Cette exigence remplace le comportement legacy RG-023-009.

Critère d'acceptation (test) : après un virement de 10 000,00 €, le texte de la notification affichée, le statut du virement et le solde du compte source sont cohérents entre eux et conformes à la réponse à QO-1 (a) ; dans l'option « bloqué », le solde du compte source est inchangé tant que la vérification n'a pas eu lieu.

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
