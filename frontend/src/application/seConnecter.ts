import { accueilDuRole, ROUTE_CONNEXION, validerIdentifiants, type ChampConnexion } from '../domain/connexion';

/** Réponse de l'API de connexion, traduite par l'adaptateur HTTP (rôles lus dans le jeton). */
export type ReponseConnexion =
  | { statut: 'acceptee'; jeton: string; roles: readonly string[] }
  | { statut: 'refusee' }
  | { statut: 'technique' };

/** Port sortant : appelle l'API de connexion. */
export interface ClientConnexion {
  connecter(identifiant: string, motDePasse: string): Promise<ReponseConnexion>;
}

/** Port sortant : conserve le jeton de session. */
export interface StockageJeton {
  lire(): string | null;
  enregistrer(jeton: string): void;
  supprimer(): void;
}

/** Port sortant : lit les rôles portés par un jeton, ou null s'il est illisible. */
export interface DecodeurJeton {
  roles(jeton: string): readonly string[] | null;
}

export type ResultatConnexion =
  | { type: 'champs'; champs: ChampConnexion[] }
  | { type: 'refus' }
  | { type: 'technique' }
  | { type: 'accepte'; route: string };

const TECHNIQUE: ResultatConnexion = { type: 'technique' };

/**
 * Cas d'usage : valide les champs sans appeler l'API, puis se connecte. Le jeton n'est enregistré
 * que si la connexion est acceptée et que le rôle mène à un écran d'accueil connu.
 */
export async function seConnecter(
  identifiant: string,
  motDePasse: string,
  client: ClientConnexion,
  stockage: StockageJeton,
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
    stockage.enregistrer(reponse.jeton);
    return { type: 'accepte', route };
  } catch {
    return TECHNIQUE;
  }
}

/**
 * Cas d'usage : écran d'accueil de la session en cours. Sans jeton, la connexion ; un jeton illisible
 * ou sans rôle connu est supprimé et mène aussi à la connexion.
 */
export function accueilDeLaSession(stockage: StockageJeton, decodeur: DecodeurJeton): string {
  const jeton = stockage.lire();
  if (jeton === null) {
    return ROUTE_CONNEXION;
  }
  const roles = decodeur.roles(jeton);
  const route = roles === null ? null : accueilDuRole(roles);
  if (route === null) {
    stockage.supprimer();
    return ROUTE_CONNEXION;
  }
  return route;
}
