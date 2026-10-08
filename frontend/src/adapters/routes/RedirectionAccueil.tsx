import { Navigate } from 'react-router';
import type { SessionLocale } from '../../application/session';
import { accueilDeLaSession } from '../../application/seConnecter';

interface Proprietes {
  session: SessionLocale;
}

/** `/` et toute route non déclarée : accueil du rôle de la session, sinon /login (remplacement). */
export function RedirectionAccueil({ session }: Proprietes) {
  return <Navigate to={accueilDeLaSession(session)} replace />;
}
