import { StrictMode, type ReactElement } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { ClientProfil } from '../application/chargerProfil';
import type { ClientBourse } from '../application/consulterBourse';
import type { ClientProduits } from '../application/consulterProduits';
import type { ClientReleve } from '../application/consulterReleve';
import type { ClientTableauDeBord } from '../application/consulterTableauDeBord';
import type { ClientNotifications } from '../application/gererNotifications';
import type { ClientComptesTiers } from '../application/comptesTiers';
import type { ClientConversation } from '../application/dialoguerAvecSophie';
import type { ClientPrets } from '../application/prets';
import type { ClientSession } from '../application/renouvelerSession';
import type { ClientRevue } from '../application/revue';
import type { ClientConnexion } from '../application/seConnecter';
import type { ClientVirement } from '../application/virement';
import { jetonDeTest } from '../adapters/jeton/jetonDeTest';
import type { Session } from '../adapters/session/session';

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

// Lecture par le système de fichiers (comme architecture-temps.test.ts) : aucun type Node dans tsconfig.
const fs = (
  globalThis as unknown as { process: { getBuiltinModule(id: 'node:fs'): { readFileSync(chemin: URL, encodage: 'utf-8'): string } } }
).process.getBuiltinModule('node:fs');
const lireSource = (chemin: URL) => fs.readFileSync(chemin, 'utf-8');

function installerLocalStorage() {
  const valeurs = new Map<string, string>();
  vi.stubGlobal('localStorage', {
    getItem: (cle: string) => valeurs.get(cle) ?? null,
    setItem: (cle: string, valeur: string) => valeurs.set(cle, valeur),
    removeItem: (cle: string) => valeurs.delete(cle),
  });
  return valeurs;
}

/** En-tête Authorization du dernier appel à `fetch` ; null s'il n'y en a pas. */
function autorisationDuDernierAppel(fetch: { mock: { lastCall?: unknown[] } }): string | null {
  const options = fetch.mock.lastCall?.[1] as RequestInit | undefined;
  return new Headers(options?.headers).get('Authorization');
}

type Route = { path?: string; element: ReactElement<Record<string, unknown>>; children?: Route[] };

const NOMS_CLIENTS_AUTHENTIFIES = [
  'profil',
  'session',
  'bourse',
  'conversation',
  'produits',
  'comptes tiers',
  'prets',
  'tableau de bord',
  'notifications',
  'virement',
  'releve',
  'revue',
];

/** Importe main, puis retrouve dans les routes les clients tels que main.tsx les a câblés. */
async function clientsDeMain() {
  await import('./main');
  const routes = createBrowserRouter.mock.calls[0]?.[0] as Array<{ children: Route[] }>;
  const enfants = routes[0]?.children ?? [];
  const propsDe = (route: Route | undefined) => (route?.element.props ?? {}) as Record<string, unknown>;
  const connexion = (propsDe(enfants.find((route) => route.path === '/login')).children as ReactElement<{ client: ClientConnexion }>).props
    .client;
  const protegee = enfants.find((route) => route.path === undefined) as Route;
  const garde = propsDe(protegee) as { clientProfil: ClientProfil; session: Session };
  const ecran = (chemin: string) => propsDe(protegee.children?.find((route) => route.path === chemin));
  const revue = (ecran('/administration').children as ReactElement[])[1]?.props as { client: ClientRevue };
  const authentifies: ReadonlyArray<readonly [string, () => Promise<unknown>]> = [
    ['profil', () => garde.clientProfil.lire()],
    ['session', () => (garde.session.client as ClientSession).deconnecter()],
    ['bourse', () => (ecran('/bourse').client as ClientBourse).lireWatchlist()],
    ['conversation', () => (ecran('/mon-conseiller').client as ClientConversation).envoyer('Bonjour', [])],
    ['produits', () => (ecran('/produits').client as ClientProduits).lireCatalogue()],
    ['comptes tiers', () => (ecran('/comptes-tiers').client as ClientComptesTiers).lireEtat()],
    ['prets', () => (ecran('/prets').client as ClientPrets).lister()],
    ['tableau de bord', () => (ecran('/tableau-de-bord').client as ClientTableauDeBord).lire()],
    ['notifications', () => (ecran('/tableau-de-bord').clientNotifications as ClientNotifications).lister()],
    ['virement', () => (ecran('/virement').client as ClientVirement).lireComptes()],
    ['releve', () => (ecran('/releve').releve as ClientReleve).lireOperations(1)],
    ['revue', () => revue.client.lireFile()],
  ];
  return { connexion, session: garde.session, authentifies };
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
  }, 30_000);

  it('assemble les vrais clients HTTP : le client de connexion enregistre le jeton dans le localStorage nb.jeton', async () => {
    simulerDocument({});
    const valeurs = installerLocalStorage();
    const jeton = jetonDeTest({ sub: '1', roles: ['CLIENT'] });
    const fetch = vi.fn((_entree: unknown, _options?: unknown) =>
      Promise.resolve(new Response(JSON.stringify({ jeton }), { status: 200 })),
    );
    vi.stubGlobal('fetch', fetch);

    const { connexion } = await clientsDeMain();

    await expect(connexion.connecter('user1', 'secret')).resolves.toEqual({ statut: 'acceptee', roles: ['CLIENT'] });
    expect(fetch).toHaveBeenCalledWith(
      '/api/identite-clients/connexion',
      expect.objectContaining({ method: 'POST', body: '{"identifiant":"user1","motDePasse":"secret"}' }),
    );
    expect(valeurs.get('nb.jeton')).toBe(jeton);
  });

  it('le client de connexion n’envoie jamais le jeton stocké', async () => {
    simulerDocument({});
    installerLocalStorage().set('nb.jeton', 'jeton-deja-la');
    const fetch = vi.fn((_entree: unknown, _options?: unknown) => Promise.resolve(new Response('{}', { status: 401 })));
    vi.stubGlobal('fetch', fetch);

    const { connexion } = await clientsDeMain();

    await expect(connexion.connecter('user1', 'faux')).resolves.toEqual({ statut: 'refusee' });
    expect(autorisationDuDernierAppel(fetch)).toBeNull();
  });

  it('la session de l’application lit les rôles du jeton stocké sans jamais le montrer', async () => {
    simulerDocument({});
    const valeurs = installerLocalStorage();
    vi.stubGlobal('fetch', vi.fn());

    const { session } = await clientsDeMain();

    expect(session.locale.ouverte()).toBe(false);
    valeurs.set('nb.jeton', jetonDeTest({ sub: '1', roles: ['CONSEILLER'] }));
    expect(session.locale.ouverte()).toBe(true);
    expect(session.locale.roles()).toEqual(['CONSEILLER']);
    session.locale.terminer();
    expect(valeurs.has('nb.jeton')).toBe(false);
  });

  it('chaque client authentifié porte le jeton par fetchAuthentifie (Authorization posé par le décorateur seul)', async () => {
    simulerDocument({});
    installerLocalStorage().set('nb.jeton', 'jeton-de-session');
    // 500 : aucune réaction à un 401 (le jeton reste stocké d'un appel à l'autre).
    const fetch = vi.fn((_entree: unknown, _options?: unknown) => Promise.resolve(new Response('{}', { status: 500 })));
    vi.stubGlobal('fetch', fetch);

    const { authentifies } = await clientsDeMain();

    expect(authentifies.map(([nom]) => nom)).toEqual(NOMS_CLIENTS_AUTHENTIFIES);
    for (const [nom, appeler] of authentifies) {
      fetch.mockClear();
      await appeler();
      expect(fetch, `client « ${nom} » : aucun appel`).toHaveBeenCalledOnce();
      expect(autorisationDuDernierAppel(fetch), `client « ${nom} » câblé sans fetchAuthentifie`).toBe('Bearer jeton-de-session');
    }
  });

  it('main.tsx câble fetchAuthentifie sur tous les clients HTTP, sauf celui de la connexion', () => {
    const source = lireSource(new URL('./main.tsx', import.meta.url));
    const appels = [...source.matchAll(/(creerClient\w+Http)\(([^)]*)\)/g)].map(([, nom, arguments_]) => [nom, arguments_] as const);

    expect(appels.length).toBeGreaterThan(10);
    for (const [nom, arguments_] of appels) {
      if (nom === 'creerClientConnexionHttp') {
        expect(arguments_, `${nom} est le seul client sans fetchAuthentifie`).not.toContain('fetchAuthentifie');
      } else {
        expect(arguments_, `${nom} doit recevoir fetchAuthentifie`).toContain('fetchAuthentifie');
      }
    }
  });

  it('lève une erreur si #root est absent', async () => {
    simulerDocument(null);

    await expect(import('./main')).rejects.toThrow('Élément #root introuvable dans index.html');
    expect(createRoot).not.toHaveBeenCalled();
    expect(createBrowserRouter).not.toHaveBeenCalled();
  });
});
