import { describe, expect, it, vi } from 'vitest';
import type { Echeance, Pret, Simulation } from '../domain/pret';
import {
  consulterEcheances,
  deposerDemande,
  listerPrets,
  payerEcheance,
  simulerEmprunt,
  supprimerPret,
  type ClientPrets,
  type ReponseAction,
  type ReponseDepot,
  type ReponseEcheances,
  type ReponsePrets,
  type ReponseSimulation,
} from './prets';
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

const SAISIE = { montant: '10000', tauxAnnuel: '3.50', dureeMois: '36' };
const SIMULATION: Simulation = { mensualite: '293.02', coutTotal: '10548.72', totalInterets: '548.72', echeancier: [] };
const PRET: Pret = {
  id: 1,
  type: 'TRAVAUX',
  montant: '5000.00',
  tauxAnnuel: '4.9000',
  dureeMois: 24,
  mensualite: '218.63',
  coutTotal: '5247.12',
  totalInterets: '247.12',
  objet: null,
  statut: 'EN_COURS',
  dateDebut: '2026-08-01',
  dateFin: '2028-08-01',
};

function client(
  simulation: ReponseSimulation | Error,
  prets: ReponsePrets | Error = { statut: 'prets', prets: [] },
  depot: ReponseDepot | Error = { statut: 'technique' },
  echeances: ReponseEcheances | Error = { statut: 'echeances', echeances: [] },
  action: ReponseAction | Error = { statut: 'faite' },
) {
  const repondre = <T>(reponse: T | Error) => (reponse instanceof Error ? Promise.reject(reponse) : Promise.resolve(reponse));
  return {
    simuler: vi.fn<ClientPrets['simuler']>(() => repondre(simulation)),
    lister: vi.fn<ClientPrets['lister']>(() => repondre(prets)),
    deposer: vi.fn<ClientPrets['deposer']>(() => repondre(depot)),
    echeances: vi.fn<ClientPrets['echeances']>(() => repondre(echeances)),
    payer: vi.fn<ClientPrets['payer']>(() => repondre(action)),
    supprimer: vi.fn<ClientPrets['supprimer']>(() => repondre(action)),
  };
}

describe('simulerEmprunt', () => {
  it('[FR-007-03] rend la simulation du service, appelée avec la saisie', async () => {
    const c = client({ statut: 'simulation', simulation: SIMULATION });

    await expect(simulerEmprunt(c, sessionEnMemoire(), SAISIE)).resolves.toEqual({
      type: 'simulation',
      simulation: SIMULATION,
    });
    expect(c.simuler).toHaveBeenCalledWith(SAISIE);
  });

  it('[FR-007-02] rend le message du service sur un refus 400', async () => {
    const c = client({ statut: 'refusee', message: 'Taux invalide (0-30%)' });

    await expect(simulerEmprunt(c, sessionEnMemoire(), SAISIE)).resolves.toEqual({
      type: 'refus',
      message: 'Taux invalide (0-30%)',
    });
  });

  it('[FR-007-01] sans session : session terminée, sans appel', async () => {
    const c = client({ statut: 'simulation', simulation: SIMULATION });
    const session = sessionEnMemoire(false);

    await expect(simulerEmprunt(c, session, SAISIE)).resolves.toEqual({ type: 'deconnecte' });
    expect(c.simuler).not.toHaveBeenCalled();
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-007-01] sur 401 : session terminée', async () => {
    const session = sessionEnMemoire();

    await expect(simulerEmprunt(client({ statut: 'nonAuthentifie' }), session, SAISIE)).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-007-03] sur une panne du service ou du réseau : indisponible, session conservée', async () => {
    const session = sessionEnMemoire();

    await expect(simulerEmprunt(client({ statut: 'technique' }), session, SAISIE)).resolves.toEqual({ type: 'indisponible' });
    await expect(simulerEmprunt(client(new Error('réseau')), session, SAISIE)).resolves.toEqual({ type: 'indisponible' });
    expect(session.terminer).not.toHaveBeenCalled();
  });
});

describe('listerPrets', () => {
  it('[FR-007-08] rend les prêts lus dans la session', async () => {
    const c = client({ statut: 'technique' }, { statut: 'prets', prets: [PRET] });

    await expect(listerPrets(c, sessionEnMemoire())).resolves.toEqual({ type: 'prets', prets: [PRET] });
    expect(c.lister).toHaveBeenCalledWith();
  });

  it('[FR-007-01] sans session : session terminée, sans appel', async () => {
    const c = client({ statut: 'technique' });
    const session = sessionEnMemoire(false);

    await expect(listerPrets(c, session)).resolves.toEqual({ type: 'deconnecte' });
    expect(c.lister).not.toHaveBeenCalled();
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-007-01] sur 401 : session terminée', async () => {
    const session = sessionEnMemoire();

    await expect(
      listerPrets(client({ statut: 'technique' }, { statut: 'nonAuthentifie' }), session),
    ).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-007-08] sur une panne du service ou du réseau : indisponible', async () => {
    await expect(
      listerPrets(client({ statut: 'technique' }, { statut: 'technique' }), sessionEnMemoire()),
    ).resolves.toEqual({ type: 'indisponible' });
    await expect(
      listerPrets(client({ statut: 'technique' }, new Error('réseau')), sessionEnMemoire()),
    ).resolves.toEqual({ type: 'indisponible' });
  });
});

describe('deposerDemande', () => {
  const DEMANDE = { type: 'CONSOMMATION', montant: '10000', tauxAnnuel: '3.50', dureeMois: '300', objet: 'Voiture' };
  const sansDepot = { statut: 'technique' } as const;

  it('[FR-007-07] rend le prêt déposé, appelé avec la saisie', async () => {
    const c = client(sansDepot, undefined, { statut: 'deposee', pret: PRET });

    await expect(deposerDemande(c, sessionEnMemoire(), DEMANDE)).resolves.toEqual({ type: 'deposee', pret: PRET });
    expect(c.deposer).toHaveBeenCalledWith(DEMANDE);
  });

  it('[FR-007-05] rend le message du service sur un refus', async () => {
    const c = client(sansDepot, undefined, { statut: 'refusee', message: 'Duree invalide' });

    await expect(deposerDemande(c, sessionEnMemoire(), DEMANDE)).resolves.toEqual({
      type: 'refus',
      message: 'Duree invalide',
    });
  });

  it('[FR-007-01] sans session : session terminée, sans appel', async () => {
    const c = client(sansDepot);
    const session = sessionEnMemoire(false);

    await expect(deposerDemande(c, session, DEMANDE)).resolves.toEqual({ type: 'deconnecte' });
    expect(c.deposer).not.toHaveBeenCalled();
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-007-01] sur 401 : session terminée', async () => {
    const session = sessionEnMemoire();

    await expect(
      deposerDemande(client(sansDepot, undefined, { statut: 'nonAuthentifie' }), session, DEMANDE),
    ).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-024-02] sur une panne du service ou du réseau : indisponible, session conservée', async () => {
    const session = sessionEnMemoire();

    await expect(deposerDemande(client(sansDepot), session, DEMANDE)).resolves.toEqual({ type: 'indisponible' });
    await expect(deposerDemande(client(sansDepot, undefined, new Error('réseau')), session, DEMANDE)).resolves.toEqual({
      type: 'indisponible',
    });
    expect(session.terminer).not.toHaveBeenCalled();
  });
});

const ECHEANCE: Echeance = {
  id: 5,
  numero: 5,
  dateEcheance: '2026-11-01',
  capital: '414.09',
  interets: '13.72',
  total: '427.81',
  capitalRestant: '2946.33',
  paye: false,
  datePaiement: null,
};
const sansEcheances = { statut: 'technique' } as const;

describe('consulterEcheances', () => {
  it('[FR-007-11] rend les échéances du prêt, appelées avec l’identifiant', async () => {
    const c = client(sansEcheances, undefined, undefined, { statut: 'echeances', echeances: [ECHEANCE] });

    await expect(consulterEcheances(c, sessionEnMemoire(), 1)).resolves.toEqual({
      type: 'echeances',
      echeances: [ECHEANCE],
    });
    expect(c.echeances).toHaveBeenCalledWith(1);
  });

  it('[FR-007-11] sans session : session terminée, sans appel', async () => {
    const c = client(sansEcheances);
    const session = sessionEnMemoire(false);

    await expect(consulterEcheances(c, session, 1)).resolves.toEqual({ type: 'deconnecte' });
    expect(c.echeances).not.toHaveBeenCalled();
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-007-11] sur 401 : session terminée', async () => {
    const session = sessionEnMemoire();

    await expect(
      consulterEcheances(client(sansEcheances, undefined, undefined, { statut: 'nonAuthentifie' }), session, 1),
    ).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it('[FR-007-11] sur une panne du service ou du réseau : indisponible, session conservée', async () => {
    const session = sessionEnMemoire();

    await expect(
      consulterEcheances(client(sansEcheances, undefined, undefined, { statut: 'technique' }), session, 1),
    ).resolves.toEqual({ type: 'indisponible' });
    await expect(
      consulterEcheances(client(sansEcheances, undefined, undefined, new Error('réseau')), session, 1),
    ).resolves.toEqual({ type: 'indisponible' });
    expect(session.terminer).not.toHaveBeenCalled();
  });
});

describe.each([
  ['FR-007-12', 'payerEcheance', payerEcheance, 'payer'],
  ['FR-007-13', 'supprimerPret', supprimerPret, 'supprimer'],
] as const)('[%s] %s', (etiquette, _nom, cas, methode) => {
  it(`[${etiquette}] réussit avec l’identifiant`, async () => {
    const c = client(sansEcheances);

    await expect(cas(c, sessionEnMemoire(), 7)).resolves.toEqual({ type: 'faite' });
    expect(c[methode]).toHaveBeenCalledWith(7);
  });

  it(`[${etiquette}] sans session : session terminée, sans appel`, async () => {
    const c = client(sansEcheances);
    const session = sessionEnMemoire(false);

    await expect(cas(c, session, 7)).resolves.toEqual({ type: 'deconnecte' });
    expect(c[methode]).not.toHaveBeenCalled();
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it(`[${etiquette}] sur 401 : session terminée`, async () => {
    const session = sessionEnMemoire();
    const c = client(sansEcheances, undefined, undefined, undefined, { statut: 'nonAuthentifie' });

    await expect(cas(c, session, 7)).resolves.toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalledOnce();
  });

  it(`[${etiquette}] sur toute panne : indisponible, session conservée`, async () => {
    const session = sessionEnMemoire();

    await expect(
      cas(client(sansEcheances, undefined, undefined, undefined, { statut: 'technique' }), session, 7),
    ).resolves.toEqual({ type: 'indisponible' });
    await expect(
      cas(client(sansEcheances, undefined, undefined, undefined, new Error('réseau')), session, 7),
    ).resolves.toEqual({ type: 'indisponible' });
    expect(session.terminer).not.toHaveBeenCalled();
  });
});
