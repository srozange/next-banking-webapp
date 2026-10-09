import { centimesDepuisChaine } from './montant';
import { formaterEurosArrondi } from './portefeuille';
import { formaterDateEcheance } from './pret';

/** Indicateurs globaux lus dans identite-clients (FR-016-02). */
export interface IndicateursIdentite {
  totalClients: number;
  clientsActifs: number;
  conseillers: number;
  totalUtilisateurs: number;
}

/** Indicateur des prêts EN_ATTENTE lu dans credits (FR-016-02) : nombre et cumul en chaîne à 2 décimales. */
export interface IndicateursPrets {
  pretsEnAttente: number;
  montantEnAttente: string;
}

/**
 * Client de la gestion des clients (FR-016-08, FR-016-09) : rôle tel que renvoyé (`CLIENT`), téléphone et conseiller
 * absents possibles, date de création de la fiche `aaaa-MM-jj` (BUG-055 : jamais la date d'affectation).
 */
export interface ClientAdministre {
  clientId: number;
  nomComplet: string;
  login: string;
  email: string;
  telephone: string | null;
  role: string;
  actif: boolean;
  nomConseiller: string | null;
  dateCreation: string;
}

/** Utilisateur de la liste « Tous les utilisateurs du systeme » (FR-016-13), rôle tel que renvoyé. */
export interface UtilisateurAdministre {
  utilisateurId: number;
  nomComplet: string;
  login: string;
  email: string;
  role: string;
  actif: boolean;
}

/**
 * Filtre de la gestion des clients (règle du legacy) : terme rogné, recherché sans casse et en partie dans le nom
 * complet, le login ou l'email ; un terme vide rend tous les clients, dans leur ordre.
 */
export function filtrerClients(clients: readonly ClientAdministre[], terme: string): readonly ClientAdministre[] {
  const cherche = terme.trim().toLowerCase();
  if (cherche === '') {
    return clients;
  }
  return clients.filter((client) =>
    [client.nomComplet, client.login, client.email].some((valeur) => valeur.toLowerCase().includes(cherche)),
  );
}

/** Montant des prêts en attente au format legacy `EUR15,000` (`EUR0` sans prêt), calculé en centimes entiers. */
export function montantATraiter(indicateurs: IndicateursPrets): string {
  return formaterEurosArrondi(centimesDepuisChaine(indicateurs.montantEnAttente));
}

/** « Client depuis » au format `jj/MM/aaaa`, depuis la date de création de la fiche (FR-016-10, BUG-055). */
export function clientDepuis(client: ClientAdministre): string {
  return formaterDateEcheance(client.dateCreation);
}
