import { describe, expect, it, vi } from 'vitest';
import type {
  AlerteFraudeAdministree,
  ClientAdministre,
  IndicateursIdentite,
  IndicateursPrets,
  UtilisateurAdministre,
} from '../domain/administration';
import {
  changerEtatClient,
  chargerAdministration,
  revoirAlerteFraude,
  type ClientAdministration,
  type ReponseAdministration,
} from './consulterAdministration';
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

const IDENTITE: IndicateursIdentite = { totalClients: 4, clientsActifs: 3, conseillers: 2, totalUtilisateurs: 7 };
const PRETS: IndicateursPrets = { pretsEnAttente: 2, montantEnAttente: '15000.00' };
const JEAN: ClientAdministre = {
  clientId: 1,
  nomComplet: 'Jean Dupont',
  login: 'user1',
  email: 'jean.dupont@example.com',
  telephone: null,
  role: 'CLIENT',
  actif: true,
  nomConseiller: 'Sophie Durand',
  dateCreation: '2024-02-01',
};
const ALICE: UtilisateurAdministre = {
  utilisateurId: 10,
  nomComplet: 'Alice Martin',
  login: 'admin',
  email: 'alice.martin@demobank.fr',
  role: 'ADMIN',
  actif: true,
};

const ALERTE: AlerteFraudeAdministree = {
  alerteId: 3,
  clientId: 1,
  nomClient: 'Jean Dupont',
  regle: 'LARGE_AMOUNT',
  score: 45,
  statut: 'OPEN',
  description: 'Montant eleve',
  creeLe: '2026-11-17T08:05:00Z',
  revuPar: null,
  nomRelecteur: null,
};
const CONFIRMEE: AlerteFraudeAdministree = { ...ALERTE, statut: 'CONFIRMED', revuPar: '10', nomRelecteur: 'Alice Martin' };

const ok = <T>(donnees: T): ReponseAdministration<T> => ({ statut: 'ok', donnees });
const NON_AUTHENTIFIE = { statut: 'nonAuthentifie' } as const;

function client() {
  return {
    lireIndicateursIdentite: vi.fn<ClientAdministration['lireIndicateursIdentite']>(() => Promise.resolve(ok(IDENTITE))),
    lireIndicateursPrets: vi.fn<ClientAdministration['lireIndicateursPrets']>(() => Promise.resolve(ok(PRETS))),
    lireClients: vi.fn<ClientAdministration['lireClients']>(() => Promise.resolve(ok([JEAN]))),
    lireUtilisateurs: vi.fn<ClientAdministration['lireUtilisateurs']>(() => Promise.resolve(ok([ALICE]))),
    changerEtatClient: vi.fn<ClientAdministration['changerEtatClient']>(() => Promise.resolve(ok(null))),
    lireAlertesFraude: vi.fn<ClientAdministration['lireAlertesFraude']>(() => Promise.resolve(ok([ALERTE]))),
    revoirAlerte: vi.fn<ClientAdministration['revoirAlerte']>(() => Promise.resolve(ok(null))),
  };
}

describe('chargerAdministration', () => {
  it('[FR-016-02][FR-016-08][FR-016-13][FR-016-14] rend les cinq parties, sans message', async () => {
    const service = client();

    expect(await chargerAdministration(service, sessionEnMemoire())).toEqual({
      type: 'etat',
      indicateursIdentite: { donnees: IDENTITE, message: null },
      indicateursPrets: { donnees: PRETS, message: null },
      clients: { donnees: [JEAN], message: null },
      utilisateurs: { donnees: [ALICE], message: null },
      alertes: { donnees: [ALERTE], message: null },
    });
    expect(service.lireAlertesFraude).toHaveBeenCalledWith();
    expect(service.lireIndicateursIdentite).toHaveBeenCalledWith();
    expect(service.lireIndicateursPrets).toHaveBeenCalledWith();
    expect(service.lireClients).toHaveBeenCalledWith();
    expect(service.lireUtilisateurs).toHaveBeenCalledWith();
  });

  it('[FR-016-02] lance les cinq appels avant qu’aucun ne soit résolu', async () => {
    const service = client();
    const enAttente = () => new Promise<never>(() => {});
    service.lireIndicateursIdentite.mockImplementation(enAttente);
    service.lireIndicateursPrets.mockImplementation(enAttente);
    service.lireClients.mockImplementation(enAttente);
    service.lireUtilisateurs.mockImplementation(enAttente);
    service.lireAlertesFraude.mockImplementation(enAttente);

    void chargerAdministration(service, sessionEnMemoire());
    await Promise.resolve();

    expect(service.lireIndicateursIdentite).toHaveBeenCalledOnce();
    expect(service.lireIndicateursPrets).toHaveBeenCalledOnce();
    expect(service.lireClients).toHaveBeenCalledOnce();
    expect(service.lireUtilisateurs).toHaveBeenCalledOnce();
    expect(service.lireAlertesFraude).toHaveBeenCalledOnce();
  });

  it('[FR-016-14] une panne de la lecture des alertes ne prive que leur partie', async () => {
    const service = client();
    service.lireAlertesFraude.mockResolvedValue({ statut: 'erreur', message: 'X' });

    expect(await chargerAdministration(service, sessionEnMemoire())).toMatchObject({
      type: 'etat',
      clients: { donnees: [JEAN], message: null },
      alertes: { donnees: null, message: 'X' },
    });
  });

  it('[FR-016-14] un 401 de la lecture des alertes termine la session', async () => {
    const service = client();
    service.lireAlertesFraude.mockResolvedValue(NON_AUTHENTIFIE);
    const session = sessionEnMemoire();

    expect(await chargerAdministration(service, session)).toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-016-02] une panne ne prive que sa partie, avec son message', async () => {
    const service = client();
    service.lireIndicateursPrets.mockResolvedValue({ statut: 'erreur', message: 'Service crédits indisponible' });

    const resultat = await chargerAdministration(service, sessionEnMemoire());

    expect(resultat).toMatchObject({
      type: 'etat',
      indicateursIdentite: { donnees: IDENTITE, message: null },
      indicateursPrets: { donnees: null, message: 'Service crédits indisponible' },
      clients: { donnees: [JEAN], message: null },
    });
  });

  it('[FR-016-08] un 401 de l’un des quatre termine la session', async () => {
    const service = client();
    service.lireClients.mockResolvedValue(NON_AUTHENTIFIE);
    const session = sessionEnMemoire();

    expect(await chargerAdministration(service, session)).toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-016-13] sans session ouverte, aucun appel', async () => {
    const service = client();
    const session = sessionEnMemoire(false);

    expect(await chargerAdministration(service, session)).toEqual({ type: 'deconnecte' });
    expect(service.lireIndicateursIdentite).not.toHaveBeenCalled();
    expect(service.lireUtilisateurs).not.toHaveBeenCalled();
  });
});

describe('changerEtatClient', () => {
  it.each([false, true])('[FR-016-11] une réponse ok (actif demandé : %s) est effectuée, avec l’identifiant et l’état transmis', async (actif) => {
    const service = client();
    const session = sessionEnMemoire();

    expect(await changerEtatClient(service, session, 3, actif)).toEqual({ type: 'effectue' });
    expect(service.changerEtatClient).toHaveBeenCalledExactlyOnceWith(3, actif);
    expect(session.terminer).not.toHaveBeenCalled();
  });

  it('[FR-016-11] une erreur rend le message de l’adaptateur tel quel', async () => {
    const service = client();
    service.changerEtatClient.mockResolvedValue({ statut: 'erreur', message: 'Accès refusé' });
    const session = sessionEnMemoire();

    expect(await changerEtatClient(service, session, 3, false)).toEqual({ type: 'erreur', message: 'Accès refusé' });
    expect(session.terminer).not.toHaveBeenCalled();
  });

  it('[FR-016-11] un 401 termine la session', async () => {
    const service = client();
    service.changerEtatClient.mockResolvedValue(NON_AUTHENTIFIE);
    const session = sessionEnMemoire();

    expect(await changerEtatClient(service, session, 3, false)).toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-016-11] sans session ouverte, aucun appel', async () => {
    const service = client();

    expect(await changerEtatClient(service, sessionEnMemoire(false), 3, false)).toEqual({ type: 'deconnecte' });
    expect(service.changerEtatClient).not.toHaveBeenCalled();
  });
});

describe('revoirAlerteFraude', () => {
  it.each(['confirmation', 'faux-positif'] as const)('[FR-016-15][FR-027-12] une revue « %s » acceptée relit toutes les alertes', async (issue) => {
    const service = client();
    service.lireAlertesFraude.mockResolvedValue(ok([CONFIRMEE]));
    const session = sessionEnMemoire();

    expect(await revoirAlerteFraude(service, session, 3, issue)).toEqual({ type: 'effectue', alertes: { donnees: [CONFIRMEE], message: null } });
    expect(service.revoirAlerte).toHaveBeenCalledExactlyOnceWith(3, issue);
    expect(service.lireAlertesFraude).toHaveBeenCalledOnce();
    expect(session.terminer).not.toHaveBeenCalled();
  });

  it('[FR-016-15] une relecture en panne rend son message dans la partie des alertes', async () => {
    const service = client();
    service.lireAlertesFraude.mockResolvedValue({ statut: 'erreur', message: 'X' });

    expect(await revoirAlerteFraude(service, sessionEnMemoire(), 3, 'confirmation')).toEqual({
      type: 'effectue',
      alertes: { donnees: null, message: 'X' },
    });
  });

  it('[FR-016-15] une revue en erreur rend le message tel quel, sans relecture', async () => {
    const service = client();
    service.revoirAlerte.mockResolvedValue({ statut: 'erreur', message: 'Accès refusé' });

    expect(await revoirAlerteFraude(service, sessionEnMemoire(), 3, 'faux-positif')).toEqual({ type: 'erreur', message: 'Accès refusé' });
    expect(service.lireAlertesFraude).not.toHaveBeenCalled();
  });

  it('[FR-016-15] un 401 de la revue ou de la relecture termine la session', async () => {
    const revue = client();
    revue.revoirAlerte.mockResolvedValue(NON_AUTHENTIFIE);
    const premiere = sessionEnMemoire();
    expect(await revoirAlerteFraude(revue, premiere, 3, 'confirmation')).toEqual({ type: 'deconnecte' });
    expect(premiere.terminer).toHaveBeenCalledOnce();

    const relecture = client();
    relecture.lireAlertesFraude.mockResolvedValue(NON_AUTHENTIFIE);
    const seconde = sessionEnMemoire();
    expect(await revoirAlerteFraude(relecture, seconde, 3, 'confirmation')).toEqual({ type: 'deconnecte' });
    expect(seconde.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-016-15] sans session ouverte, aucun appel', async () => {
    const service = client();

    expect(await revoirAlerteFraude(service, sessionEnMemoire(false), 3, 'confirmation')).toEqual({ type: 'deconnecte' });
    expect(service.revoirAlerte).not.toHaveBeenCalled();
  });
});
