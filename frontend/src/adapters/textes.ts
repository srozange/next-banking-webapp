/**
 * Textes affichés, jamais inventés : repris du legacy (docs/retro/features/FEAT-001/MESSAGES.md,
 * docs/retro/00-global/MESSAGES.md §1.1-1.4 pour la navigation) ou de textes-nouveaux.md (clé entre parenthèses).
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
  },
  /** Titres d'onglet des pages déclarées (FEAT-002 MSG-002-001, FEAT-015, FEAT-016 MSG-016-001). */
  titresOnglet: {
    tableauDeBord: 'BankApp Demo - Tableau de bord',
    espaceConseiller: 'BankApp Demo - Espace Conseiller',
    administration: 'BankApp Demo - Administration',
    bourse: 'BankApp Demo - Bourse',
    /** FEAT-010 MESSAGES.md, titre de navigateur. */
    monConseiller: 'BankApp Demo - Mon Conseiller',
    /** MSG-011-001 */
    produits: 'BankApp Demo - Produits bancaires',
    /** FEAT-005 MESSAGES.md, titre de page. */
    releve: 'BankApp Demo - Relevé de compte',
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
} as const;
