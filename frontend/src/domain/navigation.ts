/**
 * Règles pures de la navigation authentifiée (FEAT-030, FEAT-015, FEAT-016) : liens d'en-tête filtrés par rôle,
 * badge de rôle, sous-navigation des espaces réservés et accès aux routes réservées.
 */

export const ROUTE_TABLEAU_DE_BORD = '/tableau-de-bord';
export const ROUTE_ESPACE_CONSEILLER = '/espace-conseiller';
export const ROUTE_ADMINISTRATION = '/administration';

/** Lien d'en-tête : clé du libellé (adaptateur) et route cible. */
export type CleLien =
  | 'tableauDeBord'
  | 'virement'
  | 'virementsProgrammes'
  | 'releve'
  | 'budget'
  | 'prets'
  | 'documents'
  | 'bourse'
  | 'monConseiller'
  | 'produits'
  | 'comptesTiers'
  | 'beneficiaires'
  | 'espaceConseiller'
  | 'administration';

export interface LienNavigation {
  cle: CleLien;
  route: string;
}

/** Les 12 liens du legacy, visibles par tous les rôles, dans l'ordre du legacy (FR-030-04). */
const LIENS_COMMUNS: readonly LienNavigation[] = [
  { cle: 'tableauDeBord', route: ROUTE_TABLEAU_DE_BORD },
  { cle: 'virement', route: '/virement' },
  { cle: 'virementsProgrammes', route: '/virements-programmes' },
  { cle: 'releve', route: '/releve' },
  { cle: 'budget', route: '/budget' },
  { cle: 'prets', route: '/prets' },
  { cle: 'documents', route: '/documents' },
  { cle: 'bourse', route: '/bourse' },
  { cle: 'monConseiller', route: '/mon-conseiller' },
  { cle: 'produits', route: '/produits' },
  { cle: 'comptesTiers', route: '/comptes-tiers' },
  { cle: 'beneficiaires', route: '/beneficiaires' },
];

/** Routes réservées et rôles admis (FR-001-11, FR-016-01, RG-030-008, RG-030-009). */
const ROUTES_RESERVEES: ReadonlyArray<{ lien: LienNavigation; roles: readonly string[] }> = [
  { lien: { cle: 'espaceConseiller', route: ROUTE_ESPACE_CONSEILLER }, roles: ['CONSEILLER', 'ADMIN'] },
  { lien: { cle: 'administration', route: ROUTE_ADMINISTRATION }, roles: ['ADMIN'] },
];

const admis = (rolesAdmis: readonly string[], roles: readonly string[]) => rolesAdmis.some((role) => roles.includes(role));

/** Liens d'en-tête du rôle : les 12 liens communs, puis Espace Conseiller et Administration selon le rôle. */
export function liensDuRole(roles: readonly string[]): LienNavigation[] {
  return [
    ...LIENS_COMMUNS,
    ...ROUTES_RESERVEES.filter((reservee) => admis(reservee.roles, roles)).map((reservee) => reservee.lien),
  ];
}

export type BadgeRole = 'admin' | 'conseiller';

/** Badge de rôle (priorité ADMIN > CONSEILLER) ; aucun pour un CLIENT. */
export function badgeDuRole(roles: readonly string[]): BadgeRole | null {
  if (roles.includes('ADMIN')) {
    return 'admin';
  }
  return roles.includes('CONSEILLER') ? 'conseiller' : null;
}

/** `chemin` est `route` ou une page sous `route/`. */
function sous(chemin: string, route: string): boolean {
  return chemin === route || chemin.startsWith(`${route}/`);
}

/** Accès à une route : libre hors routes réservées, sinon réservé aux rôles admis. */
export function routeAutorisee(chemin: string, roles: readonly string[]): boolean {
  const reservee = ROUTES_RESERVEES.find(({ lien }) => sous(chemin, lien.route));
  return reservee === undefined || admis(reservee.roles, roles);
}

export type CleSousLien = 'monPortefeuille' | 'conseillerIa' | 'administration' | 'espaceConseiller' | 'monTableauDeBord';

export interface SousNavigation {
  /** Lien d'en-tête de l'espace, qui nomme la sous-navigation. */
  espace: LienNavigation;
  liens: ReadonlyArray<{ cle: CleSousLien; route: string }>;
}

/** Sous-en-têtes des deux espaces réservés (UX-DR70, UX-DR71) ; deux niveaux de navigation au plus. */
const SOUS_NAVIGATIONS: readonly SousNavigation[] = [
  {
    espace: { cle: 'espaceConseiller', route: ROUTE_ESPACE_CONSEILLER },
    liens: [
      { cle: 'monPortefeuille', route: ROUTE_ESPACE_CONSEILLER },
      { cle: 'conseillerIa', route: '/mon-conseiller' },
    ],
  },
  {
    espace: { cle: 'administration', route: ROUTE_ADMINISTRATION },
    liens: [
      { cle: 'administration', route: ROUTE_ADMINISTRATION },
      { cle: 'espaceConseiller', route: ROUTE_ESPACE_CONSEILLER },
      { cle: 'monTableauDeBord', route: ROUTE_TABLEAU_DE_BORD },
    ],
  },
];

/** Sous-navigation de la page courante, ou null hors des espaces réservés. */
export function sousNavigation(chemin: string): SousNavigation | null {
  return SOUS_NAVIGATIONS.find(({ espace }) => sous(chemin, espace.route)) ?? null;
}
