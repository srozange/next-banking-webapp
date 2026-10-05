### FEAT-023 — Traitement asynchrone des virements

**Description :** Traitement en arrière-plan, sans écran propre, qui prend le relais d'un virement exécuté depuis l'écran de virement (FEAT-003), immédiatement ou après sa libération par l'administrateur. Dans le legacy, ce traitement produisait la notification du virement, dont le type dépendait du seul montant. Désormais, la notification unique de chaque virement est produite au moment de la décision du contrôle de fraude, selon l'issue du contrôle puis le montant (CONFIRMATION, ALERTE ou FRAUDE, FR-023-03) ; un virement libéré passe par ce traitement sans nouvelle notification. Les notifications de chaque utilisateur sont plafonnées (20 par défaut). Cas limites marquants : remise impossible au traitement (désormais la validation du virement échoue et le client est informé), virement d'un montant supérieur ou égal au seuil de fraude, suspendu avant tout débit (décision RG-027-003, D2). Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-023-01 : Remise du virement au traitement asynchrone [ÉVOLUTION — décision RG-027-003 D1, D5]

Une fois le virement exécuté (contrôle de fraude passé) ou libéré par l'administrateur (état LIBÉRÉ, solde contrôlé à la libération, FR-016-19), et son identifiant attribué, le système le remet au traitement asynchrone des virements, avec son identifiant, son montant, son émetteur, le numéro du compte destinataire et son motif. Un virement suspendu, rejeté ou refusé pour erreur du contrôle n'est pas remis à ce traitement. La remise ne produit aucune notification : la notification unique du virement, y compris la notification FRAUDE d'un virement suspendu (FR-027-04), est produite au moment de la décision du contrôle de fraude (FR-023-03, FR-002-10) ; un virement libéré est remis à ce traitement sans nouvelle notification ; le traitement effectue alors son débit et son crédit et le passe à l'état EXÉCUTÉ (FR-027-16, décision de finalisation B6) ; si le solde du compte source est devenu insuffisant au moment de ce débit, le traitement n'effectue aucun débit ni crédit et passe le virement à l'état REJETÉ, sans notification (FR-027-16, décision de finalisation B7). Voir FR-003-13.

Source : RG-023-001, RG-027-003

#### FR-023-02 : Échec de la remise : la validation du virement échoue et le client est informé

La remise au traitement asynchrone est enregistrée avec la validation du virement, comme une écriture unique : sa publication suit cet enregistrement. Si la remise ne peut pas être enregistrée, la validation du virement échoue : le virement n'est pas enregistré, rien n'est débité ni crédité, aucune opération n'apparaît au relevé, et le client en est informé sur l'écran de virement, au lieu de voir le virement présenté comme pris en compte. [ASSUMPTION A-023-1 : texte du message « Le virement n'a pas pu etre pris en compte, veuillez reessayer plus tard » ; le corpus ne donne aucun texte] [ASSUMPTION A-023-3 : la remise est enregistrée dans la même écriture que la validation, et publiée ensuite ; son échec fait échouer la validation, si bien qu'aucun état n'est ajouté à FR-027-16, le virement n'existant pas ; pour une libération (FR-016-19), c'est la libération qui échoue : le virement reste SUSPENDU et l'administrateur voit le même message] Cette exigence remplace le comportement legacy RG-023-002, où l'échec était ignoré en silence et le virement jamais traité.

Critère d'acceptation (test) : avec l'enregistrement de la remise rendu impossible, la soumission d'un virement valide de 100 € affiche le message d'échec, ne redirige pas vers le tableau de bord, laisse les soldes des deux comptes inchangés, n'ajoute aucune opération et n'enregistre aucun virement.

Source : RG-023-002, BUG-073

#### FR-023-03 : Notification selon l'issue du contrôle et le montant [ÉVOLUTION — décision RG-027-003 D1, D2]

L'émetteur reçoit une seule notification par virement (FR-002-10). Elle est produite au moment de la décision du contrôle de fraude (événement de décision, addendum A.7), et non par la remise au traitement asynchrone de FR-023-01. Elle est choisie selon l'issue du contrôle puis le montant, avec le seuil d'alerte (5 000 € par défaut) et le seuil de fraude (10 000 € par défaut) de la configuration de déploiement :
- virement suspendu : FRAUDE, avec l'avis de suspension de FR-027-04 ;
- virement exécuté d'un montant strictement inférieur au seuil d'alerte : CONFIRMATION, « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme » (avec le motif selon FR-003-26) ;
- virement exécuté d'un montant supérieur ou égal au seuil d'alerte : ALERTE, « Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. » (texte legacy conservé pour la démonstration, FR-003-10, décision B1 révisée).

Un virement exécuté après libération passe par le traitement asynchrone (FR-023-01), mais ne produit aucune notification (FR-002-10). Cette règle modifie RG-023-004 : FRAUDE suit la suspension (score cumulé strictement supérieur à 75 ou montant supérieur ou égal au seuil de fraude) et non le seul montant ; son texte legacy « Virement de {amount} EUR bloque pour verification (seuil anti-fraude : …) … » est remplacé par l'avis de FR-027-04. Elle modifie RG-023-005 : l'ALERTE ne concerne plus que les virements exécutés de 5 000 € à 9 999,99 €, avec son texte legacy. Même règle que FR-003-14 et FR-025-01.

Critère d'acceptation (test) : chaque virement exécuté ou suspendu ajoute une notification dès la décision du contrôle, avant toute remise au traitement asynchrone ; la libération d'un virement suspendu, suivie de son traitement asynchrone, n'en ajoute aucune.

Source : RG-023-004, RG-023-005, RG-023-006, RG-027-003

#### FR-023-04 : Virement au-delà du seuil de fraude [ÉVOLUTION — décision RG-027-003 D2]

Un virement d'un montant supérieur ou égal au seuil de fraude (10 000 € par défaut, configuration de déploiement) est suspendu avant toute modification de solde, quel que soit son score, en attente de la revue de l'administrateur (FR-016-19). Le client reçoit l'unique notification FRAUDE avec l'avis de suspension de FR-027-04. Le legacy débitait immédiatement le compte source tout en annonçant un virement « bloque pour verification » ; ce comportement n'est pas reproduit. Cette exigence remplace le comportement legacy RG-023-009.

Critère d'acceptation (test) : après un virement de 10 000,00 € de score cumulé nul, le virement est à l'état SUSPENDU, le solde du compte source est inchangé et une seule notification FRAUDE porte l'heure butoir ; un virement de 9 999,99 € de score cumulé inférieur ou égal à 75 est exécuté.

Source : RG-023-009, BUG-076

#### FR-023-05 : Plafond de notifications par utilisateur

Le système conserve au plus un nombre de notifications par utilisateur fixé par la configuration de déploiement, 20 par défaut ; une nouvelle notification est placée en tête de liste et, lorsque la limite est atteinte, la plus ancienne est supprimée. Même règle que FR-025-04 et FR-002-09.

Source : RG-023-010

#### FR-023-06 : Conservation des notifications

Les notifications sont conservées après un redémarrage du serveur, selon FR-002-09. Écart dans le corpus : RG-023-011 (Confirmé) décrit des notifications en mémoire, perdues au redémarrage ; cette exigence remplace le comportement legacy RG-023-011, correction BUG-006 (FEAT-002, FR-002-09).

Source : RG-023-011, BUG-006

**Notes :**
- RG-023-002 et RG-023-009 (Suspect, dette legacy) sont couvertes par les FR de correction FR-023-02 (BUG-073) et FR-023-04 (BUG-076) ; leur comportement legacy n'est pas reproduit.
- RG-023-003 (dette legacy) : non reproduite. BUG-074 (conservé, non corrigé) : un message de virement dans un format inattendu est écarté sans être dirigé vers une file de récupération.
- RG-023-008 (dette legacy) : non reproduite. BUG-075 (conservé, non corrigé) : une erreur pendant le traitement d'un virement écarte le message sans le diriger vers une file de récupération.
- RG-023-007 : couverte par l'exigence fusionnée de FEAT-003 (règles voisines), FR-003-26.
