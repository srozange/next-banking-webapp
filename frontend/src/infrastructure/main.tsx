import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { creerClientConnexionHttp } from '../adapters/http/ClientConnexionHttp';
import { creerClientProfilHttp } from '../adapters/http/ClientProfilHttp';
import { decodeurJetonJwt } from '../adapters/jeton/DecodeurJetonJwt';
import { creerRoutes } from '../adapters/routes/routes';
import { creerStockageJetonLocal } from '../adapters/stockage/StockageJetonLocal';
import '../adapters/ui/theme/tokens.css';
import '../adapters/ui/theme/base.css';

const racine = document.getElementById('root');
if (racine === null) {
  throw new Error('Élément #root introuvable dans index.html');
}

const routeur = createBrowserRouter(
  creerRoutes({
    clientConnexion: creerClientConnexionHttp(decodeurJetonJwt),
    stockageJeton: creerStockageJetonLocal(),
    decodeurJeton: decodeurJetonJwt,
    clientProfil: creerClientProfilHttp(),
  }),
);

createRoot(racine).render(
  <StrictMode>
    <RouterProvider router={routeur} />
  </StrictMode>,
);
