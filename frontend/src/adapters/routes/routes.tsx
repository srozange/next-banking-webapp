import type { RouteObject } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import type { ClientConnexion, DecodeurJeton, StockageJeton } from '../../application/seConnecter';
import { Application } from '../Application';
import { EcranConnexion } from '../ecrans/connexion/EcranConnexion';
import { RedirectionAccueil } from './RedirectionAccueil';
import { RouteProtegee } from './RouteProtegee';

/** Adaptateurs dont dépendent les écrans, assemblés par src/infrastructure. */
export interface Dependances {
  clientConnexion: ClientConnexion;
  stockageJeton: StockageJeton;
  decodeurJeton: DecodeurJeton;
}

/** Routes protégées : déclarées ici, leur contenu arrive avec les stories suivantes. */
export const ROUTES_PROTEGEES = ['/tableau-de-bord', '/espace-conseiller', '/administration'] as const;

export function creerRoutes({ clientConnexion, stockageJeton, decodeurJeton }: Dependances): RouteObject[] {
  const accueil = <RedirectionAccueil stockage={stockageJeton} decodeur={decodeurJeton} />;
  return [
    {
      element: <Application />,
      children: [
        { path: ROUTE_CONNEXION, element: <EcranConnexion client={clientConnexion} stockage={stockageJeton} /> },
        {
          element: <RouteProtegee stockage={stockageJeton} decodeur={decodeurJeton} />,
          children: ROUTES_PROTEGEES.map((path) => ({ path })),
        },
        { path: '/', element: accueil },
        { path: '*', element: accueil },
      ],
    },
  ];
}
