import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { vi } from 'vitest';
import type { ClientSession, ReponseRenouvellement } from '../../application/renouvelerSession';
import type { ClientProfil, ReponseProfil } from '../../application/chargerProfil';
import type { ClientRevue, ReponseDecision, ReponseFile, ReponseHistorique } from '../../application/revue';
import type { ClientVirement, ReponseComptes, ReponseSuivi, ReponseVirement } from '../../application/virement';
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
 * technique) et une horloge réelle ou simulée.
 */
export function rendreApplication(
  chemin: string,
  repondre: ClientConnexion['connecter'] = () => Promise.resolve<ReponseConnexion>({ statut: 'refusee' }),
  lireProfil: ClientProfil['lire'] = () => Promise.resolve<ReponseProfil>({ statut: 'profil', nomComplet: 'Jean Dupont' }),
  horloge: Horloge = horlogeSysteme,
  clientSession: Partial<ClientSession> = {},
  clientVirement: Partial<ClientVirement> = {},
  clientRevue: Partial<ClientRevue> = {},
) {
  const deconnecter = vi.fn(clientSession.deconnecter ?? (() => Promise.resolve()));
  const renouveler = vi.fn(
    clientSession.renouveler ?? (() => Promise.resolve<ReponseRenouvellement>({ statut: 'technique' })),
  );
  const lireComptes = vi.fn(clientVirement.lireComptes ?? (() => Promise.resolve<ReponseComptes>({ statut: 'comptes', comptes: [] })));
  const executer = vi.fn(clientVirement.executer ?? (() => Promise.resolve<ReponseVirement>({ statut: 'erreur', message: null })));
  const lireSuivi = vi.fn(clientVirement.lireSuivi ?? (() => Promise.resolve<ReponseSuivi>({ statut: 'suivi', virements: [] })));
  const lireFile = vi.fn(clientRevue.lireFile ?? (() => Promise.resolve<ReponseFile>({ statut: 'liste', virements: [] })));
  const lireHistorique = vi.fn(clientRevue.lireHistorique ?? (() => Promise.resolve<ReponseHistorique>({ statut: 'liste', virements: [] })));
  const decider = vi.fn(clientRevue.decider ?? (() => Promise.resolve<ReponseDecision>({ statut: 'enregistre' })));
  const signal = creerSignalExpiration();
  const connecter = vi.fn(repondre);
  const lire = vi.fn(lireProfil);
  const routeur = createMemoryRouter(
    creerRoutes({
      clientConnexion: { connecter },
      stockageJeton: creerStockageJetonLocal(),
      decodeurJeton: decodeurJetonJwt,
      clientProfil: { lire },
      session: { client: { deconnecter, renouveler }, expiration: creerExpirationJeton(horloge), signal },
      clientVirement: { lireComptes, executer, lireSuivi },
      clientRevue: { lireFile, lireHistorique, decider },
    }),
    { initialEntries: [chemin] },
  );
  const rendu = render(<RouterProvider router={routeur} />);
  return { ...rendu, routeur, connecter, lire, deconnecter, renouveler, signal, lireComptes, executer, lireSuivi, lireFile, lireHistorique, decider };
}
