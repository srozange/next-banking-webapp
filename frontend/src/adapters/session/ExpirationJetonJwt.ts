import type { ExpirationJeton } from '../../application/renouvelerSession';
import { expirationDuJeton } from '../jeton/DecodeurJetonJwt';
import type { Horloge } from './horloge';

/** Secondes restantes avant la revendication `exp` du jeton, lue sur l'horloge injectée ; null si `exp` est illisible. */
export function creerExpirationJeton(horloge: Horloge): ExpirationJeton {
  return {
    secondesRestantes(jeton) {
      const exp = expirationDuJeton(jeton);
      return exp === null ? null : exp - horloge.maintenant() / 1000;
    },
  };
}
