import { describe, expect, it, vi } from 'vitest';
import {
  accueilDeLaSession,
  seConnecter,
  type ClientConnexion,
  type DecodeurJeton,
  type ReponseConnexion,
  type StockageJeton,
} from './seConnecter';

function clientRepondant(reponse: ReponseConnexion) {
  return { connecter: vi.fn<ClientConnexion['connecter']>(() => Promise.resolve(reponse)) };
}

function stockageEnMemoire(initial: string | null = null) {
  let jeton = initial;
  return {
    lire: vi.fn(() => jeton),
    enregistrer: vi.fn((valeur: string) => {
      jeton = valeur;
    }),
    supprimer: vi.fn(() => {
      jeton = null;
    }),
  } satisfies StockageJeton;
}

describe('seConnecter', () => {
  it('[FR-001-03] refuse des champs vides sans appeler l’API', async () => {
    const client = clientRepondant({ statut: 'refusee' });
    const stockage = stockageEnMemoire();

    await expect(seConnecter(' ', '', client, stockage)).resolves.toEqual({
      type: 'champs',
      champs: ['identifiant', 'motDePasse'],
    });
    await expect(seConnecter('user1', '', client, stockage)).resolves.toEqual({ type: 'champs', champs: ['motDePasse'] });
    expect(client.connecter).not.toHaveBeenCalled();
  });

  it('[FR-001-06] rend un refus sans écrire de jeton', async () => {
    const stockage = stockageEnMemoire();

    await expect(seConnecter('user1', 'faux', clientRepondant({ statut: 'refusee' }), stockage)).resolves.toEqual({
      type: 'refus',
    });
    expect(stockage.enregistrer).not.toHaveBeenCalled();
  });

  it('rend une erreur technique quand l’API est en panne, sans écrire de jeton', async () => {
    const stockage = stockageEnMemoire();

    await expect(seConnecter('user1', 'x', clientRepondant({ statut: 'technique' }), stockage)).resolves.toEqual({
      type: 'technique',
    });
    expect(stockage.enregistrer).not.toHaveBeenCalled();
  });

  it('rend une erreur technique quand le client lève une exception', async () => {
    const client: ClientConnexion = { connecter: () => Promise.reject(new Error('réseau')) };

    await expect(seConnecter('user1', 'x', client, stockageEnMemoire())).resolves.toEqual({ type: 'technique' });
  });

  it('rend une erreur technique quand le jeton ne peut pas être enregistré', async () => {
    const stockage = stockageEnMemoire();
    stockage.enregistrer.mockImplementation(() => {
      throw new Error('stockage plein');
    });
    const client = clientRepondant({ statut: 'acceptee', jeton: 'j', roles: ['CLIENT'] });

    await expect(seConnecter('user1', 'x', client, stockage)).resolves.toEqual({ type: 'technique' });
  });

  it('[FR-001-04][FR-001-05][FR-030-03] enregistre le jeton et rend l’accueil du rôle', async () => {
    const cas: ReadonlyArray<readonly [string, string]> = [
      ['ADMIN', '/administration'],
      ['CONSEILLER', '/espace-conseiller'],
      ['CLIENT', '/tableau-de-bord'],
    ];
    for (const [role, route] of cas) {
      const stockage = stockageEnMemoire();
      const client = clientRepondant({ statut: 'acceptee', jeton: `jeton-${role}`, roles: [role] });

      await expect(seConnecter('u', 'p', client, stockage)).resolves.toEqual({ type: 'accepte', route });
      expect(client.connecter).toHaveBeenCalledWith('u', 'p');
      expect(stockage.enregistrer).toHaveBeenCalledWith(`jeton-${role}`);
    }
  });

  it('rend une erreur technique pour un rôle inconnu, sans écrire de jeton', async () => {
    const stockage = stockageEnMemoire();
    const client = clientRepondant({ statut: 'acceptee', jeton: 'j', roles: ['INVITE'] });

    await expect(seConnecter('u', 'p', client, stockage)).resolves.toEqual({ type: 'technique' });
    expect(stockage.enregistrer).not.toHaveBeenCalled();
  });
});

describe('accueilDeLaSession', () => {
  const decodeur = (roles: readonly string[] | null): DecodeurJeton => ({ roles: () => roles });

  it('[FR-001-02][FR-030-01] mène à la connexion sans jeton', () => {
    expect(accueilDeLaSession(stockageEnMemoire(), decodeur(['CLIENT']))).toBe('/login');
  });

  it('[FR-030-03] mène à l’accueil du rôle lu dans le jeton stocké', () => {
    const stockage = stockageEnMemoire('jeton');

    expect(accueilDeLaSession(stockage, decodeur(['CONSEILLER']))).toBe('/espace-conseiller');
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });

  it('supprime un jeton illisible et mène à la connexion', () => {
    const stockage = stockageEnMemoire('illisible');

    expect(accueilDeLaSession(stockage, decodeur(null))).toBe('/login');
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('supprime un jeton sans rôle connu et mène à la connexion', () => {
    const stockage = stockageEnMemoire('jeton');

    expect(accueilDeLaSession(stockage, decodeur(['INVITE']))).toBe('/login');
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });
});
