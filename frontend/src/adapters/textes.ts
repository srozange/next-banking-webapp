/**
 * Textes affichés, jamais inventés : repris du legacy (docs/retro/features/FEAT-001/MESSAGES.md,
 * docs/retro/00-global/MESSAGES.md §1.1-1.4 pour la navigation, §FEAT-007 pour les prêts) ou de textes-nouveaux.md (clé entre parenthèses).
 */
export const TEXTES = {
  /** a11y.lien.evitement */
  lienEvitement: 'Aller au contenu principal',
  /** api.erreur.technique */
  erreurTechnique: 'Une erreur technique est survenue',
  connexion: {
    titreOnglet: 'BankApp Demo - Connexion',
    logo: 'DB',
    titre: 'Demo Bank',
    sousTitre: 'Espace client — Connexion sécurisée',
    identifiant: 'Identifiant',
    motDePasse: 'Mot de passe',
    meConnecter: 'Me connecter',
    identifiantRequis: 'Identifiant requis',
    motDePasseRequis: 'Mot de passe requis',
    identifiantsIncorrects: 'Identifiants incorrects',
    verifiezLogin: 'Vérifiez votre login et mot de passe',
  },
  /** Écran Virement (docs/retro/features/FEAT-003/FIELDS.md et MESSAGES.md ; textes-nouveaux.md pour les clés citées). */
  virement: {
    titreOnglet: 'BankApp Demo - Virement',
    titre: 'Effectuer un virement',
    compteSource: 'Compte source',
    compteDestinataire: 'Compte destinataire',
    choisirUnCompte: '-- Choisir un compte --',
    montant: 'Montant (€)',
    indiceMontant: 'Ex : 100,00 — Saisir plus de 5 000 € pour tester l\'alerte fraude',
    motif: 'Motif',
    executer: 'Exécuter le virement',
    lienBeneficiaires: 'Virement SEPA externe — Gérer mes bénéficiaires',
    /** virement.validation.comptes (message serveur du legacy, repris tel quel) */
    validationComptes: 'Veuillez sélectionner les deux comptes',
    /** virement.validation.montant */
    validationMontant: 'Veuillez saisir le montant',
    comptesIdentiques: 'Le compte source et destination doivent être différents',
    /** virement.avis.suspension, en deux parties autour de l'heure butoir (`virement.revue.butoir.valeur`). */
    avisSuspension: {
      avant: (montant: string, numeroCompte: string) =>
        `Votre virement de ${montant} EUR vers le compte ${numeroCompte} est en cours de vérification. Rien n'a été débité. Vous aurez notre réponse avant le `,
      apres: '. Sans décision à cette heure, il sera rejeté sans aucun débit.',
    },
    /** virement.refus.erreur.controle, en deux parties autour de la référence. */
    refusErreurControle: {
      avant:
        "Votre virement n'a pas pu être vérifié. Rien n'a été débité. Ce n'est pas un refus de votre banque. Vous pouvez refaire votre demande dans quelques minutes ; si cela persiste, contactez-nous avec la référence ",
      apres: '.',
    },
    /** « Suivi de mes virements » (virement.revue.*, virement.etat.*). */
    suivi: {
      titre: 'Suivi de mes virements',
      aide: "Vous n'avez rien à faire : l'état de chaque virement se met à jour sur cette page. Un virement rejeté n'a pas été débité ; vous pouvez refaire votre demande.",
      vide: 'Aucun virement à suivre',
      colonnes: {
        date: 'Date',
        compte: 'Compte destinataire',
        montant: 'Montant',
        etat: 'Etat',
        butoir: 'Réponse avant le',
      },
      /** virement.revue.butoir.valeur */
      butoir: (date: string, heure: string) => `${date} à ${heure}`,
      etats: {
        EXECUTE: 'Exécuté - débité',
        SUSPENDU: 'En cours de vérification - non débité',
        LIBERE: 'Accepté - pas encore débité',
        REJETE: 'Rejeté - non débité',
      },
    },
  },
  /** Section de revue des virements suspendus, écran Administration (revue.*, textes-nouveaux.md). */
  revue: {
    titre: 'Virements suspendus — En attente de revue',
    vide: 'Aucun virement suspendu',
    colonnes: {
      client: 'Client',
      montant: 'Montant',
      compte: 'Compte destinataire',
      motif: 'Motif de suspension',
      butoir: 'Heure butoir',
      actions: 'Actions',
    },
    liberer: 'Libérer',
    rejeter: 'Rejeter',
    modale: {
      titreLiberer: (montant: string, numeroCompte: string) => `Libérer le virement de ${montant} EUR vers le compte ${numeroCompte}`,
      titreRejeter: (montant: string, numeroCompte: string) => `Rejeter le virement de ${montant} EUR vers le compte ${numeroCompte}`,
      champMotif: 'Motif de la décision',
      valider: 'Valider',
      annuler: 'Annuler',
    },
    /** revue.erreur.motif.manquant (MSG-007-127) */
    erreurMotifManquant: 'Veuillez indiquer un motif',
    succesLiberer: 'Libération enregistrée : le virement est accepté, son débit va être effectué.',
    succesRejeter: 'Rejet enregistré : le virement est rejeté sans débit.',
    erreurSoldeInsuffisant: 'Libération refusée : solde insuffisant. Le virement est rejeté, sans aucun débit.',
    erreurHeureButoir: 'Heure butoir dépassée : le virement est rejeté.',
    historique: {
      titre: 'Virements traités',
      colonneIssue: 'Issue',
      colonneDate: "Date de l'issue",
    },
    issues: {
      LIBERE: 'Libéré',
      REJETE: 'Rejeté',
      REJETE_HEURE_BUTOIR: 'Rejeté - heure butoir dépassée',
      REJETE_SOLDE_AU_DEBIT: 'Rejeté : solde insuffisant au moment du débit',
    },
  },
  /** Titres d'onglet des pages déclarées (FEAT-002 MSG-002-001, FEAT-015, FEAT-016 MSG-016-001). */
  titresOnglet: {
    tableauDeBord: 'BankApp Demo - Tableau de bord',
    espaceConseiller: 'BankApp Demo - Espace Conseiller',
    administration: 'BankApp Demo - Administration',
    prets: 'BankApp Demo - Prets',
    bourse: 'BankApp Demo - Bourse',
    /** FEAT-010 MESSAGES.md, titre de navigateur. */
    monConseiller: 'BankApp Demo - Mon Conseiller',
    /** MSG-011-001 */
    produits: 'BankApp Demo - Produits bancaires',
    /** FEAT-005 MESSAGES.md, titre de page. */
    releve: 'BankApp Demo - Relevé de compte',
    comptesTiers: 'BankApp Demo - Comptes Tiers',
  },
  /** Écran Relevé de compte : FEAT-005 MESSAGES.md (casse comprise) ; bandeau « @NamedQuery » supprimé (B-22). */
  releve: {
    titre: 'Relevé de compte',
    compte: 'Compte :',
    informations: (type: string, numero: string, solde: string) => `${type} — N° ${numero} — Solde actuel : ${solde}`,
    aucuneTransaction: 'Aucune transaction pour ce compte.',
    compteur: (nombre: number) => `${nombre} opération(s)`,
    colonnes: {
      date: 'Date',
      description: 'Description',
      type: 'Type',
      montant: 'Montant',
      soldeApres: 'Solde après',
    },
    enAttente: 'En attente',
  },
  /**
   * Écran Produits : FEAT-011 MESSAGES.md (casse et absence d'accents comprises) ; titres de section sans
   * « (EJB @Schedule) » (A-011-2, MSG-011-100 et MSG-011-101), bandeau JPA supprimé (MSG-011-004).
   */
  produits: {
    catalogue: 'Catalogue des produits',
    rafraichir: 'Rafraichir',
    nom: 'Nom',
    description: 'Description',
    taux: 'Taux',
    dureeMax: 'Duree max',
    epargne: {
      titre: 'Produits d epargne',
      depotMin: 'Depot min',
      capitalGaranti: 'Capital garanti',
      aucun: 'Aucun produit d epargne disponible',
    },
    credit: {
      titre: 'Produits de credit',
      ltvMax: 'LTV max',
      assuranceRequise: 'Assurance requise',
      aucun: 'Aucun produit de credit disponible',
    },
    investissement: {
      titre: 'Produits d investissement',
      secteur: 'Secteur',
      niveauDeRisque: 'Niveau de risque',
      rendementAttendu: 'Rendement attendu',
      aucun: 'Aucun produit d investissement disponible',
    },
    traitements: {
      separateur: 'Traitements nocturnes',
      titre: 'Historique des traitements nocturnes',
      job: 'Job',
      debut: 'Debut',
      fin: 'Fin',
      duree: 'Duree (ms)',
      enregistrements: 'Enregistrements',
      statut: 'Statut',
      details: 'Details',
      aucun: 'Aucun traitement nocturne enregistre — les jobs s executent a 02h00, 02h30 et le 1er du mois a 03h00',
    },
  },
  /**
   * Écran Mon Conseiller : FEAT-010 MESSAGES.md (casse et absence d'accents comprises) ; bandeau IA supprimé.
   * Le message de bienvenue substitue « cher client » au prénom d'un profil indisponible.
   */
  conseiller: {
    nom: 'Sophie — Conseillère Demo Bank',
    statut: 'En ligne · Répond en quelques secondes',
    nouvelleConversation: 'Nouvelle conversation',
    bienvenue: (prenom: string | null) =>
      `Bonjour ${prenom ?? 'cher client'} ! Je suis Sophie, votre conseillere Demo Bank. Je suis la pour repondre a vos questions sur vos comptes, vos placements ou tout projet financier. Comment puis-je vous aider aujourd'hui ?`,
    /** Libellé du bouton d'action rapide et message envoyé ; `cle` forme le data-testid `chat-action-{cle}`. */
    actions: [
      { cle: 'optimiser-epargne', libelle: 'Optimiser mon epargne', message: 'Je voudrais optimiser mon epargne, que me conseillez-vous ?' },
      { cle: 'projet-immobilier', libelle: 'Projet immobilier', message: 'Je souhaite acheter un appartement, comment preparer mon dossier ?' },
      { cle: 'investir-bourse', libelle: 'Investir en bourse', message: 'Je veux commencer a investir en bourse, par ou commencer ?' },
      { cle: 'preparer-retraite', libelle: 'Preparer ma retraite', message: 'Comment preparer ma retraite efficacement ?' },
    ],
    placeholder: 'Posez votre question a Sophie...',
    envoyer: 'Envoyer',
    envoiEnCours: '...',
    /** RG-010-021 : réponse de Sophie quand le service est en échec sans message exploitable. */
    indisponible: 'Je suis momentanement indisponible. Veuillez reessayer ou contacter votre conseiller en agence.',
  },
  /**
   * Écran Bourse : FEAT-009 et FEAT-029 MESSAGES.md (casse comprise) ; nom accessible du graphique :
   * textes-nouveaux.md, lot « Stories » (bourse.graphique.nom).
   */
  bourse: {
    valeur: 'Valeur',
    nombreDeValeurs: (nombre: number) => `(${nombre} valeurs)`,
    periode: 'Période',
    /** Libellés du sélecteur de période, puis libellés de période du message d'absence de données et du badge du graphique. */
    periodes: {
      '1W': { selecteur: '1 Semaine', libelle: '1 semaine (données journalières)' },
      '1M': { selecteur: '1 Mois', libelle: '1 mois (données journalières)' },
      '1Y': { selecteur: '1 An', libelle: '1 an (données hebdomadaires)' },
      '5Y': { selecteur: '5 Ans', libelle: '5 ans (données hebdomadaires)' },
    },
    ajouterUneValeur: '+ Ajouter une valeur',
    formulaire: {
      titre: '➕ Ajouter une valeur à la watchlist',
      symbole: 'Symbole',
      symboleExemple: 'ex: AMZN',
      symboleAide: 'Symbole Alpha Vantage',
      nom: 'Nom',
      nomExemple: 'ex: Amazon',
      marche: 'Marché',
      devise: 'Devise',
      ajouter: 'Ajouter',
      annuler: 'Annuler',
    },
    /** Valeur envoyée au service et libellé affiché (« London SE » pour la valeur LSE). */
    marches: [
      { valeur: 'Euronext Paris', libelle: 'Euronext Paris' },
      { valeur: 'NASDAQ', libelle: 'NASDAQ' },
      { valeur: 'NYSE', libelle: 'NYSE' },
      { valeur: 'LSE', libelle: 'London SE' },
      { valeur: 'Autre', libelle: 'Autre' },
    ],
    devises: ['EUR', 'USD', 'GBP'],
    valeurAjoutee: 'Valeur ajoutée !',
    kpi: {
      variation: 'Variation sur la période',
      performance: 'Performance %',
      marche: 'Marché',
      titre: (nom: string, symbole: string) => `${nom} — ${symbole}`,
      hausse: '▲ +',
      baisse: '▼ ',
      plus: '+',
    },
    graphique: {
      titre: '📈 Évolution du cours',
      /** bourse.graphique.nom */
      nom: (symbole: string, periode: string) => `Évolution du cours de ${symbole} sur ${periode}`,
    },
    seances: {
      titre: '📋 Dernières séances',
      date: 'Date',
      ouverture: 'Ouverture',
      haut: 'Haut',
      bas: 'Bas',
      cloture: 'Clôture',
      volume: 'Volume',
      variationJour: 'Var. J',
      plus: '+',
    },
    watchlist: {
      titre: (nombre: number) => `⭐ Ma Watchlist (${nombre} valeurs)`,
      symbole: 'Symbole',
      nom: 'Nom',
      marche: 'Marché',
      devise: 'Devise',
      supprimer: 'Suppr.',
      retirer: '✕',
      infobulle: (nom: string) => `Supprimer ${nom} de la watchlist`,
    },
    aucuneDonnee: (symbole: string, periode: string) =>
      `⚠️ Aucune donnée reçue pour « ${symbole} » sur ${periode}. Causes possibles : symbole invalide pour cette place boursière (vérifiez le suffixe, ex: .PA pour Euronext), ou limite de 25 requêtes/jour atteinte (plan gratuit Alpha Vantage). Réessayez dans quelques minutes ou changez de valeur.`,
  },
  /**
   * Écran Comptes Tiers : docs/retro/features/FEAT-012/MESSAGES.md (casse et absence d'accents comprises) ; messages de
   * l'epic 8 (epic-8-context.md) pour l'indisponibilité de Powens et l'échec de la connexion.
   */
  comptesTiers: {
    /** MSG-012-050 à MSG-012-058 */
    connexion: {
      titre: 'Agregez vos comptes bancaires',
      explication:
        'Connectez vos comptes d autres banques pour avoir une vue complete de votre patrimoine financier. Connexion securisee via le protocole OAuth2 PSD2.',
      banques: ['BNP Paribas', 'Credit Agricole', 'La Banque Postale', 'CIC', 'LCL', '+ 1800 banques'],
      connecter: 'Connecter une banque',
    },
    /** MSG-012-040 (503 à la connexion) */
    connexionImpossible: 'Impossible d initialiser la connexion Powens.',
    /** Message de l'epic 8 (A-012-3) : Powens indisponible, jamais une liste vide. */
    powensIndisponible: 'Service Powens temporairement indisponible. Veuillez reessayer dans quelques instants.',
    /** MSG-012-060, MSG-012-061 et bouton exposé par A-012-5 */
    statut: 'Connected via Powens',
    deconnecter: 'Deconnecter',
    rafraichir: 'Rafraichir',
    /** MSG-012-062, MSG-012-063 */
    patrimoine: { libelle: 'Patrimoine total (tous comptes)', fournisseur: 'via Powens Open Banking API' },
    /** MSG-012-064, MSG-012-065 */
    comptes: { titre: (nombre: number) => `Mes comptes externes (${nombre})`, banque: 'Banque externe' },
    /** MSG-012-080 à MSG-012-084 */
    transactions: {
      titre: (nom: string) => `Transactions — ${nom}`,
      aucune: 'Aucune transaction disponible.',
      date: 'Date',
      libelle: 'Libelle',
      montant: 'Montant',
    },
  },
  navigation: {
    logo: 'DB',
    titre: 'Demo Bank',
    /** Orthographe du legacy, sans accents (MESSAGES.md §1.3, §1.4). */
    liens: {
      tableauDeBord: 'Tableau de bord',
      virement: 'Virement',
      virementsProgrammes: 'Virements programmes',
      releve: 'Releve',
      budget: 'Budget',
      prets: 'Prets',
      documents: 'Documents',
      bourse: 'Bourse',
      monConseiller: 'Mon Conseiller',
      produits: 'Produits',
      comptesTiers: 'Comptes Tiers',
      beneficiaires: 'Beneficiaires',
      espaceConseiller: 'Espace Conseiller',
      administration: 'Administration',
    },
    /** Sous-en-têtes des espaces réservés (FEAT-015, FEAT-016). */
    sousLiens: {
      monPortefeuille: 'Mon portefeuille',
      conseillerIa: 'Conseiller IA',
      administration: 'Administration',
      espaceConseiller: 'Espace Conseiller',
      monTableauDeBord: 'Mon tableau de bord',
    },
    badges: {
      conseiller: 'Conseiller',
      admin: 'ADMIN',
    },
    salutation: (nomComplet: string) => `Bonjour, ${nomComplet}`,
    deconnexion: 'Deconnexion',
  },
  /** Page Prêts (FEAT-007) : orthographe du legacy, sans accents ; les séparateurs sont mis en capitales par CSS. */
  prets: {
    separateurs: {
      simulateur: 'Simulateur de pret',
      demande: 'Demande de pret',
      emprunts: 'Mes emprunts',
    },
    simulateur: {
      titre: 'Simuler un emprunt',
      montant: 'Montant (EUR)',
      taux: 'Taux annuel (%)',
      duree: 'Duree (mois)',
      calculer: 'Calculer',
      mensualite: 'Mensualite',
      coutTotal: 'Cout total',
      totalInterets: 'Total interets',
      lienEcheancier: (nombre: number) => `Voir l echeancier complet (${nombre} echeances)`,
      libelleEcheancier: 'Echeancier',
      colonnes: {
        numero: '#',
        capital: 'Capital',
        interets: 'Interets',
        total: 'Total',
        capitalRestant: 'Capital restant',
      },
    },
    demande: {
      titre: 'Nouveau pret',
      type: 'Type de pret',
      montant: 'Montant (EUR)',
      taux: 'Taux annuel (%)',
      duree: 'Duree (mois)',
      objet: 'Objet du pret',
      soumettre: 'Soumettre la demande',
      /** MSG-007-131 : tiret cadratin conservé du legacy. */
      transmise: 'Demande transmise au service credit — traitement en cours (rechargez la page dans quelques secondes)',
    },
    emprunts: {
      titre: 'Emprunts en cours',
      colonnes: {
        type: 'Type',
        montant: 'Montant',
        taux: 'Taux',
        duree: 'Duree',
        mensualite: 'Mensualite',
        totalInterets: 'Total interets',
        statut: 'Statut',
        echeancier: 'Echeancier',
        action: 'Action',
      },
      enCoursDeTraitement: 'En cours de traitement...',
      aucun: 'Aucun emprunt enregistre',
      voir: 'Voir',
      supprimer: 'Supprimer',
    },
    /** Section d'échéancier (MSG-007-080 à 092) ; le séparateur est mis en capitales par CSS. */
    echeancier: {
      separateur: 'Echeancier du pret selectionne',
      titre: 'Detail des echeances',
      colonnes: {
        numero: '#',
        dateEcheance: 'Date echeance',
        capital: 'Capital',
        interets: 'Interets',
        total: 'Total',
        capitalRestant: 'Capital restant',
        statut: 'Statut',
        action: 'Action',
      },
      paye: 'Paye',
      enAttente: 'En attente',
      marquerPaye: 'Marquer paye',
    },
    /** MSG-007-132 */
    succesPaiement: 'Echeance marquee comme payee',
    /** pret.erreur.paiement */
    erreurPaiement: "Erreur : l'échéance n'a pas été marquée comme payée. Veuillez réessayer.",
    /** pret.erreur.suppression */
    erreurSuppression: "Erreur suppression : le prêt n'a pas été supprimé. Veuillez réessayer.",
    /** pret.erreur.demande */
    erreurDemande: "Erreur : la demande de prêt n'a pas été enregistrée. Veuillez réessayer.",
    /** pret.erreur.simulation */
    erreurSimulation: "Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer.",
  },
} as const;
