
## 10. Index des suppositions

Chaque `[ASSUMPTION]` du document, classée de la plus risquée à la moins risquée. Le risque mesure ce qui arrive si la supposition est fausse : correction de sécurité ou de comportement métier, texte comparé par le golden master, ou simple détail. Deux suppositions globales, A-G-01 et A-G-02, n'apparaissent qu'ici : elles couvrent des choix appliqués à tout le §4.

### Risque élevé

- **A-G-01** (§4, global) : les 405 règles de statut « À confirmer » du corpus sont reprises telles quelles, comme si elles étaient confirmées. Si le métier en invalide une partie, les FR concernées changent. — propriétaire : métier ; reprise : pendant le raffinement de chaque story.
- **A-G-02** (§4, global) : les règles de nature « dette legacy » qu'aucun bug à corriger ne couvre ne sont pas reproduites et ne sont remplacées par rien. Conséquence : aucune FR ne fixe de longueur maximale pour le motif, le numéro de compte et le libellé du bénéficiaire (RG-004-070 à RG-004-072), ni le comportement d'un motif de prêt de plus de 255 caractères (RG-007-016), ni les contrôles de l'année, du mois et du SIRET des documents (RG-008-016, RG-008-017, RG-017-006, RG-017-015). — propriétaire : métier ; reprise : pendant le raffinement de chaque story.
- **A-018-1** (FEAT-018) : les intérêts mensuels sont crédités lors de l'exécution de 2 h du 1er jour de chaque mois, et rien les autres nuits. BUG-064 exige « une fois par mois » sans dire quel jour. — propriétaire : métier ; reprise : avant la story FEAT-018.
- **A-027-2** (FEAT-027) : le signal MULTIPLE_FAILED_AUTH (règle Suspect RG-027-017) est évalué à la connexion, au 5e échec consécutif. Il ne s'ajoute pas au score d'un virement. C'est une capacité que le legacy n'avait pas. — propriétaire : métier ; reprise : avant la story de FR-027-11.
- **A-030-1** (FEAT-030) : les écrans publics sont exemptés d'authentification sur leur chemin exact, et non pour toute adresse qui contient leur nom. C'est une correction de sécurité qu'aucun bug déclaré ne couvre. — propriétaire : architecte ; reprise : à l'architecture (filtrage des requêtes), avant la story FEAT-030.
- **A-021-3** (FEAT-021) : un virement programmé refusé parce que son compte destinataire est introuvable passe à l'état inactif (le virement programmé est désactivé). Le corpus ne dit pas ce qu'il devient. — propriétaire : métier ; reprise : avant la story FEAT-021.
- **A-014-1** (FEAT-014, FEAT-012, FEAT-028) : un retour du fournisseur d'open banking qui ne porte qu'un `code` est traité comme incomplet, tant que le parcours n'est pas confirmé auprès de Powens. Écart à RG-014-004, RG-012-011 et RG-028-009 (chargement avec le code, non repris), et élargissement de la condition de RG-012-008. — propriétaire : métier, après confirmation auprès de Powens ; reprise : avant la story FEAT-014, ou au premier branchement réel de Powens.
- **A-021-6** (FEAT-021) : pendant la suspension d'une échéance de virement programmé, la date de prochaine exécution avance comme après une exécution ; le nombre d'exécutions ne change qu'à l'exécution après libération ; une échéance rejetée n'est pas rattrapée. — propriétaire : métier ; reprise : avant la story de FR-021-10.
- **A-021-7** (FEAT-021, FEAT-002) : une échéance programmée exécutée ne produit aucune notification (comme le legacy), seule une échéance suspendue en produit une (FRAUDE) ; une échéance refusée pour erreur du contrôle reste due et est retentée au passage suivant, sans notification. — propriétaire : métier ; reprise : avant la story de FR-021-10.
- **A-016-6** (FEAT-016, FEAT-027) : à l'heure butoir ou après, un rejet demandé par l'administrateur est aussi sans objet (rejet par expiration) ; l'administrateur voit « Heure butoir depassee : le virement est rejete. ». — propriétaire : métier ; reprise : avant la story de FR-016-19.

### Risque moyen

- **Textes absents du corpus**, que le golden master ou les tests compareront. Chacun doit être validé avant le rejeu :
  - A-012-1 : retour Powens en erreur. — propriétaire : métier ; reprise : avant la story FEAT-012, au plus tard à l'étape G2.
  - A-012-4 : message après un échec de chargement des comptes. — propriétaire : métier ; reprise : avant la story FEAT-012, au plus tard à l'étape G2.
  - A-016-1 : erreur de traitement d'un prêt. — propriétaire : métier ; reprise : avant la story FEAT-016, au plus tard à l'étape G2.
  - A-021-1 : refus d'un virement programmé, compte introuvable. — propriétaire : métier ; reprise : avant la story FEAT-021, au plus tard à l'étape G2.
  - A-023-1 : échec de remise d'un virement. — propriétaire : métier ; reprise : avant la story FEAT-023, au plus tard à l'étape G2.
  - A-024-1 : service de crédit indisponible. — propriétaire : métier ; reprise : avant la story FEAT-024, au plus tard à l'étape G2.
  - A-004-3 : notification de solde insuffisant d'un virement programmé. — propriétaire : métier ; reprise : avant la story FEAT-004, au plus tard à l'étape G2.
  - A-004-4 : « Compte destinataire introuvable ». — propriétaire : métier ; reprise : avant la story FEAT-004, au plus tard à l'étape G2.
  - A-006-1 : erreur d'enregistrement ou de suppression d'un plafond de budget. — propriétaire : métier ; reprise : avant la story FEAT-006, au plus tard à l'étape G2.
- **Textes du legacy non reproduits ou arbitrés.** Des écarts sont attendus au rejeu du golden master, à traiter à l'étape G2 :
  - A-001-3, A-002-4, A-003-6, A-011-2, A-018-3 : bandeaux et mentions techniques supprimés ; — propriétaire : métier ; reprise : à l'étape G2.
  - A-007-2 : « En attente » plutôt que « En cours » ; — propriétaire : métier ; reprise : à l'étape G2.
  - A-002-2 : format des montants, deux rendus dans le corpus. — propriétaire : métier ; reprise : à l'étape G2.
- **A-003-10** (FEAT-003, FEAT-002, FEAT-023, FEAT-025) : formulation exacte des textes « transaction importante » de la décision B1 (notification ALERTE « Virement de {montant} EUR vers le compte {numCompte} execute - transaction importante. », mention « [TRANSACTION IMPORTANTE] » au relevé) ; le statut enregistré garde la valeur technique PENDING, non affichée comme « en attente ». Écart attendu au golden master. — propriétaire : métier ; reprise : avant la story de FR-003-10, au plus tard à l'étape G2.
- **A-005-2** (FEAT-005) : le relevé reconnaît la nouvelle mention « [TRANSACTION IMPORTANTE] » (badge « Transaction importante ») et garde l'affichage legacy de l'ancienne mention « [EN ATTENTE] » pour les opérations déjà en base. — propriétaire : métier ; reprise : avant la story FEAT-005.
- **A-016-4** (FEAT-016, FEAT-027) : l'auteur de la revue d'une alerte est enregistré par son identifiant (RG-027-015, RG-027-016) et affiché par son nom complet (RG-016-030, RG-016-031). — propriétaire : métier ; reprise : avant la story de FR-016-15.
- **A-016-5** (FEAT-016) : à la libération ou au rejet d'un virement suspendu, seules ses alertes encore OPEN passent à REVIEWED ; les alertes déjà classées gardent leur statut. — propriétaire : métier ; reprise : avant la story de FR-016-19.
- **A-026-1** (FEAT-026) : noms et détails des codes d'audit des virements (VIREMENT avec l'issue, TRANSFER_EXECUTED, ADMIN_TRANSFER_RELEASE, ADMIN_TRANSFER_REJECT, TRANSFER_AUTO_REJECT) ; les échéances programmées sont auditées au nom du titulaire, sans adresse IP. — propriétaire : architecte ; reprise : à l'architecture (codes d'audit), avant la story FEAT-026.
- **A-010-4** (FEAT-010) : la clé Azure OpenAI n'a aucune valeur par défaut dans le code (NFR-SEC-06), écart à RG-010-013 ; sans clé, aucun appel réel. — propriétaire : architecte ; reprise : à l'architecture (configuration des secrets).
- **A-010-3** (FEAT-010) : les actions rapides du chat sont masquées pour la conversation en cours et réaffichées par « Nouvelle conversation », écart à RG-010-007 (Confirmé), qui dit « session ». — propriétaire : métier ; reprise : avant la story FEAT-010.
- **A-027-4** (FEAT-027) : texte de l'avis de suspension (notification FRAUDE), proposé en table ronde et non arbitré. Il remplace les textes legacy, d'où un écart attendu au golden master. — propriétaire : métier ; reprise : avant la story du socle RG-027-003, au plus tard à l'étape G2.
- **A-027-10** (FEAT-027) : le client voit l'état de ses virements suspendus, libérés ou rejetés sur l'écran de virement et le tableau de bord. — propriétaire : métier ; reprise : avant la story du socle RG-027-003.
- **A-027-6** (FEAT-027) : un virement libéré dont le solde est devenu insuffisant passe à REJETÉ. — propriétaire : métier ; reprise : avant la story du socle RG-027-003.
- **A-027-7** (FEAT-027) : le cumul glissant compte les virements exécutés et suspendus, pas les rejetés ni les refusés. — propriétaire : métier ; reprise : avant la story de FR-027-18.
- **A-025-3** (FEAT-025, FEAT-027) : le plafond de notifications s'applique aussi à l'avis de suspension d'un virement, qui peut être supprimé ; l'état et l'heure butoir restent visibles à l'écran. — propriétaire : métier ; reprise : avant la story du socle RG-027-003.
- **A-027-11** (FEAT-003, FEAT-027) : HIGH_FREQUENCY compte les opérations au relevé des comptes du client, débits et crédits confondus. — propriétaire : métier ; reprise : avant la story du contrôle de fraude (signaux).
- **A-G-09** (addendum A.2) : les scénarios du golden master des fonctionnalités qui appellent un fournisseur externe se rejouent contre les adaptateurs simulés. SC-010-02 demande un simulateur qui renvoie un refus d'authentification. — propriétaire : architecte ; reprise : à l'étape G2 (dérivation du golden master).
- **A-G-04** (§7.5) : les temps recommandés par `docs/retro/00-global/NFR.md` sont adoptés comme cibles au 95e centile. — propriétaire : architecte ; reprise : à l'architecture.
- **A-001-1** (FEAT-001) : un compte désactivé est refusé avec le message générique de mauvais identifiants, et une entrée d'audit LOGIN_FAILED. — propriétaire : métier ; reprise : avant la story FEAT-001.
- **A-001-2** (FEAT-001, FEAT-026) : l'adresse IP transmise par un en-tête n'est retenue que si elle vient d'un relais de confiance. — propriétaire : architecte ; reprise : à l'architecture (mandataires de confiance).
- **A-008-1, A-017-1** (documents) : l'accès au document d'un autre client est refusé comme un accès interdit (HTTP 403), sans message métier. — propriétaire : métier ; reprise : avant les stories FEAT-008 et FEAT-017.
- **A-008-2** (FEAT-008) : le compteur de téléchargements est implémenté, alors que le legacy ne l'appliquait pas (QO-2). — propriétaire : métier ; reprise : avant la story FEAT-008.
- **A-017-2** (FEAT-017) : un compte inexistant donne 404, comme la règle, et non l'erreur 500 du legacy observé (bug conservé) ; écart attendu au golden master. — propriétaire : métier ; reprise : avant la story FEAT-017, au plus tard à l'étape G2.
- **A-010-1** (FEAT-010) : « erreur d'authentification » du service d'IA couvre 401 et 403. — propriétaire : métier ; reprise : avant la story FEAT-010.
- **A-011-1** (FEAT-011) : le message « aucun produit » s'affiche par type de produit vide, comme le dit la règle, et non seulement quand tout le catalogue est vide, comme dans le legacy. — propriétaire : métier ; reprise : avant la story FEAT-011.
- **A-012-5** (FEAT-012) : un bouton « Rafraichir » est exposé. Il n'existe pas à l'écran dans le legacy (question ouverte du brief). — propriétaire : métier ; reprise : avant la story FEAT-012.
- **A-012-6, A-014-2, A-028-1** (Powens) : aucune attente imposée avant la lecture des comptes, contrairement à la pause bloquante du legacy. — propriétaire : architecte ; reprise : à l'architecture de l'adaptateur Powens.
- **A-016-2** (FEAT-016) : « Client depuis » est la date de création de la fiche client, affichée pour tout client. — propriétaire : métier ; reprise : avant la story FEAT-016.
- **A-018-2** (FEAT-018) : une relance dans le même mois ne recrédite pas d'intérêts. — propriétaire : métier ; reprise : avant la story FEAT-018.
- **A-021-4** (FEAT-021) : un virement programmé en retard de plus d'une période ne s'exécute qu'une fois par passage, puis rattrape aux passages suivants. — propriétaire : métier ; reprise : avant la story FEAT-021.
- **A-022-2** (FEAT-022) : contenu de l'entrée du journal d'exécution, y compris pour une exécution interrompue. — propriétaire : équipe de dev ; reprise : pendant la story FEAT-022.
- **A-023-3** (FEAT-023, FEAT-003) : la remise au traitement asynchrone est enregistrée dans la même écriture que la validation du virement ; son échec fait échouer la validation (virement non enregistré, rien de débité, client informé), ou la libération (virement resté SUSPENDU), sans état supplémentaire dans FR-027-16. — propriétaire : architecte ; reprise : à l'architecture (remise par outbox).
- **A-024-2** (FEAT-024) : une demande de prêt non transmise est annulée et n'apparaît pas dans la liste. — propriétaire : métier ; reprise : avant la story FEAT-024.
- **A-025-1** (FEAT-025, FEAT-022) : la notification de solde faible garde le type ACCOUNT_BALANCE et la priorité HIGH du corpus ; le centre de notifications l'affiche avec l'icône ⚠️ et son type tel quel ; elle compte dans le plafond par utilisateur. — propriétaire : métier ; reprise : avant la story de FR-025-08.
- **A-004-2** (FEAT-004) : une seule notification par échéance de virement programmé non exécutée. — propriétaire : métier ; reprise : avant la story FEAT-004.
- **A-027-1** (FEAT-027) : le signal d'heure inhabituelle utilise le fuseau de l'application, pas celui du client. — propriétaire : métier ; reprise : avant la story du contrôle de fraude (signaux).
- **A-029-1** (FEAT-029) : sans clé AlphaVantage, aucun appel réel, et la page se comporte comme une absence de données. — propriétaire : équipe de dev ; reprise : pendant la story FEAT-029.
- **A-029-2** (FEAT-029) : un symbole ou un nom trop long est bloqué par la limite du champ, sans message. — propriétaire : métier ; reprise : avant la story FEAT-029.
- **A-002-3** (FEAT-002) : un client sans compte COURANT voit un tableau d'opérations vide. — propriétaire : métier ; reprise : avant la story FEAT-002.

### Risque faible

- **A-G-03** (§7.1) : moment d'exécution de `trace-smoke.sh`, non précisé dans `docs/quality-gates.md`. — propriétaire : architecte ; reprise : à l'architecture.
- **A-G-05** (§2.2, addendum) : l'API REST est interne au front. — propriétaire : architecte ; reprise : à l'architecture.
- **A-G-06** (§2.3) : les parcours utilisateurs sont illustratifs. — propriétaire : PM ; reprise : aucune, sauf si le métier raconte ses propres parcours.
- **A-G-07** (§7.6) : fuseau des traitements planifiés, Europe/Paris. — propriétaire : architecte ; reprise : à l'architecture.
- **A-G-08** (§7.8) : attribut de test stable exigé sur les éléments actionnés par le golden master. — propriétaire : architecte ; reprise : à l'architecture, au plus tard à l'étape G2.
- **A-002-1** (FEAT-002) : un utilisateur supprimé pendant sa session est déconnecté et renvoyé vers la connexion. — propriétaire : équipe de dev ; reprise : pendant la story FEAT-002.
- **A-003-1, A-003-2** (FEAT-003) : messages non techniques pour un montant invalide et pour un solde insuffisant. — propriétaire : métier ; reprise : avant la story FEAT-003.
- **A-003-3** (FEAT-003) : les contrôles de saisie et de solde passent avant le contrôle de fraude. — propriétaire : métier ; reprise : avant la story du socle RG-027-003.
- **A-003-4** (FEAT-003, FEAT-027) : un seul signal LARGE_AMOUNT par virement, de score 40 entre 5 000 € inclus et 15 000 € exclus, de score 80 à partir de 15 000 €. RG-003-025 n'a pas de borne haute, contrairement à RG-027-004 : l'écart est visible dans la FR. — propriétaire : métier ; reprise : avant la story du contrôle de fraude (signaux).
- **A-003-5** (FEAT-003, FEAT-027) : RAPID_SUCCESSION compare au précédent virement, alors que RG-003-032 dit « transaction » et RG-027-011 dit « virement ». — propriétaire : métier ; reprise : avant la story du contrôle de fraude (signaux).
- **A-003-9** (FEAT-003, FEAT-027) : VELOCITY_CHECK compte les virements exécutés du client (RG-027-012), et non toutes ses opérations (RG-003-033). — propriétaire : métier ; reprise : avant la story du contrôle de fraude (signaux).
- **A-004-1, A-013-1** : messages de refus génériques sur la ressource d'un autre client. — propriétaire : métier ; reprise : avant les stories FEAT-004 et FEAT-013.
- **A-004-5** (FEAT-004) : la correction de BUG-069 s'applique aussi à RG-004-064. — propriétaire : métier ; reprise : avant la story FEAT-004.
- **A-005-1** (FEAT-005) : signe moins en tiret ASCII. — propriétaire : équipe de dev ; reprise : pendant la story FEAT-005.
- **A-007-1** (FEAT-007) : une durée de 301 à 360 mois est refusée avec « Duree invalide ». — propriétaire : métier ; reprise : avant la story FEAT-007.
- **A-010-2** (FEAT-010) : le champ du chat n'est pas vidé après l'envoi, comme dans le legacy. — propriétaire : métier ; reprise : avant la story FEAT-010.
- **A-012-2, A-012-3** (FEAT-012) : « Solde indisponible » et message d'indisponibilité de Powens. — propriétaire : métier ; reprise : avant la story FEAT-012, au plus tard à l'étape G2.
- **A-014-3** (FEAT-014) : la remise à zéro au retour de Powens efface aussi le message d'erreur. — propriétaire : équipe de dev ; reprise : pendant la story FEAT-014.
- **A-019-1** (FEAT-019) : une exécution par jour calendaire, y compris aux changements d'heure. — propriétaire : architecte ; reprise : à l'architecture (planification des traitements).
- **A-021-2** (FEAT-021) : le refus « compte introuvable » passe avant le report pour solde insuffisant. — propriétaire : métier ; reprise : avant la story FEAT-021.
- **A-021-5** (FEAT-021) : « isoler l'échec » veut dire annuler entièrement l'exécution en échec. — propriétaire : équipe de dev ; reprise : pendant la story FEAT-021.
- **A-022-1** (FEAT-022) : le plafond de 20 alertes de solde faible porte sur le jour calendaire, heure de Paris. — propriétaire : métier ; reprise : avant la story FEAT-022.
- **A-027-3** (FEAT-027) : texte de la description de l'alerte MULTIPLE_FAILED_AUTH. — propriétaire : équipe de dev ; reprise : pendant la story de FR-027-11.
- **A-003-8** (FEAT-002, FEAT-003) : un virement refusé pour erreur du contrôle ne produit aucune notification ; seul l'écran de refus l'annonce. — propriétaire : métier ; reprise : avant la story du socle RG-027-003.
- **A-027-9** (FEAT-027) : le rejet automatique est tracé au journal d'audit. — propriétaire : architecte ; reprise : avant la story de FR-027-17.
- **A-003-11** (FEAT-003) : la reprise préremplie reprend le compte source, le compte destinataire, le montant et le motif. — propriétaire : métier ; reprise : avant la story de FR-003-28.
- **A-018-4** (FEAT-018) : les nuits sans intérêts dus, le traitement enregistre une entrée SUCCESS avec 0 enregistrement. — propriétaire : équipe de dev ; reprise : pendant la story FEAT-018.
- **A-027-12** (FEAT-027, FEAT-001) : les échecs de connexion d'un compte désactivé comptent pour MULTIPLE_FAILED_AUTH. — propriétaire : métier ; reprise : avant la story de FR-027-11.
- **A-016-3** (FEAT-016) : la file de revue est triée par heure butoir, avec les colonnes proposées en table ronde. — propriétaire : métier (UX) ; reprise : avant la story de FR-016-19.

*Les décisions D1 à D12 sur RG-027-003 et les décisions de finalisation B1 à B5 (§9) sont des décisions du décideur, pas des suppositions : elles ne figurent pas dans cet index. A-003-7 (textes PENDING conservés), A-025-2 (une seule alerte de solde faible), A-027-8 (première action enregistrée) et A-023-2 (annulation après échec de remise) ont été retirées au gate : les trois premières sont remplacées par les décisions B1, B3 et B5, la dernière par A-023-3. Au tri de finalisation, l'ancienne A-027-9 a été scindée en A-027-9 (trace d'audit du rejet automatique) et A-003-11 (champs de la reprise préremplie).*
