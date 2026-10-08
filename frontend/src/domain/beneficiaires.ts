/** Règles pures de l'écran des bénéficiaires externes (FEAT-013) : lignes lues du service, valeurs absentes normalisées. */

/** Bénéficiaire tel que rendu par le service : IBAN déjà masqué (`FR35***9010`), BIC et banque nuls s'ils sont absents. */
export interface BeneficiaireBrut {
  id: number;
  libelle: string;
  titulaire: string;
  iban: string;
  bic: string | null;
  banque: string | null;
  zoneSepa: boolean;
}

/** Ligne prête à afficher : `bic` et `banque` sont nuls quand ils ne sont pas renseignés (blanc compris, RG-013-008, RG-013-009). */
export interface LigneBeneficiaire {
  id: number;
  libelle: string;
  titulaire: string;
  iban: string;
  bic: string | null;
  banque: string | null;
  zoneSepa: boolean;
}

const renseigne = (valeur: string | null): string | null => (valeur === null || valeur.trim() === '' ? null : valeur);

export function ligneBeneficiaire(brut: BeneficiaireBrut): LigneBeneficiaire {
  return { ...brut, bic: renseigne(brut.bic), banque: renseigne(brut.banque) };
}

/** Champs du formulaire d'ajout, nommés comme dans le corps JSON du service. */
export type ChampBeneficiaire = 'libelle' | 'titulaire' | 'iban' | 'bic' | 'banque';

export const CHAMPS_BENEFICIAIRE: readonly ChampBeneficiaire[] = ['libelle', 'titulaire', 'iban', 'bic', 'banque'];

/** Saisie du formulaire d'ajout, telle que tapée : le service valide et normalise (aucun contrôle MOD-97 ici, FR-013-04). */
export interface FormulaireBeneficiaire {
  libelle: string;
  titulaire: string;
  iban: string;
  bic: string;
  banque: string;
}

export const FORMULAIRE_BENEFICIAIRE_VIDE: FormulaireBeneficiaire = { libelle: '', titulaire: '', iban: '', bic: '', banque: '' };

/** Longueur maximale du libellé, du titulaire et de la banque (FR-013-06) ; le service la contrôle aussi. */
export const LONGUEUR_MAX_BENEFICIAIRE = 100;

/** Résultat de la vérification d'un IBAN rendu par le service, sans texte : l'écran compose le message. */
export type VerificationIban = { statut: 'vide' } | { statut: 'invalide' } | { statut: 'valide'; codePays: string; zoneSepa: boolean };
