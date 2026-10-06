import { describe, expect, it, vi } from 'vitest';
import type { Pret, Simulation } from '../domain/pret';
import { listerPrets, simulerEmprunt, type ClientPrets, type ReponsePrets, type ReponseSimulation } from './prets';
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

function client(simulation: ReponseSimulation | Error, prets: ReponsePrets | Error = { statut: 'prets', prets: [] }) {
  const repondre = <T>(reponse: T | Error) => (reponse instanceof Error ? Promise.reject(reponse) : Promise.resolve(reponse));
  return {
    simuler: vi.fn<ClientPrets['simuler']>(() => repondre(simulation)),
    lister: vi.fn<ClientPrets['lister']>(() => repondre(prets)),
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
