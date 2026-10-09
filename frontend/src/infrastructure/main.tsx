import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { createBrowserRouter, RouterProvider } from 'react-router';
import { creerClientBudgetHttp } from '../adapters/http/ClientBudgetHttp';
import { creerClientDocumentsHttp } from '../adapters/http/ClientDocumentsHttp';
import { creerClientPlafondsHttp } from '../adapters/http/ClientPlafondsHttp';
import { creerClientPortefeuilleHttp } from '../adapters/http/ClientPortefeuilleHttp';
import { creerClientBourseHttp } from '../adapters/http/ClientBourseHttp';
import { creerClientComptesTiersHttp } from '../adapters/http/ClientComptesTiersHttp';
import { creerClientConnexionHttp } from '../adapters/http/ClientConnexionHttp';
import { creerClientConversationHttp } from '../adapters/http/ClientConversationHttp';
import { creerClientNotificationsHttp } from '../adapters/http/ClientNotificationsHttp';
import { creerClientPipelinePretsHttp } from '../adapters/http/ClientPipelinePretsHttp';
import { creerClientPretsHttp } from '../adapters/http/ClientPretsHttp';
import { creerClientProduitsHttp } from '../adapters/http/ClientProduitsHttp';
import { creerClientProfilHttp } from '../adapters/http/ClientProfilHttp';
import { creerClientRevueHttp } from '../adapters/http/ClientRevueHttp';
import { creerClientReleveHttp } from '../adapters/http/ClientReleveHttp';
import { creerClientBeneficiairesHttp } from '../adapters/http/ClientBeneficiairesHttp';
import { creerClientVirementsProgrammesHttp } from '../adapters/http/ClientVirementsProgrammesHttp';
import { creerClientTableauDeBordHttp } from '../adapters/http/ClientTableauDeBordHttp';
import { creerClientVirementHttp } from '../adapters/http/ClientVirementHttp';
import { creerClientSessionHttp } from '../adapters/http/ClientSessionHttp';
import { decodeurJetonJwt } from '../adapters/jeton/DecodeurJetonJwt';
import { creerRoutes } from '../adapters/routes/routes';
import { creerExpirationJeton } from '../adapters/session/ExpirationJetonJwt';
import { creerSessionLocale } from '../adapters/session/SessionLocaleJwt';
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
    // Seul client non authentifié : il enregistre le jeton reçu, et ne passe donc pas par fetchAuthentifie.
    clientConnexion: creerClientConnexionHttp(stockageJeton, decodeurJetonJwt),
    clientProfil: creerClientProfilHttp(fetchAuthentifie),
    clientBourse: creerClientBourseHttp(fetchAuthentifie),
    clientProduits: creerClientProduitsHttp(fetchAuthentifie),
    clientConversation: creerClientConversationHttp(fetchAuthentifie),
    clientComptesTiers: creerClientComptesTiersHttp(fetchAuthentifie),
    session: {
      client: creerClientSessionHttp(stockageJeton, fetchAuthentifie),
      locale: creerSessionLocale(stockageJeton, decodeurJetonJwt, creerExpirationJeton(horlogeSysteme)),
      signal: signalExpiration,
    },
    clientVirement: creerClientVirementHttp(fetchAuthentifie),
    clientRevue: creerClientRevueHttp(fetchAuthentifie),
    clientPipelinePrets: creerClientPipelinePretsHttp(fetchAuthentifie),
    clientReleve: creerClientReleveHttp(fetchAuthentifie),
    clientBeneficiaires: creerClientBeneficiairesHttp(fetchAuthentifie),
    clientVirementsProgrammes: creerClientVirementsProgrammesHttp(fetchAuthentifie),
    clientTableauDeBord: creerClientTableauDeBordHttp(fetchAuthentifie),
    clientNotifications: creerClientNotificationsHttp(fetchAuthentifie),
    clientPrets: creerClientPretsHttp(fetchAuthentifie),
    clientBudget: creerClientBudgetHttp(fetchAuthentifie),
    clientPlafonds: creerClientPlafondsHttp(fetchAuthentifie),
    clientDocuments: creerClientDocumentsHttp(fetchAuthentifie),
    clientPortefeuille: creerClientPortefeuilleHttp(fetchAuthentifie),
  }),
);

createRoot(racine).render(
  <StrictMode>
    <RouterProvider router={routeur} />
  </StrictMode>,
);
