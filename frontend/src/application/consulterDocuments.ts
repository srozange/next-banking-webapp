import { anneesIfu, lireAujourdhui, moisRecents, type DemandeDocument, type MoisReleve } from '../domain/documents';

/** Compte du connecté tel que rendu par le service documents. */
export interface CompteDocuments {
  compteId: number;
  numero: string;
  type: string;
}

/** Réponse de la lecture de la page : date du jour (yyyy-MM-dd) et comptes du connecté, ou panne technique. */
export type ReponsePageDocuments =
  | { statut: 'page'; aujourdhui: string; comptes: readonly CompteDocuments[] }
  | { statut: 'technique' };

/** Réponse d'un téléchargement : le PDF reçu (200), ou panne technique (tout autre statut ; le 401 est traité en amont). */
export type ReponseTelechargement = { statut: 'pdf'; contenu: Blob } | { statut: 'technique' };

/** Port sortant : service documents (comptes du connecté et PDF). */
export interface ClientDocuments {
  lirePage(): Promise<ReponsePageDocuments>;
  telecharger(demande: DemandeDocument): Promise<ReponseTelechargement>;
}

/** Page prête à afficher : un bloc par compte avec ses 12 relevés, et les deux années d'IFU. */
export interface PageDocuments {
  comptes: readonly (CompteDocuments & { releves: readonly MoisReleve[] })[];
  anneesIfu: readonly number[];
}

/**
 * Cas d'usage (FR-008-01, FR-008-03, FR-008-05) : lit les comptes du connecté et `aujourdhui` auprès du service, puis
 * calcule les 12 mois récents et les années d'IFU. Null si le service est indisponible ou si la date est inexploitable.
 */
export async function chargerDocuments(client: ClientDocuments): Promise<PageDocuments | null> {
  try {
    const reponse = await client.lirePage();
    if (reponse.statut !== 'page') {
      return null;
    }
    const date = lireAujourdhui(reponse.aujourdhui);
    if (date === null) {
      return null;
    }
    const releves = moisRecents(date.annee, date.mois);
    return { comptes: reponse.comptes.map((compte) => ({ ...compte, releves })), anneesIfu: anneesIfu(date.annee) };
  } catch {
    return null;
  }
}

/** Cas d'usage (FR-008-06) : demande le PDF ; null (aucun onglet à ouvrir) pour toute réponse autre que le PDF ou toute coupure. */
export async function telechargerDocument(client: ClientDocuments, demande: DemandeDocument): Promise<Blob | null> {
  try {
    const reponse = await client.telecharger(demande);
    return reponse.statut === 'pdf' ? reponse.contenu : null;
  } catch {
    return null;
  }
}
