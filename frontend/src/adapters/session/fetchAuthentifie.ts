import type { StockageJeton } from '../../application/seConnecter';
import type { SignalExpiration } from './signalExpiration';

/**
 * `fetch` des appels authentifiés : règle unique, tout 401 de tout service supprime le jeton et signale la fin
 * de session (retour à /login sans message). La réponse est rendue telle quelle à l'appelant.
 */
export function creerFetchAuthentifie(
  stockage: StockageJeton,
  signal: SignalExpiration,
  appeler: typeof fetch = (entree, options) => fetch(entree, options),
): typeof fetch {
  return async (entree, options) => {
    const reponse = await appeler(entree, options);
    if (reponse.status === 401) {
      stockage.supprimer();
      signal.declencher();
    }
    return reponse;
  };
}
