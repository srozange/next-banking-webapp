import { Navigate, Outlet } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import { accueilDeLaSession, type DecodeurJeton, type StockageJeton } from '../../application/seConnecter';

interface Proprietes {
  stockage: StockageJeton;
  decodeur: DecodeurJeton;
}

/**
 * Route réservée aux utilisateurs connectés : sans jeton, ou avec un jeton illisible ou sans rôle connu
 * (supprimé), remplacement par /login (FR-001-02, FR-030-01). Même contrôle que `/` et `*`.
 */
export function RouteProtegee({ stockage, decodeur }: Proprietes) {
  return accueilDeLaSession(stockage, decodeur) === ROUTE_CONNEXION ? <Navigate to={ROUTE_CONNEXION} replace /> : <Outlet />;
}
