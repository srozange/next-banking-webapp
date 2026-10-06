import type { EtatComptesTiers } from '../domain/comptesTiers';
import type { StockageJeton } from './seConnecter';

/** Réponse d'un appel au service Powens, traduite par l'adaptateur HTTP (jamais d'exception). */
export type ReponseTiers<T> =
  | { statut: 'ok'; donnees: T }
  | { statut: 'nonAuthentifie' }
  /** 503 : Powens est indisponible (jamais présenté comme une liste vide, BUG-043). */
  | { statut: 'indisponible' }
  | { statut: 'technique' };

/** Port sortant : service open-banking-conseiller, intégration Powens. */
export interface ClientComptesTiers {
  lireEtat(jeton: string): Promise<ReponseTiers<EtatComptesTiers>>;
  /** Lance la connexion ; rend l'adresse de la page web de Powens. */
  connecter(jeton: string): Promise<ReponseTiers<string>>;
  selectionner(jeton: string, compteId: number): Promise<ReponseTiers<EtatComptesTiers>>;
  rafraichir(jeton: string): Promise<ReponseTiers<EtatComptesTiers>>;
  deconnecter(jeton: string): Promise<ReponseTiers<null>>;
}

/** Résultat d'un cas d'usage : la valeur, une indisponibilité de Powens, une panne technique, ou la fin de session. */
export type ResultatTiers<T> =
  | { type: 'ok'; valeur: T }
  | { type: 'indisponible' }
  | { type: 'technique' }
  | { type: 'deconnecte' };

/** Exécute un appel avec le jeton de la session ; sans jeton ou sur 401, le jeton est supprimé et la session terminée. */
async function executer<T>(stockage: StockageJeton, appel: (jeton: string) => Promise<ReponseTiers<T>>): Promise<ResultatTiers<T>> {
  const jeton = stockage.lire();
  if (jeton === null) {
    stockage.supprimer();
    return { type: 'deconnecte' };
  }
  const reponse = await appel(jeton);
  switch (reponse.statut) {
    case 'ok':
      return { type: 'ok', valeur: reponse.donnees };
    case 'nonAuthentifie':
      stockage.supprimer();
      return { type: 'deconnecte' };
    case 'indisponible':
      return { type: 'indisponible' };
    case 'technique':
      return { type: 'technique' };
  }
}

/** Cas d'usage : lit l'état Powens du client (FR-012-03, FR-012-04). */
export const chargerComptesTiers = (client: ClientComptesTiers, stockage: StockageJeton) =>
  executer(stockage, (jeton) => client.lireEtat(jeton));

/** Cas d'usage : lance la connexion à une banque ; la valeur est l'adresse où envoyer le navigateur (FR-012-05). */
export const connecterBanque = (client: ClientComptesTiers, stockage: StockageJeton) =>
  executer(stockage, (jeton) => client.connecter(jeton));

/** Cas d'usage : sélectionne un compte et rend l'état avec ses transactions (FR-012-13, FR-012-14). */
export const selectionnerCompte = (client: ClientComptesTiers, stockage: StockageJeton, compteId: number) =>
  executer(stockage, (jeton) => client.selectionner(jeton, compteId));

/** Cas d'usage : relit les comptes chez Powens (FR-012-16). */
export const rafraichirComptes = (client: ClientComptesTiers, stockage: StockageJeton) =>
  executer(stockage, (jeton) => client.rafraichir(jeton));

/** Cas d'usage : déconnecte Powens (FR-012-15). */
export const deconnecterPowens = (client: ClientComptesTiers, stockage: StockageJeton) =>
  executer(stockage, (jeton) => client.deconnecter(jeton));
