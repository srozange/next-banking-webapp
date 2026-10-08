import { describe, expect, it, vi } from 'vitest';
import { chargerProfil, type ClientProfil, type ReponseProfil } from './chargerProfil';
import type { SessionLocale } from './session';

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

function clientRepondant(reponse: ReponseProfil) {
  return { lire: vi.fn<ClientProfil['lire']>(() => Promise.resolve(reponse)) };
}

describe('chargerProfil', () => {
  it('[FR-001-05] rend le nom complet de la session', async () => {
    const client = clientRepondant({ statut: 'profil', nomComplet: 'Jean Dupont' });
    const session = sessionEnMemoire();

    await expect(chargerProfil(client, session)).resolves.toEqual({ type: 'profil', nomComplet: 'Jean Dupont' });
    expect(client.lire).toHaveBeenCalledWith();
    expect(session.terminer).not.toHaveBeenCalled();
  });

  it('[FR-001-05] sans session : session terminée, sans appel', async () => {
    const client = clientRepondant({ statut: 'profil', nomComplet: 'Jean Dupont' });
    const session = sessionEnMemoire(false);

    await expect(chargerProfil(client, session)).resolves.toEqual({ type: 'deconnecte' });
    expect(client.lire).not.toHaveBeenCalled();
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-001-05] 401 : session terminée', async () => {
    const session = sessionEnMemoire();

    await expect(chargerProfil(clientRepondant({ statut: 'nonAuthentifie' }), session)).resolves.toEqual({
      type: 'deconnecte',
    });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-001-05] panne : profil indisponible, session conservée', async () => {
    const session = sessionEnMemoire();

    await expect(chargerProfil(clientRepondant({ statut: 'technique' }), session)).resolves.toEqual({
      type: 'indisponible',
    });
    const enEchec: ClientProfil = { lire: () => Promise.reject(new Error('réseau')) };
    await expect(chargerProfil(enEchec, session)).resolves.toEqual({ type: 'indisponible' });
    expect(session.terminer).not.toHaveBeenCalled();
  });
});
