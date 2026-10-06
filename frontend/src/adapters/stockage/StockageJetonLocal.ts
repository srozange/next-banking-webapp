import type { StockageJeton } from '../../application/seConnecter';

/** Clé du jeton de session dans le localStorage (jamais le sessionStorage). */
export const CLE_JETON = 'nb.jeton';

/** Le localStorage du navigateur est lu à chaque appel, jamais à la création. */
export function creerStockageJetonLocal(stockage: () => Storage = () => localStorage): StockageJeton {
  return {
    lire: () => stockage().getItem(CLE_JETON),
    enregistrer: (jeton) => stockage().setItem(CLE_JETON, jeton),
    supprimer: () => stockage().removeItem(CLE_JETON),
  };
}
