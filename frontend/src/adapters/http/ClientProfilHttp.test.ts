import { describe, expect, it, vi } from 'vitest';
import { CHEMIN_PROFIL, creerClientProfilHttp } from './ClientProfilHttp';

function repondre(statut: number, corps: string) {
  return vi.fn<typeof fetch>(() => Promise.resolve(new Response(corps, { status: statut })));
}

describe('ClientProfilHttp', () => {
  it('[FR-001-05] lit /moi en GET avec le jeton en Bearer et rend le nom complet', async () => {
    const appeler = repondre(200, '{"identifiant":"1","nomComplet":"Jean Dupont","roles":["CLIENT"]}');

    await expect(creerClientProfilHttp(appeler).lire('jeton')).resolves.toEqual({
      statut: 'profil',
      nomComplet: 'Jean Dupont',
    });
    expect(appeler).toHaveBeenCalledWith(CHEMIN_PROFIL, {
      method: 'GET',
      headers: { Authorization: 'Bearer jeton', Accept: 'application/json' },
    });
    expect(CHEMIN_PROFIL).toBe('/api/identite-clients/moi');
  });

  it('[FR-001-05] rend non authentifié sur 401', async () => {
    await expect(creerClientProfilHttp(repondre(401, '{"message":"Authentification requise"}')).lire('j')).resolves.toEqual({
      statut: 'nonAuthentifie',
    });
  });

  it.each([
    ['500', repondre(500, '{"message":"Une erreur technique est survenue"}')],
    ['403', repondre(403, '{"message":"Accès refusé"}')],
    ['200 au JSON illisible', repondre(200, 'pas du json')],
    ['200 sans nomComplet', repondre(200, '{}')],
    ['200 au nomComplet non textuel', repondre(200, '{"nomComplet":3}')],
    ['200 au corps null', repondre(200, 'null')],
    ['réseau coupé', vi.fn<typeof fetch>(() => Promise.reject(new TypeError('Failed to fetch')))],
  ])('[FR-001-05] rend une erreur technique : %s', async (_cas, appeler) => {
    await expect(creerClientProfilHttp(appeler).lire('j')).resolves.toEqual({ statut: 'technique' });
  });

  it('appelle fetch global par défaut', async () => {
    const appeler = repondre(401, '{}');
    vi.stubGlobal('fetch', appeler);
    try {
      await expect(creerClientProfilHttp().lire('j')).resolves.toEqual({ statut: 'nonAuthentifie' });
      expect(appeler).toHaveBeenCalledOnce();
    } finally {
      vi.unstubAllGlobals();
    }
  });
});
