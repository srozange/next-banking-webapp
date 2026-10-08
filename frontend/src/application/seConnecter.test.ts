import { describe, expect, it, vi } from 'vitest';
import { accueilDeLaSession, seConnecter, type ClientConnexion, type ReponseConnexion } from './seConnecter';
import type { SessionLocale } from './session';

function clientRepondant(reponse: ReponseConnexion) {
  return { connecter: vi.fn<ClientConnexion['connecter']>(() => Promise.resolve(reponse)) };
}

function sessionEnMemoire(ouverte = true, roles: readonly string[] | null = null) {
  let enCours = ouverte;
  return {
    ouverte: vi.fn(() => enCours),
    roles: vi.fn(() => roles),
    terminer: vi.fn(() => {
      enCours = false;
    }),
    secondesRestantes: vi.fn(() => null),
  } satisfies SessionLocale;
}

describe('seConnecter', () => {
  it('[FR-001-03] refuse des champs vides sans appeler l’API', async () => {
    const client = clientRepondant({ statut: 'refusee' });
    await expect(seConnecter(' ', '', client)).resolves.toEqual({
      type: 'champs',
      champs: ['identifiant', 'motDePasse'],
    });
    await expect(seConnecter('user1', '', client)).resolves.toEqual({ type: 'champs', champs: ['motDePasse'] });
    expect(client.connecter).not.toHaveBeenCalled();
  });

  it('[FR-001-06] rend un refus', async () => {
    await expect(seConnecter('user1', 'faux', clientRepondant({ statut: 'refusee' }))).resolves.toEqual({
      type: 'refus',
    });
  });

  it('rend une erreur technique quand l’API est en panne', async () => {
    await expect(seConnecter('user1', 'x', clientRepondant({ statut: 'technique' }))).resolves.toEqual({
      type: 'technique',
    });
  });

  it('rend une erreur technique quand le client lève une exception', async () => {
    const client: ClientConnexion = { connecter: () => Promise.reject(new Error('réseau')) };

    await expect(seConnecter('user1', 'x', client)).resolves.toEqual({ type: 'technique' });
  });

  it('[FR-001-04][FR-001-05][FR-030-03] rend l’accueil du rôle', async () => {
    const cas: ReadonlyArray<readonly [string, string]> = [
      ['ADMIN', '/administration'],
      ['CONSEILLER', '/espace-conseiller'],
      ['CLIENT', '/tableau-de-bord'],
    ];
    for (const [role, route] of cas) {
        const client = clientRepondant({ statut: 'acceptee', roles: [role] });

      await expect(seConnecter('u', 'p', client)).resolves.toEqual({ type: 'accepte', route });
      expect(client.connecter).toHaveBeenCalledWith('u', 'p');
    }
  });

  it('rend une erreur technique pour un rôle inconnu', async () => {
    const client = clientRepondant({ statut: 'acceptee', roles: ['INVITE'] });

    await expect(seConnecter('u', 'p', client)).resolves.toEqual({ type: 'technique' });
  });
});

describe('accueilDeLaSession', () => {
  it('[FR-001-02][FR-030-01] mène à la connexion sans session', () => {
    expect(accueilDeLaSession(sessionEnMemoire(false, null))).toBe('/login');
  });

  it('[FR-030-03] mène à l’accueil du rôle de la session', () => {
    const session = sessionEnMemoire(true, ['CONSEILLER']);

    expect(accueilDeLaSession(session)).toBe('/espace-conseiller');
    expect(session.terminer).not.toHaveBeenCalled();
  });
});
