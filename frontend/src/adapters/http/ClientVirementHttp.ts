import type { ClientVirement, Compte, ReponseComptes, ReponseVirement } from '../../application/virement';

export const CHEMIN_COMPTES = '/api/coeur-bancaire/comptes';
export const CHEMIN_VIREMENTS = '/api/coeur-bancaire/virements';

const estCompte = (valeur: unknown): valeur is Compte => {
  const compte = valeur as Partial<Compte> | null;
  return typeof compte?.id === 'number' && typeof compte.type === 'string' && typeof compte.numero === 'string' && typeof compte.solde === 'string' && /^-?\d+\.\d{2}$/.test(compte.solde);
};

/**
 * Adaptateur HTTP du virement (cœur bancaire). `appeler` porte le jeton et la règle du 401 (fetchAuthentifie).
 * L'en-tête Idempotency-Key (UUID) est créé à la première soumission et gardé tant qu'aucune réponse définitive
 * (2xx ou 4xx) n'est reçue : une nouvelle tentative après une coupure ou un 5xx rejoue la même soumission.
 */
export function creerClientVirementHttp(
  appeler: typeof fetch,
  genererCle: () => string = () => crypto.randomUUID(),
): ClientVirement {
  let cle: string | null = null;
  return {
    async lireComptes(): Promise<ReponseComptes> {
      try {
        const reponse = await appeler(CHEMIN_COMPTES, { method: 'GET', headers: { Accept: 'application/json' } });
        if (reponse.status !== 200) {
          return { statut: 'technique' };
        }
        const corps: unknown = await reponse.json();
        return Array.isArray(corps) && corps.every(estCompte) ? { statut: 'comptes', comptes: corps } : { statut: 'technique' };
      } catch {
        return { statut: 'technique' };
      }
    },

    async executer(commande): Promise<ReponseVirement> {
      cle ??= genererCle();
      const motif = commande.motif === '' ? {} : { motif: commande.motif };
      try {
        const reponse = await appeler(CHEMIN_VIREMENTS, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', Accept: 'application/json', 'Idempotency-Key': cle },
          body: JSON.stringify({
            compteSourceId: commande.compteSourceId,
            compteDestinataireId: commande.compteDestinataireId,
            montant: commande.montant,
            ...motif,
          }),
        });
        if (reponse.status < 500) {
          cle = null;
        }
        if (reponse.status === 201) {
          return { statut: 'execute' };
        }
        return { statut: 'erreur', message: await lireMessage(reponse) };
      } catch {
        return { statut: 'erreur', message: null };
      }
    },
  };
}

async function lireMessage(reponse: Response): Promise<string | null> {
  try {
    const corps: unknown = await reponse.json();
    const message: unknown = (corps as { message?: unknown } | null)?.message;
    return typeof message === 'string' ? message : null;
  } catch {
    return null;
  }
}
