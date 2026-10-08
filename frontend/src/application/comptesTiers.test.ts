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
import type { SessionLocale } from './session';

const ETAT: EtatComptesTiers = { connecte: false, comptes: [], compteSelectionne: null, transactions: [], message: '', patrimoineTotal: '0.00' };

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
  ['charger', (c: ClientComptesTiers, s: SessionLocale) => chargerComptesTiers(c, s), 'lireEtat', []],
  ['connecter', (c: ClientComptesTiers, s: SessionLocale) => connecterBanque(c, s), 'connecter', []],
  ['sélectionner', (c: ClientComptesTiers, s: SessionLocale) => selectionnerCompte(c, s, 7), 'selectionner', [7]],
  ['rafraîchir', (c: ClientComptesTiers, s: SessionLocale) => rafraichirComptes(c, s), 'rafraichir', []],
  ['déconnecter', (c: ClientComptesTiers, s: SessionLocale) => deconnecterPowens(c, s), 'deconnecter', []],
] as const;

describe.each(CAS)('comptes tiers : %s', (_nom, lancer, methode, arguments_) => {
  it('[FR-012-03] rend la valeur reçue dans la session', async () => {
    const service = client({ statut: 'ok', donnees: ETAT });
    const session = sessionEnMemoire();

    expect(await lancer(service, session)).toEqual({ type: 'ok', valeur: ETAT });
    expect(service[methode]).toHaveBeenCalledWith(...arguments_);
  });

  it('[FR-012-01] sans session, la session est terminée sans appel', async () => {
    const service = client({ statut: 'ok', donnees: ETAT });
    const session = sessionEnMemoire(false);

    expect(await lancer(service, session)).toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalled();
    expect(service[methode]).not.toHaveBeenCalled();
  });

  it('[FR-012-01] un 401 termine la session', async () => {
    const session = sessionEnMemoire();

    expect(await lancer(client({ statut: 'nonAuthentifie' }), session)).toEqual({ type: 'deconnecte' });
    expect(session.terminer).toHaveBeenCalled();
  });

  it('[BUG-043] une indisponibilité de Powens est signalée, la session est gardée', async () => {
    const session = sessionEnMemoire();

    expect(await lancer(client({ statut: 'indisponible' }), session)).toEqual({ type: 'indisponible' });
    expect(session.terminer).not.toHaveBeenCalled();
  });

  it('[FR-012-05] une panne technique est signalée', async () => {
    expect(await lancer(client({ statut: 'technique' }), sessionEnMemoire())).toEqual({ type: 'technique' });
  });
});
