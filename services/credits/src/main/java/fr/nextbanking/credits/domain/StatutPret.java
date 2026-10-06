package fr.nextbanking.credits.domain;

/**
 * Statuts d'un prêt du legacy ; DEFAUT existe sans transition qui y mène.
 */
public enum StatutPret {
    EN_ATTENTE, EN_COURS, REMBOURSE, REFUSE, DEFAUT
}
