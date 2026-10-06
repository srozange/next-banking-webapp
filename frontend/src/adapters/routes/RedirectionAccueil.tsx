import { Navigate } from 'react-router';
import { accueilDeLaSession, type DecodeurJeton, type StockageJeton } from '../../application/seConnecter';

interface Proprietes {
  stockage: StockageJeton;
  decodeur: DecodeurJeton;
}

/** `/` et toute route non déclarée : accueil du rôle de la session, sinon /login (remplacement). */
export function RedirectionAccueil({ stockage, decodeur }: Proprietes) {
  return <Navigate to={accueilDeLaSession(stockage, decodeur)} replace />;
}
