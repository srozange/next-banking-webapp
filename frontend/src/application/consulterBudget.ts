import { budgetAffichable, type Budget, type BudgetBrut } from '../domain/budget';

/** Réponse du service du budget, traduite par l'adaptateur HTTP. */
export type ReponseBudget = { statut: 'budget'; budget: BudgetBrut } | { statut: 'technique' };

/** Port sortant : budget du client de la session (le service sait de quel client il s'agit). */
export interface ClientBudget {
  /** `periode` au format `AAAA-MM`, ou null pour laisser le service choisir la période par défaut. */
  lire(periode: string | null): Promise<ReponseBudget>;
}

/**
 * Cas d'usage (FR-006-01 à FR-006-04) : un appel au service pour la période demandée (ou celle que le service choisit).
 * Null si le service est indisponible ou rend une réponse inexploitable.
 */
export async function chargerBudget(periode: string | null, client: ClientBudget): Promise<Budget | null> {
  try {
    const reponse = await client.lire(periode);
    return reponse.statut === 'budget' ? budgetAffichable(reponse.budget) : null;
  } catch {
    return null;
  }
}
