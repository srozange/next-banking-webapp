import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { vi } from 'vitest';
import type { ClientConnexion, ReponseConnexion } from '../../application/seConnecter';
import { decodeurJetonJwt } from '../jeton/DecodeurJetonJwt';
import { creerRoutes } from '../routes/routes';
import { creerStockageJetonLocal } from '../stockage/StockageJetonLocal';

/**
 * Outil des tests DOM (jsdom) : rend l'application complète sur un chemin, avec un routeur en mémoire,
 * le vrai stockage localStorage et un client de connexion simulé.
 */
export function rendreApplication(
  chemin: string,
  repondre: ClientConnexion['connecter'] = () => Promise.resolve<ReponseConnexion>({ statut: 'refusee' }),
) {
  const connecter = vi.fn(repondre);
  const routeur = createMemoryRouter(
    creerRoutes({
      clientConnexion: { connecter },
      stockageJeton: creerStockageJetonLocal(),
      decodeurJeton: decodeurJetonJwt,
    }),
    { initialEntries: [chemin] },
  );
  const rendu = render(<RouterProvider router={routeur} />);
  return { ...rendu, routeur, connecter };
}
