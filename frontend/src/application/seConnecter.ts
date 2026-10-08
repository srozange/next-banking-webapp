import { accueilDuRole, ROUTE_CONNEXION, validerIdentifiants, type ChampConnexion } from '../domain/connexion';
import type { SessionLocale } from './session';

/** Réponse de l'API de connexion, traduite par l'adaptateur HTTP, qui a déjà ouvert la session (rôles connus). */
export type ReponseConnexion =
  | { statut: 'acceptee'; roles: readonly string[] }
  | { statut: 'refusee' }
  | { statut: 'technique' };

/** Port sortant : appelle l'API de connexion. */
export interface ClientConnexion {
  connecter(identifiant: string, motDePasse: string): Promise<ReponseConnexion>;
}

export type ResultatConnexion =
  | { type: 'champs'; champs: ChampConnexion[] }
  | { type: 'refus' }
  | { type: 'technique' }
  | { type: 'accepte'; route: string };

const TECHNIQUE: ResultatConnexion = { type: 'technique' };

/**
 * Cas d'usage : valide les champs sans appeler l'API, puis se connecte. La session n'est ouverte par l'adaptateur
 * que si la connexion est acceptée et que le rôle mène à un écran d'accueil connu.
 */
export async function seConnecter(
  identifiant: string,
  motDePasse: string,
  client: ClientConnexion,
): Promise<ResultatConnexion> {
  const champs = validerIdentifiants(identifiant, motDePasse);
  if (champs.length > 0) {
    return { type: 'champs', champs };
  }
  try {
    const reponse = await client.connecter(identifiant, motDePasse);
    if (reponse.statut === 'refusee') {
      return { type: 'refus' };
    }
    if (reponse.statut === 'technique') {
      return TECHNIQUE;
    }
    const route = accueilDuRole(reponse.roles);
    if (route === null) {
      return TECHNIQUE;
    }
    return { type: 'accepte', route };
  } catch {
    return TECHNIQUE;
  }
}

/**
 * Cas d'usage : écran d'accueil de la session en cours. Sans session, la connexion ; une session illisible
 * ou sans rôle connu est terminée et mène aussi à la connexion.
 */
export function accueilDeLaSession(session: SessionLocale): string {
  const roles = session.roles();
  // La session ne rend que des rôles dont l'accueil est connu.
  return roles === null ? ROUTE_CONNEXION : (accueilDuRole(roles) as string);
}
