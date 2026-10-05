### FEAT-021 — Exécution des virements programmés

**Description :** Traitement automatique sans écran, qui exécute les virements programmés créés par les clients (CLIENT) dans l'écran des virements programmés (FEAT-004). Toutes les 5 minutes, il sélectionne les virements actifs arrivés à échéance, débite le compte source, crédite le compte bénéficiaire interne, enregistre les opérations visibles au relevé (FEAT-005) et au tableau de bord, puis fait avancer la date de prochaine exécution selon la fréquence. Les cas limites marquants sont le solde insuffisant (virement retenté au passage suivant), le compte bénéficiaire absent de la banque (désormais refusé avant tout débit), la dérive du calendrier (désormais calculé depuis la date prévue) et l'échec d'un virement, qui ne doit pas affecter les autres. Scénarios golden master : aucun.

**Exigences fonctionnelles :**

#### FR-021-01 : Déclenchement périodique

Le système déclenche l'exécution des virements programmés dus toutes les 5 minutes, à toute heure, et journalise « Verification virements programmes dus » à chaque passage. Le déclenchement ne conserve pas d'état propre : après un redémarrage du serveur, il reprend au prochain cycle de 5 minutes, et les virements arrivés à échéance pendant l'arrêt sont exécutés à cette reprise (FR-021-02), soit au plus 5 minutes de retard après le démarrage.

Source : RG-021-001, RG-021-002

#### FR-021-02 : Sélection des virements dus

À chaque passage, le système sélectionne tous les virements programmés actifs dont la date de prochaine exécution est antérieure ou égale à l'instant courant, triés par date de prochaine exécution croissante, et les traite l'un après l'autre. Les virements inactifs (suspendus) ne sont pas sélectionnés.

Source : RG-021-003

#### FR-021-03 : Débit du compte source et crédit du compte bénéficiaire

Pour un virement dû dont le solde du compte source est suffisant, le système débite le compte source du montant du virement et crédite du même montant le compte de la banque dont le numéro correspond au compte bénéficiaire, sauf si le compte bénéficiaire n'est pas un compte de la banque (FR-021-04, aucun débit) ou si le solde est insuffisant (FR-004-12, aucun débit, virement retenté au passage suivant). Le système journalise « Virement programme execute id={id} montant={montant} ».

Source : RG-021-005, RG-021-006

#### FR-021-04 : Compte bénéficiaire hors de la banque refusé avant tout débit

Si le numéro de compte bénéficiaire ne correspond à aucun compte de la banque, le système rejette le virement avant tout débit : aucun débit, aucune opération enregistrée, et le client est informé par une notification dans l'application, avec un message d'erreur clair [ASSUMPTION A-021-1 : texte proposé « Virement programme vers {libellé bénéficiaire} refuse : compte destinataire introuvable », non fixé par le corpus]. Ce contrôle précède celui du solde (FR-004-12) [ASSUMPTION A-021-2 : le corpus ne donne pas l'ordre ; le refus définitif passe avant le report pour solde insuffisant]. Comme la création vers un tel compte est déjà refusée (FR-004-13), ce cas ne concerne que les virements existants dans les données de référence ou dont le compte bénéficiaire a disparu ; le virement est alors suspendu (état inactif), pour ne pas être refusé et notifié à chaque passage [ASSUMPTION A-021-3 : suspension du virement après le refus ; le corpus ne dit pas ce que devient le virement]. Remplace le comportement legacy RG-021-007 (compte source débité sans crédit nulle part).

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

**Notes :**
- RG-021-004 : couverte par l'exigence fusionnée de FEAT-004 (règles voisines), FR-004-12 (solde insuffisant ; BUG-014 y ajoute la notification du client).
- Contradiction dans le corpus : le texte de RG-021-007 dit « reject the transfer and refund the source account », alors que son détail de nature et BUG-068 décrivent un débit sans crédit ni remboursement. FR-021-04 retient le comportement attendu de BUG-068 (refus avant tout débit).
- Les libellés de fréquence « Hebdomadaire », « Mensuel », « Bimestriel », « Trimestriel », « Semestriel », « Annuel » sont ceux de FR-004-08.
- Ce traitement n'appelle pas le contrôle de fraude des virements (aucune règle du corpus ne le relie à RG-027-003).
