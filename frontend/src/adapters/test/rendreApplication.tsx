import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { vi } from 'vitest';
import type { ClientSession, ReponseRenouvellement } from '../../application/renouvelerSession';
import type { ClientProfil, ReponseProfil } from '../../application/chargerProfil';
import type {
  ClientPrets,
  ReponseAction,
  ReponseDepot,
  ReponseEcheances,
  ReponsePrets,
  ReponseSimulation,
} from '../../application/prets';
import type { ClientConnexion, ReponseConnexion } from '../../application/seConnecter';
import { decodeurJetonJwt } from '../jeton/DecodeurJetonJwt';
import { creerRoutes } from '../routes/routes';
import { creerExpirationJeton } from '../session/ExpirationJetonJwt';
import { horlogeSysteme, type Horloge } from '../session/horloge';
import { creerSignalExpiration } from '../session/signalExpiration';
import { creerStockageJetonLocal } from '../stockage/StockageJetonLocal';

/**
 * Outil des tests DOM (jsdom) : rend l'application complète sur un chemin, avec un routeur en mémoire,
 * le vrai stockage localStorage, un client de connexion simulé et un client de profil simulé
 * (par défaut, le profil « Jean Dupont »), un client de session simulé (déconnexion sans effet, renouvellement
 * technique), une horloge réelle ou simulée et un client des prêts simulé (par défaut : aucun prêt, simulation et dépôt
 * en panne).
 */
export function rendreApplication(
  chemin: string,
  repondre: ClientConnexion['connecter'] = () => Promise.resolve<ReponseConnexion>({ statut: 'refusee' }),
  lireProfil: ClientProfil['lire'] = () => Promise.resolve<ReponseProfil>({ statut: 'profil', nomComplet: 'Jean Dupont' }),
  horloge: Horloge = horlogeSysteme,
  clientSession: Partial<ClientSession> = {},
  clientPrets: Partial<ClientPrets> = {},
) {
  const deconnecter = vi.fn(clientSession.deconnecter ?? (() => Promise.resolve()));
  const renouveler = vi.fn(
    clientSession.renouveler ?? (() => Promise.resolve<ReponseRenouvellement>({ statut: 'technique' })),
  );
  const signal = creerSignalExpiration();
  const simuler = vi.fn(clientPrets.simuler ?? (() => Promise.resolve<ReponseSimulation>({ statut: 'technique' })));
  const deposer = vi.fn(clientPrets.deposer ?? (() => Promise.resolve<ReponseDepot>({ statut: 'technique' })));
  const lister = vi.fn(clientPrets.lister ?? (() => Promise.resolve<ReponsePrets>({ statut: 'prets', prets: [] })));
  const echeances = vi.fn(clientPrets.echeances ?? (() => Promise.resolve<ReponseEcheances>({ statut: 'technique' })));
  const payer = vi.fn(clientPrets.payer ?? (() => Promise.resolve<ReponseAction>({ statut: 'technique' })));
  const supprimer = vi.fn(clientPrets.supprimer ?? (() => Promise.resolve<ReponseAction>({ statut: 'technique' })));
  const connecter = vi.fn(repondre);
  const lire = vi.fn(lireProfil);
  const routeur = createMemoryRouter(
    creerRoutes({
      clientConnexion: { connecter },
      stockageJeton: creerStockageJetonLocal(),
      decodeurJeton: decodeurJetonJwt,
      clientProfil: { lire },
      session: { client: { deconnecter, renouveler }, expiration: creerExpirationJeton(horloge), signal },
      clientPrets: { simuler, lister, deposer, echeances, payer, supprimer },
    }),
    { initialEntries: [chemin] },
  );
  const rendu = render(<RouterProvider router={routeur} />);
  return { ...rendu, routeur, connecter, lire, deconnecter, renouveler, signal, simuler, lister, deposer, echeances, payer, supprimer };
}
