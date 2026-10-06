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
