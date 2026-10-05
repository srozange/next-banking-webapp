# Table ronde : décision sur la règle RG-027-003

**Date :** 2026-10-05
**Mode :** BMAD party mode, chaque participant est incarné par un agent qui raisonne seul.

## La règle

> **RG-027-003 (BUG-084)** : « WHEN a transfer is submitted THE SYSTEM SHALL execute fraud detection before the transfer is finally committed; IF the fraud check raises an error THE SYSTEM SHALL log the error and the transfer SHALL NOT proceed silently ».

**Acquis :** le contrôle s'exécute avant la validation finale, et son erreur est journalisée.

**Questions à trancher :**
- **(a)** Le contrôle détecte une suspicion : faut-il bloquer le virement, le signaler seulement, ou décider selon un score de risque avec des seuils ?
- **(b)** Le contrôle tombe en erreur ou ne répond pas : faut-il refuser le virement, ou l'exécuter avec une alerte ?

## Participants

- **Mary**, analyste métier
- **John**, Product Manager
- **Sally**, UX designer
- **Winston**, architecte
- **Amelia**, développeuse senior
- **Vex**, ingénieur sécurité
- **Level**, vérificatrice des affirmations
- **Sébastien**, décideur

## Discussion

### Tour 1 : ouverture (chacun parle sans avoir entendu les autres)

**Mary (analyste métier)** : Un trésor, dès le premier coup de pioche ! Le corpus tient en deux phrases : FEAT-027 dit que la détection « ne bloque pas le virement : elle le signale », alors que FEAT-023 et FEAT-003 (BUG-076) suspendent déjà, avant tout débit, un virement au-dessus du seuil de fraude, avec une notification FRAUDE « bloqué pour vérification ». Le corpus ne dit rien de ce qui doit se passer quand le contrôle plante, car les tickets Jira 9200 à 9202 sont introuvables. Il ne dit pas non plus qui libère un virement suspendu : l'administrateur ne peut aujourd'hui que changer le statut d'une alerte, et ni le conseiller ni la banque n'ont de rôle défini dans ce circuit. Côté parties prenantes, le client subit les faux positifs et les délais, l'administrateur hérite de la file de revue, et la banque porte la perte si on laisse passer. Les preuves penchent pour (a) vers la décision par score à seuils, car les scores existent déjà (alerte au-dessus de 75, suspension à 10 000 €) : bloquer seulement au-dessus du seuil et signaler en dessous prolonge le legacy sans inventer de règle. Pour (b), elles penchent vers le refus du virement, car « ne procède pas silencieusement » et le blocage avant débit de BUG-076 vont dans ce sens. Reste à nommer qui traite la file de revue, et c'est là-dessus que je veux entendre les autres.

**John (Product Manager)** : Je rouvre l'affaire côté client : qui perd quoi ? Un virement légitime bloqué à tort, c'est un client qui appelle, qui attend, qui s'en va. Mais le legacy, lui, laisse partir l'argent et se contente d'un « Attention » après coup : zéro fraude évitée, et ça, c'est le vrai scandale du dossier.

(a) Je prends parti : décision par score, pas blocage systématique. En dessous du seuil, le virement passe sans friction ; au-dessus (strictement > 75, pas ≥), on suspend avant le débit et on le dit clairement au client, avec un délai annoncé. Bloquer sur le moindre signal, c'est punir le client avec un virement rond de 1 000 € (score 20) pour rien ; signaler seulement, c'est refaire le legacy.

(b) Erreur ou silence du contrôle : on ne décide pas à sa place, donc on ne laisse pas passer en silence, ça, la règle l'interdit déjà. Je suspends, avec trace, plutôt que d'exécuter avec alerte : une panne ne doit pas devenir une porte ouverte. Question qui fâche : combien de clients bloqués par une panne, et pendant combien de temps ?

Reste la question que personne ne pose : qui traite ce qui est suspendu ? Dans le legacy, l'ADMIN change le statut d'une alerte, mais aucune action « libérer / rejeter un virement » n'existe. Sans ce traitement, la suspension est un cul-de-sac : il faut d'abord définir ce rôle et son délai de réponse, sinon on a juste déplacé le problème.

**Sally (UX designer)** : Fermez les yeux, c'est un jeudi soir, Claire règle l'acompte de 12 000 € pour l'artisan qui refait sa toiture. Option (a) « bloquer sur suspicion » : elle clique, l'écran se fige sur « virement suspendu », sans dire pourquoi ni pour combien de temps. Elle ne comprend pas, elle croit à un piratage, elle appelle sa banque le soir même, et l'administrateur se retrouve avec une file de revues sans le « pourquoi » (le FIXME JIRA-9201 jamais soldé). Option « signaler seulement », c'est le legacy : l'argent est déjà parti, l'avertissement arrive après la bataille, et une vraie fraude passe. Le score à seuils est le seul qui respecte les deux scènes : sous 75 on ne voit rien, entre le seuil d'alerte et celui de suspension le virement part avec une ALERTE, et seulement au-dessus d'un seuil franc il est suspendu avec une phrase claire (« vérification en cours, réponse sous X heures »), sachant qu'on n'a droit qu'à une seule notification, donc elle doit dire à elle seule ce qui se passe.

Pour (b), imaginez Claire face à « service indisponible » : si on refuse tout, un incident technique transforme chaque client honnête en suspect, et ils appellent par centaines ; si on exécute avec alerte, le fraudeur n'a qu'à attendre la panne. Ma préférence : refuser par défaut avec un message honnête (« rien n'a été débité, réessayez dans quelques minutes »), mais laisser passer en alerte les petits montants sous le seuil de 5 000 €, là où une erreur coûte peu. Pour l'admin, il faut enfin une action « libérer / rejeter » qui n'existe pas dans le legacy, sans quoi suspendre est une impasse. Mon parti : score à seuils en (a), refus avec message clair en (b), et ce qu'il faut peaufiner, c'est le texte de l'unique notification, pas la technique.

**Vex (ingénieur sécurité)** : Je commence par (b), c'est là que je passe. Si on exécute en cas d'erreur, mon chemin d'attaque est trivial : je sature ou fais échouer le contrôle (rafale de requêtes, entrée malformée qui lève une exception, timeout provoqué), puis je lance le gros virement pendant la panne. Le contrôle devient optionnel pour quiconque sait le faire tomber, et c'est exactement le legacy (catch + « virement déjà effectué »). Refuser en cas d'erreur évite cela : le pire que je gagne, c'est un déni de service, pénible pour les clients mais sans perte d'argent. Je prends donc parti : (b) = refus, avec un message neutre au client et une alerte à l'exploitation.

Pour (a), « signaler seulement » ne protège de rien : je vois l'avertissement, je valide quand même, et l'argent est parti avant toute revue. Blocage systématique ou score à seuils ? Le score est contournable par construction : alertes enregistrées seulement au-dessus de 75, donc je découpe en virements de 4 900 € (ni LARGE_AMOUNT ni ROUND_AMOUNT, sous le seuil de 5 000 € du statut PENDING), espacés de plus de 30 s pour éviter RAPID_SUCCESSION (55), et je reste à 8 opérations par jour pour éviter HIGH_FREQUENCY (50). Je peux donc faire sortir un montant important sans jamais dépasser 75. Le blocage sur toute suspicion évite ce jeu de seuils mais bloque trop de virements légitimes ; je préfère le score avec blocage au-dessus du seuil, à condition d'ajouter un cumul glissant par bénéficiaire et par jour, sinon le découpage passe. Le nocturne à 02h30 ne crée aucune alerte, donc il ne rattrape rien. Obligations réglementaires : point à confirmer, je n'en affirme aucune.

**Winston (architecte)** : Je prends parti : la décision vit dans le service de virement, de façon synchrone, avant la validation finale. C'est ce que dit RG-027-003 (le contrôle s'exécute avant l'engagement), et le legacy a justement l'erreur inverse : contrôle après coup, dans la couche web, erreur avalée. Un événement Kafka asynchrone arriverait après le débit, donc il ne pourrait que constater, jamais empêcher. Il n'y a pas de transaction partagée entre bases, et rattraper un débit déjà fait demande une compensation, ce que je ne veux pas pour une démo. Le coût du synchrone, c'est le couplage et la disponibilité : si le contrôle est un service distinct, son indisponibilité devient celle du virement. Je préfère donc garder le calcul de score (les signaux et seuils du corpus, de la pure logique de domaine) dans le service de virement, derrière un port, avec un adaptateur simulé. Pas d'appel réseau bloquant, pas de dépendance de disponibilité, et c'est testable à 100 % sans réseau.

Sur (a), la suspicion : je vote pour un score à seuils, avec trois issues. Sous le seuil, on exécute. Au-dessus du seuil de fraude, le virement est suspendu (statut PENDING, avant tout débit, c'est déjà BUG-076 et BUG-009), et c'est un état du virement, pas une exception. Kafka entre en jeu ici, après la décision : le service de virement publie « virement suspendu » en transactional outbox (écriture dans sa base, publication ensuite). Le service de notification (une seule notification, BUG-007) et l'espace de revue admin le consomment, et la trace OpenTelemetry traverse tout ça. La décision reste synchrone, la conséquence est asynchrone, et le virement suspendu est un agrégat dans sa propre base. La libération ou le rejet par l'admin est une commande synchrone sur ce même service, qui n'existe pas encore dans le legacy.

Sur (b), l'erreur ou le silence : je refuse le virement (fail-closed), avec un statut explicite et journalisé, jamais un silence. Exécuter avec une alerte reproduirait le défaut du legacy sous un autre habillage. Le compromis est assumé : sur un contrôle interne, donc quasi sans panne réseau, des refus à tort sont rares et le coût client est acceptable. Si un jour le contrôle devient un fournisseur externe, il faudra un délai maximal et une file de reprise. Je note que ça se rattache à un point à confirmer côté réglementaire, pas à un fait.

**Amelia (développeuse senior)** : Je commence par les tests, toujours.

(a) Trois options, trois jeux de cas. « Bloquer » : un cas rouge par signal (LARGE_AMOUNT 5 000 / 15 000, ROUND_AMOUNT, RAPID_SUCCESSION 29 s vs 30 s, HIGH_FREQUENCY 8 vs 9), et un test qui prouve qu'aucun débit n'a lieu. « Signaler seulement » : le virement passe et l'alerte est créée ; c'est le test le plus mince. « Score à seuils » : c'est de la table de vérité, et les bornes sont le piège. Il me faut 75 → pas d'alerte, 76 → alerte (le « strictement supérieur à 75 » du corpus), 9 999,99 / 10 000 pour BUG-076, 4 999,99 / 5 000 pour BUG-009, plus un cas de cumul (40 + 20 + 55 = 115). Le tout en tests de domaine purs, avec PIT à 85 pour attraper les « > » devenus « >= ».

(b) Le contrôle est un port, l'adaptateur simulé a trois modes : OK, exception, silence. Pour « ne répond pas », une horloge injectée, jamais de sleep. Cas : réponse à t < délai, à t = délai, à t > délai ; erreur levée ; réponse tardive qui arrive après la décision. Je vérifie que l'erreur est journalisée (RG-027-003) et qu'aucun débit n'a eu lieu en « refuser », ou qu'une alerte est émise en « exécuter ». Zéro réseau.

Trace : un span « fraud.check » dans la trace du virement, avec les attributs score, signaux, décision (BLOCK, FLAG, ALLOW), et pour (b) un statut ERROR avec l'exception, plus un attribut timeout=true. Si c'est asynchrone, le span Kafka porte le contexte propagé ; trace-smoke.sh doit le voir.

Écart avec le golden master : SC-003-01/02/04 rejouent un legacy qui contrôle APRÈS le virement, avale l'erreur et n'affiche qu'un avertissement. Toute option « bloquer » ou « refuser » diverge, et c'est voulu : ces scénarios doivent être « map » avec exception documentée, ou « drop » justifié. Seule l'option « signaler seulement + exécuter malgré l'erreur » ressemble au legacy. Elle reste pourtant un écart : le contrôle passe avant et l'erreur est journalisée.

Le plus dur à prouver : (b) « ne répond pas ». Le temps, la concurrence, la réponse tardive qui arrive après une décision déjà prise, l'idempotence. Le score à seuils vient juste après, par le volume de cas aux bornes. Bloquer ou signaler seulement se prouve en deux assertions.

### Tour 2 : vérification et confrontation

**Level (vérificatrice des affirmations)** : Je vérifie trois points contre le corpus. (1) Mary a raison sur la lettre : FEAT-027/RG.md, RG-027-003 [Suspect], champ « Résolution » : « ne bloque pas le virement : elle le signale ». Mais la règle elle-même dit « reject or flag », et LEGACY-BUGS (BUG-084) demande de « résoudre cette ambiguïté » : la Résolution est donc une réponse enregistrée, non un arbitrage gravé, et elle contredit RG-023-009 (suspension dès 10 000 €). Confiance élevée sur la citation, moyenne sur sa portée. (2) Winston confond deux seuils. BUG-009/RG-003-014 : PENDING dès 5 000 €, et ce statut ne suspend rien, le compte est débité aussitôt. BUG-076/RG-023-009 : suspension « avant tout débit » à ≥ 10 000 €, et aucun nom de statut n'y figure. « Suspendu = PENDING » n'est pas prouvé, et le rapprocher de BUG-009 est trompeur ; confiance haute que c'est une confusion. (3) Vex a omis deux signaux : NEW_BENEFICIARY (35) et VELOCITY_CHECK (65, ≥ 5 opérations > 500 € en 60 min), RG-003 et RG-027. Ses 4 900 € évitent bien LARGE_AMOUNT, ROUND_AMOUNT, FOREIGN_IBAN et RAPID_SUCCESSION, mais un nouveau bénéficiaire (35) plus un 5e virement dans l'heure (65) donnent 100, soit plus de 75 ; l'attaque tient seulement avec un bénéficiaire connu, au plus 4 virements par heure et dans les heures 06h–23h. L'ordre de grandeur de sa critique est plausible, ses chiffres ne sont pas établis. Aucune affirmation réglementaire non étayée ce tour-ci : les « points à confirmer » sont correctement marqués. Reste non vérifié : le rôle du conseiller, que Mary avance sans source.

**Vex (ingénieur sécurité)** : Sally, ton « petits montants sous 5 000 € passent en alerte quand le contrôle est en panne » est un guichet ouvert : je fais tomber le contrôle, ou j'attends une vraie panne, puis je sors 4 900 € en rafale, bénéficiaire par bénéficiaire, sans plafond ni cumul puisque rien n'est calculé pendant la panne. Le seuil de 5 000 € devient la consigne de découpage affichée, et le fraudeur n'a plus à jouer avec RAPID_SUCCESSION ou HIGH_FREQUENCY, qui sont aveugles à ce moment-là. Si l'on garde un passage dégradé, il lui faut un plafond cumulé par jour et par client, hors contrôle, et je préfère encore le refus pur ou la suspension de John, qui bloque l'argent sans le perdre ni le laisser partir. Winston, le score en mémoire retire la panne réseau, pas le scénario : je peux toujours provoquer une erreur par des entrées hostiles (montant, IBAN ou horodatage mal formés qui font lever une exception dans le calcul), par des données d'historique absentes ou corrompues (compteur du jour, date du précédent virement), par une base d'historique lente ou verrouillée, ou par une configuration de seuils invalide au déploiement. Je veux donc que le calcul reste fail-closed, exception comprise, et que ces cas soient testés, car « quasi impossible » n'est pas « impossible ». Bonus : un score local doit lire l'historique de la même base, donc le cumul glissant que je réclame s'y ajoute sans appel réseau.

**Sally (UX designer)** : Vex a raison, je lâche ma tolérance : un seuil qui part en alerte pendant la panne devient une porte écrite noir sur blanc, et je ne veux pas d'un écran qui apprend au fraudeur où se tenir. Pendant une panne, tout est suspendu, sans exception de montant ; la honte d'un client légitime retardé est réparable, l'argent parti ne l'est pas. Et attention, le client ne doit pas lire la différence entre « panne » et « suspicion » : un seul texte, honnête et neutre, qui n'accuse personne et ne révèle aucun signal. Ma proposition pour l'unique notification SUSPENDU : « Votre virement de 12 000,00 € vers [bénéficiaire] est en cours de vérification. Rien n'a été débité. Vous aurez notre réponse avant [date et heure précises], et vous n'avez rien à faire. » Je veux une heure butoir, pas un « sous 24 h » flou, qui est la porte d'entrée de l'appel angoissé, et si l'heure est dépassée, le client voit le statut changer sur l'écran de ses virements, sans nouvelle notification puisqu'on n'en a droit qu'à une. Côté administrateur, une file triée par échéance la plus proche, avec pour chaque ligne le montant, le bénéficiaire, le client, le motif en clair (« nouveau bénéficiaire + 5e virement dans l'heure » ou « contrôle indisponible », ce que réclamait le FIXME JIRA-9201), le score et les signaux, et deux boutons seulement, « Libérer » et « Rejeter », avec un commentaire obligatoire et la trace de qui a décidé. Un virement suspendu pour cause de panne doit se distinguer visuellement d'une vraie suspicion, car on ne les traite pas dans le même état d'esprit : le premier se relance, le second s'examine.

**Winston (architecte)** : Level a raison, je reconnais la confusion. J'ai collé « suspendu » sur le PENDING de BUG-009, qui ne suspend rien : le compte est débité aussitôt, c'est seulement un statut d'affichage dès 5 000 €. BUG-076 suspend avant tout débit à partir de 10 000 € sans nommer de statut, donc ce nom est à créer, il n'existe pas dans le corpus. Je ne dessinerais plus un seul état fourre-tout. Il en faut cinq : EXÉCUTÉ (dont le PENDING de BUG-009, qui reste débité), SUSPENDU (aucun débit, en attente de revue), LIBÉRÉ (qui mène ensuite à EXÉCUTÉ), REJETÉ (aucun débit), et REFUSÉ_ERREUR_CONTRÔLE (aucun débit, journalisé). Les trois derniers sont terminaux ou transitoires sans jamais toucher au compte. Je garde l'état d'erreur distinct de la suspicion : le client et l'exploitation n'ont pas la même chose à faire, et les mélanger masquerait les pannes.

Vex, tu as raison sur le fond : un score local supprime la panne réseau, pas l'exception. Je maintiens le fail-closed, mais « au niveau du calcul » : toute exception, entrée hostile, historique corrompu ou absent, base verrouillée ou configuration de seuils invalide donne REFUSÉ_ERREUR_CONTRÔLE, avec un test par cause. La configuration invalide doit en plus faire échouer le démarrage, pas le premier virement. Je refuse en revanche le passage dégradé de Sally sous 5 000 € : il offre la consigne de découpage que tu décris.

Le cumul glissant par bénéficiaire et par jour est une nouvelle règle : le corpus a RAPID_SUCCESSION, HIGH_FREQUENCY, VELOCITY_CHECK et NEW_BENEFICIARY, mais aucun cumul de montant par bénéficiaire. Techniquement il est peu coûteux, car l'historique est dans la même base, sans appel réseau. Mais il sort du corpus et du golden master, donc je le propose comme ajout explicite, à trancher par le décideur, pas comme règle déjà acquise.

Pour libérer un virement suspendu : ni le legacy ni le corpus n'ont de commande, et le rôle du conseiller n'est pas sourcé. Je propose deux commandes synchrones sur le service de virement, LibérerVirement et RejeterVirement, réservées au rôle ADMIN, avec motif obligatoire et identité de l'agent tracée. La libération doit refaire le contrôle de solde avant le débit, puisque le compte a pu bouger entre-temps. Ce choix du rôle reste une décision à confirmer par le décideur, car on peut vouloir séparer la personne qui libère de celle qui a changé le statut de l'alerte.

**John (Product Manager)** : Je garde la suspension, mais je la resserre : le refus et la suspension bloquent tous deux l'argent, la différence est ce qui reste au client. Avec un refus, Claire voit « rien n'a été débité, réessayez » et doit tout ressaisir, alors qu'un fraudeur, lui, réessaie en boucle jusqu'à ce que la panne passe. Avec une suspension, la demande est conservée, rien n'est débité, et personne ne ressaisit rien. Mais je concède à Winston et Vex ceci : sur une erreur de calcul interne, le refus est le défaut. On suspend seulement les montants à partir de 5 000 € (pas de file noyée par des paniers de 30 €), et en dessous on refuse avec un message clair. Qui reprend après la panne ? Le système lui-même, une fois le contrôle revenu : il rejoue le calcul sur la demande suspendue, et ne passe en revue manuelle que ce qui dépasse le seuil. Pour la file « suspect », mon idée pour un démonstrateur : un seul rôle, l'ADMIN, qui libère ou rejette depuis l'espace de revue (l'action qui manque au legacy), avec un délai annoncé au client de 24 h ouvrées. À l'expiration, on REJETTE automatiquement, avec notification unique, rien n'est jamais débité par défaut, et une demande oubliée ne devient jamais un virement. Point à confirmer : tout délai imposé par la réglementation.

### Tour 3 : propositions et préconisation

**Mary (analyste métier)** : Voilà le trésor complet, étalé sur la table : trois cartes, une préconisation, et les zones encore en friche.

**1. Trois propositions**

**P1 : Prudente (tout ce qui est douteux est gelé, rien ne part sans contrôle)**
- Décision : (a) score à seuils, suspension dès 76 de score (strictement > 75) ou dès 10 000 € (BUG-076), plus le cumul glissant de Vex ; (b) toute erreur ou silence suspend la demande, sans exception de montant (position de Sally).
- Client : peu de fraude évitée par fuite, mais plus de faux positifs et de demandes en attente. La demande est conservée, rien n'est ressaisi, un seul texte neutre avec heure butoir.
- Architecture : décision synchrone dans le service de virement, états SUSPENDU / LIBÉRÉ / REJETÉ / EXÉCUTÉ. Pas de REFUSÉ_ERREUR_CONTRÔLE : la panne se confond avec la suspicion, ce que Winston veut éviter.
- Exploitation : file ADMIN la plus lourde, car chaque panne l'alimente. Il faut un rejeu automatique après retour du contrôle.
- Tests : table de vérité aux bornes (75/76, 9 999,99/10 000), cause d'erreur testée une à une.
- Hors corpus : cumul glissant, états, commandes Libérer/Rejeter, délai, rejet automatique.

**P2 : Équilibrée (refus en panne, suspension sur suspicion, ma préférée)**
- Décision : (a) score à seuils : sous le seuil on exécute, au-dessus on suspend avant débit ; (b) refus fail-closed, état distinct REFUSÉ_ERREUR_CONTRÔLE, message « rien n'a été débité ».
- Client : pas de faux positifs sous le seuil, une panne coûte une ressaisie. Fraude évitée : la suspicion est stoppée avant débit, la panne n'ouvre aucune porte.
- Architecture : calcul local dans le domaine derrière un port, synchrone. Kafka seulement après la décision (outbox, notification unique, espace de revue). Exception de calcul = refus, configuration invalide = échec au démarrage.
- Exploitation : seule la vraie suspicion alimente la file ADMIN, avec échéance et rejet automatique ; les pannes vont à l'exploitation, pas à la file.
- Tests : adaptateur simulé à trois modes, horloge injectée, span fraud.check (score, signaux, décision, timeout).
- Hors corpus : états, commandes Libérer/Rejeter, délai ; cumul glissant en option séparée.

**P3 : Minimale (legacy corrigé)**
- Décision : (a) contrôle avant le virement, suspension seulement au seuil déjà connu de BUG-076 (10 000 €), en dessous on signale ; (b) refus en cas d'erreur.
- Client : très peu de friction. Fraude évitée : faible, car le découpage de Vex passe (la tenue de son exemple à 4 900 € reste à nuancer : Level rappelle NEW_BENEFICIARY et VELOCITY_CHECK).
- Architecture : même socle synchrone, moins d'états (EXÉCUTÉ, SUSPENDU, REFUSÉ).
- Exploitation : file minuscule.
- Tests : le moins de cas, golden master le plus proche. Mais SC-003 diverge quand même (contrôle avant, erreur non avalée).
- Hors corpus : presque rien, sauf la commande de libération.

**2. Ma préconisation : P2**
C'est celle qui s'appuie le plus sur la preuve : le score à seuils réutilise les scores existants (Winston, John, Sally), le refus en panne est défendu par Vex (le contrôle ne doit pas devenir optionnel pour qui sait le faire tomber) et par Winston (distinguer panne et suspicion), et Sally a lâché sa tolérance sous 5 000 €. J'écarte P1 car elle noie l'ADMIN à chaque incident, comme John le redoutait (ressaisie contre file noyée), et P3 car elle laisse la porte du découpage ouverte. J'y ajoute, à ta décision, le cumul glissant de Vex, qui sort du corpus mais coûte peu (même base, aucun appel réseau, dixit Winston).

**3. Désaccords restants**
- En cas d'erreur : refus (Winston, Vex) contre suspension de tout montant (Sally) contre suspension à partir de 5 000 € et refus en dessous (John). Winston refuse l'effet « consigne de découpage » de ce seuil ; John répond que la suspension conserve la demande. À trancher.
- Cumul glissant par bénéficiaire et par jour : Vex le veut, Winston l'accepte comme ajout explicite, personne ne l'a encore tranché, et il s'écarte du golden master.
- Qui libère, et quand : John veut l'ADMIN, 24 h ouvrées, rejet automatique à l'expiration ; Sally veut une heure butoir précise affichée au client. Winston ajoute que le rôle ADMIN reste à confirmer (séparer qui libère de qui change le statut de l'alerte). Rien n'a été dit sur le rejeu automatique après panne au-delà de John.

**4. Points à confirmer**
- Toute obligation réglementaire de blocage, de délai de revue, de conservation des traces et de motivation du refus au client : point à confirmer, aucune n'est établie.
- Le rôle exact qui libère (ADMIN seul, ou conseiller ?). Le corpus ne le source pas : point à confirmer.
- Le délai (24 h ouvrées ou autre) et le rejet automatique à l'expiration : point à confirmer.
- Le texte des tickets Jira 9200 à 9202, introuvable : point à confirmer s'il existe un arbitrage antérieur.
- Le seuil de suspension entre BUG-076 (10 000 €) et un score > 75, qui ne se recouvrent pas dans le corpus : point à confirmer.

### Tour 3 (suite) : réactions à la préconisation

**Winston (architecte)** : P2 me convient, et elle fixe trois choses pour l'architecture : le score est un calcul de domaine pur derrière un port, la décision est synchrone dans le service de virement, et l'agrégat Virement porte une machine d'états explicite (EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE), avec les événements publiés par outbox après la décision. Avant bmad-architecture, il faut trancher quatre points. Premièrement, la règle de suspension (voir plus bas). Deuxièmement, le cumul glissant, parce qu'il impose un modèle d'historique des virements par bénéficiaire et par jour dans la base du service. Troisièmement, qui libère, et si le rejet automatique à l'expiration est un traitement planifié dans le service (il l'est, donc il faut le dire). Quatrièmement, la décision de rejeu du golden master pour SC-003, car la machine d'états en dépend.

Sur le seuil de suspension, je défends les deux conditions cumulées en OU : suspension si score > 75 OU montant ≥ 10 000 €. Le corpus ne les fait pas se recouvrir : le seuil de 10 000 € de BUG-076 est un seuil de montant absolu, indépendant du score, et le score > 75 est le seuil d'enregistrement des alertes. Garder le montant seul laisserait un virement de 9 999 € avec un score de 115 partir sans friction, ce qui annule le sens du score. Je tiens à ce que ce soit une configuration de déploiement validée au démarrage (les deux seuils numériques, jamais en dur), et à ce que le OU soit confirmé par le décideur, car c'est une interprétation de ma part, pas un fait du corpus.

**Amelia (développeuse senior)** : Je signe P2, avec trois réserves chiffrées. Coût en tests : une quarantaine de cas, soit environ 12 pour la table de vérité (75/76, 9 999,99/10 000, 4 999,99/5 000, un cas par signal, plus NEW_BENEFICIARY et VELOCITY_CHECK que Vex avait oubliés), 8 pour les causes d'erreur de calcul (un test par cause : entrée hostile, historique absent ou corrompu, base verrouillée, configuration invalide qui doit faire échouer le démarrage), 6 pour la machine à états de Winston (transitions valides et interdites, avec LIBÉRÉ → débit qui refait le contrôle de solde), plus les commandes Libérer/Rejeter (ADMIN seul, motif obligatoire) et le rejet automatique à l'échéance, plus 3 assertions de span sur fraud.check. Le plus fragile est l'échéance : le rejet automatique repose sur l'horloge injectée, et il y a une course entre « Libérer » et « expiration » à la même seconde. Il faut un test d'exclusion, le premier arrivé gagne et l'autre est refusé, sans double débit.

Ce qui manque pour que P2 soit testable : un délai chiffré. Sans « 24 h ouvrées », sans calendrier des jours ouvrés, et sans le timeout du contrôle en millisecondes, je n'ai pas de borne à mettre en rouge, et « ne répond pas » reste invérifiable. Il me faut aussi la liste des états tranchée (cinq états, transitions explicites, REFUSÉ_ERREUR_CONTRÔLE terminal) et la décision sur le seuil de suspension : 10 000 € (BUG-076) ou score > 75, ou les deux, car sinon j'écris deux tables contradictoires. Je laisse le cumul glissant en option séparée, avec ses propres tests, pour ne pas bloquer P2.

Golden master : SC-003-01/02/04 divergent, et c'est voulu. Le contrôle passe avant le virement et l'erreur n'est plus avalée. Je les passe en « map » avec exception documentée, ou en « drop » justifié, mais pas « verts par accident ». Les cas de suspension et de refus n'ont aucun scénario legacy : ils sont validés uniquement par mes tests, comme pour BUG-076.

**Vex (ingénieur sécurité)** : Sans le cumul glissant, P2 laisse ma faille de découpage grande ouverte : des virements de 4 900 €, chacun sous 5 000 €, sans signal et avec un score à 0, passent tous, et j'ajoute un bénéficiaire à chaque fois pour éviter ce que Level appelle VELOCITY_CHECK ou NEW_BENEFICIARY. Level a raison de nuancer, mais ces signaux sont des scores unitaires : aucun ne regarde ce que le compte a perdu en tout sur la journée. Je signe donc P2 à une condition, que le cumul glissant par client et par jour (montant total, tous bénéficiaires confondus, pas seulement par bénéficiaire) soit dans le périmètre, et non en option. J'exige aussi le fail-closed testé cause par cause, y compris l'exception du calcul, l'échec au démarrage sur configuration invalide, et le rejet automatique à l'expiration de la revue. Sans cumul, je ne signe pas : vous auriez fait un contrôle que je contourne avec un tableur.

**John (Product Manager)** : Je signe P2, avec un seul amendement et un seul cas où je maintiens mon désaccord. Le cas précis : un virement légitime de 5 000 € et plus (l'acompte de Claire, 12 000 €) pendant une panne. P2 le refuse et Claire ressaisit tout, alors que la suspension aurait conservé sa demande. Je ne bloque pas la préconisation pour autant, car Winston a raison sur l'essentiel : mélanger panne et suspicion masque les pannes et noie la file ADMIN. Mon amendement, qui tient en une ligne : en cas de refus pour erreur, le message dit « rien n'a été débité, vous pouvez refaire ce virement », avec un bouton de reprise qui préremplit la saisie, de sorte que la ressaisie ne coûte presque rien. Si le décideur veut quand même trancher mon désaccord, je lui demande de le noter comme option à part (suspension dès 5 000 € en cas d'erreur) et non comme un blocage de P2. Je rappelle les points à confirmer : le rôle qui libère, le délai de 24 h ouvrées et le rejet automatique à l'expiration.

**Sally (UX designer)** : Je signe P2, avec une réserve que je dis à voix haute : je lâche la suspension générale parce que Winston et Vex m'ont convaincue qu'une panne ne doit ni noyer l'ADMIN ni se confondre avec une suspicion, et que la ressaisie est un prix que Claire peut payer un jeudi soir. Pour que ce soit acceptable, l'écran doit dire trois choses dans cet ordre : « Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque », puis ce qu'elle doit faire : un bouton « Réessayer » qui garde le bénéficiaire, le montant et le motif déjà remplis, afin qu'elle ne retape rien, et enfin une porte de sortie honnête (« si le problème persiste dans quelques minutes, contactez-nous, référence XXXX »). Surtout, aucun mot sur la fraude ni sur un signal : le client ne doit ni se croire suspect ni apprendre au fraudeur que la panne est une brèche, et cette référence permet à l'exploitation de retrouver la trace du span en cas d'appel. Ma condition : que la ressaisie conserve les champs, sinon le refus redevient cruel.

**Level (vérificatrice des affirmations)** : Les attributions sont globalement fidèles (Vex pour le refus en panne, Winston pour l'état distinct, Sally qui lâche sa tolérance sous 5 000 €), mais trois affirmations de la synthèse ne sont pas étayées. D'abord, P2 écarte P3 parce que « le découpage de Vex passe » ; or, sans le cumul glissant, que Mary laisse en option séparée, P2 exécute aussi sous le seuil et laisse la même porte ouverte, et Level avait déjà dit que les 4 900 € de Vex ne sont pas établis. Ensuite, « pas de faux positifs sous le seuil » et « fraude évitée » ne reposent sur aucune mesure, et le seuil de P2 n'est même pas défini, puisque Mary note elle-même que le score > 75 et les 10 000 € de BUG-076 ne se recouvrent pas dans le corpus. Enfin, la concession de John (refus par défaut sur erreur de calcul interne) et sa remarque que le fraudeur réessaie en boucle après un refus ne sont pas traitées dans P2, et le rejet automatique et le délai de 24 h ouvrées ne viennent que de lui, sans accord de la salle. Ma confiance dans P2 : moyenne ; la direction (calcul local synchrone, refus en panne, états distincts) est étayée par Vex et Winston, mais l'efficacité contre la fraude ne l'est pas. Elle monterait si le décideur fixait le seuil de suspension et décidait du cumul glissant, et elle baisserait si le cumul était refusé, car P2 vaudrait alors P3 contre le découpage.

### Tour 4 : questions au décideur

**Vex (ingénieur sécurité)** : Décideur, si tu dis non au cumul, je sors 4 900 € par virement tant que le compte est approvisionné, sans qu'aucun signal ne bouge, et ton contrôle ne verra jamais le total perdu dans la journée. Question fermée : le cumul glissant par client et par jour (tous bénéficiaires confondus) est-il dans le périmètre, oui ou non, et si oui, avec quel plafond journalier au-delà duquel on suspend, par exemple 10 000 € comme le seuil de BUG-076, ou un autre montant que tu fixes ?

**Winston (architecte)** : Une question pour vous sur le seuil de suspension, car c'est lui qui dessine la machine d'états. Prenons un virement de 9 000 € vers un nouveau bénéficiaire, étranger, à 23 h 30 : le score dépasse 75 alors que le montant reste sous 10 000 €, donc « montant seul » le laisse partir, « score seul » le suspend, et un virement de 12 000 € à un bénéficiaire connu en pleine journée, avec un score bas, est suspendu par « montant seul » mais pas par « score seul ». Je penche pour les deux en OU, mais c'est vous qui tranchez : voulez-vous que la suspension se déclenche sur le score seul, sur le montant seul, ou sur l'un ou l'autre ?

**John (Product Manager)** : À vous, décideur, une seule affaire reste ouverte chez moi : la file. Qui libère ou rejette un virement suspendu : l'ADMIN seul, ou un conseiller que le corpus ne source pas ? Dans quel délai : 24 h ouvrées, ou une heure butoir précise affichée au client comme le veut Sally ? Et à l'expiration : rejet automatique (rien n'est jamais débité par défaut) ou autre chose ? J'ai aussi mon option à part, suspendre dès 5 000 € en cas d'erreur du contrôle plutôt que refuser, qui conserve la demande du client : à vous de la garder ou de l'écarter. Alors, qui vide la file, et en combien de temps ?

**Amelia (développeuse senior)** : À toi de trancher, deux chiffres et mes tests rouges sont écrits. Un : le délai maximal du contrôle. Mon défaut est 500 ms, car le calcul est local et lit une seule base. Au-delà, refus, REFUSÉ_ERREUR_CONTRÔLE, span fraud.check avec timeout=true. Mes bornes seront 499, 500 et 501 ms sur l'horloge injectée. Deux : l'échéance de revue. Mon défaut est 24 heures calendaires, sans calendrier de jours ouvrés. Un calendrier de fériés est une dépendance de plus à tester, et pour un démonstrateur une durée fixe suffit. Si tu veux des jours ouvrés, dis-moi lesquels (lundi–vendredi, avec ou sans jours fériés français). Donc : « 500 ms et 24 h calendaires », ça te va, ou tu changes l'un des deux ?

**Sébastien** : Mes décisions, point par point.

Vex, cumul glissant : oui, il est dans le périmètre du projet, mais pas dans le socle. Il fait l'objet de la story qui suit immédiatement le socle RG-027-003. Règle : total des virements émis par un client sur 24 heures glissantes, tous bénéficiaires confondus ; au-delà de 10 000 €, le virement qui fait franchir le plafond est suspendu. Le plafond est une configuration de déploiement, alignée sur BUG-076. D'ici là, le découpage est un risque connu, écrit comme tel dans le PRD.

Winston, seuil de suspension : l'un ou l'autre. Suspension si le score est strictement supérieur à 75 OU si le montant est supérieur ou égal à 10 000 €. C'est une décision prise ici, pas un fait du corpus. Les deux seuils sont en configuration, validée au démarrage.

John, file de revue : l'administrateur seul libère ou rejette, avec motif obligatoire et identité tracée. Pas de rôle pour le conseiller dans ce périmètre. Délai : 24 heures calendaires à partir de la soumission, affiché au client sous forme d'heure butoir précise. À l'expiration : rejet automatique, sans débit. J'écarte ton option : en cas d'erreur du contrôle, c'est un refus pur, quel que soit le montant.

Amelia, les deux chiffres : 24 heures calendaires, oui, sans calendrier de jours fériés. Pour le délai maximal du contrôle : pas de délai dans ce périmètre. Winston a posé que le score est calculé localement dans le service de virement, sans appel réseau ; le cas « ne répond pas » n'existe donc pas ici. Seule l'exception du calcul est à traiter, et elle donne un refus. Si le contrôle devient un jour un service externe, on reprendra tes 500 ms.

Option retenue : P2, avec ces précisions. Hors périmètre du socle : le bouton de reprise prérempli, noté comme amélioration.

### Tour 4 (suite) : réactions aux décisions

**Winston (architecte)** : Merci, vous figez pour bmad-architecture : une décision synchrone et locale dans le service de virement, une machine à cinq états (EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE), deux commandes réservées à l'ADMIN (Libérer et Rejeter) avec motif et identité tracés, deux seuils validés au démarrage (score > 75, montant ≥ 10 000 €, en OU), et un rejet automatique à 24 h calendaires, qui est donc un traitement planifié dans ce service, à écrire comme tel et testé avec une horloge injectée. Une réserve sur « pas de délai du contrôle » : sans réseau, je vous l'accorde, mais la base peut encore se verrouiller ou ralentir, et un virement ne doit pas rester bloqué sur un verrou. Je demande donc un délai d'expiration sur l'accès à l'historique (côté transaction ou requête, pas dans le domaine), qui retombe en REFUSÉ_ERREUR_CONTRÔLE, ce que je tiens pour un détail d'implémentation, pas pour une nouvelle règle métier. Reste le cumul glissant de Vex, dont le modèle d'historique dépend de votre réponse.

**Vex (ingénieur sécurité)** : Je signe le séquençage, à condition que le risque de découpage soit écrit noir sur blanc au PRD avec la story du cumul nommée juste derrière le socle, car d'ici là mon contournement à 4 900 € reste valable. Mais il reste un trou : sans délai maximal, une base lente ou verrouillée n'est pas une exception, c'est un blocage qui tient le virement en attente indéfiniment (et mon attaquant peut le provoquer exprès pour saturer les connexions). Je demande donc au minimum un délai d'expiration sur la lecture de l'historique (timeout de requête ou de verrou), qui lève une exception et donne donc le refus déjà prévu. Ce n'est pas un contrôle réseau, c'est la même exception du calcul, mais sans lui « pas de délai » veut dire « pas de refus ».

**Amelia (développeuse senior)** : Bien reçu : 24 h calendaires, et pas de délai de contrôle. Disparaissent l'horloge injectée côté contrôle, les cas 499/500/501 ms, le mode « silence » de l'adaptateur, la réponse tardive et le attribut timeout=true du span. Restent les causes d'exception de calcul, une par test, chacune donnant REFUSÉ_ERREUR_CONTRÔLE avec erreur journalisée, aucun débit et statut ERROR sur fraud.check, plus l'horloge pour l'échéance de 24 h (23 h 59 min 59 s, 24 h pile, 24 h + 1 s, et la course Libérer contre expiration). Un seul cas supplémentaire que je veux couvrir : un calcul qui ne rend jamais la main, par exemple une base verrouillée sans délai de lecture. « Pas de délai » est testable pour le calcul pur, mais pas pour cette lecture d'historique. Dis-moi si on pose un délai d'expiration technique côté base (même s'il n'est pas métier), ou si on assume qu'un blocage de base est une panne d'exploitation, hors tests.

**Level (vérificatrice des affirmations)** : Vos décisions sont cohérentes entre elles, et le seuil en OU, le cumul différé et la file ADMIN à 24 h calendaires lèvent l'essentiel de mes réserves ; mais « le cas ne répond pas n'existe pas » est un trou, car Vex a cité des causes qui produisent le même effet qu'un silence (base d'historique lente ou verrouillée, entrée hostile qui ralentit le calcul). Sans délai maximal, une requête bloquée tient le virement indéfiniment ; il faut au moins un délai de sécurité sur la lecture de l'historique, qui donne REFUSÉ_ERREUR_CONTRÔLE, ou accepter ce risque par écrit dans le PRD. Ensuite, Vex ne signait qu'avec le cumul dans le périmètre, et vous l'avez repoussé après le socle : le découpage reste donc un risque connu, que vous avez bien consigné, et P2 vaut P3 contre lui jusqu'à cette story. Ma confiance dans P2 : moyenne à bonne pour le socle, moyenne pour la protection contre la fraude, et elle monte à bonne si vous tranchez le délai de lecture de l'historique et la date de la story du cumul.

**Sally (UX designer)** : J'accepte, décideur, mais je maintiens une réserve que je veux voir écrite dans le PRD : sans bouton prérempli, le refus coûte à Claire une ressaisie complète, et c'est une cruauté connue, pas un oubli. Au minimum, l'écran de refus du socle doit dire : « Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence XXXX. » Pas un mot sur la fraude ni sur un signal. Et pour que l'amélioration ne reste pas un vœu, qu'elle soit une story nommée avec un propriétaire, et que les champs saisis restent affichés au retour sur le formulaire si le navigateur le permet sans rien construire de plus.

**John (Product Manager)** : Dossier classé, je prends acte : refus pur quel que soit le montant, ADMIN seul, 24 h calendaires, rejet automatique, et mon bouton de reprise reste une amélioration hors socle. Ce que je surveillerai : le message de refus, qui doit dire clairement « rien n'a été débité » pour que Claire ne croie pas à un piratage, et la file ADMIN, parce qu'un rejet automatique à 24 h calendaires tombe aussi la nuit et le week-end, et le client doit le lire dans sa notification unique. Si les clients ressaisissent trop souvent après une panne, je rouvrirai l'affaire.

### Tour 4 (fin) : arbitrages complémentaires

**Sébastien** : Mes décisions sur les trois points.

1. Délai sur la lecture de l'historique : oui. Vous avez raison, une base verrouillée ou lente peut bloquer sans lever d'exception. Je retiens un délai technique au niveau de l'accès à la base, pas une règle métier. Son expiration lève une erreur, qui retombe sur le refus déjà prévu (état REFUSÉ_ERREUR_CONTRÔLE, rien n'est débité, erreur journalisée). La valeur est laissée à l'architecture, en configuration de déploiement validée au démarrage ; je veux un chiffre dans la décision d'architecture, pas dans le PRD. Côté tests, c'est une cause d'erreur de plus dans la liste d'Amelia, simulée par l'adaptateur : pas d'horloge ni de cas de réponse tardive.

2. Texte de l'écran de refus proposé par Sally : retenu tel quel, comme minimum du socle. La référence affichée est l'identifiant de trace du virement, pour que l'exploitation retrouve le span fraud.check.

3. Reprise préremplie : une story nommée, après le socle et après celle du cumul glissant. Elle n'est pas dans le socle.

Remarque de John sur le rejet automatique la nuit et le week-end : accepté en l'état pour ce périmètre. C'est la conséquence voulue des 24 heures calendaires ; à noter dans le PRD comme point à revoir si le projet sort du démonstrateur.

Pour Vex : le risque de découpage et la story du cumul juste après le socle seront écrits noir sur blanc dans le PRD.

Vous pouvez mettre à jour la synthèse « Décisions prises ».

## Synthèse pour la décision

*Mise à jour après les arbitrages du décideur (tour 4). Aucune position n'est ajoutée à ce qui a été dit dans la discussion.*

### Décisions prises

Décideur : « Moi », tour 4. Option retenue : **P2, la proposition équilibrée, avec les précisions ci-dessous.**

| # | Sujet | Décision | Origine |
|---|---|---|---|
| D1 | (a) suspicion | Score à seuils. Sous les seuils, le virement est exécuté. Au-dessus, il est **suspendu avant tout débit**, en attente de revue. | P2 (Mary), tour 3 |
| D2 | Seuil de suspension | Suspension si le **score est strictement supérieur à 75 OU si le montant est supérieur ou égal à 10 000 €**. C'est une décision prise ici, pas un fait du corpus. Les deux seuils sont en configuration de déploiement, validée au démarrage. | Proposition de Winston, retenue par Moi |
| D3 | (b) erreur du contrôle | **Refus pur, quel que soit le montant.** État REFUSÉ_ERREUR_CONTRÔLE, rien n'est débité, erreur journalisée. L'option de John (suspendre à partir de 5 000 €) est écartée. | Vex, Winston ; Moi |
| D4 | « Ne répond pas » | Pas de délai métier du contrôle : le score est calculé localement, sans appel réseau. Une **expiration technique sur l'accès à la base** (lecture de l'historique) lève une erreur, qui retombe sur D3. La valeur est fixée en architecture, pas dans le PRD, et elle est en configuration validée au démarrage. | Winston, Vex, Amelia, Level ; Moi |
| D5 | Architecture | Décision synchrone et locale dans le service de virement, avec le score derrière un port. Machine à cinq états : EXÉCUTÉ, SUSPENDU, LIBÉRÉ, REJETÉ, REFUSÉ_ERREUR_CONTRÔLE. Les événements sont publiés sur Kafka (outbox) après la décision. | Winston |
| D6 | Revue manuelle | **L'administrateur seul** libère ou rejette, avec un motif obligatoire et son identité tracée. Pas de rôle pour le conseiller dans ce périmètre. La libération refait le contrôle de solde. | John, Winston ; Moi |
| D7 | Délai de revue | **24 heures calendaires** à partir de la soumission, sans calendrier de jours fériés. Le client voit une **heure butoir précise**. | Amelia, Sally ; Moi |
| D8 | Expiration | **Rejet automatique, sans débit**, par un traitement planifié du service de virement. Il peut tomber la nuit et le week-end : c'est accepté pour le démonstrateur, et à revoir si le projet en sort. | John, Winston ; Moi |
| D9 | Écran de refus (minimum du socle) | « Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence XXXX. » Pas un mot sur la fraude. **La référence est l'identifiant de trace du virement.** | Sally ; Moi |
| D10 | Cumul glissant | Dans le périmètre du projet, **pas dans le socle** : c'est la story qui suit immédiatement le socle RG-027-003. Règle : total des virements émis par un client sur 24 heures glissantes, tous bénéficiaires confondus. Au-delà de 10 000 €, le virement qui fait franchir le plafond est suspendu. Plafond en configuration, aligné sur BUG-076. | Vex ; Moi |
| D11 | Risque connu jusqu'à D10 | Le découpage d'un montant en virements sous les seuils reste possible tant que la story du cumul n'est pas livrée. **Il est écrit comme tel dans le PRD.** | Vex, Level ; Moi |
| D12 | Reprise préremplie après un refus | **Story nommée, après le socle et après celle du cumul.** Hors socle. | John, Sally ; Moi |

**Ordre des stories qui en découle :**
1. le socle RG-027-003 (D1 à D9) ;
2. le cumul glissant (D10) ;
3. la reprise préremplie (D12).

### Ce que la décision change dans le PRD

- QO-1 (a) et (b) sont tranchées par D1 à D4. Les 13 exigences marquées `[EN ATTENTE RG-027-003]` sont à réécrire selon D1 à D9.
- Exigences nouvelles, hors corpus :
  - la machine d'états (D5) ;
  - les commandes Libérer et Rejeter (D6) ;
  - l'échéance et le rejet automatique (D7, D8) ;
  - l'écran de refus (D9) ;
  - le cumul glissant (D10).
- À écrire explicitement :
  - le risque de découpage (D11) ;
  - le point à revoir hors démonstrateur, sur le rejet de nuit et de week-end (D8).
- À garder hors du PRD : la valeur de l'expiration technique (D4), qui revient à la décision d'architecture.

### Signatures

| Participant | Position finale |
|---|---|
| Mary | Auteure de P2 |
| John | Prend acte, y compris l'option écartée. Surveillera le message de refus et les rejets de nuit et de week-end. |
| Sally | Accepte. Sa réserve est satisfaite par D9 et D12. |
| Winston | Signe. Sa réserve sur le délai est satisfaite par D4. |
| Amelia | Signe. Elle a ses chiffres : 24 h calendaires, une cause d'erreur supplémentaire pour l'expiration de la base. |
| Vex | Signe le découpage en deux temps. Sa condition est satisfaite par D10 et D11. |
| Level | Confiance moyenne à bonne sur le socle, moyenne contre la fraude jusqu'à la livraison de D10. Sa réserve sur « ne répond pas » est satisfaite par D4. |

### Tableau des critères

| Critère | P1 prudente | **P2 équilibrée (retenue)** | P3 minimale |
|---|---|---|---|
| **(a) suspicion** | Score à seuils, suspension, plus le cumul glissant | **Suspension si le score dépasse 75 ou si le montant atteint 10 000 € (D2), avant tout débit** | Suspension seulement à partir de 10 000 € (BUG-076), simple signalement en dessous |
| **(b) erreur du contrôle** | Suspension, quel que soit le montant | **Refus pur, état distinct. L'expiration de la base vaut une erreur (D3, D4)** | Refus |
| **Client : virements légitimes gênés** | Le plus | Moyen : une panne oblige à ressaisir, la reprise préremplie viendra plus tard (D12) | Le moins |
| **Client : délai** | Attente de la revue pour toute panne | Immédiat sous les seuils. Au-dessus, heure butoir affichée, 24 h au plus (D7) | Immédiat sauf à partir de 10 000 € |
| **Fraude réellement évitée** | La plus élevée (non mesurée) | Suspicion stoppée avant débit. Le découpage reste possible jusqu'à D10 (risque écrit, D11) | Faible : le découpage passe |
| **Architecture** | Synchrone, Kafka après la décision, pas d'état d'erreur distinct | Synchrone et local, 5 états, outbox, traitement planifié pour l'expiration (D5, D8) | Même socle, moins d'états |
| **Exploitation** | File de l'administrateur alimentée par les pannes | File de l'administrateur limitée aux suspicions, rejet automatique à 24 h. Les pannes vont à l'exploitation, avec la référence de trace (D9) | File minuscule |
| **Tests** | Bornes, causes d'erreur, rejeu | Bornes du score et du montant, une cause d'erreur par test (dont l'expiration de la base, simulée), machine d'états, échéance de 24 h avec la course entre Libérer et l'expiration, spans | Le moins de cas |
| **Trace** | `fraud.check` avant le commit | `fraud.check` avec score, signaux et décision, statut ERROR en cas d'erreur, identifiant de trace affiché au client | `fraud.check` |
| **Écart au golden master** | SC-003-01/02/04 divergents | SC-003-01/02/04 divergents : à reprendre avec une exception documentée, ou à écarter avec justification (décision G2) | Divergents aussi |
| **Hors corpus** | Cumul, états, commandes, délai, rejet automatique | États, commandes, délai, rejet automatique, écran de refus. Cumul et reprise dans des stories suivantes | Commande de libération |

### Ce qui reste ouvert

- **Valeur de l'expiration technique de la base** : à fixer dans la décision d'architecture (D4).
- **Décision de rejeu de SC-003-01/02/04** : étape G2, pendant `bmad-architecture`.
- **Points à confirmer (non établis) :**
  - obligations réglementaires (blocage, délai de revue, conservation des traces, motivation du refus au client) ;
  - un éventuel arbitrage antérieur dans les tickets Jira 9200 à 9202, dont le texte est introuvable ;
  - l'efficacité contre la fraude et le taux de faux positifs, non mesurés (7 clients de démonstration).
