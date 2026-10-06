import { Navigate, useLocation } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import { ROUTE_TABLEAU_DE_BORD, routeAutorisee } from '../../domain/navigation';
import { rolesDeLaSession, type ClientProfil } from '../../application/chargerProfil';
import type { DecodeurJeton, StockageJeton } from '../../application/seConnecter';
import { GabaritAuthentifie } from '../gabarit/GabaritAuthentifie';
import type { Session } from '../session/session';

interface Proprietes {
  stockage: StockageJeton;
  decodeur: DecodeurJeton;
  clientProfil: ClientProfil;
  session: Session;
}

/**
 * Route réservée aux utilisateurs connectés : sans jeton, ou avec un jeton illisible ou sans rôle connu
 * (supprimé), remplacement par /login (FR-001-02, FR-030-01). Rôle insuffisant pour une route réservée :
 * remplacement par /tableau-de-bord, sans message et sans rendre la page (FR-001-11, FR-016-01, FR-030-02).
 */
export function RouteProtegee({ stockage, decodeur, clientProfil, session }: Proprietes) {
  const { pathname } = useLocation();
  const roles = rolesDeLaSession(stockage, decodeur);
  if (roles === null) {
    return <Navigate to={ROUTE_CONNEXION} replace />;
  }
  if (!routeAutorisee(pathname, roles)) {
    return <Navigate to={ROUTE_TABLEAU_DE_BORD} replace />;
  }
  return <GabaritAuthentifie roles={roles} clientProfil={clientProfil} stockage={stockage} session={session} />;
}
