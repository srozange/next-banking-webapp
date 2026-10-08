import { expirationDuJeton } from '../jeton/DecodeurJetonJwt';
import type { Horloge } from './horloge';

/** Temps restant avant l'expiration d'un jeton, null si illisible. */
export interface ExpirationJeton {
  secondesRestantes(jeton: string): number | null;
}

/** Secondes restantes avant la revendication `exp` du jeton, lue sur l'horloge injectée ; null si `exp` est illisible. */
export function creerExpirationJeton(horloge: Horloge): ExpirationJeton {
  return {
    secondesRestantes(jeton) {
      const exp = expirationDuJeton(jeton);
      return exp === null ? null : exp - horloge.maintenant() / 1000;
    },
  };
}
