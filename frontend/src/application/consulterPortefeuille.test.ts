import { describe, expect, it, vi } from 'vitest';
import type { ClientDuPortefeuille, ComptesDuClient, PretsDuClient } from '../domain/portefeuille';
import { chargerPortefeuille, type ClientPortefeuille, type ReponsePortefeuille } from './consulterPortefeuille';
import type { SessionLocale } from './session';

function sessionEnMemoire(ouverte = true) {
  let enCours = ouverte;
  return {
    ouverte: vi.fn(() => enCours),
    roles: vi.fn(() => null),
    terminer: vi.fn(() => {
      enCours = false;
    }),
    secondesRestantes: vi.fn(() => null),
  } satisfies SessionLocale;
}

const JEAN: ClientDuPortefeuille = {
  clientId: 1, nomComplet: 'Jean Dupont', login: 'user1', email: 'jean@example.com', telephone: null, actif: true,
};
const COMPTES: ComptesDuClient = { clientId: 1, comptes: [{ compteId: 10, numero: 'FR76-1', type: 'COURANT', solde: '2130.00' }] };
const PRETS: PretsDuClient = {
  clientId: 1,
  prets: [{ pretId: 5, type: 'AUTO', montant: '5000.00', mensualite: '146.51', statut: 'EN_ATTENTE' }],
};

const ok = <T>(donnees: T): ReponsePortefeuille<T> => ({ statut: 'ok', donnees });
const erreur = (message: string) => ({ statut: 'erreur', message }) as const;
const NON_AUTHENTIFIE = { statut: 'nonAuthentifie' } as const;

function client(
  identites: ReponsePortefeuille<readonly ClientDuPortefeuille[]> = ok([JEAN]),
  comptes: ReponsePortefeuille<readonly ComptesDuClient[]> = ok([COMPTES]),
  prets: ReponsePortefeuille<readonly PretsDuClient[]> = ok([PRETS]),
) {
  return {
    lireIdentites: vi.fn<ClientPortefeuille['lireIdentites']>(() => Promise.resolve(identites)),
    lireComptes: vi.fn<ClientPortefeuille['lireComptes']>(() => Promise.resolve(comptes)),
    lirePrets: vi.fn<ClientPortefeuille['lirePrets']>(() => Promise.resolve(prets)),
  };
}

describe('chargerPortefeuille', () => {
  it('[FR-015-02] joint les trois réponses en un portefeuille, sans message', async () => {
    const service = client();

    const resultat = await chargerPortefeuille(service, sessionEnMemoire());

    expect(resultat).toMatchObject({ type: 'etat', messages: [], portefeuille: { nombreClients: 1, encoursCentimes: 213000, pretsEnAttente: 1 } });
    expect(service.lireIdentites).toHaveBeenCalledWith();
    expect(service.lireComptes).toHaveBeenCalledWith();
    expect(service.lirePrets).toHaveBeenCalledWith();
  });

  it('[FR-015-02] lance les trois appels avant qu’aucun ne soit résolu', async () => {
    const service = client();
    let liberer: () => void = () => {};
    service.lireIdentites.mockImplementation(() => new Promise((resolu) => (liberer = () => resolu(ok([JEAN])))));
    service.lireComptes.mockImplementation(() => new Promise(() => {}));
    service.lirePrets.mockImplementation(() => new Promise(() => {}));

    void chargerPortefeuille(service, sessionEnMemoire());
    await Promise.resolve();

    expect(service.lireIdentites).toHaveBeenCalledOnce();
    expect(service.lireComptes).toHaveBeenCalledOnce();
    expect(service.lirePrets).toHaveBeenCalledOnce();
    liberer();
  });

  it('[FR-015-02] sans session : session terminée, aucun appel', async () => {
    const service = client();
    const session = sessionEnMemoire(false);

    await expect(chargerPortefeuille(service, session)).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
    expect(service.lireIdentites).not.toHaveBeenCalled();
  });

  it.each([
    ['identité', client(NON_AUTHENTIFIE)],
    ['comptes', client(undefined, NON_AUTHENTIFIE)],
    ['prêts', client(undefined, undefined, NON_AUTHENTIFIE)],
  ])('[FR-015-02] un 401 (%s) termine la session', async (_nom, service) => {
    const session = sessionEnMemoire();

    await expect(chargerPortefeuille(service, session)).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it.each([
    ['identité', client(erreur('Service identité indisponible')), ['Service identité indisponible']],
    ['comptes', client(undefined, erreur('Panne comptes')), ['Panne comptes']],
    ['prêts', client(undefined, undefined, erreur('Service crédits indisponible')), ['Service crédits indisponible']],
    ['tous, même message', client(erreur('X'), erreur('X'), erreur('Y')), ['X', 'Y']],
  ])('[FR-015-02] échec (%s) : pas de portefeuille, messages distincts', async (_nom, service, messages) => {
    const session = sessionEnMemoire();

    await expect(chargerPortefeuille(service, session)).resolves.toEqual({ type: 'etat', portefeuille: null, messages });
    expect(session.terminer).not.toHaveBeenCalled();
  });
});
