import { describe, expect, it, vi } from 'vitest';
import { chargerProfil, rolesDeLaSession, type ClientProfil, type ReponseProfil } from './chargerProfil';
import type { DecodeurJeton, StockageJeton } from './seConnecter';

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

function clientRepondant(reponse: ReponseProfil) {
  return { lire: vi.fn<ClientProfil['lire']>(() => Promise.resolve(reponse)) };
}

describe('chargerProfil', () => {
  it('[FR-001-05] rend le nom complet lu avec le jeton de la session', async () => {
    const client = clientRepondant({ statut: 'profil', nomComplet: 'Jean Dupont' });
    const stockage = stockageEnMemoire('jeton');

    await expect(chargerProfil(client, stockage)).resolves.toEqual({ type: 'profil', nomComplet: 'Jean Dupont' });
    expect(client.lire).toHaveBeenCalledWith('jeton');
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });

  it('[FR-001-05] sans jeton : session terminée, sans appel', async () => {
    const client = clientRepondant({ statut: 'profil', nomComplet: 'Jean Dupont' });
    const stockage = stockageEnMemoire();

    await expect(chargerProfil(client, stockage)).resolves.toEqual({ type: 'deconnecte' });
    expect(client.lire).not.toHaveBeenCalled();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-001-05] 401 : jeton supprimé, session terminée', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(chargerProfil(clientRepondant({ statut: 'nonAuthentifie' }), stockage)).resolves.toEqual({
      type: 'deconnecte',
    });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-001-05] panne : profil indisponible, jeton conservé', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(chargerProfil(clientRepondant({ statut: 'technique' }), stockage)).resolves.toEqual({
      type: 'indisponible',
    });
    const enEchec: ClientProfil = { lire: () => Promise.reject(new Error('réseau')) };
    await expect(chargerProfil(enEchec, stockage)).resolves.toEqual({ type: 'indisponible' });
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });
});

describe('rolesDeLaSession', () => {
  const decodeur = (roles: readonly string[] | null): DecodeurJeton => ({ roles: vi.fn(() => roles) });

  it('[FR-030-02] rend les rôles du jeton stocké', () => {
    const stockage = stockageEnMemoire('jeton');
    const lecteur = decodeur(['CONSEILLER']);

    expect(rolesDeLaSession(stockage, lecteur)).toEqual(['CONSEILLER']);
    expect(lecteur.roles).toHaveBeenCalledWith('jeton');
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });

  it('[FR-030-02] rend null sans jeton, sans rien supprimer', () => {
    const stockage = stockageEnMemoire();

    expect(rolesDeLaSession(stockage, decodeur(['CLIENT']))).toBeNull();
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });

  it.each([
    ['illisible', null],
    ['sans rôle connu', ['INVITE']],
  ])('[FR-030-02] supprime un jeton %s et rend null', (_cas, roles) => {
    const stockage = stockageEnMemoire('jeton');

    expect(rolesDeLaSession(stockage, decodeur(roles))).toBeNull();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });
});
