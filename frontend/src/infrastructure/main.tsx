import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { creerClientConnexionHttp } from '../adapters/http/ClientConnexionHttp';
import { creerClientProfilHttp } from '../adapters/http/ClientProfilHttp';
import { creerClientSessionHttp } from '../adapters/http/ClientSessionHttp';
import { decodeurJetonJwt } from '../adapters/jeton/DecodeurJetonJwt';
import { creerRoutes } from '../adapters/routes/routes';
import { creerExpirationJeton } from '../adapters/session/ExpirationJetonJwt';
import { creerFetchAuthentifie } from '../adapters/session/fetchAuthentifie';
import { horlogeSysteme } from '../adapters/session/horloge';
import { creerSignalExpiration } from '../adapters/session/signalExpiration';
import { creerStockageJetonLocal } from '../adapters/stockage/StockageJetonLocal';
import '../adapters/ui/theme/tokens.css';
import '../adapters/ui/theme/base.css';

const racine = document.getElementById('root');
if (racine === null) {
  throw new Error('Élément #root introuvable dans index.html');
}

const stockageJeton = creerStockageJetonLocal();
const signalExpiration = creerSignalExpiration();
// Appels authentifiés : tout 401 de tout service supprime le jeton et ramène à /login.
const fetchAuthentifie = creerFetchAuthentifie(stockageJeton, signalExpiration);

const routeur = createBrowserRouter(
  creerRoutes({
    clientConnexion: creerClientConnexionHttp(decodeurJetonJwt),
    stockageJeton,
    decodeurJeton: decodeurJetonJwt,
    clientProfil: creerClientProfilHttp(fetchAuthentifie),
    session: {
      client: creerClientSessionHttp(fetchAuthentifie),
      expiration: creerExpirationJeton(horlogeSysteme),
      signal: signalExpiration,
    },
  }),
);

createRoot(racine).render(
  <StrictMode>
    <RouterProvider router={routeur} />
  </StrictMode>,
);
