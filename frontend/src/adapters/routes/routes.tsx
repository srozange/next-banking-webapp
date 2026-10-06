import type { RouteObject } from 'react-router';
import { ROUTE_CONNEXION } from '../../domain/connexion';
import type { ClientProfil } from '../../application/chargerProfil';
import type { ClientBourse } from '../../application/consulterBourse';
import type { ClientProduits } from '../../application/consulterProduits';
import type { ClientConnexion, DecodeurJeton, StockageJeton } from '../../application/seConnecter';
import { Application } from '../Application';
import type { Session } from '../session/session';
import { EcranBourse } from '../ecrans/bourse/EcranBourse';
import { EcranConnexion } from '../ecrans/connexion/EcranConnexion';
import { EcranProduits } from '../ecrans/produits/EcranProduits';
import { TEXTES } from '../textes';
import { ContenuPrincipal } from '../ui/ContenuPrincipal';
import { RedirectionAccueil } from './RedirectionAccueil';
import { RouteProtegee } from './RouteProtegee';

/** Adaptateurs dont dépendent les écrans, assemblés par src/infrastructure. */
export interface Dependances {
  clientConnexion: ClientConnexion;
  stockageJeton: StockageJeton;
  decodeurJeton: DecodeurJeton;
  clientProfil: ClientProfil;
  clientBourse: ClientBourse;
  clientProduits: ClientProduits;
  session: Session;
}

/** Routes protégées sans écran pour l'instant (un titre d'onglet seul) : leur contenu arrive avec les stories suivantes. */
export const ROUTES_PROTEGEES = ['/tableau-de-bord', '/espace-conseiller', '/administration'] as const;

/** Titre d'onglet de chaque route protégée (seul élément rendu en attendant le contenu, hissé dans <head>). */
const TITRES_ONGLET: Readonly<Record<(typeof ROUTES_PROTEGEES)[number], string>> = {
  '/tableau-de-bord': TEXTES.titresOnglet.tableauDeBord,
  '/espace-conseiller': TEXTES.titresOnglet.espaceConseiller,
  '/administration': TEXTES.titresOnglet.administration,
};

export function creerRoutes({ clientConnexion, stockageJeton, decodeurJeton, clientProfil, clientBourse, clientProduits, session }: Dependances): RouteObject[] {
  const accueil = <RedirectionAccueil stockage={stockageJeton} decodeur={decodeurJeton} />;
  return [
    {
      element: <Application />,
      children: [
        {
          path: ROUTE_CONNEXION,
          element: (
            <ContenuPrincipal>
              <EcranConnexion client={clientConnexion} stockage={stockageJeton} />
            </ContenuPrincipal>
          ),
        },
        {
          // Garde par session puis par rôle, puis gabarit authentifié (en-tête, contenu principal).
          element: <RouteProtegee stockage={stockageJeton} decodeur={decodeurJeton} clientProfil={clientProfil} session={session} />,
          children: [
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-009-01).
            { path: '/bourse', element: <EcranBourse client={clientBourse} stockage={stockageJeton} /> },
            // Même écran pour CLIENT, CONSEILLER et ADMIN (FR-010-01, FR-011-01).
            { path: '/produits', element: <EcranProduits client={clientProduits} stockage={stockageJeton} /> },
            ...ROUTES_PROTEGEES.map((path) => ({ path, element: <title>{TITRES_ONGLET[path]}</title> })),
          ],
        },
        { path: '/', element: accueil },
        { path: '*', element: accueil },
      ],
    },
  ];
}
