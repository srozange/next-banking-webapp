import { StrictMode, type ReactElement } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ClientConnexion, DecodeurJeton, StockageJeton } from '../application/seConnecter';

const { render, createRoot, createBrowserRouter, routeur } = vi.hoisted(() => {
  const render = vi.fn((_element: unknown) => {});
  const createRoot = vi.fn(() => ({ render }));
  const routeur = { factice: true };
  const createBrowserRouter = vi.fn((_routes: unknown) => routeur);
  return { render, createRoot, createBrowserRouter, routeur };
});

vi.mock('react-dom/client', () => ({ createRoot }));
vi.mock('react-router', async (original) => ({ ...(await original<typeof import('react-router')>()), createBrowserRouter }));

function simulerDocument(racine: object | null) {
  const getElementById = vi.fn(() => racine);
  vi.stubGlobal('document', { getElementById });
  return getElementById;
}

describe('main', () => {
  beforeEach(() => {
    vi.resetModules();
    render.mockClear();
    createRoot.mockClear();
    createBrowserRouter.mockClear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('rend le routeur de l’application dans #root', async () => {
    const racine = {};
    const getElementById = simulerDocument(racine);

    await import('./main');
    // Après vi.resetModules() : la même instance de module que celle importée par main.
    const { RouterProvider } = await import('react-router');
    const { Application } = await import('../adapters/Application');

    expect(getElementById).toHaveBeenCalledWith('root');
    expect(createRoot).toHaveBeenCalledWith(racine);
    expect(createBrowserRouter).toHaveBeenCalledOnce();
    const routes = createBrowserRouter.mock.calls[0]?.[0] as Array<{ element: ReactElement; children: unknown[] }>;
    expect(routes[0]?.element.type).toBe(Application);
    expect(render).toHaveBeenCalledOnce();
    const element = render.mock.calls[0]?.[0] as ReactElement<{ children: ReactElement<{ router: unknown }> }>;
    expect(element.type).toBe(StrictMode);
    expect(element.props.children.type).toBe(RouterProvider);
    expect(element.props.children.props.router).toBe(routeur);
  });

  it('assemble le vrai client HTTP, le stockage localStorage nb.jeton et le décodeur JWT', async () => {
    simulerDocument({});
    const valeurs = new Map<string, string>();
    vi.stubGlobal('localStorage', {
      getItem: (cle: string) => valeurs.get(cle) ?? null,
      setItem: (cle: string, valeur: string) => valeurs.set(cle, valeur),
      removeItem: (cle: string) => valeurs.delete(cle),
    });
    const fetch = vi.fn(() => Promise.resolve(new Response('{"message":"Identifiants incorrects"}', { status: 401 })));
    vi.stubGlobal('fetch', fetch);

    await import('./main');
    const routes = createBrowserRouter.mock.calls[0]?.[0] as Array<{
      children: Array<{ path?: string; element: ReactElement<Record<string, unknown>> }>;
    }>;
    const enfants = routes[0]?.children ?? [];
    const connexion = enfants.find((route) => route.path === '/login')?.element.props as {
      client: ClientConnexion;
      stockage: StockageJeton;
    };
    const protegee = enfants.find((route) => route.path === undefined)?.element.props as { decodeur: DecodeurJeton };

    connexion.stockage.enregistrer('jeton');
    expect(valeurs.get('nb.jeton')).toBe('jeton');
    expect(connexion.stockage.lire()).toBe('jeton');

    await expect(connexion.client.connecter('user1', 'faux')).resolves.toEqual({ statut: 'refusee' });
    expect(fetch).toHaveBeenCalledWith(
      '/api/identite-clients/connexion',
      expect.objectContaining({ method: 'POST', body: '{"identifiant":"user1","motDePasse":"faux"}' }),
    );

    const charge = btoa(JSON.stringify({ roles: ['CLIENT'] }));
    expect(protegee.decodeur.roles(`entete.${charge}.signature`)).toEqual(['CLIENT']);
  });

  it('lève une erreur si #root est absent', async () => {
    simulerDocument(null);

    await expect(import('./main')).rejects.toThrow('Élément #root introuvable dans index.html');
    expect(createRoot).not.toHaveBeenCalled();
    expect(createBrowserRouter).not.toHaveBeenCalled();
  });
});
