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
  /** Titres d'onglet des pages déclarées (FEAT-002 MSG-002-001, FEAT-015, FEAT-016 MSG-016-001). */
  titresOnglet: {
    tableauDeBord: 'BankApp Demo - Tableau de bord',
    espaceConseiller: 'BankApp Demo - Espace Conseiller',
    administration: 'BankApp Demo - Administration',
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
