import { Navigate, useLocation } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import { ROUTE_TABLEAU_DE_BORD, routeAutorisee } from '../../domain/navigation';
import type { ClientProfil } from '../../application/chargerProfil';
import { GabaritAuthentifie } from '../gabarit/GabaritAuthentifie';
import type { Session } from '../session/session';

interface Proprietes {
  clientProfil: ClientProfil;
  session: Session;
}

/**
 * Route réservée aux utilisateurs connectés : sans session, ou avec une session illisible ou sans rôle connu
 * (terminée), remplacement par /login (FR-001-02, FR-030-01). Rôle insuffisant pour une route réservée :
 * remplacement par /tableau-de-bord, sans message et sans rendre la page (FR-001-11, FR-016-01, FR-030-02).
 */
export function RouteProtegee({ clientProfil, session }: Proprietes) {
  const { pathname } = useLocation();
  const roles = session.locale.roles();
  if (roles === null) {
    return <Navigate to={ROUTE_CONNEXION} replace />;
  }
  if (!routeAutorisee(pathname, roles)) {
    return <Navigate to={ROUTE_TABLEAU_DE_BORD} replace />;
  }
  return <GabaritAuthentifie roles={roles} clientProfil={clientProfil} session={session} />;
}
