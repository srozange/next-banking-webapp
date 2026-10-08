import { chaineDepuisCentimes } from '../domain/montant';
import { lirePlafond, refusDePlafond, type FormulairePlafond, type RefusPlafond } from '../domain/plafond';

/** Corps de l'enregistrement envoyé au service : `mois` = période affichée (`AAAA-MM`), plafond en chaîne à deux décimales. */
export interface CommandePlafond {
  categorie: string;
  mois: string;
  plafond: string;
}

/** Réponse du service à un enregistrement : refus de saisie (400) avec son texte, ou échec technique. */
export type ReponseEnregistrementPlafond = { statut: 'enregistre' } | { statut: 'refuse'; message: string } | { statut: 'technique' };

/** Réponse du service à une suppression : tout échec, y compris un plafond introuvable, est technique pour l'écran. */
export type ReponseSuppressionPlafond = { statut: 'supprime' } | { statut: 'technique' };

/** Port sortant : plafonds du client (le service déduit le client de la session). */
export interface ClientPlafonds {
  enregistrer(commande: CommandePlafond): Promise<ReponseEnregistrementPlafond>;
  supprimer(id: number): Promise<ReponseSuppressionPlafond>;
}

/** Issue d'un enregistrement. */
export type ResultatEnregistrementPlafond =
  | { statut: 'enregistre'; categorie: string }
  | { statut: 'invalide'; refus: RefusPlafond }
  | { statut: 'refuse'; message: string }
  | { statut: 'technique' };

/**
 * Cas d'usage (FR-006-05, FR-006-07) : contrôles locaux (catégorie, puis plafond) sans appel, puis envoi au service,
 * qui les contrôle à son tour. Le plafond part en chaîne à deux décimales, calculée en centimes entiers.
 */
export async function enregistrerPlafond(formulaire: FormulairePlafond, mois: string, client: ClientPlafonds): Promise<ResultatEnregistrementPlafond> {
  const refus = refusDePlafond(formulaire);
  if (refus !== null) {
    return { statut: 'invalide', refus };
  }
  // Lisible et strictement positif : refusDePlafond vient de le vérifier.
  const lecture = lirePlafond(formulaire.plafond) as { centimes: number };
  let reponse: ReponseEnregistrementPlafond;
  try {
    reponse = await client.enregistrer({ categorie: formulaire.categorie, mois, plafond: chaineDepuisCentimes(lecture.centimes) });
  } catch {
    return { statut: 'technique' };
  }
  return reponse.statut === 'enregistre' ? { statut: 'enregistre', categorie: formulaire.categorie } : reponse;
}

/** Cas d'usage (FR-006-08) : supprime le plafond sans confirmation ; vrai si le service l'a supprimé. */
export async function supprimerPlafond(id: number, client: ClientPlafonds): Promise<boolean> {
  try {
    return (await client.supprimer(id)).statut === 'supprime';
  } catch {
    return false;
  }
}
