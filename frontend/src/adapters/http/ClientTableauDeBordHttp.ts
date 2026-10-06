import type { ClientTableauDeBord, ReponseTableauDeBord } from '../../application/consulterTableauDeBord';
import type { OperationBrute } from '../../domain/releve';
import type { CompteTableauBrut } from '../../domain/tableauDeBord';

export const CHEMIN_TABLEAU_DE_BORD = '/api/coeur-bancaire/tableau-de-bord';

const FORMAT_MONTANT = /^-?\d+\.\d{2}$/;

const estCompte = (valeur: unknown): valeur is CompteTableauBrut => {
  const compte = valeur as Partial<Record<keyof CompteTableauBrut, unknown>> | null;
  return (
    typeof compte?.id === 'number' &&
    typeof compte.type === 'string' &&
    typeof compte.numero === 'string' &&
    typeof compte.solde === 'string' &&
    FORMAT_MONTANT.test(compte.solde)
  );
};

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
 * Adaptateur HTTP du tableau de bord (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie) ;
 * aucun paramètre n'est envoyé : l'identité vient du jeton. Toute réponse autre que 200, tout corps illisible et
 * toute coupure sont rendus « technique ».
 */
export function creerClientTableauDeBordHttp(appeler: typeof fetch): ClientTableauDeBord {
  return {
    async lire(): Promise<ReponseTableauDeBord> {
      try {
        const reponse = await appeler(CHEMIN_TABLEAU_DE_BORD, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps = (await reponse.json()) as { comptes?: unknown; dernieresOperations?: unknown } | null;
        const comptes = corps?.comptes;
        const operations = corps?.dernieresOperations;
        return Array.isArray(comptes) && comptes.every(estCompte) && Array.isArray(operations) && operations.every(estOperation)
          ? { statut: 'tableau', comptes, dernieresOperations: operations }
          : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
