import type { ClientAdministre, IndicateursIdentite, IndicateursPrets, UtilisateurAdministre } from '../domain/administration';
import { sessionOuverteOuFin, type SessionLocale } from './session';

/**
 * Réponse d'un des quatre points d'administration, traduite par l'adaptateur HTTP (jamais d'exception) ; `erreur`
 * porte le message à afficher, déjà choisi par l'adaptateur.
 */
export type ReponseAdministration<T> = { statut: 'ok'; donnees: T } | { statut: 'nonAuthentifie' } | { statut: 'erreur'; message: string };

/**
 * Port sortant : données de la page d'administration (identite-clients et credits), réservées à l'ADMIN côté
 * serveur. Aucun paramètre n'est transmis.
 */
export interface ClientAdministration {
  lireIndicateursIdentite(): Promise<ReponseAdministration<IndicateursIdentite>>;
  lireIndicateursPrets(): Promise<ReponseAdministration<IndicateursPrets>>;
  lireClients(): Promise<ReponseAdministration<readonly ClientAdministre[]>>;
  lireUtilisateurs(): Promise<ReponseAdministration<readonly UtilisateurAdministre[]>>;
}

/** Une partie de la page : ses données si la réponse est ok (sinon null), et le message de l'échec (sinon null). */
export interface PartieAdministration<T> {
  donnees: T | null;
  message: string | null;
}

/** Ce que montre l'écran : chaque partie indépendamment des autres, ou la fin de session. */
export type ResultatAdministration =
  | {
      type: 'etat';
      indicateursIdentite: PartieAdministration<IndicateursIdentite>;
      indicateursPrets: PartieAdministration<IndicateursPrets>;
      clients: PartieAdministration<readonly ClientAdministre[]>;
      utilisateurs: PartieAdministration<readonly UtilisateurAdministre[]>;
    }
  | { type: 'deconnecte' };

const DECONNECTE: ResultatAdministration = { type: 'deconnecte' };

function partie<T>(reponse: ReponseAdministration<T>): PartieAdministration<T> {
  return reponse.statut === 'ok' ? { donnees: reponse.donnees, message: null } : { donnees: null, message: (reponse as { message: string }).message };
}

/**
 * Cas d'usage (FR-016-02, FR-016-08, FR-016-13) : lit les indicateurs (identité et prêts), les clients et tous les
 * utilisateurs en parallèle, jamais en cascade. Un 401 de l'un des quatre termine la session ; une autre panne ne
 * prive que sa partie.
 */
export async function chargerAdministration(client: ClientAdministration, session: SessionLocale): Promise<ResultatAdministration> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  const [indicateursIdentite, indicateursPrets, clients, utilisateurs] = await Promise.all([
    client.lireIndicateursIdentite(),
    client.lireIndicateursPrets(),
    client.lireClients(),
    client.lireUtilisateurs(),
  ]);
  if ([indicateursIdentite, indicateursPrets, clients, utilisateurs].some((reponse) => reponse.statut === 'nonAuthentifie')) {
    session.terminer();
    return DECONNECTE;
  }
  return {
    type: 'etat',
    indicateursIdentite: partie(indicateursIdentite),
    indicateursPrets: partie(indicateursPrets),
    clients: partie(clients),
    utilisateurs: partie(utilisateurs),
  };
}
