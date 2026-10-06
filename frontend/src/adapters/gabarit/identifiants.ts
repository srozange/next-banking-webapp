import type { CleLien, CleSousLien } from '../../domain/navigation';

/**
 * `data-testid` de la navigation, écrits en littéraux (jamais composés) : liens d'en-tête `nav-{route sans /}`,
 * sous-en-tête `nav-sous-{élément}`. Ils servent aussi d'`id` aux liens d'en-tête, qui nomment le sous-en-tête.
 */
export const TESTID_LIENS: Readonly<Record<CleLien, string>> = {
  tableauDeBord: 'nav-tableau-de-bord',
  virement: 'nav-virement',
  virementsProgrammes: 'nav-virements-programmes',
  releve: 'nav-releve',
  budget: 'nav-budget',
  prets: 'nav-prets',
  documents: 'nav-documents',
  bourse: 'nav-bourse',
  monConseiller: 'nav-mon-conseiller',
  produits: 'nav-produits',
  comptesTiers: 'nav-comptes-tiers',
  beneficiaires: 'nav-beneficiaires',
  espaceConseiller: 'nav-espace-conseiller',
  administration: 'nav-administration',
};

export const TESTID_SOUS_LIENS: Readonly<Record<CleSousLien, string>> = {
  monPortefeuille: 'nav-sous-mon-portefeuille',
  conseillerIa: 'nav-sous-conseiller-ia',
  administration: 'nav-sous-administration',
  espaceConseiller: 'nav-sous-espace-conseiller',
  monTableauDeBord: 'nav-sous-mon-tableau-de-bord',
};
