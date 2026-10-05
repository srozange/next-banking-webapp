### FEAT-025 — Système de notifications internes

**Description :** Les notifications internes informent l'utilisateur authentifié (CLIENT surtout ; tout rôle peut en recevoir) dans l'application, sur le centre de notifications du tableau de bord. Elles sont produites par le traitement des virements (CONFIRMATION, ALERTE ou FRAUDE selon le montant), par le traitement des demandes de prêt (approbation ou refus) et par le contrôle périodique des soldes faibles, qui, désormais, atteint réellement le client. L'utilisateur voit ses notifications de la plus récente à la plus ancienne, peut toutes les marquer lues ou toutes les effacer ; leur nombre est plafonné par utilisateur (20 par défaut). Cas limites marquants : plafond atteint (la plus ancienne disparaît), motif vide, solde strictement inférieur à 100 €. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-025-01 : Notification de virement selon l'issue du contrôle et le montant [ÉVOLUTION — décision RG-027-003 D1]

Le traitement d'un virement ajoute à l'émetteur une seule notification (FR-002-10), choisie selon l'issue du contrôle de fraude puis le montant, avec le seuil d'alerte (5 000 € par défaut) et le seuil de fraude (10 000 € par défaut) de la configuration de déploiement :
- virement suspendu : FRAUDE, avec l'avis de suspension de FR-027-04 ;
- virement exécuté d'un montant strictement inférieur au seuil d'alerte : CONFIRMATION, « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme », avec le montant et le numéro du compte destinataire ;
- virement exécuté d'un montant supérieur ou égal au seuil d'alerte : ALERTE, « Virement de {amount} EUR en attente de validation - transaction importante, verification en cours. » (voir FR-003-10).

Même règle que FR-003-14 et FR-023-03.

Source : RG-025-001, RG-025-002, RG-025-003

#### FR-025-02 : Motif dans la notification de confirmation

Lorsque la notification est de type CONFIRMATION et que le motif est renseigné (ni absent, ni vide, ni composé uniquement d'espaces), le message devient « Virement de {amount} EUR vers le compte {targetAccountNumber} confirme (motif : {motif}) ». Même règle que l'exigence fusionnée FR-003-26.

Source : RG-025-004

#### FR-025-03 : Notifications de décision sur une demande de prêt

Lorsque le traitement des demandes de prêt (FEAT-024) approuve une demande, le système ajoute au demandeur une notification CONFIRMATION « Votre demande de pret {loanType.libelle} de {montant} EUR a ete approuvee par le service credit. ». Lorsqu'il la refuse, il ajoute une notification ALERTE « Votre demande de pret {loanType.libelle} de {montant} EUR a ete refusee (plafond depasse ou duree excessive). ».

Source : RG-025-005, RG-025-006

#### FR-025-04 : Plafond de notifications par utilisateur

Lorsqu'une notification est ajoutée et que le nombre de notifications de l'utilisateur dépasserait la limite de la configuration de déploiement (20 par défaut), le système supprime la plus ancienne, pour que la liste reste dans la limite. Même règle que FR-023-05 et FR-002-09.

Source : RG-025-007

#### FR-025-05 : Affichage sur le tableau de bord

Le tableau de bord affiche, dans la section « Centre de notifications », les notifications de l'utilisateur authentifié, de la plus récente à la plus ancienne. Chaque notification affiche son type (« CONFIRMATION », « ALERTE » ou « FRAUDE », avec les icônes ✅, ⚠️ et 🚨), son message et son horodatage au format dd/MM/yyyy HH:mm:ss ; une notification non lue porte l'indication « Non lu ». Les autres textes du centre de notifications (état vide, badge) sont fixés par FR-002-07 et FR-002-08 ; ses étiquettes techniques ne sont pas reprises (voir FEAT-002, A-002-4).

Source : RG-025-008

#### FR-025-06 : Tout marquer comme lu

Lorsque l'utilisateur a au moins une notification non lue, le badge « {N} nouvelle(s) » et le bouton « Tout marquer lu » sont affichés ; un clic sur ce bouton marque comme lues toutes les notifications de l'utilisateur, et le badge disparaît.

Source : RG-025-009

#### FR-025-07 : Tout effacer

Lorsque l'utilisateur a au moins une notification, le bouton « Effacer tout » est affiché ; un clic sur ce bouton supprime toutes les notifications de l'utilisateur.

Source : RG-025-010

#### FR-025-08 : Alerte de solde faible affichée au client

Toutes les 30 minutes (NFR-BATCH-01), le système contrôle les comptes des clients actifs ; pour tout compte dont le solde est strictement inférieur à 100 €, il ajoute au client une notification de priorité haute, affichée dans le centre de notifications de son tableau de bord, de titre « Solde insuffisant sur votre compte » et de message « Votre solde ({currentBalance} €) est passé sous le seuil d'alerte de {lowBalanceThreshold} €. Veuillez approvisionner votre compte. ». [ASSUMPTION A-025-1 : la notification est affichée avec le type ALERTE et suit le même plafond que les autres notifications (FR-025-04)] [ASSUMPTION A-025-2 : une seule notification par compte tant que le solde reste sous le seuil ; une nouvelle notification n'est produite qu'après un retour du solde à 100 € ou plus, pour ne pas répéter l'alerte toutes les 30 minutes] Cette exigence remplace le comportement legacy RG-025-011, où la notification était enregistrée mais jamais affichée.

Critère d'acceptation (test) : un client actif dont le compte a un solde de 99,99 € voit, après le passage du contrôle périodique, la notification « Solde insuffisant sur votre compte » dans le centre de notifications de son tableau de bord ; un client dont le solde est de 100,00 € ne la reçoit pas.

Source : RG-025-011, BUG-079

**Notes :**
- RG-025-011 (Suspect, dette legacy) est couverte par la FR de correction FR-025-08 (BUG-079) ; son comportement legacy n'est pas reproduit.
- RG-025-012 (dette legacy) : non reproduite. BUG-080 (conservé, non corrigé) : le seuil de solde faible reste fixé à 100 € et n'est pas configurable.
- RG-025-013 (dette legacy) : non reproduite. BUG-081 (conservé, non corrigé) : l'ancienne méthode d'envoi de notification, obsolète, sans appelant et sans effet, n'est pas reprise.
- RG-025-014 (dette legacy) : non reproduite. BUG-082 (conservé, non corrigé) : la priorité haute des notifications de transaction de plus de 1 000 € n'est jamais appliquée dans le legacy ; les notifications de virement suivent FR-025-01.
- Le legacy a deux circuits de notifications ; seul le circuit affiché est repris, plus l'alerte de solde faible (FR-025-08). Les notifications de prêt du circuit non affiché (« Prêt approuvé », « Prêt refusé ») ne sont pas reprises : seules celles de FR-025-03 étaient visibles.
