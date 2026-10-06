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
    },
    /** pret.erreur.simulation */
    erreurSimulation: "Erreur simulation : le calcul n'a pas pu être effectué. Veuillez réessayer.",
  },
} as const;
