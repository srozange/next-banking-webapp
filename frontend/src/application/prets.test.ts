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
import type { StockageJeton } from './seConnecter';

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
  it('[FR-007-03] rend la simulation du service, appelée avec le jeton et la saisie', async () => {
    const c = client({ statut: 'simulation', simulation: SIMULATION });

    await expect(simulerEmprunt(c, stockageEnMemoire('jeton'), SAISIE)).resolves.toEqual({
      type: 'simulation',
      simulation: SIMULATION,
    });
    expect(c.simuler).toHaveBeenCalledWith('jeton', SAISIE);
  });

  it('[FR-007-02] rend le message du service sur un refus 400', async () => {
    const c = client({ statut: 'refusee', message: 'Taux invalide (0-30%)' });

    await expect(simulerEmprunt(c, stockageEnMemoire('jeton'), SAISIE)).resolves.toEqual({
      type: 'refus',
      message: 'Taux invalide (0-30%)',
    });
  });

  it('[FR-007-01] sans jeton : session terminée, sans appel', async () => {
    const c = client({ statut: 'simulation', simulation: SIMULATION });
    const stockage = stockageEnMemoire();

    await expect(simulerEmprunt(c, stockage, SAISIE)).resolves.toEqual({ type: 'deconnecte' });
    expect(c.simuler).not.toHaveBeenCalled();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-007-01] sur 401 : jeton purgé et session terminée', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(simulerEmprunt(client({ statut: 'nonAuthentifie' }), stockage, SAISIE)).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-007-03] sur une panne du service ou du réseau : indisponible, jeton conservé', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(simulerEmprunt(client({ statut: 'technique' }), stockage, SAISIE)).resolves.toEqual({ type: 'indisponible' });
    await expect(simulerEmprunt(client(new Error('réseau')), stockage, SAISIE)).resolves.toEqual({ type: 'indisponible' });
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });
});

describe('listerPrets', () => {
  it('[FR-007-08] rend les prêts lus avec le jeton de la session', async () => {
    const c = client({ statut: 'technique' }, { statut: 'prets', prets: [PRET] });

    await expect(listerPrets(c, stockageEnMemoire('jeton'))).resolves.toEqual({ type: 'prets', prets: [PRET] });
    expect(c.lister).toHaveBeenCalledWith('jeton');
  });

  it('[FR-007-01] sans jeton : session terminée, sans appel', async () => {
    const c = client({ statut: 'technique' });
    const stockage = stockageEnMemoire();

    await expect(listerPrets(c, stockage)).resolves.toEqual({ type: 'deconnecte' });
    expect(c.lister).not.toHaveBeenCalled();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-007-01] sur 401 : jeton purgé et session terminée', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(
      listerPrets(client({ statut: 'technique' }, { statut: 'nonAuthentifie' }), stockage),
    ).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-007-08] sur une panne du service ou du réseau : indisponible', async () => {
    await expect(
      listerPrets(client({ statut: 'technique' }, { statut: 'technique' }), stockageEnMemoire('jeton')),
    ).resolves.toEqual({ type: 'indisponible' });
    await expect(
      listerPrets(client({ statut: 'technique' }, new Error('réseau')), stockageEnMemoire('jeton')),
    ).resolves.toEqual({ type: 'indisponible' });
  });
});

describe('deposerDemande', () => {
  const DEMANDE = { type: 'CONSOMMATION', montant: '10000', tauxAnnuel: '3.50', dureeMois: '300', objet: 'Voiture' };
  const sansDepot = { statut: 'technique' } as const;

  it('[FR-007-07] rend le prêt déposé, appelé avec le jeton et la saisie', async () => {
    const c = client(sansDepot, undefined, { statut: 'deposee', pret: PRET });

    await expect(deposerDemande(c, stockageEnMemoire('jeton'), DEMANDE)).resolves.toEqual({ type: 'deposee', pret: PRET });
    expect(c.deposer).toHaveBeenCalledWith('jeton', DEMANDE);
  });

  it('[FR-007-05] rend le message du service sur un refus', async () => {
    const c = client(sansDepot, undefined, { statut: 'refusee', message: 'Duree invalide' });

    await expect(deposerDemande(c, stockageEnMemoire('jeton'), DEMANDE)).resolves.toEqual({
      type: 'refus',
      message: 'Duree invalide',
    });
  });

  it('[FR-007-01] sans jeton : session terminée, sans appel', async () => {
    const c = client(sansDepot);
    const stockage = stockageEnMemoire();

    await expect(deposerDemande(c, stockage, DEMANDE)).resolves.toEqual({ type: 'deconnecte' });
    expect(c.deposer).not.toHaveBeenCalled();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-007-01] sur 401 : jeton purgé et session terminée', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(
      deposerDemande(client(sansDepot, undefined, { statut: 'nonAuthentifie' }), stockage, DEMANDE),
    ).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-024-02] sur une panne du service ou du réseau : indisponible, jeton conservé', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(deposerDemande(client(sansDepot), stockage, DEMANDE)).resolves.toEqual({ type: 'indisponible' });
    await expect(deposerDemande(client(sansDepot, undefined, new Error('réseau')), stockage, DEMANDE)).resolves.toEqual({
      type: 'indisponible',
    });
    expect(stockage.supprimer).not.toHaveBeenCalled();
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
  it('[FR-007-11] rend les échéances du prêt, appelées avec le jeton et l’identifiant', async () => {
    const c = client(sansEcheances, undefined, undefined, { statut: 'echeances', echeances: [ECHEANCE] });

    await expect(consulterEcheances(c, stockageEnMemoire('jeton'), 1)).resolves.toEqual({
      type: 'echeances',
      echeances: [ECHEANCE],
    });
    expect(c.echeances).toHaveBeenCalledWith('jeton', 1);
  });

  it('[FR-007-11] sans jeton : session terminée, sans appel', async () => {
    const c = client(sansEcheances);
    const stockage = stockageEnMemoire();

    await expect(consulterEcheances(c, stockage, 1)).resolves.toEqual({ type: 'deconnecte' });
    expect(c.echeances).not.toHaveBeenCalled();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-007-11] sur 401 : jeton purgé et session terminée', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(
      consulterEcheances(client(sansEcheances, undefined, undefined, { statut: 'nonAuthentifie' }), stockage, 1),
    ).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it('[FR-007-11] sur une panne du service ou du réseau : indisponible, jeton conservé', async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(
      consulterEcheances(client(sansEcheances, undefined, undefined, { statut: 'technique' }), stockage, 1),
    ).resolves.toEqual({ type: 'indisponible' });
    await expect(
      consulterEcheances(client(sansEcheances, undefined, undefined, new Error('réseau')), stockage, 1),
    ).resolves.toEqual({ type: 'indisponible' });
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });
});

describe.each([
  ['FR-007-12', 'payerEcheance', payerEcheance, 'payer'],
  ['FR-007-13', 'supprimerPret', supprimerPret, 'supprimer'],
] as const)('[%s] %s', (etiquette, _nom, cas, methode) => {
  it(`[${etiquette}] réussit avec le jeton et l’identifiant`, async () => {
    const c = client(sansEcheances);

    await expect(cas(c, stockageEnMemoire('jeton'), 7)).resolves.toEqual({ type: 'faite' });
    expect(c[methode]).toHaveBeenCalledWith('jeton', 7);
  });

  it(`[${etiquette}] sans jeton : session terminée, sans appel`, async () => {
    const c = client(sansEcheances);
    const stockage = stockageEnMemoire();

    await expect(cas(c, stockage, 7)).resolves.toEqual({ type: 'deconnecte' });
    expect(c[methode]).not.toHaveBeenCalled();
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it(`[${etiquette}] sur 401 : jeton purgé et session terminée`, async () => {
    const stockage = stockageEnMemoire('jeton');
    const c = client(sansEcheances, undefined, undefined, undefined, { statut: 'nonAuthentifie' });

    await expect(cas(c, stockage, 7)).resolves.toEqual({ type: 'deconnecte' });
    expect(stockage.supprimer).toHaveBeenCalledOnce();
  });

  it(`[${etiquette}] sur toute panne : indisponible, jeton conservé`, async () => {
    const stockage = stockageEnMemoire('jeton');

    await expect(
      cas(client(sansEcheances, undefined, undefined, undefined, { statut: 'technique' }), stockage, 7),
    ).resolves.toEqual({ type: 'indisponible' });
    await expect(
      cas(client(sansEcheances, undefined, undefined, undefined, new Error('réseau')), stockage, 7),
    ).resolves.toEqual({ type: 'indisponible' });
    expect(stockage.supprimer).not.toHaveBeenCalled();
  });
});
