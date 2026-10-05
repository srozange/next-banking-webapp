### FEAT-021 — Exécution des virements programmés

**Description :** Traitement automatique sans écran, qui exécute les virements programmés créés par les clients (CLIENT) dans l'écran des virements programmés (FEAT-004). Toutes les 5 minutes, il sélectionne les virements actifs arrivés à échéance, soumet chaque échéance au contrôle de fraude du virement immédiat (décision de finalisation B2), débite le compte source, crédite le compte bénéficiaire interne, enregistre les opérations visibles au relevé (FEAT-005) et au tableau de bord, puis fait avancer la date de prochaine exécution selon la fréquence. Les cas limites marquants sont le solde insuffisant (virement retenté au passage suivant), le compte bénéficiaire absent de la banque (désormais refusé avant tout débit), la dérive du calendrier (désormais calculé depuis la date prévue) et l'échec d'un virement, qui ne doit pas affecter les autres. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-021-01 : Déclenchement périodique

Le système déclenche l'exécution des virements programmés dus toutes les 5 minutes, à toute heure, et journalise « Verification virements programmes dus » à chaque passage. Le déclenchement ne conserve pas d'état propre : après un redémarrage du serveur, il reprend au prochain cycle de 5 minutes, et les virements arrivés à échéance pendant l'arrêt sont exécutés à cette reprise (FR-021-02), soit au plus 5 minutes de retard après le démarrage.

Source : RG-021-001, RG-021-002

#### FR-021-02 : Sélection des virements dus

À chaque passage, le système sélectionne tous les virements programmés actifs dont la date de prochaine exécution est antérieure ou égale à l'instant courant, triés par date de prochaine exécution croissante, et les traite l'un après l'autre. Les virements programmés inactifs ne sont pas sélectionnés ; cet état ne doit pas être confondu avec l'état SUSPENDU d'un virement (FR-027-16).

Source : RG-021-003

#### FR-021-03 : Débit du compte source et crédit du compte bénéficiaire

Pour un virement dû dont le solde du compte source est suffisant et que le contrôle de fraude laisse passer (FR-021-10), le système débite le compte source du montant du virement et crédite du même montant le compte de la banque dont le numéro correspond au compte bénéficiaire, sauf si le compte bénéficiaire n'est pas un compte de la banque (FR-021-04, aucun débit) ou si le solde est insuffisant (FR-004-12, aucun débit, virement retenté au passage suivant). Le système journalise « Virement programme execute id={id} montant={montant} ».

Source : RG-021-005, RG-021-006

#### FR-021-04 : Compte bénéficiaire hors de la banque refusé avant tout débit

Si le numéro de compte bénéficiaire ne correspond à aucun compte de la banque, le système rejette le virement avant tout débit : aucun débit, aucune opération enregistrée, et le client est informé par une notification dans l'application, avec un message d'erreur clair [ASSUMPTION A-021-1 : texte proposé « Virement programme vers {libellé bénéficiaire} refuse : compte destinataire introuvable », non fixé par le corpus]. Ce contrôle précède celui du solde (FR-004-12) [ASSUMPTION A-021-2 : le corpus ne donne pas l'ordre ; le refus définitif passe avant le report pour solde insuffisant]. Comme la création vers un tel compte est déjà refusée (FR-004-13), ce cas ne concerne que les virements existants dans les données de référence ou dont le compte bénéficiaire a disparu ; le virement programmé est alors désactivé (état inactif), pour ne pas être refusé et notifié à chaque passage [ASSUMPTION A-021-3 : désactivation du virement programmé après le refus ; le corpus ne dit pas ce que devient le virement]. Remplace le comportement legacy RG-021-007 (compte source débité sans crédit nulle part).

Critère d'acceptation (test) : avec un virement programmé actif et dû vers un numéro de compte absent de la banque, après un passage du traitement automatique, le solde du compte source est inchangé, aucune opération n'est enregistrée sur ce compte, le client a reçu une notification de refus, et le virement est à l'état inactif.

Source : RG-021-007, BUG-068

#### FR-021-05 : Opération DEBIT sur le compte source

Lors de l'exécution, le système enregistre sur le compte source une opération DEBIT dont le libellé est « Virement programme vers {libellé bénéficiaire} », ou « Virement programme vers {libellé bénéficiaire} - {motif} » si un motif a été saisi.

Source : RG-021-008

#### FR-021-06 : Opération CREDIT sur le compte bénéficiaire interne

Lors de l'exécution vers un compte de la banque, le système enregistre sur le compte bénéficiaire une opération CREDIT dont le libellé est « Virement programme de {numéro du compte source} », ou « Virement programme de {numéro du compte source} - {motif} » si un motif a été saisi.

Source : RG-021-009

#### FR-021-07 : Suivi des exécutions

Après une exécution réussie, le système enregistre l'horodatage de la dernière exécution et augmente de 1 le nombre d'exécutions du virement programmé (affiché dans la colonne « Nb executions », FR-004-03).

Source : RG-021-010

#### FR-021-08 : Calcul de la prochaine échéance sans dérive

Après une exécution réussie, le système calcule la date de prochaine exécution à partir de la date d'échéance prévue (et non de l'heure réelle d'exécution), en ajoutant 7 jours calendaires pour « Hebdomadaire », 1 mois calendaire pour « Mensuel », 2 mois pour « Bimestriel », 3 mois pour « Trimestriel », 6 mois pour « Semestriel » et 1 an pour « Annuel ». Remplace le comportement legacy RG-021-011 (calcul depuis l'heure réelle d'exécution, qui faisait glisser le calendrier à chaque cycle). [ASSUMPTION A-021-4 : si la date ainsi calculée est encore passée (virement en retard de plus d'une période, par exemple après un report pour solde insuffisant), une seule exécution a lieu par passage, et le rattrapage se fait aux passages suivants ; le corpus ne traite pas ce cas.]

Critère d'acceptation (test) : un virement mensuel prévu le 15/01 à 08:00 et exécuté le 15/01 à 08:04 a pour prochaine exécution le 15/02 à 08:00 ; après douze exécutions mensuelles successives, l'échéance reste le 15 du mois à 08:00.

Source : RG-021-011, RG-021-012, BUG-069

#### FR-021-09 : Échec d'un virement isolé des autres

Si l'exécution d'un virement programmé échoue, le système annule entièrement les effets de cette exécution (ni débit, ni crédit, ni opération) [ASSUMPTION A-021-5 : « isoler l'échec » implique l'annulation complète de l'exécution en échec], journalise « Echec execution virement programme id={id} », et poursuit l'exécution des autres virements dus du même passage, sans que l'échec les affecte. Remplace le comportement legacy RG-021-013 (les virements suivants du même passage pouvaient être perdus).

Critère d'acceptation (test) : lors d'un passage avec trois virements dus dont le deuxième provoque une erreur, le premier et le troisième sont exécutés (soldes, opérations, compteurs et échéances mis à jour), le deuxième reste inchangé et un message d'échec est journalisé pour lui.

Source : RG-021-013, BUG-070

#### FR-021-10 : Contrôle de fraude à l'exécution de chaque échéance [ÉVOLUTION — décision de finalisation B2]

Une échéance de virement programmé passe par le même contrôle de fraude et la même règle de suspension qu'un virement immédiat (FR-027-02). Le contrôle s'exécute au moment de l'exécution de l'échéance, et non à la programmation (FR-004-07), par le même cas d'usage que le virement immédiat, après les contrôles du compte bénéficiaire (FR-021-04) et du solde (FR-004-12), comme FR-003-07. Selon son issue :
- **exécuté** : débit, crédit, opérations et suivi de FR-021-03 à FR-021-08 ;
- **suspendu** (score cumulé strictement supérieur à 75, montant supérieur ou égal au seuil de fraude, ou dépassement du cumul glissant de FR-027-18) : l'échéance devient un virement à l'état SUSPENDU, sans débit, qui suit FR-027-16 (états), FR-027-17 (heure butoir à 24 heures et rejet automatique) et FR-016-19 (libération ou rejet par l'administrateur) ; le client reçoit l'avis de suspension de FR-027-04 ;
- **refusé pour erreur du contrôle** : rien n'est débité et l'erreur est journalisée (FR-027-02).

L'état SUSPENDU d'une échéance (FR-027-16) ne doit pas être confondu avec un virement programmé inactif (FR-021-02, FR-004-09) : le virement programmé lui-même reste actif. [ASSUMPTION A-021-6 : pendant la suspension d'une échéance, la date de prochaine exécution du virement programmé avance comme après une exécution (FR-021-08), pour que l'échéance ne soit pas reprise à chaque passage ; le nombre d'exécutions et l'horodatage de la dernière exécution (FR-021-07) ne changent qu'au passage de l'échéance à EXÉCUTÉ, après sa libération ; une échéance rejetée n'est pas rattrapée] [ASSUMPTION A-021-7 : une échéance exécutée ne produit aucune notification, comme dans le legacy, et seule une échéance suspendue en produit une (FRAUDE, FR-027-04) ; une échéance refusée pour erreur du contrôle n'est pas exécutée, ne produit aucune notification et reste due : elle est retentée au passage suivant, comme pour un solde insuffisant (FR-004-12)] Cette exigence remplace l'absence de contrôle de fraude des virements programmés dans le legacy (RG-021-005 : débit à l'échéance sans contrôle).

Critère d'acceptation (test) : une échéance programmée de 15 000 € arrivée à échéance avec un solde suffisant est à l'état SUSPENDU après le passage du traitement, le solde du compte source est inchangé, le client a une notification FRAUDE portant l'heure butoir, et la date de prochaine exécution a avancé d'une période ; libérée avant l'heure butoir, elle est exécutée (débit, crédit, nombre d'exécutions augmenté de 1) ; non traitée, elle est REJETÉE à l'heure butoir sans débit ; une échéance de 100 € de score faible est exécutée sans notification.

Source : RG-021-005, RG-027-003

**Notes :**
- RG-021-004 : couverte par l'exigence fusionnée de FEAT-004 (règles voisines), FR-004-12 (solde insuffisant ; BUG-014 y ajoute la notification du client).
- Contradiction dans le corpus : le texte de RG-021-007 dit « reject the transfer and refund the source account », alors que son détail de nature et BUG-068 décrivent un débit sans crédit ni remboursement. FR-021-04 retient le comportement attendu de BUG-068 (refus avant tout débit).
- Les libellés de fréquence « Hebdomadaire », « Mensuel », « Bimestriel », « Trimestriel », « Semestriel », « Annuel » sont ceux de FR-004-08.
- Dans le legacy, ce traitement n'appelait pas le contrôle de fraude des virements (aucune règle du corpus ne le relie à RG-027-003). Depuis la décision de finalisation B2, chaque échéance y passe (FR-021-10).
