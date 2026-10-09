import {
  assemblerPortefeuille,
  type ClientDuPortefeuille,
  type ComptesDuClient,
  type Portefeuille,
  type PretsDuClient,
} from '../domain/portefeuille';
import { sessionOuverteOuFin, type SessionLocale } from './session';

/**
 * Réponse d'un des trois points `…/conseiller/portefeuille`, traduite par l'adaptateur HTTP (jamais d'exception) ;
 * `erreur` porte le message à afficher, déjà choisi par l'adaptateur.
 */
export type ReponsePortefeuille<T> = { statut: 'ok'; donnees: T } | { statut: 'nonAuthentifie' } | { statut: 'erreur'; message: string };

/**
 * Port sortant : portefeuille du conseiller connecté, décidé côté serveur (identité, comptes, prêts). Aucun
 * identifiant de conseiller ni de client n'est transmis.
 */
export interface ClientPortefeuille {
  lireIdentites(): Promise<ReponsePortefeuille<readonly ClientDuPortefeuille[]>>;
  lireComptes(): Promise<ReponsePortefeuille<readonly ComptesDuClient[]>>;
  lirePrets(): Promise<ReponsePortefeuille<readonly PretsDuClient[]>>;
}

/**
 * Ce que montre l'écran : le portefeuille seulement si les trois réponses sont ok (sinon null), et les messages
 * d'erreur distincts des appels en échec.
 */
export type ResultatPortefeuille =
  | { type: 'etat'; portefeuille: Portefeuille | null; messages: readonly string[] }
  | { type: 'deconnecte' };

const DECONNECTE: ResultatPortefeuille = { type: 'deconnecte' };

/**
 * Cas d'usage (FR-015-02) : lit l'identité, les comptes et les prêts du portefeuille en parallèle, jamais en
 * cascade, puis les joint (FR-015-03). Un 401 de l'un des trois termine la session.
 */
export async function chargerPortefeuille(client: ClientPortefeuille, session: SessionLocale): Promise<ResultatPortefeuille> {
  if (!sessionOuverteOuFin(session)) {
    return DECONNECTE;
  }
  const [identites, comptes, prets] = await Promise.all([client.lireIdentites(), client.lireComptes(), client.lirePrets()]);
  const reponses = [identites, comptes, prets];
  if (reponses.some((reponse) => reponse.statut === 'nonAuthentifie')) {
    session.terminer();
    return DECONNECTE;
  }
  if (identites.statut === 'ok' && comptes.statut === 'ok' && prets.statut === 'ok') {
    return { type: 'etat', portefeuille: assemblerPortefeuille(identites.donnees, comptes.donnees, prets.donnees), messages: [] };
  }
  const messages = reponses.flatMap((reponse) => (reponse.statut === 'erreur' ? [reponse.message] : []));
  return { type: 'etat', portefeuille: null, messages: [...new Set(messages)] };
}
