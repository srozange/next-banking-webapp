import type { ActionPret, ClientPipelinePrets, ReponseDecisionPret, ReponseListePrets } from '../../application/pipelinePrets';
import type { PretAdministration, StatutPret, TypePret } from '../../domain/pret';

export const CHEMIN_PIPELINE = '/api/credits/administration/prets/en-attente';
export const CHEMIN_HISTORIQUE_PRETS = '/api/credits/administration/prets';
const cheminDecision = (pretId: number, action: ActionPret) =>
  `/api/credits/administration/prets/${pretId}/${action === 'approuver' ? 'approbation' : 'refus'}`;

const TYPES: readonly string[] = ['IMMOBILIER', 'AUTO', 'CONSOMMATION', 'TRAVAUX', 'ETUDIANT'] satisfies TypePret[];
const STATUTS: readonly string[] = ['EN_ATTENTE', 'EN_COURS', 'REMBOURSE', 'REFUSE', 'DEFAUT'] satisfies StatutPret[];

const estPretAdministration = (valeur: unknown): valeur is PretAdministration => {
  const p = valeur as Partial<PretAdministration> | null;
  return (
    typeof p?.id === 'number' &&
    typeof p.clientId === 'number' &&
    typeof p.nomClient === 'string' &&
    (p.type === null || (typeof p.type === 'string' && TYPES.includes(p.type))) &&
    typeof p.montant === 'string' &&
    typeof p.statut === 'string' &&
    STATUTS.includes(p.statut) &&
    typeof p.dureeMois === 'number' &&
    typeof p.tauxAnnuel === 'string' &&
    typeof p.mensualite === 'string' &&
    (p.objet === null || typeof p.objet === 'string') &&
    typeof p.dateDebut === 'string'
  );
};

/**
 * Adaptateur HTTP des prêts de l'administration (service `credits`). `appeler` porte le jeton et la règle du 401. Le
 * service vérifie le rôle ADMIN : un autre rôle reçoit 403, traité comme tout autre échec.
 */
export function creerClientPipelinePretsHttp(appeler: typeof fetch): ClientPipelinePrets {
  async function lireListe(chemin: string): Promise<ReponseListePrets> {
    try {
      const reponse = await appeler(chemin, { method: 'GET', headers: { Accept: 'application/json' } });
      if (reponse.status !== 200) {
        return { statut: 'technique' };
      }
      const corps: unknown = await reponse.json();
      return Array.isArray(corps) && corps.every(estPretAdministration) ? { statut: 'liste', prets: corps } : { statut: 'technique' };
    } catch {
      return { statut: 'technique' };
    }
  }

  return {
    lireEnAttente: () => lireListe(CHEMIN_PIPELINE),
    lireHistorique: () => lireListe(CHEMIN_HISTORIQUE_PRETS),

    async decider(action, pretId): Promise<ReponseDecisionPret> {
      try {
        const reponse = await appeler(cheminDecision(pretId, action), { method: 'POST', headers: { Accept: 'application/json' } });
        return reponse.status === 200 ? { statut: 'effectuee' } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
