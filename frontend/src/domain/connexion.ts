/**
 * Règles pures de la connexion (FEAT-001) : champs obligatoires et écran d'accueil selon le rôle.
 */

export type ChampConnexion = 'identifiant' | 'motDePasse';

export const ROUTE_CONNEXION = '/login';

/**
 * Champs obligatoires (FR-001-03), dans l'ordre du formulaire : un identifiant blanc est vide ;
 * le mot de passe n'est refusé que s'il est vide (même règle que le service identité et clients).
 */
export function validerIdentifiants(identifiant: string, motDePasse: string): ChampConnexion[] {
  const champsEnErreur: ChampConnexion[] = [];
  if (identifiant.trim() === '') {
    champsEnErreur.push('identifiant');
  }
  if (motDePasse === '') {
    champsEnErreur.push('motDePasse');
  }
  return champsEnErreur;
}

/** Rôles par ordre de priorité, avec leur écran d'accueil (FR-001-05, FR-030-03). */
const ACCUEILS: ReadonlyArray<readonly [role: string, route: string]> = [
  ['ADMIN', '/administration'],
  ['CONSEILLER', '/espace-conseiller'],
  ['CLIENT', '/tableau-de-bord'],
];

/** Écran d'accueil du rôle le plus élevé (ADMIN > CONSEILLER > CLIENT), ou null si aucun rôle n'est connu. */
export function accueilDuRole(roles: readonly string[]): string | null {
  const accueil = ACCUEILS.find(([role]) => roles.includes(role));
  return accueil === undefined ? null : accueil[1];
}
