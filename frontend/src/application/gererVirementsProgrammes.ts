import { centimesDepuisSaisie, chaineDepuisCentimes } from '../domain/montant';
import {
  CHAMPS_VIREMENT_PROGRAMME,
  refusDeSaisie,
  type ChampVirementProgramme,
  type FormulaireVirementProgramme,
  type LigneVirementProgramme,
  type RefusSaisie,
} from '../domain/virementsProgrammes';

/** Corps de la création envoyé au service : montant en chaîne à deux décimales, motif nul s'il est vide. */
export interface CommandeVirementProgramme {
  compteSourceId: number;
  numeroBeneficiaire: string;
  libelleBeneficiaire: string;
  montant: string;
  frequence: string;
  motif: string | null;
  datePremiereExecution: string;
}

/** Réponse du service à la lecture des virements programmés, traduite par l'adaptateur HTTP. */
export type ReponseVirementsProgrammes = { statut: 'liste'; virements: readonly LigneVirementProgramme[] } | { statut: 'technique' };

/** Réponse du service à une création : refus avec son texte (et le champ fautif s'il est désigné) ou échec technique. */
export type ReponseCreation =
  | { statut: 'cree' }
  | { statut: 'refuse'; message: string; champ: ChampVirementProgramme | null }
  | { statut: 'technique' };

/** Réponse du service à une suspension, réactivation ou suppression : introuvable couvre autrui, inexistant et état déjà atteint. */
export type ReponseAction = { statut: 'ok' } | { statut: 'introuvable' } | { statut: 'technique' };

/** Port sortant : virements programmés du client (le service déduit le client de la session). */
export interface ClientVirementsProgrammes {
  lister(): Promise<ReponseVirementsProgrammes>;
  creer(commande: CommandeVirementProgramme): Promise<ReponseCreation>;
  suspendre(id: number): Promise<ReponseAction>;
  reactiver(id: number): Promise<ReponseAction>;
  supprimer(id: number): Promise<ReponseAction>;
}

/** Cas d'usage (FR-004-03) : virements du client dans l'ordre du service ; null si le service est indisponible. */
export async function chargerVirementsProgrammes(client: ClientVirementsProgrammes): Promise<readonly LigneVirementProgramme[] | null> {
  try {
    const reponse = await client.lister();
    return reponse.statut === 'liste' ? reponse.virements : null;
  } catch {
    return null;
  }
}

/** Issue d'une création : `liste` est la liste rechargée, nulle si la relecture a échoué. */
export type ResultatCreation =
  | { statut: 'cree'; liste: readonly LigneVirementProgramme[] | null }
  | { statut: 'invalide'; refus: RefusSaisie }
  | { statut: 'refuse'; message: string; champ: ChampVirementProgramme | null }
  | { statut: 'technique' };

/**
 * Cas d'usage (FR-004-04 à 07) : contrôles locaux dans l'ordre du legacy sans appel, puis envoi au service qui
 * contrôle à son tour la saisie, les comptes source et destinataire ; une création réussie recharge la liste.
 */
export async function creerVirementProgramme(formulaire: FormulaireVirementProgramme, client: ClientVirementsProgrammes): Promise<ResultatCreation> {
  const refus = refusDeSaisie(formulaire);
  if (refus !== null) {
    return { statut: 'invalide', refus };
  }
  // Lisible et strictement positif : refusDeSaisie vient de le vérifier.
  const centimes = centimesDepuisSaisie(formulaire.montant) as number;
  const motif = formulaire.motif.trim();
  let reponse: ReponseCreation;
  try {
    reponse = await client.creer({
      compteSourceId: Number(formulaire.compteSourceId),
      numeroBeneficiaire: formulaire.numeroBeneficiaire.trim(),
      libelleBeneficiaire: formulaire.libelleBeneficiaire.trim(),
      montant: chaineDepuisCentimes(centimes),
      frequence: formulaire.frequence,
      motif: motif === '' ? null : motif,
      datePremiereExecution: formulaire.datePremiereExecution.trim(),
    });
  } catch {
    return { statut: 'technique' };
  }
  if (reponse.statut === 'cree') {
    return { statut: 'cree', liste: await chargerVirementsProgrammes(client) };
  }
  return reponse;
}

/** Action sur un virement programmé existant. */
export type ActionVirementProgramme = 'suspendre' | 'reactiver' | 'supprimer';

/** Issue d'une action : `liste` est la liste rechargée, nulle si la relecture a échoué. */
export type ResultatAction =
  | { statut: 'ok'; liste: readonly LigneVirementProgramme[] | null }
  | { statut: 'introuvable' }
  | { statut: 'technique' };

/**
 * Cas d'usage (FR-004-09, FR-004-10) : suspend, réactive ou supprime le virement programmé, puis recharge la liste
 * après un succès. Introuvable (404 : d'autrui, inexistant ou état déjà atteint) et technique restent distincts.
 */
export async function agirSurVirementProgramme(action: ActionVirementProgramme, id: number, client: ClientVirementsProgrammes): Promise<ResultatAction> {
  let reponse: ReponseAction;
  try {
    reponse = await client[action](id);
  } catch {
    return { statut: 'technique' };
  }
  if (reponse.statut === 'ok') {
    return { statut: 'ok', liste: await chargerVirementsProgrammes(client) };
  }
  return reponse;
}

/** Champ du corps JSON du service reconnu, ou null. */
export function champConnu(valeur: unknown): ChampVirementProgramme | null {
  return CHAMPS_VIREMENT_PROGRAMME.includes(valeur as ChampVirementProgramme) ? (valeur as ChampVirementProgramme) : null;
}
