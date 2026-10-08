import type { EtatComptesTiers } from '../domain/comptesTiers';
import { sessionOuverteOuFin, type SessionLocale } from './session';

/** Réponse d'un appel au service Powens, traduite par l'adaptateur HTTP (jamais d'exception). */
export type ReponseTiers<T> =
  | { statut: 'ok'; donnees: T }
  | { statut: 'nonAuthentifie' }
  /** 503 : Powens est indisponible (jamais présenté comme une liste vide, BUG-043). */
  | { statut: 'indisponible' }
  | { statut: 'technique' };

/** Port sortant : service open-banking-conseiller, intégration Powens. */
export interface ClientComptesTiers {
  lireEtat(): Promise<ReponseTiers<EtatComptesTiers>>;
  /** Lance la connexion ; rend l'adresse de la page web de Powens. */
  connecter(): Promise<ReponseTiers<string>>;
  selectionner(compteId: number): Promise<ReponseTiers<EtatComptesTiers>>;
  rafraichir(): Promise<ReponseTiers<EtatComptesTiers>>;
  deconnecter(): Promise<ReponseTiers<null>>;
}

/** Résultat d'un cas d'usage : la valeur, une indisponibilité de Powens, une panne technique, ou la fin de session. */
export type ResultatTiers<T> =
  | { type: 'ok'; valeur: T }
  | { type: 'indisponible' }
  | { type: 'technique' }
  | { type: 'deconnecte' };

/** Exécute un appel dans la session ; sans session ou sur 401, la session est terminée. */
async function executer<T>(session: SessionLocale, appel: () => Promise<ReponseTiers<T>>): Promise<ResultatTiers<T>> {
  if (!sessionOuverteOuFin(session)) {
    return { type: 'deconnecte' };
  }
  const reponse = await appel();
  switch (reponse.statut) {
    case 'ok':
      return { type: 'ok', valeur: reponse.donnees };
    case 'nonAuthentifie':
      session.terminer();
      return { type: 'deconnecte' };
    case 'indisponible':
      return { type: 'indisponible' };
    case 'technique':
      return { type: 'technique' };
  }
}

/** Cas d'usage : lit l'état Powens du client (FR-012-03, FR-012-04). */
export const chargerComptesTiers = (client: ClientComptesTiers, session: SessionLocale) =>
  executer(session, () => client.lireEtat());

/** Cas d'usage : lance la connexion à une banque ; la valeur est l'adresse où envoyer le navigateur (FR-012-05). */
export const connecterBanque = (client: ClientComptesTiers, session: SessionLocale) =>
  executer(session, () => client.connecter());

/** Cas d'usage : sélectionne un compte et rend l'état avec ses transactions (FR-012-13, FR-012-14). */
export const selectionnerCompte = (client: ClientComptesTiers, session: SessionLocale, compteId: number) =>
  executer(session, () => client.selectionner(compteId));

/** Cas d'usage : relit les comptes chez Powens (FR-012-16). */
export const rafraichirComptes = (client: ClientComptesTiers, session: SessionLocale) =>
  executer(session, () => client.rafraichir());

/** Cas d'usage : déconnecte Powens (FR-012-15). */
export const deconnecterPowens = (client: ClientComptesTiers, session: SessionLocale) =>
  executer(session, () => client.deconnecter());
