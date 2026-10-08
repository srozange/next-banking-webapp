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
