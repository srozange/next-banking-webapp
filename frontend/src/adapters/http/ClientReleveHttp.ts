import type { ClientReleve, ReponseOperations } from '../../application/consulterReleve';
import type { OperationBrute } from '../../domain/releve';

export const CHEMIN_COMPTES = '/api/coeur-bancaire/comptes';

const FORMAT_MONTANT = /^-?\d+\.\d{2}$/;

const estOperation = (valeur: unknown): valeur is OperationBrute => {
  const operation = valeur as Partial<Record<keyof OperationBrute, unknown>> | null;
  return (
    typeof operation?.date === 'string' &&
    typeof operation.description === 'string' &&
    (operation.type === 'CREDIT' || operation.type === 'DEBIT') &&
    typeof operation.montant === 'string' &&
    FORMAT_MONTANT.test(operation.montant) &&
    typeof operation.soldeApres === 'string' &&
    FORMAT_MONTANT.test(operation.soldeApres)
  );
};

/**
 * Adaptateur HTTP du relevé (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie).
 * Toute réponse autre que 200 (403 compte d'un autre client, 404 compte inexistant, 5xx), tout corps illisible et
 * toute coupure sont rendus « technique ».
 */
export function creerClientReleveHttp(appeler: typeof fetch): ClientReleve {
  return {
    async lireOperations(compteId): Promise<ReponseOperations> {
      try {
        const reponse = await appeler(`${CHEMIN_COMPTES}/${compteId}/operations`, {
          method: 'GET',
          headers: { Accept: 'application/json' },
        });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return Array.isArray(corps) && corps.every(estOperation) ? { statut: 'operations', operations: corps } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
