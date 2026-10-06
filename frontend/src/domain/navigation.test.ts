import { describe, expect, it } from 'vitest';
import {
  badgeDuRole,
  liensDuRole,
  ROUTE_ADMINISTRATION,
  ROUTE_ESPACE_CONSEILLER,
  ROUTE_TABLEAU_DE_BORD,
  routeAutorisee,
  sousNavigation,
} from './navigation';

const COMMUNS = [
  ['tableauDeBord', '/tableau-de-bord'],
  ['virement', '/virement'],
  ['virementsProgrammes', '/virements-programmes'],
  ['releve', '/releve'],
  ['budget', '/budget'],
  ['prets', '/prets'],
  ['documents', '/documents'],
  ['bourse', '/bourse'],
  ['monConseiller', '/mon-conseiller'],
  ['produits', '/produits'],
  ['comptesTiers', '/comptes-tiers'],
  ['beneficiaires', '/beneficiaires'],
];

const paires = (liens: ReadonlyArray<{ cle: string; route: string }>) => liens.map(({ cle, route }) => [cle, route]);

describe('liensDuRole', () => {
  it('[FR-030-04] CLIENT : les 12 liens communs, dans l’ordre du legacy', () => {
    expect(paires(liensDuRole(['CLIENT']))).toEqual(COMMUNS);
  });

  it('[FR-030-04] CONSEILLER : les 12 liens puis Espace Conseiller', () => {
    expect(paires(liensDuRole(['CONSEILLER']))).toEqual([...COMMUNS, ['espaceConseiller', '/espace-conseiller']]);
  });

  it('[FR-030-04] ADMIN : les 12 liens, Espace Conseiller puis Administration', () => {
    expect(paires(liensDuRole(['ADMIN']))).toEqual([
      ...COMMUNS,
      ['espaceConseiller', '/espace-conseiller'],
      ['administration', '/administration'],
    ]);
  });

  it('[FR-030-04] sans rôle connu : les seuls liens communs', () => {
    expect(liensDuRole([])).toHaveLength(12);
  });
});

describe('badgeDuRole', () => {
  it('[FR-030-04] badge du rôle le plus élevé, aucun pour un client', () => {
    expect(badgeDuRole(['CLIENT'])).toBeNull();
    expect(badgeDuRole(['CONSEILLER'])).toBe('conseiller');
    expect(badgeDuRole(['ADMIN'])).toBe('admin');
    expect(badgeDuRole(['CONSEILLER', 'ADMIN'])).toBe('admin');
  });
});

describe('routeAutorisee', () => {
  it('[FR-001-11][FR-016-01] /espace-conseiller réservé à CONSEILLER et ADMIN', () => {
    expect(routeAutorisee('/espace-conseiller', ['CLIENT'])).toBe(false);
    expect(routeAutorisee('/espace-conseiller', ['CONSEILLER'])).toBe(true);
    expect(routeAutorisee('/espace-conseiller', ['ADMIN'])).toBe(true);
    expect(routeAutorisee('/espace-conseiller/client/3', ['CLIENT'])).toBe(false);
  });

  it('[FR-001-11][FR-016-01] /administration réservé à ADMIN', () => {
    expect(routeAutorisee('/administration', ['CLIENT'])).toBe(false);
    expect(routeAutorisee('/administration', ['CONSEILLER'])).toBe(false);
    expect(routeAutorisee('/administration', ['ADMIN'])).toBe(true);
    expect(routeAutorisee('/administration/clients', ['CONSEILLER'])).toBe(false);
  });

  it('[FR-030-02] les autres routes sont ouvertes à tout rôle, préfixe compris', () => {
    expect(routeAutorisee('/tableau-de-bord', ['CLIENT'])).toBe(true);
    expect(routeAutorisee('/administrationx', ['CLIENT'])).toBe(true);
  });

  it('expose les routes cibles', () => {
    expect([ROUTE_TABLEAU_DE_BORD, ROUTE_ESPACE_CONSEILLER, ROUTE_ADMINISTRATION]).toEqual([
      '/tableau-de-bord',
      '/espace-conseiller',
      '/administration',
    ]);
  });
});

describe('sousNavigation', () => {
  it('[FR-030-04] Espace Conseiller : Mon portefeuille · Conseiller IA', () => {
    expect(sousNavigation('/espace-conseiller')).toEqual({
      espace: { cle: 'espaceConseiller', route: '/espace-conseiller' },
      liens: [
        { cle: 'monPortefeuille', route: '/espace-conseiller' },
        { cle: 'conseillerIa', route: '/mon-conseiller' },
      ],
    });
    expect(sousNavigation('/espace-conseiller/client/3')?.espace.cle).toBe('espaceConseiller');
  });

  it('[FR-030-04] Administration : Administration · Espace Conseiller · Mon tableau de bord', () => {
    expect(sousNavigation('/administration')).toEqual({
      espace: { cle: 'administration', route: '/administration' },
      liens: [
        { cle: 'administration', route: '/administration' },
        { cle: 'espaceConseiller', route: '/espace-conseiller' },
        { cle: 'monTableauDeBord', route: '/tableau-de-bord' },
      ],
    });
  });

  it('[FR-030-04] aucune sous-navigation sur les pages client', () => {
    expect(sousNavigation('/tableau-de-bord')).toBeNull();
    expect(sousNavigation('/mon-conseiller')).toBeNull();
    expect(sousNavigation('/administrationx')).toBeNull();
  });
});
