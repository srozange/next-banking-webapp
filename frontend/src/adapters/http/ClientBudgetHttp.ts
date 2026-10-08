import type { ClientBudget, ReponseBudget } from '../../application/consulterBudget';
import type { BudgetBrut, LigneRepartitionBrute } from '../../domain/budget';

export const CHEMIN_BUDGET = '/api/coeur-bancaire/budget';

const FORMAT_MONTANT = /^\d+\.\d{2}$/;
const FORMAT_PERIODE = /^\d{4}-\d{2}$/;
const FORMAT_DATE = /^\d{4}-\d{2}-\d{2}$/;

const estLigne = (valeur: unknown): valeur is LigneRepartitionBrute => {
  const ligne = valeur as Partial<Record<keyof LigneRepartitionBrute, unknown>> | null;
  return typeof ligne?.categorie === 'string' && typeof ligne.montant === 'string' && FORMAT_MONTANT.test(ligne.montant);
};

const estBudget = (valeur: unknown): valeur is BudgetBrut => {
  const budget = valeur as Partial<Record<keyof BudgetBrut, unknown>> | null;
  return (
    typeof budget?.aujourdhui === 'string' &&
    FORMAT_DATE.test(budget.aujourdhui) &&
    typeof budget.periode === 'string' &&
    FORMAT_PERIODE.test(budget.periode) &&
    Array.isArray(budget.periodes) &&
    budget.periodes.every((periode) => typeof periode === 'string' && FORMAT_PERIODE.test(periode)) &&
    typeof budget.totalDepenses === 'string' &&
    FORMAT_MONTANT.test(budget.totalDepenses) &&
    typeof budget.totalRevenus === 'string' &&
    FORMAT_MONTANT.test(budget.totalRevenus) &&
    Number.isInteger(budget.plafondsDefinis) &&
    (budget.plafondsDefinis as number) >= 0 &&
    Array.isArray(budget.repartition) &&
    budget.repartition.every(estLigne)
  );
};

/**
 * Adaptateur HTTP du budget (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie).
 * Toute réponse autre que 200, tout corps illisible ou de forme inattendue et toute coupure sont rendus « technique ».
 */
export function creerClientBudgetHttp(appeler: typeof fetch): ClientBudget {
  return {
    async lire(periode): Promise<ReponseBudget> {
      try {
        const adresse = periode === null ? CHEMIN_BUDGET : `${CHEMIN_BUDGET}?periode=${encodeURIComponent(periode)}`;
        const reponse = await appeler(adresse, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return estBudget(corps) ? { statut: 'budget', budget: corps } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },
  };
}
