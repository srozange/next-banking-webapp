import type { ClientBeneficiaires, ReponseBeneficiaires, ReponseSuppression } from '../../application/gererBeneficiaires';
import type { BeneficiaireBrut } from '../../domain/beneficiaires';

export const CHEMIN_BENEFICIAIRES = '/api/coeur-bancaire/beneficiaires';

const estTexteOuNul = (valeur: unknown): valeur is string | null => valeur === null || valeur === undefined || typeof valeur === 'string';

const estBeneficiaire = (valeur: unknown): valeur is BeneficiaireBrut => {
  const b = valeur as Partial<Record<keyof BeneficiaireBrut, unknown>> | null;
  return (
    typeof b?.id === 'number' &&
    typeof b.libelle === 'string' &&
    typeof b.titulaire === 'string' &&
    typeof b.iban === 'string' &&
    estTexteOuNul(b.bic) &&
    estTexteOuNul(b.banque) &&
    typeof b.zoneSepa === 'boolean'
  );
};

/** BIC et banque absents du corps (champ omis) deviennent nuls. */
const normaliser = (b: BeneficiaireBrut): BeneficiaireBrut => ({ ...b, bic: b.bic ?? null, banque: b.banque ?? null });

/**
 * Adaptateur HTTP des bénéficiaires (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie).
 * Suppression : 204 supprimé ; 403 (bénéficiaire d'un autre client) et 404 (inexistant, déjà supprimé) refusée ;
 * tout autre statut et toute coupure, technique. Liste : tout statut autre que 200, corps illisible ou coupure, technique.
 */
export function creerClientBeneficiairesHttp(appeler: typeof fetch): ClientBeneficiaires {
  return {
    async lister(): Promise<ReponseBeneficiaires> {
      try {
        const reponse = await appeler(CHEMIN_BENEFICIAIRES, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return Array.isArray(corps) && corps.every(estBeneficiaire) ? { statut: 'liste', beneficiaires: corps.map(normaliser) } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
    async supprimer(beneficiaireId): Promise<ReponseSuppression> {
      try {
        const reponse = await appeler(`${CHEMIN_BENEFICIAIRES}/${beneficiaireId}`, { method: 'DELETE', headers: { Accept: 'application/json' } });
        if (reponse.status === 204) {
          return { statut: 'supprime' };
        }
        return reponse.status === 403 || reponse.status === 404 ? { statut: 'refuse' } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
