import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { vi } from 'vitest';
import type { ClientSession, ReponseRenouvellement } from '../../application/renouvelerSession';
import type { ClientProfil, ReponseProfil } from '../../application/chargerProfil';
import type { ClientBourse } from '../../application/consulterBourse';
import type { ClientConversation } from '../../application/dialoguerAvecSophie';
import type { ClientProduits } from '../../application/consulterProduits';
import type { ClientReleve, ReponseOperations } from '../../application/consulterReleve';
import type { ClientVirement, ReponseComptes, ReponseVirement } from '../../application/virement';
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
 * technique), un client bourse et un client produits et un client de conversation simulés (par défaut, panne technique), un client de relevé simulé (par défaut, aucune opération) et une horloge réelle ou simulée.
 */
export function rendreApplication(
  chemin: string,
  repondre: ClientConnexion['connecter'] = () => Promise.resolve<ReponseConnexion>({ statut: 'refusee' }),
  lireProfil: ClientProfil['lire'] = () => Promise.resolve<ReponseProfil>({ statut: 'profil', nomComplet: 'Jean Dupont' }),
  horloge: Horloge = horlogeSysteme,
  clientSession: Partial<ClientSession> = {},
  clientVirement: Partial<ClientVirement> = {},
  clientBourse: Partial<ClientBourse> = {},
  clientProduits: Partial<ClientProduits> = {},
  clientConversation: Partial<ClientConversation> = {},
  clientReleve: Partial<ClientReleve> = {},
) {
  const deconnecter = vi.fn(clientSession.deconnecter ?? (() => Promise.resolve()));
  const renouveler = vi.fn(
    clientSession.renouveler ?? (() => Promise.resolve<ReponseRenouvellement>({ statut: 'technique' })),
  );
  const technique = () => Promise.resolve({ statut: 'technique' as const });
  const bourse: ClientBourse = {
    lireWatchlist: vi.fn(clientBourse.lireWatchlist ?? technique),
    lireHistorique: vi.fn(clientBourse.lireHistorique ?? technique),
    ajouter: vi.fn(clientBourse.ajouter ?? technique),
    retirer: vi.fn(clientBourse.retirer ?? technique),
  };
  const produits: ClientProduits = {
    lireCatalogue: vi.fn(clientProduits.lireCatalogue ?? technique),
    lireTraitements: vi.fn(clientProduits.lireTraitements ?? technique),
  };
  const conversation: ClientConversation = {
    envoyer: vi.fn(clientConversation.envoyer ?? technique),
  };
  const lireComptes = vi.fn(clientVirement.lireComptes ?? (() => Promise.resolve<ReponseComptes>({ statut: 'comptes', comptes: [] })));
  const executer = vi.fn(clientVirement.executer ?? (() => Promise.resolve<ReponseVirement>({ statut: 'erreur', message: null })));
  const lireOperations = vi.fn(clientReleve.lireOperations ?? (() => Promise.resolve<ReponseOperations>({ statut: 'operations', operations: [] })));
  const signal = creerSignalExpiration();
  const connecter = vi.fn(repondre);
  const lire = vi.fn(lireProfil);
  const routeur = createMemoryRouter(
    creerRoutes({
      clientConnexion: { connecter },
      stockageJeton: creerStockageJetonLocal(),
      decodeurJeton: decodeurJetonJwt,
      clientProfil: { lire },
      clientBourse: bourse,
      clientProduits: produits,
      clientConversation: conversation,
      session: { client: { deconnecter, renouveler }, expiration: creerExpirationJeton(horloge), signal },
      clientVirement: { lireComptes, executer },
      clientReleve: { lireOperations },
    }),
    { initialEntries: [chemin] },
  );
  const rendu = render(<RouterProvider router={routeur} />);
  return { ...rendu, routeur, connecter, lire, deconnecter, renouveler, signal, lireComptes, executer, bourse, produits, conversation, lireOperations };
}
