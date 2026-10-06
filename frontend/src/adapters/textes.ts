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
  /** Titres d'onglet des pages déclarées (FEAT-002 MSG-002-001, FEAT-015, FEAT-016 MSG-016-001). */
  titresOnglet: {
    tableauDeBord: 'BankApp Demo - Tableau de bord',
    espaceConseiller: 'BankApp Demo - Espace Conseiller',
    administration: 'BankApp Demo - Administration',
    prets: 'BankApp Demo - Prets',
    bourse: 'BankApp Demo - Bourse',
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
