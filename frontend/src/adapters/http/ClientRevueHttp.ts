import type {
  ActionRevue,
  ClientRevue,
  IssueRefusee,
  ReponseDecision,
  ReponseFile,
  ReponseHistorique,
} from '../../application/revue';
import type { IssueRevue, VirementEnRevue, VirementTraite } from '../../domain/virement';
import { TEXTES } from '../textes';

export const CHEMIN_FILE = '/api/coeur-bancaire/revue/virements-suspendus';
export const CHEMIN_HISTORIQUE = '/api/coeur-bancaire/revue/virements-traites';
const cheminDecision = (virementId: number, action: ActionRevue) =>
  `/api/coeur-bancaire/revue/virements/${virementId}/${action}`;

const ISSUES: readonly string[] = ['LIBERE', 'REJETE', 'REJETE_HEURE_BUTOIR', 'REJETE_SOLDE_AU_DEBIT'];
const ISSUES_REFUSEES: readonly string[] = ['REFUSEE_SOLDE_INSUFFISANT', 'REFUSEE_HEURE_BUTOIR'];

const estVirementEnRevue = (valeur: unknown): valeur is VirementEnRevue => {
  const v = valeur as Partial<VirementEnRevue> | null;
  return (
    typeof v?.virementId === 'number' &&
    typeof v.clientId === 'number' &&
    (v.nomClient === null || typeof v.nomClient === 'string') &&
    typeof v.montant === 'string' &&
    typeof v.compteDestinataire === 'string' &&
    typeof v.motifSuspension === 'string' &&
    typeof v.heureButoir === 'string'
  );
};

const estVirementTraite = (valeur: unknown): valeur is VirementTraite => {
  const v = valeur as Partial<VirementTraite> | null;
  return (
    estVirementEnRevue(v?.virement) &&
    typeof v.issue === 'string' &&
    ISSUES.includes(v.issue as IssueRevue) &&
    typeof v.dateIssue === 'string'
  );
};

/**
 * Adaptateur HTTP de la revue des virements suspendus (cœur bancaire). `appeler` porte le jeton et la règle du 401.
 * Le service vérifie le rôle ADMIN : un autre rôle reçoit 403.
 */
export function creerClientRevueHttp(appeler: typeof fetch): ClientRevue {
  async function lireListe<T>(chemin: string, valide: (valeur: unknown) => valeur is T): Promise<{ statut: 'liste'; virements: readonly T[] } | { statut: 'technique' }> {
    try {
      const reponse = await appeler(chemin, { method: 'GET', headers: { Accept: 'application/json' } });
      if (reponse.status !== 200) {
        return { statut: 'technique' };
      }
      const corps: unknown = await reponse.json();
      return Array.isArray(corps) && corps.every(valide) ? { statut: 'liste', virements: corps } : { statut: 'technique' };
    } catch {
      return { statut: 'technique' };
    }
  }

  return {
    lireFile: (): Promise<ReponseFile> => lireListe(CHEMIN_FILE, estVirementEnRevue),
    lireHistorique: (): Promise<ReponseHistorique> => lireListe(CHEMIN_HISTORIQUE, estVirementTraite),

    async decider(action, virementId, motif): Promise<ReponseDecision> {
      try {
        const reponse = await appeler(cheminDecision(virementId, action), {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
          body: JSON.stringify({ motif }),
        });
        if (reponse.status === 200) {
          return { statut: 'enregistre' };
        }
        const corps = await lireCorps(reponse);
        if (reponse.status === 409 && ISSUES_REFUSEES.includes(String(corps?.issue))) {
          return { statut: 'refuse', issue: corps?.issue as IssueRefusee };
        }
        if (reponse.status === 400 && corps?.message === TEXTES.revue.erreurMotifManquant) {
          return { statut: 'motif' };
        }
        return { statut: 'erreur', message: typeof corps?.message === 'string' ? corps.message : null };
      } catch {
        return { statut: 'erreur', message: null };
      }
    },
  };
}

async function lireCorps(reponse: Response): Promise<{ message?: unknown; issue?: unknown } | null> {
  try {
    const corps: unknown = await reponse.json();
    return typeof corps === 'object' ? (corps as { message?: unknown; issue?: unknown } | null) : null;
  } catch {
    return null;
  }
}
