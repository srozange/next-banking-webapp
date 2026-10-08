import type { StockageJeton } from '../stockage/StockageJetonLocal';
import type { SignalExpiration } from './signalExpiration';

/**
 * `fetch` des appels authentifiés : règle unique, tout 401 de tout service supprime le jeton et signale la fin
 * de session (retour à /login sans message). La réponse est rendue telle quelle à l'appelant.
 * Porte le jeton : ajoute `Authorization: Bearer <jeton>` quand l'appelant ne l'a pas déjà mis et qu'un jeton est stocké.
 */
export function creerFetchAuthentifie(
  stockage: StockageJeton,
  signal: SignalExpiration,
  appeler: typeof fetch = (entree, options) => fetch(entree, options),
): typeof fetch {
  return async (entree, options) => {
    const jeton = stockage.lire();
    const avecJeton =
      jeton === null || new Headers(options?.headers).has('Authorization')
        ? options
        : { ...options, headers: ajouterJeton(options?.headers, `Bearer ${jeton}`) };
    const reponse = await appeler(entree, avecJeton);
    if (reponse.status === 401) {
      stockage.supprimer();
      signal.declencher();
    }
    return reponse;
  };
}

/** Ajoute l'en-tête Authorization en gardant la forme d'origine des en-têtes (objet, tableau ou Headers). */
function ajouterJeton(entetes: HeadersInit | undefined, valeur: string): HeadersInit {
  if (entetes instanceof Headers) {
    const copie = new Headers(entetes);
    copie.set('Authorization', valeur);
    return copie;
  }
  if (Array.isArray(entetes)) {
    return [...entetes, ['Authorization', valeur]];
  }
  return { ...entetes, Authorization: valeur };
}
