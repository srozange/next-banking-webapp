import type { ClientSession, ExpirationJeton } from '../../application/renouvelerSession';
import type { SignalExpiration } from './signalExpiration';

/** Ce dont le gabarit authentifié a besoin pour déconnecter, renouveler et réagir à une fin de session. */
export interface Session {
  client: ClientSession;
  expiration: ExpirationJeton;
  signal: SignalExpiration;
}
