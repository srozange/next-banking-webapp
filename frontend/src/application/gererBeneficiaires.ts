import { ligneBeneficiaire, type BeneficiaireBrut, type LigneBeneficiaire } from '../domain/beneficiaires';

/** Réponse du service à la lecture des bénéficiaires, traduite par l'adaptateur HTTP. */
export type ReponseBeneficiaires = { statut: 'liste'; beneficiaires: readonly BeneficiaireBrut[] } | { statut: 'technique' };

/** Réponse du service à une suppression : refusée (bénéficiaire d'un autre client, inexistant ou déjà supprimé) ou en échec technique. */
export type ReponseSuppression = { statut: 'supprime' } | { statut: 'refuse' } | { statut: 'technique' };

/** Port sortant : bénéficiaires externes du client (le service déduit le client de la session et décide de la propriété). */
export interface ClientBeneficiaires {
  lister(): Promise<ReponseBeneficiaires>;
  supprimer(beneficiaireId: number): Promise<ReponseSuppression>;
}

/** Issue d'une suppression : `liste` est la liste rechargée, nulle si la relecture a échoué. */
export type ResultatSuppression =
  | { statut: 'supprime'; liste: readonly LigneBeneficiaire[] | null }
  | { statut: 'refuse'; liste: readonly LigneBeneficiaire[] | null }
  | { statut: 'technique' };

/** Cas d'usage (FR-013-02) : bénéficiaires actifs du client, dans l'ordre du service ; null si le service est indisponible. */
export async function chargerBeneficiaires(client: ClientBeneficiaires): Promise<readonly LigneBeneficiaire[] | null> {
  try {
    const reponse = await client.lister();
    return reponse.statut === 'liste' ? reponse.beneficiaires.map(ligneBeneficiaire) : null;
  } catch {
    return null;
  }
}

/**
 * Cas d'usage (FR-013-09, FR-013-10) : supprime le bénéficiaire puis recharge la liste, sans rechargement de page.
 * Un refus (autrui, inexistant, déjà supprimé) recharge aussi la liste : elle dit ce qui est réellement actif.
 */
export async function supprimerBeneficiaire(beneficiaireId: number, client: ClientBeneficiaires): Promise<ResultatSuppression> {
  let reponse: ReponseSuppression;
  try {
    reponse = await client.supprimer(beneficiaireId);
  } catch {
    return { statut: 'technique' };
  }
  if (reponse.statut === 'technique') {
    return { statut: 'technique' };
  }
  return { statut: reponse.statut, liste: await chargerBeneficiaires(client) };
}
