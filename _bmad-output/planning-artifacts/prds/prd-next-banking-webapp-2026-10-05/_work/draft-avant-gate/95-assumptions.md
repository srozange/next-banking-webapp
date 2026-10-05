
## 10. Index des suppositions

Chaque `[ASSUMPTION]` du document, classée de la plus risquée à la moins risquée. Le risque mesure ce qui arrive si la supposition est fausse : correction de sécurité ou de comportement métier, texte comparé par le golden master, ou simple détail. Deux suppositions globales, A-G-01 et A-G-02, n'apparaissent qu'ici : elles couvrent des choix appliqués à tout le §4.

### Risque élevé

- **A-G-01** (§4, global) : les 405 règles de statut « À confirmer » du corpus sont reprises telles quelles, comme si elles étaient confirmées. Si le métier en invalide une partie, les FR concernées changent.
- **A-G-02** (§4, global) : les règles de nature « dette legacy » qu'aucun bug à corriger ne couvre ne sont pas reproduites et ne sont remplacées par rien. Conséquence : aucune FR ne fixe de longueur maximale pour le motif, le numéro de compte et le libellé du bénéficiaire (RG-004-070 à RG-004-072), ni le comportement d'un motif de prêt de plus de 255 caractères (RG-007-016), ni les contrôles de l'année, du mois et du SIRET des documents (RG-008-016, RG-008-017, RG-017-006, RG-017-015).
- **A-018-1** (FEAT-018) : les intérêts mensuels sont crédités lors de l'exécution de 2 h du 1er jour de chaque mois, et rien les autres nuits. BUG-064 exige « une fois par mois » sans dire quel jour.
- **A-027-2** (FEAT-027) : le signal MULTIPLE_FAILED_AUTH (règle Suspect RG-027-017) est évalué à la connexion, au 5e échec consécutif. Il ne s'ajoute pas au score d'un virement. C'est une capacité que le legacy n'avait pas.
- **A-030-1** (FEAT-030) : les écrans publics sont exemptés d'authentification sur leur chemin exact, et non pour toute adresse qui contient leur nom. C'est une correction de sécurité qu'aucun bug déclaré ne couvre.
- **A-021-3** (FEAT-021) : un virement programmé refusé parce que son compte destinataire est introuvable passe à l'état suspendu. Le corpus ne dit pas ce qu'il devient.
- **A-003-7** (FEAT-003, FEAT-002, FEAT-023, FEAT-025) : le libellé PENDING, la mention « [EN ATTENTE] » et la notification ALERTE (« en attente de validation… verification en cours ») sont conservés pour un virement de 5 000 € ou plus pourtant exécuté et débité, sans aucune vérification. L'écart entre le texte et le solde, que BUG-009 visait, reste entier.
- **A-014-1** (FEAT-014) : un retour du fournisseur d'open banking qui ne porte qu'un `code` est traité comme incomplet, tant que le parcours n'est pas confirmé auprès de Powens.

### Risque moyen

- **Textes absents du corpus**, que le golden master ou les tests compareront. Chacun doit être validé avant le rejeu :
  - A-012-1 : retour Powens en erreur.
  - A-012-4 : message après un échec de chargement des comptes.
  - A-016-1 : erreur de traitement d'un prêt.
  - A-021-1 : refus d'un virement programmé, compte introuvable.
  - A-023-1 : échec de remise d'un virement.
  - A-024-1 : service de crédit indisponible.
  - A-004-3 : notification de solde insuffisant d'un virement programmé.
  - A-004-4 : « Compte destinataire introuvable ».
- **Textes du legacy non reproduits ou arbitrés.** Des écarts sont attendus au rejeu du golden master, à traiter à l'étape G2 :
  - A-001-3, A-002-4, A-003-6, A-011-2, A-018-3 : bandeaux et mentions techniques supprimés ;
  - A-007-2 : « En attente » plutôt que « En cours » ;
  - A-002-2 : format des montants, deux rendus dans le corpus.
- **A-027-4** (FEAT-027) : texte de l'avis de suspension (notification FRAUDE), proposé en table ronde et non arbitré. Il remplace les textes legacy, d'où un écart attendu au golden master.
- **A-027-10** (FEAT-027) : le client voit l'état de ses virements suspendus, libérés ou rejetés sur l'écran de virement et le tableau de bord.
- **A-027-6** (FEAT-027) : un virement libéré dont le solde est devenu insuffisant passe à REJETÉ.
- **A-027-7** (FEAT-027) : le cumul glissant compte les virements exécutés et suspendus, pas les rejetés ni les refusés.
- **A-G-09** (addendum A.2) : les scénarios du golden master des fonctionnalités qui appellent un fournisseur externe se rejouent contre les adaptateurs simulés. SC-010-02 demande un simulateur qui renvoie un refus d'authentification.
- **A-G-04** (§7.5) : les temps recommandés par `docs/retro/00-global/NFR.md` sont adoptés comme cibles au 95e centile.
- **A-001-1** (FEAT-001) : un compte désactivé est refusé avec le message générique de mauvais identifiants, et une entrée d'audit LOGIN_FAILED.
- **A-001-2** (FEAT-001, FEAT-026) : l'adresse IP transmise par un en-tête n'est retenue que si elle vient d'un relais de confiance.
- **A-008-1, A-017-1** (documents) : l'accès au document d'un autre client est refusé comme un accès interdit (HTTP 403), sans message métier.
- **A-010-1** (FEAT-010) : « erreur d'authentification » du service d'IA couvre 401 et 403.
- **A-011-1** (FEAT-011) : le message « aucun produit » s'affiche par type de produit vide, comme le dit la règle, et non seulement quand tout le catalogue est vide, comme dans le legacy.
- **A-012-5** (FEAT-012) : un bouton « Rafraichir » est exposé. Il n'existe pas à l'écran dans le legacy (question ouverte du brief).
- **A-012-6, A-014-2, A-028-1** (Powens) : aucune attente imposée avant la lecture des comptes, contrairement à la pause bloquante du legacy.
- **A-016-2** (FEAT-016) : « Client depuis » est la date de création de la fiche client, affichée pour tout client.
- **A-018-2** (FEAT-018) : une relance dans le même mois ne recrédite pas d'intérêts.
- **A-021-4** (FEAT-021) : un virement programmé en retard de plus d'une période ne s'exécute qu'une fois par passage, puis rattrape aux passages suivants.
- **A-022-2** (FEAT-022) : contenu de l'entrée du journal d'exécution, y compris pour une exécution interrompue.
- **A-023-2** (FEAT-023) : un virement refusé faute de remise au traitement est entièrement annulé (enregistrement, débit, crédit, opérations).
- **A-024-2** (FEAT-024) : une demande de prêt non transmise est annulée et n'apparaît pas dans la liste.
- **A-025-1, A-025-2** (FEAT-025) : la notification de solde faible est de type ALERTE, soumise au même plafond, et n'est émise qu'une fois tant que le solde reste sous 100 €.
- **A-004-2** (FEAT-004) : une seule notification par échéance de virement programmé non exécutée.
- **A-027-1** (FEAT-027) : le signal d'heure inhabituelle utilise le fuseau de l'application, pas celui du client.
- **A-029-1** (FEAT-029) : sans clé AlphaVantage, aucun appel réel, et la page se comporte comme une absence de données.
- **A-029-2** (FEAT-029) : un symbole ou un nom trop long est bloqué par la limite du champ, sans message.
- **A-002-3** (FEAT-002) : un client sans compte COURANT voit un tableau d'opérations vide.

### Risque faible

- **A-G-03** (§7.1) : moment d'exécution de `trace-smoke.sh`, non précisé dans `docs/quality-gates.md`.
- **A-G-05** (§2.2, addendum) : l'API REST est interne au front.
- **A-G-06** (§2.3) : les parcours utilisateurs sont illustratifs.
- **A-G-07** (§7.6) : fuseau des traitements planifiés, Europe/Paris.
- **A-G-08** (§7.8) : attribut de test stable exigé sur les éléments actionnés par le golden master.
- **A-002-1** (FEAT-002) : un utilisateur supprimé pendant sa session est déconnecté et renvoyé vers la connexion.
- **A-003-1, A-003-2** (FEAT-003) : messages non techniques pour un montant invalide et pour un solde insuffisant.
- **A-003-3** (FEAT-003) : les contrôles de saisie et de solde passent avant le contrôle de fraude.
- **A-003-4** (FEAT-003, FEAT-027) : un seul signal LARGE_AMOUNT par virement, de score 40 entre 5 000 € inclus et 15 000 € exclus, de score 80 à partir de 15 000 €. RG-003-025 n'a pas de borne haute, contrairement à RG-027-004 : l'écart est visible dans la FR.
- **A-003-5** (FEAT-003, FEAT-027) : RAPID_SUCCESSION compare au précédent virement, alors que RG-003-032 dit « transaction » et RG-027-011 dit « virement ».
- **A-004-1, A-013-1** : messages de refus génériques sur la ressource d'un autre client.
- **A-004-5** (FEAT-004) : la correction de BUG-069 s'applique aussi à RG-004-064.
- **A-005-1** (FEAT-005) : signe moins en tiret ASCII.
- **A-007-1** (FEAT-007) : une durée de 301 à 360 mois est refusée avec « Duree invalide ».
- **A-010-2** (FEAT-010) : le champ du chat n'est pas vidé après l'envoi, comme dans le legacy.
- **A-012-2, A-012-3** (FEAT-012) : « Solde indisponible » et message d'indisponibilité de Powens.
- **A-014-3** (FEAT-014) : la remise à zéro au retour de Powens efface aussi le message d'erreur.
- **A-019-1** (FEAT-019) : une exécution par jour calendaire, y compris aux changements d'heure.
- **A-021-2** (FEAT-021) : le refus « compte introuvable » passe avant le report pour solde insuffisant.
- **A-021-5** (FEAT-021) : « isoler l'échec » veut dire annuler entièrement l'exécution en échec.
- **A-022-1** (FEAT-022) : le plafond de 20 alertes de solde faible porte sur le jour calendaire, heure de Paris.
- **A-027-3** (FEAT-027) : texte de la description de l'alerte MULTIPLE_FAILED_AUTH.
- **A-003-8** (FEAT-002, FEAT-003) : un virement refusé pour erreur du contrôle ne produit aucune notification ; seul l'écran de refus l'annonce.
- **A-027-8** (FEAT-027) : quand une libération et une expiration sont concurrentes, la première action enregistrée l'emporte.
- **A-027-9** (FEAT-003, FEAT-027) : le rejet automatique est tracé au journal d'audit, et la reprise préremplie reprend le compte source, le compte destinataire, le montant et le motif.
- **A-016-3** (FEAT-016) : la file de revue est triée par heure butoir, avec les colonnes proposées en table ronde.

*Les décisions D1 à D12 sur RG-027-003 (§9) sont des décisions du décideur, pas des suppositions : elles ne figurent pas dans cet index.*
