import { describe, expect, it, vi } from 'vitest';
import type { EtatComptesTiers } from '../domain/comptesTiers';
import {
  chargerComptesTiers,
  connecterBanque,
  deconnecterPowens,
  rafraichirComptes,
  selectionnerCompte,
  type ClientComptesTiers,
  type ReponseTiers,
} from './comptesTiers';
import type { StockageJeton } from './seConnecter';

const ETAT: EtatComptesTiers = { connecte: false, comptes: [], compteSelectionne: null, transactions: [], message: '', patrimoineTotal: '0.00' };

function stockage(jeton: string | null): StockageJeton {
  return { lire: vi.fn(() => jeton), enregistrer: vi.fn(), supprimer: vi.fn() };
}

function client(reponse: ReponseTiers<never> | ReponseTiers<unknown>): ClientComptesTiers {
  const repondre = vi.fn(() => Promise.resolve(reponse));
  return {
    lireEtat: repondre as ClientComptesTiers['lireEtat'],
    connecter: repondre as ClientComptesTiers['connecter'],
    selectionner: repondre as ClientComptesTiers['selectionner'],
    rafraichir: repondre as ClientComptesTiers['rafraichir'],
    deconnecter: repondre as ClientComptesTiers['deconnecter'],
  };
}

const CAS = [
  ['charger', (c: ClientComptesTiers, s: StockageJeton) => chargerComptesTiers(c, s), 'lireEtat', ['jwt']],
  ['connecter', (c: ClientComptesTiers, s: StockageJeton) => connecterBanque(c, s), 'connecter', ['jwt']],
  ['sélectionner', (c: ClientComptesTiers, s: StockageJeton) => selectionnerCompte(c, s, 7), 'selectionner', ['jwt', 7]],
  ['rafraîchir', (c: ClientComptesTiers, s: StockageJeton) => rafraichirComptes(c, s), 'rafraichir', ['jwt']],
  ['déconnecter', (c: ClientComptesTiers, s: StockageJeton) => deconnecterPowens(c, s), 'deconnecter', ['jwt']],
] as const;

describe.each(CAS)('comptes tiers : %s', (_nom, lancer, methode, arguments_) => {
  it('[FR-012-03] rend la valeur reçue avec le jeton de la session', async () => {
    const service = client({ statut: 'ok', donnees: ETAT });
    const jetons = stockage('jwt');

    expect(await lancer(service, jetons)).toEqual({ type: 'ok', valeur: ETAT });
    expect(service[methode]).toHaveBeenCalledWith(...arguments_);
  });

  it('[FR-012-01] sans jeton, la session est terminée sans appel', async () => {
    const service = client({ statut: 'ok', donnees: ETAT });
    const jetons = stockage(null);

    expect(await lancer(service, jetons)).toEqual({ type: 'deconnecte' });
    expect(jetons.supprimer).toHaveBeenCalled();
    expect(service[methode]).not.toHaveBeenCalled();
  });

  it('[FR-012-01] un 401 supprime le jeton et termine la session', async () => {
    const jetons = stockage('jwt');

    expect(await lancer(client({ statut: 'nonAuthentifie' }), jetons)).toEqual({ type: 'deconnecte' });
    expect(jetons.supprimer).toHaveBeenCalled();
  });

  it('[BUG-043] une indisponibilité de Powens est signalée, le jeton est gardé', async () => {
    const jetons = stockage('jwt');

    expect(await lancer(client({ statut: 'indisponible' }), jetons)).toEqual({ type: 'indisponible' });
    expect(jetons.supprimer).not.toHaveBeenCalled();
  });

  it('[FR-012-05] une panne technique est signalée', async () => {
    expect(await lancer(client({ statut: 'technique' }), stockage('jwt'))).toEqual({ type: 'technique' });
  });
});
