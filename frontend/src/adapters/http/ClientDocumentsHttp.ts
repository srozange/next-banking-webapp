import type { ClientDocuments, ReponsePageDocuments, ReponseTelechargement } from '../../application/consulterDocuments';
import { parametresTelechargement } from '../../domain/documents';

export const CHEMIN_COMPTES_DOCUMENTS = '/api/documents/comptes';
export const CHEMIN_TELECHARGEMENT = '/api/documents/telechargement';

const FORMAT_DATE = /^\d{4}-\d{2}-\d{2}$/;

const estCompte = (valeur: unknown): valeur is { compteId: number; numero: string; type: string } => {
  const compte = valeur as Partial<Record<'compteId' | 'numero' | 'type', unknown>> | null;
  return Number.isInteger(compte?.compteId) && typeof compte?.numero === 'string' && typeof compte.type === 'string';
};

/**
 * Adaptateur HTTP du service documents. `appeler` porte le jeton et la règle du 401 (fetchAuthentifie : suppression du
 * jeton, retour à /login). Toute réponse autre que 200, tout corps illisible et toute coupure sont rendus « technique ».
 */
export function creerClientDocumentsHttp(appeler: typeof fetch): ClientDocuments {
  return {
    async lirePage(): Promise<ReponsePageDocuments> {
      try {
        const reponse = await appeler(CHEMIN_COMPTES_DOCUMENTS, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps = (await reponse.json()) as { aujourdhui?: unknown; comptes?: unknown } | null;
        return typeof corps?.aujourdhui === 'string' && FORMAT_DATE.test(corps.aujourdhui) && Array.isArray(corps.comptes) && corps.comptes.every(estCompte)
          ? { statut: 'page', aujourdhui: corps.aujourdhui, comptes: corps.comptes }
          : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
    async telecharger(demande): Promise<ReponseTelechargement> {
      try {
        const requete = new URLSearchParams(parametresTelechargement(demande)).toString();
        const reponse = await appeler(`${CHEMIN_TELECHARGEMENT}?${requete}`, { method: 'GET', headers: { Accept: 'application/pdf' } });
        return reponse.status === 200 ? { statut: 'pdf', contenu: await reponse.blob() } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
