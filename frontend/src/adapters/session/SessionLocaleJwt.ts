import { accueilDuRole } from '../../domain/connexion';
import type { SessionLocale } from '../../application/session';
import type { DecodeurJeton } from '../jeton/DecodeurJetonJwt';
import type { StockageJeton } from '../stockage/StockageJetonLocal';
import type { ExpirationJeton } from './ExpirationJetonJwt';

/**
 * Session locale posée sur le jeton stocké : l'application n'en voit que les rôles, la fin de session et le temps
 * restant. Un jeton illisible ou sans rôle connu est supprimé.
 */
export function creerSessionLocale(stockage: StockageJeton, decodeur: DecodeurJeton, expiration: ExpirationJeton): SessionLocale {
  return {
    ouverte: () => stockage.lire() !== null,
    roles() {
      const jeton = stockage.lire();
      if (jeton === null) {
        return null;
      }
      const roles = decodeur.roles(jeton);
      if (roles === null || accueilDuRole(roles) === null) {
        stockage.supprimer();
        return null;
      }
      return roles;
    },
    terminer: () => stockage.supprimer(),
    secondesRestantes() {
      const jeton = stockage.lire();
      return jeton === null ? null : expiration.secondesRestantes(jeton);
    },
  };
}
