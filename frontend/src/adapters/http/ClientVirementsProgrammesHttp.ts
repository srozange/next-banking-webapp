import { champConnu, type ClientVirementsProgrammes, type ReponseAction, type ReponseCreation, type ReponseVirementsProgrammes } from '../../application/gererVirementsProgrammes';
import type { LigneVirementProgramme } from '../../domain/virementsProgrammes';

export const CHEMIN_VIREMENTS_PROGRAMMES = '/api/coeur-bancaire/virements-programmes';

const estTexteOuNul = (valeur: unknown): valeur is string | null => valeur === null || valeur === undefined || typeof valeur === 'string';

const estVirement = (valeur: unknown): valeur is LigneVirementProgramme => {
  const v = valeur as Partial<Record<keyof LigneVirementProgramme, unknown>> | null;
  return (
    typeof v?.id === 'number' &&
    typeof v.compteSourceId === 'number' &&
    typeof v.numeroCompteSource === 'string' &&
    typeof v.numeroBeneficiaire === 'string' &&
    typeof v.libelleBeneficiaire === 'string' &&
    typeof v.montant === 'string' &&
    typeof v.frequence === 'string' &&
    estTexteOuNul(v.motif) &&
    typeof v.prochaineExecution === 'string' &&
    typeof v.actif === 'boolean' &&
    typeof v.nbExecutions === 'number'
  );
};

const JSON_ENTETES = { Accept: 'application/json', 'Content-Type': 'application/json' };

/**
 * Adaptateur HTTP des virements programmés (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie).
 * Liste : 200 avec un tableau lisible, sinon technique. Création : 201 créé ; 400, 403 et 404 avec `message` refusé
 * (le `champ` désigne le champ fautif) ; tout autre statut, corps illisible ou coupure, technique.
 * Suspension, réactivation (POST) et suppression (DELETE) : 204 ok, 404 introuvable, tout autre statut ou coupure technique.
 */
export function creerClientVirementsProgrammesHttp(appeler: typeof fetch): ClientVirementsProgrammes {
  async function agir(chemin: string, method: 'POST' | 'DELETE'): Promise<ReponseAction> {
    try {
      const reponse = await appeler(chemin, { method, headers: { Accept: 'application/json' } });
      if (reponse.status === 204) {
        return { statut: 'ok' };
      }
      return reponse.status === 404 ? { statut: 'introuvable' } : { statut: 'technique' };
    } catch {
      return { statut: 'technique' };
    }
  }

  return {
    async lister(): Promise<ReponseVirementsProgrammes> {
      try {
        const reponse = await appeler(CHEMIN_VIREMENTS_PROGRAMMES, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return Array.isArray(corps) && corps.every(estVirement)
          ? { statut: 'liste', virements: corps.map((v) => ({ ...v, motif: v.motif ?? null })) }
          : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
    async creer(commande): Promise<ReponseCreation> {
      try {
        const reponse = await appeler(CHEMIN_VIREMENTS_PROGRAMMES, { method: 'POST', headers: JSON_ENTETES, body: JSON.stringify(commande) });
        if (reponse.status === 201) {
          return { statut: 'cree' };
        }
        if (reponse.status !== 400 && reponse.status !== 403 && reponse.status !== 404) {
          return { statut: 'technique' };
        }
        const corps = (await reponse.json()) as { message?: unknown; champ?: unknown } | null;
        return typeof corps?.message === 'string' ? { statut: 'refuse', message: corps.message, champ: champConnu(corps.champ) } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
    suspendre: (id) => agir(`${CHEMIN_VIREMENTS_PROGRAMMES}/${id}/suspension`, 'POST'),
    reactiver: (id) => agir(`${CHEMIN_VIREMENTS_PROGRAMMES}/${id}/reactivation`, 'POST'),
    supprimer: (id) => agir(`${CHEMIN_VIREMENTS_PROGRAMMES}/${id}`, 'DELETE'),
  };
}
