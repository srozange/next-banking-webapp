import {
  ligneBeneficiaire,
  type BeneficiaireBrut,
  type ChampBeneficiaire,
  type FormulaireBeneficiaire,
  type LigneBeneficiaire,
  type VerificationIban,
} from '../domain/beneficiaires';

/** Réponse du service à la lecture des bénéficiaires, traduite par l'adaptateur HTTP. */
export type ReponseBeneficiaires = { statut: 'liste'; beneficiaires: readonly BeneficiaireBrut[] } | { statut: 'technique' };

/** Réponse du service à une suppression : refusée (bénéficiaire d'un autre client, inexistant ou déjà supprimé) ou en échec technique. */
export type ReponseSuppression = { statut: 'supprime' } | { statut: 'refuse' } | { statut: 'technique' };

/** Réponse du service à la vérification d'un IBAN. */
export type ReponseVerificationIban = { statut: 'verifiee'; verification: VerificationIban } | { statut: 'technique' };

/** Réponse du service à un ajout : refus de saisie (texte du service et champ fautif) ou échec technique. */
export type ReponseAjout =
  | { statut: 'ajoute' }
  | { statut: 'refuse'; message: string; champ: ChampBeneficiaire | null }
  | { statut: 'technique' };

/** Port sortant : bénéficiaires externes du client (le service déduit le client de la session et décide de la propriété). */
export interface ClientBeneficiaires {
  lister(): Promise<ReponseBeneficiaires>;
  supprimer(beneficiaireId: number): Promise<ReponseSuppression>;
  verifierIban(iban: string): Promise<ReponseVerificationIban>;
  ajouter(formulaire: FormulaireBeneficiaire): Promise<ReponseAjout>;
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

/** Issue de la vérification d'un IBAN : le résultat du service, ou `technique` s'il est indisponible. */
export type ResultatVerificationIban = VerificationIban | { statut: 'technique' };

/** Cas d'usage (FR-013-04) : fait vérifier l'IBAN saisi par le service, sans rien enregistrer. */
export async function verifierIban(iban: string, client: ClientBeneficiaires): Promise<ResultatVerificationIban> {
  try {
    const reponse = await client.verifierIban(iban);
    return reponse.statut === 'verifiee' ? reponse.verification : { statut: 'technique' };
  } catch {
    return { statut: 'technique' };
  }
}

/** Issue d'un ajout : `liste` est la liste rechargée, nulle si la relecture a échoué. */
export type ResultatAjout =
  | { statut: 'ajoute'; liste: readonly LigneBeneficiaire[] | null }
  | { statut: 'refuse'; message: string; champ: ChampBeneficiaire | null }
  | { statut: 'technique' };

/** Cas d'usage (FR-013-05 à 08) : envoie la saisie au service, qui valide et normalise ; un ajout réussi recharge la liste. */
export async function ajouterBeneficiaire(formulaire: FormulaireBeneficiaire, client: ClientBeneficiaires): Promise<ResultatAjout> {
  let reponse: ReponseAjout;
  try {
    reponse = await client.ajouter(formulaire);
  } catch {
    return { statut: 'technique' };
  }
  if (reponse.statut === 'ajoute') {
    return { statut: 'ajoute', liste: await chargerBeneficiaires(client) };
  }
  return reponse;
}
