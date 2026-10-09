import { describe, expect, it } from 'vitest';
import {
  assemblerPortefeuille,
  carteDuClient,
  coordonnees,
  formaterEurosArrondi,
  initiale,
  statutAvecBadge,
  type ClientDuPortefeuille,
  type PretDuPortefeuille,
} from './portefeuille';

const JEAN: ClientDuPortefeuille = {
  clientId: 1, nomComplet: 'Jean Dupont', login: 'user1', email: 'jean.dupont@example.com', telephone: '+33601020304', actif: true,
};
const MARIE: ClientDuPortefeuille = {
  clientId: 2, nomComplet: 'Marie Lebrun', login: 'user2', email: 'marie@example.com', telephone: null, actif: false,
};

const pret = (pretId: number, statut: string): PretDuPortefeuille => ({
  pretId, type: 'CONSOMMATION', montant: '5000.00', mensualite: '146.51', statut,
});

describe('assemblerPortefeuille', () => {
  it('[FR-015-03] joint par clientId dans l’ordre de l’identité et calcule les indicateurs en centimes', () => {
    const portefeuille = assemblerPortefeuille(
      [JEAN, MARIE],
      [
        { clientId: 2, comptes: [] },
        {
          clientId: 1,
          comptes: [
            { compteId: 10, numero: 'FR76-0001-0001', type: 'COURANT', solde: '2130.00' },
            { compteId: 11, numero: 'FR76-0001-0002', type: 'EPARGNE', solde: '15200.00' },
          ],
        },
      ],
      [{ clientId: 1, prets: [pret(5, 'EN_ATTENTE'), pret(6, 'EN_COURS'), pret(7, 'EN_COURS')] }],
    );

    expect(portefeuille.nombreClients).toBe(2);
    expect(portefeuille.encoursCentimes).toBe(1733000);
    expect(portefeuille.pretsEnAttente).toBe(1);
    expect(portefeuille.cartes.map((carte) => carte.client.nomComplet)).toEqual(['Jean Dupont', 'Marie Lebrun']);
    const [jean, marie] = portefeuille.cartes;
    expect(jean).toMatchObject({ soldeTotalCentimes: 1733000, nombreComptes: 2, pretsEnCours: 2, pretsEnAttente: 1 });
    expect(marie).toMatchObject({ comptes: [], prets: [], soldeTotalCentimes: 0, nombreComptes: 0, pretsEnCours: 0, pretsEnAttente: 0 });
  });

  it('[FR-015-04] portefeuille vide : indicateurs à zéro', () => {
    expect(assemblerPortefeuille([], [], [])).toEqual({ cartes: [], nombreClients: 0, encoursCentimes: 0, pretsEnAttente: 0 });
  });

  it('[FR-015-08] un client absent des comptes et des prêts a des listes vides', () => {
    const [carte] = assemblerPortefeuille([JEAN], [{ clientId: 99, comptes: [] }], []).cartes;

    expect(carte?.comptes).toEqual([]);
    expect(carte?.prets).toEqual([]);
  });

  it.each([
    [[pret(1, 'EN_ATTENTE'), pret(2, 'EN_COURS'), pret(3, 'EN_COURS')], { type: 'attente', nombre: 1 }],
    [[pret(1, 'EN_COURS'), pret(2, 'EN_COURS'), pret(3, 'REMBOURSE')], { type: 'cours', nombre: 2 }],
    [[pret(1, 'REFUSE'), pret(2, 'DEFAUT')], { type: 'aucun' }],
    [[], { type: 'aucun' }],
  ])('[FR-015-06] indicateur de prêts : attente prioritaire sur en cours (%#)', (prets, attendu) => {
    const [carte] = assemblerPortefeuille([JEAN], [], [{ clientId: 1, prets }]).cartes;

    expect(carte?.indicateur).toEqual(attendu);
  });
});

describe('formaterEurosArrondi', () => {
  it.each([
    [1733000, 'EUR17,330'],
    [1733050, 'EUR17,330.5'],
    [1733055, 'EUR17,330.55'],
    [1733005, 'EUR17,330.05'],
    [0, 'EUR0'],
    [500000, 'EUR5,000'],
    [123456789, 'EUR1,234,567.89'],
    [-250050, '-EUR2,500.5'],
  ])('[FR-015-03] %i centimes → %s', (centimes, attendu) => {
    expect(formaterEurosArrondi(centimes)).toBe(attendu);
  });
});

describe('règles d’affichage', () => {
  it('[FR-015-05] initiale du nom complet, vide pour un nom vide', () => {
    expect(initiale('Jean Dupont')).toBe('J');
    expect(initiale('')).toBe('');
  });

  it.each([
    ['EN_ATTENTE', true],
    ['EN_COURS', true],
    ['REFUSE', true],
    ['REMBOURSE', true],
    ['DEFAUT', false],
    ['AUTO', false],
  ])('[FR-015-09][BUG-053] statut %s en badge : %s', (statut, attendu) => {
    expect(statutAvecBadge(statut)).toBe(attendu);
  });

  it('[FR-015-07] coordonnées : @login · email · téléphone, parties nulles omises', () => {
    expect(coordonnees(JEAN)).toBe('@user1 · jean.dupont@example.com · +33601020304');
    expect(coordonnees(MARIE)).toBe('@user2 · marie@example.com');
    expect(coordonnees({ ...MARIE, email: null })).toBe('@user2');
  });

  it('[FR-015-07] carte du client sélectionné, ou null', () => {
    const portefeuille = assemblerPortefeuille([JEAN, MARIE], [], []);

    expect(carteDuClient(portefeuille, 2)?.client).toBe(MARIE);
    expect(carteDuClient(portefeuille, 3)).toBeNull();
  });
});
