import type { ClientSession } from '../../application/renouvelerSession';
import type { SessionLocale } from '../../application/session';
import type { SignalExpiration } from './signalExpiration';

/** Ce dont le gabarit authentifié a besoin pour déconnecter, renouveler et réagir à une fin de session. */
export interface Session {
  client: ClientSession;
  /** La session de l'utilisateur, telle que l'application la voit (sans jeton). */
  locale: SessionLocale;
  signal: SignalExpiration;
}
