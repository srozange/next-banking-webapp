/** Règles pures de la saisie d'un virement (FEAT-003) : champs obligatoires et comptes distincts. */

export type ChampVirement = 'compteSource' | 'compteDestinataire' | 'montant';

export interface SaisieVirement {
  /** Identifiant du compte en texte ; vide tant qu'aucun compte n'est choisi. */
  compteSource: string;
  compteDestinataire: string;
  montant: string;
  motif: string;
}

/** Champs obligatoires vides (FR-003-02), dans l'ordre de l'écran. */
export function champsVides(saisie: SaisieVirement): ChampVirement[] {
  const vides: ChampVirement[] = [];
  if (saisie.compteSource === '') {
    vides.push('compteSource');
  }
  if (saisie.compteDestinataire === '') {
    vides.push('compteDestinataire');
  }
  if (saisie.montant.trim() === '') {
    vides.push('montant');
  }
  return vides;
}

/** Même compte en source et en destination (FR-003-03). */
export function comptesIdentiques(saisie: SaisieVirement): boolean {
  return saisie.compteSource === saisie.compteDestinataire;
}
